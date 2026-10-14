"""texgen 的单元测试（T6 测试用例文档 §1，TC-T6-01 至 TC-T6-07）。

运行：python3 -m unittest tools/texgen/test_texgen.py
"""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import texgen  # noqa: E402

PALETTES = {
    "ore_overlay": {"light": "#d9dee3", "mid": "#adb5be", "dark": "#7c8590", "shadow": "#55595e", "highlight": "#f3f6f8"},
    "ingot": {"outline": "#3b4049", "dark": "#7a828c", "mid": "#a6aeb7", "light": "#cdd3d9", "highlight": "#eef2f5", "accent": "#8892a2"},
    "raw_chunk": {"outline": "#34383e", "dark": "#6f7781", "mid": "#99a2ac", "light": "#c3cad1", "accent": "#e4e9ed"},
    "granular": {"outline": "#3a2a1c", "a_dark": "#7a4a2a", "a_mid": "#a0643a", "a_light": "#c8834f",
                 "b_dark": "#6f7781", "b_mid": "#99a2ac", "b_light": "#c3cad1"},
    "stone": {"dark": "#ddd6c7", "mid": "#e8e2d5", "light": "#f2eee5", "vein": "#c9c1b0"},
    "polished": {"dark": "#cfc8b8", "mid": "#e8e2d5", "light": "#f2eee5", "highlight": "#fbf9f4"},
    "bricks": {"mortar": "#bdb5a4", "dark": "#ddd6c7", "mid": "#e8e2d5", "light": "#f5f1e9"},
    "voronoi_masonry": {"joint": "#c4bba8", "dark": "#e0d9ca", "mid": "#ebe5d8", "light": "#f0ebe1", "highlight": "#f6f2ea"},
    "bottle": {"outline": "#3a1f14", "dark": "#7d3a20", "mid": "#a9532e", "light": "#c9764a", "band": "#2b2b2b", "stopper": "#8a6a3a"},
    "tile": {"outline": "#3a1f14", "dark": "#8a8278", "mid": "#a59d92", "light": "#bdb5aa"},
    "plate": {"outline": "#3a2614", "dark": "#7a5532", "mid": "#9c7044", "light": "#bd8e5c"},
}


def make_spec(template: str, seed: int = 1, **extra) -> texgen.Spec:
    data = {"template": template, "out": f"test/{template}", "seed": seed, "palette": PALETTES[template]}
    data.update(extra)
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8") as f:
        json.dump(data, f)
    return texgen.load_spec(Path(f.name))


def make_spec_raw(data: dict) -> texgen.Spec:
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8") as f:
        json.dump(data, f)
    return texgen.load_spec(Path(f.name))


def pixels(img):
    flat = getattr(img, "get_flattened_data", None)
    return list(flat() if flat else img.getdata())


