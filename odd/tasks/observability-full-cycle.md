# Observability Full Cycle — Grafana frontend→infra + slow queries

## Objective
Make the full request cycle (browser → API gateway → services → Kafka/Postgres) visible in Grafana (metrics + logs + traces), and expose a ranking of the slowest SQL queries.

## Problem / Why
- Grafana provisioning exists under `monitoring/grafana/` but was never mounted in docker-compose → Grafana booted with no datasources and no dashboards.
- Traces exported only to Jaeger; Grafana had no trace datasource. User decision: **replace Jaeger with Grafana Tempo**.
- Frontend never created spans: OTLP export gated on `production:true`, and `traceInterceptor` fabricated a `traceparent` without a span → browser absent from traces.
- No `pg_stat_statements` / `postgres_exporter` → "which query is slow" was invisible.

## Scope
- `docker-compose.yml`, `monitoring/**`, `otel-collector-config.yml`, `prometheus.yml`, `init-postgres/**`
- `apps/wallet-web` (telemetry, interceptors, environments, tests)
- Quarkus service log patterns (traceId correlation only)

Out of scope: business logic, PR creation, collector-side spanmetrics (T6 covered by Tempo metrics_generator instead).

## Constraints (resolved)
- TDD: **OFF** (no project/session TDD config found) → ordinary functional checks. Frontend runner: `npx ng test --watch=false` (apps/wallet-web).
- Delivery strategy: `ask-on-risk`. Chain strategy: **`stacked-to-main`** (user decision, 2026-09-22). Forecast ~489 authored lines > ~400 → PR slicing required at delivery time.
- Route: **delegated-direct** (writer trigger: 2+ non-trivial files). One bounded writer did T1–T6; orchestrator verifies and commits.
- Artifacts/code/UI strings: **English**.

## Tasks

### T1: Grafana wiring [CRITICAL]
- [x] T1.1 Mount `monitoring/grafana/provisioning` → `/etc/grafana/provisioning` and `monitoring/grafana/dashboards` → `/var/lib/grafana/dashboards` on the grafana service
- [x] T1.2 Complete datasources: Prometheus uid `Prometheus`, Loki (`http://loki:3100`, derivedFields → Tempo), Tempo uid `tempo` (`http://tempo:3200`, tracesToLogsV2 → Loki, serviceMap + tracesToMetrics → Prometheus)

### T2: Tempo backend [CRITICAL]
- [x] T2.1 `monitoring/tempo/tempo.yml`: single binary, HTTP 3200, OTLP grpc 4317 + http 4318 (in-network), local storage+wal, metrics_generator (span-metrics, service-graphs) → Prometheus remote write
- [x] T2.2 Compose `tempo` service + `tempo-data` volume. Deviation: only host port 3200 published — host 4317/4318 belong to otel-collector (browser/local dev); dual publish would collide (documented in a compose comment)
- [x] T2.3 Collector traces export → `tempo:4317` (replaced `otlp/jaeger`); `jaeger` service removed (UI :16686 gone — traces queried from Grafana)
- [x] T2.4 Collector OTLP/HTTP CORS allows `http://localhost:4200` and `http://127.0.0.1:4200`; 4318 published on host

### T3: Frontend real spans [CRITICAL]
- [x] T3.1 `tracing.ts` always configures OTLP (no production gate); `otelEndpoint` added to both environments (dev/prod: `http://localhost:4318/v1/traces`, host-reachable from browser; prod assumption documented in code — no in-repo frontend Dockerfile/nginx exists to derive another value)
- [x] T3.2 `trace.interceptor.ts`: real CLIENT span per API call, `propagation.inject` of W3C traceparent from the active span, `SpanStatusCode.ERROR` on failure, `finalize` ends span, Keycloak/OIDC excluded, inbound traceparent never overwritten
- [x] T3.3 Interceptor spec rewritten with real OTel provider + InMemorySpanExporter (7 tests). Prior report "`@opentelemetry/core` missing" is **stale** — present at ^2.11.0; suite green
- [x] T3.4 Cross-checked: collector 4318 published to host (T2.4)

### T4: Postgres slow-query metrics [HIGH]
- [x] T4.1 postgres command `-c shared_preload_libraries=pg_stat_statements`; `init-postgres/01-create-databases.sh` now runs `CREATE EXTENSION` on all app DBs and IS mounted in compose
- [x] T4.2 `monitoring/postgres/exporter-queries.yml` + 3 postgres-exporter services (payments_db, wallet, notifications_db) exposing pg_stat_statements (query/calls/mean_exec_time/total_exec_time)
- [x] T4.3 Prometheus scrape job with `db` label, 3 targets

