// 卡尔基诺斯动画（T15 设计文档 §5、制作卡 09 §5）。由 build_karkinos.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度），位移 [x, y, z]（u）；写入时经 axes.js 换算。左侧写好，右侧自动镜像（ry、rz 取反）。
// rx 为正：骨骼的 +Z 端向下（钳子低头 / 指尖合拢）；rx 为负：+Z 端向上（举钳 / 张开可动指）。
// 步足（左侧沿 +X 伸出）：rz 为正 = 膝抬高；ry 为正 = 足向后划。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const ZERO = [0, 0, 0];
    const mirror = (v) => [v[0], -v[1], -v[2]];
    const R = (name) => name.replace(/_l(?=\d|_|$)/, '_r');
    /** 按骨骼合并通道（同一骨骼可以分别给旋转与位移）。 */
    const merge = (...parts) => {
        const out = {};
        for (const p of parts) {
            for (const [bone, ch] of Object.entries(p)) out[bone] = Object.assign(out[bone] || {}, ch);
        }
        return out;
    };
    /** 左侧通道 + 镜像的右侧通道。frames: [[t, [rx, ry, rz]], …] */
    const both = (bone, frames, channel = 'rotation') => ({
        [bone]: { [channel]: frames },
        [R(bone)]: { [channel]: frames.map(([t, v]) => [t, channel === 'rotation' ? mirror(v) : [-v[0], v[1], v[2]]]) },
    });
    const one = (bone, frames, channel = 'rotation') => ({ [bone]: { [channel]: frames } });

    /** 侧行一步：A 组（左 1、3，右 2、4）与 B 组（左 2、4，右 1、3）交替抬起、划动。 */
    const STEP = 0.5;
    const legFrames = (phase) => {
        const up = [[0, [0, -10, 14]], [STEP / 2, [0, 10, 0]], [STEP, [0, -10, 14]]];
        const down = [[0, [0, 10, 0]], [STEP / 2, [0, -10, 14]], [STEP, [0, 10, 0]]];
        return phase ? up : down;
    };
    const lowerFrames = (phase) => {
        const up = [[0, [0, 0, -12]], [STEP / 2, ZERO], [STEP, [0, 0, -12]]];
        const down = [[0, ZERO], [STEP / 2, [0, 0, -12]], [STEP, ZERO]];
        return phase ? up : down;
    };
    const walkLegs = () => {
        const out = {};
        for (let i = 1; i <= 4; i++) {
            const aLeft = i % 2 === 1;
            out['leg_l' + i] = { rotation: legFrames(aLeft) };
            out['leg_l' + i + '_lower'] = { rotation: lowerFrames(aLeft) };
            out['leg_r' + i] = { rotation: legFrames(!aLeft).map(([t, v]) => [t, mirror(v)]) };
            out['leg_r' + i + '_lower'] = { rotation: lowerFrames(!aLeft).map(([t, v]) => [t, mirror(v)]) };
        }
        return out;
    };
    /** 翻倒乱蹬：每条腿相位错开地上下乱划。 */
    const flailLegs = () => {
        const out = {};
        for (let i = 1; i <= 4; i++) {
            for (const side of ['l', 'r']) {
                const ph = (i + (side === 'r' ? 0.5 : 0)) * 0.13;
                const f = (a) => [[0, a], [0.25, [0, -a[1], a[2] - 30]], [0.5, a]];
                const base = [0, 18 * (i % 2 ? 1 : -1), 10];
                const frames = f(base).map(([t, v]) => [Math.round(((t + ph) % 0.5) * 100) / 100, v]).sort((x, y) => x[0] - y[0]);
                frames.unshift([0, frames[frames.length - 1][1]]);
                frames.push([0.5, frames[1][1]]);
                const fr = frames.filter((x, k, arr) => k === 0 || x[0] !== arr[k - 1][0]);
                out['leg_' + side + i] = { rotation: side === 'l' ? fr : fr.map(([t, v]) => [t, mirror(v)]) };
            }
        }
        return out;
    };

    const FLIP_LIFT = 14.0;
    const BURY = -12.2;

    const ANIMS = {
        // 待机：眼柄轻摆、口器颤动、两钳交替微张
        idle: {
            length: 3.0,
            loop: 'loop',
            bones: merge(
                both('eyestalk_l', [[0, ZERO], [0.8, [0, 12, 4]], [1.6, ZERO], [2.2, [-8, -6, 0]], [3.0, ZERO]]),
                one('claw_l_finger', [[0, ZERO], [0.6, [-14, 0, 0]], [1.0, ZERO], [3.0, ZERO]]),
                one('claw_r_finger', [[0, ZERO], [1.6, ZERO], [2.0, [-14, 0, 0]], [2.4, ZERO], [3.0, ZERO]]),
                one('mouth', [[0, ZERO], [0.15, [0, 0, 0.25]], [0.3, ZERO], [1.5, ZERO], [1.65, [0, 0, 0.25]], [1.8, ZERO], [3.0, ZERO]], 'position'),
                one('body', [[0, ZERO], [1.5, [0, 0.25, 0]], [3.0, ZERO]], 'position'),
            ),
        },
        // 侧行：两组步足交替，身体左右轻晃
        walk: {
            length: STEP,
            loop: 'loop',
            bones: merge(walkLegs(),
                one('body', [[0, ZERO], [STEP / 4, [0, 0.3, 0]], [STEP / 2, ZERO], [STEP * 0.75, [0, 0.3, 0]], [STEP, ZERO]], 'position'),
                both('arm_l', [[0, [-6, 0, 0]], [STEP, [-6, 0, 0]]]),
            ),
        },
        // 张钳 0.7 秒：双钳举高张开，身体前压
        pinch_windup: {
            length: 0.7,
            loop: 'hold_on_last_frame',
            bones: merge(
                both('arm_l', [[0, ZERO], [0.4, [-28, -6, 8]], [0.7, [-30, -6, 8]]]),
                both('claw_l', [[0, ZERO], [0.4, [-12, 0, 0]], [0.7, [-14, 0, 0]]]),
                both('claw_l_finger', [[0, ZERO], [0.3, [-40, 0, 0]], [0.7, [-42, 0, 0]]]),
                one('body', [[0, ZERO], [0.4, [6, 0, 0]], [0.7, [7, 0, 0]]]),
                // 前压时前腿会戳进地里：身体同时抬高一点
                one('body', [[0, ZERO], [0.4, [0, 0.6, 0]], [0.7, [0, 0.7, 0]]], 'position'),
                both('eyestalk_l', [[0, ZERO], [0.3, [-15, 0, 10]], [0.7, [-15, 0, 10]]]),
            ),
        },
        // 夹击 0.4 秒：双钳猛地前伸、向内合拢，再收回
        pinch: {
            length: 0.4,
            loop: 'hold_on_last_frame',
            bones: merge(
                both('arm_l', [[0, [-30, -6, 8]], [0.08, [4, -16, 0]], [0.4, ZERO]]),
                both('claw_l', [[0, [-14, 0, 0]], [0.08, [6, -10, 0]], [0.4, ZERO]]),
                both('claw_l_finger', [[0, [-42, 0, 0]], [0.06, [4, 0, 0]], [0.4, ZERO]]),
                one('body', [[0, [7, 0, 0]], [0.08, [10, 0, 0]], [0.4, ZERO]]),
                one('body', [[0, [0, 0.7, 0]], [0.08, [0, 1.0, 1.2]], [0.4, ZERO]], 'position'),
            ),
        },
        // 翻倒 3 秒：整体翻过来腹面朝上，八条腿乱蹬、钳子乱挥
        flipped: {
            length: 0.5,
            loop: 'loop',
            bones: merge(flailLegs(),
                { root: { rotation: [[0, [0, 0, 180]]], position: [[0, [0, FLIP_LIFT, 0]]] } },
                // 翻过来以后「举钳」朝下：钳子往腹面一侧（rx 为正）乱挥，可动指小幅开合，不戳进地里
                both('arm_l', [[0, [22, 10, 0]], [0.25, [-4, -10, 10]], [0.5, [22, 10, 0]]]),
                both('claw_l_finger', [[0, [-12, 0, 0]], [0.25, ZERO], [0.5, [-12, 0, 0]]]),
                both('eyestalk_l', [[0, [0, 0, 20]], [0.25, [0, 0, -10]], [0.5, [0, 0, 20]]]),
            ),
        },
        // 白天埋泥：整体沉到地下，只露伸长的眼柄
        buried: {
            length: 2.0,
            loop: 'loop',
            bones: merge(
                { root: { position: [[0, [0, BURY, 0]]] } },
                both('eyestalk_l', [[0, [0, 2.2, 0]], [2.0, [0, 2.2, 0]]], 'position'),
                both('eyestalk_l', [[0, ZERO], [1.0, [0, 15, 0]], [2.0, ZERO]]),
                both('arm_l', [[0, [-10, 0, 0]]]),
            ),
        },
        // 钻出 0.5 秒：从地下拱出，钳子先出
        emerge: {
            length: 0.5,
            loop: 'hold_on_last_frame',
            bones: merge(
                { root: { position: [[0, [0, BURY, 0]], [0.35, [0, 0.8, 0]], [0.5, ZERO]] } },
                both('eyestalk_l', [[0, [0, 2.2, 0]], [0.5, ZERO]], 'position'),
                both('arm_l', [[0, [-40, 0, 0]], [0.3, [-35, -8, 10]], [0.5, ZERO]]),
                both('claw_l_finger', [[0, ZERO], [0.3, [-35, 0, 0]], [0.5, ZERO]]),
            ),
        },
    };
    MA.KARKINOS_ANIMS = ANIMS;

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
        const ground = ['idle', 'walk', 'pinch_windup', 'pinch', 'flipped'].includes(anim.name);
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

    MA.karkinosAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        MA.karkinosAnimReport = {};
        for (const a of Animation.all) {
            MA.karkinosAnimReport[a.name] = check(a);
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };
    return 'karkinos animations defined';
})();
