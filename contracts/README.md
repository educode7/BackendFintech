# Wallet Platform — API Contracts

> **Source of truth**: This directory defines every external (REST) and internal (gRPC)
> contract for the Wallet Backend. No service may be implemented without a contract
> existing here first.

## Directory Structure

```
contracts/
├── openapi/                          # REST API contracts (external-facing)
│   ├── payment-service-v1.yaml       # Payment Service — OpenAPI 3.1
│   ├── account-service-v1.yaml       # Account Service — OpenAPI 3.1
│   └── notification-service-v1.yaml  # Notification Service — OpenAPI 3.1
│
├── proto/                            # gRPC contracts (internal inter-service)
│   └── wallet/
│       ├── payment/v1/
│       │   └── payment_service.proto
│       ├── account/v1/
│       │   └── account_service.proto
│       ├── notification/v1/
│       │   └── notification_service.proto
│       └── event/v1/
│           └── domain_events.proto   # Kafka event schemas
│
└── README.md                         # This file
```

## Versioning Rules

### REST (OpenAPI)

- Versioned via URL segment: `/api/v1/...`
- A published major version MUST NOT introduce breaking changes within that version.
- **Compatible changes** (allowed within a version):
  - Add optional response fields
  - Add optional request parameters
  - Add new endpoints
  - Add new documented error codes
- **Breaking changes** (require new major version):
  - Rename or remove fields
  - Change field types
  - Change field semantics
  - Change authentication requirements
  - Change date/ID formats
  - Alter pagination rules incompatibly
  - Change `Idempotency-Key` semantics

### gRPC (Protocol Buffers)

- Versioned via package: `wallet.{service}.v1`
- Never reuse field numbers of removed fields; use `reserved`.
- New fields MUST be optional and retrocompatible.
- Never change field types incompatibly.
- Clients MUST tolerate unknown fields.
- Enums MUST be designed to tolerate unknown values.
- Every command with side effects MUST include `request_id`.

### Kafka Events

- Versioned via `EventEnvelope.version` field and package `wallet.event.v1`.
- Events are immutable once published.
- New event types are added as new `oneof` variants in `EventEnvelope`.
- Payload fields follow the same evolution rules as gRPC.
- Consumers MUST tolerate unknown fields.

## Breaking Change Policy

| Change Type | REST | gRPC | Events |
|---|---|---|---|
| Add optional field | ✅ Compatible | ✅ Compatible | ✅ Compatible |
| Add required field | ❌ Breaking | ❌ Breaking | ❌ Breaking |
| Remove field | ❌ New version | ❌ Use reserved | ❌ Deprecate only |
| Rename field | ❌ New version | ❌ Use reserved | ❌ Deprecate only |
| Change type | ❌ New version | ❌ New version | ❌ New version |
| Add endpoint | ✅ Compatible | ✅ Compatible | N/A |
| Remove endpoint | ❌ New version | ❌ Deprecate | N/A |
| Add enum value | ✅ Compatible | ✅ Compatible | ✅ Compatible |
| Remove enum value | ❌ New version | ❌ Use reserved | ❌ Deprecate |

## Deprecation Process

When removing an endpoint or field:

1. Mark as deprecated with documentation (date + alternative).
2. Add `Deprecation` and `Sunset` HTTP headers where applicable.
3. Maintain the deprecated endpoint for at least 2 major release cycles.
4. Monitor usage before final removal.

## Code Generation

### TypeScript from OpenAPI

```bash
# Generate TypeScript types from OpenAPI specs
npx openapi-typescript contracts/openapi/payment-service-v1.yaml -o src/features/payments/infrastructure/http/payment-api.types.ts
```

### Java from Proto

```bash
# Proto files are compiled via quarkus-grpc extension (automatic in Quarkus)
# Manual compilation:
protoc --java_out=target/generated-sources \
       --grpc-java_out=target/generated-sources \
       contracts/proto/wallet/**/*.proto
```

## Changelog

### [1.0.0] — 2026-09-14

#### Added
- Payment Service REST API (v1): `POST /payments`, `GET /payments/{id}`
- Account Service REST API (v1): `POST /accounts`, `GET /accounts/{id}`, `POST /accounts/{id}/deposits`, `POST /accounts/{id}/withdrawals`
- Notification Service REST API (v1): `GET /notifications/{userId}`
- Payment Service gRPC (v1): `ProcessPayment`, `GetPayment`, `WatchPaymentStatus`
- Account Service gRPC (v1): `OpenAccount`, `GetAccount`, `Deposit`, `Withdraw`
- Notification Service gRPC (v1): `ListNotifications`
- Domain event schemas: `PaymentCompletedEvent`, `AccountOpenedEvent`, `MoneyDepositedEvent`, `MoneyWithdrawnEvent`
- Idempotency contract: `Idempotency-Key` header on all POST operations
- Error format: RFC 9457 `application/problem+json`
- W3C Trace Context propagation in event envelopes

#### Security
- All endpoints require JWT bearer authentication (Keycloak RS256)
- `Idempotency-Key` header enforced on state-changing operations
