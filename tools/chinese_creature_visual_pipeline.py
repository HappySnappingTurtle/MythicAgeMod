#!/usr/bin/env python3
"""Design-first visual workflow for Chinese 1.0 creature candidates."""

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

DESIGN_DIR = ROOT / "docs/美术资源/chinese_creatures/design"
CONCEPT_DIR = ROOT / "docs/美术资源/chinese_creatures/concepts"
BLOCKBENCH_DIR = ROOT / "docs/美术资源/blockbench/chinese_creatures"
PREVIEW_DIR = ROOT / "docs/preview_assets/chinese_creatures"
MANIFEST_PATH = ROOT / "docs/美术资源/chinese_creatures/visual_manifest.json"
REPORT_PATH = PREVIEW_DIR / "visual_report.json"
TEXTURE_FIT_REPORT_PATH = PREVIEW_DIR / "texture_fit_report.json"
CONTACT_SHEET_PATH = PREVIEW_DIR / "blockbench_validation_contact_sheet.png"
CONCEPT_CONTACT_SHEET_PATH = PREVIEW_DIR / "concept_design_contact_sheet.png"


CHINESE_CREATURES: list[dict[str, Any]] = [
    {
        "id": "azure_dragon",
        "entity_id": "mythic_age:azure_dragon",
        "display": "Azure Dragon / 青龙",
        "target_box": [1.6, 2.2],
        "visual_height": 2.45,
        "scale_note": "长身飞行东方龙，龙角、龙须、鳞片、爪和云纹必须清楚。",
        "primary": "#2f8f8a",
        "shadow": "#123b42",
        "accent": "#d8b65a",
        "silhouette": {
            "front": "龙头、双角、龙须和前爪一眼可见。",
            "side": "长蛇身、分段身躯、尾鳍和云气侧影明显。",
            "back": "背鳍、尾部和鳞片分段可读。",
        },
        "parts": [
            "dragon_head", "snout", "left_horn", "right_horn", "left_whisker", "right_whisker",
            "left_eye", "right_eye", "body_1", "body_2", "body_3", "body_4",
            "belly_scales", "left_front_claw", "right_front_claw", "left_back_claw",
            "right_back_claw", "tail", "tail_fin", "dorsal_fin",
        ],
        "hierarchy": ["root/head", "root/body_segments", "root/claws", "root/tail", "root/fins"],
        "uv_plan": ["head_front: gold eyes and whiskers", "body_segments: teal scale bands", "horns: gold tips", "tail: jade fin"],
        "texture_detail": "青绿色鳞片、金色龙角和眼睛、浅玉腹鳞、红金云纹点缀；脸部必须有眼、须、吻部。",
        "animation": "待机悬浮盘旋；飞行时身体分段波动；攻击时龙头前探喷吐龙息。",
        "signature_features": ["东方龙长身", "双角和龙须", "青绿色鳞片", "无翼飞行"],
        "modeling_musts": ["body_segments 必须分段", "horns 和 whiskers 独立", "爪与头部不能糊成一块"],
        "reject_if": ["像普通蛇", "没有龙角/龙须", "身体没有分段"],
        "v2_design_freeze": [
            "正面必须读到东方龙头、双角、龙须、金色眼睛和前爪，不得像普通蛇或海蛇。",
            "侧面必须由至少 4 段长身、腹鳞、背鳍和尾鳍组成连续但分节的无翼飞行轮廓。",
            "龙须、龙角、爪和头部必须分层可读，青绿色鳞片和玉色腹鳞不得混成单色长条。",
        ],
        "geometry": [
            ["dragon_head", "head", [-3.25, -5.1, -7.7], [3.25, 1.3, 0.2], [0, 0]],
            ["snout", "head", [-2.1, -2.0, -11.2], [2.1, 1.0, -7.5], [36, 0]],
            ["left_horn", "head", [1.8, -10.5, -4], [3.0, -5.8, -2.5], [62, 0]],
            ["right_horn", "head", [-3.0, -10.5, -4], [-1.8, -5.8, -2.5], [70, 0]],
            ["left_whisker", "head", [3.5, -2.2, -9.2], [10.0, -0.6, -7.6], [80, 0]],
            ["right_whisker", "head", [-10.0, -2.2, -9.2], [-3.5, -0.6, -7.6], [98, 0]],
            ["left_eye", "head", [-2.45, -4.15, -8.15], [-1.55, -3.15, -7.25], [118, 0]],
            ["right_eye", "head", [1.55, -4.15, -8.15], [2.45, -3.15, -7.25], [108, 10]],
            ["body_1", "body_segments", [-3.2, -3, 0], [3.2, 3, 8], [0, 28]],
            ["body_2", "body_segments", [-3.0, -2.8, 8], [3.0, 2.8, 16], [28, 28]],
            ["body_3", "body_segments", [-2.8, -2.6, 16], [2.8, 2.6, 24], [54, 28]],
            ["body_4", "body_segments", [-2.5, -2.4, 24], [2.5, 2.4, 32], [78, 28]],
            ["belly_scales", "body_segments", [-1.8, 2.2, 0], [1.8, 3.4, 28], [0, 54]],
            ["left_front_claw", "claws", [2.8, 1, 1], [6.0, 5.5, 4.2], [62, 54]],
            ["right_front_claw", "claws", [-6.0, 1, 1], [-2.8, 5.5, 4.2], [78, 54]],
            ["left_back_claw", "claws", [2.2, 1, 18], [5.2, 5.2, 21], [94, 54]],
            ["right_back_claw", "claws", [-5.2, 1, 18], [-2.2, 5.2, 21], [110, 54]],
            ["dorsal_fin", "fins", [-0.8, -5.5, 2], [0.8, -2.6, 28], [0, 78]],
            ["tail", "tail", [-2.0, -2.0, 32], [2.0, 2.0, 44], [36, 78]],
            ["tail_fin", "tail", [-4.5, -0.8, 40], [4.5, 0.8, 48], [58, 78]],
        ],
    },
    {
        "id": "qilin_guardian",
        "entity_id": "mythic_age:qilin_guardian",
        "display": "Qilin Guardian / 麒麟守卫",
        "target_box": [1.5, 2.0],
        "visual_height": 2.25,
        "scale_note": "祥瑞兽，鹿角、鬃毛、鳞甲和蹄足必须明确。",
        "primary": "#b84a36",
        "shadow": "#3a1812",
        "accent": "#d9b84a",
        "silhouette": {"front": "鹿角、鬃毛和宽胸。", "side": "马鹿身、鳞片和尾鬃。", "back": "背鬃和尾巴可读。"},
        "parts": [
            "head", "snout", "left_eye", "right_eye", "left_antler", "right_antler",
            "left_antler_tip", "right_antler_tip", "mane", "scaled_body", "scale_plate",
            "front_left_leg", "front_right_leg", "back_left_leg", "back_right_leg",
            "hooves", "tail", "tail_plume",
        ],
        "hierarchy": ["root/head", "root/body", "root/legs", "root/tail"],
        "uv_plan": ["head_front: bright eyes", "antlers: gold", "body_side: red-gold scales"],
        "texture_detail": "朱红鳞甲、金色角、深色鬃毛和玉色眼睛。",
        "animation": "优雅行走；攻击时前蹄踏地；守护技能时头部稳定。",
        "signature_features": ["麒麟鹿角", "鳞甲", "蹄足", "祥瑞配色"],
        "modeling_musts": ["antlers 独立", "mane 独立", "四足和躯干分离"],
        "reject_if": ["像普通马", "没有角", "没有鳞片"],
        "v2_design_freeze": [
            "正面必须读到麒麟鹿角、明亮眼睛、鬃毛和宽胸，不得像普通马或鹿。",
            "头部主体必须匹配马鹿式四足身体比例；侧面必须有背部鬃毛、鳞甲板、蹄足和尾鬃分区。",
            "祥瑞朱红、金色鹿角、玉色眼睛和红金鳞甲必须在 UV 中各自落位。",
        ],
        "geometry": [
            ["body", "body", [-5.3, -6.4, -10.4], [5.3, 2.4, 10.4], [0, 28]],
            ["scale_plate", "body", [-4.5, -7, -8], [4.5, -5, 7], [56, 28]],
            ["head", "head", [-3.5, -5.6, -5.8], [3.5, 1.0, 0.6], [0, 0]],
            ["snout", "head", [-2.35, -1.1, -9.3], [2.35, 2.0, -5.5], [32, 0]],
            ["left_antler", "head", [1.8, -11, -3], [3.2, -5.5, -1], [52, 0]],
            ["right_antler", "head", [-3.2, -11, -3], [-1.8, -5.5, -1], [62, 0]],
            ["mane", "head", [-3.0, -6, 0.5], [3.0, 3, 2.0], [72, 0]],
            ["front_left_leg", "legs", [2.4, 0, -7], [4.8, 11, -4], [0, 58]],
            ["front_right_leg", "legs", [-4.8, 0, -7], [-2.4, 11, -4], [14, 58]],
            ["back_left_leg", "legs", [2.4, 0, 5], [4.8, 11, 8], [28, 58]],
            ["back_right_leg", "legs", [-4.8, 0, 5], [-2.4, 11, 8], [42, 58]],
            ["tail", "tail", [-1.2, -2, 10], [1.2, 6, 14], [68, 58]],
        ],
    },
    {
        "id": "jiangshi",
        "entity_id": "mythic_age:jiangshi",
        "display": "Jiangshi / 僵尸",
        "target_box": [0.8, 2.1],
        "visual_height": 2.15,
        "scale_note": "中式僵尸，官帽、符箓、伸直手臂和青白脸必须清楚。",
        "primary": "#46605f",
        "shadow": "#162323",
        "accent": "#d6b64a",
        "silhouette": {"front": "官帽、额头符、双臂前伸。", "side": "僵硬跳步和长袖轮廓。", "back": "长袍和帽尾可见。"},
        "parts": [
            "head", "pale_face", "eyes", "mouth", "official_hat", "hat_wings",
            "hat_tail", "talisman_paper", "talisman_red_marks", "robe_body",
            "robe_front_panel", "robe_collar", "sleeves", "cuffs", "stiff_arms",
            "legs", "black_boots",
        ],
        "hierarchy": ["root/head", "root/body", "root/arms", "root/legs"],
        "uv_plan": [
            "face_front: pale blue-white face with dark eyes and mouth",
            "talisman: yellow paper on forehead with three red symbol strokes",
            "hat: black official hat, side wings and rear tail",
            "robe: dark teal cloth, collar, front panel and cuffs",
        ],
        "texture_detail": "青白脸、深色眼口、黄符红纹、深青官服、黑官帽、长袖袖口和黑靴必须分区清楚。",
        "animation": "僵硬跳步；攻击时双臂前推；受击时头部轻晃。",
        "signature_features": ["官帽", "符箓", "伸直手臂", "青白脸"],
        "modeling_musts": ["talisman 必须独立且带红色符纹", "arms 前伸并有袖口", "robe 要有领口、前片和下摆分层"],
        "reject_if": ["像普通僵尸", "没有符", "没有官帽"],
        "v2_design_freeze": [
            "正面必须读到中式官帽、额头黄符、青白脸和双臂前伸，不得像普通僵尸。",
            "身体必须有深青官服、长袍前片和袖口，侧面要体现僵硬跳步和前伸手臂。",
            "符箓必须是独立高对比面，红色符纹不能被脸部或帽子纹理吞掉。",
        ],
        "geometry": [
            ["body", "body", [-4.2, -10.2, -2.3], [4.2, 2.0, 2.3], [0, 24]],
            ["robe_shell", "body", [-4.8, -10.0, -2.9], [4.8, 4.4, 2.9], [30, 24]],
            ["robe_front_panel", "body", [-1.2, -9.6, -3.35], [1.2, 4.0, -2.5], [66, 24]],
            ["robe_collar", "body", [-3.8, -10.8, -3.4], [3.8, -8.0, -2.3], [76, 24]],
            ["robe_lower_hem", "body", [-5.2, 1.2, -3.2], [5.2, 5.0, 3.2], [90, 36]],
            ["head", "head", [-3.1, -8.0, -3.55], [3.1, 0.0, 3.55], [0, 0]],
            ["pale_face", "head", [-2.6, -6.5, -4.2], [2.6, -1.0, -3.3], [34, 0]],
            ["left_eye", "head", [-2.2, -5.1, -4.6], [-1.1, -4.0, -3.7], [58, 0]],
            ["right_eye", "head", [1.1, -5.1, -4.6], [2.2, -4.0, -3.7], [64, 0]],
            ["mouth_line", "head", [-1.5, -2.35, -4.75], [1.5, -0.95, -3.5], [70, 0]],
            ["official_hat", "head", [-4.0, -10.1, -4.0], [4.0, -7.0, 4.0], [80, 0]],
            ["hat_crown", "head", [-3.1, -12.2, -3.4], [3.1, -9.2, 3.4], [0, 16]],
            ["hat_tail", "head", [-1.2, -11.6, 2.8], [1.2, -6.8, 6.2], [30, 16]],
            ["left_hat_wing", "head", [3.55, -9.7, -1.45], [6.45, -7.4, 1.45], [42, 16]],
            ["right_hat_wing", "head", [-6.45, -9.7, -1.45], [-3.55, -7.4, 1.45], [56, 16]],
            ["talisman_paper", "head", [-1.35, -7.25, -4.85], [1.35, -1.55, -4.05], [70, 16]],
            ["talisman_mark_top", "head", [-0.95, -6.35, -5.2], [0.95, -5.15, -4.0], [82, 16]],
            ["talisman_mark_mid", "head", [-0.95, -4.85, -5.2], [0.95, -3.65, -4.0], [92, 16]],
            ["talisman_mark_low", "head", [-0.95, -3.35, -5.2], [0.95, -2.15, -4.0], [102, 16]],
            ["left_arm", "arms", [3.5, -8.2, -8.8], [5.9, -4.9, 2.5], [0, 54]],
            ["left_cuff", "arms", [3.2, -9.0, -9.5], [6.2, -4.2, -7.2], [30, 54]],
            ["right_arm", "arms", [-5.9, -8.2, -8.8], [-3.5, -4.9, 2.5], [44, 54]],
            ["right_cuff", "arms", [-6.2, -9.0, -9.5], [-3.2, -4.2, -7.2], [74, 54]],
            ["left_leg", "legs", [0.6, 2.0, -1.5], [3.2, 13.2, 1.5], [0, 84]],
            ["right_leg", "legs", [-3.2, 2.0, -1.5], [-0.6, 13.2, 1.5], [14, 84]],
            ["left_boot", "legs", [0.2, 12.0, -2.0], [3.6, 14.2, 1.8], [28, 84]],
            ["right_boot", "legs", [-3.6, 12.0, -2.0], [-0.2, 14.2, 1.8], [46, 84]],
        ],
    },
    {
        "id": "nine_tailed_fox",
        "entity_id": "mythic_age:nine_tailed_fox",
        "display": "Nine-tailed Fox / 九尾狐",
        "target_box": [1.2, 1.4],
        "visual_height": 1.55,
        "scale_note": "灵狐，中型四足，九尾扇形必须成为主要轮廓。",
        "primary": "#f0d2a2",
        "shadow": "#8b4f35",
        "accent": "#e84a5f",
        "silhouette": {"front": "狐脸尖耳和胸毛。", "side": "四足狐身和多尾侧影。", "back": "九尾扇形最醒目。"},
        "parts": [
            "fox_body", "shoulder_fur", "head", "snout", "nose", "red_eyes",
            "ears", "chest_fur", "nine_individual_tails", "tail_tips",
            "legs", "paws",
        ],
        "hierarchy": ["root/body", "root/head", "root/tails", "root/legs"],
        "uv_plan": [
            "face_front: red eyes, dark nose and fox snout",
            "chest_fur: bright white chest patch",
            "tails: nine individually countable pale-gold tails with pink tips",
            "legs: warm fur with darker paws",
        ],
        "texture_detail": "浅金毛、红眼、深色鼻口、粉红尾尖、白胸毛和深色小爪必须分区清楚。",
        "animation": "轻快步态；攻击时尾部扇动；魅惑技能时尾巴展开。",
        "signature_features": ["九条尾巴", "狐脸尖耳", "红眼"],
        "modeling_musts": ["tails 必须 9 条独立可数并形成扇形", "ears、snout、red_eyes 和 chest_fur 必须独立", "身体轻盈且爪部小巧"],
        "reject_if": ["像普通狐狸", "尾巴不明显", "没有尖耳"],
        "v2_design_freeze": [
            "正面必须读到狐脸、尖耳、红眼和白胸毛，不得像普通狗或狼。",
            "狐脸主体必须轻巧，识别来自尖耳、眼、鼻口和九尾扇形，不能用大头压过身体。",
            "背面和 45 度必须读到九尾扇形；运行模型必须有 9 条独立尾部或等价可数结构。",
            "浅金毛、红眼、粉红尾尖和白胸毛必须在纹理上分区清楚。",
        ],
        "geometry": [
            ["body", "body", [-4.4, -4.4, -8.4], [4.4, 3.0, 7.0], [0, 28]],
            ["shoulder_fur", "body", [-3.4, -5.4, -6.4], [3.4, -3.1, 2.5], [42, 28]],
            ["chest_fur", "body", [-2.9, -3.0, -8.9], [2.9, 2.4, -7.8], [70, 28]],
            ["head", "head", [-3.0, -4.95, -5.05], [3.0, 0.85, 0.55], [0, 0]],
            ["snout", "head", [-1.75, -1.55, -8.25], [1.75, 0.9, -4.75], [28, 0]],
            ["nose", "head", [-0.7, -0.25, -8.65], [0.7, 0.55, -7.93], [46, 0]],
            ["mouth_line", "head", [-1.2, 0.58, -8.45], [1.2, 1.0, -7.79], [54, 0]],
            ["left_eye", "head", [-2.35, -2.55, -5.5], [-1.4, -1.6, -4.8], [60, 0]],
            ["right_eye", "head", [1.4, -2.55, -5.5], [2.35, -1.6, -4.8], [66, 0]],
            ["left_ear", "head", [1.55, -8.5, -3.4], [3.15, -4.9, -1.6], [72, 0]],
            ["right_ear", "head", [-3.15, -8.5, -3.4], [-1.55, -4.9, -1.6], [82, 0]],
            ["tail_1", "tails", [-10.0, -3.0, 6.0], [-8.0, 0.0, 17.0], [0, 72]],
            ["tail_2", "tails", [-7.7, -4.0, 6.0], [-5.7, 0.0, 18.0], [12, 72]],
            ["tail_3", "tails", [-5.4, -5.0, 6.0], [-3.4, 0.0, 19.0], [24, 72]],
            ["tail_4", "tails", [-3.1, -5.7, 6.0], [-1.1, 0.0, 20.0], [36, 72]],
            ["tail_5", "tails", [-1.0, -6.0, 6.0], [1.0, 0.0, 21.0], [48, 72]],
            ["tail_6", "tails", [1.1, -5.7, 6.0], [3.1, 0.0, 20.0], [60, 72]],
            ["tail_7", "tails", [3.4, -5.0, 6.0], [5.4, 0.0, 19.0], [72, 72]],
            ["tail_8", "tails", [5.7, -4.0, 6.0], [7.7, 0.0, 18.0], [84, 72]],
            ["tail_9", "tails", [8.0, -3.0, 6.0], [10.0, 0.0, 17.0], [96, 72]],
            ["tail_tip_1", "tails", [-10.3, -3.2, 15.5], [-7.7, 0.2, 18.5], [0, 96]],
            ["tail_tip_2", "tails", [-8.0, -4.2, 16.5], [-5.4, 0.2, 19.6], [12, 96]],
            ["tail_tip_3", "tails", [-5.7, -5.2, 17.6], [-3.1, 0.2, 20.8], [24, 96]],
            ["tail_tip_4", "tails", [-3.4, -5.9, 18.8], [-0.8, 0.2, 21.8], [36, 96]],
            ["tail_tip_5", "tails", [-1.3, -6.2, 19.8], [1.3, 0.2, 22.8], [48, 96]],
            ["tail_tip_6", "tails", [0.8, -5.9, 18.8], [3.4, 0.2, 21.8], [60, 96]],
            ["tail_tip_7", "tails", [3.1, -5.2, 17.6], [5.7, 0.2, 20.8], [72, 96]],
            ["tail_tip_8", "tails", [5.4, -4.2, 16.5], [8.0, 0.2, 19.6], [84, 96]],
            ["tail_tip_9", "tails", [7.7, -3.2, 15.5], [10.3, 0.2, 18.5], [96, 96]],
            ["front_left_leg", "legs", [1.8, 0.0, -5.3], [3.6, 8.0, -3.0], [0, 52]],
            ["front_right_leg", "legs", [-3.6, 0.0, -5.3], [-1.8, 8.0, -3.0], [10, 52]],
            ["back_left_leg", "legs", [1.8, 0.0, 4.0], [3.7, 8.0, 6.3], [20, 52]],
            ["back_right_leg", "legs", [-3.7, 0.0, 4.0], [-1.8, 8.0, 6.3], [32, 52]],
            ["front_paws", "legs", [-3.8, 7.1, -6.0], [3.8, 9.0, -3.2], [44, 52]],
            ["back_paws", "legs", [-3.9, 7.1, 3.7], [3.9, 9.0, 6.7], [68, 52]],
        ],
    },
    {
        "id": "stone_lion_guardian",
        "entity_id": "mythic_age:stone_lion_guardian",
        "display": "Stone Lion Guardian / 石狮守卫",
        "target_box": [1.6, 1.7],
        "visual_height": 1.85,
        "scale_note": "神殿石狮，卷鬃、石质身体和爪下绣球必须可读。",
        "primary": "#8f8a7a",
        "shadow": "#3f3b34",
        "accent": "#b93a32",
        "silhouette": {"front": "比例合理的石狮头脸、卷鬃、宽胸、前爪绣球。", "side": "蹲坐石狮身和尾卷。", "back": "石尾和背部纹样。"},
        "parts": [
            "stone_body", "shoulder_plate", "stone_plinth", "head", "snout",
            "gold_eyes", "nose", "fangs", "curled_mane_top",
            "curled_mane_sides", "curled_mane_bottom", "mane_curls",
            "front_paws", "paw_pads", "claws", "back_legs",
            "embroidered_ball", "ball_ribbons", "ball_top_knot",
            "curled_tail", "tail_curl",
        ],
        "hierarchy": ["root/body", "root/head", "root/mane", "root/paws", "root/ball", "root/tail"],
        "uv_plan": [
            "mane: carved curl blocks and dark groove lines",
            "body: grey stone cracks and shoulder/plinth layers",
            "face: gold eyes, dark nose and small fangs",
            "ball: red lacquer sphere with ribbon bands",
            "tail: curled stone tail with separated curl",
        ],
        "texture_detail": "灰石纹、深色裂缝、分块卷鬃、金色眼点、深色鼻口、厚爪尖、红色绣球和卷尾必须分区清楚。",
        "animation": "低伏守卫；攻击时前爪压下；防御时身体稳定。",
        "signature_features": ["石狮", "卷鬃", "绣球", "石质裂纹"],
        "modeling_musts": ["curled_mane 必须拆成上/侧/下卷鬃和卷纹块", "ball 必须独立且有绣球带", "前爪必须厚重并有爪尖", "tail 必须形成卷尾而不是普通尾巴"],
        "reject_if": ["像普通狮子", "没有绣球", "没有石纹"],
        "v2_design_freeze": [
            "正面必须读到神殿石狮的比例合理头脸、卷鬃、宽胸、厚前爪和爪下绣球；头本体不能和身体一样高，不得像普通大头狮子。",
            "侧面必须体现蹲坐石狮体态、石质躯干、卷尾和厚重爪掌。",
            "灰石纹、深色裂缝、金色眼点和红色绣球必须分区可读，绣球必须是独立模型部件。",
        ],
        "geometry": [
            ["stone_body", "body", [-6.4, -6.6, -8.5], [6.4, 4.2, 7.3], [0, 34]],
            ["shoulder_plate", "body", [-5.4, -7.4, -6.5], [5.4, -4.9, 3.5], [60, 34]],
            ["stone_plinth", "body", [-5.8, 0.8, -8.2], [5.8, 4.8, 6.5], [0, 60]],
            ["head", "head", [-4.65, -5.9, -6.3], [4.65, 1.5, 0.7], [0, 0]],
            ["snout", "head", [-3.05, -1.75, -9.6], [3.05, 1.55, -5.7], [42, 0]],
            ["left_eye", "head", [-3.35, -3.8, -6.85], [-2.25, -2.7, -6.1], [68, 0]],
            ["right_eye", "head", [2.25, -3.8, -6.85], [3.35, -2.7, -6.1], [76, 0]],
            ["nose", "head", [-0.9, -0.55, -10.1], [0.9, 0.45, -9.3], [84, 0]],
            ["left_fang", "head", [-2.7, 1.4, -10.5], [-1.4, 3.2, -9.4], [94, 0]],
            ["right_fang", "head", [1.4, 1.4, -10.5], [2.7, 3.2, -9.4], [104, 0]],
            ["mane_top", "mane", [-6.9, -8.4, -5.2], [6.9, -5.2, 2.4], [0, 18]],
            ["mane_left", "mane", [-7.7, -5.4, -5.4], [-4.8, 3.6, 2.2], [46, 18]],
            ["mane_right", "mane", [4.8, -5.4, -5.4], [7.7, 3.6, 2.2], [70, 18]],
            ["mane_bottom", "mane", [-5.0, 2.0, -5.2], [5.0, 5.4, 1.8], [94, 18]],
            ["mane_top_curl", "mane", [-1.6, -9.8, -4.4], [1.6, -7.1, -1.4], [0, 80]],
            ["mane_left_curl_a", "mane", [-6.0, -6.4, -5.9], [-3.4, -3.7, -3.5], [16, 80]],
            ["mane_right_curl_a", "mane", [3.4, -6.4, -5.9], [6.0, -3.7, -3.5], [30, 80]],
            ["mane_left_curl_b", "mane", [-6.5, -1.9, -5.9], [-3.9, 0.8, -3.5], [44, 80]],
            ["mane_right_curl_b", "mane", [3.9, -1.9, -5.9], [6.5, 0.8, -3.5], [58, 80]],
            ["front_left_paw", "paws", [1.2, 0.0, -8.2], [5.8, 7.0, -3.8], [0, 92]],
            ["front_left_paw_pad", "paws", [0.8, 5.6, -9.2], [6.0, 8.0, -3.8], [22, 92]],
            ["front_left_claws", "paws", [1.2, 7.6, -10.0], [5.7, 8.8, -8.8], [48, 92]],
            ["front_right_paw", "paws", [-5.8, 0.0, -8.2], [-1.2, 7.0, -3.8], [80, 70]],
            ["front_right_paw_pad", "paws", [-6.0, 5.6, -9.2], [-0.8, 8.0, -3.8], [100, 70]],
            ["front_right_claws", "paws", [-5.7, 7.6, -10.0], [-1.2, 8.8, -8.8], [64, 92]],
            ["back_left_leg", "paws", [2.0, 0.0, 4.0], [5.8, 7.8, 8.2], [0, 106]],
            ["back_left_paw", "paws", [1.6, 6.5, 3.4], [6.0, 8.4, 8.4], [20, 106]],
            ["back_right_leg", "paws", [-5.8, 0.0, 4.0], [-2.0, 7.8, 8.2], [42, 106]],
            ["back_right_paw", "paws", [-6.0, 6.5, 3.4], [-1.6, 8.4, 8.4], [62, 106]],
            ["embroidered_ball", "ball", [2.2, 3.0, -11.5], [6.2, 7.0, -7.5], [82, 92]],
            ["ball_horizontal_ribbon", "ball", [1.7, 4.6, -12.0], [6.7, 5.4, -7.0], [100, 92]],
            ["ball_vertical_ribbon", "ball", [3.9, 2.5, -12.0], [4.8, 7.5, -7.0], [0, 118]],
            ["ball_top_knot", "ball", [3.4, 1.7, -10.2], [5.0, 3.1, -8.6], [14, 118]],
            ["curled_tail", "tail", [-1.4, -2.0, 8.0], [1.4, 4.8, 14.0], [24, 110]],
            ["tail_curl", "tail", [-2.2, -4.2, 12.0], [2.2, 0.2, 16.4], [44, 110]],
            ["tail_tip", "tail", [-1.1, -5.2, 11.5], [1.1, -3.1, 13.7], [66, 110]],
        ],
    },
]


