// T19 阶段 ① 希腊居民构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_resident.js')
// 参数由 tools/blockbench/params/resident_parts.py 生成；导出后跑 tools/blockbench/resident_palettes.py 出男女 × 配色变体。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/resident_anim.js');
    return MA.buildEntity({
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
})();
