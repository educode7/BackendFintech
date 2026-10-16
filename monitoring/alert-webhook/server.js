"use strict";

/**
 * Alertmanager -> GitHub Issue webhook receiver.
 *
 * Zero-dependency Node 20 (built-in http + global fetch).
 * It only creates, comments on, and closes GitHub issues.
 * No LLM calls, no autonomous execution beyond GitHub issue bookkeeping.
 */

const http = require("http");

const PORT = parseInt(process.env.PORT || "9095", 10);
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || "";
const GITHUB_TOKEN = process.env.GITHUB_TOKEN || "";
const GITHUB_REPO = process.env.GITHUB_REPO || "educode7/BackendFintech";
const DRY_RUN =
  !["false", "0", "no"].includes(String(process.env.DRY_RUN || "true").toLowerCase());
// Dry-run also applies when no token is configured: never call GitHub blind.
const EFFECTIVE_DRY_RUN = DRY_RUN || GITHUB_TOKEN === "";

const GITHUB_API = "https://api.github.com";

const PROMQL_HINTS = {
  ServiceDown:
    'up{job=~"api-gateway|payment-service|account-service|notification-service|auth-service"} == 0',
  HighErrorRate:
    "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) by (job) / sum(rate(http_server_requests_seconds_count[5m])) by (job)",
  HighLatency:
    "sum(rate(http_server_requests_seconds_sum[5m])) by (job) / sum(rate(http_server_requests_seconds_count[5m])) by (job)",
  JvmHeapPressure: 'jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}',
  PostgresDown: "pg_up",
};

/** Issue body marker used for dedup / close detection. */
function markerFor(fingerprint) {
  return `<!-- alert:${fingerprint} -->`;
}

function buildIssueTitle(alert) {
  const severity = String((alert.labels && alert.labels.severity) || "info").toLowerCase();
  const alertname = (alert.labels && alert.labels.alertname) || "UnknownAlert";
  const summary = (alert.annotations && alert.annotations.summary) || alertname;
  return `[${severity}] ${alertname} \u2014 ${summary}`;
}

function buildIssueBody(alert) {
  const labels = alert.labels || {};
  const annotations = alert.annotations || {};
  const marker = markerFor(alert.fingerprint || "unknown");
  const alertname = labels.alertname || "UnknownAlert";
  const hint =
    PROMQL_HINTS[alertname] ||
    "the alert expression from monitoring/alerts/alert-rules.yml";

  const labelRows = Object.entries(labels)
    .map(([k, v]) => `| \`${k}\` | \`${v}\` |`)
    .join("\n");

  const annotationRows = Object.entries(annotations)
    .map(([k, v]) => `| \`${k}\` | ${v} |`)
    .join("\n");

  return [
    marker,
    "",
    `**Status:** ${alert.status || "firing"}`,
    "",
    "### Labels",
    "",
    "| Label | Value |",
    "| --- | --- |",
    labelRows || "| _(none)_ | |",
    "",
    "### Annotations",
    "",
    "| Key | Value |",
    "| --- | --- |",
    annotationRows || "| _(none)_ | |",
    "",
    `- **Starts at:** ${alert.startsAt || "n/a"}`,
    `- **Ends at:** ${alert.endsAt || "n/a"}`,
    `- **Generator:** [Prometheus](${alert.generatorURL || "http://localhost:9090/alerts"})`,
    "",
    "## Evidence to gather",
    "",
    "Guidance for the human/agent that picks this up. Nothing here runs automatically.",
    "",
    "- [ ] PromQL: evaluate the rule expression in Prometheus (http://localhost:9090/graph):",
    `      \`${hint}\``,
    "- [ ] Logs: `docker logs wallet-<job> --since 30m` or query Loki via Grafana (http://localhost:3000)",
    '- [ ] Recent changes: `git log --oneline --since="2 hours ago"`',
    "",
  ].join("\n");
}

function severityLabel(alert) {
  const severity = String((alert.labels && alert.labels.severity) || "info").toLowerCase();
  return `severity-${severity}`;
}

/** GitHub REST call. Throws on non-2xx (caller maps to 502). */
async function githubRequest(path, options) {
  const res = await fetch(`${GITHUB_API}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${GITHUB_TOKEN}`,
      Accept: "application/vnd.github+json",
      "X-GitHub-Api-Version": "2022-11-28",
      "Content-Type": "application/json",
      ...(options && options.headers),
    },
  });
  if (res.status < 200 || res.status >= 300) {
    const text = await res.text().catch(() => "");
    const err = new Error(
      `GitHub API ${options && options.method ? options.method : "GET"} ${path} -> ${res.status}: ${text.slice(0, 500)}`
    );
    err.githubStatus = res.status;
    throw err;
  }
  if (res.status === 204) return null;
  return res.json().catch(() => null);
}

