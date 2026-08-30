# ThumbType

ThumbType is a mobile-first Android typing trainer focused on two-thumb phone typing, adaptive reach coaching, rhythm, accuracy, weak-key analysis and structured practice.

## Current baseline

- Product line: **ThumbType V4.2 Premium Experience**
- Application ID: `com.sadam.thumbtype.mobile`
- Debug application ID: `com.sadam.thumbtype.mobile.debug`
- Android: minSdk 23, targetSdk 35, compileSdk 35
- Java/Kotlin target: JVM 17
- UI: Jetpack Compose / Material 3
- Persistence: Room + Preferences DataStore
- Training intelligence: deterministic mastery, trend, plateau and adaptive-workout engine
- UI system: centralized spacing/motion/sizing tokens, premium Material 3 theme and full premium screen routing
- Gradle: 8.9

### Stable checkpoints

- Pre-hardening baseline: `ab1330ffc5438817751ccaa91d57eeab50898f00`
- Architecture: `checkpoint/v3.4-architecture-part-c`
- Analytics: `checkpoint/v3.5-analytics-correctness`
- Persistence: `checkpoint/v3.6-persistence`
- Training intelligence: `checkpoint/v4.0-training-intelligence`
- UI foundation: `checkpoint/v4.1-ui-foundation`

## Build locally

Use JDK 17, then from the repository root:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
```

The debug APK is generated at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## CI verification

GitHub Actions runs the deterministic unit-test suite before compiling the debug application. The test gate includes training/analytics tests, adaptive-training intelligence tests and a Robolectric persistence migration/backup round-trip test. A build is only considered verified when the test gate and `assembleDebug` both succeed, after which the APK and Android Studio project are packaged as artifacts.

## Analytics definitions

- **Attempt:** every physical character-key press made while a target character is expected.
- **Correct:** an attempt that enters the expected target character.
- **Mistake/error:** an attempt that enters a different character.
- **Accuracy:** correct attempts divided by all attempts.
- **Raw WPM:** all character attempts divided by five and by elapsed minutes.
- **Net WPM:** raw WPM minus mistakes per elapsed minute, never below zero.
- **Recovered mistake:** a wrong attempt at a target position that is later completed correctly before the session ends.
- **Unresolved mistake:** a wrong attempt whose target position has not been completed correctly when the session ends.
- **Key accuracy:** correct attempts for a key divided by all attempts made while that key was expected.
- **Transition accuracy:** successful attempts at the second key of an intended adjacent pair divided by all attempts at that transition.
- **Transition latency:** time from the preceding correctly completed key through successful completion of the next key, including time spent recovering from mistakes.

## Persistence architecture

ThumbType uses **Room** for growing structured training data and **Preferences DataStore** for compact user/settings state.

Room stores session history, detailed result metrics, per-key analytics, transition analytics, completed lessons and daily practice totals. DataStore stores onboarding state, theme, haptics, sound, accessibility preferences, coaching level, training goals, XP, lifetime counters, best WPM, streak and the last-practice date.

The repository migrates legacy `thumbtype_elite_v3` SharedPreferences data once and leaves the old store untouched as a rollback path until the user intentionally deletes all local data. New backups use ThumbType backup version 5 and remain compatible with versions 1 through 5.

## V4.0 adaptive training intelligence

The deterministic intelligence layer calculates recent WPM, accuracy and ThumbScore windows; trend deltas; plateau detection; confidence-aware per-key and transition mastery; left/right screen-zone mastery; adaptive center-key mastery; training priority; difficulty from 1–10; targeted drill material; and daily workout allocation based on the user's selected training time.

The engine intentionally talks about **left/right screen zones and recommended reach**, not biological-thumb detection. Android touch events do not reliably identify which physical thumb produced a tap.

## V4.1 UI foundation

V4.1 introduced centralized design tokens, a refined light/dark Material 3 theme, a consistent shape hierarchy, larger-text-aware typography, reduced-motion tokens, upgraded reusable cards/metrics/pills/charts/heatmaps and the first redesigned adaptive Home dashboard.

## V4.2 premium experience

V4.2 carries the premium design system through the active application experience rather than limiting it to Home.

The routed application now uses premium versions of:

- Onboarding and baseline-plan setup
- Learning-path presentation
- Practice lab and adaptive coach summary
- Progress dashboard, trend controls, heatmap and weak-transition presentation
- Profile/settings and privacy/data controls
- Session Results presentation
- Trainer chrome, live metrics and reach coach
- Custom training keyboard with 50dp interaction height, stronger visual hierarchy and screen-reader semantics on training keys

The new trainer preserves the established analytics model and still records the side of the **screen** tapped. It does not claim biological-thumb detection. Wrong attempts still do not advance the target position; the displayed backspace currently clears the immediate error state rather than acting as a full editable-text history. Full active-session persistence and a richer correction model remain separate future engineering work.

The older screen implementations remain in the source tree temporarily as a low-risk fallback while V4.2 is validated. `MainActivity` routes real users through the premium V4.2 screens.

## Security baseline

The current offline build intentionally has no Internet, location, contacts, camera, microphone or broad-storage permissions. Cleartext traffic is disabled, automatic app-data backup is disabled, FileProvider is non-exported, and release builds enable R8 shrinking/obfuscation and resource shrinking.

Never commit signing keys, passwords, API secrets, service-account files, `.env` files or `local.properties`. The repository `.gitignore` blocks common credential and build-artifact paths, but developers are still responsible for reviewing changes before every commit.

## Architecture direction

The architecture foundation includes lifecycle-aware StateFlow UI state, ViewModel-owned actions, feature read models, repository abstraction, application dependency container, centralized navigation policy, saved-state restoration, asynchronous persistence, Room, DataStore, deterministic adaptive-training intelligence and a centralized UI design system.

Major phases continue to be implemented and verified one at a time. Upcoming production work includes accessibility/device adaptation, performance benchmarking, broader automated UI/integration testing, CI/CD hardening, account/backend architecture, offline-first cloud sync, security hardening, production signing and Google Play release validation.
