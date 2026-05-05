# Tasks: Phase 4.5 - Supabase RLS + Real Data Security Verification

**Source Spec**: `.specify/memory/phase-4.5-public-user-rls-real-data-verification-spec.md`  
**Phase Type**: Verification only  
**Status**: Draft  
**Dependency Order**: `1 -> 2 -> 3/4/5/6 -> 7/8/9 -> 10`

## Global Guardrails

- Do not implement product features.
- Do not refactor app code.
- Do not change schema or migrations.
- Do not change backend, domain, model, or repository code.
- Do not change UI or navigation.
- Do not commit service-role keys, user passwords, refresh tokens, access tokens, or real test credentials.
- If a P0 security gap is found, stop verification work for that path, document the gap, and propose Phase 4.5.1. Do not fix it automatically.
- Use redacted evidence for all real Supabase data.
- Treat existing dirty backend files as pre-existing unless this phase explicitly modifies them later with approval.

## Required Outputs

- `.specify/verification/phase-4.5-rls-audit.md`
- `.specify/verification/phase-4.5-test-matrix.md`
- `.specify/verification/phase-4.5-test-cases.md`
- `.specify/verification/phase-4.5-repository-alignment.md`
- `.specify/verification/phase-4.5-qa-checklist.md`
- `.specify/verification/phase-4.5-sql-checks.sql`
- `.specify/verification/phase-4.5-final-report.md`
- `.specify/verification/phase-4.5-risks.md`

## Task 1: Audit Current RLS Policies

**Depends on**: Phase 4.5 spec only  
**Output**: `.specify/verification/phase-4.5-rls-audit.md`

### Objective

Audit current Supabase Row Level Security policies for:

- `orders`
- `requests`
- `profiles`

### Steps

1. Collect current table RLS state for `orders`, `requests`, and `profiles`.
2. Collect all policies for those tables.
3. For each table, document:
   - `SELECT` policies
   - `INSERT` policies
   - `UPDATE` policies
   - `DELETE` policies, if any
   - Whether RLS is enabled
   - Whether policies rely on `auth.uid()`
   - Whether policies rely on profile role/tenant fields
4. For `orders`, specifically identify whether policies distinguish:
   - `CUSTOMER_PHARMACY`
   - `PHARMACY_WAREHOUSE`
   - `customer_id`
   - `pharmacy_id`
   - `warehouse_id`
   - `status`
5. For `requests`, verify whether public users can access B2B request data.
6. For `profiles`, verify whether role and tenant identifiers can be spoofed or broadly read.
7. Mark each policy as:
   - Safe
   - Missing
   - Risky
   - Ambiguous
   - Requires runtime verification
8. Add recommendations without implementing fixes.

### Stop Condition

If an obvious P0 cross-user or cross-role leak is found, document:

- Table
- Policy name
- Operation
- Actor
- Leaked data class
- Why it is P0
- Proposed Phase 4.5.1 fix area

Then mark Phase 4.5.1 required.

### Acceptance Criteria

- RLS status is documented for all three tables.
- All available policies are listed by operation.
- Missing, broad, risky, and ambiguous policies are called out.
- No schema or policy changes are made.

## Task 2: Prepare Test Actors And Data Matrix

**Depends on**: Task 1  
**Output**: `.specify/verification/phase-4.5-test-matrix.md`

### Objective

Define the test actors, profile rows, tenant identifiers, and controlled records needed to prove RLS isolation.

### Required Actors

- `public_user_a`
- `public_user_b`
- `pharmacy_a`
- `pharmacy_b`
- `warehouse_a`
- `warehouse_b`, if available
- `admin_a`, documentation only

### Required Profile Fields

For every actor, document redacted values for:

- Profile id
- Auth user id
- `account_type`
- `pharmacy_id`
- `warehouse_id`
- Any tenant or organization identifier used by RLS

### Required Test Records

Define controlled records for:

