#!/usr/bin/env python3
"""Audit Greek creature Java runtime model contracts.

This complements the Blockbench asset gate: it verifies that the Java models
registered in the mod still expose the named runtime parts that carry the
design-package silhouette. It intentionally does not start Minecraft.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "src/main/java/com/example/mythic_age/client/render/entity/model"
REPORT_PATH = ROOT / "docs/preview_assets/greek_creatures/runtime_model_quality_report.json"


CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "minotaur",
        "model": "MinotaurModel.java",
        "required_parts": [
            "head", "muzzle", "heavy_brow", "left_eye", "right_eye",
            "left_nostril", "right_nostril", "mouth", "left_horn", "right_horn",
            "left_ear", "right_ear",
            "body", "chest_harness", "left_chest_muscle", "right_chest_muscle",
            "waist_belt", "loincloth_front", "neck", "tail",
            "left_arm", "left_forearm", "left_fist", "left_finger_a", "left_finger_b",
            "right_arm", "right_forearm", "right_fist", "right_finger_a", "right_finger_b",
            "labrys_axe", "labrys_handle", "labrys_left_blade", "labrys_right_blade",
            "labrys_center_socket", "labrys_counterweight",
            "left_leg", "left_hoof", "left_hoof_claw",
            "right_leg", "right_hoof", "right_hoof_claw",
        ],
        "required_tokens": [
            ".uv(118, 0).cuboid",
            ".uv(84, 14).cuboid",
            ".uv(24, 30).cuboid",
            ".uv(36, 30).cuboid",
            ".uv(0, 42).cuboid",
            ".uv(24, 42).cuboid",
            ".uv(68, 48).mirrored().cuboid",
            ".uv(94, 48).cuboid",
            ".uv(86, 62).cuboid",
            ".uv(112, 62).cuboid",
            ".uv(88, 64).cuboid",
            ".uv(74, 86).cuboid",
            ".uv(100, 86).cuboid",
            ".uv(88, 86).cuboid",
            ".uv(74, 104).cuboid",
            ".uv(40, 78).cuboid",
            ".uv(64, 78).cuboid",
            "this.head.pitch -= 0.35F * charge",
            "this.labrysAxe.roll = -0.18F * charge",
        ],
        "reason": "Minotaur must read as a large bull-headed humanoid with nostrils, ears, horns, chest harness/muscle, fingers, hooves, hoof claws, tail, and a multi-part labrys axe",
    },
    {
        "id": "cyclops",
        "model": "CyclopsModel.java",
        "required_parts": [
            "head", "heavy_brow", "central_eye", "central_pupil", "nose_bridge",
            "left_nostril", "right_nostril", "mouth", "left_tooth", "right_tooth",
            "left_ear", "right_ear", "right_cheek_scar", "body", "chest_plate",
            "left_chest_muscle", "right_chest_muscle", "belly_shadow", "waist_belt",
            "loincloth_front", "loincloth_back", "left_arm", "left_forearm",
            "left_fist", "left_finger_a", "left_finger_b", "left_finger_c",
            "right_arm", "right_forearm", "right_fist", "right_finger_a",
            "right_finger_b", "right_finger_c", "stone_club", "stone_club_head",
            "stone_club_lump_top", "stone_club_lump_side", "stone_club_lump_back",
            "stone_club_knob", "left_leg", "left_shin", "left_foot", "left_toe_a",
            "left_toe_b", "left_toe_c", "right_leg", "right_shin", "right_foot",
            "right_toe_a", "right_toe_b", "right_toe_c",
        ],
        "required_tokens": [
            ".uv(80, 0).cuboid",  # oversized single eye
            ".uv(98, 0).cuboid",  # central pupil
            ".uv(118, 0).cuboid",  # nostril
            ".uv(72, 6).cuboid",  # tooth
            ".uv(84, 8).cuboid",  # ear
            ".uv(56, 28).cuboid",  # chest plate
            ".uv(94, 28).cuboid",  # chest muscle
            ".uv(80, 44).cuboid",  # waist belt
            ".uv(0, 62).cuboid",  # loincloth
            ".uv(0, 106).mirrored().cuboid",  # left fingers
            ".uv(18, 106).cuboid",  # right fingers
            ".uv(72, 92).mirrored().cuboid",  # left toes
            ".uv(90, 92).cuboid",  # right toes
            ".uv(112, 52).cuboid",  # stone club handle
            ".uv(88, 100).cuboid",  # stone club head
            ".uv(82, 60).cuboid",  # top club lump
            ".uv(104, 72).cuboid",  # back club lump
        ],
        "reason": "single eye with pupil, brow, nostrils, teeth, ears, chest muscles, belt/loincloth, fingers, toes, thick humanoid body, and multi-part stone club must be explicit runtime parts",
    },
    {
        "id": "centaur",
        "model": "CentaurModel.java",
        "required_parts": [
            "horse_body", "horse_chest", "horse_rump", "horse_shoulder_blend", "horse_belly_shadow",
            "human_torso", "human_waist", "human_shoulders", "human_chest_strap",
            "head", "nose_bridge", "left_eye", "right_eye", "mouth", "left_ear", "right_ear",
            "hair_crown", "hair_back", "mane_neck",
            "left_arm", "left_forearm", "left_hand", "left_finger_a", "left_finger_b",
            "right_arm", "right_forearm", "right_hand", "right_finger_a", "right_finger_b",
            "bow", "bow_upper", "bow_lower", "bow_grip", "bow_string", "arrow", "arrow_tip",
            "quiver", "quiver_strap", "quiver_arrow_a", "quiver_arrow_b",
            "tail", "tail_tuft",
            "front_left_leg", "front_left_hoof", "front_right_leg", "front_right_hoof",
            "back_left_leg", "back_left_hoof", "back_right_leg", "back_right_hoof",
        ],
        "required_tokens": [
            ".uv(0, 34).cuboid",  # long horse body
            ".uv(78, 34).cuboid",  # horse chest mass
            ".uv(36, 0).cuboid",  # forward human waist
            ".uv(28, 0).cuboid",  # human shoulders
            ".uv(90, 0).cuboid",  # nose bridge
            ".uv(104, 0).cuboid",  # left eye
            ".uv(110, 0).cuboid",  # right eye
            ".uv(104, 6).cuboid",  # mouth line
            ".uv(116, 0).cuboid",  # ear
            ".uv(90, 12).cuboid",  # hair crown
            ".uv(68, 8).cuboid",  # mane neck
            ".uv(100, 32).cuboid",  # bow upper limb
            ".uv(108, 32).cuboid",  # bow lower limb
            ".uv(116, 32).cuboid",  # bow grip
            ".uv(100, 50).cuboid",  # bow string
            ".uv(108, 50).cuboid",  # arrow shaft
            ".uv(108, 56).cuboid",  # arrow tip
            ".uv(100, 60).cuboid",  # quiver
            ".uv(96, 74).cuboid",  # quiver strap
            ".uv(118, 60).cuboid",  # quiver arrow a
            ".uv(120, 66).cuboid",  # quiver arrow b
            'addHorseLeg(root, "front_left_leg", "front_left_hoof"',
            'addHorseLeg(root, "front_right_leg", "front_right_hoof"',
            'addHorseLeg(root, "back_left_leg", "back_left_hoof"',
            'addHorseLeg(root, "back_right_leg", "back_right_hoof"',
            "this.bowString.pitch = -0.08F * draw",
            "this.arrowTip.visible = this.arrow.visible",
            "this.quiver.visible = true",
        ],
        "reason": "Centaur must read as a high-detail horse-bodied archer: long horse body, forward human warrior torso, explicit eyes/nose/mouth/ears/hair, bow with string and arrow, back quiver, tail, and four separate hooves",
    },
    {
        "id": "hydra",
        "model": "HydraModel.java",
        "design_status": "current_v2_rejected_v3_nine_head_design_not_approved",
        "required_parts": [
            "body", "shoulder_saddle", "spine_crest_a", "spine_crest_b", "spine_crest_c",
            "tail", "tail_mid", "tail_tip",
            *[
                part
                for index in range(1, 10)
                for part in (
                    f"head_{index:02d}_neck_socket",
                    f"head_{index:02d}_neck_root",
                    f"head_{index:02d}_neck_mid",
                    f"head_{index:02d}_neck_tip",
                    f"head_{index:02d}_core",
                    f"head_{index:02d}_upper_jaw",
                    f"head_{index:02d}_lower_jaw",
                    f"head_{index:02d}_left_eye",
                    f"head_{index:02d}_right_eye",
                    f"head_{index:02d}_left_fang",
                    f"head_{index:02d}_right_fang",
                    f"head_{index:02d}_left_poison_gland",
                    f"head_{index:02d}_right_poison_gland",
                )
            ],
            "front_left_leg", "front_right_leg", "back_left_leg", "back_right_leg",
        ],
        "required_tokens": [
            "private static final String[] NECK_NAMES",
            "private static final String[] HEAD_NAMES",
            "\"head_09",
            ".uv(0, 38).cuboid",  # low slender body
            ".uv(0, 76).cuboid",  # shoulder saddle under neck roots
            "_neck_socket",
            "_neck_root",
            "_neck_mid",
            "_neck_tip",
            ".uv(0, 94).cuboid",  # spine crest
            ".uv(82, 94).cuboid",  # tail tip taper
            "headName + \"_upper_jaw\"",
            "headName + \"_lower_jaw\"",
            "headName + \"_left_eye\"",
            "headName + \"_right_eye\"",
            "headName + \"_left_fang\"",
            "headName + \"_right_fang\"",
            "headName + \"_left_horn\"",
            "headName + \"_right_horn\"",
            "headName + \"_left_poison_gland\"",
            "headName + \"_right_poison_gland\"",
            "HEAD_PHASES",
        ],
        "reason": "Hydra current V2 five-head runtime model is rejected. V3 must expose nine independent socketed neck/head chains with root/mid/tip overlap, eyes, jaws, fangs, poison glands, spine crests, and a low slender body/tail before runtime model evidence can count for 1.0.",
    },
    {
        "id": "medusa",
        "model": "MedusaModel.java",
        "required_parts": [
            "torso", "robe_front", "waist_sash", "left_shoulder", "right_shoulder",
            "left_arm", "left_forearm", "left_hand", "right_arm", "right_forearm", "right_hand",
            "head", "face_plate", "left_eye", "right_eye", "left_pupil", "right_pupil",
            "stone_gaze_aura", "mouth", "nose_bridge", "left_ear", "right_ear",
            "snake_hair", "hair_crown", "hair_back",
            "front_snake_body", "front_snake_head", "front_snake_left_eye", "front_snake_right_eye",
            "front_snake_left_fang", "front_snake_right_fang", "front_snake_tongue",
            "left_snake_body", "left_snake_head", "left_snake_left_eye", "left_snake_right_eye",
            "left_snake_left_fang", "left_snake_right_fang", "left_snake_tongue",
            "right_snake_body", "right_snake_head", "right_snake_left_eye", "right_snake_right_eye",
            "right_snake_left_fang", "right_snake_right_fang", "right_snake_tongue",
            "back_snake_body", "back_snake_head", "back_snake_left_eye", "back_snake_right_eye",
            "back_snake_left_fang", "back_snake_right_fang", "back_snake_tongue",
            "left_outer_snake_body", "left_outer_snake_head", "left_outer_snake_left_eye", "left_outer_snake_right_eye",
            "left_outer_snake_left_fang", "left_outer_snake_right_fang", "left_outer_snake_tongue",
            "right_outer_snake_body", "right_outer_snake_head", "right_outer_snake_left_eye", "right_outer_snake_right_eye",
            "right_outer_snake_left_fang", "right_outer_snake_right_fang", "right_outer_snake_tongue",
            "coiled_tail", "tail_base", "tail_mid", "tail_coil_outer", "tail_coil_inner", "tail_tip", "tail_tip_spike",
        ],
        "required_tokens": [
            "private static final String[] SNAKE_NAMES",
            "private static final float[] SNAKE_PHASES",
            ".uv(104, 0).cuboid",  # stone gaze left eye
            ".uv(110, 0).cuboid",  # stone gaze right eye
            ".uv(118, 0).cuboid",  # pupil
            ".uv(104, 12).cuboid",  # stone gaze aura
            ".uv(100, 24).cuboid",  # snake tongue
            ".uv(94, 24).cuboid",  # snake fangs
            "addSnakeHair(snakeHair, \"front_snake\"",
            "addSnakeHair(snakeHair, \"left_snake\"",
            "addSnakeHair(snakeHair, \"right_snake\"",
            "addSnakeHair(snakeHair, \"back_snake\"",
            "addSnakeHair(snakeHair, \"left_outer_snake\"",
            "addSnakeHair(snakeHair, \"right_outer_snake\"",
            ".uv(56, 40).cuboid",  # outer coil
            ".uv(90, 40).cuboid",  # inner coil
            ".uv(16, 56).cuboid",  # tail spike
            "this.stoneGazeAura.visible = true",
            "this.snakeBodies[0].pitch -= cast * 0.12F",
        ],
        "reason": "Medusa must read as a high-detail petrifying gorgon: pale face with eyes/pupils/mouth/nose, visible gaze aura, six animated snake-hair bodies with heads/eyes/fangs/tongues, robe/arms, and a segmented coiled serpent tail",
    },
    {
        "id": "nemean_lion",
        "model": "NemeanLionModel.java",
        "required_parts": [
            "body", "shoulder", "back_armor_ridge",
            "head", "snout", "nose_mouth", "left_eye", "right_eye", "left_fang", "right_fang",
            "mane", "mane_core", "mane_top", "mane_bottom", "mane_left", "mane_right",
            "mane_outer_top", "mane_outer_left", "mane_outer_right", "mane_chin",
            "tail", "tail_tuft_core", "tail_tuft_top", "tail_tuft_bottom",
            "front_left_leg", "front_left_paw", "front_left_claws",
            "front_right_leg", "front_right_paw", "front_right_claws",
            "back_left_leg", "back_left_paw", "back_left_claws",
            "back_right_leg", "back_right_paw", "back_right_claws",
        ],
        "required_tokens": [
            ".uv(72, 90).cuboid",  # invulnerable back ridge
            ".uv(56, 0).cuboid",  # nose/mouth strip
            ".uv(108, 0).cuboid",  # left eye
            ".uv(114, 0).cuboid",  # right eye
            ".uv(108, 6).cuboid",  # left fang
            ".uv(114, 6).cuboid",  # right fang
            ".uv(0, 106).cuboid",  # outer top mane
            ".uv(24, 106).cuboid",  # outer left mane
            ".uv(42, 106).cuboid",  # outer right mane
            ".uv(60, 106).cuboid",  # chin mane
            ".uv(98, 54).cuboid",  # tail tuft core
            ".uv(98, 68).cuboid",  # tail tuft top
            ".uv(104, 74).cuboid",  # tail tuft bottom
            ".uv(92, 12).cuboid",  # front left claws
            ".uv(92, 18).mirrored().cuboid",  # front right claws
            ".uv(92, 24).cuboid",  # back left claws
            ".uv(92, 30).mirrored().cuboid",  # back right claws
            "resetManeLayers()",
            "resetPaws()",
            "this.frontLeftPaw.pitch -= 0.18F * pounce",
        ],
        "reason": "Nemean Lion must read as a mythic invulnerable lion: oversized radial mane layers, explicit face/fangs, heavy shoulder/chest/back ridge, four separate clawed paws, and layered tail tuft",
    },
    {
        "id": "manticore",
        "model": "ManticoreModel.java",
        "required_parts": [
            "body", "shoulder_ridge",
            "head", "snout", "nose_mouth", "left_eye", "right_eye",
            "left_fang", "right_fang", "left_ear", "right_ear",
            "left_cheek_spike", "right_cheek_spike",
            "left_wing", "left_wing_membrane_a", "left_wing_membrane_b",
            "left_wing_bone_front", "left_wing_bone_mid", "left_wing_bone_outer", "left_wing_claw",
            "right_wing", "right_wing_membrane_a", "right_wing_membrane_b",
            "right_wing_bone_front", "right_wing_bone_mid", "right_wing_bone_outer", "right_wing_claw",
            "tail", "tail_base", "tail_rise", "tail_curve", "tail_hook", "tail_sting", "tail_sting_tip",
            "front_left_leg", "front_left_claws", "front_right_leg", "front_right_claws",
            "back_left_leg", "back_left_claws", "back_right_leg", "back_right_claws",
        ],
        "required_tokens": [
            ".uv(52, 0).cuboid",  # nose/mouth strip
            ".uv(112, 0).cuboid",  # left eye
            ".uv(118, 0).cuboid",  # right eye
            ".uv(112, 6).cuboid",  # left fang
            ".uv(118, 6).cuboid",  # right fang
            ".uv(80, 0).cuboid",  # cheek spike
            ".uv(68, 0).cuboid",  # left wing membrane
            ".uv(0, 92).cuboid",  # left wing bone
            ".uv(58, 92).cuboid",  # left wing claw
            ".uv(68, 32).mirrored().cuboid",  # right wing membrane
            ".uv(0, 98).mirrored().cuboid",  # right wing bone
            ".uv(58, 98).mirrored().cuboid",  # right wing claw
            ".uv(70, 50).cuboid",  # tail base
            ".uv(106, 50).cuboid",  # tail hook
            ".uv(104, 58).cuboid",  # tail sting
            ".uv(116, 58).cuboid",  # tail sting tip
            ".uv(0, 78).cuboid",  # claw strip
            "this.tailHook.pitch -= swing",
            "this.tailSting.pitch = -1.25F",
        ],
        "reason": "Manticore must read as a high-detail lion-bodied monster with explicit face details, segmented membrane wings, clawed paws, and a raised scorpion tail hook/sting/tip instead of a generic winged quadruped",
    },
    {
        "id": "chimera",
        "model": "ChimeraModel.java",
        "required_parts": [
            "body", "shoulder_fire_crest",
            "lion_head", "lion_mane", "lion_snout", "lion_nose_mouth",
            "lion_left_eye", "lion_right_eye", "lion_left_fang", "lion_right_fang",
            "goat_neck", "goat_head", "goat_snout", "goat_left_eye", "goat_right_eye",
            "goat_left_horn", "goat_right_horn", "goat_beard",
            "tail", "tail_base", "tail_rise", "tail_tip",
            "serpent_head", "serpent_upper_jaw", "serpent_lower_jaw",
            "serpent_left_eye", "serpent_right_eye", "snake_tongue",
            "left_wing", "left_wing_membrane", "left_wing_bone", "left_wing_claw",
            "right_wing", "right_wing_membrane", "right_wing_bone", "right_wing_claw",
            "front_left_leg", "front_left_claws", "front_right_leg", "front_right_claws",
            "back_left_leg", "back_left_claws", "back_right_leg", "back_right_claws",
        ],
        "required_tokens": [
            ".uv(74, 44).cuboid",  # shoulder fire crest
            ".uv(104, 16).cuboid",  # lion nose/mouth strip
            ".uv(104, 0).cuboid",  # lion left eye
            ".uv(112, 0).cuboid",  # lion right eye
            ".uv(104, 5).cuboid",  # lion fang
            ".uv(88, 0).cuboid",  # goat horn
            ".uv(106, 20).cuboid",  # goat beard
            ".uv(80, 68).cuboid",  # tail base
            ".uv(116, 68).cuboid",  # tail tip
            ".uv(80, 80).cuboid",  # serpent head
            ".uv(106, 80).cuboid",  # serpent upper jaw
            ".uv(106, 86).cuboid",  # serpent lower jaw
            ".uv(122, 80).cuboid",  # serpent eye
            ".uv(112, 88).cuboid",  # snake tongue
            ".uv(0, 92).cuboid",  # left wing membrane
            ".uv(68, 92).cuboid",  # left wing bone
            ".uv(112, 94).cuboid",  # left wing claw
            ".uv(0, 110).mirrored().cuboid",  # right wing membrane
            ".uv(0, 86).cuboid",  # front claws
            "this.serpentLowerJaw.pitch += swing",
        ],
        "reason": "Chimera must stay a high-detail three-creature hybrid: lion face with eyes/nose/fangs/mane, horned goat side head with beard, tail-connected serpent head with jaws/eyes/tongue, layered wings, and clawed paws",
    },
    {
        "id": "colossus",
        "model": "ColossusModel.java",
        "required_parts": [
            "head", "face_mask", "eye_slit", "mouth_groove", "left_cheek_plate", "right_cheek_plate",
            "crown_band", "crown_center_spike", "crown_spike_left", "crown_spike_right",
            "body", "torso_core", "chest_plate_upper", "chest_plate_lower", "chest_center_gem",
            "left_chest_seam", "right_chest_seam", "back_plate", "bronze_belt",
            "left_shoulder_plate", "right_shoulder_plate",
            "left_arm", "left_elbow_ring", "left_forearm", "left_fist", "left_finger_a", "left_finger_b",
            "right_arm", "right_elbow_ring", "right_forearm", "right_fist", "right_finger_a", "right_finger_b",
            "left_leg", "left_knee_ring", "left_shin_plate", "left_foot", "left_toe_plate",
            "right_leg", "right_knee_ring", "right_shin_plate", "right_foot", "right_toe_plate",
        ],
        "required_tokens": [
            ".uv(40, 0).cuboid",  # face mask
            ".uv(60, 0).cuboid",  # glowing eye slit
            ".uv(72, 0).cuboid",  # mouth groove
            ".uv(28, 0).cuboid",  # center crown spike
            ".uv(34, 0).cuboid",  # side crown spike
            ".uv(0, 34).cuboid",  # upper chest plate
            ".uv(0, 48).cuboid",  # lower chest plate
            ".uv(54, 34).cuboid",  # center gem
            ".uv(74, 32).cuboid",  # back plate
            ".uv(30, upperV).mirrored(mirrored).cuboid",  # elbow ring helper
            ".uv(88, upperV).mirrored(mirrored).cuboid",  # fist helper
            ".uv(112, upperV).mirrored(mirrored).cuboid",  # fingers
            ".uv(kneeU, legV).mirrored(mirrored).cuboid",  # knee ring helper
            ".uv(footU, footV).mirrored(mirrored).cuboid",  # massive feet helper
            ".uv(toeU, 122).mirrored(mirrored).cuboid",  # toe plate helper
            "this.rightForearm.pitch = -0.35F * swing",
            "this.chestCenterGem.pitch = -0.05F * swing",
        ],
        "reason": "Colossus must read as a giant bronze statue, not a plain brown giant: face mask, glowing eye slit, mouth groove, crown band/spikes, layered chest plates, center gem, back plate, shoulder plates, elbow/knee rings, fists/fingers, massive feet and toe plates must be explicit runtime parts",
    },
    {
        "id": "pegasus",
        "model": "PegasusModel.java",
        "design_status": "current_v2_rejected_v3_horse_proportion_design_not_approved",
        "required_parts": [
            "body", "chest", "saddle_space", "left_shoulder_blend", "right_shoulder_blend",
            "neck", "mane", "mane_crest", "head", "snout", "nose_bridge",
            "left_cheek_shadow", "right_cheek_shadow", "nose_mouth",
            "left_nostril", "right_nostril", "left_ear", "right_ear", "left_eye", "right_eye",
            "left_wing", "left_wing_feathers", "right_wing", "right_wing_feathers",
            "tail", "tail_highlight",
            "front_left_leg", "front_right_leg", "back_left_leg", "back_right_leg",
        ],
        "required_tokens": [
            ".uv(112, 0).cuboid",  # left dark eye
            ".uv(118, 0).cuboid",  # right dark eye
            ".uv(104, 4).cuboid",  # left nostril
            ".uv(108, 4).cuboid",  # right nostril
            ".uv(78, 0).cuboid",  # nose bridge
            ".uv(112, 4).cuboid",  # nose/mouth
            ".uv(92, 16).cuboid",  # left ear
            ".uv(100, 16).cuboid",  # right ear
            ".uv(100, 0).cuboid",  # mane
            ".uv(110, 12).cuboid",  # mane crest
            ".uv(88, 36).cuboid",  # rider space
            ".uv(80, 64).cuboid",  # left wing coverts
            ".uv(80, 104).mirrored().cuboid",  # right wing coverts
            ".uv(48, 78).cuboid",  # fifth left feather
            ".uv(104, 78).mirrored().cuboid",  # fifth right feather
            ".uv(108, 52).cuboid",  # tail highlight
            "entity.isFlying()",
            "entity.hasPassengers()",
        ],
        "reason": "pure-white Pegasus must keep horse-proportion head/snout/nose with separate nostrils, ears, eyes, cheek planes, mane crest, rider space, layered wings with five feather slabs per side, tail detail, and flight/passenger animation hooks",
    },
]


def extract_model_names(source: str) -> set[str]:
    names = set(re.findall(r'"([a-z0-9_]+)"', source))

    # Some high-detail models generate repeated per-head/per-neck part names
    # from static string arrays plus suffixes. Expand those combinations so the
    # gate audits the actual runtime child names rather than only string
    # literals that appear whole in source.
    array_values: dict[str, list[str]] = {}
    for array_name in ("HEAD_NAMES", "NECK_NAMES", "SNAKE_NAMES"):
        match = re.search(
            rf"{array_name}\s*=\s*\{{(?P<body>.*?)\}}",
            source,
            flags=re.DOTALL,
        )
        if match:
            array_values[array_name] = re.findall(r'"([a-z0-9_]+)"', match.group("body"))

    if "headName" in source and "HEAD_NAMES" in array_values:
        suffixes = re.findall(r'headName\s*\+\s*"(_[a-z0-9_]+)"', source)
        for base in array_values["HEAD_NAMES"]:
            names.update(f"{base}{suffix}" for suffix in suffixes)

    if "neckName" in source and "NECK_NAMES" in array_values:
        suffixes = re.findall(r'neckName\s*\+\s*"(_[a-z0-9_]+)"', source)
        for base in array_values["NECK_NAMES"]:
            names.update(f"{base}{suffix}" for suffix in suffixes)

    if "SNAKE_NAMES" in array_values:
        suffixes = set(re.findall(r'snakeName\s*\+\s*"(_[a-z0-9_]+)"', source))
        suffixes.update(re.findall(r'name\s*\+\s*"(_[a-z0-9_]+)"', source))
        for base in array_values["SNAKE_NAMES"]:
            names.update(f"{base}{suffix}" for suffix in suffixes)

    return names


def audit() -> int:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    report: dict[str, Any] = {
        "ok": True,
        "scope": "greek_runtime_java_models",
        "creatures": [],
        "failures": [],
    }

    for contract in CONTRACTS:
        path = MODEL_DIR / contract["model"]
        item: dict[str, Any] = {
            "id": contract["id"],
            "model": str(path.relative_to(ROOT)),
            "reason": contract["reason"],
            "design_status": contract.get("design_status", "runtime_contract_current"),
            "required_parts": contract["required_parts"],
            "required_tokens": contract.get("required_tokens", []),
            "missing_parts": [],
            "missing_tokens": [],
            "ok": True,
        }
        if not path.exists():
            item["ok"] = False
            item["missing_parts"] = list(contract["required_parts"])
            item["missing_tokens"] = list(contract.get("required_tokens", []))
            report["failures"].append(f"{contract['id']}: missing model file {path}")
        else:
            source = path.read_text(encoding="utf-8")
            names = extract_model_names(source)
            missing = [part for part in contract["required_parts"] if part not in names]
            missing_tokens = [token for token in contract.get("required_tokens", []) if token not in source]
            item["runtime_part_names"] = sorted(names)
            item["missing_parts"] = missing
            item["missing_tokens"] = missing_tokens
            if missing or missing_tokens:
                item["ok"] = False
                for part in missing:
                    report["failures"].append(f"{contract['id']}: missing runtime model part {part}")
                for token in missing_tokens:
                    report["failures"].append(f"{contract['id']}: missing runtime model detail token {token}")
        report["creatures"].append(item)

    report["ok"] = not report["failures"]
    REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    if report["ok"]:
        print("Greek Runtime Model Quality Gate: PASS")
    else:
        print("Greek Runtime Model Quality Gate: FAIL")
        for failure in report["failures"]:
            print(f"- {failure}")
    print(f"report: {REPORT_PATH}")
    return 0 if report["ok"] else 1


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"], nargs="?", default="audit")
    parser.parse_args()
    return audit()


if __name__ == "__main__":
    sys.exit(main())
