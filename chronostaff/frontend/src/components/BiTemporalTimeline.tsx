import { useAssignmentHistory, useSalaryHistory, usePositions, useDepartments } from '../hooks/useAPI';
import { formatDate, formatDateOnly } from '../utils/date';
import BiTemporal2DTimeline from './BiTemporal2DTimeline';
import type { EmployeeAssignment, Salary } from '../types';

interface BiTemporalTimelineProps {
  employeeId: number;
}

export default function BiTemporalTimeline({ employeeId }: BiTemporalTimelineProps) {
  const { data: assignmentHistory, error: assignmentError, isLoading: assignmentLoading } = useAssignmentHistory(employeeId);
  const { data: salaryHistory, error: salaryError, isLoading: salaryLoading } = useSalaryHistory(employeeId);
  const { data: positions } = usePositions();
  const { data: departments } = useDepartments();

  if (assignmentLoading || salaryLoading) {
    return (
      <div className="flex justify-center items-center h-32">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (assignmentError) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        Error loading assignment history: {assignmentError.message}
      </div>
    );
  }

  if (salaryError) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        Error loading salary history: {salaryError.message}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Assignment History */}
      <div className="card">
        <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
          💼 配属履歴（バイテンポラル）
        </h3>
        {assignmentHistory && assignmentHistory.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    部署
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    役職
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    ビジネス有効期間
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    処理時刻期間
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    更新者
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {assignmentHistory.map((assignment, index) => {
                  const department = departments?.find(d => d.id === assignment.departmentId);
                  const position = positions?.find(p => p.id === assignment.positionId);
                  return (
                    <tr key={`${assignment.id}-${index}`} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-sm text-gray-900">
                        {department?.name || '-'}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-900">
                        {position ? `${position.name} (L${position.level})` : '-'}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600">
                        <div className="space-y-1">
                          <div>{formatDateOnly(assignment.businessFrom)}</div>
                          <div className="text-xs">〜 {formatDateOnly(assignment.businessThru)}</div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600">
                        <div className="space-y-1">
                          <div>{formatDate(assignment.processingFrom)}</div>
                          <div className="text-xs">〜 {formatDate(assignment.processingThru)}</div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-600">
                        {assignment.updatedBy}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-gray-500 italic">配属履歴なし</p>
        )}
      </div>

      {/* Salary History */}
      <div className="card">
        <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
          💰 給与履歴（バイテンポラル）
        </h3>
        {salaryHistory && salaryHistory.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    給与額
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    通貨
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    ビジネス有効期間
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    処理時刻期間
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    更新者
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {salaryHistory.map((salary, index) => (
                  <tr key={`${salary.id}-${index}`} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-semibold text-primary-600">
                      {salary.amount.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-900">
                      {salary.currency}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      <div className="space-y-1">
                        <div>{formatDateOnly(salary.businessFrom)}</div>
                        <div className="text-xs">〜 {formatDateOnly(salary.businessThru)}</div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      <div className="space-y-1">
                        <div>{formatDate(salary.processingFrom)}</div>
                        <div className="text-xs">〜 {formatDate(salary.processingThru)}</div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {salary.updatedBy}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-gray-500 italic">給与履歴なし</p>
        )}
      </div>

      {/* Bitemporal Explanation */}
      <div className="card bg-blue-50 border border-blue-200">
        <h4 className="text-sm font-semibold text-blue-900 mb-2">📚 バイテンポラルデータとは？</h4>
        <div className="text-sm text-blue-800 space-y-1">
          <p><strong>ビジネス有効期間：</strong> その事実がいつ有効だったか（実世界の時間）</p>
          <p><strong>処理時刻期間：</strong> システムがその情報をいつ記録していたか（システム時間）</p>
          <p className="text-xs mt-2 text-blue-700">
            ※ 同じビジネス期間でも、処理時刻が異なる複数のレコードが存在する場合、それは後から修正されたことを意味します
          </p>
        </div>
      </div>

      {/* 2D Visualization */}
      <BiTemporal2DTimeline employeeId={employeeId} />
    </div>
  );
}
