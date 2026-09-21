# 16. Lists answer with a paged envelope of our own

- **Date:** 2026-09-17
- **Status:** accepted

## Context

Both list endpoints returned Spring Data's `Page` straight out of the controller, so a
client received `PageImpl` as Jackson happened to serialize it: twenty fields, mirrored by
hand in the portal. That shape is a framework's field layout, not a contract.

## Decision

We will answer every paginated endpoint with `PagedResponse<T>` from `platform-commons`: a
`content` array and a `page` object with `number`, `size`, `totalElements` and
`totalPages`. `Page` stays below the controller, which calls `PagedResponse.from(...)`.

## Consequences

- The portal's mirror drops from twenty fields to six. `first`, `last`, `empty`,
  `numberOfElements` and the `sort` echo leave the wire, each derivable or unread.
- The shape matches Spring Data's `PagedModel`, so adopting that type later changes no JSON.
- `PagedResponseTest` asserts the exact keys, so a rename fails a build, not a page.
- Nothing enforces the wrapping. A new endpoint that returns `Page` publishes the old
  accidental shape again, so the two existing endpoints are the pattern to copy.
- `platform-commons` gains no dependency: spring-data-commons was already there.

---

*Implemented by:* `refactor: stable pagination and update semantics`
