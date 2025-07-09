# Database Profile Configuration

This sample application supports two database profiles:

## H2 Database (Default - Mobile/Development)

The H2 profile is the default and is ideal for mobile devices or quick development:

```bash
# Run with H2 (default)
./gradlew :reladomo-kotlin-sample:bootRun

# Or explicitly specify H2 profile
./gradlew :reladomo-kotlin-sample:bootRun --args='--spring.profiles.active=h2'
```

**Features:**
- In-memory database (no setup required)
- H2 console available at http://localhost:8080/h2-console
- PostgreSQL compatibility mode enabled
- Auto-creates schema and sample data on startup

## PostgreSQL Database (Production/PC)

The PostgreSQL profile is recommended for PC development and production:

### 1. Start PostgreSQL using Docker Compose

```bash
cd reladomo-kotlin-sample
docker-compose up -d
```

This will start PostgreSQL on port 5432 with:
- Database: `reladomo_sample`
- Username: `postgres`
- Password: `postgres`

### 2. Run the application with PostgreSQL profile

```bash
./gradlew :reladomo-kotlin-sample:bootRun --args='--spring.profiles.active=postgres'
```

### 3. Stop PostgreSQL when done

```bash
docker-compose down

# To also remove the data volume:
docker-compose down -v
```

## Profile-Specific Configuration

### H2 Profile (`application-h2.yml`)
- Uses in-memory H2 database
- Enables H2 console
- Uses `H2ConnectionManager`

### PostgreSQL Profile (`application-postgres.yml`)
- Connects to PostgreSQL on localhost:5432
- Uses `PostgreSQLConnectionManager`
- Requires PostgreSQL to be running

## Environment Variables

You can override database settings using environment variables:

```bash
# For PostgreSQL
export SPRING_DATASOURCE_URL=jdbc:postgresql://myhost:5432/mydb
export SPRING_DATASOURCE_USERNAME=myuser
export SPRING_DATASOURCE_PASSWORD=mypass

# Then run with postgres profile
./gradlew :reladomo-kotlin-sample:bootRun --args='--spring.profiles.active=postgres'
```

## Testing Different Profiles

```bash
# Test with H2
./gradlew :reladomo-kotlin-sample:test

# Test with PostgreSQL (ensure PostgreSQL is running)
./gradlew :reladomo-kotlin-sample:test --args='--spring.profiles.active=postgres'
```