package io.github.chronostaff.dto

data class OrganizationSnapshotDto(
    val asOfDate: String,  // YYYY-MM-DD format
    val departments: List<DepartmentSnapshotDto>
)

data class DepartmentSnapshotDto(
    val id: Long,
    val name: String,
    val employees: List<EmployeeSnapshotDto>
)

data class EmployeeSnapshotDto(
    val id: Long,
    val name: String,
    val positionId: Long,
    val positionName: String,
    val positionLevel: Int
)
