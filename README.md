# stock-api

REST API for stock management, built with Spring Boot.

## Tech stack

- Java 21
- Spring Boot 4.1 (Spring Web MVC, Spring Data JPA)
- PostgreSQL
- Maven (via the included Maven Wrapper)

## Prerequisites

- JDK 21
- A running PostgreSQL instance

No global Maven install is needed: use `./mvnw` (or `mvnw.cmd` on Windows).

## Configuration

The database connection is not configured yet. Add these properties to
`src/main/resources/application.properties` (or set them as environment
variables) before running the app or the tests:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/stock_api
spring.datasource.username=your_user
spring.datasource.password=your_password
```

## Build and run

```bash
./mvnw spring-boot:run
```

The API starts on http://localhost:8080.

## API documentation

With the app running, open http://localhost:8080/swagger-ui.html to see every
endpoint and try requests from the browser. The raw OpenAPI description is at
http://localhost:8080/v3/api-docs.

## Tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database, so they don't need PostgreSQL
or any `spring.datasource.*` settings.
