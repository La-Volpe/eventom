package com.evantom.presentation

import com.evantom.domain.repository.AuthRemoteDataSource
import com.evantom.data.remote.KtorAuthRemoteDataSource
import com.evantom.data.fake.fakeBackendEngine
import com.evantom.domain.model.AuthUser
import com.evantom.domain.usecase.LoginUseCase
import com.evantom.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Auth view model behaviour over the M1 fake backend (MockEngine + full
 * client pipeline).
 *
 * Concurrency notes: the view model gets an Unconfined scope so signIn() runs
 * inline up to the first real suspension; HTTP machinery may resume on its own
 * thread (Kotlin/Native hops off the test thread), so completion waits poll the
 * StateFlow in real time instead of advancing a virtual scheduler — virtual
 * time and real background threads don't mix in a cross-platform test.
 */
class AuthViewModelTest {

    private val demoUser = AuthUser(name = "Demo Fan", email = "demo@evantom.dev")

    /** The M1 fake backend, wired exactly like production dataModule (expectSuccess on). */
    private val httpClient = HttpClient(fakeBackendEngine()) {
        expectSuccess = true
        install(ContentNegotiation) { json() }
    }

    private val realRemote = KtorAuthRemoteDataSource(httpClient)

    @AfterTest
    fun tearDown() {
        httpClient.close()
    }

    private fun makeViewModel(remote: AuthRemoteDataSource, session: SessionStore = SessionStore()) =
        AuthViewModel(
            login = LoginUseCase(remote),
            session = session,
            mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        ) to session

    /** Polls [predicate] in real time until it holds or times out. */
    private suspend fun awaitState(
        viewModel: AuthViewModel,
        timeoutMs: Long = 5_000,
        predicate: (AuthUiState) -> Boolean,
    ): AuthUiState {
        val stop = TimeSource.Monotonic.markNow()
        while (stop.elapsedNow() < timeoutMs.milliseconds) {
            viewModel.state.value.let { if (predicate(it)) return it }
            delay(20)
        }
        error("Timed out waiting for state; last state = ${viewModel.state.value}")
    }

    /** Delegates to the real fake-backend source behind a completion gate. */
    private class GatedAuth(
        private val inner: AuthRemoteDataSource,
        private val gate: CompletableDeferred<Unit>,
    ) : AuthRemoteDataSource {
        override suspend fun login(email: String, password: String): AuthUser {
            gate.await()
            return inner.login(email = email, password = password)
        }
    }

    @Test
    fun validCredentialsLeadToSuccessAndSession(): Unit = runBlocking {
        val (viewModel, session) = makeViewModel(realRemote)

        viewModel.signIn(email = "demo@evantom.dev", password = "demo1234")

        val state = awaitState(viewModel) { it is AuthUiState.Success }
        assertEquals(demoUser, (state as AuthUiState.Success).user)
        assertEquals(demoUser, session.currentUser.value)
    }

    @Test
    fun wrongCredentialsLeadToFriendlyErrorWithoutSession(): Unit = runBlocking {
        val (viewModel, session) = makeViewModel(realRemote)

        viewModel.signIn(email = "demo@evantom.dev", password = "hunter2")

        val state = awaitState(viewModel) { it is AuthUiState.Error }
        assertEquals("Wrong e-mail or password", (state as AuthUiState.Error).message)
        assertNull(session.currentUser.value)
    }

    @Test
    fun signInShowsLoadingWhileCredentialsAreChecked(): Unit = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val (viewModel, _) = makeViewModel(GatedAuth(realRemote, gate))

        viewModel.signIn(email = "demo@evantom.dev", password = "demo1234")
        // Runs inline (Unconfined) up to the gate: Loading is observable immediately.
        assertIs<AuthUiState.Loading>(viewModel.state.value)

        gate.complete(Unit)
        awaitState(viewModel) { it is AuthUiState.Success }
    }

    @Test
    fun transportFailureMapsToConnectionMessage(): Unit = runBlocking {
        val remote = object : AuthRemoteDataSource {
            override suspend fun login(email: String, password: String): AuthUser =
                throw IllegalStateException("simulated transport failure")
        }
        val (viewModel, _) = makeViewModel(remote)

        viewModel.signIn(email = "demo@evantom.dev", password = "demo1234")

        val state = awaitState(viewModel) { it is AuthUiState.Error }
        assertTrue((state as AuthUiState.Error).message.contains("connection"), state.message)
    }

    @Test
    fun doubleSubmitWhileLoadingIsIgnored(): Unit = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val (viewModel, _) = makeViewModel(GatedAuth(realRemote, gate))

        viewModel.signIn(email = "demo@evantom.dev", password = "demo1234")
        assertIs<AuthUiState.Loading>(viewModel.state.value)
        viewModel.signIn(email = "stella@evantom.dev", password = "sommer1") // second tap while loading
        gate.complete(Unit)

        val state = awaitState(viewModel) { it is AuthUiState.Success }
        assertEquals(demoUser, (state as AuthUiState.Success).user)
    }

    @Test
    fun resetReturnsToIdle(): Unit = runBlocking {
        val (viewModel, _) = makeViewModel(realRemote)

        viewModel.signIn(email = "demo@evantom.dev", password = "demo1234")
        awaitState(viewModel) { it is AuthUiState.Success }

        viewModel.reset()
        assertIs<AuthUiState.Idle>(viewModel.state.value)
    }
}