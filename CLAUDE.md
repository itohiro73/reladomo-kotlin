# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin wrapper library for Reladomo ORM that enables transparent use of Reladomo's bitemporal data model features from Kotlin/Spring Boot applications. The project is starting fresh with a focus on MVP implementation.

## Key Architecture Decisions

### Module Structure (Planned)
```
reladomo-kotlin/
├── reladomo-kotlin-core/          # Core wrapper functionality
├── reladomo-kotlin-generator/     # XML to Kotlin code generation
├── reladomo-kotlin-spring-boot/   # Spring Boot integration
├── reladomo-kotlin-gradle-plugin/ # Gradle plugin for build integration
└── reladomo-kotlin-sample/        # Sample application
```

### Core Design Principles
1. **Type Safety**: Generate Kotlin data classes with null safety from Reladomo XML
2. **Spring Integration**: Seamless Spring Boot transaction and DataSource management
3. **Bitemporal Support**: First-class support for Reladomo's bitemporal features
4. **Code Generation**: Automatic generation of wrapper classes and repositories

## Development Commands

### Build Commands

**IMPORTANT**: Always check your current directory with `pwd` before running gradle commands. Gradle commands should be run from the project root directory `/data/data/com.termux/files/home/development/reladomo-kotlin/`.

**IMPORTANT**: When checking for running processes, always use `ps -ef` instead of `ps aux` for better compatibility.

```bash
# Always check current directory first
pwd

# If not in project root, navigate to it
cd /data/data/com.termux/files/home/development/reladomo-kotlin/

# Full project build
./gradlew build

# Generate Kotlin wrappers from Reladomo XML
./gradlew generateKotlinWrappers

# Run tests
./gradlew test

# Run specific module tests
./gradlew :reladomo-kotlin-generator:test
./gradlew :reladomo-kotlin-sample:test

# Clean build
./gradlew clean build
```

### Testing the Sample Application

```bash
# Run the sample application
./gradlew :reladomo-kotlin-sample:bootRun

# Test CRUD operations
# GET all orders
curl -s http://localhost:8080/api/orders | python3 -m json.tool

# CREATE a new order
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": 300, "amount": 2500.00, "status": "PENDING", "description": "New test order"}' | python3 -m json.tool

# UPDATE an order (replace {id} with actual order ID)
curl -s -X PUT http://localhost:8080/api/orders/{id} \
  -H "Content-Type: application/json" \
  -d '{"customerId": 300, "amount": 3000.00, "status": "COMPLETED", "description": "Updated test order"}' | python3 -m json.tool

# DELETE an order (replace {id} with actual order ID)
curl -s -X DELETE http://localhost:8080/api/orders/{id}

# GET specific order
curl -s http://localhost:8080/api/orders/{id} | python3 -m json.tool
```

### Code Generation Configuration
The project will use a Gradle plugin to generate Kotlin code from Reladomo XML files:
```kotlin
reladomoKotlin {
    xmlDirectory = file("src/main/resources/reladomo")
    outputDirectory = file("build/generated/kotlin")
    packageName = "com.example.domain.kotlin"
}
```

## Important Implementation Details

### MVP Implementation Guidelines
- **DO NOT comment out real functionality** - Even in MVP, maintain the actual implementation
- **DO NOT bypass any production logic until MVP functionality is completely implemented**
- **Code generation must work** - It's a core part of the MVP and must function end-to-end
- **DO NOT manually create files that should be generated** - Fix the code generation instead
- **DO NOT create workarounds or shortcuts** - Fix the actual problem in the generator
- **ALWAYS identify the root cause before making changes** - Understand the issue fully before attempting fixes
- **When implementing new versions, disable old implementations** - Don't keep conflicting implementations active simultaneously
- If a dependency is missing, fix it properly rather than working around it
- Keep all method signatures and contracts intact
- Use proper interfaces and abstractions instead of removing functionality
- Test each component individually before integration to ensure everything works
- When encountering compilation errors with generated code, fix the generator, not the generated files

### Testing Requirements
- **ALL new features MUST have automated tests** - No feature is complete without tests
- **Write tests BEFORE or ALONGSIDE implementation** - Not as an afterthought
- **Test coverage should include**:
  - Unit tests for individual components
  - Integration tests for feature interactions
  - End-to-end tests for complete workflows
- **Sample app must demonstrate REAL functionality** - Never use mocks in the sample app
- **If tests fail, fix the implementation** - Don't disable or skip tests

### Pre-Commit Testing Requirements
- **ALWAYS run build and tests before committing** - Verify changes locally first
  ```bash
  # Build publishable modules
  ./gradlew assemble -x :reladomo-kotlin-sample:assemble --warning-mode all

  # Run tests on publishable modules
  ./gradlew test -x :reladomo-kotlin-sample:test
  ```
