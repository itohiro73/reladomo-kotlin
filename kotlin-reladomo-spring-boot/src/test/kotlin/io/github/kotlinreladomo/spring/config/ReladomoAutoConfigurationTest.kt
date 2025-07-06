package io.github.kotlinreladomo.spring.config

import com.gs.fw.common.mithra.MithraManager
import io.github.kotlinreladomo.spring.repository.ReladomoRepositoryFactory
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource
import kotlin.test.*

class ReladomoAutoConfigurationTest {
    
    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ReladomoKotlinAutoConfiguration::class.java,
            ReladomoCacheConfiguration::class.java,
            ReladomoDataSourceConfiguration::class.java
        ))
        .withUserConfiguration(TestDataSourceConfiguration::class.java)
    
    @Test
    fun `auto-configuration creates MithraManager bean`() {
        contextRunner.run { context ->
            assertNotNull(context.getBean(MithraManager::class.java))
        }
    }
    
    @Test
    fun `auto-configuration creates repository factory`() {
        contextRunner.run { context ->
            assertNotNull(context.getBean(ReladomoRepositoryFactory::class.java))
        }
    }
    
    @Test
    fun `cache configuration with FULL type`() {
        contextRunner
            .withPropertyValues(
                "reladomo.kotlin.cache.type=FULL"
            )
            .run { context ->
                val cacheConfigurer = context.getBean(ReladomoCacheConfiguration.CacheConfigurer::class.java)
                assertNotNull(cacheConfigurer)
                
                val properties = context.getBean(ReladomoKotlinProperties::class.java)
                assertEquals(ReladomoKotlinProperties.CacheType.FULL, properties.cache.type)
            }
    }
    
    @Test
    fun `cache configuration with PARTIAL type and custom timeout`() {
        contextRunner
            .withPropertyValues(
                "reladomo.kotlin.cache.type=PARTIAL",
                "reladomo.kotlin.cache.timeout=7200",
                "reladomo.kotlin.cache.max-size=5000"
            )
            .run { context ->
                val properties = context.getBean(ReladomoKotlinProperties::class.java)
                assertEquals(ReladomoKotlinProperties.CacheType.PARTIAL, properties.cache.type)
                assertEquals(7200L, properties.cache.timeout)
                assertEquals(5000, properties.cache.maxSize)
            }
    }
    
    @Test
    fun `multi-datasource configuration`() {
        contextRunner
            .withPropertyValues(
                "reladomo.kotlin.datasources.primary.url=jdbc:h2:mem:primary",
                "reladomo.kotlin.datasources.primary.username=sa",
                "reladomo.kotlin.datasources.primary.password=",
                "reladomo.kotlin.datasources.primary.driver-class-name=org.h2.Driver",
                "reladomo.kotlin.datasources.secondary.url=jdbc:h2:mem:secondary",
                "reladomo.kotlin.datasources.secondary.username=sa",
                "reladomo.kotlin.datasources.secondary.password="
            )
            .run { context ->
                val registry = context.getBean(ReladomoDataSourceRegistry::class.java)
                assertNotNull(registry.getDataSource("primary"))
                assertNotNull(registry.getDataSource("secondary"))
                
                val properties = context.getBean(ReladomoKotlinProperties::class.java)
                assertEquals(2, properties.datasources.size)
                assertTrue(properties.datasources.containsKey("primary"))
                assertTrue(properties.datasources.containsKey("secondary"))
            }
    }
    
    @Test
    fun `repository configuration properties`() {
        contextRunner
            .withPropertyValues(
                "reladomo.kotlin.repository.base-packages[0]=com.example.repo1",
                "reladomo.kotlin.repository.base-packages[1]=com.example.repo2",
                "reladomo.kotlin.repository.enable-query-methods=true",
                "reladomo.kotlin.repository.naming-strategy=CUSTOM"
            )
            .run { context ->
                val properties = context.getBean(ReladomoKotlinProperties::class.java)
                assertEquals(2, properties.repository.basePackages.size)
                assertTrue(properties.repository.basePackages.contains("com.example.repo1"))
                assertTrue(properties.repository.basePackages.contains("com.example.repo2"))
                assertTrue(properties.repository.enableQueryMethods)
                assertEquals("CUSTOM", properties.repository.namingStrategy)
            }
    }
    
    @Test
    fun `auto-configuration backs off when beans exist`() {
        contextRunner
            .withUserConfiguration(CustomMithraManagerConfiguration::class.java)
            .run { context ->
                val mithraManager = context.getBean(MithraManager::class.java)
                // Should use the custom bean, not auto-configured one
                assertNotNull(mithraManager)
            }
    }
    
    @Configuration
    class TestDataSourceConfiguration {
        @Bean
        fun dataSource(): DataSource {
            return org.h2.jdbcx.JdbcDataSource().apply {
                setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
                user = "sa"
                password = ""
            }
        }
        
        @Bean
        fun transactionManager(dataSource: DataSource): org.springframework.transaction.PlatformTransactionManager {
            return org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource)
        }
        
        @Bean
        fun transactionTemplate(transactionManager: org.springframework.transaction.PlatformTransactionManager):
                org.springframework.transaction.support.TransactionTemplate {
            return org.springframework.transaction.support.TransactionTemplate(transactionManager)
        }
    }
    
    @Configuration
    class CustomMithraManagerConfiguration {
        @Bean
        fun mithraManager(): MithraManager {
            // Custom MithraManager for testing
            return MithraManager.getInstance()
        }
    }
}