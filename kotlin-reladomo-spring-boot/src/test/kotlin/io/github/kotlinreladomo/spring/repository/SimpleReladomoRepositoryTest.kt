package io.github.kotlinreladomo.spring.repository

import io.github.kotlinreladomo.core.BaseRepository
import io.github.kotlinreladomo.core.BiTemporalEntity
import io.github.kotlinreladomo.core.BiTemporalRepository
import io.github.kotlinreladomo.spring.config.ReladomoKotlinAutoConfiguration
import io.github.kotlinreladomo.spring.config.ReladomoKotlinProperties
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SimpleReladomoRepositoryTest {
    
    private lateinit var baseRepository: BaseRepository<TestEntity, Long>
    private lateinit var transactionTemplate: TransactionTemplate
    private lateinit var repository: SimpleReladomoRepository<TestEntity, Long>
    
    @BeforeEach
    fun setup() {
        baseRepository = mockk()
        transactionTemplate = mockk()
        
        repository = SimpleReladomoRepository(
            baseRepository = baseRepository,
            transactionTemplate = transactionTemplate,
            entityType = TestEntity::class,
            idType = Long::class
        )
        
        // Default transaction template behavior
        every { transactionTemplate.execute<Any>(any()) } answers {
            val callback = firstArg<TransactionCallback<Any>>()
            callback.doInTransaction(mockk())
        }
    }
    
    @Test
    fun `save delegates to base repository`() {
        val entity = TestEntity(1L, "test")
        val savedEntity = TestEntity(1L, "test")
        
        every { baseRepository.save(entity) } returns savedEntity
        
        val result = repository.save(entity)
        
        assertEquals(savedEntity, result)
        verify { baseRepository.save(entity) }
    }
    
    @Test
    fun `saveAll delegates to base repository`() {
        val entities = listOf(
            TestEntity(1L, "test1"),
            TestEntity(2L, "test2")
        )
        
        every { baseRepository.save(any<TestEntity>()) } returnsArgument 0
        
        val result = repository.saveAll(entities)
        
        assertEquals(entities, result)
        verify(exactly = 2) { baseRepository.save(any()) }
    }
    
    @Test
    fun `findById delegates to base repository`() {
        val entity = TestEntity(1L, "test")
        
        every { baseRepository.findById(1L) } returns entity
        
        val result = repository.findById(1L)
        
        assertEquals(entity, result)
        verify { baseRepository.findById(1L) }
    }
    
    @Test
    fun `existsById returns true when entity exists`() {
        every { baseRepository.findById(1L) } returns TestEntity(1L, "test")
        
        val result = repository.existsById(1L)
        
        assertTrue(result)
        verify { baseRepository.findById(1L) }
    }
    
    @Test
    fun `existsById returns false when entity does not exist`() {
        every { baseRepository.findById(1L) } returns null
        
        val result = repository.existsById(1L)
        
        assertFalse(result)
        verify { baseRepository.findById(1L) }
    }
    
    @Test
    fun `findAll delegates to base repository`() {
        val entities = listOf(
            TestEntity(1L, "test1"),
            TestEntity(2L, "test2")
        )
        
        every { baseRepository.findAll() } returns entities
        
        val result = repository.findAll()
        
        assertEquals(entities, result)
        verify { baseRepository.findAll() }
    }
    
    @Test
    fun `count delegates to base repository`() {
        every { baseRepository.count() } returns 42L
        
        val result = repository.count()
        
        assertEquals(42L, result)
        verify { baseRepository.count() }
    }
    
    @Test
    fun `deleteById delegates to base repository`() {
        every { baseRepository.deleteById(1L) } just Runs
        
        repository.deleteById(1L)
        
        verify { baseRepository.deleteById(1L) }
    }
    
    @Test
    fun `delete delegates to base repository`() {
        val entity = TestEntity(1L, "test")
        
        every { baseRepository.delete(entity) } just Runs
        
        repository.delete(entity)
        
        verify { baseRepository.delete(entity) }
    }
    
    @Test
    fun `deleteAll with entities delegates to base repository`() {
        val entities = listOf(
            TestEntity(1L, "test1"),
            TestEntity(2L, "test2")
        )
        
        every { baseRepository.delete(any<TestEntity>()) } just Runs
        
        repository.deleteAll(entities)
        
        verify(exactly = 2) { baseRepository.delete(any()) }
    }
    
    @Test
    fun `deleteAll without entities delegates to base repository`() {
        every { baseRepository.deleteAll() } just Runs
        
        repository.deleteAll()
        
        verify { baseRepository.deleteAll() }
    }
    
    data class TestEntity(val id: Long, val name: String)
}