- `CUSTOMER_PHARMACY` order for `public_user_a` and `pharmacy_a`
- `CUSTOMER_PHARMACY` order for `public_user_b` and `pharmacy_b`
- One `PENDING` B2C order
- One non-`PENDING` B2C order
- `PHARMACY_WAREHOUSE` order for `pharmacy_a` and `warehouse_a`
- `PHARMACY_WAREHOUSE` order for another tenant, if feasible
- Supporting `requests` records for B2B verification

### Matrix Requirements

Create an access matrix by:

- Actor
- Table
- Operation
- Record type
- Ownership relation
- Expected result
- Evidence field to capture

### Secrets Rule

Do not store real passwords, service-role keys, API keys, session JWTs, refresh tokens, or unredacted production identifiers in the repo.

### Acceptance Criteria

- All required actors are defined or explicitly marked unavailable.
- All required test records are defined or explicitly marked as needing setup.
- The matrix is complete enough to execute Tasks 3, 4, and 5.

## Task 3: Define PUBLIC_USER Isolation Tests

**Depends on**: Task 2  
**Output**: Entries in `.specify/verification/phase-4.5-test-cases.md`

### Objective

Define executable verification tests for public-user B2C isolation.

### Test Cases

1. `PU-001`: `PUBLIC_USER` can create own `CUSTOMER_PHARMACY` order.
2. `PU-002`: `PUBLIC_USER` can view own B2C orders.
3. `PU-003`: `PUBLIC_USER` cannot view another customer's B2C orders.
4. `PU-004`: `PUBLIC_USER` cannot view `PHARMACY_WAREHOUSE` orders.
5. `PU-005`: `PUBLIC_USER` can open own order detail by `orderId`.
6. `PU-006`: `PUBLIC_USER` can cancel own `PENDING` B2C order.
7. `PU-007`: `PUBLIC_USER` cannot cancel another customer's order.
8. `PU-008`: `PUBLIC_USER` cannot cancel non-`PENDING` order.
9. `PU-009`: `PUBLIC_USER` cannot confirm order.
10. `PU-010`: `PUBLIC_USER` cannot reject order.
11. `PU-011`: `PUBLIC_USER` cannot mark order ready for pickup.
12. `PU-012`: `PUBLIC_USER` cannot mark order out for delivery.
13. `PU-013`: `PUBLIC_USER` cannot mark order delivered.

### Each Test Must Include

- Actor
- Precondition
- Operation
- Expected Supabase result
- Expected app/repository result, if applicable
- Evidence to capture
- P0/P1/P2 severity if failed

### Acceptance Criteria

- Every listed public-user capability and denial is represented.
- Cross-user and cross-flow denial tests are explicit.
- No test requires committing credentials.

## Task 4: Define PHARMACY B2C Access Tests

**Depends on**: Task 2  
**Output**: Entries in `.specify/verification/phase-4.5-test-cases.md`

### Objective

Define executable verification tests for pharmacy access to its own B2C rows and denial for other pharmacies.

### Test Cases

1. `PH-001`: `pharmacy_a` can view B2C orders for its own `pharmacy_id`.
2. `PH-002`: `pharmacy_a` cannot view `pharmacy_b` B2C orders.
3. `PH-003`: `pharmacy_a` can confirm own B2C order when transition rules allow.
4. `PH-004`: `pharmacy_a` can reject own B2C order when transition rules allow.
5. `PH-005`: `pharmacy_a` can mark own pickup B2C order ready when transition rules allow.
6. `PH-006`: `pharmacy_a` can mark own delivery B2C order out for delivery when transition rules allow.
7. `PH-007`: `pharmacy_a` can mark own B2C order delivered when transition rules allow.
8. `PH-008`: `pharmacy_b` cannot manage `pharmacy_a` B2C orders.
9. `PH-009`: Pharmacy cannot create B2C order on behalf of a customer.
10. `PH-010`: Pharmacy cannot cancel using the customer cancel path.
11. `PH-011`: Existing B2B `PHARMACY_WAREHOUSE` behavior remains available according to current rules.

