# Phase 4.5 RLS Audit - Task 1

**Scope**: Audit current RLS policy evidence for `orders`, `requests`, and `profiles`.  
**Phase type**: Verification only.  
**Date**: 2026-04-29.  
**Result**: P0 found. Phase 4.5.1 required before closing Phase 4.5.

## Evidence Sources

Local evidence reviewed:

- `database/migrations/20250425_extend_orders_for_b2c.sql`
- `database/triggers/create_order_from_request.sql`
- `database/README_OrderCreationRefactor.md`
- `auth_debug_guide.md`
- `supabase_auth_guide.md`
- repo-wide search for `CREATE POLICY`, `ENABLE ROW LEVEL SECURITY`, `auth.uid`, `profiles`, `orders`, and `requests`

Live Supabase evidence:

- Supabase MCP resources were not exposed in this session.
- No Supabase Dashboard or live SQL output was available.
- Therefore, this audit is based on committed SQL/docs only. Any policies that exist only in the live database are marked `Requires runtime verification`.

## Executive Summary

The local repository contains concrete RLS policy definitions only for `orders`, in `database/migrations/20250425_extend_orders_for_b2c.sql`.

No local `CREATE POLICY` definitions were found for `requests` or `profiles`. The auth debug guide references a `users_insert_own_profile` policy, but the policy SQL is not present in the repo evidence. This means `requests` and `profiles` cannot be proven safe from local files alone.

The `orders` policies are directionally aligned with B2C/B2B isolation for `SELECT`, but there are P0 security gaps in write policies:

- `customer_cancel_pending` lets a `PUBLIC_USER` update any own `PENDING` B2C row into any new status, not only `CANCELLED`.
- `pharmacy_manage_b2c` lets an owning pharmacy update B2C rows without RLS-level lifecycle/status transition constraints.
- `customer_create_order` does not check `profiles.account_type = PUBLIC_USER`, so any authenticated account can insert a `CUSTOMER_PHARMACY` order for itself if it can satisfy the row shape.

No fixes were implemented.

## RLS Status By Table

| Table | Local RLS enabled evidence | Local policy evidence | Audit confidence |
|---|---:|---:|---|
| `orders` | Yes: `ALTER TABLE orders ENABLE ROW LEVEL SECURITY;` | Yes | High for local migration, runtime verification still required |
| `requests` | Not found in local evidence | Not found in local evidence | Requires runtime verification |
| `profiles` | Not found in local evidence | Not found in local evidence; docs mention `users_insert_own_profile` only | Requires runtime verification |

## `orders` Policy Inventory

### Policy: `customer_view_own_orders`

| Field | Value |
|---|---|
| Table | `orders` |
| Operation | `SELECT` |
| Role target | `TO authenticated` |
| USING | `auth.uid() = customer_id AND order_type = 'CUSTOMER_PHARMACY'` |
| WITH CHECK | None |
| Depends on `auth.uid()` | Yes |
| Depends on `profiles.account_type` | No |
| Depends on `profiles.pharmacy_id` / `profiles.warehouse_id` | No |
| Distinguishes B2C/B2B | Yes: only `CUSTOMER_PHARMACY` |
| Rating | Safe for owner-only public-user B2C reads; Requires runtime verification for combined-policy behavior |

Assessment:

- The policy prevents a user from selecting another user's B2C order via this policy.
- The policy excludes `PHARMACY_WAREHOUSE`.
- It does not check `profiles.account_type = PUBLIC_USER`, so any authenticated user whose `auth.uid()` appears in `customer_id` can read that row. This is less dangerous than broad read, but it depends on insert policies not allowing non-public roles to create or own B2C rows.

### Policy: `customer_create_order`

| Field | Value |
|---|---|
| Table | `orders` |
| Operation | `INSERT` |
| Role target | `TO authenticated` |
| USING | None |
| WITH CHECK | `auth.uid() = customer_id AND order_type = 'CUSTOMER_PHARMACY' AND pharmacy_id IS NOT NULL AND warehouse_id IS NULL AND request_id IS NULL AND status = 'PENDING'` |
| Depends on `auth.uid()` | Yes |
| Depends on `profiles.account_type` | No |
| Depends on `profiles.pharmacy_id` / `profiles.warehouse_id` | No |
| Distinguishes B2C/B2B | Yes: only `CUSTOMER_PHARMACY` |
| Rating | Risky / P0 cross-role mutation gap |

Assessment:

