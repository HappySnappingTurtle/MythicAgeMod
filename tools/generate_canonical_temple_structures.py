#!/usr/bin/env python3
"""Generate canonical Mythic Age temple structure NBT files."""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

from minecraft_structure_nbt import (
    TAG_COMPOUND,
    compound_list,
    compound_tag,
    empty_compound,
    int_list,
    int_tag,
    string_tag,
    validate_structure_template,
    write_root_compound,
)


ROOT = Path(__file__).resolve().parents[1]
STRUCTURES_ROOT = ROOT / "src/main/resources/data/mythic_age/structures/village"
MANIFEST_PATH = ROOT / "docs/preview_assets/structure_validation/generated_temple_manifest.json"
DATA_VERSION = 3465  # Minecraft 1.20.1

SIZES = {
    "small": 0,
    "medium": 1,
    "large": 2,
}


@dataclass(frozen=True)
class CivilizationTheme:
    civilization: str
    bricks: str
    slab: str
    stairs: str
    pillar: str
    relief: str
    statue_base: str
    statue_legs: str
    statue_body: str
    statue_head: str
    base_width: int
    base_height: int
    base_length: int
    roof_style: str
    entrance_width: int = 3
    extra_block: str | None = None


THEMES: dict[str, CivilizationTheme] = {
    "greek": CivilizationTheme(
        civilization="greek",
        bricks="mythic_age:temple_greek_bricks",
        slab="mythic_age:temple_greek_slab",
        stairs="mythic_age:temple_greek_stairs",
        pillar="mythic_age:temple_greek_pillar",
        relief="mythic_age:temple_greek_relief",
        statue_base="mythic_age:statue_greek_base",
        statue_legs="mythic_age:statue_greek_legs",
        statue_body="mythic_age:statue_greek_body",
        statue_head="mythic_age:statue_greek_head",
        base_width=11,
        base_height=7,
        base_length=11,
        roof_style="flat",
    ),
    "egyptian": CivilizationTheme(
        civilization="egyptian",
        bricks="mythic_age:temple_egyptian_bricks",
        slab="mythic_age:temple_egyptian_slab",
        stairs="mythic_age:temple_egyptian_stairs",
        pillar="mythic_age:temple_egyptian_pillar",
        relief="mythic_age:temple_egyptian_relief",
        statue_base="mythic_age:statue_egyptian_base",
        statue_legs="mythic_age:statue_egyptian_legs",
        statue_body="mythic_age:statue_egyptian_body",
        statue_head="mythic_age:statue_egyptian_head",
        base_width=11,
        base_height=7,
        base_length=11,
        roof_style="stepped",
        extra_block="mythic_age:egyptian_statue_pedestal",
    ),
    "chinese": CivilizationTheme(
        civilization="chinese",
        bricks="mythic_age:temple_chinese_bricks",
        slab="mythic_age:temple_chinese_slab",
        stairs="mythic_age:temple_chinese_stairs",
        pillar="mythic_age:temple_chinese_pillar",
        relief="mythic_age:temple_chinese_relief",
        statue_base="mythic_age:statue_chinese_base",
        statue_legs="mythic_age:statue_chinese_legs",
        statue_body="mythic_age:statue_chinese_body",
        statue_head="mythic_age:statue_chinese_head",
        base_width=13,
        base_height=8,
        base_length=13,
        roof_style="tiered",
    ),
    "atlantean": CivilizationTheme(
        civilization="atlantean",
        bricks="mythic_age:temple_atlantean_bricks",
        slab="mythic_age:temple_atlantean_slab",
        stairs="mythic_age:temple_atlantean_stairs",
        pillar="mythic_age:temple_atlantean_pillar",
        relief="mythic_age:temple_atlantean_relief",
        statue_base="mythic_age:statue_atlantean_base",
        statue_legs="mythic_age:statue_atlantean_legs",
        statue_body="mythic_age:statue_atlantean_body",
        statue_head="mythic_age:statue_atlantean_head",
        base_width=15,
        base_height=7,
        base_length=15,
        roof_style="crystal",
    ),
    "nordic": CivilizationTheme(
        civilization="nordic",
        bricks="mythic_age:temple_nordic_bricks",
        slab="mythic_age:temple_nordic_slab",
        stairs="mythic_age:temple_nordic_stairs",
        pillar="mythic_age:temple_nordic_pillar",
        relief="mythic_age:temple_nordic_relief",
        statue_base="mythic_age:statue_nordic_base",
        statue_legs="mythic_age:statue_nordic_legs",
        statue_body="mythic_age:statue_nordic_body",
        statue_head="mythic_age:statue_nordic_head",
        base_width=9,
        base_height=6,
        base_length=11,
        roof_style="longhouse",
    ),
}


