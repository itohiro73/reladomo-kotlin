package io.github.chronostaff.controller

import io.github.chronostaff.domain.EmployeeAssignmentFinder
import io.github.chronostaff.dto.OrganizationSnapshotDto
import io.github.chronostaff.dto.DepartmentSnapshotDto
import io.github.chronostaff.dto.EmployeeSnapshotDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/organization")
class OrganizationController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @GetMapping("/snapshot")
    fun getOrganizationSnapshot(
        @RequestParam asOfDate: String,  // Format: YYYY-MM-DD
        @RequestParam companyId: Long
    ): OrganizationSnapshotDto {
        // Parse asOfDate and set to start of day (midnight) in JST, then convert to UTC
        val date = LocalDate.parse(asOfDate)
        val asOfInstant = date.atStartOfDay().toInstant(ZoneOffset.ofHours(9))  // JST to UTC
        val asOfTimestamp = Timestamp.from(asOfInstant)

        // Query all assignments valid at the specified business date for specific company
        // Using current processing time (PROCESSING_THRU = infinity) to get current beliefs
        val sql = """
            SELECT
                ea.EMPLOYEE_ID,
                e.NAME as EMPLOYEE_NAME,
                ea.DEPARTMENT_ID,
                d.NAME as DEPARTMENT_NAME,
                ea.POSITION_ID,
                p.NAME as POSITION_NAME,
                p.LEVEL as POSITION_LEVEL
            FROM EMPLOYEE_ASSIGNMENTS ea
            JOIN EMPLOYEES e ON ea.EMPLOYEE_ID = e.ID
            JOIN DEPARTMENTS d ON ea.DEPARTMENT_ID = d.ID
            JOIN POSITIONS p ON ea.POSITION_ID = p.ID
            WHERE ea.BUSINESS_FROM <= ?
              AND ea.BUSINESS_THRU > ?
              AND ea.PROCESSING_THRU = '9999-12-01 23:59:00'
              AND e.PROCESSING_THRU = '9999-12-01 23:59:00'
              AND d.PROCESSING_THRU = '9999-12-01 23:59:00'
              AND e.COMPANY_ID = ?
              AND d.COMPANY_ID = ?
              AND p.COMPANY_ID = ?
            ORDER BY d.ID, e.NAME
        """.trimIndent()

        val assignments = jdbcTemplate.query(sql, { rs, _ ->
            mapOf(
                "employeeId" to rs.getLong("EMPLOYEE_ID"),
                "employeeName" to rs.getString("EMPLOYEE_NAME"),
                "departmentId" to rs.getLong("DEPARTMENT_ID"),
                "departmentName" to rs.getString("DEPARTMENT_NAME"),
                "positionId" to rs.getLong("POSITION_ID"),
                "positionName" to rs.getString("POSITION_NAME"),
                "positionLevel" to rs.getInt("POSITION_LEVEL")
            )
        }, asOfTimestamp, asOfTimestamp, companyId, companyId, companyId)

        // Group by department
        val departmentMap = assignments.groupBy { it["departmentId"] as Long }

        val departments = departmentMap.map { (deptId, deptAssignments) ->
            val deptName = deptAssignments.first()["departmentName"] as String
            val employees = deptAssignments.map { assignment ->
                EmployeeSnapshotDto(
                    id = assignment["employeeId"] as Long,
                    name = assignment["employeeName"] as String,
                    positionId = assignment["positionId"] as Long,
                    positionName = assignment["positionName"] as String,
                    positionLevel = assignment["positionLevel"] as Int
                )
            }
            DepartmentSnapshotDto(
                id = deptId,
                name = deptName,
                employees = employees
            )
        }.sortedBy { it.id }

        return OrganizationSnapshotDto(
            asOfDate = asOfDate,
            departments = departments
        )
    }
}
