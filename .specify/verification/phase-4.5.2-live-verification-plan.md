# Phase 4.5.2 Live Database Verification Plan

**System**: PharmaNet  
**Phase**: 4.5.2 Live Verification  
**Date**: 2026-04-30  
**Purpose**: Prove or disprove every security assumption from Phase 4.5.1B and the full-system diagnosis.  
**Execution status**: Planned / not executed  
**Live SQL executed by this document**: No  
**Runtime/API tests executed by this document**: No  
**Secrets stored**: No  

## 1. Executive Summary

Phase 4.5.2 is a live verification phase. Its goal is to collect evidence from a dev/staging Supabase project and determine whether the local security assumptions documented in `.specify/diagnosis/full-system-diagnosis.md` are true in the deployed database.

This plan is intentionally evidence-driven:

- No test is considered passed until executed against a live or staging Supabase project.
- Service-role credentials, JWTs, anon keys, and access tokens must never be committed or copied into this repository.
- Production data must not be modified.
- Destructive tests must use isolated disposable test accounts and fixtures only.

Recommended execution mode:

- Start with the manual test matrix for control, auditability, and clean evidence capture.
- Use automation only after the manual path is proven against disposable dev/staging fixtures.

## 2. Test Environment Setup

Required before execution:

- Supabase project URL.
- Service role key for admin inspection, used outside the repository only.
- Anon key for user-context simulation, used outside the repository only.
- Test account credentials or JWTs for:
  - `PUBLIC_USER` with valid profile.
  - `PHARMACY` with trusted `pharmacy_id`.
  - `WAREHOUSE` with trusted `warehouse_id`.
  - `ADMIN`, if the role exists.
- Disposable fixture records for orders, requests, pharmacies, warehouses, medicines, and notifications.

Environment rules:

- Prefer dev/staging.
- Do not execute against production without explicit approval.
- Do not save secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens in repo files, logs, screenshots, or reports.
- Redact user ids only when they are not needed to prove tenant isolation.

## 3. Setup Verification

### 3.1 Confirm Migrations Applied

```sql
SELECT version, name, executed_at
FROM supabase_migrations.schema_migrations
WHERE name LIKE '%profiles%' OR name LIKE '%orders%'
ORDER BY executed_at DESC;
```

Expected:

- `20260430_harden_profiles_rls.sql` is present.
- `20260429_harden_b2c_order_rls.sql` is present.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Evidence:

```text
Not executed.
```

Stop condition:

- Stop if either migration is missing from the target environment.

## 4. Matrix 1: Profiles RLS Verification

### 4.1 RLS Enabled Check

```sql
SELECT schemaname, tablename, rowsecurity
FROM pg_tables
WHERE tablename = 'profiles';
```

Expected:

- `rowsecurity = true`.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Evidence:

```text
Not executed.
```

### 4.2 Policies Existence Check

```sql
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'profiles'
ORDER BY policyname;
```

Expected:

- `profiles_select_own` exists for `SELECT`.
- `profiles_update_own_safe_fields` exists for `UPDATE`.
- No authenticated `INSERT` policy exists.
- No authenticated `DELETE` policy exists.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Actual policies found:

```text
Not executed.
```

### 4.3 Protected Fields Trigger Check

```sql
SELECT trigger_name, event_manipulation, event_object_table, action_statement
FROM information_schema.triggers
WHERE event_object_table = 'profiles'
  AND trigger_name LIKE '%protected%';
```

Expected:

- `prevent_profiles_protected_field_update` trigger exists.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Evidence:

```text
Not executed.
```

### 4.4 RPC Existence Check

```sql
SELECT routine_name, routine_type, security_type, data_type
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name = 'create_public_user_profile';
```

Expected:

- Function exists.
- Security type is documented.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Security type:

```text
Not executed.
```

### 4.5 Schema Hidden Fields Check

