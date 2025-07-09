package io.github.reladomokotlin.generator.types

/**
 * Sealed class representing different types of attributes in Reladomo
 */
sealed class AttributeType {
    abstract val name: String
    
    data class Simple(
        override val name: String,
        val type: ReladomoType,
        val nullable: Boolean,
        val columnName: String? = null,
        val primaryKey: Boolean = false,
        val identity: Boolean = false,
        val trim: Boolean = false,
        val pooled: Boolean = false
    ) : AttributeType() {
        init {
            require(name.isNotBlank()) { "Attribute name cannot be blank" }
            if (identity) {
                require(primaryKey) { "Identity attributes must be primary keys" }
                require(type is ReladomoType.Primitive && 
                    (type.type is PrimitiveType.Long || type.type is PrimitiveType.Int)) {
                    "Identity attributes must be numeric (Int or Long)"
                }
            }
        }
    }
    
    data class AsOfAttribute(
        override val name: String,
        val type: TemporalType,
        val fromColumnName: String,
        val toColumnName: String,
        val infinityDate: String? = null,
        val isProcessingDate: Boolean = false,
        val timezoneConversion: TimezoneConversion = TimezoneConversion.NONE
    ) : AttributeType() {
        init {
            require(name.isNotBlank()) { "AsOf attribute name cannot be blank" }
            require(fromColumnName.isNotBlank()) { "From column name cannot be blank" }
            require(toColumnName.isNotBlank()) { "To column name cannot be blank" }
        }
    }
    
    data class Relationship(
        override val name: String,
        val relatedObject: String,
        val cardinality: Cardinality,
        val reverseRelationshipName: String? = null,
        val parameters: List<RelationshipParameter> = emptyList(),
        val orderBy: String? = null
    ) : AttributeType() {
        init {
            require(name.isNotBlank()) { "Relationship name cannot be blank" }
            require(relatedObject.isNotBlank()) { "Related object cannot be blank" }
            if (cardinality == Cardinality.MANY_TO_MANY) {
                require(reverseRelationshipName != null) {
                    "Many-to-many relationships must have a reverse relationship name"
                }
            }
        }
    }
}

/**
 * Temporal type for AsOf attributes
 */
enum class TemporalType {
    BUSINESS_DATE,
    PROCESSING_DATE;
    
    fun toColumnPrefix(): String = when (this) {
        BUSINESS_DATE -> "BUSINESS"
        PROCESSING_DATE -> "PROCESSING"
    }
}

/**
 * Cardinality for relationships
 */
enum class Cardinality {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY;
    
    fun isToMany(): Boolean = this == ONE_TO_MANY || this == MANY_TO_MANY
    fun isToOne(): Boolean = this == ONE_TO_ONE || this == MANY_TO_ONE
}

/**
 * Timezone conversion options for temporal attributes
 */
enum class TimezoneConversion {
    NONE,
    CONVERT,
    UTC_CONVERT;
    
    companion object {
        fun fromString(value: String?): TimezoneConversion = when (value?.lowercase()) {
            "convert" -> CONVERT
            "utcconvert" -> UTC_CONVERT
            else -> NONE
        }
    }
}

/**
 * Relationship parameter for defining foreign key mappings
 */
data class RelationshipParameter(
    val from: String,
    val to: String
) {
    init {
        require(from.isNotBlank()) { "From parameter cannot be blank" }
        require(to.isNotBlank()) { "To parameter cannot be blank" }
    }
}

/**
 * Object type enumeration
 */
enum class ObjectType {
    TRANSACTIONAL,
    READ_ONLY,
    DATED_TRANSACTIONAL,
    BITEMPORAL;
    
    val isTransactional: Boolean
        get() = this != READ_ONLY
    val isTemporal: Boolean
        get() = this == DATED_TRANSACTIONAL || this == BITEMPORAL
    val isBitemporal: Boolean
        get() = this == BITEMPORAL
    
    companion object {
        fun fromAsOfAttributes(asOfAttributes: List<AttributeType.AsOfAttribute>, isReadOnly: Boolean): ObjectType {
            return when {
                isReadOnly -> READ_ONLY
                asOfAttributes.size == 2 -> BITEMPORAL
                asOfAttributes.size == 1 -> DATED_TRANSACTIONAL
                else -> TRANSACTIONAL
            }
        }
    }
}