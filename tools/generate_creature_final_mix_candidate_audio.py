#!/usr/bin/env python3
"""Generate final-mix candidate OGGs for non-final creature audio.

These files are better structured than the procedural first pass / technical
repair sounds, but they still require human listening review before 1.0 final.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import random
import shutil
import struct
import subprocess
import tempfile
import wave
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/audio_validation"
AUDIO_REPORT = REPORT_DIR / "audio_report.json"
REPORT_PATH = REPORT_DIR / "creature_final_mix_candidate_audio_report.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/audio/creature_final_mix_candidate_audio_report.md"

FINAL_MIX_CANDIDATE_TARGETS = {
    "minotaur",
    "anubis_warrior",
    "scarab_guardian",
    "mummy_soldier",
    "sphinx_sentinel",
    "sun_falcon",
    "frost_giant",
    "draugr_warrior",
    "valkyrie",
    "fenrir_wolf",
    "dwarf_berserker",
    "azure_dragon",
    "qilin_guardian",
    "jiangshi",
    "nine_tailed_fox",
    "stone_lion_guardian",
    "crystal_leviathan",
    "orichalcum_golem",
    "siren_priestess",
    "tidal_guardian",
    "abyssal_ray",
}

CREATURE_CIVILIZATION = {
    **{creature: "greek" for creature in [
        "minotaur", "cyclops", "centaur", "hydra", "medusa",
        "nemean_lion", "manticore", "chimera", "colossus", "pegasus",
    ]},
    **{creature: "egyptian" for creature in [
        "anubis_warrior", "scarab_guardian", "mummy_soldier", "sphinx_sentinel", "sun_falcon",
    ]},
    **{creature: "nordic" for creature in [
        "frost_giant", "draugr_warrior", "valkyrie", "fenrir_wolf", "dwarf_berserker",
    ]},
    **{creature: "chinese" for creature in [
        "azure_dragon", "qilin_guardian", "jiangshi", "nine_tailed_fox", "stone_lion_guardian",
    ]},
    **{creature: "atlantean" for creature in [
        "crystal_leviathan", "orichalcum_golem", "siren_priestess", "tidal_guardian", "abyssal_ray",
    ]},
}

EVENT_DURATIONS = {
    "ambient": 1.35,
    "hurt": 0.48,
    "death": 1.78,
    "attack": 0.62,
    "special": 1.12,
    "charge": 0.96,
    "fly": 1.08,
}

EVENT_GAIN = {
    "ambient": 0.34,
    "hurt": 0.52,
    "death": 0.46,
    "attack": 0.55,
    "special": 0.5,
    "charge": 0.58,
    "fly": 0.34,
}

EVENT_SHIFT = {
    "ambient": 0.82,
    "hurt": 1.7,
    "death": 0.58,
    "attack": 1.35,
    "special": 1.52,
    "charge": 1.12,
    "fly": 1.2,
}

CIV_PROFILES = {
    "greek": {
        "base": 118.0,
        "intervals": (1.0, 1.5, 2.0),
        "noise": 0.12,
        "metal": 0.22,
        "pulse": 5.0,
        "stereo": 0.05,
    },
    "egyptian": {
        "base": 132.0,
        "intervals": (1.0, 1.25, 1.5),
        "noise": 0.09,
        "metal": 0.18,
        "pulse": 6.0,
        "stereo": 0.07,
    },
    "nordic": {
        "base": 92.0,
        "intervals": (1.0, 1.33, 1.78),
        "noise": 0.16,
        "metal": 0.12,
        "pulse": 4.0,
        "stereo": 0.06,
    },
    "chinese": {
        "base": 176.0,
        "intervals": (1.0, 1.25, 1.5, 2.0),
        "noise": 0.07,
        "metal": 0.28,
        "pulse": 7.0,
        "stereo": 0.08,
    },
    "atlantean": {
        "base": 146.0,
        "intervals": (1.0, 1.42, 1.68, 2.24),
        "noise": 0.1,
        "metal": 0.32,
        "pulse": 3.5,
        "stereo": 0.1,
    },
}

CREATURE_TRAITS = {
    "minotaur": {"weight": 1.25, "rough": 1.35, "transient": 1.4},
    "scarab_guardian": {"click": 1.0, "metal": 1.25},
    "sun_falcon": {"air": 1.25, "bright": 1.35},
    "frost_giant": {"weight": 1.4, "rough": 1.15},
    "fenrir_wolf": {"rough": 1.5, "low": 1.25},
    "azure_dragon": {"air": 1.1, "bright": 1.2},
    "qilin_guardian": {"bright": 1.35, "metal": 1.15},
    "jiangshi": {"click": 0.8, "rough": 0.75},
    "nine_tailed_fox": {"bright": 1.5, "air": 1.2},
    "stone_lion_guardian": {"weight": 1.25, "metal": 1.1},
    "crystal_leviathan": {"low": 1.2, "metal": 1.35, "air": 1.1},
    "orichalcum_golem": {"weight": 1.45, "metal": 1.45},
    "siren_priestess": {"bright": 1.4, "air": 1.25},
    "tidal_guardian": {"weight": 1.2, "low": 1.15},
    "abyssal_ray": {"air": 1.45, "low": 1.1},
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def find_ffmpeg() -> str | None:
    return shutil.which("ffmpeg") or next(
        (candidate for candidate in ["/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg"] if Path(candidate).exists()),
        None,
    )


def sound_ref_path(sound_ref: str) -> Path:
    namespace = "mythic_age"
    path = sound_ref
    if ":" in sound_ref:
        namespace, path = sound_ref.split(":", 1)
    return ROOT / f"src/main/resources/assets/{namespace}/sounds/{path}.ogg"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def looks_like_ogg_vorbis(path: Path) -> bool:
    if not path.exists() or path.stat().st_size == 0:
        return False
    header = path.read_bytes()[:96]
    return header.startswith(b"OggS") and b"vorbis" in header


def envelope_for(event: str, progress: float) -> float:
    if event == "hurt":
        attack = min(1.0, progress / 0.035)
        release = max(0.0, (1.0 - progress) ** 1.8)
        return attack * release
    if event == "attack":
        attack = min(1.0, progress / 0.045)
        release = min(1.0, (1.0 - progress) / 0.22)
        return max(0.0, min(attack, release))
    if event == "charge":
        attack = min(1.0, progress / 0.28)
        release = min(1.0, (1.0 - progress) / 0.12)
        return max(0.0, min(attack, release))
    if event == "death":
        attack = min(1.0, progress / 0.08)
        release = max(0.0, (1.0 - progress) ** 0.65)
        return attack * release
    if event == "special":
        attack = min(1.0, progress / 0.14)
        release = min(1.0, (1.0 - progress) / 0.2)
        return max(0.0, min(attack, release))
    attack = min(1.0, progress / 0.16)
    release = min(1.0, (1.0 - progress) / 0.25)
    return max(0.0, min(attack, release))


def transient_for(event: str, progress: float, rng: random.Random, traits: dict[str, float]) -> float:
    amount = float(traits.get("transient", 1.0))
    if event in {"hurt", "attack", "charge"} and progress < 0.12:
        return (rng.random() * 2.0 - 1.0) * (0.42 * amount) * (1.0 - progress / 0.12)
    if event == "death" and 0.08 < progress < 0.28:
        return (rng.random() * 2.0 - 1.0) * 0.18 * (1.0 - (progress - 0.08) / 0.2)
    return 0.0


def synthesize_wave(creature_id: str, event: str, path: Path) -> None:
    sample_rate = 44100
    duration = EVENT_DURATIONS.get(event, 0.9)
    total_samples = int(sample_rate * duration)
    civilization = CREATURE_CIVILIZATION.get(creature_id, "greek")
    profile = CIV_PROFILES[civilization]
    traits = CREATURE_TRAITS.get(creature_id, {})
    rng = random.Random(f"{creature_id}:{event}:mythic_age_final_mix_candidate")
    stable = int(hashlib.sha256(creature_id.encode("utf-8")).hexdigest()[:8], 16)
    base = float(profile["base"]) + stable % 37
    base *= float(traits.get("low", 1.0)) ** -0.12
    event_shift = EVENT_SHIFT.get(event, 1.0)
    gain = EVENT_GAIN.get(event, 0.42)
    gain *= float(traits.get("weight", 1.0)) ** 0.08
    intervals = tuple(float(value) for value in profile["intervals"])
    rough = float(traits.get("rough", 1.0))
    bright = float(traits.get("bright", 1.0))
    metal = float(profile["metal"]) * float(traits.get("metal", 1.0))
    noise_amount = float(profile["noise"]) * rough
    stereo_width = float(profile["stereo"])

    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        frames = bytearray()
        for index in range(total_samples):
            t = index / sample_rate
            progress = index / max(1, total_samples - 1)
            env = envelope_for(event, progress)
            event_sweep = {
                "death": 1.12 - 0.42 * progress,
                "special": 0.82 + 0.34 * progress,
                "charge": 0.72 + 0.55 * progress,
                "hurt": 1.2 - 0.18 * progress,
                "attack": 0.95 + 0.24 * progress,
            }.get(event, 1.0 + math.sin(progress * math.pi) * 0.05)
            freq = base * event_shift * event_sweep
            tone = 0.0
            for harmonic_index, interval in enumerate(intervals):
                phase = math.sin(t * (float(profile["pulse"]) + harmonic_index * 0.7)) * 0.35
                weight = 0.56 / (harmonic_index + 1)
                tone += weight * math.sin(2.0 * math.pi * freq * interval * t + phase)
            low_layer = math.sin(2.0 * math.pi * freq * 0.5 * t) * 0.22 * float(traits.get("low", 1.0))
            chime = math.sin(2.0 * math.pi * freq * 2.75 * t) * metal * (0.55 if event in {"special", "ambient", "death"} else 0.32)
            click = 0.0
            if traits.get("click") and (index % max(90, int(sample_rate / (18 + stable % 7))) < 8):
                click = (rng.random() * 2.0 - 1.0) * 0.18 * float(traits["click"])
            air = (rng.random() * 2.0 - 1.0) * noise_amount * float(traits.get("air", 1.0))
            transient = transient_for(event, progress, rng, traits)
            sample = (tone * 0.64 + low_layer + chime * bright + click + air + transient) * env * gain
            sample = max(-0.92, min(0.92, sample))
            pan = math.sin(2.0 * math.pi * (0.18 + (stable % 9) * 0.01) * t) * stereo_width
            left = int(max(-1.0, min(1.0, sample * (1.0 - pan))) * 32767)
            right = int(max(-1.0, min(1.0, sample * (1.0 + pan))) * 32767)
            frames.extend(struct.pack("<hh", left, right))
        wav.writeframes(frames)


def encode_ogg(ffmpeg: str, wav_path: Path, ogg_path: Path) -> None:
    ogg_path.parent.mkdir(parents=True, exist_ok=True)
    commands = [
        [
            ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
            "-i", str(wav_path), "-c:a", "vorbis", "-strict", "-2", "-q:a", "4", str(ogg_path),
        ],
        [
            ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
            "-i", str(wav_path), "-c:a", "libvorbis", "-q:a", "4", str(ogg_path),
        ],
    ]
    last_error: subprocess.CalledProcessError | None = None
    for command in commands:
        try:
            subprocess.run(command, check=True)
            return
        except subprocess.CalledProcessError as error:
            last_error = error
    if last_error:
        raise last_error


def target_creatures(audio_report: dict[str, Any]) -> list[dict[str, Any]]:
    creatures = []
    for creature in audio_report.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        creature_id = creature.get("id")
        if creature_id in FINAL_MIX_CANDIDATE_TARGETS:
            creatures.append(creature)
    return creatures


def build_report(generated: list[dict[str, Any]], ffmpeg: str | None, missing_targets: list[str]) -> dict[str, Any]:
    generated_creatures = sorted({item["creature_id"] for item in generated if item.get("ok")})
    return {
        "version": 1,
        "scope": "mythic_age_1_0_creature_final_mix_candidate_audio",
        "ok": bool(ffmpeg) and not missing_targets and all(item.get("ok") for item in generated),
        "ffmpeg": ffmpeg,
        "asset_class": "final_mix_candidate_pending_listening",
        "final_mix_accepted": False,
        "human_listening_acceptance": "pending",
        "generated_count": len(generated),
        "generated_creatures": generated_creatures,
        "generated_creature_count": len(generated_creatures),
        "missing_targets": missing_targets,
        "generated": generated,
    }


def generate() -> dict[str, Any]:
    ffmpeg = find_ffmpeg()
    audio_report = load_json(AUDIO_REPORT, {})
    targets = target_creatures(audio_report)
    seen_targets = {item.get("id") for item in targets}
    missing_targets = sorted(FINAL_MIX_CANDIDATE_TARGETS - seen_targets)
    generated: list[dict[str, Any]] = []
    if not ffmpeg:
        return build_report(generated, ffmpeg, missing_targets)

    with tempfile.TemporaryDirectory() as temp_dir:
        temp = Path(temp_dir)
        for creature in targets:
            creature_id = str(creature.get("id"))
            civilization = str(creature.get("civilization") or CREATURE_CIVILIZATION.get(creature_id, "unknown"))
            for event in creature.get("events", []):
                if not isinstance(event, dict):
                    continue
                event_name = str(event.get("event"))
                sound_refs = [ref for ref in event.get("sound_refs", []) if isinstance(ref, str)]
                for sound_ref in sound_refs:
                    out_path = sound_ref_path(sound_ref)
                    wav_path = temp / f"{creature_id}_{event_name}_{len(generated)}.wav"
                    synthesize_wave(creature_id, event_name, wav_path)
                    encode_ogg(ffmpeg, wav_path, out_path)
                    generated.append({
                        "creature_id": creature_id,
                        "civilization": civilization,
                        "event": event_name,
                        "sound_ref": sound_ref,
                        "path": project_path(out_path),
                        "duration_target_seconds": EVENT_DURATIONS.get(event_name, 0.9),
                        "file_size_bytes": out_path.stat().st_size,
                        "sha256": sha256(out_path),
                        "status": "final_mix_candidate_pending_listening",
                        "ok": looks_like_ogg_vorbis(out_path),
                    })
    return build_report(generated, ffmpeg, missing_targets)


def write_markdown(payload: dict[str, Any]) -> None:
    counts: dict[str, int] = {}
    for item in payload.get("generated", []):
        civilization = str(item.get("civilization"))
        counts[civilization] = counts.get(civilization, 0) + 1
    lines = [
        "# MythicAgeMod 1.0 生物最终混音候选音效生成报告",
        "",
        "这些 OGG 已替换程序化首版/技术修复占位文件，但仍是 `final_mix_candidate_pending_listening`，不能自动视为 1.0 最终验收通过。",
        "",
        f"- 生成通过：{payload['ok']}",
        f"- 生成事件数：{payload['generated_count']}",
        f"- 覆盖生物数：{payload['generated_creature_count']}",
        f"- 覆盖生物：`{', '.join(payload['generated_creatures'])}`",
        f"- 缺失目标：`{', '.join(payload['missing_targets'])}`",
        f"- ffmpeg：`{payload.get('ffmpeg')}`",
        f"- 按文明事件数：`{json.dumps(dict(sorted(counts.items())), ensure_ascii=False)}`",
        "",
        "## 规则",
        "",
        "- 该报告只证明候选音频文件已生成且为 OGG/Vorbis。",
        "- 人工听感、响度、频谱平衡、战斗可读性仍由最终混音验收包跟踪。",
        "- 如果重新设计某个生物的动作峰值或文明音色，应重新生成并试听对应候选。",
        "",
    ]
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["generate"])
    args = parser.parse_args()
    if args.command == "generate":
        payload = generate()
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        status = "PASS" if payload["ok"] else "FAIL"
        print(f"Mythic Age Creature Final Mix Candidate Audio: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"generated_creatures: {payload['generated_creature_count']}, generated_events: {payload['generated_count']}")
        if payload["missing_targets"]:
            print("missing targets: " + ", ".join(payload["missing_targets"]))
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
