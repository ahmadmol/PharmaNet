# Phase 4.5.1 Policy Baseline

**Task**: Task 1 - Confirm Live/Local Policy Baseline  
**Phase**: Phase 4.5.1 RLS Security Fixes  
**Date**: 2026-04-29  
**Scope**: Read-only baseline confirmation for local and, if available, live RLS policies.  
**Implementation changes**: None.

## Evidence Sources

Local evidence reviewed:

- `database/migrations/20250425_extend_orders_for_b2c.sql`
- `.specify/verification/phase-4.5-rls-audit.md`
- repo search for `CREATE POLICY`, `ENABLE ROW LEVEL SECURITY`, `orders`, `requests`, and `profiles`

Live evidence:

- Supabase MCP resources were checked and none were exposed in this session.
- No live Supabase Dashboard/SQL output was available without supplying credentials.
- No secrets, JWTs, API keys, service-role keys, passwords, or tokens were read from or saved to the repo.

## Local Policy Baseline

Local concrete RLS policy evidence exists only for `orders` in:

`database/migrations/20250425_extend_orders_for_b2c.sql`

### Local RLS Enablement

| Table | Local RLS evidence | Classification |
|---|---|---|
| `orders` | `ALTER TABLE orders ENABLE ROW LEVEL SECURITY;` | Confirmed local |
| `requests` | No local `ENABLE ROW LEVEL SECURITY` found | Missing locally |
| `profiles` | No local `ENABLE ROW LEVEL SECURITY` found | Missing locally |

### Local `orders` Policies

| Policy | Operation | Local condition summary | Classification |
|---|---|---|---|
| `customer_view_own_orders` | `SELECT` | `auth.uid() = customer_id` and `order_type = 'CUSTOMER_PHARMACY'` | Confirmed local |
| `customer_create_order` | `INSERT` | `auth.uid() = customer_id`, B2C row shape, `status = 'PENDING'`; no account-type check | Confirmed local |
| `customer_cancel_pending` | `UPDATE` | Old row: own `PENDING` B2C; new row: own B2C only; no exact `CANCELLED` check | Confirmed local |
| `pharmacy_view_orders` | `SELECT` | B2C and B2B rows scoped by `profiles.pharmacy_id`; no account-type check | Confirmed local |
| `pharmacy_manage_b2c` | `UPDATE` | B2C rows scoped by `profiles.pharmacy_id`; no lifecycle constraints | Confirmed local |
| `warehouse_view_b2b` | `SELECT` | B2B rows scoped by `profiles.warehouse_id`; no account-type check | Confirmed local |
| `warehouse_create_b2b` | `INSERT` | B2B rows scoped by `profiles.warehouse_id`; no account-type check | Confirmed local |
| `warehouse_update_b2b` | `UPDATE` | B2B rows scoped by `profiles.warehouse_id`; no account-type check | Confirmed local |

### Local `requests` Policies

No local `CREATE POLICY` statements for `requests` were found.

Classification: Missing locally.

### Local `profiles` Policies

No local `CREATE POLICY` statements for `profiles` were found.

The prior audit notes that `auth_debug_guide.md` references `users_insert_own_profile`, but the actual policy SQL is not present in local evidence.

Classification: Missing locally / ambiguous documentation-only reference.

## P0 Findings Carried Forward

The following P0 findings from `.specify/verification/phase-4.5-rls-audit.md` remain confirmed by local evidence.

### P0-001: `customer_create_order` lacks `PUBLIC_USER` account-type check

Local status: Confirmed local.

Finding:

- Policy targets `TO authenticated`.
- Policy verifies `auth.uid() = customer_id`.
- Policy does not verify `profiles.account_type = 'PUBLIC_USER'`.

Impact:

- Non-`PUBLIC_USER` authenticated roles may be able to create `CUSTOMER_PHARMACY` orders for themselves.

### P0-002: `customer_cancel_pending` is too broad

Local status: Confirmed local.

Finding:

- `USING` limits the old row to own `PENDING` B2C order.
- `WITH CHECK` only requires the new row to remain own B2C.
- It does not require the new status to be `CANCELLED`.
- It does not prove sensitive fields remain unchanged.

Impact:

- A public user may be able to mutate own pending B2C orders beyond cancellation.