- The policy prevents creating a B2C order for another `customer_id`.
- The policy enforces B2C row shape and initial `PENDING` status.
- The policy does not require `profiles.account_type = PUBLIC_USER`.
- Because it targets all authenticated users, a `PHARMACY`, `WAREHOUSE`, or `ADMIN` authenticated user can theoretically create a `CUSTOMER_PHARMACY` order with `customer_id = auth.uid()`, even though this phase requires B2C creation to be `PUBLIC_USER` only.

P0 stop-condition note:

- **Table**: `orders`
- **Policy**: `customer_create_order`
- **Operation**: `INSERT`
- **Actor**: non-`PUBLIC_USER` authenticated account, especially `WAREHOUSE` or `PHARMACY`
- **Mutable data class**: `CUSTOMER_PHARMACY` order creation
- **Why P0**: Cross-role B2C creation is possible at the RLS policy layer because account type is not checked.
- **Proposed Phase 4.5.1 fix area**: Add RLS account-type enforcement for B2C insert, using a trusted profile lookup or claim, and verify with real role actors.

### Policy: `customer_cancel_pending`

| Field | Value |
|---|---|
| Table | `orders` |
| Operation | `UPDATE` |
| Role target | `TO authenticated` |
| USING | `auth.uid() = customer_id AND order_type = 'CUSTOMER_PHARMACY' AND status = 'PENDING'` |
| WITH CHECK | `auth.uid() = customer_id AND order_type = 'CUSTOMER_PHARMACY'` |
| Depends on `auth.uid()` | Yes |
| Depends on `profiles.account_type` | No |
| Depends on `profiles.pharmacy_id` / `profiles.warehouse_id` | No |
| Distinguishes B2C/B2B | Yes: only `CUSTOMER_PHARMACY` |
| Rating | Risky / P0 unauthorized lifecycle update gap |

Assessment:

- The old row must be the caller's own `PENDING` B2C order.
- The new row is only checked for same `customer_id` and `CUSTOMER_PHARMACY`.
- The `WITH CHECK` clause does not require `status = 'CANCELLED'`.
- The `WITH CHECK` clause does not prevent updates to `total_price_cents`, `pharmacy_id`, fulfillment fields, notes, or lifecycle fields, as long as the new row still has the same customer and B2C order type.
- This means the policy name says cancel, but the RLS rule appears to permit broad customer-side updates to own pending B2C orders.

P0 stop-condition note:

- **Table**: `orders`
- **Policy**: `customer_cancel_pending`
- **Operation**: `UPDATE`
- **Actor**: `PUBLIC_USER`
- **Mutable data class**: Own `PENDING` `CUSTOMER_PHARMACY` order
- **Why P0**: A public user appears able to perform unauthorized lifecycle or field changes, not only cancel pending orders.
- **Proposed Phase 4.5.1 fix area**: Restrict customer update policy to the exact allowed transition and permitted columns/row shape, or route cancellation through a hardened RPC that validates old/new state.

### Policy: `pharmacy_view_orders`

| Field | Value |
|---|---|
| Table | `orders` |
| Operation | `SELECT` |
| Role target | `TO authenticated` |
| USING | B2C: `order_type = 'CUSTOMER_PHARMACY' AND pharmacy_id = (SELECT pharmacy_id FROM profiles WHERE id = auth.uid())`; B2B: `order_type = 'PHARMACY_WAREHOUSE' AND pharmacy_id = (SELECT pharmacy_id FROM profiles WHERE id = auth.uid())` |
| WITH CHECK | None |
| Depends on `auth.uid()` | Yes, through profile lookup |
| Depends on `profiles.account_type` | No |
| Depends on `profiles.pharmacy_id` / `profiles.warehouse_id` | Yes: `profiles.pharmacy_id` |
| Distinguishes B2C/B2B | Yes: handles both `CUSTOMER_PHARMACY` and `PHARMACY_WAREHOUSE` |
| Rating | Ambiguous / Requires runtime verification |

Assessment:

- The policy scopes reads by `profiles.pharmacy_id`.
- It does not check `profiles.account_type = PHARMACY`.
- If non-pharmacy profiles can have a `pharmacy_id`, they may inherit pharmacy read access.
- Safety depends on `profiles` integrity: clients must not be able to spoof or update `pharmacy_id`.
- Runtime verification is required because `profiles` RLS policies are not present locally.

### Policy: `pharmacy_manage_b2c`

