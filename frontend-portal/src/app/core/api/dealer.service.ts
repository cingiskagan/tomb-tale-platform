import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RUNTIME_CONFIG } from '../config';
import { PagedResponse } from './common.model';
import {
  CreateDealerRequest,
  DealerResponse,
  InventorySettings,
  Stand,
  StandEntry,
  StandEntryRequest,
  UpdateDealerRequest,
} from './dealer.models';

/** Dealers and their stands in service-inventory. Only `platform_admin` changes them, and anyone reads a stand. */
@Injectable({
  providedIn: 'root',
})
export class DealerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(RUNTIME_CONFIG).apiBaseUrl}/api/v1/inventory`;

  listDealers(page = 0, size = 20, sort?: string): Observable<PagedResponse<DealerResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (sort) params = params.set('sort', sort);
    return this.http.get<PagedResponse<DealerResponse>>(`${this.baseUrl}/dealers`, { params });
  }

  getDealer(publicId: string): Observable<DealerResponse> {
    return this.http.get<DealerResponse>(`${this.baseUrl}/dealers/${publicId}`);
  }

  createDealer(request: CreateDealerRequest): Observable<DealerResponse> {
    return this.http.post<DealerResponse>(`${this.baseUrl}/dealers`, request);
  }

  /** Answers 409 when someone changed the dealer since `request.version`. */
  updateDealer(publicId: string, request: UpdateDealerRequest): Observable<DealerResponse> {
    return this.http.put<DealerResponse>(`${this.baseUrl}/dealers/${publicId}`, request);
  }

  retireDealer(publicId: string): Observable<DealerResponse> {
    return this.http.post<DealerResponse>(`${this.baseUrl}/dealers/${publicId}/retire`, null);
  }

  /** A stand is found by its dealer's code, as the game finds it (ADR 0032). */
  getStand(dealerCode: string): Observable<Stand> {
    return this.http.get<Stand>(`${this.baseUrl}/stands/${dealerCode}`);
  }

  addEntry(dealerCode: string, request: StandEntryRequest): Observable<StandEntry> {
    return this.http.post<StandEntry>(`${this.baseUrl}/stands/${dealerCode}/entries`, request);
  }

  removeEntry(dealerCode: string, entryId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/stands/${dealerCode}/entries/${entryId}`);
  }

  getSettings(): Observable<InventorySettings> {
    return this.http.get<InventorySettings>(`${this.baseUrl}/settings`);
  }
}
