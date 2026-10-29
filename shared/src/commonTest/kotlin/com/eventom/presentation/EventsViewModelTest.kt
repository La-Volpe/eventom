package com.eventom.presentation

import com.eventom.data.remote.EventRemoteDataSource
import com.eventom.data.repository.EventRepositoryImpl
import com.eventom.domain.model.Event
import com.eventom.domain.model.EventCategory
import com.eventom.domain.repository.EventRepository
import com.eventom.domain.usecase.FilterEventsUseCase
import com.eventom.domain.usecase.ObserveEventsUseCase
import com.eventom.domain.usecase.RefreshEventsUseCase
import com.eventom.domain.usecase.ToggleEventSavedUseCase
import com.eventom.language.AppLanguage
import com.eventom.language.LanguageSetting
import com.eventom.language.MemoryLanguageStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Language-switched remote: the payload family follows [LanguageSetting.current] —
 *  the same contract the production data source enforces via Accept-Language. */
private class LanguageFamilyRemote(private val setting: LanguageSetting) : EventRemoteDataSource {
    private val german = listOf(
        Event(
            id = 1,
            title = "Aurora Nights — Sommer Tour 2027",
            summary = "",
            category = EventCategory.CONCERTS,
            startTime = Instant.parse("2027-06-12T19:30:00Z") + 1.hours,
            city = "Berlin",
            venue = "Halle am Ufer",
            priceFrom = 59.90,
        ),
    )
    private val english = listOf(
        Event(
            id = 1,
            title = "Aurora Nights — Summer Tour 2027",
            summary = "",
            category = EventCategory.CONCERTS,
            startTime = Instant.parse("2027-06-12T19:30:00Z") + 1.hours,
            city = "Berlin",
            venue = "Halle am Ufer",
            priceFrom = 59.90,
        ),
    )

    override suspend fun fetchEvents(): List<Event> =
        if (setting.current == com.eventom.language.EffectiveLanguage.EN) english else german
}

/**
 * Bilingual 2/4: the events VM observes the language core — an effective
 * change auto-refreshes so the visible tab re-renders in the new payload
 * family (Accept-Language switches server-side; same event ids).
 */
class EventsViewModelTest {

    private suspend fun awaitTitle(viewModel: EventsViewModel, title: String) {
        val stop = TimeSource.Monotonic.markNow()
        while (stop.elapsedNow() < 5.seconds) {
            val event = viewModel.state.value.events.firstOrNull()
            if (event?.title == title) return
            kotlinx.coroutines.delay(20)
        }
        error("Timed out waiting for title \"$title\"; last events = ${viewModel.state.value.events.map { it.title }}")
    }

