# Wallet Backend

Backend de billetera digital estilo fintech, multi-módulo Spring Boot 4.1 + Kafka.
Maneja dinero, así que el código aplica rigor de servicio crítico: idempotencia,
timeouts explícitos, circuit breakers, correlación end-to-end, errores RFC 9457,
migraciones versionadas y observabilidad RED.

```
┌─────────────┐   ┌──────────────┐   ┌─────────────────────�
│  Client /   │ → │ api-gateway  │ → │ payment-service     │  Kafka  ┐
│  cURL       │   │  (8080)      │   │ (8081)              │ ──────► │
└─────────────┘   │  Resilience4j│   │ payment.events      │        │
                  │  Rate limit  │   └─────────────────────┘        │
                  │  Correlation │   ┌─────────────────────┐        │
                  └──────┬───────┘ → │ account-service     │ ◄──────┤
                         │           │ (8082)              │        │
                         │           │ event-sourced CQRS  │        │
                         │           │ account.events      │ ──────►│
                         │           └─────────────────────┘        │
                         │           ┌─────────────────────┐        │
                         └─────────► │ notification-service│ ◄──────┘
                                     │ (8083)              │
                                     │ notifications db    │
                                     └─────────────────────┘

        ┌───────────┐    ┌────────┐    ┌──────────┐    ┌────────────┐
        │ Postgres  │    │ Redis  │    │  Kafka   │    │ Prometheus │
        │  16       │    │  7     │    │  3.7     │    │ Grafana    │
        └───────────┘    └────────┘    └──────────┘    └────────────┘
```

## Stack tecnológico

| Componente | Versión | Rol |
|------------|---------|-----|
| Java       | 25      | ScopedValues, Stream Gatherers, pattern matching, virtual threads |
| Spring Boot| 4.1.0   | Framework base (web, data, security-ready, actuator) |
| Spring Cloud| 2025.1.2 | Gateway, LoadBalancer |
| Kafka      | 3.7 (KRaft) | Bus de eventos entre servicios |
| Postgres   | 16      | Persistencia (un cluster, una BD por servicio) |
| Redis      | 7       | Idempotencia, rate-limit, CQRS read model |
| Resilience4j| 2.4.0  | Circuit breakers, retries |
| Flyway     | 10.x    | Migraciones versionadas |
| Micrometer + Prometheus | 1.14.x | Métricas RED |
| Lombok     | 1.18.34 | Reducción de boilerplate |
| Maven      | 3.9     | Build multi-módulo |

## Decisiones arquitectónicas

### 1. Idempotencia con Redis (SET NX EX)
Toda operación de mutación acepta `Idempotency-Key`. Si la clave ya está en
Redis (TTL 24h) se devuelve el resultado cacheado sin re-ejecutar.

**Por qué Redis y no SQL**: SET NX es atómico (sin race), TTL es trivial y la
latencia bajo carga es sub-milisegundo. 24h cubre cualquier reintento razonable
del cliente y mantiene la memoria acotada.

**Si el cliente no envía la key**: en `payment-service` se rechaza con
`400 IDEMPOTENCY_KEY_REQUIRED`. En el consumer de Kafka el `eventId` del
metadata actúa como clave implícita.

### 2. CQRS + Event Sourcing (account-service)
El estado de la cuenta NO se guarda como columnas sino como secuencia de
eventos en `event_store` (JSONB + UNIQUE(aggregate_id, version)). El read
model vive en Redis (rápido) con un backup en `account_view` (duradero).

**Por qué separar write/read**: el write side necesita invariantes de negocio
(saldo ≥ 0, versionado optimista), el read side necesita latencia plana. Mezclar
ambos en una sola tabla fuerza a elegir entre consistencia e índice.

**Por qué Redis para el read model**: GET sub-ms vs SQL roundtrip. **Trade-off**:
Redis puede evictar en frío — por eso persistimos también en Postgres como
fuente de reconstrucción.

### 3. Circuit Breaker (gateway + payment)
Resilience4j con instancias nombradas. El gateway lo aplica por ruta; payment
lo aplica al producer de Kafka.

**Por qué en el gateway Y en payment**: el gateway protege al cliente del
tiempo total de la cadena; payment protege la integridad transaccional cuando
Kafka está degradado.

### 4. Correlation ID end-to-end
Cada request lleva `X-Correlation-Id`. Si el cliente no lo envía, el gateway
genera un UUID v7. Se propaga vía:
- filtro HTTP → `ScopedValue<UUID>` en el hilo (Java 25) + MDC en logs
- header HTTP saliente a servicios downstream
- header Kafka (`X-Correlation-Id`) en cada mensaje
- campo `correlationId` en `EventMetadata`

### 5. Errores RFC 9457
Cada respuesta de error es `application/problem+json` con campos estables:
`type`, `title`, `status`, `detail`, `instance`, `code`, `correlationId`,
`timestamp`. Los clientes pueden ramificar en `code` sin parsear mensajes.

