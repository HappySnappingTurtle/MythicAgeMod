"""根刺（树仙女的攻击，T15 阶段 ⑤）部件表。生成 art/blockbench/greek/hamadryad/root_spike.params.json。

地面裂纹（crack：一片贴地薄板，贴图只画裂缝、其余透明）+ 三根橡树根（spike_a/b/c，各两节，向外略斜）。
根平时藏在地下（动画把它们压到 y −14），出刺时冲出地面。
运行：python3 tools/blockbench/params/root_spike_parts.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "art/blockbench/greek/hamadryad/root_spike.params.json"

CRACK = "#2a1f14"
ROOT_BARK = "#4a3826"
ROOT_TIP = "#6b5232"

BONES = [("root", None, [0, 0, 0]), ("crack", "root", [0, 0, 0])]
SPIKES = []
for i, a in enumerate((20, 140, 260)):
    r = math.radians(a)
    x, z = math.sin(r) * 3.5, math.cos(r) * 3.5
    BONES.append((f"spike_{'abc'[i]}", "root", [round(x, 3), 0, round(z, 3)]))
    SPIKES.append((i, a, x, z))


def part(pid, bone, center, size, color, rot=(0, 0, 0), group=None):
    return {"id": pid, "bone": bone, "center_model_units": [round(c, 4) for c in center],
            "size_model_units": list(size), "rotation_degrees_model_axes": [round(v, 3) for v in rot], "color": color,
            "uv_group": group or pid, "uv_share": None, "surface_kind": "cuboid"}


PARTS = [part("crack", "crack", [0, 0.06, 0], [26, 0.12, 26], CRACK)]
for i, a, x, z in SPIKES:
    b = f"spike_{'abc'[i]}"
    # 向外斜 18°：绕「指向外的水平轴」的垂直轴转——用 rx（前后）与 rz（左右）近似分解
    tilt = 18
    rx = round(tilt * math.cos(math.radians(a)), 2)
    rz = round(-tilt * math.sin(math.radians(a)), 2)
    PARTS.append(part(f"{b}_base", b, [x, 4.0, z], [3.4, 8.0, 3.4], ROOT_BARK, (rx, 0, rz), group="spike_base"))
    ox, oz = math.sin(math.radians(a)) * 2.4, math.cos(math.radians(a)) * 2.4
    PARTS.append(part(f"{b}_tip", b, [x + ox, 11.0, z + oz], [2.0, 8.0, 2.0], ROOT_TIP, (rx * 1.6, 0, rz * 1.6), group="spike_tip"))


def main():
    pivots = {b[0]: b[2] for b in BONES}
    d = {"id": "WI-GR-05-ROOT", "name": "根刺", "model": "root_spike", "height_limit_u": 18.0, "atlas": 128, "texels_per_unit": 2,
         "collision_proposal_blocks": [1.6, 1.0, 1.6],
         "coordinate_system": "X anatomical left; Y up; Z forward; model rest coordinates; 16 units per block",
         "bones": [], "extra_bones": [], "parts": PARTS, "extra_parts": []}
    for bid, parent, pivot in BONES:
        pp = pivots[parent] if parent else [0, 0, 0]
        d["bones"].append({"id": bid, "parent": parent, "pivot_model_units": pivot,
                           "pivot_parent_local_units": [round(pivot[i] - pp[i], 4) for i in range(3)]})
    d["attachments"] = []
    d["revisions"] = ["v1（2026-10-22）：裂纹薄板 + 三根两节橡树根。"]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(PARTS)} 个方块")


if __name__ == "__main__":
    main()
