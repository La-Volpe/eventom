package com.eventom.domain.model

import kotlin.time.Instant

/**
 * Eventim-style categories (design notes, M1). Presentation-free: the
 * German display labels live in the per-platform presentation maps
 * (androidApp `PosterMap.kt`, iOS `PosterMap.swift`) — never here
 * (review F2 / plan T6).
 */
enum class EventCategory {
    CONCERTS,
    MUSICAL_SHOW,
    SPORTS,
    COMEDY,
    THEATER,
    LEISURE;

    companion object {
        val ordered: List<EventCategory> = entries
    }
}

/**
 * Core domain model. Pure Kotlin — no platform or framework types leak in,
 * so both apps can render it freely in Compose or SwiftUI.
 *
 * Note: avoid a `description` property name — it collides with
 * `NSObject.description` on the Swift side and silently renders the Kotlin
 * `toString()` instead (that is why `summary` exists; iOS rendering is out
 * of scope this engagement, but the naming rule stays in force).
 */
data class Event(
    val id: Long,
    val title: String,
    val summary: String,
    val category: EventCategory,
    val startTime: Instant,
    val city: String,
    val venue: String,
    /** Lowest ticket price in EUR (JSON `priceFrom`, "ab X €"); null if unknown/free. */
    val priceFrom: Double? = null,
    /**
     * Bundled poster reference (e.g. "poster_event_1"); null = render without
     * imagery. Plain string on purpose: the wire format stays app-agnostic and
     * each app maps it to its own resource system (Android-only this engagement).
     */
    val imageRef: String? = null,
    val isSaved: Boolean = false,
)