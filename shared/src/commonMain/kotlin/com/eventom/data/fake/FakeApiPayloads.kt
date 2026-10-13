package com.eventom.data.fake

import com.eventom.language.EffectiveLanguage

/**
 * Runtime payloads for the offline fake backend, mirroring the canonical
 * JSON fixtures under `src/commonMain/resources/api/` — kept in sync by
 * `FakeApiPayloadParityTest` (which fails the build if either drifts).
 *
 * Why compiled-in strings instead of reading the resource files at runtime?
 * Kotlin/Gradle multiplatform has no generic resource-reading API for native
 * targets (KGP 2.4.20 has none), Android's ClassLoader cannot read files
 * packed into an APK, and moko/compose-resources are out of bounds (no new
 * dependencies this milestone). Embedding is deterministic on every target;
 * the resource files remain the editable source of truth in the repo.
 *
 * When a real backend exists: delete `BackendConfig`, the fake payloads and
 * the resource fixtures together.
 */
object FakeApiPayloads {

    val EVENTS_JSON_DE: String = """
        [
          {
            "id": 1,
            "imageRef": "poster_event_1",
            "title": "Aurora Nights — Sommer Tour 2027",
            "category": "concerts",
            "dateTime": "2027-06-12T19:30:00Z",
            "city": "Berlin",
            "venue": "Halle am Ufer",
            "priceFrom": 59.90,
            "shortPitch": "Indie-Pop unter freiem Himmel — die Tour des Sommers."
          },
          {
            "id": 2,
            "imageRef": "poster_event_2",
            "title": "Das Geheimnis der Masken",
            "category": "musical-show",
            "dateTime": "2027-03-19T20:00:00Z",
            "city": "Hamburg",
            "venue": "Operettenpalast",
            "priceFrom": 79.50,
            "shortPitch": "Große Gefühle, große Stimmen: das Mysteriösmusical der Saison."
          },
          {
            "id": 3,
            "imageRef": "poster_event_3",
            "title": "Stadtderby: Alster Adler gegen Rhein Löwen",
            "category": "sports",
            "dateTime": "2027-02-05T15:30:00Z",
            "city": "Hamburg",
            "venue": "Eishalle Alsterdamm",
            "priceFrom": 24.90,
            "shortPitch": "Das Eishockey-Halbfinale der Stadtliga — Stehplatz-Atmosphäre garantiert."
          },
          {
            "id": 4,
            "imageRef": "poster_event_4",
            "title": "Lachstoff — Abendprogramm",
            "category": "comedy",
            "dateTime": "2027-01-23T18:00:00Z",
            "city": "München",
            "venue": "Lusthaus am Ring",
            "priceFrom": 39.90,
            "shortPitch": "Zwei Stunden Stand-up, Improvisation und eine offene Bühne."
          },
          {
            "id": 5,
            "imageRef": "poster_event_5",
            "title": "Hamlet reloaded",
            "category": "theater",
            "dateTime": "2027-04-08T19:00:00Z",
            "city": "Köln",
            "venue": "Schauspielhaus Altstadt",
            "priceFrom": 34.00,
            "shortPitch": "Shakespeares Klassiker in einer rasenden Doppel-Solo-Inszenierung."
          },
          {
            "id": 6,
            "imageRef": "poster_event_6",
            "title": "Frühlingsfest am Stadtpark",
            "category": "leisure",
            "dateTime": "2027-04-24T11:00:00Z",
            "city": "Leipzig",
            "venue": "Festwiese Nord",
            "priceFrom": 8.50,
            "shortPitch": "Fahrgeschäfte, Handwerkermarkt und Familienbühne am ersten Maiwochenende."
          }
        ]
        """.trimIndent()

