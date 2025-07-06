package io.github.kotlinreladomo.spring.config

import com.gs.fw.common.mithra.MithraObjectPortal
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Reladomo cache strategies.
 */
@Configuration
@ConditionalOnProperty(prefix = "reladomo.kotlin.cache", name = ["type"])
class ReladomoCacheConfiguration {
    
    private val logger = LoggerFactory.getLogger(ReladomoCacheConfiguration::class.java)
    
    @Bean
    fun cacheConfigurer(properties: ReladomoKotlinProperties): CacheConfigurer {
        return CacheConfigurer(properties.cache)
    }
    
    /**
     * Configures cache settings for Reladomo entities.
     */
    class CacheConfigurer(private val cacheProperties: ReladomoKotlinProperties.CacheProperties) {
        
        private val logger = LoggerFactory.getLogger(CacheConfigurer::class.java)
        
        fun configureCacheForPortal(portal: MithraObjectPortal) {
            logger.info("Configuring ${cacheProperties.type} cache for ${portal.businessClassName}")
            
            when (cacheProperties.type) {
                ReladomoKotlinProperties.CacheType.FULL -> {
                    configureFullCache(portal)
                }
                ReladomoKotlinProperties.CacheType.PARTIAL -> {
                    configurePartialCache(portal)
                }
                ReladomoKotlinProperties.CacheType.NONE -> {
                    configureNoCache(portal)
                }
            }
        }
        
        private fun configureFullCache(portal: MithraObjectPortal) {
            // Full cache configuration
            portal.reloadCache()
            logger.debug("Full cache configured for ${portal.businessClassName}")
        }
        
        private fun configurePartialCache(portal: MithraObjectPortal) {
            // Partial cache configuration with timeout
            // Note: Actual cache configuration would be done via Reladomo runtime config
            portal.reloadCache()
            logger.debug("Partial cache configured for ${portal.businessClassName} with timeout ${cacheProperties.timeout}s")
        }
        
        private fun configureNoCache(portal: MithraObjectPortal) {
            // No cache configuration
            // Note: No-cache would be configured via Reladomo runtime config
            logger.debug("No cache configured for ${portal.businessClassName}")
        }
    }
}