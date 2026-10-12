package com.evantom.domain.repository

import com.evantom.domain.model.AuthUser

/**
 * Credential check against the account service. Domain-side contract (F1 /
 * plan T11): the Ktor implementation stays in `com.evantom.data.remote`.
 *
 * Returns the signed-in user; throws (HTTP 401) on wrong credentials.
 */
interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): AuthUser
}