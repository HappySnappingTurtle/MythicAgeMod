#!/usr/bin/env python3
"""希腊村庄第 4 批：柏树与月桂（T13b 阶段 ③，规划篇 §4.2 补充表）——树叶、树苗贴图与原木换色规格。

  - 柏树 cypress：地中海柏树（Cupressus sempervirens，柱形 'Stricta'），细高的绿柱，枝条向上贴着主干；
    鳞片状小叶密集、偏蓝的深绿；树皮灰褐、纵向开裂。
  - 月桂 laurel：月桂（Laurus nobilis），常绿灌木或小乔木，圆冠；叶片长椭圆、革质有光泽、深绿，中脉色浅；
    树皮光滑、灰色。阿波罗的圣树，德尔斐女祭司嚼月桂叶，皮提亚竞技会的奖品是月桂冠。
出处见 V3-希腊-建筑（逐层图与白模篇）新方块第 4 批。

运行：python3 tools/models/greek_trees.py && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/*.json
"""
from __future__ import annotations

import json
import math

from modelgen import SPECS, grid, noise, rows, spec

CYPRESS = {"0": "#172a1f", "1": "#203828", "2": "#2a4631", "3": "#35553b", "4": "#426646", "5": "#567a55"}
LAUREL = {"0": "#1a2c17", "1": "#243b1f", "2": "#2f4b27", "3": "#3b5d30", "4": "#4a703a", "5": "#6a8a52",
          "6": "#93ad74"}


def cypress_leaves():
    """柏叶：一簇簇向上的鳞叶小枝（竖向短条纹，上亮下暗），很密，只留少量透光的小孔。"""
    g = grid()
    for y in range(16):
        for x in range(16):
            # 每 3 列一束向上的小枝，束内上端亮、下端暗
            col = (x + int(3 * noise(x // 3, 0, 11))) % 3
            ph = (y + int(5 * noise(x // 3, 1, 12))) % 5
            v = 0.5 - 0.05 * ph + (0.2 if col == 1 else (-0.12 if col == 0 else 0)) + 0.2 * (noise(x, y, 13) - 0.5)
            g[y][x] = str(max(0, min(5, int(v * 6))))
            if noise(x, y, 14) > 0.93:
                g[y][x] = "."
    return g


def laurel_leaves():
    """月桂叶：长椭圆的革质叶片朝各个方向交叠，向光一侧亮、中脉浅色；叶片之间的暗处偶有透光孔。"""
    g = grid(fill="1")
    for y in range(16):
        for x in range(16):
            if noise(x, y, 21) > 0.82:
                g[y][x] = "0"
            if noise(x, y, 22) > 0.95:
                g[y][x] = "."
    leaves = []
    for i in range(13):
        cx, cy = 16 * noise(i, 0, 23), 16 * noise(i, 1, 24)
        ang = math.pi * (noise(i, 2, 25) - 0.5) * 1.6 - math.pi / 4
        leaves.append((cx, cy, ang))
    for (cx, cy, ang) in leaves:
        ca, sa = math.cos(ang), math.sin(ang)
        for y in range(16):
            for x in range(16):
                for (ox, oy) in ((0, 0), (16, 0), (-16, 0), (0, 16), (0, -16)):
                    dx, dy = x - cx + ox, y - cy + oy
                    u, v = dx * ca + dy * sa, -dx * sa + dy * ca
                    if (u / 3.6) ** 2 + (v / 1.4) ** 2 <= 1:
                        if abs(v) < 0.5 and abs(u) < 3:
                            g[y][x] = "5"                       # 中脉
                        elif v < 0:
                            g[y][x] = "4" if (u / 3.6) ** 2 + (v / 1.4) ** 2 < 0.35 else "3"
                        else:
                            g[y][x] = "2"
                        if v < -0.6 and abs(u) < 0.8 and noise(x, y, 26) > 0.4:
                            g[y][x] = "6"                       # 革质叶面的反光
    return g


def cypress_sapling():
    return ["................",
            ".......2........",
            "......234.......",
            "......243.......",
            ".....23542......",
            "......2443......",
            ".....2354.......",
            "......2342......",
            ".....23543......",
            "......2442......",
            ".......b........",
            ".......b........",
            ".......B........",
            ".......b........",
            ".......B........",
            ".......b........"]


def laurel_sapling():
    return ["................",
            "................",
            "....34.....43...",
            "...3554...4553..",
            "....33.b.b.33...",
            "..43...b.b......",
            ".3554..bb...34..",
            "..33..bB...3553.",
            "...b..b..b..33..",
            "....b.b.b.......",
            ".....bBb...43...",
            "......bB..3553..",
            ".......b.b.33...",
            ".......Bb.......",
            ".......b........",
            ".......B........"]


RECOLOR = {
    "cypress_log": ("minecraft:block/spruce_log", ["#2e2620", "#463a31", "#5f5045", "#78695c", "#928373"],
                    "柏树原木侧面：云杉原木的纵向沟纹，换成灰褐、纵向开裂的老柏树皮"),
    "cypress_log_top": ("minecraft:block/spruce_log_top", ["#5a3b25", "#7a5434", "#9b6e45", "#b88a5c", "#cfa577"],
                        "柏树原木顶面：偏红的芯材"),
    "laurel_log": ("minecraft:block/birch_log", ["#2f2e29", "#46443d", "#5e5c53", "#76736a", "#8c897e"],
                   "月桂原木侧面：白桦原木的光滑树皮与横向皮孔，换成光滑的灰色树皮"),
    "laurel_log_top": ("minecraft:block/birch_log_top", ["#6e4f36", "#8a6648", "#a5805e", "#bd9a77", "#d2b391"],
                       "月桂原木顶面：偏粉的浅褐木心"),
}


def main():
    spec("cypress_leaves", rows(cypress_leaves()), CYPRESS)
    spec("laurel_leaves", rows(laurel_leaves()), LAUREL)
    spec("cypress_sapling", cypress_sapling(), {**CYPRESS, "b": "#4a3a2c", "B": "#66523f"})
    spec("laurel_sapling", laurel_sapling(), {**LAUREL, "b": "#5a5448", "B": "#787266"})
    for rid, (base, ramp, note) in RECOLOR.items():
        (SPECS / f"{rid}.json").write_text(json.dumps({"template": "recolor", "id": rid, "base": base, "ramp": ramp,
                                                       "note": note}, ensure_ascii=False, indent=2) + "\n",
                                           encoding="utf-8")
    print("第 4 批：cypress_{leaves,sapling,log,log_top}、laurel_{leaves,sapling,log,log_top}")


if __name__ == "__main__":
    main()
