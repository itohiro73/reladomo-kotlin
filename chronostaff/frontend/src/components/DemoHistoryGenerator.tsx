import { useState, useMemo } from 'react';
import { useEmployees, usePositions, useDepartments, useOrganizationSnapshot } from '../hooks/useAPI';
import { useCompany } from '../contexts/CompanyContext';
import { transferEmployee, adjustSalary } from '../api/client';
import type { TransferRequestDto, SalaryAdjustmentRequestDto } from '../types';

export default function DemoHistoryGenerator() {
  const { selectedCompanyId } = useCompany();
  const { data: employees, mutate: mutateEmployees } = useEmployees(selectedCompanyId);
  const { data: positions } = usePositions(selectedCompanyId);
  const { data: departments } = useDepartments(selectedCompanyId);

  // Get organization snapshot from 3 months ago to find current assignments
  const threeMonthsAgoDate = useMemo(() => {
    const date = new Date();
    date.setMonth(date.getMonth() - 3);
    date.setDate(1);
    return date.toISOString().split('T')[0];
  }, []);
  const { data: orgSnapshot } = useOrganizationSnapshot(threeMonthsAgoDate, selectedCompanyId);

  const [isGenerating, setIsGenerating] = useState(false);
  const [historyGenerated, setHistoryGenerated] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const generateHistory = async () => {
    setError(null);
    setIsGenerating(true);

    try {
      console.log('=== Starting history generation ===');
      console.log('Available employees:', employees);
      console.log('Organization snapshot (3 months ago):', orgSnapshot);

      // Find employees by name
      const tanaka = employees?.find(e => e.name === '田中花子');
      const sato = employees?.find(e => e.name === '佐藤一郎');

      console.log('Found Tanaka:', tanaka);
      console.log('Found Sato:', sato);

      if (!tanaka || !sato) {
        throw new Error('田中花子または佐藤一郎が見つかりません。先にStep 2で従業員を登録してください。');
      }

      if (!orgSnapshot) {
        throw new Error('組織スナップショットの取得に失敗しました。');
      }

      // Find current assignments from organization snapshot
      let tanakaDepartmentId: number | null = null;
      let tanakaPositionId: number | null = null;
      let satoDepartmentId: number | null = null;
      let satoPositionId: number | null = null;

      for (const dept of orgSnapshot.departments) {
        const tanakaEmp = dept.employees.find(e => e.id === tanaka.id);
        if (tanakaEmp) {
          tanakaDepartmentId = dept.id;
          tanakaPositionId = tanakaEmp.positionId;
        }
        const satoEmp = dept.employees.find(e => e.id === sato.id);
        if (satoEmp) {
          satoDepartmentId = dept.id;
          satoPositionId = satoEmp.positionId;
        }
      }

      console.log('Tanaka current department ID:', tanakaDepartmentId);
      console.log('Tanaka current position ID:', tanakaPositionId);
      console.log('Sato current department ID:', satoDepartmentId);
      console.log('Sato current position ID:', satoPositionId);

      if (tanakaDepartmentId === null || tanakaPositionId === null || satoDepartmentId === null || satoPositionId === null) {
        throw new Error('従業員の現在の配属情報が見つかりません。');
      }

      // Find positions and departments
      const managerPos = positions?.find(p => p.name.includes('マネージャー'));
      const salesDept = departments?.find(d => d.name.includes('営業'));

      console.log('Found manager position:', managerPos);
      console.log('Found sales department:', salesDept);

      if (!managerPos || !salesDept) {
        throw new Error('役職または部署が見つかりません');
      }

      // Calculate past dates
      const today = new Date();

      // 2 months ago - 佐藤一郎を開発部→営業部に異動
      const twoMonthsAgo = new Date(today);
      twoMonthsAgo.setMonth(twoMonthsAgo.getMonth() - 2);
      twoMonthsAgo.setDate(1);
      const twoMonthsAgoStr = twoMonthsAgo.toISOString().split('T')[0];

      // 1 month ago - 田中花子をマネージャーに昇進 & 佐藤一郎を昇給
      const oneMonthAgo = new Date(today);
      oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);
      oneMonthAgo.setDate(1);
      const oneMonthAgoStr = oneMonthAgo.toISOString().split('T')[0];

      // 1. 佐藤一郎を営業部に異動（2ヶ月前）
      console.log('=== Step 1: Transferring Sato to Sales ===');
      const satoTransfer: TransferRequestDto = {
        newDepartmentId: salesDept.id,
        newPositionId: satoPositionId, // Keep same position from snapshot
        effectiveDate: twoMonthsAgoStr,
        reason: 'デモ用履歴: 営業部への配置転換',
        updatedBy: 'demo-system',
      };
      console.log('Sato transfer request:', satoTransfer);
      try {
        await transferEmployee(sato.id, satoTransfer);
        console.log('Sato transfer successful');
      } catch (err) {
        console.error('Sato transfer failed:', err);
        throw new Error(`佐藤一郎の異動に失敗: ${err instanceof Error ? err.message : String(err)}`);
      }

      // 2. 田中花子をマネージャーに昇進（1ヶ月前）
      console.log('=== Step 2: Promoting Tanaka to Manager ===');
      const tanakaPromotion: TransferRequestDto = {
        newDepartmentId: tanakaDepartmentId, // Keep same department from snapshot
        newPositionId: managerPos.id,
        effectiveDate: oneMonthAgoStr,
        reason: 'デモ用履歴: マネージャーへの昇進',
        updatedBy: 'demo-system',
      };
      console.log('Tanaka promotion request:', tanakaPromotion);
      try {
        await transferEmployee(tanaka.id, tanakaPromotion);
        console.log('Tanaka promotion successful');
      } catch (err) {
        console.error('Tanaka promotion failed:', err);
        throw new Error(`田中花子の昇進に失敗: ${err instanceof Error ? err.message : String(err)}`);
      }

      // 3. 佐藤一郎を昇給（1ヶ月前）
      console.log('=== Step 3: Adjusting Sato salary ===');
      const satoSalaryAdjustment: SalaryAdjustmentRequestDto = {
        newAmount: 5200000, // 4,800,000 → 5,200,000 (about 8% raise)
        currency: 'JPY',
        effectiveDate: oneMonthAgoStr,
        reason: 'デモ用履歴: 異動に伴う昇給',
        updatedBy: 'demo-system',
      };
      console.log('Sato salary adjustment request:', satoSalaryAdjustment);
      try {
        await adjustSalary(sato.id, satoSalaryAdjustment);
        console.log('Sato salary adjustment successful');
      } catch (err) {
        console.error('Sato salary adjustment failed:', err);
        throw new Error(`佐藤一郎の昇給に失敗: ${err instanceof Error ? err.message : String(err)}`);
      }

      // Refresh employee list
      await mutateEmployees();

      console.log('=== History generation completed successfully ===');
      setHistoryGenerated(true);
    } catch (err) {
      console.error('History generation error:', err);
      const errorMessage = err instanceof Error ? err.message : '履歴の生成に失敗しました';
      setError(errorMessage);
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="bg-gradient-to-r from-purple-50 to-pink-50 rounded-lg p-6 border-2 border-purple-200">
      <h3 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
        <span>📜</span> 組織の変遷を記録
      </h3>
      <p className="text-gray-700 mb-4">
        過去3ヶ月間の組織変遷（異動・昇進・昇給）を自動で生成します。
        これによりバイテンポラルデータの履歴追跡機能を体験できます。
      </p>

      <div className="bg-white rounded-lg p-4 mb-4 text-sm text-gray-700">
        <p className="font-semibold mb-2">生成される履歴:</p>
        <ul className="list-disc list-inside space-y-1">
          <li>2ヶ月前: 佐藤一郎が開発部 → 営業部に異動</li>
          <li>1ヶ月前: 田中花子がメンバー → マネージャーに昇進</li>
          <li>1ヶ月前: 佐藤一郎が昇給（480万 → 520万）</li>
        </ul>
      </div>

      {!historyGenerated ? (
        <button
          onClick={generateHistory}
          disabled={isGenerating}
          className="w-full bg-purple-600 text-white py-3 px-6 rounded-lg hover:bg-purple-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors font-semibold flex items-center justify-center gap-2"
        >
          <span>🎬</span>
          <span>{isGenerating ? '生成中...' : '3ヶ月分の変遷を生成'}</span>
        </button>
      ) : (
        <div className="bg-green-50 border-2 border-green-500 rounded-lg p-4">
          <div className="flex items-center gap-3 text-green-800">
            <span className="text-2xl">✅</span>
            <div>
              <p className="font-semibold">履歴生成完了！</p>
              <p className="text-sm">過去3ヶ月間の組織変遷が記録されました。下の組織図で確認してください。</p>
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
  );
}
