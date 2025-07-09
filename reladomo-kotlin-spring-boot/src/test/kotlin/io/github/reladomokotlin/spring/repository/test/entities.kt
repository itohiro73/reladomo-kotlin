package io.github.reladomokotlin.spring.repository.test

import io.github.reladomokotlin.core.BiTemporalEntity
import java.time.Instant

// Test entities - moved to separate file to avoid inner class issues
data class TestOrder(
    val orderId: Long?,
    val customerId: Long,
    val status: String,
    override val businessDate: Instant,
    override val processingDate: Instant
) : BiTemporalEntity

data class TestCustomer(
    val customerId: Long?,
    val email: String,
    val firstName: String,
    val lastName: String
)

data class TestProduct(
    val productId: String,
    val name: String,
    val category: String,
    val price: Double
)