package com.evantom.domain.usecase

import com.evantom.domain.model.AuthUser
import com.evantom.domain.repository.AuthRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Direct use-case tests (plan T14 / review G2): the boundary converts
 * arbitrary transport failures into Result.failure while cancellation stays
 * a thrown exception (repo gotcha #10).
 */
class LoginUseCaseTest {

    private val demoUser = AuthUser(name = "Demo Fan", email = "demo@evantom.dev")

    private class FakeAuth(private val outcome: suspend () -> AuthUser) : AuthRemoteDataSource {
        override suspend fun login(email: String, password: String): AuthUser = outcome()
    }

    @Test
    fun successForwardsUser() = runTest {
        val useCase = LoginUseCase(FakeAuth { demoUser })

        val result = useCase(email = "demo@evantom.dev", password = "demo1234")

        assertEquals(demoUser, result.getOrNull())
    }

    @Test
    fun transportFailureBecomesCleanResultFailure() = runTest {
        val useCase = LoginUseCase(FakeAuth { throw IllegalStateException("connection reset") })

        val result = useCase(email = "demo@evantom.dev", password = "demo1234")

        assertTrue(result.isFailure)
        assertEquals("connection reset", result.exceptionOrNull()?.message)
    }

    @Test
    fun cancellationIsRethrownNotWrapped() = runTest {
        val useCase = LoginUseCase(FakeAuth { throw CancellationException("caller cancelled") })

        assertFailsWith<CancellationException> {
            useCase(email = "demo@evantom.dev", password = "demo1234")
        }
    }
}