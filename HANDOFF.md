# Session 2 in progress: read this first

Session 2 follows the "Session 2: Running API" page of the Notion Build guide. Its step 3
is split into 3a and 3b here, and 3b into two commits. Step 9 of session 2 rewrites this
whole file. Until then, this section says where things stand. The session 1 handoff
below is kept unchanged, so its "Ports and adapters" table is out of date: Postgres now
backs the persistence ports.

## Where things stand (branch `session/2`)

| Step | State | Commit |
|---|---|---|
| 1. Postgres schema, repositories, contract suites on Testcontainers | done | a43cee0 |
| 2. `SpringTransactionUnitOfWork`, Modulith outbox publisher | done | 16f8ce0 |
| Fix: Modulith JDBC starter on the main runtime classpath | done | a93da09 |
| 3a. If-Match in use cases, `ListActiveLists`, `GetAccount`, core wiring | done | 690ac6a |
| 3b-1. REST surface, If-Match parsing, `/swagger-ui` | in progress | |
| 3b-2. Idempotency store, Redis and Postgres adapters, HTTP filter | next | |
| 4–9. Error contract, auth, telemetry, operations, DX, handoff | not started | |

**After 3b-2, resume at session 2, step 4 (error contract).** Session 3 starts only after
step 9.

Local notes:

- `build/` holds files owned by root from a run on 2026-09-29, so `make test` cannot
  clean it. Run `sudo chown -R "$USER" build` once.
- `compose.yaml` and `docs/spec.md` carry uncommitted edits by the user (MinIO image
  switched to `pgsty/minio`). Leave them out of commits unless asked.
- `target/` is an untracked leftover. Ignore it.

## Decisions agreed in session 2 (do not ask again)

From the session prompt:

1. If-Match is checked in the use case. Every list-write input port takes
   `long expectedVersion`. A stale version returns `ListError.VersionConflict` with the
   current revision. See [ADR 0006](docs/adr/0006-list-writes-carry-their-version.md).
2. Two new input ports: `ListActiveLists` (home cards) and `GetAccount` (by `AccountId`
   and by `ExternalRef`). See ADR 0006.
3. Receipts paging: `GET /v1/receipts/summary?from=&to=` maps onto `GetReceiptHistory`.
   `GET /v1/receipts?cursor=` takes an opaque cursor encoding the start month; each page
   covers a fixed window of months going back and returns `nextCursor`.
   `GetReceiptHistory` does not change. ADR comes in 3b-1.
4. Idempotency:
   - A new `IdempotencyStore` port in `application.common.port`, with a fake and a
     contract suite.
   - Adapters `adapter.idempotency.redis` and a Postgres table adapter.
   - A composite in `bootstrap` that tries Redis first and falls back to Postgres.
   - The HTTP filter lives in `bootstrap`.
   - Config key: `carrinho.adapters.idempotency`.
   - ADR comes in 3b-2.
5. Not in session 2: `GET /v1/me/capabilities` (session 4) and the file-backed flags
   adapter (session 3). `CapabilityService` is not wired. `GET /v1/me` has no
   capabilities field; adding it later is compatible under §13.
6. Each controller implements an API interface annotated with `@HttpExchange`. The same
   interfaces produce `openapi/carrinho-v1.yaml` (not `contracts/`) and the typed Java
   client used by the web tests.
7. Auth uses mock-oauth2 by default. The Auth0 tenant values (issuer, audience, client
   IDs per `azp`) are not available yet and are read from environment variables.
   `make token` mints from mock-oauth2.
8. The management port stays 8081, as in the spec. Its clash with the Expo web dev server
   is a known gap.
9. `IdentityDirectory` gets its contract suite with the Auth0 Management API adapter in
   session 4, step 6 (see `PortCoverageTest`). Do not build it in session 2.

From step 3a (details in ADRs 0006 and 0007):

- A list write checks, in order:
  1. Load the list.
  2. `ListAccessPolicy`: strangers get `ListNotFound` and never see the revision.
  3. Replay.
  4. Version.
  5. Domain rule.
- A retried add, quick-add or duplicate with an item ID the list already holds, and a
  retried finish with the same receipt ID, skip the version check (ADR 0003 retries).
- A save that loses the optimistic-lock race is returned as the same `VersionConflict`.
- `ListActiveLists` returns `ActiveListCard`, because `api` may not import
  `port.ListCard`. `GetAccount` adds `AccountError.UnknownIdentity`.
- `bootstrap.CoreConfiguration` wires every service. `SystemClock` and
  `Uuid7IdGenerator` live in `bootstrap` with no config key.
- `PortBeanVerifier` fails startup on two beans for any output port, or on zero beans for
  a port the wired core needs. Add each newly used port to its required list.
- `spring-boot-starter-test` is on the `test` suite. `CoreContextTest` boots the app on
  the fakes with no Docker.

For step 3b:

- **Actor before step 5.** Controllers get the account from a `CurrentActor` interface in
  `api.rest`. Until step 5, the only production implementation (in `bootstrap`) has no
  actor, so every endpoint answers 401 `UNAUTHENTICATED`. Web tests supply their own.
  There is no dev header and no back door. Step 5 replaces the bootstrap implementation
  with one built on the verified token and `GetAccount.byIdentity`.
- **Minimal error mapping.** Controllers turn each `Result` error into a status and a
  stable `code` through one small `@RestControllerAdvice` in `api.rest`. Step 4 grows it
  into the full §13 contract (`requestId`, `traceId`, field errors, hostile input).
  `VERSION_CONFLICT` is 412, with `currentRevision`.
- **Missing If-Match** on a list write returns `428 PRECONDITION_REQUIRED`, a new code,
  compatible under §13. A malformed one returns 400 `MALFORMED_REQUEST`.
- **ETag.** Responses that carry a list send `ETag: "<version>"`. `If-Match` accepts the
  version quoted or bare.
- **Adding items.** `POST /v1/lists/{id}/items` takes either `{"text": "2 leite 5,49"}`
  (quick-add) or structured fields, never both.
- **Dependencies approved:** Spring MVC starter, validation starter, springdoc-openapi,
  Spring Data Redis (Lettuce), Testcontainers support for Redis/Valkey.

## Open questions for later steps

- **Step 5.** The Notion checkpoint expects "another user's list gets 403 NOT_A_MEMBER".
  But `ListAccessPolicy` (session 1, tested by `ShoppingListTest`) answers strangers with
  `ListNotFound`, so they cannot tell a hidden list from a missing one, and ADR 0006
  relies on that. `ListError.NotAMember` exists but nothing returns it. Decide at step 5:
  either the checkpoint changes to 404 `LIST_NOT_FOUND`, or the policy changes and
  ADR 0006 is revisited.

## Known gaps to carry into the step 9 handoff

- The seed cannot create the shared "Compras da semana" list. Joining needs invitations
  (session 3, step 4); never insert members through SQL or a back door. The seed creates
  each user's own lists and receipts.
- The seed backdates the August and September 2026 receipts with its own settable
  `Clock` in seed mode.
- Auth0 is not proven against a real tenant.
- The management port 8081 clashes with the Expo web dev server (decision 8).
- NullAway is not wired in yet.
- The Cloud Run workflow deploys `main` on every push (see ADR 0001 before merging).

---

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
