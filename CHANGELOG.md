# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.1] - 2025-10-22

### Added
- Initial release of Reladomo Kotlin wrapper
- Core module with base repository and entity interfaces
- Code generator for creating Kotlin wrappers from Reladomo XML
- Spring Boot auto-configuration and integration
- Gradle plugin for build-time code generation
- Support for bitemporal entities and queries
- Type-safe Query DSL for Kotlin
- Automatic ID generation with SimulatedSequence support
- GenericSequenceObjectFactory for simplified sequence management
- Multi-database support (H2, PostgreSQL, MySQL, Oracle)
- Comprehensive test coverage with sample application
- Full documentation and examples
- Annotation-based entity configuration with @ReladomoEntity
- Automatic entity scanning and registration

### Features
- **Core Module** (`reladomo-kotlin-core`)
  - BiTemporalEntity interface
  - AbstractBiTemporalRepository with CRUD operations
  - Type conversion adapters (Timestamp ↔ Instant)
  - Query DSL foundation
  - Sequence generation support

- **Code Generator** (`reladomo-kotlin-generator`)
  - XML parsing for Reladomo object definitions
  - Kotlin data class generation with null safety
  - Repository class generation
  - Query DSL generation per entity
  - Comprehensive type mapping support

- **Spring Boot Integration** (`reladomo-kotlin-spring-boot`)
  - Auto-configuration with @EnableAutoConfiguration
  - Transaction management integration
  - Connection manager registry
  - Repository scanning and registration
  - Programmatic configuration support
  - GenericSequenceObjectFactory for SimulatedSequence
  - Spring Data-style repository pattern

- **Gradle Plugin** (`reladomo-kotlin-gradle-plugin`)
  - Automatic code generation task
  - Configurable source and output directories
  - Integration with Kotlin compilation

### Changed
- Project renamed from `kotlin-reladomo` to `reladomo-kotlin` for better discoverability
- Migrated testing framework from MockK to Mockito for better Java 17/21 compatibility
- Group ID updated to `io.github.itohiro73` for Maven Central publishing

### Infrastructure
- **Maven Central Publishing**
  - Configured publishing via Central Portal API (2025+ requirement)
  - Integrated Vanniktech Maven Publish Plugin 0.30.0
  - Conditional GPG signing support
  - Automated release workflow via GitHub Actions

- **CI/CD Improvements**
  - Enabled Gradle daemon for 2-3x faster builds
  - Optimized CI workflow to skip sample module (reduce build time)
  - Implemented build caching for improved performance
  - Multi-version testing (Java 17 & 21)
  - Automated code quality checks and dependency scanning

### Fixed
- Java instrumentation issues with Gradle daemon and mocking frameworks
- Bitemporal update/delete operations to use proper entity fetching
- H2 connection manager configuration
- All compilation warnings in generated code

### Known Issues
- Kotlin Coroutines support not yet implemented
- Timeline API for historical queries pending
- Reactive Streams integration planned for future release
- Spring Boot module tests temporarily skipped in CI due to Java instrumentation warnings (tests pass locally)

### Documentation
- Comprehensive bitemporal guide with examples
- Spring Boot integration documentation
- Annotation-based configuration guide
- XML to annotation migration guide
- Release methodology and CI/CD documentation

[Unreleased]: https://github.com/itohiro73/reladomo-kotlin/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/itohiro73/reladomo-kotlin/releases/tag/v0.0.1
