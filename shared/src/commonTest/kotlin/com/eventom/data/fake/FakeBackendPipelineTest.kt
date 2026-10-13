package com.eventom.data.fake

import com.eventom.domain.repository.EventDetailRemoteDataSource
import com.eventom.data.remote.EventRemoteDataSource
import com.eventom.domain.repository.AuthRemoteDataSource
import com.eventom.data.remote.KtorAuthRemoteDataSource
import com.eventom.data.remote.KtorEventDetailRemoteDataSource
import com.eventom.data.remote.KtorEventRemoteDataSource
import com.eventom.domain.model.EventCategory
import com.eventom.domain.model.EventDetail
import com.eventom.domain.model.AuthUser
import com.eventom.language.EffectiveLanguage
import com.eventom.data.remote.EventDto
import com.eventom.data.remote.toDomain
import com.eventom.data.remote.API_HOST
import com.eventom.data.remote.API_BASE_PATH
import io.ktor.http.HttpHeaders
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.call.body
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Exercises the fake backend the way production does: a real [HttpClient]
 * with ContentNegotiation (mirroring `dataModule`) over the MockEngine, every
 * call going through the full serialization pipeline.
 */
class FakeBackendPipelineTest {

    private val client = HttpClient(fakeBackendEngine()) {
        // Same configuration as dataModule — keep both in sync.
        expectSuccess = true
        install(ContentNegotiation) { json() }
    }

    private val eventSource = KtorEventRemoteDataSource(client)
    private val detailSource: EventDetailRemoteDataSource = KtorEventDetailRemoteDataSource(client)
    private val authSource: AuthRemoteDataSource = KtorAuthRemoteDataSource(client)

    @Test
    fun listPayloadParsesThroughFullPipeline() = runTest {
        val events = eventSource.fetchEvents()

        assertEquals(6, events.size)
        val first = events.first()
        assertEquals(1L, first.id)
        assertEquals("Aurora Nights — Sommer Tour 2027", first.title)
        assertEquals(EventCategory.CONCERTS, first.category)
        assertEquals(Instant.parse("2027-06-12T19:30:00Z"), first.startTime)
        assertEquals("Berlin", first.city)
        assertEquals("Halle am Ufer", first.venue)
        assertEquals(59.90, first.priceFrom!!)
        assertTrue(first.summary.isNotEmpty())
        assertEquals("poster_event_1", first.imageRef)
    }

    @Test
    fun detailPayloadParsesThroughFullPipeline() = runTest {
        val detail: EventDetail = detailSource.fetchEventDetail(id = 2)

        assertEquals(2L, detail.id)
        assertEquals("17:30", detail.doorsTime)
        assertEquals(listOf("Original Cast Hamburg", "Orchester der Operettenpalast"), detail.lineup)
        assertTrue(detail.longSummary.startsWith("Ein Maskenball"))
        assertEquals(3, detail.ticketCategories.size)
        assertEquals(149.00, detail.ticketCategories.first().price)
        assertEquals("Fsk ab 12 Jahren", detail.ageInfo)
    }

    @Test
    fun unknownDetailIdServesNotFoundError() = runTest {
        // expectSuccess turns 404 into ClientRequestException (set in dataModule).
        assertFailsWith<io.ktor.client.plugins.ClientRequestException> {
            detailSource.fetchEventDetail(id = 99)
        }
    }

    @Test
    fun demoLoginReturnsUser() = runTest {
        // Documented demo account from users.json — usable by the login screen (M2).
        val user: AuthUser = authSource.login(email = "demo@eventom.dev", password = "demo1234")

        assertEquals("Demo Fan", user.name)
        assertEquals("demo@eventom.dev", user.email)
    }

    @Test
    fun secondDemoAccountWorks() = runTest {
        val user = authSource.login(email = "stella@eventom.dev", password = "sommer1")

        assertEquals("Stella Sommer", user.name)
    }

    @Test
    fun wrongPasswordIsRejected() = runTest {
        assertFailsWith<io.ktor.client.plugins.ClientRequestException> {
            authSource.login(email = "demo@eventom.dev", password = "hunter2")
        }
    }

    // MARK: - language routing (bilingual phase)

    /** Client variant adding an Accept-Language header to every request. */
    private fun clientAccepting(header: String?): HttpClient = HttpClient(fakeBackendEngine()) {
        // allow non-2xx bodies to flow into the assertions directly
        expectSuccess = true
        install(ContentNegotiation) { json() }
        if (header != null) {
            defaultRequest {
                headers.append(HttpHeaders.AcceptLanguage, header)
            }
        }
    }

    /** Raw client + pipeline call: the engine sees exactly this header. */
    private suspend fun eventsForHeader(header: String?): List<com.eventom.domain.model.Event> =
        clientAccepting(header)
            .get("$API_HOST$API_BASE_PATH/events")
            .body<List<EventDto>>()
            .map(EventDto::toDomain)

    /** Data-source level: languageProvider drives the wire header (production path). */
    @Test
    fun acceptLanguageEnServesEnglishTitles() = runTest {
        val source = KtorEventRemoteDataSource(clientAccepting(null), languageProvider = { EffectiveLanguage.EN })
        val events = source.fetchEvents()
        assertEquals("Aurora Nights — Summer Tour 2027", events.first().title)
        assertEquals("Laughter Fuel — Evening Show", events.first { it.id == 4L }.title)
    }

    @Test
    fun acceptLanguageDeServesGermanTitles() = runTest {
        val source = KtorEventRemoteDataSource(clientAccepting(null), languageProvider = { EffectiveLanguage.DE })
        val events = source.fetchEvents()
        assertEquals("Aurora Nights — Sommer Tour 2027", events.first().title)
    }

    @Test
    fun missingAcceptLanguageDefaultsToGerman() = runTest {
        val events = eventsForHeader(header = null)
        assertEquals("Aurora Nights — Sommer Tour 2027", events.first().title)
    }

    @Test
    fun bogusAcceptLanguageDefaultsToGerman() = runTest {
        val events = eventsForHeader(header = "fr-CH, fr;q=0.9")
        assertEquals("Aurora Nights — Sommer Tour 2027", events.first().title)
    }

    @Test
    fun detailRouteServesPerLanguageTiers() = runTest {
        val enDetail = KtorEventDetailRemoteDataSource(
            clientAccepting(null),
            languageProvider = { EffectiveLanguage.EN },
        ).fetchEventDetail(id = 2)
        assertEquals("Premium package incl. prosecco", enDetail.ticketCategories.first().name)

        val deDetail = KtorEventDetailRemoteDataSource(
            clientAccepting(null),
            languageProvider = { EffectiveLanguage.DE },
        ).fetchEventDetail(id = 2)
        assertEquals("Premium-Paket inkl. Prosecco", deDetail.ticketCategories.first().name)
    }

    @Test
    fun loginStillWorksWithAcceptLanguageHeaderPresent() = runTest {
        val auth = KtorAuthRemoteDataSource(clientAccepting("en"))
        val user = auth.login(email = "demo@eventom.dev", password = "demo1234")
        assertEquals("Demo Fan", user.name)
    }
}