CHINESE_1_0_DETAIL_REFINEMENTS: dict[str, dict[str, list[Any]]] = {
    "azure_dragon": {
        "parts": [
            "mouth_line", "left_brow", "right_brow", "left_horn_tip", "right_horn_tip",
            "left_whisker_tip", "right_whisker_tip", "body_1_belly_scale",
            "body_2_belly_scale", "body_3_belly_scale", "body_4_belly_scale",
            "dorsal_fin_front", "dorsal_fin_mid", "dorsal_fin_back",
            "left_front_talon", "right_front_talon", "left_back_talon", "right_back_talon",
            "tail_tip", "tail_fin_left", "tail_fin_right",
        ],
        "geometry": [
            ["mouth_line", "head", [-2.0, -1.1, -11.8], [2.0, -0.2, -10.8], [116, 8]],
            ["left_brow", "head", [-3.2, -5.7, -8.7], [-1.5, -4.8, -7.8], [108, 20]],
            ["right_brow", "head", [1.5, -5.7, -8.7], [3.2, -4.8, -7.8], [116, 20]],
            ["left_horn_tip", "head", [2.1, -12.6, -4.2], [2.9, -10.3, -3.0], [62, 12]],
            ["right_horn_tip", "head", [-2.9, -12.6, -4.2], [-2.1, -10.3, -3.0], [70, 12]],
            ["left_whisker_tip", "head", [9.0, -2.0, -9.4], [11.8, -0.8, -8.0], [80, 10]],
            ["right_whisker_tip", "head", [-11.8, -2.0, -9.4], [-9.0, -0.8, -8.0], [98, 10]],
            ["body_1_belly_scale", "body_segments", [-1.8, 2.2, 0.4], [1.8, 3.4, 6.8], [0, 54]],
            ["body_2_belly_scale", "body_segments", [-1.7, 2.0, 8.4], [1.7, 3.2, 14.8], [22, 54]],
            ["body_3_belly_scale", "body_segments", [-1.5, 1.9, 16.4], [1.5, 3.0, 22.8], [44, 54]],
            ["body_4_belly_scale", "body_segments", [-1.4, 1.7, 24.4], [1.4, 2.7, 30.8], [0, 68]],
            ["dorsal_fin_front", "fins", [-0.9, -6.2, 2.0], [0.9, -3.4, 8.0], [0, 92]],
            ["dorsal_fin_mid", "fins", [-0.85, -6.0, 10.0], [0.85, -3.2, 18.0], [10, 92]],
            ["dorsal_fin_back", "fins", [-0.75, -5.6, 20.0], [0.75, -3.0, 28.0], [20, 92]],
            ["left_front_talon", "claws", [3.7, 5.0, 0.3], [6.3, 6.0, 1.3], [62, 66]],
            ["right_front_talon", "claws", [-6.3, 5.0, 0.3], [-3.7, 6.0, 1.3], [78, 66]],
            ["left_back_talon", "claws", [3.1, 4.8, 18.5], [5.5, 5.8, 19.5], [94, 66]],
            ["right_back_talon", "claws", [-5.5, 4.8, 18.5], [-3.1, 5.8, 19.5], [110, 66]],
            ["tail_tip", "tail", [-1.0, -1.0, 46.0], [1.0, 1.0, 50.0], [90, 78]],
            ["tail_fin_left", "tail", [2.0, -0.9, 40.5], [5.4, 0.9, 48.0], [58, 92]],
            ["tail_fin_right", "tail", [-5.4, -0.9, 40.5], [-2.0, 0.9, 48.0], [76, 92]],
        ],
    },
    "qilin_guardian": {
        "parts": [
            "mouth_line", "left_antler_branch_a", "left_antler_branch_b",
            "right_antler_branch_a", "right_antler_branch_b",
            "scale_plate_front", "scale_plate_mid", "scale_plate_back",
            "left_side_scale_front", "right_side_scale_front",
            "left_side_scale_mid", "right_side_scale_mid",
            "left_side_scale_back", "right_side_scale_back",
            "mane_front", "mane_mid", "mane_back",
            "front_left_hoof", "front_right_hoof", "back_left_hoof", "back_right_hoof",
            "chest_fur", "tail_plume_upper", "tail_plume_lower",
        ],
        "geometry": [
            ["left_eye", "head", [-3.1, -3.0, -7.5], [-2.1, -2.0, -6.7], [56, 0]],
            ["right_eye", "head", [2.1, -3.0, -7.5], [3.1, -2.0, -6.7], [62, 0]],
            ["mouth_line", "head", [-1.5, 0.6, -10.8], [1.5, 1.4, -9.8], [68, 0]],
            ["left_antler_branch_a", "head", [2.7, -10.4, -3.2], [5.5, -9.4, -2.2], [82, 0]],
            ["left_antler_branch_b", "head", [2.8, -8.2, -3.0], [5.0, -7.3, -2.1], [96, 0]],
            ["right_antler_branch_a", "head", [-5.5, -10.4, -3.2], [-2.7, -9.4, -2.2], [82, 10]],
            ["right_antler_branch_b", "head", [-5.0, -8.2, -3.0], [-2.8, -7.3, -2.1], [96, 10]],
            ["scale_plate_front", "body", [-4.2, -7.2, -8.0], [4.2, -5.1, -3.5], [56, 28]],
            ["scale_plate_mid", "body", [-4.0, -7.0, -3.5], [4.0, -5.0, 2.0], [78, 28]],
            ["scale_plate_back", "body", [-3.8, -6.8, 2.0], [3.8, -4.9, 7.2], [98, 28]],
            ["left_side_scale_front", "body", [-5.5, -2.4, -6.7], [-4.5, 2.4, -3.5], [0, 54]],
            ["right_side_scale_front", "body", [4.5, -2.4, -6.7], [5.5, 2.4, -3.5], [10, 54]],
            ["left_side_scale_mid", "body", [-5.5, -2.4, -1.9], [-4.5, 2.4, 1.3], [20, 54]],
            ["right_side_scale_mid", "body", [4.5, -2.4, -1.9], [5.5, 2.4, 1.3], [30, 54]],
            ["left_side_scale_back", "body", [-5.5, -2.4, 2.9], [-4.5, 2.4, 6.1], [40, 54]],
            ["right_side_scale_back", "body", [4.5, -2.4, 2.9], [5.5, 2.4, 6.1], [50, 54]],
            ["mane_front", "mane", [-1.35, -4.8, -7.5], [1.35, 0.4, -2.3], [70, 54]],
            ["mane_mid", "mane", [-1.25, -5.8, -3.2], [1.25, -0.2, 2.0], [84, 54]],
            ["mane_back", "mane", [-1.15, -5.2, 1.2], [1.15, -0.4, 6.7], [98, 54]],
            ["front_left_hoof", "legs", [2.1, 9.6, -7.3], [5.1, 11.2, -3.8], [10, 76]],
            ["front_right_hoof", "legs", [-5.1, 9.6, -7.3], [-2.1, 11.2, -3.8], [34, 76]],
            ["back_left_hoof", "legs", [2.0, 9.6, 4.8], [5.2, 11.2, 8.2], [60, 76]],
            ["back_right_hoof", "legs", [-5.2, 9.6, 4.8], [-2.0, 11.2, 8.2], [86, 76]],
            ["chest_fur", "body", [-3.0, -3.0, -10.8], [3.0, 2.4, -9.2], [112, 42]],
            ["tail_plume_upper", "tail", [-3.2, -4.6, 15.0], [3.2, -1.6, 19.0], [22, 98]],
            ["tail_plume_lower", "tail", [-2.7, 1.6, 14.8], [2.7, 4.8, 18.8], [54, 98]],
        ],
    },
}


