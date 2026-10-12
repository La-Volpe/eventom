package com.evantom.domain.usecase

import com.evantom.domain.model.Event
import com.evantom.domain.model.EventCategory
import com.evantom.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

/**
 * Domain operations the presentation layer is allowed to call. Use cases
 * keep view models from knowing how data is fetched or stored — small,
 * single-purpose classes with an `invoke` each.
 */

/** Streams the event list; view models collect this and map it to UI state. */
class ObserveEventsUseCase(private val repository: EventRepository) {
    operator fun invoke(): Flow<List<Event>> = repository.observeEvents()
}

/** Pulls fresh data (remote-first) and updates the observable repository. */
class RefreshEventsUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(): Result<List<Event>> = repository.refresh()
}

/** Flips the saved flag on a single event. */
class ToggleEventSavedUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(id: Long) {
        repository.setSaved(id)
    }
}

/** Pure ordering/filtering policy — trivially unit-testable, no I/O. */
class FilterEventsUseCase {
    operator fun invoke(events: List<Event>, category: EventCategory?): List<Event> =
        events
            .sortedBy { it.startTime.toEpochMilliseconds() }
            .filter { category == null || it.category == category }
}