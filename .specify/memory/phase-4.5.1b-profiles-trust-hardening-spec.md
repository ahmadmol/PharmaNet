# Feature Specification: Phase 4.5.1B - Profiles Trust Hardening

**Feature Branch**: `phase-4.5.1-rls-security-fixes`  
**Created**: 2026-04-30  
**Status**: Draft  
**Input**: User description: "Phase 4.5.1B: Profiles Trust Hardening"  
**Implementation changes in this step**: None

## 1. Executive Summary

Phase 4.5.1 Task 7 found a P0 closure blocker: `profiles` is not locally proven safe, while the hardened `orders` RLS/RPC design trusts `profiles.account_type`, `profiles.pharmacy_id`, and `profiles.warehouse_id`.

This phase designs the minimal security hardening required to make profile role and tenant fields immutable from normal authenticated clients.

Final design decision:

- Normal authenticated clients may read their own profile.
- Normal authenticated clients may update only safe display/contact fields on their own row.
- Normal authenticated clients must not mutate `account_type`, `pharmacy_id`, `warehouse_id`, `is_active`, approval status, or any admin-controlled fields.
- Role and tenant assignment must be server/admin controlled.
- Client metadata may be treated as a signup request signal only, not as trusted authorization state.
- `SupabaseAuthRepository.ensureProfileForCurrentUser` must stop upserting admin-controlled fields on existing profiles and should no longer be the authority for `account_type`.

Phase 4.5.1 can continue only after this hardening is implemented and verified, or after equivalent live policies are proven with evidence. Until then, `profiles` remains a P0 trust blocker.

## 2. Current Risk

Current Phase 4.5.1 `orders` hardening depends on trusted profile fields:

- `profiles.account_type` gates `PUBLIC_USER` and `PHARMACY` operations.
- `profiles.pharmacy_id` scopes pharmacy-owned B2C orders.
- `profiles.warehouse_id` scopes warehouse-owned B2B access.

Task 7 local evidence showed:

- No local `ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY` was found.
- No local `profiles` policies were found.
- Live policies were unavailable.
- `SupabaseAuthRepository.ensureProfileForCurrentUser` performs an authenticated `upsert` to `profiles`.
- That upsert writes `account_type` and `is_active`.

If normal authenticated users can update profile trust fields, they may be able to undermine `orders` RLS/RPC authorization. P0 scenarios that must be impossible:

- A `PUBLIC_USER` changes `account_type` to `PHARMACY`.
- A `PHARMACY` changes `pharmacy_id` to another tenant.
- A `WAREHOUSE` changes `warehouse_id` to another tenant.
- Any user changes `is_active` or approval fields to bypass review.

This spec does not prove that production is exploitable. It states that the repository cannot close Phase 4.5.1 while the trust boundary is unproven.

## 3. App-Side Write Path Analysis

### `SupabaseAuthRepository.ensureProfileForCurrentUser`

File:

- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`

Current write behavior:

- Builds `ProfileUpsertDto`.
- Calls `supabase.postgrest["profiles"].upsert(profilePayload) { ignoreDuplicates = false }`.
- Writes:
  - `id`
  - `phone_number`
  - `full_name`
  - `account_type`
  - `pharmacy_name`
  - `pharmacy_location`
  - `warehouse_name`
  - `warehouse_location`
  - `is_active`

Risk:

- This path writes `account_type` and `is_active` as a normal authenticated client.
- `account_type` is derived from app/auth user metadata, which is not sufficient as a database authorization source.
- With `ignoreDuplicates = false`, an existing row may be updated, not only created.

Required repository decision:

- This path must stop updating `account_type` on existing profiles.
- This path must stop updating `is_active`.
- This path must not set `pharmacy_id` or `warehouse_id`.
- The returned session snapshot must continue reading persisted `profiles.account_type` from the database after profile creation or lookup.

### `SupabasePharmaRepository.updateProfile`

File:

- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`

Current write behavior:

- Updates `profiles` through `ProfileUpdateDto`.
- Writes:
  - `full_name`
  - `pharmacy_name`
  - `pharmacy_location`
  - `phone_number`
- Filters by `id = profile.id`.

Risk:

- The DTO is locally limited to display/contact fields.
- It still requires RLS to ensure users can update only their own row.
- It should not be expanded to include role, tenant, activation, or approval fields.

## 4. Final Decision For `account_type` Handling

Normal authenticated users must not be allowed to mutate `account_type`.

Decision answers:

