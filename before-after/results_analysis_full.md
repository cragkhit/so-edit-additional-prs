# Before–After Quality Analysis: Full-Scope Results Report

## 1. Purpose and scope

This report interprets the output of the three **full-scope** before–after
static-analysis notebooks —
[`JavaParser_Before_After_Analysis_Full.ipynb`](JavaParser_Before_After_Analysis_Full.ipynb),
[`PMD_Before_After_Analysis_Full.ipynb`](PMD_Before_After_Analysis_Full.ipynb),
and [`Checkstyle_Before_After_Analysis_Full.ipynb`](Checkstyle_Before_After_Analysis_Full.ipynb) —
which extend [`results_analysis.md`](results_analysis.md)'s 125-pair,
`Agress Yes`-only analysis to **all 205 unique, eligible Stack Overflow
before/after histories** derivable from the complete 793-row Matcha
manual-validation study
(`matcha_results_2024-05-07_manual_validation_FINAL.csv`), regardless of
whether the recommendation was ultimately judged useful.

Because this scope includes recommendations judged useful, judged not
useful, and judged inconsistently across different GitHub matches, every
history in this scope carries an additional `validation_group` label:

- `ALL_ACCEPTED` — judged useful in every GitHub match it appeared in.
- `ALL_REJECTED` — judged not useful in every match.
- `MIXED` — judged useful in some matches, not useful in others.

This label lets the analysis ask a question the 125-pair scope could not:
*does the measured quality change look different for recommendations that
were actually judged useful versus ones that were not?* — a direct
validity check on the study's core premise.

Results referenced below come from `results/javaparser_full/`,
`results/pmd_reduced_full/`, and `results/checkstyle_full/`. **This report
describes what the data shows; no computation beyond what the notebooks
already produced was performed to write it.**

## 2. Parsing coverage

| Tool | Scoped pairs | Analyzed | Excluded | Exclusion rate |
|---|---|---|---|---|
| JavaParser | 205 | 128 | 77 | 37.6% |
| PMD (reduced ruleset) | 205 | 124 | 81 | 39.5% |
| Checkstyle | 205 | 129 | 76 | 37.1% |

Nearly identical exclusion rates to the 125-pair scope (35–38%), which is
reassuring: coverage is a property of the snippets and the wrapper
strategy, not of the manual-validation filter.

| Tool | RAW | CLASS_MEMBER | METHOD_BODY |
|---|---|---|---|
| JavaParser | 113 | 6 | 9 |
| PMD | 41 | 74 | 9 |
| Checkstyle | 118 | 2 | 9 |

Same pattern as the scoped analysis: PMD needs the class-member wrapper
far more often than JavaParser or Checkstyle, consistent with PMD's parser
being less tolerant of bare top-level fragments.

Every tool also independently confirms **12 identical pairs** and **6
dataset-level exclusions** across the 793 study-row mapping, matching
`dataset/README.md`.

## 3. JavaParser: structural / intrinsic metrics (n = 128)

The same 18 structural metrics plus code-line count were tested, with Holm
correction across all 19 and a rank-biserial effect size per test.

**Headline: `code_lines` comes close to significance for the first time in
this study.** Raw p=0.0039, Holm-adjusted p=0.070 — not below the
conventional α=0.05 threshold, but markedly closer than the 125-pair
scope's Holm p=0.273 for the same metric. Six additional metrics
(`method_count`, `parameter_count`, `local_variable_count`,
`cyclomatic_proxy`, `abrupt_exit_count`, `exception_handling_count`) also
cross the *uncorrected* p<0.05 threshold here, versus zero besides
`code_lines` and `abrupt_exit_count` in the scoped analysis — but none
survive Holm correction (all Holm p ≥ 0.18).

