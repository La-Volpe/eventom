package com.eventom.data.repository

import com.eventom.data.remote.EventRemoteDataSource
import com.eventom.data.remote.parseEventCategory
import com.eventom.domain.model.Event
import com.eventom.domain.model.EventCategory
import com.eventom.domain.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Timeout-shaped failure for the HttpTimeout-policy test hook (T2). */
private class SimulatedReadTimeout : Exception("simulated read timeout")

/** Upstream mapping failure shaped by parseEventCategory's message (T15). */
private class UpstreamCategoryError(code: String) :
    IllegalStateException("Unknown event category: \"$code\"")



/** Plays canned responses call by call, then keeps returning empty. */
private class ScriptedRemote(responses: List<Result<List<Event>>>) : EventRemoteDataSource {
    private val queue = ArrayDeque(responses)
    override suspend fun fetchEvents(): List<Event> =
        (queue.removeFirstOrNull() ?: Result.success(emptyList())).getOrThrow()
}

class EventRepositoryImplTest {

    private val seed: Instant = Instant.parse("2026-10-11T09:00:00Z")

    private fun makeRepository(remote: EventRemoteDataSource): EventRepositoryImpl =
        EventRepositoryImpl(
            remote = remote,
            // Plain default dispatcher: JVM unit tests have no Main.
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )

    @Test
    fun keepsPreviousListWhenRefreshFailsAfterLoad() = runTest {
        // Plan T12 (D1): no sample fallback — a failed refresh keeps the
        // previously loaded list observable and surfaces the failure.
        val priorEvent = Event(
            id = 9,
            title = "Prior state event",
            summary = "",
            category = EventCategory.SPORTS,
            startTime = seed + 1.hours,
            city = "Old town",
            venue = "Hall 9",
        )
        val repository = makeRepository(
            ScriptedRemote(
                listOf(
                    Result.success(listOf(priorEvent)),
                    Result.failure(IllegalStateException("offline")),
                )
            )
        )

        assertTrue(repository.refresh().isSuccess)
        val prior = repository.observeEvents().first()

        val second = repository.refresh()
        assertTrue(second.isFailure, "failure must be surfaced for the offline banner")
        assertEquals(prior, repository.observeEvents().first(), "previous list must be kept after a failed refresh")
    }

    @Test
    fun successfulRefreshReplacesEvents() = runTest {
        val remoteEvent = Event(
            id = 42,
            title = "Remote event",
            summary = "",
            category = EventCategory.THEATER,
            startTime = seed + 1.hours,
            city = "Remote",
            venue = "Hall 1",
        )
        val repository = makeRepository(ScriptedRemote(listOf(Result.success(listOf(remoteEvent)))))

        val result = repository.refresh()

        assertEquals(listOf(remoteEvent), result.getOrNull())
        assertEquals(listOf(remoteEvent), repository.observeEvents().first())
    }

    @Test
    fun keepsPreviousStateWhenRemoteTimesOut() = runTest {
        // First refresh succeeds; the second throws a timeout-shaped error.
        // The repository must keep the prior list state (no blank screen, no
        // sample-data overwrite) and surface the failure through the Result.
        val priorEvent = Event(
            id = 42,
            title = "Prior state event",
            summary = "",
            category = EventCategory.THEATER,
            startTime = seed + 1.hours,
            city = "Old town",
            venue = "Hall 9",
        )
        val repository = makeRepository(
            ScriptedRemote(
                listOf(
                    Result.success(listOf(priorEvent)),
                    Result.failure(SimulatedReadTimeout()),
                )
            )
        )

        assertTrue(repository.refresh().isSuccess)
        val prior = repository.observeEvents().first()

        val second = repository.refresh()
        assertTrue(second.isFailure, "timeout-shaped failure must be surfaced")
        assertEquals(prior, repository.observeEvents().first(), "previous state must be kept after a timeout")
    }

