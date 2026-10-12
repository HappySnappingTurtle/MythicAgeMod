#!/usr/bin/env python3
"""Generate original Pegasus texture and simple OGG sound placeholders."""

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

EVENTS = {
    "ambient": (0.58, 0.24, 216.0),
    "hurt": (0.34, 0.30, 176.0),
    "death": (0.82, 0.34, 142.0),
    "step": (0.18, 0.28, 116.0),
    "fly": (0.46, 0.22, 252.0),
    "special": (0.62, 0.30, 264.0),
}


def shade(color: tuple[int, int, int], amount: int) -> tuple[int, int, int]:
    return tuple(max(0, min(255, channel + amount)) for channel in color)


def draw_pegasus_texture() -> None:
    path = ASSET_ROOT / "textures/entity/pegasus/pegasus.png"
    path.parent.mkdir(parents=True, exist_ok=True)

    white = (247, 247, 240)
    shadow = (188, 196, 202)
    pale_blue = (230, 235, 238)
    mane = (255, 255, 252)

    image = Image.new("RGBA", (128, 128), (238, 242, 244, 255))
    draw = ImageDraw.Draw(image)
    rng = random.Random("pegasus")

    for y in range(0, 128, 8):
        for x in range(0, 128, 8):
            jitter = rng.randint(-8, 10)
            draw.rectangle((x, y, x + 7, y + 7), fill=shade(white, jitter) + (255,))

    # UV-style panels: white body, head, wings and mane with only pale shadows.
    draw.rounded_rectangle((8, 32, 66, 84), radius=5, fill=white + (255,), outline=shadow + (255,), width=2)
    draw.rounded_rectangle((18, 8, 52, 30), radius=3, fill=shade(white, 5) + (255,), outline=shadow + (255,), width=2)
    draw.rectangle((58, 12, 76, 44), fill=mane + (255,), outline=shadow + (255,), width=2)
    draw.polygon([(72, 24), (124, 8), (104, 40), (78, 42)], fill=shade(white, 2) + (255,), outline=shadow + (255,))
    draw.polygon([(70, 54), (124, 62), (112, 88), (78, 82)], fill=shade(white, -2) + (255,), outline=shadow + (255,))

    for x in range(82, 122, 8):
        draw.line((x, 18, x - 6, 40), fill=pale_blue + (255,), width=1)
        draw.line((x, 64, x - 8, 84), fill=pale_blue + (255,), width=1)

    draw.rectangle((82, 96, 114, 112), fill=white + (255,), outline=shadow + (255,), width=2)
    draw.rectangle((18, 94, 66, 118), fill=shade(white, -3) + (255,), outline=shadow + (255,), width=2)
    draw.line((22, 100, 64, 114), fill=pale_blue + (255,), width=2)
    draw.line((24, 106, 62, 100), fill=shade(white, 8) + (255,), width=2)

    draw.ellipse((33, 15, 39, 21), fill=(60, 74, 92, 255))
    draw.rectangle((37, 7, 40, 14), fill=white + (255,), outline=shadow + (255,))
    draw.rectangle((45, 6, 48, 14), fill=white + (255,), outline=shadow + (255,))

    for _ in range(90):
        x = rng.randrange(128)
        y = rng.randrange(128)
        amount = rng.randint(-10, 8)
        image.putpixel((x, y), shade(white, amount) + (255,))

    image.save(path)


def sample_wave(freq: float, duration: float, volume: float, event: str) -> list[int]:
    sample_rate = 44_100
    total = int(sample_rate * duration)
    rng = random.Random(f"pegasus:{event}")
    data: list[int] = []
    for i in range(total):
        t = i / sample_rate
        envelope = min(1.0, i / (sample_rate * 0.035)) * max(0.0, 1.0 - i / total)
        breath = math.sin(2.0 * math.pi * (freq * 0.45) * t) * 0.22
        tone = math.sin(2.0 * math.pi * (freq + breath * 16.0) * t)
        overtone = math.sin(2.0 * math.pi * (freq * 1.52) * t) * 0.26
        air = (rng.random() * 2.0 - 1.0) * (0.08 if event != "fly" else 0.18)
        if event == "step":
            tone = math.sin(2.0 * math.pi * (freq * 0.5) * t)
            overtone *= 0.12
        elif event == "death":
            tone *= max(0.15, 1.0 - t / duration)
            overtone *= max(0.2, 1.0 - t / duration)
        elif event == "special":
            overtone += math.sin(2.0 * math.pi * (freq * 2.25) * t) * 0.18

        value = int(max(-1.0, min(1.0, (tone + overtone + air) * envelope * volume)) * 32767)
        data.append(value)
    return data


def write_ogg(event: str, duration: float, volume: float, freq: float) -> None:
    out_path = ASSET_ROOT / f"sounds/entity/pegasus/{event}.ogg"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    samples = sample_wave(freq, duration, volume, event)
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = Path(tmp) / f"pegasus_{event}.wav"
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
    draw_pegasus_texture()
    for event, (duration, volume, freq) in EVENTS.items():
        write_ogg(event, duration, volume, freq)


if __name__ == "__main__":
    os.chdir(ROOT)
    main()
