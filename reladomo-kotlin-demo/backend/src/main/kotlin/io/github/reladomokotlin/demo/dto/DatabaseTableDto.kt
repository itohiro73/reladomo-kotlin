package io.github.reladomokotlin.demo.dto

data class DatabaseTableDto(
    val name: String,
    val columns: List<String>,
    val rows: List<Map<String, Any?>>
)
