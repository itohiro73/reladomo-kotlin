package io.github.kotlinreladomo.spring.scanner

import io.github.kotlinreladomo.spring.annotation.*
import io.github.kotlinreladomo.spring.config.ReladomoKotlinProperties.CacheType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.ApplicationContext
import java.math.BigDecimal
import java.time.Instant

class ReladomoEntityScannerTest {

    private lateinit var scanner: ReladomoEntityScanner
    private lateinit var applicationContext: ApplicationContext

    @BeforeEach
    fun setUp() {
        applicationContext = AnnotationConfigApplicationContext()
        scanner = ReladomoEntityScanner(applicationContext)
    }

    @Test
    fun `test scan for entities in single package`() {
        // When
        val entities = scanner.scanForEntities(listOf("io.github.kotlinreladomo.spring.scanner.test.entities"))
        
        // Then
        assertEquals(4, entities.size)
        assertTrue(entities.any { it.simpleName == "Customer" })
        assertTrue(entities.any { it.simpleName == "Order" })
        assertTrue(entities.any { it.simpleName == "Product" })
        assertTrue(entities.any { it.simpleName == "OrderItem" })
    }
    
    @Test
    fun `test scan for entities in multiple packages`() {
        // When
        val entities = scanner.scanForEntities(listOf(
            "io.github.kotlinreladomo.spring.scanner.test.entities",
            "io.github.kotlinreladomo.spring.scanner.test.additional"
        ))
        
        // Then
        assertEquals(5, entities.size) // 4 from entities + 1 from additional
        assertTrue(entities.any { it.simpleName == "AuditLog" })
    }
    
    @Test
    fun `test entity metadata extraction`() {
        // When
        val entities = scanner.scanForEntities(listOf("io.github.kotlinreladomo.spring.scanner.test.entities"))
        val orderMetadata = entities.find { it.simpleName == "Order" }
        
        // Then
        assertNotNull(orderMetadata)
        assertEquals("Order", orderMetadata?.simpleName)
        assertEquals("ORDERS", orderMetadata?.tableName)
        assertEquals("default", orderMetadata?.connectionManager)
        assertEquals(CacheType.PARTIAL, orderMetadata?.cacheType)
        assertTrue(orderMetadata?.bitemporal ?: false)
        assertEquals("io.github.kotlinreladomo.spring.scanner.test.entities.Order", orderMetadata?.className)
    }
    
    @Test
    fun `test non-bitemporal entity metadata`() {
        // When
        val entities = scanner.scanForEntities(listOf("io.github.kotlinreladomo.spring.scanner.test.entities"))
        val productMetadata = entities.find { it.simpleName == "Product" }
        
        // Then
        assertNotNull(productMetadata)
        assertEquals("Product", productMetadata?.simpleName)
        assertEquals("PRODUCTS", productMetadata?.tableName)
        assertEquals(CacheType.FULL, productMetadata?.cacheType)
        assertFalse(productMetadata?.bitemporal ?: true)
    }
    
    @Test
    fun `test entity with custom connection manager`() {
        // When
        val entities = scanner.scanForEntities(listOf("io.github.kotlinreladomo.spring.scanner.test.additional"))
        val auditLogMetadata = entities.find { it.simpleName == "AuditLog" }
        
        // Then
        assertNotNull(auditLogMetadata)
        assertEquals("auditDB", auditLogMetadata?.connectionManager)
    }
    
    @Test
    fun `test entity name derivation from class name`() {
        // Given a class with @ReladomoEntity but no explicit tableName
        val entities = scanner.scanForEntities(listOf("io.github.kotlinreladomo.spring.scanner.test.entities"))
        val customerMetadata = entities.find { it.simpleName == "Customer" }
        
        // Then - entity name should be class name
        assertEquals("Customer", customerMetadata?.simpleName)
        // Table name should default to snake case version of class name
        assertEquals("CUSTOMER", customerMetadata?.tableName)
    }
    
    @Test
    fun `test scan with no entities found`() {
        // When
        val entities = scanner.scanForEntities(listOf("io.github.kotlinreladomo.spring.scanner.test.empty"))
        
        // Then
        assertTrue(entities.isEmpty())
    }
    
    @Test
    fun `test scan with invalid package`() {
        // When
        val entities = scanner.scanForEntities(listOf("com.nonexistent.package"))
        
        // Then
        assertTrue(entities.isEmpty())
    }
    
    @Test
    fun `test entity scanner filters non-entity classes`() {
        // When
        val entities = scanner.scanForEntities(listOf("io.github.kotlinreladomo.spring.scanner.test.mixed"))
        
        // Then
        assertEquals(1, entities.size) // Should only find ValidEntity, not RegularClass
        assertTrue(entities.any { it.simpleName == "ValidEntity" })
    }
    
    @Test
    fun `test metadata includes all annotation details`() {
        // When
        val entities = scanner.scanForEntities(listOf("io.github.kotlinreladomo.spring.scanner.test.entities"))
        val orderItemMetadata = entities.find { it.simpleName == "OrderItem" }
        
        // Then
        assertNotNull(orderItemMetadata)
        assertEquals("ORDER_ITEMS", orderItemMetadata?.tableName)
        assertEquals("default", orderItemMetadata?.connectionManager)
        assertEquals(CacheType.PARTIAL, orderItemMetadata?.cacheType)
        assertFalse(orderItemMetadata?.bitemporal ?: true)
        
        // Verify the class can be loaded
        val clazz = Class.forName(orderItemMetadata?.className)
        assertNotNull(clazz)
        assertTrue(clazz.isAnnotationPresent(ReladomoEntity::class.java))
    }
}