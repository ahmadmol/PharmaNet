# PharmaLink Phase 2 Rebuild Summary

## Implemented

- Connected Home, Resources, Warehouse Detail, Create Request, Orders, Request Details, and Order Details to a shared in-memory repository.
- Added route-driven detail flows for:
  - `warehouse/{warehouseId}`
  - `request/{requestId}`
  - `order/{orderId}`
- Replaced composable-level hardcoded business data in core flows with repository-backed view models.
- Improved request creation with:
  - warehouse selection
  - stronger validation
  - repository submission
  - success navigation into request details
- Reworked orders to map directly from domain models and navigate into order details.
- Reworked home into an operational dashboard that summarizes orders, requests, and notifications.

## Phase 3 Priorities

- Replace the in-memory repository with real local/remote data sources.
- Rebuild notifications into an actionable notification center.
- Rebuild profile/compliance/settings with view models and repository-backed state.
- Add draft persistence for request creation.
- Improve localization consistency for content strings and clean up legacy encoded text resources where needed.
- Add tests for navigation arguments, request validation, repository flows, and view model state mapping.
