package com.evantom.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
 * Root composition: Compose Navigation (plan T18, D4) with typed routes:
 * `splash` → `login` | (`events` + `about` tabs) → `detail/{eventId}`.
 * The bottom bar remains and switches tab destinations nav-driven.
 *
 * State hoisting per destination: screens keep taking state + callbacks;
 * the shared view models stay Koin singletons resolved once per destination
 * (`SharedKoin` as before — no new VM layer). Session routing (login vs
 * events) is nav-driven: back-stack + popUpTo, replacing the route enum.
 */
@Composable
fun EvenTomApp() {
    val navigationController = rememberNavController()
    val eventsViewModel = remember { SharedKoin.eventsViewModel() }
    val authViewModel = remember { SharedKoin.authViewModel() }
    val detailViewModel = remember { SharedKoin.eventDetailViewModel() }
    val session = remember { SharedKoin.sessionStore() }
    val eventsState = eventsViewModel.state.collectAsStateWithLifecycle().value

    Scaffold(
        bottomBar = {
            RouteBottomBar(
                navigationController = navigationController,
                visible = currentRouteIs(navigationController, "events", "about"),
            )
        },
    ) { innerPadding ->
        // Bottom-only: the detail destination keeps its full-bleed hero (no
        // top inset), while list/login content stays clear of the bottom bar.
        NavHost(
            navController = navigationController,
            startDestination = "splash",
            modifier = androidx.compose.ui.Modifier.padding(
                bottom = innerPadding.calculateBottomPadding(),
            ),
        ) {
            composable("splash") {
                SplashScreen(
                    onSplashFinished = {
                        val target = if (session.isSignedIn) "events" else "login"
                        navigationController.navigate(target) { popUpTo("splash") { inclusive = true } }
                    },
                )
            }
            composable("login") {
                val authState = authViewModel.state.collectAsStateWithLifecycle().value
                LaunchedEffect(authState) {
                    if (authState is AuthUiState.Success) {
                        authViewModel.reset()
                        navigationController.navigate("events") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                LoginScreen(
                    authState = authState,
                    onSignIn = { email, password -> authViewModel.signIn(email, password) },
                    onBack = {
                        authViewModel.reset()
                        navigationController.navigate("events") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }
            composable("events") {
                EventsScreen(
                    viewState = eventsState,
                    onSelect = { eventsViewModel.select(it) },
                    onToggleSaved = { eventsViewModel.toggleSaved(it) },
                    onRefresh = { eventsViewModel.refresh() },
                    onOpenDetail = { id -> navigationController.navigate("detail/$id") },
                    headerUser = session.currentUser.collectAsStateWithLifecycle().value?.name,
                    onSignIn = { navigationController.navigate("login") },
                    onSignOut = {
                        session.signOut()
                        authViewModel.reset()
                        navigationController.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                )
            }
            composable("about") {
                AboutScreen()
            }
            composable(
                route = "detail/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
            ) { entry ->
                val eventId = entry.arguments?.getLong("eventId") ?: -1L
                val detailState = detailViewModel.state.collectAsStateWithLifecycle().value
                // SAFETY NET (kept, verified): the nav framework restores the
                // ROUTE after process death (back-stack bundle survives
                // am-kill after a stop), but the detail payload is NOT
                // restored — the destination must re-request it once. Without
                // this, a restored detail destination sticks on the loading
                // state forever (verified by kill+relaunch with the net
                // disabled, Phase 4 evidence).
                LaunchedEffect(eventId) {
                    if (detailState is EventDetailUiState.Idle) {
                        detailViewModel.load(eventId)
                    }
                }
                EventDetailScreen(
                    state = detailState,
                    onBack = {
                        detailViewModel.reset()
                        navigationController.popBackStack()
                    },
                    onRetry = { detailViewModel.load(eventId) },
                )
            }
        }
    }
}

@Composable
private fun currentRouteIs(controller: NavHostController, vararg routes: String): Boolean {
    val current = controller.currentBackStackEntryAsState().value?.destination?.route
    return routes.any { current == it }
}

@Composable
private fun RouteBottomBar(navigationController: NavHostController, visible: Boolean) {
    if (!visible) return
    NavigationBar {
        NavigationBarItem(
            selected = currentRouteIs(navigationController, "events"),
            onClick = {
                navigationController.navigate("events") {
                    popUpTo(navigationController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            label = { Text("Events") },
            icon = {},
        )
        NavigationBarItem(
            selected = currentRouteIs(navigationController, "about"),
            onClick = {
                navigationController.navigate("about") {
                    popUpTo(navigationController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            label = { Text("About") },
            icon = {},
        )
    }
}