// T14 阶段 ② 天马构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图（含天眼发光层）、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_pegasus.js')
// 参数由 tools/blockbench/params/pegasus_parts.py 生成。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/pegasus_anim.js');
    return MA.buildEntity({
        name: 'pegasus',
        params: 'art/blockbench/greek/pegasus/pegasus.params.json',
        painter: 'tools/blockbench/pegasus_paint.js',
        animations: MA.pegasusAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/pegasus.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/pegasus/pegasus.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/pegasus.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/pegasus.json',
            bbmodel: 'art/blockbench/greek/pegasus/pegasus.bbmodel',
        },
    });
})();
