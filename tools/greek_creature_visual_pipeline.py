#!/usr/bin/env python3
"""Blockbench-only visual workflow generator and auditor for Greek creatures.

The gate validates single-creature Blockbench assets: design intent, .bbmodel
source, UV layout, attached runtime texture, textured orthographic previews, and
texture/model fit. Minecraft screenshots are intentionally out of scope.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import sys
import uuid
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
DESIGN_DIR = ROOT / "docs/美术资源/greek_creatures/design"
CONCEPT_DIR = ROOT / "docs/美术资源/greek_creatures/concepts"
BLOCKBENCH_DIR = ROOT / "docs/美术资源/blockbench/greek_creatures"
PREVIEW_DIR = ROOT / "docs/preview_assets/greek_creatures"
MANIFEST_PATH = ROOT / "docs/美术资源/greek_creatures/visual_manifest.json"
REPORT_PATH = PREVIEW_DIR / "visual_report.json"
TEXTURE_FIT_REPORT_PATH = PREVIEW_DIR / "texture_fit_report.json"
CONTACT_SHEET_PATH = PREVIEW_DIR / "blockbench_validation_contact_sheet.png"
CONCEPT_CONTACT_SHEET_PATH = PREVIEW_DIR / "concept_design_contact_sheet.png"

TEXTURE_ROOT = ROOT / "src/main/resources/assets/mythic_age/textures/entity"
MOD_ENTITIES_PATH = ROOT / "src/main/java/com/example/mythic_age/entity/ModEntities.java"

VIEWS = ("front", "side", "three_quarter")
VIEW_FACE = {"front": "north", "side": "east", "three_quarter": "north"}
VIEW_LABEL = {"front": "Front / 正面", "side": "Side / 侧面", "three_quarter": "Three-quarter / 45度"}
RESAMPLE_NEAREST = getattr(getattr(Image, "Resampling", Image), "NEAREST")
FONT_PATHS = (
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
)


CREATURES: list[dict[str, Any]] = [
    {
        "id": "minotaur",
        "entity_id": "mythic_age:minotaur",
        "display": "Minotaur / 米诺陶",
        "target_box": [1.35, 2.75],
        "visual_height": 2.9,
        "scale_note": "应明显高于玩家、宽肩厚胸，但低于独眼巨人；不能像普通僵尸换牛头。",
        "primary": "#6f3f22",
        "shadow": "#2b1a12",
        "accent": "#d6c08a",
        "silhouette": {
            "front": "巨大牛头、外展双角、宽肩人形躯干和战斧必须一眼可读。",
            "side": "牛吻前突、粗颈、厚胸、尾巴和长柄双刃斧形成冲锋怪轮廓。",
            "back": "宽背、牛尾、双角外廓和战斧背影仍能区分。",
        },
        "parts": [
            "head", "muzzle", "heavy_brow", "left_eye", "right_eye", "left_nostril", "right_nostril",
            "mouth", "left_horn", "right_horn", "left_ear", "right_ear", "neck",
            "body", "chest_harness", "left_chest_muscle", "right_chest_muscle", "waist_belt", "loincloth_front",
            "left_arm", "left_forearm", "left_fist", "left_finger_a", "left_finger_b",
            "right_arm", "right_forearm", "right_fist", "right_finger_a", "right_finger_b",
            "labrys_handle", "labrys_left_blade", "labrys_right_blade", "labrys_center_socket", "labrys_counterweight",
            "tail", "tail_tuft",
            "left_leg", "left_hoof", "left_hoof_claw",
            "right_leg", "right_hoof", "right_hoof_claw",
        ],
        "hierarchy": [
            "root/head/face",
            "root/body/chest_and_belt",
            "root/arms/hands",
            "root/right_arm/labrys_axe",
            "root/legs/hooves",
            "root/body/tail",
        ],
        "uv_plan": [
            "bull_head_front: heavy_brow, left_eye/right_eye, muzzle, nostrils and mouth fixed on front face",
            "horns/ears: pale outward horns and side ears exceeding head bounds",
            "torso_front: chest harness, left/right chest muscles, belt and loincloth",
            "hands: fists and fingers separated from arms",
            "hooves: each hoof and hoof claw uses dark UV strip",
            "labrys_axe: wooden handle, two metal blades, center socket and counterweight separated",
        ],
        "texture_detail": "深棕牛头与人身，浅色大角带渐变，黑色眉眼、鼻孔和嘴线固定在牛吻上；胸前皮革束带、胸肌阴影、腰带和前垂布分区；手指、蹄部和蹄尖深色；双刃斧用灰金属刃、深木柄和中心铆接。",
        "animation": "待机头部低压；行走时粗臂、手指和腿摆动；冲锋时头角前压、身体前倾，战斧保持侧面和 45 度可读轮廓。",
        "signature_features": [
            "牛头人身必须清楚，不能只是普通人形头部贴牛纹",
            "双角外展、牛吻前突、眉眼、鼻孔和嘴必须独立可读",
            "宽肩厚胸、胸肌、腰带、粗臂、手指、蹄足和牛尾表现强壮迷宫怪",
            "长柄双刃斧必须有左右斧刃、中心连接和尾端配重，不能只是一根棍子",
        ],
        "modeling_musts": [
            "head、muzzle、heavy_brow、left/right_eye、left/right_nostril、mouth、left/right_horn、left/right_ear 必须在正面可读",
            "labrys_axe 必须拆成 handle、left_blade、right_blade、center_socket、counterweight，侧面和 45 度不能被身体遮没",
            "胸肌、腰带、前垂布、手指、左右蹄和蹄尖必须独立建模或明确 UV",
            "碰撞盒目标约 1.35w x 2.75h，视觉体量明显大于玩家",
        ],
        "reject_if": [
            "看起来像普通僵尸或小型村民怪",
            "双角、牛吻、鼻孔、嘴眼或耳朵不清楚",
            "没有武器轮廓或武器细得不可读",
            "斧头没有左右刃或中心连接",
            "腿脚像普通人腿，没有蹄足和蹄尖",
        ],
        "v2_design_freeze": [
            "正面第一识别必须是牛头、外展双角、宽肩厚胸和长柄双刃斧，而不是普通人形怪。",
            "牛吻、眉眼、嘴线和双角必须分区建模/贴图；角的外轮廓要超过头部宽度。",
            "体量必须从旧版 0.9x2.1 提升为大型迷宫怪，约 1.35 格宽、2.75 格高，仍低于独眼巨人。",
            "头部主体必须匹配宽肩厚胸比例；牛头识别来自牛吻、眉眼、鼻孔、嘴线、耳朵和外展双角，不能靠异常大头解决。",
            "双刃斧必须有左右斧刃、中心套筒、长柄和配重；蹄足必须区别普通人脚。",
        ],
        "geometry": [
            ["torso", "body", [-6.0, -11.0, -4.0], [6.0, 5.0, 4.0], [40, 0]],
            ["chest_harness", "body", [-5.2, -9.0, -4.8], [5.2, 1.5, -3.8], [0, 30]],
            ["left_chest_muscle", "body", [0.8, -7.8, -5.2], [4.8, -2.2, -4.2], [24, 30]],
            ["right_chest_muscle", "body", [-4.8, -7.8, -5.2], [-0.8, -2.2, -4.2], [36, 30]],
            ["waist_belt", "body", [-5.6, 2.0, -4.8], [5.6, 4.2, -3.8], [0, 42]],
            ["loincloth_front", "body", [-2.4, 3.8, -5.0], [2.4, 10.5, -3.8], [24, 42]],
            ["neck", "body", [-3.2, -14.0, -3.2], [3.2, -8.0, 3.2], [0, 24]],
            ["head", "head", [-4.35, -22.2, -4.45], [4.35, -12.0, 4.25], [0, 0]],
            ["muzzle", "head", [-2.8, -16.6, -8.7], [2.8, -12.0, -4.45], [44, 0]],
            ["heavy_brow", "head", [-3.6, -18.75, -5.45], [3.6, -17.15, -4.35], [70, 0]],
            ["left_eye", "head", [-2.9, -17.65, -5.95], [-1.8, -16.55, -5.05], [106, 0]],
            ["right_eye", "head", [1.8, -17.65, -5.95], [2.9, -16.55, -5.05], [112, 0]],
            ["left_nostril", "head", [-1.7, -14.8, -9.25], [-0.7, -13.9, -8.35], [118, 0]],
            ["right_nostril", "head", [0.7, -14.8, -9.25], [1.7, -13.9, -8.35], [122, 0]],
            ["mouth", "head", [-2.0, -13.1, -9.15], [2.0, -12.2, -8.25], [106, 6]],
            ["left_horn", "head", [2.75, -26.7, -2.1], [7.75, -20.9, 2.1], [78, 0]],
            ["right_horn", "head", [-7.75, -26.7, -2.1], [-2.75, -20.9, 2.1], [100, 0]],
            ["left_ear", "head", [3.35, -20.9, -3.4], [5.35, -17.3, -0.9], [84, 14]],
            ["right_ear", "head", [-5.35, -20.9, -3.4], [-3.35, -17.3, -0.9], [94, 14]],
            ["left_arm", "arms", [4.8, -9.0, -3.2], [9.8, 3.8, 3.2], [68, 24]],
            ["left_forearm", "arms", [5.2, 3.4, -3.0], [10.0, 11.2, 3.0], [68, 48]],
            ["left_fist", "arms", [4.8, 10.4, -3.4], [10.4, 14.4, 3.4], [68, 62]],
            ["left_finger_a", "arms", [5.0, 13.4, -4.0], [6.8, 16.4, -2.0], [86, 62]],
            ["left_finger_b", "arms", [8.0, 13.4, -4.0], [9.8, 16.4, -2.0], [94, 62]],
            ["right_arm", "arms", [-9.8, -9.0, -3.2], [-4.8, 3.8, 3.2], [94, 24]],
            ["right_forearm", "arms", [-10.0, 3.4, -3.0], [-5.2, 11.2, 3.0], [94, 48]],
            ["right_fist", "arms", [-10.4, 10.4, -3.4], [-4.8, 14.4, 3.4], [102, 62]],
            ["right_finger_a", "arms", [-9.8, 13.4, -4.0], [-8.0, 16.4, -2.0], [112, 62]],
            ["right_finger_b", "arms", [-6.8, 13.4, -4.0], [-5.0, 16.4, -2.0], [120, 62]],
            ["tail", "body", [-1.0, -1.0, 3.2], [1.0, 12.0, 5.2], [0, 50]],
            ["left_leg", "legs", [1.4, 4.0, -3.0], [5.2, 22.0, 3.0], [0, 64]],
            ["right_leg", "legs", [-5.2, 4.0, -3.0], [-1.4, 22.0, 3.0], [20, 64]],
            ["left_hoof", "legs", [0.8, 20.0, -4.5], [5.6, 23.0, 3.5], [40, 64]],
            ["right_hoof", "legs", [-5.6, 20.0, -4.5], [-0.8, 23.0, 3.5], [64, 64]],
            ["left_hoof_claw", "legs", [0.5, 22.3, -5.7], [5.8, 23.7, -4.2], [40, 78]],
            ["right_hoof_claw", "legs", [-5.8, 22.3, -5.7], [-0.5, 23.7, -4.2], [64, 78]],
            ["labrys_handle", "weapon", [-13.0, -7.0, -1.0], [-11.0, 17.0, 1.0], [88, 64]],
            ["labrys_left_blade", "weapon", [-18.0, -10.0, -3.0], [-12.0, -4.0, 3.0], [74, 86]],
            ["labrys_right_blade", "weapon", [-12.0, -10.0, -3.0], [-6.0, -4.0, 3.0], [100, 86]],
            ["labrys_center_socket", "weapon", [-13.5, -9.0, -2.2], [-10.5, -5.0, 2.2], [88, 86]],
            ["labrys_counterweight", "weapon", [-13.8, 15.0, -2.0], [-10.2, 19.0, 2.0], [74, 104]],
        ],
    },
    {
        "id": "cyclops",
        "entity_id": "mythic_age:cyclops",
        "display": "Cyclops / 独眼巨人",
        "target_box": [1.8, 4.0],
        "visual_height": 4.15,
        "scale_note": "比玩家高约 2 倍，肩宽接近两个玩家，不能像普通僵尸。",
        "primary": "#7a6b5c",
        "shadow": "#3f342a",
        "accent": "#f0e0b0",
        "silhouette": {
            "front": "比例合理的独眼头脸、宽肩、粗臂、短粗腿形成高压迫感。",
            "side": "额头和鼻梁前突，胸腔厚，手臂垂到膝盖。",
            "back": "背部宽厚，双臂和短粗腿在背面仍有压迫感。",
        },
        "parts": [
            "head", "heavy_brow", "central_eye", "central_pupil", "nose_bridge", "nostrils", "mouth", "teeth", "ears",
            "torso", "chest_muscles", "waist_belt", "loincloth", "left_arm", "right_arm", "forearms", "fists", "fingers",
            "left_leg", "right_leg", "feet", "toes", "stone_club_handle", "stone_club_head", "stone_club_lumps", "stone_club_knob",
        ],
        "hierarchy": ["root/body", "root/head", "root/arms", "root/legs", "root/weapon", "root/weapon/stone_club"],
        "uv_plan": [
            "head_front: oversized single eye, dark pupil, brow, nose bridge, nostrils, mouth and teeth",
            "torso_front: broad chest muscles, waist belt and ragged loincloth",
            "arms: long muscular arms, fists and separated fingers",
            "legs: short heavy thighs, feet and separated toes",
            "weapon: stone club handle, large irregular stone head, side/top lumps and bottom knob",
        ],
        "texture_detail": "灰褐皮肤，中央大眼占头部正面核心区域并带深色瞳孔；眉骨、鼻孔、嘴线和牙齿必须高对比。胸肌用浅暗分区，腰带/兽皮为深皮革色；手指、脚趾和指甲独立；石棒必须是多块面粗糙灰石纹理并有裂纹。",
        "animation": "待机轻微呼吸；行走驱动双腿和长臂；攻击时石棒随主手大幅挥击，头部不变形。",
        "signature_features": [
            "巨大人形比例，宽肩厚胸，不是普通僵尸放大",
            "头部正面只有一个居中的大眼，必须有瞳孔、眉骨、鼻梁、鼻孔、嘴和牙",
            "单手持巨大粗糙石棒，石棒长度接近身体高度的一半以上，石块头高过肩线",
            "手臂粗长、拳头有手指，腿短而重、脚掌有脚趾，整体像古希腊巨人单位",
            "胸肌、腰带和兽皮裙提供人形身体结构，不能只是一个大矩形躯干",
        ],
        "modeling_musts": [
            "石棒必须作为独立 weapon group 建模并挂在右手侧",
            "眼睛、瞳孔、眉骨、鼻梁、鼻孔、嘴和牙必须是独立可读结构或明确 UV 区",
            "身体高度约 4 格，头部应匹配宽肩厚胸比例；单眼细节必须清楚，但不能用异常大头替代面部结构",
            "手指、脚趾、腰带、兽皮裙和不规则石棒块面必须进入 .bbmodel，而不是只靠贴图暗示",
        ],
        "reject_if": [
            "看起来只是无武器大人形",
            "单眼被纹理噪声淹没或没有嘴部结构",
            "石棒细得像棍子，或没有石质块面",
            "没有耳朵、牙、手指/脚趾或腰部服饰，导致像粗糙占位模型",
        ],
        "v2_design_freeze": [
            "正面第一识别必须是单眼巨人头脸、宽肩厚胸和右手巨大石棒，而不是盾牌状侧块。",
            "石棒由细长握柄、上端不规则巨石块和底部配重组成；巨石块必须高过肩线。",
            "头部高度约占整体视觉高度 25%，但不能用异常大头替代面部结构；中央眼必须占头部正面宽度约 35%-45%。",
            "面部必须包含瞳孔、鼻孔、牙齿和耳朵；四肢必须包含手指/脚趾，保证不是普通大号人形占位。",
            "躯干必须有人形胸肌、腰带和兽皮裙层级，支撑独眼巨人作为神话时代大型人形单位的身份。",
        ],
        "geometry": [
            ["torso", "body", [-8.5, -18.5, -4.4], [8.5, 3.0, 4.4], [0, 28]],
            ["chest_plate", "body", [-7.7, -17.5, -5.2], [7.7, -6.0, -4.1], [56, 28]],
            ["left_chest_muscle", "body", [0.8, -15.4, -5.5], [6.2, -8.8, -4.4], [94, 28]],
            ["right_chest_muscle", "body", [-6.2, -15.4, -5.5], [-0.8, -8.8, -4.4], [110, 28]],
            ["belly_shadow", "body", [-4.5, -5.8, -5.4], [4.5, -1.0, -4.3], [56, 44]],
            ["waist_belt", "body", [-7.2, -0.5, -5.6], [7.2, 2.0, -4.5], [80, 44]],
            ["loincloth_front", "body", [-3.8, 1.5, -5.7], [3.8, 7.8, -4.4], [0, 62]],
            ["loincloth_back", "body", [-3.8, 1.5, 4.4], [3.8, 7.2, 5.7], [20, 62]],
            ["head", "head", [-6.2, -12.5, -5.6], [6.2, 0.2, 5.6], [0, 0]],
            ["heavy_brow", "head", [-5.3, -8.7, -6.8], [5.3, -7.0, -5.5], [52, 0]],
            ["central_eye", "head", [-2.6, -7.5, -7.2], [2.6, -4.0, -5.8], [80, 0]],
            ["central_pupil", "head", [-0.7, -6.7, -7.4], [0.7, -5.1, -6.9], [98, 0]],
            ["nose_bridge", "head", [-1.4, -4.8, -6.8], [1.4, -1.2, -5.7], [106, 0]],
            ["left_nostril", "head", [-1.7, -2.0, -7.2], [-0.7, -1.2, -6.6], [118, 0]],
            ["right_nostril", "head", [0.7, -2.0, -7.2], [1.7, -1.2, -6.6], [122, 3]],
            ["mouth", "head", [-3.4, -1.4, -6.9], [3.4, -0.3, -5.8], [52, 6]],
            ["left_tooth", "head", [-2.2, -0.4, -7.0], [-1.2, 1.5, -6.1], [72, 6]],
            ["right_tooth", "head", [1.2, -0.4, -7.0], [2.2, 1.5, -6.1], [78, 6]],
            ["left_ear", "head", [5.0, -9.5, -2.8], [7.0, -5.0, 0.8], [84, 8]],
            ["right_ear", "head", [-7.0, -9.5, -2.8], [-5.0, -5.0, 0.8], [116, 8]],
            ["right_cheek_scar", "head", [2.8, -5.0, -7.0], [4.7, -4.4, -6.2], [100, 19]],
            ["left_arm", "arms", [6.0, -16.0, -3.8], [11.5, -2.0, 3.8], [40, 62]],
            ["left_forearm", "arms", [5.4, -2.8, -4.0], [11.1, 11.5, 4.0], [0, 82]],
            ["left_fist", "arms", [5.0, 9.8, -4.8], [11.6, 15.8, 4.8], [64, 82]],
            ["left_finger_a", "arms", [5.3, 14.7, -5.2], [6.6, 17.4, -3.4], [0, 106]],
            ["left_finger_b", "arms", [7.4, 14.8, -5.4], [8.7, 17.8, -3.4], [6, 106]],
            ["left_finger_c", "arms", [9.5, 14.7, -5.2], [10.8, 17.4, -3.4], [12, 106]],
            ["right_arm", "arms", [-11.5, -16.0, -3.8], [-6.0, -2.0, 3.8], [70, 62]],
            ["right_forearm", "arms", [-11.1, -2.8, -4.0], [-5.4, 11.5, 4.0], [32, 82]],
            ["right_fist", "arms", [-11.6, 9.8, -4.8], [-5.0, 15.8, 4.8], [94, 82]],
            ["right_finger_a", "arms", [-10.8, 14.7, -5.2], [-9.5, 17.4, -3.4], [18, 106]],
            ["right_finger_b", "arms", [-8.7, 14.8, -5.4], [-7.4, 17.8, -3.4], [24, 106]],
            ["right_finger_c", "arms", [-6.6, 14.7, -5.2], [-5.3, 17.4, -3.4], [30, 106]],
            ["left_thigh", "legs", [0.6, 3.0, -3.4], [6.0, 13.0, 3.4], [0, 110]],
            ["left_shin", "legs", [1.0, 12.5, -3.1], [5.8, 22.0, 3.1], [52, 108]],
            ["left_foot", "legs", [0.0, 20.0, -5.4], [6.8, 24.0, 4.0], [0, 92]],
            ["left_toe_a", "legs", [0.5, 23.0, -6.2], [1.9, 24.5, -4.4], [72, 92]],
            ["left_toe_b", "legs", [2.7, 23.0, -6.5], [4.1, 24.8, -4.4], [78, 92]],
            ["left_toe_c", "legs", [4.9, 23.0, -6.2], [6.3, 24.5, -4.4], [84, 92]],
            ["right_thigh", "legs", [-6.0, 3.0, -3.4], [-0.6, 13.0, 3.4], [26, 110]],
            ["right_shin", "legs", [-5.8, 12.5, -3.1], [-1.0, 22.0, 3.1], [78, 108]],
            ["right_foot", "legs", [-6.8, 20.0, -5.4], [0.0, 24.0, 4.0], [36, 92]],
            ["right_toe_a", "legs", [-6.3, 23.0, -6.2], [-4.9, 24.5, -4.4], [90, 92]],
            ["right_toe_b", "legs", [-4.1, 23.0, -6.5], [-2.7, 24.8, -4.4], [96, 92]],
            ["right_toe_c", "legs", [-1.9, 23.0, -6.2], [-0.5, 24.5, -4.4], [102, 92]],
            ["stone_club_handle", "weapon", [-14.2, -26.0, -1.1], [-11.6, 12.0, 1.1], [112, 52]],
            ["stone_club_head", "weapon", [-19.0, -33.0, -5.0], [-9.2, -15.5, 5.0], [88, 100]],
            ["stone_club_lump_top", "weapon", [-16.8, -37.5, -3.8], [-10.6, -31.7, 3.8], [82, 60]],
            ["stone_club_lump_side", "weapon", [-22.0, -29.0, -3.8], [-18.2, -19.0, 3.8], [82, 72]],
            ["stone_club_lump_back", "weapon", [-8.8, -26.2, 2.8], [-5.0, -17.2, 6.6], [104, 72]],
            ["stone_club_knob", "weapon", [-14.8, 11.0, -2.8], [-10.8, 16.2, 2.8], [104, 84]],
        ],
    },
    {
        "id": "centaur",
        "entity_id": "mythic_age:centaur",
        "display": "Centaur / 半人马",
        "target_box": [1.9, 3.0],
        "visual_height": 3.15,
        "scale_note": "必须比玩家更高、更长；马身承担主体体量，人形战士上身从前肩长出并高出马肩。",
        "primary": "#8b5a2b",
        "shadow": "#3f2614",
        "accent": "#d9b382",
        "silhouette": {
            "front": "人类战士上身、头脸、双肩和弓先被读到，四蹄左右分开支撑。",
            "side": "长马身、前置人身连接、背后箭袋、尾巴和竖向弓弦形成清楚轮廓。",
            "back": "马臀、尾巴、后腿、人身背部与箭袋分层清楚。",
        },
        "parts": [
            "horse_body", "horse_chest", "horse_rump", "horse_shoulder_blend",
            "human_waist", "human_torso", "human_shoulders", "human_chest_strap",
            "head", "left_eye", "right_eye", "nose_bridge", "mouth", "left_ear", "right_ear",
            "hair_crown", "hair_back", "mane_neck",
            "left_arm", "left_forearm", "left_hand", "right_arm", "right_forearm", "right_hand",
            "left_finger_a", "left_finger_b", "right_finger_a", "right_finger_b",
            "bow_upper", "bow_lower", "bow_grip", "bow_string", "arrow", "arrow_tip",
            "quiver", "quiver_strap", "quiver_arrow_a", "quiver_arrow_b",
            "tail", "tail_tuft",
            "front_left_leg", "front_left_hoof", "front_right_leg", "front_right_hoof",
            "back_left_leg", "back_left_hoof", "back_right_leg", "back_right_hoof",
        ],
        "hierarchy": [
            "root/horse_body/{horse_chest,horse_rump,horse_shoulder_blend,tail}",
            "root/human_torso/{human_waist,human_shoulders,human_chest_strap,head,arms,quiver}",
            "root/human_torso/head/{eyes,nose_bridge,mouth,ears,hair}",
            "root/human_torso/arms/{forearms,hands,fingers,bow,arrow}",
            "root/legs/{front_left,front_right,back_left,back_right}/{leg,hoof}",
        ],
        "uv_plan": [
            "human_head_front: left/right eyes, nose bridge, mouth and ears occupy fixed face UV islands",
            "hair_crown/hair_back/mane_neck: dark hair bands separate from skin and horse coat",
            "horse_body_side: warm brown coat with darker belly and shoulder shading",
            "equipment: bow limbs, bow string, arrow tip, quiver and diagonal strap each occupy named UV islands",
            "legs/hooves: every leg and hoof has its own dark hoof UV block; no shared front/back hoof strip",
        ],
        "texture_detail": "马身暖棕并带腹部/肩部阴影；人身为肤色，头脸固定双眼、鼻梁、嘴线和耳朵；深色头发从头顶延伸到颈后鬃毛；弓为深木色、弓弦近黑、箭头金属灰；箭袋与胸带为深皮革；四个马蹄独立深色块。",
        "animation": "马腿四足错相步态，四蹄随对应腿摆动；人身保持前置稳定；头部跟随视线；远程攻击时右手拉弦、左手稳弓、弓弦和箭发生轻微拉伸/显隐；尾巴持续轻摆。",
        "signature_features": [
            "马身加前置人形战士上身的清楚拼接，不允许像普通马背上长方块",
            "人形头脸必须有双眼、鼻梁、嘴线、耳朵和深色头发",
            "弓、弓弦、箭和背部箭袋共同构成半人马弓手识别点",
            "四条马腿、四个独立马蹄和尾巴要在正面/侧面都清楚读出",
            "人身腰部必须包在马肩前段，而不是坐在马背中央",
        ],
        "modeling_musts": [
            "horse_body、human_torso、head、bow、quiver、tail、四腿四蹄必须分组明确",
            "人形腰部必须从马肩前段长出，不能放在马身中央偏后",
            "头脸必须使用独立眼睛、鼻梁、嘴线、耳朵、头发部件，不允许只靠一个头部贴图暗示",
            "弓必须由 bow_upper、bow_lower、bow_grip、bow_string 组成，在侧面形成细长竖向轮廓",
            "四个 hoof 必须是独立部件，不能用 front_hooves/back_hooves 合并条带",
        ],
        "reject_if": [
            "看起来只是无头骑手或普通马",
            "人脸没有独立双眼、鼻梁和嘴线",
            "弓、箭、箭袋任一缺失导致战士身份不可读",
            "四足步态部件或四个马蹄无法从躯干区分",
        ],
        "v2_design_freeze": [
            "正面必须先读到人类战士上身、头脸、双肩和弓；马身只能作为下半身支撑，不得像马背方块。",
            "人身从马肩前段长出，腰部连接、胸肩、头脸和双臂必须独立可读，侧面弓弦形成清楚长弧轮廓。",
            "侧面必须显示长马身、四蹄步态、后尾、背部箭袋和前置人身连接，整体不是普通马或骑手。",
            "所有脸部、武器和四蹄细节必须是命名结构和固定 UV 区，不再依赖随机纹理面。",
        ],
        "geometry": [
            ["horse_body", "horse_body", [-6.4, -5.5, -13.0], [6.4, 4.8, 11.5], [0, 34]],
            ["horse_chest", "horse_body", [-5.0, -7.0, -15.0], [5.0, 1.5, -8.5], [78, 34]],
            ["horse_rump", "horse_body", [-5.8, -4.8, 7.5], [5.8, 4.8, 13.5], [78, 52]],
            ["horse_shoulder_blend", "horse_body", [-4.6, -8.4, -11.5], [4.6, -3.2, -5.4], [0, 72]],
            ["horse_belly_shadow", "horse_body", [-5.2, 3.7, -8.0], [5.2, 5.2, 6.5], [36, 72]],
            ["tail", "tail", [-1.2, -2.5, 10.5], [1.2, 7.5, 13.5], [96, 34]],
            ["tail_tuft", "tail", [-3.0, 5.5, 12.0], [3.0, 10.4, 16.0], [108, 34]],
            ["human_waist", "human_body", [-3.2, -8.0, -10.8], [3.2, -3.4, -5.8], [36, 0]],
            ["human_torso", "human_body", [-4.4, -16.5, -10.4], [4.4, -4.5, -5.0], [0, 0]],
            ["human_shoulders", "human_body", [-5.8, -15.8, -10.0], [5.8, -12.5, -4.8], [28, 0]],
            ["human_chest_strap", "human_body", [-4.6, -13.6, -10.8], [4.6, -12.4, -9.8], [64, 0]],
            ["mane_neck", "human_body", [-2.5, -16.2, -6.2], [2.5, -7.0, -4.8], [68, 8]],
            ["head", "head", [-3.2, -22.2, -13.2], [3.2, -15.4, -7.2], [62, 18]],
            ["nose_bridge", "head", [-1.5, -18.6, -14.2], [1.5, -16.1, -12.8], [90, 0]],
            ["left_eye", "head", [-2.4, -19.6, -13.9], [-1.25, -18.4, -12.95], [104, 0]],
            ["right_eye", "head", [1.25, -19.6, -13.9], [2.4, -18.4, -12.95], [110, 0]],
            ["mouth", "head", [-2.0, -16.7, -14.1], [2.0, -15.8, -13.2], [104, 6]],
            ["left_ear", "head", [2.9, -21.1, -10.7], [4.2, -18.0, -8.0], [116, 0]],
            ["right_ear", "head", [-4.2, -21.1, -10.7], [-2.9, -18.0, -8.0], [116, 8]],
            ["hair_crown", "head", [-3.9, -23.5, -10.8], [3.9, -21.4, -7.4], [90, 12]],
            ["hair_back", "head", [-4.0, -22.1, -7.8], [4.0, -15.3, -6.4], [90, 22]],
            ["left_arm", "arms", [4.6, -14.8, -9.5], [8.0, -5.4, -5.8], [0, 82]],
            ["left_forearm", "arms", [5.1, -7.0, -13.6], [7.8, 2.5, -9.8], [18, 82]],
            ["left_hand", "arms", [4.9, 1.4, -13.9], [8.0, 4.5, -10.4], [36, 82]],
            ["right_arm", "arms", [-8.0, -14.8, -9.5], [-4.6, -5.4, -5.8], [50, 82]],
            ["right_forearm", "arms", [-7.8, -7.3, -13.2], [-5.1, 2.3, -9.6], [68, 82]],
            ["right_hand", "arms", [-8.0, 1.4, -13.6], [-4.9, 4.5, -10.2], [86, 82]],
            ["left_finger_a", "arms", [5.0, 4.2, -14.2], [6.0, 6.0, -12.7], [104, 82]],
            ["left_finger_b", "arms", [6.5, 4.2, -14.1], [7.5, 6.0, -12.6], [110, 82]],
            ["right_finger_a", "arms", [-7.5, 4.2, -13.9], [-6.5, 6.0, -12.4], [116, 82]],
            ["right_finger_b", "arms", [-6.0, 4.2, -13.8], [-5.0, 6.0, -12.3], [122, 82]],
            ["bow_upper", "equipment", [6.8, -18.0, -15.0], [8.2, -6.0, -13.5], [100, 32]],
            ["bow_lower", "equipment", [6.8, -6.0, -15.0], [8.2, 6.8, -13.5], [108, 32]],
            ["bow_grip", "equipment", [6.2, -7.2, -14.8], [8.6, -4.8, -13.2], [116, 32]],
            ["bow_string", "equipment", [8.2, -17.7, -14.2], [8.8, 6.5, -13.8], [100, 50]],
            ["arrow", "equipment", [0.8, -7.6, -15.2], [8.5, -6.5, -14.5], [108, 50]],
            ["arrow_tip", "equipment", [8.3, -7.9, -15.6], [9.5, -6.2, -14.1], [108, 56]],
            ["quiver", "equipment", [-3.0, -13.5, -4.5], [0.0, -4.5, -2.2], [100, 60]],
            ["quiver_strap", "equipment", [-4.6, -12.0, -5.0], [4.4, -10.8, -4.0], [96, 74]],
            ["quiver_arrow_a", "equipment", [-2.6, -15.5, -4.1], [-1.6, -12.0, -2.8], [118, 60]],
            ["quiver_arrow_b", "equipment", [-1.3, -15.8, -4.1], [-0.3, -12.0, -2.8], [120, 66]],
            ["front_left_leg", "legs", [2.1, 0.0, -9.7], [4.8, 10.5, -6.4], [0, 96]],
            ["front_left_hoof", "legs", [1.7, 9.5, -10.5], [5.2, 12.1, -6.1], [16, 96]],
            ["front_right_leg", "legs", [-4.8, 0.0, -9.7], [-2.1, 10.5, -6.4], [34, 96]],
            ["front_right_hoof", "legs", [-5.2, 9.5, -10.5], [-1.7, 12.1, -6.1], [50, 96]],
            ["back_left_leg", "legs", [2.1, 0.0, 5.8], [5.1, 10.8, 9.4], [68, 96]],
            ["back_left_hoof", "legs", [1.7, 9.7, 5.0], [5.5, 12.1, 9.9], [84, 96]],
            ["back_right_leg", "legs", [-5.1, 0.0, 5.8], [-2.1, 10.8, 9.4], [102, 96]],
            ["back_right_hoof", "legs", [-5.5, 9.7, 5.0], [-1.7, 12.1, 9.9], [0, 114]],
        ],
    },
    {
        "id": "hydra",
        "entity_id": "mythic_age:hydra",
        "display": "Hydra / 九头蛇",
        "target_box": [3.2, 2.8],
        "visual_height": 3.0,
        "scale_note": "V3 当前未批准建模；目标是低伏长身、九头扇形错层和真实头-颈-肩连接，旧五头 V2 不再作为 1.0 建模依据。",
        "primary": "#234a2e",
        "shadow": "#122317",
        "accent": "#78b356",
        "silhouette": {
            "front": "V3 默认 9 个可读蛇头扇形展开，中央头最高，两侧头错层；如改 5 个主战头必须先做设计变更。",
            "side": "瘦长低伏身体、长尾、多个蛇颈从肩部前探，不能身子大头小。",
            "back": "背部鳞片、尾巴和 9 个颈部根部/错层颈影清楚分离，不糊成一团。",
        },
        "parts": [
            "slender_body", "shoulder_saddle", "neck_bridge_sockets", "neck_anchor_blocks", "spine_crests", "long_tail", "tail_tip",
            "nine_neck_sockets", "nine_independent_necks", "nine_neck_body_overlap_segments", "nine_neck_head_overlap_segments",
            "nine_proportional_heads", "head_brows", "paired_eyes",
            "upper_jaws", "lower_jaws", "mouth_lines", "fangs", "horns", "poison_glands",
            "legs", "paws",
        ],
        "hierarchy": ["root/body", "root/body/tail", "root/body/spine", "root/necks", "root/necks/heads", "root/heads/face_details", "root/legs"],
        "uv_plan": [
            "each_head_front: proportional head plane, paired yellow eyes, brow band, dark mouth line, white fangs, horns and poison glands",
            "neck: repeated scale bands and bright toxic throat ridge",
            "body_side: low slender swamp scales with shoulder saddle and visible neck_bridge sockets under every neck root",
            "tail_tip: dark taper and scale bands",
            "spine_crests: brighter toxic ridge plates from shoulder to tail",
        ],
        "texture_detail": "深绿鳞片，头部眼睛亮黄，口腔深色、獠牙白色、角/毒腺浅绿；每个头正面必须有眼睛、上下颚、嘴线、獠牙、角和毒腺。身体是低伏瘦长鳞片，不允许大块无细节深绿箱体。",
        "animation": "各头轻微错相摆动；行走时身体低伏；特殊攻击时中央头前探。",
        "signature_features": [
            "V3 默认显性 9 头作为九头蛇识别基础；每个头必须比例合理，细节靠独立部件和固定 UV 保证",
            "身体瘦长低伏，不能用一个臃肿大箱体撑满碰撞盒",
            "每个头具备眼睛、上下颚、嘴线、獠牙和小角/毒腺",
            "背部脊鳞从肩到尾形成沼泽毒兽轮廓",
            "九条长颈从肩部 socket 扇形展开，每条颈根必须通过 neck_root/body_overlap 连接到 shoulder_saddle，每个头根必须通过 head_overlap 连到对应颈段，不能悬空、糊成一根粗柱或只剩中心头",
        ],
        "modeling_musts": [
            "9 条 neck、9 个 neck_socket、9 个 body_overlap、9 个 head_overlap 和 9 个 head 必须是独立可审计结构，运行模型中头颈不得与身体断开",
            "头部宽度必须与身体、颈部和整体视觉高度匹配；中央头只允许略大，不能用大头代替细节",
            "body 高度压低，尾巴拉长，正面以头扇形而非躯干方块为主视觉",
            "每个 head 下必须有眼、上颚、下颚、牙、角/毒腺的明确子结构或同名 UV 区",
        ],
        "reject_if": [
            "只有 3 个头或头部太小",
            "头部过大、压倒身体比例，或用大头替代眼/嘴/颌/角等细节",
            "身体像圆胖蜥蜴，正面主要看到大躯干",
            "眼睛、嘴、獠牙缺失或贴图错位",
            "九头缺少上下颚/角/毒腺结构，导致像普通方块蛇头；或未经设计变更退回五头简化",
        ],
        "v2_design_freeze": [
            "旧五头 V2 和当前实现已被设计拒绝，不能继续作为 .bbmodel、Java model、贴图、动作或预览依据。",
            "V3 必须先经九头拓扑锁定表、细化设计板和用户签收；未签收前不得按本流水线继续生成最终模型。",
            "正面必须先读到九个比例合理的蛇头与长颈扇形；压迫感来自数量、错层和颈部姿态，不来自头部异常放大。",
            "九个头的脸部细节必须可审计：眼睛、上下颚、嘴线、獠牙、角或毒腺都不得只靠随机纹理暗示；每条颈根必须有 neck_root/body_overlap 与 shoulder_saddle 接触，每个头根必须有 head_overlap 与颈段接触。",
        ],
        "geometry": [
            ["body", "body", [-4.6, -1.2, -18], [4.6, 3.2, 14], [0, 38]],
            ["shoulder_saddle", "body", [-5.9, -4.0, -16], [5.9, -1.4, -6.5], [0, 76]],
            ["center_neck_bridge", "necks", [-2.9, -6.5, -18.7], [2.9, -2.5, -12.8], [92, 100]],
            ["left_neck_bridge", "necks", [1.4, -6.1, -17.9], [7.8, -2.6, -12.0], [40, 106]],
            ["right_neck_bridge", "necks", [-7.8, -6.1, -17.9], [-1.4, -2.6, -12.0], [66, 106]],
            ["outer_left_neck_bridge", "necks", [5.7, -5.5, -16.5], [12.1, -2.4, -11.3], [40, 116]],
            ["outer_right_neck_bridge", "necks", [-12.1, -5.5, -16.5], [-5.7, -2.4, -11.3], [66, 116]],
            ["center_neck_anchor", "necks", [-2.5, -5.2, -17.8], [2.5, -1.0, -13.6], [0, 116]],
            ["left_neck_anchor", "necks", [1.8, -5.0, -17.0], [7.6, -1.2, -13.0], [18, 116]],
            ["right_neck_anchor", "necks", [-7.6, -5.0, -17.0], [-1.8, -1.2, -13.0], [36, 116]],
            ["outer_left_neck_anchor", "necks", [5.4, -4.7, -15.8], [12.0, -1.3, -12.1], [54, 116]],
            ["outer_right_neck_anchor", "necks", [-12.0, -4.7, -15.8], [-5.4, -1.3, -12.1], [72, 116]],
            ["spine_crest_a", "body", [-0.9, -6.7, -14], [0.9, -4.1, -10], [0, 94]],
            ["spine_crest_b", "body", [-0.9, -6.2, -9], [0.9, -3.8, -5], [10, 94]],
            ["spine_crest_c", "body", [-0.9, -5.8, -4], [0.9, -3.4, 2], [20, 94]],
            ["tail_base", "tail", [-1.9, 0.0, 14], [1.9, 2.8, 33], [82, 52]],
            ["tail_mid", "tail", [-1.3, 0.2, 32], [1.3, 2.3, 47], [82, 76]],
            ["tail_tip", "tail", [-0.8, 0.4, 46], [0.8, 1.8, 58], [82, 94]],
            ["center_neck", "necks", [-1.45, -18.8, -16.2], [1.45, -1.8, -7.0], [64, 0]],
            ["left_neck", "necks", [3.8, -18.1, -15.7], [7.0, -1.9, -6.8], [64, 20]],
            ["right_neck", "necks", [-7.0, -18.1, -15.7], [-3.8, -1.9, -6.8], [64, 40]],
            ["outer_left_neck", "necks", [8.5, -16.4, -14.6], [11.4, -2.0, -6.4], [80, 0]],
            ["outer_right_neck", "necks", [-11.4, -16.4, -14.6], [-8.5, -2.0, -6.4], [80, 24]],
            ["center_neck_body_overlap", "necks", [-2.45, -4.6, -19.2], [2.45, 0.1, -12.6], [92, 110]],
            ["left_neck_body_overlap", "necks", [3.9, -4.3, -18.3], [8.8, 0.1, -11.9], [92, 110]],
            ["right_neck_body_overlap", "necks", [-8.8, -4.3, -18.3], [-3.9, 0.1, -11.9], [92, 110]],
            ["outer_left_neck_body_overlap", "necks", [8.5, -4.0, -17.0], [13.4, 0.2, -10.8], [92, 110]],
            ["outer_right_neck_body_overlap", "necks", [-13.4, -4.0, -17.0], [-8.5, 0.2, -10.8], [92, 110]],
            ["center_head_socket", "necks", [-3.0, -17.3, -10.2], [3.0, -14.3, -5.6], [104, 34]],
            ["left_head_socket", "necks", [2.7, -16.8, -9.8], [10.4, -13.8, -5.3], [88, 44]],
            ["right_head_socket", "necks", [-10.4, -16.8, -9.8], [-2.7, -13.8, -5.3], [88, 54]],
            ["outer_left_head_socket", "necks", [8.4, -15.4, -9.1], [15.0, -12.6, -4.9], [104, 64]],
            ["outer_right_head_socket", "necks", [-15.0, -15.4, -9.1], [-8.4, -12.6, -4.9], [104, 74]],
            ["center_neck_head_overlap", "necks", [-2.2, -17.0, -9.8], [2.2, -13.3, -4.2], [92, 118]],
            ["left_neck_head_overlap", "necks", [4.6, -16.4, -9.5], [9.0, -12.9, -4.0], [92, 118]],
            ["right_neck_head_overlap", "necks", [-9.0, -16.4, -9.5], [-4.6, -12.9, -4.0], [92, 118]],
            ["outer_left_neck_head_overlap", "necks", [9.7, -15.0, -8.9], [13.9, -11.8, -3.8], [92, 118]],
            ["outer_right_neck_head_overlap", "necks", [-13.9, -15.0, -8.9], [-9.7, -11.8, -3.8], [92, 118]],
            ["center_head", "heads", [-5.2, -24.2, -10.0], [5.2, -13.6, 0.7], [0, 0]],
            ["left_head", "heads", [2.5, -23.0, -9.5], [11.5, -13.2, 0.4], [28, 0]],
            ["right_head", "heads", [-11.5, -23.0, -9.5], [-2.5, -13.2, 0.4], [28, 20]],
            ["outer_left_head", "heads", [8.3, -21.0, -8.8], [15.9, -12.4, 0.1], [50, 0]],
            ["outer_right_head", "heads", [-15.9, -21.0, -8.8], [-8.3, -12.4, 0.1], [50, 20]],
            ["center_upper_jaw", "face_details", [-3.5, -16.2, -13.0], [3.5, -13.0, -7.9], [0, 18]],
            ["center_lower_jaw", "face_details", [-3.0, -13.0, -12.4], [3.0, -10.7, -7.9], [24, 18]],
            ["left_upper_jaw", "face_details", [3.5, -15.9, -12.3], [10.0, -12.8, -7.4], [0, 28]],
            ["left_lower_jaw", "face_details", [3.8, -12.8, -11.8], [9.7, -10.8, -7.4], [24, 28]],
            ["right_upper_jaw", "face_details", [-10.0, -15.9, -12.3], [-3.5, -12.8, -7.4], [0, 38]],
            ["right_lower_jaw", "face_details", [-9.7, -12.8, -11.8], [-3.8, -10.8, -7.4], [24, 38]],
            ["outer_left_upper_jaw", "face_details", [9.1, -15.0, -11.4], [14.5, -12.2, -6.9], [52, 28]],
            ["outer_left_lower_jaw", "face_details", [9.3, -12.2, -11.0], [14.2, -10.5, -6.9], [52, 48]],
            ["outer_right_upper_jaw", "face_details", [-14.5, -15.0, -11.4], [-9.1, -12.2, -6.9], [52, 38]],
            ["outer_right_lower_jaw", "face_details", [-14.2, -12.2, -11.0], [-9.3, -10.5, -6.9], [52, 58]],
            ["center_left_horn", "face_details", [-4.2, -26.6, -6.4], [-3.0, -23.8, -5.2], [88, 0]],
            ["center_right_horn", "face_details", [3.0, -26.6, -6.4], [4.2, -23.8, -5.2], [92, 0]],
            ["left_left_horn", "face_details", [3.8, -25.4, -6.1], [4.9, -22.8, -4.9], [88, 4]],
            ["left_right_horn", "face_details", [9.1, -25.4, -6.1], [10.2, -22.8, -4.9], [92, 4]],
            ["right_left_horn", "face_details", [-10.2, -25.4, -6.1], [-9.1, -22.8, -4.9], [88, 8]],
            ["right_right_horn", "face_details", [-4.9, -25.4, -6.1], [-3.8, -22.8, -4.9], [92, 8]],
            ["outer_left_horn", "face_details", [9.3, -23.3, -5.6], [10.3, -20.8, -4.5], [88, 12]],
            ["outer_left_second_horn", "face_details", [13.2, -23.3, -5.6], [14.2, -20.8, -4.5], [92, 12]],
            ["outer_right_horn", "face_details", [-14.2, -23.3, -5.6], [-13.2, -20.8, -4.5], [88, 16]],
            ["outer_right_second_horn", "face_details", [-10.3, -23.3, -5.6], [-9.3, -20.8, -4.5], [92, 16]],
            ["center_left_poison_gland", "face_details", [-6.2, -16.2, -7.0], [-5.0, -13.2, -4.7], [100, 0]],
            ["center_right_poison_gland", "face_details", [5.0, -16.2, -7.0], [6.2, -13.2, -4.7], [104, 0]],
            ["left_poison_gland", "face_details", [10.9, -15.9, -6.7], [12.2, -13.0, -4.5], [100, 4]],
            ["right_poison_gland", "face_details", [-12.2, -15.9, -6.7], [-10.9, -13.0, -4.5], [104, 4]],
            ["outer_left_poison_gland", "face_details", [15.1, -14.8, -6.2], [16.2, -12.2, -4.2], [100, 8]],
            ["outer_right_poison_gland", "face_details", [-16.2, -14.8, -6.2], [-15.1, -12.2, -4.2], [104, 8]],
            ["center_left_eye", "face_details", [-3.3, -19.4, -9.1], [-1.95, -17.9, -8.0], [64, 96]],
            ["center_right_eye", "face_details", [1.95, -19.4, -9.1], [3.3, -17.9, -8.0], [70, 96]],
            ["center_left_fang", "face_details", [-2.65, -11.0, -13.0], [-1.55, -8.9, -12.0], [76, 96]],
            ["center_right_fang", "face_details", [1.55, -11.0, -13.0], [2.65, -8.9, -12.0], [80, 96]],
            ["left_left_eye", "face_details", [4.4, -18.6, -8.7], [5.65, -17.25, -7.7], [64, 96]],
            ["left_right_eye", "face_details", [8.2, -18.6, -8.7], [9.45, -17.25, -7.7], [70, 96]],
            ["left_left_fang", "face_details", [4.9, -11.2, -12.2], [5.9, -9.5, -11.3], [76, 96]],
            ["left_right_fang", "face_details", [7.9, -11.2, -12.2], [8.9, -9.5, -11.3], [80, 96]],
            ["right_left_eye", "face_details", [-9.45, -18.6, -8.7], [-8.2, -17.25, -7.7], [64, 96]],
            ["right_right_eye", "face_details", [-5.65, -18.6, -8.7], [-4.4, -17.25, -7.7], [70, 96]],
            ["right_left_fang", "face_details", [-8.9, -11.2, -12.2], [-7.9, -9.5, -11.3], [76, 96]],
            ["right_right_fang", "face_details", [-5.9, -11.2, -12.2], [-4.9, -9.5, -11.3], [80, 96]],
            ["outer_left_eye", "face_details", [9.7, -17.0, -8.0], [10.85, -15.8, -7.05], [64, 96]],
            ["outer_left_second_eye", "face_details", [12.85, -17.0, -8.0], [14.0, -15.8, -7.05], [70, 96]],
            ["outer_left_fang", "face_details", [10.1, -11.0, -11.4], [11.05, -9.5, -10.55], [76, 96]],
            ["outer_left_second_fang", "face_details", [12.5, -11.0, -11.4], [13.45, -9.5, -10.55], [80, 96]],
            ["outer_right_eye", "face_details", [-14.0, -17.0, -8.0], [-12.85, -15.8, -7.05], [64, 96]],
            ["outer_right_second_eye", "face_details", [-10.85, -17.0, -8.0], [-9.7, -15.8, -7.05], [70, 96]],
            ["outer_right_fang", "face_details", [-13.45, -11.0, -11.4], [-12.5, -9.5, -10.55], [76, 96]],
            ["outer_right_second_fang", "face_details", [-11.05, -11.0, -11.4], [-10.1, -9.5, -10.55], [80, 96]],
            ["front_left_leg", "legs", [3.7, 2.5, -9], [6.9, 10.5, -4.5], [54, 76]],
            ["front_right_leg", "legs", [-6.9, 2.5, -9], [-3.7, 10.5, -4.5], [70, 76]],
            ["back_left_leg", "legs", [3.9, 2.5, 5], [7.1, 10.5, 10.0], [104, 94]],
            ["back_right_leg", "legs", [-7.1, 2.5, 5], [-3.9, 10.5, 10.0], [110, 94]],
        ],
    },
    {
        "id": "medusa",
        "entity_id": "mythic_age:medusa",
        "display": "Medusa / 美杜莎",
        "target_box": [1.1, 2.55],
        "visual_height": 2.7,
        "scale_note": "必须略高于玩家；蛇发冠和盘绕蛇尾扩大轮廓，基础头脸保持人形比例，不能像普通绿色人形也不能靠大头识别。",
        "primary": "#2c5b46",
        "shadow": "#153428",
        "accent": "#b8b8b8",
        "silhouette": {
            "front": "石化凝视双眼、女性头脸、多条蛇发冠和人形上身先被读到，下方转入蛇尾。",
            "side": "蛇发束离开头部轮廓，尾巴盘绕并向后滑出，身体不是普通双腿。",
            "back": "后蛇发、背部发束和蛇尾鳞片在背面仍能分层，不只是绿色柱体。",
        },
        "parts": [
            "head", "face_plate", "stone_gaze_aura", "left_eye", "right_eye", "left_pupil", "right_pupil",
            "nose_bridge", "mouth", "left_ear", "right_ear",
            "hair_crown", "hair_back",
            "front_snake_body", "front_snake_head", "front_snake_eyes", "front_snake_fangs", "front_snake_tongue",
            "left_snake_body", "left_snake_head", "left_snake_eyes", "left_snake_fangs", "left_snake_tongue",
            "right_snake_body", "right_snake_head", "right_snake_eyes", "right_snake_fangs", "right_snake_tongue",
            "back_snake_body", "back_snake_head", "back_snake_eyes", "back_snake_fangs", "back_snake_tongue",
            "left_outer_snake_body", "left_outer_snake_head", "left_outer_snake_eyes", "left_outer_snake_fangs", "left_outer_snake_tongue",
            "right_outer_snake_body", "right_outer_snake_head", "right_outer_snake_eyes", "right_outer_snake_fangs", "right_outer_snake_tongue",
            "torso", "robe_front", "waist_sash", "left_arm", "left_forearm", "left_hand", "right_arm", "right_forearm", "right_hand",
            "coiled_tail", "tail_base", "tail_mid", "tail_coil_outer", "tail_coil_inner", "tail_tip", "tail_tip_spike",
        ],
        "hierarchy": [
            "root/torso/{robe_front,waist_sash,shoulders,arms}",
            "root/head/{face_plate,stone_gaze_aura,eyes,pupils,nose_bridge,mouth,ears}",
            "root/head/snake_hair/{hair_crown,hair_back,six_snake_bodies}",
            "root/head/snake_hair/<snake>/{snake_head,eyes,fangs,tongue}",
            "root/coiled_tail/{tail_base,tail_mid,tail_coil_outer,tail_coil_inner,tail_tip,tail_tip_spike}",
        ],
        "uv_plan": [
            "face_front: pale face plate, high-contrast left/right eyes, dark pupils, nose bridge, mouth and stone gaze aura use fixed UV islands",
            "six snake heads: each snake head has explicit yellow eyes, ivory fangs and red tongue islands; snakes are not generic hair strips",
            "snake bodies/hair crown: dark green banded scales with lighter ridge lines",
            "torso/robe: gray-green robe plate, waist sash and pale arms separated from snake tail",
            "coiled_tail: tail_base, tail_mid, outer coil, inner coil, tail_tip and spike carry repeated scale bands",
        ],
        "texture_detail": "脸部偏灰白，石化凝视眼睛为高对比浅绿/白色并有深色瞳孔；六条蛇发为深绿鳞片，每个蛇头有黄眼、白牙和红舌；躯干袍甲为灰绿，腰带偏暗；蛇尾使用连续鳞片条纹和更暗的尾尖。",
        "animation": "六条蛇发错相摆动，蛇头端点轻微点头；尾部滑行动画带盘绕摆动；石化凝视/攻击时头部稳定面向目标、石化眼光层显示、前蛇发压低。",
        "signature_features": [
            "人形上身、女性头脸、蛇发冠、蛇形下身必须同时可读",
            "蛇发不是普通头发贴图，必须有六条独立蛇束和六个蛇头端点",
            "脸部必须有石化凝视双眼、瞳孔、鼻梁和嘴，眼部需要强对比；头块宽度必须服从躯干比例",
            "每个蛇头必须至少具备眼、牙、舌的固定 UV 细节",
            "下半身蛇尾要有盘绕/滑行趋势，而不是普通双腿或单根柱体",
        ],
        "modeling_musts": [
            "head、face_details、snake_hair、torso、arms、coiled_tail 分组固定；head 基础体积不能替代蛇发和五官细节",
            "蛇发束必须为 front、left、right、back、left_outer、right_outer 六条，正面和侧面都能看见",
            "每条蛇发必须拆出 body、head、eyes、fangs、tongue，不能只用一条管子代表",
            "尾部必须拆出 base、mid、outer coil、inner coil、tip、tip spike，鳞片 UV 必须有连续条纹",
        ],
        "reject_if": [
            "看起来只是绿色人形",
            "蛇发与头部糊成一块，或缺少六个蛇头端点",
            "脸部五官、瞳孔或石化凝视层不可读",
            "蛇头没有眼、牙、舌，导致像普通发束",
            "下半身读成双腿或单根尾柱",
        ],
        "v2_design_freeze": [
            "正面必须先读到石化凝视眼睛、女性头脸和多条蛇发冠；不能只是绿色人形，也不能用过大的头块代替蛇发分束。",
            "蛇发至少六束，前、左、右、背部都要有蛇头端点，侧面能看出蛇束离开头部轮廓。",
            "下半身必须是盘绕蛇尾/滑行蛇身，不能读成普通双腿或单根柱体。",
            "每个蛇头端点必须有固定 UV 的眼、牙、舌细节；测试只验证这些设计是否落地，不负责临场补造型。",
        ],
        "geometry": [
            ["torso", "body", [-4.2, 0.0, -2.6], [4.2, 11.4, 2.6], [0, 18]],
            ["robe_front", "body", [-4.8, 1.0, -3.2], [4.8, 8.0, -2.1], [28, 18]],
            ["waist_sash", "body", [-4.5, 8.0, -3.2], [4.5, 10.0, -2.1], [54, 18]],
            ["left_shoulder", "arms", [3.7, 1.0, -2.4], [5.4, 3.2, 2.4], [72, 18]],
            ["right_shoulder", "arms", [-5.4, 1.0, -2.4], [-3.7, 3.2, 2.4], [82, 18]],
            ["left_arm", "arms", [4.6, 3.0, -1.7], [6.7, 13.0, 1.7], [48, 28]],
            ["left_forearm", "arms", [4.2, 11.0, -1.5], [6.4, 18.0, 1.5], [60, 28]],
            ["left_hand", "arms", [4.0, 17.0, -1.6], [6.5, 19.4, 1.6], [72, 28]],
            ["right_arm", "arms", [-6.7, 3.0, -1.7], [-4.6, 13.0, 1.7], [84, 28]],
            ["right_forearm", "arms", [-6.4, 11.0, -1.5], [-4.2, 18.0, 1.5], [96, 28]],
            ["right_hand", "arms", [-6.5, 17.0, -1.6], [-4.0, 19.4, 1.6], [108, 28]],
            ["head", "head", [-3.05, -7.2, -3.25], [3.05, 0.2, 3.25], [0, 0]],
            ["face_plate", "face_details", [-2.65, -6.1, -3.75], [2.65, -1.0, -3.05], [30, 0]],
            ["left_eye", "face_details", [-2.05, -4.9, -4.05], [-0.95, -3.65, -3.15], [104, 0]],
            ["right_eye", "face_details", [0.95, -4.9, -4.05], [2.05, -3.65, -3.15], [110, 0]],
            ["left_pupil", "face_details", [-1.7, -4.55, -4.3], [-1.28, -4.0, -3.95], [118, 0]],
            ["right_pupil", "face_details", [1.28, -4.55, -4.3], [1.7, -4.0, -3.95], [122, 0]],
            ["stone_gaze_aura", "face_details", [-2.65, -5.2, -4.35], [2.65, -3.3, -4.1], [104, 12]],
            ["mouth", "face_details", [-1.55, -2.15, -4.05], [1.55, -1.25, -3.15], [104, 6]],
            ["nose_bridge", "face_details", [-0.5, -3.65, -3.95], [0.5, -2.0, -3.1], [116, 4]],
            ["left_ear", "head", [2.35, -6.0, -0.65], [3.45, -3.2, 1.5], [118, 8]],
            ["right_ear", "head", [-3.45, -6.0, -0.65], [-2.35, -3.2, 1.5], [116, 15]],
            ["hair_crown", "snake_hair", [-4.6, -9.0, -3.4], [4.6, -6.1, 3.4], [28, 0]],
            ["hair_back", "snake_hair", [-4.0, -7.3, 2.8], [4.0, 0.8, 4.2], [62, 0]],
            ["front_snake_body", "snake_hair", [-1.0, -9.8, -5.6], [1.0, -0.4, -3.6], [72, 0]],
            ["front_snake_head", "snake_hair", [-1.35, -1.2, -6.0], [1.35, 0.8, -3.4], [100, 10]],
            ["front_snake_left_eye", "snake_face", [-0.8, -0.45, -6.2], [-0.3, 0.05, -5.8], [82, 24]],
            ["front_snake_right_eye", "snake_face", [0.3, -0.45, -6.2], [0.8, 0.05, -5.8], [88, 24]],
            ["front_snake_left_fang", "snake_face", [-0.75, 0.35, -6.15], [-0.15, 1.15, -5.75], [94, 24]],
            ["front_snake_right_fang", "snake_face", [0.15, 0.35, -6.15], [0.75, 1.15, -5.75], [94, 24]],
            ["front_snake_tongue", "snake_face", [-0.25, 0.75, -6.4], [0.25, 1.9, -6.0], [100, 24]],
            ["left_snake_body", "snake_hair", [-6.2, -9.2, -1.0], [-4.2, 0.0, 1.0], [56, 0]],
            ["left_snake_head", "snake_hair", [-6.6, -0.9, -1.3], [-3.8, 1.0, 1.3], [80, 0]],
            ["left_snake_left_eye", "snake_face", [-6.35, -0.15, -1.45], [-5.85, 0.35, -1.05], [82, 24]],
            ["left_snake_right_eye", "snake_face", [-4.55, -0.15, -1.45], [-4.05, 0.35, -1.05], [88, 24]],
            ["left_snake_left_fang", "snake_face", [-6.1, 0.55, -1.45], [-5.5, 1.35, -1.05], [94, 24]],
            ["left_snake_right_fang", "snake_face", [-4.7, 0.55, -1.45], [-4.1, 1.35, -1.05], [94, 24]],
            ["left_snake_tongue", "snake_face", [-5.35, 1.0, -1.75], [-4.85, 2.1, -1.35], [100, 24]],
            ["right_snake_body", "snake_hair", [4.2, -9.2, -1.0], [6.2, 0.0, 1.0], [64, 0]],
            ["right_snake_head", "snake_hair", [3.8, -0.9, -1.3], [6.6, 1.0, 1.3], [90, 0]],
            ["right_snake_left_eye", "snake_face", [4.05, -0.15, -1.45], [4.55, 0.35, -1.05], [82, 24]],
            ["right_snake_right_eye", "snake_face", [5.85, -0.15, -1.45], [6.35, 0.35, -1.05], [88, 24]],
            ["right_snake_left_fang", "snake_face", [4.1, 0.55, -1.45], [4.7, 1.35, -1.05], [94, 24]],
            ["right_snake_right_fang", "snake_face", [5.5, 0.55, -1.45], [6.1, 1.35, -1.05], [94, 24]],
            ["right_snake_tongue", "snake_face", [4.85, 1.0, -1.75], [5.35, 2.1, -1.35], [100, 24]],
            ["back_snake_body", "snake_hair", [-1.0, -9.2, 3.3], [1.0, -0.2, 5.3], [72, 12]],
            ["back_snake_head", "snake_hair", [-1.35, -1.0, 3.0], [1.35, 0.9, 5.7], [110, 10]],
            ["back_snake_left_eye", "snake_face", [-0.8, -0.25, 5.55], [-0.3, 0.25, 5.95], [82, 24]],
            ["back_snake_right_eye", "snake_face", [0.3, -0.25, 5.55], [0.8, 0.25, 5.95], [88, 24]],
            ["back_snake_left_fang", "snake_face", [-0.75, 0.5, 5.5], [-0.15, 1.3, 5.9], [94, 24]],
            ["back_snake_right_fang", "snake_face", [0.15, 0.5, 5.5], [0.75, 1.3, 5.9], [94, 24]],
            ["back_snake_tongue", "snake_face", [-0.25, 0.95, 5.85], [0.25, 2.05, 6.25], [100, 24]],
            ["left_outer_snake_body", "snake_hair", [-8.1, -8.5, -0.8], [-6.3, 0.9, 0.8], [56, 12]],
            ["left_outer_snake_head", "snake_hair", [-8.6, -0.2, -1.2], [-5.8, 1.6, 1.2], [82, 12]],
            ["left_outer_snake_left_eye", "snake_face", [-8.35, 0.45, -1.35], [-7.85, 0.95, -0.95], [82, 24]],
            ["left_outer_snake_right_eye", "snake_face", [-6.55, 0.45, -1.35], [-6.05, 0.95, -0.95], [88, 24]],
            ["left_outer_snake_left_fang", "snake_face", [-8.05, 1.1, -1.35], [-7.45, 1.85, -0.95], [94, 24]],
            ["left_outer_snake_right_fang", "snake_face", [-6.7, 1.1, -1.35], [-6.1, 1.85, -0.95], [94, 24]],
            ["left_outer_snake_tongue", "snake_face", [-7.4, 1.45, -1.65], [-6.9, 2.55, -1.25], [100, 24]],
            ["right_outer_snake_body", "snake_hair", [6.3, -8.5, -0.8], [8.1, 0.9, 0.8], [64, 12]],
            ["right_outer_snake_head", "snake_hair", [5.8, -0.2, -1.2], [8.6, 1.6, 1.2], [92, 12]],
            ["right_outer_snake_left_eye", "snake_face", [6.05, 0.45, -1.35], [6.55, 0.95, -0.95], [82, 24]],
            ["right_outer_snake_right_eye", "snake_face", [7.85, 0.45, -1.35], [8.35, 0.95, -0.95], [88, 24]],
            ["right_outer_snake_left_fang", "snake_face", [6.1, 1.1, -1.35], [6.7, 1.85, -0.95], [94, 24]],
            ["right_outer_snake_right_fang", "snake_face", [7.45, 1.1, -1.35], [8.05, 1.85, -0.95], [94, 24]],
            ["right_outer_snake_tongue", "snake_face", [6.9, 1.45, -1.65], [7.4, 2.55, -1.25], [100, 24]],
            ["tail_base", "coiled_tail", [-3.7, 11.0, -3.2], [3.7, 19.5, 3.2], [0, 40]],
            ["tail_mid", "coiled_tail", [-2.9, 18.0, -2.6], [2.9, 25.0, 2.6], [32, 40]],
            ["tail_coil_outer", "coiled_tail", [-5.1, 23.5, -2.3], [5.1, 27.2, 2.3], [56, 40]],
            ["tail_coil_inner", "coiled_tail", [-2.8, 25.8, -1.8], [2.8, 28.7, 1.8], [90, 40]],
            ["tail_tip", "coiled_tail", [-1.5, 25.8, -3.2], [1.5, 34.2, -0.2], [0, 56]],
            ["tail_tip_spike", "coiled_tail", [-0.8, 33.5, -3.6], [0.8, 36.0, -2.0], [16, 56]],
        ],
    },
    {
        "id": "nemean_lion",
        "entity_id": "mythic_age:nemean_lion",
        "display": "Nemean Lion / 尼米亚狮",
        "target_box": [1.9, 1.8],
        "visual_height": 1.9,
        "scale_note": "要像超大型狮子，比狼和普通马更厚重；头脸与身体匹配，识别来自放射鬃毛、短吻、眼、獠牙和厚爪。",
        "primary": "#c89032",
        "shadow": "#65461d",
        "accent": "#f3d27a",
        "silhouette": {
            "front": "大鬃毛包围脸，肩部厚。",
            "side": "低伏强壮狮身，尾端毛球可见。",
            "back": "背部、尾端毛球和后腿体块可读。",
        },
        "parts": [
            "body", "shoulder", "chest_mass", "back_armor_ridge",
            "head", "snout", "nose_mouth", "left_eye", "right_eye", "left_fang", "right_fang",
            "mane_core", "mane_top", "mane_bottom", "mane_left", "mane_right",
            "mane_outer_top", "mane_outer_left", "mane_outer_right", "mane_chin",
            "tail", "tail_tuft_core", "tail_tuft_top", "tail_tuft_bottom",
            "front_left_leg", "front_left_paw", "front_left_claws",
            "front_right_leg", "front_right_paw", "front_right_claws",
            "back_left_leg", "back_left_paw", "back_left_claws",
            "back_right_leg", "back_right_paw", "back_right_claws",
        ],
        "hierarchy": [
            "root/body",
            "root/body/shoulder",
            "root/head/face",
            "root/mane/radial_layers",
            "root/tail/tuft",
            "root/legs/paws_and_claws",
        ],
        "uv_plan": [
            "mane_*: bright radial mane slabs surrounding and exceeding head bounds",
            "face_front: left_eye/right_eye/nose_mouth/snout/left_fang/right_fang fixed on front face",
            "body/shoulder/chest_mass/back_armor_ridge: heavy tawny body with invulnerable golden ridge accents",
            "front/back paws: each paw and claw strip gets distinct dark UV",
            "tail_tuft_*: separate tuft core/top/bottom so side view reads as lion tail, not plain rod",
        ],
        "texture_detail": "金棕身体，鬃毛使用更亮金色和放射纹，肩背有深阴影和硬质金色脊线；眼睛深色带金色高光，吻部浅色并有鼻口线，左右獠牙浅色，四足爪尖深色，尾端毛球分层。",
        "animation": "行走四足步态；扑击时身体前倾且前爪下压；鬃毛作为整体随头部轻微跟随但外层保持放射轮廓；尾端毛球随尾部摆动。",
        "signature_features": [
            "超大型狮子，不是狼/猫科普通小体型",
            "巨大放射状金色鬃毛必须超过头部边界并包住比例合理的头脸",
            "强壮肩背、胸前厚块和背部硬质金色脊线体现不可伤神狮",
            "短吻部、鼻口线、双眼、左右獠牙和每只爪尖都必须可读",
            "尾端毛球必须分层存在，侧面不能像普通细尾",
        ],
        "modeling_musts": [
            "mane_core/top/bottom/left/right/outer_* 必须比 head 更大，形成正面第一识别点；head 本体不能做成大头替代鬃毛",
            "left_eye/right_eye、left_fang/right_fang、nose_mouth 必须独立 ModelPart",
            "front/back paws 和 claws 必须拆到每只脚，不能合并成一条横杆",
            "shoulder、chest_mass、back_armor_ridge 必须让前半身比腰背更厚重",
            "tail_tuft_core/top/bottom 必须独立分块",
        ],
        "reject_if": [
            "看起来像普通狮子或狼",
            "没有大鬃毛或尾端毛球",
            "爪、眼、吻部或左右獠牙不可读",
            "前爪不厚或四爪没有深色爪尖",
            "鬃毛没有外层放射轮廓",
        ],
        "v2_design_freeze": [
            "正面第一识别必须是超大放射鬃毛包围脸，而不是方块黄兽或普通狼形。",
            "肩背和前爪必须厚重，短吻部、眼睛、獠牙、爪尖都要在正面和 45 度可读。",
            "尾端毛球和鬃毛边缘必须独立分块，侧面能读到大型狮身而非单一长方体。",
            "背部硬质金色脊线表现神话中的不可伤皮毛，但不能替代狮身结构。",
        ],
        "geometry": [
            ["body", "body", [-6.0, -7.4, -11.8], [6.0, 3.0, 9.8], [0, 28]],
            ["shoulder", "body", [-5.5, -9.6, -9.4], [5.5, -5.1, 2.8], [60, 32]],
            ["chest_mass", "body", [-5.2, -8.8, -12.8], [5.2, 0.8, -6.4], [0, 90]],
            ["back_armor_ridge", "body", [-1.1, -10.8, -8.0], [1.1, -8.8, 6.5], [72, 90]],
            ["head", "head", [-4.55, -6.3, -8.0], [4.55, 2.1, 1.2], [0, 0]],
            ["snout", "head", [-3.0, -1.2, -11.2], [3.0, 2.2, -6.7], [34, 0]],
            ["nose_mouth", "head", [-2.0, 0.45, -11.8], [2.0, 2.0, -10.6], [56, 0]],
            ["left_eye", "head", [-3.15, -4.1, -8.35], [-1.95, -2.95, -7.4], [108, 0]],
            ["right_eye", "head", [1.95, -4.1, -8.35], [3.15, -2.95, -7.4], [114, 0]],
            ["left_fang", "head", [-2.3, 1.55, -11.7], [-1.2, 3.65, -10.6], [108, 6]],
            ["right_fang", "head", [1.2, 1.55, -11.7], [2.3, 3.65, -10.6], [114, 6]],
            ["mane_core", "mane", [-7.8, -10.2, -6.4], [7.8, 4.2, 2.9], [54, 0]],
            ["mane_top", "mane", [-6.0, -12.8, -5.4], [6.0, -8.4, 2.0], [88, 0]],
            ["mane_bottom", "mane", [-6.2, 2.8, -4.4], [6.2, 7.6, 2.6], [84, 20]],
            ["mane_left", "mane", [-10.0, -6.9, -5.0], [-6.8, 3.2, 2.6], [56, 18]],
            ["mane_right", "mane", [6.8, -6.9, -5.0], [10.0, 3.2, 2.6], [70, 18]],
            ["mane_outer_top", "mane", [-4.8, -15.0, -4.4], [4.8, -12.4, 1.6], [0, 106]],
            ["mane_outer_left", "mane", [-12.0, -5.8, -4.4], [-9.6, 2.6, 1.8], [24, 106]],
            ["mane_outer_right", "mane", [9.6, -5.8, -4.4], [12.0, 2.6, 1.8], [42, 106]],
            ["mane_chin", "mane", [-4.8, 6.8, -3.8], [4.8, 9.8, 1.8], [60, 106]],
            ["tail", "tail", [-1.0, -1.4, 0.0], [1.0, 1.4, 12.8], [70, 54]],
            ["tail_tuft_core", "tail", [-2.9, -3.1, 11.0], [2.9, 3.1, 16.4], [98, 54]],
            ["tail_tuft_top", "tail", [-2.1, -4.4, 11.8], [2.1, -2.7, 15.6], [98, 68]],
            ["tail_tuft_bottom", "tail", [-2.1, 2.7, 11.8], [2.1, 4.4, 15.6], [104, 74]],
            ["front_left_leg", "legs", [1.1, 0.0, -8.7], [5.7, 11.2, -4.0], [0, 60]],
            ["front_left_paw", "legs", [0.5, 9.2, -11.0], [6.0, 12.1, -5.2], [36, 60]],
            ["front_left_claws", "legs", [0.3, 10.7, -12.2], [6.2, 12.3, -10.6], [92, 12]],
            ["front_right_leg", "legs", [-5.7, 0.0, -8.7], [-1.1, 11.2, -4.0], [18, 60]],
            ["front_right_paw", "legs", [-6.0, 9.2, -11.0], [-0.5, 12.1, -5.2], [58, 60]],
            ["front_right_claws", "legs", [-6.2, 10.7, -12.2], [-0.3, 12.3, -10.6], [92, 18]],
            ["back_left_leg", "legs", [1.4, 0.0, 5.0], [6.0, 11.0, 9.9], [0, 74]],
            ["back_left_paw", "legs", [1.0, 9.0, 4.4], [6.2, 12.0, 10.2], [36, 76]],
            ["back_left_claws", "legs", [0.8, 10.5, 3.3], [6.4, 12.1, 4.9], [92, 24]],
            ["back_right_leg", "legs", [-6.0, 0.0, 5.0], [-1.4, 11.0, 9.9], [18, 74]],
            ["back_right_paw", "legs", [-6.2, 9.0, 4.4], [-1.0, 12.0, 10.2], [58, 76]],
            ["back_right_claws", "legs", [-6.4, 10.5, 3.3], [-0.8, 12.1, 4.9], [92, 30]],
        ],
    },
    {
        "id": "manticore",
        "entity_id": "mythic_age:manticore",
        "display": "Manticore / 蝎尾狮",
        "target_box": [2.0, 2.05],
        "visual_height": 2.2,
        "scale_note": "狮身应比尼米亚狮略长，翅膀和蝎尾扩大整体轮廓。",
        "primary": "#8b3e3a",
        "shadow": "#2f242a",
        "accent": "#407f4a",
        "silhouette": {
            "front": "兽头、展开翅膀、宽胸。",
            "side": "长蝎尾高高翘起，尾刺明显。",
            "back": "翅膀、蝎尾和尾刺从背面清楚分离。",
        },
        "parts": [
            "body", "chest", "shoulder_ridge",
            "head", "snout", "nose_mouth", "left_eye", "right_eye",
            "left_fang", "right_fang", "left_ear", "right_ear", "left_cheek_spike", "right_cheek_spike",
            "left_wing_membrane_a", "left_wing_membrane_b", "left_wing_bone_front",
            "left_wing_bone_mid", "left_wing_bone_outer", "left_wing_claw",
            "right_wing_membrane_a", "right_wing_membrane_b", "right_wing_bone_front",
            "right_wing_bone_mid", "right_wing_bone_outer", "right_wing_claw",
            "tail_base", "tail_rise", "tail_curve", "tail_hook", "tail_sting", "tail_sting_tip",
            "front_left_leg", "front_left_claws", "front_right_leg", "front_right_claws",
            "back_left_leg", "back_left_claws", "back_right_leg", "back_right_claws",
        ],
        "hierarchy": [
            "root/body",
            "root/body/chest",
            "root/head/face",
            "root/wings/left_wing",
            "root/wings/right_wing",
            "root/tail/scorpion_segments",
            "root/legs/claws",
        ],
        "uv_plan": [
            "face_front: left_eye/right_eye/nose_mouth/left_fang/right_fang fixed around snout",
            "ears/left_cheek_spike/right_cheek_spike: side silhouette accents around the head",
            "wing_membrane_*: dark poison-green membrane panels",
            "wing_bone_* and wing_claw: dark bone struts and hooked tips separated from membrane",
            "tail_base/tail_rise/tail_curve/tail_hook: raised segmented scorpion tail arc",
            "tail_sting/tail_sting_tip: poison green sting and bright venom tip",
            "leg/claws: red fur legs with dark claw UV strips",
        ],
        "texture_detail": "暗红狮身，胸肩有更深阴影；头部必须固定金黄眼、黑鼻口线和浅色獠牙；翼膜为暗毒绿并带分段纹理，翼骨/翼爪更深；蝎尾分节上扬，毒刺和毒刺尖使用亮绿色；四足爪尖深色。",
        "animation": "翼膜随左右翼小幅拍动但翼骨保持分段；蝎尾分节待机摆动，特殊攻击时 tail_hook 与 tail_sting 明显前压；头脸随视角转动，耳朵轻微抖动。",
        "signature_features": [
            "狮身主体、分段膜翼、上扬蝎尾毒刺必须三者同时明显",
            "脸部必须有双眼、鼻口线、左右獠牙、耳朵和左右 cheek_spike，不能只是普通兽头，也不能靠大头读凶险感",
            "蝎尾必须由 base/rise/curve/hook/sting/tip 形成高过背线的钩状轮廓",
            "翼膜、翼骨、翼爪必须分层，不能用单块平板代表翅膀",
            "暗红兽身和绿色毒性翼膜/尾刺形成清楚对比",
        ],
        "modeling_musts": [
            "left/right_wing 必须拆出 membrane_a、membrane_b、bone_front、bone_mid、bone_outer、wing_claw",
            "tail_base、tail_rise、tail_curve、tail_hook、tail_sting、tail_sting_tip 必须连续组成蝎尾",
            "head 必须拆出 snout、nose_mouth、left/right_eye、left/right_fang、left/right_ear、cheek_spikes；基础头宽不得接近躯干宽度",
            "front/back legs 必须有 claws，避免四足落地像无特征红色方块",
        ],
        "reject_if": [
            "没有翼或翼像贴片",
            "尾刺不明显或没有毒性颜色",
            "头脸没有眼睛、鼻口线或左右獠牙",
            "蝎尾没有分节上扬或 hook/sting/tip 关系不清",
            "翅膀缺少翼骨/翼膜/翼爪分层",
        ],
        "v2_design_freeze": [
            "正面和 45 度必须同时读到兽脸獠牙、展开膜翼和上扬蝎尾毒刺，三者缺一即返工。",
            "蝎尾必须高过背线形成钩状轮廓，不能是一条水平尾巴；毒刺用绿色/毒性强调色独立分块。",
            "翅膀必须有骨架、翼爪和膜翼分段，不能用单条平板横条替代。",
            "头脸必须有双眼、鼻口线、左右獠牙和耳侧尖刺，让它比普通狮兽更凶险。",
        ],
        "geometry": [
            ["body", "body", [-5.7, -7.4, -11.0], [5.7, 2.4, 9.8], [0, 30]],
            ["chest", "body", [-4.8, -8.8, -10.0], [4.8, -6.8, -4.0], [50, 30]],
            ["shoulder_ridge", "body", [-4.7, -9.3, -8.2], [4.7, -6.2, 3.8], [58, 38]],
            ["head", "head", [-4.2, -5.8, -7.0], [4.2, 2.1, 1.4], [0, 0]],
            ["snout", "head", [-3.05, -1.1, -10.2], [3.05, 2.3, -5.7], [30, 0]],
            ["nose_mouth", "head", [-2.05, 0.5, -10.7], [2.05, 2.05, -9.6], [52, 0]],
            ["left_eye", "head", [-3.05, -4.0, -7.45], [-1.9, -2.85, -6.5], [112, 0]],
            ["right_eye", "head", [1.9, -4.0, -7.45], [3.05, -2.85, -6.5], [118, 0]],
            ["left_fang", "head", [-2.3, 1.55, -10.7], [-1.25, 3.7, -9.65], [112, 6]],
            ["right_fang", "head", [1.25, 1.55, -10.7], [2.3, 3.7, -9.65], [118, 6]],
            ["left_ear", "head", [2.8, -8.2, -4.05], [4.8, -4.4, -1.85], [64, 0]],
            ["right_ear", "head", [-4.8, -8.2, -4.05], [-2.8, -4.4, -1.85], [72, 0]],
            ["left_cheek_spike", "head", [3.9, -2.35, -6.0], [5.7, -0.8, -4.4], [80, 0]],
            ["right_cheek_spike", "head", [-5.7, -2.35, -6.0], [-3.9, -0.8, -4.4], [86, 0]],
            ["left_wing_membrane_a", "wings", [0.0, -5.2, -5.4], [14.8, -3.3, 4.8], [68, 0]],
            ["left_wing_membrane_b", "wings", [7.2, -3.7, 2.8], [20.0, -2.0, 12.0], [68, 16]],
            ["left_wing_bone_front", "wings", [0.0, -6.0, -6.0], [19.0, -4.4, -4.0], [0, 92]],
            ["left_wing_bone_mid", "wings", [5.0, -5.2, -4.2], [6.5, -3.0, 11.0], [40, 92]],
            ["left_wing_bone_outer", "wings", [12.2, -4.6, -3.2], [13.7, -2.3, 12.0], [48, 92]],
            ["left_wing_claw", "wings", [19.0, -4.4, 10.0], [21.0, -2.0, 12.2], [58, 92]],
            ["right_wing_membrane_a", "wings", [-14.8, -5.2, -5.4], [0.0, -3.3, 4.8], [68, 32]],
            ["right_wing_membrane_b", "wings", [-20.0, -3.7, 2.8], [-7.2, -2.0, 12.0], [68, 48]],
            ["right_wing_bone_front", "wings", [-19.0, -6.0, -6.0], [0.0, -4.4, -4.0], [0, 98]],
            ["right_wing_bone_mid", "wings", [-6.5, -5.2, -4.2], [-5.0, -3.0, 11.0], [40, 98]],
            ["right_wing_bone_outer", "wings", [-13.7, -4.6, -3.2], [-12.2, -2.3, 12.0], [48, 98]],
            ["right_wing_claw", "wings", [-21.0, -4.4, 10.0], [-19.0, -2.0, 12.2], [58, 98]],
            ["tail_base", "tail", [-1.3, -2.0, 8.0], [1.3, 0.8, 18.0], [70, 50]],
            ["tail_rise", "tail", [-1.2, -11.5, 15.5], [1.2, -1.4, 18.4], [82, 50]],
            ["tail_curve", "tail", [-1.5, -13.3, 11.4], [1.5, -10.0, 18.2], [94, 50]],
            ["tail_hook", "tail", [-1.6, -15.0, 9.8], [1.6, -12.0, 13.8], [106, 50]],
            ["tail_sting", "tail", [-2.4, -16.3, 8.6], [2.4, -12.4, 12.8], [104, 58]],
            ["tail_sting_tip", "tail", [-1.0, -15.3, 6.2], [1.0, -13.1, 9.0], [116, 58]],
            ["front_left_leg", "legs", [1.1, 0.0, -7.7], [5.3, 10.5, -3.2], [0, 60]],
            ["front_left_claws", "legs", [0.8, 9.2, -8.6], [5.6, 10.8, -6.5], [0, 78]],
            ["front_right_leg", "legs", [-5.3, 0.0, -7.7], [-1.1, 10.5, -3.2], [18, 60]],
            ["front_right_claws", "legs", [-5.6, 9.2, -8.6], [-0.8, 10.8, -6.5], [18, 78]],
            ["back_left_leg", "legs", [1.3, 0.0, 5.0], [5.5, 10.5, 9.5], [36, 60]],
            ["back_left_claws", "legs", [1.0, 9.2, 4.0], [5.8, 10.8, 6.1], [36, 78]],
            ["back_right_leg", "legs", [-5.5, 0.0, 5.0], [-1.3, 10.5, 9.5], [54, 60]],
            ["back_right_claws", "legs", [-5.8, 9.2, 4.0], [-1.0, 10.8, 6.1], [54, 78]],
        ],
    },
    {
        "id": "chimera",
        "entity_id": "mythic_age:chimera",
        "display": "Chimera / 奇美拉",
        "target_box": [2.3, 2.25],
        "visual_height": 2.4,
        "scale_note": "必须是大型混合兽，多头和背部翼/火焰轮廓比普通四足兽更复杂。",
        "primary": "#4a2c22",
        "shadow": "#2a1d19",
        "accent": "#d65a31",
        "silhouette": {
            "front": "狮头主脸，侧面羊头和蛇尾形成复合轮廓。",
            "side": "长身、羊头、蛇尾和背部翼/鬃线可见。",
            "back": "翼、蛇尾和多头后侧连接关系清楚。",
        },
        "parts": [
            "body", "chest", "shoulder_fire_crest",
            "lion_head", "lion_mane_core", "lion_mane_top", "lion_mane_left", "lion_mane_right",
            "lion_snout", "lion_nose_mouth", "lion_left_eye", "lion_right_eye", "lion_left_fang", "lion_right_fang",
            "goat_neck", "goat_head", "goat_snout", "goat_left_eye", "goat_right_eye",
            "goat_left_horn", "goat_right_horn", "goat_beard",
            "tail_base", "tail_rise", "tail_tip", "serpent_head", "serpent_upper_jaw", "serpent_lower_jaw",
            "serpent_left_eye", "serpent_right_eye", "snake_tongue",
            "left_wing_membrane", "left_wing_bone", "left_wing_claw",
            "right_wing_membrane", "right_wing_bone", "right_wing_claw",
            "front_left_leg", "front_left_claws", "front_right_leg", "front_right_claws",
            "back_left_leg", "back_left_claws", "back_right_leg", "back_right_claws",
        ],
        "hierarchy": [
            "root/body",
            "root/body/chest",
            "root/body/shoulder_fire_crest",
            "root/heads/lion",
            "root/heads/goat",
            "root/tail/serpent",
            "root/wings/left_wing",
            "root/wings/right_wing",
            "root/legs/paws",
        ],
        "uv_plan": [
            "lion_head_front: lion_left_eye/lion_right_eye/lion_nose_mouth/lion_fangs fixed on front face",
            "lion_mane_*: radial dark mane slabs around the lion head",
            "goat_head/goat_snout: separate pale goat face with goat_left_eye/goat_right_eye and cream horns",
            "serpent_head/serpent_*_jaw: dark green snake face with yellow eyes and forked tongue",
            "body/chest/shoulder_fire_crest: scorched brown body with ember ridge",
            "wing_membrane/wing_bone/wing_claw: ember membrane, dark bones, hooked claws",
            "leg/claws: paws and claws use distinct dark nail UV strips",
        ],
        "texture_detail": "深棕狮身，暗鬃毛包住主狮头；狮眼金黄、鼻口深色、獠牙浅色；羊头偏灰褐且有浅色角和胡须；蛇尾头深绿，蛇眼金黄，上下颚和红舌分离；翼膜带火焰橙纹，爪尖深色。",
        "animation": "狮头作为主攻击方向随视角转动；羊头较小幅度错相摆动；蛇尾随尾部甩动并独立张颚吐舌；翼只小幅开合保持轮廓稳定。",
        "signature_features": [
            "狮头主脸、侧探羊头、尾端蛇头三重身份必须同时可读",
            "狮头必须有鬃毛、双眼、鼻口线和獠牙，不能只是普通方块兽头",
            "羊头必须有独立颈部、鼻吻、双眼、双角和胡须，不能贴成身体侧面小块",
            "蛇尾必须从尾段过渡到蛇头，并有上下颚、眼睛和舌头",
            "翼膜/翼骨/翼爪和肩部火焰脊形成火性轮廓，但不能遮住三头结构",
        ],
        "modeling_musts": [
            "lion_head、goat_head、serpent_head 必须独立且分别有面部细节 ModelPart",
            "lion_mane_core/top/left/right 必须包住狮头形成主脸优先级",
            "goat_left_horn/goat_right_horn、goat_beard、goat_snout 必须突出侧头身份",
            "tail_base/tail_rise/tail_tip 必须连接到 serpent_head，不能让蛇头像单独漂浮部件",
            "left/right_wing 必须拆出 membrane、bone、claw，翼膜颜色区别主体",
        ],
        "reject_if": [
            "只有一个兽头，看不出复合怪",
            "狮头没有明确眼睛、鼻口或獠牙",
            "羊角、羊胡须或羊眼缺失",
            "蛇尾没有上下颚、蛇眼或舌头",
            "翼部只是一整块色块，缺少翼骨/翼爪分层",
        ],
        "v2_design_freeze": [
            "三重身份必须同时可读：狮头是主攻击脸，羊头带角和胡须从侧面探出，尾端蛇头从尾段自然过渡。",
            "狮头不得退化成普通四足兽头；必须有鬃毛环、眼睛、鼻口线和獠牙。",
            "羊头不得贴成小方块，角、眼睛、鼻吻和胡须要突出；蛇头必须有上下颚、双眼和舌头。",
            "背部翼/火焰脊强调火性轮廓，但不能遮住三头结构或让模型变成单纯飞兽。",
        ],
        "geometry": [
            ["body", "body", [-6.8, -8.2, -12.0], [6.8, 3.2, 10.8], [0, 32]],
            ["chest", "body", [-5.4, -10.0, -11.0], [5.4, -7.6, -3.8], [74, 32]],
            ["shoulder_fire_crest", "body", [-1.0, -13.0, -8.0], [1.0, -8.0, 5.4], [74, 44]],
            ["lion_head", "heads", [-5.2, -6.3, -7.8], [5.2, 2.8, 1.6], [0, 0]],
            ["lion_mane_core", "heads", [-6.8, -8.4, -7.0], [6.8, 4.8, -2.4], [42, 0]],
            ["lion_mane_top", "heads", [-5.6, -10.8, -6.3], [5.6, -7.4, -2.4], [80, 0]],
            ["lion_mane_left", "heads", [-8.3, -6.5, -5.9], [-5.3, 3.8, -2.7], [112, 0]],
            ["lion_mane_right", "heads", [5.3, -6.5, -5.9], [8.3, 3.8, -2.7], [112, 16]],
            ["lion_snout", "heads", [-3.4, -1.1, -10.8], [3.4, 2.7, -6.1], [80, 9]],
            ["lion_nose_mouth", "heads", [-2.3, 0.6, -11.3], [2.3, 2.2, -10.2], [104, 16]],
            ["lion_left_eye", "heads", [-3.7, -4.1, -8.3], [-2.4, -3.0, -7.2], [104, 0]],
            ["lion_right_eye", "heads", [2.4, -4.1, -8.3], [3.7, -3.0, -7.2], [112, 0]],
            ["lion_left_fang", "heads", [-2.5, 1.8, -10.9], [-1.4, 4.1, -9.8], [104, 5]],
            ["lion_right_fang", "heads", [1.4, 1.8, -10.9], [2.5, 4.1, -9.8], [110, 5]],
            ["goat_neck", "heads", [3.8, -5.2, -4.0], [6.5, 0.8, -0.5], [58, 20]],
            ["goat_head", "heads", [5.5, -6.7, -6.6], [11.6, 1.2, 0.6], [58, 0]],
            ["goat_snout", "heads", [7.0, -2.2, -9.8], [11.3, 1.2, -6.2], [88, 8]],
            ["goat_left_eye", "heads", [6.2, -4.3, -7.2], [7.3, -3.2, -6.2], [106, 9]],
            ["goat_right_eye", "heads", [9.8, -4.3, -7.2], [10.9, -3.2, -6.2], [112, 9]],
            ["goat_left_horn", "heads", [4.6, -11.2, -5.8], [6.5, -5.8, -3.7], [88, 0]],
            ["goat_right_horn", "heads", [10.4, -11.2, -5.8], [12.3, -5.8, -3.7], [98, 0]],
            ["goat_beard", "heads", [7.2, 0.8, -8.9], [10.2, 4.8, -6.2], [106, 20]],
            ["tail_base", "tail", [-1.4, -1.5, 8.0], [1.4, 1.0, 17.0], [80, 68]],
            ["tail_rise", "tail", [-1.2, -6.0, 14.6], [1.2, -1.0, 17.6], [104, 68]],
            ["tail_tip", "tail", [-0.9, -5.6, 17.0], [0.9, -2.4, 20.2], [116, 68]],
            ["serpent_head", "tail", [-2.8, -5.1, 19.4], [2.8, -0.4, 25.7], [80, 80]],
            ["serpent_upper_jaw", "tail", [-2.2, -4.0, 24.8], [2.2, -2.1, 28.0], [106, 80]],
            ["serpent_lower_jaw", "tail", [-2.0, -1.0, 24.2], [2.0, 1.1, 27.4], [106, 86]],
            ["serpent_left_eye", "tail", [-2.1, -4.2, 23.8], [-1.0, -3.0, 24.8], [122, 80]],
            ["serpent_right_eye", "tail", [1.0, -4.2, 23.8], [2.1, -3.0, 24.8], [122, 84]],
            ["snake_tongue", "tail", [-0.4, -2.1, 27.5], [0.4, -1.3, 31.0], [112, 88]],
            ["left_wing_membrane", "wings", [0.0, -5.0, -5.5], [18.5, -2.7, 9.2], [0, 92]],
            ["left_wing_bone", "wings", [0.0, -5.8, -6.2], [19.5, -4.2, -4.4], [68, 92]],
            ["left_wing_claw", "wings", [18.2, -6.2, -4.8], [20.6, -3.8, -2.8], [112, 94]],
            ["right_wing_membrane", "wings", [-18.5, -5.0, -5.5], [0.0, -2.7, 9.2], [0, 110]],
            ["right_wing_bone", "wings", [-19.5, -5.8, -6.2], [0.0, -4.2, -4.4], [68, 98]],
            ["right_wing_claw", "wings", [-20.6, -6.2, -4.8], [-18.2, -3.8, -2.8], [112, 100]],
            ["front_left_leg", "legs", [1.8, 0.0, -8.8], [6.1, 11.2, -4.0], [0, 68]],
            ["front_left_claws", "legs", [1.5, 10.0, -9.8], [6.4, 11.4, -7.8], [0, 86]],
            ["front_right_leg", "legs", [-6.1, 0.0, -8.8], [-1.8, 11.2, -4.0], [20, 68]],
            ["front_right_claws", "legs", [-6.4, 10.0, -9.8], [-1.5, 11.4, -7.8], [16, 86]],
            ["back_left_leg", "legs", [1.8, 0.0, 5.0], [6.5, 11.2, 9.8], [40, 68]],
            ["back_left_claws", "legs", [1.5, 10.0, 3.8], [6.7, 11.4, 5.8], [32, 86]],
            ["back_right_leg", "legs", [-6.5, 0.0, 5.0], [-1.8, 11.2, 9.8], [60, 68]],
            ["back_right_claws", "legs", [-6.7, 10.0, 3.8], [-1.5, 11.4, 5.8], [48, 86]],
        ],
    },
    {
        "id": "colossus",
        "entity_id": "mythic_age:colossus",
        "display": "Colossus / 青铜巨像",
        "target_box": [1.7, 4.2],
        "visual_height": 4.5,
        "scale_note": "已具备大体量，重点是青铜/神像质感和面部结构正确。",
        "primary": "#7b5a3a",
        "shadow": "#362b27",
        "accent": "#2dba9d",
        "silhouette": {
            "front": "巨大神像头冠、发光眼缝、面甲、宽肩、分层胸甲和柱状腿必须一眼可读。",
            "side": "厚重胸腔、前后胸甲厚度、巨大拳头和大脚像可移动青铜神像。",
            "back": "背部青铜板、肩臂、肘膝关节缝和柱状腿在背面仍有神像感。",
        },
        "parts": [
            "head", "face_mask", "eye_slit", "mouth_groove", "left_cheek_plate", "right_cheek_plate",
            "crown_band", "crown_center_spike", "crown_spike_left", "crown_spike_right",
            "torso_core", "chest_plate_upper", "chest_plate_lower", "chest_center_gem",
            "left_chest_seam", "right_chest_seam", "back_plate", "bronze_belt",
            "left_shoulder_plate", "right_shoulder_plate", "left_arm", "left_elbow_ring", "left_forearm",
            "left_fist", "left_finger_a", "left_finger_b", "right_arm", "right_elbow_ring",
            "right_forearm", "right_fist", "right_finger_a", "right_finger_b",
            "left_leg", "left_knee_ring", "left_shin_plate", "left_foot", "left_toe_plate",
            "right_leg", "right_knee_ring", "right_shin_plate", "right_foot", "right_toe_plate",
            "oxidized_cracks",
        ],
        "hierarchy": [
            "root/head/{face_mask,eye_slit,mouth_groove,cheek_plates,crown_band,crown_spikes}",
            "root/body/{torso_core,chest_plates,chest_center_gem,chest_seams,back_plate,bronze_belt}",
            "root/body/shoulders/{left_shoulder_plate,right_shoulder_plate}",
            "root/arms/{upper_arm,elbow_ring,forearm,fist,fingers}",
            "root/legs/{leg,knee_ring,shin_plate,foot,toe_plate}",
        ],
        "uv_plan": [
            "face_front: face mask, glowing eye slit, mouth groove, cheek plates and crown spikes occupy fixed islands",
            "torso_front: upper/lower chest plates, center gem and left/right seams separate large bronze surfaces",
            "body_back: back plate and oxidized cracks prevent blank rear silhouette",
            "arms: upper arm, elbow ring, forearm, fist and fingers use separate bronze/verdigris UV regions",
            "legs: knee rings, shin plates, massive feet and toe plates use separate dark joint/bronze UV regions",
        ],
        "texture_detail": "青铜主体分深浅板块；发光眼缝为青绿色白高光；面甲和嘴缝深色；胸甲上下板、中心宝石、肩甲、肘膝环、手指、脚趾均有深裂缝和少量铜绿，不允许大面积空白铜板。",
        "animation": "步态缓慢重；行走时腿和手臂摆幅小但惯性重；攻击时右臂大幅砸下、左臂平衡，胸体微前倾；死亡时整体倾倒且头冠随身体下沉。",
        "signature_features": [
            "青铜神像巨人，体量显著高于其他生物",
            "头冠、面甲、发光眼缝、嘴缝和颊板必须明显，不允许脸部空白铜块",
            "胸甲上板/下板、中心宝石、左右板缝和背板共同形成神像铠甲",
            "手臂和腿像雕像柱体，但必须有肘环、膝环、拳头、手指、大脚和脚趾板",
            "铜绿只作为关节/缝隙点缀，不改变青铜主体",
        ],
        "modeling_musts": [
            "head、crown、face_mask、eye_slit、torso_core、chest_plates、arms、legs 分组明确",
            "脸部必须有眼缝、嘴缝和左右颊板，不是空白铜块",
            "胸甲必须拆出 upper/lower/center_gem/left_seam/right_seam/back_plate",
            "每条手臂必须拆出 elbow_ring、forearm、fist、finger_a、finger_b",
            "每条腿必须拆出 knee_ring、shin_plate、foot、toe_plate",
        ],
        "reject_if": [
            "像普通石巨人而非青铜神像",
            "脸部没有眼缝、嘴缝、面甲或冠饰",
            "躯干大片空白无胸甲板缝/背板",
            "手脚没有拳头、手指、大脚和脚趾板导致像普通柱状肢体",
        ],
        "v2_design_freeze": [
            "正面必须读到青铜神像巨人、发光眼缝、冠饰和胸甲板缝，而不是普通棕色巨人。",
            "躯干和四肢必须有青铜板、腰带、关节缝与铜绿点缀，不能大面积空白色块。",
            "体量仍作为希腊最高压迫感单位，视觉高度约 4.5 格，脚掌和柱状腿要支撑神像重量。",
            "脸、胸、手、脚的细节必须是命名结构和固定 UV 区，不再只靠通用铜色纹理暗示。",
        ],
        "geometry": [
            ["torso_core", "body", [-9.2, -8.0, -5.2], [9.2, 18.0, 5.2], [42, 12]],
            ["chest_plate_upper", "armor", [-8.2, -7.2, -6.0], [8.2, -0.2, -4.8], [0, 34]],
            ["chest_plate_lower", "armor", [-7.2, -0.2, -5.9], [7.2, 6.4, -4.8], [0, 48]],
            ["chest_center_gem", "armor", [-1.6, -3.4, -6.3], [1.6, 1.0, -5.8], [54, 34]],
            ["left_chest_seam", "armor", [6.8, -6.8, -6.2], [8.1, 6.0, -5.3], [62, 34]],
            ["right_chest_seam", "armor", [-8.1, -6.8, -6.2], [-6.8, 6.0, -5.3], [68, 34]],
            ["back_plate", "armor", [-7.8, -5.8, 4.8], [7.8, 8.8, 6.0], [74, 32]],
            ["bronze_belt", "armor", [-6.6, 12.7, -5.8], [6.6, 18.0, -4.7], [82, 50]],
            ["left_shoulder_plate", "armor", [5.4, -7.8, -5.8], [10.4, -1.4, -4.6], [32, 34]],
            ["right_shoulder_plate", "armor", [-10.4, -7.8, -5.8], [-5.4, -1.4, -4.6], [44, 34]],
            ["head", "head", [-5.2, -10.0, -5.2], [5.2, 0.0, 5.2], [0, 0]],
            ["face_mask", "head", [-4.1, -8.2, -5.9], [4.1, -1.0, -4.8], [40, 0]],
            ["eye_slit", "head", [-3.2, -5.5, -6.4], [3.2, -3.7, -5.6], [60, 0]],
            ["mouth_groove", "head", [-2.5, -2.2, -6.2], [2.5, -1.2, -5.5], [72, 0]],
            ["left_cheek_plate", "head", [2.9, -6.8, -6.1], [4.4, -2.0, -5.1], [80, 0]],
            ["right_cheek_plate", "head", [-4.4, -6.8, -6.1], [-2.9, -2.0, -5.1], [86, 0]],
            ["crown_band", "head", [-4.8, -11.2, -4.8], [4.8, -9.0, 4.8], [0, 20]],
            ["crown_center_spike", "head", [-1.0, -16.0, -3.2], [1.0, -10.8, -1.2], [28, 0]],
            ["crown_spike_left", "head", [2.3, -15.0, -3.0], [4.2, -10.8, -1.1], [34, 0]],
            ["crown_spike_right", "head", [-4.2, -15.0, -3.0], [-2.3, -10.8, -1.1], [28, 8]],
            ["left_arm", "arms", [1.5, -2.0, -3.6], [8.7, 12.2, 3.6], [0, 62]],
            ["left_elbow_ring", "arms", [1.0, 10.8, -4.0], [9.2, 14.2, 4.0], [30, 62]],
            ["left_forearm", "arms", [1.7, 13.2, -3.5], [8.4, 26.0, 3.5], [60, 62]],
            ["left_fist", "arms", [0.9, 23.0, -4.4], [9.0, 29.0, 4.4], [88, 62]],
            ["left_finger_a", "arms", [1.0, 28.0, -4.8], [4.0, 31.0, -2.8], [112, 62]],
            ["left_finger_b", "arms", [5.0, 28.0, -4.8], [8.0, 31.0, -2.8], [112, 70]],
            ["right_arm", "arms", [-8.7, -2.0, -3.6], [-1.5, 12.2, 3.6], [0, 78]],
            ["right_elbow_ring", "arms", [-9.2, 10.8, -4.0], [-1.0, 14.2, 4.0], [30, 78]],
            ["right_forearm", "arms", [-8.4, 13.2, -3.5], [-1.7, 26.0, 3.5], [60, 78]],
            ["right_fist", "arms", [-9.0, 23.0, -4.4], [-0.9, 29.0, 4.4], [88, 78]],
            ["right_finger_a", "arms", [-8.0, 28.0, -4.8], [-5.0, 31.0, -2.8], [112, 78]],
            ["right_finger_b", "arms", [-4.0, 28.0, -4.8], [-1.0, 31.0, -2.8], [112, 86]],
            ["left_leg", "legs", [0.5, 0.0, -4.2], [8.7, 13.0, 4.2], [0, 94]],
            ["left_knee_ring", "legs", [0.0, 11.8, -4.7], [9.2, 15.2, 4.7], [34, 94]],
            ["left_shin_plate", "legs", [1.0, 14.5, -4.4], [8.3, 23.8, 4.4], [64, 94]],
            ["left_foot", "legs", [0.0, 20.5, -6.4], [10.2, 24.8, 5.2], [82, 94]],
            ["left_toe_plate", "legs", [0.5, 23.5, -7.2], [9.7, 26.0, -4.9], [0, 122]],
            ["right_leg", "legs", [-8.7, 0.0, -4.2], [-0.5, 13.0, 4.2], [36, 104]],
            ["right_knee_ring", "legs", [-9.2, 11.8, -4.7], [0.0, 15.2, 4.7], [72, 104]],
            ["right_shin_plate", "legs", [-8.3, 14.5, -4.4], [-1.0, 23.8, 4.4], [0, 104]],
            ["right_foot", "legs", [-10.2, 20.5, -6.4], [0.0, 24.8, 5.2], [82, 111]],
            ["right_toe_plate", "legs", [-9.7, 23.5, -7.2], [-0.5, 26.0, -4.9], [22, 122]],
        ],
    },
    {
        "id": "pegasus",
        "entity_id": "mythic_age:pegasus",
        "display": "Pegasus / 天马",
        "target_box": [1.7, 2.1],
        "visual_height": 2.15,
        "scale_note": "纯白大型飞行坐骑，应比普通马更神圣、更修长。",
        "primary": "#f7f7f0",
        "shadow": "#c8cdd0",
        "accent": "#ffffff",
        "silhouette": {
            "front": "白马头和长颈必须先被看见，双翼从肩部展开，头不能被身体/翼遮没。",
            "side": "马体比例清晰，头颈前伸，翅膀不能像薄片乱贴，骑乘空间合理。",
            "back": "双翼、尾巴和后腿从背面清楚，白色层次不丢失。",
        },
        "parts": [
            "body", "chest", "saddle_space", "neck", "neck_front_plane", "head", "snout",
            "nose_bridge", "left_cheek_shadow", "right_cheek_shadow", "left_nostril", "right_nostril",
            "nose_mouth", "left_eye", "right_eye", "left_ear", "right_ear", "mane", "mane_crest",
            "left_wing_shoulder", "left_wing_primary", "left_wing_secondary", "left_wing_coverts",
            "left_feathers", "right_wing_shoulder", "right_wing_primary", "right_wing_secondary",
            "right_wing_coverts", "right_feathers", "tail_base", "tail_plume", "four_legs", "hooves",
        ],
        "hierarchy": ["root/body", "root/head", "root/head/mane", "root/wings", "root/wings/feathers", "root/tail", "root/legs"],
        "uv_plan": [
            "head_front: horse-sized head, dark left/right eyes, snout plane, nostrils, nose bridge and mouth line",
            "neck_side: long white neck with shallow gray front plane and raised mane",
            "body_side: pure white coat shadows and clearly reserved saddle/rider space",
            "wing_top: shoulder block, primary plane, secondary plane and gray coverts",
            "wing_feathers: at least five separated feather slabs per side with gray band lines",
            "tail: white plume with layered shadow strips",
        ],
        "texture_detail": "严格白/浅灰体系，不使用金色或蓝色装饰；左右眼为小型深色高对比点，鼻口用浅灰线区分。大面积白色必须有浅灰分面线；每片羽毛要有灰线和白色高光，骑乘空间只用轻微阴影标示而不变成鞍具。",
        "animation": "待机轻拍翅；飞行时双翼大幅拍动；骑乘时身体稳定，玩家坐点不遮挡翅根。",
        "signature_features": [
            "纯白天马，第一眼必须是白马头、长颈、双翼，而不是一团白色方块",
            "头部参考原版马的细长比例，头宽约为马身宽度三分之一到二分之一之间，必须有左右耳、左右眼、鼻梁、鼻孔、口线和分段鬃毛，不能靠大头或被翅膀/身体遮挡",
            "翅膀必须有翼肩、主翼、次翼、覆羽和多片独立羽毛，具备自身飞行和载人飞行的视觉可信度",
            "背部保留骑乘空间，翅根位于肩部两侧",
            "全身必须是纯白/浅灰分面，避免白色贴图渲染为空白块",
        ],
        "modeling_musts": [
            "head、snout、ears、neck、mane 必须按马体比例清楚可读；头宽应保持在马身主体宽度约三分之一到二分之一，头高不得超过身体高度约三分之二，禁止通过异常放大头部来弥补细节不足",
            "left_eye/right_eye、left_ear/right_ear、left/right_nostril、nose_bridge、nose_mouth 必须独立或有明确 UV 区",
            "wing_primary、wing_secondary、wing_coverts 和 wing_feathers 必须分层，侧面能看出羽片",
            "身体纯白但通过浅灰阴影分面，避免渲染成空白",
            "hasPassengers 骑乘稳定和 isFlying 翼拍动画必须在运行模型中继续存在",
        ],
        "reject_if": [
            "看不出头部、头太大像玩具马、头宽超过马身一半，头颈融合成一整块白柱，或头被翼/身体吃掉",
            "翅膀像薄片，没有羽毛结构",
            "白色贴图缺少浅灰分面导致空白感",
            "缺少可审计的左右眼/左右耳/鼻口或骑乘空间",
        ],
        "v2_design_freeze": [
            "正面必须先读到比例正常的白马头、细长颈和深色眼点；头宽控制在马身主体宽度三分之一到二分之一之间，头高控制在身体高度约三分之二以内，横向翅膀不能压过头颈轮廓。",
            "侧面头、耳、鼻孔、鼻梁、口线、鬃毛必须在翅膀前方清楚可读，背部骑乘空间不得遮住翼根。",
            "纯白不是纯空白，所有大面必须有浅灰分面线，羽片至少四段分层。",
            "飞行能力必须在模型和动画双重可读：翼肩、主翼、次翼、覆羽和独立羽片都要存在。",
            "载人飞行能力必须保留背部骑乘空间，身体飞行时稳定，翼根不被玩家坐点遮挡。",
        ],
        "geometry": [
            ["body", "body", [-5.4, -5.8, -12.2], [5.4, 2.0, 10.2], [0, 32]],
            ["chest", "body", [-4.7, -7.2, -13.0], [4.7, -1.4, -6.1], [64, 34]],
            ["saddle_space", "body", [-3.8, -7.4, -5.6], [3.8, -5.9, 4.5], [88, 36]],
            ["left_shoulder_blend", "body", [3.8, -6.8, -9.0], [5.8, -3.0, -3.0], [106, 36]],
            ["right_shoulder_blend", "body", [-5.8, -6.8, -9.0], [-3.8, -3.0, -3.0], [108, 36]],
            ["neck", "head", [-2.05, -16.4, -6.5], [2.05, -2.2, -2.7], [44, 0]],
            ["neck_front_plane", "head", [-1.35, -15.4, -7.35], [1.35, -2.6, -6.3], [92, 0]],
            ["mane", "mane", [-1.05, -16.7, -4.8], [1.05, -3.9, -2.4], [100, 0]],
            ["mane_crest", "mane", [-0.7, -17.3, -5.25], [0.7, -7.9, -4.25], [110, 12]],
            ["head", "head", [-1.78, -17.2, -12.9], [1.78, -12.7, -8.2], [0, 0]],
            ["snout", "head", [-1.02, -15.0, -19.0], [1.02, -12.9, -12.85], [28, 0]],
            ["nose_bridge", "head", [-0.34, -16.0, -19.35], [0.34, -14.25, -18.6], [78, 0]],
            ["left_cheek_shadow", "head", [-2.12, -15.15, -12.8], [-1.72, -13.2, -10.0], [84, 0]],
            ["right_cheek_shadow", "head", [1.72, -15.15, -12.8], [2.12, -13.2, -10.0], [90, 0]],
            ["left_ear", "head", [0.62, -20.1, -10.9], [1.4, -17.2, -9.95], [92, 16]],
            ["right_ear", "head", [-1.4, -20.1, -10.9], [-0.62, -17.2, -9.95], [100, 16]],
            ["left_eye", "head", [-2.02, -16.25, -13.45], [-1.42, -15.58, -12.92], [112, 0]],
            ["right_eye", "head", [1.42, -16.25, -13.45], [2.02, -15.58, -12.92], [118, 0]],
            ["left_nostril", "head", [-0.72, -13.45, -19.45], [-0.24, -12.85, -18.82], [104, 4]],
            ["right_nostril", "head", [0.24, -13.45, -19.45], [0.72, -12.85, -18.82], [108, 4]],
            ["nose_mouth", "head", [-0.82, -12.65, -19.35], [0.82, -12.1, -18.65], [112, 4]],
            ["left_wing_shoulder", "wings", [0.6, -4.0, -2.8], [7.8, 0.0, 5.8], [0, 64]],
            ["left_wing_primary", "wings", [5.8, -3.7, -1.8], [24.8, -1.3, 10.0], [28, 64]],
            ["left_wing_secondary", "wings", [13.1, -2.1, 8.8], [29.6, -0.1, 16.4], [60, 64]],
            ["left_wing_coverts", "wings", [3.4, -1.5, -1.2], [16.1, 0.2, 9.0], [80, 64]],
            ["left_feather_a", "feathers", [4.0, 0.0, 7.2], [7.0, 1.2, 15.5], [0, 78]],
            ["left_feather_b", "feathers", [7.8, 0.0, 8.0], [10.8, 1.2, 17.2], [12, 78]],
            ["left_feather_c", "feathers", [11.6, 0.0, 9.0], [14.6, 1.2, 18.8], [24, 78]],
            ["left_feather_d", "feathers", [15.4, 0.0, 10.0], [18.4, 1.2, 20.0], [36, 78]],
            ["left_feather_e", "feathers", [19.2, 0.0, 10.8], [22.2, 1.2, 19.6], [48, 78]],
            ["right_wing_shoulder", "wings", [-7.8, -4.0, -2.8], [-0.6, 0.0, 5.8], [54, 64]],
            ["right_wing_primary", "wings", [-24.8, -3.7, -1.8], [-5.8, -1.3, 10.0], [66, 106]],
            ["right_wing_secondary", "wings", [-29.6, -2.1, 8.8], [-13.1, -0.1, 16.4], [20, 106]],
            ["right_wing_coverts", "wings", [-16.1, -1.5, -1.2], [-3.4, 0.2, 9.0], [80, 104]],
            ["right_feather_a", "feathers", [-7.0, 0.0, 7.2], [-4.0, 1.2, 15.5], [60, 78]],
            ["right_feather_b", "feathers", [-10.8, 0.0, 8.0], [-7.8, 1.2, 17.2], [72, 78]],
            ["right_feather_c", "feathers", [-14.6, 0.0, 9.0], [-11.6, 1.2, 18.8], [84, 78]],
            ["right_feather_d", "feathers", [-18.4, 0.0, 10.0], [-15.4, 1.2, 20.0], [96, 78]],
            ["right_feather_e", "feathers", [-22.2, 0.0, 10.8], [-19.2, 1.2, 19.6], [104, 78]],
            ["tail_base", "tail", [-1.2, -1.0, 0], [1.2, 1.2, 10], [64, 52]],
            ["tail_plume", "tail", [-3.1, -2.7, 8], [3.1, 3.3, 15.4], [94, 52]],
            ["tail_highlight", "tail", [-1.0, -3.0, 10.5], [1.0, -1.8, 16.0], [108, 52]],
            ["front_left_leg", "legs", [1.6, 0, -8], [4.9, 12, -4.7], [0, 88]],
            ["front_right_leg", "legs", [-4.9, 0, -8], [-1.6, 12, -4.7], [14, 88]],
            ["front_left_hoof", "legs", [1.2, 10.2, -9.0], [5.2, 12.5, -4.3], [28, 88]],
            ["front_right_hoof", "legs", [-5.2, 10.2, -9.0], [-1.2, 12.5, -4.3], [42, 88]],
            ["back_left_leg", "legs", [1.6, 0, 5], [5.3, 12, 8.7], [56, 88]],
            ["back_right_leg", "legs", [-5.3, 0, 5], [-1.6, 12, 8.7], [72, 88]],
            ["back_left_hoof", "legs", [1.2, 10.2, 4.5], [5.5, 12.5, 9.5], [88, 88]],
            ["back_right_hoof", "legs", [-5.5, 10.2, 4.5], [-1.2, 12.5, 9.5], [104, 88]],
        ],
    },
]


def texture_path(creature_id: str) -> Path:
    if creature_id == "centaur":
        return TEXTURE_ROOT / "centaur/centaur.png"
    return TEXTURE_ROOT / creature_id / f"{creature_id}.png"


def hex_to_rgb(value: str) -> tuple[int, int, int]:
    value = value.removeprefix("#")
    return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16)


def shade_rgb(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(max(0, min(255, channel + amount)) for channel in color)


def load_font(size: int) -> ImageFont.ImageFont:
    for path in FONT_PATHS:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def image_pixels(image: Image.Image) -> list[tuple[int, int, int, int]]:
    getter = getattr(image, "get_flattened_data", None)
    if getter is not None:
        return list(getter())
    return list(image.getdata())


def clipped_box(coords: list[int]) -> tuple[int, int, int, int] | None:
    x1, y1, x2, y2 = coords
    x1 = max(0, min(127, x1))
    y1 = max(0, min(127, y1))
    x2 = max(0, min(128, x2))
    y2 = max(0, min(128, y2))
    if x2 <= x1 or y2 <= y1:
        return None
    return x1, y1, x2, y2


def box_uv_faces(uv: list[int], frm: list[float], to: list[float]) -> dict[str, dict[str, Any]]:
    u, v = uv
    sx = int(math.ceil(abs(to[0] - frm[0])))
    sy = int(math.ceil(abs(to[1] - frm[1])))
    sz = int(math.ceil(abs(to[2] - frm[2])))
    faces = {
        "east": [u, v + sz, u + sz, v + sz + sy],
        "north": [u + sz, v + sz, u + sz + sx, v + sz + sy],
        "west": [u + sz + sx, v + sz, u + sz + sx + sz, v + sz + sy],
        "south": [u + sz + sx + sz, v + sz, u + sz + sx + sz + sx, v + sz + sy],
        "up": [u + sz, v, u + sz + sx, v + sz],
        "down": [u + sz + sx, v, u + sz + sx + sx, v + sz],
    }
    return {name: {"uv": coords, "texture": 0} for name, coords in faces.items()}


def uv_footprint(row: list[Any]) -> tuple[int, int, int, int]:
    _name, _group, frm, to, uv = row
    sx = int(math.ceil(abs(to[0] - frm[0])))
    sy = int(math.ceil(abs(to[1] - frm[1])))
    sz = int(math.ceil(abs(to[2] - frm[2])))
    u, v = uv
    return u, v, u + 2 * sz + 2 * sx, v + sz + sy


def element_from_geometry(creature_id: str, row: list[Any]) -> dict[str, Any]:
    name, group, frm, to, uv = row
    return {
        "name": name,
        "box_uv": True,
        "rescale": False,
        "locked": False,
        "render_order": "default",
        "allow_mirror_modeling": True,
        "from": frm,
        "to": to,
        "autouv": 0,
        "color": 0,
        "origin": [0, 0, 0],
        "uv_offset": uv,
        "faces": box_uv_faces(uv, frm, to),
        "type": "cube",
        "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"mythic_age:{creature_id}:{name}")),
        "_group": group,
    }


def outliner_for(elements: list[dict[str, Any]]) -> list[dict[str, Any]]:
    groups: dict[str, list[str]] = {}
    for element in elements:
        groups.setdefault(element["_group"], []).append(element["uuid"])
    return [
        {
            "name": group,
            "origin": [0, 0, 0],
            "color": 0,
            "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"mythic_age:group:{group}")),
            "children": children,
        }
        for group, children in groups.items()
    ]


def design_front_matter(creature: dict[str, Any]) -> list[str]:
    cid = creature["id"]
    fields = {
        "creature_id": cid,
        "creature_name": creature["display"],
        "design_status": "needs_user_review",
        "design_sheet_status": "needs_user_review",
        "design_approval_status": "approval_pending",
        "design_correctness_verdict": "not_reviewed",
        "design_detail_sheet_status": "needs_user_review",
        "design_detail_board_status": "needs_user_review",
        "modeling_allowed": "false",
        "design_package_status": "generated_reference_needs_human_approval",
        "approval_basis": "human_design_review_required_before_model_uv_texture_animation_or_runtime_sync",
    }
    if cid == "pegasus":
        fields.update({
            "current_v2_assets_status": "rejected",
            "blocking_reason": "current Pegasus model/design is malformed and cannot be used as modeling basis",
            "design_sheet_status": "v3_candidate_needs_user_review",
            "design_approval_status": "v3_approval_pending",
            "design_correctness_verdict": "current_design_rejected_v3_pending",
            "design_detail_sheet_status": "v3_proportion_lock_pending",
            "design_detail_board_status": "v3_detail_board_pending",
            "v3_blueprint": "docs/美术资源/greek_creatures/design_blueprints/pegasus_v3_blueprint.md",
        })
    elif cid == "hydra":
        fields.update({
            "current_v2_assets_status": "rejected",
            "blocking_reason": "current Hydra model/design lacks valid head-neck-shoulder-body topology",
            "design_sheet_status": "v3_candidate_needs_user_review",
            "design_approval_status": "v3_approval_pending",
            "design_correctness_verdict": "current_design_rejected_v3_pending",
            "design_detail_sheet_status": "v3_topology_lock_pending",
            "design_detail_board_status": "v3_detail_board_pending",
            "v3_blueprint": "docs/美术资源/greek_creatures/design_blueprints/hydra_v3_blueprint.md",
        })
    else:
        fields.update({
            "current_v2_assets_status": "generated_reference_only",
            "blocking_reason": "design package exists but has not been formally approved for 1.0 modeling",
        })
    lines = ["---"]
    lines.extend(f"{key}: {value}" for key, value in fields.items())
    lines.append("---")
    return lines


def write_design_docs() -> None:
    DESIGN_DIR.mkdir(parents=True, exist_ok=True)
    for creature in CREATURES:
        cid = creature["id"]
        path = DESIGN_DIR / f"{cid}.md"
        concept_path = CONCEPT_DIR / f"{cid}_concept.png"
        design_state_lines: list[str] = []
        if cid == "pegasus":
            design_state_lines = [
                "",
                "## 当前设计状态",
                "- 状态：`rejected_v2_reference_only`。",
                "- 当前 V2/旧实现已被用户判定畸形，不能作为 `.bbmodel`、Java model、贴图、动作或预览的继续建模依据。",
                "- 可建模依据必须改用 `docs/美术资源/greek_creatures/design_blueprints/pegasus_v3_blueprint.md`，并等待比例锁定表、细化设计板和设计正确性 gate 全部批准。",
            ]
        elif cid == "hydra":
            design_state_lines = [
                "",
                "## 当前设计状态",
                "- 状态：`rejected_v2_reference_only`。",
                "- 当前 V2/旧五头实现已被用户判定畸形，不能作为 `.bbmodel`、Java model、贴图、动作或预览的继续建模依据。",
                "- 可建模依据必须改用 `docs/美术资源/greek_creatures/design_blueprints/hydra_v3_blueprint.md`，并等待九头拓扑锁定表、细化设计板和设计正确性 gate 全部批准。",
            ]
        v2_lines: list[str] = []
        if creature.get("v2_design_freeze"):
            v2_lines = [
                "",
                "## V2 造型冻结",
                "- 触发依据：用户实测反馈指出当前模型存在体量、精细度、纹理空白和神话特征不可读问题；测试只验证设计落地，不再承担造型试错。",
                *[f"- {item}" for item in creature["v2_design_freeze"]],
            ]
        lines = [
            *design_front_matter(creature),
            "",
            f"# {creature['display']} 设计包",
            "",
            "## 目标体量",
            f"- Entity ID: `{creature['entity_id']}`",
            f"- 碰撞盒目标：`{creature['target_box'][0]}w x {creature['target_box'][1]}h`",
            f"- 视觉高度目标：约 `{creature['visual_height']}` 格",
            f"- 比例说明：{creature['scale_note']}",
            "",
            "## 轮廓语言",
            f"- 正面：{creature['silhouette']['front']}",
            f"- 侧面：{creature['silhouette']['side']}",
            f"- 45 度：{creature['silhouette']['back']}",
            *design_state_lines,
            "",
            "## 概念设计图",
            f"- 文件：`{concept_path.relative_to(ROOT)}`",
            "- 用途：这是建模前的造型合同；Blockbench 和 MC 截图只验证是否实现该设计，不负责重新发明造型。",
            "- 三视角：正面、侧面、45 度必须同时能识别该生物。",
            *v2_lines,
            "",
            "## 核心识别特征",
            *[f"- {item}" for item in creature.get("signature_features", [])],
            "",
            "## 部件清单",
            *[f"- `{part}`" for part in creature["parts"]],
            "",
            "## Blockbench 层级",
            *[f"- `{item}`" for item in creature["hierarchy"]],
            "",
            "## 建模硬性要求",
            *[f"- {item}" for item in creature.get("modeling_musts", [])],
            "",
            "## UV 计划",
            *[f"- {item}" for item in creature["uv_plan"]],
            "",
            "## 纹理细节",
            f"- 主色：`{creature['primary']}`",
            f"- 阴影：`{creature['shadow']}`",
            f"- 强调色：`{creature['accent']}`",
            f"- 细节规则：{creature['texture_detail']}",
            "",
            "## 动画约束",
            f"- {creature['animation']}",
            "",
            "## 返工触发条件",
            *[f"- {item}" for item in creature.get("reject_if", [])],
            "",
            "## 验收口径",
            "- 先验收概念设计图，再进入 Blockbench 建模。",
            "- 必须提供正面、侧面、45 度三视图，且三视图来自已挂载贴图后的模型预览。",
            "- 必须提供 UV 模板和贴图贴合报告，关键 UV 区域不得透明、空白或越界。",
            "- 如三视图或贴合报告失败，优先修 .bbmodel、UV 或贴图。",
        ]
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_manifest() -> None:
    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "version": 1,
        "workflow": "design_first_blockbench_only",
        "views": list(VIEWS),
        "concept_dir": str(CONCEPT_DIR.relative_to(ROOT)),
        "visual_report": str(REPORT_PATH.relative_to(ROOT)),
        "texture_fit_report": str(TEXTURE_FIT_REPORT_PATH.relative_to(ROOT)),
        "contact_sheet": str(CONTACT_SHEET_PATH.relative_to(ROOT)),
        "concept_contact_sheet": str(CONCEPT_CONTACT_SHEET_PATH.relative_to(ROOT)),
        "creatures": [],
    }
    for creature in CREATURES:
        cid = creature["id"]
        payload["creatures"].append({
            "id": cid,
            "entity_id": creature["entity_id"],
            "design_doc": str((DESIGN_DIR / f"{cid}.md").relative_to(ROOT)),
            "concept_art": str((CONCEPT_DIR / f"{cid}_concept.png").relative_to(ROOT)),
            "bbmodel": str((BLOCKBENCH_DIR / f"{cid}.bbmodel").relative_to(ROOT)),
            "texture": str(texture_path(cid).relative_to(ROOT)),
            "uv_template": str((PREVIEW_DIR / cid / "uv_template.png").relative_to(ROOT)),
            "blockbench_previews": {
                view: str((PREVIEW_DIR / cid / "blockbench" / f"{view}.png").relative_to(ROOT))
                for view in VIEWS
            },
            "texture_fit_preview": str((PREVIEW_DIR / cid / "texture_fit.png").relative_to(ROOT)),
            "target_box": creature["target_box"],
            "visual_height": creature["visual_height"],
            "required_parts": creature["parts"],
            "acceptance": [
                "design_doc_complete",
                "concept_art_present",
                "bbmodel_present",
                "uv_template_present",
                "blockbench_previews_present",
                "no_blank_or_transparent_key_uv_regions",
                "texture_model_fit_passed",
            ],
        })
    MANIFEST_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def blockbench_preview_paths(cid: str) -> dict[str, str]:
    return {
        view: str((PREVIEW_DIR / cid / "blockbench" / f"{view}.png").relative_to(ROOT))
        for view in VIEWS
    }


def write_visual_report() -> None:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    texture_fit = load_texture_fit_report()
    fit_by_id = {item["id"]: item for item in texture_fit.get("creatures", [])}
    payload = {
        "version": 1,
        "workflow": "design_first_blockbench_only",
        "gate_rule": "Concept art fixes the intended creature design before Blockbench and Minecraft validation.",
        "views": list(VIEWS),
        "concept_dir": str(CONCEPT_DIR.relative_to(ROOT)),
        "texture_fit_report": str(TEXTURE_FIT_REPORT_PATH.relative_to(ROOT)),
        "contact_sheet": str(CONTACT_SHEET_PATH.relative_to(ROOT)),
        "concept_contact_sheet": str(CONCEPT_CONTACT_SHEET_PATH.relative_to(ROOT)),
        "creatures": [],
    }
    for creature in CREATURES:
        cid = creature["id"]
        blockbench_previews = blockbench_preview_paths(cid)
        blockbench_ready = all((ROOT / path).exists() for path in blockbench_previews.values())
        fit_summary = fit_by_id.get(cid, {})
        fit_ok = bool(fit_summary.get("ok", False))
        payload["creatures"].append({
            "id": cid,
            "entity_id": creature["entity_id"],
            "design_target": {
                "target_box": creature["target_box"],
                "visual_height": creature["visual_height"],
                "silhouette": creature["silhouette"],
                "required_parts": creature["parts"],
                "signature_features": creature.get("signature_features", []),
                "modeling_musts": creature.get("modeling_musts", []),
                "reject_if": creature.get("reject_if", []),
                "v2_design_freeze": creature.get("v2_design_freeze", []),
                "uv_plan": creature["uv_plan"],
                "texture_detail": creature["texture_detail"],
                "animation": creature["animation"],
            },
            "artifacts": {
                "design_doc": str((DESIGN_DIR / f"{cid}.md").relative_to(ROOT)),
                "concept_art": str((CONCEPT_DIR / f"{cid}_concept.png").relative_to(ROOT)),
                "bbmodel": str((BLOCKBENCH_DIR / f"{cid}.bbmodel").relative_to(ROOT)),
                "texture": str(texture_path(cid).relative_to(ROOT)),
                "uv_template": str((PREVIEW_DIR / cid / "uv_template.png").relative_to(ROOT)),
                "blockbench_previews": blockbench_previews,
                "texture_fit_preview": str((PREVIEW_DIR / cid / "texture_fit.png").relative_to(ROOT)),
            },
            "checks": {
                "design_doc_complete": (DESIGN_DIR / f"{cid}.md").exists(),
                "concept_art_present": (CONCEPT_DIR / f"{cid}_concept.png").exists(),
                "bbmodel_present": (BLOCKBENCH_DIR / f"{cid}.bbmodel").exists(),
                "uv_template_present": (PREVIEW_DIR / cid / "uv_template.png").exists(),
                "blockbench_previews_present": blockbench_ready,
                "texture_fit_preview_present": (PREVIEW_DIR / cid / "texture_fit.png").exists(),
                "no_blank_or_transparent_key_uv_regions": fit_ok,
                "texture_model_fit_passed": fit_ok,
                "matches_design_package": "requires_human_blockbench_review",
            },
            "texture_fit_summary": {
                "ok": fit_ok,
                "elements_checked": fit_summary.get("elements_checked", 0),
                "average_opaque_ratio": fit_summary.get("average_opaque_ratio"),
                "average_color_variation": fit_summary.get("average_color_variation"),
                "failures": fit_summary.get("failures", []),
            },
            "status": "blockbench_passed_pending_human_shape_review" if blockbench_ready and fit_ok else "blockbench_failed",
        })
    REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def write_bbmodels() -> None:
    BLOCKBENCH_DIR.mkdir(parents=True, exist_ok=True)
    for creature in CREATURES:
        cid = creature["id"]
        texture = texture_path(cid)
        texture_relative_path = os.path.relpath(texture, BLOCKBENCH_DIR)
        texture_folder = os.path.relpath(texture.parent, BLOCKBENCH_DIR)
        elements = [element_from_geometry(cid, row) for row in creature["geometry"]]
        outliner = outliner_for(elements)
        clean_elements = []
        for element in elements:
            item = dict(element)
            item.pop("_group", None)
            clean_elements.append(item)
        model = {
            "meta": {"format_version": "4.10", "model_format": "modded_entity", "box_uv": True},
            "name": cid,
            "model_identifier": cid,
            "modded_entity_version": "1.17",
            "modded_entity_flip_y": True,
            "visible_box": [creature["target_box"][0], creature["target_box"][1], 0],
            "resolution": {"width": 128, "height": 128},
            "textures": [{
                "path": texture_relative_path,
                "relative_path": texture_relative_path,
                "name": cid,
                "folder": texture_folder,
                "namespace": "mythic_age",
                "id": "0",
                "particle": False,
                "render_mode": "default",
                "visible": True,
                "mode": "bitmap",
                "saved": True,
                "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"mythic_age:texture:{cid}")),
            }],
            "elements": clean_elements,
            "outliner": outliner,
        }
        (BLOCKBENCH_DIR / f"{cid}.bbmodel").write_text(json.dumps(model, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def draw_uv_templates() -> None:
    font = load_font(8)
    for creature in CREATURES:
        cid = creature["id"]
        path = PREVIEW_DIR / cid / "uv_template.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        image = Image.new("RGBA", (128, 128), (250, 247, 239, 255))
        draw = ImageDraw.Draw(image)
        for row in creature["geometry"]:
            name, group, frm, to, uv = row
            sx = int(math.ceil(abs(to[0] - frm[0])))
            sy = int(math.ceil(abs(to[1] - frm[1])))
            sz = int(math.ceil(abs(to[2] - frm[2])))
            width = 2 * sz + 2 * sx
            height = sz + sy
            u, v = uv
            fill = creature["primary"] + "55"
            draw.rectangle((u, v, min(127, u + width), min(127, v + height)), outline=creature["shadow"], fill=fill, width=1)
            draw.text((u + 1, v + 1), name[:14], fill=(20, 20, 20), font=font)
        draw.text((4, 116), f"{cid} UV plan", fill=(40, 40, 40), font=font)
        image.save(path)


def wrap_text(text: str, font: ImageFont.ImageFont, max_width: int) -> list[str]:
    lines: list[str] = []
    current = ""
    for char in text:
        candidate = current + char
        bbox = font.getbbox(candidate)
        if current and bbox[2] - bbox[0] > max_width:
            lines.append(current)
            current = char
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines


def draw_concept_art() -> None:
    title_font = load_font(24)
    label_font = load_font(16)
    body_font = load_font(13)
    small_font = load_font(11)
    for creature in CREATURES:
        cid = creature["id"]
        concept_path = CONCEPT_DIR / f"{cid}_concept.png"
        concept_path.parent.mkdir(parents=True, exist_ok=True)
        image = Image.new("RGBA", (1320, 860), (246, 242, 233, 255))
        draw = ImageDraw.Draw(image)
        primary = hex_to_rgb(creature["primary"])
        shadow = hex_to_rgb(creature["shadow"])
        accent = hex_to_rgb(creature["accent"])
        draw.rectangle((0, 0, 1320, 76), fill=(34, 31, 27, 255))
        draw.text((28, 22), f"{creature['display']} 概念设计图 / Modeling Contract", fill=(255, 255, 255, 255), font=title_font)
        draw.text((920, 24), f"box {creature['target_box'][0]}w x {creature['target_box'][1]}h | visual {creature['visual_height']}", fill=(221, 216, 205, 255), font=label_font)

        panels = [
            ("front", "正面", (40, 110, 410, 610)),
            ("side", "侧面", (470, 110, 840, 610)),
            ("three_quarter", "45度", (900, 110, 1270, 610)),
        ]
        geometry = creature["geometry"]
        for view, label, panel in panels:
            px1, py1, px2, py2 = panel
            draw.rectangle(panel, fill=(236, 231, 220, 255), outline=shadow + (255,), width=2)
            draw.text((px1 + 14, py1 + 12), label, fill=shadow + (255,), font=label_font)
            boxes = []
            for row in geometry:
                name, _group, frm, to, _uv = row
                corners = [
                    (frm[0], frm[1], frm[2]),
                    (frm[0], frm[1], to[2]),
                    (frm[0], to[1], frm[2]),
                    (frm[0], to[1], to[2]),
                    (to[0], frm[1], frm[2]),
                    (to[0], frm[1], to[2]),
                    (to[0], to[1], frm[2]),
                    (to[0], to[1], to[2]),
                ]
                pts = [transform_point(corner, view) for corner in corners]
                xs = [p[0] for p in pts]
                ys = [p[1] for p in pts]
                boxes.append((view_depth(corners, view), min(xs), min(ys), max(xs), max(ys), name))
            boxes.sort(key=lambda item: item[0])
            all_x = [value for _, x1, _, x2, _, _ in boxes for value in (x1, x2)]
            all_y = [value for _, _, y1, _, y2, _ in boxes for value in (y1, y2)]
            min_x, max_x = min(all_x), max(all_x)
            min_y, max_y = min(all_y), max(all_y)
            scale = min((px2 - px1 - 70) / max(1.0, max_x - min_x), (py2 - py1 - 90) / max(1.0, max_y - min_y))
            ox = (px1 + px2) * 0.5 - (min_x + max_x) * 0.5 * scale
            oy = py2 - 34 - max_y * scale
            for index, (_, x1, y1, x2, y2, name) in enumerate(boxes):
                sx1 = ox + x1 * scale
                sy1 = oy + y1 * scale
                sx2 = ox + x2 * scale
                sy2 = oy + y2 * scale
                color = shade_rgb(primary, (sum(ord(ch) for ch in name) % 44) - 18)
                if any(token in name for token in ("eye", "fang", "mouth", "jaw", "snout")):
                    color = accent if "eye" in name else shade_rgb(shadow, 30)
                if any(token in name for token in ("club", "horn", "sting", "claw")):
                    color = shade_rgb(accent, -20)
                if cid == "pegasus":
                    color = shade_rgb(color, -18)
                outline_width = 3 if any(token in name for token in (
                    "eye", "mouth", "jaw", "snout", "head", "club", "wing", "feather",
                    "horn", "sting", "mane", "tail", "neck"
                )) else 2
                draw.rectangle((sx1, sy1, sx2, sy2), fill=color + (232,), outline=shadow + (255,), width=outline_width)
                if index % 3 == 0 and (sx2 - sx1) > 30 and (sy2 - sy1) > 16:
                    draw.text((sx1 + 3, sy1 + 2), name[:13], fill=(255, 255, 255, 230), font=small_font)
            draw.line((px1 + 28, py2 - 34, px2 - 28, py2 - 34), fill=(174, 164, 148, 255), width=2)
            draw_key_callouts(draw, cid, view, panel, shadow, accent, label_font, small_font)

        left = 50
        y = 650
        draw.text((left, y), "核心识别特征", fill=shadow + (255,), font=label_font)
        y += 28
        for item in creature.get("signature_features", [])[:4]:
            for line in wrap_text(f"- {item}", body_font, 590):
                draw.text((left, y), line, fill=(35, 34, 31, 255), font=body_font)
                y += 20
        right = 700
        y = 650
        draw.text((right, y), "建模硬性要求 / 返工条件", fill=shadow + (255,), font=label_font)
        y += 28
        requirements = creature.get("modeling_musts", [])[:3] + creature.get("reject_if", [])[:2]
        for item in requirements:
            for line in wrap_text(f"- {item}", body_font, 560):
                draw.text((right, y), line, fill=(35, 34, 31, 255), font=body_font)
                y += 20
        image.save(concept_path)


def draw_key_callouts(draw: ImageDraw.ImageDraw, cid: str, view: str, panel: tuple[int, int, int, int],
                      shadow: tuple[int, int, int], accent: tuple[int, int, int],
                      label_font: ImageFont.ImageFont, small_font: ImageFont.ImageFont) -> None:
    px1, py1, px2, py2 = panel
    callouts: dict[str, dict[str, list[tuple[int, int, str]]]] = {
        "cyclops": {
            "front": [(82, 132, "单眼"), (92, 196, "嘴"), (30, 300, "巨石棒")],
            "side": [(72, 230, "前突头脸"), (42, 330, "石棒侧影")],
            "three_quarter": [(76, 150, "宽肩"), (52, 330, "武器必须可见")],
        },
        "hydra": {
            "front": [(75, 124, "5头扇形"), (115, 215, "头颈连接"), (128, 335, "低伏瘦身")],
            "side": [(72, 190, "长颈前探"), (180, 260, "长尾")],
            "three_quarter": [(92, 140, "多头错层"), (150, 250, "背脊鳞")],
        },
        "pegasus": {
            "front": [(75, 255, "头颈先可见"), (168, 190, "双翼"), (82, 330, "纯白分面")],
            "side": [(58, 250, "头/耳/鼻口"), (160, 190, "羽片分层"), (148, 326, "骑乘空间")],
            "three_quarter": [(78, 240, "头不能消失"), (160, 185, "翼根在肩部")],
        },
        "manticore": {
            "front": [(70, 254, "兽脸獠牙"), (182, 228, "膜翼"), (232, 265, "毒刺")],
            "side": [(82, 264, "狮身"), (220, 255, "蝎尾毒刺")],
            "three_quarter": [(82, 244, "翼+尾分离")],
        },
        "chimera": {
            "front": [(88, 232, "狮头"), (142, 210, "羊角"), (228, 248, "蛇尾")],
            "side": [(92, 256, "三重混合"), (230, 250, "蛇头尾")],
            "three_quarter": [(92, 230, "多头不可糊")],
        },
        "nemean_lion": {
            "front": [(86, 150, "巨大鬃毛"), (116, 300, "厚爪")],
            "side": [(80, 260, "短吻"), (245, 270, "尾端毛球")],
            "three_quarter": [(92, 150, "鬃毛第一识别点")],
        },
        "centaur": {
            "front": [(110, 160, "人形上身"), (90, 340, "四蹄"), (42, 230, "弓")],
            "side": [(96, 150, "人马拼接"), (48, 230, "弓侧影"), (245, 300, "尾巴")],
            "three_quarter": [(102, 170, "人脸需可读")],
        },
        "medusa": {
            "front": [(95, 128, "蛇发冠"), (112, 235, "石化凝视"), (120, 372, "蛇尾")],
            "side": [(108, 130, "蛇发侧影"), (126, 360, "滑行尾")],
            "three_quarter": [(98, 128, "蛇发分束")],
        },
        "colossus": {
            "front": [(92, 122, "冠饰"), (118, 165, "发光眼缝"), (120, 270, "青铜胸甲")],
            "side": [(116, 160, "神像厚度"), (112, 315, "柱状腿")],
            "three_quarter": [(100, 168, "板缝")],
        },
    }
    for dx, dy, text in callouts.get(cid, {}).get(view, []):
        x = px1 + dx
        y = py1 + dy
        draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=accent + (255,), outline=shadow + (255,), width=2)
        draw.line((x + 6, y, x + 42, y - 20), fill=shadow + (255,), width=2)
        tw = label_font.getbbox(text)[2]
        draw.rectangle((x + 44, y - 34, x + 56 + tw, y - 10), fill=(250, 247, 239, 235), outline=shadow + (255,), width=1)
        draw.text((x + 50, y - 31), text, fill=shadow + (255,), font=small_font)


def draw_eye_pair(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color: tuple[int, int, int]) -> None:
    x1, y1, x2, y2 = box
    width = max(1, x2 - x1)
    height = max(1, y2 - y1)
    eye_w = max(2, width // 10)
    eye_h = max(2, height // 8)
    y = y1 + max(2, height // 5)
    left_x = x1 + max(3, width // 4)
    right_x = x2 - max(3, width // 4) - eye_w
    draw.rectangle((left_x, y, left_x + eye_w, y + eye_h), fill=color + (255,))
    draw.rectangle((right_x, y, right_x + eye_w, y + eye_h), fill=color + (255,))


def draw_single_eye(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color: tuple[int, int, int]) -> None:
    x1, y1, x2, y2 = box
    width = max(1, x2 - x1)
    height = max(1, y2 - y1)
    eye_w = max(3, width // 5)
    eye_h = max(2, height // 3)
    cx = (x1 + x2) // 2
    cy = y1 + max(2, height // 3)
    draw.rectangle((cx - eye_w // 2, cy, cx + eye_w // 2, cy + eye_h), fill=color + (255,))


def decorate_texture_region(draw: ImageDraw.ImageDraw, creature: dict[str, Any], row: list[Any], rect: tuple[int, int, int, int]) -> None:
    cid = creature["id"]
    name = row[0]
    primary = hex_to_rgb(creature["primary"])
    shadow = hex_to_rgb(creature["shadow"])
    accent = hex_to_rgb(creature["accent"])
    x1, y1, x2, y2 = rect
    width = max(1, x2 - x1)
    height = max(1, y2 - y1)

    if cid == "minotaur":
        if "eye" in name:
            draw.rectangle(rect, fill=(24, 18, 14, 255), outline=(215, 185, 114, 255), width=1)
            draw.point((x1, y1), fill=(245, 220, 145, 255))
            return
        if "nostril" in name:
            draw.rectangle(rect, fill=(24, 18, 14, 255), outline=shadow + (255,), width=1)
            draw.point((x1, y1), fill=(96, 69, 45, 255))
            return
        if "mouth" in name:
            draw.rectangle(rect, fill=(39, 24, 17, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 1, (y1 + y2) // 2), fill=(119, 53, 35, 255), width=1)
            return
        if "muzzle" in name:
            draw.rectangle(rect, fill=(128, 80, 44, 255), outline=shadow + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 2, (x1 + x2) // 2, y2 - 2), fill=(55, 34, 22, 255), width=1)
            draw.line((x1 + 2, y2 - 3, x2 - 2, y2 - 3), fill=(48, 29, 20, 255), width=1)
            return
        if "brow" in name:
            draw.rectangle(rect, fill=(48, 28, 18, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=(20, 13, 10, 255), width=1)
            return
        if "horn" in name:
            horn = (221, 203, 155)
            draw.rectangle(rect, fill=horn + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(163, 138, 93, 255), width=1)
            return
        if "ear" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 10) + (255,), outline=shadow + (255,), width=1)
            draw.rectangle(
                (x1 + max(1, width // 4), y1 + max(1, height // 4), x2 - max(1, width // 4), y2 - max(1, height // 5)),
                fill=shade_rgb(primary, -18) + (255,),
            )
            return
        if "chest_muscle" in name or "chest_harness" in name or "waist_belt" in name or "loincloth" in name:
            leather = (67, 42, 28)
            if "chest_muscle" in name:
                leather = shade_rgb(primary, 14)
            elif "loincloth" in name:
                leather = (84, 50, 31)
            draw.rectangle(rect, fill=leather + (255,), outline=shadow + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 1), fill=shade_rgb(shadow, 28) + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=shade_rgb(accent, -36) + (255,), width=1)
            return
        if "finger" in name or "hoof_claw" in name:
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=(42, 30, 24, 255), outline=shadow + (255,))
            return
        if "hoof" in name:
            draw.rectangle(rect, fill=(48, 33, 27, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=(107, 77, 55, 255), width=1)
            return
        if "fist" in name or "forearm" in name or "arm" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 3) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=shade_rgb(accent, -42) + (255,), width=1)
            return
        if "labrys" in name:
            metal = (132, 134, 126)
            if "handle" in name:
                metal = (77, 49, 31)
            elif "socket" in name:
                metal = (93, 91, 84)
            elif "counterweight" in name:
                metal = (104, 96, 82)
            draw.rectangle(rect, fill=metal + (255,), outline=shadow + (255,), width=1)
            if "blade" in name:
                draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(205, 207, 196, 255), width=1)
                draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(77, 78, 73, 255), width=1)
            return
        if any(token in name for token in ("torso", "body", "neck", "leg", "tail", "head")):
            base = shade_rgb(primary, 0)
            if "neck" in name or "head" in name:
                base = shade_rgb(primary, -5)
            draw.rectangle(rect, fill=base + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(5, min(width, height) // 3)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=shade_rgb(accent, -45) + (255,), width=1)
            return

    if cid == "cyclops":
        if "central_pupil" in name:
            draw.rectangle(rect, fill=(20, 17, 12, 255), outline=(250, 228, 138, 255), width=1)
            return
        if "central_eye" in name:
            draw.rectangle(rect, fill=(250, 228, 138, 255), outline=shadow + (255,), width=1)
            draw.rectangle(
                (x1 + max(1, width // 3), y1 + max(1, height // 5), x2 - max(1, width // 3), y2 - max(1, height // 5)),
                fill=(21, 18, 13, 255),
            )
            return
        if "nostril" in name:
            draw.rectangle(rect, fill=(28, 22, 18, 255), outline=shadow + (255,), width=1)
            draw.point((x1, y1), fill=(92, 76, 62, 255))
            return
        if "scar" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 10) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1, y2, x2, y1), fill=(116, 54, 42, 255), width=1)
            draw.point((x1, y1), fill=shade_rgb(accent, -40) + (255,))
            return
        if "tooth" in name:
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=(232, 219, 188, 255), outline=shadow + (255,))
            return
        if "ear" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 12) + (255,), outline=shadow + (255,), width=1)
            draw.rectangle(
                (x1 + max(1, width // 4), y1 + max(1, height // 4), x2 - max(1, width // 4), y2 - max(1, height // 5)),
                fill=shade_rgb(primary, -16) + (255,),
            )
            return
        if "belt" in name or "loincloth" in name:
            leather = (61, 40, 28) if "belt" in name else (78, 48, 32)
            draw.rectangle(rect, fill=leather + (255,), outline=shadow + (255,), width=1)
            for offset in range(2, width, 6):
                draw.line((x1 + offset, y1 + 1, x1 + offset, y2 - 1), fill=shade_rgb(leather, 35) + (255,), width=1)
            return
        if "chest" in name or "belly" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 10) + (255,), outline=shadow + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 1), fill=shade_rgb(shadow, 25) + (255,), width=1)
            draw.line((x1 + 2, y1 + 2, x2 - 2, y2 - 2), fill=shade_rgb(accent, -30) + (255,), width=1)
            return
        if "finger" in name or "toe" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 18) + (255,), outline=shadow + (255,), width=1)
            draw.rectangle((x1 + 1, max(y1 + 1, y2 - max(2, height // 3)), x2 - 1, y2 - 1), fill=shade_rgb(accent, 12) + (255,))
            return
        if "stone_club" in name:
            stone = (96, 94, 88)
            if "handle" in name:
                stone = (75, 64, 54)
            draw.rectangle(rect, fill=stone + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(136, 135, 130, 255), width=1)
            draw.line((x1 + 2, y2 - 3, x2 - 3, y1 + 2), fill=(53, 52, 50, 255), width=1)
            return

    if cid == "centaur":
        skin = (216, 169, 118)
        horse = (138, 86, 42)
        leather = (70, 43, 28)
        hair = (47, 29, 20)
        wood = (83, 50, 30)
        hoof = (45, 34, 30)
        if "eye" in name:
            draw.rectangle(rect, fill=(22, 18, 14, 255), outline=(238, 211, 150, 255), width=1)
            draw.point((x1, y1), fill=(255, 235, 180, 255))
            return
        if "nose" in name:
            draw.rectangle(rect, fill=shade_rgb(skin, -14) + (255,), outline=shadow + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 1), fill=(89, 55, 39, 255), width=1)
            return
        if "mouth" in name:
            draw.rectangle(rect, fill=(64, 36, 31, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 1, (y1 + y2) // 2), fill=(143, 71, 55, 255), width=1)
            return
        if "ear" in name:
            draw.rectangle(rect, fill=shade_rgb(skin, 8) + (255,), outline=shadow + (255,), width=1)
            draw.rectangle(
                (x1 + max(1, width // 4), y1 + max(1, height // 4), x2 - max(1, width // 4), y2 - max(1, height // 5)),
                fill=shade_rgb(skin, -20) + (255,),
            )
            return
        if "hair" in name or "mane" in name:
            draw.rectangle(rect, fill=hair + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(4, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(91, 55, 34, 255), width=1)
            return
        if "quiver_arrow" in name or name == "arrow":
            draw.rectangle(rect, fill=(188, 151, 83, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 2, (y1 + y2) // 2), fill=(245, 221, 150, 255), width=1)
            return
        if "arrow_tip" in name:
            draw.polygon([(x1, (y1 + y2) // 2), (x2, y1), (x2, y2)], fill=(143, 145, 137, 255), outline=shadow + (255,))
            return
        if "bow_string" in name:
            draw.rectangle(rect, fill=(24, 20, 18, 255), outline=(95, 82, 69, 255), width=1)
            return
        if "bow" in name:
            draw.rectangle(rect, fill=wood + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(143, 92, 53, 255), width=1)
            return
        if "quiver" in name or "strap" in name:
            draw.rectangle(rect, fill=leather + (255,), outline=shadow + (255,), width=1)
            for offset in range(2, width, 5):
                draw.line((x1 + offset, y1 + 1, x1 + offset, y2 - 1), fill=(119, 78, 45, 255), width=1)
            return
        if "hoof" in name:
            draw.rectangle(rect, fill=hoof + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=(103, 81, 62, 255), width=1)
            return
        if "finger" in name or "hand" in name or "forearm" in name or "arm" in name or "torso" in name or "waist" in name or "shoulders" in name:
            base = skin
            if "strap" in name:
                base = leather
            draw.rectangle(rect, fill=base + (255,), outline=shadow + (255,), width=1)
            if "torso" in name or "shoulders" in name:
                draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(176, 121, 82, 255), width=1)
            else:
                draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=(165, 108, 73, 255), width=1)
            return
        if "head" in name:
            draw.rectangle(rect, fill=shade_rgb(skin, 2) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 2, y1 + 2, x2 - 2, y1 + 2), fill=(178, 116, 76, 255), width=1)
            return
        if "tail" in name:
            draw.rectangle(rect, fill=hair + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(4, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(96, 58, 35, 255), width=1)
            return
        if any(token in name for token in ("horse", "leg", "belly")):
            base = horse
            if "belly" in name:
                base = (92, 55, 31)
            elif "chest" in name or "shoulder" in name:
                base = (151, 96, 48)
            draw.rectangle(rect, fill=base + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(5, min(width, height) // 3)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(94, 55, 29, 255), width=1)
            return

    if cid == "pegasus":
        if "eye" in name:
            draw.rectangle(rect, fill=(37, 42, 45, 255), outline=(196, 202, 205, 255), width=1)
            draw.point((x1, y1), fill=(245, 247, 246, 255))
            return
        if "nostril" in name:
            draw.rectangle(rect, fill=(68, 74, 78, 255))
            draw.point((x1, y1), fill=(184, 190, 193, 255))
            if x2 > x1 and y2 > y1:
                draw.point((x2, y2), fill=(28, 32, 34, 255))
            return
        if "nose_mouth" in name:
            draw.rectangle(rect, fill=(174, 181, 184, 255), outline=(132, 139, 144, 255), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 1, (y1 + y2) // 2), fill=(72, 78, 82, 255), width=1)
            return
        if "snout" in name:
            draw.rectangle(rect, fill=(232, 235, 232, 255), outline=(172, 178, 181, 255), width=1)
            draw.line((x1 + 2, y1 + 2, x2 - 2, y2 - 2), fill=(198, 204, 207, 255), width=1)
            draw.line((x1 + 1, y2 - 3, x2 - 2, y2 - 3), fill=(143, 151, 156, 255), width=1)
            return
        if "ear" in name:
            draw.rectangle(rect, fill=(240, 242, 238, 255), outline=(172, 178, 181, 255), width=1)
            draw.rectangle(
                (x1 + max(1, width // 4), y1 + max(1, height // 4), x2 - max(1, width // 4), y2 - max(1, height // 5)),
                fill=(207, 212, 214, 255),
            )
            return
        if "mane" in name:
            draw.rectangle(rect, fill=(250, 250, 246, 255), outline=(176, 182, 185, 255), width=1)
            for offset in range(0, width + height, 5):
                draw.line((x1 + offset, y1, x1 + offset - height, y2), fill=(205, 211, 214, 255), width=1)
            return
        if "saddle_space" in name:
            draw.rectangle(rect, fill=(222, 226, 226, 255), outline=(166, 172, 176, 255), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y1 + 1), fill=(248, 249, 246, 255), width=1)
            return
        if "wing" in name or "feather" in name:
            shade = (235, 238, 238) if "feather" not in name else (246, 247, 244)
            draw.rectangle(rect, fill=shade + (255,), outline=(166, 172, 176, 255), width=1)
            for offset in range(-height, width + height, max(4, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(194, 201, 204, 255), width=1)
            if "feather" in name:
                draw.line((x1 + 1, y1 + 1, x2 - 2, y1 + 1), fill=(255, 255, 252, 255), width=1)
                draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 2), fill=(176, 184, 189, 255), width=1)
            else:
                draw.line((x1 + 1, y1 + 2, x2 - 2, y1 + 2), fill=(251, 252, 250, 255), width=1)
                draw.line((x1 + 1, y2 - 3, x2 - 2, y2 - 3), fill=(158, 167, 173, 255), width=1)
            return
        if any(token in name for token in ("body", "chest", "neck", "leg", "hoof", "tail")):
            base = (244, 245, 241)
            if "hoof" in name:
                base = (214, 219, 220)
            elif "neck" in name:
                base = (241, 243, 240)
            elif "chest" in name:
                base = (238, 241, 240)
            draw.rectangle(rect, fill=base + (255,), outline=(174, 180, 184, 255), width=1)
            draw.line((x1 + 2, y1 + 2, x2 - 2, y2 - 2), fill=(202, 208, 211, 255), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=(226, 230, 230, 255), width=1)
            if "neck" in name or "leg" in name:
                draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 2), fill=(185, 193, 198, 255), width=1)
            if "tail" in name:
                for offset in range(0, width + height, 6):
                    draw.line((x1 + offset, y1, x1 + offset - height, y2), fill=(202, 209, 213, 255), width=1)
            return

    if cid == "hydra":
        if "eye" in name:
            draw.rectangle(rect, fill=(236, 222, 74, 255), outline=shadow + (255,), width=1)
            draw.rectangle(
                (x1 + max(1, width // 3), y1 + max(1, height // 5), x2 - max(1, width // 3), y2 - max(1, height // 5)),
                fill=(21, 30, 15, 255),
            )
            return
        if "fang" in name:
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=(235, 238, 205, 255), outline=shadow + (255,))
            return
        if "jaw" in name or "mouth" in name:
            jaw = shade_rgb(accent, -6) if "upper" in name else shade_rgb(accent, -22)
            draw.rectangle(rect, fill=jaw + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - max(2, height // 3), x2 - 2, y2 - max(2, height // 3)), fill=(25, 12, 15, 255), width=1)
            return
        if "horn" in name or "gland" in name:
            draw.rectangle(rect, fill=shade_rgb(accent, 18) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(192, 231, 126, 255), width=1)
            return
        if "head" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 5) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + max(2, height // 4), x2 - 2, y1 + max(2, height // 4)), fill=shade_rgb(accent, -30) + (255,), width=2)
            return
        if "neck" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 2) + (255,), outline=shadow + (255,), width=1)
            for yy in range(y1 + 2, y2, max(3, height // 4)):
                draw.line((x1 + 1, yy, x2 - 2, yy), fill=shade_rgb(accent, -28) + (255,), width=1)
            return
        if "spine_crest" in name:
            draw.rectangle(rect, fill=shade_rgb(accent, 4) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=shade_rgb(accent, 38) + (255,), width=1)
            return
        if "tail" in name or "body" in name or "shoulder" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, -3) + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(5, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=shade_rgb(accent, -36) + (255,), width=1)
            return

    if cid == "manticore":
        if "eye" in name:
            draw.rectangle(rect, fill=(235, 196, 68, 255), outline=shadow + (255,), width=1)
            draw.rectangle(
                (x1 + max(1, width // 3), y1 + max(1, height // 5), x2 - max(1, width // 3), y2 - max(1, height // 5)),
                fill=(18, 14, 14, 255),
            )
            return
        if "fang" in name:
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=(235, 225, 190, 255), outline=shadow + (255,))
            return
        if "nose_mouth" in name:
            draw.rectangle(rect, fill=(35, 23, 24, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 1, (y1 + y2) // 2), fill=(170, 48, 44, 255), width=1)
            return
        if "snout" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 16) + (255,), outline=shadow + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 2, (x1 + x2) // 2, y2 - 2), fill=shade_rgb(shadow, 22) + (255,), width=1)
            draw.line((x1 + 2, y2 - 3, x2 - 2, y2 - 3), fill=(42, 27, 27, 255), width=1)
            return
        if "ear" in name or "cheek_spike" in name:
            ear = shade_rgb(primary, -8) if "ear" in name else shade_rgb(shadow, 10)
            draw.rectangle(rect, fill=ear + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=shade_rgb(accent, 18) + (255,), width=1)
            return
        if "wing_membrane" in name:
            membrane = (42, 89, 51)
            draw.rectangle(rect, fill=membrane + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(5, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(80, 139, 72, 255), width=1)
            return
        if "wing_bone" in name:
            bone = (49, 37, 40)
            draw.rectangle(rect, fill=bone + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(105, 61, 56, 255), width=1)
            return
        if "wing_claw" in name or "claws" in name:
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=(38, 29, 30, 255), outline=shadow + (255,))
            return
        if "tail_sting_tip" in name:
            draw.rectangle(rect, fill=(105, 206, 82, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(184, 242, 120, 255), width=1)
            return
        if "tail_sting" in name or "tail_hook" in name:
            sting = (60, 132, 65)
            draw.rectangle(rect, fill=sting + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(130, 205, 92, 255), width=1)
            return
        if "tail" in name:
            tail = (74, 47, 48)
            draw.rectangle(rect, fill=tail + (255,), outline=shadow + (255,), width=1)
            for yy in range(y1 + 2, y2, max(3, height // 4)):
                draw.line((x1 + 1, yy, x2 - 2, yy), fill=(89, 137, 70, 255), width=1)
            return
        if any(token in name for token in ("body", "chest", "shoulder", "leg", "head")):
            base = shade_rgb(primary, 0)
            if "chest" in name:
                base = shade_rgb(primary, -12)
            elif "shoulder" in name:
                base = shade_rgb(primary, 10)
            draw.rectangle(rect, fill=base + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(5, min(width, height) // 3)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=shade_rgb(shadow, 28) + (255,), width=1)
            return

    if cid == "medusa":
        skin = (186, 184, 174)
        snake = (37, 88, 57)
        snake_dark = (21, 51, 39)
        robe = (72, 92, 84)
        gaze = (205, 244, 205)
        if "pupil" in name:
            draw.rectangle(rect, fill=(232, 255, 232, 255))
            draw.point(((x1 + x2) // 2, (y1 + y2) // 2), fill=(18, 25, 21, 255))
            return
        if name in ("left_eye", "right_eye") or ("snake" not in name and "eye" in name):
            draw.rectangle(rect, fill=gaze + (255,), outline=shadow + (255,), width=1)
            draw.rectangle(
                (x1 + max(1, width // 3), y1 + max(1, height // 5), x2 - max(1, width // 3), y2 - max(1, height // 5)),
                fill=(25, 36, 30, 255),
            )
            return
        if "stone_gaze_aura" in name:
            draw.rectangle(rect, fill=(155, 217, 174, 210), outline=(225, 250, 225, 255), width=1)
            for offset in range(0, width + height, max(3, min(width, height) // 2)):
                draw.line((x1 + offset, y1, x1 + offset - height, y2), fill=(230, 255, 230, 255), width=1)
            return
        if "snake" in name and "eye" in name:
            draw.rectangle(rect, fill=(232, 218, 76, 255), outline=shadow + (255,), width=1)
            inner = (
                x1 + max(1, width // 3),
                y1 + max(1, height // 5),
                x2 - max(1, width // 3),
                y2 - max(1, height // 5),
            )
            if inner[2] >= inner[0] and inner[3] >= inner[1]:
                draw.rectangle(inner, fill=(18, 26, 14, 255))
            else:
                draw.point(((x1 + x2) // 2, (y1 + y2) // 2), fill=(18, 26, 14, 255))
            return
        if "fang" in name:
            draw.rectangle(rect, fill=shadow + (255,))
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=(232, 226, 197, 255), outline=shadow + (255,))
            draw.point(((x1 + x2) // 2, y1), fill=(255, 250, 220, 255))
            return
        if "tongue" in name:
            draw.rectangle(rect, fill=(180, 38, 54, 255), outline=(75, 17, 24, 255), width=1)
            draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 1), fill=(245, 104, 111, 255), width=1)
            return
        if "snake_head" in name:
            draw.rectangle(rect, fill=shade_rgb(snake, 8) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + max(2, height // 4), x2 - 2, y1 + max(2, height // 4)), fill=(105, 159, 91, 255), width=1)
            draw.line((x1 + 1, y2 - max(2, height // 3), x2 - 2, y2 - max(2, height // 3)), fill=(16, 28, 20, 255), width=1)
            return
        if "snake_body" in name or "snake_hair" in name or "hair_crown" in name or "hair_back" in name:
            draw.rectangle(rect, fill=snake_dark + (255,), outline=shadow + (255,), width=1)
            for yy in range(y1 + 2, y2, max(3, height // 5)):
                draw.line((x1 + 1, yy, x2 - 2, yy), fill=(65, 123, 72, 255), width=1)
            return
        if "face_plate" in name or "nose" in name:
            draw.rectangle(rect, fill=skin + (255,), outline=shadow + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 1), fill=(119, 118, 110, 255), width=1)
            return
        if "mouth" in name:
            draw.rectangle(rect, fill=(58, 43, 44, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 1, (y1 + y2) // 2), fill=(128, 76, 80, 255), width=1)
            return
        if "ear" in name or "hand" in name or "forearm" in name or "arm" in name or "shoulder" in name:
            draw.rectangle(rect, fill=shade_rgb(skin, 3) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=(137, 134, 126, 255), width=1)
            return
        if "robe" in name or "torso" in name or "waist_sash" in name:
            base = robe if "sash" not in name else (42, 53, 50)
            draw.rectangle(rect, fill=base + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(122, 141, 129, 255), width=1)
            return
        if "tail" in name or "coil" in name:
            base = snake
            if "tip" in name or "spike" in name:
                base = snake_dark
            draw.rectangle(rect, fill=base + (255,), outline=shadow + (255,), width=1)
            for yy in range(y1 + 2, y2, max(3, height // 4)):
                draw.line((x1 + 1, yy, x2 - 2, yy), fill=(83, 134, 70, 255), width=1)
            for offset in range(-height, width + height, max(6, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(24, 60, 42, 255), width=1)
            return
        if "head" in name:
            draw.rectangle(rect, fill=shade_rgb(skin, -4) + (255,), outline=shadow + (255,), width=1)
            return

    if cid == "colossus":
        bronze = (123, 90, 58)
        bronze_light = (159, 118, 70)
        verdigris = (45, 186, 157)
        dark = (54, 43, 39)
        if "eye_slit" in name:
            draw.rectangle(rect, fill=(32, 202, 168, 255), outline=dark + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 2, (y1 + y2) // 2), fill=(244, 255, 236, 255), width=1)
            return
        if "mouth_groove" in name:
            draw.rectangle(rect, fill=dark + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 1, (y1 + y2) // 2), fill=(107, 72, 48, 255), width=1)
            return
        if "gem" in name:
            draw.rectangle(rect, fill=verdigris + (255,), outline=dark + (255,), width=1)
            draw.point((x1 + 1, y1 + 1), fill=(222, 255, 232, 255))
            return
        if "crown" in name:
            draw.rectangle(rect, fill=bronze_light + (255,), outline=dark + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=verdigris + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(86, 59, 42, 255), width=1)
            return
        if "face_mask" in name or "cheek_plate" in name or name == "head":
            draw.rectangle(rect, fill=bronze + (255,), outline=dark + (255,), width=1)
            draw.line((x1 + 1, y1 + 2, x2 - 2, y1 + 2), fill=bronze_light + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 2), fill=(82, 56, 42, 255), width=1)
            return
        if "chest" in name or "back_plate" in name or "belt" in name or "shoulder_plate" in name:
            base = bronze_light if "chest_plate" in name else bronze
            if "seam" in name or "belt" in name:
                base = (94, 65, 45)
            draw.rectangle(rect, fill=base + (255,), outline=dark + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=verdigris + (255,), width=1)
            draw.line((x1 + 2, y2 - 2, x2 - 2, y1 + 2), fill=(72, 50, 39, 255), width=1)
            return
        if "elbow_ring" in name or "knee_ring" in name:
            draw.rectangle(rect, fill=(74, 55, 43, 255), outline=dark + (255,), width=1)
            for xx in range(x1 + 2, x2, max(3, width // 4)):
                draw.line((xx, y1 + 1, xx, y2 - 1), fill=verdigris + (255,), width=1)
            return
        if "finger" in name or "toe_plate" in name:
            draw.rectangle(rect, fill=(78, 55, 41, 255), outline=dark + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=verdigris + (255,), width=1)
            return
        if "fist" in name or "foot" in name:
            draw.rectangle(rect, fill=(105, 73, 48, 255), outline=dark + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(161, 118, 69, 255), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=verdigris + (255,), width=1)
            return
        if "arm" in name or "forearm" in name or "leg" in name or "shin_plate" in name or "torso_core" in name:
            draw.rectangle(rect, fill=bronze + (255,), outline=dark + (255,), width=1)
            for offset in range(-height, width + height, max(6, min(width, height) // 3)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(91, 63, 44, 255), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y1 + 1), fill=bronze_light + (255,), width=1)
            return

    if cid == "nemean_lion":
        if "eye" in name:
            draw.rectangle(rect, fill=(42, 32, 22, 255), outline=(238, 190, 72, 255), width=1)
            draw.point((x1, y1), fill=(255, 226, 116, 255))
            return
        if "fang" in name:
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=(238, 227, 192, 255), outline=shadow + (255,))
            return
        if "nose_mouth" in name:
            draw.rectangle(rect, fill=(48, 34, 25, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 1, (y1 + y2) // 2), fill=(119, 62, 38, 255), width=1)
            return
        if "snout" in name:
            draw.rectangle(rect, fill=(215, 169, 80, 255), outline=shadow + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 2, (x1 + x2) // 2, y2 - 2), fill=(104, 72, 34, 255), width=1)
            draw.line((x1 + 2, y2 - 3, x2 - 2, y2 - 3), fill=(83, 54, 28, 255), width=1)
            return
        if "mane" in name:
            mane = (231, 178, 68)
            if "outer" in name or "chin" in name:
                mane = (247, 205, 92)
            elif "core" in name:
                mane = (205, 139, 48)
            draw.rectangle(rect, fill=mane + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(4, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(254, 225, 126, 255), width=1)
            return
        if "armor_ridge" in name:
            draw.rectangle(rect, fill=(246, 202, 78, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=(255, 238, 142, 255), width=1)
            return
        if "tail_tuft" in name:
            draw.rectangle(rect, fill=(222, 164, 57, 255), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(4, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(255, 218, 106, 255), width=1)
            return
        if "claws" in name:
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=(42, 30, 24, 255), outline=shadow + (255,))
            return
        if "paw" in name:
            draw.rectangle(rect, fill=(166, 104, 38, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y2 - 2), fill=(62, 40, 25, 255), width=1)
            return
        if any(token in name for token in ("body", "shoulder", "chest", "leg", "tail", "head")):
            base = shade_rgb(primary, 0)
            if "shoulder" in name or "chest" in name:
                base = shade_rgb(primary, -10)
            elif "head" in name:
                base = shade_rgb(primary, 8)
            draw.rectangle(rect, fill=base + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(5, min(width, height) // 3)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=shade_rgb(accent, -22) + (255,), width=1)
            return

    if cid == "chimera":
        if "eye" in name:
            eye = (236, 195, 70) if "serpent" not in name else (228, 224, 78)
            draw.rectangle(rect, fill=eye + (255,), outline=shadow + (255,), width=1)
            draw.rectangle(
                (x1 + max(1, width // 3), y1 + max(1, height // 5), x2 - max(1, width // 3), y2 - max(1, height // 5)),
                fill=(22, 19, 13, 255),
            )
            return
        if "fang" in name or "claws" in name or "wing_claw" in name:
            ivory = (230, 218, 184) if "fang" in name else (42, 32, 26)
            draw.polygon([(x1, y1), (x2, y1), ((x1 + x2) // 2, y2)], fill=ivory + (255,), outline=shadow + (255,))
            return
        if "lion_nose_mouth" in name:
            draw.rectangle(rect, fill=(36, 25, 21, 255), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, (y1 + y2) // 2, x2 - 1, (y1 + y2) // 2), fill=(195, 84, 48, 255), width=1)
            return
        if "lion_snout" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 18) + (255,), outline=shadow + (255,), width=1)
            draw.line(((x1 + x2) // 2, y1 + 2, (x1 + x2) // 2, y2 - 2), fill=shade_rgb(shadow, 24) + (255,), width=1)
            draw.line((x1 + 2, y2 - 3, x2 - 2, y2 - 3), fill=(38, 25, 20, 255), width=1)
            return
        if "lion_mane" in name:
            mane = (54, 31, 25)
            draw.rectangle(rect, fill=mane + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(4, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=shade_rgb(accent, -20) + (255,), width=1)
            return
        if "lion_head" in name:
            draw.rectangle(rect, fill=shade_rgb(primary, 8) + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + max(2, height // 4), x2 - 2, y1 + max(2, height // 4)), fill=shade_rgb(shadow, 14) + (255,), width=1)
            return
        if "goat_horn" in name:
            horn = (221, 204, 164)
            draw.rectangle(rect, fill=horn + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(156, 133, 94, 255), width=1)
            return
        if "goat_beard" in name:
            beard = (105, 91, 76)
            draw.rectangle(rect, fill=beard + (255,), outline=shadow + (255,), width=1)
            for xx in range(x1 + 2, x2, max(3, width // 4)):
                draw.line((xx, y1 + 1, xx, y2 - 1), fill=(58, 47, 40, 255), width=1)
            return
        if "goat" in name:
            goat = (124, 101, 80)
            draw.rectangle(rect, fill=goat + (255,), outline=shadow + (255,), width=1)
            if "snout" in name:
                draw.line((x1 + 2, y2 - 3, x2 - 2, y2 - 3), fill=(45, 35, 30, 255), width=1)
            else:
                draw.line((x1 + 1, y1 + 2, x2 - 2, y2 - 2), fill=(167, 140, 110, 255), width=1)
            return
        if "serpent_upper_jaw" in name or "serpent_lower_jaw" in name:
            jaw = (42, 82, 44) if "upper" in name else (30, 61, 34)
            draw.rectangle(rect, fill=jaw + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y2 - max(2, height // 3), x2 - 2, y2 - max(2, height // 3)), fill=(18, 14, 12, 255), width=1)
            return
        if "serpent_head" in name:
            green = (38, 92, 48)
            draw.rectangle(rect, fill=green + (255,), outline=shadow + (255,), width=1)
            for yy in range(y1 + 2, y2, max(3, height // 4)):
                draw.line((x1 + 1, yy, x2 - 2, yy), fill=(82, 124, 55, 255), width=1)
            return
        if "snake_tongue" in name:
            tongue = (188, 43, 45)
            draw.rectangle(rect, fill=tongue + (255,), outline=(82, 16, 20, 255), width=1)
            draw.line(((x1 + x2) // 2, y1 + 1, (x1 + x2) // 2, y2 - 1), fill=(245, 105, 94, 255), width=1)
            return
        if "wing_membrane" in name:
            membrane = (132, 59, 42)
            draw.rectangle(rect, fill=membrane + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(5, min(width, height) // 2)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=(224, 90, 42, 255), width=1)
            return
        if "wing_bone" in name:
            bone = (58, 36, 30)
            draw.rectangle(rect, fill=bone + (255,), outline=shadow + (255,), width=1)
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=(128, 65, 42, 255), width=1)
            return
        if "tail" in name:
            tail = (49, 82, 42) if "tip" in name or "rise" in name else shade_rgb(primary, -8)
            draw.rectangle(rect, fill=tail + (255,), outline=shadow + (255,), width=1)
            for yy in range(y1 + 2, y2, max(3, height // 4)):
                draw.line((x1 + 1, yy, x2 - 2, yy), fill=(91, 122, 56, 255), width=1)
            return
        if "fire_crest" in name or "chest" in name or "body" in name or "leg" in name:
            base = shade_rgb(primary, 2)
            if "fire_crest" in name:
                base = shade_rgb(accent, 8)
            draw.rectangle(rect, fill=base + (255,), outline=shadow + (255,), width=1)
            for offset in range(-height, width + height, max(5, min(width, height) // 3)):
                draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=shade_rgb(accent, -24) + (255,), width=1)
            return

    if "eye" in name:
        eye_color = accent
        if cid == "pegasus":
            eye_color = (42, 48, 52)
        elif cid == "hydra":
            eye_color = (236, 222, 74)
        elif cid == "cyclops":
            eye_color = (250, 228, 138)
        draw.rectangle(rect, fill=eye_color + (255,))
        pupil_y1 = min(y2, y1 + max(1, height // 4))
        pupil_y2 = max(pupil_y1, y2 - max(0, height // 4))
        pupil_x1 = min(x2, x1 + max(1, width // 3))
        pupil_x2 = max(pupil_x1, x2 - max(1, width // 3))
        draw.rectangle((pupil_x1, pupil_y1, pupil_x2, pupil_y2), fill=shadow + (255,))
    if "nostril" in name or "pupil" in name:
        draw.rectangle(rect, fill=shadow + (255,), outline=shade_rgb(primary, 18) + (255,), width=1)
        cx = (x1 + x2) // 2
        draw.line((cx, y1, cx, y2), fill=shade_rgb(accent, 28) + (255,), width=1)
    if "bandage" in name or "wrap" in name:
        linen = shade_rgb(primary, 18)
        draw.rectangle(rect, fill=linen + (255,), outline=shadow + (255,), width=1)
        step = max(2, min(width, height) // 2)
        for offset in range(-height, width + height, step):
            draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=shade_rgb(shadow, 36) + (255,), width=1)
    biological_head = "club" not in name and ("head" in name or name in {"face", "lion_head", "goat_head", "snake_head"})
    if biological_head:
        if cid == "cyclops":
            draw_single_eye(draw, rect, (250, 228, 138))
        elif cid == "hydra":
            draw_eye_pair(draw, rect, (236, 222, 74))
        elif cid == "pegasus":
            draw_eye_pair(draw, rect, (52, 58, 64))
        else:
            draw_eye_pair(draw, rect, shadow)
    if any(token in name for token in ("mane", "hair", "tail", "wing", "feather", "plate", "armor", "body")):
        step = max(3, min(width, height) // 4)
        for offset in range(-height, width, step):
            draw.line((x1 + offset, y2, x1 + offset + height, y1), fill=shade_rgb(accent, -20) + (255,), width=1)
    if any(token in name for token in ("snout", "jaw", "mouth")):
        detail_y1 = min(y2, y1 + max(1, height // 2))
        detail_y2 = max(detail_y1, y2 - max(0, height // 5))
        detail_x1 = min(x2, x1 + max(1, width // 4))
        detail_x2 = max(detail_x1, x2 - max(1, width // 4))
        draw.rectangle((detail_x1, detail_y1, detail_x2, detail_y2), fill=shade_rgb(accent, 8) + (255,))
    if "horn" in name or "sting" in name or "claw" in name:
        draw.polygon([(x1, y2), ((x1 + x2) // 2, y1), (x2, y2)], fill=accent + (255,), outline=shadow + (255,))
    if "muzzle" in name or "brow" in name:
        draw.rectangle(rect, fill=shade_rgb(primary, 12) + (255,), outline=shadow + (255,))
        draw.line((x1 + 2, y2 - 3, x2 - 2, y2 - 3), fill=shadow + (255,), width=1)
        draw.line(((x1 + x2) // 2, y1 + 2, (x1 + x2) // 2, y2 - 2), fill=shade_rgb(shadow, 16) + (255,), width=1)
    if "labrys" in name:
        metal = (128, 132, 126) if "head" in name else shade_rgb(shadow, 18)
        draw.rectangle(rect, fill=metal + (255,), outline=shadow + (255,), width=1)
        if "head" in name:
            draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=shade_rgb(accent, 10) + (255,), width=1)
            draw.line((x1 + 1, y2 - 2, x2 - 2, y1 + 1), fill=shade_rgb(shadow, 35) + (255,), width=1)
    if "eye_slit" in name:
        draw.rectangle(rect, fill=(38, 218, 178, 255), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, (y1 + y2) // 2, x2 - 2, (y1 + y2) // 2), fill=(245, 255, 236, 255), width=1)
    if cid == "colossus" and any(token in name for token in ("plate", "belt", "seam", "crown")):
        draw.rectangle(rect, fill=shade_rgb(primary, 6) + (255,), outline=shadow + (255,), width=1)
        draw.line((x1 + 1, y1 + 1, x2 - 2, y2 - 2), fill=accent + (255,), width=1)
        draw.line((x1 + 2, y2 - 2, x2 - 2, y1 + 2), fill=shade_rgb(shadow, 28) + (255,), width=1)
    if "arrow" in name:
        mid_y = (y1 + y2) // 2
        draw.line((x1 + 1, mid_y, x2 - 2, mid_y), fill=shadow + (255,), width=max(1, height // 3))
        draw.polygon([(x2 - 4, y1 + 1), (x2 - 1, mid_y), (x2 - 4, y2 - 1)], fill=accent + (255,), outline=shadow + (255,))
        draw.line((x1 + 1, y1 + 1, x1 + 4, mid_y), fill=shade_rgb(accent, -30) + (255,), width=1)
        draw.line((x1 + 1, y2 - 1, x1 + 4, mid_y), fill=shade_rgb(accent, -30) + (255,), width=1)


def draw_runtime_textures() -> None:
    for creature in CREATURES:
        cid = creature["id"]
        path = texture_path(cid)
        path.parent.mkdir(parents=True, exist_ok=True)
        primary = hex_to_rgb(creature["primary"])
        shadow = hex_to_rgb(creature["shadow"])
        accent = hex_to_rgb(creature["accent"])
        image = Image.new("RGBA", (128, 128), shade_rgb(primary, -22) + (255,))
        draw = ImageDraw.Draw(image)
        for row in creature["geometry"]:
            name, _group, _frm, _to, _uv = row
            footprint = clipped_box(list(uv_footprint(row)))
            if footprint is None:
                continue
            x1, y1, x2, y2 = footprint
            fill = shade_rgb(primary, (sum(ord(ch) for ch in name) % 29) - 9)
            if cid == "cyclops" and "stone_club" in name:
                fill = (96, 94, 88)
            draw.rectangle((x1, y1, x2 - 1, y2 - 1), fill=fill + (255,), outline=shadow + (255,), width=1)
            decorate_texture_region(draw, creature, row, (x1, y1, x2 - 1, y2 - 1))
        if cid == "colossus":
            for x in range(8, 122, 18):
                draw.line((x, 8, x + 22, 118), fill=accent + (255,), width=2)
        elif cid == "pegasus":
            for x in range(8, 128, 10):
                draw.line((x, 66, x + 14, 86), fill=shade_rgb(shadow, 20) + (255,), width=1)
        elif cid == "hydra":
            for y in range(8, 120, 10):
                draw.line((4, y, 124, y + 5), fill=shade_rgb(accent, -35) + (255,), width=1)
        image.save(path)


def read_bbmodel(cid: str) -> dict[str, Any]:
    return json.loads((BLOCKBENCH_DIR / f"{cid}.bbmodel").read_text(encoding="utf-8"))


def face_uv_rect(element: dict[str, Any], face: str) -> list[int] | None:
    faces = element.get("faces", {})
    if face in faces:
        uv = faces[face].get("uv")
        if isinstance(uv, list) and len(uv) == 4:
            return [int(round(value)) for value in uv]
    uv_offset = element.get("uv_offset")
    frm = element.get("from")
    to = element.get("to")
    if isinstance(uv_offset, list) and isinstance(frm, list) and isinstance(to, list):
        generated = box_uv_faces(uv_offset, frm, to)
        uv = generated.get(face, {}).get("uv")
        if uv:
            return [int(round(value)) for value in uv]
    return None


def clamp_texture_rect(coords: list[int], width: int = 128, height: int = 128) -> tuple[int, int, int, int] | None:
    x1, y1, x2, y2 = coords
    x1 = max(0, min(width - 1, x1))
    y1 = max(0, min(height - 1, y1))
    x2 = max(0, min(width, x2))
    y2 = max(0, min(height, y2))
    if x2 <= x1 or y2 <= y1:
        return None
    return x1, y1, x2, y2


def texture_crop_for_face(texture: Image.Image, element: dict[str, Any], face: str) -> Image.Image:
    uv = face_uv_rect(element, face)
    if uv is None:
        return Image.new("RGBA", (1, 1), (196, 0, 196, 255))
    rect = clamp_texture_rect(uv, texture.width, texture.height)
    if rect is None:
        return Image.new("RGBA", (1, 1), (196, 0, 196, 255))
    return texture.crop(rect)


def element_uv_footprint_from_faces(element: dict[str, Any]) -> tuple[int, int, int, int] | None:
    rects: list[tuple[int, int, int, int]] = []
    for face in ("north", "south", "east", "west", "up", "down"):
        uv = face_uv_rect(element, face)
        if uv is None:
            continue
        rect = clamp_texture_rect(uv)
        if rect is not None:
            rects.append(rect)
    if not rects:
        return None
    return (
        min(rect[0] for rect in rects),
        min(rect[1] for rect in rects),
        max(rect[2] for rect in rects),
        max(rect[3] for rect in rects),
    )


def analyze_texture_fit() -> dict[str, Any]:
    payload: dict[str, Any] = {
        "version": 1,
        "workflow": "blockbench_texture_fit",
        "rule": "Every .bbmodel element must have in-bounds UVs and a nonblank opaque texture footprint.",
        "creatures": [],
    }
    for creature in CREATURES:
        cid = creature["id"]
        failures: list[str] = []
        elements_report: list[dict[str, Any]] = []
        texture = Image.open(texture_path(cid)).convert("RGBA")
        model = read_bbmodel(cid)
        overlay = texture.resize((512, 512), RESAMPLE_NEAREST)
        draw = ImageDraw.Draw(overlay)
        font = load_font(8)
        elements = model.get("elements", [])
        opaque_ratios: list[float] = []
        color_variations: list[int] = []
        for index, element in enumerate(elements):
            name = str(element.get("name", f"element_{index}"))
            face_records = []
            face_bounds_ok = True
            for face in ("north", "south", "east", "west", "up", "down"):
                uv = face_uv_rect(element, face)
                bounds_ok = False
                if uv is not None:
                    x1, y1, x2, y2 = uv
                    bounds_ok = x2 > x1 and y2 > y1 and x1 >= 0 and y1 >= 0 and x2 <= texture.width and y2 <= texture.height
                if not bounds_ok:
                    face_bounds_ok = False
                    failures.append(f"{cid}:{name}:{face} UV is missing, empty, or out of bounds")
                face_records.append({"face": face, "uv": uv, "bounds_ok": bounds_ok})
            footprint = element_uv_footprint_from_faces(element)
            opaque_ratio = 0.0
            color_variation = 0
            footprint_ok = False
            if footprint is None:
                failures.append(f"{cid}:{name} has no valid UV footprint")
            else:
                x1, y1, x2, y2 = footprint
                crop = texture.crop(footprint)
                pixels = image_pixels(crop)
                opaque = sum(1 for pixel in pixels if pixel[3] > 0)
                opaque_ratio = opaque / max(1, len(pixels))
                unique_colors = {pixel[:3] for pixel in pixels if pixel[3] > 0}
                color_variation = len(unique_colors)
                opaque_ratios.append(opaque_ratio)
                color_variations.append(color_variation)
                footprint_ok = opaque_ratio >= 0.90 and color_variation >= 2
                sx1, sy1, sx2, sy2 = [value * 4 for value in footprint]
                outline = (40 + (index * 31) % 180, 70 + (index * 47) % 150, 80 + (index * 59) % 140, 255)
                draw.rectangle((sx1, sy1, sx2, sy2), outline=outline, width=2)
                draw.text((sx1 + 3, sy1 + 3), name[:18], fill=(20, 20, 20, 255), font=font)
                if opaque_ratio < 0.90:
                    failures.append(f"{cid}:{name} texture footprint is partially transparent ({opaque_ratio:.2f})")
                if color_variation < 2:
                    failures.append(f"{cid}:{name} texture footprint is visually blank")
            elements_report.append({
                "name": name,
                "uv_footprint": list(footprint) if footprint else None,
                "face_bounds_ok": face_bounds_ok,
                "opaque_ratio": round(opaque_ratio, 4),
                "color_variation": color_variation,
                "fit_ok": face_bounds_ok and footprint_ok,
                "faces": face_records,
            })
        draw.rectangle((0, 0, 511, 24), fill=(250, 247, 239, 230))
        draw.text((6, 6), f"{cid} texture + UV fit", fill=(20, 20, 20, 255), font=font)
        fit_path = PREVIEW_DIR / cid / "texture_fit.png"
        fit_path.parent.mkdir(parents=True, exist_ok=True)
        overlay.save(fit_path)
        payload["creatures"].append({
            "id": cid,
            "texture": str(texture_path(cid).relative_to(ROOT)),
            "bbmodel": str((BLOCKBENCH_DIR / f"{cid}.bbmodel").relative_to(ROOT)),
            "texture_fit_preview": str(fit_path.relative_to(ROOT)),
            "elements_checked": len(elements),
            "average_opaque_ratio": round(sum(opaque_ratios) / max(1, len(opaque_ratios)), 4),
            "average_color_variation": round(sum(color_variations) / max(1, len(color_variations)), 2),
            "failures": failures,
            "ok": not failures,
            "elements": elements_report,
        })
    payload["ok"] = all(item["ok"] for item in payload["creatures"])
    return payload


def write_texture_fit_report() -> dict[str, Any]:
    TEXTURE_FIT_REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    payload = analyze_texture_fit()
    TEXTURE_FIT_REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return payload


def load_texture_fit_report() -> dict[str, Any]:
    if not TEXTURE_FIT_REPORT_PATH.exists():
        return {"version": 1, "creatures": []}
    return json.loads(TEXTURE_FIT_REPORT_PATH.read_text(encoding="utf-8"))


def transform_point(point: tuple[float, float, float], view: str) -> tuple[float, float]:
    x, y, z = point
    if view == "front":
        return x, y
    if view == "side":
        return z, y
    if view == "three_quarter":
        return x * 0.78 - z * 0.52, y
    return x, y


def view_depth(corners: list[tuple[float, float, float]], view: str) -> float:
    if view == "front":
        return sum(corner[2] for corner in corners) / len(corners)
    if view == "side":
        return sum(corner[0] for corner in corners) / len(corners)
    if view == "three_quarter":
        return sum(corner[0] * 0.52 + corner[2] * 0.78 for corner in corners) / len(corners)
    return 0.0


def draw_near_parts_last(view: str) -> bool:
    return view in {"front", "three_quarter"}


DETAIL_PART_TOKENS = (
    "eye", "pupil", "mouth", "nose", "nostril", "snout", "jaw", "fang", "tooth",
    "horn", "ear", "tongue", "poison", "gland", "feather", "wing", "mane",
    "club", "axe", "blade", "handle", "finger", "toe", "hoof", "claw",
    "tail_tip", "saddle_space",
)

FOCUS_GROUPS: dict[str, list[tuple[str, tuple[str, ...]]]] = {
    "minotaur": [
        ("head face", ("head", "muzzle", "eye", "nostril", "mouth", "horn", "ear")),
        ("labrys", ("labrys", "blade", "handle", "socket", "counterweight")),
        ("hooves", ("hoof", "claw")),
    ],
    "cyclops": [
        ("single eye face", ("head", "central_eye", "central_pupil", "nostril", "mouth", "tooth")),
        ("stone club", ("stone_club", "club_lump", "club_knob")),
        ("digits", ("finger", "toe")),
    ],
    "centaur": [
        ("human face", ("head", "eye", "nose", "mouth", "ear", "hair")),
        ("archer gear", ("bow", "arrow", "quiver")),
        ("horse body", ("horse_", "hoof", "tail")),
    ],
    "hydra": [
        ("nine-head V3 target", ("head", "eye", "jaw", "fang", "horn", "poison")),
        ("neck fan", ("neck", "throat")),
        ("low body tail", ("body", "spine", "tail")),
    ],
    "medusa": [
        ("gorgon face", ("face", "eye", "pupil", "gaze", "mouth", "nose")),
        ("snake hair", ("snake", "fang", "tongue")),
        ("serpent tail", ("tail", "coil", "spike")),
    ],
    "nemean_lion": [
        ("lion face", ("head", "snout", "eye", "fang", "nose")),
        ("golden mane", ("mane",)),
        ("paws tail", ("paw", "claw", "tail")),
    ],
    "manticore": [
        ("beast face", ("head", "snout", "eye", "fang", "ear", "spike")),
        ("layered wings", ("wing", "membrane", "bone", "claw")),
        ("scorpion tail", ("tail", "sting")),
    ],
    "chimera": [
        ("lion/goat heads", ("lion_", "goat_", "eye", "horn", "beard")),
        ("serpent tail", ("serpent", "snake", "tail")),
        ("fire wings", ("wing", "membrane", "bone")),
    ],
    "colossus": [
        ("bronze face", ("face", "eye", "mouth", "crown", "cheek")),
        ("armor plates", ("plate", "gem", "seam", "belt")),
        ("heavy limbs", ("finger", "toe", "ring", "shin")),
    ],
    "pegasus": [
        ("horse head", ("head", "snout", "eye", "ear", "nose", "mouth", "neck", "mane")),
        ("layered wings", ("wing", "feather", "coverts")),
        ("rider space", ("saddle_space", "body", "tail", "hoof")),
    ],
}


def is_detail_part(name: str) -> bool:
    lowered = name.lower()
    return any(token in lowered for token in DETAIL_PART_TOKENS)


def focus_groups_for(creature: dict[str, Any]) -> list[tuple[str, tuple[str, ...]]]:
    explicit = FOCUS_GROUPS.get(creature["id"])
    if explicit:
        return explicit
    return [
        ("face / head", (
            "head", "face", "eye", "pupil", "mouth", "nose", "snout", "beak",
            "jaw", "fang", "tooth", "horn", "ear", "mask",
        )),
        ("signature traits", (
            "wing", "feather", "tail", "mane", "crest", "spike", "antler", "horn",
            "tusk", "scale", "shell", "crystal", "sun", "scarab", "scepter", "staff",
            "sword", "shield", "weapon", "gill", "fin", "ray", "serpent", "fox",
        )),
        ("body / motion", (
            "body", "torso", "chest", "arm", "hand", "finger", "leg", "hoof", "paw",
            "claw", "foot", "toe", "tail", "fin", "wing", "flight", "robe", "belt",
        )),
    ]


def union_rect(rects: list[tuple[float, float, float, float]]) -> tuple[float, float, float, float] | None:
    if not rects:
        return None
    return (
        min(rect[0] for rect in rects),
        min(rect[1] for rect in rects),
        max(rect[2] for rect in rects),
        max(rect[3] for rect in rects),
    )


def expand_screen_rect(rect: tuple[float, float, float, float], amount: float,
                       width: int, height: int) -> tuple[int, int, int, int]:
    x1, y1, x2, y2 = rect
    return (
        max(0, int(math.floor(x1 - amount))),
        max(0, int(math.floor(y1 - amount))),
        min(width, int(math.ceil(x2 + amount))),
        min(height, int(math.ceil(y2 + amount))),
    )


def draw_detail_loupes(image: Image.Image, base_render: Image.Image, creature: dict[str, Any],
                       screen_boxes: dict[str, tuple[float, float, float, float]],
                       font: ImageFont.ImageFont) -> None:
    groups = focus_groups_for(creature)
    if not groups:
        return
    draw = ImageDraw.Draw(image)
    panel_x = 972
    panel_y = 84
    panel_w = 272
    panel_h = 188
    draw.rectangle((948, 54, 1260, 704), fill=(250, 247, 239, 238), outline=creature["shadow"], width=2)
    draw.text((966, 66), "Blockbench detail checks", fill=(30, 25, 20), font=font)
    for index, (label, tokens) in enumerate(groups[:3]):
        y = panel_y + index * (panel_h + 18)
        matched = [
            rect for name, rect in screen_boxes.items()
            if any(token in name.lower() for token in tokens)
        ]
        focus = union_rect(matched)
        draw.rectangle((panel_x, y, panel_x + panel_w, y + panel_h), fill=(242, 238, 229, 255),
                       outline=creature["shadow"], width=1)
        draw.text((panel_x + 8, y + 8), label, fill=(30, 25, 20), font=font)
        if focus is None:
            draw.text((panel_x + 8, y + 34), "missing focus parts", fill=(120, 30, 30), font=font)
            continue
        crop_box = expand_screen_rect(focus, 18.0, 940, image.height)
        if crop_box[2] <= crop_box[0] or crop_box[3] <= crop_box[1]:
            continue
        crop = base_render.crop(crop_box)
        crop.thumbnail((panel_w - 18, panel_h - 42), RESAMPLE_NEAREST)
        cx = panel_x + (panel_w - crop.width) // 2
        cy = y + 36 + (panel_h - 42 - crop.height) // 2
        image.alpha_composite(crop, (cx, cy))
        draw.rectangle((cx, cy, cx + crop.width, cy + crop.height), outline=creature["shadow"], width=1)


def draw_previews() -> None:
    font = load_font(10)
    for creature in CREATURES:
        cid = creature["id"]
        model = read_bbmodel(cid)
        elements_by_name = {str(element.get("name")): element for element in model.get("elements", [])}
        texture = Image.open(texture_path(cid)).convert("RGBA")
        for view in VIEWS:
            out = PREVIEW_DIR / cid / "blockbench" / f"{view}.png"
            out.parent.mkdir(parents=True, exist_ok=True)
            image = Image.new("RGBA", (1280, 760), (242, 238, 229, 255))
            draw = ImageDraw.Draw(image)
            draw.text((24, 22), f"{creature['display']} - {VIEW_LABEL[view]} - textured Blockbench gate", fill=(30, 25, 20), font=font)
            boxes = []
            for row in creature["geometry"]:
                name, group, frm, to, uv = row
                corners = [
                    (frm[0], frm[1], frm[2]),
                    (frm[0], frm[1], to[2]),
                    (frm[0], to[1], frm[2]),
                    (frm[0], to[1], to[2]),
                    (to[0], frm[1], frm[2]),
                    (to[0], frm[1], to[2]),
                    (to[0], to[1], frm[2]),
                    (to[0], to[1], to[2]),
                ]
                pts = [transform_point(corner, view) for corner in corners]
                xs = [p[0] for p in pts]
                ys = [p[1] for p in pts]
                depth = view_depth(corners, view)
                boxes.append((depth, min(xs), min(ys), max(xs), max(ys), name))
            boxes.sort(key=lambda item: item[0], reverse=draw_near_parts_last(view))
            all_x = [value for _, x1, _, x2, _, _ in boxes for value in (x1, x2)]
            all_y = [value for _, _, y1, _, y2, _ in boxes for value in (y1, y2)]
            min_x, max_x = min(all_x), max(all_x)
            min_y, max_y = min(all_y), max(all_y)
            scale = min(850 / max(1.0, max_x - min_x), 610 / max(1.0, max_y - min_y))
            ox = 470 - (min_x + max_x) * 0.5 * scale
            oy = 690 - max_y * scale
            screen_boxes: dict[str, tuple[float, float, float, float]] = {}
            for _, x1, y1, x2, y2, name in boxes:
                sx1 = ox + x1 * scale
                sy1 = oy + y1 * scale
                sx2 = ox + x2 * scale
                sy2 = oy + y2 * scale
                if is_detail_part(name):
                    center_x = (sx1 + sx2) * 0.5
                    center_y = (sy1 + sy2) * 0.5
                    min_size = 5.0
                    if sx2 - sx1 < min_size:
                        sx1 = center_x - min_size * 0.5
                        sx2 = center_x + min_size * 0.5
                    if sy2 - sy1 < min_size:
                        sy1 = center_y - min_size * 0.5
                        sy2 = center_y + min_size * 0.5
                element = elements_by_name.get(name)
                face = VIEW_FACE[view]
                if element is not None:
                    crop = texture_crop_for_face(texture, element, face)
                    resized = crop.resize((max(1, int(sx2 - sx1)), max(1, int(sy2 - sy1))), RESAMPLE_NEAREST)
                    image.alpha_composite(resized, (int(sx1), int(sy1)))
                else:
                    draw.rectangle((sx1, sy1, sx2, sy2), fill=creature["primary"] + "CC")
                outline_width = 3 if is_detail_part(name) else 2
                draw.rectangle((sx1, sy1, sx2, sy2), outline=creature["shadow"], width=outline_width)
                screen_boxes[name] = (sx1, sy1, sx2, sy2)
            draw.line((80, 690, 900, 690), fill=(170, 160, 145), width=2)
            base_render = image.copy()
            draw_detail_loupes(image, base_render, creature, screen_boxes, font)
            image.save(out)


def draw_contact_sheet() -> None:
    thumb_w, thumb_h = 426, 253
    label_h = 34
    sheet = Image.new("RGBA", (thumb_w * 3, (thumb_h + label_h) * len(CREATURES)), (242, 238, 229, 255))
    draw = ImageDraw.Draw(sheet)
    font = load_font(10)
    for row, creature in enumerate(CREATURES):
        cid = creature["id"]
        y = row * (thumb_h + label_h)
        for col, view in enumerate(VIEWS):
            path = PREVIEW_DIR / cid / "blockbench" / f"{view}.png"
            image = Image.open(path).convert("RGBA").resize((thumb_w, thumb_h), RESAMPLE_NEAREST)
            x = col * thumb_w
            sheet.alpha_composite(image, (x, y + label_h))
            draw.rectangle((x, y, x + thumb_w, y + label_h), fill=(34, 30, 24, 255))
            draw.text((x + 8, y + 10), f"{cid} - {view}", fill=(255, 255, 255, 255), font=font)
    CONTACT_SHEET_PATH.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(CONTACT_SHEET_PATH)


def draw_concept_contact_sheet() -> None:
    thumb_w, thumb_h = 440, 286
    label_h = 34
    columns = 3
    rows = math.ceil(len(CREATURES) / columns)
    sheet = Image.new("RGBA", (thumb_w * columns, (thumb_h + label_h) * rows), (242, 238, 229, 255))
    draw = ImageDraw.Draw(sheet)
    font = load_font(11)
    for index, creature in enumerate(CREATURES):
        cid = creature["id"]
        row = index // columns
        col = index % columns
        x = col * thumb_w
        y = row * (thumb_h + label_h)
        path = CONCEPT_DIR / f"{cid}_concept.png"
        image = Image.open(path).convert("RGBA").resize((thumb_w, thumb_h), RESAMPLE_NEAREST)
        sheet.alpha_composite(image, (x, y + label_h))
        draw.rectangle((x, y, x + thumb_w, y + label_h), fill=(34, 30, 24, 255))
        draw.text((x + 8, y + 10), f"{cid} concept", fill=(255, 255, 255, 255), font=font)
    CONCEPT_CONTACT_SHEET_PATH.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(CONCEPT_CONTACT_SHEET_PATH)


def audit() -> int:
    failures: list[str] = []
    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8")) if MANIFEST_PATH.exists() else None
    if manifest is None:
        failures.append(f"Missing manifest: {MANIFEST_PATH}")
    if not TEXTURE_FIT_REPORT_PATH.exists():
        failures.append(f"Missing texture fit report: {TEXTURE_FIT_REPORT_PATH}")
        texture_fit = {"creatures": []}
    else:
        texture_fit = json.loads(TEXTURE_FIT_REPORT_PATH.read_text(encoding="utf-8"))
        if not texture_fit.get("ok", False):
            failures.append(f"Texture fit report has failures: {TEXTURE_FIT_REPORT_PATH}")
    if not CONTACT_SHEET_PATH.exists():
        failures.append(f"Missing Blockbench contact sheet: {CONTACT_SHEET_PATH}")
    if not CONCEPT_CONTACT_SHEET_PATH.exists():
        failures.append(f"Missing concept contact sheet: {CONCEPT_CONTACT_SHEET_PATH}")
    fit_by_id = {item.get("id"): item for item in texture_fit.get("creatures", [])}
    entity_source = MOD_ENTITIES_PATH.read_text(encoding="utf-8") if MOD_ENTITIES_PATH.exists() else ""
    for creature in CREATURES:
        cid = creature["id"]
        design = DESIGN_DIR / f"{cid}.md"
        concept = CONCEPT_DIR / f"{cid}_concept.png"
        bbmodel = BLOCKBENCH_DIR / f"{cid}.bbmodel"
        uv_template = PREVIEW_DIR / cid / "uv_template.png"
        texture = texture_path(cid)
        for path in (design, concept, bbmodel, uv_template, texture):
            if not path.exists():
                failures.append(f"{cid}: missing {path}")
        fit_preview = PREVIEW_DIR / cid / "texture_fit.png"
        if not fit_preview.exists():
            failures.append(f"{cid}: missing texture fit preview")
        fit_summary = fit_by_id.get(cid)
        if not fit_summary:
            failures.append(f"{cid}: missing texture fit summary")
        elif not fit_summary.get("ok", False):
            failures.extend(f"{cid}: {failure}" for failure in fit_summary.get("failures", []))
        if design.exists():
            text = design.read_text(encoding="utf-8")
            for heading in (
                    "目标体量", "轮廓语言", "概念设计图", "核心识别特征", "部件清单",
                    "Blockbench 层级", "建模硬性要求", "UV 计划", "纹理细节", "动画约束",
                    "返工触发条件", "验收口径"
            ):
                if f"## {heading}" not in text:
                    failures.append(f"{cid}: design doc missing section {heading}")
            if creature.get("v2_design_freeze") and "## V2 造型冻结" not in text:
                failures.append(f"{cid}: design doc missing V2 造型冻结 section")
            for marker in creature.get("v2_design_freeze", []):
                if marker not in text:
                    failures.append(f"{cid}: design doc missing V2 marker {marker}")
        if concept.exists():
            image = Image.open(concept).convert("RGBA")
            if image.size[0] < 1000 or image.size[1] < 700:
                failures.append(f"{cid}: concept art is too small")
            pixels = image_pixels(image)
            opaque = sum(1 for pixel in pixels if pixel[3] > 0)
            unique_colors = {pixel[:3] for pixel in pixels if pixel[3] > 0}
            if opaque < len(pixels) * 0.95 or len(unique_colors) < 20:
                failures.append(f"{cid}: concept art appears blank or under-detailed")
        if bbmodel.exists():
            data = json.loads(bbmodel.read_text(encoding="utf-8"))
            names = {element.get("name") for element in data.get("elements", [])}
            for row in creature["geometry"]:
                if row[0] not in names:
                    failures.append(f"{cid}: bbmodel missing element {row[0]}")
            for element in data.get("elements", []):
                uv = element.get("uv_offset", [0, 0])
                frm = element.get("from", [0, 0, 0])
                to = element.get("to", [0, 0, 0])
                sx = math.ceil(abs(to[0] - frm[0]))
                sy = math.ceil(abs(to[1] - frm[1]))
                sz = math.ceil(abs(to[2] - frm[2]))
                if uv[0] + 2 * sz + 2 * sx > 128 or uv[1] + sz + sy > 128:
                    failures.append(f"{cid}: UV overflow on {element.get('name')}")
        texture = texture_path(cid)
        if texture.exists():
            image = Image.open(texture).convert("RGBA")
            for row in creature["geometry"]:
                rect = clipped_box(list(uv_footprint(row)))
                if rect is None:
                    failures.append(f"{cid}: invalid UV footprint on {row[0]}")
                    continue
                x1, y1, x2, y2 = rect
                pixels = image_pixels(image.crop((x1, y1, x2, y2)))
                if not pixels:
                    failures.append(f"{cid}: empty UV crop on {row[0]}")
                    continue
                opaque = sum(1 for pixel in pixels if pixel[3] > 0)
                if opaque < max(1, int(len(pixels) * 0.90)):
                    failures.append(f"{cid}: transparent key UV region on {row[0]}")
                unique_colors = {pixel[:3] for pixel in pixels if pixel[3] > 0}
                if len(unique_colors) < 2:
                    failures.append(f"{cid}: visually blank key UV region on {row[0]}")
        target_w, target_h = creature["target_box"]
        entity_constant = cid.upper()
        if cid == "nemean_lion":
            entity_constant = "NEMEAN_LION"
        pattern = (
            rf"{entity_constant}\s*=\s*Registry\.register\([\s\S]*?"
            rf"\.dimensions\(EntityDimensions\.fixed\({target_w}f,\s*{target_h}f\)\)"
        )
        if entity_source and not __import__("re").search(pattern, entity_source):
            failures.append(f"{cid}: EntityDimensions does not match design target {target_w} x {target_h}")
        for view in VIEWS:
            preview = PREVIEW_DIR / cid / "blockbench" / f"{view}.png"
            if not preview.exists():
                failures.append(f"{cid}: missing blockbench preview {view}")
    if failures:
        print("Greek Creature Visual Gate: FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Greek Creature Visual Gate: PASS")
    print(f"manifest: {MANIFEST_PATH}")
    print(f"design_dir: {DESIGN_DIR}")
    print(f"blockbench_dir: {BLOCKBENCH_DIR}")
    print(f"preview_dir: {PREVIEW_DIR}")
    print(f"texture_fit_report: {TEXTURE_FIT_REPORT_PATH}")
    print(f"contact_sheet: {CONTACT_SHEET_PATH}")
    return 0


def generate_all() -> None:
    write_design_docs()
    draw_concept_art()
    write_bbmodels()
    draw_runtime_textures()
    draw_uv_templates()
    write_texture_fit_report()
    draw_previews()
    draw_contact_sheet()
    draw_concept_contact_sheet()
    write_manifest()
    write_visual_report()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["all", "designs", "concepts", "manifest", "bbmodels", "uv", "textures", "texture-fit", "previews", "contact-sheet", "concept-contact-sheet", "report", "audit"])
    args = parser.parse_args()
    if args.command == "all":
        generate_all()
        return audit()
    if args.command == "designs":
        write_design_docs()
    elif args.command == "concepts":
        draw_concept_art()
    elif args.command == "manifest":
        write_manifest()
    elif args.command == "bbmodels":
        write_bbmodels()
    elif args.command == "uv":
        draw_uv_templates()
    elif args.command == "textures":
        draw_runtime_textures()
    elif args.command == "texture-fit":
        write_texture_fit_report()
    elif args.command == "previews":
        draw_previews()
    elif args.command == "contact-sheet":
        draw_contact_sheet()
    elif args.command == "concept-contact-sheet":
        draw_concept_contact_sheet()
    elif args.command == "report":
        write_visual_report()
    elif args.command == "audit":
        return audit()
    return 0


if __name__ == "__main__":
    sys.exit(main())
