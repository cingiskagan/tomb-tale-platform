import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RUNTIME_CONFIG } from '../config';
import { CharacterInventory, EquipmentSlot } from './inventory.models';

/** A character's inventory in service-inventory. Every change answers with the inventory after it. */
@Injectable({
  providedIn: 'root',
})
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(RUNTIME_CONFIG).apiBaseUrl;
  private readonly baseUrl = `${this.apiBaseUrl}/api/v1/inventory/characters`;

  getInventory(characterId: string): Observable<CharacterInventory> {
    return this.http.get<CharacterInventory>(`${this.baseUrl}/${characterId}`);
  }

  /** The item the slot held goes to the bag. */
  equip(characterId: string, slot: EquipmentSlot, itemId: string): Observable<CharacterInventory> {
    return this.http.put<CharacterInventory>(`${this.baseUrl}/${characterId}/equipment/${slot}`, { itemId });
  }

  /** The item goes to the lowest free cell. */
  unequip(characterId: string, slot: EquipmentSlot): Observable<CharacterInventory> {
    return this.http.delete<CharacterInventory>(`${this.baseUrl}/${characterId}/equipment/${slot}`);
  }

  /** A taken cell swaps the two items. */
  move(characterId: string, cell: number, itemId: string): Observable<CharacterInventory> {
    return this.http.put<CharacterInventory>(`${this.baseUrl}/${characterId}/bag/${cell}`, { itemId });
  }

  /** Inventory answers with the icon's path, which needs no token. */
  iconUrl(path: string): string {
    return `${this.apiBaseUrl}${path}`;
  }
}
