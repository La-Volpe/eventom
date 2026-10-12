# EvenTom

A Kotlin Multiplatform (KMP) boilerplate application: **all logic lives in one
shared Kotlin module**, while each platform renders the UI natively —
**Jetpack Compose** on Android and **SwiftUI** on iOS.

```
┌────────────────────────────────────────────┐
│                 shared (KMP)               │
│  domain  : models, repository, use cases   │
│  data    : Ktor HTTP, sample fallback      │
│  present.: view models (StateFlow) + Koin  │
└─────────────┬─────────────────┬────────────┘
              │                 │
   ┌──────────▼──────┐  ┌───────▼─────────┐
   │  androidApp     │  │  iosApp         │
   │  Jetpack Compose│  │  SwiftUI        │
   └─────────────────┘  └─────────────────┘
```

## What each layer does

| Layer | Location | Role |
|---|---|---|
| Domain | `shared/domain/model,repository,usecase` | Event model, `EventRepository` contract, use cases (observe/refresh/save/filter) |
| Data | `shared/data` | Ktor HTTP client (OkHttp on Android, Darwin on iOS), remote data source, deterministic sample fallback |
| Presentation | `shared/presentation` | `BaseViewModel` + `EventsViewModel` exposing a `StateFlow<EventsUiState>`; no Android/iOS types |
| DI | `shared/di` | Koin modules + `SharedKoin.start()` entry point used by both apps |
| Android UI | `androidApp/` | Compose screens, theme, bottom bar; collects `state` with `collectAsStateWithLifecycle` |
| iOS UI | `iosApp/` | SwiftUI screens; observes the view model via `observe(onChange:)` |

Feature demo included in both apps: event list, category filter chips,
save/unsave, refresh with offline fallback message, and an About screen fed by
expect/actual `platformInfo()`.

## Requirements

- JDK 17+ (built and tested with JDK 21)
- Android SDK (AGP 9 will pick up `ANDROID_HOME`)
- Xcode 16+ for the iOS app

## Getting started

```bash
# Verify the environment (Kotlin's own doctor)
kdoctor            # optional, if installed

# Build everything + run the shared-module unit tests
./gradlew build

# Android: install & run on a device/emulator
./gradlew :androidApp:installDebug

# iOS: open in Xcode and press Run
open iosApp/iosApp.xcodeproj
```

The iOS app does **not** require CocoaPods: the Xcode project has a Run Script
build phase that invokes `./gradlew :shared:embedAndSignAppleFrameworkForXcode`,
which compiles `shared` for the active architecture, and signs/creates
`Shared.framework` in `shared/build/xcode-frameworks`. For device builds,
set your Apple Developer team in `iosApp/iosApp.xcodeproj` (Signing & Capabilities).

## How the pieces fit

- **Single source of truth**: `EventRepositoryImpl` keeps events in a
  `MutableStateFlow`; both platform UIs simply observe it. `refresh()` is
  remote-first and falls back to `SampleEvents` when offline.
- **View models are plain Kotlin** (no `androidx.lifecycle` dependency):
  Android collects `viewModel.state` directly; SwiftUI uses a small
  `EventsBridge` `ObservableObject` that forwards `observe(onChange:)` into
  `@Published` state.
- **DI is Koin in common code**: `SharedKoin.start()` is called once at app
  start (in `EvenTomApplication` and in the SwiftUI `App.init`).
  Typed accessors (`SharedKoin.eventsViewModel()`) keep Swift free of
  `KClass` ceremony.
- **expect/actual**: `httpClientEngine()` (OkHttp/Darwin) and
  `platformInfo()` (About screen) demonstrate the pattern.

## Where to go next

- **Persistence**: add SQLDelight for an offline database and keep the
  repository's observable-state shape.
- **Networking**: point `BASE_URL` in `EventRemoteDataSource.kt` at the real
  EvenTom backend; the DTO→domain mapper is already in place.
- **Swift interop**: install [SKIE](https://skie.touchlab.co) for idiomatic
  Swift flows (`for await` on `StateFlow`, sealed classes as enums).
- **Tests**: `shared/src/commonTest` covers the repository and use cases;
  UIs stay thin, so most regression risk is testable in common code.
- **CI**: `./gradlew build` plus `xcodebuild -project iosApp/iosApp.xcodeproj
  -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build`.

## Project layout

```
settings.gradle.kts
build.gradle.kts
gradle.properties          # config cache, parallel builds
gradle/libs.versions.toml  # all dependency versions in one place
shared/
  build.gradle.kts
  src/commonMain/          # domain, data, presentation, di, platform contracts
  src/androidMain/         # OkHttp engine, Android platformInfo
  src/iosMain/             # Darwin engine, iOS platformInfo
  src/commonTest/          # unit tests for the shared logic
androidApp/                # Compose UI only
iosApp/
  iosApp/                  # SwiftUI sources
  iosApp.xcodeproj         # with the Gradle framework integration
```