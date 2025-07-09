package io.github.reladomokotlin.spring.annotation

import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties.CacheType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import java.math.BigDecimal
import java.time.Instant

class ReladomoAnnotationTest {

    @Test
    fun `test ReladomoEntity annotation with defaults`() {
        // Given
        @ReladomoEntity
        class TestEntity
        
        // When
        val annotation = TestEntity::class.findAnnotation<ReladomoEntity>()
        
        // Then
        assertNotNull(annotation)
        assertEquals("", annotation?.tableName)
        assertEquals("default", annotation?.connectionManager)
        assertEquals(CacheType.PARTIAL, annotation?.cacheType)
        assertFalse(annotation?.bitemporal ?: true)
    }
    
    @Test
    fun `test ReladomoEntity annotation with custom values`() {
        // Given
        @ReladomoEntity(
            tableName = "CUSTOM_TABLE",
            connectionManager = "customDB",
            cacheType = CacheType.FULL,
            bitemporal = true
        )
        class TestEntity
        
        // When
        val annotation = TestEntity::class.findAnnotation<ReladomoEntity>()
        
        // Then
        assertNotNull(annotation)
        assertEquals("CUSTOM_TABLE", annotation?.tableName)
        assertEquals("customDB", annotation?.connectionManager)
        assertEquals(CacheType.FULL, annotation?.cacheType)
        assertTrue(annotation?.bitemporal ?: false)
    }
    
    @Test
    fun `test PrimaryKey annotation`() {
        // Given
        class TestEntity {
            @PrimaryKey(columnName = "ID")
            val id: Long = 0
        }
        
        // When
        val property = TestEntity::class.members.find { it.name == "id" }
        val annotation = property?.findAnnotation<PrimaryKey>()
        
        // Then
        assertNotNull(annotation)
        assertEquals("ID", annotation?.columnName)
    }
    
    @Test
    fun `test Column annotation with various types`() {
        // Given
        class TestEntity {
            @Column(name = "NAME", nullable = false, length = 100)
            val name: String = ""
            
            @Column(name = "AMOUNT")
            val amount: BigDecimal = BigDecimal.ZERO
            
            @Column(name = "ACTIVE")
            val active: Boolean = true
        }
        
        // When
        val nameProperty = TestEntity::class.members.find { it.name == "name" }
        val nameAnnotation = nameProperty?.findAnnotation<Column>()
        
        val amountProperty = TestEntity::class.members.find { it.name == "amount" }
        val amountAnnotation = amountProperty?.findAnnotation<Column>()
        
        val activeProperty = TestEntity::class.members.find { it.name == "active" }
        val activeAnnotation = activeProperty?.findAnnotation<Column>()
        
        // Then
        assertNotNull(nameAnnotation)
        assertEquals("NAME", nameAnnotation?.name)
        assertFalse(nameAnnotation?.nullable ?: true)
        assertEquals(100, nameAnnotation?.length)
        
        assertNotNull(amountAnnotation)
        assertEquals("AMOUNT", amountAnnotation?.name)
        
        assertNotNull(activeAnnotation)
        assertEquals("ACTIVE", activeAnnotation?.name)
    }
    
    @Test
    fun `test temporal annotations`() {
        // Given
        @ReladomoEntity(bitemporal = true)
        class BiTemporalEntity {
            @BusinessDate
            val businessDate: Instant = Instant.now()
            
            @ProcessingDate
            val processingDate: Instant = Instant.now()
        }
        
        // When
        val businessDateProperty = BiTemporalEntity::class.members.find { it.name == "businessDate" }
        val processingDateProperty = BiTemporalEntity::class.members.find { it.name == "processingDate" }
        
        // Then
        assertTrue(businessDateProperty?.hasAnnotation<BusinessDate>() ?: false)
        assertTrue(processingDateProperty?.hasAnnotation<ProcessingDate>() ?: false)
    }
    
    @Test
    fun `test multiple ReladomoEntity annotations on same class`() {
        // Given
        @ReladomoEntity(tableName = "TEST_TABLE")
        class TestEntity {
            @PrimaryKey
            val id: Long = 0
        }
        
        // When
        val annotation = TestEntity::class.findAnnotation<ReladomoEntity>()
        
        // Then
        assertNotNull(annotation)
        assertEquals("TEST_TABLE", annotation?.tableName)
    }
    
