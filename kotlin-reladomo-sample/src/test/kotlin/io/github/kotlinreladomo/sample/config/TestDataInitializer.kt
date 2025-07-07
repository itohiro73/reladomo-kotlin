package io.github.kotlinreladomo.sample.config

import io.github.kotlinreladomo.sample.domain.Customer
import io.github.kotlinreladomo.sample.domain.CustomerFinder
import io.github.kotlinreladomo.sample.domain.Order
import io.github.kotlinreladomo.sample.domain.OrderFinder
import io.github.kotlinreladomo.sample.domain.Product
import io.github.kotlinreladomo.sample.domain.ProductFinder
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation
import com.gs.fw.common.mithra.MithraManagerProvider
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

/**
 * Test data initializer that creates sample data for integration tests.
 * This ensures consistent test data across all test runs.
 */
@TestConfiguration
@Profile("test")
class TestDataInitializer(
    private val dataSource: DataSource
) {
    
    private val logger = LoggerFactory.getLogger(TestDataInitializer::class.java)
    
    @PostConstruct
    fun initializeTestData() {
        logger.info("Initializing test data...")
        
        // Use Reladomo's transaction manager
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            try {
                // Create test customers
                createCustomer(100L, "Test Customer 100", "customer100@example.com", "555-0100", "100 Test St")
                createCustomer(101L, "Test Customer 101", "customer101@example.com", "555-0101", "101 Test St") 
                createCustomer(200L, "Test Customer 200", "customer200@example.com", "555-0200", "200 Test St")
                createCustomer(300L, "Test Customer 300", "customer300@example.com", "555-0300", "300 Test St")
                
                // Create test products
                createProduct(1L, "Test Product 1", "First test product", BigDecimal("99.99"), 100, "TEST")
                createProduct(2L, "Test Product 2", "Second test product", BigDecimal("149.99"), 50, "TEST")
                createProduct(3L, "Test Product 3", "Third test product", BigDecimal("199.99"), 25, "TEST")
                
                // Create test orders
                val now = Instant.now()
                // Use a specific business date for orders (e.g., start of the year)
                val businessDate = Instant.parse("2024-01-01T10:00:00Z")
                createOrder(1L, 100L, now, BigDecimal("99.99"), "PENDING", "Test Order 1", businessDate)
                createOrder(2L, 100L, now, BigDecimal("149.99"), "PROCESSING", "Test Order 2", businessDate)
                createOrder(3L, 101L, now, BigDecimal("199.99"), "COMPLETED", "Test Order 3", businessDate)
                
                logger.info("Test data initialization completed")
                null
            } catch (e: Exception) {
                logger.warn("Test data initialization failed (may already exist): ${e.message}")
                tx.rollback()
                null
            }
        }
    }
    
    private fun createCustomer(id: Long, name: String, email: String, phone: String, address: String) {
        try {
            // Check if customer already exists
            val existing = CustomerFinder.findByPrimaryKey(id)
            if (existing != null) {
                logger.debug("Customer $id already exists")
                return
            }
            
            val customer = Customer()
            customer.customerId = id
            customer.name = name
            customer.email = email
            customer.phone = phone
            customer.address = address
            customer.createdDate = Timestamp.from(Instant.now())
            customer.insert()
            logger.debug("Created test customer: $id")
        } catch (e: Exception) {
            logger.debug("Customer $id may already exist: ${e.message}")
        }
    }
    
    private fun createProduct(id: Long, name: String, description: String, price: BigDecimal, stock: Int, category: String) {
        try {
            // Check if product already exists
            val existing = ProductFinder.findByPrimaryKey(id)
            if (existing != null) {
                logger.debug("Product $id already exists")
                return
            }
            
            val product = Product()
            product.productId = id
            product.name = name
            product.description = description
            product.price = price
            product.stockQuantity = stock
            product.category = category
            product.insert()
            logger.debug("Created test product: $id")
        } catch (e: Exception) {
            logger.debug("Product $id may already exist: ${e.message}")
        }
    }
    
    private fun createOrder(id: Long, customerId: Long, orderDate: Instant, amount: BigDecimal, 
                           status: String, description: String, businessDate: Instant) {
        try {
            // Check if order already exists
            val businessTimestamp = Timestamp.from(businessDate)
            val processingInfinity = OrderFinder.processingDate().getInfinityDate()
            val existing = OrderFinder.findByPrimaryKey(id, businessTimestamp, processingInfinity)
            if (existing != null) {
                logger.debug("Order $id already exists")
                return
            }
            
            val order = Order(businessTimestamp)
            order.orderId = id
            order.customerId = customerId
            order.orderDate = Timestamp.from(orderDate)
            order.amount = amount
            order.status = status
            order.description = description
            order.insert()
            logger.info("Created test order: $id with businessDate: ${order.businessDate} and processingDate: ${order.processingDate}")
        } catch (e: Exception) {
            logger.warn("Failed to create order $id: ${e.message}", e)
        }
    }
}