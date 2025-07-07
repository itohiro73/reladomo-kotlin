# Sequence Generation Support

This package provides comprehensive sequence generation support for Kotlin Reladomo applications, including integration with Reladomo's SimulatedSequence strategy.

## Overview

The sequence generation framework supports multiple strategies:
- **In-Memory Sequences**: Fast, application-scoped sequences suitable for testing and single-instance deployments
- **Reladomo Sequences**: Database-backed sequences using Reladomo objects (future enhancement)
- **SimulatedSequence Support**: Generic factory for Reladomo's built-in SimulatedSequence strategy

## Components

### GenericSequenceObjectFactory

A framework-provided implementation of `MithraSequenceObjectFactory` that eliminates the need to create individual factory classes for each entity using SimulatedSequence.

#### Features:
- Automatically manages sequences in the `MITHRA_SEQUENCE` table
- Thread-safe sequence generation with proper transaction handling
- Supports H2, PostgreSQL, and other databases
- Integrates seamlessly with Spring Boot's DataSource

#### Usage:

1. Add the MITHRA_SEQUENCE table to your schema:
```sql
CREATE TABLE IF NOT EXISTS MITHRA_SEQUENCE (
    SEQUENCE_NAME VARCHAR(64) NOT NULL PRIMARY KEY,
    NEXT_ID BIGINT NOT NULL
);
```

2. Reference the factory in your Reladomo XML:
```xml
<Attribute name="customerId" javaType="long" columnName="CUSTOMER_ID" 
           primaryKey="true" primaryKeyGeneratorStrategy="SimulatedSequence">
    <SimulatedSequence sequenceName="CUSTOMER_SEQ" 
                      hasSourceAttribute="false" 
                      sequenceObjectFactoryName="io.github.kotlinreladomo.springboot.sequence.GenericSequenceObjectFactory"/>
</Attribute>
```

### SequenceGenerator Interface

For application-level sequence generation (separate from Reladomo's SimulatedSequence):

```kotlin
interface SequenceGenerator {
    fun getNextId(sequenceName: String): Long
}
```

### Configuration

Configure sequence generation in your `application.yml`:

```yaml
reladomo:
  sequence:
    enabled: true
    type: IN_MEMORY  # or RELADOMO
    default-start-value: 1000
    increment-by: 1
```

## Integration with Spring Boot

The sequence framework automatically integrates with Spring Boot through:

1. **Auto-configuration**: `SequenceAutoConfiguration` sets up the appropriate sequence generator based on configuration
2. **Bean Post-Processing**: `SequenceGeneratorBeanPostProcessor` automatically injects the sequence generator into repositories
3. **DataSource Integration**: The `GenericSequenceObjectFactory` is automatically initialized with the Spring-managed DataSource

## Best Practices

1. **Use SimulatedSequence for Reladomo entities**: When defining primary keys in Reladomo XML, use the SimulatedSequence strategy with GenericSequenceObjectFactory
2. **Use SequenceGenerator for non-Reladomo IDs**: For application-level ID generation outside of Reladomo, inject and use the SequenceGenerator
3. **Configure appropriate start values**: Set `default-start-value` high enough to avoid conflicts with existing data
4. **Consider database compatibility**: The MITHRA_SEQUENCE table DDL may need adjustment for specific databases

## Example

```kotlin
@Repository
class OrderKtRepository : BiTemporalRepository<OrderKt, Long> {
    @Autowired(required = false)
    private var sequenceGenerator: SequenceGenerator? = null
    
    override fun save(entity: OrderKt): OrderKt {
        val orderId = entity.orderId?.takeIf { it != 0L } 
            ?: sequenceGenerator?.getNextId("Order") 
            ?: throw IllegalStateException("No ID provided and sequence generator not available")
        // ... save logic
    }
}
```

## Database Support

The GenericSequenceObjectFactory uses standard SQL and has been tested with:
- H2 Database (in-memory and file-based)
- PostgreSQL
- MySQL (with minor DDL adjustments)
- Oracle (with minor DDL adjustments)

## Troubleshooting

1. **"No DataSource available" error**: Ensure the Spring Boot application has a configured DataSource
2. **"Sequence not found" error**: Check that the MITHRA_SEQUENCE table exists and is accessible
3. **Duplicate key errors**: Verify that sequence start values don't conflict with existing data
4. **ClassNotFoundException for factory**: Ensure the kotlin-reladomo-spring-boot module is on the classpath