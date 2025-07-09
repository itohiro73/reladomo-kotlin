package io.github.reladomokotlin.sample.dto

import java.math.BigDecimal
import java.time.Instant

data class OrderDto(
    val orderId: Long,
    val customerId: Long,
    val orderDate: Instant,
    val amount: BigDecimal,
    val status: String,
    val description: String?,
    val businessDate: Instant,
    val processingDate: Instant
)

data class CreateOrderRequest(
    val customerId: Long,
    val amount: BigDecimal,
    val status: String = "PENDING",
    val description: String? = null
)