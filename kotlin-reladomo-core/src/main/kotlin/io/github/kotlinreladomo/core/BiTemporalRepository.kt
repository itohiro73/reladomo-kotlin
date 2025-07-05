package io.github.kotlinreladomo.core

import java.time.Instant

/**
 * Base repository interface for bitemporal entities.
 * Provides CRUD operations with temporal support.
 *
 * @param E The entity type, must implement BiTemporalEntity
 * @param ID The primary key type
 */
interface BiTemporalRepository<E : BiTemporalEntity, ID : Any> {
    
    /**
     * Save a new entity to the database.
     * 
     * @param entity The entity to save
     * @return The saved entity
     */
    fun save(entity: E): E
    
    /**
     * Find an entity by its primary key at the current time.
     * 
     * @param id The primary key
     * @return The entity if found, null otherwise
     */
    fun findById(id: ID): E?
    
    /**
     * Find an entity by its primary key as of specific business and processing dates.
     * 
     * @param id The primary key
     * @param businessDate The business date to query
     * @param processingDate The processing date to query
     * @return The entity if found at the specified time, null otherwise
     */
    fun findByIdAsOf(id: ID, businessDate: Instant, processingDate: Instant): E?
    
    /**
     * Update an existing entity.
     * 
     * @param entity The entity with updated values
     * @return The updated entity
     * @throws EntityNotFoundException if the entity doesn't exist
     */
    fun update(entity: E): E
    
    /**
     * Delete an entity.
     * 
     * @param entity The entity to delete
     */
    fun delete(entity: E)
    
    /**
     * Delete an entity by its primary key.
     * 
     * @param id The primary key of the entity to delete
     * @throws EntityNotFoundException if the entity doesn't exist
     */
    fun deleteById(id: ID)
    
    /**
     * Find all entities at the current time.
     * 
     * @return List of all entities
     */
    fun findAll(): List<E>
    
    /**
     * Find all entities as of specific business and processing dates.
     * 
     * @param businessDate The business date to query
     * @param processingDate The processing date to query
     * @return List of entities at the specified time
     */
    fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<E>
}