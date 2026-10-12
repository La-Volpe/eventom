package com.evantom.android

import com.evantom.domain.model.EventCategory

/**
 * Maps shared `imageRef` values to bundled drawable resources — Android-only;
 * `shared` deliberately knows nothing about resource ids. Unknown refs fall
 * back to the event's category poster; so does a null ref (text-only fallback
 * path always has artwork).
 */
private val EVENT_REF_RESOURCES: Map<String, Int> = mapOf(
    "poster_event_1" to R.drawable.poster_event_1,
    "poster_event_2" to R.drawable.poster_event_2,
    "poster_event_3" to R.drawable.poster_event_3,
    "poster_event_4" to R.drawable.poster_event_4,
    "poster_event_5" to R.drawable.poster_event_5,
    "poster_event_6" to R.drawable.poster_event_6,
)

private val CATEGORY_FALLBACK: Map<EventCategory, Int> = mapOf(
    EventCategory.CONCERTS to R.drawable.poster_concerts,
    EventCategory.MUSICAL_SHOW to R.drawable.poster_musical_show,
    EventCategory.SPORTS to R.drawable.poster_sports,
    EventCategory.COMEDY to R.drawable.poster_comedy,
    EventCategory.THEATER to R.drawable.poster_theater,
    EventCategory.LEISURE to R.drawable.poster_leisure,
)

/** Resource id for an event's poster, resolving [imageRef] or the category fallback. */
internal fun posterResFor(imageRef: String?, category: EventCategory): Int =
    imageRef?.let(EVENT_REF_RESOURCES::get) ?: CATEGORY_FALLBACK.getValue(category)