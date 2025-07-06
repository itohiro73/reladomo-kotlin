package io.github.kotlinreladomo.sample

import io.github.kotlinreladomo.sample.domain.kotlin.OrderKt
import io.github.kotlinreladomo.sample.repository.OrderSpringDataRepository
import io.github.kotlinreladomo.spring.repository.EnableReladomoRepositories
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import java.math.BigDecimal
import java.time.Instant

/**
 * Demo application showcasing Spring Boot integration features.
 */
@SpringBootApplication
@EnableReladomoRepositories(basePackages = ["io.github.kotlinreladomo.sample.repository"])
class SpringBootIntegrationDemo {
    
    @Bean
    fun runner(orderRepository: OrderSpringDataRepository): CommandLineRunner = CommandLineRunner {
        println("\n=== Kotlin Reladomo Spring Boot Integration Demo ===\n")
        
        // Create sample data
        println("1. Creating sample orders...")
        val now = Instant.now()
        
        val orders = listOf(
            OrderKt(null, 100, now, BigDecimal("150.00"), "PENDING", "Small order", now, now),
            OrderKt(null, 100, now, BigDecimal("250.00"), "SHIPPED", "Medium order", now, now),
            OrderKt(null, 100, now, BigDecimal("500.00"), "PENDING", "Large order", now, now),
            OrderKt(null, 200, now, BigDecimal("75.00"), "DELIVERED", "Tiny order", now, now),
            OrderKt(null, 200, now, BigDecimal("1000.00"), "PENDING", "Huge order", now, now),
            OrderKt(null, 300, now, BigDecimal("200.00"), "CANCELLED", null, now, now)
        )
        
        orders.forEach { orderRepository.save(it) }
        println("   Created ${orders.size} orders\n")
        
        // Demonstrate query methods
        println("2. Query Method Examples:")
        
        // Basic queries
        println("\n   a) Find by customer ID:")
        val customer100Orders = orderRepository.findByCustomerId(100)
        println("      Customer 100 has ${customer100Orders.size} orders")
        
        println("\n   b) Find by status:")
        val pendingOrders = orderRepository.findByStatus("PENDING")
        println("      ${pendingOrders.size} PENDING orders")
        
        println("\n   c) Find by customer and status:")
        val customer100Pending = orderRepository.findByCustomerIdAndStatus(100, "PENDING")
        println("      Customer 100 has ${customer100Pending.size} PENDING orders")
        
        // Comparison queries
        println("\n   d) Find orders > $200:")
        val largeOrders = orderRepository.findByAmountGreaterThan(BigDecimal("200.00"))
        largeOrders.forEach { println("      - Order ${it.orderId}: $${it.amount}") }
        
        println("\n   e) Find orders between $100 and $300:")
        val midRangeOrders = orderRepository.findByAmountBetween(BigDecimal("100.00"), BigDecimal("300.00"))
        println("      Found ${midRangeOrders.size} orders in range")
        
        // String operations
        println("\n   f) Find orders with descriptions containing 'order':")
        val orderDescriptions = orderRepository.findByDescriptionContaining("order")
        println("      Found ${orderDescriptions.size} orders")
        
        // IN queries
        println("\n   g) Find orders with status IN (PENDING, SHIPPED):")
        val activeOrders = orderRepository.findByStatusIn(listOf("PENDING", "SHIPPED"))
        println("      Found ${activeOrders.size} active orders")
        
        // Null handling
        println("\n   h) Find orders with null descriptions:")
        val nullDescOrders = orderRepository.findByDescriptionIsNull()
        println("      Found ${nullDescOrders.size} orders with null descriptions")
        
        // Count queries
        println("\n   i) Count orders by status:")
        val pendingCount = orderRepository.countByStatus("PENDING")
        println("      PENDING orders count: $pendingCount")
        
        // Exists queries
        println("\n   j) Check if customer has orders:")
        val customer100Exists = orderRepository.existsByCustomerId(100)
        val customer999Exists = orderRepository.existsByCustomerId(999)
        println("      Customer 100 has orders: $customer100Exists")
        println("      Customer 999 has orders: $customer999Exists")
        
        // Limit queries
        println("\n   k) Find top 5 orders by status:")
        val top5Pending = orderRepository.findFirst5ByStatus("PENDING")
        println("      Found ${top5Pending.size} orders (limited to 5)")
        
        // Complex queries
        println("\n   l) Complex query - Customer 100, amount > 200, status in (PENDING, SHIPPED):")
        val complexResult = orderRepository.findByCustomerIdAndAmountGreaterThanAndStatusIn(
            100, BigDecimal("200.00"), listOf("PENDING", "SHIPPED")
        )
        complexResult.forEach { 
            println("      - Order ${it.orderId}: ${it.status} - $${it.amount}")
        }
        
        // Temporal queries
        println("\n   m) Temporal query - Find orders as of specific date:")
        val historicalOrders = orderRepository.findByCustomerIdAsOf(100, now.minusSeconds(3600))
        println("      Found ${historicalOrders.size} historical orders")
        
        println("\n3. Configuration Examples:")
        println("   - Multi-datasource support configured via properties")
        println("   - Cache strategies (FULL, PARTIAL, NONE) configurable")
        println("   - Repository scanning with @EnableReladomoRepositories")
        println("   - Spring transaction integration")
        println("   - Query method support enabled by default")
        
        println("\n=== Demo Complete ===")
    }
}

fun main(args: Array<String>) {
    // Set up application properties programmatically for demo
    System.setProperty("spring.main.web-application-type", "none")
    System.setProperty("reladomo.kotlin.cache.type", "PARTIAL")
    System.setProperty("reladomo.kotlin.cache.timeout", "300")
    System.setProperty("reladomo.kotlin.repository.enable-query-methods", "true")
    
    SpringApplication.run(SpringBootIntegrationDemo::class.java, *args)
}