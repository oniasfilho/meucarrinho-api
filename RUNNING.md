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
who already exist, so restarting never duplicates data.

Ctrl+C stops the API but leaves the containers running, so the next `make run` starts
fast. Run `make down` to stop them.

No environment variables are needed. If you used `env -u DATABASE_URL ./gradlew bootRun`
before, drop it: the app now reads only `CARRINHO_*` variables (see Configuration).

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

How sign-in works:
- mock-oauth2 makes the token's subject the `user` you ask for, and its audience
  `carrinho-api`.
- The API checks the signature, expiry, issuer and audience, then maps the subject to an
  account. The seed created `marina`, `jessica` and `onias` with exactly those subjects.
- It's the same code path Auth0 tokens will take, with no test header or other back door.

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

To run the whole collection from the terminal as a brand-new user, use newman, Postman's
command-line runner, which `npx` downloads on first use:

```bash
npx -y newman run postman/meucarrinho-api.postman_collection.json --env-var user=demo-$(date +%s)
```

Every request should return 2xx, except *Who am I*, which returns 401 because a new user
has no account until *Sign up* runs.

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

## Using it from the web UI

Almost the whole single-user shopping flow works today. A browser client has the
limitations listed under "Before a browser can call it" below.

| Screen | What works | Endpoints |
|---|---|---|
| Sign-in / Perfil | Create or refresh the account after sign-in, read the profile, change preferences (sort order, alerts, haptics, analytics consent) | `PUT /v1/me`, `GET /v1/me`, `PATCH /v1/me/preferences` |
| Início | Home cards with totals, budget remaining and the over-budget flag | `GET /v1/lists?status=active` |
| Nova lista | Create a list with a name, store and budget | `POST /v1/lists` |
| Lista ativa | Full list with derived totals; rename, change store or budget; quick-add bar (`"2 leite 5,49"`) or the item editor; pick or return items; delete with "Desfazer"; duplicate item | `GET/PATCH /v1/lists/{id}`, `POST …/items`, `PATCH`/`DELETE …/items/{itemId}`, `PUT …/picked`, `POST …/restore`, `POST …/duplicate` |
| Card actions | Duplicate a list; delete it and undo | `POST …/duplicate`, `DELETE`, `POST …/restore` |
| "Usar itens da última compra" | Copy the items from the latest (or a chosen) receipt | `POST …/items:import` |
| Finalizar compra | Finish the trip and create a receipt | `POST …/finish` |
| Histórico | Receipts newest first, grouped by month and paged; monthly totals for the month headers | `GET /v1/receipts?cursor=`, `GET /v1/receipts/summary` |
| Recibo / Comprar de novo | Receipt detail; start a new list from a receipt | `GET /v1/receipts/{id}`, `POST …/shop-again` |

Version checks with `If-Match`/`ETag` and retries with `Idempotency-Key` already work.

### Before a browser can call it

- **CORS isn't configured.** A browser on another origin gets blocked. Until step 5 adds
  the spec's allow-list, use a dev-server proxy (Vite or Expo) that forwards `/v1` to
  `http://localhost:8080`.
- **Browser sign-in hasn't been tested.** Tokens come from mock-oauth2 locally, and only
  `make token` has been tested. mock-oauth2 also has an interactive login page, but nobody
  has checked which audience it puts on tokens issued there; the API only accepts
  `carrinho-api`. For now, paste a token from `make token`.
- **No live updates.** There's no realtime channel yet (session 3), so re-fetch the list
  to see someone else's changes.
- **No OpenAPI file yet.** `openapi/carrinho-v1.yaml` is due in step 4. A TypeScript client
  generated from the live http://localhost:8080/v3/api-docs works now, but may still
  change before step 4 freezes the contract.
- **Errors are partial.** They carry `code` and `message`, but `requestId`, `traceId` and
  the final error contract come in step 4.

### Not available yet

| Feature | Arrives |
|---|---|
| Comprar junto: invitations, members, guest joiners, shared lists | Session 3 |
| Live highlights, "Jessica pegou Café" toasts, online presence | Session 3 (realtime) |
| Photos on items | Session 3 |
| Quick-add suggestions with last known prices, frequent items | Catalog, later sessions |
| `GET /v1/me/capabilities`, so the web hides mobile-only features | Session 4 |
| Data export and account deletion (LGPD) | Later sessions |
| Telemetry ingest | Session 4 |

Offline sync and guest import are mobile-only by design (spec §5), so the web won't get
them.

## Troubleshooting

| You see | Why | Fix |
|---|---|---|
| `make token` prints nothing or `curl: (7)` | mock-oauth2 isn't running | `make deps` (or `make run`) |
| 401 `The access token is invalid or expired.` | The token is over an hour old, for another audience, or from another server | `make token` again (Postman refreshes it by itself) |
| 401 `This sign-in has no account yet. Call PUT /v1/me first.` | A `user` the seed didn't create | *Me › Sign up / refresh*, or `PUT /v1/me` with `{"displayName": "..."}` |
| 401 `Sign in to use this endpoint.` | No `Authorization` header | Add `Bearer <token>`; in Postman, keep the collection's auth on *Inherit* |
| 428 `PRECONDITION_REQUIRED` | A list write without `If-Match` | Send the list's current `ETag`; read the list first |
| 412 `VERSION_CONFLICT` | The list changed since you read it; the body has `currentRevision` | Read the list again (*Get list*) and retry |
| 409 `IDEMPOTENCY_KEY_REUSED` | This `Idempotency-Key` was already used for a different request by this user | Use a new key (change `idempotencyKey` in Postman) |
| Startup fails: `Connection refused` to 5432 or 6379 | Postgres or Valkey isn't running | `make deps` |
| Gradle: `Unable to delete directory build/test-results` | Root owns files in `build/` from an old run | `sudo chown -R "$USER" build` |

## Not working yet

- **`make up` (the API in Docker):** the `api` service in `compose.yaml` doesn't pass the
  database, Redis or issuer settings yet (step 8). Use `make run`.
- **Shared lists:** "Compras da semana" isn't shared with Marina and Onias, because
  joining a list needs invitations (session 3).
- **Receipt dates:** seed receipts are dated today, not August and September 2026.
- **Management port:** port 8081 and the actuator don't exist yet (step 7).
- **Sign-in leftovers from step 5:**
  - the web and mobile client platform isn't read from the token yet;
  - there are no real Auth0 tenant values;
  - CORS isn't configured, so a browser front end on another origin can't call the API
    yet. Postman and curl are fine.
