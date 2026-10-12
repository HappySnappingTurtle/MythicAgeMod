#!/usr/bin/env python3
"""Design-first visual workflow for Egyptian 1.0 creature candidates.

This pipeline intentionally mirrors the Greek Blockbench gate while keeping its
own output directories. It validates Egyptian creature design packages,
Blockbench source, UV templates, runtime textures, and three-view previews.
Minecraft screenshots are intentionally out of scope for this phase.
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

DESIGN_DIR = ROOT / "docs/美术资源/egyptian_creatures/design"
CONCEPT_DIR = ROOT / "docs/美术资源/egyptian_creatures/concepts"
BLOCKBENCH_DIR = ROOT / "docs/美术资源/blockbench/egyptian_creatures"
PREVIEW_DIR = ROOT / "docs/preview_assets/egyptian_creatures"
MANIFEST_PATH = ROOT / "docs/美术资源/egyptian_creatures/visual_manifest.json"
REPORT_PATH = PREVIEW_DIR / "visual_report.json"
TEXTURE_FIT_REPORT_PATH = PREVIEW_DIR / "texture_fit_report.json"
CONTACT_SHEET_PATH = PREVIEW_DIR / "blockbench_validation_contact_sheet.png"
CONCEPT_CONTACT_SHEET_PATH = PREVIEW_DIR / "concept_design_contact_sheet.png"


EGYPTIAN_CREATURES: list[dict[str, Any]] = [
    {
        "id": "anubis_warrior",
        "entity_id": "mythic_age:anubis_warrior",
        "display": "Anubis Warrior / 阿努比斯战士",
        "target_box": [0.9, 2.3],
        "visual_height": 2.45,
        "scale_note": "比玩家略高，必须是胡狼头人形守卫；头部保持人形比例，识别来自长吻、长耳、眼睛和金饰，不靠大头。",
        "primary": "#3b2b22",
        "shadow": "#17110d",
        "accent": "#d8b45c",
        "silhouette": {
            "front": "胡狼长耳、长吻、金色胸饰和手持镰剑/权杖必须第一眼可见。",
            "side": "长吻前突，后脑耳朵高耸，武器形成清楚侧影。",
            "back": "背部披肩、耳朵和腿部护甲仍能读出埃及守卫身份。",
        },
        "parts": ["jackal_head", "long_snout", "ears", "eyes", "torso", "gold_collar", "arms", "khopesh", "legs", "sandals"],
        "hierarchy": ["root/head", "root/body", "root/arms", "root/weapon", "root/legs"],
        "uv_plan": ["head_front: gold eyes and long snout", "collar: gold bands", "weapon: bronze khopesh", "legs: linen skirt and sandals"],
        "texture_detail": "深胡狼头、砂色亚麻裙、金色胸饰和护腕，眼睛必须高对比，武器为青铜/金色弯刃。",
        "animation": "待机守卫姿态；行走长臂摆动；攻击时右手镰剑横扫；审判技能时头部稳定注视目标。",
        "signature_features": [
            "胡狼头、长吻、长耳、人形身体和埃及金饰必须同时可读，头块宽度不能压过肩胸比例",
            "右手镰剑/权杖必须作为独立武器轮廓",
            "眼睛不能被深色头部淹没，胸饰不能与身体糊成一块",
            "整体是神殿守卫，不是普通亡灵或村民",
        ],
        "modeling_musts": [
            "jackal_head、snout、ears、gold_collar、khopesh 必须独立几何",
            "头部高度和耳朵必须高于普通玩家头部轮廓，但基础头块宽度必须小于胸宽约四分之三",
            "武器至少达到身体高度的 45%，侧面可见",
        ],
        "reject_if": ["看起来像普通僵尸", "没有长吻/长耳", "武器不可读或没有埃及金饰"],
        "v2_design_freeze": [
            "正面第一识别必须是胡狼头、长吻、长耳、金色胸饰和右手镰剑；不能用大头替代吻部和耳朵细节。",
            "长吻、双耳、眼睛和嘴线必须分区建模/贴图；深色头部上眼睛必须高对比。",
            "镰剑/权杖必须形成独立武器轮廓，侧面和 45 度不能被手臂遮没。",
        ],
        "geometry": [
            ["torso", "body", [-4, -10, -2.2], [4, 2, 2.2], [0, 24]],
            ["gold_collar", "body", [-4.7, -10.5, -3.0], [4.7, -7.2, -2.2], [28, 24]],
            ["linen_skirt", "body", [-4.2, -1.0, -2.6], [4.2, 5.5, 2.6], [54, 24]],
            ["jackal_head", "head", [-3.25, -8.2, -3.05], [3.25, -0.2, 3.05], [0, 0]],
            ["long_snout", "head", [-2.0, -4.5, -8.0], [2.0, -1.0, -3.0], [34, 0]],
            ["left_ear", "head", [1.5, -12.0, -1.45], [3.05, -7.2, 0.0], [58, 0]],
            ["right_ear", "head", [-3.05, -12.0, -1.45], [-1.5, -7.2, 0.0], [66, 0]],
            ["left_eye", "head", [-2.95, -5.8, -3.45], [-2.0, -4.8, -2.7], [76, 0]],
            ["right_eye", "head", [2.0, -5.8, -3.45], [2.95, -4.8, -2.7], [82, 0]],
            ["left_arm", "arms", [4, -9, -1.7], [6.2, 3.5, 1.7], [84, 0]],
            ["right_arm", "arms", [-6.2, -9, -1.7], [-4, 3.5, 1.7], [96, 0]],
            ["khopesh_handle", "weapon", [-7.2, -2.0, -0.6], [-6.0, 9.0, 0.6], [108, 0]],
            ["khopesh_blade", "weapon", [-9.8, -7.5, -0.8], [-5.6, -1.0, 0.8], [108, 16]],
            ["left_leg", "legs", [0.7, 5, -1.5], [3.4, 16, 1.5], [0, 48]],
            ["right_leg", "legs", [-3.4, 5, -1.5], [-0.7, 16, 1.5], [14, 48]],
            ["left_sandal", "legs", [0.2, 15.0, -2.5], [3.8, 17.0, 2.2], [28, 48]],
            ["right_sandal", "legs", [-3.8, 15.0, -2.5], [-0.2, 17.0, 2.2], [46, 48]],
        ],
    },
    {
        "id": "scarab_guardian",
        "entity_id": "mythic_age:scarab_guardian",
        "display": "Scarab Guardian / 圣甲虫守卫",
        "target_box": [1.8, 1.2],
        "visual_height": 1.35,
        "scale_note": "低伏宽体，比蜘蛛更甲壳化，背甲和角必须醒目。",
        "primary": "#1f4a3f",
        "shadow": "#0b1715",
        "accent": "#d2a94b",
        "silhouette": {"front": "宽甲壳、前角和六足展开。", "side": "低伏甲虫身、前角前伸。", "back": "背甲分片和后足可见。"},
        "parts": [
            "shell", "gold_carapace", "carapace_ridge", "head", "left_mandible", "right_mandible",
            "horn", "horn_tip", "left_eye", "right_eye", "front_left_leg", "front_right_leg",
            "mid_left_leg", "mid_right_leg", "back_left_leg", "back_right_leg",
        ],
        "hierarchy": ["root/body", "root/head", "root/legs"],
        "uv_plan": ["shell: emerald panels", "gold_carapace: scarab sigil", "horn: gold tip", "eyes: dark beads"],
        "texture_detail": "深绿甲壳、金色背甲符号、黑色小眼，腿部用深色分节。",
        "animation": "六足交替爬行；攻击时前角突刺；防御时背甲稳定不变形。",
        "signature_features": ["圣甲虫轮廓", "金色背甲/太阳符号", "六足和前角分离"],
        "modeling_musts": ["shell 与 gold_carapace 分层", "六足必须外扩", "horn 必须正侧面可见"],
        "reject_if": ["像普通蜘蛛", "没有背甲符号", "腿和身体糊成一块"],
        "v2_design_freeze": [
            "正面必须先读到低伏宽甲壳、前角、六足外扩和金色圣甲虫背甲。",
            "背甲必须有分层金色甲片/太阳符号，不能只是绿色椭圆块。",
            "六足与身体必须分离，前角侧面要明显前伸，不能读成普通蜘蛛。",
        ],
        "geometry": [
            ["shell", "body", [-7, -5, -9], [7, 2, 8], [0, 26]],
            ["gold_carapace", "body", [-5.5, -6.3, -6], [5.5, -4.6, 6], [60, 26]],
            ["head", "head", [-4.5, -4, -13], [4.5, 2, -8], [0, 0]],
            ["horn", "head", [-1.2, -3.0, -17], [1.2, -1.0, -12], [34, 0]],
            ["left_eye", "head", [-3.6, -2.6, -13.6], [-2.5, -1.6, -12.8], [54, 0]],
            ["right_eye", "head", [2.5, -2.6, -13.6], [3.6, -1.6, -12.8], [60, 0]],
            ["front_left_leg", "legs", [5, 0, -8], [11, 2, -6], [0, 54]],
            ["mid_left_leg", "legs", [6, 0, -1], [12, 2, 1], [26, 54]],
            ["back_left_leg", "legs", [5, 0, 6], [11, 2, 8], [52, 54]],
            ["front_right_leg", "legs", [-11, 0, -8], [-5, 2, -6], [0, 64]],
            ["mid_right_leg", "legs", [-12, 0, -1], [-6, 2, 1], [26, 64]],
            ["back_right_leg", "legs", [-11, 0, 6], [-5, 2, 8], [52, 64]],
        ],
    },
    {
        "id": "mummy_soldier",
        "entity_id": "mythic_age:mummy_soldier",
        "display": "Mummy Soldier / 木乃伊士兵",
        "target_box": [0.8, 2.1],
        "visual_height": 2.15,
        "scale_note": "玩家体型敌人，但绷带、空眼和短矛必须可读。",
        "primary": "#b8a77e",
        "shadow": "#4a3f2b",
        "accent": "#d7caa1",
        "silhouette": {"front": "缠布头脸、空眼、短矛。", "side": "前倾慢行，武器侧影明显。", "back": "绷带层次和披布可见。"},
        "parts": [
            "wrapped_head", "head_bandage_upper", "head_bandage_lower", "left_eye", "right_eye",
            "torso", "bandage_chest", "bandage_mid", "bandage_low", "left_arm", "right_arm",
            "arm_bandages", "spear_shaft", "spear_tip", "left_leg", "right_leg", "leg_bandages",
        ],
        "hierarchy": ["root/head", "root/body", "root/arms", "root/weapon", "root/legs"],
        "uv_plan": ["head_front: dark eye holes", "bandages: diagonal linen strips", "spear: bronze tip"],
        "texture_detail": "砂色绷带、深色眼洞、青铜短矛和褐色腐朽阴影。",
        "animation": "待机摇晃；行走拖步；攻击时短矛前刺。",
        "signature_features": ["绷带层次", "黑眼洞", "短矛士兵身份"],
        "modeling_musts": ["bandages 必须多区块", "spear 独立", "眼洞高对比"],
        "reject_if": ["像普通僵尸", "没有绷带", "没有武器"],
        "v2_design_freeze": [
            "正面必须读到深色空眼、层层绷带和短矛士兵身份，不得像普通僵尸。",
            "头、胸、手臂、腿部的绷带必须有多条独立带状区，不能只靠单一砂色贴图。",
            "短矛必须作为独立武器轮廓，攻击方向正侧面都要可读。",
        ],
        "geometry": [
            ["torso", "body", [-3.6, -10, -2.0], [3.6, 2, 2.0], [0, 22]],
            ["bandage_chest", "body", [-4.0, -8.5, -2.6], [4.0, -7.1, -1.8], [26, 22]],
            ["wrapped_head", "head", [-2.7, -7.2, -3.0], [2.7, 0, 3.0], [0, 0]],
            ["left_eye", "head", [-2.05, -4.7, -3.5], [-0.95, -3.7, -2.6], [40, 0]],
            ["right_eye", "head", [0.95, -4.7, -3.5], [2.05, -3.7, -2.6], [46, 0]],
            ["left_arm", "arms", [3.6, -9, -1.5], [5.6, 3, 1.5], [54, 0]],
            ["right_arm", "arms", [-5.6, -9, -1.5], [-3.6, 3, 1.5], [66, 0]],
            ["spear_shaft", "weapon", [-6.6, -9, -0.4], [-5.6, 7, 0.4], [78, 0]],
            ["spear_tip", "weapon", [-7.0, -13, -0.8], [-5.2, -9, 0.8], [86, 0]],
            ["left_leg", "legs", [0.5, 2, -1.4], [3.0, 14, 1.4], [0, 46]],
            ["right_leg", "legs", [-3.0, 2, -1.4], [-0.5, 14, 1.4], [14, 46]],
        ],
    },
    {
        "id": "sphinx_sentinel",
        "entity_id": "mythic_age:sphinx_sentinel",
        "display": "Sphinx Sentinel / 狮身人面守卫",
        "target_box": [2.2, 1.8],
        "visual_height": 1.95,
        "scale_note": "长狮身、法老头饰和人面必须同时可读。",
        "primary": "#c79a4a",
        "shadow": "#4d3218",
        "accent": "#315f7d",
        "silhouette": {"front": "人面头饰、宽胸和前爪。", "side": "狮身长、尾巴和头饰侧片明显。", "back": "狮背和尾端可读。"},
        "parts": [
            "lion_body", "chest", "human_head", "human_face", "pharaoh_headdress",
            "headdress_left_panel", "headdress_right_panel", "headdress_brow", "left_eye", "right_eye",
            "nose_bridge", "mouth_line",
            "front_left_leg", "front_right_leg", "back_left_leg", "back_right_leg", "front_paws",
            "back_paws", "tail", "tail_tuft",
        ],
        "hierarchy": ["root/body", "root/head", "root/legs", "root/tail"],
        "uv_plan": ["head_front: human eyes", "headdress: blue-gold stripes", "body_side: sandy lion coat"],
        "texture_detail": "砂金狮身、蓝金法老头饰、深色人面眼睛和厚前爪。",
        "animation": "低伏待机；扑击时前爪前压；谜语/守卫技能时头部稳定。",
        "signature_features": ["狮身", "人面", "法老头饰"],
        "modeling_musts": ["head 与 headdress 分离", "前爪厚重", "侧面长身明显"],
        "reject_if": ["像普通狮子", "没有人面/头饰", "身体太短"],
        "v2_design_freeze": [
            "三视角必须同时读到人面、法老头饰和长狮身；不能退化成普通狮子。",
            "头饰必须有蓝金侧片和额饰，眼睛/鼻口必须在正面高对比可读。",
            "侧面长身、厚前爪和尾巴必须支持狮身人面轮廓，身体不能过短。",
        ],
        "geometry": [
            ["lion_body", "body", [-6.5, -6, -12], [6.5, 3, 10], [0, 30]],
            ["chest", "body", [-5.5, -7, -12.5], [5.5, -2, -6], [60, 34]],
            ["human_head", "head", [-4, -6.5, -6], [4, 1.5, 1], [0, 0]],
            ["human_face", "head", [-3.0, -2.4, -6.6], [3.0, -0.4, -5.6], [34, 0]],
            ["pharaoh_headdress", "head", [-5.2, -8, -4], [5.2, 2.5, 2], [32, 0]],
            ["left_eye", "head", [-2.8, -3.8, -6.5], [-1.7, -2.8, -5.7], [92, 0]],
            ["right_eye", "head", [1.7, -3.8, -6.5], [2.8, -2.8, -5.7], [98, 0]],
            ["nose_bridge", "head", [-0.8, -3.4, -6.8], [0.8, -1.4, -5.9], [104, 0]],
            ["mouth_line", "head", [-1.8, -1.0, -6.9], [1.8, -0.4, -6.0], [112, 0]],
            ["front_left_leg", "legs", [1.5, 0, -9], [5.4, 11, -5], [0, 62]],
            ["front_right_leg", "legs", [-5.4, 0, -9], [-1.5, 11, -5], [18, 62]],
            ["back_left_leg", "legs", [1.8, 0, 5], [5.8, 10, 9], [36, 62]],
            ["back_right_leg", "legs", [-5.8, 0, 5], [-1.8, 10, 9], [54, 62]],
            ["tail", "tail", [-1, -1, 9], [1, 1, 20], [72, 62]],
        ],
    },
    {
        "id": "sun_falcon",
        "entity_id": "mythic_age:sun_falcon",
        "display": "Sun Falcon / 太阳隼",
        "target_box": [1.2, 1.3],
        "visual_height": 1.45,
        "scale_note": "中型飞行单位，隼头、展开翼和太阳圆盘必须明确。",
        "primary": "#8f4b22",
        "shadow": "#2d1a12",
        "accent": "#f2c14e",
        "silhouette": {"front": "双翼横展、头顶太阳盘。", "side": "尖喙、翼羽和尾羽明显。", "back": "翼羽分段和尾羽可读。"},
        "parts": [
            "falcon_body", "belly", "head", "beak", "left_eye", "right_eye", "sun_disk",
            "sun_disk_top_ray", "sun_disk_bottom_ray", "left_wing", "left_feathers",
            "right_wing", "right_feathers", "tail", "tail_tip",
        ],
        "hierarchy": ["root/body", "root/head", "root/wings", "root/tail"],
        "uv_plan": ["head_front: falcon eyes", "sun_disk: gold disk", "wing_feathers: brown-gold bands"],
        "texture_detail": "褐金羽毛、金色太阳盘、黑色隼眼和浅色尖喙。",
        "animation": "待机轻拍翼；飞行大幅拍翼；俯冲攻击时头和喙前压。",
        "signature_features": ["太阳盘", "隼头尖喙", "分段翼羽"],
        "modeling_musts": ["sun_disk 独立", "wing_feathers 分段", "beak 高对比"],
        "reject_if": ["像普通鸟", "没有太阳盘", "翼没有羽片"],
        "v2_design_freeze": [
            "正面第一识别必须是太阳圆盘、尖喙隼头和横展双翼，不能只是普通鸟。",
            "头部主体必须服从飞鸟身体比例；太阳盘必须独立高亮，头部眼睛和尖喙在侧面必须清楚。",
            "翼羽至少三段分层，尾羽独立，飞行单位轮廓必须成立。",
        ],
        "geometry": [
            ["falcon_body", "body", [-3.8, -4.4, -5.1], [3.8, 4.2, 5.7], [0, 24]],
            ["head", "head", [-2.6, -6.0, -5.6], [2.6, -0.6, 0.0], [0, 0]],
            ["beak", "head", [-1.35, -3.9, -8.95], [1.35, -1.75, -5.3], [24, 0]],
            ["sun_disk", "head", [-3.8, -9.2, -3], [3.8, -7.7, 3], [34, 0]],
            ["left_eye", "head", [-2.0, -4.6, -5.9], [-1.15, -3.75, -5.35], [64, 0]],
            ["right_eye", "head", [1.15, -4.6, -5.9], [2.0, -3.75, -5.35], [70, 0]],
            ["left_wing", "wings", [0, -2, -3], [18, 0, 7], [0, 42]],
            ["left_feathers", "wings", [5, 0, 5], [23, 1, 10], [42, 42]],
            ["right_wing", "wings", [-18, -2, -3], [0, 0, 7], [0, 58]],
            ["right_feathers", "wings", [-23, 0, 5], [-5, 1, 10], [42, 58]],
            ["tail", "tail", [-2.2, 1, 5], [2.2, 2.5, 12], [84, 42]],
        ],
    },
]


EGYPTIAN_1_0_DETAIL_REFINEMENTS: dict[str, dict[str, list[Any]]] = {
    "anubis_warrior": {
        "parts": [
            "mouth_line", "left_nostril", "right_nostril", "forehead_uraeus",
            "chest_scarab", "collar_center_gem", "left_bracer", "right_bracer",
            "khopesh_hook", "khopesh_counterweight", "belt_buckle",
        ],
        "geometry": [
            ["mouth_line", "head", [-1.5, -1.8, -8.4], [1.5, -1.0, -7.7], [76, 6]],
            ["left_nostril", "head", [-1.3, -2.9, -8.5], [-0.5, -2.1, -7.8], [88, 0]],
            ["right_nostril", "head", [0.5, -2.9, -8.5], [1.3, -2.1, -7.8], [92, 0]],
            ["forehead_uraeus", "head", [-0.7, -8.3, -3.8], [0.7, -6.2, -3.0], [96, 0]],
            ["chest_scarab", "body", [-1.7, -8.8, -3.15], [1.7, -5.3, -2.25], [96, 8]],
            ["collar_center_gem", "body", [-1.1, -10.9, -3.25], [1.1, -8.6, -2.25], [110, 8]],
            ["belt_buckle", "body", [-1.2, -0.8, -3.05], [1.2, 1.0, -2.15], [118, 8]],
            ["left_bracer", "arms", [3.7, -0.5, -2.1], [6.4, 2.3, 2.1], [84, 18]],
            ["right_bracer", "arms", [-6.4, -0.5, -2.1], [-3.7, 2.3, 2.1], [98, 18]],
            ["khopesh_hook", "weapon", [-10.4, -8.4, -0.9], [-8.4, -5.2, 0.9], [118, 16]],
            ["khopesh_counterweight", "weapon", [-7.5, 8.2, -0.8], [-5.8, 10.5, 0.8], [118, 22]],
        ],
    },
    "scarab_guardian": {
        "parts": [
            "left_shell_plate", "right_shell_plate", "rear_shell_plate", "scarab_sigil",
            "left_mandible_tip", "right_mandible_tip", "front_left_claw", "front_right_claw",
            "mid_left_claw", "mid_right_claw", "back_left_claw", "back_right_claw",
            "horn_tip",
        ],
        "geometry": [
            ["carapace_ridge", "body", [-1.0, -7.1, -5.6], [1.0, -4.2, 5.8], [68, 38]],
            ["left_shell_plate", "body", [2.0, -6.8, -5.8], [5.8, -4.4, 5.6], [82, 38]],
            ["right_shell_plate", "body", [-5.8, -6.8, -5.8], [-2.0, -4.4, 5.6], [82, 74]],
            ["rear_shell_plate", "body", [-4.5, -6.5, 4.8], [4.5, -4.2, 8.4], [76, 54]],
            ["scarab_sigil", "body", [-2.0, -7.5, -2.8], [2.0, -4.0, 2.8], [98, 54]],
            ["left_mandible", "head", [2.5, -1.6, -14.6], [5.0, 0.5, -12.6], [34, 0]],
            ["right_mandible", "head", [-5.0, -1.6, -14.6], [-2.5, 0.5, -12.6], [44, 0]],
            ["horn_tip", "head", [-0.8, -2.8, -19.0], [0.8, -1.2, -16.4], [78, 0]],
            ["front_left_claw", "legs", [10.5, 0.0, -8.4], [12.8, 1.6, -5.6], [0, 74]],
            ["front_right_claw", "legs", [-12.8, 0.0, -8.4], [-10.5, 1.6, -5.6], [12, 74]],
            ["mid_left_claw", "legs", [11.5, 0.0, -1.4], [13.8, 1.6, 1.4], [24, 74]],
            ["mid_right_claw", "legs", [-13.8, 0.0, -1.4], [-11.5, 1.6, 1.4], [36, 74]],
            ["back_left_claw", "legs", [10.4, 0.0, 5.8], [12.6, 1.6, 8.8], [48, 74]],
            ["back_right_claw", "legs", [-12.6, 0.0, 5.8], [-10.4, 1.6, 8.8], [60, 74]],
        ],
    },
    "mummy_soldier": {
        "parts": [
            "head_bandage_mid", "head_bandage_diagonal", "mouth_shadow", "left_cheek_wrap",
            "right_cheek_wrap", "bandage_mid", "bandage_low", "waist_wrap",
            "left_arm_bandage_upper", "right_arm_bandage_upper", "left_arm_bandage_lower",
            "right_arm_bandage_lower", "left_leg_bandage", "right_leg_bandage", "spear_tassel",
        ],
        "geometry": [
            ["head_bandage_upper", "head", [-3.05, -6.5, -3.55], [3.05, -5.4, -2.7], [34, 0]],
            ["head_bandage_lower", "head", [-2.95, -2.9, -3.55], [2.95, -1.9, -2.7], [34, 10]],
            ["head_bandage_mid", "head", [-2.85, -4.5, -3.65], [2.85, -3.6, -2.65], [34, 16]],
            ["head_bandage_diagonal", "head", [-2.3, -7.4, -4.0], [-0.65, -0.4, -3.0], [54, 16]],
            ["mouth_shadow", "head", [-1.45, -2.1, -3.85], [1.45, -1.2, -2.9], [66, 16]],
            ["left_cheek_wrap", "head", [-2.45, -3.5, -3.8], [-1.35, -1.2, -2.8], [76, 16]],
            ["right_cheek_wrap", "head", [1.35, -3.5, -3.8], [2.45, -1.2, -2.8], [84, 16]],
            ["bandage_mid", "body", [-3.9, -5.5, -2.6], [3.9, -4.4, -1.8], [28, 30]],
            ["bandage_low", "body", [-3.8, -1.8, -2.5], [3.8, -0.6, -1.7], [28, 36]],
            ["waist_wrap", "body", [-4.1, 0.2, -2.55], [4.1, 1.8, -1.7], [92, 16]],
            ["left_arm_bandage_upper", "arms", [3.4, -5.8, -1.9], [5.9, -4.6, 1.9], [60, 36]],
            ["right_arm_bandage_upper", "arms", [-5.9, -5.8, -1.9], [-3.4, -4.6, 1.9], [74, 36]],
            ["left_arm_bandage_lower", "arms", [3.4, -1.2, -1.9], [5.9, 0.0, 1.9], [60, 42]],
            ["right_arm_bandage_lower", "arms", [-5.9, -1.2, -1.9], [-3.4, 0.0, 1.9], [74, 42]],
            ["left_leg_bandage", "legs", [0.3, 7.2, -1.8], [3.2, 8.5, 1.8], [28, 48]],
            ["right_leg_bandage", "legs", [-3.2, 7.2, -1.8], [-0.3, 8.5, 1.8], [42, 48]],
            ["spear_tassel", "weapon", [-7.5, -8.9, -1.2], [-5.0, -7.2, 1.2], [112, 0]],
        ],
    },
    "sphinx_sentinel": {
        "parts": [
            "headdress_left_panel", "headdress_right_panel", "headdress_top_band",
            "headdress_brow", "left_pupil", "right_pupil", "chin_beard",
            "front_left_paw", "front_right_paw", "back_left_paw", "back_right_paw",
            "front_left_claws", "front_right_claws", "tail_tuft", "shoulder_scarab",
        ],
        "geometry": [
            ["headdress_left_panel", "head", [-5.9, -5.2, -5.8], [-3.5, 2.5, 0.5], [52, 12]],
            ["headdress_right_panel", "head", [3.5, -5.2, -5.8], [5.9, 2.5, 0.5], [66, 12]],
            ["headdress_top_band", "head", [-5.8, -8.4, -4.2], [5.8, -5.4, 1.8], [52, 0]],
            ["headdress_brow", "head", [-3.3, -7.9, -6.4], [3.3, -6.6, -5.5], [84, 12]],
            ["left_pupil", "head", [-2.35, -3.6, -6.9], [-1.9, -3.15, -6.2], [116, 0]],
            ["right_pupil", "head", [1.9, -3.6, -6.9], [2.35, -3.15, -6.2], [120, 0]],
            ["chin_beard", "head", [-1.0, -0.2, -6.6], [1.0, 2.9, -5.5], [116, 8]],
            ["shoulder_scarab", "body", [-2.2, -7.4, -12.9], [2.2, -4.2, -11.9], [116, 20]],
            ["front_left_paw", "legs", [1.1, 9.3, -11.0], [5.8, 11.5, -6.0], [0, 62]],
            ["front_right_paw", "legs", [-5.8, 9.3, -11.0], [-1.1, 11.5, -6.0], [18, 62]],
            ["back_left_paw", "legs", [1.5, 8.8, 4.5], [6.0, 11.0, 9.5], [36, 62]],
            ["back_right_paw", "legs", [-6.0, 8.8, 4.5], [-1.5, 11.0, 9.5], [54, 62]],
            ["front_left_claws", "legs", [1.0, 10.8, -11.8], [5.9, 12.0, -10.6], [0, 82]],
            ["front_right_claws", "legs", [-5.9, 10.8, -11.8], [-1.0, 12.0, -10.6], [20, 82]],
            ["tail_tuft", "tail", [-2.4, -2.0, 18.0], [2.4, 2.0, 22.2], [92, 62]],
        ],
    },
    "sun_falcon": {
        "parts": [
            "beak_tip", "left_brow", "right_brow", "sun_disk_left_ray",
            "sun_disk_right_ray", "left_primary_feather_a", "left_primary_feather_b",
            "left_primary_feather_c", "right_primary_feather_a", "right_primary_feather_b",
            "right_primary_feather_c", "left_talon", "right_talon", "tail_tip",
        ],
        "geometry": [
            ["belly", "body", [-2.2, 2.7, -3.2], [2.2, 4.7, 2.8], [36, 28]],
            ["beak_tip", "head", [-0.75, -3.65, -10.2], [0.75, -2.55, -7.95], [40, 0]],
            ["left_brow", "head", [-2.35, -4.9, -6.05], [-1.05, -4.25, -5.4], [80, 0]],
            ["right_brow", "head", [1.05, -4.9, -6.05], [2.35, -4.25, -5.4], [84, 0]],
            ["sun_disk_top_ray", "head", [-0.8, -10.8, -2.8], [0.8, -8.8, 2.8], [66, 0]],
            ["sun_disk_bottom_ray", "head", [-0.8, -7.7, -2.8], [0.8, -5.9, 2.8], [74, 0]],
            ["sun_disk_left_ray", "head", [-5.8, -9.2, -1.0], [-3.6, -7.7, 1.0], [88, 8]],
            ["sun_disk_right_ray", "head", [3.6, -9.2, -1.0], [5.8, -7.7, 1.0], [98, 8]],
            ["left_primary_feather_a", "wings", [3.0, 0.2, 4.8], [5.0, 1.4, 12.0], [0, 60]],
            ["left_primary_feather_b", "wings", [7.0, 0.2, 5.3], [9.0, 1.4, 12.8], [10, 60]],
            ["left_primary_feather_c", "wings", [11.0, 0.2, 5.8], [13.0, 1.4, 13.4], [20, 60]],
            ["right_primary_feather_a", "wings", [-5.0, 0.2, 4.8], [-3.0, 1.4, 12.0], [38, 60]],
            ["right_primary_feather_b", "wings", [-9.0, 0.2, 5.3], [-7.0, 1.4, 12.8], [48, 60]],
            ["right_primary_feather_c", "wings", [-13.0, 0.2, 5.8], [-11.0, 1.4, 13.4], [58, 60]],
            ["left_talon", "body", [1.2, 3.6, -2.8], [2.6, 5.4, -0.8], [78, 62]],
            ["right_talon", "body", [-2.6, 3.6, -2.8], [-1.2, 5.4, -0.8], [84, 62]],
            ["tail_tip", "tail", [-2.4, 1.0, 10.5], [2.4, 2.8, 14.5], [92, 34]],
        ],
    },
}


for creature in EGYPTIAN_CREATURES:
    refinement = EGYPTIAN_1_0_DETAIL_REFINEMENTS.get(creature["id"], {})
    existing_parts = set(creature["parts"])
    for part in refinement.get("parts", []):
        if part not in existing_parts:
            creature["parts"].append(part)
            existing_parts.add(part)
    creature["geometry"].extend(refinement.get("geometry", []))


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
    base.CREATURES = EGYPTIAN_CREATURES


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
    for creature in EGYPTIAN_CREATURES:
        cid = creature["id"]
        required = [
            DESIGN_DIR / f"{cid}.md",
            CONCEPT_DIR / f"{cid}_concept.png",
            BLOCKBENCH_DIR / f"{cid}.bbmodel",
            PREVIEW_DIR / cid / "uv_template.png",
            base.texture_path(cid),
            PREVIEW_DIR / cid / "texture_fit.png",
        ]
        required.extend(PREVIEW_DIR / cid / "blockbench" / f"{view}.png" for view in base.VIEWS)
        for path in required:
            if not path.exists():
                failures.append(f"{cid}: missing {path}")
        design = DESIGN_DIR / f"{cid}.md"
        if design.exists():
            text = design.read_text(encoding="utf-8")
            if creature.get("v2_design_freeze") and "## V2 造型冻结" not in text:
                failures.append(f"{cid}: design doc missing V2 造型冻结 section")
            for marker in creature.get("v2_design_freeze", []):
                if marker not in text:
                    failures.append(f"{cid}: design doc missing V2 marker {marker}")
        fit_summary = fit_by_id.get(cid)
        if not fit_summary:
            failures.append(f"{cid}: missing texture fit summary")
        elif not fit_summary.get("ok", False):
            failures.extend(f"{cid}: {failure}" for failure in fit_summary.get("failures", []))
        bbmodel = BLOCKBENCH_DIR / f"{cid}.bbmodel"
        if bbmodel.exists():
            data = json.loads(bbmodel.read_text(encoding="utf-8"))
            names = {element.get("name") for element in data.get("elements", [])}
            for row in creature["geometry"]:
                if row[0] not in names:
                    failures.append(f"{cid}: bbmodel missing element {row[0]}")
                _name, _group, frm, to, uv = row
                sx = math.ceil(abs(to[0] - frm[0]))
                sy = math.ceil(abs(to[1] - frm[1]))
                sz = math.ceil(abs(to[2] - frm[2]))
                if uv[0] + 2 * sz + 2 * sx > 128 or uv[1] + sz + sy > 128:
                    failures.append(f"{cid}: UV overflow on {row[0]}")
        concept = CONCEPT_DIR / f"{cid}_concept.png"
        if concept.exists():
            image = Image.open(concept).convert("RGBA")
            if image.size[0] < 1000 or image.size[1] < 700:
                failures.append(f"{cid}: concept art is too small")
    if failures:
        print("Egyptian Creature Visual Gate: FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Egyptian Creature Visual Gate: PASS")
    print(f"manifest: {MANIFEST_PATH}")
    print(f"design_dir: {DESIGN_DIR}")
    print(f"blockbench_dir: {BLOCKBENCH_DIR}")
    print(f"preview_dir: {PREVIEW_DIR}")
    print(f"texture_fit_report: {TEXTURE_FIT_REPORT_PATH}")
    print(f"contact_sheet: {CONTACT_SHEET_PATH}")
    return 0


def generate_all() -> None:
    configure_base()
    base.generate_all()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "command",
        choices=[
            "all",
            "designs",
            "concepts",
            "manifest",
            "bbmodels",
            "uv",
            "textures",
            "texture-fit",
            "previews",
            "contact-sheet",
            "concept-contact-sheet",
            "report",
            "audit",
        ],
    )
    args = parser.parse_args()
    configure_base()
    if args.command == "all":
        generate_all()
        return audit()
    if args.command == "audit":
        return audit()
    if args.command == "designs":
        base.write_design_docs()
    elif args.command == "concepts":
        base.draw_concept_art()
    elif args.command == "manifest":
        base.write_manifest()
    elif args.command == "bbmodels":
        base.write_bbmodels()
    elif args.command == "uv":
        base.draw_uv_templates()
    elif args.command == "textures":
        base.draw_runtime_textures()
    elif args.command == "texture-fit":
        base.write_texture_fit_report()
    elif args.command == "previews":
        base.draw_previews()
    elif args.command == "contact-sheet":
        base.draw_contact_sheet()
    elif args.command == "concept-contact-sheet":
        base.draw_concept_contact_sheet()
    elif args.command == "report":
        base.write_visual_report()
    return 0


if __name__ == "__main__":
    sys.exit(main())
