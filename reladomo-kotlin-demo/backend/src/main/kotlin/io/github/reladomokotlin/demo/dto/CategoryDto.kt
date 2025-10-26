package io.github.reladomokotlin.demo.dto

data class CategoryDto(
    val id: Long?,
    val name: String,
    val description: String?
)

data class CreateCategoryRequest(
    val name: String,
    val description: String?
)
