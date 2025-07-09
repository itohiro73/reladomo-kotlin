package io.github.reladomokotlin.generator

import io.github.reladomokotlin.generator.model.AttributeDefinition
import io.github.reladomokotlin.generator.model.AsOfAttributeDefinition
import io.github.reladomokotlin.generator.model.MithraObjectDefinition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.*

/**
 * Comprehensive tests specifically for bitemporal code generation,
 * ensuring all the critical fixes are properly tested.
 */
class BiTemporalGeneratorTest {
    
    private val wrapperGenerator = KotlinWrapperGenerator()
    private val repositoryGenerator = KotlinRepositoryGenerator()
    
    private fun createBiTemporalDefinition() = MithraObjectDefinition(
        className = "Order",
        packageName = "com.example.domain",
        tableName = "ORDERS",
        attributes = listOf(
            AttributeDefinition("orderId", "long", "ORDER_ID", true, false),
            AttributeDefinition("customerId", "long", "CUSTOMER_ID", false, false),
            AttributeDefinition("status", "String", "STATUS", false, false),
            AttributeDefinition("amount", "BigDecimal", "AMOUNT", false, false),
            AttributeDefinition("description", "String", "DESCRIPTION", false, true)
        ),
        asOfAttributes = listOf(
            AsOfAttributeDefinition("businessDate", "BUSINESS_FROM", "BUSINESS_THRU"),
            AsOfAttributeDefinition("processingDate", "PROCESSING_FROM", "PROCESSING_THRU")
        )
    )
    
    @Test
    fun `test bitemporal entity implements BiTemporalEntity interface`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val wrapperFile = wrapperGenerator.generateToFile(definition, tempDir)
        val content = wrapperFile.readText()
        
