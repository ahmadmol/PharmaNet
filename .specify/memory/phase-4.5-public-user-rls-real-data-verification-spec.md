# Feature Specification: Phase 4.5 - Supabase RLS + Real Data Security Verification

**Feature Branch**: `phase-4.5-public-user-rls-real-data-verification`  
**Created**: 2026-04-29  
**Status**: Draft  
**Input**: User description: "Phase 4.5: Supabase RLS + Real Data Security Verification"

## Context

The `PUBLIC_USER` MVP is closed and compile-verified:

- Medicine search to pharmacy selection to customer order creation to success
- My Orders to customer order detail to pending-order cancellation
- Dedicated `PUBLIC_USER` customer order screens and ViewModels
- `CUSTOMER_PHARMACY` use case and repository foundation
- `CreateCustomerOrderUseCase`
- `CancelCustomerOrderUseCase`
- `GetMyOrdersUseCase`
- `SupabasePharmaRepository` with B2C validation from prior backend phases

The remaining risk is not Compose or use case compilation. The remaining risk is real Supabase security:

- Whether Row Level Security policies actually prevent cross-user and cross-role data access
- Whether repository/use case checks align with RLS behavior
- Whether real multi-user test data behaves safely
- Whether the app reports permission-denied states honestly, with no fake success

This phase is a security verification phase only. It must not add new product behavior.

## Scope

This phase verifies real-data security for the `CUSTOMER_PHARMACY` order flow before any `PHARMACY`, `WAREHOUSE`, or `ADMIN` B2C management UI is built.

In scope:

- Audit RLS policies for `orders`
- Audit RLS policies for `requests`
- Audit RLS policies for `profiles`
- Verify role isolation for `PUBLIC_USER`, `PHARMACY`, and `WAREHOUSE`
- Document current `ADMIN` behavior without expanding admin scope
- Verify repository and use case behavior against RLS expectations
- Verify role validation
- Verify ownership validation
- Verify state transition validation
- Verify no fake success paths
- Verify real Supabase app smoke flow for `PUBLIC_USER`
- Produce verification documentation with pass/fail criteria
- Run compile after any approved verification-only checks that touch the workspace

Out of scope:

- New UI
- `PHARMACY` UI
- `WAREHOUSE` UI
- `ADMIN` UI
- Payment
- Tracking
- Notifications
- Real pharmacy discovery implementation
- Real medicine catalog implementation
- `rejectionReason` or `statusReason`
- Schema changes unless a critical security blocker is documented and explicitly approved first
- Repository, domain, or model changes unless required by an approved security fix
- Fixing RLS gaps during this specification phase

## User Scenarios & Testing

### User Story 1 - Public User Can Only Access Own B2C Orders (Priority: P0)

A `PUBLIC_USER` can create, view, open, and cancel only their own `CUSTOMER_PHARMACY` orders.

**Why this priority**: Any cross-customer leak is a security failure and blocks the next phase.

**Independent Test**: Use two real `PUBLIC_USER` accounts with separate `CUSTOMER_PHARMACY` orders. Each account must only see and mutate its own eligible rows.

**Acceptance Scenarios**:

1. **Given** public user A has a B2C order, **When** public user A loads My Orders, **Then** the order is visible.
2. **Given** public user A has a B2C order, **When** public user B loads My Orders, **Then** user A's order is not visible.
3. **Given** public user A has a pending B2C order, **When** user A cancels it, **Then** cancellation succeeds and the app does not fake success.
4. **Given** public user A has a pending B2C order, **When** user B attempts to cancel it directly or indirectly, **Then** the write is blocked.
5. **Given** a `PHARMACY_WAREHOUSE` order exists, **When** any public user loads My Orders, **Then** the B2B order is not visible.

---

### User Story 2 - Pharmacy Can Only Access Its Own B2C Orders (Priority: P0)

