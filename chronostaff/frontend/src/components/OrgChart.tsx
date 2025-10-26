import { useDepartments, useEmployees, useAssignments, usePositions } from '../hooks/useAPI';

export default function OrgChart() {
  const { data: departments, isLoading: deptLoading } = useDepartments();
  const { data: employees, isLoading: empLoading } = useEmployees();
  const { data: assignments, isLoading: assignLoading } = useAssignments();
  const { data: positions } = usePositions();

  if (deptLoading || empLoading || assignLoading) return <div>Loading...</div>;
  if (!departments || !employees || !assignments) return null;

  // Build org chart data
  const rootDepts = departments.filter(d => !d.parentDepartmentId);

  return (
    <div>
      <h2>組織図</h2>
      <div style={{ marginTop: '20px' }}>
        {rootDepts.map(dept => (
          <DeptNode
            key={dept.id}
            dept={dept}
            allDepts={departments}
            employees={employees}
            assignments={assignments}
            positions={positions || []}
          />
        ))}
      </div>
    </div>
  );
}

function DeptNode({
  dept,
  allDepts,
  employees,
  assignments,
  positions,
  level = 0
}: any) {
  const childDepts = allDepts.filter((d: any) => d.parentDepartmentId === dept.id);
  const deptEmployees = assignments
    .filter((a: any) => a.departmentId === dept.id)
    .map((a: any) => {
      const emp = employees.find((e: any) => e.id === a.employeeId);
      const pos = positions.find((p: any) => p.id === a.positionId);
      return { ...emp, position: pos };
    });

  return (
    <div style={{ marginLeft: level * 40 }}>
      <div style={{
        padding: '15px',
        margin: '10px 0',
        backgroundColor: level === 0 ? '#1976d2' : '#42a5f5',
        color: 'white',
        borderRadius: '8px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
      }}>
        <h3 style={{ margin: '0 0 10px 0' }}>{dept.name}</h3>
        <div style={{ fontSize: '0.9em', opacity: 0.9 }}>
          従業員数: {deptEmployees.length}名
        </div>

        {deptEmployees.length > 0 && (
          <div style={{
            marginTop: '10px',
            padding: '10px',
            backgroundColor: 'rgba(255,255,255,0.1)',
            borderRadius: '4px'
          }}>
            {deptEmployees.map((emp: any) => (
              <div key={emp.id} style={{ padding: '4px 0' }}>
                {emp.name} ({emp.position?.name || '-'})
              </div>
            ))}
          </div>
        )}
      </div>

      {childDepts.map((child: any) => (
        <DeptNode
          key={child.id}
          dept={child}
          allDepts={allDepts}
          employees={employees}
          assignments={assignments}
          positions={positions}
          level={level + 1}
        />
      ))}
    </div>
  );
}
