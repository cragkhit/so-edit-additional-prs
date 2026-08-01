#!/usr/bin/env python3
"""Generate a self-contained HTML page for manually reviewing the PMD
before/after pairs whose combined violation count changed (outcome ==
WORSENED or IMPROVED in results/pmd_reduced/pmd_pair_metrics.csv).

For each such pair, shows the full PMD pair-metrics row and the
before/after source side by side, with lines that carry a PMD violation
highlighted and annotated with the rule name and message.

Run from before-after/:
    python3 generate_pmd_review.py
"""

import csv
import html
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PAIR_METRICS_PATH = ROOT / "results" / "pmd_reduced" / "pmd_pair_metrics.csv"
VIOLATIONS_PATH = ROOT / "results" / "pmd_reduced" / "pmd_violations_long.csv"
MANIFEST_PATH = ROOT / "dataset" / "agress_yes_pairs.csv"
OUTPUT_PATH = ROOT / "results" / "pmd_reduced" / "pmd_review.html"

METRIC_ROWS = [
    ("Non-comment code lines", "code_lines", False),
    ("Raw PMD violations (combined)", "violations", True),
    ("Violations per 100 code lines", "violations_per_100_lines", True),
    ("Bug-risk violations", "bug_risk_violations", True),
    ("Bug-risk violations per 100 code lines", "bug_risk_violations_per_100_lines", True),
    ("Performance violations", "performance_violations", True),
    ("Performance violations per 100 code lines", "performance_violations_per_100_lines", True),
]


def read_csv(path):
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def fnum(value):
    try:
        number = float(value)
    except (TypeError, ValueError):
        return value
    return f"{number:.2f}".rstrip("0").rstrip(".") if "." in f"{number:.2f}" else f"{number:.0f}"


def code_table(source_text, violations):
    """Render source as an HTML table with one row per line.

    Each violation's *anchor* line (its begin_line, where PMD reports the
    finding) gets a strong highlight and a numbered marker. The rest of a
    multi-line violation's span (begin_line+1 .. end_line) only gets a
    faint left-border tick, so a 20-30 line UseTryWithResources span
    doesn't paint the entire file and drown out the single-line findings.
    """
    anchor_hits = defaultdict(list)
    span_hits = defaultdict(list)
    for violation in violations:
        begin = int(violation["begin_line"])
        end = int(violation["end_line"])
        anchor_hits[begin].append(violation)
        for line_number in range(begin + 1, end + 1):
            span_hits[line_number].append(violation)

    rows = []
    for index, line in enumerate(source_text.splitlines() or [""], start=1):
        anchors = anchor_hits.get(index, [])
        spans = span_hits.get(index, [])
        css_class = ""
        title = ""
        marker = ""
        if anchors:
            dims = {h["analysis_dimension"] for h in anchors}
            css_class = "hit-bug_risk" if "bug_risk" in dims else "hit-performance"
            if "bug_risk" in dims and "performance" in dims:
                css_class = "hit-both"
            title = " | ".join(
                f"{h['rule']} ({h['analysis_dimension']}): {h['message']}" for h in anchors
            )
            marker = f'<span class="marker">{len(anchors)}</span>'
        elif spans:
            dims = {h["analysis_dimension"] for h in spans}
            css_class = "span-bug_risk" if "bug_risk" in dims else "span-performance"
            title = " | ".join(
                f"(span of) {h['rule']} ({h['analysis_dimension']}) starting line {h['begin_line']}"
                for h in spans
            )
        escaped = html.escape(line) or "&nbsp;"
        title_attr = f' title="{html.escape(title)}"' if title else ""
        rows.append(
            f'<tr class="{css_class}"{title_attr}>'
            f'<td class="ln">{index}</td><td class="code">{escaped}</td>'
            f'<td class="marker-cell">{marker}</td></tr>'
        )
    return "<table class=\"code-table\">\n" + "\n".join(rows) + "\n</table>"


def violation_list(violations, version_label):
    if not violations:
        return f'<p class="empty">No retained violations in the {version_label} version.</p>'
    items = []
    for v in sorted(violations, key=lambda r: int(r["begin_line"])):
        dim_class = "dim-bug_risk" if v["analysis_dimension"] == "bug_risk" else "dim-performance"
        items.append(
            f'<li class="{dim_class}"><span class="rule">{html.escape(v["rule"])}</span> '
            f'<span class="dim">[{v["analysis_dimension"]}]</span> '
            f'line {v["begin_line"]}'
            + (f"–{v['end_line']}" if v["end_line"] != v["begin_line"] else "")
            + f': {html.escape(v["message"])}</li>'
        )
    return "<ul class=\"violation-list\">\n" + "\n".join(items) + "\n</ul>"


