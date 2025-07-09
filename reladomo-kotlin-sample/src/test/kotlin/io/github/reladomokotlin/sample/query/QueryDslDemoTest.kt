package io.github.reladomokotlin.sample.query

import io.github.reladomokotlin.sample.domain.kotlin.OrderKt
import io.github.reladomokotlin.sample.domain.kotlin.repository.OrderKtRepository
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.orderId
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.customerId
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.orderDate
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.amount
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.status
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl.businessDate
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * This test demonstrates how the Query DSL would be used with the generated code.
 * Note: This is a demonstration of the intended API, not a working test yet.
 */
@SpringBootTest
class QueryDslDemoTest {
    
    @Autowired
    private lateinit var orderRepository: OrderKtRepository
    
    @Test
    fun `demonstrate Query DSL usage patterns`() {
        // This test shows the intended usage patterns of the Query DSL
        
        println("""
        |Query DSL Usage Examples:
        |
        |1. Simple equality query:
        |   orderRepository.find {
        |       customerId eq 100L
        |   }
        |
        |2. Multiple conditions:
        |   orderRepository.find {
        |       customerId eq 100L
        |       status eq "PENDING"
        |       amount greaterThan BigDecimal("100.00")
        |   }
        |
        |3. Range queries:
        |   orderRepository.find {
        |       amount between (BigDecimal("50.00") to BigDecimal("500.00"))
        |       orderDate between (startDate to endDate)
        |   }
        |
        |4. Collection queries:
        |   orderRepository.find {
        |       status `in` listOf("PENDING", "PROCESSING", "SHIPPED")
        |       customerId notIn listOf(999L, 1000L)
        |   }
        |
        |5. Null checks:
        |   orderRepository.find {
        |       description.isNotNull()
        |   }
        |
        |6. String operations:
        |   orderRepository.find {
        |       description startsWith "URGENT"
        |       status contains "ING"
        |       description wildcard "ORDER-2024-*"
        |   }
        |
        |7. Bitemporal queries:
        |   orderRepository.find {
        |       customerId eq 100L
        |       businessDate.equalsEdgePoint()
        |       processingDate.equalsEdgePoint()
        |   }
        |
        |8. AsOf queries:
        |   orderRepository.findAsOf(businessDate, processingDate) {
        |       customerId eq 100L
        |       status eq "COMPLETED"
        |   }
        |
        |9. Find single entity:
        |   val order = orderRepository.findOne {
        |       orderId eq 123L
        |   }
        |
        |10. Count entities:
        |    val count = orderRepository.count {
        |        status eq "PENDING"
        |        amount greaterThan BigDecimal("1000.00")
        |    }
        |
        |11. Check existence:
        |    val exists = orderRepository.exists {
        |        orderId eq 123L
        |    }
        """.trimMargin())
        
        // For now, just assert that the repository exists
        assertNotNull(orderRepository)
    }
    
    @Test
    fun `demonstrate type-safe Query DSL`() {
        println("""
        |Type Safety in Query DSL:
        |
        |The DSL ensures type safety at compile time:
        |
        |// This compiles:
        |orderRepository.find {
        |    customerId eq 100L         // Long value for Long attribute
        |    amount eq BigDecimal("50") // BigDecimal for BigDecimal attribute
        |    status eq "PENDING"        // String for String attribute
        |}
        |
        |// These would not compile:
        |// customerId eq "100"       // Type mismatch: String vs Long
        |// amount eq 50              // Type mismatch: Int vs BigDecimal
        |// status eq 123             // Type mismatch: Int vs String
        |
        |The DSL also provides appropriate operations for each type:
        |
        |// Numeric operations only available for numeric types:
        |amount greaterThan BigDecimal("100")
        |amount between (BigDecimal("50") to BigDecimal("500"))
        |
        |// String operations only available for string types:
        |status startsWith "PEND"
        |description contains "urgent"
        |
        |// Temporal operations only available for date/time types:
        |orderDate after Instant.now().minusSeconds(3600)
        |orderDate between (yesterday to today)
        """.trimMargin())
        
        assertTrue(true)
    }
}