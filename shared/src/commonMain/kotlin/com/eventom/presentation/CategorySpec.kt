package com.eventom.presentation

import com.eventom.domain.model.EventCategory

/**
 * Single editable source of truth for per-category visual metadata:
 * the fallback poster-id stem and the two gradient stops of the poster art.
 *
 * Consumers:
 *  - Android/iOS presentation fallback maps (`PosterMap.kt` / `PosterMap.swift`)
 *  - `tools/generate_posters.py`, which PARSES this file directly (the entry
 *    line format below is load-bearing — one `EventCategory.X to CategorySpec(
 *    "stem", 0xTOP, 0xBOTTOM)` per line; do not reformat casually).
 *
 * Adding a category therefore touches: the enum, an events.json fixture,
 * one line here, and regenerated PNG assets — nothing else.
 */
data class CategorySpec(
    /** Fallback poster ref stem (underscore-sanitized res/asset convention), e.g. "poster_concerts". */
    val posterStem: String,
    /** Poster art gradient stops (ARGB). */
    val colorTop: Long,
    val colorBottom: Long,
)

val categorySpecs: Map<EventCategory, CategorySpec> = mapOf(
    EventCategory.CONCERTS to CategorySpec("poster_concerts", 0xFF1C6EE8, 0xFF0F3FA0),
    EventCategory.MUSICAL_SHOW to CategorySpec("poster_musical_show", 0xFF7B3FE4, 0xFF4A1E9E),
    EventCategory.SPORTS to CategorySpec("poster_sports", 0xFF0E9F6E, 0xFF066544),
    EventCategory.COMEDY to CategorySpec("poster_comedy", 0xFFF59E0B, 0xFFB45309),
    EventCategory.THEATER to CategorySpec("poster_theater", 0xFFDC2626, 0xFF9A1F1F),
    EventCategory.LEISURE to CategorySpec("poster_leisure", 0xFF06B6D4, 0xFF037A9C),
)

/** Typed accessor (Swift bridges functions, not the raw map). */
fun categorySpec(category: EventCategory): CategorySpec = categorySpecs.getValue(category)