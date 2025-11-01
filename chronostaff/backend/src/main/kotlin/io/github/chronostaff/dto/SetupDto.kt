package io.github.chronostaff.dto

/**
 * DTO for initial setup wizard
 */
data class SetupRequestDto(
    val companyName: String,
    val effectiveDate: String,  // ISO 8601 date string (YYYY-MM-DD)
    val positions: List<PositionCreateDto>,
    val departments: List<DepartmentCreateDto>
)

data class PositionCreateDto(
    val name: String,
    val level: Int,
    val description: String?
)

data class DepartmentCreateDto(
    val name: String,
    val description: String
)

data class SetupResponseDto(
    val companyId: Long,
    val companyName: String,
    val positions: List<PositionDto>,
    val departments: List<DepartmentDto>
)