- **Verify all tests pass** - Do not commit if any tests fail
- **Check for compilation errors** - Ensure clean build before committing
- **Run affected module tests** - At minimum, test the modules you modified

### Git Commit Guidelines
- **ALWAYS write commit messages in English** - Maintain consistency with existing commit history
- **Use conventional commit format**:
  - First line: Concise summary (imperative mood, e.g., "Add feature" not "Added feature")
  - Blank line
  - Bullet points describing specific changes
  - Blank line
  - Claude Code footer (automatic)
- **Example commit message**:
  ```
  Add time-travel query endpoint for bitemporal demonstration

  - Implement GET /api/product-prices/asof endpoint
  - Add SQL range queries for both time dimensions
  - Support querying historical data at specific points in time
  - Add comprehensive error handling

  🤖 Generated with [Claude Code](https://claude.com/claude-code)

  Co-Authored-By: Claude <noreply@anthropic.com>
  ```

### Bitemporal Entity Pattern
All generated entities implement `BiTemporalEntity` interface:
```kotlin
interface BiTemporalEntity {
    val businessDate: Instant
    val processingDate: Instant
}
```

### SimulatedSequence Support
The framework provides `GenericSequenceObjectFactory` that works with Reladomo's SimulatedSequence:
- No need to create individual factory classes per entity
- Automatically manages sequences in MITHRA_SEQUENCE table
- Thread-safe with proper transaction handling
- Reference in XML: `sequenceObjectFactoryName="io.github.reladomokotlin.springboot.sequence.GenericSequenceObjectFactory"`

### Repository Pattern
Repositories extend `AbstractBiTemporalRepository` which provides:
- Basic CRUD operations
- AsOf queries for temporal data
- Type-safe primary key operations

### XML to Kotlin Type Mapping
- `long` → `Long`
- `Timestamp` → `Instant` 
- `BigDecimal` → `BigDecimal`
- Nullable attributes → Kotlin nullable types

## Current Status

The project has made significant progress with:
- Comprehensive PRD (`Reladomo_Kotlin_Wrapper_PRD_v1.0.md`)
- MVP Implementation Plan (`Reladomo_Kotlin_MVP_Implementation_Plan_v1.0.md`)
- Working code generation from Reladomo XML to Kotlin
- Spring Boot integration with auto-configuration
- Generic sequence factory for SimulatedSequence support
- Sample application demonstrating CRUD operations

## Technology Stack

- **Kotlin**: 1.9+
- **Spring Boot**: 3.2+
- **Reladomo**: 18.0+
- **Gradle**: 8.0+
- **Java**: 17+
- **KotlinPoet**: For code generation
- **Mockito**: 5.11.0 (with mockito-kotlin 5.2.1) for testing

### Testing Framework Migration
The project uses **Mockito** instead of MockK due to compatibility issues with Java 17/21:
- **Issue**: MockK's Java instrumentation agent conflicts with Gradle daemon on Java 17/21
- **Symptom**: CI builds hang indefinitely with `java.lang.instrument ASSERTION FAILED` errors
- **Solution**: Mockito has better compatibility with modern JVMs and Spring Boot ecosystem
- **Migration**: Completed for spring-boot module; core and generator modules don't use mocking

## Key Challenges Being Addressed

1. **Java-centric Reladomo** → Kotlin-idiomatic wrapper
2. **Complex XML configuration** → Automated code generation
3. **Manual Spring integration** → Auto-configuration
4. **Verbose temporal queries** → Simplified Kotlin DSL
5. **Manual sequence factory classes** → Generic framework-provided factory

## Implementation Phases

1. **Phase 1**: Code generation (XML parser, Kotlin generator, Gradle plugin)
2. **Phase 2**: Core functionality (entity wrappers, repositories, type conversion)
3. **Phase 3**: Spring Boot integration (auto-config, transaction management)
4. **Phase 4**: Sample implementation
5. **Phase 5**: Testing and documentation

## Database Schema Requirements

Bitemporal tables require these columns:
- Primary key columns
- Business time columns: `BUSINESS_FROM`, `BUSINESS_THRU`
- Processing time columns: `PROCESSING_FROM`, `PROCESSING_THRU`
- Composite primary key including temporal columns

## Learnings from Demo Project Implementation

### Reladomo Entity Relationships and Temporal Attributes
**Critical**: When defining Relationships in Reladomo XML, the join conditions must reference attributes that actually exist on both entities.

- ❌ **Wrong**: Referencing temporal attributes that don't exist
  ```xml
  <!-- If Product is non-temporal, it has NO businessDate attribute -->
  <Relationship name="product" relatedObject="Product">
      this.productId = Product.id and this.businessDate = Product.businessDate
  </Relationship>
  ```

