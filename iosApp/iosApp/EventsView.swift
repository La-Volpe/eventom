import SwiftUI
import Shared

/**
 * The SwiftUI half of the events feature. It renders a Kotlin StateFlow:
 * `EventsBridge` (below) observes `EventsViewModel` from `:shared` through
 * the shared `observe(onChange:)` API and republishes as ObservableObject.
 */
final class EventsBridge: ObservableObject {
    let viewModel: EventsViewModel

    @Published var state: EventsUiState

    private var observation: UiObservation?

    init(viewModel: EventsViewModel = SharedKoin.shared.eventsViewModel()) {
        self.viewModel = viewModel
        // Kotlin data classes have no default constructor in Swift, so the
        // shared module exposes `EventsUiState.Companion.initial()`.
        self.state = EventsUiState.companion.initial()
    }

    func startObserving() {
        guard observation == nil else { return }
        observation = viewModel.observe(onChange: { [weak self] newState in
            self?.state = newState
        })
    }

    func stopObserving() {
        observation?.stop()
        observation = nil
    }
}

/// Republishes the signed-in user for the header ("Hi, {name}") via the
/// shared concrete `observeCurrentUser(onChange:)`.
final class SessionBridge: ObservableObject {
    @Published var userName: String?

    // Composition-root resolution happens at bridge construction (T10: views
    // never resolve the locator mid-view — the view calls closures only).
    private let session: SessionStore

    init(session: SessionStore = SharedKoin.shared.sessionStore()) {
        self.session = session
    }

    /// Sign-out flows through the bridge; the view only calls closures.
    func signOut() {
        session.signOut()
    }
    private var observation: UiObservation?

    func startObserving() {
        guard observation == nil else { return }
        observation = session.observeCurrentUser { [weak self] user in
            self?.userName = user?.name
        }
    }

    func stopObserving() {
        observation?.stop()
        observation = nil
    }
}


struct EventsView: View {
    let strings: Strings
    // Language re-render hook (bilingual 2/4, B5): the root-owned bridge
    // triggers a refresh on effective language change — the payload family
    // the visible tab shows follows the new Accept-Language, saved flags
    // carry over through the repository.
    let languageBridge: LanguageBridge
    let onSignIn: () -> Void
    let onSignOut: () -> Void
    var onOpenDetail: (Int64) -> Void = { _ in }

    @StateObject private var bridge = EventsBridge()
    @StateObject private var sessionBridge = SessionBridge()

    var body: some View {
        VStack(spacing: 0) {
            header(state: bridge.state)
            filterChips(state: bridge.state)
            content(state: bridge.state)
        }
        .background(EvenTomColors.brightBackground.ignoresSafeArea())
        .onAppear {
            bridge.startObserving()
            sessionBridge.startObserving()
            languageBridge.startObserving()
            // B5: language change → refresh falls out of the shared bridge.
            languageBridge.onEffectiveChanged = { [weak bridge] _ in
                bridge?.viewModel.refresh()
            }
        }
        .onDisappear {
            bridge.stopObserving()
            sessionBridge.stopObserving()
        }
    }

    // MARK: header

    @ViewBuilder
    private func header(state: EventsUiState) -> some View {
        HStack(spacing: 8) {
            EvenTomWordmark(compact: true)
            Spacer()
            if let userName = sessionBridge.userName {
                Text(strings.eventsGreeting(name: userName))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(EvenTomColors.ticketBlue)
                Button(strings.signOutChip()) {
                    sessionBridge.signOut()
                    onSignOut()
                }
                .font(.footnote)
                .buttonStyle(.plain)
            } else {
                Button(strings.signInChip()) { onSignIn() }
                    .font(.footnote)
                    .buttonStyle(.plain)
            }
            if state.isRefreshing {
                ProgressView()
                    .frame(height: 20)
            } else {
                Button(strings.refreshLabel()) { bridge.viewModel.refresh() }
                    .font(.footnote)
                    .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    // MARK: offline-banner

    @ViewBuilder
    private func offlineBanner(state: EventsUiState) -> some View {
        if let message = state.message {
            Text(message)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .padding(.horizontal)
        }
    }

    // MARK: chips (single-line horizontal scroll — do not overflow-measure!)

    @ViewBuilder
    private func filterChips(state: EventsUiState) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip(title: strings.allChip(), selected: state.selectedCategory == nil) {
                    bridge.viewModel.select(category: nil)
                }
                ForEach(state.categories, id: \.name) { category in
                    chip(title: strings.category(category: category), selected: state.selectedCategory == category) {
                        bridge.viewModel.select(category: category)
                    }
                }
            }
            .padding(.horizontal, 16)
        }
        .frame(height: 44) // fixed row height — chips never reflow vertically
    }

