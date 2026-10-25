// 手够目标点（T19 阶段 ⑤b）：在某动画某时刻，网格搜索一侧手臂的上臂 [rx, ry, rz] 与前臂 rx（卡片语义），
// 让手的握点（手骨局部 (0, −1.5, 0)）落到目标点上。目标点用某个方块上的局部比例点给出：
//   {cube: 名字, f: [fx, fy, fz]}，f 是方块包围盒内的比例（−0.5..0.5）。返回解与残差（u）。
// 解出来的角度手抄回 resident_anim.js（动画按数据重建，不直接改工程）。
(function () {
    const MA = window.MA;
    const RAD = Math.PI / 180;
    const group = (n) => Group.all.find((g) => g.name === n);
    const cubeByName = (n) => Cube.all.find((c) => c.name === n);
    function targetPoint(spec) {
        const c = cubeByName(spec.cube);
        c.mesh.updateMatrixWorld(true);
        const g = c.mesh.geometry;
        g.computeBoundingBox();
        const b = g.boundingBox;
        const p = new THREE.Vector3(
            (b.min.x + b.max.x) / 2 + spec.f[0] * (b.max.x - b.min.x),
            (b.min.y + b.max.y) / 2 + spec.f[1] * (b.max.y - b.min.y),
            (b.min.z + b.max.z) / 2 + spec.f[2] * (b.max.z - b.min.z));
        return c.mesh.localToWorld(p);
    }
    MA.solveReach = (cfg) => {
        Modes.options.animate.select();
        const anim = Animation.all.find((a) => a.name === cfg.anim);
        anim.select();
        Timeline.setTime(cfg.time || 0);
        Animator.preview();
        const side = cfg.side;
        const ua = group('upper_arm_' + side);
        const fa = group('forearm_' + side);
        const hand = group('hand_' + side);
        const target = targetPoint(cfg.target);
        const p = new THREE.Vector3();
        const evalAt = (rx, ry, rz, frx) => {
            ua.mesh.rotation.set(-rx * RAD, ry * RAD, -rz * RAD);
            fa.mesh.rotation.set(-frx * RAD, 0, 0);
            ua.mesh.updateMatrixWorld(true);
            hand.mesh.localToWorld(p.set(0, -1.5, 0));
            const d = p.distanceTo(target);
            return { cost: d + 0.002 * (Math.abs(ry) + Math.abs(rz)) + 0.001 * Math.abs(frx), d, v: [rx, ry, rz], f: frx };
        };
        let best = null;
        const consider = (c) => { if (!best || c.cost < best.cost) best = c; };
        for (let rx = -170; rx <= 20; rx += 10) {
            for (let ry = -80; ry <= 80; ry += 10) {
                for (let rz = -80; rz <= 80; rz += 10) {
                    for (let f = -140; f <= 0; f += 10) consider(evalAt(rx, ry, rz, f));
                }
            }
        }
        const c0 = best;
        for (let dx = -10; dx <= 10; dx += 2.5) {
            for (let dy = -10; dy <= 10; dy += 2.5) {
                for (let dz = -10; dz <= 10; dz += 2.5) {
                    for (let df = -10; df <= 10; df += 5) consider(evalAt(c0.v[0] + dx, c0.v[1] + dy, c0.v[2] + dz, Math.min(0, c0.f + df)));
                }
            }
        }
        Animator.preview();
        return { anim: cfg.anim, time: cfg.time, side, d: Math.round(best.d * 100) / 100, upper: best.v, fore: best.f };
    };
    return 'solve reach loaded';
})();
