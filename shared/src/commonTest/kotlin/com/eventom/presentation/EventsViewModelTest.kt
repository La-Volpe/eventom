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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
}