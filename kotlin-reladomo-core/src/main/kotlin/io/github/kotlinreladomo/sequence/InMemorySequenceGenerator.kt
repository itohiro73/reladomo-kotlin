package io.github.kotlinreladomo.sequence

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Simple in-memory sequence generator for testing and development.
 * This implementation is thread-safe but does not persist sequences.
 */
class InMemorySequenceGenerator(
    private val defaultStartValue: Long = 1000L,
    private val incrementBy: Int = 1
) : SequenceGenerator {
    
    private val sequences = ConcurrentHashMap<String, AtomicLong>()
    
    override fun getNextId(sequenceName: String): Long {
        val sequence = sequences.computeIfAbsent(sequenceName) { 
            AtomicLong(defaultStartValue) 
        }
        return sequence.getAndAdd(incrementBy.toLong())
    }
    
    override fun getNextIds(sequenceName: String, count: Int): List<Long> {
        require(count > 0) { "Count must be positive" }
        
        val sequence = sequences.computeIfAbsent(sequenceName) { 
            AtomicLong(defaultStartValue) 
        }
        
        val startId = sequence.getAndAdd((count * incrementBy).toLong())
        return (0 until count).map { startId + (it * incrementBy) }
    }
    
    override fun resetSequence(sequenceName: String, value: Long) {
        sequences[sequenceName] = AtomicLong(value)
    }
    
    /**
     * Clear all sequences. Useful for testing.
     */
    fun clearAll() {
        sequences.clear()
    }
}