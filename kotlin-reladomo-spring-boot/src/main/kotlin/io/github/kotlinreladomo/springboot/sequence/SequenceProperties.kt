package io.github.kotlinreladomo.springboot.sequence

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for sequence generation.
 */
@ConfigurationProperties(prefix = "reladomo.sequence")
data class SequenceProperties(
    /**
     * Whether to enable sequence generation support.
     */
    var enabled: Boolean = false,
    
    /**
     * The type of sequence generator to use.
     */
    var type: SequenceType = SequenceType.IN_MEMORY,
    
    /**
     * The default starting value for new sequences.
     */
    var defaultStartValue: Long = 1000L,
    
    /**
     * The increment value for sequences.
     */
    var incrementBy: Int = 1,
    
    /**
     * Configuration for Reladomo-based sequence generation.
     */
    var reladomo: ReladomoSequenceProperties = ReladomoSequenceProperties()
) {
    
    enum class SequenceType {
        /**
         * Use in-memory sequence generation (not persistent).
         */
        IN_MEMORY,
        
        /**
         * Use Reladomo ObjectSequence table for persistence.
         */
        RELADOMO,
        
        /**
         * Use a custom sequence generator bean.
         */
        CUSTOM
    }
    
    data class ReladomoSequenceProperties(
        /**
         * The name of the ObjectSequence XML file to use.
         * If not specified, a default one will be created.
         */
        var xmlFile: String? = null,
        
        /**
         * Whether to auto-create the ObjectSequence table.
         */
        var autoCreateTable: Boolean = true
    )
}