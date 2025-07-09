# Annotation-Based Configuration Guide

This guide covers the annotation-based entity configuration feature introduced in Reladomo Kotlin, which provides a more idiomatic Kotlin approach to defining Reladomo entities without XML.

## Overview

The annotation-based configuration allows you to:
- Define entities using Kotlin data classes with annotations
- Automatically discover entities at runtime
- Eliminate XML configuration files
- Leverage IDE support for code completion and validation
- Maintain type safety throughout your domain model

## Basic Entity Definition

### Simple Entity

```kotlin
@ReladomoEntity
data class Customer(
    @PrimaryKey
    val customerId: Long? = null,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = false)
    val email: String,
    
    @Column
    val phone: String? = null
)
```

### Entity with Custom Table Name

```kotlin
@ReladomoEntity(tableName = "CUSTOMER_ACCOUNTS")
data class CustomerAccount(
    @PrimaryKey(columnName = "ACCOUNT_ID")
    val id: Long? = null,
    
    @Column(name = "ACCOUNT_NAME", nullable = false, length = 100)
    val accountName: String,
    
    @Column(name = "BALANCE", nullable = false)
    val balance: BigDecimal
)
```

## Bitemporal Entities

### Basic Bitemporal Entity

```kotlin
@ReladomoEntity(
    tableName = "ORDERS",
    bitemporal = true
)
data class Order(
    @PrimaryKey(columnName = "ORDER_ID")
    val orderId: Long? = null,
    
    @Column(name = "CUSTOMER_ID", nullable = false)
    val customerId: Long,
    
    @Column(name = "AMOUNT", nullable = false)
    val amount: BigDecimal,
    
    @Column(name = "STATUS", nullable = false)
    val status: String,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now()
)
```

### Custom Temporal Column Names

```kotlin
@ReladomoEntity(bitemporal = true)
data class PriceHistory(
    @PrimaryKey
    val priceId: Long? = null,
    
    @Column(nullable = false)
    val productId: Long,
    
    @Column(nullable = false)
    val price: BigDecimal,
    
    @BusinessDate(
        fromColumn = "PRICE_VALID_FROM",
        thruColumn = "PRICE_VALID_THRU"
    )
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate(
        fromColumn = "PRICE_PROC_FROM",
        thruColumn = "PRICE_PROC_THRU"
    )
    val processingDate: Instant = Instant.now()
)
```

## Relationships

### One-to-One Relationship

```kotlin
@ReladomoEntity
data class User(
    @PrimaryKey
    val userId: Long? = null,
    
    @Column(nullable = false)
    val username: String,
    
    @Relationship(
        targetEntity = "com.example.UserProfile",
        expression = "this.userId = UserProfile.userId"
    )
    val profile: UserProfile? = null
)

@ReladomoEntity
data class UserProfile(
    @PrimaryKey
    val profileId: Long? = null,
    
    @Column(nullable = false)
    val userId: Long,
    
    @Column
    val bio: String? = null
)
```

### One-to-Many Relationship

```kotlin
@ReladomoEntity(tableName = "CUSTOMERS")
data class Customer(
    @PrimaryKey
    val customerId: Long? = null,
    
    @Column(nullable = false)
    val name: String,
    
    @Relationship(
        targetEntity = "com.example.Order",
        expression = "this.customerId = Order.customerId",
        oneToMany = true
    )
    val orders: List<Order>? = null
)
```

### Many-to-Many Relationship (via Join Table)

```kotlin
@ReladomoEntity
data class Student(
    @PrimaryKey
    val studentId: Long? = null,
    
    @Column(nullable = false)
    val name: String,
    
    @Relationship(
        targetEntity = "com.example.Course",
        expression = "this.studentId = StudentCourse.studentId and StudentCourse.courseId = Course.courseId",
        oneToMany = true
    )
    val courses: List<Course>? = null
)

@ReladomoEntity
data class Course(
    @PrimaryKey
    val courseId: Long? = null,
    
    @Column(nullable = false)
    val courseName: String,
    
    @Relationship(
        targetEntity = "com.example.Student",
        expression = "this.courseId = StudentCourse.courseId and StudentCourse.studentId = Student.studentId",
        oneToMany = true
    )
    val students: List<Student>? = null
)

@ReladomoEntity(tableName = "STUDENT_COURSES")
data class StudentCourse(
    @PrimaryKey
    val id: Long? = null,
    
    @Column(nullable = false)
    val studentId: Long,
    
    @Column(nullable = false)
    val courseId: Long
)
```

## Cache Configuration

### Entity-Level Cache Settings

```kotlin
// Full cache - all records cached in memory
@ReladomoEntity(
    tableName = "REFERENCE_DATA",
    cacheType = CacheType.FULL
)
data class ReferenceData(
    @PrimaryKey
    val id: Long? = null,
    
    @Column(nullable = false)
    val code: String,
    
    @Column(nullable = false)
    val description: String
)

// Partial cache - frequently accessed records cached
@ReladomoEntity(
    tableName = "PRODUCTS",
    cacheType = CacheType.PARTIAL
)
data class Product(
    @PrimaryKey
    val productId: Long? = null,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = false)
    val price: BigDecimal
)

// No cache - for large or infrequently accessed data
@ReladomoEntity(
    tableName = "AUDIT_LOGS",
    cacheType = CacheType.NONE
)
data class AuditLog(
    @PrimaryKey
    val logId: Long? = null,
    
    @Column(nullable = false)
    val action: String,
    
    @Column(nullable = false)
    val timestamp: Instant
)
```