    val EVENTS_JSON_EN: String = """
        [
          {
            "id": 1,
            "title": "Aurora Nights — Summer Tour 2027",
            "category": "concerts",
            "dateTime": "2027-06-12T19:30:00Z",
            "city": "Berlin",
            "venue": "Halle am Ufer",
            "priceFrom": 59.90,
            "imageRef": "poster_event_1",
            "shortPitch": "Indie pop under the open sky — the tour of the summer."
          },
          {
            "id": 2,
            "title": "The Secret of the Masks",
            "category": "musical-show",
            "dateTime": "2027-03-19T20:00:00Z",
            "city": "Hamburg",
            "venue": "Operettenpalast",
            "priceFrom": 79.50,
            "imageRef": "poster_event_2",
            "shortPitch": "Big feelings, big voices: the season's mystery musical."
          },
          {
            "id": 3,
            "title": "City derby: Alster Eagles vs. Rhine Lions",
            "category": "sports",
            "dateTime": "2027-02-05T15:30:00Z",
            "city": "Hamburg",
            "venue": "Eishalle Alsterdamm",
            "priceFrom": 24.90,
            "imageRef": "poster_event_3",
            "shortPitch": "The city-league ice-hockey semi-final — standing-room atmosphere guaranteed."
          },
          {
            "id": 4,
            "title": "Laughter Fuel — Evening Show",
            "category": "comedy",
            "dateTime": "2027-01-23T18:00:00Z",
            "city": "München",
            "venue": "Lusthaus am Ring",
            "priceFrom": 39.90,
            "imageRef": "poster_event_4",
            "shortPitch": "Two hours of stand-up, improv and an open mic."
          },
          {
            "id": 5,
            "title": "Hamlet Reloaded",
            "category": "theater",
            "dateTime": "2027-04-08T19:00:00Z",
            "city": "Köln",
            "venue": "Schauspielhaus Altstadt",
            "priceFrom": 34.00,
            "imageRef": "poster_event_5",
            "shortPitch": "Shakespeare's classic as a fast-paced double-solo staging."
          },
          {
            "id": 6,
            "title": "Spring Festival at the City Park",
            "category": "leisure",
            "dateTime": "2027-04-24T11:00:00Z",
            "city": "Leipzig",
            "venue": "Festwiese Nord",
            "priceFrom": 8.50,
            "imageRef": "poster_event_6",
            "shortPitch": "Fairground rides, a craft market and a family stage on the first May weekend."
          }
        ]
        """.trimIndent()

