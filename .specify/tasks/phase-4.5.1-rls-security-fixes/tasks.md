# Tasks: Phase 4.5.1 - Approved RLS Security Fixes for PUBLIC_USER B2C Orders

**Source Spec**: `.specify/memory/phase-4.5.1-rls-security-fixes-spec.md`  
**Phase Type**: Security fixes, gated implementation  
**Status**: Draft  
**Dependency Order**: `1 -> 2 -> 3 -> 4 -> 5 if required -> 6 -> 7 -> 8 -> 9 -> 10`

## Global Guardrails

- Do not begin implementation while generating this task plan.
- Do not modify SQL, migrations, app code, repository code, domain, model, UI, or navigation in this task-planning step.
- Do not store service-role keys, JWTs, API keys, passwords, refresh tokens, access tokens, or real credentials in the repo.
- Any task that executes SQL or creates RPCs must happen only after baseline audit and explicit approval.
- Any repository integration must happen only after RPC compatibility review proves it is required.
- Preserve `Order.kt` and `Request.kt`.
- Preserve UI and navigation.
- Preserve B2B behavior.
- Do not introduce `rejectionReason`, `statusReason`, payment, tracking, inventory, PHARMACY UI, WAREHOUSE UI, or ADMIN UI.
- If a new P0 is found, stop the current path, document it, and require approval before proceeding.

## Required Outputs

- `.specify/verification/phase-4.5.1-policy-baseline.md`
- `.specify/verification/phase-4.5.1-rpc-repository-compatibility.md`
- `.specify/verification/phase-4.5.1-profiles-trust.md`
- `.specify/verification/phase-4.5.1-sql-test-plan.md`
- `.specify/verification/phase-4.5.1-final-report.md`
- Implementation task output, only after approval:
  - `database/migrations/20260429_harden_b2c_order_rls.sql`

## Approval Gates

### Gate A: Before SQL Migration Implementation

Required before Task 3:

- Task 1 baseline completed.
- Task 2 SQL design completed.
- Live/local policy contradiction, if any, reviewed.
- No secrets committed.
- Explicit approval to create the migration file.

### Gate B: Before Repository Integration

Required before Task 5:

- Task 4 proves current repository methods would break or become unauthorized after RLS hardening.
- Required RPC names and payloads are stable.
- Integration scope is limited to `SupabasePharmaRepository` B2C lifecycle methods.
- Explicit approval to modify repository code.

### Gate C: Before Closure

Required before Task 10 can close:

- SQL/RLS negative tests are documented.
- Profiles trust is verified or documented as blocker.
- App smoke test passes or failure is documented.
- Compile result is recorded.
- Remaining risks are classified.

## Task 1: Confirm Live/Local Policy Baseline

**Depends on**: Phase 4.5.1 spec  
**Output**: `.specify/verification/phase-4.5.1-policy-baseline.md`

### Objective

Confirm the current policy baseline before designing or applying any security migration.

### Inputs

- `database/migrations/20250425_extend_orders_for_b2c.sql`
- `.specify/verification/phase-4.5-rls-audit.md`
- Supabase live policies, if available without saving secrets

### Steps

1. Read the local B2C migration and list all local `orders` policies.
2. Read the Phase 4.5 RLS audit and carry forward P0 findings.
3. If available, collect live Supabase policies for:
   - `orders`
   - `requests`
   - `profiles`
4. Document:
   - Local policies
   - Live policies, if available
   - Differences between local and live
   - Whether local migration has drifted from live database
   - Whether `profiles` RLS exists locally
   - Whether `profiles` RLS exists live, if available
5. Classify evidence:
   - Confirmed local
   - Confirmed live
   - Missing locally
   - Missing live
   - Unknown

### Stop Conditions

- Stop if live evidence contradicts local audit in a way that changes the fix design.
- Stop if live policies already fix the local P0 gaps and document the contradiction before proceeding.
- Stop if live access requires saving secrets in the repo.

### Acceptance Criteria

- Baseline document exists.
- Local/live policy drift is documented.
- P0 findings are confirmed, updated, or explicitly marked as local-only.
- No SQL or app code changes are made.

