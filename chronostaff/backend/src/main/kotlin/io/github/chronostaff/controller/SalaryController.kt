package io.github.chronostaff.controller

import io.github.chronostaff.domain.SalaryFinder
import io.github.chronostaff.dto.SalaryDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/salaries")
class SalaryController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @GetMapping
    fun getAllSalaries(): List<SalaryDto> {
        // Get current salaries (both businessDate and processingDate at infinity)
        val operation = SalaryFinder.businessDate().equalsInfinity()
            .and(SalaryFinder.processingDate().equalsInfinity())
        return SalaryFinder.findMany(operation)
            .map { salary ->
                SalaryDto(
                    id = salary.id,
                    employeeId = salary.employeeId,
                    amount = salary.amount,
                    currency = salary.currency,
                    updatedBy = salary.updatedBy,
                    businessFrom = salary.businessDateFrom.toInstant().toString(),
                    businessThru = salary.businessDateTo.toInstant().toString(),
                    processingFrom = salary.processingDateFrom.toInstant().toString(),
                    processingThru = salary.processingDateTo.toInstant().toString()
                )
            }
    }

    @GetMapping("/{id}")
    fun getSalary(@PathVariable id: Long): SalaryDto? {
        val operation = SalaryFinder.id().eq(id)
            .and(SalaryFinder.businessDate().equalsInfinity())
            .and(SalaryFinder.processingDate().equalsInfinity())
        val salary = SalaryFinder.findOne(operation) ?: return null

        return SalaryDto(
            id = salary.id,
            employeeId = salary.employeeId,
            amount = salary.amount,
            currency = salary.currency,
            updatedBy = salary.updatedBy,
            businessFrom = salary.businessDateFrom.toInstant().toString(),
            businessThru = salary.businessDateTo.toInstant().toString(),
            processingFrom = salary.processingDateFrom.toInstant().toString(),
            processingThru = salary.processingDateTo.toInstant().toString()
        )
    }

    @GetMapping("/employee/{employeeId}")
    fun getSalariesByEmployee(@PathVariable employeeId: Long): List<SalaryDto> {
        // Query for salaries valid TODAY (not future-dated salaries)
        val today = java.sql.Timestamp.from(java.time.Instant.now())
        val operation = SalaryFinder.employeeId().eq(employeeId)
            .and(SalaryFinder.businessDate().eq(today))  // Valid as of today
            .and(SalaryFinder.processingDate().equalsInfinity())  // Currently believed
        return SalaryFinder.findMany(operation)
            .map { salary ->
                SalaryDto(
                    id = salary.id,
                    employeeId = salary.employeeId,
                    amount = salary.amount,
                    currency = salary.currency,
                    updatedBy = salary.updatedBy,
                    businessFrom = salary.businessDateFrom.toInstant().toString(),
                    businessThru = salary.businessDateTo.toInstant().toString(),
                    processingFrom = salary.processingDateFrom.toInstant().toString(),
                    processingThru = salary.processingDateTo.toInstant().toString()
                )
            }
    }

    @GetMapping("/employee/{employeeId}/history")
    fun getSalaryHistory(@PathVariable employeeId: Long): List<SalaryDto> {
        // Fetch only currently valid records (PROCESSING_THRU = infinity)
        // This shows what the system currently believes to be true across all business time
        val sql = """
            SELECT ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY,
                   BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU
            FROM SALARIES
            WHERE EMPLOYEE_ID = ?
              AND PROCESSING_THRU = '9999-12-01 23:59:00'
            ORDER BY BUSINESS_FROM DESC
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            SalaryDto(
                id = rs.getLong("ID"),
                employeeId = rs.getLong("EMPLOYEE_ID"),
                amount = rs.getBigDecimal("AMOUNT"),
                currency = rs.getString("CURRENCY"),
                updatedBy = rs.getString("UPDATED_BY"),
                businessFrom = rs.getTimestamp("BUSINESS_FROM").toInstant().toString(),
                businessThru = rs.getTimestamp("BUSINESS_THRU").toInstant().toString(),
                processingFrom = rs.getTimestamp("PROCESSING_FROM").toInstant().toString(),
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant().toString()
            )
        }, employeeId)
    }

    @GetMapping("/employee/{employeeId}/history/all")
    fun getAllSalaryHistory(@PathVariable employeeId: Long): List<SalaryDto> {
        // Fetch ALL salary records including past versions
        // This is used for 2D bitemporal visualization
        val sql = """
            SELECT ID, EMPLOYEE_ID, AMOUNT, CURRENCY, UPDATED_BY,
                   BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU
            FROM SALARIES
            WHERE EMPLOYEE_ID = ?
            ORDER BY PROCESSING_FROM ASC, BUSINESS_FROM ASC
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            SalaryDto(
                id = rs.getLong("ID"),
                employeeId = rs.getLong("EMPLOYEE_ID"),
                amount = rs.getBigDecimal("AMOUNT"),
                currency = rs.getString("CURRENCY"),
                updatedBy = rs.getString("UPDATED_BY"),
                businessFrom = rs.getTimestamp("BUSINESS_FROM").toInstant().toString(),
                businessThru = rs.getTimestamp("BUSINESS_THRU").toInstant().toString(),
                processingFrom = rs.getTimestamp("PROCESSING_FROM").toInstant().toString(),
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant().toString()
            )
        }, employeeId)
    }
}
