package io.github.reladomokotlin.generator

import io.github.reladomokotlin.generator.parser.ReladomoXmlParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class GeneratorIntegrationTest {
    
    @Test
    fun `test code generation from Order XML`(@TempDir tempDir: File) {
        // Create a test XML file
        val xmlFile = File(tempDir, "Order.xml")
        xmlFile.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <MithraObject objectType="transactional" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <ClassName>Order</ClassName>
                <PackageName>io.github.reladomokotlin.sample.domain</PackageName>
                
                <Attribute name="orderId" javaType="long" primaryKey="true"/>
                <Attribute name="customerId" javaType="long"/>
                <Attribute name="orderDate" javaType="Timestamp"/>
                <Attribute name="amount" javaType="BigDecimal"/>
                <Attribute name="status" javaType="String" maxLength="20"/>
                <Attribute name="description" javaType="String" maxLength="500" nullable="true"/>
                
                <AsOfAttribute name="businessDate" fromAttribute="businessFrom" toAttribute="businessThru"/>
                <AsOfAttribute name="processingDate" fromAttribute="processingFrom" toAttribute="processingThru" 
                               toIsInclusive="false" infinityDate="[com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity()]"/>
            </MithraObject>
        """.trimIndent())
        
        // Parse the XML
        val parser = ReladomoXmlParser()
        val definition = parser.parse(xmlFile)
        
        // Generate Kotlin wrapper
        val generator = KotlinWrapperGenerator()
        val outputDir = File(tempDir, "generated")
        val generatedFile = generator.generateToFile(definition, outputDir)
        
        assertTrue(generatedFile.exists(), "Generated file should exist")
        
        // Generate repository
        val repoGenerator = KotlinRepositoryGenerator()
        val repoFile = repoGenerator.generateToFile(definition, outputDir)
        
        assertTrue(repoFile.exists(), "Repository file should exist")
        
        // Print generated code for debugging
        println("Generated wrapper:\n${generatedFile.readText()}")
        println("\nGenerated repository:\n${repoFile.readText()}")
    }
}