## Task 2: Design Final SQL Migration File

**Depends on**: Task 1  
**Output**: SQL design section in verification/report docs; no migration file yet unless Task 3 is approved

### Objective

Produce the exact SQL migration design for hardening B2C order RLS and RPC writes.

### Proposed Migration Name

`database/migrations/20260429_harden_b2c_order_rls.sql`

### Required Design Contents

1. Ensure `orders` RLS remains enabled.
2. Drop risky policies:
   - `customer_create_order`
   - `customer_cancel_pending`
   - `pharmacy_manage_b2c`
3. Replace `customer_create_order` with a policy that requires:
   - `auth.uid() = customer_id`
   - `order_type = 'CUSTOMER_PHARMACY'`
   - `status = 'PENDING'`
   - `warehouse_id IS NULL`
   - `request_id IS NULL`
   - `pharmacy_id IS NOT NULL`
   - `total_price_cents IS NULL`
   - trusted `profiles.account_type = 'PUBLIC_USER'`
4. Ensure there is no broad direct customer `UPDATE` for B2C.
5. Ensure there is no broad direct pharmacy `UPDATE` for B2C.
6. Add RPC function designs:
   - `cancel_customer_order(order_id)`
   - `confirm_customer_order(order_id, total_price_cents)`
   - `reject_customer_order(order_id)`
   - `mark_customer_order_ready_for_pickup(order_id)`
   - `mark_customer_order_out_for_delivery(order_id)`
   - `mark_customer_order_delivered(order_id)`
7. Add `GRANT EXECUTE` design for `authenticated`.
8. Require safe `search_path` on functions.
9. Add comments explaining why direct B2C update policies are removed.
10. Include idempotency strategy:
    - `DROP POLICY IF EXISTS`
    - `CREATE OR REPLACE FUNCTION`
    - safe grants

### Stop Conditions

- Stop if the design requires changing app/domain models.
- Stop if the design requires introducing rejection reasons.
- Stop if the design cannot preserve B2B behavior.

### Acceptance Criteria

- SQL design answers which policies are dropped/replaced.
- SQL design answers why RPC is required.
- SQL design does not create a migration file yet.
- Approval Gate A requirements are clear.

## Task 3: Implement SQL Migration

**Depends on**: Task 2 and Approval Gate A  
**Output**: `database/migrations/20260429_harden_b2c_order_rls.sql`

### Objective

Create the approved SQL migration for B2C order RLS hardening.

### Implementation Requirements

1. Create the migration file.
2. Implement idempotently where practical.
3. Drop risky policies:
   - `customer_create_order`
   - `customer_cancel_pending`
   - `pharmacy_manage_b2c`
4. Add hardened `customer_create_order` replacement policy.
5. Do not add broad direct customer B2C `UPDATE`.
6. Do not add broad direct pharmacy B2C `UPDATE`.
7. Add RPC functions:
   - `cancel_customer_order(order_id)`
   - `confirm_customer_order(order_id, total_price_cents)`
   - `reject_customer_order(order_id)`
   - `mark_customer_order_ready_for_pickup(order_id)`
   - `mark_customer_order_out_for_delivery(order_id)`
   - `mark_customer_order_delivered(order_id)`
8. Grant execute to `authenticated`.
9. Use safe `search_path` for functions.
10. Add SQL comments explaining:
    - Why broad B2C direct updates are removed
    - Which transition each RPC owns
    - Which actor role each RPC permits

### Forbidden

- Do not change app code.
- Do not change repository code.
- Do not change `Order.kt` or `Request.kt`.
- Do not introduce `rejectionReason` or `statusReason`.
- Do not include secrets or real test data.

### Stop Conditions

- Stop if migration cannot be written without changing domain/schema beyond RLS/RPC.
- Stop if function design cannot preserve B2B behavior.
- Stop if profile trust is proven unsafe and requires separate profiles hardening first.

### Acceptance Criteria

- Migration file exists.
- Risky policies are dropped/replaced.
- RPC functions are present.
- No broad direct B2C update policy remains in the migration design.
- No app code is changed in this task.

