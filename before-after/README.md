# Before–After Code Quality Evaluation

## Motivation

This analysis is intended to address the following reviewer concern:

> R5.10 10) The manuscript presents descriptive observations rather than strong
> empirical evidence that the proposed method consistently improves software
> quality.

The proposed study compares measurable code-quality indicators for each Stack
Overflow snippet between its original and recent versions.

The defensible claim should be:

> Recent revisions are associated with improvements in selected static
> indicators of maintainability, reliability, and readability.

This analysis cannot, by itself, prove that every revision improves overall
software quality in deployed projects.

## General approach

Do not reduce code quality to one opaque score. Use a paired, multi-measure
analysis in which every original Stack Overflow snippet is directly compared
with its corresponding recent version.

## Analysis scope

The analysis will include **only study pairs whose `Final Manual Validation`
value is exactly `Agress Yes`** in
[`dataset/study_pair_mapping.csv`](dataset/study_pair_mapping.csv). `Agress
Yes` is intentionally written here exactly as it appears in the source data.
Rows marked `Agree No` are outside the scope of this analysis.

After applying that decision filter, only rows with `Dataset Status` equal to
`ELIGIBLE` are retained. Because several GitHub matches can refer to the same
Stack Overflow revision history, the retained rows are deduplicated by
`Snippet ID` before PMD or statistical analysis. This prevents a frequently
matched snippet from receiving disproportionate weight.

The prepared mapping currently contains 391 `Agress Yes` study rows. Of these,
387 are dataset-eligible and represent 125 unique Stack Overflow before–after
snippet pairs. The four excluded rows must remain in the exclusion report and
must not enter the quality calculations.

For convenience, the 125 deduplicated pairs are copied without source
modification into:

- `dataset/before_agress_yes/`
- `dataset/after_agress_yes/`

Their audit manifest is `dataset/agress_yes_pairs.csv`. Rebuild these scoped
folders only from the original source corpus if regeneration is required. The
broader generated `dataset/before/` and `dataset/after/` folders have been
removed because they are outside the final analysis scope.

The recommended implementation order is:

1. JavaParser normalization and metric extraction.
2. A curated PMD ruleset.
3. Selected Checkstyle readability checks.
4. Paired statistical analysis across eligible `Agress Yes` pairs.
5. Separate analyses for Bug Fixing and Improving Code.
6. SpotBugs sensitivity analysis on symmetrically compilable pairs.
7. Manual auditing of a random sample of analyzer findings.

## Recommended tools

### 1. JavaParser

Use JavaParser as the foundation for parsing, normalization, and intrinsic
metrics. It creates an abstract syntax tree that can be inspected
programmatically.

Documentation:

- <https://javaparser.org/inspecting-an-ast/>
- <https://javaparser.org/getting-started.html>

Compute the following for both the original and recent snippets:

- Non-comment lines of code
- Number of statements
- Cyclomatic complexity
- Cognitive-complexity approximation
- Maximum nesting depth
- Number of branches
- Number of loops
- Number of `catch` blocks
- Number of abrupt exits
- Method length
- Number of parameters
- Number of local variables
- Number of duplicated or empty branches
- Number of empty `catch` blocks
- Resource-handling constructs:
  - `try`/`finally`
  - Try-with-resources
  - Explicit `close()`
- Null-check count
- Exception-handling count
- Parse success or failure

Record how every snippet was parsed:

1. Complete compilation unit
2. Class member
3. Method
4. Method body
5. Statement sequence
6. Unparseable

Use the same synthetic wrapper for both versions of a pair.

### 2. PMD

PMD should be the primary static analyzer because it operates on Java source
and uses AST-based rules. It provides maintainability measures and
error-prone, design, performance, and best-practice checks.

Documentation:

- <https://pmd.github.io/pmd/pmd_rules_java.html>
- <https://pmd.github.io/pmd/pmd_userdocs_cli_reference.html>
- <https://pmd.github.io/pmd/pmd_userdocs_installation.html>

Use a fixed custom ruleset selected before examining the results. Candidate
categories and rules include:

- `category/java/errorprone.xml`
- `category/java/bestpractices.xml`
- Selected `design` rules
- Selected `performance` rules
- `CognitiveComplexity`
- `CyclomaticComplexity`
- `NPathComplexity`
- `AvoidDeeplyNestedIfStmts`
- `ExcessiveMethodLength`
- `EmptyCatchBlock`
- `CloseResource`
- `PreserveStackTrace`
- `AvoidCatchingGenericException`
- `SimplifyBooleanReturns`
- `CollapsibleIfStatements`
- `UselessOperationOnImmutable`
- `AvoidDuplicateLiterals`, when meaningful for the snippet size

Do not enable every available PMD rule. Naming, documentation, framework, and
project-architecture rules may be inappropriate for isolated snippets.

For each version, record:

- Total violations
- Violations by rule
- Violations by category
- Violations by priority
- Violations per 100 non-comment lines
- Whether every violation was introduced, preserved, or removed

Type resolution may be improved with an auxiliary classpath. Many
snippet-level checks can still operate without one.

### 3. Checkstyle

Use Checkstyle only for a separately named style/readability dimension.

