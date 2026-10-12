#!/usr/bin/env python3
"""Audit whether Pegasus and Hydra are actually approved designs.

This is intentionally stricter than mechanical proportion gates. A runtime
model can pass size ratios while still being the wrong creature design. This
gate answers the current user concern directly: are Pegasus and Hydra correct
enough to model?
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/greek_creatures/design_correctness"
REPORT_PATH = OUT_DIR / "pegasus_hydra_design_correctness_gate.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_current_design_verdict_2026-06-15.md"

MODEL_SPEC_CONTRACTS = {
    "pegasus": ROOT / "docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md",
    "hydra": ROOT / "docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md",
}

DESIGN_FIRST_GATE = ROOT / "docs/preview_assets/greek_creatures/design_first_gate.json"
DETAIL_BOARD_PACKAGE = ROOT / "docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_hydra_detail_design_boards.json"
RUNTIME_PROPORTION_REPORT = ROOT / "docs/preview_assets/creature_visual_acceptance/creature_runtime_proportion_quality_report.json"
V4_CANDIDATE_PACKAGE_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_approval/v4/pegasus_hydra_v4_candidate_package.json"
V4_CANDIDATE_MARKDOWN = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v4_candidate_package_2026-06-15.md"
V5_CANDIDATE_PACKAGE_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_approval/v5/pegasus_hydra_v5_design_approval_package.json"
V5_CANDIDATE_MARKDOWN = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v5_design_approval_package_2026-06-15.md"
V6_CANDIDATE_PACKAGE_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json"
V6_CANDIDATE_MARKDOWN = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v6_modeling_spec_package_2026-06-15.md"

CURRENT_ASSET_PATHS = {
    "pegasus": {
        "bbmodel": ROOT / "docs/美术资源/blockbench/greek_creatures/pegasus.bbmodel",
        "preview_side": ROOT / "docs/preview_assets/greek_creatures/pegasus/blockbench/side.png",
        "preview_three_quarter": ROOT / "docs/preview_assets/greek_creatures/pegasus/blockbench/three_quarter.png",
    },
    "hydra": {
        "bbmodel": ROOT / "docs/美术资源/blockbench/greek_creatures/hydra.bbmodel",
        "preview_side": ROOT / "docs/preview_assets/greek_creatures/hydra/blockbench/side.png",
        "preview_three_quarter": ROOT / "docs/preview_assets/greek_creatures/hydra/blockbench/three_quarter.png",
    },
}

RUNTIME_MODEL_PATHS = {
    "pegasus": ROOT / "src/main/java/com/example/mythic_age/client/render/entity/model/PegasusModel.java",
    "hydra": ROOT / "src/main/java/com/example/mythic_age/client/render/entity/model/HydraModel.java",
}

REQUIRED_APPROVAL_FIELDS = [
    "design_status",
    "design_sheet_status",
    "design_approval_status",
    "design_correctness_verdict",
    "design_detail_sheet_status",
    "design_detail_board_status",
]

APPROVED = "approved_for_modeling"

CREATURE_RULES = {
    "pegasus": {
        "display": "Pegasus / 天马",
        "current_problem": "旧实现/当前 V2 已畸形：马体比例不成立，头颈关系不稳定，不能继续调尺寸当作修复。",
        "latest_user_feedback": [
            "当前生成天马头部过大，不能继续用大头换取细节。",
            "天马必须先参考原版成年马体比例，再叠加纯白、双翼和骑乘飞行能力。",
            "当前实现看不出稳定马头/马颈/马身关系，必须回到设计阶段。",
        ],
        "current_visual_failures": [
            "当前 Blockbench 侧面预览不能先读成成年白马；躯干、长颈、头和翼面像堆叠零件。",
            "头颈比例和锚点不稳定，头部可读性依赖局部放大/方块堆叠，而不是马式小头、细长吻部和斜向颈。",
            "翼面在视觉上压过马体结构，未证明肩后翼根、骑乘空间和头颈之间的正确空间关系。",
        ],
        "candidate_problem": "现有 V6 sheet 已经把方向转成可建模规格，但仍未被用户签收；未签收前不能生成 `.bbmodel`、UV、纹理或 runtime。",
        "latest_candidate_design_verdict": "modeling_spec_concrete_but_unapproved",
        "candidate_direction": "先按原版成年马式横向马体成立白马，再加入双翼、自主飞行和骑乘飞行。",
        "must_prove": [
            "侧面先读成白马，而不是白色方块或大头玩具马。",
            "头部细长且位于胸腔前方；吻部、眼、鼻孔、嘴线和耳由部件/UV 表达。",
            "颈部从鬐甲/肩胛斜向前上方伸出，不是竖直白柱。",
            "翼根在肩后，骑乘空间不遮挡头颈或翼根。",
            "设计图必须达到可建模精度，不能只用几何示意替代生物造型图。",
        ],
    },
    "hydra": {
        "display": "Hydra / 九头蛇",
        "current_problem": "旧实现/当前 V2 已畸形：头、颈、肩带和身体没有建立可靠实体连接。",
        "latest_user_feedback": [
            "当前九头蛇头和身体没有连接上，不能继续在旧结构上修补。",
            "当前实现头数和九头蛇设计不一致，五头 runtime/Blockbench 不能进入九头蛇流程。",
            "头部大小必须匹配身体比例，细节来自眼、嘴、颌、牙、角/毒腺部件和 UV，而不是放大头部。",
        ],
        "current_visual_failures": [
            "当前 Blockbench 预览不是九头扇形蛇怪，而是五头/头墙式箱体堆叠。",
            "侧面没有证明每条颈从 shoulder_saddle 长出并连接到头，头颈身体连续性失败。",
            "压迫感来自臃肿大块和大头堆叠，不是低伏长身、长尾、九头错层和单头面部细节。",
        ],
        "candidate_problem": "现有 V6 sheet 已经把九头、socket 和连续颈路径转成可建模规格，但仍未被用户签收；未签收前不能生成 `.bbmodel`、UV、纹理或 runtime。",
        "latest_candidate_design_verdict": "modeling_spec_concrete_but_unapproved",
        "candidate_direction": "低伏长身、默认九头扇形错层，每条颈从肩带 socket 生长并与头部重叠连接。",
        "must_prove": [
            "默认九头方案在设计阶段确定；如果要改五头，必须先改设计包。",
            "九个 neck_socket 位于 shoulder_saddle，且每个 neck_root 与 socket 几何重叠。",
            "每条颈至少 neck_root/neck_mid/neck_tip 三段，neck_tip 与 head_core 重叠。",
            "身体低而长，头部不靠放大获得细节，每个头有眼、颚、嘴线、牙或毒腺。",
            "设计图必须显示头颈身体的连续体积，不允许只有拓扑命名或分离柱状颈。",
        ],
    },
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def read_front_matter(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    lines = path.read_text(encoding="utf-8").splitlines()
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


def runtime_summary_for(creature_id: str, report: dict[str, Any]) -> dict[str, Any]:
    for item in report.get("creatures", []):
        if isinstance(item, dict) and item.get("id") == creature_id:
            return {
                "runtime_proportion_ok": bool(item.get("ok")),
                "note": "mechanical runtime proportions are evidence only; they do not approve design correctness",
            }
    return {
        "runtime_proportion_ok": None,
        "note": "runtime proportion report does not contain this creature",
    }


def detail_status_for(creature_id: str, package: dict[str, Any]) -> str | None:
    for item in package.get("creatures", []):
        if isinstance(item, dict) and item.get("id") == creature_id:
            status = item.get("status")
            return status if isinstance(status, str) else None
    return None


def candidate_for(creature_id: str, package: dict[str, Any]) -> dict[str, Any]:
    for item in package.get("creatures", []):
        if isinstance(item, dict) and item.get("id") == creature_id:
            return {
                "status": item.get("status"),
                "approved_for_modeling": bool(item.get("approved_for_modeling")),
                "modeling_allowed": bool(item.get("modeling_allowed")),
                "contract": item.get("contract"),
                "candidate_sheet": item.get("candidate_sheet"),
                "candidate_design_verdict": item.get("candidate_design_verdict"),
                "supersedes": item.get("supersedes"),
                "user_problem_answered": item.get("user_problem_answered", []),
                "must_approve_before_modeling": item.get("must_approve_before_modeling", []),
            }
    return {
        "status": "missing",
        "approved_for_modeling": False,
        "modeling_allowed": False,
        "contract": None,
        "candidate_sheet": None,
        "candidate_design_verdict": None,
        "supersedes": None,
        "user_problem_answered": [],
        "must_approve_before_modeling": [],
    }


def load_bbmodel_elements(path: Path) -> list[dict[str, Any]]:
    data = load_json(path, {})
    elements = data.get("elements") if isinstance(data, dict) else None
    if not isinstance(elements, list):
        return []
    return [item for item in elements if isinstance(item, dict)]


def element_extent(element: dict[str, Any]) -> dict[str, float] | None:
    start = element.get("from")
    end = element.get("to")
    if not (
        isinstance(start, list)
        and isinstance(end, list)
        and len(start) == 3
        and len(end) == 3
    ):
        return None
    try:
        x = abs(float(end[0]) - float(start[0]))
        y = abs(float(end[1]) - float(start[1]))
        z = abs(float(end[2]) - float(start[2]))
    except (TypeError, ValueError):
        return None
    return {"x": round(x, 3), "y": round(y, 3), "z": round(z, 3)}


def named_extent(elements: list[dict[str, Any]], name: str) -> dict[str, float] | None:
    for element in elements:
        if element.get("name") == name:
            return element_extent(element)
    return None


def current_asset_evidence(creature_id: str) -> dict[str, Any]:
    paths = CURRENT_ASSET_PATHS[creature_id]
    bbmodel = paths["bbmodel"]
    elements = load_bbmodel_elements(bbmodel)
    names = [str(item.get("name")) for item in elements if item.get("name")]
    evidence: dict[str, Any] = {
        "bbmodel": project_path(bbmodel),
        "bbmodel_exists": bbmodel.exists(),
        "element_count": len(elements),
        "preview_side": project_path(paths["preview_side"]),
        "preview_side_exists": paths["preview_side"].exists(),
        "preview_three_quarter": project_path(paths["preview_three_quarter"]),
        "preview_three_quarter_exists": paths["preview_three_quarter"].exists(),
    }
    if creature_id == "pegasus":
        body = named_extent(elements, "body")
        head = named_extent(elements, "head")
        snout = named_extent(elements, "snout")
        neck = named_extent(elements, "neck")
        required_parts = [
            "body",
            "chest",
            "neck",
            "head",
            "snout",
            "left_eye",
            "right_eye",
            "left_nostril",
            "right_nostril",
            "nose_mouth",
            "left_wing_primary",
            "right_wing_primary",
            "saddle_space",
        ]
        missing = [name for name in required_parts if name not in names]
        evidence.update({
            "structural_verdict": "rejected_visual_proportion_not_horse_like",
            "body_extent": body,
            "head_extent": head,
            "snout_extent": snout,
            "neck_extent": neck,
            "head_width_to_body_width": (
                round(head["x"] / body["x"], 3)
                if body and head and body["x"]
                else None
            ),
            "required_named_parts_missing": missing,
            "note": (
                "Named parts exist, but the current preview still fails the design test: "
                "it does not read as an adult white horse before reading as a winged unit."
            ),
        })
    else:
        head_names = sorted(name for name in names if name.endswith("_head") or name == "head")
        socket_like = sorted(name for name in names if "head_socket" in name or "neck_anchor" in name)
        body_overlaps = sorted(name for name in names if "neck_body_overlap" in name)
        head_overlaps = sorted(name for name in names if "neck_head_overlap" in name)
        evidence.update({
            "structural_verdict": "rejected_wrong_head_count_and_connection_topology",
            "required_head_count": 9,
            "actual_head_count": len(head_names),
            "head_names": head_names,
            "socket_or_anchor_count": len(socket_like),
            "neck_body_overlap_count": len(body_overlaps),
            "neck_head_overlap_count": len(head_overlaps),
            "note": (
                "The current bbmodel is a five-head implementation. It cannot satisfy the approved-target "
                "Hydra design contract that requires nine heads, nine sockets, and nine connected neck paths."
            ),
        })
    return evidence


def java_string_array(text: str, array_name: str) -> list[str]:
    match = re.search(
        rf"String\[\]\s+{re.escape(array_name)}\s*=\s*\{{(?P<body>.*?)\}};",
        text,
        re.DOTALL,
    )
    if not match:
        return []
    return re.findall(r'"([^"]+)"', match.group("body"))


def runtime_model_evidence(creature_id: str) -> dict[str, Any]:
    path = RUNTIME_MODEL_PATHS[creature_id]
    text = path.read_text(encoding="utf-8") if path.exists() else ""
    evidence: dict[str, Any] = {
        "java_model": project_path(path),
        "java_model_exists": path.exists(),
    }
    if creature_id == "pegasus":
        evidence.update({
            "runtime_verdict": "blocked_runtime_model_is_implementation_of_rejected_visual_design",
            "has_flight_animation": "entity.isFlying()" in text and "leftWing" in text and "rightWing" in text,
            "has_rider_slot_marker": "saddleSpace.visible" in text,
            "horse_like_design_approved": False,
            "note": (
                "Runtime has head, snout, wings, flight animation, and saddle marker, but these facts do not "
                "prove the design is correct. The visual side preview fails the adult-horse-first requirement, "
                "so this runtime model must be regenerated from an approved design rather than patched in place."
            ),
        })
    else:
        neck_names = java_string_array(text, "NECK_NAMES")
        head_names = java_string_array(text, "HEAD_NAMES")
        evidence.update({
            "runtime_verdict": "rejected_runtime_head_count_mismatch",
            "required_head_count": 9,
            "runtime_neck_count": len(neck_names),
            "runtime_head_count": len(head_names),
            "runtime_neck_names": neck_names,
            "runtime_head_names": head_names,
            "note": (
                "Runtime Hydra is still a five-neck/five-head model. It cannot satisfy the current nine-head "
                "design contract and cannot be used as the Java sync target until the design is approved and rebuilt."
            ),
        })
    return evidence


def audit_creature(creature_id: str) -> dict[str, Any]:
    spec_contract = MODEL_SPEC_CONTRACTS[creature_id]
    front = read_front_matter(spec_contract)
    detail_package = load_json(DETAIL_BOARD_PACKAGE, {})
    runtime_report = load_json(RUNTIME_PROPORTION_REPORT, {})
    v4_package = load_json(V4_CANDIDATE_PACKAGE_REPORT, {})
    v5_package = load_json(V5_CANDIDATE_PACKAGE_REPORT, {})
    v6_package = load_json(V6_CANDIDATE_PACKAGE_REPORT, {})
    latest_candidate = candidate_for(creature_id, v6_package)
    if latest_candidate.get("status") == "missing":
        latest_candidate = candidate_for(creature_id, v5_package)
    if latest_candidate.get("status") == "missing":
        latest_candidate = candidate_for(creature_id, v4_package)
    statuses = {key: front.get(key) for key in REQUIRED_APPROVAL_FIELDS}
    modeling_allowed = front.get("modeling_allowed", "").lower() == "true"
    current_assets_rejected = front.get("current_v2_assets_status") == "rejected"
    approval_failures = [
        f"{key}={value!r}"
        for key, value in statuses.items()
        if value != APPROVED
    ]
    if not modeling_allowed:
        approval_failures.append("modeling_allowed=false")
    candidate_approved = not approval_failures
    rules = CREATURE_RULES[creature_id]
    return {
        "id": creature_id,
        "display": rules["display"],
        "modeling_spec_contract": project_path(spec_contract),
        "modeling_spec_contract_exists": spec_contract.exists(),
        "implemented_design_correct": False if current_assets_rejected else None,
        "implemented_design_verdict": (
            "rejected_not_correct"
            if current_assets_rejected
            else "unknown_current_v2_assets_status"
        ),
        "candidate_modeling_spec_verdict": (
            "approved_for_modeling"
            if candidate_approved
            else "modeling_spec_ready_but_not_user_approved"
        ),
        "candidate_modeling_spec_approved": candidate_approved,
        "modeling_allowed": modeling_allowed,
        "current_problem": rules["current_problem"],
        "latest_user_feedback": rules["latest_user_feedback"],
        "current_visual_failures": rules["current_visual_failures"],
        "current_asset_evidence": current_asset_evidence(creature_id),
        "runtime_model_evidence": runtime_model_evidence(creature_id),
        "candidate_problem": rules["candidate_problem"],
        "candidate_direction": rules["candidate_direction"],
        "latest_candidate_design_verdict": rules["latest_candidate_design_verdict"],
        "v4_candidate_verdict": "candidate_only_needs_user_review",
        "latest_candidate_verdict": "candidate_only_needs_user_review",
        "v4_candidate": latest_candidate,
        "latest_candidate": latest_candidate,
        "must_prove_before_modeling": rules["must_prove"],
        "approval_statuses": statuses,
        "design_detail_board_package_status": detail_package.get("status"),
        "design_detail_board_creature_status": detail_status_for(creature_id, detail_package),
        "approval_failures": approval_failures,
        "runtime_proportion_summary": runtime_summary_for(creature_id, runtime_report),
        "next_action": (
            "may_start_blockbench_rebuild_from_approved_v6_modeling_spec"
            if candidate_approved
            else "do_not_model_review_or_rewrite_v6_modeling_spec_first"
        ),
    }


def build_report() -> dict[str, Any]:
    creatures = [audit_creature(creature_id) for creature_id in MODEL_SPEC_CONTRACTS]
    design_first_gate = load_json(DESIGN_FIRST_GATE, {})
    v4_package = load_json(V4_CANDIDATE_PACKAGE_REPORT, {})
    v5_package = load_json(V5_CANDIDATE_PACKAGE_REPORT, {})
    v6_package = load_json(V6_CANDIDATE_PACKAGE_REPORT, {})
    blocked = [
        item["id"]
        for item in creatures
        if not item["candidate_modeling_spec_approved"]
    ]
    return {
        "version": 1,
        "scope": "pegasus_hydra_design_correctness_gate",
        "ok": not blocked,
        "verdict": (
            "approved_for_modeling"
            if not blocked
            else "blocked_current_design_not_correct_and_v6_spec_not_approved"
        ),
        "answer": (
            "Pegasus and Hydra are not currently correct designs. Their old/current V2 assets are rejected; "
            "the existing V6 sheets are concrete pre-modeling specifications, not final approved modeling designs."
        ),
        "rule": (
            "Mechanical size/proportion gates cannot override design approval. Pegasus and Hydra require "
            "approved V6 modeling spec contract, spec sheet, design correctness gate, and modeling_allowed=true; "
            "a technical diagram is not enough unless it also proves the creature silhouette and anatomical/topological design. "
            "The V6 package is concrete pre-modeling specification evidence, not approval."
        ),
        "blocked": blocked,
        "blocked_count": len(blocked),
        "design_first_gate_ok": bool(design_first_gate.get("ok")),
        "design_first_gate_report": project_path(DESIGN_FIRST_GATE),
        "detail_board_package": project_path(DETAIL_BOARD_PACKAGE),
        "runtime_proportion_report": project_path(RUNTIME_PROPORTION_REPORT),
        "v4_candidate_package_status": v4_package.get("status", "missing"),
        "v4_candidate_package_report": project_path(V4_CANDIDATE_PACKAGE_REPORT),
        "v4_candidate_markdown": project_path(V4_CANDIDATE_MARKDOWN),
        "v5_candidate_package_status": v5_package.get("status", "missing"),
        "v5_candidate_package_report": project_path(V5_CANDIDATE_PACKAGE_REPORT),
        "v5_candidate_markdown": project_path(V5_CANDIDATE_MARKDOWN),
        "v6_candidate_package_status": v6_package.get("status", "missing"),
        "v6_candidate_package_report": project_path(V6_CANDIDATE_PACKAGE_REPORT),
        "v6_candidate_markdown": project_path(V6_CANDIDATE_MARKDOWN),
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# Pegasus / Hydra 当前设计正确性结论（2026-06-15）",
        "",
        "## 结论",
        "",
        f"- 当前结论：`{payload['verdict']}`",
        f"- 是否允许建模：`{payload['ok']}`",
        "- 旧实现/当前 V2 不是正确设计，不能继续调模型尺寸当作修复。",
        "- 当前 `.bbmodel` 和 Blockbench 预览只作为失败证据保留，不作为重建基础。",
        "- 当前 Java runtime model 也不能作为同步目标：Pegasus runtime 属于错误视觉设计的实现，Hydra runtime 仍是五头。",
        "- V3/V5 是方向材料；V6 已经转成可建模规格，但仍未被用户签收，不得作为建模许可。",
        "- V6 规格包已把用户反馈转成 Blockbench 尺寸、层级、UV 和 runtime 重建门禁，但它仍不能自动放行建模。",
        "- 自动比例 gate 通过不能替代设计正确性签收。",
        "",
        "## 分项",
        "",
        "| 生物 | 当前实现设计正确 | 候选可建模 | 最新候选设计判断 | 最新候选状态 | 当前问题 | 最新规格问题 | 下一步 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in payload["creatures"]:
        lines.append(
            "| {display} | `{implemented}` | `{candidate}` | `{candidate_verdict}` | `{v4}` | {problem} | {candidate_problem} | `{next_action}` |".format(
                display=item["display"],
                implemented=item["implemented_design_verdict"],
                candidate=item["candidate_modeling_spec_verdict"],
                candidate_verdict=item["latest_candidate_design_verdict"],
                v4=item["latest_candidate"].get("status"),
                problem=item["current_problem"],
                candidate_problem=item["candidate_problem"],
                next_action=item["next_action"],
            )
        )
    lines.extend([
        "",
        "## 必须先证明的设计点",
        "",
    ])
    for item in payload["creatures"]:
        lines.extend([
            f"### {item['display']}",
            "",
            f"- 候选方向：{item['candidate_direction']}",
            f"- 当前建模许可：`{item['modeling_allowed']}`",
            f"- 审批失败项：`{json.dumps(item['approval_failures'], ensure_ascii=False)}`",
            f"- V6 可建模规格合同：`{item['modeling_spec_contract']}`",
            f"- 最新候选契约：`{item['latest_candidate'].get('contract')}`",
            f"- 最新候选设计图：`{item['latest_candidate'].get('candidate_sheet')}`",
            f"- 当前失败 `.bbmodel`：`{item['current_asset_evidence']['bbmodel']}`",
            f"- 当前失败预览：`{item['current_asset_evidence']['preview_side']}` / `{item['current_asset_evidence']['preview_three_quarter']}`",
            f"- 当前结构性结论：`{item['current_asset_evidence']['structural_verdict']}`",
            f"- Runtime 模型：`{item['runtime_model_evidence']['java_model']}`",
            f"- Runtime 结论：`{item['runtime_model_evidence']['runtime_verdict']}`",
            "- 本轮用户反馈：",
        ])
        for feedback in item["latest_user_feedback"]:
            lines.append(f"  - {feedback}")
        if item["id"] == "pegasus":
            lines.extend([
                f"- 当前头宽/身体宽比例证据：`{item['current_asset_evidence']['head_width_to_body_width']}`；这只能说明命名尺寸，不等于视觉上读成成年马。",
                f"- 当前命名部件缺失：`{json.dumps(item['current_asset_evidence']['required_named_parts_missing'], ensure_ascii=False)}`",
                f"- Runtime 飞行动画标记：`{item['runtime_model_evidence']['has_flight_animation']}`；骑乘位标记：`{item['runtime_model_evidence']['has_rider_slot_marker']}`。",
            ])
        if item["id"] == "hydra":
            lines.extend([
                f"- 当前 `.bbmodel` 头数：`{item['current_asset_evidence']['actual_head_count']}` / 设计要求 `9`。",
                f"- 当前头部命名：`{json.dumps(item['current_asset_evidence']['head_names'], ensure_ascii=False)}`",
                f"- 当前 neck/body overlap 数：`{item['current_asset_evidence']['neck_body_overlap_count']}`；当前 neck/head overlap 数：`{item['current_asset_evidence']['neck_head_overlap_count']}`。",
                f"- Runtime 头数：`{item['runtime_model_evidence']['runtime_head_count']}` / 设计要求 `9`。",
                f"- Runtime 头部命名：`{json.dumps(item['runtime_model_evidence']['runtime_head_names'], ensure_ascii=False)}`",
            ])
        lines.append("- 当前视觉失败点：")
        for failure in item["current_visual_failures"]:
            lines.append(f"  - {failure}")
        lines.append("- 建模前必须证明：")
        for rule in item["must_prove_before_modeling"]:
            lines.append(f"  - {rule}")
        lines.append("")
    lines.extend([
        "## 硬性门禁",
        "",
        "只有同时满足以下条件，才允许重新生成 `.bbmodel`、纹理、动作或同步 Java model：",
        "",
        "- `design_status=approved_for_modeling`",
        "- `design_sheet_status=approved_for_modeling`",
        "- `design_approval_status=approved_for_modeling`",
        "- `design_correctness_verdict=approved_for_modeling`",
        "- `design_detail_sheet_status=approved_for_modeling`",
        "- `design_detail_board_status=approved_for_modeling`",
        "- `modeling_allowed=true`",
        "- `tools/greek_design_first_gate.py audit` 与本 gate 均通过",
        "",
        "## 工作审视报告",
        "",
        "### 原定目标",
        "",
        "按设计先行方式确认天马和九头蛇是否正确，再决定是否进入建模。",
        "",
        "### 完成情况",
        "",
        "- [x] 已确认旧天马、旧九头蛇均不是正确设计。",
        "- [x] 已确认 V6 当前只是可审查规格，不允许直接开工建模。",
        "- [x] 已把机械比例 gate 与设计正确性 gate 区分开。",
        "- [ ] 未完成最终设计签收；需要用户确认或要求重画设计图。",
        "",
        "### 发现的问题",
        "",
        "| 严重程度 | 问题描述 | 根本原因 | 改进建议 |",
        "| --- | --- | --- | --- |",
        "| 必须改正 | 天马旧实现没有先成立原版马式身体和头颈关系，导致头大/看不出头这两类问题反复出现。 | 过早从模型调参进入实现，没有先锁定侧面马体比例。 | 先签收马体比例、头颈锚点、翼根和骑乘空间，再建模。 |",
        "| 必须改正 | 九头蛇旧实现没有先锁定九个 socket、颈段和头颈重叠关系，导致头和身体脱节。 | 多头拓扑没有在设计阶段拆到可建模粒度。 | 先签收九头拓扑、socket 排布和单头连接放大规则，再建模。 |",
        "| 必须改正 | 当前 Hydra `.bbmodel` 只有 5 个 `_head` 部件，却被放进九头蛇流程。 | 设计审批未通过时仍推进了实现产物，流程 gate 没有挡在最前。 | 在本 gate 未通过前，禁止把当前 `.bbmodel`、贴图和 Java runtime 作为后续依据。 |",
        "| 必须改正 | 当前 Pegasus `.bbmodel` 虽有头、吻部、眼、翼等命名部件，但预览不成立为成年马式天马。 | 把“有部件名”误当成“视觉造型正确”。 | 必须用三视图设计图先证明马体、头颈、翼根、骑乘位，再重建模型。 |",
        "| 必须改正 | V6 规格仍未签收，不能当作自动建模许可。 | 过去把候选图误当作可生产依据。 | 只有用户确认 V6 后，才能写回 `approved_for_modeling` 并开始重建。 |",
        "| 必须改正 | 之前的自动比例检查容易被误读为“设计正确”。 | 把机械阈值通过当成造型审批。 | 本 gate 必须作为 Pegasus/Hydra 的前置发布依据。 |",
        "",
        "### 下次重点关注",
        "",
        "- 不再基于当前 V2 `.bbmodel` 或 Java model 继续微调天马/九头蛇。",
        "- 不用放大头部换取眼嘴可读性，细节必须来自部件和 UV。",
        "- 设计图不签收，就不进入 Blockbench。",
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
        status = "PASS" if payload["ok"] else "BLOCKED"
        print(f"Pegasus/Hydra Design Correctness Gate: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"verdict: {payload['verdict']}")
        print(f"blocked: {payload['blocked_count']} {payload['blocked']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
