import SwiftUI
import Shared

/**
 * Entry point of the iOS (SwiftUI) presentation layer — everything the app
 * shows is built here; all behaviour comes from the shared Kotlin module.
 */
@main
struct EvenTomApp: App {
    init() {
        // SharedKoin is a Kotlin `object` → `SharedKoin.shared` in Swift.
        SharedKoin.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}