## Task 4: Verify RPC Compatibility With Current Repositories

**Depends on**: Task 3  
**Output**: `.specify/verification/phase-4.5.1-rpc-repository-compatibility.md`

### Objective

Determine whether current repository B2C lifecycle methods must be updated to call RPCs after RLS hardening.

### Review Targets

- `SupabasePharmaRepository.cancelCustomerOrder`
- `SupabasePharmaRepository.confirmOrder`
- `SupabasePharmaRepository.rejectOrder`
- `SupabasePharmaRepository.markOrderReadyForPickup`
- `SupabasePharmaRepository.markOrderOutForDelivery`
- `SupabasePharmaRepository.markOrderDelivered`

### Steps

1. Inspect each method.
2. Determine whether it uses:
   - Direct `orders` table update
   - RPC call
   - Other path
3. Determine whether it handles B2C and B2B differently.
4. Determine whether direct update would fail after Task 3.
5. Document required repository integration, if any.
6. Preserve existing method signatures in the recommendation.

### Stop Conditions

- Stop before modifying repository code.
- Stop if repository integration would require domain/model changes.
- Stop if B2B behavior cannot be preserved with the planned integration.

### Acceptance Criteria

- Compatibility document exists.
- Every lifecycle method is classified.
- Task 5 is marked required or not required.
- No repository code is modified.

## Task 5: Integrate Repository With RPCs If Required

**Depends on**: Task 4 and Approval Gate B  
**Output**: Minimal repository patch, only if required

### Objective

Update repository B2C lifecycle paths to call the new RPCs only if Task 4 proves direct table updates would fail after RLS hardening.

### Allowed Changes

- Minimal changes to `SupabasePharmaRepository` B2C lifecycle methods only.
- Preserve public method signatures.
- Preserve use case contracts.
- Preserve B2B behavior.

### Required Mapping

- `cancelCustomerOrder` for B2C -> `cancel_customer_order`
- `confirmOrder` for B2C -> `confirm_customer_order`
- `rejectOrder` for B2C -> `reject_customer_order`
- `markOrderReadyForPickup` for B2C -> `mark_customer_order_ready_for_pickup`
- `markOrderOutForDelivery` for B2C -> `mark_customer_order_out_for_delivery`
- `markOrderDelivered` for B2C -> `mark_customer_order_delivered`

### Forbidden

- Do not modify UI.
- Do not modify navigation.
- Do not modify domain/model.
- Do not change repository interfaces unless explicitly approved.
- Do not change B2B behavior.
- Do not introduce fake success.
- Do not use `!!`.

### Stop Conditions

- Stop if method signatures must change.
- Stop if B2B behavior would be affected.
- Stop if RPC payload/return shape is not stable.

### Acceptance Criteria

- Repository uses RPC for B2C lifecycle paths if required.
- B2B paths remain as before.
- Errors from RPC are propagated as failures.
- No fake success.

## Task 6: InMemory Repository Compatibility

**Depends on**: Task 5 if required; otherwise Task 4  
**Output**: Compatibility note or minimal compile-only patch if required

### Objective

Ensure in-memory repository behavior remains compile-compatible.

### Steps

1. Check whether repository interface signatures changed.
2. Prefer no interface changes.
3. If no interface changes occurred, document no action required.
4. If compile requires changes, make unsupported behavior explicit.
5. Do not fake success for unsupported security-sensitive paths.

### Stop Conditions

- Stop if a change would alter production behavior.
- Stop if fake success is needed to compile.

### Acceptance Criteria

- In-memory compatibility is documented.
- Any compile-only change is minimal and explicit.
- No fake success is introduced.

## Task 7: Profiles Trust Verification

**Depends on**: Task 6  
**Output**: `.specify/verification/phase-4.5.1-profiles-trust.md`

### Objective

Verify whether `profiles` can be trusted for `account_type`, `pharmacy_id`, and `warehouse_id`.

### Steps

