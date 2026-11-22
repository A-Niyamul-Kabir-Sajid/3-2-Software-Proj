# Shared Engineering Conventions (all epics)

Every epic spec assumes these conventions. Read this before implementing any epic.

## 1. Backend package layout
Root package: `edu.kuet.classsync`. One package per module, with sub-packages per layer:
```
edu.kuet.classsync.<module>/
  controller/   <Name>Controller.java          REST only: maps DTO <-> service; no business logic
  service/      <Name>Service.java              business logic, @Transactional, role/ownership rules
  repository/   <Name>Repository.java           extends JpaRepository<Entity, Long>
  entity/       <Name>.java                     JPA entities
  dto/          <Name>Request.java, <Name>Response.java   Java records + jakarta.validation annotations
  mapper/       <Name>Mapper.java               static/Component entity <-> DTO mapping (no MapStruct needed)
  pattern/      pattern classes (e.g. RoutineMemento) when they are module-specific
```
Modules: `auth`, `user`, `common`, `subject`, `routine`, `notice`, `assignment`, `form`, `comment`, `upload`, `chat`, `rag`.

## 2. REST conventions
- Base path `/api/v1`. Use plural nouns: `/api/v1/notices`, `/api/v1/notices/{id}`.
- `GET` list → 200 with a page object (`Page<T>` serialised as `{content, totalElements, totalPages, number, size}`). Query params `page` (0-based), `size` (default 20), `sort`.
- `GET` by id → 200, or 404.
- `POST` create → **201** + `Location` header + body.
- `PUT` full update → 200. `PATCH` partial update → 200.
- `DELETE` → **204** (no body).
- Validation failure → 400. Not logged in → 401. Wrong role or not owner → 403. Missing entity → 404. Duplicate (e.g. email) → 409.
- Request/response bodies are DTO records. **Never expose JPA entities directly** (this avoids lazy-loading and password leaks).

## 3. Error format (built in E1, used by everyone)
Every error response body is an `ApiError`:
```json
{
  "timestamp": "2026-11-22T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Notice 42 not found",
  "path": "/api/v1/notices/42",
  "fieldErrors": [ { "field": "title", "message": "must not be blank" } ]
}
```
Throw the shared exceptions from `edu.kuet.classsync.common.exception`:
| Exception | HTTP |
|---|---|
| `ResourceNotFoundException(String entity, Object id)` | 404 |
| `ConflictException(String msg)` | 409 |
| `BadRequestException(String msg)` | 400 |
| `ForbiddenOperationException(String msg)` | 403 |
`MethodArgumentNotValidException` → 400 with `fieldErrors`. `AccessDeniedException` → 403. `AuthenticationException` → 401. Any other exception → 500 with the generic message "Unexpected error" (the cause is logged, never returned).

## 4. Security conventions (built in E1)
- Roles are enum `RoleName { ADMIN, CR, TEACHER, STUDENT }`. Authorities are `ROLE_ADMIN` and so on.
- Protect every write endpoint with `@PreAuthorize("hasAnyRole('CR','TEACHER')")` (or similar) on the **controller method**.
- Ownership checks ("only the author can edit") go in the **service**, using `CurrentUser` (`edu.kuet.classsync.auth.CurrentUserProvider#get()` returns the logged-in `User`). Throw `ForbiddenOperationException` when the check fails.
- Public endpoints: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/health`, `/actuator/health`. Everything else needs a JWT.
- Header: `Authorization: Bearer <jwt>`.

## 5. Entity conventions
- Every entity has an `id` (`@GeneratedValue(strategy = IDENTITY)`), plus `createdAt` and `updatedAt`, set via `@CreationTimestamp` / `@UpdateTimestamp` or a shared `@MappedSuperclass BaseEntity` in `common.entity` (created in E1).
- Table names are explicit, snake_case and plural: `@Table(name = "notices")`.
- Relations: `@ManyToOne(fetch = LAZY)` with an explicit `@JoinColumn(name = "author_id", nullable = false)`. Avoid bidirectional mappings unless the epic needs them.
- Enums are stored with `@Enumerated(EnumType.STRING)`.
- Schema: `spring.jpa.hibernate.ddl-auto=update` for dev. Prod uses `update` too, which keeps things simple for this course.

## 6. Testing conventions
- **Unit tests** (service layer) live in `src/test/java/.../<module>/service/<Name>ServiceTest.java` and use `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories and `@InjectMocks` the service. No Spring context.
- **Integration tests** (controller layer) live in `src/test/java/.../<module>/controller/<Name>ControllerIT.java` and use `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")`, with real filters. Authenticate with `@WithMockUser(roles = "CR")` or a real JWT obtained from the `TestAuthHelper` that E1 creates.
- Test names follow the pattern `methodName_condition_expectedResult`, e.g. `create_whenTitleBlank_returns400`.
- Every secured endpoint has at least one test proving the **forbidden** case (403) for a wrong role.
- Surefire must pick up both `*Test` and `*IT`: E1 configures `maven-surefire-plugin` includes so `mvn verify` runs both.
- Tests run on H2 (`application-test.properties`), so they never need a real PostgreSQL.

## 7. Frontend conventions
- `frontend/js/api.js` (created in E1) is the **only** place that calls `fetch`. It exposes `Api.get(path)`, `Api.post(path, body)`, `Api.put`, `Api.patch`, `Api.del`, `Api.upload(path, formData)`, attaches the JWT, handles 401 by redirecting to login, and queues failed writes while offline.
- `API_BASE` comes from `frontend/js/config.js` (`window.APP_CONFIG = { API_BASE: "http://localhost:8080/api/v1" }`), which is changed for Render.
- Mock data in `frontend/js/data.js` is replaced module by module. Delete each mock section once its page uses the API.
- Escape all user text (the existing `esc()` helper) before inserting it into the HTML.

## 8. Definition of Done (every epic)
1. All stories are implemented and the acceptance criteria are met.
2. The epic's test quota is written and `cd backend && mvn clean verify` passes.
3. There are no hardcoded secrets and no unused code or TODO stubs left behind.
4. New endpoints are added to `docs/API.md`.
5. `docs/agent.md` status is updated and the stories are ticked in `docs/TEAM_PLAN.md`.
6. A PR into `develop` is opened per `docs/GIT_WORKFLOW.md` and approved by the reviewer.
