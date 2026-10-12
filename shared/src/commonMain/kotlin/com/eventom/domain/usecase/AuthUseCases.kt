package com.eventom.domain.usecase

import com.eventom.domain.repository.AuthRemoteDataSource
import com.eventom.domain.model.AuthUser
import kotlinx.coroutines.CancellationException

/**
 * Credential check the login screen performs. Thin by design: the data
 * source contract already throws on refusal (HTTP 401) — this boundary only
 * turns arbitrary transport failures into [Result.failure] while keeping
 * cancellation intact (repo gotcha #10: no `runCatching` around suspend).
 */
class LoginUseCase(private val auth: AuthRemoteDataSource) {

    suspend operator fun invoke(email: String, password: String): Result<AuthUser> = try {
        Result.success(auth.login(email = email.trim(), password = password))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}