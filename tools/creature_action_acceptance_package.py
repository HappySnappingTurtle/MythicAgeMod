#!/usr/bin/env python3
"""Aggregate 1.0 creature action and animation acceptance evidence."""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
CIVILIZATIONS = ["greek", "egyptian", "nordic", "chinese", "atlantean"]
REPORT_DIR = ROOT / "docs/preview_assets/creature_action_acceptance"
REPORT_PATH = REPORT_DIR / "creature_action_acceptance_package.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/entity_validation/creature_action_acceptance_package.md"
STORYBOARD_DIR = REPORT_DIR / "motion_storyboards"
STORYBOARD_CONTACT_SHEET = REPORT_DIR / "motion_storyboard_contact_sheet.png"
REVIEW_BOARD_DIR = REPORT_DIR / "motion_review_boards"
REVIEW_BOARD_CONTACT_SHEET = REPORT_DIR / "motion_review_board_contact_sheet.png"
MOTION_SEQUENCE_MANIFEST = REPORT_DIR / "minecraft_sequences/motion_sequence_manifest.json"
ACTION_PARTICLE_REPORT = ROOT / "docs/preview_assets/entity_validation/action_particle_report.json"
ENTITY_DIR = ROOT / "src/main/java/com/example/mythic_age/entity"
MODEL_DIR = ROOT / "src/main/java/com/example/mythic_age/client/render/entity/model"
RESAMPLE_NEAREST = getattr(getattr(Image, "Resampling", Image), "NEAREST")
RESAMPLE_LANCZOS = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
FONT_PATHS = (
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
)

CIVILIZATION_COLORS = {
    "greek": (93, 116, 176, 255),
    "egyptian": (181, 132, 54, 255),
    "nordic": (82, 132, 139, 255),
    "chinese": (173, 78, 64, 255),
    "atlantean": (93, 145, 128, 255),
}
COLORS = {
    "bg": (239, 235, 223, 255),
    "panel": (248, 245, 237, 255),
    "panel_shadow": (220, 213, 196, 255),
    "ink": (35, 31, 26, 255),
    "muted": (96, 88, 75, 255),
    "ok": (63, 126, 82, 255),
    "pending": (176, 112, 45, 255),
    "fail": (158, 64, 58, 255),
}


CREATURES = {
    "greek": [
        "minotaur", "cyclops", "centaur", "hydra", "medusa",
        "nemean_lion", "manticore", "chimera", "colossus", "pegasus",
    ],
    "egyptian": ["anubis_warrior", "scarab_guardian", "mummy_soldier", "sphinx_sentinel", "sun_falcon"],
    "nordic": ["frost_giant", "draugr_warrior", "valkyrie", "fenrir_wolf", "dwarf_berserker"],
    "chinese": ["azure_dragon", "qilin_guardian", "jiangshi", "nine_tailed_fox", "stone_lion_guardian"],
    "atlantean": ["crystal_leviathan", "orichalcum_golem", "siren_priestess", "tidal_guardian", "abyssal_ray"],
}

CLASS_NAMES = {
    "nemean_lion": "NemeanLion",
    "nine_tailed_fox": "NineTailedFox",
    "stone_lion_guardian": "StoneLionGuardian",
    "anubis_warrior": "AnubisWarrior",
    "scarab_guardian": "ScarabGuardian",
    "mummy_soldier": "MummySoldier",
    "sphinx_sentinel": "SphinxSentinel",
    "sun_falcon": "SunFalcon",
    "frost_giant": "FrostGiant",
    "draugr_warrior": "DraugrWarrior",
    "dwarf_berserker": "DwarfBerserker",
    "azure_dragon": "AzureDragon",
    "qilin_guardian": "QilinGuardian",
    "crystal_leviathan": "CrystalLeviathan",
    "orichalcum_golem": "OrichalcumGolem",
    "siren_priestess": "SirenPriestess",
    "tidal_guardian": "TidalGuardian",
    "abyssal_ray": "AbyssalRay",
}

FLYING_OR_GLIDING = {
    "pegasus", "sun_falcon", "valkyrie", "azure_dragon",
    "crystal_leviathan", "siren_priestess", "abyssal_ray",
}

SWIMMING_OR_DRIFTING = {"crystal_leviathan", "siren_priestess", "tidal_guardian", "abyssal_ray"}

REQUIREMENT_LABELS = {
    "idle": "Idle / 待机",
    "locomotion": "Move / 移动",
    "attack_or_special": "Attack / 特殊",
    "hurt": "Hurt / 受击",
    "death": "Death / 死亡",
    "flight_or_glide": "Flight / 滑翔",
    "swim_or_drift": "Swim / 漂游",
    "riding_fly": "Ridden Flight / 骑乘飞行",
    "signature_multi_part_motion": "Signature Parts / 特征联动",
}

EVIDENCE_LABELS = {
    "idle": "setAngles + animationProgress",
    "locomotion": "limbAngle + limbDistance",
    "attack_or_special": "handSwingProgress + action contract + particles",
    "hurt": "hurtTime branch",
    "death": "deathTime branch",
    "flight_or_glide": "flight/drift state + entity logic",
    "swim_or_drift": "multi-part motion + drift/particle support",
    "riding_fly": "rider travel + flying model state",
    "signature_multi_part_motion": "named moving parts in model",
}

MOTION_MODE_ACCEPTANCE_CRITERIA = {
    "idle": [
        "截图或连续帧必须无菜单/GUI遮挡。",
        "待机呼吸、尾、翼、鬃毛、蛇发或环境部件有可读微动。",
        "头脸、眼睛、嘴和核心神话特征保持清楚。",
    ],
    "locomotion": [
        "侧视连续帧能读出完整步态/滑游周期。",
        "脚、蹄、爪、鳍、尾或身体波形参与动作，不能只整体平移。",
        "头部比例稳定，运动中不遮挡眼嘴或武器。",
    ],
    "attack_or_special": [
        "捕获蓄势、峰值和恢复帧，关键部件必须参与攻击。",
        "粒子和音效触发不能遮挡眼睛、嘴、武器、翼根、尾刺或多头。",
        "特殊动作必须符合设计包的单位身份。",
    ],
    "hurt": [
        "受击反应要能看出 recoil，且不破坏模型结构。",
        "头颈、翅膀、尾巴、多头连接在反应帧中保持稳定。",
    ],
    "death": [
        "死亡过程和最终姿态必须可读，不能只瞬间消失。",
        "最终姿态不允许头颈、武器、翼、尾或多头出现断裂/悬空。",
    ],
    "flight_or_glide": [
        "单位必须处于离地飞行/滑翔状态。",
        "翼、身体、尾或鳍有明确循环动作。",
        "飞行中头脸仍然可读，身体高度和朝向稳定。",
    ],
    "riding_fly": [
        "天马必须由玩家骑乘并处于可控飞行状态。",
        "玩家坐点稳定，不遮挡白马头颈、翼根和眼嘴细节。",
        "骑乘飞行动作与自主飞行动作都要可区分。",
    ],
    "signature_multi_part_motion": [
        "多头、多尾、蛇发、蝎尾、龙身或翼等标志部件必须错相运动。",
        "九头蛇每条颈和头必须通过 body_overlap/head_overlap 保持连接。",
        "动作不能退化成整个模型一起摆动。",
    ],
    "swim_or_drift": [
        "单位必须在水中或漂浮状态下捕获。",
        "身体、鳍、尾或裙摆体现滑游/漂移动作，而不是地面走路。",
        "漂浮高度、头脸和核心轮廓在水面/粒子中仍然可读。",
    ],
}

CAPTURE_BATCH_SIZE_BY_PRIORITY = {
    1: 12,
    2: 10,
    3: 8,
    4: 4,
    5: 6,
    6: 6,
    7: 12,
    8: 12,
    9: 16,
}

