import SwiftUI
import Shared

/// Second native screen: shows platform info produced by the shared
/// expect/actual mechanism — proof the shared module knows which platform
/// it is running on.
struct AboutView: View {
    private let info: PlatformInfo
    private let sharedVersion: String

    init() {
        self.info = Platform.shared.info()
        self.sharedVersion = SharedVersion.shared.VERSION
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("EvenTom") {
                    LabeledContent("Shared Kotlin", value: sharedVersion)
                    LabeledContent("Running on", value: "\(info.osName) \(info.osVersion)")
                    LabeledContent("Device", value: info.deviceModel)
                }
                Section("Boilerplate layout") {
                    Text("All logic lives in the shared Kotlin module: domain, data (Ktor), view models and Koin wiring. Both apps only render shared state natively.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("About")
        }
    }
}