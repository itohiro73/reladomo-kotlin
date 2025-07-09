# Reladomo Kotlin Sample Application

This sample demonstrates how to use the Reladomo Kotlin wrapper library with Spring Boot.

## Features Demonstrated

- Basic CRUD operations with bitemporal entities
- REST API endpoints
- Spring Boot integration with auto-configuration
- Spring Data-style repositories with query methods
- Exception handling
- Multi-database support (H2, PostgreSQL, MySQL, Oracle)
- Automatic code generation from Reladomo XML
- Repository scanning with `@EnableReladomoRepositories`
- Bitemporal query support

## Prerequisites

- Java 17 or higher
- Docker and Docker Compose (for PostgreSQL)
- Gradle

## Running the Application

### 1. Start PostgreSQL Database

First, start the PostgreSQL database using Docker Compose:

```bash
cd reladomo-kotlin-sample
docker-compose up -d
```

This will start a PostgreSQL instance with:
- Database: `reladomo_sample`
- Username: `reladomo`
- Password: `reladomo123`
- Port: `5432`

The database will be automatically initialized with the schema and sample data from `src/main/resources/db/init.sql`.

### 2. Generate Code

From the project root directory, generate the Reladomo classes:

```bash
./gradlew :reladomo-kotlin-sample:generateReladomoCode
```

This generates:
- Java Reladomo classes in `build/generated/reladomo/java`
- Kotlin wrapper classes in `src/main/kotlin/io/github/reladomokotlin/sample/domain/kotlin`
- Repository classes in `src/main/kotlin/io/github/reladomokotlin/sample/domain/kotlin/repository`

### 3. Build and Run

```bash
./gradlew :reladomo-kotlin-sample:build
./gradlew :reladomo-kotlin-sample:bootRun
```

The application will start on port 8080.

## API Endpoints

### Get All Orders
```bash
curl http://localhost:8080/api/orders
```

### Get Order by ID
```bash
curl http://localhost:8080/api/orders/1
```

### Get Order As Of Specific Date
```bash
curl "http://localhost:8080/api/orders/1/asof?businessDate=2024-01-01T00:00:00Z"
```

### Create Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 100,
    "amount": 999.99,
    "status": "PENDING",
    "description": "New order"
  }'
```

### Update Order
```bash
curl -X PUT http://localhost:8080/api/orders/1 \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 100,
    "amount": 1999.99,
    "status": "PROCESSING",
    "description": "Updated order"
  }'
```

### Delete Order
```bash
curl -X DELETE http://localhost:8080/api/orders/1
```

### Get Orders by Customer
```bash
curl http://localhost:8080/api/orders/customer/100
```

## Database

The sample uses PostgreSQL database with bitemporal schema. The schema is automatically created when the Docker container starts from `src/main/resources/db/init.sql`.

## Code Generation

The Kotlin wrapper classes (`OrderKt`) and repository are generated automatically from the Reladomo XML definition in `src/main/resources/reladomo/Order.xml`.

## Known Issues and Next Steps

### Current Status
The application is configured to use PostgreSQL instead of H2 to avoid compatibility issues with Reladomo's schema handling. The connection manager implementation needs to be completed to properly extend Reladomo's base classes.

### Next Steps for Implementation
1. **Fix SpringConnectionManager compilation**: The `SpringConnectionManager` needs to properly extend either `SourcelessConnectionManager` or `ObjectSourceConnectionManager` from Reladomo. The current issue is that these base classes don't have accessible constructors in Kotlin.

2. **Alternative approaches to consider**:
   - Use Reladomo's built-in `XAConnectionManager` instead of custom implementation
   - Create a Java implementation of the connection manager
   - Use factory pattern to instantiate the connection manager

3. **Once connection manager is fixed**:
   - Verify all REST endpoints work correctly
   - Test bitemporal queries
   - Add more comprehensive integration tests

### Technical Notes
- PostgreSQL is used instead of H2 because H2 throws "Schemas not supported" errors with Reladomo
- The `SpringConnectionManager` uses a static DataSource holder pattern to work with Reladomo's connection manager instantiation
- The generated code uses `equalsEdgePoint()` for bitemporal queries, which may need adjustment

## Troubleshooting

### Connection Issues
If you encounter connection issues:
1. Ensure PostgreSQL container is running: `docker-compose ps`
2. Check logs: `docker-compose logs postgres`
3. Verify port 5432 is not already in use
4. Ensure database credentials match those in `application.yml`

### Build Issues
If the build fails:
1. Ensure you're using Java 17 or higher: `java -version`
2. Clean and rebuild: `./gradlew clean build`
3. Check that code generation completed successfully
4. Look for compilation errors in the generated code