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

## Timezone Handling Best Practices

### Fundamental Principle: "UTC Everywhere, Convert at Edges"

**Architecture Overview:**
```
[Database]     [Backend]      [Frontend]     [User]
   UTC    →→→    UTC     →→→  UTC(receive) →→→ JST(display)
                                  ↓convert
                              toLocaleString()
```

### 1. Database Layer

**Principle:** Store all timestamps in UTC

**Why:**
- Global consistency across time zones
- Avoids daylight saving time complications
- Immune to timezone configuration changes
- Easy multi-region support

**Implementation:**
```sql
-- CRITICAL: Document timezone policy clearly in schema.sql
-- TIMEZONE POLICY: All timestamps are stored in UTC
-- JVM timezone is set to UTC (-Duser.timezone=UTC) for consistent TIMESTAMP interpretation

-- Store timestamps in UTC
CREATE TABLE PRODUCT_PRICES (
    ID BIGINT NOT NULL,
    BUSINESS_FROM TIMESTAMP NOT NULL,  -- UTC
    BUSINESS_THRU TIMESTAMP NOT NULL,  -- UTC
    PROCESSING_FROM TIMESTAMP NOT NULL, -- UTC
    PROCESSING_THRU TIMESTAMP NOT NULL, -- UTC
    ...
);

-- Example data (UTC)
-- IMPORTANT: Always include JST equivalent in comments for clarity
INSERT INTO PRODUCT_PRICES (..., BUSINESS_FROM, ...)
VALUES (..., '2023-12-31 15:00:00', ...);  -- 2024-01-01 00:00:00 JST = 2023-12-31 15:00:00 UTC

-- More detailed example with conversion notes
-- Step 1 (2025/06/30 15:00 UTC = 2025/07/01 00:00 JST): pricing-team registers "1000 from Jul 1 JST"
INSERT INTO PRODUCT_PRICES (ID, PRODUCT_ID, PRICE, UPDATED_BY, BUSINESS_FROM, BUSINESS_THRU, PROCESSING_FROM, PROCESSING_THRU) VALUES
(1, 1, 1000.00, 'pricing-team', '2025-06-30 15:00:00', '9999-12-01 00:00:00', '2025-06-30 15:00:00', '2025-09-30 15:00:00');
```

**Common Mistake to Avoid:**
```sql
-- ❌ WRONG: Storing JST times directly without JVM timezone setting
INSERT INTO PRODUCT_PRICES (..., BUSINESS_FROM, ...)
VALUES (..., '2024-01-01 00:00:00', ...);  -- Ambiguous! What timezone?

-- ✅ CORRECT: Store UTC with clear documentation
INSERT INTO PRODUCT_PRICES (..., BUSINESS_FROM, ...)
VALUES (..., '2023-12-31 15:00:00', ...);  -- 2024-01-01 00:00:00 JST = 2023-12-31 15:00:00 UTC
```

### 2. Backend Layer (Spring Boot/Kotlin)

**Principle:** All internal processing uses UTC, type is `Instant` or `OffsetDateTime(UTC)`

**Configuration:**
```yaml
# application.yml
spring:
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false

reladomo:
  database-timezone: UTC
```

```xml
<!-- MithraRuntimeConfig.xml -->
<MithraRuntime>
  <ConnectionManager>
    <Property name="databaseTimezone" value="UTC"/>
  </ConnectionManager>
</MithraRuntime>
```

**JVM Settings (CRITICAL for H2 and other databases):**

**Why this is critical:**
- H2's TIMESTAMP type has **no timezone information**
- JDBC driver interprets TIMESTAMP values using **JVM's default timezone**
- Without explicit UTC setting, timestamps are interpreted as local timezone (e.g., JST in Japan)
- This creates environment-dependent behavior that breaks when deployed to different regions

**Gradle Configuration:**
```kotlin
// build.gradle.kts
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf("-Duser.timezone=UTC")
}
```

**Command-line:**
```bash
# Set JVM timezone to UTC
-Duser.timezone=UTC
```

**Verification:**
```kotlin
// Add debug endpoint to verify raw database values
@GetMapping("/raw")
fun getRawTimestamps(): List<Map<String, Any?>> {
    val sql = """
        SELECT
            ID,
            FORMATDATETIME(BUSINESS_FROM, 'yyyy-MM-dd HH:mm:ss') as BUSINESS_FROM_RAW,
            FORMATDATETIME(BUSINESS_THRU, 'yyyy-MM-dd HH:mm:ss') as BUSINESS_THRU_RAW
        FROM PRODUCT_PRICES
    """.trimIndent()
    return jdbcTemplate.queryForList(sql)
}
// With JVM timezone=UTC, RAW values should show UTC times (e.g., "2025-06-30 15:00:00")
// Without it, RAW values would show local times (e.g., "2025-07-01 00:00:00" in JST)
```

