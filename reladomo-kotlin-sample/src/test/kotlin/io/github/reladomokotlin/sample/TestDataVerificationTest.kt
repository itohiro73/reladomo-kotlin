package io.github.reladomokotlin.sample

import io.github.reladomokotlin.sample.config.TestReladomoConfiguration
import io.github.reladomokotlin.sample.domain.OrderFinder
import io.github.reladomokotlin.sample.domain.CustomerFinder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.sql.Timestamp
import java.time.Instant

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
@ActiveProfiles("test")
class TestDataVerificationTest {
    
    @Test
    fun `verify test data is created`() {
        // Check customers
        val allCustomers = CustomerFinder.findMany(CustomerFinder.all())
        println("Total customers found: ${allCustomers.size}")
        allCustomers.forEach { 
            println("Customer: ${it.customerId} - ${it.name}")
        }
        
        // Check orders - bitemporal objects require as-of operations
        // We created orders with business date 2024-01-01, so we need to query as of that date
        val businessDate = Timestamp.from(Instant.parse("2024-01-01T10:00:00Z"))
        
        // Find all orders that were valid on the business date
        val allOrders = OrderFinder.findMany(
            OrderFinder.businessDate().eq(businessDate)
                .and(OrderFinder.processingDate().equalsInfinity())
        )
        println("\nTotal orders found: ${allOrders.size}")
        allOrders.forEach {
            println("Order: ${it.orderId} - Customer: ${it.customerId} - Status: ${it.status}")
            println("  BusinessDate: ${it.businessDate} - ProcessingDate: ${it.processingDate}")
        }
        
        // Specific checks
        val customer100 = CustomerFinder.findByPrimaryKey(100L)
        assertNotNull(customer100, "Customer 100 should exist")
        assertEquals("Test Customer 100", customer100.name)
        
        // Find order by primary key with the correct dates
        val processingInfinity = OrderFinder.processingDate().getInfinityDate()
        val order1 = OrderFinder.findByPrimaryKey(1L, businessDate, processingInfinity)
        assertNotNull(order1, "Order 1 should exist")
        assertEquals(100L, order1.customerId)
        assertEquals("PENDING", order1.status)
    }
}