# Scripture

A scripture memorization app — and a sandbox for discovering a personal
UI/design style. Built with [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/):
one codebase, two targets — desktop (Windows) for the fast daily loop, Android
for your pocket.

## Run it

```bash
./gradlew :desktopApp:run        # desktop window (primary dev loop)
./gradlew :androidApp:installDebug # Android device connected via USB
./gradlew :shared:jvmTest        # unit tests for the learning logic
```

First run: open the project in IntelliJ IDEA (File → Open → this folder) and
let Gradle sync. Run configs appear after sync.

## Architecture

```
shared/           ← everything: domain, data, UI (commonMain = both platforms)
  domain/         ← pure Kotlin, no UI, no I/O — the testable core
    Verse, ReviewProgress, ReviewRecord     — the data model
    Scheduler.kt  — simplified SM-2 spaced repetition (pure functions)
    Blanking.kt  — fill-in-the-blank word selection (pure, seeded)
  data/          ← persistence
    DataStorage.kt    — "read/write named text blobs" interface
    VerseRepository.kt — verses + history ⇄ JSON (kotlinx.serialization)
  ScriptureStore.kt ← observable app state (StateFlow); UI calls intents
  ui/designsystem/ ← Theme.kt, Tokens.kt, Components.kt — THE look, one place
  ui/screens/     ← Library, AddVerse, Review
androidApp/      ← ~10 lines: MainActivity + context registration
desktopApp/      ← ~10 lines: a Window
```

**The design system is the point.** Every color, font, spacing value, and
reusable component lives in `ui/designsystem/`. Screens never hardcode a look
— they read `MaterialTheme.scriptureBody`, `Spacing.lg`, `VerseCard`, etc.
To restyle the whole app: edit `Theme.kt` / `Tokens.kt` and nothing else.

Fonts: [Lora](https://fonts.google.com/specimen/Lora) (scripture, serif) and
[Inter](https://fonts.google.com/specimen/Inter) (UI, sans), both SIL OFL —
licenses in [`licenses/`](licenses/)

## Spaced repetition semantics

Simplified SM-2. Grade with Again / Hard / Good / Easy after revealing:

| Grade | Effect |
|-------|--------|
| Again | resets; due again this session |
| Hard  | ×1.2 growth; easiness −0.15 (floor 1.3) |
| Good  | ladder 1d → 6d → interval × easiness |
| Easy  | ladder 3d → 10d → interval × easiness × 1.3 (easiness cap 3.0) |

## Conventions

- Any new field on a `@Serializable` model **must** have a default — old
  saved files must keep loading (see `RepositoryTest`).
- No network, no bundled verse text (NWT is copyrighted). You add verses yourself.
- Gradle runs on JDK 21 (`gradle/gradle-daemon-jvm.properties`); AGP's tooling
  doesn't yet support newer JDKs.