package com.eventom.session

import com.eventom.domain.model.AuthUser
import com.eventom.core.UiObservation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * In-memory holder for the signed-in user. Plain Kotlin, no persistence:
 * the app forgets the session when the process dies (splash + navigation
 * re-checks this).
 *
 * Written by login, read by screens that need to know who is signed in.
 */
class SessionStore(
    // Tests inject a scope; production resolves the Main dispatcher
    // (repo gotcha #9 — nothing hardwires Main).
    mainScope: CoroutineScope? = null,
) {

    private val observeScope: CoroutineScope = mainScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val user = MutableStateFlow<AuthUser?>(null)

    /** Emits `null` until a sign-in, then the user (and `null` again on sign-out). */
    val currentUser: StateFlow<AuthUser?> = user.asStateFlow()

    val isSignedIn: Boolean
        get() = user.value != null

    fun signIn(newUser: AuthUser) {
        user.value = newUser
    }

    fun signOut() {
        user.value = null
    }

    /**
     * Concrete (Swift-facing) observation of the signed-in user: every
     * change — sign-in, sign-out — is delivered to [onChange] until the
     * returned observation is stopped. Null means signed out.
     */
    fun observeCurrentUser(onChange: (AuthUser?) -> Unit): UiObservation =
        object : UiObservation {
            private val job: Job = observeScope.launch {
                currentUser.collect { value -> onChange(value) }
            }

            override fun stop() {
                job.cancel()
            }
        }
}