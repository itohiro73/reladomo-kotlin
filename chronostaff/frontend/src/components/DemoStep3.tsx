import DemoGuideCard from './DemoGuideCard';
import OrgChart from './OrgChart';

export default function DemoStep3() {
  return (
    <DemoGuideCard
      step={3}
      totalSteps={5}
      title="Step 3: 組織図で現在の状態を確認"
      description="作成した組織構造を視覚的に確認しましょう。組織図は現在の時点（AsOf今日）での組織状態を表示します。"
      objectives={[
        "組織図で部署構造を確認",
        "各部署の従業員配置を確認",
        "役職レベルの階層を理解",
        "次のステップで未来の変更を登録する準備"
      ]}
      nextStep="/demo/step4"
      prevStep="/demo/step2"
    >
      <OrgChart />
    </DemoGuideCard>
  );
}
