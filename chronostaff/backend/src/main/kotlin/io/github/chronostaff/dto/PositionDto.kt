package io.github.chronostaff.dto

data class PositionDto(
    val id: Long,
    val name: String,
    val level: Int,
    val description: String?,
    val businessFrom: String,
    val businessThru: String,
    val processingFrom: String,
    val processingThru: String
)
