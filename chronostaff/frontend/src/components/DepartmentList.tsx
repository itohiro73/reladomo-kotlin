import { useState } from 'react';
import { useDepartments } from '../hooks/useAPI';
import { useCompany } from '../contexts/CompanyContext';
import { formatDate } from '../utils/date';
import * as api from '../api/client';
import { mutate } from 'swr';

export default function DepartmentList() {
  const { selectedCompanyId } = useCompany();
  const { data: departments, error, isLoading } = useDepartments(selectedCompanyId);

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingDepartment, setEditingDepartment] = useState<number | null>(null);
  const [formData, setFormData] = useState({ name: '', description: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refreshDepartments = () => {
    if (selectedCompanyId) {
      mutate(`departments?companyId=${selectedCompanyId}`);
    }
  };

  const handleAdd = () => {
    setFormData({ name: '', description: '' });
    setEditingDepartment(null);
    setIsAddModalOpen(true);
    setFormError(null);
  };

  const handleEdit = (dept: Department) => {
    setFormData({ name: dept.name, description: dept.description });
    setEditingDepartment(dept.id);
    setIsAddModalOpen(true);
    setFormError(null);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('本当にこの部署を削除しますか？\n（所属している従業員がいる場合は削除できません）')) {
      return;
    }

    try {
      await api.deleteDepartment(id);
      refreshDepartments();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '削除に失敗しました';
      alert(errorMessage);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setIsSubmitting(true);

    try {
      if (editingDepartment) {
        await api.updateDepartment(editingDepartment, formData);
      } else {
        if (!selectedCompanyId) {
          throw new Error('会社が選択されていません');
        }
        await api.createDepartment(formData, selectedCompanyId);
      }
      refreshDepartments();
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
        Error loading departments: {error.message}
      </div>
    );
  }

  if (!departments) return null;

  // Build department hierarchy
  const rootDepts = departments.filter(d => !d.parentDepartmentId);
  const childDepts = departments.filter(d => d.parentDepartmentId);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold text-gray-900">部署一覧</h2>
        <div className="flex items-center gap-3">
          <span className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full font-medium text-sm">
            {departments.length}部署
          </span>
          <button
            onClick={handleAdd}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
          >
            + 部署を追加
          </button>
        </div>
      </div>

      <div className="space-y-3">
        {rootDepts.map(dept => (
          <DepartmentNode
            key={dept.id}
            dept={dept}
            children={childDepts}
            allDepts={departments}
            onEdit={handleEdit}
            onDelete={handleDelete}
          />
        ))}
      </div>

      {/* Add/Edit Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-bold mb-4">
              {editingDepartment ? '部署を編集' : '部署を追加'}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  部署名 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="開発部"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  説明
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  placeholder="システム開発を担当"
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

interface Department {
  id: number;
  name: string;
  description: string;
  parentDepartmentId?: number;
  processingFrom: string;
  processingThru: string;
}

function DepartmentNode({
  dept,
  children,
  allDepts,
  level = 0,
  onEdit,
  onDelete
}: {
  dept: Department;
  children: Department[];
  allDepts: Department[];
  level?: number;
  onEdit: (dept: Department) => void;
  onDelete: (id: number) => void;
}) {
  const myChildren = children.filter(c => c.parentDepartmentId === dept.id);
  const hasChildren = myChildren.length > 0;

  const levelColors = [
    'from-primary-50 to-primary-100 border-primary-200',
    'from-blue-50 to-blue-100 border-blue-200',
    'from-indigo-50 to-indigo-100 border-indigo-200',
  ];
  const colorClass = levelColors[Math.min(level, levelColors.length - 1)];

  const levelIcons = ['🏢', '📁', '📂'];
  const icon = levelIcons[Math.min(level, levelIcons.length - 1)];

  return (
    <div className={level > 0 ? 'ml-6 pl-4 border-l-2 border-gray-300' : ''}>
      <div className={`bg-gradient-to-r ${colorClass} border rounded-lg p-4 shadow-sm hover:shadow-md transition-all duration-200`}>
        <div className="flex items-start gap-3">
          <span className="text-2xl">{icon}</span>
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-2">
              <h3 className="text-lg font-semibold text-gray-900">{dept.name}</h3>
              {hasChildren && (
                <span className="text-xs bg-white px-2 py-1 rounded-full text-gray-600 font-medium">
                  {myChildren.length}部署
                </span>
              )}
            </div>
            <p className="text-sm text-gray-700 mb-3">{dept.description}</p>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1 text-xs text-gray-500">
                <span>⏱️</span>
                <span className="font-mono">
                  {formatDate(dept.processingFrom)} ~ {formatDate(dept.processingThru)}
                </span>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => onEdit(dept)}
                  className="px-3 py-1 bg-blue-500 text-white text-xs rounded hover:bg-blue-600 transition-colors"
                >
                  編集
                </button>
                <button
                  onClick={() => onDelete(dept.id)}
                  className="px-3 py-1 bg-red-500 text-white text-xs rounded hover:bg-red-600 transition-colors"
                >
                  削除
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {hasChildren && (
        <div className="mt-3 space-y-3">
          {myChildren.map(child => (
            <DepartmentNode
              key={child.id}
              dept={child}
              children={children}
              allDepts={allDepts}
              level={level + 1}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
}