A `PHARMACY` can view and manage `CUSTOMER_PHARMACY` orders only when `orders.pharmacy_id` belongs to that pharmacy.

**Why this priority**: Pharmacy-side lifecycle management is the next likely product surface and must be proven before UI work.

**Independent Test**: Use two real pharmacy accounts and B2C orders assigned to each pharmacy. Each pharmacy must be unable to read or update the other's B2C rows.

**Acceptance Scenarios**:

1. **Given** pharmacy A owns a B2C order, **When** pharmacy A reads B2C orders, **Then** its own order is visible.
2. **Given** pharmacy A owns a B2C order, **When** pharmacy B reads B2C orders, **Then** pharmacy A's order is not visible.
3. **Given** pharmacy A owns a pending B2C order, **When** pharmacy A confirms or rejects it through an approved path, **Then** the operation succeeds only if status transition rules allow it.
4. **Given** pharmacy A owns a B2C order, **When** pharmacy B attempts to confirm, reject, mark ready, mark out for delivery, or deliver it, **Then** the operation is blocked.
5. **Given** any pharmacy account, **When** it attempts to create a customer order on behalf of a public user, **Then** the operation is blocked.
6. **Given** any pharmacy account, **When** it attempts to cancel as a customer, **Then** the operation is blocked.

---

### User Story 3 - Warehouse Is Isolated From B2C Orders (Priority: P0)

A `WAREHOUSE` can view and manage only its own `PHARMACY_WAREHOUSE` orders and cannot access `CUSTOMER_PHARMACY` rows.

**Why this priority**: Warehouses must not see customer-to-pharmacy order data.

**Independent Test**: Use a real warehouse account and real B2C and B2B rows. The warehouse account must not read or update any B2C row.

**Acceptance Scenarios**:

1. **Given** a warehouse-owned B2B order exists, **When** the warehouse loads allowed B2B order data, **Then** the order is visible according to existing B2B rules.
2. **Given** a B2C order exists, **When** a warehouse account reads orders, **Then** the B2C order is not visible.
3. **Given** a B2C order exists, **When** a warehouse account attempts to update it, **Then** the write is blocked.
4. **Given** a warehouse account, **When** it attempts to access the B2C flow, **Then** repository/use case/RLS rules prevent access.

---

### User Story 4 - App Smoke Flow Works Against Real Supabase (Priority: P1)

The closed `PUBLIC_USER` MVP flow works with real Supabase data and fails safely on permission denied.

**Why this priority**: RLS verification must include app behavior, not only SQL-level checks.

**Independent Test**: Use the Android app with a real `PUBLIC_USER` account and a controlled test pharmacy/order fixture.

**Acceptance Scenarios**:

1. **Given** a real public user and a valid B2C test setup, **When** the user creates an order, **Then** Supabase stores a `CUSTOMER_PHARMACY` `PENDING` row owned by the user.
2. **Given** the created order, **When** the user opens My Orders, **Then** the order appears.
3. **Given** the order appears, **When** the user opens detail, **Then** only safe B2C details are shown.
4. **Given** the order is still `PENDING`, **When** the user cancels and confirms the dialog, **Then** Supabase updates the order to `CANCELLED` or the app reports a real failure.
5. **Given** RLS blocks an operation, **When** the app receives the error, **Then** the app must not show success.

## Functional Requirements

