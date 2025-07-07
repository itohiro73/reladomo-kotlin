# XML to Annotation Migration Guide

This guide helps you migrate existing Reladomo XML configurations to the new annotation-based approach in Kotlin Reladomo.

## Migration Overview

The migration process involves:
1. Converting XML entity definitions to Kotlin data classes with annotations
2. Updating repository configurations
3. Testing to ensure functionality remains intact

## Quick Reference

| XML Element | Annotation Equivalent |
|------------|----------------------|
| `<MithraObject>` | `@ReladomoEntity` |
| `<DefaultTable>` | `@ReladomoEntity(tableName = "...")` |
| `<Attribute primaryKey="true">` | `@PrimaryKey` |
| `<Attribute>` | `@Column` |
| `<AsOfAttribute name="businessDate">` | `@BusinessDate` |
| `<AsOfAttribute name="processingDate">` | `@ProcessingDate` |
| `<Relationship>` | `@Relationship` |

## Step-by-Step Migration

### 1. Basic Entity

**XML:**
```xml
<MithraObject objectType="transactional">
    <PackageName>com.example.domain</PackageName>
    <ClassName>Customer</ClassName>
    <DefaultTable>CUSTOMERS</DefaultTable>
    
    <Attribute name="customerId" javaType="long" 
               columnName="CUSTOMER_ID" primaryKey="true"/>
    <Attribute name="name" javaType="String" 
               columnName="NAME" nullable="false"/>
    <Attribute name="email" javaType="String" 
               columnName="EMAIL" nullable="false"/>
</MithraObject>
```

**Annotation:**
```kotlin
package com.example.domain

@ReladomoEntity(tableName = "CUSTOMERS")
data class Customer(
    @PrimaryKey(columnName = "CUSTOMER_ID")
    val customerId: Long? = null,
    
    @Column(name = "NAME", nullable = false)
    val name: String,
    
    @Column(name = "EMAIL", nullable = false)
    val email: String
)
```

### 2. Bitemporal Entity

**XML:**
```xml
<MithraObject objectType="transactional">
    <PackageName>com.example.domain</PackageName>
    <ClassName>Order</ClassName>
    <DefaultTable>ORDERS</DefaultTable>
    
    <Attribute name="orderId" javaType="long" 
               columnName="ORDER_ID" primaryKey="true"/>
    <Attribute name="customerId" javaType="long" 
               columnName="CUSTOMER_ID"/>
    <Attribute name="amount" javaType="BigDecimal" 
               columnName="AMOUNT"/>
    <Attribute name="status" javaType="String" 
               columnName="STATUS" maxLength="20"/>
    
    <AsOfAttribute name="businessDate" 
                   fromColumnName="BUSINESS_FROM" 
                   toColumnName="BUSINESS_THRU"
                   infinityDate="[com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity()]"/>
    <AsOfAttribute name="processingDate" 
                   fromColumnName="PROCESSING_FROM" 
                   toColumnName="PROCESSING_THRU"
                   infinityDate="[com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity()]"
                   defaultIfNotSpecified="[com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity()]"/>
</MithraObject>
```

**Annotation:**
```kotlin
package com.example.domain

@ReladomoEntity(
    tableName = "ORDERS",
    bitemporal = true
)
data class Order(
    @PrimaryKey(columnName = "ORDER_ID")
    val orderId: Long? = null,
    
    @Column(name = "CUSTOMER_ID")
    val customerId: Long,
    
    @Column(name = "AMOUNT")
    val amount: BigDecimal,
    
    @Column(name = "STATUS", length = 20)
    val status: String,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now()
)
```

### 3. Entity with Relationships

**XML:**
```xml
<MithraObject objectType="transactional">
    <PackageName>com.example.domain</PackageName>
    <ClassName>Customer</ClassName>
    <DefaultTable>CUSTOMERS</DefaultTable>
    
    <Attribute name="customerId" javaType="long" 
               columnName="CUSTOMER_ID" primaryKey="true"/>
    <Attribute name="name" javaType="String" 
               columnName="NAME" nullable="false"/>
    
    <Relationship name="orders" 
                  relatedObject="Order" 
                  cardinality="one-to-many">
        this.customerId = Order.customerId
    </Relationship>
    
    <Relationship name="primaryAddress" 
                  relatedObject="Address" 
                  cardinality="one-to-one">
        this.primaryAddressId = Address.addressId
    </Relationship>
</MithraObject>
```

**Annotation:**
```kotlin
package com.example.domain

@ReladomoEntity(tableName = "CUSTOMERS")
data class Customer(
    @PrimaryKey(columnName = "CUSTOMER_ID")
    val customerId: Long? = null,
    
    @Column(name = "NAME", nullable = false)
    val name: String,
    
    @Column(name = "PRIMARY_ADDRESS_ID")
    val primaryAddressId: Long? = null,
    
    @Relationship(
        targetEntity = "com.example.domain.Order",
        expression = "this.customerId = Order.customerId",
        oneToMany = true
    )
    val orders: List<Order>? = null,
    
    @Relationship(
        targetEntity = "com.example.domain.Address",
        expression = "this.primaryAddressId = Address.addressId"
    )
    val primaryAddress: Address? = null
)
```

### 4. Complex Data Types

