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


class RelicCardTest(unittest.TestCase):
    """T16：圣物卡校验。"""

    def run_with(self, card: dict, *, tag=True, worn_lang=True, folder="greek", second=False):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            fixture(root)
            gen = root / "neoforge/src/generated/resources"
            main = root / "neoforge/src/main/resources"
            write(gen / "assets/mythic_age/models/item/crown.json", {"parent": "item/generated"})
            if tag:
                write(gen / "data/curios/tags/item/mythic_relic.json", {"values": ["mythic_age:crown"]})
            write(main / f"data/mythic_age/mythic_age/relic/{folder}/crown.json", card)
            if second:
                write(main / f"data/mythic_age/mythic_age/relic/{folder}/crown2.json", card)
            for loc in cc.LOCALES:
                lang = {"item.mythic_age.crown.offered": "x"}
                if worn_lang:
                    lang["item.mythic_age.crown.worn"] = "x"
                write(main / f"assets/mythic_age/lang/{loc}.json", lang)
            report = cc.Report()
            cc.check_relics(cc.Paths(root), report)
            return report.errors

    CARD = {"item": "mythic_age:crown", "civ": "greek"}

    def test_valid(self):
        self.assertEqual([], self.run_with(self.CARD))

    def test_civ_mismatch_and_folder(self):
        errors = self.run_with(self.CARD, folder="lemuria")
        self.assertTrue(any("不是五文明之一" in e for e in errors))
        self.assertTrue(any("与目录不一致" in e for e in errors))

    def test_missing_item_tag_lang_duplicate(self):
        self.assertTrue(any("未实现" in e for e in self.run_with({"item": "mythic_age:nope", "civ": "greek"})))
        self.assertTrue(any("标签" in e for e in self.run_with(self.CARD, tag=False)))
        self.assertTrue(any(".worn" in e for e in self.run_with(self.CARD, worn_lang=False)))
        self.assertTrue(any("已被" in e for e in self.run_with(self.CARD, second=True)))


class VillageDialogueTest(unittest.TestCase):
    """T21 阶段 ④：有缺料条件的职业要有三语「本职缺料」与「委托」两句。"""

    def run_with(self, short_when, keys):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            main = root / "neoforge/src/main/resources"
            write(main / "data/mythic_age/mythic_age/village_profession/greek/smith.json", {"shortWhen": short_when})
            for loc in cc.LOCALES:
                write(main / f"assets/mythic_age/lang/{loc}.json", {k: "x" for k in keys})
            report = cc.Report()
            cc.check_village_dialogue(cc.Paths(root), report)
            return report.errors

    BOTH = ["mythic_age.village.say.greek.smith.short", "mythic_age.village.say.greek.smith.quest"]

    def test_complete(self):
        self.assertEqual([], self.run_with({"items": ["mythic_age:tin_ingot"]}, self.BOTH))

    def test_missing_quest_line(self):
        errors = self.run_with({"food": True}, self.BOTH[:1])
        self.assertEqual(3, len(errors), errors)
        self.assertTrue(all(".quest" in e for e in errors))

    def test_no_short_when_needs_nothing(self):
        self.assertEqual([], self.run_with({}, []))


class RationDialogueTest(unittest.TestCase):
    """TC-GA7-30～31：声明的物品必须在本文明口粮表，不能把小麦说成口粮。"""

    def run_check(self, mentions):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            paths = cc.Paths(root)
            data = paths.main / "data/mythic_age/mythic_age"
            key = "mythic_age.village.say.greek.farmer.quest"
            write(data / "village_civ/greek.json",
                  {"rations": {"portions": {"minecraft:bread": 1, "mythic_age:olive_flatbread": 1}}})
            write(data / "village_profession/greek/farmer.json",
                  {"dialogue": {"quest": {"mentions": mentions}}})
            for loc in cc.LOCALES:
                write(paths.gen / f"assets/mythic_age/lang/{loc}.json", {key: "台词"})
            report = cc.Report()
            cc.check_ration_dialogue(paths, report)
            return report.errors

    def test_ga7_30_ration_mentions_pass(self):
        self.assertEqual([], self.run_check(["minecraft:bread", "mythic_age:olive_flatbread"]))

    def test_ga7_31_wheat_is_rejected(self):
        errors = self.run_check(["minecraft:bread", "minecraft:wheat"])
        self.assertEqual(1, len(errors), errors)
        self.assertIn("mythic_age.village.say.greek.farmer.quest", errors[0])
        self.assertIn("minecraft:wheat", errors[0])
        self.assertIn("rations.portions", errors[0])


class WarningFxTest(unittest.TestCase):
    """TC-T24-94：预警代码引用装饰特效开关时报错。"""

    def test_warning_dir_must_not_use_client_fx(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            boss = root / cc.WARNING_FX_DIRS[0]
            boss.mkdir(parents=True)
            (boss / "Arena.java").write_text("class Arena { void warn() { spawnWarning(); } }", encoding="utf-8")
            r = cc.Report()
            cc.check_warning_fx(root, r)
            self.assertEqual([], r.errors)
            (boss / "Bad.java").write_text("class Bad { int n = ClientFx.decorativeCount(8); }", encoding="utf-8")
            cc.check_warning_fx(root, r)
            self.assertEqual(1, len(r.errors))
            self.assertIn("Bad.java", r.errors[0])


class HandbookTest(unittest.TestCase):
    """TC-T24-90～92：手札三语一致、引用存在、锁页进度存在。"""

    def make(self, root: Path, *, ja_missing=False, bad_item=False, bad_adv=False):
        write(root / "neoforge/src/generated/resources/assets/mythic_age/lang/zh_cn.json", {"item.mythic_age.thread": "线团"})
        write(root / "neoforge/src/generated/resources/assets/mythic_age/models/item/thread.json", {})
        write(root / "neoforge/src/generated/resources/data/mythic_age/advancement/bestiary/greek/harpy/sighted.json", {})
        book = root / "neoforge/src/main/resources/assets/mythic_age" / cc.HANDBOOK_REL
        entry = {"name": "x", "icon": "mythic_age:thread", "category": "mythic_age:trials",
                 "advancement": "mythic_age:bestiary/greek/harpy/" + ("nope" if bad_adv else "sighted"),
                 "pages": [{"type": "patchouli:spotlight", "item": "mythic_age:" + ("ghost" if bad_item else "thread"), "text": "t"}]}
        for lang in cc.HANDBOOK_LANGS:
            if ja_missing and lang == "ja_jp":
                write(book / lang / "categories/trials.json", {"name": "c"})
                continue
            write(book / lang / "categories/trials.json", {"name": "c"})
            write(book / lang / "entries/trials/thread.json", entry)

    def run_check(self, **kw):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            self.make(root, **kw)
            r = cc.Report()
            cc.check_handbook(cc.Paths(root), r)
            return r.errors

    def test_consistent_book_passes(self):
        self.assertEqual([], self.run_check())

    def test_missing_language_file(self):
        errs = self.run_check(ja_missing=True)
        self.assertTrue(any("ja_jp" in e and "thread.json" in e for e in errs), errs)

    def test_unknown_item(self):
        self.assertTrue(any("ghost" in e for e in self.run_check(bad_item=True)))

    def test_unknown_advancement(self):
        self.assertTrue(any("nope" in e for e in self.run_check(bad_adv=True)))


if __name__ == "__main__":
    unittest.main()
