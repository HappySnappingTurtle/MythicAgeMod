#!/usr/bin/env python3
"""Design-first visual workflow for Atlantean 1.0 creature candidates."""

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

DESIGN_DIR = ROOT / "docs/美术资源/atlantean_creatures/design"
CONCEPT_DIR = ROOT / "docs/美术资源/atlantean_creatures/concepts"
BLOCKBENCH_DIR = ROOT / "docs/美术资源/blockbench/atlantean_creatures"
PREVIEW_DIR = ROOT / "docs/preview_assets/atlantean_creatures"
MANIFEST_PATH = ROOT / "docs/美术资源/atlantean_creatures/visual_manifest.json"
REPORT_PATH = PREVIEW_DIR / "visual_report.json"
TEXTURE_FIT_REPORT_PATH = PREVIEW_DIR / "texture_fit_report.json"
CONTACT_SHEET_PATH = PREVIEW_DIR / "blockbench_validation_contact_sheet.png"
CONCEPT_CONTACT_SHEET_PATH = PREVIEW_DIR / "concept_design_contact_sheet.png"


ATLANTEAN_CREATURES: list[dict[str, Any]] = [
    {
        "id": "crystal_leviathan",
        "entity_id": "mythic_age:crystal_leviathan",
        "display": "Crystal Leviathan / 水晶利维坦",
        "target_box": [2.4, 2.4],
        "visual_height": 2.65,
        "scale_note": "亚特兰蒂斯海渊巨兽，长身、水晶背脊、鳍翼和棱镜眼必须明确。",
        "primary": "#1b6f7a",
        "shadow": "#092b35",
        "accent": "#72e6f2",
        "silhouette": {
            "front": "宽阔龙首、棱镜双眼、侧鳍和下颚须鳍可读。",
            "side": "长蛇身、分段躯体、水晶背刺和尾鳍形成海兽侧影。",
            "back": "背部晶簇、尾鳍和发光鳞带一眼可见。",
        },
        "parts": [
            "crystal_head",
            "snout",
            "prism_eyes",
            "lower_jaw",
            "mandible_fins",
            "left_mandible_fin",
            "right_mandible_fin",
            "segmented_body",
            "body_1",
            "body_2",
            "body_3",
            "body_4",
            "glow_belly",
            "dorsal_crystals",
            "dorsal_crystal_1",
            "dorsal_crystal_2",
            "dorsal_crystal_3",
            "side_fins",
            "left_side_fin",
            "right_side_fin",
            "tail",
            "tail_fan",
        ],
        "hierarchy": ["root/head", "root/body_segments", "root/fins", "root/crystals", "root/tail"],
        "uv_plan": [
            "head_front: cyan prism eyes and jaw line",
            "body_segments: dark teal scale bands",
            "dorsal_crystals: bright cyan facets",
            "tail_fan: wave pattern",
        ],
        "texture_detail": "深海青蓝主色、亮青水晶棱面、浅蓝发光鳞带、棱镜眼和下颚暗线；不得出现空白透明面。",
        "animation": "待机时悬浮游弋；移动时分段身体横向波动；攻击时头部前探并触发棱镜潮汐冲击。",
        "signature_features": ["长身海兽", "水晶背刺", "棱镜眼", "鳍翼和尾扇"],
        "modeling_musts": ["body_segments 必须分段", "dorsal_crystals 独立成簇", "head 与 tail_fan 轮廓不能糊成一条蛇"],
        "reject_if": ["像普通蛇", "没有水晶背刺", "没有清楚的头和眼睛", "尾鳍不明显"],
        "v2_design_freeze": [
            "正面必须读到比例适中的海兽头、棱镜眼、下颚和须鳍，不得像普通蛇或青龙换色。",
            "侧面必须由分段长身、水晶背刺、侧鳍和尾扇组成海渊巨兽轮廓，体量主要来自长身分段，头部保持从属于身体的比例。",
            "水晶背刺和发光鳞带必须是独立高亮结构，头部、身体和尾扇不能糊成单条管状体，头部宽度不得超过身体主段宽度的 92%。",
        ],
        "geometry": [
            ["crystal_head", "head", [-3.5, -5.2, -8.0], [3.5, 1.2, 0.0], [0, 0]],
            ["snout", "head", [-2.6, -2.2, -12.0], [2.6, 1.0, -8.0], [42, 0]],
            ["lower_jaw", "head", [-2.4, 0.55, -11.2], [2.4, 2.0, -6.8], [72, 0]],
            ["left_prism_eye", "head", [-2.85, -3.85, -8.45], [-1.65, -2.6, -7.45], [96, 0]],
            ["right_prism_eye", "head", [1.65, -3.85, -8.45], [2.85, -2.6, -7.45], [104, 0]],
            ["left_mandible_fin", "fins", [3.6, -1.0, -11.0], [7.8, 1.2, -8.2], [112, 0]],
            ["right_mandible_fin", "fins", [-7.8, -1.0, -11.0], [-3.6, 1.2, -8.2], [0, 26]],
            ["body_1", "body_segments", [-4.6, -4.2, 0], [4.6, 4.2, 8], [22, 26]],
            ["body_2", "body_segments", [-4.2, -3.9, 8], [4.2, 3.9, 16], [54, 26]],
            ["body_3", "body_segments", [-3.8, -3.5, 16], [3.8, 3.5, 24], [84, 26]],
            ["body_4", "body_segments", [-3.2, -3.1, 24], [3.2, 3.1, 32], [0, 54]],
            ["glow_belly", "body_segments", [-2.2, 3.0, 0], [2.2, 4.4, 28], [26, 54]],
            ["dorsal_crystal_1", "crystals", [-1.2, -8.2, 2], [1.2, -4.0, 5], [58, 54]],
            ["dorsal_crystal_2", "crystals", [-1.1, -8.6, 9], [1.1, -3.8, 12], [70, 54]],
            ["dorsal_crystal_3", "crystals", [-1.0, -8.0, 17], [1.0, -3.7, 20], [82, 54]],
            ["left_side_fin", "fins", [4.0, -1.0, 5], [9.5, 1.5, 13], [94, 54]],
            ["right_side_fin", "fins", [-9.5, -1.0, 5], [-4.0, 1.5, 13], [0, 82]],
            ["tail", "tail", [-2.4, -2.4, 32], [2.4, 2.4, 44], [24, 82]],
            ["tail_fan", "tail", [-6.5, -1.0, 41], [6.5, 1.0, 49], [50, 82]],
        ],
    },
    {
        "id": "orichalcum_golem",
        "entity_id": "mythic_age:orichalcum_golem",
        "display": "Orichalcum Golem / 山铜魔像",
        "target_box": [1.7, 3.2],
        "visual_height": 3.45,
        "scale_note": "古代亚特兰蒂斯构装体，山铜装甲、发光核心、重拳和肩部晶柱必须突出。",
        "primary": "#9a6c38",
        "shadow": "#332116",
        "accent": "#36d6c9",
        "silhouette": {"front": "宽肩、发光胸核、巨拳。", "side": "厚装甲身躯和背部晶柱。", "back": "背晶柱和山铜铆钉可读。"},
        "parts": [
            "helmet_head", "core_eye", "chest_core", "core_frame", "shoulder_plates",
            "back_crystal_array", "heavy_arms", "forearm_plates", "giant_fists",
            "orichalcum_legs", "knee_plates", "heavy_feet",
        ],
        "hierarchy": ["root/head", "root/torso", "root/arms", "root/legs", "root/crystals"],
        "uv_plan": ["chest: cyan power core", "arms: bronze plates", "head: single slit eye", "crystals: cyan facets"],
        "texture_detail": "暗金山铜、青色能量核心、深色装甲缝和铆钉。",
        "animation": "沉重待机；行走时手臂低摆；攻击时巨拳蓄力下砸。",
        "signature_features": ["山铜装甲", "发光胸核", "巨拳", "肩背晶柱"],
        "modeling_musts": ["chest_core 独立高对比并带框", "arms 和 fists 远大于普通人形", "shoulder_plates 与 back_crystal_array 独立"],
        "reject_if": ["像普通铁傀儡", "没有核心", "没有亚特兰蒂斯水晶特征"],
        "v2_design_freeze": [
            "正面必须读到山铜构装体、宽肩、发光胸核、单眼缝和巨大重拳，不得像普通铁傀儡。",
            "背面和侧面必须有肩背晶柱、厚装甲板和铆钉/装甲缝分层。",
            "青色能量核心、暗金山铜装甲和水晶晶柱必须在 UV 上高对比分区。",
        ],
        "geometry": [
            ["torso", "torso", [-6, -12, -4], [6, 2, 4], [0, 30]],
            ["upper_torso_armor", "torso", [-7.8, -13.8, -5.0], [7.8, -8.8, 5.0], [42, 30]],
            ["lower_rune_plate", "torso", [-5.4, -3.0, -5.0], [5.4, 0.8, -3.8], [0, 54]],
            ["chest_core", "torso", [-3.2, -10.0, -5.4], [3.2, -3.2, -3.9], [42, 54]],
            ["core_frame_top", "torso", [-4.1, -10.8, -5.6], [4.1, -9.8, -3.8], [62, 54]],
            ["core_frame_bottom", "torso", [-4.1, -3.2, -5.6], [4.1, -2.2, -3.8], [82, 54]],
            ["core_frame_left", "torso", [-4.2, -9.8, -5.6], [-3.1, -3.2, -3.8], [102, 54]],
            ["core_frame_right", "torso", [3.1, -9.8, -5.6], [4.2, -3.2, -3.8], [112, 54]],
            ["head", "head", [-4.5, -19, -4], [4.5, -11, 4], [0, 0]],
            ["helmet_crown", "head", [-5.2, -20.8, -4.5], [5.2, -18.4, 4.5], [36, 0]],
            ["eye_slit", "head", [-3.0, -16.2, -4.8], [3.0, -14.9, -3.9], [60, 0]],
            ["crest_crystal", "head", [-1.4, -24.2, -1.2], [1.4, -20.2, 1.2], [76, 0]],
            ["left_shoulder_plate", "arms", [5.4, -15.2, -4.8], [11.2, -9.0, 4.8], [0, 82]],
            ["right_shoulder_plate", "arms", [-11.2, -15.2, -4.8], [-5.4, -9.0, 4.8], [34, 82]],
            ["back_center_crystal", "crystals", [-1.5, -22.0, 4.0], [1.5, -10.5, 7.2], [68, 0]],
            ["back_left_crystal", "crystals", [-5.8, -20.0, 4.2], [-2.7, -11.2, 7.0], [84, 0]],
            ["back_right_crystal", "crystals", [2.7, -20.0, 4.2], [5.8, -11.2, 7.0], [100, 0]],
            ["back_lower_crystal", "crystals", [-1.2, -12.2, 4.6], [1.2, -6.2, 7.0], [116, 0]],
            ["left_arm", "arms", [6.2, -10.0, -3.4], [11.0, 5.0, 3.4], [58, 58]],
            ["right_arm", "arms", [-11.0, -10.0, -3.4], [-6.2, 5.0, 3.4], [86, 58]],
            ["left_forearm_plate", "arms", [5.8, -1.0, -4.2], [11.5, 5.2, 4.2], [0, 100]],
            ["right_forearm_plate", "arms", [-11.5, -1.0, -4.2], [-5.8, 5.2, 4.2], [32, 100]],
            ["left_fist", "arms", [5.2, 4.2, -4.8], [12.0, 11.0, 4.8], [64, 100]],
            ["right_fist", "arms", [-12.0, 4.2, -4.8], [-5.2, 11.0, 4.8], [92, 92]],
            ["left_leg", "legs", [1.2, 1.5, -3], [5.2, 16.0, 3], [0, 64]],
            ["right_leg", "legs", [-5.2, 1.5, -3], [-1.2, 16.0, 3], [22, 64]],
            ["left_knee_plate", "legs", [0.7, 8.2, -3.8], [5.7, 11.8, -2.7], [44, 64]],
            ["right_knee_plate", "legs", [-5.7, 8.2, -3.8], [-0.7, 11.8, -2.7], [58, 64]],
            ["left_heavy_foot", "legs", [0.4, 15.0, -5.4], [6.0, 19.0, 3.8], [72, 64]],
            ["right_heavy_foot", "legs", [-6.0, 15.0, -5.4], [-0.4, 19.0, 3.8], [96, 78]],
        ],
    },
    {
        "id": "siren_priestess",
        "entity_id": "mythic_age:siren_priestess",
        "display": "Siren Priestess / 海妖祭司",
        "target_box": [0.9, 2.2],
        "visual_height": 2.35,
        "scale_note": "亚特兰蒂斯海妖施法者，人形上身、鱼尾、鳍冠和水晶权杖必须清楚。",
        "primary": "#2c8f9e",
        "shadow": "#0f303e",
        "accent": "#d7f7ff",
        "silhouette": {"front": "人形头冠、双臂和权杖。", "side": "鱼尾下身和背鳍。", "back": "鳍冠和长尾可读。"},
        "parts": [
            "face", "glowing_eyes", "mouth", "fin_crown", "crown_spire",
            "hair_fins", "torso", "wave_chest_ornament", "arms",
            "crystal_staff", "fish_tail", "tail_scales", "wide_tail_fin",
        ],
        "hierarchy": ["root/head", "root/torso", "root/arms", "root/tail", "root/staff"],
        "uv_plan": ["face: pale eyes", "tail: teal scales", "staff: cyan crystal", "crown: pale fin"],
        "texture_detail": "青蓝鱼鳞、白蓝眼睛、半透明鳍冠、水晶权杖和海浪纹长袍。",
        "animation": "悬浮漂游；施法时权杖抬起；尾巴缓慢摆动。",
        "signature_features": ["海妖人形上身", "鱼尾", "鳍冠", "水晶权杖"],
        "modeling_musts": ["fish_tail 不能做成人腿且必须有鳞片条", "staff 独立并带顶部晶体", "face 必须有眼睛、口鼻和鳍冠侧翼"],
        "reject_if": ["像普通女巫", "没有鱼尾", "没有权杖"],
        "v2_design_freeze": [
            "正面必须读到海妖祭司的人形上身、发光眼、鳍冠、双臂和水晶权杖；头部保持人形比例，不能靠放大头部表现细节。",
            "下身必须是鱼尾结构，包含尾基、尾尖和尾鳍，不能退化成人腿或裙摆。",
            "权杖晶体、鳍冠、青蓝鳞片和海浪纹长袍必须分区可读，头部宽度不得超过上身宽度的 82%。",
        ],
        "geometry": [
            ["torso", "torso", [-3.9, -10.4, -2.4], [3.9, 1.2, 2.4], [0, 26]],
            ["wave_chest_ornament", "torso", [-3.9, -8.8, -2.8], [3.9, -3.8, -1.6], [26, 26]],
            ["waist_shell_belt", "torso", [-2.4, -3.0, -2.8], [2.4, -0.4, -1.7], [48, 26]],
            ["head", "head", [-2.85, -16.6, -2.9], [2.85, -10.0, 2.9], [0, 0]],
            ["hair_back_panel", "head", [-2.5, -14.8, 2.35], [2.5, -8.9, 3.35], [62, 0]],
            ["left_eye", "head", [-1.95, -14.25, -3.35], [-0.9, -13.2, -2.6], [28, 0]],
            ["right_eye", "head", [0.9, -14.25, -3.35], [1.95, -13.2, -2.6], [34, 0]],
            ["mouth_line", "head", [-1.35, -12.15, -3.45], [1.35, -11.5, -2.7], [40, 0]],
            ["fin_crown", "head", [-3.4, -22, -1.0], [3.4, -16.5, 1.0], [42, 0]],
            ["crown_spire", "head", [-1.1, -24.2, -0.8], [1.1, -21.2, 0.8], [66, 0]],
            ["left_hair_fin", "head", [2.55, -16.5, 1.8], [4.65, -8.5, 3.0], [76, 0]],
            ["right_hair_fin", "head", [-4.65, -16.5, 1.8], [-2.55, -8.5, 3.0], [86, 0]],
            ["left_arm", "arms", [3.0, -9.5, -1.4], [5.0, 1.5, 1.4], [62, 0]],
            ["right_arm", "arms", [-5.0, -9.5, -1.4], [-3.0, 1.5, 1.4], [76, 0]],
            ["staff", "staff", [5.0, -16, -1.0], [6.2, 5, 1.0], [90, 0]],
            ["staff_crystal", "staff", [4.2, -20, -1.8], [7.0, -16, 1.8], [104, 0]],
            ["staff_top_spire", "staff", [4.9, -22.2, -0.9], [6.3, -19.8, 0.9], [118, 0]],
            ["tail_base", "tail", [-3.2, 0, -2.2], [3.2, 9, 2.2], [26, 34]],
            ["tail_scale_band_1", "tail", [-3.0, 2.0, -2.9], [3.0, 3.2, -1.8], [0, 54]],
            ["tail_scale_band_2", "tail", [-2.8, 5.1, -2.8], [2.8, 6.3, -1.8], [20, 54]],
            ["tail_tip", "tail", [-2.2, 8, -1.8], [2.2, 18, 1.8], [52, 34]],
            ["tail_tip_scale_band", "tail", [-2.0, 11.5, -2.3], [2.0, 12.6, -1.5], [40, 54]],
            ["tail_fin", "tail", [-6.2, 15, -1.2], [6.2, 19.2, 1.2], [74, 34]],
        ],
    },
    {
        "id": "tidal_guardian",
        "entity_id": "mythic_age:tidal_guardian",
        "display": "Tidal Guardian / 潮汐守卫",
        "target_box": [2.0, 1.4],
        "visual_height": 1.65,
        "scale_note": "龟甲与蟹钳混合的神殿守卫，厚壳、双钳、四足和水晶甲片必须可读。",
        "primary": "#2f5d62",
        "shadow": "#102225",
        "accent": "#48d1cc",
        "silhouette": {"front": "宽壳和巨大双钳。", "side": "低矮甲壳、短足和后尾鳍。", "back": "壳上水晶甲片。"},
        "parts": [
            "shell", "head", "left_eye", "right_eye", "beak_mouth", "lower_mandible", "left_claw", "right_claw",
            "front_left_leg", "front_right_leg", "back_left_leg", "back_right_leg",
            "crystal_shell_plates", "crystal_plate_1", "crystal_plate_2", "tail_fin",
        ],
        "hierarchy": ["root/shell", "root/head", "root/claws", "root/legs", "root/crystals"],
        "uv_plan": ["shell: dark turtle plates", "claws: cyan edge", "eyes: bright dots", "crystals: glowing shell ridges"],
        "texture_detail": "深青龟甲、青色水晶壳脊、钳尖高光和小而亮的眼睛。",
        "animation": "缓慢横移；攻击时双钳夹击；防御时甲壳稳定。",
        "signature_features": ["厚龟甲", "蟹钳", "水晶甲片", "低矮守卫体量"],
        "modeling_musts": ["claws 必须大", "shell 宽过身体", "crystal plates 独立"],
        "reject_if": ["像普通乌龟", "没有双钳", "水晶壳不明显"],
        "v2_design_freeze": [
            "正面必须读到低矮宽壳、巨大双蟹钳、小亮眼和厚甲，不得像普通乌龟。",
            "侧面必须体现短足横移体态、龟甲壳脊、水晶甲片和尾鳍。",
            "双钳必须大于普通前肢，水晶壳片必须独立高亮，不能只靠纹理暗示。",
        ],
        "geometry": [
            ["shell", "shell", [-8, -6, -8], [8, 2, 8], [0, 32]],
            ["head", "head", [-3.2, -4, -12], [3.2, 1, -7], [0, 0]],
            ["left_eye", "head", [-2.2, -2.8, -12.8], [-1.1, -1.7, -11.9], [28, 0]],
            ["right_eye", "head", [1.1, -2.8, -12.8], [2.2, -1.7, -11.9], [34, 0]],
            ["beak_mouth", "head", [-1.8, 0.8, -12.9], [1.8, 1.5, -12.1], [42, 4]],
            ["lower_mandible", "head", [-2.5, 1.1, -11.9], [2.5, 2.1, -9.5], [54, 16]],
            ["left_claw", "claws", [5.8, -2, -12], [11.5, 3, -5], [54, 0]],
            ["right_claw", "claws", [-11.5, -2, -12], [-5.8, 3, -5], [82, 0]],
            ["left_front_leg", "legs", [5.2, 1, -5], [8.5, 6, -2], [0, 60]],
            ["right_front_leg", "legs", [-8.5, 1, -5], [-5.2, 6, -2], [16, 60]],
            ["left_back_leg", "legs", [5.2, 1, 4], [8.5, 6, 7], [32, 60]],
            ["right_back_leg", "legs", [-8.5, 1, 4], [-5.2, 6, 7], [48, 60]],
            ["crystal_plate_1", "crystals", [-1.0, -9.5, -4], [1.0, -5.5, -1], [64, 60]],
            ["crystal_plate_2", "crystals", [-1.0, -9.0, 2], [1.0, -5.2, 5], [74, 60]],
            ["tail_fin", "shell", [-3.5, -1, 8], [3.5, 2, 12], [84, 60]],
        ],
    },
    {
        "id": "abyssal_ray",
        "entity_id": "mythic_age:abyssal_ray",
        "display": "Abyssal Ray / 深渊鳐",
        "target_box": [2.6, 0.9],
        "visual_height": 1.2,
        "scale_note": "深海飞鳐，宽大三角翼、长尾刺、发光腹纹和头部眼点必须清楚。",
        "primary": "#183f5a",
        "shadow": "#071923",
        "accent": "#7de7ff",
        "silhouette": {"front": "薄而宽的翼展和双眼。", "side": "扁平身体、尾刺和下垂鳍边。", "back": "长尾和发光背纹。"},
        "parts": [
            "ray_body", "head_lobe", "left_eye", "right_eye", "mouth_slit", "left_wing", "right_wing",
            "left_wing_tip", "right_wing_tip", "glow_belly", "tail", "tail_spine",
        ],
        "hierarchy": ["root/body", "root/wings", "root/tail", "root/glow"],
        "uv_plan": ["wings: dark blue gradients", "eyes: cyan dots", "belly: rune glow", "tail: bright spine"],
        "texture_detail": "深蓝背部、青色发光腹部符纹、亮眼和细长尾刺。",
        "animation": "飞/游时双翼缓慢拍动；攻击时尾刺甩动。",
        "signature_features": ["巨大翼展", "扁平鳐身", "长尾刺", "发光腹纹"],
        "modeling_musts": ["wings 必须宽", "tail_spine 必须长", "eyes 独立高对比"],
        "reject_if": ["像普通鱼", "翼展不明显", "没有尾刺"],
        "v2_design_freeze": [
            "正面必须读到薄而宽的深海鳐翼展、头部眼点和扁平身体，不得像普通鱼。",
            "侧面必须读到长尾和尾刺，尾刺必须是独立尖细结构。",
            "青色发光腹纹、亮眼、深蓝背部和翼尖必须分区可读，宽翼是第一轮廓。",
        ],
        "geometry": [
            ["ray_body", "body", [-5.5, -2.0, -7.0], [5.5, 1.6, 7.0], [0, 0]],
            ["left_wing", "wings", [4.6, -0.9, -6.4], [16.1, 0.9, 6.6], [0, 38]],
            ["right_wing", "wings", [-16.1, -0.9, -6.4], [-4.6, 0.9, 6.6], [0, 58]],
            ["head_lobe", "body", [-3.7, -1.8, -11.2], [3.7, 1.5, -6.2], [0, 22]],
            ["left_eye", "body", [-3.2, -2.05, -11.55], [-1.9, -0.95, -10.85], [26, 22]],
            ["right_eye", "body", [1.9, -2.05, -11.55], [3.2, -0.95, -10.85], [32, 22]],
            ["mouth_slit", "body", [-1.4, 0.9, -9.0], [1.4, 1.6, -8.1], [38, 22]],
            ["glow_belly", "glow", [-3.4, 1.2, -5.4], [3.4, 2.2, 3.4], [76, 78]],
            ["tail", "tail", [-1.0, -0.75, 5.0], [1.0, 0.75, 18.0], [0, 78]],
            ["tail_spine", "tail", [-0.45, -0.45, 21.5], [0.45, 0.45, 26.5], [48, 78]],
            ["left_wing_tip", "wings", [17.4, -0.45, -2.0], [22.8, 0.45, 3.2], [84, 38]],
            ["right_wing_tip", "wings", [-22.8, -0.45, -2.0], [-17.4, 0.45, 3.2], [84, 58]],
        ],
    },
]