1. Client signup metadata may include requested `account_type` for UX and onboarding routing, but metadata must not be treated as trusted authorization.
2. `profiles.account_type` must be assigned by trusted server/admin code.
3. If self-service `PUBLIC_USER` signup remains required, the database may allow creation of a self-owned profile with `account_type = 'PUBLIC_USER'` only.
4. `PHARMACY`, `WAREHOUSE`, and `ADMIN` roles must not be self-assigned by normal authenticated clients.
5. `account_type` must be immutable to normal authenticated clients after profile creation.

Recommended minimal behavior:

- For self-service public users: allow profile creation with `account_type = 'PUBLIC_USER'`, if the product requires immediate public-user access.
- For sensitive roles: create a pending profile row with safe display/contact fields only, then assign `account_type`, tenant linkage, activation, and approval using service-role/admin tooling.
- If the database cannot distinguish safe self-service public signup from sensitive signup, move all profile creation to a trusted trigger/RPC and deny direct client inserts.

## 5. Final Decision For `pharmacy_id` And `warehouse_id` Handling

`pharmacy_id` and `warehouse_id` must be server/admin controlled only.

Normal authenticated users must not:

- Insert a non-null `pharmacy_id`.
- Insert a non-null `warehouse_id`.
- Update `pharmacy_id`.
- Update `warehouse_id`.
- Clear tenant linkage.
- Move themselves to another tenant.

Rationale:

- Tenant IDs are authorization inputs for order visibility and lifecycle RPCs.
- Tenant linkage is not display data.
- Any client-controlled tenant ID would make role/tenant RLS unsafe.

For `PUBLIC_USER`, both fields should normally be `NULL`.

For `PHARMACY`, `pharmacy_id` must be assigned by trusted admin/server code.

For `WAREHOUSE`, `warehouse_id` must be assigned by trusted admin/server code. The existing app has compatibility fallback logic from `pharmacy_id` to `warehouse_id`; this should remain a repository compatibility detail, not a reason to let clients write either field.

## 6. Proposed RLS Policies

The implementation phase should add a new profiles migration, for example:

`database/migrations/20260430_harden_profiles_rls.sql`

This section describes policy intent. It is not implemented in this specify step.

### Baseline

Required:

```sql
alter table public.profiles enable row level security;
```

Recommended:

```sql
alter table public.profiles force row level security;
```

Only use `FORCE ROW LEVEL SECURITY` if it does not break known trusted maintenance flows. Service-role/admin operations should use explicit trusted privileges.

### SELECT

Minimal required policy:

```sql
create policy profiles_select_own
on public.profiles
for select
to authenticated
using (id = auth.uid());
```

Role-based broader reads should not be added unless an implementation task proves a concrete app requirement.

If broader reads are required later:

- `PHARMACY` may read only safe contact/display fields for users belonging to its own tenant, if needed.
- `WAREHOUSE` may read only safe contact/display fields for users belonging to its own tenant, if needed.
- Sensitive fields should not be exposed broadly unless they are required for backend authorization and the reader is trusted.

Because PostgreSQL RLS controls rows rather than columns, avoiding sensitive-field leaks may require:

- narrower repository `select` column lists,
- views exposing safe profile columns,
- or RPCs for tenant-scoped lookup.

### INSERT

Preferred minimal direct insert policy, only if self-service profile creation is required:

```sql
create policy profiles_insert_own_public_user
on public.profiles
for insert
to authenticated
with check (
  id = auth.uid()
  and account_type = 'PUBLIC_USER'
  and pharmacy_id is null
  and warehouse_id is null
  and coalesce(is_active, true) = true
);
```

Important implementation note:

- This policy alone does not restrict which columns are provided.
- Column-level privileges, a trigger, or a safe creation RPC is required if the table has approval/status/admin-controlled fields beyond the listed checks.

Safer alternative:

- Deny direct client inserts.
- Use a `SECURITY DEFINER` RPC or auth-user trigger to create profile rows from a whitelist.
- The trusted function decides `account_type`, defaults `is_active`, and leaves tenant IDs null until admin assignment.

Recommended decision:

- Use a safe creation RPC or trigger if sensitive-role signup remains in the app.
- Direct insert may be acceptable only for `PUBLIC_USER` rows with strict checks and column privileges.

### UPDATE

Normal authenticated users may update only safe display/contact fields on their own row:

- `full_name`
- `phone_number`
- `pharmacy_name`
- `pharmacy_location`
- `warehouse_name`
- `warehouse_location`

Normal authenticated users must not update:

- `account_type`
- `pharmacy_id`
- `warehouse_id`
- `is_active`
- approval/status/admin-controlled fields
- audit fields unless managed by triggers

Recommended SQL strategy:

1. Revoke broad table update from `authenticated`.
2. Grant column-level update only on safe fields.
3. Add an own-row update policy.

Policy shape:

```sql
create policy profiles_update_own_safe_fields
on public.profiles
for update
to authenticated
using (id = auth.uid())
with check (id = auth.uid());
```

