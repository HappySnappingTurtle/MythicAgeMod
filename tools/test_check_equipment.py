"""check_equipment 的单元测试（T17 测试用例文档 §4，TC-T17-60～63）。

运行：python3 -m unittest tools/test_check_equipment.py
"""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_equipment as ce  # noqa: E402

REAL = Path(__file__).resolve().parents[1]


def fixture(root: Path, table: dict):
    contract = json.loads((REAL / ce.CONTRACT).read_text(encoding="utf-8"))
    p = root / ce.CONTRACT
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(contract, ensure_ascii=False), encoding="utf-8")
    t = root / ce.TABLES / "greek.json"
    t.parent.mkdir(parents=True, exist_ok=True)
    t.write_text(json.dumps(table, ensure_ascii=False), encoding="utf-8")


def real_table() -> dict:
    return json.loads((REAL / ce.TABLES / "greek.json").read_text(encoding="utf-8"))


class CheckEquipmentTest(unittest.TestCase):
    def run_with(self, table):
        with tempfile.TemporaryDirectory() as d:
            fixture(Path(d), table)
            return ce.check(Path(d))

    def test_60_real_table_consistent(self):
        errors, notes = ce.check(REAL)
        self.assertEqual([], errors)

    def test_61_armor_points_mismatch(self):
        t = real_table()
        t["armor"]["bronze"]["points"] = [2, 6, 6, 3]
        errors, _ = self.run_with(t)
        self.assertTrue(any("chest.points" in e for e in errors), errors)
        self.assertTrue(any("leggings.points" in e for e in errors), errors)

    def test_62_speed_mismatch(self):
        t = real_table()
        t["weapons"]["xiphos"]["speed"] = 1.6
        errors, _ = self.run_with(t)
        self.assertTrue(any("xiphos.speed" in e for e in errors), errors)

    def test_63_pending_needs_decided(self):
        t = real_table()
        errors, notes = self.run_with(t)
        self.assertEqual([], errors)
        self.assertTrue(any("xiphos.durability" in n for n in notes))
        del t["weapons"]["xiphos"]["decided"]
        errors, _ = self.run_with(t)
        self.assertTrue(any("decided" in e for e in errors), errors)


if __name__ == "__main__":
    unittest.main()
