"""卡尔基诺斯（WI-GR-04，RIG-KARKINOS，T15 阶段 ④）部件表。

生成 art/blockbench/greek/karkinos/karkinos.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。

造型（docs/重构/设计/逐项设计/希腊首批/09_卡尔基诺斯_模型与行为卡.md）：按地中海岸蟹（Carcinus）放大——头胸甲前宽后窄、
前侧缘各 5 枚锯齿，一对眼柄，一对大螯（掌、不动指、可动指 claw_*_finger 分节），四对两节步足；
甲面橄榄青铜绿带深斑，指尖深褐，腹面淡黄（翻倒时朝上）。
运行：python3 tools/blockbench/params/karkinos_parts.py
"""
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/karkinos/karkinos.params.json"

SHELL = "#556236"
SHELL_DARK = "#3b4526"
SHELL_EDGE = "#77854a"
BELLY = "#c9b27a"
BELLY_DARK = "#b09a62"
JOINT = "#8a6a32"
TIP = "#2a221c"
EYE = "#111214"
MOUTH = "#7a5a34"

LEG_Z = (3.0, 0.6, -1.8, -4.2)
LEG_SWEEP = (1.0, 0.0, -0.9, -1.9)  # 膝相对基部前后偏（后面的腿向后掠）
LEG_BASE_X, LEG_BASE_Y = 7.6, 7.6
KNEE_X, KNEE_Y = 13.2, 10.4
FOOT_X = 18.6

BONES = [("root", None, [0, 0, 0]), ("body", "root", [0, 8, 0]), ("mouth", "body", [0, 7.2, 6.2])]
BONES += [("eyestalk_l", "body", [2.2, 10.5, 6.0]), ("eyestalk_r", "body", [-2.2, 10.5, 6.0])]
# 螯足（左）：长节从基部斜向前外 35°，掌与两指斜向前内 30°，两只螯在身前合成一个「八」字
ARM = (6.5, 7.5, 5.0)
MERUS_DIR = (math.sin(math.radians(35)), 0.1, math.cos(math.radians(35)))
WRIST = tuple(ARM[k] + 5.0 * MERUS_DIR[k] for k in range(3))
CLAW_RY = -30.0
CLAW_DIR = (math.sin(math.radians(CLAW_RY)), 0.0, math.cos(math.radians(CLAW_RY)))


def along(dist, dy=0.0, base=None):
    b = base or WRIST
    return [b[0] + CLAW_DIR[0] * dist, b[1] + dy, b[2] + CLAW_DIR[2] * dist]


FINGER = along(6.2, 1.0)
for s in (1, -1):
    t = "l" if s > 0 else "r"
    BONES += [(f"arm_{t}", "body", [ARM[0] * s, ARM[1], ARM[2]]), (f"claw_{t}", f"arm_{t}", [WRIST[0] * s, WRIST[1], WRIST[2]]),
              (f"claw_{t}_finger", f"claw_{t}", [FINGER[0] * s, FINGER[1], FINGER[2]])]
    for i, (z, sw) in enumerate(zip(LEG_Z, LEG_SWEEP), 1):
        BONES += [(f"leg_{t}{i}", "body", [LEG_BASE_X * s, LEG_BASE_Y, z]),
                  (f"leg_{t}{i}_lower", f"leg_{t}{i}", [KNEE_X * s, KNEE_Y, z + sw])]

