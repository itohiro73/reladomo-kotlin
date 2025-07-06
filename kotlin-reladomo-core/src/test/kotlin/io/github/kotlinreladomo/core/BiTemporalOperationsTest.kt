package io.github.kotlinreladomo.core

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.test.MithraTestResource
import io.github.kotlinreladomo.test.domain.Order
import io.github.kotlinreladomo.test.domain.OrderFinder
import io.github.kotlinreladomo.test.domain.OrderList
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Timestamp
import java.time.Instant
import java.math.BigDecimal

/**
 * Tests for bitemporal operations to verify correct Reladomo behavior
 */
class BiTemporalOperationsTest {
    
    companion object {
        private lateinit var mithraTestResource: MithraTestResource
        
        @BeforeAll
        @JvmStatic
        fun setUpBeforeClass() {
            val configFile = BiTemporalOperationsTest::class.java.classLoader
                .getResource("test-mithra-runtime.xml")?.path
                ?: throw IllegalStateException("test-mithra-runtime.xml not found")
                
            mithraTestResource = MithraTestResource(configFile)
            
            val connectionManager = TestConnectionManager.getInstance()
            connectionManager.setDefaultSource("test_db")
            connectionManager.createTables()
            
            mithraTestResource.setUp()
        }
        
        @AfterAll
        @JvmStatic
        fun tearDownAfterClass() {
            mithraTestResource.tearDown()
            TestConnectionManager.getInstance().dropTables()
        }
    }
    
