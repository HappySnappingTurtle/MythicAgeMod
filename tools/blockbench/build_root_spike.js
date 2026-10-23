// T15 阶段 ⑤ 根刺构建脚本（树仙女的攻击实体）。运行方式同 build_hamadryad.js。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/entity_build.js');
    MA.run('tools/blockbench/root_spike_anim.js');
    return MA.buildEntity({
        name: 'root_spike',
        params: 'art/blockbench/greek/hamadryad/root_spike.params.json',
        painter: 'tools/blockbench/root_spike_paint.js',
        animations: MA.rootSpikeAnimations,
        out: {
            geo: 'neoforge/src/main/resources/assets/mythic_age/geo/entity/root_spike.geo.json',
            texture: 'neoforge/src/main/resources/assets/mythic_age/textures/entity/root_spike/root_spike.png',
            animation: 'neoforge/src/main/resources/assets/mythic_age/animations/entity/root_spike.animation.json',
            rig: 'neoforge/src/main/resources/data/mythic_age/mythic_age/rig/root_spike.json',
            bbmodel: 'art/blockbench/greek/hamadryad/root_spike.bbmodel',
        },
    });
})();
