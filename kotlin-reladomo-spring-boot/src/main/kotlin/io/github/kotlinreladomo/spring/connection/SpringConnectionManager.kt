package io.github.kotlinreladomo.spring.connection

import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager
import java.sql.Connection
import java.util.TimeZone
import javax.sql.DataSource

/**
 * Spring-based connection manager for Reladomo.
 */
class SpringConnectionManager(
    private val dataSource: DataSource,
    private val databaseTimeZoneId: String = "UTC"
) : SourcelessConnectionManager {
    
    private val databaseTimeZone: TimeZone = TimeZone.getTimeZone(databaseTimeZoneId)
    
    override fun getConnection(): Connection {
        return dataSource.connection
    }
    
    override fun getDatabaseIdentifier(): String {
        return "SpringManagedDatabase"
    }
    
    override fun getDatabaseTimeZone(): TimeZone {
        return databaseTimeZone
    }
    
    override fun getDefaultSchemaName(): String? {
        // Let Spring/database handle schema
        return null
    }
}