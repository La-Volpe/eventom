package com.evantom.data.remote

import com.evantom.domain.model.EventDetail
import com.evantom.domain.model.TicketCategory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

/**
 * One event's detail payload (M4 will render it; the data plumbing is part
 * of the fake backend milestone). Separate interface so [EventRemoteDataSource]
 * stays a pure list source and existing fakes keep compiling.
 */
interface EventDetailRemoteDataSource {
    suspend fun fetchEventDetail(id: Long): EventDetail
}

/** Wire format of `GET /api/event-details/{id}`. */
@Serializable
internal data class EventDetailDto(
    val id: Long,
    val longDescription: String,
    val lineup: List<String> = emptyList(),
    /** Time of day the doors open, as the API renders it ("18:00"). */
    val doorsTime: String = "",
    val ticketCategories: List<TicketCategoryDto> = emptyList(),
    val ageInfo: String? = null,
)

@Serializable
internal data class TicketCategoryDto(
    val name: String,
    val price: Double,
)

internal fun EventDetailDto.toDomain(): EventDetail = EventDetail(
    id = id,
    // `longSummary`, never `description` —NSObject owns that name (repo gotcha #4).
    longSummary = longDescription,
    lineup = lineup,
    doorsTime = doorsTime,
    ticketCategories = ticketCategories.map { TicketCategory(name = it.name, price = it.price) },
    ageInfo = ageInfo.orEmpty(),
)

class KtorEventDetailRemoteDataSource(private val client: HttpClient) : EventDetailRemoteDataSource {
    override suspend fun fetchEventDetail(id: Long): EventDetail =
        client.get("$API_HOST$API_BASE_PATH/event-details/$id").body<EventDetailDto>().toDomain()
}