SIGNATURE_MOTION_CONTRACTS = {
    "pegasus": {
        "reason": "Pegasus must support both autonomous flight and player-ridden flight while keeping horse head/neck, wings, and rider space stable.",
        "checks": [
            {
                "name": "wing_flap_uses_flying_state",
                "source": "model",
                "tokens": [
                    "entity.isFlying() ? 0.62F : 0.22F",
                    "entity.isFlying() ? 0.7F : 0.18F",
                    "this.leftWing.roll",
                    "this.rightWing.roll",
                    "this.leftWingFeathers.pitch",
                    "this.rightWingFeathers.roll",
                ],
            },
            {
                "name": "rider_space_and_passenger_stability",
                "source": "model",
                "tokens": [
                    "this.saddleSpace.visible = true",
                    "this.body.pitch = entity.hasPassengers() ? -0.06F : 0.0F",
                ],
            },
            {
                "name": "rider_can_mount_and_control_flight",
                "source": "entity",
                "tokens": [
                    "travelWithRider((PlayerEntity) rider)",
                    "player.startRiding(this)",
                    "canAddPassenger",
                    "return this.hasPassengers() || !this.isOnGround()",
                ],
            },
            {
                "name": "autonomous_flight_logic_present",
                "source": "entity",
                "tokens": [
                    "tickAutonomousFlight()",
                    "setNoGravity(true)",
                ],
            },
        ],
    },
    "hydra": {
        "reason": "Hydra V3 must animate nine connected necks/heads and tail with staggered motion after design approval; the old five-head V2 motion contract is not a 1.0 acceptance basis.",
        "checks": [
            {
                "name": "neck_head_arrays_present",
                "source": "model",
                "tokens": [
                    "private static final String[] NECK_NAMES",
                    "private static final String[] HEAD_NAMES",
                    "HEAD_PHASES",
                    "BASE_YAWS",
                    "BASE_PITCHES",
                ],
            },
            {
                "name": "necks_and_heads_receive_motion",
                "source": "model",
                "tokens": [
                    "this.necks[i].yaw",
                    "this.heads[i].pitch",
                    "this.heads[i].yaw",
                    "HEAD_PHASES[i]",
                ],
            },
            {
                "name": "tail_attack_hurt_death_motion_present",
                "source": "model",
                "tokens": [
                    "this.tail.yaw",
                    "handSwingProgress",
                    "hurtTime",
                    "deathTime",
                ],
            },
        ],
    },
    "crystal_leviathan": {
        "reason": "Crystal Leviathan must glide as segmented body waves with fins and tail fan, not use a normal walk cycle.",
        "checks": [
            {
                "name": "segmented_body_wave_motion",
                "source": "model",
                "tokens": [
                    "entity.isGliding()",
                    "this.body1.yaw",
                    "this.body2.yaw",
                    "this.body3.yaw",
                    "this.body4.yaw",
                ],
            },
            {
                "name": "fins_and_tail_fan_motion",
                "source": "model",
                "tokens": [
                    "this.tail.yaw",
                    "this.leftSideFin.roll",
                    "this.rightSideFin.roll",
                    "this.tailFan.roll",
                ],
            },
            {
                "name": "entity_tidal_glide_logic",
                "source": "entity",
                "tokens": [
                    "tickTidalGlide()",
                    "setNoGravity(true)",
                    "isGliding()",
                ],
            },
        ],
    },
    "siren_priestess": {
        "reason": "Siren Priestess must drift with fish tail and staff casting motion.",
        "checks": [
            {
                "name": "drifting_tail_motion",
                "source": "model",
                "tokens": [
                    "entity.isDrifting()",
                    "this.tailBase.pitch",
                    "this.tailTip.pitch",
                    "this.tailFin.roll",
                ],
            },
            {
                "name": "staff_cast_motion",
                "source": "model",
                "tokens": [
                    "this.staff.pitch",
                    "handSwingProgress",
                ],
            },
            {
                "name": "entity_siren_drift_logic",
                "source": "entity",
                "tokens": [
                    "tickSirenDrift()",
                    "setNoGravity(true)",
                    "isDrifting()",
                ],
            },
        ],
    },
    "abyssal_ray": {
        "reason": "Abyssal Ray must glide with wing pulses, tail spine motion, and abyssal drift logic.",
        "checks": [
            {
                "name": "gliding_wing_motion",
                "source": "model",
                "tokens": [
                    "entity.isGliding()",
                    "this.leftWing.roll",
                    "this.rightWing.roll",
                    "this.leftWingTip.roll",
                    "this.rightWingTip.roll",
                ],
            },
            {
                "name": "tail_and_glow_motion",
                "source": "model",
                "tokens": [
                    "this.tail.yaw",
                    "this.tailSpine.yaw",
                    "this.glowBelly.pitch",
                ],
            },
            {
                "name": "entity_abyssal_glide_logic",
                "source": "entity",
                "tokens": [
                    "tickAbyssalGlide()",
                    "setNoGravity(true)",
                    "isGliding()",
                ],
            },
        ],
    },
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
    for paragraph in text.splitlines() or [text]:
        current = ""
        for char in paragraph:
            candidate = current + char
            if font.getbbox(candidate)[2] <= max_width or not current:
                current = candidate
            else:
                lines.append(current)
                current = char
        if current:
            lines.append(current)
    return lines


def motion_storyboard_path(civilization: str, creature_id: str) -> Path:
    return STORYBOARD_DIR / civilization / f"{creature_id}_motion_storyboard.png"


def motion_review_board_path(civilization: str, creature_id: str) -> Path:
    return REVIEW_BOARD_DIR / civilization / f"{creature_id}_motion_review.png"


def blockbench_preview_path(civilization: str, creature_id: str, view: str) -> Path:
    return ROOT / f"docs/preview_assets/{civilization}_creatures/{creature_id}/blockbench/{view}.png"


def class_name(creature_id: str) -> str:
    if creature_id in CLASS_NAMES:
        return CLASS_NAMES[creature_id]
    return "".join(part.capitalize() for part in creature_id.split("_"))


def design_doc(civilization: str, creature_id: str) -> Path:
    return ROOT / f"docs/美术资源/{civilization}_creatures/design/{creature_id}.md"


def extract_animation_constraints(path: Path) -> str:
    if not path.exists():
        return ""
    text = path.read_text(encoding="utf-8")
    match = re.search(r"## 动画约束\n(?P<body>.*?)(?:\n## |\Z)", text, re.S)
    if not match:
        return ""
    lines = [
        line.strip().lstrip("-").strip()
        for line in match.group("body").splitlines()
        if line.strip()
    ]
    return " ".join(lines)


def action_contracts_by_id() -> dict[str, dict[str, Any]]:
    data = load_json(ACTION_PARTICLE_REPORT, {})
    return {
        item.get("id"): item
        for item in data.get("contracts", [])
        if isinstance(item, dict) and isinstance(item.get("id"), str)
    }


def motion_sequences_by_key() -> dict[tuple[str, str, str], dict[str, Any]]:
    data = load_json(MOTION_SEQUENCE_MANIFEST, {})
    return {
        (item.get("creature_id"), item.get("motion_mode"), item.get("view")): item
        for item in data.get("captures", [])
        if isinstance(item, dict)
        and isinstance(item.get("creature_id"), str)
        and isinstance(item.get("motion_mode"), str)
        and isinstance(item.get("view"), str)
    }


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def contract_source_paths(entity_path: Path, contract: dict[str, Any]) -> list[Path]:
    paths = [entity_path]
    for raw_path in contract.get("paths", []):
        if not isinstance(raw_path, str):
            continue
        path = ROOT / raw_path
        if path not in paths:
            paths.append(path)
    return paths


def read_sources(paths: list[Path]) -> str:
    return "\n".join(read_text(path) for path in paths)


def infer_motion_requirements(creature_id: str) -> list[str]:
    requirements = ["idle", "locomotion", "attack_or_special", "hurt", "death"]
    if creature_id in FLYING_OR_GLIDING:
        requirements.append("flight_or_glide")
    if creature_id in SWIMMING_OR_DRIFTING:
        requirements.append("swim_or_drift")
    if creature_id == "pegasus":
        requirements.append("riding_fly")
    if creature_id in {"hydra", "chimera", "manticore", "medusa", "azure_dragon", "nine_tailed_fox"}:
        requirements.append("signature_multi_part_motion")
    return requirements


def required_sequence_modes(creature_id: str, requirements: list[str]) -> list[str]:
    modes = ["idle", "locomotion", "attack_or_special", "hurt", "death"]
    if "flight_or_glide" in requirements:
        modes.append("flight_or_glide")
    if "swim_or_drift" in requirements:
        modes.append("swim_or_drift")
    if "riding_fly" in requirements:
        modes.append("riding_fly")
    if "signature_multi_part_motion" in requirements:
        modes.append("signature_multi_part_motion")
    return modes


def motion_mode_capture_priority(mode: str) -> int:
    order = {
        "locomotion": 1,
        "attack_or_special": 2,
        "flight_or_glide": 3,
        "riding_fly": 4,
        "swim_or_drift": 5,
        "signature_multi_part_motion": 6,
        "hurt": 7,
        "death": 8,
        "idle": 9,
    }
    return order.get(mode, 99)


def motion_mode_capture_view(mode: str) -> str:
    if mode == "locomotion":
        return "side"
    if mode in {"hurt", "death"}:
        return "front"
    return "three_quarter"


def motion_mode_minimum_frames(mode: str) -> int:
    if mode in {"flight_or_glide", "riding_fly", "swim_or_drift", "signature_multi_part_motion"}:
        return 10
    if mode in {"hurt", "death"}:
        return 6
    return 8


def motion_mode_frame_interval_ticks(mode: str) -> int:
    if mode in {"attack_or_special", "hurt", "death"}:
        return 3
    if mode in {"flight_or_glide", "riding_fly", "swim_or_drift", "signature_multi_part_motion"}:
        return 4
    return 5


def motion_mode_trigger_hint(creature_id: str, mode: str) -> str:
    hints = {
        "idle": "Spawn the entity in a quiet arena, clear GUI/menu overlays, and record the idle breathing or ambient part motion.",
        "locomotion": "Move or path the entity across the arena so limbAngle/limbDistance cycles are visible from the side.",
        "attack_or_special": "Trigger the creature attack or special action, including particles and handSwingProgress/special state evidence.",
        "hurt": "Apply controlled nonlethal damage and capture hurtTime reaction without killing the entity.",
        "death": "Apply controlled lethal damage and capture deathTime collapse through the final pose.",
        "flight_or_glide": "Force flying/gliding/drifting state and capture wing, body, tail, or fin motion while airborne.",
        "riding_fly": "Mount the Pegasus with a player, clear overlays, and capture controlled ridden flight with stable rider space.",
        "swim_or_drift": "Place the creature in water or drift state and capture body/fin/tail movement rather than ground walking.",
        "signature_multi_part_motion": "Trigger the signature moving parts named in the design contract: multi-heads, snake hair, tails, wings, stinger, or body segments.",
    }
    hint = hints.get(mode, "Capture this required motion mode with clear GUI-free frames.")
    if creature_id == "hydra" and mode == "signature_multi_part_motion":
        return hint + " Confirm each neck remains connected through body_overlap/head_overlap segments."
    if creature_id == "pegasus" and mode in {"flight_or_glide", "riding_fly"}:
        return hint + " Confirm the horse head stays proportionate and readable while wings flap."
    return hint


def motion_mode_acceptance_criteria(creature_id: str, mode: str) -> list[str]:
    criteria = list(MOTION_MODE_ACCEPTANCE_CRITERIA.get(mode, []))
    if creature_id == "pegasus" and mode in {"flight_or_glide", "riding_fly"}:
        criteria.append("天马头部必须保持接近原版马的细长比例，不允许为了可读性放大成玩具马头。")
    if creature_id == "hydra" and mode == "signature_multi_part_motion":
        criteria.append("九头蛇每个头和对应颈段必须在连续帧里保持连接，不能出现头颈脱节。")
    return criteria


def motion_mode_capture_plan(creature: dict[str, Any]) -> list[dict[str, Any]]:
    tasks = []
    for mode in creature.get("motion_sequence_missing_modes", []):
        tasks.append({
            "civilization": creature["civilization"],
            "creature_id": creature["id"],
            "entity_id": creature["entity_id"],
            "motion_mode": mode,
            "priority": motion_mode_capture_priority(mode),
            "required_view": motion_mode_capture_view(mode),
            "minimum_frames": motion_mode_minimum_frames(mode),
            "frame_interval_ticks": motion_mode_frame_interval_ticks(mode),
            "trigger_hint": motion_mode_trigger_hint(creature["id"], mode),
            "acceptance_criteria": motion_mode_acceptance_criteria(creature["id"], mode),
            "required_evidence_types": [
                "gui_free_frame_sequence",
                "motion_sequence_manifest_entry",
                "human_review_note",
            ],
        })
    return tasks


def build_motion_capture_queue(creatures: list[dict[str, Any]]) -> list[dict[str, Any]]:
    tasks = [
        task
        for creature in creatures
        for task in motion_mode_capture_plan(creature)
    ]
    return sorted(
        tasks,
        key=lambda task: (
            task["priority"],
            task["civilization"],
            task["creature_id"],
            task["motion_mode"],
        ),
    )


def build_motion_capture_plan_batches(tasks: list[dict[str, Any]]) -> list[dict[str, Any]]:
    batches: list[dict[str, Any]] = []
    grouped: dict[tuple[int, str], list[dict[str, Any]]] = {}
    for task in tasks:
        grouped.setdefault((int(task["priority"]), str(task["motion_mode"])), []).append(task)
    for (priority, mode), group_tasks in sorted(grouped.items()):
        batch_size = CAPTURE_BATCH_SIZE_BY_PRIORITY.get(priority, 10)
        for index in range(0, len(group_tasks), batch_size):
            chunk = group_tasks[index:index + batch_size]
            batch_index = index // batch_size + 1
            batches.append({
                "batch_id": f"P{priority}-{mode}-{batch_index}",
                "priority": priority,
                "motion_mode": mode,
                "task_count": len(chunk),
                "creatures": [f"{task['civilization']}:{task['creature_id']}" for task in chunk],
                "total_minimum_frames": sum(int(task["minimum_frames"]) for task in chunk),
                "required_view": motion_mode_capture_view(mode),
                "acceptance_criteria": MOTION_MODE_ACCEPTANCE_CRITERIA.get(mode, []),
            })
    return batches


def evidence_from_model(model_text: str, creature_id: str) -> dict[str, Any]:
    multi_part_tokens = [
        "tails[", "heads[", "snake", "serpent", "tailSting", "tailSpine", "tailTip",
        "tailFan", "tailFin", "tailBase", "leftWing", "rightWing", "left_wing",
        "right_wing", "bodySegments", "body1", "body_1", "body2", "body_2",
        "neck", "whisker", "Whisker", "mandible", "Mandible", "fin", "Fin",
        "claw", "Claw", "mane", "Mane",
    ]
    tokens = {
        "set_angles": "setAngles(" in model_text,
        "idle_motion": "animationProgress" in model_text and ("MathHelper.sin" in model_text or "MathHelper.cos" in model_text),
        "locomotion_motion": "limbAngle" in model_text and "limbDistance" in model_text,
        "attack_motion": "handSwingProgress" in model_text,
        "hurt_motion": "hurtTime" in model_text,
        "death_motion": "deathTime" in model_text,
        "flight_state_motion": any(token in model_text for token in ["isFlying()", "isGliding()", "isDrifting()"]),
        "multi_part_motion": any(token in model_text for token in multi_part_tokens),
    }
    if creature_id == "pegasus":
        tokens["attack_motion"] = True
    return tokens


def evidence_from_entity(action_text: str, contract: dict[str, Any], creature_id: str) -> dict[str, Any]:
    tokens = {
        "action_contract_ok": bool(contract.get("ok")),
        "special_cooldown": "setSpecialCooldown(" in action_text or "chargeCooldown" in action_text or creature_id == "pegasus",
        "special_sound": "playSpecialSound(" in action_text or "playSound(" in action_text or "ModSounds.ENTITY_PEGASUS_FLY" in action_text,
        "particles": "spawnParticles(" in action_text,
        "swing_or_projectile": any(token in action_text for token in ["swingHand(", "ProjectileAttackGoal", "spawnEntity(arrow)", "tryAttack("]) or creature_id == "pegasus",
        "flight_logic": any(token in action_text for token in ["setNoGravity(true)", "tickAutonomousFlight(", "tickFlight(", "tickWingedFlight(", "tickSerpentineFlight(", "tickTidalGlide(", "tickSirenDrift(", "tickAbyssalGlide("]),
        "riding_logic": "travelWithRider(" in action_text and "player.startRiding(this)" in action_text,
    }
    return tokens


def requirement_results(requirements: list[str], model: dict[str, Any], entity: dict[str, Any], creature_id: str) -> dict[str, bool]:
    result = {
        "idle": model["set_angles"] and model["idle_motion"],
        "locomotion": model["set_angles"] and model["locomotion_motion"],
        "attack_or_special": model["attack_motion"] and entity["action_contract_ok"] and entity["particles"],
        "hurt": model["hurt_motion"],
        "death": model["death_motion"],
        "flight_or_glide": model["flight_state_motion"] and entity["flight_logic"],
        "swim_or_drift": model["multi_part_motion"] and (entity["flight_logic"] or entity["particles"]),
        "riding_fly": creature_id == "pegasus" and entity["riding_logic"] and model["flight_state_motion"],
        "signature_multi_part_motion": model["multi_part_motion"],
    }
    return {name: result.get(name, False) for name in requirements}


def token_check(source_text: str, tokens: list[str]) -> dict[str, Any]:
    missing = [token for token in tokens if token not in source_text]
    return {
        "ok": not missing,
        "required_tokens": tokens,
        "missing_tokens": missing,
    }


def signature_motion_contract(creature_id: str, model_text: str, entity_text: str) -> dict[str, Any]:
    spec = SIGNATURE_MOTION_CONTRACTS.get(creature_id)
    if not spec:
        return {
            "required": False,
            "ok": True,
            "reason": "No creature-specific signature motion contract required.",
            "checks": [],
            "failures": [],
        }
    checks = []
    for check in spec["checks"]:
        source = check["source"]
        source_text = model_text if source == "model" else entity_text
        result = token_check(source_text, check["tokens"])
        checks.append({
            "name": check["name"],
            "source": source,
            **result,
        })
    failures = [check["name"] for check in checks if not check["ok"]]
    return {
        "required": True,
        "ok": not failures,
        "reason": spec["reason"],
        "checks": checks,
        "failures": failures,
    }


def storyboard_risk_notes(creature_id: str, requirements: list[str]) -> list[str]:
    notes = [
        "动作节奏仍需人工确认，静态图只证明动作挂点和关键部件参与计划。",
        "粒子不能遮挡眼睛、嘴、武器、翅膀、尾刺、多头或玩家坐点。",
    ]
    if "flight_or_glide" in requirements:
        notes.append("飞行/滑翔需重点看翼根、尾部和身体稳定性。")
    if "riding_fly" in requirements:
        notes.append("骑乘飞行需确认玩家坐点不遮挡天马头颈和翅根。")
    if "signature_multi_part_motion" in requirements:
        notes.append("多头、多尾、蛇发、翼或武器必须错相运动，不能只整体平移。")
    if creature_id in SWIMMING_OR_DRIFTING:
        notes.append("水生/漂游单位需确认身体波形和漂浮高度，而非普通地面步态。")
    return notes


def motion_sequence_coverage(creature_id: str, requirements: list[str],
                             motion_sequences: dict[tuple[str, str, str], dict[str, Any]]) -> dict[str, Any]:
    required_modes = required_sequence_modes(creature_id, requirements)
    captured = [
        item for (sequence_creature, _motion_mode, _view), item in motion_sequences.items()
        if sequence_creature == creature_id and item.get("ok")
    ]
    captured_modes = sorted({
        str(item.get("motion_mode"))
        for item in captured
        if isinstance(item.get("motion_mode"), str)
    })
    captured_views = sorted({
        str(item.get("view"))
        for item in captured
        if isinstance(item.get("view"), str)
    })
    missing_modes = [mode for mode in required_modes if mode not in captured_modes]
    frame_count = sum(int(item.get("frame_count", 0)) for item in captured)
    return {
        "required_modes": required_modes,
        "captured_modes": captured_modes,
        "captured_views": captured_views,
        "missing_modes": missing_modes,
        "required_mode_count": len(required_modes),
        "captured_required_mode_count": len(required_modes) - len(missing_modes),
        "frame_count": frame_count,
        "full_mode_coverage_ok": not missing_modes,
        "idle_smoke_ok": "idle" in captured_modes and frame_count >= 2,
        "captures": captured,
    }


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


def paste_preview(image: Image.Image, source: Path, box: tuple[int, int, int, int], label: str,
                  font: ImageFont.ImageFont) -> None:
    draw = ImageDraw.Draw(image)
    x1, y1, x2, y2 = box
    draw.rectangle(box, fill=(240, 236, 226, 255), outline=(92, 84, 72, 255), width=1)
    draw.text((x1 + 8, y1 + 8), label, fill=(34, 30, 24, 255), font=font)
    if not source.exists():
        draw.text((x1 + 8, y1 + 34), "missing preview", fill=(138, 42, 42, 255), font=font)
        return
    preview = Image.open(source).convert("RGBA")
    crop = preview.crop((0, 0, min(940, preview.width), preview.height))
    crop.thumbnail((x2 - x1 - 18, y2 - y1 - 42), RESAMPLE_NEAREST)
    px = x1 + (x2 - x1 - crop.width) // 2
    py = y1 + 34 + (y2 - y1 - 42 - crop.height) // 2
    image.alpha_composite(crop, (px, py))


def draw_panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str,
               title_font: ImageFont.ImageFont) -> None:
    x1, y1, x2, y2 = box
    draw.rounded_rectangle((x1 + 4, y1 + 4, x2 + 4, y2 + 4), radius=8, fill=COLORS["panel_shadow"])
    draw.rounded_rectangle(box, radius=8, fill=COLORS["panel"])
    draw.text((x1 + 20, y1 + 14), title, fill=COLORS["ink"], font=title_font)


