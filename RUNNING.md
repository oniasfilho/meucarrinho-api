# Running the project

How to run the API on your machine and try it by hand with Postman or curl, with demo
data and real sign-in tokens. Session 2, step 8 folds this into the spec §14 quick start.

## Prerequisites

- Docker.
- Any JDK 17+ to start Gradle. Gradle downloads the Java 25 toolchain if it's missing.

## 1. Start the API with demo data

```bash
make run
```

This does two things:

1. Starts Postgres, Valkey and mock-oauth2 (a local sign-in server standing in for Auth0)
   with Docker Compose.
2. Runs the API on http://localhost:8080 with `carrinho.seed.enabled=true`.

On first start, Flyway creates the schema and the demo seed loads through the real use
cases. The log says `Demo seed: created marina, jessica, onias`. Later starts skip people
who already exist, so restarting never duplicates data. Stop with Ctrl+C.

| Who | What they have |
|---|---|
| `marina` | "Feira de domingo" (budget R$ 120, 1 item picked); "Churrasco sábado" (over budget); one receipt, "Mercado do mês" |
| `jessica` | "Compras da semana" (her own list for now); one receipt, "Padaria" |
| `onias` | "Farmácia" (budget R$ 80) |

## 2. Get a token

```bash
make token                 # Marina
make token user=jessica
make token user=rosa       # someone new: call PUT /v1/me first to create the account
```

It prints a JWT that is valid for one hour. Send it as `Authorization: Bearer <token>`.

## 3. Postman

Import `postman/meucarrinho-api.postman_collection.json`.

- **Sign-in is automatic.** Before each request, the collection fetches a token for its
  `user` variable (default `marina`). Change `user` to switch people.
- **Folders follow a trip:**
  1. Me
  2. Lists
  3. Items
  4. Checkout
  5. Receipts
- **For a seeded user, start with *Lists › Active lists*.** It sets `listId`.
- **For a new user:** run *Me › Sign up / refresh* first.
- **List writes need `If-Match`.** The collection copies each response's `ETag` into
  `etag`, and the list and item you last touched into `listId` and `itemId`, so you can
  click through in order.
- **To see idempotency at work,** send *Items › Add item, retry-safe* twice. The second
  response carries `Idempotent-Replayed: true`, and no second item is created.

Swagger UI (http://localhost:8080/swagger-ui) lists every endpoint, but it doesn't sign
in for you: paste a token from `make token` into **Authorize**.

## 4. curl

```bash
TOKEN=$(make -s token user=marina)
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/v1/me
curl -s -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/v1/lists?status=active'
```

## 5. Tests

```bash
make test   # ~260 unit, use-case, web and ArchUnit tests on in-memory fakes, ~10 s, no Docker
make it     # ~57 integration tests on Testcontainers (Postgres, Valkey, mock-oauth2), ~2 min
```

If Gradle fails with `Unable to delete directory build/test-results`, root owns files in
`build/` from an old run. Fix it once with `sudo chown -R "$USER" build`.

## Configuration

Every value has a local default, so none of these is needed with `make run`.

| Env var | Default | |
|---|---|---|
| `CARRINHO_DATABASE_URL` | `jdbc:postgresql://localhost:5432/carrinho` | Must be a JDBC URL |
| `CARRINHO_DATABASE_USER` / `CARRINHO_DATABASE_PASSWORD` | `carrinho` / empty | |
| `CARRINHO_REDIS_URL` | `redis://localhost:6379` | |
| `CARRINHO_AUTH_ISSUER_URI` | `http://localhost:8090/default` | mock-oauth2; the Auth0 tenant in staging and prod |
| `CARRINHO_AUTH_AUDIENCE` | `carrinho-api` | Tokens for any other audience get 401 |
| `CARRINHO_ADAPTERS_IDEMPOTENCY` | `redis-postgres` | Or `postgres` to run without Valkey |

The variables carry a `CARRINHO_` prefix, so a generic `DATABASE_URL` exported for another
project can't break startup.

To stop or wipe the services: `make down` (keeps data) or
`docker compose --profile offline down -v` (wipes it).

## Not working yet

- **`make up` (the API in Docker):** the `api` service in `compose.yaml` doesn't pass the
  database, Redis or issuer settings yet (step 8). Use `make run`.
- **Shared lists:** "Compras da semana" isn't shared with Marina and Onias, because
  joining a list needs invitations (session 3).
- **Receipt dates:** seed receipts are dated today, not August and September 2026.
- **Management port:** port 8081 and the actuator don't exist yet (step 7).
