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