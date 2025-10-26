import { Link } from 'react-router-dom';
import { useEmployees, useAssignments, useSalaries, usePositions, useDepartments } from '../hooks/useAPI';
import { useCompany } from '../contexts/CompanyContext';
import { formatDateOnly } from '../utils/date';
import type { EmployeeWithDetails } from '../types';

export default function EmployeeList() {
  const { selectedCompanyId } = useCompany();
  const { data: employees, error: empError, isLoading: empLoading } = useEmployees(selectedCompanyId);
  const { data: assignments } = useAssignments();
  const { data: salaries } = useSalaries();
  const { data: positions } = usePositions();
  const { data: departments } = useDepartments(selectedCompanyId);

  if (empLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (empError) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
        Error loading employees: {empError.message}
      </div>
    );
  }

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
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold text-gray-900">従業員一覧</h2>
        <div className="flex items-center gap-2 text-sm text-gray-600">
          <span className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full font-medium">
            {employees.length}名
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {enrichedEmployees.map(emp => (
          <Link
            key={emp.id}
            to={`/employees/${emp.id}`}
            className="card group hover:scale-105 transform transition-all duration-200"
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-gradient-to-br from-primary-400 to-primary-600 rounded-full flex items-center justify-center text-white font-bold text-lg">
                  {emp.name.charAt(0)}
                </div>
                <div>
                  <h3 className="font-semibold text-lg text-gray-900 group-hover:text-primary-600 transition-colors">
                    {emp.name}
                  </h3>
                  <p className="text-sm text-gray-500">{emp.employeeNumber}</p>
                </div>
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center gap-2 text-sm">
                <span className="text-gray-500">📧</span>
                <span className="text-gray-700 truncate">{emp.email}</span>
              </div>

              {emp.department && (
                <div className="flex items-center gap-2 text-sm">
                  <span className="text-gray-500">🏢</span>
                  <span className="text-gray-700">{emp.department.name}</span>
                </div>
              )}

              {emp.position && (
                <div className="flex items-center gap-2 text-sm">
                  <span className="text-gray-500">💼</span>
                  <span className="text-gray-700">
                    {emp.position.name} <span className="text-gray-400">(Level {emp.position.level})</span>
                  </span>
                </div>
              )}

              {emp.currentSalary && (
                <div className="flex items-center gap-2 text-sm">
                  <span className="text-gray-500">💰</span>
                  <span className="font-medium text-gray-900">
                    {emp.currentSalary.currency} {emp.currentSalary.amount.toLocaleString()}
                  </span>
                </div>
              )}

              <div className="flex items-center gap-2 text-sm pt-2 border-t border-gray-100">
                <span className="text-gray-500">📅</span>
                <span className="text-gray-600">
                  入社日: {formatDateOnly(emp.hireDate)}
                </span>
              </div>
            </div>

            <div className="mt-4 flex items-center justify-end">
              <span className="text-primary-600 text-sm font-medium group-hover:underline">
                詳細を見る →
              </span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
