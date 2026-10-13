import SwiftUI
import Shared

/**
 * Language state (bilingual 2/4) as ONE ObservableObject: wraps the shared
 * `LanguageSetting` through its concrete `observe(onChange:)` API (gotcha
 * #5/#14 rules — no type-erased flows) and republishes the RESOLVED string
 * table. Views receive `strings` down as a plain parameter (value-typed
 * passing, documented pick over environmentObject: the table flows one
 * direction — root → screens — and a parameter keeps every screen's
 * dependency explicit for the machine walkthroughs).
 */
final class LanguageBridge: ObservableObject {
    let setting: LanguageSetting

    /// The live string table for the current effective language.
    @Published private(set) var strings: Strings
    @Published private(set) var effective: EffectiveLanguage
    @Published private(set) var persisted: AppLanguage

    private var observation: UiObservation?

    /// Called on every EFFECTIVE change after construction (not on the
    /// initial value). Root wires the events-list refresh here (B5).
    var onEffectiveChanged: ((EffectiveLanguage) -> Void)?

    init(setting: LanguageSetting = SharedKoin.shared.languageSetting()) {
        self.setting = setting
        self.strings = setting.strings()
        self.effective = setting.current
        // PersistedStateFlow is bridged type-erased; the concrete observation
        // corrects this on attach (the first emission is immediate).
        self.persisted = AppLanguage.system
    }

    func startObserving() {
        guard observation == nil else { return }
        var initial = true
        // The core's callbacks arrive ObjC-bridged (Any?): dynamic-cast back
        // to the concrete enums (house gotcha #5 — concrete values only).
        let effectiveObservation = setting.observeEffective { [weak self] (language: EffectiveLanguage?) in
            guard let self, let language else { return }
            self.effective = language
            self.strings = self.setting.strings()
            if initial {
                initial = false
            } else {
                self.onEffectiveChanged?(language)
            }
        }
        let persistedObservation = setting.observePersistedChoice { [weak self] (language: AppLanguage?) in
            guard let self, let language else { return }
            self.persisted = language
        }
        observation = CompositeObservation(effectiveObservation, persistedObservation)
    }

    func stopObserving() {
        observation?.stop()
        observation = nil
    }

    func choose(_ language: AppLanguage) {
        setting.choose(language: language)
        // keep the persisted chip in sync immediately (choose writes through)
        persisted = language
    }

    /// UI-test hook pass-through (B4): wins over persisted + device seeding.
    func setTestOverride(code: String) {
        setting.setTestOverride(code: code)
        effective = setting.current
        strings = setting.strings()
    }
}

/// The Settings screen (bilingual 2/4): three seeding options rendered from
/// the shared tables — System default / Deutsch / English. Tapping writes
/// through `LanguageSetting.choose`, which re-renders the app instantly and
/// (via the events auto-refresh) re-fetches the matching payload family.
struct SettingsView: View {
    let strings: Strings
    @ObservedObject var bridge: LanguageBridge

    var body: some View {
        Form {
            Section(strings.languageTitle()) {
                option(
                    label: strings.languageSystemDefault(),
                    identifier: "languageOption-system",
                    selected: bridge.persisted == AppLanguage.system,
                ) { bridge.choose(AppLanguage.system) }
                option(
                    label: strings.languageGerman(),
                    identifier: "languageOption-de",
                    selected: bridge.persisted == AppLanguage.de,
                ) { bridge.choose(AppLanguage.de) }
                option(
                    label: strings.languageEnglish(),
                    identifier: "languageOption-en",
                    selected: bridge.persisted == AppLanguage.en,
                ) { bridge.choose(AppLanguage.en) }
            }
        }
        .navigationTitle(strings.settingsTitle())
    }

    @ViewBuilder
    private func option(
        label: String,
        identifier: String,
        selected: Bool,
        onTap: @escaping () -> Void,
    ) -> some View {
        Button(action: onTap) {
            HStack {
                Text(label)
                    .foregroundStyle(.primary)
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .foregroundStyle(EvenTomColors.ticketBlue)
                }
            }
        }
        .accessibilityIdentifier(identifier)
    }
}

/// Pair helper: the bridge stops both observations together.
private final class CompositeObservation: UiObservation {
    private let parts: [UiObservation]

    init(_ parts: UiObservation...) {
        self.parts = parts
    }

    func stop() {
        parts.forEach { $0.stop() }
    }
}