- ✅ **Correct**: Only reference attributes that exist on the entity
  ```xml
  <!-- Product is non-temporal, so only join on id -->
  <Relationship name="product" relatedObject="Product">
      this.productId = Product.id
  </Relationship>
  ```

**Error symptom**: `NullPointerException: Cannot invoke "com.gs.fw.common.mithra.generator.AbstractAttribute.getOwner()" because "rightAttribute" is null`

**Fix approach**: When encountering this error, check all Relationship definitions in XML files. Ensure referenced attributes match the temporal characteristics of both entities.

### MithraRuntimeConfig.xml Configuration
Reladomo entities must be explicitly registered in `MithraRuntimeConfig.xml` for runtime initialization.

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<MithraRuntime>
    <ConnectionManager className="io.github.reladomokotlin.spring.connection.H2ConnectionManager">
        <Property name="dataSourceName" value="dataSource"/>
        <MithraObjectConfiguration className="io.github.reladomokotlin.demo.domain.Category" cacheType="partial"/>
        <MithraObjectConfiguration className="io.github.reladomokotlin.demo.domain.Product" cacheType="partial"/>
        <MithraObjectConfiguration className="io.github.reladomokotlin.demo.domain.ProductPrice" cacheType="partial"/>
    </ConnectionManager>
</MithraRuntime>
```

**Key points**:
- Use `H2ConnectionManager` for H2 database (not SpringConnectionManager which doesn't exist in Reladomo)
- All `MithraObjectConfiguration` elements **require** `cacheType` attribute (e.g., `cacheType="partial"`)
- File location: `src/main/resources/MithraRuntimeConfig.xml`

### ReladomoConfig Spring Bean
A Spring configuration class is required to initialize MithraManager:

```kotlin
@Configuration
class ReladomoConfig {
    @Autowired
    private lateinit var dataSource: DataSource

    @Bean
    fun mithraManager(): MithraManager {
        val manager = MithraManagerProvider.getMithraManager()
        manager.setTransactionTimeout(120)

        val configStream: InputStream = javaClass.classLoader.getResourceAsStream("MithraRuntimeConfig.xml")
            ?: throw IllegalStateException("Could not find MithraRuntimeConfig.xml")

        manager.readConfiguration(configStream)
        manager.fullyInitialize()  // MUST call after readConfiguration

        return manager
    }
}
```

**Critical steps**:
1. Load `MithraRuntimeConfig.xml` from classpath
2. Call `manager.readConfiguration(configStream)`
3. Call `manager.fullyInitialize()` - this is required for entities to be usable

### Database Schema Consistency
Database schema SQL must match entity temporal characteristics **exactly**.

- **Non-temporal entities**: No temporal columns in table or INSERT statements
  ```sql
  CREATE TABLE PRODUCTS (
      ID BIGINT NOT NULL PRIMARY KEY,
      CATEGORY_ID BIGINT NOT NULL,
      NAME VARCHAR(200) NOT NULL,
      DESCRIPTION VARCHAR(1000)
      -- NO VALID_FROM, VALID_TO, or other temporal columns
  );

  INSERT INTO PRODUCTS (ID, CATEGORY_ID, NAME, DESCRIPTION) VALUES
  (1, 1, 'Laptop Pro 15', 'High-performance laptop');
  ```

- **Bitemporal entities**: Four temporal columns required
  ```sql
  CREATE TABLE PRODUCT_PRICES (
      ID BIGINT NOT NULL,
      PRODUCT_ID BIGINT NOT NULL,
      PRICE DECIMAL(19, 2) NOT NULL,
      BUSINESS_FROM TIMESTAMP NOT NULL,
      BUSINESS_THRU TIMESTAMP NOT NULL,
      PROCESSING_FROM TIMESTAMP NOT NULL,
      PROCESSING_THRU TIMESTAMP NOT NULL,
      PRIMARY KEY (ID, BUSINESS_FROM, PROCESSING_FROM)
  );
  ```

**Common error**: Keeping temporal columns in CREATE TABLE but forgetting to update INSERT statements (or vice versa)
**Symptom**: `Column "VALID_FROM" not found` during application startup

### Bitemporal Update Pattern in Reladomo

**CRITICAL**: Reladomo automatically handles bitemporal chaining. **DO NOT** manually update/insert records.

**CORRECT AsOf Query Pattern**:
```kotlin
// Find record valid at specific business date, currently valid in processing time
val operation = ProductPriceFinder.productId().eq(productId)
    .and(ProductPriceFinder.businessDate().eq(Timestamp.from(businessDate)))
    .and(ProductPriceFinder.processingDate().equalsInfinity())
