# Kotlin Reladomo

A Kotlin wrapper library for [Reladomo ORM](https://github.com/goldmansachs/reladomo) that provides type-safe, null-safe Kotlin APIs with Spring Boot integration.

## Features

- **Type-Safe Kotlin Wrappers**: Generate Kotlin data classes from Reladomo XML definitions
- **Bitemporal Support**: First-class support for Reladomo's bitemporal data model
- **Spring Boot Integration**: Auto-configuration and transaction management
- **Code Generation**: Gradle plugin for automatic wrapper generation
- **Null Safety**: Leverage Kotlin's null safety features
- **Annotation-Based Configuration**: Define entities using annotations instead of XML
- **Automatic Entity Discovery**: Scan classpath for @ReladomoEntity annotated classes
- **Spring Data-Style Repositories**: Familiar repository pattern with query methods

## Quick Start

### 1. Add the Gradle Plugin

```kotlin
plugins {
    id("io.github.kotlin-reladomo") version "0.1.0-SNAPSHOT"
}
```

### 2. Configure the Plugin

```kotlin
kotlinReladomo {
    xmlDirectory = file("src/main/resources/reladomo")
    outputDirectory = file("build/generated/kotlin")
    packageName = "com.example.domain.kotlin"
}
```

### 3. Define Your Reladomo Objects

#### Option A: Using Annotations (Recommended)

Define entities with annotations for automatic discovery:

```kotlin
@ReladomoEntity(
    tableName = "ORDERS",
    bitemporal = true
)
data class Order(
    @PrimaryKey(columnName = "ORDER_ID")
    val orderId: Long? = null,
    
    @Column(name = "AMOUNT", nullable = false)
    val amount: BigDecimal,
    
    @BusinessDate
    val businessDate: Instant = Instant.now(),
    
    @ProcessingDate
    val processingDate: Instant = Instant.now()
)
```

#### Option B: Using XML

Create XML definitions in `src/main/resources/reladomo/`:

```xml
<MithraObject objectType="transactional">
    <PackageName>com.example.domain</PackageName>
    <ClassName>Order</ClassName>
    <DefaultTable>ORDERS</DefaultTable>
    
    <Attribute name="orderId" javaType="long" columnName="ORDER_ID" primaryKey="true"/>
    <Attribute name="amount" javaType="BigDecimal" columnName="AMOUNT"/>
    
    <!-- Bitemporal attributes -->
    <AsOfAttribute name="businessDate" fromColumnName="BUSINESS_FROM" toColumnName="BUSINESS_THRU"/>
    <AsOfAttribute name="processingDate" fromColumnName="PROCESSING_FROM" toColumnName="PROCESSING_THRU"/>
</MithraObject>
```

### 4. Configure Spring Boot

Add minimal configuration to enable entity scanning:

```yaml
reladomo:
  kotlin:
    repository:
      base-packages:
        - com.example.domain  # Package(s) to scan for @ReladomoEntity
```

### 5. Use Repository Pattern

Define repositories with Spring Data-style query methods:

```kotlin
@Repository
interface OrderRepository : ReladomoRepository<Order, Long> {
    fun findByStatus(status: String): List<Order>
    fun findByCustomerId(customerId: Long): List<Order>
    fun findByIdAsOf(id: Long, businessDate: Instant): Order?
}

@Service
class OrderService(
    private val orderRepository: OrderRepository
) {
    @Transactional
    fun createOrder(customerId: Long, amount: BigDecimal): Order {
        val order = Order(
            customerId = customerId,
            amount = amount,
            businessDate = Instant.now(),
            processingDate = Instant.now()
        )
        return orderRepository.save(order)
    }
    
    fun findOrderAsOf(id: Long, businessDate: Instant): Order? {
        return orderRepository.findByIdAsOf(id, businessDate)
    }
}
```

## Project Structure

- `kotlin-reladomo-core`: Core interfaces and base classes
- `kotlin-reladomo-generator`: Code generation logic
- `kotlin-reladomo-spring-boot`: Spring Boot integration
- `kotlin-reladomo-gradle-plugin`: Gradle plugin
- `kotlin-reladomo-sample`: Sample application

## Building from Source

```bash
./gradlew build
```

## Running the Sample Application

The project includes a sample Spring Boot application demonstrating the functionality:

```bash
./gradlew :kotlin-reladomo-sample:bootRun
```

Once running, you can test the REST API:

```bash
# Get all orders
curl http://localhost:8080/api/orders

# Create a new order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": 100, "amount": 999.99, "status": "PENDING"}'
```

## Documentation

### Getting Started
- [Spring Boot Integration Guide](docs/spring-boot-integration.md) - Complete guide to Spring Boot features
- [Annotation Configuration Guide](docs/annotation-configuration-guide.md) - Using annotations instead of XML
- [XML to Annotation Migration](docs/xml-to-annotation-migration.md) - Migrate from XML to annotations

### Core Concepts
- [Understanding Bitemporal Data](docs/BITEMPORAL_GUIDE.md) - Comprehensive guide to bitemporal concepts
- [Bitemporal Kotlin Examples](docs/BITEMPORAL_KOTLIN_EXAMPLES.md) - Practical code examples

### Development
- [Product Requirements Document](docs/planning/PRD.md)
- [MVP Implementation Plan](docs/planning/MVP_Implementation_Plan.md)
- [Test Verification Guide](docs/development/TEST_VERIFICATION.md)
- [Code Review Guidelines](docs/development/CODE_REVIEW.md)

## Requirements

- Kotlin 1.9+
- Java 17+
- Spring Boot 3.2+ (for Spring integration)
- Gradle 8.0+

## License

Apache License 2.0

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.