import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { DatabaseTable } from '../types';

export function DatabaseViewer() {
  const [tables, setTables] = useState<DatabaseTable[]>([]);
  const [selectedTable, setSelectedTable] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchTables = async () => {
      try {
        const data = await api.database.getTables();
        setTables(data);
        if (data.length > 0) {
          setSelectedTable(data[0].name);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch database tables');
      } finally {
        setLoading(false);
      }
    };

    fetchTables();
  }, []);

  if (loading) return <div className="loading">読み込み中...</div>;
  if (error) return <div className="error">エラー: {error}</div>;

  const currentTable = tables.find(t => t.name === selectedTable);

  const isTemporalColumn = (columnName: string): boolean => {
    const temporalColumns = ['BUSINESS_FROM', 'BUSINESS_THRU', 'PROCESSING_FROM', 'PROCESSING_THRU'];
    return temporalColumns.includes(columnName);
  };

  const isAuditColumn = (columnName: string): boolean => {
    return columnName === 'UPDATED_BY';
  };

  return (
    <div className="section">
      <div className="section-header">
        <div className="section-title">
          <h2>🗄️ データベースビューア</h2>
        </div>
        <span>生のテーブルデータを確認</span>
      </div>

      {tables.length === 0 ? (
        <div className="empty-state">テーブルがありません</div>
      ) : (
        <div className="database-viewer">
          <div className="table-selector">
            {tables.map((table) => (
              <button
                key={table.name}
                className={`table-button ${selectedTable === table.name ? 'active' : ''}`}
                onClick={() => setSelectedTable(table.name)}
              >
                {table.name}
                <span style={{ marginLeft: '0.5rem', opacity: 0.7 }}>
                  ({table.rows.length})
                </span>
              </button>
            ))}
          </div>

          {currentTable && (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    {currentTable.columns.map((column) => (
                      <th
                        key={column}
                        className={isTemporalColumn(column) ? 'highlight-temporal' : isAuditColumn(column) ? 'highlight-audit' : ''}
                      >
                        {column}
                        {isTemporalColumn(column) && ' ⏰'}
                        {isAuditColumn(column) && ' 👤'}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {currentTable.rows.length === 0 ? (
                    <tr>
                      <td colSpan={currentTable.columns.length} style={{ textAlign: 'center', color: '#666' }}>
                        データがありません
                      </td>
                    </tr>
                  ) : (
                    currentTable.rows.map((row, idx) => (
                      <tr key={idx}>
                        {currentTable.columns.map((column) => (
                          <td
                            key={column}
                            className={isTemporalColumn(column) ? 'highlight-temporal' : isAuditColumn(column) ? 'highlight-audit' : ''}
                          >
                            {row[column] !== null && row[column] !== undefined
                              ? String(row[column])
                              : '—'}
                          </td>
                        ))}
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
