# ClassSync — Progressive Web App for CR / Teacher / Student workflow

Monorepo, frontend and backend fully separated.

```
backend/    Java Spring Boot REST API (modular, package-by-module)
frontend/   Installable PWA (HTML/CSS/JS, service worker, offline cache)
docs/       Plans, architecture, per-person task prompts
```

See [docs/TEAM_PLAN.md](docs/TEAM_PLAN.md) for the 3-person plan and prompts.

## Run
- Frontend: `cd frontend && npx serve .` (or any static server) → http://localhost:3000
- Backend: `cd backend && mvn spring-boot:run` → http://localhost:8080