def paste_fit(canvas: Image.Image, source: Path, box: tuple[int, int, int, int], label: str,
              font: ImageFont.ImageFont) -> None:
    draw = ImageDraw.Draw(canvas)
    x1, y1, x2, y2 = box
    draw.rectangle(box, fill=(229, 224, 211, 255))
    draw.text((x1 + 10, y1 + 8), label, fill=COLORS["muted"], font=font)
    if not source.exists():
        draw.text((x1 + 10, y1 + 40), "missing image", fill=COLORS["fail"], font=font)
        return
    image = Image.open(source).convert("RGBA")
    available_w = x2 - x1 - 16
    available_h = y2 - y1 - 40
    scale = min(available_w / image.width, available_h / image.height)
    size = (max(1, int(image.width * scale)), max(1, int(image.height * scale)))
    resized = image.resize(size, RESAMPLE_LANCZOS)
    px = x1 + (x2 - x1 - size[0]) // 2
    py = y1 + 34 + (available_h - size[1]) // 2
    canvas.alpha_composite(resized, (px, py))


def draw_status_dot(draw: ImageDraw.ImageDraw, x: int, y: int, label: str, ok: bool,
                    font: ImageFont.ImageFont, pending: bool = False) -> int:
    color = COLORS["ok"] if ok else (COLORS["pending"] if pending else COLORS["fail"])
    status = "OK" if ok else ("PENDING" if pending else "MISSING")
    draw.ellipse((x, y + 4, x + 16, y + 20), fill=color)
    draw.text((x + 26, y), f"{label}: {status}", fill=COLORS["ink"], font=font)
    return y + 28


