package io.github.reladomokotlin.sample.demo

import io.github.reladomokotlin.sample.repository.OrderSpringDataRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Demo configuration showing Spring Boot integration features.
 * 
 * To run this demo:
 * 1. Set spring.profiles.active=demo
 * 2. Ensure you have a proper Reladomo configuration
 * 3. Run the application
 */
@Configuration
@Profile("demo")
class SpringBootIntegrationDemo {
    
    private val logger = LoggerFactory.getLogger(SpringBootIntegrationDemo::class.java)
    
    @Bean
    fun demoRunner(orderRepository: OrderSpringDataRepository): CommandLineRunner = CommandLineRunner {
        logger.info("=== Kotlin Reladomo Spring Boot Integration Demo ===")
        
        // This demonstrates that:
        // 1. Repository scanning with @EnableReladomoRepositories works
        // 2. Repository beans are properly created and injected
        // 3. Spring Data-style query methods are available
        
        logger.info("Repository bean injected: ${orderRepository.javaClass.simpleName}")
        
        // The repository provides these query methods:
        // - Basic CRUD operations (findById, save, update, delete)
        // - Custom query methods (findByCustomerId, findByStatus, etc.)
        // - Temporal queries (findByIdAsOf, findByCustomerIdAsOf)
        // - Count and exists queries
        // - Complex queries with multiple conditions
        
        logger.info("""
            Available features:
            1. Repository Scanning:
               - @EnableReladomoRepositories automatically scans for repository interfaces
               - Creates Spring beans for each repository
               
            2. Query Method Support:
               - Method name parsing (findBy, countBy, existsBy, deleteBy)
               - Multiple conditions with And/Or
               - Comparison operators (GreaterThan, LessThan, Between, etc.)
               - String operations (StartingWith, EndingWith, Containing, Like)
               - Collection operations (In, NotIn)
               - Null handling (IsNull, IsNotNull)
               - Ordering (OrderBy with Asc/Desc)
               - Limiting (First, Top)
               
            3. BiTemporal Support:
               - AsOf queries for time travel
               - Business date and processing date support
               - History tracking
               
            4. Spring Integration:
               - Transaction management with @Transactional
               - Multiple datasource support
               - Cache configuration (FULL, PARTIAL, NONE)
               - Property-based configuration
               
            5. Configuration:
               Configure in application.yml:
               ```yaml
               reladomo:
                 kotlin:
                   repository:
                     base-packages: 
                       - io.github.reladomokotlin.sample.repository
                     enable-query-methods: true
                   cache:
                     type: PARTIAL
                     partial-cache-size: 10000
                   transaction-management-enabled: true
               ```
        """.trimIndent())
        
        logger.info("=== Demo Complete ===")
    }
}