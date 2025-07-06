package io.github.kotlinreladomo.spring.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Multi-datasource configuration for Reladomo.
 */
@Configuration
@ConditionalOnProperty(prefix = "reladomo.kotlin.datasources", name = ["enabled"], matchIfMissing = true)
class ReladomoDataSourceConfiguration {
    
    private val logger = LoggerFactory.getLogger(ReladomoDataSourceConfiguration::class.java)
    
    @Bean
    fun reladomoDataSources(properties: ReladomoKotlinProperties): ReladomoDataSourceRegistry {
        val registry = ReladomoDataSourceRegistry()
        
        properties.datasources.forEach { (name, config) ->
            logger.info("Configuring datasource: $name")
            val dataSource = createDataSource(name, config)
            registry.register(name, dataSource)
        }
        
        return registry
    }
    
    private fun createDataSource(name: String, config: ReladomoKotlinProperties.DataSourceProperties): DataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.username
            password = config.password
            // Only set driver class name if it's not empty
            if (config.driverClassName.isNotEmpty()) {
                driverClassName = config.driverClassName
            }
            
            // Connection pool settings
            minimumIdle = config.connectionPool.minSize
            maximumPoolSize = config.connectionPool.maxSize
            connectionTimeout = config.connectionPool.connectionTimeout
            idleTimeout = config.connectionPool.idleTimeout
            
            // Reladomo-specific settings
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            
            // Pool name
            poolName = "Reladomo-$name"
        }
        
        return HikariDataSource(hikariConfig).also {
            logger.debug("Created HikariCP datasource '$name' with pool size ${config.connectionPool.minSize}-${config.connectionPool.maxSize}")
        }
    }
}

/**
 * Registry for managing multiple datasources.
 */
class ReladomoDataSourceRegistry {
    private val dataSources = mutableMapOf<String, DataSource>()
    private val logger = LoggerFactory.getLogger(ReladomoDataSourceRegistry::class.java)
    
    fun register(name: String, dataSource: DataSource) {
        dataSources[name] = dataSource
        logger.debug("Registered datasource: $name")
    }
    
    fun getDataSource(name: String): DataSource? = dataSources[name]
    
    fun getAllDataSources(): Map<String, DataSource> = dataSources.toMap()
    
    fun getPrimaryDataSource(): DataSource? = dataSources["primary"] ?: dataSources.values.firstOrNull()
}