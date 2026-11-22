# ClassSync REST API Reference

Base URL: `/api/v1`. Send `Authorization: Bearer <jwt>` except on public endpoints. Errors use the `ApiError` JSON format (see [epics/CONVENTIONS.md](epics/CONVENTIONS.md) §3).

Each epic appends its endpoints here in the same PR that adds them.

## Common
| Method | Path | Roles | Request | Success | Errors |
|---|---|---|---|---|---|
| GET | /api/v1/health | public | none | 200 `{status:"UP"}` | none |

<!-- E1: Auth, Admin users · E2: Subjects, Routine, Notices · E3: Assignments, Comments, Files, Forms · E4: Chat · E5: Assistant -->
