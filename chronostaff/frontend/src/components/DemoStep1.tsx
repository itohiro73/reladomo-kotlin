import DemoGuideCard from './DemoGuideCard';
import InitialSetupWizard from './InitialSetupWizard';

export default function DemoStep1() {
  return (
    <DemoGuideCard
      step={1}
      totalSteps={5}
      title="Step 1: 組織のセットアップ"
      description="ChronoStaffの旅を始めましょう。まずは会社の基本情報、役職、部署を設定します。ここで指定する「開始日」がバイテンポラルデータの起点となります。デフォルトで入力されているサンプルデータをそのまま使用できます。"
      objectives={[
        "会社名を入力（デフォルト値そのままでOK）",
        "役職を確認（社長、部長、マネージャー、メンバー）",
        "部署を確認（経営企画部、開発部、営業部）",
        "開始日を指定 → バイテンポラルの基準日（今日の日付でOK）",
        "「組織をセットアップ」ボタンをクリック"
      ]}
      nextStep="/demo/step2"
    >
      <InitialSetupWizard />
    </DemoGuideCard>
  );
}
