package com.eventom.presentation

import com.eventom.data.fake.fakeBackendEngine
import com.eventom.domain.repository.EventDetailRemoteDataSource
import com.eventom.language.LanguageSetting
import com.eventom.language.MemoryLanguageStore
import com.eventom.data.remote.KtorEventDetailRemoteDataSource
import com.eventom.domain.model.Event
import com.eventom.domain.model.EventCategory
import com.eventom.domain.model.EventDetail
import com.eventom.domain.repository.EventRepository
import com.eventom.domain.usecase.FetchEventDetailUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * Detail view model behaviour over the M1 fake backend (same approach as
 * [AuthViewModelTest]: Unconfined scope for inline start, real-time state
 * polling for completion, real HTTP status codes for failures).
 */
class EventDetailViewModelTest {

    private val listEvent = Event(
        id = 2,
        title = "Das Geheimnis der Masken",
        summary = "Große Gefühle, große Stimmen: das Mysteriösmusical der Saison.",
        category = EventCategory.MUSICAL_SHOW,
        startTime = Instant.parse("2027-03-19T20:00:00Z"),
        city = "Hamburg",
        venue = "Operettenpalast",
        priceFrom = 79.50,
    )

    /** The M1 fake backend, wired exactly like production dataModule. */
    private val httpClient = HttpClient(fakeBackendEngine()) {
        expectSuccess = true
        install(ContentNegotiation) { json() }
    }

    private val realDetailSource = KtorEventDetailRemoteDataSource(httpClient)

    @AfterTest
    fun tearDown() {
        httpClient.close()
    }

    /** Minimal in-memory repository serving a fixed list (the detail VM only reads it). */
    private class FakeEventRepository(private val events: List<Event>) : EventRepository {
        private val flow = MutableStateFlow(events)
        override fun observeEvents(): Flow<List<Event>> = flow
        override suspend fun refresh(): Result<List<Event>> = Result.success(events)
        override suspend fun setSaved(id: Long) {
            flow.value = flow.value.map { if (it.id == id) it.copy(isSaved = true) else it }
        }
    }

    /** Delegates to the real source behind a completion gate. */
    private class GatedDetail(
        private val inner: EventDetailRemoteDataSource,
        private val gate: CompletableDeferred<Unit>,
    ) : EventDetailRemoteDataSource {
        override suspend fun fetchEventDetail(id: Long): EventDetail {
            gate.await()
            return inner.fetchEventDetail(id = id)
        }
    }

    private fun makeViewModel(
        detailSource: EventDetailRemoteDataSource = realDetailSource,
        events: List<Event> = listOf(listEvent),
    ): Pair<EventDetailViewModel, FakeEventRepository> {
        val repository = FakeEventRepository(events)
        val viewModel = EventDetailViewModel(
            fetchDetail = FetchEventDetailUseCase(detailSource),
            repository = repository,
            languageSetting = LanguageSetting(
                store = MemoryLanguageStore(),
                mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            ),
            mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        return viewModel to repository
    }

    private suspend fun awaitState(
        viewModel: EventDetailViewModel,
        timeoutMs: Long = 5_000,
        predicate: (EventDetailUiState) -> Boolean,
    ): EventDetailUiState {
        val stop = TimeSource.Monotonic.markNow()
        while (stop.elapsedNow() < timeoutMs.milliseconds) {
            viewModel.state.value.let { if (predicate(it)) return it }
            delay(20)
        }
        error("Timed out waiting for state; last state = ${viewModel.state.value}")
    }

    @Test
    fun knownFixtureIdLoadsSuccessState(): Unit = runBlocking {
        val (viewModel, _) = makeViewModel()

        viewModel.load(eventId = 2L)

        val state = awaitState(viewModel) { it is EventDetailUiState.Success }
        val success = state as EventDetailUiState.Success
        assertEquals(listEvent, success.event)
        assertEquals(2L, success.detail.id)
        assertEquals("17:30", success.detail.doorsTime)
        assertEquals(3, success.detail.ticketCategories.size)
        assertEquals(149.00, success.detail.ticketCategories.first().price)
        assertTrue(success.detail.longSummary.startsWith("Ein Maskenball"))
        assertEquals("Fsk ab 12 Jahren", success.detail.ageInfo)
    }

    @Test
    fun unknownIdDrivesErrorState(): Unit = runBlocking {
        val (viewModel, _) = makeViewModel()

        viewModel.load(eventId = 99L)

        val state = awaitState(viewModel) { it is EventDetailUiState.Error }
        assertEquals("This event is no longer available", (state as EventDetailUiState.Error).message)
    }

    @Test
    fun loadShowsLoadingWhileFetching(): Unit = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val (viewModel, _) = makeViewModel(detailSource = GatedDetail(realDetailSource, gate))

        viewModel.load(eventId = 2L)
        // Runs inline (Unconfined) up to the gate.
        assertIs<EventDetailUiState.Loading>(viewModel.state.value)

        gate.complete(Unit)
        awaitState(viewModel) { it is EventDetailUiState.Success }
    }

