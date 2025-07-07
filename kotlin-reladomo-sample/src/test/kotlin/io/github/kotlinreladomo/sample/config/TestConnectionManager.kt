package io.github.kotlinreladomo.sample.config

import com.gs.fw.common.mithra.bulkloader.BulkLoader
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager
import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.H2DatabaseType
import io.github.kotlinreladomo.springboot.sequence.GenericSequenceObjectFactory
import java.sql.Connection
import java.util.Properties
import java.util.TimeZone
import javax.sql.DataSource

/**
 * Test connection manager for Reladomo that works with H2 in-memory database.
 * Provides the required static getInstance() method for Reladomo XML configuration.
 */
class TestConnectionManager : SourcelessConnectionManager {
    
    constructor() : super()
    
    companion object {
        @Volatile
        private var staticDataSource: DataSource? = null
        
        @JvmStatic
        fun getInstance(): TestConnectionManager {
            return TestConnectionManager()
        }
        
        @JvmStatic
        fun getInstance(properties: Properties): TestConnectionManager {
            return TestConnectionManager()
        }
        
        @JvmStatic
        fun setDataSource(dataSource: DataSource) {
            staticDataSource = dataSource
            // Also set it for the GenericSequenceObjectFactory
            GenericSequenceObjectFactory.setDataSource(dataSource)
        }
    }
    
    override fun getConnection(): Connection {
        // Use the statically set datasource
        val ds = staticDataSource
        return ds?.connection ?: throw IllegalStateException("No DataSource available - ensure TestConnectionManager.setDataSource() is called during test initialization")
    }
    
    override fun getDatabaseType(): DatabaseType {
        return H2DatabaseType.getInstance()
    }
    
    override fun getDatabaseIdentifier(): String {
        return "H2"
    }
    
    override fun createBulkLoader(): BulkLoader? {
        return null // No bulk loader needed for tests
    }
    
    override fun getDatabaseTimeZone(): TimeZone {
        return TimeZone.getDefault()
    }
}