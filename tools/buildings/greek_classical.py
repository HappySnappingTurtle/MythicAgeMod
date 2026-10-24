#!/usr/bin/env python3
"""希腊村庄第 4 批白模：古典阶段建筑（V3-希腊-建筑 §2.2–§2.5）。

坐标同 buildgen：x 向东、z 向南，原点为占地西北角，y = 0 是地面上第一层。
  次神祠 11 × 11（按资料重做，见 minor_shrine 的出处）：围墙圣域 + 双柱门廊小祠 + 东面露天祭坛；
      雅典娜：圣橄榄树与树下小祭坛；赫尔墨斯：门外路口石堆（herma）与方柱界柱；阿瑞斯：战利品纪念柱（tropaion）；门廊挂还愿物。
  柱廊集市 21 × 7：北墙隔成 4 间铺面（柜台、货物），南面 10 根多立克柱的柱廊，两坡顶、两端山花。
  内院宅 11 × 11：房间围着 5 × 5 的露天天井；北翼主屋（炉灶）与宴室（andron，两张卧榻），东西翼卧房（床 4）、南翼门厅与储藏；
                 天井北侧木柱廊（pastas）、井（水槽）与家神小祭坛；环形四坡顶向内外落水。
  石作坊 9 × 9：院子 + 西侧工棚（石作台、切石机）；院里石料堆、半成品柱段；东北角采石料堆（采集点）。
  大仓库 11 × 9：多边形砌石勒脚 + 大理石方石墙 + 木梁 + 四坡顶；双层陶瓮与木桶成排；东侧双开门。
  葡萄酒坊 9 × 9：棚下抬高的踩葡萄池（池里是紫色葡萄浆），出口流向下方接酒的陶瓮；酒瓮成排。

运行：python3 tools/buildings/greek_classical.py [--world <存档目录>]
"""
from __future__ import annotations

import sys
from pathlib import Path

from buildgen import Building, ROOT, write_datapack
from greek_core import bed, door

OUT = ROOT / "docs/重构/开发/TV/白模/希腊/第4批_古典建筑"


def item_frame(b: Building, x, y, z, facing: str, item: str):
    """还愿物：挂在墙上的物品展示框（盾牌、器物）；坐标是框所在的空格，facing 为框朝外的方向。"""
    code = {"north": 2, "south": 3, "west": 4, "east": 5}[facing]
    b.commands.append(f'summon item_frame ~{x + 0.5} ~{y + 0.5} ~{z + 0.5} '
                      f'{{Facing:{code}b,Fixed:1b,Item:{{id:"minecraft:{item}",count:1}}}}')


