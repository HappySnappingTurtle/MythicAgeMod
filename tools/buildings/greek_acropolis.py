#!/usr/bin/env python3
"""希腊村庄第 1 批白模：卫城（V3-希腊-建筑 §2.1、§3.2）。

神庙地块按大神庙预留 25 × 15（x 东西 × z 南北），门朝东（+x）；露天祭坛在地块东侧正门前；祭司居所在神庙北侧。
三个阶段三种神庙，原地扩建（同一地块、同一中心线 z = 7）：
  远古  小神庙（naiskos）：1 级台基，前廊 4 根多立克柱，内殿一间 + 圣火盆。
  古典  中神庙（前后柱式 amphiprostyle）：2 级台基，前后各 4 根爱奥尼柱（共 8），内殿神像 + 2 个圣火盆。
  英雄  大神庙（周柱式 peripteros）：2 级台基，6 × 11 根科林斯柱（共 30），内殿神像 + 4 个圣火盆。
各级都满足 T8a 的等级条件（柱数、柱高 ≥ 3、有顶、圣火盆、三角楣、大理石地面、台基）。

运行：python3 tools/buildings/greek_acropolis.py [--world <存档目录>]
"""
from __future__ import annotations

import sys
from pathlib import Path

from buildgen import Building, ROOT, write_datapack

OUT = ROOT / "docs/重构/开发/TV/白模/希腊/第1批_卫城"
CENTER_Z = 7


def entablature(b: Building, x0, x1, z0, z1, y):
    """额枋（y）、三陇板饰带（y+1）沿柱列外圈；檐口（y+2）向外挑 1 格；内部 y 层平顶天花。"""
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            b.set(x, y, z, "architrave", facing="north" if z == z0 else "south")
            b.set(x, y + 1, z, "frieze", facing="north" if z == z0 else "south")
    for z in range(z0 + 1, z1):
        for x in (x0, x1):
            b.set(x, y, z, "architrave", facing="west" if x == x0 else "east")
            b.set(x, y + 1, z, "frieze", facing="west" if x == x0 else "east")
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if (x, y, z) not in b.blocks:
                b.set(x, y, z, "smooth_slab", type="top")
    for x in range(x0 - 1, x1 + 2):
        b.set(x, y + 2, z0 - 1, "cornice", facing="north")
        b.set(x, y + 2, z1 + 1, "cornice", facing="south")
    for z in range(z0, z1 + 1):
        b.set(x0 - 1, y + 2, z, "cornice", facing="west")
        b.set(x1 + 1, y + 2, z, "cornice", facing="east")
    # 檐口之上整片封顶，屋面再在上面起坡
    b.fill(x0, y + 2, z0, x1, y + 2, z1, "floor")
    return y + 3


def roof(b: Building, x0, x1, z0, z1, y):
    """两坡屋顶 + 东西两端山花三角楣；屋檐两侧一排瓦当。"""
    b.gable_roof(x0, x1, z0, z1, y)
    b.pediment_gable(x1 + 1, z0, z1, y, "east")
    b.pediment_gable(x0 - 1, z0, z1, y, "west")
    for x in range(x0, x1 + 1, 2):
        b.set(x, y, z0 - 1, "antefix", facing="north")
        b.set(x, y, z1 + 1, "antefix", facing="south")


def cella(b: Building, x0, x1, z0, z1, y0, y1, door_half_width, door_height):
    b.hollow(x0, y0, z0, x1, y1, z1, "wall_marble")
    # 内墙顶下一圈回纹饰带（爱琴蓝）
    if y1 - y0 >= 4:
        for x in range(x0 + 1, x1):
            b.set(x, y1, z0, "meander_blue")
            b.set(x, y1, z1, "meander_blue")
    for z in range(CENTER_Z - door_half_width, CENTER_Z + door_half_width + 1):
        for y in range(y0, y0 + door_height):
            b.blocks[(x1, y, z)] = "minecraft:air"
    b.anchor("guardian", x1 - 2, y0, CENTER_Z)


def small_temple(b: Building):
    top = b.crepidoma(9, 3, 19, 11, 1)  # 台面 y = 1
    cella(b, 10, 15, 4, 10, top, top + 3, 0, 3)
    for z in (4, 6, 8, 10):
        b.column(18, top, z, 2, "doric")  # 柱高 4
    b.set(12, top, CENTER_Z, "brazier", lit="true")
    b.set(11, top, 5, "lamp")
    b.set(11, top, 9, "lamp")
    y = entablature(b, 10, 18, 4, 10, top + 4)
    roof(b, 10, 18, 3, 11, y)


def medium_temple(b: Building):
    top = b.crepidoma(5, 2, 23, 12, 2)  # 台面 y = 2，台面 x 6..22、z 3..11
    cella(b, 8, 20, 4, 10, top, top + 4, 1, 4)
    for x in (6, 22):
        for z in (4, 6, 8, 10):
            b.column(x, top, z, 3, "ionic", axis="z")  # 柱高 5
    b.set(10, top, CENTER_Z, "statue", half="lower", facing="east")
    b.set(10, top + 1, CENTER_Z, "statue", half="upper", facing="east")
    for z in (5, 9):
        b.set(18, top, z, "brazier", lit="true")
    y = entablature(b, 6, 22, 4, 10, top + 5)
    roof(b, 6, 22, 3, 11, y)


