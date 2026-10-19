"""check_content 的单元测试（T6 测试用例文档 §2，TC-T6-10 至 TC-T6-15）。

运行：python3 -m unittest tools/test_check_content.py
"""
import json
import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_content as cc  # noqa: E402

DESIGN = {
    "recipe_contract_examples": [
        {"id": "tin_smelting", "type": "smelting", "inputs": {"mythic_age:raw_tin": 1},
         "outputs": {"mythic_age:tin_ingot": 1}},
        {"id": "bronze_mix", "type": "crafting", "inputs": {"minecraft:copper_ingot": 3, "mythic_age:tin_ingot": 1},
         "outputs": {"mythic_age:bronze_mix": 4}, "unlock": "greek_classical"},
        {"id": "libation", "type": "crafting", "inputs": {"minecraft:apple": 1, "design:water_bottle": 2},
         "outputs": {"mythic_age:libation": 2}},
        {"id": "not_yet", "type": "crafting", "inputs": {"minecraft:stick": 1}, "outputs": {"mythic_age:nope": 1}},
    ]
}


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")


def fixture(root: Path, *, bronze_count=4, drop_ja_key=None, missing_texture=False, bad_size=False,
            power_size=None):
    gen = root / "neoforge/src/generated/resources"
    main = root / "neoforge/src/main/resources"
    if power_size:
        p = main / "assets/mythic_age/textures/block/power/lure_stone_side.png"
        p.parent.mkdir(parents=True, exist_ok=True)
        Image.new("RGBA", power_size).save(p)
    items = ["raw_tin", "tin_ingot", "bronze_mix", "libation"]
    for it in items:
        write(gen / f"assets/mythic_age/models/item/{it}.json",
              {"parent": "item/generated", "textures": {"layer0": f"mythic_age:item/{it}"}})
        if not (missing_texture and it == "libation"):
            size = (24, 24) if bad_size and it == "raw_tin" else (16, 16)
            p = main / f"assets/mythic_age/textures/item/{it}.png"
            p.parent.mkdir(parents=True, exist_ok=True)
            Image.new("RGBA", size).save(p)
    for loc in cc.LOCALES:
        lang = {f"item.mythic_age.{it}": f"{it}-{loc}" for it in items}
        if loc == "ja_jp" and drop_ja_key:
            lang.pop(drop_ja_key)
        write(gen / f"assets/mythic_age/lang/{loc}.json", lang)
    write(gen / "data/c/tags/item/ingots/tin.json", {"values": ["mythic_age:tin_ingot"]})
    write(gen / "data/mythic_age/recipe/tin_ingot_from_smelting.json",
          {"type": "minecraft:smelting", "ingredient": {"item": "mythic_age:raw_tin"},
           "result": {"id": "mythic_age:tin_ingot"}, "cookingtime": 200, "experience": 0.7})
    write(gen / "data/mythic_age/recipe/bronze_mix.json",
          {"type": "mythic_age:age_gated", "civilization": "greek", "age": "classical",
           "recipe": {"type": "minecraft:crafting_shapeless",
                      "ingredients": [{"tag": "c:ingots/copper"}] * 3 + [{"tag": "c:ingots/tin"}],
                      "result": {"id": "mythic_age:bronze_mix", "count": bronze_count}}})
    water = {"type": "neoforge:components", "items": "minecraft:potion",
             "components": {"minecraft:potion_contents": {"potion": "minecraft:water"}}}
    write(gen / "data/mythic_age/recipe/libation.json",
          {"type": "minecraft:crafting_shapeless", "ingredients": [{"item": "minecraft:apple"}, water, water],
           "result": {"id": "mythic_age:libation", "count": 2}})
    design = root / "design.json"
    write(design, DESIGN)
    return design


