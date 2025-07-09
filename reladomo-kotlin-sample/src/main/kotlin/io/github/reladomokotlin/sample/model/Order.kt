package io.github.reladomokotlin.sample.model

import io.github.reladomokotlin.spring.annotation.*
import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties.CacheType
import java.math.BigDecimal
import java.time.Instant

/**
 * Order entity using Reladomo annotations for automatic configuration.
 * This demonstrates the annotation-based approach for defining Reladomo entities.
 */
@ReladomoEntity(
    tableName = "ORDERS",
    cacheType = CacheType.PARTIAL,
    bitemporal = true
)
data class Order(
    @PrimaryKey(columnName = "ORDER_ID")
    val orderId: Long? = null,
    
    @Column(name = "CUSTOMER_ID", nullable = false)
    val customerId: Long,
    
    @Column(name = "ORDER_DATE", nullable = false)
    val orderDate: Instant,
    
    @Column(name = "AMOUNT", nullable = false)
    val amount: BigDecimal,
    
    @Column(name = "STATUS", nullable = false, length = 50)
    val status: String,
    
    @Column(name = "DESCRIPTION", nullable = true, length = 500)
    val description: String? = null,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now()
)