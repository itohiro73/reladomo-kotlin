package io.github.kotlinreladomo.spring.repository

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

/**
 * Custom component provider that scans for repository interfaces.
 */
class ReladomoRepositoryComponentProvider : ClassPathScanningCandidateComponentProvider(false) {
    
    init {
        // Add filter to find all interfaces that extend ReladomoRepository
        addIncludeFilter(AssignableTypeFilter(ReladomoRepository::class.java))
    }
    
    override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
        val metadata = beanDefinition.metadata
        // Accept interfaces that are independent (not annotation or inner class)
        return metadata.isInterface && metadata.isIndependent
    }
}