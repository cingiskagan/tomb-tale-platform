import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { ConfirmDialogComponent, Confirmation } from './confirm-dialog.component';

/** Asks the admin before a change that is hard to undo. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(MatDialog);

  /** @returns whether the admin accepted; Escape and the backdrop count as no */
  async ask(confirmation: Confirmation): Promise<boolean> {
    const ref = this.dialog.open<ConfirmDialogComponent, Confirmation, boolean>(ConfirmDialogComponent, {
      data: confirmation,
      width: '28rem',
      maxWidth: '95vw',
    });
    return (await firstValueFrom(ref.afterClosed())) === true;
  }
}
