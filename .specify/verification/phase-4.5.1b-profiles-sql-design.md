# Phase 4.5.1B Profiles SQL Migration Design

**Task**: Task 2 - Design Final Profiles SQL Migration  
**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Proposed migration**: `database/migrations/20260430_harden_profiles_rls.sql`  
**Implementation changes**: None. This is design only.

## 1. Summary

This document designs the SQL migration needed to make `public.profiles` safe as an authorization source for Phase 4.5.1 `orders` RLS/RPC logic.

Design decisions:

- Enable RLS on `public.profiles`.
- Do not use broad role-based profile reads in this phase.
- Allow authenticated users to read only their own profile row.
- Do not allow direct authenticated table insert into `public.profiles`.
- Create a trusted `SECURITY DEFINER` RPC for self-owned `PUBLIC_USER` profile creation.
- Revoke broad authenticated profile writes.
- Grant authenticated update only on safe display/contact columns.
- Add a `BEFORE UPDATE` immutability trigger as defense in depth for protected fields.
- Do not grant or policy-enable authenticated profile deletes.
- Require all role, tenant, activation, approval, and admin-controlled assignment through trusted server/admin/service-role paths.

Task 3 must not start until this design is reviewed and explicitly approved.

## 2. Baseline Assumptions

Task 1 baseline:

- Local `profiles` RLS is missing.
- Local `profiles` policies are missing.
- Local `profiles` grants/revokes are missing.
- Local protected-field immutability is not proven.
- Live evidence is unavailable.
- Complete `profiles` schema is not fully available locally.

Implication:

- The design must be defensive.
- Unknown approval/status/admin-controlled fields must be treated as protected until proven otherwise.
- Task 3 needs schema confirmation before writing the final protected-field trigger list.

## 3. RLS Enablement

Required migration design:

```sql
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
```

Decision on `FORCE ROW LEVEL SECURITY`:

- Do not enable `FORCE ROW LEVEL SECURITY` in the first implementation by default.

Rationale:

- The local repo does not contain the full operational profile maintenance path.
- Supabase service-role/admin maintenance may rely on bypass behavior.
- The immediate risk is normal authenticated client mutation; policies, grants, and the trigger address that risk.
- `FORCE ROW LEVEL SECURITY` can be revisited after live admin/service workflows are inventoried.

Task 3 note:

- If live inspection proves no owner/admin maintenance path depends on bypass behavior, `FORCE ROW LEVEL SECURITY` may be added only with explicit approval.

## 4. SELECT Policy

Design only own-row profile reads for authenticated users:

```sql
DROP POLICY IF EXISTS profiles_select_own ON public.profiles;

CREATE POLICY profiles_select_own
ON public.profiles
FOR SELECT
TO authenticated
USING (id = auth.uid());
```

No broader role-based profile reads are designed in this phase.

Deferred:

- Pharmacy tenant profile directory reads.
- Warehouse tenant profile directory reads.
- Admin profile search/listing.
- Public-safe profile views.

If broader reads are required later, design them as a separate phase using a safe view or RPC that exposes only needed columns.

## 5. INSERT Decision

Chosen strategy:

- Use a trusted `SECURITY DEFINER` RPC for public-user profile creation.
- Do not grant direct authenticated `INSERT` on `public.profiles`.
- Do not create a direct authenticated `INSERT` policy on `public.profiles`.

Why not direct insert now:

- Complete `profiles` schema is not available locally.
- Unknown approval/status/admin-controlled columns may exist.
- A direct insert policy can validate known columns but cannot safely reason about unknown writable columns.
- The current app bootstrap uses an authenticated upsert that writes `account_type` and `is_active`; direct insert would make that trust boundary easier to accidentally keep.

RPC design:

```sql
CREATE OR REPLACE FUNCTION public.create_public_user_profile(
    p_full_name text DEFAULT NULL,
    p_phone_number text DEFAULT NULL,
    p_pharmacy_name text DEFAULT NULL,
    p_pharmacy_location text DEFAULT NULL,
    p_warehouse_name text DEFAULT NULL,
    p_warehouse_location text DEFAULT NULL
)
RETURNS public.profiles
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_profile public.profiles%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Authentication required'
            USING ERRCODE = '42501';
    END IF;

    INSERT INTO public.profiles (
        id,
        full_name,
        phone_number,
        account_type,
        pharmacy_id,
        warehouse_id,
        pharmacy_name,
        pharmacy_location,
        warehouse_name,
        warehouse_location,
        is_active
    )
    VALUES (
        auth.uid(),
        p_full_name,
        p_phone_number,
        'PUBLIC_USER',
        NULL,
        NULL,
        p_pharmacy_name,
        p_pharmacy_location,
        p_warehouse_name,
        p_warehouse_location,
        TRUE
    )
    RETURNING *
    INTO v_profile;

    RETURN v_profile;
END;
$$;
```