Suitable checks include:

- Indentation
- Whitespace
- Empty blocks
- Need braces
- One statement per line
- Avoid nested blocks
- Line length
- Variable declaration usage distance
- Cyclomatic complexity

Documentation:

- <https://checkstyle.org/checks/metrics/index.html>
- <https://checkstyle.org/checks/metrics/cyclomaticcomplexity.html>

Do not treat style compliance as overall software quality. Checkstyle itself
notes that cyclomatic complexity measures paths and testing difficulty rather
than code quality directly.

Keep PMD and Checkstyle findings separate to avoid double-counting equivalent
rules.

### 4. SpotBugs

Use SpotBugs only for the subset in which both versions compile. SpotBugs
analyzes bytecode rather than arbitrary source fragments.

Documentation:

- <https://spotbugs.readthedocs.io/en/stable/introduction.html>
- <https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html>

For eligible pairs:

1. Place the snippets in compilable classes.
2. Supply required imports and dependencies without changing the snippets.
3. Compile original and recent versions under identical conditions.
4. Run SpotBugs on both versions.
5. Compare correctness, bad-practice, concurrency, performance, and security
   findings.

Report coverage explicitly, for example:

```text
SpotBugs was applicable to 148 of 793 pairs.
```

Do not analyze only one version when the other version fails to compile. Both
versions must have the same analysis eligibility.

### 5. SonarQube

SonarQube or SonarJava can be used as an optional project-level replication,
but it should not be the primary snippet analyzer. Java analysis often depends
on compiled classes and correct dependency information.

Documentation:

- <https://docs.sonarsource.com/sonarqube-server/9.9/analyzing-source-code/languages/java>

For a smaller set of complete projects:

1. Analyze the project containing the original snippet.
2. Replace it with the recent snippet.
3. Analyze the same project again.
4. Compare issues and metrics attributable to the changed lines.
5. Keep every other part of the project constant.

## Quality dimensions

ISO/IEC 25010:2023 provides a current software-product quality model:

- <https://www.iso.org/standard/78176.html>

The proposed static analysis covers only part of that model.

| Dimension | Measures |
| --- | --- |
| Maintainability | Cognitive complexity, cyclomatic complexity, nesting depth, method length, PMD design findings |
| Reliability | PMD error-prone findings, null handling, exception handling, resource leaks |
| Security | Security-related PMD or SpotBugs findings where applicable |
| Performance efficiency | PMD performance findings |
| Readability/style | Checkstyle violations and selected structural indicators |
| Analyzability | Parse success, complexity, nesting, and statement count |

Do not claim to measure the following without additional evidence:

- Functional suitability without behavioral tests
- Runtime performance without benchmarks
- Compatibility
- Usability
- Operational reliability
- Overall product quality

## Primary outcomes

Define all primary outcomes before running the experiment.

### Outcome 1: Static-analysis issue density

```text
issue density = applicable PMD violations / non-comment LOC × 100
```

For each pair:

```text
Δ issue density = recent issue density − original issue density
```

A negative delta indicates improvement.

Retain raw violation counts as well. Density can decrease merely because a
revision adds lines.

### Outcome 2: Complexity

For the corresponding method or code block:

```text
Δ cognitive complexity = recent − original
Δ cyclomatic complexity = recent − original
Δ maximum nesting depth = recent − original
```

Lower complexity often indicates simpler code, but an increase is not
necessarily harmful when a bug fix adds necessary validation, boundary checks,
or exception handling.

### Outcome 3: Defect-pattern changes

For every PMD rule:

```text
removed    = present in original, absent in recent
introduced = absent in original, present in recent
preserved  = present in both
```

This supports interpretable observations such as:

> Recent revisions removed 64 resource-management violations and introduced
> 11.

### Outcome 4: Parse and compilation outcomes

Record the following transitions:

- Fails → succeeds
- Succeeds → succeeds
- Succeeds → fails
- Fails → fails

A revision that makes intrinsically invalid code parseable or compilable
provides strong evidence, as long as the original failure was not caused by the
synthetic wrapper.

## Use the manual validation as the inclusion criterion

The manual-validation data contains:

- 391 accepted recommendations
- 402 rejected recommendations
- Accepted recommendations:
  - 334 Improving Code
  - 57 Bug Fixing

For this study, retain only the 391 recommendations recorded as `Agress Yes`;
do not use the 402 `Agree No` recommendations as a comparison group. After
dataset eligibility filtering and deduplication by `Snippet ID`, run the
paired original-versus-recent analysis on the resulting 125 unique snippet
histories.

Also analyze the following independently:

- Improving Code
- Bug Fixing

Bug fixes can legitimately increase lines of code and complexity by adding
validation, exception handling, or boundary checks. Combining them with
refactoring-oriented changes could hide meaningful effects.

## Statistical analysis

Each original snippet is paired with its recent version.

For every numerical metric, report:

- Original median and interquartile range
- Recent median and interquartile range
- Median paired change
- Percentage improved
- Percentage unchanged
- Percentage worsened
- 95% bootstrap confidence interval for the change
- Wilcoxon signed-rank test
- Paired effect size, such as rank-biserial correlation

