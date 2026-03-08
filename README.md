# Shortener

A URL shortener service built with Spring Boot. Shorten long URLs and redirect to the original destination. Uses PostgreSQL for persistence, Redis for caching, and Bucket4j for rate limiting.

## Prerequisites

- **Java 17+** (for local development)
- **Maven** (or use included `./mvnw`)
- **Docker** and **Docker Compose** (for containerized setup)
- **PostgreSQL 16** (for local run) and **Redis 7** (for caching)

---

## Setup Instructions

### Option A: Docker (recommended)

1. Clone the repository and navigate to the project directory.
2. Ensure Docker and Docker Compose are installed.
3. Build and start all services:

```bash
docker compose up --build
```

This starts:
- **PostgreSQL** on port 5432
- **Redis** on port 6379
- **Shortener API** on port 8080

Run in detached mode (background):

```bash
docker compose up --build -d
```

Stop services:

```bash
docker compose down
```

### Option B: Local development

1. **Create the database** (PostgreSQL):

```bash
createdb shortener
# Or: psql -c "CREATE DATABASE shortener;"
```

2. **Start Redis** (required for caching):

```bash
redis-server
# Or via Homebrew: brew services start redis
```

3. **Run the application**:

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/shortener-0.0.1-SNAPSHOT.jar
```

---

## How to Run and Test the API

### Run the application

See [Setup Instructions](#setup-instructions) above.

### Run unit tests

```bash
./mvnw test
```

Tests use H2 in-memory database and do not require PostgreSQL or Redis.

### Health check

```bash
curl -s http://localhost:8080/actuator/health | jq
```

---

## Example Requests

### 1. Create a user (optional, for user-associated URLs)

**cURL:**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```

**Postman:**
- Method: `POST`
- URL: `http://localhost:8080/api/users`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{"name":"John Doe","email":"john@example.com"}
```

**Example response (201 Created):**
```json
{"id":1,"name":"John Doe","email":"john@example.com","createdAt":"2025-03-08T12:00:00Z","lastLogin":null}
```

---

### 2. Shorten a URL (anonymous, no user)

**cURL:**

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com"}'
```

**Postman:**
- Method: `POST`
- URL: `http://localhost:8080/shorten`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{"url":"https://example.com"}
```

**Example response (201 Created):**
```json
{"shortUrl":"http://localhost:8080/r/abc123"}
```

---

### 3. Shorten a URL (with user)

**cURL:**

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://github.com","userId":1}'
```

**Postman:**
- Method: `POST`
- URL: `http://localhost:8080/shorten`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{"url":"https://github.com","userId":1}
```

---

### 4. Resolve short URL (redirect)

Use the `shortCode` from the shorten response (the part after `/r/`).

**cURL (follow redirect):**
```bash
curl -L -w "\nRedirected to: %{url_effective}\n" http://localhost:8080/r/abc123
```

**cURL (inspect redirect headers only):**
```bash
curl -I http://localhost:8080/r/abc123
```

---

## Configuration Notes

Configuration is via `application.properties` and environment variables. No API key is required.

### Environment variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/shortener` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `SNOWFLAKE_MACHINE_ID` | Machine ID for Snowflake ID generator (0–1023) | `0` |
| `CACHE_URL_MAPPINGS_TTL` | Cache TTL for URL mappings (seconds) | `86400` (24h) |

### Rate limiting

Anonymous shorten requests (no `userId`) are rate-limited. Override in `application.properties` or via system properties:

- `rate-limit.anonymous-shorten.capacity` — bucket capacity (default: 10)
- `rate-limit.anonymous-shorten.refill-per-second` — refill rate (default: 10)

### Short URL base

Short URLs are returned as `http://localhost:8080/r/{shortCode}`. This base URL is hardcoded in `ShortenerService`; change it there for production deployments.
