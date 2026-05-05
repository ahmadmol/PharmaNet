# Feature Specification: Phase 4.5.1 - Approved RLS Security Fixes for PUBLIC_USER B2C Orders

**Feature Branch**: `phase-4.5.1-rls-security-fixes`  
**Created**: 2026-04-29  
**Status**: Draft  
**Input**: User description: "Phase 4.5.1: Approved RLS Security Fixes for PUBLIC_USER B2C Orders"

## Executive Summary

Phase 4.5 Task 1 found P0 security gaps in local `orders` RLS evidence. Phase 4.5 cannot close until those gaps are fixed or explicitly mitigated.

This phase designs the minimal approved RLS/security fix for `CUSTOMER_PHARMACY` orders. The design must harden B2C create, customer cancellation, and pharmacy lifecycle updates without changing app UI, navigation, domain models, or the order architecture.

Key design decision:

- Direct `INSERT` policies can safely enforce B2C create row shape and account type.
- Direct broad `UPDATE` policies are not enough for exact lifecycle safety because normal RLS policy predicates cannot reliably express and compare all OLD-to-NEW transition constraints and immutable-field requirements.
- Customer cancellation and pharmacy lifecycle updates should therefore move to validated RPC functions, with broad direct client `UPDATE` policies dropped or replaced by deny-by-default/no direct update for B2C mutation paths.

## Exact P0 Gaps Being Fixed

### P0-001: `orders.customer_create_order` lacks `PUBLIC_USER` account-type check

Current local evidence:

- Policy checks `auth.uid() = customer_id`.
- Policy checks `order_type = 'CUSTOMER_PHARMACY'`.
- Policy checks initial B2C shape.
- Policy does not check `profiles.account_type = 'PUBLIC_USER'`.

Risk:

- Any authenticated account may be able to create a B2C order for itself, including `PHARMACY`, `WAREHOUSE`, or `ADMIN`.

Fix objective:

- Only authenticated users whose trusted profile has `account_type = 'PUBLIC_USER'` can insert `CUSTOMER_PHARMACY` orders.

### P0-002: `orders.customer_cancel_pending` allows broad customer-side updates

Current local evidence:

- Old row is scoped to `auth.uid() = customer_id`, `CUSTOMER_PHARMACY`, and `PENDING`.
- New row only checks same `customer_id` and `CUSTOMER_PHARMACY`.
- New row does not require `status = 'CANCELLED'`.
- New row does not prove ownership fields and sensitive fields are preserved.

Risk:

- A public user may be able to update own pending B2C order to unsupported statuses or mutate fields beyond cancellation.

Fix objective:

- Public users can only cancel own pending B2C orders through an exact validated operation.

### P0-003: `orders.pharmacy_manage_b2c` lacks pharmacy role and lifecycle constraints

Current local evidence:

- Policy scopes by `orders.pharmacy_id = profiles.pharmacy_id`.
- Policy does not require `profiles.account_type = 'PHARMACY'`.
- Policy does not enforce allowed lifecycle transitions.

Risk:

- Authenticated accounts with a matching or spoofed `pharmacy_id` may update B2C rows.
- Owning pharmacy may perform broad direct updates beyond approved lifecycle transitions.

Fix objective:

- Only trusted `PHARMACY` accounts can perform approved B2C lifecycle transitions for their own pharmacy.
- Lifecycle mutation must be transition-specific and field-safe.

## Scope

In scope:

- `orders` RLS policy replacement for B2C create/read/update hardening.
- Account-type checks using trusted `profiles` lookup.
- Customer B2C insert hardening.
- Customer cancellation hardening.
- Pharmacy B2C lifecycle hardening.
- Optional warehouse account-type hardening only if the migration touches warehouse policies.
- Validated RPC design for B2C lifecycle writes.
- Profiles trust verification requirement.
- SQL migration strategy.
- Verification SQL and manual QA plan.
- Compile check after migration file is added during implementation.

Out of scope:

