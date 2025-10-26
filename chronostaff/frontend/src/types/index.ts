// Type definitions matching backend DTOs

export interface Position {
  id: number;
  name: string;
  level: number;
  description: string | null;
}

export interface Department {
  id: number;
  name: string;
  description: string;
  parentDepartmentId: number | null;
  processingFrom: string;  // ISO-8601 UTC
  processingThru: string;  // ISO-8601 UTC
}

export interface Employee {
  id: number;
  employeeNumber: string;
  name: string;
  email: string;
  hireDate: string;  // ISO-8601 UTC
  processingFrom: string;
  processingThru: string;
}

export interface EmployeeAssignment {
  id: number;
  employeeId: number;
  departmentId: number;
  positionId: number;
  updatedBy: string;
  businessFrom: string;  // ISO-8601 UTC
  businessThru: string;
  processingFrom: string;
  processingThru: string;
}

export interface Salary {
  id: number;
  employeeId: number;
  amount: number;
  currency: string;
  updatedBy: string;
  businessFrom: string;  // ISO-8601 UTC
  businessThru: string;
  processingFrom: string;
  processingThru: string;
}

// Organization Snapshot types
export interface OrganizationSnapshot {
  asOfDate: string;  // YYYY-MM-DD format
  departments: DepartmentSnapshot[];
}

export interface DepartmentSnapshot {
  id: number;
  name: string;
  employees: EmployeeSnapshot[];
}

export interface EmployeeSnapshot {
  id: number;
  name: string;
  positionId: number;
  positionName: string;
  positionLevel: number;
}

// Enriched types for display
export interface EmployeeWithDetails extends Employee {
  currentAssignment?: EmployeeAssignment;
  currentSalary?: Salary;
  position?: Position;
  department?: Department;
}
