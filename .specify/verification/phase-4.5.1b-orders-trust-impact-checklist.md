# Phase 4.5.1B Orders Trust Impact Checklist

**Task**: Task 9 - Orders Trust Impact Checklist  
**Phase**: Phase 4.5.1B Profiles Trust Hardening  
**Date**: 2026-04-30  
**Scope**: Documentation/checklist only.  
**Live SQL executed**: No  
**Runtime order behavior verified**: No

## 1. Summary

Phase 4.5.1B reduces the main trust risk for `orders` and `requests` flows by making normal authenticated clients unable to mutate the profile role and tenant fields that order RLS/RPC logic trusts.

Local evidence shows:

- B2C order insert policy and B2C lifecycle RPCs depend on `profiles.account_type`.
- B2C pharmacy lifecycle RPCs depend on `profiles.pharmacy_id`.
- Existing B2B order policies depend on `profiles.pharmacy_id` and `profiles.warehouse_id`.
- Orders/request code does not appear to depend on `profiles.is_active` locally.
- `SupabaseAuthRepository` now reads persisted profile rows first and no longer upserts `account_type` or `is_active`.
- `SupabasePharmaRepository.updateProfile` remains safe-field only.

Remaining risk is not fully closed until live verification proves the profiles migration is applied and effective, and until remaining orders/requests RLS behavior is verified in the target environment.

## 2. Files Reviewed

Phase 4.5.1B documents:

- `.specify/memory/phase-4.5.1b-profiles-trust-hardening-spec.md`
- `.specify/tasks/phase-4.5.1b-profiles-trust-hardening/tasks.md`
- `.specify/verification/phase-4.5.1b-profiles-baseline.md`
- `.specify/verification/phase-4.5.1b-profiles-sql-design.md`
- `.specify/verification/phase-4.5.1b-profiles-migration-implementation.md`
- `.specify/verification/phase-4.5.1b-auth-bootstrap-audit.md`
- `.specify/verification/phase-4.5.1b-auth-bootstrap-update.md`
- `.specify/verification/phase-4.5.1b-update-profile-safety.md`
- `.specify/verification/phase-4.5.1b-compile-smoke.md`
- `.specify/verification/phase-4.5.1b-profiles-negative-test-plan.md`

Source and SQL reviewed read-only:

- `core/common/src/main/kotlin/com/pharmalink/core/repository/SupabaseAuthRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/PharmaRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/domain/model/Order.kt`
- `core/common/src/main/kotlin/com/pharmalink/domain/model/Request.kt`
- `core/common/src/main/kotlin/com/pharmalink/domain/mapper/UserIdentityMapper.kt`
- `feature/orders/src/main/kotlin/com/pharmalink/feature/orders/usecase/*.kt`
- `feature/orders/src/main/kotlin/com/pharmalink/feature/orders/*ViewModel.kt`
- `feature/request/src/main/kotlin/com/pharmalink/feature/request/*ViewModel.kt`
- `feature/request/src/main/kotlin/com/pharmalink/feature/request/usecase/*.kt`
- `database/migrations/20250425_extend_orders_for_b2c.sql`
- `database/migrations/20260429_harden_b2c_order_rls.sql`
- `database/migrations/20260430_harden_profiles_rls.sql`
- `database/triggers/create_order_from_request.sql`

## 3. Profile Hardening Impact

