/** What kind of item a template is. Mirrors `ItemType` in service-inventory. */
export enum ItemType {
  ONE_HANDED_WEAPON = 'ONE_HANDED_WEAPON',
  TWO_HANDED_WEAPON = 'TWO_HANDED_WEAPON',
  SHIELD = 'SHIELD',
  HELMET = 'HELMET',
  ARMOR = 'ARMOR',
  GLOVES = 'GLOVES',
  BOOTS = 'BOOTS',
  AMULET = 'AMULET',
  RING = 'RING',
}

/** The 15 stats an item can carry. Mirrors `Stat` in service-inventory, which refuses any other name. */
export const STATS = [
  'PHYSICAL_DAMAGE',
  'FIRE_DAMAGE',
  'ICE_DAMAGE',
  'DREAD_DAMAGE',
  'DIVINE_DAMAGE',
  'CRIT_CHANCE',
  'CRIT_DAMAGE',
  'ARMOR',
  'MAX_HP',
  'FIRE_RESISTANCE',
  'ICE_RESISTANCE',
  'DREAD_RESISTANCE',
  'DIVINE_RESISTANCE',
  'MAX_STAMINA',
  'STAMINA_REGENERATION',
] as const;

export type Stat = (typeof STATS)[number];

/** Stat names with their values, as base stats or a bonus. */
export type Stats = Partial<Record<Stat, number>>;

export interface ItemTemplateResponse {
  publicId: string;
  code: string;
  name: string;
  itemType: ItemType;
  maxStackSize: number;
  price: number;
  baseStats: Stats;
  assetKey: string;
  iconHash: string | null;
  itemSetId: string | null;
  retired: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateItemTemplateRequest {
  code: string;
  name: string;
  itemType: ItemType;
  price: number;
  baseStats: Stats;
  assetKey: string;
  iconHash: string | null;
  itemSetId: string | null;
}

/** The code and the type never change, and `version` is the one the edit started from. */
export interface UpdateItemTemplateRequest {
  name: string;
  price: number;
  baseStats: Stats;
  assetKey: string;
  iconHash: string | null;
  itemSetId: string | null;
  version: number;
}

export interface ItemSetResponse {
  publicId: string;
  code: string;
  name: string;
  bonus: Stats;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateItemSetRequest {
  code: string;
  name: string;
  bonus: Stats;
}

export interface UpdateItemSetRequest {
  name: string;
  bonus: Stats;
  version: number;
}

export interface ItemIconResponse {
  hash: string;
  contentType: string;
}
