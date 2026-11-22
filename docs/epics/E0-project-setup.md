# E0 — Project Setup & Git Workflow

| Field | Value |
|---|---|
| Owner | Dev1 |
| Reviewer | Dev2 (Dev1 cannot approve their own PR) |
| Branch | `feature/E0-project-setup` |
| Depends on | nothing |
| Blocks | every other epic |
| Rubric targets | CI/CD & Git Workflow: branch protection (5), workflow correct (part of 5); avoids the automatic fail "direct push to main" |

## Goal
Set up the professional workflow before any feature work: the documented plan, agent instructions, a PR template, a CI that runs tests on every PR, a `develop` branch, and branch protection that makes direct pushes to `main` impossible.

## Stories

### E0-S1 Project guideline and team plan in `docs/`
- `docs/PROJECT_GUIDELINE.md`: the course brief, verbatim.
- `docs/TEAM_PLAN.md`: epics, owners, DB design, pattern map, test quota.
- `docs/epics/*.md`: one detailed spec per epic, plus `CONVENTIONS.md`.

**Acceptance:** the files exist and the links between them work.

### E0-S2 `docs/agent.md`
Agent and developer instructions: what to read first, how to work an epic, hard rules, the epic status board, the test tracker and a decisions log.

**Acceptance:** the board lists E0–E8, with E0 `In progress` and all others `Not done`.

### E0-S3 `docs/GIT_WORKFLOW.md`
Contains the one-time GitHub setup, every commit/branch/rebase/push command, the PR and review process, and releases.

### E0-S4 PR template
`.github/pull_request_template.md` with epic, stories, tests and checklist sections. GitHub only reads this from `.github/`.

### E0-S5 CI workflow
`.github/workflows/ci.yml`:
- Triggers only on `pull_request` into `develop`/`main` (review) and `push` to `develop`/`main` (after merge), plus manual `workflow_dispatch`. Pushes to other branches do not trigger CI. A `concurrency` group cancels outdated runs.
- One job **named `build-and-test`**, because branch protection refers to this exact name.
- JDK 17 Temurin with Maven cache; runs `mvn -B clean verify` in `backend/` and uploads the surefire reports.
- E6 extends this file later with the Docker build and deploy jobs. **Do not rename the job.**

**Acceptance:** CI runs on the E0 PR and is green.

### E0-S6 GitHub settings (manual, done by Dev1 in the browser)
Follow `docs/GIT_WORKFLOW.md` §1 exactly:
1. Add Dev2 and Dev3 as collaborators (Write).
2. Create and push `develop`; make it the default branch.
3. Push `feature/E0-project-setup` and open a PR into `develop`. CI runs and registers `build-and-test`.
4. Protect `main`: PR required, 1 approval, dismiss stale approvals, `build-and-test` required, up-to-date branch, conversation resolution, no force push, no deletion, **no bypass, including admins**.
5. Protect `develop` with the same rule, minus up-to-date if it becomes annoying.
6. Merge options: squash for features, merge commit for releases, auto-delete head branches.
7. Verify that a direct push to `main` is rejected, and **take a screenshot for the demo**.
8. Dev2 approves the E0 PR, and Dev1 squash-merges it.
9. Dev2 and Dev3 clone the repo and set their git identity (`GIT_WORKFLOW` §2.1).

## Definition of Done
- [ ] `develop` exists and is the default branch
- [ ] `main` and `develop` are protected; a direct push is rejected (screenshot saved)
- [ ] The E0 PR merged into `develop` with a green CI and 1 approval
- [ ] `docs/agent.md` shows E0 = `Done`

## Notes for agents
- This epic contains **no Java code**. Do not modify `backend/` here.
- AI agents must not push or change GitHub settings. They write the files, commit locally and give Dev1 the commands.
