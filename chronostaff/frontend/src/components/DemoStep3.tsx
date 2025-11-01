import DemoGuideCard from './DemoGuideCard';
import OrgChart from './OrgChart';
import DemoHistoryGenerator from './DemoHistoryGenerator';

export default function DemoStep3() {
  return (
    <DemoGuideCard
      step={3}
      totalSteps={5}
      title="Step 3: 組織の変遷を記録"
      description="過去3ヶ月間の組織変遷（異動・昇進・昇給）を生成して、バイテンポラルデータの履歴管理機能を体験しましょう。"
      objectives={[
        "「3ヶ月分の変遷を生成」ボタンをクリック",
        "過去の異動・昇進・昇給が自動で記録されます",
        "組織図で現在の組織状態を確認",
        "従業員をクリックして過去の履歴を確認",
        "→ バイテンポラルデータによる完全な履歴追跡を実感"
      ]}
      nextStep="/demo/step4"
      prevStep="/demo/step2"
    >
      <div className="space-y-6">
        <DemoHistoryGenerator />

        <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <span>📊</span> 現在の組織図
          </h3>
          <OrgChart />
        </div>
      </div>
    </DemoGuideCard>
  );
}
