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

// AsOf query types
export interface EmployeeDetailAsOf {
  employee: Employee;
  assignment: AssignmentAsOf | null;
  salary: SalaryAsOf | null;
  asOfMonth: string;  // YYYY-MM format
}

export interface AssignmentAsOf {
  departmentId: number;
  departmentName: string;
  positionId: number;
  positionName: string;
  positionLevel: number;
  businessFrom: string;
  updatedBy: string;
}

export interface SalaryAsOf {
  amount: number;
  currency: string;
  businessFrom: string;
  updatedBy: string;
}

// Phase 1 MVP: Setup Wizard DTOs
export interface PositionCreateDto {
  name: string;
  level: number;
  description: string | null;
}

export interface DepartmentCreateDto {
  name: string;
  description: string;
}

export interface SetupRequestDto {
  companyName: string;
  effectiveDate: string;  // YYYY-MM-DD format
  positions: PositionCreateDto[];
  departments: DepartmentCreateDto[];
}

export interface SetupResponseDto {
  companyName: string;
  positions: Position[];
  departments: Department[];
}

// Phase 1 MVP: Employee Creation DTOs
export interface InitialAssignmentDto {
  departmentId: number;
  positionId: number;
  effectiveDate: string;  // YYYY-MM-DD format (JST)
  updatedBy: string;
}

export interface InitialSalaryDto {
  amount: number;
  currency: string;
  effectiveDate: string;  // YYYY-MM-DD format (JST)
  updatedBy: string;
}

export interface EmployeeCreateDto {
  employeeNumber: string;
  name: string;
  email: string;
  hireDate: string;  // YYYY-MM-DD format (JST)
  assignment: InitialAssignmentDto;
  salary: InitialSalaryDto;
}

// Phase 2: Transfer and Salary Adjustment DTOs
export interface TransferRequestDto {
  newDepartmentId: number;
  newPositionId: number;
  effectiveDate: string;  // YYYY-MM-DD format (Business Date - when transfer takes effect)
  reason?: string;  // Optional reason for the transfer
  updatedBy: string;
}

export interface SalaryAdjustmentRequestDto {
  newAmount: number;
  currency: string;  // Default: "JPY"
  effectiveDate: string;  // YYYY-MM-DD format (Business Date - when salary change takes effect)
  reason?: string;  // Optional reason for the salary adjustment
  updatedBy: string;
}

// Phase 2: Scheduled Changes Types
export type ChangeType = 'TRANSFER' | 'SALARY' | 'DEPARTMENT' | 'POSITION';
export type EntityType = 'EMPLOYEE' | 'DEPARTMENT' | 'POSITION';

export interface TransferDetails {
  fromDepartmentId: number | null;
  fromDepartmentName: string | null;
  toDepartmentId: number;
  toDepartmentName: string;
  fromPositionId: number | null;
  fromPositionName: string | null;
  toPositionId: number;
  toPositionName: string;
}

export interface SalaryAdjustmentDetails {
  fromAmount: string | null;
  toAmount: string;
}

export interface DepartmentCreationDetails {
  departmentName: string;
  description: string | null;
  parentDepartmentId: number | null;
  parentDepartmentName: string | null;
}

export interface PositionCreationDetails {
  positionName: string;
  level: number;
  description: string | null;
}

export type ChangeDetails = TransferDetails | SalaryAdjustmentDetails | DepartmentCreationDetails | PositionCreationDetails;

export interface ScheduledChange {
  effectiveDate: string;  // ISO-8601 UTC
  changeType: ChangeType;
  entityType: EntityType;
  entityId: number;
  entityName: string;
  details: ChangeDetails;
  recordId: number;
}