```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'profiles'
  AND column_name IN (
    'account_type',
    'pharmacy_id',
    'warehouse_id',
    'is_active',
    'approval_status',
    'admin_notes'
  );
```

Expected:

- All role, tenant, activation, approval, and admin-controlled fields are documented.
- No extra role/tenant/admin fields are left unprotected.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Actual schema:

```text
Not executed.
```

Stop condition:

- Stop if extra unprotected role, tenant, approval, status, or admin-controlled fields are found.

## 5. Matrix 2: Profiles Negative Tests

### 5.1 Cross-User Profile Read

Actor:

- `PUBLIC_USER` user A.

Attempt:

```sql
SELECT *
FROM profiles
WHERE id != auth.uid();
```

Expected:

- Empty result or access error.

Status:

- [ ] Blocked
- [ ] Failed: data leak
- [ ] Not executed

Evidence:

```text
Not executed.
```

### 5.2 Protected Field Update: account_type

Actor:

- `PUBLIC_USER`.

Attempt:

```sql
UPDATE profiles
SET account_type = 'PHARMACY'
WHERE id = auth.uid();
```

Expected:

- Error or no effective mutation.

Status:

- [ ] Blocked
- [ ] Failed: role escalation
- [ ] Not executed

Evidence:

```text
Not executed.
```

### 5.3 Protected Field Update: pharmacy_id

Actor:

- `PUBLIC_USER`.

Attempt:

```sql
UPDATE profiles
SET pharmacy_id = 'pharm_123_not_mine'
WHERE id = auth.uid();
```

Expected:

- Error or no effective mutation.

Status:

- [ ] Blocked
- [ ] Failed: tenant spoofing
- [ ] Not executed

Evidence:

```text
Not executed.
```

### 5.4 Direct Profile Insert Attempt

Actor:

- Authenticated normal user.

Attempt:

```sql
INSERT INTO profiles (
  id,
  account_type,
  pharmacy_id,
  full_name
) VALUES (
  auth.uid(),
  'ADMIN',
  'pharm_evil',
  'Hacker'
);
```

Expected:

- Error due to missing direct insert permission/policy.

Status:

- [ ] Blocked
- [ ] Failed: self-provisioning
- [ ] Not executed

Evidence:

```text
Not executed.
```

### 5.5 Profile Delete Attempt

Actor:

- Authenticated normal user.

Attempt:

```sql
DELETE FROM profiles
WHERE id = auth.uid();
```

Expected:

- Error or no effective deletion.

Status:

- [ ] Blocked
- [ ] Failed: unauthorized delete
- [ ] Not executed

Evidence:

```text
Not executed.
```

## 6. Matrix 3: RPC Behavior Tests

### 6.1 create_public_user_profile Success Case

Actor:

- New authenticated `PUBLIC_USER` with no profile row.

Attempt:

```sql
SELECT create_public_user_profile(
  p_full_name := 'Test User',
  p_phone_number := '+1234567890'
);

SELECT *
FROM profiles
WHERE id = auth.uid();
```

Expected:

- Profile is created for `auth.uid()`.
- `account_type = PUBLIC_USER`.
- `pharmacy_id IS NULL`.
- `warehouse_id IS NULL`.
- Response shape decodes as app `ProfileRowDto`.

Status:

- [ ] Profile created safely
- [ ] Error
- [ ] Not executed

Response shape:

```json
{
  "id": "",
  "account_type": "",
  "pharmacy_id": "",
  "warehouse_id": "",
  "is_active": "",
  "full_name": "",
  "phone_number": ""
}
```

### 6.2 RPC Parameter Injection Test

Actor:

- Authenticated user.

Attempt:

```sql
SELECT create_public_user_profile(
  p_full_name := 'Hacker',
  p_phone_number := '+999',
  p_account_type := 'ADMIN',
  p_pharmacy_id := 'evil_pharm'
);
```

Expected:

