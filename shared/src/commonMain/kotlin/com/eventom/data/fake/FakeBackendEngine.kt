package com.eventom.data.fake

import com.eventom.data.remote.LoginRequestDto
import com.eventom.data.remote.UserDto
import com.eventom.data.sha.sha256Hex
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json

/** Every served response is JSON — set via headers, the Ktor 3 mock-way. */
private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

/**
 * A fake but faithful backend: it intercepts at the Ktor engine boundary, so
 * the FULL client pipeline (ContentNegotiation, kotlinx.serialization) runs
 * exactly as it would against the real API — only the transport is local.
 *
 * Route table (mirrors `src/commonMain/resources/api/`):
 *   GET  /api/events                -> events.json
 *   GET  /api/event-details/{id}    -> event_details/{id}.json (404 if unknown)
 *   POST /api/login                 -> user object for matching credentials (401 otherwise)
 *
 * The client never knows this is a fake: matching is done on the request path
 * only (the documented [com.eventom.data.remote.API_HOST] is ignored), so
 * swapping back to a real engine needs no call-site changes.
 */
fun fakeBackendEngine(json: Json = Json): HttpClientEngine = MockEngine { request ->
    val path = request.url.encodedPath
    when {
        request.method == HttpMethod.Get && path == "/api/events" ->
            respondJson(FakeApiPayloads.EVENTS_JSON)

        request.method == HttpMethod.Get && path.startsWith("/api/event-details/") -> {
            val id = path.substringAfterLast('/').toLongOrNull()
            val payload = id?.let(FakeApiPayloads::eventDetailJson)
            if (payload != null) {
                respondJson(payload)
            } else {
                respondJsonError(HttpStatusCode.NotFound, "event not found")
            }
        }

        request.method == HttpMethod.Post && path == "/api/login" ->
            respondToLogin(request, json)

        else -> respondJsonError(HttpStatusCode.NotFound, "unknown endpoint: $path")
    }
}

private fun MockRequestHandleScope.respondJson(
    payload: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(payload, status, jsonHeaders)

private fun MockRequestHandleScope.respondJsonError(status: HttpStatusCode, hint: String): HttpResponseData =
    respond("""{"error": "$hint"}""", status, jsonHeaders)

/**
 * Credential check. The request body has already been serialized by the
 * client's ContentNegotiation plugin; the fake "server" decodes it the way a
 * real controller would.
 */
private suspend fun MockRequestHandleScope.respondToLogin(
    request: HttpRequestData,
    json: Json,
): HttpResponseData {
    val bodyText = request.body.toByteArray().decodeToString()
    val login = try {
        json.decodeFromString<LoginRequestDto>(bodyText)
    } catch (_: Exception) {
        return respondJsonError(HttpStatusCode.BadRequest, "malformed login request")
    }
    // Demo accounts ship with FakeApiPayloads (mirroring api/users.json),
    // which store SHA-256 hex of the demo passwords (AGENTS.md #18). The posted
    // password is hashed for the comparison; its plaintext is never logged
    // and never stored.
    val account = json.decodeFromString<List<FakeUserAccount>>(FakeApiPayloads.USERS_JSON)
        .firstOrNull { it.email == login.email && it.passwordHash == sha256Hex(login.password) }
    return if (account != null) {
        respondJson(json.encodeToString(UserDto(email = account.email, name = account.name)))
    } else {
        respondJsonError(HttpStatusCode.Unauthorized, "invalid credentials")
    }
}

@kotlinx.serialization.Serializable
internal data class FakeUserAccount(
    val email: String,
    /** SHA-256 hex of the demo password; the plaintext stays a UI hint only. */
    val passwordHash: String,
    val name: String,
)