**API Response Format (ISO 8601 with UTC):**
```json
{
  "businessFrom": "2024-01-01T00:00:00Z",
  "processingFrom": "2024-11-15T00:00:00Z"
}
```

**Kotlin Code:**
```kotlin
// Use Instant for temporal values
data class ProductPriceDto(
    val id: Long,
    val price: BigDecimal,
    val businessFrom: Instant,      // Always UTC internally
    val businessThru: Instant,
    val processingFrom: Instant,
    val processingThru: Instant
)

// Repository operations use Instant
interface BiTemporalRepository<T> {
    fun findAsOf(
        businessDate: Instant,    // UTC
        processingDate: Instant   // UTC
    ): List<T>
}
```

### 3. Frontend Layer (React/TypeScript)

**Principle:** Receive UTC from server, convert to local timezone only for display

**Implementation:**
```typescript
// API Response (UTC strings)
interface ProductPrice {
  businessFrom: string;   // "2024-01-01T00:00:00Z"
  processingFrom: string; // "2024-11-15T00:00:00Z"
}

// Parse to Date (internally UTC)
const date = new Date(apiResponse.businessFrom);

// Display in user's timezone (automatic)
const formatted = date.toLocaleString('ja-JP');
// Output: "2024/01/01 09:00:00" (if user is in JST)

// Display in specific timezone
const formattedJST = date.toLocaleString('ja-JP', {
  timeZone: 'Asia/Tokyo',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit'
});
// Output: "2024/01/01 09:00:00"
```

**Common Pitfall:**
```typescript
// ❌ WRONG: Using getTime() for range calculations without timezone awareness
const minTime = Math.min(...dates.map(d => d.getTime()));
// This works because getTime() returns UTC milliseconds

// ✅ CORRECT: For display, always use toLocaleString()
const displayDate = new Date(minTime).toLocaleString('ja-JP');
```

### 4. Business Time vs Processing Time

**Business Time (ビジネス時間):**
- User-intended validity period
- Example: "Price changes from January 1, 2024"
- Storage: UTC (2023-12-31T15:00:00Z)
- Display: JST (2024/01/01 00:00)
- **Requires explicit timezone context when users input dates**

**Processing Time (処理時間):**
- System-recorded timestamp
- Always server system clock (UTC)
- Independent of user timezone
- No conversion needed for input (system-generated)

### 5. Reladomo-Kotlin Specific Recommendations

```kotlin
// 1. Entity attributes use Instant
@BusinessAsOfAttribute
val businessFrom: Instant

@BusinessAsOfAttribute
val businessThru: Instant

// 2. Repository methods accept Instant
fun findAtBusinessDate(date: Instant): List<ProductPrice> {
    return ProductPriceFinder.findMany(
        ProductPriceFinder.businessDate().eq(Timestamp.from(date))
    )
}

// 3. REST controllers output ISO 8601 (UTC)
@GetMapping("/product-prices")
fun getPrices(): List<ProductPriceDto> {
    return service.findAll().map { entity ->
        ProductPriceDto(
            businessFrom = entity.businessFrom.toString(), // ISO 8601 UTC
            processingFrom = entity.processingFrom.toString()
        )
    }
}
```

### 6. Testing Timezone Behavior

```kotlin
// Test with UTC times
@Test
fun `should handle bitemporal queries in UTC`() {
    val businessDate = Instant.parse("2024-01-01T00:00:00Z")  // Midnight JST in UTC
    val prices = repository.findAtBusinessDate(businessDate)

    assertThat(prices).isNotEmpty()
}
```

### 7. Common Issues and Solutions

**Issue:** Frontend displays dates shifted by 9 hours (JST offset)
**Cause:** Data stored in UTC (2023-12-31 15:00:00) displays as 2024/01/01 00:00 in JST
**Solution:** This is correct behavior - no fix needed. UTC offset is working as intended.

**Issue:** Axis labels in 2D timeline show wrong date ranges
**Cause:** Using `Date.now()` or current time for max ranges on 9999-year records
**Solution:** Filter out 9999-year and future timestamps before calculating ranges

