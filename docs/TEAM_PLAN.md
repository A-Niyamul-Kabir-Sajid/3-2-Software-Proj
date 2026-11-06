# ClassSync — Team Plan & Prompts (3 people)

A Progressive Web App for a class: **CR** uploads routine/notice/assignment/forms, **Teacher** posts assignments & documents with subject tags and comments, **Students** view everything, chat in a group, and ask an AI (RAG) assistant. Installable and usable offline.

## 1. Features (from the flow diagram)
| Area | Requirement |
|---|---|
| PWA | Installable, offline cache, background sync of queued actions |
| CR upload | Routine (latest + viewer), Notice, Assignment, Form |
| Routine | Student = viewer; CR = routine editor |
| Assignment/Form section | Shows what is complete / pending (per-student status) |
| Teacher | Add assignment + link/document, tagged by subject; backend acts as hosting server |
| CR | Can do the same, tag by subject |
| Comments | Attached to a teacher post: 1) Public comment 2) Teacher-only (private) comment |
| Group chat | Real-time class chat |
| RAG assistant | User gives one prompt; AI understands and updates data itself **after confirmation**; if unclear, asks clarifying questions to CR/Sir |

## 2. Architecture
- **Backend**: Java 17+, Spring Boot 3, package-by-module under `edu.kuet.classsync.*`. REST + WebSocket (STOMP). H2 for dev, PostgreSQL for prod. JWT auth, roles `CR`, `TEACHER`, `STUDENT`.
- **Frontend**: static PWA (`frontend/`), talks to backend only through `js/api.js` (Adapter + Proxy over `fetch`). No shared code with backend.
- **Contract**: REST JSON under `/api/v1/**`; keep `docs/API.md` updated by whoever changes an endpoint.

### Modules
`auth` · `routine` · `notice` · `assignment` · `form` · `chat` · `rag` · `common` (config, patterns)

## 3. Design-pattern map (use where it genuinely fits)
| Category | Pattern | Where |
|---|---|---|
| Creational | **Factory Method** | `PostFactory` creates Notice/Assignment/Form from upload type |
| Creational | **Builder** | `Assignment`, `RoutineSlot`, `ApiError` DTO builders |
| Creational | **Singleton** | Spring beans; frontend `Store`, `ApiClient` |
| Creational | **Abstract Factory** | `StorageFactory` → local / cloud file storage families |
| Structural | **Adapter** | `LlmClientAdapter` (OpenAI/Gemini/Ollama → one interface); frontend `api.js` |
| Structural | **Facade** | `RagFacade`, `UploadFacade` hide multi-step flows |
| Structural | **Proxy** | `CachingRoutineServiceProxy`, role-checking proxy, frontend offline-aware fetch proxy |
| Structural | **Decorator** | Notification channels (push + email), logging on services |
| Structural | **Composite** | Routine = Week → Day → Slot |
| Structural | **Bridge / Flyweight** | Bridge: `Notifier` × `Channel`; Flyweight: shared Subject tag objects |
| Behavioral | **Strategy** | Sorting/filtering by subject/date/priority; comment visibility rule |
| Behavioral | **Chain of Responsibility** | Upload validation (size → type → virus → role) |
| Behavioral | **Command** | RAG actions (`UpdateRoutineCommand`, …) with `undo()` for confirmation/rollback |
| Behavioral | **Memento** | Routine editor undo/redo; RAG pending-change snapshot |
| Behavioral | **State** | Assignment status (Draft → Published → Closed); RAG conversation state (Idle → Clarifying → AwaitingConfirm → Applied) |
| Behavioral | **Mediator** | Chat room mediates users; frontend event bus |
| Behavioral | **Template Method** | `AbstractUploadHandler.handle()` = validate → store → persist → notify |
| Behavioral | **Visitor** | Export/render routine tree to PDF / JSON / HTML |

## 4. Division of work

| Person | Ownership | Branch prefix |
|---|---|---|
| **P1 – Platform & Auth & PWA** | `auth`, `common`, frontend shell, PWA, offline sync, CI | `p1/` |
| **P2 – Academic content** | `routine`, `notice`, `assignment`, `form`, comments, uploads/hosting | `p2/` |
| **P3 – Realtime & AI** | `chat`, `rag`, dashboard widgets, notifications | `p3/` |

Git rules: branch from `main`, small commits (`feat:`, `fix:`, `chore:`), PR with one review, **commit after every working increment**, never commit `.env`/secrets.

**Mandatory rules (apply to every person and every AI assistant):**
1. **No AI co-author.** Never add `Co-Authored-By: Claude` (or any AI) trailer or "Generated with" line to any commit or PR description. Commits are authored by the human only.
2. **Keep this plan current.** After every successful piece of work or finished module, update this file (`docs/TEAM_PLAN.md`): tick the item in the Progress Tracker (section 7), note any API/pattern changes, then commit the plan update together with (or right after) the work.

---

## 5. Prompts (copy-paste to your AI assistant)