Design constraints:

- `id` must always be `auth.uid()`.
- `account_type` must always be `'PUBLIC_USER'`.
- `pharmacy_id` must always be `NULL`.
- `warehouse_id` must always be `NULL`.
- `is_active` must be server-selected, not client-provided.
- Approval/status/admin-controlled columns must be omitted or assigned safe server defaults.
- Sensitive roles (`PHARMACY`, `WAREHOUSE`, `ADMIN`) must not be self-created through this RPC.

Important Task 3 schema requirement:

- Before implementing this RPC, verify exact `profiles` columns and defaults.
- If required columns exist that are not known locally, either:
  - provide safe server-controlled values in the RPC, or
  - switch to an auth trigger/server-side creation path with explicit approval.

Grant design:

```sql
GRANT EXECUTE ON FUNCTION public.create_public_user_profile(
    text,
    text,
    text,
    text,
    text,
    text
) TO authenticated;
```

No direct insert policy:

```sql
-- Intentionally no authenticated INSERT policy on public.profiles.
-- Public user creation must use public.create_public_user_profile(...).
```

## 6. UPDATE Policy

Authenticated users may update only their own row, and only safe display/contact fields.

Safe fields, if present:

- `full_name`
- `phone_number`
- `pharmacy_name`
- `pharmacy_location`
- `warehouse_name`
- `warehouse_location`

Policy design:

```sql
DROP POLICY IF EXISTS profiles_update_own_safe_fields ON public.profiles;

CREATE POLICY profiles_update_own_safe_fields
ON public.profiles
FOR UPDATE
TO authenticated
USING (id = auth.uid())
WITH CHECK (id = auth.uid());
```

Policy limitation:

- RLS verifies row ownership.
- RLS does not reliably enforce column immutability by itself.
- Column grants plus an immutability trigger are required.

Protected fields:

- `account_type`
- `pharmacy_id`
- `warehouse_id`
- `is_active`
- approval/status/admin-controlled fields
- any additional role/tenant fields discovered before Task 3

## 7. Protected-Field Enforcement Strategy

Chosen strategy:

- Use both column-level privileges and an immutability trigger.

Rationale:

- Column grants prevent normal clients from updating protected columns through ordinary table update privileges.
- A trigger catches future privilege drift, broad grants, accidental DTO expansion, or unexpected update paths.
- RLS `WITH CHECK` alone is not enough to compare all old and new protected values.

### Grants/Revoke Layer

Design:

```sql
REVOKE UPDATE ON public.profiles FROM authenticated;

GRANT UPDATE (
    full_name,
    phone_number,
    pharmacy_name,
    pharmacy_location,
    warehouse_name,
    warehouse_location
) ON public.profiles TO authenticated;
```

Task 3 schema note:

- If a safe column does not exist, omit it from the grant list.
- Do not add new columns in this migration just to satisfy the grant list.

### Trigger Layer

Function design:

```sql
CREATE OR REPLACE FUNCTION public.prevent_profiles_protected_field_update()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF auth.uid() IS NOT NULL THEN
        IF NEW.id IS DISTINCT FROM OLD.id THEN
            RAISE EXCEPTION 'profiles.id cannot be changed'
                USING ERRCODE = '42501';
        END IF;

        IF NEW.account_type IS DISTINCT FROM OLD.account_type THEN
            RAISE EXCEPTION 'profiles.account_type is admin controlled'
                USING ERRCODE = '42501';
        END IF;

        IF NEW.pharmacy_id IS DISTINCT FROM OLD.pharmacy_id THEN
            RAISE EXCEPTION 'profiles.pharmacy_id is admin controlled'
                USING ERRCODE = '42501';
        END IF;

        IF NEW.warehouse_id IS DISTINCT FROM OLD.warehouse_id THEN
            RAISE EXCEPTION 'profiles.warehouse_id is admin controlled'
                USING ERRCODE = '42501';
        END IF;

        IF NEW.is_active IS DISTINCT FROM OLD.is_active THEN
            RAISE EXCEPTION 'profiles.is_active is admin controlled'
                USING ERRCODE = '42501';
        END IF;

        -- Task 3 must add any discovered approval/status/admin-controlled
        -- columns here before implementation approval.
    END IF;

    IF to_jsonb(NEW) ? 'updated_at' THEN
        NEW.updated_at = now();
    END IF;

    RETURN NEW;
END;
$$;
```

