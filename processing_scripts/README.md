# Matcha manual-validation CSV processing

These scripts reproduce and validate:

`matcha_results_2024-05-07_manual_validation.csv`

## Requirements

- Ruby with the standard `csv`, `rexml`, and `open3` libraries
- `unzip`

No third-party packages are required.

## Generate

Run from the workspace root:

```bash
ruby processing_scripts/generate_matcha_manual_validation.rb
```

Optional positional arguments:

```text
generate_matcha_manual_validation.rb SOURCE_CSV SOURCE_XLSX OUTPUT_CSV USEFUL_PAIRS_XLSX
```

The processor reads only `Result All` (`sheet1.xml`) from the workbook. It
groups both sources by Stack Overflow question ID and normalized GitHub owner,
then pairs records in their preserved within-group order. The original CSV
paths remain authoritative. Each compound path is split into file path, method
name, starting line, and ending line. Dataset-root prefixes such as
`/root/2_github_projects_for_search/` are removed from the exported file paths.
The source workbook's `GitHub Code`, `SO Code`, `Code from GitHub`, and
`Code from StackOverflow` columns are intentionally excluded from the
consolidated CSV.
The final `Useful Pairs Cross-Check` column is set to `FOUND` when the record's
`No` value appears as a `Pair ID` in the `Useful Pairs` tab of
`Matcha_Pair_Analysis_29Jul2026.xlsx`. Duplicate Pair IDs are counted once.

After generation, copy the 36 PR links from the `Open PR List` tab:

```bash
ruby processing_scripts/cross_check_open_prs.rb
```

The PR cross-check uses the compound GitHub path from the PR workbook, split
into file path, method name, start line, and end line. It does not rely on the
PR workbook's `No` field because some of those identifiers conflict with the
recorded GitHub paths. The 36 matched records are marked `OLD` in the
`Old PR Status` column.

Finally, consolidate submitted PR links and skipped-candidate reasons from the
recommendation inventory:

```bash
ruby processing_scripts/consolidate_recommendation_prs.rb
```

This fills additional URLs into the existing `Open PR Link` column and adds
`Recommendation PR Status` and `Recommendation PR Reason`. Matching uses
GitHub owner/repository/file path plus Stack Overflow question ID. Existing
May 2025 `Open PR Link` values are preserved. When a file-level entry corresponds to
multiple matched methods in the same file, its status and reason are copied to
each applicable method row.

Record the audit decisions for the first ten no-PR candidates with:

```bash
ruby processing_scripts/record_first_10_pr_audit.rb
```

This records nine evidence-backed `SKIPPED` decisions and records the submitted
Aion pull request.

Record the following literal top-down no-PR batch (positions 11–20) with:

```bash
ruby processing_scripts/record_next_10_pr_audit.rb
```

All ten records in this batch are marked `SKIPPED` with project-specific
reasons because none contains an applicable source-code edit.

Record the literal top-down no-PR positions 21–40 with:

```bash
ruby processing_scripts/record_next_20_pr_audit.rb
```

All twenty records in this batch are marked `SKIPPED` with evidence-backed
reasons; none contains an applicable current source-code change.

Record the literal top-down no-PR positions 41–80 with:

```bash
ruby processing_scripts/record_next_40_pr_audit.rb
```

This records all forty audit outcomes, including the submitted Ant Media
`ClientList` fix and thirty-nine evidence-backed `SKIPPED` decisions.

Record the following top-down batch, positions 81–120 beginning at CSV line 88,
with:

```bash
ruby processing_scripts/record_following_40_pr_audit.rb
```

All forty records are marked `SKIPPED` with evidence-backed reasons. The
candidate edits were already present, unrelated to the matched method,
non-behavioral, or unsafe/incomplete to apply.

Record positions 121–160, beginning at CSV line 140, with:

```bash
ruby processing_scripts/record_next_positions_121_160_pr_audit.rb
```

All forty records are marked `SKIPPED`. Current upstream checks confirmed that
the only apparent production issue in the historical source had already been
fixed; the other recommendations were inapplicable or non-behavioral.

Record positions 161–200, beginning at CSV line 196, with:

```bash
ruby processing_scripts/record_next_positions_161_200_pr_audit.rb
```

This records thirty-nine evidence-backed `SKIPPED` decisions and the submitted
Clusion null-directory-listing fix.

Record positions 201–240, beginning at CSV line 240, with:

```bash
ruby processing_scripts/record_next_positions_201_240_pr_audit.rb
```

This records thirty-three new `SKIPPED` decisions, the submitted Freedomotic
stream-safety fix, and preserves six outcomes already populated from earlier
PR and recommendation cross-checks.

Record positions 241–280, beginning at CSV line 280, with:

```bash
ruby processing_scripts/record_next_positions_241_280_pr_audit.rb
```

This records thirty-three new `SKIPPED` decisions, the submitted CCSocialShare
stream-cleanup fix, and preserves six outcomes already populated from earlier
PR and recommendation cross-checks.

Record positions 281–320, beginning at CSV line 320, with:

```bash
ruby processing_scripts/record_next_positions_281_320_pr_audit.rb
```

This records thirty-seven new `SKIPPED` decisions and preserves three outcomes
already populated by an earlier recommendation audit.

Record positions 321–360, beginning at CSV line 360, with:

```bash
ruby processing_scripts/record_next_positions_321_360_pr_audit.rb
```

This records thirty-eight new `SKIPPED` decisions and preserves two outcomes
already populated by an earlier recommendation audit.

Record positions 361–400, beginning at CSV line 400, with:

```bash
ruby processing_scripts/record_next_positions_361_400_pr_audit.rb
```

This records thirty-two new `SKIPPED` decisions, two submitted fixes for
Kickflip and Knowage Server, and preserves six prior outcomes.

Record positions 401–440, beginning at CSV line 440, with:

```bash
ruby processing_scripts/record_next_positions_401_440_pr_audit.rb
```

This records thirty-five new `SKIPPED` decisions and preserves five prior
outcomes, including historical and previously submitted PR records.

Record positions 441–480, beginning at CSV line 480, with:

```bash
ruby processing_scripts/record_next_positions_441_480_pr_audit.rb
```

This records thirty-five new `SKIPPED` decisions and preserves five prior
outcomes, including historical and previously submitted PR records.

Record positions 481–520, beginning at CSV line 520, with:

```bash
ruby processing_scripts/record_next_positions_481_520_pr_audit.rb
```

This records twenty-four new `SKIPPED` decisions, the submitted p2abcengine
stream-cleanup fix, and preserves fifteen prior audit or historical PR outcomes.

Record positions 521–560, beginning at CSV line 560, with:

```bash
ruby processing_scripts/record_next_positions_521_560_pr_audit.rb
```

This records twenty-nine new `SKIPPED` decisions, submitted XInstaller and
Red5 fixes, and preserves nine prior audit outcomes.

Record positions 561–600, beginning at CSV line 600, with:

```bash
ruby processing_scripts/record_next_positions_561_600_pr_audit.rb
```

This records thirty-seven new `SKIPPED` decisions and preserves three prior
outcomes. The only applicable source issue was in an archived repository.

Record positions 601–700, beginning at CSV line 640, with:

```bash
ruby processing_scripts/record_next_positions_601_700_pr_audit.rb
```

This records eighty-eight new `SKIPPED` decisions, the submitted
logback-android test stream-cleanup fix, and preserves eleven prior outcomes.

Record every remaining uncovered row with:

```bash
ruby processing_scripts/record_remaining_49_pr_audit.rb
```

This closes the 49 gaps left across the beginning and end of the CSV. All 49
are marked `SKIPPED` with record-specific reasons: the selected Stack Overflow
revision was identical, formatting-only, unrelated to the matched method,
contract-changing or invalid for the target type, or (for Sealnote) the
otherwise useful cleanup targeted an archived repository.

## Validate

```bash
ruby processing_scripts/validate_matcha_manual_validation.rb
```

Optional positional arguments:

```text
validate_matcha_manual_validation.rb SOURCE_CSV OUTPUT_CSV
```

Validation checks record counts, headers, preservation of all split source
fields, one-to-one workbook record use, Stack Overflow IDs, GitHub projects,
and match statuses.
