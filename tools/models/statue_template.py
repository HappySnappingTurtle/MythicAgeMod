#!/usr/bin/env python3
"""主神像模板（T11a 设计文档 §5.2）：写出 data/mythic_age/structure/statue/<名字>.nbt。

模板按朝北绘制（正面朝 −z，核心 facing=north）：
  y0  3×3 抛光大理石底座（计入完整性）
  y1  中心 = 主神像核心；前排两角 = 花盆（装饰，不计入）
  y2、y3  中心 = 主神像部件（不可见碰撞）
其余为空气（放置时清空）。正式的各主神模板在 T11b 用游戏内结构方块导出。

用法：python3 tools/models/statue_template.py
"""
import gzip
import io
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "neoforge/src/main/resources/data/mythic_age/structure/statue"
DATA_VERSION = 3955  # 1.21.1


def _str(b: io.BytesIO, s: str):
    raw = s.encode("utf-8")
    b.write(struct.pack(">H", len(raw)))
    b.write(raw)


def _payload(b, tag, v):
    if tag == 3:
        b.write(struct.pack(">i", v))
    elif tag == 8:
        _str(b, v)
    elif tag == 9:
        etype, items = v
        b.write(struct.pack(">bi", etype, len(items)))
        for it in items:
            _payload(b, etype, it)
    elif tag == 10:
        for name, (t, val) in v.items():
            b.write(struct.pack(">b", t))
            _str(b, name)
            _payload(b, t, val)
        b.write(b"\x00")
    else:
        raise ValueError(tag)


def write_nbt(path: Path, root: dict):
    b = io.BytesIO()
    b.write(struct.pack(">b", 10))
    _str(b, "")
    _payload(b, 10, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.GzipFile(filename="", mode="wb", fileobj=path.open("wb"), mtime=0) as f:
        f.write(b.getvalue())


def state(name, props=None):
    c = {"Name": (8, name)}
    if props:
        c["Properties"] = (10, {k: (8, v) for k, v in props.items()})
    return c


def default_template():
    palette = [state("minecraft:air"), state("mythic_age:polished_marble"),
               state("mythic_age:patron_statue_core", {"facing": "north"}), state("mythic_age:patron_statue_part"),
               state("minecraft:flower_pot")]
    blocks = []
    for y in range(4):
        for z in range(3):
            for x in range(3):
                s = 0
                if y == 0:
                    s = 1
                elif (x, z) == (1, 1):
                    s = 2 if y == 1 else 3
                elif y == 1 and z == 0 and x in (0, 2):
                    s = 4
                blocks.append({"pos": (9, (3, [x, y, z])), "state": (3, s)})
    return {
        "DataVersion": (3, DATA_VERSION),
        "size": (9, (3, [3, 4, 3])),
        "palette": (9, (10, palette)),
        "blocks": (9, (10, blocks)),
        "entities": (9, (10, [])),
    }


if __name__ == "__main__":
    out = OUT / "default.nbt"
    write_nbt(out, default_template())
    print("wrote", out.relative_to(ROOT))