ATLANTEAN_1_0_DETAIL_REFINEMENTS: dict[str, dict[str, list[Any]]] = {
    "crystal_leviathan": {
        "parts": [
            "head_crown_crystal", "mouth_line", "left_gill_slits", "right_gill_slits",
            "body_1_glow_scale", "body_2_glow_scale", "body_3_glow_scale", "body_4_glow_scale",
            "dorsal_crystal_4", "left_side_fin_rib", "right_side_fin_rib",
            "tail_fan_left_rib", "tail_fan_right_rib", "tail_tip_crystal",
        ],
        "geometry": [
            ["head_crown_crystal", "head", [-1.3, -8.8, -5.5], [1.3, -5.9, -2.5], [116, 8]],
            ["mouth_line", "head", [-2.4, 1.9, -13.2], [2.4, 2.8, -12.0], [112, 16]],
            ["left_gill_slits", "head", [3.8, -2.2, -7.9], [4.8, 1.0, -3.8], [102, 12]],
            ["right_gill_slits", "head", [-4.8, -2.2, -7.9], [-3.8, 1.0, -3.8], [114, 12]],
            ["body_1_glow_scale", "body_segments", [-2.1, 3.2, 1.0], [2.1, 4.7, 6.8], [0, 96]],
            ["body_2_glow_scale", "body_segments", [-1.9, 2.9, 9.0], [1.9, 4.3, 14.8], [18, 96]],
            ["body_3_glow_scale", "body_segments", [-1.7, 2.6, 17.0], [1.7, 3.9, 22.8], [36, 96]],
            ["body_4_glow_scale", "body_segments", [-1.4, 2.3, 25.0], [1.4, 3.5, 30.8], [54, 96]],
            ["dorsal_crystal_4", "crystals", [-0.9, -7.5, 25.0], [0.9, -3.5, 28.0], [112, 54]],
            ["left_side_fin_rib", "fins", [5.0, -1.3, 6.0], [9.5, -0.2, 7.4], [94, 68]],
            ["right_side_fin_rib", "fins", [-9.5, -1.3, 6.0], [-5.0, -0.2, 7.4], [108, 68]],
            ["tail_fan_left_rib", "tail", [0.8, -1.3, 43.0], [5.8, 1.3, 44.5], [50, 96]],
            ["tail_fan_right_rib", "tail", [-5.8, -1.3, 43.0], [-0.8, 1.3, 44.5], [68, 96]],
            ["tail_tip_crystal", "tail", [-1.0, -1.0, 48.0], [1.0, 1.0, 52.0], [88, 96]],
        ],
    },
    "orichalcum_golem": {
        "parts": [
            "upper_torso_rivet_left", "upper_torso_rivet_right", "lower_rune_glyph",
            "core_vertical_glow", "left_shoulder_rivet", "right_shoulder_rivet",
            "left_fist_knuckle_a", "left_fist_knuckle_b", "right_fist_knuckle_a", "right_fist_knuckle_b",
            "left_foot_toe_plate", "right_foot_toe_plate",
        ],
        "geometry": [
            ["upper_torso_rivet_left", "torso", [-5.5, -12.6, -5.7], [-4.0, -11.0, -4.4], [12, 116]],
            ["upper_torso_rivet_right", "torso", [4.0, -12.6, -5.7], [5.5, -11.0, -4.4], [20, 116]],
            ["lower_rune_glyph", "torso", [-1.3, -2.5, -5.8], [1.3, 0.2, -4.4], [28, 116]],
            ["core_vertical_glow", "torso", [-0.7, -9.4, -6.1], [0.7, -3.8, -4.2], [38, 116]],
            ["left_shoulder_rivet", "arms", [7.8, -13.6, -5.4], [9.6, -11.8, -4.1], [48, 116]],
            ["right_shoulder_rivet", "arms", [-9.6, -13.6, -5.4], [-7.8, -11.8, -4.1], [56, 116]],
            ["left_fist_knuckle_a", "arms", [6.2, 8.2, -5.6], [8.2, 10.6, -4.4], [64, 116]],
            ["left_fist_knuckle_b", "arms", [9.0, 8.2, -5.6], [11.0, 10.6, -4.4], [72, 116]],
            ["right_fist_knuckle_a", "arms", [-11.0, 8.2, -5.6], [-9.0, 10.6, -4.4], [80, 116]],
            ["right_fist_knuckle_b", "arms", [-8.2, 8.2, -5.6], [-6.2, 10.6, -4.4], [88, 116]],
            ["left_foot_toe_plate", "legs", [1.0, 18.0, -6.4], [5.8, 20.0, -4.8], [96, 116]],
            ["right_foot_toe_plate", "legs", [-5.8, 18.0, -6.4], [-1.0, 20.0, -4.8], [110, 116]],
        ],
    },
    "siren_priestess": {
        "parts": [
            "left_ear_fin", "right_ear_fin", "left_wrist_fin", "right_wrist_fin",
            "staff_rune_band", "tail_scale_band_3", "tail_fin_left_edge", "tail_fin_right_edge",
        ],
        "geometry": [
            ["left_ear_fin", "head", [2.8, -15.3, -4.0], [4.6, -11.0, -3.0], [96, 12]],
            ["right_ear_fin", "head", [-4.6, -15.3, -4.0], [-2.8, -11.0, -3.0], [104, 12]],
            ["left_wrist_fin", "arms", [4.6, -1.0, -2.0], [6.2, 2.2, 2.0], [112, 12]],
            ["right_wrist_fin", "arms", [-6.2, -1.0, -2.0], [-4.6, 2.2, 2.0], [112, 20]],
            ["staff_rune_band", "staff", [4.6, -11.0, -1.3], [6.6, -9.8, 1.3], [90, 22]],
            ["tail_scale_band_3", "tail", [-2.4, 14.2, -2.2], [2.4, 15.3, -1.4], [58, 54]],
            ["tail_fin_left_edge", "tail", [1.6, 14.8, -1.5], [6.8, 19.5, -0.2], [98, 34]],
            ["tail_fin_right_edge", "tail", [-6.8, 14.8, -1.5], [-1.6, 19.5, -0.2], [112, 34]],
        ],
    },
    "tidal_guardian": {
        "parts": [
            "shell_plate_front", "shell_plate_left", "shell_plate_right", "shell_plate_back",
            "left_claw_upper_tip", "left_claw_lower_tip", "right_claw_upper_tip", "right_claw_lower_tip",
            "front_left_foot_claws", "front_right_foot_claws", "back_left_foot_claws", "back_right_foot_claws",
            "crystal_plate_3", "crystal_plate_4", "tail_fin_tip",
        ],
        "geometry": [
            ["shell_plate_front", "shell", [-6.8, -7.7, -7.4], [6.8, -5.8, -4.6], [54, 32]],
            ["shell_plate_left", "shell", [5.8, -7.2, -5.0], [8.4, -5.1, 5.0], [90, 32]],
            ["shell_plate_right", "shell", [-8.4, -7.2, -5.0], [-5.8, -5.1, 5.0], [0, 105]],
            ["shell_plate_back", "shell", [-5.6, -7.0, 4.4], [5.6, -5.2, 7.6], [0, 92]],
            ["left_claw_upper_tip", "claws", [9.4, -3.2, -12.8], [12.6, -0.7, -10.0], [22, 92]],
            ["left_claw_lower_tip", "claws", [9.4, 1.2, -12.8], [12.6, 3.6, -10.0], [36, 92]],
            ["right_claw_upper_tip", "claws", [-12.6, -3.2, -12.8], [-9.4, -0.7, -10.0], [50, 92]],
            ["right_claw_lower_tip", "claws", [-12.6, 1.2, -12.8], [-9.4, 3.6, -10.0], [64, 92]],
            ["front_left_foot_claws", "legs", [5.2, 5.4, -6.2], [8.6, 6.7, -5.0], [78, 92]],
            ["front_right_foot_claws", "legs", [-8.6, 5.4, -6.2], [-5.2, 6.7, -5.0], [90, 92]],
            ["back_left_foot_claws", "legs", [5.2, 5.4, 3.1], [8.6, 6.7, 4.3], [102, 92]],
            ["back_right_foot_claws", "legs", [-8.6, 5.4, 3.1], [-5.2, 6.7, 4.3], [114, 92]],
            ["crystal_plate_3", "crystals", [-4.0, -8.0, -0.2], [-2.0, -4.8, 2.4], [84, 58]],
            ["crystal_plate_4", "crystals", [2.0, -8.0, -0.2], [4.0, -4.8, 2.4], [94, 58]],
            ["tail_fin_tip", "shell", [-1.1, -0.4, 11.4], [1.1, 1.6, 14.6], [106, 86]],
        ],
    },
    "abyssal_ray": {
        "parts": [
            "back_ridge", "left_gill_mark", "right_gill_mark",
            "left_wing_rib_a", "left_wing_rib_b", "left_wing_rib_c",
            "right_wing_rib_a", "right_wing_rib_b", "right_wing_rib_c",
            "glow_rune_front", "glow_rune_center", "glow_rune_back",
            "tail_barb_left", "tail_barb_right",
        ],
        "geometry": [
            ["back_ridge", "body", [-3.8, -2.7, -5.0], [3.8, -1.5, 3.8], [44, 0]],
            ["left_gill_mark", "body", [-4.0, -0.5, -8.8], [-2.0, 0.5, -7.7], [44, 22]],
            ["right_gill_mark", "body", [2.0, -0.5, -8.8], [4.0, 0.5, -7.7], [54, 22]],
            ["left_wing_rib_a", "wings", [7.0, -1.2, -5.0], [15.2, -0.1, -3.8], [34, 98]],
            ["left_wing_rib_b", "wings", [7.6, -1.2, -0.6], [17.0, -0.1, 0.6], [52, 98]],
            ["left_wing_rib_c", "wings", [7.0, -1.2, 3.8], [15.2, -0.1, 5.0], [72, 98]],
            ["right_wing_rib_a", "wings", [-15.2, -1.2, -5.0], [-7.0, -0.1, -3.8], [34, 108]],
            ["right_wing_rib_b", "wings", [-17.0, -1.2, -0.6], [-7.6, -0.1, 0.6], [52, 108]],
            ["right_wing_rib_c", "wings", [-15.2, -1.2, 3.8], [-7.0, -0.1, 5.0], [72, 108]],
            ["glow_rune_front", "glow", [-1.2, 1.8, -5.8], [1.2, 2.5, -4.2], [0, 98]],
            ["glow_rune_center", "glow", [-1.4, 1.8, -1.4], [1.4, 2.5, 0.4], [10, 98]],
            ["glow_rune_back", "glow", [-1.2, 1.8, 2.8], [1.2, 2.5, 4.4], [22, 98]],
            ["tail_barb_left", "tail", [-1.0, -0.6, 24.6], [-0.2, 0.6, 28.0], [58, 78]],
            ["tail_barb_right", "tail", [0.2, -0.6, 24.6], [1.0, 0.6, 28.0], [68, 78]],
        ],
    },
}


