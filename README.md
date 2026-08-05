# Stack Overflow Edit Additional PRs

This workspace prepares additional pull requests from the Improving Code recommendations in the Matcha study. The study mines edits to accepted Stack Overflow answers and applies suitable newer revisions to matching code in open-source Java projects.

## Main key file

**`matcha_results_2024-05-07_manual_validation_FINAL.csv` is the main key file
for this workspace.** It is the authoritative joined dataset for recommendation
pairs, manual validation, pull-request submission and status, paper-cohort
membership, and final PR review decisions. Scripts, the `pr-double-check` app,
the README cohort counts, and publication tables should use this file as their
primary source of truth.

The CSV currently contains 793 rows and 30 columns. Each row represents one
matched Stack Overflow-to-GitHub recommendation pair, not necessarily one
unique PR. Several recommendation rows can point to the same `Open PR Link`;
there are currently 108 rows with PR links representing 100 unique PR URLs.
PR-level analyses must therefore normalize and group by `Open PR Link` before
counting. The `No` column is the stable pair ID used to join this CSV to source
workbooks such as `Manual_Validation_Result_summary 25Aug25.xlsx`.

### Column structure

| Column group | Columns | Meaning |
| --- | --- | --- |
| Pair identity and source | `No`, `GH Project`, `Link SO` | Stable pair ID, project identifier, and associated Stack Overflow question. |
| Manual answer classification | `Valid SO answer...`, the two `Baltes's Catagories...` columns, `Aspect`, `Useful Recommendation`, `Type of Recommendation`, `Implementation Detail (Open PR)`, `Remarks` | Human validation and classification of whether the answer change is useful, its recommendation type, and the rationale or implementation notes. |
| GitHub code location | `GitHub Java File Path`, `GitHub Method Name`, `GitHub Start Line`, `GitHub End Line` | Repository code location matched to the older Stack Overflow code. |
| Stack Overflow code location | `Stack Overflow Java File Path`, `Stack Overflow Method Name`, `Stack Overflow Start Line`, `Stack Overflow End Line` | Local extracted answer revision and matched code range. |
| Pair matching | `Match Status`, `Useful Pairs Cross-Check` | Whether the code pair matched and whether it was confirmed during the useful-pair cross-check. |
| PR workflow | `Recommendation PR Status`, `Recommendation PR Reason`, `Open PR Link`, `PR Opened At (UTC)` | Whether a recommendation was submitted, skipped, or closed because of an issue; its rationale; and the resulting PR URL and opening time. |
| Cohort membership | `Old PR Status`, `Already In Paper?` | PR age classification and the study or paper cohort to which the PR belongs. |
| Current review state | `PR Status`, `Review Note`, `Valid` | Latest fetched GitHub state and the manual reviewer’s note and Valid/Invalid decision. The review app writes these values back to the CSV. |

### Important values

- `Useful Recommendation` is copied verbatim from `Result Compare Afte Resolve
  Conflict` in the manual-validation workbook. Its source values are
  `Agress Yes` and `Agree No`; the original spelling is intentionally
  preserved.
- `Type of Recommendation` is copied from `Recommendation Type (Summary)` and
  contains `Improving Code`, `Bug Fixing`/`Bug fixing`, or a blank value when
  the recommendation was not accepted.
- `Recommendation PR Status` uses `SUBMITTED`, `SKIPPED`, and
  `CLOSED DUE TO ISSUE`. The accompanying reason column records the detailed
  rationale.
- `Old PR Status` uses `NEW` and `OLD`. It is a PR-age flag, not the live GitHub
  state.
- `Already In Paper?` identifies publication cohorts:
  - `IN_PAPER` identifies PRs already included in the earlier paper;
  - `IN_PAPER_REVISION` identifies PRs selected for the revision cohort, subject
    to removal when `Recommendation PR Status` becomes
    `CLOSED DUE TO ISSUE`;
  - `NOT_IN_PAPER_ISSUE` excludes PRs closed because a project or contribution
    issue prevented inclusion; and
  - `NOT_IN_PAPER_BAD_PR` excludes the five old PRs judged unsuitable for the
    revised cohort.
