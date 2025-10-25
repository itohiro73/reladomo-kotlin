package io.github.reladomokotlin.generator.model

// Import the enhanced types
import io.github.reladomokotlin.generator.types.ObjectType as EnhancedObjectType

/**
 * Legacy models for backward compatibility.
 * These will be phased out as we migrate to the enhanced type system.
 */
data class MithraObjectDefinition(
    val packageName: String,
    val className: String,
    val tableName: String,
    val objectType: ObjectType = ObjectType.TRANSACTIONAL,
    val attributes: List<AttributeDefinition>,
    val asOfAttributes: List<AsOfAttributeDefinition>,
    val relationships: List<RelationshipDefinition> = emptyList()
) {
    val isBitemporal: Boolean
        get() = asOfAttributes.size >= 2

    val isUnitemporal: Boolean
        get() = asOfAttributes.size == 1

    val primaryKeyAttributes: List<AttributeDefinition>
        get() = attributes.filter { it.isPrimaryKey }
}

/**
 * Legacy ObjectType enum - maps to enhanced ObjectType
 */
enum class ObjectType {
    TRANSACTIONAL,
    READ_ONLY,
    DATED_TRANSACTIONAL,
    DATED_READ_ONLY;
    
    fun toEnhanced(): EnhancedObjectType = when (this) {
        TRANSACTIONAL -> EnhancedObjectType.TRANSACTIONAL
        READ_ONLY -> EnhancedObjectType.READ_ONLY
        DATED_TRANSACTIONAL -> EnhancedObjectType.DATED_TRANSACTIONAL
        DATED_READ_ONLY -> EnhancedObjectType.READ_ONLY // Map to READ_ONLY as enhanced doesn't have DATED_READ_ONLY
    }
}

/**
 * Represents a regular attribute in a Mithra object.
 */
data class AttributeDefinition(
    val name: String,
    val javaType: String,
    val columnName: String,
    val isPrimaryKey: Boolean = false,
    val nullable: Boolean = true,
    val maxLength: Int? = null
) {
    val kotlinType: String
        get() = when (javaType) {
            "boolean" -> "Boolean"
            "byte" -> "Byte"
            "short" -> "Short"
            "int" -> "Int"
            "long" -> "Long"
            "float" -> "Float"
            "double" -> "Double"
            "String" -> "String"
            "Date", "Timestamp" -> "java.time.Instant"
            "BigDecimal" -> "java.math.BigDecimal"
            else -> javaType
        }
}

/**
 * Represents a temporal (AsOf) attribute in a Mithra object.
 */
data class AsOfAttributeDefinition(
    val name: String,
    val fromColumn: String,
    val toColumn: String,
    val toIsInclusive: Boolean = true,
    val infinityDate: String? = null,
    val defaultIfNotSpecified: String? = null
) {
    val isBusinessDate: Boolean
        get() = name.equals("businessDate", ignoreCase = true)
    
    val isProcessingDate: Boolean
        get() = name.equals("processingDate", ignoreCase = true)
}

/**
 * Represents a relationship between Mithra objects.
 */
data class RelationshipDefinition(
    val name: String,
    val relatedObject: String,
    val cardinality: Cardinality,
    val reverseRelationshipName: String? = null,
    val parameters: Map<String, String> = emptyMap()
)

/**
 * Represents the cardinality of a relationship.
 */
enum class Cardinality {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY
}