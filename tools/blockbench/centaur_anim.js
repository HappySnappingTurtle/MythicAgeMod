// 半人马动画（T14 设计文档 §6.3、§11.5，制作卡 06 §5）。由 build_centaur.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度），位移 [x, y, z]（u）；写入时经 axes.js 换算。
// 手臂自然下垂时：rx 为负 = 向前抬；左前臂本身平端向前（持弓）。马腿同天马：rx 为负 = 向前摆，前膝 rx 为正 = 管骨向后折。
// 箭（arrow）、松树干（pine）、背上的弓（bow_back）、手里的弓（bow）用缩放显示 / 隐藏；显隐用相邻两帧（0.01 秒）硬切，不渐变。
// 拉弓角度沿用哨兵石像的网格搜索结果（BOW_ARM / DRAW_ARM），构建时实测指尖与拉满的弦中点相距 ≤ 0.6u。
// 动画时长与服务端时间线对齐：拉弓 16 刻、掷松第 20 刻出手、踢击第 12 刻判定（SummonCentaurEntity）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const ZERO = [0, 0, 0];
    const HIDE = [0, 0, 0];
    const SHOW = [1, 1, 1];
    const P = MA.readJson('art/blockbench/greek/centaur/centaur.params.json');
    const S = P.string;
    const THETA = Math.atan(S.draw / S.half) * 180 / Math.PI;
    const STRETCH = 1 / Math.cos(THETA * Math.PI / 180);
    const r1 = (v) => Math.round(v * 1000) / 1000;

    /** 站姿：持弓的左前臂平端略上扬，右臂自然下垂。 */
    const REST = { arm_l: [-6, 0, 4], forearm_l: [-12, 0, 0], hand_l: ZERO, arm_r: [-6, 0, -4], forearm_r: [-12, 0, 0], waist: ZERO, head: ZERO };
    /** 搭箭：弓举平、右手到弦上（未拉开）。 */
    const NOCK = { arm_l: [-90, -20, 0], forearm_l: [90, 0, 0], hand_l: [0, 20, 0], arm_r: [-80, 10, -60], forearm_r: [-70, 0, 0], head: [0, 10, 0] };
    /** 拉满：左臂举平向前、略向中线收，前臂反向抵消使弓仍竖直；右手勾弦拉到下巴前。 */
    const DRAW = { arm_l: [-95, -20, 0], forearm_l: [95, 0, 0], hand_l: [0, 20, 0], arm_r: [-100, 10, -80], forearm_r: [-80, 0, 0], head: [0, 12, 0] };
    /** 举松（后仰蓄力）与掷出。左前臂在绑定姿势里是平端的，要再转 90° 才顺着上臂。 */
    const LIFT = { waist: [4, 0, 0], arm_l: [160, 0, 6], arm_r: [160, 0, -6], forearm_l: [100, 0, 0], forearm_r: [10, 0, 0], head: ZERO, hand_l: ZERO };
    const HURL = { waist: [14, 0, 0], arm_l: [172, 0, 6], arm_r: [172, 0, -6], forearm_l: [102, 0, 0], forearm_r: [12, 0, 0], head: [-8, 0, 0], hand_l: ZERO };
    const RELEASE = { waist: [-14, 0, 0], arm_l: [-40, 0, 6], arm_r: [-40, 0, -6], forearm_l: [90, 0, 0], forearm_r: [0, 0, 0], head: [6, 0, 0], hand_l: ZERO };

    /** 姿势序列 [[时刻, 姿势表], …] → 各骨骼的旋转通道（姿势里没写的骨骼在该帧回到 0）。 */
    const poses = (frames) => {
        const bones = new Set();
        frames.forEach(([, p]) => Object.keys(p).forEach((b) => bones.add(b)));
        const out = {};
        for (const b of bones) {
            out[b] = { rotation: frames.map(([t, p]) => [t, p[b] || ZERO]) };
        }
        return out;
    };
    /** 显隐硬切：[[时刻, 是否显示], …]。 */
    const vis = (frames) => {
        const keys = [];
        frames.forEach(([t, on], i) => {
            if (i > 0) keys.push([Math.max(0, t - 0.01), frames[i - 1][1] ? SHOW : HIDE]);
            keys.push([t, on ? SHOW : HIDE]);
        });
        return { scale: keys };
    };
    const merge = (...parts) => {
        const out = {};
        for (const p of parts) {
            for (const [b, ch] of Object.entries(p)) {
                out[b] = Object.assign(out[b] || {}, ch);
            }
        }
        return out;
    };
    const restProps = { arrow: vis([[0, false]]), pine: vis([[0, false]]), bow_back: vis([[0, false]]), bow: vis([[0, true]]) };

    // 对角步：左前 + 右后同相，右前 + 左后反相。每帧 [时刻比例, 上段旋转, 下段旋转]
    const gait = (len, amp) => {
        const fore = [[0, [-22 * amp, 0, 0], ZERO], [0.25, ZERO, ZERO], [0.5, [18 * amp, 0, 0], [12, 0, 0]], [0.75, [-6 * amp, 0, 0], [55 * amp, 0, 0]]];
        const hind = [[0, [-18 * amp, 0, 0], [-6, 0, 0]], [0.25, ZERO, ZERO], [0.5, [18 * amp, 0, 0], ZERO], [0.75, [-2 * amp, 0, 0], [-35 * amp, 0, 0]]];
        const leg = (upper, lower, frames, phase) => {
            const shifted = frames.map(([k, u, l]) => [((k + phase) % 1) * len, u, l]).sort((a, b) => a[0] - b[0]);
            const first = shifted[0];
            const last = shifted[shifted.length - 1];
            const span = first[0] + len - last[0];
            const f = (len - last[0]) / span;
            const lerp = (x, y) => x.map((v, i) => Math.round((v + (y[i] - v) * f) * 100) / 100);
            const edge = first[0] < 1e-6 ? null : [lerp(last[1], first[1]), lerp(last[2], first[2])];
            const keys = edge ? [[0, edge[0], edge[1]], ...shifted, [len, edge[0], edge[1]]] : [...shifted, [len, first[1], first[2]]];
            return { [upper]: { rotation: keys.map((k) => [k[0], k[1]]) }, [lower]: { rotation: keys.map((k) => [k[0], k[2]]) } };
        };
        return merge(leg('leg_fl', 'leg_fl_lower', fore, 0), leg('leg_hr', 'leg_hr_lower', hind, 0),
            leg('leg_fr', 'leg_fr_lower', fore, 0.5), leg('leg_hl', 'leg_hl_lower', hind, 0.5));
    };

    const ANIMS = {
        // 站立：呼吸、上身轻转、尾巴甩
        idle: {
            length: 4.0,
            loop: 'loop',
            bones: merge(poses([[0, REST], [4.0, REST]]), restProps, {
                waist: { rotation: [[0, ZERO], [2.0, [-2, 4, 0]], [4.0, ZERO]] },
                chest: { position: [[0, ZERO], [2.0, [0, 0.25, 0]], [4.0, ZERO]] },
                tail: { rotation: [[0, [4, 0, 0]], [1.0, [4, 6, 0]], [3.0, [4, -6, 0]], [4.0, [4, 0, 0]]] },
            }),
        },
        // 慢走：对角步 0.8 秒一循环，人上身随步轻颠
        walk: {
            length: 0.8,
            loop: 'loop',
            bones: merge(gait(0.8, 1.0), poses([[0, REST], [0.8, REST]]), restProps, {
                body: { position: [[0, ZERO], [0.2, [0, 0.5, 0]], [0.4, ZERO], [0.6, [0, 0.5, 0]], [0.8, ZERO]] },
                waist: { rotation: [[0, [2, 0, 0]], [0.4, [-1, 0, 0]], [0.8, [2, 0, 0]]] },
                tail: { rotation: [[0, [8, 0, 0]], [0.4, [8, 4, 0]], [0.8, [8, 0, 0]]] },
            }),
        },
        // 奔跑（风筝后撤、追近）：0.5 秒一循环，步幅加大，人上身前倾
        run: {
            length: 0.5,
            loop: 'loop',
            bones: merge(gait(0.5, 1.6), poses([[0, REST], [0.5, REST]]), restProps, {
                body: { position: [[0, ZERO], [0.125, [0, 1.0, 0]], [0.25, ZERO], [0.375, [0, 1.0, 0]], [0.5, ZERO]],
                    rotation: [[0, [-2, 0, 0]], [0.25, [2, 0, 0]], [0.5, [-2, 0, 0]]] },
                waist: { rotation: [[0, [-10, 0, 0]], [0.25, [-7, 0, 0]], [0.5, [-10, 0, 0]]] },
                tail: { rotation: [[0, [-25, 0, 0]], [0.25, [-20, 0, 0]], [0.5, [-25, 0, 0]]] },
            }),
        },
        // 拉弓 0.8 秒（16 刻）：0–0.25 举弓、右手搭弦、箭出现；0.25–0.8 拉满并保持
        draw: {
            length: 0.8,
            loop: 'hold_on_last_frame',
            bones: merge(poses([[0, REST], [0.25, NOCK], [0.8, DRAW]]), {
                arrow: Object.assign(vis([[0, false], [0.25, true]]), { position: [[0, ZERO], [0.25, ZERO], [0.8, [0, 0, -S.draw]]] }),
                string_top: { rotation: [[0, ZERO], [0.25, ZERO], [0.8, [r1(THETA), 0, 0]]], scale: [[0, SHOW], [0.25, SHOW], [0.8, [1, r1(STRETCH), 1]]] },
                string_bottom: { rotation: [[0, ZERO], [0.25, ZERO], [0.8, [r1(-THETA), 0, 0]]], scale: [[0, SHOW], [0.25, SHOW], [0.8, [1, r1(STRETCH), 1]]] },
                pine: vis([[0, false]]), bow_back: vis([[0, false]]), bow: vis([[0, true]]),
            }),
        },
        // 放箭：弦当即弹回、箭消失（真正的箭由服务端射出），右手顺势后甩，再回到站姿
        shoot: {
            length: 0.5,
            loop: 'hold_on_last_frame',
            bones: merge(poses([[0, DRAW], [0.08, Object.assign({}, DRAW, { arm_r: [-100, 25, -85], forearm_r: [-60, 0, 0] })], [0.5, REST]]), {
                arrow: vis([[0, true], [0.03, false]]),
                string_top: { rotation: [[0, [r1(THETA), 0, 0]], [0.04, [-4, 0, 0]], [0.1, [1.5, 0, 0]], [0.16, ZERO]],
                    scale: [[0, [1, r1(STRETCH), 1]], [0.04, SHOW]] },
                string_bottom: { rotation: [[0, [r1(-THETA), 0, 0]], [0.04, [4, 0, 0]], [0.1, [-1.5, 0, 0]], [0.16, ZERO]],
                    scale: [[0, [1, r1(STRETCH), 1]], [0.04, SHOW]] },
                pine: vis([[0, false]]), bow_back: vis([[0, false]]), bow: vis([[0, true]]),
            }),
        },
        // 掷松 1.4 秒：0.3 双臂举起时弓换到背上、松树干出现；0.85 后仰蓄满；1.0 前掷出手（服务端第 20 刻生成投射物）；1.4 回到站姿
        hurl: {
            length: 1.4,
            loop: 'hold_on_last_frame',
            bones: merge(poses([[0, REST], [0.3, LIFT], [0.85, HURL], [1.0, RELEASE], [1.4, REST]]), {
                bow: vis([[0, true], [0.3, false], [1.38, true]]),
                bow_back: vis([[0, false], [0.3, true], [1.38, false]]),
                pine: vis([[0, false], [0.3, true], [1.0, false]]),
                arrow: vis([[0, false]]),
            }),
        },
        // 营火边休息（野外版夜里，T15 §2）：马身卧下（腹部离地约 2u），前腿向前折、后腿收在身侧；人上身坐直，弓横放膝前
        rest: {
            length: 4.0,
            loop: 'loop',
            bones: merge(poses([[0, REST], [4.0, REST]]), restProps, {
                body: { position: [[0, [0, -8.5, 0]], [2.0, [0, -8.3, 0]], [4.0, [0, -8.5, 0]]] },
                leg_fl: { rotation: [[0, [-82, 0, -6]]] },
                leg_fl_lower: { rotation: [[0, [150, 0, 0]]] },
                leg_fr: { rotation: [[0, [-82, 0, 6]]] },
                leg_fr_lower: { rotation: [[0, [150, 0, 0]]] },
                leg_hl: { rotation: [[0, [-70, -20, -10]]] },
                leg_hl_lower: { rotation: [[0, [-100, 0, 0]]] },
                leg_hr: { rotation: [[0, [-70, 20, 10]]] },
                leg_hr_lower: { rotation: [[0, [-100, 0, 0]]] },
                waist: { rotation: [[0, [-4, 0, 0]], [2.0, [-2, 3, 0]], [4.0, [-4, 0, 0]]] },
                tail: { rotation: [[0, [45, 12, 0]], [2.0, [45, 18, 0]], [4.0, [45, 12, 0]]] },
            }),
        },
        // 踢击 0.9 秒：0–0.6 前身抬起（预警）、前腿折起；0.72 前蹄向前踢出（服务端第 12 刻判定）；0.9 落回
        kick: {
            length: 0.9,
            loop: 'hold_on_last_frame',
            bones: merge(poses([[0, REST], [0.6, Object.assign({}, REST, { waist: [16, 0, 0] })], [0.9, REST]]), restProps, {
                body: { rotation: [[0, ZERO], [0.6, [-22, 0, 0]], [0.72, [-18, 0, 0]], [0.9, ZERO]],
                    position: [[0, ZERO], [0.6, [0, 2.6, -0.4]], [0.72, [0, 2.2, 0]], [0.9, ZERO]] },
                leg_fl: { rotation: [[0, ZERO], [0.6, [-60, 0, 0]], [0.72, [-95, 0, 0]], [0.9, ZERO]] },
                leg_fl_lower: { rotation: [[0, ZERO], [0.6, [100, 0, 0]], [0.72, [20, 0, 0]], [0.9, ZERO]] },
                leg_fr: { rotation: [[0, ZERO], [0.6, [-50, 0, 0]], [0.72, [-90, 0, 0]], [0.9, ZERO]] },
                leg_fr_lower: { rotation: [[0, ZERO], [0.6, [95, 0, 0]], [0.72, [25, 0, 0]], [0.9, ZERO]] },
                leg_hl: { rotation: [[0, ZERO], [0.6, [22, 0, 0]], [0.72, [18, 0, 0]], [0.9, ZERO]] },
                leg_hr: { rotation: [[0, ZERO], [0.6, [22, 0, 0]], [0.72, [18, 0, 0]], [0.9, ZERO]] },
                tail: { rotation: [[0, [4, 0, 0]], [0.6, [-20, 0, 0]], [0.9, [4, 0, 0]]] },
            }),
        },
    };
    MA.CENTAUR_ANIMS = ANIMS;

    function build() {
        for (const a of Animation.all.slice()) {
            a.remove(false);
        }
        for (const [name, def] of Object.entries(ANIMS)) {
            const anim = new Animation({ name, loop: def.loop, length: def.length, snapping: 100 }).add();
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

    const at = (anim, t) => {
        anim.select();
        Timeline.setTime(t);
        Animator.preview();
    };
    const box = (c) => {
        c.mesh.updateMatrixWorld(true);
        return new THREE.Box3().setFromObject(c.mesh);
    };
    const world = (name) => {
        const v = Locator.all.find((x) => x.name === name).getWorldCenter();
        return [v.x, v.y, v.z];
    };

    /** 全程没有可见方块低于地面 0.3u 以下（缩成 0 的道具不算）。返回各动画的最低点。 */
    function checkGround(anim) {
        let lo = Infinity;
        for (let t = 0; t <= anim.length + 1e-6; t += anim.length / 12) {
            at(anim, t);
            for (const c of Cube.all) {
                const b = box(c);
                if (b.max.x - b.min.x < 1e-3) continue;
                lo = Math.min(lo, b.min.y);
                if (b.min.y < -0.3) {
                    throw new Error('动画 ' + anim.name + ' 在 ' + t.toFixed(2) + ' 秒：' + c.name + ' 低于地面 ' + b.min.y.toFixed(2));
                }
            }
        }
        return Math.round(lo * 10) / 10;
    }

    /** 拉满时指尖与弦中点相距 ≤ 0.6u。 */
    function checkDraw(anim) {
        at(anim, anim.length);
        const h = world('draw_hand');
        const n = world('nock_drawn');
        const d = Math.hypot(h[0] - n[0], h[1] - n[1], h[2] - n[2]);
        if (d > 0.6) {
            throw new Error('拉满时指尖离弦中点 ' + d.toFixed(2) + 'u（应 ≤ 0.6）');
        }
        return +d.toFixed(2);
    }

    MA.centaurAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        MA.centaurAnimReport = {};
        for (const a of Animation.all) {
            MA.centaurAnimReport[a.name] = checkGround(a);
        }
        MA.centaurAnimReport.drawGap = checkDraw(Animation.all.find((a) => a.name === 'draw'));
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };
    return 'centaur animations defined';
})();
