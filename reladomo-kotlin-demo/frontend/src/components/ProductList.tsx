import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Product } from '../types';

export function ProductList() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedProductId, setExpandedProductId] = useState<number | null>(null);
  const [productHistory, setProductHistory] = useState<Record<number, Product[]>>({});
  const [loadingHistory, setLoadingHistory] = useState<number | null>(null);

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

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    return date.toLocaleString('ja-JP', {
      timeZone: 'Asia/Tokyo',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const isInfinity = (dateStr?: string) => {
    if (!dateStr) return false;
    return dateStr.startsWith('9999');
  };

  const toggleHistory = async (productId: number) => {
    if (expandedProductId === productId) {
      setExpandedProductId(null);
      return;
    }

    setExpandedProductId(productId);

    if (!productHistory[productId]) {
      setLoadingHistory(productId);
      try {
        const history = await api.products.getHistory(productId);
        setProductHistory(prev => ({ ...prev, [productId]: history }));
      } catch (err) {
        console.error('Failed to fetch product history:', err);
      } finally {
        setLoadingHistory(null);
      }
    }
  };

  if (loading) return <div className="loading">読み込み中...</div>;
  if (error) return <div className="error">エラー: {error}</div>;

  return (
    <div className="section">
      <div className="section-header">
        <div className="section-title">
          <h2>📦 商品</h2>
          <span className="badge badge-unitemporal">Uni-Temporal</span>
        </div>
        <span>{products.length} 件</span>
      </div>

      {products.length === 0 ? (
        <div className="empty-state">商品がありません</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th style={{ width: '40px' }}></th>
              <th>ID</th>
              <th>商品名</th>
              <th>カテゴリ</th>
              <th>説明</th>
              <th>処理日時 (From)</th>
              <th>処理日時 (Thru)</th>
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <>
                <tr key={product.id} style={{ cursor: 'pointer' }} onClick={() => toggleHistory(product.id)}>
                  <td>
                    <button
                      style={{
                        background: 'none',
                        border: 'none',
                        cursor: 'pointer',
                        fontSize: '1.2rem',
                        padding: '0',
                        width: '100%'
                      }}
                    >
                      {expandedProductId === product.id ? '▼' : '▶'}
                    </button>
                  </td>
                  <td>{product.id}</td>
                  <td><strong>{product.name}</strong></td>
                  <td>{product.categoryName}</td>
                  <td>{product.description || '—'}</td>
                  <td style={{ fontSize: '0.9rem' }}>{formatDateTime(product.processingFrom)}</td>
                  <td style={{ fontSize: '0.9rem' }}>
                    {isInfinity(product.processingThru) ?
                      <span style={{ color: '#10b981', fontWeight: 'bold' }}>現在有効</span> :
                      formatDateTime(product.processingThru)
                    }
                  </td>
                </tr>
                {expandedProductId === product.id && (
                  <tr>
                    <td colSpan={7} style={{ padding: '1rem', backgroundColor: '#374151' }}>
                      {loadingHistory === product.id ? (
                        <div style={{ textAlign: 'center', color: '#9ca3af' }}>履歴を読み込み中...</div>
                      ) : (
                        <div>
                          <h4 style={{ marginTop: 0, marginBottom: '0.5rem', color: '#f9fafb' }}>
                            📋 変更履歴 ({productHistory[product.id]?.length || 0} 件)
                          </h4>
                          {productHistory[product.id]?.length === 0 ? (
                            <div style={{ color: '#9ca3af' }}>履歴がありません</div>
                          ) : (
                            <table style={{ width: '100%', marginTop: '0.5rem' }}>
                              <thead>
                                <tr style={{ backgroundColor: '#4b5563' }}>
                                  <th>バージョン</th>
                                  <th>商品名</th>
                                  <th>カテゴリID</th>
                                  <th>説明</th>
                                  <th>処理日時 (From)</th>
                                  <th>処理日時 (Thru)</th>
                                  <th>状態</th>
                                </tr>
                              </thead>
                              <tbody>
                                {productHistory[product.id]?.map((version, index) => (
                                  <tr key={`${version.id}-${version.processingFrom}`}>
                                    <td>v{productHistory[product.id].length - index}</td>
                                    <td><strong>{version.name}</strong></td>
                                    <td>{version.categoryId}</td>
                                    <td>{version.description || '—'}</td>
                                    <td style={{ fontSize: '0.85rem' }}>{formatDateTime(version.processingFrom)}</td>
                                    <td style={{ fontSize: '0.85rem' }}>
                                      {isInfinity(version.processingThru) ?
                                        <span style={{ color: '#10b981' }}>∞</span> :
                                        formatDateTime(version.processingThru)
                                      }
                                    </td>
                                    <td>
                                      {isInfinity(version.processingThru) ? (
                                        <span style={{
                                          backgroundColor: '#d1fae5',
                                          color: '#065f46',
                                          padding: '0.25rem 0.5rem',
                                          borderRadius: '0.25rem',
                                          fontSize: '0.75rem',
                                          fontWeight: 'bold'
                                        }}>
                                          現在
                                        </span>
                                      ) : (
                                        <span style={{
                                          backgroundColor: '#4b5563',
                                          color: '#d1d5db',
                                          padding: '0.25rem 0.5rem',
                                          borderRadius: '0.25rem',
                                          fontSize: '0.75rem'
                                        }}>
                                          過去
                                        </span>
                                      )}
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                )}
              </>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