class TexgenTest(unittest.TestCase):

    def test_tc_t6_01_deterministic(self):
        for template in (t for t in texgen.TEMPLATES if t != "pixels"):
            a = texgen.render(make_spec(template, 7))
            b = texgen.render(make_spec(template, 7))
            self.assertEqual(a.tobytes(), b.tobytes(), template)

    def test_tc_t6_02_seed_changes_output(self):
        for template in ("ore_overlay", "stone", "raw_chunk", "bricks"):
            a = texgen.render(make_spec(template, 1))
            b = texgen.render(make_spec(template, 2))
            self.assertNotEqual(a.tobytes(), b.tobytes(), template)

    def test_tc_t6_03_ore_overlay(self):
        img = texgen.render(make_spec("ore_overlay", 5, params={"clusters": 8, "clusterSize": [11, 18]}))
        self.assertEqual((32, 32), img.size)
        self.assertEqual("RGBA", img.mode)
        px = pixels(img)
        opaque = [p for p in px if p[3] > 0]
        self.assertTrue(all(p[3] in (0, 255) for p in px), "只能是完全透明或完全不透明")
        # 8 簇 × 11–18 像素，再加阴影与高光，约占 10%–40%
        self.assertGreater(len(opaque), 1024 * 0.10)
        self.assertLess(len(opaque), 1024 * 0.40)

    def test_tc_t6_04_item_templates_border_and_palette(self):
        for template in texgen.ITEM_TEMPLATES:
            spec = make_spec(template, 3)
            img = texgen.render(spec)
            self.assertEqual((32, 32), img.size)
            for i in range(32):
                for (x, y) in ((i, 0), (i, 31), (0, i), (31, i)):
                    self.assertEqual(0, img.getpixel((x, y))[3], f"{template} 边缘 ({x},{y}) 应透明")
            allowed = set(spec.palette.values())
            used = {p for p in pixels(img) if p[3] > 0}
            self.assertTrue(used <= allowed, f"{template} 使用了调色板以外的颜色：{used - allowed}")
            self.assertGreater(len(used), 2, template)

    def test_tc_t6_05_tiling(self):
        for template in texgen.TILING_TEMPLATES:
            img = texgen.render(make_spec(template, 9))
            px = img.load()

            def diff(a, b):
                return max(abs(a[i] - b[i]) for i in range(3))

            inner = max(max(diff(px[x, y], px[x + 1, y]) for x in range(31)) for y in range(32))
            seam = max(max(diff(px[31, y], px[0, y]) for y in range(32)),
                       max(diff(px[x, 31], px[x, 0]) for x in range(32)))
            self.assertLessEqual(seam, max(inner, 1), template)

    def test_tc_t6_06_bad_specs(self):
        cases = [
            ({"out": "a", "seed": 1, "palette": {}}, "template"),
            ({"template": "nope", "out": "a", "seed": 1, "palette": {}}, "未知模板"),
            ({"template": "stone", "out": "a", "seed": 1, "palette": {"dark": "red", "mid": "#000000", "light": "#ffffff"}}, "palette.dark"),
            ({"template": "stone", "out": "a", "seed": 1, "palette": {"dark": "#000000"}}, "缺少颜色"),
            ({"template": "stone", "out": "../x", "seed": 1, "palette": PALETTES["stone"]}, "out"),
            ({"template": "stone", "out": "a", "seed": "1", "palette": PALETTES["stone"]}, "seed"),
            ({"template": "stone", "out": "a", "seed": 1, "size": 24, "palette": PALETTES["stone"]}, "size"),
        ]
        with tempfile.TemporaryDirectory() as d:
            out = Path(d) / "out"
            for i, (data, needle) in enumerate(cases):
                f = Path(d) / f"bad{i}.json"
                f.write_text(json.dumps(data), encoding="utf-8")
                with self.assertRaises(texgen.SpecError) as ctx:
                    texgen.load_spec(f)
                self.assertIn(needle, str(ctx.exception))
                self.assertIn(str(f), str(ctx.exception))
                # 命令行：规格错误时不写出任何文件
                self.assertEqual(2, texgen.main([str(f), "--out", str(out)]))
            self.assertFalse(out.exists())

    def test_tc_t6_07_no_silent_overwrite(self):
        with tempfile.TemporaryDirectory() as d:
            target = Path(d) / "x.png"
            a = texgen.render(make_spec("stone", 1))
            b = texgen.render(make_spec("stone", 2))
            self.assertEqual("created", texgen.write_png(a, target, False))
            self.assertEqual("unchanged", texgen.write_png(a, target, False))
            with self.assertRaises(texgen.SpecError):
                texgen.write_png(b, target, False)
            self.assertEqual("overwritten", texgen.write_png(b, target, True))

    def test_pixels_template(self):
        rows = ["." * 16] * 16
        rows = rows[:5] + ["....aab.........".ljust(16, ".")] + rows[6:]
        spec = make_spec_raw({"template": "pixels", "out": "t/p", "size": 16,
                              "palette": {"a": "#112233", "b": "#445566"}, "params": {"rows": rows}})
        img = texgen.render(spec)
        self.assertEqual((16, 16), img.size)
        self.assertEqual((0x11, 0x22, 0x33, 255), img.getpixel((4, 5)))
        self.assertEqual((0x44, 0x55, 0x66, 255), img.getpixel((6, 5)))
        self.assertEqual(0, img.getpixel((0, 0))[3])

    def test_pixels_rejects_bad_rows(self):
        base = {"template": "pixels", "out": "t/p", "size": 16, "palette": {"a": "#112233"}}
        for rows, needle in ((["." * 16] * 15, "共 16 行"), (["." * 15] + ["." * 16] * 15, "16 个字符"),
                             (["z" + "." * 15] + ["." * 16] * 15, "不在调色板")):
            with self.assertRaises(texgen.SpecError) as ctx:
                make_spec_raw(dict(base, params={"rows": rows}))
            self.assertIn(needle, str(ctx.exception))
        with self.assertRaises(texgen.SpecError):
            make_spec_raw(dict(base, palette={"ab": "#112233"}, params={"rows": ["." * 16] * 16}))

    def test_repository_specs_are_valid(self):
        specs = sorted(texgen.SPEC_DIR.rglob("*.json"))
        self.assertTrue(specs)
        outs = set()
        for p in specs:
            if texgen.is_recolor(p):
                continue
            s = texgen.load_spec(p)
            self.assertNotIn(s.out, outs, f"重复的输出路径 {s.out}")
            outs.add(s.out)


if __name__ == "__main__":
    unittest.main()
