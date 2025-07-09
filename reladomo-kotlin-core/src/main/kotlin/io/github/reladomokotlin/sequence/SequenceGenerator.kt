package io.github.reladomokotlin.sequence

/**
 * Interface for generating unique sequential IDs.
 * Implementations can use different strategies like database sequences,
 * Reladomo ObjectSequence tables, or in-memory counters.
 */
interface SequenceGenerator {
    /**
     * Get the next available ID for the given sequence name.
     * 
     * @param sequenceName The name of the sequence (typically the entity name)
     * @return The next available ID
     */
    fun getNextId(sequenceName: String): Long
    
    /**
     * Get a batch of IDs for the given sequence name.
     * This is more efficient when creating multiple entities.
     * 
     * @param sequenceName The name of the sequence
     * @param count The number of IDs to reserve
     * @return A list of reserved IDs
     */
    fun getNextIds(sequenceName: String, count: Int): List<Long> {
        return (1..count).map { getNextId(sequenceName) }
    }
    
    /**
     * Reset a sequence to a specific value.
     * 
     * @param sequenceName The name of the sequence
     * @param value The new starting value
     */
    fun resetSequence(sequenceName: String, value: Long)
}