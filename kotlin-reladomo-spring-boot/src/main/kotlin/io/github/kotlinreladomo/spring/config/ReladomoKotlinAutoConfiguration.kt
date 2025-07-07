package io.github.kotlinreladomo.spring.config

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager
import io.github.kotlinreladomo.spring.connection.H2ConnectionManager
import io.github.kotlinreladomo.spring.repository.ReladomoRepositoryFactory
import io.github.kotlinreladomo.spring.scanner.ReladomoEntityScanner
import io.github.kotlinreladomo.spring.transaction.ReladomoTransactionManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Spring Boot auto-configuration for Kotlin Reladomo.
 */
@AutoConfiguration(after = [DataSourceAutoConfiguration::class])
@ConditionalOnClass(MithraManager::class)
@EnableConfigurationProperties(ReladomoKotlinProperties::class)
@Import(
    ReladomoCacheConfiguration::class,
    ReladomoDataSourceConfiguration::class
)
class ReladomoKotlinAutoConfiguration {
    
    private val logger = LoggerFactory.getLogger(ReladomoKotlinAutoConfiguration::class.java)
    
    @Bean
    @ConditionalOnMissingBean
    fun mithraManager(
        properties: ReladomoKotlinProperties,
        applicationContext: ApplicationContext,
        entityScanner: ReladomoEntityScanner,
        connectionManagers: Map<String, SourcelessConnectionManager>
    ): MithraManager {
        // Check if XML configuration exists
        val configResource = applicationContext.getResource(properties.connectionManagerConfigFile)
        
        return if (configResource.exists()) {
            // Use existing XML configuration
            logger.info("Using XML configuration from: ${properties.connectionManagerConfigFile}")
            val configReader = MithraConfigurationReader(applicationContext)
            configReader.configureMithraManager(properties.connectionManagerConfigFile)
        } else {
            // Use entity scanning and programmatic configuration
            logger.info("No XML configuration found, using entity scanning")
            val entities = entityScanner.scanForEntities(properties.repository.basePackages)
            
            if (entities.isEmpty()) {
                logger.warn("No Reladomo entities found in packages: ${properties.repository.basePackages}")
            }
            
            val programmaticConfig = ReladomoProgrammaticConfiguration(applicationContext)
            programmaticConfig.configureMithraManager(entities, connectionManagers)
        }
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun reladomoEntityScanner(applicationContext: ApplicationContext): ReladomoEntityScanner {
        return ReladomoEntityScanner(applicationContext)
    }
    
    @Bean
    fun reladomoConnectionManagers(
        dataSourceRegistry: ReladomoDataSourceRegistry,
        applicationContext: ApplicationContext
    ): Map<String, SourcelessConnectionManager> {
        val managers = mutableMapOf<String, SourcelessConnectionManager>()
        
        // Create default H2 connection manager if no other managers are configured
        if (dataSourceRegistry.getAllDataSources().isNotEmpty()) {
            val h2Manager = H2ConnectionManager().apply {
                connectionManagerName = "default"
                databaseName = "default"
                inMemory = "true"
            }
            managers["default"] = h2Manager
        }
        
        // Add more connection managers as needed based on configuration
        
        return managers
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
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "reladomo.kotlin.repository",
        name = ["enable-query-methods"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun reladomoRepositoryFactory(beanFactory: BeanFactory): ReladomoRepositoryFactory {
        logger.info("Creating ReladomoRepositoryFactory")
        return ReladomoRepositoryFactory(beanFactory)
    }
}