    @Test
    fun languageChangeAutoRefreshesTheList() = runTest {
        val languageScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val languageSetting = LanguageSetting(store = MemoryLanguageStore(), mainScope = languageScope)
        // Start German: the factory default is the DE payload family.
        languageSetting.choose(AppLanguage.DE)
        val repository: EventRepository = EventRepositoryImpl(
            remote = LanguageFamilyRemote(languageSetting),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        val viewModel = EventsViewModel(
            observeEvents = ObserveEventsUseCase(repository),
            refreshEvents = RefreshEventsUseCase(repository),
            toggleEventSaved = ToggleEventSavedUseCase(repository),
            filterEvents = FilterEventsUseCase(),
            languageSetting = languageSetting,
            // JVM tests have no Main dispatcher (gotcha #9) — inject the Unconfined scope.
            mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

        awaitTitle(viewModel, "Aurora Nights — Sommer Tour 2027")

        // Switch → auto refresh → English family renders immediately.
        languageSetting.choose(AppLanguage.EN)
        awaitTitle(viewModel, "Aurora Nights — Summer Tour 2027")

        // … and back.
        languageSetting.choose(AppLanguage.DE)
        awaitTitle(viewModel, "Aurora Nights — Sommer Tour 2027")
    }

    @Test
    fun savedFlagsSurviveTheLanguageSwitch() = runTest {
        val languageScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val languageSetting = LanguageSetting(store = MemoryLanguageStore(), mainScope = languageScope)
        languageSetting.choose(AppLanguage.DE)
        val repository: EventRepository = EventRepositoryImpl(
            remote = LanguageFamilyRemote(languageSetting),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        val viewModel = EventsViewModel(
            observeEvents = ObserveEventsUseCase(repository),
            refreshEvents = RefreshEventsUseCase(repository),
            toggleEventSaved = ToggleEventSavedUseCase(repository),
            filterEvents = FilterEventsUseCase(),
            languageSetting = languageSetting,
            // JVM tests have no Main dispatcher (gotcha #9) — inject the Unconfined scope.
            mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

        awaitTitle(viewModel, "Aurora Nights — Sommer Tour 2027")
        viewModel.toggleSaved(id = 1)
        kotlinx.coroutines.delay(100)
        assertEquals(true, viewModel.state.value.events.first().isSaved)

        // The English re-fetch re-seeds isSaved from the live repository…
        languageSetting.choose(AppLanguage.EN)
        awaitTitle(viewModel, "Aurora Nights — Summer Tour 2027")
        assertEquals(true, viewModel.state.value.events.first().isSaved)
    }

    // ---- scripts for list behaviour (selection, refresh, failure, observation) ----

    /** Two categories so `select()` has something to actually filter. */
    private val rockNight = Event(
        id = 11,
        title = "Aurora Nights — Sommer Tour 2027",
        summary = "",
        category = EventCategory.CONCERTS,
        startTime = Instant.parse("2027-06-12T19:30:00Z") + 1.hours,
        city = "Berlin",
        venue = "Halle am Ufer",
        priceFrom = 59.90,
    )
    private val sportsNight = Event(
        id = 12,
        title = "Sportnacht 2027",
        summary = "",
        category = EventCategory.SPORTS,
        startTime = Instant.parse("2027-06-13T18:00:00Z") + 1.hours,
        city = "München",
        venue = "Olympiahalle",
        priceFrom = 24.90,
    )
    private val twoEvents = listOf(rockNight, sportsNight)

    /** Lets a test make the NEXT fetch succeed, fail, or hang behind a gate. */
    private class ScriptedRemote(private var payload: () -> List<Event>) : EventRemoteDataSource {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null

        fun respondWith(next: () -> List<Event>) {
            payload = next
        }

        override suspend fun fetchEvents(): List<Event> {
            calls += 1
            gate?.await()
            return payload()
        }
    }

    private fun buildViewModel(remote: EventRemoteDataSource): EventsViewModel {
        val repository: EventRepository = EventRepositoryImpl(
            remote = remote,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        return EventsViewModel(
            observeEvents = ObserveEventsUseCase(repository),
            refreshEvents = RefreshEventsUseCase(repository),
            toggleEventSaved = ToggleEventSavedUseCase(repository),
            filterEvents = FilterEventsUseCase(),
            languageSetting = LanguageSetting(
                store = MemoryLanguageStore(),
                mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            ),
            mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
    }

    private suspend fun awaitCondition(what: String, timeout: kotlin.time.Duration = 5.seconds, predicate: () -> Boolean) {
        val stop = TimeSource.Monotonic.markNow()
        while (stop.elapsedNow() < timeout) {
            if (predicate()) return
            kotlinx.coroutines.delay(20)
        }
        error("Timed out waiting for $what")
    }

    private suspend fun awaitLoaded(viewModel: EventsViewModel) = awaitCondition("the first load to settle") {
        !viewModel.state.value.isRefreshing && viewModel.state.value.events.size == 2
    }

    /** `null` is the "All" chip; a category narrows the already-loaded list
     *  without going back to the network. */
    @Test
    fun selectFiltersTheLoadedListWithoutRefetching() = runTest {
        val remote = ScriptedRemote { twoEvents }
        val viewModel = buildViewModel(remote)
        awaitLoaded(viewModel)
        val callsAfterFirstLoad = remote.calls

        viewModel.select(EventCategory.SPORTS)

        assertEquals(EventCategory.SPORTS, viewModel.state.value.selectedCategory)
        assertEquals(listOf("Sportnacht 2027"), viewModel.state.value.events.map { it.title })

        viewModel.select(null)

        assertNull(viewModel.state.value.selectedCategory)
        assertEquals(2, viewModel.state.value.events.size)
        assertEquals(callsAfterFirstLoad, remote.calls, "filtering must not hit the remote")
    }

    /** A category with no matches must render the empty state, not a stale list. */
    @Test
    fun selectOnAnAbsentCategoryYieldsAnEmptyList() = runTest {
        val remote = ScriptedRemote { twoEvents }
        val viewModel = buildViewModel(remote)
        awaitLoaded(viewModel)

        viewModel.select(EventCategory.LEISURE)

        assertEquals(emptyList(), viewModel.state.value.events)
        assertEquals(EventCategory.LEISURE, viewModel.state.value.selectedCategory)
    }

    /** The pull-to-refresh entry point: the loaded list stays on screen (no
     *  skeleton), so a refresh never flashes the empty state. */
    @Test
    fun publicRefreshKeepsTheLoadedListVisible() = runTest {
        val remote = ScriptedRemote { twoEvents }
        val viewModel = buildViewModel(remote)
        awaitLoaded(viewModel)
        val callsAfterFirstLoad = remote.calls

        viewModel.refresh()

        assertEquals(false, viewModel.state.value.isLoading, "a refresh must not re-enter the loading state")
        awaitCondition("the refresh to finish") { !viewModel.state.value.isRefreshing }
        assertEquals(2, viewModel.state.value.events.size)
        assertEquals(callsAfterFirstLoad + 1, remote.calls)
    }

    /** Double-tap on Refresh must not stack fetches (same guard class as the
     *  login screen's no-double-submit). */
    @Test
    fun refreshWhileRefreshingIsIgnored() = runTest {
        val remote = ScriptedRemote { twoEvents }
        val viewModel = buildViewModel(remote)
        awaitLoaded(viewModel)
        val callsAfterFirstLoad = remote.calls

        val gate = CompletableDeferred<Unit>()
        remote.gate = gate
        viewModel.refresh()
        awaitCondition("the refresh to start") { viewModel.state.value.isRefreshing }

        viewModel.refresh() // ignored: isRefreshing is already true
        assertEquals(callsAfterFirstLoad + 1, remote.calls, "the second refresh must not reach the remote")

        remote.gate = null
        gate.complete(Unit)
        awaitCondition("the gated refresh to finish") { !viewModel.state.value.isRefreshing }
    }

    /** D1: no sample fallback — a failed refresh keeps the previous list and
     *  surfaces the offline banner instead of wiping the screen. */
    @Test
    fun failedRefreshKeepsTheListAndShowsTheOfflineBanner() = runTest {
        val remote = ScriptedRemote { twoEvents }
        val viewModel = buildViewModel(remote)
        awaitLoaded(viewModel)

        remote.respondWith { error("offline") }
        viewModel.refresh()

        awaitCondition("the failure banner") { viewModel.state.value.message != null }
        assertEquals("Offline — showing your last loaded events", viewModel.state.value.message)
        assertEquals(2, viewModel.state.value.events.size, "the loaded list must survive the failure")
        assertEquals(false, viewModel.state.value.isRefreshing)
    }

    /** The typed closure iOS consumes (gotcha #14). */
    @Test
    fun observeDeliversStatesAndStops() = runTest {
        val remote = ScriptedRemote { twoEvents }
        val viewModel = buildViewModel(remote)
        awaitLoaded(viewModel)

        val seen = mutableListOf<EventsUiState>()
        val observation = viewModel.observe { seen.add(it) }
        viewModel.select(EventCategory.SPORTS)
        awaitCondition("the filtered state") { seen.any { it.selectedCategory == EventCategory.SPORTS } }

        observation.stop()
        val delivered = seen.size
        viewModel.select(null)

        assertEquals(delivered, seen.size, "delivery must end after stop()")
    }

    /**
     * A manual refresh after a load that never succeeded must drop the skeleton
     * and show the list. (Deliberately asserts only the post-refresh state: the
     * pre-refresh `isLoading` value is a separate UX question, not pinned here.)
     */
    @Test
    fun manualRefreshAfterAnUnsuccessfulFirstLoadShowsTheList() = runTest {
        val remote = ScriptedRemote { error("offline") }
        val viewModel = buildViewModel(remote)
        awaitCondition("the failed first load to settle") { !viewModel.state.value.isRefreshing }

        remote.respondWith { twoEvents }
        viewModel.refresh()

        awaitCondition("the retry to land") { viewModel.state.value.events.size == 2 }
        assertEquals(false, viewModel.state.value.isLoading, "a successful retry must clear the skeleton")
        assertEquals(null, viewModel.state.value.message, "the banner clears with a successful refresh")
    }
}
