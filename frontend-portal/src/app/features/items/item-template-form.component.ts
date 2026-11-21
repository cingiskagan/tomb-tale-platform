import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { ItemCatalogService, ItemSetResponse, ItemTemplateResponse, ItemType } from '../../core/api';
import { DialogDirective } from '../../core/ui';
import { StatsEditorComponent } from './stats-editor.component';
import { statArray, statRows, statsOf } from './stats-form';
import { problemDetail } from './problem-detail';

/** Inventory's cap, so a red copy's price still fits its numbers. */
const MAX_PRICE = 1_000_000_000;

/** Creates a template, or edits one. A 409 on an edit reloads what someone else saved. */
@Component({
  selector: 'app-item-template-form',
  standalone: true,
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    DialogDirective,
    StatsEditorComponent,
  ],
  templateUrl: './item-template-form.component.html',
  styleUrl: './items.css',
})
export class ItemTemplateFormComponent {
  private readonly catalog = inject(ItemCatalogService);

  @Input() sets: ItemSetResponse[] = [];
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<void>();

  private _visible = false;
  private _templatePublicId: string | null = null;
  /** Moves when the dialog opens another template or closes, so a late answer for the old one is dropped. */
  private session = 0;
  private loadSeq = 0;
  private version = 0;

  @Input()
  set visible(value: boolean) {
    this._visible = value;
    if (value) {
      this.openSession();
    }
  }
  get visible() {
    return this._visible;
  }

  @Input()
  set templatePublicId(value: string | null) {
    this._templatePublicId = value;
    if (this._visible) {
      this.openSession();
    }
  }
  get templatePublicId() {
    return this._templatePublicId;
  }

  readonly maxPrice = MAX_PRICE;
  readonly typeOptions = Object.values(ItemType).map((type) => ({ label: type, value: type }));

  readonly form = new FormGroup({
    code: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/^[A-Z][A-Z0-9_]*$/)],
    }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    itemType: new FormControl<ItemType | null>(null, Validators.required),
    price: new FormControl<number | null>(0, [Validators.required, Validators.min(0), Validators.max(MAX_PRICE)]),
    assetKey: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    itemSetId: new FormControl<string | null>(null),
    iconHash: new FormControl<string | null>(null),
    baseStats: statArray(),
  });

  /** False until the form shows the template it edits, so a save never sends another one's fields. */
  loaded = false;
  saving = false;
  uploading = false;
  iconBroken = false;
  /** Shown above the form: why the last save or upload failed, or that the template was reloaded. */
  notice: { severity: 'warn' | 'error'; text: string } | null = null;

  get isEditMode(): boolean {
    return this._templatePublicId !== null;
  }

  get iconUrl(): string | null {
    const hash = this.form.controls.iconHash.value;
    return hash ? this.catalog.iconUrl(hash) : null;
  }

  /** @returns whether the form now shows the template */
  async load(): Promise<boolean> {
    const seq = ++this.loadSeq;
    this.loaded = false;
    this.notice = null;
    // Blank first, so a slow or failed load never leaves the previous template's fields in the form.
    this.fill(null);
    if (!this._templatePublicId) {
      this.loaded = true;
      return true;
    }
    try {
      const template = await firstValueFrom(this.catalog.getTemplate(this._templatePublicId));
      if (seq !== this.loadSeq) return false;
      this.fill(template);
      this.loaded = true;
      return true;
    } catch (error) {
      if (seq === this.loadSeq) {
        this.notice = { severity: 'error', text: problemDetail(error, 'Failed to load the template') };
      }
      return false;
    }
  }

  async uploadIcon(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // A save on its way already took the icon, so a new one would be lost.
    if (!file || this.saving) {
      input.value = '';
      return;
    }
    const session = this.session;
    this.uploading = true;
    this.notice = null;
    try {
      const icon = await firstValueFrom(this.catalog.uploadIcon(file));
      if (session !== this.session) return;
      this.iconBroken = false;
      this.form.controls.iconHash.setValue(icon.hash);
    } catch (error) {
      if (session === this.session) {
        this.notice = { severity: 'error', text: problemDetail(error, 'Failed to upload the icon') };
      }
    } finally {
      if (session === this.session) {
        this.uploading = false;
      }
      input.value = '';
    }
  }

  removeIcon() {
    if (this.saving) return;
    this.form.controls.iconHash.setValue(null);
  }

  async save() {
    if (this.form.invalid || !this.loaded || this.saving) return;
    const session = this.session;
    this.saving = true;
    this.notice = null;
    const value = this.form.getRawValue();
    const fields = {
      name: value.name,
      price: value.price ?? 0,
      baseStats: statsOf(this.form.controls.baseStats),
      assetKey: value.assetKey,
      iconHash: value.iconHash,
      itemSetId: value.itemSetId,
    };
    try {
      if (this._templatePublicId) {
        await firstValueFrom(
          this.catalog.updateTemplate(this._templatePublicId, { ...fields, version: this.version }),
        );
      } else {
        await firstValueFrom(
          this.catalog.createTemplate({ ...fields, code: value.code, itemType: value.itemType as ItemType }),
        );
      }
      // The page still refreshes its table, but a save from an earlier session leaves the open editor alone.
      this.saved.emit();
      if (session === this.session) {
        this.close();
      }
    } catch (error) {
      if (session !== this.session) return;
      if (this.isEditMode && error instanceof HttpErrorResponse && error.status === 409) {
        // A failed reload keeps its own error, so the stale version is never presented as theirs.
        if (await this.load()) {
          this.notice = {
            severity: 'warn',
            text: 'Someone else changed this template. It now shows their version, so check it and save again.',
          };
        }
      } else {
        this.notice = { severity: 'error', text: problemDetail(error, 'Failed to save the template') };
      }
    } finally {
      if (session === this.session) {
        this.saving = false;
      }
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
    this.uploading = false;
    void this.load();
  }

  private fill(template: ItemTemplateResponse | null) {
    this.version = template?.version ?? 0;
    this.iconBroken = false;
    this.form.reset({
      code: template?.code ?? '',
      name: template?.name ?? '',
      itemType: template?.itemType ?? null,
      price: template?.price ?? 0,
      assetKey: template?.assetKey ?? '',
      itemSetId: template?.itemSetId ?? null,
      iconHash: template?.iconHash ?? null,
    });
    // After the reset, which would blank the rows.
    this.form.controls.baseStats.clear();
    statRows(template?.baseStats ?? {}).forEach((row) => this.form.controls.baseStats.push(row));
    // The code and the type are fixed once the template exists.
    if (template) {
      this.form.controls.code.disable();
      this.form.controls.itemType.disable();
    } else {
      this.form.controls.code.enable();
      this.form.controls.itemType.enable();
    }
  }
}
