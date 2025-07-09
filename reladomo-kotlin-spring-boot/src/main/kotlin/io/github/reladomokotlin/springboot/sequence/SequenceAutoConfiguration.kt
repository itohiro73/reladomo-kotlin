package io.github.reladomokotlin.springboot.sequence

import io.github.reladomokotlin.sequence.InMemorySequenceGenerator
import io.github.reladomokotlin.sequence.ReladomoSequenceGenerator
import io.github.reladomokotlin.sequence.SequenceGenerator
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct
import javax.sql.DataSource

/**
 * Spring Boot auto-configuration for sequence generation support.
 */
@Configuration
@ConditionalOnClass(SequenceGenerator::class)
@ConditionalOnProperty(prefix = "reladomo.sequence", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(SequenceProperties::class)
class SequenceAutoConfiguration(
    private val dataSource: DataSource?
) {
    
    private val logger = LoggerFactory.getLogger(SequenceAutoConfiguration::class.java)
    
    @PostConstruct
    fun initializeSequenceFactory() {
        if (dataSource != null) {
            logger.debug("Initializing GenericSequenceObjectFactory with DataSource")
            GenericSequenceObjectFactory.setDataSource(dataSource)
        } else {
            logger.warn("No DataSource available for GenericSequenceObjectFactory initialization")
        }
    }
    
    @Bean
    @ConditionalOnMissingBean(SequenceGenerator::class)
    @ConditionalOnProperty(prefix = "reladomo.sequence", name = ["type"], havingValue = "IN_MEMORY", matchIfMissing = true)
    fun inMemorySequenceGenerator(properties: SequenceProperties): SequenceGenerator {
        logger.info("Configuring in-memory sequence generator with start value: ${properties.defaultStartValue}")
        return InMemorySequenceGenerator(
            defaultStartValue = properties.defaultStartValue,
            incrementBy = properties.incrementBy
        )
    }
    
    @Bean
    @ConditionalOnMissingBean(SequenceGenerator::class)
    @ConditionalOnProperty(prefix = "reladomo.sequence", name = ["type"], havingValue = "RELADOMO")
    fun reladomoSequenceGenerator(properties: SequenceProperties): SequenceGenerator {
        logger.info("Configuring Reladomo sequence generator with start value: ${properties.defaultStartValue}")
        
        // TODO: Handle auto-creation of ObjectSequence XML and table if needed
        if (properties.reladomo.autoCreateTable) {
            logger.debug("Auto-creation of ObjectSequence table is enabled")
            // This will be implemented when we have the full Reladomo integration
        }
        
        return ReladomoSequenceGenerator(
            defaultStartValue = properties.defaultStartValue,
            incrementBy = properties.incrementBy
        )
    }
    
    @Bean
    fun sequenceGeneratorBeanPostProcessor(
        sequenceGenerator: SequenceGenerator?
    ): SequenceGeneratorBeanPostProcessor {
        return SequenceGeneratorBeanPostProcessor(sequenceGenerator)
    }
}