### P0-003: `pharmacy_manage_b2c` lacks role and transition constraints

Local status: Confirmed local.

Finding:

- Policy scopes by `profiles.pharmacy_id`.
- Policy does not verify `profiles.account_type = 'PHARMACY'`.
- Policy does not encode allowed lifecycle transitions.

Impact:

- Pharmacy-side B2C updates are too broad at the RLS layer.
- Safety depends on repository/use case behavior and profile trust, not RLS alone.

## Live Policy Baseline

Live policy access was not available in this session.

| Table | Live policies available? | Live RLS status | Classification |
|---|---:|---|---|
| `orders` | No | Unknown | Unknown |
| `requests` | No | Unknown | Unknown |
| `profiles` | No | Unknown | Unknown |

Reason:

- No Supabase MCP resources were exposed.
- Live Supabase access would require credentials or dashboard access outside this session.
- This task forbids saving secrets in the repo, so no live credential setup was attempted.

## Local / Live Drift

| Area | Drift status | Notes |
|---|---|---|
| `orders` policies | Unknown because live access unavailable | Local policy baseline is confirmed; live may differ |
| `requests` policies | Unknown because live access unavailable | No local policies found |
| `profiles` policies | Unknown because live access unavailable | No local policies found; docs mention `users_insert_own_profile` only |

Live/local drift result: unknown.

## Profiles RLS Status

| Question | Result | Classification |
|---|---|---|
| Does `profiles` RLS exist locally? | No local SQL evidence found | Missing locally |
| Does `profiles` RLS exist live? | Not available in this session | Unknown |
| Can users update `account_type` locally? | Cannot determine from local RLS evidence | Unknown |
| Can users update `pharmacy_id` locally? | Cannot determine from local RLS evidence | Unknown |
| Can users update `warehouse_id` locally? | Cannot determine from local RLS evidence | Unknown |

Baseline decision:

- `orders` policies depend on `profiles.pharmacy_id` and `profiles.warehouse_id`.
- Phase 4.5.1 must keep the profiles trust verification task.
- If live profiles policies allow users to mutate role or tenant identifiers, that becomes a P0 blocker before closure.

## Classification Summary

| Evidence item | Classification |
|---|---|
| `orders` RLS enabled locally | Confirmed local |
| `orders.customer_view_own_orders` | Confirmed local |
| `orders.customer_create_order` | Confirmed local, P0 gap confirmed locally |
| `orders.customer_cancel_pending` | Confirmed local, P0 gap confirmed locally |
| `orders.pharmacy_view_orders` | Confirmed local, role-integrity gap depends on profiles trust |
| `orders.pharmacy_manage_b2c` | Confirmed local, P0 gap confirmed locally |
| `orders.warehouse_view_b2b` | Confirmed local |
| `orders.warehouse_create_b2b` | Confirmed local |
| `orders.warehouse_update_b2b` | Confirmed local |
| `requests` local RLS/policies | Missing locally |
| `profiles` local RLS/policies | Missing locally |
| Live `orders` policies | Unknown |
| Live `requests` policies | Unknown |
| Live `profiles` policies | Unknown |

## Stop Condition Review

### Live evidence contradicts local audit?

No live evidence was available, so no contradiction was found.

Result: Not triggered.

### Live policies already fix local P0 gaps?

No live evidence was available.

Result: Unknown, not triggered.

### Live access requires saving secrets in repo?

No live access was attempted because no MCP resources were exposed and credential setup would be external. No secrets were saved.

Result: Not triggered.

## Decision

Can Task 2 proceed with local evidence?

Yes, as a SQL design task.

Rationale:

- The local baseline confirms the exact risky policies that Task 2 is meant to replace or mitigate.
- Task 2 is still design-only and does not execute SQL.
- Live policy verification remains required before closure and before claiming that live Supabase is fixed.

Need live verification first?

Not before Task 2 design. Live verification is required before final closure and should be attempted before or during implementation validation.

## Final Baseline Verdict

- P0 confirmed: yes, local evidence.
- Live status: unknown.
- Local/live drift: unknown.
- Task 2 may proceed: yes, design-only.
- Implementation may proceed without approval: no.
- No implementation changes were made.
