package io.github.chronostaff.controller

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp
import io.github.chronostaff.domain.Department
import io.github.chronostaff.domain.DepartmentFinder
import io.github.chronostaff.domain.EmployeeAssignmentFinder
import io.github.chronostaff.dto.DepartmentDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp

@RestController
@RequestMapping("/api/departments")
class DepartmentController {

    @GetMapping
    fun getAllDepartments(@RequestParam companyId: Long): List<DepartmentDto> {
        // Get current departments for specific company (processingDate at infinity)
        val operation = DepartmentFinder.companyId().eq(companyId)
            .and(DepartmentFinder.processingDate().equalsInfinity())
        return DepartmentFinder.findMany(operation)
            .map { dept ->
                DepartmentDto(
                    id = dept.id,
                    name = dept.name,
                    description = dept.description ?: "",
                    parentDepartmentId = if (dept.isParentDepartmentIdNull || dept.parentDepartmentId == 0L) null else dept.parentDepartmentId,
                    processingFrom = dept.processingDateFrom.toInstant().toString(),
                    processingThru = dept.processingDateTo.toInstant().toString()
                )
            }
    }

    @GetMapping("/{id}")
    fun getDepartment(@PathVariable id: Long): DepartmentDto? {
        val operation = DepartmentFinder.id().eq(id)
            .and(DepartmentFinder.processingDate().equalsInfinity())
        val dept = DepartmentFinder.findOne(operation) ?: return null

        return DepartmentDto(
            id = dept.id,
            name = dept.name,
            description = dept.description ?: "",
            parentDepartmentId = if (dept.isParentDepartmentIdNull || dept.parentDepartmentId == 0L) null else dept.parentDepartmentId,
            processingFrom = dept.processingDateFrom.toInstant().toString(),
            processingThru = dept.processingDateTo.toInstant().toString()
        )
    }

    @PostMapping
    fun createDepartment(@RequestBody dto: DepartmentDto, @RequestParam companyId: Long): DepartmentDto {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val department = Department()
            department.companyId = companyId
            department.name = dto.name
            department.description = dto.description
            dto.parentDepartmentId?.let { department.parentDepartmentId = it }
            department.insert()

            DepartmentDto(
                id = department.id,
                name = department.name,
                description = department.description ?: "",
                parentDepartmentId = if (department.isParentDepartmentIdNull || department.parentDepartmentId == 0L) null else department.parentDepartmentId,
                processingFrom = department.processingDateFrom.toInstant().toString(),
                processingThru = department.processingDateTo.toInstant().toString()
            )
        }
    }

    @PutMapping("/{id}")
    fun updateDepartment(@PathVariable id: Long, @RequestBody dto: DepartmentDto): DepartmentDto {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val operation = DepartmentFinder.id().eq(id)
                .and(DepartmentFinder.processingDate().equalsInfinity())
            val department = DepartmentFinder.findOne(operation)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found")

            // Update properties - Reladomo handles unitemporal chaining
            department.name = dto.name
            department.description = dto.description
            dto.parentDepartmentId?.let { department.parentDepartmentId = it }

            DepartmentDto(
                id = department.id,
                name = department.name,
                description = department.description ?: "",
                parentDepartmentId = if (department.isParentDepartmentIdNull || department.parentDepartmentId == 0L) null else department.parentDepartmentId,
                processingFrom = department.processingDateFrom.toInstant().toString(),
                processingThru = department.processingDateTo.toInstant().toString()
            )
        }
    }

    @DeleteMapping("/{id}")
    fun deleteDepartment(@PathVariable id: Long) {
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val operation = DepartmentFinder.id().eq(id)
                .and(DepartmentFinder.processingDate().equalsInfinity())
            val department = DepartmentFinder.findOne(operation)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found")

            // Check if department is in use
            val assignmentsWithDepartment = EmployeeAssignmentFinder.findMany(
                EmployeeAssignmentFinder.departmentId().eq(id)
                    .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
            )

            if (assignmentsWithDepartment.isNotEmpty()) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete department: ${assignmentsWithDepartment.size} employees are currently assigned to this department"
                )
            }

            // Terminate the department (set PROCESSING_THRU to now)
            department.terminate()
        }
    }
}
