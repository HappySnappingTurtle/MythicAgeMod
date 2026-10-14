"""tools/ma_remote.py 简写语法的单元测试（T2b 单元测试用例文档 §3）。运行：python3 -m unittest tools/test_ma_remote.py"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ma_remote  # noqa: E402


class ParseShortTest(unittest.TestCase):
    def test_tc_t2b_20_key(self):
        self.assertEqual({"cmd": "key", "name": "use", "action": "down"},
                         ma_remote.parse_short("key name=use action=down"))

    def test_tc_t2b_21_text_keeps_equals(self):
        d = ma_remote.parse_short("chat text=/kill @e[type=minecraft:pig,distance=..5]")
        self.assertEqual("/kill @e[type=minecraft:pig,distance=..5]", d["text"])

    def test_tc_t2b_22_json_array(self):
        self.assertEqual([1, 64, 2], ma_remote.parse_short("look at=[1,64,2]")["at"])

    def test_tc_t2b_23_integer(self):
        self.assertEqual(20, ma_remote.parse_short("wait ticks=20")["ticks"])

    def test_tc_t2b_24_bad_token(self):
        with self.assertRaises(SystemExit):
            ma_remote.parse_short("foo bar")

    def test_bare_command(self):
        self.assertEqual({"cmd": "state"}, ma_remote.parse_short("state"))

    def test_text_with_spaces_and_chinese(self):
        d = ma_remote.parse_short("chat text=你好 世界")
        self.assertEqual("你好 世界", d["text"])

    def test_booleans(self):
        d = ma_remote.parse_short("tp x=0 y=120 z=0 fly=true")
        self.assertEqual((0, 120, 0, True), (d["x"], d["y"], d["z"], d["fly"]))


if __name__ == "__main__":
    unittest.main()
