import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DemoGuideCard from './DemoGuideCard';
import EmployeeAddForm from './EmployeeAddForm';
import { createEmployee } from '../api/client';
import { useCompany } from '../contexts/CompanyContext';
import { usePositions, useDepartments } from '../hooks/useAPI';
import type { EmployeeCreateDto, InitialAssignmentDto, InitialSalaryDto } from '../types';

export default function DemoStep2() {
  const navigate = useNavigate();
  const { selectedCompanyId } = useCompany();
  const { data: positions } = usePositions(selectedCompanyId);
  const { data: departments } = useDepartments(selectedCompanyId);
  const [firstEmployeeRegistered, setFirstEmployeeRegistered] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);
  const [additionalMembersRegistered, setAdditionalMembersRegistered] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFirstEmployeeSuccess = () => {
    setFirstEmployeeRegistered(true);
  };

  const registerAdditionalMembers = async () => {
    setError(null);
    setIsRegistering(true);

    try {
      // Set hire date to 3 months ago so we can generate historical data
      const threeMonthsAgo = new Date();
      threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
      threeMonthsAgo.setDate(1);
      const threeMonthsAgoStr = threeMonthsAgo.toISOString().split('T')[0];

      // Find departments and positions
      const salesDept = departments?.find(d => d.name.includes('営業'));
      const devDept = departments?.find(d => d.name.includes('開発'));
      const memberPos = positions?.find(p => p.name.includes('メンバー'));

      if (!salesDept || !devDept || !memberPos || !selectedCompanyId) {
        throw new Error('部署または役職が見つかりません');
      }

      // Register 田中花子 (営業部・メンバー) - hired 3 months ago
      const tanaka: EmployeeCreateDto = {
        companyId: selectedCompanyId,
        employeeNumber: 'EMP002',
        name: '田中花子',
        email: 'tanaka@example.com',
        hireDate: threeMonthsAgoStr,
        assignment: {
          departmentId: salesDept.id,
          positionId: memberPos.id,
          effectiveDate: threeMonthsAgoStr,
          updatedBy: 'hr@example.com',
        },
        salary: {
          amount: 5000000,
          currency: 'JPY',
          effectiveDate: threeMonthsAgoStr,
          updatedBy: 'hr@example.com',
        },
      };

      // Register 佐藤一郎 (開発部・メンバー) - hired 3 months ago
      const sato: EmployeeCreateDto = {
        companyId: selectedCompanyId,
        employeeNumber: 'EMP003',
        name: '佐藤一郎',
        email: 'sato@example.com',
        hireDate: threeMonthsAgoStr,
        assignment: {
          departmentId: devDept.id,
          positionId: memberPos.id,
          effectiveDate: threeMonthsAgoStr,
          updatedBy: 'hr@example.com',
        },
        salary: {
          amount: 4800000,
          currency: 'JPY',
          effectiveDate: threeMonthsAgoStr,
          updatedBy: 'hr@example.com',
        },
      };

      await createEmployee(tanaka);
      await createEmployee(sato);

      setAdditionalMembersRegistered(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '追加メンバーの登録に失敗しました');
    } finally {
      setIsRegistering(false);
    }
  };

  return (
    <DemoGuideCard
      step={2}
      totalSteps={5}
      title="Step 2: 創業メンバーの採用"
      description="創業時の初期メンバーを雇用します（デモのため入社日は3ヶ月前に設定）。最初の従業員（山田太郎）を登録した後、さらに2名の創業メンバーを追加します。各メンバーの入社日から、バイテンポラルレコードが自動生成されます。"
      objectives={[
        "【パート1】「サンプルデータで自動入力」→「登録」で山田太郎を雇用",
        "【パート2】「創業メンバー2名を追加登録」ボタンをクリック",
        "田中花子（営業部・メンバー）、佐藤一郎（開発部・メンバー）が登録されます",
        "→ 創業チーム3名のバイテンポラルレコードが作成されました（3ヶ月前から）"
      ]}
      nextStep="/demo/step3"
      prevStep="/demo/step1"
    >
      <div className="space-y-6">
        {/* Part 1: First employee */}
        <div>
          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
            <span>👤</span> パート1: 最初の従業員を雇用
          </h3>
          {!firstEmployeeRegistered ? (
            <EmployeeAddForm onSuccess={handleFirstEmployeeSuccess} />
          ) : (
            <div className="bg-green-50 border-2 border-green-500 rounded-lg p-4">
              <div className="flex items-center gap-3 text-green-800">
                <span className="text-2xl">✅</span>
                <div>
                  <p className="font-semibold">山田太郎の登録完了！</p>
                  <p className="text-sm">最初の従業員を雇用しました。次はパート2に進んでください。</p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Part 2: Additional members */}
        {firstEmployeeRegistered && (
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-6 border-2 border-blue-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
            <span>👥</span> パート2: 創業メンバーを追加
          </h3>
          <p className="text-gray-700 mb-4">
            さらに2名の創業メンバー（田中花子・佐藤一郎）を自動で登録します。
          </p>

          {!additionalMembersRegistered ? (
            <button
              onClick={registerAdditionalMembers}
              disabled={isRegistering}
              className="w-full bg-indigo-600 text-white py-3 px-6 rounded-lg hover:bg-indigo-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors font-semibold flex items-center justify-center gap-2"
            >
              <span>✨</span>
              <span>{isRegistering ? '登録中...' : '創業メンバー2名を追加登録'}</span>
            </button>
          ) : (
            <div className="bg-green-50 border-2 border-green-500 rounded-lg p-4">
              <div className="flex items-center gap-3 text-green-800">
                <span className="text-2xl">✅</span>
                <div>
                  <p className="font-semibold">追加メンバー登録完了！</p>
                  <p className="text-sm">田中花子（営業部・メンバー）、佐藤一郎（開発部・メンバー）を登録しました</p>
                </div>
              </div>
            </div>
          )}

          {error && (
            <div className="mt-4 bg-red-50 border-l-4 border-red-500 p-4 rounded">
              <p className="text-red-700">{error}</p>
            </div>
          )}
        </div>
        )}

        {/* Next step button */}
        {additionalMembersRegistered && (
          <div className="flex justify-end">
            <button
              onClick={() => navigate('/demo/step3')}
              className="bg-primary-600 text-white py-3 px-8 rounded-lg hover:bg-primary-700 transition-colors font-semibold flex items-center gap-2"
            >
              <span>次のステップへ →</span>
            </button>
          </div>
        )}
      </div>
    </DemoGuideCard>
  );
}
