# Kotlin Reladomo

A Kotlin wrapper library for [Reladomo ORM](https://github.com/goldmansachs/reladomo) that provides type-safe, null-safe Kotlin APIs with Spring Boot integration.

## Features

- **Type-Safe Kotlin Wrappers**: Generate Kotlin data classes from Reladomo XML definitions
- **Bitemporal Support**: First-class support for Reladomo's bitemporal data model
- **Spring Boot Integration**: Auto-configuration and transaction management
- **Code Generation**: Gradle plugin for automatic wrapper generation
- **Null Safety**: Leverage Kotlin's null safety features

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

### 4. Use Generated Classes

The plugin generates Kotlin wrappers and repositories:

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderKtRepository
) {
    fun createOrder(customerId: Long, amount: BigDecimal): OrderKt {
        val order = OrderKt(
            orderId = generateId(),
            customerId = customerId,
            amount = amount,
            businessDate = Instant.now(),
            processingDate = Instant.now()
        )
        return orderRepository.save(order)
    }
    
    fun findOrderAsOf(id: Long, businessDate: Instant): OrderKt? {
        return orderRepository.findByIdAsOf(id, businessDate, Instant.now())
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

- [Product Requirements Document](docs/planning/PRD.md)
- [MVP Implementation Plan](docs/planning/MVP_Implementation_Plan.md)
- [Test Verification Guide](docs/development/TEST_VERIFICATION.md)
- [Code Review Guidelines](docs/development/CODE_REVIEW.md)
- [Understanding Bitemporal Data](docs/BITEMPORAL_GUIDE.md) - Comprehensive guide to bitemporal concepts
- [Bitemporal Kotlin Examples](docs/BITEMPORAL_KOTLIN_EXAMPLES.md) - Practical code examples

## Requirements

- Kotlin 1.9+
- Java 17+
- Spring Boot 3.2+ (for Spring integration)
- Gradle 8.0+

## License

Apache License 2.0

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.