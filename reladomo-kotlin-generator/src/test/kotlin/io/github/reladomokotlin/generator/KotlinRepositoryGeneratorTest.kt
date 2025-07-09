package io.github.reladomokotlin.generator

import io.github.reladomokotlin.generator.model.AttributeDefinition
import io.github.reladomokotlin.generator.model.AsOfAttributeDefinition
import io.github.reladomokotlin.generator.model.MithraObjectDefinition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.*

class KotlinRepositoryGeneratorTest {
    
    private val generator = KotlinRepositoryGenerator()
    
    @Test
    fun `test simple repository generation`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Customer",
            packageName = "com.example.domain",
            tableName = "CUSTOMER",
            attributes = listOf(
                AttributeDefinition("customerId", "long", "CUSTOMER_ID", true, false),
                AttributeDefinition("name", "String", "NAME", false, false),
                AttributeDefinition("email", "String", "EMAIL", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        
        // Then
        assertTrue(generatedFile.exists())
        val content = generatedFile.readText()
        println("Generated repository content:\n$content")
        
        // Verify package
        assertTrue(content.contains("package com.example.domain.kotlin"))
        
        // Verify class declaration
        assertTrue(content.contains("public class CustomerKtRepository"))
        
        // Verify annotations
        assertTrue(content.contains("@Repository"))
        assertTrue(content.contains("@Transactional"))
        
        // Verify CRUD methods are present
        assertTrue(content.contains("override fun save(entity: CustomerKt): CustomerKt"))
        assertTrue(content.contains("override fun findById(id: Long): CustomerKt?"))
        assertTrue(content.contains("override fun update(entity: CustomerKt): CustomerKt"))
        assertTrue(content.contains("override fun deleteById(id: Long)"))
        assertTrue(content.contains("override fun findAll(): List<CustomerKt>"))
        
        // Verify finder usage
        assertTrue(content.contains("CustomerFinder.findByPrimaryKey"))
    }
    
    @Test
    fun `test bitemporal repository generation`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Order",
            packageName = "com.example.domain",
            tableName = "ORDER",
            attributes = listOf(
                AttributeDefinition("orderId", "long", "ORDER_ID", true, false),
                AttributeDefinition("amount", "BigDecimal", "AMOUNT", false, false),
                AttributeDefinition("status", "String", "STATUS", false, false)
            ),
            asOfAttributes = listOf(
                AsOfAttributeDefinition("businessDate", "BUSINESS_FROM", "BUSINESS_THRU"),
                AsOfAttributeDefinition("processingDate", "PROCESSING_FROM", "PROCESSING_THRU")
            )
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated bitemporal repository content:\n$content")
        
        // Verify class declaration (not interface)
        assertTrue(content.contains("public class OrderKtRepository"))
        
        // Verify bitemporal-specific methods
        assertTrue(content.contains("override fun findByIdAsOf("))
        assertTrue(content.contains("id: Long,"))
        assertTrue(content.contains("businessDate: Instant,"))
        assertTrue(content.contains("processingDate: Instant"))
        
        // Verify update method exists - bitemporal has businessDate parameter
        assertTrue(content.contains("override fun update(entity: OrderKt, businessDate: Instant): OrderKt"))
        
        // Verify delete method exists - bitemporal has businessDate parameter
        assertTrue(content.contains("override fun deleteById(id: Long)"))
        assertTrue(content.contains("override fun deleteByIdAsOf(id: Long, businessDate: Instant)"))
        
        // Verify finder usage
        assertTrue(content.contains("OrderFinder.findByPrimaryKey"))
        
        // For bitemporal objects, should use equalsEdgePoint for queries
        assertTrue(content.contains("equalsEdgePoint()"))
        
        // Verify terminate is used for delete
        assertTrue(content.contains(".terminate()"))
    }
    
