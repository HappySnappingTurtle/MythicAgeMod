// T15 阶段 ③ 哈耳庇厄构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_harpy.js')
// 参数由 tools/blockbench/params/harpy_parts.py 生成。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/harpy_anim.js');
    return MA.buildEntity({
        name: 'harpy',
        params: 'art/blockbench/greek/harpy/harpy.params.json',
        painter: 'tools/blockbench/harpy_paint.js',
        animations: MA.harpyAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/harpy.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/harpy/harpy.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/harpy.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/harpy.json',
            bbmodel: 'art/blockbench/greek/harpy/harpy.bbmodel',
        },
    });
})();
