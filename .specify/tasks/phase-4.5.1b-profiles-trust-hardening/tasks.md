# Tasks: Phase 4.5.1B - Profiles Trust Hardening

**Source Spec**: `.specify/memory/phase-4.5.1b-profiles-trust-hardening-spec.md`  
**Phase Type**: Security hardening, gated implementation  
**Status**: Draft  
**Dependency Order**: `1 -> 2 -> approval -> 3 -> 4 -> approval -> 5 -> 6 -> 7 -> 8 -> 9 -> 10`

## Global Guardrails

- Do not implement SQL or Kotlin while generating this task plan.
- Do not modify migrations, repositories, app code, UI, navigation, domain models, or order/request models in this task-planning step.
- Do not store secrets, JWTs, service-role keys, API keys, passwords, access tokens, refresh tokens, or real credentials in the repo.
- Use live Supabase/MCP evidence only if it is available without exposing or saving secrets.
- Any SQL migration work requires explicit approval after Task 2.
- Any Kotlin repository change requires explicit approval after Task 4.
- Keep SQL changes and Kotlin changes in separate tasks.
- If live evidence contradicts local risk assumptions, stop and document the contradiction before proceeding.
- If profile trust cannot be hardened safely, stop and mark Phase 4.5.1B as `FIX REQUIRED`.

## Required Outputs

- `.specify/verification/phase-4.5.1b-profiles-baseline.md`
- `.specify/verification/phase-4.5.1b-profiles-sql-design.md`
- `.specify/verification/phase-4.5.1b-auth-bootstrap-audit.md`
- `.specify/verification/phase-4.5.1b-update-profile-safety.md`
- `.specify/verification/phase-4.5.1b-profiles-negative-tests.md`
- `.specify/verification/phase-4.5.1b-orders-trust-impact.md`
- `.specify/verification/phase-4.5.1b-final-report.md`
- Approved implementation output, only after SQL approval:
  - `database/migrations/20260430_harden_profiles_rls.sql`

## Approval Gates

### Gate A: Before Profiles SQL Migration Implementation

Required before Task 3:

- Task 1 baseline audit completed.
- Task 2 final SQL migration design completed.
- Direct insert vs trusted RPC/trigger decision documented.
- Protected-field strategy documented:
  - column grants,
  - immutability trigger,
  - or both.
- Live/local contradictions reviewed, if any.
- No secrets saved.
- Explicit approval to create `database/migrations/20260430_harden_profiles_rls.sql`.

### Gate B: Before Auth Repository Update

Required before Task 5:

- Task 4 bootstrap audit completed.
- Exact current writes to `account_type`, `is_active`, and profile upsert documented.
- Task 2 creation path decision available.
- Required repository behavior is stable.
- No UI/navigation/domain/model changes required.
- Explicit approval to modify `SupabaseAuthRepository.kt`.

### Gate C: Before Closure

Required before Task 10 can close:

- SQL implementation status documented.
- Repository implementation status documented.
- Compile result documented.
- Negative test plan created.
- Orders trust impact reviewed.
- Remaining live verification requirements classified.
- Closure decision is either `CLOSE Phase 4.5.1B` or `FIX REQUIRED`.

## Task 1: Audit Current Profiles Schema/RLS/Grants

**Depends on**: Phase 4.5.1B spec  
**Output**: `.specify/verification/phase-4.5.1b-profiles-baseline.md`

### Objective

Establish the current local and live baseline for `profiles` schema, RLS, policies, grants, triggers, and app-facing trust assumptions before designing any hardening.

### Review Targets

- `database/migrations/*.sql`
- `database/triggers/*.sql`
- local docs that mention `profiles`, auth bootstrap, RLS, policies, or grants
- `.specify/verification/phase-4.5.1-profiles-trust.md`
- Supabase MCP/live policy evidence, only if available without secrets

### Steps

1. Search local migrations and triggers for `profiles`.
2. Identify whether local SQL includes:
   - `ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY`
   - `CREATE POLICY` / `DROP POLICY` on `public.profiles`
   - `GRANT` / `REVOKE` on `public.profiles`
   - triggers or functions that write protected profile fields
