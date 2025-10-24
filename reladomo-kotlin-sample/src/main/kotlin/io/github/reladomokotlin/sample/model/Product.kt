package io.github.reladomokotlin.sample.model

import io.github.reladomokotlin.spring.annotation.*
import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties.CacheType
import java.math.BigDecimal
import java.time.Instant

/**
 * Product entity using Reladomo annotations.
 */
@ReladomoEntity(
    tableName = "PRODUCTS",
    cacheType = CacheType.PARTIAL,  // Use partial cache to avoid eager loading before schema initialization
    bitemporal = false  // Product is not bitemporal in the current schema
)
data class Product(
    @PrimaryKey(columnName = "PRODUCT_ID")
    val productId: Long? = null,
    
    @Column(name = "SKU", nullable = false, length = 50)
    val sku: String,
    
    @Column(name = "NAME", nullable = false, length = 200)
    val name: String,
    
    @Column(name = "DESCRIPTION", nullable = true, length = 1000)
    val description: String? = null,
    
    @Column(name = "PRICE", nullable = false)
    val price: BigDecimal,
    
    @Column(name = "STOCK_QUANTITY", nullable = false)
    val stockQuantity: Int,
    
    @Column(name = "CATEGORY", nullable = true, length = 100)
    val category: String? = null,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now()
)