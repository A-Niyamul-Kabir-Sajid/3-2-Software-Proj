# E2 — Routine & Notices

| Field | Value |
|---|---|
| Status | **In progress** |
| Owner | Dev2 |
| Reviewer | Dev1 |
| Branch | `feature/E2-routine-notices` |
| Depends on | E1 (User, security, `ApiError`, `CurrentUserProvider`, `TestAuthHelper`) |
| Blocks | E3 (uses `Subject`), E5 (RAG indexes routine and notices) |
| Rubric targets | REST CRUD (Subject, RoutineSlot, Notice), DB tables `subjects`, `teacher_subjects` (M:M), `routine_slots`, `notices`; patterns Composite, Memento, Proxy; 5 unit + 2 IT |

## Goal
CR manages subjects and the weekly class routine (with undo), and CR or teachers publish notices. Everyone logged in can read them. The frontend Routine and Notice pages use the real API instead of mock data.

## Stories

### E2-S1 Entities
Package `subject.entity`:
- `Subject`: `code` (unique, e.g. "CSE 3201", ≤ 20), `title` (≤ 120), `teachers` as `@ManyToMany` `Set<User>` via `@JoinTable(name = "teacher_subjects", joinColumns = subject_id, inverseJoinColumns = teacher_id)`. Table `subjects`.

Package `routine.entity`:
- `DayOfWeek`: use `java.time.DayOfWeek` (stored as a STRING).
- `RoutineSlot`: `day` (DayOfWeek), `startTime` (`LocalTime`), `endTime` (`LocalTime`), `room` (≤ 30), `subject` (`@ManyToOne` not null), `teacher` (`@ManyToOne User`, nullable). Table `routine_slots`. Validation: `endTime` must be after `startTime` (400), and two slots on the same day must not overlap (409).

Package `notice.entity`:
- `Notice`: `title` (not blank, ≤ 150), `body` (`@Lob`/TEXT, ≤ 5000), `pinned` (boolean), `author` (`@ManyToOne User`, not null). Table `notices`.

### E2-S2 Subject CRUD
`SubjectController` at `/api/v1/subjects`:
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/subjects` | any authenticated | list, sorted by code |
| GET | `/subjects/{id}` | any | 404 if missing |
| POST | `/subjects` | CR, ADMIN | `{code, title, teacherIds[]}` → 201; duplicate code → 409; a teacherId whose user is not TEACHER → 400 |
| PUT | `/subjects/{id}` | CR, ADMIN | 200 |
| DELETE | `/subjects/{id}` | CR, ADMIN | 204; return 409 if routine slots or assignments use it |

`SubjectResponse{id, code, title, teachers:[{id,name}]}`.

### E2-S3 Routine (Composite + Memento)
**Composite pattern** in `routine.pattern`:
```
RoutineComponent (interface)  -> String name(); List<RoutineComponent> children(); int totalSlots();
  WeekRoutine  (composite: 7 DayRoutine, MON..SUN; only days with slots are included in the response)
  DayRoutine   (composite: slots sorted by startTime)
  SlotLeaf     (leaf: wraps a RoutineSlot)
```
`RoutineAssembler.build(List<RoutineSlot>)` returns a `WeekRoutine`, which is mapped to:
```json
{ "days": [ { "day": "SUNDAY", "slots": [ {"id":1,"start":"08:00","end":"08:50","room":"301",
   "subject":{"id":1,"code":"CSE 3201","title":"Software Eng"},"teacher":{"id":5,"name":"Dr. X"}} ] } ],
  "totalSlots": 12 }
```

`RoutineController` at `/api/v1/routine`:
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/routine` | any | the full week (composite JSON above) |
| GET | `/routine/slots/{id}` | any | 404 if missing |
| POST | `/routine/slots` | CR | `{day, startTime, endTime, room, subjectId, teacherId?}` → 201 |
| PUT | `/routine/slots/{id}` | CR | 200 |
| DELETE | `/routine/slots/{id}` | CR | 204 |
| POST | `/routine/undo` | CR | undoes the last routine change; 200 with the routine, or 409 "Nothing to undo" |

**Memento pattern** in `routine.pattern`:
- `RoutineMemento`: an immutable snapshot (a list of slot DTO copies) plus a timestamp.
- `RoutineCaretaker` (`@Component`): a bounded `Deque<RoutineMemento>` (max 10).
- `RoutineService`: before every create/update/delete, it calls `caretaker.save(snapshot())`. `undo()` pops the last memento and restores it in one transaction (delete all slots, re-insert from the snapshot).

### E2-S4 Caching Proxy
- `RoutineQueryService` interface: `RoutineResponse getWeek()`.
- `RoutineQueryServiceImpl` (real, hits the DB).
- `CachingRoutineServiceProxy` (`@Primary`): holds the last `RoutineResponse` and returns the cache when present. `RoutineService` calls `proxy.invalidate()` after any write or undo.
- The controller depends only on the `RoutineQueryService` interface.

### E2-S5 Notices CRUD
`NoticeController` at `/api/v1/notices`:
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/notices?page&size&q` | any | pinned first, then newest; `q` does a case-insensitive title/body search |
| GET | `/notices/{id}` | any | |
| POST | `/notices` | CR, TEACHER | `{title, body, pinned}` → 201; the author is the current user |
| PUT | `/notices/{id}` | CR, TEACHER | only the **author** or ADMIN; otherwise 403 |
| DELETE | `/notices/{id}` | CR, TEACHER, ADMIN | author or ADMIN only → 204 |

`NoticeResponse{id, title, body, pinned, author:{id,name}, createdAt, updatedAt}`.

### E2-S6 Frontend
- Routine page: `Api.get('/routine')` renders the same table as now (days × time slots). In CR mode the editor opens a form to add, edit or delete a slot (subject dropdown from `/subjects`), and there is an **Undo** button that calls `/routine/undo`.
- Notice page: a list from `/notices` (pinned badge), a create form for CR/TEACHER, and edit/delete buttons only on your own notices.
- Remove `routine`, `notices` and `subjects` from `frontend/js/data.js`.

### E2-S7 Tests (quota: 5 unit + 2 integration)
Unit tests:
1. `RoutineServiceTest.create_whenOverlapping_throwsConflict`
2. `RoutineServiceTest.undo_restoresPreviousSnapshot`
3. `RoutineServiceTest.undo_whenEmpty_throwsConflict`
4. `CachingRoutineServiceProxyTest.getWeek_secondCall_usesCache_andInvalidateClears`
5. `NoticeServiceTest.update_byNonAuthor_throwsForbidden`
6. `SubjectServiceTest.create_duplicateCode_throwsConflict`
7. `RoutineAssemblerTest.build_groupsSlotsByDay_sortedByStart`

Integration tests:
1. `NoticeControllerIT.create_asCR_returns201_andStudentCanRead`
2. `NoticeControllerIT.create_asStudent_returns403`
3. `RoutineControllerIT.createSlot_thenGetRoutine_containsSlot`

## Definition of Done
- [ ] CR creates subjects and slots, the student sees the routine, and undo works
- [ ] Teacher/CR post notices; a student cannot (403); a non-author cannot edit (403)
- [ ] The frontend Routine and Notice pages use the API; mock data is removed
- [ ] ≥ 5 unit + 2 IT green; `docs/agent.md` updated
