package com.eventom.session

import com.eventom.domain.model.AuthUser
import com.eventom.core.UiObservation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** Observation contract the iOS bridge consumes (typed, concrete). */
class SessionStoreTest {

    private val demoUser = AuthUser(name = "Demo Fan", email = "demo@eventom.dev")
    private val secondUser = AuthUser(name = "Stella Sommer", email = "stella@eventom.dev")

    private fun makeStore(): SessionStore =
        SessionStore(mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))

    /** Polls the last delivery in real time; see AuthViewModelTest for rationale. */
    private suspend fun awaitLast(session: SessionStore, observed: List<AuthUser?>, timeoutMs: Long = 3_000, predicate: (AuthUser?) -> Boolean): AuthUser? {
        val stop = TimeSource.Monotonic.markNow()
        while (stop.elapsedNow() < timeoutMs.milliseconds) {
            if (observed.isNotEmpty() && predicate(observed.last())) return observed.last()
            delay(20)
        }
        error("Timed out; observed = $observed")
    }

    @Test
    fun observeCurrentUserDeliversSignInAndSignOut(): Unit = runBlocking {
        val session = makeStore()
        val observed = mutableListOf<AuthUser?>()
        val observation: UiObservation = session.observeCurrentUser { observed.add(it) }

        session.signIn(demoUser)
        awaitLast(session, observed) { it == demoUser }

        session.signOut()
        awaitLast(session, observed) { it == null }

        session.signIn(secondUser)
        awaitLast(session, observed) { it == secondUser }

        observation.stop()
    }

    @Test
    fun stopCancelsDelivery(): Unit = runBlocking {
        val session = makeStore()
        val observed = mutableListOf<AuthUser?>()
        val gate = CompletableDeferred<Unit>()
        val observation: UiObservation = session.observeCurrentUser {
            gate.complete(Unit)
            observed.add(it)
        }
        // First delivery happens on collect start (initial value null).
        gate.await()

        observation.stop()
        val nowCount = observed.size

        session.signIn(demoUser)
        // Unconfined collection makes delivery synchronous, so even a leaked
        // observer would have appended during sign-in; the count proves stop().
        assertEquals(nowCount, observed.size, "delivery must stop after stop()")
        assertEquals(demoUser, session.currentUser.value)
    }

    /** Navigation branches on this flag (Android `MainActivity` splash,
     *  iOS `RootView`), so it must track sign-in/out without an observer. */
    @Test
    fun isSignedInTracksTheSession() {
        val session = makeStore()

        assertFalse(session.isSignedIn, "nobody is signed in on a cold start")
        session.signIn(demoUser)
        assertTrue(session.isSignedIn)
        session.signOut()
        assertFalse(session.isSignedIn, "sign-out must clear the flag")
    }
}
