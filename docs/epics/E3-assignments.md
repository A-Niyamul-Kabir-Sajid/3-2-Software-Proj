# E3 — Assignments, Forms, Comments & Uploads

| Field | Value |
|---|---|
| Status | **Not done** |
| Owner | Dev2 |
| Reviewer | Dev1 |
| Branch | `feature/E3-assignments` |
| Depends on | E1, E2 (`Subject`) |
| Blocks | E5 (RAG indexes assignments) |
| Rubric targets | CRUD (Assignment, Form, Comment); tables `assignments`, `submissions`, `comments`, `forms`, `form_responses`, `attachments`; patterns State, Strategy, Factory Method, Chain of Responsibility, Template Method, Abstract Factory, Builder; 6 unit + 3 IT |

## Goal
Teachers and the CR publish assignments tagged by subject, with an optional file or link. Students mark them complete, so each student sees their own pending and completed lists. Comments under an assignment are either PUBLIC or TEACHER_ONLY. The CR publishes forms (a link plus a deadline) and students mark them as submitted. The backend hosts uploaded files.

## Stories

### E3-S1 Entities
Package `assignment.entity`:
- `AssignmentStatus` enum: `DRAFT, PUBLISHED, CLOSED`.
- `Assignment`: `title` (≤ 150), `description` (TEXT), `subject` (`@ManyToOne` not null), `author` (`@ManyToOne User`), `dueAt` (`Instant`), `link` (URL, nullable), `attachment` (`@OneToOne Attachment`, nullable), `status` (STRING, default DRAFT). Table `assignments`. **Builder**: `Assignment.builder()…build()`.
- `Submission`: `assignment` (`@ManyToOne`), `student` (`@ManyToOne User`), `completedAt`. Unique constraint (`assignment_id`, `student_id`). Table `submissions`.

Package `comment.entity`:
- `CommentVisibility` enum: `PUBLIC, TEACHER_ONLY`.
- `Comment`: `assignment` (`@ManyToOne`), `author` (`@ManyToOne User`), `text` (≤ 1000), `visibility`. Table `comments`.

Package `form.entity`:
- `ClassForm` (avoids a name clash with HTML Form): `title`, `description`, `url` (an external Google Form link etc.), `deadline`, `author`. Table `forms`.
- `FormResponse`: `form` (`@ManyToOne`), `student` (`@ManyToOne`), `submittedAt`. Unique (`form_id`, `student_id`). Table `form_responses`.

Package `upload.entity`:
- `Attachment`: `originalName`, `storedName` (a UUID), `contentType`, `sizeBytes`, `storageType` (LOCAL/CLOUD), `uploadedBy`. Table `attachments`.

### E3-S2 Assignment CRUD + State + Strategy
**State pattern** in `assignment.state`:
```
AssignmentState (interface): publish(Assignment), close(Assignment), edit(Assignment), boolean visibleToStudents()
  DraftState      : publish -> PUBLISHED; close -> error; edit OK; not visible to students
  PublishedState  : publish -> error; close -> CLOSED; edit OK; visible
  ClosedState     : publish/close/edit -> BadRequestException("Assignment is closed"); visible
AssignmentStateFactory.of(AssignmentStatus) -> AssignmentState
```

**Strategy pattern** for sorting and filtering, in `assignment.strategy`:
- `AssignmentSortStrategy` interface with `Sort sort()`: `DueDateSort`, `NewestSort`, `SubjectSort`. The registry maps the `sort` query param (`due|newest|subject`) to a strategy, defaulting to `due`.

`AssignmentController` at `/api/v1/assignments`:
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/assignments?subjectId&status=pending\|completed&sort=due\|newest\|subject&page&size` | any | students see only PUBLISHED/CLOSED; `status` is relative to the current student's submissions |
| GET | `/assignments/{id}` | any | a student requesting a DRAFT gets 404 |
| POST | `/assignments` | TEACHER, CR | `{title, description, subjectId, dueAt, link?, attachmentId?}` → 201 (DRAFT) |
| PUT | `/assignments/{id}` | TEACHER, CR (author/ADMIN) | goes through `state.edit` |
| POST | `/assignments/{id}/publish` | author | DRAFT → PUBLISHED |
| POST | `/assignments/{id}/close` | author | PUBLISHED → CLOSED |
| DELETE | `/assignments/{id}` | author / ADMIN | 204 |

`AssignmentResponse{id, title, description, subject{id,code}, author{id,name}, dueAt, link, attachment{id,name,url}?, status, completedByMe (boolean), completedCount}`.

**Factory Method**: refactor the existing `assignment/PostFactory.java` into `common.pattern.PostFactory`. Its `create(PostType type, PostDraft draft, User author)` returns the right **entity** (`Notice`, `Assignment` or `ClassForm`). `UploadFacade` (S5) uses it when a post is created along with a file. Keep `PostFactoryTest` passing after updating it to the new signature.

### E3-S3 Per-student completion
| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/assignments/{id}/complete` | STUDENT | creates a Submission → 201; already completed → 409; CLOSED → 400 |
| DELETE | `/assignments/{id}/complete` | STUDENT | undo → 204 |
| GET | `/assignments/{id}/submissions` | TEACHER, CR | 200 list `{student{id,name}, completedAt}` |

