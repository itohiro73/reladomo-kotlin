import { Link } from 'react-router-dom';
import { useDepartments, useEmployees, useAssignments, usePositions } from '../hooks/useAPI';
import type { Department, Employee, EmployeeAssignment, Position } from '../types';

interface EmployeeWithPosition extends Employee {
  position?: Position;
}

export default function OrgChart() {
  const { data: departments, error: deptError, isLoading: deptLoading } = useDepartments();
  const { data: employees, error: empError, isLoading: empLoading } = useEmployees();
  const { data: assignments, error: assignError, isLoading: assignLoading } = useAssignments();
  const { data: positions } = usePositions();

  if (deptLoading || empLoading || assignLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  const error = deptError || empError || assignError;
  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        Error loading organization chart: {error.message}
      </div>
    );
  }

  if (!departments || !employees || !assignments) return null;

  // Build org chart data
  const rootDepts = departments.filter(d => !d.parentDepartmentId);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold text-gray-900">組織図</h2>
        <div className="flex items-center gap-2 text-sm text-gray-600">
          <span className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full font-medium">
            {departments.length}部署
          </span>
          <span className="bg-green-100 text-green-800 px-3 py-1 rounded-full font-medium">
            {employees.length}名
          </span>
        </div>
      </div>

      <div className="space-y-4">
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
}: {
  dept: Department;
  allDepts: Department[];
  employees: Employee[];
  assignments: EmployeeAssignment[];
  positions: Position[];
  level?: number;
}) {
  const childDepts = allDepts.filter(d => d.parentDepartmentId === dept.id);
  const deptEmployees: EmployeeWithPosition[] = assignments
    .filter(a => a.departmentId === dept.id)
    .map(a => {
      const emp = employees.find(e => e.id === a.employeeId);
      const pos = positions.find(p => p.id === a.positionId);
      return { ...emp!, position: pos };
    })
    .filter(emp => emp.id !== undefined);

  const levelColors = [
    'from-primary-500 to-primary-700',
    'from-blue-500 to-blue-700',
    'from-indigo-500 to-indigo-700',
  ];
  const bgGradient = levelColors[Math.min(level, levelColors.length - 1)];

  const levelIcons = ['🏢', '📁', '📂'];
  const icon = levelIcons[Math.min(level, levelIcons.length - 1)];

  return (
    <div className={level > 0 ? 'ml-8 pl-6 border-l-4 border-primary-200' : ''}>
      <div className={`bg-gradient-to-r ${bgGradient} rounded-lg shadow-lg overflow-hidden`}>
        {/* Department Header */}
        <div className="p-4 text-white">
          <div className="flex items-center gap-3 mb-2">
            <span className="text-3xl">{icon}</span>
            <div className="flex-1">
              <h3 className="text-xl font-bold">{dept.name}</h3>
              <p className="text-sm opacity-90">従業員数: {deptEmployees.length}名</p>
            </div>
          </div>
        </div>

        {/* Employee Grid */}
        {deptEmployees.length > 0 && (
          <div className="bg-white bg-opacity-95 p-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
              {deptEmployees.map(emp => (
                <Link
                  key={emp.id}
                  to={`/employees/${emp.id}`}
                  className="flex items-center gap-3 p-3 bg-white rounded-lg border border-gray-200 hover:border-primary-400 hover:shadow-md transition-all duration-200 group"
                >
                  <div className="w-10 h-10 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
                    {emp.name.charAt(0)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-gray-900 truncate group-hover:text-primary-600 transition-colors">
                      {emp.name}
                    </p>
                    <p className="text-xs text-gray-600 truncate">
                      {emp.position?.name || '-'}
                      {emp.position?.level && (
                        <span className="text-gray-400 ml-1">(Lv{emp.position.level})</span>
                      )}
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Child Departments */}
      {childDepts.length > 0 && (
        <div className="mt-4 space-y-4">
          {childDepts.map(child => (
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
      )}
    </div>
  );
}
