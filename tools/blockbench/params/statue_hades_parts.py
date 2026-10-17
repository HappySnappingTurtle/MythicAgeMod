"""哈迪斯主神像（T11b 阶段 ②；王者立像，与宙斯、波塞冬同一形式）部件表。

生成 art/blockbench/greek/statue_hades/statue_hades.params.json。身体、头与五官来自 statue_common.py。
与另两尊区分（2026-10-17）：
  整尊是深灰蓝大理石（宙斯、波塞冬是白大理石），须发更深；直长发、更长的一股尖须；
  左手拄双叉戟（与宙斯拄权杖同一握法），右手垂下、掌心朝下按在三头犬刻耳柏洛斯中间那颗头上（经典构图）；
  三头犬坐在底座右侧格子里（深色青铜，红眼发光），戴尖刺项圈；
  深色披风带立领（宙斯的披风是浅色、无领）；暗铜冠，正中一个骨白色小骷髅饰；
  眼睛发冷蓝微光（发光贴图 _glowmask）。
手臂姿势由网格搜索求出：左拳握孔竖直、不进底座前角；右掌朝下、四指水平朝外，掌心在 (−16.9, 30.0, 6.2)
（手臂自然下垂、掌心朝下时掌心最低约 y 29.3，所以三头犬坐高约 29u，靠粗壮的身形与大头避免「像柱子」）。
运行：python3 tools/blockbench/params/statue_hades_parts.py
"""
import math

from statue_common import (FACE, Skeleton, arms, bones, fmt, head, legs, mirrored, part, plinth, rounded, skirt, torso,
                           write)

SKIN = "#8A8790"       # 深灰蓝大理石
CLOTH = "#5C5866"
HAIR = "#4E4C56"
PLINTH = "#6A6870"
METAL = "#8C6A3A"      # 暗铜
SHAFT = "#5C4A36"
BORDER = "#4A2E5C"     # 深紫底
PATTERN = "#B8B4C0"    # 银色回纹
BONE = "#D8D2C4"
BEAST = "#3E3A44"      # 三头犬：深色青铜
BEAST_DARK = "#2C2930"
IRIS = "#5E7A86"       # 冷蓝虹膜（发光贴图里再提亮）

TWIST = 0  # 双叉戟绕杆轴的转角，按 SPREAD 计算后在下面校正
BONES = bones({
    "head": [6, -6, 0],
    "upper_arm_l": [15, 0, 4], "forearm_l": [-90, 25, -10], "hand_l": [-15, 0, 0],
    "upper_arm_r": [0, 0, -10], "forearm_r": [-40, 0, -10], "hand_r": [15, 60, -60],
    "shin_r": [0, 0, 1],
    "shin_l": [-4, 0, -1], "foot_l": [4, 0, 0],
})
BONES += [("cape", "torso", [0, 47, -4.4], [0, 0, 0]),
          ("bident", "hand_l", [10.4, 25, 0.3], [0, 0, TWIST]),
          ("cerberus", "root", [-16.5, 0, 1.5], [0, 0, 0])]
SK = Skeleton(BONES)
# 双叉要在世界 x 方向（正面）张开：算出握孔下静止 x 轴的世界方向，求绕杆轴的补转角
_x = SK.direction("hand_l", [1, 0, 0])
_y = SK.direction("hand_l", [0, 1, 0])
TWIST = round(math.degrees(math.atan2(-_x[2], _y[2])) if abs(_y[2]) > 1e-6 else 0.0, 1)
BONES = [b if b[0] != "bident" else ("bident", "hand_l", [10.4, 25, 0.3], [0, 0, TWIST]) for b in BONES]
SK = Skeleton(BONES)
FIST_L = SK.point("hand_l", [10.4, 25, 0.3])
HOLE_L = SK.direction("hand_l", [0, 0, 1])
PALM_R = SK.point("hand_r", [-10.4, 24.6, 0.3])
SPREAD_DIR = SK.direction("bident", [1, 0, 0])

PARTS = plinth(PLINTH) + legs(SKIN, CLOTH, METAL) + skirt(CLOTH, BORDER, METAL, 7.5) + torso(SKIN)
# ── 披风（深色）与立领、肩扣；左肩斜披带（薄）──
PARTS += [part("cape_shoulders", "torso", [0, 47.3, -3.4], [17.4, 1.6, 4.2], CLOTH),
          part("cape_collar", "torso", [0, 51.0, -5.6], [12.4, 5.0, 1.0], CLOTH),
          part("sash_front", "torso", [0.9, 40.5, 5.7], [4.2, 16.0, 0.5], CLOTH, (0, 0, -34), group="sash"),
          part("sash_back", "torso", [0.9, 40.5, -4.85], [4.2, 16.0, 0.5], CLOTH, (0, 0, -34), group="sash")]
