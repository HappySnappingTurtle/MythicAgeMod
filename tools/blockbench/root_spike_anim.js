// 根刺动画（T15 阶段 ⑤）：crack 0.8 秒（裂纹从中心扩开，根藏在地下）→ burst 0.4 秒（三根依次冲出）→ retract 0.6 秒（缩回、裂纹淡去）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const DOWN = [0, -15, 0];
    const ZERO = [0, 0, 0];
    const spikes = (frames) => Object.fromEntries(['spike_a', 'spike_b', 'spike_c'].map((b, i) =>
        [b, { position: frames.map(([t, v]) => [Math.min(t + (v === ZERO ? i * 0.04 : 0), 10), v]) }]));
    const ANIMS = {
        crack: { length: 0.8, loop: 'hold_on_last_frame', bones: Object.assign(
            { crack: { scale: [[0, [0.15, 1, 0.15]], [0.6, [1, 1, 1]], [0.8, [1, 1, 1]]] } },
            spikes([[0, DOWN], [0.8, DOWN]])) },
        burst: { length: 0.4, loop: 'hold_on_last_frame', bones: Object.assign(
            { crack: { scale: [[0, [1, 1, 1]]] } },
            { spike_a: { position: [[0, DOWN], [0.12, [0, 1.5, 0]], [0.2, ZERO], [0.4, ZERO]] },
              spike_b: { position: [[0, DOWN], [0.05, DOWN], [0.17, [0, 1.5, 0]], [0.25, ZERO], [0.4, ZERO]] },
              spike_c: { position: [[0, DOWN], [0.1, DOWN], [0.22, [0, 1.5, 0]], [0.3, ZERO], [0.4, ZERO]] } }) },
        retract: { length: 0.6, loop: 'hold_on_last_frame', bones: Object.assign(
            { crack: { scale: [[0, [1, 1, 1]], [0.6, [0, 1, 0]]] } },
            { spike_a: { position: [[0, ZERO], [0.5, DOWN]] }, spike_b: { position: [[0, ZERO], [0.5, DOWN]] },
              spike_c: { position: [[0, ZERO], [0.5, DOWN]] } }) },
    };
    MA.rootSpikeAnimations = (rel) => {
        for (const a of Animation.all.slice()) a.remove(false);
        for (const [name, def] of Object.entries(ANIMS)) {
            const anim = new Animation({ name, loop: def.loop, length: def.length, snapping: 20 }).add();
            for (const [boneName, channels] of Object.entries(def.bones)) {
                const group = MA.bones[boneName] || Group.all.find((g) => g.name === boneName);
                const animator = anim.getBoneAnimator(group);
                for (const [channel, frames] of Object.entries(channels)) {
                    for (const [time, v] of frames) {
                        const val = channel === 'position' ? A.pos(v) : v;
                        animator.addKeyframe({ channel, time, interpolation: 'linear', data_points: [{ x: val[0], y: val[1], z: val[2] }] });
                    }
                }
            }
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        return text;
    };
    return 'root spike animations defined';
})();
