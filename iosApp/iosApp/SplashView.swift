import SwiftUI
import Shared

/// Branded cold-start splash: bright background, centered EVENTOM wordmark in
/// ticket-blue, an animating accent line, and the Eventim-style tagline.
/// Holds ~1.5s, then [onDone] routes: signed in → Events, else → Login
/// (SharedKoin's SessionStore is in memory, so a cold start lands on Login).
struct SplashView: View {
    let onDone: () -> Void

    @State private var accentWidth: CGFloat = 28

    /// UI-testing hook: the hold is normally the spec value (1.5s); XCUITest
    /// launches can't reliably beat that window (launch idle-wait is longer),
    /// so the walkthrough test pins a longer hold via launchEnvironment.
    /// Production: environment unset → 1500ms, bytes-identical behavior.
    private var holdNanos: UInt64 {
        UInt64(Int(ProcessInfo.processInfo.environment["UITEST_SPLASH_HOLD_MS"] ?? "1500") ?? 1500) * 1_000_000
    }

    var body: some View {
        ZStack {
            EvenTomColors.brightBackground.ignoresSafeArea()
            VStack(spacing: 12) {
                EvenTomWordmark()
                Rectangle()
                    .fill(EvenTomColors.ticketBlue)
                    .frame(width: accentWidth, height: 3)
                    .clipShape(Capsule())
                Text("Tickets für Live-Entertainment")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .task {
            withAnimation(.easeIn(duration: 0.5)) {
                accentWidth = 64
            }
            try? await Task.sleep(nanoseconds: holdNanos)
            onDone()
        }
    }
}

#Preview {
    SplashView(onDone: {})
}