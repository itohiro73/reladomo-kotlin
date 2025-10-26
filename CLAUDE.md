# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin wrapper library for Reladomo ORM that enables transparent use of Reladomo's bitemporal data model features from Kotlin/Spring Boot applications.

## Key Architecture Decisions

### Module Structure
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

## Important Implementation Guidelines

### MVP Implementation
- **DO NOT bypass production logic** - Maintain actual implementation even in MVP
- **Code generation must work** - Core MVP requirement, must function end-to-end
- **DO NOT create workarounds** - Fix the actual problem
- **ALWAYS identify root cause before changes** - Understand the issue fully
- **Test each component individually** - Ensure everything works before integration

### Testing Requirements
- **ALL new features MUST have automated tests** - No feature is complete without tests
- **Test coverage**: Unit tests, integration tests, end-to-end tests
- **Sample app demonstrates REAL functionality** - Never use mocks in sample app

### Git Commit Guidelines
- **ALWAYS write commit messages in English** - Maintain consistency
- **Use conventional commit format**: Concise summary, bullet points, Claude Code footer
- **Example**:
  ```
  Add bitemporal update pattern with AsOf queries

  - Implement AsOf query approach for finding records at effective date
  - Replace insertWithIncrementUntil with property modification pattern
  - Add automatic bitemporal chaining via Reladomo
  - Update documentation with correct update patterns

  🤖 Generated with [Claude Code](https://claude.com/claude-code)

  Co-Authored-By: Claude <noreply@anthropic.com>
  ```

## Technology Stack

- **Kotlin**: 1.9+, **Spring Boot**: 3.2+, **Reladomo**: 18.0+
- **Gradle**: 8.0+, **Java**: 17+
- **KotlinPoet**: For code generation
- **Mockito**: 5.11.0 (with mockito-kotlin 5.2.1) - Used instead of MockK due to Java 17/21 compatibility

## Timezone Handling

### Fundamental Principle: "UTC Everywhere, Convert at Edges"

**Architecture**: Database (UTC) → Backend (UTC) → Frontend (UTC receive) → User (local display)

### Database Layer
- **Store all timestamps in UTC**
- **Document timezone policy** in schema.sql
- **Set JVM timezone to UTC**: `-Duser.timezone=UTC` (CRITICAL for H2 and databases without timezone info)
- **Example**:
  ```sql
  -- TIMEZONE POLICY: All timestamps stored in UTC, JVM timezone set to UTC
  CREATE TABLE PRODUCT_PRICES (
      BUSINESS_FROM TIMESTAMP NOT NULL,  -- UTC
      PROCESSING_FROM TIMESTAMP NOT NULL -- UTC
  );

  -- Always include JST equivalent in comments
  INSERT INTO PRODUCT_PRICES (..., BUSINESS_FROM, ...)
  VALUES (..., '2023-12-31 15:00:00', ...);  -- 2024-01-01 00:00 JST
  ```

### Backend Layer
- **Use Instant for all temporal values**
- **API outputs ISO 8601 UTC**: `"2024-01-01T00:00:00Z"`
- **Convert user input to UTC**:
  ```kotlin
  val effectiveDate = LocalDate.parse(dto.effectiveDate)
  val businessFrom = effectiveDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))  // JST to UTC
  ```

### Frontend Layer
- **Receive UTC, display in user's timezone**:
  ```typescript
  const date = new Date(apiResponse.businessFrom);  // UTC internally
  const formatted = date.toLocaleString('ja-JP');   // Auto-converts to JST
  ```
- **Convert user input to UTC before sending**:
  ```typescript
  const utcTimestamp = new Date(userInput + "T00:00:00+09:00").toISOString();
  // "2024-01-01" → "2023-12-31T15:00:00Z"
  ```

### Business Time vs Processing Time
- **Business Time**: User-controlled validity ("When is this effective?") - Frontend converts timezone
- **Processing Time**: System-generated timestamp (current server time UTC) - Never from user input

## Bitemporal Data Model

### Core Principle: No Gaps, No Overlaps
Bitemporal data forms a complete 2D plane where every point in time is covered by exactly one record.

### Two Time Dimensions
1. **Business Time**: When the fact is valid in the real world (BUSINESS_FROM/THRU)
2. **Processing Time**: When the system recorded this knowledge (PROCESSING_FROM/THRU)

### Timestamp Format Consistency
**IMPORTANT**: Always use `9999-12-01 00:00:00` for infinity timestamps.

```sql
-- ✅ CORRECT: Exactly matches next BUSINESS_FROM
BUSINESS_THRU = '2025-11-01 00:00:00'

-- ❌ WRONG: Creates gaps
BUSINESS_THRU = '2025-10-31 23:59:59'
```

### Update Patterns - CRITICAL

