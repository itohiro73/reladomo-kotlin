import type { Position, Department, Employee, EmployeeAssignment, Salary } from '../types';

const API_BASE = '/api';

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  return response.json();
}

// Positions
export const getPositions = () => fetchJson<Position[]>(`${API_BASE}/positions`);
export const getPosition = (id: number) => fetchJson<Position>(`${API_BASE}/positions/${id}`);

// Departments
export const getDepartments = () => fetchJson<Department[]>(`${API_BASE}/departments`);
export const getDepartment = (id: number) => fetchJson<Department>(`${API_BASE}/departments/${id}`);

// Employees
export const getEmployees = () => fetchJson<Employee[]>(`${API_BASE}/employees`);
export const getEmployee = (id: number) => fetchJson<Employee>(`${API_BASE}/employees/${id}`);

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
