# Stack Overflow Edit Additional PRs

This workspace prepares additional pull requests from the Improving Code recommendations in the Matcha study. The study mines edits to accepted Stack Overflow answers and applies suitable newer revisions to matching code in open-source Java projects.

## Inputs

- `2026_Matcha_PLOS_One.pdf`: study paper and methodology.
- `Matcha_Pair_Analysis_Jul2026.xlsx`: manually selected candidates. All 79 rows in the `Useful Pairs` sheet are treated as Improving Code recommendations; the `Bug/Improvement` column is not used as a filter.
- `matcha_recommendation_github_files.csv`: working inventory with a running ID, local recommendation file, current GitHub target URL, and Stack Overflow answer URL.
- `/Users/chaiyong/Downloads/do_not_delete/Matcha_Study/java_files/<answer-id>/`: extracted Stack Overflow code revisions. Prefer the matching `*_recent.java` file. If it does not exist, use the newest available intermediate revision for the matched code block.
- `template.md`: reusable pull-request body.

## Candidate workflow

### 1. Select and validate a candidate

1. Select a row by `id` from `matcha_recommendation_github_files.csv`.
2. Extract the Stack Overflow answer ID from `recommendation_file` or `stackoverflow_url`.
3. Compare the matching `*_original.java` and `*_recent.java` files.
4. Open the target URL and confirm that the repository code still matches the earlier Stack Overflow revision.
5. If the recorded GitHub path no longer exists, inspect Git history for a rename or move. Mark it `MISSING` only if no current file can be found.
6. Check for an existing issue or pull request that already makes the same change.
7. Confirm that the recommendation remains correct for the project's current APIs, dependencies, conventions, and architecture.

The Stack Overflow revision is a recommendation that still requires project-context validation. Once validated, preserve its code as closely as possible in the patch. Do not replace it with a behaviorally equivalent project utility or alternative implementation merely because that approach appears more idiomatic. Make only the minimal adaptations required for the target language types, surrounding code, compilation, formatting conventions, or current APIs, and document any unavoidable deviation in the pull-request description.

### 2. Clone the repository and read its instructions

```bash
git clone https://github.com/<owner>/<repository>.git <repository>
cd <repository>
```

Before editing, search for and study `AGENTS.md`, `CONTRIBUTING.md`, the README, files under `.github/` (especially pull-request templates), relevant build files, and recent accepted pull requests. Determine the repository's conventions for branch names, commit messages, PR titles and bodies, issue references, tests, checklists, licensing, formatting, authorship, and disclosures. Follow those conventions when they exist; use the Matcha template only for elements the project does not prescribe. Record any project-specific requirement that materially affects the patch or PR.

### 3. Create a focused branch

```bash
git switch -c matcha-<short-change-name>
```

Keep each pull request focused on one recommendation unless several occurrences in the same project naturally belong together.

### 4. Implement and self-review the change

1. Change only the relevant code.
2. Keep the implementation as close as possible to the matching latest Stack Overflow code revision, including its control-flow structure and ordering of checks.
3. Do not substitute an equivalent helper, library call, refactoring, or redesign unless the Stack Overflow code cannot be applied directly; explain any necessary substitution in the PR.
4. Make only the smallest syntax, type, API, and style adjustments needed for the target project.
5. Add or update tests when required or when behavior is not already covered.
6. Compare the final repository diff against the original-to-latest Stack Overflow diff and confirm that the intended changes correspond.
7. Check the final patch for accidental formatting or unrelated changes.

```bash
git diff --check
git diff -- <changed-file>
git status --short
```

### 5. Build and test

Run the smallest relevant project test suite, followed by any broader suite required by the contribution guide. Record the exact successful command for the PR handoff.

Candidate 1 used:

```bash
./gradlew :util:compileTestJava :util:test
```

### 6. Commit

Follow the established Matcha PR style:

```bash
git add <changed-file>
git commit -m "Update <filename>" -m "<one-sentence change summary>"
```

The Git commit author comes from the local `user.name` and `user.email` configuration.

### 7. Authenticate and configure the fork

```bash
gh auth login -h github.com
gh auth status
git remote add fork https://github.com/<github-user>/<repository>.git
```

The pull request appears under the account authenticated by GitHub CLI. Verify the identity before pushing.

### 8. Push the branch

```bash
git push -u fork <branch-name>
```

### 9. Prepare the PR body

Copy `template.md` and replace:

- `<STACK_OVERFLOW_ANSWER_URL>` with `https://stackoverflow.com/a/<answer-id>`.
- `<CONCISE_DESCRIPTION>` with a short past-tense explanation of the actual project-specific change.
- `<TEST_COMMAND>` with the exact command used to verify the change.
- `<TEST_RESULT>` with the outcome, such as `Passed`.

The `Testing` section is conditional. Include it only when a relevant test exists and was run. If the target project has no applicable test for the change, remove the template comment and the entire `Testing` section when populating the PR; do not add a placeholder or claim a test result.

Preserve the researcher affiliation, study explanation, IRB statement, participant-information link, separator, and `Proposed change` heading. Add any disclosures, tests, checklists, or other information required by the target repository. In particular, disclose AI assistance when a repository's contribution guide requires it.

### 10. Create and verify the PR

```bash
gh pr create \
  --repo <upstream-owner>/<repository> \
  --base <default-branch> \
  --head <github-user>:<branch-name> \
  --title "Update <filename>" \
  --body-file <completed-pr-body.md>

gh pr view <pr-number> \
  --repo <upstream-owner>/<repository> \
  --json url,title,state,author,baseRefName,headRefName,commits,files,body
```

Verify the author, base and head branches, commit, changed files, Stack Overflow link, study disclosure, and proposed-change text.

### 11. Remove the local clone

After the pull request has been created and verified, or after a candidate has been conclusively skipped, confirm that the clone contains no uncommitted or unpushed work. Then delete only that candidate's explicitly identified clone directory. Do not remove the study workspace, source workbook, Stack Overflow revision files, CSV, template, or other candidate clones.

Before deletion, verify the repository state:

```bash
git status --short
git log -1 --oneline
```

For a submitted candidate, also confirm that the local commit matches the pushed branch. Once verified, remove the exact clone path. Future work on the project should use a fresh clone so validation is based on the current upstream state.

## Mandatory per-candidate checklist

- [ ] Read the repository's `AGENTS.md`, contribution guide, README, `.github` PR templates, and relevant build files when present.
- [ ] Review recent accepted pull requests to learn the repository's current title, body, commit, test, checklist, and issue-linking conventions.
- [ ] Follow repository-specific conventions where they differ from or extend the Matcha template.
- [ ] Validate the original-to-latest Stack Overflow diff against the current repository code.
- [ ] Keep the patch as close as possible to the latest Stack Overflow revision and document unavoidable deviations.
- [ ] Run a relevant test when one exists; otherwise omit the entire `Testing` section from the PR.
- [ ] Verify the final commit, pushed branch, PR author, PR body, and changed files.
- [ ] Record a created PR in `matcha_recommendation_github_files.csv` and this README. For a skipped candidate, set the CSV `pr_url` value to `SKIPPED`, put the complete rationale in `notes`, and add it to the README's skipped-candidates table.
- [ ] Confirm the clone has no uncommitted work and that submitted commits are present on the pushed branch.
- [ ] Delete the exact local clone directory after the PR is verified or the candidate is conclusively skipped.
