#!/usr/bin/env python3
"""希腊住居、谷仓的二级模板（GA-11 §2.4，D1）：同占地、加一层，原地升级远古的坡地小屋与谷仓。

两份都从一级模板出发、坐标系相同（frame 一致），一层的床、陶瓮、管事位不动，升级时居民的床位锚点不变。
  两层小屋 house_small_ii：屋身加二层（女眷在上、男人在下，见 Lysias 1.9–10「我的小屋是两层的，楼上与楼下一样大」），
      炉灶间东墙边一跑直梯（Olynthus 民居常见楼梯石基，Robinson & Graham 1938《Excavations at Olynthus VIII》），
      楼上两张床，床位 2 → 4。炉灶、陶瓮挪到西边一排给楼梯让位。
  带阁楼的谷仓 granary_ii：墙加高一层做干燥的阁楼存粮（Xenophon《家政论》9.3：「干燥、有顶的屋子存粮」），
      屋里沿南墙一跑直梯上阁楼，阁楼两排陶瓮、高处开通风窗，仓储 1 → 2。

运行：python3 tools/buildings/greek_upgrade.py（出逐层平面图）；正式模板由 export_village.py 导出。
"""
from __future__ import annotations

from pathlib import Path

from buildgen import Building, ROOT
from greek_core import bed, door, house_roof

OUT = ROOT / "docs/重构/开发/GA/白模"

STAIRS = "mythic_age:olive_stairs"


def house_small_ii() -> Building:
    b = Building("greek_house_small_ii", "两层小屋")
    x0, x1, z0, z1 = 0, 6, 0, 5
    b.fill(x0, -1, z0, x1, -1, 8, "step")
    b.hollow(x0, 0, z0, x1, 0, z1, "socle")
    b.hollow(x0, 1, z0, x1, 2, z1, "plaster")
    b.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, "mythic_age:polygonal_marble")
    b.fill(3, 0, z0 + 1, 3, 2, z1 - 1, "plaster")
    b.clear(3, 0, 3, 3, 1, 3)
    # 一层梁圈 + 楼板（二层地面）
    for y in (3, 6):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                b.set(x, y, z, "beam", axis="x")
        for z in range(z0 + 1, z1):
            for x in (x0, x1):
                b.set(x, y, z, "beam", axis="z")
        b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, "plank_slab", type="top")
    # 二层墙：同一层的白灰墙，墙脚压在梁圈上
    b.hollow(x0, 4, z0, x1, 5, z1, "plaster")
    house_roof(b, x0, x1, z0, z1, 7)
    # 楼梯：炉灶间东墙边，自南向北上，四级到二层；楼板在梯子上方开洞
    for z, y in ((4, 0), (3, 1), (2, 2), (1, 3)):
        b.set(5, y, z, STAIRS, facing="north", half="bottom")
    b.clear(5, 3, 2, 5, 3, 3)
    b.clear(5, 4, 1, 5, 5, 3)
    # 楼梯井边的栏杆（二层西侧）
    b.set(4, 4, 2, "fence")
    b.set(4, 4, 3, "fence")
    # 门窗：一层同一级；二层四面各一扇高窗
    b.clear(4, 0, z1, 4, 1, z1)
    door(b, 4, 0, z1, "north")
    b.set(1, 2, z0, "shutter", facing="north", half="top", open="true")
    b.set(x1, 2, 3, "shutter", facing="east", half="top", open="true")
    b.set(2, 5, z0, "shutter", facing="north", half="top", open="true")
    b.set(4, 5, z1, "shutter", facing="south", half="top", open="true")
    b.set(x0, 5, 3, "shutter", facing="west", half="top", open="true")
    b.set(x1, 5, 2, "shutter", facing="east", half="top", open="true")
    # 前院（同一级）
    for x in range(x0, x1 + 1):
        b.set(x, 0, 8, "socle")
        b.set(x, 1, 8, "plaster")
    for z in (6, 7):
        for x in (x0, x1):
            b.set(x, 0, z, "socle")
            b.set(x, 1, z, "plaster")
    b.clear(3, 0, 8, 3, 1, 8)
    # 一层：卧房的床不动（床位锚点不变）；炉灶、陶瓮挪到西边一排给楼梯让位
    bed(b, 1, 0, 1, "north")
    bed(b, 2, 0, 1, "north")
    b.set(4, 0, 1, "minecraft:campfire", lit="true")
    b.set(4, 0, 2, "pithos")
    b.set(4, 1, 2, "lamp")
    b.set(1, 0, 6, "pithos")
    b.set(5, 0, 7, "pithos")
    # 二层：女眷的卧房，两张床、织机边的陶瓮与油灯
    bed(b, 1, 4, 1, "north")
    bed(b, 2, 4, 1, "north")
    b.set(1, 4, 4, "pithos")
    b.set(3, 4, 4, "pithos")
    b.set(3, 5, 4, "lamp")
    b.anchor("home", 1, 0, 2)
    b.anchor("home", 2, 0, 2)
    b.anchor("home", 1, 4, 2)
    b.anchor("home", 2, 4, 2)
    b.anchor("yard_gate", 3, 0, 8)
    return b


