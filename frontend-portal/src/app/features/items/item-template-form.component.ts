import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { firstValueFrom } from 'rxjs';
import { ItemCatalogService, ItemSetResponse, ItemTemplateResponse, ItemType } from '../../core/api';
import { StatsEditorComponent } from './stats-editor.component';
import { statArray, statRows, statsOf } from './stats-form';
import { problemDetail } from './problem-detail';

/** Creates a template, or edits one. A 409 on an edit reloads what someone else saved. */
@Component({
  selector: 'app-item-template-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    MessageModule,
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
  set templatePublicId(value: string | null) {
    this._templatePublicId = value;
    if (this._visible) {
      void this.load();
    }
  }
  get templatePublicId() {
    return this._templatePublicId;
  }

  readonly typeOptions = Object.values(ItemType).map((type) => ({ label: type, value: type }));

  readonly form = new FormGroup({
    code: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/^[A-Z][A-Z0-9_]*$/)],
    }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    itemType: new FormControl<ItemType | null>(null, Validators.required),
    price: new FormControl<number | null>(0, [Validators.required, Validators.min(0)]),
    assetKey: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    itemSetId: new FormControl<string | null>(null),
    iconHash: new FormControl<string | null>(null),
    baseStats: statArray(),
  });

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

  async load() {
    const seq = ++this.loadSeq;
    this.notice = null;
    if (!this._templatePublicId) {
      this.fill(null);
      return;
    }
    try {
      const template = await firstValueFrom(this.catalog.getTemplate(this._templatePublicId));
      if (seq === this.loadSeq) {
        this.fill(template);
      }
    } catch (error) {
      this.notice = { severity: 'error', text: problemDetail(error, 'Failed to load the template') };
    }
  }

  async uploadIcon(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.uploading = true;
    this.notice = null;
    try {
      const icon = await firstValueFrom(this.catalog.uploadIcon(file));
      this.iconBroken = false;
      this.form.controls.iconHash.setValue(icon.hash);
    } catch (error) {
      this.notice = { severity: 'error', text: problemDetail(error, 'Failed to upload the icon') };
    } finally {
      this.uploading = false;
      input.value = '';
    }
  }

  removeIcon() {
    this.form.controls.iconHash.setValue(null);
  }

  async save() {
    if (this.form.invalid) return;
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
      this.saved.emit();
      this.close();
    } catch (error) {
      if (this.isEditMode && error instanceof HttpErrorResponse && error.status === 409) {
        await this.load();
        this.notice = {
          severity: 'warn',
          text: 'Someone else changed this template. It now shows their version, so check it and save again.',
        };
      } else {
        this.notice = { severity: 'error', text: problemDetail(error, 'Failed to save the template') };
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
