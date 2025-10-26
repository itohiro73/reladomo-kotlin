import useSWR from 'swr';
import * as api from '../api/client';
import type { Position, Department, Employee, EmployeeAssignment, Salary } from '../types';

// Custom fetcher that handles errors
const fetcher = <T,>(fn: () => Promise<T>) => fn();

// Positions
export const usePositions = () => useSWR<Position[]>('positions', () => api.getPositions());
export const usePosition = (id: number | null) =>
  useSWR<Position>(id ? `positions/${id}` : null, () => id ? api.getPosition(id) : Promise.reject());

// Departments
export const useDepartments = () => useSWR<Department[]>('departments', () => api.getDepartments());
export const useDepartment = (id: number | null) =>
  useSWR<Department>(id ? `departments/${id}` : null, () => id ? api.getDepartment(id) : Promise.reject());

// Employees
export const useEmployees = () => useSWR<Employee[]>('employees', () => api.getEmployees());
export const useEmployee = (id: number | null) =>
  useSWR<Employee>(id ? `employees/${id}` : null, () => id ? api.getEmployee(id) : Promise.reject());

// Employee Assignments
export const useAssignments = () => useSWR<EmployeeAssignment[]>('assignments', () => api.getAssignments());
export const useAssignment = (id: number | null) =>
  useSWR<EmployeeAssignment>(id ? `assignments/${id}` : null, () => id ? api.getAssignment(id) : Promise.reject());
export const useAssignmentsByEmployee = (employeeId: number | null) =>
  useSWR<EmployeeAssignment[]>(
    employeeId ? `assignments/employee/${employeeId}` : null,
    () => employeeId ? api.getAssignmentsByEmployee(employeeId) : Promise.reject()
  );

// Salaries
export const useSalaries = () => useSWR<Salary[]>('salaries', () => api.getSalaries());
export const useSalary = (id: number | null) =>
  useSWR<Salary>(id ? `salaries/${id}` : null, () => id ? api.getSalary(id) : Promise.reject());
export const useSalariesByEmployee = (employeeId: number | null) =>
  useSWR<Salary[]>(
    employeeId ? `salaries/employee/${employeeId}` : null,
    () => employeeId ? api.getSalariesByEmployee(employeeId) : Promise.reject()
  );

// History
export const useAssignmentHistory = (employeeId: number | null) =>
  useSWR<EmployeeAssignment[]>(
    employeeId ? `assignments/employee/${employeeId}/history` : null,
    () => employeeId ? api.getAssignmentHistory(employeeId) : Promise.reject()
  );
export const useSalaryHistory = (employeeId: number | null) =>
  useSWR<Salary[]>(
    employeeId ? `salaries/employee/${employeeId}/history` : null,
    () => employeeId ? api.getSalaryHistory(employeeId) : Promise.reject()
  );

// Full history (for 2D visualization)
export const useAllAssignmentHistory = (employeeId: number | null) =>
  useSWR<EmployeeAssignment[]>(
    employeeId ? `assignments/employee/${employeeId}/history/all` : null,
    () => employeeId ? api.getAllAssignmentHistory(employeeId) : Promise.reject()
  );
export const useAllSalaryHistory = (employeeId: number | null) =>
  useSWR<Salary[]>(
    employeeId ? `salaries/employee/${employeeId}/history/all` : null,
    () => employeeId ? api.getAllSalaryHistory(employeeId) : Promise.reject()
  );
