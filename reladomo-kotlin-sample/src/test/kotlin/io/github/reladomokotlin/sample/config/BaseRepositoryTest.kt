package io.github.reladomokotlin.sample.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional

/**
 * Base test configuration for repository tests.
 * Provides common setup for Reladomo tests including:
 * - Test database configuration
 * - Reladomo runtime configuration
 * - Transaction management
 * - Schema creation
 */
@SpringBootTest
@Import(TestReladomoConfiguration::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=none",
    "reladomo.kotlin.connection-manager-config-file=test-reladomo-runtime-config.xml",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:schema.sql",
    "spring.sql.init.continue-on-error=true"
])
@Transactional
abstract class BaseRepositoryTest