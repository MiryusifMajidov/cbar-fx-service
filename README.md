# cbar-fx-service

A service that pulls the official AZN exchange rates from the Central Bank of Azerbaijan (CBAR), stores them in PostgreSQL and serves them over a REST API: daily rates, history, period statistics, day-over-day changes and currency conversion.

CBAR publishes rates as XML at `https://www.cbar.az/currencies/{dd.MM.yyyy}.xml`. The service downloads that file on a schedule, upserts the values and keeps a log of every sync attempt.

## Stack

- Java 17, Spring Boot 3.5
- PostgreSQL, Flyway
- Plain SQL through `JdbcClient`, no JPA
- `RestClient` + Jackson XML for the CBAR feed, Spring Retry for failed calls
- Caffeine cache
- ShedLock, so the scheduled sync runs on only one instance
- Bucket4j for per-client rate limits
- springdoc-openapi
- JUnit 5, Mockito, MockMvc, Testcontainers

## Running locally

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

To run everything in containers instead, use `docker compose up --build`.

Swagger UI: http://localhost:8080/swagger-ui.html

An empty database has no rates, so pull some history first:

```bash
curl -X POST "localhost:8080/api/v1/admin/sync/backfill?from=2026-09-01&to=2026-10-15" \
  -H "X-API-Key: local-admin-key"
```

### Configuration

| Variable | Default |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `fx` |
| `DB_USER` / `DB_PASSWORD` | `fx` / `fx` |
| `FX_ADMIN_KEY` | `local-admin-key` |
| `PORT` | `8080` |

## Authentication

Every `/api/**` request needs an `X-API-Key` header.

- The admin key comes from `FX_ADMIN_KEY`. Only the admin key can call `/api/v1/admin/**`.
- Client keys are created through the admin API. Only a SHA-256 hash is stored, so a key is shown once, when it is created.

```bash
curl -X POST localhost:8080/api/v1/admin/clients \
  -H "X-API-Key: local-admin-key" \
  -H "Content-Type: application/json" \
  -d '{"name": "mobile-app", "requestsPerMinute": 120}'
```

Every client has its own requests-per-minute limit. The remaining quota comes back in `X-Rate-Limit-Remaining`. Once the limit is reached, the API returns `429` with a `Retry-After` header.

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/currencies` | currencies and bank metals |
| GET | `/api/v1/rates?date=` | all rates for a date; if that date has none, the last published day before it |
| GET | `/api/v1/rates/changes?date=` | change against the previous published day |
| GET | `/api/v1/rates/{code}?from=&to=` | history, last 30 days by default |
| GET | `/api/v1/rates/{code}/latest` | latest stored rate |
| GET | `/api/v1/rates/{code}/stats?from=&to=` | min, max, average and change % |
| GET | `/api/v1/rates/{code}/monthly?year=` | monthly averages |
| GET | `/api/v1/convert?from=&to=&amount=&date=` | conversion through the AZN cross rate |
| POST | `/api/v1/admin/sync?date=` | sync one day |
| POST | `/api/v1/admin/sync/backfill?from=&to=` | sync a range |
| GET | `/api/v1/admin/sync/logs` | recent sync attempts |
| POST / GET / DELETE | `/api/v1/admin/clients` | manage API clients |

```
GET /api/v1/convert?from=USD&to=EUR&amount=100

{
  "from": "USD",
  "to": "EUR",
  "amount": 100,
  "result": 85.9237,
  "rate": 0.859237,
  "rateDate": "2026-10-15"
}
```

## Notes

- Some currencies are quoted per 100 units (JPY, for example). Every rate is stored with its nominal, and conversion takes the nominal into account.
- The sync runs at 00:05, 10:05 and 16:05 Baku time (`fx.sync.cron`). When CBAR returns 404, the day is treated as "not published yet", not as an error.
- Statistics and daily changes are calculated in SQL with window functions (`first_value`, `lag`); see `RateRepository`.

## Tests

```bash
./mvnw verify
```

The repository and API integration tests run against PostgreSQL in Testcontainers. Without Docker they are skipped.
