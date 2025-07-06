package io.github.kotlinreladomo.spring.config

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import io.github.kotlinreladomo.spring.connection.SpringAwareConnectionManager
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.io.ClassPathResource
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Custom configuration reader that integrates Reladomo with Spring context.
 */
class MithraConfigurationReader(
    private val applicationContext: ApplicationContext
) {
    private val logger = LoggerFactory.getLogger(MithraConfigurationReader::class.java)
    
    fun configureMithraManager(configFile: String): MithraManager {
        val manager = MithraManagerProvider.getMithraManager()
        
        try {
            val configResource = ClassPathResource(configFile)
            if (!configResource.exists()) {
                logger.warn("Reladomo configuration file not found: $configFile")
                return manager
            }
            
            // Parse the XML configuration
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = documentBuilder.parse(configResource.inputStream)
            
            // Process connection managers
            val connectionManagers = document.getElementsByTagName("ConnectionManager")
            for (i in 0 until connectionManagers.length) {
                val element = connectionManagers.item(i) as Element
                val className = element.getAttribute("className")
                
                try {
                    // Create connection manager instance
                    val connectionManager = Class.forName(className).getDeclaredConstructor().newInstance()
                    
                    // If it's Spring-aware, inject the application context
                    if (connectionManager is SpringAwareConnectionManager) {
                        connectionManager.setApplicationContext(applicationContext)
                        
                        // Set properties from XML
                        val properties = element.getElementsByTagName("Property")
                        for (j in 0 until properties.length) {
                            val prop = properties.item(j) as Element
                            val name = prop.getAttribute("name")
                            val value = prop.getAttribute("value")
                            
                            when (name) {
                                "connectionManagerName" -> connectionManager.connectionManagerName = value
                                else -> {
                                    // Set other properties via reflection
                                    try {
                                        val field = connectionManager.javaClass.getDeclaredField(name)
                                        field.isAccessible = true
                                        field.set(connectionManager, value)
                                    } catch (e: Exception) {
                                        logger.debug("Could not set property $name on $className", e)
                                    }
                                }
                            }
                        }
                        
                        // Initialize the connection manager
                        connectionManager.afterPropertiesSet()
                        logger.info("Initialized Spring-aware connection manager: $className")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to initialize connection manager: $className", e)
                }
            }
            
            // Now load the configuration normally
            manager.readConfiguration(configResource.inputStream)
            
        } catch (e: Exception) {
            logger.error("Failed to load Reladomo configuration from $configFile", e)
            logger.info("Reladomo will continue without XML configuration. " +
                    "For production use, ensure proper Reladomo XML configuration is provided.")
        }
        
        return manager
    }
}