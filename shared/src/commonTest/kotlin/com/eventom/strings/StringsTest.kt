package com.eventom.strings

import com.eventom.domain.model.EventCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Bilingual table sanity: the interface enforces the symbol set (missing
 * props fail compilation); here a few critical PAIRS are pinned byte-exact —
 * the DE side must remain the walkthrough-pinned shipped set.
 */
class StringsTest {

    @Test
    fun loginTitlePinned() {
        assertEquals("Sign in to see your events", StringsDe.loginTitle())
        assertEquals("Sign in to see your events", StringsEn.loginTitle())
    }

    @Test
    fun saveAndSavedPinned() {
        assertEquals("☆ Save", StringsDe.saveLabel())
        assertEquals("★ Saved", StringsDe.savedLabel())
        assertEquals("☆ Save", StringsEn.saveLabel())
        assertEquals("★ Saved", StringsEn.savedLabel())
    }

    @Test
    fun offlineBannerPinned() {
        assertEquals("Offline — showing your last loaded events", StringsDe.offlineBanner())
        assertEquals("Offline — showing your last loaded events", StringsEn.offlineBanner())
    }

    @Test
    fun categoryLabelsGermanPinned() {
        assertEquals("Konzerte", StringsDe.category(EventCategory.CONCERTS))
        assertEquals("Musical & Show", StringsDe.category(EventCategory.MUSICAL_SHOW))
        assertEquals("Sport", StringsDe.category(EventCategory.SPORTS))
        assertEquals("Comedy & Humor", StringsDe.category(EventCategory.COMEDY))
        assertEquals("Theater & Kultur", StringsDe.category(EventCategory.THEATER))
        assertEquals("Freizeit", StringsDe.category(EventCategory.LEISURE))
    }

    @Test
    fun categoryLabelsEnglish() {
        assertEquals("Concerts", StringsEn.category(EventCategory.CONCERTS))
        assertEquals("Musicals & Shows", StringsEn.category(EventCategory.MUSICAL_SHOW))
        assertEquals("Leisure", StringsEn.category(EventCategory.LEISURE))
    }

    @Test
    fun germanWordmarkAndTaglineUnchanged() {
        assertEquals("Tickets für Live-Entertainment", StringsDe.splashTagline())
        assertEquals("Tickets for live entertainment", StringsEn.splashTagline())
        assertTrue(StringsDe.buyTicketsCta() == "Tickets kaufen")
        assertEquals("Buy tickets", StringsEn.buyTicketsCta())
    }

    @Test
    fun accessorResolvesTables() {
        assertEquals(StringsDe, strings(language = com.eventom.language.EffectiveLanguage.DE))
        assertEquals(StringsEn, strings(language = com.eventom.language.EffectiveLanguage.EN))
    }
}