package io.github.reladomokotlin.core

import java.time.Instant

/**
 * Base repository interface for unitemporal entities.
 * Provides CRUD operations with temporal support for processing time only.
 *
 * @param E The entity type, must implement UniTemporalEntity
 * @param ID The primary key type
 */
interface UniTemporalRepository<E : UniTemporalEntity, ID : Any> : BaseRepository<E, ID> {

    /**
     * Find an entity by its primary key as of a specific processing date.
     *
     * @param id The primary key
     * @param processingDate The processing date to query (when the data was recorded in the system)
     * @return The entity if found at the specified time, null otherwise
     */
    fun findByIdAsOf(id: ID, processingDate: Instant): E?

    /**
     * Find all entities as of a specific processing date.
     *
     * @param processingDate The processing date to query
     * @return List of entities at the specified time
     */
    fun findAllAsOf(processingDate: Instant): List<E>

    /**
     * Get the history of an entity.
     * Returns all versions of the entity ordered by processing date.
     *
     * @param id The primary key
     * @return List of all versions of the entity
     */
    fun getHistory(id: ID): List<E>
}
