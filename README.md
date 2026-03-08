# Shortener

A URL shortener service built with Spring Boot. Shorten long URLs and redirect to the original destination.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose

## Running with Docker

### Build and start all services

```bash
docker compose up --build
```

This starts:
- **PostgreSQL** on port 5432
- **Shortener API** on port 8080

### Run in detached mode (background)

```bash
docker compose up --build -d
```

### Stop services

```bash
docker compose down
```

## Testing with curl

### 1. Health check

```bash
curl -s http://localhost:8080/actuator/health | jq
```

### 2. Create a user (optional, for user-associated URLs)

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```

Example response:
```json
{"id":1,"name":"John Doe","email":"john@example.com","createdAt":"2025-03-08T12:00:00Z","lastLogin":null}
```

### 3. Shorten a URL (without user)

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com"}'
```

Example response:
```json
{"shortUrl":"https://short.ly/r/abc123"}
```

### 4. Shorten a URL (with user)

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://github.com","userId":1}'
```

### 5. Follow redirect (resolve short URL)

The shorten response returns a URL like `https://short.ly/r/xyz`. For local testing, use `http://localhost:8080/r/{shortCode}` (extract the short code after `/r/`).

```bash
# Replace xyz with the short code from the shorten response (the part after /r/)
curl -L -w "\nRedirected to: %{url_effective}\n" http://localhost:8080/r/xyz
```

Or to see the redirect headers without following:

```bash
curl -I http://localhost:8080/r/xyz
```

## Running locally (without Docker)

Requires Java 17+ and PostgreSQL.

1. Start PostgreSQL with database `shortener`, user `postgres`, password `postgres`
2. Run the application:

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/shortener-0.0.1-SNAPSHOT.jar
```
