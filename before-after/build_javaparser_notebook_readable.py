#!/usr/bin/env python3
"""Generate a more heavily annotated, step-by-step version of the JavaParser
before-after analysis notebook.

This produces the *same* analysis as build_javaparser_notebook.py /
JavaParser_Before_After_Analysis.ipynb (same dataset scope, same wrapper
strategy, same metrics, same statistics), but split into smaller cells with
explanatory markdown and named helper functions, for a reader who finds the
original notebook's dense cells hard to follow. It writes to separate
work/ and results/ subfolders so running it never overwrites the original
notebook's output.
"""

import json
from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "JavaParser_Before_After_Analysis_Readable.ipynb"


def markdown(source):
    return {"cell_type": "markdown", "metadata": {}, "source": dedent(source).strip().splitlines(True)}


def code(source):
    return {"cell_type": "code", "execution_count": None, "metadata": {}, "outputs": [],
            "source": dedent(source).strip().splitlines(True)}


cells = [
    markdown("""
    # JavaParser Before–After Structural Analysis (Readable Edition)

    This notebook answers the same question as
    [`JavaParser_Before_After_Analysis.ipynb`](JavaParser_Before_After_Analysis.ipynb):
    *for the 125 unique, dataset-eligible Stack Overflow histories associated
    with `Agress Yes`, how do intrinsic AST-based structural measures change
    between the original snippet and its most recent revision?*

    It uses the exact same dataset scope, wrapper strategy, metric
    definitions, and statistical tests as the original notebook, so results
    from the two should match. The difference is presentation: every step
    here is its own cell with a short explanation of *why* the step exists,
    and the repetitive logic is pulled into small, named, documented
    functions instead of long inline blocks.

    To avoid clobbering the original notebook's output, this version reads
    the same input dataset but writes its working files and results under
    separate `work/javaparser_readable/` and `results/javaparser_readable/`
    folders.

    As in the original: these are structural indicators, not a composite
    quality score. Lower is not universally better — a bug fix can
    legitimately add branches, null checks, exception handling, or resource
    management.
    """),

    markdown("""
    ## 1. Imports

    Only pandas/SciPy for tables and paired statistics, matplotlib for the
    delta plots, and standard library modules for running the Java tools
    and hashing files for the reproducibility record.
    """),
    code("""
    %pip install -q pandas scipy matplotlib

    import hashlib
    import json
    import math
    import re
    import subprocess
    import urllib.request
    from pathlib import Path

    import matplotlib.pyplot as plt
    import pandas as pd
    from scipy import stats

    pd.set_option("display.max_columns", 100)
    pd.set_option("display.max_colwidth", 120)
    """),

    markdown("""
    ## 2. Paths and configuration

    `WORK_ROOT` holds intermediate files (generated `.java` variants,
    compiled extractor, raw per-mode CSVs). `RESULTS_ROOT` holds the final
    tables, plots, and reproducibility metadata. Both are namespaced
    `_readable` so they never collide with the original notebook's `work/`
    and `results/` folders.
    """),
    code("""
    ROOT = Path.cwd().resolve()
    DATASET = ROOT / "dataset"
    MANIFEST_PATH = DATASET / "agress_yes_pairs.csv"
    MAPPING_PATH = DATASET / "study_pair_mapping.csv"
    WORK_ROOT = ROOT / "work" / "javaparser_readable"
    RESULTS_ROOT = ROOT / "results" / "javaparser_readable"
    TOOLS_ROOT = ROOT / "tools"

    JAVAPARSER_VERSION = "3.28.1"
    JAVAPARSER_JAR = TOOLS_ROOT / f"javaparser-core-{JAVAPARSER_VERSION}.jar"
    JAVAPARSER_URL = (
        "https://repo1.maven.org/maven2/com/github/javaparser/javaparser-core/"
        f"{JAVAPARSER_VERSION}/javaparser-core-{JAVAPARSER_VERSION}.jar"
    )

    for directory in (WORK_ROOT, RESULTS_ROOT, TOOLS_ROOT):
        directory.mkdir(parents=True, exist_ok=True)
    for required_file in (MANIFEST_PATH, MAPPING_PATH):
        assert required_file.is_file(), f"Missing required input: {required_file}"
    print("Dataset manifest:", MANIFEST_PATH)
    print("Working directory:", WORK_ROOT)
    print("Results directory:", RESULTS_ROOT)
    """),

    code("""
    # JavaParser Core is pinned to a specific version and downloaded once
    # into tools/, shared with the other before-after notebooks.
    if not JAVAPARSER_JAR.is_file():
        print(f"Downloading JavaParser {JAVAPARSER_VERSION}...")
        urllib.request.urlretrieve(JAVAPARSER_URL, JAVAPARSER_JAR)
    print("JavaParser JAR ready:", JAVAPARSER_JAR)
    """),

    markdown("""
    ## 3. Load and validate the scoped dataset

    The analysis is restricted to the 125 before/after pairs whose
    `Final Manual Validation` is exactly `Agress Yes` (an intentional typo
    preserved from the source data) *and* whose `Dataset Status` is
    `ELIGIBLE`. The assertions below make that scope an explicit,
    checkable fact rather than an assumption buried in later code.
    """),
    code("""
    pairs = pd.read_csv(MANIFEST_PATH, dtype=str, keep_default_na=False).rename(
        columns={"Accepted Study Row Count": "accepted_study_rows",
                 "Recommendation Group": "recommendation_group"}
    )
    mapping = pd.read_csv(MAPPING_PATH, dtype=str, keep_default_na=False)

    accepted = mapping.loc[mapping["Final Manual Validation"] == "Agress Yes"].copy()
    accepted_exclusions = accepted.loc[accepted["Dataset Status"] != "ELIGIBLE"].copy()

    assert len(pairs) == 125 and pairs["Snippet ID"].is_unique, "Expected 125 unique scoped pairs"
    assert len(accepted) == 391, "Expected 391 Agress Yes study rows"
    assert len(accepted_exclusions) == 4, "Expected 4 Agress Yes rows outside dataset scope"

    print("Scoped before/after pairs:", len(pairs))
    print("Agress Yes study rows:", len(accepted), "| excluded from dataset:", len(accepted_exclusions))
    """),
    code("""
    # A quick look at the manifest structure before we start transforming it.
    display(pairs[["Snippet ID", "recommendation_group", "Identical Before After"]].head())
    display(pairs["recommendation_group"].value_counts().rename("histories"))
    """),

    markdown("""
    ## 4. Why snippets need a wrapper

    Stack Overflow code blocks are rarely complete, compilable Java files —
    a snippet might be a bare statement list, a single method, or a class
    member. JavaParser's default entry point expects a full compilation
    unit, so a snippet that is perfectly valid Java *in context* can fail
    to parse on its own.

    To give every snippet a fair chance, each one is tried three ways, from
    least to most wrapping:

    1. **RAW** — parsed exactly as written.
    2. **CLASS_MEMBER** — wrapped in `class SnippetWrapper { ... }`, for
       snippets that are field/method declarations rather than statements.
    3. **METHOD_BODY** — wrapped in a synthetic method body, for snippets
       that are bare statement sequences.

    The wrapper text itself must never be counted in the metrics, so its
    line count is tracked and, further down, the synthetic method it
    introduces is subtracted back out of `method_count`.
    """),
    code("""
    WRAPPERS = {
        "RAW": ("", ""),
        "CLASS_MEMBER": ("class SnippetWrapper {\\n", "\\n}\\n"),
        "METHOD_BODY": (
            "class SnippetWrapper {\\n    void snippetMethod() throws Exception {\\n",
            "\\n    }\\n}\\n",
        ),
    }
    # Order matters: this is also the preference order used later when a
    # pair parses successfully under more than one wrapper.
    MODE_ORDER = list(WRAPPERS)
    print("Wrapper modes, in preference order:", MODE_ORDER)
    """),

    code("""
    def write_wrapped_variant(pair_row, mode, version, prefix, suffix):
        \"\"\"Write one wrapped .java file for a single (pair, mode, version)
        combination and return its input-metadata record.\"\"\"
        source_column = "Before Dataset Path" if version == "before" else "After Dataset Path"
        raw_text = (ROOT / pair_row[source_column]).read_text(encoding="utf-8", errors="replace")

        target_dir = WORK_ROOT / "inputs" / mode / version
        target_dir.mkdir(parents=True, exist_ok=True)
        target_path = target_dir / f"{pair_row['Snippet ID']}.java"
        target_path.write_text(prefix + raw_text + suffix, encoding="utf-8")

        return {
            "snippet_id": pair_row["Snippet ID"], "version": version, "mode": mode,
            "input_path": str(target_path.resolve()),
            "artificial_methods": 1 if mode == "METHOD_BODY" else 0,
        }
    """),
    code("""
    # Clear out any stale variants from a previous run, then regenerate all
    # (mode x version x pair) combinations from the current dataset.
    for mode in MODE_ORDER:
        for version in ("before", "after"):
            variant_dir = WORK_ROOT / "inputs" / mode / version
            variant_dir.mkdir(parents=True, exist_ok=True)
            for stale_file in variant_dir.glob("*.java"):
                stale_file.unlink()

    input_rows = []
    for mode, (prefix, suffix) in WRAPPERS.items():
        for version in ("before", "after"):
            for pair_row in pairs.to_dict("records"):
                input_rows.append(write_wrapped_variant(pair_row, mode, version, prefix, suffix))

    input_metadata = pd.DataFrame(input_rows)
    input_metadata.to_csv(WORK_ROOT / "input_metadata.csv", index=False)
    print("Generated variants:", len(input_metadata), f"({len(pairs)} pairs x {len(MODE_ORDER)} modes x 2 versions)")
    """),

    markdown("""
    ## 5. The AST metric extractor

    Metrics are computed by a small, self-contained Java program (compiled
    against the JavaParser JAR) rather than in Python, because it needs
    direct access to the parsed AST. Given a directory of `.java` files, it
    writes one CSV row per file with:

    - whether the file parsed successfully, and the parser's error message
      if not;
    - counts of methods, parameters, local variables, `if`/loop/`switch`/
      `catch` statements;
    - a documented cyclomatic-complexity **proxy** (`1 + decisions`, where
      decisions are `if`, loops, catches, ternaries, `switch` labels, and
      boolean `&&`/`||`) — explicitly a study-specific approximation, not a
      claim of equivalence to another tool's cyclomatic complexity;
    - maximum control-flow nesting depth, abrupt exits (`return`/`break`/
      `continue`/`throw`), try/try-with-resources/empty-catch/empty-block
      counts, null-comparison counts, and an aggregate exception-handling
      count.

    A parse failure produces a **short row** — only `file,parse_ok,
    parse_error` — since there is no AST to measure. That is handled
    explicitly in Section 6 below rather than left as a downstream surprise.
    """),
    code("""
    EXTRACTOR_SOURCE = r\"\"\"
    import com.github.javaparser.*;
    import com.github.javaparser.ast.*;
    import com.github.javaparser.ast.body.*;
    import com.github.javaparser.ast.expr.*;
    import com.github.javaparser.ast.stmt.*;
    import java.io.*;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.*;
    import java.util.*;
    import java.util.stream.*;

    // Extracts one row of structural metrics per .java file in a directory.
    // Usage: java SnippetMetricsExtractor <input-dir> <output-csv>
    public class SnippetMetricsExtractor {

      // Counts every AST node of the given type anywhere under `root`.
      static long n(Node root, Class<? extends Node> type) {
        return root.stream().filter(type::isInstance).count();
      }

      // CSV-quotes a value, doubling embedded quotes and flattening newlines
      // (parser error messages can otherwise span multiple lines).
      static String q(String value) {
        return "\\"" + value.replace("\\"", "\\"\\"").replace("\\r", " ").replace("\\n", " ") + "\\"";
      }

      // Deepest nesting of control-flow constructs (if/for/while/switch/try/catch),
      // counting only nesting among control-flow nodes themselves.
      static int maxControlNesting(Node root) {
        Set<Class<?>> controls = Set.of(IfStmt.class, ForStmt.class, ForEachStmt.class,
          WhileStmt.class, DoStmt.class, SwitchStmt.class, TryStmt.class, CatchClause.class);
        int max = 0;
        for (Node node : root.getChildNodesByType(Node.class)) {
          if (!controls.contains(node.getClass())) continue;
          int depth = 1;
          Optional<Node> parent = node.getParentNode();
          while (parent.isPresent()) {
            if (controls.contains(parent.get().getClass())) depth++;
            parent = parent.get().getParentNode();
          }
          max = Math.max(max, depth);
        }
        return max;
      }

      // Non-default `switch` labels count as decision points for the
      // cyclomatic-complexity proxy.
      static long switchLabels(Node root) {
        return root.findAll(SwitchEntry.class).stream()
          .filter(entry -> !entry.getLabels().isEmpty()).count();
      }

      // Short-circuit boolean operators add a decision point each.
      static long booleanDecisions(Node root) {
        return root.findAll(BinaryExpr.class).stream().filter(expr ->
          expr.getOperator() == BinaryExpr.Operator.AND ||
          expr.getOperator() == BinaryExpr.Operator.OR).count();
      }

      // `x == null` / `x != null` style comparisons on either side.
      static long nullChecks(Node root) {
        return root.findAll(BinaryExpr.class).stream().filter(expr ->
          (expr.getOperator() == BinaryExpr.Operator.EQUALS ||
           expr.getOperator() == BinaryExpr.Operator.NOT_EQUALS) &&
          (expr.getLeft().isNullLiteralExpr() || expr.getRight().isNullLiteralExpr())).count();
      }

      static long localVariables(Node root) {
        return root.findAll(VariableDeclarationExpr.class).stream()
          .mapToLong(expr -> expr.getVariables().size()).sum();
      }

      static int maximumParameters(Node root) {
        return root.findAll(CallableDeclaration.class).stream()
          .mapToInt(call -> call.getParameters().size()).max().orElse(0);
      }

      // Parses one file and returns its CSV row: either the full metric
      // row on success, or a short file/parse_ok/parse_error row on failure.
      static String metrics(Path path) {
        ParserConfiguration config = new ParserConfiguration()
          .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        JavaParser parser = new JavaParser(config);
        try {
          ParseResult<CompilationUnit> result = parser.parse(path);
          if (!result.isSuccessful() || result.getResult().isEmpty()) {
            String problem = result.getProblems().stream().map(Object::toString)
              .collect(Collectors.joining(" | "));
            return q(path.getFileName().toString()) + ",false," + q(problem);
          }
          CompilationUnit root = result.getResult().get();
          long methods = n(root, MethodDeclaration.class) + n(root, ConstructorDeclaration.class);
          long ifs=n(root,IfStmt.class), fors=n(root,ForStmt.class), foreach=n(root,ForEachStmt.class);
          long whiles=n(root,WhileStmt.class), dos=n(root,DoStmt.class), catches=n(root,CatchClause.class);
          long conditionals=n(root,ConditionalExpr.class), labels=switchLabels(root), bools=booleanDecisions(root);
          long complexity = 1 + ifs + fors + foreach + whiles + dos + catches + conditionals + labels + bools;
          long parameters = root.findAll(CallableDeclaration.class).stream()
            .mapToLong(call -> call.getParameters().size()).sum();
          long tryResources = root.findAll(TryStmt.class).stream()
            .filter(t -> !t.getResources().isEmpty()).count();
          long emptyCatches = root.findAll(CatchClause.class).stream()
            .filter(c -> c.getBody().getStatements().isEmpty()).count();
          long emptyBlocks = root.findAll(BlockStmt.class).stream()
            .filter(b -> b.getStatements().isEmpty()).count();
          long abrupt = n(root,ReturnStmt.class)+n(root,BreakStmt.class)+n(root,ContinueStmt.class)+n(root,ThrowStmt.class);
          long exceptionHandling = catches + n(root,ThrowStmt.class) + n(root,TryStmt.class);
          long[] values = {methods, parameters, maximumParameters(root), localVariables(root),
            ifs, fors+foreach+whiles+dos, n(root,SwitchStmt.class), catches, complexity,
            maxControlNesting(root), abrupt, n(root,TryStmt.class), tryResources, n(root,ThrowStmt.class),
            emptyCatches, emptyBlocks, nullChecks(root), exceptionHandling};
          return q(path.getFileName().toString()) + ",true,\\"\\"" +
            Arrays.stream(values).mapToObj(Long::toString).collect(Collectors.joining(",", ",", ""));
        } catch (Exception ex) {
          return q(path.getFileName().toString()) + ",false," + q(ex.toString());
        }
      }

      public static void main(String[] args) throws Exception {
        Path directory=Paths.get(args[0]), output=Paths.get(args[1]);
        List<String> header=List.of("file","parse_ok","parse_error","method_count","parameter_count",
          "max_parameters","local_variable_count","if_count","loop_count","switch_count","catch_count",
          "cyclomatic_proxy","max_control_nesting","abrupt_exit_count","try_count","try_with_resources_count",
          "throw_count","empty_catch_count","empty_block_count","null_check_count","exception_handling_count");
        List<String> lines=new ArrayList<>(); lines.add(String.join(",",header));
        try (Stream<Path> paths=Files.list(directory)) {
          paths.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> lines.add(metrics(p)));
        }
        Files.write(output,lines,StandardCharsets.UTF_8);
      }
    }
    \"\"\"

    extractor_dir = WORK_ROOT / "extractor"
    extractor_dir.mkdir(parents=True, exist_ok=True)
    extractor_source_path = extractor_dir / "SnippetMetricsExtractor.java"
    extractor_source_path.write_text(EXTRACTOR_SOURCE, encoding="utf-8")
    print("Extractor source written to:", extractor_source_path)
    """),
    code("""
    compile_result = subprocess.run(
        ["javac", "-cp", str(JAVAPARSER_JAR), str(extractor_source_path)],
        text=True, capture_output=True,
    )
    if compile_result.returncode:
        raise RuntimeError(compile_result.stdout + compile_result.stderr)
    print("Compiled extractor:", extractor_dir)
    """),

    markdown("""
    ## 6. Run the extractor and select a common parse mode

    For each (wrapper mode, version) combination, run the extractor once
    over every snippet in that folder. The result is coerced to numeric
    dtypes **immediately** — a parse failure's missing metric columns
    become `NaN` rather than empty strings — so every downstream
    calculation on these columns can assume real numbers and never has to
    special-case "the column looks numeric but secretly isn't."
    """),
    code("""
    CLASSPATH = f"{JAVAPARSER_JAR}:{extractor_dir}"
    NON_NUMERIC_COLUMNS = {"file", "parse_ok", "parse_error"}

    def extract_metrics(mode, version):
        \"\"\"Run the compiled extractor over one (mode, version) input folder
        and return its results as a DataFrame with numeric metric columns.\"\"\"
        input_dir = WORK_ROOT / "inputs" / mode / version
        output_csv = WORK_ROOT / f"metrics_{mode}_{version}.csv"
        completed = subprocess.run(
            ["java", "-cp", CLASSPATH, "SnippetMetricsExtractor", str(input_dir), str(output_csv)],
            text=True, capture_output=True,
        )
        if completed.returncode:
            raise RuntimeError(completed.stdout + completed.stderr)

        frame = pd.read_csv(output_csv, keep_default_na=False)
        metric_columns = [c for c in frame.columns if c not in NON_NUMERIC_COLUMNS]
        frame[metric_columns] = frame[metric_columns].apply(pd.to_numeric, errors="coerce")

        frame["snippet_id"] = frame["file"].str.removesuffix(".java")
        frame["mode"], frame["version"] = mode, version
        return frame
    """),
    code("""
    extracted = pd.concat(
        [extract_metrics(mode, version) for mode in MODE_ORDER for version in ("before", "after")],
        ignore_index=True,
    )
    extracted.to_csv(RESULTS_ROOT / "javaparser_all_parse_attempts.csv", index=False)

    parse_ok_rate = extracted.groupby("mode")["parse_ok"].mean().rename("parse_ok_rate")
    print("Parse success rate by wrapper mode:")
    display(parse_ok_rate)
    """),
    code("""
    def first_common_mode(snippet_id, ok_pairs):
        \"\"\"Return the first wrapper mode (in MODE_ORDER preference) under
        which both the before and after version of `snippet_id` parsed
        successfully, or "" if no mode worked for both.\"\"\"
        return next(
            (mode for mode in MODE_ORDER
             if (snippet_id, "before", mode) in ok_pairs and (snippet_id, "after", mode) in ok_pairs),
            "",
        )

    ok_pairs = set(
        extracted.loc[extracted["parse_ok"], ["snippet_id", "version", "mode"]]
        .itertuples(index=False, name=None)
    )
    selected_modes = pd.DataFrame({
        "snippet_id": pairs["Snippet ID"],
        "selected_mode": [first_common_mode(sid, ok_pairs) for sid in pairs["Snippet ID"]],
    })

    display(selected_modes["selected_mode"].replace("", "NO_COMMON_MODE").value_counts())
    """),
    code("""
    # Keep only the rows for each pair's selected mode, then undo the
    # METHOD_BODY wrapper's synthetic method from the method count.
    selected_metrics = extracted.merge(selected_modes, on="snippet_id")
    selected_metrics = selected_metrics.loc[
        selected_metrics["mode"] == selected_metrics["selected_mode"]
    ].copy()
    selected_metrics.loc[selected_metrics["mode"] == "METHOD_BODY", "method_count"] -= 1
    print("Pairs with a usable common parse mode:", (selected_modes["selected_mode"] != "").sum(), "/", len(pairs))
    """),

    markdown("""
    ## 7. Build the paired before/after metrics table

    Non-comment line counts are computed directly from the dataset source
    files (not from the wrapped/parsed variants), since they should reflect
    the snippet as published, independent of parse success. Everything
    else in this table comes from `selected_metrics` for pairs that
    achieved a common parse mode; pairs that never found one are still
    included, marked `PARSE_EXCLUDED`, so exclusions stay visible instead
    of silently vanishing from the analysis.
    """),
    code("""
    def count_non_comment_lines(java_text):
        \"\"\"Strip block and line comments, then count remaining non-blank lines.\"\"\"
        without_block_comments = re.sub(r"/\\*.*?\\*/", "", java_text, flags=re.S)
        without_comments = re.sub(r"//.*", "", without_block_comments)
        return sum(bool(line.strip()) for line in without_comments.splitlines())

    METRICS = [
        "method_count", "parameter_count", "max_parameters", "local_variable_count",
        "if_count", "loop_count", "switch_count", "catch_count", "cyclomatic_proxy",
        "max_control_nesting", "abrupt_exit_count", "try_count", "try_with_resources_count",
        "throw_count", "empty_catch_count", "empty_block_count", "null_check_count",
        "exception_handling_count",
    ]
    """),
    code("""
    def build_pair_row(pair_row):
        \"\"\"Assemble one row of the paired metrics table for a single
        before/after history: code-line counts always, structural metrics
        and deltas only if a common parse mode was found.\"\"\"
        snippet_id = pair_row["Snippet ID"]
        mode = selected_modes.loc[selected_modes["snippet_id"] == snippet_id, "selected_mode"].iloc[0]
        row = {
            "snippet_id": snippet_id,
            "status": "ANALYZED" if mode else "PARSE_EXCLUDED",
            "parse_mode": mode,
            "recommendation_group": pair_row["recommendation_group"],
            "accepted_study_rows": int(pair_row["accepted_study_rows"]),
            "identical_before_after": pair_row["Identical Before After"],
        }

        for version, path_column in (("before", "Before Dataset Path"), ("after", "After Dataset Path")):
            source_text = (ROOT / pair_row[path_column]).read_text(encoding="utf-8", errors="replace")
            row[f"{version}_code_lines"] = count_non_comment_lines(source_text)
            if mode:
                metric_record = selected_metrics.loc[
                    (selected_metrics["snippet_id"] == snippet_id) & (selected_metrics["version"] == version)
                ].iloc[0]
                for metric in METRICS:
                    row[f"{version}_{metric}"] = int(metric_record[metric])

        if mode:
            row["delta_code_lines"] = row["after_code_lines"] - row["before_code_lines"]
            for metric in METRICS:
                row[f"delta_{metric}"] = row[f"after_{metric}"] - row[f"before_{metric}"]
        return row
    """),
    code("""
    pair_metrics = pd.DataFrame([build_pair_row(pair_row) for pair_row in pairs.to_dict("records")])
    pair_metrics.to_csv(RESULTS_ROOT / "javaparser_pair_metrics.csv", index=False)
    analyzed = pair_metrics.loc[pair_metrics["status"] == "ANALYZED"].copy()

    display(pair_metrics["status"].value_counts())
    display(pair_metrics.head())
    """),

    markdown("""
    ## 8. Paired descriptive statistics and Wilcoxon tests

    Each metric is tested independently — there is no single combined
    quality score. Pairs with zero delta are dropped before the Wilcoxon
    test (it is undefined for an all-zero sample), but are still reported
    separately as `unchanged_n`. Because 18 metrics are tested, Holm's
    step-down procedure corrects the p-values for multiple testing; the
    correction is written as its own function so its logic doesn't have to
    be re-read inline in the middle of a results cell.
    """),
    code("""
    def paired_summary(frame, metric):
        \"\"\"Descriptive stats and a paired Wilcoxon signed-rank test for one
        metric over one set of before/after pairs.\"\"\"
        before = frame[f"before_{metric}"].astype(float)
        after = frame[f"after_{metric}"].astype(float)
        delta = after - before
        nonzero_delta = delta[delta != 0]

        test = None
        if len(nonzero_delta):
            test = stats.wilcoxon(nonzero_delta, alternative="two-sided", method="auto")

        return {
            "metric": metric, "n": len(frame),
            "before_median": before.median(), "before_iqr": before.quantile(.75) - before.quantile(.25),
            "after_median": after.median(), "after_iqr": after.quantile(.75) - after.quantile(.25),
            "delta_median": delta.median(),
            "decreased_n": int((delta < 0).sum()),
            "unchanged_n": int((delta == 0).sum()),
            "increased_n": int((delta > 0).sum()),
            "wilcoxon_statistic": test.statistic if test else math.nan,
            "wilcoxon_p": test.pvalue if test else math.nan,
        }
    """),
    code("""
    def holm_bonferroni(pvalues):
        \"\"\"Holm's step-down multiple-testing correction.

        Sorts the (non-NaN) p-values ascending, and for the k-th smallest
        applies a factor of (n - k), taking a running maximum so adjusted
        p-values stay monotonically non-decreasing. NaN entries (no test
        could be run for that metric) are returned as NaN.
        \"\"\"
        adjusted = pd.Series(index=pvalues.index, dtype=float)
        valid_sorted = pvalues.dropna().sort_values()
        total = len(valid_sorted)
        running_max = 0.0
        for rank, (index, pvalue) in enumerate(valid_sorted.items()):
            running_max = max(running_max, min(1.0, (total - rank) * pvalue))
            adjusted.loc[index] = running_max
        return adjusted
    """),
    code("""
    summaries = pd.DataFrame([paired_summary(analyzed, metric) for metric in ["code_lines"] + METRICS])
    summaries["holm_p"] = holm_bonferroni(summaries["wilcoxon_p"])
    summaries.to_csv(RESULTS_ROOT / "javaparser_statistical_summary.csv", index=False)
    display(summaries)
    """),
    code("""
    # Repeat the same paired summary separately within each recommendation
    # group. Histories tagged with more than one type (MULTIPLE_TYPES) are
    # excluded here so a bug fix and a refactor never get averaged together.
    single_type_pairs = analyzed.loc[analyzed["recommendation_group"].isin(["Bug Fixing", "Improving Code"])]

    subgroup_summaries = pd.DataFrame([
        {"recommendation_group": group, **paired_summary(frame, metric)}
        for group, frame in single_type_pairs.groupby("recommendation_group")
        for metric in ["code_lines"] + METRICS
    ])
    subgroup_summaries.to_csv(RESULTS_ROOT / "javaparser_recommendation_subgroups.csv", index=False)

    print("Single-type subgroup histories:", len(single_type_pairs))
    print("MULTIPLE_TYPES histories excluded from the subgroup analysis:",
          (analyzed["recommendation_group"] == "MULTIPLE_TYPES").sum())
    """),

    markdown("""
    ## 9. Coverage, plots, and reproducibility metadata

    The parsing-coverage table and the excluded-`Agress-Yes`-rows file make
    every exclusion explicit and auditable, per the study's "do not
    silently discard unparseable pairs" requirement. The reproducibility
    summary hashes the extractor source and the JavaParser JAR so a later
    reader can confirm they are re-running the exact same analyzer.
    """),
    code("""
    coverage = (
        pair_metrics.groupby(["status", "parse_mode"], dropna=False).size()
        .rename("histories").reset_index()
    )
    coverage.to_csv(RESULTS_ROOT / "javaparser_parse_coverage.csv", index=False)
    accepted_exclusions.to_csv(RESULTS_ROOT / "javaparser_agress_yes_dataset_exclusions.csv", index=False)
    display(coverage)
    """),
    markdown("""
    Every metric gets its own panel, not a curated subset — with 18
    structural metrics plus `code_lines`, several are heavily zero-inflated
    (e.g. `switch_count` is 80/80 unchanged; see the statistical summary
    above), so most panels are expected to show a single tall spike at
    zero rather than a spread.
    """),
    code("""
    plot_metrics = ["code_lines"] + METRICS
    n_cols = 4
    n_rows = math.ceil(len(plot_metrics) / n_cols)

    figure, axes = plt.subplots(n_rows, n_cols, figsize=(4 * n_cols, 3 * n_rows))
    for axis, metric in zip(axes.flat, plot_metrics):
        deltas = analyzed[f"delta_{metric}"]
        axis.hist(deltas, bins=max(3, min(15, deltas.nunique() + 2)), color="#315d7d", edgecolor="white")
        axis.axvline(0, color="#14211f", linewidth=1)
        axis.set_title(metric.replace("_", " "))
        axis.set_xlabel("After − before")
        axis.set_ylabel("Histories")
    for unused_axis in axes.flat[len(plot_metrics):]:
        unused_axis.set_visible(False)
    figure.tight_layout()
    figure.savefig(RESULTS_ROOT / "javaparser_metric_deltas.png", dpi=180, bbox_inches="tight")
    plt.show()
    """),
    code("""
    run_summary = {
        "javaparser_version": JAVAPARSER_VERSION,
        "manual_validation_filter": "Agress Yes",
        "scoped_pairs": int(len(pairs)),
        "analyzed_pairs": int(len(analyzed)),
        "parse_excluded_pairs": int((pair_metrics["status"] == "PARSE_EXCLUDED").sum()),
        "identical_pairs": int((pairs["Identical Before After"] == "YES").sum()),
        "extractor_sha256": hashlib.sha256(EXTRACTOR_SOURCE.encode()).hexdigest(),
        "jar_sha256": hashlib.sha256(JAVAPARSER_JAR.read_bytes()).hexdigest(),
    }
    (RESULTS_ROOT / "javaparser_run_summary.json").write_text(json.dumps(run_summary, indent=2), encoding="utf-8")
    print(json.dumps(run_summary, indent=2))
    print("Results written to:", RESULTS_ROOT)
    """),

    markdown("""
    ## Interpretation checklist

    - Do not collapse heterogeneous metrics into one opaque quality score.
    - Report parsing coverage and the wrapper mode used for each pair.
    - Apply Holm correction across metric-level significance tests.
    - Interpret increases in null checks, catches, throws, and resource handling
      as potentially beneficial when they implement defensive behavior.
    - Manually inspect influential outliers and a random sample of parsed pairs.
    - Treat the cyclomatic value as a documented proxy, not a tool-standard
      cyclomatic-complexity implementation.
    """),
]

notebook = {
    "cells": cells,
    "metadata": {"kernelspec": {"display_name": "Python 3", "language": "python",
                                  "name": "python3"},
                 "language_info": {"name": "python", "version": "3"}},
    "nbformat": 4, "nbformat_minor": 5,
}
OUTPUT.write_text(json.dumps(notebook, indent=1) + "\n", encoding="utf-8")
print(f"Wrote {OUTPUT}")
