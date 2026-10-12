package com.eventom.data.remote

import com.eventom.domain.model.AuthUser
import com.eventom.domain.repository.AuthRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

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