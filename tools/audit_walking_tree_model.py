#!/usr/bin/env python3
import json
import math
import re
import sys
from pathlib import Path


DEFAULT_MODEL = Path(__file__).resolve().parents[1] / "docs/美术资源/blockbench/walking_tree.bbmodel"
MODEL_JAVA = Path(__file__).resolve().parents[1] / "src/main/java/com/example/mythic_age/client/render/entity/model/WalkingTreeModel.java"
ENTITY_JAVA = Path(__file__).resolve().parents[1] / "src/main/java/com/example/mythic_age/entity/WalkingTreeEntity.java"
SUPPORT_ROOT_NAMES = [
    "front_outer_left_root",
    "front_outer_right_root",
    "left_mid_root",
    "right_mid_root",
    "back_outer_left_root",
    "back_outer_right_root",
]
FRONT_LAYER_NAMES = [
    "front_inner_left_root",
    "front_inner_right_root",
    "front_secondary_left",
    "front_secondary_right",
]
SUPPORT_FIELD_NAMES = [
    "frontLeftSupportRoot",
    "frontRightSupportRoot",
    "leftMidSupportRoot",
    "rightMidSupportRoot",
    "backLeftSupportRoot",
    "backRightSupportRoot",
]
SECONDARY_ROOT_NAMES = [
    "front_secondary_left",
    "front_secondary_right",
    "left_secondary_root",
    "right_secondary_root",
    "back_secondary_left",
    "back_secondary_right",
]


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return ((1, 0, 0), (0, c, -s), (0, s, c))


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return ((c, 0, s), (0, 1, 0), (-s, 0, c))


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return ((c, -s, 0), (s, c, 0), (0, 0, 1))


def mul(a, b):
    return tuple(
        tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3))
        for i in range(3)
    )


def apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def box_vertices(frm, to):
    for x in (frm[0], to[0]):
        for y in (frm[1], to[1]):
            for z in (frm[2], to[2]):
                yield [x, y, z]


def world_matrix(elem):
    rot = elem.get("rotation")
    if not rot:
        return ((1, 0, 0), (0, 1, 0), (0, 0, 1))
    pitch, yaw, roll = [math.radians(v) for v in rot]
    return mul(rot_y(yaw), mul(rot_x(pitch), rot_z(roll)))


def world_aabb(elem):
    m = world_matrix(elem)
    origin = elem.get("origin", [0, 0, 0])
    mins = [10 ** 9, 10 ** 9, 10 ** 9]
    maxs = [-10 ** 9, -10 ** 9, -10 ** 9]
    for v in box_vertices(elem["from"], elem["to"]):
        t = apply(m, v)
        w = [t[0] + origin[0], t[1] + origin[1], t[2] + origin[2]]
        for i in range(3):
            mins[i] = min(mins[i], w[i])
            maxs[i] = max(maxs[i], w[i])
    return mins, maxs


def uv_overflow(elem, tex_w, tex_h):
    uv = elem.get("uv_offset")
    if not uv:
        return False
    frm, to = elem["from"], elem["to"]
    sx = abs(to[0] - frm[0])
    sy = abs(to[1] - frm[1])
    sz = abs(to[2] - frm[2])
    return uv[0] + (2 * sz) + (2 * sx) > tex_w or uv[1] + sz + sy > tex_h


def tip_position(elem):
    frm, to = elem["from"], elem["to"]
    origin = elem.get("origin", [0, 0, 0])
    m = world_matrix(elem)
    tip = apply(m, [0, to[1], 0])
    return [tip[0] + origin[0], tip[1] + origin[1], tip[2] + origin[2]]


def radial(v):
    return math.hypot(v[0], v[2])


def aabb_distance(a, b):
    (amin, amax), (bmin, bmax) = a, b
    sq = 0.0
    for i in range(3):
        if amax[i] < bmin[i]:
            d = bmin[i] - amax[i]
        elif bmax[i] < amin[i]:
            d = amin[i] - bmax[i]
        else:
            d = 0.0
        sq += d * d
    return math.sqrt(sq)


def build_connectivity(names, aabbs, threshold):
    graph = {name: set() for name in names}
    name_list = list(names)
    for i, name_a in enumerate(name_list):
        for name_b in name_list[i + 1:]:
            if aabb_distance(aabbs[name_a], aabbs[name_b]) <= threshold:
                graph[name_a].add(name_b)
                graph[name_b].add(name_a)
    return graph


