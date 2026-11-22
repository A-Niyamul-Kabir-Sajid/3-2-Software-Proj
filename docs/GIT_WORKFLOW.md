# Git Workflow, PR & Review Guide

Branch model: **`main`** (protected, release only) ← **`develop`** (protected, integration) ← **`feature/E<n>-<slug>`** (one branch per epic).

- **Reviewer:** Dev1 reviews and approves every PR. Dev2 approves Dev1's own PRs, because GitHub blocks self-approval.
- **Never push to `main` directly.** The course treats this as an automatic fail.

---

## 1. Epic 0: one-time GitHub setup (Dev1, repo owner)

### 1.1 Add collaborators
GitHub repo → **Settings → Collaborators → Add people**. Add Dev2 and Dev3 with the **Write** role. They must accept the email invite.

### 1.2 Create `develop`
Do this once, from an up-to-date `main`:
```bash
git switch main
git pull origin main
git switch -c develop
git push -u origin develop
```
Then go to **Settings → General → Default branch** and set it to `develop`, so PRs target it by default.

### 1.3 Push the Epic 0 branch and open its PR
```bash
git switch feature/E0-project-setup
git push -u origin feature/E0-project-setup
```
Open a PR from `feature/E0-project-setup` into `develop`. CI must run on it. This first run creates the `build-and-test` check that branch protection needs.

### 1.4 Branch protection for `main`
**Settings → Branches → Add branch ruleset** (or **Add classic branch protection rule**). Set the target or branch name pattern to `main`, then enable:
- [x] **Require a pull request before merging**
  - [x] Required approvals: **1**
  - [x] Dismiss stale pull request approvals when new commits are pushed
- [x] **Require status checks to pass before merging**, with check **`build-and-test`**
  - [x] Require branches to be up to date before merging
- [x] **Require conversation resolution before merging**
- [x] **Block force pushes** / do not allow force pushes
- [x] **Restrict deletions**
- [x] **Do not allow bypassing the above settings**, also called "Include administrators". This is important: without it the owner can still push directly.

### 1.5 Branch protection for `develop`
Create the same rule for `develop`: PR required, 1 approval, `build-and-test` must pass, no force push.

### 1.6 Repo merge settings
In **Settings → General → Pull Requests**:
- Allow **squash merging** for feature → develop PRs, so each epic becomes one clean commit.
- Allow **merge commits** for develop → main releases.
- Enable **Automatically delete head branches**.

### 1.7 Verify protection works (screenshot this for the demo)
```bash
git switch main
git commit --allow-empty -m "test: direct push should be rejected"
git push origin main      # expected: rejected (protected branch)
git reset --hard HEAD~1   # undo the local test commit
```

---

## 2. Daily commit workflow (every dev)

### 2.1 One-time Git identity
```bash
git config --global user.name  "Your Name"
git config --global user.email "you@example.com"   # same email as your GitHub account
git config --global pull.rebase true               # keep history linear when pulling
git config --global init.defaultBranch main
```

### 2.2 Start an epic
```bash
git switch develop
git pull origin develop
git switch -c feature/E2-routine-notices           # name from TEAM_PLAN / agent.md
```

### 2.3 Commit professionally
Check what changed:
```bash
git status
git diff                   # unstaged changes
```

Stage only what belongs in this commit, file by file. Avoid `git add .` when unrelated files changed.
```bash
git add backend/src/main/java/edu/kuet/classsync/routine/
git diff --staged          # review exactly what will be committed
```

Commit with a Conventional Commit message:
```bash
git commit -m "feat(routine): add RoutineSlot entity and repository" \
           -m "Slot has M:1 relations to Subject and teacher User. Refs E2-S1."
```

Commit message format:
```
<type>(<scope>): <imperative summary, ≤ 72 chars, no trailing period>

<optional body: what and why, wrapped at 72 chars>
Refs E<n>-S<m>
```

| type | use for |
|---|---|
| `feat` | new feature / endpoint |
| `fix` | bug fix |
| `test` | adding or fixing tests |
| `docs` | documentation only |
| `refactor` | code change with no behaviour change |
| `chore` | build, deps, config |
| `ci` | GitHub Actions / pipeline |
| `build` | Dockerfile, compose, Maven |

