# ThumbType

ThumbType is a mobile-first Android typing trainer focused on two-thumb phone typing, adaptive reach coaching, rhythm, accuracy, weak-key analysis and structured practice.

## Current baseline

- Product line: ThumbType V3 Elite
- Application ID: `com.sadam.thumbtype.mobile`
- Debug application ID: `com.sadam.thumbtype.mobile.debug`
- Android: minSdk 23, targetSdk 35, compileSdk 35
- Java/Kotlin target: JVM 17
- UI: Jetpack Compose / Material 3
- Gradle: 8.9
- Stable pre-hardening checkpoint: `ab1330ffc5438817751ccaa91d57eeab50898f00`
- Recovery branch: `checkpoint/v3-baseline`

## Build locally

Use JDK 17, then from the repository root:

```powershell
.\gradlew clean
.\gradlew assembleDebug
```

The debug APK is generated at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## CI verification

GitHub Actions compiles the debug application and packages the verified Android Studio project. A build is only considered build-verified when the workflow succeeds.

## Security baseline

The current offline build intentionally has no Internet, location, contacts, camera, microphone or broad-storage permissions. Cleartext traffic is disabled, automatic app-data backup is disabled, FileProvider is non-exported, and release builds enable R8 shrinking/obfuscation and resource shrinking.

Never commit signing keys, passwords, API secrets, service-account files, `.env` files or `local.properties`. The repository `.gitignore` blocks common credential and build-artifact paths, but developers are still responsible for reviewing changes before every commit.

## Architecture direction

V3 is the stable working baseline. Future production work will progressively introduce stronger architecture, corrected analytics models, structured persistence, testing, accessibility, performance benchmarks, account/backend infrastructure, offline-first sync and release hardening. Major phases should be implemented and verified one at a time rather than through large unverified rewrites.