### Each Test Must Include

- Actor
- Order type
- Ownership relation
- Starting status
- Operation
- Expected result
- Evidence to capture
- P0/P1/P2 severity if failed

### Acceptance Criteria

- Own-pharmacy access and cross-pharmacy denial are both covered.
- Seller lifecycle transitions are covered.
- B2B continuity is documented without adding new UI.

## Task 5: Define WAREHOUSE Isolation Tests

**Depends on**: Task 2  
**Output**: Entries in `.specify/verification/phase-4.5-test-cases.md`

### Objective

Define executable verification tests proving warehouses cannot access B2C rows.

### Test Cases

1. `WH-001`: Warehouse can view its own `PHARMACY_WAREHOUSE` orders.
2. `WH-002`: Warehouse can manage its own `PHARMACY_WAREHOUSE` orders according to current rules.
3. `WH-003`: Warehouse cannot view `CUSTOMER_PHARMACY` orders.
4. `WH-004`: Warehouse cannot update `CUSTOMER_PHARMACY` orders.
5. `WH-005`: Warehouse cannot access the B2C flow.
6. `WH-006`: Warehouse cannot act as a pharmacy in B2C lifecycle operations.
7. `WH-007`: Warehouse cannot act as a customer in B2C cancel operations.

### Each Test Must Include

- Actor
- Record type
- Operation
- Expected result
- Evidence to capture
- P0/P1/P2 severity if failed

### Acceptance Criteria

- B2B allowed behavior is distinguished from B2C denied behavior.
- Any B2C visibility to warehouses is marked P0.

## Task 6: Repository And UseCase Alignment Check

**Depends on**: Task 2  
**Output**: `.specify/verification/phase-4.5-repository-alignment.md`

### Objective

Review app-layer checks and confirm they align with RLS expectations.

### Review Targets

- `GetMyOrdersUseCase`
- `CreateCustomerOrderUseCase`
- `CancelCustomerOrderUseCase`
- Pharmacy-side B2C lifecycle use cases, if present
- `SupabasePharmaRepository` B2C validation
- Public-user B2C ViewModels
- Public-user B2C navigation path

### Verification Points

1. Role validation exists before sensitive writes.
2. Ownership validation exists before sensitive writes.
3. State transition validation exists before lifecycle writes.
4. B2C list/detail uses `GetMyOrdersUseCase`.
5. Cancel uses `CancelCustomerOrderUseCase`.
6. UI/ViewModels do not bypass use cases.
7. Repository writes do not fake success when Supabase rejects.
8. `PUBLIC_USER` My Orders does not use generic Orders or `observeOrders`.
9. Permission errors are propagated as failure states, not success states.

### Constraints

- Read code only.
- Do not modify repository/domain/model/UI/navigation.
- If a blocker is found, document it as a risk or Phase 4.5.1 proposal.

### Acceptance Criteria

- Alignment table exists for every review target.
- Any mismatch is documented with severity.
- No code changes are made.

## Task 7: App Smoke Test Plan With Real Supabase

**Depends on**: Tasks 3, 4, 5, and 6  
**Output**: `.specify/verification/phase-4.5-qa-checklist.md`

### Objective

Define manual app smoke test steps for the real Supabase public-user flow.

### Smoke Steps

1. Log in as `public_user_a`.
2. Create a `CUSTOMER_PHARMACY` order.
3. Verify database row shape:
   - `order_type = CUSTOMER_PHARMACY`
   - `customer_id = public_user_a`
   - `pharmacy_id = pharmacy_a`
   - `status = PENDING`
   - `total_price_cents = null`
