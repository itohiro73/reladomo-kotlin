package io.github.kotlinreladomo.spring.connection

import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.OracleDatabaseType

/**
 * Oracle database connection manager for Reladomo.
 * 
 * This connection manager automatically resolves the DataSource from Spring context.
 */
class OracleConnectionManager : SpringAwareConnectionManager() {
    
    // Oracle-specific properties
    var schemaName: String? = null
    
    override fun getDatabaseType(): DatabaseType = OracleDatabaseType.getInstance()
    
    override fun getDatabaseIdentifier(): String = "Oracle"
}