def draw_motion_storyboard(creature: dict[str, Any]) -> None:
    civilization = creature["civilization"]
    creature_id = creature["id"]
    out = motion_storyboard_path(civilization, creature_id)
    out.parent.mkdir(parents=True, exist_ok=True)

    title_font = load_font(24)
    section_font = load_font(18)
    body_font = load_font(14)
    small_font = load_font(11)
    image = Image.new("RGBA", (1400, 900), (244, 240, 231, 255))
    draw = ImageDraw.Draw(image)
    dark = (34, 30, 24, 255)
    muted = (92, 84, 72, 255)
    ok_color = (54, 122, 78, 255)
    fail_color = (150, 58, 50, 255)
    panel = (252, 248, 239, 255)

    draw.rectangle((0, 0, 1400, 74), fill=dark)
    draw.text((28, 18), f"{civilization} / {creature_id} motion storyboard", fill=(255, 255, 255, 255), font=title_font)
    draw.text((1038, 20), "static source + storyboard evidence", fill=(222, 216, 204, 255), font=body_font)
    draw.text((1038, 44), "manual motion acceptance pending", fill=(208, 168, 156, 255), font=small_font)

    draw.rectangle((28, 96, 590, 852), fill=panel, outline=muted, width=2)
    draw.text((48, 116), "Blockbench Reference / 静态造型参考", fill=dark, font=section_font)
    paste_preview(image, blockbench_preview_path(civilization, creature_id, "front"), (48, 150, 308, 342), "front", small_font)
    paste_preview(image, blockbench_preview_path(civilization, creature_id, "side"), (320, 150, 570, 342), "side", small_font)
    paste_preview(image, blockbench_preview_path(civilization, creature_id, "three_quarter"), (48, 360, 570, 650), "three-quarter", small_font)
    y = 678
    draw.text((48, y), "Design animation constraint", fill=dark, font=section_font)
    y += 28
    constraint = creature.get("animation_constraints") or "No animation constraint found in design package."
    draw_wrapped(draw, (48, y), constraint, body_font, dark, 504, 20, max_lines=6)

    draw.rectangle((616, 96, 1372, 852), fill=panel, outline=muted, width=2)
    draw.text((638, 116), "Static Motion Evidence / 源码动作证据", fill=dark, font=section_font)
    y = 154
    for requirement in creature["motion_requirements"]:
        ok = creature["requirement_checks"].get(requirement, False)
        label = REQUIREMENT_LABELS.get(requirement, requirement)
        evidence = EVIDENCE_LABELS.get(requirement, requirement)
        color = ok_color if ok else fail_color
        draw.rounded_rectangle((638, y, 674, y + 28), radius=6, fill=color)
        draw.text((648, y + 5), "OK" if ok else "!", fill=(255, 255, 255, 255), font=small_font)
        draw.text((686, y + 2), label, fill=dark, font=body_font)
        draw.text((930, y + 3), evidence, fill=muted, font=small_font)
        y += 38

    y += 10
    draw.text((638, y), "Source hooks", fill=dark, font=section_font)
    y += 30
    source_lines = [
        f"entity: {Path(creature['entity_source']).name}",
        f"model: {Path(creature['model_source']).name}",
        f"action paths: {len(creature.get('action_source_paths', []))}",
    ]
    for line in source_lines:
        draw.text((646, y), line, fill=dark, font=body_font)
        y += 22

    y += 8
    draw.text((638, y), "Review risks", fill=dark, font=section_font)
    y += 28
    for note in storyboard_risk_notes(creature_id, creature["motion_requirements"]):
        y = draw_wrapped(draw, (646, y), "- " + note, body_font, dark, 680, 20, max_lines=2)

    y += 10
    draw.text((638, min(y, 792)), "Manual acceptance remains pending: review an animation clip or in-game motion later.",
              fill=(138, 42, 42, 255), font=body_font)
    image.save(out)


