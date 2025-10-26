package io.github.chronostaff.dto

data class PositionDto(
    val id: Long,
    val name: String,
    val level: Int,
    val description: String?
)
