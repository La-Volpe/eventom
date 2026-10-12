import SwiftUI
import Shared

/**
 * Root of the iOS app: splash → (Login | Events), no navigation library.
 * Routing keys off the shared SessionStore (in memory → cold start = splash
 * → Login). Sign-out returns to Login; the detail screen presents as a
 * full-screen cover over the (preserved) tab view.
 */
struct RootView: View {
    /// Detail navigation model (IOS-3): `Identifiable` for fullScreenCover.
    private struct DetailRoute: Identifiable {
        let id: Int64
    }

    private enum Route { case splash, login, events }

    @State private var route: Route = .splash
    @State private var detailRoute: DetailRoute? = nil

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
            TabView {
                EventsView(
                    onSignIn: { route = .login },
                    onSignOut: {
                        SharedKoin.shared.authViewModel().reset()
                        route = .login
                    },
                    onOpenDetail: { detailRoute = DetailRoute(id: $0) },
                )
                    .tabItem { Label("Events", systemImage: "calendar") }
                AboutView()
                    .tabItem { Label("About", systemImage: "info.circle") }
            }
            .fullScreenCover(item: $detailRoute) { route in
                EventDetailView(eventId: route.id) {
                    // Back: reset once (before the cover closes) so a re-entry
                    // starts fresh — same contract as the login screen.
                    SharedKoin.shared.eventDetailViewModel().reset()
                    detailRoute = nil
                }
            }
        }
    }
}

#Preview {
    RootView()
}