# EvenTom

An Eventim-style ticketing demo built as a Kotlin Multiplatform learning
project. One shared Kotlin module holds everything that isn't UI, and each
platform renders natively: Jetpack Compose on Android, SwiftUI on iOS. The
app runs fully offline, and the data layer isn't stubbed at the UI — a Ktor
HTTP boundary serves real JSON that the DTO mappers parse like a live
backend.

```
┌────────────────────────────────────────────┐
│                 shared (KMP)               │
│  domain  : models, repository, use cases   │
│  data    : Ktor HTTP boundary + fixtures   │
│  present.: view models (StateFlow) + Koin  │
└─────────────┬──────────────────┬───────────┘
              │                  │
   ┌──────────▼──────┐  ┌───────▼─────────┐
   │  androidApp     │  │  iosApp         │
   │  Jetpack Compose│  │  SwiftUI        │
   └─────────────────┘  └─────────────────┘
```

## The offline fake backend

A Ktor `MockEngine` intercepts requests below `ContentNegotiation`, so
request serialization, response headers, and kotlinx-serialization DTO
parsing all run through the normal client pipeline:

- `GET /api/events` — the event list (Eventim-style categories, prices,
  venues, image refs)
- `GET /api/event-details/{id}` — per-event details, 404 for unknown ids
- `POST /api/login` — `demo@evantom.dev` / `demo1234` works, other
  credentials get a 401

The canonical fixtures live in `shared/src/commonMain/resources/api/`, and a
parity test fails the build if the compiled-in payloads drift from them.
Pointing the app at a real backend later means flipping one flag in
`BackendConfig` and aiming the HTTP client at the real host.

## Posters from one script

`tools/generate_posters.py` draws all twelve posters with Pillow (six event
posters plus one per category) and writes them for both platforms in a single
run: Android `drawable-nodpi` and iOS `Posters.xcassets`. Rerunning the
script reproduces byte-identical files.

## Building and running

Requirements: JDK 17+, Android SDK, Xcode (tested with Xcode 27 and an
iOS 26.5 simulator).

```bash
# everything + shared-module tests
./gradlew build

# Android on a device or emulator
./gradlew :androidApp:installDebug

# iOS
open iosApp/iosApp.xcodeproj        # ⌘R
# or CLI:
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
```

The iOS app doesn't use CocoaPods. The Xcode project has a Run Script phase
that invokes `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.

## Testing

- `shared/src/{commonTest,androidHostTest}` covers the fake-backend
  pipeline, the repository, the auth and detail view models, and the fixture
  parity. The suite runs twice: as JVM host tests (26 tests) and on the iOS
  simulator as Kotlin/Native tests (25 tests).
- `iosAppUITests/` is an XCUITest walkthrough of the whole app — six tests
  covering splash content, the login form, the full login journey (posters,
  save round trip, chip scrolling, refresh), the wrong-password error path,
  the detail journey, and the sign-out cycle:

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro'
```

## Project layout

```
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml  # every dependency version in one place
tools/generate_posters.py  # deterministic poster generator (both platforms)
shared/
  src/commonMain/          # domain, data, presentation, di, API fixtures
  src/androidMain/         # OkHttp engine, Android platformInfo
  src/iosMain/             # Darwin engine, iOS platformInfo
androidApp/                # Compose UI only
iosApp/
  iosApp/                  # SwiftUI sources + Posters.xcassets
  iosApp.xcodeproj         # Gradle framework integration + UI-test target
```

The layout rule is simple: if something isn't UI, it belongs in `shared/`.
The app modules render shared state and forward user actions back into the
shared view models.

AGENTS.md records the conventions and the platform gotchas from building
this — worth reading before changing code in the repo.