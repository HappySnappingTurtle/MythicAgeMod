#!/usr/bin/env python3
"""T16 希腊圣物：物品图标（16 × 16）与供奉小模型（神坛上的陈列，models/relic/<名>.json）。
配色按 06 号卡片：
  - 瑞亚之冠 rheia_crown：低矮月桂冠环，前方一个大叶簇；白石底托、青铜叶脉箍、橄榄绿叶（3 色）。
  - 阿喀琉斯之甲 armor_of_achilles：胸甲正面与肩扣；暗铜甲片、皮革棕肩扣、象牙白边饰（3 色）。
  - 皮格马利翁雕像 pygmalion_statue：修长的站立人像与基座；白石、浅金发带与基座镶边（2 色）。
供奉模型先按卡片的外廓（u，16u = 1 格）在 0–16 坐标里以 (8, 8) 为中心、底面 y = 0 建模，正面朝南（+Z），
再用 scaled() 放大到真实大小（冠 = 人头大小、胸甲 = 玩家躯干大小、雕像 = 一格高的小立像），
由 ShrineRelicRenderer 按 1:1 摆在神坛顶面（每座神坛只放一件），正面朝走近神坛的人（神坛 facing 的反方向）。贴图是 4 × 4 的色块表（每块 4 × 4 像素，大色块、不加噪声，06 号 §0.8），
面的 UV 取色块内部 2 × 2。
运行：python3 tools/models/greek_relics.py && for n in rheia_crown armor_of_achilles pygmalion_statue; do
      python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/$n.json tools/texgen/specs/greek/relic_$n.json; done
"""
from __future__ import annotations

import json

from modelgen import ROOT, spec

RELIC_MODELS = ROOT / "neoforge/src/main/resources/assets/mythic_age/models/relic"

CROWN_PAL = {"o": "#2a2a1c", "w": "#d9d4c4", "W": "#f2efe4", "s": "#a9a392",
             "b": "#8a5a2a", "B": "#c9883a", "g": "#4f6b2a", "G": "#7a9a3c", "L": "#a6c25a"}
CROWN = ["................",
         "................",
         "......oooo......",
         ".....oGLLGo.....",
         "....oGLGGLGo....",
         "...ooGGBBGGoo...",
         "..oGgoGBBGogGo..",
         ".oGgo.oggo.ogGo.",
         ".oGo........oGo.",
         ".ogBo......oBgo.",
         "..oGgo....ogGo..",
         "..obBGooooGBbo..",
         "...owWWWWWWWwo..",
         "....oswwwwwso...",
         ".....oooooooo...",
         "................"]

ARMOR_PAL = {"o": "#1f140c", "d": "#5a3a1e", "m": "#8a5a2c", "h": "#b07a3c", "k": "#d9a85e",
             "l": "#5a3a24", "L": "#7a5434", "i": "#e8dfc4"}
ARMOR = ["................",
         "..oooo....oooo..",
         ".oLLlo....olLLo.",
         ".olloooooooollo.",
         ".oiimmhkkhmmiio.",
         "..odmhkhhkhmdo..",
         "..omhkmmmmkhmo..",
         "..omhmddddmhmo..",
         "..odmhmmmmhmdo..",
         "..odmmhkkhmmdo..",
         "..oddmhhhhmddo..",
         "..odddmmmmdddo..",
         "..oiiiiiiiiiio..",
         "...odmdmmdmdo...",
         "....oooooooo....",
         "................"]

STATUE_PAL = {"o": "#2a2620", "s": "#9a9486", "w": "#d6d1c2", "W": "#f4f1e8", "y": "#c9a64a", "Y": "#ead27a"}
STATUE = [".......oo.......",
          "......oWwo......",
          "......oyYo......",
          "......oWwo......",
          ".......oo.......",
          "......oWwo......",
          ".....oWWwso.....",
          ".....owWwso.....",
          "......oWwo......",
          "......oWso......",
          "......oWso......",
          "......oWso......",
          ".....oWwwso.....",
          "...ooyYYYYyoo...",
          "...owWWWWWwso...",
          "...oooooooooo..."]


