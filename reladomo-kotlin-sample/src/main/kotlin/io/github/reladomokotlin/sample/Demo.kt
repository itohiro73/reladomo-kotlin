package io.github.reladomokotlin.sample

import io.github.reladomokotlin.sample.domain.kotlin.OrderKt
import io.github.reladomokotlin.sample.domain.kotlin.repository.OrderKtRepository
import java.math.BigDecimal
import java.time.Instant

/**
 * Simple demonstration of the Kotlin Reladomo wrapper functionality.
 * This can be run to verify basic operations work correctly.
 */
fun main() {
    println("=== Kotlin Reladomo Sample Demo ===\n")
    
    val repository = OrderKtRepository()
    
    // 1. Find all orders
    println("1. Finding all orders:")
    val allOrders = repository.findAll()
    allOrders.forEach { order ->
        println("   Order #${order.orderId}: Customer ${order.customerId}, Amount: $${order.amount}, Status: ${order.status}")
    }
    println()
    
    // 2. Find by ID
    println("2. Finding order by ID (1):")
    val order = repository.findById(1L)
    if (order != null) {
        println("   Found: Order #${order.orderId}")
        println("   Customer: ${order.customerId}")
        println("   Amount: $${order.amount}")
        println("   Status: ${order.status}")
        println("   Business Date: ${order.businessDate}")
        println("   Processing Date: ${order.processingDate}")
    }
    println()
    
    // 3. Find by customer
    println("3. Finding orders for customer 100:")
    val customerOrders = repository.findByCustomerId(100L)
    println("   Found ${customerOrders.size} orders")
    customerOrders.forEach { o ->
        println("   - Order #${o.orderId}: $${o.amount}")
    }
    println()
    
    // 4. Create new order
    println("4. Creating new order:")
    val newOrder = OrderKt(
        orderId = 99L,
        customerId = 300L,
        orderDate = Instant.now(),
        amount = BigDecimal("555.55"),
        status = "NEW",
        description = "Demo order",
        businessDate = Instant.now(),
        processingDate = Instant.now()
    )
    val saved = repository.save(newOrder)
    println("   Created order #${saved.orderId} for customer ${saved.customerId}")
    println()
    
    // 5. Update order
    println("5. Updating order #1:")
    val toUpdate = repository.findById(1L)!!
    val updated = toUpdate.copy(
        status = "PROCESSING",
        amount = BigDecimal("1111.11")
    )
    repository.update(updated)
    println("   Updated status to: ${updated.status}")
    println("   Updated amount to: $${updated.amount}")
    println()
    
    // 6. Delete order
    println("6. Deleting order #99:")
    repository.deleteById(99L)
    println("   Order deleted")
    println()
    
    // 7. Demonstrate bitemporal query (simulated)
    println("7. Bitemporal query (simulated):")
    val asOfOrder = repository.findByIdAsOf(1L, Instant.now(), Instant.now())
    if (asOfOrder != null) {
        println("   Found order #${asOfOrder.orderId} as of current time")
    }
    println()
    
    println("=== Demo completed successfully! ===")
}

/**
 * Alternative entry point for Spring Boot context
 */
class DemoRunner(
    private val repository: OrderKtRepository
) {
    fun runDemo() {
        println("\n=== Running demo with Spring context ===")
        
        // Show that repository is properly injected
        println("Repository class: ${repository::class.simpleName}")
        println("Order count: ${repository.findAll().size}")
        
        // Demonstrate a simple operation
        val orders = repository.findByCustomerId(100L)
        println("Customer 100 has ${orders.size} orders")
    }
}