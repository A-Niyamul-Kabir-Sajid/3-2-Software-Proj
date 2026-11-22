## Epic
E<n>: <Epic title>  ·  Owner: Dev<x>  ·  Reviewer: Dev1 (Dev2 if the author is Dev1)

## Stories completed
- [ ] E<n>-S1 …
- [ ] E<n>-S2 …

## What changed
<!-- short summary of endpoints, entities, patterns, UI -->

## Tests
- Unit tests added: <n>
- Integration tests added: <n>
- [ ] `mvn clean verify` passes locally

## How to test
<!-- curl / UI steps, test users, roles -->

## Checklist
- [ ] Targets `develop` (or `main` only for a release PR from `develop`)
- [ ] Role-based access is enforced and tested (403 case)
- [ ] Errors are handled by `GlobalExceptionHandler`
- [ ] No secrets or hardcoded credentials
- [ ] `docs/agent.md` status and `docs/TEAM_PLAN.md` stories updated
- [ ] Commits follow Conventional Commits; no AI co-author trailers