PARTS += mirrored("fibula_l", "torso", [7.2, 47.4, 3.2], [1.8, 1.8, 0.8], METAL, group="fibula")
PARTS += [part("cape_upper", "cape", [0, 37.5, -5.3], [17.4, 19.0, 1.0], CLOTH),
          part("cape_lower", "cape", [0, 17.5, -6.1], [18.4, 21.0, 1.0], CLOTH)]
for i, x in enumerate((-5.0, 0.0, 5.0)):
    PARTS.append(part(f"cape_fold_{i}", "cape", [x, 26.5, -6.85], [1.2, 37.0, 0.5], CLOTH, group="cape_fold"))
PARTS.append(part("cape_hem", "cape", [0, 7.6, -6.1], [18.6, 1.2, 1.2], BORDER))
# ── 头（共用，深色）——下须换成更长的一股尖须；暗铜冠：冠带、四根短刺、正中骨白小骷髅 ──
PARTS += [p for p in head(SKIN, HAIR) if p["id"] != "beard_lower"]
PARTS += [part("beard_point", "head", [0, 44.2, 5.2], [4.0, 3.2, 1.6], HAIR),
          part("beard_tip", "head", [0, 42.0, 5.0], [2.2, 1.6, 1.4], HAIR)]
PARTS += [part("crown_front", "head", [0, 59.3, 5.65], [9.8, 1.0, 0.7], METAL),
          part("crown_back", "head", [0, 59.3, -4.95], [11.0, 1.0, 0.8], METAL),
          part("crown_skull", "head", [0, 59.6, 6.1], [2.2, 2.2, 0.6], BONE)]
PARTS += mirrored("crown_side_l", "head", [5.65, 59.3, 0.3], [0.8, 1.0, 9.8], METAL)
PARTS += mirrored("crown_spike_l", "head", [3.2, 60.6, 5.65], [0.8, 1.8, 0.6], METAL, (0, 0, -10), group="crown_spike")
PARTS += mirrored("crown_spike_side_l", "head", [5.65, 60.4, 1.8], [0.6, 1.6, 0.8], METAL, group="crown_spike_side")
# ── 手臂：左手握拳（拄戟），右手张开、掌心朝下（下垂静止坐标里掌心朝内 = +x）──
PARTS += [p for p in arms(SKIN, METAL) if p["id"] not in ("fist_r", "thumb_r")]
PARTS += [part("palm_r", "hand_r", [-10.4, 24.6, 0.3], [1.8, 3.4, 4.4], SKIN),
          part("fingers_r", "hand_r", [-10.4, 21.8, 0.4], [1.5, 2.6, 4.0], SKIN),
          part("thumb_open_r", "hand_r", [-10.2, 24.2, 2.9], [1.0, 2.4, 0.9], SKIN)]

EXTRA = []

# ── 双叉戟：深色杆从底座到戟头；暗铜箍、杖脚、戟座、横梁、两根向外微张的长叉（正面张开）──
SX, SY, SZ = 10.4, 25.0, 0.3
down = FIST_L[1] / HOLE_L[1]
top = 54.0 - FIST_L[1]


def bp(pid, k, d_up, size, color=METAL, rot=(0, 0, 0), group=None):
    return part(pid, "bident", [SX + k, SY, SZ + d_up], size, color, rot, group=group)


EXTRA += [
    bp("bident_shaft", 0, (top - down) / 2, [1.4, 1.4, top + down], SHAFT),
    bp("bident_foot", 0, -down + 0.7, [2.0, 2.0, 1.4]),
    bp("bident_band_low", 0, -3.4, [1.9, 1.9, 0.8], group="bident_band"),
    bp("bident_band_high", 0, 3.4, [1.9, 1.9, 0.8], group="bident_band"),
    bp("bident_socket", 0, top + 0.6, [2.2, 2.2, 1.6]),
    bp("bident_crossbar", 0, top + 1.9, [5.6, 1.2, 1.0]),
    bp("bident_prong_l", 2.3, top + 5.4, [1.0, 1.0, 6.4], rot=(0, 6, 0), group="bident_prong"),
    bp("bident_prong_r", -2.3, top + 5.4, [1.0, 1.0, 6.4], rot=(0, -6, 0), group="bident_prong"),
    bp("bident_tip_l", 2.7, top + 9.0, [0.7, 0.7, 1.4], group="bident_tip"),
    bp("bident_tip_r", -2.7, top + 9.0, [0.7, 0.7, 1.4], group="bident_tip"),
]

# ── 刻耳柏洛斯（v1.1）：坐在底座右侧格子里（y = 0 起），粗壮的坐姿巨犬，约 29u 高——宽大的后臀与后爪、斜向前上的身躯、
#    前探的厚胸、直撑的粗前腿与爪、尖刺项圈、卷在地上的尾；三根粗颈与三颗大头（中间那颗最高、头顶在哈迪斯右掌下，
#    两侧向外张开），每颗头：颅、长口鼻、两只立耳；红眼发光。深色青铜。 ──
CX, CZ = -16.5, 1.5


