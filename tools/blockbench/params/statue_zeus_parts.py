"""宙斯主神像 v2（T11b 阶段 ②，设计文档 §6.1；2026-10-16 用户：参考《神话时代》→ 王者立像）部件表。

生成 art/blockbench/greek/statue_zeus/statue_zeus.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。原点 = 核心方块底面中心；
总高 64u = 4 格（核心 + 三格部件方块，底座模板 3×5×3）。

形式（仿《神话时代：重制版》城镇中心的主神像）：正面站立的众神之王——
  左手外伸拄金色长权杖（杖立在神像左侧、落在底座上），右手把雷霆横举在肩旁（锯齿状闪电，金），
  台座前右角站一只鹰；背后披风从双肩垂到脚跟，金扣针别在肩前；
  英雄比例：宽肩、厚胸、粗臂，裸上身；腰系金带，希玛申裹到小腿中部，下摆蓝底金回纹；
  白色长须发、金色橄榄冠，头微低俯视信众；上臂与腕上金箍。
手臂、腿在「下垂」的静止坐标里建模，姿势全部由骨骼静止旋转给出；权杖、雷霆挂在手骨下，
沿拳头握孔穿过拳心（检查清单 §三）。握孔方向与落点用 statue_fk.py 算出并写在下面的注释里。
运行：python3 tools/blockbench/params/statue_zeus_parts.py
"""
import json
import math
from pathlib import Path

from statue_fk import Skeleton, fmt

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/statue_zeus/statue_zeus.params.json"

MARBLE = "#E7E2D7"
MARBLE_SHADE = "#D2CBBC"
HAIR = "#DAD3C4"
PLINTH = "#CFCAC0"
GOLD = "#C9A13B"
BRONZE = "#A8782E"
BORDER = "#3E5C8A"
DARK_GOLD = "#8A6420"

# 骨骼：id、父、枢轴、静止旋转（卡片轴，度；先 X 再 Y 再 Z）
# 右臂：上臂向外抬到略低于水平（rz −75，略向前 rx −10），前臂竖直向上（rz −105）并绕自身轴转 90°（ry 90），
#       握孔因此朝外水平 → 雷霆横在头侧；拳心约在 (−20.4, 55.2, 1.4)。
# 左臂（v2.2，用户：左手太夸张 → 收回）：上臂贴身略后收（rx 15、rz 4），前臂向前偏外、略下垂（rx −90、ry 25、rz −10），
#       手腕微屈（rx −15）使握孔竖直 → 权杖垂直落地；拳心约在 (16.0, 34.2, 6.9)，在底座左中格内（不碰前角的圣火盆）。
#       角度由网格搜索求出：拳心 x 14–18、z ≤ 7、握孔竖直、前臂略向下。
BONES = [
    ("root", None, [0, 0, 0], [0, 0, 0]),
    ("plinth", "root", [0, 0, 0], [0, 0, 0]),
    ("pelvis", "root", [0, 31, 0], [0, 0, 0]),
    ("torso", "pelvis", [0, 33, 0], [0, 0, 0]),
    ("head", "torso", [0, 49, 0.4], [6, 0, 0]),
    ("cape", "torso", [0, 47, -4.4], [0, 0, 0]),
    ("upper_arm_r", "torso", [-10, 46.5, 0], [-10, 0, -75]),
    ("forearm_r", "upper_arm_r", [-10.4, 36, 0], [0, 90, -105]),
    ("hand_r", "forearm_r", [-10.4, 27, 0.3], [0, 0, 0]),
    ("bolt", "hand_r", [-10.4, 25, 0.3], [0, 0, 0]),
    ("upper_arm_l", "torso", [10, 46.5, 0], [15, 0, 4]),
    ("forearm_l", "upper_arm_l", [10.4, 36, 0], [-90, 25, -10]),
    ("hand_l", "forearm_l", [10.4, 27, 0.3], [-15, 0, 0]),
    ("sceptre", "hand_l", [10.4, 25, 0.3], [0, 0, 0]),
    ("shin_r", "pelvis", [-3.2, 19, 0], [0, 0, 1]),
    ("foot_r", "shin_r", [-3.2, 6.6, 0], [0, 0, 0]),
    ("shin_l", "pelvis", [3.6, 19, 0], [-4, 0, -1]),
    ("foot_l", "shin_l", [3.6, 6.6, 0], [4, 0, 0]),
    ("eagle", "plinth", [-6.0, 4.5, 4.8], [0, -30, 0]),
]
SK = Skeleton(BONES)
FIST_R = SK.point("hand_r", [-10.4, 25, 0.3])
FIST_L = SK.point("hand_l", [10.4, 25, 0.3])
HOLE_L = SK.direction("hand_l", [0, 0, 1])

