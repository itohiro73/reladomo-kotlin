package io.github.kotlinreladomo.spring.connection

import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.H2DatabaseType

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
}