class CheckContentTest(unittest.TestCase):

    def run_fixture(self, **kw):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            design = fixture(root, **kw)
            return cc.run(root, design)

    def test_tc_t6_10_consistent_fixture_passes(self):
        r = self.run_fixture()
        self.assertEqual([], r.errors)
        self.assertTrue(any("尚未实现" in n for n in r.notes))

    def test_tc_t6_11_wrong_count_is_reported(self):
        r = self.run_fixture(bronze_count=3)
        self.assertTrue(any("bronze_mix" in e and "不一致" in e for e in r.errors), r.errors)
        with tempfile.TemporaryDirectory() as d:
            design = fixture(Path(d), bronze_count=3)
            self.assertEqual(1, cc.main(["--root", d, "--design", str(design)]))

    def test_tc_t6_12_missing_texture(self):
        r = self.run_fixture(missing_texture=True)
        self.assertTrue(any("libation" in e and "不存在" in e for e in r.errors), r.errors)

    def test_tc_t6_13_missing_japanese(self):
        r = self.run_fixture(drop_ja_key="item.mythic_age.tin_ingot")
        self.assertTrue(any("ja_jp 缺少键 item.mythic_age.tin_ingot" in e for e in r.errors), r.errors)

    def test_tc_t6_14_bad_texture_size(self):
        r = self.run_fixture(bad_size=True)
        self.assertTrue(any("raw_tin.png" in e and "24×24" in e for e in r.errors), r.errors)

    def test_power_block_texture_may_be_32(self):
        """神力展示方块（block/power/）允许 32×32；其他尺寸、非正方形仍报错。"""
        self.assertEqual([], self.run_fixture(power_size=(32, 32)).errors)
        self.assertEqual([], self.run_fixture(power_size=(16, 16)).errors)
        for size in ((24, 24), (32, 16)):
            r = self.run_fixture(power_size=size)
            self.assertTrue(any("lure_stone_side.png" in e and "神力展示方块" in e for e in r.errors), r.errors)

    def test_tc_t6_15_missing_design_is_skipped(self):
        with tempfile.TemporaryDirectory() as d:
            fixture(Path(d))
            r = cc.run(Path(d), Path(d) / "no_such.json")
            self.assertEqual([], r.errors)
            self.assertTrue(any("跳过" in n for n in r.notes))

    def test_vanilla_tag_not_folded(self):
        """往原版标签里追加一个本模组成员（如 #minecraft:wooden_slabs 加橄榄木台阶）后，不应把标签折叠成该成员。"""
        tags = {"#minecraft:wooden_slabs": {"mythic_age:olive_slab"}, "#c:ingots/tin": {"mythic_age:tin_ingot"}}
        self.assertEqual("#minecraft:wooden_slabs", cc.ingredient_key({"tag": "minecraft:wooden_slabs"}, tags))
        self.assertEqual("mythic_age:tin_ingot", cc.ingredient_key({"tag": "c:ingots/tin"}, tags))

    def recolor_fixture(self, root: Path, perm_colors, extra_model_ref=None):
        main = root / "neoforge/src/main/resources"
        key = [(10, 10, 10), (120, 120, 120), (240, 240, 240)]
        for name, colors in (("key/k", key), ("tin", perm_colors)):
            p = main / f"assets/mythic_age/textures/palettes/{name}.png"
            p.parent.mkdir(parents=True, exist_ok=True)
            img = Image.new("RGBA", (len(colors), 1))
            img.putdata([c + (255,) for c in colors])
            img.save(p)
        write(main / "assets/minecraft/atlases/blocks.json", {"sources": [{
            "type": "paletted_permutations", "textures": ["minecraft:item/iron_ingot"],
            "palette_key": "mythic_age:palettes/key/k",
            "permutations": {"mythic_age_tin": "mythic_age:palettes/tin"}}]})
        ref = extra_model_ref or "minecraft:item/iron_ingot_mythic_age_tin"
        write(root / "neoforge/src/generated/resources/assets/mythic_age/models/item/tin_ingot.json",
              {"parent": "item/generated", "textures": {"layer0": ref}})

    def test_recolor_ok_and_unregistered_sprite(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            design = fixture(root)
            self.recolor_fixture(root, [(40, 20, 80), (90, 140, 200), (200, 230, 255)])
            self.assertEqual([], cc.run(root, design).errors)
            self.recolor_fixture(root, [(40, 20, 80), (90, 140, 200), (200, 230, 255)],
                                 extra_model_ref="minecraft:item/iron_ingot_mythic_age_bronze")
            self.assertTrue(any("没有在图集中登记" in e for e in cc.run(root, design).errors))

    def test_recolor_too_close_to_vanilla(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            design = fixture(root)
            self.recolor_fixture(root, [(12, 12, 12), (122, 120, 121), (238, 240, 240)])
            self.assertTrue(any("太接近" in e for e in cc.run(root, design).errors))

    def test_repository_passes(self):
        r = cc.run(cc.DEFAULT_ROOT, None)
        self.assertEqual([], r.errors)


if __name__ == "__main__":
    unittest.main()
