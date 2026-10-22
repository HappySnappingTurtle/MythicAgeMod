// T14 阶段 ③ 半人马构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_centaur.js')
// 参数由 tools/blockbench/params/centaur_parts.py 生成。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/centaur_anim.js');
    return MA.buildEntity({
        name: 'centaur',
        params: 'art/blockbench/greek/centaur/centaur.params.json',
        painter: 'tools/blockbench/centaur_paint.js',
        animations: MA.centaurAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/centaur.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/centaur/centaur.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/centaur.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/centaur.json',
            bbmodel: 'art/blockbench/greek/centaur/centaur.bbmodel',
        },
    });
})();
