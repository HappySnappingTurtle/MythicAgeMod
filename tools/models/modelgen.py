"""方块模型与像素贴图规格的公共函数（T8b 起使用；T8a 的 greek_temple.py 自带同名函数，保持不变）。"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODELS = ROOT / "neoforge/src/main/resources/assets/mythic_age/models/block"
SPECS = ROOT / "tools/texgen/specs/greek"

MARBLE = {"h": "#f6f2ea", "m": "#ebe5d8", "n": "#e0d9ca", "j": "#c4bba8", "k": "#a99f8b", "d": "#8f8676"}
TERRACOTTA = {"R": "#e0875a", "r": "#b5532e", "s": "#7f3519", "b": "#2b1a12", "o": "#9a4426"}
NAMES = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}


def tex(name: str) -> str:
    return "mythic_age:block/" + name


def spec(name: str, grid: list[str], palette: dict[str, str], size: int = 16, folder: str = "block/") -> None:
    """size×size（16 或 32）像素贴图规格，输出到 textures/<folder><name>.png；'.' 为透明。"""
    assert len(grid) == size and all(len(r) == size for r in grid), name
    used = {c for r in grid for c in r} - {"."}
    missing = used - set(palette)
    assert not missing, f"{name}: 调色板缺少 {missing}"
    data = {"template": "pixels", "out": folder + name, "size": size,
            "palette": {k: v for k, v in palette.items() if k in used},
            "params": {"kind": "block", "rows": grid}}
    (SPECS / f"{name}.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def box(frm, to, t, faces="nsewud", uv=None, comment=None, rotation=None, shade=True) -> dict:
    """t 可以是贴图键，也可以是 {面名: 贴图键}；uv 为 {面名: [u0, v0, u1, v1]}，缺省时按方块坐标自动取。"""
    el = {}
    if comment:
        el["__comment"] = comment
    el["from"] = [round(v, 4) for v in frm]
    el["to"] = [round(v, 4) for v in to]
    if rotation:
        el["rotation"] = rotation
    if not shade:
        el["shade"] = False
    el["faces"] = {}
    for c in faces:
        n = NAMES[c]
        f = {"texture": "#" + (t if isinstance(t, str) else t[n])}
        if uv and n in uv:
            f["uv"] = uv[n]
        el["faces"][n] = f
    return el


def plane(frm, to, t, normal: str, uv=None, comment=None, rotation=None) -> dict:
    """零厚度面片，正反两面贴同一张图（cutout 叶片、瓦当用）。"""
    opp = {"north": "south", "south": "north", "east": "west", "west": "east"}[normal]
    el = {"from": list(frm), "to": list(to), "faces": {}}
    if comment:
        el["__comment"] = comment
    if rotation:
        el["rotation"] = rotation
    for n in (normal, opp):
        f = {"texture": "#" + t}
        if uv:
            f["uv"] = uv
        el["faces"][n] = f
    return el


def cross(x0, x1, y0, y1, inset, t, faces="nsewud", comment=None, uv=None):
    """「十」字截面的圆柱近似：两个方块交叉，四角各切去 inset，截面读作圆形。"""
    return [box((x0, y0, x0 + inset), (x1, y1, x1 - inset), t, faces, uv=uv, comment=comment),
            box((x0 + inset, y0, x0), (x1 - inset, y1, x1), t, faces, uv=uv)]


def model(name: str, textures: dict, elements: list, parent="minecraft:block/block", render_type=None,
          display=None) -> None:
    m = {"parent": parent, "textures": textures, "elements": elements}
    if render_type:
        m["render_type"] = render_type
    if display:
        m["display"] = display
    (MODELS / f"{name}.json").write_text(json.dumps(m, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def rotate_y90(elements: list) -> list:
    """把元素绕方块中心转 90°（x → z），用于由 _z 模型派生 _x 模型（面名同步轮换，旋转属性一并转换）。"""
    face_map = {"north": "east", "east": "south", "south": "west", "west": "north", "up": "up", "down": "down"}
    out = []
    for el in elements:
        e = json.loads(json.dumps(el))
        (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
        # 绕中心 (8, 8) 顺时针（俯视）转 90°：(x, z) → (16 - z, x)
        e["from"] = [16 - z1, y0, x0]
        e["to"] = [16 - z0, y1, x1]
        e["faces"] = {face_map[k]: v for k, v in el["faces"].items()}
        if "rotation" in el:
            r = dict(el["rotation"])
            ox, oy, oz = r["origin"]
            r["origin"] = [16 - oz, oy, ox]
            if r["axis"] == "x":
                r["axis"] = "z"
            elif r["axis"] == "z":
                r["axis"] = "x"
                r["angle"] = -r["angle"]
            e["rotation"] = r
        out.append(e)
    return out


def grid(w: int = 16, h: int = 16, fill: str = ".") -> list[list[str]]:
    return [[fill] * w for _ in range(h)]


def rows(g: list[list[str]]) -> list[str]:
    return ["".join(r) for r in g]


def noise(x: int, y: int, k: int) -> float:
    """可复现的像素噪声，0–1。"""
    h = (x * 374761393 + y * 668265263 + k * 2147483647) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFFFFFF) / 0xFFFFFFFF