- **FR-001**: The verification MUST audit existing RLS policies for `orders`, `requests`, and `profiles`.
- **FR-002**: The verification MUST identify missing, broad, risky, or ambiguous RLS policies.
- **FR-003**: The verification MUST prove that `PUBLIC_USER` can create only their own `CUSTOMER_PHARMACY` orders.
- **FR-004**: The verification MUST prove that `PUBLIC_USER` can view only their own `CUSTOMER_PHARMACY` orders.
- **FR-005**: The verification MUST prove that `PUBLIC_USER` cannot view other customers' orders.
- **FR-006**: The verification MUST prove that `PUBLIC_USER` cannot view `PHARMACY_WAREHOUSE` orders.
- **FR-007**: The verification MUST prove that `PUBLIC_USER` can cancel only their own `PENDING` `CUSTOMER_PHARMACY` orders.
- **FR-008**: The verification MUST prove that `PUBLIC_USER` cannot cancel other customers' orders.
- **FR-009**: The verification MUST prove that `PUBLIC_USER` cannot cancel non-`PENDING` orders.
- **FR-010**: The verification MUST prove that `PUBLIC_USER` cannot confirm, reject, mark ready, mark out for delivery, or deliver orders.
- **FR-011**: The verification MUST prove that `PHARMACY` can view `CUSTOMER_PHARMACY` orders only for its own `pharmacy_id`.
- **FR-012**: The verification MUST prove that `PHARMACY` cannot view or manage another pharmacy's `CUSTOMER_PHARMACY` orders.
- **FR-013**: The verification MUST prove that `PHARMACY` cannot create orders on behalf of customers.
- **FR-014**: The verification MUST prove that `PHARMACY` cannot cancel customer orders through the customer cancel path.
- **FR-015**: The verification MUST prove that existing `PHARMACY_WAREHOUSE` behavior remains available for `PHARMACY` according to existing B2B rules.
- **FR-016**: The verification MUST prove that `WAREHOUSE` can view and manage only `PHARMACY_WAREHOUSE` orders for its own `warehouse_id`.
- **FR-017**: The verification MUST prove that `WAREHOUSE` cannot view `CUSTOMER_PHARMACY` orders.
- **FR-018**: The verification MUST prove that `WAREHOUSE` cannot update `CUSTOMER_PHARMACY` orders.
- **FR-019**: The verification MUST document current `ADMIN` behavior without adding admin capabilities.
- **FR-020**: The verification MUST compare repository and use case checks with the RLS policy model.
- **FR-021**: The verification MUST confirm no fake success behavior when Supabase blocks writes.
- **FR-022**: The verification MUST confirm the `PUBLIC_USER` app route does not fall back to generic Orders.
- **FR-023**: The verification MUST confirm app behavior does not crash on permission-denied responses.
- **FR-024**: The verification MUST produce all required verification documents.
- **FR-025**: The verification MUST run `.\gradlew.bat --no-daemon :app:compileDebugKotlin` and record the result.
- **FR-026**: The verification MUST NOT implement fixes during specification.
- **FR-027**: If a security gap is found later, it MUST be documented and proposed as a separate approved Phase 4.5.1 fix.

## Verification Deliverables

This phase must create the following documentation under `.specify/verification/`:

1. `.specify/verification/phase-4.5-rls-audit.md`
   - Existing RLS policies
   - Missing policies
   - Risky policies
   - Policy-to-code alignment notes
   - Recommendations

2. `.specify/verification/phase-4.5-test-matrix.md`
   - Test users by role
   - Required test data
   - Access matrix by role, table, operation, and expected result

3. `.specify/verification/phase-4.5-test-cases.md`
   - `PUBLIC_USER` tests
   - `PHARMACY` tests
   - `WAREHOUSE` tests
   - Admin documentation checks
   - App smoke tests

4. `.specify/verification/phase-4.5-qa-checklist.md`
   - Manual QA checklist
   - Evidence to capture
   - Pass/fail criteria
   - Compile result

5. `.specify/verification/phase-4.5-risks.md`
   - P0, P1, and P2 risks
   - Mitigation
   - Fix-now versus defer recommendation
   - Proposed Phase 4.5.1 items if needed

## Required Test Actors

The verification must use real Supabase-authenticated identities or equivalent authenticated Supabase sessions:

- `public_user_a`
- `public_user_b`
- `pharmacy_a`
- `pharmacy_b`
- `warehouse_a`
- `warehouse_b` when available
- `admin_a` only for current behavior documentation

