package io.github.reladomokotlin.sample.repository

import io.github.reladomokotlin.sample.config.TestReladomoConfiguration
import io.github.reladomokotlin.sample.domain.kotlin.OrderKt
import io.github.reladomokotlin.sample.domain.kotlin.CustomerKt
import io.github.reladomokotlin.sample.domain.kotlin.repository.OrderKtRepository
import io.github.reladomokotlin.sample.domain.kotlin.repository.CustomerKtRepository
import io.github.reladomokotlin.sample.domain.Customer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * Unit tests for OrderKtRepository to verify CRUD operations work correctly
 * with generated code and bitemporal support
 */
@SpringBootTest
@Import(TestReladomoConfiguration::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=none",
    "reladomo.kotlin.connection-manager-config-file=test-reladomo-runtime-config.xml",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:schema.sql"
])
@Transactional
class OrderKtRepositoryTest {

    @Autowired
    private lateinit var repository: OrderKtRepository
    
    @Autowired
    private lateinit var customerRepository: CustomerKtRepository

    private val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
    
    companion object {
        private val idGenerator = AtomicLong(10000) // Start from 10000 to avoid conflicts
    }

    @BeforeEach
    fun setUp() {
        // Test data will be initialized by TestDataInitializer
        // No need to create customers here as they're created globally
    }

    @Test
    fun `test create order`() {
        // Given
        val now = Instant.now()
        val order = OrderKt(
            orderId = idGenerator.incrementAndGet(),
            customerId = 100L,
            orderDate = now,
            amount = BigDecimal("99.99"),
            status = "NEW",
            description = "Test order",
            businessDate = now,
            processingDate = infinityDate
        )

        // When
        val savedOrder = repository.save(order)

        // Then
        assertNotNull(savedOrder)
        assertEquals(order.orderId, savedOrder.orderId)
        assertEquals(100L, savedOrder.customerId)
        assertEquals("NEW", savedOrder.status)
        assertEquals(BigDecimal("99.99"), savedOrder.amount)
        assertEquals("Test order", savedOrder.description)
    }

    @Test
    fun `test find by id`() {
        // Given
        val now = Instant.now()
        val order = OrderKt(
            orderId = idGenerator.incrementAndGet(),
            customerId = 100L,
            orderDate = now,
            amount = BigDecimal("199.99"),
            status = "PENDING",
            description = "Find test order",
            businessDate = now,
            processingDate = infinityDate
        )
        repository.save(order)

        // When
        val foundOrder = repository.findById(order.orderId!!)

        // Then
        assertNotNull(foundOrder)
        assertEquals(order.orderId, foundOrder?.orderId)
        assertEquals("PENDING", foundOrder?.status)
        assertEquals(BigDecimal("199.99"), foundOrder?.amount)
    }

    @Test
    fun `test update order`() {
        // Given
        val businessDate = Instant.now().minusSeconds(60) // 1 minute ago
        val order = OrderKt(
            orderId = idGenerator.incrementAndGet(),
            customerId = 100L,
            orderDate = businessDate,
            amount = BigDecimal("50.00"),
            status = "NEW",
            description = "Original description",
            businessDate = businessDate,
            processingDate = infinityDate
        )
        repository.save(order)

        // When - update with the same business date
        val updatedOrder = order.copy(
            status = "PROCESSING",
            amount = BigDecimal("75.00"),
            description = "Updated description"
        )
        val result = repository.update(updatedOrder, businessDate)

        // Then
        assertNotNull(result)
        assertEquals("PROCESSING", result.status)
        assertEquals(BigDecimal("75.00"), result.amount)
        assertEquals("Updated description", result.description)

        // Verify the update persisted
        val foundOrder = repository.findById(order.orderId!!)
        assertEquals("PROCESSING", foundOrder?.status)
        assertEquals(BigDecimal("75.00"), foundOrder?.amount)
    }

    @Test
    fun `test delete order`() {
        // Given
        val businessDate = Instant.now().minusSeconds(60) // 1 minute ago
        val order = OrderKt(
            orderId = idGenerator.incrementAndGet(),
            customerId = 100L,
            orderDate = businessDate,
            amount = BigDecimal("25.00"),
            status = "CANCELLED",
            description = "To be deleted",
            businessDate = businessDate,
            processingDate = infinityDate
        )
        repository.save(order)

        // Verify it exists
        assertNotNull(repository.findById(order.orderId!!))

        // When - delete with the same business date
        repository.deleteByIdAsOf(order.orderId!!, businessDate)

        // Then - for bitemporal, it should be terminated (not found at current time)
        val deletedOrder = repository.findById(order.orderId!!)
        assertNull(deletedOrder, "Order should not be found after deletion")
    }

