package com.evantom.data.local

import com.evantom.domain.model.Event
import com.evantom.domain.model.EventCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Deterministic sample data used when the remote API cannot be reached.
 * Start times are relative to [list]'s `seed` argument, so the demo always
 * shows upcoming events instead of going stale.
 */
object SampleEvents {

    fun list(seed: Instant): List<Event> = listOf(
        Event(
            id = 1,
            title = "EvenTom Launch Party",
            summary = "Meet the team, watch demos, grab swag.",
            category = EventCategory.CONCERTS,
            startTime = seed + 2.hours,
            imageRef = "poster_concerts",
            city = "Berlin",
            venue = "Halle am Ufer",
            priceFrom = 19.90,
        ),
        Event(
            id = 2,
            title = "Kotlin Multiplatform Deep Dive",
            summary = "Sharing logic across Android and iOS without sharing UI.",
            category = EventCategory.THEATER,
            startTime = seed + 1.days,
            imageRef = "poster_theater",
            city = "Hamburg",
            venue = "Tech Hub, Room 204",
            priceFrom = 12.00,
        ),
        Event(
            id = 3,
            title = "EvenTom Open Cup",
            summary = "Five-a-side football, all levels welcome.",
            category = EventCategory.SPORTS,
            startTime = seed + 3.days,
            imageRef = "poster_sports",
            city = "München",
            venue = "Green Park, Pitch 3",
            priceFrom = 5.50,
        ),
        Event(
            id = 4,
            title = "Indie Night Live",
            summary = "Three local bands, one small stage.",
            category = EventCategory.CONCERTS,
            startTime = seed + 4.days,
            imageRef = "poster_concerts",
            city = "Leipzig",
            venue = "The Attic Club",
            priceFrom = 14.90,
        ),
        Event(
            id = 5,
            title = "Compose vs SwiftUI Showdown",
            summary = "Same features, two frameworks, friendly rivalry.",
            category = EventCategory.MUSICAL_SHOW,
            startTime = seed + 6.days,
            imageRef = "poster_musical_show",
            city = "Köln",
            venue = "Tech Hub, Auditorium",
            priceFrom = 9.90,
        ),
        Event(
            id = 6,
            title = "Neighborhood Cleanup Day",
            summary = "Gloves provided; bring your own enthusiasm.",
            category = EventCategory.LEISURE,
            startTime = seed + 8.days,
            imageRef = "poster_leisure",
            city = "Dresden",
            venue = "Riverside Entrance",
        ),
    )
}