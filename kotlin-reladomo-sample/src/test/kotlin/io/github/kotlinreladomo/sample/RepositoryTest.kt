package io.github.kotlinreladomo.sample

import io.github.kotlinreladomo.core.exceptions.EntityNotFoundException
import io.github.kotlinreladomo.sample.domain.kotlin.OrderKt
import io.github.kotlinreladomo.sample.repository.OrderKtRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the in-memory repository implementation.
 */
class RepositoryTest {
    
    private val repository = OrderKtRepository()
    
    @Test
    fun `should find order by id`() {
        // Given: repository is initialized with sample data
        
        // When
        val order = repository.findById(1L)
        
        // Then
        assertNotNull(order)
        assertEquals(1L, order.orderId)
        assertEquals(100L, order.customerId)
        assertEquals("PENDING", order.status)
    }
    
    @Test
    fun `should return null for non-existent order`() {
        // When
        val order = repository.findById(999L)
        
        // Then
        assertNull(order)
    }
    
    @Test
    fun `should save new order`() {
        // Given
        val newOrder = OrderKt(
            orderId = 100L,
            customerId = 300L,
            orderDate = Instant.now(),
            amount = BigDecimal("299.99"),
            status = "NEW",
            description = "Test order",
            businessDate = Instant.now(),
            processingDate = Instant.now()
        )
        
        // When
        val saved = repository.save(newOrder)
        
        // Then
        assertEquals(newOrder.orderId, saved.orderId)
        assertNotNull(repository.findById(100L))
    }
    
    @Test
    fun `should update existing order`() {
        // Given
        val existing = repository.findById(1L)!!
        val updated = existing.copy(
            status = "COMPLETED",
            amount = BigDecimal("1999.99")
        )
        
        // When
        val result = repository.update(updated)
        
        // Then
        assertEquals("COMPLETED", result.status)
        assertEquals(BigDecimal("1999.99"), result.amount)
        
        val fromDb = repository.findById(1L)!!
        assertEquals("COMPLETED", fromDb.status)
    }
    
    @Test
    fun `should throw exception when updating non-existent order`() {
        // Given
        val nonExistent = OrderKt(
            orderId = 999L,
            customerId = 100L,
            orderDate = Instant.now(),
            amount = BigDecimal("100.00"),
            status = "TEST",
            description = null,
            businessDate = Instant.now(),
            processingDate = Instant.now()
        )
        
        // When/Then
        assertThrows<EntityNotFoundException> {
            repository.update(nonExistent)
        }
    }
    
    @Test
    fun `should delete order by id`() {
        // Given
        assertNotNull(repository.findById(3L))
        
        // When
        repository.deleteById(3L)
        
        // Then
        assertNull(repository.findById(3L))
    }
    
    @Test
    fun `should find orders by customer id`() {
        // When
        val orders = repository.findByCustomerId(100L)
        
        // Then
        assertEquals(2, orders.size)
        orders.forEach { order ->
            assertEquals(100L, order.customerId)
        }
    }
    
    @Test
    fun `should return empty list for customer with no orders`() {
        // When
        val orders = repository.findByCustomerId(999L)
        
        // Then
        assertEquals(0, orders.size)
    }
    
    @Test
    fun `should find all orders`() {
        // When
        val orders = repository.findAll()
        
        // Then
        assertEquals(3, orders.size)
    }
}