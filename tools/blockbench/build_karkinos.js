// T15 阶段 ④ 卡尔基诺斯构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_karkinos.js')
// 参数由 tools/blockbench/params/karkinos_parts.py 生成。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/karkinos_anim.js');
    return MA.buildEntity({
        name: 'karkinos',
        params: 'art/blockbench/greek/karkinos/karkinos.params.json',
        painter: 'tools/blockbench/karkinos_paint.js',
        animations: MA.karkinosAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/karkinos.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/karkinos/karkinos.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/karkinos.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/karkinos.json',
            bbmodel: 'art/blockbench/greek/karkinos/karkinos.bbmodel',
        },
    });
})();
