export interface Category {
  id: number;
  name: string;
  description: string | null;
}

export interface Product {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string | null;
}

export interface ProductPrice {
  id: number;
  productId: number;
  productName: string;
  price: number;
  businessFrom: string;
  businessThru: string;
  processingFrom: string;
  processingThru: string;
}

export interface DatabaseTable {
  name: string;
  columns: string[];
  rows: Record<string, unknown>[];
}
