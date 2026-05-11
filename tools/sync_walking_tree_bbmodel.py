#!/usr/bin/env python3
import json
import math
import uuid
from pathlib import Path


MODEL_PATH = Path(__file__).resolve().parents[1] / "docs/美术资源/blockbench/walking_tree.bbmodel"
GROUP_NS = uuid.UUID("68f54f86-cd72-42d3-8d7e-b2fd65f4c11e")


def deg(rad):
    return round(math.degrees(rad), 4)


def stable_uuid(path):
    return str(uuid.uuid5(GROUP_NS, path))


def main():
    data = json.loads(MODEL_PATH.read_text(encoding="utf-8"))
    elements = [
        element for element in data.get("elements", [])
        if element.get("name") not in {"right_root_leg", "left_root_leg"}
    ]
    data["elements"] = elements
    by_name = {element["name"]: element for element in elements}

    for name, element in by_name.items():
        if "uuid" not in element or not element["uuid"]:
            element["uuid"] = stable_uuid(f"element/{name}")

    attack_specs = {
        "left_attack_root": {
            "from": [-1.55, 0.0, -1.55],
            "to": [1.55, 28.0, 1.55],
            "origin": [-11.0, 15.5, -13.6],
            "rotation": [deg(-1.16), deg(-0.20), deg(1.00)],
            "uv_offset": [208, 112],
        },
        "right_attack_root": {
            "from": [-1.55, 0.0, -1.55],
            "to": [1.55, 28.0, 1.55],
            "origin": [11.0, 15.5, -13.6],
            "rotation": [deg(-1.16), deg(0.20), deg(-1.00)],
            "uv_offset": [208, 112],
        },
    }
    for name, spec in attack_specs.items():
        if name not in by_name:
            raise SystemExit(f"Missing element in bbmodel: {name}")
        by_name[name].update(spec)

    support_specs = {
        "front_outer_left_root": {
            "from": [-1.9, 0.0, -1.9],
            "to": [1.9, 26.0, 1.9],
            "origin": [-12.0, 15.2, -8.5],
            "rotation": [deg(-1.20), deg(-0.20), deg(0.78)],
        },
        "front_outer_right_root": {
            "from": [-1.9, 0.0, -1.9],
            "to": [1.9, 26.0, 1.9],
            "origin": [12.0, 15.2, -8.5],
            "rotation": [deg(-1.20), deg(0.20), deg(-0.78)],
        },
        "left_mid_root": {
            "from": [-1.7, 0.0, -1.7],
            "to": [1.7, 24.0, 1.7],
            "origin": [-13.5, 15.7, 0.0],
            "rotation": [deg(-0.85), 0.0, deg(1.18)],
        },
        "right_mid_root": {
            "from": [-1.7, 0.0, -1.7],
            "to": [1.7, 24.0, 1.7],
            "origin": [13.5, 15.7, 0.0],
            "rotation": [deg(-0.85), 0.0, deg(-1.18)],
        },
        "back_outer_left_root": {
            "from": [-1.6, 0.0, -1.6],
            "to": [1.6, 18.0, 1.6],
            "origin": [-8.0, 14.0, 10.5],
            "rotation": [deg(1.02), deg(0.14), deg(0.28)],
        },
        "back_outer_right_root": {
            "from": [-1.6, 0.0, -1.6],
            "to": [1.6, 18.0, 1.6],
            "origin": [8.0, 14.0, 10.5],
            "rotation": [deg(1.02), deg(-0.14), deg(-0.28)],
        },
    }
    for name, spec in support_specs.items():
        if name not in by_name:
            raise SystemExit(f"Missing element in bbmodel: {name}")
        by_name[name].update(spec)

    canopy_names = [
        "canopy_lower",
        "canopy_middle",
        "canopy_top",
        "canopy_under",
        "canopy_east_west",
        "canopy_north_south",
    ]
    trunk_names = ["trunk", "trunk_base"]
    attack_root_names = ["left_attack_root", "right_attack_root"]
    root_collar_names = [
        "root_flare",
        "root_crown",
        "inner_root_bundle",
        "front_left_buttress_root",
        "front_right_buttress_root",
        "left_buttress_root",
        "right_buttress_root",
        "back_left_buttress_root",
        "back_right_buttress_root",
        "front_left_bridge_root",
        "front_right_bridge_root",
        "rear_left_bridge_root",
        "rear_right_bridge_root",
        "front_outer_left_root",
        "front_inner_left_root",
        "front_inner_right_root",
        "front_outer_right_root",
        "left_front_root",
        "left_mid_root",
        "left_back_root",
        "right_front_root",
        "right_mid_root",
        "right_back_root",
        "back_outer_left_root",
        "back_center_root",
        "back_outer_right_root",
        "front_secondary_left",
        "front_secondary_right",
        "left_secondary_root",
        "right_secondary_root",
        "back_secondary_left",
        "back_secondary_right",
        "left_hanging_root",
        "right_hanging_root",
        "front_hanging_root",
        "back_hanging_root",
    ]

    def element_uuid(name):
        if name not in by_name:
            raise SystemExit(f"Missing required element in bbmodel: {name}")
        return by_name[name]["uuid"]

    data["outliner"] = [
        {
            "name": "walking_tree",
            "origin": [0, 0, 0],
            "color": 0,
            "uuid": stable_uuid("walking_tree"),
            "children": [
                {
                    "name": "canopy",
                    "origin": [0, 0, 0],
                    "color": 0,
                    "uuid": stable_uuid("walking_tree/canopy"),
                    "children": [*[element_uuid(name) for name in canopy_names]],
                },
                {
                    "name": "trunk_group",
                    "origin": [0, 0, 0],
                    "color": 0,
                    "uuid": stable_uuid("walking_tree/trunk_group"),
                    "children": [*[element_uuid(name) for name in trunk_names]],
                },
                {
                    "name": "root_collar",
                    "origin": [0, 0, 0],
                    "color": 0,
                    "uuid": stable_uuid("walking_tree/root_collar"),
                    "children": [
                        *[element_uuid(name) for name in root_collar_names],
                        {
                            "name": "attack_roots",
                            "origin": [0, 0, 0],
                            "color": 0,
                            "uuid": stable_uuid("walking_tree/root_collar/attack_roots"),
                            "children": [element_uuid(name) for name in attack_root_names],
                        },
                    ],
                },
            ],
        }
    ]

    MODEL_PATH.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Synchronized {MODEL_PATH}")


if __name__ == "__main__":
    main()
