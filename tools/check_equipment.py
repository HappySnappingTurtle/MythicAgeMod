#!/usr/bin/env python3
"""装备数值校验（T17 设计文档 §1）：模组的装备数值表（neoforge/src/main/resources/mythic_age/equipment/<文明>.json）
与 10 号合同的 json（docs/重构/设计/跨文明详细设计/数据/49普通装备来源与84子件提案.json）逐项对比。

对比：护甲点、韧性、每件耐久；武器的伤害、攻速、触及、耐久、满弓刻数。合同里写「待定」的字段，
数值表里要写拍板后的值并加 "decided" 注明——这些只出一条提示，不算错误。
运行：python3 tools/check_equipment.py（也由 check_content 调用）
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

DEFAULT_ROOT = Path(__file__).resolve().parents[1]
CONTRACT = Path("docs/重构/设计/跨文明详细设计/数据/49普通装备来源与84子件提案.json")
TABLES = Path("neoforge/src/main/resources/mythic_age/equipment")
PARTS = ("helmet", "chest", "leggings", "boots")
# 数值表字段 → 合同字段
WEAPON_FIELDS = {"damage": "damage", "speed": "attack_speed", "reach": "reach_bonus", "durability": "durability",
                 "draw_ticks": "draw_ticks"}


def value(v):
    return v.get("value") if isinstance(v, dict) else v


def check(root: Path) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    notes: list[str] = []
    contract = json.loads((root / CONTRACT).read_text(encoding="utf-8"))
    items = {it["id"]: it for it in contract["items"]}
    folder = root / TABLES
    if not folder.exists():
        return errors, ["没有装备数值表，跳过"]
    for f in sorted(folder.glob("*.json")):
        table = json.loads(f.read_text(encoding="utf-8"))
        civ = f.stem
        for set_id, a in table.get("armor", {}).items():
            src = a.get("source")
            for i, part in enumerate(PARTS):
                it = items.get(f"{src}-{part}")
                where = f"{civ}.armor.{set_id}.{part}"
                if it is None:
                    errors.append(f"{where}：合同里没有 {src}-{part}")
                    continue
                pairs = [("points", a["points"][i], value(it.get("armor_points"))),
                         ("toughness", a.get("toughness"), value(it.get("toughness"))),
                         ("durability", a["durability"][i], it.get("durability_derived"))]
                for name, mine, theirs in pairs:
                    compare(where, name, mine, theirs, "decided" in a, errors, notes)
        for wid, w in table.get("weapons", {}).items():
            src = w.get("source")
            it = items.get(f"{src}-item")
            where = f"{civ}.weapons.{wid}"
            if it is None:
                errors.append(f"{where}：合同里没有 {src}-item")
                continue
            for mine_key, their_key in WEAPON_FIELDS.items():
                if mine_key not in w and their_key not in it:
                    continue
                compare(where, mine_key, w.get(mine_key), it.get(their_key), "decided" in w, errors, notes)
    return errors, notes


def compare(where, name, mine, theirs, decided, errors, notes):
    if theirs == "待定" or theirs is None:
        if mine is None:
            return
        if decided:
            notes.append(f"{where}.{name} = {mine}（合同待定，已拍板）")
        else:
            errors.append(f"{where}.{name}：合同待定，数值表写了 {mine} 却没有 decided 注明")
        return
    if mine is None:
        errors.append(f"{where}.{name}：数值表缺少（合同为 {theirs}）")
    elif abs(float(mine) - float(theirs)) > 1e-9:
        errors.append(f"{where}.{name}：数值表 {mine}，合同 {theirs}")


def main(argv=None) -> int:
    root = Path(argv[0]) if argv else DEFAULT_ROOT
    errors, notes = check(root)
    for n in notes:
        print(f"提示：{n}")
    for e in errors:
        print(f"错误：{e}")
    print(f"共 {len(errors)} 个错误" if errors else "装备数值全部一致")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
