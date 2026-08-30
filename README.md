# ThumbType

ThumbType is a mobile-first Android typing trainer focused on two-thumb phone typing, adaptive reach coaching, rhythm, accuracy, weak-key analysis and structured practice.

## Current baseline

- Product line: ThumbType V3.5 Analytics
- Application ID: `com.sadam.thumbtype.mobile`
- Debug application ID: `com.sadam.thumbtype.mobile.debug`
- Android: minSdk 23, targetSdk 35, compileSdk 35
- Java/Kotlin target: JVM 17
- UI: Jetpack Compose / Material 3
- Gradle: 8.9
- Stable pre-hardening checkpoint: `ab1330ffc5438817751ccaa91d57eeab50898f00`
- Architecture checkpoint: `checkpoint/v3.4-architecture-part-c`

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

GitHub Actions now runs deterministic analytics unit tests before compiling the debug application. A build is only considered verified when both the unit-test gate and `assembleDebug` succeed, after which the APK and Android Studio project are packaged as artifacts.

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

Legacy V1-V3 key and transition statistics are migrated when read: old successful `presses`/`count` values are combined with legacy error counts to reconstruct total attempts. New backups use format version 4 while imports remain compatible with backup versions 1 through 4.

## Security baseline

The current offline build intentionally has no Internet, location, contacts, camera, microphone or broad-storage permissions. Cleartext traffic is disabled, automatic app-data backup is disabled, FileProvider is non-exported, and release builds enable R8 shrinking/obfuscation and resource shrinking.

Never commit signing keys, passwords, API secrets, service-account files, `.env` files or `local.properties`. The repository `.gitignore` blocks common credential and build-artifact paths, but developers are still responsible for reviewing changes before every commit.

## Architecture direction

The architecture foundation now includes lifecycle-aware StateFlow UI state, ViewModel-owned application actions, feature read models, a repository abstraction, an application dependency container, centralized navigation policy and saved-state restoration for navigation/selected lessons. Future production work will introduce structured Room/DataStore persistence, deeper testing, accessibility, performance benchmarks, account/backend infrastructure, offline-first sync and release hardening. Major phases should continue to be implemented and verified one at a time rather than through large unverified rewrites.
