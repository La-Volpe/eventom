package com.eventom.presentation

import com.eventom.data.remote.EventRemoteDataSource
import com.eventom.data.repository.EventRepositoryImpl
import com.eventom.domain.model.Event
import com.eventom.domain.model.EventCategory
import com.eventom.domain.usecase.FilterEventsUseCase
import com.eventom.domain.usecase.ObserveEventsUseCase
import com.eventom.domain.usecase.RefreshEventsUseCase
import com.eventom.domain.usecase.ToggleEventSavedUseCase
import com.eventom.language.LanguageSetting
import com.eventom.language.LanguageStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Production scope wiring, which every other view-model test deliberately
 * bypasses by injecting an Unconfined scope (gotcha #9). `EventsViewModel`
 * starts fetching in its `init`, so its default `mainScope` cannot be exercised
 * without a Main dispatcher — hence a JVM-host test that installs one, rather
 * than a common test that would have to fake it on the iOS targets too.
 */
class ViewModelDefaultScopeTest {
    private val event =
        Event(
            id = 1,
            title = "Aurora Nights — Sommer Tour 2027",
            summary = "",
            category = EventCategory.CONCERTS,
            startTime = Instant.parse("2027-06-12T19:30:00Z") + 2.hours,
            city = "Berlin",
            venue = "Halle am Ufer",
            priceFrom = 59.90,
        )

    private class FixedRemote(
        private val events: List<Event>,
    ) : EventRemoteDataSource {
        override suspend fun fetchEvents(): List<Event> = events
    }

    /** The store default is covered by the Robolectric actuals test (it needs a
     *  Context); here only the SCOPE has to stay defaulted. */
    private class InMemoryStore : LanguageStore {
        private var value: String = ""

        override fun readStoredLanguage(): String = value

        override fun writeStoredLanguage(value: String) {
            this.value = value
        }
    }

    @Before
    fun installMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun uninstallMainDispatcher() {
        Dispatchers.resetMain()
    }

    /** The constructor argument is OMITTED on purpose: production leaves it
     *  unset and the base class resolves `Dispatchers.Main`. */
    @Test
    fun eventsViewModelLoadsOnTheDefaultMainScope() =
        runTest(timeout = 15.seconds) {
            val repository =
                EventRepositoryImpl(
                    remote = FixedRemote(listOf(event)),
                    scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
                )

            val viewModel =
                EventsViewModel(
                    observeEvents = ObserveEventsUseCase(repository),
                    refreshEvents = RefreshEventsUseCase(repository),
                    toggleEventSaved = ToggleEventSavedUseCase(repository),
                    filterEvents = FilterEventsUseCase(),
                    languageSetting = LanguageSetting(store = InMemoryStore()),
                )

            // The init block refreshes on Main; with the test dispatcher installed
            // that work runs eagerly, so the list is populated without polling.
            assertEquals(listOf(event), viewModel.state.value.events)
            assertEquals(false, viewModel.state.value.isLoading)
        }
}
