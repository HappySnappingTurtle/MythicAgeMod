// T13 阶段 ③ 哨兵石像构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_sentinel.js')
// 参数由 tools/blockbench/params/sentinel_parts.py 生成。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    const animPath = 'tools/blockbench/sentinel_anim.js';
    if (MA.fs.existsSync(MA.abs(animPath))) {
        MA.run(animPath);
    }
    return MA.buildEntity({
        name: 'sentinel',
        params: 'art/blockbench/greek/sentinel/sentinel.params.json',
        painter: 'tools/blockbench/sentinel_paint.js',
        animations: MA.sentinelAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/sentinel.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/sentinel/sentinel.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/sentinel.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/sentinel.json',
            bbmodel: 'art/blockbench/greek/sentinel/sentinel.bbmodel',
        },
    });
})();
