import { useMemo } from 'react';
import { useCompany } from '../contexts/CompanyContext';
import { useScheduledChanges } from '../hooks/useAPI';
import { formatDateOnly } from '../utils/date';
import type {
  ScheduledChange,
  TransferDetails,
  SalaryAdjustmentDetails,
  DepartmentCreationDetails,
  PositionCreationDetails
} from '../types';

export default function ScheduledChangesView() {
  const { selectedCompanyId } = useCompany();
  const { data: changes, error, isLoading } = useScheduledChanges(selectedCompanyId);

  const groupedChanges = useMemo(() => {
    if (!changes) return new Map<string, ScheduledChange[]>();

    const groups = new Map<string, ScheduledChange[]>();
    changes.forEach(change => {
      const dateKey = change.effectiveDate;
      const group = groups.get(dateKey) || [];
      group.push(change);
      groups.set(dateKey, group);
    });

    // Sort by date
    return new Map([...groups.entries()].sort((a, b) => a[0].localeCompare(b[0])));
  }, [changes]);

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
        エラーが発生しました: {error.message}
      </div>
    );
  }

  if (!changes || changes.length === 0) {
    return (
      <div className="space-y-6">
        <h2 className="text-3xl font-bold text-gray-900">予定されている変更 🔮</h2>
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-8 text-center">
          <p className="text-blue-700 text-lg">現在、予定されている変更はありません</p>
          <p className="text-blue-600 text-sm mt-2">
            従業員の異動や給与調整を登録すると、ここに表示されます
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold text-gray-900">予定されている変更 🔮</h2>
        <div className="flex items-center gap-2 text-sm text-gray-600">
          <span className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full font-medium">
            {changes.length}件の変更
          </span>
        </div>
      </div>

      <div className="space-y-6">
        {Array.from(groupedChanges.entries()).map(([date, dateChanges]) => (
          <div key={date} className="card">
            <div className="flex items-center gap-3 mb-4 pb-3 border-b border-gray-200">
              <div className="text-2xl">📅</div>
              <h3 className="text-xl font-bold text-gray-900">
                {formatDateOnly(date)} 予定
              </h3>
              <span className="ml-auto text-sm text-gray-500">
                {dateChanges.length}件
              </span>
            </div>

            <div className="space-y-4">
              {dateChanges.map(change => (
                <ChangeItem key={`${change.changeType}-${change.recordId}`} change={change} />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function ChangeItem({ change }: { change: ScheduledChange }) {
  const icon = getChangeIcon(change.changeType);
  const details = renderChangeDetails(change);

  return (
    <div className="bg-gradient-to-r from-gray-50 to-white rounded-lg p-4 border border-gray-200 hover:border-primary-300 transition-colors">
      <div className="flex items-start gap-4">
        <div className="text-2xl flex-shrink-0">{icon}</div>
        <div className="flex-grow">
          <div className="flex items-center gap-2 mb-2">
            <h4 className="font-semibold text-gray-900">{change.entityName}</h4>
            <span className="text-xs px-2 py-1 rounded bg-gray-100 text-gray-600">
              {getChangeTypeLabel(change.changeType)}
            </span>
          </div>
          {details}
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <button className="text-xs px-3 py-1.5 bg-primary-100 text-primary-700 rounded hover:bg-primary-200 transition-colors font-medium">
            詳細
          </button>
          <button className="text-xs px-3 py-1.5 bg-gray-100 text-gray-700 rounded hover:bg-gray-200 transition-colors font-medium">
            編集
          </button>
          <button className="text-xs px-3 py-1.5 bg-red-100 text-red-700 rounded hover:bg-red-200 transition-colors font-medium">
            キャンセル
          </button>
        </div>
      </div>
    </div>
  );
}

function getChangeIcon(changeType: string): string {
  switch (changeType) {
    case 'TRANSFER':
      return '📍';
    case 'SALARY':
      return '💰';
    case 'DEPARTMENT':
      return '🏢';
    case 'POSITION':
      return '👔';
    default:
      return '📝';
  }
}

function getChangeTypeLabel(changeType: string): string {
  switch (changeType) {
    case 'TRANSFER':
      return '異動';
    case 'SALARY':
      return '給与調整';
    case 'DEPARTMENT':
      return '部署作成';
    case 'POSITION':
      return '役職作成';
    default:
      return '変更';
  }
}

function renderChangeDetails(change: ScheduledChange): JSX.Element {
  switch (change.changeType) {
    case 'TRANSFER':
      return <TransferDetailsView details={change.details as TransferDetails} />;
    case 'SALARY':
      return <SalaryDetailsView details={change.details as SalaryAdjustmentDetails} />;
    case 'DEPARTMENT':
      return <DepartmentDetailsView details={change.details as DepartmentCreationDetails} />;
    case 'POSITION':
      return <PositionDetailsView details={change.details as PositionCreationDetails} />;
    default:
      return <div className="text-sm text-gray-600">詳細情報なし</div>;
  }
}

function TransferDetailsView({ details }: { details: TransferDetails }) {
  return (
    <div className="text-sm text-gray-600 space-y-1">
      {details.fromDepartmentName && (
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-700">部署:</span>
          <span>{details.fromDepartmentName}</span>
          <span className="text-primary-600">→</span>
          <span className="text-primary-700 font-medium">{details.toDepartmentName}</span>
        </div>
      )}
      {details.fromPositionName && (
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-700">役職:</span>
          <span>{details.fromPositionName}</span>
          <span className="text-primary-600">→</span>
          <span className="text-primary-700 font-medium">{details.toPositionName}</span>
        </div>
      )}
      {!details.fromDepartmentName && (
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-700">新規配属:</span>
          <span className="text-primary-700 font-medium">{details.toDepartmentName} / {details.toPositionName}</span>
        </div>
      )}
    </div>
  );
}

function SalaryDetailsView({ details }: { details: SalaryAdjustmentDetails }) {
  return (
    <div className="text-sm text-gray-600 space-y-1">
      <div className="flex items-center gap-2">
        {details.fromAmount ? (
          <>
            <span className="font-medium text-gray-700">給与:</span>
            <span>¥{Number(details.fromAmount).toLocaleString()}</span>
            <span className="text-primary-600">→</span>
            <span className="text-primary-700 font-medium">¥{Number(details.toAmount).toLocaleString()}</span>
          </>
        ) : (
          <>
            <span className="font-medium text-gray-700">初期給与:</span>
            <span className="text-primary-700 font-medium">¥{Number(details.toAmount).toLocaleString()}</span>
          </>
        )}
      </div>
    </div>
  );
}

function DepartmentDetailsView({ details }: { details: DepartmentCreationDetails }) {
  return (
    <div className="text-sm text-gray-600 space-y-1">
      <div className="flex items-center gap-2">
        <span className="font-medium text-gray-700">新規部署:</span>
        <span className="text-primary-700 font-medium">{details.departmentName}</span>
      </div>
      {details.parentDepartmentName && (
        <div className="flex items-center gap-2">
          <span className="font-medium text-gray-700">親部署:</span>
          <span>{details.parentDepartmentName}</span>
        </div>
      )}
      {details.description && (
        <div className="text-xs text-gray-500 mt-1">{details.description}</div>
      )}
    </div>
  );
}

function PositionDetailsView({ details }: { details: PositionCreationDetails }) {
  return (
    <div className="text-sm text-gray-600 space-y-1">
      <div className="flex items-center gap-2">
        <span className="font-medium text-gray-700">新規役職:</span>
        <span className="text-primary-700 font-medium">{details.positionName}</span>
        <span className="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-600">
          レベル {details.level}
        </span>
      </div>
      {details.description && (
        <div className="text-xs text-gray-500 mt-1">{details.description}</div>
      )}
    </div>
  );
}
