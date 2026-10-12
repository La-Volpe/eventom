package com.evantom.data.repository

import com.evantom.data.local.SampleEvents
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * Remote-first repository that falls back to bundled sample data. Events
 * live in a single [MutableStateFlow] — the hot source of truth both UIs
 * observe. A real product would swap the fallback for a local persistence
 * layer (SQLDelight/DataStore); the observable state shape can stay.
 */
class EventRepositoryImpl(
    private val remote: EventRemoteDataSource,
    private val clock: Clock,
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
        // Network unavailable (or endpoint not real yet): keep the demo alive
        // with sample data. Real apps surface this via error mapping instead.
        if (events.value.isEmpty()) {
            events.value = SampleEvents.list(seed = clock.now() + 500.milliseconds)
        }
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