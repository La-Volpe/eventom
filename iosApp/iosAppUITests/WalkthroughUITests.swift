import XCTest

/**
 * Machine-run walkthrough of the IOS-2 flows (splash → login → events).
 * Encodes what the human walkthrough would do: the scripted-emulator setup on
 * this machine (no Simulator.app, no idb HID), so these
 * XCUITest functions ARE the walkthrough.
 *
 * Input strategy (found empirically during early diagnostics): this toolchain's
 * element taps abort on AX snapshot re-resolution races, while FRAME READS
 * are reliable — so taps are performed at raw screen coordinates derived
 * from a fresh frame read, and typing goes through `app.typeText` on the
 * focused responder. Assertions poll `exists` over time instead of trusting
 * a single snapshot.
 *
 * Strings mirror the app's UI byte-for-byte (they are also QA contract
 * strings shared with the Android app).
 */
final class WalkthroughUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @discardableResult
    private func wait(_ element: XCUIElement, _ timeout: TimeInterval = 5, _ name: String = "") -> XCUIElement {
        XCTAssertTrue(element.waitForExistence(timeout: timeout), "missing element: \(name.isEmpty ? element.description : name)")
        return element
    }

    /// Polls a predicate against repeated fresh snapshots (survives AX churn).
    private func poll(timeout: TimeInterval = 8, _ predicate: () -> Bool, _ name: String) {
        let stop = Date().addingTimeInterval(timeout)
        while Date() < stop {
            if predicate() { return }
            Thread.sleep(forTimeInterval: 0.3)
        }
        XCTFail("condition not met within \(Int(timeout))s: \(name)")
    }

    /// Raw screen-coordinate tap for an element frame — no element resolution
    /// at tap time (immune to AX snapshot churn).
    private func tapFrame(of element: XCUIElement, in app: XCUIApplication) {
        let f = element.frame
        let appFrame = app.frame
        let v = CGVector(
            dx: (f.midX - appFrame.minX) / appFrame.width,
            dy: (f.midY - appFrame.minY) / appFrame.height,
        )
        app.coordinate(withNormalizedOffset: v).tap()
    }

    /**
     * Taps an element (fresh frame at tap time) and polls for the expected
     * a11y label; retries the tap a few times — synthesized touches are
     * intermittently dropped right after route changes on this toolchain,
     * whichever delivery mechanism is used.
     */
    @discardableResult
    private func toggleUntil(_ element: XCUIElement, in app: XCUIApplication, label: String, attempts: Int = 3) -> Bool {
        for attempt in 0..<attempts {
            Thread.sleep(forTimeInterval: attempt == 0 ? 0.8 : 1.2)
            tapFrame(of: element, in: app)
            let deadline = Date().addingTimeInterval(3.0)
            while Date() < deadline {
                if element.exists && element.label == label { return true }
                Thread.sleep(forTimeInterval: 0.3)
            }
        }
        return element.exists && element.label == label
    }

    private func launchToLogin() -> XCUIApplication {
        let app = XCUIApplication()
        app.launch()
        wait(app.staticTexts["Sign in to see your events"], 6, "login title (routed via splash)")
        return app
    }

    private func fillLogin(app: XCUIApplication, email: String, password: String) {
        let emailField = wait(app.textFields["E-mail"], 3, "e-mail field")
        let passwordField = wait(app.secureTextFields["Password"], 3, "password field")
        tapFrame(of: emailField, in: app)
        Thread.sleep(forTimeInterval: 0.5)
        app.typeText(email)
        Thread.sleep(forTimeInterval: 0.5)
        tapFrame(of: passwordField, in: app)
        Thread.sleep(forTimeInterval: 0.5)
        app.typeText(password)
        Thread.sleep(forTimeInterval: 0.5)
    }

    // MARK: - splash

    /// Splash content, deterministically: with the env-pinned hold the window
    /// is comfortably longer than XCUITest's launch idle-wait (which cannot
    /// reliably beat the real 1.5s flight timing). No env → production
    /// behavior unchanged (that timing is exercised by every other test).
    func testSplashShowsWordmarkAndTagline() throws {
        let app = XCUIApplication()
        app.launchEnvironment["UITEST_SPLASH_HOLD_MS"] = "10000"
        app.launch()

        wait(app.staticTexts["EVENTOM"], 4, "splash wordmark")
        wait(app.staticTexts["Tickets für Live-Entertainment"], 4, "splash tagline")
        XCTAssertFalse(
            app.staticTexts["Sign in to see your events"].exists,
            "splash is still holding — login must not have appeared yet",
        )
        app.terminate()
    }

    // MARK: - login form (asserted on a normal launch)

    func testLoginFormElementsAfterSplash() throws {
        let app = launchToLogin()

        wait(app.textFields["E-mail"], 3, "e-mail field")
        XCTAssertTrue(app.secureTextFields["Password"].exists, "password secure field")
        wait(app.buttons["Sign in"], 3, "sign-in CTA")
        wait(app.staticTexts["Demo account: demo@eventom.dev · demo1234"], 3, "demo hint")
    }

    // MARK: - login → events walkthrough

    func testValidLoginWalkthroughToEvents() throws {
        let app = launchToLogin()

        fillLogin(app: app, email: "demo@eventom.dev", password: "demo1234")

        // Sign-in proceeds through the fake backend into the events list.
        let signIn = wait(app.buttons["Sign in"], 2, "sign-in CTA")
        tapFrame(of: signIn, in: app)
        XCTAssertTrue(
            app.staticTexts["Hi, Demo Fan"].waitForExistence(timeout: 10),
            "events header greeting after successful login",
        )
        Thread.sleep(forTimeInterval: 1.0)

        // Chips row: "All" is first and always exposed. Newer iOS runtimes
        // do not expose the horizontally scrolled sibling chips to the AX
        // tree until dragged into view — drag, then accept any surfacing
        // German label (byte-parity is also asserted on card texts below).
        poll(timeout: 6, { app.buttons["All"].exists }, "All chip")
        let chipDragStart = app.coordinate(withNormalizedOffset: CGVector(dx: 0.75, dy: 0.157))
        let chipDragEnd = app.coordinate(withNormalizedOffset: CGVector(dx: 0.25, dy: 0.157))
        chipDragStart.press(forDuration: 0.2, thenDragTo: chipDragEnd)
        poll(timeout: 6, {
            ["Konzerte", "Musical & Show", "Sport", "Comedy & Humor", "Theater & Kultur", "Freizeit"]
                .contains { app.buttons[$0].exists || app.staticTexts[$0].exists }
        }, "a category chip beyond All after drag")

        // First card (sorted by start time: Lachstoff, id 4) with poster thumb.
        // The image surfaces in AX either by identifier (posterThumb-<id>) or
        // with the ASSET NAME as its label — both verified; accept both.
        poll(timeout: 10, { app.images["posterThumb-4"].exists || app.images["PosterEvent4"].exists }, "poster thumbnail of first card")
        poll(timeout: 4, { app.staticTexts["Lachstoff — Abendprogramm"].exists }, "first card title")
        poll(timeout: 3, { app.staticTexts["ab 39.90 €"].exists }, "price-from on card")
        poll(timeout: 3, { app.staticTexts["Comedy & Humor"].exists }, "card category label")

        // Second card renders too.
        poll(timeout: 4, { app.staticTexts["Stadtderby: Alster Adler gegen Rhein Löwen"].exists }, "second card title")

        // Save / unsave round trip on the first card (identifier-addressed;
        // viewable text ☆ Save / ★ Saved, a11y label Save / Unsave). Tapped
        // via raw screen fractions — the button frame is layout-stable
        // ({328, 186, 46×17.7}pt on the 402×874pt simulator), so this is
        // zero-resolution input with no AX race surface at all.
        // Save / unsave round trip on the first card. Tapped via raw screen
        // fractions (layout-stable {328, 186, 46×17.7}pt on the 402×874pt
        // simulator) after generous settles — the events screen's AX tree
        // accepts synthesized touches unreliably until fully settled.
        // Toolchain quirk: the first synthesized touch after a route change is
        // intermittently dropped; a benign warm-up tap on inert, non-
        // interactive real estate (the card body) stabilizes the next taps.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.25)).tap()
        Thread.sleep(forTimeInterval: 0.8)

        let saveToggle = app.buttons["saveToggle-4"]
        poll(timeout: 6, { saveToggle.exists }, "save control present")
        // Normalize the entry state: test methods share one runner process
        // (the Koin singletons outlive a method), and earlier journeys leave
        // the toggle in either state.
        if !saveToggle.exists || saveToggle.label != "Save" {
            XCTAssertTrue(toggleUntil(saveToggle, in: app, label: "Save"), "normalized to unsaved before the round trip")
        }
        XCTAssertTrue(toggleUntil(saveToggle, in: app, label: "Unsave"), "saved state after tapping save")
        XCTAssertTrue(toggleUntil(saveToggle, in: app, label: "Save"), "unsaved again")

        // Chips row scrolls horizontally to reach later categories.
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: 0.88, dy: 0.157))
        let end = app.coordinate(withNormalizedOffset: CGVector(dx: 0.2, dy: 0.157))
        start.press(forDuration: 0.25, thenDragTo: end)
        poll(timeout: 5, { app.staticTexts["Theater & Kultur"].exists || app.buttons["Theater & Kultur"].exists }, "later chip reachable via horizontal scroll")

        // Refresh keeps the list.
        let refresh = wait(app.buttons["Refresh"], 3, "refresh control")
        tapFrame(of: refresh, in: app)
        poll(timeout: 6, { app.staticTexts["Lachstoff — Abendprogramm"].exists }, "list still populated after refresh")
    }

    // MARK: - detail journey (IOS-3)

    /// Taps a card by its title's frame (raw screen coordinates), waits for
    /// the detail, runs CTA + back flows.
    private func openDetail(app: XCUIApplication, title: String) {
        let titleText = app.staticTexts[title].firstMatch
        poll(timeout: 6, { titleText.exists }, "card title to tap: \(title)")
        Thread.sleep(forTimeInterval: 0.8)
        tapFrame(of: titleText, in: app)
        Thread.sleep(forTimeInterval: 1.0)
    }

    private func assertHeroAndSections(app: XCUIApplication, posterAsset: String) {
        poll(timeout: 10, { app.buttons["detailBack"].exists }, "detail back control (a11y Back)")
        poll(timeout: 10, { app.images["posterHero-4"].exists || app.images["PosterEvent4"].exists }, "detail hero poster")
        poll(timeout: 8, { app.staticTexts["detailCategoryChip"].exists || app.staticTexts["Musical & Show"].exists }, "category chip present")
        poll(timeout: 8, { app.staticTexts["About this event"].exists }, "About section")
        poll(timeout: 6, { app.staticTexts["Line-up"].exists }, "Line-up section")
        poll(timeout: 6, { app.staticTexts["Doors: 17:15"].exists }, "doors info")
        poll(timeout: 6, { app.staticTexts["Ticket categories"].exists }, "ticket tiers section")
    }

    func testDetailJourneyFromEvents() throws {
        let app = launchToLogin()
        try fillLogin(app: app, email: "demo@eventom.dev", password: "demo1234")
        let signIn = wait(app.buttons["Sign in"], 2, "sign-in CTA")
        tapFrame(of: signIn, in: app)
        poll(timeout: 10, { app.staticTexts["Hi, Demo Fan"].exists }, "events greeting")
        Thread.sleep(forTimeInterval: 1.5)

        // Warm-up (toolchain: first post-route touch is intermittently dropped).
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.25)).tap()
        Thread.sleep(forTimeInterval: 0.8)

        // Save ON first so we can prove the state survives the detail round trip.
        let saveToggle = app.buttons["saveToggle-4"]
        poll(timeout: 6, { saveToggle.exists }, "save control present")
        if !saveToggle.exists || saveToggle.label != "Save" {
            XCTAssertTrue(toggleUntil(saveToggle, in: app, label: "Save"), "normalized to unsaved before the journey")
        }
        XCTAssertTrue(toggleUntil(saveToggle, in: app, label: "Unsave"), "saved before detail")

        // --- Lachstoff detail ---
        openDetail(app: app, title: "Lachstoff — Abendprogramm")

        poll(timeout: 10, { app.buttons["detailBack"].exists }, "detail back control")
        poll(timeout: 10, { app.images["posterHero-4"].exists || app.images["PosterEvent4"].exists }, "detail hero poster")
        poll(timeout: 8, { app.staticTexts["detailCategoryChip"].exists || app.staticTexts["Musical & Show"].exists }, "category chip present")
        poll(timeout: 8, { app.staticTexts["About this event"].exists }, "About section")
        poll(timeout: 6, { app.staticTexts["Line-up"].exists }, "Line-up section")
        poll(timeout: 6, { app.staticTexts["Doors: 17:15"].exists }, "doors info")
        poll(timeout: 6, { app.staticTexts["Ticket categories"].exists }, "ticket tiers section")

        // Sticky CTA + Offline-Demo feedback (in-place banner, Android Snackbar parity).
        let cta = wait(app.buttons["Tickets kaufen"], 6, "Tickets kaufen CTA")
        Thread.sleep(forTimeInterval: 0.6)
        tapFrame(of: cta, in: app)
        poll(timeout: 6, { app.staticTexts["Offline-Demo"].exists }, "Offline-Demo feedback appears")
        // Screen stays: the CTA is still there.
        XCTAssertTrue(app.buttons["Tickets kaufen"].exists, "detail stays after demo CTA")

        // NavigationStack route evidence: the pushed detail hides the tab bar
        // (NavigationStack sits above the TabView), and it returns on pop.
        poll(timeout: 6, { !app.tabBars.buttons["Events"].exists }, "tab bar hidden while detail is pushed")

        // Back → Events with state intact.
        let back = app.buttons["detailBack"]
        poll(timeout: 5, { back.exists }, "back control present")
        Thread.sleep(forTimeInterval: 0.6)
        tapFrame(of: back, in: app)
        poll(timeout: 10, { app.staticTexts["Hi, Demo Fan"].exists }, "greeting intact after back")
        poll(timeout: 8, { app.tabBars.buttons["Events"].exists }, "tab bar restored after pop")
        poll(timeout: 6, { saveToggle.exists && saveToggle.label == "Unsave" }, "saved state intact after detail journey")

        // --- Stadtderby (id 3) route as well ---
        openDetail(app: app, title: "Stadtderby: Alster Adler gegen Rhein Löwen")
        poll(timeout: 10, { app.images["posterHero-3"].exists || app.images["PosterEvent3"].exists }, "stadtderby hero")
        poll(timeout: 8, { app.staticTexts["Doors: 14:00"].exists }, "stadtderby doors line")
        poll(timeout: 6, { app.staticTexts["Ticket categories"].exists }, "stadtderby tiers")
        let back2 = app.buttons["detailBack"]
        poll(timeout: 5, { back2.exists }, "back control present")
        Thread.sleep(forTimeInterval: 0.6)
        tapFrame(of: back2, in: app)
        poll(timeout: 10, { app.staticTexts["Hi, Demo Fan"].exists }, "back on events again")
    }

    // MARK: - sign-out cycle

    func testSignOutCycle() throws {
        let app = launchToLogin()
        try fillLogin(app: app, email: "demo@eventom.dev", password: "demo1234")
        let signIn = wait(app.buttons["Sign in"], 2, "sign-in CTA")
        tapFrame(of: signIn, in: app)
        poll(timeout: 10, { app.staticTexts["Hi, Demo Fan"].exists }, "signed in greeting")

        // Sign out → Login (fresh form via reset). Warm-up + retry: the
        // header buttons are narrow and the first post-route touch is
        // intermittently dropped on this toolchain.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.25)).tap()
        Thread.sleep(forTimeInterval: 0.8)
        let signOut = wait(app.buttons["Sign out"], 8, "sign out control")
        tapFrame(of: signOut, in: app)
        var backOnLogin = app.staticTexts["Sign in to see your events"].waitForExistence(timeout: 4)
        if !backOnLogin {
            Thread.sleep(forTimeInterval: 0.8)
            tapFrame(of: signOut, in: app)
            backOnLogin = app.staticTexts["Sign in to see your events"].waitForExistence(timeout: 5)
        }
        XCTAssertTrue(backOnLogin, "back on login after sign-out")
        XCTAssertTrue(app.textFields["E-mail"].exists, "login form present")
        XCTAssertTrue(app.staticTexts["Demo account: demo@eventom.dev · demo1234"].exists, "demo hint present")

        // Sign in again → Events.
        try fillLogin(app: app, email: "stella@eventom.dev", password: "sommer1")
        let signInAgain = wait(app.buttons["Sign in"], 2, "sign-in CTA")
        tapFrame(of: signInAgain, in: app)
        poll(timeout: 10, { app.staticTexts["Hi, Stella Sommer"].exists }, "greeting for re-signed-in user")
    }

    // MARK: - invalid login

    func testWrongPasswordShowsInlineErrorAndStaysOnLogin() throws {
        let app = launchToLogin()

        fillLogin(app: app, email: "demo@eventom.dev", password: "hunter2")

        let signIn = wait(app.buttons["Sign in"], 2, "sign-in CTA")
        tapFrame(of: signIn, in: app)

        poll(timeout: 8, { app.staticTexts["Wrong e-mail or password"].exists }, "inline 401 error")
        XCTAssertTrue(app.staticTexts["Sign in to see your events"].exists, "still on login")
        XCTAssertTrue(app.textFields["E-mail"].exists, "e-mail retained")
    }
}