package io.github.kotlinreladomo.generator

import io.github.kotlinreladomo.generator.model.AttributeDefinition
import io.github.kotlinreladomo.generator.model.AsOfAttributeDefinition
import io.github.kotlinreladomo.generator.model.MithraObjectDefinition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.*

class KotlinWrapperGeneratorTest {
    
    private val generator = KotlinWrapperGenerator()
    
    @Test
    fun `test simple entity generation`(@TempDir tempDir: File) {
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
        assertTrue(generatedFile.exists(), "Generated file should exist at: ${generatedFile.absolutePath}")
        val content = generatedFile.readText()
        println("Generated content:\n$content")
        
        // Verify package
        assertTrue(content.contains("package com.example.domain.kotlin"), "Content should contain package declaration")
        
        // Verify data class
        assertTrue(content.contains("public data class CustomerKt("))
        
        // Verify fields
        assertTrue(content.contains("public val customerId: Long?"))
        assertTrue(content.contains("public val name: String"))
        assertTrue(content.contains("public val email: String"))
        
        // Verify companion object
        assertTrue(content.contains("public companion object {"))
        assertTrue(content.contains("public fun fromReladomo(obj: Customer): CustomerKt"))
    }
    
    @Test
    fun `test bitemporal entity generation`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Order",
            packageName = "com.example.domain",
            tableName = "ORDER",
            attributes = listOf(
                AttributeDefinition("orderId", "long", "ORDER_ID", true, false),
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
        // Verify implements BiTemporalEntity
        assertTrue(content.contains(": BiTemporalEntity"))
        
        // Verify temporal fields
        assertTrue(content.contains("override val businessDate: Instant"))
        assertTrue(content.contains("override val processingDate: Instant"))
        
        // Verify imports
        assertTrue(content.contains("import io.github.kotlinreladomo.core.BiTemporalEntity"))
        assertTrue(content.contains("import java.time.Instant"))
    }
    
    @Test
    fun `test nullable field handling`(@TempDir tempDir: File) {
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
        // Non-nullable fields
        assertTrue(content.contains("public val name: String"))
        assertTrue(content.contains("public val price: BigDecimal"))
        
        // Nullable fields
        assertTrue(content.contains("public val description: String?"))
        assertTrue(content.contains("public val discountPrice: BigDecimal?"))
        
        // Verify nullable handling in toReladomo
        assertTrue(content.contains("this.description?.let { obj.description = it }"))
        assertTrue(content.contains("this.discountPrice?.let { obj.discountPrice = it }"))
    }
    
