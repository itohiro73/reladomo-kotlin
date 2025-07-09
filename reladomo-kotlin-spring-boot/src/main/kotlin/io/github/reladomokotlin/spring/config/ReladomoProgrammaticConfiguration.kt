package io.github.reladomokotlin.spring.config

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager
import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties.CacheType
import io.github.reladomokotlin.spring.scanner.EntityMetadata
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import java.io.StringReader
import java.util.Properties

/**
 * Programmatically configures Reladomo based on discovered entities.
 */
class ReladomoProgrammaticConfiguration(
    private val applicationContext: ApplicationContext
) {
    private val logger = LoggerFactory.getLogger(ReladomoProgrammaticConfiguration::class.java)
    
    /**
     * Configures MithraManager programmatically based on entity metadata.
     */
    fun configureMithraManager(
        entities: List<EntityMetadata>,
        connectionManagers: Map<String, SourcelessConnectionManager>
    ): MithraManager {
        logger.info("Configuring MithraManager programmatically with {} entities", entities.size)
        
        // Generate runtime configuration XML
        val runtimeConfigXml = generateRuntimeConfigXml(entities, connectionManagers)
        logger.debug("Generated runtime configuration:\n{}", runtimeConfigXml)
        
        // Generate class list XML
        val classListXml = generateClassListXml(entities)
        logger.debug("Generated class list:\n{}", classListXml)
        
        // Initialize MithraManager with generated configuration
        try {
            // Create input streams for the configuration
            val runtimeStream = java.io.ByteArrayInputStream(runtimeConfigXml.toByteArray())
            
            // Use MithraManager to load the configuration
            val mithraManager = MithraManagerProvider.getMithraManager()
            mithraManager.readConfiguration(runtimeStream)
            
            // Set transaction timeout
            mithraManager.setTransactionTimeout(120)
            
            // Log successful initialization
            logger.info("MithraManager configured programmatically with {} entities", entities.size)
            
            return mithraManager
        } catch (e: Exception) {
            logger.error("Failed to configure MithraManager programmatically", e)
            throw RuntimeException("Failed to configure MithraManager", e)
        }
    }
    
    private fun generateRuntimeConfigXml(
        entities: List<EntityMetadata>,
        connectionManagers: Map<String, SourcelessConnectionManager>
    ): String {
        val xml = StringBuilder()
        xml.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        xml.appendLine("<MithraRuntime>")
        
        // Group entities by connection manager
        val entitiesByConnectionManager = entities.groupBy { it.connectionManager }
        
        for ((connectionManagerName, managerEntities) in entitiesByConnectionManager) {
            val connectionManager = connectionManagers[connectionManagerName]
                ?: connectionManagers["default"]
                ?: throw IllegalStateException("No connection manager found for: $connectionManagerName")
            
            xml.appendLine("""    <ConnectionManager className="${connectionManager::class.qualifiedName}">""")
            xml.appendLine("""        <Property name="connectionManagerName" value="$connectionManagerName"/>""")
            
            // Add connection manager specific properties
            when (connectionManager) {
                is io.github.reladomokotlin.spring.connection.H2ConnectionManager -> {
                    xml.appendLine("""        <Property name="databaseName" value="${connectionManager.databaseName ?: "test"}"/>""")
                    xml.appendLine("""        <Property name="inMemory" value="${connectionManager.inMemory ?: "true"}"/>""")
                }
            }
            
            // Add entity configurations
            for (entity in managerEntities) {
                // Map model class to domain class
                val domainClassName = entity.className.replace(".model.", ".domain.").removeSuffix("Kt")
                xml.appendLine("""        <MithraObjectConfiguration className="$domainClassName" cacheType="${entity.cacheType.name.lowercase()}"/>""")
            }
            
            xml.appendLine("    </ConnectionManager>")
        }
        
        xml.appendLine("</MithraRuntime>")
        return xml.toString()
    }
    
    private fun generateClassListXml(entities: List<EntityMetadata>): String {
        val xml = StringBuilder()
        xml.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        xml.appendLine("<Mithra>")
        
        for (entity in entities) {
            xml.appendLine("""    <MithraObjectResource name="${entity.simpleName}"/>""")
        }
        
        xml.appendLine("</Mithra>")
        return xml.toString()
    }
}

/**
 * Builder for creating Reladomo configuration programmatically.
 */
class ReladomoConfigurationBuilder {
    private val connectionManagers = mutableMapOf<String, ConnectionManagerConfig>()
    private val entities = mutableListOf<EntityConfig>()
    private var scanPackages = mutableListOf<String>()
    
    /**
     * Configures a connection manager.
     */
    fun connectionManager(name: String = "default", configure: ConnectionManagerConfig.() -> Unit): ReladomoConfigurationBuilder {
        val config = ConnectionManagerConfig(name).apply(configure)
        connectionManagers[name] = config
        return this
    }
    
    /**
     * Configures an entity.
     */
    fun entity(className: String, configure: EntityConfig.() -> Unit): ReladomoConfigurationBuilder {
        val config = EntityConfig(className).apply(configure)
        entities.add(config)
        return this
    }
    
    /**
     * Adds packages to scan for entities.
     */
    fun scanPackages(vararg packages: String): ReladomoConfigurationBuilder {
        scanPackages.addAll(packages)
        return this
    }
    
    /**
     * Builds the configuration.
     */
    fun build(): ReladomoConfiguration {
        return ReladomoConfiguration(
            connectionManagers = connectionManagers.toMap(),
            entities = entities.toList(),
            scanPackages = scanPackages.toList()
        )
    }
}

/**
 * Connection manager configuration.
 */
data class ConnectionManagerConfig(
    val name: String,
    var className: String = "io.github.reladomokotlin.spring.connection.H2ConnectionManager",
    var properties: MutableMap<String, String> = mutableMapOf()
) {
    fun property(key: String, value: String) {
        properties[key] = value
    }
}

/**
 * Entity configuration.
 */
data class EntityConfig(
    val className: String,
    var tableName: String? = null,
    var connectionManager: String = "default",
    var cacheType: CacheType = CacheType.PARTIAL
)

/**
 * Complete Reladomo configuration.
 */
data class ReladomoConfiguration(
    val connectionManagers: Map<String, ConnectionManagerConfig>,
    val entities: List<EntityConfig>,
    val scanPackages: List<String>
)