for creature in ATLANTEAN_CREATURES:
    refinement = ATLANTEAN_1_0_DETAIL_REFINEMENTS.get(creature["id"], {})
    existing_parts = set(creature["parts"])
    for part in refinement.get("parts", []):
        if part not in existing_parts:
            creature["parts"].append(part)
            existing_parts.add(part)
    existing_geometry = {row[0] for row in creature["geometry"]}
    for row in refinement.get("geometry", []):
        if row[0] not in existing_geometry:
            creature["geometry"].append(row)
            existing_geometry.add(row[0])


BASE_DECORATE_TEXTURE_REGION = base.decorate_texture_region


def decorate_atlantean_texture_region(draw: Any, creature: dict[str, Any], row: list[Any], rect: tuple[int, int, int, int]) -> None:
    BASE_DECORATE_TEXTURE_REGION(draw, creature, row, rect)

    name = row[0]
    shadow = base.hex_to_rgb(creature["shadow"])
    accent = base.hex_to_rgb(creature["accent"])
    primary = base.hex_to_rgb(creature["primary"])
    x1, y1, x2, y2 = rect
    width = max(1, x2 - x1)
    height = max(1, y2 - y1)

    if any(token in name for token in ("crystal", "glow", "core", "rune")):
        draw.rectangle(rect, fill=base.shade_rgb(accent, 8) + (255,), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(229, 255, 255, 255), width=1)
        return
    if any(token in name for token in ("eye", "prism")):
        draw.rectangle(rect, fill=(210, 255, 255, 255), outline=shadow + (255,), width=1)
        draw.rectangle((x1 + max(1, width // 3), y1 + max(1, height // 3), x2 - max(1, width // 3), y2), fill=(12, 66, 76, 255))
        return
    if any(token in name for token in ("mouth", "jaw", "mandible", "gill")):
        draw.rectangle(rect, fill=base.shade_rgb(shadow, 16) + (255,), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y1, x2 - 2, y2 - 1), fill=accent + (255,), width=1)
        return
    if any(token in name for token in ("fin", "wing", "rib")):
        draw.rectangle(rect, fill=base.shade_rgb(primary, 16) + (255,), outline=shadow + (255,), width=1)
        for offset in range(-height, width + height, max(3, min(width, height) // 2)):
            draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=base.shade_rgb(accent, -8) + (255,), width=1)
        return
    if any(token in name for token in ("claw", "barb", "spine", "tip", "tooth")):
        draw.polygon([(x1, y2), ((x1 + x2) // 2, y1), (x2, y2)], fill=base.shade_rgb(accent, 10) + (255,), outline=shadow + (255,))
        return
    if any(token in name for token in ("plate", "armor", "shell", "rivet", "knuckle", "foot")):
        draw.rectangle(rect, fill=base.shade_rgb(primary, -6) + (255,), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=base.shade_rgb(accent, -18) + (255,), width=1)
        return
    if "scale" in name:
        draw.rectangle(rect, fill=base.shade_rgb(primary, 10) + (255,), outline=shadow + (255,), width=1)
        for yy in range(y1 + 1, y2, max(2, height // 3)):
            draw.line((x1 + 1, yy, x2 - 2, yy), fill=base.shade_rgb(accent, -20) + (255,), width=1)


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
    base.CREATURES = ATLANTEAN_CREATURES
    base.decorate_texture_region = decorate_atlantean_texture_region


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
    for creature in ATLANTEAN_CREATURES:
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
        concept_path = CONCEPT_DIR / f"{cid}_concept.png"
        if concept_path.exists():
            with Image.open(concept_path) as image:
                if image.width < 900 or image.height < 420:
                    failures.append(f"{cid}: concept image too small for design review")
    if failures:
        print("Atlantean Creature Visual Gate: FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Atlantean Creature Visual Gate: PASS")
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
