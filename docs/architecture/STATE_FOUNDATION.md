# ThumbType V3.2 State Foundation

Verified checkpoint commit: `136af9c1ad6f9b2b8eed0d731dd3f3665090b019`

## What changed

- App-level mutable Compose state moved out of `ThumbTypeRoot` into `ThumbTypeViewModel`.
- `ThumbTypeUiState` is the single observable app-shell state object.
- UI state is exposed as read-only `StateFlow` and collected with lifecycle awareness.
- Profile/settings persistence, lesson launch, baseline launch, session completion, backup restore, local-data deletion, weakness training, retry and back-navigation orchestration now live in the ViewModel.
- `MainActivity` remains responsible for Android UI concerns such as activity-result launchers, screen security flags, sharing and Compose rendering.

## Deliberately deferred

This checkpoint does not claim the full architecture migration is complete. Navigation Compose, repository interfaces, domain/data boundaries, dependency injection and process-death restoration are separate verified steps to avoid a high-risk rewrite.

## Verification

GitHub Actions run `33297412075` completed successfully and built the debug APK after these changes.