3. Identify protected fields in local evidence:
   - `account_type`
   - `pharmacy_id`
   - `warehouse_id`
   - `is_active`
   - approval/status/admin-controlled fields, if present
4. Review docs for references to profile creation or policies.
5. If Supabase MCP/live access is available without secrets, inspect live `profiles` RLS/policies/grants/triggers.
6. Compare local and live evidence.
7. Classify profile trust:
   - verified trusted,
   - missing locally,
   - unknown live,
   - contradicted by live evidence,
   - or P0 hardening required.

### Stop Conditions

- Stop if live evidence contradicts local risk assumptions in a way that changes the hardening design.
- Stop if live evidence proves equivalent protections already exist; document the evidence before proceeding.
- Stop if live access requires saving or exposing secrets.
- Stop if the schema has protected fields not covered by the source spec; document them before Task 2.

### Acceptance Criteria

- Baseline document exists.
- Local profiles RLS/policy/grant/trigger evidence is documented.
- Live evidence is documented or explicitly marked unavailable.
- Protected fields are listed.
- No SQL, Kotlin, migration, repository, app, UI, navigation, domain, or model changes are made.

## Task 2: Design Final Profiles SQL Migration

**Depends on**: Task 1  
**Output**: `.specify/verification/phase-4.5.1b-profiles-sql-design.md`  
**Proposed Migration Name**: `database/migrations/20260430_harden_profiles_rls.sql`  
**Implementation**: No migration file yet

### Objective

Design the final SQL migration for hardening `profiles` so normal authenticated clients cannot mutate role, tenant, activation, approval, or admin-controlled fields.

### Required Design Contents

1. Enable RLS on `public.profiles`.
2. Decide whether to use `FORCE ROW LEVEL SECURITY`.
3. Define own-row `SELECT` policy:
   - authenticated user can read own profile.
4. Define safe own-row `UPDATE` policy:
   - authenticated user can update own row only.
   - update must be limited to safe display/contact fields through grants and/or trigger protection.
5. Define safe display/contact fields:
   - `full_name`
   - `phone_number`
   - `pharmacy_name`
   - `pharmacy_location`
   - `warehouse_name`
   - `warehouse_location`
6. Define protected fields that normal authenticated users cannot mutate:
   - `account_type`
   - `pharmacy_id`
   - `warehouse_id`
   - `is_active`
   - approval/status/admin-controlled fields
7. Define no authenticated `DELETE` policy.
8. Make a clear profile creation decision:
   - direct self-owned `PUBLIC_USER` insert,
   - or trusted `SECURITY DEFINER` RPC,
   - or trusted auth trigger,
   - or deny direct insert until server/admin creates profiles.
9. If direct insert is chosen, constrain it to:
   - `id = auth.uid()`
   - `account_type = 'PUBLIC_USER'`
   - `pharmacy_id IS NULL`
   - `warehouse_id IS NULL`
   - no client-controlled admin/approval fields
10. Choose protected-field enforcement:
    - column-level grants,
    - immutability trigger,
    - or both.
11. Define admin/service-role path requirements for assigning:
    - `account_type`
    - `pharmacy_id`
    - `warehouse_id`
    - `is_active`
    - approval/status/admin-controlled fields
12. Define idempotency strategy:
    - `DROP POLICY IF EXISTS`
    - safe `GRANT` / `REVOKE`
    - `CREATE OR REPLACE FUNCTION` if trigger/RPC is designed
13. Define verification expectations for Task 8.

### Stop Conditions

- Stop if safe hardening requires schema changes outside RLS/grants/trigger/RPC.
- Stop if the design would let normal clients mutate protected fields.
- Stop if the design cannot support the existing public-user bootstrap path without an explicit product decision.
- Stop if sensitive-role signup cannot be made safe without a trusted server/admin path.

### Acceptance Criteria

- SQL design document exists.
- Migration name is specified but migration file is not created.
- Direct insert vs trusted RPC/trigger decision is explicit.
- Protected-field strategy is explicit.
- Approval Gate A is ready.
- No SQL or Kotlin implementation is performed.

