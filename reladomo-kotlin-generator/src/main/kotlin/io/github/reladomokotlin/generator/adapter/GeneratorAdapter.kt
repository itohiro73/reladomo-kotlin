package io.github.reladomokotlin.generator.adapter

import io.github.reladomokotlin.generator.KotlinWrapperGenerator
import io.github.reladomokotlin.generator.KotlinRepositoryGenerator
import io.github.reladomokotlin.generator.migration.TypeMigration
import io.github.reladomokotlin.generator.model.MithraObjectDefinition
import io.github.reladomokotlin.generator.parser.ReladomoXmlParser
import io.github.reladomokotlin.generator.parser.EnhancedXmlParser
import io.github.reladomokotlin.generator.EnhancedKotlinCodeGenerator
import io.github.reladomokotlin.generator.result.*
import java.io.File

/**
 * Adapter to use enhanced generators with existing infrastructure
 */
class GeneratorAdapter {
    
    private val legacyParser = ReladomoXmlParser()
    private val enhancedParser = EnhancedXmlParser()
    private val enhancedGenerator = EnhancedKotlinCodeGenerator()
    private val legacyWrapperGenerator = KotlinWrapperGenerator()
    private val legacyRepoGenerator = KotlinRepositoryGenerator()
    
    /**
     * Generate using enhanced types with fallback to legacy
     */
    fun generateWithFallback(xmlFile: File, outputDir: File): Result<GeneratedFiles> {
        // Try enhanced parser first
        val enhancedResult = Result.runCatching {
            val parsedObject = enhancedParser.parseXmlFile(xmlFile)
            enhancedGenerator.generateKotlinWrapper(parsedObject, outputDir)
        }.flatten()
        
        return when (enhancedResult) {
            is Result.Success -> Result.success(GeneratedFiles(
                wrapper = enhancedResult.value.wrapper,
                repository = enhancedResult.value.repository,
                builder = enhancedResult.value.builder
            ))
            is Result.Failure -> {
                // Fallback to legacy parser and generator
                println("  ! Falling back to legacy generator due to: ${enhancedResult.error.message}")
                generateWithLegacy(xmlFile, outputDir)
            }
        }
    }
    
    private fun generateWithLegacy(xmlFile: File, outputDir: File): Result<GeneratedFiles> {
        return Result.runCatching {
            val definition = legacyParser.parse(xmlFile)
            
            val wrapperFile = legacyWrapperGenerator.generateToFile(definition, outputDir)
            val repoFile = legacyRepoGenerator.generateToFile(definition, outputDir)
            
            GeneratedFiles(
                wrapper = wrapperFile,
                repository = repoFile,
                builder = null
            )
        }
    }
    
    /**
     * Use enhanced parser with legacy generators
     */
    fun generateHybrid(xmlFile: File, outputDir: File): Result<GeneratedFiles> {
        return Result.runCatching {
            // Parse with enhanced parser
            val enhancedParsed = enhancedParser.parseXmlFile(xmlFile)
            
            // Convert to legacy format
            val legacyDefinition = convertToLegacyFormat(enhancedParsed)
            
            // Generate with legacy generators
            val wrapperFile = legacyWrapperGenerator.generateToFile(legacyDefinition, outputDir)
            val repoFile = legacyRepoGenerator.generateToFile(legacyDefinition, outputDir)
            
            GeneratedFiles(
                wrapper = wrapperFile,
                repository = repoFile,
                builder = null
            )
        }
    }
    
    private fun convertToLegacyFormat(
        enhanced: io.github.reladomokotlin.generator.model.ParsedMithraObject
    ): MithraObjectDefinition {
        // Convert back to legacy format
        val simpleAttrs = enhanced.simpleAttributes.map { attr ->
            io.github.reladomokotlin.generator.model.AttributeDefinition(
                name = attr.name,
                javaType = attr.type.xmlName,
                columnName = attr.columnName ?: attr.name,
                isPrimaryKey = attr.primaryKey,
                nullable = attr.nullable
            )
        }
        
        val asOfAttrs = enhanced.asOfAttributes.map { asOf ->
            io.github.reladomokotlin.generator.model.AsOfAttributeDefinition(
                name = asOf.name,
                fromColumn = asOf.fromColumnName,
                toColumn = asOf.toColumnName,
                infinityDate = asOf.infinityDate
            )
        }
        
        val legacyObjectType = when (enhanced.objectType) {
            io.github.reladomokotlin.generator.types.ObjectType.TRANSACTIONAL -> 
                io.github.reladomokotlin.generator.model.ObjectType.TRANSACTIONAL
            io.github.reladomokotlin.generator.types.ObjectType.READ_ONLY -> 
                io.github.reladomokotlin.generator.model.ObjectType.READ_ONLY
            io.github.reladomokotlin.generator.types.ObjectType.DATED_TRANSACTIONAL -> 
                io.github.reladomokotlin.generator.model.ObjectType.DATED_TRANSACTIONAL
            io.github.reladomokotlin.generator.types.ObjectType.BITEMPORAL -> 
                io.github.reladomokotlin.generator.model.ObjectType.DATED_TRANSACTIONAL
        }
        
        return MithraObjectDefinition(
            packageName = enhanced.packageName,
            className = enhanced.className,
            tableName = enhanced.tableName,
            objectType = legacyObjectType,
            attributes = simpleAttrs,
            asOfAttributes = asOfAttrs
        )
    }
    
    data class GeneratedFiles(
        val wrapper: File,
        val repository: File,
        val builder: File?
    )
}

// Extension function to flatten nested Results
private fun <T> Result<Result<T>>.flatten(): Result<T> = when (this) {
    is Result.Success -> value
    is Result.Failure -> Result.Failure(error)
}