- `PR Status` records the fetched GitHub state, such as `Open`, `Merged`,
  `Closed`, or `Unavailable`. It can change over time and should be refreshed
  before reporting results.
- `Valid` is the final manual PR-level decision (`Valid`, `Invalid`, or blank
  for pending), while `Review Note` stores the reviewer’s explanation.

When one PR URL occurs on multiple rows, PR-level flags and decisions should be
interpreted across the entire URL group. In particular, an exclusion flag such
as `NOT_IN_PAPER_ISSUE` on any associated row excludes that PR from the cohort.

## Inputs

- `2026_Matcha_PLOS_One.pdf`: study paper and methodology.
- `Matcha_Pair_Analysis_Jul2026.xlsx`: manually selected candidates. All 79 rows in the `Useful Pairs` sheet are treated as Improving Code recommendations; the `Bug/Improvement` column is not used as a filter.
- `matcha_recommendation_github_files.csv`: working inventory with a running ID, local recommendation file, current GitHub target URL, and Stack Overflow answer URL.
- `/Users/chaiyong/Downloads/do_not_delete/Matcha_Study/java_files/<answer-id>/`: extracted Stack Overflow code revisions. Prefer the matching `*_recent.java` file. If it does not exist, use the newest available intermediate revision for the matched code block.
- `template.md`: reusable pull-request body.

## 33-PR analysis cohort

The current additional-PR analysis cohort contains **33 unique pull requests**.
It is derived from `matcha_results_2024-05-07_manual_validation_FINAL.csv` by
grouping rows by the normalized value of `Open PR Link` and retaining a PR only
when:

1. at least one associated row has `Old PR Status` set to `NEW`; and
2. none of its associated rows has `Recommendation PR Status` set to
   `CLOSED DUE TO ISSUE`.

This is a PR-level selection rather than a row-level selection. A PR represented
by multiple recommendation rows is therefore counted once, and a
`CLOSED DUE TO ISSUE` value on any associated row excludes the entire PR.

| Selection step | Unique PRs |
| --- | ---: |
| All PRs recorded in the validation CSV | 100 |
| Marked `NEW` | 71 |
| `NEW` and marked `CLOSED DUE TO ISSUE` | 38 |
| Final `NEW` cohort excluding `CLOSED DUE TO ISSUE` | **33** |

For the LaTeX summary table, `GH_selection.csv` is the authoritative source for
the historical `stargazers`, `forks`, and `watchers` values. The displayed
`stars/forks/watchers` field uses those columns in that order. The H, M, and L
popularity labels come from `stars_region`, mapped as `3 = H`, `2 = M`, and
`1 = L`.

Two repositories have been renamed since the metrics snapshot and are joined
to their historical `GH_selection.csv` records as follows:

| PR repository | Metrics repository in `GH_selection.csv` |
| --- | --- |
| `apache/hugegraph-toolchain` | `apache/incubator-hugegraph-toolchain` |
| `joshiejack/Harvest-Festival` | `penguinsquad/harvest-festival` |

The status snapshot used for the 33-PR LaTeX table contains 24 PRs under review
(`U`), 3 merged PRs (`M`), and 6 PRs closed without merging (`C`). Its project
groups contain 18 high-popularity (`H`), 13 medium-popularity (`M`), and 2
low-popularity (`L`) PRs. PR status is time-dependent and should be fetched
again before regenerating publication results; the repository metrics remain
fixed to the `GH_selection.csv` study snapshot.

### Included pull requests

The following PRs form the effective revision cohort: they are marked
`IN_PAPER_REVISION` and are not marked `CLOSED DUE TO ISSUE` in the validation
CSV:

| No. | GitHub project | Pull request | Status | Feedback |
| ---: | --- | --- | --- | --- |
| 1 | `1024-lab/smart-admin` | [PR #133](https://github.com/1024-lab/smart-admin/pull/133) | Open | |
| 2 | `AbFab3D/AbFab3D` | [PR #20](https://github.com/AbFab3D/AbFab3D/pull/20) | Open | |
| 3 | `aionnetwork/aion` | [PR #1180](https://github.com/aionnetwork/aion/pull/1180) | Open | |
| 4 | `alchitry/Alchitry-Labs` | [PR #38](https://github.com/alchitry/Alchitry-Labs/pull/38) | Open | |
| 5 | `alibaba/spring-cloud-alibaba` | [PR #4376](https://github.com/alibaba/spring-cloud-alibaba/pull/4376) | Merged | |
| 6 | `ambiverse-nlu/ambiverse-nlu` | [PR #55](https://github.com/ambiverse-nlu/ambiverse-nlu/pull/55) | Open | |
| 7 | `ant-media/Ant-Media-Server` | [PR #7990](https://github.com/ant-media/Ant-Media-Server/pull/7990) | Open | |
| 8 | `Anuken/Arc` | [PR #207](https://github.com/Anuken/Arc/pull/207) | Closed | Feedback |
| 9 | `apache/hugegraph-toolchain` | [PR #750](https://github.com/apache/hugegraph-toolchain/pull/750) | Open | Feedback |
| 10 | `artisynth/artisynth_core` | [PR #20](https://github.com/artisynth/artisynth_core/pull/20) | Closed | Feedback |
| 11 | `atilika/kuromoji` | [PR #143](https://github.com/atilika/kuromoji/pull/143) | Open | |
| 12 | `ballerina-platform/ballerina-lang` | [PR #44681](https://github.com/ballerina-platform/ballerina-lang/pull/44681) | Open | |
| 13 | `bcgit/bc-java` | [PR #2386](https://github.com/bcgit/bc-java/pull/2386) | Closed | Feedback |
| 14 | `carlspring/s3fs-nio` | [PR #973](https://github.com/carlspring/s3fs-nio/pull/973) | Open | |
| 15 | `chatty/chatty` | [PR #562](https://github.com/chatty/chatty/pull/562) | Open | |
| 16 | `DaxiaK/MyDiary` | [PR #77](https://github.com/DaxiaK/MyDiary/pull/77) | Open | |
| 17 | `dietzm/GCodeInfo` | [PR #17](https://github.com/dietzm/GCodeInfo/pull/17) | Open | |
| 18 | `encryptedsystems/Clusion` | [PR #28](https://github.com/encryptedsystems/Clusion/pull/28) | Open | |
| 19 | `evgenyzinoviev/gravitydefied` | [PR #13](https://github.com/evgenyzinoviev/gravitydefied/pull/13) | Open | |
| 20 | `freedomotic/freedomotic` | [PR #535](https://github.com/freedomotic/freedomotic/pull/535) | Open | |
| 21 | `giginet/CCSocialShare` | [PR #8](https://github.com/giginet/CCSocialShare/pull/8) | Open | |
| 22 | `Kickflip/kickflip-android-sdk` | [PR #72](https://github.com/Kickflip/kickflip-android-sdk/pull/72) | Open | |
| 23 | `KnowageLabs/Knowage-Server` | [PR #986](https://github.com/KnowageLabs/Knowage-Server/pull/986) | Merged | |
| 24 | `liquibase/liquibase` | [PR #7870](https://github.com/liquibase/liquibase/pull/7870) | Closed | |
| 25 | `MesquiteProject/MesquiteCore` | [PR #135](https://github.com/MesquiteProject/MesquiteCore/pull/135) | Open | |
| 26 | `mucommander/mucommander` | [PR #1500](https://github.com/mucommander/mucommander/pull/1500) | Closed | |
| 27 | `p2abcengine/p2abcengine` | [PR #12](https://github.com/p2abcengine/p2abcengine/pull/12) | Open | |
| 28 | `joshiejack/Harvest-Festival` | [PR #233](https://github.com/joshiejack/Harvest-Festival/pull/233) | Open | |
| 29 | `pylerSM/XInstaller` | [PR #58](https://github.com/pylerSM/XInstaller/pull/58) | Open | |
| 30 | `Red5/red5-server` | [PR #448](https://github.com/Red5/red5-server/pull/448) | Open | |
| 31 | `TheAlgorithms/Java` | [PR #7550](https://github.com/TheAlgorithms/Java/pull/7550) | Merged | |
| 32 | `tony19/logback-android` | [PR #446](https://github.com/tony19/logback-android/pull/446) | Closed | |
| 33 | `yahoo/elide` | [PR #3420](https://github.com/yahoo/elide/pull/3420) | Open | |

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
