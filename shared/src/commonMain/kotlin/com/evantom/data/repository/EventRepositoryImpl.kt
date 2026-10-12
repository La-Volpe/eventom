package com.evantom.data.repository

import com.evantom.data.remote.EventRemoteDataSource
import com.evantom.domain.model.Event
import com.evantom.domain.repository.EventRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Remote-first repository over a single [MutableStateFlow] — the hot source
 * of truth both UIs observe. Demo content has exactly one source (the API
 * fixtures served by the fake backend; AGENTS.md gotcha #19): on refresh
 * failure the previously loaded list stays observable and the failure is
 * surfaced through the [Result] (the UI maps it to the offline banner) —
 * there is deliberately no bundled sample fallback.
 */
class EventRepositoryImpl(
    private val remote: EventRemoteDataSource,
    private val scope: CoroutineScope,
) : EventRepository {

    private val events = MutableStateFlow<List<Event>>(emptyList())

    override fun observeEvents(): Flow<List<Event>> = events.asStateFlow()

    override suspend fun refresh(): Result<List<Event>> = try {
        val fresh = remote.fetchEvents().let { remoteEvents ->
            // Carry over saved flags from what the user has on device.
            val savedIds = events.value.filter(Event::isSaved).map(Event::id).toSet()
            remoteEvents.map { it.copy(isSaved = it.id in savedIds) }
        }
        events.value = fresh
        Result.success(fresh)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        // Refresh failed (transport, timeout, parse): keep the previously
        // loaded observable state untouched — no sample fallback — and let
        // the failure surface through the Result for the UI banner.
        Result.failure(error)
    }

    override suspend fun setSaved(id: Long) {
        events.value = events.value.map { event ->
            if (event.id == id) event.copy(isSaved = !event.isSaved) else event
        }
    }

    /** Kick off the first fetch without blocking UI start-up. */
    fun bootstrap() {
        scope.launch { refresh() }
    }
}