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