| Field | Value |
|---|---|
| Table | `orders` |
| Operation | `UPDATE` |
| Role target | `TO authenticated` |
| USING | `order_type = 'CUSTOMER_PHARMACY' AND pharmacy_id = (SELECT pharmacy_id FROM profiles WHERE id = auth.uid())` |
| WITH CHECK | None explicit |
| Depends on `auth.uid()` | Yes, through profile lookup |
| Depends on `profiles.account_type` | No |
| Depends on `profiles.pharmacy_id` / `profiles.warehouse_id` | Yes: `profiles.pharmacy_id` |
| Distinguishes B2C/B2B | Yes: only `CUSTOMER_PHARMACY` |
| Rating | Risky / P0 unauthorized lifecycle update gap |

Assessment:

- The policy scopes updates to B2C orders assigned to the caller's `profiles.pharmacy_id`.
- It does not check `profiles.account_type = PHARMACY`.
- It does not encode allowed status transitions.
- It does not distinguish confirm, reject, ready-for-pickup, out-for-delivery, or delivered transitions.
- Repository/use case code may validate transitions, but RLS itself does not appear to enforce lifecycle constraints. Direct API misuse could bypass client-side/use-case constraints if the anon/authenticated client can issue updates.

P0 stop-condition note:

- **Table**: `orders`
- **Policy**: `pharmacy_manage_b2c`
- **Operation**: `UPDATE`
- **Actor**: authenticated account with matching `profiles.pharmacy_id`
- **Mutable data class**: Own-pharmacy `CUSTOMER_PHARMACY` order
- **Why P0**: RLS does not enforce role type or lifecycle transition constraints for pharmacy-side B2C updates.
- **Proposed Phase 4.5.1 fix area**: Add account-type and transition-aware RLS constraints or replace direct lifecycle updates with validated RPC functions.

### Policy: `warehouse_view_b2b`

| Field | Value |
|---|---|
| Table | `orders` |
| Operation | `SELECT` |
| Role target | `TO authenticated` |
| USING | `order_type = 'PHARMACY_WAREHOUSE' AND warehouse_id = (SELECT warehouse_id FROM profiles WHERE id = auth.uid())` |
| WITH CHECK | None |
| Depends on `auth.uid()` | Yes, through profile lookup |
| Depends on `profiles.account_type` | No |
| Depends on `profiles.pharmacy_id` / `profiles.warehouse_id` | Yes: `profiles.warehouse_id` |
| Distinguishes B2C/B2B | Yes: only `PHARMACY_WAREHOUSE` |
| Rating | Safe for excluding B2C; Ambiguous for role integrity |

Assessment:

- The policy excludes `CUSTOMER_PHARMACY` rows.
- It scopes B2B rows by `profiles.warehouse_id`.
- It does not check `profiles.account_type = WAREHOUSE`.
- Safety depends on profile integrity and whether other profile roles can have or spoof `warehouse_id`.

### Policy: `warehouse_create_b2b`

| Field | Value |
|---|---|
| Table | `orders` |
| Operation | `INSERT` |
| Role target | `TO authenticated` |
| USING | None |
| WITH CHECK | `order_type = 'PHARMACY_WAREHOUSE' AND warehouse_id = (SELECT warehouse_id FROM profiles WHERE id = auth.uid()) AND customer_id IS NULL` |
| Depends on `auth.uid()` | Yes, through profile lookup |
| Depends on `profiles.account_type` | No |
| Depends on `profiles.pharmacy_id` / `profiles.warehouse_id` | Yes: `profiles.warehouse_id` |
| Distinguishes B2C/B2B | Yes: only `PHARMACY_WAREHOUSE` |
| Rating | Ambiguous / Requires runtime verification |

Assessment:

- The policy excludes B2C and requires a matching warehouse id.
- It does not check `profiles.account_type = WAREHOUSE`.
- It does not independently verify pharmacy/request ownership in this local evidence.
- Runtime verification is required against current B2B intended behavior.

### Policy: `warehouse_update_b2b`

| Field | Value |
|---|---|
| Table | `orders` |
| Operation | `UPDATE` |
| Role target | `TO authenticated` |
| USING | `order_type = 'PHARMACY_WAREHOUSE' AND warehouse_id = (SELECT warehouse_id FROM profiles WHERE id = auth.uid())` |
| WITH CHECK | None explicit |
| Depends on `auth.uid()` | Yes, through profile lookup |
| Depends on `profiles.account_type` | No |
| Depends on `profiles.pharmacy_id` / `profiles.warehouse_id` | Yes: `profiles.warehouse_id` |
| Distinguishes B2C/B2B | Yes: only `PHARMACY_WAREHOUSE` |
| Rating | Safe for excluding B2C; Ambiguous for lifecycle/status constraints |

Assessment:

- The policy excludes `CUSTOMER_PHARMACY` updates by warehouses.
- It scopes B2B rows by warehouse id.
- It does not check `profiles.account_type = WAREHOUSE`.
- It does not encode lifecycle/status transition constraints in local evidence.

