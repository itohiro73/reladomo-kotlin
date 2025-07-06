package io.github.kotlinreladomo.spring.repository

import io.github.kotlinreladomo.core.BaseRepository
import io.github.kotlinreladomo.core.BiTemporalEntity
import io.github.kotlinreladomo.core.BiTemporalRepository
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Default implementation of ReladomoRepository.
 */
open class SimpleReladomoRepository<T : Any, ID : Any>(
    internal val baseRepository: BaseRepository<T, ID>,
    protected val transactionTemplate: TransactionTemplate,
    protected val entityType: KClass<T>,
    protected val idType: KClass<ID>
) : ReladomoRepository<T, ID> {
    
    override fun save(entity: T): T {
        return transactionTemplate.execute {
            baseRepository.save(entity)
        } ?: throw IllegalStateException("Failed to save entity")
    }
    
    override fun saveAll(entities: Iterable<T>): List<T> {
        return transactionTemplate.execute {
            entities.map { baseRepository.save(it) }
        } ?: throw IllegalStateException("Failed to save entities")
    }
    
    override fun findById(id: ID): T? {
        return baseRepository.findById(id)
    }
    
    override fun existsById(id: ID): Boolean {
        return findById(id) != null
    }
    
    override fun findAll(): List<T> {
        return baseRepository.findAll()
    }
    
    override fun findAllById(ids: Iterable<ID>): List<T> {
        return ids.mapNotNull { findById(it) }
    }
    
    override fun count(): Long {
        return baseRepository.count()
    }
    
    override fun deleteById(id: ID) {
        transactionTemplate.execute {
            baseRepository.deleteById(id)
        }
    }
    
    override fun delete(entity: T) {
        transactionTemplate.execute {
            baseRepository.delete(entity)
        }
    }
    
    override fun deleteAll(entities: Iterable<T>) {
        transactionTemplate.execute {
            entities.forEach { baseRepository.delete(it) }
        }
    }
    
    override fun deleteAll() {
        transactionTemplate.execute {
            baseRepository.deleteAll()
        }
    }
}

/**
 * Implementation for bitemporal repositories.
 */
open class SimpleBiTemporalReladomoRepository<T : BiTemporalEntity, ID : Any>(
    private val biTemporalRepository: BiTemporalRepository<T, ID>,
    transactionTemplate: TransactionTemplate,
    entityType: KClass<T>,
    idType: KClass<ID>
) : SimpleReladomoRepository<T, ID>(biTemporalRepository, transactionTemplate, entityType, idType), 
    BiTemporalReladomoRepository<T, ID> {
    
    override fun findByIdAsOf(id: ID, businessDate: Instant, processingDate: Instant): T? {
        return biTemporalRepository.findByIdAsOf(id, businessDate, processingDate)
    }
    
    override fun update(entity: T, businessDate: Instant): T {
        return transactionTemplate.execute {
            biTemporalRepository.update(entity, businessDate)
        } ?: throw IllegalStateException("Failed to update entity")
    }
    
    override fun deleteByIdAsOf(id: ID, businessDate: Instant) {
        transactionTemplate.execute {
            biTemporalRepository.deleteByIdAsOf(id, businessDate)
        }
    }
    
    override fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<T> {
        return biTemporalRepository.findAllAsOf(businessDate, processingDate)
    }
    
    override fun getHistory(id: ID): List<T> {
        return biTemporalRepository.getHistory(id)
    }
}