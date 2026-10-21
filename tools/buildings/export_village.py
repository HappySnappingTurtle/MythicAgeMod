#!/usr/bin/env python3
"""导出希腊村庄的正式模板与建筑数据（T13b）。

门的位置与朝向写在建筑数据的 door 里，规划器按它旋转，让门朝向道路 / 广场。
运行：python3 tools/buildings/export_village.py
"""
from __future__ import annotations

from buildgen import ROOT, export_template
import greek_acropolis
import greek_core
import greek_farm
import greek_patron
import greek_terrain


def meta(category, plot, door, facing, terrain=("flat", "slope"), provides=None, stage="archaic", generated=True):
    return {"category": category, "stage": stage, "generated": generated, "plot_kinds": [plot],
            "terrain": list(terrain), "provides": provides or {}, "door": {"pos": door, "facing": facing}}


# 远古生成的建筑 + 主神差异件 + 地形片段（V3-希腊-建筑 §2、§5、§6；白模篇第 1–3、6 批）
GREEK = [
    (lambda: greek_acropolis.build("archaic"), "acropolis_archaic",
     meta("core", "temple", (29, 0, 7), "east", provides={"guardian": 1})),
    (lambda: greek_acropolis.build("archaic", "zeus"), "acropolis_archaic_zeus",
     meta("core", "temple", (29, 0, 7), "east", provides={"guardian": 1})),
    (lambda: greek_acropolis.build("archaic", "poseidon"), "acropolis_archaic_poseidon",
     meta("core", "temple", (29, 0, 7), "east", provides={"guardian": 1})),
    (lambda: greek_acropolis.build("archaic", "hades"), "acropolis_archaic_hades",
     meta("core", "temple", (29, 0, 7), "east", provides={"guardian": 1})),
    (greek_core.agora, "agora", meta("core", "center", (7, 0, 14), "south", ("flat",), {"meeting": 1})),
    (greek_core.krene, "krene", meta("core", "center_side", (6, 0, 2), "east", ("flat",), {"water": 1})),
    (greek_core.house_small, "house_small", meta("basic", "house", (3, 0, 8), "south", provides={"beds": 2})),
    (greek_core.granary, "granary", meta("basic", "storage", (8, 0, 3), "east", ("flat",), {"storage": 1})),
    (greek_farm.sheep_pen, "pen", meta("basic", "pen", (4, 0, 8), "south")),
    (greek_farm.olive_grove, "olive_grove", meta("profession", "grove", (7, 0, 14), "south")),
    (greek_farm.vineyard, "vineyard", meta("profession", "grove", (5, 0, 12), "south")),
    (greek_farm.wheat_field, "wheat_field", meta("profession", "field", (7, 0, 10), "south", ("flat",))),
    (greek_farm.pottery, "pottery", meta("profession", "workshop", (4, 0, 8), "south")),
    (lambda: greek_patron.zeus_oak(False), "zeus_oak_archaic", meta("patron", "patron", (8, 0, 16), "south")),
    (greek_patron.poseidon_spring, "poseidon_spring", meta("patron", "patron", (9, 0, 4), "east", ("flat",))),
    (lambda: greek_patron.poseidon_harbor(natural=True), "poseidon_harbor",
     meta("patron", "dock", (16, 0, 0), "north", ("coast",))),
    (greek_patron.hades_cemetery, "hades_cemetery", meta("patron", "roadside", (10, 0, 1), "north")),
    (lambda: greek_terrain.bridge_narrow(natural=True), "bridge_narrow",
     meta("terrain", "bridge", (0, 0, 4), "west", ("riverbank",))),
    (lambda: greek_terrain.bridge_wide(natural=True), "bridge_wide",
     meta("terrain", "bridge", (0, 0, 6), "west", ("riverbank",))),
]


def main():
    for build, short, m in GREEK:
        b = build()
        tpl, js = export_template(b, short, m)
        print(f"{short}: {len(b.blocks)} 方块")


if __name__ == "__main__":
    main()
