"""村庄界面（GA-12 方案 A「告示牌墙」）的九宫格贴图：外框、状态栏、木牌（普通 / 选中 / 悬停）、羊皮纸、按钮。

输出到 assets/mythic_age/textures/gui/sprites/village/，并写 .mcmeta（nine_slice）。随机数固定种子，重跑结果不变。
用法：python3 tools/texgen/village_gui.py
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "neoforge/src/main/resources/assets/mythic_age/textures/gui/sprites/village"


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)


def wood(w, h, base, grain, seed, vertical=False):
    """木纹：沿长边的深浅条纹 + 少量节疤。"""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (w, h), base)
    px = img.load()
    rows = w if vertical else h
    offs = [rnd.choice([-6, -3, 0, 0, 2, 4]) for _ in range(rows)]
    for y in range(h):
        for x in range(w):
            r = x if vertical else y
            d = offs[r] + rnd.choice([0, 0, 0, -2, 2])
            px[x, y] = shade(base, d)
    for _ in range(max(1, (w * h) // 400)):
        cx, cy = rnd.randrange(w), rnd.randrange(h)
        for dx in (-1, 0, 1):
            nx = cx + dx
            if 0 <= nx < w:
                px[nx, cy] = grain
    return img


def border(img, outer, light, dark, width=1):
    w, h = img.size
    px = img.load()
    for i in range(width):
        for x in range(w):
            px[x, i] = outer if i == 0 else light
            px[x, h - 1 - i] = outer if i == 0 else dark
        for y in range(h):
            px[i, y] = outer if i == 0 else light
            px[w - 1 - i, y] = outer if i == 0 else dark
    return img


def save(name, img, border_px):
    OUT.mkdir(parents=True, exist_ok=True)
    img.save(OUT / f"{name}.png")
    meta = {"gui": {"scaling": {"type": "nine_slice", "width": img.size[0], "height": img.size[1], "border": border_px}}}
    (OUT / f"{name}.png.mcmeta").write_text(json.dumps(meta, indent=2) + "\n")


def frame():
    """外框：深色木板，外圈黑边，内圈高光，四角铜钉。"""
    img = wood(32, 32, hexc("#3a2616"), hexc("#2b1d12"), 1)
    border(img, hexc("#1a110a"), hexc("#7a5434"), hexc("#2b1d12"), 2)
    px = img.load()
    for (x, y) in [(3, 3), (28, 3), (3, 28), (28, 28)]:
        px[x, y] = hexc("#c99a56")
        px[x + 1, y] = hexc("#8a6a3a")
        px[x, y + 1] = hexc("#8a6a3a")
    save("frame", img, 6)


def status():
    """状态栏：稍浅的横木。"""
    img = wood(32, 18, hexc("#4a3020"), hexc("#35231a"), 2)
    border(img, hexc("#2b1d12"), hexc("#7a5434"), hexc("#35231a"), 2)
    save("status", img, 3)


def board(name, base, edge, seed):
    """木牌：横纹木板，上缘两颗钉子。"""
    img = wood(64, 24, hexc(base), shade(hexc(base), -22), seed)
    border(img, hexc("#1f140c"), hexc(edge), shade(hexc(base), -28), 2)
    px = img.load()
    for x in (6, 57):
        px[x, 4] = hexc("#d8c08a")
        px[x + 1, 4] = hexc("#8a6a3a")
        px[x, 5] = hexc("#6e4b2c")
    save(name, img, 4)


def parchment():
    """羊皮纸：米黄底、细噪点、边缘发暗。"""
    rnd = random.Random(7)
    w = h = 32
    base = hexc("#efe3c4")
    img = Image.new("RGBA", (w, h), base)
    px = img.load()
    for y in range(h):
        for x in range(w):
            px[x, y] = shade(base, rnd.choice([0, 0, 0, -3, -5, 2]))
    border(img, hexc("#8a6a3a"), hexc("#d9c69a"), hexc("#c4ab78"), 2)
    save("parchment", img, 4)


def inset():
    """内嵌区（地图底、详情栏）：比羊皮纸深一档。"""
    rnd = random.Random(11)
    base = hexc("#e3d3a8")
    img = Image.new("RGBA", (16, 16), base)
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = shade(base, rnd.choice([0, 0, -2, -4, 2]))
    border(img, hexc("#b89b68"), hexc("#b89b68"), hexc("#b89b68"), 1)
    save("inset", img, 2)


def button(name, base, edge, seed):
    img = wood(32, 14, hexc(base), shade(hexc(base), -18), seed)
    border(img, hexc("#2b1d12"), hexc(edge), shade(hexc(base), -30), 2)
    save(name, img, 3)


if __name__ == "__main__":
    frame()
    status()
    board("board", "#5b3a22", "#6e4b2c", 3)
    board("board_selected", "#8a5a2e", "#c99a56", 4)
    board("board_hover", "#684329", "#8a6440", 5)
    parchment()
    inset()
    button("button", "#7a4a1c", "#b07a3e", 6)
    button("button_hover", "#8f5a26", "#d0a060", 8)
    button("button_disabled", "#6b5a48", "#857260", 9)
    print("写到", OUT)

