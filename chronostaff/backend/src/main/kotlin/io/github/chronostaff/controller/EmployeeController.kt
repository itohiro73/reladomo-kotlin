package io.github.chronostaff.controller

import io.github.chronostaff.domain.EmployeeFinder
import io.github.chronostaff.dto.EmployeeDto
import io.github.chronostaff.dto.EmployeeDetailAsOfDto
import io.github.chronostaff.dto.AssignmentAsOfDto
import io.github.chronostaff.dto.SalaryAsOfDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/employees")
class EmployeeController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @GetMapping
    fun getAllEmployees(): List<EmployeeDto> {
        // Get current employees (processingDate at infinity)
        val operation = EmployeeFinder.processingDate().equalsInfinity()
        return EmployeeFinder.findMany(operation)
            .map { emp ->
                EmployeeDto(
                    id = emp.id,
                    employeeNumber = emp.employeeNumber,
                    name = emp.name,
                    email = emp.email,
                    hireDate = emp.hireDate.toInstant().toString(),
                    processingFrom = emp.processingDateFrom.toInstant().toString(),
                    processingThru = emp.processingDateTo.toInstant().toString()
                )
            }
    }

    @GetMapping("/{id}")
    fun getEmployee(@PathVariable id: Long): EmployeeDto? {
        val operation = EmployeeFinder.id().eq(id)
            .and(EmployeeFinder.processingDate().equalsInfinity())
        val emp = EmployeeFinder.findOne(operation) ?: return null

        return EmployeeDto(
            id = emp.id,
            employeeNumber = emp.employeeNumber,
            name = emp.name,
            email = emp.email,
            hireDate = emp.hireDate.toInstant().toString(),
            processingFrom = emp.processingDateFrom.toInstant().toString(),
            processingThru = emp.processingDateTo.toInstant().toString()
        )
    }

    @GetMapping("/{id}/asof")
    fun getEmployeeAsOf(
        @PathVariable id: Long,
        @RequestParam month: String  // Format: YYYY-MM
    ): EmployeeDetailAsOfDto {
        // Parse month and set to start of month (midnight) in JST, then convert to UTC
        val date = LocalDate.parse("${month}-01")
        val asOfInstant = date.atStartOfDay().toInstant(ZoneOffset.ofHours(9))  // JST to UTC
        val asOfTimestamp = Timestamp.from(asOfInstant)

        // Query employee data
        val employeeSql = """
            SELECT ID, EMPLOYEE_NUMBER, NAME, EMAIL, HIRE_DATE, PROCESSING_FROM, PROCESSING_THRU
            FROM EMPLOYEES
            WHERE ID = ?
              AND PROCESSING_THRU = '9999-12-01 23:59:00'
        """.trimIndent()

        val employee = jdbcTemplate.query(employeeSql, { rs, _ ->
            EmployeeDto(
                id = rs.getLong("ID"),
                employeeNumber = rs.getString("EMPLOYEE_NUMBER"),
                name = rs.getString("NAME"),
                email = rs.getString("EMAIL"),
                hireDate = rs.getTimestamp("HIRE_DATE").toInstant().toString(),
                processingFrom = rs.getTimestamp("PROCESSING_FROM").toInstant().toString(),
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant().toString()
            )
        }, id).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")

        // Query assignment data at the specified business date
        val assignmentSql = """
            SELECT
                ea.DEPARTMENT_ID,
                d.NAME as DEPARTMENT_NAME,
                ea.POSITION_ID,
                p.NAME as POSITION_NAME,
                p.LEVEL as POSITION_LEVEL,
                ea.BUSINESS_FROM,
                ea.UPDATED_BY
            FROM EMPLOYEE_ASSIGNMENTS ea
            JOIN DEPARTMENTS d ON ea.DEPARTMENT_ID = d.ID
            JOIN POSITIONS p ON ea.POSITION_ID = p.ID
            WHERE ea.EMPLOYEE_ID = ?
              AND ea.BUSINESS_FROM <= ?
              AND ea.BUSINESS_THRU > ?
              AND ea.PROCESSING_THRU = '9999-12-01 23:59:00'
              AND d.PROCESSING_THRU = '9999-12-01 23:59:00'
        """.trimIndent()

        val assignment = jdbcTemplate.query(assignmentSql, { rs, _ ->
            AssignmentAsOfDto(
                departmentId = rs.getLong("DEPARTMENT_ID"),
                departmentName = rs.getString("DEPARTMENT_NAME"),
                positionId = rs.getLong("POSITION_ID"),
                positionName = rs.getString("POSITION_NAME"),
                positionLevel = rs.getInt("POSITION_LEVEL"),
                businessFrom = rs.getTimestamp("BUSINESS_FROM").toInstant().toString(),
                updatedBy = rs.getString("UPDATED_BY")
            )
        }, id, asOfTimestamp, asOfTimestamp).firstOrNull()

        // Query salary data at the specified business date
        val salarySql = """
            SELECT
                AMOUNT,
                CURRENCY,
                BUSINESS_FROM,
                UPDATED_BY
            FROM SALARIES
            WHERE EMPLOYEE_ID = ?
              AND BUSINESS_FROM <= ?
              AND BUSINESS_THRU > ?
              AND PROCESSING_THRU = '9999-12-01 23:59:00'
        """.trimIndent()

        val salary = jdbcTemplate.query(salarySql, { rs, _ ->
            SalaryAsOfDto(
                amount = rs.getDouble("AMOUNT"),
                currency = rs.getString("CURRENCY"),
                businessFrom = rs.getTimestamp("BUSINESS_FROM").toInstant().toString(),
                updatedBy = rs.getString("UPDATED_BY")
            )
        }, id, asOfTimestamp, asOfTimestamp).firstOrNull()

        return EmployeeDetailAsOfDto(
            employee = employee,
            assignment = assignment,
            salary = salary,
            asOfMonth = month
        )
    }

    // TODO: Implement history endpoint - requires proper Reladomo temporal query API
    // @GetMapping("/{id}/history")
    // fun getEmployeeHistory(@PathVariable id: Long): List<EmployeeDto>
}
