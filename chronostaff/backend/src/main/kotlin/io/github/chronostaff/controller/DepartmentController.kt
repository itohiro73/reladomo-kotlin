package io.github.chronostaff.controller

import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp
import io.github.chronostaff.domain.DepartmentFinder
import io.github.chronostaff.dto.DepartmentDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.Timestamp

@RestController
@RequestMapping("/api/departments")
class DepartmentController {

    @GetMapping
    fun getAllDepartments(): List<DepartmentDto> {
        // Get current departments (processingDate at infinity)
        val operation = DepartmentFinder.processingDate().equalsEdgePoint()
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
            .and(DepartmentFinder.processingDate().equalsEdgePoint())
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

    // TODO: Implement history endpoint - requires proper Reladomo temporal query API
    // @GetMapping("/{id}/history")
    // fun getDepartmentHistory(@PathVariable id: Long): List<DepartmentDto>
}
