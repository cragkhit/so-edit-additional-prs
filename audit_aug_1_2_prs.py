#!/usr/bin/env python3
"""Collect PR and Stack Overflow diff metadata for the 1-2 Aug 2026 batches."""

import csv
import json
import re
import subprocess
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo


ROOT = Path(__file__).resolve().parent
FINAL_CSV = ROOT / "matcha_results_2024-05-07_manual_validation_FINAL.csv"
JAVA_FILES = Path("/Users/chaiyong/Downloads/do_not_delete/Matcha_Study/java_files")
OUTPUT = ROOT / "audit_aug_1_2_prs.json"
PAIR_MAPPING = ROOT / "before-after/dataset/study_pair_mapping.csv"
LOCAL_TZ = ZoneInfo("Asia/Bangkok")


def run(*args: str) -> str:
    return subprocess.run(args, check=True, text=True, capture_output=True).stdout


def unified_diff(original: Path, recent: Path) -> str:
    result = subprocess.run(
        ("diff", "-u", str(original), str(recent)), text=True, capture_output=True
    )
    if result.returncode not in (0, 1):
        result.check_returncode()
    return result.stdout


def changed_lines(diff_text: str) -> tuple[int, int]:
    additions = deletions = 0
    for line in diff_text.splitlines():
        if line.startswith(("+++", "---")):
            continue
        additions += line.startswith("+")
        deletions += line.startswith("-")
    return additions, deletions


def so_pair(relative_recent: str) -> tuple[Path, Path]:
    recent = JAVA_FILES / relative_recent
    original = recent.with_name(recent.name.replace("_recent.java", "_original.java"))
    return original, recent


def main() -> None:
    with PAIR_MAPPING.open(encoding="utf-8-sig", newline="") as source:
        pair_paths = {
            row["No"]: (row["Before Dataset Path"], row["After Dataset Path"])
            for row in csv.DictReader(source)
        }
    records = []
    with FINAL_CSV.open(encoding="utf-8-sig", newline="") as source:
        for row in csv.DictReader(source):
            url = row.get("Open PR Link", "").strip()
            opened = row.get("PR Opened At (UTC)", "").strip()
            if not url or not opened:
                continue
            local_date = datetime.fromisoformat(opened.replace("Z", "+00:00")).astimezone(LOCAL_TZ).date()
            if str(local_date) not in {"2026-08-01", "2026-08-02"}:
                continue
            match = re.fullmatch(r"https://github.com/([^/]+/[^/]+)/pull/(\d+)", url)
            if not match:
                continue
            repo, number = match.groups()
            metadata = json.loads(run(
                "gh", "pr", "view", number, "--repo", repo,
                "--json", "url,title,state,isDraft,baseRefName,headRefName,headRepositoryOwner,commits,files,body",
            ))
            pr_diff = run("gh", "pr", "diff", number, "--repo", repo)
            before_path, after_path = pair_paths.get(row["No"], ("", ""))
            if before_path and after_path:
                original = ROOT / "before-after" / before_path
                recent = ROOT / "before-after" / after_path
            else:
                original, recent = so_pair(row["Stack Overflow Java File Path"])
            so_diff = unified_diff(original, recent) if original.exists() and recent.exists() else ""
            pr_add, pr_del = changed_lines(pr_diff)
            so_add, so_del = changed_lines(so_diff)
            records.append({
                "row": row["No"],
                "repo": repo,
                "number": int(number),
                "url": url,
                "local_date": str(local_date),
                "so_url": row["Link SO"],
                "so_recent": row["Stack Overflow Java File Path"],
                "github_file": row["GitHub Java File Path"],
                "pr": metadata,
                "pr_additions": pr_add,
                "pr_deletions": pr_del,
                "so_additions": so_add,
                "so_deletions": so_del,
                "pr_diff": pr_diff,
                "so_diff": so_diff,
            })
    OUTPUT.write_text(json.dumps(records, indent=2), encoding="utf-8")
    print(f"Wrote {len(records)} records to {OUTPUT}")
    for item in records:
        print(
            f"{item['row']:>4} {item['repo']}#{item['number']} "
            f"PR +{item['pr_additions']}/-{item['pr_deletions']} "
            f"SO +{item['so_additions']}/-{item['so_deletions']} {item['pr']['state']}"
        )


if __name__ == "__main__":
    main()
