"""哨兵石像（GP-GR-03，T13 阶段 ③）部件表。

生成 art/blockbench/greek/sentinel/sentinel.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。

造型（T13 设计文档 §4.2）：约 2 格高的哈迪斯神殿守卫立像——黑石身体、青铜头盔（顶冠黑鬃）与胸甲、
冥界暗绿刻纹；立在黑石方台上；双手持青铜反曲弓。静止姿势：左前臂向前平端，弓竖直穿过左拳（弓弦朝向身体），
右臂自然下垂。弓弦分上下两段（各自的骨骼在弓梢），拉弓时绕弓梢转动并按 1/cos 拉长，弦的中点被拉到右手。
眼睛是两道暗绿刻痕（贴图），不加框。
运行：python3 tools/blockbench/params/sentinel_parts.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/sentinel/sentinel.params.json"

STONE = "#30333a"
STONE_HI = "#41454f"
STONE_DARK = "#24262c"
BRONZE = "#93693a"
BRONZE_HI = "#b28449"
BRONZE_DARK = "#694a26"
CREST = "#1c1c20"
GREEN = "#2e8a58"
CAPE = "#1d4a35"
STRING = "#d8d0c0"
ARROW_WOOD = "#6b4a2a"
ARROW_HEAD = "#7d8a86"

# 方台 0–4，脚底 4，髋 15，肩 24.6，颈 26，头 26–32，盔顶 32.8，鬃冠到 34.2
SHOULDER_Y = 24.6
ELBOW_Y = 19.6
GRIP = [5.4, 19.6, 6.0]  # 左拳中心 = 弓把中心
# 反曲弓的半边轮廓（弓把中心为原点的 (dy, dz)）：弓把顶 → 弓臂向射手一侧弯 → 弓梢向前反翘
BOW_PROFILE = [(2.3, 0.0), (5.6, -1.2), (7.4, -1.9), (8.4, -1.3)]
BOW_HALF = BOW_PROFILE[-1][0]  # 弓梢到弓把的竖直距离
STRING_Z = GRIP[2] + BOW_PROFILE[-1][1]  # 弦挂在弓梢上，在弓把后方（朝身体）
DRAW = 5.5  # 拉弓时弦的中点后拉的距离（拉到下巴前）

BONES = [
    ("root", None, [0, 0, 0]),
    ("plinth", "root", [0, 0, 0]),
    ("body", "root", [0, 4, 0]),
    ("pelvis", "body", [0, 15, 0]),
    ("torso", "pelvis", [0, 15.8, 0]),
    ("head", "torso", [0, 26, 0]),
    ("upper_arm_l", "torso", [5.4, SHOULDER_Y, 0]),
    ("forearm_l", "upper_arm_l", [5.4, ELBOW_Y, 0]),
    ("hand_l", "forearm_l", [5.4, ELBOW_Y, 4.8]),
    ("bow", "hand_l", GRIP),
    ("string_top", "bow", [GRIP[0], GRIP[1] + BOW_HALF, STRING_Z]),
    ("string_bottom", "bow", [GRIP[0], GRIP[1] - BOW_HALF, STRING_Z]),
    ("arrow", "bow", [GRIP[0] - 0.55, GRIP[1] + 0.3, STRING_Z]),
    ("upper_arm_r", "torso", [-5.4, SHOULDER_Y, 0]),
    ("forearm_r", "upper_arm_r", [-5.4, ELBOW_Y, 0]),
    ("hand_r", "forearm_r", [-5.4, 15.0, 0]),
    ("cape", "torso", [0, 25.0, -2.6]),
    ("thigh_l", "pelvis", [2.0, 15, 0]),
    ("shin_l", "thigh_l", [2.0, 9.8, 0]),
    ("thigh_r", "pelvis", [-2.0, 15, 0]),
    ("shin_r", "thigh_r", [-2.0, 9.8, 0]),
]

ATTACHMENTS = [
    ("bow_grip", "bow", GRIP),
    ("nock", "bow", [GRIP[0], GRIP[1], STRING_Z]),
    ("nock_drawn", "bow", [GRIP[0], GRIP[1], STRING_Z - DRAW]),
    ("draw_hand", "hand_r", [-5.4, 12.9, 1.0]),  # 指尖勾弦处
    ("muzzle", "bow", [GRIP[0], GRIP[1] + 0.6, GRIP[2] + 0.8]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None, kind="cuboid", normal=None):
    p = {"id": pid, "bone": bone, "center_model_units": list(center), "size_model_units": list(size),
         "rotation_degrees_model_axes": list(rot), "color": color, "uv_group": group or pid,
         "uv_share": None, "surface_kind": kind}
    if normal:
        p["plane_normal_model_axes"] = list(normal)
    return p


def mirrored(pid_l, bone_l, center, size, color, rot=(0, 0, 0), group=None, kind="cuboid", normal=None):
    """左侧部件 + 镜像的右侧部件，两侧共用 UV 组。"""
    g = group or pid_l.removesuffix("_l")
    left = part(pid_l, bone_l, center, size, color, rot, g, kind, normal)
    rbone = bone_l[:-2] + "_r" if bone_l.endswith("_l") else bone_l
    right = part(pid_l[:-2] + "_r", rbone, [-center[0], center[1], center[2]], size, color,
                 (rot[0], -rot[1], -rot[2]), g, kind, normal)
    left["uv_share"] = right["uv_share"] = g
    return [left, right]


def rounded(pid, bone, center, size, color, bevel=0.5, group=None):
    """倒角体块：一窄一宽两个方块交叉（检查清单 §二.2）。"""
    w, h, d = size
    g = group or pid
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color, group=g),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color, group=g + "_side")]


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 方台：底座、台身（四面刻纹带在贴图里）、台面压边；前面一枚冥府双叉戟浮雕 ──
add(part("plinth_base", "plinth", [0, 0.75, 0], [13, 1.5, 13], STONE_DARK))
ext(rounded("plinth_body", "plinth", [0, 2.5, 0], [11.4, 2.0, 11.4], STONE, 0.6))
add(part("plinth_cap", "plinth", [0, 3.75, 0], [12.2, 0.5, 12.2], STONE_HI))
add(part("bident_shaft", "plinth", [0, 2.5, 5.75], [0.6, 1.6, 0.3], GREEN))
ext(mirrored("bident_prong_l", "plinth", [0.6, 3.05, 5.75], [0.4, 0.6, 0.3], GREEN, group="bident_prong"))

# ── 腿：青铜胫甲（前片 + 膝盖）、凉鞋式脚，双脚略分开站稳 ──
ext(mirrored("foot_l", "shin_l", [2.0, 4.7, 0.7], [3.0, 1.4, 4.6], STONE))
ext(mirrored("shin_l", "shin_l", [2.0, 7.6, 0], [2.8, 4.4, 2.8], STONE))
ext(mirrored("greave_l", "shin_l", [2.0, 7.4, 1.35], [3.2, 4.2, 0.6], BRONZE))
ext(mirrored("knee_l", "shin_l", [2.0, 9.7, 1.4], [2.6, 1.2, 0.7], BRONZE_HI))
ext(mirrored("thigh_l", "thigh_l", [2.0, 12.4, 0], [3.2, 5.2, 3.2], STONE))

# ── 腰：青铜腰带与皮条裙（pteruges，前后各 4 条、两侧各 2 条） ──
add(part("belt", "pelvis", [0, 15.2, 0], [8.8, 1.2, 4.9], BRONZE_DARK))
for i, x in enumerate((-2.85, -0.95, 0.95, 2.85)):
    add(part(f"strip_front_{i}", "pelvis", [x, 12.9, 2.45], [1.7, 3.6, 0.5], BRONZE, group="strip"))
    add(part(f"strip_back_{i}", "pelvis", [x, 12.9, -2.45], [1.7, 3.6, 0.5], BRONZE, group="strip"))
for z in (-1.1, 1.1):
    ext(mirrored(f"strip_side{'p' if z > 0 else 'n'}_l", "pelvis", [4.3, 12.9, z], [0.5, 3.6, 1.8], BRONZE,
                 group="strip_side"))

# ── 躯干：黑石胸身（倒角）、青铜胸甲（倒角，胸前刻纹在贴图）、肩甲、颈 ──
ext(rounded("chest", "torso", [0, 20.8, 0], [8.2, 10.0, 4.4], STONE, 0.5))
ext(rounded("cuirass", "torso", [0, 21.4, 0], [8.8, 6.6, 5.2], BRONZE, 0.5))
add(part("cuirass_rim", "torso", [0, 24.85, 0], [8.0, 0.5, 4.8], BRONZE_HI))
# 肌肉胸甲：两块胸肌、腹部中线与三道腹甲横棱、下缘外翻
ext(mirrored("pec_l", "torso", [2.05, 23.0, 2.75], [3.4, 2.4, 0.4], BRONZE_HI, group="pec"))
add(part("abs_line", "torso", [0, 20.6, 2.72], [0.5, 3.6, 0.3], BRONZE_DARK))
for i, y in enumerate((21.6, 20.3, 19.0)):
    add(part(f"abs_{i}", "torso", [0, y, 2.7], [5.0, 0.35, 0.25], BRONZE_DARK, group="abs"))
add(part("cuirass_flare", "torso", [0, 18.15, 0], [9.4, 0.7, 5.8], BRONZE_DARK))
ext(mirrored("pauldron_l", "torso", [4.9, 24.9, 0], [3.6, 1.4, 4.6], BRONZE))
add(part("neck", "torso", [0, 26.3, 0], [2.6, 1.6, 2.6], STONE))
# 冥府暗绿短斗篷：搭在双肩，背后垂到膝上；两道竖褶
add(part("cape", "cape", [0, 17.4, -2.85], [8.4, 15.0, 0.4], CAPE))
ext(mirrored("cape_fold_l", "cape", [2.0, 16.4, -3.1], [0.8, 12.5, 0.2], CAPE, group="cape_fold"))
ext(mirrored("cape_clasp_l", "torso", [3.9, 24.4, 2.1], [1.1, 1.1, 0.5], BRONZE_HI, group="cape_clasp"))

# ── 头与头盔：方头，青铜盔顶 + 护颊 + 护颈 + 鼻甲，黑鬃冠从额前到脑后 ──
add(part("head", "head", [0, 29.0, 0], [6.0, 6.0, 6.0], STONE))
add(part("helm_cap", "head", [0, 31.4, 0], [6.8, 2.4, 6.8], BRONZE))
ext(mirrored("cheek_guard_l", "head", [3.25, 28.4, 0.9], [0.5, 3.6, 4.2], BRONZE, group="cheek_guard"))
add(part("nape_guard", "head", [0, 28.9, -3.25], [6.6, 3.4, 0.5], BRONZE))
add(part("nose_guard", "head", [0, 29.2, 3.2], [0.8, 2.6, 0.5], BRONZE_HI))
add(part("brow_band", "head", [0, 30.35, 0.2], [7.0, 0.5, 6.9], BRONZE_HI))
# 冠座（青铜）上的黑鬃冠：前低、中高、后部垂到后颈
add(part("crest_base", "head", [0, 32.85, 0], [1.4, 0.5, 6.4], BRONZE_DARK))
add(part("crest_front", "head", [0, 33.6, 2.3], [1.3, 1.6, 2.2], CREST))
add(part("crest_mid", "head", [0, 34.0, -0.3], [1.3, 2.4, 3.0], CREST))
add(part("crest_back", "head", [0, 33.4, -2.9], [1.3, 1.6, 2.2], CREST))
add(part("crest_tail", "head", [0, 30.6, -3.95], [1.1, 4.0, 0.8], CREST))

# ── 手臂：肩下的上臂、青铜护臂、前臂；左前臂向前平端，右臂下垂 ──
ext(mirrored("upper_arm_l", "upper_arm_l", [5.4, 22.0, 0], [2.8, 5.2, 2.8], STONE))
add(part("forearm_l", "forearm_l", [5.4, ELBOW_Y, 2.4], [2.6, 2.6, 4.8], STONE, group="forearm_level"))
add(part("vambrace_l", "forearm_l", [5.4, ELBOW_Y, 2.9], [3.0, 3.0, 2.6], BRONZE, group="vambrace_level"))
add(part("forearm_r", "forearm_r", [-5.4, 17.3, 0], [2.6, 4.6, 2.6], STONE, group="forearm"))
add(part("vambrace_r", "forearm_r", [-5.4, 17.8, 0], [3.0, 2.6, 3.0], BRONZE, group="vambrace"))
# 左拳：握弓，拳心竖直（指节朝外），弓把穿过拳心；右手半握（拉弦用）
add(part("fist_l", "hand_l", [GRIP[0], GRIP[1], GRIP[2]], [2.6, 2.8, 2.4], STONE, group="fist_l"))
add(part("knuckles_l", "hand_l", [GRIP[0] + 1.35, GRIP[1], GRIP[2]], [0.3, 2.6, 2.0], STONE_HI, group="knuckles"))
add(part("hand_r", "hand_r", [-5.4, 13.7, 0], [2.6, 2.6, 2.6], STONE, group="hand_r"))
add(part("fingers_r", "hand_r", [-5.4, 12.8, 0.9], [2.4, 0.9, 1.0], STONE_HI, group="fingers_r"))

# ── 青铜反曲弓：弓把（穿过左拳）、上下各三段（弓臂两段 + 反曲弓梢），按 BOW_PROFILE 首尾相接 ──
G = GRIP
BOW = [part("bow_riser", "bow", [G[0], G[1], G[2]], [0.8, 2 * BOW_PROFILE[0][0], 1.0], BRONZE_DARK)]
THICK = [(0.75, 0.8, BRONZE, "limb1"), (0.6, 0.7, BRONZE, "limb2"), (0.5, 0.6, BRONZE_HI, "tip")]


def segment(a, b, s):
    """弓梢轮廓上相邻两点之间的一段：中心、长度（两端各多 0.15 让相邻段咬合）、绕 X 的倾角（上段与下段镜像）。"""
    dy, dz = (b[0] - a[0]) * s, b[1] - a[1]
    if dy < 0:
        dy, dz = -dy, -dz
    length = math.hypot(dy, dz) + 0.3
    rx = math.degrees(math.atan2(dz, dy))
    center = [G[0], G[1] + s * (a[0] + b[0]) / 2, G[2] + (a[1] + b[1]) / 2]
    return center, length, rx


for s, name in ((1, "upper"), (-1, "lower")):
    for k in range(3):
        c, length, rx = segment(BOW_PROFILE[k], BOW_PROFILE[k + 1], s)
        w, d, color, grp = THICK[k]
        BOW.append(part(f"{grp}_{name}", "bow", c, [w, length, d], color, (round(rx, 3), 0, 0), group=grp))
# 弦：上下两段，各自从弓梢指向弓把高度；拉弓时由动画绕弓梢转动并拉长
BOW.append(part("string_top", "string_top", [G[0], G[1] + BOW_HALF / 2, STRING_Z], [0.2, BOW_HALF, 0.2], STRING,
                group="string"))
BOW.append(part("string_bottom", "string_bottom", [G[0], G[1] - BOW_HALF / 2, STRING_Z], [0.2, BOW_HALF, 0.2], STRING,
                group="string"))
# 箭：架在弓把左侧、箭尾扣在弦上；平时缩成 0，拉弓时显示并随弦后移
AX, AY = G[0] - 0.55, G[1] + 0.3
ARROW = [
    part("arrow_shaft", "arrow", [AX, AY, STRING_Z + 4.6], [0.35, 0.35, 9.2], ARROW_WOOD),
    part("arrow_head", "arrow", [AX, AY, STRING_Z + 9.6], [0.7, 0.7, 1.2], ARROW_HEAD),
    part("arrow_fletch", "arrow", [AX, AY, STRING_Z + 1.1], [0.1, 1.2, 2.0], GREEN),
]


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "GP-GR-03",
        "name": "哨兵石像",
        "model": "sentinel",
        "height_limit_u": 34.5,
        "atlas": 256,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [0.8, 2.0, 0.8],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": BOW + ARROW,
        "string": {"half": BOW_HALF, "z": STRING_Z, "grip": GRIP, "draw": DRAW},
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["白模 v1（2026-10-18）：黑石身体、青铜盔与胸甲、黑鬃冠、皮条裙、胫甲；方台带双叉戟浮雕；"
                      "左拳握竖直反曲弓，弦分两段可拉。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"部件 {len(PARTS)} + 弓与箭 {len(BOW) + len(ARROW)} = {len(PARTS) + len(BOW) + len(ARROW)} 个方块")


if __name__ == "__main__":
    main()
