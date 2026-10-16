# Alertas → GitHub Issues

Flujo: **Prometheus (reglas) → Alertmanager → webhook receiver → GitHub Issue**.

Sin LLM en el loop y sin ejecución autónoma: el receiver solo **crea, comenta y
cierra** issues de GitHub. La evidencia se junta en el issue y decide un humano
qué agente usa después.

| Componente | Servicio / archivo | URL |
| --- | --- | --- |
| Reglas de alerta | `monitoring/alerts/alert-rules.yml` | http://localhost:9090/alerts |
| Alertmanager | `alertmanager` (`wallet-alertmanager`) | http://localhost:9093 |
| Webhook receiver | `alert-webhook` (`wallet-alert-webhook`) | http://localhost:9095/health |
| Issues | repo `educode7/BackendFintech` con label `alertmanager` | — |

## Setup

1. **Variables de entorno** — copia el ejemplo y edítalo:

   ```bash
   cp .env.example .env
   ```

   - `GITHUB_TOKEN`: PAT de GitHub **fine-grained** con permiso
     *Issues: Read and write* sobre este repo (nada más).
   - `WEBHOOK_SECRET`: secreto compartido entre Alertmanager y el receiver.
     Genera uno con `openssl rand -hex 32` (o `[guid]::NewGuid()` en PowerShell).

2. **Secreto de Alertmanager** — el archivo que lee Alertmanager
   (`credentials_file`) debe contener exactamente el mismo valor que
   `WEBHOOK_SECRET`:

   ```bash
   openssl rand -hex 32 > monitoring/alerts/webhook_secret.txt
   ```

   (Existe `webhook_secret.txt.example` como plantilla; el archivo real está en
   `.gitignore` y **nunca** se commitea.)

3. **Modo seguro primero** — déjalo en `DRY_RUN=true` en `.env`: el receiver no
   llama a GitHub, solo imprime la acción que haría. Cuando verifiques el flujo
   real, cambia a `DRY_RUN=false`.

4. **Levantar el stack de monitoreo:**

   ```bash
   docker compose up -d prometheus alertmanager alert-webhook
   ```

   Prometheus monta `monitoring/alerts/` en `/etc/prometheus/alerts/` y
   evalúa las reglas cada 15s; Alertmanager reenvía los grupos de alertas al
   receiver con `Authorization: Bearer <WEBHOOK_SECRET>`.

## Prueba manual con curl

No hace falta esperar a que una alerta dispare. Envía un payload de ejemplo de
Alertmanager (modo firing) directo al receiver:

```bash
curl -i -X POST http://localhost:9095/webhook \
  -H "Authorization: Bearer $WEBHOOK_SECRET" \
  -H "Content-Type: application/json" \
  -d '{
    "receiver": "issue-webhook",
    "status": "firing",
    "alerts": [
      {
        "status": "firing",
        "labels": {
          "alertname": "ServiceDown",
          "severity": "critical",
          "team": "platform",
          "job": "payment-service",
          "instance": "payment-service:8081"
        },
        "annotations": {
          "summary": "Service payment-service is down",
          "description": "Target payment-service:8081 of job payment-service has been unreachable for 2 minutes."
        },
        "startsAt": "2026-01-15T10:00:00Z",
        "endsAt": "0001-01-01T00:00:00Z",
        "fingerprint": "a1b2c3d4e5f60718",
        "generatorURL": "http://localhost:9090/graph?g0.expr=up%7Bjob%3D%22payment-service%22%7D%20%3D%3D%200"
      }
    ],
    "groupLabels": { "alertname": "ServiceDown" },
    "commonLabels": { "alertname": "ServiceDown", "severity": "critical" },
    "commonAnnotations": { "summary": "Service payment-service is down" },
    "externalURL": "http://localhost:9093"
  }'
```

Esperado con `DRY_RUN=true` → `HTTP 200` y algo como:

```json
{"dry_run":true,"actions":["DRY_RUN would create issue: [critical] ServiceDown \u2014 Service payment-service is down"]}
```

Comprobaciones rápidas:

- Secret incorrecto o ausente → `401` (el receiver **falla cerrado**: sin
  `WEBHOOK_SECRET` configurado rechaza todo).
- JSON malformado → `400`.
- `GET http://localhost:9095/health` → `200 {"ok":true}`.

Para cerrar la alerta en el mismo flujo, reenvía el mismo payload con
`"status": "resolved"` en el alert (y `endsAt` con fecha real); en DRY_RUN verás
`close attempt ... no open issue found`, y en modo real la issue se comenta y se
cierra.

## Cómo se ven los issues

- **Título:** `[critical] ServiceDown — Service payment-service is down`
  (severidad en minúsculas).
- **Labels:** `alertmanager`, `incident`, `severity-critical` (o
  `severity-warning`).
- **Cuerpo (markdown, inglés):** marcador oculto `<!-- alert:<fingerprint> -->`
  para deduplicación, estado, tablas de labels/annotations, `startsAt`/`endsAt`,
  enlace al gráfico de Prometheus y un checklist **"Evidence to gather"**
  (PromQL, logs con `docker logs`/Grafana http://localhost:3000, y
  `git log --since`) como guía — **no** se ejecuta nada de eso automáticamente.
- **Dedup:** mientras la alerta siga `firing`, cada notificación añade un
  comentario `🔔 Still firing since <startsAt>` a la misma issue. Al pasar a
  `resolved` se comenta `✅ Resolved at <endsAt>` y la issue se **cierra**.

## Advertencias de seguridad

- `WEBHOOK_SECRET` y `GITHUB_TOKEN` viven en `.env` y
  `monitoring/alerts/webhook_secret.txt`; ambos están en `.gitignore`.
  **Nunca** los subas al repo ni los imprimas en logs.
- El token debe ser **fine-grained**, limitado a este repo, con el permiso
  mínimo *Issues: Read and write*. Nada de admin, contents ni acciones.
- El endpoint `/webhook` valida el Bearer en cada petición; sin secreto
  configurado responde `401` a todo (fail closed).
- Si `GITHUB_TOKEN` está vacío, el receiver entra en modo DRY_RUN aunque
  `DRY_RUN=false`: jamás llama a GitHub sin credenciales.

## Referencias

- Reglas: http://localhost:9090/alerts
- Alertmanager: http://localhost:9093
- Grafana (métricas + Loki): http://localhost:3000
