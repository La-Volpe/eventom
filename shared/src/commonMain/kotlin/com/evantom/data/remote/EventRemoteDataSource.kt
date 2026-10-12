package com.evantom.data.remote

import com.evantom.domain.model.Event
import com.evantom.domain.model.EventCategory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Contract for fetching events from the network. Kept as an interface so the
 * repository (and tests) can swap implementations: a fake, a second API, or
 * a cached variant.
 */
interface EventRemoteDataSource {
    suspend fun fetchEvents(): List<Event>
}

/**
 * Where the EvenTom API lives. The fake backend (M1, `data/fake/`) ignores
 * the host and routes purely on the path; a real engine needs the full URL.
 */
internal const val API_HOST = "https://api.evantom.example.com"
internal const val API_BASE_PATH = "/api"

/**
 * JSON shape we expect from the remote API — exactly the documented wire
 * format of `GET /api/events`. `@Serializable` takes care of the mapping;
 * add fields here — not in the domain model — when the API changes, then
 * translate in [EventDto.toDomain].
 */
@Serializable
internal data class EventDto(
    val id: Long,
    val title: String,
    /** Stable wire code: "concerts", "musical-show", "sports", "comedy", "theater", "leisure". */
    val category: String,
    /** ISO-8601 timestamp. */
    val dateTime: String,
    val city: String,
    val venue: String,
    /** Lowest ticket price in EUR; null when unknown or free. */
    val priceFrom: Double? = null,
    /** Bundled poster reference (e.g. "poster_event_1"); null keeps cards
     *  text-only and still parses on older/malformed responses. Never a
     *  remote URL — apps map it to a resource locally. */
    val imageRef: String? = null,
    val shortPitch: String? = null,
)

internal fun EventDto.toDomain(): Event = Event(
    id = id,
    title = title,
    summary = shortPitch.orEmpty(),
    category = parseEventCategory(category),
    startTime = Instant.parse(dateTime),
    city = city,
    venue = venue,
    priceFrom = priceFrom,
    imageRef = imageRef,
)

/** Wire code -> enum. An unknown code fails parsing loudly (refresh falls back to sample data). */
internal fun parseEventCategory(code: String): EventCategory {
    val trimmed = code.trim()
    return EventCategory.entries.firstOrNull { it.wireName == trimmed }
        ?: error("Unknown event category: \"$code\"")
}

private val EventCategory.wireName: String
    get() = name.lowercase().replace('_', '-')

/**
 * Real implementation backed by Ktor. Throws on any transport problem —
 * the repository decides what to do about it (fall back to local data).
 */
class KtorEventRemoteDataSource(private val client: HttpClient) : EventRemoteDataSource {
    override suspend fun fetchEvents(): List<Event> =
        client.get("$API_HOST$API_BASE_PATH/events").body<List<EventDto>>().map(EventDto::toDomain)
}