// T9b 神庙祭司构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、骨架数据与 .bbmodel 存档。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_greek_guardian.js')
// 参数：art/blockbench/greek/greek_guardian/greek_guardian.params.json（卡片坐标，+X 为角色左、+Z 为前），
// 由 tools/blockbench/params/greek_guardian_parts.py 生成。流程与 build_minotaur.js 相同（检查清单 §一）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const animPath = 'tools/blockbench/greek_guardian_anim.js';
    if (MA.fs.existsSync(MA.abs(animPath))) {
        MA.run(animPath);
    }
    const A = MA.axes;
    const NAME = 'greek_guardian';
    const P = MA.readJson('art/blockbench/greek/greek_guardian/greek_guardian.params.json');
    const ATLAS = P.atlas;
    const ALL = P.parts.concat(P.extra_parts);

    MA.reset();
    MA.newEntityProject(NAME, ATLAS, ATLAS);
    const nets = MA.packUvNets(ALL, ATLAS, P.texels_per_unit);

    // ───────── 贴图：按面朝向分明暗 + 侧面上亮下暗，再叠加细节 ─────────
    const painterPath = 'tools/blockbench/greek_guardian_paint.js';
    const painter = MA.fs.existsSync(MA.abs(painterPath)) ? MA.run(painterPath) : null;
    const FACE_LIGHT = { top: 1.12, front: 1.0, left: 0.92, right: 0.92, back: 0.86, bottom: 0.74, surface: 1.0 };
    const tex = MA.canvasTexture(NAME, ATLAS, ATLAS, (ctx) => {
        for (const n of Object.values(nets)) {
            for (const [face, r] of Object.entries(n.faces)) {
                const base = MA.shade(n.color, FACE_LIGHT[face]);
                const vertical = face !== 'top' && face !== 'bottom';
                for (let v = 0; v < r[3]; v++) {
                    let f = 1;
                    if (vertical && r[3] >= 4) {
                        if (v === 0) f = 1.08;
                        else if (v === r[3] - 1) f = 0.82;
                        else if (v === r[3] - 2) f = 0.92;
                    }
                    ctx.fillStyle = MA.shade(base, f);
                    ctx.fillRect(r[0], r[1] + v, r[2], 1);
                }
            }
        }
        if (painter) {
            painter.paint(ctx, nets, P);
        }
    });

    // ───────── 骨骼 ─────────
    for (const b of P.bones.concat(P.extra_bones)) {
        MA.bone(b.id, A.pos(b.pivot_model_units), b.parent);
    }

    // ───────── 方块 ─────────
    const isRightSide = (id) => /_r($|_)/.test(id);
    const rect = (r, mirror) => (mirror ? [r[0] + r[2], r[1], r[0], r[1] + r[3]] : [r[0], r[1], r[0] + r[2], r[1] + r[3]]);

    /** 单面片的法线轴（0 = x，1 = y，2 = z）与正负。 */
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
        // 右侧部件与左侧共用 UV：左右面互换并水平镜像
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

    // ───────── 定位点 ─────────
    for (const a of P.attachments) {
        MA.locator(a.id, a.bone, A.pos(a.position_model_units));
    }
    Canvas.updateAll();

    const out = {
        geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/greek_guardian.geo.json',
        texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/greek_guardian/greek_guardian.png',
    };
    const geo = MA.exportGeo(out.geo);
    MA.exportTexture(tex, out.texture);
    const exportsByPath = { [out.geo]: geo, [out.texture]: MA.fs.readFileSync(MA.abs(out.texture)) };
    if (MA.greekGuardianAnimations) {
        out.animation = 'neoforge/src/main/resources/assets/mythic_age/animations/entity/greek_guardian.animation.json';
        exportsByPath[out.animation] = MA.greekGuardianAnimations(out.animation);
    }
    out.rig = 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/greek_guardian.json';
    const rig = {
        model: 'mythic_age:greek_guardian',
        collision: { width: P.collision_proposal_blocks[0], height: P.collision_proposal_blocks[1] },
        locators: Object.fromEntries(P.attachments.map((a) => [a.id, a.position_model_units])),
        variants: {},
    };
    const rigText = JSON.stringify(rig, null, 2) + '\n';
    MA.writeText(out.rig, rigText);
    exportsByPath[out.rig] = rigText;
    MA.saveProject('art/blockbench/greek/greek_guardian/greek_guardian.bbmodel', exportsByPath);

    // 实测包围盒（卡片 Y 即 Blockbench Y）
    let minY = Infinity;
    let maxY = -Infinity;
    for (const c of Cube.all) {
        minY = Math.min(minY, c.from[1], c.to[1]);
        maxY = Math.max(maxY, c.from[1], c.to[1]);
    }
    return { out, cubes: Cube.all.length, bones: Group.all.length, height: [minY, maxY], painted: !!painter };
})();
