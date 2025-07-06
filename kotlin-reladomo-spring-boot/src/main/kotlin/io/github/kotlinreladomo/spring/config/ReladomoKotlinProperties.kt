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
    var initializeOnStartup: Boolean = true,
    
    /**
     * Cache configuration
     */
    var cache: CacheProperties = CacheProperties(),
    
    /**
     * Multi-datasource configuration
     */
    var datasources: Map<String, DataSourceProperties> = emptyMap(),
    
    /**
     * Repository configuration
     */
    var repository: RepositoryProperties = RepositoryProperties()
) {
    data class CacheProperties(
        /**
         * Cache type: FULL, PARTIAL, NONE
         */
        var type: CacheType = CacheType.PARTIAL,
        
        /**
         * Cache timeout in seconds (for partial cache)
         */
        var timeout: Long = 3600,
        
        /**
         * Maximum cache size (for partial cache)
         */
        var maxSize: Int = 10000,
        
        /**
         * Relationship cache timeout in seconds
         */
        var relationshipTimeout: Long = 3600
    )
    
    data class DataSourceProperties(
        /**
         * JDBC URL
         */
        var url: String = "",
        
        /**
         * Database username
         */
        var username: String = "",
        
        /**
         * Database password
         */
        var password: String = "",
        
        /**
         * Driver class name
         */
        var driverClassName: String = "",
        
        /**
         * Connection pool configuration
         */
        var connectionPool: ConnectionPoolProperties = ConnectionPoolProperties()
    )
    
    data class ConnectionPoolProperties(
        /**
         * Minimum pool size
         */
        var minSize: Int = 5,
        
        /**
         * Maximum pool size
         */
        var maxSize: Int = 20,
        
        /**
         * Connection timeout in milliseconds
         */
        var connectionTimeout: Long = 30000,
        
        /**
         * Idle timeout in milliseconds
         */
        var idleTimeout: Long = 600000
    )
    
    data class RepositoryProperties(
        /**
         * Base packages to scan for repositories
         */
        var basePackages: List<String> = emptyList(),
        
        /**
         * Whether to enable query method support
         */
        var enableQueryMethods: Boolean = true,
        
        /**
         * Query method naming strategy
         */
        var namingStrategy: String = "DEFAULT"
    )
    
    enum class CacheType {
        FULL,
        PARTIAL,
        NONE
    }
}