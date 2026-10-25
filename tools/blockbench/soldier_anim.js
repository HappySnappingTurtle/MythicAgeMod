// T20 阶段 ③ 士兵动作（盾矛卫兵、轻装投矛兵），由 resident_anim.js 合进居民的动画表。
// 关键帧语义同 resident_anim.js：旋转 [rx, ry, rz]（度），位移 [x, y, z]（u）；both() 把左侧镜像到右侧。
// 武器（长矛、投矛、圆盾）是手 / 前臂的子骨骼：只靠上臂、前臂、手腕摆（T7 握持规则）。手臂角度来自 solve_pose.js 的网格搜索，
// 搜索目标写在每个姿势旁边（握点位置、矛的方向；盾心位置、盾面朝向）。时长与服务端时间轴对齐：
//   矛刺 24 刻 = 1.2 秒（前摇 0–0.6、有效 0.6–0.75、恢复 0.75–1.2）；投矛 32 刻 = 1.6 秒（前摇 0–1.0、出手 1.0–1.05、恢复到 1.6）；
//   警觉 16 刻 = 0.8 秒；扬火 8 刻 = 0.4 秒。
(function () {
    const MA = window.MA;
    MA.SOLDIER_ANIM_NAMES = ['guard_idle', 'patrol_walk', 'alert', 'spear_thrust', 'block_hold', 'intercept_step', 'javelin_throw',
        'brazier_signal', 'train', 'wrestle'];
    /** 这些动作里藏起武器（扬火、角力）；角力连盔甲也藏（赤身角力）。ResidentGeoModel 用同一张表。 */
    MA.SOLDIER_NO_WEAPON = ['brazier_signal', 'wrestle'];

    // ── 手臂姿势（solve_pose.js 解出，卡片语义：上臂 [rx, ry, rz]、前臂 rx、手骨 [rx, ry, rz]；括号里是求解目标与残差）──
    // 矛刺用下手平端：长矛的矛尖从拇指一侧伸出、矛只能绕柄转，上手持矛（矛尖从小指一侧出）表达不了；
    // 下手平端也是史实里的接敌持法（Somatophylaques：接近时下手持矛）。
    // 盾挂在左手骨上：手骨只绕 Y 转就是前臂旋前 / 旋后，靠它把盾面转向前方。
    const P = MA.SOLDIER_POSES = {
        // 站岗：矛竖直、尾刺着地（握点 (-5.5, 13.5, 4)，握孔朝上；残差 0.1u、0°）
        spearUpright: { upper: [5, 0, 0], fore: -65, hand: [-30, 0, 0] },
        // 巡逻：竖持、抬高 1 格（握点 (-5.5, 14.5, 4)；0.4u、0.2°）
        spearCarry: { upper: [10, -3, 0], fore: -80, hand: [-23, 0, 0] },
        // 下手平端待刺（握点 (-6, 13, 4)，矛向前 (0, 0.05, 1)；0.1u、1°）
        ready: { upper: [-3, 0, -3], fore: -74, hand: [67, 0, 0] },
        // 前摇收矛（握点 (-6.5, 13.5, -2)；0.2u、0.2°）
        pullBack: { upper: [35, -8, -12], fore: -95, hand: [55, 0, 28] },
        // 刺出（握点 (-5, 15, 10)，矛向前略上 (0, 0.1, 1)；0.1u、0.6°）
        strike: { upper: [-45, -10, -5], fore: -65, hand: [90, 0, -7] },
        // 盾挂在身侧：左臂微张，盾不贴腿
        shieldRest: { upper: [0, 0, 6], fore: -8, hand: [0, 0, 0] },
        // 举盾：盾心 (2.2, 18.5, 7.1)、盾面朝正前（0°），前臂横在胸前、离胸甲 ≥ 4.3u
        shieldUp: { upper: [-80, -45, 40], fore: -110, hand: [0, 40, 0] },
        // 投矛待投：握点 (-6, 23, 0)，矛向前上 (0, 0.2, 1)（0.1u、0.2°）
        javReady: { upper: [-157, 0, 21], fore: -160, hand: [-55, 8, 0] },
        // 后引：握点 (-7, 23.5, -6)，(0, 0.4, 1)（0.1u、0.2°）
        javBack: { upper: [-140, 15, 62], fore: -125, hand: [-73, 45, 0] },
        // 出手：握点 (-5, 23.5, 5)，(0, 0.1, 1)（0.9u、0.3°）
        javRelease: { upper: [-135, -20, 2], fore: -155, hand: [-87, -45, 0] },
    };

    MA.soldierAnimations = (H) => {
        const { merge, both, one, squat, LEGS, ZERO } = H;
        const arm = (side, frames) => {
            // frames: [[t, pose]]；右臂按卡片写的角度直接用，左臂同理（不镜像：两侧各自解）
            const ua = 'upper_arm_' + side;
            const fa = 'forearm_' + side;
            const hd = 'hand_' + side;
            return merge(
                one(ua, frames.map(([t, p]) => [t, p.upper])),
                one(fa, frames.map(([t, p]) => [t, [p.fore, 0, 0]])),
                one(hd, frames.map(([t, p]) => [t, p.hand])),
            );
        };
        const R = (frames) => arm('r', frames);
        const L = (frames) => arm('l', frames);
        /** 屈膝弓步：左脚在前（持盾的一侧向前），后脚伸。 */
        const lunge = (frames) => merge(
            one('thigh_l', frames.map(([t, k]) => [t, [-22 * k, 0, 0]])),
            one('shin_l', frames.map(([t, k]) => [t, [18 * k, 0, 0]])),
            one('foot_l', frames.map(([t, k]) => [t, [4 * k, 0, 0]])),
            one('thigh_r', frames.map(([t, k]) => [t, [16 * k, 0, 0]])),
            one('shin_r', frames.map(([t, k]) => [t, [6 * k, 0, 0]])),
            one('foot_r', frames.map(([t, k]) => [t, [-22 * k, 0, 0]])),
            // 骨盆下沉按两条腿里竖直跨度较大的那条算（大腿、小腿各 6u），保证着地的那只脚贴地
            one('root', frames.map(([t, k]) => {
                const c = (d) => Math.cos(d * Math.PI / 180);
                const front = 6 * c(-22 * k) + 6 * c(-4 * k);
                const back = 6 * c(16 * k) + 6 * c(22 * k);
                return [t, [0, -(12 - Math.max(front, back)), 0]];
            }), 'position'),
            one('hem_front', frames.map(([t, k]) => [t, [-14 * k, 0, 0]])),
            one('hem_back', frames.map(([t, k]) => [t, [8 * k, 0, 0]])),
        );
        return {
            // 站岗：持矛竖立、尾刺着地，盾垂在身侧；呼吸、偶尔转头看
            guard_idle: {
                length: 4.0,
                loop: 'loop',
                bones: merge(
                    R([[0, P.spearUpright]]),
                    L([[0, P.shieldRest]]),
                    one('torso', [[0, ZERO], [2.0, [-1.2, 0, 0]], [4.0, ZERO]]),
                    one('head', [[0, ZERO], [1.4, [0, 18, 0]], [2.2, [0, 18, 0]], [2.9, [0, -14, 0]], [3.4, [0, -14, 0]], [4.0, ZERO]]),
                ),
            },
            // 巡逻：走路的腿，矛竖持离地，盾不大摆
            patrol_walk: {
                length: 1.0,
                loop: 'loop',
                bones: merge(LEGS,
                    R([[0, P.spearCarry]]),
                    L([[0, P.shieldRest]]),
                    one('torso', [[0, [3, 3, 0]], [0.5, [3, -3, 0]], [1.0, [3, 3, 0]]]),
                ),
            },
            // 警觉（16 刻）：猛转头、屈膝，矛从竖持放平成下手待刺、举盾
            alert: {
                length: 0.8,
                loop: 'loop',
                bones: merge(
                    R([[0, P.spearUpright], [0.4, P.ready], [0.8, P.ready]]),
                    L([[0, P.shieldRest], [0.4, P.shieldUp], [0.8, P.shieldUp]]),
                    lunge([[0, 0], [0.3, 0.6], [0.8, 0.6]]),
                    one('torso', [[0, ZERO], [0.3, [8, 0, 0]], [0.8, [8, 0, 0]]]),
                    one('head', [[0, ZERO], [0.15, [-4, 0, 0]], [0.8, [-6, 0, 0]]]),
                ),
            },
            // 矛刺（24 刻）：前摇收矛、送肩扭腰下手刺出、收回
            spear_thrust: {
                length: 1.2,
                loop: 'loop',
                bones: merge(
                    R([[0, P.ready], [0.45, P.pullBack], [0.6, P.pullBack], [0.68, P.strike], [0.75, P.strike], [1.2, P.ready]]),
                    L([[0, P.shieldUp], [0.6, P.shieldUp], [0.75, P.shieldUp], [1.2, P.shieldUp]]),
                    lunge([[0, 0.6], [0.6, 0.5], [0.72, 1.0], [0.85, 1.0], [1.2, 0.6]]),
                    one('torso', [[0, [8, 0, 0]], [0.45, [2, -16, 0]], [0.6, [2, -16, 0]], [0.72, [16, 14, 0]], [0.85, [16, 14, 0]], [1.2, [8, 0, 0]]]),
                    one('head', [[0, [-6, 0, 0]], [0.6, [0, 14, 0]], [0.72, [-12, -12, 0]], [1.2, [-6, 0, 0]]]),
                ),
            },
            // 举盾：盾在胸前，右手下手平端待刺，屈膝
            block_hold: {
                length: 2.0,
                loop: 'loop',
                bones: merge(
                    R([[0, P.ready]]),
                    L([[0, P.shieldUp]]),
                    lunge([[0, 0.6], [1.0, 0.7], [2.0, 0.6]]),
                    one('torso', [[0, [8, 0, 0]], [1.0, [9, 0, 0]], [2.0, [8, 0, 0]]]),
                    one('head', [[0, [-6, 0, 0]]]),
                ),
            },
            // 拦截：举盾的上身 + 走路的腿（侧步插到居民与威胁之间）
            intercept_step: {
                length: 1.0,
                loop: 'loop',
                bones: merge(LEGS,
                    R([[0, P.ready]]),
                    L([[0, P.shieldUp]]),
                    one('torso', [[0, [8, 0, 0]]]),
                    one('head', [[0, [-6, 0, 0]]]),
                ),
            },
            // 投矛（32 刻）：左手前指、右臂后引、扭腰；第 20 刻出手，随身送出，收回
            javelin_throw: {
                length: 1.6,
                loop: 'loop',
                bones: merge(
                    R([[0, P.javReady], [0.7, P.javBack], [1.0, P.javBack], [1.05, P.javRelease], [1.25, P.javRelease], [1.6, P.javReady]]),
                    one('upper_arm_l', [[0, [-20, 0, 6]], [0.7, [-86, 0, 12]], [1.0, [-86, 0, 12]], [1.1, [-30, 0, 20]], [1.6, [-20, 0, 6]]]),
                    one('forearm_l', [[0, [-10, 0, 0]], [0.7, [-6, 0, 0]], [1.1, [-40, 0, 0]], [1.6, [-10, 0, 0]]]),
                    lunge([[0, 0.3], [0.7, 0.9], [1.0, 0.9], [1.1, 1.0], [1.6, 0.3]]),
                    one('torso', [[0, [2, 0, 0]], [0.7, [-8, -30, 0]], [1.0, [-8, -30, 0]], [1.05, [12, 22, 0]], [1.3, [14, 18, 0]], [1.6, [2, 0, 0]]]),
                    one('head', [[0, ZERO], [0.7, [4, 28, 0]], [1.0, [4, 28, 0]], [1.05, [-6, -18, 0]], [1.6, ZERO]]),
                ),
            },
            // 扬火（8 刻）：双手举起拍打火苗（武器收起）
            brazier_signal: {
                length: 0.4,
                loop: 'loop',
                bones: merge(
                    both('upper_arm_l', [[0, [-150, 0, 18]], [0.2, [-120, 0, 30]], [0.4, [-150, 0, 18]]]),
                    both('forearm_l', [[0, [-20, 0, 0]], [0.2, [-50, 0, 0]], [0.4, [-20, 0, 0]]]),
                    one('torso', [[0, [-6, 0, 0]], [0.2, [-2, 0, 0]], [0.4, [-6, 0, 0]]]),
                    one('head', [[0, [-14, 0, 0]]]),
                ),
            },
            // 训练：对空练刺（两刺一循环）
            train: {
                length: 2.0,
                loop: 'loop',
                bones: merge(
                    R([[0, P.ready], [0.4, P.pullBack], [0.55, P.strike], [0.8, P.ready], [1.2, P.pullBack], [1.35, P.strike],
                        [1.6, P.ready], [2.0, P.ready]]),
                    L([[0, P.shieldUp]]),
                    lunge([[0, 0.5], [0.55, 1.0], [0.8, 0.5], [1.35, 1.0], [1.6, 0.5], [2.0, 0.5]]),
                    one('torso', [[0, [8, 0, 0]], [0.4, [2, -14, 0]], [0.55, [16, 12, 0]], [0.8, [8, 0, 0]], [1.2, [2, -14, 0]],
                        [1.35, [16, 12, 0]], [1.6, [8, 0, 0]], [2.0, [8, 0, 0]]]),
                ),
            },
            // 角力：屈膝弯腰、双手前探抓对手肩臂，左右晃（兵装全收起）
            wrestle: {
                length: 2.0,
                loop: 'loop',
                bones: merge(
                    squat([[0, -30, 40], [1.0, -34, 46], [2.0, -30, 40]]),
                    one('torso', [[0, [34, 0, -6]], [0.5, [38, 6, 0]], [1.0, [36, 0, 6]], [1.5, [38, -6, 0]], [2.0, [34, 0, -6]]]),
                    one('head', [[0, [-24, 0, 0]]]),
                    both('upper_arm_l', [[0, [-70, 0, -12]], [0.5, [-80, 0, -16]], [1.0, [-66, 0, -10]], [1.5, [-80, 0, -16]], [2.0, [-70, 0, -12]]]),
                    both('forearm_l', [[0, [-46, 0, 0]], [1.0, [-34, 0, 0]], [2.0, [-46, 0, 0]]]),
                ),
            },
        };
    };
    return 'soldier animations defined';
})();
