"""独眼巨人（SU-GR-04 召唤 / EL-GR-02 牧人精英，RIG-CYCLOPS，T25a 阶段 ③）部件表。

生成 art/blockbench/greek/cyclops/cyclops.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。
族基准包络 W/H/L = 1.8 / 3.8 / 1.3 格（28.8 / 60.8 / 20.8u），图集 256 起评（07 号 RIG-CYCLOPS）。

造型依据：
- 赫西俄德《神谱》142–145：「一只圆眼长在额头正中」（κυκλοτερής，Kyklops 之名由此）——眼在前额正中、构造上的圆眼
  （眼白横竖两块交叉成圆，虹膜单独一块；眉骨与颧骨凸出把眼窝压深），不是两眼位置涂一只；
- 《奥德赛》9.190–192：不像吃面包的人，像高山上林木茂密的峰顶——驼背、厚肩、长臂、粗腿，块头大；
- 《奥德赛》9.319–320：巨人的大棒是一根青橄榄木——右手握一根带树疤与断枝的橄榄树干；
- 古风瓶画里的波吕斐摩斯（埃莱夫西斯双耳瓶，约公元前 670 年；拉科尼亚杯，约公元前 565 年）：大胡子、长发——蓬发、浓须。
召唤版（阿瑞斯）只系皮裹裙与腰绳；牧人精英多一张搭在左肩、斜挂到右腰的羊皮（骨骼 fleece，召唤版隐藏），
投掷用的巨石（骨骼 boulder，挂在左手上）平时由渲染器隐藏，举石、投石时显示（阶段 ⑥：右手握棍，单手举石）。
识别方向（07 号）：中央独眼 / 粗棍 / 大手。握棍点（club_grip）与抓取点（grab_palm）分开；头部判定体单独给定位点（head_hit）。
握持：右前臂平端向前，拳心竖直（拇指在上），棍身竖直穿过拳心的握孔；棍骨骼相对手只绕棍身自转（检查清单 §三.1）。
运行：python3 tools/blockbench/params/cyclops_parts.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/cyclops/cyclops.params.json"

SKIN = "#b98663"
SKIN_DARK = "#9a6a4c"
HAIR = "#2f251d"
BEARD = "#3d2f22"
EYE = "#e9e2cf"
IRIS = "#9c6b2e"
MOUTH = "#3a1f1a"
TOOTH = "#e2d6b4"
LEATHER = "#5a3a22"
CLOTH = "#7c4a2e"
ROPE = "#9c8656"
WOOD = "#7a6a4e"
WOOD_DARK = "#4f4330"
FLEECE = "#e4d9c0"
FLEECE_DARK = "#b9ab8c"
STONE = "#8a857a"
NAIL = "#d6c5a6"

HIP_Y = 25.0
SHOULDER = (12.4, 44.2, 0.6)
ELBOW = (13.0, 33.6, 0.8)
HEAD_C = (0.0, 53.4, 5.0)
GRIP = [-13.2, 33.6, 11.6]  # 右拳中心 = 棍的握点（右前臂平端向前）
ROCK = [6.4, 15.6, 2.0]     # 投掷巨石的中心：贴在左手掌心内侧（T25a ⑥：右手握棍，只能左手单手举石）

BONES = [
    ("root", None, [0, 0, 0]),
    ("body", "root", [0, HIP_Y, 0]),
    ("waist", "body", [0, 29.0, 0.4]),
    ("chest", "waist", [0, 38.0, 0.4]),
    ("neck", "chest", [0, 46.6, 2.6]),
    ("head", "neck", [0, 48.6, 4.4]),
    ("arm_l", "chest", list(SHOULDER)),
    ("forearm_l", "arm_l", list(ELBOW)),
    ("hand_l", "forearm_l", [13.4, 23.6, 1.2]),
    ("arm_r", "chest", [-SHOULDER[0], SHOULDER[1], SHOULDER[2]]),
    ("forearm_r", "arm_r", [-ELBOW[0], ELBOW[1], ELBOW[2]]),
    ("hand_r", "forearm_r", [-13.2, 33.6, 9.0]),
    ("club", "hand_r", GRIP),
    ("fleece", "chest", [0, 44.0, 0.4]),
    ("boulder", "hand_l", ROCK),
    ("leg_l", "body", [5.6, HIP_Y, 0]),
    ("shin_l", "leg_l", [5.8, 13.0, 0.6]),
    ("leg_r", "body", [-5.6, HIP_Y, 0]),
    ("shin_r", "leg_r", [-5.8, 13.0, 0.6]),
]

ATTACHMENTS = [
    ("club_grip", "club", GRIP),
    ("club_head", "club", [GRIP[0], 51.2, GRIP[2]]),
    ("grab_palm", "hand_l", [11.9, 17.6, 2.0]),
    ("rock_hold", "boulder", ROCK),
    ("head_hit", "head", list(HEAD_C)),
    ("eye", "head", [0.0, 53.8, 10.4]),
    ("mouth", "head", [0.0, 49.0, 10.2]),
    ("foot_l", "shin_l", [5.9, 0, 1.6]),
    ("foot_r", "shin_r", [-5.9, 0, 1.6]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None):
    return {"id": pid, "bone": bone, "center_model_units": [round(c, 4) for c in center],
            "size_model_units": list(size), "rotation_degrees_model_axes": [round(r, 3) for r in rot], "color": color,
            "uv_group": group or pid, "uv_share": None, "surface_kind": "cuboid"}


def mirror_bone(bone):
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


def rounded_mirrored(pid_l, bone_l, center, size, color, bevel=0.6, group=None):
    g = group or pid_l.removesuffix("_l")
    w, h, d = size
    return (mirrored(pid_l, bone_l, center, [w - 2 * bevel, h, d], color, group=g)
            + mirrored(pid_l.removesuffix("_l") + "_side_l", bone_l, center, [w, h, d - 2 * bevel], color, group=g + "_side"))


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 腿：粗大腿、膝、小腿（后侧小腿肚）、皮绑腿、踝、宽脚掌与脚趾、脚跟 ──
ext(rounded_mirrored("thigh_l", "leg_l", [5.6, 19.2, 0.2], [7.6, 10.4, 7.6], SKIN, 1.2))
ext(mirrored("knee_l", "shin_l", [5.8, 13.4, 1.6], [6.2, 2.8, 5.8], SKIN))
ext(rounded_mirrored("calf_l", "shin_l", [5.8, 8.6, 0.2], [6.4, 8.4, 6.4], SKIN, 1.0))
ext(mirrored("calf_muscle_l", "shin_l", [5.8, 9.4, -3.0], [4.8, 5.2, 1.4], SKIN))
ext(mirrored("shin_wrap_l", "shin_l", [5.8, 5.4, 0.3], [6.8, 1.6, 6.8], LEATHER))
ext(mirrored("ankle_l", "shin_l", [5.8, 3.4, 0.3], [5.2, 2.4, 5.2], SKIN))
ext(mirrored("foot_l", "shin_l", [5.9, 1.1, 1.6], [6.0, 2.2, 9.0], SKIN))
ext(mirrored("toes_l", "shin_l", [5.9, 0.9, 6.8], [5.8, 1.8, 1.4], SKIN, group="toes"))
ext(mirrored("heel_l", "shin_l", [5.8, 1.0, -2.8], [4.6, 2.0, 1.6], SKIN))

# ── 骨盆与裹裙：倒角骨盆、腰绳、皮裹裙前后片、绳结 ──
ext(rounded("pelvis", "body", [0, 25.8, 0], [15.6, 6.4, 9.6], SKIN, 1.4))
add(part("belt", "body", [0, 29.2, 0.2], [16.4, 2.0, 11.0], ROPE))
add(part("loin_front", "body", [0, 23.4, 5.3], [7.6, 8.4, 0.8], CLOTH, (-5, 0, 0)))
add(part("loin_back", "body", [0, 23.8, -5.2], [10.0, 7.6, 0.8], CLOTH, (6, 0, 0)))
add(part("belt_knot", "body", [3.6, 29.0, 5.9], [2.2, 2.4, 1.0], ROPE))

# ── 腰腹：啤酒肚（倒角）与前凸 ──
ext(rounded("belly", "waist", [0, 33.4, 1.4], [15.0, 8.8, 11.2], SKIN, 1.6))
add(part("belly_front", "waist", [0, 32.6, 7.2], [10.0, 6.0, 1.2], SKIN))

# ── 胸背：倒角胸廓、胸肌、斜方肌、驼背 ──
ext(rounded("ribcage", "chest", [0, 41.4, 0.4], [19.0, 8.8, 11.6], SKIN, 1.6))
ext(mirrored("pec_l", "chest", [4.6, 43.2, 6.6], [7.4, 4.4, 1.4], SKIN, group="pec"))
add(part("traps", "chest", [0, 46.4, -0.8], [13.0, 2.4, 7.6], SKIN))
add(part("hump", "chest", [0, 44.0, -5.9], [15.2, 8.4, 2.2], SKIN))
add(part("hump_top", "chest", [0, 47.0, -3.8], [11.0, 1.6, 4.6], SKIN))
add(part("neck", "neck", [0, 47.6, 3.0], [9.4, 3.8, 8.4], SKIN))

# ── 头：方颅（倒角）、厚下颌；前额正中的圆眼：眼白横竖两块交叉 + 虹膜；粗眉骨压在眼上、颧骨托在眼下（眼窝）；
#    宽塌鼻、嘴（单独贴图区）与两颗下獠牙、招风耳；蓬发（顶、后、两侧、额前乱发）、浓须（前、下垂到胸、两颊） ──
HX, HY, HZ = HEAD_C
FZ = HZ + 4.8  # 脸面（头块前面）的 z
ext(rounded("head", "head", [HX, HY, HZ], [11.0, 10.4, 9.6], SKIN, 1.4))
add(part("jaw", "head", [HX, 49.2, HZ + 1.6], [10.0, 3.2, 6.4], SKIN))
add(part("brow", "head", [HX, 56.4, FZ + 0.5], [8.6, 1.8, 1.4], SKIN_DARK))
ext(mirrored("cheekbone_l", "head", [3.4, 51.9, FZ + 0.3], [3.4, 1.6, 1.0], SKIN, group="cheekbone"))
add(part("eye_h", "head", [HX, 53.8, FZ + 0.2], [5.0, 3.6, 0.5], EYE))
add(part("eye_v", "head", [HX, 53.8, FZ + 0.15], [3.6, 5.0, 0.4], EYE))  # 比横块薄一点，重叠处不闪
add(part("iris_h", "head", [HX, 53.8, FZ + 0.5], [3.0, 2.0, 0.2], IRIS))
add(part("iris_v", "head", [HX, 53.8, FZ + 0.48], [2.0, 3.0, 0.16], IRIS))
add(part("nose", "head", [HX, 51.2, FZ + 0.4], [3.0, 2.2, 1.0], SKIN))
add(part("nose_bridge", "head", [HX, 52.4, FZ + 0.2], [1.6, 0.8, 0.6], SKIN))
add(part("mouth", "head", [HX, 49.2, FZ + 0.1], [5.6, 1.2, 0.4], MOUTH))
ext(mirrored("lip_corner_l", "head", [3.1, 49.0, FZ + 0.1], [0.8, 0.8, 0.4], SKIN_DARK, group="lip_corner"))
ext(mirrored("tusk_l", "head", [1.8, 49.9, FZ + 0.25], [0.8, 1.0, 0.4], TOOTH, group="tusk"))
ext(mirrored("ear_l", "head", [5.8, 53.2, HZ], [1.0, 3.6, 2.6], SKIN, group="ear"))
# 蓬发：头顶高低不齐的几簇、后脑长发垂到肩、两鬓乱发、额前几绺
add(part("hair_cap", "head", [HX, 59.0, HZ - 0.4], [11.6, 1.4, 10.2], HAIR))
add(part("hair_tuft_a", "head", [2.6, 60.2, HZ - 1.4], [4.4, 1.4, 4.8], HAIR, (0, 0, 8)))
add(part("hair_tuft_b", "head", [-2.8, 60.0, HZ + 0.8], [4.0, 1.2, 4.4], HAIR, (0, 0, -10)))
add(part("hair_tuft_c", "head", [0.2, 60.0, HZ - 4.2], [5.0, 1.0, 3.0], HAIR))
add(part("hair_back", "head", [HX, 52.2, HZ - 5.2], [12.0, 13.4, 1.4], HAIR))
add(part("hair_back_low", "head", [HX, 45.6, HZ - 5.6], [9.0, 2.4, 1.2], HAIR))
ext(mirrored("hair_side_l", "head", [5.9, 55.8, HZ - 1.6], [1.2, 5.6, 6.0], HAIR, group="hair_side"))
ext(mirrored("hair_lock_l", "head", [6.1, 51.6, HZ - 2.6], [0.8, 3.2, 2.2], HAIR, group="hair_lock"))
add(part("forelock", "head", [-1.0, 58.2, FZ + 0.1], [5.4, 1.6, 1.0], HAIR, (0, 0, 6)))
add(part("forelock_b", "head", [3.4, 58.0, FZ + 0.1], [2.4, 1.6, 1.0], HAIR, (0, 0, -10)))
# 浓须：从嘴下开始（嘴露出来），下垂到胸，两颊连鬓
add(part("beard", "head", [HX, 46.4, HZ + 3.4], [10.0, 4.0, 3.4], BEARD))
add(part("beard_low", "head", [HX, 43.4, HZ + 3.0], [7.0, 3.0, 3.0], BEARD))
add(part("beard_tip", "head", [HX, 41.4, HZ + 2.8], [4.0, 1.6, 2.4], BEARD))
ext(mirrored("whisker_l", "head", [4.6, 49.6, HZ + 2.4], [1.2, 4.4, 4.0], BEARD, group="whisker"))
add(part("moustache", "head", [HX, 50.0, FZ + 0.25], [6.4, 0.7, 0.6], BEARD))

# ── 手臂：三角肌、上臂（倒角）与二头肌、肘 ──
ext(rounded_mirrored("deltoid_l", "arm_l", [12.6, 44.4, 0.6], [6.4, 5.4, 7.6], SKIN, 1.0))
ext(rounded_mirrored("upper_arm_l", "arm_l", [12.8, 38.8, 0.7], [5.8, 10.4, 5.8], SKIN, 0.9))
ext(mirrored("bicep_l", "arm_l", [12.8, 39.4, 3.9], [4.4, 5.4, 1.0], SKIN, group="bicep"))
ext(mirrored("elbow_l", "forearm_l", [13.0, 33.6, 0.8], [5.2, 2.6, 5.2], SKIN, group="elbow"))

# 左臂下垂、张开的大手（抓投）：前臂（倒角）、皮护腕、手掌、四指（微屈）、拇指
ext(rounded("forearm_l", "forearm_l", [13.2, 28.2, 1.0], [6.0, 9.4, 6.0], SKIN, 1.0))
add(part("bracer_l", "forearm_l", [13.3, 25.2, 1.1], [6.6, 2.2, 6.6], LEATHER))
add(part("palm_l", "hand_l", [13.8, 20.0, 1.8], [3.4, 6.6, 7.6], SKIN))
add(part("fingers_l", "hand_l", [13.4, 15.0, 2.0], [3.0, 4.4, 7.0], SKIN, (0, 0, -8)))
add(part("knuckle_ridge_l", "hand_l", [15.4, 18.4, 1.8], [0.4, 2.0, 6.6], SKIN_DARK))
add(part("thumb_l", "hand_l", [12.2, 20.0, 6.2], [2.2, 4.2, 2.2], SKIN, (14, 0, 0)))

# 右臂：前臂平端向前（倒角）、护腕、握棍的拳（拳心竖直、拇指在上扣住棍身）、外侧指节
ext(rounded("forearm_r", "forearm_r", [-13.2, 33.6, 5.0], [6.0, 6.0, 9.0], SKIN, 1.0))
add(part("bracer_r", "forearm_r", [-13.2, 33.6, 7.6], [6.6, 6.6, 2.2], LEATHER))
add(part("fist_r", "hand_r", GRIP, [6.0, 6.2, 5.8], SKIN))
add(part("knuckles_r", "hand_r", [GRIP[0] - 3.15, GRIP[1], GRIP[2]], [0.4, 5.2, 4.8], SKIN_DARK))
add(part("thumb_r", "hand_r", [GRIP[0] + 0.6, GRIP[1] + 3.1, GRIP[2] + 1.6], [2.6, 1.4, 2.2], SKIN))

# ── 橄榄木大棒（竖直穿过右拳）：握柄、柄尾、渐粗的中段、倒角棒头、顶盖、三个树疤、一截断枝 ──
GX, GZ = GRIP[0], GRIP[2]
CLUB_PARTS = [
    part("club_handle", "club", [GX, 30.0, GZ], [3.0, 16.0, 3.0], WOOD),
    part("club_butt", "club", [GX, 21.4, GZ], [3.8, 1.8, 3.8], WOOD_DARK),
    *rounded("club_mid", "club", [GX, 42.4, GZ], [4.8, 9.0, 4.8], WOOD, 0.8),
    *rounded("club_head", "club", [GX, 51.2, GZ], [7.2, 8.8, 7.2], WOOD, 1.2),
    part("club_cap", "club", [GX + 0.4, 56.0, GZ - 0.2], [5.2, 1.2, 5.0], WOOD_DARK),
    part("club_knot_a", "club", [GX - 3.6, 50.6, GZ + 1.0], [1.8, 2.0, 2.0], WOOD_DARK, group="club_knot"),
    part("club_knot_b", "club", [GX + 3.0, 45.0, GZ - 1.4], [1.6, 1.8, 1.6], WOOD_DARK, group="club_knot_b"),
    part("club_knot_c", "club", [GX - 0.6, 53.4, GZ + 3.6], [2.0, 1.8, 1.4], WOOD_DARK, group="club_knot_c"),
    part("club_knot_d", "club", [GX + 0.8, 48.6, GZ - 3.6], [2.2, 1.6, 1.4], WOOD_DARK, group="club_knot_d"),
    part("club_twig", "club", [GX + 4.4, 54.4, GZ + 1.0], [1.2, 3.0, 1.2], WOOD, (0, 0, -35)),
]

# ── 羊皮（牧人精英）：背后一大片、搭在左肩、从左肩斜挂到右腰的前片、右腰皮绳结、垂下的两只羊腿 ──
FLEECE_PARTS = [
    part("fleece_back", "fleece", [0, 40.8, -6.8], [17.0, 13.6, 1.2], FLEECE),
    part("fleece_shoulder", "fleece", [8.4, 46.8, 0.4], [8.6, 1.4, 13.4], FLEECE),
    part("fleece_front", "fleece", [4.2, 40.6, 7.6], [9.6, 14.0, 1.0], FLEECE, (0, 0, -22)),
    part("fleece_tie", "fleece", [-2.6, 34.6, 7.9], [2.4, 2.0, 1.0], LEATHER),
    part("fleece_leg_a", "fleece", [9.0, 36.6, 7.9], [1.6, 4.4, 1.0], FLEECE_DARK, group="fleece_leg"),
    part("fleece_leg_b", "fleece", [10.8, 37.4, -7.6], [1.6, 4.4, 1.0], FLEECE_DARK, group="fleece_leg"),
]

# ── 巨石（牧人投石）：倒角石块 + 两块棱角，平时缩成 0 ──
RX, RY, RZ = ROCK
BOULDER_PARTS = [
    *rounded("boulder", "boulder", [RX, RY, RZ], [11.0, 9.6, 10.4], STONE, 1.6),
    part("boulder_lump_a", "boulder", [RX + 3.6, RY + 3.6, RZ + 2.0], [4.0, 3.0, 4.0], STONE),
    part("boulder_lump_b", "boulder", [RX - 3.4, RY - 3.4, RZ - 1.6], [4.4, 3.2, 3.8], STONE),
]


def main():
    pivots = {b[0]: b[2] for b in BONES}
    extra = CLUB_PARTS + FLEECE_PARTS + BOULDER_PARTS
    d = {
        "id": "SU-GR-04",
        "name": "独眼巨人",
        "model": "cyclops",
        "height_limit_u": 60.8,
        "atlas": 512,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [1.8, 3.8, 1.3],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": extra,
        "hidden_bones": {"summon": ["fleece", "boulder"], "shepherd": ["boulder"]},
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["白模 v1（2026-11-17）：前额正中构造圆眼、驼背厚肩长臂、右手竖握橄榄木大棒、左手张开（抓投）、"
                      "皮裹裙与腰绳；牧人版羊皮（fleece）；投掷巨石平时缩成 0。",
                      "白模 v2（2026-11-17，自查）：大棒加粗（倒角中段与棒头、四个树疤）；虹膜也做成横竖交叉的圆；鼻子收小、加鼻梁；"
                      "胡须下移露出嘴、改深褐、加上唇须；蓬发加头顶三簇、额前两绺、后脑垂到肩；左手放大；头颈前移、驼背加厚。",
                      "阶段 ⑥（2026-11-18）：投掷巨石改挂左手掌心内侧（原来挂胸口、固定在头顶上方；右手握着棍，只能左手单手举石）。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8", newline="\n")
    area = sum(2 * (p["size_model_units"][0] * p["size_model_units"][1] + p["size_model_units"][1] * p["size_model_units"][2]
                    + p["size_model_units"][0] * p["size_model_units"][2]) for p in PARTS + extra
               if p["uv_share"] is None or p["id"].endswith("_l") or not p["id"].endswith("_r"))
    print(f"身体 {len(PARTS)} + 棍 {len(CLUB_PARTS)} + 羊皮 {len(FLEECE_PARTS)} + 巨石 {len(BOULDER_PARTS)}"
          f" = {len(PARTS) + len(extra)} 个方块；表面积约 {area:.0f} u²（×4 texel² ≈ {area * 4:.0f}，图集 512² = 262144）")


if __name__ == "__main__":
    main()
