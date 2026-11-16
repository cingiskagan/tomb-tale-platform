import { Component, OnInit, inject } from '@angular/core';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';
import { DealerResponse, DealerService } from '../../core/api';
import { pageOf } from '../items/page-of';
import { problemDetail } from '../items/problem-detail';
import { DealerFormComponent } from './dealer-form.component';
import { StandEditorComponent } from './stand-editor.component';

/** The dealers for `platform_admin`, each with a Store button that opens its stand. */
@Component({
  selector: 'app-dealer-list',
  standalone: true,
  imports: [
    TableModule,
    ButtonModule,
    TagModule,
    ConfirmDialogModule,
    ToastModule,
    DealerFormComponent,
    StandEditorComponent,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './dealer-list.component.html',
  styleUrl: './dealers.css',
})
export class DealerListComponent implements OnInit {
  private readonly dealerService = inject(DealerService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  dealers: DealerResponse[] = [];
  total = 0;
  loading = true;
  private lastEvent: TableLazyLoadEvent | null = null;
  /** Only the newest request fills the table, so a slow answer for an old page never replaces the current one. */
  private seq = 0;

  /** Whether inventory takes custom stand entries, so the stand editor offers them only then. */
  customStandItemsEnabled = false;

  isFormVisible = false;
  selectedDealerId: string | null = null;
  isStandVisible = false;
  standDealer: DealerResponse | null = null;

  ngOnInit() {
    void this.loadSettings();
  }

  async loadDealers(event: TableLazyLoadEvent) {
    this.lastEvent = event;
    const seq = ++this.seq;
    this.loading = true;
    try {
      const { page, size, sort } = pageOf(event);
      const response = await firstValueFrom(this.dealerService.listDealers(page, size, sort));
      if (seq !== this.seq) return;
      this.dealers = response.content;
      this.total = response.page.totalElements;
    } catch (error) {
      if (seq === this.seq) this.showError(error, 'Failed to load the dealers');
    } finally {
      if (seq === this.seq) this.loading = false;
    }
  }

  openForm(publicId: string | null) {
    this.selectedDealerId = publicId;
    this.isFormVisible = true;
  }

  openStand(dealer: DealerResponse) {
    this.standDealer = dealer;
    this.isStandVisible = true;
  }

  onSaved() {
    this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Dealer saved' });
    this.reload();
  }

  confirmRetire(dealer: DealerResponse) {
    this.confirmationService.confirm({
      header: 'Retire dealer',
      message: `Retire ${dealer.code}? Its stand shows nothing from then on, and no new dealer takes its code.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Retire',
      rejectLabel: 'Cancel',
      accept: async () => {
        try {
          await firstValueFrom(this.dealerService.retireDealer(dealer.publicId));
          this.messageService.add({ severity: 'success', summary: 'Retired', detail: `${dealer.code} is retired` });
          this.reload();
        } catch (error) {
          this.showError(error, 'Failed to retire the dealer');
        }
      },
    });
  }

  private reload() {
    if (this.lastEvent) void this.loadDealers(this.lastEvent);
  }

  private async loadSettings() {
    try {
      this.customStandItemsEnabled = (await firstValueFrom(this.dealerService.getSettings())).customStandItemsEnabled;
    } catch (error) {
      this.showError(error, "Failed to load inventory's settings");
    }
  }

  private showError(error: unknown, fallback: string) {
    console.error(fallback, error);
    this.messageService.add({ severity: 'error', summary: 'Error', detail: problemDetail(error, fallback) });
  }
}
