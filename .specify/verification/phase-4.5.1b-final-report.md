# Phase 4.5.1B Final Report

**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Scope**: Final verification and readiness report  
**Documentation only in Task 10**: Yes  
**Live SQL executed**: No

## 1. Phase Summary

Phase 4.5.1B hardened `profiles` as a trusted authorization source for orders/RPC logic.

What was hardened:

- `public.profiles` RLS and own-row access.
- Normal authenticated client writes to protected profile trust fields.
- Profile creation path for self-service public users.
- Auth bootstrap behavior that previously wrote trusted profile fields.

SQL changes:

- Created `database/migrations/20260430_harden_profiles_rls.sql`.
- Enabled RLS on `public.profiles`.
- Added own-row `SELECT` and `UPDATE` policies.
- Revoked broad authenticated `INSERT`, `UPDATE`, and `DELETE`.
- Granted authenticated `UPDATE` only on safe display/contact fields.
- Added protected-field immutability trigger.
- Added `public.create_public_user_profile(...)` RPC for self-owned `PUBLIC_USER` profile creation.

Kotlin changes:

- Updated `SupabaseAuthRepository.ensureProfileForCurrentUser`.
- Removed direct authenticated `profiles` upsert.
- Removed `ProfileUpsertDto`.
- Added safe public profile RPC params.
- Existing profiles are now read first and used as the source of truth.
- Missing sensitive-role profiles now fail safely instead of self-provisioning.

Risks eliminated locally:

- Client metadata can no longer overwrite persisted `profiles.account_type` through auth bootstrap.
- Auth bootstrap no longer writes `is_active`.
- Auth bootstrap does not write `pharmacy_id` or `warehouse_id`.
- Normal profile update DTO remains safe-field only.
- Sensitive roles cannot be self-created through the repository bootstrap path.

## 2. Trust Model Before vs After

Before Phase 4.5.1B:

- `profiles.account_type` was not locally protected by RLS/policies/grants.
- `profiles.pharmacy_id` was trusted by orders policies/RPCs but not locally proven immutable.
- `profiles.warehouse_id` was trusted by warehouse/B2B access logic but not locally proven immutable.
- `profiles.is_active` was written by normal authenticated bootstrap code.
- `SupabaseAuthRepository.ensureProfileForCurrentUser` performed direct authenticated upsert and wrote `account_type` and `is_active`.

After Phase 4.5.1B, assuming the migration is applied:

- `profiles.account_type` is DB-trusted and immutable to normal authenticated clients.
- `profiles.pharmacy_id` is DB-trusted and immutable to normal authenticated clients.
- `profiles.warehouse_id` is DB-trusted and immutable to normal authenticated clients.
- `profiles.is_active` is admin/server controlled and immutable to normal authenticated clients.
- Client metadata `account_type` remains only an onboarding/request signal.
- Persisted profile row values are the source of truth for repository session snapshots.

## 3. Security Guarantees Achieved

Assuming `database/migrations/20260430_harden_profiles_rls.sql` is applied correctly:

- No role escalation from normal authenticated clients through `profiles.account_type`.
- No tenant reassignment from normal authenticated clients through `profiles.pharmacy_id`.
- No tenant reassignment from normal authenticated clients through `profiles.warehouse_id`.
- No normal authenticated mutation of `profiles.is_active`.
- No direct authenticated table insert into `public.profiles`.
- No authenticated profile delete policy.
- Safe profile updates are limited to display/contact fields on the user's own row.
- Missing `PUBLIC_USER` profiles can be created only through the whitelisted RPC.
- Missing `PHARMACY`, `WAREHOUSE`, and `ADMIN` profiles are not self-provisioned by the client.

## 4. Remaining Risks

### Must Verify Before Production

- Profiles RLS live verification:
  - RLS enabled on `public.profiles`.
  - Expected own-row policies present.
  - Unsafe policies absent.
  - Grants/revokes match the migration.
  - Protected-field trigger exists and covers all live protected columns.

- RPC enforcement correctness:
  - `create_public_user_profile(...)` rejects anonymous calls.
  - It cannot accept `account_type`, `is_active`, `pharmacy_id`, `warehouse_id`, or arbitrary `id`.
  - It returns a row shape compatible with `ProfileRowDto`.
  - Repeated calls fail safely or are operationally handled.

- Orders RLS correctness:
  - B2C insert policy denies role spoofing and customer spoofing.
  - B2C lifecycle RPCs deny cross-role and cross-tenant transitions.
  - Warehouse/B2B policies still isolate by trusted profile tenant fields.

