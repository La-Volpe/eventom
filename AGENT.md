# AGENT.md — EvenTom

Guide for AI agents (and humans) working in this repo: architecture, commands,
and the platform gotchas already paid for. Read before changing anything.

## What this project is

An Eventim-style ticketing demo (splash → login → events → detail):
**`shared/` owns everything non-UI**; each platform renders natively —
Jetpack Compose (`androidApp/`) and SwiftUI (`iosApp/`) — at feature parity.
If you find yourself adding business logic to either app module, stop: it
belongs in `shared/`. The app modules only render shared state and forward
user actions back into shared view models.

```
shared/            domain (model/repository/usecase) · data (Ktor + fake backend + fixtures)
                   presentation (StateFlow view models) · di (Koin) · platform (expect/actual)
androidApp/        Compose UI only (plain Android app)
iosApp/            SwiftUI only + Xcode project consuming Shared.framework
```

Data flow, in one line: `FakeBackendEngine` (Ktor `MockEngine` over the
`api/*.json` fixtures) → typed data sources (events / event-details / auth)
→ repositories → view models (`StateFlow` ui states) → Compose
`collectAsStateWithLifecycle` / SwiftUI bridges over `observe(onChange:)`.
The app runs fully offline; `BackendConfig.useFakeBackend = false` points the
HTTP boundary back at a real engine. Strings shown in the two apps are
byte-identical (the XCUITest walkthrough asserts fixture strings) — never
reword one platform.

## Commands

```bash
./gradlew build                                   # everything + all shared tests
./gradlew :shared:testAndroidHostTest             # common tests on JVM
./gradlew :shared:testIosSimulatorArm64Test       # common tests on iOS simulator  (task: :shared:iosSimulatorArm64Test)
./gradlew :androidApp:installDebug                # run Android app
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build   # build iOS app
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro'        # iOS done-gate: XCUITest walkthrough (6 test funcs)
open iosApp/iosApp.xcodeproj                      # run iOS app from Xcode (⌘R)
```

Careful with device names: two "iPhone 17 Pro" simulators exist on this
machine (an x86_64 iOS 26.4 one that never boots on arm64, and the working
arm64 iOS 26.5 one). Resolve simulators by UDID or by the `(Booted)` state,
not by name.

Verify an iOS change at runtime without GUI:

```bash
xcodebuild ... build   # as above
APP="$(ls -d ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphonesimulator/iosApp.app | head -1)"
xcrun simctl boot "iPhone 17 Pro" 2>/dev/null
xcrun simctl install "iPhone 17 Pro" "$APP"
xcrun simctl launch "iPhone 17 Pro" dev.evantom.iosApp
xcrun simctl io "iPhone 17 Pro" screenshot /tmp/s.png
```

If the app crashes at launch, read
`~/Library/Logs/DiagnosticReports/iosApp-*.ips` (parse `.ips` as JSON) and/or
`xcrun simctl launch --console-pty "iPhone 17 Pro" dev.evantom.iosApp` — the
latter prints uncaught Kotlin exceptions in full. This is the fastest way to
debug shared-code crashes on iOS.

## Version matrix (verified working together as of 2026-10; bump carefully)

| Layer | Choice |
|---|---|
| Gradle | 9.8.1 (wrapper) |
| AGP | 9.4.1 — **new KMP world**: `shared` uses `com.android.kotlin.multiplatform.library`, NOT `com.android.library` |
| Kotlin | 2.4.20 |
| Compose | BOM 2026.09.00, plugin `org.jetbrains.kotlin.plugin.compose` |
| DI | Koin 4.2.2 (`io.insert-koin`) |
| Ktor | 3.6.0 (OkHttp / Darwin engines via expect/actual; `ktor-client-mock` on the same pin — the fake backend) |
| compile/target SDK | 37; minSdk 26; iOS min 16.0 |
| Xcode | 27 — Simulators are driven via `simctl`/`idb`; there is no standalone Simulator.app in Xcode 27 (DeviceHub hosts the device UI) |

All pinned in `gradle/libs.versions.toml`.

## Gotchas already paid for — do not regress

1. **Kotlin 2.4 does not create default framework binaries for Apple targets.**
   `shared/build.gradle.kts` registers them explicitly:
   `binaries { framework { baseName = "Shared" } }` on each iOS target. Without
   it, the Xcode build phase fails with
   `task 'embedAndSignAppleFrameworkForXcode' not found`.

2. **AGP 9 rule for KMP modules** (`shared/`): `kotlin { android { namespace =
   ...; compileSdk = ...; minSdk = ... } }` inside the `kotlin { }` block, and
   there is **no top-level `android { }`** with
   `com.android.kotlin.multiplatform.library`. In the app module
   (`androidApp/`), AGP 9 has built-in Kotlin support — never apply
   `org.jetbrains.kotlin.android` there (AGP errors on it); only
   `com.android.application` + `org.jetbrains.kotlin.plugin.compose`.

