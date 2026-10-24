#!/usr/bin/env python3
"""T17 阶段 ② 青铜重装步兵甲的护甲贴图（64 × 64，配合 BronzeArmorModel 的 3D 部件）。

上半部（64 × 32）沿用原版护甲的 UV 布局：以原版铁甲贴图映射到青铜色阶为底，再画细节；下半部放 3D 部件。
- 头盔：科林斯式闭面盔，T 形开口（两侧眼口 + 护鼻条 + 下方口缝）透明，露出脸（10 号合同：脸部窗口保持可见）
- 马鬃冠（横向）：红褐，深色竖纹
- 胸甲：两道胸肌浅浮雕、腹部中线；肩上深棕皮带
- 垂条 pteruges：米白亚麻与皮革条交替，上沿一道皮腰带
- 护腿：躯干下沿与大腿上段是米白短裙甲，小腿前、侧是青铜胫甲（另有 3D 胫甲板）
- 靴：交叉绑带皮凉鞋（只画在脚面、脚踝），踝部 3D 小护片
运行：python3 tools/models/greek_armor.py <解压后的 minecraft 资源目录>
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from greek_equipment import BRONZE, TEX, ramp  # noqa: E402

DARK = (26, 18, 10, 255)
CLEAR = (0, 0, 0, 0)
HI = (240, 200, 120, 255)
MID = (176, 122, 54, 255)
SHADE = (122, 78, 32, 255)
LEATHER = (90, 58, 36, 255)
LEATHER_D = (66, 42, 26, 255)
LINEN = (216, 207, 180, 255)
LINEN_D = (184, 172, 140, 255)
HAIR = (142, 58, 36, 255)
HAIR_D = (106, 40, 24, 255)
HAIR_L = (176, 88, 58, 255)


def base(src: Path) -> Image.Image:
    im = Image.open(src).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (*ramp((0.299 * r + 0.587 * g + 0.114 * b) / 255, BRONZE), a)
    out = Image.new("RGBA", (64, 64), CLEAR)
    out.paste(im, (0, 0))
    return out


def fill(im, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            im.putpixel((x, y), c)


def cube(im, u, v, w, h, d, top, side, front=None):
    """按原版方块 UV 布局给一个 w × h × d 的盒子上色（top 是上下面，side 是四个侧面）。"""
    fill(im, u + d, v, u + d + w + w, v + d, top)
    fill(im, u, v + d, u + 2 * (d + w), v + d + h, side)
    if front:
        fill(im, u + d, v + d, u + d + w, v + d + h, front)


def layer1(mc: Path) -> Image.Image:
    im = base(mc / "assets/minecraft/textures/models/armor/iron_layer_1.png")
    # 头盔正面 (8,8)-(16,16)：护鼻条，两侧眼口与下方口缝透明（T 形开口）
    fill(im, 8, 8, 16, 16, MID)
    fill(im, 8, 8, 16, 9, HI)
    for x, y in [(9, 11), (10, 11), (13, 11), (14, 11), (11, 13), (12, 13), (11, 14), (12, 14), (11, 15), (12, 15)]:
        im.putpixel((x, y), CLEAR)
    fill(im, 11, 9, 13, 13, HI)
    for y in range(12, 16):  # 颊甲内缘的暗线
        im.putpixel((10, y), SHADE)
        im.putpixel((13, y), SHADE)
    # 胸甲正面 (20,20)-(28,32)：胸肌两道浅浮雕、下沿阴影、腹部中线
    fill(im, 20, 20, 28, 32, MID)
    for x in (21, 22, 23):
        im.putpixel((x, 22), HI)
    for x in (24, 25, 26):
        im.putpixel((x, 22), HI)
    fill(im, 21, 24, 27, 25, SHADE)
    for y in range(25, 31):
        im.putpixel((24, y), SHADE)
    fill(im, 20, 31, 28, 32, SHADE)
    # 肩带：躯干顶面两侧与上臂顶面
    fill(im, 20, 16, 22, 20, LEATHER)
    fill(im, 26, 16, 28, 20, LEATHER)
    fill(im, 44, 16, 48, 20, LEATHER)
    fill(im, 44, 20, 48, 22, LEATHER_D)
    # 靴：腿部只留脚踝以下的绑带（腿的四个侧面 x 0–16，y 20–32）
    fill(im, 0, 16, 16, 32, CLEAR)
    for x in range(0, 16):
        im.putpixel((x, 31), LEATHER_D)  # 鞋底一圈
        im.putpixel((x, 26), LEATHER)  # 踝上一道
    for x in range(0, 16):  # 交叉绑带
        for y in (27, 28, 29, 30):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                im.putpixel((x, y), LEATHER)
    fill(im, 8, 16, 12, 20, LEATHER_D)  # 脚底
    # ── 下半部：3D 部件 ──
    cube(im, 0, 32, 11, 3, 2, HAIR_L, HAIR)  # 马鬃冠
    for x in range(0, 26, 2):
        for y in range(34, 37):
            im.putpixel((x, y), HAIR_D)
    cube(im, 28, 32, 2, 1, 3, SHADE, SHADE)  # 冠座
    cube(im, 40, 32, 3, 2, 1, HI, MID)  # 踝部护片
    cube(im, 0, 40, 8, 5, 4, LEATHER_D, LINEN)  # 垂条
    for x in range(0, 24):
        im.putpixel((x, 44), LEATHER_D)  # 上沿皮腰带
        for y in range(45, 49):
            im.putpixel((x, y), LINEN if x % 2 == 0 else LEATHER)
        im.putpixel((x, 48), LINEN_D if x % 2 == 0 else LEATHER_D)
    return im


def layer2(mc: Path) -> Image.Image:
    im = base(mc / "assets/minecraft/textures/models/armor/iron_layer_2.png")
    # 躯干（护腿件的腰部）：下沿米白短裙甲
    fill(im, 16, 16, 40, 32, CLEAR)
    fill(im, 16, 28, 40, 32, LINEN)
    fill(im, 16, 28, 40, 29, LEATHER_D)
    # 腿：大腿上段短裙、小腿前与两侧青铜胫甲、后面不包
    fill(im, 0, 16, 16, 32, CLEAR)
    fill(im, 0, 20, 16, 23, LINEN)
    for x in range(0, 16, 2):
        im.putpixel((x, 22), LINEN_D)
    fill(im, 0, 25, 12, 31, MID)
    fill(im, 4, 25, 8, 26, HI)  # 胫甲上缘高光（正面）
    fill(im, 0, 30, 12, 31, SHADE)
    # ── 3D 胫甲板（texOffs 48,32；4 × 6 × 1）──
    cube(im, 48, 32, 4, 6, 1, HI, MID)
    for y in range(33, 39):
        im.putpixel((50, y), HI)  # 中脊
    return im


def main(mc: Path) -> None:
    out = TEX / "models/armor"
    out.mkdir(parents=True, exist_ok=True)
    layer1(mc).save(out / "bronze_layer_1.png")
    layer2(mc).save(out / "bronze_layer_2.png")
    print("ok")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
