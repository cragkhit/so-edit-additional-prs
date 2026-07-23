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

Before editing, read `AGENTS.md`, `CONTRIBUTING.md`, the README, pull-request templates, and relevant build files. Follow project-specific testing, licensing, formatting, authorship, and disclosure requirements.

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

## Completed pull requests

| Candidate | Repository | Change | Verification | Pull request |
| --- | --- | --- | --- | --- |
| 1 | `bcgit/bc-java` | Added identity and null checks to `SMIMETest`'s byte-array comparison. | `./gradlew :util:compileTestJava :util:test` | [bcgit/bc-java#2371](https://github.com/bcgit/bc-java/pull/2371) |

## Skipped candidates

| Candidate | Repository | Reason |
| --- | --- | --- |
| 2 | `lenve/javaeetest` | The matched Stack Overflow revision only removes Markdown backticks around the Java code block. The repository file already contains ordinary Java source without those backticks, so there is no applicable code change or test to submit. |

## Candidate 1 record

- Stack Overflow answer: <https://stackoverflow.com/a/40056844>
- Recommendation: add same-reference and null handling before comparing arrays.
- Target: `util/src/test/java/org/bouncycastle/asn1/smime/test/SMIMETest.java`.
- Implementation: preserved the existing `isSameAs` helper and added the identity and null checks in the same order as the latest Stack Overflow revision.
- Branch: `matcha-update-smime-array-comparison`.
- Commit: `e9aaa7a2c`.
- Result: pull request opened by `cragkhit` against `bcgit/bc-java:main`.
- Bouncy Castle's contribution guide requires disclosure of generative-AI assistance, so the PR includes that disclosure in addition to the Matcha study and IRB text.
