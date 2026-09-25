# 0002. How capabilities reach each other

Date: 2026-09-25 · Status: accepted

## Context

Spec §3 says capabilities in `application` talk only through each other's `api`
subpackage or domain events. Some needs cross capabilities in the first session:
finishing a trip (lists) writes a receipt (receipts); "Comprar de novo" (lists) reads a
receipt; photos and sync will ask `CapabilityPolicy`; every capability needs a clock, IDs,
a unit of work and event publishing, and spec §9 lets every use case call the telemetry
ports.

## Decision

- `application.common.port` holds the shared kernel: `Clock`, `IdGenerator`, `UnitOfWork`,
  `DomainEventPublisher`, `AuditTrail` and `PersistenceException`. Any capability may use it.
- `application.telemetry.port` is open to every capability, as §9 describes.
- `receipts.api.ReceiptBook` is how lists records and reads receipts. `FinishPurchase`,
  `ShopAgain` and `ImportItemsFromReceipt` live in lists because they change lists.
  Receipts depends on nothing in lists, so there is no cycle.
- `CapabilityPolicy`, `Capability`, `ClientContext` and `CapabilityUnavailable` live in
  `capabilities.api` so other capabilities can call the policy.
- `ListAccessPolicy` lives in `domain.list`: it reads only the aggregate (spec §6,
  "Authorization: Domain"), and the aggregate calls it before every change.

`ArchitectureTest.capabilities_meet_only_through_their_api` encodes exactly these
exceptions, and `capabilities_are_free_of_cycles` keeps the graph acyclic.

## Consequences

A new cross-capability need means adding to some `api` subpackage, which is visible in
review. The shared kernel must stay small; anything with business meaning belongs to a
capability.