### Shared preamble (give to everyone)
```
Project: ClassSync, a PWA for CR/Teacher/Student. Monorepo: backend/ (Spring Boot 3, Java 17,
package-by-module under edu.kuet.classsync) and frontend/ (vanilla JS PWA, no build step).
Frontend and backend must stay completely separate; they communicate only via REST/WebSocket JSON
under /api/v1. Roles: CR, TEACHER, STUDENT. Apply GoF design patterns only where they truly fit
(see docs/TEAM_PLAN.md section 3) and name classes after the pattern (e.g. PostFactory).
Write unit tests for service layer. Commit after every working increment with conventional commits.
NEVER add a Co-Authored-By AI trailer to commits. After every successful task or finished module,
update docs/TEAM_PLAN.md (Progress Tracker, section 7) and commit it.
UI style: dark glassy theme with red→orange gradient, large display type (see frontend/css/theme.css).
```

### Prompt for P1 — Platform, Auth, PWA
```
You own: backend modules `auth`, `common`; frontend shell, service worker, offline sync.
Tasks:
1. Backend `auth`: User entity (id, name, email, passwordHash, role), JWT login/register,
   Spring Security config with role-based method security. Use Builder for User, Strategy for
   password policy, Proxy/Decorator for audit logging. Endpoints: POST /api/v1/auth/login,
   /register, GET /api/v1/auth/me.
2. `common`: global exception handler returning ApiError (Builder), CORS config for the frontend
   origin, OpenAPI docs, Singleton-style config beans, StorageFactory (Abstract Factory) for file storage.
3. Frontend: finish manifest.webmanifest, sw.js (cache-first for shell, network-first for API,
   background sync queue for POSTs made offline), install prompt button, online/offline badge.
   js/api.js = Adapter + Proxy: one place that attaches JWT, queues failed writes, and replays them.
4. Login/register pages wired to the API with role-aware routing.
Acceptance: app installs, shell loads offline, login works, queued actions sync after reconnect.
```

### Prompt for P2 — Academic content
```
You own: backend modules `routine`, `notice`, `assignment`, `form`; frontend pages
Routine, Notice board, Assignments, Forms.
Tasks:
1. Routine: Composite (Week→Day→Slot), Memento for editor undo/redo, Visitor for export
   (JSON/HTML), Proxy for caching latest routine. CR can edit; students view only.
   Endpoints: GET /routine/latest, PUT /routine, POST /routine/undo.
2. Upload pipeline: Template Method (AbstractUploadHandler) + Chain of Responsibility validators
   (size, mime type, role) + Factory Method (PostFactory) producing Notice/Assignment/Form.
   Backend hosts the files (StorageFactory from P1) and returns a download URL.
3. Assignment: subject Tag (Flyweight), State pattern (Draft/Published/Closed),
   per-student completion status ("tumi ki ki complete korcho"), Strategy for sort/filter by
   subject/date. Teacher and CR can both create and tag.
4. Comments on teacher posts: Strategy for visibility — PUBLIC (everyone) vs TEACHER_ONLY
   (author + teachers). Endpoint: POST /posts/{id}/comments {text, visibility}.
5. Frontend pages call only js/api.js; show completed/pending sections; subject tag filter chips.
Acceptance: CR uploads routine & notice, teacher posts tagged assignment with document,
student sees them and marks complete, private comments hidden from students.
```

### Prompt for P3 — Realtime & AI
```
You own: backend modules `chat`, `rag`; frontend Chat and AI Assistant pages, dashboard widgets.
Tasks:
1. Group chat: WebSocket/STOMP at /ws, topic /topic/class.{id}; Mediator (ChatRoom) between
   sessions; persist messages; load history via GET /api/v1/chat/messages.
2. RAG assistant: embed notices/routine/assignments into a vector store (start with in-memory
   cosine similarity, interface allows pgvector later). Facade `RagFacade.ask(prompt)`.
   LlmClientAdapter (Adapter) wraps the chosen LLM provider.
3. Agentic update flow: user sends ONE prompt ("move Monday DBMS to 10am"). Assistant parses
   intent into a Command (UpdateRoutineCommand, CreateNoticeCommand...). Conversation State:
   Idle → Clarifying → AwaitingConfirm → Applied. If confident, show a diff and ask
   "Confirm?" — only on yes execute the Command (support undo via Memento). If NOT confident,
   ask clarifying questions and route unresolved ones to CR/Sir via a control panel inbox.
   Only CR and TEACHER may confirm writes; students get read-only Q&A.
4. Frontend: chat bubbles, typing indicator, confirmation card with Confirm/Cancel, offline
   banner (chat disabled offline, history cached).
Acceptance: students chat in real time; assistant answers from class data with sources;
CR can change the routine through one prompt + confirmation, and unclear prompts trigger questions.
```

## 6. Milestones
1. **Week 1** — skeleton, auth, shell/PWA, static UI (done in repo), routine read.
2. **Week 2** — uploads, assignments, tags, comments, chat.
3. **Week 3** — RAG + confirmation flow, offline sync, polish, tests.
4. **Week 4** — demo, docs, bug-fix.

## 7. Progress Tracker (update after every successful task / module)
- [x] Repo + project structure
- [x] Team plan
- [x] Frontend UI (mock data)
- [x] Backend skeleton (Spring Boot, CORS, health, PostFactory)
- [x] Maven 3.9.9 installed (~/tools), `mvn test` passes (3 tests)
- [ ] P1: auth, JWT, offline sync
- [ ] P2: routine, notices, assignments, forms, comments, uploads
- [ ] P3: chat, RAG assistant
