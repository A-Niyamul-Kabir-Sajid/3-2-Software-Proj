# agent.md — Instructions for AI agents and developers

Read this file first, before any work in this repository.

> **Always, without being asked:** when an epic's status changes (especially to **Done**), update it in **every** place it appears, in the same commit:
> 1. the §4 status board below,
> 2. the epic heading in `docs/TEAM_PLAN.md` §6 (`· Status: **Done**`) plus its story checkboxes,
> 3. the `| Status |` row and Definition-of-Done checkboxes in `docs/epics/E<n>-*.md`,
> 4. anywhere else the epic's status is mentioned (README, API.md notes, …).
>
> The developer should never have to remind you of this.

## 1. Read before working
1. [docs/PROJECT_GUIDELINE.md](PROJECT_GUIDELINE.md) is the course requirements and marking rubric. Everything must satisfy it.
2. [docs/TEAM_PLAN.md](TEAM_PLAN.md) holds the epics, stories, owners, DB design, pattern map and test quota.
3. [docs/epics/CONVENTIONS.md](epics/CONVENTIONS.md) is the shared package layout, REST/error/security/test conventions and Definition of Done.
4. **`docs/epics/E<n>-*.md` is the detailed spec of the epic you are working on** (entities, endpoints, roles, patterns, exact test names, acceptance criteria). Implement exactly what it says. If something is unclear or conflicts, ask the developer, then record the decision in §6 below.
5. [docs/GIT_WORKFLOW.md](GIT_WORKFLOW.md) covers branching, commit commands, PRs and the review process.
6. [docs/API.md](API.md) lists the endpoints that already exist. Append yours.

## 2. How to work on an epic
When a developer says "work on Epic E<n>":
1. Check the status board below. Confirm the epic's dependencies (listed in the epic spec header) are Done.
2. Branch from an up-to-date `develop`: `git switch develop && git pull && git switch -c feature/E<n>-<slug>`.
3. Set the epic to **In progress** on the board below.
4. Implement the stories in order. Make one or more small Conventional Commits per story, e.g. `feat(routine): add RoutineSlot entity` with `Refs E2-S1` in the body.
5. Write the epic's tests (the exact names are in the spec). `cd backend && mvn clean verify` must pass.
6. Tick finished stories in TEAM_PLAN §6, update the epic status in all 3 places (§3), update the test tracker (§5), and append endpoints to `docs/API.md`.
7. **AI agents must not push or open the PR themselves.** Commit locally, then tell the developer to push and open a PR into `develop` using GIT_WORKFLOW §3. Set the status to `In review`.

### Short version: what a developer does to complete an epic
1. **Sync**: `git switch develop && git pull origin develop`
2. **Branch**: `git switch -c feature/E<n>-<slug>`
3. **Tell the agent**: "Read docs/agent.md and implement Epic E<n>". The agent follows `docs/epics/E<n>-*.md`.
4. **Commit per story** (Conventional Commits) and run `cd backend && mvn clean verify`.
5. **Update docs in the same branch**: tick stories in TEAM_PLAN, set the status here to `In review`, update the test tracker, add rows to `docs/API.md`.
6. **Push and open a PR into `develop`**: `git push -u origin feature/E<n>-<slug>`, then a PR with base `develop`, reviewer **Dev1** (Dev2 for Dev1's PRs). Wait for green CI.
7. **Fix review comments** with new commits and push again.
8. **After the merge**: sync `develop`, delete the branch, and mark the epic `Done` via a small `docs/…` PR into `develop` (GIT_WORKFLOW §5.1).

## 3. Hard rules
- Never commit or push to `main` directly. Direct pushes to `main` are an automatic fail.
- **`docs/` is never updated on `main`.** All docs changes go to `develop`, either in the epic PR or in a `docs/<topic>` branch PR. `main` only gets docs through a `develop → main` release PR. Never open a docs PR into `main`.
- **Keep docs in sync.** Before starting work, check for new docs on `develop` (`git fetch origin && git log HEAD..origin/develop -- docs/`). If there are any, rebase on `develop` first (GIT_WORKFLOW §5.1).
- Never add a `Co-Authored-By:` AI trailer or a "Generated with …" line to commits or PRs.
- No hardcoded credentials. Read them from env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, …).
- Every endpoint is under `/api/v1/**` and protected by role (TEAM_PLAN §2).
- Use design patterns only where they fit, and name classes after them (TEAM_PLAN §4).
- Use the global exception handler and return `ApiError` JSON. Never return raw stack traces.
- The frontend talks to the backend only through `frontend/js/api.js`.

## 4. Epic status board
Status values: `Not done` · `In progress` · `In review` (PR open) · `Done` (merged into develop)
Keep this board, the TEAM_PLAN §6 epic headings and the `| Status |` row of each epic spec identical (see §3).

| Epic | Title | Owner | Branch | Status |
|---|---|---|---|---|
| E0 | Project Setup & Git Workflow | Dev1 | `feature/E0-project-setup`, `feature/E0-followup` | Done |
| E1 | Security, Users & Platform Core | Dev1 | `feature/E1-security-core` | In review |
| E2 | Routine & Notices | Dev2 | `feature/E2-routine-notices` | Not done |
| E3 | Assignments, Forms, Comments & Uploads | Dev2 | `feature/E3-assignments` | Not done |
| E4 | Group Chat & Notifications | Dev3 | `feature/E4-chat` | Not done |
| E5 | AI (RAG) Assistant | Dev3 | `feature/E5-rag-assistant` | Not done |
| E6 | Docker, CI/CD & Render Deployment | Dev1 | `feature/E6-docker-cicd` | Not done |
| E7 | Bonus: Monitoring & Second Deployment | Dev3 | `feature/E7-observability` | Not done |
| E8 | Documentation & Demo | Dev2 | `feature/E8-docs` | Not done |

## 5. Test count tracker
| | Target | Current |
|---|---|---|
| Unit tests (service layer) | ≥ 20 (aim 25) | 22 |
| Integration tests (controller, MockMvc) | ≥ 10 (aim 12) | 13 |

## 6. Notes / decisions log
- Roles are `ADMIN`, `CR`, `TEACHER`, `STUDENT`, the guideline's ADMIN/SELLER/BUYER adapted to the class domain.
- Dev1 reviews all PRs. Dev2 reviews Dev1's own PRs, because GitHub blocks self-approval.
- Tests run on H2 (`test` profile). Dev and prod run on PostgreSQL.
- E1: tests need `-Dnet.bytebuddy.experimental=true` (set in surefire `argLine`) only because some devs run JDK 25 locally; CI uses JDK 17.
- E1: self-registration allows only STUDENT or CR; ADMIN promotes users to TEACHER via `PUT /api/v1/admin/users/{id}/roles`.
- E1: logout uses an in-memory JWT blacklist (per instance, cleared on restart), an accepted trade-off for a single Render instance.
- E1: shared test helpers `@IntegrationTest` and `TestAuthHelper` (`src/test/java/edu/kuet/classsync/support/`) are to be reused by every later epic.
