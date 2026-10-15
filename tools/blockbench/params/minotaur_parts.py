"""弥诺陶洛斯部件表（白模 v2，2026-10-14 用户要求精修）。

生成 art/blockbench/greek/minotaur/minotaur.params.json 的 parts / extra_parts / bones / attachments。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。
左右对称的部件只写左侧（x > 0），右侧由镜像生成：x 取反，旋转 (rx, ry, rz) → (rx, -ry, -rz)。
运行：python3 tools/blockbench/params/minotaur_parts.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/minotaur/minotaur.params.json"

FUR = "#865632"       # 中棕皮毛
FUR_DARK = "#6E4428"  # 腹侧、四肢内侧用的深一阶
MANE = "#47362D"      # 深棕鬃毛
IVORY = "#CAB38C"     # 乳白：鼻吻、角
HORN_TIP = "#3A2E26"
HOOF = "#42382E"
BRONZE = "#8C7A45"    # 低饱和青铜：束带、护腕、腰带
GOLD = "#C9A13B"      # 金属件：带扣、铜章、斧
BLADE = "#D8B24A"
HAFT = "#5A3A22"
CLOTH_RED = "#A95135" # 赤陶红前片
CLOTH_DARK = "#5A3B26"  # 深色裹裙
STONE = "#8C8478"

# 骨骼：名称、父级、卡片坐标枢轴。与 T7 v1 相同，只有尾尖枢轴下移（尾巴加长）。
BONES = [
    ("root", None, [0, 0, 0]),
    ("pelvis", "root", [0, 18, 0]),
    ("torso", "pelvis", [0, 22, 0]),
    ("neck", "torso", [0, 29, 0]),
    ("head", "neck", [0, 32, 1]),
    ("jaw", "head", [0, 32.5, 5.5]),
    ("tail_root", "pelvis", [0, 18, -4]),
    ("tail_tip", "tail_root", [0, 12, -4.8]),
    ("loincloth", "pelvis", [0, 20, 4.5]),
    ("upper_arm_r", "torso", [-10, 29, 0]),
    ("forearm_r", "upper_arm_r", [-10, 21, 0]),
    ("hand_r", "forearm_r", [-10, 14, 0]),
    ("thigh_r", "pelvis", [-4.5, 18, 0]),
    ("shin_r", "thigh_r", [-4.5, 10, 0]),
    ("hoof_r", "shin_r", [-4.5, 3, 0]),
    ("upper_arm_l", "torso", [10, 29, 0]),
    ("forearm_l", "upper_arm_l", [10, 21, 0]),
    ("hand_l", "forearm_l", [10, 14, 0]),
    ("thigh_l", "pelvis", [4.5, 18, 0]),
    ("shin_l", "thigh_l", [4.5, 10, 0]),
    ("hoof_l", "shin_l", [4.5, 3, 0]),
]
EXTRA_BONES = [
    {"id": "axe", "parent": "hand_l", "pivot_model_units": [10, 12, 1.5]},
    {"id": "stone_r", "parent": "hand_r", "pivot_model_units": [-10, 12, 1.5], "hidden_by_default": True},
]

ATTACHMENTS = [
    ("axe_grip_l", "hand_l", [10, 12, 1.5]),
    ("stone_grip_r", "hand_r", [-10, 12, 1.5]),
    ("snort", "head", [0, 34.2, 11.5]),
    ("horn_contact_visual", "head", [0, 38.5, 7.5]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None, kind="cuboid", normal=None):
    p = {"id": pid, "bone": bone, "center_model_units": list(center), "size_model_units": list(size),
         "rotation_degrees_model_axes": list(rot), "color": color, "uv_group": group or pid,
         "uv_share": None, "surface_kind": kind}
    if normal:
        p["plane_normal_model_axes"] = list(normal)
    return p


def mirrored(pid_l, bone_l, center, size, color, rot=(0, 0, 0), group=None):
    """左侧部件 + 镜像的右侧部件，两侧共用 UV 组。"""
    g = group or pid_l.removesuffix("_l")
    left = part(pid_l, bone_l, center, size, color, rot, g)
    right = part(pid_l[:-2] + "_r" if pid_l.endswith("_l") else pid_l + "_r",
                 bone_l[:-2] + "_r" if bone_l.endswith("_l") else bone_l,
                 [-center[0], center[1], center[2]], size, color, (rot[0], -rot[1], -rot[2]), g)
    left["uv_share"] = right["uv_share"] = g
    return [left, right]


def rounded(pid, bone, center, size, color, bevel=0.6, rot=(0, 0, 0)):
    """倒角体块：一窄一宽两个方块交叉，竖边多一级台阶，截面接近八边形。
    主块（名称为 pid）前后全深、左右各收 bevel，正面贴图（如眼睛）画在主块上；
    侧块（pid_side）左右全宽、前后各收 bevel。"""
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color, rot),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color, rot)]


def mirrored_rounded(pid_l, bone_l, center, size, color, bevel=0.6):
    base = pid_l.removesuffix("_l")
    w, h, d = size
    return (mirrored(pid_l, bone_l, center, [w - 2 * bevel, h, d], color, group=base)
            + mirrored(base + "_side_l", bone_l, center, [w, h, d - 2 * bevel], color, group=base + "_side"))


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 骨盆：皮毛、深色裹裙、腰带与带扣、后片 ──
ext(rounded("pelvis", "pelvis", [0, 19.5, 0], [13, 5, 8], FUR))
add(part("kilt", "pelvis", [0, 17, 0], [13.6, 4, 8.6], CLOTH_DARK))
add(part("belt", "pelvis", [0, 21, 0], [14, 2, 9], BRONZE))
add(part("belt_buckle", "pelvis", [0, 21, 4.8], [3, 2.6, 0.6], GOLD))
add(part("loincloth", "loincloth", [0, 15, 4.65], [6, 10, 0], CLOTH_RED, kind="single_plane", normal=[0, 0, 1]))
add(part("loincloth_back", "pelvis", [0, 16, -4.65], [5, 8, 0], CLOTH_RED, kind="single_plane", normal=[0, 0, -1]))

# ── 躯干：腹、胸、胸肌、斜方肌；X 形束带与前后铜章 ──
ext(rounded("abdomen", "torso", [0, 24, 0.3], [13, 4, 8], FUR, 0.8))
ext(rounded("chest", "torso", [0, 28, 0.5], [17, 6, 10], FUR, 1.0))
add(part("pecs", "torso", [0, 27.75, 6], [13, 3.5, 1], FUR))
add(part("trapezius", "torso", [0, 31.5, -0.5], [11, 2, 7], FUR))
add(part("strap_front_a", "torso", [0, 27.5, 6.75], [1.6, 11, 0.5], BRONZE, (0, 0, 38), "strap"))
add(part("strap_front_b", "torso", [0, 27.5, 6.75], [1.6, 11, 0.5], BRONZE, (0, 0, -38), "strap"))
add(part("strap_back_a", "torso", [0, 27.5, -4.75], [1.6, 11, 0.5], BRONZE, (0, 0, 38), "strap"))
add(part("strap_back_b", "torso", [0, 27.5, -4.75], [1.6, 11, 0.5], BRONZE, (0, 0, -38), "strap"))
add(part("medallion_front", "torso", [0, 27, 7.2], [4, 4, 0.6], GOLD, group="medallion"))
add(part("medallion_back", "torso", [0, 27, -5.1], [4, 4, 0.6], GOLD, group="medallion"))

# ── 颈与鬃毛：鬃毛分三层，从后脑垂到上背 ──
add(part("neck", "neck", [0, 31.25, 1], [8, 4.5, 7], FUR))
add(part("mane_upper", "neck", [0, 36, -2.9], [10.4, 8.4, 2.8], MANE))
add(part("mane_lower", "neck", [0, 29.6, -4.9], [8, 5.2, 1.6], MANE))
# 发梢：三缕长短不一，下缘参差，读作毛发而不是一块板
add(part("mane_strand_c", "neck", [0, 25.8, -5.0], [2.6, 3.4, 1.2], MANE, group="mane_strand"))
add(part("mane_strand_l", "neck", [2.6, 26.6, -4.95], [2.2, 2.4, 1.1], MANE, group="mane_strand_side"))
add(part("mane_strand_r", "neck", [-2.4, 26.2, -4.95], [2.2, 2.4, 1.1], MANE, group="mane_strand_side"))

# ── 头：颅、眉骨、鼻吻、下颌、耳、顶毛与额前卷毛、三段牛角 ──
ext(rounded("cranium", "head", [0, 36, 2], [10, 8, 8], FUR, 0.7))
add(part("brow", "head", [0, 39, 6.5], [10, 1.6, 1.5], FUR))
ext(rounded("muzzle", "head", [0, 33.75, 8], [8, 3, 4], IVORY, 0.6))
add(part("nose", "head", [0, 33.9, 10.5], [7, 2.6, 1], IVORY))
add(part("jaw", "jaw", [0, 31.75, 7.75], [7, 1.5, 4.5], IVORY))
add(part("mane_crown", "head", [0, 40.5, 1.5], [8, 1.5, 6], MANE))
ext(mirrored("ruff_l", "neck", [4.6, 31.5, 0], [1.8, 4.5, 5], MANE))
ext(mirrored("jowl_l", "head", [5.2, 33.6, 3.6], [1.2, 3, 4], FUR))
add(part("forelock", "head", [0, 39.9, 5.6], [4, 2, 2], MANE))
ext(mirrored("ear_l", "head", [6.6, 36.3, 1.5], [3.8, 2, 3], FUR, (0, 0, -22)))
# 角：根部向外上方，中段转向上，角尖略向内收；角尖为深色
ext(mirrored("horn_base_l", "head", [6.38, 39.68, 1.5], [3.2, 4, 3.2], IVORY, (0, 0, -70)))
ext(mirrored("horn_mid_l", "head", [8.96, 41.35, 1.5], [2.6, 3, 2.6], IVORY, (0, 0, -40)))
ext(mirrored("horn_tip_l", "head", [9.71, 43.1, 1.5], [2, 1.6, 2], HORN_TIP, (0, 0, 5)))

# ── 手臂：三角肌、上臂、前臂、宽护腕、拳与拇指 ──
ext(mirrored_rounded("deltoid_l", "upper_arm_l", [10.4, 28.3, 0], [6.8, 5, 7.2], FUR, 0.8))
ext(mirrored_rounded("upper_arm_l", "upper_arm_l", [10.2, 24, 0], [6, 7, 6], FUR))
ext(mirrored_rounded("forearm_l", "forearm_l", [10.1, 17.5, 0.2], [5.6, 7, 6], FUR))
ext(mirrored("bracer_l", "forearm_l", [10.1, 16, 0.2], [6.6, 3.5, 7], BRONZE))
# 左手握斧、右手空拳，贴图不同，不共用 UV
add(part("hand_l", "hand_l", [10.1, 12, 0.3], [6, 4.6, 6], FUR, group="hand_grip"))
add(part("hand_r", "hand_r", [-10.1, 12, 0.3], [6, 4.6, 6], FUR, group="hand_fist"))
ext(mirrored("thumb_l", "hand_l", [7.6, 12.6, 2.2], [1.4, 2.4, 2], FUR))

# ── 腿：粗大腿、小腿、两瓣蹄与后悬蹄 ──
ext(mirrored_rounded("thigh_l", "thigh_l", [4.7, 13.5, 0.3], [7, 9, 7.5], FUR, 0.8))
ext(mirrored_rounded("shin_l", "shin_l", [4.6, 6.5, 0], [5, 7, 5.5], FUR))
ext(mirrored("calf_l", "shin_l", [4.6, 7.6, -3.1], [4, 4, 1.2], FUR))
ext(mirrored("fetlock_l", "shin_l", [4.6, 3.7, 0.2], [5.8, 1.6, 6.3], MANE))
ext(mirrored("hoof_outer_l", "hoof_l", [6.1, 1.5, 1.2], [2.8, 3, 6.5], HOOF, group="hoof_toe"))
ext(mirrored("hoof_inner_l", "hoof_l", [3.1, 1.5, 1.2], [2.8, 3, 6.5], HOOF, group="hoof_toe"))
ext(mirrored("dewclaw_l", "hoof_l", [4.6, 2.5, -2.6], [3, 2, 1.5], HOOF))

# ── 尾：加长到膝下，末端深色毛簇 ──
add(part("tail_base", "tail_root", [0, 15, -4.8], [1.6, 6, 1.6], FUR))
add(part("tail_mid", "tail_tip", [0, 10, -4.8], [1.3, 4, 1.3], FUR))
add(part("tail_tuft", "tail_tip", [0, 7, -4.8], [2.6, 4, 2.6], MANE))

# ── 斧（零姿势：斧柄沿 +Z 穿过左手，握点到顶端 20u、到底端 14u；双刃沿 ±Y 展开）──
G = 1.5  # 握点 z
EXTRA = [
    part("axe_haft", "axe", [10, 12, G + 3], [1.5, 1.5, 34], HAFT),
    part("axe_pommel", "axe", [10, 12, G - 14], [2.2, 2.2, 2], GOLD, group="axe_metal"),
    part("axe_band_low", "axe", [10, 12, G - 8], [2, 2, 1], GOLD, group="axe_band"),
    part("axe_band_mid", "axe", [10, 12, G + 6], [2, 2, 1], GOLD, group="axe_band"),
    part("axe_socket", "axe", [10, 12, G + 15.5], [2.6, 2.6, 7], GOLD, group="axe_metal"),
    part("axe_cap", "axe", [10, 12, G + 20], [1.8, 1.8, 1.6], GOLD, group="axe_metal"),
]
# 月牙刃：每侧一片刃身加一条更长的外缘，外缘两端外扩成弧形刃口；总宽 13.6u
for side, sign in (("up", 1), ("down", -1)):
    EXTRA += [
        part(f"axe_blade_{side}", "axe", [10, 12 + sign * 3.55, G + 15.5], [0.8, 4.5, 8], BLADE, group="axe_blade"),
        part(f"axe_blade_{side}_edge", "axe", [10, 12 + sign * 6.2, G + 15.5], [0.8, 1.2, 11], BLADE, group="axe_blade_edge"),
    ]
EXTRA.append(part("stone", "stone_r", [-10, 12, G + 1], [3.5, 3, 3.5], STONE))


def main():
    d = json.loads(OUT.read_text(encoding="utf-8"))
    pivots = {b[0]: b[2] for b in BONES}
    d["bones"] = []
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["extra_bones"] = EXTRA_BONES
    d["parts"] = PARTS
    d["extra_parts"] = EXTRA
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    # 画在贴图上的五官（不是几何）：眼在眉骨下的颅前面，鼻孔在鼻吻前面，嘴线在鼻吻与下颌之间
    # v2.2：96 个方块按 2 像素/单位需要约 5.7 万像素，256×256 放不下；图集改 512×512，像素密度不变
    d["atlas"] = 512
    d["paint_features"] = [
        {"id": "eye_l", "on": "cranium.front", "center_model_units": [2.7, 37.25, 6.01], "size_model_units": [2, 1.5, 0], "color": "#EEE6D3"},
        {"id": "pupil_l", "on": "cranium.front", "center_model_units": [2.5, 37.3, 6.02], "size_model_units": [0.8, 1, 0], "color": "#2C221C"},
        {"id": "eye_r", "on": "cranium.front", "center_model_units": [-2.7, 37.25, 6.01], "size_model_units": [2, 1.5, 0], "color": "#EEE6D3"},
        {"id": "pupil_r", "on": "cranium.front", "center_model_units": [-2.5, 37.3, 6.02], "size_model_units": [0.8, 1, 0], "color": "#2C221C"},
        {"id": "nostril_l", "on": "nose.front", "center_model_units": [2, 34.0, 11.01], "size_model_units": [1.2, 1, 0], "color": "#34251D"},
        {"id": "nostril_r", "on": "nose.front", "center_model_units": [-2, 34.0, 11.01], "size_model_units": [1.2, 1, 0], "color": "#34251D"},
        {"id": "mouth_seam", "on": "jaw.front", "center_model_units": [0, 32.4, 10.01], "size_model_units": [6, 0.4, 0], "color": "#4C3426"},
    ]
    d["revisions"] = [r for r in d.get("revisions", []) if not r.startswith("T7-R5")]
    d["revisions"].append("T7-R5（2026-10-14 白模 v2，用户要求精修）：部件表改由 tools/blockbench/params/minotaur_parts.py 生成；"
                          "增加眉骨、顶毛、三段牛角、三角肌、胸肌、斜方肌、X 形束带与前后铜章、宽护腕、拳与拇指、"
                          "深色裹裙、带扣、后片、悬蹄、加长尾与毛簇、月牙形三层斧刃与金箍。骨骼与挂点名称不变；尾尖枢轴下移，头部枢轴上移 1u（头不再压在胸前），牛角按 44u 上限重算。"
                          "v2.1（同日，用户「继续优化」）：手臂加粗，前臂、拳更大；大腿加粗，加小腿肚与蹄上毛；耳加厚下垂；颈两侧毛领、颊侧下颌肉；后鬃改为后脑一片 + 上背一片 + 三缕长短不一的发梢。"
                          "v2.2（同日，用户要求「更多边、更自然、红眼」）：躯干、头、鼻吻、四肢主体改为倒角体块（一窄一宽交叉）；五官与细节画在贴图上，眼睛为红色并有自发光遮罩；图集 256→512（密度仍为 2 像素/单位）；眉骨与眼上移 1u、鼻吻压低 0.5u，平视时红眼不被鼻吻挡住；眼改为无眼框的怒视眼型（用户：原来像戴眼镜）；斧柄只沿左拳握孔方向（卡片 Z）穿过，姿势由手腕与前臂摆出（用户：原来像插进手里），左右手贴图分开。")
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"部件 {len(PARTS)} + 附加 {len(EXTRA)} = {len(PARTS) + len(EXTRA)} 个方块")


if __name__ == "__main__":
    main()
