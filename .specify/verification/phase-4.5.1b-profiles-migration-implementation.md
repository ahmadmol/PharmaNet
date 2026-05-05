# Phase 4.5.1B Profiles Migration Implementation

**Task**: Task 3 - Implement Profiles SQL Migration  
**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Migration created**: `database/migrations/20260430_harden_profiles_rls.sql`  
**Live SQL executed**: No  
**Kotlin changes**: None

## 1. Summary

Schema confirmation succeeded from the available local app DTOs and repository usage. The required `profiles` columns for the approved migration were confirmed from `SupabaseAuthRepository` and `SupabasePharmaRepository`.

The migration was created as a SQL-only hardening migration. It enables RLS on `public.profiles`, resets existing profiles policies, creates own-row `SELECT` and `UPDATE` policies, denies direct authenticated insert/delete, limits authenticated updates to safe display/contact columns, adds a protected-field trigger, and creates a trusted `SECURITY DEFINER` RPC for self-owned `PUBLIC_USER` profile creation.

No SQL was executed against live Supabase.

## 2. Schema Confirmation

Confirmed `public.profiles` columns from local DTOs and repository usage:

| Column | Evidence | Classification |
|---|---|---|
| `id` | `ProfileUpsertDto`, `ProfileRowDto`, `ProfileDetailsDto`; used in profile filters | identity/protected |
| `phone_number` | `ProfileUpsertDto`, `ProfileRowDto`, `ProfileDetailsDto`, `ProfileUpdateDto` | safe display/contact |
| `full_name` | `ProfileUpsertDto`, `ProfileRowDto`, `ProfileDetailsDto`, `ProfileUpdateDto` | safe display/contact |
| `account_type` | `ProfileUpsertDto`, `ProfileRowDto`; used for auth/session and orders RPC trust | protected |
| `pharmacy_id` | `ProfileRowDto`, `ProfileDetailsDto`, `ProfilePharmacyDto`; used for order tenant trust | protected |
| `warehouse_id` | `ProfileRowDto`; used for warehouse/order tenant trust | protected |
| `pharmacy_name` | `ProfileUpsertDto`, `ProfileRowDto`, `ProfileDetailsDto`, `ProfileUpdateDto` | safe display/contact |
| `pharmacy_location` | `ProfileUpsertDto`, `ProfileDetailsDto`, `ProfileUpdateDto` | safe display/contact |
| `warehouse_name` | `ProfileUpsertDto`, `ProfileRowDto` | safe display/contact |
| `warehouse_location` | `ProfileUpsertDto` | safe display/contact |
| `is_active` | `ProfileUpsertDto` | protected |

Additional profile approval/status/admin-controlled fields:

- No additional `profiles` approval/status/admin-controlled fields were confirmed from local files.
- If live schema later reveals such fields, the protected-field trigger must be extended before production closure.

Confirmed safe update fields included in the migration:

- `full_name`
- `phone_number`
- `pharmacy_name`
- `pharmacy_location`
- `warehouse_name`
- `warehouse_location`

Confirmed protected fields included in the migration trigger:

- `id`
- `account_type`
- `pharmacy_id`
- `warehouse_id`
- `is_active`

## 3. Migration Contents

Created:

- `database/migrations/20260430_harden_profiles_rls.sql`

The migration includes:

1. `ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;`
2. Policy reset loop for existing `public.profiles` policies.
3. Own-row `SELECT` policy:
   - `profiles_select_own`
   - `USING (id = auth.uid())`
4. Own-row `UPDATE` policy:
   - `profiles_update_own_safe_fields`
   - `USING (id = auth.uid())`
   - `WITH CHECK (id = auth.uid())`
5. Protected-field trigger function:
   - `public.prevent_profiles_protected_field_update()`
6. Protected-field trigger:
   - `prevent_profiles_protected_field_update`
   - `BEFORE UPDATE ON public.profiles`
7. Trusted public profile creation RPC:
   - `public.create_public_user_profile(...)`
8. Grants/revokes for authenticated profile access.

## 4. Policies Created

### `profiles_select_own`

