import DemoGuideCard from './DemoGuideCard';
import EmployeeAddForm from './EmployeeAddForm';

export default function DemoStep2() {
  return (
    <DemoGuideCard
      step={2}
      totalSteps={5}
      title="Step 2: 従業員の雇用"
      description="最初の従業員を雇用しましょう。配属先と初任給を設定します。入社日をベースに、バイテンポラルレコードが自動生成されます。簡単な入力でOKです。"
      objectives={[
        "社員番号: EMP001（など適当に）",
        "氏名: 山田太郎（など適当に）",
        "メール: yamada@example.com（など適当に）",
        "入社日: 今日の日付",
        "配属: 開発部・マネージャー",
        "年俸: 6000000",
        "登録者: hr@example.com",
        "「従業員を登録」をクリック"
      ]}
      nextStep="/demo/step3"
      prevStep="/demo/step1"
    >
      <EmployeeAddForm />
    </DemoGuideCard>
  );
}
