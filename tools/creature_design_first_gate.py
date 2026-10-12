#!/usr/bin/env python3
"""Audit design-first approval coverage for the full 1.0 creature roster.

The visual pipelines generate design documents, concept art, bbmodels, UVs, and
previews. Those artifacts are useful evidence, but they are not the same as a
signed-off creature design. This gate makes that distinction explicit for all
30 creatures before further model, texture, animation, or runtime rebuild work
is treated as 1.0-ready.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/creature_design_first_gate"
REPORT_PATH = OUT_DIR / "creature_design_first_gate.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/entity_validation/creature_design_first_gate_2026-06-15.md"

CIVILIZATIONS = ["greek", "egyptian", "nordic", "chinese", "atlantean"]
MANIFESTS = {
    civ: ROOT / f"docs/美术资源/{civ}_creatures/visual_manifest.json"
    for civ in CIVILIZATIONS
}

V6_MODELING_SPECS = {
    ("greek", "pegasus"): ROOT / "docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md",
    ("greek", "hydra"): ROOT / "docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md",
}

DESIGN_CORRECTNESS_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_correctness/pegasus_hydra_design_correctness_gate.json"
GREEK_DESIGN_FIRST_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_first_gate.json"

APPROVED = "approved_for_modeling"

REQUIRED_DESIGN_SECTIONS = [
    "目标体量",
    "轮廓语言",
    "概念设计图",
    "核心识别特征",
    "部件清单",
    "Blockbench 层级",
    "建模硬性要求",
    "UV 计划",
    "纹理细节",
    "动画约束",
    "返工触发条件",
    "验收口径",
]

APPROVAL_FIELDS = [
    "design_status",
    "design_sheet_status",
    "design_approval_status",
    "design_correctness_verdict",
    "design_detail_sheet_status",
    "design_detail_board_status",
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def read_front_matter(path: Path) -> dict[str, str]:
    text = read_text(path)
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return {}
    data: dict[str, str] = {}
    for line in lines[1:]:
        if line.strip() == "---":
            break
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        data[key.strip()] = value.strip().strip('"')
    return data


def artifact_exists(path_text: Any) -> bool:
    if not isinstance(path_text, str) or not path_text:
        return False
    return (ROOT / path_text).exists()


def preview_paths(item: dict[str, Any]) -> list[str]:
    previews = item.get("blockbench_previews")
    if not isinstance(previews, dict):
        return []
    return [
        value
        for value in previews.values()
        if isinstance(value, str)
    ]


def formal_approval_failures(front: dict[str, str]) -> list[str]:
    failures = [
        f"{field}={front.get(field)!r}"
        for field in APPROVAL_FIELDS
        if front.get(field) != APPROVED
    ]
    if front.get("modeling_allowed", "").lower() != "true":
        failures.append("modeling_allowed=false")
    return failures


def v6_modeling_spec_summary(civilization: str, creature_id: str) -> dict[str, Any]:
    spec = V6_MODELING_SPECS.get((civilization, creature_id))
    if spec is None:
        return {}
    front = read_front_matter(spec)
    failures = formal_approval_failures(front)
    return {
        "spec_contract": project_path(spec),
        "spec_contract_exists": spec.exists(),
        "front_matter": front,
        "approval_failures": failures,
        "approved_for_modeling": not failures,
    }


def classify_design(
    civilization: str,
    creature_id: str,
    design_doc: Path,
    front: dict[str, str],
    text: str,
) -> tuple[str, str, list[str]]:
    if (civilization, creature_id) in V6_MODELING_SPECS:
        summary = v6_modeling_spec_summary(civilization, creature_id)
        if summary.get("approved_for_modeling"):
            return (
                "approved_for_modeling",
                "V6 modeling spec contract and approval metadata are approved.",
                [],
            )
        return (
            "blocked_current_design_rejected_v6_spec_pending",
            "Current V2/reference design is rejected; V6 modeling spec exists but is not approved for modeling.",
            list(summary.get("approval_failures", [])),
        )

    failures = formal_approval_failures(front) if front else ["missing formal approval front matter"]
    if front and not failures:
        return (
            "approved_for_modeling",
            "Design document has formal approval metadata.",
            [],
        )
    if "rejected_v2_reference_only" in text:
        return (
            "blocked_current_design_rejected",
            "Design document marks this creature as rejected V2 reference only.",
            failures,
        )
    return (
        "manual_design_approval_required",
        "Design package exists, but it has not been formally approved for modeling.",
        failures,
    )


def audit_creature(civilization: str, item: dict[str, Any]) -> dict[str, Any]:
    creature_id = str(item.get("id"))
    design_doc_text = item.get("design_doc")
    design_doc = ROOT / design_doc_text if isinstance(design_doc_text, str) else ROOT / "__missing__"
    text = read_text(design_doc)
    front = (
        read_front_matter(V6_MODELING_SPECS[(civilization, creature_id)])
        if (civilization, creature_id) in V6_MODELING_SPECS
        else read_front_matter(design_doc)
    )
    missing_sections = [
        section for section in REQUIRED_DESIGN_SECTIONS
        if f"## {section}" not in text
    ]
    artifact_checks = {
        "design_doc": design_doc.exists(),
        "concept_art": artifact_exists(item.get("concept_art")),
        "bbmodel": artifact_exists(item.get("bbmodel")),
        "texture": artifact_exists(item.get("texture")),
        "uv_template": artifact_exists(item.get("uv_template")),
        "texture_fit_preview": artifact_exists(item.get("texture_fit_preview")),
        "blockbench_previews": all(artifact_exists(path) for path in preview_paths(item)),
    }
    status, reason, approval_failures = classify_design(civilization, creature_id, design_doc, front, text)
    v6_summary = v6_modeling_spec_summary(civilization, creature_id)
    approved = status == "approved_for_modeling"
    return {
        "civilization": civilization,
        "id": creature_id,
        "entity_id": item.get("entity_id"),
        "status": status,
        "approved_for_modeling": approved,
        "reason": reason,
        "design_doc": project_path(design_doc) if design_doc.exists() else str(design_doc_text),
        "design_doc_exists": design_doc.exists(),
        "has_formal_front_matter": bool(front),
        "front_matter": front,
        "required_sections": REQUIRED_DESIGN_SECTIONS,
        "missing_sections": missing_sections,
        "section_complete": not missing_sections,
        "artifact_checks": artifact_checks,
        "artifact_complete": all(artifact_checks.values()),
        "approval_failures": approval_failures,
        "v6_modeling_spec": v6_summary,
        "next_action": (
            "proceed_to_modeling_from_approved_design"
            if approved
            else "approve_or_rewrite_design_before_modeling"
            if status.startswith("blocked")
            else "human_review_design_package_and_record_approval"
        ),
        "must_not": [
            "Do not treat generated design docs or previews as approval.",
            "Do not rebuild model, UV, texture, animation, or runtime Java from an unapproved design.",
            "Do not use oversized heads to compensate for missing face or signature details.",
        ],
    }


def build_report() -> dict[str, Any]:
    creatures: list[dict[str, Any]] = []
    manifest_status: dict[str, dict[str, Any]] = {}
    for civilization, manifest_path in MANIFESTS.items():
        manifest = load_json(manifest_path, {})
        items = manifest.get("creatures", []) if isinstance(manifest, dict) else []
        manifest_status[civilization] = {
            "path": project_path(manifest_path),
            "exists": manifest_path.exists(),
            "creature_count": len(items),
        }
        for item in items:
            if isinstance(item, dict):
                creatures.append(audit_creature(civilization, item))

    status_counts = Counter(item["status"] for item in creatures)
    section_incomplete = [item for item in creatures if not item["section_complete"]]
    artifact_incomplete = [item for item in creatures if not item["artifact_complete"]]
    approved_count = sum(1 for item in creatures if item["approved_for_modeling"])
    blocked_count = sum(1 for item in creatures if item["status"].startswith("blocked"))
    manual_required_count = sum(1 for item in creatures if item["status"] == "manual_design_approval_required")
    ok = (
        creatures
        and approved_count == len(creatures)
        and not section_incomplete
        and not artifact_incomplete
    )
    return {
        "version": 1,
        "scope": "mythic_age_1_0_full_creature_design_first_gate",
        "ok": ok,
        "rule": (
            "A creature is design-first ready only when its design is formally approved for modeling. "
            "Generated design docs, concept art, Blockbench previews, and runtime model gates are evidence, not approval."
        ),
        "creature_count": len(creatures),
        "approved_count": approved_count,
        "blocked_count": blocked_count,
        "manual_design_approval_required_count": manual_required_count,
        "section_incomplete_count": len(section_incomplete),
        "artifact_incomplete_count": len(artifact_incomplete),
        "status_counts": dict(sorted(status_counts.items())),
        "manifest_status": manifest_status,
        "design_correctness_report": project_path(DESIGN_CORRECTNESS_REPORT),
        "greek_design_first_report": project_path(GREEK_DESIGN_FIRST_REPORT),
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 全生物设计先行门禁（2026-06-15）",
        "",
        "本文档由 `tools/creature_design_first_gate.py audit` 生成。它检查 30 个 1.0 生物是否真的完成“设计先行审批”，而不只是在流水线里生成了设计包、概念图和预览。",
        "",
        "## 总览",
        "",
        f"- 门禁通过：`{payload['ok']}`",
        f"- 生物数量：{payload['creature_count']}",
        f"- 正式批准可建模：{payload['approved_count']}",
        f"- 设计拒绝/阻断：{payload['blocked_count']}",
        f"- 仅有设计包但未人工签收：{payload['manual_design_approval_required_count']}",
        f"- 章节不完整：{payload['section_incomplete_count']}",
        f"- 资产证据不完整：{payload['artifact_incomplete_count']}",
        f"- 状态统计：`{json.dumps(payload['status_counts'], ensure_ascii=False)}`",
        "",
        "## 判定规则",
        "",
        f"- {payload['rule']}",
        "- `pegasus` 和 `hydra` 当前仍由 V6 可建模规格签收 gate 阻断；旧 V2/当前产物不能继续作为建模依据。",
        "- 其余生物虽然有设计包和预览，但缺正式 `approved_for_modeling` 记录，仍需人工设计签收。",
        "",
        "## 生物列表",
        "",
        "| 文明 | 生物 | 状态 | 设计包 | 章节完整 | 资产完整 | 下一步 |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in payload["creatures"]:
        lines.append(
            "| {civ} | `{cid}` | `{status}` | `{design_doc}` | `{section}` | `{artifact}` | `{next_action}` |".format(
                civ=item["civilization"],
                cid=item["id"],
                status=item["status"],
                design_doc=item["design_doc"],
                section=item["section_complete"],
                artifact=item["artifact_complete"],
                next_action=item["next_action"],
            )
        )
    lines.extend(["", "## 阻断详情", ""])
    for item in payload["creatures"]:
        if item["approved_for_modeling"]:
            continue
        lines.extend([
            f"### {item['civilization']} / {item['id']}",
            "",
            f"- 状态：`{item['status']}`",
            f"- 原因：{item['reason']}",
            f"- 设计包：`{item['design_doc']}`",
        ])
        if item["missing_sections"]:
            lines.append(f"- 缺失章节：`{json.dumps(item['missing_sections'], ensure_ascii=False)}`")
        if item["approval_failures"]:
            lines.append(f"- 审批缺口：`{json.dumps(item['approval_failures'], ensure_ascii=False)}`")
        if item["v6_modeling_spec"]:
            lines.append(f"- V6 可建模规格合同：`{item['v6_modeling_spec'].get('spec_contract')}`")
        lines.append(f"- 下一步：`{item['next_action']}`")
        lines.append("")
    lines.extend([
        "## 退出标准",
        "",
        "- 30 个生物均有正式 `approved_for_modeling` 设计状态。",
        "- 设计包章节完整，且概念图、`.bbmodel`、UV、贴图和三视图证据齐全。",
        "- Pegasus/Hydra 的 V6 可建模规格合同、规格图、人工签收和设计正确性 gate 均通过。",
        "- 只有通过本 gate 后，后续模型、动作、纹理和 runtime 同步才能作为 1.0 正式验收证据。",
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
        print(f"Creature Design-First Gate: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"approved: {payload['approved_count']}/{payload['creature_count']}")
        print(f"status_counts: {payload['status_counts']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
