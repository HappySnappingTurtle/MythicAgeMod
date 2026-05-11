#!/usr/bin/env python3
import json
import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODEL_PATH = ROOT / "docs/美术资源/blockbench/walking_tree.bbmodel"
PREVIEW_DIR = ROOT / "docs/preview_assets/walking_tree"
OUTPUT_PATH = PREVIEW_DIR / "walking_tree_preview.svg"
FOCUS_OUTPUT_PATH = PREVIEW_DIR / "walking_tree_root_focus.svg"
SUPPORT_ROOTS = {
    "front_outer_left_root",
    "front_outer_right_root",
    "left_mid_root",
    "right_mid_root",
    "back_outer_left_root",
    "back_outer_right_root",
}


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


def world_vertices(elem):
    m = world_matrix(elem)
    origin = elem.get("origin", [0, 0, 0])
    vertices = []
    for v in box_vertices(elem["from"], elem["to"]):
        t = apply(m, v)
        vertices.append([t[0] + origin[0], t[1] + origin[1], t[2] + origin[2]])
    return vertices


def classify(name):
    if name.startswith("canopy") or name == "canopy_under":
        return ("#3a8b3a", 0.55, "#215321", 0)
    if "attack_root" in name:
        return ("#c04c2d", 0.96, "#7a2714", 3)
    if name in SUPPORT_ROOTS:
        return ("#5b3519", 0.96, "#2f170c", 2)
    if "root" in name:
        return ("#8b633d", 0.58, "#5a3d24", 1)
    if "trunk" in name:
        return ("#7d522b", 0.9, "#4a2f19", 1)
    return ("#999999", 0.6, "#666666", 0)


def front_bbox(vertices):
    xs = [v[0] for v in vertices]
    ys = [v[1] for v in vertices]
    zs = [v[2] for v in vertices]
    return min(xs), max(xs), min(ys), max(ys), sum(zs) / len(zs)


def side_bbox(vertices):
    zs = [v[2] for v in vertices]
    ys = [v[1] for v in vertices]
    xs = [v[0] for v in vertices]
    return min(zs), max(zs), min(ys), max(ys), sum(xs) / len(xs)


def iso_points(vertices):
    pts = []
    for x, y, z in vertices:
        pts.append(((x - z) * 0.85, y + (x + z) * 0.24))
    return pts


def convex_hull(points):
    pts = sorted(set(points))
    if len(pts) <= 1:
        return pts

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

    lower = []
    for p in pts:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], p) <= 0:
            lower.pop()
        lower.append(p)

    upper = []
    for p in reversed(pts):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], p) <= 0:
            upper.pop()
        upper.append(p)

    return lower[:-1] + upper[:-1]


