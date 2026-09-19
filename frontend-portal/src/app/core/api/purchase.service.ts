import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RUNTIME_CONFIG } from '../config';
import {
  CreatePurchaseRequest,
  PurchaseFilterRequest,
  PurchaseResponse,
  UpdatePurchaseRequest,
} from './purchase.models';
import { PagedResponse } from './common.model';

/**
 * Service to handle CRUD operations for in-game purchases.
 * Interacts with the service-commerce /api/v1/purchases endpoints.
 */
@Injectable({
  providedIn: 'root',
})
export class PurchaseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(RUNTIME_CONFIG).apiBaseUrl}/api/v1/purchases`;

  /**
   * Retrieves a paginated list of purchases based on filters.
   *
   * @param filter The filter parameters
   * @param page The requested page index (0-based)
   * @param size The number of items per page
   * @param sort Sort format like "createdAt,desc"
   * @returns An observable of the paginated response
   */
  listPurchases(filter: PurchaseFilterRequest, page = 0, size = 20, sort?: string): Observable<PagedResponse<PurchaseResponse>> {
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
    if (filter.createdAfter) {
      params = params.set('createdAfter', filter.createdAfter);
    }
    if (filter.createdBefore) {
      params = params.set('createdBefore', filter.createdBefore);
    }

    return this.http.get<PagedResponse<PurchaseResponse>>(this.baseUrl, { params });
  }

  /**
   * Retrieves a single purchase by its publicId.
   *
   * @param publicId The purchase identifier
   * @returns An observable of the purchase
   */
  getPurchaseByPublicId(publicId: string): Observable<PurchaseResponse> {
    return this.http.get<PurchaseResponse>(`${this.baseUrl}/${publicId}`);
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
   * Applies a partial update to an existing purchase.
   *
   * @param publicId The purchase identifier
   * @param request The fields to change
   * @returns An observable of the updated purchase
   */
  updatePurchase(publicId: string, request: UpdatePurchaseRequest): Observable<PurchaseResponse> {
    return this.http.patch<PurchaseResponse>(`${this.baseUrl}/${publicId}`, request);
  }

  /**
   * Soft-deletes a purchase.
   *
   * @param publicId The purchase identifier
   * @returns An empty observable upon completion
   */
  deletePurchase(publicId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${publicId}`);
  }
}
