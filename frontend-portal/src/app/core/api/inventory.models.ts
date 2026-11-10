import { ItemType, Stats } from './item-catalog.models';

/** A character's nine equipment slots. Mirrors `EquipmentSlot` in service-inventory. */
export enum EquipmentSlot {
  MAIN_HAND = 'MAIN_HAND',
  OFF_HAND = 'OFF_HAND',
  HEAD = 'HEAD',
  CHEST = 'CHEST',
  HANDS = 'HANDS',
  FEET = 'FEET',
  NECK = 'NECK',
  RING_1 = 'RING_1',
  RING_2 = 'RING_2',
}

/** An item's rarity, lowest first. Mirrors `Rarity` in service-inventory. */
export enum Rarity {
  GRAY = 'GRAY',
  GREEN = 'GREEN',
  BLUE = 'BLUE',
  PURPLE = 'PURPLE',
  ORANGE = 'ORANGE',
  RED = 'RED',
}

/** One item or stack a character owns. It has a slot or a cell, never both. */
export interface InventoryItem {
  publicId: string;
  code: string;
  name: string;
  itemType: ItemType;
  /** A path on the API's origin, or null for a generic icon. */
  iconUrl: string | null;
  rarity: Rarity;
  bonus: Stats;
  finalStats: Stats;
  quantity: number;
  equipmentSlot: EquipmentSlot | null;
  bagPosition: number | null;
}

export interface SetBonus {
  code: string;
  name: string;
  bonus: Stats;
}

/** What every inventory read and change answers with. Stats are computed at read time. */
export interface CharacterInventory {
  characterId: string;
  coins: number;
  equipment: Partial<Record<EquipmentSlot, InventoryItem>>;
  bag: InventoryItem[];
  setBonuses: SetBonus[];
}
