# E8 — Documentation & Demo

| Field | Value |
|---|---|
| Status | **Not done** |
| Owner | Dev2 |
| Reviewer | Dev1 |
| Branch | `feature/E8-docs` (can be opened several times: `feature/E8-docs-api`, `feature/E8-docs-final`) |
| Depends on | Content from every epic. Start the skeleton early, finish after E6 |
| Blocks | final submission |
| Rubric targets | Documentation (5), Proper demonstration (5), plus it supports every other rubric item by making it visible |

## Goal
A clear `README.md` on GitHub that covers everything the guideline lists under deliverables, an always-current API reference, and a rehearsed 15-minute demo.

## Stories

### E8-S1 README: overview
`README.md` sections, in this order:
1. Title, one-line description, badges (CI status from Actions, live demo link)
2. **Live URLs**: Render frontend, Render API, second deployment, Swagger UI (`/swagger-ui.html`)
3. **Team**: Dev1/Dev2/Dev3 names, roll numbers and owned epics
4. **Features** by role: ADMIN / CR / TEACHER / STUDENT
5. **Tech stack**: Spring Boot 3, Spring Security + JWT + BCrypt, JPA/Hibernate, PostgreSQL 16, WebSocket/STOMP, Docker, GitHub Actions, Render, Prometheus/Grafana, and the vanilla JS PWA
6. **Design patterns**: a table of pattern → class → why (copy and verify from TEAM_PLAN §4 against the real code)

### E8-S2 Diagrams (Mermaid, rendered by GitHub)
- **Architecture diagram**: `flowchart LR` showing Browser/PWA → Render Static Site; PWA → REST/WebSocket → Spring Boot (controllers → services → repositories) → PostgreSQL; Spring Boot → LLM API; Prometheus → `/actuator/prometheus`; Grafana → Prometheus; GitHub Actions → Render deploy hook.
- **ER diagram**: `erDiagram` with **all** tables from TEAM_PLAN §3, plus `attachments` and `assistant_actions`, showing cardinalities (`||--o{`, `}o--o{` for `user_roles` and `teacher_subjects`). Generate it from the actual entities and check it against them.
- **CI/CD diagram**: feature → PR → develop → release PR → main → Actions (test → docker build → deploy) → Render.

### E8-S3 API reference
Create `docs/API.md` (linked from the README). Each epic appends its endpoints there. One table per module:

| Method | Path | Roles | Request | Success | Errors |
|---|---|---|---|---|---|
| POST | /api/v1/auth/login | public | `{email,password}` | 200 `{token,…}` | 400, 401 |

Add 2–3 copy-paste `curl` examples (register, login, create a notice with the token). Mention Swagger UI.

### E8-S4 Run instructions + CI/CD explanation
- **Run locally (Docker)**: `cp .env.example .env` → `docker compose up --build` → URLs for :3000, :8080, :9090, :3001.
- **Run locally (without Docker)**: PostgreSQL, env vars, `mvn spring-boot:run`, `npx serve frontend`.
- **Run tests**: `cd backend && mvn clean verify`, with the test count.
- **Environment variables** table: name, purpose, example, required?
- **Git workflow**: a summary of `docs/GIT_WORKFLOW.md` (branches, protection, PR + 1 approval) plus a screenshot of the branch protection settings.
- **CI/CD explanation**: what each job does, when it runs, the secrets used, and why Render auto-deploy is off (so deploys happen only after tests pass).

### E8-S5 Demo (15 minutes)
- Seed data: `common.config.DemoDataSeeder`, active only with `app.demo-data=true`. It creates the users `admin@`, `cr@`, `teacher@`, `student1@`, `student2@` (passwords documented in the README for the demo only), 3 subjects, a week of routine slots, 3 notices, 3 assignments, 1 form and some chat messages.
- `docs/DEMO_SCRIPT.md`, roughly 15 minutes:
  | Min | Who | What |
  |---|---|---|
  | 0–2 | Dev1 | Problem, architecture diagram, ER diagram |
  | 2–5 | Dev1 | Security: register/login, a 403 as student, the BCrypt hash in the DB, branch protection, CI run |
  | 5–9 | Dev2 | CR routine edit + undo, notice, teacher assignment + file, student completes, private comment |
  | 9–12 | Dev3 | Live chat in 2 browsers, notification toast, AI assistant Q&A + confirm a routine change |
  | 12–14 | Dev1/Dev3 | Docker compose, Render live URL, Grafana dashboard, second deployment |
  | 14–15 | all | Design patterns summary, tests count, Q&A |
- Rehearse once end-to-end on the **live** URL; keep a local docker-compose run as a backup.

## Definition of Done
- [ ] The README contains: description, architecture diagram, ER diagram, API endpoints, run instructions, CI/CD explanation, live URL, repo link
- [ ] `docs/API.md` is complete for every merged endpoint
- [ ] `docs/DEMO_SCRIPT.md` exists, and the demo seed works
- [ ] `docs/agent.md` updated