- Rejected due to unknown parameters, or extra parameters ignored by the API layer without mutation.
- No role escalation.
- No tenant assignment.

Status:

- [ ] Rejected
- [ ] Failed: parameter injection
- [ ] Not executed

Evidence:

```text
Not executed.
```

### 6.3 RPC Duplicate Call Test

Actor:

- Authenticated `PUBLIC_USER` with existing profile.

Attempt:

```sql
SELECT create_public_user_profile(
  p_full_name := 'Duplicate',
  p_phone_number := '+111'
);
```

Expected:

- Idempotent behavior or safe failure.
- No duplicate profile row.
- No protected-field mutation.

Status:

- [ ] Safe
- [ ] Failed: duplicate or mutation
- [ ] Not executed

Behavior:

```text
Not executed.
```

## 7. Matrix 4: Orders RLS Verification

### 7.1 Orders RLS Enabled

```sql
SELECT rowsecurity
FROM pg_tables
WHERE tablename = 'orders';
```

Expected:

- `true`.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

### 7.2 Orders Policies List

```sql
SELECT policyname, cmd, roles, qual, with_check
FROM pg_policies
WHERE tablename = 'orders'
ORDER BY policyname;
```

Expected:

- B2C policies reflect `20260429_harden_b2c_order_rls.sql`.
- B2B legacy policies are explicitly documented if still active.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Actual policies:

```text
Not executed.
```

### 7.3 B2C Cross-Customer Read

Actor:

- `PUBLIC_USER` user A.

Attempt:

```sql
SELECT *
FROM orders
WHERE customer_id != auth.uid()
  AND order_type = 'CUSTOMER_PHARMACY';
```

Expected:

- Empty result or access error.

Status:

- [ ] Blocked
- [ ] Failed: cross-customer leak
- [ ] Not executed

Rows returned:

```text
Not executed.
```

### 7.4 B2C Order Creation With Spoofed Customer

Actor:

- `PUBLIC_USER` user A.

Attempt:

```sql
INSERT INTO orders (
  order_type,
  customer_id,
  pharmacy_id,
  status,
  medicine_name,
  quantity
) VALUES (
  'CUSTOMER_PHARMACY',
  'user_b_uuid',
  'pharm_123',
  'PENDING',
  'Medicine X',
  10
);
```

Expected:

- Error or RLS denial because `customer_id != auth.uid()`.

Status:

- [ ] Blocked
- [ ] Failed: customer spoofing
- [ ] Not executed

Evidence:

```text
Not executed.
```

### 7.5 B2B Order Policies Check

```sql
SELECT policyname, cmd, roles, qual, with_check
FROM pg_policies
WHERE tablename = 'orders'
  AND (
    policyname ILIKE '%b2b%'
    OR qual ILIKE '%PHARMACY_WAREHOUSE%'
    OR with_check ILIKE '%PHARMACY_WAREHOUSE%'
  )
ORDER BY policyname;
```

Expected:

- Legacy B2B policies are either absent, replaced, or explicitly documented as active gaps.
- Policies verify trusted profile role and tenant where applicable.

Status:

- [ ] Enhanced
- [ ] Old policies active
- [ ] Not executed

Policies found:

```text
Not executed.
```

## 8. Matrix 5: Requests RLS Verification

### 8.1 Requests RLS Status

```sql
SELECT rowsecurity
FROM pg_tables
WHERE tablename = 'requests';
```

Expected from diagnosis:

- Likely missing or unverified. If `false`, this confirms a critical gap.

Status:

- [ ] Enabled
- [ ] Critical: RLS missing
- [ ] Not executed

### 8.2 Requests Policies

```sql
SELECT policyname, cmd, roles, qual, with_check
FROM pg_policies
WHERE tablename = 'requests'
ORDER BY policyname;
```

Expected:

- If RLS is enabled, role/tenant-aware policies exist.
- If empty and RLS is disabled, critical gap confirmed.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Policies found:

```text
Not executed.
```

