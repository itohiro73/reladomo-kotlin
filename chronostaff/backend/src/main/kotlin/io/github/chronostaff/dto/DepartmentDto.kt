package io.github.chronostaff.dto

data class DepartmentDto(
    val id: Long,
    val name: String,
    val description: String?,
    val parentDepartmentId: Long?,
    val businessFrom: String,
    val businessThru: String,
    val processingFrom: String,
    val processingThru: String
)