ATTACHMENTS = [
    ("bolt_tip", "bolt", [-10.4, 25, 0.3 + 8.4]),
    ("sceptre_top", "sceptre", [10.4, 25, 0.3 + 25.0]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None, kind="cuboid", normal=None):
    p = {"id": pid, "bone": bone, "center_model_units": [round(c, 3) for c in center],
         "size_model_units": [round(c, 3) for c in size], "rotation_degrees_model_axes": list(rot), "color": color,
         "uv_group": group or pid, "uv_share": None, "surface_kind": kind}
    if normal:
        p["plane_normal_model_axes"] = list(normal)
    return p


def mirrored(pid_l, bone_l, center, size, color, rot=(0, 0, 0), group=None):
    """左侧部件 + 镜像的右侧部件，两侧共用 UV 组。"""
    g = group or pid_l.removesuffix("_l")
    left = part(pid_l, bone_l, center, size, color, rot, g)
    rbone = bone_l[:-2] + "_r" if bone_l.endswith("_l") else bone_l
    right = part(pid_l[:-2] + "_r", rbone, [-center[0], center[1], center[2]], size, color,
                 (rot[0], -rot[1], -rot[2]), g)
    left["uv_share"] = right["uv_share"] = g
    return [left, right]


def rounded(pid, bone, center, size, color, bevel=0.5):
    """倒角体块：一窄一宽两个方块交叉（检查清单 §二.2）。"""
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color)]


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 台座：三级（底 16、中 14 刻回纹、顶 15 线脚），顶面 y = 4.5 ──
add(part("plinth_base", "plinth", [0, 1.0, 0], [16, 2.0, 16], PLINTH))
add(part("plinth_mid", "plinth", [0, 2.75, 0], [15, 1.5, 15], PLINTH))
add(part("plinth_top", "plinth", [0, 4.0, 0], [16, 1.0, 16], PLINTH))

# ── 腿：小腿（下摆以下可见）、小腿肚、脚、凉鞋底、金趾带与踝带 ──
for s, x in (("l", 3.6), ("r", -3.2)):
    add(part(f"shin_{s}", f"shin_{s}", [x, 12.6, 0.2], [4.8, 12.0, 5.0], MARBLE, group="shin"))
    add(part(f"calf_{s}", f"shin_{s}", [x, 14.2, -2.4], [3.8, 5.0, 1.2], MARBLE, group="calf"))
    add(part(f"ankle_strap_{s}", f"shin_{s}", [x, 7.6, 0.2], [5.2, 0.6, 5.4], GOLD, group="ankle_strap"))
    add(part(f"foot_{s}", f"foot_{s}", [x, 5.6, 1.2], [4.6, 2.2, 7.0], MARBLE, group="foot"))
    add(part(f"sole_{s}", f"foot_{s}", [x, 4.75, 1.3], [5.0, 0.5, 7.6], MARBLE_SHADE, group="sole"))
    add(part(f"toe_strap_{s}", f"foot_{s}", [x, 6.0, 3.4], [5.0, 0.5, 1.0], GOLD, group="toe_strap"))

# ── 下身：希玛申裹到小腿中部（倒角）、四道前褶（斜搭的布纹画在贴图上）、下摆蓝底金回纹、左胯垂角；金腰带与带扣 ──
ext(rounded("skirt", "pelvis", [0, 23.5, 0.3], [14, 19, 9.6], MARBLE_SHADE, 0.8))
for i, x in enumerate((-4.5, -1.5, 1.5, 4.5)):
    add(part(f"skirt_fold_{i}", "pelvis", [x, 23.0, 5.25], [1.0, 16.0, 0.5], MARBLE_SHADE, group="skirt_fold"))