Trigger design:

```sql
DROP TRIGGER IF EXISTS prevent_profiles_protected_field_update
ON public.profiles;

CREATE TRIGGER prevent_profiles_protected_field_update
BEFORE UPDATE ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.prevent_profiles_protected_field_update();
```

Important implementation note:

- The `updated_at` snippet is design intent only; PostgreSQL row assignment must be implemented in a schema-aware way.
- If `updated_at` does not exist, omit timestamp mutation.
- If approval/status/admin-controlled columns exist, add explicit `OLD` vs `NEW` comparisons for each.

## 8. DELETE

No authenticated delete policy should be created.

Design:

```sql
-- Intentionally no DELETE policy for authenticated users.
-- Profile deletion/deactivation is an admin/server process.
```

Privilege design:

```sql
REVOKE DELETE ON public.profiles FROM authenticated;
```

Expected behavior:

- Authenticated users cannot delete own profile.
- Authenticated users cannot delete another user's profile.

## 9. Admin/Service Path

The following fields must be assigned only by trusted server/admin/service-role paths:

- `account_type`
- `pharmacy_id`
- `warehouse_id`
- `is_active`
- approval/status/admin-controlled fields
- any additional role/tenant fields discovered later

This phase does not implement:

- admin UI,
- pharmacy approval UI,
- warehouse approval UI,
- role management UI,
- production service automation.

Operational requirement:

- Admin/service tooling must not use normal authenticated client privileges.
- Any trusted function that assigns protected fields must be separately reviewed, use a safe `search_path`, and avoid exposing arbitrary role/tenant assignment to normal clients.

## 10. Grants/Revoke Design

Baseline design:

```sql
REVOKE INSERT ON public.profiles FROM authenticated;
REVOKE UPDATE ON public.profiles FROM authenticated;
REVOKE DELETE ON public.profiles FROM authenticated;

GRANT SELECT ON public.profiles TO authenticated;

GRANT UPDATE (
    full_name,
    phone_number,
    pharmacy_name,
    pharmacy_location,
    warehouse_name,
    warehouse_location
) ON public.profiles TO authenticated;
```

Rationale:

- `SELECT` is safe only with own-row RLS.
- Direct `INSERT` is denied; public-user creation uses the RPC.
- `UPDATE` is limited to safe columns and own-row RLS.
- `DELETE` remains denied.

Task 3 schema note:

- Column-level grants must match actual columns.
- If `warehouse_name` or `warehouse_location` does not exist, omit them.
- If approval/status/admin-controlled fields exist, do not grant them.

## 11. Idempotency Strategy

Use idempotent or repeat-safe patterns where possible:

```sql
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS profiles_select_own ON public.profiles;
CREATE POLICY profiles_select_own ...

DROP POLICY IF EXISTS profiles_update_own_safe_fields ON public.profiles;
CREATE POLICY profiles_update_own_safe_fields ...

CREATE OR REPLACE FUNCTION public.create_public_user_profile(...) ...
CREATE OR REPLACE FUNCTION public.prevent_profiles_protected_field_update() ...

DROP TRIGGER IF EXISTS prevent_profiles_protected_field_update ON public.profiles;
CREATE TRIGGER prevent_profiles_protected_field_update ...

REVOKE ... FROM authenticated;
GRANT ... TO authenticated;
```

Caution:

- `CREATE POLICY` does not support `IF NOT EXISTS`; use `DROP POLICY IF EXISTS` first.
- Column-level grants fail if a column does not exist; Task 3 must confirm schema before final SQL.
- Trigger/function code must be adjusted to actual column names discovered before implementation.

## 12. SupabaseAuthRepository Compatibility

Expected Kotlin behavior for a later approved Task 5:

