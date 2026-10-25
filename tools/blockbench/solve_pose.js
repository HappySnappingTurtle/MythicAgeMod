// 姿势求解（T20 阶段 ③）：在某动画某时刻，网格搜索一侧手臂的上臂 [rx, ry, rz]、前臂 rx、手腕 [rx, rz]（卡片语义），
// 让手上的握点（手骨局部 (0, −1, 0)，即握孔中心）落到目标点、并让某根局部轴指向目标方向：
//   {anim, time, side: 'r'|'l', point: [x, y, z]（卡片坐标，可省）, axis: 'grip'|'shield', dir: [x, y, z]（卡片方向，可省）,
//    wPoint, wDir（权重）, wrist: true（是否搜手腕）, from: 'hand'|'shield'（point 量的是握点还是盾心）}
// grip = 握孔方向（手骨局部卡片 +Z）；shield = 盾面外法线（前臂局部卡片 +X，盾挂在左前臂）。
// 解出的角度手抄回 soldier_anim.js（动画按数据重建，不直接改工程）。
(function () {
    const MA = window.MA;
    const RAD = Math.PI / 180;
    const group = (n) => Group.all.find((g) => g.name === n);
    const bb = (v) => new THREE.Vector3(-v[0], v[1], -v[2]);
    MA.solvePose = (cfg) => {
        Modes.options.animate.select();
        const anim = Animation.all.find((a) => a.name === cfg.anim);
        anim.select();
        Timeline.setTime(cfg.time || 0);
        Animator.preview();
        const s = cfg.side;
        const ua = group('upper_arm_' + s);
        const fa = group('forearm_' + s);
        const hand = group('hand_' + s);
        const wP = cfg.wPoint === undefined ? 1 : cfg.wPoint;
        const wD = cfg.wDir === undefined ? 0.15 : cfg.wDir;
        const target = cfg.point ? bb(cfg.point) : null;
        const tdir = cfg.dir ? bb(cfg.dir).normalize() : null;
        const o = new THREE.Vector3();
        const p = new THREE.Vector3();
        const q = new THREE.Vector3();
        const measure = () => {
            ua.mesh.updateMatrixWorld(true);
            let pt;
            let d;
            if (cfg.axis === 'shield' || cfg.from === 'shield') {
                // 盾心：前臂局部（盾心在卡片 (±8.2, 14, 0)，前臂枢轴 (±5.5, 15.5, 0)）
                const sx = s === 'l' ? -2.7 : 2.7;
                fa.mesh.localToWorld(p.set(sx, -1.5, 0));
                pt = p.clone();
                fa.mesh.localToWorld(o.set(0, 0, 0));
                fa.mesh.localToWorld(q.set(s === 'l' ? -1 : 1, 0, 0));
                d = q.clone().sub(o).normalize();
            } else {
                hand.mesh.localToWorld(p.set(0, -1, 0));
                pt = p.clone();
                hand.mesh.localToWorld(o.set(0, -1, 0));
                hand.mesh.localToWorld(q.set(0, -1, -1));
                d = q.clone().sub(o).normalize();
            }
            return { pt, d };
        };
        const evalAt = (u, f, w) => {
            ua.mesh.rotation.set(-u[0] * RAD, u[1] * RAD, -u[2] * RAD);
            fa.mesh.rotation.set(-f * RAD, 0, 0);
            hand.mesh.rotation.set(-w[0] * RAD, (w[2] || 0) * RAD, -w[1] * RAD);
            const m = measure();
            const dp = target ? m.pt.distanceTo(target) : 0;
            const dd = tdir ? Math.acos(Math.max(-1, Math.min(1, m.d.dot(tdir)))) / RAD : 0;
            return { cost: wP * dp + wD * dd + 0.002 * (Math.abs(u[1]) + Math.abs(u[2]) + Math.abs(w[0]) + Math.abs(w[1])),
                dp, dd, u: u.slice(), f, w: w.slice() };
        };
        let best = null;
        const consider = (c) => { if (!best || c.cost < best.cost) best = c; };
        const wr = cfg.wrist ? [-90, -60, -30, 0, 30, 60, 90] : [0];
        // 搜手腕三轴时粗网格放宽一倍（否则一次调用超时），细化时再收紧
        const k = cfg.wristY ? 2 : 1;
        for (let rx = -180; rx <= 40; rx += 10 * k) {
            for (let ry = -90; ry <= 90; ry += 15 * k) {
                for (let rz = -60; rz <= 90; rz += 15 * k) {
                    for (let f = -150; f <= 0; f += 15 * k) {
                        for (const wx of wr) {
                            for (const wz of (cfg.wrist ? [-30, 0, 30] : [0])) {
                                for (const wy of (cfg.wristY ? [-90, -45, 0, 45, 90] : [0])) consider(evalAt([rx, ry, rz], f, [wx, wz, wy]));
                            }
                        }
                    }
                }
            }
        }
        for (const step of (cfg.wristY ? [10, 5, 2] : [5, 2])) {
            const c0 = best;
            for (let dx = -2; dx <= 2; dx++) {
                for (let dy = -2; dy <= 2; dy++) {
                    for (let dz = -2; dz <= 2; dz++) {
                        for (let df = -2; df <= 2; df++) {
                            for (const dw of (cfg.wrist ? [[-step, 0, 0], [0, 0, 0], [step, 0, 0], [0, -step, 0], [0, step, 0], [0, 0, -step], [0, 0, step]] : [[0, 0, 0]])) {
                                consider(evalAt([c0.u[0] + dx * step, c0.u[1] + dy * step, c0.u[2] + dz * step],
                                    Math.min(0, c0.f + df * step), [c0.w[0] + dw[0], c0.w[1] + dw[1], (c0.w[2] || 0) + (cfg.wristY ? dw[2] : 0)]));
                            }
                        }
                    }
                }
            }
        }
        Animator.preview();
        const r1 = (v) => Math.round(v * 10) / 10;
        // 手腕按卡片 [rx, ry, rz] 返回（求解时存的是 [rx, rz, ry]）
        return { anim: cfg.anim, time: cfg.time, side: s, dPoint: r1(best.dp), dDir: r1(best.dd), upper: best.u, fore: best.f,
            hand: [best.w[0], best.w[2] || 0, best.w[1]] };
    };
    return 'solve pose loaded';
})();
