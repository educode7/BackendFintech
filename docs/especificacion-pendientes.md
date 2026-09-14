# Especificación de Pendientes

Estado del backend al 2026-08-20. Documenta todo lo que falta antes de producción.

---

## ✅ Completado

| # | Item | Detalle |
|---|------|---------|
| 1 | Spring Security + JWT | Keycloak 25.0 en Docker, `SecurityConfig` reactivo en gateway, `ServiceSecurityConfig` MVC en shared |
| 2 | Rate limit por usuario | `RateLimitConfig` usa JWT `sub` claim, fallback a IP para no autenticados |
| 3 | OAuth2 Resource Server | `spring-boot-starter-oauth2-resource-server` en todos los POMs |
| 4 | Config centralizada | Spring Cloud Config Server con `issuer-uri` y `jwk-set-uri` de Keycloak |

---

## ❌ Pendiente — Deuda técnica consciente

### 1. Outbox transaccional

**Problema**: El publicador de Kafka se ejecuta tras el commit JPA via
`TransactionSynchronizationManager.registerSynchronization`. Si Kafka falla
después del commit de DB, el evento se pierde.

**Solución**: Migrar a outbox transaccional con tabla `outbox_events` dentro
de la misma transacción que el negocio. Debezium o轮询 para CDC hacia Kafka.

**Archivos afectados**:
- `payment-service/.../PaymentCommandService.java` (productor actual)
- `account-service/.../AccountCommandService.java` (productor actual)
- Nueva tabla `outbox_events` + migración Flyway

**Esfuerzo estimado**: 2-3 días

---

### 2. Snapshots de agregado

**Problema**: El event store en `account-service` reconstruye estado por
replay completo de eventos. Cuentas con miles de eventos tienen latencia
creciente en cada carga.

**Solución**: Snapshot periódico cada N eventos (ej. cada 100). Al cargar,
replay desde el último snapshot en vez del evento inicial.

**Archivos afectados**:
- `account-service/.../Account.java` (aplicar snapshot)
- `account-service/.../EventStoreRepository.java` (guardar/cargar snapshots)
- Nueva tabla `aggregate_snapshots` + migración Flyway

**Esfuerzo estimado**: 1-2 días

---

### 3. Testcontainers

**Problema**: Tests de integración usan H2, que no es exactamente Postgres.
Hay diferencias en SQL, functions y comportamiento de sequences.

**Solución**: Reemplazar H2 por Testcontainers con Postgres real en tests de
integración. Redis y Kafka también vía Testcontainers.

**Archivos afectados**:
- `payment-service/src/test/` (tests de integración)
- `account-service/src/test/` (tests de integración)
- `notification-service/src/test/` (tests de integración)
- Parent POM (agregar dependency management para Testcontainers)

**Esfuerzo estimado**: 2 días

---

### 4. Pago → Cuenta automático

**Problema**: El consumer de `account-service` recibe `PaymentCompletedEvent`
pero la lógica de "qué cuenta acreditar" está simplificada. En producción
necesita `accountId` explícito en el evento o auto-crear cuenta para el
`userId` la primera vez.

**Solución**: Definir protocolo de eventos entre payment y account. O
payment envía `accountId` explícito, o account auto-crea al primer pago.

**Archivos afectados**:
- `shared/.../PaymentCompletedEvent.java` (agregar `accountId`)
- `account-service/.../PaymentEventListener.java` (lógica de acreditación)

**Esfuerzo estimado**: 1 día

---

## ❌ Pendiente — Próximos pasos para producción

### 5. Tracing distribuido (OpenTelemetry)

**Problema**: `X-Correlation-Id` Propaga identificador pero no latencia
por tramo ni topology del request.

**Solución**: Agregar OpenTelemetry SDK + OTLP exporter. Span por cada
hop (gateway → service → Kafka → consumer).

**Esfuerzo estimado**: 1-2 días

---

### 6. Schema Registry para Kafka

**Problema**: Los payloads Kafka son JSON libre. Un cambio de formato en
un productor rompe consumers silenciosamente.

**Soluation**: Confluent Schema Registry con Avro o JSON Schema. Validar
en productor antes de publicar.

**Esfuerzo estimado**: 2 días

---

### 7. Tests de carga

**Problema**: No hay métricas de throughput ni latencia bajo carga.

**Solución**: Gatling o k6 con escenarios de carga realistas. Medir p50,
p95, p99 bajo 100, 500, 1000 req/s.

**Esfuerzo estimado**: 2-3 días

---

### 8. CI/CD con GitHub Actions

**Problema**: No hay pipeline automatizado de build, test y deploy.

**Solución**: GitHub Actions con stages: lint → build → test → security
scan → docker build → deploy (staging). Escaneo de imágenes con Trivy.

**Esfuerzo estimado**: 2 días

---

### 9. Multi-tenancy

**Problema**: Todo asume un solo tenant. `userId` es global.

**Solución**: Cada `userId` pertenece a un `tenantId`. RLS en Postgres,
namespaces en Redis, prefijo en topics Kafka.

**Esfuerzo estimado**: 3-5 días

---

### 10. Backups PITR para Postgres

**Problema**: No hay estrategia de backup.

**Solución**: WAL archiving + pg_basebackup. Restauración point-in-time
con `pg_wal_replay`.

**Esfuerzo estimado**: 1 día

---

## 🔄 Actualización pendiente del README

Línea 303 dice: `Sin Spring Security (explícitamente fuera de scope)`.
Esto ya no aplica — actualizar o eliminar esa línea.

---

## Resumen de esfuerzo

| Categoría | Items | Días estimados |
|-----------|-------|----------------|
| Deuda técnica | 4 | 6-8 |
| Producción | 6 | 11-15 |
| **Total** | **10** | **17-23** |
