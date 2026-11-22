# E7 — Bonus: Monitoring & Second Deployment

| Field | Value |
|---|---|
| Owner | Dev3 |
| Reviewer | Dev1 |
| Branch | `feature/E7-observability` |
| Depends on | E1 (security), E6 (compose, Dockerfile) |
| Blocks | none |
| Rubric targets | Bonus: Actuator (2), Prometheus (4), Grafana (4), second platform (5) + documentation (5) = up to **20 bonus marks** |

## Goal
Expose application metrics, scrape them with Prometheus, visualise them in a provisioned Grafana dashboard (all in docker-compose), and deploy the same Docker image to a second platform.

## Stories

### E7-S1 Spring Boot Actuator
- Dependencies: `spring-boot-starter-actuator` and `micrometer-registry-prometheus`.
- Properties:
  ```properties
  management.endpoints.web.exposure.include=health,info,prometheus,metrics
  management.endpoint.health.probes.enabled=true
  management.endpoint.health.show-details=when-authorized
  management.metrics.tags.application=${spring.application.name}
  info.app.name=ClassSync
  info.app.version=@project.version@
  ```
- Security (update E1's `SecurityConfig`): `/actuator/health/**` and `/actuator/info` are public; `/actuator/prometheus` is allowed with HTTP Basic for the user `PROMETHEUS_USER`/`PROMETHEUS_PASSWORD` (env), or restricted to the internal docker network. Pick one and document it. Everything else under `/actuator/**` requires ADMIN.
- Custom business metrics (a Micrometer `Counter`): `classsync_notices_created_total`, `classsync_assignments_completed_total`, `classsync_chat_messages_total`, incremented in the respective services.

### E7-S2 Prometheus
- `monitoring/prometheus/prometheus.yml`:
  ```yaml
  global: { scrape_interval: 15s }
  scrape_configs:
    - job_name: classsync
      metrics_path: /actuator/prometheus
      static_configs: [{ targets: ['app:8080'] }]
      # basic_auth: { username: ..., password: ... }  if S1 chose basic auth
  ```
- Add a compose service `prometheus` (`prom/prometheus:v2.54.1`) with port `9090:9090` and the config mounted read-only.

**Acceptance:** the Prometheus UI → Status → Targets shows `classsync` **UP**.

### E7-S3 Grafana
- Compose service `grafana` (`grafana/grafana:11.2.0`), port `3001:3000`, admin password from env `GRAFANA_ADMIN_PASSWORD`.
- Provisioning:
  - `monitoring/grafana/provisioning/datasources/prometheus.yml`: datasource `http://prometheus:9090`, default.
  - `monitoring/grafana/provisioning/dashboards/dashboards.yml`: loads JSON files from `/var/lib/grafana/dashboards`.
  - `monitoring/grafana/dashboards/classsync.json` with these panels:
    1. HTTP request rate: `sum(rate(http_server_requests_seconds_count[1m])) by (uri)`
    2. p95 latency: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))` (enable `management.metrics.distribution.percentiles-histogram.http.server.requests=true`)
    3. Error rate (5xx): `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))`
    4. JVM heap used: `jvm_memory_used_bytes{area="heap"}`
    5. Business counters: notices created, assignments completed, chat messages
    6. App uptime: `process_uptime_seconds`

**Acceptance:** `docker compose up` gives Grafana at :3001 with the dashboard pre-loaded and showing live data. Take a screenshot for the README and demo.

### E7-S4 Second deployment
Choose one platform that runs Docker, e.g. **Railway**, **Fly.io** or **Koyeb**:
- Add its config file (`railway.json` / `fly.toml` / `koyeb.yaml`) at the repo root or in `deploy/<platform>/`.
- Use a managed PostgreSQL there (or point to a separate DB) with the same env vars as Render.
- Optional: add a GitHub Actions job deploying to it on push to `main` (e.g. `flyctl deploy` with the `FLY_API_TOKEN` secret).

**Acceptance:** a second public URL serves `/api/v1/health` = UP.

### E7-S5 Documentation
Add a README section **"Monitoring"** (how to open Prometheus and Grafana, plus a screenshot) and a section **"Second deployment"** (platform, config file, step-by-step commands, env vars, URL).

## Tests
Bonus epics have no quota, but add:
- `ActuatorIT.health_isPublic`
- `ActuatorIT.prometheus_withoutAuth_returns401` (if basic auth was chosen)

## Definition of Done
- [ ] Prometheus target UP; Grafana dashboard live in compose
- [ ] Second platform URL works
- [ ] README sections written; `docs/agent.md` updated
