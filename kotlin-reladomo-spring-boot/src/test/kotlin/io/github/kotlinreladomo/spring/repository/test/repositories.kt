package io.github.kotlinreladomo.spring.repository.test

import io.github.kotlinreladomo.spring.repository.BiTemporalReladomoRepository
import io.github.kotlinreladomo.spring.repository.ReladomoRepository
import org.springframework.stereotype.Repository

@Repository
interface TestOrderRepository : BiTemporalReladomoRepository<TestOrder, Long> {
    fun findByCustomerId(customerId: Long): List<TestOrder>
    fun findByStatus(status: String): List<TestOrder>
    fun countByStatus(status: String): Long
    fun existsByCustomerId(customerId: Long): Boolean
}

@Repository
interface TestCustomerRepository : ReladomoRepository<TestCustomer, Long> {
    fun findByEmail(email: String): TestCustomer?
    fun findByLastName(lastName: String): List<TestCustomer>
}

@Repository
interface TestProductRepository : ReladomoRepository<TestProduct, String> {
    fun findByCategory(category: String): List<TestProduct>
    fun findByPriceGreaterThan(price: Double): List<TestProduct>
}