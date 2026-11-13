#!/usr/bin/env python3
"""导出希腊村庄的正式模板与建筑数据（T13b 远古生成的建筑；T19 阶段 ③ 古典 / 英雄 / 神话的施工建筑）。

门的位置与朝向写在建筑数据的 door 里，规划器按它旋转，让门朝向道路 / 广场；施工时按它让门朝向地块正面。
施工建筑（generated = false）另有：priority（越小越先建）、unique（每村一座）、replace（原地替换的建筑 id 前缀）、
god（次神祠；"*" 是通用祠）、jobs（岗位：职业 → 人数，T19 §1.6 H1）、requires（anchor:<锚点> / building:<建筑>）、patron（主神变体）、buildCosts（本建筑的折算规则，
排在文明折算表前面）。T19 设计文档 §1.4。
运行：python3 tools/buildings/export_village.py [短名 ...]（不带参数导出全部）
"""
from __future__ import annotations

from buildgen import ROOT, export_template
import sys

import greek_acropolis
import greek_classical
import greek_core
import greek_farm
import greek_heroic
import greek_patron
import greek_terrain
import greek_upgrade


def meta(category, plot, door, facing, terrain=("flat", "slope"), provides=None, stage="archaic", generated=True, door_width=1,
         **extra):
    return {"category": category, "stage": stage, "generated": generated, "plot_kinds": [plot],
            "terrain": list(terrain), "provides": provides or {}, **extra,
            "door": {"pos": door, "facing": facing, "width": door_width}}


def built(category, plot, door, facing, stage, priority, provides=None, terrain=("flat", "slope"), **extra):
    """施工建筑（T19 阶段 ③）：不在生成时出现，由村庄自己备料施工。"""
    return meta(category, plot, door, facing, terrain, provides, stage, False, priority=priority, **extra)


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
    (greek_core.granary, "granary", meta("basic", "storage", (8, 0, 3), "east", ("flat",), {"storage": 1}, door_width=2)),
    (greek_farm.sheep_pen, "pen", meta("basic", "pen", (4, 0, 8), "south")),
    (greek_farm.olive_grove, "olive_grove", meta("profession", "grove", (7, 0, 14), "south", jobs={"greek:farmer": 2})),
    (greek_farm.vineyard, "vineyard", meta("profession", "grove", (5, 0, 12), "south", jobs={"greek:farmer": 1})),
    (greek_farm.wheat_field, "wheat_field", meta("profession", "field", (7, 0, 10), "south", ("flat",),
                                                 jobs={"greek:farmer": 1})),
    (greek_farm.pottery, "pottery", meta("profession", "workshop", (4, 0, 8), "south", jobs={"greek:potter": 2})),
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


# 石作坊院里的半成品柱段：这时村里还没有石作台，柱段只能从原石粗凿，按原石算（否则古典第一座石作坊永远备不齐料）
RAW_COLUMNS = [{"match": "mythic_age:marble_column*", "cost": {"mythic_age:marble": 0.25}},
               {"match": "*_capital", "cost": {"mythic_age:marble": 0.5}}]

