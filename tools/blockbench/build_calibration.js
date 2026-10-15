// T7 §3 校准样板：1×1×1 格立方体，六个面按 Blockbench 面名标字母（N/S/E/W/U/D），
// 另在 Blockbench +X 一侧挂一根品红细杆（骨骼 probe_bone，定位点 probe_tip）。
// 进游戏后看哪个字母朝实体前进方向、细杆在实体哪一侧，即可确定 Blockbench → 游戏的轴向换算。
// 运行：MA.run('tools/blockbench/build_calibration.js')（先加载 lib/bb.js）。
(function () {
    MA.reset();
    MA.newEntityProject('calibration', 128, 64);

    const FACE = {
        north: { rect: [0, 0, 32, 32], bg: '#C0392B', ch: 'N' },
        south: { rect: [32, 0, 64, 32], bg: '#2E5BBA', ch: 'S' },
        east: { rect: [64, 0, 96, 32], bg: '#D4A017', ch: 'E' },
        west: { rect: [96, 0, 128, 32], bg: '#2E8B57', ch: 'W' },
        up: { rect: [0, 32, 32, 64], bg: '#EEEEEE', ch: 'U' },
        down: { rect: [32, 32, 64, 64], bg: '#777777', ch: 'D' },
    };
    const ROD = [64, 32, 72, 64];

    const tex = MA.canvasTexture('calibration', 128, 64, (ctx) => {
        for (const f of Object.values(FACE)) {
            const [x1, y1, x2, y2] = f.rect;
            ctx.fillStyle = f.bg;
            ctx.fillRect(x1, y1, x2 - x1, y2 - y1);
            // 2 像素黑边，便于看清面的边界与 UV 是否翻转
            ctx.fillStyle = '#111111';
            ctx.fillRect(x1, y1, 32, 2);
            ctx.fillRect(x1, y1, 2, 32);
            // 左上角的小方块标记 UV 原点，用来判断贴图是否左右或上下翻转
            ctx.fillRect(x1 + 3, y1 + 3, 4, 4);
            MA.drawGlyph(ctx, f.ch, x1 + 8, y1 + 4, 3, f.ch === 'U' ? '#111111' : '#FFFFFF');
        }
        ctx.fillStyle = '#E040FB';
        ctx.fillRect(ROD[0], ROD[1], ROD[2] - ROD[0], ROD[3] - ROD[1]);
    });

    MA.bone('root', [0, 0, 0]);
    const uv = {};
    for (const [name, f] of Object.entries(FACE)) {
        uv[name] = f.rect;
    }
    MA.cube('block', 'root', [-8, 0, -8], [8, 16, 8], uv, { texture: tex });

    MA.bone('probe_bone', [10, 8, 0], 'root');
    const rodUv = { north: ROD, south: ROD, east: ROD, west: ROD, up: [64, 32, 66, 34], down: [64, 32, 66, 34] };
    MA.cube('probe_rod', 'probe_bone', [9, 0, -1], [11, 24, 1], rodUv, { texture: tex });
    MA.locator('probe_tip', 'probe_bone', [10, 24, 0]);

    Canvas.updateAll();

    const geoPath = 'neoforge/src/agent/resources/assets/mythic_age/geo/entity/dev_calibration.geo.json';
    const texPath = 'neoforge/src/agent/resources/assets/mythic_age/textures/entity/dev_calibration.png';
    const geo = MA.exportGeo(geoPath);
    MA.exportTexture(tex, texPath);
    MA.saveProject('art/blockbench/dev/calibration.bbmodel', { [geoPath]: geo });
    return { geo: geoPath, cubes: Cube.all.length, groups: Group.all.length, locators: Locator.all.length };
})();
