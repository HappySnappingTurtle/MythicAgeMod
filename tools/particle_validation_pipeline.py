#!/usr/bin/env python3
"""Static particle resource and registration gate for Mythic Age."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/particle_validation"
REPORT_PATH = REPORT_DIR / "particle_report.json"
MOD_PARTICLES = ROOT / "src/main/java/com/example/mythic_age/particle/ModParticles.java"
CLIENT_PARTICLES = ROOT / "src/main/java/com/example/mythic_age/client/particle/ModClientParticles.java"
MOD_INIT = ROOT / "src/main/java/com/example/mythic_age/MythicAgeMod.java"
CLIENT_INIT = ROOT / "src/main/java/com/example/mythic_age/MythicAgeModClient.java"
PARTICLE_RESOURCE_DIR = ROOT / "src/main/resources/assets/mythic_age/particles"
TEXTURE_DIR = ROOT / "src/main/resources/assets/mythic_age/textures/particle"

PARTICLES: list[dict[str, Any]] = [
    {
        "id": "mythic_flame",
        "constant": "MYTHIC_FLAME",
        "factory": "MythicFlameParticle.Factory::new",
        "class": ROOT / "src/main/java/com/example/mythic_age/client/particle/MythicFlameParticle.java",
        "users": {
            "src/main/java/com/example/mythic_age/entity/ChimeraEntity.java": "ModParticles.MYTHIC_FLAME",
        },
    },
    {
        "id": "divine_sparkle",
        "constant": "DIVINE_SPARKLE",
        "factory": "DivineSparkleParticle.Factory::new",
        "class": ROOT / "src/main/java/com/example/mythic_age/client/particle/DivineSparkleParticle.java",
        "users": {
            "src/main/java/com/example/mythic_age/entity/PegasusEntity.java": "ModParticles.DIVINE_SPARKLE",
        },
    },
    {
        "id": "shockwave",
        "constant": "SHOCKWAVE",
        "factory": "ShockwaveParticle.Factory::new",
        "class": ROOT / "src/main/java/com/example/mythic_age/client/particle/ShockwaveParticle.java",
        "users": {
            "src/main/java/com/example/mythic_age/entity/ColossusEntity.java": "ModParticles.SHOCKWAVE",
        },
    },
]


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def texture_path(texture_id: str) -> Path:
    namespace, _, name = texture_id.partition(":")
    if namespace != "mythic_age" or not name:
        return ROOT / "__unsupported_particle_texture__"
    return TEXTURE_DIR / f"{name}.png"


def audit() -> tuple[int, dict[str, Any]]:
    failures: list[str] = []
    mod_particles_text = read_text(MOD_PARTICLES)
    client_particles_text = read_text(CLIENT_PARTICLES)
    mod_init_text = read_text(MOD_INIT)
    client_init_text = read_text(CLIENT_INIT)

    required_chain = [
        (MOD_PARTICLES, "FabricParticleTypes.simple()", mod_particles_text),
        (MOD_PARTICLES, "Registry.register(", mod_particles_text),
        (MOD_PARTICLES, "Registries.PARTICLE_TYPE", mod_particles_text),
        (MOD_PARTICLES, "registerModParticles()", mod_particles_text),
        (MOD_INIT, "ModParticles.registerModParticles();", mod_init_text),
        (CLIENT_PARTICLES, "ParticleFactoryRegistry.getInstance()", client_particles_text),
        (CLIENT_INIT, "ModClientParticles.registerClientParticles();", client_init_text),
    ]
    chain_results: list[dict[str, Any]] = []
    for path, token, text in required_chain:
        ok = path.exists() and token in text
        if not ok:
            failures.append(f"{path.relative_to(ROOT)} missing token {token}")
        chain_results.append({
            "path": str(path.relative_to(ROOT)),
            "token": token,
            "ok": ok,
        })

    particle_results: list[dict[str, Any]] = []
    for particle in PARTICLES:
        particle_id = particle["id"]
        constant = particle["constant"]
        factory = particle["factory"]
        json_path = PARTICLE_RESOURCE_DIR / f"{particle_id}.json"
        class_path = particle["class"]

        missing_tokens: list[str] = []
        if constant not in mod_particles_text:
            missing_tokens.append(constant)
        if f'register("{particle_id}")' not in mod_particles_text:
            missing_tokens.append(f'register("{particle_id}")')
        if factory not in client_particles_text:
            missing_tokens.append(factory)

        class_text = read_text(class_path)
        for token in ["extends SpriteBillboardParticle", "setSpriteForAge", "PARTICLE_SHEET_TRANSLUCENT"]:
            if token not in class_text:
                missing_tokens.append(f"{class_path.name}:{token}")

        resource_ok = False
        texture_results: list[dict[str, Any]] = []
        if not json_path.exists():
            failures.append(f"missing particle resource {json_path.relative_to(ROOT)}")
        else:
            try:
                payload = json.loads(json_path.read_text(encoding="utf-8"))
                textures = payload.get("textures", [])
                resource_ok = isinstance(textures, list) and len(textures) > 0
                if not resource_ok:
                    failures.append(f"{json_path.relative_to(ROOT)} has no textures list")
                for texture in textures:
                    resolved = texture_path(texture)
                    exists = resolved.exists()
                    if not exists:
                        failures.append(f"{json_path.relative_to(ROOT)} references missing texture {texture}")
                    texture_results.append({
                        "id": texture,
                        "path": str(resolved.relative_to(ROOT)) if resolved.is_relative_to(ROOT) else str(resolved),
                        "exists": exists,
                    })
            except json.JSONDecodeError as exc:
                failures.append(f"{json_path.relative_to(ROOT)} invalid json: {exc}")

        user_results: list[dict[str, Any]] = []
        for relative_path, token in particle["users"].items():
            path = ROOT / relative_path
            text = read_text(path)
            ok = path.exists() and token in text
            if not ok:
                failures.append(f"{relative_path} missing custom particle use {token}")
            user_results.append({
                "path": relative_path,
                "token": token,
                "ok": ok,
            })

        if missing_tokens:
            failures.append(f"{particle_id} missing registration/class tokens " + "|".join(missing_tokens))

        particle_results.append({
            "id": particle_id,
            "constant": constant,
            "resource": str(json_path.relative_to(ROOT)),
            "resource_ok": resource_ok,
            "factory": factory,
            "class": str(class_path.relative_to(ROOT)),
            "missing_tokens": missing_tokens,
            "textures": texture_results,
            "users": user_results,
            "ok": not missing_tokens and resource_ok and all(item["ok"] for item in user_results)
                    and all(item["exists"] for item in texture_results),
        })

    payload = {
        "version": 1,
        "scope": "custom_particle_registration_resources_and_usage",
        "ok": not failures,
        "failures": failures,
        "chain": chain_results,
        "particles": particle_results,
    }
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return (0 if not failures else 1), payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        code, payload = audit()
        if code == 0:
            print("Particle Validation Gate: PASS")
            print(f"report: {REPORT_PATH}")
        else:
            print("Particle Validation Gate: FAIL")
            for failure in payload["failures"]:
                print(f"- {failure}")
        return code
    return 0


if __name__ == "__main__":
    sys.exit(main())
