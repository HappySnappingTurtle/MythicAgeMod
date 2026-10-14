"""recolor（原版造型换色）的单元测试。运行：python3 -m unittest tools/texgen/test_recolor.py"""
import json
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import recolor  # noqa: E402
import texgen  # noqa: E402


def fake_vanilla(root: Path):
    """一张 3 色的「锭」与一张 2 色的「石头」，以及一张含石头色的「矿石」。"""
    tex = root / "assets/minecraft/textures"
    (tex / "item").mkdir(parents=True)
    (tex / "block").mkdir(parents=True)
    ingot = Image.new("RGBA", (4, 1))
    ingot.putdata([(0, 0, 0, 0), (40, 40, 40, 255), (120, 120, 120, 255), (230, 230, 230, 255)])
    ingot.save(tex / "item/iron_ingot.png")
    stone = Image.new("RGBA", (2, 1))
    stone.putdata([(100, 100, 100, 255), (110, 110, 110, 255)])
    stone.save(tex / "block/stone.png")
    ore = Image.new("RGBA", (4, 1))
    ore.putdata([(100, 100, 100, 255), (110, 110, 110, 255), (200, 150, 120, 255), (90, 60, 50, 255)])
    ore.save(tex / "block/iron_ore.png")


def spec(path, rid, base, ramp, keep=None):
    data = {"template": "recolor", "id": rid, "base": base, "ramp": ramp}
    if keep:
        data["keep"] = keep
    return recolor.load_recolor_spec(Path(path), data)


class RecolorTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        fake_vanilla(self.root)
        self.vanilla = recolor.VanillaAssets(self.root)

    def tearDown(self):
        self.tmp.cleanup()

    def test_key_sorted_by_luminance_and_ramp_interpolated(self):
        s = spec("a.json", "tin_ingot", "minecraft:item/iron_ingot", ["#000010", "#ffffff"])
        out = recolor.build([s], self.vanilla)
        key = list(out.keys[s.key_name].getdata())
        self.assertEqual([(40, 40, 40, 255), (120, 120, 120, 255), (230, 230, 230, 255)], key)
        pal = [p[:3] for p in out.palettes["tin_ingot"].getdata()]
        self.assertEqual((0, 0, 16), pal[0])
        self.assertEqual((255, 255, 255), pal[-1])
        self.assertEqual(3, len(pal))

    def test_keep_leaves_stone_colors(self):
        s = spec("a.json", "tin_ore", "minecraft:block/iron_ore", ["#203040", "#a0b0c0"], keep="minecraft:block/stone")
        out = recolor.build([s], self.vanilla)
        self.assertEqual(2, out.keys[s.key_name].width)
        preview = list(out.previews[s.sprite].getdata())
        self.assertEqual((100, 100, 100, 255), preview[0], "石头底色保持不变")
        self.assertNotEqual((200, 150, 120, 255), preview[2], "矿粒颜色被替换")

    def test_atlas_groups_by_base_and_names_sprites(self):
        a = spec("a.json", "tin_ingot", "minecraft:item/iron_ingot", ["#000010", "#ffffff"])
        b = spec("b.json", "bronze_ingot", "minecraft:item/iron_ingot", ["#301000", "#ffe0a0"])
        out = recolor.build([a, b], self.vanilla)
        self.assertEqual(1, len(out.atlas["sources"]))
        src = out.atlas["sources"][0]
        self.assertEqual("paletted_permutations", src["type"])
        self.assertEqual({"mythic_age_bronze_ingot", "mythic_age_tin_ingot"}, set(src["permutations"]))
        self.assertEqual("minecraft:item/iron_ingot_mythic_age_tin_ingot", a.sprite)

    def test_duplicate_id_and_bad_specs(self):
        a = spec("a.json", "x", "minecraft:item/iron_ingot", ["#000000", "#ffffff"])
        with self.assertRaises(recolor.RecolorError):
            recolor.build([a, a], self.vanilla)
        for data, needle in (({"id": "X", "base": "minecraft:item/iron_ingot", "ramp": ["#000000", "#ffffff"]}, "id"),
                             ({"id": "x", "base": "iron_ingot", "ramp": ["#000000", "#ffffff"]}, "base"),
                             ({"id": "x", "base": "minecraft:item/iron_ingot", "ramp": ["#000000"]}, "ramp"),
                             ({"id": "x", "base": "minecraft:item/iron_ingot", "ramp": ["red", "#ffffff"]}, "颜色")):
            with self.assertRaises(recolor.RecolorError) as ctx:
                recolor.load_recolor_spec(Path("bad.json"), data)
            self.assertIn(needle, str(ctx.exception))
        with self.assertRaises(recolor.RecolorError):
            recolor.build([spec("c.json", "y", "minecraft:item/missing", ["#000000", "#ffffff"])], self.vanilla)

    def test_cli_writes_palettes_and_atlas(self):
        specs = self.root / "specs"
        specs.mkdir()
        (specs / "tin.json").write_text(json.dumps({"template": "recolor", "id": "tin_ingot",
                                                    "base": "minecraft:item/iron_ingot",
                                                    "ramp": ["#000010", "#ffffff"]}), encoding="utf-8")
        out = self.root / "out/textures"
        atlas = self.root / "out/blocks.json"
        rc = texgen.main(["--spec-dir", str(specs), "--out", str(out), "--atlas", str(atlas),
                          "--vanilla", str(self.root)])
        self.assertEqual(0, rc)
        self.assertTrue((out / "palettes/tin_ingot.png").exists())
        self.assertTrue((out / "palettes/key/minecraft_item_iron_ingot.png").exists())
        self.assertEqual(1, len(json.loads(atlas.read_text(encoding="utf-8"))["sources"]))
        # 删除规格后再运行：多余的调色板被清理
        (specs / "tin.json").unlink()
        texgen.main(["--spec-dir", str(specs), "--out", str(out), "--atlas", str(atlas), "--vanilla", str(self.root)])
        self.assertFalse((out / "palettes/tin_ingot.png").exists())
        self.assertEqual([], json.loads(atlas.read_text(encoding="utf-8"))["sources"])


if __name__ == "__main__":
    unittest.main()
