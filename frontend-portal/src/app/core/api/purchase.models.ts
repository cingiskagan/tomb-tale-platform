export enum PurchaseStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  REFUNDED = 'REFUNDED',
  CANCELLED = 'CANCELLED'
}

export interface PurchaseResponse {
  publicId: string;
  playerId: string;
  itemCode: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: PurchaseStatus;
  createdAt: string;
}

export interface PurchaseFilterRequest {
  playerId?: string | null;
  itemCode?: string | null;
  status?: PurchaseStatus | null;
  createdAfter?: string | null;
  createdBefore?: string | null;
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
