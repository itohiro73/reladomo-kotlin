package io.github.reladomokotlin.spring.config

import io.github.reladomokotlin.spring.config.test.TestEntityRepository
import io.github.reladomokotlin.spring.repository.EnableReladomoRepositories
import io.github.reladomokotlin.spring.repository.ReladomoRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Repository
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Integration test demonstrating Spring Boot auto-configuration.
 */
@SpringBootTest
@TestPropertySource(properties = [
    "reladomo.kotlin.connection-manager-config-file=classpath:ReladomoRuntimeConfig.xml",
    "reladomo.kotlin.repository.base-packages[0]=io.github.reladomokotlin.spring.config.test",
    "reladomo.kotlin.repository.enable-query-methods=true",
    "reladomo.kotlin.cache.type=PARTIAL",
    "reladomo.kotlin.cache.max-size=1000",
    "reladomo.kotlin.transaction-management-enabled=true",
    "reladomo.kotlin.database-timezone=UTC",
    "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
])
class SpringBootIntegrationTest {
    
    @Autowired
    private lateinit var applicationContext: ApplicationContext
    
    @Test
    fun `Spring Boot context loads with auto-configuration`() {
        // Verify context loads
        assertNotNull(applicationContext)
        
        // Verify auto-configuration beans
        assertTrue(applicationContext.containsBean("mithraManager"))
        assertTrue(applicationContext.containsBean("reladomoTransactionManager"))
        assertTrue(applicationContext.containsBean("reladomoRepositoryFactory"))
        assertTrue(applicationContext.containsBean("reladomoInitializer"))
    }
    
    @Test
    fun `transaction manager is properly configured`() {
        val transactionManager = applicationContext.getBean("reladomoTransactionManager", PlatformTransactionManager::class.java)
        assertNotNull(transactionManager)
        assertTrue(transactionManager is io.github.reladomokotlin.spring.transaction.ReladomoTransactionManager)
    }
    
    @Test
    fun `repository scanning creates repository beans`() {
        // Verify repository bean was created
        val repositories = applicationContext.getBeansOfType(TestEntityRepository::class.java)
        assertFalse(repositories.isEmpty())
        
        // Verify we can get the specific repository
        val testRepository = applicationContext.getBean(TestEntityRepository::class.java)
        assertNotNull(testRepository)
    }
    
    @Test
    fun `properties are correctly loaded`() {
        val properties = applicationContext.getBean(ReladomoKotlinProperties::class.java)
        
        assertEquals("PARTIAL", properties.cache.type.name)
        assertEquals(1000, properties.cache.maxSize)
        assertTrue(properties.repository.enableQueryMethods)
        assertTrue(properties.repository.basePackages.contains("io.github.reladomokotlin.spring.config.test"))
        assertEquals("UTC", properties.databaseTimeZone)
    }
    
    @Test
    fun `cache configuration is applied`() {
        val cacheConfig = applicationContext.getBean(ReladomoCacheConfiguration::class.java)
        assertNotNull(cacheConfig)
    }
    
    @Test
    fun `datasource registry is available for multi-datasource support`() {
        val registry = applicationContext.getBean(ReladomoDataSourceRegistry::class.java)
        assertNotNull(registry)
        
        // Default datasource should be registered
        val defaultDs = registry.getDataSource("default")
        assertNotNull(defaultDs)
    }
    
    @SpringBootApplication
    @EnableReladomoRepositories(basePackages = ["io.github.reladomokotlin.spring.config.test"])
    class TestApplication
}