# Phase 4.5.1B Auth Bootstrap Update

**Task**: Task 5 - Update SupabaseAuthRepository Safely  
**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Kotlin file changed**: `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`  
**Live SQL executed**: No

## 1. Summary

`SupabaseAuthRepository.ensureProfileForCurrentUser` was updated to align with the hardened `profiles` migration.

The previous direct authenticated `profiles` upsert path was removed. Bootstrap now reads an existing profile first and treats the persisted database profile row as the source of truth for role and tenant fields. Missing `PUBLIC_USER` profiles are created only through the trusted `public.create_public_user_profile(...)` RPC. Missing sensitive-role profiles fail safely and require trusted admin/server provisioning.

## 2. What Changed In `ensureProfileForCurrentUser`

Previous behavior:

- Build `ProfileUpsertDto`.
- Directly upsert into `profiles`.
- Write `account_type`.
- Write `is_active`.
- Fetch profile after upsert.

New behavior:

- Fetch existing profile first with `fetchProfileRowOrNull(authUser.id)`.
- If the row exists, use it directly.
- If the row is missing, call `createMissingProfileForAllowedPublicUser(user)`.
- Parse `account_type` only from the persisted or RPC-created profile row.
- Build `UserSnapshot` from the profile row.
- Preserve pharmacy/warehouse compatibility fallback behavior.
- Preserve `MissingPharmacyLinkageException` behavior for persisted `PHARMACY` profiles missing `pharmacy_id`.

## 3. Direct Profiles Upsert

Direct profiles upsert removed/stopped: yes.

Removed path:

```kotlin
supabase.postgrest["profiles"].upsert(profilePayload) { ignoreDuplicates = false }
```

`ProfileUpsertDto` was removed.

No replacement DTO writes protected fields.

## 4. Existing Profile Behavior

If a profile row exists:

- No upsert is performed.
- `account_type` is not written.
- `is_active` is not written.
- `pharmacy_id` is not written.
- `warehouse_id` is not written.
- Persisted `profileRow.accountType` is parsed and used as the source of truth.
- Snapshot creation remains based on persisted profile fields plus existing compatibility fallbacks.

Metadata `account_type` is not allowed to overwrite the existing profile.

## 5. Missing `PUBLIC_USER` Profile Behavior

If no profile row exists and the current requested/metadata account type is `PUBLIC_USER`:

- The repository calls:
  - `public.create_public_user_profile(...)`
- Only safe display/contact parameters are sent:
  - `p_full_name`
  - `p_phone_number`
  - `p_pharmacy_name`
  - `p_pharmacy_location`
  - `p_warehouse_name`
  - `p_warehouse_location`
- The RPC response is decoded as `ProfileRowDto`.
- The returned row is then used for persisted profile parsing and snapshot creation.

The RPC params DTO is:

- `CreatePublicUserProfileRpcParams`

It does not contain:

- `account_type`
- `is_active`
- `pharmacy_id`
- `warehouse_id`

## 6. Missing Sensitive-Role Profile Behavior

If no profile row exists and the requested/metadata account type is one of:

- `PHARMACY`
- `WAREHOUSE`
- `ADMIN`

the repository does not self-create a profile.

It fails safely with a clear message that trusted admin/server provisioning is required before login can continue.

Sensitive role self-creation blocked: yes.

## 7. Protected Field Writes

Current repository behavior after Task 5:

| Field | Written by `ensureProfileForCurrentUser`? | Notes |
|---|---:|---|
| `account_type` | No | Only parsed from profile row. |
| `is_active` | No | Not present in RPC params; not updated. |
| `pharmacy_id` | No | Only read from profile row. |
| `warehouse_id` | No | Only read from profile row. |
| `id` | No direct table write | RPC uses `auth.uid()` server-side. |
| `full_name` | RPC param for missing public profile only | Safe display field. |
| `phone_number` | RPC param for missing public profile only | Safe contact field. |
| `pharmacy_name` | RPC param for missing public profile only | Safe display field. |
| `pharmacy_location` | RPC param for missing public profile only | Safe display field. |
| `warehouse_name` | RPC param for missing public profile only | Safe display field. |
| `warehouse_location` | RPC param for missing public profile only | Safe display field. |

## 8. Compile Result

Command:

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

Initial result:

- Failed due to `java.lang.OutOfMemoryError: Metaspace`.
- The failure occurred while compiling feature modules and was memory-related.

Retry command:

```powershell
$env:GRADLE_OPTS='-Xmx4g -XX:MaxMetaspaceSize=1g'
$env:KOTLIN_DAEMON_JVMARGS='-Xmx2g -XX:MaxMetaspaceSize=1g'
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

Final result:

- `BUILD SUCCESSFUL`

Warnings:

- Existing Kotlin/Compose deprecation and annotation-target warnings were emitted.
- No compile error was caused by the Task 5 change.

## 9. Remaining Risks

- Runtime validation against live Supabase remains required.
- The RPC response shape must be verified live against `public.create_public_user_profile(...)`.
- Missing sensitive-role profiles now fail safely; product/admin provisioning flow must exist before sensitive-role login can succeed.
- Existing auth metadata still contains requested `account_type`, but it no longer overwrites persisted profile role during bootstrap.
- `mapUser` still requires metadata `account_type` to construct an app `User`; this is acceptable only as a pre-profile request signal and should not be treated as authorization truth.

## 10. Scope Confirmation

Changed:

- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`

Created:

- `.specify/verification/phase-4.5.1b-auth-bootstrap-update.md`

Not changed:

- `SupabasePharmaRepository.kt`
- `PharmaRepository.kt`
- `Order.kt`
- `Request.kt`
- UI/navigation files
- domain/model files
- migrations

No live SQL was executed.

No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were saved.

## 11. Can Task 6 Proceed?

Task 6 can proceed: yes.

Reason:

- Auth bootstrap no longer uses direct profile upsert.
- Protected fields are no longer written by `ensureProfileForCurrentUser`.
- Compile succeeded after the memory retry.
- Next task should verify `SupabasePharmaRepository.updateProfile` remains limited to safe display/contact fields.
