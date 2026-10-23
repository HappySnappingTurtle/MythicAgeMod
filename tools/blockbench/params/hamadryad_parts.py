"""树仙女（WI-GR-05，RIG-HAMADRYAD，T15 阶段 ⑤）部件表。

生成 art/blockbench/greek/hamadryad/hamadryad.params.json。
坐标为设计卡模型空间：+X 角色左、+Y 上、+Z 前，单位 u（16u = 1 格）。左右对称的部件只写左侧（x > 0）。

造型（docs/重构/设计/逐项设计/希腊首批/10_树仙女_模型与行为卡.md）：两个形态组共用一个模型——
  maiden：少女，未染亚麻希顿（长到小腿）、绿腰带，深栗发编着橡叶与橡子、戴橡叶冠，小臂与小腿外侧带树皮纹，赤足；
  tree：一棵细橡树，深色树皮，树皮上隐约一张脸（只用阴影），树冠是几团橡叶（贴图镂空）。
两组在绑定姿势里重叠，由动画把不用的那组缩成 0（idle / walk 里 tree 缩 0，tree 动画里 maiden 缩 0）。
运行：python3 tools/blockbench/params/hamadryad_parts.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/hamadryad/hamadryad.params.json"

SKIN = "#d4b896"
HAIR = "#4a3a24"
LEAF = "#5d7a32"
ACORN = "#8a6a32"
LINEN = "#d8cfb4"
LINEN_DARK = "#c4b998"
GIRDLE = "#5d6b3a"
BARK = "#3f2f1e"
BARK_DARK = "#2e2216"
CANOPY = "#4f6e2a"
CANOPY_DARK = "#3e5a22"

BONES = [
    ("root", None, [0, 0, 0]),
    ("maiden", "root", [0, 0, 0]),
    ("body", "maiden", [0, 12.0, 0]),
    ("chest", "body", [0, 13.5, 0]),
    ("neck", "chest", [0, 21.6, 0.2]),
    ("head", "neck", [0, 22.4, 0.3]),
    ("arm_l", "chest", [3.3, 20.6, 0]),
    ("forearm_l", "arm_l", [3.5, 16.8, 0]),
    ("arm_r", "chest", [-3.3, 20.6, 0]),
    ("forearm_r", "arm_r", [-3.5, 16.8, 0]),
    ("leg_l", "body", [1.2, 12.0, 0]),
    ("shin_l", "leg_l", [1.2, 6.4, 0.1]),
    ("leg_r", "body", [-1.2, 12.0, 0]),
    ("shin_r", "leg_r", [-1.2, 6.4, 0.1]),
    ("tree", "root", [0, 0, 0]),
    ("trunk", "tree", [0, 0, 0]),
    ("crown", "trunk", [0, 30.0, 0]),
]

ATTACHMENTS = [
    ("eye", "head", [1.25, 25.0, 2.8]),
    ("hand_l", "forearm_l", [3.6, 12.6, 0.2]),
    ("hand_r", "forearm_r", [-3.6, 12.6, 0.2]),
    ("tree_face", "trunk", [0, 20.0, 4.2]),
]


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None):
    return {"id": pid, "bone": bone, "center_model_units": [round(c, 4) for c in center],
            "size_model_units": [round(v, 4) for v in size], "rotation_degrees_model_axes": [round(r, 3) for r in rot],
            "color": color, "uv_group": group or pid, "uv_share": None, "surface_kind": "cuboid"}


def mirror_bone(bone):
    if bone.endswith("_l"):
        return bone[:-2] + "_r"
    return bone


def mirrored(pid_l, bone_l, center, size, color, rot=(0, 0, 0), group=None):
    g = group or pid_l.removesuffix("_l")
    left = part(pid_l, bone_l, center, size, color, rot, g)
    pid_r = pid_l[:-2] + "_r" if pid_l.endswith("_l") else pid_l + "_r"
    right = part(pid_r, mirror_bone(bone_l), [-center[0], center[1], center[2]], size, color,
                 (rot[0], -rot[1], -rot[2]), g)
    left["uv_share"] = right["uv_share"] = g
    return [left, right]


def rounded(pid, bone, center, size, color, bevel=0.6, rot=(0, 0, 0)):
    w, h, d = size
    return [part(pid, bone, center, [w - 2 * bevel, h, d], color, rot),
            part(pid + "_side", bone, center, [w, h, d - 2 * bevel], color, rot)]


MAIDEN = []
add = MAIDEN.append
ext = MAIDEN.extend

# ── 希顿：长到小腿的裙（略呈 A 字，下摆外张）、腰带、上身、胸、肩头的别针褶 ──
ext(rounded("skirt", "body", [0, 8.4, 0], [5.4, 7.6, 3.8], LINEN, 0.6))
add(part("hem", "body", [0, 4.9, 0], [6.0, 1.2, 4.4], LINEN_DARK))
add(part("girdle", "chest", [0, 13.2, 0], [4.8, 0.7, 3.2], GIRDLE))
ext(rounded("waist", "chest", [0, 14.9, 0], [4.4, 2.8, 2.8], LINEN, 0.5))
ext(rounded("bodice", "chest", [0, 18.6, 0], [5.6, 4.6, 3.0], LINEN, 0.6))
add(part("bust", "chest", [0, 18.2, 1.7], [4.2, 1.6, 0.6], LINEN_DARK))
ext(mirrored("pin_l", "chest", [2.2, 20.95, 0.0], [1.4, 0.5, 2.4], LINEN_DARK, group="pin"))
add(part("collar", "chest", [0, 21.0, 0.3], [2.6, 0.4, 2.0], SKIN))
add(part("neck", "neck", [0, 22.1, 0.2], [1.6, 1.6, 1.6], SKIN))

# ── 头：颅（脸前面 10 × 10 像素）、鼻、发（顶、后长发到背、两侧发辫）、橡叶冠与橡子 ──
HY, HZ = 25.0, 0.3
add(part("head", "head", [0, HY, HZ], [5.0, 5.0, 5.0], SKIN))
add(part("nose", "head", [0, HY - 0.6, HZ + 2.75], [0.7, 1.1, 0.5], SKIN))
add(part("hair_cap", "head", [0, HY + 2.75, HZ - 0.2], [5.4, 0.6, 5.4], HAIR))
add(part("hair_back", "head", [0, HY - 3.2, HZ - 2.75], [5.2, 9.6, 0.7], HAIR, (6, 0, 0)))
ext(mirrored("hair_side_l", "head", [2.75, HY + 0.4, HZ - 0.4], [0.6, 4.2, 4.2], HAIR, group="hair_side"))
ext(mirrored("braid_l", "head", [2.6, HY - 4.0, HZ + 0.6], [1.0, 5.0, 1.0], HAIR, (-8, 0, 6), group="braid"))
add(part("fringe", "head", [0, HY + 2.15, HZ + 2.6], [5.0, 0.7, 0.5], HAIR))
# 橡叶冠：前后左右四片叶团 + 前额两侧各一颗橡子
add(part("crown_front", "head", [0, HY + 2.9, HZ + 2.6], [4.4, 1.2, 0.8], LEAF))
add(part("crown_back", "head", [0, HY + 2.9, HZ - 2.7], [4.4, 1.2, 0.8], LEAF))
ext(mirrored("crown_side_l", "head", [2.8, HY + 2.8, HZ], [0.8, 1.3, 4.6], LEAF, group="crown_side"))
ext(mirrored("leaf_tuft_l", "head", [2.6, HY + 3.4, HZ + 1.8], [1.6, 1.0, 1.4], LEAF, (0, 30, 20), group="leaf_tuft"))
ext(mirrored("acorn_l", "head", [1.6, HY + 2.5, HZ + 3.0], [0.8, 0.9, 0.8], ACORN, group="acorn"))

# ── 手臂（赤臂）：上臂、小臂（外侧树皮纹在贴图）、手 ──
ext(mirrored("upper_arm_l", "arm_l", [3.5, 18.8, 0], [1.6, 4.0, 1.6], SKIN, group="upper_arm"))
ext(mirrored("forearm_l", "forearm_l", [3.6, 14.9, 0.1], [1.5, 3.8, 1.5], SKIN, group="forearm"))
ext(mirrored("hand_l", "forearm_l", [3.6, 12.4, 0.2], [1.4, 1.4, 1.2], SKIN, group="hand"))

# ── 腿：大腿藏在裙里，小腿露出（外侧树皮纹），赤足 ──
ext(mirrored("thigh_l", "leg_l", [1.2, 9.3, 0], [1.9, 5.2, 1.9], SKIN, group="thigh"))
ext(mirrored("shin_l", "shin_l", [1.2, 3.4, 0.1], [1.7, 5.4, 1.7], SKIN, group="shin"))
ext(mirrored("foot_l", "shin_l", [1.2, 0.4, 0.6], [1.7, 0.8, 2.6], SKIN, group="foot"))

TREE = []
tadd = TREE.append
text = TREE.extend
# ── 化树：根部外张、树干两节、树皮人脸在贴图；四条根 ──
text(rounded("trunk_base", "trunk", [0, 3.5, 0], [10.0, 7.0, 10.0], BARK, 1.5))
text(rounded("trunk_mid", "trunk", [0, 15.0, 0], [8.0, 16.0, 8.0], BARK, 1.2))
text(rounded("trunk_top", "trunk", [0, 27.0, 0], [6.0, 8.0, 6.0], BARK, 0.8))
for i, a in enumerate((45, 135, 225, 315)):
    r = math.radians(a)
    tadd(part(f"root_{i}", "trunk", [math.sin(r) * 5.6, 1.0, math.cos(r) * 5.6], [2.4, 2.0, 4.0], BARK_DARK, (0, a, 0),
              group="root_knot"))
# 枝：三根粗枝伸向叶团
tadd(part("branch_a", "crown", [5.0, 33.0, 1.0], [8.0, 2.0, 2.0], BARK, (0, -10, 30)))
tadd(part("branch_b", "crown", [-5.0, 32.0, -1.5], [8.0, 2.0, 2.0], BARK, (0, 15, -28)))
tadd(part("branch_c", "crown", [0.5, 34.0, 5.0], [2.0, 2.0, 8.0], BARK, (-30, 0, 0)))
# 叶团：主冠 + 三团侧冠 + 顶
tadd(part("canopy_main", "crown", [0, 38.0, 0], [20.0, 9.0, 20.0], CANOPY))
tadd(part("canopy_l", "crown", [10.5, 34.5, 2.0], [10.0, 7.0, 10.0], CANOPY_DARK))
tadd(part("canopy_r", "crown", [-10.0, 34.0, -3.0], [10.0, 7.0, 10.0], CANOPY_DARK))
tadd(part("canopy_f", "crown", [1.0, 35.0, 10.0], [9.0, 6.0, 8.0], CANOPY_DARK))
tadd(part("canopy_top", "crown", [0, 44.0, 0], [12.0, 4.0, 12.0], CANOPY))


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {
        "id": "WI-GR-05",
        "name": "树仙女",
        "model": "hamadryad",
        "height_limit_u": 48.0,
        "atlas": 256,
        "texels_per_unit": 2,
        "collision_proposal_blocks": [0.6, 1.8, 0.6],
        "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
        "bones": [],
        "extra_bones": [],
        "parts": MAIDEN,
        "extra_parts": TREE,
    }
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = [{"id": a, "bone": b, "position_model_units": p,
                         "position_bone_local_units": [round(p[i] - pivots[b][i], 4) for i in range(3)]}
                        for a, b, p in ATTACHMENTS]
    d["revisions"] = ["白模 v1（2026-10-22）：少女（亚麻希顿、橡叶冠、发辫、树皮纹小臂小腿、赤足）+ 化树（深树皮、人脸阴影、"
                      "四团镂空橡叶冠）两个形态组。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"少女 {len(MAIDEN)} + 化树 {len(TREE)} = {len(MAIDEN) + len(TREE)} 个方块")


if __name__ == "__main__":
    main()
