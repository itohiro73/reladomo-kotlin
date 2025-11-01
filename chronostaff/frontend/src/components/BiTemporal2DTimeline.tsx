import { useState, useMemo } from 'react';
import { useAllAssignmentHistory, useAllSalaryHistory, usePositions, useDepartments } from '../hooks/useAPI';
import { useCompany } from '../contexts/CompanyContext';
import type { EmployeeAssignment, Salary } from '../types';

interface BiTemporal2DTimelineProps {
  employeeId: number;
}

interface TimeRange {
  minTime: number;
  maxTime: number;
}

export default function BiTemporal2DTimeline({ employeeId }: BiTemporal2DTimelineProps) {
  const { selectedCompanyId } = useCompany();
  const { data: assignmentHistory, error: assignmentError, isLoading: assignmentLoading } = useAllAssignmentHistory(employeeId);
  const { data: salaryHistory, error: salaryError, isLoading: salaryLoading } = useAllSalaryHistory(employeeId);
  const { data: positions } = usePositions(selectedCompanyId);
  const { data: departments } = useDepartments(selectedCompanyId);
  const [hoveredRecord, setHoveredRecord] = useState<{ type: 'assignment' | 'salary'; data: EmployeeAssignment | Salary } | null>(null);
  const [viewMode, setViewMode] = useState<'assignment' | 'salary'>('assignment');

  // Calculate time ranges
  const timeRanges = useMemo(() => {
    if (!assignmentHistory && !salaryHistory) return null;

    const allRecords = [
      ...(assignmentHistory || []),
      ...(salaryHistory || [])
    ];

    // Filter out infinity timestamps (9999-12-01)
    const finiteTimestamps = allRecords.flatMap(r => {
      const timestamps = [
        new Date(r.businessFrom).getTime(),
        new Date(r.processingFrom).getTime()
      ];

      // Only include business/processing THRU if not infinity
      if (!r.businessThru.startsWith('9999')) {
        timestamps.push(new Date(r.businessThru).getTime());
      }
      if (!r.processingThru.startsWith('9999')) {
        timestamps.push(new Date(r.processingThru).getTime());
      }

      return timestamps.filter(t => !isNaN(t));
    });

    if (finiteTimestamps.length === 0) return null;

    const minBusiness = Math.min(...finiteTimestamps);
    const maxBusiness = Math.max(...finiteTimestamps);
    const minProcessing = Math.min(...finiteTimestamps);
    const maxProcessing = Math.max(...finiteTimestamps);

    // Add 10% padding
    const businessPadding = (maxBusiness - minBusiness) * 0.1;
    const processingPadding = (maxProcessing - minProcessing) * 0.1;

    return {
      business: {
        minTime: minBusiness - businessPadding,
        maxTime: maxBusiness + businessPadding
      },
      processing: {
        minTime: minProcessing - processingPadding,
        maxTime: maxProcessing + processingPadding
      }
    };
  }, [assignmentHistory, salaryHistory]);

  if (assignmentLoading || salaryLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (assignmentError || salaryError) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        Error loading bitemporal data
      </div>
    );
  }

  if (!timeRanges || (!assignmentHistory?.length && !salaryHistory?.length)) {
    return (
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-yellow-700">
        データなし
      </div>
    );
  }

  const data = viewMode === 'assignment' ? assignmentHistory : salaryHistory;
  if (!data || data.length === 0) {
    return (
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-yellow-700">
        {viewMode === 'assignment' ? '配属' : '給与'}履歴なし
      </div>
    );
  }

  const width = 800;
  const height = 600;
  const margin = { top: 40, right: 120, bottom: 60, left: 60 };
  const chartWidth = width - margin.left - margin.right;
  const chartHeight = height - margin.top - margin.bottom;

  // Scaling functions
  const scaleX = (timestamp: string) => {
    const time = new Date(timestamp).getTime();
    const { minTime, maxTime } = timeRanges.business;
    return margin.left + ((time - minTime) / (maxTime - minTime)) * chartWidth;
  };

  const scaleY = (timestamp: string) => {
    const time = new Date(timestamp).getTime();
    const { minTime, maxTime } = timeRanges.processing;
    // Y axis is inverted (top = recent, bottom = old)
    return margin.top + chartHeight - ((time - minTime) / (maxTime - minTime)) * chartHeight;
  };

  // Format date for axis labels
  const formatDate = (timestamp: number) => {
    return new Date(timestamp).toLocaleString('ja-JP', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  };

  // Color mapping for departments (assignments)
  const getDepartmentColor = (departmentId: number) => {
    const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
    return colors[departmentId % colors.length];
  };

  // Color mapping for salary ranges
  const getSalaryColor = (amount: number) => {
    if (amount < 60000) return '#94a3b8';
    if (amount < 80000) return '#3b82f6';
    if (amount < 100000) return '#10b981';
    return '#f59e0b';
  };

  return (
    <div className="card">
      <div className="flex justify-between items-center mb-4 pb-3 border-b border-gray-200">
        <h3 className="text-xl font-semibold text-gray-900">
          📊 2D バイテンポラルタイムライン
        </h3>
        <div className="flex gap-2">
          <button
            onClick={() => setViewMode('assignment')}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              viewMode === 'assignment'
                ? 'bg-primary-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            配属履歴
          </button>
          <button
            onClick={() => setViewMode('salary')}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              viewMode === 'salary'
                ? 'bg-primary-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            給与履歴
          </button>
        </div>
      </div>

      <div className="relative">
        <svg width={width} height={height} className="border border-gray-200 rounded">
          {/* Grid lines */}
          <g>
            {[0, 0.25, 0.5, 0.75, 1].map(ratio => {
              const x = margin.left + chartWidth * ratio;
              const y = margin.top + chartHeight * (1 - ratio);
              return (
                <g key={ratio}>
                  <line
                    x1={x}
                    y1={margin.top}
                    x2={x}
                    y2={margin.top + chartHeight}
                    stroke="#e5e7eb"
                    strokeWidth="1"
                  />
                  <line
                    x1={margin.left}
                    y1={y}
                    x2={margin.left + chartWidth}
                    y2={y}
                    stroke="#e5e7eb"
                    strokeWidth="1"
                  />
                </g>
              );
            })}
          </g>

          {/* Axis labels */}
          <text
            x={margin.left + chartWidth / 2}
            y={height - 10}
            textAnchor="middle"
            className="text-sm font-medium fill-gray-700"
          >
            ビジネス時間（Business Time）→
          </text>
          <text
            x={20}
            y={margin.top + chartHeight / 2}
            textAnchor="middle"
            transform={`rotate(-90 20 ${margin.top + chartHeight / 2})`}
            className="text-sm font-medium fill-gray-700"
          >
            処理時間（Processing Time）→
          </text>

          {/* Axis tick labels */}
          {[0, 0.5, 1].map(ratio => {
            const businessTime = timeRanges.business.minTime +
              (timeRanges.business.maxTime - timeRanges.business.minTime) * ratio;
            const processingTime = timeRanges.processing.minTime +
              (timeRanges.processing.maxTime - timeRanges.processing.minTime) * ratio;

            return (
              <g key={ratio}>
                <text
                  x={margin.left + chartWidth * ratio}
                  y={margin.top + chartHeight + 20}
                  textAnchor="middle"
                  className="text-xs fill-gray-600"
                >
                  {formatDate(businessTime)}
                </text>
                <text
                  x={margin.left - 10}
                  y={margin.top + chartHeight * (1 - ratio) + 4}
                  textAnchor="end"
                  className="text-xs fill-gray-600"
                >
                  {formatDate(processingTime)}
                </text>
              </g>
            );
          })}

          {/* Data rectangles */}
          {data.map((record, index) => {
            const x1 = scaleX(record.businessFrom);
            const x2 = record.businessThru.startsWith('9999')
              ? margin.left + chartWidth
              : scaleX(record.businessThru);
            const y1 = scaleY(record.processingFrom);
            const y2 = record.processingThru.startsWith('9999')
              ? margin.top
              : scaleY(record.processingThru);

            const isInvalid = !record.processingThru.startsWith('9999');
            const isHovered = hoveredRecord?.data === record;

            const color = viewMode === 'assignment'
              ? getDepartmentColor((record as EmployeeAssignment).departmentId)
              : getSalaryColor((record as Salary).amount);

            // Gray out invalid records
            const fillColor = isInvalid ? '#d1d5db' : color;
            const strokeColor = isInvalid ? '#9ca3af' : (isHovered ? '#1f2937' : color);
            const opacity = isInvalid ? 0.4 : (isHovered ? 0.9 : 0.6);

            return (
              <rect
                key={index}
                x={Math.min(x1, x2)}
                y={Math.min(y1, y2)}
                width={Math.abs(x2 - x1)}
                height={Math.abs(y2 - y1)}
                fill={fillColor}
                fillOpacity={opacity}
                stroke={strokeColor}
                strokeWidth={isHovered ? 2 : 1}
                onMouseEnter={() => setHoveredRecord({ type: viewMode, data: record })}
                onMouseLeave={() => setHoveredRecord(null)}
                className="cursor-pointer transition-all"
              />
            );
          })}
        </svg>

        {/* Tooltip */}
        {hoveredRecord && (
          <div className="absolute top-4 right-4 bg-white border border-gray-300 rounded-lg shadow-lg p-4 max-w-xs z-10">
            {hoveredRecord.type === 'assignment' ? (
              <div className="space-y-2">
                <div className="font-semibold text-gray-900">
                  {departments?.find(d => d.id === (hoveredRecord.data as EmployeeAssignment).departmentId)?.name}
                </div>
                <div className="text-sm text-gray-600">
                  {positions?.find(p => p.id === (hoveredRecord.data as EmployeeAssignment).positionId)?.name}
                </div>
                <div className="text-xs text-gray-500 pt-2 border-t">
                  <div>ビジネス: {new Date(hoveredRecord.data.businessFrom).toLocaleDateString('ja-JP')} 〜 {hoveredRecord.data.businessThru.startsWith('9999') ? '∞' : new Date(hoveredRecord.data.businessThru).toLocaleDateString('ja-JP')}</div>
                  <div>処理: {new Date(hoveredRecord.data.processingFrom).toLocaleDateString('ja-JP')} 〜 {hoveredRecord.data.processingThru.startsWith('9999') ? '∞' : new Date(hoveredRecord.data.processingThru).toLocaleDateString('ja-JP')}</div>
                  <div className="mt-1">更新者: {hoveredRecord.data.updatedBy}</div>
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                <div className="font-semibold text-primary-600 text-lg">
                  {(hoveredRecord.data as Salary).currency} {(hoveredRecord.data as Salary).amount.toLocaleString()}
                </div>
                <div className="text-xs text-gray-500 pt-2 border-t">
                  <div>ビジネス: {new Date(hoveredRecord.data.businessFrom).toLocaleDateString('ja-JP')} 〜 {hoveredRecord.data.businessThru.startsWith('9999') ? '∞' : new Date(hoveredRecord.data.businessThru).toLocaleDateString('ja-JP')}</div>
                  <div>処理: {new Date(hoveredRecord.data.processingFrom).toLocaleDateString('ja-JP')} 〜 {hoveredRecord.data.processingThru.startsWith('9999') ? '∞' : new Date(hoveredRecord.data.processingThru).toLocaleDateString('ja-JP')}</div>
                  <div className="mt-1">更新者: {hoveredRecord.data.updatedBy}</div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Legend */}
      <div className="mt-4 p-4 bg-gray-50 rounded-lg">
        <h4 className="text-sm font-semibold text-gray-900 mb-2">凡例</h4>
        {viewMode === 'assignment' ? (
          <div className="grid grid-cols-2 gap-2 text-xs">
            {departments?.slice(0, 6).map((dept, index) => (
              <div key={dept.id} className="flex items-center gap-2">
                <div
                  className="w-4 h-4 rounded"
                  style={{ backgroundColor: getDepartmentColor(dept.id) }}
                />
                <span>{dept.name}</span>
              </div>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded" style={{ backgroundColor: '#94a3b8' }} />
              <span>&lt; $60,000</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded" style={{ backgroundColor: '#3b82f6' }} />
              <span>$60,000 - $80,000</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded" style={{ backgroundColor: '#10b981' }} />
              <span>$80,000 - $100,000</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded" style={{ backgroundColor: '#f59e0b' }} />
              <span>&gt;= $100,000</span>
            </div>
          </div>
        )}
      </div>

      {/* Explanation */}
      <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <h4 className="text-sm font-semibold text-blue-900 mb-2">📖 2D タイムラインの見方</h4>
        <div className="text-sm text-blue-800 space-y-1">
          <p>• 各矩形が1つのレコードを表します</p>
          <p>• <strong>横幅</strong>: ビジネス有効期間（その事実がいつ有効だったか）</p>
          <p>• <strong>縦幅</strong>: 処理時刻期間（システムがその情報をいつ記録していたか）</p>
          <p>• 隙間や重複がないことで、データの完全性を視覚的に確認できます</p>
        </div>
      </div>
    </div>
  );
}
