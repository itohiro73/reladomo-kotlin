package io.github.chronostaff.dto

data class EmployeeAssignmentDto(
    val id: Long,
    val employeeId: Long,
    val departmentId: Long,
    val positionId: Long,
    val updatedBy: String,
    val businessFrom: String,
    val businessThru: String,
    val processingFrom: String,
    val processingThru: String
)
