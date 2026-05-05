# Phase 4.5.1 SQL Migration Design

**Task**: Task 2 - Design Final SQL Migration File  
**Phase**: Phase 4.5.1 RLS Security Fixes  
**Date**: 2026-04-29  
**Scope**: Design only. No SQL was executed. No migration file was created.  
**Implementation changes**: None.

## Baseline Inputs

This design is based on:

- `.specify/verification/phase-4.5.1-policy-baseline.md`
- `.specify/verification/phase-4.5-rls-audit.md`
- `.specify/tasks/phase-4.5.1-rls-security-fixes/tasks.md`
- Local policy evidence in `database/migrations/20250425_extend_orders_for_b2c.sql`

Live Supabase policy evidence remains unavailable in the current session. Live verification is still required before claiming production is fixed.

## Proposed Migration File

Proposed migration name:

`database/migrations/20260429_harden_b2c_order_rls.sql`

This file must not be created until Task 3 is explicitly approved.

## Policies To Drop Or Replace

The migration should drop the risky B2C policies identified in the Phase 4.5 audit:

| Policy | Operation | Reason |
|---|---|---|
| `customer_create_order` | `INSERT` | Missing `PUBLIC_USER` account-type check |
| `customer_cancel_pending` | `UPDATE` | Allows broad updates to own pending B2C rows instead of only cancellation |
| `pharmacy_manage_b2c` | `UPDATE` | Missing `PHARMACY` account-type check and transition constraints |

Design snippet only:

```sql
drop policy if exists customer_create_order on public.orders;
drop policy if exists customer_cancel_pending on public.orders;
drop policy if exists pharmacy_manage_b2c on public.orders;
```

## Replacement `customer_create_order` Policy

The replacement insert policy should allow only a real `PUBLIC_USER` to create their own pending `CUSTOMER_PHARMACY` order.

Required conditions:

- `auth.uid() = customer_id`
- `order_type = 'CUSTOMER_PHARMACY'`
- `status = 'PENDING'`
- `warehouse_id IS NULL`
- `request_id IS NULL`
- `pharmacy_id IS NOT NULL`
- `total_price_cents IS NULL`
- Current user's `profiles.account_type = 'PUBLIC_USER'`

Design snippet only:

```sql
create policy customer_create_order
on public.orders
for insert
to authenticated
with check (
  auth.uid() = customer_id
  and order_type = 'CUSTOMER_PHARMACY'
  and status = 'PENDING'
  and warehouse_id is null
  and request_id is null
  and pharmacy_id is not null
  and total_price_cents is null
  and exists (
    select 1
    from public.profiles p
    where p.id = auth.uid()
      and p.account_type = 'PUBLIC_USER'
  )
);
```

## Direct UPDATE Policy Decision

The migration should not replace the dropped B2C update policies with broad direct `UPDATE` policies.

Decisions:

- No broad direct customer `UPDATE` for `CUSTOMER_PHARMACY`.
- No broad direct pharmacy `UPDATE` for `CUSTOMER_PHARMACY`.
- Customer cancellation should be done through a validated RPC.
- Pharmacy lifecycle actions should be done through transition-specific validated RPCs.

Reason:

Standard RLS update policies split checks between the old row (`USING`) and the proposed new row (`WITH CHECK`). That model is not safe enough here because the system must prove exact transitions and preserve sensitive fields across the old and new row. A broad table update policy can accidentally allow field mutation beyond the intended action, especially as the schema grows.

RPC functions can lock each action to one validated transition, check the caller role and ownership, and update only the approved columns.

## RPC Security Model

Recommended function mode:

- Use `SECURITY DEFINER` with a safe `search_path = public`.
- Each function must validate `auth.uid()` and the caller profile before writing.
- Each function must perform a single narrow transition.
- Each function must update only the columns owned by that transition.
- Each function should raise an error on invalid role, ownership, status, fulfillment type, or missing target row.

Why `SECURITY DEFINER`:

- Direct broad B2C `UPDATE` policies are intentionally removed.
- `SECURITY INVOKER` would either fail because the caller has no direct update path or require reintroducing broad update policies.
- `SECURITY DEFINER` is acceptable only if every function performs strict caller validation using `auth.uid()` and trusted `profiles` fields.

Expected failure behavior:

- Unauthorized or invalid operations should fail with a database error.
- The repository must surface failure honestly.
- No function should silently return success when no valid row was updated.

Return contract:

- Recommended: return the updated `public.orders` row for verification and future repository compatibility.
- Task 4 must confirm whether the current repository can ignore or consume the returned row before implementation.

## RPC Function Designs

### `cancel_customer_order(order_id)`

Recommended SQL signature:

```sql
public.cancel_customer_order(p_order_id text)
returns public.orders
```

