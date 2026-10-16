"""神庙祭司（希腊守护者，T9b）部件表。

生成 art/blockbench/greek/greek_guardian/greek_guardian.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。
身体沿用陶工 VI-GR-POT-01 的骨架；右前臂在静止姿势中前伸托碗（T9b 设计文档 §2）。
运行：python3 tools/blockbench/params/greek_guardian_parts.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/greek_guardian/greek_guardian.params.json"

SKIN = "#AC7859"
ROBE = "#E9E2CF"
ROBE_SHADE = "#D3CAB2"
MANTLE = "#E4DCC6"
PURPLE = "#6B2A78"
HAIR = "#8E877D"
BEARD = "#A79F92"
LAUREL = "#5B7A36"
LEAF = "#6E8F3F"
GOLD = "#C9A13B"
BRONZE = "#B0813A"
WINE = "#6A1E2A"
LEATHER = "#5A3B26"

BONES = [
    ("root", None, [0, 0, 0]),
    ("pelvis", "root", [0, 14, 0]),
    ("torso", "pelvis", [0, 16, 0]),
    ("head", "torso", [0, 22, 0]),
    ("mantle", "torso", [0, 21.5, -2.4]),
    ("hem_front", "pelvis", [0, 10, 0]),
    ("hem_back", "pelvis", [0, 10, 0]),
    ("upper_arm_r", "torso", [-5.5, 21.5, 0]),
    ("forearm_r", "upper_arm_r", [-5.5, 15.5, 0]),
    ("hand_r", "forearm_r", [-5.5, 15.5, 4]),
    ("bowl", "hand_r", [-5.5, 15.4, 5.2]),
    ("thigh_r", "pelvis", [-2, 14, 0]),
    ("shin_r", "thigh_r", [-2, 8, 0]),
    ("foot_r", "shin_r", [-2, 2, 0]),
    ("upper_arm_l", "torso", [5.5, 21.5, 0]),
    ("forearm_l", "upper_arm_l", [5.5, 15.5, 0]),
    ("hand_l", "forearm_l", [5.5, 11.5, 0]),
    ("thigh_l", "pelvis", [2, 14, 0]),
    ("shin_l", "thigh_l", [2, 8, 0]),
    ("foot_l", "shin_l", [2, 2, 0]),
]

ATTACHMENTS = [
    ("bowl_contact", "bowl", [-5.5, 16.5, 5.2]),
    ("point_tip", "hand_l", [5.5, 9.5, 0]),
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


def rounded(pid, bone, center, size, color, bevel=0.5):
    """倒角体块：一窄一宽两个方块交叉（检查清单 §二.2）。主块前后全深，侧块左右全宽。"""
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color)]


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 头：方头、方鼻、灰白短发、胡须 ──
add(part("head", "head", [0, 26, 0], [8, 8, 8], SKIN))
add(part("nose", "head", [0, 24.75, 5], [2, 3.5, 2], SKIN))
add(part("hair_cap", "head", [0, 30.25, 0], [8.6, 1.5, 8.6], HAIR))
add(part("hair_back", "head", [0, 27, -4.3], [8.6, 5.5, 0.6], HAIR))
ext(mirrored("hair_side_l", "head", [4.3, 27.75, -1], [0.6, 3.5, 6], HAIR))
add(part("beard_front", "head", [0, 23.0, 4.35], [7.0, 3.0, 0.7], BEARD))
add(part("beard_chin", "head", [0, 21.25, 4.0], [4.0, 1.5, 1.6], BEARD))
ext(mirrored("beard_side_l", "head", [4.15, 23.6, 1.8], [0.6, 3.2, 4.4], BEARD))
ext(mirrored("ear_l", "head", [4.2, 25.6, 0.0], [0.6, 1.8, 1.4], SKIN))

# ── 月桂冠：两侧与后脑的冠带，每侧 3 片斜叶，额前两片叶尖，后脑两条紫缎带 ──
ext(mirrored("wreath_side_l", "head", [4.55, 28.6, 0.2], [0.7, 1.0, 8.2], LAUREL))
add(part("wreath_back", "head", [0, 28.6, -4.75], [8.8, 1.0, 0.7], LAUREL))
for i, (z, dy, rx) in enumerate(((3.0, 0.45, -28), (0.8, -0.45, 28), (-1.4, 0.45, -28))):
    ext(mirrored(f"leaf{i}_l", "head", [4.95, 28.6 + dy, z], [0.5, 0.8, 2.2], LEAF, (rx, 0, 0), group="laurel_leaf"))
ext(mirrored("leaf_front_l", "head", [3.4, 28.75, 4.45], [1.8, 0.7, 0.5], LEAF, (0, 0, -8), group="laurel_leaf_front"))
ext(mirrored("ribbon_l", "head", [1.2, 26.4, -5.15], [0.9, 4.2, 0], PURPLE, (0, 0, -8), group="ribbon",
             kind="single_plane", normal=[0, 0, -1]))

# ── 躯干：长袍上身（倒角）、胸前折返、斜披带、肩搭布 ──
add(part("neck", "torso", [0, 22, 0], [2, 2, 2], SKIN))
ext(rounded("chiton", "torso", [0, 18, 0], [8, 9, 4], ROBE, 0.4))
add(part("overfold", "torso", [0, 19.6, 0], [8.5, 3.2, 4.5], ROBE))
add(part("sash_front", "torso", [0.2, 18, 2.5], [2.4, 11.5, 0.5], MANTLE, (0, 0, -38), group="sash"))
add(part("sash_back", "torso", [0.2, 18, -2.5], [2.4, 11.5, 0.5], MANTLE, (0, 0, -38), group="sash"))
ext(mirrored("mantle_shoulder_l", "torso", [3.6, 21.75, -0.6], [3.0, 0.8, 4.2], MANTLE))
# 两肩的金扣针（fibula），把披风别在肩上
ext(mirrored("fibula_l", "torso", [3.6, 21.5, 1.6], [1.2, 1.2, 0.5], GOLD))

# ── 披风：背后一片，及小腿，独立骨骼 ──
add(part("mantle", "mantle", [0, 13.75, -2.65], [8.6, 15.5, 0.5], MANTLE))
ext(mirrored("mantle_fold_l", "mantle", [2.0, 12.5, -2.95], [0.8, 12, 0.3], MANTLE, group="mantle_fold"))

# ── 骨盆：腰绳、绳结与垂穗、长袍下摆上段（倒角）与前褶 ──
add(part("girdle", "pelvis", [0, 15.2, 0], [8.4, 1.0, 4.4], PURPLE))
add(part("girdle_knot", "pelvis", [0, 15.0, 2.45], [1.4, 1.4, 0.5], GOLD))
ext(mirrored("girdle_tail_l", "pelvis", [0.5, 13.4, 2.55], [0.6, 2.2, 0], PURPLE, (0, 0, -6), group="girdle_tail",
             kind="single_plane", normal=[0, 0, 1]))
ext(rounded("skirt", "pelvis", [0, 12.25, 0], [8.6, 5.5, 4.6], ROBE, 0.4))
ext(mirrored("pleat_upper_l", "pelvis", [2.0, 12.25, 2.45], [0.7, 5.0, 0.3], ROBE_SHADE, group="pleat_upper"))

# ── 下摆下段：前后两片，可摆动；底部紫色宽边与竖褶 ──
for side, sz in (("front", 1), ("back", -1)):
    bone = f"hem_{side}"
    add(part(f"hem_{side}", bone, [0, 7.25, 1.2 * sz], [8.8, 5.5, 2.4], ROBE, group="hem"))
    add(part(f"hem_{side}_band", bone, [0, 4.9, 1.25 * sz], [9.0, 0.8, 2.6], PURPLE, group="hem_band"))
xs = (-2.6, 0, 2.6)
for i, x in enumerate(xs):
    add(part(f"pleat_front_{i}", "hem_front", [x, 7.6, 2.55], [0.7, 4.6, 0.3], ROBE_SHADE, group="pleat"))
for i, x in enumerate((-1.8, 1.8)):
    add(part(f"pleat_back_{i}", "hem_back", [x, 7.6, -2.55], [0.7, 4.6, 0.3], ROBE_SHADE, group="pleat"))

# ── 腿脚：小腿、踝带、脚、鞋底、趾带 ──
ext(mirrored("shin_l", "shin_l", [2, 5, 0], [2.8, 6, 2.8], SKIN))
ext(mirrored("ankle_strap_l", "shin_l", [2, 2.6, 0], [3.1, 0.6, 3.1], LEATHER))
ext(mirrored("foot_l", "foot_l", [2, 1.25, 0.8], [3, 1.5, 4.4], SKIN))
ext(mirrored("sole_l", "foot_l", [2, 0.3, 0.9], [3.4, 0.6, 5.0], LEATHER))
ext(mirrored("toe_strap_l", "foot_l", [2, 2.05, 1.6], [3.2, 0.4, 0.8], LEATHER))

# ── 手臂：短袖与紫边、上臂、前臂、金手镯 ──
ext(mirrored("sleeve_l", "upper_arm_l", [5.6, 19.6, 0], [3.6, 4.2, 4.4], ROBE))
ext(mirrored("sleeve_band_l", "upper_arm_l", [5.6, 17.7, 0], [3.8, 0.7, 4.6], PURPLE))
ext(mirrored("upper_arm_l", "upper_arm_l", [5.5, 16.5, 0], [3, 3, 3], SKIN))
# 左前臂下垂；右前臂向前平伸（托碗）
add(part("forearm_l", "forearm_l", [5.5, 13.5, 0], [3, 4, 3], SKIN, group="forearm"))
add(part("forearm_r", "forearm_r", [-5.5, 14.0, 1.9], [3, 2.8, 3.6], SKIN, group="forearm_level"))
add(part("bracelet_l", "forearm_l", [5.5, 12.2, 0], [3.3, 0.6, 3.3], GOLD, group="bracelet"))
add(part("bracelet_r", "forearm_r", [-5.5, 14.0, 3.0], [3.3, 3.1, 0.6], GOLD, group="bracelet_level"))
# 左手空手（拇指在内侧）；右手掌心向上，拇指沿内侧向前，指尖在前缘微微上卷托住碗
add(part("hand_l", "hand_l", [5.5, 10.5, 0], [3, 2, 3], SKIN, group="hand_open"))
add(part("thumb_l", "hand_l", [3.75, 10.9, 0.9], [0.6, 1.4, 1.0], SKIN, group="thumb_l"))
add(part("hand_r", "hand_r", [-5.5, 14.6, 5.85], [3, 1.6, 3.9], SKIN, group="hand_cup"))
# 拇指贴在碗壁内侧一边的外面（不穿过碗底），指尖在碗前沿外侧向上扣住：碗由掌心与手指托稳
add(part("thumb_r", "hand_r", [-3.3, 15.3, 5.5], [1.4, 0.6, 1.0], SKIN, group="thumb_r"))
add(part("thumb_tip_r", "hand_r", [-2.65, 15.85, 5.5], [0.5, 1.7, 1.0], SKIN, group="thumb_tip_r"))
add(part("fingers_r", "hand_r", [-5.5, 15.8, 8.05], [3, 1.6, 0.5], SKIN, group="fingers_r"))

# ── 奠酒碗（phiale）：碗底、四面碗壁、碗心金凸、碗内红酒 ──
B = (-5.5, 5.2)
EXTRA = [
    part("phiale_base", "bowl", [B[0], 15.7, B[1]], [3.2, 0.6, 3.2], BRONZE),
    part("phiale_wall_front", "bowl", [B[0], 16.4, B[1] + 2.2], [5.2, 0.8, 0.8], BRONZE, group="phiale_wall_x"),
    part("phiale_wall_back", "bowl", [B[0], 16.4, B[1] - 2.2], [5.2, 0.8, 0.8], BRONZE, group="phiale_wall_x"),
    part("phiale_wall_left", "bowl", [B[0] + 2.2, 16.4, B[1]], [0.8, 0.8, 3.6], BRONZE, group="phiale_wall_z"),
    part("phiale_wall_right", "bowl", [B[0] - 2.2, 16.4, B[1]], [0.8, 0.8, 3.6], BRONZE, group="phiale_wall_z"),
    part("phiale_wine", "bowl", [B[0], 16.25, B[1]], [3.6, 0, 3.6], WINE, kind="single_plane", normal=[0, 1, 0]),
    part("phiale_boss", "bowl", [B[0], 16.3, B[1]], [1.0, 0.5, 1.0], GOLD),
]


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "VI-GR-PRI-01",
        "name": "神庙祭司",
        "model": "greek_guardian",
        "height_limit_u": 31.2,
        "atlas": 256,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [0.6, 1.95, 0.6],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": EXTRA,
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["T9b-R3（贴图 v1 自查）：前臂压到碗底以下并缩短（原从碗后壁穿出）；手掌延长到碗前沿，手指从掌心前端上扣碗壁，拇指分横竖两段从掌缘接到碗壁外侧（原拇指穿出碗底、手指悬空）。", "T9b-R2（2026-10-15 用户：口斜眼歪）：五官改为按像素镜像绘制，瞳孔居中，胡须宽 7u（14 像素，偶数）让嘴居中。", "T9b-R1（2026-10-15 白模 v1，自查后补：耳、肩扣针、披风竖褶，额前叶尖放平）：沿用陶工骨架；及踝希顿长袍 + 胸前折返 + 斜披带 + 背后披风；"
                      "前后两片可摆动的下摆；灰白须发与月桂冠；右手掌心向上托奠酒碗。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"部件 {len(PARTS)} + 附加 {len(EXTRA)} = {len(PARTS) + len(EXTRA)} 个方块")


if __name__ == "__main__":
    main()