ATTACHMENTS = [
    ("eye_l", "eyestalk_l", [2.2, 12.9, 6.3]),
    ("eye_r", "eyestalk_r", [-2.2, 12.9, 6.3]),
    ("pinch", "body", [0, 8.0, 20.0]),
    ("belly", "body", [0, 6.0, 0.0]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None):
    return {"id": pid, "bone": bone, "center_model_units": [round(c, 4) for c in center],
            "size_model_units": [round(v, 4) for v in size], "rotation_degrees_model_axes": [round(r, 3) for r in rot],
            "color": color, "uv_group": group or pid, "uv_share": None, "surface_kind": "cuboid"}


def mirror_name(name):
    return re.sub(r"_l(?=\d|_|$)", "_r", name)


def mirrored(pid_l, bone_l, center, size, color, rot=(0, 0, 0), group=None):
    """左侧部件 + 镜像的右侧部件（x 取反、ry 与 rz 取反），两侧共用 UV 组。"""
    g = group or re.sub(r"_l(?=\d|_|$)", "", pid_l)
    left = part(pid_l, bone_l, center, size, color, rot, g)
    right = part(mirror_name(pid_l), mirror_name(bone_l), [-center[0], center[1], center[2]], size, color,
                 (rot[0], -rot[1], -rot[2]), g)
    left["uv_share"] = right["uv_share"] = g
    return [left, right]


def rounded(pid, bone, center, size, color, bevel=0.6, rot=(0, 0, 0)):
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color, rot),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color, rot)]


PARTS = []
add = PARTS.append
ext = PARTS.extend

# ── 头胸甲：倒角主甲、甲顶隆起、额缘（两眼之间）、后缘收窄、甲缘亮线 ──
ext(rounded("carapace", "body", [0, 9.0, 0.4], [16.0, 4.0, 11.0], SHELL, 1.2))
ext(rounded("dome", "body", [0, 11.6, 0.0], [12.0, 1.4, 8.0], SHELL, 1.0))
add(part("rear", "body", [0, 8.8, -5.6], [10.0, 3.2, 1.6], SHELL))
add(part("front", "body", [0, 9.9, 6.2], [5.6, 1.6, 1.0], SHELL_DARK))
# 前侧缘 5 枚锯齿：从额缘外侧沿弧线到甲最宽处
for i in range(5):
    x = 3.9 + i * 1.12
    z = 6.1 - i * 1.15
    ext(mirrored(f"tooth_{i}_l", "body", [x + 0.35, 9.4, z + 0.35], [1.1, 0.9, 1.1], SHELL_DARK, (0, 45, 0), group="tooth"))

# ── 腹面：胸板（淡黄）与折在下面的腹节 ──
add(part("sternum", "body", [0, 6.6, 0.4], [12.0, 1.0, 9.0], BELLY))
add(part("abdomen", "body", [0, 6.0, -0.6], [5.0, 0.6, 6.0], BELLY_DARK))
add(part("mouthparts", "mouth", [0, 7.7, 6.2], [3.0, 1.6, 0.6], MOUTH))

# ── 眼柄与眼 ──
ext(mirrored("eyestalk_l", "eyestalk_l", [2.2, 11.5, 6.2], [0.7, 2.2, 0.7], SHELL, group="eyestalk"))
ext(mirrored("eye_l", "eyestalk_l", [2.2, 12.8, 6.3], [1.0, 1.0, 1.0], EYE, group="eye"))

# ── 螯足：长节（斜向前外）、腕节、掌（倒角）、不动指 + 指尖、可动指 + 指尖；掌与指都斜向前内 ──
_mry = math.degrees(math.atan2(MERUS_DIR[0], MERUS_DIR[2]))
ext(mirrored("merus_l", "arm_l", [(ARM[k] + WRIST[k]) / 2 for k in range(3)], [2.2, 2.2, 5.6], SHELL, (0, _mry, 0), group="merus"))
ext(mirrored("carpus_l", "claw_l", along(0.4), [2.6, 2.4, 2.6], SHELL, (0, CLAW_RY, 0), group="carpus"))
ext(mirrored("palm_l", "claw_l", along(3.8, 0.3), [3.0, 3.6, 5.4], SHELL, (0, CLAW_RY, 0), group="palm"))
ext(mirrored("palm_side_l", "claw_l", along(3.8, 0.3), [4.2, 3.6, 4.2], SHELL, (0, CLAW_RY, 0), group="palm_side"))
ext(mirrored("pollex_l", "claw_l", along(8.5, -0.8), [1.6, 1.4, 4.4], SHELL, (0, CLAW_RY, 0), group="pollex"))
ext(mirrored("pollex_tip_l", "claw_l", along(11.1, -0.9), [1.2, 1.2, 1.2], TIP, (0, CLAW_RY, 0), group="claw_tip"))
ext(mirrored("dactyl_l", "claw_l_finger", along(8.6, 1.1), [1.6, 1.4, 4.6], SHELL, (0, CLAW_RY, 0), group="dactyl"))
ext(mirrored("dactyl_tip_l", "claw_l_finger", along(11.3, 1.0), [1.2, 1.2, 1.2], TIP, (0, CLAW_RY, 0), group="claw_tip"))