    private func chip(title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.footnote.weight(.medium))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(selected ? AnyShapeStyle(EvenTomColors.ticketBlue.opacity(0.2)) : AnyShapeStyle(Color.primary.opacity(0.06)))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: content

    @ViewBuilder
    private func content(state: EventsUiState) -> some View {
        if state.isLoading {
            Spacer()
            ProgressView()
            Spacer()
        } else if state.events.isEmpty {
            Spacer()
            Text(strings.emptyList())
                .foregroundStyle(.secondary)
            Spacer()
        } else {
            ScrollView {
                VStack(spacing: 12) {
                    offlineBanner(state: state)
                    ForEach(state.events, id: \.id) { event in
                        EventCard(
                            event: event,
                            strings: strings,
                            onToggleSaved: { bridge.viewModel.toggleSaved(id: event.id) },
                            onOpen: { onOpenDetail(event.id) },
                        )
                    }
                }
                .padding(16)
            }
        }
    }
}

// MARK: event card

private struct EventCard: View {
    let event: Event
    let strings: Strings
    let onToggleSaved: () -> Void
    let onOpen: () -> Void

    private static let timeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "EEE d MMM · HH:mm"
        return formatter
    }()

    /// kotlin.time.Instant → Foundation Date (no ObjC helper is exported).
    private static func date(from instant: KotlinInstant) -> Date {
        Date(
            timeIntervalSince1970: Double(instant.epochSeconds) + Double(instant.nanosecondsOfSecond) / 1_000_000_000
        )
    }

    var body: some View {
        HStack(alignment: .center) {
            // Poster thumbnail (offline artwork). The AX element surfaces with
            // the asset name as its label (observed stable in the AX tree);
            // the label/identifier below are best-effort extras for VO/tests.
            Image(posterThumbImageName(imageRef: event.imageRef, category: event.category))
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 104, height: 104)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .accessibilityLabel("Poster of \(event.title)")
                .accessibilityIdentifier("posterThumb-\(event.id)")
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                Text(strings.category(category: event.category))
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(EvenTomColors.ticketBlue)
                    Spacer()
                    Button(action: onToggleSaved) {
                        Text(event.isSaved ? strings.savedLabel() : strings.saveLabel())
                            .font(.footnote)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(event.isSaved ? "Unsave" : "Save")
                    .accessibilityIdentifier("saveToggle-\(event.id)")
                }
                Text(event.title)
                    .font(.headline)
                if !event.summary.isEmpty {
                    Text(event.summary)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                Text(
                    "\(Self.timeFormatter.string(from: Self.date(from: event.startTime))) · \(event.venue), \(event.city)"
                )
                .font(.caption)
                .foregroundStyle(.tertiary)
                if let priceFrom = event.priceFrom {
                    Text(String(format: "ab %.2f €", priceFrom.doubleValue))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(EvenTomColors.ticketBlue)
                }
            }
        }
        .contentShape(RoundedRectangle(cornerRadius: 14))
        .onTapGesture { onOpen() }
        .padding(12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .shadow(color: .black.opacity(0.06), radius: 4, y: 1)
    }
}

#Preview {
    EventsView(
        strings: StringsDe.shared,
        languageBridge: LanguageBridge(),
        onSignIn: {},
        onSignOut: {},
        onOpenDetail: { _ in },
    )
}