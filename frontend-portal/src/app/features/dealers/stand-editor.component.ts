import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Observable, firstValueFrom } from 'rxjs';
import {
  BonusLine,
  DealerResponse,
  DealerService,
  InventoryService,
  ItemCatalogService,
  ItemTemplateResponse,
  Rarity,
  StandEntry,
} from '../../core/api';
import { DialogDirective, NotificationService, PageQuery, firstPage, pageOf, sortBy, turnPage } from '../../core/ui';
import { problemDetail } from '../items/problem-detail';
import { CustomEntryFormComponent, RARITY_OPTIONS } from './custom-entry-form.component';

/** One dealer's stand for `platform_admin`. A template dragged from the catalog joins it at the picked rarity. */
@Component({
  selector: 'app-stand-editor',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
    DialogDirective,
    CustomEntryFormComponent,
  ],
  templateUrl: './stand-editor.component.html',
  styleUrl: './dealers.css',
})
export class StandEditorComponent {
  private readonly dealerService = inject(DealerService);
  private readonly catalog = inject(ItemCatalogService);
  private readonly inventoryService = inject(InventoryService);
  private readonly notifications = inject(NotificationService);

  /** Whether inventory takes custom entries. The page reads it from inventory's settings. */
  @Input() customEnabled = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  readonly rarities = RARITY_OPTIONS;
  /** The rarity a dropped template joins the stand at. */
  readonly rarity = new FormControl(Rarity.GRAY, { nonNullable: true });

  entries: StandEntry[] = [];
  /** Set from a change until the stand's reload ends, so no second change starts meanwhile (ADR 0034). */
  busy = false;
  templates: ItemTemplateResponse[] = [];
  templateTotal = 0;
  templatesLoading = true;
  templateQuery: PageQuery = firstPage(10, 'code', 'asc');
  /** Icons whose image failed to load, shown as a generic one. */
  readonly brokenIcons = new Set<string>();

  customVisible = false;
  customTemplate: ItemTemplateResponse | null = null;

  private _visible = false;
  private _dealer: DealerResponse | null = null;
  /** Moves when the dialog opens another dealer or closes, so a late answer for the old one is dropped. */
  private session = 0;
  /** Only the newest stand read fills the stand, so a read that started before a change can't undo it. */
  private standRead = 0;
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

  /** The catalog's columns, with the custom one only while inventory takes custom entries. */
  get catalogColumns(): string[] {
    return this.customEnabled ? ['icon', 'code', 'name', 'price', 'custom'] : ['icon', 'code', 'name', 'price'];
  }

  sortTemplates(sort: Sort) {
    void this.loadTemplates(sortBy(this.templateQuery, sort));
  }

  pageTemplates(event: PageEvent) {
    void this.loadTemplates(turnPage(this.templateQuery, event));
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

  formatBonus(bonus: BonusLine[]): string {
    return bonus.map((line) => `${line.stat} ${line.value}`).join(', ');
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
    if (this.dragged && !this.busy) event.preventDefault();
  }

  async dropOnStand(event: DragEvent) {
    event.preventDefault();
    const template = this.dragged;
    this.dragged = null;
    const dealer = this._dealer;
    if (!template || !dealer || this.busy) return;
    const entry = { templateId: template.publicId, rarity: this.rarity.value };
    await this.change(this.dealerService.addEntry(dealer.code, entry), 'Inventory refused the entry');
  }

  openCustom(template: ItemTemplateResponse) {
    this.customTemplate = template;
    this.customVisible = true;
  }

  onCustomSaved() {
    void this.loadStand(this.session);
  }

  async remove(entry: StandEntry) {
    const dealer = this._dealer;
    if (!dealer || this.busy) return;
    await this.change(this.dealerService.removeEntry(dealer.code, entry.publicId), 'Failed to remove the entry');
  }

  private async open() {
    const session = ++this.session;
    // A change still on its way belongs to the old session, which can no longer clear this.
    this.busy = false;
    this.entries = [];
    // Each opening starts the catalog over, on its first page by code.
    void this.loadTemplates(firstPage(10, 'code', 'asc'));
    await this.loadStand(session);
  }

  private async change(request: Observable<unknown>, fallback: string) {
    const session = this.session;
    this.busy = true;
    try {
      await firstValueFrom(request);
      await this.loadStand(session);
    } catch (error) {
      if (session === this.session) this.showError(error, fallback);
    } finally {
      if (session === this.session) this.busy = false;
    }
  }

  /** A change reloads the stand rather than patching it, and the read after it is the one that counts. */
  private async loadStand(session: number) {
    const dealer = this._dealer;
    // A change for a dealer the dialog no longer shows must not drop the shown dealer's read.
    if (!dealer || session !== this.session) return;
    const read = ++this.standRead;
    try {
      const stand = await firstValueFrom(this.dealerService.getStand(dealer.code));
      if (session === this.session && read === this.standRead) this.entries = stand.entries;
    } catch (error) {
      if (session === this.session && read === this.standRead) this.showError(error, 'Failed to load the stand');
    }
  }

  private showError(error: unknown, fallback: string) {
    console.error(fallback, error);
    this.notifications.error(problemDetail(error, fallback));
  }
}
