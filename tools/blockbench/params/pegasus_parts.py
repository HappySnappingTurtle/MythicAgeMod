"""天马（SU-GR-01 召唤 / WI-GR-08 野外，RIG-PEGASUS，T14 阶段 ②）部件表。

生成 art/blockbench/greek/pegasus/pegasus.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。

造型（docs/重构/设计/逐项设计/希腊首批/05_天马_模型与行为卡.md）：白色细腿马身，短竖鬃，青铜蹄；
三段「卷翼」——翼根、中段各有一排淡金覆羽带，后缘错开的白色飞羽，翼梢的长飞羽向前卷（科林斯银币上的古风卷翼）。
**绑定姿势 = 双翼平展**（翼展约 72u ≈ 4.5 格）；站立时的半举收翼由 idle 等动画给出（pegasus_anim.js）。
辔头、鞍只留骨骼（T16 天马辔头圣物）。
运行：python3 tools/blockbench/params/pegasus_parts.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/pegasus/pegasus.params.json"

COAT = "#ece9e2"
COAT_SHADE = "#d9d8d4"
MANE = "#f4efe3"
HOOF = "#93693a"
NOSE = "#cfc6bf"
GOLD = "#e6d6a8"
GOLD_HI = "#efe4c4"
FEATHER = "#f6f4ef"
FEATHER_SHADE = "#e6e4df"

WING_Y = 20.5
WING_Z = 4.5
ROOT_X = 3.2
MID_X = 13.5
TIP_X = 25.5
POLL = (0.0, 27.0, 12.0)
HEAD_PITCH = 40.0  # 头的长轴（+Z）向下倾 40°

BONES = [
    ("root", None, [0, 0, 0]),
    ("body", "root", [0, 16, 0]),
    ("neck", "body", [0, 19, 7]),
    ("head", "neck", list(POLL)),
    ("bridle", "head", list(POLL)),
    ("saddle", "body", [0, 20, -1.5]),
    ("tail", "body", [0, 18.5, -9]),
    ("tail_tip", "tail", [0, 12, -11.8]),
    ("wing_l_root", "body", [ROOT_X, WING_Y, WING_Z]),
    ("wing_l_mid", "wing_l_root", [MID_X, WING_Y, WING_Z]),
    ("wing_l_tip", "wing_l_mid", [TIP_X, WING_Y, WING_Z]),
    ("wing_r_root", "body", [-ROOT_X, WING_Y, WING_Z]),
    ("wing_r_mid", "wing_r_root", [-MID_X, WING_Y, WING_Z]),
    ("wing_r_tip", "wing_r_mid", [-TIP_X, WING_Y, WING_Z]),
    ("leg_fl", "body", [2.6, 14, 6.5]),
    ("leg_fl_lower", "leg_fl", [2.6, 8, 6.5]),
    ("leg_fr", "body", [-2.6, 14, 6.5]),
    ("leg_fr_lower", "leg_fr", [-2.6, 8, 6.5]),
    ("leg_hl", "body", [2.6, 15, -7]),
    ("leg_hl_lower", "leg_hl", [2.6, 8, -8.5]),
    ("leg_hr", "body", [-2.6, 15, -7]),
    ("leg_hr_lower", "leg_hr", [-2.6, 8, -8.5]),
]

ATTACHMENTS = [
    ("rider_seat", "saddle", [0, 22.5, -1.5]),
    ("nose", "head", None),  # 由头部几何算出
    ("eye_glint", "head", None),
    ("hoof_fl", "leg_fl_lower", [2.6, 0, 6.8]),
    ("hoof_fr", "leg_fr_lower", [-2.6, 0, 6.8]),
    ("hoof_hl", "leg_hl_lower", [2.6, 0, -7.9]),
    ("hoof_hr", "leg_hr_lower", [-2.6, 0, -7.9]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None):
    return {"id": pid, "bone": bone, "center_model_units": [round(c, 4) for c in center],
            "size_model_units": list(size), "rotation_degrees_model_axes": [round(r, 3) for r in rot], "color": color,
            "uv_group": group or pid, "uv_share": None, "surface_kind": "cuboid"}


def mirror_bone(bone):
    for a, b in (("_l_", "_r_"), ("_fl", "_fr"), ("_hl", "_hr")):
        if a in bone:
            return bone.replace(a, b)
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
    """倒角体块：一窄一宽两个方块交叉（检查清单 §二.2）。"""
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color, rot),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color, rot)]


def head_point(along, up, x=0.0):
    """头部局部坐标（沿头的长轴 along、垂直向上 up）→ 模型坐标。长轴 = +Z 绕 X 向下转 HEAD_PITCH。"""
    a = math.radians(HEAD_PITCH)
    d = (0.0, -math.sin(a), math.cos(a))
    u = (0.0, math.cos(a), math.sin(a))
    return [POLL[0] + x, POLL[1] + along * d[1] + up * u[1], POLL[2] + along * d[2] + up * u[2]]


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 身体：倒角桶身、前胸、臀部（略高于鬐甲后方）、鬐甲、肩与后腿肌群 ──
ext(rounded("barrel", "body", [0, 16, 0], [8, 8, 18], COAT, 1.0))
add(part("breast", "body", [0, 15.2, 8.6], [6.6, 6.6, 2.6], COAT))
ext(rounded("croup", "body", [0, 18.0, -6.6], [7.6, 4.2, 8.0], COAT, 0.8))
add(part("withers", "body", [0, 20.1, 4.6], [4.0, 1.6, 5.0], COAT))
# 肩胛斜面（贴着桶身，不外凸成方块）、腹线（桶身下沿收窄）
ext(mirrored("shoulder_l", "body", [3.3, 16.4, 5.6], [1.6, 6.0, 4.0], COAT, (-18, 0, 0)))
ext(mirrored("haunch_l", "leg_hl", [3.1, 15.0, -7.4], [2.0, 6.0, 5.6], COAT))
add(part("belly", "body", [0, 11.6, -0.5], [6.0, 1.2, 12.0], COAT))

# ── 颈：自鬐甲前斜向前上（前倾 32°），短竖鬃沿颈背，额前一撮 ──
NECK_RX = 32.0
ext(rounded("neck", "neck", [0, 23.0, 9.5], [3.8, 10.0, 5.0], COAT, 0.5, (NECK_RX, 0, 0)))
add(part("neck_base", "neck", [0, 20.6, 7.8], [4.8, 5.0, 5.6], COAT, (NECK_RX, 0, 0)))
add(part("mane", "neck", [0, 24.6, 7.1], [1.2, 9.4, 1.6], MANE, (NECK_RX, 0, 0)))

# ── 头：颅、颊、吻、鼻端、下唇（沿 40° 下倾的长轴排布）；耳朵竖立、略外张 ──
H = HEAD_PITCH
add(part("cranium", "head", head_point(2.5, -0.8), [4.4, 4.6, 5.0], COAT, (H, 0, 0)))
add(part("jowl", "head", head_point(2.0, -2.6), [4.8, 3.0, 3.6], COAT, (H, 0, 0)))
add(part("muzzle", "head", head_point(7.5, -0.6), [3.4, 3.6, 6.0], COAT, (H, 0, 0)))
add(part("nose", "head", head_point(10.6, -0.6), [3.6, 3.8, 1.6], NOSE, (H, 0, 0)))
add(part("lip", "head", head_point(9.3, -2.3), [2.6, 1.2, 3.0], NOSE, (H, 0, 0)))
add(part("forelock", "head", head_point(1.6, 2.2), [1.8, 1.0, 2.6], MANE, (H, 0, 0)))
ext(mirrored("ear_l", "head", head_point(0.4, 2.4, 1.3), [1.0, 2.6, 1.0], COAT, (12, 0, -10)))

# ── 腿：前腿（前臂、腕、管骨、球节、青铜蹄）；后腿（胫、飞节、管骨、球节、蹄） ──
for side, bx in (("l", 2.6),):
    ext(mirrored(f"forearm_{side}", f"leg_f{side}", [bx, 11.0, 6.5], [2.4, 6.5, 2.8], COAT, group="forearm"))
    ext(mirrored(f"knee_f_{side}", f"leg_f{side}_lower", [bx, 8.0, 6.6], [2.4, 1.4, 2.6], COAT_SHADE, group="knee"))
    ext(mirrored(f"cannon_f_{side}", f"leg_f{side}_lower", [bx, 4.75, 6.5], [1.6, 5.5, 1.8], COAT, group="cannon"))
    ext(mirrored(f"fetlock_f_{side}", f"leg_f{side}_lower", [bx, 2.0, 6.6], [2.0, 1.2, 2.0], COAT_SHADE, group="fetlock"))
    ext(mirrored(f"hoof_f_{side}", f"leg_f{side}_lower", [bx, 0.75, 6.8], [2.2, 1.5, 2.6], HOOF, group="hoof"))
    ext(mirrored(f"gaskin_{side}", f"leg_h{side}", [bx, 10.6, -8.0], [2.2, 5.2, 2.6], COAT, (10, 0, 0), group="gaskin"))
    ext(mirrored(f"hock_{side}", f"leg_h{side}_lower", [bx, 8.0, -8.8], [2.0, 1.6, 2.4], COAT_SHADE, group="hock"))
    ext(mirrored(f"cannon_h_{side}", f"leg_h{side}_lower", [bx, 4.75, -8.3], [1.6, 5.5, 1.8], COAT, group="cannon"))
    ext(mirrored(f"fetlock_h_{side}", f"leg_h{side}_lower", [bx, 2.0, -8.1], [2.0, 1.2, 2.0], COAT_SHADE, group="fetlock"))
    ext(mirrored(f"hoof_h_{side}", f"leg_h{side}_lower", [bx, 0.75, -7.9], [2.2, 1.5, 2.6], HOOF, group="hoof"))

# ── 尾：尾根 + 两段长尾（自尾根向下、向后飘） ──
add(part("tail_dock", "tail", [0, 18.0, -9.8], [1.6, 2.2, 2.0], MANE, (-30, 0, 0)))
add(part("tail_hair", "tail", [0, 15.0, -11.0], [2.4, 6.4, 1.6], MANE, (15, 0, 0)))
add(part("tail_hair2", "tail_tip", [0, 8.6, -12.2], [2.8, 7.0, 1.8], MANE, (8, 0, 0)))
add(part("tail_end", "tail_tip", [0, 4.6, -12.6], [2.0, 2.0, 1.4], MANE, (4, 0, 0)))

# ── 翅膀（左；右侧镜像）。绑定姿势平展：翼根 x 3.2–13.5、中段 13.5–25.5、翼梢 25.5 起 ──
WING = []


def wing(*parts_):
    for p in parts_:
        WING.extend(p)


def feather_back(pid, bone, x, attach_z, length, width, ry, y):
    """向后（−Z）伸出的飞羽薄片：从 (x, attach_z) 起，绕 Y 转 ry（负值 = 羽尖外扫）。"""
    a = math.radians(ry)
    cx = x - math.sin(a) * length / 2
    cz = attach_z - math.cos(a) * length / 2
    return mirrored(pid, bone, [cx, y, cz], [width, 0.5, length], FEATHER, (0, ry, 0))


def feather_out(pid, bone, x, attach_z, length, width, ry, y):
    """向外（+X）伸出的翼梢长羽：绕 Y 转 ry（负值 = 羽尖前卷）。"""
    a = math.radians(ry)
    cx = x + math.cos(a) * length / 2
    cz = attach_z - math.sin(a) * length / 2
    return mirrored(pid, bone, [cx, y, cz], [length, 0.5, width], FEATHER, (0, ry, 0))


# 翼根：骨板（金色覆羽带）+ 第二排覆羽 + 4 片次级飞羽
wing(mirrored("wing_l_root_band", "wing_l_root", [8.35, WING_Y, 4.3], [10.3, 1.2, 4.0], GOLD, group="wing_root_band"))
wing(mirrored("wing_l_root_cov", "wing_l_root", [8.35, WING_Y - 0.25, 1.6], [10.3, 0.8, 2.2], GOLD_HI, group="wing_root_cov"))
for i, (x, L) in enumerate(((4.8, 7.0), (7.4, 7.6), (10.0, 8.2), (12.6, 8.8))):
    wing(feather_back(f"sec_{i}_l", "wing_l_root", x, 1.2, L, 3.2, -4 * i, WING_Y - 0.3 - 0.12 * i))
# 中段：覆羽带 + 第二排 + 4 片初级飞羽（后缘外扫）
wing(mirrored("wing_l_mid_band", "wing_l_mid", [19.5, WING_Y, 4.4], [12.0, 1.1, 3.6], GOLD, group="wing_mid_band"))
wing(mirrored("wing_l_mid_cov", "wing_l_mid", [19.5, WING_Y - 0.25, 2.0], [12.0, 0.7, 2.0], GOLD_HI, group="wing_mid_cov"))
for i, (x, L, ry) in enumerate(((15.0, 9.5, -10), (18.0, 10.5, -15), (21.0, 11.0, -21), (24.0, 11.0, -27))):
    wing(feather_back(f"pri_{i}_l", "wing_l_mid", x, 1.6, L, 3.8, ry, WING_Y - 0.3 - 0.12 * i))
# 翼梢：小覆羽 + 4 根长羽，由后到前依次前卷（古风卷翼）
wing(mirrored("wing_l_tip_band", "wing_l_tip", [28.3, WING_Y, 3.6], [5.6, 1.0, 4.0], GOLD, group="wing_tip_band"))
for i, (z, L, ry) in enumerate(((-1.4, 10.0, -5), (0.6, 10.0, -18), (2.5, 9.4, -32), (4.2, 8.4, -48))):
    wing(feather_out(f"cur_{i}_l", "wing_l_tip", TIP_X + 0.4, z, L, 2.8, ry, WING_Y - 0.1 * i))


def main():
    pivots = {b[0]: b[2] for b in BONES}
    atts = []
    for aid, bone, pos in ATTACHMENTS:
        if aid == "nose":
            pos = head_point(11.4, -0.6)
        elif aid == "eye_glint":
            pos = head_point(2.6, 0.4)
        atts.append((aid, bone, [round(c, 4) for c in pos]))
    d = {
        "id": "SU-GR-01",
        "name": "天马",
        "model": "pegasus",
        "height_limit_u": 33.0,
        "atlas": 256,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [0.9, 1.5, 0.9],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": WING,
        "wing": {"y": WING_Y, "z": WING_Z, "root_x": ROOT_X, "mid_x": MID_X, "tip_x": TIP_X},
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in atts]
    d["revisions"] = ["白模 v1（2026-10-21）：白色细腿马身、短竖鬃、青铜蹄；三段卷翼（金色覆羽带 + 错开飞羽，翼梢前卷）；"
                      "绑定姿势双翼平展，站姿由动画半举收翼。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"身体 {len(PARTS)} + 翅膀 {len(WING)} = {len(PARTS) + len(WING)} 个方块")


if __name__ == "__main__":
    main()
