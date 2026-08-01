# Before–After Quality Analysis: Results Report

## 1. Purpose and scope

This report interprets the output of the three before–after static-analysis
notebooks in this folder — [`JavaParser_Before_After_Analysis_Readable.ipynb`](JavaParser_Before_After_Analysis_Readable.ipynb),
[`PMD_Before_After_Analysis.ipynb`](PMD_Before_After_Analysis.ipynb), and
[`Checkstyle_Before_After_Analysis.ipynb`](Checkstyle_Before_After_Analysis.ipynb) —
against their stated goal in [`README.md`](README.md): address reviewer
comment **R5.10** on the *2026 Matcha PLOS One* manuscript, which asks for
"strong empirical evidence that the proposed method consistently improves
software quality" rather than descriptive observation alone.

All three notebooks analyze the same 125 unique, dataset-eligible
Stack Overflow before/after histories whose `Final Manual Validation` is
exactly `Agress Yes` (see [`dataset/README.md`](dataset/README.md)). Each
tool independently tries to parse both the original and the latest snippet
under three wrapper strategies (raw source, class-member, method-body) and
only analyzes a pair when both versions share a common successful mode.
Results referenced below come from `results/javaparser_readable/`,
`results/pmd_reduced/`, and `results/checkstyle/`.

**This report is a description of what the data shows, not a new
statistical analysis** — no computation beyond what the notebooks already
produced was performed to write it.

## 2. Parsing coverage across tools

| Tool | Scoped pairs | Analyzed | Excluded | Exclusion rate |
|---|---|---|---|---|
| JavaParser | 125 | 80 | 45 | 36.0% |
| PMD (reduced ruleset) | 125 | 77 | 48 | 38.4% |
| Checkstyle | 125 | 81 | 44 | 35.2% |

Parse-mode breakdown for analyzed pairs:

| Tool | RAW | CLASS_MEMBER | METHOD_BODY |
|---|---|---|---|
| JavaParser | 66 | 5 | 9 |
| PMD | 21 | 47 | 9 |
| Checkstyle | 70 | 2 | 9 |

**Observation:** all three tools independently exclude roughly a third of
the scoped dataset (35–38%) because the original and latest snippet never
share a common successful parse mode. This consistency across
independently-implemented extractors suggests the exclusion rate reflects
a genuine property of the snippets (many originals or revisions are
fragments that only parse under one specific wrapper, and the *other*
version of the same pair needs a different one) rather than a bug or
quirk specific to one tool. It should be reported plainly in the
manuscript as a coverage/eligibility figure, per the study's own
"do not silently discard unparseable pairs" principle.