# ── 步足：上节（向外上斜、后面的腿向后掠），关节一圈橙褐，下节斜插到地上，足尖深褐 ──
for i, (z, sw) in enumerate(zip(LEG_Z, LEG_SWEEP), 1):
    bone = f"leg_l{i}"
    a = (LEG_BASE_X, LEG_BASE_Y, z)
    b = (KNEE_X, KNEE_Y, z + sw)
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    L = math.sqrt(dx * dx + dy * dy + dz * dz)
    ry = -math.degrees(math.asin(dz / L))
    rz = math.degrees(math.atan2(dy, dx))
    mid = [(a[k] + b[k]) / 2 for k in range(3)]
    ext(mirrored(f"leg_l{i}_upper", bone, mid, [L + 0.4, 1.7, 1.7], SHELL, (0, ry, rz), group="leg_upper"))
    ext(mirrored(f"leg_l{i}_joint", f"leg_l{i}_lower", list(b), [1.5, 1.5, 1.5], JOINT, group="leg_joint"))
    foot = (FOOT_X, 0.3, z + sw * 1.8)
    fx, fy, fz = foot[0] - b[0], foot[1] - b[1], foot[2] - b[2]
    ll = math.sqrt(fx * fx + fy * fy + fz * fz)
    # 本地 +Y 指向膝（-d）：R = Rz(θ)·Rx(φ)，Rz·Rx·(0,1,0) = (−sinθ·cosφ, cosθ·cosφ, sinφ)
    phi = math.asin(-fz / ll)
    theta = math.atan2(fx / ll / math.cos(phi), -fy / ll / math.cos(phi))
    lmid = [(b[k] + foot[k]) / 2 for k in range(3)]
    ext(mirrored(f"leg_l{i}_lower", f"leg_l{i}_lower", lmid, [1.2, ll, 1.2], SHELL,
                 (math.degrees(phi), 0, math.degrees(theta)), group="leg_lower"))
    tip = [foot[0] - fx / ll * 0.6, foot[1] + 0.5, foot[2] - fz / ll * 0.6]
    ext(mirrored(f"leg_l{i}_tip", f"leg_l{i}_lower", tip, [0.8, 1.0, 0.8], TIP,
                 (math.degrees(phi), 0, math.degrees(theta)), group="leg_tip"))


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "WI-GR-04",
        "name": "卡尔基诺斯",
        "model": "karkinos",
        "height_limit_u": 15.0,
        "atlas": 256,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [1.4, 0.8, 1.4],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": PARTS,
        "extra_parts": [],
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["白模 v1（2026-10-22）：按地中海岸蟹放大；前侧缘 5 齿、眼柄、分节大螯（可动指）、四对两节步足；"
                      "橄榄青铜绿甲、深褐指尖、淡黄腹面。",
                      "v1.1（2026-10-22，自查）：步足拱起外伸（膝抬高、足尖外展到 18.6u）；两螯斜向前内成八字；口器缩小改深色；眼缩小。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(PARTS)} 个方块，{len(BONES)} 根骨骼")


if __name__ == "__main__":
    main()
