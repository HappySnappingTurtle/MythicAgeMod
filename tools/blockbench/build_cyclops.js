// T25a 阶段 ③ 独眼巨人构建脚本：参数 JSON → Blockbench 工程 → 几何、贴图、骨架数据与 .bbmodel。
// 运行（MCP risky_eval）：
//   window.MA_REPO = '<repo>'; window.MA_REQUIRE = require;
//   (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'));
//   window.MA.run('tools/blockbench/build_cyclops.js')
// 参数由 tools/blockbench/params/cyclops_parts.py 生成。
// 白模审阅阶段（用户确认前）只写到 art/blockbench/greek/cyclops/，不进运行时资源；确认后再把 out 指向 assets 并加动画。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    const DIR = 'art/blockbench/greek/cyclops/';
    return MA.buildEntity({
        name: 'cyclops',
        params: DIR + 'cyclops.params.json',
        painter: 'tools/blockbench/cyclops_paint.js',
        out: {
            geo: DIR + 'review/cyclops.geo.json',
            texture: DIR + 'review/cyclops.png',
            rig: DIR + 'review/cyclops.rig.json',
            bbmodel: DIR + 'cyclops.bbmodel',
        },
    });
})();