## Task 3: Implement Profiles SQL Migration

**Depends on**: Task 2 and Approval Gate A  
**Output**: `database/migrations/20260430_harden_profiles_rls.sql`

### Objective

Create the approved SQL migration that hardens `profiles` RLS, grants, and protected-field behavior.

### Implementation Requirements

1. Create `database/migrations/20260430_harden_profiles_rls.sql`.
2. Enable RLS on `public.profiles`.
3. Add own-row `SELECT` policy.
4. Add safe own-row `UPDATE` policy.
5. Revoke broad authenticated writes where needed.
6. Grant column-level update only for approved safe display/contact fields where needed.
7. Prevent normal authenticated clients from mutating:
   - `account_type`
   - `pharmacy_id`
   - `warehouse_id`
   - `is_active`
   - approval/status/admin-controlled fields
8. Do not create authenticated `DELETE` policy.
9. Implement the Task 2 creation decision:
   - direct `PUBLIC_USER` insert,
   - trusted RPC,
   - trusted trigger,
   - or no direct insert.
10. Add protected-field trigger/RPC if Task 2 requires it.
11. Add comments explaining that `profiles` is trusted by `orders` RLS/RPC.

### Forbidden

- Do not change Kotlin.
- Do not change repositories.
- Do not change UI/navigation/domain/model.
- Do not change `Order` or `Request` models.
- Do not include secrets or real credentials.
- Do not use live service-role keys in repo.

### Stop Conditions

- Stop if the migration cannot be written without allowing protected-field client mutation.
- Stop if the migration would break required public-user profile creation and no approved alternative exists.
- Stop if live policy drift requires a different migration design.

### Acceptance Criteria

- Migration file exists only after approval.
- RLS/policies/grants/protected-field controls match Task 2.
- No Kotlin or app code is changed in this task.
- No secrets are saved.

## Task 4: Audit SupabaseAuthRepository Bootstrap Behavior

**Depends on**: Task 3  
**Output**: `.specify/verification/phase-4.5.1b-auth-bootstrap-audit.md`

### Objective

Audit `SupabaseAuthRepository.ensureProfileForCurrentUser` and related signup/bootstrap code before any repository changes.

### Review Target

- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`

### Steps

1. Locate `ensureProfileForCurrentUser`.
2. Locate profile upsert code.
3. Identify the exact DTO used for profile upsert.
4. Document every field written by the upsert.
5. Specifically document where these are written:
   - `account_type`
   - `is_active`
6. Document whether the path writes or could write:
   - `pharmacy_id`
   - `warehouse_id`
7. Trace `account_type` source:
   - signup metadata,
   - mapped user,
   - persisted profile row.
8. Document whether existing profiles can be overwritten.
9. Document the exact repository changes required for Task 5.

### Stop Conditions

- Stop before modifying Kotlin.
- Stop if the repository behavior differs from the source spec.
- Stop if Task 2 creation path is not compatible with current bootstrap assumptions.

### Acceptance Criteria

- Bootstrap audit document exists.
- Current `account_type`, `is_active`, and upsert behavior is precisely documented.
- Required repository changes are listed.
- No Kotlin is modified.

## Task 5: Update SupabaseAuthRepository Safely

**Depends on**: Task 4 and Approval Gate B  
**Output**: Minimal repository patch in `SupabaseAuthRepository.kt`

### Objective

Update auth bootstrap so profile role/tenant/admin fields are never controlled by normal authenticated client metadata or upsert behavior.

### Required Behavior

1. Read existing profile first.
2. Use persisted `profiles.account_type` as source of truth.
3. Do not update `account_type` on existing profiles.
4. Do not update `is_active`.
5. Do not write `pharmacy_id`.
6. Do not write `warehouse_id`.
7. Do not trust auth metadata as authorization truth.
8. Allow safe `PUBLIC_USER` profile creation only if Task 2 approved:
   - direct insert,
   - or RPC path,
   - or trigger path.
9. For sensitive roles, fail or remain pending safely until trusted assignment exists.
10. Preserve existing snapshot behavior where safe, using persisted profile values.

### Forbidden

- Do not change UI.
- Do not change navigation.
- Do not change domain/model.
- Do not change `Order` or `Request` models.
- Do not change `SupabasePharmaRepository` in this task.
- Do not add secrets.
- Do not treat metadata `account_type` as database authorization truth.

### Stop Conditions

- Stop if implementation requires changing UI/navigation/domain/model.
- Stop if the profile creation path from Task 2 is unclear.
- Stop if public-user bootstrap cannot be preserved without weakening protected-field security.

### Acceptance Criteria

- Existing profiles are fetched before write decisions.
- Existing `account_type` is not overwritten by client metadata.
- `is_active` is not written by normal client bootstrap.
- Tenant IDs are not written.
- Sensitive role escalation through metadata is impossible.
- No UI/navigation/domain/model changes are made.

## Task 6: Verify SupabasePharmaRepository.updateProfile Remains Safe

**Depends on**: Task 5  
**Output**: `.specify/verification/phase-4.5.1b-update-profile-safety.md`

### Objective

Verify profile editing remains limited to safe display/contact fields and does not introduce protected-field writes.

### Review Target

- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`

