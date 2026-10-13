import XCTest

/**
 * Machine walkthrough of the bilingual settings flow (bilingual 2/4):
 * login → Settings tab → switch Deutsch ↔ English via the picker → the
 * tab labels / language screen / events list re-render live (effective
 * StateFlow) and the events payload family follows the new Accept-Language.
 *
 * Evidence: every state dumps the visible UI labels via `print` (captured in
 * the XCUITest console) and asserts the localized marker strings explicitly.
 * Order matters: the run ENDS on Deutsch (the sim's device default family),
 * so the persisted choice stays in the byte-identical language zone the
 * German-pinned walkthrough tests expect.
 */
final class LanguageSwitchUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @discardableResult
    private func wait(_ element: XCUIElement, _ timeout: TimeInterval = 5, _ name: String = "") -> XCUIElement {
        XCTAssertTrue(element.waitForExistence(timeout: timeout), "missing element: \(name.isEmpty ? element.description : name)")
        return element
    }

    private func tapFrame(of element: XCUIElement, in app: XCUIApplication) {
        let f = element.frame
        let appFrame = app.frame
        let v = CGVector(
            dx: (f.midX - appFrame.minX) / appFrame.width,
            dy: (f.midY - appFrame.minY) / appFrame.height,
        )
        app.coordinate(withNormalizedOffset: v).tap()
    }

    private func visibleLabels(in app: XCUIApplication) -> [String] {
        app.staticTexts.allElementsBoundByIndex.prefix(24).map(\.label)
            + app.buttons.allElementsBoundByIndex.prefix(24).map(\.label)
    }

    /**
     * Tab switch with retries: synthesized touches are intermittently dropped
     * right after route/navigation changes on this toolchain (walkthrough
     * precedent); the marker polls until the settings screen really rendered.
     */
    private func openTab(_ index: Int, in app: XCUIApplication, marker: XCUIElement, _ name: String) {
        let tabBar = app.tabBars.firstMatch
        if !marker.waitForExistence(timeout: 1) {
            wait(tabBar, 4, "tab bar")
            for attempt in 0..<3 {
                Thread.sleep(forTimeInterval: 0.8)
                tapFrame(of: tabBar.buttons.element(boundBy: index), in: app)
                if marker.waitForExistence(timeout: 3) { return }
            }
        }
        wait(marker, 3, name.isEmpty ? "tab marker" : name)
    }

    func testSettingsLanguageSwitchWalkthrough() throws {
        let app = XCUIApplication()
        app.launch()

        wait(app.staticTexts["Sign in to see your events"], 8, "login title")

        // login → events (demo credentials)
        let emailField = wait(app.textFields["E-mail"], 3, "e-mail field")
        tapFrame(of: emailField, in: app)
        Thread.sleep(forTimeInterval: 0.4)
        app.typeText("demo@eventom.dev")
        let passwordField = wait(app.secureTextFields["Password"], 3, "password field")
        tapFrame(of: passwordField, in: app)
        Thread.sleep(forTimeInterval: 0.4)
        app.typeText("demo1234")
        let signIn = wait(app.buttons["Sign in"], 2, "sign-in CTA")
        tapFrame(of: signIn, in: app)
        wait(app.staticTexts["Hi, Demo Fan"], 10, "events greeting")

        // open Settings (second tab; label may be in either language — use the bar index)
        let tabBar = app.tabBars.firstMatch
        wait(tabBar, 4, "tab bar")
        openTab(1, in: app, marker: app.buttons["languageOption-en"].firstMatch, "language option EN row")
        print("SMOKE settings (before switch): \(visibleLabels(in: app))")

        // normalize to EN first — persisted choice can be any state from a
        // previous run; this makes the walkthrough deterministic.
        for attempt in 0..<3 {
            Thread.sleep(forTimeInterval: 0.8)
            tapFrame(of: app.buttons["languageOption-en"].firstMatch, in: app)
            if app.buttons["System default"].firstMatch.waitForExistence(timeout: 4) { break }
        }
        XCTAssertTrue(app.buttons["System default"].firstMatch.exists, "EN normalized (picker re-render)")
        print("SMOKE settings (EN normalize): \(visibleLabels(in: app))")
        
        // EN → DE

        // DE: instant re-render + DE events family (marker row is content,
        // independent of the navigationTitle bridge behavior)
        tapFrame(of: app.buttons["languageOption-de"].firstMatch, in: app)
        Thread.sleep(forTimeInterval: 1.0)
        wait(app.buttons["Systemstandard"].firstMatch, 6, "DE system option")
        print("SMOKE settings (Deutsch): \(visibleLabels(in: app))")

        openTab(0, in: app, marker: app.staticTexts["Lachstoff — Abendprogramm"].firstMatch, "DE card title (auto-refresh)")
        wait(app.staticTexts["Lachstoff — Abendprogramm"].firstMatch, 12, "DE card title (auto-refresh)")
        wait(app.staticTexts["Konzerte"].firstMatch, 4, "DE category chip")
        print("SMOKE events (Deutsch): \(visibleLabels(in: app))")

        // EN: back through the picker (row taps retried — dropped-touch rule)
        openTab(1, in: app, marker: app.buttons["languageOption-en"].firstMatch, "language option EN row")
        for attempt in 0..<3 {
            Thread.sleep(forTimeInterval: 0.8)
            tapFrame(of: app.buttons["languageOption-en"].firstMatch, in: app)
            print("SMOKE en-attempt#\(attempt): \(visibleLabels(in: app))")
            if app.buttons["System default"].firstMatch.waitForExistence(timeout: 4) { break }
        }
        wait(app.buttons["System default"].firstMatch, 6, "EN system option")
        print("SMOKE settings (English): \(visibleLabels(in: app))")

        openTab(0, in: app, marker: app.staticTexts["Laughter Fuel — Evening Show"].firstMatch, "EN card title (auto-refresh)")
        wait(app.staticTexts["Laughter Fuel — Evening Show"].firstMatch, 12, "EN card title (auto-refresh)")
        wait(app.staticTexts["Concerts"].firstMatch, 4, "EN category chip")
        print("SMOKE events (English): \(visibleLabels(in: app))")

        // leave the app on the sim-default family (Deutsch) for the walkthrough tests
        openTab(1, in: app, marker: app.buttons["languageOption-de"].firstMatch, "language option DE row")
        for attempt in 0..<3 {
            Thread.sleep(forTimeInterval: 0.8)
            tapFrame(of: app.buttons["languageOption-de"].firstMatch, in: app)
            if app.buttons["Systemstandard"].firstMatch.waitForExistence(timeout: 4) { break }
        }
        XCTAssertTrue(app.buttons["Systemstandard"].firstMatch.exists, "final state: German UI")
        app.terminate()
    }
}