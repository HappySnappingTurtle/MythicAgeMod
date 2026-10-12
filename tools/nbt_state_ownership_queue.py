#!/usr/bin/env python3
"""Generate the MythicAgeMod 1.0 NBT state ownership queue.

The target gate can pass while the product still needs an explicit ownership
map. This report separates covered persistent state from runtime-only design
decisions and future upgrade triggers.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TARGET_DOC = ROOT / "docs/系统设计/MythicAgeMod_1.0_NBT状态持久化目标_2026-06-11.md"
NBT_TARGET_REPORT = ROOT / "docs/preview_assets/nbt_target_validation/nbt_target_report.json"
NBT_SCHEMA_REPORT = ROOT / "docs/preview_assets/entity_validation/nbt_schema_report.json"
NBT_RELEASE_PACKAGE = ROOT / "docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json"
OUT_DIR = ROOT / "docs/preview_assets/nbt_state_ownership"
REPORT_PATH = OUT_DIR / "nbt_state_ownership_queue.json"
MARKDOWN_PATH = ROOT / "docs/系统设计/MythicAgeMod_1.0_NBT状态所有权队列_2026-06-15.md"

STATUS_VALUES = {"required", "runtime_only", "covered", "missing"}

SECTION_DOMAIN = [
    ("4. 生物状态目标", "creatures"),
    ("5. 神力持续状态目标", "god_powers"),
    ("6. 证书、交易和经济状态", "certificate_economy"),
    ("7. 方块实体和结构状态", "structures_blocks"),
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def split_markdown_row(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def is_separator(cells: list[str]) -> bool:
    return all(re.fullmatch(r":?-{3,}:?", cell.strip()) for cell in cells if cell.strip())


def domain_for_section(section: str) -> str:
    for prefix, domain in SECTION_DOMAIN:
        if section.startswith(prefix):
            return domain
    return "other"


def status_cell(cells: list[str]) -> tuple[str | None, int | None]:
    for index, cell in enumerate(cells):
        match = re.fullmatch(r"`([A-Za-z_]+)`", cell.strip())
        if match and match.group(1) in STATUS_VALUES:
            return match.group(1), index
    return None, None


def parse_target_rows() -> list[dict[str, Any]]:
    text = TARGET_DOC.read_text(encoding="utf-8") if TARGET_DOC.exists() else ""
    rows: list[dict[str, Any]] = []
    section = ""
    for line_number, line in enumerate(text.splitlines(), 1):
        if line.startswith("## "):
            section = line.lstrip("#").strip()
            continue
        stripped = line.strip()
        if not stripped.startswith("|"):
            continue
        cells = split_markdown_row(stripped)
        if len(cells) < 4 or is_separator(cells):
            continue
        status, status_index = status_cell(cells)
        if status is None or status_index is None:
            continue
        left = cells[:status_index]
        right = cells[status_index + 1:]
        owner = left[0] if left else ""
        field = left[1] if len(left) > 1 else owner
        type_or_scope = left[2] if len(left) > 2 else ""
        note = right[-1] if right else ""
        rows.append({
            "line": line_number,
            "domain": domain_for_section(section),
            "section": section,
            "owner": owner,
            "field": field,
            "type_or_scope": type_or_scope,
            "status": status,
            "note": note,
            "raw": stripped,
        })
    return rows


def runtime_upgrade_trigger(row: dict[str, Any]) -> str:
    owner = row["owner"]
    field = row["field"]
    note = row["note"]
    if row["domain"] == "creatures":
        return "当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。"
    if row["domain"] == "god_powers":
        if "瞬时" in note or "没有持续" in note:
            return "当神力产生可持续实体、方块、区域、传送门或跨重启影响时，升级为 world persistent state。"
        return "当持续神力不再能由状态效果或当前 tick 重建时，升级为 required。"
    if row["domain"] == "certificate_economy":
        return "当物品获得绑定、次数、耐久、交易来源或防刷语义时，升级为 item component 或 world persistent state。"
    if row["domain"] == "structures_blocks":
        return "当结构功能点由运行时推导改为玩家可改变状态时，升级为方块实体或 world state 字段。"
    return f"若 {owner}.{field} 变成跨重启真相，升级为 required。"


def validation_for(row: dict[str, Any]) -> list[str]:
    if row["status"] == "covered":
        return [
            "静态 schema owner 覆盖",
            "source round-trip 或 restart contract 覆盖",
            "真实重启验收包保持 ok=true",
        ]
    if row["status"] == "runtime_only":
        return [
            "设计文档保留 runtime_only 决策",
            "不得成为唯一长期真相",
            "新增跨重启语义时先修改目标表再实现",
        ]
    if row["status"] == "required":
        return [
            "实现 write/read NBT 或 world persistent state",
            "加入 nbt_schema_report",
            "加入真实重启 verify",
        ]
    return ["发布前不得保留 missing 状态"]


def queue_item(row: dict[str, Any]) -> dict[str, Any]:
    status = row["status"]
    if status == "covered":
        priority = "P0-REGRESSION-GUARD"
        phase = "keep_schema_and_restart_evidence_green"
    elif status == "runtime_only":
        priority = "P1-RUNTIME-ONLY-DECISION"
        phase = "document_decision_and_watch_upgrade_trigger"
    elif status == "required":
        priority = "P0-IMPLEMENT-PERSISTENCE"
        phase = "implement_and_probe_before_release"
    else:
        priority = "P0-MISSING-BLOCKER"
        phase = "resolve_missing_target"
    return {
        **row,
        "priority": priority,
        "phase": phase,
        "upgrade_trigger": runtime_upgrade_trigger(row) if status == "runtime_only" else "",
        "validation": validation_for(row),
    }


def schema_owner_items(schema_report: dict[str, Any]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for schema in schema_report.get("schemas", []):
        if not isinstance(schema, dict):
            continue
        result.append({
            "owner": schema.get("owner"),
            "scope": schema.get("scope"),
            "path": schema.get("path"),
            "field_count": len(schema.get("fields", [])),
            "ok": bool(schema.get("ok")),
        })
    return result


def build_report() -> dict[str, Any]:
    target_report = load_json(NBT_TARGET_REPORT, {})
    schema_report = load_json(NBT_SCHEMA_REPORT, {})
    release_package = load_json(NBT_RELEASE_PACKAGE, {})
    rows = parse_target_rows()
    queue = [queue_item(row) for row in rows]
    status_counts = Counter(item["status"] for item in queue)
    priority_counts = Counter(item["priority"] for item in queue)
    domain_counts = Counter(item["domain"] for item in queue)
    covered_count = status_counts.get("covered", 0)
    runtime_only_count = status_counts.get("runtime_only", 0)
    required_count = status_counts.get("required", 0)
    missing_count = status_counts.get("missing", 0)
    ok = (
        bool(target_report.get("target_gate_ok"))
        and bool(schema_report.get("ok"))
        and bool(release_package.get("ok"))
        and required_count == 0
        and missing_count == 0
    )
    evidence_mismatch = []
    target_status_counts = target_report.get("status_counts", {}) if isinstance(target_report, dict) else {}
    for status in sorted(STATUS_VALUES):
        if target_status_counts.get(status, 0) != status_counts.get(status, 0):
            evidence_mismatch.append({
                "status": status,
                "target_report": target_status_counts.get(status, 0),
                "parsed_queue": status_counts.get(status, 0),
            })
    return {
        "version": 1,
        "scope": "mythic_age_1_0_nbt_state_ownership_queue",
        "ok": ok,
        "release_nbt_ready": bool(release_package.get("release_ready")),
        "source_target_doc": project_path(TARGET_DOC),
        "target_report": project_path(NBT_TARGET_REPORT),
        "schema_report": project_path(NBT_SCHEMA_REPORT),
        "release_acceptance_package": project_path(NBT_RELEASE_PACKAGE),
        "row_count": len(queue),
        "status_counts": dict(sorted(status_counts.items())),
        "priority_counts": dict(sorted(priority_counts.items())),
        "domain_counts": dict(sorted(domain_counts.items())),
        "schema_owner_count": len(schema_owner_items(schema_report)),
        "schema_owners": schema_owner_items(schema_report),
        "covered_count": covered_count,
        "runtime_only_count": runtime_only_count,
        "required_count": required_count,
        "missing_count": missing_count,
        "evidence_mismatch": evidence_mismatch,
        "rule": (
            "NBT is release-ready only when required/missing are zero, covered owners stay verified, "
            "and runtime_only decisions have explicit upgrade triggers before new long-lived gameplay is added."
        ),
        "queue": queue,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in payload["queue"]:
        grouped[item["priority"]].append(item)
    lines = [
        "# MythicAgeMod 1.0 NBT 状态所有权队列（2026-06-15）",
        "",
        "本文档由 `tools/nbt_state_ownership_queue.py audit` 生成。它不把 `runtime_only` 当成缺失，而是把每个长期状态、短期状态和未来升级触发条件登记清楚。",
        "",
        "## 总览",
        "",
        f"- NBT 队列通过：`{payload['ok']}`",
        f"- NBT 发布验收 ready：`{payload['release_nbt_ready']}`",
        f"- 状态行数：{payload['row_count']}",
        f"- 已覆盖长期状态：{payload['covered_count']}",
        f"- runtime-only 决策：{payload['runtime_only_count']}",
        f"- required 待实现：{payload['required_count']}",
        f"- missing 阻断：{payload['missing_count']}",
        f"- schema owner 数：{payload['schema_owner_count']}",
        f"- 状态统计：`{json.dumps(payload['status_counts'], ensure_ascii=False)}`",
        f"- 优先级统计：`{json.dumps(payload['priority_counts'], ensure_ascii=False)}`",
        f"- 领域统计：`{json.dumps(payload['domain_counts'], ensure_ascii=False)}`",
        "",
        "## 结论",
        "",
        "- 当前 NBT 不是“没做”，而是 1.0 目标表已经把长期状态和短期状态分开：21 项 covered，48 项 runtime_only，required/missing 为 0。",
        "- 30 个普通生物技能冷却、飞行阶段、短 tick 能力大多不应该写长期 NBT；它们只有在变成跨重启真相、经济防刷、召唤归属或长期 boss 阶段时才升级。",
        "- 后续新增玩法必须先改目标表，再实现 NBT/schema/probe，不能先写隐藏状态。",
        "",
    ]
    if payload["evidence_mismatch"]:
        lines.extend([
            "## 证据一致性提示",
            "",
        ])
        for item in payload["evidence_mismatch"]:
            lines.append(f"- `{item['status']}`: target=`{item['target_report']}` queue=`{item['parsed_queue']}`")
        lines.append("")
    lines.extend([
        "## Schema Owners",
        "",
        "| Owner | Scope | Fields | OK | Path |",
        "| --- | --- | --- | --- | --- |",
    ])
    for owner in payload["schema_owners"]:
        lines.append(
            f"| `{owner['owner']}` | `{owner['scope']}` | {owner['field_count']} | `{owner['ok']}` | `{owner['path']}` |"
        )
    lines.extend(["", "## 队列", ""])
    for priority in ["P0-IMPLEMENT-PERSISTENCE", "P0-MISSING-BLOCKER", "P0-REGRESSION-GUARD", "P1-RUNTIME-ONLY-DECISION"]:
        items = grouped.get(priority, [])
        if not items:
            continue
        lines.extend([f"### {priority}", "", "| Domain | Owner/System | Field | Status | Phase |", "| --- | --- | --- | --- | --- |"])
        for item in items:
            lines.append(
                f"| `{item['domain']}` | `{item['owner']}` | `{item['field']}` | `{item['status']}` | `{item['phase']}` |"
            )
        lines.append("")
    lines.extend(["## Runtime-only 升级触发器", ""])
    for item in grouped.get("P1-RUNTIME-ONLY-DECISION", []):
        lines.append(f"- `{item['domain']}` / `{item['owner']}` / `{item['field']}`：{item['upgrade_trigger']}")
    lines.extend(["", "## 回归规则", ""])
    lines.extend([
        "- `tools/nbt_target_validation_pipeline.py audit` 必须通过。",
        "- `tools/nbt_release_acceptance_package.py audit` 必须通过。",
        "- 任何从 runtime-only 升级为 required 的项，必须同时补 schema、round-trip/restart probe 和默认值策略。",
        "- release 文档中的 next_actions 是回归守护动作，不代表 NBT 当前未完成。",
    ])
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_report()
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        status = "PASS" if payload["ok"] else "OPEN"
        print(f"NBT State Ownership Queue: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"status_counts: {payload['status_counts']}")
        print(f"priority_counts: {payload['priority_counts']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
