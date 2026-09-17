# 16. Lists answer with a paged envelope of our own

- **Date:** 2026-09-17
- **Status:** accepted

## Context

Both list endpoints returned Spring Data's `Page` straight out of the
controller, so what a client received was `PageImpl` as Jackson happened to
serialise it: `content`, plus a nested `pageable` object, plus `sort` twice,
plus `first`, `last`, `empty` and `numberOfElements`. Twenty fields in all, and
the portal mirrored every one of them by hand in `common.model.ts`.

None of that shape is a contract. It is the field layout of a framework class,
and it changes when the framework changes — which is why Spring Data ships
`PagedModel` as the thing you are meant to send instead. The project is on
Spring Boot 4, young enough that the risk is not theoretical.

## Decision

We will answer every paginated endpoint with `PagedResponse<T>` from
`platform-commons`: a `content` array and a `page` object carrying `number`,
`size`, `totalElements` and `totalPages`.

`Page` stays inside the repository and service layers. The controller calls
`PagedResponse.from(...)` at the boundary, because the envelope is a wire
format and nothing below the controller should know about it.

## Consequences

- The portal's mirror drops from twenty fields to six.
- The shape is deliberately the same as Spring Data's `PagedModel`. If we later
  decide the framework type is good enough after all, no JSON changes.
- Four fields are gone from the wire: `first`, `last`, `empty` and
  `numberOfElements`. Each is derivable from the four counts, and the portal
  read none of them. The `sort` echo is gone too — a client knows what it
  asked for.
- `PagedResponseTest` asserts the exact JSON keys. That test is the contract; a
  rename now fails a build instead of a page.
- Nothing enforces the wrapping. A new list endpoint that returns `Page`
  directly compiles and works, and publishes the old accidental shape again.
  The two existing endpoints are the pattern to copy.
- `platform-commons` gains no dependency. It already had spring-data-commons
  for `BaseEntity`.

---

*Implemented by:* `refactor: stable pagination and update semantics`
