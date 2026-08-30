# ThumbType V3.4 Architecture Checkpoint

This checkpoint establishes the final foundation layer before analytics-correctness work.

## Implemented

- Application-level dependency container (`ThumbTypeApplication` / `ThumbTypeAppContainer`).
- `ThumbTypeRepository` remains the application data contract; screens do not depend on SharedPreferences directly.
- Saved-state-aware `ThumbTypeViewModelFactory`.
- `SavedStateHandle` persistence for navigation destination and the selected lesson definition.
- Safe recreation policy: a stale Results destination returns to Home because the completed session is already persisted and the full `SessionResult` is intentionally not placed in Android saved state.
- Trainer destination can restore the selected lesson after Activity/process recreation. In-progress keystroke/event-buffer persistence is intentionally deferred to the later structured session-persistence work rather than storing an unbounded event list in the Android Bundle.

## Dependency flow

```text
Compose screens
  -> ThumbTypeViewModel / immutable UI models
  -> ThumbTypeRepository
  -> current local AppRepository

ThumbTypeApplication
  -> ThumbTypeAppContainer
  -> repository implementation
```

## Verification

The checkpoint is valid only when the standard GitHub Actions `assembleDebug` workflow succeeds. Do not treat intermediate commits as verified if their build failed.
