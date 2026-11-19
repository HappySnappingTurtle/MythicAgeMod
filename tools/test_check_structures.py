"""GA-6：真实 NBT 正负例、登记完整性与模型/物品继承回归。"""
import copy
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import check_structures as cs
from buildings.nbtio import Compound, Int, List, String, TAG_COMPOUND, TAG_INT, write_gzip


def fixture(path, support=None, facing='north', support_pos=(1, 1, 2)):
    palette = [Compound({'Name': String('mythic_age:terracotta_antefix'),
                         'Properties': Compound({'facing': String(facing)})})]
    blocks = [Compound({'pos': List(TAG_INT, [Int(1), Int(1), Int(1)]), 'state': Int(0)})]
    if support:
        palette.append(Compound({'Name': String(support)}))
        blocks.append(Compound({'pos': List(TAG_INT, [Int(v) for v in support_pos]), 'state': Int(1)}))
    write_gzip(path, Compound({'DataVersion': Int(3955), 'size': List(TAG_INT, [Int(3)] * 3),
                              'palette': List(TAG_COMPOUND, palette), 'blocks': List(TAG_COMPOUND, blocks),
                              'entities': List(TAG_COMPOUND, [])}))


class StructureAttachmentsTest(unittest.TestCase):
    def setUp(self):
        self.table = cs.load_table()

    def test_tc_ga6_01_all_greek_templates(self):
        paths = sorted((cs.ROOT / cs.STRUCTURES).glob('**/greek/**/*.nbt'))
        self.assertTrue(paths)
        for path in paths:
            with self.subTest(template=path.name):
                self.assertEqual([], cs.check_template(path, self.table))

    def test_tc_ga6_02_floating_nbt_fails_cli_with_location(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'floating_antefix.nbt'
            fixture(path)
            # 子进程输出统一按 UTF-8（Windows 上管道默认用 GBK）
            result = subprocess.run([sys.executable, str(cs.ROOT / 'tools/check_structures.py'), str(path)],
                                    capture_output=True, text=True, encoding='utf-8',
                                    env={**os.environ, 'PYTHONIOENCODING': 'utf-8'})
            self.assertEqual(1, result.returncode, result.stdout + result.stderr)
            self.assertIn('floating_antefix.nbt', result.stdout)
            self.assertIn('(1, 1, 1)', result.stdout)
            self.assertIn('minecraft:air', result.stdout)

    def test_tc_ga6_03_cornice_and_roof_support(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'supported_antefix.nbt'
            for support in ['marble_cornice', 'terracotta_roof_tiles', 'terracotta_roof_stairs', 'terracotta_roof_slab']:
                with self.subTest(support=support):
                    fixture(path, 'mythic_age:' + support)
                    self.assertEqual([], cs.check_template(path, self.table))

    def test_tc_ga6_04_registry_complete(self):
        self.assertEqual([], cs.check_registry(cs.ROOT, self.table))
        for key in self.table['blocks']:
            with self.subTest(removed=key):
                changed = copy.deepcopy(self.table)
                del changed['blocks'][key]
                self.assertTrue(cs.check_registry(cs.ROOT, changed), key)
        changed = copy.deepcopy(self.table)
        changed['blocks']['mythic_age:missing_decoration'] = changed['blocks']['mythic_age:terracotta_antefix']
        self.assertTrue(cs.check_registry(cs.ROOT, changed))

    def test_all_directions_and_front_is_not_support(self):
        behind = {'north': (1, 1, 2), 'south': (1, 1, 0), 'east': (0, 1, 1), 'west': (2, 1, 1)}
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'rotated.nbt'
            for facing, pos in behind.items():
                with self.subTest(facing=facing):
                    fixture(path, 'minecraft:stone', facing, pos)
                    self.assertEqual([], cs.check_template(path, self.table))
                    fixture(path, 'minecraft:stone', facing, tuple(2 - v for v in pos))
                    self.assertEqual(1, len(cs.check_template(path, self.table)))

    def test_non_solid_and_missing_facing_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / 'bad_support.nbt'
            for support in ['minecraft:air', 'minecraft:water', 'minecraft:flower_pot', 'mythic_age:terracotta_antefix']:
                fixture(path, support)
                errors = cs.check_template(path, self.table)
                self.assertTrue(any('(1, 1, 1)' in error for error in errors), errors)
            fixture(path, 'minecraft:stone', 'up')
            self.assertEqual(1, len(cs.check_template(path, self.table)))

    def test_pediment_ornaments_must_have_backing(self):
        for name in ['pediment_thunderbolt', 'pediment_trident']:
            blocks = {(0, 0, 0): {'Name': 'mythic_age:' + name, 'Properties': {'facing': 'north'}}}
            self.assertEqual(1, len(cs.check_blocks(name, blocks, self.table)))
            blocks[(0, 0, 1)] = {'Name': 'mythic_age:greek_pediment'}
            self.assertEqual([], cs.check_blocks(name, blocks, self.table))

    def test_oil_lamp_requires_full_height_support(self):
        blocks = {(0, 1, 0): {'Name': 'mythic_age:greek_oil_lamp', 'Properties': {'facing': 'north'}}}
        for kind in ['top', 'double', 'bottom', None]:
            blocks[(0, 0, 0)] = {'Name': 'mythic_age:smooth_marble_slab', 'Properties': {'type': kind}}
            self.assertEqual(0 if kind in ('top', 'double') else 1,
                             len(cs.check_blocks('灯座', blocks, self.table)))

    def test_block_geometry_and_item_parent(self):
        assets = cs.ROOT / 'neoforge/src/main/resources/assets/mythic_age/models'
        model = json.loads((assets / 'block/terracotta_antefix.json').read_text(encoding='utf-8'))
        self.assertEqual([(15, 15), (14, 16)], [(e['from'][2], e['to'][2]) for e in model['elements']])
        item = cs.ROOT / 'neoforge/src/generated/resources/assets/mythic_age/models/item/terracotta_antefix.json'
        self.assertEqual('mythic_age:block/terracotta_antefix', json.loads(item.read_text(encoding='utf-8'))['parent'])


if __name__ == '__main__':
    unittest.main()