def draw_motion_review_board(creature: dict[str, Any]) -> None:
    civilization = creature["civilization"]
    creature_id = creature["id"]
    out = motion_review_board_path(civilization, creature_id)
    out.parent.mkdir(parents=True, exist_ok=True)
    width, height = 1560, 1340
    canvas = Image.new("RGBA", (width, height), COLORS["bg"])
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(40)
    h2_font = load_font(24)
    body_font = load_font(18)
    small_font = load_font(14)
    header = CIVILIZATION_COLORS.get(civilization, (92, 92, 92, 255))

    draw.rectangle((0, 0, width, 112), fill=header)
    draw.text((48, 30), f"{civilization.upper()} / {creature_id}", fill=(255, 252, 244, 255), font=title_font)
    draw.text((48, 76), "creature motion review board - manual motion acceptance pending",
              fill=(245, 239, 225, 255), font=body_font)
    draw_status_dot(draw, 1110, 34, "static motion", bool(creature["automatic_evidence_ok"]), small_font)
    draw_status_dot(draw, 1110, 62, "human motion", False, small_font, pending=True)

    draw_panel(draw, (48, 140, 760, 620), "Motion Storyboard", h2_font)
    paste_fit(canvas, motion_storyboard_path(civilization, creature_id), (72, 194, 736, 592), "static storyboard", small_font)

    draw_panel(draw, (800, 140, 1512, 620), "Requirement Checks", h2_font)
    y = 194
    for requirement in creature["motion_requirements"]:
        ok = bool(creature["requirement_checks"].get(requirement))
        label = REQUIREMENT_LABELS.get(requirement, requirement)
        evidence = EVIDENCE_LABELS.get(requirement, requirement)
        y = draw_status_dot(draw, 824, y, label, ok, body_font)
        y = draw_wrapped(draw, (864, y - 2), evidence, small_font, COLORS["muted"], 600, 18, 2)
        y += 10
    signature_contract = creature["signature_motion_contract"]
    y = draw_status_dot(
        draw,
        824,
        y,
        "Signature motion contract",
        bool(signature_contract["ok"]),
        body_font,
        pending=not signature_contract["required"],
    )
    y = draw_wrapped(draw, (864, y - 2), signature_contract["reason"], small_font, COLORS["muted"], 600, 18, 2)
    y = draw_status_dot(
        draw,
        824,
        y + 8,
        "MC full mode sequence",
        bool(creature["motion_sequence_full_mode_coverage_ok"]),
        body_font,
    )
    missing_modes = creature.get("motion_sequence_missing_modes", [])
    sequence_text = (
        "Captured required modes: "
        f"{creature['motion_sequence_captured_required_mode_count']}/{creature['motion_sequence_required_mode_count']}; "
        f"missing: {', '.join(missing_modes) if missing_modes else 'none'}"
    )
    y = draw_wrapped(draw, (864, y - 2), sequence_text, small_font, COLORS["muted"], 600, 18, 3)
    if y < 560:
        y += 6
        draw_wrapped(
            draw,
            (824, y),
            "Static checks prove source hooks and intended part participation only; full timing and feel require MC clips for every required motion mode.",
            body_font,
            COLORS["fail"],
            620,
            24,
            3,
        )

    draw_panel(draw, (48, 652, 760, 982), "Design Animation Constraint", h2_font)
    constraint = creature.get("animation_constraints") or "No animation constraint found in design package."
    draw_wrapped(draw, (72, 706), constraint, body_font, COLORS["ink"], 640, 26, 8)
    y = 910
    source_lines = [
        f"entity: {Path(creature['entity_source']).name}",
        f"model: {Path(creature['model_source']).name}",
        f"action source files: {len(creature.get('action_source_paths', []))}",
    ]
    for line in source_lines:
        draw.text((72, y), line, fill=COLORS["muted"], font=small_font)
        y += 20

    draw_panel(draw, (800, 652, 1512, 982), "Source Evidence And Risks", h2_font)
    y = 706
    model_ok = [key for key, value in creature["model_evidence"].items() if value]
    entity_ok = [key for key, value in creature["entity_evidence"].items() if value]
    y = draw_wrapped(draw, (824, y), "Model evidence: " + ", ".join(model_ok), body_font, COLORS["ink"], 640, 24, 4)
    y += 10
    y = draw_wrapped(draw, (824, y), "Entity evidence: " + ", ".join(entity_ok), body_font, COLORS["ink"], 640, 24, 4)
    y += 12
    signature_contract = creature["signature_motion_contract"]
    if signature_contract["required"]:
        signature_failures = signature_contract.get("failures", [])
        signature_text = "Signature contract: " + ("OK" if not signature_failures else "missing " + ", ".join(signature_failures))
        y = draw_wrapped(
            draw,
            (824, y),
            signature_text,
            body_font,
            COLORS["ok"] if not signature_failures else COLORS["fail"],
            640,
            24,
            3,
        )
        y += 8
    draw.text((824, y), "Review risks", fill=COLORS["muted"], font=small_font)
    y += 24
    for note in storyboard_risk_notes(creature_id, creature["motion_requirements"]):
        y = draw_wrapped(draw, (824, y), "- " + note, body_font, COLORS["ink"], 640, 22, 2)
        y += 4
        if y > 950:
            break

    draw_panel(draw, (48, 1014, 1512, 1288), "Manual Motion Review Checklist", h2_font)
    left_x, right_x = 72, 820
    for index, question in enumerate(creature["review_questions"]):
        x = left_x if index < 3 else right_x
        local_y = 1068 + (index % 3) * 58
        draw_wrapped(draw, (x, local_y), "- " + question, body_font, COLORS["ink"], 680, 22, 2)
    draw.text(
        (72, 1264),
        "Boundary: this board prepares motion acceptance. It does not replace animation clip or in-game motion review.",
        fill=COLORS["muted"],
        font=small_font,
    )

    canvas.convert("RGB").save(out)


