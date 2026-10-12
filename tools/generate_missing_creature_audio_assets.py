#!/usr/bin/env python3
"""Generate technical placeholder OGGs for missing or empty creature audio files."""

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


ROOT = Path(__file__).resolve().parents[1]
SOUND_DIR = ROOT / "src/main/resources/assets/mythic_age/sounds/entity"
REPORT_DIR = ROOT / "docs/preview_assets/audio_validation"
REPORT_PATH = REPORT_DIR / "audio_placeholder_repair_report.json"

CIVILIZATION_BASE = {
    "greek": 116.0,
    "egyptian": 125.0,
    "nordic": 92.0,
    "chinese": 176.0,
    "atlantean": 148.0,
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
    "ambient": 0.95,
    "hurt": 0.36,
    "death": 1.22,
    "attack": 0.42,
    "special": 0.86,
    "charge": 0.74,
    "fly": 0.78,
    "step": 0.18,
}
EVENT_GAIN = {
    "ambient": 0.32,
    "hurt": 0.46,
    "death": 0.42,
    "attack": 0.48,
    "special": 0.5,
    "charge": 0.5,
    "fly": 0.32,
    "step": 0.26,
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def find_ffmpeg() -> str | None:
    return shutil.which("ffmpeg") or next(
        (candidate for candidate in ["/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg"] if Path(candidate).exists()),
        None,
    )


def looks_like_ogg_vorbis(path: Path) -> bool:
    if not path.exists() or path.stat().st_size == 0:
        return False
    header = path.read_bytes()[:96]
    return header.startswith(b"OggS") and b"vorbis" in header


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def synthesize_wave(creature_id: str, event: str, path: Path) -> None:
    sample_rate = 44100
    duration = EVENT_DURATIONS.get(event, 0.7)
    total_samples = int(sample_rate * duration)
    rng = random.Random(f"{creature_id}:{event}:mythic_age_audio_repair")
    civilization = CREATURE_CIVILIZATION.get(creature_id, "greek")
    stable_id = int(hashlib.sha256(creature_id.encode("utf-8")).hexdigest()[:8], 16)
    base = CIVILIZATION_BASE[civilization] + (stable_id % 48)
    event_shift = {
        "ambient": 0.72,
        "hurt": 1.52,
        "death": 0.58,
        "attack": 1.25,
        "special": 1.84,
        "charge": 1.08,
        "fly": 1.36,
        "step": 0.9,
    }.get(event, 1.0)
    freq = base * event_shift
    secondary = freq * (1.46 + rng.random() * 0.22)
    tremor = 2.5 + rng.random() * 4.5
    gain = EVENT_GAIN.get(event, 0.36)

    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        frames = bytearray()
        for index in range(total_samples):
            t = index / sample_rate
            progress = index / max(1, total_samples - 1)
            attack = min(1.0, progress / 0.06)
            release = min(1.0, (1.0 - progress) / 0.18)
            envelope = max(0.0, min(attack, release))
            sweep = 1.0 + (0.2 if event in {"attack", "special", "charge"} else -0.07) * progress
            tone = math.sin(2.0 * math.pi * freq * sweep * t)
            tone += 0.38 * math.sin(2.0 * math.pi * secondary * t + math.sin(t * tremor) * 0.75)
            tone += 0.2 * math.sin(2.0 * math.pi * (freq * 0.5) * t)
            noise_level = 0.18 if event in {"hurt", "death", "attack", "charge", "step"} else 0.08
            noise = (rng.random() * 2.0 - 1.0) * noise_level
            sample = (tone * 0.62 + noise) * envelope * gain
            value = int(max(-1.0, min(1.0, sample)) * 32767)
            frames.extend(struct.pack("<hh", value, value))
        wav.writeframes(frames)


def encode_ogg(ffmpeg: str, wav_path: Path, ogg_path: Path) -> None:
    ogg_path.parent.mkdir(parents=True, exist_ok=True)
    commands = [
        [
            ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
            "-i", str(wav_path), "-c:a", "libvorbis", "-q:a", "3", str(ogg_path),
        ],
        [
            ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
            "-i", str(wav_path), "-c:a", "vorbis", "-strict", "-2", "-q:a", "3", str(ogg_path),
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


def invalid_audio_files() -> list[Path]:
    return [
        path
        for path in sorted(SOUND_DIR.glob("*/*.ogg"))
        if path.parent.name in CREATURE_CIVILIZATION and not looks_like_ogg_vorbis(path)
    ]


def build_repair_report(generated: list[dict[str, object]], ffmpeg: str | None) -> dict[str, object]:
    return {
        "version": 1,
        "scope": "technical_placeholder_repair_for_missing_creature_audio",
        "ok": bool(ffmpeg) and all(item.get("ok") for item in generated),
        "ffmpeg": ffmpeg,
        "asset_status": "technical_placeholder_not_final_mix",
        "generated_count": len(generated),
        "generated_creatures": sorted({item["creature_id"] for item in generated}),
        "generated": generated,
    }


def repair() -> dict[str, object]:
    ffmpeg = find_ffmpeg()
    targets = invalid_audio_files()
    generated: list[dict[str, object]] = []
    if not ffmpeg:
        return build_repair_report([
            {
                "path": project_path(path),
                "creature_id": path.parent.name,
                "event": path.stem,
                "ok": False,
                "error": "ffmpeg_not_available",
            }
            for path in targets
        ], ffmpeg)

    with tempfile.TemporaryDirectory() as temp_dir:
        temp = Path(temp_dir)
        for target in targets:
            creature_id = target.parent.name
            event = target.stem
            wav_path = temp / f"{creature_id}_{event}.wav"
            synthesize_wave(creature_id, event, wav_path)
            encode_ogg(ffmpeg, wav_path, target)
            generated.append({
                "path": project_path(target),
                "creature_id": creature_id,
                "event": event,
                "status": "technical_placeholder_not_final_mix",
                "file_size_bytes": target.stat().st_size,
                "sha256": sha256(target),
                "ok": looks_like_ogg_vorbis(target),
            })
    return build_repair_report(generated, ffmpeg)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["repair"])
    args = parser.parse_args()
    if args.command == "repair":
        payload = repair()
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        status = "PASS" if payload["ok"] else "FAIL"
        print(f"Mythic Age Missing Creature Audio Repair: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"generated: {payload['generated_count']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
