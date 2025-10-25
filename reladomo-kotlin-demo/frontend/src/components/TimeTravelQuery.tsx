import { useState } from 'react';
import { api } from '../api/client';
import type { ProductPrice } from '../types';
import './TimeTravelQuery.css';

export function TimeTravelQuery() {
  const [businessDate, setBusinessDate] = useState('2024-12-01T00:00:00Z');
  const [processingDate, setProcessingDate] = useState('2024-11-01T00:00:00Z');
  const [results, setResults] = useState<ProductPrice[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleQuery = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.productPrices.getAsOf(businessDate, processingDate);
      setResults(data);
    } catch (err) {
      setError('クエリの実行に失敗しました');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const presetQueries = [
    {
      name: '11月1日時点の12月1日価格（修正前）',
      businessDate: '2024-12-01T00:00:00Z',
      processingDate: '2024-11-01T00:00:00Z',
      description: '値上げ計画が1200円だった時点'
    },
    {
      name: '11月15日時点の12月1日価格（修正後）',
      businessDate: '2024-12-01T00:00:00Z',
      processingDate: '2024-11-15T00:00:00Z',
      description: '1100円に修正された後'
    },
    {
      name: '1月1日時点の1月1日価格',
      businessDate: '2024-01-01T00:00:00Z',
      processingDate: '2024-01-01T00:00:00Z',
      description: '初期価格1000円'
    }
  ];

  return (
    <div className="time-travel-query">
      <h2>⏰ タイムトラベルクエリ - バイテンポラルの真髄</h2>

      <div className="explanation">
        <p><strong>バイテンポラルデータの最大の特徴：</strong> 「いつ時点の情報で、いつの価格を知りたいか？」を指定できます</p>
        <ul>
          <li><strong>ビジネス日付</strong>: その価格がいつから有効か（未来の計画も記録可能）</li>
          <li><strong>処理日付</strong>: その情報をいつシステムに記録したか（監査履歴）</li>
        </ul>
      </div>

      <div className="preset-queries">
        <h3>プリセットクエリ</h3>
        {presetQueries.map((preset, index) => (
          <button
            key={index}
            onClick={() => {
              setBusinessDate(preset.businessDate);
              setProcessingDate(preset.processingDate);
            }}
            className="preset-button"
          >
            {preset.name}
            <span className="preset-description">{preset.description}</span>
          </button>
        ))}
      </div>

      <div className="query-form">
        <div className="form-group">
          <label>
            ビジネス日付（いつの価格？）:
            <input
              type="text"
              value={businessDate}
              onChange={(e) => setBusinessDate(e.target.value)}
              placeholder="2024-12-01T00:00:00Z"
            />
          </label>
        </div>

        <div className="form-group">
          <label>
            処理日付（いつ時点の情報？）:
            <input
              type="text"
              value={processingDate}
              onChange={(e) => setProcessingDate(e.target.value)}
              placeholder="2024-11-01T00:00:00Z"
            />
          </label>
        </div>

        <button onClick={handleQuery} disabled={loading} className="query-button">
          {loading ? 'クエリ実行中...' : 'タイムトラベルクエリを実行'}
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {results.length > 0 && (
        <div className="results">
          <h3>
            {new Date(businessDate).toLocaleDateString('ja-JP')} の価格
            （{new Date(processingDate).toLocaleDateString('ja-JP')} 時点の情報）
          </h3>
          <table>
            <thead>
              <tr>
                <th>商品名</th>
                <th>価格</th>
                <th>ビジネス有効期間</th>
                <th>処理記録期間</th>
              </tr>
            </thead>
            <tbody>
              {results.map((price) => (
                <tr key={price.id}>
                  <td>{price.productName}</td>
                  <td className="price">¥{price.price.toLocaleString()}</td>
                  <td className="date-range">
                    {new Date(price.businessFrom).toLocaleDateString('ja-JP')} 〜<br/>
                    {new Date(price.businessThru).toLocaleDateString('ja-JP')}
                  </td>
                  <td className="date-range">
                    {new Date(price.processingFrom).toLocaleDateString('ja-JP')} 〜<br/>
                    {new Date(price.processingThru).toLocaleDateString('ja-JP')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {results.length === 0 && !loading && !error && (
        <div className="no-results">
          プリセットクエリを選択するか、日付を指定してクエリを実行してください
        </div>
      )}
    </div>
  );
}