**Issue:** User input "2024-01-01" gets stored with wrong timestamp
**Cause:** Browser interprets local date as UTC without explicit timezone
**Solution:** When accepting user input for business dates, explicitly construct UTC instant:
```typescript
// Frontend: Convert user input to UTC
const userInput = "2024-01-01";  // User means JST midnight
const utcDate = new Date(userInput + "T00:00:00+09:00").toISOString();
// Sends: "2023-12-31T15:00:00Z" to backend
```

**Issue:** Database viewer shows UTC timestamps but users expect JST
**Cause:** Database stores UTC, direct query returns UTC values
**Solution:** Convert to JST in the database query for display purposes:
```kotlin
// Database viewer endpoint with UTC-to-JST conversion
private fun getProductPricesTable(): DatabaseTableDto {
    // Database stores UTC, but display in JST for user-friendliness
    val sql = """
        SELECT
            ID,
            PRODUCT_ID,
            PRICE,
            UPDATED_BY,
            FORMATDATETIME(DATEADD('HOUR', 9, BUSINESS_FROM), 'yyyy-MM-dd HH:mm:ss') as BUSINESS_FROM_JST,
            FORMATDATETIME(DATEADD('HOUR', 9, BUSINESS_THRU), 'yyyy-MM-dd HH:mm:ss') as BUSINESS_THRU_JST,
            FORMATDATETIME(DATEADD('HOUR', 9, PROCESSING_FROM), 'yyyy-MM-dd HH:mm:ss') as PROCESSING_FROM_JST,
            FORMATDATETIME(DATEADD('HOUR', 9, PROCESSING_THRU), 'yyyy-MM-dd HH:mm:ss') as PROCESSING_THRU_JST
        FROM PRODUCT_PRICES
        ORDER BY PRODUCT_ID, BUSINESS_FROM, PROCESSING_FROM
    """.trimIndent()

    val rows = jdbcTemplate.query(sql) { rs, _ ->
        mapOf<String, Any?>(
            "ID" to rs.getLong("ID"),
            "PRODUCT_ID" to rs.getLong("PRODUCT_ID"),
            "PRICE" to rs.getBigDecimal("PRICE"),
            "UPDATED_BY" to rs.getString("UPDATED_BY"),
            "BUSINESS_FROM" to rs.getString("BUSINESS_FROM_JST"),  // Displayed as JST
            "BUSINESS_THRU" to rs.getString("BUSINESS_THRU_JST"),
            "PROCESSING_FROM" to rs.getString("PROCESSING_FROM_JST"),
            "PROCESSING_THRU" to rs.getString("PROCESSING_THRU_JST")
        )
    }

    return DatabaseTableDto(
        name = "PRODUCT_PRICES",
        columns = listOf("ID", "PRODUCT_ID", "PRICE", "UPDATED_BY",
                        "BUSINESS_FROM", "BUSINESS_THRU", "PROCESSING_FROM", "PROCESSING_THRU"),
        rows = rows
    )
}
// Result: Users see "2025-07-01 00:00:00" (JST) instead of "2025-06-30 15:00:00" (UTC)
// This is ONLY for display - the actual database still stores UTC
```

### 8. Create and Update Operations with Timezone Handling

**Principle:** User input for business dates must be explicitly converted to UTC before sending to backend.

#### Frontend: User Input to UTC Conversion

**Pattern 1: Date Picker Input (Business Date)**
```typescript
// User selects "2024-01-01" in a date picker (meaning JST midnight)
const handleBusinessDateChange = (userInputDate: string) => {
  // User's date is in local timezone (JST for Japanese users)
  // We need to send UTC to the backend

  // Method 1: Using timezone offset
  const utcTimestamp = new Date(userInputDate + "T00:00:00+09:00").toISOString();
  // Result: "2023-12-31T15:00:00Z" (JST midnight converted to UTC)

  // Method 2: Using Intl.DateTimeFormat (more robust)
  const date = new Date(userInputDate);
  const utcTimestamp = new Date(
    date.toLocaleString('en-US', { timeZone: 'Asia/Tokyo' })
  ).toISOString();

  return utcTimestamp;
};
```

