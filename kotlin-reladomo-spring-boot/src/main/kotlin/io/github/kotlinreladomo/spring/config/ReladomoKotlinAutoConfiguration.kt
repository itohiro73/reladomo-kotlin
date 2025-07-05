package io.github.kotlinreladomo.spring.config

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import io.github.kotlinreladomo.spring.connection.SpringConnectionManager
import io.github.kotlinreladomo.spring.transaction.ReladomoTransactionManager
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Spring Boot auto-configuration for Kotlin Reladomo.
 */
@AutoConfiguration(after = [DataSourceAutoConfiguration::class])
@ConditionalOnClass(MithraManager::class)
@EnableConfigurationProperties(ReladomoKotlinProperties::class)
class ReladomoKotlinAutoConfiguration {
    
    private val logger = LoggerFactory.getLogger(ReladomoKotlinAutoConfiguration::class.java)
    
    @Bean
    @ConditionalOnMissingBean
    fun mithraManager(
        properties: ReladomoKotlinProperties,
        dataSource: DataSource
    ): MithraManager {
        logger.info("Initializing MithraManager with configuration from: ${properties.connectionManagerConfigFile}")
        
        val manager = MithraManagerProvider.getMithraManager()
        manager.setTransactionTimeout(properties.defaultTransactionTimeout)
        
        // Set up connection manager
        val connectionManager = SpringConnectionManager(dataSource, properties.databaseTimeZone)
        manager.setDefaultConnectionManager(connectionManager)
        
        // Load configuration if available
        try {
            val configResource = ClassPathResource(properties.connectionManagerConfigFile)
            if (configResource.exists()) {
                logger.info("Loading Reladomo configuration from: ${configResource.url}")
                manager.readConfiguration(configResource.inputStream)
            } else {
                logger.warn("Reladomo configuration file not found: ${properties.connectionManagerConfigFile}")
            }
        } catch (e: Exception) {
            logger.error("Failed to load Reladomo configuration", e)
            throw e
        }
        
        return manager
    }
    
    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager::class)
    @ConditionalOnProperty(
        prefix = "reladomo.kotlin",
        name = ["transaction-management-enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun reladomoTransactionManager(mithraManager: MithraManager): PlatformTransactionManager {
        logger.info("Creating ReladomoTransactionManager")
        return ReladomoTransactionManager(mithraManager)
    }
    
    @Bean
    fun reladomoInitializer(
        mithraManager: MithraManager,
        properties: ReladomoKotlinProperties
    ): ReladomoInitializer {
        return ReladomoInitializer(mithraManager, properties)
    }
}