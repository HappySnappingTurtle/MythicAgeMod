"""波塞冬主神像（T11b 阶段 ②；王者立像，与宙斯 v2.2 同一形式）部件表。

生成 art/blockbench/greek/statue_poseidon/statue_poseidon.params.json。身体、头与五官来自 statue_common.py。
与宙斯区分的轮廓（2026-10-17）：
  右手在胸侧向外握住竖直的三叉戟（青铜杆、金戟头，三叉向左右张开、正面看得清），戟杆落在底座右侧；
  左手叉腰（拳抵腰侧、肘向外后撑开）；
  金冠上竖五根尖齿（宙斯是橄榄冠）；须发画成波浪纹；
  希玛申从左肩斜披过胸，左肩后垂一片布到小腿（宙斯是双肩披风）；下摆与台座用青绿底金色海浪纹；
  台座左前、左腿外侧一只从浪里跃起的青铜海豚（宙斯是金鹰）。
手臂姿势由网格搜索求出（握孔竖直、拳心不进底座前角的圣火盆格子）。
运行：python3 tools/blockbench/params/statue_poseidon_parts.py
"""
from statue_common import (BRONZE, DARK_GOLD, FACE, GOLD, MARBLE, MARBLE_SHADE, Skeleton, arms, bones, fmt, head, legs,
                           mirrored, part, plinth, skirt, torso, write)

TEAL = "#2E7D7A"
PATINA = "#5E9C8F"
PATINA_DARK = "#3F6F68"

# 三叉戟绕自身杆轴（静止 z 轴）转 TWIST，使三叉在世界 x 方向（正面）张开（检查清单 §三：武器只允许绕柄自转）。
TWIST = 30
# 右臂：上臂外展略后（rx 20、rz −30），前臂向前偏外（rx −110、rz 30），腕微偏（rz −20）→ 握孔竖直，拳心 (−17.8, 39.0, 7.0)。
# 左臂叉腰：上臂外展（rx 10、rz 50），前臂向内下折回（rx −10、ry −15、rz −100）→ 拳抵左腰 (9.9, 33.4, 0.7)，肘在 (18.2, 40.2, −1.8)。
BONES = bones({
    "head": [6, 8, 0],
    "upper_arm_r": [20, 0, -30], "forearm_r": [-110, 0, 30], "hand_r": [0, 0, -20],
    "upper_arm_l": [10, 0, 50], "forearm_l": [-10, -15, -100],
    "shin_r": [0, 0, 1],
    "shin_l": [-4, 0, -1], "foot_l": [4, 0, 0],
}) + [
    ("trident", "hand_r", [-10.4, 25, 0.3], [0, 0, TWIST]),
    ("dolphin", "plinth", [4.5, 4.5, 6.4], [0, 0, 0]),
]
SK = Skeleton(BONES)
FIST_R = SK.point("hand_r", [-10.4, 25, 0.3])
FIST_L = SK.point("hand_l", [10.4, 25, 0.3])
HOLE_R = SK.direction("hand_r", [0, 0, 1])
# 戟头的三叉要在世界 x 方向（正面）张开：取静止坐标里摆好后最接近世界 x 的轴
_ax = {"x": SK.direction("trident", [1, 0, 0]), "y": SK.direction("trident", [0, 1, 0])}
SPREAD = max(_ax, key=lambda k: abs(_ax[k][0]))

PARTS = plinth() + legs() + skirt(hem=TEAL, tail_x=-7.5) + torso()
# ── 希玛申：从左肩斜披到右胯（前后两片，压在胸肌外）、左肩搭布、左肩后垂布 ──
PARTS += [part("sash_front", "torso", [0.9, 40.5, 5.8], [4.6, 16.0, 0.8], MARBLE_SHADE, (0, 0, -34), group="sash"),
          part("sash_back", "torso", [0.9, 40.5, -4.95], [4.6, 16.0, 0.8], MARBLE_SHADE, (0, 0, -34), group="sash"),
          part("shoulder_fold", "torso", [9.0, 48.6, 0.3], [5.6, 0.8, 6.8], MARBLE_SHADE),
          part("back_drape", "torso", [7.0, 31.0, -5.2], [5.0, 34.0, 0.9], MARBLE_SHADE),
          part("back_drape_fold", "torso", [7.6, 30.0, -5.8], [1.0, 30.0, 0.5], MARBLE_SHADE)]
