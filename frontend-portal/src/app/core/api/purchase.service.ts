import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreatePurchaseRequest,
  Page,
  PurchaseFilterRequest,
  PurchaseResponse,
  UpdatePurchaseRequest,
} from './purchase.models';

/**
 * Service to handle CRUD operations for in-game purchases.
 * Interacts with the service-commerce /api/v1/purchases endpoints.
 */
@Injectable({
  providedIn: 'root',
})
export class PurchaseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/purchases`;

  /**
   * Retrieves a paginated list of purchases based on filters.
   *
   * @param filter The filter parameters
   * @param page The requested page index (0-based)
   * @param size The number of items per page
   * @param sort Sort format like "purchasedAt,desc"
   * @returns An observable of the paginated response
   */
  listPurchases(filter: PurchaseFilterRequest, page = 0, size = 20, sort?: string): Observable<Page<PurchaseResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (sort) {
      params = params.set('sort', sort);
    }

    if (filter.playerId) {
      params = params.set('playerId', filter.playerId);
    }
    if (filter.itemCode) {
      params = params.set('itemCode', filter.itemCode);
    }
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.purchasedAfter) {
      params = params.set('purchasedAfter', filter.purchasedAfter);
    }
    if (filter.purchasedBefore) {
      params = params.set('purchasedBefore', filter.purchasedBefore);
    }

    return this.http.get<Page<PurchaseResponse>>(this.baseUrl, { params });
  }

  /**
   * Retrieves a single purchase by its UUID.
   *
   * @param id The purchase identifier
   * @returns An observable of the purchase
   */
  getPurchaseById(id: string): Observable<PurchaseResponse> {
    return this.http.get<PurchaseResponse>(`${this.baseUrl}/${id}`);
  }

  /**
   * Creates a new purchase.
   *
   * @param request The creation payload
   * @returns An observable of the created purchase
   */
  createPurchase(request: CreatePurchaseRequest): Observable<PurchaseResponse> {
    return this.http.post<PurchaseResponse>(this.baseUrl, request);
  }

  /**
   * Updates an existing purchase (full replacement).
   *
   * @param id The purchase identifier
   * @param request The update payload
   * @returns An observable of the updated purchase
   */
  updatePurchase(id: string, request: UpdatePurchaseRequest): Observable<PurchaseResponse> {
    return this.http.put<PurchaseResponse>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Soft-deletes a purchase.
   *
   * @param id The purchase identifier
   * @returns An empty observable upon completion
   */
  deletePurchase(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
