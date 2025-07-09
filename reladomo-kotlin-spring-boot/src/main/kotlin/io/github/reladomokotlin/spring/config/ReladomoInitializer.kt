package io.github.reladomokotlin.spring.config

import com.gs.fw.common.mithra.MithraManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import java.util.TimeZone

/**
 * Initializes Reladomo on application startup.
 */
class ReladomoInitializer(
    private val mithraManager: MithraManager,
    private val properties: ReladomoKotlinProperties
) : InitializingBean {
    
    private val logger = LoggerFactory.getLogger(ReladomoInitializer::class.java)
    
    override fun afterPropertiesSet() {
        if (properties.initializeOnStartup) {
            initializeReladomo()
        }
    }
    
    private fun initializeReladomo() {
        logger.info("Initializing Reladomo with database timezone: ${properties.databaseTimeZone}")
        
        // Set database timezone
        TimeZone.setDefault(TimeZone.getTimeZone(properties.databaseTimeZone))
        
        // Enable debug logging if configured
        if (properties.enableDebugLogging) {
            System.setProperty("mithra.debug.logging", "true")
        }
        
        logger.info("Reladomo initialization completed")
    }
}