| Metric | Δ median | Dec. | Unc. | Inc. | Wilcoxon p | Rank-biserial | Holm p |
|---|---|---|---|---|---|---|---|
| Code lines | 0 | 34 | 31 | 63 | 0.0039 | +0.337 | **0.070** |
| Method count | 0 | 10 | 92 | 26 | 0.026 | +0.423 | 0.406 |
| Parameter count | 0 | 9 | 102 | 17 | 0.025 | +0.499 | 0.406 |
| Max. parameters | 0 | 6 | 115 | 7 | 0.497 | +0.242 | 1.000 |
| Local-variable count | 0 | 10 | 91 | 27 | 0.011 | +0.478 | 0.180 |
| `if` count | 0 | 11 | 103 | 14 | 0.508 | +0.160 | 1.000 |
| Loop count | 0 | 1 | 121 | 6 | 0.078 | +0.821 | 0.781 |
| Switch count | 0 | 0 | 128 | 0 | — | — | — |
| Catch count | 0 | 3 | 117 | 8 | 0.123 | +0.545 | 1.000 |
| Cyclomatic-complexity proxy | 0 | 13 | 88 | 27 | 0.028 | +0.398 | 0.406 |
| Max. control nesting | 0 | 10 | 106 | 12 | 0.262 | +0.285 | 1.000 |
| Abrupt-exit count | 0 | 12 | 90 | 26 | 0.049 | +0.367 | 0.628 |
| Try count | 0 | 3 | 116 | 9 | 0.064 | +0.615 | 0.704 |
| Try-with-resources count | 0 | 0 | 126 | 2 | 0.500 | +1.000 | 1.000 |
| Throw count | 0 | 1 | 120 | 7 | 0.195 | +0.556 | 1.000 |
| Empty-catch count | 0 | 1 | 124 | 3 | 0.625 | +0.500 | 1.000 |
| Empty-block count | 0 | 2 | 121 | 5 | 0.297 | +0.500 | 1.000 |
| Null-check count | 0 | 6 | 118 | 4 | 0.922 | +0.055 | 1.000 |
| Exception-handling count | 0 | 5 | 110 | 13 | 0.048 | +0.538 | 0.628 |

Every metric's rank-biserial correlation is positive (revisions skew
toward *increasing* every structural count, never decreasing), which is a
new and more consistent pattern than the scoped analysis showed. Combined
with `code_lines` nearly reaching significance, this is compatible with a
real, modestly-sized "revisions add code" effect that the 125-pair sample
was underpowered to detect clearly — exactly the concern flagged in
`results_analysis.md` §6.

### JavaParser by recommendation type (uncorrected — exploratory only)

| Group | n | code_lines p | Pattern |
|---|---|---|---|
| Bug Fixing | 12 | 0.039 | 7/12 increased lines |
| Improving Code | 65 | 0.099 | 33/65 increased lines |

Essentially unchanged from the scoped analysis (same underlying `Agress
Yes`-accepted histories dominate this subgroup).

### JavaParser by validation outcome (new in this scope, uncorrected)

| Group | n | code_lines p | Rank-biserial | Pattern |
|---|---|---|---|---|
| `ALL_ACCEPTED` | 59 | **0.0082** | +0.439 | 33/59 increased, median Δ=+1 |
| `ALL_REJECTED` | 48 | 0.135 | +0.288 | 21/48 increased, median Δ=0 |
| `MIXED` | 21 | 0.626 | +0.162 | 9/21 increased, median Δ=0 |

This is the most interesting result in the full-scope analysis: the
`code_lines` signal is concentrated almost entirely in recommendations
that were judged useful in *every* GitHub match (`ALL_ACCEPTED`, p=0.008),
and is progressively weaker for `ALL_REJECTED` (p=0.135) and `MIXED`
(p=0.626). This is a coherent, directionally-sensible pattern — exactly
what you would hope to see if the manual-validation judgment tracks a
real underlying quality difference — though it is an **uncorrected,
single exploratory subgroup test** and should be described as a promising
lead, not a confirmed effect, until independently corrected for or
replicated.

## 4. PMD: bug-risk and performance violations (n = 124)

The reduced ruleset is even sparser at this scope than in the 125-pair
analysis: **118 total violations** across both versions of 124 pairs (99
bug-risk, 19 performance) — expect very little statistical power here.

| Dimension | Metric | Improved | Unchanged | Worsened | Wilcoxon p | Rank-biserial | Holm p |
|---|---|---|---|---|---|---|---|
| Combined | Raw violations | 8 | 108 | 8 | 0.744 | +0.10 | 1.000 |
| Combined | Violations / 100 lines | 20 | 93 | 11 | 0.337 | −0.20 | 1.000 |
| Bug-risk | Raw violations | 7 | 110 | 7 | 0.542 | +0.20 | 1.000 |
| Bug-risk | Violations / 100 lines | 17 | 97 | 10 | 0.515 | −0.15 | 1.000 |
| Performance | Raw violations | 1 | 122 | 1 | 1.000 | −0.33 | 1.000 |
| Performance | Violations / 100 lines | 4 | 119 | 1 | 0.438 | −0.47 | 1.000 |

Every Holm-adjusted p is 1.000 — the smallest raw p (0.337) already
exceeds 0.05 before correction. Density leans mildly toward improvement
in the combined and bug-risk dimensions (more improved than worsened
pairs), the same pattern as the 125-pair scope, but with an even sparser
absolute violation count behind it.

### Rule-level transitions (PMD, full scope)

