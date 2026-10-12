package com.evantom.data.fake

import com.evantom.data.remote.EventDetailRemoteDataSource
import com.evantom.data.remote.EventRemoteDataSource
import com.evantom.data.remote.AuthRemoteDataSource
import com.evantom.data.remote.KtorAuthRemoteDataSource
import com.evantom.data.remote.KtorEventDetailRemoteDataSource
import com.evantom.data.remote.KtorEventRemoteDataSource
import com.evantom.domain.model.EventCategory
import com.evantom.domain.model.EventDetail
import com.evantom.domain.model.AuthUser
import io.ktor.client.HttpClient
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
        val user: AuthUser = authSource.login(email = "demo@evantom.dev", password = "demo1234")

        assertEquals("Demo Fan", user.name)
        assertEquals("demo@evantom.dev", user.email)
    }

    @Test
    fun secondDemoAccountWorks() = runTest {
        val user = authSource.login(email = "stella@evantom.dev", password = "sommer1")

        assertEquals("Stella Sommer", user.name)
    }

    @Test
    fun wrongPasswordIsRejected() = runTest {
        assertFailsWith<io.ktor.client.plugins.ClientRequestException> {
            authSource.login(email = "demo@evantom.dev", password = "hunter2")
        }
    }
}