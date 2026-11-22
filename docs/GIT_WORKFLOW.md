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

### 1.3 Push the Epic 0 branch, open its PR and get the first CI run
Branch protection can only *require* a status check that GitHub has already seen run. So E0 must open a PR first; that run registers the check name `build-and-test`.

**Step 1: push the branch** (`develop` must already be pushed, see §1.2):
```bash
git switch feature/E0-project-setup
git status                                   # must be clean
git log --oneline develop..HEAD              # the E0 commits that will be in the PR
git push -u origin feature/E0-project-setup
```

**Step 2: open the PR on GitHub (web UI)**
1. Open the repo page. A yellow banner **"feature/E0-project-setup had recent pushes → Compare & pull request"** appears; click it. If there is no banner, go to **Pull requests → New pull request**.
2. Set the dropdowns: **base: `develop`** ← **compare: `feature/E0-project-setup`**. Check that base is **not** `main`.
3. Title: `E0: Project Setup & Git Workflow`.
4. The description is pre-filled from the PR template. Fill it in: tick E0-S1…S5, Tests: "no code changes; existing 3 tests", How to test: "CI green".
5. Right sidebar → **Reviewers** → choose **Dev2** (Dev1 cannot approve their own PR).
6. Optionally: **Assignees** → yourself; **Labels** → `epic`, `docs`.
7. Click **Create pull request**.

**Alternative: GitHub CLI** (`gh auth login` once):
```bash
gh pr create --base develop --head feature/E0-project-setup \
  --title "E0: Project Setup & Git Workflow" \
  --body-file .github/pull_request_template.md \
  --reviewer <dev2-github-username>
gh pr view --web          # open it in the browser to finish the description
```