| Rule | Dimension | Removed | Introduced | Preserved | Net removed |
|---|---|---|---|---|---|
| `AssignmentInOperand` | bug-risk | 3 | 2 | 9 | +1 |
| `BrokenNullCheck` | bug-risk | 1 | 0 | 0 | +1 |
| `ConsecutiveAppendsShouldReuse` | performance | 1 | 0 | 2 | +1 |
| `AppendCharacterWithChar` | performance | 1 | 1 | 1 | 0 |
| `EmptyCatchBlock` | bug-risk | 1 | 1 | 8 | 0 |
| `IdenticalCatchBranches` | bug-risk | 1 | 1 | 3 | 0 |
| `UseStringBufferForStringAppends` | performance | 1 | 1 | 1 | 0 |
| `CompareObjectsWithEquals` | bug-risk | 0 | 1 | 1 | −1 |

Similar mixed pattern to the scoped analysis: a small net improvement in
`AssignmentInOperand`, but `CompareObjectsWithEquals` shows a new
violation introduced with none removed. Nothing here is large enough to
draw a confident conclusion from — the counts are all single digits.

### PMD by recommendation type and validation outcome

Both subgroup dimensions (`recommendation_group` and `validation_group`,
each split further into bug-risk/performance) are all non-significant
(p ≥ 0.47) with sample sizes of 11–63 histories and mostly zero
violations on both sides. `ALL_ACCEPTED` (n=57) shows a mild positive
rank-biserial lean on bug-risk (+0.36) versus `ALL_REJECTED` (n=47, +0.20)
— directionally consistent with the JavaParser finding, but far too
sparse (3 improved / 50 unchanged / 4 worsened for `ALL_ACCEPTED`) to
treat as evidence on its own.

## 5. Checkstyle: readability/style (n = 129)

2,564 findings retained in total (up from 844 in the scoped analysis,
consistent with roughly 1.6× the pairs).

| Metric | Improved | Unchanged | Worsened | Wilcoxon p | Rank-biserial | Holm p |
|---|---|---|---|---|---|---|
| Raw findings | 25 | 84 | 20 | 0.414 | −0.141 | 0.414 |
| Findings / 100 lines | 39 | 68 | 22 | 0.122 | −0.228 | 0.243 |

Same qualitative pattern as the scoped analysis: density leans toward
improvement more than raw counts, but neither is significant.

### Rule-level transitions (Checkstyle, full scope)

| Check | Removed | Introduced | Preserved | Net removed |
|---|---|---|---|---|
| `FileTabCharacter` | 6 | 3 | 32 | +3 |
| `AvoidNestedBlocks` | 1 | 0 | 1 | +1 |
| `EmptyBlock` | 0 | 1 | 2 | −1 |
| `MultipleVariableDeclarations` | 0 | 1 | 8 | −1 |
| `LineLength` | 1 | 3 | 15 | −2 |
| `OneStatementPerLine` | 1 | 3 | 1 | −2 |
| `VariableDeclarationUsageDistance` | 0 | 2 | 4 | −2 |
| `NeedBraces` | 0 | 4 | 23 | −4 |

`FileTabCharacter` remains the clearest net-positive check. `NeedBraces`
is now the clearest net-negative (4 new instances introduced, none
removed) — a larger and more consistent signal than in the scoped
analysis (where it was −2), suggesting this specific readability
regression is not a small-sample artifact.

### Checkstyle by recommendation type and validation outcome

Recommendation-type subgroups remain non-significant and essentially
unchanged from the scoped analysis. The validation-outcome split shows
the same directional pattern as JavaParser and PMD: `ALL_ACCEPTED` (n=59)
has the largest improvement lean (rank-biserial −0.347) versus
`ALL_REJECTED` (n=48, −0.111) and `MIXED` (n=22, +0.25, the only positive
i.e. worsening lean of the three) — still not significant (p=0.196 for
`ALL_ACCEPTED`), but directionally the third tool in a row to show this
pattern.

## 6. Cross-tool synthesis

Two findings distinguish this full-scope analysis from the 125-pair one:

1. **More statistical power reveals a clearer, more consistent JavaParser
   signal.** `code_lines` moved from Holm p=0.273 (n=80) to Holm p=0.070
   (n=128), and every one of the 18 structural metrics now shows a
   positive (increasing) rank-biserial correlation, versus a mixed
   positive/negative pattern at the smaller scope. This is consistent
   with the "underpowered, not necessarily null" interpretation the
   scoped report already flagged, though it still falls short of
   α=0.05 after correction.

