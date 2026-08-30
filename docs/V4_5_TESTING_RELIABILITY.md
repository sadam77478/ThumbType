# ThumbType V4.5 — Professional Testing & Reliability

V4.5 turns reliability checks into build gates rather than relying only on manual inspection.

## Automated host tests

The deterministic unit/Robolectric suite now covers analytics/training correctness, adaptive intelligence, responsive width classes, persistence migration/backup round-trip, read-model I/O regression protection, navigation invariants, SavedState round-trips, repeated session persistence, malformed backup rejection without data loss, and delete-all reset behavior.

## Android UI journey

An Android instrumentation journey exercises the real Compose activity through onboarding, completes onboarding by skipping the baseline and verifies the Home dashboard is reached.

## CI gates

CI must pass deterministic unit/Robolectric tests, Android lint, debug compilation, minified release compilation, instrumentation-test APK compilation, an Android emulator onboarding journey, APK size budgets, and artifact packaging. Test/lint reports are uploaded for inspection.

## Scope boundary

This phase adds a real emulator journey but does not claim exhaustive device coverage. Foldables, OEM-specific behavior, physical-touch latency, TalkBack/Switch Access, mid-step onboarding recreation, low-memory kills, battery/thermal behavior and broad device-farm matrices still require dedicated device testing.
