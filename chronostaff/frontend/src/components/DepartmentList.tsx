import { useDepartments } from '../hooks/useAPI';
import { formatDate } from '../utils/date';

export default function DepartmentList() {
  const { data: departments, error, isLoading } = useDepartments();

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;
  if (!departments) return null;

  // Build department hierarchy
  const rootDepts = departments.filter(d => !d.parentDepartmentId);
  const childDepts = departments.filter(d => d.parentDepartmentId);

  return (
    <div>
      <h2>部署一覧</h2>

      <div style={{ marginTop: '20px' }}>
        {rootDepts.map(dept => (
          <DepartmentNode key={dept.id} dept={dept} children={childDepts} allDepts={departments} />
        ))}
      </div>
    </div>
  );
}

function DepartmentNode({
  dept,
  children,
  allDepts,
  level = 0
}: {
  dept: any;
  children: any[];
  allDepts: any[];
  level?: number;
}) {
  const myChildren = children.filter(c => c.parentDepartmentId === dept.id);

  return (
    <div style={{ marginLeft: level * 30 }}>
      <div style={{
        padding: '12px',
        margin: '8px 0',
        backgroundColor: level === 0 ? '#e3f2fd' : '#f5f5f5',
        border: '1px solid #ccc',
        borderRadius: '4px'
      }}>
        <h3 style={{ margin: '0 0 8px 0' }}>{dept.name}</h3>
        <p style={{ margin: '4px 0', color: '#666', fontSize: '0.9em' }}>
          {dept.description}
        </p>
        <p style={{ margin: '4px 0', fontSize: '0.85em', color: '#999' }}>
          Processing: {formatDate(dept.processingFrom)} ~ {formatDate(dept.processingThru)}
        </p>
      </div>

      {myChildren.map(child => (
        <DepartmentNode
          key={child.id}
          dept={child}
          children={children}
          allDepts={allDepts}
          level={level + 1}
        />
      ))}
    </div>
  );
}
