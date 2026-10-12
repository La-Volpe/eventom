package com.evantom.domain.model

import kotlin.time.Instant

/**
 * Eventim-style categories (design notes, M1): Konzerte, Musical & Show,
 * Sport, Comedy & Humor, Theater & Kultur, Freizeit. `label` is the string
 * the UI renders for chips/nav — kept here so every platform shows the same.
 */
enum class EventCategory(val label: String) {
    CONCERTS("Konzerte"),
    MUSICAL_SHOW("Musical & Show"),
    SPORTS("Sport"),
    COMEDY("Comedy & Humor"),
    THEATER("Theater & Kultur"),
    LEISURE("Freizeit");

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