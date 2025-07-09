package io.github.reladomokotlin.generator.model

import io.github.reladomokotlin.generator.types.AttributeType
import io.github.reladomokotlin.generator.types.ObjectType
import io.github.reladomokotlin.generator.types.TemporalType

/**
 * Validated representation of a parsed Mithra object with comprehensive type safety
 */
data class ParsedMithraObject(
    val className: String,
    val packageName: String,
    val tableName: String,
    val attributes: List<AttributeType>,
    val objectType: ObjectType,
    val defaultTableName: String? = null,
    val superClass: String? = null,
    val interfaces: List<String> = emptyList()
) {
    // Derived properties
    val simpleAttributes: List<AttributeType.Simple> = 
        attributes.filterIsInstance<AttributeType.Simple>()
    
    val asOfAttributes: List<AttributeType.AsOfAttribute> = 
        attributes.filterIsInstance<AttributeType.AsOfAttribute>()
    
    val relationships: List<AttributeType.Relationship> = 
        attributes.filterIsInstance<AttributeType.Relationship>()
    
    val primaryKeyAttributes: List<AttributeType.Simple> = 
        simpleAttributes.filter { it.primaryKey }
    
    val nonPrimaryKeyAttributes: List<AttributeType.Simple> = 
        simpleAttributes.filter { !it.primaryKey }
    
    val identityAttribute: AttributeType.Simple? = 
        simpleAttributes.find { it.identity }
    
    val businessDateAttribute: AttributeType.AsOfAttribute? = 
        asOfAttributes.find { it.type == TemporalType.BUSINESS_DATE }
    
    val processingDateAttribute: AttributeType.AsOfAttribute? = 
        asOfAttributes.find { it.type == TemporalType.PROCESSING_DATE || it.isProcessingDate }
    
    val fullyQualifiedClassName: String = "$packageName.$className"
    
    init {
        // Validation
        require(className.isNotBlank()) { "className cannot be blank" }
        require(packageName.isNotBlank()) { "packageName cannot be blank" }
        require(tableName.isNotBlank()) { "tableName cannot be blank" }
        
        validatePrimaryKey()
        validateTemporalConsistency()
        validateIdentityAttribute()
        validateAttributeNames()
        validateRelationships()
    }
    
    private fun validatePrimaryKey() {
        require(primaryKeyAttributes.isNotEmpty()) { 
            "Object $className must have at least one primary key attribute" 
        }
        
        // Ensure primary key attributes are not nullable (unless temporal)
        primaryKeyAttributes.forEach { attr ->
            if (!objectType.isTemporal) {
                require(!attr.nullable) {
                    "Primary key attribute '${attr.name}' in $className cannot be nullable"
                }
            }
        }
    }
    
    private fun validateTemporalConsistency() {
        when (objectType) {
            ObjectType.TRANSACTIONAL -> {
                require(asOfAttributes.isEmpty()) { 
                    "Transactional object $className should not have AsOf attributes" 
                }
            }
            ObjectType.DATED_TRANSACTIONAL -> {
                require(asOfAttributes.size == 1) { 
                    "Dated transactional object $className should have exactly one AsOf attribute, found: ${asOfAttributes.size}" 
                }
                require(businessDateAttribute != null) {
                    "Dated transactional object $className must have a business date attribute"
                }
            }
            ObjectType.BITEMPORAL -> {
                require(asOfAttributes.size == 2) { 
                    "Bitemporal object $className should have exactly two AsOf attributes, found: ${asOfAttributes.size}" 
                }
                require(businessDateAttribute != null) {
                    "Bitemporal object $className must have a business date attribute"
                }
                require(processingDateAttribute != null) {
                    "Bitemporal object $className must have a processing date attribute"
                }
            }
            ObjectType.READ_ONLY -> {
                // Read-only objects can have any number of AsOf attributes
            }
        }
    }
    
    private fun validateIdentityAttribute() {
        if (identityAttribute != null) {
            require(objectType.isTransactional) {
                "Identity attributes are only supported for transactional objects"
            }
            require(primaryKeyAttributes.size == 1) {
                "Objects with identity attributes must have exactly one primary key"
            }
            require(primaryKeyAttributes.first() == identityAttribute) {
                "Identity attribute must be the primary key"
            }
        }
    }
    
    private fun validateAttributeNames() {
        val allNames = attributes.map { it.name }
        val duplicates = allNames.groupBy { it }.filter { it.value.size > 1 }.keys
        
        require(duplicates.isEmpty()) {
            "Duplicate attribute names found in $className: ${duplicates.joinToString()}"
        }
        
        // Check for reserved names
        val reservedNames = setOf("class", "toString", "hashCode", "equals", "copy")
        val conflicts = allNames.filter { it in reservedNames }
        
        require(conflicts.isEmpty()) {
            "Reserved attribute names found in $className: ${conflicts.joinToString()}"
        }
    }
    
    private fun validateRelationships() {
        relationships.forEach { rel ->
            // Validate relationship parameters match existing attributes
            rel.parameters.forEach { param ->
                require(simpleAttributes.any { it.name == param.from }) {
                    "Relationship '${rel.name}' references non-existent attribute '${param.from}'"
                }
            }
            
            // Validate order by references existing attributes
            if (rel.orderBy != null) {
                val orderByAttrs = rel.orderBy.split(",").map { it.trim() }
                orderByAttrs.forEach { orderAttr ->
                    val attrName = orderAttr.removePrefix("-") // Handle DESC ordering
                    require(attrName.isNotBlank()) {
                        "Invalid order by attribute in relationship '${rel.name}'"
                    }
                }
            }
        }
    }
    
    /**
     * Get the effective table name considering temporal aspects
     */
    fun getEffectiveTableName(): String = when {
        objectType.isTemporal && defaultTableName != null -> defaultTableName
        else -> tableName
    }
    
    /**
     * Check if this object requires special handling for infinity dates
     */
    fun requiresInfinityHandling(): Boolean = objectType.isTemporal
    
    /**
     * Get all columns including temporal columns
     */
    fun getAllColumns(): List<String> {
        val columns = mutableListOf<String>()
        
        // Add simple attribute columns
        simpleAttributes.forEach { attr ->
            columns.add(attr.columnName ?: attr.name.toUpperCase())
        }
        
        // Add temporal columns
        asOfAttributes.forEach { asOf ->
            columns.add(asOf.fromColumnName)
            columns.add(asOf.toColumnName)
        }
        
        return columns
    }
}