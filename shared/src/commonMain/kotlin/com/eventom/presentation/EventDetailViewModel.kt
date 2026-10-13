package com.eventom.presentation

import com.eventom.domain.model.Event
import com.eventom.domain.model.EventDetail
import com.eventom.domain.repository.EventRepository
import com.eventom.domain.usecase.FetchEventDetailUseCase
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import com.eventom.core.UiObservation
import com.eventom.language.LanguageSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/**
 * Event detail screen states (mirror of [AuthUiState]; `sealed class` keeps
 * the future Swift surface a castable class hierarchy — repo gotcha #5).
 */
sealed class EventDetailUiState {
    /** Nothing requested yet. */
    data object Idle : EventDetailUiState()

    /** Detail fetch in flight. */
    data object Loading : EventDetailUiState()

    /**
     * Everything the screen renders: the list-side [Event] (title, category,
     * venue, price — the detail endpoint does not repeat those) plus the
     * detail payload itself.
     */
    data class Success(val event: Event, val detail: EventDetail) : EventDetailUiState()

    /** Request refused or payload missing; [message] is UI-ready. */
    data class Error(val message: String) : EventDetailUiState()
}

/**
 * Owns the detail screen logic: [load] resolves the list event from the
 * repository and the payload from the remote source. Call [reset] when the
 * screen goes away so a re-entry starts fresh (same contract as auth).
 */
class EventDetailViewModel(
    private val fetchDetail: FetchEventDetailUseCase,
    repository: EventRepository,
    private val languageSetting: LanguageSetting,
    mainScope: CoroutineScope? = null,
) : BaseViewModel<EventDetailUiState>(initialState = EventDetailUiState.Idle, mainScope = mainScope) {

    private val events = repository.observeEvents()

    /** Swift-safe observation (concrete closure, repo gotcha #5). */
    fun observe(onChange: (EventDetailUiState) -> Unit): UiObservation = observeState(onChange)

    fun load(eventId: Long) {
        if (state.value is EventDetailUiState.Loading) return
        launch {
            updateState { EventDetailUiState.Loading }
            val outcome = fetchDetail(id = eventId)
            // Resolve the list event AFTER the fetch completes, from the LIVE
            // repository stream — the rendered Event mirrors current isSaved
            // flags instead of a stale one-shot snapshot (repo gotcha: detail
            // snapshot freshness).
            val event = events.first().firstOrNull { it.id == eventId }
            updateState { _ ->
                when {
                    outcome.getOrNull() != null && event != null ->
                        EventDetailUiState.Success(event = event, detail = outcome.getOrNull()!!)
                    event == null -> EventDetailUiState.Error(eventMissingMessage)
                    else -> EventDetailUiState.Error(failureMessage(outcome.exceptionOrNull()))
                }
            }
        }
    }

    /** Called when the detail screen is left, so the next visit starts fresh. */
    fun reset() = updateState { EventDetailUiState.Idle }

    private val eventMissingMessage: String
        get() = languageSetting.strings().eventNoLongerAvailable()

    private fun failureMessage(error: Throwable?): String {
        val strings = languageSetting.strings()
        return when {
            error is ClientRequestException && error.response.status == HttpStatusCode.NotFound ->
                strings.eventNoLongerAvailable()
            else ->
                strings.detailGenericError()
        }
    }
}