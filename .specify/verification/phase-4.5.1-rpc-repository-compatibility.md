# Phase 4.5.1 RPC Repository Compatibility

**Task**: Task 4 - Verify RPC Compatibility With Current Repositories  
**Phase**: Phase 4.5.1 RLS Security Fixes  
**Date**: 2026-04-30  
**Scope**: Read-only repository compatibility audit.  
**Implementation changes**: None.

## Evidence Sources

Files reviewed:

- `database/migrations/20260429_harden_b2c_order_rls.sql`
- `database/migrations/20250425_extend_orders_for_b2c.sql`
- `database/triggers/create_order_from_request.sql`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/PharmaRepository.kt`
- `core/common/src/main/kotlin/com/pharmalink/data/repository/InMemoryPharmaRepository.kt`
- `feature/orders/src/main/kotlin/com/pharmalink/feature/orders/usecase/*.kt`

No repository, app, domain, UI, navigation, SQL migration, or live database changes were made.

## Executive Summary

RPC integration is required before the app can work correctly with the hardened RLS migration.

Current repository behavior:

- `createCustomerOrder` uses direct `orders` insert.
- `cancelCustomerOrder` uses direct `orders` update.
- `confirmOrder` uses direct `orders` update.
- `rejectOrder` uses direct `orders` update.
- `markOrderReadyForPickup` uses direct `orders` update.
- `markOrderOutForDelivery` uses direct `orders` update.
- `markOrderDelivered` uses direct `orders` update.

After the migration drops `customer_cancel_pending` and `pharmacy_manage_b2c`, the direct B2C update methods are expected to fail under RLS. They must be changed in Task 5 to call the new RPC functions.

`createCustomerOrder` can remain a direct insert because the migration recreates a hardened `customer_create_order` insert policy.

## Repository Contract Compatibility

`PharmaRepository` already has stable method signatures that map cleanly to the RPC migration:

| Repository method | Current signature | Interface change needed? |
|---|---|---:|
| `createCustomerOrder` | Returns `Result<Order>` | No |
| `cancelCustomerOrder` | Returns `Result<Unit>` | No |
| `confirmOrder` | Returns `Result<Order>` | No |
| `rejectOrder` | Returns `Result<Order>` | No |
| `markOrderReadyForPickup` | Returns `Result<Order>` | No |
| `markOrderOutForDelivery` | Returns `Result<Order>` | No |
| `markOrderDelivered` | Returns `Result<Order>` | No |

Task 5 should preserve these signatures.

## Method Compatibility Matrix

### `createCustomerOrder`

| Check | Result |
|---|---|
| Exists? | Yes, `SupabasePharmaRepository.createCustomerOrder` |
| Current data path | Direct `orders` insert with `select(Columns.ALL)` |
| Direct table update? | No |
| Breaks after dropping B2C update policies? | No |
| Needs RPC conversion? | No |
| Matching RPC | None required |
| Can remain direct insert? | Yes |
| Relevant hardened policy | `customer_create_order` |
| Signature change needed? | No |
| Use case impact | `CreateCustomerOrderUseCase` can remain unchanged |

Assessment:

- The method performs app-layer `PUBLIC_USER` validation and inserts a `CUSTOMER_PHARMACY` `PENDING` row.
- The hardened `customer_create_order` policy should allow this insert only for trusted `PUBLIC_USER` profiles.
- Task 5 should not convert this method to RPC unless a later SQL/API test proves insert compatibility fails.

### `cancelCustomerOrder`

| Check | Result |
|---|---|
| Exists? | Yes, `SupabasePharmaRepository.cancelCustomerOrder` |
| Current data path | Direct `orders` update |
| Direct table update? | Yes |
| Breaks after dropping `customer_cancel_pending`? | Yes, expected |
| Needs RPC conversion? | Yes |
| Matching RPC | `cancel_customer_order` |
| Signature change needed? | No |
| Use case impact | `CancelCustomerOrderUseCase` can keep calling `pharmaRepository.cancelCustomerOrder(order.id)` |

Current update behavior:

- Validates role, ownership, type, and pending state in Kotlin.
- Runs direct table update to set `status = CANCELLED`.

Task 5 recommendation:

- Replace the direct update with `rpc("cancel_customer_order", ...)`.
- Keep returning `Result<Unit>`.
- Treat any RPC error as `Result.failure`; no fake success.

### `confirmOrder`

| Check | Result |
|---|---|
| Exists? | Yes, `SupabasePharmaRepository.confirmOrder` |
| Current data path | Direct `orders` update |
| Direct table update? | Yes |
| Breaks after dropping `pharmacy_manage_b2c`? | Yes, expected |
| Needs RPC conversion? | Yes |
| Matching RPC | `confirm_customer_order` |
| Signature change needed? | No |
| Use case impact | `ConfirmCustomerOrderUseCase` can keep calling `pharmaRepository.confirmOrder(order.id, totalPriceCents)` |

Current update behavior:

- Validates pharmacy role, pharmacy ownership, type, pending state, and non-negative price in Kotlin.
- Runs direct table update to set `CONFIRMED`, `total_price_cents`, `confirmed_at`, and `updated_at`.

Task 5 recommendation:

- Replace the direct update plus follow-up `getOrder` with `rpc("confirm_customer_order", ...)`.
- Decode the returned `OrderDto` into `DomainOrder`, or if the client RPC API returns no body in practice, call `getOrder(orderId)` after successful RPC.

### `rejectOrder`

| Check | Result |
|---|---|
| Exists? | Yes, `SupabasePharmaRepository.rejectOrder` |
| Current data path | Direct `orders` update |
| Direct table update? | Yes |
| Breaks after dropping `pharmacy_manage_b2c`? | Yes, expected |
| Needs RPC conversion? | Yes |
| Matching RPC | `reject_customer_order` |
| Signature change needed? | No |
| Use case impact | `RejectCustomerOrderUseCase` can remain unchanged |

Task 5 recommendation:

- Replace direct update with `rpc("reject_customer_order", ...)`.
- Do not add rejection reason or use notes as a rejection reason.

### `markOrderReadyForPickup`

| Check | Result |
|---|---|
| Exists? | Yes, `SupabasePharmaRepository.markOrderReadyForPickup` |
| Current data path | Direct `orders` update |
| Direct table update? | Yes |
| Breaks after dropping `pharmacy_manage_b2c`? | Yes, expected |
| Needs RPC conversion? | Yes |
| Matching RPC | `mark_customer_order_ready_for_pickup` |
| Signature change needed? | No |
| Use case impact | `MarkOrderReadyUseCase` can remain unchanged |

Task 5 recommendation:

- Replace direct update with `rpc("mark_customer_order_ready_for_pickup", ...)`.
- Preserve existing app-layer validation; the RPC becomes the database enforcement layer.

### `markOrderOutForDelivery`

| Check | Result |
|---|---|
| Exists? | Yes, `SupabasePharmaRepository.markOrderOutForDelivery` |
| Current data path | Direct `orders` update |
| Direct table update? | Yes |
| Breaks after dropping `pharmacy_manage_b2c`? | Yes, expected |
| Needs RPC conversion? | Yes |
| Matching RPC | `mark_customer_order_out_for_delivery` |
| Signature change needed? | No |
| Use case impact | `MarkOrderOutForDeliveryUseCase` can remain unchanged |

Task 5 recommendation:

- Replace direct update with `rpc("mark_customer_order_out_for_delivery", ...)`.

### `markOrderDelivered`

| Check | Result |
|---|---|
| Exists? | Yes, `SupabasePharmaRepository.markOrderDelivered` |
| Current data path | Direct `orders` update |
| Direct table update? | Yes |
| Breaks after dropping `pharmacy_manage_b2c`? | Yes, expected |
| Needs RPC conversion? | Yes |
| Matching RPC | `mark_customer_order_delivered` |
| Signature change needed? | No |
| Use case impact | `MarkOrderDeliveredUseCase` can remain unchanged |

Task 5 recommendation:

- Replace direct update with `rpc("mark_customer_order_delivered", ...)`.

## SQL ID Type Compatibility

Local evidence supports `p_order_id text`:

- `OrderDto.id` is `String`.
- `PharmaRepository` lifecycle methods accept `orderId: String`.
- `database/triggers/create_order_from_request.sql` declares `new_order_id TEXT`.
- The trigger generates IDs like `'order_' || substr(...)`, which are not UUID-shaped.
- The new migration RPCs accept `p_order_id text`.

No local migration that creates the base `orders` table was found in this audit scope, so the live schema must still be verified.

Decision:

- Migration signature issue found from local evidence: no.
- Task 3.1 migration correction is not required from local evidence.
- If live `orders.id` is UUID, Task 3.1 must adjust RPC signatures or add safe casting before Task 5 proceeds.

## Supabase Kotlin RPC Payload Compatibility

Task 5 must verify the exact Supabase Kotlin RPC call shape used by the project dependency.

Expected payload keys should match SQL parameter names:

| RPC | Expected payload |
|---|---|
| `cancel_customer_order` | `p_order_id` |
| `confirm_customer_order` | `p_order_id`, `p_total_price_cents` |
| `reject_customer_order` | `p_order_id` |
| `mark_customer_order_ready_for_pickup` | `p_order_id` |
| `mark_customer_order_out_for_delivery` | `p_order_id` |
| `mark_customer_order_delivered` | `p_order_id` |

Compatibility risk:

- Low to medium. The repository currently uses direct PostgREST table operations, not RPC calls in the audited methods.
- Task 5 should add small serializable RPC parameter DTOs or maps according to the existing Supabase Kotlin API style.
- RPC errors must propagate through `runCatching` as failures.

## Use Case Impact

The use cases call the repository interface methods, not direct Supabase APIs:

- `CreateCustomerOrderUseCase` calls `createCustomerOrder`.
- `CancelCustomerOrderUseCase` calls `cancelCustomerOrder`.
- `ConfirmCustomerOrderUseCase` calls `confirmOrder`.
- `RejectCustomerOrderUseCase` calls `rejectOrder`.
- `MarkOrderReadyUseCase` calls `markOrderReadyForPickup`.
- `MarkOrderOutForDeliveryUseCase` calls `markOrderOutForDelivery`.
- `MarkOrderDeliveredUseCase` calls `markOrderDelivered`.

No use case signature changes are required if Task 5 keeps `PharmaRepository` unchanged.

## InMemory Repository Compatibility

`InMemoryPharmaRepository` already implements the B2C methods as explicit `Result.failure(UnsupportedOperationException(...))`.

No change is required if `PharmaRepository` signatures remain unchanged.

No fake success is present for these B2C methods in the in-memory repository.

## B2B Preservation

The audited B2C lifecycle methods enforce `OrderType.CUSTOMER_PHARMACY` before updating.

Task 5 should only change B2C lifecycle paths in `SupabasePharmaRepository` to call the new RPCs. It should not change:

- Generic B2B order creation
- `PHARMACY_WAREHOUSE` policies
- Warehouse B2B methods
- Request/order trigger behavior
- `updateOrderStatus`
- `createOrder`
- `deleteOrder`

## Compile Risk For Task 5

Expected compile risk: manageable.

No interface changes are required, so compile risk is limited to:

- Correct Supabase Kotlin RPC call syntax.
- Correct RPC parameter serialization.
- Correct decoding of `public.orders` RPC return into `OrderDto`.
- `cancelCustomerOrder` returning `Result<Unit>` while the RPC returns a row.

Recommended Task 5 implementation posture:

- Keep method signatures unchanged.
- Keep existing Kotlin pre-validation if useful.
- Replace only the final direct B2C update operation with the matching RPC.
- For methods returning `Result<Order>`, decode the RPC result to `OrderDto` and map to domain, or perform a safe post-RPC `getOrder(orderId)` if RPC decoding is not compatible.
- For `cancelCustomerOrder`, ignore the returned row after successful RPC and return `Unit`.

## Stop Condition Review

### Migration RPC signatures incompatible with local order id type?

No.

Local evidence supports text/string order IDs.

### Supabase Kotlin RPC payload incompatible?

Unknown until Task 5 implementation checks the exact client API. This does not require migration correction, but it is a Task 5 implementation detail.

### Task 3.1 required before Task 5?

No, not from local evidence.

Task 3.1 is only required if live schema verification proves `orders.id` is UUID or another non-text type incompatible with the migration.

## Task 4 Decision

Repository direct updates found: yes.

RPC integration required: yes.

Migration signature issue found: no, local evidence supports `p_order_id text`.

Can Task 5 proceed: yes, after Approval Gate B and with the scope limited to `SupabasePharmaRepository` B2C lifecycle methods.

No implementation changes were made.