def metrics_table(pair):
    rows = []
    for label, key, higher_is_worse in METRIC_ROWS:
        before = pair[f"before_{key}"]
        after = pair[f"after_{key}"]
        before_display = fnum(before)
        after_display = fnum(after)
        try:
            delta = float(after) - float(before)
        except ValueError:
            delta = None
        delta_class = ""
        delta_display = "--"
        if delta is not None:
            delta_display = f"{delta:+.2f}".rstrip("0").rstrip(".")
            if delta_display in ("+", "-"):
                delta_display = "0"
            if higher_is_worse and delta > 0:
                delta_class = "worse"
            elif higher_is_worse and delta < 0:
                delta_class = "better"
        rows.append(
            f"<tr><td>{label}</td><td>{before_display}</td><td>{after_display}</td>"
            f'<td class="{delta_class}">{delta_display}</td></tr>'
        )
    return (
        "<table class=\"metrics-table\">\n"
        "<tr><th>Metric</th><th>Before</th><th>After</th><th>Δ</th></tr>\n"
        + "\n".join(rows)
        + "\n</table>"
    )


def fdelta(value):
    number = float(value)
    if number == 0:
        return "0"
    if number == int(number):
        return f"{int(number):+d}"
    return f"{number:+.2f}".rstrip("0").rstrip(".")


def delta_class(value):
    number = float(value)
    if number > 0:
        return "worse"
    if number < 0:
        return "better"
    return ""


SUMMARY_HEADERS = [
    ("Snippet ID", "text"), ("Group", "text"), ("Mode", "text"),
    ("Lines before", "num"), ("Lines after", "num"),
    ("Viol. before", "num"), ("Viol. after", "num"), ("Δ Viol.", "num"),
    ("Bug-risk before", "num"), ("Bug-risk after", "num"), ("Δ Bug-risk", "num"),
    ("Perf. before", "num"), ("Perf. after", "num"), ("Δ Perf.", "num"),
    ("Outcome", "text"),
]


def summary_table(analyzed_pairs, changed_ids):
    header_html = "".join(
        f'<th data-type="{data_type}" onclick="sortTable(this)">{label}</th>'
        for label, data_type in SUMMARY_HEADERS
    )
    rows = []
    for pair in sorted(analyzed_pairs, key=lambda r: -float(r["delta_violations"])):
        snippet_id = pair["snippet_id"]
        outcome = pair["outcome"]
        outcome_badge_class = {"WORSENED": "badge-worse", "IMPROVED": "badge-better"}.get(outcome, "")
        cell = f'<a href="#{snippet_id}">{snippet_id}</a>' if snippet_id in changed_ids else snippet_id
        d_violations = pair["delta_violations"]
        d_bug_risk = pair["delta_bug_risk_violations"]
        d_performance = pair["delta_performance_violations"]
        rows.append(
            "<tr>"
            f'<td>{cell}</td>'
            f'<td>{html.escape(pair["recommendation_group"])}</td>'
            f'<td>{pair["parse_mode"]}</td>'
            f'<td>{pair["before_code_lines"]}</td>'
            f'<td>{pair["after_code_lines"]}</td>'
            f'<td>{pair["before_violations"]}</td>'
            f'<td>{pair["after_violations"]}</td>'
            f'<td class="{delta_class(d_violations)}">{fdelta(d_violations)}</td>'
            f'<td>{pair["before_bug_risk_violations"]}</td>'
            f'<td>{pair["after_bug_risk_violations"]}</td>'
            f'<td class="{delta_class(d_bug_risk)}">{fdelta(d_bug_risk)}</td>'
            f'<td>{pair["before_performance_violations"]}</td>'
            f'<td>{pair["after_performance_violations"]}</td>'
            f'<td class="{delta_class(d_performance)}">{fdelta(d_performance)}</td>'
            f'<td><span class="badge {outcome_badge_class}">{outcome}</span></td>'
            "</tr>"
        )
    return (
        '<table class="summary-table" id="summary-table"><thead><tr>'
        + header_html
        + "</tr></thead><tbody>"
        + "\n".join(rows)
        + "</tbody></table>"
    )


