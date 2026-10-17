"""主神像（王者立像）共用部件（T11b 阶段 ②）。

三尊希腊主神像统一「王者立像」形式（2026-10-16 用户选定，仿《神话时代：重制版》城镇神像）：同一套英雄比例的
身体（台座、腿、下身、上身、头与精细五官 v2、手臂），每位主神只写自己的姿势、冠饰、衣着差异、持物与脚边的动物。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u；原点 = 核心方块底面中心，总高 ≤ 64u（4 格）。
各尺寸来自宙斯 v2.2（用户定版），改这里会同时改三尊。
"""
import json
from pathlib import Path

from statue_fk import Skeleton, fmt

ROOT = Path(__file__).resolve().parents[3]

MARBLE = "#E7E2D7"
MARBLE_SHADE = "#D2CBBC"
HAIR = "#DAD3C4"
PLINTH = "#CFCAC0"
GOLD = "#C9A13B"
BRONZE = "#A8782E"
BORDER = "#3E5C8A"
DARK_GOLD = "#8A6420"

# 握拳的静止坐标（手臂下垂）：拳心与握孔方向（静止 z 轴）
FIST = {"l": [10.4, 25, 0.3], "r": [-10.4, 25, 0.3]}

# 共用骨骼（手臂、腿的静止旋转由各主神给出）
def bones(rot):
    """rot：骨骼 id → 静止旋转；其余为 0。返回 [(id, 父, 枢轴, 旋转)]。"""
    base = [
        ("root", None, [0, 0, 0]),
        ("plinth", "root", [0, 0, 0]),
        ("pelvis", "root", [0, 31, 0]),
        ("torso", "pelvis", [0, 33, 0]),
        ("head", "torso", [0, 49, 0.4]),
        ("upper_arm_r", "torso", [-10, 46.5, 0]),
        ("forearm_r", "upper_arm_r", [-10.4, 36, 0]),
        ("hand_r", "forearm_r", [-10.4, 27, 0.3]),
        ("upper_arm_l", "torso", [10, 46.5, 0]),
        ("forearm_l", "upper_arm_l", [10.4, 36, 0]),
        ("hand_l", "forearm_l", [10.4, 27, 0.3]),
        ("shin_r", "pelvis", [-3.2, 19, 0]),
        ("foot_r", "shin_r", [-3.2, 6.6, 0]),
        ("shin_l", "pelvis", [3.6, 19, 0]),
        ("foot_l", "shin_l", [3.6, 6.6, 0]),
    ]
    return [(b, p, piv, list(rot.get(b, [0, 0, 0]))) for b, p, piv in base]


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


def plinth(color=PLINTH):
    """三级台座（底 16、中 15 刻纹、顶 16 线脚），顶面 y = 4.5。"""
    return [part("plinth_base", "plinth", [0, 1.0, 0], [16, 2.0, 16], color),
            part("plinth_mid", "plinth", [0, 2.75, 0], [15, 1.5, 15], color),
            part("plinth_top", "plinth", [0, 4.0, 0], [16, 1.0, 16], color)]


def legs(skin=MARBLE, sole=MARBLE_SHADE, strap=GOLD):
    """小腿（下摆以下可见）、小腿肚、脚、凉鞋底、趾带与踝带。"""
    out = []
    for s, x in (("l", 3.6), ("r", -3.2)):
        out += [part(f"shin_{s}", f"shin_{s}", [x, 12.6, 0.2], [4.8, 12.0, 5.0], skin, group="shin"),
                part(f"calf_{s}", f"shin_{s}", [x, 14.2, -2.4], [3.8, 5.0, 1.2], skin, group="calf"),
                part(f"ankle_strap_{s}", f"shin_{s}", [x, 7.6, 0.2], [5.2, 0.6, 5.4], strap, group="ankle_strap"),
                part(f"foot_{s}", f"foot_{s}", [x, 5.6, 1.2], [4.6, 2.2, 7.0], skin, group="foot"),
                part(f"sole_{s}", f"foot_{s}", [x, 4.75, 1.3], [5.0, 0.5, 7.6], sole, group="sole"),
                part(f"toe_strap_{s}", f"foot_{s}", [x, 6.0, 3.4], [5.0, 0.5, 1.0], strap, group="toe_strap")]
    return out