| Design field | Requirement |
|---|---|
| Allowed actor role | `PUBLIC_USER` |
| Account-type check | `profiles.account_type = 'PUBLIC_USER'` for `auth.uid()` |
| Ownership check | `orders.customer_id = auth.uid()` |
| Order type check | `orders.order_type = 'CUSTOMER_PHARMACY'` |
| Required current status | `PENDING` |
| Fulfillment check | None |
| Exact status update | `PENDING -> CANCELLED` |
| Timestamp updates | Set `updated_at = now()` if column exists |
| `total_price_cents` | Must not be changed |
| Expected failure behavior | Raise error if caller is not public user, row is not owned by caller, row is not B2C, or status is not `PENDING` |

The function must preserve:

- `customer_id`
- `pharmacy_id`
- `warehouse_id`
- `request_id`
- `order_type`
- `total_price_cents`
- fulfillment fields
- notes

### `confirm_customer_order(order_id, total_price_cents)`

Recommended SQL signature:

```sql
public.confirm_customer_order(p_order_id text, p_total_price_cents bigint)
returns public.orders
```

| Design field | Requirement |
|---|---|
| Allowed actor role | `PHARMACY` |
| Account-type check | `profiles.account_type = 'PHARMACY'` for `auth.uid()` |
| Ownership check | `orders.pharmacy_id = profiles.pharmacy_id` |
| Order type check | `orders.order_type = 'CUSTOMER_PHARMACY'` |
| Required current status | `PENDING` |
| Fulfillment check | None |
| Exact status update | `PENDING -> CONFIRMED` |
| Timestamp updates | Set `updated_at = now()` and `confirmed_at = now()` if columns exist |
| `total_price_cents` | Set to `p_total_price_cents`; require non-null and `>= 0` |
| Expected failure behavior | Raise error if caller is not owning pharmacy, status is not `PENDING`, or price is invalid |

The function must not change customer ownership, pharmacy ownership, warehouse ownership, request linkage, order type, fulfillment fields, or notes.

### `reject_customer_order(order_id)`

Recommended SQL signature:

```sql
public.reject_customer_order(p_order_id text)
returns public.orders
```

| Design field | Requirement |
|---|---|
| Allowed actor role | `PHARMACY` |
| Account-type check | `profiles.account_type = 'PHARMACY'` for `auth.uid()` |
| Ownership check | `orders.pharmacy_id = profiles.pharmacy_id` |
| Order type check | `orders.order_type = 'CUSTOMER_PHARMACY'` |
| Required current status | `PENDING` |
| Fulfillment check | None |
| Exact status update | `PENDING -> REJECTED` |
| Timestamp updates | Set `updated_at = now()` if column exists |
| `total_price_cents` | Must not be set by rejection |
| Expected failure behavior | Raise error if caller is not owning pharmacy or status is not `PENDING` |

The function must not introduce or write `rejectionReason` or `statusReason`, and must not use notes as a rejection reason.

### `mark_customer_order_ready_for_pickup(order_id)`

Recommended SQL signature:

```sql
public.mark_customer_order_ready_for_pickup(p_order_id text)
returns public.orders
```

| Design field | Requirement |
|---|---|
| Allowed actor role | `PHARMACY` |
| Account-type check | `profiles.account_type = 'PHARMACY'` for `auth.uid()` |
| Ownership check | `orders.pharmacy_id = profiles.pharmacy_id` |
| Order type check | `orders.order_type = 'CUSTOMER_PHARMACY'` |
| Required current status | `CONFIRMED` |
| Fulfillment check | `fulfillment_type = 'PICKUP'` |
| Exact status update | `CONFIRMED -> READY_FOR_PICKUP` |
| Timestamp updates | Set `updated_at = now()` if column exists |
| `total_price_cents` | Must not be changed |
| Expected failure behavior | Raise error if caller is not owning pharmacy, status is not `CONFIRMED`, or fulfillment type is not `PICKUP` |

### `mark_customer_order_out_for_delivery(order_id)`

Recommended SQL signature:

```sql
public.mark_customer_order_out_for_delivery(p_order_id text)
returns public.orders
```

| Design field | Requirement |
|---|---|
| Allowed actor role | `PHARMACY` |
| Account-type check | `profiles.account_type = 'PHARMACY'` for `auth.uid()` |
| Ownership check | `orders.pharmacy_id = profiles.pharmacy_id` |
| Order type check | `orders.order_type = 'CUSTOMER_PHARMACY'` |
| Required current status | `CONFIRMED` |
| Fulfillment check | `fulfillment_type = 'DELIVERY'` |
| Exact status update | `CONFIRMED -> OUT_FOR_DELIVERY` |
| Timestamp updates | Set `updated_at = now()` if column exists |
| `total_price_cents` | Must not be changed |
| Expected failure behavior | Raise error if caller is not owning pharmacy, status is not `CONFIRMED`, or fulfillment type is not `DELIVERY` |

### `mark_customer_order_delivered(order_id)`

Recommended SQL signature:

```sql
public.mark_customer_order_delivered(p_order_id text)
returns public.orders
```