def reachable(graph, start):
    seen = set(start)
    stack = list(start)
    while stack:
        name = stack.pop()
        for other in graph[name]:
            if other not in seen:
                seen.add(other)
                stack.append(other)
    return seen


def walk_outliner(node, path, by_uuid, element_paths):
    if isinstance(node, str):
        if node in by_uuid:
            element_paths[by_uuid[node]["name"]] = list(path)
        return
    if not isinstance(node, dict):
        return
    next_path = path + [node.get("name", "")]
    for child in node.get("children", []):
        walk_outliner(child, next_path, by_uuid, element_paths)


def main():
    model_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_MODEL
    data = json.loads(model_path.read_text(encoding="utf-8"))
    java_text = MODEL_JAVA.read_text(encoding="utf-8")
    entity_text = ENTITY_JAVA.read_text(encoding="utf-8")
    elements = data.get("elements", [])
    by_name = {e["name"]: e for e in elements}
    by_uuid = {e["uuid"]: e for e in elements if "uuid" in e}
    tex_w = data["resolution"]["width"]
    tex_h = data["resolution"]["height"]
    element_paths = {}
    for node in data.get("outliner", []):
        walk_outliner(node, [], by_uuid, element_paths)

    overflow = [e["name"] for e in elements if uv_overflow(e, tex_w, tex_h)]

    trunk_names = [name for name in ("trunk", "trunk_base") if name in by_name]
    trunk_widths = []
    for name in trunk_names:
        e = by_name[name]
        trunk_widths.append(abs(e["to"][0] - e["from"][0]))
    trunk_aabb = world_aabb(by_name["trunk"]) if "trunk" in by_name else None
    trunk_top_y = trunk_aabb[0][1] if trunk_aabb else None
    root_collar_aabb = world_aabb(by_name["root_collar"]) if "root_collar" in by_name else None

    buttress = [e for e in elements if "buttress_root" in e["name"]]
    hanging = [e for e in elements if "hanging_root" in e["name"]]
    attack_roots = [e for e in elements if "attack_root" in e["name"]]
    attack_lengths = [abs(e["to"][1] - e["from"][1]) for e in attack_roots]
    attack_origins = [e.get("origin", [0, 0, 0]) for e in attack_roots]
    attack_aabbs = [world_aabb(e) for e in attack_roots]
    attack_top_y = [aabb[0][1] for aabb in attack_aabbs]
    attack_tip_y = [tip_position(e)[1] for e in attack_roots]
    attack_tips = [tip_position(e) for e in attack_roots]
    attack_paths = [element_paths.get(e["name"], []) for e in attack_roots]
    attack_x_ranges = [(aabb[0][0], aabb[1][0]) for aabb in attack_aabbs]
    attack_y_ranges = [(aabb[0][1], aabb[1][1]) for aabb in attack_aabbs]
    attack_z_ranges = [(aabb[0][2], aabb[1][2]) for aabb in attack_aabbs]
    support_roots = [by_name[name] for name in SUPPORT_ROOT_NAMES if name in by_name]
    secondary_roots = [by_name[name] for name in SECONDARY_ROOT_NAMES if name in by_name]
    front_layer_roots = [by_name[name] for name in FRONT_LAYER_NAMES if name in by_name]
    front_layer_aabbs = [world_aabb(root) for root in front_layer_roots]
    excluded = {"root_flare", "root_crown", "inner_root_bundle"}
    main_roots = [
        e for e in elements
        if e["name"].endswith("_root")
        and e["name"] not in excluded
        and "hanging" not in e["name"]
        and "attack_root" not in e["name"]
    ]

    outward = []
    for e in main_roots:
        origin = e.get("origin", [0, 0, 0])
        tip = tip_position(e)
        outward.append((e["name"], radial(tip) > radial(origin)))

    main_tip_y = [tip_position(e)[1] for e in main_roots]
    hanging_tip_y = [tip_position(e)[1] for e in hanging]
    support_tip_y = [tip_position(e)[1] for e in support_roots]
    secondary_tip_y = [tip_position(e)[1] for e in secondary_roots]
    support_outward = []
    for e in support_roots:
        origin = e.get("origin", [0, 0, 0])
        tip = tip_position(e)
        support_outward.append((e["name"], radial(tip) > radial(origin)))
    strict_support_disconnected = []

    mins = [10 ** 9, 10 ** 9, 10 ** 9]
    maxs = [-10 ** 9, -10 ** 9, -10 ** 9]
    rootish = [e for e in elements if "root" in e["name"]]
    for e in rootish:
        m = world_matrix(e)
        origin = e.get("origin", [0, 0, 0])
        for v in box_vertices(e["from"], e["to"]):
            t = apply(m, v)
            w = [t[0] + origin[0], t[1] + origin[1], t[2] + origin[2]]
            for i in range(3):
                mins[i] = min(mins[i], w[i])
                maxs[i] = max(maxs[i], w[i])

    connectivity_names = [
        e["name"] for e in elements
        if "root" in e["name"] or e["name"] in {"trunk", "trunk_base", "canopy_lower", "canopy_middle", "canopy_top", "canopy_under", "canopy_east_west", "canopy_north_south"}
    ]
    aabbs = {name: world_aabb(by_name[name]) for name in connectivity_names}
    start = {
        name for name in ("trunk", "trunk_base", "root_flare", "root_crown", "inner_root_bundle", "canopy_lower", "canopy_middle", "canopy_top", "canopy_under", "canopy_east_west", "canopy_north_south")
        if name in aabbs
    }
    strict_graph = build_connectivity(connectivity_names, aabbs, threshold=0.5)
    loose_graph = build_connectivity(connectivity_names, aabbs, threshold=1.0)
    strict_seen = reachable(strict_graph, start)
    loose_seen = reachable(loose_graph, start)
    disconnected_strict = [
        name for name in connectivity_names
        if name not in strict_seen and not name.endswith("_leg")
    ]
    disconnected_loose = [
        name for name in connectivity_names
        if name not in loose_seen and not name.endswith("_leg")
    ]
    strict_support_disconnected = [name for name in SUPPORT_ROOT_NAMES if name in by_name and name not in strict_seen]

    animated_support_fields = []
    for field in SUPPORT_FIELD_NAMES:
        if (
            f"this.{field}.pitch" in java_text
            or f"this.{field}.yaw" in java_text
            or f"this.{field}.roll" in java_text
        ):
            animated_support_fields.append(field)

    def read_java_constant(name):
        match = re.search(rf"{name}\s*=\s*(-?\d+(?:\.\d+)?)F", java_text)
        return float(match.group(1)) if match else None

    def read_entity_int_constant(name):
        match = re.search(rf"{name}\s*=\s*(\d+)", entity_text)
        return int(match.group(1)) if match else None

    attack_swing_pitch = read_java_constant("ATTACK_ROOT_SWING_PITCH")
    attack_swing_yaw = read_java_constant("ATTACK_ROOT_SWING_YAW")
    attack_swing_roll = read_java_constant("ATTACK_ROOT_SWING_ROLL")
    attack_animation_duration = read_entity_int_constant("ATTACK_ANIMATION_DURATION")

    logic_failures = []
    if overflow:
        logic_failures.append(f"UV overflow: {', '.join(overflow)}")
    if not trunk_widths or max(trunk_widths) < 10.0:
        logic_failures.append("Trunk width too thin (< 10)")
    if len(buttress) < 4:
        logic_failures.append("Missing buttress roots connecting trunk to outer roots")
    if any(name in by_name for name in ("right_root_leg", "left_root_leg")):
        logic_failures.append("Hidden root-leg helpers are still present in bbmodel")
    if len(support_roots) < 6:
        logic_failures.append("Missing major visible support roots for multi-root support logic")
    if len(animated_support_fields) < 4:
        logic_failures.append("WalkingTreeModel does not animate enough visible support roots")
    if strict_support_disconnected:
        logic_failures.append("Support roots not strictly connected: " + ", ".join(strict_support_disconnected))
    if any(not ok for _, ok in support_outward):
        inward_support = [name for name, ok in support_outward if not ok]
        logic_failures.append("Support roots not facing outward: " + ", ".join(inward_support))
    if support_tip_y and secondary_tip_y:
        if sum(y >= max(secondary_tip_y) + 1.0 for y in support_tip_y) < 4:
            logic_failures.append("Visible support roots are not clearly lower than the secondary root layer")
        if (sum(support_tip_y) / len(support_tip_y)) <= (sum(secondary_tip_y) / len(secondary_tip_y)) + 1.8:
            logic_failures.append("Average support-root height is too close to the secondary root layer")
    if len(attack_roots) < 2:
        logic_failures.append("Missing attack roots for attack animation")
    if attack_lengths and min(attack_lengths) < 24.0:
        logic_failures.append("Attack roots are too short to stay visible beyond the front root layer")
    if attack_swing_pitch is None or attack_swing_pitch < 0.7:
        logic_failures.append("Attack root pitch swing is too small for a clear top-down strike")
    if attack_swing_roll is None or attack_swing_roll < 0.75:
        logic_failures.append("Attack root roll swing is too small for a readable downward slam")
    if attack_swing_yaw is None or attack_swing_yaw < 0.12:
        logic_failures.append("Attack root yaw swing is too small for a visible outward-to-inward attack")
    if attack_swing_pitch is None or attack_swing_roll is None or attack_swing_pitch + attack_swing_roll < 1.5:
        logic_failures.append("Combined attack swing amplitude is too small for a dramatic top-down strike")
    if attack_animation_duration is None or attack_animation_duration < 28:
        logic_failures.append("Attack animation duration is too short to read in game")
    if "raisedAttackPose" not in java_text or "strikeAttackPose" not in java_text:
        logic_failures.append("Attack animation is not split into visible lift and strike phases")
    if any(e["to"][1] <= e["from"][1] for e in attack_roots):
        logic_failures.append("Attack roots are not modeled as downward-growing geometry")
    if any(tip_y <= origin[1] + 5.0 for tip_y, origin in zip(attack_tip_y, attack_origins)):
        logic_failures.append("Attack root tips do not extend far enough below their attachment points")
    if attack_paths and any("attack_roots" not in path or "root_collar" not in path for path in attack_paths):
        logic_failures.append("Attack roots are not nested under root_collar/attack_roots in bbmodel outliner")
    if root_collar_aabb is not None and attack_origins:
        collar_min_y, collar_max_y = root_collar_aabb[0][1], root_collar_aabb[1][1]
        if any(origin[1] < collar_min_y - 1.0 or origin[1] > collar_max_y + 2.0 for origin in attack_origins):
            logic_failures.append("Attack root origins are not anchored inside the lower front root band")
    if any(origin[2] >= -3.0 for origin in attack_origins):
        logic_failures.append("Attack root origins are not in the front root zone")
    if any(tip[2] >= origin[2] - 2.0 for tip, origin in zip(attack_tips, attack_origins)):
        logic_failures.append("Attack root tips do not extend forward beyond their root origin")
    if front_layer_aabbs and attack_aabbs:
        front_layer_max_y = max(aabb[1][1] for aabb in front_layer_aabbs)
        front_layer_min_z = min(aabb[0][2] for aabb in front_layer_aabbs)
        left_front_secondary = by_name.get("front_secondary_left")
        right_front_secondary = by_name.get("front_secondary_right")
        if any(ymax <= front_layer_max_y + 1.0 for _, ymax in attack_y_ranges):
            logic_failures.append("Attack roots do not rise clearly above the front root layer")
        if any(zmin >= front_layer_min_z - 3.0 for zmin, _ in attack_z_ranges):
            logic_failures.append("Attack roots do not project far enough in front of the front root layer")
        if left_front_secondary:
            left_secondary_aabb = world_aabb(left_front_secondary)
            left_attack = by_name.get("left_attack_root")
            if left_attack:
                left_attack_aabb = world_aabb(left_attack)
                if left_attack_aabb[1][0] >= left_secondary_aabb[1][0] - 0.25:
                    logic_failures.append("Left attack root is still buried laterally inside the left front root cluster")
        if right_front_secondary:
            right_secondary_aabb = world_aabb(right_front_secondary)
            right_attack = by_name.get("right_attack_root")
            if right_attack:
                right_attack_aabb = world_aabb(right_attack)
                if right_attack_aabb[0][0] <= right_secondary_aabb[0][0] + 0.25:
                    logic_failures.append("Right attack root is still buried laterally inside the right front root cluster")
    if trunk_top_y is not None and attack_top_y:
        if any(top_y < trunk_top_y - 1.0 for top_y in attack_top_y):
            logic_failures.append("Attack root geometry rises above the trunk top")
    if any(not ok for _, ok in outward):
        inward = [name for name, ok in outward if not ok]
        logic_failures.append("Inward-facing roots: " + ", ".join(inward))
    if main_tip_y and hanging_tip_y and min(hanging_tip_y) <= min(main_tip_y):
        logic_failures.append("Hanging roots are not below the main root layer")
    if disconnected_strict:
        logic_failures.append(
            "Strict connectivity gaps (<=0.5 block contact required): " + ", ".join(disconnected_strict)
        )

    print("WalkingTree Model Audit")
    print(f"model: {model_path}")
    print(f"uv_overflow: {len(overflow)}")
    print(f"trunk_widths: {trunk_widths}")
    print(f"buttress_root_count: {len(buttress)}")
    print(f"support_root_count: {len(support_roots)}")
    print(f"attack_root_count: {len(attack_roots)}")
    if attack_lengths:
        print(f"attack_root_lengths: {[round(x, 2) for x in attack_lengths]}")
    print(f"attack_root_swing_pitch: {attack_swing_pitch}")
    print(f"attack_root_swing_yaw: {attack_swing_yaw}")
    print(f"attack_root_swing_roll: {attack_swing_roll}")
    print(f"attack_animation_duration: {attack_animation_duration}")
    if attack_origins:
        print(f"attack_root_origins_y: {[round(origin[1], 2) for origin in attack_origins]}")
        print(f"attack_root_origins_z: {[round(origin[2], 2) for origin in attack_origins]}")
    if attack_tip_y:
        print(f"attack_root_tip_y: {[round(y, 2) for y in attack_tip_y]}")
    if attack_x_ranges:
        print(f"attack_root_x_ranges: {[(round(x0, 2), round(x1, 2)) for x0, x1 in attack_x_ranges]}")
    if attack_y_ranges:
        print(f"attack_root_y_ranges: {[(round(y0, 2), round(y1, 2)) for y0, y1 in attack_y_ranges]}")
    if attack_z_ranges:
        print(f"attack_root_z_ranges: {[(round(z0, 2), round(z1, 2)) for z0, z1 in attack_z_ranges]}")
    if trunk_top_y is not None:
        print(f"trunk_top_y: {round(trunk_top_y, 2)}")
    if attack_top_y:
        print(f"attack_root_top_y: {[round(y, 2) for y in attack_top_y]}")
    if attack_paths:
        print(f"attack_root_paths: {attack_paths}")
    if support_roots:
        print(f"support_root_names: {[root['name'] for root in support_roots]}")
    print(f"animated_support_root_fields: {animated_support_fields}")
    print(f"support_root_outward: {sum(1 for _, ok in support_outward if ok)}/{len(support_outward)}")
    print(f"main_root_outward: {sum(1 for _, ok in outward if ok)}/{len(outward)}")
    print(f"strictly_connected_parts: {len(strict_seen)}/{len(connectivity_names)}")
    print(f"loosely_connected_parts: {len(loose_seen)}/{len(connectivity_names)}")
    if strict_support_disconnected:
        print("strict_disconnected_support_roots:")
        for name in strict_support_disconnected:
            print(f"- {name}")
    if disconnected_strict:
        print("strict_disconnected_parts:")
        for name in disconnected_strict:
            print(f"- {name}")
    if disconnected_loose:
        print("loose_disconnected_parts:")
        for name in disconnected_loose:
            print(f"- {name}")
    if main_tip_y:
        print(f"main_root_tip_y: {min(main_tip_y):.2f}..{max(main_tip_y):.2f}")
    if support_tip_y:
        print(f"support_root_tip_y: {min(support_tip_y):.2f}..{max(support_tip_y):.2f}")
    if secondary_tip_y:
        print(f"secondary_root_tip_y: {min(secondary_tip_y):.2f}..{max(secondary_tip_y):.2f}")
    if hanging_tip_y:
        print(f"hanging_root_tip_y: {min(hanging_tip_y):.2f}..{max(hanging_tip_y):.2f}")
    print(
        "root_bounds: "
        f"{[round(x, 2) for x in mins]} -> {[round(x, 2) for x in maxs]} "
        f"size={[round(maxs[i] - mins[i], 2) for i in range(3)]}"
    )
    if logic_failures:
        print("logic_status: FAIL")
        for item in logic_failures:
            print(f"- {item}")
        sys.exit(1)

    print("logic_status: PASS")


if __name__ == "__main__":
    main()
