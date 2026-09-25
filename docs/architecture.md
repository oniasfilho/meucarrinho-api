# Architecture

Hexagonal: the core (domain and application) knows no framework or vendor, and everything
outside talks to it through ports it owns. Arrows point in the direction of dependency;
adapters depend on ports, never the reverse. The rules are enforced by
`src/test/java/app/meucarrinho/architecture/ArchitectureTest.java`.

```mermaid
flowchart LR
  subgraph Driving["Driving side (session 2+)"]
    REST["api.rest<br>controllers"]
    RT["api.realtime<br>STOMP"]
    TEST["Tests today:<br>ShoppingTripScenarioTest"]
  end

  subgraph Core["Application core (session 1)"]
    direction TB
    IN["Input ports<br>CreateShoppingList, AddItem, QuickAddItem,<br>PickItem, FinishPurchase, ShopAgain,<br>GetReceiptHistory, UpsertAccount, ..."]
    POL["Policies<br>ListAccessPolicy, CapabilityPolicy"]
    DOM["Domain<br>ShoppingList, Item, ListTotals, Receipt,<br>Account, Invitation, Entitlement,<br>DomainEvent"]
    OUT["Output ports"]
    IN --> POL --> DOM
    IN --> OUT
  end

  subgraph Driven["Output ports and their adapters"]
    P1["ShoppingListRepository, ReceiptRepository,<br>AccountRepository, InvitationRepository,<br>EntitlementRepository, ListQueries,<br>ChangeLog, UnitOfWork"]
    P2["IdentityDirectory, GuestPassStore"]
    P3["FeatureFlags, CatalogSource"]
    P4["EmailSender, PushSender, PhotoStorage"]
    P5["ListChangeBroadcaster, PresenceTracker"]
    P6["ProductAnalytics, BusinessMetrics, Tracing"]
    P7["PaymentGateway, BillingEventSource (dormant)"]
    P8["Clock, IdGenerator, DomainEventPublisher, AuditTrail"]
  end

  subgraph Adapters["Adapters"]
    FAKE["testFixtures: an in-memory fake for every port<br>+ contract suites (session 1)"]
    PG["Postgres, Auth0, OTel (session 2)"]
    S3["Outbox, Redis, STOMP, MinIO/S3, SMTP (session 3)"]
    S4["PostHog, curated catalog, Mongo (session 4)"]
  end

  REST --> IN
  RT --> IN
  TEST --> IN
  OUT --- P1 & P2 & P3 & P4 & P5 & P6 & P7 & P8
  FAKE -. implements .-> P1 & P2 & P3 & P4 & P5 & P6 & P7 & P8
  PG -. implements .-> P1 & P2 & P6
  S3 -. implements .-> P4 & P5 & P8
  S4 -. implements .-> P3 & P6 & P1
```

## Packages

| Package | Holds | May depend on |
|---|---|---|
| `domain` | Aggregates, value objects, domain events, `QuickAddParser` | JDK, JSpecify |
| `application.<capability>` | Input ports and their services; output ports in `port` | `domain`; other capabilities only through their `api` subpackage; `application.common` and `application.telemetry.port` (ADR 0002) |
| `api.rest`, `api.realtime` | Controllers (session 2) | Input ports only |
| `adapter.<concern>.<vendor>` | One port implementation each (session 2+) | Ports, domain, its own vendor SDK |
| `bootstrap` | `main()`, adapter selection | Everything |

## What each session adds

| Session | Adds |
|---|---|
| 1 | Skeleton, ArchUnit rules, domain, every port, fakes and contract suites, use cases, scenario test |
| 2 | Postgres adapter and Flyway, REST API and Problem Details, OpenAPI, Auth0 and mock auth, OTel traces, health probes, seed data |
| 3 | Outbox listeners, op-log sync and guest import, invitations and guest passes, realtime, photos on MinIO, e-mail to Mailpit, capability checks at the edges |
| 4 | Product events and PostHog, telemetry ingest, dashboards, PostHog flags, curated catalog, audit trail, LGPD endpoints, resilience, Mongo adapter |
