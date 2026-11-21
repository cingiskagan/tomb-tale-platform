import { Component, EventEmitter, Input, Output, inject, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { Observable, firstValueFrom } from 'rxjs';
import {
  BonusLine,
  CharacterInventory,
  DealerResponse,
  DealerService,
  EquipmentSlot,
  GameCharacter,
  InventoryItem,
  InventoryService,
  Stand,
  StandEntry,
  Stats,
} from '../../core/api';
import { RUNTIME_CONFIG } from '../../core/config';
import { ConfirmService, DialogDirective, NotificationService } from '../../core/ui';
import { problemDetail } from '../items/problem-detail';

/** The bag is drawn this many cells wide. Inventory stores one number per cell, so the width is ours to pick. */
export const BAG_COLUMNS = 8;
const MIN_BAG_ROWS = 4;
/** The picker reads the dealers this many at a time, page after page until it has them all. */
const DEALER_PAGE = 200;

/** The slots in the order the paper doll lists them. Each one's CSS grid area is its name. */
export const SLOTS: readonly { slot: EquipmentSlot; label: string }[] = [
  { slot: EquipmentSlot.HEAD, label: 'Head' },
  { slot: EquipmentSlot.NECK, label: 'Neck' },
  { slot: EquipmentSlot.MAIN_HAND, label: 'Main hand' },
  { slot: EquipmentSlot.CHEST, label: 'Chest' },
  { slot: EquipmentSlot.OFF_HAND, label: 'Off hand' },
  { slot: EquipmentSlot.HANDS, label: 'Hands' },
  { slot: EquipmentSlot.FEET, label: 'Feet' },
  { slot: EquipmentSlot.RING_1, label: 'Ring' },
  { slot: EquipmentSlot.RING_2, label: 'Ring' },
];

/**
 * A character's slots, bag and coins for `platform_admin`, beside the stand of a dealer the admin picks. Drag and
 * drop equips, unequips, moves, buys and sells, and the dialog redraws from inventory's answer.
 */
@Component({
  selector: 'app-inventory-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    DialogDirective,
    ReactiveFormsModule,
    DecimalPipe,
    NgTemplateOutlet,
  ],
  templateUrl: './inventory-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './inventory-dialog.component.css',
})
export class InventoryDialogComponent {
  private readonly inventoryService = inject(InventoryService);
  private readonly dealerService = inject(DealerService);
  private readonly notifications = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly config = inject(RUNTIME_CONFIG);

  @Output() visibleChange = new EventEmitter<boolean>();

  readonly slots = SLOTS;
  readonly columns = BAG_COLUMNS;

  inventory: CharacterInventory | null = null;
  cells: (InventoryItem | null)[] = [];
  /** True while a change is on its way, so a second drag can't start from a stale picture. */
  busy = false;
  /** Icon paths whose image failed to load, shown as a generic one. */
  readonly brokenIcons = new Set<string>();

  /** The dealers that still sell, for the picker. */
  dealers: DealerResponse[] = [];
  /** The code of the dealer whose stand shows beside the bag. It stays picked when the dialog opens again. */
  readonly dealerCode = new FormControl<string | null>(null);
  stand: Stand | null = null;

  private _visible = false;
  private _character: GameCharacter | null = null;
  private dragged: InventoryItem | null = null;
  private draggedEntry: StandEntry | null = null;
  /** The characters whose inventory the admin agreed to change, so in production each one asks once. */
  private readonly confirmed = new Set<string>();
  /** Moves when the dialog opens another character or closes, so a late answer for the old one is dropped. */
  private session = 0;
  /** The last change sent. Inventory commits before it answers, so a load that waits for it sees the change. */
  private pending: Promise<unknown> = Promise.resolve();

  @Input()
  set visible(value: boolean) {
    this._visible = value;
    if (value) {
      void this.open();
    }
  }
  get visible() {
    return this._visible;
  }

  @Input()
  set character(value: GameCharacter | null) {
    this._character = value;
    if (this._visible) {
      void this.open();
    }
  }
  get character() {
    return this._character;
  }

  close() {
    this.session++;
    this._visible = false;
    this.visibleChange.emit(false);
  }

  /** For an item or a stand entry, which both carry their template's icon path. */
  iconUrl(shown: { iconUrl: string | null }): string | null {
    return shown.iconUrl && !this.brokenIcons.has(shown.iconUrl) ? this.inventoryService.iconUrl(shown.iconUrl) : null;
  }