def minor_shrine(god: str) -> Building:
    """次神祠：围墙圣域（temenos + peribolos）+ 双柱门廊小祠（distyle in antis）+ 东面露天祭坛 + 各神的圣物。

    出处（V3-希腊-建筑 白模篇第 4 批「出处」表）：双柱门廊——Wikipedia「Distyle in antis」「Naiskos」；
    圣域围墙、祭坛朝东、门口界石——Wikipedia「Peribolos」、Wiley《Greek Religion》节选；
    雅典娜圣橄榄与树下祭坛——Wikipedia「Pandroseion」；赫尔墨斯路口石堆——Wikipedia「Herm」、theoi.com「Hermes Cult」；
    阿瑞斯战利品纪念柱——Wikipedia「Tropaion」；还愿盾牌——Wikipedia「Votive offering」。
    """
    titles = {"athena": "次神祠·雅典娜", "hermes": "次神祠·赫尔墨斯", "ares": "次神祠·阿瑞斯",
              "generic": "次神祠（通用，英雄 / 神话次神用）"}
    b = Building(f"greek_shrine_{god}", titles[god])
    n = 10
    # 圣域：地面、矮围墙（peribolos），东面正中开门，门旁界石（horos）
    b.fill(0, -1, 0, n, -1, n, "gravel")
    b.fill(1, -1, 2, 7, -1, 8, "step")
    for x in range(n + 1):
        for z in (0, n):
            b.set(x, 0, z, "mythic_age:polygonal_marble_wall")
    for z in range(n + 1):
        for x in (0, n):
            b.set(x, 0, z, "mythic_age:polygonal_marble_wall")
    b.clear(n, 0, 4, n, 0, 6)
    b.set(n, 0, 3, "floor")
    b.set(n, 1, 3, "smooth_slab", type="bottom")  # 界石
    # 小祠：双柱门廊——侧墙向前伸到门廊口成壁柱（anta），中间两根多立克柱
    top = 1
    b.fill(1, 0, 3, 6, 0, 7, "floor")  # 台基
    b.fill(1, top, 3, 1, top + 3, 7, "wall_marble")  # 后墙
    for z in (3, 7):
        b.fill(1, top, z, 6, top + 3, z, "wall_marble")  # 侧墙伸到门廊口（壁柱）
    b.fill(4, top, 4, 4, top + 3, 6, "wall_marble")  # 内殿前墙
    b.clear(4, top, 5, 4, top + 2, 5)  # 门
    for z in (4, 6):
        b.column(6, top, z, 2, "doric")
    for x in range(1, 7):
        b.set(x, top + 4, 3, "architrave", facing="north")
        b.set(x, top + 4, 7, "architrave", facing="south")
    for z in range(4, 7):
        b.set(6, top + 4, z, "architrave", facing="east")
        b.set(1, top + 4, z, "wall_marble")
    b.fill(2, top + 4, 4, 5, top + 4, 6, "floor")
    b.gable_roof(1, 6, 2, 8, top + 5)
    b.pediment_gable(7, 2, 8, top + 5, "east")
    b.pediment_gable(0, 2, 8, top + 5, "west")
    b.set(2, top, 5, "statue", half="lower", facing="east")
    b.set(2, top + 1, 5, "statue", half="upper", facing="east")
    b.set(3, top, 4, "lamp")
    b.set(3, top, 6, "lamp")
    # 东面露天祭坛（朝东）
    b.fill(8, 0, 4, 8, 0, 6, "floor")
    b.set(8, 1, 4, "smooth_slab", type="bottom")
    b.set(8, 1, 5, "brazier", lit="true")
    b.set(8, 1, 6, "smooth_slab", type="bottom")
    b.anchor("shrine", 9, 0, 5)
    # 门廊壁柱上挂的还愿物
    if god == "athena":
        # 圣域西北的圣橄榄树（潘德洛索斯圣域），树下家神宙斯的小祭坛
        from greek_farm import olive_tree
        olive_tree(b, 8, 1, 1)
        b.set(7, 0, 2, "smooth_slab", type="bottom")
        b.set(9, 0, 2, "smooth_slab", type="bottom")
        item_frame(b, 6, 2, 2, "north", "shield")
        item_frame(b, 6, 2, 8, "south", "shield")
    elif god == "hermes":
        # 门外路口的石堆（herma）与方柱界柱，石堆上是行人供的油灯
        hx, hz = n + 1, 8
        for (x, z, h) in ((hx, hz, 1), (hx + 1, hz, 0), (hx, hz + 1, 0), (hx + 1, hz + 1, 0)):
            b.set(x, -1, z, "minecraft:cobblestone")
            b.fill(x, 0, z, x, h, z, "minecraft:cobblestone")
        b.set(hx + 1, 0, hz, "minecraft:cobblestone_slab", type="bottom")
        b.set(hx, 0, hz + 1, "minecraft:cobblestone_slab", type="bottom")
        b.set(hx, 2, hz, "lamp")
        b.set(hx + 1, 1, hz, "lamp")
        hx, hz = n + 1, 2
        b.set(hx, 0, hz, "mythic_age:marble_wall")
        b.set(hx, 1, hz, "mythic_age:marble_wall")
        b.set(hx, 2, hz, "mythic_age:herm_head", facing="east")  # 界柱顶端的胸像
        item_frame(b, 6, 2, 2, "north", "golden_boots")
        item_frame(b, 6, 2, 8, "south", "golden_boots")
    elif god == "ares":
        # 圣域西北角的战利品纪念柱（tropaion）：木桩 + 横杆，挂盔、甲、盾、矛；脚下堆着缴获的兵器
        b.fill(8, 0, 2, 8, 1, 2, "beam", axis="y")
        b.commands.append('summon armor_stand ~8.5 ~2 ~2.5 {ShowArms:1b,NoBasePlate:1b,Invulnerable:1b,Rotation:[90f,0f],'
                          'Pose:{LeftArm:[0f,0f,-90f],RightArm:[0f,0f,90f]},'
                          'ArmorItems:[{},{},{id:"minecraft:iron_chestplate",count:1},{id:"minecraft:iron_helmet",count:1}],'
                          'HandItems:[{id:"minecraft:trident",count:1},{id:"minecraft:shield",count:1}]}')
        b.set(7, 0, 2, "minecraft:barrel", facing="up")
        b.set(9, 0, 1, "minecraft:barrel", facing="up")
        item_frame(b, 6, 2, 2, "north", "shield")
        item_frame(b, 6, 2, 8, "south", "iron_sword")
    return b


