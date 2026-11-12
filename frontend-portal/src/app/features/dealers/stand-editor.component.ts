import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { MessageService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';
import {
  DealerResponse,
  DealerService,
  InventoryService,
  ItemCatalogService,
  ItemTemplateResponse,
  Rarity,
  StandEntry,
  Stats,
} from '../../core/api';
import { pageOf } from '../items/page-of';
import { problemDetail } from '../items/problem-detail';
import { CustomEntryFormComponent, RARITY_OPTIONS } from './custom-entry-form.component';

/** One dealer's stand for `platform_admin`. A template dragged from the catalog joins it at the picked rarity. */
@Component({
  selector: 'app-stand-editor',
  standalone: true,
  imports: [DialogModule, TableModule, ButtonModule, TagModule, SelectModule, ReactiveFormsModule, CustomEntryFormComponent],
  templateUrl: './stand-editor.component.html',
  styleUrl: './dealers.css',
})
export class StandEditorComponent {
  private readonly dealerService = inject(DealerService);
  private readonly catalog = inject(ItemCatalogService);
  private readonly inventoryService = inject(InventoryService);
  private readonly messageService = inject(MessageService);

  /** Whether inventory takes custom entries. The page reads it from inventory's settings. */
  @Input() customEnabled = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  readonly rarities = RARITY_OPTIONS;
  /** The rarity a dropped template joins the stand at. */
  readonly rarity = new FormControl(Rarity.GRAY, { nonNullable: true });

  entries: StandEntry[] = [];
  templates: ItemTemplateResponse[] = [];
  templateTotal = 0;
  templatesLoading = true;
  /** Icons whose image failed to load, shown as a generic one. */
  readonly brokenIcons = new Set<string>();

  customVisible = false;
  customTemplate: ItemTemplateResponse | null = null;

  private _visible = false;
  private _dealer: DealerResponse | null = null;
  /** Moves when the dialog opens another dealer or closes, so a late answer for the old one is dropped. */
  private session = 0;
  private templateSeq = 0;
  private dragged: ItemTemplateResponse | null = null;

  @Input()
  set visible(value: boolean) {
    this._visible = value;
    if (value) void this.open();
  }
  get visible() {
    return this._visible;
  }

  @Input()
  set dealer(value: DealerResponse | null) {
    this._dealer = value;
    if (this._visible) void this.open();
  }
  get dealer() {
    return this._dealer;
  }

  close() {
    this.session++;
    this._visible = false;
    this.visibleChange.emit(false);
  }

  async loadTemplates(event: TableLazyLoadEvent) {
    const seq = ++this.templateSeq;
    this.templatesLoading = true;
    try {
      const { page, size, sort } = pageOf(event);
      const response = await firstValueFrom(this.catalog.listTemplates(page, size, sort));
      if (seq !== this.templateSeq) return;
      this.templates = response.content;
      this.templateTotal = response.page.totalElements;
    } catch (error) {
      if (seq === this.templateSeq) this.showError(error, 'Failed to load the catalog');
    } finally {
      if (seq === this.templateSeq) this.templatesLoading = false;
    }
  }

  templateIconUrl(template: ItemTemplateResponse): string | null {
    const hash = template.iconHash;
    return hash && !this.brokenIcons.has(hash) ? this.catalog.iconUrl(hash) : null;
  }

  entryIconUrl(entry: StandEntry): string | null {
    const path = entry.iconUrl;
    return path && !this.brokenIcons.has(path) ? this.inventoryService.iconUrl(path) : null;
  }

  /** An icon that failed to load shows as a generic one from then on. */
  hideIcon(path: string | null) {
    if (path) this.brokenIcons.add(path);
  }

  formatStats(stats: Stats): string {
    return Object.entries(stats)
      .map(([stat, value]) => `${stat} ${value}`)
      .join(', ');
  }

  /** A retired template leaves every stand, so it can't be dragged onto one. */
  onDragStart(event: DragEvent, template: ItemTemplateResponse) {
    if (template.retired) {
      event.preventDefault();
      return;
    }
    this.dragged = template;
    // Firefox starts no drag without data.
    event.dataTransfer?.setData('text/plain', template.publicId);
  }

  onDragEnd() {
    this.dragged = null;
  }

  allowDrop(event: DragEvent) {
    if (this.dragged) event.preventDefault();
  }

  async dropOnStand(event: DragEvent) {
    event.preventDefault();
    const template = this.dragged;
    this.dragged = null;
    const dealer = this._dealer;
    if (!template || !dealer) return;
    const session = this.session;
    try {
      const entry = await firstValueFrom(
        this.dealerService.addEntry(dealer.code, { templateId: template.publicId, rarity: this.rarity.value }),
      );
      if (session === this.session) this.entries = [...this.entries, entry];
    } catch (error) {
      if (session === this.session) this.showError(error, 'Inventory refused the entry');
    }
  }

  openCustom(template: ItemTemplateResponse) {
    this.customTemplate = template;
    this.customVisible = true;
  }

  /** Reloads rather than appends, because the stand may show another dealer by the time the form answers. */
  onCustomSaved() {
    void this.loadStand(this.session);
  }

  async remove(entry: StandEntry) {
    const dealer = this._dealer;
    if (!dealer) return;
    const session = this.session;
    try {
      await firstValueFrom(this.dealerService.removeEntry(dealer.code, entry.publicId));
      if (session === this.session) this.entries = this.entries.filter((other) => other.publicId !== entry.publicId);
    } catch (error) {
      if (session === this.session) this.showError(error, 'Failed to remove the entry');
    }
  }

  private async open() {
    const session = ++this.session;
    this.entries = [];
    await this.loadStand(session);
  }

  private async loadStand(session: number) {
    const dealer = this._dealer;
    if (!dealer) return;
    try {
      const stand = await firstValueFrom(this.dealerService.getStand(dealer.code));
      if (session === this.session) this.entries = stand.entries;
    } catch (error) {
      if (session === this.session) this.showError(error, 'Failed to load the stand');
    }
  }

  private showError(error: unknown, fallback: string) {
    console.error(fallback, error);
    this.messageService.add({ severity: 'error', summary: 'Error', detail: problemDetail(error, fallback) });
  }
}
