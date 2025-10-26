package io.github.chronostaff.dto

data class DepartmentDto(
    val id: Long,
    val name: String,
    val description: String?,
    val parentDepartmentId: Long?,
    val processingFrom: String,
    val processingThru: String
)
