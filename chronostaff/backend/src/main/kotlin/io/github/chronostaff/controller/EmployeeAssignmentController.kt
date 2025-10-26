package io.github.chronostaff.controller

import io.github.chronostaff.domain.EmployeeAssignmentFinder
import io.github.chronostaff.dto.EmployeeAssignmentDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assignments")
class EmployeeAssignmentController {

    @GetMapping
    fun getAllAssignments(): List<EmployeeAssignmentDto> {
        // Get current assignments (both businessDate and processingDate at infinity)
        val operation = EmployeeAssignmentFinder.businessDate().equalsEdgePoint()
            .and(EmployeeAssignmentFinder.processingDate().equalsEdgePoint())
        return EmployeeAssignmentFinder.findMany(operation)
            .map { assignment ->
                EmployeeAssignmentDto(
                    id = assignment.id,
                    employeeId = assignment.employeeId,
                    departmentId = assignment.departmentId,
                    positionId = assignment.positionId,
                    updatedBy = assignment.updatedBy,
                    businessFrom = assignment.businessDateFrom.toInstant().toString(),
                    businessThru = assignment.businessDateTo.toInstant().toString(),
                    processingFrom = assignment.processingDateFrom.toInstant().toString(),
                    processingThru = assignment.processingDateTo.toInstant().toString()
                )
            }
    }

    @GetMapping("/{id}")
    fun getAssignment(@PathVariable id: Long): EmployeeAssignmentDto? {
        val operation = EmployeeAssignmentFinder.id().eq(id)
            .and(EmployeeAssignmentFinder.businessDate().equalsEdgePoint())
            .and(EmployeeAssignmentFinder.processingDate().equalsEdgePoint())
        val assignment = EmployeeAssignmentFinder.findOne(operation) ?: return null

        return EmployeeAssignmentDto(
            id = assignment.id,
            employeeId = assignment.employeeId,
            departmentId = assignment.departmentId,
            positionId = assignment.positionId,
            updatedBy = assignment.updatedBy,
            businessFrom = assignment.businessDateFrom.toInstant().toString(),
            businessThru = assignment.businessDateTo.toInstant().toString(),
            processingFrom = assignment.processingDateFrom.toInstant().toString(),
            processingThru = assignment.processingDateTo.toInstant().toString()
        )
    }

    // TODO: Implement history endpoint - requires proper Reladomo bitemporal query API
    // @GetMapping("/{id}/history")
    // fun getAssignmentHistory(@PathVariable id: Long): List<EmployeeAssignmentDto>

    @GetMapping("/employee/{employeeId}")
    fun getAssignmentsByEmployee(@PathVariable employeeId: Long): List<EmployeeAssignmentDto> {
        val operation = EmployeeAssignmentFinder.employeeId().eq(employeeId)
            .and(EmployeeAssignmentFinder.businessDate().equalsEdgePoint())
            .and(EmployeeAssignmentFinder.processingDate().equalsEdgePoint())
        return EmployeeAssignmentFinder.findMany(operation)
            .map { assignment ->
                EmployeeAssignmentDto(
                    id = assignment.id,
                    employeeId = assignment.employeeId,
                    departmentId = assignment.departmentId,
                    positionId = assignment.positionId,
                    updatedBy = assignment.updatedBy,
                    businessFrom = assignment.businessDateFrom.toInstant().toString(),
                    businessThru = assignment.businessDateTo.toInstant().toString(),
                    processingFrom = assignment.processingDateFrom.toInstant().toString(),
                    processingThru = assignment.processingDateTo.toInstant().toString()
                )
            }
    }
}
