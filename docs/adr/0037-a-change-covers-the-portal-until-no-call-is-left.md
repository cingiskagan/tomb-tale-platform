# 37. A change covers the portal until no call is left

- **Date:** 2026-11-21
- **Status:** accepted

## Context

[0034](0034-every-api-call-shows-loading-and-a-change-blocks-the-next.md) left blocking to each component, as a `busy` flag nothing enforced. Reviews kept finding
gaps: a New button left enabled, a sort that ended a reload early and cleared `busy`, a Save pressed twice.

## Decision

We will show and block in three layers, replacing 0034:

- The loading interceptor counts the API calls in flight, and the layout shows a thin bar.
- A table waiting on its own call shows a progress bar, and a button a spinner for its icon.
- Every call but a GET is a change. Until a tick passes with no call in flight, a cover over
  the portal and its dialogs takes clicks and swallows Enter, Space and Escape, so the
  reload a change starts as it answers is covered too. Reads cover nothing.
- Each `save()` still refuses while saving. The stand editor and inventory keep their `busy`.

## Consequences

- A new screen is blocked during its changes without code of its own.
- A slow change freezes the whole portal, and one that never answers freezes it until a page
  reload, since HttpClient has no timeout. A read beside a change keeps the cover up too.

---

*Implemented by:* `feat(portal): a change covers the portal until no call is left`
