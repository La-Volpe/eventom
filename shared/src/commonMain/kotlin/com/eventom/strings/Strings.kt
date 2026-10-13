package com.eventom.strings

import com.eventom.domain.model.EventCategory
import com.eventom.language.EffectiveLanguage

/**
 * Every user-visible UI string, bilingual (owner-locked decision 3: strings
 * live in shared). [StringsDe] is the byte-identical set shipped today
 * (walkthroughs pin it); [StringsEn] is the English translation — natural
 * English, "EVENTOM" never translated, German venue/city proper names stay.
 *
 * Consumer wiring (Android/iOS screens + view models) happens in task 2;
 * per-platform category maps already delegate here (see PosterMap.*).
 */
interface Strings {
    // splash
    fun splashTagline(): String

    // login
    fun loginTitle(): String
    fun emailLabel(): String
    fun passwordLabel(): String
    fun signInCta(): String
    fun backLabel(): String
    fun demoHint(name: String, password: String): String

    // login errors (produced by the shared AuthViewModel)
    fun loginWrongPassword(): String
    fun loginGenericError(): String

    // events list
    fun eventsGreeting(name: String): String
    fun signInChip(): String
    fun signOutChip(): String
    fun refreshLabel(): String
    fun allChip(): String
    fun saveLabel(): String
    fun savedLabel(): String
    fun emptyList(): String
    fun offlineBanner(): String

    // detail
    fun aboutTitle(): String
    fun lineupTitle(): String
    fun infoTitle(): String
    fun doorsLabel(time: String): String
    fun ticketCategoriesTitle(): String
    fun buyTicketsCta(): String
    fun offlineDemoFeedback(): String
    fun loadingDetails(): String
    fun tryAgain(): String
    fun backToEvents(): String
    fun eventNoLongerAvailable(): String
    fun detailGenericError(): String

    // about screen
    fun aboutHeading(): String
    fun aboutSharedKotlin(): String
    fun aboutRunningOn(): String
    fun aboutDevice(): String
    fun aboutBoilerplateTitle(): String
    fun aboutBoilerplateBody(): String

    // bottom-bar tab labels (both platforms render these identically today,
    // so DE keeps the shipped bytes)
    fun eventsTabLabel(): String
    fun aboutTabLabel(): String

    // settings (task 2 wires the UI)
    fun settingsTitle(): String
    fun languageTitle(): String
    fun languageSystemDefault(): String
    fun languageGerman(): String
    fun languageEnglish(): String

    /** Category chips/labels — single source for wire-code↔label. */
    fun category(category: EventCategory): String
}

/** German = today's shipped set, byte-identical. */
object StringsDe : Strings {
    override fun splashTagline(): String = "Tickets für Live-Entertainment"
    override fun loginTitle(): String = "Sign in to see your events"
    override fun emailLabel(): String = "E-mail"
    override fun passwordLabel(): String = "Password"
    override fun signInCta(): String = "Sign in"
    override fun backLabel(): String = "Back"
    override fun demoHint(name: String, password: String): String = "Demo account: $name · $password"
    override fun loginWrongPassword(): String = "Wrong e-mail or password"
    override fun loginGenericError(): String = "Sign in failed — check your connection and try again"
    override fun eventsGreeting(name: String): String = "Hi, $name"
    override fun signInChip(): String = "Sign in"
    override fun signOutChip(): String = "Sign out"
    override fun refreshLabel(): String = "Refresh"
    override fun allChip(): String = "All"
    override fun saveLabel(): String = "☆ Save"
    override fun savedLabel(): String = "★ Saved"
    override fun emptyList(): String = "Nothing here yet — hit Refresh"
    override fun offlineBanner(): String = "Offline — showing your last loaded events"
    override fun aboutTitle(): String = "About this event"
    override fun lineupTitle(): String = "Line-up"
    override fun infoTitle(): String = "Info"
    override fun doorsLabel(time: String): String = "Doors: $time"
    override fun ticketCategoriesTitle(): String = "Ticket categories"
    override fun buyTicketsCta(): String = "Tickets kaufen"
    override fun offlineDemoFeedback(): String = "Offline-Demo"
    override fun loadingDetails(): String = "Loading details…"
    override fun tryAgain(): String = "Try again"
    override fun backToEvents(): String = "Back to events"
    override fun eventNoLongerAvailable(): String = "This event is no longer available"
    override fun detailGenericError(): String = "Couldn't load details — check your connection and try again"
    override fun aboutHeading(): String = "About"
    override fun aboutSharedKotlin(): String = "Shared Kotlin"
    override fun aboutRunningOn(): String = "Running on"
    override fun aboutDevice(): String = "Device"
    override fun aboutBoilerplateTitle(): String = "Boilerplate layout"
    override fun aboutBoilerplateBody(): String =
        "All logic lives in the shared Kotlin module: domain, data (Ktor), view models and Koin wiring. " +
            "Both apps only render shared state natively."
    override fun eventsTabLabel(): String = "Events"
    override fun aboutTabLabel(): String = "About"
    override fun settingsTitle(): String = "Einstellungen"
    override fun languageTitle(): String = "Sprache"
    override fun languageSystemDefault(): String = "Systemstandard"
    override fun languageGerman(): String = "Deutsch"
    override fun languageEnglish(): String = "Englisch"
    override fun category(category: EventCategory): String = when (category) {
        // LOAD-BEARING (generate_posters.py): EventCategory.CONCERTS -> "Konzerte"
        EventCategory.CONCERTS -> "Konzerte"
        // LOAD-BEARING (generate_posters.py): EventCategory.MUSICAL_SHOW -> "Musical & Show"
        EventCategory.MUSICAL_SHOW -> "Musical & Show"
        // LOAD-BEARING (generate_posters.py): EventCategory.SPORTS -> "Sport"
        EventCategory.SPORTS -> "Sport"
        // LOAD-BEARING (generate_posters.py): EventCategory.COMEDY -> "Comedy & Humor"
        EventCategory.COMEDY -> "Comedy & Humor"
        // LOAD-BEARING (generate_posters.py): EventCategory.THEATER -> "Theater & Kultur"
        EventCategory.THEATER -> "Theater & Kultur"
        // LOAD-BEARING (generate_posters.py): EventCategory.LEISURE -> "Freizeit"
        EventCategory.LEISURE -> "Freizeit"
    }
}