### 6. Observabilidad
Cada servicio expone `/actuator/prometheus` con métricas RED (Rate, Errors,
Duration) por endpoint. Health checks personalizados para Kafka, Redis y
Postgres. Logs estructurados con patrón que incluye `correlationId`.

### 7. Resiliencia
Timeouts explícitos (Hikari connection-timeout=5s, Kafka delivery=30s).
Retry SOLO en operaciones idempotentes. Fallback en circuit breakers abiertos
(`/fallback/{service}` en el gateway).

## Estructura del proyecto

```
BackendFintech/
├── pom.xml                                  parent (packaging=pom)
├── docker-compose.yml                       infra + 4 servicios
├── Makefile
├── README.md
├── prometheus.yml
├── init-postgres/                           scripts primer arranque
├── monitoring/grafana/
│   ├── provisioning/{datasources,dashboards}/
│   └── dashboards/wallet-overview.json
├── shared/                                  módulo común
│   └── src/main/java/com/wallet/shared/
│       ├── api/{ErrorResponse,PageResponse}.java
│       ├── event/{AccountEvent,AccountOpenedEvent,MoneyDepositedEvent,
│       │           MoneyWithdrawnEvent,PaymentCompletedEvent,EventMetadata}.java
│       ├── filter/{CorrelationIdFilter,RequestLoggingFilter}.java
│       ├── money/{Money,MoneySerializer}.java
│       ├── util/{IdGenerator,JsonUtil}.java
│       └── kafka/KafkaTopics.java
├── api-gateway/                             Spring Cloud Gateway
├── payment-service/                         Spring MVC + JPA
├── account-service/                         Spring MVC + JPA + Event Sourcing
└── notification-service/                    Spring MVC + JPA + Kafka consumer
```

## Cómo arrancarlo

### Prerrequisitos
- Java 25 (`java -version` debe decir 25) — `JAVA_HOME` debe apuntar a JDK 25
- Maven 3.9+
- Docker + Docker Compose (para la infraestructura)

### 1) Compilar todo
```bash
# Asegurar JAVA_HOME apunte a JDK 25
export JAVA_HOME=/path/to/jdk-25
mvn -s ~/.m2/settings.local.xml -pl shared,api-gateway,payment-service,account-service,notification-service -am clean package -DskipTests
```

### 2) Levantar la infraestructura
```bash
make up
# o
docker compose up -d --build
```

Esto levanta Postgres, Redis, Kafka (KRaft, sin Zookeeper), Prometheus y
Grafana, además de los 4 microservicios. La primera vez tarda ~3-5 minutos
(descarga imágenes, compila módulos).

### 3) Verificar
- API Gateway: http://localhost:8080/actuator/health
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin / change-me)

## Cómo probarlo

### Abrir una cuenta
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: demo-001" \
  -d '{"userId":"user-1","initialBalance":"100.00 USD"}'
```

### Depositar
```bash
curl -X POST http://localhost:8080/api/v1/accounts/<ACCOUNT_ID>/deposits \
  -H "Content-Type: application/json" \
  -d '{"amount":"50.00","currency":"USD"}'
```

### Crear un pago (requiere `Idempotency-Key`)
```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pay-abc-001" \
  -H "X-Correlation-Id: demo-002" \
  -d '{"userId":"user-1","amount":"25.00 USD"}'
