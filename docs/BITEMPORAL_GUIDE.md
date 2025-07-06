# Understanding Bitemporal Data in Reladomo

## Table of Contents
- [What is Bitemporal Data?](#what-is-bitemporal-data)
- [The Two Time Dimensions](#the-two-time-dimensions)
- [Visual Timeline Examples](#visual-timeline-examples)
- [Common Operations](#common-operations)
- [Query Patterns](#query-patterns)
- [Best Practices](#best-practices)
- [Common Pitfalls](#common-pitfalls)

## What is Bitemporal Data?

Bitemporal data models track changes along two independent time dimensions:
1. **Business Time** (Valid Time): When a fact is true in the real world
2. **Processing Time** (Transaction Time): When a fact is recorded in the database

This allows you to answer questions like:
- "What was the order status on January 1st?" (Business Time)
- "What did we think the order status was when we ran reports on January 15th?" (Processing Time)
- "When did we learn about the status change?" (Processing Time)
- "When did the status actually change?" (Business Time)

## The Two Time Dimensions

### Business Date (Valid Time)
- Represents when something is true in the real world
- Can be in the past, present, or future
- Can be corrected retroactively

### Processing Date (Transaction Time)
- Represents when data was recorded in the system
- Always moves forward (append-only)
- Cannot be changed once recorded
- Uses "infinity" (9999-12-01) for current records

## Visual Timeline Examples

### Example 1: Simple Update

Let's say we have an order created on Jan 1st with status "NEW", then updated to "SHIPPED" on Jan 5th:

```
Business Time:    Jan 1 -------- Jan 5 -----------> ∞
                  [NEW]          [SHIPPED]
                  
Processing Time:  Jan 1 ---------------------------------> ∞
                  (recorded)
```

**Query Results:**
- `findAsOf(Jan 3)` → Status: "NEW"
- `findAsOf(Jan 7)` → Status: "SHIPPED"
- `findCurrent()` → Status: "SHIPPED"

### Example 2: Retroactive Correction

Order was shipped on Jan 5th, but we didn't record it until Jan 10th:

```
Business Time:    Jan 1 -------- Jan 5 -----------> ∞
                  [NEW]          [SHIPPED]
                  
Processing Time:  Jan 1 -------- Jan 10 -----------> ∞
                  (recorded NEW) (recorded SHIPPED)
```

**Query Results:**
- `findAsOf(businessDate=Jan 7, processingDate=Jan 8)` → Status: "NEW" (we didn't know yet!)
- `findAsOf(businessDate=Jan 7, processingDate=Jan 12)` → Status: "SHIPPED" (now we know)

### Example 3: Historical Correction

We discover on Jan 15th that the order was actually shipped on Jan 3rd, not Jan 5th:

```
BEFORE CORRECTION:
Business Time:    Jan 1 -------- Jan 5 -----------> ∞
                  [NEW]          [SHIPPED]

AFTER CORRECTION:
Business Time:    Jan 1 --- Jan 3 ---------------> ∞
                  [NEW]     [SHIPPED]
                  
Processing Time Track:
- Jan 1: Recorded NEW
- Jan 10: Recorded SHIPPED as of Jan 5
- Jan 15: Corrected SHIPPED to Jan 3
```

This creates multiple versions in the database:

| OrderId | Status  | Business_From | Business_Thru | Processing_From | Processing_Thru |
|---------|---------|---------------|---------------|-----------------|-----------------|
| 1       | NEW     | Jan 1         | Jan 5         | Jan 1          | Jan 15          |
| 1       | SHIPPED | Jan 5         | ∞             | Jan 10         | Jan 15          |
| 1       | NEW     | Jan 1         | Jan 3         | Jan 15         | ∞               |
| 1       | SHIPPED | Jan 3         | ∞             | Jan 15         | ∞               |

## Common Operations

### 1. Create a New Record
```kotlin
val order = OrderKt(
    orderId = 123,
    status = "NEW",
    businessDate = Instant.now(),        // When it's valid from
    processingDate = infinityDate        // Current version
)
repository.save(order)
```

### 2. Update Current Record (Same Business Date)
```kotlin
// This updates the record in-place
val updated = existingOrder.copy(status = "PROCESSING")
repository.update(updated, existingOrder.businessDate)
```

### 3. Update with New Business Date
```kotlin
// This creates a new version, preserving history
val updated = existingOrder.copy(
    status = "SHIPPED",
    businessDate = Instant.now()  // New business date
)
repository.update(updated, existingOrder.businessDate)
```

### 4. Query Current State
```kotlin
// Find current version (latest business date, active processing date)
val current = repository.findById(123)
```

### 5. Query Historical State
```kotlin
// Find as of specific business date
val historical = repository.findByIdAsOf(
    id = 123,
    businessDate = parseDate("2024-01-05"),
    processingDate = infinityDate  // Current processing view
)
```

### 6. Query What We Knew At a Point in Time
```kotlin
// Find what we knew on Jan 10th about Jan 5th
val pointInTime = repository.findByIdAsOf(
    id = 123,
    businessDate = parseDate("2024-01-05"),
    processingDate = parseDate("2024-01-10")
)
```

## Query Patterns

### Finding Current Records
```kotlin
// DON'T use equalsEdgePoint() - it can return multiple results
// DO use findByPrimaryKey with current time and infinity
val now = Timestamp.from(Instant.now())
val infinity = Timestamp.valueOf("9999-12-01 23:59:00.0")
val current = OrderFinder.findByPrimaryKey(id, now, infinity)
```

### Finding Historical Records
```kotlin
// Find record as it was on a specific business date
val historicalDate = Timestamp.from(businessDate)
val infinity = Timestamp.valueOf("9999-12-01 23:59:00.0")
val historical = OrderFinder.findByPrimaryKey(id, historicalDate, infinity)
```

### Updating Records
```kotlin
// Always fetch as of the business date you want to update
val recordToUpdate = OrderFinder.findByPrimaryKey(id, businessDateTs, infinityTs)
recordToUpdate.setStatus("UPDATED")
// Reladomo handles creating new versions automatically
```

## Best Practices

### 1. Always Use Infinity for Current Processing Date
```kotlin
val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
```

### 2. Fetch Before Update
Always fetch the record as of the business date you want to update:
```kotlin
// GOOD: Fetch as of specific business date
val existing = repository.findByPrimaryKey(id, businessDate, infinity)
existing.setStatus(newStatus)

// BAD: Using equalsEdgePoint can cause issues
val existing = repository.findOne(finder.id().eq(id)
    .and(finder.businessDate().equalsEdgePoint()))
```

### 3. Let Reladomo Handle Versioning
Don't try to manually manage the temporal aspects. Reladomo automatically:
- Sets proper FROM/THRU dates
- Creates new versions when needed
- Maintains referential integrity

### 4. Use Business Dates That Make Sense
- Use the actual date when something happened in the real world
- Don't use "now" unless the change is actually happening now
- Consider timezone implications

## Common Pitfalls

### 1. Confusing Business and Processing Time
- **Business Time**: When did the order ship? (Jan 5th)
- **Processing Time**: When did we record that it shipped? (Jan 10th)

### 2. Using equalsEdgePoint() Incorrectly
`equalsEdgePoint()` can return multiple records if there are multiple versions. Use `findByPrimaryKey` instead.

### 3. Not Preserving History
When updating with a different business date, make sure to:
- Fetch the record as of its current business date
- Update with the new business date
- Let Reladomo create the new version

### 4. Forgetting Infinity for Processing Date
Current records always have processing_thru = infinity (9999-12-01). Never use "now" for processing_thru.

## Visual Summary

```
Traditional (Non-temporal):
    [Current State Only]

Temporal (Single Time):
    Past ----[State 1]----[State 2]----[Current]----> Future

Bitemporal (Two Times):
    Business Time →
    ↓ Processing Time
    
    What actually happened →
    ├─ NEW (Jan 1) ─────┬─ SHIPPED (Jan 5) ─────→ ∞
    │                   │
    ↓ When we knew it   │
    Jan 1: NEW ─────────┤
    Jan 10: ────────────┴─ SHIPPED ─────────────→ ∞
```

## Practical Example: Order Status Tracking

Consider an e-commerce order that goes through multiple status changes:

1. **Jan 1, 10:00** - Customer places order (status: NEW)
2. **Jan 2, 14:00** - Payment processed (status: PAID)
3. **Jan 3, 09:00** - Order shipped (status: SHIPPED)
4. **Jan 5, 11:00** - Customer reports non-delivery
5. **Jan 5, 15:00** - Investigation reveals order was actually shipped Jan 4, not Jan 3

Here's how this looks in the bitemporal model:

```
Business Timeline:
Jan 1 ─[NEW]─ Jan 2 ─[PAID]─ Jan 4 ─[SHIPPED]─→ ∞
                              ↑
                      (Corrected from Jan 3)

Processing Timeline:
- Jan 1, 10:00: Recorded NEW
- Jan 2, 14:00: Recorded PAID
- Jan 3, 09:00: Recorded SHIPPED (thought it was Jan 3)
- Jan 5, 15:00: Corrected SHIPPED to Jan 4
```

**Queries:**
- "Current status?" → SHIPPED (since Jan 4)
- "What did we think on Jan 3 at noon?" → SHIPPED (we thought since Jan 3)
- "When did it actually ship?" → Jan 4 (corrected information)
- "When did we first think it shipped?" → Jan 3, 09:00 (processing time)

This bitemporal model is powerful for:
- Audit trails
- Compliance reporting
- Historical analysis
- Error corrections without losing history
- "As-of" date reporting