val existing = ProductPriceFinder.findOne(operation)
```

**Key points for AsOf queries**:
- Use `.eq(Timestamp)` for businessDate - finds records where the date falls within [BUSINESS_FROM, BUSINESS_THRU)
- Use `.equalsInfinity()` for processingDate - finds current version (PROCESSING_THRU = infinity)
- **DO NOT** use `.equalsEdgePoint()` - this method does not exist in Reladomo-generated Finders
- processingDate does NOT need "as of now" specification when using equalsInfinity()

**CORRECT Update Pattern**:
```kotlin
// 1. Find the record valid at the target business date using AsOf query
val businessDate = Instant.parse("2025-01-02T00:00:00Z")
val operation = ProductPriceFinder.productId().eq(productId)
    .and(ProductPriceFinder.businessDate().eq(Timestamp.from(businessDate)))
    .and(ProductPriceFinder.processingDate().equalsInfinity())
val existing = ProductPriceFinder.findOne(operation)

// 2. Update the property (e.g., price) - Reladomo handles chaining automatically
existing?.let {
    it.price = BigDecimal("1200.00")
    // Reladomo detects the change and creates new version with bitemporal chaining:
    // - Terminates old version (sets PROCESSING_THRU to now)
    // - Creates new version (PROCESSING_FROM = now, PROCESSING_THRU = infinity)
    // - Preserves BUSINESS_FROM/THRU from existing record
}
```

**WRONG Pattern** (manual chaining):
```kotlin
// ❌ DO NOT DO THIS
jdbcTemplate.update("UPDATE ... SET PROCESSING_THRU = ?")
jdbcTemplate.update("INSERT INTO ... VALUES (...)")
```

**Why this matters**:
- Reladomo tracks object state and generates proper SQL
- Manual UPDATE/INSERT bypasses Reladomo's bitemporal logic
- BUSINESS_FROM/THRU must be preserved from the existing record
- Only PROCESSING_FROM/THRU should change when updating

**Common mistake**: Creating new records with wrong BUSINESS_FROM
- If you manually insert, you might use the target business date as BUSINESS_FROM
- This creates a new business validity period instead of updating the existing one
- Correct: Use AsOf query to find existing record, then update its properties

### Entity Scanning Configuration
Update `application.yml` to specify correct packages for entity scanning:

```yaml
reladomo:
  kotlin:
    repository:
      base-packages:
        - io.github.reladomokotlin.demo.domain              # Reladomo entity classes
        - io.github.reladomokotlin.demo.domain.kotlin.repository  # Kotlin repositories
      enable-query-methods: true
```

**Why both packages**:
- Domain package: Contains generated Reladomo entity classes
- Repository package: Contains generated Kotlin repository classes

### Uni-temporal vs Bitemporal Complexity
**Recommendation**: For MVPs and demos, prefer **non-temporal** or **bitemporal** over **uni-temporal**.

- **Uni-temporal** (single time dimension with VALID_FROM/VALID_TO) adds significant complexity:
  - Code generators need special handling for single AsOfAttribute
  - Repository methods require different parameter sets
  - Query construction is more complex than bitemporal

- **Bitemporal** is better supported in the framework:
  - Code generators fully handle dual AsOfAttribute pattern
  - Repository methods consistently use businessDate and processingDate
  - Demonstrates more advanced Reladomo capabilities

**For demos**: Use non-temporal entities for simple relationships and bitemporal for showcasing temporal features. Skip uni-temporal unless specifically required.

### Full-Stack Feature Implementation Checklist

When adding new fields to entities (especially for demo applications), ensure ALL layers are updated:

1. **Database Schema** (`schema.sql`)
   - Add column to CREATE TABLE statement
   - Update sample INSERT statements

2. **Reladomo XML** (`*.xml`)
   - Add attribute definition
   - Regenerate code if using code generation

3. **Controllers and DTOs**
   - Update domain controllers to save/return the field
   - Update DTO classes if using separate DTOs

4. **DatabaseViewController** (for demo apps with raw database views)
   - ⚠️ **CRITICAL**: Update SQL queries to include new column
   - Add column to row mapping (e.g., `"FIELD_NAME" to rs.getString("FIELD_NAME")`)
   - Add column to columns list
   - **Why this is easy to miss**: DatabaseViewController bypasses the ORM layer and directly queries the database, so changes aren't automatically picked up through Reladomo entities

5. **Frontend Components**
   - Update API client types
   - Add UI fields for input/display
   - Add appropriate styling

**Common pitfall**: Implementing the feature in controllers (1-3) and frontend (5) but forgetting DatabaseViewController (4), resulting in the field not appearing in raw database views even though the data is being saved correctly.

**Debugging tip**: If a field isn't displaying in the UI but you've confirmed the frontend code is correct, check the API response from the backend. DatabaseViewController often requires manual updates when new columns are added.