def build_section(pair, manifest, violations_by_snippet_version, direction):
    snippet_id = pair["snippet_id"]
    manifest_row = manifest[snippet_id]
    before_path = ROOT / manifest_row["Before Dataset Path"]
    after_path = ROOT / manifest_row["After Dataset Path"]
    before_text = before_path.read_text(encoding="utf-8", errors="replace")
    after_text = after_path.read_text(encoding="utf-8", errors="replace")
    before_violations = violations_by_snippet_version.get((snippet_id, "before"), [])
    after_violations = violations_by_snippet_version.get((snippet_id, "after"), [])

    delta = int(float(pair["delta_violations"]))
    delta_badge_class = "badge-worse" if direction == "WORSENED" else "badge-better"
    delta_label = f"{delta:+d} violation(s)"

    nav_item = (
        f'<a href="#{snippet_id}" class="nav-{direction.lower()}">{snippet_id}</a> '
        f'<span class="nav-delta nav-delta-{direction.lower()}">({delta:+d})</span>'
    )

    section = f"""
    <section id="{snippet_id}">
      <h2>{snippet_id}
        <span class="badge badge-group">{html.escape(pair["recommendation_group"])}</span>
        <span class="badge badge-mode">{pair["parse_mode"]}</span>
        <span class="badge {delta_badge_class}">{delta_label}</span>
      </h2>

      {metrics_table(pair)}

      <div class="violations-columns">
        <div>
          <h3>Violations — before ({len(before_violations)})</h3>
          {violation_list(before_violations, "before")}
        </div>
        <div>
          <h3>Violations — after ({len(after_violations)})</h3>
          {violation_list(after_violations, "after")}
        </div>
      </div>

      <div class="code-columns">
        <div>
          <h3>Before &mdash; <code>{html.escape(manifest_row["Before Dataset Path"])}</code></h3>
          {code_table(before_text, before_violations)}
        </div>
        <div>
          <h3>After &mdash; <code>{html.escape(manifest_row["After Dataset Path"])}</code></h3>
          {code_table(after_text, after_violations)}
        </div>
      </div>
    </section>
    """
    return nav_item, section


def main():
    pair_metrics = read_csv(PAIR_METRICS_PATH)
    all_violations = read_csv(VIOLATIONS_PATH)
    manifest = {row["Snippet ID"]: row for row in read_csv(MANIFEST_PATH)}

    analyzed_pairs = [row for row in pair_metrics if row["status"] == "ANALYZED"]

    worsened = [row for row in analyzed_pairs if row["outcome"] == "WORSENED"]
    worsened.sort(key=lambda r: -float(r["delta_violations"]))
    improved = [row for row in analyzed_pairs if row["outcome"] == "IMPROVED"]
    improved.sort(key=lambda r: float(r["delta_violations"]))
    changed_ids = {row["snippet_id"] for row in worsened} | {row["snippet_id"] for row in improved}

    violations_by_snippet_version = defaultdict(list)
    for v in all_violations:
        violations_by_snippet_version[(v["snippet_id"], v["version"])].append(v)

    worsened_nav, worsened_sections = [], []
    for pair in worsened:
        nav_item, section = build_section(pair, manifest, violations_by_snippet_version, "WORSENED")
        worsened_nav.append(nav_item)
        worsened_sections.append(section)

    improved_nav, improved_sections = [], []
    for pair in improved:
        nav_item, section = build_section(pair, manifest, violations_by_snippet_version, "IMPROVED")
        improved_nav.append(nav_item)
        improved_sections.append(section)

    per_pair_table = summary_table(analyzed_pairs, changed_ids)
    total_analyzed = len(analyzed_pairs)
    html_doc = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>PMD Changed Pairs — Manual Review</title>
