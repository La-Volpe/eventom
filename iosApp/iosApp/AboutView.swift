import SwiftUI
import Shared

/// Second native screen: shows platform info produced by the shared
/// expect/actual mechanism — proof the shared module knows which platform
/// it is running on.
struct AboutView: View {
    let strings: Strings

    private let info: PlatformInfo
    private let sharedVersion: String

    init(strings: Strings) {
        self.strings = strings
        self.info = Platform.shared.info()
        self.sharedVersion = SharedVersion.shared.VERSION
    }

    var body: some View {
        NavigationStack {
            Form {
                // "EvenTom" stays a brand wordmark; never translated.
                Section("EvenTom") {
                    LabeledContent(strings.aboutSharedKotlin(), value: sharedVersion)
                    LabeledContent(strings.aboutRunningOn(), value: "\(info.osName) \(info.osVersion)")
                    LabeledContent(strings.aboutDevice(), value: info.deviceModel)
                }
                Section(strings.aboutBoilerplateTitle()) {
                    Text(strings.aboutBoilerplateBody())
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle(strings.aboutHeading())
        }
    }
}