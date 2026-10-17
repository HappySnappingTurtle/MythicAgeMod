// T11b 主神像构建脚本：参数 JSON → Blockbench 工程 → 方块实体几何、贴图与 .bbmodel 存档。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require; window.MA_STATUE = 'zeus';
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_statue.js')
// 参数：art/blockbench/greek/statue_<主神>/statue_<主神>.params.json（卡片坐标，+X 为角色左、+Z 为前），
// 由 tools/blockbench/params/statue_<主神>_parts.py 生成。与生物的区别：静止姿势写在骨骼的静止旋转里（没有动画），
// 输出到 geo/block 与 textures/statue（模型贴图不放 textures/block，那里只放 16×16 方块贴图）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const GOD = window.MA_STATUE;
    if (!GOD) {
        throw new Error('先设置 window.MA_STATUE（如 zeus）');
    }
    const NAME = 'statue_' + GOD;
    const P = MA.readJson('art/blockbench/greek/' + NAME + '/' + NAME + '.params.json');
    const ATLAS = P.atlas;
    const ALL = P.parts.concat(P.extra_parts);

    MA.reset();
    MA.newEntityProject(NAME, ATLAS, ATLAS);
    const nets = MA.packUvNets(ALL, ATLAS, P.texels_per_unit);

    // ───────── 贴图：按面朝向分明暗 + 侧面上亮下暗，再叠加大理石细节 ─────────
    const painter = MA.run('tools/blockbench/statue_paint.js');
    const FACE_LIGHT = { top: 1.1, front: 1.0, left: 0.93, right: 0.93, back: 0.88, bottom: 0.78, surface: 1.0 };
    const tex = MA.canvasTexture(NAME, ATLAS, ATLAS, (ctx) => {
        for (const n of Object.values(nets)) {
            for (const [face, r] of Object.entries(n.faces)) {
                const base = MA.shade(n.color, FACE_LIGHT[face]);
                const vertical = face !== 'top' && face !== 'bottom';
                for (let v = 0; v < r[3]; v++) {
                    let f = 1;
                    if (vertical && r[3] >= 4) {
                        if (v === 0) f = 1.05;
                        else if (v === r[3] - 1) f = 0.88;
                        else if (v === r[3] - 2) f = 0.95;
                    }
                    ctx.fillStyle = MA.shade(base, f);
                    ctx.fillRect(r[0], r[1] + v, r[2], 1);
                }
            }
        }
        painter.paint(ctx, nets, P, GOD);
    });

    // ───────── 骨骼（带静止旋转）─────────
    for (const b of P.bones) {
        const g = MA.bone(b.id, A.pos(b.pivot_model_units), b.parent);
        if (b.rest_rotation_degrees && b.rest_rotation_degrees.some((v) => v !== 0)) {
            g.rotation = A.rot(b.rest_rotation_degrees);
        }
    }

    // ───────── 方块 ─────────
    const isRightSide = (id) => /_r($|_)/.test(id);
    const rect = (r, mirror) => (mirror ? [r[0] + r[2], r[1], r[0], r[1] + r[3]] : [r[0], r[1], r[0] + r[2], r[1] + r[3]]);

    function planeAxis(part) {
        const n = part.plane_normal_model_axes || [0, 0, 1];
        const axis = n.findIndex((c) => c !== 0);
        return { axis, positive: n[axis] > 0 };
    }

    function faceUv(part) {
        const net = nets[part.uv_group];
        if (!net) {
            throw new Error('部件 ' + part.id + ' 的 UV 组不存在：' + part.uv_group);
        }
        const uv = {};
        if (net.plane) {
            const { axis, positive } = planeAxis(part);
            const anat = axis === 2 ? (positive ? 'front' : 'back') : axis === 1 ? (positive ? 'top' : 'bottom')
                : (positive ? 'left' : 'right');
            uv[A.face[anat]] = rect(net.faces.surface, false);
            return uv;
        }
        const mirror = isRightSide(part.id) && part.uv_share;
        for (const [anat, r] of Object.entries(net.faces)) {
            let target = anat;
            if (mirror && anat === 'left') target = 'right';
            else if (mirror && anat === 'right') target = 'left';
            uv[A.face[target]] = rect(r, mirror);
        }
        return uv;
    }

    for (const part of ALL) {
        const size = part.size_model_units.slice();
        if (part.surface_kind === 'single_plane') {
            size[planeAxis(part).axis] = 0;
        }
        const box = A.box(part.center_model_units, size);
        const rot = part.rotation_degrees_model_axes ? A.rot(part.rotation_degrees_model_axes) : [0, 0, 0];
        MA.cube(part.id, part.bone, box.from, box.to, faceUv(part), { texture: tex, rotation: rot });
    }
    for (const a of P.attachments) {
        MA.locator(a.id, a.bone, A.pos(a.position_model_units));
    }
    Canvas.updateAll();

    const out = {
        geo: 'neoforge/src/main/resources/assets/mythic_age/geo/block/' + NAME + '.geo.json',
        texture: 'neoforge/src/main/resources/assets/mythic_age/textures/statue/' + GOD + '.png',
    };
    const geo = MA.exportGeo(out.geo);
    MA.exportTexture(tex, out.texture);
    const exportsByPath = { [out.geo]: geo, [out.texture]: MA.fs.readFileSync(MA.abs(out.texture)) };
    // 发光贴图（哈迪斯的眼睛等）：透明画布上只画发光像素，渲染器见到 <主神>_glowmask.png 就叠加发光层
    if (P.glow) {
        const glowTex = MA.canvasTexture(NAME + '_glowmask', ATLAS, ATLAS, (ctx) => painter.glow(ctx, nets, P, GOD));
        out.glowmask = 'neoforge/src/main/resources/assets/mythic_age/textures/statue/' + GOD + '_glowmask.png';
        MA.exportTexture(glowTex, out.glowmask);
        exportsByPath[out.glowmask] = MA.fs.readFileSync(MA.abs(out.glowmask));
    }
    MA.saveProject('art/blockbench/greek/' + NAME + '/' + NAME + '.bbmodel', exportsByPath);

    // 实测包围盒（摆好姿势后的世界坐标，卡片 Y 即 Blockbench Y）
    const box = new THREE.Box3();
    for (const c of Cube.all) {
        if (c.mesh) {
            c.mesh.updateMatrixWorld(true);
            box.expandByObject(c.mesh);
        }
    }
    return { out, cubes: Cube.all.length, bones: Group.all.length,
        bounds: [box.min.toArray().map((v) => +v.toFixed(2)), box.max.toArray().map((v) => +v.toFixed(2))] };
})();
