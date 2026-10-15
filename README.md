# recon-engine

[![CI](https://github.com/manasshah1810/recon-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/manasshah1810/recon-engine/actions/workflows/ci.yml)

A Spring Boot service that reconciles two transaction files (for example an internal ledger and a bank
statement), reports what matched, and tracks every item that needs a human decision.

Built to practise the day-to-day shape of reconciliation work: a deterministic matching core, a REST API,
a relational schema with migrations, tests, and a container build.

## What it does

1. Upload two CSVs (`reference,amount,currency,date`) to `POST /api/runs`.
2. **Pass 1, exact:** same reference (case-insensitive), currency, amount and date.
3. **Pass 2, tolerance** (only if you pass `amountTolerance` and/or `dateToleranceDays`): same reference and
   currency, amount and date within the limits (inclusive). The closest candidate wins.
4. Everything left over, plus every tolerance-only match, is stored as a *break* with status
   `OPEN`, `RESOLVED` or `ESCALATED` and an optional note.

Rules worth knowing: each line is used at most once (duplicates pair one-to-one); amounts use `BigDecimal`
(`10.0` equals `10.00`); different currencies never match; results are deterministic for the same input.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/runs` | multipart `fileA`, `fileB`; optional `amountTolerance`, `dateToleranceDays`. Returns the run summary |
| GET | `/api/runs/{id}` | run summary (counts of exact, tolerance, unmatched) |
| GET | `/api/runs/{id}/breaks?status=&type=` | list breaks. `type`: `UNMATCHED_A`, `UNMATCHED_B`, `TOLERANCE_MATCH` |
| PATCH | `/api/breaks/{id}` | body `{"status":"RESOLVED","note":"..."}` |
| GET | `/actuator/health` | health |
| GET | `/swagger-ui.html` | OpenAPI UI |

Try it with the sample files:

```bash
curl -F fileA=@samples/ledger.csv -F fileB=@samples/bank.csv \
     -F amountTolerance=0.10 -F dateToleranceDays=1 http://localhost:8080/api/runs
curl http://localhost:8080/api/runs/1/breaks
```

With those samples: 3 exact matches, 1 tolerance match (INV-1002), 1 unmatched ledger line (INV-1004) and
1 unmatched bank line (INV-1006).

## Streaming ingestion (Kafka)

Besides file upload, transactions can arrive as JSON events on a Kafka topic (`txn-events`, enabled with
`recon.kafka.enabled=true`, broker from `KAFKA_BOOTSTRAP`):

```json
{"streamId":"2026-03-02","eventId":"e1","side":"A","reference":"INV-1001","amount":"1500.00","currency":"USD","date":"2026-03-02"}
```

Events are staged in `txn_event`. Delivery is at-least-once, so ingestion is idempotent on
`(streamId, eventId)`; redelivered events are ignored, and malformed messages are logged and skipped so they
cannot block a partition. `POST /api/streams/{streamId}/reconcile` then reconciles everything received for
that stream. `POST /api/streams/events` feeds the same ingestion path over HTTP.

## Run

```bash
mvn spring-boot:run                                   # in-memory H2, no setup
docker compose up --build                             # app + PostgreSQL 16 (profile "postgres")
```

Requires JDK 17+ and Maven 3.9+.

## Tests

```bash
mvn verify
```

31 tests: matcher unit tests (duplicates, tolerance boundaries, currency, determinism, 100,000-line case),
CSV parser tests (quoting, BOM, line-numbered errors), API integration tests with MockMvc on H2, and Kafka ingestion tests on an embedded broker (no Docker needed).
Flyway runs the real migration in every integration test.

## Design notes

- `match/` is plain Java with no Spring dependency, so the matching rules are unit-testable in isolation.
- Pass 1 indexes side B in a hash map of FIFO queues, so matching is roughly O(n) rather than O(n*m).
  Pass 2 compares only against leftover lines with the same reference.
- The schema is created by Flyway (`db/migration`); Hibernate only validates it.
- Uploads are parsed fully in memory (20 MB limit). For much larger files, stream and batch the inserts.

## Limitations (honest list)

- Matching is reference-based. Lines without a shared reference are never paired, even if amount and date agree.
- Kafka: no dead-letter topic, and only tested on an embedded broker (not against a real cluster). No streaming-time
  windows: a stream is reconciled on request.
- No authentication, no multi-tenancy, no many-to-one matching. Tests run against H2; the PostgreSQL profile and
  Docker image are configured but not covered by automated tests in CI.
