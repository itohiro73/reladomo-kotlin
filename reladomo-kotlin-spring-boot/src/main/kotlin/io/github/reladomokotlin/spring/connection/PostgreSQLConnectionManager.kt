package io.github.reladomokotlin.spring.connection

import com.gs.fw.common.mithra.bulkloader.BulkLoader
import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.PostgresDatabaseType

/**
 * PostgreSQL database connection manager for Reladomo.
 * 
 * This connection manager automatically resolves the DataSource from Spring context.
 */
class PostgreSQLConnectionManager : SpringAwareConnectionManager() {
    
    // PostgreSQL-specific properties
    var databaseName: String? = null
    var schemaName: String? = null
    
    override fun getDatabaseType(): DatabaseType = PostgresDatabaseType.getInstance()
    
    override fun getDatabaseIdentifier(): String = "PostgreSQL"
    
    override fun createBulkLoader(): BulkLoader? {
        // PostgreSQL bulk loader is not available via the simple API
        // Would need to implement custom bulk loader for production use
        return null
    }
}