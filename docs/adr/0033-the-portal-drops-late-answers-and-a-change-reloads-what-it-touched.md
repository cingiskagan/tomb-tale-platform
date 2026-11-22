# 33. The portal drops late answers, and a change reloads what it touched

- **Date:** 2026-11-16
- **Status:** amended by [0037](0037-a-change-covers-the-portal-until-no-call-is-left.md), whose cover replaced `busy` outside the stand editor and inventory

## Context

A screen often has several requests in flight: the read when it opens, a change, the
reload after it. A late answer can overwrite a newer one, and CodeRabbit finds a case in
most PRs. The services' locks and versions keep the data right, so the screen only goes stale.

## Decision

We will follow four rules in every component, with no state management library:

- A `session` counter moves when a dialog changes subject or closes, dropping older answers.
- A load counter, such as `seq`, lets only the newest load fill the view.
- A change reloads what it touched instead of patching it, so the read after it counts.
- A change keeps `busy` set until its reload ends, and a new subject clears the old view.

## Consequences

- A store such as NgRx would not remove these races, because its effects need `switchMap` too.
- The counters are written by hand, and a path that forgets one is a race. RxJS `switchMap`
  would make the newest-load rule automatic, and moving to it is in the backlog.
- Every change costs one more read.

---

*Implemented by:* `fix(portal): a late read never undoes a stand or a trade`