**XML:**
```xml
<MithraObject objectType="transactional">
    <PackageName>com.example.domain</PackageName>
    <ClassName>Product</ClassName>
    <DefaultTable>PRODUCTS</DefaultTable>
    
    <Attribute name="productId" javaType="long" 
               columnName="PRODUCT_ID" primaryKey="true"/>
    <Attribute name="name" javaType="String" 
               columnName="NAME" nullable="false" maxLength="100"/>
    <Attribute name="price" javaType="BigDecimal" 
               columnName="PRICE" nullable="false" 
               precision="10" scale="2"/>
    <Attribute name="createdDate" javaType="Timestamp" 
               columnName="CREATED_DATE"/>
    <Attribute name="isActive" javaType="boolean" 
               columnName="IS_ACTIVE"/>
    <Attribute name="stockQuantity" javaType="int" 
               columnName="STOCK_QUANTITY"/>
</MithraObject>
```

**Annotation:**
```kotlin
package com.example.domain

@ReladomoEntity(tableName = "PRODUCTS")
data class Product(
    @PrimaryKey(columnName = "PRODUCT_ID")
    val productId: Long? = null,
    
    @Column(name = "NAME", nullable = false, length = 100)
    val name: String,
    
    @Column(name = "PRICE", nullable = false)
    val price: BigDecimal,
    
    @Column(name = "CREATED_DATE")
    val createdDate: Instant? = null,
    
    @Column(name = "IS_ACTIVE")
    val isActive: Boolean = true,
    
    @Column(name = "STOCK_QUANTITY")
    val stockQuantity: Int = 0
)
```

## Type Mapping Reference

| XML javaType | Kotlin Type | Notes |
|--------------|-------------|-------|
| `boolean` | `Boolean` | Use default values |
| `byte` | `Byte` | |
| `short` | `Short` | |
| `int` | `Int` | |
| `long` | `Long` | Make nullable for primary keys |
| `float` | `Float` | |
| `double` | `Double` | |
| `BigDecimal` | `BigDecimal` | Import `java.math.BigDecimal` |
| `String` | `String` | |
| `Date` | `LocalDate` | Import `java.time.LocalDate` |
| `Timestamp` | `Instant` | Import `java.time.Instant` |
| `Time` | `LocalTime` | Import `java.time.LocalTime` |

## Configuration Changes

### From XML Configuration

**MithraRuntime.xml:**
```xml
<MithraRuntime>
    <ConnectionManager className="com.example.MyConnectionManager">
        <Property name="resourceName" value="mydb"/>
        <MithraObjectConfiguration className="com.example.domain.Customer" 
                                  cacheType="partial"/>
        <MithraObjectConfiguration className="com.example.domain.Order" 
                                  cacheType="partial"/>
    </ConnectionManager>
</MithraRuntime>
```

### To Spring Boot Configuration

**application.yml:**
```yaml
reladomo:
  kotlin:
    repository:
      base-packages:
        - com.example.domain
    cache:
      type: PARTIAL
    connection-manager: default
```

The entities are now discovered automatically through classpath scanning.

## Gradual Migration Strategy

You can migrate incrementally:

1. **Phase 1**: Keep XML files, add annotation-based entities alongside
2. **Phase 2**: Convert simple entities first (no relationships)
3. **Phase 3**: Convert entities with relationships
4. **Phase 4**: Remove XML files once all entities are converted

### Mixed Configuration Example

```yaml
reladomo:
  kotlin:
    # Scan for annotation-based entities
    repository:
      base-packages:
        - com.example.domain.new
    # Also load XML-based entities
    xml:
      class-list: classpath:reladomo/MithraClassList.xml
      runtime-config: classpath:reladomo/MithraRuntime.xml
```

## Testing the Migration

### 1. Unit Tests

Ensure your repository tests still pass:

```kotlin
@Test
fun `test customer CRUD operations`() {
    // Given
    val customer = Customer(
        name = "John Doe",
        email = "john@example.com"
    )
    
    // When
    val saved = customerRepository.save(customer)
    
    // Then
    assertNotNull(saved.customerId)
    assertEquals("John Doe", saved.name)
}
```

### 2. Integration Tests

Test relationships and temporal queries:

```kotlin
@Test
fun `test customer orders relationship`() {
    // Given
    val customer = customerRepository.findById(1L)
    
    // When
    val orders = customer?.orders
    
    // Then
    assertNotNull(orders)
    assertTrue(orders.isNotEmpty())
}
```

### 3. Performance Tests

Compare performance before and after migration to ensure no regression.

## Common Migration Issues

### Issue 1: Column Name Mismatch
**Problem**: Column names don't match database schema  
**Solution**: Use explicit column names in annotations
```kotlin
@Column(name = "CUST_NAME")  // Matches database column
val customerName: String
```

### Issue 2: Nullable Types
**Problem**: Kotlin requires explicit nullability  
**Solution**: Make optional fields nullable
```kotlin
@Column(nullable = true)
val middleName: String? = null
```

### Issue 3: Primary Key Generation
**Problem**: Auto-generated IDs need nullable types  
**Solution**: Make primary keys nullable
```kotlin
@PrimaryKey
val id: Long? = null  // Nullable for auto-generation
```

### Issue 4: Default Values
**Problem**: XML defaultIfNotSpecified not directly supported  
**Solution**: Use Kotlin default parameters
```kotlin
@Column
val status: String = "ACTIVE"  // Default value
```

## Benefits After Migration

1. **Type Safety**: Compile-time checking of entity definitions
2. **IDE Support**: Auto-completion and refactoring support
3. **Less Boilerplate**: No separate XML files to maintain
4. **Better Integration**: Natural fit with Kotlin and Spring Boot
5. **Easier Testing**: Entities are just Kotlin classes

## Next Steps

After migrating your entities:

1. Remove unused XML files
2. Update your build configuration to remove XML processing
3. Consider using Query DSL for type-safe queries
4. Implement repository interfaces for better abstraction

See also:
- [Annotation Configuration Guide](annotation-configuration-guide.md)
- [Spring Boot Integration Guide](spring-boot-integration.md)