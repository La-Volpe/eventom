package com.eventom.presentation

import com.eventom.domain.model.AuthUser
import com.eventom.domain.usecase.LoginUseCase
import com.eventom.session.SessionStore
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import com.eventom.core.UiObservation
import com.eventom.language.LanguageSetting
import kotlinx.coroutines.CoroutineScope

/**
 * Login screen states. `sealed class` (not interface) so the Swift surface
 * stays a class hierarchy with `is`-castable subclasses (repo gotcha #5);
 * iOS rendering is out of scope this engagement but the rule stays in force.
 */
sealed class AuthUiState {
    /** Fresh form, nothing submitted yet. */
    data object Idle : AuthUiState()

    /** Credentials are being checked — the CTA shows progress. */
    data object Loading : AuthUiState()

    /** Credentials accepted; the session holder now carries [user]. */
    data class Success(val user: AuthUser) : AuthUiState()

    /** Attempt refused; [message] is already phrased for the UI. */
    data class Error(val message: String) : AuthUiState()
}

/**
 * Owns the login screen logic. The UI only submits via [signIn] and renders
 * [state]. On success the shared [SessionStore] is updated (in memory, no
 * persistence), which is what navigation (M3) will branch on.
 */
class AuthViewModel(
    private val login: LoginUseCase,
    private val session: SessionStore,
    // Message source for the localized login errors (bilingual 2/4).
    private val languageSetting: LanguageSetting,
    // Tests inject a scope; production resolves the Main dispatcher (gotcha #9).
    mainScope: CoroutineScope? = null,
) : BaseViewModel<AuthUiState>(initialState = AuthUiState.Idle, mainScope = mainScope) {

    /**
     * Swift-safe observation: every state change is delivered to [onChange]
     * until the returned observation is stopped.
     */
    fun observe(onChange: (AuthUiState) -> Unit): UiObservation = observeState(onChange)

    fun signIn(email: String, password: String) {
        if (state.value is AuthUiState.Loading) return // no double-submit
        launch {
            updateState { AuthUiState.Loading }
            val outcome = login(email = email, password = password)
            updateState {
                val user = outcome.getOrNull()
                if (user != null) {
                    session.signIn(user)
                    AuthUiState.Success(user)
                } else {
                    AuthUiState.Error(failureMessage(outcome.exceptionOrNull()))
                }
            }
        }
    }

    /** Called when the screen goes away so a re-entry starts fresh. */
    fun reset() = updateState { AuthUiState.Idle }

    private fun failureMessage(error: Throwable?): String {
        val strings = languageSetting.strings()
        return when {
            error is ClientRequestException && error.response.status == HttpStatusCode.Unauthorized ->
                strings.loginWrongPassword()
            else ->
                strings.loginGenericError()
        }
    }
}