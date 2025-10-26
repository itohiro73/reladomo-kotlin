import { Link } from 'react-router-dom';
import { useEmployees, useAssignments, useSalaries, usePositions, useDepartments } from '../hooks/useAPI';
import { formatDateOnly } from '../utils/date';
import type { EmployeeWithDetails } from '../types';

export default function EmployeeList() {
  const { data: employees, error: empError, isLoading: empLoading } = useEmployees();
  const { data: assignments } = useAssignments();
  const { data: salaries } = useSalaries();
  const { data: positions } = usePositions();
  const { data: departments } = useDepartments();

  if (empLoading) return <div>Loading...</div>;
  if (empError) return <div>Error loading employees: {empError.message}</div>;
  if (!employees) return null;

  // Enrich employees with their current assignment and salary
  const enrichedEmployees: EmployeeWithDetails[] = employees.map(emp => {
    const assignment = assignments?.find(a => a.employeeId === emp.id);
    const salary = salaries?.find(s => s.employeeId === emp.id);
    const position = assignment && positions?.find(p => p.id === assignment.positionId);
    const department = assignment && departments?.find(d => d.id === assignment.departmentId);

    return {
      ...emp,
      currentAssignment: assignment,
      currentSalary: salary,
      position,
      department
    };
  });

  return (
    <div>
      <h2>従業員一覧</h2>
      <table style={{
        width: '100%',
        borderCollapse: 'collapse',
        marginTop: '20px'
      }}>
        <thead>
          <tr style={{ backgroundColor: '#f0f0f0' }}>
            <th style={cellStyle}>社員番号</th>
            <th style={cellStyle}>氏名</th>
            <th style={cellStyle}>部署</th>
            <th style={cellStyle}>役職</th>
            <th style={cellStyle}>給与</th>
            <th style={cellStyle}>入社日</th>
            <th style={cellStyle}>詳細</th>
          </tr>
        </thead>
        <tbody>
          {enrichedEmployees.map(emp => (
            <tr key={emp.id} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={cellStyle}>{emp.employeeNumber}</td>
              <td style={cellStyle}>{emp.name}</td>
              <td style={cellStyle}>{emp.department?.name || '-'}</td>
              <td style={cellStyle}>
                {emp.position ? `${emp.position.name} (Level ${emp.position.level})` : '-'}
              </td>
              <td style={cellStyle}>
                {emp.currentSalary
                  ? `${emp.currentSalary.currency} ${emp.currentSalary.amount.toLocaleString()}`
                  : '-'}
              </td>
              <td style={cellStyle}>{formatDateOnly(emp.hireDate)}</td>
              <td style={cellStyle}>
                <Link to={`/employees/${emp.id}`}>詳細 →</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const cellStyle: React.CSSProperties = {
  padding: '12px 8px',
  textAlign: 'left'
};
