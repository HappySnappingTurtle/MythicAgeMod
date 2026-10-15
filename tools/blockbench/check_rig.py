"""实体资源校验（T7 单元测试用例文档 §2，TC-T7-20 至 TC-T7-26）。

对每个登记在 RIGS 中的实体，比对「参数 JSON（唯一可信来源）」与导出的运行时文件：
  - 骨骼名称、父级、枢轴（卡片坐标 → Bedrock 坐标为 (x, y, -z)，容差 0.01u）；
  - 定位点：geo.json 与服务端骨架 rig/*.json 一致，且与参数 JSON 一致；
  - UV：都在贴图范围内；矩形之间不得部分交叉（完全相同或包含视为共用）；
  - 动画：设计要求的动画齐全，循环方式与时长一致，关键帧引用的骨骼都存在；
  - 哈希：.bbmodel 记录的导出哈希与运行时文件一致，否则报「工程与导出不同步」。

运行：python3 tools/blockbench/check_rig.py（也由 tools/check_content.py 调用）
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

DEFAULT_ROOT = Path(__file__).resolve().parents[2]
PIVOT_TOL = 0.01

RES = "neoforge/src/main/resources"
RIGS = [
    {
        "name": "minotaur",
        "params": "art/blockbench/greek/minotaur/minotaur.params.json",
        "bbmodel": "art/blockbench/greek/minotaur/minotaur.bbmodel",
        "geo": f"{RES}/assets/mythic_age/geo/entity/minotaur.geo.json",
        "animation": f"{RES}/assets/mythic_age/animations/entity/minotaur.animation.json",
        "texture": f"{RES}/assets/mythic_age/textures/entity/minotaur/minotaur.png",
        "glowmask": f"{RES}/assets/mythic_age/textures/entity/minotaur/minotaur_glowmask.png",
        "rig": f"{RES}/data/mythic_age/mythic_age/rig/minotaur.json",
    },
]


def card_to_bedrock(p):
    return [p[0], p[1], -p[2]]


def close(a, b, tol=PIVOT_TOL):
    return len(a) == len(b) and all(abs(x - y) <= tol for x, y in zip(a, b))


def load_json(root: Path, rel: str, errors: list[str], name: str):
    f = root / rel
    if not f.exists():
        errors.append(f"{name}：缺少文件 {rel}")
        return None
    try:
        return json.loads(f.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        errors.append(f"{name}：{rel} 不是合法 JSON（{e}）")
        return None


def geo_bones(geo: dict) -> dict[str, dict]:
    return {b["name"]: b for b in geo["minecraft:geometry"][0]["bones"]}


def check_bones(name, params, geo, errors):
    bones = geo_bones(geo)
    expected = params["bones"] + params.get("extra_bones", [])
    for b in expected:
        gb = bones.get(b["id"])
        if gb is None:
            errors.append(f"{name}：geo 缺少骨骼 {b['id']}")
            continue
        if gb.get("parent") != b["parent"]:
            errors.append(f"{name}：骨骼 {b['id']} 的父级应为 {b['parent']}，geo 中为 {gb.get('parent')}")
        want = card_to_bedrock(b["pivot_model_units"])
        if not close(gb.get("pivot", []), want):
            errors.append(f"{name}：骨骼 {b['id']} 的枢轴应为 {want}，geo 中为 {gb.get('pivot')}")
    extra = set(bones) - {b["id"] for b in expected}
    for b in sorted(extra):
        errors.append(f"{name}：geo 中有参数 JSON 未声明的骨骼 {b}")


def check_locators(name, params, geo, rig, errors):
    geo_locs = {}
    for b in geo["minecraft:geometry"][0]["bones"]:
        for k, v in (b.get("locators") or {}).items():
            geo_locs[k] = v if isinstance(v, list) else v.get("offset")
    want = {a["id"]: a["position_model_units"] for a in params["attachments"]}
    rig_locs = (rig or {}).get("locators", {})
    for k, p in want.items():
        if k not in geo_locs:
            errors.append(f"{name}：geo 缺少定位点 {k}")
        elif not close(geo_locs[k], card_to_bedrock(p)):
            errors.append(f"{name}：定位点 {k} 在 geo 中为 {geo_locs[k]}，应为 {card_to_bedrock(p)}")
        if k not in rig_locs:
            errors.append(f"{name}：骨架数据缺少定位点 {k}")
        elif not close(rig_locs[k], p):
            errors.append(f"{name}：定位点 {k} 在骨架数据中为 {rig_locs[k]}，与参数 {p} 不一致")
    for k in sorted(set(geo_locs) - set(want)):
        errors.append(f"{name}：geo 中有参数 JSON 未声明的定位点 {k}")
    for k in sorted(set(rig_locs) - set(want)):
        errors.append(f"{name}：骨架数据中有参数 JSON 未声明的定位点 {k}")


def face_rects(geo: dict):
    """返回 [(骨骼, 面, (x0, y0, x1, y1))]，矩形已规范化（尺寸可为负）。"""
    out = []
    for b in geo["minecraft:geometry"][0]["bones"]:
        for i, c in enumerate(b.get("cubes", [])):
            uv = c.get("uv")
            if not isinstance(uv, dict):
                continue
            for face, f in uv.items():
                (u, v), (w, h) = f["uv"], f["uv_size"]
                x0, x1 = sorted((u, u + w))
                y0, y1 = sorted((v, v + h))
                out.append((f"{b['name']}#{i}", face, (x0, y0, x1, y1)))
    return out


def contains(a, b) -> bool:
    """a 完全包含 b（共用组取子区域，如徽章侧面取图案边缘）。"""
    return a[0] <= b[0] and a[1] <= b[1] and b[2] <= a[2] and b[3] <= a[3]


def check_uv(name, geo, errors):
    desc = geo["minecraft:geometry"][0]["description"]
    tw, th = desc["texture_width"], desc["texture_height"]
    rects = face_rects(geo)
    for owner, face, (x0, y0, x1, y1) in rects:
        if x0 < 0 or y0 < 0 or x1 > tw or y1 > th:
            errors.append(f"{name}：{owner} 的 {face} 面 UV {(x0, y0, x1, y1)} 超出 {tw}×{th}")
    distinct = sorted({r for _, _, r in rects if r[2] > r[0] and r[3] > r[1]})
    for i, a in enumerate(distinct):
        for b in distinct[i + 1:]:
            if b[0] >= a[2]:
                break
            if a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3] and not (contains(a, b) or contains(b, a)):
                errors.append(f"{name}：UV 矩形 {a} 与 {b} 部分重叠")
    return tw, th


def loop_kind(a: dict) -> str:
    v = a.get("loop", False)
    if v is True:
        return "loop"
    if v == "hold_on_last_frame":
        return "hold_on_last_frame"
    return "once"


def check_animations(name, params, anim, geo, errors):
    want = params.get("animations", {})
    have = (anim or {}).get("animations", {})
    bones = set(geo_bones(geo))
    for k, spec in want.items():
        a = have.get(k)
        if a is None:
            errors.append(f"{name}：缺少动画 {k}")
            continue
        if loop_kind(a) != spec["loop"]:
            errors.append(f"{name}：动画 {k} 的循环方式应为 {spec['loop']}，实际 {loop_kind(a)}")
        if abs(float(a.get("animation_length", 0)) - spec["length"]) > 1e-4:
            errors.append(f"{name}：动画 {k} 的时长应为 {spec['length']} 秒，实际 {a.get('animation_length')}")
    for k, a in have.items():
        for b in a.get("bones", {}):
            if b not in bones:
                errors.append(f"{name}：动画 {k} 引用了不存在的骨骼 {b}")


def sha256(f: Path) -> str:
    return hashlib.sha256(f.read_bytes()).hexdigest()


def check_hashes(name, root, entry, bbmodel, errors):
    recorded = ((bbmodel or {}).get("mythic_age") or {}).get("exports") or {}
    for key in ("geo", "animation", "texture", "glowmask", "rig"):
        rel = entry.get(key)
        if rel is None:
            continue
        f = root / rel
        if not f.exists():
            continue
        if rel not in recorded:
            errors.append(f"{name}：.bbmodel 没有记录 {rel} 的导出哈希（请重新运行构建脚本）")
        elif recorded[rel] != sha256(f):
            errors.append(f"{name}：工程与导出不同步：{rel}（请在 Blockbench 中重新运行构建脚本）")


def check_texture_size(name, root, entry, tw, th, errors):
    f = root / entry["texture"]
    if not f.exists():
        errors.append(f"{name}：缺少贴图 {entry['texture']}")
        return
    from PIL import Image
    with Image.open(f) as img:
        if img.size != (tw, th):
            errors.append(f"{name}：贴图尺寸 {img.size} 与 geo 声明的 {tw}×{th} 不一致")
    glow = entry.get("glowmask")
    if glow and (root / glow).exists():
        with Image.open(root / glow) as img:
            if img.size != (tw, th):
                errors.append(f"{name}：自发光遮罩尺寸 {img.size} 与贴图 {tw}×{th} 不一致")


def check_entry(root: Path, entry: dict) -> list[str]:
    errors: list[str] = []
    name = entry["name"]
    params = load_json(root, entry["params"], errors, name)
    geo = load_json(root, entry["geo"], errors, name)
    anim = load_json(root, entry["animation"], errors, name)
    rig = load_json(root, entry["rig"], errors, name)
    bbmodel = load_json(root, entry["bbmodel"], errors, name)
    if params is None or geo is None:
        return errors
    check_bones(name, params, geo, errors)
    check_locators(name, params, geo, rig, errors)
    tw, th = check_uv(name, geo, errors)
    check_texture_size(name, root, entry, tw, th, errors)
    if anim is not None:
        check_animations(name, params, anim, geo, errors)
    if rig is not None:
        coll = rig.get("collision", {})
        want = params["collision_proposal_blocks"]
        if not close([coll.get("width", -1), coll.get("height", -1)], want[:2], 1e-6):
            errors.append(f"{name}：骨架数据的碰撞 {coll} 与参数 {want[:2]} 不一致")
    if bbmodel is not None:
        check_hashes(name, root, entry, bbmodel, errors)
    return errors


def run(root: Path, rigs=None) -> list[str]:
    errors: list[str] = []
    for entry in rigs if rigs is not None else RIGS:
        errors.extend(check_entry(root, entry))
    return errors


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description="Mythic Age 实体资源校验")
    ap.add_argument("--root", type=Path, default=DEFAULT_ROOT)
    a = ap.parse_args(argv)
    errors = run(a.root)
    for e in errors:
        print(f"错误：{e}")
    print(f"共 {len(errors)} 个错误" if errors else "实体资源全部通过")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