### 8.3 Cross-Pharmacy Request Read

Actor:

- `PHARMACY` A.

Attempt:

```sql
SELECT *
FROM requests
WHERE pharmacy_id != 'my_pharmacy_id';
```

Expected:

- Empty result or access error.

Status:

- [ ] Blocked
- [ ] Failed: cross-tenant leak
- [ ] Not executed

Rows returned:

```text
Not executed.
```

### 8.4 Request Insert With Spoofed Pharmacy

Actor:

- `PHARMACY` A.

Attempt:

```sql
INSERT INTO requests (
  pharmacy_id,
  warehouse_id,
  medicine_name,
  quantity,
  status
) VALUES (
  'pharmacy_b_id',
  'warehouse_x',
  'Medicine Y',
  100,
  'DRAFT'
);
```

Expected:

- Error or RLS denial.

Status:

- [ ] Blocked
- [ ] Failed: pharmacy spoofing
- [ ] Not executed

Evidence:

```text
Not executed.
```

## 9. Matrix 6: Trigger Verification

### 9.1 Trigger Exists

```sql
SELECT trigger_name, event_manipulation, action_timing
FROM information_schema.triggers
WHERE event_object_table = 'requests'
  AND trigger_name LIKE '%order%';
```

Expected:

- `create_order_from_request` trigger is found.
- Timing is documented.

Status:

- [ ] Found
- [ ] Missing
- [ ] Not executed

Timing:

```text
Not executed.
```

### 9.2 Trigger Behavior Test

Actor:

- Trusted `PHARMACY` test account in dev/staging.

Attempt:

```sql
INSERT INTO requests (
  pharmacy_id,
  warehouse_id,
  medicine_name,
  quantity,
  status
) VALUES (
  'my_pharmacy_id',
  'warehouse_x',
  'Test Medicine',
  50,
  'DRAFT'
) RETURNING id;

SELECT *
FROM orders
WHERE request_id = '<returned_id>';
```

Expected from diagnosis:

- Order may exist with `status = PENDING`, confirming trigger fires on `INSERT`.
- If that happens while request is `DRAFT`, timing issue is confirmed.

Status:

- [ ] Trigger timing confirmed
- [ ] No order created
- [ ] Not executed

Order status:

```text
Not executed.
```

Request status:

```text
Not executed.
```

### 9.3 Trigger Function Security

```sql
SELECT routine_name, security_type
FROM information_schema.routines
WHERE routine_name IN (
  'create_order_from_request',
  'create_order_for_existing_request'
);
```

Expected:

- Security context is documented.
- Helper function exposure is reviewed.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Functions found:

```text
Not executed.
```

## 10. Matrix 7: Admin Role Verification

### 10.1 Admin Profile Existence

```sql
SELECT id, account_type, pharmacy_id, warehouse_id, is_active
FROM profiles
WHERE account_type = 'ADMIN';
```

Expected:

- Admin accounts are documented.
- Provisioning method is known.

Status:

- [ ] Pass
- [ ] Fail
- [ ] Not executed

Admin accounts found:

```text
Not executed.
```

Creation method:

```text
Not executed.
```

### 10.2 Admin RLS Behavior

Actor:

- Authenticated `ADMIN` test account.

Attempt:

```sql
SELECT COUNT(*) FROM orders;
SELECT COUNT(*) FROM requests;
SELECT COUNT(*) FROM profiles WHERE id != auth.uid();
```

Expected:

- Actual admin access model is documented.
- If admin broad access exists, it is justified by explicit RLS/RPC design.

Status:

- [ ] Full access as designed
- [ ] Same as regular user
- [ ] Undefined / risky
- [ ] Not executed

Evidence:

```text
Not executed.
```

## 11. Matrix 8: Miscellaneous Tables

### 11.1 warehouses RLS

```sql
SELECT rowsecurity
FROM pg_tables
WHERE tablename = 'warehouses';
```

