# ThumbType V4.4 Performance Engineering Foundation

This phase improves measurable hot paths and adds performance gates without pretending GitHub CI can replace real Android device benchmarking.

## Implemented optimizations

1. **DataStore snapshot cache**
   - The local preferences store keeps the latest decoded immutable preference snapshot in memory after the first load.
   - Every successful DataStore write refreshes that cache from the committed Preferences result.
   - Repeated reads for profile, XP, streak, targets and settings no longer need to repeatedly collect and decode the DataStore flow during the same app process.

2. **One-pass UI read-model snapshot**
   - Home/Learn/Practice/Progress/Profile data is built from one read of each required backing dataset.
   - Independent repository reads are started together on the caller's background context.
   - Weak-transition rankings and achievements are derived from already-loaded history/transition/counter data instead of issuing duplicate repository reads.

3. **Compose stability contract**
   - Read-model and root UI-state objects are explicitly immutable at the Compose boundary.
   - Their collections are treated as immutable snapshots and are replaced rather than mutated in place.

4. **Deterministic no-redundant-I/O test**
   - A counting fake repository verifies that read-model generation reads each backing dataset once.
   - The test fails if the snapshot builder regresses to calling aggregate helpers that re-read history, transition stats or counters.

## CI performance gates

The main workflow now runs:

1. deterministic unit tests;
2. debug APK build;
3. minified/shrunk release APK build;
4. APK-size budget check;
5. project and artifact packaging.

Current safety budgets are intentionally generous regression ceilings, not product targets:

- Debug APK: **30 MiB maximum**
- Unsigned minified release APK: **25 MiB maximum**

The workflow writes exact APK byte counts to `performance-ci.txt` and packages that report with the verified Android Studio project.

## What CI does NOT prove

A Linux GitHub Actions build cannot prove real phone startup time, rendering smoothness, touch latency, battery use, thermal behavior or memory pressure. Those metrics must be measured on Android runtime hardware/emulators.

Before production, run dedicated device measurements for:

- cold/warm startup;
- frame timing and jank;
- trainer input-to-visual-response latency;
- memory before/during/after long sessions;
- Room query/transaction latency with large synthetic history;
- battery/CPU behavior during 5–15 minute training sessions;
- compact, mid-range and lower-end Android devices;
- release build rather than debug build.

## Recommended next measurement harness

A later validation step should add/run Android Macrobenchmark + Baseline Profile generation on an API/device configuration that supports instrumentation. Target scenarios should include cold startup, Home scrolling, Progress scrolling, lesson start, sustained trainer interaction and Results transition.

Baseline Profile generation should only be shipped after it is generated and validated from the actual release navigation/training paths.

## Performance rule

ThumbType should not claim a specific startup millisecond, jank percentage, memory ceiling or typing latency until that number has been produced by a reproducible Android benchmark on a named device/build configuration.
