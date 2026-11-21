import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/** What the confirm dialog asks, and what its two buttons say. */
export interface Confirmation {
  title: string;
  message: string;
  accept: string;
  reject: string;
}

/** The question ConfirmService asks. It closes with true only through the accept button. */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p class="confirm-message">
        <mat-icon fontIcon="pi-exclamation-triangle" aria-hidden="true" />
        <span>{{ data.message }}</span>
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">{{ data.reject }}</button>
      <button mat-flat-button [mat-dialog-close]="true">{{ data.accept }}</button>
    </mat-dialog-actions>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: `
    .confirm-message {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
    }

    mat-icon {
      flex-shrink: 0;
      color: var(--tt-color-gold);
    }
  `,
})
export class ConfirmDialogComponent {
  readonly data = inject<Confirmation>(MAT_DIALOG_DATA);
}
