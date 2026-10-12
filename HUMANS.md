# HUMANS.md

The short version of this project, for a human on a coffee break.

## What it is

An Eventim-style ticketing demo: splash → login → events list → event detail.
No real backend, no store release. One Kotlin codebase drives a native Android
app (Compose) and a native iOS app (SwiftUI).

## The shape

```
┌─ androidApp/ ──────────────┐        ┌─ iosApp/ ─────────────────┐
│ Compose screens            │        │ SwiftUI views             │
│ render state, forward      │        │ render state, forward     │
│ actions into shared VMs    │        │ actions into shared VMs   │
└────────────┬───────────────┘        └────────────┬──────────────┘
             │ collectAsStateWithLifecycle          │ observe(onChange:)
┌────────────┴──────────────────────────────────────┴───────────┐
│ shared/ (pure Kotlin)                                         │
│   presentation — StateFlow view models + UI states            │
│   core · session                                              │
│   domain — models, use cases, ALL boundary interfaces         │
│   data — Ktor impls, fake backend, fixtures, digest           │
└───────────────────────────────────────────────────────────────┘
```

Flow: fixtures → Ktor client → DTOs → domain → view models → both apps'
screens. The apps contain only UI.

## How it's built

**1. Business logic lives in `shared`.**
Shared view models expose subscriptions one way, `BaseViewModel.kt:45`
`observeState(onChange:)`. Android collects the StateFlow; iOS wraps the same
call in a small ObservableObject bridge (`EventsView.swift:25`,
`viewModel.observe(onChange:)`). Compose and SwiftUI can't be shared. Everything
under them can.

**2. The backend is a fake, and mostly it behaves like a real one.**
No server; JSON fixtures are served by a Ktor `MockEngine`, swapped at the
engine boundary (`AppModules.kt:53`):

```kotlin
if (BackendConfig.useFakeBackend) fakeBackendEngine() else httpClientEngine()
```

The fake serves compiled-in copies of the fixtures (`FakeBackendEngine.kt:41`
serves `FakeApiPayloads.EVENTS_JSON`), and a parity test fails the build if
either copy drifts. Content negotiation, error paths, and login all actually
run. A real backend later means changing a config flag.

**3. The domain owns every interface.**
`domain/repository/EventRepository.kt:13` declares `interface EventRepository`;
data implements it (`data/remote/EventDetailRemoteDataSource.kt:39`,
`KtorEventDetailRemoteDataSource : EventDetailRemoteDataSource`). Domain never
imports the data layer, so the whole data side can be replaced without touching
anything above it.

**4. Two poster sizes, on purpose.**
Same artwork in two files: hero 1200×1500 for the detail screen, thumb 480×600
for list cards (`THUMB_SIZE`, `generate_posters.py:35`). Lists render the thumb
(`EventsScreen.kt:161`, `posterThumbResFor(...)`). A full-res decode costs about
29 MB per screen position; the thumb costs about 1.2 MB. The generator emits
both and verifies each.

**5. Navigation is the platforms' job.**
Android uses a Compose `NavHost` with typed routes (`MainActivity.kt:70`, such
as `detail/{eventId}`); iOS uses `NavigationStack(path: $detailPath)`
(`RootView.swift:36`). Views don't navigate themselves. The framework restores
routes across process death, but not fetched content — a detail screen that
wakes up empty re-requests it. We proved that by killing the app mid-detail and
relaunching.

## Run it

- Android: `./gradlew :androidApp:installDebug`
- iOS: `open iosApp/iosApp.xcodeproj` (⌘R), or run the 6-test XCUITest
  walkthrough with `xcodebuild test`.
- Sign in: `demo@evantom.dev` / `demo1234`
- The fixtures store the SHA-256 hash of that password, not the password.

## What this is not

- No persistence: the session and saved events live in memory and die with the
  process.
- German only, and the content is offline only (no remote images).
- No CI: the quality gates are local — 67 shared tests plus two walkthrough
  suites.

## More

`AGENTS.md` holds the working conventions, the paid-for gotchas, and our review/QA
standards. Review, plan, and QA reports live outside the repo, in the orchestrator
workspace.