# 施工建筑（T19 阶段 ③；V3-希腊-建筑 白模篇第 4、5 批，规划篇 §2、§3）。优先级：古典先石作坊（石作台是其余建筑的方石来源），
# 再大仓库 → 内院宅 → 集市 → 中神庙 → 葡萄酒坊；英雄：训练场 → 青铜工坊 → 大神庙 → 剧场 → 体育场；神话：大祭坛 → 锻炉 → 神谕所。
TEMPLE = dict(provides={"guardian": 1})
BUILT = [
    (greek_classical.stoneworks, "stoneworks",
     built("profession", "workshop", (4, 0, 8), "south", "classical", 10, buildCosts=RAW_COLUMNS,
           jobs={"greek:mason": 2})),
    (greek_classical.storehouse, "storehouse", built("basic", "storage", (10, 0, 4), "east", "classical", 20,
                                                     {"storage": 2}, ("flat",))),
    (greek_classical.house_court, "house_court", built("basic", "house", (5, 0, 10), "south", "classical", 30,
                                                       {"beds": 4}, unique=False)),
    # 原地升级（GA-11 D1）：同占地加一层；谷仓排在大仓库之后、住居之前
    (greek_upgrade.granary_ii, "granary_ii", built("basic", "storage", (8, 0, 3), "east", "classical", 22, {"storage": 2},
                                                   ("flat",), unique=False, replace=["greek:granary"], door_width=2)),
    (greek_upgrade.house_small_ii, "house_small_ii", built("basic", "house", (3, 0, 8), "south", "classical", 25,
                                                           {"beds": 4}, unique=False, replace=["greek:house_small"])),
    (greek_classical.stoa, "stoa", built("stage", "market", (10, 0, 6), "south", "classical", 40, {"merchant": 4},
                                         ("flat",), jobs={"greek:merchant": 2})),
    (greek_classical.winery, "winery", built("feature", "workshop", (4, 0, 8), "south", "classical", 60, None, ("flat",),
                                             requires=["anchor:trellis"], jobs={"greek:farmer": 1})),
    (lambda: greek_classical.minor_shrine("athena"), "shrine_athena",
     built("stage", "shrine", (10, 0, 5), "east", "classical", 0, god="athena")),
    (lambda: greek_classical.minor_shrine("hermes"), "shrine_hermes",
     built("stage", "shrine", (10, 0, 5), "east", "classical", 0, god="hermes")),
    (lambda: greek_classical.minor_shrine("ares"), "shrine_ares",
     built("stage", "shrine", (10, 0, 5), "east", "classical", 0, god="ares")),
    (lambda: greek_classical.minor_shrine("generic"), "shrine",
     built("stage", "shrine", (10, 0, 5), "east", "classical", 0, god="*")),
    (greek_heroic.palaestra, "palaestra", built("stage", "training", (7, 0, 14), "south", "heroic", 10,
                                                {"training": 1}, ("flat",))),
    (greek_heroic.bronze_workshop, "bronze_workshop", built("profession", "workshop", (4, 0, 10), "south", "heroic", 20,
                                                            jobs={"greek:bronzesmith": 2})),
    (greek_heroic.theater, "theater", built("feature", "theater", (10, 0, 16), "south", "heroic", 50, {"festival": 1},
                                            ("slope",))),
    (greek_heroic.gymnasion, "gymnasion", built("feature", "gymnasion", (12, 0, 10), "south", "heroic", 60,
                                                {"training": 1}, ("flat",), requires=["building:greek:palaestra"])),
    (greek_heroic.great_altar, "great_altar", built("stage", "altar", (12, 0, 6), "east", "mythic", 10, {"flame": 1},
                                                    ("flat",))),
    (greek_heroic.forge, "forge", built("profession", "workshop", (8, 0, 4), "east", "mythic", 20,
                                        jobs={"greek:bronzesmith": 1})),
    (greek_heroic.oracle, "oracle", built("feature", "feature", (10, 0, 5), "east", "mythic", 30, {"oracle": 1})),
]
# 神庙原地扩建：古典中神庙替换远古小神庙，英雄大神庙替换中神庙；四种主神变体（同 T13b 远古卫城）
for _stage, _old, _prio in (("classical", "acropolis_archaic", 50), ("heroic", "acropolis_classical", 40)):
    for _patron in (None, "zeus", "poseidon", "hades"):
        _short = f"acropolis_{_stage}" + (f"_{_patron}" if _patron else "")
        _extra = {"replace": [f"greek:{_old}"]}
        if _patron:
            _extra["patron"] = _patron
        BUILT.append(((lambda st=_stage, pa=_patron: greek_acropolis.build(st, pa)), _short,
                      built("core", "temple_ext", (29, 0, 7), "east", _stage, _prio, **TEMPLE, **_extra)))


def main():
    only = set(sys.argv[1:])
    for build, short, m in GREEK + BUILT:
        if only and short not in only:
            continue
        b = build()
        tpl, js = export_template(b, short, m)
        print(f"{short}: {len(b.blocks)} 方块")


if __name__ == "__main__":
    main()
