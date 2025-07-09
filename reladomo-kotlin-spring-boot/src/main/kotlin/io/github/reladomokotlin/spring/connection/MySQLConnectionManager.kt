package io.github.reladomokotlin.spring.connection

import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.MariaDatabaseType

/**
 * MySQL database connection manager for Reladomo.
 * 
 * This connection manager automatically resolves the DataSource from Spring context.
 */
class MySQLConnectionManager : SpringAwareConnectionManager() {
    
    // MySQL-specific properties
    var databaseName: String? = null
    
    override fun getDatabaseType(): DatabaseType = MariaDatabaseType.getInstance()
    
    override fun getDatabaseIdentifier(): String = "MySQL"
}