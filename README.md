# User Sync Service

A Spring Boot microservice that consumes user change events from Kafka (e.g., Debezium CDC messages) and synchronizes users to Keycloak via the Admin REST API using client-credentials OAuth2.

This README documents the stack, requirements, setup and run commands, scripts, environment variables, tests, project structure, and licensing notes. Unknowns are explicitly marked as TODOs.

## Stack
- Language: Java 21
- Frameworks/Libraries:
  - Spring Boot 3.5.5
  - Spring Kafka
  - Spring Security OAuth2 Client
  - Spring Cloud Circuit Breaker (Resilience4j)
  - Lombok
  - ICU4J
- Build/Package manager: Maven (mvnw wrapper included)
- Containerization: Docker (multi-stage build)
- Orchestration (local dev): Docker Compose

## Overview
- Listens to a Kafka topic for Debezium-style payloads describing user CRUD operations.
- Maps Debezium payloads to internal UserData model and syncs changes to Keycloak:
  - CREATE/READ/UPDATE -> create or update user in Keycloak
  - DELETE -> delete user in Keycloak
- Uses OAuth2 client credentials to obtain an access token to call Keycloak Admin endpoints.

Entry point:
- Main class: `com.verifix.usersync.UserSyncServiceApplication`
- Kafka listener: `com.verifix.usersync.service.KafkaConsumerService` (topic configured via `app.kafka.topic`)

## Requirements
- Java 21 (JDK)
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Docker 24+ (optional, for container build)
- Docker Compose v2+ (optional, for local orchestration)

## Setup
1. Clone the repository
   git clone <your-fork-or-origin-url>
   cd usersync

2. Configure environment
   - Copy `template.env` to `.env` and fill in values, or export env vars in your shell.
   - See the Environment Variables section below for details.

3. Build (without running tests)
   ./mvnw -DskipTests package

4. Build (with tests)
   ./mvnw clean verify

## Running
You can run locally on your machine, via Docker, or with Docker Compose.

### Run locally (JAR)
- Build the jar:
  ./mvnw -DskipTests package
- Run the app (ensure required env vars are set):
  java -jar target/usersync-0.0.4.jar
  
  Note: the final JAR name follows the version in `pom.xml` (<version>0.0.4</version> at the time of writing). If it changes, adjust the filename accordingly.

### Run locally (Spring Boot plugin)
- Start the app directly with Maven:
  ./mvnw spring-boot:run

### Run with Docker
- Build the image (multi-stage Dockerfile):
  docker build -t usersync:local .
- Run the container (pass env vars):
  docker run --rm -p 8080:8080 \
    -e KAFKA_BROKERS=host.docker.internal:9092 \
    -e KAFKA_USERNAME=... \
    -e KAFKA_PASSWORD=... \
    -e KAFKA_CLIENT_ID=user-sync-client \
    -e KAFKA_GROUP_ID=user-sync-group \
    -e KAFKA_TOPIC=user-changes \
    -e KAFKA_FROM_BEGINNING=earliest \
    -e KEYCLOAK_URL=http://localhost:8080 \
    -e KEYCLOAK_REALM=birunix \
    -e KEYCLOAK_CLIENT_ID=... \
    -e KEYCLOAK_CLIENT_SECRET=... \
    usersync:local

### Run with Docker Compose
- Prepare a `.env` file (based on `template.env`).
- Start:
  docker compose up --build
- Service exposes port `8080`. Note: this service primarily runs as a background Kafka consumer; no public HTTP endpoints are defined by default.

## Scripts and Developer Commands
- Build: `./mvnw -DskipTests package`
- Build (clean + tests): `./mvnw clean verify`
- Run: `./mvnw spring-boot:run`
- Tests: `./mvnw test`
- Docker build: `docker build -t usersync:local .`
- Docker Compose (dev): `docker compose up --build`

## Environment Variables
Values are read by Spring from `application.yaml` with environment overrides.

