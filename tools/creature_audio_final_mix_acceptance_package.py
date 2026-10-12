#!/usr/bin/env python3
"""Build the creature audio final-mix acceptance package.

This package does not promote procedural placeholder sounds to final assets.
It records objective OGG metadata plus the remaining listening/mix review work.
"""

from __future__ import annotations

import argparse
import html
import hashlib
import json
import os
import shutil
import struct
import sys
from collections import Counter
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/audio_validation"
REPORT_PATH = REPORT_DIR / "creature_audio_final_mix_acceptance_package.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/audio/creature_audio_final_mix_acceptance_package.md"
AUDIO_REPORT = REPORT_DIR / "audio_report.json"
AUDIO_ART_DIRECTION = ROOT / "docs/美术资源/audio/creature_audio_art_direction_2026-06-12.json"
REVIEW_BOARD_DIR = REPORT_DIR / "final_mix_review_boards"
REVIEW_BOARD_CONTACT_SHEET = REPORT_DIR / "final_mix_review_board_contact_sheet.png"
AUDITION_INDEX_PATH = REPORT_DIR / "final_mix_audition_index.html"
RESAMPLE_NEAREST = getattr(getattr(Image, "Resampling", Image), "NEAREST")
FONT_PATHS = (
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
)

EVENT_DURATION_RANGES = {
    "ambient": (0.35, 4.0),
    "hurt": (0.12, 1.4),
    "death": (0.45, 4.2),
    "attack": (0.12, 1.6),
    "special": (0.25, 3.8),
    "charge": (0.25, 3.2),
    "fly": (0.25, 4.0),
}

ASSET_CLASS_LABELS = {
    "authored_or_existing": "authored/existing",
    "final_mix_candidate_pending_listening": "final mix candidate",
    "procedural_first_pass": "procedural first-pass",
    "technical_placeholder_repair": "technical placeholder repair",
}

ASSET_CLASS_COLORS = {
    "authored_or_existing": (63, 126, 82, 255),
    "final_mix_candidate_pending_listening": (58, 112, 158, 255),
    "procedural_first_pass": (176, 112, 45, 255),
    "technical_placeholder_repair": (158, 64, 58, 255),
}

MIX_TOOL_COMMANDS = ("ffprobe", "ffmpeg", "soxi", "ogginfo")

EVENT_LISTENING_PRIORITY = {
    "special": 1,
    "attack": 2,
    "charge": 2,
    "death": 3,
    "fly": 4,
    "hurt": 5,
    "ambient": 6,
}

EVENT_LISTENING_CRITERIA = {
    "special": [
        "必须能一听辨认该生物的神话身份和特殊能力。",
        "与特殊动作峰值同步，不能抢掉玩家命中/受击反馈。",
        "低频、尾音和粒子阶段不能在混战中糊成噪声。",
    ],
    "attack": [
        "起手、命中或挥击峰值必须清楚。",
        "音量不能盖过玩家受击、死亡或关键 UI 提示。",
        "同类普通攻击之间要有身份差异，不能全部像同一个方块击打声。",
    ],
    "charge": [
        "蓄力阶段要逐渐增强，峰值前不能误判为已经释放。",
        "循环或尾音不能出现明显点击、断裂或过长拖尾。",
    ],
    "death": [
        "死亡音效要明显区别 hurt，长度和体量感符合生物尺寸。",
        "尾音不能拖慢战斗节奏，也不能被环境噪声完全吞掉。",
    ],
    "fly": [
        "翼拍、气流或滑翔质感必须能支撑飞行动作。",
        "循环感不能太机械，且不能遮住骑乘/移动反馈。",
    ],
    "hurt": [
        "必须短、清楚、可重复播放，不能像死亡或特殊技能。",
        "受击音量要能被听见，但不能刺耳或抢过攻击音。",
    ],
    "ambient": [
        "体现文明和生物身份，但不能太频繁或太响。",
        "在待机和群体场景中不能造成持续噪声疲劳。",
    ],
}

