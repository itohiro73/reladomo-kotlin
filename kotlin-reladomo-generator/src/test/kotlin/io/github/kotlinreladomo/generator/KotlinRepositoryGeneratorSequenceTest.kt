package io.github.kotlinreladomo.generator

import com.squareup.kotlinpoet.FileSpec
import io.github.kotlinreladomo.generator.model.AttributeDefinition
import io.github.kotlinreladomo.generator.model.MithraObjectDefinition
import io.github.kotlinreladomo.generator.model.AsOfAttributeDefinition
import io.github.kotlinreladomo.generator.model.ObjectType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KotlinRepositoryGeneratorSequenceTest {
    
    private val generator = KotlinRepositoryGenerator()
    
    @Test
    fun `should generate repository with optional sequence generator`() {
        val definition = createOrderDefinition()
        val fileSpec = generator.generate(definition)
        val generatedCode = fileSpec.toString()
        
        // Should import SequenceGenerator
        assertTrue(generatedCode.contains("import io.github.kotlinreladomo.sequence.SequenceGenerator"))
        
        // Should have @Autowired(required = false) annotation
        assertTrue(generatedCode.contains("@Autowired(required = false)"))
        
        // Should have nullable sequence generator property
        assertTrue(generatedCode.contains("private var sequenceGenerator: SequenceGenerator? = null"))
    }
    
    @Test
    fun `should generate save method with sequence generator for entity with long primary key`() {
        val definition = createOrderDefinition()
        val fileSpec = generator.generate(definition)
        val generatedCode = fileSpec.toString()
        
        // Should check for null or 0L
        assertTrue(generatedCode.contains("entity.orderId?.takeIf { it != 0L }"))
        
        // Should use sequence generator if ID is null or 0
        assertTrue(generatedCode.contains("sequenceGenerator?.getNextId(\"Order\")"))
        
        // Should throw exception if no ID and no generator
        assertTrue(generatedCode.contains("throw IllegalStateException(\"No ID provided and sequence generator not available\")"))
    }
    
    @Test
    fun `should not use sequence generator for non-long primary key`() {
        val definition = MithraObjectDefinition(
            packageName = "com.example.domain",
            className = "Product",
            tableName = "PRODUCTS",
            objectType = ObjectType.TRANSACTIONAL,
            attributes = listOf(
                AttributeDefinition(
                    name = "productCode",
                    javaType = "String",
                    columnName = "PRODUCT_CODE",
                    isPrimaryKey = true,
                    maxLength = 50
                ),
                AttributeDefinition(
                    name = "name",
                    javaType = "String", 
                    columnName = "NAME",
                    maxLength = 100
                )
            ),
            asOfAttributes = emptyList()
        )
        
        val fileSpec = generator.generate(definition)
        val generatedCode = fileSpec.toString()
        
        // Should still have sequence generator property (for consistency)
        assertTrue(generatedCode.contains("private var sequenceGenerator: SequenceGenerator? = null"))
        
        // But should NOT use it in save method for String primary key
        assertFalse(generatedCode.contains("sequenceGenerator?.getNextId(\"Product\")"))
    }
    
    @Test
    fun `should generate proper sequence usage for bitemporal entity`() {
        val definition = createBitemporalOrderDefinition()
        val fileSpec = generator.generate(definition)
        val generatedCode = fileSpec.toString()
        
        // Should use sequence generator in bitemporal save method
        assertTrue(generatedCode.contains("entity.orderId?.takeIf { it != 0L }"))
        assertTrue(generatedCode.contains("sequenceGenerator?.getNextId(\"Order\")"))
        
        // Should handle bitemporal constructor properly
        assertTrue(generatedCode.contains("Order(Timestamp.from(entity.businessDate))"))
    }
    
    @Test
    fun `should not import Qualifier when not needed`() {
        // The current implementation always uses @Autowired(required = false)
        // without @Qualifier, so Qualifier import should not be present
        val definition = createOrderDefinition()
        val fileSpec = generator.generate(definition)
        val generatedCode = fileSpec.toString()
        
        // Should import Autowired
        assertTrue(generatedCode.contains("import org.springframework.beans.factory.`annotation`.Autowired"))
        
        // Should NOT import Qualifier anymore since we removed it
        assertFalse(generatedCode.contains("import org.springframework.beans.factory.`annotation`.Qualifier"))
    }
    
    @Test
    fun `should handle multiple entities with different primary key types`() {
        // Entity with long primary key - should use sequence
        val orderDef = createOrderDefinition()
        val orderSpec = generator.generate(orderDef)
        val orderCode = orderSpec.toString()
        
        assertTrue(orderCode.contains("sequenceGenerator?.getNextId(\"Order\")"))
        
        // Entity with composite primary key - should not use sequence
        val compositeDef = MithraObjectDefinition(
            packageName = "com.example.domain",
            className = "OrderItem",
            tableName = "ORDER_ITEMS",
            objectType = ObjectType.TRANSACTIONAL,
            attributes = listOf(
                AttributeDefinition(
                    name = "orderId",
                    javaType = "long",
                    columnName = "ORDER_ID",
                    isPrimaryKey = true
                ),
                AttributeDefinition(
                    name = "itemId",
                    javaType = "long",
                    columnName = "ITEM_ID",
                    isPrimaryKey = true
                ),
                AttributeDefinition(
                    name = "quantity",
                    javaType = "int",
                    columnName = "QUANTITY"
                )
            ),
            asOfAttributes = emptyList()
        )
        
        val compositeSpec = generator.generate(compositeDef)
        val compositeCode = compositeSpec.toString()
        
        // Should have sequence generator property but not use it
        assertTrue(compositeCode.contains("private var sequenceGenerator: SequenceGenerator? = null"))
        assertFalse(compositeCode.contains("sequenceGenerator?.getNextId(\"OrderItem\")"))
    }
    
    private fun createOrderDefinition() = MithraObjectDefinition(
        packageName = "com.example.domain",
        className = "Order",
        tableName = "ORDERS",
        objectType = ObjectType.TRANSACTIONAL,
        attributes = listOf(
            AttributeDefinition(
                name = "orderId",
                javaType = "long",
                columnName = "ORDER_ID",
                isPrimaryKey = true
            ),
            AttributeDefinition(
                name = "customerId",
                javaType = "long",
                columnName = "CUSTOMER_ID"
            ),
            AttributeDefinition(
                name = "orderDate",
                javaType = "Timestamp",
                columnName = "ORDER_DATE"
            ),
            AttributeDefinition(
                name = "amount",
                javaType = "BigDecimal",
                columnName = "AMOUNT"
            ),
            AttributeDefinition(
                name = "status",
                javaType = "String",
                columnName = "STATUS",
                maxLength = 20
            )
        ),
        asOfAttributes = emptyList()
    )
    
    private fun createBitemporalOrderDefinition() = MithraObjectDefinition(
        packageName = "com.example.domain",
        className = "Order",
        tableName = "ORDERS",
        objectType = ObjectType.TRANSACTIONAL,
        attributes = listOf(
            AttributeDefinition(
                name = "orderId",
                javaType = "long",
                columnName = "ORDER_ID",
                isPrimaryKey = true
            ),
            AttributeDefinition(
                name = "customerId",
                javaType = "long",
                columnName = "CUSTOMER_ID"
            ),
            AttributeDefinition(
                name = "orderDate",
                javaType = "Timestamp",
                columnName = "ORDER_DATE"
            ),
            AttributeDefinition(
                name = "amount",
                javaType = "BigDecimal",
                columnName = "AMOUNT"
            ),
            AttributeDefinition(
                name = "businessFrom",
                javaType = "Timestamp",
                columnName = "BUSINESS_FROM"
            ),
            AttributeDefinition(
                name = "businessThru",
                javaType = "Timestamp",
                columnName = "BUSINESS_THRU"
            ),
            AttributeDefinition(
                name = "processingFrom",
                javaType = "Timestamp",
                columnName = "PROCESSING_FROM"
            ),
            AttributeDefinition(
                name = "processingThru",
                javaType = "Timestamp",
                columnName = "PROCESSING_THRU"
            )
        ),
        asOfAttributes = listOf(
            AsOfAttributeDefinition(
                name = "businessDate",
                fromColumn = "BUSINESS_FROM",
                toColumn = "BUSINESS_THRU",
                infinityDate = "9999-12-01"
            ),
            AsOfAttributeDefinition(
                name = "processingDate",
                fromColumn = "PROCESSING_FROM",
                toColumn = "PROCESSING_THRU",
                infinityDate = "9999-12-01"
            )
        )
    )
}