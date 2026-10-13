import SwiftUI
import Shared

/// Republishes `EventDetailViewModel` state through the shared concrete
/// `observe(onChange:)` API (same bridge pattern as auth/events).
final class EventDetailBridge: ObservableObject {
    let viewModel: EventDetailViewModel

    @Published var state: EventDetailUiState

    private var observation: UiObservation?

    init(viewModel: EventDetailViewModel = SharedKoin.shared.eventDetailViewModel()) {
        self.viewModel = viewModel
        self.state = EventDetailUiState.Idle.shared
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

/// Eventim-style product page (IOS-3), parity with the Android detail screen.
/// The bitmap already carries the event title in the art, so the hero keeps
/// the category accent + back control; title/date/venue/price render in the
/// stacked info block below (also the fallback-safe choice: category-fallback
/// artwork shows no title).
struct EventDetailView: View {
    let eventId: Int64
    let strings: Strings
    let onBack: () -> Void

    @StateObject private var bridge: EventDetailBridge
    @State private var showOfflineDemo = false

    init(eventId: Int64, strings: Strings, onBack: @escaping () -> Void) {
        self.eventId = eventId
        self.strings = strings
        self.onBack = onBack
        _bridge = StateObject(wrappedValue: EventDetailBridge())
    }

    var body: some View {
        let state = bridge.state
        ZStack(alignment: .bottom) {
            content(state: state)
                .background(EvenTomColors.brightBackground.ignoresSafeArea())
                .onAppear {
                    bridge.startObserving()
                    if bridge.state is EventDetailUiState.Idle {
                        bridge.viewModel.load(eventId: eventId)
                    }
                }
                .onDisappear {
                    bridge.stopObserving()
                    // One owner of the reset — the root calls reset() on back;
                    // a process-restore landing here reloads via the Idle branch.
                }
        }
        .onChange(of: stateTag(of: bridge.state)) { _ in
            if state is EventDetailUiState.Idle {
                bridge.viewModel.load(eventId: eventId)
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if state is EventDetailUiState.Success {
                ctaBar
            }
        }
        // "Offline-Demo" feedback: in-place banner anchored above the CTA bar
        // (Android uses a Snackbar with the same string), auto-dismissing.
        .overlay(alignment: .bottom) {
            if showOfflineDemo {
                Text(strings.offlineDemoFeedback())
                    .font(.footnote.weight(.semibold))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.8))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
                    .padding(.bottom, 106)
                    .transition(.opacity)
                    .task {
                        try? await Task.sleep(nanoseconds: 2_000_000_000)
                        withAnimation(.easeOut(duration: 0.2)) { showOfflineDemo = false }
                    }
            }
        }
    }

    private func stateTag(of state: EventDetailUiState) -> String {
        String(describing: type(of: state))
    }

    // MARK: CTA bar (sticky, safe-area aware)

    private var ctaBar: some View {
        VStack(spacing: 0) {
            Button(action: {
                withAnimation(.easeOut(duration: 0.2)) { showOfflineDemo = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    withAnimation(.easeOut(duration: 0.2)) { showOfflineDemo = false }
                }
            }) {
                Text(strings.buyTicketsCta())
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity, minHeight: 52)
            }
            .background(EvenTomColors.ticketBlue)
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
        .background(
            // Scrim-style bar ground over the bright background.
            Rectangle()
                .fill(EvenTomColors.brightBackground)
                .shadow(color: .black.opacity(0.12), radius: 6, y: -2),
            alignment: .top,
        )
    }

    private var offlineDemoBanner: some View {
        Text(strings.offlineDemoFeedback())
            .font(.footnote.weight(.semibold))
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color.black.opacity(0.8))
            .foregroundStyle(.white)
            .clipShape(Capsule())
            .padding(.bottom, 106)
            .transition(.opacity)
    }

    // MARK: states

