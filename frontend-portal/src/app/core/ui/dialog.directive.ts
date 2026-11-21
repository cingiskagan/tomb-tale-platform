import { Directive, EventEmitter, Input, OnChanges, OnDestroy, Output, TemplateRef, ViewContainerRef, inject } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';

/**
 * Shows its template as a Material dialog while `appDialog` is true, so the host component and its state outlive
 * each opening. Escape, the backdrop or a navigation emits `appDialogDismissed`, and the host closes itself.
 */
@Directive({
  selector: 'ng-template[appDialog]',
  standalone: true,
})
export class DialogDirective implements OnChanges, OnDestroy {
  private readonly dialog = inject(MatDialog);
  private readonly template = inject(TemplateRef);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private ref: MatDialogRef<unknown> | null = null;

  @Input({ required: true }) appDialog = false;
  /** The panel's width, as CSS. Read when the dialog opens. */
  @Input() appDialogWidth = '30rem';
  @Output() readonly appDialogDismissed = new EventEmitter<void>();

  ngOnChanges() {
    if (this.appDialog && !this.ref) {
      this.open();
    } else if (!this.appDialog && this.ref) {
      this.close();
    }
  }

  ngOnDestroy() {
    this.close();
  }

  private open() {
    const ref = this.dialog.open(this.template, {
      width: this.appDialogWidth,
      maxWidth: '95vw',
      viewContainerRef: this.viewContainerRef,
    });
    this.ref = ref;
    ref.afterClosed().subscribe(() => {
      // Only a close the host did not ask for is news to it.
      if (this.ref !== ref) return;
      this.ref = null;
      this.appDialogDismissed.emit();
    });
  }

  private close() {
    const ref = this.ref;
    this.ref = null;
    ref?.close();
  }
}
