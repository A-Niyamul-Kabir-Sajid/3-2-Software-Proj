# ClassSync REST API Reference

Base URL: `/api/v1`. Send `Authorization: Bearer <jwt>` except on public endpoints. Errors use the `ApiError` JSON format (see [epics/CONVENTIONS.md](epics/CONVENTIONS.md) §3). Interactive docs: `/swagger-ui.html`.

Each epic appends its endpoints here in the same PR that adds them.

## Common
| Method | Path | Roles | Request | Success | Errors |
|---|---|---|---|---|---|
| GET | /api/v1/health | public | none | 200 `{status:"UP"}` | none |

## Auth (E1)
| Method | Path | Roles | Request | Success | Errors |
|---|---|---|---|---|---|
| POST | /api/v1/auth/register | public | `{name, email, password, role?}`; role is `STUDENT` (default) or `CR` | 201 `UserResponse` + `Location` | 400 validation / weak password / role not allowed, 409 email taken |
| POST | /api/v1/auth/login | public | `{email, password}` | 200 `{token, tokenType:"Bearer", expiresIn, user}` | 400, 401 invalid credentials, 403 disabled account |
| POST | /api/v1/auth/logout | authenticated | none (uses the Bearer header) | 204; the token is revoked | 401 |
| GET | /api/v1/auth/me | authenticated | none | 200 `UserResponse` | 401 |

`UserResponse` = `{id, name, email, roles:[…], enabled, createdAt}`. It never contains the password.

## Admin users (E1)
| Method | Path | Roles | Request | Success | Errors |
|---|---|---|---|---|---|
| GET | /api/v1/admin/users?page&size&sort | ADMIN | none | 200 `PageResponse<UserResponse>` | 401, 403 |
| GET | /api/v1/admin/users/{id} | ADMIN | none | 200 `UserResponse` | 401, 403, 404 |
| PUT | /api/v1/admin/users/{id}/roles | ADMIN | `{roles:["TEACHER"]}` | 200 `UserResponse` | 400 (empty, or removing your own ADMIN), 403, 404 |
| PATCH | /api/v1/admin/users/{id}/status | ADMIN | `{enabled:false}` | 200 `UserResponse` | 400 (disabling yourself), 403, 404 |
| DELETE | /api/v1/admin/users/{id} | ADMIN | none | 204 | 400 (deleting yourself), 403, 404 |

`PageResponse<T>` = `{content:[…], number, size, totalElements, totalPages}`.

### curl examples
```bash
API=http://localhost:8080/api/v1
curl -X POST $API/auth/register -H 'Content-Type: application/json' \
     -d '{"name":"Rafi","email":"rafi@kuet.ac.bd","password":"secret123","role":"CR"}'
TOKEN=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"rafi@kuet.ac.bd","password":"secret123"}' | jq -r .token)
curl $API/auth/me -H "Authorization: Bearer $TOKEN"
```

<!-- E2: Subjects, Routine, Notices -->

## Subjects (E2)
| Method | Path | Roles | Request | Success | Errors |
|---|---|---|---|---|---|
| GET | /api/v1/subjects | authenticated | none | 200 `List<SubjectResponse>` (sorted by code) | 401 |
| GET | /api/v1/subjects/{id} | authenticated | none | 200 `SubjectResponse` | 401, 404 |
| POST | /api/v1/subjects | CR, ADMIN | `{code, title, teacherIds?}` | 201 `SubjectResponse` + `Location` | 400 (validation / non-TEACHER), 401, 403, 404 (missing user), 409 (duplicate code) |
| PUT | /api/v1/subjects/{id} | CR, ADMIN | `{code, title, teacherIds?}` | 200 `SubjectResponse` | 400, 401, 403, 404, 409 |
| DELETE | /api/v1/subjects/{id} | CR, ADMIN | none | 204 | 401, 403, 404, 409 (in use by routine slots) |

`SubjectResponse` = `{id, code, title, teachers:[{id, name}]}`.

## Routine (E2)
| Method | Path | Roles | Request | Success | Errors |
|---|---|---|---|---|---|
| GET | /api/v1/routine | authenticated | none | 200 `RoutineResponse` (`{days:[{day, slots:[…]}], totalSlots}`) | 401 |
| GET | /api/v1/routine/slots/{id} | authenticated | none | 200 `RoutineSlotResponse` | 401, 404 |
| POST | /api/v1/routine/slots | CR | `{day, startTime, endTime, room, subjectId, teacherId?}` | 201 `RoutineSlotResponse` + `Location` | 400 (endTime <= startTime / non-TEACHER), 401, 403, 404, 409 (overlap) |
| PUT | /api/v1/routine/slots/{id} | CR | `{day, startTime, endTime, room, subjectId, teacherId?}` | 200 `RoutineSlotResponse` | 400, 401, 403, 404, 409 (overlap) |
| DELETE | /api/v1/routine/slots/{id} | CR | none | 204 | 401, 403, 404 |
| POST | /api/v1/routine/undo | CR | none | 200 `RoutineResponse` | 401, 403, 409 (`"Nothing to undo"`) |

## Notices (E2)
| Method | Path | Roles | Request | Success | Errors |
|---|---|---|---|---|---|
| GET | /api/v1/notices?page&size&q | authenticated | none (pinned first, then newest; `q` searches title/body) | 200 `PageResponse<NoticeResponse>` | 401 |
| GET | /api/v1/notices/{id} | authenticated | none | 200 `NoticeResponse` | 401, 404 |
| POST | /api/v1/notices | CR, TEACHER | `{title, body?, pinned}` | 201 `NoticeResponse` + `Location` | 400, 401, 403 |
| PUT | /api/v1/notices/{id} | CR, TEACHER, ADMIN | `{title, body?, pinned}` | 200 `NoticeResponse` | 400, 401, 403 (not author/ADMIN), 404 |
| DELETE | /api/v1/notices/{id} | CR, TEACHER, ADMIN | none | 204 | 401, 403 (not author/ADMIN), 404 |

`NoticeResponse` = `{id, title, body, pinned, author:{id, name}, createdAt, updatedAt}`.

<!-- E3: Assignments, Comments, Files, Forms · E4: Chat · E5: Assistant -->
