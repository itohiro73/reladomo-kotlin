# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.1] - 2025-01-09

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

### Features
- **Core Module** (`reladomo-kotlin-core`)
  - BiTemporalEntity interface
  - AbstractBiTemporalRepository with CRUD operations
  - Type conversion adapters (Timestamp ↔ Instant)
  - Query DSL foundation

- **Code Generator** (`reladomo-kotlin-generator`)
  - XML parsing for Reladomo object definitions
  - Kotlin data class generation with null safety
  - Repository class generation
  - Query DSL generation per entity

- **Spring Boot Integration** (`reladomo-kotlin-spring-boot`)
  - Auto-configuration with @EnableAutoConfiguration
  - Transaction management integration
  - Connection manager registry
  - Repository scanning and registration
  - Programmatic configuration support

- **Gradle Plugin** (`reladomo-kotlin-gradle-plugin`)
  - Automatic code generation task
  - Configurable source and output directories
  - Integration with Kotlin compilation

### Known Issues
- Kotlin Coroutines support not yet implemented
- Timeline API for historical queries pending
- Reactive Streams integration planned for future release

[Unreleased]: https://github.com/reladomo-kotlin/reladomo-kotlin/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/reladomo-kotlin/reladomo-kotlin/releases/tag/v0.0.1