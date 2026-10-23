"""哈耳庇厄（WI-GR-02，RIG-HARPY，T15 阶段 ③）部件表。

生成 art/blockbench/greek/harpy/harpy.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。

造型（docs/重构/设计/逐项设计/希腊首批/08_哈耳庇厄_模型与行为卡.md）：女子的头与上身，穿无袖短希顿（束腰、下摆回纹）；
双臂就是翅膀（翼臂：上臂、前臂、手三段，飞羽朝后）；腰以下是灰黑羽裙、羽腿与黄灰鸟足、黑色钩爪（抓举用脚）；
蓝黑长发（骨骼 hair 随风摆）；下颌单独一骨（jaw），尖啸时张口露出暗红口腔。
**绑定姿势 = 双翼平展**；站立收翼由动画给出（harpy_anim.js）。
运行：python3 tools/blockbench/params/harpy_parts.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/harpy/harpy.params.json"

SKIN = "#c8bcb0"
SKIN_DARK = "#ad9f93"
HAIR = "#23242c"
CHITON = "#7a2e2a"
CHITON_DARK = "#64241f"
BELT = "#3a2a20"
MOUTH = "#4a1a1c"
PLUME = "#4c515b"
PLUME_DARK = "#3c4049"
COVERT = "#5b616c"
FLIGHT = "#2c2e35"
LEG = "#a69a6a"
TALON = "#1c1a18"

HIP = (0.0, 12.5, 0.0)
WAIST = (0.0, 14.2, 0.2)
SHOULDER = (3.4, 20.4, 0.0)
ELBOW_X = 9.4
WRIST_X = 15.2
WY = SHOULDER[1]
HEAD_C = (0.0, 25.2, 0.7)
ANKLE = (1.9, 6.2, -0.6)
FOOT = (1.9, 0.8, 0.4)

BONES = [
    ("root", None, [0, 0, 0]),
    ("body", "root", list(HIP)),
    ("chest", "body", list(WAIST)),
    ("neck", "chest", [0, 21.0, 0.4]),
    ("head", "neck", [0, 22.2, 0.6]),
    ("jaw", "head", [0, 23.0, 0.2]),
    ("hair", "head", [0, 26.6, -1.4]),
    ("wing_l", "chest", list(SHOULDER)),
    ("wing_l_fore", "wing_l", [ELBOW_X, WY, 0]),
    ("wing_l_hand", "wing_l_fore", [WRIST_X, WY, 0]),
    ("wing_r", "chest", [-SHOULDER[0], WY, 0]),
    ("wing_r_fore", "wing_r", [-ELBOW_X, WY, 0]),
    ("wing_r_hand", "wing_r_fore", [-WRIST_X, WY, 0]),
    ("tail", "body", [0, 11.8, -2.2]),
    ("leg_l", "body", [1.9, 12.0, 0.4]),
    ("leg_l_lower", "leg_l", list(ANKLE)),
    ("foot_l", "leg_l_lower", list(FOOT)),
    ("leg_r", "body", [-1.9, 12.0, 0.4]),
    ("leg_r_lower", "leg_r", [-ANKLE[0], ANKLE[1], ANKLE[2]]),
    ("foot_r", "leg_r_lower", [-FOOT[0], FOOT[1], FOOT[2]]),
]

ATTACHMENTS = [
    ("mouth", "jaw", [0, 22.7, 3.15]),
    ("eye", "head", [1.25, 25.3, 3.2]),
    ("grab", "body", [0, -0.6, 1.2]),
    ("foot_l", "foot_l", [1.9, 0, 1.2]),
    ("foot_r", "foot_r", [-1.9, 0, 1.2]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None):
    return {"id": pid, "bone": bone, "center_model_units": [round(c, 4) for c in center],
            "size_model_units": [round(s, 4) for s in size], "rotation_degrees_model_axes": [round(r, 3) for r in rot],
            "color": color, "uv_group": group or pid, "uv_share": None, "surface_kind": "cuboid"}


def mirror_bone(bone):
    if "_l_" in bone:
        return bone.replace("_l_", "_r_")
    if bone.endswith("_l"):
        return bone[:-2] + "_r"
    return bone


def mirrored(pid_l, bone_l, center, size, color, rot=(0, 0, 0), group=None):
    """左侧部件 + 镜像的右侧部件（x 取反、ry 与 rz 取反），两侧共用 UV 组。"""
    g = group or pid_l.removesuffix("_l").replace("_l_", "_")
    left = part(pid_l, bone_l, center, size, color, rot, g)
    pid_r = pid_l[:-2] + "_r" if pid_l.endswith("_l") else pid_l.replace("_l_", "_r_")
    right = part(pid_r, mirror_bone(bone_l), [-center[0], center[1], center[2]], size, color,
                 (rot[0], -rot[1], -rot[2]), g)
    left["uv_share"] = right["uv_share"] = g
    return [left, right]


def rounded(pid, bone, center, size, color, bevel=0.6, rot=(0, 0, 0)):
    """倒角体块：一窄一宽两个方块交叉。"""
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color, rot),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color, rot)]


def toward(start, direction, length):
    """从 start 沿 direction 伸出 length 的方块中心。"""
    n = math.sqrt(sum(c * c for c in direction))
    return [start[i] + direction[i] / n * length / 2 for i in range(3)]


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 下身：羽裙（髋）、前后左右四片下垂羽片（下摆外翻） ──
ext(rounded("hips", "body", [0, 12.4, -0.2], [6.0, 3.2, 4.6], PLUME, 0.7))
add(part("skirt_front", "body", [0, 10.3, 2.0], [5.0, 3.0, 0.6], PLUME_DARK, (-12, 0, 0)))
add(part("skirt_back", "body", [0, 10.4, -2.5], [5.4, 3.4, 0.6], PLUME_DARK, (14, 0, 0)))
ext(mirrored("skirt_side_l", "body", [3.0, 10.5, -0.2], [0.6, 3.0, 4.0], PLUME_DARK, (0, 0, 12), group="skirt_side"))

# ── 尾：三片短尾羽向后下 ──
TAIL_ROOT = [0, 11.6, -2.6]
add(part("tail_mid", "tail", toward(TAIL_ROOT, [0, -0.36, -1], 5.6), [1.8, 0.5, 5.6], FLIGHT, (-20, 0, 0)))
_a = math.radians(16)
ext(mirrored("tail_side_l", "tail", toward([0.9, 11.6, -2.6], [math.sin(_a), -0.34, -math.cos(_a)], 5.0), [1.6, 0.5, 5.0],
             FLIGHT, (-19, -16, 0), group="tail_side"))

# ── 羽腿：大腿羽（膝在前、踝在后，斜 20°）、踝上羽毛翻边；裸露的跗跖、趾、钩爪 ──
ext(mirrored("thigh_l", "leg_l", [1.9, 8.7, 0.3], [2.4, 5.6, 2.6], PLUME, (20, 0, 0)))
ext(mirrored("cuff_l", "leg_l", [1.9, 6.8, -0.35], [2.8, 1.2, 2.8], PLUME_DARK, (20, 0, 0)))
ext(mirrored("tarsus_l", "leg_l_lower", [1.9, 3.5, -0.1], [1.2, 5.6, 1.2], LEG, (-10.5, 0, 0)))
ext(mirrored("scute_l", "leg_l_lower", [1.9, 3.5, 0.55], [1.0, 5.0, 0.3], LEG, (-10.5, 0, 0)))
TOES = (("mid", 0.0, 3.0, 0.8), ("out", 25.0, 2.6, 0.7), ("in", -25.0, 2.6, 0.7))
for name, ang, length, w in TOES:
    a = math.radians(ang)
    d = [math.sin(a), 0, math.cos(a)]
    start = [FOOT[0] + d[0] * 0.4, 0.4, FOOT[2] + 0.4]
    ext(mirrored(f"toe_{name}_l", "foot_l", toward(start, d, length), [w, w, length], LEG, (0, ang, 0), group=f"toe_{name}"))
    tip = [start[0] + d[0] * (length + 0.3), 0.45, start[2] + d[2] * (length + 0.3)]
    ext(mirrored(f"talon_{name}_l", "foot_l", tip, [0.5, 0.9, 0.7], TALON, (30, ang, 0), group="talon"))
ext(mirrored("toe_back_l", "foot_l", [FOOT[0], 0.4, FOOT[2] - 1.2], [0.7, 0.7, 1.8], LEG, group="toe_back"))
ext(mirrored("talon_back_l", "foot_l", [FOOT[0], 0.45, FOOT[2] - 2.35], [0.5, 0.9, 0.7], TALON, (-30, 0, 0), group="talon"))

# ── 上身：希顿（腹、下摆、腰带、倒角胸廓、胸），领口露出锁骨 ──
ext(rounded("abdomen", "chest", [0, 15.7, 0.2], [4.6, 3.0, 3.2], CHITON, 0.5))
add(part("chiton_hem", "chest", [0, 13.7, 0.2], [5.6, 1.6, 4.2], CHITON))
add(part("girdle", "chest", [0, 14.9, 0.2], [4.9, 0.7, 3.5], BELT))
ext(rounded("ribcage", "chest", [0, 18.7, 0.2], [6.0, 3.8, 3.4], CHITON, 0.6))
add(part("bust", "chest", [0, 18.4, 1.95], [4.6, 1.8, 0.8], CHITON_DARK))
add(part("collar", "chest", [0, 20.9, 0.2], [4.4, 0.8, 3.0], SKIN))
add(part("neck", "neck", [0, 21.8, 0.4], [1.8, 1.8, 1.8], SKIN))

# ── 头：颅（脸前面 10 × 9 像素）、鼻、下颌（尖啸张口）与口腔、头发 ──
HX, HY, HZ = HEAD_C
add(part("head", "head", [HX, HY, HZ], [5.0, 4.4, 5.0], SKIN))
add(part("nose", "head", [HX, HY - 0.6, HZ + 2.75], [0.8, 1.2, 0.5], SKIN))
add(part("throat", "head", [0, 22.62, 0.9], [3.4, 0.7, 3.8], MOUTH))
add(part("jaw", "jaw", [0, 22.6, 0.95], [4.0, 0.9, 4.4], SKIN))
add(part("hair_cap", "head", [HX, HY + 2.55, HZ - 0.1], [5.4, 0.8, 5.4], HAIR))
ext(mirrored("hair_side_l", "head", [2.75, HY + 0.5, HZ - 0.6], [0.6, 3.6, 4.0], HAIR, group="hair_side"))
add(part("fringe", "head", [HX, HY + 1.9, HZ + 2.8], [5.0, 0.7, 0.6], HAIR))
add(part("hair_back", "hair", [0, 22.4, -2.3], [5.2, 8.4, 0.8], HAIR, (8, 0, 0)))
ext(mirrored("lock_l", "hair", [1.5, 19.4, -3.3], [1.4, 4.0, 0.6], HAIR, (16, 0, 0), group="lock"))

# ── 翅膀（左；右侧镜像）。绑定姿势平展：上臂 3.4–9.4、前臂 9.4–15.2、手 15.2 起 ──
WING = []


def wing(parts_):
    WING.extend(parts_)


def feather_back(pid, bone, x, attach_z, length, width, ry, y, color=FLIGHT):
    """向后（−Z）伸出的飞羽薄片：从 (x, attach_z) 起，绕 Y 转 ry（负值 = 羽尖外扫）。"""
    a = math.radians(ry)
    cx = x - math.sin(a) * length / 2
    cz = attach_z - math.cos(a) * length / 2
    return mirrored(pid, bone, [cx, y, cz], [width, 0.45, length], color, (0, ry, 0))


wing(mirrored("mantle_l", "wing_l", [3.6, WY + 0.3, 0.0], [3.0, 1.8, 3.6], PLUME, group="mantle"))
wing(mirrored("arm_l", "wing_l", [6.6, WY, 0.2], [6.0, 1.6, 1.8], PLUME, group="arm"))
wing(mirrored("arm_cov_l", "wing_l", [6.6, WY - 0.3, -1.3], [6.0, 0.8, 2.2], COVERT, group="arm_cov"))
for i, (x, L) in enumerate(((5.0, 5.0), (7.6, 5.6))):
    wing(feather_back(f"ter_{i}_l", "wing_l", x, -2.2, L, 2.6, 0, WY - 0.5 - 0.1 * i))
wing(mirrored("fore_l", "wing_l_fore", [12.3, WY, 0.2], [5.8, 1.3, 1.5], PLUME, group="fore"))
wing(mirrored("fore_cov_l", "wing_l_fore", [12.3, WY - 0.3, -1.2], [5.8, 0.8, 2.0], COVERT, group="fore_cov"))
for i, (x, L) in enumerate(((10.4, 6.6), (12.4, 7.0), (14.4, 7.4))):
    wing(feather_back(f"sec_{i}_l", "wing_l_fore", x, -2.0, L, 2.2, -2 * i, WY - 0.5 - 0.1 * i))
wing(mirrored("hand_l", "wing_l_hand", [17.0, WY, 0.2], [3.6, 1.2, 1.4], PLUME, group="hand"))
wing(mirrored("hand_cov_l", "wing_l_hand", [17.0, WY - 0.3, -1.0], [3.6, 0.7, 1.6], COVERT, group="hand_cov"))
for i, (x, L, ry) in enumerate(((15.8, 7.6, -8), (16.9, 8.2, -16), (18.0, 8.6, -24), (19.0, 8.8, -32), (19.8, 8.6, -40))):
    wing(feather_back(f"pri_{i}_l", "wing_l_hand", x, -0.2, L, 2.0, ry, WY - 0.3 - 0.1 * i))


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "WI-GR-02",
        "name": "哈耳庇厄",
        "model": "harpy",
        "height_limit_u": 30.0,
        "atlas": 256,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [0.8, 1.7, 0.8],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": WING,
        "wing": {"y": WY, "shoulder_x": SHOULDER[0], "elbow_x": ELBOW_X, "wrist_x": WRIST_X},
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["白模 v1（2026-10-22）：女子头与上身穿褪红短希顿；翼臂三段、炭黑飞羽；灰黑羽裙、羽腿、黄灰鸟足与黑钩爪；"
                      "蓝黑长发；下颌单独骨骼。绑定姿势双翼平展，站姿由动画收翼。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"身体 {len(PARTS)} + 翅膀 {len(WING)} = {len(PARTS) + len(WING)} 个方块")


if __name__ == "__main__":
    main()
