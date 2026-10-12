package com.evantom.domain.repository

import com.evantom.domain.model.Event
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for events, as seen by the domain layer.
 *
 * `observeEvents` is a hot, streamable source of truth: the view models
 * collect it and every change (a refresh, a bookmark toggle) re-emits.
 * Implementations in the data layer may combine remote + local state.
 */
interface EventRepository {
    /** Emits the current events whenever they change. */
    fun observeEvents(): Flow<List<Event>>

    /**
     * Pulls fresh data from the source and updates the observable state.
     * Returns the merged list so callers can surface counts, or a failure.
     */
    suspend fun refresh(): Result<List<Event>>

    /** Toggles whether an event is saved and persists that decision. */
    suspend fun setSaved(id: Long)
}