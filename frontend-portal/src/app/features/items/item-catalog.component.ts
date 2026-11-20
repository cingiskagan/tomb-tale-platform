import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { firstValueFrom } from 'rxjs';
import { ItemCatalogService, ItemSetResponse, ItemTemplateResponse, Stats } from '../../core/api';
import { ConfirmService, NotificationService, PageQuery, initQuery, pageOf, sortBy, turnPage } from '../../core/ui';
import { ItemTemplateFormComponent } from './item-template-form.component';
import { ItemSetFormComponent } from './item-set-form.component';
import { problemDetail } from './problem-detail';

/** Enough for every set in one page, so the template form can offer them all. */
const ALL_SETS = 200;

/** The item catalog for `platform_admin`: templates with their icons, and item sets. */
@Component({
  selector: 'app-item-catalog',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSortModule,
    MatTableModule,
    MatTabsModule,
    ItemTemplateFormComponent,
    ItemSetFormComponent,
  ],
  templateUrl: './item-catalog.component.html',
  styleUrl: './items.css',
})
export class ItemCatalogComponent implements OnInit {
  private readonly catalog = inject(ItemCatalogService);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  readonly templateColumns = ['icon', 'code', 'name', 'itemType', 'price', 'baseStats', 'set', 'status', 'actions'];
  readonly setColumns = ['code', 'name', 'bonus', 'actions'];

  templates: ItemTemplateResponse[] = [];
  templateTotal = 0;
  templatesLoading = true;
  /** Set from a retirement until its reload ends, so no second one starts meanwhile (ADR 0034). */
  busy = false;
  templateQuery: PageQuery = initQuery(20, 'code', 'asc');
  /** Only the newest request fills a table, so a slow answer for an old page never replaces the current one. */
  private templateSeq = 0;

  sets: ItemSetResponse[] = [];
  setTotal = 0;
  setsLoading = true;
  /** Every set, for the template form and the Set column. */
  allSets: ItemSetResponse[] = [];
  setQuery: PageQuery = initQuery(20, 'code', 'asc');
  private setSeq = 0;

  /** Icons whose image failed to load, shown as a generic one. */
  readonly brokenIcons = new Set<string>();

  isTemplateFormVisible = false;
  selectedTemplateId: string | null = null;
  isSetFormVisible = false;
  selectedSetId: string | null = null;

  ngOnInit() {
    void this.loadAllSets();
    void this.loadTemplates(this.templateQuery);
    void this.loadSets(this.setQuery);
  }

  sortTemplates(sort: Sort) {
    void this.loadTemplates(sortBy(this.templateQuery, sort));
  }

  pageTemplates(event: PageEvent) {
    void this.loadTemplates(turnPage(this.templateQuery, event));
  }

  sortSets(sort: Sort) {
    void this.loadSets(sortBy(this.setQuery, sort));
  }

  pageSets(event: PageEvent) {
    void this.loadSets(turnPage(this.setQuery, event));
  }

  async loadTemplates(query: PageQuery) {
    this.templateQuery = query;
    const seq = ++this.templateSeq;
    this.templatesLoading = true;
    try {
      const { page, size, sort } = pageOf(query);
      const response = await firstValueFrom(this.catalog.listTemplates(page, size, sort));
      if (seq !== this.templateSeq) return;
      this.templates = response.content;
      this.templateTotal = response.page.totalElements;
    } catch (error) {
      if (seq === this.templateSeq) {
        this.showError(error, 'Failed to load the templates');
      }
    } finally {
      if (seq === this.templateSeq) {
        this.templatesLoading = false;
      }
    }
  }

  async loadSets(query: PageQuery) {
    this.setQuery = query;
    const seq = ++this.setSeq;
    this.setsLoading = true;
    try {
      const { page, size, sort } = pageOf(query);
      const response = await firstValueFrom(this.catalog.listSets(page, size, sort));
      if (seq !== this.setSeq) return;
      this.sets = response.content;
      this.setTotal = response.page.totalElements;
    } catch (error) {
      if (seq === this.setSeq) {
        this.showError(error, 'Failed to load the sets');
      }
    } finally {
      if (seq === this.setSeq) {
        this.setsLoading = false;
      }
    }
  }

  iconUrl(hash: string): string {
    return this.catalog.iconUrl(hash);
  }

  setName(publicId: string | null): string {
    return this.allSets.find((itemSet) => itemSet.publicId === publicId)?.name ?? '';
  }

  formatStats(stats: Stats): string {
    return Object.entries(stats)
      .map(([stat, value]) => `${stat} ${value}`)
      .join(', ');
  }

  openTemplate(publicId: string | null) {
    this.selectedTemplateId = publicId;
    this.isTemplateFormVisible = true;
  }

  openSet(publicId: string | null) {
    this.selectedSetId = publicId;
    this.isSetFormVisible = true;
  }

  onTemplateSaved() {
    this.notifications.success('Template saved');
    void this.loadTemplates(this.templateQuery);
  }

  onSetSaved() {
    this.notifications.success('Item set saved');
    void this.loadAllSets();
    void this.loadSets(this.setQuery);
  }

  async confirmRetire(template: ItemTemplateResponse) {
    if (this.busy) return;
    const accepted = await this.confirm.ask({
      title: 'Retire template',
      message: `Retire ${template.code}? It is never sold or granted again. Copies players own stay.`,
      accept: 'Retire',
      reject: 'Cancel',
    });
    if (!accepted || this.busy) return;
    this.busy = true;
    try {
      await firstValueFrom(this.catalog.retireTemplate(template.publicId));
      this.notifications.success(`${template.code} is retired`);
      await this.loadTemplates(this.templateQuery);
    } catch (error) {
      this.showError(error, 'Failed to retire the template');
    } finally {
      this.busy = false;
    }
  }

  private async loadAllSets() {
    try {
      this.allSets = (await firstValueFrom(this.catalog.listSets(0, ALL_SETS))).content;
    } catch (error) {
      this.showError(error, 'Failed to load the sets');
    }
  }

  private showError(error: unknown, fallback: string) {
    console.error(fallback, error);
    this.notifications.error(problemDetail(error, fallback));
  }
}