LISTENING_BATCH_SIZE_BY_PRIORITY = {
    1: 18,
    2: 20,
    3: 20,
    4: 18,
    5: 24,
    6: 24,
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


def review_board_path(creature: dict[str, Any]) -> Path:
    return REVIEW_BOARD_DIR / str(creature["civilization"]) / f"{creature['id']}_final_mix_review.png"


def audition_index_relative(path_text: str | None) -> str:
    if not path_text:
        return ""
    target = ROOT / path_text
    return os.path.relpath(target, AUDITION_INDEX_PATH.parent).replace(os.sep, "/")


def escape(value: Any) -> str:
    return html.escape(str(value), quote=True)


def sound_ref_path(sound_ref: str) -> Path:
    namespace = "mythic_age"
    path = sound_ref
    if ":" in sound_ref:
        namespace, path = sound_ref.split(":", 1)
    return ROOT / f"src/main/resources/assets/{namespace}/sounds/{path}.ogg"


def sha256(path: Path) -> str | None:
    if not path.exists():
        return None
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def audio_tooling_status() -> dict[str, Any]:
    tools = {
        command: {
            "available": shutil.which(command) is not None,
            "path": shutil.which(command),
        }
        for command in MIX_TOOL_COMMANDS
    }
    decoder_available = bool(
        tools["ffprobe"]["available"]
        or tools["ffmpeg"]["available"]
        or tools["soxi"]["available"]
        or tools["ogginfo"]["available"]
    )
    loudness_metrics_available = bool(
        tools["ffprobe"]["available"] and tools["ffmpeg"]["available"]
    )
    return {
        "tools": tools,
        "container_parser": "built_in_ogg_vorbis_page_parser",
        "container_metrics_available": True,
        "decoder_available": decoder_available,
        "loudness_metrics_available": loudness_metrics_available,
        "spectral_metrics_available": bool(tools["ffmpeg"]["available"]),
        "missing_tools": [
            command for command, status in tools.items()
            if not status["available"]
        ],
        "blocking_for_final_mix_acceptance": not loudness_metrics_available,
        "missing_reason": (
            "Built-in OGG metadata parsing is available, but final loudness, peak, spectral balance, "
            "and decoded waveform checks require ffmpeg/ffprobe or equivalent audio tooling/MCP."
        ),
    }


def parse_ogg_vorbis_metadata(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {
            "exists": False,
            "parse_ok": False,
            "error": "missing_file",
        }
    data = path.read_bytes()
    sample_rate: int | None = None
    channels: int | None = None
    marker = data.find(b"\x01vorbis")
    if marker >= 0 and marker + 16 <= len(data):
        channels = data[marker + 11]
        sample_rate = struct.unpack_from("<I", data, marker + 12)[0]

    offset = 0
    last_granule: int | None = None
    page_count = 0
    while offset + 27 <= len(data):
        if data[offset:offset + 4] != b"OggS":
            break
        page_segments = data[offset + 26]
        segment_table_start = offset + 27
        segment_table_end = segment_table_start + page_segments
        if segment_table_end > len(data):
            break
        body_size = sum(data[segment_table_start:segment_table_end])
        body_end = segment_table_end + body_size
        if body_end > len(data):
            break
        granule = struct.unpack_from("<q", data, offset + 6)[0]
        if granule >= 0:
            last_granule = granule
        page_count += 1
        offset = body_end

    duration_seconds = None
    if sample_rate and last_granule is not None:
        duration_seconds = round(last_granule / sample_rate, 3)

    return {
        "exists": True,
        "parse_ok": sample_rate is not None and duration_seconds is not None,
        "file_size_bytes": path.stat().st_size,
        "sha256": sha256(path),
        "sample_rate": sample_rate,
        "channels": channels,
        "duration_seconds": duration_seconds,
        "ogg_page_count": page_count,
    }


def duration_check(event: str, duration: float | None) -> dict[str, Any]:
    expected_min, expected_max = EVENT_DURATION_RANGES.get(event, (0.1, 5.0))
    ok = duration is not None and expected_min <= duration <= expected_max
    return {
        "expected_min_seconds": expected_min,
        "expected_max_seconds": expected_max,
        "ok": ok,
    }


def event_duration(event_summary: dict[str, Any]) -> float | None:
    files = event_summary.get("files", [])
    if not files:
        return None
    metadata = files[0].get("metadata", {}) if isinstance(files[0], dict) else {}
    value = metadata.get("duration_seconds")
    return float(value) if isinstance(value, (int, float)) else None


def event_file_size(event_summary: dict[str, Any]) -> int | None:
    files = event_summary.get("files", [])
    if not files:
        return None
    metadata = files[0].get("metadata", {}) if isinstance(files[0], dict) else {}
    value = metadata.get("file_size_bytes")
    return int(value) if isinstance(value, int) else None


def event_priority(event_name: str | None) -> int:
    return EVENT_LISTENING_PRIORITY.get(str(event_name or ""), 9)


def summarize_event(event: dict[str, Any]) -> dict[str, Any]:
    sound_refs = event.get("sound_refs", [])
    files = []
    metadata_ok = bool(sound_refs)
    for sound_ref in sound_refs:
        path = sound_ref_path(sound_ref)
        metadata = parse_ogg_vorbis_metadata(path)
        check = duration_check(event.get("event", ""), metadata.get("duration_seconds"))
        file_ok = bool(metadata.get("parse_ok")) and bool(check["ok"])
        metadata_ok = metadata_ok and file_ok
        files.append({
            "sound_ref": sound_ref,
            "path": project_path(path),
            "metadata": metadata,
            "duration_check": check,
            "metadata_ok": file_ok,
        })
    return {
        "event": event.get("event"),
        "sound_key": event.get("sound_key"),
        "technical_contract_ok": bool(event.get("ok")),
        "subtitle": event.get("subtitle"),
        "files": files,
        "metadata_ok": metadata_ok,
        "mix_metrics_available": False,
        "mix_metrics_missing_reason": "Container metadata is parsed locally; decoded loudness, peak, spectral balance and listening quality still require ffmpeg/ffprobe, equivalent audio tooling, or human review.",
    }


def draw_event_rows(draw: ImageDraw.ImageDraw, creature: dict[str, Any], x: int, y: int,
                    width: int, font: ImageFont.ImageFont, small_font: ImageFont.ImageFont) -> int:
    dark = (34, 30, 24, 255)
    muted = (92, 84, 72, 255)
    ok_color = (63, 126, 82, 255)
    fail_color = (158, 64, 58, 255)
    max_duration = max(
        [event_duration(event) or 0.0 for event in creature["events"]] + [1.0]
    )
    for event in creature["events"]:
        duration = event_duration(event)
        file_size = event_file_size(event)
        metadata_ok = bool(event.get("metadata_ok"))
        color = ok_color if metadata_ok else fail_color
        draw.text((x, y), str(event["event"]), fill=dark, font=font)
        bar_x = x + 124
        bar_w = int((duration or 0.0) / max_duration * (width - 330))
        draw.rectangle((bar_x, y + 4, bar_x + width - 330, y + 22), fill=(226, 220, 208, 255))
        draw.rectangle((bar_x, y + 4, bar_x + max(4, bar_w), y + 22), fill=color)
        duration_text = "n/a" if duration is None else f"{duration:.2f}s"
        size_text = "n/a" if file_size is None else f"{file_size / 1024:.1f} KB"
        draw.text((bar_x + width - 312, y + 2), duration_text, fill=dark, font=small_font)
        draw.text((bar_x + width - 246, y + 2), size_text, fill=muted, font=small_font)
        draw.text((bar_x + width - 154, y + 2), "metadata ok" if metadata_ok else "metadata fail",
                  fill=color, font=small_font)
        y += 34
    return y


def draw_final_mix_review_board(creature: dict[str, Any]) -> None:
    out = review_board_path(creature)
    out.parent.mkdir(parents=True, exist_ok=True)
    title_font = load_font(24)
    section_font = load_font(18)
    body_font = load_font(14)
    small_font = load_font(11)
    image = Image.new("RGBA", (1280, 760), (244, 240, 231, 255))
    draw = ImageDraw.Draw(image)
    dark = (34, 30, 24, 255)
    muted = (92, 84, 72, 255)
    panel = (252, 248, 239, 255)
    asset_class = str(creature["asset_class"])
    class_color = ASSET_CLASS_COLORS.get(asset_class, muted)

    draw.rectangle((0, 0, 1280, 74), fill=dark)
    draw.text((28, 18), f"{creature['civilization']} / {creature['id']} final mix review", fill=(255, 255, 255, 255), font=title_font)
    draw.rounded_rectangle((946, 18, 1248, 54), radius=8, fill=class_color)
    draw.text((964, 28), ASSET_CLASS_LABELS.get(asset_class, asset_class), fill=(255, 255, 255, 255), font=small_font)

    draw.rectangle((28, 96, 782, 704), fill=panel, outline=muted, width=2)
    draw.text((52, 120), "Event metadata / OGG 元数据", fill=dark, font=section_font)
    y = 164
    y = draw_event_rows(draw, creature, 56, y, 690, body_font, small_font)

    y += 16
    draw.text((52, y), "Acceptance state", fill=dark, font=section_font)
    y += 30
    state_lines = [
        f"metadata_ok: {creature['metadata_ok']}",
        f"technical_ok: {creature['technical_ok']}",
        f"final_mix_candidate: {creature['final_mix_candidate']}",
        f"replacement_required: {creature['replacement_required']}",
        f"human_listening_acceptance: {creature['human_listening_acceptance']}",
        f"final_mix_accepted: {creature['final_mix_accepted']}",
    ]
    for line in state_lines:
        draw.text((60, y), line, fill=dark, font=body_font)
        y += 24

    draw.rectangle((812, 96, 1252, 704), fill=panel, outline=muted, width=2)
    draw.text((836, 120), "1.0 listening checklist / 听感验收", fill=dark, font=section_font)
    y = 164
    if creature["replacement_required"]:
        y = draw_wrapped(
            draw,
            (836, y),
            "该生物当前仍是程序化首版或技术修复占位，不能作为 1.0 最终音频；需要替换或重新混音。",
            body_font,
            (150, 58, 50, 255),
            374,
            22,
            max_lines=4,
        )
        y += 12
    elif creature["final_mix_candidate"]:
        y = draw_wrapped(
            draw,
            (836, y),
            "该生物已生成最终混音候选，不再是技术占位；发布前仍需要人工听感、响度和战斗可读性验收。",
            body_font,
            (58, 112, 158, 255),
            374,
            22,
            max_lines=4,
        )
        y += 12
    for question in creature["review_questions"]:
        y = draw_wrapped(draw, (836, y), "- " + question, body_font, dark, 374, 22, max_lines=2)
        y += 4

    y += 8
    draw.text((836, min(y, 648)), "Final mix acceptance remains pending.", fill=(150, 58, 50, 255), font=body_font)
    image.save(out)


def draw_final_mix_review_boards(payload: dict[str, Any]) -> None:
    for creature in payload["creatures"]:
        draw_final_mix_review_board(creature)
        creature["final_mix_review_board"] = project_path(review_board_path(creature))
        creature["final_mix_review_board_present"] = review_board_path(creature).exists()

    thumb_w, thumb_h = 426, 253
    label_h = 32
    columns = 3
    rows = (len(payload["creatures"]) + columns - 1) // columns
    sheet = Image.new("RGBA", (thumb_w * columns, (thumb_h + label_h) * rows), (244, 240, 231, 255))
    draw = ImageDraw.Draw(sheet)
    font = load_font(12)
    for index, creature in enumerate(payload["creatures"]):
        row = index // columns
        col = index % columns
        x = col * thumb_w
        y = row * (thumb_h + label_h)
        path = review_board_path(creature)
        preview = Image.open(path).convert("RGBA")
        preview.thumbnail((thumb_w, thumb_h), RESAMPLE_NEAREST)
        sheet.alpha_composite(preview, (x + (thumb_w - preview.width) // 2, y + label_h))
        draw.rectangle((x, y, x + thumb_w, y + label_h), fill=(34, 30, 24, 255))
        label = f"{creature['civilization']} / {creature['id']}"
        draw.text((x + 8, y + 9), label, fill=(255, 255, 255, 255), font=font)
    REVIEW_BOARD_CONTACT_SHEET.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(REVIEW_BOARD_CONTACT_SHEET)
    payload["final_mix_review_board_contact_sheet"] = project_path(REVIEW_BOARD_CONTACT_SHEET)
    payload["final_mix_review_board_count"] = sum(
        1 for item in payload["creatures"] if item.get("final_mix_review_board_present")
    )


def build_listening_queue(creatures: list[dict[str, Any]]) -> list[dict[str, Any]]:
    queue: list[dict[str, Any]] = []
    for creature in creatures:
        for event in creature["events"]:
            files = event.get("files", [])
            first_file = files[0] if files else {}
            metadata = first_file.get("metadata", {}) if isinstance(first_file, dict) else {}
            sound_path = first_file.get("path") if isinstance(first_file, dict) else None
            priority = event_priority(event.get("event"))
            if creature["replacement_required"]:
                priority -= 3
            elif creature["final_mix_candidate"]:
                priority -= 1
            queue.append({
                "civilization": creature["civilization"],
                "creature_id": creature["id"],
                "asset_class": creature["asset_class"],
                "event": event.get("event"),
                "sound_key": event.get("sound_key"),
                "path": sound_path,
                "duration_seconds": metadata.get("duration_seconds"),
                "file_size_bytes": metadata.get("file_size_bytes"),
                "metadata_ok": event.get("metadata_ok"),
                "final_mix_candidate": creature["final_mix_candidate"],
                "replacement_required": creature["replacement_required"],
                "priority": max(1, priority),
                "review_focus": (
                    "replace_before_final_mix" if creature["replacement_required"]
                    else "candidate_loudness_and_identity" if creature["final_mix_candidate"]
                    else "authored_asset_regression_listening"
                ),
                "acceptance_criteria": EVENT_LISTENING_CRITERIA.get(str(event.get("event")), []),
            })
    queue.sort(key=lambda item: (
        int(item["priority"]),
        str(item["civilization"]),
        str(item["creature_id"]),
        str(item["event"]),
    ))
    return queue


def build_listening_batches(queue: list[dict[str, Any]]) -> list[dict[str, Any]]:
    batches: list[dict[str, Any]] = []
    grouped: dict[tuple[int, str], list[dict[str, Any]]] = {}
    for item in queue:
        grouped.setdefault((int(item["priority"]), str(item["event"])), []).append(item)
    for (priority, event), items in sorted(grouped.items()):
        batch_size = LISTENING_BATCH_SIZE_BY_PRIORITY.get(priority, 20)
        for index in range(0, len(items), batch_size):
            chunk = items[index:index + batch_size]
            batch_index = index // batch_size + 1
            batches.append({
                "batch_id": f"P{priority}-{event}-{batch_index}",
                "priority": priority,
                "event": event,
                "task_count": len(chunk),
                "creatures": [f"{item['civilization']}:{item['creature_id']}" for item in chunk],
                "candidate_task_count": sum(1 for item in chunk if item["final_mix_candidate"]),
                "replacement_task_count": sum(1 for item in chunk if item["replacement_required"]),
                "total_duration_seconds": round(sum(float(item.get("duration_seconds") or 0.0) for item in chunk), 3),
                "acceptance_criteria": EVENT_LISTENING_CRITERIA.get(event, []),
            })
    return batches


def write_audition_index(payload: dict[str, Any]) -> None:
    rows = []
    for creature in payload["creatures"]:
        asset_class = str(creature["asset_class"])
        status_class = "replace" if creature["replacement_required"] else ("candidate" if creature["final_mix_candidate"] else "authored")
        board = audition_index_relative(creature.get("final_mix_review_board"))
        rows.append(
            "\n".join([
                f'<section class="creature {status_class}" id="{escape(creature["civilization"])}-{escape(creature["id"])}">',
                "  <header>",
                "    <div>",
                f'      <h2>{escape(creature["civilization"])} / {escape(creature["id"])}</h2>',
                f'      <p>{escape(ASSET_CLASS_LABELS.get(asset_class, asset_class))}</p>',
                "    </div>",
                f'    <a href="{escape(board)}">review board</a>',
                "  </header>",
            ])
        )
        if creature["replacement_required"]:
            rows.append(
                '  <div class="warning">Replacement required before 1.0 final audio. '
                'Use these clips for technical playback/listening reference only.</div>'
            )
        elif creature["final_mix_candidate"]:
            rows.append(
                '  <div class="candidate-note">Final mix candidate generated. '
                'Manual listening, loudness and combat-readability acceptance remain pending.</div>'
            )
        rows.append('  <div class="events">')
        for event in creature["events"]:
            subtitle = event.get("subtitle") or ""
            files = event.get("files", [])
            first_file = files[0] if files else {}
            metadata = first_file.get("metadata", {}) if isinstance(first_file, dict) else {}
            sound_path = audition_index_relative(first_file.get("path") if isinstance(first_file, dict) else "")
            duration = metadata.get("duration_seconds")
            size = metadata.get("file_size_bytes")
            duration_text = "n/a" if duration is None else f"{duration:.3f}s"
            size_text = "n/a" if not isinstance(size, int) else f"{size / 1024:.1f} KB"
            metadata_ok = event.get("metadata_ok")
            rows.append(
                "\n".join([
                    '    <article class="event">',
                    '      <div class="event-meta">',
                    f'        <strong>{escape(event.get("event"))}</strong>',
                    f'        <span>{escape(event.get("sound_key"))}</span>',
                    f'        <span>{escape(subtitle)}</span>',
                    f'        <span>{escape(duration_text)} · {escape(size_text)} · metadata_ok={escape(metadata_ok)}</span>',
                    '      </div>',
                    f'      <audio controls preload="none" src="{escape(sound_path)}"></audio>',
                    '    </article>',
                ])
            )
        rows.append('  </div>')
        rows.append('  <details><summary>Listening checklist</summary><ul>')
        for question in creature["review_questions"]:
            rows.append(f'    <li>{escape(question)}</li>')
        rows.append('  </ul></details>\n</section>')

    rows_html = "\n".join(rows)
    html_text = f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>MythicAgeMod Creature Audio Final Mix Audition</title>
  <style>
    :root {{
      color-scheme: light;
      --bg: #eee9dd;
      --panel: #fbf7ed;
      --ink: #24201b;
      --muted: #6c6255;
      --line: #d7cdbb;
      --ok: #3f7e52;
      --warn: #b0702d;
      --bad: #9e403a;
      --candidate: #3a709e;
    }}
    body {{ margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: var(--bg); color: var(--ink); }}
    main {{ max-width: 1180px; margin: 0 auto; padding: 28px 20px 56px; }}
    h1 {{ margin: 0 0 8px; font-size: 32px; }}
    .summary {{ display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin: 22px 0; }}
    .metric {{ background: var(--panel); border: 1px solid var(--line); border-radius: 8px; padding: 14px; }}
    .metric span {{ display: block; color: var(--muted); font-size: 13px; }}
    .metric strong {{ font-size: 24px; }}
    .creature {{ background: var(--panel); border: 1px solid var(--line); border-left: 8px solid var(--ok); border-radius: 8px; margin: 18px 0; padding: 18px; }}
    .creature.replace {{ border-left-color: var(--bad); }}
    .creature.candidate {{ border-left-color: var(--candidate); }}
    .creature header {{ display: flex; justify-content: space-between; gap: 16px; align-items: start; margin-bottom: 12px; }}
    .creature h2 {{ margin: 0; font-size: 24px; }}
    .creature p {{ margin: 4px 0 0; color: var(--muted); }}
    .creature a {{ color: #315b87; }}
    .warning {{ background: #f7e6df; color: var(--bad); border: 1px solid #dfb6aa; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; }}
    .candidate-note {{ background: #e4eef7; color: var(--candidate); border: 1px solid #b7cee1; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; }}
    .events {{ display: grid; gap: 10px; }}
    .event {{ display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 14px; align-items: center; border-top: 1px solid var(--line); padding-top: 10px; }}
    .event-meta {{ display: grid; gap: 3px; }}
    .event-meta span {{ color: var(--muted); font-size: 13px; overflow-wrap: anywhere; }}
    audio {{ width: 100%; }}
    details {{ margin-top: 12px; color: var(--muted); }}
    li {{ margin: 4px 0; }}
    @media (max-width: 760px) {{
      .summary {{ grid-template-columns: repeat(2, minmax(0, 1fr)); }}
      .event {{ grid-template-columns: 1fr; }}
    }}
  </style>
</head>
<body>
<main>
  <h1>MythicAgeMod 1.0 Creature Audio Final Mix Audition</h1>
  <p>This local page supports manual listening review only. It does not mark final_mix_acceptance_ok=true.</p>
  <div class="summary">
    <div class="metric"><span>creatures</span><strong>{payload['creature_count']}</strong></div>
    <div class="metric"><span>events</span><strong>{payload['event_count']}</strong></div>
    <div class="metric"><span>replacement required</span><strong>{payload['replacement_required_count']}</strong></div>
    <div class="metric"><span>candidate listening</span><strong>{payload['candidate_listening_required_count']}</strong></div>
    <div class="metric"><span>human listening</span><strong>{escape(payload['human_listening_acceptance_ok'])}</strong></div>
  </div>
  {rows_html}
</main>
</body>
</html>
"""
    AUDITION_INDEX_PATH.write_text(html_text, encoding="utf-8")
    payload["audition_index"] = project_path(AUDITION_INDEX_PATH)
    payload["audition_index_present"] = AUDITION_INDEX_PATH.exists()
    payload["audition_event_count"] = sum(
        1
        for creature in payload["creatures"]
        for event in creature["events"]
        if event.get("files")
    )


def build_package() -> dict[str, Any]:
    audio_report = load_json(AUDIO_REPORT, {})
    art_direction = load_json(AUDIO_ART_DIRECTION, {})
    tooling_status = audio_tooling_status()
    final_acceptance_shared = art_direction.get("final_acceptance_shared", []) if isinstance(art_direction, dict) else []
    creatures = []
    for creature in audio_report.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        events = [summarize_event(event) for event in creature.get("events", []) if isinstance(event, dict)]
        asset_class = creature.get("asset_class")
        final_mix_candidate = asset_class == "final_mix_candidate_pending_listening"
        replacement_required = asset_class in {"procedural_first_pass", "technical_placeholder_repair"}
        metadata_ok = bool(events) and all(event["metadata_ok"] for event in events)
        technical_ok = bool(creature.get("technical_ok")) and metadata_ok
        creatures.append({
            "id": creature.get("id"),
            "civilization": creature.get("civilization"),
            "asset_class": asset_class,
            "final_mix_candidate": final_mix_candidate,
            "replacement_required": replacement_required,
            "listening_required": True,
            "technical_ok": technical_ok,
            "metadata_ok": metadata_ok,
            "art_direction_status": creature.get("art_direction", {}).get("final_asset_status"),
            "human_listening_acceptance": "pending",
            "final_mix_accepted": False,
            "events": events,
            "review_questions": [
                "是否符合该生物的文明音色和神话身份？",
                "ambient/hurt/death/attack/special 是否能在战斗中互相区分？",
                "攻击和特殊音效是否与动作峰值同步，不抢玩家命中反馈？",
                "死亡音效是否比 hurt 更长且能表达体量，但不拖慢战斗节奏？",
                "音量、低频、瞬态和循环尾音是否在 Minecraft 实机环境中可接受？",
            ],
        })

    status_counts = Counter(
        item["asset_class"] if (item["replacement_required"] or item["final_mix_candidate"]) else "authored_or_existing_pending_listening"
        for item in creatures
    )
    replacement_required_count = sum(1 for item in creatures if item["replacement_required"])
    final_mix_candidate_count = sum(1 for item in creatures if item["final_mix_candidate"])
    procedural_first_pass_count = sum(1 for item in creatures if item["asset_class"] == "procedural_first_pass")
    technical_placeholder_repair_count = sum(1 for item in creatures if item["asset_class"] == "technical_placeholder_repair")
    metadata_ok = all(item["metadata_ok"] for item in creatures)
    technical_ok = all(item["technical_ok"] for item in creatures)
    events = [
        event
        for creature in creatures
        for event in creature["events"]
    ]
    metadata_failure_count = sum(1 for event in events if not event.get("metadata_ok"))
    listening_queue = build_listening_queue(creatures)
    candidate_listening_queue = [
        item for item in listening_queue
        if item["final_mix_candidate"]
    ]
    replacement_listening_queue = [
        item for item in listening_queue
        if item["replacement_required"]
    ]
    listening_batches = build_listening_batches(listening_queue)
    listening_total_duration = round(sum(
        float(item.get("duration_seconds") or 0.0)
        for item in listening_queue
    ), 3)
    payload = {
        "version": 1,
        "scope": "mythic_age_1_0_creature_audio_final_mix_acceptance_package",
        "ok": metadata_ok and technical_ok,
        "art_final_ok": False,
        "final_mix_acceptance_ok": False,
        "human_listening_acceptance_ok": False,
        "creature_count": len(creatures),
        "event_count": sum(len(item["events"]) for item in creatures),
        "ogg_container_metrics_ok": metadata_ok,
        "ogg_container_metrics_event_count": len(events),
        "ogg_container_metrics_failure_count": metadata_failure_count,
        "replacement_required_count": replacement_required_count,
        "final_mix_candidate_count": final_mix_candidate_count,
        "candidate_listening_required_count": final_mix_candidate_count,
        "manual_listening_queue_count": len(listening_queue),
        "manual_listening_batch_count": len(listening_batches),
        "manual_listening_total_duration_seconds": listening_total_duration,
        "candidate_listening_queue_count": len(candidate_listening_queue),
        "replacement_listening_queue_count": len(replacement_listening_queue),
        "manual_listening_queue_by_priority": dict(sorted(
            Counter(str(item["priority"]) for item in listening_queue).items(),
            key=lambda item: int(item[0]),
        )),
        "candidate_listening_queue_by_event": dict(sorted(Counter(item["event"] for item in candidate_listening_queue).items())),
        "manual_listening_queue_by_event": dict(sorted(Counter(item["event"] for item in listening_queue).items())),
        "manual_listening_queue_by_civilization": dict(sorted(Counter(item["civilization"] for item in listening_queue).items())),
        "procedural_first_pass_count": procedural_first_pass_count,
        "technical_placeholder_repair_count": technical_placeholder_repair_count,
        "manual_review_required_count": len(creatures),
        "status_counts": dict(sorted(status_counts.items())),
        "sources": {
            "audio_report": project_path(AUDIO_REPORT),
            "audio_art_direction": project_path(AUDIO_ART_DIRECTION),
        },
        "final_acceptance_shared": final_acceptance_shared,
        "audio_tooling_status": tooling_status,
        "mix_metrics_available": tooling_status["loudness_metrics_available"],
        "mix_metrics_gap": tooling_status["missing_reason"],
        "final_mix_tooling_blocker": tooling_status["blocking_for_final_mix_acceptance"],
        "event_listening_criteria": EVENT_LISTENING_CRITERIA,
        "manual_listening_batches": listening_batches,
        "manual_listening_queue": listening_queue,
        "candidate_listening_queue": candidate_listening_queue,
        "creatures": creatures,
    }
    draw_final_mix_review_boards(payload)
    write_audition_index(payload)
    return payload


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 生物音效最终混音验收包",
        "",
        "本文档由 `tools/creature_audio_final_mix_acceptance_package.py audit` 生成。它只聚合文件元数据、音效方向和人工听感待办，不会把程序化首版音效标记为最终资源。",
        "",
        "## 总览",
        "",
        f"- 生物数量：{payload['creature_count']}",
        f"- 事件数量：{payload['event_count']}",
        f"- OGG 元数据/时长检查通过：{payload['ok']}",
        f"- OGG 容器指标事件覆盖：{payload['ogg_container_metrics_event_count'] - payload['ogg_container_metrics_failure_count']} / {payload['ogg_container_metrics_event_count']}",
        f"- 最终混音验收通过：{payload['final_mix_acceptance_ok']}",
        f"- 人工听感验收通过：{payload['human_listening_acceptance_ok']}",
        f"- 响度/频谱工具可用：{payload['mix_metrics_available']}",
        f"- 最终混音工具链阻塞：{payload['final_mix_tooling_blocker']}",
        f"- 需要替换占位/程序首版音效的生物数：{payload['replacement_required_count']}",
        f"- 已生成最终混音候选、待听感验收的生物数：{payload['candidate_listening_required_count']}",
        f"- 逐事件人工试听队列：{payload['manual_listening_queue_count']}",
        f"- 人工试听批次：{payload['manual_listening_batch_count']}",
        f"- 人工试听素材总时长：{payload['manual_listening_total_duration_seconds']} 秒",
        f"- 人工试听队列按优先级：`{json.dumps(payload['manual_listening_queue_by_priority'], ensure_ascii=False)}`",
        f"- 最终混音候选逐事件试听队列：{payload['candidate_listening_queue_count']}",
        f"- 需替换资产逐事件试听队列：{payload['replacement_listening_queue_count']}",
        f"- 程序化首版生物数：{payload['procedural_first_pass_count']}",
        f"- 技术修复占位生物数：{payload['technical_placeholder_repair_count']}",
        f"- 需要人工听感验收的生物数：{payload['manual_review_required_count']}",
        f"- 最终混音 review board：{payload.get('final_mix_review_board_count', 0)} / {payload['creature_count']}",
        f"- Review board 总览图：`{payload.get('final_mix_review_board_contact_sheet', '')}`",
        f"- 本地试听验收页面：`{payload.get('audition_index', '')}`",
        f"- 可试听事件数：{payload.get('audition_event_count', 0)} / {payload['event_count']}",
        f"- 状态统计：`{json.dumps(payload['status_counts'], ensure_ascii=False)}`",
        "",
        "## 音频工具链状态",
        "",
        f"- 内置容器解析：`{payload['audio_tooling_status']['container_parser']}`",
        f"- 容器指标可用：`{payload['audio_tooling_status']['container_metrics_available']}`",
        f"- 解码工具可用：`{payload['audio_tooling_status']['decoder_available']}`",
        f"- 响度指标可用：`{payload['audio_tooling_status']['loudness_metrics_available']}`",
        f"- 频谱指标可用：`{payload['audio_tooling_status']['spectral_metrics_available']}`",
        f"- 缺失工具：`{', '.join(payload['audio_tooling_status']['missing_tools']) or 'none'}`",
        f"- 缺口说明：{payload['mix_metrics_gap']}",
        "",
        "## 事件听审标准",
        "",
        "| 事件 | 听审标准 |",
        "| --- | --- |",
    ]
    for event, criteria in sorted(EVENT_LISTENING_CRITERIA.items(), key=lambda item: event_priority(item[0])):
        lines.append(f"| `{event}` | {'；'.join(criteria).replace('|', '/')} |")
    lines.extend([
        "",
        "## 人工试听批次计划",
        "",
        "批次按事件重要性排序；special/attack/charge 优先，因为它们最容易影响战斗读秒、动作峰值和技能识别。",
        "",
        "| 批次 | 优先级 | 事件 | 任务数 | 候选数 | 替换项 | 总时长秒 | 生物 |",
        "| --- | ---: | --- | ---: | ---: | ---: | ---: | --- |",
    ])
    for batch in payload["manual_listening_batches"]:
        creatures = ", ".join(batch["creatures"]).replace("|", "/")
        if len(creatures) > 180:
            creatures = creatures[:177] + "..."
        lines.append(
            "| {batch_id} | {priority} | `{event}` | {count} | {candidate} | {replacement} | {duration} | {creatures} |".format(
                batch_id=batch["batch_id"],
                priority=batch["priority"],
                event=batch["event"],
                count=batch["task_count"],
                candidate=batch["candidate_task_count"],
                replacement=batch["replacement_task_count"],
                duration=batch["total_duration_seconds"],
                creatures=creatures,
            )
        )
    lines.extend([
        "",
        "## 逐事件试听队列摘要",
        "",
        f"- 全量试听队列按事件：`{json.dumps(payload['manual_listening_queue_by_event'], ensure_ascii=False)}`",
        f"- 全量试听队列按文明：`{json.dumps(payload['manual_listening_queue_by_civilization'], ensure_ascii=False)}`",
        f"- 最终混音候选队列按事件：`{json.dumps(payload['candidate_listening_queue_by_event'], ensure_ascii=False)}`",
        "",
        "| 优先级 | 文明 | 生物 | 事件 | 资产状态 | 时长 | 路径 | 关注点 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ])
    for item in payload["manual_listening_queue"][:80]:
        duration = item.get("duration_seconds")
        duration_text = "n/a" if duration is None else f"{duration:.3f}s"
        lines.append(
            "| {priority} | {civilization} | `{creature}` | `{event}` | `{asset_class}` | {duration} | `{path}` | `{focus}` |".format(
                priority=item["priority"],
                civilization=item["civilization"],
                creature=item["creature_id"],
                event=item["event"],
                asset_class=item["asset_class"],
                duration=duration_text,
                path=item.get("path") or "",
                focus=item["review_focus"],
            )
        )
    lines.extend([
        "",
        "## 单体清单",
        "",
        "| 文明 | 生物 | 资产状态 | OGG 元数据 | 需替换 | Review board | Audition anchor | 事件数 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ])
    for creature in payload["creatures"]:
        lines.append(
            "| {civilization} | `{id}` | `{asset_class}` | `{metadata_ok}` | `{replacement}` | `{board}` | `#{anchor}` | {event_count} |".format(
                civilization=creature["civilization"],
                id=creature["id"],
                asset_class=creature["asset_class"],
                metadata_ok=creature["metadata_ok"],
                replacement=creature["replacement_required"],
                board=creature.get("final_mix_review_board", ""),
                anchor=f"{creature['civilization']}-{creature['id']}",
                event_count=len(creature["events"]),
            )
        )
    lines.extend([
        "",
        "## 验收规则",
        "",
        "- `replacement_required=true` 的生物不能进入 1.0 最终音频，只能作为技术占位或听感参考。",
        "- `final_mix_candidate_pending_listening` 表示已经替换掉占位/首版资源，但仍需要人工听感、响度和战斗可读性验收。",
        "- 当前环境没有本地响度/频谱解码工具，响度、峰值、频谱平衡和听感必须通过外部音频工具、音频 MCP 或人工试听补证。",
        "- 最终混音通过前，每个生物至少检查 ambient、hurt、death、attack/special 的身份辨识、战斗可读性和与动作峰值的同步。",
        "",
    ])
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
        status = "PASS" if payload["ok"] else "FAIL"
        print(f"Mythic Age Creature Audio Final Mix Package: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(
            "creatures: {creatures}, events: {events}, replacement_required: {replacement}".format(
                creatures=payload["creature_count"],
                events=payload["event_count"],
                replacement=payload["replacement_required_count"],
            )
        )
        if not payload["ok"]:
            failed = [item["id"] for item in payload["creatures"] if not item["metadata_ok"]]
            print("failed metadata: " + ", ".join(failed), file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
