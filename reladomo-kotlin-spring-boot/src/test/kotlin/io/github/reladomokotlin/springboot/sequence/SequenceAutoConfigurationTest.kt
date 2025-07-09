package io.github.reladomokotlin.springboot.sequence

import io.github.reladomokotlin.sequence.InMemorySequenceGenerator
import io.github.reladomokotlin.sequence.ReladomoSequenceGenerator
import io.github.reladomokotlin.sequence.SequenceGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class SequenceAutoConfigurationTest {
    
    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SequenceAutoConfiguration::class.java))
    
    @Test
    fun `should not create sequence generator when disabled`() {
        contextRunner
            .withPropertyValues("reladomo.sequence.enabled=false")
            .run { context ->
                assertFalse(context.containsBean("inMemorySequenceGenerator"))
                assertFalse(context.containsBean("reladomoSequenceGenerator"))
                assertFalse(context.containsBean("sequenceGeneratorBeanPostProcessor"))
            }
    }
    
    @Test
    fun `should create in-memory sequence generator by default when enabled`() {
        contextRunner
            .withPropertyValues("reladomo.sequence.enabled=true")
            .run { context ->
                assertTrue(context.containsBean("inMemorySequenceGenerator"))
                assertFalse(context.containsBean("reladomoSequenceGenerator"))
                
                val generator = context.getBean(SequenceGenerator::class.java)
                assertInstanceOf(InMemorySequenceGenerator::class.java, generator)
                
                // Test default configuration
                val id = generator.getNextId("Test")
                assertEquals(1000L, id) // Default start value
            }
    }
    
    @Test
    fun `should create in-memory sequence generator with custom configuration`() {
        contextRunner
            .withPropertyValues(
                "reladomo.sequence.enabled=true",
                "reladomo.sequence.type=IN_MEMORY",
                "reladomo.sequence.default-start-value=5000",
                "reladomo.sequence.increment-by=10"
            )
            .run { context ->
                val generator = context.getBean(SequenceGenerator::class.java)
                assertInstanceOf(InMemorySequenceGenerator::class.java, generator)
                
                val id1 = generator.getNextId("Test")
                val id2 = generator.getNextId("Test")
                
                assertEquals(5000L, id1)
                assertEquals(5010L, id2)
            }
    }
    
    @Test
    fun `should create Reladomo sequence generator when configured`() {
        contextRunner
            .withPropertyValues(
                "reladomo.sequence.enabled=true",
                "reladomo.sequence.type=RELADOMO",
                "reladomo.sequence.default-start-value=2000",
                "reladomo.sequence.increment-by=5"
            )
            .run { context ->
                assertTrue(context.containsBean("reladomoSequenceGenerator"))
                assertFalse(context.containsBean("inMemorySequenceGenerator"))
                
                val generator = context.getBean(SequenceGenerator::class.java)
                assertInstanceOf(ReladomoSequenceGenerator::class.java, generator)
            }
    }
    
    @Test
    fun `should use custom sequence generator when provided`() {
        contextRunner
            .withUserConfiguration(CustomSequenceGeneratorConfig::class.java)
            .withPropertyValues(
                "reladomo.sequence.enabled=true",
                "reladomo.sequence.type=CUSTOM"
            )
            .run { context ->
                // Should not create default generators
                assertFalse(context.containsBean("inMemorySequenceGenerator"))
                assertFalse(context.containsBean("reladomoSequenceGenerator"))
                
                // Should use the custom generator
                val generator = context.getBean(SequenceGenerator::class.java)
                assertInstanceOf(CustomSequenceGenerator::class.java, generator)
                
                val id = generator.getNextId("Test")
                assertEquals(42L, id) // Custom generator always returns 42
            }
    }
    
    @Test
    fun `should create bean post processor when sequence is enabled`() {
        contextRunner
            .withPropertyValues("reladomo.sequence.enabled=true")
            .run { context ->
                assertTrue(context.containsBean("sequenceGeneratorBeanPostProcessor"))
                
                val processor = context.getBean(SequenceGeneratorBeanPostProcessor::class.java)
                assertNotNull(processor)
            }
    }
    
    @Test
    fun `should handle missing sequence generator in bean post processor`() {
        contextRunner
            .withPropertyValues("reladomo.sequence.enabled=true")
            .withUserConfiguration(NoSequenceGeneratorConfig::class.java)
            .run { context ->
                // Bean post processor should still be created even without generator
                assertTrue(context.containsBean("sequenceGeneratorBeanPostProcessor"))
                
                // It should handle null generator gracefully
                val processor = context.getBean(SequenceGeneratorBeanPostProcessor::class.java)
                assertNotNull(processor)
            }
    }
    
    @Configuration
    class CustomSequenceGeneratorConfig {
        @Bean
        fun customSequenceGenerator(): SequenceGenerator = CustomSequenceGenerator()
    }
    
    @Configuration
    class NoSequenceGeneratorConfig {
        // Intentionally provide no SequenceGenerator bean
    }
    
    class CustomSequenceGenerator : SequenceGenerator {
        override fun getNextId(sequenceName: String): Long = 42L
        override fun getNextIds(sequenceName: String, count: Int): List<Long> = List(count) { 42L }
        override fun resetSequence(sequenceName: String, value: Long) {
            // No-op for test
        }
    }
}