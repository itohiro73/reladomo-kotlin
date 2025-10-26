package io.github.chronostaff.controller

import io.github.chronostaff.domain.SalaryFinder
import io.github.chronostaff.dto.SalaryDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/salaries")
class SalaryController {

    @GetMapping
    fun getAllSalaries(): List<SalaryDto> {
        // Get current salaries (both businessDate and processingDate at infinity)
        val operation = SalaryFinder.businessDate().equalsEdgePoint()
            .and(SalaryFinder.processingDate().equalsEdgePoint())
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
            .and(SalaryFinder.businessDate().equalsEdgePoint())
            .and(SalaryFinder.processingDate().equalsEdgePoint())
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

    // TODO: Implement history endpoint - requires proper Reladomo bitemporal query API
    // @GetMapping("/{id}/history")
    // fun getSalaryHistory(@PathVariable id: Long): List<SalaryDto>

    @GetMapping("/employee/{employeeId}")
    fun getSalariesByEmployee(@PathVariable employeeId: Long): List<SalaryDto> {
        val operation = SalaryFinder.employeeId().eq(employeeId)
            .and(SalaryFinder.businessDate().equalsEdgePoint())
            .and(SalaryFinder.processingDate().equalsEdgePoint())
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
}
