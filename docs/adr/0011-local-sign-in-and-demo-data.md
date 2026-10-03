# 0011. Local sign-in and demo data ahead of steps 5 and 8

Date: 2026-10-03 · Status: accepted

## Context

After step 3b-2 every endpoint answered 401: ADR 0009 deferred authentication to step 5
and forbade a development header or other back door. The API could not be tried by hand.
The user asked to test it with Postman and mock data now. Spec §14 already names the way
to do that without a back door: mock-oauth2 issues tokens locally, `make token` prints
one, and a seed loads the prototype's world through the use cases.

## Decision

**Sign-in (the core of step 5, brought forward).**
- `spring-boot-starter-security-oauth2-resource-server` verifies bearer JWTs: signature,
  expiry, issuer and audience.
  - Issuer: `CARRINHO_AUTH_ISSUER_URI`, default mock-oauth2 at
    `http://localhost:8090/default`. Keys are fetched on the first token, so the API
    starts without it.
  - Audience: `CARRINHO_AUTH_AUDIENCE`, default `carrinho-api`.
- `bootstrap.SecurityConfiguration`: stateless, with no sessions, cookies, CSRF or login
  page. Every request is permitted at the filter level:
  - With no token, the controller's `CurrentActor.require()` answers
    `401 UNAUTHENTICATED`, as before.
  - The public endpoints of later sessions need no security change.
  - A bad, expired or wrong-audience token is refused before any controller with the
    same problem, rendered through the MVC exception resolvers.
- `bootstrap.TokenActor` implements `CurrentActor`. The identity is
  `ExternalRef("auth0", sub)`: mock-oauth2 stands in for Auth0, so the subjects share a
  namespace. The account comes from `GetAccount.byIdentity`. `CurrentActor` gains
  `identity()`, and an identity without an account is told to call `PUT /v1/me`.
- `PUT /v1/me` (spec §7) creates or refreshes the caller's account from the token's
  identity. The body carries `displayName` and an optional `email`.
- The idempotency filter runs after the security filters, so it sees the caller
  (ADR 0010).

**Demo data (part of step 8).**
- `bootstrap.DemoSeed` runs when `carrinho.seed.enabled=true`, a new key that is off by
  default. It loads Marina, Jessica and Onias with their lists and receipts through
  `UpsertAccount`, `CreateShoppingList`, `QuickAddItem`, `PickItem` and `FinishPurchase`,
  never SQL.
- A person who already has an account is skipped, so it can run on every start.
- Their token subjects are `marina`, `jessica` and `onias`.

**Developer commands.**
- `make deps` starts Postgres, Valkey and mock-oauth2.
- `make run` runs the API with the seed.
- `make token user=<name>` prints a token. mock-oauth2 makes the client ID the subject and
  the scope the audience.
- `postman/meucarrinho-api.postman_collection.json` signs in by itself and keeps `etag`,
  `listId`, `itemId` and `receiptId` current. A newman run as a new user returns 2xx for
  every request after sign-up.

**Environment variables.** `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` and
`REDIS_URL` become `CARRINHO_*`. A generic `DATABASE_URL` holding a non-JDBC URL from
another project broke startup. No deploy configuration used the old names.

## Consequences

ADR 0009's "401 until step 5" no longer holds. Step 5 still owes:
- the `azp` claim mapped to the client platform (spec §5, for capabilities);
- real Auth0 tenant values;
- the JWKS cache and timeouts from §13;
- CORS;
- the 403/404 question for strangers.

Step 8 still owes the Docker `make up` path, receipts back-dated to August and September,
and the shared "Compras da semana", which needs invitations (session 3).