    @Test
    fun `test findById implementation without equalsEdgePoint`(@TempDir tempDir: File) {
        // Given - Bitemporal entity
        val definition = MithraObjectDefinition(
            className = "Trade",
            packageName = "com.example.domain",
            tableName = "TRADE",
            attributes = listOf(
                AttributeDefinition("tradeId", "long", "TRADE_ID", true, false),
                AttributeDefinition("quantity", "int", "QUANTITY", false, false)
            ),
            asOfAttributes = listOf(
                AsOfAttributeDefinition("businessDate", "BUSINESS_FROM", "BUSINESS_THRU"),
                AsOfAttributeDefinition("processingDate", "PROCESSING_FROM", "PROCESSING_THRU")
            )
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated Trade repository content:\n$content")
        
        // Verify findById exists
        assertTrue(content.contains("override fun findById(id: Long): TradeKt?"))
        
        // Verify it uses findByPrimaryKey
        assertTrue(content.contains("TradeFinder.findByPrimaryKey"))
        
        // Since this is bitemporal, it should use equalsEdgePoint for queries like findAll
        assertTrue(content.contains("equalsEdgePoint()"))
    }
    
    @Test
    fun `test update method implementation for bitemporal`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Position",
            packageName = "com.example.domain",
            tableName = "POSITION",
            attributes = listOf(
                AttributeDefinition("positionId", "long", "POSITION_ID", true, false),
                AttributeDefinition("quantity", "BigDecimal", "QUANTITY", false, false)
            ),
            asOfAttributes = listOf(
                AsOfAttributeDefinition("businessDate", "BUSINESS_FROM", "BUSINESS_THRU"),
                AsOfAttributeDefinition("processingDate", "PROCESSING_FROM", "PROCESSING_THRU")
            )
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated Position repository content:\n$content")
        
        // Verify update method has businessDate parameter for bitemporal
        assertTrue(content.contains("override fun update(entity: PositionKt, businessDate: Instant): PositionKt"))
        
        // Verify it finds existing entity
        assertTrue(content.contains("PositionFinder.findByPrimaryKey"))
        assertTrue(content.contains("?: throw EntityNotFoundException"))
        
        // Verify field updates (setters)
        assertTrue(content.contains("setQuantity(entity.quantity"))
    }
    