## Connection Manager Configuration

### Using Different Data Sources

```kotlin
// Default connection manager
@ReladomoEntity
data class User(
    @PrimaryKey
    val userId: Long? = null,
    
    @Column(nullable = false)
    val username: String
)

// Custom connection manager
@ReladomoEntity(
    tableName = "AUDIT_LOGS",
    connectionManager = "auditDB"
)
data class AuditLog(
    @PrimaryKey
    val logId: Long? = null,
    
    @Column(nullable = false)
    val action: String
)
```

Configure multiple data sources in `application.yml`:

```yaml
reladomo:
  kotlin:
    datasources:
      default:
        url: jdbc:postgresql://localhost:5432/main
        username: app_user
        password: secret
      auditDB:
        url: jdbc:postgresql://localhost:5432/audit
        username: audit_user
        password: secret
```

## Column Mappings

### Supported Data Types

```kotlin
@ReladomoEntity
data class DataTypesExample(
    @PrimaryKey
    val id: Long? = null,
    
    // Numeric types
    @Column
    val intValue: Int,
    
    @Column
    val longValue: Long,
    
    @Column
    val doubleValue: Double,
    
    @Column
    val bigDecimalValue: BigDecimal,
    
    // String types
    @Column(length = 50)
    val shortString: String,
    
    @Column(length = 4000)
    val longString: String,
    
    // Date/Time types
    @Column
    val instant: Instant,
    
    @Column
    val localDate: LocalDate,
    
    @Column
    val localDateTime: LocalDateTime,
    
    // Boolean
    @Column
    val isActive: Boolean,
    
    // Nullable columns
    @Column(nullable = true)
    val optionalValue: String? = null
)
```

## Best Practices

### 1. Nullable Primary Keys

Always make primary keys nullable for auto-generated IDs:

```kotlin
@ReladomoEntity
data class Entity(
    @PrimaryKey
    val id: Long? = null,  // Nullable for auto-generation
    
    @Column(nullable = false)
    val name: String
)
```

### 2. Default Values

Provide sensible defaults for temporal fields:

```kotlin
@ReladomoEntity(bitemporal = true)
data class BiTemporalEntity(
    @PrimaryKey
    val id: Long? = null,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now()
)
```

### 3. Immutable Data Classes

Use `val` properties for immutability:

```kotlin
@ReladomoEntity
data class ImmutableEntity(
    @PrimaryKey
    val id: Long? = null,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
```

### 4. Package Organization

Organize entities by domain:

```
com.example.domain/
├── customer/
│   ├── Customer.kt
│   ├── CustomerAccount.kt
│   └── CustomerRepository.kt
├── order/
│   ├── Order.kt
│   ├── OrderItem.kt
│   └── OrderRepository.kt
└── product/
    ├── Product.kt
    ├── ProductCategory.kt
    └── ProductRepository.kt
```

## Migration from XML

### Before (XML):

```xml
<MithraObject objectType="transactional">
    <PackageName>com.example.domain</PackageName>
    <ClassName>Customer</ClassName>
    <DefaultTable>CUSTOMERS</DefaultTable>
    
    <Attribute name="customerId" javaType="long" 
               columnName="CUSTOMER_ID" primaryKey="true"/>
    <Attribute name="name" javaType="String" 
               columnName="NAME" nullable="false" maxLength="100"/>
    <Attribute name="email" javaType="String" 
               columnName="EMAIL" nullable="false" maxLength="200"/>
    <Attribute name="createdDate" javaType="Timestamp" 
               columnName="CREATED_DATE"/>
</MithraObject>
```

### After (Annotation):

```kotlin
package com.example.domain

@ReladomoEntity(tableName = "CUSTOMERS")
data class Customer(
    @PrimaryKey(columnName = "CUSTOMER_ID")
    val customerId: Long? = null,
    
    @Column(name = "NAME", nullable = false, length = 100)
    val name: String,
    
    @Column(name = "EMAIL", nullable = false, length = 200)
    val email: String,
    
    @Column(name = "CREATED_DATE")
    val createdDate: Instant? = null
)
```

## Troubleshooting

### Common Issues

1. **Entity Not Found**
   - Ensure the package is included in `repository.base-packages`
   - Verify the class has `@ReladomoEntity` annotation
   - Check for compilation errors

2. **Column Mapping Errors**
   - Verify column names match database schema
   - Check nullable settings match database constraints
   - Ensure data types are compatible

3. **Relationship Issues**
   - Verify the expression syntax is correct
   - Check that referenced entities exist
   - Ensure foreign key columns are properly defined

### Debug Tips

Enable debug logging to see entity discovery:

```yaml
logging:
  level:
    io.github.reladomokotlin.spring.scanner: DEBUG
    io.github.reladomokotlin.spring.config: DEBUG
```

## See Also

- [Spring Boot Integration Guide](spring-boot-integration.md)
- [Repository Pattern Guide](repository-pattern-guide.md)
- [Bitemporal Guide](../BITEMPORAL_GUIDE.md)