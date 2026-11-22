# E6 — Docker, CI/CD & Render Deployment

| Field | Value |
|---|---|
| Owner | Dev1 |
| Reviewer | Dev2 |
| Branch | `feature/E6-docker-cicd` |
| Depends on | E1 (env-var config, PostgreSQL profile). S1–S3 can start right after E1 |
| Blocks | E7 (compose services), final release |
| Rubric targets | Dockerization (10), GitHub Actions + auto-deploy (10), Deployment (5); avoids the automatic fails "No Dockerization" and "App not deployed" |

## Goal
`docker compose up --build` runs the whole stack locally with no hardcoded credentials. GitHub Actions builds and tests every PR, and every push to `main` deploys automatically to Render. The app is publicly reachable.

## Stories

### E6-S1 Backend Dockerfile (multi-stage)
`backend/Dockerfile`:
```dockerfile
# ---- build stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build /app/target/*.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD wget -qO- http://localhost:8080/api/v1/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```
- Add `backend/.dockerignore` (`target/`, `.idea/`, `*.iml`, `.env`, `uploads/`).
- Tests are skipped inside the image because CI runs them. Explain this in the README.

### E6-S2 docker-compose
`docker-compose.yml` at the repo root:
```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${DB_NAME}"]
      interval: 5s
      retries: 10
  app:
    build: ./backend
    depends_on: { db: { condition: service_healthy } }
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:postgresql://db:5432/${DB_NAME}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      ADMIN_EMAIL: ${ADMIN_EMAIL}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD}
      CORS_ALLOWED_ORIGIN: ${CORS_ALLOWED_ORIGIN}
      LLM_API_KEY: ${LLM_API_KEY:-}
    ports: ["8080:8080"]
    volumes: [uploads:/app/uploads]
  frontend:
    image: nginx:1.27-alpine
    volumes: ["./frontend:/usr/share/nginx/html:ro"]
    ports: ["3000:80"]
volumes: { pgdata: {}, uploads: {} }
```
- Add `.env.example` at the repo root with every variable (dummy values). The developer runs `cp .env.example .env` and then `docker compose up --build`.
- `frontend/js/config.js` must point to `http://localhost:8080/api/v1` for compose.

**Acceptance:** on a clean machine, `cp .env.example .env && docker compose up --build` gives an API on :8080 and the UI on :3000, and login works.

### E6-S3 CI pipeline (extend `.github/workflows/ci.yml`)
Keep the job `build-and-test` (its name is referenced by branch protection). Add:
- `docker-build` job (`needs: build-and-test`): runs `docker build ./backend` to prove the image builds. No push needed.
- Test report: publish the surefire results (e.g. `dorny/test-reporter@v1` or the uploaded artifact), so the CI test count is visible to the examiner.
- Optional: start a `postgres:16` service and run ITs against it with `SPRING_PROFILES_ACTIVE=test-pg`. This is a nice touch but not required.

### E6-S4 Render setup (manual, documented)
1. On Render, create a **PostgreSQL** instance (free) and copy its *Internal Database URL* parts.
2. Create a **Web Service** from the GitHub repo:
   - Runtime **Docker**, root directory `backend`, Dockerfile path `./Dockerfile`
   - Branch `main`
   - **Auto-Deploy: Off**, because the deploy is triggered by GitHub Actions only after tests pass
   - Health check path: `/api/v1/health`
   - Env vars: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL=jdbc:postgresql://<host>:5432/<db>`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (generate it), `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `CORS_ALLOWED_ORIGIN=https://<frontend>.onrender.com`, `LLM_API_KEY` (optional). Render sets `PORT`, and `server.port=${PORT:8080}` handles it.
3. Create a **Static Site** for `frontend/` (publish dir `frontend`, no build command). Set `config.js` `API_BASE` to the backend URL. The simplest approach is a `frontend/js/config.js` that picks the URL by `location.hostname`.
4. In Render Web Service → Settings → **Deploy Hook**, copy the URL. Add it in GitHub → Settings → Secrets and variables → Actions as **`RENDER_DEPLOY_HOOK_URL`**. Do the same for the static site (`RENDER_FRONTEND_DEPLOY_HOOK_URL`).
5. Add `render.yaml` (Blueprint) at the repo root describing both services, as documentation and infrastructure-as-code.

### E6-S5 CD job
In `ci.yml` (or a separate `deploy.yml` with `on: push: branches: [main]`):
```yaml
  deploy:
    needs: [build-and-test, docker-build]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Trigger Render backend deploy
        run: curl -fsS -X POST "${{ secrets.RENDER_DEPLOY_HOOK_URL }}"
      - name: Trigger Render frontend deploy
        run: curl -fsS -X POST "${{ secrets.RENDER_FRONTEND_DEPLOY_HOOK_URL }}"
      - name: Wait for health
        run: |
          for i in $(seq 1 30); do
            curl -fsS https://<backend>.onrender.com/api/v1/health && exit 0; sleep 20; done; exit 1
```
Deploys only happen from `main`, and only after the tests pass.

### E6-S6 Verify and document
- Do a release PR from `develop` to `main`, then confirm the Actions deploy job is green and the live URL works (register, log in, view the routine).
- Record the live URLs in `README.md` and `docs/agent.md` (notes).
- Take screenshots for the demo: a green Actions run, the Render dashboard and the live app.

## Definition of Done
- [ ] `docker compose up --build` works from a clean clone with `.env`
- [ ] No credentials in the Dockerfile, compose, `render.yaml` or the repo
- [ ] CI: build-and-test + docker-build on PRs; deploy on push to `main`
- [ ] The live Render URL is public, and `/api/v1/health` returns UP
- [ ] `docs/agent.md` updated
