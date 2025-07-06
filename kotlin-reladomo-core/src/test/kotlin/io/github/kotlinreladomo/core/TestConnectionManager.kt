package io.github.kotlinreladomo.core

import com.gs.fw.common.mithra.bulkloader.BulkLoader
import com.gs.fw.common.mithra.bulkloader.BulkLoaderException
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager
import com.gs.fw.common.mithra.connectionmanager.XAConnectionManager
import com.gs.fw.common.mithra.databasetype.DatabaseType
import com.gs.fw.common.mithra.databasetype.H2DatabaseType
import java.sql.Connection
import java.util.Properties
import java.util.TimeZone
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

/**
 * Test connection manager for H2 in-memory database
 */
class TestConnectionManager private constructor() : SourcelessConnectionManager() {
    
    private val dataSource: DataSource
    private val databaseType: DatabaseType = H2DatabaseType.getInstance()
    
    init {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 10
            isAutoCommit = true
        }
        dataSource = HikariDataSource(config)
        
        setDefaultSource("test_db")
    }
    
    override fun createBulkLoader(): BulkLoader {
        throw BulkLoaderException("Bulk loading not supported for H2")
    }
    
    override fun getDatabaseType(): DatabaseType = databaseType
    
    override fun getDatabaseTimeZone(): TimeZone = TimeZone.getDefault()
    
    override fun getConnection(): Connection = dataSource.connection
    
    fun createTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                // Create Order table with bitemporal columns
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        order_id BIGINT NOT NULL,
                        customer_id BIGINT NOT NULL,
                        order_date TIMESTAMP NOT NULL,
                        amount DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        description VARCHAR(255),
                        from_z TIMESTAMP NOT NULL,
                        thru_z TIMESTAMP NOT NULL,
                        in_z TIMESTAMP NOT NULL,
                        out_z TIMESTAMP NOT NULL,
                        PRIMARY KEY (order_id, from_z, thru_z, in_z, out_z)
                    )
                """.trimIndent())
                
                // Create indexes for bitemporal queries
                stmt.execute("""
                    CREATE INDEX idx_order_temporal ON orders (order_id, thru_z, out_z)
                """.trimIndent())
            }
        }
    }
    
    fun dropTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DROP TABLE IF EXISTS orders")
            }
        }
    }
    
    companion object {
        @Volatile
        private var instance: TestConnectionManager? = null
        
        @JvmStatic
        fun getInstance(): TestConnectionManager {
            return instance ?: synchronized(this) {
                instance ?: TestConnectionManager().also { instance = it }
            }
        }
        
        @JvmStatic
        fun getInstance(properties: Properties): TestConnectionManager {
            return getInstance()
        }
    }
}