# Kotlin Reladomo Query DSL Demo

This document demonstrates the Query DSL functionality that has been implemented for the Kotlin Reladomo wrapper.

## Overview

The Query DSL provides a type-safe, Kotlin-idiomatic way to build queries for Reladomo entities. It leverages Kotlin's language features to create an intuitive API for querying bitemporal data.

## Key Features

1. **Type-safe queries** - All queries are checked at compile time
2. **Kotlin DSL syntax** - Uses infix functions and property extensions
3. **Bitemporal support** - First-class support for temporal queries
4. **Auto-generated** - DSL extensions are generated from Reladomo XML

## Example Usage

### Basic Queries

```kotlin
// Find orders by customer ID
val orders = orderRepository.find {
    customerId eq 100
}

// Find orders by status
val pendingOrders = orderRepository.find {
    status eq "PENDING"
}

// Find orders with amount greater than 200
val largeOrders = orderRepository.find {
    amount greaterThan BigDecimal("200.00")
}
```

### Complex Queries

```kotlin
// Multiple conditions (automatically ANDed)
val complexQuery = orderRepository.find {
    customerId eq 100
    status eq "PENDING"
    amount between (BigDecimal("100.00") to BigDecimal("500.00"))
}

// IN queries
val multiCustomerOrders = orderRepository.find {
    customerId inList listOf(100L, 200L, 300L)
}

// String operations
val descriptionQuery = orderRepository.find {
    description contains "urgent"
    status notEq "CANCELLED"
}
```

### Temporal Queries

```kotlin
// Find orders as of a specific date
val historicalOrders = orderRepository.find {
    businessDate asOf Instant.parse("2024-01-01T00:00:00Z")
    customerId eq 100
}

// Edge point queries (current data)
val currentOrders = orderRepository.find {
    businessDate equalsEdgePoint()
    processingDate equalsEdgePoint()
}
```

### Repository Methods

The generated repositories provide several DSL-enabled methods:

```kotlin
// Find multiple results
val orders = orderRepository.find {
    customerId eq 100
}

// Find single result
val order = orderRepository.findOne {
    orderId eq 12345
}

// Count matching records
val count = orderRepository.count {
    status eq "PENDING"
}

// Check existence
val hasOrders = orderRepository.exists {
    customerId eq 100
}
```

## Generated Code Structure

For each Reladomo entity, the following is generated:

1. **Entity-specific Query DSL** (e.g., `OrderQueryDsl.kt`)
   - Extension properties for each attribute
   - Type-specific operators

2. **Repository with DSL methods** (e.g., `OrderKtRepository.kt`)
   - `find`, `findOne`, `count`, `exists` methods
   - Automatic DSL import

3. **Kotlin wrapper entity** (e.g., `OrderKt.kt`)
   - Data class with proper Kotlin types
   - Conversion methods to/from Reladomo

## Implementation Details

### Query Context

All queries run within a `QueryContext` that collects operations:

```kotlin
class QueryContext : QueryBuilder {
    private val operations = mutableListOf<Operation>()
    
    fun addOperation(operation: Operation) {
        operations.add(operation)
    }
    
    override fun build(): Operation {
        return when (operations.size) {
            0 -> throw IllegalStateException("No query conditions specified")
            1 -> operations.first()
            else -> operations.reduce { acc, operation -> acc.and(operation) }
        }
    }
}
```

### Type-Safe Properties

Each attribute type has specialized property classes:

- `NumericAttributeProperty` - For numeric types with comparison operators
- `StringAttributeProperty` - For strings with text operations
- `TemporalAttributeProperty` - For dates/timestamps
- `AsOfAttributeProperty` - For bitemporal queries

### DSL Marker

The `@QueryDslMarker` annotation prevents scope leakage:

```kotlin
@DslMarker
annotation class QueryDslMarker
```

## Benefits

1. **Compile-time safety** - Typos and type mismatches caught early
2. **IDE support** - Full auto-completion and documentation
3. **Readable code** - Queries read like natural language
4. **Maintainable** - Changes to schema automatically reflected
5. **Bitemporal-first** - Temporal queries are easy and intuitive

## Next Steps

The Query DSL is now fully integrated into the Kotlin Reladomo wrapper. Future enhancements could include:

1. OR operations support
2. Subquery support
3. Aggregate functions
4. Join operations
5. Custom operators