  /** The hover text: the name and rarity, then the bonus and the final stats. */
  describe(item: InventoryItem): string {
    return [
      `${item.name} (${item.rarity.toLowerCase()})`,
      `Bonus: ${formatBonus(item.bonus) || 'none'}`,
      `Stats: ${formatStats(item.finalStats) || 'none'}`,
    ].join('\n');
  }

  /** The hover text of a stand entry: the name and rarity, the price, and the bonus it carries or rolls. */
  describeEntry(entry: StandEntry): string {
    return [
      `${entry.name} (${entry.rarity.toLowerCase()})`,
      `Price: ${entry.price} coins`,
      entry.bonus ? `Bonus: ${formatBonus(entry.bonus) || 'none'}` : 'Bonus: rolled when bought',
    ].join('\n');
  }

  /** The old dealer's stand goes at once, so a failed load can't leave its entries under the new dealer. */
  pickDealer() {
    this.stand = null;
    void this.loadStand(this.session);
  }

  onDragStart(event: DragEvent, item: InventoryItem) {
    if (this.busy) {
      event.preventDefault();
      return;
    }
    this.dragged = item;
    // Firefox starts no drag without data.
    event.dataTransfer?.setData('text/plain', item.publicId);
  }

  onEntryDragStart(event: DragEvent, entry: StandEntry) {
    if (this.busy) {
      event.preventDefault();
      return;
    }
    this.draggedEntry = entry;
    event.dataTransfer?.setData('text/plain', entry.publicId);
  }

  onDragEnd() {
    this.dragged = null;
    this.draggedEntry = null;
  }

  /** A slot and the stand take an item. */
  allowDrop(event: DragEvent) {
    if (this.dragged) {
      event.preventDefault();
    }
  }

  /** A bag cell takes an item, and a stand entry, which it buys. */
  allowBagDrop(event: DragEvent) {
    if (this.dragged || this.draggedEntry) {
      event.preventDefault();
    }
  }

  /** A bag item dropped on a slot is equipped there. An item from another slot is not moved. */
  dropOnSlot(event: DragEvent, slot: EquipmentSlot) {
    event.preventDefault();
    const item = this.take();
    if (item?.bagPosition != null && item.equipmentSlot !== slot) {
      void this.change((id) => this.inventoryService.equip(id, slot, item.publicId), 'Inventory refused the equip');
    }
  }

  /**
   * A stand entry dropped on the bag is bought into the lowest free cell. An equipped item goes to the lowest free
   * cell too, and a bag item to the cell it is dropped on.
   */
  dropOnCell(event: DragEvent, cell: number) {
    event.preventDefault();
    const entry = this.takeEntry();
    const item = this.take();
    const slot = item?.equipmentSlot;
    if (entry) {
      void this.change(
        (id) => this.inventoryService.buy(id, crypto.randomUUID(), entry.publicId, entry.price),
        'Inventory refused the buy',
        true,
      );
    } else if (slot) {
      void this.change((id) => this.inventoryService.unequip(id, slot), 'Inventory refused the unequip');
    } else if (item && item.bagPosition !== cell) {
      void this.change((id) => this.inventoryService.move(id, cell, item.publicId), 'Inventory refused the move');
    }
  }

  /** An item dropped on the stand is sold. An equipped one is sent too, so inventory says why it refuses. */
  dropOnStand(event: DragEvent) {
    event.preventDefault();
    const item = this.take();
    if (item) {
      void this.change(
        (id) => this.inventoryService.sell(id, crypto.randomUUID(), item.publicId),
        'Inventory refused the sell',
        true,
      );
    }
  }

  private take(): InventoryItem | null {
    const item = this.dragged;
    this.dragged = null;
    return this.busy ? null : item;
  }

  private takeEntry(): StandEntry | null {
    const entry = this.draggedEntry;
    this.draggedEntry = null;
    return this.busy ? null : entry;
  }

  private async open() {
    const session = ++this.session;
    // A change still on its way belongs to the old session, which can no longer clear this.
    this.busy = false;
    this.show(null);
    const character = this._character;
    if (!character) return;
    void this.loadDealers(session);
    try {
      await this.pending;
      if (session !== this.session) return;
      const inventory = await firstValueFrom(this.inventoryService.getInventory(character.publicId));
      if (session === this.session) this.show(inventory);
    } catch (error) {
      if (session === this.session) this.showError(error, 'Failed to load the inventory');
    }
  }

