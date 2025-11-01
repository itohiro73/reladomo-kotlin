import { useState } from 'react';
import { transferEmployee } from '../api/client';
import { usePositions, useDepartments } from '../hooks/useAPI';
import { useCompany } from '../contexts/CompanyContext';
import type { TransferRequestDto } from '../types';

interface TransferFormProps {
  employeeId: number;
  employeeName: string;
  currentDepartmentId: number;
  currentPositionId: number;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export default function TransferForm({
  employeeId,
  employeeName,
  currentDepartmentId,
  currentPositionId,
  onSuccess,
  onCancel
}: TransferFormProps) {
  const { selectedCompanyId } = useCompany();
  const { data: positions } = usePositions(selectedCompanyId);
  const { data: departments } = useDepartments(selectedCompanyId);

  const [newDepartmentId, setNewDepartmentId] = useState<number | ''>(currentDepartmentId);
  const [newPositionId, setNewPositionId] = useState<number | ''>(currentPositionId);
  const [effectiveDate, setEffectiveDate] = useState(() => {
    // Default to today in local timezone (JST)
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  });
  const [reason, setReason] = useState('');
  const [updatedBy, setUpdatedBy] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      if (newDepartmentId === '' || newPositionId === '') {
        throw new Error('部署と役職を選択してください');
      }

      // Check if anything actually changed
      if (newDepartmentId === currentDepartmentId && newPositionId === currentPositionId) {
        throw new Error('変更がありません。異なる部署または役職を選択してください');
      }

      const data: TransferRequestDto = {
        newDepartmentId: Number(newDepartmentId),
        newPositionId: Number(newPositionId),
        effectiveDate,
        reason: reason || undefined,
        updatedBy,
      };

      console.log('DEBUG: Sending effectiveDate to backend:', effectiveDate);
      console.log('DEBUG: Full request data:', JSON.stringify(data, null, 2));

      await transferEmployee(employeeId, data);

      // Success
      onSuccess?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : '異動の登録に失敗しました');
    } finally {
      setIsSubmitting(false);
    }
  };

  const currentDepartment = departments?.find(d => d.id === currentDepartmentId);
  const currentPosition = positions?.find(p => p.id === currentPositionId);

  return (
    <div className="bg-white rounded-lg shadow-lg overflow-hidden">
      {/* Header */}
      <div className="bg-gradient-to-r from-green-600 to-green-800 px-6 py-6 text-white">
        <h2 className="text-2xl font-bold flex items-center gap-3">
          <span className="text-3xl">🔄</span>
          従業員異動・配置転換
        </h2>
        <p className="mt-2 text-green-100">
          {employeeName} の異動を登録します
        </p>
      </div>

      {/* Current Assignment Info */}
      <div className="bg-blue-50 px-6 py-4 border-b border-blue-200">
        <h3 className="text-sm font-semibold text-blue-900 mb-2">現在の配属</h3>
        <div className="flex items-center gap-6 text-sm text-blue-800">
          <div className="flex items-center gap-2">
            <span className="font-medium">部署:</span>
            <span>{currentDepartment?.name || '-'}</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-medium">役職:</span>
            <span>{currentPosition?.name || '-'} (Level {currentPosition?.level || '-'})</span>
          </div>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="p-6 space-y-6">
        {/* New Assignment */}
        <section>
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <span>🎯</span> 異動先
          </h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                異動先部署 <span className="text-red-500">*</span>
              </label>
              <select
                required
                value={newDepartmentId}
                onChange={(e) => setNewDepartmentId(Number(e.target.value))}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
              >
                <option value="">選択してください</option>
                {departments?.map((dept) => (
                  <option key={dept.id} value={dept.id}>
                    {dept.name}
                    {dept.id === currentDepartmentId ? ' (現在の部署)' : ''}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                異動後の役職 <span className="text-red-500">*</span>
              </label>
              <select
                required
                value={newPositionId}
                onChange={(e) => setNewPositionId(Number(e.target.value))}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
              >
                <option value="">選択してください</option>
                {positions?.map((pos) => (
                  <option key={pos.id} value={pos.id}>
                    {pos.name} (Level {pos.level})
                    {pos.id === currentPositionId ? ' (現在の役職)' : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </section>

        {/* Effective Date and Metadata */}
        <section>
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <span>📅</span> 実効日・登録情報
          </h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                実効日 <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                required
                value={effectiveDate}
                onChange={(e) => setEffectiveDate(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
              />
              <p className="text-xs text-gray-500 mt-1">
                この日付から新しい配属が有効になります（Business Date）
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                登録者 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                value={updatedBy}
                onChange={(e) => setUpdatedBy(e.target.value)}
                placeholder="hr@example.com"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                異動理由（任意）
              </label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="昇進に伴う営業部への異動"
                rows={3}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
              />
            </div>
          </div>
        </section>

        {/* Explanation */}
        <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
          <div className="flex items-start gap-3">
            <span className="text-2xl">💡</span>
            <div className="flex-1">
              <h4 className="font-semibold text-purple-900 mb-1">実効日（Business Date）について</h4>
              <p className="text-sm text-purple-800">
                実効日は「いつから新しい配属が有効になるか」を表します。
                過去日を指定すると遡及異動、未来日を指定すると予定異動として登録されます。
                システムは現在時刻（Processing Time）を自動で記録し、監査証跡として保存します。
              </p>
            </div>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded">
            <p className="text-red-700">{error}</p>
          </div>
        )}

        {/* Submit Buttons */}
        <div className="flex gap-4">
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex-1 bg-green-600 text-white py-3 px-6 rounded-lg hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors font-semibold"
          >
            {isSubmitting ? '登録中...' : '異動を登録'}
          </button>
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              disabled={isSubmitting}
              className="px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
            >
              キャンセル
            </button>
          )}
        </div>
      </form>
    </div>
  );
}
