# EvenTom

[![tests](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2FLa-Volpe%2Feventom%2Fmain%2F.github%2Fbadges%2Ftests.json)](https://github.com/La-Volpe/eventom/actions/workflows/build.yml)
[![coverage](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2FLa-Volpe%2Feventom%2Fmain%2F.github%2Fbadges%2Fcoverage.json)](https://github.com/La-Volpe/eventom/actions/workflows/build.yml)

A ticketing demo in the spirit of Eventim, built as a Kotlin Multiplatform
project. One shared Kotlin module holds everything that isn't UI. The two
platform clients render fully natively: Jetpack Compose on Android, SwiftUI
on iOS.

It is a demo, and it says so up front. The "backend" ships inside the app,
the UI is bilingual (device language by default, with a live German/English
switch in settings), and almost nothing survives a relaunch. The language
choice is the only thing that persists. What is real is the architecture and
the tests around it.

What's hard here:

- **One codebase, two UIs, tested twice.** The exact same strings get
  asserted on Compose and SwiftUI, in both languages (`AGENTS.md` #24:
  [bilingual architecture](https://github.com/La-Volpe/eventom/blob/main/AGENTS.md#L259)).
- **The backend is faked below content negotiation.** A Ktor `MockEngine`
  answers inside the real client pipeline, so serialization and error paths
  run for real. Switching to a live backend is a config flag (#13, #22:
  [headers, timeouts, logging](https://github.com/La-Volpe/eventom/blob/main/AGENTS.md#L232)).
- **193 tests, three targets, five CI jobs.** Per-language fixture families
  are guarded by a parity test, and every push is gated (#19, #23:
  [per-language content + the badge mechanism](https://github.com/La-Volpe/eventom/blob/main/AGENTS.md#L241)).

| Android | iOS |
|---|---|
| ![Events on Android](.github/assets/android-events.png) | ![Events on iOS](.github/assets/ios-events.png) |
| ![Flow on Android: sign-in → list → detail](https://github.com/user-attachments/assets/2685d76e-c205-45f2-8e76-0976074098c2) | ![Flow on iOS: sign-in → list → detail](https://github.com/user-attachments/assets/e1c6e1f4-1be3-46b3-90f3-d05fb4aeea6c) |

```text
┌────────────────────────────────────────────┐
│                 shared (KMP)               │
│  domain  : models, repository, contracts   │
│  data    : Ktor HTTP boundary + fixtures   │
│  present.: view models (StateFlow) + Koin  │
└─────────────┬──────────────────┬───────────┘
              │                  │
   ┌──────────▼──────┐  ┌───────▼─────────┐
   │  androidApp     │  │  iosApp         │
   │  Jetpack Compose│  │  SwiftUI        │
   └─────────────────┘  └─────────────────┘
```

## The fake backend

There is no server. A Ktor `MockEngine` sits where a real HTTP engine would
go and answers from JSON fixtures. Request serialization, response headers,
and DTO parsing all run through the genuine client pipeline:

- `GET /api/events` returns the event list (categories, prices, venues, image refs)
- `GET /api/event-details/{id}` returns per-event details, with a 404 for unknown ids
- `POST /api/login` accepts `demo@eventom.dev` / `demo1234`; anything else gets a 401

The fixtures live in `shared/src/commonMain/resources/api/`, mirrored as
compiled-in strings and guarded by a parity test. If either copy drifts, the
build fails. Swapping in a real backend later is a config flag, not a
rewrite.

Why fake the engine boundary instead of stubbing the repository? A stub
proves nothing, because it skips exactly the layer where real apps break.
Done this way, the network itself is the only pretend part.

## Posters

`tools/generate_posters.py` draws all twelve posters with Pillow and writes
them for both platforms in a single run: Android `drawable-nodpi` and iOS
`Posters.xcassets`, each poster in two sizes (hero 1200×1500, thumbnail
480×600). Rerunning the script reproduces the files byte for byte.

## Building and running

Prerequisites: JDK 17+, the Android SDK, and Xcode (tested with Xcode 27 and
the iOS 26.5 simulator).

```bash
./gradlew build                       # everything + shared-module tests
./gradlew :androidApp:installDebug    # Android on a device/emulator

open iosApp/iosApp.xcodeproj          # iOS, ⌘R
```

There is no CocoaPods. The Xcode project's Run Script phase calls
`./gradlew :shared:embedAndSignAppleFrameworkForXcode`.

## Testing

- `shared/src/{commonTest,androidHostTest}`: 193 tests (101 on the JVM —
  8 of them Robolectric-backed against the android actuals — and 92 as
  Kotlin/Native on the iOS simulator) covering the fake-backend pipeline,
  the repository, the auth and detail view models, fixture parity per
  language, language switching, the no-fallback error paths, and the full
  bilingual string table. Line coverage is gated at 100 % (`koverVerify`).
- `androidApp/src/androidTest`: a single connected Compose walkthrough of
  the full journey — German-pinned, then through the in-app switch to
  English and back — with navigation route assertions along the way.
- `iosApp/iosAppUITests`: seven XCUITests across two suites covering splash,
  login form, the login journey (posters, save round trip, chip scrolling,
  refresh), the wrong-password path, the bilingual detail journey, sign-out,
  and the language switch itself.

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro'
```

## Layout

```
settings.gradle.kts
gradle/libs.versions.toml  # every dependency version in one place
tools/generate_posters.py  # deterministic poster generator (both platforms)
shared/src/commonMain/     # domain, data, presentation, core, session, di, fixtures
shared/src/*Main/          # platform engines via expect/actual
androidApp/                # Compose UI only
iosApp/iosApp/             # SwiftUI sources + Posters.xcassets
```

One rule shapes the layout: whatever isn't UI goes into `shared/`, and
whatever isn't logic stays out of `shared/`.

## How this was built

Built with an AI-agent orchestrator that was governed like a team member.
Specs and a plan were written first. Each phase got its own commits with its
own gates, a human led the QA round, and an independent auditor checked the
evidence before anything counted as done. The audit has teeth: it once
rejected a completion over an untranslated German "ab" price prefix, and the
fix shipped before the phase closed. Every gotcha in `AGENTS.md` started as
a lesson one of us paid for.

## Notes for agents (and the curious)

`AGENTS.md` records the conventions, the review/QA standards, and the
platform gotchas that QA surfaced, including which corners were deliberately
cut and why. `HUMANS.md` is the short version of the same story.

The best code in this repo is the code it doesn't contain: no manual JSON
parsing, no image-loader dependency, no navigation framework on iOS. The
platforms already provide all three, and the diff stays honest about what
was actually needed.