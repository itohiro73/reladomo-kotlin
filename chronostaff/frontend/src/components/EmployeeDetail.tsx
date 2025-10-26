import { useParams, Link } from 'react-router-dom';
import {
  useEmployee,
  useAssignmentsByEmployee,
  useSalariesByEmployee,
  usePositions,
  useDepartments
} from '../hooks/useAPI';
import { formatDate, formatDateOnly } from '../utils/date';

export default function EmployeeDetail() {
  const { id } = useParams<{ id: string }>();
  const employeeId = id ? parseInt(id) : null;

  const { data: employee, error, isLoading } = useEmployee(employeeId);
  const { data: assignments } = useAssignmentsByEmployee(employeeId);
  const { data: salaries } = useSalariesByEmployee(employeeId);
  const { data: positions } = usePositions();
  const { data: departments } = useDepartments();

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;
  if (!employee) return <div>Employee not found</div>;

  const currentAssignment = assignments?.[0];
  const currentSalary = salaries?.[0];
  const position = currentAssignment && positions?.find(p => p.id === currentAssignment.positionId);
  const department = currentAssignment && departments?.find(d => d.id === currentAssignment.departmentId);

  return (
    <div>
      <Link to="/">← 従業員一覧に戻る</Link>

      <div style={{ marginTop: '20px' }}>
        <h2>{employee.name}</h2>

        <section style={sectionStyle}>
          <h3>基本情報</h3>
          <table>
            <tbody>
              <Row label="社員番号" value={employee.employeeNumber} />
              <Row label="メールアドレス" value={employee.email} />
              <Row label="入社日" value={formatDateOnly(employee.hireDate)} />
            </tbody>
          </table>
        </section>

        <section style={sectionStyle}>
          <h3>現在の配属・役職</h3>
          {currentAssignment ? (
            <table>
              <tbody>
                <Row label="部署" value={department?.name || '-'} />
                <Row label="役職" value={position ? `${position.name} (Level ${position.level})` : '-'} />
                <Row label="配属開始日" value={formatDateOnly(currentAssignment.businessFrom)} />
                <Row label="更新者" value={currentAssignment.updatedBy} />
              </tbody>
            </table>
          ) : (
            <p>配属情報なし</p>
          )}
        </section>

        <section style={sectionStyle}>
          <h3>現在の給与情報</h3>
          {currentSalary ? (
            <table>
              <tbody>
                <Row
                  label="給与"
                  value={`${currentSalary.currency} ${currentSalary.amount.toLocaleString()}`}
                />
                <Row label="適用開始日" value={formatDateOnly(currentSalary.businessFrom)} />
                <Row label="更新者" value={currentSalary.updatedBy} />
              </tbody>
            </table>
          ) : (
            <p>給与情報なし</p>
          )}
        </section>

        <section style={sectionStyle}>
          <h3>テンポラル情報（デバッグ用）</h3>
          <div style={{ fontSize: '0.9em', color: '#666' }}>
            <p>Processing From: {formatDate(employee.processingFrom)}</p>
            <p>Processing Thru: {formatDate(employee.processingThru)}</p>
          </div>
        </section>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <tr>
      <td style={{ padding: '8px', fontWeight: 'bold', width: '150px' }}>{label}:</td>
      <td style={{ padding: '8px' }}>{value}</td>
    </tr>
  );
}

const sectionStyle: React.CSSProperties = {
  marginTop: '30px',
  padding: '20px',
  border: '1px solid #ddd',
  borderRadius: '4px',
  backgroundColor: '#f9f9f9'
};
