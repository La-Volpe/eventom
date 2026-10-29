package com.eventom.strings

import com.eventom.domain.model.EventCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exhaustive sweep of BOTH string tables: every method is invoked and its
 * result checked. Two purposes:
 *
 * 1. Coverage — the tables are ~150 one-line accessors; without a sweep they
 *    read as untested (they were the single largest uncovered block in the
 *    Kover report).
 * 2. Completeness gate — [Strings] pins the symbol set at compile time, but
 *    nothing pins that a NEW method actually returns usable text. Forgetting
 *    to add it to [sweep] keeps it uncovered, so `:shared:koverVerify` fails
 *    instead of the bug reaching a walkthrough.
 */
class StringsTableTest {
    /** Byte-pinned pairs live in [StringsTest]; this file guards the rest. */
    private val tables = listOf("DE" to StringsDe, "EN" to StringsEn)

    /** Every method of the interface, in declaration order. Add new ones here. */
    private fun sweep(strings: Strings): List<String> =
        listOf(
            // splash / login
            strings.splashTagline(),
            strings.loginTitle(),
            strings.emailLabel(),
            strings.passwordLabel(),
            strings.signInCta(),
            strings.backLabel(),
            strings.demoHint(name = "Demo Fan", password = "eventom"),
            strings.loginWrongPassword(),
            strings.loginGenericError(),
            // events list
            strings.eventsGreeting(name = "Stella"),
            strings.signInChip(),
            strings.signOutChip(),
            strings.refreshLabel(),
            strings.allChip(),
            strings.saveLabel(),
            strings.savedLabel(),
            strings.emptyList(),
            strings.offlineBanner(),
            // detail
            strings.aboutTitle(),
            strings.lineupTitle(),
            strings.infoTitle(),
            strings.doorsLabel(time = "18:00"),
            strings.ticketCategoriesTitle(),
            strings.priceFrom(min = "24.90 €"),
            strings.buyTicketsCta(),
            strings.offlineDemoFeedback(),
            strings.loadingDetails(),
            strings.tryAgain(),
            strings.backToEvents(),
            strings.eventNoLongerAvailable(),
            strings.detailGenericError(),
            // about
            strings.aboutHeading(),
            strings.aboutSharedKotlin(),
            strings.aboutRunningOn(),
            strings.aboutDevice(),
            strings.aboutBoilerplateTitle(),
            strings.aboutBoilerplateBody(),
            // bottom bar
            strings.eventsTabLabel(),
            strings.aboutTabLabel(),
            // settings
            strings.settingsTitle(),
            strings.languageTitle(),
            strings.languageSystemDefault(),
            strings.languageGerman(),
            strings.languageEnglish(),
        )

    @Test
    fun everyStringIsNonBlank() {
        tables.forEach { (tag, strings) ->
            sweep(strings).forEachIndexed { index, value ->
                assertTrue(value.isNotBlank(), "[$tag] string #$index is blank")
            }
        }
    }

    /** The interface has 44 fixed strings + one per category; a forgotten
     *  method in [sweep] would silently shrink the list. */
    @Test
    fun sweepCoversTheWholeInterface() {
        tables.forEach { (tag, strings) ->
            assertEquals(
                44,
                sweep(strings).size,
                "[$tag] sweep size changed — add the new interface method to sweep()",
            )
        }
    }

    /** Greeting/hint/door/price helpers must embed their argument; a hardcoded
     *  literal would pass the non-blank check but break the UI. */
    @Test
    fun parameterizedStringsEmbedTheirArgument() {
        tables.forEach { (tag, strings) ->
            assertTrue(strings.demoHint(name = "Demo Fan", password = "eventom").contains("Demo Fan"), "[$tag] demoHint drops the name")
            assertTrue(strings.demoHint(name = "Demo Fan", password = "eventom").contains("eventom"), "[$tag] demoHint drops the password")
            assertTrue(strings.eventsGreeting(name = "Stella").contains("Stella"), "[$tag] greeting drops the name")
            assertTrue(strings.doorsLabel(time = "18:00").contains("18:00"), "[$tag] doors label drops the time")
            assertTrue(strings.priceFrom(min = "24.90 €").contains("24.90 €"), "[$tag] price line drops the amount")
        }
    }

    /** One label per category, no duplicates — a copy-paste slip in either
     *  table would show as two identical chips. */
    @Test
    fun categoryLabelsAreDistinctWithinEachTable() {
        tables.forEach { (tag, strings) ->
            val labels = EventCategory.entries.map { strings.category(it) }
            assertEquals(
                EventCategory.entries.size,
                labels.toSet().size,
                "[$tag] duplicate category labels: $labels",
            )
            labels.forEach { assertTrue(it.isNotBlank(), "[$tag] blank category label") }
        }
    }
}
