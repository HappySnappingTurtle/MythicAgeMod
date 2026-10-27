// T7 弥诺陶洛斯构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图与 .bbmodel 存档。
// 运行（MCP risky_eval）：
//   window.MA_REQUIRE = require; (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_minotaur.js')
// 参数：art/blockbench/greek/minotaur/minotaur.params.json（卡片坐标，+X 为角色左、+Z 为前）。
// 贴图绘制在 minotaur_paint.js 中（缺省时用各 UV 组的底色平涂，即白模阶段）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    MA.run('tools/blockbench/minotaur_anim.js');
    const A = MA.axes;
    const P = MA.readJson('art/blockbench/greek/minotaur/minotaur.params.json');
    const ATLAS = P.atlas;

    MA.reset();
    MA.newEntityProject('minotaur', ATLAS, ATLAS);

    const nets = MA.packUvNets(P.parts.concat(P.extra_parts), ATLAS, P.texels_per_unit);

    // ───────── 贴图 ─────────
    const painterPath = 'tools/blockbench/minotaur_paint.js';
    const painter = MA.fs.existsSync(MA.abs(painterPath)) ? MA.run(painterPath) : null;
    // 底色：按面的朝向分明暗（顶亮、底暗、背面略暗）；侧面再做上亮下暗的体积渐变（不描边，描边会让体块像甲片）
    const FACE_LIGHT = { top: 1.12, front: 1.0, left: 0.92, right: 0.92, back: 0.86, bottom: 0.74, surface: 1.0 };
    const tex = MA.canvasTexture('minotaur', ATLAS, ATLAS, (ctx) => {
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
    // 自发光遮罩（红眼）：GeckoLib 的 AutoGlowingGeoLayer 读取 <贴图>_glowmask.png
    let glowCanvas = null;
    if (painter && painter.glow) {
        glowCanvas = document.createElement('canvas');
        glowCanvas.width = ATLAS;
        glowCanvas.height = ATLAS;
        painter.glow(glowCanvas.getContext('2d'), nets, P);
    }

    // ───────── 骨骼 ─────────
    for (const b of P.bones.concat(P.extra_bones)) {
        MA.bone(b.id, A.pos(b.pivot_model_units), b.parent);
    }

    // ───────── 方块 ─────────
    const isRightSide = (id) => /_r($|_)/.test(id);
    const rect = (r, mirror) => (mirror ? [r[0] + r[2], r[1], r[0], r[1] + r[3]] : [r[0], r[1], r[0] + r[2], r[1] + r[3]]);

    function faceUv(part) {
        const net = nets[part.uv_group];
        if (!net) {
            throw new Error('部件 ' + part.id + ' 的 UV 组不存在：' + part.uv_group);
        }
        const uv = {};
        if (net.plane) {
            // 单面片：只在法线方向的一面贴图（运行时不剔除背面，从另一侧也看得到）
            const n = part.plane_normal_model_axes || [0, 0, 1];
            uv[A.face[n[2] < 0 ? 'back' : 'front']] = rect(net.faces.surface, false);
            return uv;
        }
        // 右侧部件与左侧共用 UV：左右面互换并水平镜像，让两侧的外侧面取同一块贴图
        const mirror = isRightSide(part.id) && part.uv_share;
        for (const [anat, r] of Object.entries(net.faces)) {
            let target = anat;
            if (mirror && anat === 'left') target = 'right';
            else if (mirror && anat === 'right') target = 'left';
            uv[A.face[target]] = rect(r, mirror);
        }
        return uv;
    }

    for (const part of P.parts.concat(P.extra_parts)) {
        let center = part.center_model_units.slice();
        let size = part.size_model_units.slice();
        if (part.surface_kind === 'single_plane') {
            size[2] = 0; // 衣片：零厚度单面，位置即参数中的中心
        }
        const box = A.box(center, size);
        const rot = part.rotation_degrees_model_axes ? A.rot(part.rotation_degrees_model_axes) : [0, 0, 0];
        MA.cube(part.id, part.bone, box.from, box.to, faceUv(part), { texture: tex, rotation: rot });
    }

    // 默认隐藏的骨骼（投石）只在 Blockbench 视图中隐藏；运行时由渲染器控制显隐
    for (const b of P.extra_bones.filter((b) => b.hidden_by_default)) {
        for (const c of MA.bones[b.id].children) {
            c.visibility = false;
        }
    }

    // ───────── 定位点 ─────────
    for (const a of P.attachments) {
        MA.locator(a.id, a.bone, A.pos(a.position_model_units));
    }

    Canvas.updateAll();

    const out = {
        geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/minotaur.geo.json',
        texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/minotaur/minotaur.png',
    };
    const geo = MA.exportGeo(out.geo);
    MA.exportTexture(tex, out.texture);
    const exportsByPath = { [out.geo]: geo, [out.texture]: MA.fs.readFileSync(MA.abs(out.texture)) };
    if (glowCanvas) {
        out.glowmask = out.texture.replace(/\.png$/, '_glowmask.png');
        MA.writeText(out.glowmask, Buffer.from(glowCanvas.toDataURL('image/png').split(',')[1], 'base64'));
        exportsByPath[out.glowmask] = MA.fs.readFileSync(MA.abs(out.glowmask));
    }
    // Boss 变体贴图（T23 §8）：在主贴图上改色与加纹样；发光遮罩与主贴图相同（红眼位置不变）
    if (painter && painter.boss) {
        const bossCanvas = document.createElement('canvas');
        bossCanvas.width = ATLAS;
        bossCanvas.height = ATLAS;
        const bctx = bossCanvas.getContext('2d');
        bctx.imageSmoothingEnabled = false;
        bctx.drawImage(tex.maCanvas, 0, 0);
        painter.boss(bctx, nets, P);
        out.bossTexture = out.texture.replace(/\.png$/, '_boss.png');
        MA.writeText(out.bossTexture, Buffer.from(bossCanvas.toDataURL('image/png').split(',')[1], 'base64'));
        exportsByPath[out.bossTexture] = MA.fs.readFileSync(MA.abs(out.bossTexture));
        if (glowCanvas) {
            out.bossGlowmask = out.bossTexture.replace(/\.png$/, '_glowmask.png');
            MA.writeText(out.bossGlowmask, Buffer.from(glowCanvas.toDataURL('image/png').split(',')[1], 'base64'));
            exportsByPath[out.bossGlowmask] = MA.fs.readFileSync(MA.abs(out.bossGlowmask));
        }
    }
    if (MA.minotaurAnimations) {
        out.animation = 'neoforge/src/main/resources/assets/mythic_age/animations/entity/minotaur.animation.json';
        exportsByPath[out.animation] = MA.minotaurAnimations(out.animation);
    }
    // 服务端骨架数据（卡片坐标，不经 Blockbench 换算）：碰撞、挂点、变体
    out.rig = 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/minotaur.json';
    const rig = {
        model: 'mythic_age:minotaur',
        collision: { width: P.collision_proposal_blocks[0], height: P.collision_proposal_blocks[1] },
        locators: Object.fromEntries(P.attachments.map((a) => [a.id, a.position_model_units])),
        variants: { boss: { scale: P.boss_scale, texture: 'mythic_age:textures/entity/minotaur/minotaur_boss.png' } },
    };
    const rigText = JSON.stringify(rig, null, 2) + '\n';
    MA.writeText(out.rig, rigText);
    exportsByPath[out.rig] = rigText;
    MA.saveProject('art/blockbench/greek/minotaur/minotaur.bbmodel', exportsByPath);
    return { out, cubes: Cube.all.length, bones: Group.all.length, locators: Locator.all.length, painted: !!painter };
})();
