import SwiftUI
import Shared

/**
 * Entry point of the iOS (SwiftUI) presentation layer — everything the app
 * shows is built here; all behaviour comes from the shared Kotlin module.
 */
@main
struct EvenTomApp: App {
    @StateObject private var languageBridge = LanguageBridge()

    init() {
        // SharedKoin is a Kotlin `object` → `SharedKoin.shared` in Swift.
        SharedKoin.shared.start()
        // Bilingual 2/4 UI-test hook: the runner can pin the content language
        // through defaults (UITEST_LANG); it wins over persisted + device
        // seeding (LanguageSetting rule, task 1). Also a manual QA channel:
        // `defaults write dev.eventom.iosApp UITEST_LANG de`.
        if let override = UserDefaults.standard.string(forKey: "UITEST_LANG") {
            SharedKoin.shared.languageSetting().setTestOverride(code: override)
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .onAppear { languageBridge.startObserving() }
                .onDisappear { languageBridge.stopObserving() }
        }
    }
}
