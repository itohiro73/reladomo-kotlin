package io.github.reladomokotlin.springboot.sequence

import io.github.reladomokotlin.sequence.SequenceGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * BeanPostProcessor that injects SequenceGenerator into repositories
 * that have properties annotated with @SequenceGenerated.
 */
class SequenceGeneratorBeanPostProcessor(
    private val sequenceGenerator: SequenceGenerator?
) : BeanPostProcessor {
    
    private val logger = LoggerFactory.getLogger(SequenceGeneratorBeanPostProcessor::class.java)
    
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        if (sequenceGenerator == null) {
            return bean
        }
        
        val beanClass = bean::class
        
        // Check if this is a repository (by convention or annotation)
        if (!beanName.endsWith("Repository") && !beanClass.simpleName?.endsWith("Repository")!!) {
            return bean
        }
        
        // Look for properties that need sequence generator injection
        beanClass.memberProperties
            .filterIsInstance<KMutableProperty<*>>()
            .filter { it.returnType.classifier == SequenceGenerator::class }
            .forEach { property ->
                try {
                    property.isAccessible = true
                    property.setter.call(bean, sequenceGenerator)
                    logger.debug("Injected SequenceGenerator into $beanName.${property.name}")
                } catch (e: Exception) {
                    logger.warn("Failed to inject SequenceGenerator into $beanName.${property.name}", e)
                }
            }
        
        return bean
    }
}