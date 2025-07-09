package io.github.reladomokotlin.core

import com.gs.fw.common.mithra.finder.Operation
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import io.github.reladomokotlin.core.exceptions.EntityNotFoundException
import io.github.reladomokotlin.query.QueryContext
import java.sql.Timestamp
import java.time.Instant

/**
 * Abstract base class for bitemporal repositories.
 * Provides common CRUD operations with temporal support.
 *
 * @param E The entity type (Kotlin wrapper)
 * @param ID The primary key type
 * @param R The Reladomo object type
 */
abstract class AbstractBiTemporalRepository<E : BiTemporalEntity, ID : Any, R : ReladomoObject> 
    : BiTemporalRepository<E, ID> {
    
    /**
     * Get the Reladomo finder for this entity type.
     */
    protected abstract fun getFinder(): ReladomoFinder<R>
    
    /**
     * Convert a Reladomo object to a Kotlin entity.
     */
    protected abstract fun toEntity(reladomoObject: R): E
    
    /**
     * Convert a Kotlin entity to a Reladomo object.
     */
    protected abstract fun fromEntity(entity: E): R
    
    /**
     * Extract the primary key from an entity.
     */
    protected abstract fun getPrimaryKey(entity: E): ID
    
    /**
     * Create an operation to find by primary key.
     */
    protected abstract fun createPrimaryKeyOperation(id: ID): Operation
    
    /**
     * Get the business date attribute for temporal queries.
     */
    protected abstract fun getBusinessDateAttribute(): TimestampAttribute<*>
    
    /**
     * Get the processing date attribute for temporal queries.
     */
    protected abstract fun getProcessingDateAttribute(): TimestampAttribute<*>
    
    override fun save(entity: E): E {
        val reladomoObject = fromEntity(entity)
        reladomoObject.insert()
        return toEntity(reladomoObject)
    }
    
    override fun findById(id: ID): E? {
        val operation = createPrimaryKeyOperation(id)
        val result = getFinder().findOne(operation)
        return result?.let { toEntity(it) }
    }
    
    override fun findByIdAsOf(id: ID, businessDate: Instant, processingDate: Instant): E? {
        val operation = createPrimaryKeyOperation(id)
            .and(getBusinessDateAttribute().eq(Timestamp.from(businessDate)))
            .and(getProcessingDateAttribute().eq(Timestamp.from(processingDate)))
        
        val result = getFinder().findOne(operation)
        return result?.let { toEntity(it) }
    }
    
    override fun update(entity: E): E {
        val existingEntity = findById(getPrimaryKey(entity))
            ?: throw EntityNotFoundException("Entity not found with id: ${getPrimaryKey(entity)}")
        
        val reladomoObject = fromEntity(entity)
        
        // For bitemporal updates, we need to terminate the old record and insert a new one
        val oldReladomoObject = fromEntity(existingEntity)
        oldReladomoObject.terminate()
        reladomoObject.insert()
        
        return toEntity(reladomoObject)
    }
    
    override fun delete(entity: E) {
        val reladomoObject = fromEntity(entity)
        reladomoObject.delete()
    }
    
    override fun deleteById(id: ID) {
        val entity = findById(id)
            ?: throw EntityNotFoundException("Entity not found with id: $id")
        delete(entity)
    }
    
    override fun findAll(): List<E> {
        val operation = getFinder().all()
        val results = getFinder().findMany(operation)
        return results.map { toEntity(it) }
    }
    
    override fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<E> {
        val operation = getBusinessDateAttribute().eq(Timestamp.from(businessDate))
            .and(getProcessingDateAttribute().eq(Timestamp.from(processingDate)))
        
        val results = getFinder().findMany(operation)
        return results.map { toEntity(it) }
    }
    
    /**
     * Execute a custom operation and return the results.
     */
    protected fun findMany(operation: Operation): List<E> {
        val results = getFinder().findMany(operation)
        return results.map { toEntity(it) }
    }
    
    /**
     * Execute a custom operation and return a single result.
     */
    protected fun findOne(operation: Operation): E? {
        val result = getFinder().findOne(operation)
        return result?.let { toEntity(it) }
    }
    
    /**
     * Find entities using DSL query builder.
     * 
     * Example:
     * ```
     * repository.find {
     *     customerId eq 100
     *     status `in` listOf("PENDING", "PROCESSING")
     *     amount greaterThan 100.00
     * }
     * ```
     */
    open fun find(query: QueryContext.() -> Unit): List<E> {
        val context = QueryContext()
        context.query()
        val operation = context.build()
        return findMany(operation)
    }
    
    /**
     * Find a single entity using DSL query builder.
     * Returns null if no entity is found.
     * 
     * Example:
     * ```
     * repository.findOne {
     *     orderId eq 123
     * }
     * ```
     */
    open fun findOne(query: QueryContext.() -> Unit): E? {
        val context = QueryContext()
        context.query()
        val operation = context.build()
        return findOne(operation)
    }
    
    /**
     * Find entities as of specific dates using DSL query builder.
     * 
     * Example:
     * ```
     * repository.findAsOf(businessDate, processingDate) {
     *     customerId eq 100
     *     status eq "COMPLETED"
     * }
     * ```
     */
    open fun findAsOf(
        businessDate: Instant,
        processingDate: Instant,
        query: QueryContext.() -> Unit
    ): List<E> {
        val context = QueryContext()
        context.query()
        
        val baseOperation = context.build()
        val temporalOperation = baseOperation
            .and(getBusinessDateAttribute().eq(Timestamp.from(businessDate)))
            .and(getProcessingDateAttribute().eq(Timestamp.from(processingDate)))
        
        return findMany(temporalOperation)
    }
    
    /**
     * Count entities matching the query.
     */
    open fun count(query: QueryContext.() -> Unit): Int {
        val context = QueryContext()
        context.query()
        val operation = context.build()
        return getFinder().findMany(operation).size
    }
    
    /**
     * Check if any entity exists matching the query.
     */
    open fun exists(query: QueryContext.() -> Unit): Boolean {
        return findOne(query) != null
    }
}