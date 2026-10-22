// 天马动画（T14 设计文档 §11.4、制作卡 05 §6）。由 build_pegasus.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度），位移 [x, y, z]（u）；写入时经 axes.js 换算。只写左翼，右翼自动镜像（ry、rz 取反）。
// 四肢自然下垂时：rx 为负 = 向前摆。前腿膝（leg_f*_lower）rx 为正 = 管骨向后折；后腿飞节（leg_h*_lower）rx 为负 = 管骨向前收。
// 绑定姿势是双翼平展；各种收翼姿势的角度由目标方向数值求解（scratchpad wingsolve.py：给每段的世界方向与飞羽朝向，
// 正交化后分解为 X→Y→Z 欧拉角），注释里写目标方向。翅膀在收翼与展翼之间切换时经过 HALF，避免逐分量插值时扭转。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const ZERO = [0, 0, 0];

    /** 收翼（方案 A，用户 2026-10-21 认可）：翼根 (0.2, 0.88, -0.42)、中段 (0.15, -0.55, -0.82)、翼梢 (0.05, 0.75, 0.65)。 */
    const FOLD = { wing_l_root: [-0.8, 25.0, 77.2], wing_l_mid: [-119.5, 73.8, -113.3], wing_l_tip: [-179.4, -16.3, -170.0] };
    /** 室内紧收：翼根 (0.25, 0.2, -0.95)、中段 (0.12, -0.05, -0.99)、翼梢回折 (0.1, 0.15, 0.98)；最高约 24u。 */
    const TIGHT = { wing_l_root: [-47.2, 71.4, 38.7], wing_l_mid: [-4.2, 15.0, 6.6], wing_l_tip: [0.6, -5.9, -167.3] };
    /** 半举（过渡）：翼根 (0.35, 0.93, -0.1)、中段 (0.7, 0.7, -0.15)、翼梢 (0.85, 0.45, 0.25)。 */
    const HALF = { wing_l_root: [-2.0, 5.7, 69.4], wing_l_mid: [-7.5, 4.2, -24.0], wing_l_tip: [-1.3, -18.7, -22.2] };
    /** 高举卷翼（审阅方案 B，用于天眼）：翼根 (0.3, 0.9, -0.3)、中段 (0.12, 0.35, -0.93)、翼梢 (0.05, 0.55, 0.83)。 */
    const RAISED = { wing_l_root: [-5.2, 17.5, 71.6], wing_l_mid: [-6.7, 50.5, 6.1], wing_l_tip: [4.8, -54.5, -171.7] };
    /** 扇翼：上扇顶点、下扇底点；滑翔：平展略上反。 */
    const UP = { wing_l_root: [0, 4, 30], wing_l_mid: [0, 0, 8], wing_l_tip: [0, 0, 10] };
    const DOWN = { wing_l_root: [0, -4, -24], wing_l_mid: [0, 0, -10], wing_l_tip: [0, 0, -16] };
    const GLIDE = { wing_l_root: [0, 6, 8], wing_l_mid: [0, 4, 2], wing_l_tip: [0, -6, 6] };
    const GLIDE2 = { wing_l_root: [0, 6, 11], wing_l_mid: [0, 4, 0], wing_l_tip: [0, -6, 9] };

    /** 飞行时四肢收在身下：前腿向后折、后腿向后伸。 */
    const TUCK = {
        leg_fl: [-40, 0, 0], leg_fl_lower: [100, 0, 0], leg_fr: [-40, 0, 0], leg_fr_lower: [100, 0, 0],
        leg_hl: [30, 0, 0], leg_hl_lower: [-25, 0, 0], leg_hr: [30, 0, 0], leg_hr_lower: [-25, 0, 0],
    };

    const mirror = (v) => [v[0], -v[1], -v[2]];
    /** 左翼的关键帧表 [[时刻, 姿势], …] → 左右两翼的骨骼通道。 */
    const wings = (frames) => {
        const out = {};
        for (const seg of ['root', 'mid', 'tip']) {
            const l = 'wing_l_' + seg;
            out[l] = { rotation: frames.map(([t, pose]) => [t, pose[l]]) };
            out['wing_r_' + seg] = { rotation: frames.map(([t, pose]) => [t, mirror(pose[l])]) };
        }
        return out;
    };
    /** 一个静态姿势表 {骨骼: 旋转} 在若干时刻重复 → 骨骼通道。 */
    const hold = (pose, times) => Object.fromEntries(Object.entries(pose).map(([b, v]) => [b, { rotation: times.map((t) => [t, v]) }]));
    const lift = (pose, d) => Object.assign({}, pose, { wing_l_root: [pose.wing_l_root[0], pose.wing_l_root[1], pose.wing_l_root[2] + d] });
    const merge = (...parts) => Object.assign({}, ...parts);

    // 对角步（快步）一循环 0.8 秒：左前 + 右后同相，右前 + 左后反相。每帧 [时刻, 上段旋转, 下段旋转]
    const STEP = 0.8;
    const fore = (phase) => {
        const t = (k) => ((k + phase) % 1) * STEP;
        // 前腿：向前摆时膝折起（抬蹄），向后摆时伸直
        return [[t(0), [-22, 0, 0], [0, 0, 0]], [t(0.25), [0, 0, 0], [0, 0, 0]], [t(0.5), [18, 0, 0], [12, 0, 0]],
            [t(0.75), [-6, 0, 0], [55, 0, 0]]];
    };
    const hind = (phase) => {
        const t = (k) => ((k + phase) % 1) * STEP;
        return [[t(0), [-18, 0, 0], [-6, 0, 0]], [t(0.25), [0, 0, 0], [0, 0, 0]], [t(0.5), [18, 0, 0], [0, 0, 0]],
            [t(0.75), [-2, 0, 0], [-35, 0, 0]]];
    };
    function interp(a, b, t) {
        const f = (t - a[0]) / (b[0] - a[0]);
        const lerp = (x, y) => x.map((v, i) => Math.round((v + (y[i] - v) * f) * 100) / 100);
        return [lerp(a[1], b[1]), lerp(a[2], b[2])];
    }
    /** 一条腿的关键帧（时刻可能绕回）：按时刻排序，并在 0 与循环末各补一帧使循环连续。 */
    const legChannels = (upperBone, lowerBone, frames) => {
        const sorted = frames.slice().sort((a, b) => a[0] - b[0]);
        const first = sorted[0];
        const last = sorted[sorted.length - 1];
        let keys;
        if (first[0] < 1e-6) {
            keys = [...sorted, [STEP, first[1], first[2]]];
        } else {
            const at0 = interp(last, [first[0] + STEP, first[1], first[2]], STEP);
            keys = [[0, at0[0], at0[1]], ...sorted, [STEP, at0[0], at0[1]]];
        }
        return {
            [upperBone]: { rotation: keys.map((k) => [k[0], k[1]]) },
            [lowerBone]: { rotation: keys.map((k) => [k[0], k[2]]) },
        };
    };

    const ANIMS = {
        // 站立：收翼 A，翅膀随呼吸微微开合；尾巴左右甩，颈部轻点
        idle: {
            length: 4.0,
            loop: 'loop',
            bones: merge(wings([[0, FOLD], [2.0, lift(FOLD, 3)], [4.0, FOLD]]), {
                body: { position: [[0, ZERO], [2.0, [0, 0.25, 0]], [4.0, ZERO]] },
                neck: { rotation: [[0, ZERO], [2.0, [-3, 0, 0]], [4.0, ZERO]] },
                tail: { rotation: [[0, [4, 0, 0]], [1.0, [4, 6, 0]], [3.0, [4, -6, 0]], [4.0, [4, 0, 0]]] },
            }),
        },
        // 地面行走：对角步；收翼随步伐轻颠，头颈一点一点
        walk: {
            length: STEP,
            loop: 'loop',
            bones: merge(
                wings([[0, FOLD], [STEP / 2, lift(FOLD, 2)], [STEP, FOLD]]),
                legChannels('leg_fl', 'leg_fl_lower', fore(0)),
                legChannels('leg_hr', 'leg_hr_lower', hind(0)),
                legChannels('leg_fr', 'leg_fr_lower', fore(0.5)),
                legChannels('leg_hl', 'leg_hl_lower', hind(0.5)),
                {
                    body: { position: [[0, ZERO], [STEP / 4, [0, 0.5, 0]], [STEP / 2, ZERO], [STEP * 0.75, [0, 0.5, 0]], [STEP, ZERO]] },
                    neck: { rotation: [[0, [2, 0, 0]], [STEP / 2, [-3, 0, 0]], [STEP, [2, 0, 0]]] },
                    tail: { rotation: [[0, [8, 0, 0]], [STEP / 2, [8, 4, 0]], [STEP, [8, 0, 0]]] },
                }),
        },
        // 起飞 0.6 秒：后腿蹲、前身抬起；翅膀经半举到上扇顶点，再猛扇一下，四肢收起
        takeoff: {
            length: 0.6,
            loop: 'hold_on_last_frame',
            bones: merge(wings([[0, FOLD], [0.15, HALF], [0.3, UP], [0.45, DOWN], [0.6, UP]]), {
                body: { rotation: [[0, ZERO], [0.2, [-14, 0, 0]], [0.6, [-6, 0, 0]]] },
                leg_hl: { rotation: [[0, ZERO], [0.2, [-25, 0, 0]], [0.6, TUCK.leg_hl]] },
                leg_hl_lower: { rotation: [[0, ZERO], [0.2, [-35, 0, 0]], [0.6, TUCK.leg_hl_lower]] },
                leg_hr: { rotation: [[0, ZERO], [0.2, [-25, 0, 0]], [0.6, TUCK.leg_hr]] },
                leg_hr_lower: { rotation: [[0, ZERO], [0.2, [-35, 0, 0]], [0.6, TUCK.leg_hr_lower]] },
                leg_fl: { rotation: [[0, ZERO], [0.2, [-30, 0, 0]], [0.6, TUCK.leg_fl]] },
                leg_fl_lower: { rotation: [[0, ZERO], [0.2, [70, 0, 0]], [0.6, TUCK.leg_fl_lower]] },
                leg_fr: { rotation: [[0, ZERO], [0.2, [-30, 0, 0]], [0.6, TUCK.leg_fr]] },
                leg_fr_lower: { rotation: [[0, ZERO], [0.2, [70, 0, 0]], [0.6, TUCK.leg_fr_lower]] },
                neck: { rotation: [[0, ZERO], [0.3, [-10, 0, 0]], [0.6, [-8, 0, 0]]] },
                tail: { rotation: [[0, [4, 0, 0]], [0.6, [-30, 0, 0]]] },
            }),
        },
        // 扇翼巡飞：0.8 秒一扇，下扇时身体略上抬
        fly: {
            length: 0.8,
            loop: 'loop',
            bones: merge(wings([[0, UP], [0.4, DOWN], [0.8, UP]]), hold(TUCK, [0]), {
                body: { position: [[0, ZERO], [0.4, [0, 1, 0]], [0.8, ZERO]] },
                neck: { rotation: [[0, [-8, 0, 0]], [0.4, [-5, 0, 0]], [0.8, [-8, 0, 0]]] },
                tail: { rotation: [[0, [-35, 0, 0]], [0.4, [-28, 0, 0]], [0.8, [-35, 0, 0]]] },
            }),
        },
        // 滑翔：翅膀平展略上反，翼梢随气流轻颤
        glide: {
            length: 2.0,
            loop: 'loop',
            bones: merge(wings([[0, GLIDE], [1.0, GLIDE2], [2.0, GLIDE]]), hold(TUCK, [0]), {
                neck: { rotation: [[0, [-6, 0, 0]]] },
                tail: { rotation: [[0, [-38, 0, 0]], [1.0, [-36, 3, 0]], [2.0, [-38, 0, 0]]] },
            }),
        },
        // 落地 0.5 秒：翅膀上举刹车、前腿前伸着地，最后收翼
        land: {
            length: 0.5,
            loop: 'hold_on_last_frame',
            bones: merge(wings([[0, UP], [0.2, HALF], [0.5, FOLD]]), {
                body: { rotation: [[0, [-10, 0, 0]], [0.25, [4, 0, 0]], [0.5, ZERO]] },
                leg_fl: { rotation: [[0, [-35, 0, 0]], [0.25, [-8, 0, 0]], [0.5, ZERO]] },
                leg_fl_lower: { rotation: [[0, [20, 0, 0]], [0.25, ZERO], [0.5, ZERO]] },
                leg_fr: { rotation: [[0, [-35, 0, 0]], [0.25, [-8, 0, 0]], [0.5, ZERO]] },
                leg_fr_lower: { rotation: [[0, [20, 0, 0]], [0.25, ZERO], [0.5, ZERO]] },
                leg_hl: { rotation: [[0, [10, 0, 0]], [0.25, [-10, 0, 0]], [0.5, ZERO]] },
                leg_hl_lower: { rotation: [[0, [-10, 0, 0]], [0.25, [-20, 0, 0]], [0.5, ZERO]] },
                leg_hr: { rotation: [[0, [10, 0, 0]], [0.25, [-10, 0, 0]], [0.5, ZERO]] },
                leg_hr_lower: { rotation: [[0, [-10, 0, 0]], [0.25, [-20, 0, 0]], [0.5, ZERO]] },
                tail: { rotation: [[0, [-20, 0, 0]], [0.5, [4, 0, 0]]] },
            }),
        },
        // 天眼 1 秒：抬头远望，双翼高举成卷翼（方案 B 的姿势），眼睛金光由发光层给出
        sky_eye: {
            length: 1.0,
            loop: 'hold_on_last_frame',
            bones: merge(wings([[0, FOLD], [0.25, RAISED], [0.75, RAISED], [1.0, FOLD]]), {
                neck: { rotation: [[0, ZERO], [0.25, [-22, 0, 0]], [0.75, [-22, 0, 0]], [1.0, ZERO]] },
                head: { rotation: [[0, ZERO], [0.25, [-12, 0, 0]], [0.75, [-12, 0, 0]], [1.0, ZERO]] },
            }),
        },
        // 室内紧收：翅膀贴背、翼梢回折，不超过 25u
        fold: {
            length: 4.0,
            loop: 'loop',
            bones: merge(wings([[0, TIGHT], [4.0, TIGHT]]), {
                neck: { rotation: [[0, [4, 0, 0]], [2.0, [1, 0, 0]], [4.0, [4, 0, 0]]] },
                tail: { rotation: [[0, [4, 0, 0]], [1.0, [4, 6, 0]], [3.0, [4, -6, 0]], [4.0, [4, 0, 0]]] },
            }),
        },
    };
    MA.PEGASUS_ANIMS = ANIMS;

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
    /** 地面动画全程没有方块低于地面 0.3u 以下；室内紧收时翅膀不高于头顶（29.9u），整体在 2 格（32u）以内。返回各动画的最低点与最高点（u）。 */
    function check(anim) {
        const ground = ['idle', 'walk', 'sky_eye', 'fold'].includes(anim.name);
        let lo = Infinity;
        let hi = -Infinity;
        for (let t = 0; t <= anim.length + 1e-6; t += anim.length / 8) {
            anim.select();
            Timeline.setTime(t);
            Animator.preview();
            let top = -Infinity;
            let wingTop = -Infinity;
            for (const c of Cube.all) {
                const b = box(c);
                top = Math.max(top, b.max.y);
                if (/wing|sec_|pri_|cur_/.test(c.name)) wingTop = Math.max(wingTop, b.max.y);
                lo = Math.min(lo, b.min.y);
                if (ground && b.min.y < -0.3) {
                    throw new Error('动画 ' + anim.name + ' 在 ' + t.toFixed(2) + ' 秒：' + c.name + ' 低于地面 ' + b.min.y.toFixed(2));
                }
            }
            hi = Math.max(hi, top);
            if (anim.name === 'fold' && (wingTop > 29.9 || top > 32)) {
                throw new Error('室内紧收：翅膀最高 ' + wingTop.toFixed(1) + 'u、整体 ' + top.toFixed(1) + 'u（应 ≤ 29.9 / 32）');
            }
        }
        return [Math.round(lo * 10) / 10, Math.round(hi * 10) / 10];
    }

    MA.pegasusAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        MA.pegasusAnimReport = {};
        for (const a of Animation.all) {
            MA.pegasusAnimReport[a.name] = check(a);
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };
    return 'pegasus animations defined';
})();
