// 希腊居民通用动作（T19 阶段 ①，V3-希腊-居民 §9.2「通用」组的前 8 个）。由 build_resident.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度），位移 [x, y, z]（u）。左侧写好，右侧镜像（ry、rz 取反）。
//   手臂（自然下垂）：rx 为负向前抬；rz 为正向外张（左臂），右臂镜像后同样向外。
//   躯干、头（向上）：rx 为正向前俯。大腿：rx 为负向前抬；小腿：rx 为正向后弯。
// 男女两套服装的下摆都挂在 hem_front / hem_back 下面，摆动一处写就够。
// 睡觉：躺在床上时由渲染器（原版睡姿）放倒整个模型，动画只给放松的姿势；没床时站着打盹也是这一段。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const ZERO = [0, 0, 0];
    const mirror = (v) => [v[0], -v[1], -v[2]];
    const merge = (...parts) => {
        const out = {};
        for (const p of parts) {
            for (const [bone, ch] of Object.entries(p)) out[bone] = Object.assign(out[bone] || {}, ch);
        }
        return out;
    };
    const both = (bone, frames) => ({
        [bone]: { rotation: frames },
        [bone.replace(/_l$/, '_r')]: { rotation: frames.map(([t, v]) => [t, mirror(v)]) },
    });
    const one = (bone, frames, channel = 'rotation') => ({ [bone]: { [channel]: frames } });
    const STEP = 1.0;

    /** 走路的腿与下摆（walk 与 carry_walk 共用）。 */
    const LEGS = merge(
        one('thigh_l', [[0, [-20, 0, 0]], [STEP / 2, [18, 0, 0]], [STEP, [-20, 0, 0]]]),
        one('thigh_r', [[0, [18, 0, 0]], [STEP / 2, [-20, 0, 0]], [STEP, [18, 0, 0]]]),
        one('shin_l', [[0, [4, 0, 0]], [STEP / 4, [22, 0, 0]], [STEP / 2, [2, 0, 0]], [STEP, [4, 0, 0]]]),
        one('shin_r', [[0, [2, 0, 0]], [STEP / 2, [4, 0, 0]], [STEP * 0.75, [22, 0, 0]], [STEP, [2, 0, 0]]]),
        one('foot_l', [[0, [12, 0, 0]], [STEP / 4, [-14, 0, 0]], [STEP / 2, [-10, 0, 0]], [STEP, [12, 0, 0]]]),
        one('foot_r', [[0, [-10, 0, 0]], [STEP / 2, [12, 0, 0]], [STEP * 0.75, [-14, 0, 0]], [STEP, [-10, 0, 0]]]),
        one('hem_front', [[0, [-6, 0, 0]], [STEP / 4, [-12, 0, 0]], [STEP / 2, [-6, 0, 0]], [STEP * 0.75, [-12, 0, 0]], [STEP, [-6, 0, 0]]]),
        one('hem_back', [[0, [6, 0, 0]], [STEP / 4, [3, 0, 0]], [STEP / 2, [6, 0, 0]], [STEP * 0.75, [3, 0, 0]], [STEP, [6, 0, 0]]]),
        one('root', [[0, ZERO], [STEP / 4, [0, 0.4, 0]], [STEP / 2, ZERO], [STEP * 0.75, [0, 0.4, 0]], [STEP, ZERO]], 'position'),
    );

    /** 双手从两侧托住胸前的东西（篮、木料、石料、黏土）：上臂前抬内收、前臂抬平。 */
    const HOLD = merge(
        both('upper_arm_l', [[0, [-28, 0, -8]]]),
        both('forearm_l', [[0, [-50, 0, 0]]]),
        both('hand_l', [[0, [0, 0, -20]]]),
    );

    const ANIMS = {
        // 待机：呼吸、头左右看、手轻摆
        idle: {
            length: 4.0,
            loop: 'loop',
            bones: merge(
                one('torso', [[0, ZERO], [2.0, [-1.5, 0, 0]], [4.0, ZERO]]),
                one('head', [[0, ZERO], [1.2, [0, 14, 0]], [2.0, [0, 14, 0]], [2.8, [2, -10, 0]], [3.4, [2, -10, 0]], [4.0, ZERO]]),
                both('upper_arm_l', [[0, [0, 0, 3]], [2.0, [-3, 0, 5]], [4.0, [0, 0, 3]]]),
                both('forearm_l', [[0, [-4, 0, 0]], [2.0, [-8, 0, 0]], [4.0, [-4, 0, 0]]]),
            ),
        },
        // 走路：一步一秒（按位移调相位由 GeckoLib 的移动判断决定）
        walk: {
            length: STEP,
            loop: 'loop',
            bones: merge(LEGS,
                one('upper_arm_l', [[0, [16, 0, 3]], [STEP / 2, [-16, 0, 3]], [STEP, [16, 0, 3]]]),
                one('upper_arm_r', [[0, [-16, 0, -3]], [STEP / 2, [16, 0, -3]], [STEP, [-16, 0, -3]]]),
                both('forearm_l', [[0, [-10, 0, 0]]]),
                one('torso', [[0, [2, 4, 0]], [STEP / 2, [2, -4, 0]], [STEP, [2, 4, 0]]]),
            ),
        },
        // 搬着东西走：腿照常，手托住，身体略后仰（东西压在胸前）
        carry_walk: {
            length: STEP,
            loop: 'loop',
            bones: merge(LEGS, HOLD,
                one('torso', [[0, [-4, 2, 0]], [STEP / 2, [-4, -2, 0]], [STEP, [-4, 2, 0]]]),
                one('head', [[0, [6, 0, 0]]]),
            ),
        },
        // 睡觉（在床上，模型由渲染器放倒）：放松、慢呼吸、低头
        sleep: {
            length: 4.0,
            loop: 'loop',
            bones: merge(
                one('head', [[0, [12, 0, 4]], [2.0, [14, 0, 4]], [4.0, [12, 0, 4]]]),
                one('torso', [[0, ZERO], [2.0, [-2, 0, 0]], [4.0, ZERO]]),
                both('upper_arm_l', [[0, [-6, 0, 4]]]),
                both('forearm_l', [[0, [-20, 0, 0]]]),
            ),
        },
        // 坐在石凳上（广场、柱廊、剧场看台）：大腿水平、小腿垂直、脚踩地，手搭膝上，前摆盖在腿上、后摆搭在凳面
        sit: {
            length: 4.0,
            loop: 'loop',
            bones: merge(
                one('root', [[0, [0, -5.6, 0]]], 'position'),
                both('thigh_l', [[0, [-88, 0, 4]]]),
                both('shin_l', [[0, [88, 0, 0]]]),
                one('hem_front', [[0, [-84, 0, 0]]]),
                one('hem_back', [[0, [88, 0, 0]]]),
                one('torso', [[0, [4, 0, 0]], [2.0, [2, 0, 0]], [4.0, [4, 0, 0]]]),
                one('head', [[0, [4, 0, 0]], [2.0, [4, 10, 0]], [4.0, [4, 0, 0]]]),
                both('upper_arm_l', [[0, [-26, 0, 2]]]),
                both('forearm_l', [[0, [-48, 0, 0]]]),
            ),
        },
        // 弯腰拾起：屈膝俯身、双手下探，再起身（1 秒）
        haul_pick: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                one('root', [[0, ZERO], [0.45, [0, -1.1, 0]], [0.6, [0, -1.1, 0]], [1.0, ZERO]], 'position'),
                one('torso', [[0, ZERO], [0.45, [38, 0, 0]], [0.6, [38, 0, 0]], [1.0, ZERO]]),
                one('head', [[0, ZERO], [0.45, [-14, 0, 0]], [0.6, [-14, 0, 0]], [1.0, ZERO]]),
                both('thigh_l', [[0, ZERO], [0.45, [-34, 0, 0]], [0.6, [-34, 0, 0]], [1.0, ZERO]]),
                both('shin_l', [[0, ZERO], [0.45, [44, 0, 0]], [0.6, [44, 0, 0]], [1.0, ZERO]]),
                both('foot_l', [[0, ZERO], [0.45, [-12, 0, 0]], [0.6, [-12, 0, 0]], [1.0, ZERO]]),
                both('upper_arm_l', [[0, ZERO], [0.45, [-36, 0, -6]], [0.6, [-36, 0, -6]], [1.0, [-6, 0, 0]]]),
                both('forearm_l', [[0, ZERO], [0.45, [-12, 0, 0]], [0.6, [-24, 0, 0]], [1.0, [-10, 0, 0]]]),
            ),
        },
        // 俯身放下：手里托着的东西往前下方放（1 秒）
        haul_put: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                one('root', [[0, ZERO], [0.5, [0, -0.65, 0]], [0.65, [0, -0.65, 0]], [1.0, ZERO]], 'position'),
                one('torso', [[0, ZERO], [0.5, [30, 0, 0]], [0.65, [30, 0, 0]], [1.0, ZERO]]),
                one('head', [[0, ZERO], [0.5, [-10, 0, 0]], [1.0, ZERO]]),
                both('thigh_l', [[0, ZERO], [0.5, [-26, 0, 0]], [0.65, [-26, 0, 0]], [1.0, ZERO]]),
                both('shin_l', [[0, ZERO], [0.5, [34, 0, 0]], [0.65, [34, 0, 0]], [1.0, ZERO]]),
                both('foot_l', [[0, ZERO], [0.5, [-8, 0, 0]], [0.65, [-8, 0, 0]], [1.0, ZERO]]),
                both('upper_arm_l', [[0, [-28, 0, -8]], [0.5, [-46, 0, -8]], [0.65, [-46, 0, -2]], [1.0, [-4, 0, 2]]]),
                both('forearm_l', [[0, [-50, 0, 0]], [0.5, [-30, 0, 0]], [0.65, [-20, 0, 0]], [1.0, [-6, 0, 0]]]),
            ),
        },
        // 敲打施工：右手抡起再砸下，左手扶住前方（0.8 秒一下）
        build_hammer: {
            length: 0.8,
            loop: 'loop',
            bones: merge(
                one('torso', [[0, [8, -6, 0]], [0.3, [4, -12, 0]], [0.45, [14, 2, 0]], [0.8, [8, -6, 0]]]),
                one('head', [[0, [10, 4, 0]], [0.45, [14, 0, 0]], [0.8, [10, 4, 0]]]),
                one('upper_arm_r', [[0, [-60, 0, -6]], [0.3, [-150, 10, -10]], [0.45, [-50, 0, -6]], [0.8, [-60, 0, -6]]]),
                one('forearm_r', [[0, [-30, 0, 0]], [0.3, [-40, 0, 0]], [0.45, [-10, 0, 0]], [0.8, [-30, 0, 0]]]),
                one('upper_arm_l', [[0, [-42, 0, -6]]]),
                one('forearm_l', [[0, [-34, 0, 0]]]),
                both('thigh_l', [[0, [-8, 0, 2]]]),
                both('shin_l', [[0, [10, 0, 0]]]),
                both('foot_l', [[0, [-2, 0, 0]]]),
            ),
        },
    };

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
    /** 站在地上的动画全程没有可见方块低于地面 0.3u；返回各动画的最低点与最高点（u）。搬运附件平时隐藏，不计。 */
    function check(anim) {
        const ground = anim.name !== 'sleep';
        let lo = Infinity;
        let hi = -Infinity;
        for (let t = 0; t <= anim.length + 1e-6; t += anim.length / 8) {
            anim.select();
            Timeline.setTime(t);
            Animator.preview();
            for (const c of Cube.all) {
                if (c.parent && c.parent.name && c.parent.name.startsWith('carry_')) continue;
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

    MA.residentAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        MA.residentAnimReport = {};
        for (const a of Animation.all) {
            MA.residentAnimReport[a.name] = check(a);
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };
    return 'resident animations defined';
})();
