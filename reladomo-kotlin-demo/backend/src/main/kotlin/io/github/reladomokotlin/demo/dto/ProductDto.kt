package io.github.reladomokotlin.demo.dto

data class ProductDto(
    val id: Long?,
    val categoryId: Long,
    val categoryName: String?,
    val name: String,
    val description: String?
)

data class CreateProductRequest(
    val categoryId: Long,
    val name: String,
    val description: String?
)
