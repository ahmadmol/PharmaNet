# Phase 4.5.1B Auth Bootstrap Audit

**Task**: Task 4 - Audit SupabaseAuthRepository Bootstrap Behavior  
**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Scope**: Read-only audit of `SupabaseAuthRepository` profile bootstrap behavior.  
**Implementation changes**: None.

## 1. Summary

`SupabaseAuthRepository.ensureProfileForCurrentUser` is not compatible with the new profiles hardening migration as currently written.

Current behavior:

- Builds `ProfileUpsertDto`.
- Performs direct authenticated upsert into `profiles`.
- Writes `account_type`.
- Writes `is_active`.
- Does not write `pharmacy_id` or `warehouse_id`.
- Reads persisted `profiles.account_type` after the upsert and uses it for the final `UserSnapshot`.

After `database/migrations/20260430_harden_profiles_rls.sql`, this bootstrap path is expected to fail because:

- Direct authenticated `INSERT` on `profiles` is revoked and no direct insert policy exists.
- Broad authenticated `UPDATE` is revoked.
- The protected-field trigger blocks authenticated changes to `account_type` and `is_active`.

Task 5 is required.

## 2. Files Reviewed

Primary file:

- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`

Directly related model files used to trace source values:

- `core/common/src/main/kotlin/com/pharmalink/domain/model/AuthModels.kt`
- `core/common/src/main/kotlin/com/pharmalink/domain/model/AccountType.kt`
- `core/common/src/main/kotlin/com/pharmalink/domain/model/AuthSessionModels.kt`

Migration compatibility reference:

- `database/migrations/20260430_harden_profiles_rls.sql`

## 3. Located Bootstrap Paths

### Signup metadata path

Function:

- `signUp(request: SignUpRequest)`

Current metadata writes into Supabase Auth user metadata:

- `phone_number`
- `phone_e164`
- `full_name`
- `account_type`
- `pharmacy_name`
- `pharmacy_location`
- `warehouse_name`
- `warehouse_location`

`account_type` source:

- `SignUpRequest.accountType`
- serialized as `request.accountType.name`

Sensitive role behavior:

- `PHARMACY`, `WAREHOUSE`, and `ADMIN` use `requiresManualLoginAfterSignUp()`.
- They are signed out after signup if a transient session exists.
- This reduces session/adoption risk, but metadata still carries the requested account type.

### Metadata-to-user mapping path

Function:

- `mapUser(info: UserInfo)`

Behavior:

- Reads `info.userMetadata`.
- Reads `account_type` metadata.
- Requires it to be non-blank.
- Converts it with `AccountType.valueOf(rawAccountType)`.
- Returns `User(accountType = accountType, isActive = true, ...)`.

Risk:

- `mapUser` treats metadata `account_type` as required for constructing an app `User`.
- This metadata should be treated as an onboarding/request signal, not database authorization truth.

### Profile bootstrap path

Function:

- `ensureProfileForCurrentUser(user: User)`

Current operation:

```kotlin
val profilePayload = buildProfilePayload(user, authUser)
supabase.postgrest["profiles"].upsert(profilePayload) { ignoreDuplicates = false }
```

Table:

- `profiles`

Operation type:

- direct authenticated `upsert`

DTO:

- `ProfileUpsertDto`

Existing-row overwrite behavior:

- `ignoreDuplicates = false`
- This means the upsert may update an existing row, not only insert a missing row.

### Profile read path

Function:

- `fetchProfileRow(userId: String)`

Behavior:

- Selects from `profiles`.
- Filters by `id = userId`.
- Decodes `ProfileRowDto`.
- Requires exactly one row.

Final snapshot role source:

- `parsePersistedAccountType(profileRow.accountType, authUser.id)`
- The final `UserSnapshot.accountType` uses the persisted profile row after the upsert.

Important nuance:

- Even though the final snapshot uses persisted `profiles.account_type`, the code first upserts `account_type` from `User.accountType`, which may have come from metadata.

## 4. Exact Profile Write Operation

Current write operation:

| Item | Current value |
|---|---|
| Table | `profiles` |
| Operation | direct authenticated `upsert` |
| API | `supabase.postgrest["profiles"].upsert(profilePayload) { ignoreDuplicates = false }` |
| DTO | `ProfileUpsertDto` |
| Insert possible | Yes |
| Existing-row update possible | Yes |

Fields written by `ProfileUpsertDto`:

| Field | Written? | Source |
|---|---:|---|
| `id` | Yes | `authUser.id` |
| `phone_number` | Yes | `user.phoneNumber` |
| `full_name` | Yes | `user.fullName` |
| `account_type` | Yes | `user.accountType.name` |
| `pharmacy_id` | No | Not in DTO |
| `warehouse_id` | No | Not in DTO |
| `pharmacy_name` | Yes | `user.pharmacyName.takeIf { it.isNotBlank() }` |
| `pharmacy_location` | Yes | `user.pharmacyLocation.takeIf { it.isNotBlank() }` |
| `warehouse_name` | Yes | `user.warehouseName.takeIf { it.isNotBlank() }` |
| `warehouse_location` | Yes | `user.warehouseLocation.takeIf { it.isNotBlank() }` |
| `is_active` | Yes | `user.isActive` |

## 5. Account Type Source Trace

Primary source chain:

1. UI/domain constructs `SignUpRequest(accountType = ...)`.
2. `signUp` writes `request.accountType.name` into auth metadata under `account_type`.
3. `mapUser` reads `account_type` from auth metadata.
4. `mapUser` creates `User(accountType = metadataAccountType)`.
5. `buildProfilePayload` writes `user.accountType.name` into `ProfileUpsertDto.accountType`.
6. `ensureProfileForCurrentUser` upserts the DTO into `profiles`.
7. `fetchProfileRow` reads the profile row.
8. `parsePersistedAccountType` parses `profileRow.accountType`.
9. `UserSnapshot.accountType` is set from the fetched profile row.

Other account type paths:

- `buildSensitivePendingUser` uses `request.accountType` for pending sensitive signup when no active session is available.
- `User.accountType` has a default of `AccountType.PHARMACY`, but `mapUser` requires metadata and does not intentionally fall back to the default in normal authenticated mapping.

Conclusion:

- Metadata `account_type` currently feeds the profile upsert.
- Persisted profile row is used for the final snapshot, but only after a potentially unsafe upsert has already happened.

## 6. Compatibility With New Profiles Migration

Migration reference:

- `database/migrations/20260430_harden_profiles_rls.sql`

### Direct insert compatibility

Current code:

- Uses direct authenticated upsert to `profiles`.

Migration:

- Revokes `INSERT` on `public.profiles` from `authenticated`.
- Creates no authenticated `INSERT` policy.
- Provides `public.create_public_user_profile(...)` RPC for missing public-user profile creation.

Result:

- Current missing-profile bootstrap is expected to fail after migration.

### Existing profile update compatibility

Current code:

- Uses upsert with `ignoreDuplicates = false`.
- Writes all DTO fields on conflict/update behavior.

Migration:

- Revokes broad `UPDATE` from `authenticated`.
- Grants column-level update only for safe display/contact fields.
- Adds trigger blocking `account_type` and `is_active` changes for normal authenticated users.

Result:

- Existing-profile bootstrap is expected to fail if the upsert attempts to update protected fields.
- Even if column grants block before trigger, the current operation is incompatible.

### Protected fields

Current code writes:

- `account_type`: yes
- `is_active`: yes
- `pharmacy_id`: no
- `warehouse_id`: no

Migration protects:

- `account_type`
- `is_active`
- `pharmacy_id`
- `warehouse_id`
- `id`

Result:

- Current bootstrap conflicts with protected `account_type` and `is_active`.

### Metadata trust

Current code:

- Requires metadata `account_type` to construct `User`.
- Uses that `User.accountType` to build the profile upsert payload.

Result:

- Current code treats metadata as a write source for database role state.
- This is not acceptable after profiles trust hardening.

## 7. Required Task 5 Plan

Task 5 should make the smallest safe Kotlin change in `SupabaseAuthRepository`.

Recommended flow:

1. Get authenticated user.
2. Attempt to fetch existing profile row first.
3. If profile exists:
   - Use persisted `profileRow.accountType` as source of truth.
   - Do not update `account_type`.
   - Do not update `is_active`.
   - Do not write `pharmacy_id`.
   - Do not write `warehouse_id`.
   - Optionally update safe display/contact fields only through a safe DTO or leave that to `SupabasePharmaRepository.updateProfile`.
4. If profile is missing:
   - If the requested/metadata account type is `PUBLIC_USER`, call `public.create_public_user_profile(...)`.
   - If the requested/metadata account type is `PHARMACY`, `WAREHOUSE`, or `ADMIN`, fail safely or keep pending until trusted server/admin creates and approves the profile.
5. Fetch the profile row after creation or existing-row detection.
6. Build `UserSnapshot` from persisted profile fields.
7. Preserve current compatibility behavior where safe:
   - warehouse ID fallback from `warehouse_id` to `pharmacy_id`,
   - pharmacy display fallback to profile/user display fields,
   - existing `MissingPharmacyLinkageException` behavior for missing pharmacy linkage.

Required new/changed DTOs:

- Remove or stop using `ProfileUpsertDto` for trusted/admin fields.
- Add an RPC params DTO for `create_public_user_profile`, or use the Supabase client's RPC parameter mechanism.
- Consider a safe display/contact update DTO only if Task 5 intentionally updates display fields during bootstrap.

Forbidden in Task 5:

- Do not let metadata overwrite persisted `profiles.account_type`.
- Do not write `is_active`.
- Do not write tenant IDs.
- Do not self-create sensitive role profiles.
- Do not change UI/navigation/domain/model.

## 8. Risk Classification

| Risk | Classification | Reason |
|---|---|---|
| Current code writes `account_type` | P0 pre-hardening risk; blocked after migration | Metadata-fed role value is written through authenticated upsert. |
| Current code writes `is_active` | P0/pre-hardening risk; blocked after migration | Activation is admin-controlled in the hardening design. |
| Current code direct-upserts `profiles` | Runtime incompatibility after migration | Direct insert revoked; protected-field update blocked. |
| Sensitive role self-assignment after migration | Expected blocked by SQL; Kotlin must align | Migration blocks protected fields and direct insert, but Kotlin must stop attempting unsafe writes. |
| Compile risk for Task 5 | Medium | Removing/replacing `ProfileUpsertDto` may require new RPC params DTO and flow adjustments. |
| Runtime risk for Task 5 | Medium | RPC response shape and Supabase Kotlin RPC decoding must be implemented carefully. |
| Snapshot compatibility risk | Medium | Existing snapshot behavior depends on persisted role plus compatibility fallback logic. Preserve it. |

## 9. Task 4 Decision

Current code writes `account_type`:

- Yes.

Current code writes `is_active`:

- Yes.

Current code uses direct upsert/insert to `profiles`:

- Yes, direct authenticated upsert.

Compatible with new migration:

- No.

Kotlin change required in Task 5:

- Yes.

Can Task 5 proceed after approval:

- Yes.

## 10. Files Modified

Created:

- `.specify/verification/phase-4.5.1b-auth-bootstrap-audit.md`

No implementation files were modified.

## 11. No Implementation Changes Confirmation

Confirmed:

- No Kotlin was modified.
- No repositories were modified.
- No migrations were modified.
- No SQL was executed.
- No app code was modified.
- No UI/navigation/domain/model files were modified.
- No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were saved.
