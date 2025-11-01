import { useState } from 'react';
import { adjustSalary } from '../api/client';
import type { SalaryAdjustmentRequestDto } from '../types';

interface SalaryAdjustmentFormProps {
  employeeId: number;
  employeeName: string;
  currentAmount: number;
  currentCurrency: string;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export default function SalaryAdjustmentForm({
  employeeId,
  employeeName,
  currentAmount,
  currentCurrency,
  onSuccess,
  onCancel
}: SalaryAdjustmentFormProps) {
  const [newAmount, setNewAmount] = useState<number | ''>(currentAmount);
  const [currency, setCurrency] = useState(currentCurrency || 'JPY');
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
      if (newAmount === '' || newAmount <= 0) {
        throw new Error('有効な給与額を入力してください');
      }

      // Check if anything actually changed
      if (newAmount === currentAmount && currency === currentCurrency) {
        throw new Error('変更がありません。異なる給与額または通貨を入力してください');
      }

      const data: SalaryAdjustmentRequestDto = {
        newAmount: Number(newAmount),
        currency,
        effectiveDate,
        reason: reason || undefined,
        updatedBy,
      };

      console.log('DEBUG: Sending effectiveDate to backend:', effectiveDate);
      console.log('DEBUG: Full request data:', JSON.stringify(data, null, 2));

      await adjustSalary(employeeId, data);

      // Success
      onSuccess?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : '給与調整の登録に失敗しました');
    } finally {
      setIsSubmitting(false);
    }
  };

  const amountChange = newAmount !== '' && newAmount !== currentAmount
    ? newAmount - currentAmount
    : 0;
  const percentChange = currentAmount > 0 && amountChange !== 0
    ? ((amountChange / currentAmount) * 100).toFixed(1)
    : '0.0';

  return (
    <div className="bg-white rounded-lg shadow-lg overflow-hidden">
      {/* Header */}
      <div className="bg-gradient-to-r from-amber-600 to-amber-800 px-6 py-6 text-white">
        <h2 className="text-2xl font-bold flex items-center gap-3">
          <span className="text-3xl">💰</span>
          給与調整
        </h2>
        <p className="mt-2 text-amber-100">
          {employeeName} の給与を調整します
        </p>
      </div>

      {/* Current Salary Info */}
      <div className="bg-blue-50 px-6 py-4 border-b border-blue-200">
        <h3 className="text-sm font-semibold text-blue-900 mb-2">現在の給与</h3>
        <div className="flex items-center gap-2 text-lg font-semibold text-blue-800">
          <span>{currentCurrency}</span>
          <span>{currentAmount.toLocaleString()}</span>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="p-6 space-y-6">
        {/* New Salary */}
        <section>
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <span>💵</span> 新しい給与
          </h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                新給与額 <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                required
                min="0"
                step="1000"
                value={newAmount}
                onChange={(e) => setNewAmount(e.target.value ? Number(e.target.value) : '')}
                placeholder="6000000"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent text-lg font-semibold"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                通貨 <span className="text-red-500">*</span>
              </label>
              <select
                required
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent"
              >
                <option value="JPY">JPY (日本円)</option>
                <option value="USD">USD (米ドル)</option>
                <option value="EUR">EUR (ユーロ)</option>
              </select>
            </div>
          </div>

          {/* Change Summary */}
          {newAmount !== '' && newAmount !== currentAmount && (
            <div className={`mt-4 p-4 rounded-lg ${amountChange > 0 ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-700">変更額:</span>
                <span className={`text-lg font-bold ${amountChange > 0 ? 'text-green-700' : 'text-red-700'}`}>
                  {amountChange > 0 ? '+' : ''}{amountChange.toLocaleString()} {currency}
                  <span className="ml-2 text-sm">({amountChange > 0 ? '+' : ''}{percentChange}%)</span>
                </span>
              </div>
            </div>
          )}
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
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent"
              />
              <p className="text-xs text-gray-500 mt-1">
                この日付から新しい給与が有効になります（Business Date）
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
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                調整理由（任意）
              </label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="年次昇給、昇進に伴う増額、等"
                rows={3}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent"
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
                実効日は「いつから新しい給与が有効になるか」を表します。
                過去日を指定すると遡及調整、未来日を指定すると予定調整として登録されます。
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
            className="flex-1 bg-amber-600 text-white py-3 px-6 rounded-lg hover:bg-amber-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors font-semibold"
          >
            {isSubmitting ? '登録中...' : '給与調整を登録'}
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
