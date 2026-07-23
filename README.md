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

## Completed pull requests

| Candidate | Repository | Change | Verification | Pull request |
| --- | --- | --- | --- | --- |
| 1 | `bcgit/bc-java` | Added identity and null checks to `SMIMETest`'s byte-array comparison. | `./gradlew :util:compileTestJava :util:test` | [bcgit/bc-java#2371](https://github.com/bcgit/bc-java/pull/2371) |
| 14 | `open-hand/choerodon-starters` | Removed the redundant `JsonProcessingException` declaration from `UserListSerializer.serialize`. | `mvn -pl choerodon-gitlab4j-api -am test` could not start because Maven blocks the project's configured HTTP HZero repositories. | [open-hand/choerodon-starters#45](https://github.com/open-hand/choerodon-starters/pull/45) |
| 56 | `evgenyzinoviev/gravitydefied` | Ensured both streams in `LevelsManager.copy` are closed on failure using the latest answer's nested `try`/`finally` structure. | No relevant test exists in the project. | [evgenyzinoviev/gravitydefied#13](https://github.com/evgenyzinoviev/gravitydefied/pull/13) |
| 69 | `DaxiaK/MyDiary` | Ensured both streams in `FileManager.copy` are closed on failure. | No relevant test exists in the project. | [DaxiaK/MyDiary#77](https://github.com/DaxiaK/MyDiary/pull/77) |
| 79 | `yahoo/elide` | Sorted archive inputs and normalized file-entry timestamps for deterministic test archives. | Targeted Maven test could not start from the sparse checkout because the parent requires all sibling modules. | [yahoo/elide#3420](https://github.com/yahoo/elide/pull/3420) |

## Skipped candidates

| Candidate | Repository | Reason |
| --- | --- | --- |
| 2 | `lenve/javaeetest` | The matched Stack Overflow revision only removes Markdown backticks around the Java code block. The repository file already contains ordinary Java source without those backticks, so there is no applicable code change or test to submit. |
| 3 | `ibmruntimes/openj9-openjdk-jdk11` | The target already has the latest Stack Overflow revision's corrected `i > 0` loop. Its `RandomFactory.getRandom()` call deliberately supplies a logged, reproducible seed for OpenJDK regression testing; replacing it with `ThreadLocalRandom.current()` would bypass that infrastructure and make failures harder to reproduce. |
| 4 | `spring-projects/sts4` | The target contains only the Stack Overflow answer's first `home` handler, which is unchanged between revisions. The latest revision only renames a separate `/serverTime` handler from `home` to `serverTime`; that handler is absent from the target, so there is no applicable change or test to submit. |
| 5 | `apache/rocketmq` | The latest Stack Overflow revision replaces an empty Netty `Bootstrap.handler()` call with `new DiscardServerHandler()` for a separate third outbound connection. The matched RocketMQ file has never contained that bootstrap, third connection, empty handler call, or `DiscardServerHandler`; it is a single-outbound-channel HTTP/2 forwarding handler. Applying the recommendation would therefore require unrelated architecture rather than preserving the Stack Overflow edit. |
| 6 | `vipshop/pallas` | The mapped Stack Overflow block (block 2) is byte-for-byte identical in the original and latest revisions, so it contains no recommendation to apply. The answer's only edited block adds Android `runOnUiThread` handling around `randomNote()`, whereas the matched Pallas file is a non-Android JUnit test of a thread-pool executor and contains no `Handler`, UI thread, or `randomNote()` call. |
| 7 | `shulietech/linkagent` | The only code shared by the Stack Overflow answer and `ByteUtils` is the `byte2hex` helper, and LinkAgent already matches the latest revision exactly; Git history shows that helper has been unchanged since the repository's first commit. The answer's substantive edits add complete four- and five-party Diffie–Hellman demo programs, which are unrelated to this general-purpose byte utility and cannot be applied faithfully. |
| 8 | `google/gson` | The Stack Overflow edit fixes use of an uninitialized local by assigning `name = constant.name()` before looking up `@SerializedName`. Gson's enum handling has moved from `Gson.java` to `internal/bind/EnumTypeAdapter.java`, where the current implementation already initializes `name` from `constant.name()` before reading the annotation and uses the enum `Field` directly. Adding the answer's separate `classPrefix` customization would change Gson's serialized enum format rather than apply the bug fix. |
| 9 | `corretto/corretto-8` | The only code shared with the Stack Overflow answer is the `byte2hex`/`toHexString` formatting helper, and Corretto already matches the latest answer's helper; blame shows it has been unchanged since the test was introduced in 2013. The answer's substantive edits replace a four-party Diffie–Hellman demo and add a five-party demo, whereas this is an intentional two-party PKCS#11 regression test for stripping leading zeroes. Applying those demos would be unrelated, and Corretto's PR template directs non-Corretto-specific OpenJDK changes upstream. |
| 10 | `corretto/corretto-11` | The selected recommendation file is byte-for-byte identical to its original Stack Overflow block, so it contains no edit to apply. Corretto's `TimSort.sort` already contains the same algorithm; only inconsequential whitespace differs. |
| 11 | `googlesamples/io2015-codelabs` | The latest Stack Overflow revision adds `buildGoogleApiClient()` after `setContentView()`. The codelab already calls it from `onCreate` after initializing the button and geofence list, which is the required ordering for the complete application. |
| 12 | `jfxtras/jfxtras` | The Stack Overflow edit adds the `Stream` overload of `takeWhile`. `RecurrenceRuleValue` already contains that overload verbatim and uses it for recurrence processing. |
| 13 | `strapdata/elassandra` | The Stack Overflow edit changes a standalone odd/even `SynchronousQueue` example. `TaskExecutorTests` contains no integer handoff loop, `SynchronousQueue`, or `queue.put` construct; it tests Elasticsearch cluster-state task execution. |
| 15 | `ldoublem/loadingview` | The selected Stack Overflow block is unchanged and only shows attaching `RectProgressDrawable` to a `ProgressBar`. `LVSunSetView` is a separate custom sunset view with neither class. |
| 16 | `zhou-you/RxEasyHttp` | The Stack Overflow edit replaces an `InputStream` helper with a `ParcelFileDescriptor` helper. RxEasyHttp's public helper is specifically called for `InputStream` file wrappers; replacing it would break that supported path, while a new unused overload would not preserve the recommended replacement. |
| 17 | `camunda-consulting/code` | The selected edit adds `URL` and `URLClassLoader` imports. `SysoutClasspath` already contains both imports and uses both classes. |
| 18 | `cymcsg/UltimateAndroid` | The edited answer logs an `Elements` selection and stores `elem.text()` in a story list. `UtilsDemoActivity` only retrieves a document title and contains none of those constructs. |
| 19 | `eugenp/tutorials` | The selected `allChannels.writeAndFlush(getOut)` block is unchanged. The answer's actual edit concerns a `ChannelGroupFuture` listener for a console broadcast loop, while `NettyServer` has no `ChannelGroup`, console loop, or matching listener. |
| 20 | `puniverse/quasar` | Quasar already contains the shared stream-copy loop. The answer's latest edits add thread labels, progress printing, and file/pipe orchestration to a standalone PDF-copy demo; adding that console output to an internal bytecode utility would be unrelated. |
| 21 | `icyphy/ptii` | The selected code block is unchanged; the answer only adds prose about naming conventions. `LinAlg.transpose` already uses camelCase and `LinAlg` is PascalCase. |
| 22 | `alibaba/dragonwell8` | The selected edit adds only a `//create UI here` placeholder. `SwingApplet` already calls `initUI()` inside `invokeAndWait`, constructing the real UI. |
| 23 | `alibaba/atlas` | The selected edit changes an illustrative malformed URL. The substantive `decode()` placement fix is already present in `PathUtils.basedir`. |
| 24 | `klinker24/talon-for-twitter-android` | The selected block is unchanged, while the actual `buildGoogleApiClient()` recommendation is already present in `Compose.onCreate`. The repository is also archived. |
| 25 | `xwjie/PLMCodeTemplate` | The edited `/serverTime` method does not exist in the target controller; only the separate unchanged `home` handler is present. |
| 26 | `webjournal/journaldev` | The edited `/serverTime` method does not exist in the target controller; only the separate unchanged `home` handler is present. |
| 27 | `ibmruntimes/openj9-openjdk-jdk8` | The answer's actual edit customizes an `AbstractButton`; `MetalTabbedPaneUI.update` paints a tabbed pane's background, so the button cast and state logic do not apply. |
| 28 | `eugenp/tutorials` | The selected latest block is an incomplete body fragment. The answer's separate `getAllDays` helper returns a count, while Baeldung's utility intentionally returns dates and supports both addition and subtraction. |
| 29 | `matsim-org/matsim-libs` | The answer replaces the server with a Windows-specific hard-coded ZIP demo and matching protocol changes. Applying it would break MATSim's project-specific simulation-file server. |
| 30 | `alibaba/dragonwell8` | The selected recommendation is byte-for-byte unchanged, and Dragonwell's `TimSort.sort` already contains the same algorithm. |
| 31 | `reneargento/algorithms-sedgewick-wayne` | The socket-closing edit has no match: the target only sleeps between graph experiments and contains no `ServerSocket` or accept loop. |
| 32 | `corretto/corretto-8` | One generalized `create3ByteImage` helper serves both RGB and GRB layouts; renaming it to RGB-specific wording would misdescribe the GRB caller. |
| 33 | `vmware/singleton` | The target filters entries by directory status and has no allowed/not-allowed filename field or `testFolder` logic. |
| 34 | `EvoSuite/evosuite` | The Maven fixture contains only `isPositive(int)`; the edited prime method is absent. |
| 35 | `EvoSuite/evosuite` | The Gradle fixture contains only `isPositive(int)`; the edited prime method is absent. |
| 36 | `corretto/corretto-8` | The answer adds a UI placeholder comment, while `SwingApplet` already invokes its working `initUI()` implementation. |
| 37 | `corretto/corretto-8` | The shared hex helpers already match; the answer's added multi-party Diffie–Hellman demos are unrelated to keytool digest formatting. |
| 38 | `corretto/corretto-8` | The selected recommendation is unchanged; `ComparableTimSort` already contains the equivalent natural-ordering algorithm. |
| 39 | `corretto/corretto-8` | The selected recommendation is unchanged; `TimSort.sort` already contains the same algorithm. |
| 40 | `corretto/corretto-8` | The latest block is `Collection.addAll` code using `E` and `add(e)`, which cannot replace `HashMap.putMapEntries` using `K`, `V`, and `putVal`. |
| 41 | `corretto/corretto-8` | The selected Timer block is byte-for-byte unchanged. |
| 42 | `corretto/corretto-8` | The latest block is an incomplete internal divide branch; the public overload correctly delegates with `roundingMode.oldMode`. |
| 43 | `elastic/elasticsearch` | The edited `SynchronousQueue` loop is absent from cluster-state `TaskExecutorTests`. |
| 44 | `openjdk-mirror/jdk7u-jdk` | The shared hex helpers already match; multi-party DH demos are unrelated to the X500Name test. |
| 45 | `openjdk-mirror/jdk7u-jdk` | The shared helpers already match; the added multi-party demos do not apply to a two-party DH test. |
| 46 | `freeplane/freeplane` | The edit only adds Azure Blob RSS instructions, unrelated to precompiled-script metadata. |
| 47 | `SevenEx/bitrade-parent` | Working Spring CORS configuration is already present; the appended servlet-filter fallback would duplicate it. |
| 48 | `SevenEx/bitrade-parent` | Working Spring CORS configuration is already present; the appended servlet-filter fallback would duplicate it. |
| 49 | `ibmruntimes/openj9-openjdk-jdk8` | The selected Timer block is byte-for-byte unchanged. |
| 50 | `nativejdb/nativejdb` | The latest block is an incomplete internal divide fragment and cannot replace the complete public overload fixture. |
| 51 | `processing/processing4` | The answer replaces a standalone autocomplete example wholesale; `CompletionPanel` has different project-specific APIs and no isolated matching edit. |
| 52 | `atjiu/pybbs` | `BCryptPasswordEncoder.encode` already matches the latest implementation. |
| 53 | `corretto/corretto-8` | The hex helpers already match; multi-party demos do not apply to the two-party provider test. |
| 54 | `wupeixuan/JDKSourceCode1.8` | The latest block is an incomplete internal divide fragment and cannot replace the complete public overload. |
| 55 | `evgenyzinoviev/gravitydefied` | The OpenCV ROI edit has no match in `LevelsManager`. |
| 57 | `yangzongzhuan/RuoYi-Vue` | Working Spring CORS configuration is already present; a servlet-filter fallback would duplicate it. |
| 58 | `phoenixctms/ctsms` | The recommendation uses Jersey 2 APIs, while this project depends on Jersey 1 response-filter interfaces. |
| 59 | `kontalk/desktopclient-java` | `TextLimitDocument` already matches the latest simplified implementation. |
| 60 | `pkainulainen/spring-data-jpa-examples` | The edit only removes Markdown backticks; the target is already ordinary Java source. |
| 61 | `pkainulainen/spring-data-jpa-examples` | The edit only removes Markdown backticks; the target is already Java source. |
| 62 | `pkainulainen/spring-data-solr-examples` | The edit only removes Markdown backticks; the target is already Java source. |
| 63 | `openjdk-mirror/jdk7u-jdk` | The UI placeholder is already represented by the working `initUI()` call. |
| 64 | `apache/netbeans` | The standalone mouse-wheel demo has no matching construct in `SecurityMultiViewElement`. |
| 65 | `wupeixuan/JDKSourceCode1.8` | A custom `BasicTableHeaderUI` subclass cannot be inserted into the base implementation itself. |
| 66 | `dragome/dragome-sdk` | The selected TimSort block is unchanged and the equivalent algorithm is already present. |
| 67 | `eclipse-ee4j/jersey` | The Swing table-threading example is absent from Jersey's `ParallelTest`. |
| 68 | `ronancpl/HeavenMS` | The fix applied, but GitHub rejected the PR because the upstream repository is archived and read-only. |
| 70 | `qiurunze123/threadandjuc` | The networking/client-limit loop is absent from the JVM field-visibility example. |
| 71 | `TheAlgorithms/Java` | The three-way merge recommendation does not apply to the distinct bitonic-sort algorithm. |
| 72 | `openjdk-mirror/jdk7u-jdk` | The shared hex helper already matches; DH demos are unrelated to Blowfish vectors. |
| 73 | `bcgit/bc-java` | The target has no matched custom array-comparison helper. |
| 74 | `leapframework/framework` | The selected latest block is a sample BCrypt hash, not Java code. |
| 75 | `shulietech/linkagent` | The shared copy loop exists; demo-specific progress logging is inappropriate for compression internals. |
| 76 | `prometheus/client_java` | The file-flattening example has no match in the volume metadata test. |
| 77 | `geoodk/collect` | The target uses separate control-character and whitespace filters, not the edited alphanumeric filter. |
| 78 | `huawei-hadoop/hindex` | The generic Mockito answer's exception field/getter is absent from the specific coordinator callback. |

## Candidate 1 record

- Stack Overflow answer: <https://stackoverflow.com/a/40056844>
- Recommendation: add same-reference and null handling before comparing arrays.
- Target: `util/src/test/java/org/bouncycastle/asn1/smime/test/SMIMETest.java`.
- Implementation: preserved the existing `isSameAs` helper and added the identity and null checks in the same order as the latest Stack Overflow revision.
- Branch: `matcha-update-smime-array-comparison`.
- Commit: `e9aaa7a2c`.
- Result: pull request opened by `cragkhit` against `bcgit/bc-java:main`.
- Bouncy Castle's contribution guide requires disclosure of generative-AI assistance, so the PR includes that disclosure in addition to the Matcha study and IRB text.
