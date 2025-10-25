import { useState, useEffect } from 'react';
import { api } from '../api/client';
import type { Product } from '../types';
import './PriceUpdateForm.css';

export function PriceUpdateForm() {
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<number>(1);
  const [newPrice, setNewPrice] = useState<string>('1100');
  const [businessDate, setBusinessDate] = useState<string>('2025-01-01T00:00:00Z');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const data = await api.products.getAll();
        setProducts(data);
      } catch (err) {
        console.error('Failed to fetch products', err);
      }
    };
    fetchProducts();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await api.productPrices.updatePrice({
        productId: selectedProductId,
        price: parseFloat(newPrice),
        businessDate: businessDate,
      });
      setSuccess(`価格を更新しました！新しい価格: ¥${parseFloat(newPrice).toLocaleString()}`);
    } catch (err) {
      setError('価格の更新に失敗しました');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const presetScenarios = [
    {
      name: '2025年の値上げを計画',
      productId: 1,
      price: '1200',
      businessDate: '2025-01-01T00:00:00Z',
      description: '来年1月から1200円に値上げする計画を記録'
    },
    {
      name: '緊急値下げ（即日適用）',
      productId: 1,
      price: '950',
      businessDate: new Date().toISOString(),
      description: '今日から950円に値下げ'
    },
  ];

  return (
    <div className="price-update-form">
      <h2>💰 価格変更フォーム - バイテンポラルの力</h2>

      <div className="explanation">
        <p><strong>未来の価格計画も記録可能！</strong></p>
        <p>
          バイテンポラルデータでは、「いつから有効か」（ビジネス日付）を指定して価格を記録できます。
          未来の価格計画を今記録し、後で変更することも可能です。
        </p>
      </div>

      <div className="preset-scenarios">
        <h3>プリセットシナリオ</h3>
        {presetScenarios.map((scenario, index) => (
          <button
            key={index}
            onClick={() => {
              setSelectedProductId(scenario.productId);
              setNewPrice(scenario.price);
              setBusinessDate(scenario.businessDate);
            }}
            className="scenario-button"
          >
            {scenario.name}
            <span className="scenario-description">{scenario.description}</span>
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="update-form">
        <div className="form-group">
          <label>
            商品:
            <select
              value={selectedProductId}
              onChange={(e) => setSelectedProductId(parseInt(e.target.value))}
            >
              {products.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="form-group">
          <label>
            新しい価格 (円):
            <input
              type="number"
              value={newPrice}
              onChange={(e) => setNewPrice(e.target.value)}
              min="0"
              step="1"
            />
          </label>
        </div>

        <div className="form-group">
          <label>
            ビジネス日付（いつから有効？）:
            <input
              type="text"
              value={businessDate}
              onChange={(e) => setBusinessDate(e.target.value)}
              placeholder="2025-01-01T00:00:00Z"
            />
          </label>
          <small>未来の日付を指定すると、その日から有効な価格計画として記録されます</small>
        </div>

        <button type="submit" disabled={loading} className="submit-button">
          {loading ? '更新中...' : '価格を更新'}
        </button>
      </form>

      {error && <div className="error">{error}</div>}
      {success && (
        <div className="success">
          {success}
          <p className="success-hint">
            データベースビューアーやタイムラインで、履歴が記録されているのを確認できます！
          </p>
        </div>
      )}
    </div>
  );
}