    @Test
    fun eventMissingFromListFailsWithoutRemoteCall(): Unit = runBlocking {
        val (viewModel, _) = makeViewModel(events = emptyList())

        viewModel.load(eventId = 2L)

        val state = awaitState(viewModel) { it is EventDetailUiState.Error }
        assertEquals("This event is no longer available", (state as EventDetailUiState.Error).message)
    }

    @Test
    fun resetReturnsToIdle(): Unit = runBlocking {
        val (viewModel, _) = makeViewModel()

        viewModel.load(eventId = 2L)
        awaitState(viewModel) { it is EventDetailUiState.Success }

        viewModel.reset()
        assertIs<EventDetailUiState.Idle>(viewModel.state.value)
    }

    /**
     * A 404 for an event that IS in the list must be phrased by
     * `failureMessage` ("no longer available"), not by the generic branch —
     * the two paths look identical to the user but only one is reached when
     * the list-side event resolves.
     */
    @Test
    fun remoteNotFoundForListedEventReportsUnavailable(): Unit = runBlocking {
        val missing = listEvent.copy(id = 99L)
        val (viewModel, _) = makeViewModel(events = listOf(missing))

        viewModel.load(eventId = 99L)

        val state = awaitState(viewModel) { it is EventDetailUiState.Error }
        assertEquals("This event is no longer available", (state as EventDetailUiState.Error).message)
    }

    /** Transport/parse failures are NOT "no longer available": the event is
     *  still listed, so the message must invite a retry. */
    @Test
    fun transportFailureForListedEventReportsGenericError(): Unit = runBlocking {
        val broken = object : EventDetailRemoteDataSource {
            override suspend fun fetchEventDetail(id: Long): EventDetail = error("socket closed")
        }
        val (viewModel, _) = makeViewModel(detailSource = broken)

        viewModel.load(eventId = 2L)

        val state = awaitState(viewModel) { it is EventDetailUiState.Error }
        assertEquals(
            "Couldn't load details — check your connection and try again",
            (state as EventDetailUiState.Error).message,
        )
    }

    /** Double-tap on the card must not fire a second fetch (same guard class as
     *  the login screen's no-double-submit). */
    @Test
    fun secondLoadWhileLoadingIsIgnored(): Unit = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val counting = object : EventDetailRemoteDataSource {
            var calls = 0
            override suspend fun fetchEventDetail(id: Long): EventDetail {
                calls += 1
                return realDetailSource.fetchEventDetail(id = id)
            }
        }
        val (viewModel, _) = makeViewModel(detailSource = GatedDetail(counting, gate))

        viewModel.load(eventId = 2L)
        assertIs<EventDetailUiState.Loading>(viewModel.state.value)
        viewModel.load(eventId = 2L) // ignored: state is Loading

        gate.complete(Unit)
        awaitState(viewModel) { it is EventDetailUiState.Success }
        assertEquals(1, counting.calls, "the second load() must not reach the remote source")
    }

    /** The typed closure iOS consumes (gotcha #14): states arrive on subscribe
     *  and on every change, and stop() ends delivery. */
    @Test
    fun observeDeliversStatesAndStops(): Unit = runBlocking {
        val (viewModel, _) = makeViewModel()
        val seen = mutableListOf<EventDetailUiState>()

        val observation = viewModel.observe { seen.add(it) }
        viewModel.load(eventId = 2L)
        awaitState(viewModel) { it is EventDetailUiState.Success }

        assertIs<EventDetailUiState.Idle>(seen.first())
        assertTrue(seen.any { it is EventDetailUiState.Loading }, "Loading was skipped: $seen")
        assertTrue(seen.any { it is EventDetailUiState.Success }, "Success never delivered: $seen")

        observation.stop()
        val delivered = seen.size
        viewModel.reset()
        assertEquals(delivered, seen.size, "delivery must end after stop()")
    }

    /**
     * A 4xx that is NOT 404 (here a 403 from a real Ktor engine) must fall
     * through to the generic message. Note 5xx would NOT reach this branch at
     * all: Ktor maps 5xx to ServerResponseException, which is not a
     * ClientRequestException — the `error is ClientRequestException` test is
     * false for it.
     */
    @Test
    fun forbiddenForListedEventReportsGenericError(): Unit = runBlocking {
        val failing = HttpClient(
            MockEngine { respondError(HttpStatusCode.Forbidden) },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json() }
        }
        try {
            val (viewModel, _) = makeViewModel(detailSource = KtorEventDetailRemoteDataSource(failing))

            viewModel.load(eventId = 2L)

            val state = awaitState(viewModel) { it is EventDetailUiState.Error }
            assertEquals(
                "Couldn't load details — check your connection and try again",
                (state as EventDetailUiState.Error).message,
            )
        } finally {
            failing.close()
        }
    }

    /** Production leaves `mainScope` unset (Main dispatcher) — construction with
     *  the DEFAULT argument (not an explicit null) must not throw before any
     *  work is dispatched (gotcha #9). */
    @Test
    fun defaultScopeConstructionIsUsable() {
        val viewModel = EventDetailViewModel(
            fetchDetail = FetchEventDetailUseCase(realDetailSource),
            repository = FakeEventRepository(listOf(listEvent)),
            languageSetting = LanguageSetting(store = MemoryLanguageStore()),
        )

        assertIs<EventDetailUiState.Idle>(viewModel.state.value)
    }
}