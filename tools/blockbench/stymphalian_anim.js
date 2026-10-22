// 斯廷法利斯怪鸟动画（T15 设计文档 §3、制作卡 07 §5）。由 build_stymphalian.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度），位移 [x, y, z]（u）；写入时经 axes.js 换算。只写左翼，右翼自动镜像（ry、rz 取反）。
// rx 为正：骨骼的 +Y 端向前倾、+Z 端向下；腿（自然下垂）rx 为正 = 向后摆。
// 绑定姿势是双翼平展；收翼角度由目标方向数值求解（scratchpad birdsolve.py：给每段的世界方向与飞羽朝向，
// 正交化后分解为 X→Y→Z 欧拉角），注释里写目标方向。收翼与展翼之间经过 HALF，避免逐分量插值时扭转。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const ZERO = [0, 0, 0];

    /** 收翼：内翼 (0.12, 0.02, -1)、羽片朝 (0.3, -1, 0) 盖住两胁；外翼 (0.05, -0.25, -1) 向后下垂越过尾部。 */
    const FOLD = { wing_l: [-97.2, 83.1, 9.5], wing_l_hand: [2.7, 13.4, 8.2] };
    /** 半举（过渡、抖羽）：内翼 (0.45, 0.85, -0.3)、外翼 (0.6, 0.6, -0.5)。 */
    const HALF = { wing_l: [-5.4, 17.3, 62.1], wing_l_hand: [0.5, 15.2, -13.8] };
    /** 扇翼：上扇顶点、下扇底点；滑翔：平展略上反。 */
    const UP = { wing_l: [0, 6, 38], wing_l_hand: [0, -4, 14] };
    const DOWN = { wing_l: [0, -6, -32], wing_l_hand: [0, 4, -16] };
    const BIG_UP = { wing_l: [0, 8, 55], wing_l_hand: [0, -6, 20] };
    const BIG_DOWN = { wing_l: [0, -8, -45], wing_l_hand: [0, 6, -22] };
    const GLIDE = { wing_l: [0, 4, 8], wing_l_hand: [0, -2, 4] };
    const GLIDE2 = { wing_l: [0, 4, 11], wing_l_hand: [0, -2, 2] };
    /** 抖羽：半举附近 ±6° 高频抖动，外翼羽尖跟着颤。 */
    const SHAKE_A = { wing_l: [-5.4, 17.3, 68.0], wing_l_hand: [0.5, 15.2, -8.0] };
    const SHAKE_B = { wing_l: [-5.4, 17.3, 56.0], wing_l_hand: [0.5, 15.2, -19.0] };
    /** 射羽：双翼猛地向前下扇。 */
    const SNAP = { wing_l: [0, -24, -40], wing_l_hand: [0, -10, -18] };

    /** 飞行：颈向前平伸、头放平，腿向后拖。 */
    const FLIGHT = {
        neck: [58, 0, 0], head: [-50, 0, 0],
        leg_l: [80, 0, 0], leg_l_lower: [10, 0, 0], leg_r: [80, 0, 0], leg_r_lower: [10, 0, 0],
    };

    const mirror = (v) => [v[0], -v[1], -v[2]];
    /** 左翼的关键帧表 [[时刻, 姿势], …] → 左右两翼的骨骼通道。 */
    /** 收翼时内翼缩到 0.85 × 1 × 0.6（骨骼本地轴：翼长、厚、羽长），羽片不再垂到腿上；其余姿势 1 × 1 × 1。 */
    const FOLD_SCALE = [0.85, 1, 0.6];
    const ONE = [1, 1, 1];
    const wings = (frames) => {
        const out = {};
        for (const seg of ['wing_l', 'wing_l_hand']) {
            out[seg] = { rotation: frames.map(([t, pose]) => [t, pose[seg]]) };
            out[seg.replace('_l', '_r')] = { rotation: frames.map(([t, pose]) => [t, mirror(pose[seg])]) };
        }
        const sc = frames.map(([t, pose]) => [t, pose === FOLD ? FOLD_SCALE : ONE]);
        out.wing_l.scale = sc;
        out.wing_r.scale = sc;
        return out;
    };
    const hold = (pose, times) => Object.fromEntries(Object.entries(pose).map(([b, v]) => [b, { rotation: times.map((t) => [t, v]) }]));
    const merge = (...parts) => Object.assign({}, ...parts);
    const shake = (len, step) => {
        const fr = [[0, HALF]];
        let i = 0;
        for (let t = step; t < len - 1e-6; t += step) fr.push([Math.round(t * 100) / 100, i++ % 2 ? SHAKE_B : SHAKE_A]);
        fr.push([len, HALF]);
        return fr;
    };

    // 走路一循环 0.8 秒：左右腿反相；抬腿时跗跖向后折
    const STEP = 0.8;
    const legWalk = (side, phase) => {
        const t = (k) => Math.round((((k + phase) % 1) * STEP) * 100) / 100;
        const frames = [[t(0), [-28, 0, 0], [28, 0, 0]], [t(0.25), [-12, 0, 0], [0, 0, 0]], [t(0.5), [0, 0, 0], [0, 0, 0]],
            [t(0.75), [18, 0, 0], [-18, 0, 0]]].sort((a, b) => a[0] - b[0]);
        const first = frames[0];
        const keys = first[0] < 1e-6 ? [...frames, [STEP, first[1], first[2]]] : [[0, frames[frames.length - 1][1], frames[frames.length - 1][2]], ...frames, [STEP, frames[frames.length - 1][1], frames[frames.length - 1][2]]];
        return {
            ['leg_' + side]: { rotation: keys.map((k) => [k[0], k[1]]) },
            ['leg_' + side + '_lower']: { rotation: keys.map((k) => [k[0], k[2]]) },
        };
    };

    const ANIMS = {
        // 站立：收翼，颈轻轻伸缩，头左右看
        idle: {
            length: 4.0,
            loop: 'loop',
            bones: merge(wings([[0, FOLD], [4.0, FOLD]]), {
                neck: { rotation: [[0, ZERO], [1.0, [-4, 0, 0]], [2.0, ZERO], [4.0, ZERO]] },
                head: { rotation: [[0, ZERO], [2.2, [0, 25, 0]], [2.8, [0, 25, 0]], [3.2, [0, -20, 0]], [3.7, [0, -20, 0]], [4.0, ZERO]] },
                tail: { rotation: [[0, ZERO], [2.0, [-3, 0, 0]], [4.0, ZERO]] },
            }),
        },
        // 地面行走：左右腿反相，颈随步前后点
        walk: {
            length: STEP,
            loop: 'loop',
            bones: merge(wings([[0, FOLD], [STEP, FOLD]]), legWalk('l', 0), legWalk('r', 0.5), {
                neck: { rotation: [[0, [8, 0, 0]], [STEP / 4, [-4, 0, 0]], [STEP / 2, [8, 0, 0]], [STEP * 0.75, [-4, 0, 0]], [STEP, [8, 0, 0]]] },
                head: { rotation: [[0, [-8, 0, 0]], [STEP / 4, [4, 0, 0]], [STEP / 2, [-8, 0, 0]], [STEP * 0.75, [4, 0, 0]], [STEP, [-8, 0, 0]]] },
                body: { position: [[0, ZERO], [STEP / 4, [0, 0.4, 0]], [STEP / 2, ZERO], [STEP * 0.75, [0, 0.4, 0]], [STEP, ZERO]] },
            }),
        },
        // 扇翼盘旋：0.6 秒一扇，下扇时身体略上抬
        fly: {
            length: 0.6,
            loop: 'loop',
            bones: merge(wings([[0, UP], [0.3, DOWN], [0.6, UP]]), hold(FLIGHT, [0]), {
                body: { position: [[0, ZERO], [0.3, [0, 0.8, 0]], [0.6, ZERO]] },
                tail: { rotation: [[0, [6, 0, 0]], [0.3, [2, 0, 0]], [0.6, [6, 0, 0]]] },
            }),
        },
        // 滑翔：双翼平展略上反，翼梢随气流轻颤
        glide: {
            length: 2.0,
            loop: 'loop',
            bones: merge(wings([[0, GLIDE], [1.0, GLIDE2], [2.0, GLIDE]]), hold(FLIGHT, [0]), {
                tail: { rotation: [[0, [4, 0, 0]], [1.0, [4, 4, 0]], [2.0, [4, 0, 0]]] },
            }),
        },
        // 抖羽 0.8 秒（齐射前摇）：半举双翼 0.1 秒一抖，尾羽张开上翘，头低下瞄准
        rattle: {
            length: 0.8,
            loop: 'loop',
            bones: merge(wings(shake(0.8, 0.1)), hold(FLIGHT, [0]), {
                neck: { rotation: [[0, [58, 0, 0]], [0.8, [70, 0, 0]]] },
                head: { rotation: [[0, [-50, 0, 0]], [0.8, [-30, 0, 0]]] },
                tail: { rotation: [[0, [-12, 0, 0]], [0.8, [-12, 0, 0]]] },
            }),
        },
        // 射羽 0.3 秒：从半举猛地向前下扇，再回到上扇
        shoot: {
            length: 0.3,
            loop: 'hold_on_last_frame',
            bones: merge(wings([[0, HALF], [0.1, SNAP], [0.3, UP]]), hold(FLIGHT, [0]), {
                body: { rotation: [[0, ZERO], [0.1, [8, 0, 0]], [0.3, ZERO]] },
            }),
        },
        // 受惊高飞：0.35 秒一扇、幅度大，颈上扬
        startle: {
            length: 0.35,
            loop: 'loop',
            bones: merge(wings([[0, BIG_UP], [0.175, BIG_DOWN], [0.35, BIG_UP]]), hold(FLIGHT, [0]), {
                neck: { rotation: [[0, [30, 0, 0]]] },
                head: { rotation: [[0, [-20, 0, 0]]] },
                body: { rotation: [[0, [-18, 0, 0]]] },
            }),
        },
        // 夜间栖息：收翼，头收到背上（喙朝后），右腿收进腹下
        roost: {
            length: 4.0,
            loop: 'loop',
            bones: merge(wings([[0, FOLD], [4.0, FOLD]]), {
                neck: { rotation: [[0, [-60, 0, 0]]] },
                head: { rotation: [[0, [-110, 0, 0]]] },
                leg_r: { rotation: [[0, [-35, 0, 0]]] },
                leg_r_lower: { rotation: [[0, [140, 0, 0]]] },
                body: { position: [[0, ZERO], [2.0, [0, 0.3, 0]], [4.0, ZERO]] },
            }),
        },
    };
    MA.STYMPHALIAN_ANIMS = ANIMS;

    function build() {
        for (const a of Animation.all.slice()) {
            a.remove(false);
        }
        for (const [name, def] of Object.entries(ANIMS)) {
            const anim = new Animation({ name, loop: def.loop, length: def.length, snapping: 20 }).add();
            for (const [boneName, channels] of Object.entries(def.bones)) {
                const group = MA.bones[boneName] || Group.all.find((g) => g.name === boneName);
                if (!group) {
                    throw new Error('动画 ' + name + ' 引用了不存在的骨骼 ' + boneName);
                }
                const animator = anim.getBoneAnimator(group);
                for (const [channel, frames] of Object.entries(channels)) {
                    for (const [time, v] of frames) {
                        const val = channel === 'rotation' ? A.rot(v) : channel === 'position' ? A.pos(v) : v;
                        animator.addKeyframe({ channel, time, interpolation: 'linear', data_points: [{ x: val[0], y: val[1], z: val[2] }] });
                    }
                }
            }
        }
    }

    const box = (c) => {
        c.mesh.updateMatrixWorld(true);
        return new THREE.Box3().setFromObject(c.mesh);
    };
    /** 地面动画全程没有方块低于地面 0.3u 以下。返回各动画的最低点与最高点（u）。 */
    function check(anim) {
        const ground = ['idle', 'walk', 'roost'].includes(anim.name);
        let lo = Infinity;
        let hi = -Infinity;
        for (let t = 0; t <= anim.length + 1e-6; t += anim.length / 8) {
            anim.select();
            Timeline.setTime(t);
            Animator.preview();
            for (const c of Cube.all) {
                const b = box(c);
                hi = Math.max(hi, b.max.y);
                lo = Math.min(lo, b.min.y);
                if (ground && b.min.y < -0.3) {
                    throw new Error('动画 ' + anim.name + ' 在 ' + t.toFixed(2) + ' 秒：' + c.name + ' 低于地面 ' + b.min.y.toFixed(2));
                }
            }
        }
        return [Math.round(lo * 10) / 10, Math.round(hi * 10) / 10];
    }

    MA.stymphalianAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        MA.stymphalianAnimReport = {};
        for (const a of Animation.all) {
            MA.stymphalianAnimReport[a.name] = check(a);
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };
    return 'stymphalian animations defined';
})();
