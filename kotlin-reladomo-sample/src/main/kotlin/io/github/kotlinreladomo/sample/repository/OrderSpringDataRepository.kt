package io.github.kotlinreladomo.sample.repository

import io.github.kotlinreladomo.sample.domain.kotlin.OrderKt
import io.github.kotlinreladomo.spring.repository.BiTemporalReladomoRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

/**
 * Spring Data-style repository for Orders.
 * Demonstrates query method support and Spring Data conventions.
 */
@Repository
interface OrderSpringDataRepository : BiTemporalReladomoRepository<OrderKt, Long> {
    
    // Basic query methods
    fun findByCustomerId(customerId: Long): List<OrderKt>
    
    fun findByStatus(status: String): List<OrderKt>
    
    fun findByCustomerIdAndStatus(customerId: Long, status: String): List<OrderKt>
    
    // Comparison operators
    fun findByAmountGreaterThan(amount: BigDecimal): List<OrderKt>
    
    fun findByAmountLessThanEqual(amount: BigDecimal): List<OrderKt>
    
    fun findByAmountBetween(minAmount: BigDecimal, maxAmount: BigDecimal): List<OrderKt>
    
    // String operations
    fun findByDescriptionContaining(keyword: String): List<OrderKt>
    
    fun findByDescriptionStartingWith(prefix: String): List<OrderKt>
    
    fun findByStatusIn(statuses: Collection<String>): List<OrderKt>
    
    fun findByStatusNotIn(statuses: Collection<String>): List<OrderKt>
    
    // Null handling
    fun findByDescriptionIsNull(): List<OrderKt>
    
    fun findByDescriptionIsNotNull(): List<OrderKt>
    
    // Count queries
    fun countByStatus(status: String): Long
    
    fun countByCustomerId(customerId: Long): Long
    
    // Exists queries
    fun existsByCustomerId(customerId: Long): Boolean
    
    fun existsByCustomerIdAndStatus(customerId: Long, status: String): Boolean
    
    // Delete queries
    fun deleteByStatus(status: String)
    
    fun deleteByCustomerId(customerId: Long)
    
    // Limiting results
    fun findFirst5ByStatus(status: String): List<OrderKt>
    
    fun findTopByCustomerIdOrderByAmountDesc(customerId: Long): OrderKt?
    
    // Distinct
    fun findDistinctByStatus(status: String): List<OrderKt>
    
    // Complex queries
    fun findByCustomerIdAndAmountGreaterThanAndStatusIn(
        customerId: Long,
        amount: BigDecimal,
        statuses: Collection<String>
    ): List<OrderKt>
    
    // Temporal queries (bitemporal specific)
    fun findByCustomerIdAsOf(customerId: Long, businessDate: Instant): List<OrderKt>
    
    fun findByStatusAsOf(status: String, businessDate: Instant, processingDate: Instant): List<OrderKt>
}