add(part("skirt_hem", "pelvis", [0, 15.0, 0.3], [14.6, 2.0, 10.2], BORDER))
add(part("skirt_tail", "pelvis", [7.5, 22.0, 2.0], [1.2, 14.0, 3.6], MARBLE_SHADE, (0, 0, 3)))
add(part("belt", "pelvis", [0, 33.2, 0.3], [14.4, 1.6, 10.0], GOLD))
add(part("belt_buckle", "pelvis", [0, 33.2, 5.45], [2.6, 2.2, 0.6], GOLD))

# ── 上身：腹（倒角）与六块腹肌、腹外斜肌、胸（倒角）与胸肌、斜方肌、三角肌、颈；披风肩部与金扣针 ──
ext(rounded("abdomen", "torso", [0, 36.6, 0.3], [12, 5.6, 8.0], MARBLE, 0.7))
for row, y in enumerate((35.0, 36.8, 38.6)):
    ext(mirrored(f"abs{row}_l", "torso", [1.5, y, 4.55], [2.6, 1.5, 0.6], MARBLE, group="abs"))
ext(mirrored("oblique_l", "torso", [6.5, 38.6, 0.3], [1.4, 5.2, 6.6], MARBLE, group="oblique"))
ext(rounded("chest", "torso", [0, 43.0, 0.3], [16, 8, 9.6], MARBLE, 1.0))
ext(mirrored("pec_l", "torso", [3.6, 43.6, 5.1], [6.6, 4.2, 1.0], MARBLE, group="pec"))
add(part("trapezius", "torso", [0, 47.6, -0.8], [11, 1.8, 6.4], MARBLE))
ext(mirrored("deltoid_l", "torso", [9.6, 45.6, 0.3], [5.6, 5.4, 6.6], MARBLE, group="deltoid"))
add(part("neck", "torso", [0, 48.6, 0.6], [5.2, 3.6, 5.0], MARBLE))
add(part("cape_shoulders", "torso", [0, 47.3, -3.4], [17.4, 1.6, 4.2], MARBLE_SHADE))
ext(mirrored("fibula_l", "torso", [7.2, 47.4, 3.2], [1.8, 1.8, 0.8], GOLD, group="fibula"))

# ── 披风：上下两片（下片略向后张）、背面三道竖褶、底边蓝金饰带 ──
add(part("cape_upper", "cape", [0, 37.5, -5.3], [17.4, 19.0, 1.0], MARBLE_SHADE))
add(part("cape_lower", "cape", [0, 17.5, -6.1], [18.4, 21.0, 1.0], MARBLE_SHADE))
for i, x in enumerate((-5.0, 0.0, 5.0)):
    add(part(f"cape_fold_{i}", "cape", [x, 26.5, -6.85], [1.2, 37.0, 0.5], MARBLE_SHADE, group="cape_fold"))
add(part("cape_hem", "cape", [0, 7.6, -6.1], [18.6, 1.2, 1.2], BORDER))

