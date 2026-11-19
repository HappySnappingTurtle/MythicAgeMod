#!/usr/bin/env python3
"""GA-6：用共用依附表检查全部希腊 NBT（村庄、遗迹、迷宫），失败输出模板名与坐标。

运行：python3 tools/check_structures.py [模板.nbt ...] [--root 仓库根目录]
回归：python3 -m unittest discover -s tools -p test_check_structures.py
"""
from __future__ import annotations

import argparse
import gzip
import json
import re
from pathlib import Path

from buildings.nbtio import loads

ROOT = Path(__file__).resolve().parents[1]
TABLE = Path('neoforge/src/main/resources/data/mythic_age/mythic_age/architecture_attach.json')
STRUCTURES = Path('neoforge/src/main/resources/data/mythic_age/structure')
JAVA = Path('neoforge/src/main/java/io/github/happysnappingturtle/mythicage/content')
FORWARD = {'north': (0, 0, -1), 'east': (1, 0, 0), 'south': (0, 0, 1), 'west': (-1, 0, 0)}


def load_table(root=ROOT):
    return json.loads((root / TABLE).read_text(encoding='utf-8'))


def offset(direction, facing):
    if direction == 'down':
        return 0, -1, 0
    if direction == 'up':
        return 0, 1, 0
    x, _, z = FORWARD[facing]
    return {'back': (-x, 0, -z), 'left': (z, 0, -x), 'right': (-z, 0, x)}[direction]


def support_properties_match(rule, state):
    """顶面依附还要核验台阶状态，不能让下半台阶作为同 ID 的上半台阶蒙混过关。"""
    required = rule.get('support_properties', {}).get(state['Name'], {})
    actual = state.get('Properties', {})
    return all(actual.get(key) in values for key, values in required.items())


def check_blocks(name, blocks, table):
    errors = []
    for pos, state in blocks.items():
        rule = table['blocks'].get(state['Name'])
        if not rule or not rule['directions']:
            continue
        supports = set(table['solid_blocks']) | set(rule['allowed_supports'])
        facing = state.get('Properties', {}).get('facing')
        candidates = []
        try:
            for direction in rule['directions']:
                target = tuple(a + b for a, b in zip(pos, offset(direction, facing)))
                other = blocks.get(target, {}).get('Name', 'minecraft:air')
                candidates.append((target, other))
        except KeyError:
            errors.append(f'{name}: {pos} {state["Name"]} 缺失或无效朝向 {facing}')
            continue
        if not any(other in supports and support_properties_match(rule, blocks[target])
                   for target, other in candidates):
            errors.append(f'{name}: {pos} {state["Name"]} 无依附支撑：{candidates}')
    return errors


def check_template(path, table):
    nbt = loads(gzip.decompress(path.read_bytes()))
    # 兼容多 palette 模板；每种变体必须分别通过，不能只检查第一套。
    palettes = nbt.get('palettes', [nbt.get('palette', [])])
    errors = []
    for i, palette in enumerate(palettes):
        blocks = {tuple(b['pos']): palette[b['state']] for b in nbt['blocks']}
        errors.extend(check_blocks(f'{path} palette={i}', blocks, table))
    return errors


def check_registry(root, table):
    """TC-GA6-04：核验真实注册归属，包含 FacingShapedBlock 的派生类，增删必须同步依附表。"""
    sources = list((root / JAVA).rglob('*.java'))
    text = '\n'.join(p.read_text(encoding='utf-8') for p in sources)
    types = {'FacingShapedBlock', 'PedimentOrnamentBlock', 'PedimentBlock', 'BrazierBlock', 'StatueBlock',
             'WallSignBlock', 'CeilingHangingSignBlock', 'WallHangingSignBlock', 'OilLampBlock'}
    while True:
        children = {child for child, parent in re.findall(r'class\s+(\w+)\s+extends\s+(\w+)', text) if parent in types}
        if children <= types:
            break
        types |= children
    registered, audited = {}, set()
    for p in sources:
        # 泛型参数可能写全限定名（如 VILLAGE_ALTAR），取最后一段类名
        for kind, name in re.findall(r'DeferredBlock<([\w.]+)>\s+\w+\s*=\s*(?:Content\.block|shape)\(\s*"([^"]+)"', p.read_text(encoding="utf-8")):
            kind = kind.rsplit('.', 1)[-1]
            key = 'mythic_age:' + name
            registered[key] = p.stem
            if kind in types:
                audited.add(key)
    errors = []
    for key, rule in table['blocks'].items():
        if registered.get(key) != rule['owner']:
            errors.append(f'依附表 {key}: 注册归属应为 {rule["owner"]}，实际 {registered.get(key)}')
        if any(d not in {'back', 'down', 'up', 'left', 'right'} for d in rule['directions']):
            errors.append(f'依附表 {key}: 无效方向')
        for support in rule['allowed_supports']:
            if support.startswith('mythic_age:') and support not in registered:
                errors.append(f'依附表 {key}: 支撑方块未注册 {support}')
    for key, rule in table['blocks'].items():
        for support in rule.get('support_properties', {}):
            if support not in rule['allowed_supports']:
                errors.append(f'依附表 {key}: 状态约束没有对应许可支撑 {support}')
    for key in sorted(audited - table['blocks'].keys()):
        errors.append(f'依附表漏登记：{key} ({registered[key]})')
    return errors


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('templates', nargs='*', type=Path)
    parser.add_argument('--root', type=Path, default=ROOT)
    args = parser.parse_args(argv)
    table = load_table(args.root)
    paths = args.templates or sorted((args.root / STRUCTURES).glob('**/greek/**/*.nbt'))
    errors = check_registry(args.root, table)
    if not paths:
        errors.append('没有找到希腊建筑模板')
    for path in paths:
        try:
            errors.extend(check_template(path, table))
        except (OSError, ValueError, KeyError, IndexError) as exc:
            errors.append(f'{path}: 无法读取模板：{exc}')
    for error in errors:
        print('错误：' + error)
    print(f'结构依附检查：{len(paths)} 个模板，{len(table["blocks"])} 种已排查方块，{len(errors)} 个错误')
    return 1 if errors else 0


if __name__ == '__main__':
    raise SystemExit(main())