**AsOf Query + Property Modification Pattern** (CORRECT):
```kotlin
// 1. Find record valid at effective date using AsOf query
val operation = EmployeeAssignmentFinder.employeeId().eq(id)
    .and(EmployeeAssignmentFinder.businessDate().eq(businessTimestamp))
    .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
val existing = EmployeeAssignmentFinder.findOne(operation)

// 2. Modify properties - Reladomo handles bitemporal chaining automatically
existing.departmentId = newDepartmentId
existing.positionId = newPositionId
existing.updatedBy = updatedBy

// Reladomo automatically:
// - Splits business periods if needed
// - Terminates old version (PROCESSING_THRU = now)
// - Creates new version (PROCESSING_FROM = now, PROCESSING_THRU = infinity)
// - Preserves BUSINESS_FROM/THRU from existing record
```

**Key Points**:
- `.businessDate().eq(Timestamp)` - Finds records where date falls within [BUSINESS_FROM, BUSINESS_THRU)
- `.processingDate().equalsInfinity()` - Finds current version (PROCESSING_THRU = infinity)
- `.equalsEdgePoint()` - ONLY for retrieving ALL historical records (debugging/reporting)

**Common Mistakes**:

❌ **WRONG - Using insertWithIncrementUntil()**:
```kotlin
// This does NOT work when BUSINESS_THRU is already at infinity
val assignment = EmployeeAssignment(businessFrom, infinityDate)
assignment.insertWithIncrementUntil(businessFrom)  // Creates overlapping periods!
```

❌ **WRONG - Manual UPDATE/INSERT**:
```kotlin
// Bypasses Reladomo's bitemporal logic
jdbcTemplate.update("UPDATE ... SET PROCESSING_THRU = ?")
jdbcTemplate.update("INSERT INTO ... VALUES (...)")
```

**Why AsOf Query Pattern Works**:
- Reladomo tracks object state and generates proper SQL
- Automatically handles business period splitting when properties change
- Preserves BUSINESS_FROM/THRU, only changes PROCESSING_FROM/THRU
- Creates continuation records when splitting business periods

### Record Splitting Example

When Alice registers "Sales Manager from 2025-11-01" on 2025-10-26:

**Before** (ID=6):
```
Engineering Manager, BUS[2025-09-30, 9999), PROC[2025-05-31, 9999)
```

**After** (Reladomo splits into 3 records):
```
ID=6: Eng Manager, BUS[2025-09-30, 2025-10-27), PROC[2025-05-31, 2025-10-26)  # Past version
ID=6: Eng Manager, BUS[2025-09-30, 2025-11-01), PROC[2025-10-26, 9999)        # Continuation
ID=6: Sales Manager, BUS[2025-11-01, 9999),     PROC[2025-10-26, 9999)        # New
```

### Correction vs New Price

**Correction** (fixing mistake): BUSINESS_THRU stays 9999, only PROCESSING_THRU changes
**New Price** (future change): BUSINESS_THRU splits, both time dimensions change

## Reladomo Configuration

### MithraRuntimeConfig.xml
```xml
<MithraRuntime>
    <ConnectionManager className="io.github.reladomokotlin.spring.connection.H2ConnectionManager">
        <Property name="dataSourceName" value="dataSource"/>
        <MithraObjectConfiguration className="..." cacheType="partial"/>
    </ConnectionManager>
</MithraRuntime>
```

### ReladomoConfig Spring Bean
```kotlin
@Configuration
class ReladomoConfig {
    @Bean
    fun mithraManager(): MithraManager {
        val manager = MithraManagerProvider.getMithraManager()
        manager.setTransactionTimeout(120)
        val configStream = javaClass.classLoader.getResourceAsStream("MithraRuntimeConfig.xml")
        manager.readConfiguration(configStream)
        manager.fullyInitialize()  // REQUIRED
        return manager
    }
}
```

## Temporal Patterns

### When to Use Each Pattern
- **Non-temporal**: Reference data with no change history (Categories)
- **Unitemporal**: Master data with change history (Products, org structures) - PROCESSING_FROM/THRU only
- **Bitemporal**: Transaction data with full temporal tracking (Prices, salaries) - BUSINESS + PROCESSING

### Unitemporal Configuration
```xml
<AsOfAttribute name="processingDate"
               fromColumnName="PROCESSING_FROM"
               toColumnName="PROCESSING_THRU"
               toIsInclusive="false"
               isProcessingDate="true"  <!-- REQUIRED -->
               infinityDate="[...]"/>
```

### Temporal JOINs
```sql
-- ✅ CORRECT: Correlate temporal entities based on processing time overlap
SELECT pp.*, p.NAME
FROM PRODUCT_PRICES pp
LEFT JOIN PRODUCTS p ON pp.PRODUCT_ID = p.ID
    AND pp.PROCESSING_FROM >= p.PROCESSING_FROM
    AND pp.PROCESSING_FROM < p.PROCESSING_THRU
```

## Common Implementation Patterns

