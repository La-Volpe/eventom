package com.evantom.data.fake

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

    val EVENTS_JSON: String = """
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

    private val EVENT_DETAILS = mapOf(
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
        """.trimIndent(),
    )

    fun eventDetailJson(id: Long): String? = EVENT_DETAILS[id]

    val USERS_JSON: String = """
        [
          {
            "email": "demo@evantom.dev",
            "password": "demo1234",
            "name": "Demo Fan"
          },
          {
            "email": "stella@evantom.dev",
            "password": "sommer1",
            "name": "Stella Sommer"
          }
        ]
    """.trimIndent()
}