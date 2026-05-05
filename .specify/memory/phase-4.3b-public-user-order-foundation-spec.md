# Feature Specification: Phase 4.3B - PUBLIC_USER Order Foundation: Repository + UseCases

**Feature Branch**: `phase-4.3b-public-user-order-foundation`  
**Created**: 2026-04-28  
**Status**: Draft  
**Input**: User description: "Phase 4.3B — PUBLIC_USER Order Foundation: Repository + UseCases"

## Context

PharmaNet is an Android modular MVVM + Repository-driven Compose application using Hilt, Supabase, DataStore, and StateFlow. The project constitution is active and non-optional.

Phase 4.3A is already complete and provides the following baseline:
- `Order` model extended for dual-flow support
- `OrderType` and `FulfillmentType` are already present
- `OrderDto` and `RequestDto` are separated
- order mapping is stable
- app compile was previously restored
- runtime validation was completed
- temporary logs were removed

This phase is backend/domain only. UI and navigation are explicitly out of scope.

## Scope

This phase delivers the repository and use case foundation for `CUSTOMER_PHARMACY` orders only.

In scope:
- B2C order creation for `PUBLIC_USER`
- B2C order cancellation by the owning customer
- B2C order lifecycle actions for the owning pharmacy
- business validation in use cases
- defensive data access and ownership checks in repositories
- compile-safe medicine validation using an existing safe contract when available, otherwise `medicineId.isNotBlank()` with a clear TODO
- preservation of existing B2B `PHARMACY_WAREHOUSE` behavior

Out of scope for this phase:
- UI
- navigation
- screen wiring
- ViewModel business logic
- schema redesign
- `Order` model redesign
- `Request` model redesign
- unified transactions
- inventory validation

## User Scenarios & Testing

### User Story 1 - Customer Creates A B2C Order (Priority: P1)

A `PUBLIC_USER` can create a `CUSTOMER_PHARMACY` order directed to a pharmacy without involving any warehouse flow.

**Why this priority**: This is the entry point for the B2C order flow and the minimum usable slice for customer ordering.

**Independent Test**: Can be tested by invoking the create use case and repository path with `PUBLIC_USER` identity and verifying a compile-safe successful contract for a `PENDING` B2C order.

**Acceptance Scenarios**:

1. **Given** a `PUBLIC_USER` and valid B2C order input, **When** create order is invoked, **Then** a `CUSTOMER_PHARMACY` order is created with status `PENDING`.
2. **Given** a `PUBLIC_USER` and `DELIVERY` fulfillment, **When** address or phone is missing, **Then** the operation fails validation.
3. **Given** a `PUBLIC_USER` and `PICKUP` fulfillment, **When** address and phone are omitted, **Then** the operation remains valid.
4. **Given** a non-`PUBLIC_USER` identity, **When** create order is invoked, **Then** the operation fails role validation.

---

### User Story 2 - Customer Cancels Own Pending B2C Order (Priority: P1)

A `PUBLIC_USER` can cancel only their own pending `CUSTOMER_PHARMACY` order.

**Why this priority**: Cancellation is a core buyer-side safety rule and must be available before any UI is built on top of the flow.

**Independent Test**: Can be tested by invoking the cancel use case with a preloaded B2C order and verifying role, ownership, order type, and status checks.

**Acceptance Scenarios**:

1. **Given** a `PUBLIC_USER` who owns a `CUSTOMER_PHARMACY` order in `PENDING`, **When** cancel is invoked, **Then** the order is cancelled successfully.
2. **Given** a `PUBLIC_USER` who does not own the order, **When** cancel is invoked, **Then** the operation fails ownership validation.
3. **Given** a non-`PENDING` order, **When** cancel is invoked, **Then** the operation fails state transition validation.
4. **Given** a `PHARMACY_WAREHOUSE` order, **When** cancel is invoked through the B2C path, **Then** the operation is rejected.

---

