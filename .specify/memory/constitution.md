# PharmaNet Android Constitution

## Core Principles

### I. Layer Ownership Is Mandatory
PharmaNet is an Android modular MVVM + Repository-driven Compose application using Hilt, Supabase, DataStore, and StateFlow. Each layer has a strict responsibility boundary:
- Repositories own data access, DTO orchestration, mappers integration, and defensive ownership checks.
- UseCases own business rules, role rules, state transition rules, and flow-specific invariants.
- ViewModels own UI state, effects, and use case invocation only.
- UI and navigation must not absorb backend, domain, access-control, or transition logic.

### II. Backend/Domain Work Must Not Change UI By Default
During backend, schema, repository, mapper, and domain phases, UI and navigation are frozen unless the task explicitly authorizes UI work. No implicit screen, route, Compose state, or navigation graph changes may be introduced while finishing backend/domain slices. Before any new UI is added, the required schema, repository, use cases, and compile validation must already exist.

### III. Domain Contract Integrity Is Non-Negotiable
Any domain model change must be propagated in the same slice to every dependent DTO, mapper, and call site. Partial migration is not allowed. No fake success states are allowed anywhere in the stack. Ownership and nullable domain fields must never use `!!`; safe handling, explicit validation, and explicit failures are required instead.

### IV. Request Flow And Order Flow Stay Logically Separate
The platform supports multiple roles: `PUBLIC_USER`, `PHARMACY`, `WAREHOUSE`, and `ADMIN`. Flow boundaries are mandatory:
- `Request` is only for `PHARMACY -> WAREHOUSE`.
- `Order` supports `PHARMACY_WAREHOUSE` and `CUSTOMER_PHARMACY`.
- Adding or extending `PUBLIC_USER` B2C flow must not break or dilute the existing B2B pharmacy/warehouse flow.
- Shared code is allowed only when semantics remain explicit and the two flows stay independently understandable, testable, and enforceable.

### V. Every Write Must Be Explicitly Defended
Every write operation must validate all of the following before persistence:
- role validation
- ownership validation
- state transition validation

Client-side checks alone are insufficient. Any new write-capable flow must also be supported by Supabase RLS and backend-side policy design. A feature is incomplete if client logic passes but RLS does not enforce the same authorization boundary.

## Delivery Constraints

All implementation must proceed as small vertical slices. Wide-scope refactors, broad speculative rewrites, and multi-flow rewiring in a single phase are prohibited.

Each phase must end with all of the following:
- compile success
- a clear list of modified files
- confirmation that no UI changes were made if the phase is backend/domain only

No phase may claim completion while compilation is broken, mappings are half-updated, or validations are deferred to a future phase without explicit approval.

## Execution Protocol

Before generating code, the implementation scope and intended files must be stated explicitly. If a request conflicts with this constitution, execution must stop and clarification must be requested before changes begin.

Implementation order for new capabilities is:
1. schema and access policy design
2. repository contract and defensive checks
3. use case business rules
4. compile validation
5. UI or navigation work only when explicitly in scope

Review checklist for every backend/domain slice:
- Are request and order semantics still separate?
- Did every domain change update DTOs, mappers, and call sites?
- Does every write path enforce role, ownership, and state transition validation?
- Are repository and use case responsibilities still cleanly separated?
- Is Supabase RLS aligned with the new flow?
- Was fake success avoided?
- Was `!!` avoided on ownership and nullable domain fields?

## Governance

This constitution overrides conflicting implementation shortcuts, task prompts, and convenience-based decisions. Any prompt or plan that conflicts with these rules must be paused for clarification.

Amendments require:
- an explicit update to this constitution
- a rationale for the change
- identification of affected flows, layers, and policies
- a migration plan when the rule change affects existing contracts or role behavior

Compliance is mandatory for specs, plans, task generation, code review, and implementation.

**Version**: 1.0.0 | **Ratified**: 2026-04-28 | **Last Amended**: 2026-04-28
