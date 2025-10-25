package io.github.reladomokotlin.demo.dto

import java.math.BigDecimal
import java.time.Instant

data class ProductPriceDto(
    val id: Long,
    val productId: Long,
    val productName: String?,
    val price: BigDecimal,
    val businessFrom: Instant,
    val businessThru: Instant,
    val processingFrom: Instant,
    val processingThru: Instant
)

data class CreateProductPriceRequest(
    val productId: Long,
    val price: BigDecimal,
    val businessDate: String  // ISO-8601 format - when this price becomes effective
)

data class DatabaseRowDto(
    val tableName: String,
    val columns: Map<String, Any?>
)
