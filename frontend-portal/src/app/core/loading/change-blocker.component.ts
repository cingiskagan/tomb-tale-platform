import { Component, DestroyRef, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LoadingService } from './loading.service';

/** The keys that would press a button, submit a form or close a dialog. */
const BLOCKED_KEYS = new Set(['Enter', ' ', 'Escape']);

/**
 * Covers the portal, dialogs included, while a change and the reload after it are on their way (ADR 0037). Clicks
 * land on the cover, and Enter, Space and Escape stop before any control or dialog sees them.
 */
@Component({
  selector: 'app-change-blocker',
  standalone: true,
  template: `
    @if (blocking) {
      <div class="change-blocker" aria-hidden="true"></div>
    }
  `,
  styles: `
    /* Above Material's overlays at 1000, below the loading bar at 2000. It dims only if the wait is long enough to see. */
    .change-blocker {
      position: fixed;
      inset: 0;
      z-index: 1999;
      cursor: progress;
      animation: dim 150ms ease 300ms forwards;
    }

    @keyframes dim {
      to {
        background: rgba(0, 0, 0, 0.25);
      }
    }
  `,
})
export class ChangeBlockerComponent {
  blocking = false;

  constructor() {
    inject(LoadingService)
      .blocking$.pipe(takeUntilDestroyed())
      .subscribe((blocking) => (this.blocking = blocking));

    const window = inject(DOCUMENT).defaultView;
    const swallow = (event: KeyboardEvent) => {
      if (this.blocking && BLOCKED_KEYS.has(event.key)) {
        event.preventDefault();
        event.stopImmediatePropagation();
      }
    };
    // Capturing on the window runs before any element's handler, and before the CDK's Escape handler on the body.
    window?.addEventListener('keydown', swallow, true);
    window?.addEventListener('keyup', swallow, true);
    inject(DestroyRef).onDestroy(() => {
      window?.removeEventListener('keydown', swallow, true);
      window?.removeEventListener('keyup', swallow, true);
    });
  }
}
