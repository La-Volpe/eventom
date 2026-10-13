package com.eventom.data.remote

import com.eventom.domain.model.EventDetail
import com.eventom.domain.repository.EventDetailRemoteDataSource
import com.eventom.domain.model.TicketCategory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import com.eventom.language.EffectiveLanguage
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

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

class KtorEventDetailRemoteDataSource(
    private val client: HttpClient,
    // Injectable provider (bilingual 1/4): every outgoing request carries
    // Accept-Language sourced from the shared LanguageSetting.
    private val languageProvider: () -> EffectiveLanguage = { EffectiveLanguage.DE },
) : EventDetailRemoteDataSource {
    override suspend fun fetchEventDetail(id: Long): EventDetail =
        client.get("$API_HOST$API_BASE_PATH/event-details/$id") {
            headers.append(HttpHeaders.AcceptLanguage, acceptLanguageWire(languageProvider()))
        }.body<EventDetailDto>().toDomain()
}