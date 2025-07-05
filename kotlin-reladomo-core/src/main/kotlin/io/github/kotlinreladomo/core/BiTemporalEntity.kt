package io.github.kotlinreladomo.core

import java.time.Instant

/**
 * Base interface for all bitemporal entities in the Kotlin Reladomo wrapper.
 * 
 * Bitemporal entities track data along two time dimensions:
 * - Business Date: When the fact was true in the real world
 * - Processing Date: When the fact was recorded in the system
 */
interface BiTemporalEntity {
    /**
     * The business date represents when this data was/is valid in the real world.
     */
    val businessDate: Instant
    
    /**
     * The processing date represents when this data was recorded in the system.
     */
    val processingDate: Instant
}