    val EVENT_DETAILS_DE = mapOf(
        1L to """
            {
              "id": 1,
              "longDescription": "Aurora Nights kommen mit ihrer Sommer Tour 2027 zurück nach Berlin. Zwischen Lichtinstallationen und Open-Air-Sommerabenteuer spielt die Band die komplette neue Platte 'Lichterfest' samt allen Radiohits. Der Sound: treibende Synths, nordische Melancholie und ein Publikum, das bis in die Nacht bleibt.",
              "lineup": [
                "Aurora Nights",
                "Support: Nordlicht Kollektiv",
                "Support: Fjord & Feuer"
              ],
              "doorsTime": "18:00",
              "ticketCategories": [
                { "name": "Kategorie 1 — Best Seats", "price": 119.90 },
                { "name": "Kategorie 2 — Parkett", "price": 89.90 },
                { "name": "Kategorie 3 — Stehplatz", "price": 59.90 }
              ],
              "ageInfo": "Fsk ab 6 Jahren"
            }
        """.trimIndent(),
        2L to """
            {
              "id": 2,
              "longDescription": "Ein Maskenball, ein verschwundenes Testament und eine Stimme, die über drei Galas hinausreicht: 'Das Geheimnis der Masken' verbindet klassische Musicaldramaturgie mit moderner Bühnentechnik. 24 Sänger:innen, ein komplettes Orchester und Masken, die auf der Bühne ihre Besitzer wechseln.",
              "lineup": [
                "Original Cast Hamburg",
                "Orchester der Operettenpalast"
              ],
              "doorsTime": "17:30",
              "ticketCategories": [
                { "name": "Premium-Paket inkl. Prosecco", "price": 149.00 },
                { "name": "Kategorie 1 — Loge", "price": 109.50 },
                { "name": "Kategorie 2 — Rang", "price": 79.50 }
              ],
              "ageInfo": "Fsk ab 12 Jahren"
            }
        """.trimIndent(),
        3L to """
            {
              "id": 3,
              "longDescription": "Wenn die Alster Adler auf die Rhein Löwen treffen, zählt jede Sekunde: Das Halbfinale der Stadtliga entscheidet, wer im März um die Meisterschaft spielt. Eishockey in Hamburg heißt Vollkontakt, Vollgesang und Stehplatz-Atmosphäre auf ganzer Linie.",
              "lineup": [
                "Alster Adler Hamburg",
                "Rhein Löwen Köln"
              ],
              "doorsTime": "14:00",
              "ticketCategories": [
                { "name": "Kategorie 1 — Sitzplatz Tribüne", "price": 44.90 },
                { "name": "Kategorie 2 — Stehplatz", "price": 24.90 }
              ],
              "ageInfo": "Keine Altersbeschränkung"
            }
        """.trimIndent(),
        4L to """
            {
              "id": 4,
              "longDescription": "Lachstoff ist Hamburgs Antwort auf den grauen Alltag: zwei Stunden Stand-up, Improvisation und eine offene Bühne, auf der auch unbekannte Namen ihre fünf Minuten bekommen. Das Programm wechselt jeden Abend — mitkommen lohnt sich zweimal.",
              "lineup": [
                "Host: Miriam Kaltbach",
                "Headliner: Jonas Zwiebelt",
                "Open Mic"
              ],
              "doorsTime": "17:15",
              "ticketCategories": [
                { "name": "Kategorie 1 — Tisch nahe der Bühne", "price": 54.90 },
                { "name": "Kategorie 2 — Freie Platzwahl", "price": 39.90 }
              ],
              "ageInfo": "Fsk ab 18 Jahren"
            }
        """.trimIndent(),
        5L to """
            {
              "id": 5,
              "longDescription": "Zwei Schauspieler:innen, ein Bühnenbild, 90 Minuten: 'Hamlet reloaded' verdichtet Shakespeares Klassiker zur Doppel-Solo-Inszenierung, in der jede:n Darsteller:in alle Rollen spielt. Regie: Ines Falkenrath. Sein oder nicht — hier wird beides gespielt.",
              "lineup": [
                "Inszenierung: Ines Falkenrath",
                "Darstellung: Timo Brandt & Ada Silberstein"
              ],
              "doorsTime": "18:15",
              "ticketCategories": [
                { "name": "Kategorie 1 — Reihe 1–5", "price": 54.00 },
                { "name": "Kategorie 2 — Parkett", "price": 34.00 }
              ],
              "ageInfo": "Fsk ab 14 Jahren"
            }
        """.trimIndent(),
        6L to """
            {
              "id": 6,
              "longDescription": "Am ersten Maiwochenende verwandelt sich die Festwiese Nord in einen Frühlingsmarkt: über 40 Handwerker:innen, drei Bühnen für Familienprogramm und Fahrgeschäfte, die seit drei Generationen dazugehören. Der Eintritt kostet einmalig 8,50 € — alle Bühnen inklusive.",
              "lineup": [
                "Familienbühne Nord",
                "Volksmusikensemble Stadtpark",
                "Zirkus Kleine Wolke"
              ],
              "doorsTime": "10:30",
              "ticketCategories": [
                { "name": "Eintritt Tageskasse", "price": 8.50 }
              ],
              "ageInfo": "Keine Altersbeschränkung"
            }
        """.trimIndent()
    )