    @Test
    fun refreshCarriesSavedFlagsAcrossRemoteData() = runTest {
        // First refresh loads a remote list; user saves event 2; second
        // refresh (different remote payload, same ids) -> the saved flag
        // survives the merge. Seeding happens through the remote now that
        // the sample fallback is gone (plan T12).
        val repository = makeRepository(
            ScriptedRemote(
                listOf(
                    Result.success(
                        listOf(
                            Event(
                                id = 2,
                                title = "Remote event",
                                summary = "",
                                category = EventCategory.THEATER,
                                startTime = seed,
                                city = "x",
                                venue = "Hall 1",
                            ),
                            Event(
                                id = 7,
                                title = "Other",
                                summary = "",
                                category = EventCategory.CONCERTS,
                                startTime = seed,
                                city = "y",
                                venue = "Hall 2",
                            ),
                        )
                    ),
                    Result.success(
                        listOf(
                            Event(
                                id = 2,
                                title = "Remote event (refreshed)",
                                summary = "",
                                category = EventCategory.THEATER,
                                startTime = seed + 1.hours,
                                city = "x",
                                venue = "Hall 1",
                            ),
                            Event(
                                id = 7,
                                title = "Other (refreshed)",
                                summary = "",
                                category = EventCategory.CONCERTS,
                                startTime = seed + 1.hours,
                                city = "y",
                                venue = "Hall 2",
                            ),
                        )
                    ),
                )
            )
        )
        repository.refresh()
        repository.setSaved(id = 2)
        repository.refresh()

        val events = repository.observeEvents().first()
        assertTrue(events.first { it.id == 2L }.isSaved)
        assertFalse(events.first { it.id == 7L }.isSaved)
    }

    @Test
    fun setSavedTogglesPersistently() = runTest {
        val repository = makeRepository(
            ScriptedRemote(
                listOf(
                    Result.success(
                        listOf(
                            Event(
                                id = 1,
                                title = "Toggle target",
                                summary = "",
                                category = EventCategory.SPORTS,
                                startTime = seed,
                                city = "z",
                                venue = "Hall 1",
                            ),
                        )
                    ),
                )
            )
        )
        repository.refresh()

        repository.setSaved(id = 1)
        assertTrue(repository.observeEvents().first().first { it.id == 1L }.isSaved)

        repository.setSaved(id = 1)
        assertFalse(repository.observeEvents().first().first { it.id == 1L }.isSaved)
    }

    @Test
    fun unknownCategorySurfacesFailureAndKeepsPriorState() = runTest {
        // Parse decision pinned: unknown wire codes throw at the mapping site —...
        val parseError = assertFailsWith<IllegalStateException> { parseEventCategory("burlesque") }
        assertTrue(parseError.message?.contains("burlesque") == true, parseError.message)

        // Through the repository layer (not just the parser): the remote's
        // upstream mapping throws; the prior list stays and failure surfaces.
        val priorEvent = Event(
            id = 5,
            title = "Prior state event",
            summary = "",
            category = EventCategory.THEATER,
            startTime = seed + 1.hours,
            city = "Old town",
            venue = "Hall 5",
        )
        val repository = makeRepository(
            ScriptedRemote(
                listOf(
                    Result.success(listOf(priorEvent)),
                    Result.failure(UpstreamCategoryError("burlesque")),
                )
            )
        )
        assertTrue(repository.refresh().isSuccess)
        val prior = repository.observeEvents().first()

        val second = repository.refresh()
        assertTrue(second.isFailure, "unknown-category failure must surface through the repository")
        assertEquals(prior, repository.observeEvents().first(), "prior list must be kept on the parse failure")
    }

    /**
     * `bootstrap()` is the production entry point (Koin calls it right after
     * constructing the repository), so the first page must arrive on the
     * repository's own scope without the caller awaiting it.
     */
    @Test
    fun bootstrapLoadsTheFirstPageOnTheRepositoryScope() = runTest(timeout = 10.seconds) {
        val remoteEvent = Event(
            id = 7,
            title = "Bootstrapped event",
            summary = "",
            category = EventCategory.CONCERTS,
            startTime = seed + 1.hours,
            city = "Bremen",
            venue = "Pier 2",
        )
        val repository = makeRepository(ScriptedRemote(listOf(Result.success(listOf(remoteEvent)))))

        repository.bootstrap() // returns immediately; the fetch runs on `scope`

        // `.first { }` suspends on the flow, so no virtual-time delay is
        // involved while the Default-dispatcher fetch is in flight.
        val loaded = repository.observeEvents().first { it.isNotEmpty() }
        assertEquals(listOf(remoteEvent), loaded)
    }
}
