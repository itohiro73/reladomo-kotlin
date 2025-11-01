import { useState } from 'react';
import { usePositions } from '../hooks/useAPI';
import { useCompany } from '../contexts/CompanyContext';
import * as api from '../api/client';
import { mutate } from 'swr';

export default function PositionList() {
  const { selectedCompanyId } = useCompany();
  const { data: positions, error, isLoading } = usePositions(selectedCompanyId);

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingPosition, setEditingPosition] = useState<number | null>(null);
  const [formData, setFormData] = useState({ name: '', level: 1, description: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refreshPositions = () => {
    if (selectedCompanyId) {
      mutate(`positions?companyId=${selectedCompanyId}`);
    }
  };

  const handleAdd = () => {
    setFormData({ name: '', level: 1, description: '' });
    setEditingPosition(null);
    setIsAddModalOpen(true);
    setFormError(null);
  };

  const handleEdit = (position: any) => {
    setFormData({
      name: position.name,
      level: position.level,
      description: position.description || ''
    });
    setEditingPosition(position.id);
    setIsAddModalOpen(true);
    setFormError(null);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('本当にこの役職を削除しますか？\n（この役職に配属されている従業員がいる場合は削除できません）')) {
      return;
    }

    try {
      await api.deletePosition(id);
      refreshPositions();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '削除に失敗しました';
      alert(errorMessage);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setIsSubmitting(true);

    if (!selectedCompanyId) {
      setFormError('会社が選択されていません');
      setIsSubmitting(false);
      return;
    }

    try {
      const data = {
        name: formData.name,
        level: formData.level,
        description: formData.description || undefined
      };

      if (editingPosition) {
        await api.updatePosition(editingPosition, data);
      } else {
        await api.createPosition(data, selectedCompanyId);
      }
      refreshPositions();
      setIsAddModalOpen(false);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : '保存に失敗しました');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        Error loading positions: {error.message}
      </div>
    );
  }

  if (!positions) return null;

  // Sort positions by level (descending) and then by name
  const sortedPositions = [...positions].sort((a, b) => {
    if (a.level !== b.level) {
      return b.level - a.level;
    }
    return a.name.localeCompare(b.name);
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold text-gray-900">役職一覧</h2>
        <div className="flex items-center gap-3">
          <span className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full font-medium text-sm">
            {positions.length}役職
          </span>
          <button
            onClick={handleAdd}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
          >
            + 役職を追加
          </button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {sortedPositions.map((position) => (
          <div
            key={position.id}
            className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow border-l-4"
            style={{ borderLeftColor: getLevelColor(position.level) }}
          >
            <div className="flex items-start justify-between mb-3">
              <div>
                <h3 className="text-xl font-semibold text-gray-900">{position.name}</h3>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-sm font-medium px-2 py-1 rounded"
                        style={{ backgroundColor: getLevelColor(position.level), color: 'white' }}>
                    Level {position.level}
                  </span>
                </div>
              </div>
            </div>
            {position.description && (
              <p className="text-gray-600 text-sm mb-4">{position.description}</p>
            )}
            <div className="flex gap-2">
              <button
                onClick={() => handleEdit(position)}
                className="flex-1 px-3 py-1 bg-blue-500 text-white text-sm rounded hover:bg-blue-600 transition-colors"
              >
                編集
              </button>
              <button
                onClick={() => handleDelete(position.id)}
                className="flex-1 px-3 py-1 bg-red-500 text-white text-sm rounded hover:bg-red-600 transition-colors"
              >
                削除
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Add/Edit Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-bold mb-4">
              {editingPosition ? '役職を編集' : '役職を追加'}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  役職名 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="部長"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  レベル (1-10) <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  required
                  min="1"
                  max="10"
                  value={formData.level}
                  onChange={(e) => setFormData({ ...formData, level: parseInt(e.target.value) })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">
                  1=一般社員、10=最高責任者
                </p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  説明
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="部門責任者"
                  rows={3}
                />
              </div>
              {formError && (
                <div className="bg-red-50 border border-red-200 rounded p-3 text-red-700 text-sm">
                  {formError}
                </div>
              )}
              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-1 bg-primary-600 text-white py-2 px-4 rounded-lg hover:bg-primary-700 disabled:bg-gray-400 transition-colors"
                >
                  {isSubmitting ? '保存中...' : '保存'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsAddModalOpen(false)}
                  className="flex-1 bg-gray-200 text-gray-700 py-2 px-4 rounded-lg hover:bg-gray-300 transition-colors"
                >
                  キャンセル
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// Helper function to get color based on level
function getLevelColor(level: number): string {
  if (level >= 9) return '#dc2626'; // red-600 - Executive
  if (level >= 7) return '#ea580c'; // orange-600 - Senior Management
  if (level >= 5) return '#ca8a04'; // yellow-600 - Middle Management
  if (level >= 3) return '#16a34a'; // green-600 - Team Lead
  return '#0284c7'; // sky-600 - Individual Contributor
}