**Step 3: watch CI run**
1. At the bottom of the PR, the checks box shows **"CI / build-and-test — In progress"** (yellow dot). It takes about 1–3 minutes.
2. Click **Details** (or the **Checks** tab, or the repo's **Actions** tab) to see the live log: checkout → JDK 17 → `mvn -B clean verify` → upload test reports.
3. Green ✔ means it passed. A red ✖ means you should open the log, fix the problem locally, then `git commit` and `git push`. The PR re-runs automatically.
4. With the CLI: `gh pr checks --watch`.

If no check appears:
- **Actions → "I understand my workflows, go ahead and enable them"** (Actions may be disabled on a new repo).
- **Settings → Actions → General → Allow all actions**.
- The workflow file must be at exactly `.github/workflows/ci.yml` on the PR branch, and the PR base must be `develop` or `main`. CI runs only on PRs into `develop`/`main` and on pushes or merges into `develop`/`main`. Pushes to feature or docs branches alone do not trigger it (a manual run is possible via **Actions → CI → Run workflow**).

**Step 4: protect the branches** (§1.4). Now that the check has run, **`build-and-test`** shows up in the "Require status checks" search box. Type `build-and-test` and select it.

**Step 5: review and merge E0**
1. Dev2 opens the PR → **Files changed** → reads the docs → **Review changes → Approve → Submit review**.
2. Dev1: when the PR is green and approved, click **Squash and merge**. The title becomes `docs(E0): project setup & git workflow (#1)`. **Confirm**, then **Delete branch**.
3. Everyone syncs locally:
   ```bash
   git switch develop
   git pull origin develop
   git branch -d feature/E0-project-setup
   ```
4. Dev1 sets E0 to `Done` in `docs/agent.md` via a small docs PR (§5.1).

### 1.4 Branch rulesets for `main` and `develop`
Only `main` and `develop` get rules. `feature/*` and `docs/*` branches have **no rules**, so anyone can push to them freely.

Create **two rulesets**: **Settings → Rules → Rulesets → New ruleset → New branch ruleset**.

**Header of each ruleset:**
| Field | `protect-main` | `protect-develop` |
|---|---|---|
| Ruleset name | `protect-main` | `protect-develop` |
| Enforcement status | **Active** | **Active** |
| Bypass list | **Empty**: nobody, not even the owner or admins, can bypass | **Empty** |
| Target branches | Add target → Include by pattern → `main` | Add target → Include by pattern → `develop` |

**Branch rules** (✅ = tick, ⬜ = leave unticked):
| Rule | `main` | `develop` | Why |
|---|---|---|---|
| Restrict creations | ⬜ | ⬜ | The branches already exist |
| Restrict updates | ⬜ | ⬜ | ⚠ With an empty bypass list this would block **all** merges, including PRs |
| Restrict deletions | ✅ | ✅ | Nobody can delete `main`/`develop` |
| Require linear history | ⬜ | ✅ | `main` receives **merge commits** from release PRs; `develop` only gets squash merges |
| Require deployments to succeed | ⬜ | ⬜ | Deploy happens *after* merge (E6) |
| Require signed commits | ⬜ | ⬜ | Needs a GPG/SSH signing setup for every dev; not required by the course |
| **Require a pull request before merging** | ✅ | ✅ | **No direct push**: the course's automatic-fail rule |
| ↳ Required approvals | **1** | **1** | Dev1 reviews (Dev2 for Dev1's own PRs) |
| ↳ Dismiss stale pull request approvals when new commits are pushed | ✅ | ✅ | A new push needs a fresh approval |
| ↳ Require review from specific teams | ⬜ | ⬜ | No GitHub teams (personal repo) |
| ↳ Require review from Code Owners | ⬜ | ⬜ | No `CODEOWNERS` file |
| ↳ Require approval of the most recent reviewable push | ⬜ | ⬜ | Would block Dev1 from approving release PRs that contain merges Dev1 performed |
| ↳ Require conversation resolution before merging | ✅ | ✅ | Every review comment must be resolved |
| ↳ Require an additional approval for unattributed Copilot PRs | ⬜ | ⬜ | Copilot is not used |
| ↳ Allowed merge methods | **Merge** only | **Squash** only | Releases keep history; each epic becomes one commit on develop |
| **Require status checks to pass** | ✅ | ✅ | CI must be green before a merge |
| ↳ Require branches to be up to date before merging | ⬜ | ✅ | develop: the PR must contain the latest develop (click **Update branch** on the PR). main: off, because develop never contains main's release merge commit and would always show as "out of date" |
| ↳ Do not require status checks on creation | ⬜ | ⬜ | Not needed |
| ↳ Required status checks → **Add checks** | `build-and-test` | `build-and-test` | Type `build-and-test` and select it (source: GitHub Actions). It only appears after CI ran once (§1.3) |
| Block force pushes | ✅ | ✅ | History cannot be rewritten |
| Require code scanning results | ⬜ | ⬜ | Not configured |
| Require code quality results | ⬜ | ⬜ | Not configured |
| Restrict code coverage | ⬜ | ⬜ | No coverage upload |
| Automatically request Copilot code review | ⬜ | ⬜ | The human reviewer is Dev1 |

Click **Create** for each ruleset. Check that both appear under **Rulesets** with status **Active**.

### 1.5 What the rules mean in practice
| Action | `feature/*`, `docs/*` | `develop` | `main` |
|---|---|---|---|
| `git push` directly | ✅ allowed | ❌ rejected | ❌ rejected |
| Force push / delete branch | ✅ (own branch) | ❌ | ❌ |
| Merge a PR | n/a | needs 1 approval + green `build-and-test` + up to date + resolved conversations → **Squash and merge** | needs 1 approval + green `build-and-test` + resolved conversations → **Create a merge commit** |
| CI runs | no | on PR + after merge | on PR + after merge |

### 1.6 Repo merge settings
In **Settings → General → Pull Requests** (repo-wide; the rulesets above narrow it per branch):
- ✅ Allow **merge commits** (used for develop → main)
- ✅ Allow **squash merging** (used for feature/docs → develop), with default message "Pull request title"
- ⬜ Allow rebase merging
- ✅ Always suggest updating pull request branches (shows the **Update branch** button)
- ✅ **Automatically delete head branches**

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

## 5.1 Docs changes and syncing them with every developer
`docs/` is the team's shared brain: plan, epic specs, status board and API list. **All docs changes go to `develop`** so every developer works from the same version.

Rules:
- **Never change `docs/` on `main`** (no direct commits, no docs-only PRs into `main`, no hotfixes). `main` only receives docs through a normal `develop → main` release PR.
- Docs changes that belong to an epic (ticking stories, `agent.md` status, `API.md` rows) go **in that epic's PR into `develop`**.
- Docs changes that cannot wait for an epic PR (e.g. marking an epic `Done` after merge, fixing a spec, logging a decision) go in a small docs branch into `develop`:
  ```bash
  git switch develop && git pull origin develop
  git switch -c docs/E2-mark-done                      # docs/<short-topic>
  # edit docs/agent.md, docs/TEAM_PLAN.md ...
  git add docs/agent.md docs/TEAM_PLAN.md
  git commit -m "docs(agent): mark E2 as done"
  git push -u origin docs/E2-mark-done
  # open PR: base develop <- docs/E2-mark-done, reviewer Dev1 → approve → squash & merge
  ```
- **After any docs PR merges into `develop`, every developer pulls it into their work:**
  ```bash
  git switch develop && git pull origin develop        # update local develop
  git switch feature/E<n>-<slug>                       # back to your epic
  git rebase develop                                   # bring the new docs into your branch
  git push --force-with-lease                          # only if the branch was already pushed
  ```
- Conflicts in `docs/agent.md` (two people edited the status board at the same time): keep **both** rows' latest values, then `git add docs/agent.md && git rebase --continue`.
- AI agents: before starting work, run `git fetch origin && git log HEAD..origin/develop -- docs/`. If that shows commits, tell the developer to sync (commands above) first.

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