- UI changes
- Navigation changes
- `Order.kt` changes
- `Request.kt` changes
- Repository/use case changes unless separately approved as required by migration/RPC integration
- `rejectionReason` or `statusReason`
- Payment
- Tracking
- Inventory
- New order architecture
- Unified transactions
- `PHARMACY` UI
- `WAREHOUSE` UI
- `ADMIN` UI
- Real pharmacy discovery
- Real medicine catalog

## Proposed SQL Migration Strategy

The implementation phase should add a new migration, for example:

`database/migrations/20260429_harden_b2c_order_rls.sql`

The migration should be idempotent where practical and must not include secrets or test credentials.

### Migration Sequence

1. Ensure RLS remains enabled on `orders`.
2. Add helper functions for trusted profile checks if needed:
   - `current_account_type()`
   - `current_pharmacy_id()`
   - `current_warehouse_id()`
3. Drop or replace risky `orders` policies:
   - `customer_create_order`
   - `customer_cancel_pending`
   - `pharmacy_manage_b2c`
4. Replace B2C create policy with `PUBLIC_USER` account-type enforcement.
5. Remove direct customer B2C update policy.
6. Remove direct broad pharmacy B2C update policy.
7. Add validated RPC functions for:
   - `cancel_customer_order(order_id)`
   - `confirm_customer_order(order_id, total_price_cents)`
   - `reject_customer_order(order_id)`
   - `mark_customer_order_ready_for_pickup(order_id)`
   - `mark_customer_order_out_for_delivery(order_id)`
   - `mark_customer_order_delivered(order_id)`
8. Grant execute on these RPC functions only to `authenticated`.
9. Ensure RPC functions perform role, ownership, order type, and status transition checks.
10. Ensure direct `UPDATE` for B2C rows is not broadly available to authenticated clients.
11. Add verification comments or companion SQL checks in `.specify/verification` during implementation.

## RLS Alone Versus RPC Decision

### Can exact lifecycle transitions be safely enforced with RLS alone?

Answer: Not safely with the current direct-table update shape.

Reason:

- Standard RLS `USING` checks the visible old row.
- Standard RLS `WITH CHECK` checks the proposed new row.
- RLS policies are not a good fit for comparing a complete OLD row to a complete NEW row and guaranteeing immutable fields are preserved across multiple lifecycle operations.
- Column-level restrictions would still need careful privilege design and may become brittle as the schema evolves.
- The customer cancel policy must prove `PENDING -> CANCELLED` and preserve ownership fields.
- The pharmacy lifecycle policy must prove a small finite state machine with fulfillment-type-dependent transitions.

Decision:

- Use RLS for read/insert row visibility and row shape.
- Use validated RPC functions for customer cancel and pharmacy lifecycle writes.
- Drop or avoid broad direct `UPDATE` policies for B2C lifecycle changes.

## Policy And RPC Design

### Helper Function Design

Optional helper functions should be `SECURITY DEFINER` only if necessary and must set a safe `search_path`.

Recommended shape:

```sql
create or replace function public.current_account_type()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select p.account_type
  from public.profiles p
  where p.id = auth.uid()
$$;
```

Similar helpers may be added for `current_pharmacy_id()` and `current_warehouse_id()`.

If helper functions are not used, policies/RPCs may inline trusted profile lookups.

### Fix 1: Harden `customer_create_order`

Replace the existing policy with a policy requiring:

- `auth.uid() = customer_id`
- `order_type = 'CUSTOMER_PHARMACY'`
- `status = 'PENDING'`
- `warehouse_id IS NULL`
- `request_id IS NULL`
- `pharmacy_id IS NOT NULL`
- `total_price_cents IS NULL`, if the column exists
- trusted `profiles.account_type = 'PUBLIC_USER'` for `auth.uid()`

Required behavior:

- A `PUBLIC_USER` can create their own B2C pending order.
- A `PUBLIC_USER` cannot create a B2B row.
- A `PHARMACY`, `WAREHOUSE`, or `ADMIN` cannot create a B2C order through this policy.
- A user cannot create a B2C order for another `customer_id`.

### Fix 2: Harden `customer_cancel_pending`

Direct broad update policy should not remain available for customers.

