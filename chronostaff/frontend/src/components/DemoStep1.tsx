import DemoGuideCard from './DemoGuideCard';
import InitialSetupWizard from './InitialSetupWizard';

export default function DemoStep1() {
  return (
    <DemoGuideCard
      step={1}
      totalSteps={5}
      title="Step 1: 創業 - 組織のセットアップ"
      description="スタートアップ企業の創業をシミュレートします（デモのため開始日は3ヶ月前に設定）。会社の基本情報、役職、部署を設定しましょう。ここで指定する「開始日」がバイテンポラルデータの起点となり、以降すべての変更がこの時点を基準に記録されます。"
      objectives={[
        "「デモデータで自動入力」ボタンをクリック",
        "内容を確認（会社名、役職、部署、開始日=3ヶ月前）",
        "「組織をセットアップ」ボタンをクリック",
        "→ 創業完了！バイテンポラルデータの起点が設定されました（3ヶ月前から）"
      ]}
      nextStep="/demo/step2"
    >
      <InitialSetupWizard redirectPath="/demo/step2" />
    </DemoGuideCard>
  );
}
