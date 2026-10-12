package com.evantom.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evantom.di.SharedKoin
import com.evantom.presentation.AuthUiState
import com.evantom.presentation.EventDetailUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EvenTomTheme {
                EvenTomApp()
            }
        }
    }
}

/**
 * Root composition: Splash → (Login | Events) decided by the shared
 * SessionStore (in-memory, so a cold start always goes to Login). No
 * navigation library — plain state. View models come from the shared Koin
 * graph started in [com.evantom.android.EvenTomApplication].
 */
@Composable
fun EvenTomApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    var detailEventId by rememberSaveable { mutableLongStateOf(-1L) }
    val eventsViewModel = remember { SharedKoin.eventsViewModel() }
    val authViewModel = remember { SharedKoin.authViewModel() }
    val detailViewModel = remember { SharedKoin.eventDetailViewModel() }
    val session = remember { SharedKoin.sessionStore() }
    val eventsState = eventsViewModel.state.collectAsStateWithLifecycle().value
    val authState = authViewModel.state.collectAsStateWithLifecycle().value
    val detailState = detailViewModel.state.collectAsStateWithLifecycle().value
    val currentUser = session.currentUser.collectAsStateWithLifecycle().value

    // Successful sign-in: park the login state and go to Events.
    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            authViewModel.reset()
            screen = Screen.EVENTS
        }
    }

    Scaffold(
        bottomBar = {
            // The splash and the login sheet stand alone — no bottom bar.
            if (screen == Screen.EVENTS || screen == Screen.ABOUT) {
                NavigationBar {
                    NavigationBarItem(
                        selected = screen == Screen.EVENTS,
                        onClick = { screen = Screen.EVENTS },
                        label = { Text("Events") },
                        icon = {},
                    )
                    NavigationBarItem(
                        selected = screen == Screen.ABOUT,
                        onClick = { screen = Screen.ABOUT },
                        label = { Text("About") },
                        icon = {},
                    )
                }
            }
        },
    ) { innerPadding ->
        // `shared` knows nothing about safe areas; the app maps Scaffold's
        // padding into the content before rendering shared state.
        when (screen) {
            Screen.SPLASH -> SplashScreen(
                onSplashFinished = { screen = if (session.isSignedIn) Screen.EVENTS else Screen.LOGIN },
            )
            Screen.EVENTS -> EventsScreen(
                viewState = eventsState,
                onSelect = { eventsViewModel.select(it) },
                onToggleSaved = { eventsViewModel.toggleSaved(it) },
                onRefresh = { eventsViewModel.refresh() },
                onOpenDetail = { id ->
                    detailEventId = id
                    detailViewModel.load(id)
                    screen = Screen.DETAIL
                },
                headerUser = currentUser?.name,
                onSignIn = { screen = Screen.LOGIN },
                onSignOut = {
                    session.signOut()
                    screen = Screen.LOGIN
                },
                contentPadding = innerPadding,
            )
            Screen.DETAIL -> {
                // Safety net for a process restore straight onto the detail
                // screen: if the view model has no attempt running, reload.
                LaunchedEffect(detailState) {
                    if (detailState is EventDetailUiState.Idle) {
                        detailViewModel.load(detailEventId)
                    }
                }
                BackHandler {
                    detailViewModel.reset()
                    screen = Screen.EVENTS
                }
                EventDetailScreen(
                    state = detailState,
                    onBack = {
                        detailViewModel.reset()
                        screen = Screen.EVENTS
                    },
                    onRetry = { detailViewModel.load(detailEventId) },
                )
            }
            Screen.ABOUT -> AboutScreen(contentPadding = innerPadding)
            Screen.LOGIN -> {
                // System back behaves like the Back button while logging in.
                BackHandler(enabled = authState !is AuthUiState.Loading) {
                    authViewModel.reset()
                    screen = Screen.EVENTS
                }
                LoginScreen(
                    authState = authState,
                    onSignIn = { email, password -> authViewModel.signIn(email, password) },
                    onBack = {
                        authViewModel.reset()
                        screen = Screen.EVENTS
                    },
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

private enum class Screen { SPLASH, EVENTS, LOGIN, ABOUT, DETAIL }