**Pattern 2: DateTime Picker Input (Business DateTime)**
```typescript
// User selects "2024-01-01 14:30" in a datetime picker
const handleBusinessDateTimeChange = (userInputDateTime: string) => {
  // Parse the user input with explicit timezone
  const utcTimestamp = new Date(userInputDateTime + "+09:00").toISOString();
  // Input: "2024-01-01T14:30:00+09:00"
  // Result: "2024-01-01T05:30:00Z"

  return utcTimestamp;
};
```

**Pattern 3: Create Request**
```typescript
interface ProductPriceCreateRequest {
  productId: number;
  price: number;
  businessFrom: string;  // ISO 8601 UTC
  updatedBy: string;
}

const createPrice = async () => {
  const userInputDate = "2024-01-01";  // From date picker

  const request: ProductPriceCreateRequest = {
    productId: 123,
    price: 1000.00,
    businessFrom: new Date(userInputDate + "T00:00:00+09:00").toISOString(),
    updatedBy: "user@example.com"
  };

  // Sends: { businessFrom: "2023-12-31T15:00:00Z", ... }
  await fetch('/api/product-prices', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  });
};
```

**Pattern 4: Update Request**
```typescript
const updatePrice = async (id: number) => {
  const userInputDate = "2024-01-02";  // User wants to change business date

  const request = {
    price: 1200.00,
    businessFrom: new Date(userInputDate + "T00:00:00+09:00").toISOString(),
    updatedBy: "user@example.com"
  };

  // Sends: { businessFrom: "2024-01-01T15:00:00Z", ... }
  await fetch(`/api/product-prices/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  });
};
```

#### Backend: Accepting and Processing UTC Dates

**Pattern 1: Create DTO with Instant**
```kotlin
data class ProductPriceCreateDto(
    val productId: Long,
    val price: BigDecimal,
    val businessFrom: String,  // ISO 8601 UTC from frontend
    val updatedBy: String
)

@PostMapping("/product-prices")
fun createPrice(@RequestBody dto: ProductPriceCreateDto): ProductPriceDto {
    return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
        // Convert ISO 8601 string to Instant (already UTC)
        val businessFrom = Instant.parse(dto.businessFrom)

        // Create new entity
        val entity = ProductPrice()
        entity.productId = dto.productId
        entity.price = dto.price
        entity.businessFromAttribute = Timestamp.from(businessFrom)
        entity.businessThruAttribute = Timestamp.from(Instant.parse("9999-12-31T23:59:59Z"))
        entity.updatedBy = dto.updatedBy

        // Reladomo automatically sets processingFrom to current UTC time
        // and processingThru to infinity
        entity.insert()

        // Convert back to DTO
        toDto(entity)
    }
}
```

**Pattern 2: Update with AsOf Query**
```kotlin
data class ProductPriceUpdateDto(
    val price: BigDecimal,
    val businessFrom: String,  // New business date (ISO 8601 UTC)
    val updatedBy: String
)

@PutMapping("/product-prices/{id}")
fun updatePrice(
    @PathVariable id: Long,
    @RequestBody dto: ProductPriceUpdateDto
): ProductPriceDto {
    return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
        // Parse the target business date
        val newBusinessFrom = Instant.parse(dto.businessFrom)

        // Find existing record using AsOf query
        // Query for record valid at new business date, currently active
        val operation = ProductPriceFinder.id().eq(id)
            .and(ProductPriceFinder.businessDate().eq(Timestamp.from(newBusinessFrom)))
            .and(ProductPriceFinder.processingDate().equalsInfinity())

        val existing = ProductPriceFinder.findOne(operation)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Price not found for business date ${dto.businessFrom}"
            )

        // Update properties - Reladomo handles bitemporal chaining
        existing.price = dto.price
        existing.updatedBy = dto.updatedBy

        // Reladomo automatically:
        // 1. Terminates old version (sets PROCESSING_THRU to now)
        // 2. Creates new version (PROCESSING_FROM = now, PROCESSING_THRU = infinity)
        // 3. Preserves BUSINESS_FROM/THRU from existing record

        toDto(existing)
    }
}
```

**Pattern 3: Create with Future Business Date (Planning)**
```kotlin
// User wants to plan a price change for future business date
@PostMapping("/product-prices/plan")
fun planFuturePrice(@RequestBody dto: ProductPriceCreateDto): ProductPriceDto {
    return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
        val futureBusinessFrom = Instant.parse(dto.businessFrom)  // e.g., 2024-06-01
        val now = Instant.now()

        // Validate future date
        if (futureBusinessFrom.isBefore(now)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Business date must be in the future for planning"
            )
        }

        // Create entity with future business date
        val entity = ProductPrice()
        entity.productId = dto.productId
        entity.price = dto.price
        entity.businessFromAttribute = Timestamp.from(futureBusinessFrom)
        entity.businessThruAttribute = Timestamp.from(Instant.parse("9999-12-31T23:59:59Z"))
        entity.updatedBy = dto.updatedBy

        // Processing time is NOW (when the plan is recorded)
        // Business time is FUTURE (when the price becomes effective)
        entity.insert()

        toDto(entity)
    }
}
```

#### Business Time vs Processing Time in Create/Update

**Business Time (User-Controlled):**
- User explicitly sets when the data is **valid/effective**
- Frontend must convert user's timezone to UTC
- Backend accepts as ISO 8601 UTC string
- Example: "Price changes from January 1, 2024" → `"2023-12-31T15:00:00Z"` (UTC)

**Processing Time (System-Controlled):**
- Reladomo **automatically** sets to current server time (UTC)
- Frontend **NEVER** sends this value
- Backend **NEVER** accepts this from user input
- Always represents "when the system recorded this change"

**Example Flow:**
```typescript
// Frontend: User creates new price effective from 2024-01-01 (JST)
const request = {
  businessFrom: "2023-12-31T15:00:00Z",  // User-specified (converted to UTC)
  price: 1000.00
  // NO processingFrom - system will set automatically
};