def granary_ii() -> Building:
    b = Building("greek_granary_ii", "带阁楼的谷仓")
    x0, x1, z0, z1 = 0, 8, 0, 6
    b.fill(x0, -1, z0, x1, -1, z1, "step")
    b.hollow(x0, 0, z0, x1, 0, z1, "socle")
    b.hollow(x0, 1, z0, x1, 3, z1, "mudbrick")
    b.hollow(x0, 5, z0, x1, 6, z1, "mudbrick")
    b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, "minecraft:air")
    for y in (4, 7):
        for x in range(x0, x1 + 1):
            for z in (z0, z1):
                b.set(x, y, z, "beam", axis="x")
        for z in range(z0 + 1, z1):
            for x in (x0, x1):
                b.set(x, y, z, "beam", axis="z")
        b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, "plank_slab", type="top")
    house_roof(b, x0, x1, z0, z1, 8)
    # 东侧双开门（同一级）
    b.clear(x1, 0, 2, x1, 2, 4)
    door(b, x1, 0, 3, "west", hinge="left")
    door(b, x1, 0, 4, "west", hinge="right")
    b.set(x1, 0, 2, "socle")
    b.set(x1, 1, 2, "mudbrick")
    b.set(x1, 2, 2, "mudbrick")
    for z in (3, 4):
        b.set(x1, 2, z, "beam", axis="z")  # 门楣过梁
    # 一层陶瓮（同一级：货堆显示位）
    for x in range(1, x1):
        for z in (1, z1 - 1):
            b.set(x, 0, z, "pithos")
            b.anchor("goods", x, 0, z)
    b.set(1, 0, 3, "pithos")
    b.set(1, 1, 3, "pithos")
    b.set(1, 2, 3, "lamp")
    for x in (2, 6):
        b.set(x, 3, z0, "shutter", facing="north", half="top", open="true")
        b.set(x, 3, z1, "shutter", facing="south", half="top", open="true")
    # 楼梯：南排陶瓮北边一排，自东向西上，五级到阁楼；阁楼楼板在梯子上方开洞
    for x, y in ((6, 0), (5, 1), (4, 2), (3, 3), (2, 4)):
        b.set(x, y, 4, STAIRS, facing="west", half="bottom")
    b.clear(3, 4, 4, 4, 4, 4)
    b.clear(2, 5, 4, 4, 6, 4)
    b.set(3, 5, 3, "fence")
    b.set(4, 5, 3, "fence")
    b.set(5, 5, 4, "fence")
    b.set(3, 5, 5, "fence")
    b.set(4, 5, 5, "fence")
    # 阁楼：两排陶瓮存粮，四面高处的通风窗
    for x in range(1, x1):
        b.set(x, 5, 1, "pithos")
        b.anchor("goods", x, 5, 1)
    for x in (5, 6, 7):
        b.set(x, 5, 5, "pithos")
        b.anchor("goods", x, 5, 5)
    b.set(1, 5, 5, "pithos")
    b.anchor("goods", 1, 5, 5)
    b.set(7, 6, 1, "pithos")
    b.set(1, 6, 1, "lamp")
    for x in (2, 6):
        b.set(x, 6, z0, "shutter", facing="north", half="top", open="true")
        b.set(x, 6, z1, "shutter", facing="south", half="top", open="true")
    b.set(x0, 6, 3, "shutter", facing="west", half="top", open="true")
    b.anchor("storage_keeper", 6, 0, 3)
    return b


BUILDERS = (house_small_ii, granary_ii)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for f in BUILDERS:
        b = f()
        p = b.layer_plans(OUT / f"{b.name}_逐层.png", cell=18)
        print(f"{b.title}: {len(b.blocks)} 方块 → {p.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
