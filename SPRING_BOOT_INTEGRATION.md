# Spring Boot Integration for Reladomo Kotlin

This document describes the comprehensive Spring Boot integration features available in Reladomo Kotlin.

## Overview

Reladomo Kotlin provides seamless Spring Boot integration with auto-configuration, Spring Data-style repositories, and query method support. It follows Spring conventions to make Reladomo feel like a native part of the Spring ecosystem.

## Features

### 1. Enhanced Auto-Configuration

#### Multi-DataSource Support

Configure multiple datasources for different entities:

```yaml
reladomo:
  kotlin:
    datasources:
      primary:
        url: jdbc:postgresql://localhost:5432/orders_db
        username: orders_user
        password: orders_pass
        connection-pool:
          min-size: 5
          max-size: 20
      
      customer:
        url: jdbc:postgresql://localhost:5432/customers_db
        username: customers_user
        password: customers_pass
```

#### Cache Configuration

Choose between different caching strategies:

```yaml
reladomo:
  kotlin:
    cache:
      type: PARTIAL          # Options: FULL, PARTIAL, NONE
      timeout: 3600          # Seconds (for PARTIAL cache)
      max-size: 10000        # Maximum objects in cache
      relationship-timeout: 3600
```

### 2. Spring Data-Style Repositories

#### Basic Repository Interface

```kotlin
@Repository
interface OrderRepository : BiTemporalReladomoRepository<OrderKt, Long> {
    // Inherits standard CRUD operations
}
```

Available methods:
- `save(entity)` - Save an entity
- `saveAll(entities)` - Save multiple entities
- `findById(id)` - Find by primary key
- `findAll()` - Find all entities
- `existsById(id)` - Check existence
- `count()` - Count all entities
- `deleteById(id)` - Delete by ID
- `delete(entity)` - Delete entity

#### BiTemporal Support

Additional methods for bitemporal entities:
- `findByIdAsOf(id, businessDate, processingDate)`
- `update(entity, businessDate)`
- `deleteByIdAsOf(id, businessDate)`
- `findAllAsOf(businessDate, processingDate)`
- `getHistory(id)`

### 3. Query Method Support

#### Method Name Parsing

Create queries by following Spring Data naming conventions:

```kotlin
@Repository
interface OrderRepository : BiTemporalReladomoRepository<OrderKt, Long> {
    
    // Basic queries
    fun findByCustomerId(customerId: Long): List<OrderKt>
    fun findByStatus(status: String): List<OrderKt>
    fun findByCustomerIdAndStatus(customerId: Long, status: String): List<OrderKt>
    
    // Comparison operators
    fun findByAmountGreaterThan(amount: BigDecimal): List<OrderKt>
    fun findByAmountLessThanEqual(amount: BigDecimal): List<OrderKt>
    fun findByAmountBetween(min: BigDecimal, max: BigDecimal): List<OrderKt>
    
    // String operations
    fun findByDescriptionContaining(keyword: String): List<OrderKt>
    fun findByDescriptionStartingWith(prefix: String): List<OrderKt>
    fun findByDescriptionEndingWith(suffix: String): List<OrderKt>
    
    // IN queries
    fun findByStatusIn(statuses: Collection<String>): List<OrderKt>
    fun findByStatusNotIn(statuses: Collection<String>): List<OrderKt>
    
    // Null handling
    fun findByDescriptionIsNull(): List<OrderKt>
    fun findByDescriptionIsNotNull(): List<OrderKt>
    
    // Count queries
    fun countByStatus(status: String): Long
    fun countByCustomerId(customerId: Long): Long
    
    // Exists queries
    fun existsByCustomerId(customerId: Long): Boolean
    fun existsByStatus(status: String): Boolean
    
    // Delete queries
    fun deleteByStatus(status: String)
    fun deleteByCustomerId(customerId: Long)
    
    // Limiting results
    fun findFirst5ByStatus(status: String): List<OrderKt>
    fun findTop10ByCustomerIdOrderByAmountDesc(customerId: Long): List<OrderKt>
    
    // Complex queries
    fun findByCustomerIdAndAmountGreaterThanAndStatusIn(
        customerId: Long,
        amount: BigDecimal,
        statuses: Collection<String>
    ): List<OrderKt>
}
```

