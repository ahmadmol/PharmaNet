# Phase 4.5.1 Profiles Trust Verification

**Task**: Task 7 - Profiles Trust Verification  
**Phase**: Phase 4.5.1 RLS Security Fixes  
**Date**: 2026-04-30  
**Scope**: Read-only audit of `profiles` trust for RLS/RPC authorization.  
**Implementation changes**: None.

## 1. Summary

Profiles trust is not proven safe from local evidence.

The hardened `orders` RLS/RPC design depends on trusted `profiles` fields:

- `profiles.account_type`
- `profiles.pharmacy_id`
- `profiles.warehouse_id`

Local repo evidence does not contain `profiles` RLS enablement or policy SQL. In addition, app-side code contains an authenticated profile upsert path that writes `account_type` to `profiles`. Without live RLS proving that role and tenant fields cannot be updated by normal authenticated users, `profiles` must be treated as not trusted for Phase 4.5.1 closure.

Task 7 finding:

- P0 found: yes, as a local trust blocker.
- Profiles hardening required: yes, unless live Supabase policies already block sensitive field mutation and can be evidenced.
- Phase 4.5.1 must not close until profile role/tenant immutability is proven or hardened.

## 2. Local Profiles RLS Evidence

Local SQL evidence reviewed:

- `database/migrations/*.sql`
- `database/triggers/*.sql`
- `auth_debug_guide.md`
- `supabase_auth_guide.md`
- `.specify/verification/phase-4.5.1-policy-baseline.md`

Local findings:

| Check | Result | Classification |
|---|---|---|
| `ALTER TABLE profiles ENABLE ROW LEVEL SECURITY` | Not found | Missing locally |
| `CREATE POLICY` for `profiles SELECT` | Not found | Missing locally |
| `CREATE POLICY` for `profiles INSERT` | Not found | Missing locally |
| `CREATE POLICY` for `profiles UPDATE` | Not found | Missing locally |
| `CREATE POLICY` for `profiles DELETE` | Not found | Missing locally |
| Documentation reference | `auth_debug_guide.md` mentions `users_insert_own_profile`, but policy SQL is not present | Ambiguous |

Local RLS trust result:

- `profiles` is not trusted locally.
- Local evidence cannot prove users are blocked from mutating `account_type`, `pharmacy_id`, or `warehouse_id`.

## 3. Live Profiles RLS Evidence

Live evidence:

- MCP resources were checked in this session and none were exposed.
- No live Supabase Dashboard/SQL policy output was available.
- No credentials, API keys, JWTs, service-role keys, or passwords were read from or saved to the repo.

| Check | Result | Classification |
|---|---|---|
| Live `profiles` RLS enabled? | Unknown | Unknown live |
| Live `profiles SELECT` policies | Unknown | Unknown live |
| Live `profiles INSERT` policies | Unknown | Unknown live |
| Live `profiles UPDATE` policies | Unknown | Unknown live |
| Live `profiles DELETE` policies | Unknown | Unknown live |
| Live sensitive-field immutability | Unknown | Unknown live |

Live RLS trust result:

- Unknown live.
- Live verification remains required before claiming production security is fixed.

## 4. App-Side Profile Write Paths

### `SupabasePharmaRepository.updateProfile`

File:

- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`

Write path:

- `supabase.postgrest.from("profiles").update(updateDto)`
- Filter: `eq("id", profile.id)`

Fields written through `ProfileUpdateDto`:

- `full_name`
- `pharmacy_name`
- `pharmacy_location`
- `phone_number`

Sensitive fields written:

- `account_type`: no
- `pharmacy_id`: no
- `warehouse_id`: no

Assessment:

- This path appears locally whitelisted to profile display/contact fields only.
- It does not accept `account_type`, `pharmacy_id`, or `warehouse_id` from UI input.
- It still depends on RLS to ensure a user can update only their own row.

### `SupabaseAuthRepository.ensureProfileForCurrentUser`

File:

- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`

Write path:

- `supabase.postgrest["profiles"].upsert(profilePayload) { ignoreDuplicates = false }`

Fields written through `ProfileUpsertDto`:

- `id`
- `phone_number`
- `full_name`
- `account_type`
- `pharmacy_name`
- `pharmacy_location`
- `warehouse_name`
- `warehouse_location`
- `is_active`

Sensitive fields written:

- `account_type`: yes
- `pharmacy_id`: no
- `warehouse_id`: no

Assessment:

- This is a sensitive app-side write path because it upserts `account_type`.
- The value is built from the app/user auth model, ultimately derived from sign-up metadata and persisted profile reads.
- If live `profiles` RLS allows authenticated users to upsert or update their own `account_type` after initial profile creation, then a user could potentially change role trust inputs used by `orders` RLS/RPC logic.
- Local repo evidence does not prove this upsert is insert-only, server-controlled, trigger-protected, or blocked from updating role fields.

### Profile UI Inputs

Profile edit UI path reviewed:

- `feature/profile/src/main/kotlin/com/pharmalink/feature/profile/ProfileViewModel.kt`

