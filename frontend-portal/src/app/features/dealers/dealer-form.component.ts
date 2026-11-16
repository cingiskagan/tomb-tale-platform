import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { firstValueFrom } from 'rxjs';
import { DealerResponse, DealerService } from '../../core/api';
import { problemDetail } from '../items/problem-detail';

/** Creates a dealer, or renames one. A 409 on an edit reloads what someone else saved. */
@Component({
  selector: 'app-dealer-form',
  standalone: true,
  imports: [ReactiveFormsModule, DialogModule, ButtonModule, InputTextModule, MessageModule],
  templateUrl: './dealer-form.component.html',
  styleUrl: './dealers.css',
})
export class DealerFormComponent {
  private readonly dealerService = inject(DealerService);

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<void>();

  private _visible = false;
  private _dealerPublicId: string | null = null;
  /** Moves when the dialog opens another dealer or closes, so a late answer for the old one is dropped. */
  private session = 0;
  private loadSeq = 0;
  private version = 0;

  @Input()
  set visible(value: boolean) {
    this._visible = value;
    if (value) this.openSession();
  }
  get visible() {
    return this._visible;
  }

  @Input()
  set dealerPublicId(value: string | null) {
    this._dealerPublicId = value;
    if (this._visible) this.openSession();
  }
  get dealerPublicId() {
    return this._dealerPublicId;
  }

  readonly form = new FormGroup({
    code: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/^[A-Z][A-Z0-9_]*$/)],
    }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
  });

  /** False until the form shows the dealer it edits, so a save never sends another one's name. */
  loaded = false;
  saving = false;
  notice: { severity: 'warn' | 'error'; text: string } | null = null;

  get isEditMode(): boolean {
    return this._dealerPublicId !== null;
  }

  /** @returns whether the form now shows the dealer */
  async load(): Promise<boolean> {
    const seq = ++this.loadSeq;
    this.loaded = false;
    this.notice = null;
    this.fill(null);
    if (!this._dealerPublicId) {
      this.loaded = true;
      return true;
    }
    try {
      const dealer = await firstValueFrom(this.dealerService.getDealer(this._dealerPublicId));
      if (seq !== this.loadSeq) return false;
      this.fill(dealer);
      this.loaded = true;
      return true;
    } catch (error) {
      if (seq === this.loadSeq) {
        this.notice = { severity: 'error', text: problemDetail(error, 'Failed to load the dealer') };
      }
      return false;
    }
  }

  async save() {
    if (this.form.invalid || !this.loaded) return;
    const session = this.session;
    this.saving = true;
    this.notice = null;
    const { code, name } = this.form.getRawValue();
    try {
      if (this._dealerPublicId) {
        await firstValueFrom(this.dealerService.updateDealer(this._dealerPublicId, { name, version: this.version }));
      } else {
        await firstValueFrom(this.dealerService.createDealer({ code, name }));
      }
      this.saved.emit();
      if (session === this.session) this.close();
    } catch (error) {
      if (session !== this.session) return;
      if (this.isEditMode && error instanceof HttpErrorResponse && error.status === 409) {
        if (await this.load()) {
          this.notice = {
            severity: 'warn',
            text: 'Someone else changed this dealer. It now shows their version, so check it and save again.',
          };
        }
      } else {
        this.notice = { severity: 'error', text: problemDetail(error, 'Failed to save the dealer') };
      }
    } finally {
      if (session === this.session) this.saving = false;
    }
  }

  close() {
    this.session++;
    this.loadSeq++;
    this._visible = false;
    this.visibleChange.emit(false);
  }

  private openSession() {
    this.session++;
    this.saving = false;
    void this.load();
  }

  private fill(dealer: DealerResponse | null) {
    this.version = dealer?.version ?? 0;
    this.form.reset({ code: dealer?.code ?? '', name: dealer?.name ?? '' });
    if (dealer) {
      this.form.controls.code.disable();
    } else {
      this.form.controls.code.enable();
    }
  }
}
