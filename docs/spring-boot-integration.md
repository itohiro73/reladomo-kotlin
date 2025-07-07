# Spring Boot Integration Guide

The Kotlin Reladomo Spring Boot integration provides seamless integration between Reladomo ORM and Spring Boot applications, offering auto-configuration, entity scanning, and Spring-native features.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Auto-Configuration](#auto-configuration)
3. [Entity Configuration](#entity-configuration)
4. [Repository Support](#repository-support)
5. [Connection Managers](#connection-managers)
6. [Transaction Management](#transaction-management)
7. [Caching Configuration](#caching-configuration)
8. [Advanced Configuration](#advanced-configuration)

## Getting Started

Add the Spring Boot starter to your project:

```kotlin
dependencies {
    implementation("io.github.kotlinreladomo:kotlin-reladomo-spring-boot-starter:1.0.0")
}
```

The starter includes all necessary dependencies and auto-configuration.

## Auto-Configuration

The framework provides intelligent auto-configuration that works with zero configuration:

### Convention over Configuration

The auto-configuration follows these conventions:

1. **Entity Discovery**: Automatically scans for `@ReladomoEntity` annotated classes
2. **DataSource Integration**: Uses Spring's configured DataSource automatically
3. **Transaction Management**: Integrates with Spring's transaction management
4. **Repository Creation**: Automatically creates repository implementations

### Minimal Configuration

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

reladomo:
  kotlin:
    repository:
      base-packages:
        - com.example.domain  # Package to scan for entities
```

## Entity Configuration

### Annotation-Based Configuration

Define entities using annotations for automatic discovery:

```kotlin
@ReladomoEntity(
    tableName = "ORDERS",
    cacheType = CacheType.PARTIAL,
    bitemporal = true
)
data class Order(
    @PrimaryKey(columnName = "ORDER_ID")
    val orderId: Long? = null,
    
    @Column(name = "CUSTOMER_ID", nullable = false)
    val customerId: Long,
    
    @Column(name = "AMOUNT", nullable = false)
    val amount: BigDecimal,
    
    @Column(name = "STATUS", nullable = false, length = 50)
    val status: String,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now()
)
```

### Available Annotations

- `@ReladomoEntity`: Marks a class as a Reladomo entity
- `@PrimaryKey`: Identifies the primary key field
- `@Column`: Configures column mapping
- `@BusinessDate`: Marks business date for bitemporal entities
- `@ProcessingDate`: Marks processing date for bitemporal entities
- `@Relationship`: Defines relationships between entities

### Relationships

Define relationships between entities:

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

## Repository Support

### Automatic Repository Creation

Define repository interfaces that are automatically implemented:

```kotlin
@Repository
interface OrderRepository : ReladomoRepository<Order, Long> {
    fun findByStatus(status: String): List<Order>
    fun findByCustomerId(customerId: Long): List<Order>
    fun countByStatus(status: String): Long
    fun existsByOrderId(orderId: Long): Boolean
}
```

### Query Methods

The framework supports Spring Data-style query methods:

- `findBy...`: Returns entities matching criteria
- `countBy...`: Returns count of matching entities
- `existsBy...`: Returns boolean if any entities match
- `deleteBy...`: Deletes matching entities

### Temporal Queries

Access bitemporal data with temporal query methods:

```kotlin
interface OrderRepository : ReladomoRepository<Order, Long> {
    fun findByIdAsOf(id: Long, businessDate: Instant): Order?
    fun findAllAsOf(businessDate: Instant): List<Order>
    fun getHistory(id: Long): List<Order>
}
```

## Connection Managers

### Built-in Connection Managers

The framework provides Spring-aware connection managers for popular databases:

- **H2ConnectionManager**: For H2 database (in-memory or file-based)
- **PostgreSQLConnectionManager**: For PostgreSQL
- **MySQLConnectionManager**: For MySQL/MariaDB
- **OracleConnectionManager**: For Oracle Database

### Configuration

Connection managers are auto-configured based on your DataSource:

```yaml
reladomo:
  kotlin:
    # Use default connection manager (auto-detected from DataSource)
    connection-manager: default
    
    # Or specify explicitly
    connection-manager-class: io.github.kotlinreladomo.spring.connection.PostgreSQLConnectionManager
```

### Multi-DataSource Support

Configure multiple datasources for different entities:

```yaml
reladomo:
  kotlin:
    datasources:
      orders:
        url: jdbc:postgresql://localhost:5432/orders
        username: orders_user
        password: secret
      customers:
        url: jdbc:postgresql://localhost:5432/customers
        username: customers_user
        password: secret
```

Then specify the datasource in your entity:

```kotlin
@ReladomoEntity(
    tableName = "ORDERS",
    connectionManager = "orders"  // Use "orders" datasource
)
data class Order(...)
```

## Transaction Management

### Spring Transaction Integration

The framework integrates seamlessly with Spring's `@Transactional`:

```kotlin
@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository
) {
    fun createOrder(request: CreateOrderRequest): Order {
        val order = Order(
            customerId = request.customerId,
            amount = request.amount,
            status = "PENDING"
        )
        return orderRepository.save(order)
    }
    
    @Transactional(readOnly = true)
    fun getOrderHistory(orderId: Long): List<Order> {
        return orderRepository.getHistory(orderId)
    }
}
```

### Configuration Options

```yaml
reladomo:
  kotlin:
    transaction-management-enabled: true  # Enable Spring transaction integration
    default-transaction-timeout: 120      # Default timeout in seconds
```

## Caching Configuration

### Cache Types

Configure caching strategy per entity or globally:

```kotlin
@ReladomoEntity(
    tableName = "PRODUCTS",
    cacheType = CacheType.FULL  // Cache all products
)
data class Product(...)

@ReladomoEntity(
    tableName = "ORDERS",
    cacheType = CacheType.PARTIAL  // Cache frequently accessed orders
)
data class Order(...)

@ReladomoEntity(
    tableName = "AUDIT_LOGS",
    cacheType = CacheType.NONE  // Don't cache audit logs
)
data class AuditLog(...)
```

### Global Cache Configuration

```yaml
reladomo:
  kotlin:
    cache:
      type: PARTIAL             # Default cache type
      timeout: 3600            # Cache timeout in seconds
      max-size: 10000          # Maximum cache size
      relationship-timeout: 3600  # Relationship cache timeout
```

## Advanced Configuration

### Programmatic Configuration

For complex scenarios, use programmatic configuration:

```kotlin
@Configuration
class ReladomoConfig {
    
    @Bean
    fun reladomoConfiguration(): ReladomoConfiguration {
        return ReladomoConfigurationBuilder()
            .connectionManager("primary") {
                className = "io.github.kotlinreladomo.spring.connection.PostgreSQLConnectionManager"
                property("databaseName", "myapp")
            }
            .entity("com.example.Order") {
                tableName = "ORDERS"
                cacheType = CacheType.PARTIAL
                connectionManager = "primary"
            }
            .scanPackages("com.example.domain")
            .build()
    }
}
```

### Custom Connection Manager

Create custom connection managers for specific requirements:

```kotlin
@Component
class CustomConnectionManager : SpringAwareConnectionManager() {
    
    override fun getDatabaseType(): DatabaseType {
        return CustomDatabaseType.getInstance()
    }
    
    override fun getDatabaseIdentifier(): String {
        return "CUSTOM"
    }
    
    override fun createBulkLoader(): BulkLoader? {
        return CustomBulkLoader()
    }
}
```

### Event Listeners

Listen to Reladomo lifecycle events:

```kotlin
@Component
class ReladomoEventListener {
    
    @EventListener
    fun onMithraManagerStarted(event: MithraManagerStartedEvent) {
        logger.info("Reladomo initialized with ${event.entityCount} entities")
    }
    
    @EventListener
    fun onEntitySaved(event: EntitySavedEvent) {
        logger.debug("Entity saved: ${event.entity}")
    }
}
```

### Performance Tuning

Optimize performance with these settings:

```yaml
reladomo:
  kotlin:
    # Connection pool settings
    connection-pool:
      min-size: 5
      max-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
    
    # Query optimization
    query:
      default-fetch-size: 1000
      use-prepared-statements: true
      statement-cache-size: 250
    
    # Bulk operations
    bulk:
      batch-size: 1000
      use-multi-insert: true
```

## Migration from XML Configuration

If you have existing Reladomo XML configuration, you can:

1. **Continue using XML**: The framework supports existing XML configuration files
2. **Gradual migration**: Mix XML and annotation-based configuration
3. **Full migration**: Convert all XML to annotations

### Example Migration

From XML:
```xml
<MithraObject objectType="transactional" table="ORDERS">
    <Attribute name="orderId" javaType="long" columnName="ORDER_ID" primaryKey="true"/>
    <Attribute name="customerId" javaType="long" columnName="CUSTOMER_ID"/>
    <Attribute name="amount" javaType="BigDecimal" columnName="AMOUNT"/>
</MithraObject>
```

To Annotation:
```kotlin
@ReladomoEntity(tableName = "ORDERS")
data class Order(
    @PrimaryKey(columnName = "ORDER_ID")
    val orderId: Long?,
    
    @Column(name = "CUSTOMER_ID")
    val customerId: Long,
    
    @Column(name = "AMOUNT")
    val amount: BigDecimal
)
```

## Troubleshooting

### Common Issues

1. **Entities not found**: Ensure packages are listed in `repository.base-packages`
2. **Transaction errors**: Check that `@Transactional` is properly configured
3. **Cache issues**: Verify cache configuration matches your performance needs

### Debug Configuration

Enable debug logging:

```yaml
logging:
  level:
    io.github.kotlinreladomo: DEBUG
    com.gs.fw.common.mithra: DEBUG
```

### Health Checks

The framework provides Spring Boot Actuator health indicators:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  health:
    reladomo:
      enabled: true
```

Access health status at: `/actuator/health/reladomo`