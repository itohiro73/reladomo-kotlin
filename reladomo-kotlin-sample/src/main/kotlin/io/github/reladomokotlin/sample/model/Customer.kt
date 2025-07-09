package io.github.reladomokotlin.sample.model

import io.github.reladomokotlin.spring.annotation.*
import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties.CacheType
import java.time.Instant

/**
 * Customer entity using Reladomo annotations.
 */
@ReladomoEntity(
    tableName = "CUSTOMERS",
    cacheType = CacheType.PARTIAL,
    bitemporal = true
)
data class Customer(
    @PrimaryKey(columnName = "CUSTOMER_ID")
    val customerId: Long? = null,
    
    @Column(name = "NAME", nullable = false, length = 200)
    val name: String,
    
    @Column(name = "EMAIL", nullable = false, length = 100)
    val email: String,
    
    @Column(name = "PHONE", nullable = true, length = 50)
    val phone: String? = null,
    
    @Column(name = "ADDRESS", nullable = true, length = 500)
    val address: String? = null,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now(),
    
    @Relationship(
        targetEntity = "io.github.reladomokotlin.sample.model.Order",
        expression = "this.customerId = Order.customerId",
        oneToMany = true
    )
    val orders: List<Order>? = null
)