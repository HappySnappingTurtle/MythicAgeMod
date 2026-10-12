#!/usr/bin/env python3
"""Generate first-pass creature audio resources and wire ModSounds references."""

from __future__ import annotations

import json
import hashlib
import math
import random
import re
import shutil
import struct
import subprocess
import tempfile
import wave
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOUNDS_JSON = ROOT / "src/main/resources/assets/mythic_age/sounds.json"
MOD_SOUNDS = ROOT / "src/main/java/com/example/mythic_age/sound/ModSounds.java"
ENTITY_DIR = ROOT / "src/main/java/com/example/mythic_age/entity"
SOUND_DIR = ROOT / "src/main/resources/assets/mythic_age/sounds/entity"

CREATURES = {
    "anubis_warrior": "AnubisWarriorEntity",
    "scarab_guardian": "ScarabGuardianEntity",
    "mummy_soldier": "MummySoldierEntity",
    "sphinx_sentinel": "SphinxSentinelEntity",
    "sun_falcon": "SunFalconEntity",
    "frost_giant": "FrostGiantEntity",
    "draugr_warrior": "DraugrWarriorEntity",
    "valkyrie": "ValkyrieEntity",
    "fenrir_wolf": "FenrirWolfEntity",
    "dwarf_berserker": "DwarfBerserkerEntity",
    "azure_dragon": "AzureDragonEntity",
    "qilin_guardian": "QilinGuardianEntity",
    "jiangshi": "JiangshiEntity",
    "nine_tailed_fox": "NineTailedFoxEntity",
    "stone_lion_guardian": "StoneLionGuardianEntity",
    "crystal_leviathan": "CrystalLeviathanEntity",
    "orichalcum_golem": "OrichalcumGolemEntity",
    "siren_priestess": "SirenPriestessEntity",
    "tidal_guardian": "TidalGuardianEntity",
    "abyssal_ray": "AbyssalRayEntity",
}

EVENTS = ["ambient", "hurt", "death", "attack", "special"]
EVENT_DURATIONS = {
    "ambient": 0.92,
    "hurt": 0.34,
    "death": 1.18,
    "attack": 0.38,
    "special": 0.82,
}
EVENT_GAIN = {
    "ambient": 0.32,
    "hurt": 0.46,
    "death": 0.42,
    "attack": 0.48,
    "special": 0.5,
}
CIVILIZATION_BASE = {
    "egyptian": 125.0,
    "nordic": 92.0,
    "chinese": 176.0,
    "atlantean": 148.0,
}
CREATURE_CIVILIZATION = {
    **{creature: "egyptian" for creature in ["anubis_warrior", "scarab_guardian", "mummy_soldier", "sphinx_sentinel", "sun_falcon"]},
    **{creature: "nordic" for creature in ["frost_giant", "draugr_warrior", "valkyrie", "fenrir_wolf", "dwarf_berserker"]},
    **{creature: "chinese" for creature in ["azure_dragon", "qilin_guardian", "jiangshi", "nine_tailed_fox", "stone_lion_guardian"]},
    **{creature: "atlantean" for creature in ["crystal_leviathan", "orichalcum_golem", "siren_priestess", "tidal_guardian", "abyssal_ray"]},
}


def constant_name(creature_id: str, event: str) -> str:
    return f"ENTITY_{creature_id.upper()}_{event.upper()}"


def sound_key(creature_id: str, event: str) -> str:
    return f"entity.{creature_id}.{event}"


def synthesize_wave(creature_id: str, event: str, path: Path) -> None:
    sample_rate = 44100
    duration = EVENT_DURATIONS[event]
    total_samples = int(sample_rate * duration)
    rng = random.Random(f"{creature_id}:{event}:mythic_age_1_0")
    civilization = CREATURE_CIVILIZATION[creature_id]
    stable_id = int(hashlib.sha256(creature_id.encode("utf-8")).hexdigest()[:8], 16)
    base = CIVILIZATION_BASE[civilization] + (stable_id % 54)
    event_shift = {
        "ambient": 0.82,
        "hurt": 1.55,
        "death": 0.62,
        "attack": 1.25,
        "special": 1.88,
    }[event]
    freq = base * event_shift
    secondary = freq * (1.48 + rng.random() * 0.18)
    tremor = 3.0 + rng.random() * 4.0
    gain = EVENT_GAIN[event]

    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        frames = bytearray()
        for index in range(total_samples):
            t = index / sample_rate
            progress = index / max(1, total_samples - 1)
            attack = min(1.0, progress / 0.08)
            release = min(1.0, (1.0 - progress) / 0.16)
            envelope = max(0.0, min(attack, release))
            sweep = 1.0 + (0.18 if event in {"attack", "special"} else -0.08) * progress
            tone = math.sin(2.0 * math.pi * freq * sweep * t)
            tone += 0.42 * math.sin(2.0 * math.pi * secondary * t + math.sin(t * tremor) * 0.8)
            tone += 0.18 * math.sin(2.0 * math.pi * (freq * 0.5) * t)
            noise = (rng.random() * 2.0 - 1.0) * (0.16 if event in {"hurt", "death", "attack"} else 0.08)
            sample = (tone * 0.62 + noise) * envelope * gain
            value = int(max(-1.0, min(1.0, sample)) * 32767)
            frames.extend(struct.pack("<hh", value, value))
        wav.writeframes(frames)