| Trusted profile field | Orders/requests dependency | Local evidence | Phase 4.5.1B impact |
|---|---|---|---|
| `profiles.account_type` | B2C order creation requires `PUBLIC_USER`; cancellation requires `PUBLIC_USER`; pharmacy lifecycle RPCs require `PHARMACY`. | `20260429_harden_b2c_order_rls.sql` `customer_create_order`, `cancel_customer_order`, `confirm_customer_order`, `reject_customer_order`, `mark_customer_order_*`. | Reduced client-side trust: normal clients can no longer mutate `account_type`, and auth bootstrap no longer writes it. |
| `profiles.pharmacy_id` | Pharmacy B2C lifecycle RPCs scope orders to the trusted pharmacy; B2B policies scope pharmacy order/request access. | `20260429_harden_b2c_order_rls.sql`; `20250425_extend_orders_for_b2c.sql`; repository filters via `UserIdentity.organizationId`. | Reduced client-side trust: normal clients can no longer mutate `pharmacy_id`; snapshots derive it from persisted profile rows. |
| `profiles.warehouse_id` | B2B warehouse order policies and warehouse request reads use warehouse linkage. | `20250425_extend_orders_for_b2c.sql`; repository warehouse filters; `UserIdentityMapper` warehouse fallback. | Reduced client-side trust: normal clients can no longer mutate `warehouse_id`; snapshots derive it from persisted profile rows. |
| `profiles.is_active` | No local orders/requests policy or repository flow was found to enforce `is_active`. | Local `rg` review of migrations/repository code found protection in profiles migration, but no order/request check. | Mutation risk reduced, but orders/requests do not currently appear to gate access on `is_active`. Live/product decision needed if inactive users must be blocked from order flows. |

Answer:

- Phase 4.5.1B materially reduces client-side trust in profile role/tenant fields.
- It does not by itself prove all orders/requests behavior in production, because live RLS/RPC verification remains required.

## 4. Client-Side Trust Risk Review

| Risk question | Local finding | Risk | Recommended follow-up |
|---|---|---|---|
| Does client code accept role identity from UI state? | Feature use cases receive `accountType` from ViewModel/session state, but repository re-resolves `UserSnapshot` through `resolveAccessContext()` for critical writes. | Low after repository validation. | Keep repository/server validation as source of truth; do not rely on UI-only role checks. |
| Does client code accept `pharmacy_id` from user-controlled input? | B2C `createCustomerOrder` receives `pharmacyId` from selected pharmacy UI state. Repository validates caller is `PUBLIC_USER` and customer id, but target pharmacy id is client-selected. | Medium. | Verify order insert RLS/FK/catalog constraints ensure `pharmacy_id` is valid, active, and allowed as a B2C target. |
| Does client code accept `warehouse_id` from user-controlled input? | `CreateRequestViewModel` selects a warehouse from observed warehouse options; `createRequest` writes that `warehouseId`. | Medium. | Verify request insert policies/FKs ensure the selected warehouse is valid and visible to the pharmacy. |
| Are orders filtered only client-side? | Repository applies role-aware filters for `orders` and `requests`; RLS is still required as source of truth. | Low/Medium. | Live RLS tests must prove cross-tenant reads are denied even if repository filters are bypassed. |
| Are role/tenant/status fields written directly from client DTOs? | `CreateOrderDto` writes B2C order fields including `pharmacy_id`, `customer_id`, `order_type`, `status`; `RequestInsertDto` writes `pharmacy_id`, `warehouse_id`, `status`; `RequestUpdateDto` can write `status` and `warehouse_id`. | Medium. | Keep DB policies/RPCs as final authority. Review request RLS separately because Phase 4.5.1B only hardens profiles. |
| Does code trust auth metadata over persisted profile row? | `SupabaseAuthRepository` now uses persisted profile row for snapshot role/tenant after bootstrap. Metadata remains a request signal only for missing-profile decisions. | Low. | Add repository regression tests from Task 8. |
| Does Android manually create orders when request trigger should create them? | Generic `createOrder` is unsupported. B2B request flow inserts into `requests`; DB trigger can create corresponding order. B2C customer order flow directly inserts `orders`, which is separate from request-trigger flow. | Low for B2B duplication; Medium trigger lifecycle concern. | Verify trigger timing with DRAFT/PENDING request lifecycle and ensure request insert does not prematurely create B2B orders if product expects submit-only creation. |

## 5. Orders/Requests Write-Path Checklist