Preferred design:

- Drop `customer_cancel_pending`.
- Add `cancel_customer_order(order_id)` RPC.

RPC validation requirements:

- Caller must be authenticated.
- Caller's trusted profile must have `account_type = 'PUBLIC_USER'`.
- Target row must exist.
- Target row must have `order_type = 'CUSTOMER_PHARMACY'`.
- Target row must have `customer_id = auth.uid()`.
- Target row must have `status = 'PENDING'`.
- Target row must have `warehouse_id IS NULL`.
- Target row must have `request_id IS NULL`.
- RPC updates only allowed fields:
  - `status = 'CANCELLED'`
  - `updated_at = now()` if applicable
- RPC must not change:
  - `customer_id`
  - `pharmacy_id`
  - `warehouse_id`
  - `request_id`
  - `order_type`
  - `total_price_cents`
  - lifecycle fields unrelated to cancellation

Required behavior:

- Public user can cancel own pending B2C order.
- Public user cannot cancel another customer's order.
- Public user cannot cancel non-pending order.
- Public user cannot mutate fields beyond cancellation.

### Fix 3: Harden `pharmacy_manage_b2c`

Direct broad update policy should not remain available for pharmacies.

Preferred design:

- Drop `pharmacy_manage_b2c`.
- Add transition-specific RPC functions:
  - `confirm_customer_order(order_id, total_price_cents)`
  - `reject_customer_order(order_id)`
  - `mark_customer_order_ready_for_pickup(order_id)`
  - `mark_customer_order_out_for_delivery(order_id)`
  - `mark_customer_order_delivered(order_id)`

Shared RPC validation requirements:

- Caller must be authenticated.
- Caller's trusted profile must have `account_type = 'PHARMACY'`.
- Caller profile must have a non-null `pharmacy_id`.
- Target row must exist.
- Target row must have `order_type = 'CUSTOMER_PHARMACY'`.
- Target row `pharmacy_id` must match caller profile `pharmacy_id`.
- Target row ownership fields must remain unchanged.
- Target row `customer_id` must remain unchanged.
- Target row `warehouse_id` must remain null.
- Target row `request_id` must remain null.

Allowed transitions:

- `PENDING -> CONFIRMED`
- `PENDING -> REJECTED`
- `CONFIRMED -> READY_FOR_PICKUP` when `fulfillment_type = 'PICKUP'`
- `CONFIRMED -> OUT_FOR_DELIVERY` when `fulfillment_type = 'DELIVERY'`
- `READY_FOR_PICKUP -> DELIVERED`
- `OUT_FOR_DELIVERY -> DELIVERED`

Transition-specific requirements:

- `confirm_customer_order(order_id, total_price_cents)`
  - Requires current status `PENDING`.
  - Requires `total_price_cents >= 0`.
  - Sets `status = 'CONFIRMED'`.
  - Sets `total_price_cents`.
  - Sets `confirmed_at = now()` if applicable.

- `reject_customer_order(order_id)`
  - Requires current status `PENDING`.
  - Sets `status = 'REJECTED'`.
  - Does not write rejection reason.
  - Does not use `notes` as rejection reason.

- `mark_customer_order_ready_for_pickup(order_id)`
  - Requires current status `CONFIRMED`.
  - Requires `fulfillment_type = 'PICKUP'`.
  - Sets `status = 'READY_FOR_PICKUP'`.

- `mark_customer_order_out_for_delivery(order_id)`
  - Requires current status `CONFIRMED`.
  - Requires `fulfillment_type = 'DELIVERY'`.
  - Sets `status = 'OUT_FOR_DELIVERY'`.

- `mark_customer_order_delivered(order_id)`
  - Requires current status in `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`.
  - Sets `status = 'DELIVERED'`.
  - Sets `fulfilled_at = now()` if applicable.

Required behavior:

- Owning pharmacy can perform only allowed transitions.
- Other pharmacies cannot view or mutate the order.
- Warehouse cannot mutate B2C orders.
- Public users cannot run pharmacy lifecycle RPCs.

### Fix 4: Profiles Trust Dependency

