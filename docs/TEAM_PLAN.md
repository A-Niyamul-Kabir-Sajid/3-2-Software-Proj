# ClassSync — Team Plan (Epics & Stories)

A Progressive Web App for a class. The **CR** uploads the routine, notices, assignments and forms. A **Teacher** posts assignments and documents tagged by subject, with comments. **Students** view everything, chat in a group and ask an AI (RAG) assistant. An **Admin** manages users and roles.

This plan must satisfy [PROJECT_GUIDELINE.md](PROJECT_GUIDELINE.md) (CSE 3220). Live epic status is tracked in [`agent.md`](agent.md). The Git and PR process is in [GIT_WORKFLOW.md](GIT_WORKFLOW.md).

---

## 1. Team & roles

| Dev | Epics owned | PR reviewer |
|---|---|---|
| **Dev1** | E0, E1, E6 | Dev1 reviews and approves **all** PRs. Dev1's own PRs are approved by **Dev2**, because GitHub does not let an author approve their own PR. |
| **Dev2** | E2, E3, E8 | |
| **Dev3** | E4, E5, E7 | |

> Fill in the GitHub usernames: Dev1 = `________`, Dev2 = `________`, Dev3 = `________`

## 2. Architecture (summary)
- **Backend**: Java 17, Spring Boot 3, package-by-module under `edu.kuet.classsync.*`. It exposes a REST API under `/api/v1/**` plus WebSocket (STOMP) at `/ws`.
- **Database**: PostgreSQL (dev/prod, via Docker) with JPA/Hibernate. Tests use H2 (profile `test`).
- **Security**: Spring Security, BCrypt, JWT (stateless) and method-level security (`@PreAuthorize`).
- **Application roles**: `ADMIN`, `CR`, `TEACHER`, `STUDENT`. These are the guideline's ADMIN/SELLER/BUYER adapted to the class domain: CR and TEACHER act as content producers, STUDENT as consumer.
- **Frontend**: a static PWA in `frontend/` that talks to the backend only through `js/api.js`.
- **Delivery**: Docker and docker-compose (app + PostgreSQL), GitHub Actions CI/CD, and auto-deploy to Render from `main`.

## 3. Database (target ≥ 6 tables, all relationship types)
| Table | Relationships |
|---|---|
| `users` | M:M `roles` via `user_roles`; 1:M notices, assignments, comments, chat_messages |
| `roles` | M:M `users` |
| `user_roles` | join table (M:M) |
| `subjects` | 1:M assignments, routine_slots; M:M teachers via `teacher_subjects` |
| `routine_slots` | M:1 subject, M:1 teacher (user) |
| `notices` | M:1 author (user) |
| `assignments` | M:1 subject, M:1 author; 1:M comments, submissions |
| `submissions` | M:1 assignment, M:1 student (per-student complete/pending) |
| `comments` | M:1 assignment, M:1 author; visibility PUBLIC / TEACHER_ONLY |
| `forms`, `form_responses` | 1:M responses, M:1 user |
| `chat_messages` | M:1 sender (user) |
| `attachments` | uploaded files; 1:1 from assignment, M:1 uploader |
| `assistant_actions` | M:1 created_by (user); RAG pending/applied commands |

## 4. Design-pattern map (use only where it genuinely fits)
| Pattern | Where | Epic |
|---|---|---|
| Builder | `User`, `ApiError`, `Assignment` builders | E1, E3 |
| Strategy | Password policy; comment visibility; sort/filter by subject/date | E1, E3 |
| Proxy | `CachingRoutineServiceProxy`; frontend offline-aware `api.js` | E2, E1 |
| Composite | Routine = Week → Day → Slot | E2 |
| Memento | Routine editor undo | E2 |
| Factory Method | `PostFactory` (Notice / Assignment / Form) | E3 |
| Chain of Responsibility | Upload validation (size → type → role) | E3 |
| Template Method | `AbstractUploadHandler` (validate → store → persist) | E3 |
| State | Assignment status Draft → Published → Closed | E3 |
| Abstract Factory | `StorageFactory` (local / cloud storage) | E3 |
| Mediator | `ChatRoom` between sessions | E4 |
| Observer | Notify on new notice/assignment | E4 |
| Adapter | `LlmClientAdapter` (any LLM → one interface) | E5 |
| Facade | `RagFacade.ask()` | E5 |
| Command | RAG actions with `undo()` after confirmation | E5 |

## 5. Testing quota (guideline: ≥ 20 unit, ≥ 10 integration; target 25 / 12)
| Epic | Unit (Service, JUnit + Mockito) | Integration (Controller, SpringBootTest + MockMvc) |
|---|---|---|
| E1 | 6 | 3 |
| E2 | 5 | 2 |
| E3 | 6 | 3 |
| E4 | 3 | 2 |
| E5 | 5 | 2 |
| **Total** | **25** | **12** |

