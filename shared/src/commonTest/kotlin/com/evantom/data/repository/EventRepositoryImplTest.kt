package com.evantom.data.repository

import com.evantom.data.remote.EventRemoteDataSource
import com.evantom.domain.model.Event
import com.evantom.domain.model.EventCategory
import com.evantom.domain.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Fixed clock for deterministic tests. */
private class FixedClock(private val frozen: Instant) : Clock {
    override fun now(): Instant = frozen
}

/** Timeout-shaped failure for the HttpTimeout-policy test hook (T2). */
private class SimulatedReadTimeout : Exception("simulated read timeout")

/** Always fails — exercises the sample-data fallback path. */
private class FailingRemote : EventRemoteDataSource {
    override suspend fun fetchEvents(): List<Event> = throw IllegalStateException("offline")
}

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
            clock = FixedClock(seed),
            // Plain default dispatcher: JVM unit tests have no Main.
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )

    @Test
    fun fallsBackToSampleEventsWhenRemoteFails() = runTest {
        val repository = makeRepository(FailingRemote())

        val result = repository.refresh()

        assertTrue(result.isFailure)
        assertTrue(
            repository.observeEvents().first().isNotEmpty(),
            "sample data should appear after a failed refresh",
        )
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
        // First call fails -> sample data (events 1..6); user saves event 2;
        // second call succeeds -> the saved flag survives the merge.
        val repository = makeRepository(
            ScriptedRemote(
                listOf(
                    Result.failure(IllegalStateException("scripted failure")),
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
        val repository = makeRepository(FailingRemote())
        repository.refresh()

        repository.setSaved(id = 1)
        assertTrue(repository.observeEvents().first().first { it.id == 1L }.isSaved)

        repository.setSaved(id = 1)
        assertFalse(repository.observeEvents().first().first { it.id == 1L }.isSaved)
    }
}