#!/usr/bin/env python3
"""Generate original placeholder-quality assets for Greek myth batch 2."""

from __future__ import annotations

import math
import os
import random
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "src/main/resources/assets/mythic_age"


CREATURES = {
    "nemean_lion": {
        "base": (196, 138, 45),
        "dark": (101, 70, 29),
        "accent": (244, 209, 112),
        "sound_freq": 88,
    },
    "manticore": {
        "base": (130, 54, 51),
        "dark": (51, 36, 42),
        "accent": (64, 129, 74),
        "sound_freq": 116,
    },
    "chimera": {
        "base": (88, 50, 36),
        "dark": (42, 29, 25),
        "accent": (214, 89, 49),
        "sound_freq": 102,
    },
    "colossus": {
        "base": (126, 91, 58),
        "dark": (54, 43, 39),
        "accent": (45, 186, 157),
        "sound_freq": 66,
    },
}

EVENTS = {
    "ambient": (0.62, 0.30),
    "hurt": (0.34, 0.36),
    "death": (0.86, 0.42),
    "step": (0.24, 0.45),
    "attack": (0.38, 0.42),
    "special": (0.58, 0.48),
}


def shade(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(max(0, min(255, channel + amount)) for channel in color)


def draw_texture(name: str, palette: dict[str, tuple[int, int, int]]) -> None:
    path = ASSET_ROOT / f"textures/entity/{name}/{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image = Image.new("RGBA", (128, 128), shade(palette["base"], -28) + (255,))
    draw = ImageDraw.Draw(image)

    rng = random.Random(name)
    for y in range(0, 128, 8):
        for x in range(0, 128, 8):
            jitter = rng.randint(-18, 18)
            draw.rectangle((x, y, x + 7, y + 7), fill=shade(palette["base"], jitter) + (255,))

    # Body paint panels matching the simple cuboid model UV style.
    draw.rounded_rectangle((8, 28, 74, 88), radius=4, fill=palette["base"] + (255,), outline=palette["dark"] + (255,), width=3)
    draw.rounded_rectangle((18, 8, 58, 34), radius=3, fill=shade(palette["base"], 18) + (255,), outline=palette["dark"] + (255,), width=2)
    draw.rectangle((82, 14, 118, 42), fill=shade(palette["dark"], 18) + (255,), outline=palette["accent"] + (255,), width=2)
    draw.rectangle((78, 56, 120, 88), fill=shade(palette["base"], -10) + (255,), outline=palette["dark"] + (255,), width=2)

    if name == "nemean_lion":
        for i in range(12):
            x = 18 + i * 3
            draw.polygon([(x, 14), (x + 8, 25), (x - 2, 28)], fill=palette["accent"] + (255,))
        draw.ellipse((30, 16, 46, 30), fill=palette["dark"] + (255,))
        draw.ellipse((35, 20, 40, 25), fill=palette["accent"] + (255,))
    elif name == "manticore":
        draw.polygon([(88, 12), (118, 4), (108, 36)], fill=shade(palette["accent"], -20) + (255,))
        draw.polygon([(96, 82), (120, 72), (112, 98)], fill=palette["accent"] + (255,))
        draw.line((18, 64, 70, 38), fill=palette["dark"] + (255,), width=3)
    elif name == "chimera":
        draw.ellipse((18, 10, 38, 30), fill=palette["accent"] + (255,), outline=palette["dark"] + (255,), width=2)
        draw.ellipse((40, 10, 58, 28), fill=shade(palette["base"], 28) + (255,), outline=palette["dark"] + (255,), width=2)
        draw.ellipse((58, 12, 74, 28), fill=shade(palette["accent"], -30) + (255,), outline=palette["dark"] + (255,), width=2)
        draw.polygon([(82, 10), (122, 20), (88, 44)], fill=shade(palette["accent"], -5) + (255,))
    elif name == "colossus":
        for x in range(12, 72, 12):
            draw.line((x, 30, x + 16, 86), fill=palette["accent"] + (255,), width=2)
        draw.rectangle((88, 12, 116, 38), fill=shade(palette["base"], 24) + (255,), outline=palette["accent"] + (255,), width=3)
        draw.rectangle((22, 92, 106, 112), fill=palette["dark"] + (255,), outline=palette["accent"] + (255,), width=2)

    for _ in range(110):
        x = rng.randrange(128)
        y = rng.randrange(128)
        image.putpixel((x, y), shade(palette["accent"], rng.randint(-30, 20)) + (255,))

    image.save(path)


def sample_wave(freq: float, duration: float, volume: float, event: str, creature_seed: int) -> list[int]:
    sample_rate = 44_100
    total = int(sample_rate * duration)
    rng = random.Random(f"{event}:{creature_seed}")
    data: list[int] = []
    for i in range(total):
        t = i / sample_rate
        envelope = min(1.0, i / (sample_rate * 0.04)) * max(0.0, 1.0 - i / total)
        wobble = math.sin(2.0 * math.pi * (freq * 0.13) * t) * 0.45
        tone = math.sin(2.0 * math.pi * (freq + wobble * freq) * t)
        overtone = math.sin(2.0 * math.pi * (freq * 1.9) * t) * 0.35
        grit = (rng.random() * 2.0 - 1.0) * 0.18
        if event == "step":
            tone = math.sin(2.0 * math.pi * (freq * 0.55) * t)
            grit *= 0.35
        elif event == "special":
            overtone += math.sin(2.0 * math.pi * (freq * 2.8) * t) * 0.22
        elif event == "death":
            tone *= max(0.2, 1.0 - t / duration)
        value = int(max(-1.0, min(1.0, (tone + overtone + grit) * envelope * volume)) * 32767)
        data.append(value)
    return data


def write_ogg(name: str, event: str, freq: float, duration: float, volume: float) -> None:
    out_path = ASSET_ROOT / f"sounds/entity/{name}/{event}.ogg"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    creature_seed = sum(ord(ch) for ch in name)
    samples = sample_wave(freq, duration, volume, event, creature_seed)
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / f"{name}_{event}.wav"
        with wave.open(str(wav_path), "wb") as wav:
            wav.setnchannels(2)
            wav.setsampwidth(2)
            wav.setframerate(44_100)
            frames = bytearray()
            for sample in samples:
                frames.extend(struct.pack("<hh", sample, sample))
            wav.writeframes(frames)
        subprocess.run(
            [
                "ffmpeg",
                "-y",
                "-hide_banner",
                "-loglevel",
                "error",
                "-i",
                str(wav_path),
                "-ac",
                "2",
                "-c:a",
                "vorbis",
                "-strict",
                "-2",
                str(out_path),
            ],
            check=True,
        )


def main() -> None:
    for name, palette in CREATURES.items():
        draw_texture(name, palette)
        for index, (event, (duration, volume)) in enumerate(EVENTS.items()):
            freq = palette["sound_freq"] * (1.0 + index * 0.06)
            write_ogg(name, event, freq, duration, volume)


if __name__ == "__main__":
    os.chdir(ROOT)
    main()
