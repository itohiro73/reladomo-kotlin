package io.github.reladomokotlin.spring.repository

import io.github.reladomokotlin.spring.repository.query.QueryMethodParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * Factory bean for creating Reladomo repository instances.
 */
class ReladomoRepositoryFactoryBean<T : ReladomoRepository<*, *>>(
    private val repositoryInterface: Class<T>
) : FactoryBean<T> {
    
    private val logger = LoggerFactory.getLogger(ReladomoRepositoryFactoryBean::class.java)
    
    @Autowired
    private lateinit var applicationContext: ApplicationContext
    
    override fun getObject(): T {
        logger.debug("Creating repository proxy for: ${repositoryInterface.name}")
        
        // Get the factory and create the repository
        val factory = applicationContext.getBean(ReladomoRepositoryFactory::class.java)
        val entityInfo = extractEntityInfo(repositoryInterface)
        
        @Suppress("UNCHECKED_CAST")
        return factory.createRepository(
            repositoryInterface,
            entityInfo.entityClass,
            ReladomoEntityMetadata(
                entityType = entityInfo.entityClass,
                idType = entityInfo.idClass,
                isBitemporal = entityInfo.isBitemporal
            )
        ) as T
    }
    
    override fun getObjectType(): Class<*> = repositoryInterface
    
    override fun isSingleton(): Boolean = true
    
    private fun extractEntityInfo(repositoryInterface: Class<*>): EntityInfo {
        // Look for generic type parameters
        repositoryInterface.genericInterfaces.forEach { type ->
            if (type is java.lang.reflect.ParameterizedType) {
                val rawType = type.rawType
                if (rawType == ReladomoRepository::class.java || 
                    rawType == BiTemporalReladomoRepository::class.java) {
                    val typeArgs = type.actualTypeArguments
                    if (typeArgs.size >= 2) {
                        val entityType = typeArgs[0] as? Class<*>
                        val idType = typeArgs[1] as? Class<*>
                        if (entityType != null && idType != null) {
                            return EntityInfo(
                                entityClass = entityType.kotlin,
                                idClass = idType.kotlin,
                                isBitemporal = rawType == BiTemporalReladomoRepository::class.java
                            )
                        }
                    }
                }
            }
        }
        throw IllegalArgumentException("Cannot extract entity information from repository: ${repositoryInterface.name}")
    }
    
    
    private data class EntityInfo(
        val entityClass: KClass<*>,
        val idClass: KClass<*>,
        val isBitemporal: Boolean
    )
}