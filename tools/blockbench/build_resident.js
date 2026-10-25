// T19 阶段 ① 希腊居民构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_resident.js')
// 参数由 tools/blockbench/params/resident_parts.py 生成；导出后跑 tools/blockbench/resident_palettes.py 出男女 × 配色变体。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/soldier_anim.js');
    MA.run('tools/blockbench/resident_anim.js');
    const result = MA.buildEntity({
        name: 'resident',
        params: 'art/blockbench/greek/resident/resident.params.json',
        painter: 'tools/blockbench/resident_paint.js',
        animations: MA.residentAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/resident.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/resident/resident.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/resident.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/resident.json',
            bbmodel: 'art/blockbench/greek/resident/resident.bbmodel',
        },
    });
    // 换色遮罩（resident_palettes.py 用）：红 = 皮肤，绿 = 男装，蓝 = 女装，黄 = 披巾；UV 排布与贴图一致
    const P = MA.readJson('art/blockbench/greek/resident/resident.params.json');
    const nets = MA.packUvNets(P.parts.concat(P.extra_parts), P.atlas, P.texels_per_unit);
    const KEY = { '#AE7656': '#FF0000', '#D6C7A6': '#00FF00', '#BDAE8E': '#00FF00', '#D2AC58': '#0000FF', '#DDBB6C': '#0000FF',
        '#B48F42': '#0000FF', '#7C3A2F': '#FFFF00' };
    const mask = MA.canvasTexture('resident_mask', P.atlas, P.atlas, (ctx) => {
        for (const n of Object.values(nets)) {
            const k = KEY[n.color.toUpperCase()];
            if (!k) continue;
            ctx.fillStyle = k;
            for (const r of Object.values(n.faces)) ctx.fillRect(r[0], r[1], r[2], r[3]);
        }
    });
    MA.exportTexture(mask, 'art/blockbench/greek/resident/resident_mask.png');
    mask.remove();
    return result;
})();