# ───────────────────────────── 供奉小模型 ─────────────────────────────

def swatches(name: str, cells: dict[str, str]) -> dict[str, list[float]]:
    """色块表：cells 按顺序占 4 × 4 网格；返回 色名 → UV（色块内部 2 × 2）。"""
    keys = list(cells)
    assert len(keys) <= 16, name
    rows = []
    for py in range(16):
        row = ""
        for px in range(16):
            i = (py // 4) * 4 + px // 4
            row += keys[i] if i < len(keys) else keys[0]
        rows.append(row)
    spec("relic_" + name, rows, cells, 16, "relic/")
    uv = {}
    for i, k in enumerate(keys):
        cx, cy = (i % 4) * 4, (i // 4) * 4
        uv[k] = [cx + 1, cy + 1, cx + 3, cy + 3]
    return uv


def el(frm, to, color, uv, rot=None, comment=None):
    e = {}
    if comment:
        e["__comment"] = comment
    e["from"] = [round(v, 3) for v in frm]
    e["to"] = [round(v, 3) for v in to]
    if rot:
        e["rotation"] = rot
    e["faces"] = {f: {"texture": "#0", "uv": uv[color]} for f in ("north", "south", "east", "west", "up", "down")}
    return e


def scaled(elements: list, sx: float, sy: float, sz: float, y0: float = 0.0, ybase: float = 0.0) -> list:
    """按真实大小放大：x、z 绕 (8, 8) 缩放，y 从 ybase 起缩放后平移到 y0（旋转原点一并变换）。"""
    out = []
    for e in elements:
        e = json.loads(json.dumps(e))
        for k in ("from", "to"):
            x, y, z = e[k]
            e[k] = [round(8 + (x - 8) * sx, 3), round(y0 + (y - ybase) * sy, 3), round(8 + (z - 8) * sz, 3)]
        if "rotation" in e:
            ox, oy, oz = e["rotation"]["origin"]
            e["rotation"]["origin"] = [round(8 + (ox - 8) * sx, 3), round(y0 + (oy - ybase) * sy, 3), round(8 + (oz - 8) * sz, 3)]
        out.append(e)
    return out


def write_model(name: str, elements: list) -> None:
    RELIC_MODELS.mkdir(parents=True, exist_ok=True)
    m = {"textures": {"0": "mythic_age:relic/relic_" + name, "particle": "mythic_age:relic/relic_" + name},
         "elements": elements}
    (RELIC_MODELS / f"{name}.json").write_text(json.dumps(m, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def crown_model():
    """瑞亚之冠：低矮月桂冠环（外廓 8 × 4 × 8），白石底托、青铜叶脉箍、环上一圈橄榄叶，正面一个大叶簇；环心中空。"""
    uv = swatches("rheia_crown", {"w": "#e6e1d3", "W": "#f6f3ea", "b": "#9a6a32", "B": "#c99446",
                                  "g": "#56722e", "G": "#7a9a3c", "L": "#a2bf58"})
    e = [el((4, 0, 4), (12, 0.75, 5), "w", uv, comment="白石底托（北）"), el((4, 0, 11), (12, 0.75, 12), "w", uv),
         el((4, 0, 5), (5, 0.75, 11), "w", uv), el((11, 0, 5), (12, 0.75, 11), "w", uv),
         el((4.25, 0.75, 4.25), (11.75, 1.5, 4.75), "B", uv, comment="青铜叶脉箍"),
         el((4.25, 0.75, 11.25), (11.75, 1.5, 11.75), "b", uv),
         el((4.25, 0.75, 4.75), (4.75, 1.5, 11.25), "b", uv), el((11.25, 0.75, 4.75), (11.75, 1.5, 11.25), "B", uv)]
    # 环上的月桂叶：沿四边每 1.5u 一片，向外斜 22.5°，深浅交替
    leaves = []
    for i, t in enumerate([4.75, 6.25, 7.75, 9.25, 10.75]):
        c = "G" if i % 2 == 0 else "g"
        leaves += [el((t - 0.5, 1.25, 4.0), (t + 0.5, 2.75, 4.75), c, uv, {"angle": -22.5, "axis": "x", "origin": [t, 1.25, 4.5]}),
                   el((t - 0.5, 1.25, 11.25), (t + 0.5, 2.75, 12.0), c, uv, {"angle": 22.5, "axis": "x", "origin": [t, 1.25, 11.5]}),
                   el((4.0, 1.25, t - 0.5), (4.75, 2.75, t + 0.5), c, uv, {"angle": 22.5, "axis": "z", "origin": [4.5, 1.25, t]}),
                   el((11.25, 1.25, t - 0.5), (12.0, 2.75, t + 0.5), c, uv, {"angle": -22.5, "axis": "z", "origin": [11.5, 1.25, t]})]
    leaves[0]["__comment"] = "环上的月桂叶"
    # 正面大叶簇：三片大叶呈扇形 + 中间一颗青铜果
    cluster = [el((6.75, 1.25, 11.75), (9.25, 4.0, 12.5), "L", uv, comment="正面大叶簇"),
               el((5.5, 1.25, 11.5), (7.25, 3.5, 12.25), "G", uv, {"angle": 22.5, "axis": "z", "origin": [7.0, 1.25, 12.0]}),
               el((8.75, 1.25, 11.5), (10.5, 3.5, 12.25), "G", uv, {"angle": -22.5, "axis": "z", "origin": [9.0, 1.25, 12.0]}),
               el((7.5, 1.75, 12.25), (8.5, 2.75, 12.75), "B", uv)]
    # 真实大小：冠环约 9u 见方，正好戴在玩家头上（头 8u）
    write_model("rheia_crown", scaled(e + leaves + cluster, 1.15, 1.15, 1.15))


def armor_model():
    """阿喀琉斯之甲：胸甲正背两片与肩扣（外廓 6 × 8 × 2），正面胸肌浮雕两级明暗；暗铜、皮革棕、象牙白边饰。"""
    uv = swatches("armor_of_achilles", {"d": "#6e4520", "m": "#8f5c2c", "h": "#b47c3e", "l": "#5c3c24",
                                        "L": "#7a5434", "i": "#e6dcc0", "s": "#3a2a1c"})
    e = [el((5.0, 4.0, 8.0), (11.0, 6.5, 9.0), "m", uv, comment="正面胸部（宽）"),
         el((5.6, 1.5, 8.0), (10.4, 4.0, 8.9), "m", uv, comment="正面腰部（收窄）"),
         el((5.2, 0.6, 7.9), (10.8, 1.5, 9.1), "i", uv, comment="下摆外扩的象牙白边"),
         el((5.0, 6.5, 8.0), (6.6, 7.4, 9.0), "m", uv, comment="左肩（领口两侧）"),
         el((9.4, 6.5, 8.0), (11.0, 7.4, 9.0), "m", uv),
         el((6.6, 6.3, 8.7), (9.4, 6.6, 9.05), "i", uv, comment="领口白边（下凹）"),
         el((4.85, 4.6, 7.4), (5.05, 6.2, 8.6), "i", uv, comment="袖口白边"),
         el((10.95, 4.6, 7.4), (11.15, 6.2, 8.6), "i", uv),
         el((5.1, 4.0, 7.0), (10.9, 7.2, 8.0), "d", uv, comment="背面甲片"),
         el((5.7, 1.5, 7.1), (10.3, 4.0, 8.0), "d", uv),
         el((5.3, 7.4, 7.2), (6.4, 7.9, 8.8), "L", uv, comment="皮革肩扣"),
         el((9.6, 7.4, 7.2), (10.7, 7.9, 8.8), "L", uv),
         el((5.6, 7.55, 8.8), (6.1, 7.85, 9.1), "h", uv, comment="肩扣铜钉"),
         el((9.9, 7.55, 8.8), (10.4, 7.85, 9.1), "h", uv),
         el((5.6, 4.6, 9.0), (7.8, 6.1, 9.35), "h", uv, comment="胸肌浮雕（亮）"),
         el((8.2, 4.6, 9.0), (10.4, 6.1, 9.35), "h", uv),
         el((6.4, 3.2, 8.9), (7.85, 3.9, 9.2), "h", uv, comment="腹肌（两排）"),
         el((8.15, 3.2, 8.9), (9.6, 3.9, 9.2), "h", uv),
         el((6.5, 2.2, 8.9), (7.85, 2.9, 9.15), "h", uv),
         el((8.15, 2.2, 8.9), (9.5, 2.9, 9.15), "h", uv),
         el((7.85, 1.6, 8.9), (8.15, 6.2, 9.4), "d", uv, comment="中线（暗）")]
    # 真实大小：玩家躯干 8 × 12 × 4u，胸甲罩在外面约 10 × 13 × 4.4u，架在木支架上
    e = scaled(e, 10 / 6, 13 / 7.3, 2.2, y0=2.0, ybase=0.6)
    e += [el((7.3, 0, 7.3), (8.7, 2.6, 8.7), "s", uv, comment="木支架立柱"),
          el((5.0, 0, 7.4), (11.0, 0.6, 8.6), "s", uv, comment="木支架底座"),
          el((7.4, 0, 5.5), (8.6, 0.6, 10.5), "s", uv)]
    write_model("armor_of_achilles", e)


def statue_model():
    """皮格马利翁雕像：修长的站立人像（外廓 5 × 10 × 5），基座 / 身躯 / 头三组；白石与浅金发带、基座镶边。"""
    uv = swatches("pygmalion_statue", {"w": "#e3ded0", "W": "#f6f3ea", "s": "#bdb6a4", "y": "#d6b45a", "Y": "#efd889"})
    e = [el((5.5, 0, 5.5), (10.5, 1.25, 10.5), "w", uv, comment="基座"),
         el((5.4, 1.25, 5.4), (10.6, 1.6, 10.6), "y", uv, comment="基座浅金镶边"),
         el((6.0, 1.6, 6.0), (10.0, 2.0, 10.0), "s", uv),
         el((6.6, 2.0, 6.9), (9.4, 4.0, 9.1), "W", uv, comment="长袍下摆"),
         el((6.85, 4.0, 7.1), (9.15, 6.25, 8.9), "W", uv, comment="长袍"),
         el((7.2, 2.0, 9.1), (7.45, 5.8, 9.2), "s", uv, comment="长袍褶纹"),
         el((8.0, 2.0, 9.1), (8.2, 5.6, 9.2), "s", uv),
         el((8.75, 2.0, 9.1), (9.0, 5.8, 9.2), "s", uv),
         el((7.0, 6.2, 8.85), (9.0, 6.45, 9.0), "s", uv, comment="束腰"),
         el((6.8, 6.25, 7.15), (9.2, 8.0, 8.85), "w", uv, comment="上身"),
         el((6.3, 5.6, 7.45), (6.8, 7.9, 8.4), "w", uv, comment="手臂"),
         el((9.2, 6.6, 7.45), (9.7, 7.9, 8.4), "w", uv, comment="右上臂"),
         el((8.4, 6.4, 8.4), (9.6, 6.9, 9.0), "w", uv, comment="右前臂横在胸前"),
         el((7.65, 8.0, 7.65), (8.35, 8.4, 8.35), "w", uv, comment="颈"),
         el((7.25, 8.4, 7.25), (8.75, 10.0, 8.75), "W", uv, comment="头"),
         el((7.15, 9.3, 7.15), (8.85, 9.6, 8.85), "Y", uv, comment="浅金发带"),
         el((7.5, 8.8, 6.75), (8.5, 9.6, 7.25), "s", uv, comment="发髻（脑后）")]
    # 真实大小：手持的小立像，高约 1 格（15u）
    write_model("pygmalion_statue", scaled(e, 1.5, 1.5, 1.5))


def main():
    spec("rheia_crown", CROWN, CROWN_PAL, 16, "item/")
    spec("armor_of_achilles", ARMOR, ARMOR_PAL, 16, "item/")
    spec("pygmalion_statue", STATUE, STATUE_PAL, 16, "item/")
    crown_model()
    armor_model()
    statue_model()
    print("rheia_crown armor_of_achilles pygmalion_statue")


if __name__ == "__main__":
    main()
