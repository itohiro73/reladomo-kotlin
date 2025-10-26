package io.github.chronostaff.dto

data class EmployeeDto(
    val id: Long,
    val employeeNumber: String,
    val name: String,
    val email: String,
    val hireDate: String,
    val processingFrom: String,
    val processingThru: String
)