| File/function | Actor/role expected | Fields written | Protected fields client-controlled? | RLS/server validation needed | Risk | Recommended follow-up |
|---|---|---|---|---|---|---|
| `SupabasePharmaRepository.createCustomerOrder` | `PUBLIC_USER` | `orders`: `medicine_id`, `medicine_name`, `quantity`, `unit`, `pharmacy_id`, `customer_id`, `order_type`, `fulfillment_type`, `delivery_address`, `delivery_phone`, `notes`, default `status`, default `currency`. | `customer_id` is from `UserIdentity.userId`; `pharmacy_id` is selected by client UI. No role/tenant profile fields are written. | `customer_create_order` must enforce `auth.uid() = customer_id`, `account_type = PUBLIC_USER`, B2C invariants, and valid target pharmacy. | Medium | Live negative tests for spoofed `customer_id`, invalid `pharmacy_id`, non-`PUBLIC_USER` insert. Consider target-pharmacy validation if absent. |
| `SupabasePharmaRepository.cancelCustomerOrder` | `PUBLIC_USER` owner | No direct table update; calls `cancel_customer_order(p_order_id)`. | No. | RPC must verify `profiles.account_type = PUBLIC_USER`, `customer_id = auth.uid()`, order type/status. | Low after profiles hardening | Live RPC negative tests. |
| `SupabasePharmaRepository.confirmOrder` | Owning `PHARMACY` | No direct table update; calls `confirm_customer_order(p_order_id, p_total_price_cents)`. | No direct tenant write. Uses persisted `organizationId` for local precheck. | RPC must verify `profiles.account_type = PHARMACY`, `profiles.pharmacy_id`, ownership, status, and price. | Low after profiles hardening | Live RPC negative tests for cross-pharmacy and role spoofing. |
| `SupabasePharmaRepository.rejectOrder` | Owning `PHARMACY` | No direct table update; calls `reject_customer_order(p_order_id)`. | No. | RPC must verify profile role/tenant and pending B2C order. | Low after profiles hardening | Live RPC negative tests. |
| `SupabasePharmaRepository.markOrderReadyForPickup` | Owning `PHARMACY` | No direct table update; calls `mark_customer_order_ready_for_pickup(p_order_id)`. | No. | RPC must verify role/tenant, B2C order, `CONFIRMED`, `PICKUP`. | Low after profiles hardening | Live RPC negative tests. |
| `SupabasePharmaRepository.markOrderOutForDelivery` | Owning `PHARMACY` | No direct table update; calls `mark_customer_order_out_for_delivery(p_order_id)`. | No. | RPC must verify role/tenant, B2C order, `CONFIRMED`, `DELIVERY`. | Low after profiles hardening | Live RPC negative tests. |
| `SupabasePharmaRepository.markOrderDelivered` | Owning `PHARMACY` | No direct table update; calls `mark_customer_order_delivered(p_order_id)`. | No. | RPC must verify role/tenant, B2C order, valid ready/out-for-delivery status. | Low after profiles hardening | Live RPC negative tests. |
| `SupabasePharmaRepository.createRequest` | `PHARMACY` | `requests`: `pharmacy_id`, `warehouse_id`, medicine fields, `priority`, warehouse display fields, `status = DRAFT`. | `pharmacy_id` comes from persisted profile snapshot; `warehouse_id` comes from selected warehouse UI/domain request. | Request insert RLS/FK must validate owning pharmacy and target warehouse. | Medium | Separate request RLS review; test forged warehouse id and non-pharmacy inserts. |
| `SupabasePharmaRepository.updateRequest` | Owning `PHARMACY`, owning `WAREHOUSE`, or read-only `ADMIN` for lifecycle | `requests`: `status`, `warehouse_id`, `warehouse_name`, `notes`. | `warehouse_id` can be supplied through `RequestUpdate`; role/ownership checks happen before update. | Request update RLS must enforce role, ownership, allowed transitions, and whether warehouse reassignment is allowed. | Medium/High | Must review request RLS before production if warehouse reassignment is not intended. |
| `SupabasePharmaRepository.submitRequest` | Owning `PHARMACY` | `requests.status = PENDING`. | No profile fields. Status transition requested by client, prechecked by repository. | Request update RLS must enforce owning pharmacy and transition. | Medium | Live request lifecycle negative tests. |
| `SupabasePharmaRepository.deleteRequest` | Owning `PHARMACY` | Deletes `requests` row by `id` and `pharmacy_id`; only DRAFT after repository precheck. | No profile fields. | Request delete RLS must enforce own pharmacy and allowed state. | Medium | Live request delete negative tests. |
| `SupabasePharmaRepository.updateOrderStatus` | Legacy/general | Unsupported. | No write. | None in current implementation. | Low | Keep unsupported unless backend contract is proven. |
| `SupabasePharmaRepository.createOrder` | Legacy/general | Unsupported. | No write. | None in current implementation. | Low | Keep unsupported for B2B trigger-created orders. |
| `SupabasePharmaRepository.deleteOrder` | Legacy/general | Unsupported. | No write. | None in current implementation. | Low | Keep unsupported unless backend contract is designed. |