### User Story 3 - Pharmacy Manages Its Own B2C Orders (Priority: P1)

The owning `PHARMACY` can confirm, reject, mark ready for pickup, mark out for delivery, and mark delivered for its own `CUSTOMER_PHARMACY` orders only.

**Why this priority**: Seller-side order progression is the foundation that allows the B2C flow to exist without changing the existing B2B order flow.

**Independent Test**: Can be tested by invoking each use case against a B2C order and verifying role, ownership, fulfillment type, and status transition rules before repository writes occur.

**Acceptance Scenarios**:

1. **Given** a pharmacy-owned `CUSTOMER_PHARMACY` order in `PENDING`, **When** confirm is invoked with `totalPriceCents >= 0`, **Then** the order transitions to `CONFIRMED`.
2. **Given** a pharmacy-owned `CUSTOMER_PHARMACY` order in `PENDING`, **When** reject is invoked, **Then** the order transitions to `REJECTED`.
3. **Given** a pharmacy-owned `CUSTOMER_PHARMACY` order with `PICKUP`, **When** mark ready is invoked from an allowed seller state, **Then** the order transitions to `READY_FOR_PICKUP`.
4. **Given** a pharmacy-owned `CUSTOMER_PHARMACY` order with `DELIVERY`, **When** mark out for delivery is invoked from an allowed seller state, **Then** the order transitions to `OUT_FOR_DELIVERY`.
5. **Given** a pharmacy-owned `CUSTOMER_PHARMACY` order in `READY_FOR_PICKUP` or `OUT_FOR_DELIVERY`, **When** mark delivered is invoked, **Then** the order transitions to `DELIVERED`.
6. **Given** a pharmacy that does not own the order, **When** any seller action is invoked, **Then** the operation fails ownership validation.

## Functional Requirements

- **FR-001**: The system MUST allow a `PUBLIC_USER` to create a `CUSTOMER_PHARMACY` order through repository and use case layers only.
- **FR-002**: The system MUST create every new `CUSTOMER_PHARMACY` order with status `PENDING`.
- **FR-003**: The system MUST create every new `CUSTOMER_PHARMACY` order with `totalPriceCents = null`.
- **FR-004**: The system MUST require `deliveryAddress` and `deliveryPhone` when `fulfillmentType = DELIVERY`.
- **FR-005**: The system MUST allow `PICKUP` orders without delivery address or phone.
- **FR-006**: The system MUST allow a `PUBLIC_USER` to cancel an order only when all of the following are true: `orderType = CUSTOMER_PHARMACY`, `customerId` matches the current user, and `status = PENDING`.
- **FR-007**: The system MUST allow a `PHARMACY` to confirm a B2C order only when the pharmacy owns the order and `totalPriceCents >= 0`.
- **FR-008**: The system MUST allow a `PHARMACY` to reject a B2C order without a reason parameter in this phase, only when the pharmacy owns the order and the order is in an allowed seller state.
- **FR-009**: The system MUST allow `markOrderReadyForPickup` only for `CUSTOMER_PHARMACY` orders where `fulfillmentType = PICKUP` and the transition is valid.
- **FR-010**: The system MUST allow `markOrderOutForDelivery` only for `CUSTOMER_PHARMACY` orders where `fulfillmentType = DELIVERY` and the transition is valid.
- **FR-011**: The system MUST allow `markOrderDelivered` only from `READY_FOR_PICKUP` or `OUT_FOR_DELIVERY`.
- **FR-012**: Every write operation in this phase MUST enforce role validation, ownership validation, and state transition validation.
- **FR-013**: Repositories in this phase MUST own data access and defensive ownership checks.
- **FR-014**: Use cases in this phase MUST own business rules and flow-specific validation.
- **FR-015**: ViewModels MUST NOT gain new business rules as part of this phase.
- **FR-016**: The system MUST NOT introduce inventory validation in this phase.
- **FR-017**: Medicine validation MUST use an existing ready and safe contract only if one already exists; otherwise the implementation MUST fall back to compile-safe validation using `medicineId.isNotBlank()` without fake success.
- **FR-018**: Existing `PHARMACY_WAREHOUSE` behavior MUST remain unchanged.
- **FR-019**: The implementation MUST NOT modify UI or navigation files in this phase.
- **FR-020**: The implementation MUST compile successfully via `.\gradlew.bat --no-daemon :app:compileDebugKotlin`.
- **FR-021**: `rejectOrder(orderId)` MUST only change status to `REJECTED` after role validation, ownership validation, and state transition validation succeed.
- **FR-022**: `RejectCustomerOrderUseCase` MUST NOT accept a `reason` parameter in this phase.
- **FR-023**: Repository `rejectOrder(orderId)` MUST NOT use `notes` to store a rejection reason.
- **FR-024**: The implementation MUST NOT change `Order` model or schema to support rejection reasons in this phase.
- **FR-025**: This phase MUST NOT introduce a new `MedicineRepository` if one does not already exist.
- **FR-026**: If no ready and safe catalog contract exists, the implementation MUST use compile-safe validation only via `medicineId.isNotBlank()`.
- **FR-027**: The implementation MUST leave a clear TODO: `TODO: replace with catalog-backed medicine existence validation.`

