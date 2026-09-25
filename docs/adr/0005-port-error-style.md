# 0005. How ports report failure

Date: 2026-09-25 · Status: accepted

## Decision

- Ports that call a vendor return `Result<T, SealedError>` (`NotificationError`,
  `StorageError`, `IdentityError`, `PaymentError`, `GuestPassError`, `ChangeLogError`,
  `CatalogError`, `RealtimeError`, `AuditError`, `WebhookError`).
- Repositories throw the sealed `PersistenceException` (`ConcurrentModification`,
  `StoreUnavailable`) instead, as the spec's `ShoppingListRepository.save` comment says.
  A failed save must also abort the surrounding unit of work, which an exception does and
  a returned value does not.
- `FeatureFlags`, `ProductAnalytics`, `BusinessMetrics` and `PresenceTracker` have no error
  type: by contract they never fail the caller (flags fall back, telemetry drops and counts).
- Domain rules return `Result<T, ListError>` and similar; value objects throw
  `IllegalArgumentException` from their constructors, which the API maps to
  `422 VALIDATION_FAILED` in session 2.
