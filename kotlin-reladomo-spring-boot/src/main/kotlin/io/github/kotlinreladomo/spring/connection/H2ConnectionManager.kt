package io.github.kotlinreladomo.spring.connection

import com.gs.fw.common.mithra.bulkloader.BulkLoader
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager
import com.gs.fw.common.mithra.connectionmanager.XAConnectionManager
import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.H2DatabaseType
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.TimeZone
import javax.sql.DataSource

/**
 * H2 database connection manager for Reladomo.
 * Used primarily for testing and development.
 */
class H2ConnectionManager : SourcelessConnectionManager {
    
    private val logger = LoggerFactory.getLogger(H2ConnectionManager::class.java)
    private lateinit var dataSource: DataSource
    private val databaseType = H2DatabaseType.getInstance()
    
    fun setDataSource(dataSource: DataSource) {
        this.dataSource = dataSource
    }
    
    override fun createBulkLoader(): BulkLoader? {
        // H2 doesn't support bulk loading
        return null
    }
    
    override fun getDatabaseIdentifier(): String {
        return "H2"
    }
    
    override fun getDatabaseType(): DatabaseType {
        return databaseType
    }
    
    override fun getDatabaseTimeZone(): TimeZone {
        return TimeZone.getDefault()
    }
    
    override fun getConnection(): Connection {
        return dataSource.connection
    }
}