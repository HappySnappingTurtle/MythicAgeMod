"""希腊居民（T19 阶段 ①）通用身体部件表。

生成 art/blockbench/greek/resident/resident.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。
身体沿用陶工 VI-GR-POT-01 的骨架（与神庙祭司同一套比例）；服装分两组，靠骨骼显隐二选一（V3-希腊-居民 H3）：
  outfit_male_*   男装：及膝短希顿（chiton）、腰绳、深色短发与短须
  outfit_female_* 女装：及踝希顿 + 胸前折返（apoptygma）、披巾（himation）、盘发（krobylos）与发带
搬运附件 carry_*（篮、木料、石料、黏土）挂在躯干前方，只显示正在搬的那一个（V1 §4.3：手上的东西只是外观）。
服色在这里是第 0 号配色；其余配色由 resident_palettes.py 从贴图换色生成。
出处见 docs/重构/开发/T19/美术/居民_白模卡.md。
运行：python3 tools/blockbench/params/resident_parts.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/resident/resident.params.json"

SKIN_M = "#AE7656"
SKIN_F = "#C48F6D"
HAIR = "#3B2B20"
TUNIC = "#D6C7A6"        # 男装：本色羊毛
TUNIC_SHADE = "#BDAE8E"
CORD = "#6B4A2E"
DRESS = "#D2AC58"        # 女装：番红花黄长袍（krokotos）
DRESS_LIGHT = "#DDBB6C"  # 折返略浅
DRESS_SHADE = "#B48F42"
SHAWL = "#7C3A2F"        # 披巾：深红
FILLET = "#2F4F6E"       # 发带：靛蓝
LEATHER = "#5A3B26"
WICKER = "#A07A44"
RIM = "#8A6638"
WOOD = "#7A5A36"
WOOD_END = "#B89466"
MARBLE = "#E6E1D6"
CLAY = "#9C8B7C"
OLIVE = "#4E5A2C"
HAFT = "#8A6A44"         # 工具木柄
IRON = "#8E9196"         # 铁刃
BRONZE = "#B0813A"       # 青铜刃
TERRACOTTA = "#B5653A"   # 赤陶（水罐、酒罐）
GLAZE = "#2A1C14"        # 黑釉带
REED = "#C8B07A"         # 芦苇（双管笛）
CAKE = "#C9A066"
BARLEY = "#D8C27A"
RIBBON = "#A8322B"
RIBBON_W = "#E8E0CC"
MASK = "#E8DCC0"
NET = "#6B5636"
LEAD = "#7A7C80"
PITCH = "#3A2A1E"
FLAME = "#F2A33A"
FLAME_HI = "#FCE08A"

BONES = [
    ("root", None, [0, 0, 0]),
    ("pelvis", "root", [0, 14, 0]),
    ("torso", "pelvis", [0, 16, 0]),
    ("head", "torso", [0, 22, 0]),
    ("outfit_male_hair", "head", [0, 22, 0]),
    ("outfit_female_hair", "head", [0, 22, 0]),
    ("outfit_male_top", "torso", [0, 16, 0]),
    ("outfit_female_top", "torso", [0, 16, 0]),
    ("carry_basket", "torso", [0, 15, 5]),
    ("carry_wood", "torso", [0, 15, 5]),
    ("carry_stone", "torso", [0, 15, 5]),
    ("carry_clay", "torso", [0, 15, 5]),
    # 生活与节庆（T19 阶段 ⑤b）：酒罐、糕饼盘抱在胸前；水罐、祭篮顶在头上；双管笛含在嘴上
    ("carry_wine_jar", "torso", [0, 15, 5]),
    ("carry_cakes", "torso", [0, 15, 5]),
    ("carry_hydria", "head", [0, 30, 0]),
    ("carry_kanoun", "head", [0, 30, 0]),
    ("carry_aulos", "head", [0, 24, 4]),
    ("tool_mask", "head", [0, 26, 4]),
    ("tool_hydria", "root", [0, 0, 7]),
    ("outfit_male_skirt", "pelvis", [0, 14, 0]),
    ("outfit_female_skirt", "pelvis", [0, 14, 0]),
    ("hem_front", "pelvis", [0, 10, 0]),
    ("hem_back", "pelvis", [0, 10, 0]),
    ("outfit_male_hem_front", "hem_front", [0, 10, 0]),
    ("outfit_female_hem_front", "hem_front", [0, 10, 0]),
    ("outfit_male_hem_back", "hem_back", [0, 10, 0]),
    ("outfit_female_hem_back", "hem_back", [0, 10, 0]),
    ("upper_arm_r", "torso", [-5.5, 21.5, 0]),
    ("outfit_male_sleeve_r", "upper_arm_r", [-5.5, 21.5, 0]),
    ("outfit_female_sleeve_r", "upper_arm_r", [-5.5, 21.5, 0]),
    ("forearm_r", "upper_arm_r", [-5.5, 15.5, 0]),
    ("hand_r", "forearm_r", [-5.5, 11.5, 0]),
    ("thigh_r", "pelvis", [-2, 14, 0]),
    ("shin_r", "thigh_r", [-2, 8, 0]),
    ("foot_r", "shin_r", [-2, 2, 0]),
    ("upper_arm_l", "torso", [5.5, 21.5, 0]),
    ("outfit_male_sleeve_l", "upper_arm_l", [5.5, 21.5, 0]),
    ("outfit_female_sleeve_l", "upper_arm_l", [5.5, 21.5, 0]),
    ("forearm_l", "upper_arm_l", [5.5, 15.5, 0]),
    ("hand_l", "forearm_l", [5.5, 11.5, 0]),
    # 工具（T19 阶段 ②b）：柄沿拳头握孔（+Z，前方）穿过手心；只按动作名显示（ResidentGeoModel）
    ("tool_pole", "hand_r", [-5.5, 10.5, 0]),
    ("tool_hook", "hand_r", [-5.5, 10.5, 0]),
    ("tool_shears", "hand_r", [-5.5, 10.5, 0]),
    ("tool_sickle", "hand_r", [-5.5, 10.5, 0]),
    ("tool_fork", "hand_r", [-5.5, 10.5, 0]),
    ("tool_spade", "hand_r", [-5.5, 10.5, 0]),
    ("tool_mallet", "hand_r", [-5.5, 10.5, 0]),
    ("tool_hammer", "hand_r", [-5.5, 10.5, 0]),
    ("tool_axe", "hand_r", [-5.5, 10.5, 0]),
    ("tool_saw", "hand_r", [-5.5, 10.5, 0]),
    ("carry_torch", "hand_r", [-5.5, 10.5, 0]),
    ("tool_cup", "hand_r", [-5.5, 10.5, 0]),
    ("tool_bread", "hand_r", [-5.5, 10.5, 0]),
    ("tool_net", "hand_r", [-5.5, 10.5, 0]),
    ("tool_wedge", "hand_l", [5.5, 10.5, 0]),
    ("tool_chisel", "hand_l", [5.5, 10.5, 0]),
    ("carry_ribbon", "hand_l", [5.5, 10.5, 0]),
    ("thigh_l", "pelvis", [2, 14, 0]),
    ("shin_l", "thigh_l", [2, 8, 0]),
    ("foot_l", "shin_l", [2, 2, 0]),
]

ATTACHMENTS = [
    ("hand_r_grip", "hand_r", [-5.5, 10.0, 0]),
    ("hand_l_grip", "hand_l", [5.5, 10.0, 0]),
    ("carry_center", "torso", [0, 15, 5]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None, kind="cuboid", normal=None):
    p = {"id": pid, "bone": bone, "center_model_units": list(center), "size_model_units": list(size),
         "rotation_degrees_model_axes": list(rot), "color": color, "uv_group": group or pid,
         "uv_share": None, "surface_kind": kind}
    if normal:
        p["plane_normal_model_axes"] = list(normal)
    return p


def side_bone(bone_l):
    if bone_l.endswith("_l"):
        return bone_l[:-2] + "_r"
    return bone_l


def mirrored(pid_l, bone_l, center, size, color, rot=(0, 0, 0), group=None, kind="cuboid", normal=None):
    """左侧部件 + 镜像的右侧部件，两侧共用 UV 组。"""
    g = group or pid_l.removesuffix("_l")
    left = part(pid_l, bone_l, center, size, color, rot, g, kind, normal)
    right = part(pid_l[:-2] + "_r", side_bone(bone_l), [-center[0], center[1], center[2]], size, color,
                 (rot[0], -rot[1], -rot[2]), g, kind, normal)
    left["uv_share"] = right["uv_share"] = g
    return [left, right]


def rounded(pid, bone, center, size, color, bevel=0.5):
    """倒角体块：一窄一宽两个方块交叉。主块前后全深，侧块左右全宽。"""
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color)]


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 头（共用）：方头、方鼻、耳 ──
add(part("head", "head", [0, 26, 0], [8, 8, 8], SKIN_M))
add(part("nose", "head", [0, 24.75, 5], [2, 3.5, 2], SKIN_M))
ext(mirrored("ear_l", "head", [4.2, 25.6, 0.0], [0.6, 1.8, 1.4], SKIN_M))

# ── 男：深色卷曲短发、短须（雅典成年男子多蓄须）──
MH = "outfit_male_hair"
add(part("m_hair_cap", MH, [0, 30.25, 0], [8.6, 1.5, 8.6], HAIR))
add(part("m_hair_back", MH, [0, 27.6, -4.3], [8.6, 4.3, 0.6], HAIR))
ext(mirrored("m_hair_side_l", MH, [4.3, 28.25, -1.2], [0.6, 2.5, 5.6], HAIR))
add(part("m_hair_fringe", MH, [0, 29.3, 4.25], [8.2, 0.9, 0.5], HAIR))
add(part("m_beard_front", MH, [0, 22.85, 4.3], [6.0, 2.1, 0.6], HAIR))
add(part("m_beard_chin", MH, [0, 21.6, 3.9], [3.6, 1.0, 1.4], HAIR))
ext(mirrored("m_beard_side_l", MH, [4.15, 23.4, 1.8], [0.6, 2.6, 4.2], HAIR))

# ── 女：中分长发收到脑后盘成发髻，额上一道发带 ──
FH = "outfit_female_hair"
add(part("f_hair_cap", FH, [0, 30.25, 0], [8.6, 1.5, 8.6], HAIR))
add(part("f_hair_back", FH, [0, 26.75, -4.3], [8.6, 6.0, 0.6], HAIR))
ext(mirrored("f_hair_side_l", FH, [4.3, 27.75, -0.6], [0.6, 3.5, 6.8], HAIR))
add(part("f_hair_fringe", FH, [0, 29.2, 4.25], [8.2, 1.1, 0.5], HAIR))
add(part("f_bun", FH, [0, 29.3, -5.6], [3.8, 3.4, 2.6], HAIR))
add(part("f_bun_top", FH, [0, 31.0, -4.8], [2.6, 1.2, 2.2], HAIR))
add(part("f_fillet", FH, [0, 30.0, 0], [9.1, 0.6, 9.1], FILLET))

# ── 躯干（共用）：颈 ──
add(part("neck", "torso", [0, 22, 0], [2, 2, 2], SKIN_M))

# ── 男上身：短希顿（倒角）、领口、斜褶 ──
MT = "outfit_male_top"
ext(rounded("m_chiton", MT, [0, 18, 0], [8, 9, 4], TUNIC, 0.4))
add(part("m_overfold", MT, [0, 19.9, 0], [8.4, 2.6, 4.4], TUNIC))
ext(mirrored("m_shoulder_l", MT, [3.4, 21.75, 0], [2.4, 0.6, 4.2], TUNIC_SHADE, group="m_shoulder"))

# ── 女上身：希顿 + 折返、披巾（背后一片、搭肩、前襟两条下垂）──
FT = "outfit_female_top"
ext(rounded("f_chiton", FT, [0, 18, 0], [8, 9, 4], DRESS, 0.4))
add(part("f_overfold", FT, [0, 19.2, 0], [8.6, 4.4, 4.6], DRESS_LIGHT))
add(part("f_shawl_back", FT, [0, 18.4, -2.6], [8.8, 7.6, 0.5], SHAWL))
ext(mirrored("f_shawl_shoulder_l", FT, [3.5, 21.8, -0.5], [3.0, 0.8, 4.6], SHAWL))
# 披巾从左肩斜披到右腰（前后各一条），不是两条直垂的背带
add(part("f_shawl_drape_front", FT, [0.4, 18.2, 2.55], [2.8, 11.0, 0.5], SHAWL, (0, 0, -36), group="f_shawl_drape"))
add(part("f_shawl_drape_back", FT, [0.4, 18.2, -2.85], [2.8, 11.0, 0.5], SHAWL, (0, 0, -36), group="f_shawl_drape"))
add(part("f_girdle", FT, [0, 16.75, 0], [8.4, 0.5, 4.4], CORD))

# ── 男：腰绳（打结）、短裙上段（倒角）──
MS = "outfit_male_skirt"
add(part("m_girdle", MS, [0, 15.2, 0], [8.4, 0.8, 4.4], CORD))
add(part("m_knot", MS, [0, 15.0, 2.45], [1.2, 1.2, 0.5], CORD))
ext(rounded("m_skirt", MS, [0, 12.75, 0], [8.6, 4.5, 4.6], TUNIC, 0.4))

# ── 女：长裙上段（倒角）──
FS = "outfit_female_skirt"
ext(rounded("f_skirt", FS, [0, 12.25, 0], [8.6, 5.5, 4.6], DRESS, 0.4))

# ── 下摆：男到膝上（y 8–10.5），女到踝（y 2.5–10）；前后两片可摆动，带竖褶 ──
for side, sz in (("front", 1), ("back", -1)):
    add(part(f"m_hem_{side}", f"outfit_male_hem_{side}", [0, 9.25, 1.2 * sz], [8.8, 2.5, 2.4], TUNIC, group="m_hem"))
    add(part(f"f_hem_{side}", f"outfit_female_hem_{side}", [0, 6.25, 1.2 * sz], [8.8, 7.5, 2.4], DRESS, group="f_hem"))
for i, x in enumerate((-2.6, 0, 2.6)):
    add(part(f"m_pleat_{i}", "outfit_male_hem_front", [x, 9.25, 2.55], [0.7, 2.2, 0.3], TUNIC_SHADE, group="m_pleat"))
    add(part(f"f_pleat_{i}", "outfit_female_hem_front", [x, 6.5, 2.55], [0.7, 6.6, 0.3], DRESS_SHADE, group="f_pleat"))
for i, x in enumerate((-1.8, 1.8)):
    add(part(f"f_pleat_back_{i}", "outfit_female_hem_back", [x, 6.5, -2.55], [0.7, 6.6, 0.3], DRESS_SHADE,
             group="f_pleat"))

# ── 腿脚（共用）：小腿、膝、踝带、脚、鞋底、趾带 ──
ext(mirrored("knee_l", "thigh_l", [2, 9, 0], [3.0, 2, 3.0], SKIN_M))
ext(mirrored("shin_l", "shin_l", [2, 5, 0], [2.8, 6, 2.8], SKIN_M))
ext(mirrored("ankle_strap_l", "shin_l", [2, 2.6, 0], [3.1, 0.6, 3.1], LEATHER))
ext(mirrored("foot_l", "foot_l", [2, 1.25, 0.8], [3, 1.5, 4.4], SKIN_M))
ext(mirrored("sole_l", "foot_l", [2, 0.3, 0.9], [3.4, 0.6, 5.0], LEATHER))
ext(mirrored("toe_strap_l", "foot_l", [2, 2.05, 1.6], [3.2, 0.4, 0.8], LEATHER))

# ── 手臂（共用）：上臂、前臂、手与拇指；袖子分男女 ──
ext(mirrored("upper_arm_l", "upper_arm_l", [5.5, 16.5, 0], [3, 3, 3], SKIN_M))
ext(mirrored("m_sleeve_l", "outfit_male_sleeve_l", [5.6, 19.6, 0], [3.6, 4.2, 4.4], TUNIC, group="m_sleeve"))
ext(mirrored("f_sleeve_l", "outfit_female_sleeve_l", [5.6, 18.9, 0], [3.6, 5.6, 4.4], DRESS, group="f_sleeve"))
ext(mirrored("forearm_l", "forearm_l", [5.5, 13.5, 0], [3, 4, 3], SKIN_M))
ext(mirrored("hand_l", "hand_l", [5.5, 10.5, 0], [3, 2, 3], SKIN_M, group="hand"))
add(part("thumb_l", "hand_l", [3.75, 10.9, 0.9], [0.6, 1.4, 1.0], SKIN_M, group="thumb"))
add(part("thumb_r", "hand_r", [-3.75, 10.9, 0.9], [0.6, 1.4, 1.0], SKIN_M, group="thumb"))

# ── 工具：右手柄心在 (-5.5, 10.5, 0)，左手在 (5.5, 10.5, 0)；柄沿 +Z ──
R = -5.5
L = 5.5
Y = 10.5
TOOLS = [
    # 打橄榄的长杆：握在后段，杆身向前伸 1.4 格
    part("pole_shaft", "tool_pole", [R, Y, 9], [0.9, 0.9, 26], HAFT),
    part("pole_tip", "tool_pole", [R, Y, 22.4], [1.1, 1.1, 0.8], "#5E4630"),
    # 修枝钩刀：短柄 + 前端向下弯的刃
    part("hook_handle", "tool_hook", [R, Y, 1.5], [1.0, 1.0, 4.0], HAFT),
    part("hook_blade", "tool_hook", [R, Y, 5.2], [0.4, 1.2, 3.4], IRON),
    part("hook_tip", "tool_hook", [R, Y - 1.1, 6.6], [0.4, 1.4, 0.8], IRON),
    # 剪羊毛的弹簧剪：两片刃 + 弓背
    part("shears_bow", "tool_shears", [R, Y, -0.6], [1.6, 0.6, 1.2], IRON),
    part("shears_blade_a", "tool_shears", [R - 0.4, Y, 2.8], [0.4, 0.8, 5.0], IRON, group="shears_blade"),
    part("shears_blade_b", "tool_shears", [R + 0.4, Y, 2.8], [0.4, 0.8, 5.0], IRON, group="shears_blade"),
    # 镰刀：短柄 + 向内弯的弧形刃（三段）
    part("sickle_handle", "tool_sickle", [R, Y, 1.5], [1.0, 1.0, 4.0], HAFT),
    part("sickle_blade_1", "tool_sickle", [R + 0.9, Y, 4.4], [2.2, 0.4, 0.8], BRONZE, group="sickle_blade"),
    part("sickle_blade_2", "tool_sickle", [R + 2.4, Y, 5.6], [0.8, 0.4, 2.4], BRONZE, group="sickle_blade"),
    part("sickle_blade_3", "tool_sickle", [R + 1.6, Y, 7.0], [1.6, 0.4, 0.8], BRONZE, group="sickle_blade"),
    # 扬场木叉：长柄 + 三根叉齿
    part("fork_shaft", "tool_fork", [R, Y, 7.0], [0.9, 0.9, 20], HAFT),
    part("fork_bar", "tool_fork", [R, Y, 17.2], [3.2, 0.7, 0.7], HAFT),
    part("fork_tine_l", "tool_fork", [R - 1.2, Y, 19.0], [0.5, 0.5, 3.2], HAFT, group="fork_tine"),
    part("fork_tine_m", "tool_fork", [R, Y, 19.0], [0.5, 0.5, 3.2], HAFT, group="fork_tine"),
    part("fork_tine_r", "tool_fork", [R + 1.2, Y, 19.0], [0.5, 0.5, 3.2], HAFT, group="fork_tine"),
    # 木铲（挖黏土）：柄 + 前端铲面
    part("spade_shaft", "tool_spade", [R, Y, 5.0], [0.9, 0.9, 13], HAFT),
    part("spade_blade", "tool_spade", [R, Y, 13.2], [3.0, 0.5, 3.6], "#9C7A4E"),
    # 木槌（打楔）：柄 + 横置的槌头
    part("mallet_handle", "tool_mallet", [R, Y, 2.5], [0.9, 0.9, 6.0], HAFT),
    part("mallet_head", "tool_mallet", [R, Y, 6.2], [2.2, 3.2, 2.4], "#6E5236"),
    # 锤：柄 + 铁锤头
    part("hammer_handle", "tool_hammer", [R, Y, 2.0], [0.8, 0.8, 5.0], HAFT),
    part("hammer_head", "tool_hammer", [R, Y, 4.9], [1.4, 2.6, 1.4], IRON),
    # 斧：柄 + 斧头（刃朝下）
    part("axe_handle", "tool_axe", [R, Y, 2.25], [0.9, 0.9, 14.5], HAFT),  # 柄尾留 5 格给左手
    part("axe_head", "tool_axe", [R, Y - 1.0, 9.2], [0.6, 3.4, 2.0], IRON),
    # 手锯：把手 + 锯条
    part("saw_grip", "tool_saw", [R, Y, 0.2], [1.0, 2.0, 2.0], HAFT),
    part("saw_blade", "tool_saw", [R, Y - 0.4, 6.4], [0.3, 2.4, 10.4], IRON),
    # 左手：铁楔（打楔时扶住）、凿子
    part("wedge", "tool_wedge", [L, Y, 1.6], [1.0, 1.4, 3.2], IRON),
    part("chisel", "tool_chisel", [L, Y, 1.8], [0.6, 0.6, 4.6], IRON),
]

# ── 搬运附件（躯干前方，双手从两侧托住；只显示正在搬的那个）──
EXTRA = [
    # 果篮：编织篮身、篮沿、篮里的橄榄
    part("basket_body", "carry_basket", [0, 14.4, 5.4], [6.0, 3.2, 4.0], WICKER),
    part("basket_rim", "carry_basket", [0, 16.15, 5.4], [6.4, 0.5, 4.4], RIM),
    part("basket_olives", "carry_basket", [0, 16.0, 5.4], [5.4, 0, 3.4], OLIVE, kind="single_plane", normal=[0, 1, 0]),
    # 木料：两根原木横担
    part("wood_log_low", "carry_wood", [0, 14.0, 5.2], [8.6, 2.2, 2.2], WOOD, group="wood_log"),
    part("wood_log_high", "carry_wood", [0.4, 16.1, 5.0], [8.6, 2.2, 2.2], WOOD, group="wood_log"),
    # 石料：一块方石
    part("stone_block", "carry_stone", [0, 14.8, 5.4], [5.0, 4.0, 4.2], MARBLE),
    # 黏土：一团湿黏土
    part("clay_lump", "carry_clay", [0, 14.6, 5.4], [4.4, 3.2, 3.8], CLAY),
    part("clay_lump_top", "carry_clay", [0.3, 16.5, 5.3], [3.0, 0.8, 2.8], CLAY),
] + TOOLS


def hydria(prefix, bone, x, y, z):
    """水罐（hydria）：圆腹、肩、窄颈、宽口沿，两侧横耳、背面竖耳（三耳）；腹部一圈黑釉带（黑绘水罐）。y 是罐底。"""
    return [
        part(f"{prefix}_body", bone, [x, y + 2.6, z], [5.0, 4.2, 5.0], TERRACOTTA, group="hydria_body"),
        part(f"{prefix}_body_side", bone, [x, y + 2.6, z], [5.6, 3.2, 4.4], TERRACOTTA, group="hydria_body_side"),
        part(f"{prefix}_band", bone, [x, y + 3.3, z], [5.7, 1.0, 5.1], GLAZE, group="hydria_band"),
        part(f"{prefix}_foot", bone, [x, y + 0.3, z], [3.2, 0.6, 3.2], TERRACOTTA, group="hydria_foot"),
        part(f"{prefix}_shoulder", bone, [x, y + 5.2, z], [3.8, 1.0, 3.8], TERRACOTTA, group="hydria_shoulder"),
        part(f"{prefix}_neck", bone, [x, y + 6.3, z], [1.8, 1.4, 1.8], TERRACOTTA, group="hydria_neck"),
        part(f"{prefix}_lip", bone, [x, y + 7.2, z], [3.0, 0.5, 3.0], GLAZE, group="hydria_lip"),
        part(f"{prefix}_handle_l", bone, [x + 3.0, y + 3.6, z], [0.6, 0.6, 1.8], TERRACOTTA, group="hydria_handle"),
        part(f"{prefix}_handle_r", bone, [x - 3.0, y + 3.6, z], [0.6, 0.6, 1.8], TERRACOTTA, group="hydria_handle"),
        part(f"{prefix}_handle_back", bone, [x, y + 5.6, z - 2.2], [0.6, 2.6, 0.6], TERRACOTTA, group="hydria_handle_back"),
    ]


# ── 生活与节庆（T19 阶段 ⑤b；出处见 T19 设计文档 §1.10、V3-希腊-居民 §13）──
LIFE = (
    # 头顶水罐：罐底垫一圈布垫（头上顶物时的垫圈）
    [part("hydria_pad", "carry_hydria", [0, 30.25, 0], [4.0, 0.5, 4.0], RIBBON_W)]
    + hydria("hydria", "carry_hydria", 0, 30.5, 0)
    # 在泉口接水时双手扶着的水罐（立在身前的泉台上，罐底离地半格；挂在根骨上，不随弯腰转）
    + hydria("fill_hydria", "tool_hydria", 0, 7.0, 6.5)
    + [
        # 祭篮（kanoun）：浅篮、三只立耳，篮里是大麦与一条彩带（持篮少女顶在头上领头）
        part("kanoun_base", "carry_kanoun", [0, 30.7, 0], [7.0, 1.4, 7.0], WICKER),
        part("kanoun_rim", "carry_kanoun", [0, 31.6, 0], [7.4, 0.5, 7.4], RIM),
        part("kanoun_barley", "carry_kanoun", [0, 31.5, 0], [6.4, 0, 6.4], BARLEY, kind="single_plane", normal=[0, 1, 0]),
        part("kanoun_ear_0", "carry_kanoun", [0, 33.2, 3.2], [1.2, 2.6, 0.5], WICKER, group="kanoun_ear"),
        part("kanoun_ear_1", "carry_kanoun", [2.8, 33.2, -1.6], [0.5, 2.6, 1.2], WICKER, group="kanoun_ear"),
        part("kanoun_ear_2", "carry_kanoun", [-2.8, 33.2, -1.6], [0.5, 2.6, 1.2], WICKER, group="kanoun_ear"),
        part("kanoun_fillet", "carry_kanoun", [1.2, 31.9, 0.4], [3.6, 0.4, 0.8], RIBBON),
        # 酒罐（抬着游行）：双耳尖底罐，横抱在胸前
        part("wine_jar_body", "carry_wine_jar", [0, 14.6, 5.6], [4.4, 5.4, 4.4], TERRACOTTA),
        part("wine_jar_toe", "carry_wine_jar", [0, 11.4, 5.6], [1.6, 1.2, 1.6], TERRACOTTA),
        part("wine_jar_neck", "carry_wine_jar", [0, 18.0, 5.6], [1.8, 1.6, 1.8], TERRACOTTA),
        part("wine_jar_lip", "carry_wine_jar", [0, 19.0, 5.6], [2.4, 0.5, 2.4], GLAZE),
        part("wine_jar_handle_l", "carry_wine_jar", [1.6, 17.6, 5.6], [0.6, 2.2, 0.6], TERRACOTTA, group="wine_jar_handle"),
        part("wine_jar_handle_r", "carry_wine_jar", [-1.6, 17.6, 5.6], [0.6, 2.2, 0.6], TERRACOTTA, group="wine_jar_handle"),
        # 糕饼盘：木盘上三块圆饼
        part("cakes_tray", "carry_cakes", [0, 15.0, 5.6], [6.4, 0.6, 4.2], WOOD),
        part("cake_0", "carry_cakes", [-1.8, 15.7, 5.6], [1.8, 0.8, 1.8], CAKE, group="cake"),
        part("cake_1", "carry_cakes", [0.2, 15.7, 5.2], [1.8, 0.8, 1.8], CAKE, group="cake"),
        part("cake_2", "carry_cakes", [2.0, 15.7, 6.0], [1.8, 0.8, 1.8], CAKE, group="cake"),
        # 双管笛（aulos）：两根芦苇管从嘴边向前下方斜伸、左右分开，嘴上系着口罩带（phorbeia）
        part("aulos_pipe_l", "carry_aulos", [1.2, 21.4, 7.4], [0.6, 0.6, 7.0], REED, rot=(-38, 12, 0), group="aulos_pipe"),
        part("aulos_pipe_r", "carry_aulos", [-1.2, 21.4, 7.4], [0.6, 0.6, 7.0], REED, rot=(-38, -12, 0), group="aulos_pipe"),
        part("aulos_band", "carry_aulos", [0, 24.0, 4.15], [6.0, 0.8, 0.3], LEATHER),
        # 火炬（火炬接力）：木杆、松脂裹头、火苗；杆沿握孔（+Z）
        part("torch_shaft", "carry_torch", [-5.5, 10.5, 3.5], [0.8, 0.8, 11.0], WOOD),
        part("torch_head", "carry_torch", [-5.5, 10.5, 9.6], [1.6, 1.6, 1.6], PITCH),
        part("torch_flame", "carry_torch", [-5.5, 10.5, 11.2], [1.4, 1.4, 1.8], FLAME),
        part("torch_flame_tip", "carry_torch", [-5.5, 10.5, 12.5], [0.8, 0.8, 1.0], FLAME_HI),
        # 墓前的丝带（tainiai）：左手里垂下两条，红白各一
        part("ribbon_red", "carry_ribbon", [5.2, 7.0, 0.6], [0.4, 6.0, 0.6], RIBBON, group="ribbon"),
        part("ribbon_white", "carry_ribbon", [5.9, 7.6, 0.2], [0.4, 5.0, 0.6], RIBBON_W, group="ribbon"),
        # 奠酒用的浅碗（phiale）：碗口沿握孔朝向（手平伸时朝上）
        part("cup_bowl", "tool_cup", [-5.5, 10.5, 1.6], [3.0, 3.0, 0.6], GLAZE),
        part("cup_boss", "tool_cup", [-5.5, 10.5, 2.0], [1.0, 1.0, 0.3], TERRACOTTA),
        # 橄榄薄饼（吃早饭）
        part("bread", "tool_bread", [-5.5, 10.5, 1.4], [2.8, 2.8, 0.6], CAKE),
        # 投网（amphiblestron）：收拢的网兜，下沿一圈铅坠
        part("net_bundle", "tool_net", [-5.5, 7.6, 1.2], [3.0, 5.0, 3.0], NET),
        part("net_weights", "tool_net", [-5.5, 5.0, 1.2], [3.8, 0.8, 3.8], LEAD),
        # 戏剧面具：像头盔一样套住整个头（古希腊的面具连着假发），脸面盖过鼻子和胡须；眼洞、张开的嘴
        part("mask_face", "tool_mask", [0, 25.8, 5.25], [8.8, 9.0, 2.3], MASK),
        part("mask_eye_l", "tool_mask", [1.8, 26.9, 6.45], [1.4, 0.8, 0.1], GLAZE, group="mask_eye"),
        part("mask_eye_r", "tool_mask", [-1.8, 26.9, 6.45], [1.4, 0.8, 0.1], GLAZE, group="mask_eye"),
        part("mask_brow", "tool_mask", [0, 28.0, 6.45], [5.6, 0.5, 0.1], GLAZE),
        part("mask_mouth", "tool_mask", [0, 23.4, 6.45], [2.8, 1.6, 0.1], GLAZE),
        part("mask_hair_top", "tool_mask", [0, 30.7, 0.6], [9.2, 1.6, 9.6], HAIR),
        part("mask_hair_back", "tool_mask", [0, 26.5, -4.4], [9.2, 7.4, 1.0], HAIR),
        part("mask_hair_l", "tool_mask", [4.55, 27.2, 0.4], [0.5, 5.6, 8.6], HAIR, group="mask_hair_side"),
        part("mask_hair_r", "tool_mask", [-4.55, 27.2, 0.4], [0.5, 5.6, 8.6], HAIR, group="mask_hair_side"),
    ]
)
EXTRA = EXTRA + LIFE


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "VI-GR-RES-01",
        "name": "希腊居民（通用身体）",
        "model": "resident",
        "height_limit_u": 31.6,
        "atlas": 512,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [0.6, 1.95, 0.6],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": EXTRA,
        "skin_female": SKIN_F,
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["T19-R4（阶段 ⑤b）：生活与节庆道具 12 组——头顶水罐与祭篮、接水的水罐、酒罐、糕饼盘、双管笛、火炬、丝带、奠酒浅碗、薄饼、投网、戏剧面具。", "T19-R3（阶段 ②b）：12 件手持工具，柄沿握孔（+Z）穿过手心。", "T19-R2（自查）：女装改番红花黄 + 深红披巾（原赭红与肤色太近，看着像没穿衣服）；披巾改为左肩斜披；发带移到刘海上方、改靛蓝；眉毛改细。", "T19-R1（2026-10-23 白模 v1）：沿用陶工 / 祭司骨架；男装及膝短希顿 + 腰绳 + 短发短须，"
                      "女装及踝希顿 + 折返 + 披巾 + 盘发发带；搬运附件 4 种。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"部件 {len(PARTS)} + 附加 {len(EXTRA)} = {len(PARTS) + len(EXTRA)} 个方块")


if __name__ == "__main__":
    main()
