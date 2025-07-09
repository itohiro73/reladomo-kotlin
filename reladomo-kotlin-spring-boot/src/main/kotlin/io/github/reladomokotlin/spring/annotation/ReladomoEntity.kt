package io.github.reladomokotlin.spring.annotation

import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties.CacheType

/**
 * Marks a class as a Reladomo entity for automatic discovery and configuration.
 * This annotation enables convention-over-configuration for Reladomo entities.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ReladomoEntity(
    /**
     * The database table name. If not specified, defaults to the class name
     * converted to snake_case (e.g., OrderItem -> order_item).
     */
    val tableName: String = "",
    
    /**
     * The connection manager name to use for this entity.
     * Defaults to "default".
     */
    val connectionManager: String = "default",
    
    /**
     * The cache type for this entity.
     * Defaults to PARTIAL for optimal performance.
     */
    val cacheType: CacheType = CacheType.PARTIAL,
    
    /**
     * Whether this entity uses bitemporal features.
     * When true, the entity must have business and processing date columns.
     */
    val bitemporal: Boolean = false
)

/**
 * Marks a property as the primary key for a Reladomo entity.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrimaryKey(
    /**
     * The database column name. If not specified, defaults to the property name
     * converted to snake_case (e.g., orderId -> order_id).
     */
    val columnName: String = ""
)

/**
 * Marks a property as the business date column for bitemporal entities.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class BusinessDate(
    /**
     * The database column name for the business date start.
     * Defaults to "BUSINESS_FROM".
     */
    val fromColumn: String = "BUSINESS_FROM",
    
    /**
     * The database column name for the business date end.
     * Defaults to "BUSINESS_THRU".
     */
    val thruColumn: String = "BUSINESS_THRU"
)

/**
 * Marks a property as the processing date column for bitemporal entities.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProcessingDate(
    /**
     * The database column name for the processing date start.
     * Defaults to "PROCESSING_FROM".
     */
    val fromColumn: String = "PROCESSING_FROM",
    
    /**
     * The database column name for the processing date end.
     * Defaults to "PROCESSING_THRU".
     */
    val thruColumn: String = "PROCESSING_THRU"
)

/**
 * Marks a property as a database column with custom mapping.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    /**
     * The database column name. If not specified, defaults to the property name
     * converted to snake_case.
     */
    val name: String = "",
    
    /**
     * Whether the column allows null values.
     * Defaults to the nullability of the Kotlin property type.
     */
    val nullable: Boolean = true,
    
    /**
     * The maximum length for string columns.
     * Only applies to String properties.
     */
    val length: Int = 255
)

/**
 * Defines a relationship to another Reladomo entity.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Relationship(
    /**
     * The target entity class.
     */
    val targetEntity: String,
    
    /**
     * The relationship expression (e.g., "this.customerId = Customer.id").
     */
    val expression: String,
    
    /**
     * Whether this is a one-to-many relationship.
     * Defaults to false (one-to-one).
     */
    val oneToMany: Boolean = false
)