Each actor must have an associated profile row sufficient to test role and ownership policies.

## Required Test Data

The verification must prepare or identify controlled rows for:

- A `CUSTOMER_PHARMACY` order owned by `public_user_a` and assigned to `pharmacy_a`
- A `CUSTOMER_PHARMACY` order owned by `public_user_b` and assigned to `pharmacy_b`
- A `CUSTOMER_PHARMACY` order in `PENDING`
- A `CUSTOMER_PHARMACY` order in at least one non-`PENDING` status
- A `PHARMACY_WAREHOUSE` order owned by `pharmacy_a` and assigned to `warehouse_a`
- A `PHARMACY_WAREHOUSE` order owned by `pharmacy_b` or another warehouse when feasible
- Supporting `profiles` rows for all test actors
- Supporting `requests` rows for B2B verification when needed

The verification must avoid relying on production customer data. If production-like data is unavoidable, all evidence must be redacted.

## RLS Audit Requirements

The audit must inspect policies for:

- `SELECT`
- `INSERT`
- `UPDATE`
- `DELETE`, if any policy exists or if deletion is intentionally disabled

For `orders`, the audit must specifically verify:

- `CUSTOMER_PHARMACY` rows are scoped by `customer_id` for public users.
- `CUSTOMER_PHARMACY` rows are scoped by `pharmacy_id` for pharmacies.
- `PHARMACY_WAREHOUSE` rows are scoped by `pharmacy_id` and `warehouse_id` according to existing B2B rules.
- Warehouses cannot read or update `CUSTOMER_PHARMACY`.
- Public users cannot read or update `PHARMACY_WAREHOUSE`.
- Updates are limited by role, ownership, order type, and allowed status transition.
- Insert policies prevent cross-role or cross-owner row creation.

For `requests`, the audit must verify:

- Existing B2B request access remains role-scoped.
- Public users cannot access B2B request data unless explicitly intended and safe.
- B2C order verification does not require exposing request data to public users.

For `profiles`, the audit must verify:

- Users can access only the profile data required for the app.
- Role and tenant identifiers cannot be spoofed by clients.
- Pharmacy and warehouse ownership identifiers used by RLS are trustworthy.

## Repository And Use Case Alignment

The verification must compare RLS behavior with:

- `GetMyOrdersUseCase`
- `CreateCustomerOrderUseCase`
- `CancelCustomerOrderUseCase`
- Pharmacy-side B2C lifecycle use cases, if present
- Existing B2B repository/use case flows
- `SupabasePharmaRepository` B2C validation

The verification must confirm:

- Role validation exists before sensitive operations.
- Ownership validation exists before sensitive operations.
- State transition validation exists before lifecycle writes.
- Repository writes do not return success when Supabase rejects the operation.
- UI and ViewModels do not bypass use cases for B2C order operations.

## App Smoke Verification

The real-device or emulator smoke test must verify:

1. Log in as a real `PUBLIC_USER`.
2. Create a `CUSTOMER_PHARMACY` order.
3. Confirm the database row has:
   - `order_type = CUSTOMER_PHARMACY`
   - `customer_id` matching the authenticated public user
   - `pharmacy_id` matching the selected pharmacy
   - `status = PENDING`
   - `total_price_cents = null`
4. Open My Orders.
5. Confirm only the public user's B2C orders appear.
6. Open order detail by `orderId`.
7. Confirm detail displays safe fields only.
8. Cancel while pending.
9. Confirm the order is cancelled in Supabase or an honest error is shown.
10. Confirm My Orders refreshes safely after cancellation.
11. Confirm no generic Orders route is used for public-user My Orders.

## Pass Criteria

Phase 4.5 passes only if all of the following are true:

- `PUBLIC_USER` cannot access other users' B2C orders.
- `PUBLIC_USER` cannot access B2B orders.
- `PUBLIC_USER` cannot cancel other users' orders.
- `PUBLIC_USER` cannot cancel non-`PENDING` orders.
- `PUBLIC_USER` cannot run pharmacy-side lifecycle actions.
- `PHARMACY` cannot access another pharmacy's B2C orders.
- `PHARMACY` cannot create B2C orders on behalf of customers.
- `WAREHOUSE` cannot see `CUSTOMER_PHARMACY` orders.
- `WAREHOUSE` cannot update `CUSTOMER_PHARMACY` orders.
- Create, cancel, confirm, reject, ready, out-for-delivery, and delivered lifecycle paths respect role, ownership, order type, and status.
- App smoke flow works with real Supabase.
- No fake success is observed.
- No app crash occurs on permission denied.
- `PUBLIC_USER` My Orders does not use the generic Orders route.
- Compile remains `BUILD SUCCESSFUL`.

## Fail Criteria

Phase 4.5 fails if any of the following are true:

- Any cross-user data leak is possible.
- Any cross-role data leak is possible.
- Unauthorized update succeeds.
- The app reports success while RLS actually blocked the write.
- `PUBLIC_USER` route falls back to generic Orders.
- `WAREHOUSE` can see `CUSTOMER_PHARMACY` orders.
- `PHARMACY` can see or manage another pharmacy's `CUSTOMER_PHARMACY` orders.
- `PUBLIC_USER` can see `PHARMACY_WAREHOUSE` orders.
- Permission denied crashes the app.

## Risks

- **P0**: RLS policy allows broad `orders` reads across users or roles.
  - **Mitigation**: Block closing the phase; document and propose Phase 4.5.1 RLS fix.
  - **Fix timing**: Required before any further order UI expansion.

- **P0**: RLS allows unauthorized lifecycle updates.
  - **Mitigation**: Block closing the phase; document exact policy and operation.
  - **Fix timing**: Required before pharmacy-side UI.

- **P0**: Repository reports success after an RLS-blocked write.
  - **Mitigation**: Block closing the phase; document repository behavior and failed assertion.
  - **Fix timing**: Required before release.

- **P1**: Admin behavior is incomplete or ambiguous.
  - **Mitigation**: Document as Phase 7 gap unless it exposes non-admin data to non-admin users.
  - **Fix timing**: Deferred unless it causes a P0 leak.

- **P1**: Test fixture creation requires service-role setup.
  - **Mitigation**: Document setup procedure, keep service-role keys out of the repo, and use redacted evidence.
  - **Fix timing**: Required for repeatable verification.

- **P2**: Existing B2B policies are hard to map to current app flows.
  - **Mitigation**: Document unknowns and add targeted tests before modifying B2B UI.
  - **Fix timing**: Deferred unless a leak is found.

## Non-Goals

- No UI implementation
- No UI redesign
- No new navigation
- No backend fixes during specification
- No schema migration during specification
- No model changes
- No repository refactor
- No admin expansion
- No real pharmacy discovery implementation
- No real medicine catalog implementation
- No payment
- No tracking
- No notifications
- No rejection reason

## Assumptions

- Phase 4.4C remains closed and compile-successful before this verification starts.
- Existing dirty backend files may include prior approved backend work and must not be treated as Phase 4.5 changes unless this phase modifies them.
- Supabase is the source of truth for real RLS verification.
- The verification runner has access to safe test credentials or a safe procedure for creating them.
- Service-role access, if needed for fixture setup, is used only outside the app and is never committed.
- RLS gaps found during verification are documented, not fixed, unless a separate Phase 4.5.1 fix is approved.

## Closure Decision

Close Phase 4.5 only when all required verification documents exist, all P0 checks pass, app smoke testing passes with real Supabase, and `.\gradlew.bat --no-daemon :app:compileDebugKotlin` reports `BUILD SUCCESSFUL`.

If any P0 security gap is found, Phase 4.5 must remain open and the next action must be a documented Phase 4.5.1 security fix proposal.
