package io.github.reladomokotlin.sample.repository

import io.github.reladomokotlin.sample.config.BaseRepositoryTest
import io.github.reladomokotlin.sample.domain.kotlin.CustomerKt
import io.github.reladomokotlin.sample.domain.kotlin.OrderKt
import io.github.reladomokotlin.sample.domain.kotlin.ProductKt
import io.github.reladomokotlin.sample.domain.kotlin.repository.CustomerKtRepository
import io.github.reladomokotlin.sample.domain.kotlin.repository.OrderKtRepository
import io.github.reladomokotlin.sample.domain.kotlin.repository.ProductKtRepository
import io.github.reladomokotlin.sequence.SequenceGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.Instant

@TestPropertySource(properties = [
    "reladomo.sequence.enabled=true",
    "reladomo.sequence.type=IN_MEMORY",
    "reladomo.sequence.default-start-value=2000",
    "reladomo.sequence.increment-by=1"
])
class RepositorySequenceIntegrationTest : BaseRepositoryTest() {
    
    @Autowired
    private lateinit var orderRepository: OrderKtRepository
    
    @Autowired
    private lateinit var customerRepository: CustomerKtRepository
    
    @Autowired
    private lateinit var productRepository: ProductKtRepository
    
    @Autowired(required = false)
    private var sequenceGenerator: SequenceGenerator? = null
    
    @BeforeEach
    fun setupTestData() {
        // Create test customer for order tests
        try {
            customerRepository.save(CustomerKt(100L, "Test Customer", "test@example.com", null, null, Instant.now()))
        } catch (e: Exception) {
            // Ignore if already exists
        }
    }
    
    @Test
    fun `sequence generator should be available`() {
        assertNotNull(sequenceGenerator, "Sequence generator should be autowired")
    }
    
    @Test
    fun `should generate ID for order when not provided`() {
        val order = OrderKt(
            orderId = null, // No ID provided
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("999.99"),
            status = "PENDING",
            description = "Test order",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        val savedOrder = orderRepository.save(order)
        
        assertNotNull(savedOrder.orderId)
        assertTrue(savedOrder.orderId!! >= 1000L, "Generated ID should be >= 1000 (sequence start value)")
    }
    
    @Test
    fun `should generate ID for order when ID is 0`() {
        val order = OrderKt(
            orderId = 0L, // ID is 0, should be treated as not provided
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("1234.56"),
            status = "PENDING",
            description = "Test order with 0 ID",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        val savedOrder = orderRepository.save(order)
        
        assertNotNull(savedOrder.orderId)
        assertNotEquals(0L, savedOrder.orderId)
        assertTrue(savedOrder.orderId!! >= 1000L, "Generated ID should be >= 1000 (sequence start value)")
    }
    
    @Test
    fun `should use provided ID when given`() {
        val specificId = 5555L
        val order = OrderKt(
            orderId = specificId,
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("777.77"),
            status = "COMPLETED",
            description = "Test order with specific ID",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        val savedOrder = orderRepository.save(order)
        
        assertEquals(specificId, savedOrder.orderId)
    }
    
    @Test
    fun `should generate sequential IDs for multiple orders`() {
        val orders = (1..3).map { i ->
            OrderKt(
                orderId = null,
                customerId = 100,
                orderDate = Instant.now(),
                amount = BigDecimal("100.00").multiply(BigDecimal(i)),
                status = "PENDING",
                description = "Test order $i",
                businessDate = Instant.now(),
                processingDate = Instant.parse("9999-12-01T23:59:00Z")
            )
        }
        
        val savedOrders = orders.map { orderRepository.save(it) }
        val ids = savedOrders.mapNotNull { it.orderId }
        
        assertEquals(3, ids.size)
        // Check that IDs are sequential
        val sortedIds = ids.sorted()
        assertEquals(sortedIds[0] + 1, sortedIds[1], "IDs should be sequential")
        assertEquals(sortedIds[1] + 1, sortedIds[2], "IDs should be sequential")
    }
    
    @Test
    fun `should generate ID for customer when not provided`() {
        val customer = CustomerKt(
            customerId = null,
            name = "Test Customer",
            email = "test@example.com",
            phone = "555-0123",
            address = "123 Test St",
            createdDate = Instant.now()
        )
        
        val savedCustomer = customerRepository.save(customer)
        
        assertNotNull(savedCustomer.customerId)
        assertTrue(savedCustomer.customerId!! >= 1000L, "Generated ID should be >= 1000 (sequence start value)")
    }
    
    @Test
    fun `should generate ID for product when not provided`() {
        val product = ProductKt(
            productId = null,
            name = "Test Product",
            description = "A test product",
            price = BigDecimal("49.99"),
            stockQuantity = 100,
            category = "Test"
        )
        
        val savedProduct = productRepository.save(product)
        
        assertNotNull(savedProduct.productId)
        assertTrue(savedProduct.productId!! >= 1000L, "Generated ID should be >= 1000 (sequence start value)")
    }
    
    @Test
    fun `different entities should use separate sequences`() {
        // Create entities without IDs
        val order = OrderKt(
            orderId = null,
            customerId = 100,
            orderDate = Instant.now(),
            amount = BigDecimal("100.00"),
            status = "PENDING",
            description = "Test",
            businessDate = Instant.now(),
            processingDate = Instant.parse("9999-12-01T23:59:00Z")
        )
        
        val customer = CustomerKt(
            customerId = null,
            name = "Test",
            email = "test@example.com",
            phone = null,
            address = null,
            createdDate = Instant.now()
        )
        
        val product = ProductKt(
            productId = null,
            name = "Test",
            description = null,
            price = BigDecimal("10.00"),
            stockQuantity = 1,
            category = null
        )
        
        // Save them
        val savedOrder = orderRepository.save(order)
        val savedCustomer = customerRepository.save(customer)
        val savedProduct = productRepository.save(product)
        
        // Each should start from 2000 (separate sequences)
        assertEquals(2000L, savedOrder.orderId)
        assertEquals(2000L, savedCustomer.customerId)
        assertEquals(2000L, savedProduct.productId)
    }
}