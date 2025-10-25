import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Product } from '../types';

export function ProductList() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const data = await api.products.getAll();
        setProducts(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch products');
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  if (loading) return <div className="loading">読み込み中...</div>;
  if (error) return <div className="error">エラー: {error}</div>;

  return (
    <div className="section">
      <div className="section-header">
        <div className="section-title">
          <h2>📦 商品</h2>
          <span className="badge badge-non-temporal">Non-Temporal</span>
        </div>
        <span>{products.length} 件</span>
      </div>

      {products.length === 0 ? (
        <div className="empty-state">商品がありません</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>商品名</th>
              <th>カテゴリ</th>
              <th>説明</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.id}>
                <td>{product.id}</td>
                <td><strong>{product.name}</strong></td>
                <td>{product.categoryName}</td>
                <td>{product.description || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