<style>
  :root {{
    --bg: #ffffff; --fg: #1a1a1a; --muted: #6b7280; --border: #e5e7eb;
    --code-bg: #f8f9fa; --hit-bug: #ffe0e0; --hit-perf: #dbeafe; --hit-both: #fde68a;
    --better: #0a7d3d; --better-bg: #e3f6ea; --worse: #b3261e; --worse-bg: #fde3e1;
    --badge-bg: #eef1f4;
  }}
  @media (prefers-color-scheme: dark) {{
    :root {{
      --bg: #14171a; --fg: #e6e6e6; --muted: #9aa4af; --border: #2b3138;
      --code-bg: #1b1f23; --hit-bug: #4a1f1f; --hit-perf: #1f2d4a; --hit-both: #4a3f1f;
      --better: #4fd18b; --better-bg: #163827; --worse: #ff8a80; --worse-bg: #3d1f1d;
      --badge-bg: #22262b;
    }}
  }}
  * {{ box-sizing: border-box; }}
  body {{
    background: var(--bg); color: var(--fg); margin: 0; padding: 0 0 4rem 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  }}
  header {{ padding: 1.5rem 2rem; border-bottom: 1px solid var(--border); }}
  header h1 {{ margin: 0 0 0.4rem 0; font-size: 1.4rem; }}
  header p {{ margin: 0.2rem 0; color: var(--muted); font-size: 0.9rem; }}
  nav {{ padding: 1rem 2rem; border-bottom: 1px solid var(--border); font-size: 0.9rem; }}
  nav h4 {{ margin: 0.6rem 0 0.3rem 0; font-size: 0.75rem; text-transform: uppercase;
    letter-spacing: 0.03em; color: var(--muted); }}
  nav .nav-group {{ margin-bottom: 0.4rem; }}
  nav a {{ margin-right: 1rem; color: inherit; }}
  .nav-delta-worsened {{ color: var(--worse); font-size: 0.85em; }}
  .nav-delta-improved {{ color: var(--better); font-size: 0.85em; }}
  main {{ padding: 0 2rem; max-width: 1400px; margin: 0 auto; }}
  section {{ border-bottom: 2px solid var(--border); padding: 2rem 0; }}
  h2 {{ font-size: 1.2rem; display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }}
  h3 {{ font-size: 0.95rem; color: var(--muted); margin: 1.2rem 0 0.5rem 0; }}
  .section-heading {{ padding: 1.5rem 2rem 0 2rem; max-width: 1400px; margin: 0 auto; }}
  .section-heading h2 {{ font-size: 1.05rem; }}
  .badge {{
    font-size: 0.7rem; font-weight: 600; padding: 0.15rem 0.5rem; border-radius: 999px;
    background: var(--badge-bg); color: var(--muted); text-transform: uppercase;
    letter-spacing: 0.02em;
  }}
  .badge-worse {{ background: var(--worse-bg); color: var(--worse); }}
  .badge-better {{ background: var(--better-bg); color: var(--better); }}
  table.metrics-table {{
    border-collapse: collapse; font-size: 0.85rem; margin-top: 0.5rem;
  }}
  table.metrics-table th, table.metrics-table td {{
    border: 1px solid var(--border); padding: 0.3rem 0.7rem; text-align: right;
  }}
  table.metrics-table td:first-child, table.metrics-table th:first-child {{ text-align: left; }}
  table.metrics-table .worse {{ color: var(--worse); font-weight: 600; }}
  table.metrics-table .better {{ color: var(--better); font-weight: 600; }}
  p.table-hint {{ color: var(--muted); font-size: 0.85rem; margin: 0.2rem 0 0.8rem 0; }}
  table.summary-table {{
    border-collapse: collapse; width: 100%; font-size: 0.8rem; margin-bottom: 1rem;
  }}
  table.summary-table th, table.summary-table td {{
    border: 1px solid var(--border); padding: 0.3rem 0.6rem; text-align: right;
    white-space: nowrap;
  }}
  table.summary-table th:nth-child(-n+3), table.summary-table td:nth-child(-n+3) {{ text-align: left; }}
  table.summary-table th:last-child, table.summary-table td:last-child {{ text-align: left; }}
  table.summary-table thead th {{
    background: var(--badge-bg); cursor: pointer; user-select: none; position: sticky; top: 0;
  }}
  table.summary-table thead th:hover {{ color: var(--better); }}
  table.summary-table tbody tr:nth-child(even) {{ background: var(--code-bg); }}
  table.summary-table td.worse {{ color: var(--worse); font-weight: 600; }}
  table.summary-table td.better {{ color: var(--better); font-weight: 600; }}
  .violations-columns, .code-columns {{
    display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin-top: 0.5rem;
  }}
  @media (max-width: 900px) {{
    .violations-columns, .code-columns {{ grid-template-columns: 1fr; }}
  }}
  ul.violation-list {{ list-style: none; margin: 0; padding: 0; font-size: 0.82rem; }}
  ul.violation-list li {{
    padding: 0.35rem 0.5rem; border-left: 3px solid var(--border); margin-bottom: 0.3rem;
    background: var(--code-bg);
  }}
  ul.violation-list li.dim-bug_risk {{ border-left-color: var(--worse); }}
  ul.violation-list li.dim-performance {{ border-left-color: #2563eb; }}
  ul.violation-list .rule {{ font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-weight: 600; }}
  ul.violation-list .dim {{ color: var(--muted); font-size: 0.75em; }}
  p.empty {{ color: var(--muted); font-size: 0.85rem; font-style: italic; }}
  table.code-table {{
    border-collapse: collapse; width: 100%; font-size: 0.78rem;
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    background: var(--code-bg); border: 1px solid var(--border);
  }}
  table.code-table td {{ padding: 0 0.5rem; white-space: pre; vertical-align: top; }}
  table.code-table td.ln {{
    color: var(--muted); text-align: right; user-select: none; width: 2.5rem;
    border-right: 1px solid var(--border);
  }}
  table.code-table td.marker-cell {{ width: 1.5rem; text-align: center; }}
  table.code-table tr.hit-bug_risk {{ background: var(--hit-bug); }}
  table.code-table tr.hit-performance {{ background: var(--hit-perf); }}
  table.code-table tr.hit-both {{ background: var(--hit-both); }}
  table.code-table tr.span-bug_risk td.code {{ border-left: 3px solid var(--worse); }}
  table.code-table tr.span-performance td.code {{ border-left: 3px solid #2563eb; }}
  table.code-table .marker {{
    display: inline-block; background: var(--worse); color: white; border-radius: 999px;
    font-size: 0.65rem; padding: 0 0.35rem; cursor: help;
  }}
  code {{ font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 0.85em; }}
</style>
</head>
<body>
<header>
  <h1>PMD Changed Pairs — Manual Review</h1>
  <p>Of {total_analyzed} analyzed pairs, <strong>{len(worsened)}</strong> had a net
  <em>increase</em> and <strong>{len(improved)}</strong> had a net <em>decrease</em>
  in combined PMD (reduced ruleset) violations from the original snippet to its
  latest Stack Overflow revision (the remaining {total_analyzed - len(worsened) - len(improved)}
  pairs were unchanged and are not shown here). Rows highlighted in code panels
  carry a PMD finding &mdash; hover for the rule and message.</p>
  <p>Generated by <code>generate_pmd_review.py</code> from
  <code>results/pmd_reduced/pmd_pair_metrics.csv</code> and
  <code>results/pmd_reduced/pmd_violations_long.csv</code>.</p>
</header>
<div class="section-heading">
  <h2>PMD results per pair ({total_analyzed} analyzed)</h2>
  <p class="table-hint">Click a column header to sort. Snippet IDs are linked
  where a before/after code detail section exists below (the 11 changed pairs).</p>
</div>
<main>
{per_pair_table}
</main>
<nav>
  <div class="nav-group">
    <h4>Violations increased ({len(worsened)})</h4>
    {" &middot; ".join(worsened_nav) if worsened_nav else '<span class="empty">none</span>'}
  </div>
  <div class="nav-group">
    <h4>Violations decreased ({len(improved)})</h4>
    {" &middot; ".join(improved_nav) if improved_nav else '<span class="empty">none</span>'}
  </div>
</nav>
<div class="section-heading"><h2>Violations increased ({len(worsened)})</h2></div>
<main>
{"".join(worsened_sections)}
</main>
<div class="section-heading"><h2>Violations decreased ({len(improved)})</h2></div>
<main>
{"".join(improved_sections)}
</main>
<script>
function sortTable(th) {{
  const table = th.closest("table");
  const tbody = table.querySelector("tbody");
  const headers = Array.from(th.parentNode.children);
  const index = headers.indexOf(th);
  const type = th.dataset.type;
  const ascending = th.dataset.asc !== "true";
  headers.forEach(h => delete h.dataset.asc);
  th.dataset.asc = ascending;
  const rows = Array.from(tbody.querySelectorAll("tr"));
  rows.sort((rowA, rowB) => {{
    let a = rowA.children[index].textContent.trim();
    let b = rowB.children[index].textContent.trim();
    if (type === "num") {{
      a = parseFloat(a.replace(/[^0-9.+-]/g, "")) || 0;
      b = parseFloat(b.replace(/[^0-9.+-]/g, "")) || 0;
      return ascending ? a - b : b - a;
    }}
    return ascending ? a.localeCompare(b) : b.localeCompare(a);
  }});
  rows.forEach(row => tbody.appendChild(row));
}}
</script>
</body>
</html>
"""
    OUTPUT_PATH.write_text(html_doc, encoding="utf-8")
    print(f"Wrote {OUTPUT_PATH} ({len(worsened)} worsened, {len(improved)} improved)")


if __name__ == "__main__":
    main()
