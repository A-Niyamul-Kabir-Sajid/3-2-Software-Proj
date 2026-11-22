# E1 — Security, Users & Platform Core

| Field | Value |
|---|---|
| Status | **In progress** |
| Owner | Dev1 |
| Reviewer | Dev2 |
| Branch | `feature/E1-security-core` |
| Depends on | E0 |
| Blocks | E2, E3, E4, E5 (they need `User`, security, `ApiError`, test helpers) |
| Rubric targets | Security & Roles (15), Exception handling (5), Database (`users`, `roles`, `user_roles` M:M), Testing (6 unit + 3 IT) |

## Goal
Provide the foundation every other epic builds on: PostgreSQL configuration via env vars, the User/Role model, JWT authentication with BCrypt, role-based access (URL plus method level), the global exception handling, an admin user-management controller, test helpers, and the frontend login.

## Dependencies to add to `backend/pom.xml`
```xml
<dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
<dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.6</version></dependency>
<dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
<dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
<dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.6.0</version></dependency>
```
- Change H2 to `<scope>test</scope>`, since it is only used by tests.
- Configure `maven-surefire-plugin` with includes `**/*Test.java` and `**/*IT.java`.

## Stories

### E1-S1 Configuration and profiles
Files in `backend/src/main/resources/`:
- `application.properties` (shared):
  ```properties
  spring.application.name=classsync
  server.port=${PORT:8080}
  spring.profiles.default=dev
  spring.jpa.hibernate.ddl-auto=update
  spring.jpa.open-in-view=false
  app.cors.allowed-origin=${CORS_ALLOWED_ORIGIN:http://localhost:3000}
  app.jwt.secret=${JWT_SECRET}
  app.jwt.expiration-minutes=${JWT_EXPIRATION_MINUTES:120}
  app.admin.email=${ADMIN_EMAIL:admin@classsync.local}
  app.admin.password=${ADMIN_PASSWORD}
  ```
- `application-dev.properties` and `application-prod.properties`: `spring.datasource.url=${DB_URL}`, `spring.datasource.username=${DB_USERNAME}`, `spring.datasource.password=${DB_PASSWORD}`. Prod adds `spring.jpa.show-sql=false`.
- `src/test/resources/application-test.properties`: H2 in-memory (`jdbc:h2:mem:testdb;MODE=PostgreSQL`), `ddl-auto=create-drop`, a fixed test `app.jwt.secret` (≥ 32 bytes), and `app.admin.password=admin12345`.
- Add `backend/.env.example` listing every variable above with dummy values. The real `.env` is already git-ignored.

**Acceptance:** there is no credential literal in `src/main`, and the app starts against a local PostgreSQL when the env vars are set.

### E1-S2 User and Role model
- `common.entity.BaseEntity` (`@MappedSuperclass`): `id`, `createdAt`, `updatedAt`.
- `user.entity.RoleName` enum: `ADMIN, CR, TEACHER, STUDENT`.
- `user.entity.Role`: `id`, `name` (unique, `EnumType.STRING`). Table `roles`.
- `user.entity.User`: `name` (not blank, ≤ 100), `email` (unique, lower-cased), `passwordHash`, `enabled` (default true), `roles` as `@ManyToMany(fetch = EAGER)` with `@JoinTable(name = "user_roles", joinColumns = user_id, inverseJoinColumns = role_id)`. Table `users`.
  - **Builder pattern**: a static nested `User.Builder` (hand-written, not Lombok), so the pattern is visible: `User.builder().name(..).email(..).passwordHash(..).role(role).build()`.
- `UserRepository`: `findByEmail`, `existsByEmail`. `RoleRepository`: `findByName`.
- `common.config.DataSeeder` (`ApplicationRunner`): inserts the 4 roles if they are missing, plus one ADMIN user from `app.admin.*` if none exists.

### E1-S3 Auth endpoints
Package `auth`:
| Method | Path | Access | Request | Response |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | public | `RegisterRequest{name, email, password, role}` | 201 `UserResponse` |
| POST | `/api/v1/auth/login` | public | `LoginRequest{email, password}` | 200 `AuthResponse{token, tokenType:"Bearer", expiresIn, user}` |
| POST | `/api/v1/auth/logout` | auth | none | 204 |
| GET | `/api/v1/auth/me` | auth | none | 200 `UserResponse` |

Rules:
- Self-registration may choose only `STUDENT` or `CR`. `TEACHER` and `ADMIN` are assigned by an ADMIN via E1-S6. Choosing any other role returns 400.
- A duplicate email returns 409 (`ConflictException`).
- Passwords are hashed with a `BCryptPasswordEncoder` bean (strength 10).
- **Strategy pattern**: `auth.policy.PasswordPolicy` interface, implemented by `DefaultPasswordPolicy` (≥ 8 chars, at least one letter and one digit). It is injected into `AuthService`; a violation returns 400.
- Logout: since JWT is stateless, `TokenBlacklistService` keeps the token's `jti` in an in-memory `ConcurrentHashMap` until it expires, and the JWT filter rejects blacklisted tokens. Document this trade-off in a comment.
- `JwtService`: `generate(User)` (subject = email, claims `roles` and `uid`, `jti` = UUID), `parse(token)` and `isValid`. HS256 signing with `app.jwt.secret`.
- `UserResponse{id, name, email, roles}`. It **never** includes the password hash.

