#!/usr/bin/env python3
"""Universal Java Model → BBModel synchronization tool.

Parses getTexturedModelData() from any creature Model.java and generates
or updates the corresponding .bbmodel file. Uses creature_model_map.json
for path mappings.

Usage:
    python3 tools/sync_creature_bbmodel.py audit          # Check all 30 creatures
    python3 tools/sync_creature_bbmodel.py diff <id>       # Show diff for one creature
    python3 tools/sync_creature_bbmodel.py sync <id>       # Sync one creature
    python3 tools/sync_creature_bbmodel.py sync-all        # Sync all
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import uuid
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
MAP_PATH = ROOT / "tools" / "creature_model_map.json"

# ── Java Model Parser ──

CUBOID_RE = re.compile(
    r'\.cuboid\(\s*'
    r'(-?[\d.]+)[fF]?\s*,\s*(-?[\d.]+)[fF]?\s*,\s*(-?[\d.]+)[fF]?\s*,\s*'
    r'(-?[\d.]+)[fF]?\s*,\s*(-?[\d.]+)[fF]?\s*,\s*(-?[\d.]+)[fF]?\s*'
)

PIVOT_RE = re.compile(
    r'ModelTransform\.pivot\(\s*'
    r'(-?[\d.]+)[fF]?\s*,\s*(-?[\d.]+)[fF]?\s*,\s*(-?[\d.]+)[fF]?\s*\s*\)'
)

UV_RE = re.compile(r'\.uv\(\s*(\d+)\s*,\s*(\d+)\s*\)')

ADD_CHILD_RE = re.compile(
    r'(\w+)\.addChild\(\s*"([^"]+)"\s*,'
)

TEXTURE_SIZE_RE = re.compile(
    r'TexturedModelData\.of\(\s*\w+\s*,\s*(\d+)\s*,\s*(\d+)\s*\)'
)


class ModelNode:
    def __init__(self, name: str, parent_name: str | None = None):
        self.name = name
        self.parent_name = parent_name
        self.pivot: tuple[float, float, float] = (0.0, 0.0, 0.0)
        self.cuboids: list[dict] = []
        self.children: list[ModelNode] = []

    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "pivot": list(self.pivot),
            "cuboids": self.cuboids,
            "children": [c.to_dict() for c in self.children],
        }


def parse_java_model(java_path: Path) -> tuple[list[ModelNode], tuple[int, int]]:
    text = java_path.read_text(encoding="utf-8")

    texture_size = (64, 64)
    m = TEXTURE_SIZE_RE.search(text)
    if m:
        texture_size = (int(m.group(1)), int(m.group(2)))

    nodes: dict[str, ModelNode] = {}
    root_children: list[str] = []

    for match in ADD_CHILD_RE.finditer(text):
        parent_var = match.group(1)
        child_name = match.group(2)
        node = ModelNode(child_name)
        nodes[child_name] = node

        if parent_var in ("root", "modelPartData", "partData"):
            root_children.append(child_name)
            node.parent_name = "root"
        else:
            for existing_name, existing_node in nodes.items():
                if parent_var.replace("_", "").lower() in existing_name.replace("_", "").lower():
                    node.parent_name = existing_name
                    break

    result = []
    for name in root_children:
        if name in nodes:
            result.append(nodes[name])

    return result, texture_size


# ── BBModel Parser ──

def parse_bbmodel(bbmodel_path: Path) -> dict:
    if not bbmodel_path.exists():
        return {"elements": [], "outliner": [], "resolution": {"width": 64, "height": 64}}
    return json.loads(bbmodel_path.read_text(encoding="utf-8"))


# ── Diff ──

def diff_trees(java_nodes: list[ModelNode], bbmodel: dict) -> dict:
    java_names = {n.name for n in java_nodes}
    bb_names = set()

    def collect_bb_names(outliner):
        for item in outliner:
            if isinstance(item, dict):
                bb_names.add(item.get("name", ""))
                collect_bb_names(item.get("children", []))

    collect_bb_names(bbmodel.get("outliner", []))

    return {
        "java_only": sorted(java_names - bb_names),
        "bbmodel_only": sorted(bb_names - java_names),
        "common": sorted(java_names & bb_names),
        "java_count": len(java_names),
        "bbmodel_count": len(bb_names),
    }


# ── Write BBModel ──

def write_bbmodel(java_nodes: list[ModelNode], texture_size: tuple[int, int], output_path: Path) -> None:
    elements = []
    outliner = []

    def build_element(node: ModelNode) -> str:
        eid = str(uuid.uuid4())
        elements.append({
            "name": node.name,
            "uuid": eid,
            "from": [0, 0, 0],
            "to": [1, 1, 1],
            "origin": list(node.pivot),
        })
        return eid

    def build_outliner(node: ModelNode) -> dict:
        eid = build_element(node)
        children = []
        for child in node.children:
            children.append(build_outliner(child))
        return {
            "name": node.name,
            "uuid": str(uuid.uuid4()),
            "children": [eid] + children,
            "origin": list(node.pivot),
        }

    for node in java_nodes:
        outliner.append(build_outliner(node))

    bbmodel = {
        "meta": {"format_version": "4.5", "model_format": "java_block", "box_uv": False},
        "name": output_path.stem,
        "resolution": {"width": texture_size[0], "height": texture_size[1]},
        "elements": elements,
        "outliner": outliner,
        "textures": [],
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(bbmodel, indent=2, ensure_ascii=False), encoding="utf-8")


# ── Commands ──

def load_map() -> dict:
    if not MAP_PATH.exists():
        print(f"ERROR: {MAP_PATH} not found. Run with --generate-map first.")
        sys.exit(1)
    return json.loads(MAP_PATH.read_text(encoding="utf-8"))


def cmd_audit(args):
    creature_map = load_map()
    results = []
    for creature_id, info in creature_map.items():
        java_path = ROOT / info["java_model"]
        bb_path = ROOT / info["bbmodel"]
        status = "OK"
        notes = []
        if not java_path.exists():
            status = "MISSING_JAVA"
            notes.append(f"Java model not found: {info['java_model']}")
        if not bb_path.exists():
            status = "MISSING_BBMODEL"
            notes.append(f"BBModel not found: {info['bbmodel']}")
        if java_path.exists() and bb_path.exists():
            nodes, _ = parse_java_model(java_path)
            bbmodel = parse_bbmodel(bb_path)
            d = diff_trees(nodes, bbmodel)
            if d["java_only"]:
                status = "DRIFT"
                notes.append(f"Java-only parts: {d['java_only']}")
            if d["bbmodel_only"]:
                if status == "OK":
                    status = "DRIFT"
                notes.append(f"BBModel-only parts: {d['bbmodel_only']}")
        results.append({"creature": creature_id, "status": status, "notes": "; ".join(notes)})

    ok_count = sum(1 for r in results if r["status"] == "OK")
    print(f"\n=== BBModel Sync Audit: {ok_count}/{len(results)} consistent ===\n")
    for r in results:
        marker = "  " if r["status"] == "OK" else "! "
        print(f"{marker}{r['creature']:30s} {r['status']:20s} {r['notes']}")

    return 0 if ok_count == len(results) else 1


def cmd_diff(args):
    creature_map = load_map()
    creature_id = args.creature_id
    if creature_id not in creature_map:
        print(f"ERROR: Unknown creature '{creature_id}'")
        return 1
    info = creature_map[creature_id]
    java_path = ROOT / info["java_model"]
    bb_path = ROOT / info["bbmodel"]
    nodes, tex = parse_java_model(java_path)
    bbmodel = parse_bbmodel(bb_path)
    d = diff_trees(nodes, bbmodel)
    print(json.dumps(d, indent=2))
    return 0


def cmd_sync(args):
    creature_map = load_map()
    creature_id = args.creature_id
    if creature_id not in creature_map:
        print(f"ERROR: Unknown creature '{creature_id}'")
        return 1
    info = creature_map[creature_id]
    java_path = ROOT / info["java_model"]
    bb_path = ROOT / info["bbmodel"]
    nodes, tex = parse_java_model(java_path)
    write_bbmodel(nodes, tex, bb_path)
    print(f"Synced {creature_id}: {bb_path}")
    return 0


def cmd_sync_all(args):
    creature_map = load_map()
    for creature_id in creature_map:
        info = creature_map[creature_id]
        java_path = ROOT / info["java_model"]
        bb_path = ROOT / info["bbmodel"]
        if not java_path.exists():
            print(f"SKIP {creature_id}: Java model missing")
            continue
        nodes, tex = parse_java_model(java_path)
        write_bbmodel(nodes, tex, bb_path)
        print(f"Synced {creature_id}")
    return 0


def main():
    parser = argparse.ArgumentParser(description="Java Model ↔ BBModel sync tool")
    sub = parser.add_subparsers(dest="command")

    sub.add_parser("audit", help="Check consistency for all creatures")

    p_diff = sub.add_parser("diff", help="Show diff for one creature")
    p_diff.add_argument("creature_id")

    p_sync = sub.add_parser("sync", help="Sync one creature")
    p_sync.add_argument("creature_id")

    sub.add_parser("sync-all", help="Sync all creatures")

    args = parser.parse_args()
    if args.command == "audit":
        return cmd_audit(args)
    elif args.command == "diff":
        return cmd_diff(args)
    elif args.command == "sync":
        return cmd_sync(args)
    elif args.command == "sync-all":
        return cmd_sync_all(args)
    else:
        parser.print_help()
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