# ── 头（共用）+ 金冠：冠带（前、两侧、后）与五根尖齿（中间最高），两侧各一根短齿 ──
# 头（共用）——下须改为两股叉须（宙斯是一股收尖），加两绺垂到胸前的湿长发
PARTS += [p for p in head() if p["id"] != "beard_lower"]
PARTS += mirrored("beard_fork_l", "head", [1.5, 44.9, 5.2], [2.2, 2.2, 1.6], "#DAD3C4",
                  (0, 0, -8), group="beard_fork")
PARTS += mirrored("hair_front_lock_l", "head", [5.4, 47.0, 2.4], [1.6, 6.4, 1.6], "#DAD3C4", (0, 0, 6),
                  group="hair_front_lock")
PARTS += [part("crown_front", "head", [0, 59.3, 5.65], [9.8, 1.0, 0.7], GOLD),
          part("crown_back", "head", [0, 59.3, -4.95], [11.0, 1.0, 0.8], GOLD)]
PARTS += mirrored("crown_side_l", "head", [5.65, 59.3, 0.3], [0.8, 1.0, 9.8], GOLD)
for i, (x, h) in enumerate(((0.0, 2.6), (1.9, 1.9), (3.8, 1.5))):
    if x == 0:
        PARTS.append(part("crown_spike_mid", "head", [0, 59.8 + h / 2, 5.65], [0.9, h, 0.6], GOLD))
    else:
        PARTS += mirrored(f"crown_spike{i}_l", "head", [x, 59.8 + h / 2, 5.65], [0.8, h, 0.6], GOLD, group=f"crown_spike{i}")
PARTS += mirrored("crown_spike_side_l", "head", [5.65, 60.5, 2.0], [0.6, 1.4, 0.8], GOLD, group="crown_spike_side")
PARTS += arms()

EXTRA = []

# ── 三叉戟：青铜杆从底座（y = 0）到戟头；金箍、杖脚；金戟头 = 戟座 + 横梁 + 三叉（两侧叉向外弯后竖起）+ 倒钩 ──
TX, TY, TZ = -10.4, 25.0, 0.3
down = FIST_R[1] / HOLE_R[1]
top = 54.5 - FIST_R[1]          # 戟座底在 y ≈ 54.5，戟尖约 62.5
sgn = 1 if HOLE_R[1] > 0 else -1


def along(d):
    """静止坐标里沿握孔（z 轴）走 d（世界里向上为正）。"""
    return TZ + sgn * d


def spread(k, d_up, size_spread, size_up, size_other, pid, color=GOLD, group=None, rot=(0, 0, 0)):
    """戟头部件：沿 SPREAD 轴偏 k、沿握孔向上 d_up。"""
    c = [TX, TY, along(d_up)]
    s = [size_other, size_other, size_up]
    if SPREAD == "x":
        c[0] += k
        s[0] = size_spread
    else:
        c[1] += k
        s[1] = size_spread
    return part(pid, "trident", c, s, color, rot, group=group)


EXTRA += [
    part("trident_shaft", "trident", [TX, TY, along((top - down) / 2)], [1.4, 1.4, top + down], BRONZE),
    part("trident_foot", "trident", [TX, TY, along(-down + 0.7)], [2.0, 2.0, 1.4], GOLD),
    part("trident_band_low", "trident", [TX, TY, along(-3.4)], [1.9, 1.9, 0.8], GOLD, group="trident_band"),
    part("trident_band_high", "trident", [TX, TY, along(3.4)], [1.9, 1.9, 0.8], GOLD, group="trident_band"),
    part("trident_socket", "trident", [TX, TY, along(top + 0.6)], [2.2, 2.2, 1.6], GOLD),
    spread(0, top + 1.9, 8.4, 1.0, 1.2, "trident_crossbar"),
    spread(0, top + 5.2, 1.2, 6.0, 1.2, "trident_prong_mid"),
    spread(3.6, top + 4.3, 1.0, 4.4, 1.0, "trident_prong_l", group="trident_prong_side"),
    spread(-3.6, top + 4.3, 1.0, 4.4, 1.0, "trident_prong_r", group="trident_prong_side"),
    spread(0, top + 8.5, 0.8, 1.2, 0.8, "trident_tip_mid"),
    spread(2.9, top + 6.0, 0.6, 1.0, 0.6, "trident_barb_l", group="trident_barb"),
    spread(-2.9, top + 6.0, 0.6, 1.0, 0.6, "trident_barb_r", group="trident_barb"),
]