# ── 头：方颅倒角、眉骨、直鼻、额前卷发、发顶、及肩长发、两侧鬓发与垂肩发绺；
#        粗眉（略皱）、颧骨（眼窝因此凹进去）、鼻梁 + 鼻头；三层收窄的长卷须 + 两腮、两端下垂的八字须、下唇；金色橄榄冠（两侧、后、每侧四叶；额前只有一道细冠带压在发际上，不放叶，免得像眉毛）──
ext(rounded("head", "head", [0, 54, 0.6], [9.6, 10, 9.6], MARBLE, 0.8))
add(part("brow", "head", [0, 55.4, 5.55], [8.4, 1.1, 0.7], MARBLE))
ext(mirrored("eyebrow_l", "head", [2.2, 55.55, 5.95], [3.0, 0.8, 0.5], HAIR, (0, 0, 6), group="eyebrow"))
ext(mirrored("cheek_l", "head", [2.75, 53.1, 5.6], [2.3, 1.2, 0.5], MARBLE, group="cheek"))
add(part("nose", "head", [0, 54.0, 5.75], [1.4, 2.2, 1.0], MARBLE))
add(part("nose_tip", "head", [0, 52.95, 6.15], [2.0, 1.1, 1.4], MARBLE))
add(part("hair_cap", "head", [0, 59.4, 0.3], [10.6, 1.8, 10.4], HAIR))
add(part("fringe", "head", [0, 58.2, 5.55], [9.8, 1.4, 0.8], HAIR))
add(part("hair_back", "head", [0, 53.0, -4.6], [10.6, 12.0, 1.2], HAIR))
ext(mirrored("hair_side_l", "head", [5.15, 55.0, -0.8], [0.9, 6.0, 8.0], HAIR))
ext(mirrored("hair_lock_l", "head", [4.4, 46.8, -3.4], [2.4, 5.0, 1.6], HAIR, group="hair_lock"))
add(part("beard_front", "head", [0, 50.2, 5.75], [9.0, 5.2, 1.6], HAIR))
add(part("beard_mid", "head", [0, 46.9, 5.6], [7.4, 2.2, 1.6], HAIR))
add(part("beard_lower", "head", [0, 45.0, 5.2], [4.6, 1.8, 1.6], HAIR))
ext(mirrored("beard_side_l", "head", [5.0, 51.4, 2.8], [0.9, 5.0, 5.6], HAIR))
add(part("moustache", "head", [0, 51.95, 6.85], [5.6, 0.9, 0.7], HAIR))
ext(mirrored("moustache_end_l", "head", [3.05, 51.2, 6.75], [0.9, 1.8, 0.6], HAIR, group="moustache_end"))
add(part("lower_lip", "head", [0, 50.95, 6.7], [2.2, 0.6, 0.4], MARBLE))
ext(mirrored("wreath_side_l", "head", [5.65, 57.5, 0.4], [0.8, 1.2, 9.6], GOLD))
add(part("wreath_back", "head", [0, 57.5, -4.9], [11.0, 1.2, 0.8], GOLD))
add(part("wreath_front", "head", [0, 59.05, 5.55], [9.8, 0.8, 0.7], GOLD))
for i, (z, dy, rx) in enumerate(((3.6, 0.5, -26), (1.0, -0.5, 26), (-1.6, 0.5, -26), (-3.8, -0.5, 26))):
    ext(mirrored(f"leaf{i}_l", "head", [5.9, 57.5 + dy * 0.6, z], [0.4, 0.9, 2.2], GOLD, (rx * 0.7, 0, 0), group="wreath_leaf"))

# ── 手臂（下垂的静止坐标）：上臂（倒角）、肩头、二头肌、金臂箍、前臂、金护腕；两手都握拳（拇指在内前侧）──
for s in ("l", "r"):
    x = 10.4 if s == "l" else -10.4
    inner = -1 if s == "l" else 1
    ext(rounded(f"upper_arm_{s}", f"upper_arm_{s}", [x, 41.2, 0.3], [5.4, 10.5, 5.8], MARBLE, 0.6))
    add(part(f"shoulder_cap_{s}", f"upper_arm_{s}", [x, 45.4, 0.3], [6.0, 3.0, 6.6], MARBLE, group="shoulder_cap"))
    add(part(f"biceps_{s}", f"upper_arm_{s}", [x, 41.0, 3.3], [3.8, 5.2, 1.0], MARBLE, group="biceps"))
    add(part(f"armband_{s}", f"upper_arm_{s}", [x, 38.2, 0.3], [5.9, 1.0, 6.3], GOLD, group="armband"))
    add(part(f"forearm_{s}", f"forearm_{s}", [x, 31.5, 0.3], [4.8, 9.0, 5.0], MARBLE, group="forearm"))
    add(part(f"bracer_{s}", f"forearm_{s}", [x, 28.4, 0.3], [5.2, 1.6, 5.4], GOLD, group="bracer"))
    add(part(f"fist_{s}", f"hand_{s}", [x, 25.0, 0.3], [4.4, 4.0, 5.0], MARBLE, group="fist"))
    add(part(f"thumb_{s}", f"hand_{s}", [x + inner * 2.4, 25.6, 1.9], [0.9, 2.0, 1.4], MARBLE, group="thumb"))

EXTRA = []

# ── 雷霆（金）：握柄穿过拳心（静止坐标 z 轴 = 握孔，摆好后朝外水平）；两端锯齿状闪电，外端长、内端短 ──
# 静止坐标里：z 沿雷霆长度（+z 朝外），y 摆好后朝下；锯齿在 z–y 平面里，绕 x 轴转。
BX, BY, BZ = -10.4, 25.0, 0.3
EXTRA += [
    part("bolt_grip", "bolt", [BX, BY, BZ], [1.8, 1.8, 7.0], GOLD),
    part("bolt_knot_out", "bolt", [BX, BY, BZ + 3.6], [2.6, 2.6, 1.2], GOLD, group="bolt_knot"),
    part("bolt_knot_in", "bolt", [BX, BY, BZ - 3.6], [2.6, 2.6, 1.2], GOLD, group="bolt_knot"),
]


