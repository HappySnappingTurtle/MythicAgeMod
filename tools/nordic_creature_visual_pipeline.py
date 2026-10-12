#!/usr/bin/env python3
"""Design-first visual workflow for Nordic 1.0 creature candidates.

This mirrors the Greek/Egyptian Blockbench gate and keeps Minecraft runtime
screenshots out of scope until the user asks for in-game validation again.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import math
import sys
from pathlib import Path
from typing import Any

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
GREEK_PIPELINE_PATH = ROOT / "tools/greek_creature_visual_pipeline.py"

spec = importlib.util.spec_from_file_location("greek_visual_base", GREEK_PIPELINE_PATH)
if spec is None or spec.loader is None:
    raise RuntimeError(f"Cannot load visual base pipeline: {GREEK_PIPELINE_PATH}")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

DESIGN_DIR = ROOT / "docs/美术资源/nordic_creatures/design"
CONCEPT_DIR = ROOT / "docs/美术资源/nordic_creatures/concepts"
BLOCKBENCH_DIR = ROOT / "docs/美术资源/blockbench/nordic_creatures"
PREVIEW_DIR = ROOT / "docs/preview_assets/nordic_creatures"
MANIFEST_PATH = ROOT / "docs/美术资源/nordic_creatures/visual_manifest.json"
REPORT_PATH = PREVIEW_DIR / "visual_report.json"
TEXTURE_FIT_REPORT_PATH = PREVIEW_DIR / "texture_fit_report.json"
CONTACT_SHEET_PATH = PREVIEW_DIR / "blockbench_validation_contact_sheet.png"
CONCEPT_CONTACT_SHEET_PATH = PREVIEW_DIR / "concept_design_contact_sheet.png"


NORDIC_CREATURES: list[dict[str, Any]] = [
    {
        "id": "frost_giant",
        "entity_id": "mythic_age:frost_giant",
        "display": "Frost Giant / 霜巨人",
        "target_box": [1.8, 3.9],
        "visual_height": 4.05,
        "scale_note": "高大寒霜巨人，比玩家高约两倍，宽肩、冰须和霜石棒必须清楚。",
        "primary": "#8fb8d8",
        "shadow": "#26394a",
        "accent": "#e6f6ff",
        "silhouette": {
            "front": "比例合理的巨人头脸、宽肩、冰须、冰蓝胸纹和巨大霜石棒一眼可读。",
            "side": "厚胸、前突鼻梁、长臂和石棒形成压迫侧影。",
            "back": "背部冰刺、宽肩和垂落兽皮披肩清楚分层。",
        },
        "parts": ["giant_head", "brow", "eyes", "ice_beard", "torso", "fur_pelt", "arms", "frost_club", "legs", "ice_spikes"],
        "hierarchy": ["root/head", "root/body", "root/arms", "root/weapon", "root/legs", "root/spikes"],
        "uv_plan": ["head_front: glowing eyes and brow", "beard: white ice strips", "torso: blue runes", "club: frosted stone"],
        "texture_detail": "蓝灰皮肤、白色冰须、胸口浅蓝符文、兽皮深棕边缘、霜石棒带冰裂纹。",
        "animation": "待机呼吸重；行走长臂摆动；攻击时霜石棒下砸；特殊震地时身体前压、头部稳定。",
        "signature_features": ["巨人体量", "冰须和冰刺", "巨大霜石棒", "蓝白寒霜配色"],
        "modeling_musts": ["frost_club 必须独立 weapon group", "ice_beard 必须有垂落分段", "眼睛和眉骨必须高对比"],
        "reject_if": ["像普通大僵尸", "没有冰须或冰刺", "武器不可读"],
        "v2_design_freeze": [
            "正面第一识别必须是巨人体量、冰须、冰刺和右手巨大霜石棒，不得像普通大人形。",
            "头脸必须有眉骨、发光眼和冰须分区；胸前冰蓝符文和兽皮披肩要与身体分层。",
            "视觉高度约 4 格；长臂、宽肩、背刺和霜石棒共同支撑霜巨人的压迫感。",
        ],
        "geometry": [
            ["torso", "body", [-7.5, -18, -4], [7.5, 0, 4], [0, 28]],
            ["fur_pelt", "body", [-8.3, -18.5, -4.8], [8.3, -12.0, 4.8], [52, 28]],
            ["rune_chest", "body", [-3.5, -13.5, -5.2], [3.5, -9.0, -4.2], [88, 28]],
            ["giant_head", "head", [-5.5, -11.0, -5.0], [5.5, 0.0, 5.0], [0, 0]],
            ["heavy_brow", "head", [-4.6, -7.8, -6.0], [4.6, -6.4, -5.0], [44, 0]],
            ["left_eye", "head", [-3.5, -6.3, -6.3], [-2.1, -5.0, -5.3], [62, 0]],
            ["right_eye", "head", [2.1, -6.3, -6.3], [3.5, -5.0, -5.3], [70, 0]],
            ["ice_beard", "head", [-4.0, -3.0, -6.0], [4.0, 4.8, -4.5], [78, 0]],
            ["back_spikes", "spikes", [-1.0, -18.8, 3.8], [1.0, -4.0, 6.8], [104, 0]],
            ["left_arm", "arms", [1.5, -3, -3.4], [8.8, 20, 3.4], [0, 58]],
            ["right_arm", "arms", [-8.8, -3, -3.4], [-1.5, 20, 3.4], [32, 58]],
            ["frost_club_handle", "weapon", [-12.0, -1.0, -1.4], [-9.2, 24.0, 1.4], [64, 58]],
            ["frost_club_head", "weapon", [-15.0, -6.0, -4.5], [-7.4, 9.0, 4.5], [88, 58]],
            ["left_leg", "legs", [0.8, 0, -3.2], [6.0, 17, 3.2], [0, 88]],
            ["right_leg", "legs", [-6.0, 0, -3.2], [-0.8, 17, 3.2], [24, 88]],
            ["left_boot", "legs", [0.2, 14.0, -5.4], [6.7, 18.0, 3.8], [50, 88]],
            ["right_boot", "legs", [-6.7, 14.0, -5.4], [-0.2, 18.0, 3.8], [80, 88]],
        ],
    },
    {
        "id": "draugr_warrior",
        "entity_id": "mythic_age:draugr_warrior",
        "display": "Draugr Warrior / 亡灵战士",
        "target_box": [0.85, 2.2],
        "visual_height": 2.25,
        "scale_note": "玩家体型亡灵战士，但必须有北欧头盔、圆盾和斧。",
        "primary": "#4f5a5b",
        "shadow": "#171f22",
        "accent": "#9aa7a8",
        "silhouette": {"front": "破损圆盾、斧头和角盔清楚。", "side": "前倾拖步，斧刃和盾厚度可读。", "back": "破披风和头盔后沿可见。"},
        "parts": [
            "helmet", "helmet_horns", "empty_eyes", "mouth", "torso", "rib_marks",
            "belt", "round_shield", "shield_cracks", "axe", "axe_blade",
            "arms", "bracers", "legs", "boots", "ragged_cape",
        ],
        "hierarchy": ["root/head", "root/body", "root/arms", "root/weapons", "root/legs"],
        "uv_plan": ["head_front: hollow eyes", "shield: cracked circle", "axe: iron blade", "cape: torn dark cloth"],
        "texture_detail": "灰尸皮、铁盔、破圆盾、铁斧和暗色披风；眼窝必须黑而亮边。",
        "animation": "拖步行走；攻击时斧头横劈；防御时盾臂保持在身体前侧。",
        "signature_features": ["北欧亡灵", "圆盾", "斧头", "角盔/铁盔"],
        "modeling_musts": ["shield 和 axe 独立且大轮廓可读", "眼洞、口线和肋骨痕高对比", "披风破片与身体分离"],
        "reject_if": ["像普通僵尸", "没有盾或斧", "没有北欧头盔"],
        "v2_design_freeze": [
            "正面必须读到北欧亡灵、角盔或铁盔、破圆盾和战斧，不得像普通僵尸。",
            "空眼、破披风、圆盾裂纹/中心扣和斧刃必须分区可读。",
            "拖步姿态和盾斧双侧轮廓要在侧面与 45 度视角成立。",
        ],
        "geometry": [
            ["torso", "body", [-4, -10, -2.2], [4, 2, 2.2], [0, 24]],
            ["shoulder_armor", "body", [-5.0, -10.8, -2.8], [5.0, -7.8, 2.8], [30, 24]],
            ["rib_marks", "body", [-2.8, -6.8, -2.8], [2.8, -2.0, -2.0], [58, 24]],
            ["belt_buckle", "body", [-1.2, -0.8, -3.0], [1.2, 1.2, -2.1], [74, 24]],
            ["ragged_cape", "body", [-4.6, -10, 2.2], [4.6, 4, 3.2], [84, 24]],
            ["cape_left_rip", "body", [-4.6, 2.4, 2.0], [-2.4, 7.0, 3.3], [0, 48]],
            ["cape_mid_rip", "body", [-1.1, 2.8, 2.0], [1.1, 6.4, 3.3], [12, 48]],
            ["cape_right_rip", "body", [2.4, 2.2, 2.0], [4.6, 7.2, 3.3], [24, 48]],
            ["head", "head", [-3.25, -8, -3.8], [3.25, 0, 3.8], [0, 0]],
            ["helmet", "head", [-3.6, -9.2, -4.2], [3.6, -6.6, 4.2], [36, 0]],
            ["helmet_crown", "head", [-3.1, -10.6, -3.6], [3.1, -8.8, 3.4], [62, 0]],
            ["helmet_nose_guard", "head", [-0.7, -8.6, -4.7], [0.7, -4.8, -3.8], [86, 0]],
            ["left_horn", "head", [3.0, -9.2, -2.5], [7.4, -7.8, -1.2], [94, 0]],
            ["right_horn", "head", [-7.4, -9.2, -2.5], [-3.0, -7.8, -1.2], [108, 0]],
            ["left_eye", "head", [-2.3, -5.2, -4.4], [-1.2, -4.0, -3.5], [68, 0]],
            ["right_eye", "head", [1.2, -5.2, -4.4], [2.3, -4.0, -3.5], [74, 0]],
            ["mouth_line", "head", [-1.6, -2.6, -4.5], [1.6, -1.9, -3.7], [80, 0]],
            ["left_arm", "arms", [3.8, -9, -1.7], [6.4, 3, 1.7], [36, 48]],
            ["right_arm", "arms", [-6.4, -9, -1.7], [-3.8, 3, 1.7], [50, 48]],
            ["left_bracer", "arms", [3.5, -0.5, -2.1], [6.7, 3.0, 2.1], [64, 48]],
            ["right_bracer", "arms", [-6.7, -0.5, -2.1], [-3.5, 3.0, 2.1], [78, 48]],
            ["round_shield", "weapons", [5.0, -7.8, -4.4], [7.0, 2.4, 4.4], [0, 64]],
            ["shield_inner_ring", "weapons", [4.8, -6.4, -3.1], [5.4, 1.0, 3.1], [36, 64]],
            ["shield_boss", "weapons", [4.5, -3.4, -1.0], [5.2, -1.6, 1.0], [58, 64]],
            ["shield_crack_a", "weapons", [4.3, -6.8, -3.8], [4.8, -2.6, -3.1], [66, 64]],
            ["shield_crack_b", "weapons", [4.2, -1.8, 0.5], [4.8, 2.0, 1.2], [72, 64]],
            ["axe_handle", "weapons", [-7.2, -9.0, -0.5], [-6.1, 7.0, 0.5], [82, 64]],
            ["axe_blade", "weapons", [-10.2, -10.0, -0.9], [-5.5, -5.4, 0.9], [90, 64]],
            ["axe_hook", "weapons", [-11.3, -8.4, -0.8], [-9.0, -5.6, 0.8], [110, 64]],
            ["axe_spike", "weapons", [-5.6, -9.3, -0.7], [-3.6, -5.8, 0.7], [120, 64]],
            ["left_leg", "legs", [0.6, 2, -1.5], [3.2, 12.4, 1.5], [0, 88]],
            ["right_leg", "legs", [-3.2, 2, -1.5], [-0.6, 12.4, 1.5], [14, 88]],
            ["left_boot", "legs", [0.2, 11.5, -2.3], [3.7, 14.5, 2.0], [28, 88]],
            ["right_boot", "legs", [-3.7, 11.5, -2.3], [-0.2, 14.5, 2.0], [44, 88]],
        ],
    },
    {
        "id": "valkyrie",
        "entity_id": "mythic_age:valkyrie",
        "display": "Valkyrie / 女武神",
        "target_box": [0.9, 2.35],
        "visual_height": 2.55,
        "scale_note": "精英飞行人形，翼、翼盔、长矛和金属甲必须明确；头部保持成人形比例，不能靠大头读身份。",
        "primary": "#d8d7cf",
        "shadow": "#5a6270",
        "accent": "#d7b65a",
        "silhouette": {"front": "双翼、翼盔和长矛竖线可读。", "side": "披风、翼厚度和矛尖明显。", "back": "两翼和披风分离。"},
        "parts": [
            "head", "face", "winged_helmet", "helmet_left_wing", "helmet_right_wing",
            "armor", "armor_plate", "cape", "left_wing", "right_wing", "wing_feather_strips",
            "left_arm", "spear_arm", "spear_shaft", "spear_tip", "left_leg", "right_leg",
        ],
        "hierarchy": ["root/head", "root/body", "root/wings", "root/weapon", "root/legs"],
        "uv_plan": ["helmet: gold wings", "armor: silver plates", "wings: pale feathers", "spear: bright tip"],
        "texture_detail": "银甲、金色翼盔、浅色羽翼、红暗披风和亮矛尖。",
        "animation": "待机轻拍翼；飞行翼大幅摆动；攻击时长矛前刺。",
        "signature_features": ["翼盔", "双翼", "长矛", "银金甲", "比例合理的头盔脸部"],
        "modeling_musts": ["wings 分段", "spear 独立", "helmet wings 独立或高对比", "基础头块宽度不得超过胸甲宽度约七成，识别由翼盔和矛完成"],
        "reject_if": ["像普通村民", "没有翼", "没有长矛"],
        "v2_design_freeze": [
            "正面必须读到翼盔、银金甲、双翼和长矛，不得像普通人形飞行单位，也不得用大头替代翼盔细节。",
            "翼羽必须分段；头盔小翼与背翼不能混淆，长矛要形成清晰竖线和刺击方向。",
            "飞行单位需要保持披风、翼根、腿部和矛臂结构可读。",
        ],
        "geometry": [
            ["body", "body", [-3.8, -10, -2], [3.8, 2, 2], [0, 24]],
            ["armor_plate", "body", [-4.2, -9.5, -2.6], [4.2, -3, -1.8], [32, 24]],
            ["head", "head", [-3.0, -7, -3.0], [3.0, 0, 3.0], [0, 0]],
            ["left_eye", "head", [-2.1, -4.2, -3.55], [-1.25, -3.1, -2.85], [100, 0]],
            ["right_eye", "head", [1.25, -4.2, -3.55], [2.1, -3.1, -2.85], [106, 0]],
            ["helmet", "head", [-3.35, -8.5, -3.35], [3.35, -5.8, 3.35], [30, 0]],
            ["helmet_left_wing", "head", [3.2, -8.0, -1.0], [7.2, -5.0, 1.0], [60, 0]],
            ["helmet_right_wing", "head", [-7.2, -8.0, -1.0], [-3.2, -5.0, 1.0], [72, 0]],
            ["cape", "body", [-3.6, -9.5, 2.0], [3.6, 5.0, 3.2], [100, 24]],
            ["left_arm", "arms", [3.4, -8.0, -1.5], [5.4, 2.5, 1.5], [100, 42]],
            ["spear_arm", "arms", [-5.4, -8.0, -1.5], [-3.4, 2.5, 1.5], [112, 42]],
            ["left_wing", "wings", [3.4, -11, 0], [18, 2, 1.5], [0, 48]],
            ["right_wing", "wings", [-18, -11, 0], [-3.4, 2, 1.5], [42, 48]],
            ["left_feather_strips", "wings", [6.0, -0.5, 1.0], [17.5, 2.0, 3.0], [60, 64]],
            ["right_feather_strips", "wings", [-17.5, -0.5, 1.0], [-6.0, 2.0, 3.0], [88, 64]],
            ["spear_shaft", "weapon", [-6, -12, -0.4], [-5, 8, 0.4], [84, 0]],
            ["spear_tip", "weapon", [-6.8, -16, -0.8], [-4.2, -12, 0.8], [92, 0]],
            ["left_leg", "legs", [0.5, 2, -1.4], [2.8, 14, 1.4], [0, 78]],
            ["right_leg", "legs", [-2.8, 2, -1.4], [-0.5, 14, 1.4], [14, 78]],
        ],
    },
    {
        "id": "fenrir_wolf",
        "entity_id": "mythic_age:fenrir_wolf",
        "display": "Fenrir Wolf / 芬里尔巨狼",
        "target_box": [2.1, 1.8],
        "visual_height": 1.95,
        "scale_note": "巨型黑狼，锁链、红眼和强壮口鼻必须读出；狼头要与长身厚颈匹配，不能做成卡通大头。",
        "primary": "#1d2028",
        "shadow": "#08090d",
        "accent": "#b02a2a",
        "silhouette": {"front": "比例合理的狼头、红眼、锁链项圈。", "side": "长背、厚颈、蓬尾、低伏捕食姿态。", "back": "背毛和尾巴清楚。"},
        "parts": [
            "wolf_body", "back_fur", "wolf_head", "left_eye", "right_eye", "upper_jaw",
            "lower_jaw", "fangs", "chain_collar", "front_left_leg", "front_right_leg",
            "back_left_leg", "back_right_leg", "tail", "tail_tip", "shoulder_hump",
        ],
        "hierarchy": ["root/body", "root/head", "root/legs", "root/tail", "root/chains"],
        "uv_plan": ["head_front: red eyes and jaws", "chain: iron links", "body_side: black fur highlights"],
        "texture_detail": "黑灰毛、红眼、铁链和白牙；口鼻前突但宽度受身体比例约束。",
        "animation": "潜行式行走；扑咬时头前冲；尾部低摆。",
        "signature_features": ["巨狼", "红眼", "锁链", "强壮下颚"],
        "modeling_musts": ["chain_collar 独立", "jaws 明确", "tail 蓬大", "wolf_head 宽度不得接近躯干宽度，细节靠眼、耳、上下颚、獠牙和锁链"],
        "reject_if": ["像普通狼", "没有锁链", "眼睛不可读"],
        "v2_design_freeze": [
            "正面和侧面必须读到巨狼体量、红眼、巨大上下颚和锁链项圈，不得像普通狼。",
            "嘴部上颚、下颚和獠牙必须分离，锁链项圈必须独立且高对比。",
            "低伏长身、背毛和蓬尾要形成芬里尔捕食轮廓。",
        ],
        "geometry": [
            ["wolf_body", "body", [-6, -6, -12], [6, 3, 10], [0, 30]],
            ["back_fur", "body", [-5, -8, -8], [5, -5.5, 7], [64, 30]],
            ["shoulder_hump", "body", [-4.8, -8.0, -6.5], [4.8, -5.5, 4.0], [0, 80]],
            ["wolf_head", "head", [-4.55, -6, -6.75], [4.55, 1.9, 1.2], [0, 0]],
            ["jaws", "head", [-2.9, -1.8, -11], [2.9, 1.8, -6.5], [42, 0]],
            ["upper_jaw", "head", [-2.9, -2.4, -11.0], [2.9, -0.5, -6.8], [94, 0]],
            ["lower_jaw", "head", [-2.5, 0.0, -10.5], [2.5, 1.8, -7.0], [88, 12]],
            ["left_fang", "head", [-2.0, 1.2, -10.8], [-1.0, 3.5, -9.8], [112, 12]],
            ["right_fang", "head", [1.0, 1.2, -10.8], [2.0, 3.5, -9.8], [120, 12]],
            ["left_eye", "head", [-2.95, -4.2, -7.3], [-1.85, -3.0, -6.4], [78, 0]],
            ["right_eye", "head", [1.85, -4.2, -7.3], [2.95, -3.0, -6.4], [84, 0]],
            ["chain_collar", "chains", [-5.8, -4, -6], [5.8, -2.2, 0], [86, 12]],
            ["tail", "tail", [-2, -2, 9], [2, 2, 22], [86, 30]],
            ["tail_tip", "tail", [-1.3, -1.3, 20.5], [1.3, 1.3, 23.5], [100, 30]],
            ["front_left_leg", "legs", [2, 0, -8], [5, 11, -5], [0, 62]],
            ["front_right_leg", "legs", [-5, 0, -8], [-2, 11, -5], [14, 62]],
            ["back_left_leg", "legs", [2, 0, 5], [5, 11, 8], [28, 62]],
            ["back_right_leg", "legs", [-5, 0, 5], [-2, 11, 8], [42, 62]],
        ],
    },
    {
        "id": "dwarf_berserker",
        "entity_id": "mythic_age:dwarf_berserker",
        "display": "Dwarf Berserker / 矮人狂战士",
        "target_box": [0.95, 1.45],
        "visual_height": 1.55,
        "scale_note": "矮而宽，胡须、圆盔和双斧必须突出。",
        "primary": "#7b4c2a",
        "shadow": "#26160d",
        "accent": "#c5a15c",
        "silhouette": {"front": "大胡须、宽肩、双斧。", "side": "短腿厚躯、斧头外扩。", "back": "背包/披肩和圆盔可见。"},
        "parts": [
            "head", "eyes", "nose", "mouth", "round_helmet", "helmet_crown",
            "helmet_rims", "helmet_nose_guard", "beard_mass", "beard_center",
            "braids", "torso", "shoulder_armor", "belt", "belt_buckle",
            "cloak_pack", "arms", "bracers", "left_axe_handle",
            "left_axe_blade", "left_axe_hook", "right_axe_handle",
            "right_axe_blade", "right_axe_hook", "short_legs", "boots",
        ],
        "hierarchy": ["root/head", "root/body", "root/arms", "root/weapons", "root/legs"],
        "uv_plan": [
            "head_front: dark eyes, nose and mouth under helmet brim",
            "beard: orange main beard, center lock and two braids",
            "helmet: brass dome, side rim and nose guard",
            "axes: dark handles with separate iron blades/hooks",
            "belt: dark leather strip and bright buckle",
        ],
        "texture_detail": "棕橙胡须、双辫、铜盔盔缘、皮甲、铁斧分刃和金色腰扣；脸部眼鼻口必须在胡须上方可读。",
        "animation": "短步快跑；攻击时双斧交替；狂暴时身体前倾。",
        "signature_features": ["矮宽比例", "大胡须", "双斧", "圆盔"],
        "modeling_musts": ["胡须主体、中心束和双辫必须独立", "双斧必须拆成斧柄、斧刃和斧钩", "短腿、宽肩和大靴子共同支撑矮宽比例"],
        "reject_if": ["像缩小玩家", "没有胡须", "没有双斧"],
        "v2_design_freeze": [
            "正面必须读到矮宽比例、大胡须、圆盔和双斧，不得像缩小玩家。",
            "胡须至少包含主体和双辫，腰带、盔缘和斧刃必须高对比分区。",
            "短腿宽身和外扩双斧要在侧面与 45 度视角保持可读。",
        ],
        "geometry": [
            ["body", "body", [-5.6, -8.2, -3.0], [5.6, 2.0, 3.0], [0, 24]],
            ["shoulder_armor", "body", [-6.4, -8.8, -3.6], [6.4, -5.7, 3.6], [38, 24]],
            ["chest_panel", "body", [-4.2, -5.6, -3.7], [4.2, -1.4, -2.8], [82, 24]],
            ["belt", "body", [-5.8, -1.2, -3.5], [5.8, 0.4, 3.5], [0, 42]],
            ["belt_buckle", "body", [-1.4, -1.7, -4.0], [1.4, 0.9, -3.0], [40, 42]],
            ["cloak_pack", "body", [-4.8, -7.4, 2.8], [4.8, 1.6, 4.4], [50, 42]],
            ["head", "head", [-4.0, -6.4, -3.8], [4.0, 0.0, 3.8], [0, 0]],
            ["left_eye", "head", [-2.65, -4.25, -4.45], [-1.55, -3.25, -3.55], [36, 0]],
            ["right_eye", "head", [1.55, -4.25, -4.45], [2.65, -3.25, -3.55], [42, 0]],
            ["nose", "head", [-0.55, -3.3, -4.55], [0.55, -1.85, -3.65], [48, 0]],
            ["mouth_line", "head", [-1.55, -1.65, -4.75], [1.55, -0.45, -3.55], [54, 0]],
            ["helmet", "head", [-4.3, -7.4, -4.15], [4.3, -4.9, 4.15], [64, 0]],
            ["helmet_crown", "head", [-3.4, -8.6, -3.35], [3.4, -6.9, 3.35], [0, 16]],
            ["helmet_nose_guard", "head", [-0.75, -7.6, -4.75], [0.75, -3.7, -3.85], [32, 16]],
            ["left_rim", "head", [3.65, -6.4, -2.1], [7.85, -5.1, -0.9], [40, 16]],
            ["right_rim", "head", [-7.85, -6.4, -2.1], [-3.65, -5.1, -0.9], [56, 16]],
            ["beard", "head", [-3.7, -2.1, -4.6], [3.7, 4.3, -2.9], [72, 16]],
            ["beard_center", "head", [-1.2, 1.5, -5.1], [1.2, 6.6, -3.6], [94, 16]],
            ["left_braid", "head", [2.1, 1.2, -5.2], [3.8, 7.3, -3.8], [106, 16]],
            ["right_braid", "head", [-3.8, 1.2, -5.2], [-2.1, 7.3, -3.8], [116, 16]],
            ["left_arm", "arms", [4.9, -7.4, -1.8], [7.7, 2.2, 1.8], [0, 54]],
            ["right_arm", "arms", [-7.7, -7.4, -1.8], [-4.9, 2.2, 1.8], [16, 54]],
            ["left_bracer", "arms", [4.8, -0.2, -2.2], [8.0, 2.8, 2.2], [32, 54]],
            ["right_bracer", "arms", [-8.0, -0.2, -2.2], [-4.8, 2.8, 2.2], [52, 54]],
            ["left_axe_handle", "weapons", [7.1, -10.0, -0.55], [8.0, 5.0, 0.55], [72, 54]],
            ["left_axe_blade", "weapons", [7.5, -10.2, -1.0], [11.4, -5.8, 1.0], [80, 54]],
            ["left_axe_hook", "weapons", [10.5, -8.8, -0.9], [12.2, -6.2, 0.9], [94, 54]],
            ["left_axe_counterweight", "weapons", [5.0, -9.6, -0.8], [7.2, -6.0, 0.8], [104, 54]],
            ["right_axe_handle", "weapons", [-8.0, -10.0, -0.55], [-7.1, 5.0, 0.55], [0, 72]],
            ["right_axe_blade", "weapons", [-11.4, -10.2, -1.0], [-7.5, -5.8, 1.0], [8, 72]],
            ["right_axe_hook", "weapons", [-12.2, -8.8, -0.9], [-10.5, -6.2, 0.9], [22, 72]],
            ["right_axe_counterweight", "weapons", [-7.2, -9.6, -0.8], [-5.0, -6.0, 0.8], [32, 72]],
            ["left_leg", "legs", [0.7, 2.0, -1.5], [3.5, 8.6, 1.5], [44, 72]],
            ["right_leg", "legs", [-3.5, 2.0, -1.5], [-0.7, 8.6, 1.5], [58, 72]],
            ["left_boot", "legs", [0.3, 7.6, -2.5], [3.9, 10.1, 2.0], [72, 72]],
            ["right_boot", "legs", [-3.9, 7.6, -2.5], [-0.3, 10.1, 2.0], [92, 72]],
        ],
    },
]


NORDIC_1_0_DETAIL_REFINEMENTS: dict[str, dict[str, list[Any]]] = {
    "frost_giant": {
        "parts": [
            "nose_bridge", "mouth_line", "left_brow_ice", "right_brow_ice",
            "left_beard_strand", "center_beard_strand", "right_beard_strand",
            "left_shoulder_ice", "right_shoulder_ice", "left_bracer_ice",
            "right_bracer_ice", "club_crack_a", "club_crack_b", "club_ice_cap",
            "left_knee_ice", "right_knee_ice", "left_boot_spikes", "right_boot_spikes",
            "back_left_spike", "back_right_spike",
        ],
        "geometry": [
            ["nose_bridge", "head", [-0.8, -5.8, -6.6], [0.8, -2.8, -5.4], [58, 12]],
            ["mouth_line", "head", [-2.6, -2.2, -6.5], [2.6, -1.2, -5.5], [70, 12]],
            ["left_brow_ice", "head", [-4.2, -8.4, -6.4], [-1.0, -6.6, -5.3], [82, 12]],
            ["right_brow_ice", "head", [1.0, -8.4, -6.4], [4.2, -6.6, -5.3], [96, 12]],
            ["left_beard_strand", "head", [-4.2, -2.6, -6.5], [-2.2, 6.5, -5.1], [78, 0]],
            ["center_beard_strand", "head", [-1.2, -2.2, -6.8], [1.2, 7.5, -5.2], [100, 0]],
            ["right_beard_strand", "head", [2.2, -2.6, -6.5], [4.2, 6.5, -5.1], [110, 0]],
            ["left_shoulder_ice", "body", [3.6, -20.0, -5.5], [8.8, -16.0, 5.5], [0, 108]],
            ["right_shoulder_ice", "body", [-8.8, -20.0, -5.5], [-3.6, -16.0, 5.5], [26, 108]],
            ["left_bracer_ice", "arms", [1.2, 9.0, -4.0], [9.3, 13.0, 4.0], [52, 108]],
            ["right_bracer_ice", "arms", [-9.3, 9.0, -4.0], [-1.2, 13.0, 4.0], [80, 108]],
            ["club_crack_a", "weapon", [-15.4, -3.2, -4.9], [-7.0, -1.8, -3.8], [100, 58]],
            ["club_crack_b", "weapon", [-15.1, 3.2, 3.8], [-7.4, 4.6, 5.0], [100, 64]],
            ["club_ice_cap", "weapon", [-13.8, -10.8, -3.8], [-8.0, -6.2, 3.8], [96, 72]],
            ["left_knee_ice", "legs", [0.4, 7.0, -3.8], [6.3, 10.8, -2.6], [0, 120]],
            ["right_knee_ice", "legs", [-6.3, 7.0, -3.8], [-0.4, 10.8, -2.6], [20, 120]],
            ["left_boot_spikes", "legs", [0.0, 16.0, -6.2], [7.0, 18.8, -4.8], [40, 120]],
            ["right_boot_spikes", "legs", [-7.0, 16.0, -6.2], [0.0, 18.8, -4.8], [62, 120]],
            ["back_left_spike", "spikes", [-5.0, -16.8, 3.9], [-3.0, -7.0, 6.4], [114, 0]],
            ["back_right_spike", "spikes", [3.0, -16.8, 3.9], [5.0, -7.0, 6.4], [114, 18]],
        ],
    },
    "valkyrie": {
        "parts": [
            "mouth_line", "helmet_crest", "left_helmet_feather_tip", "right_helmet_feather_tip",
            "left_wing_primary_a", "left_wing_primary_b", "left_wing_primary_c",
            "right_wing_primary_a", "right_wing_primary_b", "right_wing_primary_c",
            "left_bracer", "spear_bracer", "spear_blade", "spear_counterweight",
            "left_boot", "right_boot", "cape_left_rip", "cape_right_rip",
        ],
        "geometry": [
            ["mouth_line", "head", [-1.4, -1.4, -4.0], [1.4, -0.7, -3.2], [42, 0]],
            ["helmet_crest", "head", [-1.0, -9.2, -4.6], [1.0, -6.6, -3.4], [34, 16]],
            ["left_helmet_feather_tip", "head", [6.2, -10.2, -1.2], [9.0, -7.0, 1.2], [62, 16]],
            ["right_helmet_feather_tip", "head", [-9.0, -10.2, -1.2], [-6.2, -7.0, 1.2], [62, 22]],
            ["left_wing_primary_a", "wings", [5.6, 0.3, 4.0], [7.4, 1.5, 11.5], [0, 70]],
            ["left_wing_primary_b", "wings", [8.9, 0.3, 4.5], [10.7, 1.5, 12.3], [10, 70]],
            ["left_wing_primary_c", "wings", [12.2, 0.3, 5.0], [14.0, 1.5, 13.0], [20, 70]],
            ["right_wing_primary_a", "wings", [-7.4, 0.3, 4.0], [-5.6, 1.5, 11.5], [38, 70]],
            ["right_wing_primary_b", "wings", [-10.7, 0.3, 4.5], [-8.9, 1.5, 12.3], [48, 70]],
            ["right_wing_primary_c", "wings", [-14.0, 0.3, 5.0], [-12.2, 1.5, 13.0], [58, 70]],
            ["left_bracer", "arms", [3.2, 0.4, -1.8], [5.8, 2.6, 1.8], [12, 90]],
            ["spear_bracer", "arms", [-5.8, 0.4, -1.8], [-3.2, 2.6, 1.8], [38, 90]],
            ["spear_blade", "weapon", [-7.3, -16.5, -0.9], [-4.7, -11.6, 0.9], [62, 88]],
            ["spear_counterweight", "weapon", [-6.6, 7.8, -0.8], [-5.1, 10.6, 0.8], [82, 88]],
            ["left_boot", "legs", [0.2, 8.6, -2.0], [3.2, 11.0, 1.8], [12, 110]],
            ["right_boot", "legs", [-3.2, 8.6, -2.0], [-0.2, 11.0, 1.8], [40, 110]],
            ["cape_left_rip", "body", [-3.8, 4.0, 2.4], [-1.8, 7.2, 3.4], [112, 36]],
            ["cape_right_rip", "body", [1.8, 4.0, 2.4], [3.8, 7.2, 3.4], [122, 36]],
        ],
    },
    "fenrir_wolf": {
        "parts": [
            "left_ear", "right_ear", "left_brow", "right_brow", "left_lower_fang",
            "right_lower_fang", "chain_link_left", "chain_link_right", "chain_link_center",
            "front_left_paw", "front_right_paw", "back_left_paw", "back_right_paw",
            "front_left_claws", "front_right_claws", "back_left_claws", "back_right_claws",
            "back_fur_a", "back_fur_b", "back_fur_c",
        ],
        "geometry": [
            ["left_ear", "head", [-5.8, -8.4, -2.2], [-3.7, -4.8, 0.7], [56, 0]],
            ["right_ear", "head", [3.7, -8.4, -2.2], [5.8, -4.8, 0.7], [68, 0]],
            ["left_brow", "head", [-3.8, -4.8, -8.0], [-2.2, -3.7, -7.1], [44, 0]],
            ["right_brow", "head", [2.2, -4.8, -8.0], [3.8, -3.7, -7.1], [50, 0]],
            ["left_lower_fang", "head", [-2.4, 0.2, -12.3], [-1.5, 2.4, -11.3], [108, 12]],
            ["right_lower_fang", "head", [1.5, 0.2, -12.3], [2.4, 2.4, -11.3], [114, 12]],
            ["chain_link_left", "chains", [-6.8, -4.2, -6.5], [-4.8, -2.0, -3.5], [32, 22]],
            ["chain_link_right", "chains", [4.8, -4.2, -6.5], [6.8, -2.0, -3.5], [42, 22]],
            ["chain_link_center", "chains", [-1.4, -4.5, -7.0], [1.4, -2.0, -5.2], [76, 22]],
            ["front_left_paw", "legs", [1.7, 9.5, -9.2], [5.4, 11.5, -5.1], [16, 82]],
            ["front_right_paw", "legs", [-5.4, 9.5, -9.2], [-1.7, 11.5, -5.1], [52, 82]],
            ["back_left_paw", "legs", [1.6, 9.4, 4.3], [5.6, 11.4, 8.4], [90, 82]],
            ["back_right_paw", "legs", [-5.6, 9.4, 4.3], [-1.6, 11.4, 8.4], [90, 100]],
            ["front_left_claws", "legs", [1.6, 10.9, -10.0], [5.5, 12.0, -8.8], [0, 120]],
            ["front_right_claws", "legs", [-5.5, 10.9, -10.0], [-1.6, 12.0, -8.8], [16, 120]],
            ["back_left_claws", "legs", [1.5, 10.8, 3.6], [5.7, 11.9, 4.8], [32, 120]],
            ["back_right_claws", "legs", [-5.7, 10.8, 3.6], [-1.5, 11.9, 4.8], [48, 120]],
            ["back_fur_a", "body", [-1.9, -9.4, -10.8], [1.9, -6.1, -5.5], [34, 58]],
            ["back_fur_b", "body", [-1.7, -9.8, -5.8], [1.7, -6.2, -0.2], [52, 58]],
            ["back_fur_c", "body", [-1.5, -9.1, -0.3], [1.5, -6.0, 5.3], [70, 58]],
        ],
    },
}


for creature in NORDIC_CREATURES:
    refinement = NORDIC_1_0_DETAIL_REFINEMENTS.get(creature["id"], {})
    existing_parts = set(creature["parts"])
    for part in refinement.get("parts", []):
        if part not in existing_parts:
            creature["parts"].append(part)
            existing_parts.add(part)
    creature["geometry"].extend(refinement.get("geometry", []))


BASE_DECORATE_TEXTURE_REGION = base.decorate_texture_region


def decorate_nordic_texture_region(draw: Any, creature: dict[str, Any], row: list[Any], rect: tuple[int, int, int, int]) -> None:
    BASE_DECORATE_TEXTURE_REGION(draw, creature, row, rect)

    name = row[0]
    shadow = base.hex_to_rgb(creature["shadow"])
    accent = base.hex_to_rgb(creature["accent"])
    primary = base.hex_to_rgb(creature["primary"])
    x1, y1, x2, y2 = rect
    width = max(1, x2 - x1)
    height = max(1, y2 - y1)

    if any(token in name for token in ("ice", "spike", "rune", "frost")):
        draw.rectangle(rect, fill=base.shade_rgb(accent, 10) + (255,), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(236, 250, 255, 255), width=1)
        return
    if "club_crack" in name:
        draw.rectangle(rect, fill=(88, 100, 104, 255), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(224, 242, 250, 255), width=1)
        return
    if "feather" in name or "wing_primary" in name or "helmet_feather" in name:
        draw.rectangle(rect, fill=(224, 225, 217, 255), outline=shadow + (255,), width=1)
        for offset in range(-height, width + height, max(3, min(width, height) // 2)):
            draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(156, 162, 170, 255), width=1)
        return
    if "chain" in name:
        draw.rectangle(rect, fill=(74, 78, 82, 255), outline=shadow + (255,), width=1)
        draw.ellipse((x1 + 1, y1 + 1, x2 - 2, y2 - 2), outline=(164, 170, 170, 255), width=1)
        return
    if "claw" in name or "fang" in name:
        draw.polygon([(x1, y2), ((x1 + x2) // 2, y1), (x2, y2)], fill=(226, 230, 214, 255), outline=shadow + (255,))
        return
    if "paw" in name or "boot" in name or "bracer" in name:
        draw.rectangle(rect, fill=base.shade_rgb(primary, -14) + (255,), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=accent + (255,), width=1)
        return
    if "mouth" in name or "nose" in name or "brow" in name:
        draw.rectangle(rect, fill=base.shade_rgb(primary, 12) + (255,), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y1 + 1, x2 - 1, y1 + 1), fill=base.shade_rgb(accent, 25) + (255,), width=1)
        draw.line((x1 + 2, y2 - 2, x2 - 2, y2 - 2), fill=shadow + (255,), width=1)
        return

    if creature["id"] != "dwarf_berserker":
        return

    if "eye" in name:
        draw.rectangle(rect, fill=(24, 18, 12, 255), outline=shadow + (255,), width=1)
        return
    if "mouth" in name or "nose" in name:
        draw.rectangle(rect, fill=(43, 23, 14, 255), outline=shadow + (255,), width=1)
        return
    if "beard" in name or "braid" in name:
        draw.rectangle(rect, fill=(166, 82, 32, 255), outline=shadow + (255,), width=1)
        for offset in range(0, width + height, max(3, width // 4)):
            draw.line((x1 + offset, y1, x1 + offset - height, y2), fill=(94, 47, 20, 255), width=1)
        return
    if "helmet" in name or "rim" in name:
        draw.rectangle(rect, fill=(151, 113, 54, 255), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(214, 170, 82, 255), width=1)
        return
    if "axe_blade" in name or "axe_hook" in name or "axe_counterweight" in name:
        draw.rectangle(rect, fill=(120, 123, 118, 255), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(196, 199, 190, 255), width=1)
        return
    if "axe_handle" in name:
        draw.rectangle(rect, fill=(62, 36, 20, 255), outline=shadow + (255,), width=1)
        return
    if "belt_buckle" in name:
        draw.rectangle(rect, fill=(205, 157, 68, 255), outline=shadow + (255,), width=1)
        draw.rectangle((x1 + max(1, width // 4), y1 + max(1, height // 4), x2 - max(1, width // 4), y2 - max(1, height // 4)), outline=(92, 58, 24, 255), width=1)
        return
    if "belt" in name or "bracer" in name or "boot" in name:
        draw.rectangle(rect, fill=(61, 34, 18, 255), outline=shadow + (255,), width=1)
        return
    if "chest_panel" in name or "shoulder_armor" in name:
        draw.rectangle(rect, fill=(105, 62, 31, 255), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(190, 128, 54, 255), width=1)


def configure_base() -> None:
    base.DESIGN_DIR = DESIGN_DIR
    base.CONCEPT_DIR = CONCEPT_DIR
    base.BLOCKBENCH_DIR = BLOCKBENCH_DIR
    base.PREVIEW_DIR = PREVIEW_DIR
    base.MANIFEST_PATH = MANIFEST_PATH
    base.REPORT_PATH = REPORT_PATH
    base.TEXTURE_FIT_REPORT_PATH = TEXTURE_FIT_REPORT_PATH
    base.CONTACT_SHEET_PATH = CONTACT_SHEET_PATH
    base.CONCEPT_CONTACT_SHEET_PATH = CONCEPT_CONTACT_SHEET_PATH
    base.CREATURES = NORDIC_CREATURES
    base.decorate_texture_region = decorate_nordic_texture_region


def audit() -> int:
    configure_base()
    failures: list[str] = []
    if not MANIFEST_PATH.exists():
        failures.append(f"Missing manifest: {MANIFEST_PATH}")
    if not TEXTURE_FIT_REPORT_PATH.exists():
        failures.append(f"Missing texture fit report: {TEXTURE_FIT_REPORT_PATH}")
        texture_fit: dict[str, Any] = {"creatures": []}
    else:
        texture_fit = json.loads(TEXTURE_FIT_REPORT_PATH.read_text(encoding="utf-8"))
        if not texture_fit.get("ok", False):
            failures.append(f"Texture fit report has failures: {TEXTURE_FIT_REPORT_PATH}")
    fit_by_id = {item.get("id"): item for item in texture_fit.get("creatures", [])}
    for creature in NORDIC_CREATURES:
        cid = creature["id"]
        required = [
            DESIGN_DIR / f"{cid}.md",
            CONCEPT_DIR / f"{cid}_concept.png",
            BLOCKBENCH_DIR / f"{cid}.bbmodel",
            PREVIEW_DIR / cid / "uv_template.png",
            PREVIEW_DIR / cid / "texture_fit.png",
            base.texture_path(cid),
        ]
        required.extend(PREVIEW_DIR / cid / "blockbench" / f"{view}.png" for view in base.VIEWS)
        for path in required:
            if not path.exists():
                failures.append(f"{cid}: missing artifact {path}")
        design_path = DESIGN_DIR / f"{cid}.md"
        if design_path.exists():
            text = design_path.read_text(encoding="utf-8")
            if creature.get("v2_design_freeze") and "## V2 造型冻结" not in text:
                failures.append(f"{cid}: design doc missing V2 造型冻结 section")
            for marker in creature.get("v2_design_freeze", []):
                if marker not in text:
                    failures.append(f"{cid}: design doc missing V2 marker {marker}")
        fit = fit_by_id.get(cid)
        if not fit or not fit.get("ok", False):
            failures.append(f"{cid}: texture fit failed or missing")
        bbmodel_path = BLOCKBENCH_DIR / f"{cid}.bbmodel"
        if bbmodel_path.exists():
            data = json.loads(bbmodel_path.read_text(encoding="utf-8"))
            elements = data.get("elements", [])
            if len(elements) < len(creature["parts"]):
                failures.append(f"{cid}: bbmodel element count too low for required parts")
            for element in elements:
                for face_data in element.get("faces", {}).values():
                    uv = face_data.get("uv")
                    if not uv or len(uv) != 4 or any(value < 0 or value > 128 for value in uv):
                        failures.append(f"{cid}: out-of-bounds UV in {bbmodel_path}")
                        break
        concept_path = CONCEPT_DIR / f"{cid}_concept.png"
        if concept_path.exists():
            with Image.open(concept_path) as image:
                if image.width < 900 or image.height < 420:
                    failures.append(f"{cid}: concept image too small for design review")
    if failures:
        print("Nordic Creature Visual Gate: FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Nordic Creature Visual Gate: PASS")
    print(f"manifest: {MANIFEST_PATH}")
    print(f"design_dir: {DESIGN_DIR}")
    print(f"blockbench_dir: {BLOCKBENCH_DIR}")
    print(f"preview_dir: {PREVIEW_DIR}")
    print(f"texture_fit_report: {TEXTURE_FIT_REPORT_PATH}")
    print(f"contact_sheet: {CONTACT_SHEET_PATH}")
    return 0


def generate_all() -> int:
    configure_base()
    base.generate_all()
    return audit()


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "command",
        choices=(
            "all",
            "design",
            "designs",
            "concepts",
            "manifest",
            "models",
            "bbmodels",
            "uv",
            "textures",
            "texture-fit",
            "previews",
            "contact-sheet",
            "concept-contact-sheet",
            "report",
            "audit",
        ),
    )
    args = parser.parse_args(argv)
    configure_base()
    if args.command == "all":
        return generate_all()
    if args.command in {"design", "designs"}:
        base.write_design_docs()
        base.write_manifest()
        return 0
    if args.command == "concepts":
        base.draw_concept_art()
        base.draw_concept_contact_sheet()
        base.write_manifest()
        return 0
    if args.command == "manifest":
        base.write_manifest()
        return 0
    if args.command in {"models", "bbmodels"}:
        base.write_bbmodels()
        base.write_manifest()
        return 0
    if args.command == "uv":
        base.draw_uv_templates()
        base.write_manifest()
        return 0
    if args.command == "textures":
        base.draw_runtime_textures()
        base.write_manifest()
        return 0
    if args.command == "texture-fit":
        base.write_texture_fit_report()
        base.write_manifest()
        return 0
    if args.command == "previews":
        base.draw_previews()
        base.write_manifest()
        return 0
    if args.command == "contact-sheet":
        base.draw_contact_sheet()
        base.write_manifest()
        return 0
    if args.command == "concept-contact-sheet":
        base.draw_concept_contact_sheet()
        base.write_manifest()
        return 0
    if args.command == "report":
        base.write_visual_report()
        base.write_manifest()
        return 0
    if args.command == "audit":
        return audit()
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
