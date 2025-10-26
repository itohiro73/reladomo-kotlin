import { useParams, Link, useSearchParams } from 'react-router-dom';
import {
  useEmployee,
  useEmployeeAsOf,
  useAssignmentsByEmployee,
  useSalariesByEmployee,
  usePositions,
  useDepartments
} from '../hooks/useAPI';
import { formatDate, formatDateOnly } from '../utils/date';
import BiTemporalTimeline from './BiTemporalTimeline';

export default function EmployeeDetail() {
  const { id } = useParams<{ id: string }>();
  const employeeId = id ? parseInt(id) : null;
  const [searchParams] = useSearchParams();
  const asOfMonth = searchParams.get('asOfMonth');

  // Use AsOf query if asOfMonth parameter is present
  const { data: asOfData, error: asOfError, isLoading: asOfLoading } = useEmployeeAsOf(employeeId, asOfMonth);
  const { data: employee, error, isLoading } = useEmployee(asOfMonth ? null : employeeId);
  const { data: assignments } = useAssignmentsByEmployee(asOfMonth ? null : employeeId);
  const { data: salaries } = useSalariesByEmployee(asOfMonth ? null : employeeId);
  const { data: positions } = usePositions();
  const { data: departments } = useDepartments();

  if (isLoading || asOfLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error || asOfError) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        Error loading employee: {(error || asOfError)?.message}
      </div>
    );
  }

  // For AsOf view
  if (asOfMonth && asOfData) {
    const { employee: emp, assignment, salary } = asOfData;

    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <Link
            to={`/org-chart?month=${asOfMonth}`}
            className="inline-flex items-center gap-2 text-primary-600 hover:text-primary-700 font-medium transition-colors"
          >
            <span>←</span>
            <span>組織図タイムトラベルに戻る</span>
          </Link>

          <div className="flex items-center gap-2">
            <span className="bg-purple-100 text-purple-800 px-3 py-1 rounded-full font-medium text-sm">
              📅 {asOfMonth} 時点の情報
            </span>
            <Link
              to={`/employees/${employeeId}`}
              className="bg-gray-100 text-gray-700 hover:bg-gray-200 px-3 py-1 rounded-full font-medium text-sm transition-colors"
            >
              現在の情報を表示
            </Link>
          </div>
        </div>

        {/* Header Card with Avatar */}
        <div className="card">
          <div className="flex items-start gap-6">
            <div className="w-20 h-20 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center text-white font-bold text-3xl flex-shrink-0">
              {emp.name.charAt(0)}
            </div>
            <div className="flex-1">
              <h2 className="text-3xl font-bold text-gray-900 mb-2">{emp.name}</h2>
              <div className="flex items-center gap-4 text-sm text-gray-600">
                <span className="flex items-center gap-2">
                  <span>🆔</span>
                  {emp.employeeNumber}
                </span>
                <span className="flex items-center gap-2">
                  <span>📧</span>
                  {emp.email}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Basic Information */}
        <div className="card">
          <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
            📋 基本情報
          </h3>
          <div className="space-y-3">
            <InfoRow icon="🆔" label="社員番号" value={emp.employeeNumber} />
            <InfoRow icon="📧" label="メールアドレス" value={emp.email} />
            <InfoRow icon="📅" label="入社日" value={formatDateOnly(emp.hireDate)} />
          </div>
        </div>

        {/* AsOf Assignment */}
        <div className="card">
          <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
            💼 {asOfMonth} 時点での配属・役職
          </h3>
          {assignment ? (
            <div className="space-y-3">
              <InfoRow icon="🏢" label="部署" value={assignment.departmentName} />
              <InfoRow
                icon="💼"
                label="役職"
                value={`${assignment.positionName} (Level ${assignment.positionLevel})`}
              />
              <InfoRow icon="📅" label="配属開始日" value={formatDateOnly(assignment.businessFrom)} />
              <InfoRow icon="👤" label="更新者" value={assignment.updatedBy} />
            </div>
          ) : (
            <p className="text-gray-500 italic">この時点では配属情報がありません</p>
          )}
        </div>

        {/* AsOf Salary */}
        <div className="card">
          <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
            💰 {asOfMonth} 時点での給与情報
          </h3>
          {salary ? (
            <div className="space-y-3">
              <InfoRow
                icon="💰"
                label="給与"
                value={`${salary.currency} ${salary.amount.toLocaleString()}`}
                valueClassName="font-semibold text-primary-600"
              />
              <InfoRow icon="📅" label="適用開始日" value={formatDateOnly(salary.businessFrom)} />
              <InfoRow icon="👤" label="更新者" value={salary.updatedBy} />
            </div>
          ) : (
            <p className="text-gray-500 italic">この時点では給与情報がありません</p>
          )}
        </div>

        {/* Info note */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-start gap-3">
            <span className="text-2xl">ℹ️</span>
            <div className="flex-1">
              <h3 className="font-semibold text-blue-900 mb-2">AsOfクエリについて</h3>
              <p className="text-sm text-blue-800">
                この画面では {asOfMonth} 時点で有効だったビジネスデータを表示しています。
                ビジネス時間（BUSINESS_FROM/THRU）で {asOfMonth}-01 時点のデータをクエリし、
                処理時間（PROCESSING_FROM/THRU）は現在の認識（infinity）を使用しています。
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Current view (no AsOf parameter)
  if (!employee) {
    return (
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-yellow-700">
        従業員が見つかりません
      </div>
    );
  }

  const currentAssignment = assignments?.[0];
  const currentSalary = salaries?.[0];
  const position = currentAssignment && positions?.find(p => p.id === currentAssignment.positionId);
  const department = currentAssignment && departments?.find(d => d.id === currentAssignment.departmentId);

  return (
    <div className="space-y-6">
      <Link
        to="/"
        className="inline-flex items-center gap-2 text-primary-600 hover:text-primary-700 font-medium transition-colors"
      >
        <span>←</span>
        <span>従業員一覧に戻る</span>
      </Link>

      {/* Header Card with Avatar */}
      <div className="card">
        <div className="flex items-start gap-6">
          <div className="w-20 h-20 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center text-white font-bold text-3xl flex-shrink-0">
            {employee.name.charAt(0)}
          </div>
          <div className="flex-1">
            <h2 className="text-3xl font-bold text-gray-900 mb-2">{employee.name}</h2>
            <div className="flex items-center gap-4 text-sm text-gray-600">
              <span className="flex items-center gap-2">
                <span>🆔</span>
                {employee.employeeNumber}
              </span>
              <span className="flex items-center gap-2">
                <span>📧</span>
                {employee.email}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Basic Information */}
      <div className="card">
        <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
          📋 基本情報
        </h3>
        <div className="space-y-3">
          <InfoRow icon="🆔" label="社員番号" value={employee.employeeNumber} />
          <InfoRow icon="📧" label="メールアドレス" value={employee.email} />
          <InfoRow icon="📅" label="入社日" value={formatDateOnly(employee.hireDate)} />
        </div>
      </div>

      {/* Current Assignment */}
      <div className="card">
        <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
          💼 現在の配属・役職
        </h3>
        {currentAssignment ? (
          <div className="space-y-3">
            <InfoRow icon="🏢" label="部署" value={department?.name || '-'} />
            <InfoRow
              icon="💼"
              label="役職"
              value={position ? `${position.name} (Level ${position.level})` : '-'}
            />
            <InfoRow icon="📅" label="配属開始日" value={formatDateOnly(currentAssignment.businessFrom)} />
            <InfoRow icon="👤" label="更新者" value={currentAssignment.updatedBy} />
          </div>
        ) : (
          <p className="text-gray-500 italic">配属情報なし</p>
        )}
      </div>

      {/* Current Salary */}
      <div className="card">
        <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
          💰 現在の給与情報
        </h3>
        {currentSalary ? (
          <div className="space-y-3">
            <InfoRow
              icon="💰"
              label="給与"
              value={`${currentSalary.currency} ${currentSalary.amount.toLocaleString()}`}
              valueClassName="font-semibold text-primary-600"
            />
            <InfoRow icon="📅" label="適用開始日" value={formatDateOnly(currentSalary.businessFrom)} />
            <InfoRow icon="👤" label="更新者" value={currentSalary.updatedBy} />
          </div>
        ) : (
          <p className="text-gray-500 italic">給与情報なし</p>
        )}
      </div>

      {/* Temporal Information */}
      <div className="card bg-gray-50">
        <h3 className="text-xl font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">
          ⏱️ テンポラル情報（デバッグ用）
        </h3>
        <div className="space-y-2 text-sm text-gray-600">
          <div className="flex items-center gap-2">
            <span className="font-medium w-40">Processing From:</span>
            <span className="font-mono">{formatDate(employee.processingFrom)}</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="font-medium w-40">Processing Thru:</span>
            <span className="font-mono">{formatDate(employee.processingThru)}</span>
          </div>
        </div>
      </div>

      {/* Bitemporal Timeline */}
      {employeeId && <BiTemporalTimeline employeeId={employeeId} />}
    </div>
  );
}

function InfoRow({
  icon,
  label,
  value,
  valueClassName = 'text-gray-900'
}: {
  icon: string;
  label: string;
  value: string;
  valueClassName?: string;
}) {
  return (
    <div className="flex items-start gap-3 py-2">
      <span className="text-xl">{icon}</span>
      <div className="flex-1 grid grid-cols-1 sm:grid-cols-3 gap-2">
        <span className="font-medium text-gray-700">{label}:</span>
        <span className={`sm:col-span-2 ${valueClassName}`}>{value}</span>
      </div>
    </div>
  );
}
