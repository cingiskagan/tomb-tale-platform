import { Component, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { PurchaseService, PurchaseResponse, PurchaseFilterRequest, PurchaseStatus } from '../../core/api';
import { ConfirmService, NotificationService, PageQuery, initQuery, pageOf, sortBy, turnPage } from '../../core/ui';
import { PurchaseFormComponent } from './purchase-form.component';
import { debounceTime, firstValueFrom } from 'rxjs';

/** How long the list waits after the last keystroke in a filter before it asks again. */
const FILTER_DELAY_MS = 300;

@Component({
  selector: 'app-purchase-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatTableModule,
    PurchaseFormComponent,
  ],
  templateUrl: './purchase-list.component.html',
  styleUrl: './purchase-list.component.css'
})
export class PurchaseListComponent implements OnInit {
  private loadRequestSeq = 0;
  private readonly purchaseService = inject(PurchaseService);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  readonly columns = ['publicId', 'playerId', 'itemCode', 'quantity', 'unitPrice', 'totalPrice', 'status', 'createdAt', 'actions'];

  /** A filter narrows the list from its first page, once the admin stops typing. */
  readonly filters = new FormGroup({
    playerId: new FormControl('', { nonNullable: true }),
    itemCode: new FormControl('', { nonNullable: true }),
  });

  purchases: PurchaseResponse[] = [];
  totalRecords = 0;
  loading = true;
  query: PageQuery = initQuery(20);

  // Dialog state
  isFormDialogVisible = false;
  selectedPurchasePublicId: string | null = null;

  constructor() {
    this.filters.valueChanges
      .pipe(debounceTime(FILTER_DELAY_MS), takeUntilDestroyed())
      .subscribe(() => void this.loadPurchases({ ...this.query, page: 0 }));
  }

  ngOnInit() {
    void this.loadPurchases(this.query);
  }

  sortPurchases(sort: Sort) {
    void this.loadPurchases(sortBy(this.query, sort));
  }

  pagePurchases(event: PageEvent) {
    void this.loadPurchases(turnPage(this.query, event));
  }

  async loadPurchases(query: PageQuery) {
    const requestSeq = ++this.loadRequestSeq;
    this.query = query;
    this.loading = true;

    try {
      const { page, size, sort } = pageOf(query);
      const { playerId, itemCode } = this.filters.getRawValue();
      const filters: PurchaseFilterRequest = { playerId: playerId.trim(), itemCode: itemCode.trim() };

      const response = await firstValueFrom(
        this.purchaseService.listPurchases(filters, page, size, sort)
      );
      if (requestSeq !== this.loadRequestSeq) return;

      this.purchases = response.content;
      this.totalRecords = response.page.totalElements;
    } catch (err) {
      if (requestSeq !== this.loadRequestSeq) return;
      console.error('Error loading purchases', err);
      this.notifications.error('Failed to fetch purchases');
    } finally {
      if (requestSeq === this.loadRequestSeq) {
        this.loading = false;
      }
    }
  }

  openNewPurchase() {
    this.selectedPurchasePublicId = null;
    this.isFormDialogVisible = true;
  }

  openEditPurchase(publicId: string) {
    this.selectedPurchasePublicId = publicId;
    this.isFormDialogVisible = true;
  }

  onFormSaved() {
    this.notifications.success('Purchase saved successfully');
    void this.loadPurchases(this.query);
  }

  async confirmDelete(publicId: string) {
    const accepted = await this.confirm.ask({
      title: 'Confirm Deletion',
      message: 'Are you sure you want to cancel this purchase?',
      accept: 'Yes',
      reject: 'No',
    });
    if (!accepted) return;
    try {
      await firstValueFrom(this.purchaseService.deletePurchase(publicId));
      this.notifications.success('Purchase Deleted');
      void this.loadPurchases(this.query);
    } catch (error) {
      console.error('Failed to delete purchase', error);
      this.notifications.error('Failed to delete purchase');
    }
  }

  /** A pending purchase waits for inventory's item grant, and the backend refuses to edit or delete it. */
  isPending(status: PurchaseStatus): boolean {
    return status === PurchaseStatus.PENDING;
  }

  getStatusSeverity(status: PurchaseStatus): 'success' | 'secondary' | 'info' | 'warn' | 'danger' {
    switch (status) {
      case PurchaseStatus.COMPLETED:
        return 'success';
      case PurchaseStatus.PENDING:
        return 'warn';
      case PurchaseStatus.REFUNDED:
        return 'info';
      case PurchaseStatus.CANCELLED:
        return 'danger';
      default:
        return 'secondary';
    }
  }
}
