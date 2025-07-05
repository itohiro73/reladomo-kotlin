package io.github.kotlinreladomo.sample.repository

import io.github.kotlinreladomo.core.exceptions.EntityNotFoundException
import io.github.kotlinreladomo.sample.domain.kotlin.OrderKt
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory repository implementation for testing.
 * In production, this would extend AbstractBiTemporalRepository and use Reladomo.
 */
@Repository
class OrderKtRepository {
    
    private val orders = ConcurrentHashMap<Long, OrderKt>()
    
    init {
        // Add sample data
        val now = Instant.now()
        
        save(OrderKt(
            orderId = 1L,
            customerId = 100L,
            orderDate = now.minusSeconds(86400),
            amount = BigDecimal("999.99"),
            status = "PENDING",
            description = "First order",
            businessDate = now,
            processingDate = now
        ))
        
        save(OrderKt(
            orderId = 2L,
            customerId = 100L,
            orderDate = now.minusSeconds(43200),
            amount = BigDecimal("1500.00"),
            status = "COMPLETED",
            description = "Second order",
            businessDate = now,
            processingDate = now
        ))
        
        save(OrderKt(
            orderId = 3L,
            customerId = 200L,
            orderDate = now,
            amount = BigDecimal("750.50"),
            status = "PENDING",
            description = "Third order",
            businessDate = now,
            processingDate = now
        ))
    }
    
    fun save(entity: OrderKt): OrderKt {
        orders[entity.orderId] = entity
        return entity
    }
    
    fun findById(id: Long): OrderKt? {
        return orders[id]
    }
    
    fun findByIdAsOf(id: Long, businessDate: Instant, processingDate: Instant): OrderKt? {
        // For demo purposes, we'll ignore the temporal aspect
        return findById(id)
    }
    
    fun update(entity: OrderKt): OrderKt {
        if (!orders.containsKey(entity.orderId)) {
            throw EntityNotFoundException("Order not found with id: ${entity.orderId}")
        }
        orders[entity.orderId] = entity
        return entity
    }
    
    fun deleteById(id: Long) {
        if (!orders.containsKey(id)) {
            throw EntityNotFoundException("Order not found with id: $id")
        }
        orders.remove(id)
    }
    
    fun findAll(): List<OrderKt> {
        return orders.values.toList()
    }
    
    fun findByCustomerId(customerId: Long): List<OrderKt> {
        return orders.values.filter { it.customerId == customerId }
    }
}