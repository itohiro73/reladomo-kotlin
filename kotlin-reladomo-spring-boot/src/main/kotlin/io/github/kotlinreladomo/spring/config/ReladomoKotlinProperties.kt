package io.github.kotlinreladomo.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Kotlin Reladomo integration.
 */
@ConfigurationProperties(prefix = "reladomo.kotlin")
data class ReladomoKotlinProperties(
    /**
     * Path to Reladomo runtime configuration XML file.
     */
    var connectionManagerConfigFile: String = "reladomo-runtime-config.xml",
    
    /**
     * Default business date provider.
     */
    var defaultBusinessDateProvider: String = "CURRENT_TIMESTAMP",
    
    /**
     * Default processing date provider.
     */
    var defaultProcessingDateProvider: String = "CURRENT_TIMESTAMP",
    
    /**
     * Default transaction timeout in seconds.
     */
    var defaultTransactionTimeout: Int = 120,
    
    /**
     * Whether to enable debug logging for Reladomo operations.
     */
    var enableDebugLogging: Boolean = false,
    
    /**
     * Database time zone.
     */
    var databaseTimeZone: String = "UTC",
    
    /**
     * Whether to initialize Reladomo on startup.
     */
    var initializeOnStartup: Boolean = true
)