package com.eventom.data.remote

import com.eventom.domain.model.EventCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * Wire -> domain translation for the optional fields. The fixtures always send
 * every key, so nothing else exercises the absent-key path — and that path is
 * what keeps an older/malformed server response parseable instead of blanking
 * the list (plan T12: there is no sample fallback to fall back on).
 */
class EventDtoMappingTest {
    private fun requiredOnly() =
        EventDto(
            id = 4,
            title = "Nacht der Trommeln",
            category = "musical-show",
            dateTime = "2027-05-01T20:00:00Z",
            city = "Köln",
            venue = "Lanxess Arena",
        )

    @Test
    fun optionalFieldsAreKeptWhenPresent() {
        val filled =
            requiredOnly().copy(
                priceFrom = 39.90,
                imageRef = "poster_event_4",
                shortPitch = "Ein Abend voller Rhythmus.",
            )

        val event = filled.toDomain()

        assertEquals(39.90, event.priceFrom)
        assertEquals("poster_event_4", event.imageRef)
        assertEquals("Ein Abend voller Rhythmus.", event.summary)
        assertEquals(EventCategory.MUSICAL_SHOW, event.category)
        assertEquals(Instant.parse("2027-05-01T20:00:00Z"), event.startTime)
    }

    @Test
    fun absentOptionalFieldsDegradeToNullAndEmptySummary() {
        val event = requiredOnly().toDomain()

        assertNull(event.priceFrom, "unknown price must stay null, not 0.0")
        assertNull(event.imageRef, "no poster keeps the card text-only")
        assertEquals("", event.summary, "a missing pitch must be an empty summary, never null")
    }

    /** The wire code is trimmed before matching, so a stray space from a
     *  hand-edited fixture still maps. */
    @Test
    fun wireCodesAreTrimmed() {
        val event = requiredOnly().copy(category = "  comedy  ").toDomain()

        assertEquals(EventCategory.COMEDY, event.category)
    }
}