- `ensureProfileForCurrentUser` must read existing profile first.
- Existing `profiles.account_type` is the source of truth.
- Existing profile bootstrap must not overwrite `account_type`.
- Existing profile bootstrap must not write `is_active`.
- Existing profile bootstrap must not write `pharmacy_id`.
- Existing profile bootstrap must not write `warehouse_id`.
- Signup metadata `account_type` is an untrusted request signal only.
- If a public user profile is missing, bootstrap may call `create_public_user_profile(...)` after approval.
- Sensitive roles must remain pending or fail safely until trusted admin/server assignment exists.

Do not modify Kotlin in Task 2.

## 13. SupabasePharmaRepository.updateProfile Compatibility

The SQL design permits profile edits for display/contact fields only.

Expected safe update fields:

- `full_name`
- `phone_number`
- `pharmacy_name`
- `pharmacy_location`
- `warehouse_name`, if present
- `warehouse_location`, if present

Repository expectations:

- `ProfileUpdateDto` must not include `account_type`.
- `ProfileUpdateDto` must not include `pharmacy_id`.
- `ProfileUpdateDto` must not include `warehouse_id`.
- `ProfileUpdateDto` must not include `is_active`.
- Updates must target the authenticated user's own row under RLS.

Do not modify Kotlin in Task 2.

## 14. Test Expectations

Task 8 should cover at least:

| Test | Expected result |
|---|---|
| Authenticated user reads own profile | Success |
| Authenticated user reads another user's profile | Denied / zero rows |
| Authenticated user creates own public profile through RPC | Success, only if no profile exists |
| Authenticated user directly inserts into `profiles` | Denied |
| Authenticated user self-creates `PHARMACY` profile | Denied |
| Authenticated user self-creates `WAREHOUSE` profile | Denied |
| Authenticated user self-creates `ADMIN` profile | Denied |
| Authenticated user sets `pharmacy_id` during creation | Denied |
| Authenticated user sets `warehouse_id` during creation | Denied |
| Authenticated user updates own `account_type` | Denied |
| Authenticated user updates own `pharmacy_id` | Denied |
| Authenticated user updates own `warehouse_id` | Denied |
| Authenticated user updates own `is_active` | Denied |
| Authenticated user updates approval/status/admin fields | Denied |
| Authenticated user updates own safe display/contact fields | Success |
| Authenticated user updates another user's safe fields | Denied |
| Authenticated user deletes own profile | Denied |
| Authenticated user deletes another user's profile | Denied |

Additional regression:

- Phase 4.5.1 `orders` RPCs must still be able to read trusted profile role/tenant fields.
- `PUBLIC_USER` must not become `PHARMACY` through profile mutation.
- Tenant spoofing through `pharmacy_id` or `warehouse_id` must fail.

## 15. Stop And Approval Gate

Task 3 must not start until this SQL design is reviewed and explicitly approved.

Task 3 must perform or obtain schema confirmation before implementation because complete `profiles` schema is not available locally.

If schema confirmation discovers additional role, tenant, activation, approval, status, or admin-controlled profile fields:

- Add those fields to the protected trigger comparisons.
- Do not grant them to authenticated users.
- Do not allow them in public profile creation input.

If unknown schema blocks a safe migration:

- Mark schema inspection required before Task 3.
- Do not create `database/migrations/20260430_harden_profiles_rls.sql`.

Live verification remains required before production closure because Task 1 had no live policy/grant/trigger evidence.

## 16. Task 2 Decision

Direct insert or RPC/trigger chosen:

- Chosen: `SECURITY DEFINER` RPC for public-user profile creation.
- Direct authenticated `INSERT` is not chosen for this phase.

Protected-field strategy chosen:

- Both column-level `GRANT`/`REVOKE` and a `BEFORE UPDATE` immutability trigger.

Can Task 3 proceed after approval:

- Yes, after explicit approval and schema confirmation.
- If schema confirmation is unavailable, Task 3 must stop before creating the migration.

## 17. Files Modified

Created:

- `.specify/verification/phase-4.5.1b-profiles-sql-design.md`

No other files were intentionally modified.

## 18. No Implementation Changes Confirmation

Confirmed:

- No SQL was executed.
- No migration file was created.
- No migration file was modified.
- No Kotlin was modified.
- No repositories were modified.
- No app code was modified.
- No UI/navigation/domain/model files were modified.
- No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were saved.