def skirt(cloth=MARBLE_SHADE, hem=BORDER, belt=GOLD, tail_x=7.5):
    """希玛申裹到小腿中部（倒角）、四道前褶、彩绘下摆、一侧垂角；腰带与带扣。"""
    out = rounded("skirt", "pelvis", [0, 23.5, 0.3], [14, 19, 9.6], cloth, 0.8)
    for i, x in enumerate((-4.5, -1.5, 1.5, 4.5)):
        out.append(part(f"skirt_fold_{i}", "pelvis", [x, 23.0, 5.25], [1.0, 16.0, 0.5], cloth, group="skirt_fold"))
    out += [part("skirt_hem", "pelvis", [0, 15.0, 0.3], [14.6, 2.0, 10.2], hem),
            part("skirt_tail", "pelvis", [tail_x, 22.0, 2.0], [1.2, 14.0, 3.6], cloth, (0, 0, 3 if tail_x > 0 else -3)),
            part("belt", "pelvis", [0, 33.2, 0.3], [14.4, 1.6, 10.0], belt),
            part("belt_buckle", "pelvis", [0, 33.2, 5.45], [2.6, 2.2, 0.6], belt)]
    return out


def torso(skin=MARBLE):
    """腹（倒角）与六块腹肌、腹外斜肌、胸（倒角）与胸肌、斜方肌、三角肌、颈。"""
    out = rounded("abdomen", "torso", [0, 36.6, 0.3], [12, 5.6, 8.0], skin, 0.7)
    for row, y in enumerate((35.0, 36.8, 38.6)):
        out += mirrored(f"abs{row}_l", "torso", [1.5, y, 4.55], [2.6, 1.5, 0.6], skin, group="abs")
    out += mirrored("oblique_l", "torso", [6.5, 38.6, 0.3], [1.4, 5.2, 6.6], skin, group="oblique")
    out += rounded("chest", "torso", [0, 43.0, 0.3], [16, 8, 9.6], skin, 1.0)
    out += mirrored("pec_l", "torso", [3.6, 43.6, 5.1], [6.6, 4.2, 1.0], skin, group="pec")
    out += [part("trapezius", "torso", [0, 47.6, -0.8], [11, 1.8, 6.4], skin)]
    out += mirrored("deltoid_l", "torso", [9.6, 45.6, 0.3], [5.6, 5.4, 6.6], skin, group="deltoid")
    out += [part("neck", "torso", [0, 48.6, 0.6], [5.2, 3.6, 5.0], skin)]
    return out


def head(skin=MARBLE, hair=HAIR):
    """头与精细五官 v2（宙斯 v2.2 定版）：方颅倒角、眉骨、粗眉、颧骨（眼窝）、鼻梁与鼻头、发顶、额前卷发、
    及肩长发、鬓发、垂肩发绺；三层收窄的长须与两腮、两端下垂的八字须、下唇。冠饰由各主神另加。"""
    out = rounded("head", "head", [0, 54, 0.6], [9.6, 10, 9.6], skin, 0.8)
    out += [part("brow", "head", [0, 55.4, 5.55], [8.4, 1.1, 0.7], skin)]
    out += mirrored("eyebrow_l", "head", [2.2, 55.55, 5.95], [3.0, 0.8, 0.5], hair, (0, 0, 6), group="eyebrow")
    out += mirrored("cheek_l", "head", [2.75, 53.1, 5.6], [2.3, 1.2, 0.5], skin, group="cheek")
    out += [part("nose", "head", [0, 54.0, 5.75], [1.4, 2.2, 1.0], skin),
            part("nose_tip", "head", [0, 52.95, 6.15], [2.0, 1.1, 1.4], skin),
            part("hair_cap", "head", [0, 59.4, 0.3], [10.6, 1.8, 10.4], hair),
            part("fringe", "head", [0, 58.2, 5.55], [9.8, 1.4, 0.8], hair),
            part("hair_back", "head", [0, 53.0, -4.6], [10.6, 12.0, 1.2], hair)]
    out += mirrored("hair_side_l", "head", [5.15, 55.0, -0.8], [0.9, 6.0, 8.0], hair)
    out += mirrored("hair_lock_l", "head", [4.4, 46.8, -3.4], [2.4, 5.0, 1.6], hair, group="hair_lock")
    out += [part("beard_front", "head", [0, 50.2, 5.75], [9.0, 5.2, 1.6], hair),
            part("beard_mid", "head", [0, 46.9, 5.6], [7.4, 2.2, 1.6], hair),
            part("beard_lower", "head", [0, 45.0, 5.2], [4.6, 1.8, 1.6], hair)]
    out += mirrored("beard_side_l", "head", [5.0, 51.4, 2.8], [0.9, 5.0, 5.6], hair)
    out += [part("moustache", "head", [0, 51.95, 6.85], [5.6, 0.9, 0.7], hair)]
    out += mirrored("moustache_end_l", "head", [3.05, 51.2, 6.75], [0.9, 1.8, 0.6], hair, group="moustache_end")
    out += [part("lower_lip", "head", [0, 50.95, 6.7], [2.2, 0.6, 0.4], skin)]
    return out