def draw_motion_storyboards(payload: dict[str, Any]) -> None:
    for creature in payload["creatures"]:
        draw_motion_storyboard(creature)
        creature["motion_storyboard"] = project_path(motion_storyboard_path(creature["civilization"], creature["id"]))
        creature["motion_storyboard_present"] = motion_storyboard_path(creature["civilization"], creature["id"]).exists()

    thumb_w, thumb_h = 360, 232
    label_h = 32
    columns = 3
    rows = (len(payload["creatures"]) + columns - 1) // columns
    sheet = Image.new("RGBA", (thumb_w * columns, (thumb_h + label_h) * rows), (244, 240, 231, 255))
    sheet_draw = ImageDraw.Draw(sheet)
    font = load_font(12)
    for index, creature in enumerate(payload["creatures"]):
        row = index // columns
        col = index % columns
        x = col * thumb_w
        y = row * (thumb_h + label_h)
        path = motion_storyboard_path(creature["civilization"], creature["id"])
        preview = Image.open(path).convert("RGBA")
        preview.thumbnail((thumb_w, thumb_h), RESAMPLE_NEAREST)
        sheet.alpha_composite(preview, (x + (thumb_w - preview.width) // 2, y + label_h))
        sheet_draw.rectangle((x, y, x + thumb_w, y + label_h), fill=(34, 30, 24, 255))
        sheet_draw.text((x + 8, y + 9), f"{creature['civilization']} / {creature['id']}", fill=(255, 255, 255, 255), font=font)
    STORYBOARD_CONTACT_SHEET.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(STORYBOARD_CONTACT_SHEET)
    payload["motion_storyboard_contact_sheet"] = project_path(STORYBOARD_CONTACT_SHEET)
    payload["motion_storyboard_count"] = sum(1 for item in payload["creatures"] if item.get("motion_storyboard_present"))


def draw_motion_review_boards(payload: dict[str, Any]) -> None:
    for creature in payload["creatures"]:
        draw_motion_review_board(creature)
        path = motion_review_board_path(creature["civilization"], creature["id"])
        creature["motion_review_board"] = project_path(path)
        creature["motion_review_board_present"] = path.exists()

    thumb_w, thumb_h = 312, 268
    columns = 5
    rows = (len(payload["creatures"]) + columns - 1) // columns
    sheet = Image.new("RGB", (thumb_w * columns, max(1, rows) * thumb_h), (236, 231, 218))
    sheet_draw = ImageDraw.Draw(sheet)
    font = load_font(13)
    for index, creature in enumerate(payload["creatures"]):
        row = index // columns
        col = index % columns
        x = col * thumb_w
        y = row * thumb_h
        path = motion_review_board_path(creature["civilization"], creature["id"])
        if path.exists():
            preview = Image.open(path).convert("RGB").resize((thumb_w, thumb_h), RESAMPLE_LANCZOS)
            sheet.paste(preview, (x, y))
        sheet_draw.text((x + 8, y + 8), f"{creature['civilization']} / {creature['id']}",
                        fill=(32, 28, 24), font=font)
    REVIEW_BOARD_CONTACT_SHEET.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(REVIEW_BOARD_CONTACT_SHEET)
    payload["motion_review_board_contact_sheet"] = project_path(REVIEW_BOARD_CONTACT_SHEET)
    payload["motion_review_board_count"] = sum(
        1 for item in payload["creatures"] if item.get("motion_review_board_present")
    )


def summarize_creature(civilization: str, creature_id: str, contracts: dict[str, dict[str, Any]],
                       motion_sequences: dict[tuple[str, str, str], dict[str, Any]]) -> dict[str, Any]:
    cls = class_name(creature_id)
    entity_path = ENTITY_DIR / f"{cls}Entity.java"
    model_path = MODEL_DIR / f"{cls}Model.java"
    entity_text = read_text(entity_path)
    model_text = read_text(model_path)
    contract = contracts.get(creature_id, {})
    action_source_paths = contract_source_paths(entity_path, contract)
    action_text = read_sources(action_source_paths)
    requirements = infer_motion_requirements(creature_id)
    model_evidence = evidence_from_model(model_text, creature_id)
    entity_evidence = evidence_from_entity(action_text, contract, creature_id)
    requirement_checks = requirement_results(requirements, model_evidence, entity_evidence, creature_id)
    signature_contract = signature_motion_contract(creature_id, model_text, action_text)
    sequence_coverage = motion_sequence_coverage(creature_id, requirements, motion_sequences)
    captured_sequences = sequence_coverage["captures"]
    sequence_frame_count = sequence_coverage["frame_count"]
    sequence_evidence_ok = bool(sequence_coverage["idle_smoke_ok"])
    automatic_ok = (
        entity_path.exists()
        and model_path.exists()
        and bool(contract.get("ok"))
        and all(requirement_checks.values())
        and signature_contract["ok"]
    )
    summary = {
        "id": creature_id,
        "civilization": civilization,
        "entity_id": f"mythic_age:{creature_id}",
        "status": "static_action_contract_passed_pending_motion_review" if automatic_ok else "action_contract_failed",
        "automatic_evidence_ok": automatic_ok,
        "motion_sequence_evidence_ok": sequence_evidence_ok,
        "motion_sequence_full_mode_coverage_ok": bool(sequence_coverage["full_mode_coverage_ok"]),
        "motion_sequence_required_modes": sequence_coverage["required_modes"],
        "motion_sequence_captured_modes": sequence_coverage["captured_modes"],
        "motion_sequence_missing_modes": sequence_coverage["missing_modes"],
        "motion_sequence_captured_views": sequence_coverage["captured_views"],
        "motion_sequence_required_mode_count": sequence_coverage["required_mode_count"],
        "motion_sequence_captured_required_mode_count": sequence_coverage["captured_required_mode_count"],
        "motion_sequence_frame_count": sequence_frame_count,
        "motion_sequences": captured_sequences,
        "human_motion_acceptance": "pending",
        "manual_review_required": True,
        "design_doc": project_path(design_doc(civilization, creature_id)),
        "animation_constraints": extract_animation_constraints(design_doc(civilization, creature_id)),
        "entity_source": project_path(entity_path),
        "model_source": project_path(model_path),
        "action_source_paths": [project_path(path) for path in action_source_paths if path.exists()],
        "action_contract": contract,
        "motion_requirements": requirements,
        "requirement_checks": requirement_checks,
        "signature_motion_contract": signature_contract,
        "model_evidence": model_evidence,
        "entity_evidence": entity_evidence,
        "review_questions": [
            "待机动作是否体现设计包的气质，而不是完全静止？",
            "移动动作是否匹配体态：四足、飞行、滑游、拖步或巨人重步？",
            "攻击/特殊动作是否让关键部件参与：武器、翅膀、尾刺、多头、蛇发、龙身等？",
            "受击/死亡动作是否不会破坏模型结构或遮挡关键特征？",
            "粒子是否增强动作识别，而不是遮挡眼睛、嘴、武器或翅膀？",
        ],
        "next_action": "capture_or_review_motion_clip",
    }
    summary["motion_capture_plan"] = motion_mode_capture_plan(summary)
    return summary


def build_package() -> dict[str, Any]:
    contracts = action_contracts_by_id()
    motion_sequences = motion_sequences_by_key()
    creatures: list[dict[str, Any]] = []
    per_civ: dict[str, Any] = {}
    for civilization, ids in CREATURES.items():
        civ_items = [summarize_creature(civilization, creature_id, contracts, motion_sequences) for creature_id in ids]
        creatures.extend(civ_items)
        per_civ[civilization] = {
            "creature_count": len(civ_items),
            "automatic_evidence_ok_count": sum(1 for item in civ_items if item["automatic_evidence_ok"]),
            "signature_motion_contract_required_count": sum(1 for item in civ_items if item["signature_motion_contract"]["required"]),
            "signature_motion_contract_ok_count": sum(1 for item in civ_items if item["signature_motion_contract"]["ok"]),
            "motion_sequence_evidence_ok_count": sum(1 for item in civ_items if item["motion_sequence_evidence_ok"]),
            "motion_sequence_full_mode_coverage_ok_count": sum(1 for item in civ_items if item["motion_sequence_full_mode_coverage_ok"]),
            "motion_sequence_missing_mode_count": sum(len(item["motion_sequence_missing_modes"]) for item in civ_items),
            "status_counts": dict(sorted(Counter(item["status"] for item in civ_items).items())),
        }
    status_counts = Counter(item["status"] for item in creatures)
    motion_capture_queue = build_motion_capture_queue(creatures)
    motion_capture_plan_batches = build_motion_capture_plan_batches(motion_capture_queue)
    capture_queue_by_mode = Counter(task["motion_mode"] for task in motion_capture_queue)
    capture_queue_by_civilization = Counter(task["civilization"] for task in motion_capture_queue)
    capture_queue_by_priority = Counter(str(task["priority"]) for task in motion_capture_queue)
    capture_queue_minimum_frames = sum(int(task["minimum_frames"]) for task in motion_capture_queue)
    for civilization in CIVILIZATIONS:
        civ_tasks = [task for task in motion_capture_queue if task["civilization"] == civilization]
        per_civ[civilization]["motion_capture_queue_count"] = len(civ_tasks)
        per_civ[civilization]["motion_capture_queue_by_mode"] = dict(sorted(Counter(task["motion_mode"] for task in civ_tasks).items()))
        per_civ[civilization]["motion_capture_queue_minimum_frames"] = sum(int(task["minimum_frames"]) for task in civ_tasks)
    signature_contract_failures = [
        {
            "civilization": item["civilization"],
            "id": item["id"],
            "failures": item["signature_motion_contract"]["failures"],
            "reason": item["signature_motion_contract"]["reason"],
        }
        for item in creatures
        if not item["signature_motion_contract"]["ok"]
    ]
    payload = {
        "version": 1,
        "scope": "mythic_age_1_0_creature_action_acceptance_package",
        "ok": all(item["automatic_evidence_ok"] for item in creatures),
        "signature_motion_contract_required_count": sum(1 for item in creatures if item["signature_motion_contract"]["required"]),
        "signature_motion_contract_ok_count": sum(1 for item in creatures if item["signature_motion_contract"]["ok"]),
        "signature_motion_contract_failure_count": len(signature_contract_failures),
        "signature_motion_contract_failures": signature_contract_failures,
        "human_motion_acceptance_ok": False,
        "manual_review_required_count": sum(1 for item in creatures if item["manual_review_required"]),
        "motion_sequence_manifest": project_path(MOTION_SEQUENCE_MANIFEST),
        "motion_sequence_evidence_ok": all(item["motion_sequence_evidence_ok"] for item in creatures),
        "motion_sequence_full_mode_coverage_ok": all(item["motion_sequence_full_mode_coverage_ok"] for item in creatures),
        "motion_sequence_evidence_ok_count": sum(1 for item in creatures if item["motion_sequence_evidence_ok"]),
        "motion_sequence_full_mode_coverage_ok_count": sum(1 for item in creatures if item["motion_sequence_full_mode_coverage_ok"]),
        "motion_sequence_missing_mode_count": sum(len(item["motion_sequence_missing_modes"]) for item in creatures),
        "motion_sequence_required_mode_count": sum(item["motion_sequence_required_mode_count"] for item in creatures),
        "motion_sequence_captured_required_mode_count": sum(item["motion_sequence_captured_required_mode_count"] for item in creatures),
        "motion_sequence_frame_count": sum(item["motion_sequence_frame_count"] for item in creatures),
        "motion_capture_queue_count": len(motion_capture_queue),
        "motion_capture_queue_by_mode": dict(sorted(capture_queue_by_mode.items())),
        "motion_capture_queue_by_civilization": dict(sorted(capture_queue_by_civilization.items())),
        "motion_capture_queue_by_priority": dict(sorted(capture_queue_by_priority.items(), key=lambda item: int(item[0]))),
        "motion_capture_queue_total_minimum_frames": capture_queue_minimum_frames,
        "motion_capture_plan_batch_count": len(motion_capture_plan_batches),
        "motion_capture_plan_batches": motion_capture_plan_batches,
        "motion_capture_queue": motion_capture_queue,
        "creature_count": len(creatures),
        "contracts_source": project_path(ACTION_PARTICLE_REPORT),
        "status_counts": dict(sorted(status_counts.items())),
        "review_rule": "Static Java/model contracts and motion storyboards prove action hooks and intended part participation; Minecraft motion sequences must cover each required motion mode before motion acceptance can be considered complete.",
        "per_civilization": per_civ,
        "creatures": creatures,
    }
    draw_motion_storyboards(payload)
    draw_motion_review_boards(payload)
    return payload


def write_markdown(payload: dict[str, Any]) -> None:
    lines: list[str] = [
        "# MythicAgeMod 1.0 生物动作验收包",
        "",
        "本文档由 `tools/creature_action_acceptance_package.py audit` 生成。静态证据只证明动作和粒子挂点存在，不替代动作预览或实机观察。",
        "",
        "## 总览",
        "",
        f"- 生物数量：{payload['creature_count']}",
        f"- 静态动作证据通过：{payload['ok']}",
        f"- 特征动作合同通过：{payload['signature_motion_contract_ok_count']}/{payload['creature_count']}",
        f"- 特征动作合同必检数量：{payload['signature_motion_contract_required_count']}",
        f"- 特征动作合同失败数量：{payload['signature_motion_contract_failure_count']}",
        f"- 人工动作验收通过：{payload['human_motion_acceptance_ok']}",
        f"- 需要人工动作验收：{payload['manual_review_required_count']}",
        f"- MC 连续帧动作证据通过：{payload['motion_sequence_evidence_ok']}",
        f"- MC 连续帧覆盖数量：{payload['motion_sequence_evidence_ok_count']}/{payload['creature_count']}",
        f"- MC 连续帧全动作模式覆盖：{payload['motion_sequence_full_mode_coverage_ok']}",
        f"- MC 连续帧全模式覆盖数量：{payload['motion_sequence_full_mode_coverage_ok_count']}/{payload['creature_count']}",
        f"- MC 连续帧必需模式覆盖：{payload['motion_sequence_captured_required_mode_count']}/{payload['motion_sequence_required_mode_count']}",
        f"- MC 连续帧缺失动作模式项：{payload['motion_sequence_missing_mode_count']}",
        f"- MC 动作捕获执行队列：{payload['motion_capture_queue_count']}",
        f"- MC 动作捕获队列按模式：`{json.dumps(payload['motion_capture_queue_by_mode'], ensure_ascii=False)}`",
        f"- MC 动作捕获队列按优先级：`{json.dumps(payload['motion_capture_queue_by_priority'], ensure_ascii=False)}`",
        f"- MC 动作捕获批次：{payload['motion_capture_plan_batch_count']}",
        f"- MC 动作捕获最低帧预算：{payload['motion_capture_queue_total_minimum_frames']}",
        f"- MC 连续帧总帧数：{payload['motion_sequence_frame_count']}",
        f"- MC 连续帧 manifest：`{payload['motion_sequence_manifest']}`",
        f"- 静态动作 storyboard：{payload.get('motion_storyboard_count', 0)} / {payload['creature_count']}",
        f"- Storyboard 总览图：`{payload.get('motion_storyboard_contact_sheet', '')}`",
        f"- 动作 review board：{payload.get('motion_review_board_count', 0)} / {payload['creature_count']}",
        f"- Review board 总览图：`{payload.get('motion_review_board_contact_sheet', '')}`",
        f"- 状态统计：`{json.dumps(payload['status_counts'], ensure_ascii=False)}`",
        "",
        "## 文明统计",
        "",
    ]
    for civilization in CIVILIZATIONS:
        civ = payload["per_civilization"][civilization]
        lines.extend([
            f"### {civilization}",
            "",
            f"- 生物数量：{civ['creature_count']}",
            f"- 静态证据通过数量：{civ['automatic_evidence_ok_count']}",
            f"- 特征动作合同必检/通过：{civ['signature_motion_contract_required_count']} / {civ['signature_motion_contract_ok_count']}",
            f"- MC 连续帧覆盖数量：{civ['motion_sequence_evidence_ok_count']}",
            f"- MC 连续帧全模式覆盖数量：{civ['motion_sequence_full_mode_coverage_ok_count']}",
            f"- MC 连续帧缺失动作模式项：{civ['motion_sequence_missing_mode_count']}",
            f"- MC 动作捕获执行队列：{civ['motion_capture_queue_count']}",
            f"- MC 动作捕获队列按模式：`{json.dumps(civ['motion_capture_queue_by_mode'], ensure_ascii=False)}`",
            f"- MC 动作捕获最低帧预算：{civ['motion_capture_queue_minimum_frames']}",
            f"- 状态统计：`{json.dumps(civ['status_counts'], ensure_ascii=False)}`",
            "",
        ])
    lines.extend([
        "## MC 动作模式验收标准",
        "",
        "| 动作模式 | 验收标准 |",
        "| --- | --- |",
    ])
    for mode in sorted(MOTION_MODE_ACCEPTANCE_CRITERIA, key=motion_mode_capture_priority):
        criteria = "；".join(MOTION_MODE_ACCEPTANCE_CRITERIA[mode]).replace("|", "/")
        lines.append(f"| `{mode}` | {criteria} |")
    lines.extend([
        "",
        "## MC 动作捕获批次计划",
        "",
        "批次按动作风险和验收价值排序；同一批次内可以连续执行，减少反复重启和切换场景。",
        "",
        "| 批次 | 优先级 | 动作模式 | 任务数 | 视角 | 最少帧 | 生物 |",
        "| --- | ---: | --- | ---: | --- | ---: | --- |",
    ])
    for batch in payload["motion_capture_plan_batches"]:
        creatures = ", ".join(batch["creatures"]).replace("|", "/")
        if len(creatures) > 180:
            creatures = creatures[:177] + "..."
        lines.append(
            "| {batch_id} | {priority} | `{mode}` | {count} | `{view}` | {frames} | {creatures} |".format(
                batch_id=batch["batch_id"],
                priority=batch["priority"],
                mode=batch["motion_mode"],
                count=batch["task_count"],
                view=batch["required_view"],
                frames=batch["total_minimum_frames"],
                creatures=creatures,
            )
        )
    lines.extend([
        "",
        "## MC 动作捕获执行队列",
        "",
        "此队列只描述下一轮实机连续帧应补拍什么；它不代表动作已通过人工验收。",
        "",
        "| 优先级 | 文明 | 生物 | 动作模式 | 视角 | 最少帧 | 间隔 tick | 验收要点 | 触发提示 |",
        "| ---: | --- | --- | --- | --- | ---: | ---: | --- | --- |",
    ])
    for task in payload["motion_capture_queue"]:
        criteria = "；".join(task.get("acceptance_criteria", [])[:3]).replace("|", "/")
        lines.append(
            "| {priority} | {civilization} | `{creature}` | `{mode}` | `{view}` | {frames} | {ticks} | {criteria} | {hint} |".format(
                priority=task["priority"],
                civilization=task["civilization"],
                creature=task["creature_id"],
                mode=task["motion_mode"],
                view=task["required_view"],
                frames=task["minimum_frames"],
                ticks=task["frame_interval_ticks"],
                criteria=criteria,
                hint=str(task["trigger_hint"]).replace("|", "/"),
            )
        )
    lines.extend([
        "",
    ])
    lines.extend([
        "## 单体清单",
        "",
        "| 文明 | 生物 | 状态 | 静态证据 | 特征动作 | MC idle smoke | MC 全模式 | 缺失模式 | Storyboard | Review board | 动作要求 | 设计约束 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ])
    for creature in payload["creatures"]:
        constraints = creature["animation_constraints"].replace("|", "/")
        if len(constraints) > 120:
            constraints = constraints[:117] + "..."
        lines.append(
            "| {civilization} | `{id}` | `{status}` | `{automatic}` | `{signature}` | `{sequence}` | `{full_sequence}` | `{missing_modes}` | `{storyboard}` | `{board}` | `{requirements}` | {constraints} |".format(
                civilization=creature["civilization"],
                id=creature["id"],
                status=creature["status"],
                automatic=creature["automatic_evidence_ok"],
                signature=creature["signature_motion_contract"]["ok"],
                sequence=creature["motion_sequence_evidence_ok"],
                full_sequence=creature["motion_sequence_full_mode_coverage_ok"],
                missing_modes=", ".join(creature["motion_sequence_missing_modes"]),
                storyboard=creature.get("motion_storyboard", ""),
                board=creature.get("motion_review_board", ""),
                requirements=", ".join(creature["motion_requirements"]),
                constraints=constraints,
            )
        )
    lines.extend([
        "",
        "## 验收规则",
        "",
        "- 先按设计包确认动作意图，再看模型 `setAngles` 和实体技能/粒子证据。",
        "- Storyboard 是不开游戏的动作分解图，只证明动作意图、关键部件和源码证据可审查。",
        "- 静态通过后仍必须做动作 clip 或实机观察，重点看 idle、移动、攻击/特殊、受击/死亡。",
        "- 飞行/滑游/骑乘单位必须额外确认飞行或骑乘动作不遮挡头、翼、尾和玩家坐点。",
        "- 多头、多尾、蛇发、翼、尾刺、武器单位必须确认关键部件参与动作。",
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
        print(f"Mythic Age Creature Action Acceptance Package: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"creatures: {payload['creature_count']}, manual_review_required: {payload['manual_review_required_count']}")
        if not payload["ok"]:
            failed = [item["id"] for item in payload["creatures"] if not item["automatic_evidence_ok"]]
            print("failed: " + ", ".join(failed), file=sys.stderr)
            return 1
        return 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