1. Check local migrations/docs for `profiles` RLS.
2. Check live database policies if available without saving secrets.
3. Verify users cannot update:
   - `account_type`
   - `pharmacy_id`
   - `warehouse_id`
4. Verify profile role/tenant fields are assigned by trusted code.
5. Document whether `orders` RLS can safely depend on `profiles`.

### Stop Conditions

- If live verification is unavailable, document runtime blocker.
- If users can mutate role or tenant fields, mark P0 and open a profiles hardening subtask before closure.
- Do not fix profiles policies in this task unless separately approved.

### Acceptance Criteria

- Profiles trust document exists.
- Trust status is one of:
  - Verified trusted
  - Runtime verification required
  - P0 hardening required
- No secrets are saved.

## Task 8: SQL/RLS Negative Test Plan

**Depends on**: Task 7  
**Output**: `.specify/verification/phase-4.5.1-sql-test-plan.md`

### Objective

Create a SQL/API negative test plan for the hardened policies and RPCs.

### Required Tests

1. `PUBLIC_USER` creates own B2C order successfully.
2. `PHARMACY` creates B2C order: denied.
3. `WAREHOUSE` creates B2C order: denied.
4. `PUBLIC_USER` direct-updates B2C order: denied.
5. `PUBLIC_USER` cancel RPC succeeds only for own `PENDING`.
6. `PUBLIC_USER` cancel RPC denied for non-own order.
7. `PUBLIC_USER` cancel RPC denied for non-`PENDING`.
8. `PHARMACY` lifecycle RPCs succeed only for own allowed transitions.
9. Invalid pharmacy lifecycle transitions are denied.
10. `PHARMACY` lifecycle RPCs denied for another pharmacy's order.
11. `WAREHOUSE` cannot access `CUSTOMER_PHARMACY`.
12. `WAREHOUSE` cannot run B2C RPCs.

### Evidence Requirements

Each test must define:

- Actor
- Setup row
- Operation
- Expected result
- Evidence to capture
- Severity if failed

### Acceptance Criteria

- Test plan covers all P0 gaps.
- Tests avoid storing credentials.
- Tests distinguish direct table access from RPC access.

## Task 9: App Smoke Test

**Depends on**: Task 8  
**Output**: Smoke result in `.specify/verification/phase-4.5.1-final-report.md`

### Objective

Verify the public-user app flow still works after RLS/RPC hardening.

### Smoke Steps

1. Log in as `PUBLIC_USER`.
2. Create `CUSTOMER_PHARMACY` order.
3. Open My Orders.
4. Open detail.
5. Cancel pending order.
6. Verify no fake success when RLS denies action.
7. Verify My Orders refreshes safely.
8. Run compile:

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

### Stop Conditions

- Stop if app reports success while Supabase/RPC denies write.
- Stop if app crashes on permission denied.
- Stop if public-user route falls back to generic Orders.

### Acceptance Criteria

- Smoke test result is documented.
- Compile result is documented.
- Failures are classified and not hidden.

## Task 10: Final Security Report

**Depends on**: Task 9  
**Output**: `.specify/verification/phase-4.5.1-final-report.md`

### Objective

Produce the final report for Phase 4.5.1.

### Required Report Sections

1. QA verdict: `PASS` or `FAIL`
2. Files/docs created
3. Policies dropped/replaced
4. RPCs added
5. Repository integration status
6. In-memory compatibility status
7. Profiles trust status
8. SQL/RLS test result
9. App smoke result
10. Compile result
11. Remaining risks
12. Secrets committed: must be no
13. Scope violations: yes/no
14. Closure decision:
    - `CLOSE Phase 4.5.1`
    - or `FIX REQUIRED`

### Acceptance Criteria

- Final report references all supporting verification docs.
- Final report clearly states whether Phase 4.5 can resume/close.
- Any unresolved P0 means `FIX REQUIRED`.
- No secrets are included.

## Final Acceptance Criteria

- Tasks file created.
- No implementation in this planning step.
- No app/backend/schema changes in this planning step.
- Clear stop conditions are included.
- Clear approval gates are included before SQL migration and repository integration.
- Future implementation must preserve no-secrets rule.
