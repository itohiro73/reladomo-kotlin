import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Category } from '../types';

export function CategoryList() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await api.categories.getAll();
        setCategories(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch categories');
      } finally {
        setLoading(false);
      }
    };

    fetchCategories();
  }, []);

  if (loading) return <div className="loading">読み込み中...</div>;
  if (error) return <div className="error">エラー: {error}</div>;

  return (
    <div className="section">
      <div className="section-header">
        <div className="section-title">
          <h2>📁 カテゴリ</h2>
          <span className="badge badge-non-temporal">Non-Temporal</span>
        </div>
        <span>{categories.length} 件</span>
      </div>

      {categories.length === 0 ? (
        <div className="empty-state">カテゴリがありません</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名前</th>
              <th>説明</th>
            </tr>
          </thead>
          <tbody>
            {categories.map((category) => (
              <tr key={category.id}>
                <td>{category.id}</td>
                <td><strong>{category.name}</strong></td>
                <td>{category.description || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
