import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService, FilterMetadata } from 'primeng/api';
import { PurchaseService, PurchaseResponse, PurchaseFilterRequest, PurchaseStatus } from '../../core/api';
import { PurchaseFormComponent } from './purchase-form.component';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-purchase-list',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    TagModule,
    ConfirmDialogModule,
    ToastModule,
    PurchaseFormComponent,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './purchase-list.component.html',
  styleUrl: './purchase-list.component.css'
})
export class PurchaseListComponent implements OnInit {
  private loadRequestSeq = 0;
  private readonly purchaseService = inject(PurchaseService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  purchases: PurchaseResponse[] = [];
  totalRecords = 0;
  loading = true;

  // Dialog state
  isFormDialogVisible = false;
  selectedPurchaseId: string | null = null;

  private lastLazyEvent: TableLazyLoadEvent | null = null;

  ngOnInit() {
    this.loading = true;
  }

  async loadPurchases(event: TableLazyLoadEvent) {
    const requestSeq = ++this.loadRequestSeq;
    this.lastLazyEvent = event;
    this.loading = true;

    try {
      const page = event.first ? Math.floor(event.first / (event.rows || 20)) : 0;
      const size = event.rows || 20;

      let sortParam = undefined;
      if (event.sortField) {
        const direction = event.sortOrder === 1 ? 'asc' : 'desc';
        sortParam = `${event.sortField as string},${direction}`;
      }

      const filters: PurchaseFilterRequest = {};

      if (event.filters) {
        filters.playerId = this.extractFilterValue(event.filters, 'playerId');
        filters.itemCode = this.extractFilterValue(event.filters, 'itemCode');
      }

      const response = await firstValueFrom(
        this.purchaseService.listPurchases(filters, page, size, sortParam)
      );
      if (requestSeq !== this.loadRequestSeq) return;

      this.purchases = response.content;
      this.totalRecords = response.totalElements;
    } catch (err) {
      if (requestSeq !== this.loadRequestSeq) return;
      console.error('Error loading purchases', err);
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to fetch purchases' });
    } finally {
      if (requestSeq === this.loadRequestSeq) {
        this.loading = false;
      }
    }
  }

  openNewPurchase() {
    this.selectedPurchaseId = null;
    this.isFormDialogVisible = true;
  }

  openEditPurchase(id: string) {
    this.selectedPurchaseId = id;
    this.isFormDialogVisible = true;
  }

  onFormSaved() {
    this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Purchase saved successfully' });
    if (this.lastLazyEvent) {
      void this.loadPurchases(this.lastLazyEvent);
    }
  }

  confirmDelete(id: string) {
    this.confirmationService.confirm({
      message: 'Are you sure you want to cancel this purchase?',
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      accept: async () => {
        try {
          await firstValueFrom(this.purchaseService.deletePurchase(id));
          this.messageService.add({ severity: 'success', summary: 'Successful', detail: 'Purchase Deleted', life: 3000 });
          if (this.lastLazyEvent) {
            void this.loadPurchases(this.lastLazyEvent);
          }
        } catch (error) {
          console.error('Failed to delete purchase', error);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete purchase' });
        }
      },
    });
  }

  getStatusSeverity(status: PurchaseStatus): 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast' {
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

  private extractFilterValue(filters: Record<string, FilterMetadata | FilterMetadata[] | undefined>, field: string): string | undefined {
    const filter = filters[field];
    if (Array.isArray(filter)) {
      return filter[0]?.value as string | undefined;
    }
    return (filter)?.value as string | undefined;
  }
}
