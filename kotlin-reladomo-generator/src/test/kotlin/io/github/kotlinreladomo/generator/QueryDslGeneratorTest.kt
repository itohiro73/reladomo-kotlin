package io.github.kotlinreladomo.generator

import io.github.kotlinreladomo.generator.model.AttributeDefinition
import io.github.kotlinreladomo.generator.model.AsOfAttributeDefinition
import io.github.kotlinreladomo.generator.model.MithraObjectDefinition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class QueryDslGeneratorTest {
    
    @Test
    fun `test Query DSL generation for simple entity`(@TempDir tempDir: File) {
        // Given
        val generator = QueryDslGenerator()
        val definition = MithraObjectDefinition(
            className = "Product",
            packageName = "com.example.domain",
            tableName = "PRODUCTS",
            attributes = listOf(
                AttributeDefinition("productId", "long", "PRODUCT_ID", true, false),
                AttributeDefinition("name", "String", "NAME", false, false),
                AttributeDefinition("price", "BigDecimal", "PRICE", false, false),
                AttributeDefinition("quantity", "int", "QUANTITY", false, false),
                AttributeDefinition("description", "String", "DESCRIPTION", false, true)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        
        // Then
        assertTrue(generatedFile.exists())
        assertTrue(generatedFile.path.contains("com/example/domain/kotlin/query"))
        assertTrue(generatedFile.name == "ProductQueryDsl.kt")
        
        val content = generatedFile.readText()
        
        // Verify imports
        assertTrue(content.contains("import io.github.kotlinreladomo.query.QueryContext"))
        assertTrue(content.contains("import io.github.kotlinreladomo.query.longAttribute"))
        assertTrue(content.contains("import io.github.kotlinreladomo.query.stringAttribute"))
        assertTrue(content.contains("import com.example.domain.ProductFinder"))
        
        // Verify DSL object
        assertTrue(content.contains("public object ProductQueryDsl"))
        
        // Verify attribute properties
        assertTrue(content.contains("public val QueryContext.productId:"))
        assertTrue(content.contains("NumericAttributeProperty<Long, LongAttribute<*>>"))
        assertTrue(content.contains("get() = longAttribute(ProductFinder.productId())"))
        
        assertTrue(content.contains("public val QueryContext.name:"))
        assertTrue(content.contains("StringAttributeProperty"))
        assertTrue(content.contains("get() = stringAttribute(ProductFinder.name())"))
        
        assertTrue(content.contains("public val QueryContext.price:"))
        assertTrue(content.contains("NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>>"))
        
        assertTrue(content.contains("public val QueryContext.quantity:"))
        assertTrue(content.contains("NumericAttributeProperty<Int, IntegerAttribute<*>>"))
        assertTrue(content.contains("get() = intAttribute(ProductFinder.quantity())"))
        
        // Verify nullable attribute
        assertTrue(content.contains("public val QueryContext.description:"))
        assertTrue(content.contains("StringAttributeProperty"))
    }
    
    @Test
    fun `test Query DSL generation for bitemporal entity`(@TempDir tempDir: File) {
        // Given
        val generator = QueryDslGenerator()
        val definition = MithraObjectDefinition(
            className = "Order",
            packageName = "com.example.domain",
            tableName = "ORDERS",
            attributes = listOf(
                AttributeDefinition("orderId", "long", "ORDER_ID", true, false),
                AttributeDefinition("customerId", "long", "CUSTOMER_ID", false, false),
                AttributeDefinition("orderDate", "Timestamp", "ORDER_DATE", false, false),
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
        
        // Then
        val content = generatedFile.readText()
        
        // Verify temporal attributes
        assertTrue(content.contains("public val QueryContext.businessDate:"))
        assertTrue(content.contains("AsOfAttributeProperty"))
        assertTrue(content.contains("get() = asOfAttribute(OrderFinder.businessDate())"))
        
        assertTrue(content.contains("public val QueryContext.processingDate:"))
        assertTrue(content.contains("AsOfAttributeProperty"))
        assertTrue(content.contains("get() = asOfAttribute(OrderFinder.processingDate())"))
        
        // Verify timestamp attribute
        assertTrue(content.contains("public val QueryContext.orderDate:"))
        assertTrue(content.contains("TemporalAttributeProperty<Timestamp, TimestampAttribute<*>>"))
        assertTrue(content.contains("get() = timestampAttribute(OrderFinder.orderDate())"))
    }
    
    @Test
    fun `test Query DSL generation with all supported types`(@TempDir tempDir: File) {
        // Given
        val generator = QueryDslGenerator()
        val definition = MithraObjectDefinition(
            className = "AllTypes",
            packageName = "com.example.domain",
            tableName = "ALL_TYPES",
            attributes = listOf(
                AttributeDefinition("id", "long", "ID", true, false),
                AttributeDefinition("booleanField", "boolean", "BOOLEAN_FIELD", false, false),
                AttributeDefinition("intField", "int", "INT_FIELD", false, false),
                AttributeDefinition("longField", "long", "LONG_FIELD", false, false),
                AttributeDefinition("floatField", "float", "FLOAT_FIELD", false, false),
                AttributeDefinition("doubleField", "double", "DOUBLE_FIELD", false, false),
                AttributeDefinition("stringField", "String", "STRING_FIELD", false, false),
                AttributeDefinition("dateField", "Date", "DATE_FIELD", false, false),
                AttributeDefinition("timestampField", "Timestamp", "TIMESTAMP_FIELD", false, false),
                AttributeDefinition("bigDecimalField", "BigDecimal", "BIG_DECIMAL_FIELD", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        
        // Then
        val content = generatedFile.readText()
        
        // Verify all type mappings
        assertTrue(content.contains("public val QueryContext.booleanField:"))
        assertTrue(content.contains("AttributeProperty<Boolean, BooleanAttribute<*>>"))
        
        assertTrue(content.contains("public val QueryContext.intField:"))
        assertTrue(content.contains("NumericAttributeProperty<Int, IntegerAttribute<*>>"))
        
        assertTrue(content.contains("public val QueryContext.longField:"))
        assertTrue(content.contains("NumericAttributeProperty<Long, LongAttribute<*>>"))
        
        assertTrue(content.contains("public val QueryContext.floatField:"))
        assertTrue(content.contains("NumericAttributeProperty<Float, FloatAttribute<*>>"))
        
        assertTrue(content.contains("public val QueryContext.doubleField:"))
        assertTrue(content.contains("NumericAttributeProperty<Double, DoubleAttribute<*>>"))
        
        assertTrue(content.contains("public val QueryContext.stringField:"))
        assertTrue(content.contains("StringAttributeProperty"))
        
        assertTrue(content.contains("public val QueryContext.dateField:"))
        assertTrue(content.contains("TemporalAttributeProperty<Date, DateAttribute<*>>"))
        
        assertTrue(content.contains("public val QueryContext.timestampField:"))
        assertTrue(content.contains("TemporalAttributeProperty<Timestamp, TimestampAttribute<*>>"))
        
        assertTrue(content.contains("public val QueryContext.bigDecimalField:"))
        assertTrue(content.contains("NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>>"))
    }
    
    @Test
    fun `test Query DSL field name conversion`(@TempDir tempDir: File) {
        // Given
        val generator = QueryDslGenerator()
        val definition = MithraObjectDefinition(
            className = "TestEntity",
            packageName = "com.example.domain",
            tableName = "TEST_ENTITY",
            attributes = listOf(
                AttributeDefinition("id", "long", "ID", true, false),
                AttributeDefinition("customer_name", "String", "CUSTOMER_NAME", false, false),
                AttributeDefinition("created_date", "Timestamp", "CREATED_DATE", false, false),
                AttributeDefinition("is_active", "boolean", "IS_ACTIVE", false, false)
            ),
            asOfAttributes = emptyList()
        )
        
        // When
        val generatedFile = generator.generateToFile(definition, tempDir)
        
        // Then
        val content = generatedFile.readText()
        
        // Verify snake_case to camelCase conversion
        assertTrue(content.contains("public val QueryContext.customerName:"))
        assertTrue(content.contains("get() = stringAttribute(TestEntityFinder.customer_name())"))
        
        assertTrue(content.contains("public val QueryContext.createdDate:"))
        assertTrue(content.contains("get() = timestampAttribute(TestEntityFinder.created_date())"))
        
        assertTrue(content.contains("public val QueryContext.isActive:"))
        assertTrue(content.contains("get() = attribute(TestEntityFinder.is_active())"))
    }
}