3. **Source-set layout differs by module**: `shared` uses the KMP layout
   (`src/androidMain/`, `src/iosMain/`); `androidApp` is a *plain* Android
   module and uses `src/main/` (manifest, res, kotlin) — NOT `src/androidMain`.

4. **Swift interop: avoid the property name `description`** on any shared data
   class — Swift resolves it to `NSObject.description` and silently renders the
   Kotlin `toString()` (that's why `Event.summary`, not `Event.description`).
   New shared DTOs/models: keep the rule for any name that NSObject owns
   (`description`, `hash`, `debugDescription`, `isProxy`, `class`…).

5. **In shared view models, Swift-facing APIs must be concrete.** Generic
   type parameters erase over the ObjC boundary. `EventsViewModel.observe
   (onChange: (EventsUiState) -> Unit)` exposes a clean closure; if you add a
   generic `fun interface`/generic method to the Swift surface, Swift gets
   `Any`-typed params. Kotlin collections (`List<T>`) bridge fine; data-class
   constructors require **all** parameters in Swift (no default args) — that's
   why `EventsUiState.Companion.initial()` exists; give data classes a
   `companion fun initial()` (or `zero`) if the UI must construct them.

6. **Koin: type definitions explicitly** — `single<Clock> { Clock.System }`,
   `single<EventRepository> { EventRepositoryImpl(...) }`. Koin keys on the
   *expression* type; `single { Clock.System }` silently registers
   `Clock.System`, then resolution of the interface fails at runtime with
   `NoDefinitionFoundException` (this exact crash happened). Reified `get()`
   inside module lambdas needs `import org.koin.core.component.get`.

7. **kotlin.time vs kotlinx.datetime (0.8+)**: the live types are stdlib's
   `kotlin.time.Instant` / `kotlin.time.Clock`; `kotlinx.datetime`'s copies are
   gone (only extensions remain). Import from `kotlin.time`. In Swift, Instant
   is `KotlinInstant` and has **no** `toNSDate()` — convert via
   `epochSeconds`/`nanosecondsOfSecond` (see `EventsView` `EventRow`).

8. **Cancellation hygiene**: any `catch (e: Exception)` that guards a suspend
   call must rethrow `CancellationException` first (see
   `EventRepositoryImpl.refresh`). Otherwise cancelled loads get treated as
   offline-fallbacks and the sample data overwrites real data.

9. **JVM unit tests have no `Dispatchers.Main`.** Test files construct
   `CoroutineScope(Dispatchers.Default)`, never Main. Anything hard-wiring Main
   must be injected (the repository takes a scope).

10. **`runCatching` hides cancellation** — prefer try/catch with an explicit
    CancellationException rethrow (see #8; this repo deliberately avoids
    `runCatching` around suspend calls).
11. **Chips rows must scroll.** A fixed-width `Row` with all six category
    chips overflowed: children wrapped into thin clipped slivers and the row
    ballooned to ~850px, pushing the list down ~758px (this shipped and was
    caught in QA). Android uses `horizontalScroll(rememberScrollState())`,
    iOS uses `ScrollView(.horizontal)` with a fixed row height.
12. **Never hand-edit poster PNGs.** Regenerate them with
    `tools/generate_posters.py` (deterministic, emits Android
    `drawable-nodpi` AND the iOS asset catalog in one run, verifies itself).
    Android res names can't contain `-` (`poster_musical_show`); Swift asset
    names are PascalCase (`PosterMusicalShow`). Mapping helpers:
    `PosterMap.kt` / `PosterMap.swift`.
13. **`expectSuccess = true` is set on the shared `HttpClient`.** Ktor 3
    defaults to `false`, which feeds non-2xx response JSON into the DTO
    decoders. With it set, non-2xx → `ClientRequestException` (401 → login
    error text, 404 → detail "no longer available" state).
14. **Swift-facing state comes only through concrete `observe(onChange:)`** —
    never the type-erased `id<KotlinStateFlow>` (values arrive as `Any?`):
    that's why `SessionStore.observeCurrentUser` was added. Sealed classes
    bridge as flattened subclasses (`SharedAuthUiStateSuccess` etc.), match
    with `is`/`as?`; boxed numerics (`KotlinLong`/`KotlinDouble`) need
    `.longValue`/`.doubleValue` in Swift. UIs start observations on appear,
    stop on disappear, and screens reset their view model once on leave
    (single owner of transitions — views don't navigate themselves).
15. **HTTP timeouts are an explicit shared policy, and logging redacts.**
    `HttpTimeout` is installed on the shared client (request 15s, connect
    10s) instead of trusting engine defaults — OkHttp and NSURLSession
    disagree on defaults, so the policy must live in one place. Ktor logging
    runs at `LogLevel.INFO` only: request line/status metadata, never
    request/response bodies — demo credentials must never reach a log.
    While `BackendConfig.useFakeBackend = true` the logging plugin is
    not installed at all (the fake demo must not spam logcat).
16. **Detail pages resolve through the LIVE repository stream.** The detail
    view model resolves its list event *after* the fetch completes via
    `repository.observeEvents()` — never a one-shot snapshot — so the
    rendered event mirrors current `isSaved` flags.
17. **Posters come in a hero/thumb pair; regenerate, never hand-edit.**
    `tools/generate_posters.py` emits both platforms in one run: hero
    1200×1500 and thumb 480×600 (`poster_event_N_thumb`). Lists decode the
    thumb (~1.2MB ARGB_8888), detail screens the hero (~29MB decode) — see
    call-site comments. Lookup helpers: `PosterMap.kt` (posterResFor /
    posterThumbResFor) and `PosterMap.swift`. The script parses its palette
    and stems from `CategorySpec.kt` + the Android label map — one editable
    source of truth for category metadata.
18. **Demo passwords are hashed in fixtures.** `users.json` + its embedded
    mirror store `passwordHash` (SHA-256 hex, lowercase) instead of the
    plaintext; `FakeBackendEngine` compares `sha256Hex(posted password)`.
    The plaintext stays ONLY in the user-facing demo hint; the digest is
    provided by the expect/actual `com.evantom.data.sha.sha256Hex` (JVM
    MessageDigest / CommonCrypto `CC_SHA256`).
19. **Demo content has exactly one source: the API fixtures.**
    `data/local/` (SampleEvents) is gone — refresh failures keep previously
    loaded state and surface `Result.failure`; no bundled fallback lists
    may return.
20. **Coverage is measured, not chased.** Run
    `./gradlew :shared:koverHtmlReport` (+ `:shared:koverXmlReport` for
    machine parsing); HTML lands in `shared/build/reports/kover/html/`.
    Excluded from counting: `com.evantom.di` (Koin wiring),
    `com.evantom.data.fake` (offline demo backend — exercised end-to-end by
    walkthroughs) and `com.evantom.platform` (expect/actual plumbing).

## Conventions

- One Gradle version pin per dependency, all in `gradle/libs.versions.toml`.
- Shared code is pure Kotlin: no Android/framework classes in `commonMain` —
  platform needs go through `expect/actual` (`data/http`, `platform/`).
- Swift reaches the Koin graph only via typed accessors on `SharedKoin`
  (`SharedKoin.shared.eventsViewModel()`), never via `Koin.get(objCClass:)`.
- Kotlin objects in Swift are `Foo.shared`; top-level functions live behind
  file-named classes (`PlatformInfo.kt` → `PlatformInfoKt`) — that's why
  `object Platform { fun info() }` wraps `platformInfo()`.
- Xcode↔Gradle: `iosApp.xcodeproj`'s first Run Script phase invokes
  `./gradlew :shared:embedAndSignAppleFrameworkForXcode`; the framework is
  looked up in `shared/build/xcode-frameworks` (gitignored). Don't add
  CocoaPods; don't remove that phase.
- The app icon placeholder is a plain vector (`@drawable/ic_launcher`); no
  app-icon asset catalog yet (posters live in `Posters.xcassets`, icons don't).
- `imageRef` flows fixtures → `EventDto` → `Event` → poster resource, resolved
  per platform by the PosterMap helpers. Every shared UI string is asserted
  by the XCUITest walkthrough; changing wording means updating fixtures and
  the test in one commit.
- The repo is git (history starts at the first feature-complete commit); no
  remote is configured.

## When adding a feature, the expected diff shape

1. Model/contract in `shared/.../domain` (pure Kotlin).
2. Data plumbing in `shared/.../data` + register in `shared/.../di/AppModules.kt`
   (type the `single<>`s explicitly). If the fake backend serves new data,
   update the fixtures under `shared/src/commonMain/resources/api/` and the
   compiled-in mirror (`FakeApiPayloads`) — the parity test guards drift.
3. State + view model in `shared/.../presentation` (data-class state + a
   concrete `observe(onChange:)`; Swift needs it for every observable).
4. Render it twice: Compose in `androidApp/.../EventsScreen.kt` style,
   SwiftUI in `iosApp/iosApp/EventsView.swift` style.
5. Unit tests in `shared/src/commonTest` — the UIs are thin; logic must be
   testable in common code.
6. Extend `WalkthroughUITests.swift` when a flow changes, and regenerate
   posters via the script if artwork changes.
7. Done-gates: `./gradlew build` (all suites) AND `xcodebuild test`
   (walkthrough green). For OCR-level runtime checks without a GUI, the
   established tools: `idb ui describe-all` (accessibility frames) and
   macOS Vision OCR on `simctl` screenshots.