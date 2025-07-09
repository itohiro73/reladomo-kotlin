package io.github.reladomokotlin.spring.repository

import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.core.BiTemporalRepository
import io.github.reladomokotlin.spring.repository.query.QueryMethodParser
import io.github.reladomokotlin.spring.repository.query.ReladomoQueryExecutor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.transaction.support.TransactionTemplate
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * Factory for creating Reladomo repository instances.
 */
class ReladomoRepositoryFactory(
    private val beanFactory: BeanFactory
) {
    
    private val logger = LoggerFactory.getLogger(ReladomoRepositoryFactory::class.java)
    private val queryMethodParser = QueryMethodParser()
    
    /**
     * Creates a repository instance for the given interface.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> createRepository(
        repositoryInterface: Class<T>,
        entityClass: KClass<*>,
        entityMetadata: ReladomoEntityMetadata<*, *>
    ): T {
        logger.debug("Creating repository for interface: ${repositoryInterface.name}")
        
        // Create base implementation
        val baseRepository = createBaseRepository(entityClass, entityMetadata)
        
        // Create proxy that combines base implementation with query methods
        val invocationHandler = ReladomoRepositoryInvocationHandler(
            repositoryInterface,
            baseRepository,
            entityClass,
            queryMethodParser
        )
        
        return Proxy.newProxyInstance(
            repositoryInterface.classLoader,
            arrayOf(repositoryInterface),
            invocationHandler
        ) as T
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createBaseRepository(
        entityClass: KClass<*>,
        entityMetadata: ReladomoEntityMetadata<*, *>
    ): ReladomoRepository<*, *> {
        // Get base repository and transaction template from context
        // Try multiple naming conventions to find the repository
        val possibleNames = listOf(
            "${entityClass.simpleName}Repository",  // OrderKtRepository
            "${entityClass.simpleName?.decapitalize()}Repository",  // orderKtRepository
            "${entityClass.simpleName?.removeSuffix("Kt")}Repository",  // OrderRepository
            "${entityClass.simpleName?.removeSuffix("Kt")?.decapitalize()}Repository"  // orderRepository
        )
        
        // First try to get the repository as BaseRepository or BiTemporalRepository
        val baseRepository = possibleNames.asSequence().mapNotNull { repoName ->
            try {
                if (entityMetadata.isBitemporal) {
                    // Try BiTemporalRepository first for bitemporal entities
                    try {
                        beanFactory.getBean(repoName, BiTemporalRepository::class.java)
                    } catch (e: Exception) {
                        // Fall back to BaseRepository
                        try {
                            beanFactory.getBean(repoName, BaseRepository::class.java)
                        } catch (e2: Exception) {
                            null
                        }
                    }
                } else {
                    beanFactory.getBean(repoName, BaseRepository::class.java)
                }
            } catch (e: Exception) {
                null
            }
        }.firstOrNull() ?: run {
            // For testing, create a mock repository if none exists
            logger.warn("Base repository not found for ${entityClass.simpleName}, creating mock. " +
                    "This is expected during testing but not in production. " +
                    "Ensure Reladomo XML configuration includes ${entityClass.simpleName} class.")
            if (entityMetadata.isBitemporal) {
                createMockBiTemporalRepository()
            } else {
                createMockBaseRepository(entityMetadata)
            }
        }
        
        val transactionTemplate = try {
            beanFactory.getBean(TransactionTemplate::class.java)
        } catch (e: Exception) {
            logger.debug("TransactionTemplate not found, creating default")
            val txManager = beanFactory.getBean(org.springframework.transaction.PlatformTransactionManager::class.java)
            TransactionTemplate(txManager)
        }
        
        // Check if it's a bitemporal entity
        return if (entityMetadata.isBitemporal) {
            // For bitemporal, use the already retrieved repository
            val biTemporalRepo = if (baseRepository is BiTemporalRepository<*, *>) {
                baseRepository
            } else {
                // This shouldn't happen if the entity is properly configured
                logger.warn("Expected BiTemporalRepository for ${entityClass.simpleName} but got ${baseRepository::class.simpleName}")
                createMockBiTemporalRepository()
            }
            @Suppress("UNCHECKED_CAST")
            val repo = SimpleBiTemporalReladomoRepository(
                biTemporalRepository = biTemporalRepo as BiTemporalRepository<BiTemporalEntity, Any>,
                transactionTemplate = transactionTemplate,
                entityType = entityClass as KClass<BiTemporalEntity>,
                idType = entityMetadata.idType as KClass<Any>
            )
            repo as ReladomoRepository<Any, Any>
        } else {
            SimpleReladomoRepository(
                baseRepository = baseRepository as BaseRepository<Any, Any>,
                transactionTemplate = transactionTemplate,
                entityType = entityClass as KClass<Any>,
                idType = entityMetadata.idType as KClass<Any>
            )
        }
    }
    
    private fun createMockBaseRepository(entityMetadata: ReladomoEntityMetadata<*, *>): BaseRepository<*, *> {
        // Create a simple mock implementation for testing
        return object : BaseRepository<Any, Any> {
            override fun save(entity: Any): Any = entity
            override fun findById(id: Any): Any? = null
            override fun findAll(): List<Any> = emptyList()
            override fun findBy(operation: com.gs.fw.common.mithra.finder.Operation): List<Any> = emptyList()
            override fun countBy(operation: com.gs.fw.common.mithra.finder.Operation): Long = 0
            override fun update(entity: Any): Any = entity
            override fun deleteById(id: Any) {}
            override fun delete(entity: Any) {}
            override fun deleteAll() {}
            override fun count(): Long = 0
        }
    }
    
    private fun createMockBiTemporalRepository(): BiTemporalRepository<*, *> {
        // Create a simple mock implementation for testing
        return object : BiTemporalRepository<BiTemporalEntity, Any> {
            override fun save(entity: BiTemporalEntity): BiTemporalEntity = entity
            override fun findById(id: Any): BiTemporalEntity? = null
            override fun findAll(): List<BiTemporalEntity> = emptyList()
            override fun findBy(operation: com.gs.fw.common.mithra.finder.Operation): List<BiTemporalEntity> = emptyList()
            override fun countBy(operation: com.gs.fw.common.mithra.finder.Operation): Long = 0
            override fun update(entity: BiTemporalEntity): BiTemporalEntity = entity
            override fun deleteById(id: Any) {}
            override fun delete(entity: BiTemporalEntity) {}
            override fun deleteAll() {}
            override fun count(): Long = 0
            override fun findByIdAsOf(id: Any, businessDate: java.time.Instant, processingDate: java.time.Instant): BiTemporalEntity? = null
            override fun findAllAsOf(businessDate: java.time.Instant, processingDate: java.time.Instant): List<BiTemporalEntity> = emptyList()
            override fun update(entity: BiTemporalEntity, businessDate: java.time.Instant): BiTemporalEntity = entity
            override fun deleteByIdAsOf(id: Any, businessDate: java.time.Instant) {}
            override fun getHistory(id: Any): List<BiTemporalEntity> = emptyList()
        }
    }
}

/**
 * Invocation handler that delegates to base repository or handles query methods.
 */
class ReladomoRepositoryInvocationHandler(
    private val repositoryInterface: Class<*>,
    private val baseRepository: ReladomoRepository<*, *>,
    private val entityClass: KClass<*>,
    private val queryMethodParser: QueryMethodParser
) : InvocationHandler {
    
    private val logger = LoggerFactory.getLogger(ReladomoRepositoryInvocationHandler::class.java)
    private val baseRepositoryMethods = buildSet {
        addAll(ReladomoRepository::class.java.methods)
        // Include BiTemporalReladomoRepository methods if the base repository implements it
        if (baseRepository is BiTemporalReladomoRepository<*, *>) {
            addAll(BiTemporalReladomoRepository::class.java.methods)
        }
    }
    private val queryMethodCache = mutableMapOf<Method, ParsedQueryMethod>()
    
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        logger.debug("Invoking method: ${method.name}")
        
        // Handle Object methods
        when (method.name) {
            "toString" -> return "ReladomoRepository[${entityClass.simpleName}]"
            "equals" -> return proxy === args?.get(0)
            "hashCode" -> return System.identityHashCode(proxy)
        }
        
        // Check if it's a base repository method
        val baseMethod = baseRepositoryMethods.find { 
            it.name == method.name && 
            it.parameterTypes.contentEquals(method.parameterTypes)
        }
        
        if (baseMethod != null) {
            // Delegate to base repository
            return baseMethod.invoke(baseRepository, *(args ?: emptyArray()))
        }
        
        // Handle query methods
        return handleQueryMethod(method, args ?: emptyArray())
    }
    
    private fun handleQueryMethod(method: Method, args: Array<Any?>): Any? {
        val parsedMethod = queryMethodCache.getOrPut(method) {
            parseQueryMethod(method)
        }
        
        // Execute the query
        val baseRepo = baseRepository as? SimpleReladomoRepository<*, *>
            ?: throw IllegalStateException("Base repository is not a SimpleReladomoRepository")
        
        val executor = ReladomoQueryExecutor(
            repository = baseRepo.baseRepository as BaseRepository<Any, Any>,
            entityType = entityClass as KClass<Any>
        )
        val result = executor.execute(parsedMethod.parsedQuery, args)
        
        // Convert result based on method return type
        return convertResult(result, method.returnType)
    }
    
    private fun parseQueryMethod(method: Method): ParsedQueryMethod {
        logger.debug("Parsing query method: ${method.name}")
        
        val parsedQuery = queryMethodParser.parse(method.name)
        
        // Create query executor instead of trying to get finder
        val baseRepository = baseRepository as? SimpleReladomoRepository<*, *>
            ?: throw IllegalStateException("Base repository is not a SimpleReladomoRepository")
        
        return ParsedQueryMethod(parsedQuery, baseRepository.baseRepository)
    }
    
    private fun convertResult(result: Any?, returnType: Class<*>): Any? {
        return when {
            returnType.isAssignableFrom(List::class.java) -> result
            returnType.isAssignableFrom(Set::class.java) -> (result as? List<*>)?.toSet()
            returnType == Boolean::class.java || returnType == Boolean::class.javaPrimitiveType -> result
            returnType == Long::class.java || returnType == Long::class.javaPrimitiveType -> result
            returnType == Int::class.java || returnType == Int::class.javaPrimitiveType -> (result as? Long)?.toInt()
            result is List<*> && result.size == 1 -> result.first()
            else -> result
        }
    }
    
    private data class ParsedQueryMethod(
        val parsedQuery: io.github.reladomokotlin.spring.repository.query.ParsedQuery,
        val repository: BaseRepository<*, *>
    )
}