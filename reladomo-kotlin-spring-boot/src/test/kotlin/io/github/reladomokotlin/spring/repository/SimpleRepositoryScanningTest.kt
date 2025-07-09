package io.github.reladomokotlin.spring.repository

import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalRepository
import io.github.reladomokotlin.spring.config.ReladomoKotlinAutoConfiguration
import io.github.reladomokotlin.spring.repository.test.*
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Configuration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimpleRepositoryScanningTest {
    
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(
            TestConfiguration::class.java,
            ReladomoKotlinAutoConfiguration::class.java
        )
    
    @Test
    fun `repository factory can create repositories`() {
        contextRunner.run { context ->
            val factory = context.getBean(ReladomoRepositoryFactory::class.java)
            assertNotNull(factory)
            
            // Test manual repository creation
            val orderMetadata = ReladomoEntityMetadata(
                entityType = TestOrder::class,
                idType = Long::class,
                isBitemporal = true
            )
            
            val orderRepo = factory.createRepository(
                TestOrderRepository::class.java,
                TestOrder::class,
                orderMetadata
            )
            
            assertNotNull(orderRepo)
            assertEquals(TestOrderRepository::class.java, orderRepo.javaClass.interfaces[0])
        }
    }
    
    @Configuration
    class TestConfiguration {
        @Bean
        fun dataSource() = org.h2.jdbcx.JdbcDataSource().apply {
            setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = ""
        }
        
        @Bean
        fun transactionManager(dataSource: javax.sql.DataSource) = 
            org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource)
        
        @Bean
        fun transactionTemplate(transactionManager: org.springframework.transaction.PlatformTransactionManager) = 
            org.springframework.transaction.support.TransactionTemplate(transactionManager)
        
        // Mock base repository for TestOrder
        @Bean
        fun testOrderRepository() = io.mockk.mockk<BiTemporalRepository<TestOrder, Long>>()
    }
}