#!/usr/bin/env python3
"""Close invalid Aug 1-2 Matcha PRs and update their final-CSV records."""

import csv
import json
import os
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parent
CSV_PATH = ROOT / "matcha_results_2024-05-07_manual_validation_FINAL.csv"
LOG_PATH = ROOT / "closed_invalid_aug_prs.json"

INVALID = {
    "https://github.com/actframework/actframework/pull/1434": "The recorded Stack Overflow edit only removes the surrounding UTF8Control wrapper; this PR instead changes HTTP response resource handling.",
    "https://github.com/AddstarMC/Minigames/pull/404": "The recorded Stack Overflow edit only removes the surrounding UTF8Control wrapper; this PR instead rewrites bundle stream handling.",
    "https://github.com/AdoptOpenJDK/IcedTea-Web/pull/1001": "The recorded Stack Overflow edit concerns piped-stream example code; this PR instead changes list sorting and mutation behavior.",
    "https://github.com/aicis/fresco/pull/441": "The canonical Stack Overflow before and after snapshots are identical, so there is no snippet edit supporting this cleanup change.",
    "https://github.com/airlift/airlift/pull/2086": "The canonical Stack Overflow before and after snapshots are identical, so there is no snippet edit supporting this documentation change.",
    "https://github.com/aliyun/aliyun-odps-java-sdk/pull/80": "The recorded Stack Overflow edit adds a DecimalFormat field; this PR instead introduces ThreadLocal formatting and changes call sites.",
    "https://github.com/apache/netbeans/pull/9536": "The recorded Stack Overflow edit refactors a Swing mouse-wheel example; this PR instead changes descriptor property-name matching.",
    "https://github.com/apache/rocketmq/pull/10746": "The recorded Stack Overflow edit installs a DiscardServerHandler; this PR instead changes channel-closing behavior.",
    "https://github.com/apache/pulsar/pull/26261": "The recorded Stack Overflow edit installs a DiscardServerHandler; this PR instead adds reference-count release behavior.",
    "https://github.com/arduino/Arduino/pull/12126": "The recorded Stack Overflow edit adds an alternative classpath approach; this PR instead changes delimiter wording.",
    "https://github.com/aws/amazon-redshift-jdbc-driver/pull/160": "The recorded Stack Overflow edit rewrites JDBC resource cleanup; this PR instead changes utility-class construction.",
    "https://github.com/bookdash/bookdash-android-app/pull/69": "The recorded Stack Overflow edit concerns a piped-stream example; this PR instead rewrites ZIP extraction security and resource handling.",
    "https://github.com/camunda-community-hub/camunda-8-lowcode-ui-template/pull/118": "The recorded Stack Overflow edit only reformats the security-scheme declaration; this PR instead adds a global OpenAPI security requirement.",
    "https://github.com/controlsfx/controlsfx/pull/1622": "The recorded Stack Overflow edit rewrites a side-drawer animation example; this PR instead changes table-filter memory-sample ownership.",
    "https://github.com/CruxFramework/crux/pull/1013": "The recorded Stack Overflow edit only adds diagnostic document output; this PR instead introduces synchronization and parser-state changes.",
    "https://github.com/dockstore/dockstore/pull/6330": "The recorded Stack Overflow edit adds Azure Blob RSS publishing guidance; this PR instead changes null handling in RSS generation.",
    "https://github.com/dromara/hodor/pull/68": "The recorded Stack Overflow edit adds a hexadecimal lookup array; this PR instead adds input validation and new tests.",
    "https://github.com/eclipse-platform/eclipse.platform.swt/pull/3480": "The recorded Stack Overflow edit changes Shell IDList transfer names and registration; this PR instead changes Display disposal.",
    "https://github.com/elastic/elasticsearch/pull/155679": "The recorded Stack Overflow edit changes producer queue increment and termination logic; this PR instead adds latch timeouts.",
    "https://github.com/gitlab4j/gitlab4j-api/pull/1330": "The recorded Stack Overflow edit corrects a serializer name and exception declaration; this PR instead removes a date formatter.",
    "https://github.com/jcodec/jcodec/pull/520": "The recorded Stack Overflow edit adds file streams around a copy operation; this PR instead changes IOUtils construction.",
    "https://github.com/netty/netty/pull/17186": "The recorded Stack Overflow edit installs a DiscardServerHandler; this PR instead changes proxy-channel closure behavior.",
    "https://github.com/polypheny/Polypheny-DB/pull/573": "The recorded Stack Overflow edit restricts Gson object handling by JSON path; this PR instead changes a varchar metadata comparison.",
    "https://github.com/robolectric/robolectric/pull/11391": "The recorded Stack Overflow edit changes a concurrency example and shutdown sequence; this PR instead changes handler-thread teardown waits.",
    "https://github.com/RPTools/maptool/pull/6021": "The recorded Stack Overflow edit changes PDF XObject text-field detection; this PR instead changes marker-stream resource handling.",
    "https://github.com/sofastack/sofa-jraft/pull/1273": "The recorded Stack Overflow edit adds a hexadecimal lookup array; this PR instead adds input validation and modifies tests.",
    "https://github.com/spring-projects/spring-tools/pull/1957": "The recorded Stack Overflow edit renames a controller method; this PR instead replaces date-formatting APIs.",
    "https://github.com/thingsboard/thingsboard/pull/15999": "The source pair is excluded because its original Stack Overflow snapshot is missing, so the proposed logging change cannot be validated against an edit.",
}


def close_pr(url: str) -> tuple[bool, str]:
    result = subprocess.run(("gh", "pr", "close", url), text=True, capture_output=True)
    message = (result.stdout + result.stderr).strip()
    return result.returncode == 0, message


def update_csv(closed: set[str]) -> tuple[int, int]:
    with CSV_PATH.open(encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        fieldnames = reader.fieldnames
        rows = list(reader)
    if not fieldnames:
        raise RuntimeError("CSV has no header")

    changed = 0
    for row in rows:
        url = row.get("Open PR Link", "").strip()
        if url in closed:
            row["Recommendation PR Status"] = "CLOSED DUE TO ISSUE"
            row["Recommendation PR Reason"] = "Minimal-change audit: " + INVALID[url]
            row["PR Status"] = "Closed"
            changed += 1

    fd, temp_name = tempfile.mkstemp(prefix="matcha-final-", suffix=".csv", dir=CSV_PATH.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8-sig", newline="") as target:
            writer = csv.DictWriter(target, fieldnames=fieldnames, lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)
        with open(temp_name, encoding="utf-8-sig", newline="") as check:
            checked = list(csv.DictReader(check))
        if len(checked) != len(rows):
            raise RuntimeError(f"Row-count mismatch: wrote {len(checked)}, expected {len(rows)}")
        os.replace(temp_name, CSV_PATH)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)
    return changed, len(rows)


def main() -> None:
    results = []
    for url, reason in INVALID.items():
        success, output = close_pr(url)
        results.append({"url": url, "success": success, "reason": reason, "output": output})
        print(("CLOSED" if success else "FAILED"), url, output)

    closed = {item["url"] for item in results if item["success"]}
    changed, total = update_csv(closed)
    LOG_PATH.write_text(json.dumps(results, indent=2), encoding="utf-8")
    print(f"Updated {changed} CSV rows out of {total}; {len(closed)} PRs closed")
    if len(closed) != len(INVALID):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