Privilege shape:

```sql
revoke update on public.profiles from authenticated;
grant update (
  full_name,
  phone_number,
  pharmacy_name,
  pharmacy_location,
  warehouse_name,
  warehouse_location
) on public.profiles to authenticated;
```

If the existing grants cannot safely be reasoned about, add a defensive `BEFORE UPDATE` trigger that raises an exception when a non-admin caller changes protected fields:

- `account_type`
- `pharmacy_id`
- `warehouse_id`
- `is_active`
- approval/status/admin-controlled fields

The trigger is recommended because RLS cannot compare all old and new column values cleanly, and column grants can drift.

### DELETE

No normal authenticated delete policy should be created.

Expected behavior:

- Users cannot delete profile rows.
- Deactivation, suspension, and deletion are admin/server processes outside this phase.

## 7. Proposed SQL Migration Strategy

The implementation phase should create a migration with this sequence:

1. Inspect existing live `profiles` grants, RLS state, policies, triggers, and dependent views/functions before applying changes.
2. Enable RLS on `public.profiles`.
3. Drop or replace unsafe `profiles` policies, if present.
4. Revoke broad client write privileges from `authenticated`, especially broad `UPDATE`.
5. Grant authenticated `SELECT` only as needed.
6. Grant authenticated column-level `UPDATE` only for safe display/contact fields.
7. Add `profiles_select_own`.
8. Add own-row safe update policy.
9. Decide and implement one safe profile creation path:
   - direct insert for self-owned `PUBLIC_USER` only, with strict `WITH CHECK` and protected columns, or
   - trigger/RPC creation for all profiles.
10. Add a protected-field immutability trigger if column grants or future drift could allow sensitive-field updates.
11. Ensure admin/service-role paths can assign:
   - `account_type`
   - `pharmacy_id`
   - `warehouse_id`
   - `is_active`
   - approval/status fields
12. Add comments in the migration explaining that `profiles` is an authorization source for `orders` RLS/RPC.
13. Add verification SQL/manual test notes in `.specify/verification` during implementation.

No live SQL should be executed during this specify step.

## 8. Required Repository Changes

Repository changes are required during implementation.

### `SupabaseAuthRepository.ensureProfileForCurrentUser`

Must change:

- Stop authenticated upsert from writing `account_type` to existing rows.
- Stop authenticated upsert from writing `is_active`.
- Prefer fetch-first behavior:
  - Fetch the existing profile.
  - If it exists, use persisted `account_type`, `pharmacy_id`, `warehouse_id`, and `is_active`.
  - Update or create only allowed display/contact fields through the approved safe path.
- If the profile does not exist, create it through the approved creation path:
  - safe RPC/trigger, or
  - direct insert for self-owned `PUBLIC_USER` only.

Must not do:

- Do not trust app-side `User.accountType` as database role truth.
- Do not let metadata overwrite persisted `profiles.account_type`.
- Do not infer tenant linkage from user-editable display fields.

### Signup Metadata

Signup metadata may keep `account_type` as an onboarding request signal, but the app must treat the database profile row as authoritative after login/bootstrap.

Sensitive roles:

- `PHARMACY`, `WAREHOUSE`, and `ADMIN` signup should remain pending/manual until trusted assignment exists.
- The repository should surface missing linkage as a bootstrap failure for sensitive roles, as it already does for missing `pharmacy_id` in the `PHARMACY` path.

### `SupabasePharmaRepository.updateProfile`

No DTO expansion is required.

Required implementation guard:

- Keep `ProfileUpdateDto` limited to safe display/contact fields.
- Ensure the update target is the current user's own row, either by using `auth.uid()` server-side/RLS or by ensuring `profile.id` comes from the trusted current snapshot.
- Expect RLS to deny any attempt to update another user's row.

## 9. Test Matrix

### RLS And Privilege Tests

| Actor | Attempt | Expected |
|---|---|---|
| Anonymous | Select from `profiles` | Denied / zero rows |
| Authenticated user A | Select own profile | Success |
| Authenticated user A | Select user B profile | Denied / zero rows |
| Authenticated user A | Insert profile with `id != auth.uid()` | Denied |
| Authenticated user A | Insert self profile as `PUBLIC_USER` with null tenant IDs, if direct insert is allowed | Success |
| Authenticated user A | Insert self profile as `PHARMACY` | Denied |
| Authenticated user A | Insert self profile as `WAREHOUSE` | Denied |
| Authenticated user A | Insert self profile as `ADMIN` | Denied |
| Authenticated user A | Insert or update non-null `pharmacy_id` | Denied |
| Authenticated user A | Insert or update non-null `warehouse_id` | Denied |
| Authenticated user A | Update own `full_name` | Success |
| Authenticated user A | Update own `phone_number` | Success |
| Authenticated user A | Update own display organization fields | Success |
| Authenticated user A | Update another user's safe fields | Denied |
| Authenticated user A | Update own `account_type` | Denied |
| Authenticated user A | Update own `is_active` | Denied |
| Authenticated user A | Update approval/status/admin-controlled fields | Denied |
| Authenticated user A | Delete own profile | Denied |
| Authenticated user A | Delete another profile | Denied |

