package com.eventom.domain.model

/**
 * The signed-in user as the API returns it (no password — that never leaves
 * the request). Consumed by the login milestone (M2); the plumbing lands M1
 * with the fake backend.
 */
data class AuthUser(
    val name: String,
    val email: String,
)