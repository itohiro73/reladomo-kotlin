package io.github.reladomokotlin.spring.repository

import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.Repository
import java.time.Instant

/**
 * Base repository interface for Reladomo entities.
 * Follows Spring Data conventions for familiarity.
 * 
 * @param T The entity type
 * @param ID The primary key type
 */
@NoRepositoryBean
interface ReladomoRepository<T : Any, ID : Any> : Repository<T, ID> {
    
    // Basic CRUD operations
    
    /**
     * Saves the given entity.
     */
    fun save(entity: T): T
    
    /**
     * Saves all given entities.
     */
    fun saveAll(entities: Iterable<T>): List<T>
    
    /**
     * Retrieves an entity by its id.
     */
    fun findById(id: ID): T?
    
    /**
     * Returns whether an entity with the given id exists.
     */
    fun existsById(id: ID): Boolean
    
    /**
     * Returns all instances of the type.
     */
    fun findAll(): List<T>
    
    /**
     * Returns all instances of the type with the given IDs.
     */
    fun findAllById(ids: Iterable<ID>): List<T>
    
    /**
     * Returns the number of entities available.
     */
    fun count(): Long
    
    /**
     * Deletes the entity with the given id.
     */
    fun deleteById(id: ID)
    
    /**
     * Deletes a given entity.
     */
    fun delete(entity: T)
    
    /**
     * Deletes the given entities.
     */
    fun deleteAll(entities: Iterable<T>)
    
    /**
     * Deletes all entities.
     */
    fun deleteAll()
}

/**
 * Extension of ReladomoRepository for bitemporal entities.
 */
@NoRepositoryBean
interface BiTemporalReladomoRepository<T : Any, ID : Any> : ReladomoRepository<T, ID> {
    
    /**
     * Finds an entity by ID as of specific business and processing dates.
     */
    fun findByIdAsOf(id: ID, businessDate: Instant, processingDate: Instant = Instant.now()): T?
    
    /**
     * Updates an entity as of a specific business date.
     */
    fun update(entity: T, businessDate: Instant = Instant.now()): T
    
    /**
     * Deletes an entity as of a specific business date (terminates it).
     */
    fun deleteByIdAsOf(id: ID, businessDate: Instant = Instant.now())
    
    /**
     * Finds all entities as of specific dates.
     */
    fun findAllAsOf(businessDate: Instant, processingDate: Instant = Instant.now()): List<T>
    
    /**
     * Gets the history of an entity.
     */
    fun getHistory(id: ID): List<T>
}