package com.eventom.domain.usecase

import com.eventom.domain.model.Event
import com.eventom.domain.model.EventCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterEventsUseCaseTest {

    private val seed = Instant.parse("2026-10-11T09:00:00Z")
    private val filter = FilterEventsUseCase()

    private fun event(id: Long, category: EventCategory, inDays: Long) = Event(
        id = id,
        title = "Event $id",
        summary = "",
        category = category,
        startTime = seed + inDays.days,
        city = "",
        venue = "",
    )

    @Test
    fun sortsByStartTimeAscending() {
        val events = listOf(
            event(3, EventCategory.THEATER, 3),
            event(1, EventCategory.CONCERTS, 1),
            event(2, EventCategory.SPORTS, 2),
        )

        val filtered = filter(events, category = null)

        assertEquals(listOf(1L, 2L, 3L), filtered.map(Event::id))
    }

    @Test
    fun filtersByCategoryWhenProvided() {
        val events = listOf(
            event(1, EventCategory.CONCERTS, 1),
            event(2, EventCategory.THEATER, 2),
            event(3, EventCategory.THEATER, 3),
        )

        val filtered = filter(events, category = EventCategory.THEATER)

        assertEquals(listOf(2L, 3L), filtered.map(Event::id))
    }
}