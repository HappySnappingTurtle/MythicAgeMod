// T25a 独眼巨人构建脚本：参数 JSON → Blockbench 工程 → 运行时几何、贴图、动画、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_cyclops.js')
// 参数由 tools/blockbench/params/cyclops_parts.py 生成。白模 v2 用户 2026-11-18 确认，输出改到运行时资源并加动画（阶段 ④）。
// 召唤版隐藏 fleece、boulder；牧人隐藏 boulder（举石时由动画显示，阶段 ⑥）——隐藏在渲染器里按骨骼做。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/cyclops_anim.js');
    const DIR = 'art/blockbench/greek/cyclops/';
    return MA.buildEntity({
        name: 'cyclops',
        params: DIR + 'cyclops.params.json',
        painter: 'tools/blockbench/cyclops_paint.js',
        animations: MA.cyclopsAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/cyclops.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/cyclops/cyclops.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/cyclops.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/cyclops.json',
            bbmodel: DIR + 'cyclops.bbmodel',
        },
    });
})();
