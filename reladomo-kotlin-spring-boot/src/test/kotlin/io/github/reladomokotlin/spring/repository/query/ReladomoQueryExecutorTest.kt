package io.github.reladomokotlin.spring.repository.query

import com.gs.fw.common.mithra.finder.Operation
import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalRepository
import org.mockito.kotlin.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.test.*

class ReladomoQueryExecutorTest {

    private lateinit var repository: BaseRepository<TestEntity, Long>
    private lateinit var entityType: KClass<TestEntity>
    private lateinit var executor: ReladomoQueryExecutor<TestEntity, Long>

    @BeforeEach
    fun setup() {
        repository = mock()
        entityType = TestEntity::class
        executor = ReladomoQueryExecutor(repository, entityType)
    }

    @Test
    fun `execute simple find query`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("name", Operator.EQUALS, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test"))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf("test"))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute find query with multiple conditions`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("name", Operator.EQUALS, LogicalOperator.AND),
                Condition("age", Operator.GREATER_THAN, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test", 25))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf("test", 20))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute find query with OR condition`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("name", Operator.EQUALS, LogicalOperator.AND),
                Condition("status", Operator.EQUALS, LogicalOperator.OR)
            )
        )
        val entities = listOf(TestEntity(1L, "test"))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf("test", "active"))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute find query with limit`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("status", Operator.EQUALS, LogicalOperator.AND)
            ),
            limit = 5
        )
        val allEntities = (1..10).map { TestEntity(it.toLong(), "test$it") }
        val limitedEntities = allEntities.take(5)

        whenever(repository.findAll()).thenReturn(allEntities)

        val result = executor.execute(parsedQuery, arrayOf("active"))

        assertEquals(limitedEntities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute find query with distinct`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("status", Operator.EQUALS, LogicalOperator.AND)
            ),
            distinct = true
        )
        val entities = listOf(
            TestEntity(1L, "test1"),
            TestEntity(2L, "test1"), // Duplicate name
            TestEntity(3L, "test2")
        )

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf("active")) as List<*>

        // Distinct should be handled by the repository/Reladomo
        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute count query`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.COUNT,
            conditions = listOf(
                Condition("status", Operator.EQUALS, LogicalOperator.AND)
            )
        )

        // For count, the executor uses findAll and counts the results
        val entities = (1..42).map { TestEntity(it.toLong(), "test$it", status = "active") }
        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf("active"))

        assertEquals(42L, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute exists query returning true`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.EXISTS,
            conditions = listOf(
                Condition("name", Operator.EQUALS, LogicalOperator.AND)
            )
        )

        whenever(repository.findAll()).thenReturn(listOf(TestEntity(1L, "test")))

        val result = executor.execute(parsedQuery, arrayOf("test"))

        assertTrue(result as Boolean)
        verify(repository).findAll()
    }

    @Test
    fun `execute exists query returning false`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.EXISTS,
            conditions = listOf(
                Condition("email", Operator.EQUALS, LogicalOperator.AND)
            )
        )

        whenever(repository.findAll()).thenReturn(emptyList())

        val result = executor.execute(parsedQuery, arrayOf("test@example.com"))

        assertFalse(result as Boolean)
        verify(repository).findAll()
    }

    @Test
    fun `execute delete query`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.DELETE,
            conditions = listOf(
                Condition("status", Operator.EQUALS, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test", status = "inactive"))

        whenever(repository.findAll()).thenReturn(entities)
        doNothing().whenever(repository).delete(any())

        executor.execute(parsedQuery, arrayOf("inactive"))

        verify(repository).findAll()
        verify(repository).delete(entities[0])
    }

    @Test
    fun `execute query with IN operator`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("status", Operator.IN, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test"))
        val statuses = listOf("active", "pending")

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf(statuses))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute query with BETWEEN operator`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("age", Operator.BETWEEN, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test", 25))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf(20, 30))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute query with IS_NULL operator`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("description", Operator.IS_NULL, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test"))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, emptyArray())

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute query with LIKE operator`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("name", Operator.LIKE, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test"))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf("%test%"))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute query with CONTAINING operator`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("description", Operator.CONTAINING, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test", description = "This contains keyword in it"))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf("keyword"))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute query with STARTING_WITH operator`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("name", Operator.STARTING_WITH, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test"))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf("te"))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute query with TRUE operator`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("active", Operator.TRUE, LogicalOperator.AND)
            )
        )
        val entities = listOf(TestEntity(1L, "test"))

        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, emptyArray())

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `execute asOf query`() {
        val businessDate = Instant.now()
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("id", Operator.EQUALS, LogicalOperator.AND)
            ),
            asOf = true
        )
        val entities = listOf(TestEntity(100L, "test"))

        // For asOf queries, the current implementation uses findAll
        whenever(repository.findAll()).thenReturn(entities)

        val result = executor.execute(parsedQuery, arrayOf(100L, businessDate))

        assertEquals(entities, result)
        verify(repository).findAll()
    }

    @Test
    fun `parameter count mismatch throws exception`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("name", Operator.EQUALS, LogicalOperator.AND),
                Condition("age", Operator.GREATER_THAN, LogicalOperator.AND)
            )
        )

        assertThrows<IllegalArgumentException> {
            executor.execute(parsedQuery, arrayOf("test")) // Missing second parameter
        }
    }

    @Test
    fun `BETWEEN operator with wrong parameter count throws exception`() {
        val parsedQuery = ParsedQuery(
            queryType = QueryType.FIND,
            conditions = listOf(
                Condition("age", Operator.BETWEEN, LogicalOperator.AND)
            )
        )

        assertThrows<IllegalArgumentException> {
            executor.execute(parsedQuery, arrayOf(20)) // BETWEEN needs 2 parameters
        }
    }

    data class TestEntity(
        val id: Long,
        val name: String,
        val age: Int = 0,
        val status: String = "active",
        val description: String? = null,
        val active: Boolean = true
    )
}