User-editable fields passed to `updateProfile`:

- name
- pharmacy display name
- phone
- address

Sensitive fields passed from UI:

- `account_type`: no
- `pharmacy_id`: no
- `warehouse_id`: no

Assessment:

- The visible profile edit UI does not appear to let users directly submit role or tenant identifiers.
- The more serious local concern is the auth/bootstrap upsert path plus missing local RLS proof.

## 5. Sensitive Field Mutation Risk

| Sensitive field | Local app write path found? | Local RLS proof blocking mutation? | Risk |
|---|---:|---:|---|
| `account_type` | Yes, `ProfileUpsertDto` in `SupabaseAuthRepository.ensureProfileForCurrentUser` | No | P0 blocker until hardened/proven safe |
| `pharmacy_id` | No app-side write path found in reviewed Kotlin DTOs | No | Unknown; must be proven by live RLS |
| `warehouse_id` | No app-side write path found in reviewed Kotlin DTOs | No | Unknown; must be proven by live RLS |
| approval/status fields | `is_active` is written by `ProfileUpsertDto` | No | Risky; should be server-controlled or policy-limited |

P0 scenarios that must be blocked before closure:

- `PUBLIC_USER` changing `account_type` to `PHARMACY` or `WAREHOUSE`.
- `PHARMACY` changing `pharmacy_id` to another pharmacy.
- `WAREHOUSE` changing `warehouse_id` to another warehouse.
- Any authenticated user changing `is_active` or role/approval fields in a way that bypasses review.

## 6. Trust Classification

| Area | Classification | Reason |
|---|---|---|
| Local `profiles` RLS | Not trusted locally | No local RLS enablement or policy SQL found |
| Local `profiles.account_type` | Not trusted locally | App-side upsert writes `account_type`; no local RLS proof blocks mutation |
| Local `profiles.pharmacy_id` | Unknown locally | No app write path found, but no local RLS/policy proof |
| Local `profiles.warehouse_id` | Unknown locally | No app write path found, but no local RLS/policy proof |
| Live `profiles` RLS | Unknown live | Live policy access unavailable |
| Live sensitive-field immutability | Unknown live | No live policy/test evidence |

Overall trust classification:

- Not trusted locally.
- Unknown live.

## 7. P0 Found?

P0 found: yes, as a local trust blocker.

Why:

- The Phase 4.5.1 RLS/RPC fix depends on `profiles.account_type` and tenant identifiers being trusted.
- Local evidence does not include `profiles` RLS policies.
- App-side code can upsert `account_type` into `profiles`.
- Without live policy evidence proving this cannot be abused, the security model can be undermined.

This is not a proof that live production currently allows exploitation. It is a P0 closure blocker because the repository cannot prove the trust boundary from local evidence.

## 8. Profiles Hardening Required?

Profiles hardening required: yes, unless live evidence proves equivalent protections already exist.

Recommended next action:

- Open a profiles hardening subtask inside Phase 4.5.1, or create Phase 4.5.2 for profile role/tenant immutability.

Required hardening goals:

- Enable RLS on `profiles`, if not already enabled live.
- Allow users to read only safe profile data required by the app.
- Allow profile creation only for the authenticated user's own `id`.
- Prevent normal authenticated clients from updating:
  - `account_type`
  - `pharmacy_id`
  - `warehouse_id`
  - approval/status fields such as `is_active`, unless explicitly intended and safely constrained
- Restrict regular profile updates to safe display/contact fields only.
- Prefer server/admin-controlled assignment for role and tenant identifiers.
- Re-test `orders` RPCs after profile trust is hardened or proven live.

## 9. Files Reviewed

- `database/migrations/20250425_extend_orders_for_b2c.sql`
- `database/migrations/20260429_harden_b2c_order_rls.sql`
- `database/triggers/create_order_from_request.sql`
- `auth_debug_guide.md`
- `supabase_auth_guide.md`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/domain/model/PharmacyProfile.kt`
- `feature/profile/src/main/kotlin/com/pharmalink/feature/profile/ProfileViewModel.kt`
- `app/src/main/kotlin/com/pharmalink/feature/profile/presentation/ProfileViewModel.kt`
- `.specify/verification/phase-4.5.1-policy-baseline.md`
- `.specify/verification/phase-4.5.1-rpc-repository-integration.md`
- `.specify/verification/phase-4.5.1-inmemory-compatibility.md`

## 10. Files Modified

Created/updated:

- `.specify/verification/phase-4.5.1-profiles-trust.md`

No implementation files were modified.

## 11. No Implementation Changes Confirmation

Confirmed:

- No SQL was changed.
- No migration was created or modified.
- No repository code was changed.
- No app code was changed.
- No UI/navigation code was changed.
- No domain/model code was changed.
- No live SQL was executed.
- No secrets, API keys, JWTs, service-role keys, or passwords were saved.

## Task 7 Decision

Can Task 8 proceed: yes, as a SQL/RLS negative test plan task.

Closure warning:

- Phase 4.5.1 cannot close until profiles trust is verified live or hardened by an approved profiles security subtask.
