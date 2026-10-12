package com.eventom.domain.usecase

import com.eventom.domain.repository.EventDetailRemoteDataSource
import com.eventom.domain.model.EventDetail
import kotlinx.coroutines.CancellationException

/**
 * Fetches one event's detail payload (GET /api/event-details/{id} through the
 * fake backend). Boundary use case: keeps the view model away from the data
 * source contract and turns transport failures into [Result.failure] while
 * keeping cancellation intact (repo gotcha #10: no `runCatching` on suspend).
 */
class FetchEventDetailUseCase(private val detail: EventDetailRemoteDataSource) {

    suspend operator fun invoke(id: Long): Result<EventDetail> = try {
        Result.success(detail.fetchEventDetail(id = id))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}