### Repository Tests

| Path | Scenario | Expected |
|---|---|---|
| `ensureProfileForCurrentUser` | Existing profile has `account_type = PUBLIC_USER`, metadata says `PHARMACY` | Snapshot uses persisted `PUBLIC_USER`; database role is unchanged |
| `ensureProfileForCurrentUser` | Existing profile has `is_active = false`, app user has `isActive = true` | Database `is_active` remains false |
| `ensureProfileForCurrentUser` | Existing pharmacy profile lacks `pharmacy_id` | Bootstrap fails with linkage error |
| `ensureProfileForCurrentUser` | New public user with approved creation path | Own profile is created safely |
| `ensureProfileForCurrentUser` | New sensitive-role user without server/admin assignment | No trusted role escalation; bootstrap remains pending/fails safely |
| `updateProfile` | Own safe display/contact update | Success |
| `updateProfile` | Cross-user update attempt | Denied by RLS |

### Phase 4.5.1 Regression Tests

After profiles hardening, rerun the Phase 4.5.1 `orders` tests:

- `PUBLIC_USER` can create own B2C order.
- Non-`PUBLIC_USER` cannot create B2C order.
- Public users can cancel only through approved RPC.
- Pharmacies can run only own-tenant lifecycle RPCs.
- Warehouse cannot access B2C rows.
- Role changes attempted through profile mutation do not alter order access.

## 10. Rollback Strategy

Rollback must be treated carefully because the previous state is a P0 trust blocker.

Emergency rollback steps:

1. Restore the previous `profiles` policies and grants only if needed to recover production functionality.
2. Drop new profile immutability trigger or creation RPC only if it is the cause of the incident.
3. Reopen Phase 4.5.1B immediately.
4. Mark `orders` RLS/RPC trust as unproven again.
5. Re-run profiles and orders negative tests before reclosing.

Preferred operational rollback:

- Keep sensitive-field immutability in place.
- Temporarily relax only safe display/contact update behavior if a UI regression occurs.
- Do not re-enable normal authenticated mutation of `account_type`, `pharmacy_id`, `warehouse_id`, `is_active`, or approval fields.

## 11. Acceptance Criteria

This design is accepted when all required decisions are explicit:

- Can user mutate `account_type`? No.
- Can user mutate `pharmacy_id` or `warehouse_id`? No.
- Can user mutate `is_active`? No.
- Can user update safe profile fields? Yes, own row only.
- Should normal authenticated users insert their own profile row? Yes only for self-owned `PUBLIC_USER` if direct insert is retained; otherwise use trusted RPC/trigger.
- Which fields may be inserted by the client? Only self-owned identity plus safe display/contact fields, with `account_type = PUBLIC_USER`, null tenant IDs, and no admin/approval fields if direct insert is allowed.
- Should `account_type` be accepted from client metadata during signup? Only as an untrusted request signal, not authorization truth.
- Should `account_type` become immutable after creation? Yes for normal authenticated clients.
- Should `pharmacy_id` and `warehouse_id` be admin/server controlled only? Yes.
- Should `is_active` be admin/server controlled only? Yes.
- Should `ensureProfileForCurrentUser` stop updating `account_type` on existing profiles? Yes.
- Is a trigger or RPC required for safe profile creation? Recommended; required if sensitive-role signup remains in the app or if column privileges cannot fully protect admin fields.
- What RLS policies are needed? Own-row select, safe own-row update, optional self-owned public-user insert or trusted creation RPC/trigger, and no delete policy.
- What repository changes are required? `ensureProfileForCurrentUser` must stop writing trusted/admin fields and must use persisted profile role as authority; `updateProfile` must remain safe-field only.
- Can Phase 4.5.1 continue after this hardening? Yes, after implementation and verification prove profile role/tenant immutability. Until then, Phase 4.5.1 cannot close.

## Non-Goals

- No UI changes.
- No navigation changes.
- No `Order` or `Request` model changes.
- No `CUSTOMER_PHARMACY` flow changes.
- No `PHARMACY`, `WAREHOUSE`, or `ADMIN` UI work.
- No live SQL execution during specify.
- No secrets in repo.
- No implementation SQL or app code in this step.