def main():
    data = json.loads(MODEL_PATH.read_text(encoding="utf-8"))
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
    elements = data["elements"]

    front_items = []
    iso_items = []
    for elem in elements:
        verts = world_vertices(elem)
        xmin, xmax, ymin, ymax, depth = front_bbox(verts)
        fill, opacity, stroke, priority = classify(elem["name"])
        front_items.append((priority, depth, xmin, xmax, ymin, ymax, fill, opacity, stroke, elem["name"]))
        iso = convex_hull([(round(x, 3), round(y, 3)) for x, y in iso_points(verts)])
        iso_depth = sum(v[2] for v in verts) / len(verts)
        iso_items.append((priority, iso_depth, iso, fill, opacity, stroke, elem["name"]))

    front_items.sort(key=lambda item: (item[0], item[1]))
    iso_items.sort(key=lambda item: (item[0], item[1]))

    width = 1750
    height = 760
    front_origin = (270, 640)
    side_origin = (850, 640)
    iso_origin = (1440, 620)
    front_scale = 9.2
    side_scale = 9.2
    iso_scale = 7.4

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
        '<rect width="100%" height="100%" fill="#f3efe7"/>',
        '<text x="80" y="70" font-size="32" font-family="Arial, sans-serif" fill="#2b2218">WalkingTree Model Preview</text>',
        '<text x="80" y="110" font-size="20" font-family="Arial, sans-serif" fill="#5c4b3a">Front</text>',
        '<text x="650" y="110" font-size="20" font-family="Arial, sans-serif" fill="#5c4b3a">Side</text>',
        '<text x="1260" y="110" font-size="20" font-family="Arial, sans-serif" fill="#5c4b3a">45°</text>',
        '<line x1="50" y1="645" x2="520" y2="645" stroke="#b9ad9d" stroke-width="2"/>',
        '<line x1="610" y1="645" x2="1080" y2="645" stroke="#b9ad9d" stroke-width="2"/>',
        '<line x1="1170" y1="645" x2="1700" y2="645" stroke="#b9ad9d" stroke-width="2"/>',
    ]

    for priority, _, xmin, xmax, ymin, ymax, fill, opacity, stroke, name in front_items:
        x = front_origin[0] + xmin * front_scale
        y = front_origin[1] + ymin * front_scale
        w = (xmax - xmin) * front_scale
        h = (ymax - ymin) * front_scale
        stroke_width = 2.4 if priority >= 2 else 1.2
        parts.append(
            f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" '
            f'fill="{fill}" fill-opacity="{opacity}" stroke="{stroke}" stroke-width="{stroke_width}"/>'
        )

    side_items = []
    for elem in elements:
        verts = world_vertices(elem)
        zmin, zmax, ymin, ymax, depth = side_bbox(verts)
        fill, opacity, stroke, priority = classify(elem["name"])
        side_items.append((priority, depth, zmin, zmax, ymin, ymax, fill, opacity, stroke, elem["name"]))

    side_items.sort(key=lambda item: (item[0], item[1]))
    for priority, _, zmin, zmax, ymin, ymax, fill, opacity, stroke, name in side_items:
        x = side_origin[0] + zmin * side_scale
        y = side_origin[1] + ymin * side_scale
        w = (zmax - zmin) * side_scale
        h = (ymax - ymin) * side_scale
        stroke_width = 2.4 if priority >= 2 else 1.2
        parts.append(
            f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" '
            f'fill="{fill}" fill-opacity="{opacity}" stroke="{stroke}" stroke-width="{stroke_width}"/>'
        )

    for priority, _, poly, fill, opacity, stroke, name in iso_items:
        if not poly:
            continue
        coords = []
        for x, y in poly:
            sx = iso_origin[0] + x * iso_scale
            sy = iso_origin[1] + y * iso_scale
            coords.append(f"{sx:.2f},{sy:.2f}")
        stroke_width = 2.2 if priority >= 2 else 1.1
        parts.append(
            f'<polygon points="{" ".join(coords)}" fill="{fill}" fill-opacity="{opacity}" '
            f'stroke="{stroke}" stroke-width="{stroke_width}"/>'
        )

    parts.extend([
        '<rect x="80" y="665" width="18" height="18" fill="#3a8b3a" fill-opacity="0.55" stroke="#215321"/>',
        '<text x="108" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">canopy</text>',
        '<rect x="200" y="665" width="18" height="18" fill="#7d522b" fill-opacity="0.9" stroke="#4a2f19"/>',
        '<text x="228" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">trunk</text>',
        '<rect x="300" y="665" width="18" height="18" fill="#5b3519" fill-opacity="0.96" stroke="#2f170c"/>',
        '<text x="328" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">support roots</text>',
        '<rect x="450" y="665" width="18" height="18" fill="#8b633d" fill-opacity="0.58" stroke="#5a3d24"/>',
        '<text x="478" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">secondary roots</text>',
        '<rect x="630" y="665" width="18" height="18" fill="#c04c2d" fill-opacity="0.96" stroke="#7a2714"/>',
        '<text x="658" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">attack roots</text>',
        '</svg>',
    ])

    OUTPUT_PATH.write_text("\n".join(parts), encoding="utf-8")
    focus_width = 900
    focus_height = 780
    focus_origin = (430, 260)
    focus_scale = 15.0
    focus_parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{focus_width}" height="{focus_height}" viewBox="0 0 {focus_width} {focus_height}">',
        '<rect width="100%" height="100%" fill="#f3efe7"/>',
        '<text x="70" y="70" font-size="32" font-family="Arial, sans-serif" fill="#2b2218">WalkingTree Root Logic Preview</text>',
        '<text x="70" y="112" font-size="20" font-family="Arial, sans-serif" fill="#5c4b3a">Front Lower-Body Close-up</text>',
        '<line x1="60" y1="635" x2="840" y2="635" stroke="#b9ad9d" stroke-width="2"/>',
    ]

    for priority, _, xmin, xmax, ymin, ymax, fill, opacity, stroke, name in front_items:
        if ymax < 2.0:
            continue
        x = focus_origin[0] + xmin * focus_scale
        y = focus_origin[1] + ymin * focus_scale
        w = (xmax - xmin) * focus_scale
        h = (ymax - ymin) * focus_scale
        stroke_width = 2.8 if priority >= 2 else 1.4
        focus_parts.append(
            f'<rect x="{x:.2f}" y="{y:.2f}" width="{w:.2f}" height="{h:.2f}" '
            f'fill="{fill}" fill-opacity="{opacity}" stroke="{stroke}" stroke-width="{stroke_width}"/>'
        )

    focus_parts.extend([
        '<rect x="70" y="665" width="18" height="18" fill="#5b3519" fill-opacity="0.96" stroke="#2f170c"/>',
        '<text x="98" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">support roots</text>',
        '<rect x="220" y="665" width="18" height="18" fill="#8b633d" fill-opacity="0.58" stroke="#5a3d24"/>',
        '<text x="248" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">secondary roots</text>',
        '<rect x="410" y="665" width="18" height="18" fill="#c04c2d" fill-opacity="0.96" stroke="#7a2714"/>',
        '<text x="438" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">attack roots</text>',
        '<rect x="560" y="665" width="18" height="18" fill="#7d522b" fill-opacity="0.9" stroke="#4a2f19"/>',
        '<text x="588" y="680" font-size="16" font-family="Arial, sans-serif" fill="#3b3127">trunk/root collar</text>',
        '</svg>',
    ])
    FOCUS_OUTPUT_PATH.write_text("\n".join(focus_parts), encoding="utf-8")
    print(OUTPUT_PATH)


if __name__ == "__main__":
    main()