    @ViewBuilder
    private func content(state: EventDetailUiState) -> some View {
        if state is EventDetailUiState.Loading || state is EventDetailUiState.Idle {
            VStack(spacing: 12) {
                Spacer()
                ProgressView()
                    .tint(EvenTomColors.ticketBlue)
                    .scaleEffect(1.4)
                Text(strings.loadingDetails())
                    .font(.subheadline)
                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = state as? EventDetailUiState.Error {
            VStack(spacing: 16) {
                Spacer()
                Text(error.message)
                    .font(.body)
                    .multilineTextAlignment(.center)
                Button(strings.tryAgain()) {
                    bridge.viewModel.load(eventId: eventId)
                }
                .buttonStyle(.borderedProminent)
                .tint(EvenTomColors.ticketBlue)
                Button(strings.backToEvents()) { onBack() }
                    .buttonStyle(.bordered)
                Spacer()
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let success = state as? EventDetailUiState.Success {
            EventDetailContent(
                event: success.event,
                detail: success.detail,
                strings: strings,
                onBack: onBack,
            )
        }
    }
}

// MARK: content

private struct EventDetailContent: View {
    let event: Event
    let detail: EventDetail
    let strings: Strings
    let onBack: () -> Void

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = .current
        f.dateFormat = "EEE, dd.MM.yyyy · HH:mm"
        return f
    }()

    private static func date(from instant: KotlinInstant) -> Date {
        Date(
            timeIntervalSince1970: Double(instant.epochSeconds) + Double(instant.nanosecondsOfSecond) / 1_000_000_000
        )
    }

    private var priceText: String? {
        event.priceFrom.map { String(format: "ab %.2f €", $0.doubleValue) }
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                hero
                infoBlock
                if let priceText {
                    Spacer().frame(height: 16)
                    Text(priceText)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(EvenTomColors.ticketBlue)
                    Spacer().frame(height: 16)
                }
                about
                if !detail.lineup.isEmpty {
                    Spacer().frame(height: 16)
                    lineup
                }
                Spacer().frame(height: 16)
                info
                if !detail.ticketCategories.isEmpty {
                    Spacer().frame(height: 16)
                    ticketCategories
                }
                Spacer().frame(height: 24)
            }
        }
    }

    // MARK: hero (full-bleed poster + scrim + back + category chip)

    private var hero: some View {
        ZStack(alignment: .bottomLeading) {
            GeometryReader { geo in
                Image(posterImageName(imageRef: event.imageRef, category: event.category))
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: geo.size.width, height: geo.size.height, alignment: .top) // in-art title stays out of frame
                    .clipped()
                    .accessibilityLabel("Poster of \(event.title)")
                    .accessibilityIdentifier("posterHero-\(event.id)")
            }
            .frame(height: 230)

            // Scrim: transparent art up top, dark toward the chip.
            GeometryReader { geo in
                LinearGradient(
                    stops: [
                        .init(color: .black.opacity(0), location: 0),
                        .init(color: .black.opacity(0.35), location: 0.55),
                        .init(color: .black.opacity(0.72), location: 1.0),
                    ],
                    startPoint: .top, endPoint: .bottom,
                )
                .frame(width: geo.size.width, height: geo.size.height)
            }
            .frame(height: 230)

            // Back affordance on the hero, clear of the status bar.
            Button(action: onBack) {
                Text("‹")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .background(Color.black.opacity(0.35), in: Circle())
            }
            .accessibilityLabel(strings.backLabel())
            .accessibilityIdentifier("detailBack")
            .padding(.leading, 12)
            .padding(.top, 4) // status-bar clearance via hero's bottom-leading alignment trick
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

            // Category accent chip (over the dark scrim's ground).
            Text(strings.category(category: event.category))
                .font(.caption.weight(.semibold))
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(EvenTomColors.ticketBlue, in: RoundedRectangle(cornerRadius: 8))
                .foregroundStyle(.white)
                .padding(16)
                .accessibilityIdentifier("detailCategoryChip")
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
        }
        .frame(height: 230)
    }

    // MARK: stacked info block

    private var infoBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Below the artwork: legible on the bright background on BOTH
            // paths (the category-fallback bitmap carries no title).
            Text(event.title)
                .font(.title2.weight(.bold))
            Text("\(Self.dateFormatter.string(from: Self.date(from: event.startTime))) · \(event.venue), \(event.city)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(16)
    }

    private var info: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.infoTitle())
                .font(.headline)
            if !detail.doorsTime.isEmpty {
                Text(strings.doorsLabel(time: detail.doorsTime))
                    .font(.subheadline)
            }
            if !detail.ageInfo.isEmpty {
                Text(detail.ageInfo)
                    .font(.subheadline)
            }
        }
        .padding(.horizontal, 16)
    }

    private var about: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.aboutTitle())
                .font(.headline)
            Text(detail.longSummary)
                .font(.subheadline)
        }
        .padding(.horizontal, 16)
    }

    private var lineup: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.lineupTitle())
                .font(.headline)
            ForEach(Array(detail.lineup.enumerated()), id: \.offset) { _, act in
                Text("•  \(act)")
                    .font(.subheadline)
            }
        }
        .padding(.horizontal, 16)
    }

    private var ticketCategories: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strings.ticketCategoriesTitle())
                .font(.headline)
            ForEach(Array(detail.ticketCategories.enumerated()), id: \.offset) { _, ticket in
                HStack {
                    Text(ticket.name)
                        .font(.subheadline)
                    Spacer()
                    Text(String(format: "%.2f €", ticket.price))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(EvenTomColors.ticketBlue)
                }
                .padding(.vertical, 10)
                Divider()
            }
        }
        .padding(.horizontal, 16)
    }
}


#Preview {
    EventDetailView(eventId: 4, strings: StringsDe.shared, onBack: {})
}
