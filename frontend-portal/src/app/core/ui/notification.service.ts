import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** A short message at the bottom of the screen, for a change that worked or a call that failed. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string) {
    this.snackBar.open(message, 'Close', { duration: 4000, panelClass: 'tt-snack-success' });
  }

  /** Stays twice as long, since it usually carries the backend's reason. */
  error(message: string) {
    this.snackBar.open(message, 'Close', { duration: 8000, panelClass: 'tt-snack-error' });
  }
}
