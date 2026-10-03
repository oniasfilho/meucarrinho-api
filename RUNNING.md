# Running the project (as of session 2, step 3b-2)

A quick guide to what runs today. Session 2, step 8 replaces it with the spec §14 quick
start (`make up`, `make seed`, `make token`).

## Prerequisites

- Docker, for Postgres and Valkey and for the integration tests.
- Any JDK 17+ to start Gradle. Gradle downloads the Java 25 toolchain if it's missing.

## 1. Run the tests

```bash
make test   # about 260 unit, use-case, web and ArchUnit tests on in-memory fakes, ~10 s, no Docker
make it     # about 55 integration tests on Testcontainers (Postgres 17, Valkey 8), ~1.5 min
```

If Gradle fails with `Unable to delete directory build/test-results`, root owns files in
`build/` from an old run. Fix it once with `sudo chown -R "$USER" build`.

## 2. Run the API on your machine

Start the two services the API needs:

```bash
docker compose up -d --wait postgres valkey
```

Then start the API. **Your shell exports `DATABASE_URL` for another project (Neon), and
`application.yaml` reads the same variable.** Leave it out of this process:

```bash
env -u DATABASE_URL ./gradlew bootRun
```

Otherwise startup fails with `'url' must start with "jdbc"`. To point at another
database on purpose, set a JDBC URL, for example
`DATABASE_URL=jdbc:postgresql://localhost:5432/carrinho`.

On start, Flyway applies migrations V1–V3 to the local `carrinho` database, and the API
listens on port 8080.

| What | Where |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Postgres | `localhost:5432`, database and user `carrinho`, no password (trust auth, local only) |
| Valkey | `localhost:6379` |

Stop the API with Ctrl+C. Then stop or wipe the services:

```bash
docker compose stop postgres valkey   # keep the data
docker compose down -v                # wipe the volumes
```

## 3. What to expect

- **Every `/v1` endpoint answers `401 UNAUTHENTICATED`.** Authentication comes in
  session 2, step 5, and there is deliberately no dev header or back door. You can browse
  the API in Swagger, but you can't call it successfully yet.
- To see the endpoints work end to end, read and run the web tests. They sign in a test
  user and call the real controllers on the fakes:
  - `src/test/java/app/meucarrinho/api/rest/lists/ItemsApiTest.java`
  - `src/test/java/app/meucarrinho/bootstrap/IdempotencyFilterTest.java` (retries with
    `Idempotency-Key`)

  Run one with `./gradlew test --tests '*IdempotencyFilterTest'`.
- There is no seed data and no `make token` yet (step 8).

## 4. Configuration you may want to change

| Key (env var) | Default | Notes |
|---|---|---|
| `spring.datasource.url` (`DATABASE_URL`) | `jdbc:postgresql://localhost:5432/carrinho` | Must be a JDBC URL |
| `spring.datasource.username` / `password` (`DATABASE_USER` / `DATABASE_PASSWORD`) | `carrinho` / empty | |
| `spring.data.redis.url` (`REDIS_URL`) | `redis://localhost:6379` | |
| `carrinho.adapters.persistence` | `postgres` | `memory` only in tests |
| `carrinho.adapters.idempotency` | `redis-postgres` | `redis`, `postgres`, or Redis with PostgreSQL fallback |

To run without Valkey, set the idempotency store to Postgres only:

```bash
env -u DATABASE_URL CARRINHO_ADAPTERS_IDEMPOTENCY=postgres ./gradlew bootRun
```

## Not working yet

- **`make up` (the API in Docker):** the `api` service in `compose.yaml` doesn't set
  `DATABASE_URL` or `REDIS_URL` yet, so it can't reach Postgres or Valkey (step 8).
- **Port 8081:** management and actuator don't exist yet (step 7).
- **Missing services:** Grafana, Mailpit, MinIO and mock-oauth2 are in `compose.yaml`,
  but nothing uses them yet.