## 6. Orders/Requests Read And Observe Checklist

| File/function | Query filters used | Source of identity fields | Role-aware? | RLS still required? | Risk | Recommended follow-up |
|---|---|---|---|---|---|---|
| `fetchOrdersForIdentity` for `ADMIN` | No filter. | Persisted profile snapshot role. | Yes. | Yes, if normal admin client access is intended. | Medium | Admin broad reads require explicit policy/design outside this phase. |
| `fetchOrdersForIdentity` for `WAREHOUSE` | `orders.warehouse_id = organizationId`. | `UserIdentityMapper` from persisted `profiles.warehouse_id`, with legacy fallback to `pharmacy_id`. | Yes. | Yes. | Medium | Remove warehouse fallback after schema migration; live test warehouse cross-tenant isolation. |
| `fetchOrdersForIdentity` for `PHARMACY` | Reads `requests.id` for `pharmacy_id`, then `orders.request_id IN (...)`. | Persisted `profiles.pharmacy_id`. | Yes. | Yes. | Medium | Confirm B2B and B2C pharmacy order visibility matches product expectations. B2C pharmacy reads may need direct `orders.pharmacy_id` filter or RLS proof. |
| `fetchOrdersForIdentity` for `PUBLIC_USER` | Throws unsupported/unauthorized. | Persisted role. | Yes. | Yes. | Low | Public users should use `getMyOrders`. |
| `getOrder` for `ADMIN` | `orders.id = orderId`. | Persisted role. | Yes. | Yes. | Medium | Admin read policy must be explicit if used. |
| `getOrder` for `WAREHOUSE` | `orders.id = orderId` and `warehouse_id = organizationId`. | Persisted `profiles.warehouse_id` or fallback. | Yes. | Yes. | Low/Medium | Live cross-warehouse negative test. |
| `getOrder` for `PHARMACY` | Fetches order by id, then verifies linked request has `pharmacy_id = organizationId`. | Persisted `profiles.pharmacy_id`. | Yes. | Yes. | Medium | For B2C orders with `request_id = null`, this path throws; pharmacy B2C detail reads may rely on different flow or need follow-up. |
| `getOrder` for `PUBLIC_USER` | Throws unsupported/unauthorized. | Persisted role. | Yes. | Yes. | Low | Public users should use dedicated customer order paths. |
| `getMyOrders` | `orders.customer_id = customerId` after checking `customerId == identity.userId`. | Persisted role and user id. | Yes. | Yes. | Low | Live test that forged customer id is denied. |
| `fetchRequests` for `PHARMACY` | `requests.pharmacy_id = organizationId`. | Persisted `profiles.pharmacy_id`. | Yes. | Yes. | Low/Medium | Live request isolation test. |
| `fetchRequests` for `WAREHOUSE` | `requests.warehouse_id = organizationId`. | Persisted `profiles.warehouse_id` or fallback. | Yes. | Yes. | Medium | Remove fallback later; live request isolation test. |
| `fetchRequests` for `ADMIN` | No filter. | Persisted role. | Yes. | Yes. | Medium | Admin broad read requires explicit policy/design outside this phase. |
| `fetchRequests` for `PUBLIC_USER` | Returns empty list. | Persisted role. | Yes. | Yes. | Low | Live test public user cannot query requests directly. |
| `observeIncomingRequestsForWarehouse(warehouseId)` | `requests.warehouse_id = warehouseId`. | Caller supplies `warehouseId`; current ViewModel derives it from snapshot. | Partly. | Yes. | Medium | Prefer repository-resolved warehouse id or ensure RLS denies arbitrary warehouse id. |

