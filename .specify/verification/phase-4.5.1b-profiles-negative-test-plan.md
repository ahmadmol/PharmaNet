# Phase 4.5.1B Profiles Negative Test Plan

**Task**: Task 8 - Profiles Negative Test Plan  
**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Scope**: Documentation/test-plan only.  
**Live SQL executed**: No  
**Runtime tests executed**: No

## 1. Summary

This plan defines the negative and safety tests required to verify profiles trust hardening after Tasks 1-7.

The hardening design and implementation under test:

- `public.profiles` RLS is enabled.
- Authenticated users can read only their own profile row.
- Direct authenticated `INSERT` into `profiles` is denied.
- Authenticated profile creation is allowed only through `public.create_public_user_profile(...)` for self-owned `PUBLIC_USER` profiles.
- Authenticated users can update only safe display/contact fields on their own row.
- Authenticated users cannot mutate `account_type`, `pharmacy_id`, `warehouse_id`, `is_active`, or protected admin-controlled fields.
- Auth bootstrap no longer performs direct `profiles` upsert and no longer writes trusted profile fields.

All tests in this document are planned and not executed.

## 2. Evidence Sources

Primary implementation evidence:

- `database/migrations/20260430_harden_profiles_rls.sql`
- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`

Verification reports:

- `.specify/verification/phase-4.5.1b-profiles-baseline.md`
- `.specify/verification/phase-4.5.1b-profiles-sql-design.md`
- `.specify/verification/phase-4.5.1b-profiles-migration-implementation.md`
- `.specify/verification/phase-4.5.1b-auth-bootstrap-audit.md`
- `.specify/verification/phase-4.5.1b-auth-bootstrap-update.md`
- `.specify/verification/phase-4.5.1b-update-profile-safety.md`
- `.specify/verification/phase-4.5.1b-compile-smoke.md`

## 3. Test Status Legend

- `planned/not executed`: test is defined but not run in this task.
- `live required`: requires an authenticated Supabase session or local test database with equivalent RLS/auth context.
- `static evidence`: can be checked by source inspection, compile, or diff review.

## 4. Anonymous User Tests

| Test ID | Actor/role | Precondition | Attempt/action | Expected result | Evidence source or file to inspect | Status |
|---|---|---|---|---|---|---|
| PHT-ANON-001 | Anonymous | No authenticated JWT/session. Hardened migration applied. | `SELECT * FROM public.profiles`. | Denied or zero rows. No protected profile data leaks. | Live Supabase SQL/runtime check; `profiles_select_own` policy in migration. | planned/not executed |
| PHT-ANON-002 | Anonymous | No authenticated JWT/session. Hardened migration applied. | Direct `INSERT` into `public.profiles`. | Denied. No profile row created. | Live Supabase SQL/runtime check; no anonymous insert policy. | planned/not executed |
| PHT-ANON-003 | Anonymous | No authenticated JWT/session. Hardened migration applied. | Direct `UPDATE public.profiles`. | Denied. No fields changed. | Live Supabase SQL/runtime check; no anonymous update policy/grant. | planned/not executed |
| PHT-ANON-004 | Anonymous | No authenticated JWT/session. RPC exists. | Call `public.create_public_user_profile(...)`. | Denied with authentication-required behavior. | Live RPC/runtime check; RPC `auth.uid()` guard. | planned/not executed |

## 5. PUBLIC_USER Tests

| Test ID | Actor/role | Precondition | Attempt/action | Expected result | Evidence source or file to inspect | Status |
|---|---|---|---|---|---|---|
| PHT-PUB-001 | `PUBLIC_USER` | Authenticated user has own profile. | Update own `account_type` to `PHARMACY`. | Denied by column grants and/or protected-field trigger. | Live Supabase SQL/runtime check; migration trigger. | planned/not executed |
| PHT-PUB-002 | `PUBLIC_USER` | Authenticated user has own profile. | Update own `is_active`. | Denied. `is_active` remains unchanged. | Live Supabase SQL/runtime check; migration trigger. | planned/not executed |
| PHT-PUB-003 | `PUBLIC_USER` | Authenticated user has own profile. | Update own `pharmacy_id` to any non-null tenant id. | Denied. Tenant linkage remains unchanged/null. | Live Supabase SQL/runtime check; migration trigger. | planned/not executed |
| PHT-PUB-004 | `PUBLIC_USER` | Authenticated user has own profile. | Update own `warehouse_id` to any non-null tenant id. | Denied. Tenant linkage remains unchanged/null. | Live Supabase SQL/runtime check; migration trigger. | planned/not executed |
| PHT-PUB-005 | `PUBLIC_USER` user A | User B profile exists. | Update user B safe field such as `full_name`. | Denied or zero rows by own-row RLS. | Live Supabase SQL/runtime check; `profiles_update_own_safe_fields`. | planned/not executed |
| PHT-PUB-006 | `PUBLIC_USER` | Authenticated user has own profile. | Update own safe fields: `full_name`, `phone_number`, allowed display fields. | Success for own row only. No protected fields changed. | Live Supabase runtime check; `SupabasePharmaRepository.updateProfile` safety report. | planned/not executed |
| PHT-PUB-007 | `PUBLIC_USER` | Authenticated user has no profile row. | Bootstrap/login calls `create_public_user_profile(...)` through repository path. | Missing profile is created through RPC only. Returned row has `account_type = PUBLIC_USER`, tenant IDs null, server-selected `is_active`. | Live runtime check; `SupabaseAuthRepository.kt`; migration RPC. | planned/not executed |
| PHT-PUB-008 | `PUBLIC_USER` | Authenticated user has no profile row. | Direct table `INSERT` with `account_type = PUBLIC_USER`. | Denied because direct authenticated insert is revoked and no insert policy exists. | Live Supabase SQL/runtime check; migration grants/policies. | planned/not executed |
| PHT-PUB-009 | `PUBLIC_USER` | Authenticated user has no profile row. | Try to create profile with `account_type = PHARMACY`, `WAREHOUSE`, or `ADMIN`. | Denied. Sensitive role self-assignment impossible. | Live Supabase SQL/RPC/runtime check; RPC parameter list. | planned/not executed |

## 6. PHARMACY Tests

| Test ID | Actor/role | Precondition | Attempt/action | Expected result | Evidence source or file to inspect | Status |
|---|---|---|---|---|---|---|
| PHT-PHA-001 | `PHARMACY` request signal | Authenticated user metadata requests `PHARMACY`; no profile row exists. | Bootstrap/login through `ensureProfileForCurrentUser`. | Fails safely with provisioning-required behavior. No profile is self-created. | Runtime repository test; `createMissingProfileForAllowedPublicUser`. | planned/not executed |
| PHT-PHA-002 | `PHARMACY` | Existing trusted pharmacy profile. | Update own `account_type` to another role. | Denied. Persisted role remains unchanged. | Live Supabase SQL/runtime check; protected-field trigger. | planned/not executed |
| PHT-PHA-003 | `PHARMACY` | Existing trusted pharmacy profile. | Update own `is_active`. | Denied. Activation remains admin/server controlled. | Live Supabase SQL/runtime check; protected-field trigger. | planned/not executed |
| PHT-PHA-004 | `PHARMACY` | Existing trusted pharmacy profile. | Update own `warehouse_id`. | Denied. Warehouse linkage cannot be self-assigned. | Live Supabase SQL/runtime check; protected-field trigger. | planned/not executed |
| PHT-PHA-005 | `PHARMACY` user A | User B profile exists. | Update user B safe profile fields. | Denied or zero rows by own-row RLS. | Live Supabase SQL/runtime check; `profiles_update_own_safe_fields`. | planned/not executed |
| PHT-PHA-006 | `PHARMACY` | Existing profile lacks `pharmacy_id`. | Bootstrap/login builds snapshot. | Fails with existing pharmacy linkage error behavior. | Runtime repository test; `MissingPharmacyLinkageException`. | planned/not executed |

## 7. WAREHOUSE Tests

| Test ID | Actor/role | Precondition | Attempt/action | Expected result | Evidence source or file to inspect | Status |
|---|---|---|---|---|---|---|
| PHT-WH-001 | `WAREHOUSE` request signal | Authenticated user metadata requests `WAREHOUSE`; no profile row exists. | Bootstrap/login through `ensureProfileForCurrentUser`. | Fails safely with provisioning-required behavior. No profile is self-created. | Runtime repository test; `createMissingProfileForAllowedPublicUser`. | planned/not executed |
| PHT-WH-002 | `WAREHOUSE` | Existing trusted warehouse profile. | Update own `account_type`. | Denied. Persisted role remains unchanged. | Live Supabase SQL/runtime check; protected-field trigger. | planned/not executed |
| PHT-WH-003 | `WAREHOUSE` | Existing trusted warehouse profile. | Update own `is_active`. | Denied. Activation remains admin/server controlled. | Live Supabase SQL/runtime check; protected-field trigger. | planned/not executed |
| PHT-WH-004 | `WAREHOUSE` | Existing trusted warehouse profile. | Update own `pharmacy_id`. | Denied. Pharmacy linkage cannot be self-assigned. | Live Supabase SQL/runtime check; protected-field trigger. | planned/not executed |
| PHT-WH-005 | `WAREHOUSE` user A | User B profile exists. | Update user B safe profile fields. | Denied or zero rows by own-row RLS. | Live Supabase SQL/runtime check; `profiles_update_own_safe_fields`. | planned/not executed |

## 8. ADMIN Tests

| Test ID | Actor/role | Precondition | Attempt/action | Expected result | Evidence source or file to inspect | Status |
|---|---|---|---|---|---|---|
| PHT-ADM-001 | `ADMIN` request signal | Authenticated user metadata requests `ADMIN`; no profile row exists. | Bootstrap/login through `ensureProfileForCurrentUser`. | Fails safely with provisioning-required behavior. No client-side admin profile self-provisioning. | Runtime repository test; `createMissingProfileForAllowedPublicUser`. | planned/not executed |
| PHT-ADM-002 | `ADMIN` request signal | Authenticated session exists without trusted admin profile. | Call `create_public_user_profile(...)`. | Creates only `PUBLIC_USER` if allowed by repository path/request type; repository must not use this for admin self-provisioning. | Runtime repository/RPC test; `SupabaseAuthRepository.kt`. | planned/not executed |
| PHT-ADM-003 | `ADMIN` | Existing trusted admin profile. | Attempt admin-specific profile mutations from normal client privileges. | No admin-specific mutation should be available from this phase unless separately defined by trusted admin/server policy. | Live Supabase SQL/runtime check; migration contains no admin write policy. | planned/not executed |

## 9. RPC Abuse Tests

| Test ID | Actor/role | Precondition | Attempt/action | Expected result | Evidence source or file to inspect | Status |
|---|---|---|---|---|---|---|
| PHT-RPC-001 | Authenticated user | RPC exists. | Call `create_public_user_profile` with only safe params. | Success only for missing self-owned public profile. Returned row shape decodes as `ProfileRowDto`. | Live runtime RPC test; `CreatePublicUserProfileRpcParams`; `ProfileRowDto`. | planned/not executed |
| PHT-RPC-002 | Authenticated user | RPC exists. | Attempt to pass `account_type` param. | Ignored/rejected by RPC signature. No role escalation. | Live RPC/runtime check; migration RPC signature. | planned/not executed |
| PHT-RPC-003 | Authenticated user | RPC exists. | Attempt to pass `is_active` param. | Ignored/rejected by RPC signature. `is_active` remains server-selected. | Live RPC/runtime check; migration RPC signature. | planned/not executed |
| PHT-RPC-004 | Authenticated user | RPC exists. | Attempt to pass `pharmacy_id` or `warehouse_id` params. | Ignored/rejected by RPC signature. Tenant IDs remain null for public profile creation. | Live RPC/runtime check; migration RPC signature. | planned/not executed |
| PHT-RPC-005 | Authenticated user | RPC exists. | Attempt to pass arbitrary `id`. | Ignored/rejected by RPC signature. Inserted `id`, if any, must equal `auth.uid()`. | Live RPC/runtime check; migration RPC uses `auth.uid()`. | planned/not executed |
| PHT-RPC-006 | Authenticated user | Profile row already exists. | Call `create_public_user_profile(...)` again. | Must be idempotent or fail safely with no protected-field mutation and no duplicate profile row. | Live RPC/runtime check; unique profile id behavior. | planned/not executed |
| PHT-RPC-007 | Anonymous | No authenticated session. | Call `create_public_user_profile(...)`. | Denied by `auth.uid()` guard. | Live RPC/runtime check; migration RPC guard. | planned/not executed |
| PHT-RPC-008 | Authenticated user | Missing public profile. | Bootstrap decodes RPC response. | Response shape matches `ProfileRowDto`; no runtime decode failure. | Live runtime app/repository test. | planned/not executed |

## 10. Repository Regression Tests

| Test ID | Actor/role | Precondition | Attempt/action | Expected result | Evidence source or file to inspect | Status |
|---|---|---|---|---|---|---|
| PHT-REG-001 | Static repository check | Source tree available. | Search `SupabaseAuthRepository.kt` for `upsert(` against `profiles`. | No direct profile upsert is present. | `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`; Task 7 smoke report. | planned/not executed |
| PHT-REG-002 | Static repository check | Source tree available. | Search for `ProfileUpsertDto`. | DTO remains absent from `SupabaseAuthRepository`. | `SupabaseAuthRepository.kt`; Task 7 smoke report. | planned/not executed |
| PHT-REG-003 | Static repository check | Source tree available. | Inspect `CreatePublicUserProfileRpcParams`. | Contains only safe RPC params and no protected fields. | `SupabaseAuthRepository.kt`; Task 5 report. | planned/not executed |
| PHT-REG-004 | Runtime repository test | Existing profile has `account_type = PUBLIC_USER`; metadata says `PHARMACY`. | Bootstrap/login. | Snapshot uses persisted `PUBLIC_USER`; database profile role is unchanged. | Repository test or manual runtime test; Task 5 behavior. | planned/not executed |
| PHT-REG-005 | Runtime repository test | Existing profile has `is_active = false`; app user object has active signal. | Bootstrap/login. | Repository does not update `is_active`; persisted value remains unchanged. | Repository test or manual runtime test; Task 5 behavior. | planned/not executed |
| PHT-REG-006 | Static repository check | Source tree available. | Inspect `SupabasePharmaRepository.ProfileUpdateDto`. | DTO remains safe-field only. | `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`; Task 6 report. | planned/not executed |
| PHT-REG-007 | Runtime repository test | User A and user B exist. | Use `updateProfile` with user B id from user A session. | Denied or zero rows by RLS; no cross-user update. | Live runtime test; Task 6 report. | planned/not executed |

## 11. Live Supabase Verification Checklist

These checks must be executed before production closure. They were not executed in Task 8.

| Test ID | Actor/role | Precondition | Attempt/action | Expected result | Evidence source or file to inspect | Status |
|---|---|---|---|---|---|---|
| PHT-LIVE-001 | Admin/operator running safe inspection | Live project access available without saving secrets in repo. | Inspect `public.profiles` RLS enabled state. | RLS is enabled on `public.profiles`. | Supabase dashboard/SQL output captured outside secrets. | planned/not executed |
| PHT-LIVE-002 | Admin/operator running safe inspection | Live project access available. | List policies on `public.profiles`. | Own-row SELECT and UPDATE policies exist; no unsafe INSERT/DELETE policies. | Supabase dashboard/SQL output. | planned/not executed |
| PHT-LIVE-003 | Admin/operator running safe inspection | Live project access available. | List grants on `public.profiles`. | No broad authenticated INSERT/UPDATE/DELETE; safe column update grants only. | Supabase dashboard/SQL output. | planned/not executed |
| PHT-LIVE-004 | Admin/operator running safe inspection | Live project access available. | Inspect triggers/functions on `public.profiles`. | Protected-field trigger exists and protects all live role/tenant/admin fields. | Supabase dashboard/SQL output. | planned/not executed |
| PHT-LIVE-005 | Admin/operator running safe inspection | Live schema available. | Inspect live `profiles` columns. | No additional approval/status/admin-controlled column is left unprotected. | Supabase dashboard/SQL output. | planned/not executed |
| PHT-LIVE-006 | Authenticated test users | Live or staging users exist for PUBLIC_USER, PHARMACY, WAREHOUSE, ADMIN request signals. | Run role-specific negative tests above. | All protected mutations denied; safe own-row updates still work. | Runtime logs/manual test evidence. | planned/not executed |
| PHT-LIVE-007 | Authenticated public user | Live/staging missing-profile scenario can be safely created. | Run public bootstrap through `create_public_user_profile`. | RPC creates expected row and repository decodes response as `ProfileRowDto`. | Runtime logs/manual test evidence. | planned/not executed |
| PHT-LIVE-008 | Authenticated sensitive-role test user | Missing sensitive-role profile can be safely simulated. | Run bootstrap. | Fails safely with provisioning-required behavior. | Runtime logs/manual test evidence. | planned/not executed |

## 12. Stop Conditions For Test Execution

Stop execution and mark `FIX REQUIRED` if any of the following happens:

- Any normal authenticated user can mutate `account_type`.
- Any normal authenticated user can mutate `pharmacy_id`.
- Any normal authenticated user can mutate `warehouse_id`.
- Any normal authenticated user can mutate `is_active`.
- Any normal authenticated user can mutate approval/status/admin-controlled profile fields.
- Any user can update another user's profile through normal client privileges.
- Sensitive-role users can self-create trusted `PHARMACY`, `WAREHOUSE`, or `ADMIN` profiles.
- `create_public_user_profile` accepts protected fields or arbitrary `id`.
- Public-user profile creation cannot work through the approved RPC and no trusted alternative exists.
- Testing requires storing secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens in the repo.

## 13. Task 8 Result

Negative test plan status:

- Created.
- Not executed.

Live verification status:

- Required before production closure.
- Not executed in this task.

Runtime RPC response shape status:

- Not verified in this task.

Can Task 9 proceed:

- Yes.

Reason:

- The required negative and live verification test plan exists.
- Task 9 can now assess whether `orders` trust assumptions are covered by this profiles hardening plan and remaining verification requirements.

## 14. No Implementation Changes Confirmation

Confirmed:

- No Kotlin source code was modified.
- No SQL migration was modified.
- No UI/navigation/domain/model files were modified.
- No live Supabase SQL was executed.
- No service-role secrets, JWTs, API keys, passwords, access tokens, or refresh tokens were used or saved.