### T5: Dashboard + log correlation [HIGH]
- [x] T5.1 `[traceId=%X{traceId}]` added to log format of all 5 services — MDC key `traceId` **verified from quarkus-opentelemetry 3.33.3.2 source**, not guessed
- [x] T5.2 Dashboard updated: removed dead `resilience4j_circuitbreaker_state` panel; replaced `spring_kafka_*` panel with `payment_processed_total`/`notification_processed_total`; added "Slowest SQL queries" table; JSON validated

### T6: Span metrics / service map [STRETCH — done]
- [x] T6.1 Tempo metrics_generator processors `["span-metrics","service-graphs"]` + `remote_write` → prometheus (`send_exemplars: true`); prometheus command gains `--enable-feature=remote-write-receiver`; Grafana `serviceMap` + `tracesToMetrics` wired

## Dependencies
T2→T1.2 (C1), T4→T5.2 (C4), T3 independent (C2), T6 last (in C1, after T1–T5 verified)

## Verification (all reported by writer; parent spot-checked compose config)
1. `docker compose config -q`: OK (exit 0) — parent re-ran: OK
2. Dashboard `JSON.parse` via node: exit 0
3. `npx ng test --watch=false` (apps/wallet-web): 33 files / **231 tests passed** (run pre- and post-T6)
4. YAML parse of all 11 new/edited YAML files: OK

## Delivery plan — work units (Conventional Commits, no AI attribution)
- **C1** `feat(observability): wire Grafana provisioning, Tempo backend, and slow-query metrics` — `docker-compose.yml`, `otel-collector-config.yml`, `prometheus.yml`, `init-postgres/01-create-databases.sh`, `monitoring/tempo/`, `monitoring/postgres/`, `monitoring/grafana/provisioning/`
- **C2** `feat(wallet-web): export real browser spans via OTLP` — `apps/wallet-web/**` (tracing, interceptor, spec, environments)
- **C3** `feat(observability): add traceId to Quarkus log patterns` — 5 × `*-quarkus/src/main/resources/application.yml`
- **C4** `feat(observability): fix stale dashboard panels and add slow-SQL panel` — `monitoring/grafana/dashboards/wallet-overview.json` + this doc

First review boundary: branch point `98ef4b4`. Chain: **stacked-to-main** — if delivery splits into PRs, each PR merges to main in C1→C4 order.

## Acceptance criteria
- [x] Grafana boots with Prometheus/Loki/Tempo datasources + dashboard, zero manual clicks (static: provisioning mounted, uids aligned; runtime pending stack start)
- [ ] One request = one trace including browser span, queryable in Grafana Explore → Tempo (runtime verification pending stack start)
- [x] Prometheus scrapes pg_stat_statements metrics; dashboard shows top slow queries (static wiring complete; runtime pending)
- [x] All verification commands pass (docker CLI available and used)

## Progress
- [x] Diagnosis (Engram `discovery/observability-gaps`)
- [x] Feature doc + Engram mirror
- [x] T1–T6 implemented by bounded writer (status: success)
- [x] Parent verification: compose config spot-check OK; dashboard JSON OK; wallet-web 231 tests passed; structural readback PASS
- [x] Clone review disabled (free-tier provider blocked all bound captures) → ordinary checks only
- [x] C1 `cc86b88` — Grafana provisioning, Tempo backend, slow-query metrics
- [x] C2 `84b5684` — wallet-web real browser spans via OTLP
- [x] C3 `0d13ae5` — traceId in Quarkus log patterns (landed after C1; commit originally mislabeled, message amended before push)
- [x] C4 `8e03d1d` — dashboard panels + slow-SQL panel
- Commit order note: history is C1 → C3 → C2 → C4 due to a mid-flight index race; content per unit is correct. First review boundary remains branch point `98ef4b4`.

## Known environmental / follow-up notes
- **Existing `postgres-data` volume**: initdb scripts only run on an empty volume → run manually after up: `psql -d <db> -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"` for payments_db, wallet, notifications_db (and accounts_db if used), or reset the volume. The `shared_preload_libraries` change recreates the container anyway.
- **`angular.json` has no `fileReplacements`**: `environment.prod.ts` is dead config; `environment.production` is always false (out of T3 scope, pre-existing — affects `apiGateway` in real prod builds; candidate follow-up task).
- Grafana provisioning `$$` interpolation and tracesToLogsV2/tracesToMetrics queries are doc-verified, not runtime-executed.
- Exemplars: `send_exemplars: true` set; add `--enable-feature=exemplar-storage` to prometheus later only if exemplars don't appear.
- Pre-existing dirty files never staged: `.atl/skill-registry.*`, `openspec/changes/`, `test-payment-flow.ps1`.
- Preserved: CorrelationContext, CorrelationIdFilter, correlation/request/idempotency interceptors.

## Notes
- Correlation helpers already exist — intact.