// Backend: Reladomo creates record
// BUSINESS_FROM = 2023-12-31 15:00:00 (from request)
// BUSINESS_THRU = 9999-12-31 23:59:59 (infinity)
// PROCESSING_FROM = 2024-11-15 10:30:45 (current server time, automatic)
// PROCESSING_THRU = 9999-12-31 23:59:59 (infinity, automatic)
```

#### Common Mistakes and Solutions

**Mistake 1: Sending local timezone without conversion**
```typescript
// ❌ WRONG
const date = new Date("2024-01-01").toISOString();
// This interprets as local midnight, but toISOString() converts browser timezone
// In JST: Results in "2023-12-31T15:00:00Z" - accidentally correct!
// In EST: Results in "2024-01-01T05:00:00Z" - WRONG (off by 5 hours)
```

```typescript
// ✅ CORRECT: Always specify timezone explicitly
const date = new Date("2024-01-01T00:00:00+09:00").toISOString();
// Always results in "2023-12-31T15:00:00Z" regardless of browser timezone
```

**Mistake 2: Trying to set processingFrom from frontend**
```typescript
// ❌ WRONG - Frontend should NOT send processingFrom
const request = {
  businessFrom: "2024-01-01T00:00:00Z",
  processingFrom: new Date().toISOString()  // DON'T DO THIS
};
```

```kotlin
// ✅ CORRECT - Backend ignores any processingFrom in request
@PostMapping("/product-prices")
fun createPrice(@RequestBody dto: ProductPriceCreateDto) {
    // DTO should not have processingFrom field
    // Reladomo sets it automatically
}
```

**Mistake 3: Not preserving businessFrom/businessThru on updates**
```kotlin
// ❌ WRONG - Creating new business validity period instead of updating
val entity = ProductPrice()
entity.businessFromAttribute = Timestamp.from(Instant.now())  // WRONG
entity.price = newPrice
entity.insert()
```

```kotlin
// ✅ CORRECT - Use AsOf query to find existing record
val existing = ProductPriceFinder.findOne(
    ProductPriceFinder.productId().eq(productId)
        .and(ProductPriceFinder.businessDate().eq(targetBusinessDate))
        .and(ProductPriceFinder.processingDate().equalsInfinity())
)
existing.price = newPrice  // Reladomo preserves business time range
```

### Reference Documentation

For detailed timezone handling patterns, see:
- [ISO 8601 Standard](https://en.wikipedia.org/wiki/ISO_8601)
- [MDN: Date.toLocaleString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toLocaleString)
- [MDN: Date.toISOString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toISOString)
- [Spring Boot Jackson Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.data.spring.jackson.time-zone)

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

## Bitemporal Data Chaining - Critical Concepts

### Core Principle: No Gaps, No Overlaps
When properly chained, bitemporal data forms a **complete 2D plane with NO gaps and NO overlaps**. Every point in time must be covered by exactly one valid record version.

### Understanding the Two Time Dimensions

1. **Business Time (横軸)**: When the fact is valid in the real world
   - `BUSINESS_FROM`: Start of validity period
   - `BUSINESS_THRU`: End of validity period
   - `BUSINESS_THRU = 9999`: "We don't know when this will end"

2. **Processing Time (縦軸)**: When the system recorded this knowledge
   - `PROCESSING_FROM`: When we learned this fact
   - `PROCESSING_THRU`: When we stopped believing this version
   - `PROCESSING_THRU = 9999`: "This is currently what we believe"

### Record Splitting Pattern

When a new price is registered for a future business date, existing records **split into two parts**:

**Example**: pricing-team registers 1000円 on 2025-07-01, then Alice registers "1200円 from 2025-11-01" on 2025-10-01

1. **Past Version** (what we thought before):
   ```
   1000円, BUS[2025-07-01, 9999), PROC[2025-07-01, 2025-10-01)
   ```
   - `BUSINESS_THRU` changes from `9999` to `2025-11-01` (when the new price starts)
   - `PROCESSING_THRU` changes from `9999` to `2025-10-01` (when we learned about the change)

2. **Continuation Version** (what we know now):
   ```
   1000円, BUS[2025-07-01, 2025-11-01), PROC[2025-10-01, 9999)
   ```
   - Same business period, but new processing period
   - "From Oct 1 onwards, we know Jul 1 to Nov 1 was 1000"

3. **New Record**:
   ```
   1200円, BUS[2025-11-01, 9999), PROC[2025-10-01, 9999)
   ```

### Correction vs New Price

**CRITICAL**: When correcting a mistake, `BUSINESS_THRU` stays `9999` - only `PROCESSING_THRU` changes!

**Example**: Bob corrects Alice's 1200円 to 1100円 on 2025-10-15

Alice's record becomes:
```
1200円, BUS[2025-11-01, 9999), PROC[2025-10-01, 2025-10-15)
```
- `BUSINESS_THRU` **stays 9999** (it was meant to be valid forever, just wrong)
- `PROCESSING_THRU` = `2025-10-15` (we stopped believing this on Oct 15)
- This preserves the history: "From Oct 1 to Oct 15, we incorrectly thought Nov 1 onwards would be 1200"

Bob's correction creates:
```
1100円, BUS[2025-11-01, 9999), PROC[2025-10-15, 9999)
```

### Timestamp Format Consistency

**IMPORTANT**: Always use `9999-12-01 00:00:00` format for infinite timestamps, never `9999-12-31 23:59:59`.

**BAD** (causes gaps):
```sql
BUSINESS_THRU = '2025-10-31 23:59:59'  -- Next record starts at 2025-11-01 00:00:00
```

**GOOD** (perfect alignment):
```sql
BUSINESS_THRU = '2025-11-01 00:00:00'  -- Exactly matches next BUSINESS_FROM
```

### Verification Checklist for Seed Data

When creating bitemporal test data:

1. ✅ **2D Coverage**: Plot all records on a 2D plane - there should be no gaps or overlaps
2. ✅ **THRU matches FROM**: Each record's `BUSINESS_THRU` must exactly equal the next record's `BUSINESS_FROM`
3. ✅ **Corrections preserve BUSINESS_THRU**: When fixing mistakes, only `PROCESSING_THRU` changes
4. ✅ **Splits create pairs**: New future prices split existing records into (past version, continuation version)
5. ✅ **Current records**: Exactly one chain of records should have `PROCESSING_THRU = 9999` at any business time

### Example: Complete Chain

```sql
-- Step 1: Initial registration (2025-07-01)
-- "We think 1000 from Jul 1 forever"
(1, 1000.00, 'pricing-team', '2025-07-01', '9999', '2025-07-01', '2025-10-01')

-- Step 2: Alice registers future price (2025-10-01)
-- "We now know: Jul 1-Nov 1 was 1000, Nov 1 onwards will be 1200"
(2, 1000.00, 'pricing-team', '2025-07-01', '2025-11-01', '2025-10-01', '9999')
(3, 1200.00, 'alice@example.com', '2025-11-01', '9999', '2025-10-01', '2025-10-15')

-- Step 3: Bob corrects (2025-10-15)
-- "We now know: Nov 1 onwards should be 1100, not 1200"
(4, 1100.00, 'bob@example.com', '2025-11-01', '9999', '2025-10-15', '9999')
-- Note: Alice's record (3) keeps BUSINESS_THRU=9999, only PROCESSING_THRU changed
```

This creates a **complete 2D plane** where every business time × processing time point is covered by exactly one record.

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