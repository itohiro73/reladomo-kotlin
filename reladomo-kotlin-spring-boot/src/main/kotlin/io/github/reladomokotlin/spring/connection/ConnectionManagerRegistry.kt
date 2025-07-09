package io.github.reladomokotlin.spring.connection

import javax.sql.DataSource
import org.slf4j.LoggerFactory

/**
 * Global registry for connection managers to access datasources.
 * This is used to bridge the gap between Reladomo's static factory methods
 * and Spring's dependency injection.
 */
object ConnectionManagerRegistry {
    private val logger = LoggerFactory.getLogger(ConnectionManagerRegistry::class.java)
    private val dataSources = mutableMapOf<String, DataSource>()
    
    fun registerDataSource(name: String, dataSource: DataSource) {
        dataSources[name] = dataSource
        logger.debug("Registered datasource '{}' in ConnectionManagerRegistry", name)
    }
    
    fun getDataSource(name: String): DataSource? {
        return dataSources[name]
    }
    
    fun clear() {
        dataSources.clear()
    }
}