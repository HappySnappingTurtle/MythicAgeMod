// T9b §3 神庙祭司动画（数据 + 生成）。由 build_greek_guardian.js 调用。
// 关键帧用设计卡语义书写：旋转 [rx, ry, rz]（度；rx 为负 = 肢体向前抬起，rx 为正 = 躯干/头前俯、小腿后屈；
// 左臂外展 rz 为正、右臂外展 rz 为负），位移 [x, y, z]（u）。写入 Blockbench 时经 axes.js 换算。
//
// 托碗规则（检查清单 §三）：碗骨骼永不单独旋转；右臂的姿势由上臂、前臂摆出，并让「上臂 rx + 前臂 rx」= 0，
// 碗在世界中保持水平（checkBowlLevel 在每个贴地时刻实测，倾斜超过 6° 即中止构建）。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    const A = MA.axes;
    const k = (pairs) => pairs.map(([t, v]) => [t, v]);

    const ANIMS = {
        // 待机：呼吸；碗随呼吸极轻地起伏；约 2 秒时低头看一眼碗再抬起；披风与左手轻摆
        idle: {
            length: 4.0,
            loop: 'loop',
            groundLock: [0, 2.0, 4.0],
            bones: {
                torso: {
                    rotation: k([[0, [0, 0, 0]], [2.0, [1.5, 0, 0]], [4.0, [0, 0, 0]]]),
                    scale: k([[0, [1, 1, 1]], [2.0, [1.015, 1.02, 1.02]], [4.0, [1, 1, 1]]]),
                },
                head: { rotation: k([[0, [0, 0, 0]], [1.6, [0, 4, 0]], [2.2, [10, -2, 0]], [2.9, [10, -2, 0]], [3.5, [0, 0, 0]], [4.0, [0, 0, 0]]]) },
                upper_arm_r: { rotation: k([[0, [0, 0, -3]], [2.0, [-2, 0, -3]], [4.0, [0, 0, -3]]]) },
                forearm_r: { rotation: k([[0, [0, 0, 3]], [2.0, [2, 0, 3]], [4.0, [0, 0, 3]]]) },
                upper_arm_l: { rotation: k([[0, [2, 0, 4]], [2.0, [-2, 0, 6]], [4.0, [2, 0, 4]]]) },
                forearm_l: { rotation: k([[0, [-6, 0, 0]], [4.0, [-6, 0, 0]]]) },
                mantle: { rotation: k([[0, [2, 0, 0]], [2.0, [4, 0, 0]], [4.0, [2, 0, 0]]]) },
                hem_front: { rotation: k([[0, [0, 0, 0]], [2.0, [-1.5, 0, 0]], [4.0, [0, 0, 0]]]) },
            },
        },
        // 慢走：小步（大腿 ±14°；±18° 时小腿会从长袍前面戳出），下摆前片随前伸的腿外扬、后片随后蹬的腿后扬，披风后飘；
        // 左臂与左腿反相摆动；右臂托碗只做极小的稳定摆动（前臂反向补偿保持碗水平）
        walk: {
            length: 1.2,
            loop: 'loop',
            groundLock: [0, 0.15, 0.3, 0.45, 0.6, 0.75, 0.9, 1.05, 1.2],
            bones: {
                thigh_l: { rotation: k([[0, [-14, 0, 0]], [0.6, [14, 0, 0]], [1.2, [-14, 0, 0]]]) },
                thigh_r: { rotation: k([[0, [14, 0, 0]], [0.6, [-14, 0, 0]], [1.2, [14, 0, 0]]]) },
                shin_l: { rotation: k([[0, [0, 0, 0]], [0.3, [6, 0, 0]], [0.6, [0, 0, 0]], [0.9, [26, 0, 0]], [1.2, [0, 0, 0]]]) },
                shin_r: { rotation: k([[0, [0, 0, 0]], [0.3, [26, 0, 0]], [0.6, [0, 0, 0]], [0.9, [6, 0, 0]], [1.2, [0, 0, 0]]]) },
                foot_l: { rotation: k([[0, [14, 0, 0]], [0.6, [-14, 0, 0]], [1.2, [14, 0, 0]]]) },
                foot_r: { rotation: k([[0, [-14, 0, 0]], [0.6, [14, 0, 0]], [1.2, [-14, 0, 0]]]) },
                hem_front: { rotation: k([[0, [-18, 0, 3]], [0.3, [-6, 0, 0]], [0.6, [-18, 0, -3]], [0.9, [-6, 0, 0]], [1.2, [-18, 0, 3]]]) },
                hem_back: { rotation: k([[0, [16, 0, -3]], [0.3, [5, 0, 0]], [0.6, [16, 0, 3]], [0.9, [5, 0, 0]], [1.2, [16, 0, -3]]]) },
                mantle: { rotation: k([[0, [7, 0, 0]], [0.3, [10, 0, 0]], [0.6, [7, 0, 0]], [0.9, [10, 0, 0]], [1.2, [7, 0, 0]]]) },
                torso: { rotation: k([[0, [2, 3, 0]], [0.6, [2, -3, 0]], [1.2, [2, 3, 0]]]) },
                head: { rotation: k([[0, [-1, -2, 0]], [0.6, [-1, 2, 0]], [1.2, [-1, -2, 0]]]) },
                upper_arm_l: { rotation: k([[0, [16, 0, 4]], [0.6, [-16, 0, 4]], [1.2, [16, 0, 4]]]) },
                forearm_l: { rotation: k([[0, [-4, 0, 0]], [0.6, [-14, 0, 0]], [1.2, [-4, 0, 0]]]) },
                upper_arm_r: { rotation: k([[0, [-3, 0, -3]], [0.6, [3, 0, -3]], [1.2, [-3, 0, -3]]]) },
                forearm_r: { rotation: k([[0, [3, 0, 3]], [0.6, [-3, 0, 3]], [1.2, [3, 0, 3]]]) },
            },
        },
        // 迎客（打开交易界面时播一次）：左上臂向外张开约 60°，前臂立起，手举在头的外侧、掌心向前，轻挥一下；头微点。
        // 角度由 Blockbench 中网格搜索得出：上臂前伸 + 前臂上抬的方案会让前臂斜回脸前、碰到胡须，改为外展 + 立前臂。
        greet: {
            length: 1.6,
            loop: 'once',
            groundLock: [0, 0.5, 0.9, 1.6],
            bones: {
                upper_arm_l: { rotation: k([[0, [0, 0, 4]], [0.45, [-12, 0, 60]], [1.1, [-12, 0, 60]], [1.6, [0, 0, 4]]]) },
                forearm_l: { rotation: k([[0, [-6, 0, 0]], [0.45, [0, 0, 80]], [0.7, [0, 0, 68]], [0.9, [0, 0, 80]], [1.1, [0, 0, 80]], [1.6, [-6, 0, 0]]]) },
                head: { rotation: k([[0, [0, 0, 0]], [0.5, [0, 0, 0]], [0.75, [12, 0, 0]], [1.0, [0, 0, 0]], [1.6, [0, 0, 0]]]) },
                torso: { rotation: k([[0, [0, 0, 0]], [0.75, [4, 0, 0]], [1.6, [0, 0, 0]]]) },
            },
        },
        // 宣告（初见祝福成功时）：抬头、左臂高举向天，右手把碗举到胸前（碗保持水平），身体略后仰
        announce: {
            length: 2.2,
            loop: 'once',
            groundLock: [0, 0.6, 1.5, 2.2],
            bones: {
                torso: { rotation: k([[0, [0, 0, 0]], [0.6, [-5, 0, 0]], [1.5, [-5, 0, 0]], [2.2, [0, 0, 0]]]) },
                head: { rotation: k([[0, [0, 0, 0]], [0.6, [-14, 0, 0]], [1.5, [-14, 0, 0]], [2.2, [0, 0, 0]]]) },
                // 手臂举过头后，绕 Z 的正旋转会把手带向头顶（旋转顺序 X→Y→Z），所以这里用负 rz 让手偏向外侧（实测）
                upper_arm_l: { rotation: k([[0, [0, 0, 4]], [0.6, [-155, 0, -25]], [1.5, [-155, 0, -25]], [2.2, [0, 0, 4]]]) },
                forearm_l: { rotation: k([[0, [-6, 0, 0]], [0.6, [-12, 0, 0]], [1.5, [-12, 0, 0]], [2.2, [-6, 0, 0]]]) },
                upper_arm_r: { rotation: k([[0, [0, 0, -3]], [0.6, [-38, 0, -3]], [1.5, [-38, 0, -3]], [2.2, [0, 0, -3]]]) },
                forearm_r: { rotation: k([[0, [0, 0, 3]], [0.6, [38, 0, 3]], [1.5, [38, 0, 3]], [2.2, [0, 0, 3]]]) },
                mantle: { rotation: k([[0, [2, 0, 0]], [0.6, [8, 0, 0]], [1.5, [8, 0, 0]], [2.2, [2, 0, 0]]]) },
            },
        },
        // 指向（T10 状态面板「指向缺件」）：左臂向前平指，头看向指的方向
        point: {
            length: 1.4,
            loop: 'once',
            groundLock: [0, 0.4, 1.0, 1.4],
            bones: {
                // 水平前指时 rz 只让手臂绕自身转，向外偏要用 ry（+12° 让前臂离开头侧，实测）
                upper_arm_l: { rotation: k([[0, [0, 0, 4]], [0.4, [-86, 12, 4]], [1.0, [-86, 12, 4]], [1.4, [0, 0, 4]]]) },
                forearm_l: { rotation: k([[0, [-6, 0, 0]], [0.4, [-4, 0, 0]], [1.0, [-4, 0, 0]], [1.4, [-6, 0, 0]]]) },
                head: { rotation: k([[0, [0, 0, 0]], [0.4, [5, 6, 0]], [1.0, [5, 6, 0]], [1.4, [0, 0, 0]]]) },
                torso: { rotation: k([[0, [0, 0, 0]], [0.4, [0, 5, 0]], [1.0, [0, 5, 0]], [1.4, [0, 0, 0]]]) },
            },
        },
    };
    MA.GREEK_GUARDIAN_ANIMS = ANIMS;

    /** 托碗：碗骨骼不得有关键帧（姿势只靠上臂、前臂）。 */
    function checkBowlBone(name, def) {
        if (def.bones.bowl || def.bones.hand_r) {
            throw new Error('动画 ' + name + ' 不能直接转碗或右手：托碗姿势只能由上臂、前臂摆出');
        }
    }

    function build() {
        for (const a of Animation.all.slice()) {
            a.remove(false);
        }
        for (const [name, def] of Object.entries(ANIMS)) {
            checkBowlBone(name, def);
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

    /** 贴地：在 groundLock 时刻测鞋底最低点，修正骨盆 y 位移让鞋底恰好着地。 */
    function groundLock(anim, def) {
        const pelvis = MA.bones.pelvis || Group.all.find((g) => g.name === 'pelvis');
        const animator = anim.getBoneAnimator(pelvis);
        const feet = Cube.all.filter((c) => /^(sole|foot|toe_strap)/.test(c.name));
        for (const t of def.groundLock) {
            if (!animator.position.find((f) => Math.abs(f.time - t) < 1e-4)) {
                animator.addKeyframe({ channel: 'position', time: t, interpolation: 'linear', data_points: [{ x: 0, y: 0, z: 0 }] });
            }
        }
        for (const t of def.groundLock) {
            at(anim, t);
            let minY = Infinity;
            for (const c of feet) {
                minY = Math.min(minY, box(c).min.y);
            }
            const kf = animator.position.find((f) => Math.abs(f.time - t) < 1e-4);
            const y = parseFloat(kf.data_points[0].y) || 0;
            kf.data_points[0].y = Math.round((y - minY) * 100) / 100;
        }
        Animator.preview();
    }

    /** 互穿：左手不穿进头部（含冠、须）；没有方块低于地面。碗在世界中保持水平（倾斜 ≤ 6°）。 */
    function check(anim, def) {
        const hand = Cube.all.filter((c) => /^(hand_l|thumb_l|forearm_l|bracelet_l)$/.test(c.name));
        const head = Cube.all.filter((c) => /^(head|nose|hair|beard|wreath|leaf|ear)/.test(c.name));
        const bowl = MA.bones.bowl || Group.all.find((g) => g.name === 'bowl');
        for (const t of def.groundLock) {
            at(anim, t);
            for (const a of hand) {
                const ab = box(a);
                for (const h of head) {
                    if (ab.intersectsBox(box(h))) {
                        throw new Error('动画 ' + anim.name + ' 在 ' + t + ' 秒：' + a.name + ' 穿进 ' + h.name);
                    }
                }
            }
            for (const c of Cube.all) {
                if (c.visibility && box(c).min.y < -0.05) {
                    throw new Error('动画 ' + anim.name + ' 在 ' + t + ' 秒：' + c.name + ' 低于地面');
                }
            }
            bowl.mesh.updateMatrixWorld(true);
            const q = new THREE.Quaternion();
            bowl.mesh.getWorldQuaternion(q);
            const up = new THREE.Vector3(0, 1, 0).applyQuaternion(q);
            const tilt = Math.acos(Math.max(-1, Math.min(1, up.y))) * 180 / Math.PI;
            if (tilt > 6) {
                throw new Error('动画 ' + anim.name + ' 在 ' + t + ' 秒：奠酒碗倾斜 ' + tilt.toFixed(1) + '°（酒会洒）');
            }
        }
    }

    /** 导出 GeckoLib 读取的 Bedrock 动画文件，返回文本。 */
    MA.greekGuardianAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        for (const [name, def] of Object.entries(ANIMS)) {
            const anim = Animation.all.find((a) => a.name === name);
            groundLock(anim, def);
            check(anim, def);
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        Modes.options.edit.select();
        return text;
    };

    /** 预览：切到动画模式，定格在某动画的某一时刻（秒）。 */
    MA.pose = (name, time) => {
        Modes.options.animate.select();
        const anim = Animation.all.find((a) => a.name === name);
        at(anim, time);
        return name + '@' + time;
    };
    return 'greek guardian animations defined';
})();