# ── 海豚（v1.2）：侧身对着正面，在左脚前方从浪上跃起成弧形——尾在台座左前角，身子向中线拱起，尖喙朝向中线略上；
#    正面看是完整的海豚侧影（尖喙、背鳍、上翘的尾鳍）。青铜绿锈色、浅色腹部；浪花底座青绿带白沫。
#    各段按折线顶点摆放：方块沿 x，绕 z 轴转 θ = atan2(dy, dx)。z 在 5.2–7.6，不碰左脚（脚尖到 z 4.7）。
import math

DZ = 6.4


def seg(pid, p0, p1, thick_y, thick_z, color, group=None, extra_len=0.6):
    (x0, y0), (x1, y1) = p0, p1
    dx, dy = x1 - x0, y1 - y0
    theta = math.degrees(math.atan2(dy, dx))
    return part(pid, "dolphin", [(x0 + x1) / 2, (y0 + y1) / 2, DZ], [math.hypot(dx, dy) + extra_len, thick_y, thick_z],
                color, (0, 0, round(theta, 2)), group=group)


EXTRA += [
    part("dolphin_wave", "dolphin", [5.4, 5.0, DZ], [5.6, 1.0, 2.8], TEAL),
    part("dolphin_wave_crest", "dolphin", [7.2, 5.9, DZ], [1.8, 0.8, 2.6], TEAL, (0, 0, 30)),
    part("dolphin_fluke", "dolphin", [8.0, 6.6, DZ], [0.8, 0.6, 3.4], PATINA_DARK, (0, 0, -25)),
    seg("dolphin_tail", (7.9, 6.6), (6.3, 8.3), 1.4, 1.4, PATINA),
    seg("dolphin_body", (6.3, 8.3), (3.4, 10.2), 2.4, 2.2, PATINA),
    seg("dolphin_chest", (3.4, 10.2), (1.7, 10.8), 2.2, 2.0, PATINA),
    seg("dolphin_beak", (1.7, 10.6), (0.2, 10.9), 0.8, 1.0, PATINA, extra_len=0.2),
    part("dolphin_fin", "dolphin", [4.9, 10.9, DZ], [1.4, 1.6, 0.5], PATINA_DARK, (0, 0, 24)),
    part("dolphin_pec_l", "dolphin", [3.4, 9.2, DZ + 1.3], [1.6, 0.4, 0.8], PATINA_DARK, (0, 0, -30), group="dolphin_pec"),
    part("dolphin_pec_r", "dolphin", [3.4, 9.2, DZ - 1.3], [1.6, 0.4, 0.8], PATINA_DARK, (0, 0, -30), group="dolphin_pec"),
]

ATTACHMENTS = [
    ("trident_tip", "trident", [TX, TY, along(top + 9.1)]),
]

REVISIONS = [
    "v1.2（自查）：海豚改为侧身对着正面、从浪上跃起成弧形（竖着贴腿时正面只看到一根柱子）。",
    "v1.1（自查）：五官原与宙斯一模一样 → 下须改两股叉须、加两绺垂胸湿发、波浪发纹加强；海豚放大 1.35 倍转向左前跃起（原来贴在腿边认不出）。",
    "v1（2026-10-17）：王者立像——右手胸侧握竖直三叉戟、左手叉腰；五齿金冠、波浪须发；左肩斜披希玛申与后垂布；"
    "青绿海浪纹下摆与台座；左腿外侧一只跃起的青铜海豚。",
]

if __name__ == "__main__":
    write("poseidon", "波塞冬主神像", BONES, PARTS, EXTRA, ATTACHMENTS, REVISIONS,
          {"fk": {"fist_r": FIST_R, "fist_l": FIST_L, "hole_r": HOLE_R, "spread_axis": SPREAD},
           "face": FACE, "hair_style": "waves", "pattern": "wave", "border_color": TEAL})
    print("三叉张开方向", fmt(_ax[SPREAD]), "右拳", fmt(FIST_R), "握孔", fmt(HOLE_R), "左拳", fmt(FIST_L), "三叉张开轴", SPREAD,
          f"戟杆 下 {down:.1f} 上 {top:.1f}")
