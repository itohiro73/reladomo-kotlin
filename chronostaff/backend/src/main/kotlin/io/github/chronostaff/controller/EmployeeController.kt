package io.github.chronostaff.controller

import io.github.chronostaff.domain.EmployeeFinder
import io.github.chronostaff.dto.EmployeeDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/employees")
class EmployeeController {

    @GetMapping
    fun getAllEmployees(): List<EmployeeDto> {
        // Get current employees (processingDate at infinity)
        val operation = EmployeeFinder.processingDate().equalsEdgePoint()
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
            .and(EmployeeFinder.processingDate().equalsEdgePoint())
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

    // TODO: Implement history endpoint - requires proper Reladomo temporal query API
    // @GetMapping("/{id}/history")
    // fun getEmployeeHistory(@PathVariable id: Long): List<EmployeeDto>
}