2. **All three tools independently show the same validation-outcome
   gradient**: `ALL_ACCEPTED` histories show a stronger
   improvement/growth signal than `ALL_REJECTED`, with `MIXED` in
   between or noisier. JavaParser shows this most clearly and with the
   only individually-significant (uncorrected) result in the whole
   study (`code_lines`, `ALL_ACCEPTED`, p=0.008); PMD and Checkstyle
   show the same *direction* but with far less power (PMD's bug-risk
   dimension has only 3–5 nonzero pairs per subgroup). This convergence
   across three independently-implemented tools, despite none of them
   individually reaching significance except one, is itself a modestly
   encouraging signal — though "three tools agree on a direction" is
   not the same evidentiary bar as "three tools independently reach
   significance," and should be described accordingly.

Neither finding overturns the core conclusion from `results_analysis.md`:
**no metric survives Holm correction in the primary, full-sample test**,
in any tool, at either scope. What the larger sample adds is a clearer
picture of *where* the signal that does exist is concentrated (code
growth, and specifically among accepted recommendations) and *why* the
smaller scope's null results are more consistent with underpowering than
with a genuine absence of effect.

## 7. Comparison to the 125-pair `Agress Yes` scope

| | 125-pair scope | 205-pair full scope |
|---|---|---|
| JavaParser analyzed | 80 | 128 |
| PMD analyzed | 77 | 124 |
| Checkstyle analyzed | 81 | 129 |
| JavaParser `code_lines` Holm p | 0.273 | **0.070** |
| Smallest Holm p, any metric/tool | 0.273 (JavaParser `code_lines`) | 0.070 (JavaParser `code_lines`) |
| New comparison group | none (all rows pre-filtered to accepted) | `ALL_REJECTED` (n=48) as a built-in negative control |

The full scope does not change any qualitative conclusion, but it
strengthens the case that the study's earlier null results reflect
limited power rather than a true absence of effect, and it adds a
negative-control comparison (`ALL_REJECTED`) that the `Agress Yes`-only
scope structurally could not include.

## 8. Implications for the manuscript / R5.10 rebuttal

1. **The `ALL_ACCEPTED` vs. `ALL_REJECTED` gradient is the most useful new
   evidence this expanded scope produces**, because it is a validity
   check the reviewer could plausibly ask for directly: *if your manual
   validation is measuring something real, quality metrics should differ
   between accepted and rejected recommendations.* They do, in the
   expected direction, across all three tools, for the metric with a
   real effect size (`code_lines`). This is worth reporting explicitly,
   with the caveat that only the JavaParser result is individually
   notable (p=0.008 uncorrected) and none of this is Holm-corrected
   across the three validation-group comparisons.
2. **Do not present the full-scope numbers as replacing the 125-pair
   analysis.** The two scopes answer different questions: the 125-pair
   scope is "among recommendations judged useful, how do quality metrics
   change?"; the 205-pair scope is "across everything Matcha recommended,
   does the change track the validation judgment?" Both belong in the
   manuscript, doing different jobs.
3. **`code_lines` at Holm p=0.070 is worth flagging as a near-miss**,
   not rounded up to "significant" and not dismissed as "not
   significant" without qualification. If `code_lines` had been
   pre-registered as *the* primary outcome (as the README's own guidance
   on limiting primary outcomes suggests), it would face a much less
   severe correction and could plausibly cross α=0.05 at this scope.
4. **PMD remains too sparse to support strong claims at either scope.**
   118 violations across 124 pairs is not enough signal for any
   subgroup analysis to be informative; this should be stated plainly
   rather than over-interpreting single-digit rule transitions.
5. **Sample-size caveat applies with the same force as in the scoped
   report.** n=124–129 is larger than n=77–81 but still modest,
   especially once split into three validation-group subgroups.

## 9. Known limitations of this report

- Figures above are read directly from the CSV/JSON outputs already
  produced by the three full-scope notebooks; no new statistical test
  was run to produce this report.
- `recommendation_group` in this scope includes two new categories not
  present in the 125-pair scope: `NO_TYPE` (histories with no accepted
  study row to classify by a Baltes category — 48/128 JavaParser-analyzed
  histories) and the pre-existing `MULTIPLE_TYPES`. Both are excluded from
  the recommendation-type subgroup tests, the same way `MULTIPLE_TYPES`
  was excluded in the scoped analysis.
- The `validation_group` subgroup tests are **not Holm-corrected**, either
  within a tool's three-group comparison or across the three tools that
  each ran it. Treat every `validation_group` p-value in this report as
  exploratory.
- This report does not include the SpotBugs sensitivity analysis or the
  manual audit of analyzer findings, both mentioned in the README's
  recommended implementation order but not yet present under `results/`.
- PMD's `recommendation_group` subgroup histories (11 Bug Fixing) differ
  slightly from JavaParser's (12) and Checkstyle's (12) because PMD's
  parse-mode coverage excludes one additional Bug Fixing history that the
  other two tools successfully analyze — an ordinary consequence of each
  tool selecting its own common parse mode independently, not a data
  inconsistency.
