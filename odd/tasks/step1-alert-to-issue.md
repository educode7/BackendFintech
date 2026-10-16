# ODD Feature: Step 1 — Alert → GitHub Issue (SRE loop, no-LLM, no-autonomy)

## Objetivo
Cuando Prometheus detecta una falla, Alertmanager crea automáticamente un issue en
GitHub con toda la evidencia. Un humano luego elige qué agente (Claude, Qwen, Kimi…)
usa para resolverlo. **Sin LLM en el loop y sin ejecución autónoma**: solo registrar.

## Problema / Por qué
Hoy Prometheus scrapea métricas pero no evalúa nada: no hay reglas de alerta ni
Alertmanager. Si algo se cae, nadie lo registra ni notifica. Esto es el paso 1 del
caso de uso "Claude Code for SRE" acotado a: Alerta → Issue → humano+agente después.

## Scope
- IN: reglas de alerta, Alertmanager, webhook receiver que crea/cierra issues en GitHub,
  runbook de setup, verificación mecánica.
- OUT: cualquier invocación de LLM, despliegues autónomos, CI/CD (paso 2), Kubernetes.

## Constraints
- No acoplar a ningún proveedor de LLM.
- El receiver solo registra issues (mutación limitada a GitHub Issues).
- Secretos fuera del repo (`.env` gitgeado en `.gitignore`).
- Artefactos técnicos en inglés; runbook en español (consistente con el repo).

## Autoridad / Ruta
- Ruta: delegated direct (writer delegado si el runtime lo permite; si no, inline).
- Disparadores: writer trigger (5+ archivos no triviales).

## Checklist

- [x] T1: Feature branch `feat/alert-to-github-issue` desde `main` (base `1253cb8`)
- [x] T2: `monitoring/alerts/alert-rules.yml` — reglas (ServiceDown, HighErrorRate,
      HighLatency, JvmHeapPressure, PostgresDown) + montaje y `rule_files` en
      `prometheus.yml` + `alertmanagers` config
- [x] T3: `monitoring/alerts/alertmanager.yml` + servicio `alertmanager` en
      `docker-compose.yml` (ruta al webhook con Bearer secret)
- [x] T4: Webhook receiver Node zero-dep (`monitoring/alert-webhook/`): valida secret,
      crea issue con fingerprint marker, dedup en firing, cierra en resolved,
      modo `DRY_RUN`
- [x] T5: `.env.example` (GITHUB_TOKEN, GITHUB_REPO, WEBHOOK_SECRET, DRY_RUN) +
      entrada `.env` en `.gitignore` + `webhook_secret.txt.example`
- [x] T6: Runbook `monitoring/alerts/README.md` (setup + prueba con curl)
- [x] T7: Verificación: `promtool check rules`, `docker compose config`, dry-run
      curl con payload de ejemplo
- [ ] T8: Work-unit commit(s) Conventional Commits

## Acceptance criteria
- `promtool check rules` pasa con las reglas nuevas.
- `docker compose config` válido con servicios alertmanager + alert-webhook.
- POST de ejemplo al receiver en DRY_RUN devuelve 200 y payload de issue armado.
- Ningún token/secret en el repo.

## Progress / Evidencia
- Commits (rama `feat/alert-to-github-issue`, base `1253cb8`):
  - `54fe0a6` feat(monitoring): add prometheus alert rules and alertmanager routing — T2 + T3 (reglas + ruteo)
  - `1cdc679` feat(alert-webhook): turn alertmanager alerts into github issues — T4 + T5 + wiring de deploy (compose, env, gitignore)
  - C3 docs(alerts): runbook + este feature doc — T6 + T8 (hash registrado en el commit de cierre)
- T2–T6: artefactos escritos en el working tree (rama sin commits propios todavía).
- T7 (verificación ejecutada el 2026-10-15):
  - `docker compose config --quiet` → OK; `--services` lista `prometheus`,
    `alertmanager`, `alert-webhook`.
  - `docker run --rm --entrypoint promtool prom/prometheus:latest check rules
    /rules/alert-rules.yml` → `SUCCESS: 5 rules found`.
  - Receiver en `DRY_RUN=true` (node v24 local, puerto 9099, secreto de prueba):
    - `GET /health` → 200 `{"ok":true}`
    - `POST /webhook` sin Authorization → 401 `unauthorized`
    - `POST /webhook` con Bearer incorrecto → 401 `unauthorized`
    - `POST /webhook` firing + Bearer correcto → 200
      `{"dry_run":true,"actions":["DRY_RUN would create issue: [critical] ServiceDown — payment-service is down"]}`
    - `POST /webhook` resolved + Bearer correcto → 200
      `{"dry_run":true,"actions":["DRY_RUN close attempt: no open issue found ... (marker <!-- alert:abc123def456 -->)"]}`
    - `POST /webhook` JSON malformado → 400 `malformed JSON`
    - `POST /webhook` sin `alerts[]` → 400 `payload missing alerts[] array`
- Riesgo: item 2 (guard Bearer + manejo de secretos/PAT) e item 5 (superficie de
  despliegue en `docker-compose.yml`). Prueba real contra la API de GitHub
  (crear/comentar/cerrar issue) no ejecutada: requiere `GITHUB_TOKEN` del usuario
  y `DRY_RUN=false`; queda fuera de la verificación mecánica local.

## Next step
- T8: work-unit commits (el receiver + `.env`/secretos va en su propio commit por
  ser la unidad de riesgo alto).

## Delivery forecast
- ~6-7 archivos nuevos/tocados, estimado < 400 líneas → `ask-on-risk` probablemente
  no se activa; estrategia: single PR al final (pendiente de decisión si excede).
- delivery_strategy: ask-on-risk (default), chain_strategy: n/a por ahora.
