# Handoff after session 1

## Built this session

- Gradle skeleton: Java 25 toolchain, Spring Boot 4.1.1 BOM, version catalog, source sets
  `main`, `test`, `testFixtures`, `integrationTest`; `Makefile`; `compose.yaml` with every
  local service from spec §14 (unused so far); an empty main class in `bootstrap`.
- ArchUnit rules from spec §11 and §13, written before any feature code.
- Domain: value objects and UUIDv7 IDs, `Money`, `Quantity`, `Result`; the `ShoppingList`
  aggregate with `Item`, `ListTotals`, lifecycle, limits and domain events;
  `ListAccessPolicy`; `Receipt`, `Account`, `Invitation`, `Entitlement`; `QuickAddParser`.
- Every output port from spec §5 plus `AuditTrail`, `Clock`, `IdGenerator`,
  `DomainEventPublisher`, with Javadoc and sealed error types.
- In `testFixtures`: a fake for every port, a rollback-capable `InMemoryUnitOfWork`,
  contract suites for the five repositories, `ChangeLog`, `GuestPassStore` and
  `CatalogSource`, and `InMemoryCore`, which wires the whole core to the fakes.
- Use cases: create/get/update/delete/restore/duplicate list; add/quick-add/edit/pick/
  unpick/remove/restore/duplicate item; finish purchase; get receipt; history by month;
  shop again; import items from a receipt; upsert account; update preferences;
  `CapabilityService` with the static defaults.
- `ShoppingTripScenarioTest` replays the prototype story.

## How to run and verify it (exact commands)

```bash
make test                      # 147 tests, about 10 s from a clean build, Docker not needed
docker compose config -q       # compose.yaml is valid
./gradlew test --tests '*ShoppingTripScenarioTest'
./gradlew test --tests '*ArchitectureTest'
```

To watch a wall hold, add `org.springframework.context.ApplicationContext field;` to any
class in `app.meucarrinho.domain` and run `make test`.

## Ports and adapters

| Port | Adapters that exist | Selected locally |
|---|---|---|
| `ShoppingListRepository` | `InMemoryShoppingListRepository` (testFixtures) | none yet |
| `ReceiptRepository` | `InMemoryReceiptRepository` | none yet |
| `AccountRepository` | `InMemoryAccountRepository` | none yet |
| `InvitationRepository` | `InMemoryInvitationRepository` | none yet |
| `EntitlementRepository` | `InMemoryEntitlementRepository` | none yet |
| `ListQueries` | `InMemoryListQueries` | none yet |
| `ChangeLog` | `InMemoryChangeLog` | none yet |
| `UnitOfWork` | `InMemoryUnitOfWork` | none yet |
| `IdentityDirectory` | `FakeIdentityDirectory` | none yet |
| `GuestPassStore` | `InMemoryGuestPassStore` | none yet |
| `FeatureFlags` | `FixedFeatureFlags` | none yet |
| `CatalogSource` | `InMemoryCatalogSource` | none yet |
| `EmailSender`, `PushSender` | `RecordingEmailSender`, `RecordingPushSender` | none yet |
| `PhotoStorage` | `InMemoryPhotoStorage` | none yet |
| `ListChangeBroadcaster`, `PresenceTracker` | `RecordingListChangeBroadcaster`, `InMemoryPresenceTracker` | none yet |
| `ProductAnalytics`, `BusinessMetrics`, `Tracing` | `RecordingProductAnalytics`, `RecordingBusinessMetrics`, `RecordingTracing` | none yet |
| `PaymentGateway`, `BillingEventSource` | `FakePaymentGateway`, `FakeBillingEventSource` | none (dormant) |
| `AuditTrail`, `Clock`, `IdGenerator`, `DomainEventPublisher` | `RecordingAuditTrail`, `MutableClock`, `SequentialIdGenerator`, `RecordingDomainEventPublisher` | none yet |

## Decisions made (links to docs/adr)

- [0001](docs/adr/0001-replace-the-maven-prototype.md) Replace the Maven prototype in place.
- [0002](docs/adr/0002-capability-boundaries.md) How capabilities reach each other.
- [0003](docs/adr/0003-domain-choices-the-spec-left-open.md) Domain choices the spec left open.
- [0004](docs/adr/0004-quick-add-grammar.md) Quick-add grammar.
- [0005](docs/adr/0005-port-error-style.md) How ports report failure.

## Known gaps and TODOs for session 2

- No Spring wiring yet: `bootstrap` has only `main()`. Session 2 adds the Postgres adapter,
  REST controllers, `carrinho.adapters.*` selection and the error mapping in §13.
- `PortCoverageTest.CONTRACT_PENDING` lists the ports whose contract suites arrive with
  their first real adapter; remove each entry as its contract lands.
- `Tracing`, `UnitOfWork` and `ListQueries` need contract suites with their Postgres/OTel
  adapters.
- Not built yet (later sessions by design): sync use cases, invitations and guest passes,
  photos, realtime, notifications and `NotificationRenderer`, telemetry ingest and the
  `events.yaml` schema, the observability decorators, account deletion and export.
- NullAway is not wired in; packages are `@NullMarked` so it can be switched on.
- The Cloud Run workflow deploys `main` on every push; see ADR 0001 before merging.

## Five files to read first

1. `src/main/java/app/meucarrinho/domain/list/ShoppingList.java`: every rule of a trip.
2. `src/main/java/app/meucarrinho/application/lists/FinishPurchaseService.java`: load, change, save, publish.
3. `src/test/java/app/meucarrinho/architecture/ArchitectureTest.java`: the walls.
4. `src/testFixtures/java/app/meucarrinho/testfixtures/lists/ShoppingListRepositoryContract.java`: what the Postgres adapter must pass.
5. `src/test/java/app/meucarrinho/scenario/ShoppingTripScenarioTest.java`: the story, end to end.