    val EVENT_DETAILS_EN = mapOf(
        1L to """
            {
              "id": 1,
              "longDescription": "Aurora Nights return to Berlin for their Summer Tour 2027. Between light installations and an open-air summer adventure, the band plays the complete new album 'Lichterfest' along with all the radio hits. The sound: driving synths, Nordic melancholy and an audience that stays until midnight.",
              "lineup": [
                "Aurora Nights",
                "Support: Nordlicht Kollektiv",
                "Support: Fjord & Feuer"
              ],
              "doorsTime": "18:00",
              "ticketCategories": [
                { "name": "Category 1 — Best Seats", "price": 119.90 },
                { "name": "Category 2 — Parquet", "price": 89.90 },
                { "name": "Category 3 — Standing", "price": 59.90 }
              ],
              "ageInfo": "Age 6 and up"
            }
        """.trimIndent(),
        2L to """
            {
              "id": 2,
              "longDescription": "A masked ball, a missing will and a voice that carries across three gala nights: 'The Secret of the Masks' combines classic musical dramaturgy with modern stage technology. 24 singers, a full orchestra and masks that change owners on stage.",
              "lineup": [
                "Original Cast Hamburg",
                "Orchester der Operettenpalast"
              ],
              "doorsTime": "17:30",
              "ticketCategories": [
                { "name": "Premium package incl. prosecco", "price": 149.00 },
                { "name": "Category 1 — Box seats", "price": 109.50 },
                { "name": "Category 2 — Balcony", "price": 79.50 }
              ],
              "ageInfo": "Age 12 and up"
            }
        """.trimIndent(),
        3L to """
            {
              "id": 3,
              "longDescription": "When the Alster Eagles meet the Rhine Lions, every second counts: the city-league semi-final decides who plays for the championship in March. Ice hockey in Hamburg means full contact, full song and standing-room atmosphere from start to finish.",
              "lineup": [
                "Alster Adler Hamburg",
                "Rhein Löwen Köln"
              ],
              "doorsTime": "14:00",
              "ticketCategories": [
                { "name": "Category 1 — Seated stand", "price": 44.90 },
                { "name": "Category 2 — Standing", "price": 24.90 }
              ],
              "ageInfo": "No age restriction"
            }
        """.trimIndent(),
        4L to """
            {
              "id": 4,
              "longDescription": "Laughter Fuel is Hamburg's answer to the grey everyday: two hours of stand-up, improvisation and an open stage where unknown names also get their five minutes. The program changes every evening — coming twice is worth it.",
              "lineup": [
                "Host: Miriam Kaltbach",
                "Headliner: Jonas Zwiebelt",
                "Open Mic"
              ],
              "doorsTime": "17:15",
              "ticketCategories": [
                { "name": "Category 1 — table near the stage", "price": 54.90 },
                { "name": "Category 2 — Free seating", "price": 39.90 }
              ],
              "ageInfo": "Age 18 and up"
            }
        """.trimIndent(),
        5L to """
            {
              "id": 5,
              "longDescription": "Two actors, one set, 90 minutes: 'Hamlet Reloaded' condenses Shakespeare's classic into a double-solo staging where each performer plays every role. Direction: Ines Falkenrath. To be or not — here both are played.",
              "lineup": [
                "Direction: Ines Falkenrath",
                "Starring: Timo Brandt & Ada Silberstein"
              ],
              "doorsTime": "18:15",
              "ticketCategories": [
                { "name": "Category 1 — Rows 1–5", "price": 54.00 },
                { "name": "Category 2 — Stalls", "price": 34.00 }
              ],
              "ageInfo": "Age 14 and up"
            }
        """.trimIndent(),
        6L to """
            {
              "id": 6,
              "longDescription": "On the first May weekend the Festwiese Nord turns into a spring market: over 40 artisans, three stages of family programming and fairground rides that have belonged here for three generations. Admission is a single 8.50 € — all stages included.",
              "lineup": [
                "Familienbühne Nord",
                "Volksmusikensemble Stadtpark",
                "Zirkus Kleine Wolke"
              ],
              "doorsTime": "10:30",
              "ticketCategories": [
                { "name": "Admission — box office", "price": 8.50 }
              ],
              "ageInfo": "No age restriction"
            }
        """.trimIndent()
    )

    /** Serial demo users: SHA-256 hex passwords (AGENTS.md #18). */
    @kotlinx.serialization.Serializable
    internal data class FakeUserAccount(
        val email: String,
        /** SHA-256 hex of the demo password; the plaintext stays a UI hint only. */
        val passwordHash: String,
        val name: String,
    )

    /** Decodes the embedded users for the credential check (codec owned here). */
    internal fun accounts(json: kotlinx.serialization.json.Json): List<FakeUserAccount> =
        json.decodeFromString<List<FakeUserAccount>>(USERS_JSON)

    /** Serves the payload family the Accept-Language request resolves to. */
    internal fun eventsJson(language: EffectiveLanguage): String = when (language) {
        EffectiveLanguage.DE -> EVENTS_JSON_DE
        EffectiveLanguage.EN -> EVENTS_JSON_EN
    }

    internal fun eventDetailJson(id: Long, language: EffectiveLanguage): String? =
        detailByLanguage[language]?.get(id)

    private val detailByLanguage: Map<EffectiveLanguage, Map<Long, String>> = mapOf(
        EffectiveLanguage.DE to EVENT_DETAILS_DE,
        EffectiveLanguage.EN to EVENT_DETAILS_EN,
    )

    val USERS_JSON: String = """
        [
          {
            "email": "demo@eventom.dev",
            "passwordHash": "0ead2060b65992dca4769af601a1b3a35ef38cfad2c2c465bb160ea764157c5d",
            "name": "Demo Fan"
          },
          {
            "email": "stella@eventom.dev",
            "passwordHash": "b638c676930197c2526766aa7b1b2b696f0aae70a5c3107671c9392395e4e222",
            "name": "Stella Sommer"
          }
        ]
    """.trimIndent()
}
