import { Component, OnInit, inject } from '@angular/core';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TabsModule } from 'primeng/tabs';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';
import { ItemCatalogService, ItemSetResponse, ItemTemplateResponse, Stats } from '../../core/api';
import { ItemTemplateFormComponent } from './item-template-form.component';
import { ItemSetFormComponent } from './item-set-form.component';
import { problemDetail } from './problem-detail';

const PAGE_SIZE = 20;
/** Enough for every set in one page, so the template form can offer them all. */
const ALL_SETS = 200;

/** The item catalog for `platform_admin`: templates with their icons, and item sets. */
@Component({
  selector: 'app-item-catalog',
  standalone: true,
  imports: [
    TableModule,
    ButtonModule,
    TagModule,
    TabsModule,
    ConfirmDialogModule,
    ToastModule,
    ItemTemplateFormComponent,
    ItemSetFormComponent,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './item-catalog.component.html',
  styleUrl: './items.css',
})
export class ItemCatalogComponent implements OnInit {
  private readonly catalog = inject(ItemCatalogService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  templates: ItemTemplateResponse[] = [];
  templateTotal = 0;
  templatesLoading = true;
  private lastTemplateEvent: TableLazyLoadEvent | null = null;

  sets: ItemSetResponse[] = [];
  setTotal = 0;
  setsLoading = true;
  /** Every set, for the template form and the Set column. */
  allSets: ItemSetResponse[] = [];
  private lastSetEvent: TableLazyLoadEvent | null = null;

  /** Icons whose image failed to load, shown as a generic one. */
  readonly brokenIcons = new Set<string>();

  isTemplateFormVisible = false;
  selectedTemplateId: string | null = null;
  isSetFormVisible = false;
  selectedSetId: string | null = null;

  ngOnInit() {
    void this.loadAllSets();
  }

  async loadTemplates(event: TableLazyLoadEvent) {
    this.lastTemplateEvent = event;
    this.templatesLoading = true;
    try {
      const { page, size, sort } = pageOf(event);
      const response = await firstValueFrom(this.catalog.listTemplates(page, size, sort));
      this.templates = response.content;
      this.templateTotal = response.page.totalElements;
    } catch (error) {
      this.showError(error, 'Failed to load the templates');
    } finally {
      this.templatesLoading = false;
    }
  }

  async loadSets(event: TableLazyLoadEvent) {
    this.lastSetEvent = event;
    this.setsLoading = true;
    try {
      const { page, size, sort } = pageOf(event);
      const response = await firstValueFrom(this.catalog.listSets(page, size, sort));
      this.sets = response.content;
      this.setTotal = response.page.totalElements;
    } catch (error) {
      this.showError(error, 'Failed to load the sets');
    } finally {
      this.setsLoading = false;
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
    this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Template saved' });
    this.reloadTemplates();
  }

  onSetSaved() {
    this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Item set saved' });
    void this.loadAllSets();
    if (this.lastSetEvent) {
      void this.loadSets(this.lastSetEvent);
    }
  }

  confirmRetire(template: ItemTemplateResponse) {
    this.confirmationService.confirm({
      header: 'Retire template',
      message: `Retire ${template.code}? It is never sold or granted again. Copies players own stay.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Retire',
      rejectLabel: 'Cancel',
      accept: async () => {
        try {
          await firstValueFrom(this.catalog.retireTemplate(template.publicId));
          this.messageService.add({ severity: 'success', summary: 'Retired', detail: `${template.code} is retired` });
          this.reloadTemplates();
        } catch (error) {
          this.showError(error, 'Failed to retire the template');
        }
      },
    });
  }

  private reloadTemplates() {
    if (this.lastTemplateEvent) {
      void this.loadTemplates(this.lastTemplateEvent);
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
    this.messageService.add({ severity: 'error', summary: 'Error', detail: problemDetail(error, fallback) });
  }
}

function pageOf(event: TableLazyLoadEvent): { page: number; size: number; sort?: string } {
  const size = event.rows || PAGE_SIZE;
  const page = event.first ? Math.floor(event.first / size) : 0;
  const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
  const sort = field ? `${field},${event.sortOrder === -1 ? 'desc' : 'asc'}` : undefined;
  return { page, size, sort };
}
