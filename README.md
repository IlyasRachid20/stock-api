# Stock API

[![CI](https://github.com/IlyasRachid20/stock-api/actions/workflows/ci.yml/badge.svg)](https://github.com/IlyasRachid20/stock-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)

A REST API for small shops to manage **customers, products, sales and stock**.
Every sale updates the stock automatically, overselling is impossible, and every
error comes back with a clear message.

## Highlights

- **Stock always in sync:** selling an item takes it out of stock; deleting an item or a whole sale puts it back.
- **No overselling, even under load:** the product row is locked while its stock changes (by a sale or a product update), so two requests at the same moment can't both take the last unit or overwrite each other's stock.
- **Fast lists:** related rows are loaded in batches, so a page of sales takes at most 5 queries instead of one per sale, item and product (41 before). A test fails if this regresses.
- **Standard HTTP semantics:** `201 Created` on create, `204 No Content` on delete.
- **Clear, consistent errors:** every error has the same JSON shape, including malformed JSON, wrong types, unknown URLs and wrong methods: `400` with a message per invalid field, `404` for unknown ids, `409` for business conflicts (not enough stock, email already used), and a generic `500` that never leaks internals.
- **Sales with totals:** each sale returns its items and a computed total.
- **Versioned database schema:** tables are created by Flyway migration scripts; Hibernate only validates them at startup and never changes the database.
- **Pagination, search and sorting** on every list, with a hard cap of 100 items per page.
- **Clean API contract:** requests and responses are dedicated DTOs (Java records), separate from the database entities, so internal fields never leak and the database can change without breaking clients.
- **Interactive documentation:** Swagger UI lists every endpoint and lets you try it from the browser.
- **86 automated tests** run on every pull request with GitHub Actions, on H2 **and on a real PostgreSQL**.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation) |
| Database | PostgreSQL, schema managed by Flyway (H2 in-memory for fast tests) |
| API docs | springdoc-openapi 3 / Swagger UI |
| Tests | JUnit 5, MockMvc, AssertJ |
| CI | GitHub Actions: tests on H2 and PostgreSQL, plus a Docker Compose smoke test |
| Packaging | Docker multi-stage image (non-root), Docker Compose |
| Build | Maven (wrapper included) |

## Data model

```mermaid
erDiagram
    CUSTOMER ||--o{ SALE : places
    SALE ||--o{ SALE_ITEM : contains
    PRODUCT ||--o{ SALE_ITEM : "sold as"

    CUSTOMER {
        long id
        string name
        string email "unique, optional"
        string phone
    }
    PRODUCT {
        long id
        string name
        decimal price
        int quantity "current stock"
    }
    SALE {
        long id
        datetime saleDate
    }
    SALE_ITEM {
        long id
        int quantity
        decimal unitPrice "defaults to product price"
    }
```

## API overview

| Resource | Endpoints |
|---|---|
| Customers | `GET/POST /api/customers` · `GET/PUT/DELETE /api/customers/{id}` |
| Products | `GET/POST /api/products` · `GET/PUT/DELETE /api/products/{id}` |
| Sales | `GET/POST /api/sales` · `GET/DELETE /api/sales/{id}` |
| Sale items | `GET/POST /api/sale-items` · `GET/DELETE /api/sale-items/{id}` |

Full details, request bodies and a **Try it out** button: http://localhost:8080/swagger-ui.html

### Lists: pagination, search and sorting

Every list endpoint is paginated (20 items per page by default, at most 100) and accepts `page`, `size` and `sort`:

| Endpoint | Filter |
|---|---|
| `GET /api/products` | `search`: part of the name, case-insensitive |
| `GET /api/customers` | `search`: part of the name or email |
| `GET /api/sales` | `customerId` (newest sales first by default) |
| `GET /api/sale-items` | `saleId` |

```http
GET /api/products?search=galaxy&sort=price,desc&page=0&size=20
```
```json
{
  "content": [
    {"id": 1, "name": "Galaxy S26", "price": 9500.00, "quantity": 10}
  ],
  "page": {"size": 20, "number": 0, "totalElements": 1, "totalPages": 1}
}
```

### Example: selling a product

```http
POST /api/sales
{"customerId": 1}

POST /api/sale-items
{"saleId": 1, "productId": 1, "quantity": 3}
```

`unitPrice` is optional and defaults to the product's current price.

The product's stock goes from 10 to 7, and the sale now shows its total:

```http
GET /api/sales/1
```
```json
{
  "id": 1,
  "customer": {"id": 1, "name": "Ahmed"},
  "saleDate": "2026-09-29T17:11:37",
  "items": [
    {"id": 1, "saleId": 1, "product": {"id": 1, "name": "Galaxy S26"}, "quantity": 3, "unitPrice": 9500.00, "lineTotal": 28500.00}
  ],
  "total": 28500.00
}
```

### Error responses

Every error uses one of two shapes: `{"error": "message"}`, or `{"errors": {"field": "message"}}` for invalid request bodies.

```http
POST /api/sale-items   (quantity 50, only 7 left)
409 Conflict
{"error": "Not enough stock for product 'Galaxy S26': 7 available, 50 requested"}

GET /api/products/abc
400 Bad Request
{"error": "Invalid value 'abc' for parameter 'id'"}

GET /api/products?sort=color
400 Bad Request
{"error": "Cannot sort by unknown field 'color'"}

POST /api/products     {"name": "", "price": -5}
400 Bad Request
{"errors": {"name": "must not be blank", "price": "must be greater than or equal to 0.00"}}
```

## Getting started

### Option 1: Docker (recommended)

Only [Docker](https://www.docker.com/products/docker-desktop/) is needed: no Java, no PostgreSQL install.

```bash
git clone https://github.com/IlyasRachid20/stock-api.git
cd stock-api
cp .env.example .env        # then edit the password in .env
docker compose up --build
```

Open http://localhost:8080/swagger-ui.html. The database is kept in a Docker volume between restarts; `docker compose down --volumes` deletes it.

### Option 2: Run with Java

**Prerequisites:** JDK 21 and a running PostgreSQL database. No Maven install is needed: use `./mvnw` (or `mvnw.cmd` on Windows).

1. Clone the repository:
   ```bash
   git clone https://github.com/IlyasRachid20/stock-api.git
   cd stock-api
   ```
2. Point the app at your database with environment variables, so credentials never end up in Git:
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/stock_api
   export SPRING_DATASOURCE_USERNAME=your_user
   export SPRING_DATASOURCE_PASSWORD=your_password
   ```
   On Windows PowerShell use `$env:SPRING_DATASOURCE_URL = "..."`. In an IDE, set them in the run configuration.
   The database must exist but can be empty: Flyway creates the tables on first start.
3. Start the app:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Open http://localhost:8080/swagger-ui.html.

## Tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database, so they need no PostgreSQL and no credentials. In CI they also run against PostgreSQL 17, twice: on an empty database, and on one created before Flyway was added, to prove both upgrade paths. They cover the API end to end through MockMvc: validation, not-found and conflict errors, stock updates, sale totals, and the API documentation. The same suite runs on every pull request.

## Project structure

```
src/main/java/com/ilyas/stockapi
├── config/        OpenAPI (Swagger) setup
├── controller/    REST endpoints only: HTTP in, DTOs out, no business logic
├── dto/           Request and response records (the API contract)
├── entity/        JPA entities (database tables)
├── exception/     NotFound / Conflict / BadRequest, mapped to HTTP by the error handler
├── repository/    Spring Data JPA repositories
└── service/       Business rules and transactions (customers, products, sales and stock)

src/main/resources/db/migration
├── V1__create_tables.sql
└── V2__add_foreign_key_indexes.sql
```

## Roadmap

- [x] Request/response DTOs
- [x] Pagination and search
- [x] Flyway database migrations
- [x] Docker Compose
- [ ] JWT authentication with `ADMIN` / `CASHIER` roles
- [ ] Stock movement history and low-stock alerts
- [ ] Sales reports and CSV/PDF export
- [ ] Live demo
- [ ] Web dashboard

## Author

**Ilyas Rachid** · [GitHub @IlyasRachid20](https://github.com/IlyasRachid20)
