import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createEmployee } from '../api/client';
import { usePositions, useDepartments } from '../hooks/useAPI';
import { useCompany } from '../contexts/CompanyContext';
import type { EmployeeCreateDto, InitialAssignmentDto, InitialSalaryDto } from '../types';

export default function EmployeeAddForm() {
  const navigate = useNavigate();
  const { selectedCompanyId } = useCompany();
  const { data: positions } = usePositions();
  const { data: departments } = useDepartments(selectedCompanyId);

  // Employee basic info
  const [employeeNumber, setEmployeeNumber] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [hireDate, setHireDate] = useState('');

  // Assignment info
  const [departmentId, setDepartmentId] = useState<number | ''>('');
  const [positionId, setPositionId] = useState<number | ''>('');
  const [assignmentEffectiveDate, setAssignmentEffectiveDate] = useState('');
  const [assignmentUpdatedBy, setAssignmentUpdatedBy] = useState('');

  // Salary info
  const [amount, setAmount] = useState<number | ''>('');
  const [currency, setCurrency] = useState('JPY');
  const [salaryEffectiveDate, setSalaryEffectiveDate] = useState('');
  const [salaryUpdatedBy, setSalaryUpdatedBy] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Auto-fill effective dates to hire date
  const handleHireDateChange = (date: string) => {
    setHireDate(date);
    if (!assignmentEffectiveDate) setAssignmentEffectiveDate(date);
    if (!salaryEffectiveDate) setSalaryEffectiveDate(date);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      if (departmentId === '' || positionId === '' || amount === '') {
        throw new Error('すべての必須項目を入力してください');
      }

      const assignment: InitialAssignmentDto = {
        departmentId: Number(departmentId),
        positionId: Number(positionId),
        effectiveDate: assignmentEffectiveDate,
        updatedBy: assignmentUpdatedBy,
      };

      const salary: InitialSalaryDto = {
        amount: Number(amount),
        currency,
        effectiveDate: salaryEffectiveDate,
        updatedBy: salaryUpdatedBy,
      };

      const data: EmployeeCreateDto = {
        companyId: selectedCompanyId!,
        employeeNumber,
        name,
        email,
        hireDate,
        assignment,
        salary,
      };

      await createEmployee(data);

      // Success - navigate to employee list
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : '従業員の追加に失敗しました');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white shadow-lg rounded-lg overflow-hidden">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-blue-800 px-6 py-8 text-white">
          <h2 className="text-3xl font-bold flex items-center gap-3">
            <span className="text-4xl">👤</span>
            新規従業員登録
          </h2>
          <p className="mt-2 text-blue-100">
            従業員の基本情報、配属、給与を登録します。
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-8">
          {/* Employee Basic Info */}
          <section>
            <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <span>📋</span> 基本情報
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  社員番号 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={employeeNumber}
                  onChange={(e) => setEmployeeNumber(e.target.value)}
                  placeholder="EMP001"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  氏名 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="田中太郎"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  メールアドレス <span className="text-red-500">*</span>
                </label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="tanaka@example.com"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  入社日 <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  required
                  value={hireDate}
                  onChange={(e) => handleHireDateChange(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>
          </section>

          {/* Assignment Info */}
          <section>
            <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <span>🏢</span> 配属情報
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  部署 <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={departmentId}
                  onChange={(e) => setDepartmentId(Number(e.target.value))}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">選択してください</option>
                  {departments?.map((dept) => (
                    <option key={dept.id} value={dept.id}>
                      {dept.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  役職 <span className="text-red-500">*</span>
                </label>
                <select
                  required
                  value={positionId}
                  onChange={(e) => setPositionId(Number(e.target.value))}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">選択してください</option>
                  {positions?.map((pos) => (
                    <option key={pos.id} value={pos.id}>
                      {pos.name} (Level {pos.level})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  配属実効日 <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  required
                  value={assignmentEffectiveDate}
                  onChange={(e) => setAssignmentEffectiveDate(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">デフォルト: 入社日</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  登録者 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={assignmentUpdatedBy}
                  onChange={(e) => setAssignmentUpdatedBy(e.target.value)}
                  placeholder="hr@example.com"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>
          </section>

          {/* Salary Info */}
          <section>
            <h3 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <span>💰</span> 給与情報
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  年俸 <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  required
                  min="0"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value ? Number(e.target.value) : '')}
                  placeholder="5000000"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
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
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="JPY">JPY (日本円)</option>
                  <option value="USD">USD (米ドル)</option>
                  <option value="EUR">EUR (ユーロ)</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  給与実効日 <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  required
                  value={salaryEffectiveDate}
                  onChange={(e) => setSalaryEffectiveDate(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">デフォルト: 入社日</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  登録者 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={salaryUpdatedBy}
                  onChange={(e) => setSalaryUpdatedBy(e.target.value)}
                  placeholder="hr@example.com"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>
          </section>

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
              className="flex-1 bg-blue-600 text-white py-3 px-6 rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors font-semibold"
            >
              {isSubmitting ? '登録中...' : '従業員を登録'}
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