def stoa() -> Building:
    b = Building("greek_stoa", "柱廊集市")
    b.fill(0, 0, 0, 20, 0, 6, "floor")
    b.fill(0, 1, 0, 20, 4, 0, "wall_marble")
    for x in (0, 20):
        b.fill(x, 1, 1, x, 4, 6, "wall_marble")
    # 铺面隔墙（深 3 格），柜台、货物
    for x in (5, 10, 15):
        b.fill(x, 1, 1, x, 3, 3, "wall_marble")
    goods = ("minecraft:barrel", "pithos", "minecraft:hay_block", "pithos")
    for i, x0 in enumerate((1, 6, 11, 16)):
        for x in range(x0, x0 + 4):
            b.set(x, 1, 3, "smooth_slab", type="top")
        b.set(x0, 1, 1, goods[i], **({"facing": "up"} if "barrel" in goods[i] else {"axis": "y"} if "hay" in goods[i] else {}))
        b.set(x0 + 1, 1, 1, "pithos")
        b.set(x0 + 2, 2, 3, "lamp")
        b.anchor("merchant", x0 + 1, 1, 2)
    # 南面柱廊
    for x in range(1, 20, 2):
        b.column(x, 1, 6, 2, "doric")
    for x in range(0, 21):
        b.set(x, 5, 6, "architrave", facing="south")
        b.set(x, 5, 0, "wall_marble")
    for z in range(1, 6):
        b.set(0, 5, z, "wall_marble")
        b.set(20, 5, z, "wall_marble")
    b.fill(1, 5, 1, 19, 5, 5, "smooth_slab", type="top")
    b.gable_roof(0, 20, -1, 7, 6)
    b.pediment_gable(21, -1, 7, 6, "east")
    b.pediment_gable(-1, -1, 7, 6, "west")
    return b


