# 15. One error envelope, and it is RFC 9457

- **Date:** 2026-09-14
- **Status:** accepted

## Context

A client of this platform needs two error parsers. service-commerce answers
with a hand-written `ErrorResponse` record — `status`, `error`, `message`,
`timestamp` — built by its `GlobalExceptionHandler`. service-player has no
handler at all: its service layer throws `ResponseStatusException`, Spring's
default error handling takes over, and the reason never reaches the client. A
404 from `PATCH /players/me` arrives with an empty body.

A rejected `?sort=` field reaches neither. Both repositories validate sort
properties against an allow-list and threw `IllegalArgumentException`, which
nothing handled — a typo in a query string came back as a 500.

Spring Framework 6 added `ProblemDetail`, the RFC 9457 (formerly 7807) shape,
and made `ResponseStatusException` carry one. `ResponseEntityExceptionHandler`
already maps every framework exception to it.

## Decision

We will answer every error from every service with RFC 9457
`application/problem+json`, and delete the `ErrorResponse` record.

`platform-commons` holds an abstract `PlatformExceptionHandler extends
ResponseEntityExceptionHandler` carrying the cross-cutting handlers. Each
service declares a `@RestControllerAdvice` extending it and adds only the
exceptions it alone can throw.

## Consequences

- Player's problem disappears without a handler being written for it.
  `ResponseStatusException` is an `ErrorResponseException`, so the base class
  already turns it into a populated problem detail. Its advice is an empty
  subclass whose only job is to register the inherited handlers.
- Commerce's response keys change: `message` becomes `detail`, `error` becomes
  `title`, `timestamp` is gone. Nothing consumes them — the portal branches on
  the status code only — so the break is paid now, while it is free.
- Validation failures gain an `errors` member mapping field to message, instead
  of the old handler's one joined string. RFC 9457 allows extension members; a
  form can point at the field that failed rather than parse prose.
- Registering any `ResponseEntityExceptionHandler` bean makes Boot back off its
  own `ProblemDetailsExceptionHandler`. `spring.mvc.problemdetails.enabled` is
  therefore not set anywhere, and setting it would do nothing.
- `type` stays `about:blank` on every problem, so Jackson omits it. Typed error
  URIs are the obvious next step and deliberately not taken here: a `type` URI
  is a permanent identifier, and inventing them before a client needs to branch
  on one would freeze names we have not thought about.
- A rejected sort field becomes 400 through a new `InvalidSortFieldException`
  in commons. Mapping `IllegalArgumentException` itself would have been less
  code and would have turned every unrelated argument bug into a 400.
- Access denial is deliberately not handled in the advice. Spring Security's
  filter chain translates it, and that is what distinguishes a missing token
  (401) from an insufficient role (403); an advice catching
  `AccessDeniedException` would flatten both to 403.
- `platform-commons` now depends on Spring MVC and Spring Security. Both are
  `optional`, so a future consumer that only wants `BaseEntity` does not
  inherit a servlet container.
- Inheritance is the coupling we accept. Adding a cross-cutting handler to the
  base class changes both services at once, which is the point, and also means
  neither service can opt out of one without overriding it.

---

*Implemented by:* `refactor: extract shared security and error contract`