class SimpleBiTemporalReladomoRepositoryTest {
    
    private lateinit var biTemporalRepository: BiTemporalRepository<TestBiTemporalEntity, Long>
    private lateinit var transactionTemplate: TransactionTemplate
    private lateinit var repository: SimpleBiTemporalReladomoRepository<TestBiTemporalEntity, Long>
    
    @BeforeEach
    fun setup() {
        biTemporalRepository = mockk()
        transactionTemplate = mockk()
        
        repository = SimpleBiTemporalReladomoRepository(
            biTemporalRepository = biTemporalRepository,
            transactionTemplate = transactionTemplate,
            entityType = TestBiTemporalEntity::class,
            idType = Long::class
        )
        
        // Default transaction template behavior
        every { transactionTemplate.execute<Any>(any()) } answers {
            val callback = firstArg<TransactionCallback<Any>>()
            callback.doInTransaction(mockk())
        }
    }
    
    @Test
    fun `findByIdAsOf delegates to base repository`() {
        val businessDate = Instant.now()
        val processingDate = Instant.now()
        val entity = TestBiTemporalEntity(1L, "test", businessDate, processingDate)
        
        every { biTemporalRepository.findByIdAsOf(1L, businessDate, processingDate) } returns entity
        
        val result = repository.findByIdAsOf(1L, businessDate, processingDate)
        
        assertEquals(entity, result)
        verify { biTemporalRepository.findByIdAsOf(1L, businessDate, processingDate) }
    }
    
    @Test
    fun `findByIdAsOf with default processing date`() {
        val businessDate = Instant.now()
        val entity = TestBiTemporalEntity(1L, "test", businessDate, Instant.now())
        
        every { biTemporalRepository.findByIdAsOf(1L, businessDate, any()) } returns entity
        
        val result = repository.findByIdAsOf(1L, businessDate)
        
        assertEquals(entity, result)
        verify { biTemporalRepository.findByIdAsOf(1L, businessDate, any()) }
    }
    
    @Test
    fun `update delegates to base repository`() {
        val businessDate = Instant.now()
        val entity = TestBiTemporalEntity(1L, "test", businessDate, Instant.now())
        val updatedEntity = TestBiTemporalEntity(1L, "updated", businessDate, Instant.now())
        
        every { biTemporalRepository.update(entity, businessDate) } returns updatedEntity
        
        val result = repository.update(entity, businessDate)
        
        assertEquals(updatedEntity, result)
        verify { biTemporalRepository.update(entity, businessDate) }
    }
    
    @Test
    fun `deleteByIdAsOf delegates to base repository`() {
        val businessDate = Instant.now()
        
        every { biTemporalRepository.deleteByIdAsOf(1L, businessDate) } just Runs
        
        repository.deleteByIdAsOf(1L, businessDate)
        
        verify { biTemporalRepository.deleteByIdAsOf(1L, businessDate) }
    }
    
    @Test
    fun `findAllAsOf delegates to base repository`() {
        val businessDate = Instant.now()
        val processingDate = Instant.now()
        val entities = listOf(
            TestBiTemporalEntity(1L, "test1", businessDate, processingDate),
            TestBiTemporalEntity(2L, "test2", businessDate, processingDate)
        )
        
        every { biTemporalRepository.findAllAsOf(businessDate, processingDate) } returns entities
        
        val result = repository.findAllAsOf(businessDate, processingDate)
        
        assertEquals(entities, result)
        verify { biTemporalRepository.findAllAsOf(businessDate, processingDate) }
    }
    
    @Test
    fun `findAllAsOf with default processing date`() {
        val businessDate = Instant.now()
        val entities = listOf(
            TestBiTemporalEntity(1L, "test1", businessDate, Instant.now())
        )
        
        every { biTemporalRepository.findAllAsOf(businessDate, any()) } returns entities
        
        val result = repository.findAllAsOf(businessDate)
        
        assertEquals(entities, result)
        verify { biTemporalRepository.findAllAsOf(businessDate, any()) }
    }
    
    @Test
    fun `getHistory delegates to base repository`() {
        val history = listOf(
            TestBiTemporalEntity(1L, "version1", Instant.now().minusSeconds(3600), Instant.now()),
            TestBiTemporalEntity(1L, "version2", Instant.now(), Instant.now())
        )
        
        every { biTemporalRepository.getHistory(1L) } returns history
        
        val result = repository.getHistory(1L)
        
        assertEquals(history, result)
        verify { biTemporalRepository.getHistory(1L) }
    }
    
    data class TestBiTemporalEntity(
        val id: Long,
        val name: String,
        override val businessDate: Instant,
        override val processingDate: Instant
    ) : BiTemporalEntity
}