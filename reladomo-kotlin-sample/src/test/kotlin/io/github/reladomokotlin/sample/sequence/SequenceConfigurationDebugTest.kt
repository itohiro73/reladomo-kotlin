package io.github.reladomokotlin.sample.sequence

import io.github.reladomokotlin.sequence.SequenceGenerator
import io.github.reladomokotlin.springboot.sequence.SequenceAutoConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = [
    "reladomo.sequence.enabled=true",
    "reladomo.sequence.type=IN_MEMORY",
    "reladomo.sequence.in-memory.start-value=2000",
    "reladomo.sequence.in-memory.increment-by=1"
])
class SequenceConfigurationDebugTest {
    
    @Autowired
    private lateinit var applicationContext: ApplicationContext
    
    @Autowired(required = false)
    private var sequenceGenerator: SequenceGenerator? = null
    
    @Test
    fun `debug sequence configuration`() {
        println("=== Sequence Configuration Debug ===")
        
        // Check if ReladomoKotlinAutoConfiguration is loaded
        try {
            val reladomoAutoConfig = applicationContext.getBean("io.github.reladomokotlin.spring.config.ReladomoKotlinAutoConfiguration")
            println("ReladomoKotlinAutoConfiguration loaded: ${reladomoAutoConfig != null}")
        } catch (e: Exception) {
            println("ReladomoKotlinAutoConfiguration not found: ${e.message}")
        }
        
        // Check MithraManager
        try {
            val mithraManager = applicationContext.getBean(com.gs.fw.common.mithra.MithraManager::class.java)
            println("MithraManager bean found: ${mithraManager != null}")
        } catch (e: Exception) {
            println("MithraManager not found: ${e.message}")
        }
        
        // Check if SequenceAutoConfiguration is loaded
        val autoConfigBeans = applicationContext.getBeansOfType(SequenceAutoConfiguration::class.java)
        println("\nSequenceAutoConfiguration beans: ${autoConfigBeans.size}")
        autoConfigBeans.forEach { (name, bean) ->
            println("  - $name: ${bean.javaClass.name}")
        }
        
        // Check if SequenceGenerator bean exists
        val sequenceGeneratorBeans = applicationContext.getBeansOfType(SequenceGenerator::class.java)
        println("\nSequenceGenerator beans: ${sequenceGeneratorBeans.size}")
        sequenceGeneratorBeans.forEach { (name, bean) ->
            println("  - $name: ${bean.javaClass.name}")
        }
        
        // Check properties
        val env = applicationContext.environment
        println("\nSequence properties:")
        println("  - reladomo.sequence.enabled: ${env.getProperty("reladomo.sequence.enabled")}")
        println("  - reladomo.sequence.type: ${env.getProperty("reladomo.sequence.type")}")
        println("  - reladomo.sequence.in-memory.start-value: ${env.getProperty("reladomo.sequence.in-memory.start-value")}")
        
        // List all bean names containing "sequence"
        println("\nAll beans containing 'sequence':")
        applicationContext.beanDefinitionNames
            .filter { it.contains("sequence", ignoreCase = true) }
            .forEach { println("  - $it") }
        
        println("\nSequence generator injected: ${sequenceGenerator != null}")
        if (sequenceGenerator != null) {
            println("Sequence generator type: ${sequenceGenerator!!.javaClass.name}")
        }
    }
}