Because hardened `orders` RLS depends on `profiles.account_type`, `profiles.pharmacy_id`, and `profiles.warehouse_id`, implementation must verify profile trust before final closure.

Required verification:

- Determine whether `profiles` RLS policies exist in live database.
- Determine whether local migrations are missing profile policies.
- Confirm users cannot update:
  - `account_type`
  - `pharmacy_id`
  - `warehouse_id`
- Confirm role and tenant identifiers are assigned by trusted code only.

If profile trust cannot be verified locally:

- Mark as runtime verification requirement in Phase 4.5.1 report.
- Do not close Phase 4.5 until runtime verification is complete.

If users can update role or tenant fields:

- Mark as P0.
- Add a Phase 4.5.1 profiles hardening subtask before closing.

## Existing Policies To Drop Or Replace

Must drop/replace:

- `customer_create_order`
- `customer_cancel_pending`
- `pharmacy_manage_b2c`

May keep with optional account-type hardening:

- `customer_view_own_orders`
- `pharmacy_view_orders`
- `warehouse_view_b2b`
- `warehouse_create_b2b`
- `warehouse_update_b2b`

Recommended hardening if touched:

- Add `profiles.account_type = 'PUBLIC_USER'` to customer read.
- Add `profiles.account_type = 'PHARMACY'` to pharmacy read.
- Add `profiles.account_type = 'WAREHOUSE'` to warehouse B2B policies.

## Test Matrix

### PUBLIC_USER

| Test | Expected result |
|---|---|
| Public user creates own B2C order | Success |
| Public user creates B2C order for another customer | Denied |
| Public user creates B2B order | Denied |
| Pharmacy account creates B2C order for itself | Denied |
| Warehouse account creates B2C order for itself | Denied |
| Public user cancels own pending B2C via RPC | Success |
| Public user cancels another customer's B2C via RPC | Denied |
| Public user cancels non-pending B2C via RPC | Denied |
| Public user direct-updates B2C order table | Denied |
| Public user runs pharmacy lifecycle RPC | Denied |

### PHARMACY

| Test | Expected result |
|---|---|
| Owning pharmacy confirms pending B2C | Success |
| Owning pharmacy rejects pending B2C | Success |
| Owning pharmacy marks confirmed pickup order ready | Success |
| Owning pharmacy marks confirmed delivery order out for delivery | Success |
| Owning pharmacy marks ready/out-for-delivery order delivered | Success |
| Pharmacy confirms another pharmacy's B2C | Denied |
| Pharmacy performs invalid status transition | Denied |
| Pharmacy direct-updates B2C order table broadly | Denied |
| Pharmacy creates B2C order on behalf of customer | Denied |
| Pharmacy runs customer cancellation RPC | Denied |

### WAREHOUSE

| Test | Expected result |
|---|---|
| Warehouse reads own B2B rows | Success according to existing B2B rules |
| Warehouse reads B2C rows | Denied / zero rows |
| Warehouse updates B2C row directly | Denied |
| Warehouse creates B2C row | Denied |
| Warehouse runs customer cancellation RPC | Denied |
| Warehouse runs pharmacy lifecycle RPC | Denied |

### ADMIN

Admin behavior remains documentation-only in this phase. If admin bypass behavior is required later, defer to Phase 7 unless it creates a non-admin leak.

## Verification Plan

Implementation of this spec must include:

- SQL policy inspection after migration.
- Direct API/SQL tests with real authenticated test actors.
- App smoke test for public-user create, My Orders, detail, and cancel.
- Negative tests for cross-role and cross-tenant access.
- Compile check:

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

No service-role keys, JWTs, passwords, or real secrets may be committed.

## Rollback Strategy

The migration must be written so it can be reverted safely if it blocks legitimate flows.

Rollback plan should include:

1. Drop new RPC functions:
   - `cancel_customer_order`
   - `confirm_customer_order`
   - `reject_customer_order`
   - `mark_customer_order_ready_for_pickup`
   - `mark_customer_order_out_for_delivery`
   - `mark_customer_order_delivered`
