# 0010. Idempotency keys

Date: 2026-10-03 · Status: accepted

## Context

Spec §7 says every mutating call accepts an `Idempotency-Key` header, stored for 24 h in
Redis, and §13 lists `IDEMPOTENCY_KEY_REUSED` under 409/412. Clients retry writes with
backoff (§13), so a retried `POST …/items` must not add a second item. The spec does not
say where the check runs, what is stored, how keys are scoped, or what happens when Redis
is down. It also requires exactly one bean per port (§5, ADR 0007), while the session
prompt asked for Redis with a PostgreSQL fallback.

## Decision

**Port.** `IdempotencyStore` lives in `application.common.port` and has four operations:
- `claim`: atomic. Of several simultaneous claims of a free key, one is `Acquired`. The
  others see `InProgress` or `Completed` for the same request, or `KeyReused` for a
  different one.
- `lookup`: reports the same outcomes without taking the key.
- `complete`: stores the response, even if this store never saw the claim.
- `release`: frees only this request's running claim.

Failures are `IdempotencyError.Unavailable` (ADR 0005). Types:
- `IdempotencyKey` is the account plus the client's value.
- `RequestFingerprint` is a SHA-256 in hex.
- `StoredResponse` holds the status, the headers and the body bytes.

**Lease and retention.** A claim holds the key for a 60 s lease, longer than the slowest
request (sync, 20 s). A completed response is kept for 24 h. A crashed request frees its
key within a minute instead of blocking it for a day.

**Adapters.**
- `adapter.idempotency.redis` uses one string per key with server-side expiry. The claim
  is `SET NX PX`, and release is a compare-and-delete Lua script.
- `adapter.idempotency.postgres` uses the `idempotency_keys` table (V3). The claim is one
  upsert that only takes over an expired row, and expiry follows the core's `Clock`.
- Both translate every `DataAccessException` into `Unavailable`. Both pass
  `IdempotencyStoreContract` on Testcontainers (`valkey/valkey:8`, `postgres:17`).
- The table has an index on `expires_at` for a bulk purge in step 7. Until then, an
  expired row is reused when its key is claimed again.

**Selection.** `carrinho.adapters.idempotency` takes three values:
- `redis` or `postgres` select that adapter's own `@Configuration`.
- `redis-postgres`, the default, selects `bootstrap.FallbackIdempotencyStore`. It builds
  both members through the public static `create` methods on the adapters'
  configurations, so neither member becomes a port bean and PortBeanVerifier still sees
  exactly one. Adapters may not reference each other (§11), so the composite lives in
  bootstrap. `IdempotencyStore` is on the verifier's required list.

**Composite behaviour.**
- It tries Redis first and uses PostgreSQL when Redis returns `Unavailable`.
- When Redis grants a claim, the composite also looks the key up in PostgreSQL. A
  response stored there during an outage then still replays once Redis is back, at the
  cost of one primary-key read per new key.
- `release` goes to both stores.
- It logs once when Redis goes down and once when it recovers.

**HTTP filter.** `bootstrap.IdempotencyFilter` handles POST, PUT, PATCH and DELETE under
`/v1/` when the call carries the header. The REST layer may not see output ports (§11),
which is why it is in bootstrap.
- **Anonymous calls** pass through untouched and get their 401.
- **Key format:** 1 to 128 characters of `A-Z a-z 0-9 . _ : -`. A UUID is recommended.
  Anything else is `400 MALFORMED_REQUEST`.
- **Key scope:** keys belong to one account, so two accounts may use the same value.
- **Fingerprint:** SHA-256 of the method, path and query, `If-Match`, and the body bytes.
- **Only 2xx responses are stored**, with `Content-Type`, `ETag`, `Location` and the body.
  A use case commits all or nothing, so a 4xx or 5xx changed nothing. Running the retry
  again is safe, and the client can fix the request and keep its key. This replaces the
  first proposal, which stored deterministic 4xx too.
- **A replay** returns the stored status, headers and body plus
  `Idempotent-Replayed: true`, a new response header.

**Outcomes:**

| Situation | Response |
|---|---|
| Same key, same request, still running | `409 IDEMPOTENCY_REQUEST_IN_PROGRESS` (new code), `Retry-After: 1` |
| Same key, different request | `409 IDEMPOTENCY_KEY_REUSED` (§13 lists 409 or 412; 412 means preconditions) |
| Both stores unavailable | `503 DEPENDENCY_UNAVAILABLE`, `Retry-After: 1`; the call does not run unprotected |

**Rendering.** The filter hands its `ApiProblem` to Spring's `HandlerExceptionResolver`,
which reaches `ApiExceptionHandler` even without a handler. Its problems therefore render
like every other error, and pick up step 4's `requestId` and `traceId` for free.

**Ordering.** The filter bean has the default, lowest order, so it runs after the
security filters arrive in step 5. `CurrentActor` must already know the caller there.

## Consequences

- A retried write with the same key reaches the use case once. ADR 0003 replays still
  cover retries without a key.
- Lettuce keeps its default timeouts (connect 10 s, command 60 s), as agreed for the
  development stage. If Valkey dies while connected, each keyed write can wait up to the
  command timeout before falling back. Production needs short timeouts, or a circuit
  breaker, before launch (§12 targets).
- Web tests now run every servlet filter (`RestTest`).
- The `Idempotency-Key` request header and the two new codes are not in the OpenAPI
  document yet. Step 4 adds them with `openapi/carrinho-v1.yaml`.
