import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ProductPrice } from '../types';
import './BitemporalTimeline2D.css';

interface TimelinePoint {
  id: number;
  productName: string;
  price: number;
  businessFrom: Date;
  businessThru: Date;
  processingFrom: Date;
  processingThru: Date;
  updatedBy: string;
}

export function BitemporalTimeline2D() {
  const [prices, setPrices] = useState<TimelinePoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPoint, setSelectedPoint] = useState<TimelinePoint | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<string | null>(null);

  useEffect(() => {
    const fetchPrices = async () => {
      try {
        const data = await api.productPrices.getAll();
        const points = data.map(p => ({
          id: p.id,
          productName: p.productName,
          price: p.price,
          businessFrom: new Date(p.businessFrom),
          businessThru: new Date(p.businessThru),
          processingFrom: new Date(p.processingFrom),
          processingThru: new Date(p.processingThru),
          updatedBy: p.updatedBy || 'unknown'
        }));
        setPrices(points);

        // Set first product as default selection
        if (points.length > 0 && !selectedProduct) {
          setSelectedProduct(points[0].productName);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch prices');
      } finally {
        setLoading(false);
      }
    };

    fetchPrices();
  }, [selectedProduct]);

  if (loading) return <div className="loading">読み込み中...</div>;
  if (error) return <div className="error">エラー: {error}</div>;

  // Get unique products
  const uniqueProducts = Array.from(new Set(prices.map(p => p.productName)));

  // Filter prices by selected product
  const filteredPrices = selectedProduct
    ? prices.filter(p => p.productName === selectedProduct)
    : prices;

  // Calculate time bounds
  // For 9999 records, exclude them from range calculation
  // Also exclude records with future processingThru (temporary test data)
  const now = Date.now();

  // Filter out records with processingThru in the future (excluding 9999)
  const validPrices = filteredPrices.filter(p =>
    p.processingThru.getFullYear() === 9999 || p.processingThru.getTime() <= now
  );

  const actualBusinessTimes = validPrices
    .filter(p => p.businessThru.getFullYear() !== 9999)
    .flatMap(p => [p.businessFrom.getTime(), p.businessThru.getTime()]);
  const actualProcessingTimes = validPrices
    .filter(p => p.processingThru.getFullYear() !== 9999)
    .flatMap(p => [p.processingFrom.getTime(), p.processingThru.getTime()]);

  // Include all from times
  const allBusinessFromTimes = validPrices.map(p => p.businessFrom.getTime());
  const allProcessingFromTimes = validPrices.map(p => p.processingFrom.getTime());

  const minBusinessTime = Math.min(...allBusinessFromTimes);
  const minProcessingTime = Math.min(...allProcessingFromTimes);

  // Use actual THRU values (excluding 9999) with small margin for better visualization
  const maxBusinessTime = actualBusinessTimes.length > 0
    ? Math.max(...actualBusinessTimes) + (90 * 24 * 60 * 60 * 1000)  // +90 days
    : Math.max(...allBusinessFromTimes) + (180 * 24 * 60 * 60 * 1000); // +180 days fallback
  const maxProcessingTime = actualProcessingTimes.length > 0
    ? Math.max(...actualProcessingTimes) + (90 * 24 * 60 * 60 * 1000)  // +90 days
    : Math.max(...allProcessingFromTimes) + (180 * 24 * 60 * 60 * 1000); // +180 days fallback

  const businessRange = maxBusinessTime - minBusinessTime;
  const processingRange = maxProcessingTime - minProcessingTime;

  // Position calculation helper
  const getPosition = (point: TimelinePoint) => {
    // Calculate positions for bitemporal rectangle
    // X-axis (Business Time): left edge starts at businessFrom
    const left = ((point.businessFrom.getTime() - minBusinessTime) / businessRange) * 100;

    // Y-axis (Processing Time): top edge is at processingThru (newer time = higher on screen)
    const processingThruTime = point.processingThru.getFullYear() === 9999
      ? maxProcessingTime
      : point.processingThru.getTime();
    const top = ((maxProcessingTime - processingThruTime) / processingRange) * 100;

    // Width: businessFrom to businessThru
    const businessThruTime = point.businessThru.getFullYear() === 9999
      ? maxBusinessTime
      : point.businessThru.getTime();
    const width = ((businessThruTime - point.businessFrom.getTime()) / businessRange) * 100;

    // Height: processingFrom to processingThru
    const height = ((processingThruTime - point.processingFrom.getTime()) / processingRange) * 100;

    return { left, top, width, height };
  };

  const formatDate = (date: Date): string => {
    if (date.getFullYear() === 9999) return '∞';
    return date.toLocaleString('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatPrice = (price: number): string => {
    return `¥${price.toLocaleString('ja-JP')}`;
  };

  const formatAxisDate = (timestamp: number): string => {
    const date = new Date(timestamp);
    if (date.getFullYear() === 9999) return '∞';
    return date.toLocaleDateString('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  };

  const formatAxisTime = (timestamp: number): string => {
    const date = new Date(timestamp);
    if (date.getFullYear() === 9999) return '';
    return date.toLocaleTimeString('ja-JP', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="section">
      <div className="section-header">
        <div className="section-title">
          <h2>📊 2次元バイテンポラルタイムライン</h2>
          <span className="badge badge-bitemporal">Visual 2D</span>
        </div>
        <span>{validPrices.length} 件のバージョン</span>
      </div>

      <div className="timeline-2d-explanation">
        <p>
          <strong>横軸（ビジネス時間）</strong>: その価格がいつから有効か（現実世界の時間）<br/>
          <strong>縦軸（処理時間）</strong>: その情報をいつシステムに記録したか（システム記録時刻）
        </p>
        <p style={{ marginTop: '0.5rem', color: '#10b981' }}>
          💡 同じビジネス期間に複数のバージョンが存在することで、過去の修正や計画変更の履歴を完全に保持できます
        </p>
      </div>

      <div className="timeline-2d-legend">
        <div className="legend-item">
          <div className="legend-color active"></div>
          <span>現在有効なバージョン</span>
        </div>
        <div className="legend-item">
          <div className="legend-color historical"></div>
          <span>過去のバージョン（上書きされた）</span>
        </div>
      </div>

      {/* Product Selector */}
      <div className="product-selector">
        <label style={{ fontWeight: 600, marginRight: '1rem' }}>商品を選択:</label>
        {uniqueProducts.map(productName => (
          <button
            key={productName}
            className={`product-button ${selectedProduct === productName ? 'active' : ''}`}
            onClick={() => setSelectedProduct(productName)}
          >
            {productName}
          </button>
        ))}
      </div>

      <div className="timeline-2d-container">
        <div className="timeline-2d-axis-label timeline-2d-y-axis-label">
          処理時間（いつ記録したか）⬆️
        </div>
        <div className="timeline-2d-axis-label timeline-2d-x-axis-label">
          ビジネス時間（いつから有効か）➡️
        </div>

        {/* Y-axis (Processing Time) labels */}
        <div className="axis-labels y-axis-labels">
          <div className="axis-tick top">
            <span className="tick-date">{formatAxisDate(maxProcessingTime)}</span>
            <span className="tick-time">{formatAxisTime(maxProcessingTime)}</span>
          </div>
          <div className="axis-tick middle">
            <span className="tick-date">{formatAxisDate((maxProcessingTime + minProcessingTime) / 2)}</span>
            <span className="tick-time">{formatAxisTime((maxProcessingTime + minProcessingTime) / 2)}</span>
          </div>
          <div className="axis-tick bottom">
            <span className="tick-date">{formatAxisDate(minProcessingTime)}</span>
            <span className="tick-time">{formatAxisTime(minProcessingTime)}</span>
          </div>
        </div>

        {/* X-axis (Business Time) labels */}
        <div className="axis-labels x-axis-labels">
          <div className="axis-tick left">
            <span className="tick-date">{formatAxisDate(minBusinessTime)}</span>
            <span className="tick-time">{formatAxisTime(minBusinessTime)}</span>
          </div>
          <div className="axis-tick middle">
            <span className="tick-date">{formatAxisDate((maxBusinessTime + minBusinessTime) / 2)}</span>
            <span className="tick-time">{formatAxisTime((maxBusinessTime + minBusinessTime) / 2)}</span>
          </div>
          <div className="axis-tick right">
            <span className="tick-date">{formatAxisDate(maxBusinessTime)}</span>
            <span className="tick-time">{formatAxisTime(maxBusinessTime)}</span>
          </div>
        </div>

        <div className="timeline-2d-grid">
          {validPrices.map((point, idx) => {
            const { left, top, width, height } = getPosition(point);
            const isActive = point.processingThru.getFullYear() === 9999;

            return (
              <div
                key={`${point.id}-${idx}`}
                className={`timeline-2d-point ${isActive ? 'active' : 'historical'} ${selectedPoint === point ? 'selected' : ''}`}
                style={{
                  left: `${left}%`,
                  top: `${top}%`,
                  width: `${Math.max(width, 2)}%`,
                  height: `${Math.max(height, 2)}%`
                }}
                onClick={() => setSelectedPoint(point)}
                title={`${point.productName}: ${formatPrice(point.price)}`}
              >
                <div className="timeline-2d-point-label">
                  {formatPrice(point.price)}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {selectedPoint && (
        <div className="timeline-2d-detail">
          <h3>{selectedPoint.productName}</h3>
          <div className="detail-grid">
            <div className="detail-section">
              <h4>💰 価格情報</h4>
              <p><strong>価格:</strong> {formatPrice(selectedPoint.price)}</p>
              <p><strong>更新者:</strong> {selectedPoint.updatedBy}</p>
            </div>
            <div className="detail-section">
              <h4>📅 ビジネス時間</h4>
              <p><strong>開始:</strong> {formatDate(selectedPoint.businessFrom)}</p>
              <p><strong>終了:</strong> {formatDate(selectedPoint.businessThru)}</p>
            </div>
            <div className="detail-section">
              <h4>🔄 処理時間</h4>
              <p><strong>記録日時:</strong> {formatDate(selectedPoint.processingFrom)}</p>
              <p><strong>終了日時:</strong> {formatDate(selectedPoint.processingThru)}</p>
            </div>
          </div>
          <button
            className="close-button"
            onClick={() => setSelectedPoint(null)}
          >
            閉じる
          </button>
        </div>
      )}
    </div>
  );
}
