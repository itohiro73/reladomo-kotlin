package io.github.kotlinreladomo.sample.repository

import io.github.kotlinreladomo.sample.config.BaseRepositoryTest
import io.github.kotlinreladomo.sample.domain.kotlin.OrderKt
import io.github.kotlinreladomo.sample.domain.kotlin.CustomerKt
import io.github.kotlinreladomo.sample.domain.kotlin.repository.OrderKtRepository
import io.github.kotlinreladomo.sample.domain.kotlin.repository.CustomerKtRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.Instant

@TestPropertySource(properties = [
    "reladomo.sequence.enabled=false" // Sequence generation disabled
])
class RepositoryWithoutSequenceTest : BaseRepositoryTest() {
    
    @Autowired
    private lateinit var orderRepository: OrderKtRepository
    
    @Autowired
    private lateinit var customerRepository: CustomerKtRepository
    
    @BeforeEach
    fun setupTestData() {
        // Create test customer
        try {
            customerRepository.save(CustomerKt(100L, "Test Customer", "test@example.com", null, null, Instant.now()))
        } catch (e: Exception) {
            // Ignore if already exists
        }
    }
    
    @Test
    fun `should throw exception when no ID provided and sequence generator disabled`() {
        val order = OrderKt(
            orderId = null, // No ID provided
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("999.99"),
            status = "PENDING",
            description = "Test order without sequence",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        val exception = assertThrows<IllegalStateException> {
            orderRepository.save(order)
        }
        
        assertEquals("No ID provided and sequence generator not available", exception.message)
    }
    
    @Test
    fun `should throw exception when ID is 0 and sequence generator disabled`() {
        val order = OrderKt(
            orderId = 0L, // ID is 0
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("999.99"),
            status = "PENDING",
            description = "Test order with 0 ID",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        val exception = assertThrows<IllegalStateException> {
            orderRepository.save(order)
        }
        
        assertEquals("No ID provided and sequence generator not available", exception.message)
    }
    
    @Test
    fun `should save successfully when ID is provided and sequence generator disabled`() {
        val providedId = 9999L
        val order = OrderKt(
            orderId = providedId,
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("999.99"),
            status = "PENDING",
            description = "Test order with explicit ID",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        val savedOrder = orderRepository.save(order)
        
        assertEquals(providedId, savedOrder.orderId)
        assertEquals("Test order with explicit ID", savedOrder.description)
    }
    
    @Test
    fun `should handle update operations normally without sequence generator`() {
        // First create an order with explicit ID
        val order = OrderKt(
            orderId = 8888L,
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("100.00"),
            status = "PENDING",
            description = "Original description",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        val savedOrder = orderRepository.save(order)
        
        // Update the order
        val updatedOrder = savedOrder.copy(
            amount = BigDecimal("200.00"),
            status = "COMPLETED",
            description = "Updated description"
        )
        
        val result = orderRepository.update(updatedOrder)
        
        assertEquals(8888L, result.orderId)
        assertEquals(BigDecimal("200.00"), result.amount)
        assertEquals("COMPLETED", result.status)
        assertEquals("Updated description", result.description)
    }
    
    @Test
    fun `should handle find and delete operations normally without sequence generator`() {
        // Create with explicit ID
        val order = OrderKt(
            orderId = 7777L,
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("500.00"),
            status = "PENDING",
            description = "Order to delete",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        orderRepository.save(order)
        
        // Find by ID should work
        val found = orderRepository.findById(7777L)
        assertNotNull(found)
        assertEquals(7777L, found?.orderId)
        
        // Delete should work
        assertDoesNotThrow {
            orderRepository.deleteById(7777L)
        }
        
        // Should not find after delete
        val afterDelete = orderRepository.findById(7777L)
        assertNull(afterDelete)
    }
}