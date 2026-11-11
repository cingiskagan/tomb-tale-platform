import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { MessageService } from 'primeng/api';
import { Observable, firstValueFrom } from 'rxjs';
import {
  CharacterInventory,
  EquipmentSlot,
  GameCharacter,
  InventoryItem,
  InventoryService,
  Stats,
} from '../../core/api';
import { problemDetail } from '../items/problem-detail';

/** The bag is drawn this many cells wide. Inventory stores one number per cell, so the width is ours to pick. */
export const BAG_COLUMNS = 8;
const MIN_BAG_ROWS = 4;

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
 * A character's slots, bag and coins for `platform_admin`. Drag and drop equips, unequips and moves, and the
 * dialog redraws from inventory's answer.
 */
@Component({
  selector: 'app-inventory-dialog',
  standalone: true,
  imports: [DialogModule, DecimalPipe, NgTemplateOutlet],
  templateUrl: './inventory-dialog.component.html',
  styleUrl: './inventory-dialog.component.css',
})
export class InventoryDialogComponent {
  private readonly inventoryService = inject(InventoryService);
  private readonly messageService = inject(MessageService);

  @Output() visibleChange = new EventEmitter<boolean>();

  readonly slots = SLOTS;
  readonly columns = BAG_COLUMNS;

  inventory: CharacterInventory | null = null;
  cells: (InventoryItem | null)[] = [];
  /** True while a change is on its way, so a second drag can't start from a stale picture. */
  busy = false;
  /** Icon paths whose image failed to load, shown as a generic one. */
  readonly brokenIcons = new Set<string>();

  private _visible = false;
  private _character: GameCharacter | null = null;
  private dragged: InventoryItem | null = null;
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

  iconUrl(item: InventoryItem): string | null {
    return item.iconUrl && !this.brokenIcons.has(item.iconUrl) ? this.inventoryService.iconUrl(item.iconUrl) : null;
  }

  /** The hover text: the name and rarity, then the bonus and the final stats. */
  describe(item: InventoryItem): string {
    return [
      `${item.name} (${item.rarity.toLowerCase()})`,
      `Bonus: ${formatStats(item.bonus) || 'none'}`,
      `Stats: ${formatStats(item.finalStats) || 'none'}`,
    ].join('\n');
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

  onDragEnd() {
    this.dragged = null;
  }

  allowDrop(event: DragEvent) {
    if (this.dragged) {
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

  /** An equipped item dropped on the bag goes to the lowest free cell, and a bag item to the cell it is dropped on. */
  dropOnCell(event: DragEvent, cell: number) {
    event.preventDefault();
    const item = this.take();
    const slot = item?.equipmentSlot;
    if (slot) {
      void this.change((id) => this.inventoryService.unequip(id, slot), 'Inventory refused the unequip');
    } else if (item && item.bagPosition !== cell) {
      void this.change((id) => this.inventoryService.move(id, cell, item.publicId), 'Inventory refused the move');
    }
  }

  private take(): InventoryItem | null {
    const item = this.dragged;
    this.dragged = null;
    return this.busy ? null : item;
  }

  private async open() {
    const session = ++this.session;
    // A change still on its way belongs to the old session, which can no longer clear this.
    this.busy = false;
    this.show(null);
    const character = this._character;
    if (!character) return;
    try {
      await this.pending;
      if (session !== this.session) return;
      const inventory = await firstValueFrom(this.inventoryService.getInventory(character.publicId));
      if (session === this.session) this.show(inventory);
    } catch (error) {
      if (session === this.session) this.showError(error, 'Failed to load the inventory');
    }
  }

  private async change(request: (characterId: string) => Observable<CharacterInventory>, fallback: string) {
    if (!this.inventory) return;
    const session = this.session;
    this.busy = true;
    const answer = firstValueFrom(request(this.inventory.characterId));
    this.pending = answer.catch(() => undefined);
    try {
      const inventory = await answer;
      if (session === this.session) this.show(inventory);
    } catch (error) {
      if (session === this.session) this.showError(error, fallback);
    } finally {
      if (session === this.session) this.busy = false;
    }
  }

  private show(inventory: CharacterInventory | null) {
    this.inventory = inventory;
    this.cells = inventory ? bagCells(inventory.bag) : [];
  }

  private showError(error: unknown, fallback: string) {
    console.error(fallback, error);
    this.messageService.add({ severity: 'error', summary: 'Error', detail: problemDetail(error, fallback) });
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

function formatStats(stats: Stats): string {
  return Object.entries(stats)
    .map(([stat, value]) => `${stat} ${value}`)
    .join(', ');
}