### E1-S4 Security configuration
`auth.config.SecurityConfig`:
- `@EnableWebSecurity` and `@EnableMethodSecurity`.
- `csrf` disabled (stateless API), `sessionManagement` STATELESS, CORS enabled using `app.cors.allowed-origin`. Move the CORS config out of `WebConfig` into a `CorsConfigurationSource` bean and delete the old `addCorsMappings`.
- URL rules:
  - `permitAll`: `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/health`, `/actuator/health/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/ws/**` (WebSocket auth is handled in E4)
  - `hasRole('ADMIN')`: `/api/v1/admin/**`
  - `authenticated`: everything else
- `JwtAuthenticationFilter extends OncePerRequestFilter`, added before `UsernamePasswordAuthenticationFilter`.
- `RestAuthenticationEntryPoint` (401) and `RestAccessDeniedHandler` (403) write `ApiError` JSON.
- `CurrentUserProvider` (bean): `User get()` loads the logged-in user from the `SecurityContext`, for ownership checks in other modules.

### E1-S5 Global exception handling
Package `common.exception`: `ApiError` (immutable, with a **Builder**: `ApiError.builder().status(404).message(..).path(..).build()`), `FieldErrorItem`, `ResourceNotFoundException`, `ConflictException`, `BadRequestException`, `ForbiddenOperationException` and `GlobalExceptionHandler` (`@RestControllerAdvice`). The mapping table is in CONVENTIONS §3.

**Acceptance:** an unknown id returns 404 JSON, an invalid body returns 400 with `fieldErrors`, and nothing ever returns a stack trace.

### E1-S6 Admin user management
`user.controller.AdminUserController` at `/api/v1/admin/users`, with `@PreAuthorize("hasRole('ADMIN')")` at class level:
| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/api/v1/admin/users?page&size` | none | 200 page of `UserResponse` |
| GET | `/api/v1/admin/users/{id}` | none | 200 / 404 |
| PUT | `/api/v1/admin/users/{id}/roles` | `{roles:["TEACHER"]}` | 200 |
| PATCH | `/api/v1/admin/users/{id}/status` | `{enabled:false}` | 200 |
| DELETE | `/api/v1/admin/users/{id}` | none | 204; deleting yourself returns 400 |

### E1-S7 Frontend auth
- New `frontend/js/config.js` (`API_BASE`) and `frontend/js/api.js`. `api.js` is an **Adapter + Proxy** over `fetch`: it adds the base URL and JWT, parses `ApiError`, clears the token on 401 and shows login, and while offline stores `POST/PUT/PATCH/DELETE` in an IndexedDB/localStorage queue, then replays it on the `online` event.
- New login and register views in `frontend/js/app.js` (same styling as `theme.css`). Store the token and user in `localStorage`.
- Replace the role switcher in the mock UI with the real `user.roles`. Hide CR/Teacher controls for students.
- Add `config.js` and `api.js` to the `sw.js` shell cache list.

### E1-S8 Tests (quota: 6 unit + 3 integration)
Test helper `src/test/java/edu/kuet/classsync/support/TestAuthHelper.java`: a `@Component` that creates a user with a role and returns a valid JWT. Other epics' ITs reuse it.

Unit tests (`AuthServiceTest`, `JwtServiceTest`, `AdminUserServiceTest`, `DefaultPasswordPolicyTest`):
1. `register_withValidData_savesUserWithHashedPassword`
2. `register_withDuplicateEmail_throwsConflict`
3. `register_withTeacherRole_throwsBadRequest`
4. `login_withWrongPassword_throwsBadCredentials`
5. `jwt_generateThenParse_returnsSameEmailAndRoles`
6. `passwordPolicy_tooShort_isRejected`
7. `changeRoles_unknownUser_throwsNotFound`

Integration tests (`AuthControllerIT`, `AdminUserControllerIT`):
1. `register_returns201_andLoginReturnsToken`
2. `me_withoutToken_returns401`
3. `adminUsers_asStudent_returns403`
4. `adminUsers_asAdmin_returns200`

Update `ClassSyncApplicationTests` to `@ActiveProfiles("test")`, and remove `addFilters = false` because the health endpoint is public.

## Definition of Done
- [ ] All 8 stories done; `mvn clean verify` green with ≥ 6 unit + 3 IT
- [ ] The app runs locally against PostgreSQL with env vars only
- [ ] A curl flow works: register → login → `/auth/me` with token → admin endpoint as student gives 403
- [ ] `docs/agent.md` E1 status updated; the test tracker counts updated
