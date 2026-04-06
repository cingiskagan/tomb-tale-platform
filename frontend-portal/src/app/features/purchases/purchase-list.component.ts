import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
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
  template: `
    <div class="purchase-container">
      <div class="header-actions">
        <h2 class="page-title">Purchases</h2>
        <p-button
          label="New Purchase"
          icon="pi pi-plus"
          (onClick)="openNewPurchase()"
        />
      </div>

      <p-table
        #dt
        [value]="purchases"
        [lazy]="true"
        (onLazyLoad)="loadPurchases($event)"
        [tableStyle]="{ 'min-width': '75rem' }"
        [paginator]="true"
        [rows]="20"
        [totalRecords]="totalRecords"
        [loading]="loading"
        styleClass="p-datatable-sm purchase-table"
        [showCurrentPageReport]="true"
        currentPageReportTemplate="Showing {first} to {last} of {totalRecords} entries"
        [rowsPerPageOptions]="[10, 20, 50]"
      >
        <ng-template pTemplate="header">
          <tr>
            <th>ID</th>
            <th pSortableColumn="playerId">Player ID <p-sortIcon field="playerId" /></th>
            <th pSortableColumn="itemCode">Item Code <p-sortIcon field="itemCode" /></th>
            <th>Quantity</th>
            <th>Unit Price</th>
            <th>Total Price</th>
            <th pSortableColumn="status">Status <p-sortIcon field="status" /></th>
            <th pSortableColumn="purchasedAt">Date <p-sortIcon field="purchasedAt" /></th>
            <th>Actions</th>
          </tr>
          <tr>
            <th></th>
            <th>
              <p-columnFilter type="text" field="playerId" placeholder="Search Player" [showMenu]="false" />
            </th>
            <th>
              <p-columnFilter type="text" field="itemCode" placeholder="Search Item" [showMenu]="false" />
            </th>
            <th></th>
            <th></th>
            <th></th>
            <th></th>
            <th></th>
            <th></th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-purchase>
          <tr>
            <td [title]="purchase.id">{{ purchase.id | slice:0:8 }}...</td>
            <td>{{ purchase.playerId }}</td>
            <td>{{ purchase.itemCode }}</td>
            <td>{{ purchase.quantity }}</td>
            <td>{{ purchase.unitPrice | currency }}</td>
            <td>{{ purchase.totalPrice | currency }}</td>
            <td>
              <p-tag
                [value]="purchase.status"
                [severity]="getStatusSeverity(purchase.status)"
              />
            </td>
            <td>{{ purchase.purchasedAt | date:'short' }}</td>
            <td>
              <p-button
                icon="pi pi-pencil"
                [rounded]="true"
                [text]="true"
                severity="success"
                (onClick)="openEditPurchase(purchase.id)"
              />
              <p-button
                icon="pi pi-trash"
                [rounded]="true"
                [text]="true"
                severity="danger"
                (onClick)="confirmDelete(purchase.id)"
              />
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="9" class="text-center p-4 text-gray-500">
              No purchases found. Use the filters or create a new purchase.
            </td>
          </tr>
        </ng-template>
      </p-table>
    </div>

    <!-- Create / Edit Dialog Component -->
    <app-purchase-form
      [(visible)]="isFormDialogVisible"
      [purchaseId]="selectedPurchaseId"
      (saved)="onFormSaved()"
    />

    <p-confirmDialog [style]="{width: '450px'}" />
    <p-toast />
  `,
  styles: [`
    .purchase-container {
      background: rgba(15, 15, 25, 0.9);
      border: 1px solid rgba(212, 162, 78, 0.2);
      border-radius: 8px;
      padding: 1.5rem;
    }

    .header-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }

    .page-title {
      font-family: 'Cinzel', serif;
      color: #d4a24e;
      margin: 0;
      font-size: 1.5rem;
    }

    ::ng-deep .purchase-table .p-datatable-header,
    ::ng-deep .purchase-table .p-datatable-tbody > tr > td {
      border-color: rgba(212, 162, 78, 0.1);
    }
  `],
})
export class PurchaseListComponent implements OnInit {
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
        const playerIdFilter = event.filters['playerId'];
        if (Array.isArray(playerIdFilter)) {
          filters.playerId = playerIdFilter[0]?.value;
        } else if (playerIdFilter) {
          filters.playerId = playerIdFilter.value;
        }

        const itemCodeFilter = event.filters['itemCode'];
        if (Array.isArray(itemCodeFilter)) {
          filters.itemCode = itemCodeFilter[0]?.value;
        } else if (itemCodeFilter) {
          filters.itemCode = itemCodeFilter.value;
        }
      }

      const response = await firstValueFrom(
        this.purchaseService.listPurchases(filters, page, size, sortParam)
      );

      this.purchases = response.content;
      this.totalRecords = response.totalElements;
    } catch (err) {
      console.error('Error loading purchases', err);
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to fetch purchases' });
    } finally {
      this.loading = false;
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
}