Kafka
- KAFKA_BROKERS (required) — Kafka bootstrap servers (e.g., `localhost:9092`)
- KAFKA_USERNAME — SASL/PLAIN username
- KAFKA_PASSWORD — SASL/PLAIN password
- KAFKA_CLIENT_ID — defaults to `user-sync-client` (maps to `app.kafka.client-id`)
- KAFKA_GROUP_ID — defaults to `user-sync-group`
- KAFKA_TOPIC — topic to consume (maps to `app.kafka.topic`), defaults to `user-changes`
- KAFKA_FROM_BEGINNING — consumer offset reset, defaults to `earliest`

Keycloak / OAuth2
- KEYCLOAK_URL (required) — base URL of Keycloak server (e.g., `http://localhost:8080`)
- KEYCLOAK_REALM (required) — realm name (default `birunix`)
- KEYCLOAK_CLIENT_ID (required) — OAuth2 client id used for client credentials
- KEYCLOAK_CLIENT_SECRET (required) — OAuth2 client secret

Notes
- The service uses OAuth2 client credentials to get tokens from: `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`.
- application.yaml sets Kafka security protocol to SASL_PLAINTEXT with PLAIN mechanism by default; adjust as needed for your broker.
- TODO: Confirm whether the default `app.keycloak.base-url` fallback in application.yaml contains a typo (`locahost` vs `localhost`). Prefer setting KEYCLOAK_URL explicitly.

## Configuration Highlights
- app.tracked-columns — Debezium columns used to decide whether an event is relevant:
  - COMPANY_ID, USER_ID, NAME, LOGIN, PASSWORD, EMAIL
- Consumer logs info about processed operations and skips messages without relevant changes.

## Tests
- To run unit tests:
  ./mvnw test
- Current tests:
  - `src/test/java/com/verifix/usersync/UserSyncServiceApplicationTests.java`
- Test reports are generated under `target/surefire-reports/`.

## Project Structure
- Dockerfile — multi-stage Docker build (build with Maven, run on JRE)
- compose.yaml — local Docker Compose setup passing env vars to the service
- mvnw, mvnw.cmd — Maven wrapper scripts
- pom.xml — Maven build configuration
- src/main/java/com/verifix/usersync/
  - UserSyncServiceApplication.java — main application entry point
  - config/ — Spring configuration (ApplicationProperties, OAuth2, Kafka error handling, RestTemplate)
  - service/ — Kafka consumer, Keycloak service, token service, sync logic
  - mapper/ — message and Keycloak mapping logic
  - model/ — domain models and Debezium/Keycloak DTOs
- src/main/resources/
  - application.yaml — application config with env var placeholders
  - logback-spring.xml — logging configuration
- src/test/java/ — tests
- template.env — example environment file
- VERSION.md — versioning notes

## Logging
- Default log level for Spring Kafka: INFO
- Custom package logging examples in `application.yaml`
- Logback pattern is configured for console output; logs directory is present (`logs/usersync.log`) but file appender configuration depends on `logback-spring.xml`.

## License
- TODO: Add a LICENSE file or specify the license for this project. If a license exists elsewhere, update this section accordingly.

## Versioning and Releases
- Project version is managed in `pom.xml` (<version>0.0.4</version> at the time of writing).
- Artifacts publishing is configured to GitHub Packages in `distributionManagement`.

## Troubleshooting
- Kafka connectivity/auth: verify KAFKA_BROKERS, KAFKA_USERNAME/PASSWORD, and security protocol/mechanism per your cluster.
- Keycloak auth: verify KEYCLOAK_URL/REALM/CLIENT_ID/CLIENT_SECRET. Ensure the client has permissions to call Admin endpoints.
- Message parsing: service expects Debezium-like envelope; unknown or malformed messages are logged and skipped.

## Roadmap / TODOs
- TODO: Document the precise Debezium message schema and example payloads used by this service.
- TODO: Clarify any HTTP endpoints (if added in the future; currently the service acts as a background consumer).
- TODO: Add integration tests against a test Keycloak and Kafka broker (e.g., Testcontainers).