for creature in CHINESE_CREATURES:
    if creature["id"] == "azure_dragon":
        for row in creature["geometry"]:
            if row[0] == "dragon_head":
                row[2][:] = [-3.25, -5.1, -7.7]
                row[3][:] = [3.25, 1.3, 0.2]
            elif row[0] == "snout":
                row[2][:] = [-2.1, -2.0, -11.2]
                row[3][:] = [2.1, 1.0, -7.5]
            elif row[0] == "left_eye":
                row[2][:] = [-2.45, -4.15, -8.15]
                row[3][:] = [-1.55, -3.15, -7.25]
            elif row[0] == "right_eye":
                row[2][:] = [1.55, -4.15, -8.15]
                row[3][:] = [2.45, -3.15, -7.25]

    refinement = CHINESE_1_0_DETAIL_REFINEMENTS.get(creature["id"], {})
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


def decorate_chinese_texture_region(draw: Any, creature: dict[str, Any], row: list[Any], rect: tuple[int, int, int, int]) -> None:
    BASE_DECORATE_TEXTURE_REGION(draw, creature, row, rect)
    name = row[0]
    shadow = base.hex_to_rgb(creature["shadow"])
    accent = base.hex_to_rgb(creature["accent"])
    primary = base.hex_to_rgb(creature["primary"])
    x1, y1, x2, y2 = rect
    width = max(1, x2 - x1)
    height = max(1, y2 - y1)

    if creature["id"] == "azure_dragon":
        if "eye" in name or "brow" in name:
            draw.rectangle(rect, fill=(216, 184, 72, 255), outline=shadow + (255,), width=1)
            draw.rectangle((x1 + max(1, width // 3), y1 + max(1, height // 3), x2 - max(1, width // 3), y2), fill=(30, 28, 16, 255))
            return
        if "mouth" in name:
            draw.rectangle(rect, fill=(52, 24, 22, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(180, 52, 38, 255), width=1)
            return
        if "horn" in name:
            draw.rectangle(rect, fill=(216, 184, 82, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(244, 220, 126, 255), width=1)
            return
        if "whisker" in name:
            draw.rectangle(rect, fill=(223, 202, 126, 255), outline=shadow + (255,), width=1)
            return
        if "belly_scale" in name:
            draw.rectangle(rect, fill=(174, 218, 194, 255), outline=shadow + (255,), width=1)
            for yy in range(y1 + 2, y2, max(2, height // 3)):
                draw.line((x1 + 1, yy, x2 - 2, yy), fill=(91, 148, 136, 255), width=1)
            return
        if "dorsal_fin" in name or "tail_fin" in name or "tail_tip" in name:
            draw.rectangle(rect, fill=base.shade_rgb(accent, 10) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(236, 226, 142, 255), width=1)
            return
        if "talon" in name or "claw" in name:
            if "talon" in name:
                draw.rectangle(rect, fill=(232, 218, 176, 255), outline=shadow + (255,), width=1)
                draw.line((x1 + 1, y1, x2 - 2, y2 - 1), fill=(110, 88, 54, 255), width=1)
                return
            draw.polygon([(x1, y2), ((x1 + x2) // 2, y1), (x2, y2)], fill=(232, 218, 176, 255), outline=shadow + (255,))
            return
        if "body_" in name or name == "dragon_head" or name == "snout":
            draw.rectangle(rect, fill=base.shade_rgb(primary, 8) + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width, max(4, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(43, 125, 116, 255), width=1)
            return

    if creature["id"] == "qilin_guardian":
        if "eye" in name:
            draw.rectangle(rect, fill=(226, 236, 190, 255), outline=shadow + (255,), width=1)
            draw.rectangle((x1 + max(1, width // 3), y1 + max(1, height // 3), x2 - max(1, width // 3), y2), fill=(36, 26, 18, 255))
            return
        if "mouth" in name:
            draw.rectangle(rect, fill=(72, 26, 22, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1, x2 - 2, y2 - 1), fill=(190, 74, 48, 255), width=1)
            return
        if "antler" in name:
            draw.rectangle(rect, fill=(218, 184, 74, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(245, 219, 130, 255), width=1)
            return
        if "scale" in name or "plate" in name:
            draw.rectangle(rect, fill=(205, 92, 58, 255), outline=shadow + (255,), width=1)
            for offset in range(-height, width, max(4, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(222, 178, 72, 255), width=1)
            return
        if "mane" in name or "chest_fur" in name:
            draw.rectangle(rect, fill=(75, 38, 26, 255), outline=shadow + (255,), width=1)
            for offset in range(0, width + height, max(3, width // 4)):
                draw.line((x1 + offset, y1, x1 + offset - height, y2), fill=(155, 70, 42, 255), width=1)
            return
        if "hoof" in name:
            draw.rectangle(rect, fill=(44, 25, 20, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=(218, 184, 74, 255), width=1)
            return
        if "tail_plume" in name:
            draw.rectangle(rect, fill=(88, 42, 30, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(205, 92, 58, 255), width=1)
            return

    if creature["id"] == "nine_tailed_fox":
        if "eye" in name:
            draw.rectangle(rect, fill=(224, 42, 72, 255))
            draw.point((x1, y1), fill=(92, 18, 28, 255))
            draw.point((min(x2, x1 + 1), y1), fill=(255, 192, 205, 255))
            return
        if "chest_fur" in name:
            draw.rectangle(rect, fill=(255, 244, 218, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(230, 199, 156, 255), width=1)
            return
        if "tail_tip" in name:
            draw.rectangle(rect, fill=(232, 74, 95, 255), outline=(142, 62, 55, 255), width=1)
            return
        if "tail_" in name:
            draw.rectangle(rect, fill=(246, 213, 154, 255), outline=shadow + (255,), width=1)
            for offset in range(-height, width, max(4, width // 4)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(220, 158, 102, 255), width=1)
            return
        if "nose" in name or "mouth" in name or "paw" in name:
            draw.rectangle(rect, fill=(106, 65, 42, 255), outline=shadow + (255,), width=1)
            if "nose" in name or "mouth" in name:
                draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(56, 32, 24, 255), width=1)
            return
        if "ear" in name:
            draw.rectangle(rect, fill=(246, 213, 154, 255), outline=shadow + (255,), width=1)
            inner = (x1 + max(1, width // 4), y1 + max(1, height // 4), x2 - max(1, width // 4), y2 - max(1, height // 4))
            draw.rectangle(inner, fill=(232, 74, 95, 255))
            return
        if "snout" in name or "shoulder_fur" in name:
            draw.rectangle(rect, fill=(238, 190, 126, 255), outline=shadow + (255,), width=1)
            return

    if creature["id"] == "stone_lion_guardian":
        if "eye" in name:
            draw.rectangle(rect, fill=(218, 177, 58, 255), outline=(48, 42, 30, 255), width=1)
            return
        if "ball" in name:
            draw.rectangle(rect, fill=(176, 48, 42, 255), outline=(80, 24, 20, 255), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(224, 92, 75, 255), width=1)
            return
        if "fang" in name:
            draw.rectangle(rect, fill=(220, 215, 190, 255), outline=shadow + (255,), width=1)
            return
        if "nose" in name:
            draw.rectangle(rect, fill=(49, 45, 39, 255))
            draw.point((x1, y1), fill=(22, 20, 18, 255))
            draw.point((min(x2, x1 + 1), y1), fill=(105, 101, 88, 255))
            return
        if "claw" in name:
            draw.rectangle(rect, fill=(48, 45, 38, 255), outline=shadow + (255,), width=1)
            return
        if "mane" in name:
            draw.rectangle(rect, fill=(132, 130, 113, 255), outline=shadow + (255,), width=1)
            for offset in range(-height, width, max(4, width // 4)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(67, 63, 55, 255), width=1)
            return
        if "tail" in name or "paw" in name or "leg" in name or "body" in name or "stone" in name or "shoulder" in name or "plinth" in name or "head" in name or "snout" in name:
            draw.rectangle(rect, fill=(145, 140, 123, 255), outline=shadow + (255,), width=1)
            if width > 5 and height > 5:
                draw.line((x1 + 1, y1 + max(1, height // 3), x2 - 2, y2 - 2), fill=(70, 66, 58, 255), width=1)
                draw.line((x1 + max(1, width // 3), y1 + 1, x2 - 2, y1 + max(2, height // 2)), fill=(105, 101, 88, 255), width=1)
            return

    if creature["id"] != "jiangshi":
        return

    if "pale_face" in name:
        draw.rectangle(rect, fill=(169, 205, 197, 255), outline=shadow + (255,), width=1)
        return
    if "eye" in name or "mouth" in name:
        draw.rectangle(rect, fill=(15, 24, 24, 255), outline=shadow + (255,), width=1)
        return
    if "talisman_paper" in name:
        draw.rectangle(rect, fill=(226, 198, 72, 255), outline=(112, 77, 20, 255), width=1)
        return
    if "talisman_mark" in name:
        draw.rectangle(rect, fill=(167, 34, 24, 255), outline=(82, 15, 12, 255), width=1)
        draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(238, 83, 52, 255), width=1)
        return
    if "official_hat" in name or "hat_" in name:
        draw.rectangle(rect, fill=(18, 24, 23, 255), outline=(71, 91, 86, 255), width=1)
        draw.line((x1 + 1, y1 + 1, x2 - 2, y1 + max(1, height // 3)), fill=(72, 86, 82, 255), width=1)
        return
    if "robe" in name or "body" in name:
        draw.rectangle(rect, fill=(40, 74, 70, 255), outline=shadow + (255,), width=1)
        if width > 4 and height > 4:
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(77, 113, 105, 255), width=1)
        return
    if "cuff" in name or "arm" in name:
        draw.rectangle(rect, fill=(34, 60, 57, 255), outline=shadow + (255,), width=1)
        if "cuff" in name:
            draw.rectangle((x1, y1, x2, y2), outline=(181, 151, 60, 255), width=1)
        return
    if "boot" in name:
        draw.rectangle(rect, fill=(19, 18, 16, 255), outline=shadow + (255,), width=1)


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
    base.CREATURES = CHINESE_CREATURES
    base.decorate_texture_region = decorate_chinese_texture_region


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
    for creature in CHINESE_CREATURES:
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
        print("Chinese Creature Visual Gate: FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Chinese Creature Visual Gate: PASS")
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
