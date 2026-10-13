package com.eventom.presentation

import com.eventom.domain.model.Event
import com.eventom.domain.model.EventCategory
import com.eventom.domain.usecase.FilterEventsUseCase
import com.eventom.domain.usecase.ObserveEventsUseCase
import com.eventom.domain.usecase.RefreshEventsUseCase
import com.eventom.domain.usecase.ToggleEventSavedUseCase
import com.eventom.core.UiObservation
import com.eventom.language.LanguageSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Everything the events screen needs to render — no behaviour, just data.
 * A data class keeps the Swift bridge trivial (plain getters); add fields
 * freely, defaults included, and both UIs keep compiling.
 */
data class EventsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val events: List<Event> = emptyList(),
    val categories: List<EventCategory> = EventCategory.ordered,
    val selectedCategory: EventCategory? = null,
    val message: String? = null,
) {
    companion object {
        /** Swift-safe "empty first frame"; Swift cannot use default args. */
        fun initial(): EventsUiState = EventsUiState()
    }
}

/**
 * Owns the events screen logic. The UI (either platform) only calls
 * [select], [toggleSaved], [refresh] and renders [state].
 */
class EventsViewModel(
    observeEvents: ObserveEventsUseCase,
    private val refreshEvents: RefreshEventsUseCase,
    private val toggleEventSaved: ToggleEventSavedUseCase,
    private val filterEvents: FilterEventsUseCase,
    private val languageSetting: LanguageSetting,
    // Optional scope injection (Tests inject; production resolves Main — gotcha #9).
    mainScope: CoroutineScope? = null,
) : BaseViewModel<EventsUiState>(initialState = EventsUiState.initial(), mainScope = mainScope) {

    private var allEvents: List<Event> = emptyList()

    /** Language the currently rendered payload family was fetched with. */
    private var languageOnLastRefresh = languageSetting.current

    /**
     * Swift-facing observation: every state change is delivered to
     * [onChange] until the returned observation is stopped.
     */
    fun observe(onChange: (EventsUiState) -> Unit): UiObservation = observeState(onChange)

    init {
        // 1) Mirror the repository stream into UI state, applying the filter.
        launch {
            observeEvents().collect { events ->
                allEvents = events
                updateState { it.copy(isLoading = false, events = filterEvents(events, it.selectedCategory)) }
            }
        }
        // 2) Language re-render (bilingual 2/4): an effective change re-fetches
        // through the repository — the data source resolves Accept-Language
        // from [languageSetting.current] per request, so the next refresh
        // carries the new payload family; saved flags survive (same ids).
        // The first (current) emission is skipped: init already refreshed.
        launch {
            languageSetting.effective.collect { language ->
                if (language != languageOnLastRefresh) {
                    languageOnLastRefresh = language
                    refresh(silent = true)
                }
            }
        }
        // 3) Kick off the first network fetch (falls back to sample data).
        refresh(silent = true)
    }

    /** `null` selects "All". */
    fun select(category: EventCategory?) {
        updateState { current ->
            current.copy(selectedCategory = category, events = filterEvents(allEvents, category))
        }
    }

    fun toggleSaved(id: Long) {
        launch { toggleEventSaved(id) }
    }

    fun refresh() = refresh(silent = false)

    private fun refresh(silent: Boolean) {
        if (state.value.isRefreshing) return
        updateState { it.copy(isRefreshing = true, message = null, isLoading = it.isLoading && silent) }
        launch {
            val outcome = refreshEvents()
            updateState { current ->
                val failure = outcome.exceptionOrNull()
                // D1: no sample fallback — the previously loaded list stays on
                // screen, so the banner must not claim "sample events".
                current.copy(
                    isRefreshing = false,
                    message = failure?.let { languageSetting.strings().offlineBanner() },
                )
            }
        }
    }
}