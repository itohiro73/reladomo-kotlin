package io.github.reladomokotlin.sample.model

import io.github.reladomokotlin.spring.annotation.*
import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties.CacheType
import java.math.BigDecimal
import java.time.Instant

/**
 * OrderItem entity using Reladomo annotations.
 * Represents line items in an order.
 */
@ReladomoEntity(
    tableName = "ORDER_ITEMS",
    cacheType = CacheType.PARTIAL,
    bitemporal = true
)
data class OrderItem(
    @PrimaryKey(columnName = "ORDER_ITEM_ID")
    val orderItemId: Long? = null,
    
    @Column(name = "ORDER_ID", nullable = false)
    val orderId: Long,
    
    @Column(name = "PRODUCT_ID", nullable = false)
    val productId: Long,
    
    @Column(name = "QUANTITY", nullable = false)
    val quantity: Int,
    
    @Column(name = "UNIT_PRICE", nullable = false)
    val unitPrice: BigDecimal,
    
    @Column(name = "TOTAL_PRICE", nullable = false)
    val totalPrice: BigDecimal,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now(),
    
    @Relationship(
        targetEntity = "io.github.reladomokotlin.sample.model.Order",
        expression = "this.orderId = Order.orderId",
        oneToMany = false
    )
    val order: Order? = null,
    
    @Relationship(
        targetEntity = "io.github.reladomokotlin.sample.model.Product",
        expression = "this.productId = Product.productId",
        oneToMany = false
    )
    val product: Product? = null
)