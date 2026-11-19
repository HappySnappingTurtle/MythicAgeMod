#!/usr/bin/env python3
"""导出村庄以外的结构模板（T22）：遗迹、迷宫模块、精英巢穴 → structure/<类别>/<文明>/<名>.nbt + mythic_age/structure_spec/<文明>/<名>.json。

运行：python3 tools/buildings/export_structures.py
"""
from __future__ import annotations

from buildgen import export_template
import greek_knossos
import greek_lairs
import greek_ruins

STRUCTURES = [
    (greek_ruins.abandoned_temple, "abandoned_temple", "ruin",
     {"category": "ruin", "stage": "archaic", "generated": True, "plot_kinds": [], "terrain": ["flat", "slope"], "provides": {}}),
]

# 精英巢穴（T25a）：嵌进山体，只写明确给出的格子
STRUCTURES.append((greek_lairs.cyclops_cave, "cyclops_cave", "lair",
                   {"category": "lair", "stage": "classical", "generated": True, "plot_kinds": [], "terrain": ["hillside"],
                    "provides": {}, "keep_terrain": True}))

RUIN_META = {"category": "ruin", "stage": "archaic", "generated": True, "plot_kinds": [], "terrain": ["flat"], "provides": {}}
for _b in greek_knossos.all_buildings():
    STRUCTURES.append((lambda b=_b: b, _b.name, "labyrinth", dict(RUIN_META, category="labyrinth")))

if __name__ == "__main__":
    for build, short, kind, meta in STRUCTURES:
        tpl, js = export_template(build(), short, meta, "greek", kind)
        print(tpl.relative_to(tpl.parents[5]))