The scope is the module: `auth`, `routine`, `notice`, `assignment`, `form`, `chat`, `rag`, `common`, `frontend`, `docker`, `ci`, `docs`.

**Good:** `feat(auth): add JWT login endpoint`, `test(notice): cover NoticeService delete not found`
**Bad:** `update`, `fixed stuff`, `final final`, and anything with a `Co-Authored-By:` AI trailer

Fix the last commit before pushing (never after it has been pushed):
```bash
git add <forgotten-file>
git commit --amend --no-edit
```

### 2.4 Keep your branch up to date with `develop`
```bash
git fetch origin
git rebase origin/develop
# if there are conflicts: fix files → git add <file> → git rebase --continue
# to abort:               git rebase --abort
```

### 2.5 Before pushing, run the tests locally
```bash
cd backend && mvn clean verify && cd ..
```

### 2.6 Push
```bash
git push -u origin feature/E2-routine-notices          # first push
git push                                               # later pushes
git push --force-with-lease                            # only after a rebase, only on your own branch
```

---

## 3. Opening a PR (epic owner)
1. On GitHub, go to **Pull requests → New** and set base **`develop`** ← compare **`feature/E<n>-…`**.
2. Title: `E<n>: <Epic title>`, for example `E2: Routine & Notices`.
3. Fill in the PR template (it loads automatically): stories done, tests added, how to test.
4. Reviewer: **Dev1**. For Dev1's own PRs, the reviewer is **Dev2**.
5. Wait for CI to pass (green). Fix any failures with new commits on the same branch.
6. In the same PR, update `docs/agent.md` (set the status to `In review`) and tick the stories in `docs/TEAM_PLAN.md`.

## 4. Reviewing a PR (Dev1)
Check the PR out locally if needed:
```bash
git fetch origin
git switch feature/E2-routine-notices
cd backend && mvn clean verify
```

Review checklist:
- [ ] CI `build-and-test` is green
- [ ] Every story in the epic is done, or listed as remaining in the PR description
- [ ] Endpoints are under `/api/v1/**` with correct HTTP methods and status codes (201 create, 204 delete, 404, 403…)
- [ ] Role checks are present (`@PreAuthorize` or URL rules), plus a test proving a 403
- [ ] Errors go through `GlobalExceptionHandler` / `ApiError`
- [ ] Design patterns are used where the plan says, with pattern-named classes
- [ ] Tests meet the quota (service unit tests + MockMvc integration tests)
- [ ] There are no secrets or hardcoded credentials, and no stray files (`target/`, `.env`, IDE folders)
- [ ] Commit messages follow the convention, with no AI co-author trailer
- [ ] `docs/agent.md` and `docs/TEAM_PLAN.md` are updated

Then go to **Files changed → Review changes** and choose **Approve**, or **Request changes** with comments.

## 5. Merging
- **Feature → develop:** use **Squash and merge**, with a title like `feat(E2): routine & notices (#12)`, and delete the branch.
- After the merge, the owner sets the epic to **Done** in `docs/agent.md`. This can go in the next PR, or in a small `docs:` PR.
- Everyone syncs:
  ```bash
  git switch develop && git pull origin develop
  git branch -d feature/E2-routine-notices
  ```

## 6. Release to `main` (deploys to Render)
When a set of epics is stable on `develop`:
1. Open a PR from **`develop` → `main`**, titled `release: v0.<n>.0`.
2. Dev1 approves it (Dev2 if Dev1 opened it), CI passes, then **Create a merge commit**.
3. The push to `main` triggers the deploy job in GitHub Actions, which deploys to Render (Epic E6).
4. Optionally tag the release:
   ```bash
   git switch main && git pull
   git tag -a v0.1.0 -m "Release v0.1.0"
   git push origin v0.1.0
   ```

## 7. Handy commands
```bash
git log --oneline --graph --decorate -20   # view history
git restore <file>                         # discard unstaged changes to a file
git restore --staged <file>                # unstage a file
git stash push -m "wip" && git stash pop   # park work temporarily
git branch -a                              # list local + remote branches
```
