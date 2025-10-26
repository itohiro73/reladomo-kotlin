import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useOrganizationSnapshot } from '../hooks/useAPI';
import type { DepartmentSnapshot, EmployeeSnapshot } from '../types';

export default function OrgChart() {
  // Default to current month (YYYY-MM format)
  const today = new Date();
  const defaultMonth = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;

  const [selectedMonth, setSelectedMonth] = useState(defaultMonth);

  // Convert YYYY-MM to YYYY-MM-01 for API call
  const asOfDate = `${selectedMonth}-01`;
  const { data: snapshot, error, isLoading } = useOrganizationSnapshot(asOfDate);

  const handleMonthChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedMonth(e.target.value);
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
        Error loading organization chart: {error.message}
      </div>
    );
  }

  if (!snapshot) return null;

  const totalEmployees = snapshot.departments.reduce((sum, dept) => sum + dept.employees.length, 0);

  return (
    <div className="space-y-6">
      {/* Header with Date Selector */}
      <div className="bg-white rounded-lg shadow-lg p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h2 className="text-3xl font-bold text-gray-900 mb-2">組織図 タイムトラベル</h2>
            <p className="text-sm text-gray-600">
              指定した日付時点での組織状態を表示します（ビジネス時間でのAsOfクエリ）
            </p>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex flex-col gap-2">
              <label htmlFor="asOfMonth" className="text-sm font-medium text-gray-700">
                基準年月
              </label>
              <input
                id="asOfMonth"
                type="month"
                value={selectedMonth}
                onChange={handleMonthChange}
                className="px-4 py-2 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              />
            </div>

            <div className="flex items-center gap-2 pt-6">
              <span className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full font-medium text-sm">
                {snapshot.departments.length}部署
              </span>
              <span className="bg-green-100 text-green-800 px-3 py-1 rounded-full font-medium text-sm">
                {totalEmployees}名
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Organization Chart */}
      <div className="space-y-4">
        {snapshot.departments.map(dept => (
          <DeptNode key={dept.id} dept={dept} />
        ))}
      </div>

      {/* Info Footer */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <span className="text-2xl">ℹ️</span>
          <div className="flex-1">
            <h3 className="font-semibold text-blue-900 mb-2">タイムトラベル機能について</h3>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>• 選択した日付時点で有効だった組織状態を表示します</li>
              <li>• ビジネス時間（BUSINESS_FROM/THRU）でのAsOfクエリを使用</li>
              <li>• 処理時間（PROCESSING_FROM/THRU）は現在の認識（infinity）を使用</li>
              <li>• 過去の日付を選択すると、その時点の組織構成を確認できます</li>
              <li>• 未来の日付を選択すると、予定されている組織変更を確認できます</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

function DeptNode({ dept }: { dept: DepartmentSnapshot }) {
  // Sort employees by position level (descending) and name
  const sortedEmployees = [...dept.employees].sort((a, b) => {
    if (a.positionLevel !== b.positionLevel) {
      return b.positionLevel - a.positionLevel;
    }
    return a.name.localeCompare(b.name);
  });

  return (
    <div className="bg-gradient-to-r from-primary-500 to-primary-700 rounded-lg shadow-lg overflow-hidden">
      {/* Department Header */}
      <div className="p-4 text-white">
        <div className="flex items-center gap-3 mb-2">
          <span className="text-3xl">🏢</span>
          <div className="flex-1">
            <h3 className="text-xl font-bold">{dept.name}</h3>
            <p className="text-sm opacity-90">従業員数: {dept.employees.length}名</p>
          </div>
        </div>
      </div>

      {/* Employee Grid */}
      {sortedEmployees.length > 0 && (
        <div className="bg-white bg-opacity-95 p-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {sortedEmployees.map(emp => (
              <Link
                key={emp.id}
                to={`/employees/${emp.id}`}
                className="flex items-center gap-3 p-3 bg-white rounded-lg border border-gray-200 hover:border-primary-400 hover:shadow-md transition-all duration-200 group"
              >
                <div className="w-10 h-10 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
                  {emp.name.charAt(0)}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-900 truncate group-hover:text-primary-600 transition-colors">
                    {emp.name}
                  </p>
                  <p className="text-xs text-gray-600 truncate">
                    {emp.positionName}
                    <span className="text-gray-400 ml-1">(Lv{emp.positionLevel})</span>
                  </p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