Status:

- [ ] Enabled
- [ ] Disabled
- [ ] Not executed

### 11.2 medicines RLS

```sql
SELECT rowsecurity
FROM pg_tables
WHERE tablename = 'medicines';
```

Status:

- [ ] Enabled
- [ ] Disabled
- [ ] Not executed

### 11.3 app_notifications RLS

```sql
SELECT rowsecurity
FROM pg_tables
WHERE tablename = 'app_notifications';
```

Status:

- [ ] Enabled
- [ ] Disabled
- [ ] Not executed

Critical note:

- Disabled RLS is critical if `app_notifications` contains tenant/user-specific data.

## 12. Matrix 9: is_active Enforcement

### 12.1 Deactivated User Access Test

Setup, using trusted admin/service channel in dev/staging only:

```sql
UPDATE profiles
SET is_active = false
WHERE id = 'test_user_id';
```

As that user, attempt:

```sql
-- Example action: create/read own customer orders.
SELECT *
FROM orders
WHERE customer_id = auth.uid();
```

Expected from diagnosis:

- Operations may still succeed unless live RLS/RPC explicitly checks `is_active`.

Status:

- [ ] Blocked
- [ ] Gap confirmed: operations succeed
- [ ] Not executed

Evidence:

```text
Not executed.
```

Cleanup requirement:

- Restore the test user's `is_active` state after execution.

## 13. Final Verification Checklist

Critical assumptions:

- [ ] Profiles RLS working.
- [ ] Profiles protected-field trigger working.
- [ ] Profiles RPC working and response shape compatible.
- [ ] Orders B2C RLS working.
- [ ] Orders B2B RLS enhanced or legacy gap confirmed.
- [ ] Requests RLS enabled or missing gap confirmed.
- [ ] Trigger timing verified.
- [ ] `is_active` enforced or gap confirmed.
- [ ] Admin model defined or gap confirmed.

Evidence collected:

- [ ] SQL result snapshots.
- [ ] Redacted error messages.
- [ ] Exported policy definitions.
- [ ] Schema snapshots.
- [ ] Runtime/API responses where needed.

## 14. Output Report Template

Create after execution:

- `.specify/verification/phase-4.5.2-results.md`

Template:

```markdown
# Phase 4.5.2 Live Verification Results

## Test Environment
- Supabase Project: ___
- Tested: ___
- Tester: ___
- Environment: dev/staging/production-with-approval

## Matrix Results
[Paste all executed matrix results]

## Critical Findings
[List all failures]

## Confirmed Gaps
[Map to diagnosis predictions]

## Production Blockers
[Prioritized list]

## Next Steps
[Reference Phase 4.5.3/4.5.4/4.5.5 as needed]
```

## 15. Execution Safety Rules

Must do:

- Use test accounts only.
- Prefer read-only inspection before mutation tests.
- Use dev/staging unless production is explicitly approved.
- Document every result.
- Stop if unexpected identity, tenant, approval, admin, or status columns are found.
- Redact all credentials and tokens from evidence.

Never do:

- Modify production data without explicit approval.
- Delete production records.
- Disable RLS to test.
- Save service keys, JWTs, anon keys, passwords, access tokens, or refresh tokens.
- Paste secrets into Markdown reports.

## 16. Success Criteria

Phase 4.5.2 succeeds when:

1. Every assumption from `.specify/diagnosis/full-system-diagnosis.md` is proven or disproven.
2. All critical gaps are confirmed or cleared with evidence.
3. Production blockers are complete and prioritized.
4. Next phase scope is clear.

## 17. Recommended Execution Mode

Preferred:

- Manual test matrix first.

Reason:

- This is a high-risk multi-tenant security verification.
- Manual execution gives cleaner evidence, clearer stop points, and less chance of accidental data mutation.

Optional later:

- Codex/agent automation against a disposable dev/staging environment only after credentials and fixture setup are provided outside the repository.
