package io.github.reladomokotlin.core

import java.time.Instant

/**
 * Base interface for all unitemporal entities in the Kotlin Reladomo wrapper.
 *
 * Unitemporal entities track data along one time dimension:
 * - Processing Date: When the fact was recorded/changed in the system
 *
 * This is ideal for tracking the history of master data changes.
 * For example, a Product entity can track when its name or description was changed.
 */
interface UniTemporalEntity {
    /**
     * The processing date represents when this data was recorded in the system.
     * This is typically set automatically by Reladomo when the record is inserted.
     */
    val processingDate: Instant
}
