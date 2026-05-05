# Phase 4.5.1B Update Profile Safety

**Task**: Task 6 - Verify `SupabasePharmaRepository.updateProfile` remains safe  
**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Scope**: Read-only audit of `SupabasePharmaRepository.updateProfile` profile update behavior.  
**Implementation changes**: None.

## 1. Summary

`SupabasePharmaRepository.updateProfile` remains limited to safe display/contact profile fields.

No protected profile trust fields are written by the update payload. The repository updates a subset of the safe columns granted by `database/migrations/20260430_harden_profiles_rls.sql`, and cross-row protection relies on the new own-row `profiles` RLS policy.

No code changes were required for this task.

## 2. File Reviewed

Reviewed:

- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`

Migration compatibility reference:

- `database/migrations/20260430_harden_profiles_rls.sql`

## 3. `updateProfile` Location

Function:

- `SupabasePharmaRepository.updateProfile(profile: PharmacyProfile)`

Location:

- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt:256`

Current update operation:

```kotlin
val updatedRows = supabase.postgrest.from("profiles").update(updateDto) {
    select(Columns.ALL)
    filter { eq("id", profile.id) }
}.decodeList<ProfileDetailsDto>()
```

The update targets `profiles` and filters by `id = profile.id`.

## 4. Payload DTO Fields

DTO:

- `ProfileUpdateDto`

Location:

- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt:1570`

Fields written:

| DTO field | Database column | Classification |
|---|---|---|
| `fullName` | `full_name` | safe display/contact |
| `pharmacyName` | `pharmacy_name` | safe display/contact |
| `pharmacyLocation` | `pharmacy_location` | safe display/contact |
| `phoneNumber` | `phone_number` | safe display/contact |

Current mapping in `updateProfile`:

```kotlin
val updateDto = ProfileUpdateDto(
    fullName = profile.managerName,
    pharmacyName = profile.pharmacyName,
    pharmacyLocation = profile.addressLine,
    phoneNumber = profile.contactPhone
)
```

Safe fields found:

- `full_name`
- `phone_number`
- `pharmacy_name`
- `pharmacy_location`

Safe fields not written by this repository path:

- `warehouse_name`
- `warehouse_location`

This is acceptable because the repository only needs to update the pharmacy display/contact subset.

## 5. Protected Fields Check

`ProfileUpdateDto` does not include protected trust fields.

| Protected field | Present in update payload? | Result |
|---|---:|---|
| `id` | No | safe |
| `account_type` | No | safe |
| `pharmacy_id` | No | safe |
| `warehouse_id` | No | safe |
| `is_active` | No | safe |
| approval/status/admin-controlled fields | No local payload evidence | safe from this DTO |

Read DTO note:

- `ProfileDetailsDto` includes `id` and `pharmacy_id` for reading/verification.
- Reading `pharmacy_id` from the updated own row is not a mutation.
- `ProfileUpdateDto` does not write `pharmacy_id`.

## 6. Target Safety

The repository filters the update by:

```kotlin
filter { eq("id", profile.id) }
```

The function does not independently fetch `auth.uid()` before updating. Under the new migration, target safety is enforced by:

- `profiles_update_own_safe_fields`
- `USING (id = auth.uid())`
- `WITH CHECK (id = auth.uid())`

Expected behavior:

- If `profile.id` is the current authenticated user's profile id, the safe-field update can succeed.
- If `profile.id` belongs to another user, RLS should deny the update or return no rows.

The repository also verifies that exactly one row is returned and that the returned row id matches the requested profile id.

## 7. Compatibility With New Profiles Migration

The new migration grants authenticated users update privileges only for safe display/contact columns:

- `full_name`
- `phone_number`
- `pharmacy_name`
- `pharmacy_location`
- `warehouse_name`
- `warehouse_location`

`ProfileUpdateDto` writes only this granted subset:

- `full_name`
- `phone_number`
- `pharmacy_name`
- `pharmacy_location`

The protected-field trigger should not fire for this payload because no protected fields are included or changed by the repository update.

The update response uses `select(Columns.ALL)`. This remains compatible because the migration grants `SELECT` on `profiles` and RLS limits reads to the authenticated user's own row.

## 8. Risk Status

| Risk | Status | Notes |
|---|---|---|
| Protected field write in `ProfileUpdateDto` | Not found | No `account_type`, tenant id, activation, id, or admin field writes. |
| Cross-user update | Controlled by RLS | Repository filters by supplied `profile.id`; hardened RLS must enforce own-row access. |
| Migration grant mismatch | Low | DTO writes a subset of granted safe columns. |
| Trigger incompatibility | Low | Payload does not mutate protected fields. |
| Live verification | Still required | Production closure still requires live RLS/grant verification. |

Overall status:

- Safe for Task 6.
- No repository patch required.
- Task 7 can proceed.

## 9. Files Modified

Created:

- `.specify/verification/phase-4.5.1b-update-profile-safety.md`

No Kotlin, migration, UI, navigation, domain, model, order, or request files were modified by this task.

## 10. Compile Result

Compile was not run for Task 6.

Reason:

- This task was audit-only.
- No Kotlin code was changed.

## 11. No Implementation Changes Confirmation

Confirmed:

- No Kotlin was modified.
- `SupabaseAuthRepository.kt` was not modified.
- `SupabasePharmaRepository.kt` was not modified.
- No migrations were modified.
- No SQL was executed.
- `Order.kt` was not modified.
- `Request.kt` was not modified.
- No UI/navigation/domain/model files were modified.
- No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were saved.

## 12. Can Task 7 Proceed?

Task 7 can proceed: yes.

Reason:

- `updateProfile` writes only safe display/contact fields.
- Protected fields are absent from the update payload.
- The update path is compatible with the new own-row RLS policy, safe column grants, and protected-field trigger.
