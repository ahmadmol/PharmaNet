# PharmaLink Phase 1 Rebuild Summary

## What changed

- Centralized app navigation into an argument-ready destination model under `app/.../core/navigation`.
- Reorganized app-owned screens into feature-first packages.
- Added shared domain models for `Order`, `Warehouse`, `Request`, `AppNotification`, and `PharmacyProfile`.
- Added reusable UI contracts for `UiAction`, `UiEvent`, `ScreenState`, and `ScreenContract`.
- Added a first repository contract in `data/repository/PharmaRepository.kt`.

## Notes for Phase 2

- Move static sample data out of screens and view models into repository implementations.
- Split `PharmaRepository` into feature-specific repositories and add data sources.
- Add dedicated view models for resources, warehouse details, profile, help, messages, and other remaining app-local screens.
- Complete route adoption for `order/{orderId}` and `request/{requestId}` with real detail screens.
- Decide whether remaining app-local screens such as messages and rate should become dedicated feature modules.
- Add tests for navigation contracts, UI state reducers, and mappings.
