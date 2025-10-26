import { useDepartments } from '../hooks/useAPI';
import { formatDate } from '../utils/date';

export default function DepartmentList() {
  const { data: departments, error, isLoading } = useDepartments();

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        Error loading departments: {error.message}
      </div>
    );
  }

  if (!departments) return null;

  // Build department hierarchy
  const rootDepts = departments.filter(d => !d.parentDepartmentId);
  const childDepts = departments.filter(d => d.parentDepartmentId);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold text-gray-900">部署一覧</h2>
        <div className="flex items-center gap-2 text-sm text-gray-600">
          <span className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full font-medium">
            {departments.length}部署
          </span>
        </div>
      </div>

      <div className="space-y-3">
        {rootDepts.map(dept => (
          <DepartmentNode key={dept.id} dept={dept} children={childDepts} allDepts={departments} />
        ))}
      </div>
    </div>
  );
}

interface Department {
  id: number;
  name: string;
  description: string;
  parentDepartmentId?: number;
  processingFrom: string;
  processingThru: string;
}

function DepartmentNode({
  dept,
  children,
  allDepts,
  level = 0
}: {
  dept: Department;
  children: Department[];
  allDepts: Department[];
  level?: number;
}) {
  const myChildren = children.filter(c => c.parentDepartmentId === dept.id);
  const hasChildren = myChildren.length > 0;

  const levelColors = [
    'from-primary-50 to-primary-100 border-primary-200',
    'from-blue-50 to-blue-100 border-blue-200',
    'from-indigo-50 to-indigo-100 border-indigo-200',
  ];
  const colorClass = levelColors[Math.min(level, levelColors.length - 1)];

  const levelIcons = ['🏢', '📁', '📂'];
  const icon = levelIcons[Math.min(level, levelIcons.length - 1)];

  return (
    <div className={level > 0 ? 'ml-6 pl-4 border-l-2 border-gray-300' : ''}>
      <div className={`bg-gradient-to-r ${colorClass} border rounded-lg p-4 shadow-sm hover:shadow-md transition-all duration-200`}>
        <div className="flex items-start gap-3">
          <span className="text-2xl">{icon}</span>
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-2">
              <h3 className="text-lg font-semibold text-gray-900">{dept.name}</h3>
              {hasChildren && (
                <span className="text-xs bg-white px-2 py-1 rounded-full text-gray-600 font-medium">
                  {myChildren.length}部署
                </span>
              )}
            </div>
            <p className="text-sm text-gray-700 mb-3">{dept.description}</p>
            <div className="flex items-center gap-4 text-xs text-gray-500">
              <div className="flex items-center gap-1">
                <span>⏱️</span>
                <span className="font-mono">
                  {formatDate(dept.processingFrom)} ~ {formatDate(dept.processingThru)}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {hasChildren && (
        <div className="mt-3 space-y-3">
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
      )}
    </div>
  );
}