    @Test
    fun `test find all orders`() {
        // Given
        val now = Instant.now()
        val orders = listOf(
            OrderKt(idGenerator.incrementAndGet(), 100L, now, BigDecimal("10.00"), "NEW", "Order 1", now, infinityDate),
            OrderKt(idGenerator.incrementAndGet(), 100L, now, BigDecimal("20.00"), "PROCESSING", "Order 2", now, infinityDate),
            OrderKt(idGenerator.incrementAndGet(), 101L, now, BigDecimal("30.00"), "COMPLETED", "Order 3", now, infinityDate)
        )
        orders.forEach { repository.save(it) }

        // When
        val allOrders = repository.findAll()

        // Then
        assertTrue(allOrders.size >= 3, "Should have at least 3 orders")
        val orderIds = allOrders.map { it.orderId }
        assertTrue(orders.all { order -> orderIds.contains(order.orderId) })
    }

    @Test
    fun `test find by customer id`() {
        // Given
        val now = Instant.now()
        val testCustomerId = 100L // Use existing customer
        val orders = listOf(
            OrderKt(idGenerator.incrementAndGet(), testCustomerId, now, BigDecimal("100.00"), "NEW", "Customer Order 1", now, infinityDate),
            OrderKt(idGenerator.incrementAndGet(), testCustomerId, now, BigDecimal("200.00"), "NEW", "Customer Order 2", now, infinityDate),
            OrderKt(idGenerator.incrementAndGet(), 101L, now, BigDecimal("300.00"), "NEW", "Other Customer Order", now, infinityDate)
        )
        orders.forEach { repository.save(it) }

        // When
        val customerOrders = repository.findByCustomerId(testCustomerId)

        // Then - we should find orders for this customer
        // Since equalsEdgePoint() seems to find all current records regardless of business date,
        // we'll find both the TestDataInitializer orders and our new ones
        assertTrue(customerOrders.size >= 2, "Should have at least the 2 orders we just created")
        assertTrue(customerOrders.all { it.customerId == testCustomerId })
    }

    @Test
    fun `test bitemporal find as of`() {
        // Given
        val baseTime = Instant.now()
        val order = OrderKt(
            orderId = idGenerator.incrementAndGet(),
            customerId = 100L,
            orderDate = baseTime,
            amount = BigDecimal("50.00"),
            status = "NEW",
            description = "Bitemporal test",
            businessDate = baseTime,
            processingDate = infinityDate
        )
        repository.save(order)

        // Update the order with the same business date (should update in place)
        val updatedOrder = order.copy(
            status = "UPDATED",
            amount = BigDecimal("100.00")
        )
        repository.update(updatedOrder, baseTime) // Update as of same business date

        // When - find current version
        val currentOrder = repository.findById(order.orderId!!)

        // Then - verify it was updated
        assertNotNull(currentOrder)
        assertEquals("UPDATED", currentOrder?.status)
        assertEquals(BigDecimal("100.00"), currentOrder?.amount)
        
        // Historical query at the same business date should also show updated version
        // since we updated in place
        val historicalOrder = repository.findByIdAsOf(order.orderId!!, baseTime, infinityDate)
        assertNotNull(historicalOrder)
        assertEquals("UPDATED", historicalOrder?.status)
    }

    @Test
    fun `test update non-existent order throws exception`() {
        // Given
        val now = Instant.now()
        val nonExistentOrder = OrderKt(
            orderId = 9999L,
            customerId = 100L,
            orderDate = now,
            amount = BigDecimal("50.00"),
            status = "GHOST",
            description = "Does not exist",
            businessDate = now,
            processingDate = infinityDate
        )

        // When/Then
        assertThrows(Exception::class.java) {
            repository.update(nonExistentOrder)
        }
    }

    @Test
    fun `test delete non-existent order throws exception`() {
        // When/Then
        assertThrows(Exception::class.java) {
            repository.deleteById(9999L)
        }
    }
}