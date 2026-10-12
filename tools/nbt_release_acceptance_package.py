#!/usr/bin/env python3
"""Aggregate Mythic Age 1.0 NBT release acceptance evidence."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/nbt_roundtrip_validation"
REPORT_PATH = REPORT_DIR / "nbt_release_acceptance_package.json"
MARKDOWN_PATH = ROOT / "docs/系统设计/MythicAgeMod_1.0_NBT发布验收包_2026-06-12.md"
REVIEW_BOARD_PATH = REPORT_DIR / "nbt_release_review_board.png"

NBT_SCHEMA_REPORT = ROOT / "docs/preview_assets/entity_validation/nbt_schema_report.json"
NBT_TARGET_REPORT = ROOT / "docs/preview_assets/nbt_target_validation/nbt_target_report.json"
NBT_ROUNDTRIP_REPORT = ROOT / "docs/preview_assets/nbt_roundtrip_validation/nbt_roundtrip_report.json"
NBT_RESTART_CONTRACT_REPORT = ROOT / "docs/preview_assets/nbt_roundtrip_validation/nbt_restart_contract_report.json"
RUNTIME_ENTITY_NBT_REPORT = ROOT / "run/visual_probe/entity_nbt_report.json"
RUNTIME_RESTART_SEMANTIC_REPORT = ROOT / "run/visual_probe/nbt_restart_report.json"
RUNTIME_RESTART_PREPARE_REPORT = ROOT / "run/visual_probe/nbt_restart_prepare_report.json"
RUNTIME_RESTART_VERIFY_REPORT = ROOT / "run/visual_probe/nbt_restart_verify_report.json"

FONT_PATHS = (
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
)
COLORS = {
    "bg": (239, 235, 223, 255),
    "header": (83, 112, 130, 255),
    "panel": (248, 245, 237, 255),
    "panel_shadow": (220, 213, 196, 255),
    "ink": (35, 31, 26, 255),
    "muted": (96, 88, 75, 255),
    "ok": (63, 126, 82, 255),
    "pending": (176, 112, 45, 255),
    "fail": (158, 64, 58, 255),
    "line": (206, 197, 177, 255),
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def load_font(size: int) -> ImageFont.ImageFont:
    for path in FONT_PATHS:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                pass
    return ImageFont.load_default()


def wrap_text(text: str, font: ImageFont.ImageFont, max_width: int) -> list[str]:
    lines: list[str] = []
    current = ""
    for char in text:
        candidate = current + char
        if font.getbbox(candidate)[2] <= max_width or not current:
            current = candidate
        else:
            lines.append(current)
            current = char
    if current:
        lines.append(current)
    return lines


def draw_wrapped(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, font: ImageFont.ImageFont,
                 fill: tuple[int, int, int, int], max_width: int, line_height: int,
                 max_lines: int | None = None) -> int:
    x, y = xy
    lines = wrap_text(text, font, max_width)
    if max_lines is not None and len(lines) > max_lines:
        lines = lines[:max_lines]
        lines[-1] = lines[-1].rstrip("。,.，") + "..."
    for line in lines:
        draw.text((x, y), line, fill=fill, font=font)
        y += line_height
    return y


def draw_panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str,
               title_font: ImageFont.ImageFont) -> None:
    x1, y1, x2, y2 = box
    draw.rounded_rectangle((x1 + 4, y1 + 4, x2 + 4, y2 + 4), radius=8, fill=COLORS["panel_shadow"])
    draw.rounded_rectangle(box, radius=8, fill=COLORS["panel"])
    draw.text((x1 + 20, y1 + 16), title, fill=COLORS["ink"], font=title_font)


def status_color(ok: bool, pending: bool = False) -> tuple[int, int, int, int]:
    if ok:
        return COLORS["ok"]
    if pending:
        return COLORS["pending"]
    return COLORS["fail"]


def draw_status_row(draw: ImageDraw.ImageDraw, x: int, y: int, label: str, ok: bool,
                    font: ImageFont.ImageFont, pending: bool = False) -> int:
    color = status_color(ok, pending)
    text = "OK" if ok else ("PENDING" if pending else "MISSING")
    draw.ellipse((x, y + 4, x + 16, y + 20), fill=color)
    draw.text((x + 26, y), f"{label}: {text}", fill=COLORS["ink"], font=font)
    return y + 30


def schema_owner_summary(schema_report: dict[str, Any]) -> list[dict[str, Any]]:
    result = []
    for item in schema_report.get("schemas", []):
        fields = item.get("fields", []) if isinstance(item, dict) else []
        result.append({
            "owner": item.get("owner"),
            "scope": item.get("scope"),
            "field_count": len(fields),
            "ok": bool(item.get("ok")),
        })
    return result


def runtime_probe_status(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {
            "path": project_path(path),
            "exists": False,
            "ok": False,
            "failed": None,
            "passed": None,
        }
    payload = load_json(path, {})
    failed = payload.get("failed")
    ok = bool(payload.get("ok")) if "ok" in payload else failed == 0
    return {
        "path": project_path(path),
        "exists": True,
        "ok": ok,
        "failed": failed,
        "passed": payload.get("passed"),
        "scope": payload.get("scope"),
        "phase": payload.get("phase"),
        "process_id": payload.get("process_id"),
        "summary": payload.get("summary"),
    }


def draw_nbt_release_review_board(payload: dict[str, Any]) -> None:
    REVIEW_BOARD_PATH.parent.mkdir(parents=True, exist_ok=True)
    width, height = 1320, 1080
    image = Image.new("RGBA", (width, height), COLORS["bg"])
    draw = ImageDraw.Draw(image)
    title_font = load_font(42)
    h2_font = load_font(24)
    body_font = load_font(20)
    small_font = load_font(16)

    draw.rectangle((0, 0, width, 122), fill=COLORS["header"])
    draw.text((48, 34), "NBT RELEASE ACCEPTANCE", fill=(255, 252, 244, 255), font=title_font)
    draw.text((48, 84), "static contracts and real MC restart cycle are tracked as release evidence",
              fill=(243, 237, 223, 255), font=body_font)
    draw_status_row(draw, 992, 35, "release", bool(payload["release_ready"]), small_font, pending=not payload["release_ready"])

    summary = payload["summary"]
    evidence = payload["evidence"]
    counts = summary.get("target_status_counts") or {}
    coverage = summary.get("restart_contract_coverage") or {}

    draw_panel(draw, (48, 152, 624, 434), "Static Contract", h2_font)
    y = 204
    y = draw_status_row(draw, 72, y, "schema report", bool(payload["static_contract_ok"]), body_font)
    y = draw_status_row(draw, 72, y, "source round-trip", bool(payload["static_contract_ok"]), body_font)
    y = draw_status_row(draw, 72, y, "restart contract", bool(payload["static_contract_ok"]), body_font)
    draw.text((72, y + 10), f"schema owners: {summary.get('schema_owner_count')}", fill=COLORS["ink"], font=body_font)
    y += 40
    draw.text((72, y), f"target statuses: covered={counts.get('covered')} runtime_only={counts.get('runtime_only')} missing={counts.get('missing')}",
              fill=COLORS["ink"], font=small_font)
    y += 30
    draw.text((72, y), f"restart coverage: entities={coverage.get('entities')} block_entities={coverage.get('block_entities')} persistent={coverage.get('persistent_block_entities')} entrypoints={coverage.get('runtime_entrypoints')}",
              fill=COLORS["muted"], font=small_font)

    draw_panel(draw, (688, 152, 1272, 434), "Runtime Evidence", h2_font)
    y = 204
    runtime_entity = evidence["runtime_entity_nbt_report"]
    prepare = evidence["runtime_restart_prepare_report"]
    verify = evidence["runtime_restart_verify_report"]
    y = draw_status_row(draw, 712, y, "entity probe", bool(payload["runtime_entity_probe_ok"]), body_font)
    y = draw_status_row(draw, 712, y, "prepare phase", bool(prepare.get("ok")), body_font, pending=not prepare.get("ok"))
    y = draw_status_row(draw, 712, y, "verify phase", bool(verify.get("ok")), body_font, pending=not verify.get("ok"))
    y = draw_status_row(draw, 712, y, "distinct JVM process", bool(payload["restart_processes_are_distinct"]),
                        body_font, pending=not payload["restart_processes_are_distinct"])
    draw.text((712, y + 8), f"entity report exists={runtime_entity.get('exists')} ok={runtime_entity.get('ok')}",
              fill=COLORS["muted"], font=small_font)

    draw_panel(draw, (48, 468, 624, 880), "Schema Owners", h2_font)
    y = 520
    owners = payload.get("schema_owner_summary", [])
    for owner in owners:
        color = COLORS["ok"] if owner.get("ok") else COLORS["fail"]
        draw.ellipse((72, y + 4, 86, y + 18), fill=color)
        draw.text((96, y), f"{owner.get('owner')} ({owner.get('scope')}) fields={owner.get('field_count')}",
                  fill=COLORS["ink"], font=small_font)
        y += 24
        if y > 850:
            break

    draw_panel(draw, (688, 468, 1272, 880), "Next NBT Steps", h2_font)
    y = 520
    for index, action in enumerate(payload["next_actions"], start=1):
        y = draw_wrapped(draw, (712, y), f"{index}. {action}", body_font, COLORS["ink"], 520, 28, 3)
        y += 12
    if payload["blockers"]:
        y += 8
        draw.text((712, y), "Current blocker", fill=COLORS["muted"], font=small_font)
        y += 24
        y = draw_wrapped(draw, (712, y), ", ".join(payload["blockers"]), body_font, COLORS["fail"], 520, 28, 3)

    draw_panel(draw, (48, 912, 1272, 1028), "Acceptance Boundary", h2_font)
    draw_wrapped(
        draw,
        (72, 964),
        "This board confirms the static NBT evidence package and, when release_ready is true, the real save/quit/restart persistence evidence from distinct prepare and verify JVM process ids.",
        body_font,
        COLORS["ink"],
        1160,
        28,
        3,
    )
    image.convert("RGB").save(REVIEW_BOARD_PATH)


def build_package() -> dict[str, Any]:
    schema_report = load_json(NBT_SCHEMA_REPORT, {})
    target_report = load_json(NBT_TARGET_REPORT, {})
    roundtrip_report = load_json(NBT_ROUNDTRIP_REPORT, {})
    restart_contract_report = load_json(NBT_RESTART_CONTRACT_REPORT, {})
    runtime_entity = runtime_probe_status(RUNTIME_ENTITY_NBT_REPORT)
    runtime_restart_semantic = runtime_probe_status(RUNTIME_RESTART_SEMANTIC_REPORT)
    runtime_restart_prepare = runtime_probe_status(RUNTIME_RESTART_PREPARE_REPORT)
    runtime_restart_verify = runtime_probe_status(RUNTIME_RESTART_VERIFY_REPORT)
    prepare_process_id = runtime_restart_prepare.get("process_id")
    verify_process_id = runtime_restart_verify.get("process_id")
    restart_processes_are_distinct = (
        prepare_process_id is not None
        and verify_process_id is not None
        and prepare_process_id != verify_process_id
    )

    static_contract_ok = all([
        bool(schema_report.get("ok")),
        bool(target_report.get("target_gate_ok")),
        bool(roundtrip_report.get("ok")),
        bool(restart_contract_report.get("ok")),
    ])
    runtime_entity_probe_ok = bool(runtime_entity["ok"])
    real_restart_cycle_ok = bool(
        runtime_restart_prepare["ok"]
        and runtime_restart_verify["ok"]
        and restart_processes_are_distinct
    )
    release_ready = static_contract_ok and runtime_entity_probe_ok and real_restart_cycle_ok
    blockers = []
    if not static_contract_ok:
        blockers.append("static_nbt_contract_not_complete")
    if not runtime_entity_probe_ok:
        blockers.append("runtime_entity_nbt_probe_missing_or_failed")
    if not real_restart_cycle_ok:
        blockers.append("real_mc_restart_cycle_missing")
    if runtime_restart_prepare["ok"] and runtime_restart_verify["ok"] and not restart_processes_are_distinct:
        blockers.append("prepare_and_verify_process_ids_not_distinct")

    if release_ready:
        next_actions = [
            "保持 tools/nbt_target_validation_pipeline.py audit 与本验收包通过。",
            "新增长期状态前先更新 NBT 目标表，再补 schema、round-trip/restart probe 和默认值策略。",
            "发布前保留 run/visual_probe/nbt_restart_prepare_report.json 与 run/visual_probe/nbt_restart_verify_report.json 作为真实重启证据。",
            "重跑 tools/mythic_age_1_0_gap_audit.py audit。",
        ]
    else:
        next_actions = [
            "通过 tools/mc_visual_probe_mcp.py 的 run_true_nbt_restart_cycle 执行 prepare -> quit client -> restart same world -> verify。",
            "确认 run/visual_probe/nbt_restart_prepare_report.json 与 run/visual_probe/nbt_restart_verify_report.json 均 failed=0。",
            "确认两个报告中的 process_id 不同，证明 verify 来自重启后的 JVM。",
            "重跑 tools/mythic_age_1_0_gap_audit.py audit。",
        ]

    payload = {
        "version": 1,
        "scope": "mythic_age_1_0_nbt_release_acceptance_package",
        "ok": release_ready,
        "release_ready": release_ready,
        "static_contract_ok": static_contract_ok,
        "runtime_entity_probe_ok": runtime_entity_probe_ok,
        "real_restart_cycle_ok": real_restart_cycle_ok,
        "restart_processes_are_distinct": restart_processes_are_distinct,
        "blockers": blockers,
        "summary": {
            "schema_owner_count": len(schema_report.get("schemas", [])) if isinstance(schema_report.get("schemas"), list) else None,
            "target_status_counts": target_report.get("status_counts"),
            "restart_contract_coverage": restart_contract_report.get("coverage"),
        },
        "schema_owner_summary": schema_owner_summary(schema_report),
        "evidence": {
            "schema_report": project_path(NBT_SCHEMA_REPORT),
            "target_report": project_path(NBT_TARGET_REPORT),
            "roundtrip_source_report": project_path(NBT_ROUNDTRIP_REPORT),
            "restart_contract_report": project_path(NBT_RESTART_CONTRACT_REPORT),
            "runtime_entity_nbt_report": runtime_entity,
            "runtime_restart_semantic_report": runtime_restart_semantic,
            "runtime_restart_prepare_report": runtime_restart_prepare,
            "runtime_restart_verify_report": runtime_restart_verify,
        },
        "acceptance_rule": "NBT can be release-accepted only after static contracts pass, runtime entity probe passes, and a real prepare -> save/quit -> restart same world -> verify cycle reports failed=0 with distinct JVM process ids in prepare and verify reports.",
        "next_actions": next_actions,
    }
    draw_nbt_release_review_board(payload)
    payload["review_board"] = project_path(REVIEW_BOARD_PATH)
    payload["review_board_present"] = REVIEW_BOARD_PATH.exists()
    return payload


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 NBT 发布验收包",
        "",
        "本文档由 `tools/nbt_release_acceptance_package.py audit` 生成，用于区分静态 NBT 合约完成度和真实 MC 存档重启验收。",
        "",
        "## 总览",
        "",
        f"- 静态 NBT 合约通过：{payload['static_contract_ok']}",
        f"- 运行中实体 NBT 探针通过：{payload['runtime_entity_probe_ok']}",
        f"- 真实 MC 重启周期通过：{payload['real_restart_cycle_ok']}",
        f"- prepare/verify 进程不同：{payload['restart_processes_are_distinct']}",
        f"- NBT 发布验收通过：{payload['release_ready']}",
        f"- NBT release review board：`{payload.get('review_board', '')}`",
        f"- 阻塞项：`{json.dumps(payload['blockers'], ensure_ascii=False)}`",
        "",
        "## 证据",
        "",
        f"- schema report：`{payload['evidence']['schema_report']}`",
        f"- target report：`{payload['evidence']['target_report']}`",
        f"- source round-trip：`{payload['evidence']['roundtrip_source_report']}`",
        f"- restart contract：`{payload['evidence']['restart_contract_report']}`",
        f"- runtime entity：`{payload['evidence']['runtime_entity_nbt_report']['path']}`，exists={payload['evidence']['runtime_entity_nbt_report']['exists']}，ok={payload['evidence']['runtime_entity_nbt_report']['ok']}",
        f"- restart semantic：`{payload['evidence']['runtime_restart_semantic_report']['path']}`，exists={payload['evidence']['runtime_restart_semantic_report']['exists']}，ok={payload['evidence']['runtime_restart_semantic_report']['ok']}",
        f"- restart prepare：`{payload['evidence']['runtime_restart_prepare_report']['path']}`，exists={payload['evidence']['runtime_restart_prepare_report']['exists']}，ok={payload['evidence']['runtime_restart_prepare_report']['ok']}",
        f"- restart verify：`{payload['evidence']['runtime_restart_verify_report']['path']}`，exists={payload['evidence']['runtime_restart_verify_report']['exists']}，ok={payload['evidence']['runtime_restart_verify_report']['ok']}",
        f"- prepare process id：`{payload['evidence']['runtime_restart_prepare_report'].get('process_id')}`",
        f"- verify process id：`{payload['evidence']['runtime_restart_verify_report'].get('process_id')}`",
        "",
        "## 下一步",
        "",
    ]
    lines.extend(f"- {action}" for action in payload["next_actions"])
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_package()
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        status = "PASS" if payload["ok"] else "NOT READY"
        print(f"Mythic Age NBT Release Acceptance Package: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"blockers: {', '.join(payload['blockers']) if payload['blockers'] else 'none'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
