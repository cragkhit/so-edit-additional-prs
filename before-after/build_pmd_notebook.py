#!/usr/bin/env python3
"""Generate the PMD before-after analysis notebook without third-party packages."""

import json
from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parent


def markdown(source):
    return {"cell_type": "markdown", "metadata": {}, "source": dedent(source).strip().splitlines(True)}


def code(source):
    return {
        "cell_type": "code",
        "execution_count": None,
        "metadata": {},
        "outputs": [],
        "source": dedent(source).strip().splitlines(True),
    }


cells = [
    markdown(
        """
        # Reduced PMD Before–After Analysis of Stack Overflow Snippets

        This notebook compares reduced PMD bug-risk and performance findings for the subset of original and
        recent/latest Stack Overflow snippet pairs whose manual-validation
        value is exactly `Agress Yes`.

        Design principles:

        - The unique SO question/code-block history is the independent unit.
        - `Agree No` rows are outside the analysis scope.
        - Repeated `Agress Yes` study rows are deduplicated by `Snippet ID`.
        - Original and after versions must use the same parse wrapper.
        - Wrapper-only findings are removed.
        - Bug-risk and performance indicators are reported independently.
        - Raw violation counts and violations per 100 non-comment lines are
          reported separately for each dimension.
        - Identical revisions remain valid zero-change observations.
        - PMD findings are static indicators, not a complete measure of
          software quality.

        Run this notebook from the `before-after/` directory.
        """
    ),
    markdown(
        """
        ## 1. Python dependencies

        The analysis uses pandas for tables, SciPy for paired tests, and
        matplotlib for plots. The first code cell installs any missing
        packages into the notebook environment.
        """
    ),
    code(
        """
        %pip install -q pandas scipy matplotlib

        import csv
        import hashlib
        import json
        import math
        import os
        import re
        import shutil
        import subprocess
        import urllib.request
        import zipfile
        from collections import Counter, defaultdict
        from pathlib import Path
        import xml.etree.ElementTree as ET

        import matplotlib.pyplot as plt
        import pandas as pd
        from scipy import stats

        pd.set_option("display.max_columns", 100)
        pd.set_option("display.max_colwidth", 120)
        """
    ),
    markdown(
        """
        ## 2. Reproducible configuration

        PMD is pinned to a specific release. If `PMD_HOME` is defined, that
        installation is used; otherwise the notebook downloads the pinned
        official binary distribution into `tools/`.

        `JAVA_LANGUAGE_VERSION` is fixed for both sides of every pair. Change
        it only as a documented sensitivity analysis.
        """
    ),
    code(
        """
        ROOT = Path.cwd().resolve()
        DATASET = ROOT / "dataset"
        MANIFEST_PATH = DATASET / "agress_yes_pairs.csv"
        MAPPING_PATH = DATASET / "study_pair_mapping.csv"
        RULESET_PATH = ROOT / "pmd-ruleset-reduced.xml"
        WORK_ROOT = ROOT / "work" / "pmd_reduced"
        RESULTS_ROOT = ROOT / "results" / "pmd_reduced"
        TOOLS_ROOT = ROOT / "tools"

        PMD_VERSION = "7.25.0"
        JAVA_LANGUAGE_VERSION = "java-17"
        BUG_RISK_RULES = {
            "BrokenNullCheck", "ComparisonWithNaN", "UseEqualsToCompareStrings",
            "CompareObjectsWithEquals", "AvoidDecimalLiteralsInBigDecimalConstructor",
            "CloseResource", "UseTryWithResources", "CheckSkipResult",
            "AssignmentInOperand", "EmptyCatchBlock", "PreserveStackTrace",
            "IdenticalCatchBranches",
        }
        PERFORMANCE_RULES = {
            "AppendCharacterWithChar", "AvoidArrayLoops",
            "ConsecutiveAppendsShouldReuse", "InefficientEmptyStringCheck",
            "StringInstantiation", "UseIndexOfChar", "UseStringBufferForStringAppends",
        }
        RULE_DIMENSION = {
            **{rule: "bug_risk" for rule in BUG_RISK_RULES},
            **{rule: "performance" for rule in PERFORMANCE_RULES},
        }
        PMD_URL = (
            "https://github.com/pmd/pmd/releases/download/"
            f"pmd_releases%2F{PMD_VERSION}/pmd-dist-{PMD_VERSION}-bin.zip"
        )

        RESULTS_ROOT.mkdir(parents=True, exist_ok=True)
        WORK_ROOT.mkdir(parents=True, exist_ok=True)
        TOOLS_ROOT.mkdir(parents=True, exist_ok=True)

        assert MANIFEST_PATH.is_file(), MANIFEST_PATH
        assert MAPPING_PATH.is_file(), MAPPING_PATH
        assert RULESET_PATH.is_file(), RULESET_PATH
        """
    ),
    code(
        """
        def find_or_install_pmd():
            configured = os.environ.get("PMD_HOME")
            candidates = []
            if configured:
                candidates.append(Path(configured) / "bin" / "pmd")
            system_pmd = shutil.which("pmd")
            if system_pmd:
                candidates.append(Path(system_pmd))
            candidates.append(TOOLS_ROOT / f"pmd-bin-{PMD_VERSION}" / "bin" / "pmd")

            for candidate in candidates:
                if candidate.is_file():
                    return candidate.resolve()

            archive = TOOLS_ROOT / f"pmd-dist-{PMD_VERSION}-bin.zip"
            print(f"Downloading PMD {PMD_VERSION} from the official release...")
            urllib.request.urlretrieve(PMD_URL, archive)
            with zipfile.ZipFile(archive) as bundle:
                bundle.extractall(TOOLS_ROOT)
            candidate = TOOLS_ROOT / f"pmd-bin-{PMD_VERSION}" / "bin" / "pmd"
            if not candidate.is_file():
                raise FileNotFoundError(f"PMD executable not found after extraction: {candidate}")
            candidate.chmod(candidate.stat().st_mode | 0o111)
            return candidate.resolve()


        PMD = find_or_install_pmd()
        version_result = subprocess.run([str(PMD), "--version"], text=True, capture_output=True)
        print(version_result.stdout or version_result.stderr)
        print("PMD executable:", PMD)
        """
    ),
    markdown(
        """
        ## 3. Apply the `Agress Yes` scope and deduplicate

        The spelling `Agress Yes` is intentional because it is the exact value
        in the source data. The filter is applied to the study-row mapping
        before deduplication. Dataset-excluded rows are retained for the audit
        output but are not passed to PMD.

        Several GitHub study rows may point to the same Stack Overflow snippet
        history. Those rows are collapsed to one independent observation per
        `Snippet ID`. If an accepted history appears under both recommendation
        types, its subgroup is recorded as `MULTIPLE_TYPES`.
        """
    ),
    code(
        """
        manifest = pd.read_csv(MANIFEST_PATH, dtype=str, keep_default_na=False)
        mapping = pd.read_csv(MAPPING_PATH, dtype=str, keep_default_na=False)

        accepted_rows = mapping.loc[
            mapping["Final Manual Validation"] == "Agress Yes"
        ].copy()
        accepted_eligible_rows = accepted_rows.loc[
            accepted_rows["Dataset Status"] == "ELIGIBLE"
        ].copy()
        accepted_excluded_rows = accepted_rows.loc[
            accepted_rows["Dataset Status"] != "ELIGIBLE"
        ].copy()

        eligible = (
            manifest.rename(
                columns={
                    "Accepted Study Row Count": "accepted_study_rows",
                    "Recommendation Group": "recommendation_group",
                }
            )
            .copy()
        )

        assert len(accepted_rows) == 391
        assert len(accepted_eligible_rows) == 387
        assert eligible["Snippet ID"].is_unique
        assert len(eligible) == 125

        print("All study rows:", len(mapping))
        print("Agress Yes study rows:", len(accepted_rows))
        print("Eligible Agress Yes study rows:", len(accepted_eligible_rows))
        print("Excluded Agress Yes study rows:", len(accepted_excluded_rows))
        print("Unique eligible Agress Yes snippet pairs:", len(eligible))
        print("Identical pairs:", (eligible["Identical Before After"] == "YES").sum())
        display(eligible["recommendation_group"].value_counts().rename("histories"))
        """
    ),
    markdown(
        """
        ## 4. Create symmetric PMD inputs

        Isolated SO snippets may be complete compilation units, class members,
        or method-body statements. Three deterministic variants are prepared
        for both versions:

        1. `RAW`
        2. `CLASS_MEMBER`
        3. `METHOD_BODY`

        The first mode successfully parsed by PMD on **both** sides is selected.
        Prefix and suffix lines are recorded so wrapper-only violations can be
        removed and reported line numbers can be mapped back to the snippet.
        """
    ),
    code(
        r'''
        WRAPPERS = {
            "RAW": ("", ""),
            "CLASS_MEMBER": ("class SnippetWrapper {\n", "\n}\n"),
            "METHOD_BODY": (
                "class SnippetWrapper {\n"
                "    void snippetMethod() throws Exception {\n",
                "\n    }\n}\n",
            ),
        }
        MODE_ORDER = list(WRAPPERS)
        input_metadata = []

        for mode, (prefix, suffix) in WRAPPERS.items():
            for version in ("before", "after"):
                target_dir = WORK_ROOT / "inputs" / mode / version
                target_dir.mkdir(parents=True, exist_ok=True)
                for old_file in target_dir.glob("*.java"):
                    old_file.unlink()

                source_column = "Before Dataset Path" if version == "before" else "After Dataset Path"
                for row in eligible.to_dict("records"):
                    snippet_id = row["Snippet ID"]
                    source = ROOT / row[source_column]
                    raw = source.read_text(encoding="utf-8", errors="replace")
                    target = target_dir / f"{snippet_id}.java"
                    target.write_text(prefix + raw + suffix, encoding="utf-8")
                    source_lines = len(raw.splitlines())
                    input_metadata.append(
                        {
                            "snippet_id": snippet_id,
                            "version": version,
                            "mode": mode,
                            "input_path": str(target.resolve()),
                            "prefix_lines": len(prefix.splitlines()),
                            "snippet_start_line": len(prefix.splitlines()) + 1,
                            "snippet_end_line": len(prefix.splitlines()) + source_lines,
                            "source_lines": source_lines,
                        }
                    )

        input_metadata = pd.DataFrame(input_metadata)
        input_metadata.to_csv(WORK_ROOT / "input_metadata.csv", index=False)
        display(input_metadata.head())
        '''
    ),
    markdown(
        """
        ## 5. Run PMD

        PMD runs once for each wrapper/version combination. XML reports and
        logs are preserved under `work/pmd/reports/`.
        """
    ),
    code(
        """
        REPORT_ROOT = WORK_ROOT / "reports"
        REPORT_ROOT.mkdir(parents=True, exist_ok=True)


        def run_pmd(mode, version):
            source_dir = WORK_ROOT / "inputs" / mode / version
            report_path = REPORT_ROOT / f"{mode.lower()}_{version}.xml"
            log_path = REPORT_ROOT / f"{mode.lower()}_{version}.log"
            command = [
                str(PMD), "check",
                "-d", str(source_dir),
                "-R", str(RULESET_PATH),
                "-f", "xml",
                "-r", str(report_path),
                "--no-cache",
                "--no-fail-on-violation",
                "--use-version", JAVA_LANGUAGE_VERSION,
            ]
            completed = subprocess.run(command, text=True, capture_output=True)
            log_path.write_text(
                "COMMAND: " + " ".join(command) + "\\n\\n"
                + completed.stdout + "\\n" + completed.stderr,
                encoding="utf-8",
            )
            if not report_path.is_file():
                raise RuntimeError(
                    f"PMD did not create {report_path}. See {log_path}. "
                    f"Exit code: {completed.returncode}"
                )
            print(mode, version, "exit", completed.returncode)
            return report_path


        report_paths = {
            (mode, version): run_pmd(mode, version)
            for mode in MODE_ORDER
            for version in ("before", "after")
        }
        """
    ),
    markdown(
        """
        ## 6. Parse reports and select a common mode

        A mode is successful for a file when PMD reports no processing error.
        Selection is pairwise: a mode is usable only when both versions parse
        under that same mode.
        """
    ),
    code(
        r'''
        def local_name(tag):
            return tag.rsplit("}", 1)[-1]


        def parse_pmd_xml(path, mode, version):
            root = ET.parse(path).getroot()
            violations = []
            errors = []
            for node in root.iter():
                kind = local_name(node.tag)
                if kind == "file":
                    filename = Path(node.attrib.get("name", "")).name
                    snippet_id = Path(filename).stem
                    for child in node:
                        if local_name(child.tag) != "violation":
                            continue
                        violations.append(
                            {
                                "snippet_id": snippet_id,
                                "version": version,
                                "mode": mode,
                                "rule": child.attrib.get("rule", ""),
                                "ruleset": child.attrib.get("ruleset", ""),
                                "priority": int(child.attrib.get("priority", "0")),
                                "begin_line_wrapped": int(child.attrib.get("beginline", "0")),
                                "end_line_wrapped": int(child.attrib.get("endline", "0")),
                                "message": " ".join("".join(child.itertext()).split()),
                                "external_info_url": child.attrib.get("externalInfoUrl", ""),
                            }
                        )
                elif kind in {"error", "processingError"}:
                    filename = Path(
                        node.attrib.get("filename")
                        or node.attrib.get("file")
                        or node.attrib.get("name", "")
                    ).name
                    errors.append(
                        {
                            "snippet_id": Path(filename).stem,
                            "version": version,
                            "mode": mode,
                            "message": node.attrib.get("msg")
                            or node.attrib.get("message")
                            or " ".join("".join(node.itertext()).split()),
                        }
                    )
            return violations, errors


        all_violations = []
        all_errors = []
        for (mode, version), path in report_paths.items():
            violations, errors = parse_pmd_xml(path, mode, version)
            all_violations.extend(violations)
            all_errors.extend(errors)

        raw_violations = pd.DataFrame(all_violations)
        processing_errors = pd.DataFrame(all_errors)
        if processing_errors.empty:
            processing_errors = pd.DataFrame(columns=["snippet_id", "version", "mode", "message"])

        error_keys = set(
            processing_errors[["snippet_id", "version", "mode"]]
            .itertuples(index=False, name=None)
        )
        selected_modes = []
        for snippet_id in eligible["Snippet ID"]:
            selected = ""
            for mode in MODE_ORDER:
                before_ok = (snippet_id, "before", mode) not in error_keys
                after_ok = (snippet_id, "after", mode) not in error_keys
                if before_ok and after_ok:
                    selected = mode
                    break
            selected_modes.append({"snippet_id": snippet_id, "selected_mode": selected})

        selected_modes = pd.DataFrame(selected_modes)
        display(selected_modes["selected_mode"].replace("", "NO_COMMON_MODE").value_counts())
        display(processing_errors.head(10))
        '''
    ),
    markdown(
        """
        ## 7. Remove wrapper findings and map line numbers

        Only violations whose starting line falls inside the original snippet
        region are retained. Wrapped line numbers are converted back to
        one-based snippet-relative lines.
        """
    ),
    code(
        """
        if raw_violations.empty:
            violations = pd.DataFrame(
                columns=[
                    "snippet_id", "version", "mode", "rule", "ruleset", "priority",
                    "begin_line_wrapped", "end_line_wrapped", "message", "external_info_url"
                ]
            )
        else:
            violations = raw_violations.merge(
                selected_modes, on="snippet_id", how="inner"
            )
            violations = violations.loc[violations["mode"] == violations["selected_mode"]].copy()
            violations = violations.merge(
                input_metadata,
                on=["snippet_id", "version", "mode"],
                how="left",
                validate="many_to_one",
            )
            violations = violations.loc[
                violations["begin_line_wrapped"].between(
                    violations["snippet_start_line"],
                    violations["snippet_end_line"],
                )
            ].copy()
            violations["begin_line"] = (
                violations["begin_line_wrapped"] - violations["prefix_lines"]
            )
            violations["end_line"] = (
                violations["end_line_wrapped"] - violations["prefix_lines"]
            ).clip(lower=1)

        violations["analysis_dimension"] = violations["rule"].map(RULE_DIMENSION)
        if violations["analysis_dimension"].isna().any():
            unknown = sorted(violations.loc[violations["analysis_dimension"].isna(), "rule"].unique())
            raise RuntimeError(f"Rules missing dimension assignment: {unknown}")

        violation_columns = [
            "snippet_id", "version", "mode", "rule", "ruleset", "priority",
            "analysis_dimension", "begin_line", "end_line", "message", "external_info_url"
        ]
        violations = violations.reindex(columns=violation_columns)
        violations.to_csv(RESULTS_ROOT / "pmd_violations_long.csv", index=False)
        print("Retained snippet violations:", len(violations))
        display(violations.head())
        """
    ),
    markdown(
        """
        ## 8. Build paired metrics

        `code_lines` is a physical nonblank, non-comment line count computed
        with a small Java-aware scanner. It is not PMD's NCSS metric.
        """
    ),
    code(
        r'''
        def physical_code_lines(text):
            lines = set()
            line = 1
            i = 0
            state = "code"
            line_has_code = False
            quote = ""
            escaped = False
            while i < len(text):
                ch = text[i]
                nxt = text[i + 1] if i + 1 < len(text) else ""
                if ch == "\n":
                    if line_has_code:
                        lines.add(line)
                    line += 1
                    line_has_code = False
                    if state == "line_comment":
                        state = "code"
                    i += 1
                    continue
                if state == "line_comment":
                    i += 1
                    continue
                if state == "block_comment":
                    if ch == "*" and nxt == "/":
                        state = "code"
                        i += 2
                    else:
                        i += 1
                    continue
                if state == "string":
                    line_has_code = True
                    if escaped:
                        escaped = False
                    elif ch == "\\":
                        escaped = True
                    elif ch == quote:
                        state = "code"
                    i += 1
                    continue
                if ch == "/" and nxt == "/":
                    state = "line_comment"
                    i += 2
                elif ch == "/" and nxt == "*":
                    state = "block_comment"
                    i += 2
                elif ch in {'"', "'"}:
                    state = "string"
                    quote = ch
                    line_has_code = True
                    i += 1
                else:
                    if not ch.isspace():
                        line_has_code = True
                    i += 1
            if line_has_code:
                lines.add(line)
            return len(lines)


        metrics_rows = []
        violation_counts = (
            violations.groupby(["snippet_id", "version"]).size().rename("violations")
            if not violations.empty
            else pd.Series(dtype=int, name="violations")
        )
        dimension_counts = (
            violations.groupby(["snippet_id", "version", "analysis_dimension"])
            .size().rename("violations")
            if not violations.empty
            else pd.Series(dtype=int, name="violations")
        )

        for row in eligible.to_dict("records"):
            snippet_id = row["Snippet ID"]
            selected_mode = selected_modes.loc[
                selected_modes["snippet_id"] == snippet_id, "selected_mode"
            ].iloc[0]
            metric = {
                "snippet_id": snippet_id,
                "status": "ANALYZED" if selected_mode else "PARSE_EXCLUDED",
                "parse_mode": selected_mode,
                "accepted_study_rows": int(row["accepted_study_rows"]),
                "recommendation_group": row["recommendation_group"],
                "identical_before_after": row["Identical Before After"],
            }
            for version, column in (
                ("before", "Before Dataset Path"),
                ("after", "After Dataset Path"),
            ):
                source = ROOT / row[column]
                text = source.read_text(encoding="utf-8", errors="replace")
                loc = physical_code_lines(text)
                count = int(violation_counts.get((snippet_id, version), 0))
                metric[f"{version}_code_lines"] = loc
                metric[f"{version}_violations"] = count
                metric[f"{version}_violations_per_100_lines"] = (
                    100 * count / loc if loc else math.nan
                )
                for dimension in ("bug_risk", "performance"):
                    dimension_count = int(
                        dimension_counts.get((snippet_id, version, dimension), 0)
                    )
                    metric[f"{version}_{dimension}_violations"] = dimension_count
                    metric[f"{version}_{dimension}_violations_per_100_lines"] = (
                        100 * dimension_count / loc if loc else math.nan
                    )
            metric["delta_violations"] = (
                metric["after_violations"] - metric["before_violations"]
            )
            metric["delta_violations_per_100_lines"] = (
                metric["after_violations_per_100_lines"]
                - metric["before_violations_per_100_lines"]
            )
            for dimension in ("bug_risk", "performance"):
                metric[f"delta_{dimension}_violations"] = (
                    metric[f"after_{dimension}_violations"]
                    - metric[f"before_{dimension}_violations"]
                )
                metric[f"delta_{dimension}_violations_per_100_lines"] = (
                    metric[f"after_{dimension}_violations_per_100_lines"]
                    - metric[f"before_{dimension}_violations_per_100_lines"]
                )
                dimension_delta = metric[f"delta_{dimension}_violations"]
                metric[f"{dimension}_outcome"] = (
                    "IMPROVED" if dimension_delta < 0
                    else "WORSENED" if dimension_delta > 0
                    else "UNCHANGED"
                )
            metric["outcome"] = (
                "IMPROVED" if metric["delta_violations"] < 0
                else "WORSENED" if metric["delta_violations"] > 0
                else "UNCHANGED"
            )
            metrics_rows.append(metric)

        pair_metrics = pd.DataFrame(metrics_rows)
        pair_metrics.to_csv(RESULTS_ROOT / "pmd_pair_metrics.csv", index=False)
        display(pair_metrics.head())
        display(pair_metrics["status"].value_counts())
        display(pair_metrics.loc[pair_metrics["status"] == "ANALYZED", "outcome"].value_counts())
        '''
    ),
    markdown(
        """
        ## 9. Rule-level transitions

        A rule is `removed` when it appears only in the original version,
        `introduced` when it appears only in the after version, and `preserved`
        when it appears in both. Counts refer to snippet histories, not the
        number of individual warning locations.
        """
    ),
    code(
        """
        analyzed_ids = set(pair_metrics.loc[pair_metrics["status"] == "ANALYZED", "snippet_id"])
        rules = sorted(violations["rule"].unique()) if not violations.empty else []
        presence = set(
            violations[["snippet_id", "version", "rule"]].drop_duplicates()
            .itertuples(index=False, name=None)
        )
        transitions = []
        for rule in rules:
            removed = introduced = preserved = absent = 0
            for snippet_id in analyzed_ids:
                before_has = (snippet_id, "before", rule) in presence
                after_has = (snippet_id, "after", rule) in presence
                if before_has and after_has:
                    preserved += 1
                elif before_has:
                    removed += 1
                elif after_has:
                    introduced += 1
                else:
                    absent += 1
            transitions.append(
                {
                    "rule": rule,
                    "analysis_dimension": RULE_DIMENSION[rule],
                    "removed_histories": removed,
                    "introduced_histories": introduced,
                    "preserved_histories": preserved,
                    "absent_histories": absent,
                    "net_removed": removed - introduced,
                }
            )

        rule_transitions = pd.DataFrame(transitions)
        if not rule_transitions.empty:
            rule_transitions = rule_transitions.sort_values(
                ["net_removed", "removed_histories"], ascending=False
            )
        rule_transitions.to_csv(RESULTS_ROOT / "pmd_rule_transitions.csv", index=False)
        display(rule_transitions.head(20))
        """
    ),
    markdown(
        """
        ## 10. Paired descriptive statistics and Wilcoxon tests

        Negative deltas indicate fewer PMD findings in the after version.
        Statistical significance must be interpreted together with effect size,
        confidence intervals, and the improved/unchanged/worsened proportions.
        """
    ),
    code(
        """
        analyzed = pair_metrics.loc[pair_metrics["status"] == "ANALYZED"].copy()


        def paired_summary(frame, before_column, after_column):
            before = frame[before_column].astype(float)
            after = frame[after_column].astype(float)
            delta = after - before
            nonzero = delta[delta != 0]
            if len(nonzero):
                wilcoxon = stats.wilcoxon(nonzero, alternative="two-sided", method="auto")
                positive_ranks = stats.rankdata(abs(nonzero))[nonzero > 0].sum()
                negative_ranks = stats.rankdata(abs(nonzero))[nonzero < 0].sum()
                rank_biserial = (positive_ranks - negative_ranks) / (positive_ranks + negative_ranks)
            else:
                wilcoxon = None
                rank_biserial = 0.0
            return {
                "n": len(frame),
                "before_median": before.median(),
                "before_iqr": before.quantile(0.75) - before.quantile(0.25),
                "after_median": after.median(),
                "after_iqr": after.quantile(0.75) - after.quantile(0.25),
                "delta_median": delta.median(),
                "improved_n": int((delta < 0).sum()),
                "unchanged_n": int((delta == 0).sum()),
                "worsened_n": int((delta > 0).sum()),
                "wilcoxon_statistic": wilcoxon.statistic if wilcoxon else math.nan,
                "wilcoxon_p": wilcoxon.pvalue if wilcoxon else math.nan,
                "rank_biserial_after_minus_before": rank_biserial,
            }


        statistical_rows = [
                {
                    "analysis_dimension": "combined_reduced",
                    "metric": "Raw PMD violations",
                    **paired_summary(analyzed, "before_violations", "after_violations"),
                },
                {
                    "analysis_dimension": "combined_reduced",
                    "metric": "PMD violations per 100 code lines",
                    **paired_summary(
                        analyzed,
                        "before_violations_per_100_lines",
                        "after_violations_per_100_lines",
                    ),
                },
        ]
        for dimension in ("bug_risk", "performance"):
            statistical_rows.extend(
                [
                    {
                        "analysis_dimension": dimension,
                        "metric": "Raw PMD violations",
                        **paired_summary(
                            analyzed,
                            f"before_{dimension}_violations",
                            f"after_{dimension}_violations",
                        ),
                    },
                    {
                        "analysis_dimension": dimension,
                        "metric": "PMD violations per 100 code lines",
                        **paired_summary(
                            analyzed,
                            f"before_{dimension}_violations_per_100_lines",
                            f"after_{dimension}_violations_per_100_lines",
                        ),
                    },
                ]
            )
        statistical_summary = pd.DataFrame(statistical_rows)
        statistical_summary.to_csv(RESULTS_ROOT / "pmd_statistical_summary.csv", index=False)
        display(statistical_summary)
        """
    ),
    markdown(
        """
        ## 11. Recommendation-type subgroup analysis

        Every history in the primary analysis has at least one `Agress Yes`
        study row. Bug Fixing and Improving Code histories are summarized
        separately. Histories assigned both labels are reported as
        `MULTIPLE_TYPES` and excluded from the single-type subgroup tests.
        """
    ),
    code(
        """
        subgroup = analyzed.loc[
            analyzed["recommendation_group"].isin(["Bug Fixing", "Improving Code"])
        ].copy()
        subgroup_summary = (
            subgroup.groupby("recommendation_group")
            .agg(
                histories=("snippet_id", "size"),
                median_delta=("delta_violations", "median"),
                mean_delta=("delta_violations", "mean"),
                improved=("outcome", lambda s: (s == "IMPROVED").sum()),
                unchanged=("outcome", lambda s: (s == "UNCHANGED").sum()),
                worsened=("outcome", lambda s: (s == "WORSENED").sum()),
            )
            .reset_index()
        )
        subgroup_tests = []
        for group_name, group_data in subgroup.groupby("recommendation_group"):
            for dimension in ("bug_risk", "performance"):
                subgroup_tests.append(
                    {
                        "recommendation_group": group_name,
                        "analysis_dimension": dimension,
                        **paired_summary(
                            group_data,
                            f"before_{dimension}_violations",
                            f"after_{dimension}_violations",
                        ),
                    }
                )
        subgroup_statistical_summary = pd.DataFrame(subgroup_tests)
        subgroup_summary.to_csv(
            RESULTS_ROOT / "pmd_recommendation_subgroup_summary.csv", index=False
        )
        subgroup_statistical_summary.to_csv(
            RESULTS_ROOT / "pmd_recommendation_subgroup_tests.csv", index=False
        )
        display(subgroup_summary)
        display(subgroup_statistical_summary)
        print(
            "MULTIPLE_TYPES histories excluded from subgroup tests:",
            (analyzed["recommendation_group"] == "MULTIPLE_TYPES").sum(),
        )
        """
    ),
    markdown(
        """
        ## 12. Visual summaries
        """
    ),
    code(
        """
        figure, axes = plt.subplots(1, 2, figsize=(12, 4.5), sharey=True)
        for axis, dimension, title in zip(
            axes,
            ("bug_risk", "performance"),
            ("Potential bug-risk indicators", "Performance indicators"),
        ):
            outcome_counts = analyzed[f"{dimension}_outcome"].value_counts().reindex(
                ["IMPROVED", "UNCHANGED", "WORSENED"], fill_value=0
            )
            outcome_counts.plot(
                kind="bar",
                ax=axis,
                color=["#146c5a", "#9a9588", "#a64343"],
                title=title,
            )
            axis.set_xlabel("")
            axis.set_ylabel("Histories")
            axis.tick_params(axis="x", rotation=0)

        figure.tight_layout()
        figure.savefig(RESULTS_ROOT / "pmd_reduced_outcomes.png", dpi=180, bbox_inches="tight")
        plt.show()
        """
    ),
    markdown(
        """
        ## 13. Export audit and coverage summaries
        """
    ),
    code(
        """
        parse_coverage = (
            pair_metrics.groupby(["status", "parse_mode"], dropna=False)
            .size()
            .rename("histories")
            .reset_index()
        )
        parse_coverage.to_csv(RESULTS_ROOT / "pmd_parse_coverage.csv", index=False)
        processing_errors.to_csv(RESULTS_ROOT / "pmd_processing_errors.csv", index=False)
        accepted_excluded_rows.to_csv(
            RESULTS_ROOT / "pmd_agress_yes_dataset_exclusions.csv", index=False
        )

        run_summary = {
            "pmd_version": PMD_VERSION,
            "java_language_version": JAVA_LANGUAGE_VERSION,
            "scoped_manifest_pairs": int(len(manifest)),
            "manual_validation_filter": "Agress Yes",
            "agress_yes_study_rows": int(len(accepted_rows)),
            "eligible_agress_yes_study_rows": int(len(accepted_eligible_rows)),
            "excluded_agress_yes_study_rows": int(len(accepted_excluded_rows)),
            "unique_eligible_agress_yes_pairs": int(len(eligible)),
            "analyzed_pairs": int((pair_metrics["status"] == "ANALYZED").sum()),
            "parse_excluded_pairs": int((pair_metrics["status"] == "PARSE_EXCLUDED").sum()),
            "identical_pairs": int((eligible["Identical Before After"] == "YES").sum()),
            "retained_violations": int(len(violations)),
            "bug_risk_violations": int((violations["analysis_dimension"] == "bug_risk").sum()),
            "performance_violations": int((violations["analysis_dimension"] == "performance").sum()),
            "ruleset_sha256": hashlib.sha256(RULESET_PATH.read_bytes()).hexdigest(),
        }
        (RESULTS_ROOT / "pmd_run_summary.json").write_text(
            json.dumps(run_summary, indent=2), encoding="utf-8"
        )

        display(parse_coverage)
        print(json.dumps(run_summary, indent=2))
        print("\\nResults written to:", RESULTS_ROOT)
        """
    ),
    markdown(
        """
        ## Interpretation checklist

        Before using the results in the manuscript:

        - Inspect processing errors and report PMD coverage.
        - Manually audit a random sample of removed and introduced findings.
        - Report raw findings and density; do not choose only the favorable one.
        - Keep identical edits as zero-change observations.
        - Correct for multiple testing if individual PMD rules are tested.
        - Do not interpret a PMD warning as a confirmed defect.
        - Describe PMD results as static quality indicators.
        - Preserve the PMD version, Java language version, ruleset hash, raw
          reports, and generated tables in the replication package.
        """
    ),
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

target = ROOT / "PMD_Before_After_Analysis.ipynb"
target.write_text(json.dumps(notebook, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"Wrote {target}")