## 7. Trigger Interaction Checklist

Known DB behavior:

- `database/triggers/create_order_from_request.sql` defines `create_order_from_request()`.
- The trigger is `AFTER INSERT ON requests`.
- It inserts into `orders` using `NEW.pharmacy_id`, `NEW.warehouse_id`, medicine fields, quantity, unit, and status `PENDING`.
- It updates `requests.related_order_id` when order creation succeeds.
- It skips duplicate order creation when an order already exists for the request.

Android interaction:

- `SupabasePharmaRepository.createRequest` inserts into `requests`.
- Generic `SupabasePharmaRepository.createOrder` is unsupported.
- No B2B Android path was found that manually inserts into `orders` after request creation.
- B2C `createCustomerOrder` directly inserts into `orders`; this is separate from the request-trigger flow because B2C uses `order_type = CUSTOMER_PHARMACY` and no request id.

Trigger risk:

- The trigger appears to create an order on every request insert, while Android creates requests with `status = DRAFT`.
- This may be intended legacy behavior, but if the product expects B2B orders only after `submitRequest`, this is a lifecycle mismatch.
- This task does not change trigger behavior.

Recommended follow-up:

- Verify in a separate request/order phase whether B2B order creation should happen on request insert or request submit.
- Ensure request insert RLS prevents spoofed `pharmacy_id` and invalid `warehouse_id`, since the trigger copies both into `orders`.

## 8. Out-Of-Scope Boundaries

This task does not:

- Fix orders RLS.
- Fix requests RLS.
- Change domain models.
- Change `Order.kt` or `Request.kt`.
- Change navigation or UI.
- Execute live SQL.
- Verify live runtime behavior.
- Introduce admin/server role management.

Any implementation findings must be deferred to a separate approved phase/task.

## 9. Final Risk Summary

Overall risk level after profiles hardening:

- Medium until live verification is complete.

Improved by Phase 4.5.1B:

- `profiles.account_type` can no longer be treated as client-mutable from normal authenticated clients if the migration is applied.
- `profiles.pharmacy_id` and `profiles.warehouse_id` can no longer be spoofed through normal profile updates if the migration is applied.
- Auth bootstrap no longer lets metadata overwrite persisted profile authorization fields.
- B2C lifecycle RPCs now have a stronger profile trust foundation.

Must fix or verify before production closure:

- Live verification that the profiles hardening migration is applied and effective.
- Live verification that B2C order RPCs deny role spoofing and tenant spoofing.
- Live verification that request insert/update/delete policies prevent forged `pharmacy_id`, forged `warehouse_id`, and invalid lifecycle transitions.
- Runtime verification that `create_public_user_profile(...)` response shape works with `ProfileRowDto`.
- Review whether `profiles.is_active` should gate order/request access; local orders/requests logic does not currently enforce it.

Can defer if tracked:

- Remove `WAREHOUSE` organization fallback from `pharmacy_id` after warehouse linkage migration is complete.
- Refine B2C target pharmacy validation beyond `pharmacy_id IS NOT NULL`.
- Decide whether B2B order creation should happen on request insert or submit.
- Admin broad read/write policy design.

Task 10 readiness:

- Task 10 can proceed: yes.
- Closure should not claim production-safe profile/order trust until the live verification items above are executed and captured.

## 10. No Implementation Changes Confirmation

Confirmed:

- No Kotlin source code was modified.
- No SQL migration was modified.
- No UI/navigation/domain/model files were modified.
- `Order.kt` was not modified.
- `Request.kt` was not modified.
- No live Supabase SQL was executed.
- No secrets, JWTs, service-role keys, API keys, passwords, access tokens, or refresh tokens were used or saved.