# 五官定位（像素行）：头前面顶边 y = 59，眉骨下沿 ≈ 54.85 → 眼睑从第 9 行起；胡须前片顶 52.8，嘴在第 3 行
FACE = {"eyeTop": 9, "mouthTop": 3, "v": 2}


def arms(skin=MARBLE, band=GOLD):
    """手臂（下垂的静止坐标）：上臂（倒角）、肩头、二头肌、臂箍、前臂、护腕；两手都握拳（拇指在内前侧）。"""
    out = []
    for s in ("l", "r"):
        x = 10.4 if s == "l" else -10.4
        inner = -1 if s == "l" else 1
        out += rounded(f"upper_arm_{s}", f"upper_arm_{s}", [x, 41.2, 0.3], [5.4, 10.5, 5.8], skin, 0.6)
        out += [part(f"shoulder_cap_{s}", f"upper_arm_{s}", [x, 45.4, 0.3], [6.0, 3.0, 6.6], skin, group="shoulder_cap"),
                part(f"biceps_{s}", f"upper_arm_{s}", [x, 41.0, 3.3], [3.8, 5.2, 1.0], skin, group="biceps"),
                part(f"armband_{s}", f"upper_arm_{s}", [x, 38.2, 0.3], [5.9, 1.0, 6.3], band, group="armband"),
                part(f"forearm_{s}", f"forearm_{s}", [x, 31.5, 0.3], [4.8, 9.0, 5.0], skin, group="forearm"),
                part(f"bracer_{s}", f"forearm_{s}", [x, 28.4, 0.3], [5.2, 1.6, 5.4], band, group="bracer"),
                part(f"fist_{s}", f"hand_{s}", [x, 25.0, 0.3], [4.4, 4.0, 5.0], skin, group="fist"),
                part(f"thumb_{s}", f"hand_{s}", [x + inner * 2.4, 25.6, 1.9], [0.9, 2.0, 1.4], skin, group="thumb")]
    return out


def staff_along_hole(prefix, bone, side, fist_world, hole_world, top_y, parts_fn):
    """在握孔方向上排一根竖直长杆：杆底落在 y = 0，杆顶在 top_y。parts_fn(c, down, top) 返回部件列表，
    c = 静止坐标的拳心，沿静止 z 轴量：−down 处为杆底，+top 处为杆顶。"""
    down = fist_world[1] / hole_world[1]
    top = top_y - fist_world[1]
    return parts_fn(FIST[side], down, top), down, top


def write(name, cn, bone_list, parts, extra, attachments, revisions, extra_fields=None):
    """写出 art/blockbench/greek/statue_<name>/statue_<name>.params.json（build_statue.js 读取）。"""
    out = ROOT / f"art/blockbench/greek/statue_{name}/statue_{name}.params.json"
    pivots = {b[0]: b[2] for b in bone_list}
    d = {
        "id": f"ST-GR-{name.upper()}",
        "name": cn,
        "model": f"statue_{name}",
        "height_limit_u": 64,
        "atlas": 512,
        "texels_per_unit": 2,
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": parts,
        "extra_parts": extra,
    }
    d.update(extra_fields or {})
    for bid, parent, pivot, rot in bone_list:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot, "rest_rotation_degrees": rot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p} for a, b, p in attachments]
    d["revisions"] = revisions
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    ids = [p["id"] for p in parts + extra]
    dup = {i for i in ids if ids.count(i) > 1}
    if dup:
        raise SystemExit(f"部件 id 重复：{sorted(dup)}")
    print(f"{cn}：部件 {len(parts)} + 附加 {len(extra)} = {len(parts) + len(extra)} 个方块")
    sk = Skeleton(bone_list)
    for bone, p in (("foot_l", [3.6, 4.5, 1.2]), ("foot_r", [-3.2, 4.5, 1.2])):
        print(" ", bone, "鞋底中心", fmt(sk.point(bone, p)))
    return d
