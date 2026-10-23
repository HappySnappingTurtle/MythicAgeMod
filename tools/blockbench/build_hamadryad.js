// T15 阶段 ⑤ 树仙女构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_hamadryad.js')
// 参数由 tools/blockbench/params/hamadryad_parts.py 生成。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/hamadryad_anim.js');
    return MA.buildEntity({
        name: 'hamadryad',
        params: 'art/blockbench/greek/hamadryad/hamadryad.params.json',
        painter: 'tools/blockbench/hamadryad_paint.js',
        animations: MA.hamadryadAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/hamadryad.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/hamadryad/hamadryad.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/hamadryad.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/hamadryad.json',
            bbmodel: 'art/blockbench/greek/hamadryad/hamadryad.bbmodel',
        },
    });
})();