def zigzag(prefix, points, thick, group):
    """折线（静止坐标 (z, y) 顶点）→ 首尾相接的方块段：中心在两点中间，长度 = 两点距离 + 厚度（接头处重叠），
    绕 x 轴转 θ = atan2(−dy, dz)（方块沿 z 的方向转到 (0, −sinθ, cosθ)）。"""
    out = []
    for i in range(len(points) - 1):
        (z0, y0), (z1, y1) = points[i], points[i + 1]
        dz, dy = z1 - z0, y1 - y0
        length = math.hypot(dz, dy) + thick * 0.8
        theta = math.degrees(math.atan2(-dy, dz))
        out.append(part(f"{prefix}_{i}", "bolt", [BX, BY + (y0 + y1) / 2, BZ + (z0 + z1) / 2], [thick, thick, length],
                        GOLD, (round(theta, 2), 0, 0), group=f"{group}_{i}"))
    return out


# 外端（朝外）三折、内端（朝头）两折，最外一段收细成尖
EXTRA += zigzag("bolt_out", [(4.2, 0.0), (6.2, -1.6), (8.0, 0.9), (9.8, -0.7)], 1.4, "bolt_out")
EXTRA += zigzag("bolt_in", [(-4.2, 0.0), (-5.8, 1.4), (-7.2, -0.4)], 1.2, "bolt_in")
EXTRA.append(part("bolt_tip_spike", "bolt", [BX, BY - 1.0, BZ + 10.9], [0.8, 0.8, 1.6], GOLD, (24, 0, 0)))

# ── 权杖：青铜杖身从底座（y = 0）到 y ≈ 58（顶尖约 63），金杖箍、杖脚、顶端金球与尖顶（静止坐标 z = 握孔，摆好后朝上）──
SX, SY, SZ = 10.4, 25.0, 0.3
down = FIST_L[1] / HOLE_L[1]  # 拳心到地面沿握孔的长度
top = 57.6 - FIST_L[1]
EXTRA += [
    part("sceptre_shaft", "sceptre", [SX, SY, SZ + (top - down) / 2], [1.4, 1.4, top + down], BRONZE),
    part("sceptre_foot", "sceptre", [SX, SY, SZ - down + 0.7], [2.0, 2.0, 1.4], GOLD),
    part("sceptre_band_low", "sceptre", [SX, SY, SZ - 3.4], [1.9, 1.9, 0.8], GOLD, group="sceptre_band"),
    part("sceptre_band_high", "sceptre", [SX, SY, SZ + 3.4], [1.9, 1.9, 0.8], GOLD, group="sceptre_band"),
    part("sceptre_collar", "sceptre", [SX, SY, SZ + top - 0.4], [2.6, 2.6, 1.0], GOLD),
    part("sceptre_orb", "sceptre", [SX, SY, SZ + top + 1.6], [3.2, 3.2, 3.2], GOLD),
    part("sceptre_spire", "sceptre", [SX, SY, SZ + top + 4.2], [1.0, 1.0, 2.0], GOLD),
]
for i, (dx, dy) in enumerate(((1, 0), (-1, 0), (0, 1), (0, -1))):
    EXTRA.append(part(f"sceptre_petal_{i}", "sceptre", [SX + dx * 1.7, SY + dy * 1.7, SZ + top + 0.6],
                      [0.8 if dx else 1.6, 0.8 if dy else 1.6, 1.8], GOLD, group="sceptre_petal"))