---

## 6. Epics & stories

This section is the summary. **The detailed, implementable spec for each epic is in [`docs/epics/`](epics/)**, and shared rules are in [epics/CONVENTIONS.md](epics/CONVENTIONS.md). Each story is one or more commits on the epic's branch. Branch names follow `feature/E<n>-<slug>`. Tick a story `[x]` when it is merged.

### E0 — Project Setup & Git Workflow · **Dev1** · `feature/E0-project-setup` · Status: **Done**

> **Full spec:** [epics/E0-project-setup.md](epics/E0-project-setup.md)
- [x] E0-S1 Save the project guideline, Jira-style team plan and detailed epic specs in `docs/`
- [x] E0-S2 Add `docs/agent.md` (agent instructions and epic status board)
- [x] E0-S3 Add `docs/GIT_WORKFLOW.md` (branching, PR, review guide)
- [x] E0-S4 Add a PR template (`.github/pull_request_template.md`)
- [x] E0-S5 Add a CI workflow `.github/workflows/ci.yml` (build + test on PRs)
- [x] E0-S6 GitHub settings: collaborators, `develop` branch, branch protection on `main` and `develop` (manual, see GIT_WORKFLOW §1)

**Done when**: `develop` exists, `main` and `develop` are protected, and CI runs on this PR.

### E1 — Security, Users & Platform Core · **Dev1** · `feature/E1-security-core` · Status: **Not done**

> **Full spec:** [epics/E1-security-core.md](epics/E1-security-core.md)
- [ ] E1-S1 PostgreSQL config with profiles (`dev`, `test` = H2, `prod`) and all credentials taken from env vars
- [ ] E1-S2 Entities `User`, `Role` (M:M `user_roles`) and repositories; seed roles at startup
- [ ] E1-S3 Register / Login / Logout / Me (`/api/v1/auth/**`): BCrypt, JWT, `Builder` for User, `Strategy` password policy
- [ ] E1-S4 `SecurityConfig`: stateless JWT filter, URL rules, `@EnableMethodSecurity`, 401/403 JSON responses
- [ ] E1-S5 `GlobalExceptionHandler` + `ApiError` (Builder) + custom exceptions (`NotFoundException`, `ConflictException`, …)
- [ ] E1-S6 `UserController` (ADMIN): list, get, change role, delete user
- [ ] E1-S7 Frontend: login/register screens, JWT stored and attached in `js/api.js`, role-aware navigation
- [ ] E1-S8 Tests: 6 unit + 3 integration

### E2 — Routine & Notices · **Dev2** · `feature/E2-routine-notices` · Status: **Not done**

> **Full spec:** [epics/E2-routine-notices.md](epics/E2-routine-notices.md)
- [ ] E2-S1 Entities `Subject`, `RoutineSlot` (M:1 subject, M:1 teacher), `Notice` (M:1 author)
- [ ] E2-S2 `SubjectController` CRUD (CR/ADMIN write, all read)
- [ ] E2-S3 `RoutineController`: `GET /routine` (Composite Week → Day → Slot view), CRUD slots (CR only), `POST /routine/undo` (Memento)
- [ ] E2-S4 `CachingRoutineServiceProxy` (Proxy) for the latest routine
- [ ] E2-S5 `NoticeController` CRUD (CR/TEACHER write, all read), pagination, proper status codes
- [ ] E2-S6 Frontend Routine and Notice pages wired to the API (replace mock data)
- [ ] E2-S7 Tests: 5 unit + 2 integration

### E3 — Assignments, Forms, Comments & Uploads · **Dev2** · `feature/E3-assignments` · Status: **Not done**

> **Full spec:** [epics/E3-assignments.md](epics/E3-assignments.md)
- [ ] E3-S1 Entities `Assignment` (State: Draft/Published/Closed), `Submission`, `Comment`, `Form`, `FormResponse`
- [ ] E3-S2 `AssignmentController` CRUD (TEACHER/CR), filter/sort by subject/date (Strategy); `PostFactory` reused
- [ ] E3-S3 Per-student completion: `POST /assignments/{id}/complete`, `GET /assignments?status=pending|completed`
- [ ] E3-S4 `CommentController`: `POST /assignments/{id}/comments {text, visibility}`; visibility Strategy hides TEACHER_ONLY comments from students
- [ ] E3-S5 File upload: `AbstractUploadHandler` (Template Method) + validator chain (Chain of Responsibility) + `StorageFactory` (Abstract Factory)
- [ ] E3-S6 `FormController` CRUD + submit a response
- [ ] E3-S7 Frontend Assignments / Forms pages wired to the API
- [ ] E3-S8 Tests: 6 unit + 3 integration

### E4 — Group Chat & Notifications · **Dev3** · `feature/E4-chat` · Status: **Not done**

