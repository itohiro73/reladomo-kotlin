package io.github.chronostaff.dto

import java.math.BigDecimal

data class SalaryDto(
    val id: Long,
    val employeeId: Long,
    val amount: BigDecimal,
    val currency: String,
    val updatedBy: String,
    val businessFrom: String,
    val businessThru: String,
    val processingFrom: String,
    val processingThru: String
)
