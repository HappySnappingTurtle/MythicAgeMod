// T14 弥诺陶洛斯挥斧的姿势搜索（一次性工具）：刃面必须竖在挥动平面里（斧只绕柄转 rz = 0，手臂外展由前臂反向抵消），
// 在给定网格里找举斧帧与劈下帧都不碰头、劈下时刃口朝前下方的组合。结果写到 window.MA.chopSearch。
(function () {
    const MA = window.MA;
    const def = MA.MINOTAUR_ANIMS.attack_swing;
    const setKey = (bone, t, v) => {
        const fr = def.bones[bone].rotation;
        const i = fr.findIndex((f) => Math.abs(f[0] - t) < 1e-6);
        fr[i] = [t, v];
    };
    const axe = Cube.all.filter((c) => /^axe/.test(c.name));
    const head = Cube.all.filter((c) => /^(cranium|muzzle|nose|horn|brow|ear|jowl)/.test(c.name));
    const axeGroup = Group.all.find((g) => g.name === 'axe');
    const at = (t) => {
        const anim = Animation.all.find((a) => a.name === 'attack_swing');
        anim.select();
        Timeline.setTime(t);
        Animator.preview();
        const r = MA.preciseHit(axe, head);
        const ax = MA.boneAxes(axeGroup);
        return { hit: r.hit, minY: Math.round(r.minY), lateral: Math.round(Math.abs(ax.x.x) * 100) / 100,
            haft: [ax.z.x, ax.z.y, ax.z.z].map((v) => Math.round(v * 100) / 100) };
    };
    const cfg = window.MA_CHOP || {};
    const out = [];
    const t = cfg.t;
    for (const ab of cfg.ab) {
        for (const ux of cfg.ux) {
            for (const fx of cfg.fx) {
                for (const hx of cfg.hx) {
                    setKey('upper_arm_l', t, [ux, 0, ab]);
                    setKey('forearm_l', t, [fx, 0, -ab]);
                    setKey('hand_l', t, [hx, 0, 0]);
                    setKey('axe', t, [0, 0, 0]);
                    MA.buildMinotaurAnimations();
                    Modes.options.animate.select();
                    const r = at(t);
                    // 斧头方向 = 斧柄局部 −Z；角色正面是 −Z
                    r.head = r.haft.map((v) => -v);
                    out.push(Object.assign({ ab, ux, fx, hx }, r));
                }
            }
        }
    }
    MA.chopSearch = out;
    return out.length;
})();