/** Open issues labeled `alertmanager` whose body carries our marker. */
async function findOpenIssues(marker) {
  const issues = await githubRequest(
    `/repos/${GITHUB_REPO}/issues?state=open&labels=alertmanager&per_page=100`,
    { method: "GET" }
  );
  if (!Array.isArray(issues)) return [];
  return issues.filter(
    (issue) =>
      !issue.pull_request && typeof issue.body === "string" && issue.body.includes(marker)
  );
}

/**
 * Process a single alert. Returns a list of human-readable actions taken
 * (or the actions that WOULD be taken in dry-run mode).
 */
async function processAlert(alert) {
  const status = alert.status || "firing";
  const marker = markerFor(alert.fingerprint || "unknown");
  const title = buildIssueTitle(alert);

  if (EFFECTIVE_DRY_RUN) {
    if (status === "resolved") {
      // Dry-run never queries GitHub, so there is no open issue to close.
      return `DRY_RUN close attempt: no open issue found for ${title} (marker ${marker})`;
    }
    return `DRY_RUN would create issue: ${title}`;
  }

  const openIssues = await findOpenIssues(marker);

  if (status === "resolved") {
    if (openIssues.length === 0) {
      return `no open issue found for resolved alert: ${title}`;
    }
    const actions = [];
    for (const issue of openIssues) {
      await githubRequest(`/repos/${GITHUB_REPO}/issues/${issue.number}/comments`, {
        method: "POST",
        body: JSON.stringify({ body: `\u2705 Resolved at ${alert.endsAt || "n/a"}` }),
      });
      await githubRequest(`/repos/${GITHUB_REPO}/issues/${issue.number}`, {
        method: "PATCH",
        body: JSON.stringify({ state: "closed" }),
      });
      actions.push(`closed issue #${issue.number}: ${title}`);
    }
    return actions.join("; ");
  }

  // status === "firing"
  if (openIssues.length > 0) {
    const actions = [];
    for (const issue of openIssues) {
      await githubRequest(`/repos/${GITHUB_REPO}/issues/${issue.number}/comments`, {
        method: "POST",
        body: JSON.stringify({
          body: `\u{1F514} Still firing since ${alert.startsAt || "n/a"}`,
        }),
      });
      actions.push(`commented on issue #${issue.number}: ${title}`);
    }
    return actions.join("; ");
  }

  const created = await githubRequest(`/repos/${GITHUB_REPO}/issues`, {
    method: "POST",
    body: JSON.stringify({
      title,
      body: buildIssueBody(alert),
      labels: ["alertmanager", "incident", severityLabel(alert)],
    }),
  });
  return `created issue #${created ? created.number : "?"}: ${title}`;
}

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(body),
  });
  res.end(body);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", (chunk) => chunks.push(chunk));
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    req.on("error", reject);
  });
}

const server = http.createServer(async (req, res) => {
  const path = (req.url || "").split("?")[0];

  if (req.method === "GET" && path === "/health") {
    return sendJson(res, 200, { ok: true });
  }

  if (req.method === "POST" && path === "/webhook") {
    // Fail closed: no secret configured means refuse everything.
    if (WEBHOOK_SECRET === "") {
      console.error("webhook rejected: WEBHOOK_SECRET is not configured");
      return sendJson(res, 401, { ok: false, error: "unauthorized" });
    }
    if (req.headers.authorization !== `Bearer ${WEBHOOK_SECRET}`) {
      return sendJson(res, 401, { ok: false, error: "unauthorized" });
    }

    let payload;
    try {
      const raw = await readBody(req);
      payload = JSON.parse(raw);
    } catch (err) {
      return sendJson(res, 400, { ok: false, error: "malformed JSON" });
    }

    if (!payload || !Array.isArray(payload.alerts)) {
      return sendJson(res, 400, { ok: false, error: "payload missing alerts[] array" });
    }

    const actions = [];
    try {
      for (const alert of payload.alerts) {
        const action = await processAlert(alert);
        if (action) actions.push(action);
        console.log(`alert ${alert.status || "firing"} ${action}`);
      }
    } catch (err) {
      console.error(`GitHub API error: ${err.message}`);
      return sendJson(res, 502, { ok: false, error: "github api error" });
    }

    return sendJson(res, 200, EFFECTIVE_DRY_RUN ? { dry_run: true, actions } : { dry_run: false, actions });
  }

  return sendJson(res, 404, { ok: false, error: "not found" });
});

server.listen(PORT, () => {
  console.log(
    `alert-webhook listening on :${PORT} repo=${GITHUB_REPO} dry_run=${EFFECTIVE_DRY_RUN}`
  );
});
