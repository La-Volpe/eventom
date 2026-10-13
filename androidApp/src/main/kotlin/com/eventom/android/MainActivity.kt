package com.eventom.android

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
import com.eventom.di.SharedKoin
import com.eventom.language.AppLanguage
import com.eventom.presentation.AuthUiState
import com.eventom.presentation.EventDetailUiState
import com.eventom.strings.Strings
import com.eventom.strings.strings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Bilingual 2/4 UI-test hook: machine tests pass UITEST_LANG through
        // launch-intent extras; it wins over the persisted choice and the
        // device locale (LanguageSetting seeding rule, task 1).
        intent.getStringExtra("UITEST_LANG")?.let { code ->
            SharedKoin.languageSetting().setTestOverride(code)
        }
        setContent {
            EvenTomTheme {
                EvenTomApp()
            }
        }
    }
}

/**
 * Root composition: Compose Navigation (plan T18, D4) with typed routes:
 * `splash` → `login` | (`events` + `settings` + `about` tabs) → `detail/{eventId}`.
 *
 * Language state (bilingual 2/4): the effective language is collected ONCE
 * at the root; `strings(effective)` resolves the shared string table per
 * composition and is passed DOWN to every screen (state-hoisting style —
 * screens never reach into Koin for language/strings).
 *
 * Bottom-bar pick: Events | Settings | About — Settings sits second, one tap
 * from either neighbor, which made the language switch reachable from both
 * content tabs identically during the machine walkthrough.
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
    val languageSetting = remember { SharedKoin.languageSetting() }
    val eventsState = eventsViewModel.state.collectAsStateWithLifecycle().value
    val effective by languageSetting.effective.collectAsStateWithLifecycle()
    val strings = remember(effective) { strings(effective) }

    Scaffold(
        bottomBar = {
            RouteBottomBar(
                navigationController = navigationController,
                strings = strings,
                visible = currentRouteIs(navigationController, "events", "settings", "about"),
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
                    strings = strings,
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
                    strings = strings,
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
                    strings = strings,
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
            composable("settings") {
                val persisted = languageSetting.persistedChoice.collectAsStateWithLifecycle().value
                SettingsScreen(
                    strings = strings,
                    persisted = persisted,
                    onChoose = { languageSetting.choose(it) },
                )
            }
            composable("about") {
                AboutScreen(strings = strings)
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
                    strings = strings,
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
private fun RouteBottomBar(navigationController: NavHostController, strings: Strings, visible: Boolean) {
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
            label = { Text(strings.eventsTabLabel()) },
            icon = {},
        )
        NavigationBarItem(
            selected = currentRouteIs(navigationController, "settings"),
            onClick = {
                navigationController.navigate("settings") {
                    popUpTo(navigationController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            label = { Text(strings.settingsTitle()) },
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
            label = { Text(strings.aboutTabLabel()) },
            icon = {},
        )
    }
}