2. Drop newly added replacement policies.
3. Restore previous policies only if necessary for emergency rollback:
   - `customer_create_order`
   - `customer_cancel_pending`
   - `pharmacy_manage_b2c`
4. Re-run RLS audit and app smoke tests after rollback.

Because previous policies contain P0 gaps, rollback must be treated as emergency-only and must reopen Phase 4.5.1.

## Acceptance Criteria

Phase 4.5.1 implementation can begin only after this spec is accepted.

Implementation can close only when all of the following are true:

- Non-`PUBLIC_USER` accounts cannot create `CUSTOMER_PHARMACY` orders.
- Public users cannot create B2B rows.
- Public users cannot create B2C rows for another customer.
- Public users can cancel only own `PENDING -> CANCELLED` B2C orders.
- Public users cannot direct-update B2C orders broadly.
- Public users cannot run pharmacy lifecycle RPCs.
- Pharmacies can only manage B2C orders for their own `pharmacy_id`.
- Pharmacies can only perform allowed lifecycle transitions.
- Pharmacies cannot direct-update B2C orders broadly.
- Warehouses cannot view or update `CUSTOMER_PHARMACY` rows.
- Profile role/tenant fields are verified trustworthy or separately hardened.
- No UI/navigation/domain/model changes are introduced.
- No rejection reason is introduced.
- App smoke flow remains successful.
- Compile reports `BUILD SUCCESSFUL`.

## Required Questions Answered

### Can we safely enforce exact lifecycle transitions with RLS alone?

No, not safely with the current broad direct-table update approach. Exact transition and immutable-field guarantees should use validated RPC functions.

### Which RPCs are required?

- `cancel_customer_order(order_id)`
- `confirm_customer_order(order_id, total_price_cents)`
- `reject_customer_order(order_id)`
- `mark_customer_order_ready_for_pickup(order_id)`
- `mark_customer_order_out_for_delivery(order_id)`
- `mark_customer_order_delivered(order_id)`

### Which existing policies must be dropped/replaced?

- `customer_create_order`
- `customer_cancel_pending`
- `pharmacy_manage_b2c`

### How do we prove non-`PUBLIC_USER` cannot create B2C?

Run authenticated insert attempts as `PHARMACY`, `WAREHOUSE`, and `ADMIN` test actors and verify RLS denies `CUSTOMER_PHARMACY` inserts even when `customer_id = auth.uid()`.

### How do we prove `PUBLIC_USER` can only cancel `PENDING -> CANCELLED`?

Run RPC and direct-table negative tests:

- Own pending B2C via `cancel_customer_order` succeeds and status becomes `CANCELLED`.
- Own confirmed/delivered/cancelled B2C via RPC is denied.
- Another user's pending B2C via RPC is denied.
- Direct table updates to any B2C fields are denied.

### How do we prove `PHARMACY` can only perform allowed lifecycle transitions?

Run each RPC with valid and invalid starting states, fulfillment types, and pharmacy ownership:

- Valid transitions succeed.
- Invalid transitions fail.
- Other pharmacy's orders fail.
- Direct broad table update fails.

### How do we prove `WAREHOUSE` cannot access `CUSTOMER_PHARMACY`?

Run warehouse authenticated read/update/insert/RPC attempts:

- B2C select returns zero rows or denied.
- B2C direct update denied.
- B2C insert denied.
- Customer and pharmacy RPCs denied.

## Non-Goals

- No UI work
- No navigation work
- No repository refactor
- No domain/model changes
- No schema redesign beyond minimal RLS/RPC migration
- No rejection reason
- No payment
- No tracking
- No inventory
- No new PHARMACY UI
- No new WAREHOUSE UI
- No ADMIN expansion

## Assumptions

- `profiles.id = auth.uid()` is intended to link authenticated users to app profiles.
- Profile role and tenant fields are intended to be trusted by RLS.
- If that trust is not true in the live database, profile hardening becomes part of Phase 4.5.1 before closure.
- Existing app repository methods may need a later approved integration task if they currently use direct table updates instead of RPC calls.
- This specification step does not implement SQL or app changes.