- Requests RLS correctness:
  - Request insert denies forged `pharmacy_id`.
  - Request insert/update denies invalid or unauthorized `warehouse_id`.
  - Request status transitions are enforced server-side or by RLS/RPC.
  - Request delete is limited to authorized owners and intended states.

### Known Design Gaps

- Trigger behavior vs request lifecycle:
  - `create_order_from_request` creates an order on request insert, while Android creates requests with `DRAFT` status.
  - Verify whether B2B orders should be created on insert or submit.

- Warehouse fallback:
  - `UserIdentityMapper` still allows `WAREHOUSE` `organizationId` fallback from `pharmacyId`.
  - Remove this after warehouse linkage migration is complete.

- B2C pharmacy targeting validation:
  - B2C order creation accepts selected `pharmacyId`.
  - DB should verify that target pharmacy is valid, active, and allowed for customer ordering.

- `is_active` not enforced:
  - Local orders/requests logic does not currently gate access on `profiles.is_active`.
  - Product/security decision required: should inactive users be blocked from all order/request flows?

## 5. Test Coverage Status

Static verification:

- Done.
- Tasks 1, 2, 4, 6, and 9 reviewed local evidence and documented risks.

SQL implementation:

- Done locally.
- Migration file created.
- Not executed against live Supabase in this phase.

Kotlin implementation:

- Done locally for `SupabaseAuthRepository`.
- No code change required for `SupabasePharmaRepository.updateProfile`.

Compile:

- Done.
- `.\gradlew.bat --no-daemon :app:compileDebugKotlin`
- Result: `BUILD SUCCESSFUL`

Negative test plan:

- Done.
- `.specify/verification/phase-4.5.1b-profiles-negative-test-plan.md`

Orders trust impact checklist:

- Done.
- `.specify/verification/phase-4.5.1b-orders-trust-impact-checklist.md`

Live runtime tests:

- NOT DONE.

## 6. Production Readiness Verdict

Verdict:

- ⚠️ Conditionally ready (requires live verification)

Justification:

- Local design, migration, repository patch, static review, and compile verification are complete.
- The original local P0 trust gap has an implemented local fix.
- However, live Supabase RLS, grants, triggers, RPC behavior, and runtime response shape were not verified.
- Production-safe closure must wait for live/staging evidence that the migration is applied and that protected profile mutations are denied.

This phase should not be represented as fully production ready until the live verification checklist is executed.

## 7. Next Phase Recommendations

Recommended next phase tasks:

1. Profiles live verification phase:
   - Apply/inspect profiles migration in staging.
   - Execute protected-field negative tests.
   - Verify `create_public_user_profile(...)` runtime response shape.

2. Orders/Requests RLS hardening phase:
   - Verify B2C order RLS/RPCs against trusted profile fields.
   - Verify request insert/update/delete RLS.
   - Add request lifecycle RPCs if direct updates remain too broad.

3. Trigger lifecycle decision:
   - Decide whether `create_order_from_request` should fire on request insert or request submit.
   - Update trigger/RPC design in a separate approved task if needed.

4. `is_active` enforcement decision:
   - Decide whether inactive users should be blocked in auth bootstrap, profiles RLS, orders RPCs, requests RPCs, or all of the above.

5. Warehouse linkage cleanup:
   - Remove `WAREHOUSE` fallback from `pharmacy_id` after explicit `warehouse_id` assignment is complete and live-verified.

6. Integration testing phase:
   - PUBLIC_USER create/cancel B2C order.
   - PHARMACY lifecycle B2C order transitions.
   - PHARMACY request lifecycle.
   - WAREHOUSE request lifecycle.
   - Cross-role and cross-tenant negative tests.

## 8. Final Decision

Can Phase 4.5.1B be closed?

- Yes, as a local implementation and verification-design phase.
- No, as a production security verification claim.

Can development continue on top of this?

- Yes.

Blocking item:

- Live/staging verification is still required before production closure.

Closure decision:

- `CLOSE Phase 4.5.1B` for local design, migration, repository alignment, compile, and verification planning.
- `FIX REQUIRED` is not currently indicated by local evidence.
- Production readiness remains conditional on live verification.

## 9. Scope And Secrets Confirmation

Confirmed:

- Task 10 changed documentation only.
- No Kotlin source code was modified in Task 10.
- No SQL migration was modified in Task 10.
- No UI/navigation/domain/model files were modified in Task 10.
- `Order.kt` was not modified in Task 10.
- `Request.kt` was not modified in Task 10.
- No live Supabase SQL was executed.
- No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were used or saved.
