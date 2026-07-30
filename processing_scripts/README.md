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
