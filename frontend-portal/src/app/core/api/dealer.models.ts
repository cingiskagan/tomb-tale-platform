import { ItemType, Stats } from './item-catalog.models';
import { Rarity } from './inventory.models';

/** An NPC that sells the items on its stand. It is retired, never deleted. */
export interface DealerResponse {
  publicId: string;
  /** Stable, such as `TOWN_BLACKSMITH`. A stand is found by it (ADR 0032). */
  code: string;
  name: string;
  retired: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDealerRequest {
  code: string;
  name: string;
}

/** The code never changes, and `version` is the one the edit started from. */
export interface UpdateDealerRequest {
  name: string;
  version: number;
}

/** One item a dealer sells. It never sells out. */
export interface StandEntry {
  publicId: string;
  code: string;
  name: string;
  itemType: ItemType;
  /** A path on the API's origin, or null for a generic icon. */
  iconUrl: string | null;
  baseStats: Stats;
  rarity: Rarity;
  /** The bonus every copy of a custom entry carries, or null when each copy rolls its own. */
  bonus: Stats | null;
  /** The template's price times the rarity's multiplier. */
  price: number;
}

/** A dealer's stand. A retired dealer's has no entries. */
export interface Stand {
  code: string;
  name: string;
  entries: StandEntry[];
}

/** A bonus makes it a custom entry, which inventory refuses while custom stand items are off. */
export interface StandEntryRequest {
  templateId: string;
  rarity: Rarity;
  bonus?: Stats;
}

/** The settings inventory runs with, so the portal hides what inventory would refuse. */
export interface InventorySettings {
  customStandItemsEnabled: boolean;
}
