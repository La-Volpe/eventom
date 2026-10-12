package com.evantom.data.remote

import com.evantom.domain.model.AuthUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * Credential check against the account service. Data-side plumbing for the
 * login milestone (M2): the login SCREEN is M2, the HTTP surface is shipped
 * and tested here with the fake backend.
 */
interface AuthRemoteDataSource {
    /** Returns the signed-in user; throws (HTTP 401) on wrong credentials. */
    suspend fun login(email: String, password: String): AuthUser
}

/** Wire format of the `POST /api/login` request body. */
@Serializable
internal data class LoginRequestDto(
    val email: String,
    val password: String,
)

/** Wire format of the `POST /api/login` response — no password echoes back. */
@Serializable
internal data class UserDto(
    val email: String,
    val name: String,
)

internal fun UserDto.toDomain(): AuthUser = AuthUser(
    name = name,
    email = email,
)

class KtorAuthRemoteDataSource(private val client: HttpClient) : AuthRemoteDataSource {
    override suspend fun login(email: String, password: String): AuthUser =
        client.post("$API_HOST$API_BASE_PATH/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email = email, password = password))
        }.body<UserDto>().toDomain()
}