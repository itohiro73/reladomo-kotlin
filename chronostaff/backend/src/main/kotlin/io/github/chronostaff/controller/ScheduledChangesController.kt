package io.github.chronostaff.controller

import io.github.chronostaff.domain.*
import io.github.chronostaff.dto.ChangeDetails
import io.github.chronostaff.dto.ScheduledChangeDto
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/changes")
class ScheduledChangesController {

    /**
     * Get all scheduled changes (future-dated organizational changes)
     * @param companyId Company ID to filter changes
     * @return List of scheduled changes sorted by effective date
     */
    @GetMapping("/scheduled")
    fun getScheduledChanges(@RequestParam companyId: Long): List<ScheduledChangeDto> {
        // Get current date in JST (start of today)
        val today = LocalDate.now(ZoneId.of("Asia/Tokyo"))
        val todayInstant = today.atStartOfDay().toInstant(ZoneOffset.ofHours(9))

        val changes = mutableListOf<ScheduledChangeDto>()

        // 1. Find future-dated employee assignments (transfers)
        val allAssignments = EmployeeAssignmentFinder.findMany(
            EmployeeAssignmentFinder.businessDate().equalsEdgePoint()
                .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
        )

        allAssignments.filter { it.businessDateFrom.toInstant().isAfter(todayInstant) }
            .forEach { assignment ->
                val employee = EmployeeFinder.findOne(
                    EmployeeFinder.id().eq(assignment.employeeId)
                        .and(EmployeeFinder.processingDate().equalsInfinity())
                )

                // Get current assignment (most recent before this change)
                val currentAssignment = allAssignments
                    .filter { it.employeeId == assignment.employeeId }
                    .filter { it.businessDateFrom.toInstant().isBefore(assignment.businessDateFrom.toInstant()) }
                    .maxByOrNull { it.businessDateFrom.toInstant() }

                val toDept = DepartmentFinder.findOne(
                    DepartmentFinder.id().eq(assignment.departmentId)
                        .and(DepartmentFinder.businessDate().equalsInfinity())
                        .and(DepartmentFinder.processingDate().equalsInfinity())
                )

                val fromDept = currentAssignment?.let { curr ->
                    DepartmentFinder.findOne(
                        DepartmentFinder.id().eq(curr.departmentId)
                            .and(DepartmentFinder.businessDate().equalsInfinity())
                            .and(DepartmentFinder.processingDate().equalsInfinity())
                    )
                }

                val toPos = PositionFinder.findOne(
                    PositionFinder.id().eq(assignment.positionId)
                        .and(PositionFinder.businessDate().equalsInfinity())
                        .and(PositionFinder.processingDate().equalsInfinity())
                )

                val fromPos = currentAssignment?.let { curr ->
                    PositionFinder.findOne(
                        PositionFinder.id().eq(curr.positionId)
                            .and(PositionFinder.businessDate().equalsInfinity())
                            .and(PositionFinder.processingDate().equalsInfinity())
                    )
                }

                employee?.let { emp ->
                    changes.add(
                        ScheduledChangeDto(
                            effectiveDate = assignment.businessDateFrom.toInstant().toString(),
                            changeType = "TRANSFER",
                            entityType = "EMPLOYEE",
                            entityId = emp.id,
                            entityName = emp.name,
                            details = ChangeDetails.Transfer(
                                fromDepartmentId = currentAssignment?.departmentId,
                                fromDepartmentName = fromDept?.name,
                                toDepartmentId = assignment.departmentId,
                                toDepartmentName = toDept?.name ?: "Unknown",
                                fromPositionId = currentAssignment?.positionId,
                                fromPositionName = fromPos?.name,
                                toPositionId = assignment.positionId,
                                toPositionName = toPos?.name ?: "Unknown"
                            ),
                            recordId = assignment.id
                        )
                    )
                }
            }

        // 2. Find future-dated salaries
        val allSalaries = SalaryFinder.findMany(
            SalaryFinder.businessDate().equalsEdgePoint()
                .and(SalaryFinder.processingDate().equalsInfinity())
        )

        allSalaries.filter { it.businessDateFrom.toInstant().isAfter(todayInstant) }
            .forEach { salary ->
                val employee = EmployeeFinder.findOne(
                    EmployeeFinder.id().eq(salary.employeeId)
                        .and(EmployeeFinder.processingDate().equalsInfinity())
                )

                val currentSalary = allSalaries
                    .filter { it.employeeId == salary.employeeId }
                    .filter { it.businessDateFrom.toInstant().isBefore(salary.businessDateFrom.toInstant()) }
                    .maxByOrNull { it.businessDateFrom.toInstant() }

                employee?.let { emp ->
                    changes.add(
                        ScheduledChangeDto(
                            effectiveDate = salary.businessDateFrom.toInstant().toString(),
                            changeType = "SALARY",
                            entityType = "EMPLOYEE",
                            entityId = emp.id,
                            entityName = emp.name,
                            details = ChangeDetails.SalaryAdjustment(
                                fromAmount = currentSalary?.amount?.toString(),
                                toAmount = salary.amount.toString()
                            ),
                            recordId = salary.id
                        )
                    )
                }
            }

        // 3. Find future-dated departments (new departments)
        val allDepartments = DepartmentFinder.findMany(
            DepartmentFinder.companyId().eq(companyId)
                .and(DepartmentFinder.businessDate().equalsEdgePoint())
                .and(DepartmentFinder.processingDate().equalsInfinity())
        )

        allDepartments.filter { it.businessDateFrom.toInstant().isAfter(todayInstant) }
            .forEach { dept ->
                val parentDept = if (!dept.isParentDepartmentIdNull && dept.parentDepartmentId != 0L) {
                    DepartmentFinder.findOne(
                        DepartmentFinder.id().eq(dept.parentDepartmentId)
                            .and(DepartmentFinder.businessDate().equalsInfinity())
                            .and(DepartmentFinder.processingDate().equalsInfinity())
                    )
                } else null

                changes.add(
                    ScheduledChangeDto(
                        effectiveDate = dept.businessDateFrom.toInstant().toString(),
                        changeType = "DEPARTMENT",
                        entityType = "DEPARTMENT",
                        entityId = dept.id,
                        entityName = dept.name,
                        details = ChangeDetails.DepartmentCreation(
                            departmentName = dept.name,
                            description = if (dept.isDescriptionNull) null else dept.description,
                            parentDepartmentId = if (dept.isParentDepartmentIdNull || dept.parentDepartmentId == 0L) null else dept.parentDepartmentId,
                            parentDepartmentName = parentDept?.name
                        ),
                        recordId = dept.id
                    )
                )
            }

        // 4. Find future-dated positions (new positions)
        val allPositions = PositionFinder.findMany(
            PositionFinder.companyId().eq(companyId)
                .and(PositionFinder.businessDate().equalsEdgePoint())
                .and(PositionFinder.processingDate().equalsInfinity())
        )

        allPositions.filter { it.businessDateFrom.toInstant().isAfter(todayInstant) }
            .forEach { pos ->
                changes.add(
                    ScheduledChangeDto(
                        effectiveDate = pos.businessDateFrom.toInstant().toString(),
                        changeType = "POSITION",
                        entityType = "POSITION",
                        entityId = pos.id,
                        entityName = pos.name,
                        details = ChangeDetails.PositionCreation(
                            positionName = pos.name,
                            level = pos.level,
                            description = if (pos.isDescriptionNull) null else pos.description
                        ),
                        recordId = pos.id
                    )
                )
            }

        // Sort by effective date
        return changes.sortedBy { Instant.parse(it.effectiveDate) }
    }
}
