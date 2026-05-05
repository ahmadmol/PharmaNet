# Phase 4.5.1B Profiles Baseline Audit

**Task**: Task 1 - Audit Current Profiles Schema/RLS/Grants  
**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Scope**: Read-only audit of local `profiles` schema/RLS/grants/triggers/docs and available live evidence.  
**Implementation changes**: None.

## 1. Summary

Local evidence still does not prove `profiles` is safe as an authorization source.

The local repository contains SQL that reads `public.profiles` from `orders` RLS/RPC logic, but it does not contain local SQL enabling RLS on `public.profiles`, defining `profiles` policies, granting/revoking `profiles` privileges, or enforcing immutability for protected profile fields.

No live Supabase/MCP policy evidence was available in this session. MCP resources were checked and returned no resources. No secrets were read, requested, or saved.

Task 1 result:

- Local `profiles` RLS: missing locally.
- Local `profiles` policies: missing locally.
- Local `profiles` grants/revokes: missing locally.
- Local protected-field immutability: not proven locally.
- Live evidence: unknown live.
- Profile trust classification: P0 hardening required, unless later live evidence proves equivalent protections.

## 2. Files Reviewed

Local SQL:

- `database/migrations/20250425_extend_orders_for_b2c.sql`
- `database/migrations/20260429_harden_b2c_order_rls.sql`
- `database/triggers/create_order_from_request.sql`

Local docs and verification:

- `auth_debug_guide.md`
- `database/README_OrderCreationRefactor.md`
- `.specify/verification/phase-4.5.1-profiles-trust.md`
- `.specify/verification/phase-4.5-rls-audit.md`
- `.specify/verification/phase-4.5.1-policy-baseline.md`
- `.specify/verification/phase-4.5.1-rpc-repository-compatibility.md`
- `.specify/verification/phase-4.5.1-rpc-repository-integration.md`
- `.specify/memory/phase-4.5-public-user-rls-real-data-verification-spec.md`
- `.specify/memory/phase-4.5.1-rls-security-fixes-spec.md`
- `.specify/memory/phase-4.5.1b-profiles-trust-hardening-spec.md`

App write-path references used only to understand baseline risk:

- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`

## 3. Local Profiles RLS Checks

| Required check | Local result | Evidence |
|---|---|---|
| `ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY` | Not found | Local migrations only enable RLS on `orders`. |
| `ALTER TABLE profiles ENABLE ROW LEVEL SECURITY` | Not found | No unqualified `profiles` RLS enablement found. |
| `FORCE ROW LEVEL SECURITY` on `profiles` | Not found | No profiles RLS DDL found. |
| `CREATE POLICY` on `public.profiles` | Not found | Local policy SQL targets `orders`, not `profiles`. |
| `DROP POLICY` on `public.profiles` | Not found | Local drop policy SQL targets `public.orders`, not `profiles`. |
| `profiles` SELECT policy | Not found | No local `profiles` policy exists. |
| `profiles` INSERT policy | Not found | No local `profiles` policy exists. |
| `profiles` UPDATE policy | Not found | No local `profiles` policy exists. |
| `profiles` DELETE policy | Not found | No local `profiles` policy exists. |

Local conclusion:

- `profiles` RLS cannot be proven enabled from local SQL.
- No local row-level policies prove safe profile reads, inserts, updates, or deletes.

## 4. Local Profiles Grants/Revoke Checks

| Required check | Local result | Evidence |
|---|---|---|
| `GRANT ... ON public.profiles` | Not found | No local grant found for `public.profiles`. |
| `GRANT ... ON profiles` | Not found | No local grant found for unqualified `profiles`. |
| `REVOKE ... ON public.profiles` | Not found | No local revoke found for `public.profiles`. |
| Column-level safe update grants | Not found | No local grant restricting updates to safe display/contact fields. |
| Broad authenticated write revoke | Not found | No local revoke protecting profile writes from `authenticated`. |

Local conclusion:

- No local privilege SQL proves normal authenticated clients are limited to safe profile columns.
- No local privilege SQL proves protected fields are denied at the grant layer.

## 5. Local Trigger/Function Checks

### Database triggers

Local trigger file reviewed:

- `database/triggers/create_order_from_request.sql`

Findings:

- Defines `create_order_from_request()` and `create_order_for_existing_request(request_id_param TEXT)`.
- Creates trigger `create_order_from_request` on `requests`.
- Writes `orders` and updates `requests`.
- Copies `pharmacy_id`, `warehouse_id`, and `status` from `requests` into `orders`.
- Does not target `profiles`.
- Does not write `profiles.account_type`, `profiles.pharmacy_id`, `profiles.warehouse_id`, or `profiles.is_active`.

### Migration functions

`database/migrations/20260429_harden_b2c_order_rls.sql` defines order lifecycle RPCs:

- `public.cancel_customer_order(text)`
- `public.confirm_customer_order(text, bigint)`
- `public.reject_customer_order(text)`
- `public.mark_customer_order_ready_for_pickup(text)`
- `public.mark_customer_order_out_for_delivery(text)`
- `public.mark_customer_order_delivered(text)`

These functions read `public.profiles.account_type` and `public.profiles.pharmacy_id` as authorization inputs. They do not protect or mutate `profiles`.

Local conclusion:

- No local trigger/function protects `profiles` protected fields.
- No local trigger/function creates profiles safely from a whitelist.
- Existing order RPCs deepen the dependency on trusted profile role/tenant fields.

## 6. Protected Field Evidence

Protected fields required by Phase 4.5.1B:

- `account_type`
- `pharmacy_id`
- `warehouse_id`
- `is_active`
- approval/status/admin-controlled fields, if present

| Field | Local app/database evidence | Local protection evidence | Result |
|---|---|---|---|
| `account_type` | Read by order hardening RPCs and written by `SupabaseAuthRepository.ProfileUpsertDto`. | No local profiles RLS/policy/grant/trigger protection found. | Not proven safe locally. |
| `pharmacy_id` | Read by order policies/RPCs for pharmacy authorization. | No local profiles RLS/policy/grant/trigger protection found. | Not proven safe locally. |
| `warehouse_id` | Read by order policies for warehouse authorization. | No local profiles RLS/policy/grant/trigger protection found. | Not proven safe locally. |
| `is_active` | Written by `SupabaseAuthRepository.ProfileUpsertDto`. | No local profiles RLS/policy/grant/trigger protection found. | Not proven safe locally. |
| approval/status/admin-controlled fields | No complete local `profiles` schema was found, so additional profile approval/status columns cannot be enumerated from local SQL. | No local protection found. | Unknown locally; must be checked before Task 2 design. |

Additional schema note:

- No local `CREATE TABLE public.profiles` or equivalent schema definition was found in reviewed SQL.
- Because the complete `profiles` schema is not present locally, Task 2 must allow for extra protected profile columns discovered from live/schema evidence.

## 7. Local Documentation Evidence

`auth_debug_guide.md` contains a troubleshooting reference:

- `Permission denied` may be caused by an RLS policy blocking writes.
- The suggested check references `users_insert_own_profile`.
- The guide also claims RLS is fully enabled with appropriate security policies.

Assessment:

- This is documentation-only evidence.
- No matching local SQL for `users_insert_own_profile` was found.
- The documentation claim is ambiguous and cannot prove actual local or live policy state.

Existing verification docs already warned:

- `.specify/verification/phase-4.5.1-profiles-trust.md` classifies local profile trust as not proven.
- `.specify/verification/phase-4.5-rls-audit.md` states no local `CREATE POLICY` definitions were found for `profiles`.

## 8. Live Evidence

Live/MCP check:

- MCP resources were checked with `list_mcp_resources`.
- Result: no resources exposed.

No live Supabase Dashboard SQL output, live policy list, grant list, trigger list, or table schema was available in this task.

No secrets were requested or saved:

- No service-role key.
- No JWT.
- No API key.
- No password.
- No access token or refresh token.

| Live check | Result | Classification |
|---|---|---|
| Live `profiles` RLS enabled | Not available | Unknown live |
| Live `profiles` SELECT policy | Not available | Unknown live |
| Live `profiles` INSERT policy | Not available | Unknown live |
| Live `profiles` UPDATE policy | Not available | Unknown live |
| Live `profiles` DELETE policy | Not available | Unknown live |
| Live grants/revokes on `profiles` | Not available | Unknown live |
| Live triggers/functions protecting `profiles` | Not available | Unknown live |
| Live protected-field immutability | Not available | Unknown live |

Stop-condition assessment:

- No live evidence contradicted local risk assumptions.
- No live evidence proved equivalent protections.
- Live access did not require saving secrets because no live resources were exposed.

## 9. Required Check Answers

| Question | Answer |
|---|---|
| Does local SQL enable RLS on `public.profiles`? | No. |
| Are local `profiles` SELECT policies present? | No. |
| Are local `profiles` INSERT policies present? | No. |
| Are local `profiles` UPDATE policies present? | No. |
| Are local `profiles` DELETE policies present? | No. |
| Are local GRANT/REVOKE statements on `public.profiles` present? | No. |
| Are local triggers/functions writing protected profile fields present? | No. |
| Is there local protection for `account_type`? | No local proof. |
| Is there local protection for `pharmacy_id`? | No local proof. |
| Is there local protection for `warehouse_id`? | No local proof. |
| Is there local protection for `is_active`? | No local proof. |
| Are extra approval/status/admin-controlled profile fields known locally? | Unknown; no complete local `profiles` schema found. |
| Is live evidence available? | No. |

## 10. Trust Classification

| Area | Classification | Reason |
|---|---|---|
| Local `profiles` RLS | Missing locally | No local RLS enablement found for `profiles`. |
| Local `profiles` policies | Missing locally | No local `profiles` policy SQL found. |
| Local `profiles` grants | Missing locally | No local `profiles` grant/revoke SQL found. |
| Local protected-field immutability | P0 hardening required | No local RLS/grant/trigger proof blocks mutation. |
| Live `profiles` RLS/policies/grants/triggers | Unknown live | No live resources/evidence available without secrets. |
| Overall profile trust | P0 hardening required | `orders` RLS/RPC trusts profile fields that are not proven immutable. |

Profile trust classification:

- Missing locally.
- Unknown live.
- P0 hardening required unless later live evidence proves equivalent protection.

## 11. Can Task 2 Proceed?

Task 2 can proceed: yes.

Reason:

- No live contradiction was found.
- No live equivalent protection was proven.
- Local evidence supports the risk assumptions in the Phase 4.5.1B spec.
- Task 2 should design the profiles SQL migration with an explicit check for any extra protected profile columns if schema evidence becomes available.

Task 2 warning:

- Because the complete `profiles` schema is not present locally, Task 2 should include a schema inspection prerequisite or defensive protected-field strategy before final migration implementation.

## 12. Files Modified

Created:

- `.specify/verification/phase-4.5.1b-profiles-baseline.md`

No other files were intentionally modified.

## 13. No Implementation Changes Confirmation

Confirmed:

- No SQL was executed.
- No migration was created.
- No migration was modified.
- No Kotlin was modified.
- No repositories were modified.
- No app code was modified.
- No UI/navigation/domain/model files were modified.
- No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were saved.
