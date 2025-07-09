package io.github.reladomokotlin.spring.scanner

import io.github.reladomokotlin.spring.annotation.*
import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties.CacheType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

/**
 * Scans the classpath for Reladomo entities annotated with @ReladomoEntity.
 */
@Component
class ReladomoEntityScanner(
    private val applicationContext: ApplicationContext
) {
    private val logger = LoggerFactory.getLogger(ReladomoEntityScanner::class.java)
    
    /**
     * Scans the specified packages for Reladomo entities.
     */
    fun scanForEntities(basePackages: List<String>): List<EntityMetadata> {
        if (basePackages.isEmpty()) {
            logger.warn("No base packages configured for entity scanning")
            return emptyList()
        }
        
        logger.info("Scanning for Reladomo entities in packages: {}", basePackages)
        
        val scanner = createScanner()
        val entities = mutableListOf<EntityMetadata>()
        
        for (basePackage in basePackages) {
            val candidates = scanner.findCandidateComponents(basePackage)
            for (candidate in candidates) {
                try {
                    val entityClass = Class.forName(candidate.beanClassName).kotlin
                    val metadata = extractEntityMetadata(entityClass)
                    entities.add(metadata)
                    logger.debug("Found Reladomo entity: {}", entityClass.simpleName)
                } catch (e: Exception) {
                    logger.error("Failed to process entity class: ${candidate.beanClassName}", e)
                }
            }
        }
        
        logger.info("Found {} Reladomo entities", entities.size)
        return entities
    }
    
    private fun createScanner(): ClassPathScanningCandidateComponentProvider {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(ReladomoEntity::class.java))
        return scanner
    }
    
    private fun extractEntityMetadata(entityClass: KClass<*>): EntityMetadata {
        val annotation = entityClass.findAnnotation<ReladomoEntity>()
            ?: throw IllegalArgumentException("Class ${entityClass.simpleName} is not annotated with @ReladomoEntity")
        
        val className = entityClass.qualifiedName ?: entityClass.simpleName ?: "Unknown"
        val tableName = if (annotation.tableName.isEmpty()) {
            toSnakeCase(entityClass.simpleName ?: "unknown")
        } else {
            annotation.tableName
        }
        
        val properties = extractProperties(entityClass)
        
        return EntityMetadata(
            className = className,
            simpleName = entityClass.simpleName ?: "Unknown",
            tableName = tableName,
            connectionManager = annotation.connectionManager,
            cacheType = annotation.cacheType,
            bitemporal = annotation.bitemporal,
            properties = properties
        )
    }
    
    private fun extractProperties(entityClass: KClass<*>): List<PropertyMetadata> {
        return entityClass.memberProperties.mapNotNull { property ->
            val propertyName = property.name
            val propertyType = property.returnType
            
            when {
                property.hasAnnotation<PrimaryKey>() -> {
                    val annotation = property.findAnnotation<PrimaryKey>()!!
                    PropertyMetadata(
                        name = propertyName,
                        columnName = if (annotation.columnName.isEmpty()) {
                            toSnakeCase(propertyName)
                        } else {
                            annotation.columnName
                        },
                        type = propertyType.javaType.typeName,
                        isPrimaryKey = true,
                        nullable = propertyType.isMarkedNullable
                    )
                }
                
                property.hasAnnotation<BusinessDate>() -> {
                    val annotation = property.findAnnotation<BusinessDate>()!!
                    PropertyMetadata(
                        name = propertyName,
                        columnName = annotation.fromColumn,
                        type = propertyType.javaType.typeName,
                        isBusinessDate = true,
                        thruColumnName = annotation.thruColumn
                    )
                }
                
                property.hasAnnotation<ProcessingDate>() -> {
                    val annotation = property.findAnnotation<ProcessingDate>()!!
                    PropertyMetadata(
                        name = propertyName,
                        columnName = annotation.fromColumn,
                        type = propertyType.javaType.typeName,
                        isProcessingDate = true,
                        thruColumnName = annotation.thruColumn
                    )
                }
                
                property.hasAnnotation<Column>() -> {
                    val annotation = property.findAnnotation<Column>()!!
                    PropertyMetadata(
                        name = propertyName,
                        columnName = if (annotation.name.isEmpty()) {
                            toSnakeCase(propertyName)
                        } else {
                            annotation.name
                        },
                        type = propertyType.javaType.typeName,
                        nullable = annotation.nullable,
                        length = annotation.length
                    )
                }
                
                property.hasAnnotation<Relationship>() -> {
                    val annotation = property.findAnnotation<Relationship>()!!
                    PropertyMetadata(
                        name = propertyName,
                        type = propertyType.javaType.typeName,
                        isRelationship = true,
                        relationshipTarget = annotation.targetEntity,
                        relationshipExpression = annotation.expression,
                        isOneToMany = annotation.oneToMany
                    )
                }
                
                else -> {
                    // Skip properties without Reladomo annotations
                    null
                }
            }
        }
    }
    
    private fun toSnakeCase(name: String): String {
        return name.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
    }
}

/**
 * Metadata about a Reladomo entity extracted from annotations.
 */
data class EntityMetadata(
    val className: String,
    val simpleName: String,
    val tableName: String,
    val connectionManager: String,
    val cacheType: CacheType,
    val bitemporal: Boolean,
    val properties: List<PropertyMetadata>
) {
    val primaryKeyProperty: PropertyMetadata?
        get() = properties.find { it.isPrimaryKey }
    
    val businessDateProperty: PropertyMetadata?
        get() = properties.find { it.isBusinessDate }
    
    val processingDateProperty: PropertyMetadata?
        get() = properties.find { it.isProcessingDate }
}

/**
 * Metadata about a property of a Reladomo entity.
 */
data class PropertyMetadata(
    val name: String,
    val columnName: String? = null,
    val type: String,
    val isPrimaryKey: Boolean = false,
    val isBusinessDate: Boolean = false,
    val isProcessingDate: Boolean = false,
    val isRelationship: Boolean = false,
    val nullable: Boolean = true,
    val length: Int = 255,
    val thruColumnName: String? = null,
    val relationshipTarget: String? = null,
    val relationshipExpression: String? = null,
    val isOneToMany: Boolean = false
)