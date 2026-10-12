import SwiftUI

/// Brand tokens + wordmark, mirroring androidApp/Theme.kt (M2 scheme):
/// bright background, Eventim ticket-blue. Keep values in lockstep.
enum EvenTomColors {
    /// #1C6EE8 — Eventim brand blue.
    static let ticketBlue = Color(red: 28 / 255.0, green: 110 / 255.0, blue: 232 / 255.0)
    /// #FBFBFC — bright background.
    static let brightBackground = Color(red: 251 / 255.0, green: 251 / 255.0, blue: 252 / 255.0)
}

/// The EVENTOM wordmark — one face for splash, login and the events header.
/// `compact` is the appbar-sized variant (splash/login use the full face).
struct EvenTomWordmark: View {
    var compact: Bool = false

    var body: some View {
        Text("EVENTOM")
            .font(compact ? .title2.weight(.heavy) : .largeTitle.weight(.heavy))
            .tracking(compact ? 2 : 4)
            .foregroundStyle(EvenTomColors.ticketBlue)
    }
}