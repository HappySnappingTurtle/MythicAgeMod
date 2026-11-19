#!/usr/bin/env python3
"""村庄建筑生成器（TV 白模 → T13b 正式模板）。

建筑用 Python 写成体素：{(x, y, z): 方块状态}，坐标 x 向东、y 向上、z 向南，原点是建筑占地的西北角、地面一层。
导出：
  - 数据包函数（setblock 相对坐标）：游戏里 `execute positioned X Y Z run function mawm:<名>` 搭出白模；
  - 逐层平面图 PNG：每层一格一色，图例按方块分类，供设计文档使用；
  - 结构模板 NBT：T13b 再加（同一份体素）。

方块放置顺序按「先下后上、先实心后附着」排序，门、神像等两格高方块显式写上下两半。
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
MA = "mythic_age:"

# ───────────── 方块别名（规划篇 §4：已有方块 + 新方块的白模替身） ─────────────

B = {
    # 神庙（T8a / T8b）
    "stylobate": MA + "polished_marble",
    "step": MA + "marble_ashlar",
    "step_stair": MA + "marble_ashlar_stairs",
    "floor": MA + "polished_marble",
    "wall_marble": MA + "smooth_marble",
    "col_base": MA + "marble_column_base",
    "col_shaft": MA + "marble_column",
    "doric": MA + "doric_capital",
    "ionic": MA + "ionic_capital",
    "corinthian": MA + "corinthian_capital",
    "architrave": MA + "marble_architrave",
    "frieze": MA + "triglyph_frieze",
    "cornice": MA + "marble_cornice",
    "pediment": MA + "greek_pediment",
    "roof_slab": MA + "terracotta_roof_slab",
    "roof_tiles": MA + "terracotta_roof_tiles",
    "ridge": MA + "terracotta_ridge",
    "antefix": MA + "terracotta_antefix",
    "statue": MA + "greek_statue",
    "brazier": MA + "greek_brazier",
    "lamp": MA + "greek_oil_lamp",
    "mosaic": MA + "pebble_mosaic",
    "polygonal": MA + "polygonal_marble",
    "polygonal_wall": MA + "polygonal_marble_wall",
    "meander_blue": MA + "blue_meander_marble",
    "smooth_slab": MA + "smooth_marble_slab",
    # 民居（T13b 阶段 ③ 换装：白灰墙、赭红墙裙、日晒泥砖、陶瓮都是正式方块）
    "plaster": MA + "plaster",
    "socle": MA + "plaster_dado",
    "mudbrick": MA + "mudbrick",
    "pithos": MA + "pithos",
    "beam": MA + "olive_log",
    "planks": MA + "olive_planks",
    "plank_slab": MA + "olive_slab",
    "door": MA + "olive_door",
    "shutter": MA + "olive_trapdoor",
    "fence": MA + "olive_fence",
    "bed": "minecraft:white_bed",
    "path": "minecraft:dirt_path",
    "gravel": "minecraft:gravel",
    "grass": "minecraft:grass_block",
}

# 平面图配色（按方块分类）
CATEGORY = [
    ("米诺斯红柱 / 双斧 / 圣角 / 牛首门", ("minoan_column", "labrys", "horns_of", "bull_gate"), (160, 45, 30)),
    ("柱", ("column", "capital"), (236, 232, 222)),
    ("台基 / 地面", ("polished_marble", "marble_ashlar", "mosaic"), (200, 196, 186)),
    ("大理石墙", ("smooth_marble",), (222, 218, 205)),
    ("檐部", ("architrave", "frieze", "cornice", "pediment"), (170, 160, 140)),
    ("屋顶", ("terracotta_roof", "ridge", "antefix"), (181, 83, 46)),
    ("勒脚 / 泥砖", ("plaster_dado", "mudbrick"), (150, 95, 67)),
    ("白灰墙", ("plaster",), (245, 245, 240)),
    ("木作", ("olive",), (122, 92, 60)),
    ("神像 / 祭坛 / 火盆", ("statue", "brazier", "lamp"), (214, 170, 60)),
    ("床", ("bed",), (90, 120, 200)),
    ("陶瓮 / 油瓶", ("pithos", "lekythos", "decorated_pot"), (170, 110, 70)),
    ("工作站", ("pottery_", "bronze_furnace", "anvil", "mason_bench", "treading_vat"), (130, 80, 50)),
    ("雕刻 / 圣物", ("herm_head", "lion_spout", "grave_relief", "boundary_stone", "omphalos", "village_altar",
                   "pediment_thunderbolt", "pediment_trident"), (230, 200, 120)),
    ("挡土墙", ("polygonal",), (140, 140, 132)),
    ("路面", ("path", "gravel"), (160, 140, 100)),
    ("水", ("water",), (90, 140, 220)),
    ("树叶", ("leaves",), (80, 140, 60)),
    ("树干", ("_log", "_wood"), (95, 70, 45)),
    ("草地 / 土", ("grass_block", "dirt"), (150, 175, 110)),
    ("铜鼎", ("tripod", "cauldron"), (120, 100, 70)),
    ("打谷场", ("threshing_floor",), (180, 170, 150)),
    ("黑石", ("blackstone",), (50, 48, 55)),
    ("倒地柱段", ("marble_pillar",), (205, 200, 185)),
    ("碎石", ("cobblestone", "marble_rubble"), (105, 105, 100)),
    ("宝库 / 箱子", ("barrel", "chest", "ruin_vault"), (200, 120, 30)),
    ("苔藓", ("moss",), (95, 125, 55)),
]


def category(state: str):
    for name, keys, color in CATEGORY:
        if any(k in state for k in keys):
            return name, color
    return "其他", (120, 120, 120)


@dataclass
class Building:
    name: str
    title: str
    blocks: dict = field(default_factory=dict)
    anchors: dict = field(default_factory=dict)  # 锚点名 → [(x, y, z)]
    commands: list = field(default_factory=list)  # 方块之后执行的附带命令（相对坐标，如召唤动物）

    def set(self, x, y, z, block, nbt=None, **props):
        state = B.get(block, block)
        if props:
            state += "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"
        if nbt:
            state += nbt
        self.blocks[(x, y, z)] = state

    def fill(self, x0, y0, z0, x1, y1, z1, block, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, block, **props)

    def hollow(self, x0, y0, z0, x1, y1, z1, block, **props):
        """只填外壳（墙）。"""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.set(x, y, z, block, **props)

    def clear(self, x0, y0, z0, x1, y1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.blocks[(x, y, z)] = "minecraft:air"

    def anchor(self, name, x, y, z):
        self.anchors.setdefault(name, []).append((x, y, z))

    # ── 复合构件 ──

    def column(self, x, y, z, shaft, capital, **cap_props):
        """柱础 + 柱身 × shaft + 柱头（自 y 起）。"""
        self.set(x, y, z, "col_base")
        for i in range(shaft):
            self.set(x, y + 1 + i, z, "col_shaft")
        self.set(x, y + 1 + shaft, z, capital, **cap_props)

    def gable_roof(self, x0, x1, z0, z1, y, pitch_rows=None):
        """东西走向的两坡屋顶（屋脊沿 x），1:2 缓坡：离檐口每 1 格升半格。z0..z1 为檐口到檐口。
        返回每列（按 z）的屋面高度表，供山花三角楣使用。"""
        width = z1 - z0 + 1
        half = (width - 1) / 2
        for z in range(z0, z1 + 1):
            d = min(z - z0, z1 - z)
            ridge = width % 2 == 1 and d == half
            for x in range(x0, x1 + 1):
                if ridge:
                    self.set(x, y + d // 2, z, "ridge", axis="x")
                else:
                    self.set(x, y + d // 2, z, "roof_slab", type="bottom" if d % 2 == 0 else "top")

    def hip_roof(self, x0, x1, z0, z1, y):
        """四坡屋顶（民居）：1:2 缓坡，离最近的檐口每 1 格升半格；最高一线放屋脊（沿长边）。"""
        cells = {}
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                cells[(x, z)] = min(x - x0, x1 - x, z - z0, z1 - z)
        top = max(cells.values())
        axis = "x" if (x1 - x0) >= (z1 - z0) else "z"
        for (x, z), d in cells.items():
            if d == top:
                self.set(x, y + d // 2, z, "ridge", axis=axis)
            else:
                self.set(x, y + d // 2, z, "roof_slab", type="bottom" if d % 2 == 0 else "top")

    def ring_roof(self, x0, x1, z0, z1, ix0, ix1, iz0, iz1, y):
        """围着天井的四坡环形屋顶：外圈 x0..x1 × z0..z1，中间 ix0..ix1 × iz0..iz1 露天；每格的坡度按离内外檐口较近的
        那一边计算，屋脊在每一翼的中线，向外、向天井两边落水。"""
        cells = {}
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if ix0 <= x <= ix1 and iz0 <= z <= iz1:
                    continue
                outer = min(x - x0, x1 - x, z - z0, z1 - z)
                dx = ix0 - 1 - x if x < ix0 else x - ix1 - 1 if x > ix1 else -1
                dz = iz0 - 1 - z if z < iz0 else z - iz1 - 1 if z > iz1 else -1
                inner = max(dx, dz)
                cells[(x, z)] = min(outer, inner)
        top = max(cells.values())
        for (x, z), d in cells.items():
            if d == top:
                side_wing = (x < ix0 or x > ix1) and iz0 <= z <= iz1
                self.set(x, y + d // 2, z, "ridge", axis="z" if side_wing else "x")
            else:
                self.set(x, y + d // 2, z, "roof_slab", type="bottom" if d % 2 == 0 else "top")

    def pediment_gable(self, x, z0, z1, y, facing):
        """山花：底行与屋檐同宽，每行左右各收 2 格（T8a 修订 02）。"""
        row = 0
        while z0 + 2 * row <= z1 - 2 * row:
            for z in range(z0 + 2 * row, z1 - 2 * row + 1):
                self.set(x, y + row, z, "pediment", facing=facing)
            row += 1

    def pediment_ornaments(self, kind):
        """在每面宽 ≥ 9 的山花底行正中前一格挂 3 格楣饰（规划篇 §5 主神差异），三叉戟再在中间那格上叠叉头。
        kind：pediment_thunderbolt / pediment_trident。"""
        gables = {}
        for (x, y, z), state in self.blocks.items():
            if "greek_pediment" in state:
                facing = re.search(r"facing=(\w+)", state).group(1)
                key = (facing, x if facing in ("east", "west") else z)
                gables.setdefault(key, []).append((x, y, z))
        step = {"east": (1, 0), "west": (-1, 0), "north": (0, -1), "south": (0, 1)}
        for (facing, _), cells in gables.items():
            y0 = min(c[1] for c in cells)
            row = sorted(c for c in cells if c[1] == y0)
            if len(row) < 9:
                continue
            mid = row[len(row) // 2]
            dx, dz = step[facing]
            along = (0, 1) if facing in ("east", "west") else (1, 0)
            for k in (-1, 0, 1):
                self.set(mid[0] + dx + along[0] * k, y0, mid[2] + dz + along[1] * k, MA + kind, facing=facing)
            if kind == "pediment_trident":
                self.set(mid[0] + dx, y0 + 1, mid[2] + dz, MA + kind, facing=facing)

    def crepidoma(self, x0, z0, x1, z1, steps, y=0):
        """台基：自 y 起 steps 级，每级向内收 1 格；返回台面（柱座）所在高度。"""
        for s in range(steps):
            self.fill(x0 + s, y + s, z0 + s, x1 - s, y + s, z1 - s, "step" if s < steps - 1 else "stylobate")
        return y + steps

    # ── 导出 ──

    def bounds(self):
        xs = [p[0] for p in self.blocks]
        ys = [p[1] for p in self.blocks]
        zs = [p[2] for p in self.blocks]
        return min(xs), min(ys), min(zs), max(xs), max(ys), max(zs)

    def mcfunction(self) -> str:
        def order(item):
            (x, y, z), s = item
            late = any(k in s for k in ("door", "bed", "statue", "brazier", "lamp", "antefix", "trapdoor", "pot"))
            return (late, y, x, z)
        lines = [f"# {self.title}（{self.name}）：由 tools/buildings 生成"]
        for (x, y, z), s in sorted(self.blocks.items(), key=order):
            lines.append(f"setblock ~{x} ~{y} ~{z} {s}")
        lines.extend(self.commands)
        return "\n".join(lines) + "\n"

    def layer_plans(self, out: Path, cell=18) -> Path:
        """逐层平面图：每层一个小图，排成网格；图例列出用到的分类。"""
        x0, y0, z0, x1, y1, z1 = self.bounds()
        w, d = x1 - x0 + 1, z1 - z0 + 1
        layers = [y for y in range(y0, y1 + 1) if any(p[1] == y and not self.blocks[p].endswith("air") for p in self.blocks)]
        font = _font(14)
        small = _font(11)
        pw, ph = w * cell + 20, d * cell + 40
        cols = min(4, len(layers))
        rows = math.ceil(len(layers) / cols)
        legend_h = 30
        img = Image.new("RGB", (cols * pw + 10, rows * ph + legend_h + 40), "white")
        dr = ImageDraw.Draw(img)
        dr.text((10, 8), f"{self.title} 逐层平面（x 向右为东，z 向下为南；每格 1 方块）", fill="black", font=font)
        used = {}
        for i, y in enumerate(layers):
            ox, oy = 10 + (i % cols) * pw, 40 + (i // cols) * ph
            dr.text((ox, oy), f"第 {y} 层", fill="black", font=small)
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    s = self.blocks.get((x, y, z))
                    px, py = ox + (x - x0) * cell, oy + 16 + (z - z0) * cell
                    if s and not s.endswith("air"):
                        name, color = category(s)
                        used[name] = color
                        dr.rectangle([px, py, px + cell - 1, py + cell - 1], fill=color, outline=(80, 80, 80))
                    else:
                        dr.rectangle([px, py, px + cell - 1, py + cell - 1], outline=(225, 225, 225))
            for name, pts in self.anchors.items():
                for (ax, ay, az) in pts:
                    if ay == y:
                        cx, cy = ox + (ax - x0) * cell + cell // 2, oy + 16 + (az - z0) * cell + cell // 2
                        dr.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=(220, 40, 40))
        ly = rows * ph + 45
        lx = 10
        for name, color in list(used.items()) + [("锚点", (220, 40, 40))]:
            dr.rectangle([lx, ly, lx + 14, ly + 14], fill=color, outline=(80, 80, 80))
            dr.text((lx + 18, ly), name, fill="black", font=small)
            lx += 18 + 12 * len(name) + 24
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(out)
        return out


def _font(size):
    """中文字体：macOS → Windows → Linux，都没有时退回 Pillow 默认字体（不显示中文）。"""
    for p in ("/System/Library/Fonts/PingFang.ttc", "/System/Library/Fonts/STHeiti Medium.ttc",
              "/System/Library/Fonts/Hiragino Sans GB.ttc",
              "C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/simhei.ttf", "C:/Windows/Fonts/simsun.ttc",
              "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"):
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def write_datapack(world_dir: Path, buildings: list, namespace="mawm"):
    """白模数据包：datapacks/mythic_age_whitemodel/data/<ns>/function/<名>.mcfunction。"""
    pack = world_dir / "datapacks" / "mythic_age_whitemodel"
    (pack / "data" / namespace / "function").mkdir(parents=True, exist_ok=True)
    (pack / "pack.mcmeta").write_text('{"pack": {"pack_format": 48, "description": "Mythic Age white models"}}\n',
                                      encoding="utf-8")
    for b in buildings:
        (pack / "data" / namespace / "function" / f"{b.name}.mcfunction").write_text(b.mcfunction(), encoding="utf-8")
    return pack


# ───────────── 正式模板导出（T13b） ─────────────

DATA_VERSION = 3955  # MC 1.21.1
DATA_ROOT = ROOT / "neoforge/src/main/resources/data/mythic_age"


def parse_state(state: str):
    """「ns:id[k=v,...]{nbt}」→ (名字, 属性表)。方块实体 NBT 暂不导出（白模里只有旗帜用到），遇到就报错。"""
    if "{" in state:
        raise ValueError(f"模板导出暂不支持方块 NBT：{state}")
    if "[" not in state:
        return state, {}
    name, rest = state.split("[", 1)
    props = dict(kv.split("=", 1) for kv in rest.rstrip("]").split(",") if kv)
    return name, props


def export_template(b: Building, short: str, meta: dict, civ="greek", kind="village"):
    """把体素导出成结构模板（.nbt）+ 建筑数据（.json）。

    原点 = 体素包围盒最小角；y ≥ 0 的空格写成空气（建筑内部与上方清空），y < 0 的空格不写（保留地形）。
    kind：village（村庄建筑，数据在 village_building/）或其他结构类别（ruin、labyrinth，T22；lair，T25a；数据在 structure_spec/，
    村庄系统读不到它们）。meta 带 keep_terrain 时 y ≥ 0 的空格也不写（保留地形）。
    返回 (模板路径, 数据路径)。
    """
    import json
    from nbtio import Compound, Int, List, String, TAG_COMPOUND, TAG_INT, write_gzip

    x0, y0, z0, x1, y1, z1 = b.bounds()
    size = (x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1)
    cells = dict(b.blocks)
    if not meta.get("keep_terrain"):
        # keep_terrain（嵌进山体的巢穴，T25a）：只写明确给出的格子，其余保留原地形
        for x in range(x0, x1 + 1):
            for y in range(max(0, y0), y1 + 1):
                for z in range(z0, z1 + 1):
                    cells.setdefault((x, y, z), "minecraft:air")
    palette, index, blocks = [], {}, []
    materials = {}
    for (x, y, z), state in sorted(cells.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
        name, props = parse_state(state)
        key = (name, tuple(sorted(props.items())))
        if key not in index:
            index[key] = len(palette)
            entry = {"Name": String(name)}
            if props:
                entry["Properties"] = Compound({k: String(v) for k, v in sorted(props.items())})
            palette.append(Compound(entry))
        blocks.append(Compound({"pos": List(TAG_INT, [Int(x - x0), Int(y - y0), Int(z - z0)]),
                                "state": Int(index[key])}))
        if name != "minecraft:air":
            materials[name] = materials.get(name, 0) + 1
    # 实体：只导出牲畜（白模里的 NoAI 去掉，加 PersistenceRequired 防止消失）；展示框、船等留到需要时再支持
    import re
    from nbtio import Byte, Double, TAG_DOUBLE
    entities = []
    for cmd in b.commands:
        m = re.match(r"summon (?:minecraft:)?(sheep|goat|cow|pig|chicken) ~([-\d.]+) ~([-\d.]+) ~([-\d.]+)", cmd)
        if not m:
            continue
        ex, ey, ez = float(m.group(2)) - x0, float(m.group(3)) - y0, float(m.group(4)) - z0
        entities.append(Compound({
            "pos": List(TAG_DOUBLE, [Double(ex), Double(ey), Double(ez)]),
            "blockPos": List(TAG_INT, [Int(int(ex)), Int(int(ey)), Int(int(ez))]),
            "nbt": Compound({"id": String("minecraft:" + m.group(1)), "PersistenceRequired": Byte(1)}),
        }))
    root = Compound({
        "DataVersion": Int(DATA_VERSION),
        "size": List(TAG_INT, [Int(v) for v in size]),
        "palette": List(TAG_COMPOUND, palette),
        "blocks": List(TAG_COMPOUND, blocks),
        "entities": List(TAG_COMPOUND, entities),
    })
    tpl = DATA_ROOT / "structure" / kind / civ / f"{short}.nbt"
    write_gzip(tpl, root)

    def rel(p):
        return [p[0] - x0, p[1] - y0, p[2] - z0]

    data = {
        "id": f"{civ}:{short}",
        "civ": civ,
        "template": f"mythic_age:{kind}/{civ}/{short}",
        "size": list(size),
        "floor_y": -y0,
        "frame": [x0, y0, z0],
        **{k: v for k, v in meta.items() if k != "door"},
        "anchors": {name: [rel(p) for p in pts] for name, pts in sorted(b.anchors.items())},
        "materials": dict(sorted(materials.items(), key=lambda kv: -kv[1])),
    }
    if "door" in meta:
        d = meta["door"]
        data["door"] = {"pos": rel(d["pos"]), "facing": d["facing"]}
        if d.get("width", 1) > 1:
            data["door"]["width"] = d["width"]  # 门宽（GA-14）：第二格在朝向的顺时针一侧
    js = DATA_ROOT / "mythic_age" / ("village_building" if kind == "village" else "structure_spec") / civ / f"{short}.json"
    js.parent.mkdir(parents=True, exist_ok=True)
    js.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    return tpl, js