    @Test
    fun `test type mapping`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "AllTypes",
            packageName = "com.example.domain",
            tableName = "ALL_TYPES",
            attributes = listOf(
                AttributeDefinition("id", "long", "ID", true, false),
                AttributeDefinition("booleanField", "boolean", "BOOLEAN_FIELD", false, false),
                AttributeDefinition("byteField", "byte", "BYTE_FIELD", false, false),
                AttributeDefinition("shortField", "short", "SHORT_FIELD", false, false),
                AttributeDefinition("intField", "int", "INT_FIELD", false, false),
                AttributeDefinition("longField", "long", "LONG_FIELD", false, false),
                AttributeDefinition("floatField", "float", "FLOAT_FIELD", false, false),
                AttributeDefinition("doubleField", "double", "DOUBLE_FIELD", false, false),
                AttributeDefinition("stringField", "String", "STRING_FIELD", false, false),
                AttributeDefinition("dateField", "Date", "DATE_FIELD", false, false),
                AttributeDefinition("timestampField", "Timestamp", "TIMESTAMP_FIELD", false, false),
                AttributeDefinition("bigDecimalField", "BigDecimal", "BIG_DECIMAL_FIELD", false, false),
                AttributeDefinition("byteArrayField", "byte[]", "BYTE_ARRAY_FIELD", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then - Verify Kotlin type mappings
        assertTrue(content.contains("public val booleanField: Boolean"))
        assertTrue(content.contains("public val byteField: Byte"))
        assertTrue(content.contains("public val shortField: Short"))
        assertTrue(content.contains("public val intField: Int"))
        assertTrue(content.contains("public val longField: Long"))
        assertTrue(content.contains("public val floatField: Float"))
        assertTrue(content.contains("public val doubleField: Double"))
        assertTrue(content.contains("public val stringField: String"))
        assertTrue(content.contains("public val dateField: LocalDate"))
        assertTrue(content.contains("public val timestampField: Instant"))
        assertTrue(content.contains("public val bigDecimalField: BigDecimal"))
        assertTrue(content.contains("public val byteArrayField: ByteArray"))
        
        // Verify timestamp conversion
        assertTrue(content.contains("timestampField = obj.timestampField.toInstant()"))
        assertTrue(content.contains("obj.timestampField = Timestamp.from(this.timestampField)"))
    }
    
    @Test
    fun `test date and time type mappings`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "DateTimeEntity",
            packageName = "com.example.domain",
            tableName = "DATE_TIME_ENTITY",
            attributes = listOf(
                AttributeDefinition("id", "long", "ID", true, false),
                AttributeDefinition("dateField", "Date", "DATE_FIELD", false, false),
                AttributeDefinition("timeField", "Time", "TIME_FIELD", false, false),
                AttributeDefinition("nullableDateField", "Date", "NULLABLE_DATE_FIELD", false, true),
                AttributeDefinition("nullableTimeField", "Time", "NULLABLE_TIME_FIELD", false, true),
                AttributeDefinition("timestampField", "Timestamp", "TIMESTAMP_FIELD", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        println("Generated Date/Time entity content:\n$content")
        
        // Then - Verify Kotlin type mappings
        assertTrue(content.contains("public val dateField: LocalDate"), "Should map Date to LocalDate")
        assertTrue(content.contains("public val timeField: LocalTime"), "Should map Time to LocalTime")
        assertTrue(content.contains("public val nullableDateField: LocalDate?"), "Should map nullable Date to LocalDate?")
        assertTrue(content.contains("public val nullableTimeField: LocalTime?"), "Should map nullable Time to LocalTime?")
        
        // Verify imports
        assertTrue(content.contains("import java.time.LocalDate"), "Should import LocalDate")
        assertTrue(content.contains("import java.time.LocalTime"), "Should import LocalTime")
        assertTrue(content.contains("import java.util.Date"), "Should import java.util.Date")
        
        // Verify fromReladomo conversion - check for essential parts
        assertTrue(content.contains("dateField = obj.dateField.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()"), 
            "Should convert Date to LocalDate in fromReladomo")
        assertTrue(content.contains("timeField = obj.timeField.toLocalTime()"), 
            "Should convert Time to LocalTime in fromReladomo")
        assertTrue(content.contains("nullableDateField =") && content.contains("obj.nullableDateField?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()"), 
            "Should convert nullable Date to LocalDate? in fromReladomo")
        assertTrue(content.contains("nullableTimeField = obj.nullableTimeField?.toLocalTime()"), 
            "Should convert nullable Time to LocalTime? in fromReladomo")
        
        // Verify toReladomo conversion - check for essential parts while allowing for formatting differences
        assertTrue(content.contains("obj.dateField =") && content.contains("Date.from(this.dateField.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())"), 
            "Should convert LocalDate to Date in toReladomo")
        assertTrue(content.contains("obj.timeField = java.sql.Time.valueOf(this.timeField)"), 
            "Should convert LocalTime to Time in toReladomo")
        assertTrue(content.contains("obj.nullableDateField = this.nullableDateField?.let") && content.contains("Date.from(it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())"), 
            "Should convert LocalDate? to Date in toReladomo")
        assertTrue(content.contains("obj.nullableTimeField = this.nullableTimeField?.let { java.sql.Time.valueOf(it) }"), 
            "Should convert LocalTime? to Time in toReladomo")
    }
    
    @Test
    fun `test date and time fields in bitemporal entity`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "BiTemporalDateTimeEntity",
            packageName = "com.example.domain",
            tableName = "BITEMPORAL_DATE_TIME_ENTITY",
            attributes = listOf(
                AttributeDefinition("id", "long", "ID", true, false),
                AttributeDefinition("effectiveDate", "Date", "EFFECTIVE_DATE", false, false),
                AttributeDefinition("startTime", "Time", "START_TIME", false, false),
                AttributeDefinition("endTime", "Time", "END_TIME", false, true),
                AttributeDefinition("createdAt", "Timestamp", "CREATED_AT", false, false)
            ),
            asOfAttributes = listOf(
                AsOfAttributeDefinition("businessDate", "BUSINESS_FROM", "BUSINESS_THRU"),
                AsOfAttributeDefinition("processingDate", "PROCESSING_FROM", "PROCESSING_THRU")
            )
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        println("Generated BiTemporal Date/Time entity content:\n$content")
        
        // Then - Verify bitemporal entity with Date/Time fields
        assertTrue(content.contains(": BiTemporalEntity"), "Should implement BiTemporalEntity")
        assertTrue(content.contains("public val effectiveDate: LocalDate"), "Should have LocalDate field")
        assertTrue(content.contains("public val startTime: LocalTime"), "Should have LocalTime field")
        assertTrue(content.contains("public val endTime: LocalTime?"), "Should have nullable LocalTime field")
        assertTrue(content.contains("override val businessDate: Instant"), "Should have businessDate")
        assertTrue(content.contains("override val processingDate: Instant"), "Should have processingDate")
        
        // Verify conversion in bitemporal context - check for essential parts
        assertTrue(content.contains("effectiveDate =") && content.contains("obj.effectiveDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()"))
        assertTrue(content.contains("startTime = obj.startTime.toLocalTime()"))
        assertTrue(content.contains("endTime = obj.endTime?.toLocalTime()"))
    }
    
    @Test
    fun `test composite primary key handling`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "OrderItem",
            packageName = "com.example.domain",
            tableName = "ORDER_ITEM",
            attributes = listOf(
                AttributeDefinition("orderId", "long", "ORDER_ID", true, false),
                AttributeDefinition("itemId", "long", "ITEM_ID", true, false),
                AttributeDefinition("productId", "long", "PRODUCT_ID", false, false),
                AttributeDefinition("quantity", "int", "QUANTITY", false, false),
                AttributeDefinition("price", "BigDecimal", "PRICE", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        // Both primary key fields should be nullable for new entities
        assertTrue(content.contains("public val orderId: Long?"))
        assertTrue(content.contains("public val itemId: Long?"))
        
        // Regular fields are non-nullable
        assertTrue(content.contains("public val productId: Long"))
        assertTrue(content.contains("public val quantity: Int"))
    }
    
    @Test
    fun `test complex entity with relationships`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "Department",
            packageName = "com.example.domain",
            tableName = "DEPARTMENT",
            attributes = listOf(
                AttributeDefinition("departmentId", "long", "DEPARTMENT_ID", true, false),
                AttributeDefinition("name", "String", "NAME", false, false),
                AttributeDefinition("managerId", "long", "MANAGER_ID", false, true),
                AttributeDefinition("budget", "BigDecimal", "BUDGET", false, false),
                AttributeDefinition("createdDate", "Timestamp", "CREATED_DATE", false, false),
                AttributeDefinition("lastModified", "Timestamp", "LAST_MODIFIED", false, true)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        // Verify proper handling of optional foreign keys
        assertTrue(content.contains("public val managerId: Long?"))
        
        // Verify imports are correct
        assertTrue(content.contains("import java.math.BigDecimal"))
        assertTrue(content.contains("import java.time.Instant"))
        assertTrue(content.contains("import java.sql.Timestamp"))
    }
    
    @Test
    fun `test special characters in field names`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "LegacyTable",
            packageName = "com.example.domain",
            tableName = "LEGACY_TABLE",
            attributes = listOf(
                AttributeDefinition("id", "long", "ID", true, false),
                AttributeDefinition("is_active", "boolean", "IS_ACTIVE", false, false),
                AttributeDefinition("created_date", "Timestamp", "CREATED_DATE", false, false),
                AttributeDefinition("last_modified_by", "String", "LAST_MODIFIED_BY", false, true)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        // Verify field names are properly converted to camelCase
        assertTrue(content.contains("public val isActive: Boolean"))
        assertTrue(content.contains("public val createdDate: Instant"))
        assertTrue(content.contains("public val lastModifiedBy: String?"))
    }
    
    @Test
    fun `test enum-like string fields`(@TempDir tempDir: File) {
        // Given
        val definition = MithraObjectDefinition(
            className = "OrderStatus",
            packageName = "com.example.domain",
            tableName = "ORDER_STATUS",
            attributes = listOf(
                AttributeDefinition("orderId", "long", "ORDER_ID", true, false),
                AttributeDefinition("status", "String", "STATUS", false, false, maxLength = 20),
                AttributeDefinition("reason", "String", "REASON", false, true, maxLength = 255)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        val content = generatedFile.readText()
        
        // Then
        // String fields should remain as String (enum conversion is future enhancement)
        assertTrue(content.contains("public val status: String"))
        assertTrue(content.contains("public val reason: String?"))
    }
    
    @Test
    fun `test file structure and naming`(@TempDir tempDir: File) {
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
        assertEquals("com/example/domain/kotlin/CustomerOrderKt.kt", 
            generatedFile.relativeTo(tempDir).path.replace(File.separatorChar, '/'))
        
        // Verify class name
        assertTrue(generatedFile.readText().contains("public data class CustomerOrderKt"))
    }
}