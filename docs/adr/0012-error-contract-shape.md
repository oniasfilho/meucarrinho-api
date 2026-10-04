# 0012. The error contract's shape, ahead of limits and tracing

Date: 2026-10-04 · Status: accepted

## Context

Session 2, step 4 grows the minimal advice from ADR 0009 into the full spec §13 contract:
`requestId`, `traceId`, a clean catch-all for anything unmapped, and a hostile-input test.
§13 also covers rate limiting, request-size limits and real timeouts/retries; those need
Bucket4j and Redis wiring that does not exist yet, so the user scoped step 4 to the
Problem Details shape alone and left limits for a later step.

## Decision

**`requestId` and `traceId`.** `bootstrap.RequestCorrelationFilter` runs first in the
filter chain (`@Order(HIGHEST_PRECEDENCE)`), ahead of security and idempotency, and mints
or echoes `X-Request-Id` into `api.rest.RequestCorrelation`, a request-scoped holder.
`ApiExceptionHandler` reads it for every problem. There is no real tracer yet, so
`traceId` is a minted 32-hex value that only correlates a problem with the application
log, not a trace backend; a real one arrives in step 6 and only has to replace how
`traceId` is minted, not the shape around it.

**The catch-all.** One more `@ExceptionHandler(Exception.class)` on `ApiExceptionHandler`
renders `500 INTERNAL` with the fixed message "Something went wrong." and logs the
exception with its stack trace. This is exactly §13's own fallback ("anything unmapped
becomes 500 INTERNAL"), not a new behavior to design: today it also catches a few
pre-existing gaps (an unsupported `Content-Type`, for instance) that step 4 does not
otherwise close.

**Idempotency's OpenAPI carryover.** `IdempotencyFilter`'s `Idempotency-Key` header and
its three codes (ADR 0010) are documented with one `GlobalOpenApiCustomizer` bean in
`IdempotencyConfiguration`, applied to every mutating `/v1` operation, instead of
per-endpoint annotations on each API interface. The customizer mirrors the filter's own
`/v1/` + mutating-method rule, so the two stay in step by construction. The
`openapi/carrinho-v1.yaml` static export is still later work (it needs its own Gradle
task); this documents the live `/v3/api-docs` only.

**Hostile-input coverage.** `HostileInputTest` lists every operation once (method, URI
template, path-variable count, whether it takes a body) and drives two generic checks
from that list: a malformed JSON body, and a non-UUID path segment shaped like an
injection attempt. Both assert a clean `400 MALFORMED_REQUEST` with no stack trace, SQL
keyword or `app.meucarrinho` package name in the body. A hostile value sent as a
legitimate field (a list name) is asserted to round-trip verbatim, proving it is stored
and returned as plain text, not executed or dropped.

## Consequences

Rate limiting (429), request-size limits (413/415/422) and the real timeout/retry table
in §13 are still open; they need Bucket4j and Redis-backed buckets and are deferred to a
step the user picks later. The 403/404 question for strangers from step 5 (HANDOFF) is
unaffected. When step 6 adds a real tracer, only `RequestCorrelation`'s minting changes.
