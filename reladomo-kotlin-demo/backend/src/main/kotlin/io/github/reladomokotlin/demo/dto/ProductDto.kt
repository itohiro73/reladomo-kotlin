package io.github.reladomokotlin.demo.dto

import java.time.Instant

data class ProductDto(
    val id: Long?,
    val categoryId: Long,
    val categoryName: String?,
    val name: String,
    val description: String?,
    val processingFrom: Instant? = null,
    val processingThru: Instant? = null
)

data class CreateProductRequest(
    val categoryId: Long,
    val name: String,
    val description: String?
)
