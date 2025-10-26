package io.github.chronostaff.controller

import com.gs.fw.common.mithra.MithraManagerProvider
import io.github.chronostaff.domain.Employee
import io.github.chronostaff.domain.EmployeeAssignment
import io.github.chronostaff.domain.EmployeeFinder
import io.github.chronostaff.domain.Salary
import io.github.chronostaff.dto.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/employees")
class EmployeeController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @GetMapping
    fun getAllEmployees(@RequestParam companyId: Long): List<EmployeeDto> {
        // Get current employees for specific company (processingDate at infinity)
        val operation = EmployeeFinder.companyId().eq(companyId)
            .and(EmployeeFinder.processingDate().equalsInfinity())
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

    /**
     * Create new employee with initial assignment and salary
     * Demonstrates "Effective Date" concept - user only specifies when changes take effect
     */
    @PostMapping
    fun createEmployee(@RequestBody request: EmployeeCreateDto): EmployeeDto {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            try {
                // 1. Create Employee (unitemporal - processingDate only)
                // For unitemporal entities with audit tracking, must pass infinity for insert
                // SimulatedSequence automatically generates IDs
                val infinityDate = Timestamp.from(Instant.parse("9999-12-01T23:59:00Z"))
                val now = Timestamp.from(Instant.now())
                val employee = Employee(infinityDate)
                employee.setCompanyId(request.companyId)  // Link to company
                employee.setEmployeeNumber(request.employeeNumber)
                employee.setName(request.name)
                employee.setEmail(request.email)

                // Convert hire date from YYYY-MM-DD (user's timezone) to UTC
                val hireDate = LocalDate.parse(request.hireDate)
                employee.setHireDate(Timestamp.from(
                    hireDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))  // JST to UTC
                ))

                employee.insert()

                // 2. Create Assignment (bitemporal - businessDate + processingDate)
                // User-specified "Effective Date" → Business Time
                // SimulatedSequence automatically generates IDs
                val assignmentEffectiveDate = LocalDate.parse(request.assignment.effectiveDate)
                val assignmentBusinessFrom = Timestamp.from(
                    assignmentEffectiveDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))  // JST to UTC
                )

                val assignment = EmployeeAssignment(assignmentBusinessFrom, infinityDate)
                assignment.setEmployeeId(employee.id)
                assignment.setDepartmentId(request.assignment.departmentId)
                assignment.setPositionId(request.assignment.positionId)
                assignment.setUpdatedBy(request.assignment.updatedBy)

                assignment.insert()

                // 3. Create Salary (bitemporal - businessDate + processingDate)
                // User-specified "Effective Date" → Business Time
                // SimulatedSequence automatically generates IDs
                val salaryEffectiveDate = LocalDate.parse(request.salary.effectiveDate)
                val salaryBusinessFrom = Timestamp.from(
                    salaryEffectiveDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))  // JST to UTC
                )

                val salary = Salary(salaryBusinessFrom, infinityDate)
                salary.setEmployeeId(employee.id)
                salary.setAmount(BigDecimal.valueOf(request.salary.amount))
                salary.setCurrency(request.salary.currency)
                salary.setUpdatedBy(request.salary.updatedBy)

                salary.insert()

                // Return created employee DTO
                EmployeeDto(
                    id = employee.id,
                    employeeNumber = employee.employeeNumber,
                    name = employee.name,
                    email = employee.email,
                    hireDate = employee.hireDate.toInstant().toString(),
                    processingFrom = employee.processingDateFrom.toInstant().toString(),
                    processingThru = employee.processingDateTo.toInstant().toString()
                )
            } catch (e: Exception) {
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create employee: ${e.message}",
                    e
                )
            }
        }
    }

    // TODO: Implement history endpoint - requires proper Reladomo temporal query API
    // @GetMapping("/{id}/history")
    // fun getEmployeeHistory(@PathVariable id: Long): List<EmployeeDto>
}
