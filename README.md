# Meu Carrinho API

Backend for Meu Carrinho, an offline-first shopping list app. Java 25, Spring Boot 4.1,
Gradle, hexagonal architecture: the core knows no framework or vendor, and every
database, identity provider, e-mail service or telemetry tool sits behind a port.

- What to build: [`docs/spec.md`](docs/spec.md)
- Where things stand: [`HANDOFF.md`](HANDOFF.md)
- How the pieces connect: [`docs/architecture.md`](docs/architecture.md)
- Why: [`docs/adr`](docs/adr)
- House rules for LLM sessions: [`AGENTS.md`](AGENTS.md)

## Running the tests

Prerequisite: any JDK 17+ to start Gradle; the Java 25 toolchain is downloaded if missing.

```bash
make test   # unit, use-case and architecture tests with in-memory fakes; no Docker
make it     # integration tests on Testcontainers (from session 2)
make up     # the local stack from compose.yaml (the API joins in session 2)
```
