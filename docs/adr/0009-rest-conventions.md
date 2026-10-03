# 0009. REST conventions the spec left open

Date: 2026-10-03 · Status: accepted

## Context

Session 2, step 3b puts the core behind HTTP before authentication (step 5) and before
the full error contract (step 4). Spec §3, §7 and §13 fix the shape: API interfaces,
If-Match on list writes, Problem Details with a stable `code`. They leave the details
below open.

## Decision

**Interfaces.** Each controller implements an API interface annotated with
`@HttpExchange`. Spring MVC maps it on the server, `HttpServiceProxyFactory` turns it into
the typed client the web tests use, and springdoc reads it for `/v3/api-docs` and
`/swagger-ui`. `POST /v1/receipts/{id}/shop-again` lives in `ShopAgainApi` in the lists
package: it creates a list, just as `ShopAgain` lives in the lists capability (ADR 0002).
This keeps the REST packages free of cycles.

**Who is calling.** Controllers ask `api.rest.CurrentActor` for the account. Until step
5, `bootstrap` provides one that never has an actor, so every endpoint answers
`401 UNAUTHENTICATED`. Web tests swap in `TestActor`. There is no development header or
other back door.

**Versions.**
- Responses carrying a list send `ETag: "<version>"`.
- List writes need `If-Match`, with the version quoted or bare.
- A missing `If-Match` returns `428 PRECONDITION_REQUIRED`, a new code (§13 allows new
  codes).
- `*`, weak tags, lists of tags and anything non-numeric return `400 MALFORMED_REQUEST`.
- A stale version returns `412 VERSION_CONFLICT` with `currentRevision`. §13 lists both
  409 and 412; its example uses 412.

**Status codes.**
- `201 Created` with `Location` when something new is made: create list, duplicate list,
  shop again, and finish (which makes a receipt).
- `200` for every other write, including `DELETE`, which returns the deleted list so the
  client has the version it needs to restore it.

**Bodies.**
- **Adding items.** `POST …/items` takes either `text` (quick-add, parsed by the shared
  grammar) or `name` with optional `quantity` (default 1 UN), `unitPrice` and `note`.
  Sending both returns `422` with code `TEXT_OR_FIELDS`.
- **PATCH bodies.** An absent field is kept, `null` clears it, and a value sets it. Jackson
  3 reads absent and `null` the same way for `Optional`, so PATCH fields that can be
  cleared use `api.rest.Patch<T>`, which maps onto the domain's `Change<T>`. Preferences
  cannot be cleared, so there `null` also keeps the value.
- **Amounts** are `{"minorUnits": 549, "currency": "BRL"}`. Quantities are
  `{"amount": 1.2, "unit": "KG"}`. Enums use the domain names.
- **Collections** are wrapped in an object (`{"lists": […]}`), so fields can be added
  later.
- **Unknown request fields** are ignored (§13).

**Errors until step 4.** One `@RestControllerAdvice` renders `ApiProblem` as Problem
Details with `code` and `message`. `ApiErrors` maps every use-case error with exhaustive
switches:

| Code | Status |
|---|---|
| `LIST_NOT_FOUND`, `ITEM_NOT_FOUND`, `RECEIPT_NOT_FOUND`, `NO_PAST_PURCHASE`, `ACCOUNT_NOT_FOUND` | 404 |
| `NOT_A_MEMBER`, `OWNER_ONLY` | 403 |
| `LIST_NOT_ACTIVE`, `ITEM_DELETED`, `NOTHING_PICKED`, `LIST_ID_TAKEN` | 409 |
| `RESTORE_WINDOW_EXPIRED`, `ACCOUNT_DELETED` | 410 |
| `VERSION_CONFLICT` | 412 |
| `LIMIT_EXCEEDED`, `VALIDATION_FAILED` (with `errors[].field` and `code`) | 422 |
| `MALFORMED_REQUEST` (also IDs that are not UUIDv7) | 400 |
| `UNAUTHENTICATED` | 401 |
| `PRECONDITION_REQUIRED` | 428 |

Error messages never echo request content.

## Consequences

Step 4 extends the same advice with `requestId`, `traceId`, the hostile-input test and
`500 INTERNAL`, so no controller changes. Step 5 replaces one bean in `bootstrap`. Every
code above is part of the contract the apps will branch on. Renaming one after the
OpenAPI file is published is a breaking change.
