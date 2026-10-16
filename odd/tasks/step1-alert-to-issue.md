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
- [x] T8: Work-unit commit(s) Conventional Commits

## Acceptance criteria
- `promtool check rules` pasa con las reglas nuevas.
- `docker compose config` válido con servicios alertmanager + alert-webhook.
- POST de ejemplo al receiver en DRY_RUN devuelve 200 y payload de issue armado.
- Ningún token/secret en el repo.

## Progress / Evidencia
- Commits (rama `feat/alert-to-github-issue`, base `1253cb8`):
  - `54fe0a6` feat(monitoring): add prometheus alert rules and alertmanager routing — T2 + T3 (reglas + ruteo)
  - `1cdc679` feat(alert-webhook): turn alertmanager alerts into github issues — T4 + T5 + wiring de deploy (compose, env, gitignore)
  - `e455d1f` docs(alerts): runbook + este feature doc — T6 + T8 (C3)
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
- Verificación independiente (subagente `gentle-ai-verify`, 2026-10-15) sobre
  `1253cb8..e455d1f` en copia scratch: verdict **pass**, sin blockers. Re-ejecutó
  AC1–AC4 (promtool rules/config, amtool check-config, `docker compose config`,
  los 7 casos curl del receiver, fail-closed con `WEBHOOK_SECRET` vacío, grep de
  secretos en los 3 commits, y `server.js` sin child_process/LLM). Extras:
  `DRY_RUN=false` + token vacío sigue en dry-run; `alerts:[]` → 200 `actions:[]`;
  `docker compose config` pasa de 19 a 21 servicios (superset estricto).
  Advisories (todas pre-existentes o de entorno, ninguna defecto del cambio):
  1. el bind-mount `webhook_secret.txt` se crea como **directorio** si no existe
     el archivo antes de `compose up` → Alertmanager falla al leer
     `credentials_file`; 2. el dir `monitoring/alerts/` montado en Prometheus
     expone el secreto dentro de ese contenedor (read-only, mismo host, no es
     fuga al repo); 3. `webhook_secret.txt.example` dice `change-me` en vez de
     vacío; 4. el checklist marcaba T8 sin cerrar (ya corregido).
- Riesgo: item 2 (guard Bearer + manejo de secretos/PAT) e item 5 (superficie de
  despliegue en `docker-compose.yml`). Prueba real contra la API de GitHub
  (crear/comentar/cerrar issue) no ejecutada: requiere `GITHUB_TOKEN` del usuario
  y `DRY_RUN=false`; queda fuera de la verificación mecánica local.

## Next step
- Feature cerrada a nivel de código y verificación. Falta solo lo que decide el
  usuario: push de la rama y PR (`feat/alert-to-github-issue` → `main`).
- Presupuesto de entrega: ~648 líneas authored (85 + 336 + 227), supera el umbral
  de ~400 → al hacer el PR hay que elegir estrategia de cadena
  (`stacked-to-main` o `feature-branch-chain`) o confirmar un PR único.

## Delivery forecast
- Estimado inicial: ~6-7 archivos, < 400 líneas, single PR.
- Real: 648 líneas authored (85 + 336 + 227) + 25 líneas de docs de cierre → supera
  el umbral de ~400.
- delivery_strategy: ask-on-risk (default) → **disparado**: hay que preguntar una
  vez por la estrategia de cadena antes de abrir el PR.