    @Test
    fun `test Relationship annotation`() {
        // Given
        class Order {
            @Relationship(
                targetEntity = "Customer",
                expression = "this.customerId = Customer.id"
            )
            val customer: Any? = null
            
            @Relationship(
                targetEntity = "OrderItem",
                expression = "this.id = OrderItem.orderId",
                oneToMany = true
            )
            val orderItems: List<Any> = emptyList()
        }
        
        // When
        val customerProperty = Order::class.members.find { it.name == "customer" }
        val customerAnnotation = customerProperty?.findAnnotation<Relationship>()
        
        val itemsProperty = Order::class.members.find { it.name == "orderItems" }
        val itemsAnnotation = itemsProperty?.findAnnotation<Relationship>()
        
        // Then
        assertNotNull(customerAnnotation)
        assertEquals("Customer", customerAnnotation?.targetEntity)
        assertEquals("this.customerId = Customer.id", customerAnnotation?.expression)
        assertFalse(customerAnnotation?.oneToMany ?: true)
        
        assertNotNull(itemsAnnotation)
        assertEquals("OrderItem", itemsAnnotation?.targetEntity)
        assertEquals("this.id = OrderItem.orderId", itemsAnnotation?.expression)
        assertTrue(itemsAnnotation?.oneToMany ?: false)
    }
    
    @Test
    fun `test annotation combinations for complete entity`() {
        // Given
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
            
            @Column(name = "STATUS", length = 20)
            val status: String = ""
            
            @Column(name = "AMOUNT")
            val amount: BigDecimal = BigDecimal.ZERO
            
            @BusinessDate
            val businessDate: Instant = Instant.now()
            
            @ProcessingDate
            val processingDate: Instant = Instant.now()
            
            @Relationship(
                targetEntity = "Customer",
                expression = "this.customerId = Customer.id"
            )
            val customer: Any? = null
        }
        
        // When
        val entityAnnotation = Order::class.findAnnotation<ReladomoEntity>()
        val primaryKeyProperty = Order::class.members.find { it.name == "orderId" }
        val hasBusinessDate = Order::class.members.any { it.hasAnnotation<BusinessDate>() }
        val hasProcessingDate = Order::class.members.any { it.hasAnnotation<ProcessingDate>() }
        
        // Then
        assertNotNull(entityAnnotation)
        assertTrue(entityAnnotation?.bitemporal ?: false)
        assertTrue(primaryKeyProperty?.hasAnnotation<PrimaryKey>() ?: false)
        assertTrue(hasBusinessDate)
        assertTrue(hasProcessingDate)
    }
    
    @Test
    fun `test annotation validation helpers`() {
        // Given
        @ReladomoEntity(bitemporal = true)
        class ValidBiTemporalEntity {
            @PrimaryKey
            val id: Long = 0
            
            @BusinessDate
            val businessDate: Instant = Instant.now()
            
            @ProcessingDate
            val processingDate: Instant = Instant.now()
        }
        
        @ReladomoEntity(bitemporal = true)
        class InvalidBiTemporalEntity {
            @PrimaryKey
            val id: Long = 0
            // Missing temporal date annotations
        }
        
        // When/Then - This demonstrates what validation logic could check
        val validEntity = ValidBiTemporalEntity::class
        val invalidEntity = InvalidBiTemporalEntity::class
        
        // Check if bitemporal entity has required date fields
        if (validEntity.findAnnotation<ReladomoEntity>()?.bitemporal == true) {
            val hasBusinessDate = validEntity.members.any { it.hasAnnotation<BusinessDate>() }
            val hasProcessingDate = validEntity.members.any { it.hasAnnotation<ProcessingDate>() }
            assertTrue(hasBusinessDate && hasProcessingDate)
        }
        
        if (invalidEntity.findAnnotation<ReladomoEntity>()?.bitemporal == true) {
            val hasBusinessDate = invalidEntity.members.any { it.hasAnnotation<BusinessDate>() }
            val hasProcessingDate = invalidEntity.members.any { it.hasAnnotation<ProcessingDate>() }
            assertFalse(hasBusinessDate && hasProcessingDate)
        }
    }
}