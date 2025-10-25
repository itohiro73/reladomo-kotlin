import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ProductPrice, Product } from '../types';

interface ProductPriceWithVersion extends ProductPrice {
  productVersion?: Product;
}

interface ProductTimeline {
  productId: number;
  productName: string;
  versions: ProductPriceWithVersion[];
}

export function ProductPriceTimeline() {
  const [timelines, setTimelines] = useState<ProductTimeline[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedProductId, setExpandedProductId] = useState<number | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const prices = await api.productPrices.getAll();
        const products = await api.products.getAll();

        // Group prices by productId
        const pricesByProduct = new Map<number, ProductPrice[]>();
        prices.forEach(price => {
          if (!pricesByProduct.has(price.productId)) {
            pricesByProduct.set(price.productId, []);
          }
          pricesByProduct.get(price.productId)!.push(price);
        });

        // For each product, fetch its history and correlate with prices
        const timelinesData: ProductTimeline[] = [];

        for (const [productId, productPrices] of pricesByProduct.entries()) {
          const productHistory = await api.products.getHistory(productId);

          // Correlate prices with product versions based on temporal overlap
          const correlatedPrices: ProductPriceWithVersion[] = productPrices.map(price => {
            // Find product version that was valid during this price's validity period
            // Check overlap in processing time dimension
            const matchingVersion = productHistory.find(version => {
              if (!version.processingFrom) return false;

              const priceProcessingFrom = new Date(price.processingFrom).getTime();
              const priceProcessingThru = new Date(price.processingThru).getTime();
              const versionProcessingFrom = new Date(version.processingFrom).getTime();

              // Handle infinity dates (year 9999)
              const INFINITY_THRESHOLD = new Date('9999-01-01').getTime();
              const versionProcessingThru = version.processingThru && new Date(version.processingThru).getFullYear() < 9999
                ? new Date(version.processingThru).getTime()
                : INFINITY_THRESHOLD;

              // Check if processing time ranges overlap
              // Overlap exists if: priceFrom < versionThru AND priceThru > versionFrom
              return (
                priceProcessingFrom < versionProcessingThru &&
                priceProcessingThru > versionProcessingFrom
              );
            });

            return {
              ...price,
              productVersion: matchingVersion
            };
          });

          // Sort by processing date (most recent first)
          correlatedPrices.sort((a, b) =>
            new Date(b.processingFrom).getTime() - new Date(a.processingFrom).getTime()
          );

          const productName = productPrices[0]?.productName || `Product ${productId}`;
          timelinesData.push({
            productId,
            productName,
            versions: correlatedPrices
          });
        }

        // Sort timelines by productId
        timelinesData.sort((a, b) => a.productId - b.productId);

        setTimelines(timelinesData);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch data');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    if (date.getFullYear() === 9999) return '∞';
    return date.toLocaleString('ja-JP', {
      timeZone: 'Asia/Tokyo',
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

  const toggleTimeline = (productId: number) => {
    setExpandedProductId(expandedProductId === productId ? null : productId);
  };

  if (loading) return <div className="loading">読み込み中...</div>;
  if (error) return <div className="error">エラー: {error}</div>;

  const totalPrices = timelines.reduce((sum, t) => sum + t.versions.length, 0);

  return (
    <div className="section">
      <div className="section-header">
        <div className="section-title">
          <h2>💰 商品価格履歴</h2>
          <span className="badge badge-bitemporal">Bitemporal</span>
        </div>
        <span>{totalPrices} 件の価格レコード ({timelines.length} 商品)</span>
      </div>

      {timelines.length === 0 ? (
        <div className="empty-state">価格履歴がありません</div>
      ) : (
        <div>
          {timelines.map((timeline) => (
            <div key={timeline.productId} style={{ marginBottom: '1.5rem' }}>
              <div
                style={{
                  padding: '0.75rem 1rem',
                  backgroundColor: '#2a2a2a',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}
                onClick={() => toggleTimeline(timeline.productId)}
              >
                <div>
                  <span style={{ fontSize: '1.2rem', marginRight: '0.5rem' }}>
                    {expandedProductId === timeline.productId ? '▼' : '▶'}
                  </span>
                  <strong>{timeline.productName}</strong>
                  <span style={{ marginLeft: '0.5rem', color: '#888', fontSize: '0.9rem' }}>
                    (ID: {timeline.productId})
                  </span>
                </div>
                <span style={{ color: '#888' }}>
                  {timeline.versions.length} 件の価格履歴
                </span>
              </div>

              {expandedProductId === timeline.productId && (
                <div className="timeline" style={{ marginTop: '1rem' }}>
                  {timeline.versions.map((price) => (
                    <div
                      key={`${price.id}-${price.businessFrom}-${price.processingFrom}`}
                      className="timeline-item"
                      style={{ borderLeft: '3px solid #4f46e5' }}
                    >
                      <div className="timeline-item-header">
                        <div>
                          <div className="timeline-item-title">
                            {price.productVersion ? (
                              <>
                                <strong>{price.productVersion.name}</strong>
                                {price.productVersion.description && (
                                  <div style={{ fontSize: '0.85rem', color: '#aaa', marginTop: '0.25rem' }}>
                                    {price.productVersion.description}
                                  </div>
                                )}
                              </>
                            ) : (
                              <span style={{ color: '#888' }}>商品情報不明</span>
                            )}
                          </div>
                          <div style={{ fontSize: '0.85rem', color: '#888', marginTop: '0.25rem' }}>
                            価格ID: {price.id}
                            {price.productVersion && price.productVersion.processingFrom && (
                              <span style={{ marginLeft: '1rem' }}>
                                商品バージョン: {formatDate(price.productVersion.processingFrom)}
                              </span>
                            )}
                          </div>
                        </div>
                        <div className="timeline-item-price">{formatPrice(price.price)}</div>
                      </div>

                      <div className="timeline-dates">
                        <div className="date-group">
                          <div className="date-label">📅 ビジネス日付 (価格の有効期間)</div>
                          <div className="date-value">
                            開始: {formatDate(price.businessFrom)}
                          </div>
                          <div className="date-value">
                            終了: {formatDate(price.businessThru)}
                          </div>
                        </div>

                        <div className="date-group">
                          <div className="date-label">🔄 処理日付 (記録期間)</div>
                          <div className="date-value">
                            開始: {formatDate(price.processingFrom)}
                          </div>
                          <div className="date-value">
                            終了: {formatDate(price.processingThru)}
                          </div>
                        </div>
                      </div>

                      {price.productVersion && (
                        <div style={{
                          marginTop: '0.75rem',
                          padding: '0.5rem',
                          backgroundColor: '#1a1a1a',
                          borderRadius: '4px',
                          fontSize: '0.85rem'
                        }}>
                          <strong style={{ color: '#4f46e5' }}>📦 対応商品バージョン:</strong>
                          <div style={{ marginTop: '0.25rem', color: '#aaa' }}>
                            カテゴリID: {price.productVersion.categoryId} |
                            処理期間: {formatDate(price.productVersion.processingFrom || '')} 〜 {formatDate(price.productVersion.processingThru || '')}
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <div style={{ marginTop: '2rem', padding: '1rem', background: '#2a2a2a', borderRadius: '4px' }}>
        <h3 style={{ marginBottom: '0.5rem', fontSize: '1rem' }}>💡 バイテンポラルデータとは</h3>
        <p style={{ color: '#aaa', fontSize: '0.9rem', lineHeight: '1.6' }}>
          <strong>ビジネス日付</strong>: その価格がいつから有効になるか（未来の計画も記録可能）<br />
          <strong>処理日付</strong>: その情報をいつシステムに記録したか（監査履歴）<br />
          <strong>商品バージョン</strong>: 価格が記録された時点での商品情報を表示<br />
          <br />
          この2つの時間軸により、過去の計画や修正履歴を完全に追跡でき、<br />
          価格と商品バージョンの対応関係も正確に把握できます。
        </p>
      </div>
    </div>
  );
}
