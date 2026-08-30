# ThumbType

ThumbType is a mobile-first Android typing trainer focused on two-thumb phone typing, adaptive reach coaching, rhythm, accuracy, weak-key analysis and structured practice.

## Current baseline

- Product line: ThumbType V4.0 Training Intelligence
- Application ID: `com.sadam.thumbtype.mobile`
- Debug application ID: `com.sadam.thumbtype.mobile.debug`
- Android: minSdk 23, targetSdk 35, compileSdk 35
- Java/Kotlin target: JVM 17
- UI: Jetpack Compose / Material 3
- Persistence: Room + Preferences DataStore
- Training intelligence: deterministic mastery, trend, plateau and adaptive-workout engine
- Gradle: 8.9
- Stable pre-hardening checkpoint: `ab1330ffc5438817751ccaa91d57eeab50898f00`
- Architecture checkpoint: `checkpoint/v3.4-architecture-part-c`
- Analytics checkpoint: `checkpoint/v3.5-analytics-correctness`
- Persistence checkpoint: `checkpoint/v3.6-persistence`

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

## V3.5 analytics definitions

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

## V3.6 persistence architecture

ThumbType no longer uses SharedPreferences/JSON as the primary long-term store for growing training data.

**Room database (`thumbtype.db`) stores:**

- Session history with detailed result metrics
- Per-key analytics
- Per-transition analytics
- Completed lesson progress
- Daily practice totals

**Preferences DataStore stores:**

- Onboarding state
- Theme, haptics, sound and accessibility preferences
- Coaching level
- Training goals and baseline
- XP and lifetime counters
- Best WPM
- Streak and last-practice date

Repository operations are asynchronous `suspend` functions. The ViewModel performs persistence work on an IO dispatcher so database/file work is no longer intentionally performed on the Compose UI path.

### Existing-user migration

On first V3.6 launch, the structured repository checks for the legacy `thumbtype_elite_v3` SharedPreferences store. Existing profile, settings, XP, totals, streak, lesson completion, history, key statistics, transition statistics and current-day practice are copied into Room/DataStore once. The legacy store is left untouched after migration to preserve a rollback path. Intentionally deleting all local data clears both the structured data and the legacy rollback store so deleted data cannot reappear.

### Backup format

New backups use **ThumbType backup version 5** and include structured sessions plus key/transition analytics and daily-practice records. Import remains compatible with backup versions 1 through 5. The automated persistence test verifies legacy migration, export, deletion and V5 restore as one round trip.

## V4.0 adaptive training intelligence

ThumbType now derives a deterministic training profile from actual local performance data instead of serving only fixed daily exercises.

The intelligence layer calculates:

- Recent WPM, accuracy and ThumbScore windows
- Speed, accuracy and score trend deltas
- Plateau detection across recent and previous session windows
- Confidence-aware per-key mastery
- Confidence-aware transition/digraph mastery
- Left-zone, right-zone and adaptive center-key mastery
- A current training priority such as accuracy, speed, transitions, center reach, left/right zone, rhythm or endurance
- A 1-10 adaptive difficulty level
- Targeted drill text generated from weak keys and weak transitions
- A daily workout whose four blocks are automatically allocated to the user's selected daily time goal

Mastery scores combine accuracy, latency and sample confidence so a tiny sample cannot be mistaken for proven mastery. The engine intentionally describes left/right **screen zones and recommended reach**, not biological-thumb detection; Android touch events do not reliably identify which physical thumb produced a tap.

The current V4 intelligence is deterministic by design. It does not require generative AI, cloud transmission or private typed-text uploads, which keeps recommendations measurable, testable and offline-first.

## Security baseline

The current offline build intentionally has no Internet, location, contacts, camera, microphone or broad-storage permissions. Cleartext traffic is disabled, automatic app-data backup is disabled, FileProvider is non-exported, and release builds enable R8 shrinking/obfuscation and resource shrinking.

Never commit signing keys, passwords, API secrets, service-account files, `.env` files or `local.properties`. The repository `.gitignore` blocks common credential and build-artifact paths, but developers are still responsible for reviewing changes before every commit.

## Architecture direction

The architecture foundation includes lifecycle-aware StateFlow UI state, ViewModel-owned application actions, feature read models, a repository abstraction, an application dependency container, centralized navigation policy, saved-state restoration, asynchronous persistence, Room, DataStore and a deterministic adaptive-training intelligence layer. Future production work will continue with premium UI/UX, accessibility, performance benchmarks, broader testing, account/backend infrastructure, offline-first cloud sync and release hardening. Major phases should continue to be implemented and verified one at a time rather than through large unverified rewrites.
