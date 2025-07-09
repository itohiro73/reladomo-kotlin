package io.github.reladomokotlin.sample.query

import io.github.reladomokotlin.sample.domain.kotlin.OrderKt
import io.github.reladomokotlin.sample.domain.kotlin.repository.OrderKtRepository
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.customerId
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.amount
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.status
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.orderDate
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.time.Instant

/**
 * Example application demonstrating the Query DSL functionality
 * This is excluded from tests via @Profile annotation
 */
@SpringBootApplication
@Profile("demo") // Only run when demo profile is active
class QueryDslExampleApp {
    
    @Bean
    fun runner(orderRepository: OrderKtRepository): CommandLineRunner = CommandLineRunner {
        println("\n=== Kotlin Reladomo Query DSL Example ===\n")
        
        // Create some sample orders
        println("Creating sample orders...")
        val now = Instant.now()
        
        val order1 = OrderKt(
            orderId = 1001L,
            customerId = 100,
            orderDate = now,
            amount = BigDecimal("250.00"),
            status = "PENDING",
            description = "First order",
            businessDate = now,
            processingDate = now
        )
        
        val order2 = OrderKt(
            orderId = 1001L,
            customerId = 100,
            orderDate = now.plusSeconds(3600),
            amount = BigDecimal("150.00"),
            status = "SHIPPED",
            description = "Second order",
            businessDate = now,
            processingDate = now
        )
        
        val order3 = OrderKt(
            orderId = 1001L,
            customerId = 200,
            orderDate = now.plusSeconds(7200),
            amount = BigDecimal("500.00"),
            status = "PENDING",
            description = "Large order",
            businessDate = now,
            processingDate = now
        )
        
        orderRepository.save(order1)
        orderRepository.save(order2)
        orderRepository.save(order3)
        
        println("Created 3 sample orders\n")
        
        // Example 1: Find orders by customer ID
        println("1. Finding orders for customer 100:")
        val customerOrders = orderRepository.find {
            customerId eq 100
        }
        customerOrders.forEach { println("   - Order ${it.orderId}: ${it.status} - $${it.amount}") }
        
        // Example 2: Find orders by status
        println("\n2. Finding PENDING orders:")
        val pendingOrders = orderRepository.find {
            status eq "PENDING"
        }
        pendingOrders.forEach { println("   - Order ${it.orderId}: Customer ${it.customerId} - $${it.amount}") }
        
        // Example 3: Find orders with amount greater than 200
        println("\n3. Finding orders with amount > 200:")
        val largeOrders = orderRepository.find {
            amount greaterThan BigDecimal("200.00")
        }
        largeOrders.forEach { println("   - Order ${it.orderId}: $${it.amount}") }
        
        // Example 4: Complex query with multiple conditions
        println("\n4. Finding PENDING orders for customer 100:")
        val complexQuery = orderRepository.find {
            customerId eq 100
            status eq "PENDING"
        }
        complexQuery.forEach { println("   - Order ${it.orderId}: ${it.description}") }
        
        // Example 5: Find single order
        println("\n5. Finding single order with amount = 500:")
        val singleOrder = orderRepository.findOne {
            amount eq BigDecimal("500.00")
        }
        singleOrder?.let { println("   - Found: Order ${it.orderId} - ${it.description}") }
        
        // Example 6: Count orders
        println("\n6. Counting orders for customer 100:")
        val count = orderRepository.count {
            customerId eq 100
        }
        println("   - Count: $count orders")
        
        // Example 7: Check existence
        println("\n7. Checking if any DELIVERED orders exist:")
        val hasDelivered = orderRepository.exists {
            status eq "DELIVERED"
        }
        println("   - Has delivered orders: $hasDelivered")
        
        println("\n=== Query DSL Example Complete ===")
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(QueryDslExampleApp::class.java, *args)
}