BlockMap = dict[tuple[int, int, int], str]


def dimensions(theme: CivilizationTheme, size: str) -> tuple[int, int, int]:
    scale = SIZES[size]
    return (
        theme.base_width + scale * 4,
        theme.base_height + scale * 2,
        theme.base_length + scale * 4,
    )


def place_box(blocks: BlockMap, start: tuple[int, int, int], end: tuple[int, int, int], block: str) -> None:
    sx, sy, sz = start
    ex, ey, ez = end
    for x in range(sx, ex + 1):
        for y in range(sy, ey + 1):
            for z in range(sz, ez + 1):
                blocks[(x, y, z)] = block


def place_statue(blocks: BlockMap, x: int, y: int, z: int, theme: CivilizationTheme) -> None:
    blocks[(x, y, z)] = theme.statue_base
    blocks[(x, y + 1, z)] = theme.statue_legs
    blocks[(x, y + 2, z)] = theme.statue_body
    blocks[(x, y + 3, z)] = theme.statue_head


def add_roof(blocks: BlockMap, width: int, height: int, length: int, theme: CivilizationTheme, size: str) -> None:
    roof_y = height - 1
    if theme.roof_style == "stepped":
        layers = min(3 + SIZES[size], height - 2)
        for layer in range(layers):
            y = roof_y - layer
            inset = 1 + layer
            if inset >= width // 2 or inset >= length // 2:
                continue
            place_box(blocks, (inset, y, inset), (width - inset - 1, y, length - inset - 1), theme.stairs)
        return

    if theme.roof_style == "tiered":
        place_box(blocks, (1, roof_y - 1, 1), (width - 2, roof_y - 1, length - 2), theme.stairs)
        place_box(blocks, (2, roof_y, 2), (width - 3, roof_y, length - 3), theme.stairs)
        blocks[(width // 2, roof_y + 1, length // 2)] = theme.relief
        return

    if theme.roof_style == "crystal":
        place_box(blocks, (2, roof_y, 2), (width - 3, roof_y, length - 3), theme.stairs)
        for offset in range(0, min(width, length) // 2, 3):
            x = width // 2
            z1 = offset
            z2 = length - 1 - offset
            if 0 <= z1 < length:
                blocks[(x, roof_y + 1, z1)] = theme.relief
            if 0 <= z2 < length:
                blocks[(x, roof_y + 1, z2)] = theme.relief
        return

    if theme.roof_style == "longhouse":
        for z in range(1, length - 1):
            for x in range(1, width - 1):
                ridge = abs(x - width // 2)
                y = roof_y - min(ridge, 2)
                blocks[(x, y, z)] = theme.stairs
        return

    place_box(blocks, (1, roof_y, 1), (width - 2, roof_y, length - 2), theme.stairs)


def build_temple_blocks(theme: CivilizationTheme, size: str) -> tuple[tuple[int, int, int], BlockMap]:
    width, height, length = dimensions(theme, size)
    blocks: BlockMap = {}
    entrance_half = theme.entrance_width // 2
    center_x = width // 2
    center_z = length // 2

    place_box(blocks, (0, 0, 0), (width - 1, 0, length - 1), theme.bricks)
    place_box(blocks, (1, 1, 1), (width - 2, 1, length - 2), theme.slab)

    wall_top = max(3, height - 3)
    for y in range(1, wall_top + 1):
        for x in range(width):
            for z in range(length):
                outer = x == 0 or x == width - 1 or z == 0 or z == length - 1
                entrance = z == 0 and center_x - entrance_half <= x <= center_x + entrance_half and y <= 3
                if not outer or entrance:
                    continue
                blocks[(x, y, z)] = theme.bricks

    pillar_positions = [
        (2, 2),
        (width - 3, 2),
        (2, length - 3),
        (width - 3, length - 3),
    ]
    if size != "small":
        pillar_positions.extend(
            [
                (center_x, 2),
                (center_x, length - 3),
                (2, center_z),
                (width - 3, center_z),
            ]
        )
    for x, z in pillar_positions:
        place_box(blocks, (x, 1, z), (x, wall_top, z), theme.pillar)

    for y in range(2, wall_top + 1):
        blocks[(center_x, y, length - 1)] = theme.relief
        if y == wall_top:
            blocks[(center_x, y, 0)] = theme.relief
    if size == "large":
        for x in (center_x - 2, center_x + 2):
            blocks[(x, wall_top, length - 1)] = theme.relief

    add_roof(blocks, width, height, length, theme, size)

    blocks[(center_x, 2, center_z)] = "mythic_age:relic_pedestal"
    place_statue(blocks, center_x, 1, 2, theme)
    if theme.extra_block:
        blocks[(center_x, 1, length - 3)] = theme.extra_block

    if size in {"medium", "large"}:
        for x in range(2, width - 2):
            blocks[(x, 1, center_z - 2)] = theme.slab
            blocks[(x, 1, center_z + 2)] = theme.slab
    if size == "large":
        place_box(blocks, (2, 0, 2), (width - 3, 0, length - 3), theme.bricks)
        for z in range(2, length - 2):
            blocks[(2, 1, z)] = theme.slab
            blocks[(width - 3, 1, z)] = theme.slab

    return (width, height + (1 if theme.roof_style in {"tiered", "crystal"} else 0), length), blocks


def state_tag(block_name: str) -> dict[str, object]:
    return {
        "Name": string_tag(block_name),
        "Properties": empty_compound(),
    }


def make_structure_template(size: tuple[int, int, int], blocks: BlockMap) -> dict[str, object]:
    block_names = sorted(set(blocks.values()))
    palette_index = {name: index for index, name in enumerate(block_names)}
    palette = [state_tag(name) for name in block_names]
    block_entries = []
    for (x, y, z), block_name in sorted(blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        block_entries.append(
            {
                "pos": int_list([x, y, z]),
                "state": int_tag(palette_index[block_name]),
                "nbt": empty_compound(),
            }
        )

    return {
        "DataVersion": int_tag(DATA_VERSION),
        "size": int_list(list(size)),
        "palette": compound_list(palette),
        "blocks": compound_list(block_entries),
        "entities": compound_list([]),
    }


def generate_structure(civilization: str, size_name: str) -> dict[str, object]:
    theme = THEMES[civilization]
    size, blocks = build_temple_blocks(theme, size_name)
    output = STRUCTURES_ROOT / civilization / f"temple_{size_name}.nbt"
    write_root_compound(output, make_structure_template(size, blocks))
    errors = validate_structure_template(output)
    return {
        "civilization": civilization,
        "size": size_name,
        "path": str(output.relative_to(ROOT)),
        "dimensions": list(size),
        "block_count": len(blocks),
        "ok": not errors,
        "errors": errors,
    }


def generate_all() -> tuple[int, dict[str, object]]:
    generated = []
    for civilization in THEMES:
        for size_name in SIZES:
            generated.append(generate_structure(civilization, size_name))

    failures = [entry for entry in generated if not entry["ok"]]
    manifest = {
        "version": 1,
        "scope": "generated_canonical_temple_structures",
        "generator": "tools/generate_canonical_temple_structures.py",
        "data_version": DATA_VERSION,
        "generated_count": len(generated),
        "ok": not failures,
        "structures": generated,
    }
    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST_PATH.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return (0 if not failures else 1), manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["all"])
    args = parser.parse_args()
    if args.command == "all":
        code, manifest = generate_all()
        status = "PASS" if code == 0 else "FAIL"
        print(f"Canonical Temple Structure Generation: {status}")
        print(f"manifest: {MANIFEST_PATH}")
        print(f"generated: {manifest['generated_count']}")
        for entry in manifest["structures"]:
            print(f"- {entry['civilization']} {entry['size']}: {entry['path']} ({entry['block_count']} blocks)")
        return code
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
