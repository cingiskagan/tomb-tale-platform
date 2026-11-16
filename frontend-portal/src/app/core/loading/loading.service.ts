import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, debounceTime, distinctUntilChanged, map } from 'rxjs';

/** Counts the API calls in flight, so the layout shows one loading bar for all of them (ADR 0034). */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly calls = new BehaviorSubject(0);

  /** Whether a call is in flight. It settles a tick later, so a call a render starts can't change that render. */
  readonly loading$: Observable<boolean> = this.calls.pipe(
    map((calls) => calls > 0),
    debounceTime(0),
    distinctUntilChanged(),
  );

  start() {
    this.calls.next(this.calls.value + 1);
  }

  stop() {
    this.calls.next(this.calls.value - 1);
  }
}