  /**
   * A refused trade means the picture was stale, so the dialog reloads the coins and the prices. It stays busy until
   * that reload ends, so the reload can't land on top of the next change.
   */
  private async change(
    request: (characterId: string) => Observable<CharacterInventory>,
    fallback: string,
    reloadOnRefusal = false,
  ) {
    if (!this.inventory) return;
    const characterId = this.inventory.characterId;
    const session = this.session;
    this.busy = true;
    try {
      if (!(await this.confirmFirstChange(characterId)) || session !== this.session) return;
      const answer = firstValueFrom(request(characterId));
      this.pending = answer.catch(() => undefined);
      try {
        const inventory = await answer;
        if (session === this.session) this.show(inventory);
      } catch (error) {
        if (session !== this.session) return;
        this.showError(error, fallback);
        if (reloadOnRefusal) await this.reload(session);
      }
    } finally {
      if (session === this.session) this.busy = false;
    }
  }

  /** In production the inventory belongs to a live player, so the admin's first change to it asks first. */
  private async confirmFirstChange(characterId: string): Promise<boolean> {
    if (!this.config.production || this.confirmed.has(characterId)) return true;
    const accepted = await this.confirm.ask({
      title: "A live player's inventory",
      message: 'This inventory belongs to a live player, who sees every change at once. Change it anyway?',
      accept: 'Change it',
      reject: 'Cancel',
    });
    if (accepted) this.confirmed.add(characterId);
    return accepted;
  }

  private async reload(session: number) {
    const character = this._character;
    if (!character) return;
    try {
      const inventory = await firstValueFrom(this.inventoryService.getInventory(character.publicId));
      if (session === this.session) this.show(inventory);
    } catch (error) {
      if (session === this.session) this.showError(error, 'Failed to reload the inventory');
    }
    await this.loadStand(session);
  }

  /** Keeps the dealer picked last time while it still sells. */
  private async loadDealers(session: number) {
    try {
      const dealers = await this.allDealers();
      if (session !== this.session) return;
      this.dealers = dealers.filter((dealer) => !dealer.retired);
      if (!this.dealers.some((dealer) => dealer.code === this.dealerCode.value)) this.dealerCode.setValue(null);
      await this.loadStand(session);
    } catch (error) {
      if (session === this.session) this.showError(error, 'Failed to load the dealers');
    }
  }

  private async allDealers(): Promise<DealerResponse[]> {
    const dealers: DealerResponse[] = [];
    for (let page = 0, pages = 1; page < pages; page++) {
      const answer = await firstValueFrom(this.dealerService.listDealers(page, DEALER_PAGE, 'name,asc'));
      dealers.push(...answer.content);
      pages = answer.page.totalPages;
    }
    return dealers;
  }

  private async loadStand(session: number) {
    const code = this.dealerCode.value;
    if (!code) {
      this.stand = null;
      return;
    }
    try {
      const stand = await firstValueFrom(this.dealerService.getStand(code));
      if (session === this.session && this.dealerCode.value === code) this.stand = stand;
    } catch (error) {
      if (session === this.session) this.showError(error, 'Failed to load the stand');
    }
  }

  private show(inventory: CharacterInventory | null) {
    this.inventory = inventory;
    this.cells = inventory ? bagCells(inventory.bag) : [];
  }

  private showError(error: unknown, fallback: string) {
    console.error(fallback, error);
    this.notifications.error(problemDetail(error, fallback));
  }
}

/** Each item at its cell, in whole rows, with at least one free row after the last item to drop on. */
export function bagCells(bag: InventoryItem[]): (InventoryItem | null)[] {
  const highest = Math.max(-1, ...bag.map((item) => item.bagPosition ?? -1));
  const rows = Math.max(MIN_BAG_ROWS, Math.ceil((highest + 1) / BAG_COLUMNS) + 1);
  const cells: (InventoryItem | null)[] = Array.from({ length: rows * BAG_COLUMNS }, () => null);
  bag.forEach((item) => {
    if (item.bagPosition !== null) cells[item.bagPosition] = item;
  });
  return cells;
}

function formatBonus(bonus: BonusLine[]): string {
  return bonus.map((line) => `${line.stat} ${line.value}`).join(', ');
}

function formatStats(stats: Stats): string {
  return Object.entries(stats)
    .map(([stat, value]) => `${stat} ${value}`)
    .join(', ');
}
