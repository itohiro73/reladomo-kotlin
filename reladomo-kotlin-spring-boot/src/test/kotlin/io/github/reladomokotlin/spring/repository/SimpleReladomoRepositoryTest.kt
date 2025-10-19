package io.github.reladomokotlin.spring.repository

import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.core.BiTemporalRepository
import io.github.reladomokotlin.spring.config.ReladomoKotlinAutoConfiguration
import io.github.reladomokotlin.spring.config.ReladomoKotlinProperties
import org.mockito.kotlin.*
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
        baseRepository = mock()
        transactionTemplate = mock()

        repository = SimpleReladomoRepository(
            baseRepository = baseRepository,
            transactionTemplate = transactionTemplate,
            entityType = TestEntity::class,
            idType = Long::class
        )

        // Default transaction template behavior
        whenever(transactionTemplate.execute<Any>(any())).thenAnswer {
            val callback = it.arguments[0] as TransactionCallback<Any>
            callback.doInTransaction(mock())
        }
    }

    @Test
    fun `save delegates to base repository`() {
        val entity = TestEntity(1L, "test")
        val savedEntity = TestEntity(1L, "test")

        whenever(baseRepository.save(entity)).thenReturn(savedEntity)

        val result = repository.save(entity)

        assertEquals(savedEntity, result)
        verify(baseRepository).save(entity)
    }

    @Test
    fun `saveAll delegates to base repository`() {
        val entities = listOf(
            TestEntity(1L, "test1"),
            TestEntity(2L, "test2")
        )

        whenever(baseRepository.save(any<TestEntity>())).thenAnswer { it.arguments[0] }

        val result = repository.saveAll(entities)

        assertEquals(entities, result)
        verify(baseRepository, times(2)).save(any())
    }

    @Test
    fun `findById delegates to base repository`() {
        val entity = TestEntity(1L, "test")

        whenever(baseRepository.findById(1L)).thenReturn(entity)

        val result = repository.findById(1L)

        assertEquals(entity, result)
        verify(baseRepository).findById(1L)
    }

    @Test
    fun `existsById returns true when entity exists`() {
        whenever(baseRepository.findById(1L)).thenReturn(TestEntity(1L, "test"))

        val result = repository.existsById(1L)

        assertTrue(result)
        verify(baseRepository).findById(1L)
    }

    @Test
    fun `existsById returns false when entity does not exist`() {
        whenever(baseRepository.findById(1L)).thenReturn(null)

        val result = repository.existsById(1L)

        assertFalse(result)
        verify(baseRepository).findById(1L)
    }

    @Test
    fun `findAll delegates to base repository`() {
        val entities = listOf(
            TestEntity(1L, "test1"),
            TestEntity(2L, "test2")
        )

        whenever(baseRepository.findAll()).thenReturn(entities)

        val result = repository.findAll()

        assertEquals(entities, result)
        verify(baseRepository).findAll()
    }

    @Test
    fun `count delegates to base repository`() {
        whenever(baseRepository.count()).thenReturn(42L)

        val result = repository.count()

        assertEquals(42L, result)
        verify(baseRepository).count()
    }

    @Test
    fun `deleteById delegates to base repository`() {
        doNothing().whenever(baseRepository).deleteById(1L)

        repository.deleteById(1L)

        verify(baseRepository).deleteById(1L)
    }

    @Test
    fun `delete delegates to base repository`() {
        val entity = TestEntity(1L, "test")

        doNothing().whenever(baseRepository).delete(entity)

        repository.delete(entity)

        verify(baseRepository).delete(entity)
    }

    @Test
    fun `deleteAll with entities delegates to base repository`() {
        val entities = listOf(
            TestEntity(1L, "test1"),
            TestEntity(2L, "test2")
        )

        doNothing().whenever(baseRepository).delete(any<TestEntity>())

        repository.deleteAll(entities)

        verify(baseRepository, times(2)).delete(any())
    }

    @Test
    fun `deleteAll without entities delegates to base repository`() {
        doNothing().whenever(baseRepository).deleteAll()

        repository.deleteAll()

        verify(baseRepository).deleteAll()
    }

    data class TestEntity(val id: Long, val name: String)
}

class SimpleBiTemporalReladomoRepositoryTest {

    private lateinit var biTemporalRepository: BiTemporalRepository<TestBiTemporalEntity, Long>
    private lateinit var transactionTemplate: TransactionTemplate
    private lateinit var repository: SimpleBiTemporalReladomoRepository<TestBiTemporalEntity, Long>

