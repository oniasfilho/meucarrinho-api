# 0008. How receipt history pages

Date: 2026-10-03 · Status: accepted

## Context

Spec §7 lists `GET /v1/receipts?cursor=` ("History, newest first, grouped by month") and
`GET /v1/receipts/summary?from=&to=` ("Monthly totals"). It does not say what a cursor
holds, how big a page is, or when paging ends. `GetReceiptHistory` already returns the
receipts between two months, grouped by month. We agreed not to change it.

## Decision

- **The summary** maps `from` and `to` (`yyyy-MM`) straight onto `GetReceiptHistory`,
  newest month first. A month that doesn't parse, or `from` after `to`, returns
  `422 VALIDATION_FAILED`.
- **History pages by month windows, not by receipt count.** Each page covers 6 calendar
  months in America/Sao_Paulo, newest first, and drops empty months. The first page ends
  at the current month.
- **The cursor is opaque.** It is base64url of `m1:<yyyy-MM>`, the newest month of the
  next page. The `m1:` prefix lets the format change later without breaking old cursors
  silently. A cursor that doesn't decode returns `400 MALFORMED_REQUEST`.
- **Paging ends at January 2026.** `nextCursor` is null once a page reaches that month.
  No receipt can be older than the product, including guest-imported ones, because the
  app creates them. Without a floor, a client would page through empty years forever.
  Nothing cheaper exists to find the oldest receipt without changing a port.
- **"Now" for the first page** comes from the core's `Clock`, exposed to the REST layer as
  a `java.time.InstantSource` bean, because `api` may not depend on output ports.

## Consequences

A page can be empty yet still carry a `nextCursor` (a user who did not shop for six
months). Clients keep following the cursor until it is null. A month with hundreds of
receipts arrives in one page; if that ever matters, the cursor format can grow (`m2:`)
without changing the endpoint.