def house_court() -> Building:
    b = Building("greek_house_court", "内院宅")
    n = 10
    b.fill(0, -1, 0, n, -1, n, "step")
    b.fill(3, -1, 3, 7, -1, 7, "mosaic")  # 天井地面
    b.hollow(0, 0, 0, n, 0, n, "socle")
    b.hollow(0, 1, 0, n, 2, n, "plaster")
    # 内圈墙（围天井），留门
    b.hollow(2, 0, 2, 8, 2, 8, "plaster")
    b.fill(3, 0, 2, 7, 2, 2, "minecraft:air")  # 北翼对天井敞开成柱廊（pastas）
    for x in (3, 5, 7):
        b.fill(x, 0, 2, x, 2, 2, "fence")
    for (x, z) in ((2, 5), (8, 5), (5, 8)):
        b.clear(x, 0, z, x, 1, z)
    # 翼间隔墙
    b.fill(5, 0, 1, 5, 2, 1, "plaster")  # 北翼：主屋 | 宴室
    b.clear(5, 0, 1, 5, 1, 1)
    b.fill(1, 0, 5, 1, 2, 5, "plaster")  # 西翼两间
    b.fill(9, 0, 5, 9, 2, 5, "plaster")  # 东翼两间
    # 入口：南墙正中门，门厅
    b.clear(5, 0, n, 5, 1, n)
    door(b, 5, 0, n, "north")
    # 梁圈
    for x in range(0, n + 1):
        for z in (0, n):
            b.set(x, 3, z, "beam", axis="x")
    for z in range(1, n):
        for x in (0, n):
            b.set(x, 3, z, "beam", axis="z")
    for x in range(2, 9):
        for z in (2, 8):
            b.set(x, 3, z, "beam", axis="x")
    for z in range(3, 8):
        for x in (2, 8):
            b.set(x, 3, z, "beam", axis="z")
    for (x, z) in [(x, z) for x in range(1, n) for z in range(1, n) if not (2 <= x <= 8 and 2 <= z <= 8)]:
        b.set(x, 3, z, "plank_slab", type="top")
    b.ring_roof(-1, n + 1, -1, n + 1, 3, 7, 3, 7, 4)
    # 室内
    b.set(2, 0, 1, "minecraft:campfire", lit="true")  # 主屋炉灶
    bed(b, 7, 0, 1, "west")  # 宴室卧榻 × 2（白模用床）
    bed(b, 9, 0, 1, "west")
    bed(b, 1, 0, 3, "north")
    bed(b, 9, 0, 3, "north")
    bed(b, 1, 0, 7, "south")
    bed(b, 9, 0, 7, "south")
    for (x, z) in ((3, 9), (4, 9), (7, 9)):
        b.set(x, 0, z, "pithos")
    b.set(6, 0, 4, "minecraft:water_cauldron", level=3)  # 井
    b.set(4, 0, 6, "mythic_age:marble_ashlar_slab", type="bottom")  # 家神小祭坛
    b.set(4, 1, 6, "lamp")
    for p in ((1, 0, 3), (9, 0, 3), (1, 0, 7), (9, 0, 7)):
        b.anchor("home", *p)
    b.anchor("yard_gate", 5, 0, n)
    return b


def stoneworks() -> Building:
    b = Building("greek_stoneworks", "石作坊")
    b.fill(0, -1, 0, 8, -1, 8, "gravel")
    for x in range(0, 9):
        for z in (0, 8):
            b.set(x, 0, z, "mythic_age:polygonal_marble_wall")
    for z in range(0, 9):
        for x in (0, 8):
            b.set(x, 0, z, "mythic_age:polygonal_marble_wall")
    b.clear(4, 0, 8, 4, 0, 8)
    # 西侧工棚
    for z in (1, 4, 7):
        b.fill(3, 0, z, 3, 2, z, "beam", axis="y")
    b.fill(1, 0, 1, 1, 2, 7, "plaster")
    b.hip_roof(0, 4, 0, 8, 3)
    b.set(2, 0, 3, "mythic_age:mason_bench", facing="east")  # 石作台 × 2（T19 换装：原为锻造台、切石机替身）
    b.set(2, 0, 5, "mythic_age:mason_bench", facing="east")
    b.anchor("workstation", 2, 0, 4)
    # 院里：石料、半成品柱段、待运的构件
    for (x, z, h) in ((5, 2, 2), (6, 2, 1), (5, 3, 1)):
        b.fill(x, 0, z, x, h - 1, z, "mythic_age:marble")
    b.set(6, 0, 5, "col_base")
    b.set(6, 1, 5, "col_shaft")
    b.set(5, 0, 6, "col_shaft")
    b.set(7, 0, 6, "doric")
    # 东北角采石料堆（采集点，余量随采集减少）
    b.set(7, 0, 1, "mythic_age:marble")
    b.set(7, 0, 2, "minecraft:gravel")
    b.set(6, 0, 1, "minecraft:gravel")
    b.anchor("quarry", 6, 0, 2)
    return b