## `orders` Operation Coverage

| Operation | Policies found | Coverage assessment |
|---|---|---|
| `SELECT` | `customer_view_own_orders`, `pharmacy_view_orders`, `warehouse_view_b2b` | Directionally scoped by user/customer/pharmacy/warehouse and order type, but profile role integrity needs runtime verification |
| `INSERT` | `customer_create_order`, `warehouse_create_b2b` | B2C insert lacks `PUBLIC_USER` account-type check; B2B insert lacks explicit `WAREHOUSE` account-type check |
| `UPDATE` | `customer_cancel_pending`, `pharmacy_manage_b2c`, `warehouse_update_b2b` | B2C customer update is too broad; pharmacy lifecycle updates lack transition constraints; role checks depend on profile integrity |
| `DELETE` | None found | With RLS enabled and no delete policy, deletes should be denied to authenticated clients; runtime verification required |

## `orders` Required Checks

### Can `PUBLIC_USER` see own `CUSTOMER_PHARMACY` only?

Local theoretical result: mostly yes for reads via `customer_view_own_orders`.

Evidence:

- `customer_view_own_orders` requires `auth.uid() = customer_id`.
- It requires `order_type = 'CUSTOMER_PHARMACY'`.

Caveat:

- The policy does not check `profiles.account_type = PUBLIC_USER`.
- Runtime verification is still needed with real public, pharmacy, and warehouse accounts.

Rating: Safe for local SELECT condition, Requires runtime verification.

### Is `PUBLIC_USER` blocked from `PHARMACY_WAREHOUSE`?

Local theoretical result: yes for the customer read policy.

Evidence:

- Customer select policy only allows `CUSTOMER_PHARMACY`.
- Customer insert policy requires `CUSTOMER_PHARMACY`.
- Customer update policy requires `CUSTOMER_PHARMACY`.

Rating: Safe for local customer policies, Requires runtime verification for combined policy/profile behavior.

### Is `PHARMACY` constrained to its own `pharmacy_id`?

Local theoretical result: yes by `profiles.pharmacy_id`, but role integrity is not proven.

Evidence:

- `pharmacy_view_orders` and `pharmacy_manage_b2c` compare `orders.pharmacy_id` to `(SELECT pharmacy_id FROM profiles WHERE id = auth.uid())`.

Caveat:

- No local `profiles` policy or trigger evidence proves that `pharmacy_id` is trustworthy or immutable by clients.
- No local policy checks `profiles.account_type = PHARMACY`.

Rating: Ambiguous / Requires runtime verification.

### Is `WAREHOUSE` blocked from `CUSTOMER_PHARMACY`?

Local theoretical result: yes for warehouse-named policies, but cross-role B2C insert remains a P0 concern.

Evidence:

- `warehouse_view_b2b`, `warehouse_create_b2b`, and `warehouse_update_b2b` require `order_type = 'PHARMACY_WAREHOUSE'`.

P0 caveat:

- `customer_create_order` targets all authenticated users and does not check account type. A warehouse account can theoretically create a B2C row for itself if it can satisfy `auth.uid() = customer_id`.

Rating: Risky / P0 for cross-role B2C creation.

### Does `INSERT` prevent creating an order as another user?

Local theoretical result: yes for B2C customer id, but no for role type.

Evidence:

- `customer_create_order` requires `auth.uid() = customer_id`.

Caveat:

- No `profiles.account_type = PUBLIC_USER` check.
- No policy-level validation that the target `pharmacy_id` is a valid pharmacy profile.

Rating: Risky.

### Does `UPDATE` prevent unauthorized lifecycle changes?

Local theoretical result: no.

Evidence:

- `customer_cancel_pending` allows selecting old own pending B2C rows but only checks new row for same customer and B2C type.
- It does not require new status to be `CANCELLED`.
- `pharmacy_manage_b2c` scopes ownership but does not encode lifecycle transitions.

Rating: P0.

### Is `DELETE` forbidden or controlled?

Local theoretical result: no delete policies found; if RLS is enabled, authenticated deletes should be denied by default.

Caveat:

- Runtime verification is required.

Rating: Requires runtime verification.

## `requests` Audit

Local policy evidence found: none.

RLS enabled evidence found: none.

Policy inventory:

| Operation | Policy evidence | Rating |
|---|---|---|
| `SELECT` | None found | Missing local evidence / Requires runtime verification |
| `INSERT` | None found | Missing local evidence / Requires runtime verification |
| `UPDATE` | None found | Missing local evidence / Requires runtime verification |
| `DELETE` | None found | Missing local evidence / Requires runtime verification |

