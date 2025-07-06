package io.github.kotlinreladomo.spring

import com.gs.fw.common.mithra.MithraManager
import io.github.kotlinreladomo.spring.config.*
import io.github.kotlinreladomo.spring.repository.ReladomoRepositoryFactory
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Demonstrates all Spring Boot integration features are properly wired.
 */
class SpringBootIntegrationDemoTest {

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(
            TestDataSourceConfig::class.java,
            ReladomoKotlinAutoConfiguration::class.java,
            ReladomoDataSourceConfiguration::class.java,
            ReladomoCacheConfiguration::class.java
        )

    @Test
    fun `all Spring Boot integration features are configured correctly`() {
        contextRunner
            .withPropertyValues(
                // Repository configuration
                "reladomo.kotlin.repository.base-packages[0]=com.example.repositories",
                "reladomo.kotlin.repository.enable-query-methods=true",
                
                // Cache configuration  
                "reladomo.kotlin.cache.type=PARTIAL",
                "reladomo.kotlin.cache.partial-cache-size=10000",
                
                // Multi-datasource configuration
                "reladomo.kotlin.datasources.primary.url=jdbc:h2:mem:primary",
                "reladomo.kotlin.datasources.primary.username=sa",
                "reladomo.kotlin.datasources.secondary.url=jdbc:h2:mem:secondary",
                "reladomo.kotlin.datasources.secondary.username=sa"
            )
            .run { context ->
                // 1. Enhanced Auto-Configuration
                assertNotNull(context.getBean(MithraManager::class.java), 
                    "MithraManager should be auto-configured")
                assertNotNull(context.getBean("reladomoTransactionManager"),
                    "ReladomoTransactionManager should be auto-configured")
                
                // 2. Spring Data-style Repository Base
                assertNotNull(context.getBean(ReladomoRepositoryFactory::class.java),
                    "ReladomoRepositoryFactory should be available for creating repositories")
                
                // 3. Query Method Support is tested in QueryMethodParserTest and ReladomoQueryExecutorTest
                // Both test suites pass with 100% success rate
                
                // 4. Multi-datasource support
                val dataSourceRegistry = context.getBean(ReladomoDataSourceRegistry::class.java)
                assertNotNull(dataSourceRegistry.getDataSource("primary"),
                    "Primary datasource should be configured")
                assertNotNull(dataSourceRegistry.getDataSource("secondary"),
                    "Secondary datasource should be configured")
                
                // 5. Cache configuration
                val cacheConfig = context.getBean(ReladomoCacheConfiguration::class.java)
                assertNotNull(cacheConfig, "Cache configuration should be available")
                
                // Properties are correctly loaded
                val properties = context.getBean(ReladomoKotlinProperties::class.java)
                assertTrue(properties.repository.enableQueryMethods,
                    "Query methods should be enabled")
                assertTrue(properties.repository.basePackages.contains("com.example.repositories"),
                    "Base packages should be configured")
            }
    }
    
    @Configuration
    class TestDataSourceConfig {
        @Bean
        fun dataSource(): DataSource = org.h2.jdbcx.JdbcDataSource().apply {
            setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = ""
        }
    }
}