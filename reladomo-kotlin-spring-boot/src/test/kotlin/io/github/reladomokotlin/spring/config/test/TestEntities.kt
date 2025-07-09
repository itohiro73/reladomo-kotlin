package io.github.reladomokotlin.spring.config.test

import io.github.reladomokotlin.spring.repository.ReladomoRepository
import org.springframework.stereotype.Repository

// Test repository interface
@Repository
interface TestEntityRepository : ReladomoRepository<TestEntity, Long> {
    fun findByName(name: String): List<TestEntity>
    fun countByStatus(status: String): Long
    fun existsByName(name: String): Boolean
}

// Test entity
data class TestEntity(
    val id: Long,
    val name: String,
    val status: String
)