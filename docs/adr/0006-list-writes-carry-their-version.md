# 0006. List writes carry the version they were based on

Date: 2026-10-03 · Status: accepted

## Context

Spec §7 says every list write carries `If-Match: <version>`, and §13 answers a stale one
with `VERSION_CONFLICT` and `currentRevision`. The spec does not say where the check runs,
how it meets the safe retries of ADR 0003, or what a save that loses a race returns. §3
rule 1 also makes every use case an input port, and two endpoints in §7 had none:
`GET /v1/lists?status=active` and `GET /v1/me`.

## Decision

- **The use case checks the version, not the controller.** This follows the feature-slice
  guide's pick-item example. These 13 input ports take `long expectedVersion` right after
  the actor: update, delete and restore list; add, quick-add, edit, pick, unpick, remove,
  restore and duplicate item; import items; finish. Create list, duplicate list and shop
  again make a new list, so they take no version. A mismatch is
  `ListError.VersionConflict(listId, currentRevision)`, decided by
  `ShoppingList.requireVersion`.
- **Order of checks:**
  1. Load the list (`ListNotFound`).
  2. `ListAccessPolicy` (strangers get `ListNotFound` and never learn the revision).
  3. Replay.
  4. Version.
  5. Domain rule.
- **Replays skip the version check.** A retried add, quick-add or duplicate whose
  client-supplied item ID the list already holds, and a retried finish with the receipt
  ID the list already finished with, return the earlier result without saving, whatever
  version they carry. Without this, every retry from ADR 0003 would become a conflict,
  because it carries the version from before the first attempt. Other writes (pick, edit
  and the rest) rely on `Idempotency-Key` (step 3b).
- **A lost race is the same conflict.** The repository's optimistic lock still guards the
  save. A `PersistenceException.ConcurrentModification` on that list is returned as
  `VersionConflict` with the stored version, after the unit of work has rolled back. A
  conflict on another aggregate, such as a receipt ID already taken, still propagates.
  The API cannot see `PersistenceException`, which lives in a port package.
- **Import items** looks up its receipt inside the versioned write, so the order above
  holds there too.
- **`ListActiveLists`** (`application.lists`) returns `ActiveListCard`, a copy of
  `port.ListCard`, because `api` may not import output-port packages (§11).
  `ListQueries` is unchanged.
- **`GetAccount`** (`application.accounts`) has `byId` and `byIdentity`. An identity with
  no account is the new `AccountError.UnknownIdentity(ExternalRef)`. A deleted account
  is `AccountDeleted` from both lookups, so a deleted user's token cannot act.

## Consequences

This is the only input-port signature change in session 2. Clients send the version from
their last response. The controller only turns the header into a `long` (step 3b), and
the error advice maps `VersionConflict` to a problem with `currentRevision` (step 4).