### E3-S4 Comments + visibility Strategy
`CommentController`:
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/assignments/{id}/comments` | any | filtered by visibility strategy |
| POST | `/assignments/{id}/comments` | any | `{text, visibility}` → 201; students may post TEACHER_ONLY (a private question to the teacher) |
| DELETE | `/comments/{id}` | author / ADMIN | 204 |

**Strategy**: the `CommentVisibilityStrategy` interface has `boolean canView(Comment c, User viewer)`:
- `PublicVisibility`: always true.
- `TeacherOnlyVisibility`: true if the viewer is the comment author, a TEACHER, the assignment author, or an ADMIN.

The `Map<CommentVisibility, CommentVisibilityStrategy>` is injected, and the service filters the list with it.

### E3-S5 File upload pipeline
**Chain of Responsibility** in `upload.validation`:
```
UploadValidator (abstract): setNext(UploadValidator); validate(MultipartFile, User) -> check() then next
  FileNotEmptyValidator
  FileSizeValidator      (max from app.upload.max-size-mb, default 10)
  ContentTypeValidator   (allow pdf, docx, pptx, png, jpg, zip)
  RoleValidator          (STUDENT cannot upload)
UploadValidationChain builds: notEmpty -> size -> type -> role
```
Each validator throws `BadRequestException` (or `ForbiddenOperationException` for the role check).

**Abstract Factory** in `upload.storage`:
- `StorageFactory` interface: `FileStorage createStorage()`, `UrlResolver createUrlResolver()`.
- `LocalStorageFactory`: saves under `${app.upload.dir:./uploads}` and resolves URLs as `/api/v1/files/{id}`.
- `CloudStorageFactory` is a stub that throws `UnsupportedOperationException`, documented as the future S3 option. The bean is selected by `app.storage.type=local|cloud`.

**Template Method**: `AbstractUploadHandler.handle(file, user)` is `final` and runs `validate()` → `store()` → `persist()` → `afterUpload()` (a hook). `AttachmentUploadHandler` extends it.

**Facade**: `UploadFacade` hides chain + storage + handler from the controllers.

`FileController`:
| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/files` (multipart `file`) | TEACHER, CR | 201 `{id, name, url, size}` |
| GET | `/files/{id}` | any authenticated | streams the file with `Content-Disposition` |
| DELETE | `/files/{id}` | uploader / ADMIN | 204 |

In Docker the upload dir is a volume (E6).

### E3-S6 Forms
`FormController` at `/api/v1/forms`: full CRUD (CR, TEACHER create; author/ADMIN edit and delete), plus `POST /forms/{id}/submit` (STUDENT, 201; after the deadline → 400) and `DELETE /forms/{id}/submit`. A list item includes `submittedByMe` and `submittedCount`. The `status=pending|completed` filter works the same way as for assignments.

### E3-S7 Frontend
- Assignments page: subject filter chips (from `/subjects`), Pending/Completed tabs, sort select, "Mark complete" for students, a create form for teacher/CR (with file upload via `Api.upload('/files')` and then `attachmentId`), and Publish/Close buttons for the author.
- Assignment detail: a comments list with a visibility selector, and a lock icon on TEACHER_ONLY comments.
- Forms page: open link, "Mark submitted", and a CR create form.
- Remove `assignments` and `forms` from `data.js`.

### E3-S8 Tests (quota: 6 unit + 3 integration)
Unit tests:
1. `AssignmentStateTest.draft_publish_becomesPublished`
2. `AssignmentStateTest.closed_edit_throwsBadRequest`
3. `AssignmentServiceTest.complete_twice_throwsConflict`
4. `CommentServiceTest.list_asStudent_hidesOthersTeacherOnlyComments`
5. `UploadValidationChainTest.oversizedFile_isRejectedBySizeValidator`
6. `UploadValidationChainTest.studentUpload_isRejectedByRoleValidator`
7. `PostFactoryTest.create_assignmentType_returnsAssignmentEntity`
8. `FormServiceTest.submit_afterDeadline_throwsBadRequest`

Integration tests:
1. `AssignmentControllerIT.teacherCreatesPublishes_studentCompletes_flow`
2. `AssignmentControllerIT.create_asStudent_returns403`
3. `CommentControllerIT.teacherOnlyComment_notVisibleToOtherStudent`
4. `FileControllerIT.upload_disallowedType_returns400`

## Definition of Done
- [ ] Full flow: the teacher uploads a file → creates and publishes an assignment → a student sees it as pending → marks it complete → it moves to completed
- [ ] TEACHER_ONLY comments are hidden from other students
- [ ] All listed patterns are present with pattern-named classes
- [ ] ≥ 6 unit + 3 IT green; `docs/agent.md` updated
