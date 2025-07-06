package io.github.kotlinreladomo.core

import java.time.Instant

/**
 * Base repository interface for bitemporal entities.
 * Provides CRUD operations with temporal support.
 *
 * @param E The entity type, must implement BiTemporalEntity
 * @param ID The primary key type
 */
interface BiTemporalRepository<E : BiTemporalEntity, ID : Any> : BaseRepository<E, ID> {
    
    
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
     * Update an existing entity as of a specific business date.
     * 
     * @param entity The entity with updated values
     * @param businessDate The business date for the update
     * @return The updated entity
     * @throws EntityNotFoundException if the entity doesn't exist
     */
    fun update(entity: E, businessDate: Instant): E
    
    /**
     * Delete an entity by its primary key as of a specific business date.
     * 
     * @param id The primary key of the entity to delete
     * @param businessDate The business date for the deletion
     * @throws EntityNotFoundException if the entity doesn't exist
     */
    fun deleteByIdAsOf(id: ID, businessDate: Instant)
    
    
    /**
     * Find all entities as of specific business and processing dates.
     * 
     * @param businessDate The business date to query
     * @param processingDate The processing date to query
     * @return List of entities at the specified time
     */
    fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<E>
    
    /**
     * Get the history of an entity.
     * 
     * @param id The primary key
     * @return List of all versions of the entity
     */
    fun getHistory(id: ID): List<E>
}