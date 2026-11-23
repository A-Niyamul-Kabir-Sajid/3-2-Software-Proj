# CODEOWNERS: how it works and its variations

Our file is [`.github/CODEOWNERS`](../.github/CODEOWNERS):
```
* @A-Niyamul-Kabir-Sajid
```
It means: **Dev1 (`A-Niyamul-Kabir-Sajid`) owns every file in the repository.**

---

## 1. How it works
1. **Location.** GitHub looks for the file in `.github/CODEOWNERS`, then in the repo root `CODEOWNERS`, then in `docs/CODEOWNERS`, and uses the **first one it finds**. Keep exactly one; ours is in `.github/`.
2. **It is read from the PR's base branch.** When a PR targets `develop`, GitHub uses `develop`'s CODEOWNERS. A new CODEOWNERS file therefore only takes effect **after it is merged** into that branch (and into `main` for PRs targeting `main`).
3. **Auto-request.** When a PR changes a file that matches a pattern, GitHub **automatically adds the owner as a reviewer**. On its own, this is only a request; the PR can still merge without the owner's approval.
4. **Enforcement (optional).** If the branch ruleset has **Require a pull request → Require review from Code Owners** turned on, the PR **cannot merge until a code owner of each changed file approves**.
5. **Owners must have Write access** to the repo, otherwise the line is ignored. GitHub shows an error banner on the CODEOWNERS file page for invalid lines.
6. **An author can never approve their own PR.** This matters for us, see §4.

## 2. Syntax
```
<pattern>   <owner> [<owner> ...]
```
- The pattern uses the same rules as `.gitignore` (`*`, `**`, `/`, a trailing `/` for directories).
- An owner is a `@username`, `@org/team-name` (organisation repos only) or an email address of a GitHub user.
- `#` starts a comment.
- **Order matters: the LAST matching line wins.** Put general rules at the top and specific ones below them.
- With several owners on one line, **any one** of them approving satisfies the rule, not all of them.

## 3. Variations
```gitignore
# 1. Whole repository, one owner (what we use)
*                       @A-Niyamul-Kabir-Sajid

# 2. Whole repository, several owners: any ONE of them can approve
*                       @dev1-username @dev2-username

# 3. By file type
*.java                  @dev1-username
*.js  *.css             @dev3-username          # NOTE: only ONE pattern per line; this line is invalid
*.js                    @dev3-username
*.css                   @dev3-username

# 4. By directory (trailing slash = everything inside, recursively)
/backend/               @dev1-username
/frontend/              @dev3-username
/docs/                  @dev2-username

# 5. Leading slash = relative to repo root; without it, it matches anywhere
/docs/                  @dev2-username          # only the root docs folder
docs/                   @dev2-username          # any folder named docs, at any depth

# 6. Directory only at one level (direct files, not sub-folders)
/backend/*              @dev1-username

# 7. Any depth with **
**/security/**          @dev1-username
/backend/**/test/**     @dev2-username

# 8. Single specific files
/.github/workflows/ci.yml   @dev1-username
/backend/pom.xml            @dev1-username
/docker-compose.yml         @dev1-username

# 9. Per-module ownership (matching our epics)
/backend/src/main/java/edu/kuet/classsync/auth/      @dev1-username
/backend/src/main/java/edu/kuet/classsync/routine/   @dev2-username
/backend/src/main/java/edu/kuet/classsync/chat/      @dev3-username

# 10. Teams (only in repositories owned by a GitHub Organisation)
*                       @kuet-cse/classsync-reviewers

# 11. Email instead of username (must be a verified email of a GitHub account)
*                       dev1@example.com

# 12. Remove ownership for a path (a pattern with NO owner).
#     Since the last match wins, files here get no code owner.
/docs/generated/
```

## 4. Important for our team
- Dev1 is the only owner. GitHub **does not let Dev1 approve Dev1's own PRs** (E0, E1, E6).
- So in the rulesets (GIT_WORKFLOW §1.4) we **keep "Require review from Code Owners" OFF**. CODEOWNERS then:
  - automatically requests Dev1 on every PR from Dev2 and Dev3, so Dev1 never misses a review;
  - is still covered by "Required approvals: 1", so Dev2 can approve Dev1's PRs.
- If you later want Code Owner review to be **enforced**, change the line to include a second owner, so Dev1's own PRs can be approved by Dev2:
  ```
  * @A-Niyamul-Kabir-Sajid @dev2-username
  ```
  Then turn **Require review from Code Owners** ON in both rulesets. Any one of the two owners can approve.

## 5. Check that it works
1. Merge the PR containing `.github/CODEOWNERS` into `develop`.
2. Open `.github/CODEOWNERS` on GitHub in the `develop` branch. If there is no red error banner, the syntax is valid.
3. Have Dev2 open any PR into `develop`. **Reviewers** should show `A-Niyamul-Kabir-Sajid` with a shield icon and the text "Code owner".
