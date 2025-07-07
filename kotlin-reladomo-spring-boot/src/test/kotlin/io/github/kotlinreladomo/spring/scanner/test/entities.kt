package io.github.kotlinreladomo.spring.scanner.test.entities

import io.github.kotlinreladomo.spring.annotation.*
import io.github.kotlinreladomo.spring.config.ReladomoKotlinProperties.CacheType
import java.math.BigDecimal
import java.time.Instant

@ReladomoEntity
class Customer {
    @PrimaryKey(columnName = "CUSTOMER_ID")
    val customerId: Long = 0
    
    @Column(name = "NAME", nullable = false)
    val name: String = ""
    
    @Column(name = "EMAIL", nullable = false)
    val email: String = ""
}

@ReladomoEntity(
    tableName = "ORDERS",
    cacheType = CacheType.PARTIAL,
    bitemporal = true
)
class Order {
    @PrimaryKey(columnName = "ORDER_ID")
    val orderId: Long = 0
    
    @Column(name = "CUSTOMER_ID", nullable = false)
    val customerId: Long = 0
    
    @Column(name = "AMOUNT")
    val amount: BigDecimal = BigDecimal.ZERO
    
    @BusinessDate
    val businessDate: Instant = Instant.now()
    
    @ProcessingDate
    val processingDate: Instant = Instant.now()
}

@ReladomoEntity(
    tableName = "PRODUCTS",
    cacheType = CacheType.FULL,
    bitemporal = false
)
class Product {
    @PrimaryKey(columnName = "PRODUCT_ID")
    val productId: Long = 0
    
    @Column(name = "NAME", nullable = false)
    val name: String = ""
    
    @Column(name = "PRICE")
    val price: BigDecimal = BigDecimal.ZERO
}

@ReladomoEntity(tableName = "ORDER_ITEMS")
class OrderItem {
    @PrimaryKey(columnName = "ORDER_ITEM_ID")
    val orderItemId: Long = 0
    
    @Column(name = "ORDER_ID", nullable = false)
    val orderId: Long = 0
    
    @Column(name = "PRODUCT_ID", nullable = false)
    val productId: Long = 0
    
    @Column(name = "QUANTITY")
    val quantity: Int = 0
}