def storehouse() -> Building:
    b = Building("greek_storehouse", "大仓库")
    x1, z1 = 10, 8
    b.fill(0, -1, 0, x1, -1, z1, "step")
    b.hollow(0, 0, 0, x1, 0, z1, "polygonal")
    b.hollow(0, 1, 0, x1, 3, z1, "step")
    for x in range(0, x1 + 1):
        for z in (0, z1):
            b.set(x, 4, z, "beam", axis="x")
    for z in range(1, z1):
        for x in (0, x1):
            b.set(x, 4, z, "beam", axis="z")
    b.fill(1, 4, 1, x1 - 1, 4, z1 - 1, "plank_slab", type="top")
    b.hip_roof(-1, x1 + 1, -1, z1 + 1, 5)
    b.clear(x1, 0, 3, x1, 2, 5)
    door(b, x1, 0, 4, "west", hinge="left")
    door(b, x1, 0, 5, "west", hinge="right")
    b.set(x1, 0, 3, "polygonal")
    b.fill(x1, 1, 3, x1, 2, 3, "step")
    for x in range(1, x1 - 1):
        for z in (1, z1 - 1):
            b.set(x, 0, z, "pithos")
            b.set(x, 1, z, "pithos" if x % 2 else "minecraft:barrel", **({} if x % 2 else {"facing": "up"}))
            b.anchor("goods", x, 0, z)
    for x in (2, 5):
        for z in (3, 5):
            b.set(x, 0, z, "minecraft:barrel", facing="up")
    for x in (3, 7):
        b.set(x, 3, 0, "shutter", facing="north", half="top", open="true")
        b.set(x, 3, z1, "shutter", facing="south", half="top", open="true")
    b.anchor("storage_keeper", x1 - 2, 0, 4)
    return b


def winery() -> Building:
    b = Building("greek_winery", "葡萄酒坊")
    b.fill(0, -1, 0, 8, -1, 8, "step")
    b.fill(0, 0, 0, 8, 2, 0, "plaster")
    b.fill(0, 0, 0, 0, 2, 8, "plaster")
    b.fill(0, 0, 0, 8, 0, 0, "socle")
    b.fill(0, 0, 0, 0, 0, 8, "socle")
    for (x, z) in ((8, 8), (8, 4), (4, 8)):
        b.fill(x, 0, z, x, 2, z, "beam", axis="y")
    b.hip_roof(-1, 9, -1, 9, 3)
    # 抬高的踩葡萄池：池沿一圈台阶，池里紫色葡萄浆（白模用紫色地毯）
    b.fill(1, 0, 1, 5, 0, 5, "floor")
    for x in range(1, 6):
        for z in range(1, 6):
            if x in (1, 5) or z in (1, 5):
                b.set(x, 1, z, "smooth_slab", type="bottom")
            else:
                b.set(x, 1, z, "minecraft:purple_carpet")
    # 池中央的踩葡萄池方块（T19 换装：工作站，前格朝南对着出口）
    b.set(3, 1, 3, "mythic_age:treading_vat", facing="south", part="head")
    b.set(3, 1, 4, "mythic_age:treading_vat", facing="south", part="foot")
    b.anchor("workstation", 3, 1, 3)
    # 出口：池南沿开口，下面接酒的陶瓮
    b.set(3, 1, 5, "minecraft:air")
    b.set(3, 0, 6, "pithos")
    b.anchor("workstation", 3, 0, 7)
    # 酒瓮成排（成品）
    for x in (6, 7):
        for z in range(1, 6):
            b.set(x, 0, z, "pithos")
    b.set(1, 0, 7, "pithos")
    b.set(1, 1, 7, "lamp")
    return b


BUILDERS = (lambda: minor_shrine("athena"), lambda: minor_shrine("hermes"), lambda: minor_shrine("ares"),
            stoa, house_court, stoneworks, storehouse, winery)


def main():
    buildings = [f() for f in BUILDERS]
    OUT.mkdir(parents=True, exist_ok=True)
    for b in buildings:
        p = b.layer_plans(OUT / f"{b.name}_逐层.png", cell=18)
        print(f"{b.title}：{len(b.blocks)} 方块，平面图 {p.relative_to(ROOT)}")
    if "--world" in sys.argv:
        world = Path(sys.argv[sys.argv.index("--world") + 1])
        print("数据包：", write_datapack(world, buildings))


if __name__ == "__main__":
    main()