## Key Entities

- **Order**: Dual-flow order entity already supporting `PHARMACY_WAREHOUSE` and `CUSTOMER_PHARMACY`, including ownership fields, status, fulfillment type, and nullable B2C delivery data.
- **PharmaRepository**: Repository contract expected to expose B2C order lifecycle methods while preserving current B2B paths.
- **B2C Order UseCases**: Business-rule entry points for create, cancel, confirm, reject, ready-for-pickup, out-for-delivery, and delivered transitions.
- **Access Context**: Current authenticated role and ownership identity used to validate customer and pharmacy actions.

## Non-Goals

- No UI changes
- No navigation changes
- No `CreateOrderScreen`
- No Stitch work
- No `Order` model changes
- No `Request` model changes
- No unified transactions
- No schema redesign
- No inventory validation
- No fake success paths

## Acceptance Criteria

- B2C repository methods for the `CUSTOMER_PHARMACY` lifecycle exist and compile.
- B2C use cases exist and contain the required business validation.
- Repository write paths enforce role, ownership, and defensive state checks.
- Use cases enforce business validation without moving rules into ViewModels.
- `rejectOrder(orderId)` and `RejectCustomerOrderUseCase` operate without a `reason` argument.
- No rejection reason is stored in `notes`, schema, or `Order` model in this phase.
- Medicine validation uses either an existing safe contract or the compile-safe fallback `medicineId.isNotBlank()`.
- No new `MedicineRepository` is introduced in this phase.
- No UI or navigation files are changed.
- Existing B2B `PHARMACY_WAREHOUSE` behavior is preserved.
- The project compiles successfully with `.\gradlew.bat --no-daemon :app:compileDebugKotlin`.

## Risks

- Supabase RLS may allow the broad flow but still require alignment checks for each specific write transition.
- If catalog-based medicine validation is only partially available, implementation may need a temporary TODO boundary to avoid fake confidence.
- Existing B2C-related files already present in the codebase may not fully match the constitution and could require tightening rather than simple addition.

## Future Enhancement

- Add `rejectionReason` or `statusReason` field to schema/model later.

## Assumptions

- The constitution overrides any conflicting shortcut during implementation.
- Phase 4.3A outputs remain valid and should be treated as stable inputs to this phase.
- Supabase remains the write backend for B2C order operations in this phase.
- This phase is a small vertical slice limited to repository and use case logic only.
- No new medicine data contract will be introduced in this phase solely for validation.
- If an existing safe medicine catalog contract is unavailable, `medicineId.isNotBlank()` is the approved fallback for this phase.