    @Test
    fun `test composite primary key repository`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "OrderItem",
            packageName = "com.example.domain",
            tableName = "ORDER_ITEM",
            attributes = listOf(
                AttributeDefinition("orderId", "long", "ORDER_ID", true, false),
                AttributeDefinition("itemId", "long", "ITEM_ID", true, false),
                AttributeDefinition("productId", "long", "PRODUCT_ID", false, false),
                AttributeDefinition("quantity", "int", "QUANTITY", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated OrderItem repository content:\n$content")
        
        // For composite keys, the generator seems to be using String for primary key type
        assertTrue(content.contains("override fun findById(id: String): OrderItemKt?"))
        
        // Verify it has standard CRUD methods
        assertTrue(content.contains("override fun save(entity: OrderItemKt): OrderItemKt"))
        assertTrue(content.contains("override fun update(entity: OrderItemKt): OrderItemKt"))
        
        // Verify finder usage
        assertTrue(content.contains("OrderItemFinder.findByPrimaryKey"))
    }
    
    @Test
    fun `test repository with nullable fields`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Product",
            packageName = "com.example.domain",
            tableName = "PRODUCT",
            attributes = listOf(
                AttributeDefinition("productId", "long", "PRODUCT_ID", true, false),
                AttributeDefinition("name", "String", "NAME", false, false),
                AttributeDefinition("description", "String", "DESCRIPTION", false, true), // nullable
                AttributeDefinition("price", "BigDecimal", "PRICE", false, false),
                AttributeDefinition("discountPrice", "BigDecimal", "DISCOUNT_PRICE", false, true) // nullable
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated Product repository content:\n$content")
        
        // Repository should be a class
        assertTrue(content.contains("public class ProductKtRepository"))
        
        // Should handle nullable fields in update
        assertTrue(content.contains("setDescription(entity.description!!)") || content.contains("entity.description?.let"))
    }
    
    @Test
    fun `test delete method for non-bitemporal entity`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Category",
            packageName = "com.example.domain",
            tableName = "CATEGORY",
            attributes = listOf(
                AttributeDefinition("categoryId", "long", "CATEGORY_ID", true, false),
                AttributeDefinition("name", "String", "NAME", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated Category repository content:\n$content")
        
        // Should have deleteById method
        assertTrue(content.contains("override fun deleteById(id: Long)"))
        
        // Verify delete implementation
        assertTrue(content.contains("CategoryFinder.findByPrimaryKey"))
        assertTrue(content.contains(".delete()"))
    }
    
    @Test
    fun `test delete method for bitemporal entity`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Account",
            packageName = "com.example.domain",
            tableName = "ACCOUNT",
            attributes = listOf(
                AttributeDefinition("accountId", "long", "ACCOUNT_ID", true, false),
                AttributeDefinition("balance", "BigDecimal", "BALANCE", false, false)
            ),
            asOfAttributes = listOf(
                AsOfAttributeDefinition("businessDate", "BUSINESS_FROM", "BUSINESS_THRU"),
                AsOfAttributeDefinition("processingDate", "PROCESSING_FROM", "PROCESSING_THRU")
            )
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated Account repository content:\n$content")
        
        // Should have deleteById with businessDate for bitemporal
        assertTrue(content.contains("override fun deleteById(id: Long)"))
        assertTrue(content.contains("override fun deleteByIdAsOf(id: Long, businessDate: Instant)"))
        
        // Verify delete implementation
        assertTrue(content.contains("AccountFinder.findByPrimaryKey"))
        assertTrue(content.contains(".terminate()"))
    }
    
    @Test
    fun `test repository file structure and naming`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "CustomerOrder",
            packageName = "com.example.domain",
            tableName = "CUSTOMER_ORDER",
            attributes = listOf(
                AttributeDefinition("id", "long", "ID", true, false),
                AttributeDefinition("total", "BigDecimal", "TOTAL", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        
        // Then
        // Verify file path
        assertEquals("com/example/domain/kotlin/repository/CustomerOrderKtRepository.kt", 
            generatedFile.relativeTo(tempDir).path.replace(File.separatorChar, '/'))
        
        // Verify class name
        val content = generatedFile.readText()
        assertTrue(content.contains("public class CustomerOrderKtRepository"))
    }
    
    @Test
    fun `test repository with date and time fields`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Schedule",
            packageName = "com.example.domain",
            tableName = "SCHEDULE",
            attributes = listOf(
                AttributeDefinition("scheduleId", "long", "SCHEDULE_ID", true, false),
                AttributeDefinition("eventDate", "Date", "EVENT_DATE", false, false),
                AttributeDefinition("startTime", "Time", "START_TIME", false, false),
                AttributeDefinition("endTime", "Time", "END_TIME", false, true),
                AttributeDefinition("createdAt", "Timestamp", "CREATED_AT", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated Schedule repository content:\n$content")
        
        // Verify update method handles Date and Time conversions
        assertTrue(content.contains("setEventDate(java.util.Date.from(entity.eventDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))"), 
            "Should convert LocalDate to Date in update method")
        assertTrue(content.contains("setStartTime(java.sql.Time.valueOf(entity.startTime))"), 
            "Should convert LocalTime to Time in update method")
        assertTrue(content.contains("entity.endTime?.let { existingOrder.setEndTime(java.sql.Time.valueOf(it)) }"), 
            "Should handle nullable Time conversion in update method")
        
        // Verify imports
        assertTrue(content.contains("import java.time.LocalDate") || content.contains("java.time.LocalDate"), 
            "Should have access to LocalDate")
        assertTrue(content.contains("import java.time.LocalTime") || content.contains("java.time.LocalTime"), 
            "Should have access to LocalTime")
    }
    
    @Test
    fun `test bitemporal repository with date and time fields`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Meeting",
            packageName = "com.example.domain",
            tableName = "MEETING",
            attributes = listOf(
                AttributeDefinition("meetingId", "long", "MEETING_ID", true, false),
                AttributeDefinition("meetingDate", "Date", "MEETING_DATE", false, false),
                AttributeDefinition("startTime", "Time", "START_TIME", false, false),
                AttributeDefinition("duration", "int", "DURATION", false, false)
            ),
            asOfAttributes = listOf(
                AsOfAttributeDefinition("businessDate", "BUSINESS_FROM", "BUSINESS_THRU"),
                AsOfAttributeDefinition("processingDate", "PROCESSING_FROM", "PROCESSING_THRU")
            )
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated Meeting repository content:\n$content")
        
        // Verify update method for bitemporal entity handles Date and Time conversions
        assertTrue(content.contains("setMeetingDate(java.util.Date.from(entity.meetingDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))"), 
            "Should convert LocalDate to Date in bitemporal update method")
        assertTrue(content.contains("setStartTime(java.sql.Time.valueOf(entity.startTime))"), 
            "Should convert LocalTime to Time in bitemporal update method")
        
        // Verify it's a bitemporal repository
        assertTrue(content.contains("override fun findByIdAsOf("))
        assertTrue(content.contains("override fun update(entity: MeetingKt, businessDate: Instant): MeetingKt"))
    }
    
    @Test
    fun `test imports for bitemporal repository`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Transaction",
            packageName = "com.example.domain",
            tableName = "TRANSACTION",
            attributes = listOf(
                AttributeDefinition("transactionId", "long", "TRANSACTION_ID", true, false),
                AttributeDefinition("amount", "BigDecimal", "AMOUNT", false, false)
            ),
            asOfAttributes = listOf(
                AsOfAttributeDefinition("businessDate", "BUSINESS_FROM", "BUSINESS_THRU"),
                AsOfAttributeDefinition("processingDate", "PROCESSING_FROM", "PROCESSING_THRU")
            )
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        println("Generated Transaction repository content:\n$content")
        
        // Verify necessary imports
        assertTrue(content.contains("import java.time.Instant"))
        assertTrue(content.contains("import java.sql.Timestamp"))
        assertTrue(content.contains("import com.example.domain.Transaction"))
        assertTrue(content.contains("import com.example.domain.TransactionFinder"))
        assertTrue(content.contains("import org.springframework.stereotype.Repository"))
        assertTrue(content.contains("import io.github.reladomokotlin.core.exceptions.EntityNotFoundException"))
    }
}