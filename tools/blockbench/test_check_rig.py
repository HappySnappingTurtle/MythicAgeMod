"""check_rig 的单元测试（T7 单元测试用例文档 §2，TC-T7-20 至 TC-T7-26）。

运行：python3 -m unittest tools/blockbench/test_check_rig.py
坏样例在临时目录中由仓库里的真实资源复制后改坏，不改仓库文件。
"""
import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_rig as cr  # noqa: E402

ROOT = cr.DEFAULT_ROOT
ENTRY = cr.RIGS[0]


class CheckRigTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        for key in ("params", "bbmodel", "geo", "animation", "texture", "glowmask", "rig"):
            dst = self.tmp / ENTRY[key]
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy(ROOT / ENTRY[key], dst)

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def edit(self, key, fn):
        f = self.tmp / ENTRY[key]
        data = json.loads(f.read_text(encoding="utf-8"))
        fn(data)
        f.write_text(json.dumps(data), encoding="utf-8")

    def errors(self):
        return cr.run(self.tmp, [ENTRY])

    def assertReports(self, fragment):
        errs = self.errors()
        self.assertTrue(any(fragment in e for e in errs), f"应报出「{fragment}」，实际：{errs}")

    def test_tc20_repository_passes(self):
        self.assertEqual([], cr.run(ROOT))

    def test_tc20_copy_passes(self):
        self.assertEqual([], self.errors())

    def test_tc21_renamed_bone(self):
        def rename(g):
            for b in g["minecraft:geometry"][0]["bones"]:
                if b["name"] == "jaw":
                    b["name"] = "jaw2"
        self.edit("geo", rename)
        errs = self.errors()
        self.assertTrue(any("缺少骨骼 jaw" in e for e in errs), errs)
        self.assertTrue(any("未声明的骨骼 jaw2" in e for e in errs), errs)

    def test_tc21_reparented_bone(self):
        def reparent(g):
            for b in g["minecraft:geometry"][0]["bones"]:
                if b["name"] == "hand_l":
                    b["parent"] = "torso"
        self.edit("geo", reparent)
        self.assertReports("hand_l 的父级应为 forearm_l")

    def test_tc21_pivot_tolerance(self):
        def nudge(delta):
            def fn(g):
                for b in g["minecraft:geometry"][0]["bones"]:
                    if b["name"] == "head":
                        b["pivot"][1] += delta
            return fn
        self.edit("geo", nudge(0.005))
        self.assertEqual([], [e for e in self.errors() if "枢轴" in e and "哈希" not in e and "同步" not in e])
        self.edit("geo", nudge(0.1))
        self.assertReports("head 的枢轴应为")

    def test_tc22_locator_mismatch(self):
        self.edit("rig", lambda r: r["locators"].__setitem__("snort", [0, 34, 9]))
        self.assertReports("定位点 snort 在骨架数据中为")

    def test_tc22_missing_locator_in_geo(self):
        def drop(g):
            for b in g["minecraft:geometry"][0]["bones"]:
                (b.get("locators") or {}).pop("axe_grip_l", None)
        self.edit("geo", drop)
        self.assertReports("geo 缺少定位点 axe_grip_l")

    def test_tc23_uv_out_of_bounds(self):
        size = {}

        def push(g):
            desc = g["minecraft:geometry"][0]["description"]
            size["w"], size["h"] = desc["texture_width"], desc["texture_height"]
            cube = g["minecraft:geometry"][0]["bones"][1]["cubes"][0]
            face = next(iter(cube["uv"].values()))
            face["uv"] = [size["w"] - 6, size["h"] - 6]
            face["uv_size"] = [10, 10]
        self.edit("geo", push)
        self.assertReports(f"超出 {size['w']}×{size['h']}")

    def test_tc23_uv_partial_overlap(self):
        def shift(g):
            bones = {b["name"]: b for b in g["minecraft:geometry"][0]["bones"]}
            head = bones["head"]["cubes"][0]["uv"]["north"]
            torso = bones["torso"]["cubes"][0]["uv"]["north"]
            head["uv"] = [torso["uv"][0] + 20, torso["uv"][1] + 2]  # 右侧伸出 torso，部分交叉
        self.edit("geo", shift)
        self.assertReports("部分重叠")

    def test_tc24_missing_animation(self):
        self.edit("animation", lambda a: a["animations"].pop("charge_run"))
        self.assertReports("缺少动画 charge_run")

    def test_tc24_wrong_loop_and_length(self):
        def bad(a):
            a["animations"]["charge_windup"]["loop"] = True
            a["animations"]["idle"]["animation_length"] = 2.0
        self.edit("animation", bad)
        errs = self.errors()
        self.assertTrue(any("charge_windup 的循环方式应为 hold_on_last_frame" in e for e in errs), errs)
        self.assertTrue(any("idle 的时长应为 3.0" in e for e in errs), errs)

    def test_tc24_animation_unknown_bone(self):
        self.edit("animation", lambda a: a["animations"]["idle"]["bones"].__setitem__("wing", {"rotation": [0, 0, 0]}))
        self.assertReports("引用了不存在的骨骼 wing")

    def test_tc25_hash_out_of_sync(self):
        f = self.tmp / ENTRY["geo"]
        f.write_text(f.read_text(encoding="utf-8") + "\n", encoding="utf-8")
        self.assertReports("工程与导出不同步")

    def test_tc25_hash_missing(self):
        self.edit("bbmodel", lambda m: m.pop("mythic_age"))
        self.assertReports("没有记录")

    def test_collision_mismatch(self):
        self.edit("rig", lambda r: r["collision"].__setitem__("height", 3.0))
        self.assertReports("碰撞")


if __name__ == "__main__":
    unittest.main()
