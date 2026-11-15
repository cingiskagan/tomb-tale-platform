# 34. Every API call shows loading, and a change blocks the next

- **Date:** 2026-11-16
- **Status:** accepted

## Context

Without a sign, an admin can't tell a slow answer from none and clicks again, and a second
change on top of one in flight sends a duplicate request, such as a DELETE for an entry
already gone. [0033](0033-the-portal-drops-late-answers-and-a-change-reloads-what-it-touched.md) keeps late answers off the screen, not the second click.

## Decision

We will show and block in three layers:

- An HTTP interceptor counts the calls in flight to the API, and the main layout shows a
  thin bar on top of everything while any is. No component has to remember it.
- A table, form or button that waits on its own call also shows PrimeNG's `loading`.
- A read blocks nothing. A change sets its component's `busy` from the request until its
  reload ends. While set, the controls that start a change are disabled and refused, and
  the area dims. The rest of the portal stays usable.

## Consequences

- The bar flashes on fast calls. Keycloak's calls don't count, so a token refresh shows none.
- Each new control that starts a change must check `busy`, and nothing enforces it.

---

*Implemented by:* `feat(portal): every API call shows loading, and a change blocks the next`
