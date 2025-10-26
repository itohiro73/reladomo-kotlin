package io.github.chronostaff.controller

import io.github.chronostaff.domain.EmployeeAssignmentFinder
import io.github.chronostaff.dto.EmployeeAssignmentDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/assignments")
class EmployeeAssignmentController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @GetMapping
    fun getAllAssignments(): List<EmployeeAssignmentDto> {
        // Get current assignments (both businessDate and processingDate at infinity)
        val operation = EmployeeAssignmentFinder.businessDate().equalsInfinity()
            .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
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
            .and(EmployeeAssignmentFinder.businessDate().equalsInfinity())
            .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
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

    @GetMapping("/employee/{employeeId}")
    fun getAssignmentsByEmployee(@PathVariable employeeId: Long): List<EmployeeAssignmentDto> {
        val operation = EmployeeAssignmentFinder.employeeId().eq(employeeId)
            .and(EmployeeAssignmentFinder.businessDate().equalsInfinity())
            .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
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

    @GetMapping("/employee/{employeeId}/history")
    fun getAssignmentHistory(@PathVariable employeeId: Long): List<EmployeeAssignmentDto> {
        // Fetch only currently valid records (PROCESSING_THRU = infinity)
        // This shows what the system currently believes to be true across all business time
        val sql = """
            SELECT ID, EMPLOYEE_ID, DEPARTMENT_ID, POSITION_ID, UPDATED_BY,
                   BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU
            FROM EMPLOYEE_ASSIGNMENTS
            WHERE EMPLOYEE_ID = ?
              AND PROCESSING_THRU = '9999-12-01 23:59:00'
            ORDER BY BUSINESS_FROM DESC
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            EmployeeAssignmentDto(
                id = rs.getLong("ID"),
                employeeId = rs.getLong("EMPLOYEE_ID"),
                departmentId = rs.getLong("DEPARTMENT_ID"),
                positionId = rs.getLong("POSITION_ID"),
                updatedBy = rs.getString("UPDATED_BY"),
                businessFrom = rs.getTimestamp("BUSINESS_FROM").toInstant().toString(),
                businessThru = rs.getTimestamp("BUSINESS_THRU").toInstant().toString(),
                processingFrom = rs.getTimestamp("PROCESSING_FROM").toInstant().toString(),
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant().toString()
            )
        }, employeeId)
    }
}