    @BeforeEach
    fun setUp() {
        // Clear any existing data
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val allOrders = OrderFinder.findMany(OrderFinder.all())
            allOrders.terminateAll()
            null
        }
    }
    
    @Test
    fun `test insert bitemporal object`() {
        val orderId = 1001L
        val now = Instant.now()
        val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
        
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val order = Order()
            order.orderId = orderId
            order.customerId = 100L
            order.orderDate = Timestamp.from(now)
            order.amount = BigDecimal("99.99")
            order.status = "NEW"
            order.description = "Test order"
            
            // For new records, set business date to now and processing date to infinity
            order.businessDateFrom = Timestamp.from(now)
            order.businessDateTo = Timestamp.from(infinityDate)
            order.processingDateFrom = Timestamp.from(now)
            order.processingDateTo = Timestamp.from(infinityDate)
            
            order.insert()
            null
        }
        
        // Verify the insert
        val savedOrder = OrderFinder.findByPrimaryKey(
            orderId,
            Timestamp.from(now),
            Timestamp.from(now)
        )
        
        assertNotNull(savedOrder)
        assertEquals(orderId, savedOrder.orderId)
        assertEquals(100L, savedOrder.customerId)
        assertEquals("NEW", savedOrder.status)
    }
    
    @Test
    fun `test update bitemporal object using direct setters`() {
        val orderId = 1002L
        val now = Instant.now()
        val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
        
        // First insert an order
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val order = Order()
            order.orderId = orderId
            order.customerId = 100L
            order.orderDate = Timestamp.from(now)
            order.amount = BigDecimal("99.99")
            order.status = "NEW"
            order.description = "Original description"
            
            order.businessDateFrom = Timestamp.from(now)
            order.businessDateTo = Timestamp.from(infinityDate)
            order.processingDateFrom = Timestamp.from(now)
            order.processingDateTo = Timestamp.from(infinityDate)
            
            order.insert()
            null
        }
        
        Thread.sleep(100) // Small delay to ensure different timestamp
        
        // Update the order using direct setters
        val updateTime = Instant.now()
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val existingOrder = OrderFinder.findByPrimaryKey(
                orderId,
                Timestamp.from(updateTime),
                Timestamp.from(updateTime)
            )
            
            assertNotNull(existingOrder)
            
            // Update using direct setters - Reladomo handles bitemporal chaining
            existingOrder.status = "PROCESSING"
            existingOrder.amount = BigDecimal("149.99")
            existingOrder.description = "Updated description"
            
            null
        }
        
        // Verify the update worked
        val updatedOrder = OrderFinder.findByPrimaryKey(
            orderId,
            Timestamp.from(updateTime),
            Timestamp.from(updateTime)
        )
        
        assertNotNull(updatedOrder)
        assertEquals("PROCESSING", updatedOrder.status)
        assertEquals(BigDecimal("149.99"), updatedOrder.amount)
        assertEquals("Updated description", updatedOrder.description)
        
        // Verify we can still see the old version
        val historicalOrder = OrderFinder.findByPrimaryKey(
            orderId,
            Timestamp.from(now),
            Timestamp.from(now)
        )
        
        assertNotNull(historicalOrder)
        assertEquals("NEW", historicalOrder.status)
        assertEquals(BigDecimal("99.99"), historicalOrder.amount)
        assertEquals("Original description", historicalOrder.description)
    }
    
    @Test
    fun `test terminate bitemporal object`() {
        val orderId = 1003L
        val now = Instant.now()
        val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
        
        // First insert an order
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val order = Order()
            order.orderId = orderId
            order.customerId = 100L
            order.orderDate = Timestamp.from(now)
            order.amount = BigDecimal("99.99")
            order.status = "NEW"
            order.description = "To be terminated"
            
            order.businessDateFrom = Timestamp.from(now)
            order.businessDateTo = Timestamp.from(infinityDate)
            order.processingDateFrom = Timestamp.from(now)
            order.processingDateTo = Timestamp.from(infinityDate)
            
            order.insert()
            null
        }
        
        Thread.sleep(100)
        
        // Terminate the order
        val terminateTime = Instant.now()
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val existingOrder = OrderFinder.findByPrimaryKey(
                orderId,
                Timestamp.from(terminateTime),
                Timestamp.from(terminateTime)
            )
            
            assertNotNull(existingOrder)
            existingOrder.terminate()
            null
        }
        
        // Verify the order is terminated (not found at current time)
        val terminatedOrder = OrderFinder.findByPrimaryKey(
            orderId,
            Timestamp.from(terminateTime),
            Timestamp.from(terminateTime)
        )
        
        assertNull(terminatedOrder, "Order should not be found after termination")
        
        // But we can still see it historically
        val historicalOrder = OrderFinder.findByPrimaryKey(
            orderId,
            Timestamp.from(now),
            Timestamp.from(now)
        )
        
        assertNotNull(historicalOrder, "Order should still exist historically")
        assertEquals("NEW", historicalOrder.status)
    }
    
    @Test
    fun `test edge point queries for current data`() {
        val now = Instant.now()
        val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
        
        // Insert multiple orders
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            for (i in 1..3) {
                val order = Order()
                order.orderId = 2000L + i
                order.customerId = 100L
                order.orderDate = Timestamp.from(now)
                order.amount = BigDecimal("${i * 10}.99")
                order.status = "ACTIVE"
                order.description = "Order $i"
                
                order.businessDateFrom = Timestamp.from(now)
                order.businessDateTo = Timestamp.from(infinityDate)
                order.processingDateFrom = Timestamp.from(now)
                order.processingDateTo = Timestamp.from(infinityDate)
                
                order.insert()
            }
            null
        }
        
        // Query using edge point operations
        val operation = OrderFinder.businessDate().equalsEdgePoint()
            .and(OrderFinder.processingDate().equalsEdgePoint())
            .and(OrderFinder.status().eq("ACTIVE"))
        
        val activeOrders = OrderFinder.findMany(operation)
        
        assertEquals(3, activeOrders.size())
        activeOrders.forEach { order ->
            assertEquals("ACTIVE", order.status)
        }
    }
    
    @Test
    fun `test complex bitemporal scenario with multiple updates`() {
        val orderId = 3000L
        val t0 = Instant.now()
        val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
        
        // Create order
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val order = Order()
            order.orderId = orderId
            order.customerId = 100L
            order.orderDate = Timestamp.from(t0)
            order.amount = BigDecimal("50.00")
            order.status = "NEW"
            order.description = "Version 1"
            
            order.businessDateFrom = Timestamp.from(t0)
            order.businessDateTo = Timestamp.from(infinityDate)
            order.processingDateFrom = Timestamp.from(t0)
            order.processingDateTo = Timestamp.from(infinityDate)
            
            order.insert()
            null
        }
        
        Thread.sleep(100)
        val t1 = Instant.now()
        
        // First update
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val order = OrderFinder.findByPrimaryKey(orderId, Timestamp.from(t1), Timestamp.from(t1))
            order.status = "PROCESSING"
            order.amount = BigDecimal("75.00")
            order.description = "Version 2"
            null
        }
        
        Thread.sleep(100)
        val t2 = Instant.now()
        
        // Second update
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val order = OrderFinder.findByPrimaryKey(orderId, Timestamp.from(t2), Timestamp.from(t2))
            order.status = "COMPLETED"
            order.amount = BigDecimal("100.00")
            order.description = "Version 3"
            null
        }
        
        // Verify we can see all versions at their respective times
        val v1 = OrderFinder.findByPrimaryKey(orderId, Timestamp.from(t0), Timestamp.from(t0))
        assertEquals("NEW", v1.status)
        assertEquals("Version 1", v1.description)
        
        val v2 = OrderFinder.findByPrimaryKey(orderId, Timestamp.from(t1), Timestamp.from(t1))
        assertEquals("PROCESSING", v2.status)
        assertEquals("Version 2", v2.description)
        
        val v3 = OrderFinder.findByPrimaryKey(orderId, Timestamp.from(t2), Timestamp.from(t2))
        assertEquals("COMPLETED", v3.status)
        assertEquals("Version 3", v3.description)
        
        // Verify current version using edge point
        val current = OrderFinder.findOne(
            OrderFinder.orderId().eq(orderId)
                .and(OrderFinder.businessDate().equalsEdgePoint())
                .and(OrderFinder.processingDate().equalsEdgePoint())
        )
        assertEquals("COMPLETED", current.status)
        assertEquals("Version 3", current.description)
    }
    
    @Test
    fun `test incorrect update pattern throws exception`() {
        val orderId = 4000L
        val now = Instant.now()
        val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
        
        // Insert order
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { tx ->
            val order = Order()
            order.orderId = orderId
            order.customerId = 100L
            order.orderDate = Timestamp.from(now)
            order.amount = BigDecimal("50.00")
            order.status = "NEW"
            order.description = "Test"
            
            order.businessDateFrom = Timestamp.from(now)
            order.businessDateTo = Timestamp.from(infinityDate)
            order.processingDateFrom = Timestamp.from(now)
            order.processingDateTo = Timestamp.from(infinityDate)
            
            order.insert()
            null
        }
        
        // Try to update without transaction - should fail
        assertThrows<Exception> {
            val order = OrderFinder.findByPrimaryKey(orderId, Timestamp.from(now), Timestamp.from(now))
            order.status = "INVALID"
        }
    }
}