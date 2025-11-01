import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setupOrganization } from '../api/client';
import { useCompany } from '../contexts/CompanyContext';
import type { PositionCreateDto, DepartmentCreateDto, SetupRequestDto } from '../types';

export default function InitialSetupWizard() {
  const navigate = useNavigate();
  const { setSelectedCompanyId, addCompany } = useCompany();
  const [companyName, setCompanyName] = useState('');
  // Default: 3 months ago (for demo historical data)
  const [effectiveDate, setEffectiveDate] = useState(() => {
    const threeMonthsAgo = new Date();
    threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
    threeMonthsAgo.setDate(1);
    return threeMonthsAgo.toISOString().split('T')[0];
  });
  const [positions, setPositions] = useState<PositionCreateDto[]>([
    { name: '社長', level: 10, description: '最高経営責任者' },
    { name: '部長', level: 7, description: '部門責任者' },
    { name: 'マネージャー', level: 5, description: 'チームリーダー' },
    { name: 'メンバー', level: 3, description: '一般社員' },
  ]);
  const [departments, setDepartments] = useState<DepartmentCreateDto[]>([
    { name: '経営企画部', description: '経営戦略立案' },
    { name: '開発部', description: 'システム開発' },
    { name: '営業部', description: '営業活動' },
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const data: SetupRequestDto = {
        companyName,
        effectiveDate,
        positions,
        departments,
      };

      const response = await setupOrganization(data);

      // Add the newly created company to the list
      addCompany({
        id: response.companyId,
        name: response.companyName
      });

      // Save the newly created company ID to context
      setSelectedCompanyId(response.companyId);

      // Success - navigate to employee list
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : '組織のセットアップに失敗しました');
    } finally {
      setIsSubmitting(false);
    }
  };

  const addPosition = () => {
    setPositions([...positions, { name: '', level: 1, description: null }]);
  };

  const updatePosition = (index: number, field: keyof PositionCreateDto, value: string | number | null) => {
    const updated = [...positions];
    updated[index] = { ...updated[index], [field]: value };
    setPositions(updated);
  };

  const removePosition = (index: number) => {
    setPositions(positions.filter((_, i) => i !== index));
  };

  const addDepartment = () => {
    setDepartments([...departments, { name: '', description: '' }]);
  };

  const updateDepartment = (index: number, field: keyof DepartmentCreateDto, value: string) => {
    const updated = [...departments];
    updated[index] = { ...updated[index], [field]: value };
    setDepartments(updated);
  };

  const removeDepartment = (index: number) => {
    setDepartments(departments.filter((_, i) => i !== index));
  };

  const fillDemoData = () => {
    setCompanyName('株式会社サンプル');
    // Positions and departments already have default values
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white shadow-lg rounded-lg overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-primary-600 to-primary-800 px-6 py-8 text-white">
          <h2 className="text-3xl font-bold flex items-center gap-3">
            <span className="text-4xl">🎯</span>
            初期セットアップウィザード
          </h2>
          <p className="mt-2 text-primary-100">
            組織の基本情報を設定します。役職と部署を登録してください。
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-8">
          {/* Demo Data Button */}
          <button
            type="button"
            onClick={fillDemoData}
            className="w-full bg-indigo-100 text-indigo-700 border-2 border-indigo-300 py-3 px-6 rounded-lg hover:bg-indigo-200 transition-colors font-semibold flex items-center justify-center gap-2"
          >
            <span>✨</span>
            <span>デモデータで自動入力</span>
          </button>

          {/* Company Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              会社名 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              required
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              placeholder="株式会社サンプル"
            />
          </div>

          {/* Effective Date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              開始日 <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              required
              value={effectiveDate}
              onChange={(e) => setEffectiveDate(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
            />
            <p className="mt-1 text-sm text-gray-500">
              役職と部署が有効になる開始日（デモでは3ヶ月前に設定し、履歴データを生成できるようにします）
            </p>
          </div>

          {/* Positions Section */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                <span>👔</span> 役職
              </h3>
              <button
                type="button"
                onClick={addPosition}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
              >
                + 役職を追加
              </button>
            </div>

            <div className="space-y-4">
              {positions.map((position, index) => (
                <div key={index} className="flex gap-3 items-start p-4 bg-gray-50 rounded-lg">
                  <div className="flex-1 grid grid-cols-3 gap-3">
                    <input
                      type="text"
                      required
                      value={position.name}
                      onChange={(e) => updatePosition(index, 'name', e.target.value)}
                      placeholder="役職名"
                      className="px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-primary-500"
                    />
                    <input
                      type="number"
                      required
                      min="1"
                      max="10"
                      value={position.level}
                      onChange={(e) => updatePosition(index, 'level', parseInt(e.target.value))}
                      placeholder="レベル (1-10)"
                      className="px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-primary-500"
                    />
                    <input
                      type="text"
                      value={position.description || ''}
                      onChange={(e) => updatePosition(index, 'description', e.target.value || null)}
                      placeholder="説明（任意）"
                      className="px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-primary-500"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => removePosition(index)}
                    className="px-3 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                  >
                    削除
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* Departments Section */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                <span>🏢</span> 部署
              </h3>
              <button
                type="button"
                onClick={addDepartment}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
              >
                + 部署を追加
              </button>
            </div>

            <div className="space-y-4">
              {departments.map((department, index) => (
                <div key={index} className="flex gap-3 items-start p-4 bg-gray-50 rounded-lg">
                  <div className="flex-1 grid grid-cols-2 gap-3">
                    <input
                      type="text"
                      required
                      value={department.name}
                      onChange={(e) => updateDepartment(index, 'name', e.target.value)}
                      placeholder="部署名"
                      className="px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-primary-500"
                    />
                    <input
                      type="text"
                      required
                      value={department.description}
                      onChange={(e) => updateDepartment(index, 'description', e.target.value)}
                      placeholder="説明"
                      className="px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-primary-500"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => removeDepartment(index)}
                    className="px-3 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                  >
                    削除
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded">
              <p className="text-red-700">{error}</p>
            </div>
          )}

          {/* Submit Button */}
          <div className="flex gap-4">
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex-1 bg-primary-600 text-white py-3 px-6 rounded-lg hover:bg-primary-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors font-semibold"
            >
              {isSubmitting ? 'セットアップ中...' : '組織をセットアップ'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/')}
              className="px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              キャンセル
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
