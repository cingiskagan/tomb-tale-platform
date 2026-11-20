import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { firstValueFrom } from 'rxjs';
import { DealerResponse, DealerService } from '../../core/api';
import { ConfirmService, NotificationService, PageQuery, initQuery, pageOf, sortBy, turnPage } from '../../core/ui';
import { problemDetail } from '../items/problem-detail';
import { DealerFormComponent } from './dealer-form.component';
import { StandEditorComponent } from './stand-editor.component';

/** The dealers for `platform_admin`, each with a Store button that opens its stand. */
@Component({
  selector: 'app-dealer-list',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatTableModule,
    DealerFormComponent,
    StandEditorComponent,
  ],
  templateUrl: './dealer-list.component.html',
  styleUrl: './dealers.css',
})
export class DealerListComponent implements OnInit {
  private readonly dealerService = inject(DealerService);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  readonly columns = ['code', 'name', 'status', 'actions'];

  dealers: DealerResponse[] = [];
  total = 0;
  loading = true;
  /** Set from a retirement until its reload ends, so no second one starts meanwhile (ADR 0034). */
  busy = false;
  query: PageQuery = initQuery(20, 'code', 'asc');
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
    void this.loadDealers(this.query);
  }

  sortDealers(sort: Sort) {
    void this.loadDealers(sortBy(this.query, sort));
  }

  pageDealers(event: PageEvent) {
    void this.loadDealers(turnPage(this.query, event));
  }

  async loadDealers(query: PageQuery) {
    this.query = query;
    const seq = ++this.seq;
    this.loading = true;
    try {
      const { page, size, sort } = pageOf(query);
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
    this.notifications.success('Dealer saved');
    void this.loadDealers(this.query);
  }

  async confirmRetire(dealer: DealerResponse) {
    if (this.busy) return;
    const accepted = await this.confirm.ask({
      title: 'Retire dealer',
      message: `Retire ${dealer.code}? Its stand shows nothing from then on, and no new dealer takes its code.`,
      accept: 'Retire',
      reject: 'Cancel',
    });
    if (!accepted || this.busy) return;
    this.busy = true;
    try {
      await firstValueFrom(this.dealerService.retireDealer(dealer.publicId));
      this.notifications.success(`${dealer.code} is retired`);
      await this.loadDealers(this.query);
    } catch (error) {
      this.showError(error, 'Failed to retire the dealer');
    } finally {
      this.busy = false;
    }
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
    this.notifications.error(problemDetail(error, fallback));
  }
}
