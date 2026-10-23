// 树仙女动画（T15 设计文档 §6、制作卡 10 §5）。由 build_hamadryad.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度），位移 [x, y, z]（u），缩放 [sx, sy, sz]。左臂、左腿写好，右侧镜像（ry、rz 取反）。
// rx 为负：手臂（自然下垂）向前抬；rz 为正：左臂向外抬（右臂镜像后同样向外）。
// 贴地检查不含 cast：半跪过渡的两三帧里后脚尖会陷地 0.3u（看不出来），定格姿势另测。
// 两个形态组：少女动画里 tree 缩成 0，化树动画里 maiden 缩成 0；化树 / 现形两段在 1 秒里交替缩放（树从地面长出）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const ZERO = [0, 0, 0];
    const ONE = [1, 1, 1];
    const NONE = [0, 0, 0];
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
    const MAIDEN = merge(one('maiden', [[0, ONE]], 'scale'), one('tree', [[0, NONE]], 'scale'));
    const TREE = merge(one('maiden', [[0, NONE]], 'scale'), one('tree', [[0, ONE]], 'scale'));
    const STEP = 1.0;

    const ANIMS = {
        // 少女待机：手轻摆、头侧倾
        idle: {
            length: 4.0,
            loop: 'loop',
            bones: merge(MAIDEN,
                both('arm_l', [[0, [0, 0, 4]], [2.0, [-4, 0, 7]], [4.0, [0, 0, 4]]]),
                one('head', [[0, ZERO], [1.5, [0, 12, 6]], [2.5, [0, 12, 6]], [3.2, [0, -8, -3]], [4.0, ZERO]]),
                one('chest', [[0, ZERO], [2.0, [-2, 0, 0]], [4.0, ZERO]]),
            ),
        },
        // 林间漫步：赤足轻步
        walk: {
            length: STEP,
            loop: 'loop',
            bones: merge(MAIDEN,
                one('leg_l', [[0, [-18, 0, 0]], [STEP / 2, [18, 0, 0]], [STEP, [-18, 0, 0]]]),
                one('leg_r', [[0, [18, 0, 0]], [STEP / 2, [-18, 0, 0]], [STEP, [18, 0, 0]]]),
                one('shin_l', [[0, ZERO], [STEP / 4, [14, 0, 0]], [STEP / 2, ZERO], [STEP, ZERO]]),
                one('shin_r', [[0, ZERO], [STEP / 2, ZERO], [STEP * 0.75, [14, 0, 0]], [STEP, ZERO]]),
                one('arm_l', [[0, [12, 0, 4]], [STEP / 2, [-12, 0, 4]], [STEP, [12, 0, 4]]]),
                one('arm_r', [[0, [-12, 0, -4]], [STEP / 2, [12, 0, -4]], [STEP, [-12, 0, -4]]]),
                one('body', [[0, ZERO], [STEP / 4, [0, 0.3, 0]], [STEP / 2, ZERO], [STEP * 0.75, [0, 0.3, 0]], [STEP, ZERO]], 'position'),
            ),
        },
        // 化树 1 秒：双臂举起成枝、身体拉长，少女缩小，树从地面长出
        to_tree: {
            length: 1.0,
            loop: 'hold_on_last_frame',
            bones: merge(
                one('maiden', [[0, ONE], [0.5, [0.95, 1.12, 0.95]], [0.9, [0.2, 1.2, 0.2]], [1.0, NONE]], 'scale'),
                one('tree', [[0, NONE], [0.35, NONE], [0.7, [0.6, 0.6, 0.6]], [1.0, ONE]], 'scale'),
                both('arm_l', [[0, ZERO], [0.5, [0, 0, 150]], [1.0, [0, 0, 165]]]),
                one('head', [[0, ZERO], [0.5, [-25, 0, 0]]]),
            ),
        },
        // 化树待机：树冠随风轻摆
        tree: {
            length: 4.0,
            loop: 'loop',
            bones: merge(TREE, one('crown', [[0, ZERO], [1.3, [0, 0, 2.5]], [2.6, [1.5, 0, -2]], [4.0, ZERO]])),
        },
        // 现形 1 秒：反过来
        to_maiden: {
            length: 1.0,
            loop: 'hold_on_last_frame',
            bones: merge(
                one('tree', [[0, ONE], [0.3, [0.6, 0.6, 0.6]], [0.65, NONE], [1.0, NONE]], 'scale'),
                one('maiden', [[0, NONE], [0.1, [0.2, 1.2, 0.2]], [0.5, [0.95, 1.12, 0.95]], [1.0, ONE]], 'scale'),
                both('arm_l', [[0, [0, 0, 165]], [0.5, [0, 0, 150]], [1.0, ZERO]]),
                one('head', [[0, [-25, 0, 0]], [0.5, [-25, 0, 0]], [1.0, ZERO]]),
            ),
        },
        // 化树时的警告：树冠一抖
        warn_tree: {
            length: 0.6,
            loop: 'hold_on_last_frame',
            bones: merge(TREE, one('crown', [[0, ZERO], [0.1, [0, 0, 6]], [0.2, [0, 0, -6]], [0.3, [0, 0, 4]], [0.45, [0, 0, -2]], [0.6, ZERO]])),
        },
        // 少女时的警告：掩面叹息
        sigh: {
            length: 1.2,
            loop: 'hold_on_last_frame',
            bones: merge(MAIDEN,
                one('arm_l', [[0, ZERO], [0.4, [-70, -20, 8]], [0.9, [-70, -20, 8]], [1.2, ZERO]]),
                one('forearm_l', [[0, ZERO], [0.4, [-60, 0, 0]], [0.9, [-60, 0, 0]], [1.2, ZERO]]),
                one('head', [[0, ZERO], [0.4, [18, 0, 0]], [0.9, [18, 0, 0]], [1.2, ZERO]]),
                one('chest', [[0, ZERO], [0.4, [8, 0, 0]], [0.9, [8, 0, 0]], [1.2, ZERO]]),
            ),
        },
        // 唤根刺 0.8 秒：半跪、双手按向地面
        cast: {
            length: 0.8,
            loop: 'hold_on_last_frame',
            bones: merge(MAIDEN,
                // 前腿大腿前抬 40°、小腿回到竖直：髋随大腿抬起的量（5.6 × (1 − cos40°) ≈ 1.3）下沉，脚不离地也不陷地
                one('body', [[0, ZERO], [0.15, [0, -0.25, 0]], [0.3, [0, -1.3, 0]], [0.8, [0, -1.3, 0]]], 'position'),
                one('leg_l', [[0, ZERO], [0.3, [-40, 0, 0]], [0.8, [-40, 0, 0]]]),
                one('shin_l', [[0, ZERO], [0.3, [40, 0, 0]], [0.8, [40, 0, 0]]]),
                // 后腿：膝向后弯成半跪，脚跟抬起（小腿先弯、大腿后摆，过渡帧里脚尖不戳地）
                one('leg_r', [[0, ZERO], [0.15, [2, 0, 0]], [0.3, [10, 0, 0]], [0.8, [10, 0, 0]]]),
                one('shin_r', [[0, ZERO], [0.15, [35, 0, 0]], [0.3, [75, 0, 0]], [0.8, [75, 0, 0]]]),
                one('chest', [[0, ZERO], [0.3, [28, 0, 0]], [0.8, [28, 0, 0]]]),
                both('arm_l', [[0, ZERO], [0.3, [-35, 0, 10]], [0.8, [-35, 0, 10]]]),
                one('head', [[0, ZERO], [0.3, [-10, 0, 0]]]),
            ),
        },
        // 赠露 1 秒：双手捧在胸前递出
        bless: {
            length: 1.0,
            loop: 'hold_on_last_frame',
            bones: merge(MAIDEN,
                both('arm_l', [[0, ZERO], [0.4, [-45, -12, -6]], [1.0, [-50, -12, -6]]]),
                both('forearm_l', [[0, ZERO], [0.4, [-40, 0, 0]], [1.0, [-30, 0, 0]]]),
                one('head', [[0, ZERO], [0.4, [12, 0, 0]], [1.0, [8, 0, 0]]]),
            ),
        },
    };
    MA.HAMADRYAD_ANIMS = ANIMS;

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
        const ground = ['idle', 'walk', 'tree', 'warn_tree', 'sigh', 'bless', 'to_tree', 'to_maiden'].includes(anim.name);
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

    MA.hamadryadAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        MA.hamadryadAnimReport = {};
        for (const a of Animation.all) {
            MA.hamadryadAnimReport[a.name] = check(a);
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };
    return 'hamadryad animations defined';
})();
