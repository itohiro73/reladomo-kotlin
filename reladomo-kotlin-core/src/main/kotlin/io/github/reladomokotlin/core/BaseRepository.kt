package io.github.reladomokotlin.core

import com.gs.fw.common.mithra.finder.Operation

/**
 * Base repository interface for all entities.
 * Provides basic CRUD operations.
 *
 * @param T The entity type
 * @param ID The primary key type
 */
interface BaseRepository<T : Any, ID : Any> {
    
    /**
     * Save a new entity to the database.
     * 
     * @param entity The entity to save
     * @return The saved entity
     */
    fun save(entity: T): T
    
    /**
     * Find an entity by its primary key.
     * 
     * @param id The primary key
     * @return The entity if found, null otherwise
     */
    fun findById(id: ID): T?
    
    /**
     * Find all entities.
     * 
     * @return All entities
     */
    fun findAll(): List<T>
    
    /**
     * Find entities matching the given operation.
     *
     * @param operation The operation to match
     * @return Matching entities
     */
    fun findBy(operation: Operation): List<T>
    
    /**
     * Count entities matching the given operation.
     *
     * @param operation The operation to match
     * @return Count of matching entities
     */
    fun countBy(operation: Operation): Long
    
    /**
     * Update an existing entity.
     * 
     * @param entity The entity to update
     * @return The updated entity
     */
    fun update(entity: T): T
    
    /**
     * Delete an entity by its primary key.
     * 
     * @param id The primary key
     */
    fun deleteById(id: ID)
    
    /**
     * Delete an entity.
     * 
     * @param entity The entity to delete
     */
    fun delete(entity: T)
    
    /**
     * Delete all entities.
     */
    fun deleteAll()
    
    /**
     * Count all entities.
     * 
     * @return The total count
     */
    fun count(): Long
}