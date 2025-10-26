import type {
  Position,
  Department,
  Employee,
  EmployeeAssignment,
  Salary,
  OrganizationSnapshot,
  EmployeeDetailAsOf,
  SetupRequestDto,
  SetupResponseDto,
  EmployeeCreateDto
} from '../types';

const API_BASE = '/api';

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  return response.json();
}

async function postJson<T, R>(url: string, data: T): Promise<R> {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  return response.json();
}

// Positions
export const getPositions = () => fetchJson<Position[]>(`${API_BASE}/positions`);
export const getPosition = (id: number) => fetchJson<Position>(`${API_BASE}/positions/${id}`);

// Departments
export const getDepartments = (companyId: number) => fetchJson<Department[]>(`${API_BASE}/departments?companyId=${companyId}`);
export const getDepartment = (id: number) => fetchJson<Department>(`${API_BASE}/departments/${id}`);

// Employees
export const getEmployees = (companyId: number) => fetchJson<Employee[]>(`${API_BASE}/employees?companyId=${companyId}`);
export const getEmployee = (id: number) => fetchJson<Employee>(`${API_BASE}/employees/${id}`);
export const getEmployeeAsOf = (id: number, month: string) =>
  fetchJson<EmployeeDetailAsOf>(`${API_BASE}/employees/${id}/asof?month=${month}`);

// Employee Assignments
export const getAssignments = () => fetchJson<EmployeeAssignment[]>(`${API_BASE}/assignments`);
export const getAssignment = (id: number) => fetchJson<EmployeeAssignment>(`${API_BASE}/assignments/${id}`);
export const getAssignmentsByEmployee = (employeeId: number) =>
  fetchJson<EmployeeAssignment[]>(`${API_BASE}/assignments/employee/${employeeId}`);

// Salaries
export const getSalaries = () => fetchJson<Salary[]>(`${API_BASE}/salaries`);
export const getSalary = (id: number) => fetchJson<Salary>(`${API_BASE}/salaries/${id}`);
export const getSalariesByEmployee = (employeeId: number) =>
  fetchJson<Salary[]>(`${API_BASE}/salaries/employee/${employeeId}`);

// History endpoints
export const getAssignmentHistory = (employeeId: number) =>
  fetchJson<EmployeeAssignment[]>(`${API_BASE}/assignments/employee/${employeeId}/history`);
export const getSalaryHistory = (employeeId: number) =>
  fetchJson<Salary[]>(`${API_BASE}/salaries/employee/${employeeId}/history`);

// Full history endpoints (for 2D visualization)
export const getAllAssignmentHistory = (employeeId: number) =>
  fetchJson<EmployeeAssignment[]>(`${API_BASE}/assignments/employee/${employeeId}/history/all`);
export const getAllSalaryHistory = (employeeId: number) =>
  fetchJson<Salary[]>(`${API_BASE}/salaries/employee/${employeeId}/history/all`);

// Organization snapshot endpoint (time-travel query)
export const getOrganizationSnapshot = (asOfDate: string, companyId: number) =>
  fetchJson<OrganizationSnapshot>(`${API_BASE}/organization/snapshot?asOfDate=${asOfDate}&companyId=${companyId}`);

// Phase 1 MVP: Setup and Employee Creation
export const setupOrganization = (data: SetupRequestDto) =>
  postJson<SetupRequestDto, SetupResponseDto>(`${API_BASE}/setup`, data);

export const createEmployee = (data: EmployeeCreateDto) =>
  postJson<EmployeeCreateDto, Employee>(`${API_BASE}/employees`, data);
