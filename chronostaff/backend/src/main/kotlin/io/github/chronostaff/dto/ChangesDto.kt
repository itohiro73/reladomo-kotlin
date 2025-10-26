package io.github.chronostaff.dto

/**
 * DTOs for scheduled changes (future-dated assignments and salaries)
 */
data class ScheduledChangeDto(
    val id: String,  // Composite ID: "assignment-{id}" or "salary-{id}"
    val type: ChangeType,
    val employeeId: Long,
    val employeeName: String,
    val effectiveDate: String,  // YYYY-MM-DD format (Business Date)
    val description: String,  // Human-readable description of the change
    val updatedBy: String,
    val processingFrom: String  // When this change was scheduled
)

enum class ChangeType {
    ASSIGNMENT,  // Transfer/assignment change
    SALARY       // Salary adjustment
}

data class ScheduledAssignmentChangeDto(
    val id: Long,
    val employeeId: Long,
    val employeeName: String,
    val fromDepartmentName: String?,
    val fromPositionName: String?,
    val toDepartmentName: String,
    val toPositionName: String,
    val effectiveDate: String,
    val updatedBy: String,
    val processingFrom: String
)

data class ScheduledSalaryChangeDto(
    val id: Long,
    val employeeId: Long,
    val employeeName: String,
    val fromAmount: Double?,
    val toAmount: Double,
    val currency: String,
    val effectiveDate: String,
    val updatedBy: String,
    val processingFrom: String
)