The Wilcoxon signed-rank test is designed for related paired samples:

- <https://docs.scipy.org/doc/scipy/reference/generated/scipy.stats.wilcoxon.html>

Because many pairs may have zero change, also report a paired sign or
permutation analysis:

- <https://docs.scipy.org/doc/scipy/reference/generated/scipy.stats.permutation_test.html>

For binary findings such as the presence of an empty `catch` block, report:

- Four-cell transition table
- McNemar's exact test
- Risk difference with a confidence interval

Correct for multiple testing with Holm's procedure. Designate two or three
primary outcomes so that the study does not become a search for statistically
significant results.

## Handling incomplete snippets

Snippet handling is likely to be the most important validity issue.

For each pair:

1. Identify the corresponding code block in both revisions.
2. Remove Markdown without changing Java.
3. Parse the smallest valid unit.
4. If necessary, place both versions in identical synthetic wrappers.
5. Add only declarations required for parsing.
6. Keep wrapper code out of every measurement.
7. Record every injected import, stub, variable, or enclosing declaration.
8. Require the same analysis eligibility for both versions.
9. Report every exclusion reason.

Suggested eligibility columns:

```text
Original Parse Status
Recent Parse Status
Common Parse Mode
Wrapper Type
Wrapper Added LOC
PMD Eligible
Checkstyle Eligible
Compilation Eligible
SpotBugs Eligible
Exclusion Reason
```

Do not silently discard unparseable pairs. Report the complete analysis flow:

```text
793 pairs identified
742 parsed symmetrically
701 eligible for PMD
683 eligible for complexity analysis
129 compiled symmetrically
118 eligible for SpotBugs
```

## Avoid a single unvalidated score

Do not calculate a weighted score such as:

```text
quality = 0.4 × complexity + 0.3 × PMD + 0.3 × Checkstyle
```

The weights would be arbitrary, and violations from different rules have
different meanings.

Report a quality profile with a small number of predefined outcomes instead.
If the manuscript needs a single summary outcome, use an interpretable ordinal
classification:

```text
Improved:
  At least one primary indicator improved and no primary indicator worsened.

Worsened:
  At least one primary indicator worsened and none improved.

Mixed:
  Both improvement and worsening occurred.

Unchanged:
  No primary indicator changed.
```

This summary should supplement rather than replace the individual metrics.

## Reproducibility requirements

- Pin exact versions of Java, JavaParser, PMD, Checkstyle, and SpotBugs.
- Store the complete analyzer configuration and custom rulesets.
- Run original and recent versions with identical tool configurations.
- Preserve raw analyzer reports.
- Record all exclusions and parsing adaptations.
- Do not manually repair only one version of a pair.
- Use deterministic wrappers and generated filenames.
- Publish scripts, rule configurations, metric definitions, and statistical
  notebooks with the replication package.
- Manually audit a random sample of analyzer results and report agreement.

## Recommended conclusion scope

A positive result would support a conclusion such as:

> Compared with their original versions, recent Stack Overflow revisions
> exhibit statistically significant improvements in selected static
> indicators of maintainability and reliability, with the strongest changes
> among recommendations independently accepted during manual validation.

Avoid a broader claim that the method consistently improves overall software
quality unless it is additionally supported by project-level tests, runtime
measurements, defect outcomes, and downstream maintenance evidence.

## Prepared dataset

The paired source dataset is available under [`dataset/`](dataset/). It
contains 205 eligible, deduplicated Stack Overflow before–after pairs and a
manifest mapping all 793 original study rows to those unique snippet
histories. See [`dataset/README.md`](dataset/README.md) for selection rules,
counts, exclusions, and rebuild instructions.

## PMD analysis notebook

Run [`PMD_Before_After_Analysis.ipynb`](PMD_Before_After_Analysis.ipynb) from
this directory. The notebook installs its Python analysis dependencies and
uses PMD 7.25.0. If that PMD version is not already available through
`PMD_HOME` or `PATH`, the notebook downloads the official binary release into
`tools/`.

```bash
cd before-after
python3 -m jupyter lab PMD_Before_After_Analysis.ipynb
```

Run all cells in order. Before results are used for the study, the notebook's
data-loading stage must enforce the scope above. The scoped PMD run:

1. filters `dataset/study_pair_mapping.csv` to eligible rows whose
   `Final Manual Validation` value is exactly `Agress Yes`, then deduplicates
   them by `Snippet ID`;
2. tries each pair symmetrically as raw Java, a class member, and a method
   body;
3. retains only pairs for which both versions share a successful parse mode;
4. removes wrapper-only findings and restores source-relative line numbers;
5. compares PMD violations per 100 non-comment code lines;
6. reports paired Wilcoxon tests, effect sizes, rule-level transitions, and
   Bug Fixing/Improving Code subgroup results; and
7. writes raw reports, exclusions, tables, plots, and a reproducibility
   summary under `results/pmd/`.

The fixed analyzer configuration is
[`pmd-ruleset.xml`](pmd-ruleset.xml). Rebuild the notebook after editing its
generator with:

```bash
python3 build_pmd_notebook.py
```
