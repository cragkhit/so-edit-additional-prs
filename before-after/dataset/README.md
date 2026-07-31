# Stack Overflow Before–After Dataset

## Contents

- `before_agress_yes/`: Original snapshots for the 125 unique, eligible
  histories associated with at least one `Agress Yes` study row.
- `after_agress_yes/`: Corresponding recent/latest snapshots.
- `agress_yes_pairs.csv`: The active 125-pair PMD analysis manifest, including
  hashes and paths to the scoped files.
- `snippet_pairs.csv`: Provenance manifest for all unique Stack Overflow
  histories. Its former broad generated source folders are intentionally not
  retained.
- `study_pair_mapping.csv`: Maps all 793 GitHub–Stack Overflow study rows to
  the unique snippet histories.

## Dataset unit

The independent unit for before–after quality analysis is a unique Stack
Overflow question and code-block number, represented as:

```text
so_<question-id>_block_<block-number>
```

The 793 study rows contain repeated uses of the same Stack Overflow snippets.
Counting every study row as an independent before–after observation would
pseudoreplicate some edits. Therefore, `snippet_pairs.csv` contains the
deduplicated analysis units, while `study_pair_mapping.csv` preserves
traceability to every original study row.

## Counts

- Study rows mapped: 793
- Unique Stack Overflow snippet histories: 209
- Eligible before–after pairs: 205
- Excluded histories: 4
- Eligible pairs with identical snapshots: 12
- Pairs using `_recent.java` as the after snapshot: 191
- Pairs using the study-selected latest available snapshot: 14

The 12 identical pairs are intentionally retained. They are valid unchanged
observations and should normally contribute zero deltas to the quality metrics.

## Selection rules

For each unique question/code-block history:

1. The before version is `<question>_<block>_original.java`.
2. The after version is `<question>_<block>_recent.java` when that file exists.
3. If no `_recent.java` snapshot exists, the Java file selected in
   `matcha_results_2024-05-07_manual_validation_FINAL.csv` is used as the
   latest available after version.
4. A history is excluded if either side is unavailable.

Every active scoped file has a SHA-256 hash and path in
`agress_yes_pairs.csv`. The broader provenance metadata remains in
`snippet_pairs.csv`.

## Exclusions

Four histories lack an original snapshot:

- `so_5061920_block_1`
- `so_9109728_block_8`
- `so_18004334_block_4`
- `so_19435226_block_1`

They remain in `snippet_pairs.csv` with `Status=EXCLUDED` and are also retained
in the 793-row mapping.

## Manual-validation grouping

Manual accept/reject decisions concern whether a Stack Overflow recommendation
is useful for a particular GitHub match. The same SO edit can therefore have
different decisions for different GitHub projects.

At the unique-snippet level:

- 91 histories are accepted for every associated GitHub pair.
- 81 histories are rejected for every associated GitHub pair.
- 37 histories have mixed accept/reject decisions.

Do not force the mixed histories into a single accepted or rejected label.
Use the 793-row mapping for project-specific validation analysis.

## Rebuild

From `before-after/`:

```bash
ruby prepare_dataset.rb
```

Optional arguments:

```text
prepare_dataset.rb SOURCE_CSV VALIDATION_CSV REVISION_ROOT
```

The script deterministically recreates the copied snippets and both manifests.
