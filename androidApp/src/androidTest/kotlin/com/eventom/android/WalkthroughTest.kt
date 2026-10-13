package com.eventom.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.eventom.di.SharedKoin
import com.eventom.language.AppLanguage
import org.junit.Rule
import org.junit.Test

/**
 * Connected Android walkthrough (plan T13, review G1): splash strings →
 * login form (demo creds via the UI) → events list (byte-identical QA
 * strings) → detail (byte-identical detail strings) → back → sign out →
 * log back in.
 *
 * Bilingual 3/4 phase 2: the SAME journey then switches the language
 * through the SETTINGS UI (English via the ids languageOption-*) and
 * asserts the English byte set live — tab labels, greeting, chips, card
 * titles — before switching back to Deutsch and re-asserting the German
 * set. The test-override pin is cleared before phase 2 (the hook wins
 * over user choices by design; the walk must exercise the real settings
 * flow), normalizing the persisted choice to DE deterministically first.
 *
 * Discipline copied from the iOS suite: existence polls with generous
 * timeouts instead of one-shot asserts, one journey per app process, no
 * navigation assumptions between steps. The splash holds the production
 * timing (1.5s) — the journey catches both splash-strings presence up
 * front and settles through the hold with waits; production timing is
 * never altered.
 */
class WalkthroughTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /**
     * Polls until a node with [text] exists (multi-match safe: the events
     * list shows several cards, so labels like "☆ Save" appear more than
     * once — the FIRST occurrence is asserted).
     */
    private fun waitUntilText(text: String, timeoutMs: Long = 10_000) {
        compose.waitUntil(timeoutMillis = timeoutMs) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText(text)[0].assertIsDisplayed()
    }

    /** Presence-only polling: horizontal-scroll rows (chips) may sit
     *  off-viewport but their semantics nodes still exist. */
    private fun waitUntilTextExists(text: String, timeoutMs: Long = 10_000) {
        compose.waitUntil(timeoutMillis = timeoutMs) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText(text)[0].assertExists("expected $text")
    }

    @Test
    fun walkthroughSplashToEventsDetailAndBackToLogin() {
        // Bilingual 2/4: pin the content language through the shared test
        // hook — this walk asserts the byte-identical German QA set, while
        // the emulator device locale is en-US (SYSTEM would seed EN).
        SharedKoin.languageSetting().setTestOverride("de")
        // Splash strings — caught up front, inside the 1.5s window.
        compose.waitUntil(timeoutMillis = 3_000) {
            compose.onAllNodesWithText("EVENTOM").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Sign in to see your events").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("EVENTOM").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Tickets für Live-Entertainment").assertIsDisplayed()
        }

        // Login form; production splash timing already elapsed by here.
        waitUntilText("Sign in to see your events")
        waitUntilText("E-mail", 3_000)
        waitUntilText("Demo account: demo@eventom.dev · demo1234", 3_000)
        compose.onNodeWithText("E-mail").performTextInput("demo@eventom.dev")
        compose.onNodeWithText("Password").performTextInput("demo1234")
        compose.onNodeWithText("Sign in").performClick()

        // Events list with byte-identical QA strings.
        waitUntilText("Hi, Demo Fan")
        waitUntilText("Konzerte", 6_000)
        waitUntilText("Lachstoff — Abendprogramm", 6_000)
        waitUntilText("ab 39.90 €", 6_000)

        // Save the first card (state survives the detail round trip).
        waitUntilText("☆ Save", 6_000)
        compose.onAllNodesWithText("☆ Save")[0].performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("★ Saved").fetchSemanticsNodes().isNotEmpty()
        }

        // Detail (byte-identical detail strings), CTA + Offline-Demo, back.
        compose.onAllNodesWithText("Lachstoff — Abendprogramm")[0].performClick()
        waitUntilText("About this event", 10_000)
        waitUntilText("Doors: 17:15", 6_000)
        waitUntilText("Ticket categories", 6_000)
        waitUntilText("Tickets kaufen", 6_000)
        compose.onNodeWithText("Tickets kaufen").performClick()
        waitUntilText("Offline-Demo", 5_000)
        compose.onNodeWithText("About this event").assertIsDisplayed() // screen stays
        compose.onNodeWithContentDescription("Back").performClick()

        // Back-stack shape after detail → back: events destination still
        // composed (greeting, card, bottom-bar tabs) — nav-pop evidence.
        waitUntilText("Hi, Demo Fan", 10_000)
        waitUntilText("★ Saved", 6_000)
        waitUntilText("Lachstoff — Abendprogramm", 6_000)
        waitUntilText("About", 6_000) // bottom-bar tab missing would mean a swapped stack
        compose.onNodeWithText("About this event").assertDoesNotExist() // detail popped off

        // Sign out → Login → sign back in (state resets, credentials work).
        waitUntilText("Sign out", 6_000)
        compose.onNodeWithText("Sign out").performClick()
        waitUntilText("Sign in to see your events", 10_000)
        compose.onNodeWithText("E-mail").performTextInput("stella@eventom.dev")
        compose.onNodeWithText("Password").performTextInput("sommer1")
        compose.onNodeWithText("Sign in").performClick()
        waitUntilText("Hi, Stella Sommer")

        // No stale detail destination left in the back stack: the detail
        // content must not be composed anywhere on (or behind) the login.
        compose.onNodeWithText("About this event").assertDoesNotExist()
        compose.onNodeWithText("Tickets kaufen").assertDoesNotExist()

        // --- Bilingual 3/4 phase 2: live language switch via Settings ------
        // Clear the pin so real user choices take over, and normalize the
        // persisted choice to DE (a fresh state would seed SYSTEM → EN on
        // this en-US emulator).
        SharedKoin.languageSetting().setTestOverride(null)
        SharedKoin.languageSetting().choose(AppLanguage.DE)

        // Settings tab (German label).
        compose.onNodeWithText("Einstellungen").performClick()
        waitUntilText("Sprache", 6_000)

        // Switch to ENGLISH through the settings row (semantics id).
        compose.onNodeWithContentDescription("languageOption-en").performClick()

        // English byte set, live: tab label, settings headings, chips.
        waitUntilText("Settings", 6_000) // bottom-bar tab label
        waitUntilText("Language", 6_000) // settings section title
        waitUntilText("System default", 6_000)

        // Events tab re-renders English instantly (the signed-in user at
        // this point is Stella — phase 1 ended re-signed-in as her).
        compose.onNodeWithText("Events").performClick()
        waitUntilText("Hi, Stella Sommer", 6_000)
        waitUntilTextExists("Concerts", 6_000)
        waitUntilTextExists("Musicals & Shows", 6_000)
        waitUntilTextExists("Theater & Culture", 6_000)

        // …unchanged QA contract where languages match byte-for-byte.
        waitUntilText("Laughter Fuel — Evening Show", 8_000) // auto-refresh family
        waitUntilText("ab 39.90 €", 6_000)
        waitUntilText("☆ Save", 6_000)

        // And the saved flag survived the payload-family switch.
        waitUntilText("★ Saved", 6_000)

        // Back through the picker to Deutsch; German set re-asserted.
        compose.onNodeWithText("Settings").performClick()
        waitUntilText("Language", 6_000)
        compose.onNodeWithContentDescription("languageOption-de").performClick()
        waitUntilText("Sprache", 6_000)
        waitUntilText("Systemstandard", 6_000)

        compose.onNodeWithText("Events").performClick()
        waitUntilText("Hi, Stella Sommer", 6_000)
        waitUntilTextExists("Konzerte", 6_000)
        waitUntilTextExists("Musical & Show", 6_000)
        waitUntilTextExists("Theater & Kultur", 6_000)
        waitUntilText("Lachstoff — Abendprogramm", 8_000)
        waitUntilText("★ Saved", 6_000)
    }
}