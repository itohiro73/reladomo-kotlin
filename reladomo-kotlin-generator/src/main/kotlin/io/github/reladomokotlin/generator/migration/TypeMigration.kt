package io.github.reladomokotlin.generator.migration

import io.github.reladomokotlin.generator.model.MithraObjectDefinition
import io.github.reladomokotlin.generator.model.AttributeDefinition
import io.github.reladomokotlin.generator.model.AsOfAttributeDefinition
import io.github.reladomokotlin.generator.model.ParsedMithraObject as EnhancedParsedMithraObject
import io.github.reladomokotlin.generator.types.*

/**
 * Utility to migrate from old model types to new enhanced types
 */
object TypeMigration {
    
    fun migrateToEnhancedModel(old: MithraObjectDefinition): EnhancedParsedMithraObject {
        // Convert attributes
        val attributes = mutableListOf<AttributeType>()
        
        // Add simple attributes
        old.attributes.forEach { attr ->
            attributes.add(convertToSimpleAttribute(attr))
        }
        
        // Add AsOf attributes
        old.asOfAttributes.forEach { asOf ->
            attributes.add(convertToAsOfAttribute(asOf))
        }
        
        // Determine object type
        val objectType = when (old.objectType) {
            io.github.reladomokotlin.generator.model.ObjectType.TRANSACTIONAL -> ObjectType.TRANSACTIONAL
            io.github.reladomokotlin.generator.model.ObjectType.READ_ONLY -> ObjectType.READ_ONLY
            io.github.reladomokotlin.generator.model.ObjectType.DATED_TRANSACTIONAL -> ObjectType.DATED_TRANSACTIONAL
            io.github.reladomokotlin.generator.model.ObjectType.DATED_READ_ONLY -> {
                // Map DATED_READ_ONLY to READ_ONLY since the new model handles this differently
                ObjectType.READ_ONLY
            }
        }
        
        return EnhancedParsedMithraObject(
            className = old.className,
            packageName = old.packageName,
            tableName = old.tableName,
            attributes = attributes,
            objectType = objectType
        )
    }
    
    private fun convertToSimpleAttribute(attr: AttributeDefinition): AttributeType.Simple {
        val type = TypeMapper.fromXmlType(attr.javaType)
        return AttributeType.Simple(
            name = attr.name,
            type = type,
            nullable = attr.nullable,
            columnName = attr.columnName,
            primaryKey = attr.isPrimaryKey
        )
    }
    
    private fun convertToAsOfAttribute(asOf: AsOfAttributeDefinition): AttributeType.AsOfAttribute {
        val temporalType = when {
            asOf.isBusinessDate -> TemporalType.BUSINESS_DATE
            asOf.isProcessingDate -> TemporalType.PROCESSING_DATE
            else -> TemporalType.BUSINESS_DATE
        }
        
        return AttributeType.AsOfAttribute(
            name = asOf.name,
            type = temporalType,
            fromColumnName = asOf.fromColumn,
            toColumnName = asOf.toColumn,
            infinityDate = asOf.infinityDate,
            isProcessingDate = asOf.isProcessingDate
        )
    }
}