#### Supported Keywords

**Query Types:**
- `find...By`, `read...By`, `get...By`, `query...By`, `search...By`
- `count...By`
- `exists...By`
- `delete...By`, `remove...By`

**Conditions:**
- `Is`, `Equals` - Equality
- `Not`, `NotEquals` - Inequality
- `LessThan`, `LessThanEqual`, `GreaterThan`, `GreaterThanEqual`
- `Between` - Range queries
- `In`, `NotIn` - Collection membership
- `Like`, `NotLike` - Pattern matching
- `StartingWith`, `StartsWith`, `EndingWith`, `EndsWith`, `Containing`, `Contains`
- `IsNull`, `IsNotNull`, `NotNull`
- `True`, `False` - Boolean values

**Modifiers:**
- `First`, `Top` - Limit results
- `Distinct` - Unique results
- `OrderBy` - Sorting (with `Asc` or `Desc`)

**Logical Operators:**
- `And` - Combine conditions with AND
- `Or` - Combine conditions with OR

### 4. Repository Scanning

Enable repository scanning with `@EnableReladomoRepositories`:

```kotlin
@SpringBootApplication
@EnableReladomoRepositories(
    basePackages = ["com.example.repository"],
    basePackageClasses = [OrderRepository::class]
)
class Application
```

### 5. Transaction Support

Repositories are automatically transactional:

```kotlin
@Service
class OrderService(private val orderRepository: OrderRepository) {
    
    @Transactional
    fun processOrder(order: OrderKt) {
        orderRepository.save(order)
        // Additional business logic
        // All operations participate in the same transaction
    }
}
```

## Configuration Reference

### application.yml

```yaml
reladomo:
  kotlin:
    # Core settings
    connection-manager-config-file: reladomo-runtime-config.xml
    database-time-zone: UTC
    enable-debug-logging: false
    initialize-on-startup: true
    default-transaction-timeout: 120
    
    # Cache configuration
    cache:
      type: PARTIAL          # FULL, PARTIAL, NONE
      timeout: 3600
      max-size: 10000
      relationship-timeout: 3600
    
    # Multi-datasource
    datasources:
      datasource-name:
        url: jdbc:url
        username: user
        password: pass
        driver-class-name: driver.class
        connection-pool:
          min-size: 5
          max-size: 20
          connection-timeout: 30000
          idle-timeout: 600000
    
    # Repository settings
    repository:
      base-packages:
        - com.example.repository
      enable-query-methods: true
      naming-strategy: DEFAULT
```

## Getting Started

1. Add the dependency:
```kotlin
implementation("io.github.reladomokotlin:reladomo-kotlin-spring-boot:1.0.0")
```

2. Create your repository interface:
```kotlin
@Repository
interface CustomerRepository : ReladomoRepository<Customer, Long> {
    fun findByEmail(email: String): Customer?
    fun findByLastNameOrderByFirstName(lastName: String): List<Customer>
}
```

3. Enable repositories:
```kotlin
@SpringBootApplication
@EnableReladomoRepositories
class MyApplication
```

4. Use in your services:
```kotlin
@Service
class CustomerService(
    private val customerRepository: CustomerRepository
) {
    fun getCustomerByEmail(email: String) = 
        customerRepository.findByEmail(email)
}
```

## Benefits

1. **Familiar API** - Uses Spring Data conventions
2. **Zero Boilerplate** - No implementation needed for standard queries
3. **Type-Safe** - Compile-time checking of query methods
4. **Flexible** - Supports complex queries through method names
5. **Performant** - Leverages Reladomo's caching and optimization
6. **Bitemporal-Ready** - First-class support for temporal queries

## Future Enhancements

- `@Query` annotation support for custom queries
- Pagination and sorting support
- Specifications/Criteria API
- Async/reactive repository methods
- GraphQL integration