    @BeforeEach
    fun setup() {
        biTemporalRepository = mock()
        transactionTemplate = mock()

        repository = SimpleBiTemporalReladomoRepository(
            biTemporalRepository = biTemporalRepository,
            transactionTemplate = transactionTemplate,
            entityType = TestBiTemporalEntity::class,
            idType = Long::class
        )

        // Default transaction template behavior
        whenever(transactionTemplate.execute<Any>(any())).thenAnswer {
            val callback = it.arguments[0] as TransactionCallback<Any>
            callback.doInTransaction(mock())
        }
    }

    @Test
    fun `findByIdAsOf delegates to base repository`() {
        val businessDate = Instant.now()
        val processingDate = Instant.now()
        val entity = TestBiTemporalEntity(1L, "test", businessDate, processingDate)

        whenever(biTemporalRepository.findByIdAsOf(1L, businessDate, processingDate)).thenReturn(entity)

        val result = repository.findByIdAsOf(1L, businessDate, processingDate)

        assertEquals(entity, result)
        verify(biTemporalRepository).findByIdAsOf(1L, businessDate, processingDate)
    }

    @Test
    fun `findByIdAsOf with default processing date`() {
        val businessDate = Instant.now()
        val entity = TestBiTemporalEntity(1L, "test", businessDate, Instant.now())

        whenever(biTemporalRepository.findByIdAsOf(eq(1L), eq(businessDate), any())).thenReturn(entity)

        val result = repository.findByIdAsOf(1L, businessDate)

        assertEquals(entity, result)
        verify(biTemporalRepository).findByIdAsOf(eq(1L), eq(businessDate), any())
    }

    @Test
    fun `update delegates to base repository`() {
        val businessDate = Instant.now()
        val entity = TestBiTemporalEntity(1L, "test", businessDate, Instant.now())
        val updatedEntity = TestBiTemporalEntity(1L, "updated", businessDate, Instant.now())

        whenever(biTemporalRepository.update(entity, businessDate)).thenReturn(updatedEntity)

        val result = repository.update(entity, businessDate)

        assertEquals(updatedEntity, result)
        verify(biTemporalRepository).update(entity, businessDate)
    }

    @Test
    fun `deleteByIdAsOf delegates to base repository`() {
        val businessDate = Instant.now()

        doNothing().whenever(biTemporalRepository).deleteByIdAsOf(1L, businessDate)

        repository.deleteByIdAsOf(1L, businessDate)

        verify(biTemporalRepository).deleteByIdAsOf(1L, businessDate)
    }

    @Test
    fun `findAllAsOf delegates to base repository`() {
        val businessDate = Instant.now()
        val processingDate = Instant.now()
        val entities = listOf(
            TestBiTemporalEntity(1L, "test1", businessDate, processingDate),
            TestBiTemporalEntity(2L, "test2", businessDate, processingDate)
        )

        whenever(biTemporalRepository.findAllAsOf(businessDate, processingDate)).thenReturn(entities)

        val result = repository.findAllAsOf(businessDate, processingDate)

        assertEquals(entities, result)
        verify(biTemporalRepository).findAllAsOf(businessDate, processingDate)
    }

    @Test
    fun `findAllAsOf with default processing date`() {
        val businessDate = Instant.now()
        val entities = listOf(
            TestBiTemporalEntity(1L, "test1", businessDate, Instant.now())
        )

        whenever(biTemporalRepository.findAllAsOf(eq(businessDate), any())).thenReturn(entities)

        val result = repository.findAllAsOf(businessDate)

        assertEquals(entities, result)
        verify(biTemporalRepository).findAllAsOf(eq(businessDate), any())
    }

    @Test
    fun `getHistory delegates to base repository`() {
        val history = listOf(
            TestBiTemporalEntity(1L, "version1", Instant.now().minusSeconds(3600), Instant.now()),
            TestBiTemporalEntity(1L, "version2", Instant.now(), Instant.now())
        )

        whenever(biTemporalRepository.getHistory(1L)).thenReturn(history)

        val result = repository.getHistory(1L)

        assertEquals(history, result)
        verify(biTemporalRepository).getHistory(1L)
    }

    data class TestBiTemporalEntity(
        val id: Long,
        val name: String,
        override val businessDate: Instant,
        override val processingDate: Instant
    ) : BiTemporalEntity
}