### Database Schema Consistency
- **Non-temporal**: No temporal columns
- **Unitemporal**: PROCESSING_FROM/THRU, PRIMARY KEY (ID, PROCESSING_FROM)
- **Bitemporal**: BUSINESS_FROM/THRU + PROCESSING_FROM/THRU, PRIMARY KEY (ID, BUSINESS_FROM, PROCESSING_FROM)

### Entity Relationships
**CRITICAL**: Join conditions must reference attributes that exist on both entities.
```xml
<!-- ✅ CORRECT: Product is non-temporal, no businessDate -->
<Relationship name="product" relatedObject="Product">
    this.productId = Product.id
</Relationship>

<!-- ❌ WRONG: References non-existent businessDate -->
<Relationship name="product" relatedObject="Product">
    this.productId = Product.id and this.businessDate = Product.businessDate
</Relationship>
```

### Full-Stack Feature Checklist
1. Database Schema (schema.sql) - Add column, update INSERTs
2. Reladomo XML - Add attribute, regenerate code
3. Controllers/DTOs - Update save/return logic
4. DatabaseViewController (if exists) - Update SQL queries manually
5. Frontend - Update types, UI fields, styling

## Reference Documentation

- [Reladomo Tour](https://goldmansachs.github.io/reladomo-kata/reladomo-tour-docs/tour-guide.html)
- [ISO 8601 Standard](https://en.wikipedia.org/wiki/ISO_8601)
- [MDN: Date.toLocaleString()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toLocaleString)
- [Spring Boot Jackson Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.data.spring.jackson.time-zone)

---

# ChronoStaff-Specific Guidelines

**NOTE**: This section contains guidance specific to the **ChronoStaff** demo application (`/chronostaff/`). These guidelines apply ONLY to ChronoStaff.

## Design Philosophy

### Core Principle: One Time Dimension for Users

Users only think about **"When is this effective?"** (Business Date). System automatically manages Processing Time.

**What Users See**:
- Single time concept: "Effective Date" (実効日)
- Simple forms: "When should this change take effect?"

**What System Handles**:
- Processing Time (PROCESSING_FROM/THRU) - automatic
- Bitemporal chaining - automatic via AsOf query pattern
- Audit trail preservation - automatic

## Technical Patterns

### Effective Date → Business Date Conversion

**Frontend** (User input JST → UTC):
```typescript
const effectiveDate = "2025-04-01";  // User input
const utcTimestamp = new Date(effectiveDate + "T00:00:00+09:00").toISOString();
// Result: "2025-03-31T15:00:00Z"
```

**Backend** (UTC → BUSINESS_FROM):
```kotlin
val effectiveDate = LocalDate.parse(dto.effectiveDate)
val businessFrom = effectiveDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))
assignment.businessFromAttribute = Timestamp.from(businessFrom)
assignment.businessThruAttribute = Timestamp.from(Instant.parse("9999-12-01T23:59:00Z"))
// PROCESSING_FROM/THRU automatically managed by Reladomo
```

### Transfer/Change Pattern

**Use AsOf query to find record, then modify properties**:

```kotlin
// 1. Find record valid at effective date
val operation = EmployeeAssignmentFinder.employeeId().eq(id)
    .and(EmployeeAssignmentFinder.businessDate().eq(businessTimestamp))
    .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
val existing = EmployeeAssignmentFinder.findOne(operation)

// 2. Modify properties - Reladomo handles bitemporal chaining
existing.departmentId = newDepartmentId
existing.positionId = newPositionId
existing.updatedBy = updatedBy
```

### Correction Pattern

**Corrections preserve BUSINESS_FROM/THRU, only PROCESSING_FROM/THRU changes**:

```kotlin
// Find record at target business date
val operation = EmployeeAssignmentFinder.employeeId().eq(id)
    .and(EmployeeAssignmentFinder.businessDate().eq(targetBusinessDate))
    .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
val existing = EmployeeAssignmentFinder.findOne(operation)

// Update business data - Reladomo creates new processing version
existing.departmentId = correctedDepartmentId
existing.positionId = correctedPositionId
existing.updatedBy = correctorEmail
```

## UI/UX Principles

1. **Progressive Disclosure**: Simple forms first, advanced features behind extra clicks
2. **Clear Mental Model**: One concept - "When is this effective?"
3. **Safety Rails**: Confirmation dialogs, preview before commit
4. **Helpful Defaults**: Effective Date defaults to "today" or "hire date"
5. **Visual Feedback**: Timeline visualizations, color coding (past/current/future)

## Success Metrics

- **User Understanding**: Users should NOT know about "Processing Time"
- **System Correctness**: No gaps in temporal data, complete audit trail
- **Demo Effectiveness**: Complexity hidden but power demonstrated

## Reference

- **CRUD Design**: See `/chronostaff/CRUD_DESIGN.md` for comprehensive UI/UX design
- **Reladomo Tour**: https://goldmansachs.github.io/reladomo-kata/reladomo-tour-docs/tour-guide.html
