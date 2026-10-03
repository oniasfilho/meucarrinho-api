# 0007. How bootstrap wires the core and fails fast

Date: 2026-10-03 · Status: accepted

## Context

The use-case services are plain Java (§3), so something has to declare them as Spring
beans. Spec §5 says "startup fails fast if a port has zero or two beans". But most output
ports have no adapter until sessions 3 and 4, so a literal "exactly one bean per port"
rule would stop the app today. `Clock` and `IdGenerator` need production implementations
but have no vendor to select.

## Decision

- **`bootstrap.CoreConfiguration` declares every service** by its concrete type. Callers
  inject them by input-port interface. The services stay free of Spring annotations.
- **`SystemClock` and `Uuid7IdGenerator`** live in `bootstrap`, unconditionally, with no
  `carrinho.adapters` key: there is nothing to swap. IDs are UUIDv7 from `SecureRandom`,
  stamped with the core's `Clock`. The seed mode (step 8) will replace the clock behind
  a condition.
- **`PortBeanVerifier`** is a `BeanFactoryPostProcessor`, so it runs on bean definitions
  before any adapter connects to anything:
  - Every interface in `application.*.port` may have at most one bean.
  - The ports the wired use cases need must have exactly one. Today these are the six
    persistence ports plus `Clock` and `IdGenerator`.
  - The error names each port and the key that selects it.
  - Ports with no adapter yet may have none. Each session adds the ports it starts using
    to the required list.
- **Tests:**
  - `CoreContextTest` boots `MeuCarrinhoApplication` with
    `carrinho.adapters.persistence=memory` on the testFixtures fakes. It turns off the
    DataSource and Modulith outbox auto-configuration, so it needs no Docker. It also
    asserts that every input port resolves to exactly one bean.
  - `PortBeanVerifierTest` covers zero and two beans with `ApplicationContextRunner`.
  - Both need `spring-boot-starter-test` on the `test` suite.

## Consequences

A misconfigured swap fails at startup with a message, not on the first request. A new use
case that is not wired fails `CoreContextTest`. The required-port list is one more thing to
update when a capability starts using a port; forgetting it still fails at injection, only
with Spring's less specific message.
