package io.github.kotlinreladomo.sample.config

import com.gs.fw.common.mithra.MithraManager
import com.gs.fw.common.mithra.MithraManagerProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@TestConfiguration
@Import(TestDataInitializer::class)
class TestReladomoConfiguration {
    
    @Bean
    @Primary
    fun testMithraManager(dataSource: DataSource): MithraManager {
        // First, ensure the schema is created
        createSchema(dataSource)
        
        // Set the DataSource in TestConnectionManager before initializing Reladomo
        TestConnectionManager.setDataSource(dataSource)
        
        // Then initialize MithraManager
        val mithraManager = MithraManagerProvider.getMithraManager()
        
        // Use the XML configuration which properly sets up everything
        val configResource = ClassPathResource("test-reladomo-runtime-config.xml")
        mithraManager.readConfiguration(configResource.inputStream)
        
        return mithraManager
    }
    
    private fun createSchema(dataSource: DataSource) {
        val jdbcTemplate = JdbcTemplate(dataSource)
        val schemaResource = ClassPathResource("schema.sql")
        
        if (schemaResource.exists()) {
            val sql = schemaResource.inputStream.bufferedReader().use { it.readText() }
            // Execute each statement separately
            sql.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { statement ->
                    try {
                        jdbcTemplate.execute(statement)
                    } catch (e: Exception) {
                        // Ignore errors (e.g., table already exists)
                        println("Schema creation warning: ${e.message}")
                    }
                }
        }
    }
}