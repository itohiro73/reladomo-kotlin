package io.github.kotlinreladomo.spring.connection

import com.gs.fw.common.mithra.bulkloader.BulkLoader
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager
import com.gs.fw.common.mithra.databasetype.DatabaseType
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import java.sql.Connection
import java.util.TimeZone
import javax.sql.DataSource

/**
 * Base class for Spring-aware connection managers that can resolve DataSource from Spring context.
 */
abstract class SpringAwareConnectionManager : SourcelessConnectionManager, ApplicationContextAware, InitializingBean, DisposableBean {
    
    private var applicationContext: ApplicationContext? = null
    protected var dataSource: DataSource? = null
    
    // Connection manager name for multi-datasource support
    var connectionManagerName: String = "default"
    
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }
    
    override fun afterPropertiesSet() {
        // Resolve datasource from Spring context
        dataSource = resolveDataSource()
    }
    
    override fun destroy() {
        // Cleanup if needed
    }
    
    protected open fun resolveDataSource(): DataSource {
        // First try to find a datasource with matching name
        val namedDataSource = try {
            applicationContext?.getBean("${connectionManagerName}DataSource", DataSource::class.java)
        } catch (e: Exception) {
            null
        }
        
        // Fall back to default datasource
        return namedDataSource 
            ?: applicationContext?.getBean(DataSource::class.java)
            ?: throw IllegalStateException("No DataSource found in Spring context for connection manager: $connectionManagerName")
    }
    
    override fun createBulkLoader(): BulkLoader? = null
    
    override fun getDatabaseTimeZone(): TimeZone = TimeZone.getDefault()
    
    override fun getConnection(): Connection {
        return dataSource?.connection
            ?: throw IllegalStateException("DataSource not initialized for connection manager: $connectionManagerName")
    }
    
    abstract override fun getDatabaseType(): DatabaseType
    abstract override fun getDatabaseIdentifier(): String
}