def large_temple(b: Building):
    top = b.crepidoma(0, 0, 24, 14, 2)  # 台面 y = 2，台面 x 1..23、z 1..13
    cella(b, 5, 19, 4, 10, top, top + 5, 1, 5)
    for x in range(2, 23, 2):
        for z in (2, 12):
            b.column(x, top, z, 4, "corinthian")  # 柱高 6
    for z in range(4, 11, 2):
        for x in (2, 22):
            b.column(x, top, z, 4, "corinthian")
    b.set(7, top, CENTER_Z, "statue", half="lower", facing="east")
    b.set(7, top + 1, CENTER_Z, "statue", half="upper", facing="east")
    # 内殿两列内柱（帕特农式），柱间是通向神像的中央通道
    for x in range(9, 18, 2):
        for z in (5, 9):
            b.column(x, top, z, 3, "doric")
    for x in (8, 18):
        for z in (6, 8):
            b.set(x, top, z, "brazier", lit="true")
    y = entablature(b, 2, 22, 2, 12, top + 6)
    roof(b, 2, 22, 1, 13, y)


def altar(b: Building):
    """露天祭坛：地块东侧、正门前；装饰性（不是玩家的神坛方块）。"""
    b.fill(28, 0, 6, 28, 0, 8, "floor")
    b.set(28, 1, 6, "smooth_slab", type="bottom")
    b.set(28, 1, 7, "mythic_age:village_altar", facing="east")
    b.set(28, 1, 8, "smooth_slab", type="bottom")
    b.anchor("ritual", 29, 0, CENTER_Z)


def priest_house(b: Building, ox=14, oz=-10):
    """祭司居所 7 × 7：赭色勒脚、白灰墙、橄榄木梁、两坡赤陶瓦顶；门朝南（对着神庙）。"""
    x0, x1, z0, z1 = ox, ox + 6, oz, oz + 6
    b.hollow(x0, 0, z0, x1, 0, z1, "socle")
    b.hollow(x0, 1, z0, x1, 2, z1, "plaster")
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            b.set(x, 3, z, "beam", axis="x")
    for z in range(z0 + 1, z1):
        for x in (x0, x1):
            b.set(x, 3, z, "beam", axis="z")
    b.fill(x0 + 1, 3, z0 + 1, x1 - 1, 3, z1 - 1, "plank_slab", type="top")
    b.hip_roof(x0 - 1, x1 + 1, z0 - 1, z1 + 1, 4)
    door_x = x0 + 3
    b.blocks[(door_x, 0, z1)] = "minecraft:air"
    b.set(door_x, 0, z1, "door", facing="north", half="lower", hinge="left", open="false")
    b.set(door_x, 1, z1, "door", facing="north", half="upper", hinge="left", open="false")
    for x in (x0, x1):
        b.set(x, 2, z0 + 3, "shutter", facing="west" if x == x0 else "east", half="top", open="true")
    b.set(x0 + 1, 0, z0 + 1, "bed", facing="south", part="head")
    b.set(x0 + 1, 0, z0 + 2, "bed", facing="south", part="foot")
    b.set(x1 - 1, 0, z0 + 1, "pithos")
    b.set(x1 - 1, 0, z0 + 2, "pithos")
    b.set(x1 - 1, 0, z1 - 1, "pithos")
    b.set(x1 - 1, 1, z1 - 1, "lamp")
    b.anchor("priest_home", x0 + 1, 0, z0 + 2)  # 祭司床位（不生成普通居民）


PATRON_ORNAMENT = {"zeus": "pediment_thunderbolt", "poseidon": "pediment_trident"}


# 各阶段神庙台基最下一级的范围（small / medium / large_temple 里 crepidoma 的参数）
CREPIDOMA = {"archaic": (9, 3, 19, 11), "classical": (5, 2, 23, 12), "heroic": (0, 0, 24, 14), "mythic": (0, 0, 24, 14)}


def hades_accents(b: Building, stage: str):
    """哈迪斯：神庙台基最下一级的外圈换成磨制黑石砖（规划篇 §5「黑石点缀」）。"""
    x0, z0, x1, z1 = CREPIDOMA[stage]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                b.set(x, 0, z, "minecraft:polished_blackstone_bricks")


def build(stage: str, patron: str | None = None) -> Building:
    titles = {"archaic": "卫城·远古（小神庙）", "classical": "卫城·古典（中神庙）", "heroic": "卫城·英雄（大神庙）",
              "mythic": "卫城·神话（大神庙 + 大祭坛）"}
    b = Building(f"greek_acropolis_{stage}" + (f"_{patron}" if patron else ""), titles[stage])
    {"archaic": small_temple, "classical": medium_temple, "heroic": large_temple, "mythic": large_temple}[stage](b)
    if patron in PATRON_ORNAMENT:
        b.pediment_ornaments(PATRON_ORNAMENT[patron])
    elif patron == "hades":
        hades_accents(b, stage)
    if stage == "mythic":
        # 神话阶段：正门前的露天祭坛换成帕加马式大祭坛（台阶朝东，封闭的一面对着神庙正门）
        from greek_heroic import great_altar_into
        great_altar_into(b, ox=28, oz=1)
    else:
        altar(b)
    priest_house(b)
    return b


def main():
    buildings = [build(s) for s in ("archaic", "classical", "heroic", "mythic")]
    OUT.mkdir(parents=True, exist_ok=True)
    for b in buildings:
        p = b.layer_plans(OUT / f"{b.name}_逐层.png", cell=14)
        print(f"{b.title}：{len(b.blocks)} 方块，平面图 {p.relative_to(ROOT)}")
    if "--world" in sys.argv:
        world = Path(sys.argv[sys.argv.index("--world") + 1])
        print("数据包：", write_datapack(world, buildings))


if __name__ == "__main__":
    main()
