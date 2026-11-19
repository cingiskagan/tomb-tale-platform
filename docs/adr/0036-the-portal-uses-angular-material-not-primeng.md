# 36. The portal uses Angular Material, not PrimeNG

- **Date:** 2026-11-19
- **Status:** accepted

## Context

PrimeNG 22, the only one for Angular 22, left MIT: its license is free under limits on revenue,
staff and funding, renewed yearly, and a page without a key shows a red banner. Angular 20's
support ends in late November 2026, and PrimeNG 22 silently breaks our tables and dialogs.

## Decision

We will build the portal on Angular Material and the CDK, MIT and released with Angular:

- A dialog stays a component in its page and shows its template as a MatDialog through
  `appDialog`, so its [0033](0033-the-portal-drops-late-answers-and-a-change-reloads-what-it-touched.md) session counters outlive each opening.
- Toasts go through a snackbar notifier, questions through a confirm dialog.
- [0034](0034-every-api-call-shows-loading-and-a-change-blocks-the-next.md)'s second layer is a progress bar over a table and a spinner in a button.
- PrimeIcons 7 stays as mat-icon's font. It is MIT and needs no PrimeNG.

## Consequences

- The portal follows Angular to 22 with `ng update`, which migrates Material too.
- The theme keeps the old colours, not the old shapes, and a number field loses its buttons.
- Column filters, a button's loading state and status tags are code we keep.

---

*Implemented by:* `feat(portal): Angular Material arrives, and the shell moves to it` and the three after it
