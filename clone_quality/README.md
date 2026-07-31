# GitHub–Stack Overflow Code Clone Validation

## Objective

For every row in `matcha_results_2024-05-07_manual_validation_FINAL.csv`, determine whether the code identified by:

- `GitHub Java File Path`
- `GitHub Method Name`
- `GitHub Start Line`
- `GitHub End Line`

is a clone of the code identified by:

- `Stack Overflow Java File Path`
- `Stack Overflow Method Name`
- `Stack Overflow Start Line`
- `Stack Overflow End Line`

The classification should follow Roy and Cordy's clone taxonomy in *A Survey on Software Clone Detection Research* (Technical Report 2007-541).

## Clone taxonomy

- **Type I:** Identical code except for whitespace, layout, and comments.
- **Type II:** Structurally or syntactically identical code except for identifiers, literals, types, layout, and comments.
- **Type III:** Copied code with statements changed, added, or removed, in addition to Type II variations.
- **Type IV:** Code that performs the same computation, or has sufficiently similar preconditions and postconditions, but uses different syntax.

Types should be tested from strictest to loosest. Assign the lowest type that explains the pair.

The clone relation does not by itself prove which fragment was copied from the other.

## Important properties of the data

The CSV contains 793 pairs, and all current `Match Status` values are `MATCHED`. That column is therefore not independent ground truth.

The extracted snippets are also present in `matcha-review-app/public/pairs.json`. Inspection of those snippets found:

- GitHub snippets have a median of 11 nonempty lines.
- Stack Overflow snippets have a median of 23 nonempty lines and can contain as many as 417 lines.
- 648 Stack Overflow method names are the generic name `method`.
- Only 110 pairs have the same recorded method name.
- At least 113 Stack Overflow snippets contain Markdown or other non-Java material.

Consequently, comparing the two complete recorded ranges directly would produce false negatives. The detector must locate corresponding subregions within the supplied ranges.

## Proposed result columns

Do not overwrite `Match Status`. Add independent validation fields:

| Column | Meaning |
| --- | --- |
| `Clone Decision` | `CLONE`, `NOT_CLONE`, or `UNCERTAIN` |
| `Clone Type` | `TYPE_1`, `TYPE_2`, `TYPE_3`, `TYPE_4`, or blank |
| `GH Matched Start Line` | Actual beginning of the matched GitHub subregion |
| `GH Matched End Line` | Actual end of the matched GitHub subregion |
| `SO Matched Start Line` | Actual beginning of the matched Stack Overflow subregion |
| `SO Matched End Line` | Actual end of the matched Stack Overflow subregion |
| `Coverage GH` | Fraction of the GitHub fragment explained by the alignment |
| `Coverage SO` | Fraction of the Stack Overflow fragment explained by the alignment |
| `Text Similarity` | Similarity after Type I normalization |
| `Parameterized Similarity` | Similarity after Type II normalization |
| `Structural Similarity` | AST or statement-alignment score |
| `Semantic Evidence` | Tests, data-flow evidence, or reviewer explanation |
| `Confidence` | `HIGH`, `MEDIUM`, or `LOW` |
| `Reason` | Concise evidence-based justification |
| `Reviewer 1` | First independent manual label |
| `Reviewer 2` | Second independent manual label |
| `Adjudicated Label` | Final label after resolving disagreement |

Clone status and recommendation usefulness must remain separate. A pair can be a clone even when applying the Stack Overflow edit to the GitHub project would be inappropriate.

## Detection pipeline

### 1. Extract and validate ranges

For each row:

1. Read both Java files.
2. Extract the inclusive start-to-end line ranges.
3. Record file and snippet hashes for reproducibility.
4. Flag missing files, invalid ranges, or ranges inconsistent with the named method.
5. Preserve the raw snippets before cleaning.

Do not assume that equal method names are required or that the generic Stack Overflow name `method` carries useful evidence.

### 2. Remove Stack Overflow presentation artifacts

Maintain two representations:

- The untouched raw snippet for auditing.
- A Java representation with Markdown fences, answer numbering, prose separators, and obvious non-code lines removed.

Attempt parsing in this order:

1. Compilation unit
2. Class member
3. Method body
4. Statement sequence
5. Token-only fallback

Record the parsing mode. Do not silently repair meaningful Java syntax.

### 3. Locate corresponding subregions

Use local alignment rather than comparing only the complete ranges:

1. Java-tokenize both snippets.
2. Generate normalized token shingles, such as five-token sequences.
3. Use winnowing or a suffix-based matcher to find shared anchors.
4. Extend anchors in both directions.
5. Align nearby statements with a local sequence-alignment algorithm.
6. Select the highest-coverage coherent alignment.

Prefer contiguous, ordered regions but permit gaps for Type III. Do not build a match from unrelated single lines scattered through a large Stack Overflow answer.

The result should identify both the supplied and actual matched boundaries.

### 4. Classify the aligned fragments

#### Type I

Remove or normalize only:

- Comments
- Whitespace
- Layout

If the resulting Java token sequences are identical, classify the pair as Type I with high confidence.

#### Type II

Normalize:

- User-defined identifiers
- Literals
- Types
- Comments and layout

Require the syntactic or AST structure to remain identical, with no inserted or deleted statements.

Identifier renaming should be consistent. If one occurrence of `input` maps to `source`, subsequent corresponding occurrences should preserve that mapping. Replacing every identifier with one generic token would produce excessive false positives.