def cb(pid, dx, y, dz, size, color=BEAST, rot=(0, 0, 0), group=None):
    return part(pid, "cerberus", [CX + dx, y, CZ + dz], size, color, rot, group=group)


EXTRA += [
    cb("cer_haunch_l", 3.4, 4.8, -2.0, [3.8, 9.0, 7.6], group="cer_haunch"),
    cb("cer_haunch_r", -3.4, 4.8, -2.0, [3.8, 9.0, 7.6], group="cer_haunch"),
    cb("cer_hindpaw_l", 3.6, 1.0, 2.4, [2.8, 2.0, 3.6], BEAST_DARK, group="cer_hindpaw"),
    cb("cer_hindpaw_r", -3.6, 1.0, 2.4, [2.8, 2.0, 3.6], BEAST_DARK, group="cer_hindpaw"),
    cb("cer_body", 0, 11.0, -0.4, [8.0, 11.0, 7.0], rot=(-30, 0, 0)),
    cb("cer_chest", 0, 17.0, 2.6, [9.2, 8.0, 7.0], rot=(-10, 0, 0)),
    cb("cer_frontleg_l", 2.6, 7.6, 4.6, [2.8, 13.0, 3.0], group="cer_frontleg"),
    cb("cer_frontleg_r", -2.6, 7.6, 4.6, [2.8, 13.0, 3.0], group="cer_frontleg"),
    cb("cer_frontpaw_l", 2.6, 1.2, 6.2, [3.0, 2.4, 3.6], BEAST_DARK, group="cer_paw"),
    cb("cer_frontpaw_r", -2.6, 1.2, 6.2, [3.0, 2.4, 3.6], BEAST_DARK, group="cer_paw"),
    cb("cer_collar", 0, 21.0, 3.0, [9.6, 1.6, 7.4], METAL),
    cb("cer_tail", 1.0, 1.0, -5.6, [1.6, 1.4, 6.4], rot=(0, 40, 0)),
]
for name, dx, dz, y, ry, rz in (("mid", -0.4, 6.0, 27.0, 0, 0), ("l", 4.6, 4.6, 25.0, 34, -16),
                                 ("r", -5.4, 4.6, 25.0, -34, 16)):
    g = "" if name == "mid" else "_side"
    s_ = math.sin(math.radians(ry))
    c_ = math.cos(math.radians(ry))
    EXTRA += [
        cb(f"cer_neck_{name}", dx * 0.7, y - 3.8, dz - 2.6, [3.6, 5.0, 3.4], rot=(-20, ry, rz), group=f"cer_neck{g}"),
        cb(f"cer_head_{name}", dx, y, dz - 1.2, [4.2, 3.6, 4.4], rot=(0, ry, 0), group=f"cer_head{g}"),
        cb(f"cer_snout_{name}", dx + s_ * 3.4, y - 0.8, dz - 1.2 + c_ * 3.4, [2.6, 2.2, 3.0], BEAST_DARK, (0, ry, 0),
           group=f"cer_snout{g}"),
        cb(f"cer_ear_{name}_a", dx + c_ * 1.4, y + 2.4, dz - 2.2 - s_ * 1.4, [1.0, 1.8, 0.7], rot=(0, ry, -10),
           group="cer_ear"),
        cb(f"cer_ear_{name}_b", dx - c_ * 1.4, y + 2.4, dz - 2.2 + s_ * 1.4, [1.0, 1.8, 0.7], rot=(0, ry, 10),
           group="cer_ear"),
    ]

ATTACHMENTS = [("bident_tip", "bident", [SX, SY, SZ + top + 9.7])]

REVISIONS = [
    "v1.1（自查）：三头犬原来细高、头小，像一根柱子 → 改为粗壮坐姿巨犬（宽臀、前探厚胸、粗前腿、大头长口鼻、两侧头外张）；右手换成四指水平朝外的解。",
    "v1（2026-10-17）：王者立像——左手拄双叉戟、右手按在刻耳柏洛斯头上；深灰蓝大理石、深色立领披风、暗铜骷髅冠；"
    "长尖须、直长发；冷蓝发光眼、三头犬红眼。",
]

if __name__ == "__main__":
    write("hades", "哈迪斯主神像", BONES, PARTS, EXTRA, ATTACHMENTS, REVISIONS,
          {"fk": {"fist_l": FIST_L, "hole_l": HOLE_L, "palm_r": PALM_R, "spread": SPREAD_DIR, "twist": TWIST},
           "face": FACE, "hair_style": "straight", "pattern": "meander", "glow": True,
           "colors": {"skin": SKIN, "cloth": CLOTH, "hair": HAIR, "plinth": PLINTH, "metal": METAL, "border": BORDER,
                      "pattern": PATTERN, "iris": IRIS, "beast": BEAST}})
    print("左拳", fmt(FIST_L), "握孔", fmt(HOLE_L), "右掌", fmt(PALM_R), "双叉张开", fmt(SPREAD_DIR), "补转", TWIST,
          f"戟杆 下 {down:.1f} 上 {top:.1f}")
