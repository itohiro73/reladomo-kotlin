package io.github.reladomokotlin.spring.connection

import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.H2DatabaseType
import java.util.Properties

/**
 * H2 database connection manager for Reladomo.
 * Used primarily for testing and development.
 * 
 * This connection manager automatically resolves the DataSource from Spring context.
 */
class H2ConnectionManager : SpringAwareConnectionManager() {
    
    // Additional H2-specific properties
    var databaseName: String? = null
    var inMemory: String? = null
    
    override fun getDatabaseType(): DatabaseType = H2DatabaseType.getInstance()
    
    override fun getDatabaseIdentifier(): String = "H2"
    
    companion object {
        @JvmStatic
        fun getInstance(properties: Properties): H2ConnectionManager {
            val connectionManager = H2ConnectionManager()
            // Apply properties if needed
            properties.getProperty("databaseName")?.let { connectionManager.databaseName = it }
            properties.getProperty("inMemory")?.let { connectionManager.inMemory = it }
            return connectionManager
        }
    }
}