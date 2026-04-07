export enum PurchaseStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  REFUNDED = 'REFUNDED',
  CANCELLED = 'CANCELLED'
}

export interface PurchaseResponse {
  id: string;
  playerId: string;
  itemCode: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: PurchaseStatus;
  purchasedAt: string;
}

export interface PurchaseFilterRequest {
  playerId?: string | null;
  itemCode?: string | null;
  status?: PurchaseStatus | null;
  purchasedAfter?: string | null;
  purchasedBefore?: string | null;
}

export interface CreatePurchaseRequest {
  playerId: string;
  itemCode: string;
  quantity: number;
  unitPrice: number;
}

export interface UpdatePurchaseRequest {
  quantity?: number | null;
  status?: PurchaseStatus | null;
}

export interface PageSort {
  sorted: boolean;
  unsorted: boolean;
  empty: boolean;
}

export interface Pageable {
  pageNumber: number;
  pageSize: number;
  sort: PageSort;
  offset: number;
  paged: boolean;
  unpaged: boolean;
}

export interface Page<T> {
  content: T[];
  pageable: Pageable;
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  sort: PageSort;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}
