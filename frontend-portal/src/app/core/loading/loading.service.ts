import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, debounceTime, distinctUntilChanged, map } from 'rxjs';

/**
 * Counts the API calls in flight, so the layout shows one loading bar for all of them, and covers the portal from a
 * change until no call is left, so the reload after it is covered too (ADR 0037).
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly calls = new BehaviorSubject(0);
  private readonly changing = new BehaviorSubject(false);

  /** Whether a call is in flight. It settles a tick later, so a call a render starts can't change that render. */
  readonly loading$: Observable<boolean> = this.calls.pipe(
    map((calls) => calls > 0),
    debounceTime(0),
    distinctUntilChanged(),
  );

  /** True from the moment a change starts until a tick passes with no call in flight. */
  readonly blocking$: Observable<boolean> = this.changing.pipe(distinctUntilChanged());

  /** @param change whether the call changes something, which a GET never does */
  start(change: boolean) {
    if (change) this.changing.next(true);
    this.calls.next(this.calls.value + 1);
  }

  stop() {
    this.calls.next(this.calls.value - 1);
    // The reload after a change starts as soon as the change answers, so one tick tells it apart from the end.
    if (this.calls.value === 0) {
      setTimeout(() => {
        if (this.calls.value === 0) this.changing.next(false);
      });
    }
  }
}
