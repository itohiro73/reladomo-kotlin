import axios from 'axios';
import type { Category, Product, ProductPrice, DatabaseTable } from '../types';

const API_BASE_URL = '/api';

export const api = {
  categories: {
    getAll: async (): Promise<Category[]> => {
      const response = await axios.get(`${API_BASE_URL}/categories`);
      return response.data;
    },
  },

  products: {
    getAll: async (): Promise<Product[]> => {
      const response = await axios.get(`${API_BASE_URL}/products`);
      return response.data;
    },
  },

  productPrices: {
    getAll: async (): Promise<ProductPrice[]> => {
      const response = await axios.get(`${API_BASE_URL}/product-prices`);
      return response.data;
    },
  },

  database: {
    getTables: async (): Promise<DatabaseTable[]> => {
      const response = await axios.get(`${API_BASE_URL}/database/tables`);
      return response.data;
    },
  },
};
