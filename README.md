# ClassSync — Progressive Web App for CR / Teacher / Student workflow

Monorepo, frontend and backend fully separated.

```
backend/    Java Spring Boot REST API (modular, package-by-module)
frontend/   Installable PWA (HTML/CSS/JS, service worker, offline cache)
docs/       Plans, architecture, per-person task prompts
```

## Documentation
- [docs/PROJECT_GUIDELINE.md](docs/PROJECT_GUIDELINE.md): CSE 3220 requirements & rubric
- [docs/TEAM_PLAN.md](docs/TEAM_PLAN.md): epics, stories, owners
- [docs/epics/](docs/epics/): detailed spec per epic + shared conventions
- [docs/agent.md](docs/agent.md): instructions for AI agents & epic status board
- [docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md): branching, commits, PR & review
- [docs/API.md](docs/API.md): REST API reference

## Run
- Frontend: `cd frontend && npx serve .` (or any static server) → http://localhost:3000
- Backend: needs PostgreSQL and the env vars from [backend/.env.example](backend/.env.example) (at least `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`), then `cd backend && mvn spring-boot:run` → http://localhost:8080 (Swagger: `/swagger-ui.html`)
- Tests: `cd backend && mvn clean verify` (H2, no database needed)