#### Type III

Perform statement-level and AST-level alignment after Type II normalization. Permit inserted, deleted, and modified statements while requiring a substantial common syntactic core.

Possible initial pilot thresholds are:

- At least 50 normalized Java tokens or five substantive statements in the shared core.
- At least 70% coverage of the shorter fragment.
- At least 50% coverage of the longer matched region.
- Most matched statements preserve their order.
- The central control-flow skeleton is compatible.

These are operational starting points, not thresholds defined by the paper. They must be calibrated against manually labelled examples.

Boilerplate such as getters, setters, logging calls, import lists, standard equality helpers, and empty lifecycle methods should require stronger evidence or be tagged as `UBIQUITOUS_PATTERN`.

#### Type IV

Do not assign Type IV merely because two fragments address the same topic. Require explicit semantic evidence:

- Equivalent input/output contract
- Equivalent externally visible state changes
- Compatible exceptional behavior
- Equivalent side effects
- Passing differential tests on normal and boundary inputs
- Compatible control and data dependencies despite different syntax

Differential testing is practical for pure methods. I/O, UI, framework, and concurrent code will often require manual semantic review.

If semantic equivalence is plausible but cannot be demonstrated, classify the pair as `UNCERTAIN`, not Type IV.

### 5. Identify non-clones

Classify a pair as `NOT_CLONE` when:

- Only isolated boilerplate tokens match.
- The fragments address the same broad topic but compute different things.
- The shared code is too small to be a meaningful fragment.
- Their control flow or data flow is incompatible.
- A large Stack Overflow range contains an unrelated method that caused the apparent match.
- One side is only an API alternative, dependency declaration, prose, or configuration unrelated to the other method.

## Decision tree

```text
Valid Java-bearing ranges?
  No  -> UNCERTAIN / INVALID_INPUT
  Yes
    |
    +-- Equal after comments/layout removal?        -> TYPE_1
    +-- Same syntax after parameter normalization?  -> TYPE_2
    +-- Substantial ordered AST/statement match?    -> TYPE_3
    +-- Demonstrably equivalent computation?        -> TYPE_4
    +-- Otherwise                                    -> NOT_CLONE
```

## Manual-validation protocol

Automatic results should provide evidence rather than act as the sole authority, especially for Types III and IV.

1. Select a pilot of approximately 100 pairs stratified by:
   - Similarity score
   - Fragment-length ratio
   - Parse success or failure
   - Named versus generic methods
   - Proposed clone type
2. Have two reviewers label each pair independently.
3. Show reviewers aligned snippets and relevant file context, but hide recommendation usefulness, PR status, and existing remarks to avoid bias.
4. Resolve disagreements through adjudication.
5. Measure:
   - Cohen's kappa for `CLONE` versus `NOT_CLONE`
   - Weighted kappa for Types I-IV
   - Per-type precision and recall of the automatic classifier
6. Calibrate Type III thresholds using the pilot.
7. Apply the calibrated pipeline to all 793 pairs.
8. Manually inspect:
   - Every proposed Type IV pair
   - Every low-confidence pair
   - Every parse failure
   - Samples close to each threshold

`UNCERTAIN` should be retained as a legitimate result rather than forcing ambiguous cases into clone or non-clone labels.

## Suggested implementation

- JavaParser or Eclipse JDT for Java parsing and AST construction
- A Java lexer for normalized tokens
- Winnowing or token-shingle matching for local alignment
- GumTree-style AST differencing for Type III evidence
- Optional program-dependence or data-flow analysis for Type IV
- JUnit and generated inputs for differential semantic tests where possible
- The existing review application for displaying:
  - Raw snippets
  - Cleaned Java
  - Matched subregions
  - Token and statement alignment
  - Similarity scores
  - Classification evidence
  - Reviewer labels

## Recommended overall workflow

```text
Range validation
  -> Java cleanup and parsing
  -> Local subregion alignment
  -> Hierarchical Type I-IV classification
  -> Evidence presentation
  -> Independent human review
  -> Adjudicated final result
```

This follows the paper's general detection process: preprocessing, transformation, matching, formatting the source locations, and manually filtering false positives.

## Current implementation

The first-stage analyzer uses only the Ruby standard library:

```bash
ruby clone_quality/analyze_clones.rb
```

It reads `clone_quality/clone_pairs.csv` for pair identity and
`matcha-review-app/public/pairs.json` for the extracted snippets. It writes
`clone_quality/clone_analysis_results.csv`.

The current stage implements:

- Stack Overflow presentation-artifact cleanup.
- Java-aware tokenization with comment removal and source-line tracking.
- Exact local containment for provisional Type I classification.
- Parameterized local containment with consistent identifier mapping for
  provisional Type II classification.
- Ordered normalized-statement alignment and conservative thresholds for
  provisional Type III classification.
- Similarity, coverage, matched-line, parse-mode, confidence, review, and
  SHA-256 audit fields.

Type III results remain marked for manual review. Type IV is intentionally not
assigned automatically because it requires behavioral evidence.

Run a small sample with:

```bash
ruby clone_quality/analyze_clones.rb --limit 20 \
  --output clone_quality/sample_results.csv
```

Run the tests with:

```bash
ruby clone_quality/test/test_clone_analyzer.rb
```

This is a starting point. Its provisional thresholds must be calibrated using
the independently labelled pilot described above.
