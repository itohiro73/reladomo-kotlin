package io.github.kotlinreladomo.spring.repository

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.util.ClassUtils
import org.springframework.util.StringUtils

/**
 * Registers Reladomo repository beans.
 */
class ReladomoRepositoriesRegistrar : ImportBeanDefinitionRegistrar, ResourceLoaderAware {
    
    private val logger = LoggerFactory.getLogger(ReladomoRepositoriesRegistrar::class.java)
    private lateinit var resourceLoader: ResourceLoader
    
    override fun setResourceLoader(resourceLoader: ResourceLoader) {
        this.resourceLoader = resourceLoader
    }
    
    override fun registerBeanDefinitions(
        importingClassMetadata: AnnotationMetadata,
        registry: BeanDefinitionRegistry
    ) {
        val attributes = AnnotationAttributes.fromMap(
            importingClassMetadata.getAnnotationAttributes(EnableReladomoRepositories::class.java.name)
        ) ?: return
        
        // Register repository factory
        registerRepositoryFactory(registry)
        
        // Scan for repository interfaces
        val basePackages = getBasePackages(attributes, importingClassMetadata)
        scanForRepositories(basePackages, registry)
    }
    
    private fun registerRepositoryFactory(registry: BeanDefinitionRegistry) {
        // Factory is now created through auto-configuration
    }
    
    private fun getBasePackages(
        attributes: AnnotationAttributes,
        importingClassMetadata: AnnotationMetadata
    ): Set<String> {
        val basePackages = mutableSetOf<String>()
        
        // Add packages from annotation
        attributes.getStringArray("value").forEach { 
            if (it.isNotBlank()) basePackages.add(it)
        }
        attributes.getStringArray("basePackages").forEach { 
            if (it.isNotBlank()) basePackages.add(it)
        }
        
        // Add packages from basePackageClasses
        attributes.getClassArray("basePackageClasses").forEach { clazz ->
            basePackages.add(ClassUtils.getPackageName(clazz))
        }
        
        // If no packages specified, use the package of the importing class
        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.className))
        }
        
        return basePackages
    }
    
    private fun scanForRepositories(basePackages: Set<String>, registry: BeanDefinitionRegistry) {
        val scanner = createScanner()
        
        basePackages.forEach { basePackage ->
            logger.info("Scanning for Reladomo repositories in package: $basePackage")
            
            val candidateComponents = scanner.findCandidateComponents(basePackage)
            logger.debug("Found ${candidateComponents.size} candidate components in package: $basePackage")
            
            candidateComponents.forEach { candidate ->
                val beanClassName = candidate.beanClassName ?: return@forEach
                logger.debug("Processing candidate: $beanClassName")
                
                try {
                    val repositoryInterface = Class.forName(beanClassName)
                    
                    if (repositoryInterface.isInterface) {
                        logger.debug("Registering repository interface: ${repositoryInterface.name}")
                        registerRepository(repositoryInterface, registry)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to register repository: $beanClassName", e)
                }
            }
        }
    }
    
    private fun createScanner(): ClassPathScanningCandidateComponentProvider {
        val scanner = ReladomoRepositoryComponentProvider()
        scanner.resourceLoader = resourceLoader
        return scanner
    }
    
    private fun registerRepository(repositoryInterface: Class<*>, registry: BeanDefinitionRegistry) {
        val beanName = StringUtils.uncapitalize(repositoryInterface.simpleName)
        
        if (registry.containsBeanDefinition(beanName)) {
            logger.debug("Repository bean already registered: $beanName")
            return
        }
        
        logger.debug("Registering repository bean: $beanName for interface ${repositoryInterface.name}")
        
        // Extract entity class from repository interface
        val entityClass = extractEntityClass(repositoryInterface)
            ?: throw IllegalArgumentException("Cannot determine entity class for repository: ${repositoryInterface.name}")
        
        // Create bean definition using factory bean
        val beanDefinition = BeanDefinitionBuilder
            .genericBeanDefinition(ReladomoRepositoryFactoryBean::class.java)
            .addConstructorArgValue(repositoryInterface)
            .setLazyInit(false)
            .getBeanDefinition()
        
        registry.registerBeanDefinition(beanName, beanDefinition)
    }
    
    private fun extractEntityClass(repositoryInterface: Class<*>): Class<*>? {
        // Look for generic type parameters on ReladomoRepository interface
        repositoryInterface.genericInterfaces.forEach { type ->
            if (type is java.lang.reflect.ParameterizedType) {
                val rawType = type.rawType
                if (rawType == ReladomoRepository::class.java || 
                    rawType == BiTemporalReladomoRepository::class.java) {
                    val typeArgs = type.actualTypeArguments
                    if (typeArgs.isNotEmpty()) {
                        val entityType = typeArgs[0]
                        if (entityType is Class<*>) {
                            return entityType
                        }
                    }
                }
            }
        }
        
        // Try to infer from repository name (e.g., OrderRepository -> Order)
        val repoName = repositoryInterface.simpleName
        if (repoName.endsWith("Repository")) {
            val entityName = repoName.substring(0, repoName.length - "Repository".length)
            
            // Try to find entity class in same package
            val packageName = repositoryInterface.`package`.name
            val possiblePackages = listOf(
                packageName,
                packageName.replace(".repository", ""),
                packageName.replace(".repository", ".domain"),
                packageName.replace(".repository", ".entity")
            )
            
            possiblePackages.forEach { pkg ->
                try {
                    return Class.forName("$pkg.$entityName")
                } catch (e: ClassNotFoundException) {
                    // Try next package
                }
            }
        }
        
        return null
    }
}

// Extension to decapitalize string
private fun String.decapitalize(): String {
    return if (isNotEmpty() && this[0].isUpperCase()) {
        this[0].lowercase() + substring(1)
    } else {
        this
    }
}