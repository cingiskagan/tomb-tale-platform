import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { firstValueFrom } from 'rxjs';
import { ItemCatalogService, ItemSetResponse } from '../../core/api';
import { StatsEditorComponent } from './stats-editor.component';
import { statArray, statRows, statsOf } from './stats-form';
import { problemDetail } from './problem-detail';

/** Creates an item set, or edits one. A 409 on an edit reloads what someone else saved. */
@Component({
  selector: 'app-item-set-form',
  standalone: true,
  imports: [ReactiveFormsModule, DialogModule, ButtonModule, InputTextModule, MessageModule, StatsEditorComponent],
  templateUrl: './item-set-form.component.html',
  styleUrl: './items.css',
})
export class ItemSetFormComponent {
  private readonly catalog = inject(ItemCatalogService);

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<void>();

  private _visible = false;
  private _setPublicId: string | null = null;
  private loadSeq = 0;
  private version = 0;

  @Input()
  set visible(value: boolean) {
    this._visible = value;
    if (value) {
      void this.load();
    }
  }
  get visible() {
    return this._visible;
  }

  @Input()
  set setPublicId(value: string | null) {
    this._setPublicId = value;
    if (this._visible) {
      void this.load();
    }
  }
  get setPublicId() {
    return this._setPublicId;
  }

  readonly form = new FormGroup({
    code: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/^[A-Z][A-Z0-9_]*$/)],
    }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    bonus: statArray(),
  });

  saving = false;
  notice: { severity: 'warn' | 'error'; text: string } | null = null;

  get isEditMode(): boolean {
    return this._setPublicId !== null;
  }

  async load() {
    const seq = ++this.loadSeq;
    this.notice = null;
    if (!this._setPublicId) {
      this.fill(null);
      return;
    }
    try {
      const itemSet = await firstValueFrom(this.catalog.getSet(this._setPublicId));
      if (seq === this.loadSeq) {
        this.fill(itemSet);
      }
    } catch (error) {
      this.notice = { severity: 'error', text: problemDetail(error, 'Failed to load the set') };
    }
  }

  async save() {
    if (this.form.invalid) return;
    this.saving = true;
    this.notice = null;
    const { code, name } = this.form.getRawValue();
    const bonus = statsOf(this.form.controls.bonus);
    try {
      if (this._setPublicId) {
        await firstValueFrom(this.catalog.updateSet(this._setPublicId, { name, bonus, version: this.version }));
      } else {
        await firstValueFrom(this.catalog.createSet({ code, name, bonus }));
      }
      this.saved.emit();
      this.close();
    } catch (error) {
      if (this.isEditMode && error instanceof HttpErrorResponse && error.status === 409) {
        await this.load();
        this.notice = {
          severity: 'warn',
          text: 'Someone else changed this set. It now shows their version, so check it and save again.',
        };
      } else {
        this.notice = { severity: 'error', text: problemDetail(error, 'Failed to save the set') };
      }
    } finally {
      this.saving = false;
    }
  }

  close() {
    this.loadSeq++;
    this._visible = false;
    this.visibleChange.emit(false);
  }

  private fill(itemSet: ItemSetResponse | null) {
    this.version = itemSet?.version ?? 0;
    this.form.reset({ code: itemSet?.code ?? '', name: itemSet?.name ?? '' });
    // After the reset, which would blank the rows.
    this.form.controls.bonus.clear();
    statRows(itemSet?.bonus ?? {}).forEach((row) => this.form.controls.bonus.push(row));
    if (itemSet) {
      this.form.controls.code.disable();
    } else {
      this.form.controls.code.enable();
    }
  }
}
