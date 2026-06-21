import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { PurchaseService, PurchaseStatus } from '../../core/api';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-purchase-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
  ],
  templateUrl: './purchase-form.component.html',
  styleUrl: './purchase-form.component.css'
})
export class PurchaseFormComponent implements OnInit {
  private loadRequestSeq = 0;
  private readonly fb = inject(FormBuilder);
  private readonly purchaseService = inject(PurchaseService);

  private _visible = false;
  @Input()
  set visible(val: boolean) {
    this._visible = val;
    if (val && this.form) {
      void this.loadPurchaseData();
    }
  }
  get visible() {
    return this._visible;
  }
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<void>();

  private _purchaseId: string | null = null;
  @Input()
  set purchaseId(val: string | null) {
    this._purchaseId = val;
    this.isEditMode = !!val;
    if (this._visible && this.form) {
      void this.loadPurchaseData();
    }
  }
  get purchaseId(): string | null {
    return this._purchaseId;
  }

  isEditMode = false;
  isSaving = false;
  form!: FormGroup;

  readonly statusOptions = [
    { label: 'PENDING', value: PurchaseStatus.PENDING },
    { label: 'COMPLETED', value: PurchaseStatus.COMPLETED },
    { label: 'REFUNDED', value: PurchaseStatus.REFUNDED },
    { label: 'CANCELLED', value: PurchaseStatus.CANCELLED },
  ];

  ngOnInit() {
    this.initForm();
    if (this._visible) {
      void this.loadPurchaseData();
    }
  }

  private initForm() {
    this.form = this.fb.group({
      playerId: ['', Validators.required],
      itemCode: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: [0, [Validators.required, Validators.min(0)]],
      status: [PurchaseStatus.PENDING],
    });
  }

  async loadPurchaseData() {
    const requestSeq = ++this.loadRequestSeq;

    if (!this.purchaseId) {
      this.form.reset({ quantity: 1, unitPrice: 0, status: PurchaseStatus.PENDING });
      this.form.get('playerId')?.enable();
      this.form.get('itemCode')?.enable();
      return;
    }

    const purchaseId = this.purchaseId;
    try {
      const data = await firstValueFrom(this.purchaseService.getPurchaseById(purchaseId));
      if (requestSeq !== this.loadRequestSeq || this.purchaseId !== purchaseId || !this.visible) return;
      this.form.patchValue({
        playerId: data.playerId,
        itemCode: data.itemCode,
        quantity: data.quantity,
        unitPrice: data.unitPrice,
        status: data.status,
      });
      // playerId, itemCode, and unitPrice are immutable after creation on the backend.
      // The backend DTO UpdatePurchaseRequest only has quantity and status.
      // So we must disable them in the edit form or mark them readonly. The template handles this via [readonly]="isEditMode".
    } catch (err) {
      console.error('Failed to load purchase data', err);
      this.close();
    }
  }

  close() {
    this.loadRequestSeq++;
    this.visible = false;
    this.visibleChange.emit(this.visible);
    this.form.reset({ quantity: 1, unitPrice: 0, status: PurchaseStatus.PENDING });
  }

  async save() {
    if (this.form.invalid) return;

    this.isSaving = true;
    try {
      const val = this.form.getRawValue();

      if (this.isEditMode && this.purchaseId) {
        await firstValueFrom(this.purchaseService.updatePurchase(this.purchaseId, {
          quantity: val.quantity,
          status: val.status,
        }));
      } else {
        await firstValueFrom(this.purchaseService.createPurchase({
          playerId: val.playerId,
          itemCode: val.itemCode,
          quantity: val.quantity,
          unitPrice: val.unitPrice,
        }));
      }

      this.saved.emit();
      this.close();
    } catch (error) {
      console.error('Failed to save purchase', error);
      // In a real app we would show a toast here.
    } finally {
      this.isSaving = false;
    }
  }
}
