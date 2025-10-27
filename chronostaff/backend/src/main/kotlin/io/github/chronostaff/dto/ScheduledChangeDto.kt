package io.github.chronostaff.dto

import java.time.Instant

/**
 * DTO representing a scheduled change (future-dated organizational change)
 */
data class ScheduledChangeDto(
    val effectiveDate: String,  // Business date when change takes effect (ISO 8601)
    val changeType: String,     // Type of change: TRANSFER, SALARY, DEPARTMENT, POSITION
    val entityType: String,     // Entity being changed: EMPLOYEE, DEPARTMENT, POSITION
    val entityId: Long,         // ID of the entity
    val entityName: String,     // Display name (employee name, department name, etc.)
    val details: ChangeDetails, // Type-specific details
    val recordId: Long          // ID of the record for editing/canceling
)

/**
 * Polymorphic details for different change types
 */
sealed class ChangeDetails {
    /**
     * Employee transfer details
     */
    data class Transfer(
        val fromDepartmentId: Long?,
        val fromDepartmentName: String?,
        val toDepartmentId: Long,
        val toDepartmentName: String,
        val fromPositionId: Long?,
        val fromPositionName: String?,
        val toPositionId: Long,
        val toPositionName: String
    ) : ChangeDetails()

    /**
     * Salary adjustment details
     */
    data class SalaryAdjustment(
        val fromAmount: String?,  // Previous salary (may be null for new hire)
        val toAmount: String      // New salary
    ) : ChangeDetails()

    /**
     * New department creation
     */
    data class DepartmentCreation(
        val departmentName: String,
        val description: String?,
        val parentDepartmentId: Long?,
        val parentDepartmentName: String?
    ) : ChangeDetails()

    /**
     * New position creation
     */
    data class PositionCreation(
        val positionName: String,
        val level: Int,
        val description: String?
    ) : ChangeDetails()
}
