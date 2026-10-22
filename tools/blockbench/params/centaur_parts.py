"""半人马（SU-GR-03 召唤 / W1 野外，RIG-CENTAUR，T14 阶段 ③）部件表。

生成 art/blockbench/greek/centaur/centaur.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。

造型（docs/重构/设计/逐项设计/希腊首批/06_半人马_模型与行为卡.md）：古典式——人的腰接在马的前胸上方，四条马腿；
年长、浓须长发、怒容（帕特农神庙柱间壁）；马身栗色、马鬃马尾与小腿黑色；召唤版披豹皮（骨骼 pelt，野外版隐藏）。
武器：左手的木角复合反曲弓（普攻射箭；左前臂平端向前、弓竖直穿过左拳，弦分上下两段、拉弓时绕弓梢转动并拉长，
同哨兵石像），背上皮箭囊；特殊攻击「掷松」时手里的弓隐藏、背上的弓（bow_back）出现，双手举起松树干（pine）。
箭（arrow）、松树干、背上的弓平时缩成 0，由动画显示。
运行：python3 tools/blockbench/params/centaur_parts.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/centaur/centaur.params.json"

COAT = "#8a4f2a"
COAT_DARK = "#6e3d20"
POINTS = "#2c211b"  # 栗色马的黑色小腿、鬃尾
HOOF = "#3a3634"
SKIN = "#c08a63"
SKIN_DARK = "#a8734f"
HAIR = "#3b2a1e"
PELT = "#c99a4a"
PELT_PALE = "#e2c48a"
WOOD = "#4a3020"
HORN = "#d2bf96"
WRAP = "#7a5232"
STRING = "#e8e0cc"
LEATHER = "#5a3a22"
FLETCH = "#e9e4d8"
ARROW_WOOD = "#8a6a44"
ARROW_HEAD = "#8f8a7e"
BARK = "#5a3b24"
ROOTBALL = "#6b5137"
NEEDLE = "#2f5a2e"

WAIST = (0.0, 21.0, 8.5)
SHOULDER_Y = 30.0
TORSO_Z = 8.4
ARM_X = 6.2
ELBOW_Y = 24.2
WRIST_Y = 18.8
HEAD_C = (0.0, 35.6, 8.9)
WRIST_Z = TORSO_Z + 5.0           # 左前臂平端向前，腕在胸前 5u
GRIP = [ARM_X, ELBOW_Y, WRIST_Z + 1.2]  # 左拳中心 = 弓把中心
# 反曲弓的半边轮廓（弓把中心为原点的 (dy, dz)）：弓把顶 → 弓臂向射手一侧弯 → 弓梢向前反翘（同哨兵石像）
BOW_PROFILE = [(2.3, 0.0), (5.6, -1.2), (7.4, -1.9), (8.4, -1.3)]
BOW_HALF = BOW_PROFILE[-1][0]
STRING_Z = GRIP[2] + BOW_PROFILE[-1][1]
DRAW = 5.5
BACK_GRIP = [2.6, 27.0, TORSO_Z - 4.4]  # 背上的弓（掷松时显示）

BONES = [
    ("root", None, [0, 0, 0]),
    ("body", "root", [0, 16, 0]),
    ("waist", "body", list(WAIST)),
    ("chest", "waist", [0, 25.5, TORSO_Z]),
    ("neck", "chest", [0, 31.6, TORSO_Z + 0.2]),
    ("head", "neck", [0, 32.4, TORSO_Z + 0.3]),
    ("pelt", "chest", [0, SHOULDER_Y, TORSO_Z]),
    ("arm_l", "chest", [ARM_X, SHOULDER_Y, TORSO_Z]),
    ("forearm_l", "arm_l", [ARM_X, ELBOW_Y, TORSO_Z]),
    ("hand_l", "forearm_l", [ARM_X, ELBOW_Y, WRIST_Z]),
    ("bow", "hand_l", GRIP),
    ("string_top", "bow", [GRIP[0], GRIP[1] + BOW_HALF, STRING_Z]),
    ("string_bottom", "bow", [GRIP[0], GRIP[1] - BOW_HALF, STRING_Z]),
    ("arrow", "bow", [GRIP[0] - 0.55, GRIP[1] + 0.3, STRING_Z]),
    ("arm_r", "chest", [-ARM_X, SHOULDER_Y, TORSO_Z]),
    ("forearm_r", "arm_r", [-ARM_X, ELBOW_Y, TORSO_Z]),
    ("hand_r", "forearm_r", [-ARM_X, WRIST_Y, TORSO_Z]),
    ("quiver", "chest", [-2.2, 30.0, TORSO_Z - 4.2]),
    ("bow_back", "chest", BACK_GRIP),
    ("pine", "chest", [0, 44.0, TORSO_Z]),
    ("tail", "body", [0, 18.5, -9.2]),
    ("tail_tip", "tail", [0, 12, -12.0]),
    ("leg_fl", "body", [2.8, 14, 6.5]),
    ("leg_fl_lower", "leg_fl", [2.8, 8, 6.5]),
    ("leg_fr", "body", [-2.8, 14, 6.5]),
    ("leg_fr_lower", "leg_fr", [-2.8, 8, 6.5]),
    ("leg_hl", "body", [2.8, 15, -7]),
    ("leg_hl_lower", "leg_hl", [2.8, 8, -8.5]),
    ("leg_hr", "body", [-2.8, 15, -7]),
    ("leg_hr_lower", "leg_hr", [-2.8, 8, -8.5]),
]

ATTACHMENTS = [
    ("bow_grip", "bow", GRIP),
    ("nock", "bow", [GRIP[0], GRIP[1], STRING_Z]),
    ("nock_drawn", "bow", [GRIP[0], GRIP[1], STRING_Z - DRAW]),
    ("draw_hand", "hand_r", [-ARM_X, 16.4, TORSO_Z + 1.0]),
    ("muzzle", "bow", [GRIP[0], GRIP[1] + 0.6, GRIP[2] + 0.8]),
    ("pine_release", "pine", [0, 44.0, TORSO_Z]),
    ("hoof_fl", "leg_fl_lower", [2.8, 0, 6.8]),
    ("hoof_fr", "leg_fr_lower", [-2.8, 0, 6.8]),
    ("hoof_hl", "leg_hl_lower", [2.8, 0, -7.9]),
    ("hoof_hr", "leg_hr_lower", [-2.8, 0, -7.9]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None):
    return {"id": pid, "bone": bone, "center_model_units": [round(c, 4) for c in center],
            "size_model_units": list(size), "rotation_degrees_model_axes": [round(r, 3) for r in rot], "color": color,
            "uv_group": group or pid, "uv_share": None, "surface_kind": "cuboid"}


def mirror_bone(bone):
    for a, b in (("_fl", "_fr"), ("_hl", "_hr")):
        if a in bone:
            return bone.replace(a, b)
    if bone.endswith("_l"):
        return bone[:-2] + "_r"
    return bone


def mirrored(pid_l, bone_l, center, size, color, rot=(0, 0, 0), group=None):
    """左侧部件 + 镜像的右侧部件（x 取反、ry 与 rz 取反），两侧共用 UV 组。"""
    g = group or pid_l.removesuffix("_l")
    left = part(pid_l, bone_l, center, size, color, rot, g)
    pid_r = pid_l[:-2] + "_r" if pid_l.endswith("_l") else pid_l + "_r"
    right = part(pid_r, mirror_bone(bone_l), [-center[0], center[1], center[2]], size, color,
                 (rot[0], -rot[1], -rot[2]), g)
    left["uv_share"] = right["uv_share"] = g
    return [left, right]


def rounded(pid, bone, center, size, color, bevel=0.6, rot=(0, 0, 0)):
    """倒角体块：一窄一宽两个方块交叉（检查清单 §二.2）。"""
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color, rot),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color, rot)]


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 马身：倒角桶身、前胸、臀部、腹线；肩胛斜面与后腿肌群 ──
ext(rounded("barrel", "body", [0, 16, 0], [9, 8.5, 18], COAT, 1.0))
add(part("breast", "body", [0, 15.4, 8.8], [7.4, 7.2, 2.8], COAT))
ext(rounded("croup", "body", [0, 18.1, -6.6], [8.4, 4.4, 8.0], COAT, 0.8))
add(part("belly", "body", [0, 11.5, -0.5], [6.6, 1.2, 12.0], COAT_DARK))
ext(mirrored("shoulder_l", "body", [3.8, 16.4, 5.6], [1.6, 6.0, 4.2], COAT, (-18, 0, 0)))
ext(mirrored("haunch_l", "leg_hl", [3.4, 15.0, -7.4], [2.2, 6.4, 5.8], COAT))
# 人腰与马身接合：一圈毛边（马毛色）盖住接缝
add(part("fur_ring", "waist", [0, 21.2, 8.2], [8.6, 1.4, 6.2], COAT_DARK))

# ── 马腿：前臂 / 胫为栗色，腕以下黑色（栗色马的「黑四肢」），角质蹄 ──
ext(mirrored("forearm_leg_l", "leg_fl", [2.8, 11.0, 6.5], [2.8, 6.5, 3.0], COAT, group="forearm_leg"))
ext(mirrored("knee_f_l", "leg_fl_lower", [2.8, 8.0, 6.6], [2.7, 1.4, 2.8], POINTS, group="knee"))
ext(mirrored("cannon_f_l", "leg_fl_lower", [2.8, 4.75, 6.5], [1.9, 5.5, 2.0], POINTS, group="cannon"))
ext(mirrored("fetlock_f_l", "leg_fl_lower", [2.8, 2.0, 6.6], [2.3, 1.2, 2.2], POINTS, group="fetlock"))
ext(mirrored("hoof_f_l", "leg_fl_lower", [2.8, 0.75, 6.8], [2.5, 1.5, 2.8], HOOF, group="hoof"))
ext(mirrored("gaskin_l", "leg_hl", [2.8, 10.6, -8.0], [2.5, 5.2, 2.8], COAT, (10, 0, 0), group="gaskin"))
ext(mirrored("hock_l", "leg_hl_lower", [2.8, 8.0, -8.8], [2.2, 1.6, 2.6], POINTS, group="hock"))
ext(mirrored("cannon_h_l", "leg_hl_lower", [2.8, 4.75, -8.3], [1.9, 5.5, 2.0], POINTS, group="cannon"))
ext(mirrored("fetlock_h_l", "leg_hl_lower", [2.8, 2.0, -8.1], [2.3, 1.2, 2.2], POINTS, group="fetlock"))
ext(mirrored("hoof_h_l", "leg_hl_lower", [2.8, 0.75, -7.9], [2.5, 1.5, 2.8], HOOF, group="hoof"))

# ── 马尾：黑色，尾根 + 两段 ──
add(part("tail_dock", "tail", [0, 18.0, -10.0], [1.8, 2.2, 2.0], POINTS, (-30, 0, 0)))
add(part("tail_hair", "tail", [0, 15.0, -11.2], [2.6, 6.4, 1.8], POINTS, (15, 0, 0)))
add(part("tail_hair2", "tail_tip", [0, 8.6, -12.4], [3.0, 7.0, 2.0], POINTS, (8, 0, 0)))
add(part("tail_end", "tail_tip", [0, 4.6, -12.8], [2.2, 2.0, 1.6], POINTS, (4, 0, 0)))

# ── 人上身：腹（腹肌画在贴图）、倒角胸廓、胸肌、斜方肌、三角肌、颈 ──
add(part("abdomen", "waist", [0, 23.4, TORSO_Z], [7.6, 4.6, 5.0], SKIN))
ext(rounded("ribcage", "chest", [0, 28.0, TORSO_Z], [10.0, 5.6, 5.8], SKIN, 0.8))
ext(mirrored("pec_l", "chest", [2.4, 28.9, TORSO_Z + 2.95], [4.0, 2.6, 0.5], SKIN, group="pec"))
add(part("trapezius", "chest", [0, 30.9, TORSO_Z - 0.6], [6.4, 1.2, 3.6], SKIN))
add(part("neck", "neck", [0, 32.2, TORSO_Z + 0.2], [3.2, 2.0, 3.2], SKIN))

# ── 头：方头、眉骨、鼻、长发（头顶 + 后垂到肩）、浓须（前、下巴、两颊）、上唇须、人耳 ──
HX, HY, HZ = HEAD_C
add(part("head", "head", [HX, HY, HZ], [7.0, 7.0, 7.0], SKIN))
add(part("brow", "head", [HX, HY + 1.7, HZ + 3.65], [6.0, 1.0, 0.6], SKIN_DARK))
add(part("nose", "head", [HX, HY - 0.6, HZ + 3.9], [1.6, 2.4, 1.2], SKIN))
add(part("hair_cap", "head", [HX, HY + 3.9, HZ - 0.3], [7.6, 1.2, 7.6], HAIR))
add(part("hair_back", "head", [HX, HY - 0.6, HZ - 3.85], [7.6, 8.4, 0.8], HAIR))
ext(mirrored("hair_side_l", "head", [3.75, HY + 1.2, HZ - 1.2], [0.6, 4.0, 5.0], HAIR, group="hair_side"))
add(part("fringe", "head", [HX, HY + 3.0, HZ + 3.55], [7.0, 0.8, 0.6], HAIR))
add(part("beard", "head", [HX, HY - 3.6, HZ + 3.4], [6.2, 3.6, 1.4], HAIR))
add(part("beard_chin", "head", [HX, HY - 5.6, HZ + 2.9], [4.2, 2.0, 2.2], HAIR))
ext(mirrored("beard_side_l", "head", [3.65, HY - 2.0, HZ + 1.4], [0.6, 3.6, 3.8], HAIR, group="beard_side"))
add(part("moustache", "head", [HX, HY - 1.9, HZ + 3.95], [4.4, 0.7, 0.6], HAIR))
ext(mirrored("ear_l", "head", [3.75, HY + 0.1, HZ - 0.2], [0.6, 1.8, 1.4], SKIN, group="ear"))

# ── 手臂：三角肌、上臂、前臂（青筋在贴图）、手（握拳） ──
ext(mirrored("deltoid_l", "arm_l", [ARM_X, SHOULDER_Y - 0.4, TORSO_Z], [3.6, 2.4, 3.8], SKIN, group="deltoid"))
ext(mirrored("upper_arm_l", "arm_l", [ARM_X, 27.0, TORSO_Z], [3.2, 5.6, 3.2], SKIN, group="upper_arm"))
# 左前臂平端向前（持弓），右前臂下垂（拉弦的手）
add(part("forearm_l", "forearm_l", [ARM_X, ELBOW_Y, TORSO_Z + 2.5], [2.9, 2.9, 5.4], SKIN, group="forearm_level"))
add(part("forearm_r", "forearm_r", [-ARM_X, 21.5, TORSO_Z], [2.9, 5.4, 2.9], SKIN, group="forearm"))
add(part("bracer_l", "forearm_l", [ARM_X, ELBOW_Y, TORSO_Z + 3.6], [3.2, 3.2, 2.2], LEATHER, group="bracer"))
# 左拳：握弓，拳心竖直（指节朝外），弓把穿过拳心；右手半握（勾弦）
add(part("fist_l", "hand_l", GRIP, [2.6, 2.8, 2.4], SKIN, group="fist_l"))
add(part("knuckles_l", "hand_l", [GRIP[0] + 1.35, GRIP[1], GRIP[2]], [0.3, 2.6, 2.0], SKIN_DARK, group="knuckles"))
add(part("hand_r", "hand_r", [-ARM_X, 17.5, TORSO_Z], [2.8, 2.6, 2.8], SKIN, group="hand_r"))
add(part("fingers_r", "hand_r", [-ARM_X, 16.6, TORSO_Z + 0.9], [2.6, 0.9, 1.0], SKIN_DARK, group="fingers_r"))

# ── 豹皮披肩（召唤版）：搭在双肩，背后一片垂到腰下，胸前两只前爪打结 ──
PELT_PARTS = [
    part("pelt_back", "pelt", [0, 26.6, TORSO_Z - 3.25], [10.6, 8.0, 0.6], PELT),
    *mirrored("pelt_shoulder_l", "pelt", [4.2, SHOULDER_Y + 0.75, TORSO_Z - 0.2], [3.2, 0.6, 6.2], PELT, group="pelt_shoulder"),
    *mirrored("pelt_paw_l", "pelt", [1.6, 30.0, TORSO_Z + 3.25], [2.6, 1.2, 0.6], PELT, (0, 0, -25), group="pelt_paw"),
    part("pelt_knot", "pelt", [0, 29.6, TORSO_Z + 3.4], [1.6, 1.4, 0.8], PELT_PALE),
]

# ── 弓（左手）：弓把（缠皮）穿过左拳，上下各三段（弓臂两段 + 角质反曲弓梢），按 BOW_PROFILE 首尾相接；弦分上下两段；箭 ──
THICK = [(0.75, 0.8, WOOD, "limb1"), (0.6, 0.7, WOOD, "limb2"), (0.5, 0.6, HORN, "tip")]


def segment(grip, a, b, s):
    """弓梢轮廓上相邻两点之间的一段：中心、长度（两端各多 0.15 让相邻段咬合）、绕 X 的倾角（上段与下段镜像）。"""
    dy, dz = (b[0] - a[0]) * s, b[1] - a[1]
    if dy < 0:
        dy, dz = -dy, -dz
    length = math.hypot(dy, dz) + 0.3
    rx = math.degrees(math.atan2(dz, dy))
    center = [grip[0], grip[1] + s * (a[0] + b[0]) / 2, grip[2] + (a[1] + b[1]) / 2]
    return center, length, rx


def bow_parts(prefix, bone, grip, string_bones):
    """一张弓的方块。string_bones 为 (上弦骨骼, 下弦骨骼)；背上的弓弦不动，用同一骨骼。"""
    out = [part(prefix + "riser", bone, grip, [0.8, 2 * BOW_PROFILE[0][0], 1.0], WRAP, group="bow_riser")]
    for sgn, name in ((1, "upper"), (-1, "lower")):
        for k in range(3):
            c, length, rx = segment(grip, BOW_PROFILE[k], BOW_PROFILE[k + 1], sgn)
            w, d, color, grp = THICK[k]
            out.append(part(f"{prefix}{grp}_{name}", bone, c, [w, length, d], color, (round(rx, 3), 0, 0), group="bow_" + grp))
    sz = grip[2] + BOW_PROFILE[-1][1]
    out.append(part(prefix + "string_top", string_bones[0], [grip[0], grip[1] + BOW_HALF / 2, sz], [0.2, BOW_HALF, 0.2],
                    STRING, group="bow_string"))
    out.append(part(prefix + "string_bottom", string_bones[1], [grip[0], grip[1] - BOW_HALF / 2, sz], [0.2, BOW_HALF, 0.2],
                    STRING, group="bow_string"))
    return out


BOW_PARTS = bow_parts("", "bow", GRIP, ("string_top", "string_bottom"))
AX, AY = GRIP[0] - 0.55, GRIP[1] + 0.3
BOW_PARTS += [
    part("arrow_shaft", "arrow", [AX, AY, STRING_Z + 4.6], [0.35, 0.35, 9.2], ARROW_WOOD),
    part("arrow_head", "arrow", [AX, AY, STRING_Z + 9.6], [0.7, 0.7, 1.2], ARROW_HEAD),
    part("arrow_fletch", "arrow", [AX, AY, STRING_Z + 1.1], [0.1, 1.2, 2.0], FLETCH),
]
BACK_BOW_PARTS = bow_parts("back_", "bow_back", BACK_GRIP, ("bow_back", "bow_back"))

# ── 箭囊：斜挎在背上（口在右肩后），口沿一道箍，露出四支箭羽 ──
QUIVER_PARTS = [
    part("quiver", "quiver", [-2.2, 26.0, TORSO_Z - 4.5], [2.6, 9.0, 2.6], LEATHER, (0, 0, 18)),
    part("quiver_rim", "quiver", [-3.6, 30.3, TORSO_Z - 4.5], [3.0, 0.6, 3.0], WRAP, (0, 0, 18)),
    part("quiver_strap", "chest", [0, 27.5, TORSO_Z + 3.0], [0.8, 8.0, 0.4], LEATHER, (0, 0, -40)),
]
for i, (dx, dz) in enumerate(((-0.5, -0.5), (0.5, -0.4), (-0.4, 0.5), (0.6, 0.6))):
    QUIVER_PARTS.append(part(f"fletch_{i}", "quiver", [-4.0 + dx, 31.6, TORSO_Z - 4.5 + dz], [0.3, 1.6, 0.9], FLETCH,
                             (0, 0, 18), group="fletch"))

PINE_PARTS = [
    part("trunk", "pine", [0, 44.0, TORSO_Z], [26.0, 2.8, 2.8], BARK),
    part("rootball", "pine", [14.2, 44.0, TORSO_Z], [3.6, 4.4, 4.4], ROOTBALL),
    part("root_a", "pine", [15.6, 46.2, TORSO_Z + 1.0], [2.4, 1.0, 1.0], ROOTBALL, (0, 0, 35)),
    part("root_b", "pine", [15.4, 41.8, TORSO_Z - 1.2], [2.4, 1.0, 1.0], ROOTBALL, (0, 0, -35)),
    part("branch_a", "pine", [-3.0, 46.6, TORSO_Z], [1.0, 3.6, 1.0], BARK, (0, 0, 30)),
    part("branch_b", "pine", [-8.0, 41.6, TORSO_Z + 0.5], [1.0, 3.2, 1.0], BARK, (0, 0, -35)),
    part("needles", "pine", [-14.6, 44.0, TORSO_Z], [4.4, 5.0, 5.0], NEEDLE),
    part("needles_b", "pine", [-8.8, 39.4, TORSO_Z + 0.6], [2.6, 2.6, 2.6], NEEDLE),
]


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "SU-GR-03",
        "name": "半人马",
        "model": "centaur",
        "height_limit_u": 41.6,
        "atlas": 256,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [1.1, 2.5, 1.1],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": PELT_PARTS + BOW_PARTS + BACK_BOW_PARTS + QUIVER_PARTS + PINE_PARTS,
        "string": {"half": BOW_HALF, "z": STRING_Z, "grip": GRIP, "draw": DRAW},
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["白模 v1（2026-10-21）：古典式四马腿，栗色马身、黑四肢与鬃尾；人上身年长浓须长发；召唤版豹皮披肩；"
                      "右手石块、双手松树干平时缩成 0。",
                      "白模 v2（2026-10-21，用户：还是带弓箭比较好）：去掉石块；左前臂平端持木角复合反曲弓，弦分两段、箭平时缩成 0；"
                      "背上皮箭囊；掷松时用的背上备用弓。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    extra = PELT_PARTS + BOW_PARTS + BACK_BOW_PARTS + QUIVER_PARTS + PINE_PARTS
    print(f"身体 {len(PARTS)} + 豹皮 {len(PELT_PARTS)} + 弓与箭 {len(BOW_PARTS)} + 背弓 {len(BACK_BOW_PARTS)} + 箭囊 {len(QUIVER_PARTS)}"
          f" + 松树干 {len(PINE_PARTS)} = {len(PARTS) + len(extra)} 个方块")


if __name__ == "__main__":
    main()