Required verification:

- Prove `PUBLIC_USER` cannot read B2B `requests`.
- Prove `PHARMACY` can access only its own requests according to existing B2B rules.
- Prove `WAREHOUSE` can access only requests associated with its warehouse according to existing B2B rules.
- Prove request access does not expose B2B data to public users.

Assessment:

- Because no local RLS definitions for `requests` are present, safety cannot be proven from repo files.
- This is not automatically a P0 finding from local evidence alone, because policies may exist only in the live Supabase database.
- Task 2 or runtime audit must collect live policy output.

Rating: Requires runtime verification.

## `profiles` Audit

Local policy evidence found:

- `auth_debug_guide.md` mentions `users_insert_own_profile` in a troubleshooting table.
- No SQL definition for this policy was found in the repo.

RLS enabled evidence found: none.

Policy inventory:

| Operation | Policy evidence | Rating |
|---|---|---|
| `SELECT` | None found | Missing local evidence / Requires runtime verification |
| `INSERT` | `users_insert_own_profile` name referenced in docs only | Ambiguous / Requires runtime verification |
| `UPDATE` | None found | Missing local evidence / Requires runtime verification |
| `DELETE` | None found | Missing local evidence / Requires runtime verification |

Required verification:

- Prove users cannot spoof `account_type`.
- Prove users cannot spoof or update `pharmacy_id`.
- Prove users cannot spoof or update `warehouse_id`.
- Prove any profile reads are limited to app-required fields or app-required rows.
- Prove profile fields used by `orders` RLS are trustworthy.

Assessment:

- `orders` RLS depends on profile tenant fields for pharmacy and warehouse policies.
- Because profile policies are not present locally, the trustworthiness of `profiles.pharmacy_id` and `profiles.warehouse_id` cannot be proven from local files.
- If clients can update their own `pharmacy_id` or `warehouse_id`, the `orders` RLS model becomes unsafe.
- Runtime policy audit is mandatory before closing Phase 4.5.

Rating: Requires runtime verification.

## Missing / Risky / Ambiguous Policy Summary

| Severity | Table | Operation | Policy | Finding |
|---|---|---|---|---|
| P0 | `orders` | `UPDATE` | `customer_cancel_pending` | Public user can likely update own pending B2C order to statuses or fields beyond cancellation |
| P0 | `orders` | `UPDATE` | `pharmacy_manage_b2c` | Pharmacy-side B2C updates are not transition-constrained at RLS level |
| P0 | `orders` | `INSERT` | `customer_create_order` | Any authenticated role can likely create a B2C order for itself because account type is not checked |
| P1 | `orders` | `SELECT` / `UPDATE` | pharmacy and warehouse policies | Role checks depend on profile tenant fields but do not verify `account_type` |
| P1 | `profiles` | all | unknown | Local repo lacks policy SQL proving tenant fields cannot be spoofed |
| P1 | `requests` | all | unknown | Local repo lacks policy SQL proving B2B request isolation |
| P2 | `orders` | `DELETE` | none | Expected deny-by-default with RLS, but runtime verification needed |

## Recommendations For Phase 4.5.1 Proposal

Do not apply these in Task 1. These are proposed fix areas only:

1. Add account-type checks to B2C customer policies:
   - B2C create/select/cancel should require trusted `profiles.account_type = 'PUBLIC_USER'`.

2. Harden customer cancellation:
   - Restrict the update to exactly `PENDING -> CANCELLED`.
   - Prevent customer-side changes to price, pharmacy ownership, fulfillment, and lifecycle fields.
   - Consider a validated RPC for cancellation if column-level transition enforcement is difficult in RLS.

3. Harden pharmacy lifecycle updates:
   - Require trusted `profiles.account_type = 'PHARMACY'`.
   - Enforce allowed status transitions at RLS or via validated RPC functions.
   - Prevent changing customer ownership/order type.

4. Harden warehouse policies:
   - Require trusted `profiles.account_type = 'WAREHOUSE'`.
   - Confirm B2B lifecycle rules and status transitions.

5. Verify and harden `profiles`:
   - Ensure users cannot update `account_type`, `pharmacy_id`, or `warehouse_id`.
   - Consider server-side/admin-only assignment for role and tenant identifiers.

6. Verify and harden `requests`:
   - Ensure public users cannot read B2B request data.
   - Ensure pharmacy/warehouse access is tenant-scoped.

## Stop Condition Result

P0 found: yes.

Phase 4.5.1 required: yes.

No fixes were implemented. No schema, migration, repository, domain, model, UI, or navigation files were changed.