        // Then
        assertTrue(content.contains(": BiTemporalEntity"), "Should implement BiTemporalEntity")
        assertTrue(content.contains("override val businessDate: Instant"), "Should have businessDate property")
        assertTrue(content.contains("override val processingDate: Instant"), "Should have processingDate property")
        assertTrue(content.contains("import io.github.reladomokotlin.core.BiTemporalEntity"), "Should import BiTemporalEntity")
    }
    
    @Test
    fun `test repository findById does NOT use equalsEdgePoint`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val repoFile = repositoryGenerator.generateToFile(definition, tempDir)
        val content = repoFile.readText()
        
        // Then - Critical fix validation
        val findByIdMethod = content.substringAfter("override fun findById(id: Long): OrderKt?")
            .substringBefore("override fun")
        
        assertFalse(
            findByIdMethod.contains("equalsEdgePoint()"),
            "findById should NOT use equalsEdgePoint() - this was a critical fix!"
        )
        
        // Verify it uses findByPrimaryKey instead
        
        assertTrue(
            findByIdMethod.contains("OrderFinder.findByPrimaryKey(id, now, infinityTs)"),
            "findById should use findByPrimaryKey with current time and infinity"
        )
    }
    
    @Test
    fun `test repository update method accepts business date parameter`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val repoFile = repositoryGenerator.generateToFile(definition, tempDir)
        val content = repoFile.readText()
        
        // Then
        assertTrue(
            content.contains("fun update(entity: OrderKt, businessDate: Instant): OrderKt"),
            "Update method should accept businessDate parameter"
        )
        
        // Verify implementation fetches as of business date
        val updateMethod = content.substringAfter("fun update(entity: OrderKt, businessDate: Instant")
            .substringBefore("fun delete")
        
        assertTrue(
            updateMethod.contains("val businessDateTs = Timestamp.from(businessDate)"),
            "Should convert business date to timestamp"
        )
        assertTrue(
            updateMethod.contains("OrderFinder.findByPrimaryKey(entity.orderId!!, businessDateTs, infinityTs)"),
            "Should fetch existing record as of business date"
        )
    }
    
    @Test
    fun `test repository delete method accepts business date parameter`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val repoFile = repositoryGenerator.generateToFile(definition, tempDir)
        val content = repoFile.readText()
        
        // Then
        assertTrue(
            content.contains("fun deleteByIdAsOf(id: Long, businessDate: Instant)"),
            "Delete method should have deleteByIdAsOf with businessDate parameter"
        )
        
        // Verify implementation - check deleteByIdAsOf method
        val deleteByIdAsOfMethod = content.substringAfter("fun deleteByIdAsOf(id: Long, businessDate: Instant)")
            .substringBefore("fun find")
        
        assertTrue(
            deleteByIdAsOfMethod.contains("OrderFinder.findByPrimaryKey(id, businessDateTs, infinityTs)"),
            "Should fetch record using findByPrimaryKey"
        )
        assertTrue(
            deleteByIdAsOfMethod.contains(".terminate()"),
            "Should use terminate() for bitemporal deletion"
        )
    }
    
    @Test
    fun `test repository has findByIdAsOf method`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val repoFile = repositoryGenerator.generateToFile(definition, tempDir)
        val content = repoFile.readText()
        
        // Then
        assertTrue(
            content.contains("fun findByIdAsOf(") && 
            content.contains("id: Long,") && 
            content.contains("businessDate: Instant,") && 
            content.contains("processingDate: Instant,"),
            "Should have findByIdAsOf method"
        )
        
        // Verify implementation
        val findByIdAsOfMethod = content.substringAfter("fun findByIdAsOf")
            .substringBefore("fun update")
        
        assertTrue(
            findByIdAsOfMethod.contains("OrderFinder.findByPrimaryKey(id, Timestamp.from(businessDate)") ||
            findByIdAsOfMethod.contains("OrderFinder.findByPrimaryKey(id, businessDateTs, processingDateTs)"),
            "Should use findByPrimaryKey with both temporal parameters"
        )
    }
    
    @Test
    fun `test wrapper converts timestamps correctly`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val wrapperFile = wrapperGenerator.generateToFile(definition, tempDir)
        val content = wrapperFile.readText()
        
        // Then
        // In toReladomo method
        val toReladomoMethod = content.substringAfter("fun toReladomo(): Order")
            .substringBefore("companion object")
        
        assertTrue(
            toReladomoMethod.contains("Timestamp.from(this.businessDate)"),
            "Should convert Instant to Timestamp for business date"
        )
        assertTrue(
            toReladomoMethod.contains("Timestamp.from(this.processingDate)"),
            "Should convert Instant to Timestamp for processing date"
        )
        
        // In fromReladomo method
        val fromReladomoMethod = content.substringAfter("fun fromReladomo(obj: Order): OrderKt")
            .substringBefore("}")
        
        assertTrue(
            fromReladomoMethod.contains("businessDate = obj.businessDate.toInstant()"),
            "Should convert Timestamp to Instant for business date"
        )
        assertTrue(
            fromReladomoMethod.contains("processingDate = obj.processingDate.toInstant()"),
            "Should convert Timestamp to Instant for processing date"
        )
    }
    
    @Test
    fun `test primary keys are nullable for new entities`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val wrapperFile = wrapperGenerator.generateToFile(definition, tempDir)
        val content = wrapperFile.readText()
        
        // Then
        assertTrue(
            content.contains("val orderId: Long?"),
            "Primary key should be nullable for new entities"
        )
        
        // But other fields should respect their nullable setting
        assertTrue(
            content.contains("val customerId: Long,") && !content.contains("val customerId: Long?"),
            "Non-nullable fields should not have ?"
        )
        assertTrue(
            content.contains("val description: String?"),
            "Nullable fields should have ?"
        )
    }
    
    @Test
    fun `test repository uses proper Spring annotations`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val repoFile = repositoryGenerator.generateToFile(definition, tempDir)
        val content = repoFile.readText()
        
        // Then
        assertTrue(content.contains("@Repository"), "Should have @Repository annotation")
        assertTrue(content.contains("@Transactional"), "Should have @Transactional annotation")
        assertTrue(
            content.contains("import org.springframework.stereotype.Repository"),
            "Should import Spring Repository"
        )
        assertTrue(
            content.contains("import org.springframework.transaction.`annotation`.Transactional"),
            "Should import Spring Transactional with backticks"
        )
    }
    
    @Test
    fun `test complete bitemporal CRUD flow`(@TempDir tempDir: File) {
        // Given
        val definition = createBiTemporalDefinition()
        
        // When
        val wrapperFile = wrapperGenerator.generateToFile(definition, tempDir)
        val repoFile = repositoryGenerator.generateToFile(definition, tempDir)
        
        val wrapperContent = wrapperFile.readText()
        val repoContent = repoFile.readText()
        
        // Then - Verify complete flow is possible
        // 1. Create new entity with nullable ID
        assertTrue(wrapperContent.contains("val orderId: Long?"))
        
        // 2. Save method exists
        assertTrue(repoContent.contains("fun save(entity: OrderKt): OrderKt"))
        
        // 3. Find current version
        assertTrue(repoContent.contains("fun findById(id: Long): OrderKt?"))
        val findByIdSection = repoContent.substringAfter("override fun findById(id: Long): OrderKt?").substringBefore("override fun")
        assertFalse(findByIdSection.contains("equalsEdgePoint()"))
        
        // 4. Find historical version
        assertTrue(repoContent.contains("fun findByIdAsOf(") && repoContent.contains("id: Long,") && repoContent.contains("businessDate: Instant,"))
        
        // 5. Update as of business date
        assertTrue(repoContent.contains("fun update(entity: OrderKt, businessDate: Instant): OrderKt"))
        
        // 6. Delete (terminate) as of business date  
        assertTrue(repoContent.contains("fun deleteByIdAsOf(id: Long, businessDate: Instant)"))
    }
}