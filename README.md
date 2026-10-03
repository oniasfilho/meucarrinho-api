# Meu Carrinho API

Backend for Meu Carrinho, an offline-first shopping list app. Java 25, Spring Boot 4.1,
Gradle, hexagonal architecture: the core knows no framework or vendor, and every
database, identity provider, e-mail service or telemetry tool sits behind a port.

- What to build: [`docs/spec.md`](docs/spec.md)
- Where things stand: [`HANDOFF.md`](HANDOFF.md)
- How the pieces connect: [`docs/architecture.md`](docs/architecture.md)
- Why: [`docs/adr`](docs/adr)
- House rules for LLM sessions: [`AGENTS.md`](AGENTS.md)

## Running it

Prerequisites: Docker, and any JDK 17+ to start Gradle (the Java 25 toolchain is
downloaded if missing). Details, demo users and the Postman collection:
[`RUNNING.md`](RUNNING.md).

```bash
make run                 # Postgres, Valkey and mock-oauth2 in Docker, then the API on :8080 with demo data
make token user=marina   # a sign-in token for Postman or curl
make test                # unit, use-case, web and architecture tests with in-memory fakes; no Docker
make it                  # integration tests on Testcontainers
```
