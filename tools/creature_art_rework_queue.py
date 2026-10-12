#!/usr/bin/env python3
"""Generate the 1.0 creature art rework queue.

The visual acceptance package contains automatic evidence. The human ledger
contains release decisions. This queue turns both into an implementation order
for model, UV, texture, animation, and runtime sync work.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
VISUAL_PACKAGE = ROOT / "docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json"
HUMAN_LEDGER = ROOT / "docs/preview_assets/creature_visual_acceptance/human_visual_acceptance_ledger.json"
PEGASUS_HYDRA_DESIGN_CORRECTNESS = ROOT / "docs/preview_assets/greek_creatures/design_correctness/pegasus_hydra_design_correctness_gate.json"
PEGASUS_HYDRA_V6_MODELING_SPEC = ROOT / "docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json"
OUT_DIR = ROOT / "docs/preview_assets/creature_art_rework_queue"
REPORT_PATH = OUT_DIR / "creature_art_rework_queue.json"
MARKDOWN_PATH = ROOT / "docs/项目管理/MythicAgeMod_1.0生物美术重做队列_2026-06-15.md"

CIVILIZATION_ORDER = ["greek", "egyptian", "nordic", "chinese", "atlantean"]

CIVILIZATION_FOCUS = {
    "egyptian": {
        "anubis_warrior": "胡狼头、长耳、黑金面部、金/青金石护甲、权杖或长矛必须可读。",
        "scarab_guardian": "圣甲虫甲壳、分节腿、钳/角、腹甲纹和埃及金色符号必须突出。",
        "mummy_soldier": "绷带层、空洞眼、干枯脸、短矛/盾和破损布条必须分层。",
        "sphinx_sentinel": "狮身、人面/王冠、前爪、尾和守卫姿态必须一眼可读。",
        "sun_falcon": "猎鹰头、翼羽、太阳圆盘或日光纹、爪和金白羽色必须明确。",
    },
    "nordic": {
        "frost_giant": "冰霜巨人体量、冰晶肩背、粗臂、寒霜裂纹和北欧粗糙石/冰材质。",
        "draugr_warrior": "亡灵脸、塌陷眼、锈甲、破布、武器和寒光符文必须清楚。",
        "valkyrie": "女武神人形、翼盔、长矛/盾、披风和金属甲胄要区别于普通战士。",
        "fenrir_wolf": "巨狼头、长吻、鬃毛、利齿、爪、符文伤痕和低伏攻击姿态。",
        "dwarf_berserker": "矮人体型、胡须、圆盔、双斧、厚靴和重甲，不靠大头表现细节。",
    },
    "chinese": {
        "azure_dragon": "青龙分段蛇形龙身、角、须、鬃、爪、鳞片和云雷纹必须分层。",
        "qilin_guardian": "麒麟角、鬃毛、鳞身、蹄、尾焰/祥云和瑞兽姿态必须清楚。",
        "jiangshi": "僵尸官帽、符箓、青白脸、僵直袖臂、长袍和小跳姿态必须明确。",
        "nine_tailed_fox": "狐脸、耳、九条独立尾、尾尖层次和妖异眼神不能糊成毛团。",
        "stone_lion_guardian": "石狮卷鬃、宽嘴、獠牙、爪、石雕底座感和守门姿态。",
    },
    "atlantean": {
        "crystal_leviathan": "海兽长身、晶体背棘、鳍、发光裂纹和深海水晶材质。",
        "orichalcum_golem": "山铜板甲、核心光源、粗重四肢、铆钉/板缝和远古机械感。",
        "siren_priestess": "海妖祭司人形、鳍、法杖、头饰、鳞片裙和仪式符号。",
        "tidal_guardian": "潮汐守卫护甲、水波冠、盾/戟、贝壳/珊瑚纹和坚固轮廓。",
        "abyssal_ray": "深渊鳐宽扁轮廓、翼状鳍、尾刺、生物发光点和黑蓝深海材质。",
    },
}

DESIGN_BLOCKER_FOCUS = {
    "pegasus": [
        "V6 必须先成立原版成年马式横向白马身体，再成立双翼飞行坐骑。",
        "头部必须细长、位于胸腔前方，有真实吻部、眼、鼻孔、嘴线和耳，不能大头化。",
        "颈部从鬐甲/肩胛斜向前上方连接头部，不能竖直插在背上。",
        "翼根在肩后，骑乘空间不遮挡头颈或翼根，纯白材质必须有浅灰结构面。",
    ],
    "hydra": [
        "V6 默认显性九头；任何五头简化都必须先作为设计变更重新审批。",
        "九个 neck_socket 必须位于 shoulder_saddle，每个 neck_root 与 socket 几何重叠。",
        "每条颈至少 neck_root/neck_mid/neck_tip 三段，neck_tip 与 head_core 重叠。",
        "低伏长身和长尾优先，头部不靠放大表达细节，每个头有眼、颚、嘴线、牙或毒腺。",
    ],
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def key(civilization: str, creature_id: str) -> str:
    return f"{civilization}:{creature_id}"


def index_creatures(items: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for item in items:
        civilization = item.get("civilization")
        creature_id = item.get("id")
        if isinstance(civilization, str) and isinstance(creature_id, str):
            result[key(civilization, creature_id)] = item
    return result


def priority_for(civilization: str, creature_id: str, decision: str) -> str:
    if decision == "blocked_by_design_rejection":
        return "P0-DESIGN-BLOCKER"
    if decision == "pending_user_acceptance":
        return "P0-USER-ACCEPTANCE"
    if civilization == "greek":
        return "P0-GREEK-REVIEW"
    return "P1-CIV-ART-UPGRADE"


def phase_for(priority: str, decision: str) -> str:
    if priority == "P0-DESIGN-BLOCKER":
        return "design_rewrite_or_user_approval_before_modeling"
    if decision == "pending_user_acceptance":
        return "user_acceptance_or_return_to_design"
    if priority == "P0-GREEK-REVIEW":
        return "greek_shape_review_then_targeted_rebuild"
    return "civilization_art_upgrade_then_model_uv_texture_runtime_sync"


def work_items_for(civilization: str, creature_id: str, priority: str, decision: str) -> list[str]:
    if priority == "P0-DESIGN-BLOCKER":
        if creature_id == "pegasus":
            return [
                "审查并签收天马 V6 可建模规格：原版成年马式横向身体、细长前伸马头、斜向颈根、肩后翼根、骑乘空间。",
                "签收 V6 规格合同、规格图和设计正确性 gate 后才允许建模。",
                "从批准设计重新生成 Blockbench 模型、UV 模板、纯白但有灰面结构的纹理。",
                "同步 Java runtime model、飞行动作、自主飞行和骑乘飞行坐点。",
            ]
        return [
            "审查并签收九头蛇 V6 可建模规格：低伏长身、默认九头、九个 socket、每条颈真实连接身体。",
            "签收 V6 规格合同、规格图和设计正确性 gate 后才允许建模。",
            "从批准设计重新生成 Blockbench 模型、UV 模板、深绿鳞片/毒性高光纹理。",
            "同步 Java runtime model 和多头错相 idle/attack/hurt 动作，保证不断颈。",
        ]
    if decision == "pending_user_acceptance":
        return [
            "让用户审查当前修订板，决定通过、返修或回设计重写。",
            "如果返修，先更新设计包和局部结构拆解，再改模型/UV/纹理/runtime。",
            "重点确认头身比例、标志五官/武器、动作可读性和材质不再占位。",
        ]
    if civilization == "greek":
        return [
            "人工审查三视图、texture-fit、runtime model 和视觉 review board。",
            "若形体通过，只做 UV/纹理/细节 polish；若不通过，先更新设计包再重建。",
            "检查神话标志部件、眼嘴、武器/翼/尾/蛇发、多头连接和比例不得靠大头补细节。",
        ]
    return [
        "按文明重新审查设计包，补足该文明的神话轮廓、材质语言和标志部件。",
        "必要时重建 Blockbench 模型，固定部件层级和 UV 区域。",
        "按 UV 重绘最终纹理，避免随机面板/占位感。",
        "同步 Java runtime model、动作需求、粒子/音频/UI 命名和视觉验收证据。",
    ]


def exit_gate_for(priority: str, decision: str) -> list[str]:
    gates = [
        "human_visual_acceptance_ledger decision=approved",
        "checklist 无 pending/fail",
        "creature_visual_acceptance_package ok=true",
        "runtime model 与 Blockbench 设计一致",
        "构建 ./gradlew build --offline 通过",
    ]
    if priority == "P0-DESIGN-BLOCKER":
        gates.insert(0, "pegasus_hydra_design_correctness_gate ok=true")
        gates.insert(1, "greek_design_first_gate ok=true")
    if decision == "pending_user_acceptance":
        gates.insert(0, "用户接受当前修订或明确进入返修分支")
    return gates


def must_not_for(priority: str) -> list[str]:
    base = [
        "不得用大头弥补眼嘴、牙、角、吻部、武器或尾翼细节。",
        "不得把自动比例/UV gate 当成人工美术通过。",
        "不得用通用随机面板纹理作为最终资源。",
    ]
    if priority == "P0-DESIGN-BLOCKER":
        base.insert(0, "设计未签收前不得继续生成 .bbmodel、纹理、动作或同步 Java model。")
        base.insert(1, "不得从当前 V2 产物反推最终设计。")
    return base


def art_focus_for(creature: dict[str, Any], civilization: str, creature_id: str, priority: str) -> list[str]:
    if priority == "P0-DESIGN-BLOCKER" and creature_id in DESIGN_BLOCKER_FOCUS:
        return DESIGN_BLOCKER_FOCUS[creature_id]
    target = creature.get("design_target", {}) if isinstance(creature.get("design_target"), dict) else {}
    signature = target.get("signature_features", [])
    reject_if = target.get("reject_if", [])
    focus: list[str] = []
    if isinstance(signature, list):
        focus.extend(str(item) for item in signature[:4])
    civ_focus = CIVILIZATION_FOCUS.get(civilization, {}).get(creature_id)
    if civ_focus:
        focus.insert(0, civ_focus)
    if isinstance(reject_if, list) and reject_if:
        focus.append("拒绝条件：" + "；".join(str(item) for item in reject_if[:3]))
    return focus


def evidence_for(creature: dict[str, Any], ledger: dict[str, Any]) -> dict[str, Any]:
    artifacts = creature.get("artifacts", {}) if isinstance(creature.get("artifacts"), dict) else {}
    return {
        "visual_review_board": creature.get("visual_review_board") or ledger.get("visual_review_board"),
        "design_doc": artifacts.get("design_doc") or ledger.get("design_doc"),
        "concept_art": artifacts.get("concept_art") or ledger.get("concept_art"),
        "bbmodel": artifacts.get("bbmodel") or ledger.get("bbmodel"),
        "texture": artifacts.get("texture") or ledger.get("texture"),
        "uv_template": artifacts.get("uv_template") or ledger.get("uv_template"),
        "texture_fit_preview": artifacts.get("texture_fit_preview") or ledger.get("texture_fit_preview"),
        "blockbench_previews": artifacts.get("blockbench_previews") or ledger.get("blockbench_previews"),
        "design_rejection": creature.get("design_rejection") or ledger.get("design_rejection"),
    }


def build_queue() -> dict[str, Any]:
    visual_package = load_json(VISUAL_PACKAGE, {})
    human_ledger = load_json(HUMAN_LEDGER, {})
    design_correctness = load_json(PEGASUS_HYDRA_DESIGN_CORRECTNESS, {})
    v6_modeling_spec = load_json(PEGASUS_HYDRA_V6_MODELING_SPEC, {})
    visual_map = index_creatures(visual_package.get("creatures", []))
    ledger_map = index_creatures(human_ledger.get("creatures", []))
    queue: list[dict[str, Any]] = []

    for queue_key, creature in sorted(
        visual_map.items(),
        key=lambda item: (
            CIVILIZATION_ORDER.index(item[1].get("civilization", "zz"))
            if item[1].get("civilization") in CIVILIZATION_ORDER
            else 99,
            item[1].get("id", ""),
        ),
    ):
        civilization = creature["civilization"]
        creature_id = creature["id"]
        ledger = ledger_map.get(queue_key, {})
        decision = ledger.get("decision") or "pending_shape_review"
        priority = priority_for(civilization, creature_id, str(decision))
        item = {
            "key": queue_key,
            "civilization": civilization,
            "id": creature_id,
            "entity_id": creature.get("entity_id") or ledger.get("entity_id"),
            "priority": priority,
            "phase": phase_for(priority, str(decision)),
            "source_status": creature.get("status"),
            "human_decision": decision,
            "automatic_evidence_ok": bool(creature.get("automatic_evidence_ok")),
            "visual_quality_contract_ok": bool(creature.get("visual_quality_contract", {}).get("ok"))
            if isinstance(creature.get("visual_quality_contract"), dict)
            else bool(ledger.get("visual_quality_contract_ok")),
            "runtime_proportion_ok": bool(creature.get("runtime_proportion_summary", {}).get("ok"))
            if isinstance(creature.get("runtime_proportion_summary"), dict)
            else bool(ledger.get("runtime_proportion_ok")),
            "art_focus": art_focus_for(creature, civilization, creature_id, priority),
            "work_items": work_items_for(civilization, creature_id, priority, str(decision)),
            "must_not": must_not_for(priority),
            "exit_gate": exit_gate_for(priority, str(decision)),
            "evidence": evidence_for(creature, ledger),
            "next_action": (
                "block_modeling_until_design_approval"
                if priority == "P0-DESIGN-BLOCKER"
                else "perform_human_visual_review"
                if str(decision) == "pending_shape_review"
                else "obtain_user_acceptance_or_rework"
                if str(decision) == "pending_user_acceptance"
                else "resolve_manual_decision"
            ),
        }
        queue.append(item)

    priority_counts = Counter(item["priority"] for item in queue)
    civilization_counts = Counter(item["civilization"] for item in queue)
    decision_counts = Counter(str(item["human_decision"]) for item in queue)
    approved_count = decision_counts.get("approved", 0)
    unresolved = [item for item in queue if item["human_decision"] != "approved"]
    non_greek_upgrade_count = sum(1 for item in queue if item["priority"] == "P1-CIV-ART-UPGRADE")
    return {
        "version": 1,
        "scope": "mythic_age_1_0_creature_art_rework_queue",
        "ok": not unresolved,
        "source_visual_package": project_path(VISUAL_PACKAGE),
        "source_human_ledger": project_path(HUMAN_LEDGER),
        "pegasus_hydra_design_correctness_report": project_path(PEGASUS_HYDRA_DESIGN_CORRECTNESS),
        "pegasus_hydra_design_correctness_ok": bool(design_correctness.get("ok")),
        "pegasus_hydra_design_correctness_verdict": design_correctness.get("verdict"),
        "pegasus_hydra_v6_modeling_spec_report": project_path(PEGASUS_HYDRA_V6_MODELING_SPEC),
        "pegasus_hydra_v6_modeling_spec_status": v6_modeling_spec.get("status"),
        "pegasus_hydra_v6_modeling_spec_verdict": v6_modeling_spec.get("verdict"),
        "pegasus_hydra_v6_modeling_allowed": v6_modeling_spec.get("modeling_allowed"),
        "creature_count": len(queue),
        "approved_count": approved_count,
        "unresolved_count": len(unresolved),
        "blocked_count": priority_counts.get("P0-DESIGN-BLOCKER", 0),
        "non_greek_upgrade_count": non_greek_upgrade_count,
        "priority_counts": dict(sorted(priority_counts.items())),
        "civilization_counts": dict(sorted(civilization_counts.items())),
        "decision_counts": dict(sorted(decision_counts.items())),
        "rule": (
            "Automatic evidence only places creatures in the queue. A creature exits the queue "
            "only after human visual approval and relevant design gates pass."
        ),
        "queue": queue,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 生物美术重做队列（2026-06-15）",
        "",
        "本文档由 `tools/creature_art_rework_queue.py audit` 生成。它把自动视觉证据、人工验收台账和天马/九头蛇设计正确性 gate 合并成 30 个生物的后续执行队列。",
        "",
        "## 总览",
        "",
        f"- 队列通过：`{payload['ok']}`",
        f"- 生物数量：{payload['creature_count']}",
        f"- 已人工批准：{payload['approved_count']}",
        f"- 未解决：{payload['unresolved_count']}",
        f"- 设计阻断：{payload['blocked_count']}",
        f"- 非希腊 20 生物美术升级：{payload['non_greek_upgrade_count']}",
        f"- 优先级统计：`{json.dumps(payload['priority_counts'], ensure_ascii=False)}`",
        f"- 文明统计：`{json.dumps(payload['civilization_counts'], ensure_ascii=False)}`",
        f"- 人工决策统计：`{json.dumps(payload['decision_counts'], ensure_ascii=False)}`",
        f"- 天马/九头蛇设计正确性：`{payload['pegasus_hydra_design_correctness_verdict']}`",
        f"- 天马/九头蛇 V6 可建模规格：`{payload['pegasus_hydra_v6_modeling_spec_status']}` / `modeling_allowed={payload['pegasus_hydra_v6_modeling_allowed']}`",
        "",
        "## 优先级含义",
        "",
        "- `P0-DESIGN-BLOCKER`：设计已拒绝，禁止建模，必须先签收设计。",
        "- `P0-USER-ACCEPTANCE`：已修订但等待用户接受或要求返修。",
        "- `P0-GREEK-REVIEW`：希腊 vertical slice 中待人工形体审查。",
        "- `P1-CIV-ART-UPGRADE`：非希腊文明生物需要按文明美术标准升级到 1.0 成品级。",
        "",
        "## 队列",
        "",
        "| 优先级 | 文明 | 生物 | 决策 | 阶段 | 下一步 |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for item in payload["queue"]:
        lines.append(
            f"| `{item['priority']}` | {item['civilization']} | `{item['id']}` | `{item['human_decision']}` | `{item['phase']}` | `{item['next_action']}` |"
        )

    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in payload["queue"]:
        grouped[item["priority"]].append(item)
    lines.extend(["", "## 详细执行项", ""])
    for priority in ["P0-DESIGN-BLOCKER", "P0-USER-ACCEPTANCE", "P0-GREEK-REVIEW", "P1-CIV-ART-UPGRADE"]:
        items = grouped.get(priority, [])
        if not items:
            continue
        lines.extend([f"### {priority}", ""])
        for item in items:
            lines.extend([
                f"#### {item['civilization']} / `{item['id']}`",
                "",
                f"- 当前状态：`{item['source_status']}` / `{item['human_decision']}`",
                f"- 阶段：`{item['phase']}`",
                f"- 视觉 board：`{item['evidence'].get('visual_review_board')}`",
                "- 美术重点：",
            ])
            for focus in item["art_focus"][:6]:
                lines.append(f"  - {focus}")
            lines.append("- 工作项：")
            for work in item["work_items"]:
                lines.append(f"  - {work}")
            lines.append("- 禁止项：")
            for rule in item["must_not"]:
                lines.append(f"  - {rule}")
            lines.append("- 退出 gate：")
            for gate in item["exit_gate"]:
                lines.append(f"  - {gate}")
            lines.append("")

    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_queue()
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        status = "PASS" if payload["ok"] else "OPEN"
        print(f"Creature Art Rework Queue: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"creatures: {payload['creature_count']}, unresolved: {payload['unresolved_count']}")
        print(f"priority_counts: {payload['priority_counts']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
