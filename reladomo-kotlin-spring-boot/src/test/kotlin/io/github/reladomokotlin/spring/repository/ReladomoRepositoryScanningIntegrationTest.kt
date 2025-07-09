package io.github.reladomokotlin.spring.repository

import io.github.reladomokotlin.spring.config.ReladomoKotlinAutoConfiguration
import io.github.reladomokotlin.spring.config.ReladomoCacheConfiguration
import io.github.reladomokotlin.spring.config.ReladomoDataSourceConfiguration
import io.github.reladomokotlin.spring.repository.test.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import javax.sql.DataSource
import kotlin.test.*

@SpringBootTest(
    classes = [
        ReladomoKotlinAutoConfiguration::class,
        ReladomoCacheConfiguration::class,
        ReladomoDataSourceConfiguration::class,
        TestRepositoryScanConfiguration::class,
        ReladomoRepositoryScanningIntegrationTest.TestDataSourceConfig::class
    ]
)
@EnableReladomoRepositories(
    basePackages = ["io.github.reladomokotlin.spring.repository.test"]
)
class ReladomoRepositoryScanningIntegrationTest {
    
    @Autowired
    private lateinit var applicationContext: ApplicationContext
    
    @Autowired(required = false)
    private var testOrderRepository: TestOrderRepository? = null
    
    @Autowired(required = false)
    private var testCustomerRepository: TestCustomerRepository? = null
    
    @Autowired(required = false)
    private var testProductRepository: TestProductRepository? = null
    
    @Test
    fun `repository scanning creates repository beans`() {
        assertNotNull(testOrderRepository, "Order repository should be created")
        assertNotNull(testCustomerRepository, "Customer repository should be created")
        assertNotNull(testProductRepository, "Product repository should be created")
    }
    
    @Test
    fun `repositories are registered in application context`() {
        assertTrue(applicationContext.containsBean("testOrderRepository"))
        assertTrue(applicationContext.containsBean("testCustomerRepository"))
        assertTrue(applicationContext.containsBean("testProductRepository"))
    }
    
    @Test
    fun `repositories can be retrieved from context`() {
        val orderRepo = applicationContext.getBean(TestOrderRepository::class.java)
        val customerRepo = applicationContext.getBean(TestCustomerRepository::class.java)
        val productRepo = applicationContext.getBean(TestProductRepository::class.java)
        
        assertNotNull(orderRepo)
        assertNotNull(customerRepo)
        assertNotNull(productRepo)
    }
    
    @Test
    fun `custom query methods are available`() {
        assertNotNull(testOrderRepository)
        // Verify that custom methods exist (will throw if not)
        val method = testOrderRepository!!::class.java.getMethod("findByCustomerId", Long::class.java)
        assertNotNull(method)
    }
    
    @Test
    fun `bitemporal repository methods are available`() {
        assertNotNull(testOrderRepository)
        // Verify bitemporal methods exist
        val asOfMethod = testOrderRepository!!::class.java.getMethod(
            "findByIdAsOf", 
            Any::class.java,  // The proxy uses Object type
            Instant::class.java, 
            Instant::class.java
        )
        assertNotNull(asOfMethod)
    }
    
    
    @Configuration
    class TestDataSourceConfig {
        @Bean
        fun dataSource(): DataSource {
            return org.h2.jdbcx.JdbcDataSource().apply {
                setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
                user = "sa"
                password = ""
            }
        }
        
        @Bean
        fun transactionTemplate(transactionManager: org.springframework.transaction.PlatformTransactionManager) = 
            org.springframework.transaction.support.TransactionTemplate(transactionManager)
        
        @Bean
        fun transactionManager(dataSource: DataSource) = 
            org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource)
    }
}

@Configuration
class TestRepositoryScanConfiguration {
    // No additional configuration needed - repositories will be created by scanning
}