#!/usr/bin/env python3
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
MODEL_JAVA = ROOT / "src/main/java/com/example/mythic_age/client/render/entity/model/WalkingTreeModel.java"
MODEL_BB = ROOT / "docs/美术资源/blockbench/walking_tree.bbmodel"
AUDIT_REPORT = ROOT / "docs/项目管理/WalkingTree_独立审计记录.md"


def main():
    failures = []
    for path in (MODEL_JAVA, MODEL_BB, AUDIT_REPORT):
        if not path.exists():
            failures.append(f"Missing required gate file: {path}")

    if failures:
        for item in failures:
            print(item)
        sys.exit(1)

    report_text = AUDIT_REPORT.read_text(encoding="utf-8")
    if "结论：PASS" not in report_text:
        failures.append("Independent audit report does not contain PASS conclusion")

    report_mtime = AUDIT_REPORT.stat().st_mtime
    for path in (MODEL_JAVA, MODEL_BB):
        if report_mtime < path.stat().st_mtime:
            failures.append(f"Independent audit report is older than {path.name}")

    if failures:
        print("WalkingTree Gate Check: FAIL")
        for item in failures:
            print(f"- {item}")
        sys.exit(1)

    print("WalkingTree Gate Check: PASS")
    print(f"audit_report: {AUDIT_REPORT}")
    print(f"model_java: {MODEL_JAVA}")
    print(f"model_bbmodel: {MODEL_BB}")


if __name__ == "__main__":
    main()