> **Full spec:** [epics/E4-chat.md](epics/E4-chat.md)
- [ ] E4-S1 Entity `ChatMessage` (M:1 sender) and repository
- [ ] E4-S2 WebSocket/STOMP `/ws`, topic `/topic/class`, JWT auth on CONNECT, `ChatRoom` Mediator
- [ ] E4-S3 `ChatController` REST: `GET /chat/messages` (paginated history), `DELETE /chat/messages/{id}` (ADMIN/author)
- [ ] E4-S4 Observer: publish an event when a notice or assignment is created, so the frontend gets a toast
- [ ] E4-S5 Frontend chat page wired (live messages, history)
- [ ] E4-S6 Tests: 3 unit + 2 integration

### E5 — AI (RAG) Assistant · **Dev3** · `feature/E5-rag-assistant` · Status: **Not done**

> **Full spec:** [epics/E5-rag-assistant.md](epics/E5-rag-assistant.md)
- [ ] E5-S1 `LlmClientAdapter` interface + one provider implementation (API key from env) + a fake for tests
- [ ] E5-S2 Retrieval: index notices / routine / assignments (in-memory cosine similarity, or keyword fallback)
- [ ] E5-S3 `RagFacade.ask(prompt)` and `POST /api/v1/assistant/ask` (answer + sources)
- [ ] E5-S4 Action flow: prompt → `Command` (e.g. `UpdateRoutineSlotCommand`) → pending → `POST /assistant/confirm/{id}` executes (CR/TEACHER only); unclear prompts return clarifying questions
- [ ] E5-S5 Frontend assistant page with a Confirm/Cancel card
- [ ] E5-S6 Tests: 5 unit + 2 integration

### E6 — Docker, CI/CD & Render Deployment · **Dev1** · `feature/E6-docker-cicd` · Status: **Not done**

> **Full spec:** [epics/E6-docker-cicd.md](epics/E6-docker-cicd.md)
- [ ] E6-S1 Multi-stage `backend/Dockerfile` (Maven build → JRE runtime, non-root user)
- [ ] E6-S2 `docker-compose.yml` (app + postgres, healthcheck, volume) + `.env.example`; `docker compose up --build` works
- [ ] E6-S3 Extend CI: build, test, upload test report, build the Docker image
- [ ] E6-S4 Render: PostgreSQL + Web Service (backend) + Static Site (frontend); env vars set in the Render dashboard
- [ ] E6-S5 CD: on push to `main`, after tests pass, trigger the Render deploy hook (`RENDER_DEPLOY_HOOK_URL` secret)
- [ ] E6-S6 Verify the live URL and record it in the README

### E7 — Bonus: Monitoring & Second Deployment · **Dev3** · `feature/E7-observability` · Status: **Not done**

> **Full spec:** [epics/E7-observability.md](epics/E7-observability.md)
- [ ] E7-S1 Spring Boot Actuator (`health`, `info`, `prometheus`) with secured endpoints
- [ ] E7-S2 Micrometer Prometheus registry + `monitoring/prometheus.yml` + a Prometheus service in compose
- [ ] E7-S3 Grafana service in compose + provisioned datasource + dashboard JSON (requests, latency, JVM)
- [ ] E7-S4 Second deployment (Railway / Fly.io / Koyeb …) + its config file
- [ ] E7-S5 Document monitoring and the second deployment in the README

### E8 — Documentation & Demo · **Dev2** · `feature/E8-docs` · Status: **Not done**

> **Full spec:** [epics/E8-docs.md](epics/E8-docs.md)
- [ ] E8-S1 README: description, features, tech stack, roles
- [ ] E8-S2 Architecture diagram (Mermaid) + ER diagram (Mermaid)
- [ ] E8-S3 API endpoint table (method, path, role, status codes)
- [ ] E8-S4 Run instructions (local, Docker) + CI/CD explanation + live URL + repo link
- [ ] E8-S5 Demo script for 15 minutes (who shows what) + seed demo data/users

---

## 7. Suggested order
1. **E0** (blocks everything) →
2. **E1** (security and core, which blocks every API epic) →
3. **E2, E3, E4** in parallel (E6-S1/S2 Docker can start in parallel) →
4. **E5, E6** →
5. **E7, E8** last (E8 is updated continuously).

## 8. Mandatory rules (every person and every AI assistant)
1. **No AI co-author.** Never add a `Co-Authored-By:` AI trailer or a "Generated with" line to any commit or PR.
2. **Never push to `main` directly.** Work goes into a feature branch, then a PR into `develop`. Releases go through a PR from `develop` into `main`.
3. **Keep this plan current.** When a story is done, tick it here, and update the epic status in [`agent.md`](agent.md) in the same PR.
4. No secrets in git. Use env vars and `.env` (git-ignored); `.env.example` is committed.
