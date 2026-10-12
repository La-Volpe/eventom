# AGENT.md — EvenTom

Guide for AI agents (and humans) working in this repo: architecture, commands,
and the platform gotchas already paid for. Read before changing anything.

## What this project is

A Kotlin Multiplatform (KMP) boilerplate: **`shared/` owns everything non-UI**;
each platform renders natively — Jetpack Compose (`androidApp/`) and SwiftUI
(`iosApp/`). If you find yourself adding business logic to either app module,
stop: it belongs in `shared/`. The app modules only render shared state and
forward user actions back into shared view models.

```
shared/            domain (model/repository/usecase) · data (Ktor + sample fallback)
                   presentation (StateFlow view models) · di (Koin) · platform (expect/actual)
androidApp/        Compose UI only (plain Android app)
iosApp/            SwiftUI only + Xcode project consuming Shared.framework
```

Data flow, in one line: `SampleEvents`/`KtorEventRemoteDataSource` →
`EventRepositoryImpl` (single `MutableStateFlow`, remote-first refresh with
sample fallback) → `EventsViewModel` (`StateFlow<EventsUiState>`) →
Compose `collectAsStateWithLifecycle` / SwiftUI `EventsBridge.observe(onChange:)`.

## Commands

```bash
./gradlew build                                   # everything + all shared tests
./gradlew :shared:testAndroidHostTest             # common tests on JVM
./gradlew :shared:testIosSimulatorArm64Test       # common tests on iOS simulator  (task: :shared:iosSimulatorArm64Test)
./gradlew :androidApp:installDebug                # run Android app
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build   # build iOS app
open iosApp/iosApp.xcodeproj                      # run iOS app from Xcode (⌘R)
```

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
| Ktor | 3.6.0 (OkHttp / Darwin engines via expect/actual) |
| compile/target SDK | 37; minSdk 26; iOS min 16.0 |

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
- The app icon placeholder is a plain vector (`@drawable/ic_launcher`); there
  is no iOS Asset Catalog (App Icon can be added later).

## When adding a feature, the expected diff shape

1. Model/contract in `shared/.../domain` (pure Kotlin).
2. Data plumbing in `shared/.../data` + register in `shared/.../di/AppModules.kt`
   (type the `single<>`s explicitly).
3. State + view model in `shared/.../presentation` (data-class state + a
   concrete `observe(onChange:)` if Swift needs it).
4. Render it twice: Compose in `androidApp/.../EventsScreen.kt` style,
   SwiftUI in `iosApp/iosApp/EventsView.swift` style.
5. Unit tests in `shared/src/commonTest` — the UIs are thin; logic must be
   testable in common code. Run `./gradlew build` before declaring done,
   and for anything touching the Swift surface, also run the `xcodebuild`
   command above and a simulator launch.