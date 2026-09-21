# 15. One error envelope, and it is RFC 9457

- **Date:** 2026-09-14
- **Status:** accepted

## Context

A client needs two error parsers: commerce answers with a hand-written `ErrorResponse`, and
player has no handler at all, so a 404 arrives empty and a rejected `?sort=` field is a 500.

## Decision

We will answer every error from every service with `application/problem+json`, and delete
`ErrorResponse`. `platform-commons` holds an abstract `PlatformExceptionHandler extends
ResponseEntityExceptionHandler`, and each service declares a `@RestControllerAdvice`
extending it that adds only the exceptions it alone throws.

## Consequences

- Player needs no handler of its own. `ResponseStatusException` already carries a problem
  detail, so its advice is an empty subclass that registers the inherited handlers.
- Commerce's keys change: `message` to `detail`, `error` to `title`, `timestamp` gone.
  The portal branches on status alone, so the break is paid now, while it is free.
- A rejected sort field becomes 400 through `InvalidSortFieldException`, and validation
  failures gain an `errors` member that points at the field.
- Inheritance couples the two services: a base-class handler changes both at once.

---

*Implemented by:* `refactor: extract shared security and error contract`
