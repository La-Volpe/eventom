import SwiftUI
import Shared

/**
 * Root of the iOS app (T18, D4): the session-state switch (splash → login |
 * tabs) and a `NavigationStack` for the detail push. The Route enum for
 * detail navigation is retired — the push happens through a value-based
 * `navigationDestination(for: Int64.self)` carrying the event id, over the
 * PRESERVED TabView (chips/list stay alive underneath; detail covers the
 * tab bar on push — matching the pre-Phase-4 full-screen feel).
 */
struct RootView: View {
    private enum Route { case splash, login, events }

    @State private var route: Route = .splash
    @State private var detailPath: [Int64] = []

    var body: some View {
        switch route {
        case .splash:
            SplashView(onDone: {
                route = SharedKoin.shared.sessionStore().isSignedIn ? .events : .login
            })
        case .login:
            LoginView(
                onSuccess: {
                    SharedKoin.shared.authViewModel().reset()
                    route = .events
                },
                onBack: {
                    SharedKoin.shared.authViewModel().reset()
                    route = .events
                },
            )
        case .events:
            NavigationStack(path: $detailPath) {
                TabView {
                    EventsView(
                        onSignIn: { route = .login },
                        onSignOut: {
                            SharedKoin.shared.sessionStore().signOut()
                            SharedKoin.shared.authViewModel().reset()
                            route = .login
                        },
                        onOpenDetail: { detailPath.append($0) },
                    )
                        .tabItem { Label("Events", systemImage: "calendar") }
                    AboutView()
                        .tabItem { Label("About", systemImage: "info.circle") }
                }
                .navigationDestination(for: Int64.self) { eventId in
                    EventDetailView(eventId: eventId) {
                        // Back: reset once (before the pop) so a re-entry
                        // starts fresh — the single-owner contract held since
                        // the login screen.
                        SharedKoin.shared.eventDetailViewModel().reset()
                        if !detailPath.isEmpty {
                            detailPath.removeLast()
                        }
                    }
                }
            }
        }
    }
}

#Preview {
    RootView()
}