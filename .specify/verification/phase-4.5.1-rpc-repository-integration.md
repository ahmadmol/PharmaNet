# Phase 4.5.1 RPC Repository Integration

**Task**: Task 5 - Integrate Repository With RPCs  
**Phase**: Phase 4.5.1 RLS Security Fixes  
**Date**: 2026-04-30  
**Scope**: Minimal `SupabasePharmaRepository` B2C lifecycle integration.  

## Methods Changed

Only `core/common/src/main/kotlin/com/pharmalink/data/repository/SupabasePharmaRepository.kt` was updated.

Changed B2C lifecycle methods:

- `cancelCustomerOrder`
- `confirmOrder`
- `rejectOrder`
- `markOrderReadyForPickup`
- `markOrderOutForDelivery`
- `markOrderDelivered`

Added private repository helper/data payloads:

- `callOrderRpc(functionName, params)`
- `OrderIdRpcParams`
- `ConfirmCustomerOrderRpcParams`

## RPC Mapping

| Repository method | Previous write path | New write path | Return behavior |
|---|---|---|---|
| `cancelCustomerOrder` | Direct `orders.update` | `cancel_customer_order` | RPC success maps to `Unit`; RPC failure remains `Result.failure` |
| `confirmOrder` | Direct `orders.update` | `confirm_customer_order` | Decodes returned `OrderDto` to domain `Order` |
| `rejectOrder` | Direct `orders.update` | `reject_customer_order` | Decodes returned `OrderDto` to domain `Order` |
| `markOrderReadyForPickup` | Direct `orders.update` | `mark_customer_order_ready_for_pickup` | Decodes returned `OrderDto` to domain `Order` |
| `markOrderOutForDelivery` | Direct `orders.update` | `mark_customer_order_out_for_delivery` | Decodes returned `OrderDto` to domain `Order` |
| `markOrderDelivered` | Direct `orders.update` | `mark_customer_order_delivered` | Decodes returned `OrderDto` to domain `Order` |

RPC parameter keys:

| RPC | Payload keys |
|---|---|
| `cancel_customer_order` | `p_order_id` |
| `confirm_customer_order` | `p_order_id`, `p_total_price_cents` |
| `reject_customer_order` | `p_order_id` |
| `mark_customer_order_ready_for_pickup` | `p_order_id` |
| `mark_customer_order_out_for_delivery` | `p_order_id` |
| `mark_customer_order_delivered` | `p_order_id` |

## `createCustomerOrder`

`createCustomerOrder` stayed as a direct `orders` insert.

Reason:

- The Phase 4.5.1 migration preserves this path through the hardened `customer_create_order` insert policy.
- No B2C create RPC was introduced by the approved migration design.

## Interface And Use Case Status

`PharmaRepository` signatures changed: no.

UseCases changed: no.

The existing use cases continue calling the same repository methods:

- `CancelCustomerOrderUseCase`
- `ConfirmCustomerOrderUseCase`
- `RejectCustomerOrderUseCase`
- `MarkOrderReadyUseCase`
- `MarkOrderOutForDeliveryUseCase`
- `MarkOrderDeliveredUseCase`
- `CreateCustomerOrderUseCase`

## B2B Path Status

B2B paths changed: no.

This task did not modify:

- `updateOrderStatus`
- `createOrder`
- `deleteOrder`
- request/order trigger behavior
- `PHARMACY_WAREHOUSE` behavior
- warehouse B2B behavior

## Failure Handling

RPC calls are executed inside existing `runCatching` blocks.

Expected behavior:

- RPC success returns the updated row or `Unit` for cancellation.
- RPC failure propagates as `Result.failure`.
- No fake success path was added.
- No `!!` was added.

## Compile Result

Command requested:

```powershell
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

First run:

- Result: failed due to `java.lang.OutOfMemoryError: Metaspace`.
- `:core:common:compileDebugKotlin` completed before the memory failure.
- Failure occurred later in feature module Kotlin compilation workers.

Second run:

```powershell
$env:GRADLE_OPTS='-Xmx4g -XX:MaxMetaspaceSize=1g'
$env:KOTLIN_DAEMON_JVMARGS='-Xmx2g -XX:MaxMetaspaceSize=1g'
.\gradlew.bat --no-daemon :app:compileDebugKotlin
```

- Result: `BUILD SUCCESSFUL`
- No source or Gradle configuration files were changed to adjust memory.

## Non-Blocking Warnings

Compile emitted existing warnings, including:

- Kotlin annotation default target warnings.
- Deprecated Material/Icon API warnings.
- One existing condition-always-true warning in `feature/request`.
- Gradle daemon memory setting warning.

No warning was introduced that blocks Task 5 closure.

## Remaining Risks

- Live Supabase must have the migration applied before runtime behavior can be proven.
- The RPCs return `public.orders`; live PostgREST response shape should be verified in SQL/API tests.
- `profiles.account_type` and tenant fields still require Task 7 trust verification.
- If live `orders.id` is not text-compatible, a migration signature correction is required before production use.

## Task 5 Decision

RPC integration complete: yes.

Compile result: `BUILD SUCCESSFUL` after temporary command-line memory increase.

No UI, navigation, domain model, repository interface, use case, SQL migration, or B2B behavior changes were made in this task.