One inconsistency to note: JavaParser and Checkstyle mostly succeed under
RAW parsing (66/80, 70/81), while PMD mostly needs the CLASS_MEMBER wrapper
(47/77 vs. only 21/77 RAW). This is very likely a PMD-specific parsing
requirement (PMD's Java parser is less tolerant of top-level fragments than
JavaParser or Checkstyle's checkers) rather than a difference in the
underlying snippets, since all three tools draw from the same 125-pair
manifest and the same wrapper text.

Each tool also reports **9 identical pairs** (`Identical Before After ==
YES` in the manifest) — these are valid observations expected to contribute
zero deltas everywhere, and they do, consistently.

## 3. JavaParser: structural / intrinsic metrics (n = 80)

18 structural metrics were tested (method/parameter/local-variable counts,
branch and loop counts, a cyclomatic-complexity proxy, max nesting,
abrupt exits, try/resource handling, null checks, exception handling) plus
non-comment code-line count, each independently via a paired Wilcoxon
signed-rank test with Holm correction across all 19 tests. Each test is
also paired with a matched-pairs rank-biserial correlation (signed after
minus before) as an effect-size estimate.

**Headline: no metric is significant after Holm correction.** The two
smallest raw p-values are `code_lines` (p=0.015) and `abrupt_exit_count`
(p=0.061); both rise to Holm-adjusted p≥0.27 once corrected for testing 19
metrics simultaneously.

| Metric | Δ median | Dec. | Unc. | Inc. | Wilcoxon p | Rank-biserial | Holm p |
|---|---|---|---|---|---|---|---|
| Code lines | +1 | 19 | 19 | 42 | 0.015 | 0.357 | 0.273 |
| Method count | 0 | 7 | 58 | 15 | 0.321 | 0.253 | 1.000 |
| Parameter count | 0 | 8 | 62 | 10 | 0.442 | 0.216 | 1.000 |
| Max. parameters | 0 | 5 | 71 | 4 | 1.000 | 0.000 | 1.000 |
| Local-variable count | 0 | 8 | 54 | 18 | 0.116 | 0.356 | 1.000 |
| `if` count | 0 | 9 | 62 | 9 | 0.766 | −0.088 | 1.000 |
| Loop count | 0 | 0 | 78 | 2 | 0.500 | 1.000 | 1.000 |
| Switch count | 0 | 0 | 80 | 0 | — | — | — |
| Catch count | 0 | 3 | 71 | 6 | 0.426 | 0.333 | 1.000 |
| Cyclomatic-complexity proxy | 0 | 10 | 53 | 17 | 0.336 | 0.220 | 1.000 |
| Max. control nesting | 0 | 7 | 66 | 7 | 0.542 | 0.200 | 1.000 |
| Abrupt-exit count | 0 | 7 | 57 | 16 | 0.061 | 0.449 | 1.000 |
| Try count | 0 | 2 | 71 | 7 | 0.129 | 0.600 | 1.000 |
| Try-with-resources count | 0 | 0 | 79 | 1 | 1.000 | 1.000 | 1.000 |
| Throw count | 0 | 1 | 74 | 5 | 0.438 | 0.429 | 1.000 |
| Empty-catch count | 0 | 1 | 76 | 3 | 0.625 | 0.500 | 1.000 |
| Empty-block count | 0 | 2 | 75 | 3 | 0.813 | 0.200 | 1.000 |
| Null-check count | 0 | 5 | 73 | 2 | 0.375 | −0.393 | 1.000 |
| Exception-handling count | 0 | 4 | 66 | 10 | 0.153 | 0.457 | 1.000 |

For nearly every metric, `unchanged_n` is the largest bucket — e.g.
`method_count` 58/80 unchanged, `if_count` 62/80, `cyclomatic_proxy` 53/80,
`switch_count` 80/80 (no revision touched a switch statement at all). Most
revisions in this sample simply do not change structural shape.

The one metric with real movement is `code_lines`: 42 pairs increased vs.
19 decreased (median delta +1 line). Revisions skew toward *adding* code,
consistent with the paper's own framing that bug fixes can legitimately
add validation/boundary checks — but the effect is small in magnitude and
not significant once corrected.

A caution on the rank-biserial column: several of the largest-looking
effect sizes (`loop_count` 1.00, `try_with_resources_count` 1.00,
`try_count` 0.60, `empty_catch_count` 0.50) come from metrics with only
1–9 nonzero-delta pairs out of 80. A rank-biserial correlation computed
from 1–2 nonzero pairs is not a meaningful estimate of effect size — it is
a symptom of being severely underpowered, not evidence of a large real
effect. Treat these large values as noise, not as a competing signal to
`code_lines`.

### JavaParser by recommendation type (uncorrected — exploratory only)

| Group | n | code_lines p (raw) | Pattern |
|---|---|---|---|
| Bug Fixing | 12 | 0.039 | 7/12 increased lines; every other metric ≥10/12 unchanged |
| Improving Code | 65 | 0.099 | 33/65 increased lines; other metrics mostly unchanged |

Both subgroups are far too small, and too dominated by zero-deltas, to
support an inferential claim on their own; `code_lines` is the only metric
that shows any separation from "nothing happened," and even that is only
suggestive at n=12.

## 4. PMD: bug-risk and performance violations (n = 77)

The reduced ruleset (12 bug-risk rules, 7 performance rules — see
[`pmd-ruleset-reduced.xml`](pmd-ruleset-reduced.xml)) is extremely sparse
on this snippet-level corpus: **only 81 total retained violations across
both before and after versions of all 77 pairs combined** (72 bug-risk, 9
performance).

| Dimension | Metric | Improved | Unchanged | Worsened | Wilcoxon p | Rank-biserial | Holm p |
|---|---|---|---|---|---|---|---|
| Combined | Raw violations | 5 | 66 | 6 | 0.520 | 0.24 | 1.000 |
| Combined | Violations / 100 lines | 16 | 52 | 9 | 0.578 | −0.13 | 1.000 |
| Bug-risk | Raw violations | 5 | 67 | 5 | 0.695 | 0.18 | 1.000 |
| Bug-risk | Violations / 100 lines | 14 | 55 | 8 | 0.588 | −0.14 | 1.000 |
| Performance | Raw violations | 0 | 76 | 1 | 1.000 | 1.00 | 1.000 |
| Performance | Violations / 100 lines | 3 | 73 | 1 | 0.875 | −0.20 | 1.000 |

Holm correction is applied across these six related tests; since the
smallest raw p-value (0.520) already exceeds 0.05, every corrected value
is 1.000 and the correction does not change the conclusion — it simply
confirms it more rigorously.

**No PMD metric is anywhere close to significant** (all p ≥ 0.52). Density
(violations per 100 lines) leans mildly positive (more "improved" than
"worsened" pairs in both the combined and bug-risk dimensions), but the
effect sizes are small and the raw violation counts don't show the same
lean — some of the density improvement is arithmetically driven by
revisions adding lines (denominator effect), which the study's own
methodology (README §"Outcome 1") explicitly warns about: *"density can
decrease merely because a revision adds lines... retain raw violation
counts as well."* That caveat is directly relevant here — the raw-count
result (5 improved / 66 unchanged / 6 worsened) is essentially a wash,
weaker than the density result.

### Rule-level transitions (bug-risk and performance rules with any activity)

| Rule | Dimension | Removed | Introduced | Preserved | Net removed |
|---|---|---|---|---|---|
| `AssignmentInOperand` | bug-risk | 3 | 1 | 7 | +2 |
| `IdenticalCatchBranches` | bug-risk | 1 | 1 | 1 | 0 |
| `UseStringBufferForStringAppends` | performance | 1 | 1 | 1 | 0 |
| `AppendCharacterWithChar` | performance | 0 | 1 | 1 | −1 |
| `CompareObjectsWithEquals` | bug-risk | 0 | 1 | 0 | −1 |
| `EmptyCatchBlock` | bug-risk | 0 | 1 | 7 | −1 |

Most rules never fire on either version. Where they do, the picture is
mixed: `AssignmentInOperand` shows a real net improvement (3 removed vs. 1
introduced), but `EmptyCatchBlock` and `CompareObjectsWithEquals` each show
a revision introducing a new instance with none removed — i.e., at least
one "latest" SO answer in this sample is arguably *worse* by this specific
rule than the original it replaced. `CloseResource` and `UseTryWithResources`
(both resource-leak rules central to the paper's Fixing Bug narrative) show
0 removed / 0 introduced — any resource-handling instances present were
already present in both versions (`preserved`), not something these
particular revisions changed.

### PMD by recommendation type

| Group | n | Bug-risk improved/unchanged/worsened | Wilcoxon p |
|---|---|---|---|
| Bug Fixing | 11 | 0 / 10 / 1 | 1.000 |
| Improving Code | 63 | 5 / 55 / 3 | 0.844 |

Notably, the **Bug Fixing** subgroup — the one group where you'd most
expect PMD's bug-risk rules to show improvement — shows *zero* improved
pairs and one worsened pair under this reduced ruleset. This is not
evidence that the bug fixes are bad; it mostly reflects that PMD's narrow,
deliberately conservative 12-rule bug-risk subset rarely fires on these
snippets at all (10/11 pairs have zero bug-risk violations on both sides),
so there is little for a fix to remove. It does mean this specific PMD
lens cannot be used to substantiate the Bug Fixing improvement claim.

## 5. Checkstyle: readability/style (n = 81)

8 checks are enabled (see [`checkstyle-reduced.xml`](checkstyle-reduced.xml)):
`FileTabCharacter`, `LineLength` (120 chars), `NeedBraces`, `EmptyBlock`,
`OneStatementPerLine`, `MultipleVariableDeclarations`, `AvoidNestedBlocks`,
`VariableDeclarationUsageDistance` (distance 3). 844 findings were retained
in total.

| Metric | Improved | Unchanged | Worsened | Wilcoxon p | Rank-biserial | Holm p |
|---|---|---|---|---|---|---|
| Raw findings | 14 | 55 | 12 | 0.452 | −0.17 | 0.723 |
| Findings / 100 lines | 23 | 44 | 14 | 0.362 | −0.17 | 0.723 |

Holm correction across these two related tests leaves both at p=0.723 —
unsurprising given the raw p-values were already far from 0.05.

Not significant, but this is the **largest directional lean of the three
tools**: 23 improved vs. 14 worsened pairs by density (rank-biserial
−0.17, meaning the "after" distribution skews slightly lower/better). Still
not close to p<0.05, and the raw-count split (14 vs. 12) is close to even,
again suggesting some of the density improvement is a denominator effect
from revisions adding lines.

### Rule-level transitions

| Check | Removed | Introduced | Preserved | Net removed |
|---|---|---|---|---|
| `FileTabCharacter` | 4 | 3 | 18 | +1 |
| `OneStatementPerLine` | 1 | 1 | 0 | 0 |
| `AvoidNestedBlocks` | 0 | 0 | 1 | 0 |
| `EmptyBlock` | 0 | 0 | 1 | 0 |
| `MultipleVariableDeclarations` | 0 | 1 | 2 | −1 |
| `VariableDeclarationUsageDistance` | 0 | 1 | 1 | −1 |
| `LineLength` | 0 | 2 | 5 | −2 |
| `NeedBraces` | 0 | 2 | 17 | −2 |

`FileTabCharacter` is the only check with a clear net improvement (4
removed vs. 3 introduced). Two checks trend the other way: `LineLength`
(2 new long lines introduced, 0 fixed) and `NeedBraces` (2 new missing-brace
instances introduced, 0 fixed) — some revisions make code *less* readable
by these specific measures even though the aggregate density metric leans
positive. `NeedBraces` also has the largest `preserved` count (17), meaning
most brace-omission style issues present in the original are simply
carried over unchanged into the latest revision, not addressed by it.

### Checkstyle by recommendation type

| Group | n | Improved/Unchanged/Worsened | Wilcoxon p |
|---|---|---|---|
| Bug Fixing | 12 | 2 / 8 / 2 | 0.875 |
| Improving Code | 66 | 10 / 46 / 10 | 0.430 |

Both subgroups are essentially balanced between improved and worsened —
no directional signal at all, let alone a significant one.

## 6. Cross-tool synthesis

All three independent dimensions — structural (JavaParser), bug-risk and
performance (PMD), and readability (Checkstyle) — converge on the same
top-line finding:

> **No metric, in any dimension, reaches statistical significance after
> appropriate multiple-testing correction, in either the full analyzed
> sample or the Bug Fixing / Improving Code subgroups.**

Where a directional lean exists, it is:

- **Small where it's estimable.** PMD and Checkstyle rank-biserial effect
  sizes are consistently under ~0.25 in magnitude. JavaParser's are more
  variable (up to ±0.6, occasionally 1.00) precisely for the metrics with
  the fewest nonzero-delta pairs (as few as 1–2 out of 80) — an artifact
  of a tiny effective sample, not evidence of a real large effect.
- **Inconsistent between raw counts and per-100-line density**, and the
  density measure is the one more prone to the denominator artifact the
  study's own README warns against (adding lines mechanically lowers
  density even with no violations fixed).
- **Mixed at the rule/check level.** Every tool shows at least one
  individual rule getting *net worse* (PMD: `EmptyCatchBlock`,
  `CompareObjectsWithEquals`; Checkstyle: `LineLength`, `NeedBraces`) at
  the same time the aggregate leans positive — a genuinely mixed picture,
  not a uniform improvement obscured by noise.
- **Underpowered.** With n=77–81 analyzed pairs, single-digit
  improved/worsened counts, and (for PMD especially) very sparse violation
  data, these tests have limited ability to detect anything but a large
  effect. Non-significance here is consistent with "the study can't tell"
  as much as with "there is no effect" — a Type II error risk worth stating
  explicitly rather than treating the null result as proof of no
  improvement.

The most reliable positive signal across all three tools is directional
rather than statistical: Checkstyle's density measure and PMD's density
measure both lean toward improvement (23/44/14 and 16/52/9 respectively),
and JavaParser shows revisions tend to add rather than remove code — a
pattern compatible with the paper's framing that bug fixes and
improvements often mean *adding* validation, checks, or better resource
handling rather than shrinking the snippet.

## 7. Implications for the manuscript / R5.10 rebuttal

1. **This quantitative analysis, taken alone, does not deliver "strong
   empirical evidence that the proposed method consistently improves
   software quality."** It should not be presented as such. The honest,
   defensible claim it supports is closer to the pre-registered fallback
   language already drafted in the README:

   > Compared with their original versions, recent Stack Overflow
   > revisions show small, non-significant, and rule-dependent changes in
   > static-analysis indicators of maintainability, bug-risk, and
   > readability, with directional trends leaning toward improvement in
   > violation density but not in raw counts.

2. **The manual classification and PR-acceptance results remain the
   strongest evidence in the paper** for the improvement claim (391
   applicable recommendations; 11/36 Fixing Bug recommendations accepted
   into real projects). This quantitative analysis should be framed as a
   complementary, rigorous *robustness check* that appropriately tempers
   rather than inflates that claim — which is itself a defensible response
   to a reviewer asking for more rigor, not a weakness to hide.

3. **Report the coverage/exclusion figures explicitly** (Section 2 above)
   alongside any effect estimates — over a third of the scoped sample is
   excluded from each tool's analysis, and that should sit next to any
   headline number, not in a footnote.

4. **Report both raw counts and per-100-line density**, and note the
   denominator caveat, whenever a density-based improvement is cited —
   the PMD and Checkstyle density results are visibly more favorable than
   their raw-count counterparts, and the study's own methodology document
   already anticipates and warns against over-reading that gap.

5. **Rule-level mixed results are worth a sentence.** Reviewers may ask
   "did anything get worse?" — the honest answer is yes, in specific,
   nameable rules (`EmptyCatchBlock`, `CompareObjectsWithEquals`,
   `LineLength`, `NeedBraces`), even while the aggregate trend leans
   positive. Naming this proactively is more credible than letting a
   reviewer find it.

6. **Sample-size caveat.** With 77–81 pairs and mostly single-digit
   improved/worsened splits, these tests are underpowered for anything but
   a large effect. If reviewers push further on statistical rigor, the
   options are: (a) explicitly report this as a power limitation rather
   than a null result, (b) consider a permutation test as the README
   already suggests for outcomes with many zero-change pairs, or (c) widen
   the sample past the 125-pair `Agress Yes` scope if that is
   methodologically defensible — though the current manual-validation
   scoping was itself a deliberate, defensible choice and shouldn't be
   abandoned lightly.

## 8. Known limitations of this report

- Figures above are read directly from the CSV/JSON outputs already
  produced by the three notebooks; no new statistical test was run to
  produce this report.
- As of this revision, all three notebooks report both a Holm-corrected
  p-value and a matched-pairs rank-biserial effect size for every primary
  test, so the three tools are now directly comparable on both dimensions.
  (Earlier drafts of this report only had Holm correction for JavaParser
  and only had rank-biserial effect sizes for PMD/Checkstyle.)
- `pandas`, `scipy`, and `matplotlib` are now pinned to exact versions
  (2.3.3, 1.13.1, 3.9.4) in all four notebooks. This matters: an earlier,
  unpinned run of the JavaParser notebook produced a different
  `abrupt_exit_count` p-value (0.047, Holm-adjusted 0.806) than the
  pinned, currently-reported figure (0.061, Holm-adjusted 1.000), for the
  *identical* underlying data and the *identical* Wilcoxon test statistic
  (W=76.0) — `scipy.stats.wilcoxon(..., method="auto")` silently chose a
  different p-value approximation across environments. Most JavaParser
  p-values in Section 3 shifted similarly by small amounts between
  unpinned runs; PMD's and Checkstyle's did not, likely because their
  much sparser violation counts keep them in a p-value-computation regime
  that happens to be stable across the scipy versions observed. No
  qualitative conclusion in this report changed as a result — every
  affected p-value remained non-significant before and after — but the
  specific numbers now reflect the pinned, reproducible configuration and
  should be the ones cited going forward.
- Subgroup (Bug Fixing / Improving Code) tests are reported **without**
  Holm correction in all three notebooks — treat every subgroup p-value
  here as exploratory, not confirmatory.
- This report does not include the SpotBugs sensitivity analysis or the
  manual audit of analyzer findings, both mentioned in the README's
  recommended implementation order but not yet present under `results/`.
