"""斯廷法利斯怪鸟（WI-GR-03，RIG-STYMPHALIAN，T15 阶段 ②）部件表。

生成 art/blockbench/greek/stymphalian/stymphalian.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。

造型（docs/重构/设计/逐项设计/希腊首批/07_斯廷法利斯怪鸟_模型与行为卡.md）：鹤大小的长颈水鸟，直而粗的青铜色喙，
暗青铜褐身体羽，青铜覆羽，磨亮的青铜飞羽与尾羽；长腿三前趾一后趾；领头鸟头后三根青铜冠羽（骨骼 crest）。
**绑定姿势 = 双翼平展**（飞羽朝后）；站立收翼由动画给出（stymphalian_anim.js）。
运行：python3 tools/blockbench/params/stymphalian_parts.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/stymphalian/stymphalian.params.json"

PLUME = "#4a3a2c"
PLUME_DARK = "#3e3125"
THROAT = "#5a4733"
COVERT = "#7a5a30"
COVERT_HI = "#86643a"
FEATHER = "#9c6a2a"
BEAK = "#5c4a32"
LEG = "#3d342c"
TALON = "#a07a40"

WING_Y = 15.2
WING_Z = 2.5
SHOULDER_X = 2.8
WRIST_X = 11.8

BONES = [
    ("root", None, [0, 0, 0]),
    ("body", "root", [0, 12.5, 0]),
    ("neck", "body", [0, 14.5, 4.5]),
    ("head", "neck", [0, 21, 7]),
    ("crest", "head", [0, 22.2, 6.2]),
    ("tail", "body", [0, 13.8, -5]),
    ("wing_l", "body", [SHOULDER_X, WING_Y, WING_Z]),
    ("wing_l_hand", "wing_l", [WRIST_X, WING_Y, WING_Z]),
    ("wing_r", "body", [-SHOULDER_X, WING_Y, WING_Z]),
    ("wing_r_hand", "wing_r", [-WRIST_X, WING_Y, WING_Z]),
    ("leg_l", "body", [1.6, 10.5, 0.3]),
    ("leg_l_lower", "leg_l", [1.6, 6.0, 0.3]),
    ("leg_r", "body", [-1.6, 10.5, 0.3]),
    ("leg_r_lower", "leg_r", [-1.6, 6.0, 0.3]),
]

ATTACHMENTS = [
    ("beak_tip", "head", [0, 20.75, 15.1]),
    ("eye", "head", [1.45, 22.0, 8.2]),
    ("launch", "body", [0, 9.0, 1.0]),
    ("foot_l", "leg_l_lower", [1.6, 0, 0.8]),
    ("foot_r", "leg_r_lower", [-1.6, 0, 0.8]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None):
    return {"id": pid, "bone": bone, "center_model_units": [round(c, 4) for c in center],
            "size_model_units": list(size), "rotation_degrees_model_axes": [round(r, 3) for r in rot], "color": color,
            "uv_group": group or pid, "uv_share": None, "surface_kind": "cuboid"}


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


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 身体：倒角躯干（前高后低 8°）、胸、背上覆羽披、臀 ──
ext(rounded("torso", "body", [0, 12.5, 0], [6.0, 5.6, 10.0], PLUME, 0.8, (-8, 0, 0)))
ext(rounded("breast", "body", [0, 12.9, 5.0], [5.0, 4.8, 2.6], PLUME, 0.6, (-14, 0, 0)))
add(part("mantle", "body", [0, 15.4, -0.8], [5.0, 0.8, 7.0], COVERT, (-8, 0, 0)))
add(part("rump", "body", [0, 12.9, -5.4], [4.2, 3.6, 3.0], PLUME_DARK, (-8, 0, 0)))
add(part("belly", "body", [0, 9.9, 0.2], [4.4, 0.8, 6.0], PLUME_DARK))

# ── 颈：下段前倾 26°、上段略后仰（S 形）；喉部略浅 ──
ext(rounded("neck_low", "neck", [0, 16.7, 5.7], [2.8, 5.2, 2.8], PLUME, 0.4, (26, 0, 0)))
add(part("neck_up", "neck", [0, 19.9, 6.8], [2.2, 3.6, 2.2], PLUME, (-6, 0, 0)))
add(part("throat", "neck", [0, 18.9, 7.7], [1.6, 3.0, 0.6], THROAT, (10, 0, 0)))

# ── 头：颅、颊；直喙（根粗、尖细、略下倾 4°） ──
add(part("cranium", "head", [0, 21.7, 7.7], [2.8, 2.8, 3.6], PLUME))
add(part("cheek", "head", [0, 21.0, 8.9], [2.4, 1.6, 1.8], THROAT))
add(part("beak_base", "head", [0, 21.25, 10.8], [1.5, 1.4, 2.8], BEAK, (4, 0, 0)))
add(part("beak_tip", "head", [0, 20.95, 13.6], [0.9, 0.9, 3.0], BEAK, (4, 0, 0)))
add(part("beak_gape", "head", [0, 20.75, 10.4], [1.2, 0.4, 1.8], PLUME_DARK, (4, 0, 0)))

# ── 领头鸟冠羽：头后三根青铜长羽，向后上扬 ──
add(part("crest_mid", "crest", [0, 23.2, 4.8], [0.6, 0.4, 4.0], FEATHER, (-28, 0, 0)))
ext(mirrored("crest_l", "crest", [0.6, 22.9, 5.0], [0.5, 0.4, 3.4], FEATHER, (-20, 12, 0), group="crest_side"))

# ── 尾：三根青铜尾羽，中间平、两侧外张 ──
add(part("tail_mid", "tail", [0, 13.4, -8.2], [1.8, 0.5, 6.4], FEATHER, (-8, 0, 0)))
for i, (x, ry) in enumerate(((1.4, 14),)):
    a = math.radians(ry)
    ext(mirrored("tail_side_l", "tail", [x + math.sin(a) * 2.6, 13.3, -7.6 - math.cos(a) * 0.4], [1.8, 0.5, 5.8], FEATHER,
                 (-8, ry, 0), group="tail_side"))
add(part("tail_coverts", "tail", [0, 13.9, -5.6], [3.2, 0.9, 2.4], COVERT))

# ── 腿：大腿羽、胫（裸露）、踝、跗跖、三前趾一后趾、青铜爪 ──
ext(mirrored("thigh_l", "leg_l", [1.6, 9.8, 0.3], [1.8, 2.6, 2.2], PLUME_DARK))
ext(mirrored("tibia_l", "leg_l", [1.6, 7.8, 0.3], [0.8, 3.4, 0.8], LEG))
ext(mirrored("ankle_l", "leg_l_lower", [1.6, 6.0, 0.3], [1.0, 0.8, 1.0], LEG))
ext(mirrored("tarsus_l", "leg_l_lower", [1.6, 3.3, 0.3], [0.8, 5.4, 0.8], LEG))
ext(mirrored("toe_mid_l", "leg_l_lower", [1.6, 0.25, 1.8], [0.6, 0.5, 3.0], LEG))
ext(mirrored("toe_out_l", "leg_l_lower", [2.25, 0.25, 1.5], [0.5, 0.5, 2.6], LEG, (0, 28, 0)))
ext(mirrored("toe_in_l", "leg_l_lower", [0.95, 0.25, 1.5], [0.5, 0.5, 2.6], LEG, (0, -28, 0)))
ext(mirrored("toe_back_l", "leg_l_lower", [1.6, 0.25, -0.7], [0.5, 0.5, 1.4], LEG))
ext(mirrored("talon_l", "leg_l_lower", [1.6, 0.2, 3.45], [0.5, 0.4, 0.5], TALON))

# ── 翅膀（左；右侧镜像）。绑定姿势平展：内翼 x 2.8–11.8、外翼 11.8 起 ──
WING = []


def wing(*parts_):
    for p in parts_:
        WING.extend(p)


def feather_back(pid, bone, x, attach_z, length, width, ry, y, color=FEATHER):
    """向后（−Z）伸出的飞羽薄片：从 (x, attach_z) 起，绕 Y 转 ry（负值 = 羽尖外扫）。"""
    a = math.radians(ry)
    cx = x - math.sin(a) * length / 2
    cz = attach_z - math.cos(a) * length / 2
    return mirrored(pid, bone, [cx, y, cz], [width, 0.5, length], color, (0, ry, 0))


# 内翼：前缘覆羽带 + 第二排覆羽 + 4 片次级飞羽
wing(mirrored("wing_l_band", "wing_l", [7.3, WING_Y, 2.2], [9.0, 1.2, 3.0], COVERT, group="wing_band"))
wing(mirrored("wing_l_cov", "wing_l", [7.3, WING_Y - 0.25, 0.1], [9.0, 0.8, 2.0], COVERT_HI, group="wing_cov"))
for i, (x, L) in enumerate(((4.0, 6.6), (6.3, 7.0), (8.6, 7.4), (10.9, 7.6))):
    wing(feather_back(f"sec_{i}_l", "wing_l", x, -0.4, L, 2.4, -2 * i, WING_Y - 0.35 - 0.1 * i))
# 外翼：覆羽带 + 5 片初级飞羽（后缘外扫）
wing(mirrored("hand_l_band", "wing_l_hand", [15.6, WING_Y, 2.0], [7.6, 1.0, 2.6], COVERT, group="hand_band"))
wing(mirrored("hand_l_cov", "wing_l_hand", [15.6, WING_Y - 0.2, 0.3], [7.6, 0.7, 1.6], COVERT_HI, group="hand_cov"))
for i, (x, L, ry) in enumerate(((12.6, 7.0, -6), (14.4, 7.5, -12), (16.2, 8.0, -20), (18.0, 8.5, -28), (19.6, 8.5, -36))):
    wing(feather_back(f"pri_{i}_l", "wing_l_hand", x, 0.8, L, 2.2, ry, WING_Y - 0.3 - 0.1 * i))


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "WI-GR-03",
        "name": "斯廷法利斯怪鸟",
        "model": "stymphalian_bird",
        "height_limit_u": 24.0,
        "atlas": 256,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [0.8, 1.0, 0.8],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": WING,
        "wing": {"y": WING_Y, "z": WING_Z, "shoulder_x": SHOULDER_X, "wrist_x": WRIST_X},
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["白模 v1（2026-10-21）：鹤大小长颈水鸟、直喙、暗青铜褐身体、青铜覆羽与亮青铜飞羽；领头鸟冠羽；"
                      "绑定姿势双翼平展，站姿由动画收翼。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"身体 {len(PARTS)} + 翅膀 {len(WING)} = {len(PARTS) + len(WING)} 个方块")


if __name__ == "__main__":
    main()
