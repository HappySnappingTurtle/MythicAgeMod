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

    const COS = (d) => Math.cos(d * Math.PI / 180);
    /**
     * 下蹲：frames = [[时刻, 大腿 t（负 = 前抬）, 小腿 s（正 = 后弯）], …]。根骨下沉量按腿长算（大腿、小腿各 6u），
     * 脚掌转回水平，保证脚底贴地：下沉 = 12 − 6·cos t − 6·cos(t + s)。
     */
    const squat = (frames) => merge(
        both('thigh_l', frames.map(([tm, t]) => [tm, [t, 0, 0]])),
        both('shin_l', frames.map(([tm, t, s]) => [tm, [s, 0, 0]])),
        both('foot_l', frames.map(([tm, t, s]) => [tm, [-(t + s), 0, 0]])),
        one('root', frames.map(([tm, t, s]) => [tm, [0, -(12 - 6 * COS(t) - 6 * COS(t + s)), 0]]), 'position'),
        // 长袍下摆：前片随大腿向前抬，后片向后搭（蹲深了才不会扎进地里）
        one('hem_front', frames.map(([tm, t]) => [tm, [t * 0.95, 0, 0]])),
        one('hem_back', frames.map(([tm, t]) => [tm, [-t * 1.2, 0, 0]])),
    );
    const rx = (bone, frames) => one(bone, frames.map(([t, v]) => [t, typeof v === 'number' ? [v, 0, 0] : v]));
    /** 下蹲时手臂也跟着降低，这里只写上身。 */

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

        // ───────────── 工作动作（T19 阶段 ②b）─────────────
        // 说话：右手比划、点头
        chat: {
            length: 3.0,
            loop: 'loop',
            bones: merge(
                rx('upper_arm_r', [[0, [-10, 0, -4]], [0.8, [-35, 10, -6]], [1.5, [-25, 0, -4]], [2.2, [-40, 15, -8]], [3.0, [-10, 0, -4]]]),
                rx('forearm_r', [[0, -10], [0.8, -50], [1.5, -30], [2.2, -60], [3.0, -10]]),
                rx('head', [[0, ZERO], [0.8, [4, 8, 0]], [1.5, [-2, 0, 0]], [2.2, [4, -6, 0]], [3.0, ZERO]]),
                rx('upper_arm_l', [[0, [0, 0, 3]]]),
            ),
        },
        // 挖黏土（木铲）：铲头扎下 → 撬起
        dig: {
            length: 1.6,
            loop: 'loop',
            bones: merge(
                squat([[0, -10, 15], [0.6, -28, 40], [1.0, -20, 30], [1.6, -10, 15]]),
                rx('torso', [[0, 15], [0.6, 35], [1.0, 25], [1.6, 15]]),
                rx('head', [[0, 10]]),
                rx('upper_arm_r', [[0, -20], [0.6, -50], [1.0, -35], [1.6, -20]]),
                rx('forearm_r', [[0, -45], [0.6, -15], [1.0, -50], [1.6, -45]]),
                rx('hand_r', [[0, 30], [0.6, 40], [1.0, 20], [1.6, 30]]),
                rx('upper_arm_l', [[0, [-40, -15, -10]], [0.6, [-55, -15, -10]], [1.6, [-40, -15, -10]]]),
                rx('forearm_l', [[0, -30], [0.6, -20], [1.6, -30]]),
            ),
        },
        // 采石：半蹲，左手扶楔，右手抡木槌砸下
        quarry_wedge: {
            length: 1.4,
            loop: 'loop',
            bones: merge(
                squat([[0, -30, 45]]),
                rx('torso', [[0, 28], [0.5, 36], [1.4, 28]]),
                rx('head', [[0, 12]]),
                rx('upper_arm_l', [[0, [-45, -10, -6]]]),
                rx('forearm_l', [[0, -15]]),
                rx('hand_l', [[0, 60]]),
                rx('upper_arm_r', [[0, [-150, 0, -8]], [0.5, [-55, 0, -6]], [0.7, [-70, 0, -6]], [1.4, [-150, 0, -8]]]),
                rx('forearm_r', [[0, -25], [0.5, -10], [1.4, -25]]),
                rx('hand_r', [[0, 0], [0.5, 70], [0.7, 50], [1.4, 0]]),
            ),
        },
        // 修枝：仰头，左手扶枝，右手举钩刀割
        prune: {
            length: 1.5,
            loop: 'loop',
            bones: merge(
                rx('head', [[0, -25]]),
                rx('torso', [[0, -6]]),
                rx('upper_arm_l', [[0, [-145, 0, 8]]]),
                rx('forearm_l', [[0, -15]]),
                rx('upper_arm_r', [[0, [-130, 0, -10]], [0.6, [-140, 0, -10]], [0.9, [-125, 0, -10]], [1.5, [-130, 0, -10]]]),
                rx('forearm_r', [[0, -30], [0.6, -60], [0.9, -20], [1.5, -30]]),
            ),
        },
        // 剪羊毛：俯身，双手在低处剪
        shear: {
            length: 1.2,
            loop: 'loop',
            bones: merge(
                squat([[0, -40, 60]]),
                rx('torso', [[0, 36]]),
                rx('head', [[0, 10]]),
                rx('upper_arm_l', [[0, [-35, -10, -8]]]),
                rx('forearm_l', [[0, -20]]),
                rx('upper_arm_r', [[0, [-40, 0, -6]]]),
                rx('forearm_r', [[0, -25], [0.3, -38], [0.6, -25], [0.9, -38], [1.2, -25]]),
                rx('hand_r', [[0, 60]]),
            ),
        },
        // 打橄榄：双手举长杆，杆梢从近乎竖直打到斜前方的树冠（大英博物馆 B 226 瓶画）
        olive_beat: {
            length: 1.6,
            loop: 'loop',
            bones: merge(
                rx('upper_arm_r', [[0, [-85, -12, -10]], [0.5, [-50, -12, -10]], [0.7, [-58, -12, -10]], [1.6, [-85, -12, -10]]]),
                rx('forearm_r', [[0, -5]]),
                rx('upper_arm_l', [[0, [-80, 20, -2]], [0.5, [-45, 20, -2]], [0.7, [-52, 20, -2]], [1.6, [-80, 20, -2]]]),
                rx('forearm_l', [[0, -25]]),
                rx('torso', [[0, -8], [0.5, 8], [1.6, -8]]),
                rx('head', [[0, -20], [0.5, -10], [1.6, -20]]),
                both('thigh_l', [[0, [-6, 0, 4]]]),
                both('shin_l', [[0, [6, 0, 0]]]),
            ),
        },
        // 摘葡萄：双手在胸前高处交替摘
        vine_pick: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                rx('upper_arm_l', [[0, [-75, -10, 0]], [0.25, [-90, -10, 0]], [0.5, [-75, -10, 0]], [1.0, [-75, -10, 0]]]),
                rx('forearm_l', [[0, -15], [0.25, -5], [0.5, -15]]),
                rx('upper_arm_r', [[0, [-75, 10, 0]], [0.5, [-75, 10, 0]], [0.75, [-90, 10, 0]], [1.0, [-75, 10, 0]]]),
                rx('forearm_r', [[0, -15], [0.5, -15], [0.75, -5], [1.0, -15]]),
                rx('head', [[0, -6]]),
            ),
        },
        // 割麦：俯身半蹲，左手拢住麦秆，右手镰刀横扫
        scythe_harvest: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                squat([[0, -35, 55]]),
                rx('torso', [[0, [40, 8, 0]], [0.5, [40, -8, 0]], [1.0, [40, 8, 0]]]),
                rx('head', [[0, 8]]),
                rx('upper_arm_l', [[0, [-45, -20, -6]]]),
                rx('forearm_l', [[0, -10]]),
                rx('upper_arm_r', [[0, [-45, 35, -10]], [0.5, [-50, -25, -10]], [1.0, [-45, 35, -10]]]),
                rx('forearm_r', [[0, -10]]),
                rx('hand_r', [[0, 60]]),
            ),
        },
        // 扬场：木叉铲起 → 高高扬起，糠随风散去
        winnow: {
            length: 2.0,
            loop: 'loop',
            bones: merge(
                squat([[0, -18, 28], [0.8, -4, 6], [2.0, -18, 28]]),
                rx('torso', [[0, 18], [0.8, -10], [2.0, 18]]),
                rx('head', [[0, 8], [0.8, -20], [2.0, 8]]),
                rx('upper_arm_r', [[0, [-30, 0, -6]], [0.8, [-125, 0, -8]], [1.1, [-115, 0, -8]], [2.0, [-30, 0, -6]]]),
                rx('forearm_r', [[0, -20], [0.8, -10], [2.0, -20]]),
                rx('hand_r', [[0, 25], [0.8, 0], [2.0, 25]]),
                rx('upper_arm_l', [[0, [-50, -18, -2]], [0.8, [-105, -18, -2]], [2.0, [-50, -18, -2]]]),
                rx('forearm_l', [[0, -25], [0.8, -15], [2.0, -25]]),
            ),
        },
        // 伐木：双手抡斧过头，劈到身前
        chop: {
            length: 1.2,
            loop: 'loop',
            bones: merge(
                both('thigh_l', [[0, [-4, 0, 6]]]),
                both('shin_l', [[0, [6, 0, 0]]]),
                rx('torso', [[0, -10], [0.45, 26], [0.6, 20], [1.2, -10]]),
                rx('head', [[0, -6], [0.45, 10], [1.2, -6]]),
                rx('upper_arm_r', [[0, [-165, -10, -8]], [0.45, [-60, -15, -8]], [0.6, [-70, -15, -8]], [1.2, [-165, -10, -8]]]),
                rx('forearm_r', [[0, -20], [0.45, -5], [1.2, -20]]),
                rx('hand_r', [[0, 0], [0.45, 55], [0.6, 45], [1.2, 0]]),
                rx('upper_arm_l', [[0, [-160, 22, 6]], [0.45, [-55, 25, 6]], [0.6, [-65, 25, 6]], [1.2, [-160, 22, 6]]]),
                rx('forearm_l', [[0, -25], [0.45, -15], [1.2, -25]]),
            ),
        },
        // 锯木板：左手按住木料，右手推拉手锯（锯条保持水平向前）
        saw: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                squat([[0, -15, 25]]),
                rx('torso', [[0, 26]]),
                rx('head', [[0, 14]]),
                rx('upper_arm_r', [[0, [-40, 0, -4]], [0.5, [-65, 0, -4]], [1.0, [-40, 0, -4]]]),
                rx('forearm_r', [[0, -45], [0.5, -5], [1.0, -45]]),
                rx('hand_r', [[0, 85], [0.5, 70], [1.0, 85]]),
                rx('upper_arm_l', [[0, [-50, -15, -6]]]),
                rx('forearm_l', [[0, -30]]),
            ),
        },
        // 推压榨杆：俯身弓步用力推，一推一松
        press_turn: {
            length: 2.0,
            loop: 'loop',
            bones: merge(
                squat([[0, -18, 26], [1.0, -26, 36], [2.0, -18, 26]]),
                rx('torso', [[0, 22], [1.0, 32], [2.0, 22]]),
                rx('head', [[0, 6]]),
                both('upper_arm_l', [[0, [-80, 0, -6]], [1.0, [-88, 0, -6]], [2.0, [-80, 0, -6]]]),
                both('forearm_l', [[0, -14], [1.0, -2], [2.0, -14]]),
            ),
        },
        // 烤饼：蹲在灶前，把饼放上去
        bake: {
            length: 1.6,
            loop: 'loop',
            bones: merge(
                squat([[0, -50, 80]]),
                rx('torso', [[0, 28], [0.8, 34], [1.6, 28]]),
                rx('head', [[0, 10]]),
                both('upper_arm_l', [[0, [-50, 0, -6]], [0.8, [-65, 0, -6]], [1.6, [-50, 0, -6]]]),
                both('forearm_l', [[0, -20], [0.8, -10], [1.6, -20]]),
            ),
        },
        // 揉泥：俯身，双掌向下推压
        pottery_knead: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                rx('torso', [[0, 28], [0.4, 34], [1.0, 28]]),
                rx('head', [[0, 14]]),
                both('upper_arm_l', [[0, [-55, 0, -8]]]),
                both('forearm_l', [[0, -20], [0.4, -4], [1.0, -20]]),
                one('root', [[0, ZERO], [0.4, [0, -0.3, 0]], [1.0, ZERO]], 'position'),
            ),
        },
        // 拉坯：坐在陶轮前，双手在低处扶着转动的坯
        pottery_throw: {
            length: 2.0,
            loop: 'loop',
            bones: merge(
                one('root', [[0, [0, -5.6, 0]]], 'position'),
                both('thigh_l', [[0, [-88, 0, 10]]]),
                both('shin_l', [[0, [88, 0, 0]]]),
                one('hem_front', [[0, [-84, 0, 0]]]),
                one('hem_back', [[0, [88, 0, 0]]]),
                rx('torso', [[0, [22, 0, -2]], [1.0, [24, 0, 2]], [2.0, [22, 0, -2]]]),
                rx('head', [[0, 18]]),
                both('upper_arm_l', [[0, [-55, 0, -10]]]),
                both('forearm_l', [[0, -30], [1.0, -38], [2.0, -30]]),
            ),
        },
        // 装窑：从胸前把坯托起，送进窑口
        kiln_load: {
            length: 1.5,
            loop: 'loop',
            bones: merge(
                rx('torso', [[0, 6], [0.8, 16], [1.5, 6]]),
                one('thigh_l', [[0, ZERO], [0.8, [-16, 0, 0]], [1.5, ZERO]]),
                one('shin_l', [[0, ZERO], [0.8, [14, 0, 0]], [1.5, ZERO]]),
                both('upper_arm_l', [[0, [-30, 0, -8]], [0.8, [-95, 0, -6]], [1.5, [-30, 0, -8]]]),
                both('forearm_l', [[0, -50], [0.8, -10], [1.5, -50]]),
            ),
        },
        // 出窑：伸进窑口取出，抱回胸前
        kiln_unload: {
            length: 1.5,
            loop: 'loop',
            bones: merge(
                rx('torso', [[0, 16], [0.8, 6], [1.5, 16]]),
                one('thigh_l', [[0, [-16, 0, 0]], [0.8, ZERO], [1.5, [-16, 0, 0]]]),
                one('shin_l', [[0, [14, 0, 0]], [0.8, ZERO], [1.5, [14, 0, 0]]]),
                both('upper_arm_l', [[0, [-95, 0, -6]], [0.8, [-30, 0, -8]], [1.5, [-95, 0, -6]]]),
                both('forearm_l', [[0, -10], [0.8, -50], [1.5, -10]]),
            ),
        },
        // 凿石：左手执凿抵住石面，右手锤击（定位—抬锤—接触—回摆）
        chisel_strike: {
            length: 0.8,
            loop: 'loop',
            bones: merge(
                squat([[0, -12, 20]]),
                rx('torso', [[0, 24]]),
                rx('head', [[0, 16]]),
                rx('upper_arm_l', [[0, [-50, -12, -8]]]),
                rx('forearm_l', [[0, -20]]),
                rx('hand_l', [[0, 60]]),
                rx('upper_arm_r', [[0, [-110, 0, -6]], [0.3, [-60, -8, -6]], [0.45, [-68, -8, -6]], [0.8, [-110, 0, -6]]]),
                rx('forearm_r', [[0, -40], [0.3, -20], [0.8, -40]]),
                rx('hand_r', [[0, 0], [0.3, 50], [0.8, 0]]),
            ),
        },
        // 配青铜料：右手在炉前搅拌，左手扶着
        bronze_mix: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                rx('torso', [[0, 18]]),
                rx('head', [[0, 12]]),
                rx('upper_arm_r', [[0, [-60, 15, -6]], [0.25, [-68, 0, -6]], [0.5, [-60, -15, -6]], [0.75, [-52, 0, -6]], [1.0, [-60, 15, -6]]]),
                rx('forearm_r', [[0, -20]]),
                rx('upper_arm_l', [[0, [-40, -10, -6]]]),
                rx('forearm_l', [[0, -25]]),
            ),
        },
        // 鼓风：半蹲，双手一下下压风箱
        bellows: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                squat([[0, -30, 45], [0.5, -34, 52], [1.0, -30, 45]]),
                rx('torso', [[0, 20], [0.5, 28], [1.0, 20]]),
                rx('head', [[0, 6]]),
                both('upper_arm_l', [[0, [-45, 0, -8]], [0.5, [-30, 0, -8]], [1.0, [-45, 0, -8]]]),
                both('forearm_l', [[0, -40], [0.5, -5], [1.0, -40]]),
            ),
        },
        // 踩葡萄：原地高抬腿踩踏，双臂张开保持平衡
        tread_grapes: {
            length: 1.0,
            loop: 'loop',
            bones: merge(
                one('thigh_l', [[0, ZERO], [0.25, [-42, 0, 0]], [0.5, ZERO], [1.0, ZERO]]),
                one('shin_l', [[0, ZERO], [0.25, [55, 0, 0]], [0.5, ZERO], [1.0, ZERO]]),
                one('thigh_r', [[0, ZERO], [0.5, ZERO], [0.75, [-42, 0, 0]], [1.0, ZERO]]),
                one('shin_r', [[0, ZERO], [0.5, ZERO], [0.75, [55, 0, 0]], [1.0, ZERO]]),
                one('torso', [[0, [0, 0, 3]], [0.5, [0, 0, -3]], [1.0, [0, 0, 3]]]),
                both('upper_arm_l', [[0, [-10, 0, 28]], [0.5, [-14, 0, 34]], [1.0, [-10, 0, 28]]]),
                both('forearm_l', [[0, -25]]),
                one('root', [[0, ZERO], [0.25, [0, 0.4, 0]], [0.5, ZERO], [0.75, [0, 0.4, 0]], [1.0, ZERO]], 'position'),
            ),
        },
        // 祈祷（T19 阶段 ④）：站立、双手上举、掌心向上向前，抬头望天——古希腊人祈祷的常见姿势
        // （Iliad 1.450「举起双手」；Burkert《Greek Religion》1985 的祈祷一节，经 Wikipedia「Prayer」「Raising hands」转引）
        pray: {
            length: 4.0,
            loop: 'loop',
            bones: merge(
                rx('torso', [[0, -4], [2.0, -6], [4.0, -4]]),
                rx('head', [[0, -14], [2.0, -18], [4.0, -14]]),
                both('upper_arm_l', [[0, [-125, 0, -32]], [2.0, [-132, 0, -36]], [4.0, [-125, 0, -32]]]),
                both('forearm_l', [[0, -22], [2.0, -16], [4.0, -22]]),
                both('hand_l', [[0, [-30, 0, 0]]]),
            ),
        },
        // 跪着祈祷：同一资料说女性祈求救助时有时会跪下；单膝着地、双手上举
        pray_kneel: {
            length: 4.0,
            loop: 'loop',
            bones: merge(
                one('thigh_l', [[0, [-90, 0, 0]]]),
                one('shin_l', [[0, [90, 0, 0]]]),
                one('foot_l', [[0, [0, 0, 0]]]),
                one('thigh_r', [[0, [0, 0, 0]]]),
                one('shin_r', [[0, [90, 0, 0]]]),
                one('foot_r', [[0, [-90, 0, 0]]]),
                one('root', [[0, [0, -6, 0]]], 'position'),
                one('hem_front', [[0, [-80, 0, 0]]]),
                one('hem_back', [[0, [70, 0, 0]]]),
                rx('torso', [[0, -2], [2.0, -4], [4.0, -2]]),
                rx('head', [[0, -14], [2.0, -18], [4.0, -14]]),
                both('upper_arm_l', [[0, [-125, 0, -32]], [2.0, [-132, 0, -36]], [4.0, [-125, 0, -32]]]),
                both('forearm_l', [[0, -22], [2.0, -16], [4.0, -22]]),
                both('hand_l', [[0, [-30, 0, 0]]]),
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

    /** 握持约束：工具骨骼相对手只能绕柄（卡片 Z）转；姿势一律靠上臂、前臂、手腕。 */
    function checkGrip(name, def) {
        for (const [bone, ch] of Object.entries(def.bones)) {
            if (bone.startsWith('tool_') && ch.rotation && ch.rotation.some(([, v]) => Math.abs(v[0]) > 1e-6 || Math.abs(v[1]) > 1e-6)) {
                throw new Error('动画 ' + name + ' 让 ' + bone + ' 离开握孔方向（只能转 rz）');
            }
        }
    }

    function build() {
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
                if (c.parent && c.parent.name && (c.parent.name.startsWith('carry_') || c.parent.name.startsWith('tool_'))) continue;
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

    /**
     * 双手握的工具（长杆、木叉、斧、木铲）：右手握着工具，左手的上臂、前臂角度在 Blockbench 里网格搜索，
     * 让左拳的握点落到柄上指定的一段（卡片 z 区间，柄沿右手握孔 +Z），再把结果写成左臂关键帧（覆盖手写的）。
     */
    const TWO_HANDED = {
        olive_beat: { tool: 'tool_pole', seg: [5, 14] },
        winnow: { tool: 'tool_fork', seg: [3, 15] },
        chop: { tool: 'tool_axe', seg: [-4.5, -1.5] },
        dig: { tool: 'tool_spade', seg: [5, 11] },
    };
    const RAD = Math.PI / 180;

    function solveLeftGrips() {
        const report = {};
        const group = (n) => Group.all.find((g) => g.name === n);
        const ua = group('upper_arm_l');
        const fa = group('forearm_l');
        const hand = group('hand_l');
        const uaR = group('upper_arm_r');
        const handR = group('hand_r');
        for (const [name, cfg] of Object.entries(TWO_HANDED)) {
            const anim = Animation.all.find((a) => a.name === name);
            const tool = group(cfg.tool);
            const rightFrames = ANIMS[name].bones.upper_arm_r.rotation;
            const times = [...new Set(rightFrames.map(([t]) => t))];
            const solved = [];
            const solvedRight = [];
            for (const t of times) {
                anim.select();
                Timeline.setTime(t);
                Animator.preview();
                // ① 右臂保持手写的抬起角度（rx），只搜内收（ry、rz），让右拳靠到身体中线（卡片 x ≈ −0.5）
                const rxR = rightFrames.find(([ft]) => ft === t)[1][0];
                const pr = new THREE.Vector3();
                let bestR = null;
                for (let ryv = -70; ryv <= 70; ryv += 5) {
                    for (let rzv = -20; rzv <= 70; rzv += 5) {
                        uaR.mesh.rotation.set(-rxR * RAD, ryv * RAD, -rzv * RAD);
                        uaR.mesh.updateMatrixWorld(true);
                        handR.mesh.localToWorld(pr.set(0, -1.5, 0));
                        const cost = Math.abs(pr.x - 0.5) + 0.01 * (Math.abs(ryv) + Math.abs(rzv));
                        if (!bestR || cost < bestR.cost) bestR = { cost, v: [rxR, ryv, rzv] };
                    }
                }
                uaR.mesh.rotation.set(-bestR.v[0] * RAD, bestR.v[1] * RAD, -bestR.v[2] * RAD);
                uaR.mesh.updateMatrixWorld(true);
                solvedRight.push([t, bestR.v]);
                tool.mesh.updateMatrixWorld(true);
                const a = tool.mesh.localToWorld(new THREE.Vector3(0, 0, -cfg.seg[0]));
                const b = tool.mesh.localToWorld(new THREE.Vector3(0, 0, -cfg.seg[1]));
                const ab = b.clone().sub(a);
                const len2 = ab.lengthSq();
                const p = new THREE.Vector3();
                const evalAt = (rxv, ryv, rzv, frx) => {
                    ua.mesh.rotation.set(-rxv * RAD, ryv * RAD, -rzv * RAD);
                    fa.mesh.rotation.set(-frx * RAD, 0, 0);
                    ua.mesh.updateMatrixWorld(true);
                    hand.mesh.localToWorld(p.set(0, -1.5, 0));
                    const f = Math.max(0, Math.min(1, p.clone().sub(a).dot(ab) / len2));
                    const d = p.distanceTo(a.clone().add(ab.clone().multiplyScalar(f)));
                    return { cost: d + 0.002 * (Math.abs(ryv) + Math.abs(rzv)) + 0.001 * Math.abs(frx), d,
                        ua: [rxv, ryv, rzv], fa: [frx, 0, 0] };
                };
                let best = null;
                const consider = (c) => {
                    if (!best || c.cost < best.cost) best = c;
                };
                // 粗搜 10°，再在最好的一组周围细搜 2.5°
                for (let rxv = -175; rxv <= 10; rxv += 10) {
                    for (let ryv = -90; ryv <= 90; ryv += 10) {
                        for (let rzv = -110; rzv <= 40; rzv += 10) {
                            for (let frx = -130; frx <= 0; frx += 10) {
                                consider(evalAt(rxv, ryv, rzv, frx));
                            }
                        }
                    }
                }
                const c0 = best;
                for (let dx = -10; dx <= 10; dx += 2.5) {
                    for (let dy = -10; dy <= 10; dy += 2.5) {
                        for (let dz = -10; dz <= 10; dz += 2.5) {
                            for (let df = -10; df <= 10; df += 5) {
                                consider(evalAt(c0.ua[0] + dx, c0.ua[1] + dy, c0.ua[2] + dz, Math.min(0, c0.fa[0] + df)));
                            }
                        }
                    }
                }
                solved.push([t, best]);
            }
            // 写回：右上臂换成内收后的角度；去掉手写的左臂关键帧，换成解出来的
            {
                const animator = anim.getBoneAnimator(uaR);
                for (const kf of animator.rotation.slice()) {
                    kf.remove();
                }
                for (const [t, v] of solvedRight) {
                    const bb = A.rot(v);
                    animator.addKeyframe({ channel: 'rotation', time: t, interpolation: 'linear', data_points: [{ x: bb[0], y: bb[1], z: bb[2] }] });
                }
            }
            for (const bone of ['upper_arm_l', 'forearm_l']) {
                const animator = anim.getBoneAnimator(group(bone));
                for (const kf of animator.rotation.slice()) {
                    kf.remove();
                }
                for (const [t, s] of solved) {
                    const v = A.rot(bone === 'upper_arm_l' ? s.ua : s.fa);
                    animator.addKeyframe({ channel: 'rotation', time: t, interpolation: 'linear', data_points: [{ x: v[0], y: v[1], z: v[2] }] });
                }
            }
            report[name] = solved.map(([t, s], i) => [t, Math.round(s.d * 100) / 100, s.ua, s.fa[0], solvedRight[i][1]]);
        }
        Animator.preview();
        return report;
    }

    MA.residentAnimations = (rel) => {
        build();
        Modes.options.animate.select();
        MA.residentGripReport = solveLeftGrips();
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
