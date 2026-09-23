# Feature: notifications-read-status

## Objective
Dashboard "Unread notifications" must mean "not read by the user", not delivery status PENDING/SENT.

## Problem
`unreadCount` filters `status === 'PENDING'`, but NotificationService marks SENT immediately after dispatch, so the counter is always 0. Domain has no read/unread field.

## Why
User selected option 2: "Mark all as read" button + individual click to mark as read.

## Scope
- Backend: `read_at` column, domain `markAsRead()`, PATCH endpoints, fix `NotificationEntity.toDomain()` losing `createdAt`/`sentAt`.
- Frontend: unreadCount = `!readAt`, list unread styling = `!readAt`, Mark-all button, click-to-mark-read.
- Out of scope: notifications-page auto-read on open, per-type read filters.

## Constraints
- Artifacts/code English; chat Spanish.
- Review DISABLED; TDD OFF; delivery ask-on-risk.
- Never stage: `.atl/skill-registry.*`, `openspec/changes/`, `test-payment-flow.ps1`.
- Original forecast: ~250–350 authored changed lines. Actual: **~492** (460+32) → exceeded ~400 budget → user chose chain strategy **`stacked-to-main`** (2026-09-22).

## Acceptance criteria
- [x] Migration adds nullable `read_at TIMESTAMPTZ`.
- [x] GET response includes `readAt`.
- [x] `PATCH /api/v1/notifications/{id}/read` marks one as read (idempotent).
- [x] `PATCH /api/v1/notifications/{userId}/read-all` marks all unread for user.
- [x] `unreadCount` uses `!readAt`.
- [x] Notification list: Mark all as read button; individual card click marks that notification read.
- [x] `toDomain()` preserves DB `createdAt`/`sentAt`/`readAt`.
- [x] Unit/store tests updated and green.

## Tasks
- [x] T1 Backend domain + entity + migration + toDomain fix
- [x] T2 Backend repository helpers + PATCH endpoints
- [x] T3 Frontend model/adapter/store unread semantics + mark actions
- [x] T4 Frontend notification-list UI (Mark all + click individual)
- [x] T5 Tests green (backend + frontend store/list/adapter)
- [x] T6 Work-unit commit

## Route / delegation
- Route: delegated direct (writer trigger: 8+ non-trivial files).
- TDD: OFF (session config).

## Delivery plan — work units (Conventional Commits, no AI attribution)
Chain: **stacked-to-main** (user decision 2026-09-22). First review boundary: branch point `ce5fa40` (branch `feat/notifications-read-status` cut from `main`).
- **C1** `feat(notifications): add read/unread semantics and PATCH read endpoints` — notification-service-quarkus (domain, entity, migration V2, adapter, resource, exception mapper, tests) ~175 authored lines.
- **C2** `feat(wallet-web): unread count and mark-read actions based on readAt` — apps/wallet-web notifications feature (model, adapter, store, list UI, specs) + this doc ~317 authored lines.

Each commit < 400 → eligible as stacked PR slices to main in C1→C2 order.

## Progress
- [x] Diagnosis + feature doc + Engram mirror
- [x] T1–T5 implemented by bounded writer (status: success)
- [x] Parent gatekeeper: structural readback + spot-check
- [x] C1 `052a4c5` — backend read/unread + PATCH endpoints
- [x] C2 (this commit) — frontend unread semantics + mark actions + this doc; final hash recorded in Engram mirror `odd/notifications-read-status/tasks`
- [x] Runtime 500 diagnosis: notification-service image was stale (pre-feature) → rebuilt; V2 migration applied; direct service PATCH verified 200/404
- [x] CorsFilter: added PATCH to Access-Control-Allow-Methods
- [x] Gateway fix: empty-body methods (incl. PATCH /read) no longer re-register bodyHandler after BodyHandler already consumed the stream

## Verification evidence
- Backend compile: `mvn -q -DskipTests compile` (notification-service-quarkus): exit 0 (parent re-ran: COMPILE_OK).
- Backend tests: `mvn -q test`: TEST_OK (parent re-ran; includes 3 new domain tests: markAsRead sets readAt, idempotent, `of` preserves timestamps).
- Frontend tests: `npx ng test --watch=false` (apps/wallet-web): **33 files / 240 tests passed** (parent re-ran: green).
- Writer reported same suites green; parent spot-check confirmed.
- Note: no `mvnw` wrapper in repo — system Maven 3.9.16 used (`C:\Users\educode\apache-maven-3.9.16\bin\mvn.cmd`).
- Runtime (notification-service:8083): Flyway v2 applied; `PATCH /{id}/read` → 200 with `readAt`; missing id → 404; `PATCH /{userId}/read-all` → 200 `{"marked":N}`.

## Known environmental / follow-up notes
- Migration V2 adds partial index `idx_notifications_user_unread` on `(user_id) WHERE read_at IS NULL`.
- Existing DB volumes: Flyway migration runs on service start; no manual step expected unless Flyway is disabled (not the case here).
- Delivery-status badge (SENT/PENDING/FAILED) intentionally left unchanged — delivery vs read are different concepts.
- Out of scope (unchanged): notifications-page auto-read on open, per-type read filters.

## Next step
- Commit C3 runtime fixes (gateway empty-body proxy + service CorsFilter PATCH), rebuild/restart gateway, re-verify end-to-end `localhost:8080`.
- Push branch / open stacked PRs C1→C2 (+ C3) — user decision under ordinary repository policy.
