package com.evantom.domain.usecase

import com.evantom.domain.model.EventDetail
import com.evantom.domain.model.TicketCategory
import com.evantom.domain.repository.EventDetailRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Direct use-case tests (plan T14 / review G2) for the detail boundary.
 */
class FetchEventDetailUseCaseTest {

    private val fixtureDetail = EventDetail(
        id = 2,
        longSummary = "Ein Maskenball, ein verschwundenes Testament…",
        lineup = listOf("Original Cast Hamburg"),
        doorsTime = "17:30",
        ticketCategories = listOf(TicketCategory(name = "Kategorie 1 — Loge", price = 109.50)),
        ageInfo = "Fsk ab 12 Jahren",
    )

    private class FakeDetail(private val outcome: suspend () -> EventDetail) : EventDetailRemoteDataSource {
        override suspend fun fetchEventDetail(id: Long): EventDetail = outcome()
    }

    @Test
    fun successForwardsDetail() = runTest {
        val useCase = FetchEventDetailUseCase(FakeDetail { fixtureDetail })

        val result = useCase(id = 2)

        assertEquals(fixtureDetail, result.getOrNull())
    }

    @Test
    fun notFoundSurfacesFailureWithMessage() = runTest {
        val useCase = FetchEventDetailUseCase(
            FakeDetail { throw IllegalStateException("event not found: 99") },
        )

        val result = useCase(id = 99)

        assertTrue(result.isFailure)
        assertEquals("event not found: 99", result.exceptionOrNull()?.message)
    }

    @Test
    fun cancellationIsRethrownNotWrapped() = runTest {
        val useCase = FetchEventDetailUseCase(FakeDetail { throw CancellationException("caller cancelled") })

        assertFailsWith<CancellationException> {
            useCase(id = 2)
        }
    }
}