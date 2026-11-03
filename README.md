# PayFlow Payment API

A payment processing REST API built with Java 21 and Spring Boot. This project demonstrates transactional persistence, payment gateway abstraction, and database-backed idempotency for payment creation.

## Features

- Payment creation and lookup REST endpoints.
- Idempotency-key enforcement backed by a PostgreSQL `UNIQUE` constraint.
- Atomic idempotency-key reservation with `INSERT ... ON CONFLICT DO NOTHING`.
- Pessimistic locking to prevent concurrent processing of the same payment.
- SHA-256 request fingerprinting to detect payload changes for a reused key.
- Bean Validation for customer, amount, and currency fields.
- Flyway database migrations.
- Pageable payment listing with customer and status filters.
- Deterministic fake payment gateway configurable without code changes.
- OpenAPI/Swagger documentation.

## Requirements

- JDK 21
- Maven 3.9+
- Docker and Docker Compose

## Running locally

Start PostgreSQL:

```bash
docker compose up -d
```

Start the application:

```bash
mvn spring-boot:run
```

Flyway creates and validates the database schema automatically.

Swagger UI is available at:

<http://localhost:8080/swagger-ui/index.html>

## Payment gateway outcomes

The fake gateway is deterministic and can be configured through an environment variable:

```bash
PAYFLOW_GATEWAY_OUTCOME=REJECTED mvn spring-boot:run
```

Supported values are:

- `APPROVED`
- `REJECTED`
- `FAILED`

The default outcome is `APPROVED`.

## API examples

Create a payment:

```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: order-12345' \
  -d '{"customerId":"550e8400-e29b-41d4-a716-446655440000","amount":99.90,"currency":"BRL"}'
```

List approved payments:

```bash
curl 'http://localhost:8080/api/payments?status=APPROVED&page=0&size=20'
```

Get a payment:

```bash
curl http://localhost:8080/api/payments/{id}
```

## Idempotency

The `Idempotency-Key` header is required for payment creation.

Repeating a request with the same key and the same payload returns the original payment. Reusing the key with a different payload returns `409 Conflict`.

The application first reserves the key atomically and then loads the payment with a pessimistic lock before calling the gateway. This prevents two application instances from processing the same key concurrently.

The gateway call is synchronous and runs inside the transaction so concurrent requests wait for the final result. A process failure after an external gateway approval but before the database commit can still require reconciliation. A production integration should use an idempotent external gateway, an outbox, and reconciliation workflows.

## Project structure

The code is organized into the following layers:

- `controller`: REST endpoints and HTTP contracts.
- `service`: payment business rules and orchestration.
- `repository`: Spring Data persistence operations.
- `entity`: JPA payment model.
- `dto`: API request and response records.
- `enums`: payment states.
- `exception`: domain exceptions and global error handling.
- `config`: OpenAPI configuration.

The domain depends on the `PaymentGateway` interface rather than a concrete provider, so the fake gateway can be replaced by a real integration without changing the service contract.

## Testing

Run the unit tests with:

```bash
mvn test
```

The tests cover approved, rejected, and failed outcomes, idempotent retries, payload conflicts, missing keys, and missing payments.

Integration tests with Testcontainers can be added to validate PostgreSQL constraints, transaction behavior, and real concurrent requests.

## Future improvements

For higher transaction volumes, the next steps would be:

- Asynchronous gateway processing.
- Kafka or another event broker when event volume and decoupling requirements justify it.
- Outbox-based event publication.
- Structured logs, metrics, tracing, and correlation IDs.
- Gateway reconciliation and retry workflows.
- Authentication, authorization, and rate limiting.
- Additional PostgreSQL indexes based on production query patterns.

These improvements are intentionally outside the scope of this MVP.