/** English translation (natural English; EVENTOM/venues/cities untouched). */
object StringsEn : Strings {
    override fun splashTagline(): String = "Tickets for live entertainment"
    override fun loginTitle(): String = "Sign in to see your events"
    override fun emailLabel(): String = "E-mail"
    override fun passwordLabel(): String = "Password"
    override fun signInCta(): String = "Sign in"
    override fun backLabel(): String = "Back"
    override fun demoHint(name: String, password: String): String = "Demo account: $name · $password"
    override fun loginWrongPassword(): String = "Wrong e-mail or password"
    override fun loginGenericError(): String = "Sign in failed — check your connection and try again"
    override fun eventsGreeting(name: String): String = "Hi, $name"
    override fun signInChip(): String = "Sign in"
    override fun signOutChip(): String = "Sign out"
    override fun refreshLabel(): String = "Refresh"
    override fun allChip(): String = "All"
    override fun saveLabel(): String = "☆ Save"
    override fun savedLabel(): String = "★ Saved"
    override fun emptyList(): String = "Nothing here yet — hit Refresh"
    override fun offlineBanner(): String = "Offline — showing your last loaded events"
    override fun aboutTitle(): String = "About this event"
    override fun lineupTitle(): String = "Line-up"
    override fun infoTitle(): String = "Info"
    override fun doorsLabel(time: String): String = "Doors: $time"
    override fun ticketCategoriesTitle(): String = "Ticket categories"
    override fun buyTicketsCta(): String = "Buy tickets"
    override fun offlineDemoFeedback(): String = "Offline demo"
    override fun loadingDetails(): String = "Loading details…"
    override fun tryAgain(): String = "Try again"
    override fun backToEvents(): String = "Back to events"
    override fun eventNoLongerAvailable(): String = "This event is no longer available"
    override fun detailGenericError(): String = "Couldn't load details — check your connection and try again"
    override fun aboutHeading(): String = "About"
    override fun aboutSharedKotlin(): String = "Shared Kotlin"
    override fun aboutRunningOn(): String = "Running on"
    override fun aboutDevice(): String = "Device"
    override fun aboutBoilerplateTitle(): String = "Boilerplate layout"
    override fun aboutBoilerplateBody(): String =
        "All logic lives in the shared Kotlin module: domain, data (Ktor), view models and Koin wiring. " +
            "Both apps only render shared state natively."
    override fun eventsTabLabel(): String = "Events"
    override fun aboutTabLabel(): String = "About"
    override fun settingsTitle(): String = "Settings"
    override fun languageTitle(): String = "Language"
    override fun languageSystemDefault(): String = "System default"
    override fun languageGerman(): String = "Deutsch"
    override fun languageEnglish(): String = "English"
    override fun category(category: EventCategory): String = when (category) {
        EventCategory.CONCERTS -> "Concerts"
        EventCategory.MUSICAL_SHOW -> "Musicals & Shows"
        EventCategory.SPORTS -> "Sport"
        EventCategory.COMEDY -> "Comedy & Humor"
        EventCategory.THEATER -> "Theater & Culture"
        EventCategory.LEISURE -> "Leisure"
    }
}

/** Accessor per house style (concrete, no generics). */
fun strings(language: EffectiveLanguage): Strings = when (language) {
    EffectiveLanguage.DE -> StringsDe
    EffectiveLanguage.EN -> StringsEn
}