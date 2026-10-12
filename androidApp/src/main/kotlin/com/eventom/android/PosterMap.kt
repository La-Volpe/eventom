package com.eventom.android

import com.eventom.domain.model.EventCategory

/**
 * Maps shared `imageRef` values to bundled drawable resources — Android-only;
 * `shared` deliberately knows nothing about resource ids. Unknown refs fall
 * back to the event's category poster; so does a null ref (text-only fallback
 * path always has artwork).
 *
 * The fallback STEMS live in the shared `categorySpecs` table
 * (`com.eventom.presentation.CategorySpec`) — the single editable source of
 * truth also consumed by `tools/generate_posters.py`. The R-id maps below are
 * the compile-time counterparts of those stems; the display labels (moved out
 * of the domain model, T6) live here as the ANDROID presentation map — the
 * iOS map in PosterMap.swift must match byte-for-byte (XCUITests walk them).
 */
private val EVENT_REF_RESOURCES: Map<String, Int> = mapOf(
    "poster_event_1" to R.drawable.poster_event_1,
    "poster_event_2" to R.drawable.poster_event_2,
    "poster_event_3" to R.drawable.poster_event_3,
    "poster_event_4" to R.drawable.poster_event_4,
    "poster_event_5" to R.drawable.poster_event_5,
    "poster_event_6" to R.drawable.poster_event_6,
)

private val EVENT_THUMB_RESOURCES: Map<String, Int> = mapOf(
    "poster_event_1" to R.drawable.poster_event_1_thumb,
    "poster_event_2" to R.drawable.poster_event_2_thumb,
    "poster_event_3" to R.drawable.poster_event_3_thumb,
    "poster_event_4" to R.drawable.poster_event_4_thumb,
    "poster_event_5" to R.drawable.poster_event_5_thumb,
    "poster_event_6" to R.drawable.poster_event_6_thumb,
)

private val CATEGORY_FALLBACK: Map<EventCategory, Int> = mapOf(
    EventCategory.CONCERTS to R.drawable.poster_concerts,
    EventCategory.MUSICAL_SHOW to R.drawable.poster_musical_show,
    EventCategory.SPORTS to R.drawable.poster_sports,
    EventCategory.COMEDY to R.drawable.poster_comedy,
    EventCategory.THEATER to R.drawable.poster_theater,
    EventCategory.LEISURE to R.drawable.poster_leisure,
)

private val CATEGORY_THUMB_FALLBACK: Map<EventCategory, Int> = mapOf(
    EventCategory.CONCERTS to R.drawable.poster_concerts_thumb,
    EventCategory.MUSICAL_SHOW to R.drawable.poster_musical_show_thumb,
    EventCategory.SPORTS to R.drawable.poster_sports_thumb,
    EventCategory.COMEDY to R.drawable.poster_comedy_thumb,
    EventCategory.THEATER to R.drawable.poster_theater_thumb,
    EventCategory.LEISURE to R.drawable.poster_leisure_thumb,
)

/** Resource id for an event's poster (hero), resolving [imageRef] or the category fallback. */
internal fun posterResFor(imageRef: String?, category: EventCategory): Int =
    imageRef?.let(EVENT_REF_RESOURCES::get) ?: CATEGORY_FALLBACK.getValue(category)

/**
 * Resource id for an event's LIST thumbnail (T5): a 480×600 decode (~1.2MB
 * at ARGB_8888) instead of the ~29MB hero decode — the card-scroll budget.
 * Detail (hero) keeps `posterResFor`.
 */
internal fun posterThumbResFor(imageRef: String?, category: EventCategory): Int =
    imageRef?.let(EVENT_THUMB_RESOURCES::get) ?: CATEGORY_THUMB_FALLBACK.getValue(category)

/** Android presentation label map (T6) — German text moves here from the domain enum. */
internal fun categoryLabel(category: EventCategory): String = when (category) {
    EventCategory.CONCERTS -> "Konzerte"
    EventCategory.MUSICAL_SHOW -> "Musical & Show"
    EventCategory.SPORTS -> "Sport"
    EventCategory.COMEDY -> "Comedy & Humor"
    EventCategory.THEATER -> "Theater & Kultur"
    EventCategory.LEISURE -> "Freizeit"
}