# 32. A stand is found by its dealer's code

- **Date:** 2026-11-12
- **Status:** accepted

## Context

[0013](0013-every-entity-carries-the-same-six-fields.md) makes `publicId` the only
identifier an API names. The Unity town places each NPC dealer at build time, but a
dealer's `publicId` is made by the database, so it differs in every environment.

## Decision

We will read and change a dealer's stand at `/api/v1/inventory/stands/{dealerCode}`,
under the dealer's stable `code`, such as `TOWN_BLACKSMITH`. The dealer itself, and each
entry on its stand, keep their `publicId` in the API.

## Consequences

- A dealer has two identifiers in the API, and the portal must use the right one.
- A code never changes, and a retired dealer keeps its code, so no new dealer takes
  over an NPC's stand.
- The client finds its stand with no lookup and no copy of the dealer list.

---

*Implemented by:* `feat(inventory): admins put templates on dealers' stands`
