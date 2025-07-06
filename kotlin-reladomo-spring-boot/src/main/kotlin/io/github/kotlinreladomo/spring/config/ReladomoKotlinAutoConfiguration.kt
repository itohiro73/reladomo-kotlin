package io.github.kotlinreladomo.spring.config

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import io.github.kotlinreladomo.spring.connection.H2ConnectionManager
import io.github.kotlinreladomo.spring.repository.ReladomoRepositoryFactory
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
        applicationContext: ApplicationContext
    ): MithraManager {
        logger.info("Initializing MithraManager with configuration from: ${properties.connectionManagerConfigFile}")
        
        // Use our custom configuration reader that handles Spring integration
        val configReader = MithraConfigurationReader(applicationContext)
        return configReader.configureMithraManager(properties.connectionManagerConfigFile)
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