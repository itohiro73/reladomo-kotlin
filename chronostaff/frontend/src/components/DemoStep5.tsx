import DemoGuideCard from './DemoGuideCard';
import EmployeeList from './EmployeeList';

export default function DemoStep5() {
  return (
    <DemoGuideCard
      step={5}
      totalSteps={5}
      title="Step 5: タイムトラベル - 組織の歴史を旅する"
      description="バイテンポラルデータの最も強力な機能「時間旅行」を体験しましょう。創業時から現在、そして未来まで、組織の変遷を自由に行き来できます。過去の任意の時点での組織状態を正確に再現できるのがバイテンポラルデータの真価です。"
      objectives={[
        "下の従業員一覧から従業員（田中花子 or 佐藤一郎）をクリック",
        "詳細画面の「履歴タイムライン」タブで3ヶ月間の変更履歴を確認",
        "「2Dタイムライン」タブでBusiness Time × Processing Timeを確認",
        "各レコードの色分けを観察（過去=青、現在=緑、未来=紫）",
        "→ 完全な監査証跡：誰が、いつ、何を変更したか全て追跡可能！"
      ]}
      prevStep="/demo/step4"
    >
      <div className="space-y-6">
        <div className="bg-gradient-to-r from-purple-50 to-pink-50 border-l-4 border-purple-500 p-6 rounded-lg">
          <h3 className="text-lg font-bold text-purple-900 mb-3 flex items-center gap-2">
            <span>🎓</span>
            バイテンポラルデータの2つの時間軸
          </h3>
          <div className="space-y-2 text-purple-800">
            <p><strong>Business Time (有効期間):</strong> データが実世界で有効だった期間 → 「いつから、いつまで有効だったか」</p>
            <p><strong>Processing Time (記録時間):</strong> システムがそのデータを記録していた期間 → 「いつ記録されて、いつ修正されたか」</p>
            <p className="mt-3 text-sm bg-white p-3 rounded">
              💡 この2つの時間軸により、<strong>「過去のある時点で、システムは何を知っていたか」</strong>を完全に再現できます
            </p>
          </div>
        </div>

        <div className="bg-blue-50 border-l-4 border-blue-500 p-4 rounded">
          <p className="text-blue-900 font-medium">
            👉 <strong>操作方法:</strong> 従業員をクリック → 詳細画面で「履歴タイムライン」と「2Dタイムライン」タブを確認
          </p>
        </div>

        <EmployeeList />

        <div className="bg-green-50 rounded-lg p-6 mt-8">
          <h3 className="text-lg font-bold text-green-900 mb-3 flex items-center gap-2">
            <span>🎉</span>
            デモ完了！
          </h3>
          <p className="text-green-800 mb-4">
            お疲れ様でした！ChronoStaffのバイテンポラルデータ管理機能を体験いただきました。
          </p>
          <div className="bg-white p-4 rounded space-y-2 text-gray-700">
            <p><strong>✅ 学んだこと:</strong></p>
            <ul className="list-disc list-inside space-y-1 ml-4">
              <li>未来の変更を計画できる（人事異動、昇給）</li>
              <li>過去の任意の時点の組織状態を確認できる</li>
              <li>データ修正の完全な履歴を追跡できる</li>
              <li>Business TimeとProcessing Timeの2軸管理</li>
            </ul>
          </div>
        </div>
      </div>
    </DemoGuideCard>
  );
}
