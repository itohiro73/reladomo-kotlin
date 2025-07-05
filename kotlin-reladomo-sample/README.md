# Kotlin Reladomo Sample Application

This sample demonstrates how to use the Kotlin Reladomo wrapper library with Spring Boot.

## Features Demonstrated

- Basic CRUD operations with bitemporal entities
- REST API endpoints
- Spring Boot integration
- Exception handling
- In-memory repository for testing

## Running the Application

```bash
./gradlew :kotlin-reladomo-sample:bootRun
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

The sample uses an H2 in-memory database with bitemporal schema. The schema is automatically created on startup from `schema.sql`.

## Code Generation

In a real application, the Kotlin wrapper classes (`OrderKt`) and repository would be generated automatically by the Gradle plugin from the Reladomo XML definition.

For this sample, we've included manual implementations to demonstrate the expected structure and behavior.