| Design field | Requirement |
|---|---|
| Allowed actor role | `PHARMACY` |
| Account-type check | `profiles.account_type = 'PHARMACY'` for `auth.uid()` |
| Ownership check | `orders.pharmacy_id = profiles.pharmacy_id` |
| Order type check | `orders.order_type = 'CUSTOMER_PHARMACY'` |
| Required current status | `READY_FOR_PICKUP` or `OUT_FOR_DELIVERY` |
| Fulfillment check | `READY_FOR_PICKUP` should correspond to `PICKUP`; `OUT_FOR_DELIVERY` should correspond to `DELIVERY` |
| Exact status update | `READY_FOR_PICKUP -> DELIVERED` or `OUT_FOR_DELIVERY -> DELIVERED` |
| Timestamp updates | Set `updated_at = now()` and `fulfilled_at = now()` if columns exist |
| `total_price_cents` | Must not be changed |
| Expected failure behavior | Raise error if caller is not owning pharmacy or status is not deliverable |

## Transition Rules

### PUBLIC_USER

| From | To | Function |
|---|---|---|
| `PENDING` | `CANCELLED` | `cancel_customer_order(order_id)` |

No other public-user lifecycle transition is allowed.

### PHARMACY

| From | To | Extra condition | Function |
|---|---|---|---|
| `PENDING` | `CONFIRMED` | Valid final price | `confirm_customer_order(order_id, total_price_cents)` |
| `PENDING` | `REJECTED` | None | `reject_customer_order(order_id)` |
| `CONFIRMED` | `READY_FOR_PICKUP` | `fulfillment_type = 'PICKUP'` | `mark_customer_order_ready_for_pickup(order_id)` |
| `CONFIRMED` | `OUT_FOR_DELIVERY` | `fulfillment_type = 'DELIVERY'` | `mark_customer_order_out_for_delivery(order_id)` |
| `READY_FOR_PICKUP` | `DELIVERED` | Pickup order | `mark_customer_order_delivered(order_id)` |
| `OUT_FOR_DELIVERY` | `DELIVERED` | Delivery order | `mark_customer_order_delivered(order_id)` |

## Grants And Security

The migration should grant execute only to authenticated users:

```sql
grant execute on function public.cancel_customer_order(text) to authenticated;
grant execute on function public.confirm_customer_order(text, bigint) to authenticated;
grant execute on function public.reject_customer_order(text) to authenticated;
grant execute on function public.mark_customer_order_ready_for_pickup(text) to authenticated;
grant execute on function public.mark_customer_order_out_for_delivery(text) to authenticated;
grant execute on function public.mark_customer_order_delivered(text) to authenticated;
```

Each function should include:

```sql
security definer
set search_path = public
```

Security requirements:

- Use `auth.uid()` inside the function to identify the caller.
- Look up the caller profile inside the function.
- Require the expected `profiles.account_type`.
- Require matching ownership fields.
- Update by primary key and validated current state.
- Return or raise; never silently succeed after updating zero rows.
- Do not include secrets, credentials, JWTs, or service-role keys.

## Idempotency Strategy

The implementation migration should be repeat-safe where practical:

- Use `alter table public.orders enable row level security;`
- Use `drop policy if exists ... on public.orders;`
- Use `create policy ...` after dropping the previous policy.
- Use `create or replace function ...`.
- Use `grant execute on function ... to authenticated;`.
- Add SQL comments explaining that broad B2C direct updates were removed because lifecycle writes are now handled by validated RPC functions.

Design snippet only:

```sql
-- Broad direct B2C update policies are intentionally removed.
-- Customer cancellation and pharmacy lifecycle transitions must use validated RPC functions.
```

## B2B Preservation

This task should not change `PHARMACY_WAREHOUSE` or warehouse B2B policies.

Policies intentionally not changed in this design:

- `warehouse_view_b2b`
- `warehouse_create_b2b`
- `warehouse_update_b2b`

Reason:

- The P0 gaps being fixed are specific to `CUSTOMER_PHARMACY` creation, customer cancellation, and pharmacy-side B2C lifecycle updates.
- B2B behavior must be preserved until it is separately audited and approved for change.
- Any warehouse/account-type hardening for B2B should be handled only if a later approved task expands the scope.

## Profiles Trust Dependency

This design assumes that `profiles` fields used by RLS and RPC validation are trustworthy:

- `profiles.account_type`
- `profiles.pharmacy_id`
- `profiles.warehouse_id`

Task 7 must verify before closure that users cannot spoof or update these fields.

If users can update `account_type`, `pharmacy_id`, or `warehouse_id`, this design is not sufficient and a profiles hardening subtask becomes a P0 blocker.

## Stop And Approval Gate

Task 3 must not start until this design is reviewed and explicitly approved.

Live verification is still required before claiming production Supabase policies are fixed.

This design does not create:

- `database/migrations/20260429_harden_b2c_order_rls.sql`

This design does not modify:

- `SupabasePharmaRepository.kt`
- `PharmaRepository.kt`
- `Order.kt`
- `Request.kt`
- UI files
- Navigation files
- Existing migrations

## Task 2 Decision

RPC is required: yes.

Task 3 can proceed after approval: yes, as the migration implementation task.

Live verification required before production closure: yes.
