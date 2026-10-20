#!/usr/bin/env python3
"""导出希腊村庄的正式模板与建筑数据（T13b）。

模板按「门朝南」的约定不强求：门的位置与朝向写在建筑数据的 door 里，规划器按它旋转。
运行：python3 tools/buildings/export_village.py
"""
from __future__ import annotations

from buildgen import ROOT, export_template
import greek_acropolis
import greek_core

# 阶段 ① 生成用的建筑：卫城（远古）、广场、泉屋、坡地小屋、谷仓
GREEK = [
    (lambda: greek_acropolis.build("archaic"), "acropolis_archaic",
     {"category": "core", "stage": "archaic", "generated": True, "plot_kinds": ["temple"],
      "terrain": ["flat", "slope"], "provides": {"guardian": 1},
      "door": {"pos": (29, 0, 7), "facing": "east"}}),
    (greek_core.agora, "agora",
     {"category": "core", "stage": "archaic", "generated": True, "plot_kinds": ["center"], "terrain": ["flat"],
      "provides": {"meeting": 1}, "door": {"pos": (7, 0, 14), "facing": "south"}}),
    (greek_core.krene, "krene",
     {"category": "core", "stage": "archaic", "generated": True, "plot_kinds": ["center_side"], "terrain": ["flat"],
      "provides": {"water": 1}, "door": {"pos": (6, 0, 2), "facing": "east"}}),
    (greek_core.house_small, "house_small",
     {"category": "basic", "stage": "archaic", "generated": True, "plot_kinds": ["house"],
      "terrain": ["flat", "slope"], "provides": {"beds": 2}, "door": {"pos": (3, 0, 8), "facing": "south"}}),
    (greek_core.granary, "granary",
     {"category": "basic", "stage": "archaic", "generated": True, "plot_kinds": ["storage"], "terrain": ["flat"],
      "provides": {"storage": 1}, "door": {"pos": (8, 0, 3), "facing": "east"}}),
]


def main():
    for build, short, meta in GREEK:
        b = build()
        tpl, js = export_template(b, short, meta)
        print(f"{short}: {tpl.relative_to(ROOT)}  {js.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
