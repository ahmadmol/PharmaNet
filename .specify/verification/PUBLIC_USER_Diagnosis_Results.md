# PUBLIC_USER Diagnosis Results

**System**: PharmaNet  
**Scope**: PUBLIC_USER identity, profile trust hardening, RLS/RPC enforcement, and B2C order trust checks  
**Date**: 2026-04-30  
**Environment**: Local repository/static verification only  
**Live SQL executed**: No  
**Runtime API tests executed**: No  
**Secrets used or saved**: No  

## 1. Executive Summary

This report implements the PUBLIC_USER diagnosis plan as far as the local repository environment allows.

Local/static evidence confirms that Phase 4.5.1B added the expected profile hardening controls:

- `public.profiles` RLS migration exists locally.
- Own-row `SELECT` and `UPDATE` policies are defined.
- Direct authenticated `INSERT`, broad `UPDATE`, and `DELETE` are revoked.
- `create_public_user_profile(...)` exists as the approved PUBLIC_USER creation RPC.
- A protected-field trigger blocks normal authenticated changes to:
  - `id`
  - `account_type`
  - `pharmacy_id`
  - `warehouse_id`
  - `is_active`
- `SupabaseAuthRepository` no longer performs direct `profiles` upsert.
- `SupabaseAuthRepository` calls `create_public_user_profile(...)` only for missing `PUBLIC_USER` profiles.
- Missing sensitive-role profiles fail safely instead of self-provisioning.
- `SupabasePharmaRepository.updateProfile` writes only safe display/contact fields.

Live Supabase verification was not executed in this task because no live/staging authenticated test sessions, service/admin setup channel, or API test credentials were provided. Therefore, all runtime scenarios are marked `⚪ غير منفذ`.

## 2. Evidence Sources

Local files inspected:

- `database/migrations/20260430_harden_profiles_rls.sql`
- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`
- `.specify/verification/phase-4.5.1b-auth-bootstrap-update.md`
- `.specify/verification/phase-4.5.1b-update-profile-safety.md`
- `.specify/verification/phase-4.5.1b-compile-smoke.md`
- `.specify/verification/phase-4.5.1b-profiles-negative-test-plan.md`
- `.specify/verification/phase-4.5.1b-orders-trust-impact-checklist.md`
- `.specify/verification/phase-4.5.1b-final-report.md`

Static evidence found:

- `profiles_select_own` policy exists locally.
- `profiles_update_own_safe_fields` policy exists locally.
- `prevent_profiles_protected_field_update` function and trigger exist locally.
- `create_public_user_profile(...)` RPC exists locally.
- `REVOKE INSERT`, `REVOKE UPDATE`, and `REVOKE DELETE` from `authenticated` exist locally.
- `GRANT EXECUTE` on `create_public_user_profile(...)` to `authenticated` exists locally.
- `CreatePublicUserProfileRpcParams` contains only safe fields.
- `ProfileUpsertDto` is absent from `SupabaseAuthRepository`.
- `ProfileUpdateDto` contains only safe profile update fields.

## 3. Status Legend

- `✅ نجاح`: Verified by local/static evidence in this task or previous Phase 4.5.1B reports.
- `❌ فشل`: Failure observed.
- `⚪ غير منفذ`: Requires live Supabase/API/runtime execution and was not run in this task.

## 4. Preflight Results

| Check | Steps executed | Actual result | Expected result | Evidence | Status | Notes |
|---|---|---|---|---|---|---|
| Migration exists locally | Inspected migration references. | `20260430_harden_profiles_rls.sql` exists locally. | Migration file exists. | `database/migrations/20260430_harden_profiles_rls.sql` | ✅ نجاح | Local file only; not proof applied live. |
| Profiles RLS design exists | Searched migration for profiles policies. | Own-row `SELECT` and `UPDATE` policies found. | Own-row policies are defined. | `profiles_select_own`, `profiles_update_own_safe_fields` | ✅ نجاح | Live policy application still unverified. |
| Protected-field trigger exists | Searched migration for trigger/function. | Trigger/function found for protected fields. | Protected fields are blocked from normal client mutation. | `prevent_profiles_protected_field_update` | ✅ نجاح | Runtime trigger behavior still unverified. |
| RPC exists locally | Searched migration for RPC. | `create_public_user_profile(...)` found. | Trusted RPC exists in migration. | `create_public_user_profile` | ✅ نجاح | Live RPC response shape still unverified. |
| Live migration applied | Not executed. | No live evidence collected. | Migration applied in dev/staging. | Supabase dashboard/SQL needed. | ⚪ غير منفذ | Required before production confidence. |
| Extra live protected fields | Not executed. | No live schema evidence collected. | No unprotected role/tenant/admin fields exist. | Live schema inspection needed. | ⚪ غير منفذ | Stop if extra fields are found live. |

## 5. Identity Creation Flow

### Scenario 1.1: Create Valid PUBLIC_USER

| Field | Result |
|---|---|
| Scenario | إنشاء `PUBLIC_USER` صالح |
| Actor/role | Authenticated user requesting `PUBLIC_USER` |
| Steps executed | Static inspection only: checked repository RPC path and migration RPC definition. |
| Actual result | Local code calls `create_public_user_profile(...)` for missing `PUBLIC_USER` profiles and passes only safe display/contact params. |
| Expected result | Runtime creation succeeds with `account_type = PUBLIC_USER`, `pharmacy_id = NULL`, `warehouse_id = NULL`, and server-selected `is_active`. |
| Evidence | `SupabaseAuthRepository.createMissingProfileForAllowedPublicUser`; `CreatePublicUserProfileRpcParams`; migration RPC. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Runtime signup/bootstrap and RPC response decode were not executed. |

### Scenario 1.2: Attempt Sensitive Role Through PUBLIC_USER RPC

| Field | Result |
|---|---|
| Scenario | محاولة إنشاء دور حساس عبر RPC المخصص لـ `PUBLIC_USER` |
| Actor/role | Authenticated user |
| Steps executed | Static inspection only: checked RPC params and SQL body. |
| Actual result | RPC signature does not expose `account_type`, `is_active`, `pharmacy_id`, `warehouse_id`, or arbitrary `id` parameters. |
| Expected result | Runtime attempt to pass sensitive params is rejected or ignored and cannot create `PHARMACY`, `WAREHOUSE`, or `ADMIN`. |
| Evidence | `create_public_user_profile(...)`; `CreatePublicUserProfileRpcParams`. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Live RPC abuse call not executed. |

### Scenario 1.3: Prevent Sensitive Self-Provisioning

| Field | Result |
|---|---|
| Scenario | منع self-create لملف `PHARMACY` أو `WAREHOUSE` أو `ADMIN` |
| Actor/role | Authenticated user with sensitive role request signal in metadata |
| Steps executed | Static inspection only: checked missing-profile branch. |
| Actual result | Missing profile creation is allowed only for `AccountType.PUBLIC_USER`; sensitive roles fail with provisioning-required behavior. |
| Expected result | Runtime sensitive-role bootstrap fails safely and no trusted sensitive profile is created by the client. |
| Evidence | `SupabaseAuthRepository.createMissingProfileForAllowedPublicUser`. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Runtime sensitive signup/bootstrap not executed. |

## 6. Profile Update Flow

### Scenario 2.1: Update Safe Profile Fields

| Field | Result |
|---|---|
| Scenario | تحديث الحقول الآمنة |
| Actor/role | Authenticated `PUBLIC_USER` |
| Steps executed | Static inspection only: checked `ProfileUpdateDto`. |
| Actual result | `ProfileUpdateDto` writes only `full_name`, `pharmacy_name`, `pharmacy_location`, and `phone_number`. |
| Expected result | Runtime own-row safe update succeeds and protected fields remain unchanged. |
| Evidence | `SupabasePharmaRepository.ProfileUpdateDto`; Task 6 safety report. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Runtime update was not executed. |

### Scenario 2.2: Attempt Protected Profile Field Update

| Field | Result |
|---|---|
| Scenario | محاولة تحديث الحقول الحساسة |
| Actor/role | Authenticated `PUBLIC_USER` |
| Steps executed | Static inspection only: checked migration trigger and grants. |
| Actual result | Migration revokes broad update and trigger blocks changes to `account_type`, `pharmacy_id`, `warehouse_id`, and `is_active`. |
| Expected result | Runtime protected-field update fails and values remain unchanged. |
| Evidence | `prevent_profiles_protected_field_update`; `REVOKE UPDATE`; safe column `GRANT UPDATE`. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Live protected-field mutation attempts were not executed. |

## 7. RLS Checks

### Scenario 3.1: Read Own Profile

| Field | Result |
|---|---|
| Scenario | قراءة profile الخاص بالمستخدم |
| Actor/role | `PUBLIC_USER` user A |
| Steps executed | Static inspection only. |
| Actual result | Migration defines `profiles_select_own` with `id = auth.uid()`. |
| Expected result | User A can read only user A profile. |
| Evidence | `profiles_select_own` policy. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Live own-row read not executed. |

### Scenario 3.2: Read Another User Profile

| Field | Result |
|---|---|
| Scenario | محاولة قراءة profile مستخدم آخر |
| Actor/role | `PUBLIC_USER` user A targeting user B |
| Steps executed | Static inspection only. |
| Actual result | Own-row select policy should filter out user B. |
| Expected result | Query fails or returns zero rows. |
| Evidence | `profiles_select_own` policy. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Live cross-user read not executed. |

### Scenario 3.3: Update Another User Profile

| Field | Result |
|---|---|
| Scenario | محاولة تحديث profile مستخدم آخر |
| Actor/role | `PUBLIC_USER` user A targeting user B |
| Steps executed | Static inspection only. |
| Actual result | Own-row update policy should deny or affect zero rows. |
| Expected result | User B profile remains unchanged. |
| Evidence | `profiles_update_own_safe_fields` policy. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Live cross-user update not executed. |

### Scenario 3.4: Anonymous Profile Access

| Field | Result |
|---|---|
| Scenario | Anonymous read/insert/update/RPC |
| Actor/role | Anonymous |
| Steps executed | Static inspection only. |
| Actual result | Policies/grants are scoped to `authenticated`; RPC checks `auth.uid()`. |
| Expected result | Anonymous user cannot read, insert, update, or call `create_public_user_profile(...)` successfully. |
| Evidence | Migration policies/grants; RPC auth guard. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Anonymous API/RPC calls not executed. |

## 8. RPC Enforcement

### Scenario 4.1: PUBLIC_USER RPC Path

| Field | Result |
|---|---|
| Scenario | استدعاء RPC مخصص لـ `PUBLIC_USER` |
| Actor/role | Authenticated `PUBLIC_USER` |
| Steps executed | Static inspection only. |
| Actual result | `SupabaseAuthRepository` uses `rpc("create_public_user_profile", params)` for missing public profiles. |
| Expected result | Runtime RPC succeeds only for valid missing self-owned public profile and returns `ProfileRowDto` shape. |
| Evidence | `createMissingProfileForAllowedPublicUser`; `ProfileRowDto`. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Runtime RPC response shape not verified. |

### Scenario 4.2: PUBLIC_USER Calls PHARMACY/Admin RPCs

| Field | Result |
|---|---|
| Scenario | محاولة استدعاء RPC لدور آخر |
| Actor/role | `PUBLIC_USER` |
| Steps executed | Static inspection only: reviewed order trust checklist and B2C lifecycle RPC evidence. |
| Actual result | B2C pharmacy lifecycle RPCs are designed to require `profiles.account_type = PHARMACY` and trusted `profiles.pharmacy_id`. |
| Expected result | `PUBLIC_USER` cannot call `confirm_customer_order`, `reject_customer_order`, or `mark_customer_order_*` successfully. |
| Evidence | `20260429_harden_b2c_order_rls.sql`; Task 9 orders trust checklist. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Live RPC calls were not executed. |

### Scenario 4.3: Repeated PUBLIC_USER Profile RPC

| Field | Result |
|---|---|
| Scenario | تكرار استدعاء `create_public_user_profile(...)` |
| Actor/role | Authenticated `PUBLIC_USER` with existing profile |
| Steps executed | Static inspection only. |
| Actual result | RPC performs insert into `profiles`; duplicate behavior depends on live unique constraint/error behavior. |
| Expected result | Repeated call fails safely or is idempotent with no duplicate row and no protected-field mutation. |
| Evidence | Migration RPC body. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Must be verified live. |

## 9. B2C Order Checks For PUBLIC_USER

### Scenario 4.4: Create B2C Order As PUBLIC_USER

| Field | Result |
|---|---|
| Scenario | إنشاء B2C order كـ `PUBLIC_USER` |
| Actor/role | `PUBLIC_USER` |
| Steps executed | Static inspection only. |
| Actual result | Repository requires `identity.role == PUBLIC_USER` and uses `customerId = identity.userId`. |
| Expected result | Runtime order creation succeeds for valid selected pharmacy and own customer id only. |
| Evidence | `SupabasePharmaRepository.createCustomerOrder`; `customer_create_order` policy. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Runtime order fixture/test not executed. |

### Scenario 4.5: Spoof Customer Or Role During B2C Order Creation

| Field | Result |
|---|---|
| Scenario | محاولة spoofing لـ `customer_id` أو role |
| Actor/role | Normal authenticated user |
| Steps executed | Static inspection only. |
| Actual result | Repository derives `customer_id` from persisted identity; DB policy checks `auth.uid()` and `profiles.account_type = PUBLIC_USER`. |
| Expected result | Direct spoofed insert fails under RLS/policy. |
| Evidence | `SupabasePharmaRepository.createCustomerOrder`; `20260429_harden_b2c_order_rls.sql`. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Direct DB/API spoof attempt not executed. |

### Scenario 4.6: Cancel Own Customer Order

| Field | Result |
|---|---|
| Scenario | إلغاء order خاص بالمستخدم العام |
| Actor/role | `PUBLIC_USER` owner |
| Steps executed | Static inspection only. |
| Actual result | Repository requires `PUBLIC_USER`, checks owner locally, and calls `cancel_customer_order`. |
| Expected result | Runtime cancel succeeds only for own cancellable `CUSTOMER_PHARMACY` order. |
| Evidence | `SupabasePharmaRepository.cancelCustomerOrder`; `cancel_customer_order`. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Runtime order cancellation not executed. |

## 10. is_active Handling

### Scenario 5.1: Protect is_active

| Field | Result |
|---|---|
| Scenario | حماية `is_active` من تحديث client |
| Actor/role | Authenticated `PUBLIC_USER` |
| Steps executed | Static inspection only. |
| Actual result | Migration trigger blocks `NEW.is_active IS DISTINCT FROM OLD.is_active`; repository no longer writes `is_active`. |
| Expected result | Runtime client update to `is_active` fails. |
| Evidence | `prevent_profiles_protected_field_update`; `SupabaseAuthRepository` update report. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Live mutation attempt not executed. |

### Scenario 5.2: Inactive User Behavior

| Field | Result |
|---|---|
| Scenario | سلوك المستخدم غير النشط |
| Actor/role | `PUBLIC_USER` with `is_active = false` set by trusted admin/setup |
| Steps executed | Static inspection only. |
| Actual result | Local Task 9 evidence found no order/request enforcement of `profiles.is_active`. |
| Expected result | If product requires inactive-user blocking, login and order actions should be denied. |
| Evidence | Task 9 orders trust impact checklist. |
| Status | ⚪ غير منفذ |
| Notes/gaps | Known design gap unless product explicitly decides `is_active` is not an access gate yet. |

## 11. Findings And Gaps

### Confirmed By Static Evidence

- Direct profile upsert was removed from `SupabaseAuthRepository`.
- `ProfileUpsertDto` is absent from `SupabaseAuthRepository`.
- Missing profile creation is limited to `PUBLIC_USER`.
- Sensitive roles are not self-created by the Kotlin bootstrap path.
- Profile update payload writes safe fields only.
- Protected profile fields are covered by the SQL trigger locally.
- B2C pharmacy lifecycle RPCs rely on trusted profile role/tenant fields.

### Not Verified Live

- Whether the migration has been applied to the target Supabase project.
- Whether live `profiles` has additional protected/admin fields not covered by the migration.
- Whether `create_public_user_profile(...)` returns a shape that decodes as `ProfileRowDto`.
- Whether RLS/grants/triggers deny all protected-field mutations at runtime.
- Whether anonymous access is fully denied at runtime.
- Whether order RPCs reject PUBLIC_USER access to pharmacy-only lifecycle transitions.
- Whether request/order RLS prevents all cross-tenant or spoofed ID cases.
- Whether inactive users are meant to be blocked and, if so, whether that is enforced.

## 12. Stop Conditions For Live Execution

During live execution, stop and mark the diagnosis as `❌ فشل` / `Insecure` if any of the following occurs:

- A normal authenticated user can mutate `account_type`.
- A normal authenticated user can mutate `pharmacy_id`.
- A normal authenticated user can mutate `warehouse_id`.
- A normal authenticated user can mutate `is_active`.
- A normal authenticated user can self-provision `PHARMACY`, `WAREHOUSE`, or `ADMIN`.
- A user can read or update another user's profile through normal client privileges.
- Anonymous access can read or mutate protected profile data.
- `PUBLIC_USER` can call pharmacy/admin lifecycle RPCs successfully.
- `PUBLIC_USER` can spoof `customer_id` for B2C order creation.
- Live schema contains additional role/tenant/admin profile fields that are not protected.
- Test execution requires saving secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens in the repository.

## 13. Final Verdict

**Verdict**: Conditionally Secure

Rationale:

- Local/static evidence supports the intended Phase 4.5.1B hardening design.
- The Kotlin bootstrap no longer writes trusted profile fields.
- The SQL migration locally blocks direct authenticated profile insert/update/delete paths except approved safe behavior.
- PUBLIC_USER self-provisioning is constrained to the approved RPC path locally.
- However, live Supabase verification and runtime API tests were not executed in this task.

This system cannot be classified as `Fully Secure` until live/staging tests prove:

- The migration is applied.
- RLS policies, grants, trigger, and RPC behave correctly.
- PUBLIC_USER cannot mutate protected fields.
- PUBLIC_USER cannot self-escalate or call role-specific RPCs.
- Order/request RLS denies spoofed or cross-tenant operations.
- The product decision around `is_active` is implemented or explicitly accepted as out of scope.

## 14. Recommended Next Actions

1. Run the live preflight inspection in a dev/staging Supabase project.
2. Execute the scenarios in sections 5-10 with disposable test users.
3. Capture redacted API responses and DB before/after snapshots in this file.
4. If all live tests pass, update each scenario status from `⚪ غير منفذ` to `✅ نجاح`.
5. If any protected mutation or role escalation succeeds, mark the verdict `Insecure` and open a fix task before production closure.

## 15. Scope Confirmation

Confirmed for this task:

- No Kotlin source code was modified.
- No SQL migration was modified.
- No UI/navigation/domain/model files were modified.
- No live Supabase SQL was executed.
- No runtime API tests were executed.
- No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were used or saved.