Allows authenticated users to read only their own profile row:

```sql
USING (id = auth.uid())
```

### `profiles_update_own_safe_fields`

Allows authenticated users to update only their own profile row:

```sql
USING (id = auth.uid())
WITH CHECK (id = auth.uid())
```

Column-level grants and the trigger provide the field-level safety.

No authenticated `INSERT` policy was created.

No authenticated `DELETE` policy was created.

## 5. Grants And Revokes

Revokes:

- `REVOKE INSERT ON public.profiles FROM authenticated;`
- `REVOKE UPDATE ON public.profiles FROM authenticated;`
- `REVOKE DELETE ON public.profiles FROM authenticated;`

Grants:

- `GRANT SELECT ON public.profiles TO authenticated;`
- `GRANT UPDATE (...) ON public.profiles TO authenticated;` for safe display/contact columns only.
- `GRANT EXECUTE ON FUNCTION public.create_public_user_profile(...) TO authenticated;`

No broad insert/update/delete grants are added for `authenticated`.

## 6. RPC Created

RPC:

- `public.create_public_user_profile(...)`

Behavior:

- Requires `auth.uid()` to be non-null.
- Inserts only a self-owned row with `id = auth.uid()`.
- Sets `account_type = 'PUBLIC_USER'`.
- Sets `pharmacy_id = NULL`.
- Sets `warehouse_id = NULL`.
- Sets `is_active = TRUE`.
- Accepts only display/contact fields as parameters.
- Does not allow client-provided protected fields.
- Returns the inserted `public.profiles` row.

Sensitive role self-creation is not supported by this RPC.

## 7. Trigger Created

Trigger function:

- `public.prevent_profiles_protected_field_update()`

Trigger:

- `prevent_profiles_protected_field_update`

Protected-field behavior for normal authenticated users:

- Blocks changes to `id`.
- Blocks changes to `account_type`.
- Blocks changes to `pharmacy_id`.
- Blocks changes to `warehouse_id`.
- Blocks changes to `is_active`.

The trigger compares `OLD` and `NEW` values with `IS DISTINCT FROM` and raises `42501` on protected-field mutation.

The trigger allows display/contact field updates.

`updated_at` handling:

- The trigger updates `updated_at` only if the row contains an `updated_at` field.
- No local DTO confirmed `profiles.updated_at`, so this remains schema-safe and conditional.

## 8. Assumptions

- The confirmed DTO columns match the live `public.profiles` table columns.
- `public.profiles.id` is comparable to `auth.uid()`.
- Supabase exposes `auth.uid()` and `auth.role()` in the database environment.
- Service-role/admin maintenance paths do not use normal `authenticated` table privileges.
- Additional approval/status/admin-controlled fields do not exist locally; if found live, they must be added to trigger protection before closure.

## 9. Kotlin Impact

Kotlin changes are required next, but were not made in this task.

Expected next issue:

- `SupabaseAuthRepository.ensureProfileForCurrentUser` currently performs an authenticated upsert to `profiles` and writes `account_type` and `is_active`.
- After this migration, direct authenticated insert is denied and protected-field mutation is blocked.

Task 4 should audit the current bootstrap behavior precisely.

Task 5 should update `SupabaseAuthRepository` to:

- Read an existing profile first.
- Treat persisted `profiles.account_type` as source of truth.
- Stop writing `account_type`.
- Stop writing `is_active`.
- Stop writing `pharmacy_id`.
- Stop writing `warehouse_id`.
- Use `create_public_user_profile(...)` only for approved missing `PUBLIC_USER` profile creation.

## 10. No Implementation Scope Violations

Confirmed:

- No Kotlin was modified.
- No repositories were modified.
- No UI/navigation files were modified.
- No domain/model files were modified.
- `Order.kt` was not modified.
- `Request.kt` was not modified.
- No old migration was modified.
- No live SQL was executed.
- No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were saved.

## 11. Can Task 4 Proceed?

Task 4 can proceed: yes.

Reason:

- The SQL migration file now exists.
- No Kotlin changes were made.
- Auth bootstrap behavior must now be audited before the approved repository update task.
