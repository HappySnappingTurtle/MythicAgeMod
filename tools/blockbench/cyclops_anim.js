// T25a 独眼巨人动画（④ 召唤版：待机、行走、挥棒、抓取、投出；⑥ 牧人：举石、投石、慢挥棒、喝酒、醉倒、睡觉）。由 build_cyclops.js 调用；也可单独运行以只重建动画：
//   window.MA.run('tools/blockbench/cyclops_anim.js'); window.MA.buildCyclopsAnimations();
//
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度，+X 角色左、+Y 上、+Z 前），位移 [x, y, z]（u）。
// rx 为负 = 肢体向前抬起；rx 为正 = 躯干 / 头向前俯、小腿向后屈。左臂外展 rz 为正、右臂外展 rz 为负。
// 零姿势：右上臂下垂、右前臂平端向前、拳心竖直，橄榄木棍竖直穿过拳心的握孔；左臂下垂、手张开。
// 握持约束（检查清单 §三.1）：棍相对手只能绕棍身（卡片 Y 轴）自转，即 club 的 rx、rz 必须为 0；姿势改上臂、前臂、手腕。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    MA.run('tools/blockbench/lib/clearance.js');
    const A = MA.axes;

    const k = (pairs) => pairs.map(([t, v]) => [t, v]);
    const R = (frames) => ({ rotation: k(frames) });
    const Z = [0, 0, 0];

    // 待机的基础姿势：驼背略前倾、膝微屈、头抬起平视
    const STAND = {
        waist: [4, 0, 0],
        chest: [6, 0, 0],
        neck: [-6, 0, 0],
        head: [-4, 0, 0],
        arm_l: [-4, 0, 6],
        forearm_l: [-10, 0, 0],
        arm_r: [-4, 0, -6],
        leg_l: [-5, 0, 2],
        leg_r: [-5, 0, -2],
        shin_l: [8, 0, 0],
        shin_r: [8, 0, 0],
    };
    const st = (b) => STAND[b] || Z;
    /** 从待机姿势出发再回到待机的一组骨骼帧：mid = [[t, v], …]，首尾自动补待机。 */
    const fromStand = (len, bones) => {
        const out = {};
        for (const [b, mid] of Object.entries(bones)) {
            out[b] = R([[0, st(b)]].concat(mid, [[len, st(b)]]));
        }
        return out;
    };

    // 抓取：左手前伸到身前低处（被抓者多为 2 格高，抓腰），身体前压、左腿前跨
    const REACH = {
        waist: [12, 0, 0],
        chest: [22, -10, 0],
        neck: [-18, 4, 0],
        head: [-16, 6, 0],
        arm_l: [-62, 0, 10],
        forearm_l: [-14, 0, 0],
        hand_l: [-10, 0, 0],
        // 握棍的右手往后、往外收，前臂上抬抵消身体前压（合计 rx ≈ +10，棍略前倾），手腕外撇，棍头离开头侧、不挡脸
        arm_r: [20, 0, -34],
        forearm_r: [-44, 0, 0],
        hand_r: [0, 0, 28],
        leg_l: [-26, 0, 2],
        shin_l: [18, 0, 0],
        leg_r: [8, 0, -2],
        shin_r: [12, 0, 0],
    };
    // 举过头顶：左臂高举、肘微屈，身体后仰蓄势
    const LIFT = {
        waist: [-4, 0, 0],
        chest: [-14, 12, 0],
        neck: [4, -6, 0],
        head: [2, -6, 0],
        arm_l: [-168, 0, 8],
        forearm_l: [-24, 0, 0],
        hand_l: [-20, 0, 0],
        arm_r: [10, 0, -20],
        forearm_r: [8, 0, 0],
        leg_l: [-16, 0, 2],
        shin_l: [10, 0, 0],
        leg_r: [6, 0, -2],
        shin_r: [10, 0, 0],
    };
    // 投出：左臂向前下甩、躯干前压扭转
    const RELEASE = {
        waist: [12, 0, 0],
        chest: [24, -14, 0],
        neck: [-16, 6, 0],
        head: [-14, 6, 0],
        arm_l: [-70, 0, 4],
        forearm_l: [-6, 0, 0],
        hand_l: [10, 0, 0],
        arm_r: [20, 0, -34],
        forearm_r: [-46, 0, 0],
        hand_r: [0, 0, 28],
        leg_l: [-28, 0, 2],
        shin_l: [20, 0, 0],
        leg_r: [10, 0, -2],
        shin_r: [14, 0, 0],
    };
    const poseFrames = (seq) => {
        // seq: [[t, POSE], …] → 每块骨骼的帧（POSE 里没写的骨骼按待机）
        const bones = new Set();
        for (const [, p] of seq) Object.keys(p).forEach((b) => bones.add(b));
        const out = {};
        for (const b of bones) {
            out[b] = R(seq.map(([t, p]) => [t, p[b] || st(b)]));
        }
        return out;
    };

    const ANIMS = {
        idle: {
            length: 4.0,
            loop: 'loop',
            groundLock: [0, 2.0, 4.0],
            bones: {
                waist: R([[0, STAND.waist], [2.0, [5, 0, 0]], [4.0, STAND.waist]]),
                chest: Object.assign(R([[0, STAND.chest], [2.0, [8, 0, 0]], [4.0, STAND.chest]]),
                    { scale: k([[0, [1, 1, 1]], [2.0, [1.03, 1.02, 1.04]], [4.0, [1, 1, 1]]]) }),
                neck: R([[0, STAND.neck], [4.0, STAND.neck]]),
                head: R([[0, STAND.head], [1.2, [-6, -8, 0]], [2.6, [-2, 6, 0]], [4.0, STAND.head]]),
                arm_l: R([[0, STAND.arm_l], [2.0, [-2, 0, 8]], [4.0, STAND.arm_l]]),
                forearm_l: R([[0, STAND.forearm_l], [2.0, [-14, 0, 0]], [4.0, STAND.forearm_l]]),
                arm_r: R([[0, STAND.arm_r], [2.0, [-6, 0, -7]], [4.0, STAND.arm_r]]),
                leg_l: R([[0, STAND.leg_l], [4.0, STAND.leg_l]]),
                leg_r: R([[0, STAND.leg_r], [4.0, STAND.leg_r]]),
                shin_l: R([[0, STAND.shin_l], [4.0, STAND.shin_l]]),
                shin_r: R([[0, STAND.shin_r], [4.0, STAND.shin_r]]),
            },
        },
        walk: {
            // 沉重的大步：一个循环两步，身体每步下沉一次，躯干随步伐左右扭、向落地脚一侧微倾
            length: 1.4,
            loop: 'loop',
            groundLock: [0, 0.175, 0.35, 0.525, 0.7, 0.875, 1.05, 1.225, 1.4],
            bones: {
                leg_l: R([[0, [-22, 0, 2]], [0.7, [20, 0, 2]], [1.4, [-22, 0, 2]]]),
                leg_r: R([[0, [20, 0, -2]], [0.7, [-22, 0, -2]], [1.4, [20, 0, -2]]]),
                shin_l: R([[0, [6, 0, 0]], [0.35, [8, 0, 0]], [0.7, [4, 0, 0]], [1.05, [42, 0, 0]], [1.4, [6, 0, 0]]]),
                shin_r: R([[0, [4, 0, 0]], [0.35, [42, 0, 0]], [0.7, [6, 0, 0]], [1.05, [8, 0, 0]], [1.4, [4, 0, 0]]]),
                waist: R([[0, [6, 6, 2]], [0.7, [6, -6, -2]], [1.4, [6, 6, 2]]]),
                chest: R([[0, [8, 6, 0]], [0.7, [8, -6, 0]], [1.4, [8, 6, 0]]]),
                neck: R([[0, [-8, -4, 0]], [0.7, [-8, 4, 0]], [1.4, [-8, -4, 0]]]),
                head: R([[0, [-6, -6, 0]], [0.7, [-6, 6, 0]], [1.4, [-6, -6, 0]]]),
                // 空着的左臂与右腿同相摆（与左腿反相）；握棍的右臂只小幅摆，棍保持竖直
                arm_l: R([[0, [20, 0, 6]], [0.7, [-24, 0, 6]], [1.4, [20, 0, 6]]]),
                forearm_l: R([[0, [-8, 0, 0]], [0.7, [-26, 0, 0]], [1.4, [-8, 0, 0]]]),
                arm_r: R([[0, [-10, 0, -6]], [0.7, [4, 0, -6]], [1.4, [-10, 0, -6]]]),
            },
        },
        // 挥棒（普攻，0.7 秒）：右臂把棍举到右肩后上方（0.28 秒）→ 向前下砸（0.42 秒，棍头在身前略高于腰）→ 收回
        attack_swing: {
            length: 0.7,
            loop: 'hold',
            groundLock: [0, 0.28, 0.42, 0.7],
            bones: fromStand(0.7, {
                waist: [[0.28, [-4, 10, 0]], [0.42, [12, -8, 0]]],
                chest: [[0.28, [-8, 16, 0]], [0.42, [20, -12, 0]]],
                neck: [[0.28, [2, -10, 0]], [0.42, [-16, 8, 0]]],
                head: [[0.28, [2, -8, 0]], [0.42, [-12, 6, 0]]],
                arm_r: [[0.28, [-150, 0, -14]], [0.42, [-62, 0, -6]]],
                forearm_r: [[0.28, [56, 0, 0]], [0.42, [62, 0, 0]]],
                hand_r: [[0.28, [0, 0, 0]], [0.42, [50, 0, 0]]],
                arm_l: [[0.28, [-24, 0, 14]], [0.42, [16, 0, 10]]],
                forearm_l: [[0.28, [-30, 0, 0]], [0.42, [-14, 0, 0]]],
                leg_l: [[0.28, [-8, 0, 2]], [0.42, [-24, 0, 2]]],
                shin_l: [[0.28, [10, 0, 0]], [0.42, [20, 0, 0]]],
                leg_r: [[0.28, [-2, 0, -2]], [0.42, [10, 0, -2]]],
                shin_r: [[0.28, [8, 0, 0]], [0.42, [12, 0, 0]]],
            }),
        },
        // 抓取（召唤版特殊攻击的 1 秒前摇）：0–0.75 秒伸出左手、身体前压；0.85 秒合拢（手腕上翻）；停在抓住的姿势
        grab: {
            length: 1.0,
            loop: 'hold',
            groundLock: [0, 0.4, 0.75, 1.0],
            bones: (() => {
                const b = poseFrames([[0, STAND], [0.75, REACH], [1.0, REACH]]);
                b.hand_l = R([[0, Z], [0.75, REACH.hand_l], [0.85, [24, 0, 0]], [1.0, [24, 0, 0]]]);
                b.forearm_l = R([[0, STAND.forearm_l], [0.75, REACH.forearm_l], [0.85, [-30, 0, 0]], [1.0, [-30, 0, 0]]]);
                return b;
            })(),
        },
        // 投出（0.8 秒，第 0.5 秒松手）：从抓住的姿势把人举过头顶（0.3 秒）→ 向前甩出（0.5 秒）→ 回到待机
        hurl: {
            length: 0.8,
            loop: 'hold',
            groundLock: [0, 0.3, 0.5, 0.8],
            bones: (() => {
                const grabbed = Object.assign({}, REACH, { forearm_l: [-30, 0, 0], hand_l: [24, 0, 0] });
                return poseFrames([[0, grabbed], [0.3, LIFT], [0.5, RELEASE], [0.8, STAND]]);
            })(),
        },
    };
    // ───────── 牧人（T25a 阶段 ⑥） ─────────
    // 举石（1.2 秒，投石前的预警）：弯腰用左手从地上抄起巨石（0.45 秒）→ 举过头顶、身体后仰蓄势（0.9 秒）→ 停住。
    // 巨石挂在左手掌心内侧（params：boulder 的父骨骼是 hand_l），只在举石、投石时由渲染器显示。
    const SCOOP = Object.assign({}, REACH, {
        waist: [18, 0, 0], chest: [30, -6, 0], arm_l: [-38, 0, 8], forearm_l: [-6, 0, 0], hand_l: [10, 0, 0],
        leg_l: [-30, 0, 2], shin_l: [36, 0, 0], leg_r: [6, 0, -2], shin_r: [26, 0, 0],
    });
    const HEFT = Object.assign({}, LIFT, { chest: [-18, 16, 0], waist: [-6, 0, 0], arm_l: [-172, 0, 14], forearm_l: [-30, 0, 0] });
    ANIMS.lift_rock = {
        length: 1.2,
        loop: 'hold',
        groundLock: [0, 0.45, 0.9, 1.2],
        bones: poseFrames([[0, STAND], [0.45, SCOOP], [0.9, HEFT], [1.2, HEFT]]),
    };
    // 投石（0.6 秒，第 0.2 秒出手）
    ANIMS.throw_rock = {
        length: 0.6,
        loop: 'hold',
        groundLock: [0, 0.2, 0.6],
        bones: poseFrames([[0, HEFT], [0.2, RELEASE], [0.6, STAND]]),
    };
    // 慢挥棒（精英，0.8 秒前摇 + 砸下）：举到肩后上方停住蓄力，第 0.9 秒砸下
    // 慢挥棒（精英，0.8 秒前摇 + 砸下）：前 0.28 秒照召唤版挥棒的举棒路径（全程不碰头），停在肩后上方蓄力到 0.8 秒，0.9 秒砸下
    ANIMS.club_smash = (() => {
        const sw = ANIMS.attack_swing.bones;
        const bones = {};
        for (const [bone, ch] of Object.entries(sw)) {
            const f = ch.rotation;
            const up = f.find(([t]) => Math.abs(t - 0.28) < 1e-6)[1];
            const hit = f.find(([t]) => Math.abs(t - 0.42) < 1e-6)[1];
            bones[bone] = R([[0, f[0][1]], [0.28, up], [0.8, up], [0.9, hit], [1.2, f[f.length - 1][1]]]);
        }
        return { length: 1.2, loop: 'hold', groundLock: [0, 0.28, 0.8, 0.9, 1.2], bones };
    })();
    // 喝酒（2 秒循环）：左手把酒碗捧到嘴边，仰头喝（《奥德赛》9.353–361 接过酒碗一饮而尽，再要）
    // 站着、略弯腰（酒碗先从地上拿起来——落地的酒碗由实体显示，这里只做喝的动作）
    const SQUAT = {
        waist: [6, 0, 0], chest: [-4, 0, 0],
        leg_l: [-8, 0, 4], shin_l: [12, 0, 0], leg_r: [2, 0, -4], shin_r: [8, 0, 0],
        // 握棍的右手往外、往前放，棍头前倾离开头侧
        arm_r: [10, 0, -34], forearm_r: [-10, 0, 0], hand_r: [0, 0, 28],
    };
    const SIP = Object.assign({}, SQUAT, {
        neck: [-20, 0, 0], head: [-24, 0, 0], arm_l: [-120, 0, -24], forearm_l: [-70, 0, 0], hand_l: [-10, 0, 0],
    });
    const SIP2 = Object.assign({}, SIP, { neck: [-26, 0, 0], head: [-30, 0, 0], arm_l: [-128, 0, -24] });
    ANIMS.drink = {
        length: 2.0,
        loop: 'loop',
        groundLock: [0, 0.5, 1.0, 1.5, 2.0],
        bones: poseFrames([[0, SIP], [0.5, SIP2], [1.0, SIP], [1.5, SIP2], [2.0, SIP]]),
    };
    // 醉倒（2 秒循环，《奥德赛》9.371–374：仰面倒下，粗脖子歪向一边）：整个人以脚为轴向后仰倒平躺，
    // 四肢摊开、头歪向一侧随呼吸起伏；棍还攥在手里，平放在身侧地上
    const SPRAWL = {
        root: [-90, 0, 0],
        neck: [-4, 0, 0], head: [-6, 26, 0],
        arm_l: [-8, 0, 62], forearm_l: [-20, 0, 0], hand_l: [0, 0, 0],
        // 握棍的右手：网格搜索（棍不碰头、不入地、最平）得出——上臂抬起、肘深屈，棍横放在右肩外的地上
        arm_r: [-60, 0, -20], forearm_r: [-120, 0, 0], hand_r: [0, 0, 0],
        leg_l: [-4, 0, 14], shin_l: [10, 0, 0], leg_r: [-4, 0, -12], shin_r: [16, 0, 0],
    };
    const SPRAWL2 = Object.assign({}, SPRAWL, { head: [-6, 30, 0], chest: [-3, 0, 0], arm_l: [-8, 0, 66] });
    ANIMS.drunk = {
        length: 2.0,
        loop: 'loop',
        groundLock: [0, 1.0, 2.0],
        groundBone: 'root',
        groundAll: true,
        bones: Object.assign(poseFrames([[0, SPRAWL], [1.0, SPRAWL2], [2.0, SPRAWL]]),
            { chest: Object.assign(R([[0, [0, 0, 0]], [1.0, [-3, 0, 0]], [2.0, [0, 0, 0]]]),
                { scale: k([[0, [1, 1, 1]], [1.0, [1.04, 1.03, 1.05]], [2.0, [1, 1, 1]]]) }) }),
    };
    // 睡觉（4 秒循环）：同样仰躺，手臂收在身侧，慢呼吸
    const REST = Object.assign({}, SPRAWL, { head: [-4, 10, 0], arm_l: [-4, 0, 18], leg_l: [-2, 0, 6], leg_r: [-2, 0, -6] });
    ANIMS.sleep = {
        length: 4.0,
        loop: 'loop',
        groundLock: [0, 2.0, 4.0],
        groundBone: 'root',
        groundAll: true,
        bones: Object.assign(poseFrames([[0, REST], [2.0, REST], [4.0, REST]]),
            { chest: Object.assign(R([[0, [0, 0, 0]], [2.0, [-2, 0, 0]], [4.0, [0, 0, 0]]]),
                { scale: k([[0, [1, 1, 1]], [2.0, [1.03, 1.02, 1.04]], [4.0, [1, 1, 1]]]) }) }),
    };
    MA.CYCLOPS_ANIMS = ANIMS;

    /** 握持约束：棍相对手只能绕棍身（卡片 Y 轴）转。 */
    function checkGrip(name, def) {
        const club = def.bones.club;
        if (!club || !club.rotation) return;
        for (const [t, v] of club.rotation) {
            if (Math.abs(v[0]) > 1e-6 || Math.abs(v[2]) > 1e-6) {
                throw new Error('动画 ' + name + ' 在 ' + t + ' 秒让棍离开握孔方向：' + JSON.stringify(v) + '（只能转 ry）');
            }
        }
    }

    MA.buildCyclopsAnimations = () => {
        for (const a of Animation.all.slice()) {
            a.remove(false);
        }
        for (const [name, def] of Object.entries(ANIMS)) {
            checkGrip(name, def);
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
        return Animation.all.map((a) => a.name);
    };

    /**
     * 贴地：在 groundLock 的时刻测最低点，修正某块骨骼位移的 y，让它恰好着地。
     * 默认测脚（脚掌、脚趾、脚跟）、修正 body；躺下的动画（醉倒、睡觉）用 groundBone: 'root' + groundAll: true，
     * 测全身看得见的方块（不算巨石），修正 root。
     */
    function groundLock(anim, def) {
        const name = def.groundBone || 'body';
        const bone = MA.bones[name] || Group.all.find((g) => g.name === name);
        const animator = anim.getBoneAnimator(bone);
        const cubes = Cube.all.filter((c) => (def.groundAll ? !/^boulder/.test(c.name) : /^(foot|toes|heel)_/.test(c.name)));
        for (const t of def.groundLock) {
            if (!animator.position.find((f) => Math.abs(f.time - t) < 1e-4)) {
                animator.addKeyframe({ channel: 'position', time: t, interpolation: 'linear', data_points: [{ x: 0, y: 0, z: 0 }] });
            }
        }
        for (const t of def.groundLock) {
            anim.select();
            Timeline.setTime(t);
            Animator.preview();
            let minY = Infinity;
            for (const c of cubes) {
                c.mesh.updateMatrixWorld(true);
                minY = Math.min(minY, new THREE.Box3().setFromObject(c.mesh).min.y);
            }
            const kf = animator.position.find((f) => Math.abs(f.time - t) < 1e-4);
            const y = parseFloat(kf.data_points[0].y) || 0;
            kf.data_points[0].y = Math.round((y - minY) * 100) / 100;
        }
        Animator.preview();
    }

    const HEAD = /^(head|jaw|brow|cheekbone|eye|iris|nose|mouth|lip|tusk|ear|hair|forelock|beard|whisker|moustache)/;

    /** 互穿检查：每个贴地时刻棍不碰头（按方块自身朝向），也没有方块低于地面 0.05u。 */
    function checkClearance(anim, def) {
        const club = Cube.all.filter((c) => /^club/.test(c.name));
        const head = Cube.all.filter((c) => HEAD.test(c.name));
        for (const t of def.groundLock) {
            anim.select();
            Timeline.setTime(t);
            Animator.preview();
            const r = MA.preciseHit(club, head);
            if (r.hit) {
                throw new Error('动画 ' + anim.name + ' 在 ' + t + ' 秒：' + r.hit);
            }
            for (const c of Cube.all) {
                if (c.visibility && !/^boulder/.test(c.name) && new THREE.Box3().setFromObject(c.mesh).min.y < -0.05) {
                    throw new Error('动画 ' + anim.name + ' 在 ' + t + ' 秒：' + c.name + ' 低于地面');
                }
            }
        }
    }

    /** 导出 GeckoLib 读取的 Bedrock 动画文件，返回文本。 */
    MA.cyclopsAnimations = (rel) => {
        MA.buildCyclopsAnimations();
        Modes.options.animate.select();
        for (const [name, def] of Object.entries(ANIMS)) {
            const anim = Animation.all.find((a) => a.name === name);
            groundLock(anim, def);
            checkClearance(anim, def);
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        return text;
    };

    /** 预览：定格在某动画的某一时刻（秒）。 */
    MA.pose = (name, time) => {
        Modes.options.animate.select();
        const anim = Animation.all.find((a) => a.name === name);
        anim.select();
        Timeline.setTime(time);
        Animator.preview();
        return name + '@' + time;
    };
    return 'cyclops animations defined';
})();
