// 哨兵石像动画（T13 设计文档 §4.2）。由 build_sentinel.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度；rx 为负 = 肢体向前抬起），位移 [x, y, z]（u）。写入时经 axes.js 换算。
//
// 拉弓：左臂（弓臂）上抬到略高于肩、向身体中线收 20°，前臂与手腕反向补偿，弓始终竖直、箭指正前方；
// 右臂肘部外展、指尖勾弦拉到下巴下方——角度由 Blockbench 中的网格搜索求出（指尖 draw_hand 与拉满时的弦中点
// nock_drawn 相距 ≤ 0.6u，前臂与手不穿进头、躯干），构建时 checkDraw 实测。
// 弦：上下两段各绕弓梢转 θ = atan(拉距 / 半弦长)，并沿自身拉长 1/cos θ，两段末端正好在后拉 DRAW 处相会。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const P = MA.readJson('art/blockbench/greek/sentinel/sentinel.params.json');
    const S = P.string;
    const THETA = Math.atan(S.draw / S.half) * 180 / Math.PI;
    const STRETCH = 1 / Math.cos(THETA * Math.PI / 180);
    const r1 = (v) => Math.round(v * 1000) / 1000;

    // 姿势（卡片语义）
    const BOW_ARM = { upper_arm_l: [-95, -20, 0], forearm_l: [95, 0, 0], hand_l: [0, 20, 0] };
    const DRAW_ARM = { upper_arm_r: [-100, 10, -80], forearm_r: [-80, 0, 0] };
    // 搭箭：右手先到弦上（未拉开），角度取拉满姿势与静止姿势之间
    const NOCK_ARM = { upper_arm_r: [-80, 10, -60], forearm_r: [-70, 0, 0] };
    const ZERO = [0, 0, 0];
    const HIDE = [0, 0, 0];
    const SHOW = [1, 1, 1];

    const k = (pairs) => pairs;
    const pose = (map, t) => Object.fromEntries(Object.entries(map).map(([b, v]) => [b, [t, v]]));

    const ANIMS = {
        // 待机：石像不呼吸；头缓慢左右扫视，斗篷微摆；箭藏起
        idle: {
            length: 4.0,
            loop: 'loop',
            bones: {
                head: { rotation: k([[0, ZERO], [1.0, [0, 14, 0]], [1.6, [0, 14, 0]], [2.6, [0, -14, 0]], [3.2, [0, -14, 0]], [4.0, ZERO]]) },
                cape: { rotation: k([[0, [3, 0, 0]], [2.0, [5, 0, 1]], [4.0, [3, 0, 0]]]) },
                arrow: { scale: k([[0, HIDE]]) },
            },
        },
        // 升起：整尊连方台从地下升起（20 刻），最后一下轻顿
        rise: {
            length: 1.0,
            loop: 'hold_on_last_frame',
            bones: {
                root: {
                    position: k([[0, [0, -36, 0]], [0.75, [0, -1.2, 0]], [0.85, [0.3, 0.4, 0]], [0.92, [-0.2, 0, 0]], [1.0, ZERO]]),
                },
                arrow: { scale: k([[0, HIDE]]) },
            },
        },
        // 拉弓（12 刻 = 0.6 秒）：0–0.25 举弓、右手搭弦、箭出现；0.25–0.6 拉满
        draw: {
            length: 0.6,
            loop: 'hold_on_last_frame',
            bones: {
                upper_arm_l: { rotation: k([[0, ZERO], [0.25, BOW_ARM.upper_arm_l], [0.6, BOW_ARM.upper_arm_l]]) },
                forearm_l: { rotation: k([[0, ZERO], [0.25, BOW_ARM.forearm_l], [0.6, BOW_ARM.forearm_l]]) },
                hand_l: { rotation: k([[0, ZERO], [0.25, BOW_ARM.hand_l], [0.6, BOW_ARM.hand_l]]) },
                upper_arm_r: { rotation: k([[0, ZERO], [0.25, NOCK_ARM.upper_arm_r], [0.6, DRAW_ARM.upper_arm_r]]) },
                forearm_r: { rotation: k([[0, ZERO], [0.25, NOCK_ARM.forearm_r], [0.6, DRAW_ARM.forearm_r]]) },
                string_top: { rotation: k([[0, ZERO], [0.25, ZERO], [0.6, [r1(THETA), 0, 0]]]),
                    scale: k([[0, SHOW], [0.25, SHOW], [0.6, [1, r1(STRETCH), 1]]]) },
                string_bottom: { rotation: k([[0, ZERO], [0.25, ZERO], [0.6, [r1(-THETA), 0, 0]]]),
                    scale: k([[0, SHOW], [0.25, SHOW], [0.6, [1, r1(STRETCH), 1]]]) },
                arrow: { scale: k([[0, HIDE], [0.2, HIDE], [0.25, SHOW], [0.6, SHOW]]),
                    position: k([[0, ZERO], [0.25, ZERO], [0.6, [0, 0, -S.draw]]]) },
            },
        },
        // 放箭：弦当即弹回、箭消失（真正的箭由服务端射出），弓臂轻微后坐，右手顺势后甩，再回到静止
        shoot: {
            length: 0.6,
            loop: 'once',
            bones: {
                upper_arm_l: { rotation: k([[0, BOW_ARM.upper_arm_l], [0.08, [-90, -20, 0]], [0.25, BOW_ARM.upper_arm_l], [0.6, ZERO]]) },
                forearm_l: { rotation: k([[0, BOW_ARM.forearm_l], [0.25, BOW_ARM.forearm_l], [0.6, ZERO]]) },
                hand_l: { rotation: k([[0, BOW_ARM.hand_l], [0.25, BOW_ARM.hand_l], [0.6, ZERO]]) },
                upper_arm_r: { rotation: k([[0, DRAW_ARM.upper_arm_r], [0.08, [-100, 25, -85]], [0.25, [-95, 25, -80]], [0.6, ZERO]]) },
                forearm_r: { rotation: k([[0, DRAW_ARM.forearm_r], [0.08, [-60, 0, 0]], [0.6, ZERO]]) },
                string_top: { rotation: k([[0, [r1(THETA), 0, 0]], [0.04, [-4, 0, 0]], [0.1, [1.5, 0, 0]], [0.16, ZERO]]),
                    scale: k([[0, [1, r1(STRETCH), 1]], [0.04, SHOW]]) },
                string_bottom: { rotation: k([[0, [r1(-THETA), 0, 0]], [0.04, [4, 0, 0]], [0.1, [-1.5, 0, 0]], [0.16, ZERO]]),
                    scale: k([[0, [1, r1(STRETCH), 1]], [0.04, SHOW]]) },
                arrow: { scale: k([[0, HIDE]]) },
            },
        },
        // 碎化（20 刻）：先一阵颤动，头盔连头掉落、双臂垂下、身体歪斜下沉，最后整尊缩没（尘土由表现事件补）
        crumble: {
            length: 1.0,
            loop: 'hold_on_last_frame',
            bones: {
                root: {
                    position: k([[0, ZERO], [0.1, [0.4, 0, 0]], [0.2, [-0.4, 0, 0]], [0.3, [0.3, 0, 0]], [0.4, ZERO], [1.0, [0, -10, 0]]]),
                    scale: k([[0, SHOW], [0.7, SHOW], [1.0, HIDE]]),
                },
                head: {
                    position: k([[0, ZERO], [0.4, ZERO], [0.75, [1.5, -9, 3]], [1.0, [2, -12, 4]]]),
                    rotation: k([[0, ZERO], [0.4, ZERO], [0.75, [40, 0, -30]], [1.0, [70, 0, -45]]]),
                },
                upper_arm_l: { rotation: k([[0, ZERO], [0.4, ZERO], [0.8, [20, 0, 25]]]) },
                upper_arm_r: { rotation: k([[0, ZERO], [0.4, ZERO], [0.8, [20, 0, -25]]]) },
                body: { rotation: k([[0, ZERO], [0.45, ZERO], [1.0, [12, 0, 8]]]) },
                arrow: { scale: k([[0, HIDE]]) },
            },
        },
    };
    MA.SENTINEL_ANIMS = ANIMS;

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
        const l = Locator.all.find((x) => x.name === name);
        const v = l.getWorldCenter();
        return [v.x, v.y, v.z];
    };

    /** 拉满时：指尖与弦中点相距 ≤ 0.6u；右前臂、右手不穿进头与躯干；弓仍竖直（上下弓梢水平偏差 ≤ 1u）。 */
    function checkDraw(anim) {
        at(anim, anim.length);
        const h = world('draw_hand');
        const n = world('nock_drawn');
        const d = Math.hypot(h[0] - n[0], h[1] - n[1], h[2] - n[2]);
        if (d > 0.6) {
            throw new Error('拉满时指尖离弦中点 ' + d.toFixed(2) + 'u（应 ≤ 0.6）');
        }
        const fore = Cube.all.filter((c) => /^(forearm_r|vambrace_r|hand_r|fingers_r)$/.test(c.name));
        const body = Cube.all.filter((c) => /^(head|helm|cheek|nose|brow|crest|nape|neck|chest|cuirass|pec|abs|cape_clasp)/.test(c.name));
        for (const a of fore) {
            const ab = box(a).expandByScalar(-0.3);
            for (const b of body) {
                if (ab.intersectsBox(box(b))) {
                    throw new Error('拉满时 ' + a.name + ' 穿进 ' + b.name);
                }
            }
        }
        const top = box(Cube.all.find((c) => c.name === 'tip_upper'));
        const bottom = box(Cube.all.find((c) => c.name === 'tip_lower'));
        const tc = top.getCenter(new THREE.Vector3());
        const bc = bottom.getCenter(new THREE.Vector3());
        const drift = Math.hypot(tc.x - bc.x, tc.z - bc.z);
        if (drift > 1.0) {
            throw new Error('拉满时弓不竖直：上下弓梢水平偏差 ' + drift.toFixed(2) + 'u');
        }
        return +d.toFixed(2);
    }

    /** 待机、拉弓、放箭全程没有方块低于方台顶面以下的地面（升起与碎化除外）。 */
    function checkGround(anim) {
        for (let t = 0; t <= anim.length + 1e-6; t += anim.length / 6) {
            at(anim, t);
            for (const c of Cube.all) {
                if (c.visibility && box(c).min.y < -0.05) {
                    throw new Error('动画 ' + anim.name + ' 在 ' + t.toFixed(2) + ' 秒：' + c.name + ' 低于地面');
                }
            }
        }
    }

    MA.sentinelAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        for (const name of ['idle', 'draw', 'shoot']) {
            checkGround(Animation.all.find((a) => a.name === name));
        }
        MA.sentinelDrawGap = checkDraw(Animation.all.find((a) => a.name === 'draw'));
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };

    MA.pose = (name, time) => {
        Modes.options.animate.select();
        const anim = Animation.all.find((a) => a.name === name);
        at(anim, time);
        return name + '@' + time;
    };
    return 'sentinel animations defined';
})();
