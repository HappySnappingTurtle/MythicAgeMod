// T15 阶段 ② 斯廷法利斯怪鸟构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_stymphalian.js')
// 参数由 tools/blockbench/params/stymphalian_parts.py 生成。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/stymphalian_anim.js');
    return MA.buildEntity({
        name: 'stymphalian_bird',
        params: 'art/blockbench/greek/stymphalian/stymphalian.params.json',
        painter: 'tools/blockbench/stymphalian_paint.js',
        animations: MA.stymphalianAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/stymphalian_bird.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/stymphalian_bird/stymphalian_bird.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/stymphalian_bird.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/stymphalian_bird.json',
            bbmodel: 'art/blockbench/greek/stymphalian/stymphalian.bbmodel',
        },
    });
})();