# ── 金鹰：站在台座前右角、身体朝右前方，约 10u 高；金色身子、青铜色收拢长翅与扇尾、暗金钩喙与爪；
#    直立的胸、羽毛「裤腿」、后颈羽、昂起的头（检查清单 §二.1：轮廓一眼认出是鹰，不是鸡）──
EX, EY, EZ = -6.0, 4.5, 4.8
EXTRA += [
    part("eagle_talons", "eagle", [EX, EY + 0.3, EZ + 0.7], [3.0, 0.6, 2.2], DARK_GOLD),
    part("eagle_legs", "eagle", [EX, EY + 1.6, EZ + 0.2], [2.6, 2.0, 1.8], BRONZE),
    part("eagle_belly", "eagle", [EX, EY + 4.0, EZ - 0.1], [3.4, 3.0, 3.4], GOLD),
    part("eagle_chest", "eagle", [EX, EY + 6.6, EZ + 0.4], [3.8, 3.2, 3.6], GOLD, (-14, 0, 0)),
    part("eagle_wing_l", "eagle", [EX + 2.1, EY + 5.2, EZ - 1.0], [0.8, 5.6, 4.6], BRONZE, (-16, 0, 0), group="eagle_wing"),
    part("eagle_wing_r", "eagle", [EX - 2.1, EY + 5.2, EZ - 1.0], [0.8, 5.6, 4.6], BRONZE, (-16, 0, 0), group="eagle_wing"),
    part("eagle_wing_tips", "eagle", [EX, EY + 2.6, EZ - 3.4], [3.0, 1.0, 2.4], BRONZE, (30, 0, 0)),
    part("eagle_tail", "eagle", [EX, EY + 1.4, EZ - 4.0], [3.0, 0.6, 3.0], BRONZE, (40, 0, 0)),
    part("eagle_neck", "eagle", [EX, EY + 8.6, EZ + 0.9], [2.6, 1.6, 2.6], GOLD),
    part("eagle_hackle", "eagle", [EX, EY + 8.4, EZ - 0.6], [2.8, 2.0, 0.8], GOLD),
    part("eagle_head", "eagle", [EX, EY + 10.1, EZ + 1.2], [2.6, 2.4, 3.0], GOLD),
    part("eagle_brow", "eagle", [EX, EY + 10.8, EZ + 2.2], [2.8, 0.6, 1.4], GOLD),
    part("eagle_beak", "eagle", [EX, EY + 9.9, EZ + 3.2], [1.2, 1.2, 1.4], DARK_GOLD),
    part("eagle_beak_hook", "eagle", [EX, EY + 9.3, EZ + 3.7], [0.8, 0.8, 0.6], DARK_GOLD),
]


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "ST-GR-ZEUS",
        "name": "宙斯主神像",
        "model": "statue_zeus",
        "height_limit_u": 64,
        "atlas": 512,
        "texels_per_unit": 2,
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": EXTRA,
        "fk": {"fist_r": FIST_R, "fist_l": FIST_L, "hole_l": HOLE_L},
        # 五官定位（像素行）：头前面顶边 y = 59，眉骨下沿 ≈ 54.85 → 眼睑从第 9 行起；胡须前片顶 52.8，嘴在第 3 行
        "face": {"eyeTop": 9, "mouthTop": 3, "v": 2},
    }
    for bid, parent, pivot, rot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot, "rest_rotation_degrees": rot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p} for a, b, p in ATTACHMENTS]
    d["revisions"] = [
        "v2.2（2026-10-17 用户：左手有点夸张、面部再精细）：左手收到腰侧前方握杖；面部加粗眉、颧骨、鼻头、三层胡须、下垂的八字须与下唇，眼睛画出眼白与虹膜。",
        "v2.1（自查）：额前加细冠带（只有两鬓的金叶时像耳罩）、瞳孔加深；雷霆锯齿首尾相接；去掉像木板的斜搭布与像眉毛的额前冠叶；鹰改为约 10u 高的金鹰（钩喙、收拢长翅、扇尾）；权杖顶降到 63u 内；台座顶加宽到整格。",
        "v2（2026-10-16 用户：「不太好，参考神话时代」→ 选王者立像）：正面站立，左手拄长权杖、右手把雷霆横举在肩旁，"
        "脚边一只鹰，背后披风及踵；英雄比例（肩宽 22u）；高 64u（4 格），底座模板改为 3×5×3。",
        "v1.1：阿耳忒弥西翁式投掷姿势（已废弃）。",
    ]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"部件 {len(PARTS)} + 附加 {len(EXTRA)} = {len(PARTS) + len(EXTRA)} 个方块")
    print("右拳", fmt(FIST_R), "左拳", fmt(FIST_L), "左握孔", fmt(HOLE_L), f"权杖 下 {down:.1f} 上 {top:.1f}")
    for bone, p in (("foot_l", [3.6, 4.5, 1.2]), ("foot_r", [-3.2, 4.5, 1.2])):
        print(bone, "鞋底中心", fmt(SK.point(bone, p)))


if __name__ == "__main__":
    main()