### Steps

1. Locate `updateProfile`.
2. Locate `ProfileUpdateDto`.
3. Confirm fields written are limited to:
   - `full_name`
   - `phone_number`
   - `pharmacy_name`
   - `pharmacy_location`
   - `warehouse_name`, if present
   - `warehouse_location`, if present
4. Confirm `ProfileUpdateDto` does not include:
   - `account_type`
   - `pharmacy_id`
   - `warehouse_id`
   - `is_active`
   - approval/status/admin-controlled fields
5. Confirm update target is own-row safe under the new RLS design.
6. Document any repository risk or follow-up needed.

### Forbidden

- Do not add sensitive fields.
- Do not broaden profile update capability.
- Do not modify UI/navigation/domain/model.

### Stop Conditions

- Stop if `updateProfile` writes protected fields.
- Stop if profile update requires a repository change not approved by this task.

### Acceptance Criteria

- Safety document exists.
- Safe fields are confirmed.
- Protected fields are absent from update DTO.
- Any gap is classified before proceeding.

## Task 7: Compile And Repository Smoke Verification

**Depends on**: Task 6  
**Output**: Compile and smoke result recorded in `.specify/verification/phase-4.5.1b-final-report.md`

### Objective

Verify the repository changes compile and no forbidden app surface was changed.

### Commands

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

### Steps

1. Run compile command.
2. Record success or failure.
3. Verify no UI/navigation/domain/model changes were made for this phase.
4. Verify profile bootstrap code compiles.
5. Verify repository method signatures remain compatible unless explicitly approved.
6. Document any compile failure with exact failing task and high-level cause.

### Stop Conditions

- Stop if compile fails due to profile hardening changes.
- Stop if UI/navigation/domain/model changes are detected.
- Stop if repository signatures changed without approval.

### Acceptance Criteria

- Compile result is recorded.
- No forbidden file category changes are present.
- Profile bootstrap compiles or failure is documented as `FIX REQUIRED`.

## Task 8: Profiles Negative Test Plan

**Depends on**: Task 7  
**Output**: `.specify/verification/phase-4.5.1b-profiles-negative-tests.md`

### Objective

Create a negative test plan that proves normal authenticated clients cannot mutate profile trust fields and can still update safe own-row display/contact data.

### Required Tests

1. User cannot update `account_type`.
2. User cannot update `pharmacy_id`.
3. User cannot update `warehouse_id`.
4. User cannot update `is_active`.
5. User cannot update approval/status/admin-controlled fields.
6. User can update own `full_name`.
7. User can update own `phone_number`.
8. User can update own allowed display organization fields.
9. User cannot update another user's profile.
10. User cannot delete own profile.
11. User cannot delete another user's profile.
12. Sensitive role self-assignment is denied:
    - `PHARMACY`
    - `WAREHOUSE`
    - `ADMIN`
13. `PUBLIC_USER` profile creation path works only if approved by Task 2.
14. Direct insert with `id != auth.uid()` is denied.
15. Direct insert with non-null tenant IDs is denied.