```

### Ver notificaciones del usuario
```bash
curl http://localhost:8080/api/v1/notifications/user-1?page=0&size=20
```

## Tópicos Kafka

| Topic              | Productor           | Consumidores                              | Headers                                     |
|--------------------|---------------------|-------------------------------------------|---------------------------------------------|
| `payment.events`   | payment-service     | account-service, notification-service     | `X-Correlation-Id`, `event-type`, `event-id`|
| `account.events`   | account-service     | (reservado para futuros consumidores)     | igual                                       |

**Consumer groups** (cada uno recibe TODOS los eventos):
- `account-service`
- `notification-service`

## Esquema de base de datos

### `payments_db` (payment-service)
- `payments(id, user_id, amount NUMERIC(19,4), currency CHAR(3), status, idempotency_key UNIQUE, created_at, updated_at, version)`
- Índices: `idx_payments_user_id`, `idx_payments_created_at DESC`

### `accounts_db` (account-service)
- `event_store(id, aggregate_id, aggregate_type, event_type, event_data JSONB, version, created_at, correlation_id, UNIQUE(aggregate_id, version))`
- `account_view(account_id, user_id, balance_amount, balance_currency, status, version, last_updated)` — backup del read model

### `notifications_db` (notification-service)
- `notifications(id, user_id, type, subject, body, status, processed_event_id UNIQUE, created_at, sent_at)`

Las tres BDs se crean automáticamente al primer arranque vía `init-postgres/`.

## Errores y códigos

Todos los errores son `application/problem+json`. Códigos estables:

| HTTP | code                       | Significado                                          |
|------|----------------------------|------------------------------------------------------|
| 400  | `BAD_REQUEST`              | Cuerpo o parámetros malformados                      |
| 400  | `IDEMPOTENCY_KEY_REQUIRED` | Falta el header `Idempotency-Key`                     |
| 404  | `NOT_FOUND`                | Ruta no encontrada                                   |
| 404  | `PAYMENT_NOT_FOUND`        | Pago inexistente                                     |
| 404  | `ACCOUNT_NOT_FOUND`        | Cuenta inexistente                                   |
| 409  | `DUPLICATE_PAYMENT`        | Idempotency-Key ya usado                             |
| 409  | `CONCURRENT_MODIFICATION`  | Conflicto de versión en event store                  |
| 422  | `VALIDATION_FAILED`        | Bean Validation falló                                |
| 422  | `INSUFFICIENT_FUNDS`       | Saldo insuficiente para withdrawal                   |
| 429  | `TOO_MANY_REQUESTS`        | Rate limit por IP excedido                           |
| 503  | `CIRCUIT_OPEN`             | Circuit breaker abierto en el gateway                |
| 5xx  | `INTERNAL_ERROR`           | Error inesperado (ver logs con correlationId)        |

## Deuda técnica consciente

- **Outbox pattern**: el publicador de Kafka se ejecuta tras el commit JPA
  (`TransactionSynchronizationManager.registerSynchronization`). Funciona,
  pero deja una ventana donde DB committed y Kafka falló. Migrar a outbox
  transaccional cuando se necesite.
- **Snapshots**: el event store se reconstruye por replay completo. Para
  cuentas con miles de eventos, agregar snapshots periódicos.
- **Auth / JWT**: no hay autenticación. El rate-limit es por IP. Cuando
  aterrice auth, cambiar `ipKeyResolver` en `RateLimitConfig` por un
  resolver basado en `userId` (extraído del JWT).
- **Rate limiting más granular**: actualmente 100 req/min por IP global.
  Produciría querer buckets por endpoint o por usuario autenticado.
- **Pago → Cuenta automático**: el consumer de `account-service` recibe
  `PaymentCompletedEvent` pero la lógica de "qué cuenta acreditar" está
  simplificada. En producción: payment-service debería enviar el
  `accountId` explícito en el evento, o el consumer debería auto-crear
  cuenta para el `userId` la primera vez.
- **Tests**: 158 tests unitarios pasando (shared 54, api-gateway 1, payment 30,
  account 48, notification 25). Falta Testcontainers para tests de integración
  contra Postgres/Redis/Kafka reales y tests del consumer idempotente.

## Próximos pasos para producción

1. **Spring Security + JWT**: añadir capa de auth, key-resolver por usuario.
2. **Outbox transaccional** con Debezium o equivalente.
3. **Snapshots de agregado** cada N eventos.
4. **Tracing distribuido** (OpenTelemetry) además de correlationId.
5. **Schema Registry** para los payloads Kafka.
6. **Rate limit por usuario** (no solo IP).
7. **Tests de carga** (Gatling / k6).
8. **CI/CD** con GitHub Actions, escaneo de imágenes, política de admisión.
9. **Multi-tenancy**: cada `userId` pertenece a un tenant.
10. **Backups PITR** para Postgres.

## Comandos útiles (Makefile)

```bash
make build         # mvn clean package -DskipTests
make up            # docker compose up -d
make down          # docker compose down
make logs          # docker compose logs -f
make ps            # docker compose ps
make clean         # mvn clean + docker compose down -v
make test-payments # curl de ejemplo al endpoint de pago
make kafka-topics  # listar tópicos de Kafka
```

## Notas de implementación

- Java 25: se usan `record`, `sealed interface AccountEvent`, pattern
  matching exhaustivo en `Account.apply()`, y `ScopedValue` para correlación
  end-to-end (con try-catch porque `ScopedValue.orElse(null)` es ilegal en JDK 25).
- Spring Boot 4.1: test annotations relocados (`@WebMvcTest` →
  `spring-boot-webmvc.test.autoconfigure`, `@DataJpaTest` →
  `spring-boot.data.jpa.test.autoconfigure`). Auto-config de Jackson 3 por
  defecto; se excluye `spring-boot-jackson` y se provee `ObjectMapper` Jackson 2
  vía `JacksonConfig` para compatibilidad con serialización de eventos existente.
- UUID v7 vía `com.fasterxml.uuid:java-uuid-generator`. Se prefiere v7 sobre
  v4 por orden temporal (mejor para primary keys y orden de eventos).
- `Money` siempre `BigDecimal`. Nunca `float`/`double`.
- Lombok: se usan `@Getter`, `@Setter`, `@ToString`, `@RequiredArgsConstructor`,
  nunca `@Data` (explícito > mágico).
- Sin Spring Security (explícitamente fuera de scope).
- Sin H2 en main (siempre Postgres).
- `var` se usa solo cuando mejora legibilidad (poco, no indiscriminado).