4. Open My Orders.
5. Verify only `public_user_a` B2C orders appear.
6. Open order detail.
7. Verify safe details only:
   - medicine name
   - pharmacy name
   - quantity
   - status
   - fulfillment type
   - delivery address/phone only for delivery
   - notes only if present
   - price only if available
8. Cancel pending order through confirmation dialog.
9. Verify Supabase status becomes `CANCELLED` or the app shows an honest error.
10. Verify My Orders refreshes safely.
11. Confirm public-user path did not use generic Orders route.

### Acceptance Criteria

- Smoke checklist is executable by a manual tester.
- Required evidence is listed.
- No real secrets are included.

## Task 8: Permission Denied And Error Handling Checks

**Depends on**: Tasks 3, 4, 5, and 6  
**Output**: Add entries to `.specify/verification/phase-4.5-qa-checklist.md`

### Objective

Define app and API checks for blocked operations.

### Test Areas

1. Permission denied does not crash the app.
2. App does not show fake success when Supabase rejects.
3. Error messages are user-safe.
4. Raw SQL errors are not exposed to end users.
5. Retry paths remain safe.
6. Cancellation failure keeps the user on a truthful state.
7. Unauthorized detail access does not reveal protected data.

### Acceptance Criteria

- Permission-denied checks are included in the QA checklist.
- Expected user-facing behavior is documented.
- Any raw-error exposure is marked as a risk, not fixed in this task.

## Task 9: Optional SQL Verification Script Plan

**Depends on**: Tasks 3, 4, 5, and 6  
**Output**: `.specify/verification/phase-4.5-sql-checks.sql`

### Objective

Provide a safe SQL verification helper plan when practical, without committing secrets.

### Script Requirements

- Include policy inspection queries for `orders`, `requests`, and `profiles`.
- Include RLS-enabled checks for the target tables.
- Include comments showing where authenticated actor context must be supplied.
- Include placeholders for test IDs, not real IDs.
- Include expected-result comments for public-user, pharmacy, and warehouse checks.
- Do not include service-role keys.
- Do not include JWTs.
- Do not include real passwords.

### If SQL Script Is Not Practical

Document manual alternatives using:

- Supabase dashboard policy view
- Supabase SQL editor with redacted evidence
- Supabase API calls from a local environment with credentials outside the repo
- App smoke testing with controlled accounts

### Acceptance Criteria

- Either a safe SQL helper exists or the manual alternative is documented.
- No secrets or real credentials are committed.

## Task 10: Final Report Task

**Depends on**: Tasks 7, 8, and 9  
**Output**: `.specify/verification/phase-4.5-final-report.md`

### Objective

Define and complete the final verification report after all prior tasks have evidence.

### Required Report Structure

1. QA verdict: `PASS` or `FAIL`
2. Files/docs created
3. RLS audit summary
4. Test matrix summary
5. `PUBLIC_USER` result
6. `PHARMACY` result
7. `WAREHOUSE` result
8. `ADMIN` current behavior note
9. App smoke result
10. Permission-denied/error-handling result
11. Compile result
12. Risks
13. Scope violations: yes/no
14. Secrets committed: must be no
15. Closure decision:
    - `CLOSE Phase 4.5`
    - or `OPEN Phase 4.5.1 for approved security fixes`
16. Whether Phase 4.5.1 is required

### Required Compile Check

Run and record:

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

### Acceptance Criteria

- Final report references all required verification docs.
- Final report includes a clear closure decision.
- Any P0 gap results in `OPEN Phase 4.5.1`, not an automatic fix.
- Compile result is recorded.

## Final Acceptance Criteria

- All required verification docs are created.
- No app code changes are made unless explicitly approved later.
- No schema changes are made.
- No backend/domain/model/repository changes are made.
- No UI/navigation changes are made.
- P0 gaps are documented, not fixed.
- No service-role key or test credential is saved in the repo.
- Final report recommends either:
  - `CLOSE Phase 4.5`
  - `OPEN Phase 4.5.1 for approved security fixes`