### Evidence Requirements

Each test must define:

- Actor
- Setup
- Operation
- Expected result
- Evidence to capture
- Severity if failed
- Whether live Supabase access is required

### Stop Conditions

- Stop if any protected-field mutation succeeds.
- Stop if public-user profile creation fails and no trusted alternative exists.
- Stop if tests require storing secrets in repo.

### Acceptance Criteria

- Negative test plan exists.
- Protected-field tests are explicit.
- Safe-field positive tests are explicit.
- No secrets are included.

## Task 9: Re-Run Orders Trust Impact Checklist

**Depends on**: Task 8  
**Output**: `.specify/verification/phase-4.5.1b-orders-trust-impact.md`

### Objective

Verify that after profiles hardening, Phase 4.5.1 `orders` RLS/RPC can safely trust profile role and tenant fields.

### Checklist

1. Confirm `orders` RPCs/policies read `profiles.account_type` only after profile trust hardening.
2. Confirm `profiles.account_type` cannot be mutated by normal authenticated users.
3. Confirm `profiles.pharmacy_id` cannot be spoofed or changed by normal authenticated users.
4. Confirm `profiles.warehouse_id` cannot be spoofed or changed by normal authenticated users.
5. Confirm `PUBLIC_USER` cannot become `PHARMACY` through profile mutation.
6. Confirm `PHARMACY` cannot move to another `pharmacy_id`.
7. Confirm `WAREHOUSE` cannot move to another `warehouse_id`.
8. Confirm `is_active` and approval/status fields remain server/admin controlled.
9. Confirm public-user B2C create still depends on trusted `PUBLIC_USER`.
10. Confirm pharmacy lifecycle RPCs still depend on trusted `PHARMACY` plus tenant match.

### Stop Conditions

- Stop if any orders policy/RPC still depends on untrusted profile fields.
- Stop if tenant spoofing remains possible.
- Stop if `PUBLIC_USER -> PHARMACY` escalation remains possible.

### Acceptance Criteria

- Orders trust impact document exists.
- The checklist clearly states whether `orders` can trust `profiles`.
- Any unresolved trust gap is marked `FIX REQUIRED`.

## Task 10: Final Profiles Hardening Report

**Depends on**: Task 9  
**Output**: `.specify/verification/phase-4.5.1b-final-report.md`

### Objective

Produce the final closure report for Phase 4.5.1B.

### Required Report Sections

1. QA verdict: `PASS` or `FAIL`
2. Files/docs created
3. SQL migration status
4. Policies added/changed
5. Grants/revokes added/changed
6. Triggers/RPCs added, if any
7. Repository changes
8. `SupabaseAuthRepository.ensureProfileForCurrentUser` final behavior
9. `SupabasePharmaRepository.updateProfile` safety status
10. Compile result
11. Profiles negative test status
12. Orders trust impact status
13. Profiles trust status:
    - trusted,
    - partially trusted,
    - unknown live,
    - or not trusted
14. Remaining live verification requirements
15. Secrets committed: must be no
16. Scope violations: yes/no
17. Closure decision:
    - `CLOSE Phase 4.5.1B`
    - or `FIX REQUIRED`

### Stop Conditions

- Any protected-field mutation risk remains.
- Compile failed and is not resolved.
- Live verification is required but unavailable and closure would claim production safety.
- Any secret was saved in repo.
- SQL or Kotlin was changed without required approval.

### Acceptance Criteria

- Final report exists.
- Closure decision is explicit.
- Remaining live verification requirements are explicit.
- If profile trust cannot be proven or hardened safely, result is `FIX REQUIRED`.

## Final Acceptance Criteria For This Task Plan

- Tasks file is created under `.specify/tasks/phase-4.5.1b-profiles-trust-hardening/`.
- No implementation is performed in this step.
- No migration file is created in this step.
- No Kotlin/repository/app code is changed in this step.
- Approval gates are explicit before SQL and Kotlin changes.
- Stop condition exists for unsafe or contradictory profile trust evidence.
- No secrets, JWTs, service-role keys, or credentials are saved.
