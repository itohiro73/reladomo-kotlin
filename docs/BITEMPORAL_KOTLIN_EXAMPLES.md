# Kotlin Reladomo Bitemporal Examples

This guide provides practical code examples for working with bitemporal data in the Kotlin Reladomo wrapper.

## Table of Contents
- [Setup](#setup)
- [Basic CRUD Operations](#basic-crud-operations)
- [Bitemporal Queries](#bitemporal-queries)
- [Update Patterns](#update-patterns)
- [Common Scenarios](#common-scenarios)
- [Testing Bitemporal Logic](#testing-bitemporal-logic)

## Setup

### Constants
```kotlin
// Always use this for current processing date
val INFINITY_DATE = Instant.parse("9999-12-01T23:59:00Z")
```

### Entity Definition
```kotlin
data class OrderKt(
    val orderId: Long?,
    val customerId: Long,
    val status: String,
    val amount: BigDecimal,
    override val businessDate: Instant,      // When the data is valid
    override val processingDate: Instant     // When we recorded it
) : BiTemporalEntity
```

## Basic CRUD Operations

### Create a New Order
```kotlin
@Test
fun `create new order`() {
    val now = Instant.now()
    
    val order = OrderKt(
        orderId = null,  // Let database generate ID
        customerId = 123L,
        status = "NEW",
        amount = BigDecimal("99.99"),
        businessDate = now,           // Valid from now
        processingDate = INFINITY_DATE // Current version
    )
    
    val saved = repository.save(order)
    assertNotNull(saved.orderId)
    assertEquals("NEW", saved.status)
}
```

### Find Current Order
```kotlin
@Test
fun `find current order state`() {
    // This finds the most recent version
    val current = repository.findById(orderId)
    
    assertNotNull(current)
    println("Current status: ${current.status}")
    println("Valid since: ${current.businessDate}")
}
```

### Update Order (Same Business Date)
```kotlin
@Test
fun `update order in place`() {
    val order = repository.findById(orderId)!!
    
    // Update with same business date - modifies existing record
    val updated = order.copy(
        status = "PROCESSING",
        amount = order.amount.multiply(BigDecimal("0.9")) // 10% discount
    )
    
    repository.update(updated, order.businessDate)
    
    // Verify update
    val current = repository.findById(orderId)
    assertEquals("PROCESSING", current?.status)
}
```

### Delete Order
```kotlin
@Test
fun `delete order`() {
    val order = repository.findById(orderId)!!
    
    // For bitemporal, this "terminates" the record
    repository.deleteById(orderId, order.businessDate)
    
    // Record no longer visible in current view
    assertNull(repository.findById(orderId))
    
    // But still exists in history
    val historical = repository.findByIdAsOf(
        orderId, 
        order.businessDate, 
        INFINITY_DATE
    )
    assertNotNull(historical)
}
```

## Bitemporal Queries

### Query As Of Specific Date
```kotlin
@Test
fun `find order as of specific date`() {
    val orderId = 123L
    val checkDate = Instant.parse("2024-01-05T10:00:00Z")
    
    // Find order as it was on Jan 5th
    val historical = repository.findByIdAsOf(
        id = orderId,
        businessDate = checkDate,
        processingDate = INFINITY_DATE  // Current processing view
    )
    
    println("Order status on Jan 5th: ${historical?.status}")
}
```

### Query What We Knew At a Point in Time
```kotlin
@Test
fun `find what we knew on specific date`() {
    val orderId = 123L
    val businessDate = Instant.parse("2024-01-05T00:00:00Z")
    val processingDate = Instant.parse("2024-01-10T15:30:00Z")
    
    // What did we think the Jan 5th state was on Jan 10th?
    val pointInTime = repository.findByIdAsOf(
        id = orderId,
        businessDate = businessDate,
        processingDate = processingDate
    )
    
    println("On Jan 10th, we thought Jan 5th status was: ${pointInTime?.status}")
}
```

### Find History of Changes
```kotlin
@Test
fun `track order history`() {
    // Using Reladomo's finder API directly for complex queries
    val orderId = 123L
    
    val operation = OrderFinder.orderId().eq(orderId)
    val allVersions = OrderFinder.findMany(operation)
    
    println("Order history:")
    allVersions
        .sortedBy { it.businessDate }
        .forEach { order ->
            println("${order.businessDate}: ${order.status} " +
                   "(recorded: ${order.processingDate})")
        }
}
```

## Update Patterns

### Pattern 1: Correct Current Data
```kotlin
@Test
fun `correct current order amount`() {
    val order = repository.findById(orderId)!!
    
    // Fix amount, keep same business date
    val corrected = order.copy(amount = BigDecimal("149.99"))
    repository.update(corrected, order.businessDate)
    
    // History shows the correction
    val current = repository.findById(orderId)
    assertEquals(BigDecimal("149.99"), current?.amount)
}
```

### Pattern 2: Record State Change
```kotlin
@Test
fun `record order shipment`() {
    val order = repository.findById(orderId)!!
    val shipmentTime = Instant.now()
    
    // Create new version with new business date
    val shipped = order.copy(
        status = "SHIPPED",
        businessDate = shipmentTime  // When it actually shipped
    )
    
    // Fetch existing and update with new business date
    repository.update(shipped, order.businessDate)
    
    // Now we have history
    val beforeShipment = repository.findByIdAsOf(
        orderId, 
        shipmentTime.minusSeconds(60),
        INFINITY_DATE
    )
    assertEquals("PROCESSING", beforeShipment?.status)
    
    val afterShipment = repository.findById(orderId)
    assertEquals("SHIPPED", afterShipment?.status)
}
```

### Pattern 3: Retroactive Correction
```kotlin
@Test
fun `correct historical data`() {
    val orderId = 123L
    
    // Discover order was actually shipped earlier than recorded
    val actualShipDate = Instant.parse("2024-01-03T14:00:00Z")
    val recordedShipDate = Instant.parse("2024-01-05T10:00:00Z")
    
    // Find the order that needs correction
    val order = repository.findByIdAsOf(
        orderId,
        recordedShipDate,
        INFINITY_DATE
    )!!
    
    // Update with correct business date
    val corrected = order.copy(
        businessDate = actualShipDate  // Correct ship date
    )
    
    // This creates new versions with corrected timeline
    repository.update(corrected, recordedShipDate)
    
    // Verify correction
    val historical = repository.findByIdAsOf(
        orderId,
        actualShipDate.plusSeconds(3600),  // 1 hour after actual ship
        INFINITY_DATE
    )
    assertEquals("SHIPPED", historical?.status)
}
```

## Common Scenarios

### Scenario 1: Order Lifecycle
```kotlin
class OrderLifecycleExample {
    
    @Test
    fun `complete order lifecycle`() {
        val startTime = Instant.now()
        
        // 1. Create order
        val newOrder = OrderKt(
            orderId = null,
            customerId = 100L,
            status = "NEW",
            amount = BigDecimal("200.00"),
            businessDate = startTime,
            processingDate = INFINITY_DATE
        )
        val saved = repository.save(newOrder)
        val orderId = saved.orderId!!
        
        // 2. Payment received (1 hour later)
        val paymentTime = startTime.plusSeconds(3600)
        val paidOrder = saved.copy(
            status = "PAID",
            businessDate = paymentTime
        )
        repository.update(paidOrder, saved.businessDate)
        
        // 3. Order shipped (1 day later)
        val shipTime = startTime.plusSeconds(86400)
        val shippedOrder = repository.findById(orderId)!!.copy(
            status = "SHIPPED",
            businessDate = shipTime
        )
        repository.update(shippedOrder, paymentTime)
        
        // 4. Query different points in time
        
        // Just after creation
        val afterCreation = repository.findByIdAsOf(
            orderId,
            startTime.plusSeconds(60),
            INFINITY_DATE
        )
        assertEquals("NEW", afterCreation?.status)
        
        // Just after payment
        val afterPayment = repository.findByIdAsOf(
            orderId,
            paymentTime.plusSeconds(60),
            INFINITY_DATE
        )
        assertEquals("PAID", afterPayment?.status)
        
        // Current state
        val current = repository.findById(orderId)
        assertEquals("SHIPPED", current?.status)
    }
}
```

### Scenario 2: Audit Trail
```kotlin
class AuditTrailExample {
    
    @Test
    fun `generate audit trail`() {
        val orderId = 123L
        
        // Get all versions of the order
        val operation = OrderFinder.orderId().eq(orderId)
        val allVersions = OrderFinder.findMany(operation)
        
        // Group by processing date to see changes
        val auditTrail = allVersions
            .groupBy { it.processingDate }
            .map { (processingDate, orders) ->
                AuditEntry(
                    when = processingDate.toInstant(),
                    changes = orders.map { 
                        "${it.businessDate.toInstant()}: ${it.status}"
                    }
                )
            }
            .sortedBy { it.`when` }
        
        println("Audit Trail for Order $orderId:")
        auditTrail.forEach { entry ->
            println("${entry.`when`}: ${entry.changes}")
        }
    }
    
    data class AuditEntry(
        val `when`: Instant,
        val changes: List<String>
    )
}
```

### Scenario 3: Compliance Reporting
```kotlin
class ComplianceReportingExample {
    
    @Test
    fun `generate as-of-date report`() {
        val reportDate = Instant.parse("2024-01-31T23:59:59Z")
        
        // Find all orders as they were on report date
        val operation = OrderFinder.businessDate()
            .eq(reportDate)
            .and(OrderFinder.processingDate().equalsEdgePoint())
        
        val ordersAsOfDate = OrderFinder.findMany(operation)
        
        // Generate report
        val report = ordersAsOfDate.groupBy { it.status }
            .mapValues { (_, orders) -> 
                Report(
                    count = orders.size,
                    totalAmount = orders.sumOf { it.amount }
                )
            }
        
        println("Order Status Report as of $reportDate:")
        report.forEach { (status, data) ->
            println("$status: ${data.count} orders, total: ${data.totalAmount}")
        }
    }
    
    data class Report(val count: Int, val totalAmount: BigDecimal)
}
```

## Testing Bitemporal Logic

### Test Utilities
```kotlin
object BiTemporalTestUtils {
    
    fun createTestOrder(
        orderId: Long = Random.nextLong(1000, 9999),
        status: String = "NEW",
        businessDate: Instant = Instant.now()
    ): OrderKt {
        return OrderKt(
            orderId = orderId,
            customerId = 100L,
            status = status,
            amount = BigDecimal("99.99"),
            businessDate = businessDate,
            processingDate = INFINITY_DATE
        )
    }
    
    fun assertHistoricalState(
        repository: OrderKtRepository,
        orderId: Long,
        asOfDate: Instant,
        expectedStatus: String
    ) {
        val historical = repository.findByIdAsOf(
            orderId, 
            asOfDate, 
            INFINITY_DATE
        )
        assertEquals(expectedStatus, historical?.status,
            "Expected status $expectedStatus as of $asOfDate")
    }
}
```

### Testing Edge Cases
```kotlin
@Test
fun `test multiple updates on same day`() {
    val orderId = 123L
    val today = Instant.now().truncatedTo(ChronoUnit.DAYS)
    
    // Multiple status changes on same day
    val times = listOf(
        today.plus(2, ChronoUnit.HOURS) to "NEW",
        today.plus(4, ChronoUnit.HOURS) to "PROCESSING",
        today.plus(6, ChronoUnit.HOURS) to "SHIPPED"
    )
    
    // Create versions
    times.forEach { (time, status) ->
        val order = OrderKt(
            orderId = orderId,
            customerId = 100L,
            status = status,
            amount = BigDecimal("99.99"),
            businessDate = time,
            processingDate = INFINITY_DATE
        )
        repository.save(order)
    }
    
    // Verify we can query each state
    times.forEach { (time, expectedStatus) ->
        val result = repository.findByIdAsOf(
            orderId,
            time.plus(30, ChronoUnit.MINUTES),
            INFINITY_DATE
        )
        assertEquals(expectedStatus, result?.status)
    }
}
```

## Best Practices Summary

1. **Always use INFINITY_DATE for current processing date**
2. **Fetch before update** - Get the record as of the business date you want to update
3. **Let Reladomo handle versioning** - Don't manually manage temporal columns
4. **Use meaningful business dates** - Record when things actually happened
5. **Test temporal queries** - Verify your history is preserved correctly
6. **Avoid equalsEdgePoint()** for single record queries - Use findByPrimaryKey instead

## Troubleshooting

### "Multiple results" error
```kotlin
// BAD - can return multiple versions
val order = OrderFinder.findOne(
    OrderFinder.orderId().eq(123)
        .and(OrderFinder.businessDate().equalsEdgePoint())
)

// GOOD - returns specific version
val order = OrderFinder.findByPrimaryKey(
    123, 
    Timestamp.from(businessDate), 
    Timestamp.valueOf("9999-12-01 23:59:00.0")
)
```

### "Business date must not be infinity"
Make sure you're not using INFINITY_DATE for businessDate - only for processingDate:
```kotlin
// BAD
OrderKt(businessDate = INFINITY_DATE, ...)

// GOOD
OrderKt(businessDate = Instant.now(), processingDate = INFINITY_DATE, ...)
```

### Updates not creating history
Make sure you're fetching the existing record and updating it:
```kotlin
// Fetch existing
val existing = repository.findById(orderId)!!

// Update with new business date
val updated = existing.copy(
    status = "NEW_STATUS",
    businessDate = newBusinessDate
)

// Update via repository
repository.update(updated, existing.businessDate)
```