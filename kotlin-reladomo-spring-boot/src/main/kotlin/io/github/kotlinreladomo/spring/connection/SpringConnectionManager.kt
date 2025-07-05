package io.github.kotlinreladomo.spring.connection

import com.gs.fw.common.mithra.bulkloader.BulkLoader
import com.gs.fw.common.mithra.bulkloader.BulkLoaderException
import com.gs.fw.common.mithra.connectionmanager.ObjectSourceConnectionManager
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager
import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.PostgresDatabaseType
import org.springframework.jdbc.datasource.DataSourceUtils
import java.sql.Connection
import java.util.TimeZone
import javax.sql.DataSource

/**
 * Spring-aware connection manager for Reladomo.
 * Integrates with Spring's transaction management.
 * Implements ObjectSourceConnectionManager to handle source-based connections.
 */
class SpringConnectionManager : ObjectSourceConnectionManager() {
    
    companion object {
        @Volatile
        private var springDataSource: DataSource? = null
        
        fun setDataSource(dataSource: DataSource) {
            springDataSource = dataSource
        }
    }
    
    private val databaseType: DatabaseType = PostgresDatabaseType.getInstance()
    
    override fun getConnection(source: Any?): Connection {
        val ds = springDataSource ?: throw IllegalStateException("DataSource not configured")
        return DataSourceUtils.getConnection(ds)
    }
    
    override fun getDatabaseType(source: Any?): DatabaseType {
        return databaseType
    }
    
    override fun getDatabaseTimeZone(source: Any?): TimeZone {
        return TimeZone.getTimeZone("UTC")
    }
    
    override fun createBulkLoader(source: Any?): BulkLoader? {
        throw BulkLoaderException("Bulk loading not implemented yet")
    }
    
    override fun getDatabaseIdentifier(source: Any?): String {
        return "POSTGRESQL"
    }
}