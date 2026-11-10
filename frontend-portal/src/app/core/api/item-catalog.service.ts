import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RUNTIME_CONFIG } from '../config';
import { PagedResponse } from './common.model';
import {
  CreateItemSetRequest,
  CreateItemTemplateRequest,
  ItemIconResponse,
  ItemSetResponse,
  ItemTemplateResponse,
  UpdateItemSetRequest,
  UpdateItemTemplateRequest,
} from './item-catalog.models';

/** The item catalog in service-inventory: templates, their icons and item sets, all for `platform_admin`. */
@Injectable({
  providedIn: 'root',
})
export class ItemCatalogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(RUNTIME_CONFIG).apiBaseUrl}/api/v1/inventory`;

  listTemplates(page = 0, size = 20, sort?: string): Observable<PagedResponse<ItemTemplateResponse>> {
    return this.http.get<PagedResponse<ItemTemplateResponse>>(`${this.baseUrl}/templates`, {
      params: pageParams(page, size, sort),
    });
  }

  getTemplate(publicId: string): Observable<ItemTemplateResponse> {
    return this.http.get<ItemTemplateResponse>(`${this.baseUrl}/templates/${publicId}`);
  }

  createTemplate(request: CreateItemTemplateRequest): Observable<ItemTemplateResponse> {
    return this.http.post<ItemTemplateResponse>(`${this.baseUrl}/templates`, request);
  }

  /** Answers 409 when someone changed the template since `request.version`. */
  updateTemplate(publicId: string, request: UpdateItemTemplateRequest): Observable<ItemTemplateResponse> {
    return this.http.put<ItemTemplateResponse>(`${this.baseUrl}/templates/${publicId}`, request);
  }

  retireTemplate(publicId: string): Observable<ItemTemplateResponse> {
    return this.http.post<ItemTemplateResponse>(`${this.baseUrl}/templates/${publicId}/retire`, null);
  }

  /** Inventory checks the file's bytes, so a renamed SVG is refused whatever its name says. */
  uploadIcon(file: File): Observable<ItemIconResponse> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<ItemIconResponse>(`${this.baseUrl}/icons`, body);
  }

  /** Needs no token, so an `img` tag loads it directly. */
  iconUrl(hash: string): string {
    return `${this.baseUrl}/icons/${hash}`;
  }

  listSets(page = 0, size = 20, sort?: string): Observable<PagedResponse<ItemSetResponse>> {
    return this.http.get<PagedResponse<ItemSetResponse>>(`${this.baseUrl}/sets`, {
      params: pageParams(page, size, sort),
    });
  }

  getSet(publicId: string): Observable<ItemSetResponse> {
    return this.http.get<ItemSetResponse>(`${this.baseUrl}/sets/${publicId}`);
  }

  createSet(request: CreateItemSetRequest): Observable<ItemSetResponse> {
    return this.http.post<ItemSetResponse>(`${this.baseUrl}/sets`, request);
  }

  /** Answers 409 when someone changed the set since `request.version`. */
  updateSet(publicId: string, request: UpdateItemSetRequest): Observable<ItemSetResponse> {
    return this.http.put<ItemSetResponse>(`${this.baseUrl}/sets/${publicId}`, request);
  }
}

function pageParams(page: number, size: number, sort?: string): HttpParams {
  const params = new HttpParams().set('page', page).set('size', size);
  return sort ? params.set('sort', sort) : params;
}
