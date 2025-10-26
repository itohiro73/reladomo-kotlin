package io.github.chronostaff.dto

data class EmployeeDto(
    val id: Long,
    val employeeNumber: String,
    val name: String,
    val email: String,
    val hireDate: String,
    val processingFrom: String,
    val processingThru: String
)

// AsOf query result with assignment and salary information
data class EmployeeDetailAsOfDto(
    val employee: EmployeeDto,
    val assignment: AssignmentAsOfDto?,
    val salary: SalaryAsOfDto?,
    val asOfMonth: String  // YYYY-MM format
)

data class AssignmentAsOfDto(
    val departmentId: Long,
    val departmentName: String,
    val positionId: Long,
    val positionName: String,
    val positionLevel: Int,
    val businessFrom: String,
    val updatedBy: String
)

data class SalaryAsOfDto(
    val amount: Double,
    val currency: String,
    val businessFrom: String,
    val updatedBy: String
)

// Employee creation with initial assignment and salary
data class EmployeeCreateDto(
    val companyId: Long,  // Company ID for multi-tenancy
    val employeeNumber: String,
    val name: String,
    val email: String,
    val hireDate: String,  // YYYY-MM-DD format (will be converted to UTC)
    val assignment: InitialAssignmentDto,
    val salary: InitialSalaryDto
)

data class InitialAssignmentDto(
    val departmentId: Long,
    val positionId: Long,
    val effectiveDate: String,  // YYYY-MM-DD format (Business Date in user's timezone)
    val updatedBy: String
)

data class InitialSalaryDto(
    val amount: Double,
    val currency: String = "JPY",
    val effectiveDate: String,  // YYYY-MM-DD format (Business Date in user's timezone)
    val updatedBy: String
)

// Transfer (assignment change) request
data class TransferRequestDto(
    val newDepartmentId: Long,
    val newPositionId: Long,
    val effectiveDate: String,  // YYYY-MM-DD format (Business Date - when transfer takes effect)
    val reason: String?,  // Optional reason for the transfer
    val updatedBy: String
)

// Salary adjustment request
data class SalaryAdjustmentRequestDto(
    val newAmount: Double,
    val currency: String = "JPY",
    val effectiveDate: String,  // YYYY-MM-DD format (Business Date - when salary change takes effect)
    val reason: String?,  // Optional reason for the salary adjustment
    val updatedBy: String
)
