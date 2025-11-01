import DemoGuideCard from './DemoGuideCard';
import EmployeeList from './EmployeeList';
import ScheduledChangesView from './ScheduledChangesView';

export default function DemoStep4() {
  return (
    <DemoGuideCard
      step={4}
      totalSteps={5}
      title="Step 4: 未来の変更を登録"
      description="バイテンポラルデータの真価を発揮する瞬間です。未来の人事異動や昇給を今日登録できます。これにより、計画的な人事管理が可能になります。"
      objectives={[
        "下の従業員一覧から登録した従業員名をクリック",
        "詳細画面で「人事異動」ボタンをクリック",
        "実効日: 来月1日、部署: 営業部、役職: 部長 で登録",
        "詳細画面に戻り「給与調整」ボタンをクリック",
        "実効日: 来月1日、年俸: 8000000 で登録",
        "予定されている変更（下部）で未来のイベントを確認",
        "→ バイテンポラルならではの「未来の計画」機能を体感"
      ]}
      nextStep="/demo/step5"
      prevStep="/demo/step3"
    >
      <div className="space-y-6">
        <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4 rounded">
          <p className="text-yellow-800 font-medium">
            💡 <strong>ポイント:</strong> 従業員名をクリック → 詳細画面で「人事異動」「給与調整」ボタンから未来の日付を指定
          </p>
        </div>

        <EmployeeList />

        <div className="bg-blue-50 rounded-lg p-6">
          <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
            <span>🔮</span>
            予定されている変更
          </h3>
          <ScheduledChangesView />
        </div>
      </div>
    </DemoGuideCard>
  );
}
