// 实体模型的通用构建流程（由 build_greek_guardian.js 的流程提炼，T13 起新实体用它）：
// 参数 JSON → Blockbench 工程（骨骼、方块、按面明暗的贴图 + 细节绘制、定位点）→ 几何、贴图、动画、骨架数据、.bbmodel。
// 用法：window.MA.buildEntity({name, params, painter?, animations?, out: {geo, texture, animation?, rig, bbmodel}})
//   painter：脚本路径，返回 {paint(ctx, nets, P), glow?(ctx, nets, P)}；有 glow 时另出 <贴图>_glowmask.png
//   （透明画布上只画发光像素，GeckoLib AutoGlowingGeoLayer 读取）；animations：函数 (rel) → 动画文件文本（脚本里定义）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const FACE_LIGHT = { top: 1.12, front: 1.0, left: 0.92, right: 0.92, back: 0.86, bottom: 0.74, surface: 1.0 };

    MA.buildEntity = (cfg) => {
        const P = MA.readJson(cfg.params);
        const ATLAS = P.atlas;
        const ALL = P.parts.concat(P.extra_parts || []);
        MA.reset();
        MA.newEntityProject(cfg.name, ATLAS, ATLAS);
        const nets = MA.packUvNets(ALL, ATLAS, P.texels_per_unit);
        const painter = cfg.painter && MA.fs.existsSync(MA.abs(cfg.painter)) ? MA.run(cfg.painter) : null;
        const tex = MA.canvasTexture(cfg.name, ATLAS, ATLAS, (ctx) => {
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
        for (const b of P.bones.concat(P.extra_bones || [])) {
            MA.bone(b.id, A.pos(b.pivot_model_units), b.parent);
        }
        const isRightSide = (id) => /_r($|_)/.test(id);
        const rect = (r, mirror) => (mirror ? [r[0] + r[2], r[1], r[0], r[1] + r[3]] : [r[0], r[1], r[0] + r[2], r[1] + r[3]]);
        const planeAxis = (part) => {
            const n = part.plane_normal_model_axes || [0, 0, 1];
            const axis = n.findIndex((c) => c !== 0);
            return { axis, positive: n[axis] > 0 };
        };
        const faceUv = (part) => {
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
        };
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

        const out = cfg.out;
        const geo = MA.exportGeo(out.geo);
        MA.exportTexture(tex, out.texture);
        const exportsByPath = { [out.geo]: geo, [out.texture]: MA.fs.readFileSync(MA.abs(out.texture)) };
        if (painter && painter.glow) {
            const glowTex = MA.canvasTexture(cfg.name + '_glowmask', ATLAS, ATLAS, (ctx) => painter.glow(ctx, nets, P));
            out.glowmask = out.texture.replace(/\.png$/, '_glowmask.png');
            MA.exportTexture(glowTex, out.glowmask);
            exportsByPath[out.glowmask] = MA.fs.readFileSync(MA.abs(out.glowmask));
        }
        if (cfg.animations && out.animation) {
            exportsByPath[out.animation] = cfg.animations(out.animation);
        }
        const rig = {
            model: 'mythic_age:' + cfg.name,
            collision: { width: P.collision_proposal_blocks[0], height: P.collision_proposal_blocks[1] },
            locators: Object.fromEntries(P.attachments.map((a) => [a.id, a.position_model_units])),
            variants: {},
        };
        const rigText = JSON.stringify(rig, null, 2) + '\n';
        MA.writeText(out.rig, rigText);
        exportsByPath[out.rig] = rigText;
        MA.saveProject(out.bbmodel, exportsByPath);
        let minY = Infinity;
        let maxY = -Infinity;
        for (const c of Cube.all) {
            minY = Math.min(minY, c.from[1], c.to[1]);
            maxY = Math.max(maxY, c.from[1], c.to[1]);
        }
        return { out, cubes: Cube.all.length, bones: Group.all.length, height: [minY, maxY], painted: !!painter,
            glow: !!out.glowmask };
    };
    return 'entity build loaded';
})();
