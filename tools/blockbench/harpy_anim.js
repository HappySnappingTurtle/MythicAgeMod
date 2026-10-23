// 哈耳庇厄动画（T15 设计文档 §4、制作卡 08 §5）。由 build_harpy.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度），位移 [x, y, z]（u）；写入时经 axes.js 换算。只写左翼，右翼自动镜像（ry、rz 取反）。
// rx 为正：骨骼的 +Y 端向前倾、+Z 端向下；腿（自然下垂）rx 为正 = 向后摆。
// 翼臂三段（wing_l 上臂、wing_l_fore 前臂、wing_l_hand 手）的姿势按目标方向数值求解：给每段的世界方向（段轴 = 本地 +X）
// 与飞羽朝向（本地 −Z），扣掉父骨骼（chest、body）的旋转后分解为 X→Y→Z 欧拉角（R = Rz·Ry·Rx）；注释里写目标方向。
// v1（白模审阅）：idle、fly、glide、screech、dive；v1.1（白模通过后）：carry、swipe、stunned。地面跳行不做（她在地上只有栖息与眩晕）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const ZERO = [0, 0, 0];

    /** 收翼（上身前倾 12°）：上臂 (0.25, −0.55, −0.8) 沿体侧向后下；前臂 (0.12, −0.2, 0.97) 折回向前；
     *  手 (0.08, −0.15, −1) 再向后，初级飞羽垂在羽裙与尾上。三段飞羽都朝下（像披风）。 */
    const FOLD = { wing_l: [-164.7, 41.8, -70.5], wing_l_fore: [8.3, 44.9, -153.8], wing_l_hand: [-2.5, 19.8, 167.7] };
    /** 半举（尖啸，身体前倾 10°）：上臂 (0.6, 0.75, −0.25)、前臂 (0.75, 0.45, −0.45)、手 (0.6, 0.2, −0.75)，飞羽朝后。 */
    const HALF = { wing_l: [-6.7, 22.3, 49.2], wing_l_fore: [5.1, 13.4, -18.6], wing_l_hand: [6.2, 23.1, -8.7] };
    /** 俯冲（身体前倾 70°）：三段后掠贴身 (0.41, 0.14, −0.9) → (0.05, 0.05, −1)，飞羽斜向内上 (−0.85, 0.52, 0)，
     *  两翼在背上合成屋脊状（不竖成一面墙）。 */
    const DIVE = { wing_l: [-45.5, 25.9, -63.1], wing_l_fore: [-3.0, 11.6, -10.8], wing_l_hand: [-0.4, 3.3, -5.3] };
    /** 扇翼：上扇顶点、下扇底点；滑翔：平展略上反。 */
    const UP = { wing_l: [0, 6, 38], wing_l_fore: [0, -4, 10], wing_l_hand: [0, -4, 14] };
    const DOWN = { wing_l: [0, -6, -32], wing_l_fore: [0, 4, -8], wing_l_hand: [0, 4, -16] };
    const GLIDE = { wing_l: [0, 4, 8], wing_l_fore: [0, -2, 2], wing_l_hand: [0, -2, 4] };
    const GLIDE2 = { wing_l: [0, 4, 11], wing_l_fore: [0, -2, 1], wing_l_hand: [0, -2, 2] };

    /** 眩晕：整体前倒 85° 后飞羽会朝上竖起，所以上臂绕自身轴转回 −85°，让三段飞羽摊平在地上（翼尖略垂）；
     *  STUN_LIFT 把身体抬到不陷地（构建时检查）。 */
    const STUN = { wing_l: [-85, 0, -10], wing_l_fore: [0, 0, -4], wing_l_hand: [0, 0, -4] };
    const STUN2 = { wing_l: [-85, 0, -7], wing_l_fore: [0, 0, -4], wing_l_hand: [0, 0, -6] };
    const STUN_LIFT = 2.1;

    /** 飞行：身体前倾 30°，颈、头抬回平视；双腿向后收、跗跖折起；头发向后飘。 */
    const FLIGHT = {
        body: [30, 0, 0], neck: [-10, 0, 0], head: [-18, 0, 0], hair: [35, 0, 0],
        leg_l: [36, 0, 0], leg_l_lower: [40, 0, 0], leg_r: [36, 0, 0], leg_r_lower: [40, 0, 0],
    };

    const SEGS = ['wing_l', 'wing_l_fore', 'wing_l_hand'];
    const mirror = (v) => [v[0], -v[1], -v[2]];
    /** 左翼的关键帧表 [[时刻, 姿势], …] → 左右两翼的骨骼通道。 */
    const wings = (frames) => {
        const out = {};
        for (const seg of SEGS) {
            out[seg] = { rotation: frames.map(([t, pose]) => [t, pose[seg]]) };
            out[seg.replace('_l', '_r')] = { rotation: frames.map(([t, pose]) => [t, mirror(pose[seg])]) };
        }
        return out;
    };
    const hold = (pose, times) => Object.fromEntries(Object.entries(pose).map(([b, v]) => [b, { rotation: times.map((t) => [t, v]) }]));
    const merge = (...parts) => Object.assign({}, ...parts);
    const legs = (l, lower, foot) => ({
        leg_l: { rotation: [[0, l]] }, leg_r: { rotation: [[0, mirror(l)]] },
        leg_l_lower: { rotation: [[0, lower]] }, leg_r_lower: { rotation: [[0, mirror(lower)]] },
        foot_l: { rotation: [[0, foot]] }, foot_r: { rotation: [[0, mirror(foot)]] },
    });

    const ANIMS = {
        // 栖息：上身前倾 12°、收翼，头转动张望，头发轻摆
        idle: {
            length: 4.0,
            loop: 'loop',
            bones: merge(wings([[0, FOLD], [4.0, FOLD]]), {
                chest: { rotation: [[0, [12, 0, 0]], [2.0, [14, 0, 0]], [4.0, [12, 0, 0]]] },
                neck: { rotation: [[0, [-4, 0, 0]]] },
                head: { rotation: [[0, [-8, 0, 0]], [1.6, [-8, 30, 0]], [2.2, [-8, 30, 0]], [2.8, [-8, -25, 0]], [3.4, [-8, -25, 0]], [4.0, [-8, 0, 0]]] },
                hair: { rotation: [[0, [2, 0, 0]], [2.0, [6, 0, 3]], [4.0, [2, 0, 0]]] },
            }),
        },
        // 盘旋：0.5 秒一扇，下扇时身体上抬
        fly: {
            length: 0.5,
            loop: 'loop',
            bones: merge(wings([[0, UP], [0.25, DOWN], [0.5, UP]]), hold(FLIGHT, [0]), {
                root: { position: [[0, ZERO], [0.25, [0, 0.8, 0]], [0.5, ZERO]] },
                tail: { rotation: [[0, [6, 0, 0]], [0.25, [0, 0, 0]], [0.5, [6, 0, 0]]] },
            }),
        },
        // 滑翔：双翼平展略上反
        glide: {
            length: 2.0,
            loop: 'loop',
            bones: merge(wings([[0, GLIDE], [1.0, GLIDE2], [2.0, GLIDE]]), hold(FLIGHT, [0])),
        },
        // 尖啸 0.8 秒（俯冲预警）：近直立悬停，翼臂半举，仰头张口，双足前伸张爪
        screech: {
            length: 0.8,
            loop: 'hold_on_last_frame',
            bones: merge(wings([[0, UP], [0.25, HALF], [0.8, HALF]]), legs([-55, 0, 0], [20, 0, 0], [-25, 0, 0]), {
                body: { rotation: [[0, [30, 0, 0]], [0.25, [10, 0, 0]], [0.8, [10, 0, 0]]] },
                neck: { rotation: [[0, ZERO], [0.25, [-12, 0, 0]]] },
                head: { rotation: [[0, ZERO], [0.25, [-22, 0, 0]], [0.8, [-26, 0, 0]]] },
                jaw: { rotation: [[0, ZERO], [0.2, [30, 0, 0]], [0.8, [32, 0, 0]]] },
                hair: { rotation: [[0, [35, 0, 0]], [0.8, [20, 0, 6]]] },
            }),
        },
        // 俯冲：身体前倾 70°，翼臂后掠贴身，双足前伸、爪张开对准目标
        dive: {
            length: 1.0,
            loop: 'loop',
            bones: merge(wings([[0, DIVE], [1.0, DIVE]]), legs([-142, 0, 0], [30, 0, 0], [-30, 0, 0]), {
                body: { rotation: [[0, [70, 0, 0]]] },
                neck: { rotation: [[0, [-20, 0, 0]]] },
                head: { rotation: [[0, [-35, 0, 0]]] },
                jaw: { rotation: [[0, [20, 0, 0]]] },
                hair: { rotation: [[0, [40, 0, 0]], [0.5, [44, 0, 4]], [1.0, [40, 0, 0]]] },
            }),
        },
        // 抓着目标飞：上身近直立，0.3 秒一扇的短促扇翼，双腿下垂、趾爪收拢
        carry: {
            length: 0.3,
            loop: 'loop',
            bones: merge(wings([[0, UP], [0.15, DOWN], [0.3, UP]]), legs([-8, 0, 0], [6, 0, 0], [40, 0, 0]), {
                body: { rotation: [[0, [15, 0, 0]]] },
                neck: { rotation: [[0, [-8, 0, 0]]] },
                head: { rotation: [[0, [-10, 0, 0]]] },
                hair: { rotation: [[0, [25, 0, 0]], [0.15, [30, 0, 0]], [0.3, [25, 0, 0]]] },
                root: { position: [[0, ZERO], [0.15, [0, 0.6, 0]], [0.3, ZERO]] },
            }),
        },
        // 爪击 0.5 秒（不抓举时）：从俯冲姿势双足猛地前蹬，再收腿扇翼拉起
        swipe: {
            length: 0.5,
            loop: 'hold_on_last_frame',
            bones: merge(wings([[0, DIVE], [0.15, DIVE], [0.3, DOWN], [0.5, UP]]), {
                body: { rotation: [[0, [70, 0, 0]], [0.15, [45, 0, 0]], [0.5, [30, 0, 0]]] },
                neck: { rotation: [[0, [-20, 0, 0]], [0.5, [-10, 0, 0]]] },
                head: { rotation: [[0, [-35, 0, 0]], [0.5, [-18, 0, 0]]] },
                jaw: { rotation: [[0, [20, 0, 0]], [0.15, [30, 0, 0]], [0.5, ZERO]] },
                leg_l: { rotation: [[0, [-142, 0, 0]], [0.15, [-100, 0, 0]], [0.5, [36, 0, 0]]] },
                leg_r: { rotation: [[0, [-142, 0, 0]], [0.15, [-100, 0, 0]], [0.5, [36, 0, 0]]] },
                leg_l_lower: { rotation: [[0, [30, 0, 0]], [0.15, [0, 0, 0]], [0.5, [40, 0, 0]]] },
                leg_r_lower: { rotation: [[0, [30, 0, 0]], [0.15, [0, 0, 0]], [0.5, [40, 0, 0]]] },
                foot_l: { rotation: [[0, [-30, 0, 0]], [0.15, [-45, 0, 0]], [0.3, [35, 0, 0]], [0.5, ZERO]] },
                foot_r: { rotation: [[0, [-30, 0, 0]], [0.15, [-45, 0, 0]], [0.3, [35, 0, 0]], [0.5, ZERO]] },
                hair: { rotation: [[0, [40, 0, 0]], [0.5, [35, 0, 0]]] },
            }),
        },
        // 坠地眩晕 2 秒：扑倒在地（整体前倒 85°），双翼摊在地上，头侧过来晃
        stunned: {
            length: 2.0,
            loop: 'loop',
            bones: merge(wings([[0, STUN], [1.0, STUN2], [2.0, STUN]]), legs([4, 0, 6], [10, 0, 0], [30, 0, 0]), {
                // 前倒后腿自然平伸在身后；尾羽转回贴地（抵消前倒的 85°）
                tail: { rotation: [[0, [-80, 0, 0]]] },
                root: { rotation: [[0, [85, 0, 0]]], position: [[0, [0, STUN_LIFT, 0]]] },
                head: { rotation: [[0, [0, 70, 0]], [0.5, [0, 62, 6]], [1.0, [0, 70, 0]], [1.5, [0, 76, -6]], [2.0, [0, 70, 0]]] },
                jaw: { rotation: [[0, [12, 0, 0]]] },
                hair: { rotation: [[0, [-30, 0, 0]]] },
            }),
        },
    };
    MA.HARPY_ANIMS = ANIMS;

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
        const ground = ['idle', 'stunned'].includes(anim.name);
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

    MA.harpyAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        MA.harpyAnimReport = {};
        for (const a of Animation.all) {
            MA.harpyAnimReport[a.name] = check(a);
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };
    return 'harpy animations defined';
})();
