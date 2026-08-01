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


## Relaxed batch opened 2026-08-01

Candidate-selection criteria were relaxed at the user's direction; all other procedural steps remained applicable.

| Final CSV No. | Repository | Pull request | Change |
| ---: | --- | --- | --- |
| 555 | AdoptOpenJDK/IcedTea-Web | [PR](https://github.com/AdoptOpenJDK/IcedTea-Web/pull/1001) | Copied the list before sorting to avoid mutating caller-owned state. |
| 688 | aliyun/aliyun-odps-java-sdk | [PR](https://github.com/aliyun/aliyun-odps-java-sdk/pull/80) | Made shared number formatting thread-safe with per-thread DecimalFormat state. |
| 225 | Anuken/Arc | [PR](https://github.com/Anuken/Arc/pull/207) | Handled Long.MIN_VALUE without overflow while formatting durations. |
| 712 | artisynth/artisynth_core | [PR](https://github.com/artisynth/artisynth_core/pull/20) | Closed the movie metadata reader with try-with-resources. |
| 793 | atilika/kuromoji | [PR](https://github.com/atilika/kuromoji/pull/143) | Escaped CSV fields and closed converter streams reliably. |
| 39 | aws/amazon-redshift-jdbc-driver | [PR](https://github.com/aws/amazon-redshift-jdbc-driver/pull/160) | Prevented accidental construction and subclassing of a utility class. |
| 430 | bookdash/bookdash-android-app | [PR](https://github.com/bookdash/bookdash-android-app/pull/69) | Rejected ZIP entries outside the extraction directory and closed resources. |
| 115 | camunda-community-hub/camunda-8-lowcode-ui-template | [PR](https://github.com/camunda-community-hub/camunda-8-lowcode-ui-template/pull/118) | Applied the existing bearer scheme as a global OpenAPI security requirement. |
| 682 | carlspring/s3fs-nio | [PR](https://github.com/carlspring/s3fs-nio/pull/973) | Compared mock base paths by value before cleanup. |
| 93 | chatty/chatty | [PR](https://github.com/chatty/chatty/pull/562) | Rejected grouped calculator expressions with a missing closing parenthesis. |
| 504 | controlsfx/controlsfx | [PR](https://github.com/controlsfx/controlsfx/pull/1622) | Detached sample rows from the enclosing memory-test application. |
| 87 | ConvertAPI/convertapi-library-java | [PR](https://github.com/ConvertAPI/convertapi-library-java/pull/47) | Closed the Okio request source with try-with-resources. |
| 328 | CruxFramework/crux | [PR](https://github.com/CruxFramework/crux/pull/1013) | Kept shared DocumentBuilder parsing inside a reliably released lock. |
| 694 | dromara/hodor | [PR](https://github.com/dromara/hodor/pull/68) | Rejected odd-length and invalid hexadecimal input. |
| 722 | dockstore/dockstore | [PR](https://github.com/dockstore/dockstore/pull/6330) | Normalized absent RSS text values before XML generation. |
| 674 | ambiverse-nlu/ambiverse-nlu | [PR](https://github.com/ambiverse-nlu/ambiverse-nlu/pull/55) | Preserved directory structure and propagated traversal failures while copying. |
| 266 | Devil-Chen/DVMediaSelector | [PR](https://github.com/Devil-Chen/DVMediaSelector/pull/14) | Respected the requested image directory and closed the output stream. |
| 786 | dougkeen/BartRunnerAndroid | [PR](https://github.com/dougkeen/BartRunnerAndroid/pull/33) | Checked wake-lock state before releasing it. |
| 19 | datastax/dsbulk | [PR](https://github.com/datastax/dsbulk/pull/533) | Propagated file-tree cleanup failures instead of swallowing them. |
| 231 | ballerina-platform/ballerina-lang | [PR](https://github.com/ballerina-platform/ballerina-lang/pull/44681) | Propagated file-visit failures and deleted visited directories directly. |


## Relaxed batch of 27 opened 2026-08-02

Candidate-selection criteria were relaxed at the user's direction; procedural validation, unique-URL checks, PR verification, and clone cleanup remained required.

| Final CSV No. | Repository | Pull request | Change |
| ---: | --- | --- | --- |
| 787 | 1024-lab/smart-admin | [PR](https://github.com/1024-lab/smart-admin/pull/133) | Cached successful CORS preflight responses for one hour. |
| 650 | airlift/airlift | [PR](https://github.com/airlift/airlift/pull/2086) | Clarified malformed-JSON exception behavior in JaxRsJsonMapper documentation. |
| 434 | alibaba/spring-cloud-alibaba | [PR](https://github.com/alibaba/spring-cloud-alibaba/pull/4376) | Cached gateway CORS preflight responses for one hour. |
| 514 | apache/netbeans | [PR](https://github.com/apache/netbeans/pull/9536) | Handled matching descriptor property names at index zero. |
| 522 | apache/hugegraph-toolchain | [PR](https://github.com/apache/hugegraph-toolchain/pull/750) | Cached Hubble CORS preflight responses for one hour. |
| 523 | apache/rocketmq | [PR](https://github.com/apache/rocketmq/pull/10746) | Closed both HTTP/2 proxy channels on backend failures. |
| 525 | apache/iotdb | [PR](https://github.com/apache/iotdb/pull/18379) | Stopped snapshot traversal after post-visit directory failures. |
| 526 | apache/pulsar | [PR](https://github.com/apache/pulsar/pull/26261) | Released reference-counted messages that could not be forwarded. |
| 655 | arduino/Arduino | [PR](https://github.com/arduino/Arduino/pull/12126) | Corrected delimiter spelling in split method documentation. |
| 85 | liquibase/liquibase | [PR](https://github.com/liquibase/liquibase/pull/7870) | Propagated post-visit directory traversal failures. |
| 528 | prometheus/client_java | [PR](https://github.com/prometheus/client_java/pull/2362) | Preserved nested temporary-volume deletion failures. |
| 164 | netty/netty | [PR](https://github.com/netty/netty/pull/17186) | Closed the inbound proxy peer after backend exceptions. |
| 581 | robolectric/robolectric | [PR](https://github.com/robolectric/robolectric/pull/11391) | Bound handler-thread teardown waits and asserted completion. |
| 597 | spring-projects/spring-tools | [PR](https://github.com/spring-projects/spring-tools/pull/1957) | Used locale-aware java.time formatting in the validation fixture. |
| 680 | OpenLiberty/open-liberty | [PR](https://github.com/OpenLiberty/open-liberty/pull/35407) | Handled JUnit descriptions that do not expose a test class. |
| 721 | opensearch-project/OpenSearch | [PR](https://github.com/opensearch-project/OpenSearch/pull/22627) | Bound OpenSearch task-executor test latch waits. |
| 242 | elastic/elasticsearch | [PR](https://github.com/elastic/elasticsearch/pull/155679) | Bound Elasticsearch task-executor test latch waits. |
| 621 | eugenp/tutorials | [PR](https://github.com/eugenp/tutorials/pull/19287) | Used UTF-8 and decoded only received echo bytes. |
| 633 | thingsboard/thingsboard | [PR](https://github.com/thingsboard/thingsboard/pull/15999) | Removed PEM certificate contents from TLS failure logs. |
| 218 | RPTools/maptool | [PR](https://github.com/RPTools/maptool/pull/6021) | Closed the PDF extraction marker stream with try-with-resources. |
| 723 | jcodec/jcodec | [PR](https://github.com/jcodec/jcodec/pull/520) | Prevented IOUtils construction and subclassing. |
| 86 | mucommander/mucommander | [PR](https://github.com/mucommander/mucommander/pull/1500) | Tracked the drag origin during incremental image panning. |
| 350 | sofastack/sofa-jraft | [PR](https://github.com/sofastack/sofa-jraft/pull/1273) | Rejected odd-length and invalid hexadecimal input. |
| 282 | eclipse-platform/eclipse.platform.swt | [PR](https://github.com/eclipse-platform/eclipse.platform.swt/pull/3480) | Disposed the SWT Display through try/finally. |
| 240 | gitlab4j/gitlab4j-api | [PR](https://github.com/gitlab4j/gitlab4j-api/pull/1330) | Removed an unused shared mutable date formatter. |
| 96 | polypheny/Polypheny-DB | [PR](https://github.com/polypheny/Polypheny-DB/pull/573) | Made the varchar metadata comparison null-safe. |
| 770 | TheAlgorithms/Java | [PR](https://github.com/TheAlgorithms/Java/pull/7550) | Rejected null BitonicSort arrays with a descriptive validation. |
