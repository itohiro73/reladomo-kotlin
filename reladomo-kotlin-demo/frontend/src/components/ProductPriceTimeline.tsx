import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ProductPrice } from '../types';

export function ProductPriceTimeline() {
  const [prices, setPrices] = useState<ProductPrice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPrices = async () => {
      try {
        const data = await api.productPrices.getAll();
        // Sort by processing date (most recent first)
        const sorted = data.sort((a, b) =>
          new Date(b.processingFrom).getTime() - new Date(a.processingFrom).getTime()
        );
        setPrices(sorted);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch prices');
      } finally {
        setLoading(false);
      }
    };

    fetchPrices();
  }, []);

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    if (date.getFullYear() === 9999) return '無期限';
    return date.toLocaleString('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatPrice = (price: number): string => {
    return `¥${price.toLocaleString('ja-JP')}`;
  };

  if (loading) return <div className="loading">読み込み中...</div>;
  if (error) return <div className="error">エラー: {error}</div>;

  return (
    <div className="section">
      <div className="section-header">
        <div className="section-title">
          <h2>💰 商品価格履歴</h2>
          <span className="badge badge-bitemporal">Bitemporal</span>
        </div>
        <span>{prices.length} 件の価格レコード</span>
      </div>

      {prices.length === 0 ? (
        <div className="empty-state">価格履歴がありません</div>
      ) : (
        <div className="timeline">
          {prices.map((price) => (
            <div key={`${price.id}-${price.businessFrom}-${price.processingFrom}`} className="timeline-item">
              <div className="timeline-item-header">
                <div>
                  <div className="timeline-item-title">{price.productName}</div>
                  <div style={{ fontSize: '0.85rem', color: '#888' }}>ID: {price.id}</div>
                </div>
                <div className="timeline-item-price">{formatPrice(price.price)}</div>
              </div>

              <div className="timeline-dates">
                <div className="date-group">
                  <div className="date-label">📅 ビジネス日付 (いつから有効か)</div>
                  <div className="date-value">
                    開始: {formatDate(price.businessFrom)}
                  </div>
                  <div className="date-value">
                    終了: {formatDate(price.businessThru)}
                  </div>
                </div>

                <div className="date-group">
                  <div className="date-label">🔄 処理日付 (いつ記録されたか)</div>
                  <div className="date-value">
                    開始: {formatDate(price.processingFrom)}
                  </div>
                  <div className="date-value">
                    終了: {formatDate(price.processingThru)}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <div style={{ marginTop: '2rem', padding: '1rem', background: '#2a2a2a', borderRadius: '4px' }}>
        <h3 style={{ marginBottom: '0.5rem', fontSize: '1rem' }}>💡 バイテンポラルデータとは</h3>
        <p style={{ color: '#aaa', fontSize: '0.9rem', lineHeight: '1.6' }}>
          <strong>ビジネス日付</strong>: その価格がいつから有効になるか（未来の計画も記録可能）<br />
          <strong>処理日付</strong>: その情報をいつシステムに記録したか（監査履歴）<br />
          <br />
          この2つの時間軸により、過去の計画や修正履歴を完全に追跡できます。
        </p>
      </div>
    </div>
  );
}
