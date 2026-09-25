# House rules for the Meu Carrinho backend

You are implementing the backend described in docs/spec.md. Follow it. If the spec is
silent or ambiguous, stop and ask instead of guessing.

## Stack
- Java 25, Spring Boot 4.1.x (latest patch), Gradle Kotlin DSL + version catalog.
- One Gradle module. Root package: app.meucarrinho.

## Architecture (non-negotiable)
- Packages: domain, application.<capability> (ports in a `port` subpackage),
  api.rest, api.realtime, adapter.<concern>.<vendor>, bootstrap.
- domain and application never import Spring, Micrometer, OpenTelemetry, JDBC or any
  vendor SDK. ArchUnit tests enforce this. Never weaken, skip or delete them.
- Controllers depend only on input-port interfaces. Adapters implement output ports and
  translate vendor errors into the port's sealed error types.
- Each adapter package has one @Configuration guarded by
  @ConditionalOnProperty("carrinho.adapters.<port>" = "<name>").
- Every output port has an in-memory fake and an abstract contract test in
  src/testFixtures. Every real adapter must pass that contract test.
- Money = long minor units + currency (BRL only). IDs = UUIDv7.
- Records for value objects and events; sealed interfaces for errors and event families.
- Side effects (e-mail, push, realtime, analytics) never run inside a cart-write
  transaction; they run from domain-event listeners.

## Working rhythm
- Before writing code, list the steps for this session and wait for my OK.
- One step at a time. After each step: run the tests, list the files you changed,
  explain in at most 5 sentences how the step fits the architecture, then wait for "next".
- Tests with or before code. `make test` must stay under 30 seconds and need no Docker.
- Do not add a dependency, endpoint, table or config key the spec does not call for
  without asking.
- Do not change an existing port signature without asking; if you must, explain why
  and write an ADR.
- One commit per step, conventional commit messages.
- At the end: update HANDOFF.md and add one ADR in docs/adr per decision the spec did
  not dictate.
