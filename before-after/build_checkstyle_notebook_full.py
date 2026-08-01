#!/usr/bin/env python3
"""Generate the full-scope Checkstyle before-after analysis notebook.

Unlike build_checkstyle_notebook.py (scoped to the 125 `Agress Yes`
pairs), this covers all 205 eligible, deduplicated Stack Overflow
histories derived from the complete 793-row Matcha manual-validation
study, using dataset/snippet_pairs.csv instead of
dataset/agress_yes_pairs.csv.
"""

import json
from pathlib import Path
from textwrap import dedent


ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "Checkstyle_Before_After_Analysis_Full.ipynb"


def markdown(source):
    return {"cell_type": "markdown", "metadata": {}, "source": dedent(source).strip().splitlines(True)}


def code(source):
    return {
        "cell_type": "code", "execution_count": None, "metadata": {},
        "outputs": [], "source": dedent(source).strip().splitlines(True),
    }


cells = [
    markdown("""
    # Checkstyle Before–After Analysis of Stack Overflow Snippets (Full Scope)

    This notebook measures a focused **style/readability** dimension for all
    **205 unique, eligible** Stack Overflow before/after histories derived
    from the complete 793-row Matcha manual-validation study
    (`matcha_results_2024-05-07_manual_validation_FINAL.csv`) -- not just the
    subset whose `Final Manual Validation` is `Agress Yes` analyzed in
    [`Checkstyle_Before_After_Analysis.ipynb`](Checkstyle_Before_After_Analysis.ipynb).
    It does not treat Checkstyle findings as runtime bugs or performance
    measurements.

    The same wrapper mode must parse on both sides of each pair. Wrapper-only
    findings are removed, identical revisions remain valid zero-change
    observations, and the unique `Snippet ID` is the independent unit. Each
    history also carries a `validation_group` label (`ALL_ACCEPTED`,
    `ALL_REJECTED`, `MIXED`) so results can be checked separately for
    accepted vs. rejected recommendations.
    """),
    markdown("""
    ## 1. Dependencies and pinned configuration

    Checkstyle 13.9.0 is downloaded from its official GitHub release if the
    pinned all-in-one JAR is not already present. Python packages are used only
    for tables, paired tests, and plots, and are pinned to exact versions: an
    unpinned install can silently change `scipy.stats.wilcoxon`'s internal
    choice between its exact and normal-approximation p-value methods across
    environments, which can shift a borderline p-value even though the
    underlying data and test statistic are unchanged.
    """),
    code("""
    %pip install -q pandas==2.3.3 scipy==1.13.1 matplotlib==3.9.4

    import hashlib
    import json
    import math
    import re
    import subprocess
    import urllib.request
    from pathlib import Path
    import xml.etree.ElementTree as ET

    import matplotlib.pyplot as plt
    import pandas as pd
    from scipy import stats

    pd.set_option("display.max_columns", 100)
    pd.set_option("display.max_colwidth", 120)
    """),
    code("""
    ROOT = Path.cwd().resolve()
    DATASET = ROOT / "dataset"
    MANIFEST_PATH = DATASET / "snippet_pairs.csv"
    MAPPING_PATH = DATASET / "study_pair_mapping.csv"
    CONFIG_PATH = ROOT / "checkstyle-reduced.xml"
    WORK_ROOT = ROOT / "work" / "checkstyle_full"
    RESULTS_ROOT = ROOT / "results" / "checkstyle_full"
    TOOLS_ROOT = ROOT / "tools"

    CHECKSTYLE_VERSION = "13.9.0"
    CHECKSTYLE_URL = (
        "https://github.com/checkstyle/checkstyle/releases/download/"
        f"checkstyle-{CHECKSTYLE_VERSION}/checkstyle-{CHECKSTYLE_VERSION}-all.jar"
    )
    CHECKSTYLE_JAR = TOOLS_ROOT / f"checkstyle-{CHECKSTYLE_VERSION}-all.jar"

    for directory in (WORK_ROOT, RESULTS_ROOT, TOOLS_ROOT):
        directory.mkdir(parents=True, exist_ok=True)
    for required in (MANIFEST_PATH, MAPPING_PATH, CONFIG_PATH):
        assert required.is_file(), required

    if not CHECKSTYLE_JAR.is_file():
        print(f"Downloading Checkstyle {CHECKSTYLE_VERSION}...")
        urllib.request.urlretrieve(CHECKSTYLE_URL, CHECKSTYLE_JAR)

    version = subprocess.run(
        ["java", "-jar", str(CHECKSTYLE_JAR), "--version"],
        text=True, capture_output=True, check=True,
    )
    print(version.stdout or version.stderr)
    """),
    markdown("""
    ## 2. Load and verify the full-scope dataset

    `snippet_pairs.csv` covers every unique Stack Overflow question/code-block
    history reachable from all 793 study rows, filtered here to
    `Status == ELIGIBLE`. `Recommendation Types` (semicolon-joined, possibly
    empty) collapses to a single `recommendation_group`: a single type is
    kept as-is, more than one becomes `MULTIPLE_TYPES`, and no type becomes
    `NO_TYPE`. The full study-row mapping is loaded only to preserve the
    dataset-level exclusions in the audit output.
    """),
    code("""
    def classify_recommendation_group(value):
        types = [item for item in value.split(";") if item]
        if len(types) > 1:
            return "MULTIPLE_TYPES"
        if len(types) == 1:
            return types[0]
        return "NO_TYPE"

    pairs = pd.read_csv(MANIFEST_PATH, dtype=str, keep_default_na=False)
    pairs = pairs.loc[pairs["Status"] == "ELIGIBLE"].copy()
    pairs = pairs.rename(columns={
        "Accepted Pair Count": "accepted_study_rows",
        "Validation Group": "validation_group",
    })
    pairs["recommendation_group"] = pairs["Recommendation Types"].map(classify_recommendation_group)

    mapping = pd.read_csv(MAPPING_PATH, dtype=str, keep_default_na=False)
    dataset_exclusions = mapping.loc[mapping["Dataset Status"] != "ELIGIBLE"].copy()

    assert len(pairs) == 205 and pairs["Snippet ID"].is_unique
    assert len(mapping) == 793 and len(dataset_exclusions) == 6
    print("Scoped unique pairs:", len(pairs))
    print("Dataset exclusions:", len(dataset_exclusions))
    display(pairs["recommendation_group"].value_counts().rename("histories"))
    display(pairs["validation_group"].value_counts().rename("histories"))
    """),
    markdown("""
    ## 3. Create symmetric source variants

    Each side is tried as raw Java, a class member, and a method body. Files
    are checked individually so one malformed snippet cannot abort a batch.
    """),
    code(r'''
    WRAPPERS = {
        "RAW": ("", ""),
        "CLASS_MEMBER": ("class SnippetWrapper {\n", "\n}\n"),
        "METHOD_BODY": (
            "class SnippetWrapper {\n    void snippetMethod() throws Exception {\n",
            "\n    }\n}\n",
        ),
    }
    MODE_ORDER = list(WRAPPERS)
    input_rows = []

    for mode, (prefix, suffix) in WRAPPERS.items():
        for version, source_column in (
            ("before", "Before Dataset Path"), ("after", "After Dataset Path")
        ):
            directory = WORK_ROOT / "inputs" / mode / version
            directory.mkdir(parents=True, exist_ok=True)
            for stale in directory.glob("*.java"):
                stale.unlink()
            for row in pairs.to_dict("records"):
                source = ROOT / row[source_column]
                raw = source.read_text(encoding="utf-8", errors="replace")
                target = directory / f"{row['Snippet ID']}.java"
                target.write_text(prefix + raw + suffix, encoding="utf-8")
                prefix_lines = len(prefix.splitlines())
                source_lines = len(raw.splitlines())
                input_rows.append({
                    "snippet_id": row["Snippet ID"], "version": version, "mode": mode,
                    "input_path": str(target.resolve()), "prefix_lines": prefix_lines,
                    "snippet_start_line": prefix_lines + 1,
                    "snippet_end_line": prefix_lines + source_lines,
                })

    input_metadata = pd.DataFrame(input_rows)
    input_metadata.to_csv(WORK_ROOT / "input_metadata.csv", index=False)
    print("Generated inputs:", len(input_metadata))
    '''),
    markdown("""
    ## 4. Run Checkstyle and parse reports

    XML reports and stderr logs are retained for every file. A mode is
    successful only when Checkstyle produces a parseable report and does not
    report a Java parse exception. Ordinary Checkstyle violations do not make
    a mode unsuccessful.
    """),
    code(r'''
    REPORT_ROOT = WORK_ROOT / "reports"
    REPORT_ROOT.mkdir(parents=True, exist_ok=True)
    finding_rows, run_rows = [], []

    for item in input_metadata.to_dict("records"):
        report_dir = REPORT_ROOT / item["mode"] / item["version"]
        report_dir.mkdir(parents=True, exist_ok=True)
        report_path = report_dir / f"{item['snippet_id']}.xml"
        command = [
            "java", "-jar", str(CHECKSTYLE_JAR), "-c", str(CONFIG_PATH),
            "-f", "xml", "-o", str(report_path), item["input_path"],
        ]
        completed = subprocess.run(command, text=True, capture_output=True)
        log_text = (completed.stdout or "") + (completed.stderr or "")
        (report_dir / f"{item['snippet_id']}.log").write_text(log_text, encoding="utf-8")
        parse_ok = report_path.is_file()
        report_error = ""
        if parse_ok:
            try:
                root = ET.parse(report_path).getroot()
                for file_node in root.findall("file"):
                    for error in file_node.findall("error"):
                        source = error.attrib.get("source", "")
                        finding_rows.append({
                            "snippet_id": item["snippet_id"], "version": item["version"],
                            "mode": item["mode"], "line_wrapped": int(error.attrib.get("line", 0)),
                            "column": int(error.attrib.get("column", 0)),
                            "severity": error.attrib.get("severity", ""),
                            "message": error.attrib.get("message", ""),
                            "source": source,
                            "check": source.rsplit(".", 1)[-1].removesuffix("Check"),
                        })
            except ET.ParseError as exc:
                parse_ok, report_error = False, str(exc)
        if re.search(r"(JavaParseException|TokenStreamRecognitionException|Exception was thrown)", log_text):
            parse_ok = False
            report_error = report_error or "Java parse exception"
        run_rows.append({
            "snippet_id": item["snippet_id"], "version": item["version"],
            "mode": item["mode"], "return_code": completed.returncode,
            "parse_ok": parse_ok, "error": report_error,
        })

    raw_findings = pd.DataFrame(finding_rows)
    runs = pd.DataFrame(run_rows)
    runs.to_csv(RESULTS_ROOT / "checkstyle_run_log.csv", index=False)
    print("Individual Checkstyle runs:", len(runs))
    display(runs["parse_ok"].value_counts())
    '''),
    markdown("""
    ## 5. Select a common mode and remove wrapper findings

    The first mode successful for both versions is selected. Only findings
    beginning on a snippet source line are retained, and wrapper offsets are
    removed from the reported line numbers.
    """),
    code("""
    ok = set(runs.loc[runs["parse_ok"], ["snippet_id", "version", "mode"]]
             .itertuples(index=False, name=None))
    selected = []
    for snippet_id in pairs["Snippet ID"]:
        chosen = next((mode for mode in MODE_ORDER
                       if (snippet_id, "before", mode) in ok
                       and (snippet_id, "after", mode) in ok), "")
        selected.append({"snippet_id": snippet_id, "selected_mode": chosen})
    selected_modes = pd.DataFrame(selected)

    if raw_findings.empty:
        findings = pd.DataFrame(columns=[
            "snippet_id", "version", "mode", "check", "severity", "line", "column",
            "message", "source",
        ])
    else:
        findings = raw_findings.merge(selected_modes, on="snippet_id")
        findings = findings.loc[findings["mode"] == findings["selected_mode"]].copy()
        findings = findings.merge(input_metadata, on=["snippet_id", "version", "mode"])
        findings = findings.loc[findings["line_wrapped"].between(
            findings["snippet_start_line"], findings["snippet_end_line"]
        )].copy()
        findings["line"] = findings["line_wrapped"] - findings["prefix_lines"]
        findings = findings[[
            "snippet_id", "version", "mode", "check", "severity", "line", "column",
            "message", "source",
        ]]
    findings.to_csv(RESULTS_ROOT / "checkstyle_findings_long.csv", index=False)
    display(selected_modes["selected_mode"].replace("", "NO_COMMON_MODE").value_counts())
    print("Retained findings:", len(findings))
    """),
    markdown("""
    ## 6. Paired metrics and rule transitions

    Negative deltas indicate fewer style/readability findings after revision.
    Raw counts and counts per 100 nonblank, non-comment code lines are both
    retained.
    """),
    code(r'''
    def physical_code_lines(text):
        cleaned = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
        cleaned = re.sub(r"//.*", "", cleaned)
        return sum(bool(line.strip()) for line in cleaned.splitlines())

    counts = (findings.groupby(["snippet_id", "version"]).size()
              if not findings.empty else pd.Series(dtype=int))
    metric_rows = []
    for row in pairs.to_dict("records"):
        snippet_id = row["Snippet ID"]
        mode = selected_modes.loc[selected_modes["snippet_id"] == snippet_id,
                                  "selected_mode"].iloc[0]
        metric = {
            "snippet_id": snippet_id, "status": "ANALYZED" if mode else "PARSE_EXCLUDED",
            "parse_mode": mode, "recommendation_group": row["recommendation_group"],
            "validation_group": row["validation_group"],
            "accepted_study_rows": int(row["accepted_study_rows"]),
            "identical_before_after": row["Identical Before After"],
        }
        for version, column in (("before", "Before Dataset Path"), ("after", "After Dataset Path")):
            text = (ROOT / row[column]).read_text(encoding="utf-8", errors="replace")
            loc = physical_code_lines(text)
            count = int(counts.get((snippet_id, version), 0))
            metric[f"{version}_code_lines"] = loc
            metric[f"{version}_findings"] = count
            metric[f"{version}_findings_per_100_lines"] = 100 * count / loc if loc else math.nan
        metric["delta_findings"] = metric["after_findings"] - metric["before_findings"]
        metric["delta_findings_per_100_lines"] = (
            metric["after_findings_per_100_lines"] - metric["before_findings_per_100_lines"]
        )
        metric["outcome"] = ("IMPROVED" if metric["delta_findings"] < 0 else
                             "WORSENED" if metric["delta_findings"] > 0 else "UNCHANGED")
        metric_rows.append(metric)
    pair_metrics = pd.DataFrame(metric_rows)
    pair_metrics.to_csv(RESULTS_ROOT / "checkstyle_pair_metrics.csv", index=False)
    analyzed = pair_metrics.loc[pair_metrics["status"] == "ANALYZED"].copy()

    analyzed_ids = set(analyzed["snippet_id"])
    checks = sorted(findings["check"].unique()) if not findings.empty else []
    presence = set(findings[["snippet_id", "version", "check"]].drop_duplicates()
                   .itertuples(index=False, name=None))
    transitions = []
    for check in checks:
        removed = introduced = preserved = absent = 0
        for snippet_id in analyzed_ids:
            before_has, after_has = ((snippet_id, side, check) in presence
                                     for side in ("before", "after"))
            if before_has and after_has: preserved += 1
            elif before_has: removed += 1
            elif after_has: introduced += 1
            else: absent += 1
        transitions.append({
            "check": check, "removed_histories": removed,
            "introduced_histories": introduced, "preserved_histories": preserved,
            "absent_histories": absent, "net_removed": removed - introduced,
        })
    rule_transitions = pd.DataFrame(transitions)
    if not rule_transitions.empty:
        rule_transitions = rule_transitions.sort_values(
            ["net_removed", "removed_histories"], ascending=False
        )
    rule_transitions.to_csv(RESULTS_ROOT / "checkstyle_rule_transitions.csv", index=False)
    display(analyzed["outcome"].value_counts())
    display(rule_transitions)
    '''),
    markdown("""
    ## 7. Paired statistics and subgroup sensitivity analyses

    The Wilcoxon test is paired by unique snippet history. Bug Fixing and
    Improving Code are secondary subgroups; `MULTIPLE_TYPES` and `NO_TYPE`
    histories are reported but excluded from the single-type tests.
    `validation_group` (`ALL_ACCEPTED`/`ALL_REJECTED`/`MIXED`) is a second,
    full-scope-only subgroup dimension.

    The two primary rows (raw findings and findings per 100 code lines)
    are a family of related tests on the same pairs, so Holm's step-down
    procedure is applied across their `wilcoxon_p` values to control the
    family-wise error rate.
    """),
    code("""
    def paired_summary(frame, before_column, after_column):
        before, after = frame[before_column].astype(float), frame[after_column].astype(float)
        delta = after - before
        nonzero = delta[delta != 0]
        if len(nonzero):
            test = stats.wilcoxon(nonzero, alternative="two-sided", method="auto")
            ranks = stats.rankdata(abs(nonzero))
            positive, negative = ranks[nonzero > 0].sum(), ranks[nonzero < 0].sum()
            effect = (positive - negative) / (positive + negative)
        else:
            test, effect = None, 0.0
        return {
            "n": len(frame), "before_median": before.median(),
            "before_iqr": before.quantile(.75) - before.quantile(.25),
            "after_median": after.median(),
            "after_iqr": after.quantile(.75) - after.quantile(.25),
            "delta_median": delta.median(), "improved_n": int((delta < 0).sum()),
            "unchanged_n": int((delta == 0).sum()), "worsened_n": int((delta > 0).sum()),
            "wilcoxon_statistic": test.statistic if test else math.nan,
            "wilcoxon_p": test.pvalue if test else math.nan,
            "rank_biserial_after_minus_before": effect,
        }

    def holm_correct(frame):
        adjusted = pd.Series(index=frame.index, dtype=float)
        valid = frame["wilcoxon_p"].dropna().sort_values()
        running, total = 0.0, len(valid)
        for rank, (index, pvalue) in enumerate(valid.items()):
            running = max(running, min(1.0, (total - rank) * pvalue))
            adjusted.loc[index] = running
        return adjusted

    statistical_summary = pd.DataFrame([
        {"metric": "Raw Checkstyle findings",
         **paired_summary(analyzed, "before_findings", "after_findings")},
        {"metric": "Checkstyle findings per 100 code lines",
         **paired_summary(analyzed, "before_findings_per_100_lines",
                          "after_findings_per_100_lines")},
    ])
    statistical_summary["holm_p"] = holm_correct(statistical_summary)
    statistical_summary.to_csv(RESULTS_ROOT / "checkstyle_statistical_summary.csv", index=False)

    subgroup_rows = []
    for group, frame in analyzed.loc[analyzed["recommendation_group"].isin(
        ["Bug Fixing", "Improving Code"]
    )].groupby("recommendation_group"):
        subgroup_rows.append({"recommendation_group": group,
                              **paired_summary(frame, "before_findings", "after_findings")})
    subgroup_summary = pd.DataFrame(subgroup_rows)
    subgroup_summary.to_csv(RESULTS_ROOT / "checkstyle_recommendation_subgroups.csv", index=False)

    validation_rows = []
    for group, frame in analyzed.groupby("validation_group"):
        validation_rows.append({"validation_group": group,
                                **paired_summary(frame, "before_findings", "after_findings")})
    validation_summary = pd.DataFrame(validation_rows)
    validation_summary.to_csv(RESULTS_ROOT / "checkstyle_validation_group_subgroups.csv", index=False)

    display(statistical_summary)
    display(subgroup_summary)
    display(validation_summary)
    """),
    markdown("""
    ## 8. Plot and export reproducibility summary
    """),
    code("""
    counts = analyzed["outcome"].value_counts().reindex(
        ["IMPROVED", "UNCHANGED", "WORSENED"], fill_value=0
    )
    axis = counts.plot(kind="bar", figsize=(7, 4.5),
                       color=["#146c5a", "#9a9588", "#a64343"],
                       title="Checkstyle outcome by unique snippet history")
    axis.set_xlabel("")
    axis.set_ylabel("Histories")
    axis.tick_params(axis="x", rotation=0)
    axis.figure.tight_layout()
    axis.figure.savefig(RESULTS_ROOT / "checkstyle_outcomes.png", dpi=180, bbox_inches="tight")
    plt.show()

    coverage = pair_metrics.groupby(["status", "parse_mode"], dropna=False).size().rename(
        "histories"
    ).reset_index()
    coverage.to_csv(RESULTS_ROOT / "checkstyle_parse_coverage.csv", index=False)
    dataset_exclusions.to_csv(
        RESULTS_ROOT / "checkstyle_dataset_exclusions.csv", index=False
    )
    summary = {
        "checkstyle_version": CHECKSTYLE_VERSION,
        "manual_validation_filter": "None (full 793-recommendation scope)",
        "scoped_pairs": int(len(pairs)),
        "analyzed_pairs": int(len(analyzed)),
        "parse_excluded_pairs": int((pair_metrics["status"] == "PARSE_EXCLUDED").sum()),
        "identical_pairs": int((pairs["Identical Before After"] == "YES").sum()),
        "study_row_mappings": int(len(mapping)),
        "dataset_level_exclusions": int(len(dataset_exclusions)),
        "retained_findings": int(len(findings)),
        "configuration_sha256": hashlib.sha256(CONFIG_PATH.read_bytes()).hexdigest(),
    }
    (RESULTS_ROOT / "checkstyle_run_summary.json").write_text(
        json.dumps(summary, indent=2), encoding="utf-8"
    )
    display(coverage)
    print(json.dumps(summary, indent=2))
    print("Results written to:", RESULTS_ROOT)
    """),
    markdown("""
    ## Interpretation checklist

    - Treat findings as style/readability indicators, not defects.
    - Report parsing coverage and all exclusions.
    - Interpret p-values with effect sizes and improved/unchanged/worsened counts;
      the two primary rows are Holm-corrected (`holm_p`).
    - This full scope mixes accepted, rejected, and mixed-validation
      recommendations; always check `validation_group` before generalizing
      a finding to "useful recommendations."
    - Inspect rule-level transitions; a total can hide opposing rule changes.
    - Manually audit a sample of findings, especially wrapper-sensitive checks.
    - Preserve the pinned JAR version, configuration hash, raw reports, and logs.
    """),
]

notebook = {
    "cells": cells,
    "metadata": {
        "kernelspec": {"display_name": "Python 3", "language": "python", "name": "python3"},
        "language_info": {"name": "python", "version": "3"},
    },
    "nbformat": 4,
    "nbformat_minor": 5,
}
OUTPUT.write_text(json.dumps(notebook, indent=1) + "\n", encoding="utf-8")
print(f"Wrote {OUTPUT}")