def encode_ogg(wav_path: Path, ogg_path: Path) -> None:
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        raise RuntimeError("ffmpeg is required to encode OGG audio")
    ogg_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            ffmpeg,
            "-y",
            "-hide_banner",
            "-loglevel",
            "error",
            "-i",
            str(wav_path),
            "-c:a",
            "vorbis",
            "-strict",
            "-2",
            "-q:a",
            "3",
            str(ogg_path),
        ],
        check=True,
    )


def generate_audio_files() -> None:
    with tempfile.TemporaryDirectory() as temp_dir:
        temp = Path(temp_dir)
        for creature_id in CREATURES:
            for event in EVENTS:
                wav_path = temp / f"{creature_id}_{event}.wav"
                ogg_path = SOUND_DIR / creature_id / f"{event}.ogg"
                synthesize_wave(creature_id, event, wav_path)
                encode_ogg(wav_path, ogg_path)


def update_sounds_json() -> None:
    payload = json.loads(SOUNDS_JSON.read_text(encoding="utf-8"))
    for creature_id in CREATURES:
        for event in EVENTS:
            key = sound_key(creature_id, event)
            payload[key] = {
                "subtitle": f"subtitles.mythic_age.entity.{creature_id}.{event}",
                "sounds": [f"mythic_age:entity/{creature_id}/{event}"],
            }
    SOUNDS_JSON.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def update_mod_sounds() -> None:
    text = MOD_SOUNDS.read_text(encoding="utf-8")
    lines: list[str] = ["    // Non-Greek Myth Creature Sounds"]
    for creature_id in CREATURES:
        label = creature_id.upper()
        lines.append(f"    // {creature_id}")
        for event in EVENTS:
            lines.append(f'    public static final SoundEvent {constant_name(creature_id, event)} = registerSoundEvent("{sound_key(creature_id, event)}");')
        lines.append("")
    block = "\n".join(lines).rstrip() + "\n\n"

    if "    // Non-Greek Myth Creature Sounds" in text:
        text = re.sub(
            r"    // Non-Greek Myth Creature Sounds\n.*?\n\n    // Meteor Sounds",
            block + "    // Meteor Sounds",
            text,
            flags=re.S,
        )
    else:
        text = text.replace("    // Meteor Sounds", block + "    // Meteor Sounds")
    MOD_SOUNDS.write_text(text, encoding="utf-8")


def replace_method_return(text: str, method_name: str, replacement: str) -> tuple[str, bool]:
    pattern = re.compile(
        rf"(@Override\s+protected SoundEvent {method_name}\([^)]*\) \{{\s*)return [^;]+;(\s*\}})",
        re.S,
    )
    new_text, count = pattern.subn(rf"\1return {replacement};\2", text, count=1)
    return new_text, count > 0


def insert_ambient_method(text: str, replacement: str) -> str:
    method = (
        "    @Override\n"
        "    protected SoundEvent getAmbientSound() {\n"
        f"        return {replacement};\n"
        "    }\n\n"
    )
    return text.replace("    @Override\n    protected SoundEvent getHurtSound", method + "    @Override\n    protected SoundEvent getHurtSound", 1)


def update_entity_sources() -> None:
    for creature_id, class_name in CREATURES.items():
        path = ENTITY_DIR / f"{class_name}.java"
        text = path.read_text(encoding="utf-8")
        if "import com.example.mythic_age.sound.ModSounds;" not in text:
            text = text.replace(f"package com.example.mythic_age.entity;\n\n", f"package com.example.mythic_age.entity;\n\nimport com.example.mythic_age.sound.ModSounds;\n", 1)

        for event, method in [
            ("ambient", "getAmbientSound"),
            ("hurt", "getHurtSound"),
            ("death", "getDeathSound"),
            ("attack", "getAttackSound"),
        ]:
            text, replaced = replace_method_return(text, method, f"ModSounds.{constant_name(creature_id, event)}")
            if event == "ambient" and not replaced:
                text = insert_ambient_method(text, f"ModSounds.{constant_name(creature_id, event)}")

        text = re.sub(
            r"this\.playSpecialSound\([^;]+;",
            f"this.playSpecialSound(ModSounds.{constant_name(creature_id, 'special')});",
            text,
            count=1,
        )
        path.write_text(text, encoding="utf-8")


def main() -> int:
    generate_audio_files()
    update_sounds_json()
    update_mod_sounds()
    update_entity_sources()
    print(f"generated first-pass audio for {len(CREATURES)} creatures")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
