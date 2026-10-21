// T7 §5 弥诺陶洛斯动画（数据 + 生成）。由 build_minotaur.js 调用；也可单独运行以只重建动画：
//   window.MA.run('tools/blockbench/minotaur_anim.js'); window.MA.buildMinotaurAnimations();
//
// 关键帧用设计卡的语义书写：旋转 [rx, ry, rz]（度，右手系，+X 角色左、+Y 上、+Z 前），位移 [x, y, z]（u）。
// 常用方向：rx 为负 = 肢体向前抬起；rx 为正 = 躯干/头向前俯、小腿向后屈；左臂外展 rz 为正、右臂外展 rz 为负。
// 写入 Blockbench 时经 axes.js 换算；Bedrock 导出器再按自己的约定写文件，GeckoLib 按同一约定读取。
(function () {
    const MA = window.MA;
    MA.run('tools/blockbench/lib/axes.js');
    MA.run('tools/blockbench/lib/clearance.js');
    const A = MA.axes;

    // 握持约束：斧柄在零姿势下沿 +Z 穿过左拳的握孔（拇指一侧进、小指一侧出）。
    // 摆姿势只能转上臂、前臂、手腕；斧相对手只允许绕斧柄自身（卡片 Z 轴）旋转，
    // 否则斧柄会斜着从指节面或腕面穿出，看起来像插进手里（用户 2026-10-14 指出）。
    // 待机：上臂外展，前臂前抬 60°、手腕再上翻 30°，合计 90° 使斧柄竖直；刃面绕柄转 40°，让开头部。
    // 上臂外展 rz 会把竖直的斧柄带歪（顶端倒向头部），前臂用相反的 rz 抵消，让斧柄保持竖直、斧刃落在肩外侧。
    // 上臂略向后收、前臂少抬、手腕多翻，三者合计约 -90°，斧柄竖直且斧刃退到肩旁，不挡脸。
    const HOLD = {
        upper_arm_l: [12, 0, 26],
        forearm_l: [-52, 0, -26],
        hand_l: [-50, 0, 0],
        axe: [0, 0, -40],
    };
    // 冲锋（用户 2026-10-14：整个身体弯下、头低下、腰更弯、武器跟着身体走）：
    // 骨盆 22° + 躯干 40°，上身合计约 62°；颈 +8°、头 +8°，头总俯角约 78°——公牛的角朝上长，
    // 头压到接近朝地，角才指向正前方。
    // 斧与头同向（用户 2026-10-14）：斧刃指向与牛角一致——头俯下约 78° 后牛角大致水平朝前，
    // 斧也平端在头外侧朝前，与牛角一起指向目标。角度由 Blockbench 中的网格搜索得出：
    // 在冲刺帧上遍历上臂/前臂/手腕，取斧刃方向与牛角方向夹角最小（约 3°）且不碰头、角、地面的一组。
    // 斧相对手仍只绕柄旋转；自转角让刃面在冲刺中竖立，不横向戳到牛角。
    const TUCK = {
        upper_arm_l: [20, 0, 22],
        forearm_l: [-90, 0, -22],
        hand_l: [0, 0, 0],
        axe: [0, 0, -10], // 绕柄自转到刃面竖立（同样由搜索得出）
    };

    // 蓄力末尾的伏身姿势（冲刺第一帧与之衔接）。蹄子的世界角 = 骨盆 + 大腿 + 小腿 + 蹄，保持约 0°（蹄底贴地）。
    const CROUCH = {
        pelvis: [22, 0, 0],
        torso: [40, 0, 0],
        neck: [8, 0, 0],
        head: [8, 0, 0],
        thigh_l: [-46, 0, 0], shin_l: [60, 0, 0], hoof_l: [-36, 0, 0],
        thigh_r: [-16, 0, 0], shin_r: [36, 0, 0], hoof_r: [-42, 0, 0],
        upper_arm_r: [-56, 0, -12], forearm_r: [-40, 0, 0],
        tail_root: [-55, 0, 0], tail_tip: [-10, 0, 0],
    };

    const k = (pairs) => pairs.map(([t, v]) => [t, v]);
    const hold = (len, bones, extra) => {
        const out = {};
        for (const [b, v] of Object.entries(bones)) {
            out[b] = { rotation: k([[0, v], [len, v]]) };
        }
        return Object.assign(out, extra || {});
    };

    const ANIMS = {
        idle: {
            length: 3.0,
            loop: 'loop',
            groundLock: [0, 1.5, 3.0],
            // 基础站姿：上身前倾、膝微屈、头保持平视（概念图的压迫感）；呼吸叠加在基础姿势上
            bones: hold(3.0, HOLD, {
                torso: { rotation: k([[0, [6, 0, 0]], [1.5, [7.5, 0, 0]], [3.0, [6, 0, 0]]]), scale: k([[0, [1, 1, 1]], [1.5, [1.02, 1.03, 1.03]], [3.0, [1, 1, 1]]]) },
                neck: { rotation: k([[0, [-3, 0, 0]], [3.0, [-3, 0, 0]]]) },
                head: { rotation: k([[0, [-3, 0, 0]], [1.0, [-1, -3, 0]], [2.2, [-4, 2, 0]], [3.0, [-3, 0, 0]]]) },
                jaw: { rotation: k([[0, [0, 0, 0]], [1.4, [4, 0, 0]], [1.8, [0, 0, 0]], [3.0, [0, 0, 0]]]) },
                tail_root: { rotation: k([[0, [12, 0, 6]], [1.5, [12, 0, -6]], [3.0, [12, 0, 6]]]) },
                tail_tip: { rotation: k([[0, [6, 0, -8]], [1.5, [6, 0, 8]], [3.0, [6, 0, -8]]]) },
                upper_arm_r: { rotation: k([[0, [-4, 0, -7]], [1.5, [-6, 0, -9]], [3.0, [-4, 0, -7]]]) },
                forearm_r: { rotation: k([[0, [-14, 0, 0]], [3.0, [-14, 0, 0]]]) },
                upper_arm_l: { rotation: k([[0, HOLD.upper_arm_l], [1.5, [10, 0, 28]], [3.0, HOLD.upper_arm_l]]) },
                thigh_l: { rotation: k([[0, [-6, 0, 0]], [3.0, [-6, 0, 0]]]) },
                thigh_r: { rotation: k([[0, [-6, 0, 0]], [3.0, [-6, 0, 0]]]) },
                shin_l: { rotation: k([[0, [10, 0, 0]], [3.0, [10, 0, 0]]]) },
                shin_r: { rotation: k([[0, [10, 0, 0]], [3.0, [10, 0, 0]]]) },
                hoof_l: { rotation: k([[0, [-4, 0, 0]], [3.0, [-4, 0, 0]]]) },
                hoof_r: { rotation: k([[0, [-4, 0, 0]], [3.0, [-4, 0, 0]]]) },
            }),
        },
        walk: {
            length: 1.0,
            loop: 'loop',
            groundLock: [0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0],
            bones: {
                thigh_l: { rotation: k([[0, [-24, 0, 0]], [0.5, [24, 0, 0]], [1.0, [-24, 0, 0]]]) },
                thigh_r: { rotation: k([[0, [24, 0, 0]], [0.5, [-24, 0, 0]], [1.0, [24, 0, 0]]]) },
                shin_l: { rotation: k([[0, [0, 0, 0]], [0.25, [10, 0, 0]], [0.5, [0, 0, 0]], [0.75, [38, 0, 0]], [1.0, [0, 0, 0]]]) },
                shin_r: { rotation: k([[0, [0, 0, 0]], [0.25, [38, 0, 0]], [0.5, [0, 0, 0]], [0.75, [10, 0, 0]], [1.0, [0, 0, 0]]]) },
                hoof_l: { rotation: k([[0, [24, 0, 0]], [0.5, [-24, 0, 0]], [1.0, [24, 0, 0]]]) },
                hoof_r: { rotation: k([[0, [-24, 0, 0]], [0.5, [24, 0, 0]], [1.0, [-24, 0, 0]]]) },
                pelvis: { position: k([[0, [0, 0, 0]], [0.25, [0, 0.8, 0]], [0.5, [0, 0, 0]], [0.75, [0, 0.8, 0]], [1.0, [0, 0, 0]]]) },
                torso: { rotation: k([[0, [4, 4, 0]], [0.5, [4, -4, 0]], [1.0, [4, 4, 0]]]) },
                upper_arm_r: { rotation: k([[0, [-22, 0, -4]], [0.5, [22, 0, -4]], [1.0, [-22, 0, -4]]]) },
                forearm_r: { rotation: k([[0, [-18, 0, 0]], [0.5, [-4, 0, 0]], [1.0, [-18, 0, 0]]]) },
                upper_arm_l: { rotation: k([[0, [16, 0, 26]], [0.5, [8, 0, 26]], [1.0, [16, 0, 26]]]) },
                forearm_l: { rotation: k([[0, HOLD.forearm_l], [1.0, HOLD.forearm_l]]) },
                hand_l: { rotation: k([[0, HOLD.hand_l], [1.0, HOLD.hand_l]]) },
                axe: { rotation: k([[0, HOLD.axe], [1.0, HOLD.axe]]) },
                tail_root: { rotation: k([[0, [16, 0, 8]], [0.5, [16, 0, -8]], [1.0, [16, 0, 8]]]) },
                head: { rotation: k([[0, [2, -3, 0]], [0.5, [2, 3, 0]], [1.0, [2, -3, 0]]]) },
            },
        },
        charge_windup: {
            length: 1.0,
            loop: 'hold',
            // 0–0.5 秒伏身（从待机过渡到 CROUCH）；0.5–1.0 秒右蹄刨地两次、鼻息时头上下点动
            groundLock: [0, 0.25, 0.5, 0.58, 0.68, 0.78, 0.88, 1.0],
            bones: {
                pelvis: { rotation: k([[0, [0, 0, 0]], [0.5, CROUCH.pelvis], [1.0, CROUCH.pelvis]]) },
                torso: { rotation: k([[0, [6, 0, 0]], [0.5, CROUCH.torso], [1.0, CROUCH.torso]]) },
                neck: { rotation: k([[0, [-3, 0, 0]], [0.5, CROUCH.neck], [1.0, CROUCH.neck]]) },
                head: { rotation: k([[0, [-3, 0, 0]], [0.5, CROUCH.head], [0.6, [-2, 0, 0]], [0.7, CROUCH.head], [0.85, [-2, 0, 0]], [1.0, CROUCH.head]]) },
                jaw: { rotation: k([[0, [0, 0, 0]], [0.6, [8, 0, 0]], [0.7, [0, 0, 0]], [0.85, [8, 0, 0]], [1.0, [0, 0, 0]]]) },
                upper_arm_r: { rotation: k([[0, [-4, 0, -7]], [0.5, CROUCH.upper_arm_r], [1.0, CROUCH.upper_arm_r]]) },
                forearm_r: { rotation: k([[0, [-14, 0, 0]], [0.5, CROUCH.forearm_r], [1.0, CROUCH.forearm_r]]) },
                upper_arm_l: { rotation: k([[0, HOLD.upper_arm_l], [0.5, TUCK.upper_arm_l], [1.0, TUCK.upper_arm_l]]) },
                forearm_l: { rotation: k([[0, HOLD.forearm_l], [0.5, TUCK.forearm_l], [1.0, TUCK.forearm_l]]) },
                hand_l: { rotation: k([[0, HOLD.hand_l], [0.5, TUCK.hand_l], [1.0, TUCK.hand_l]]) },
                axe: { rotation: k([[0, HOLD.axe], [0.5, TUCK.axe], [1.0, TUCK.axe]]) },
                thigh_l: { rotation: k([[0, [-6, 0, 0]], [0.5, CROUCH.thigh_l], [1.0, CROUCH.thigh_l]]) },
                shin_l: { rotation: k([[0, [10, 0, 0]], [0.5, CROUCH.shin_l], [1.0, CROUCH.shin_l]]) },
                hoof_l: { rotation: k([[0, [-4, 0, 0]], [0.5, CROUCH.hoof_l], [1.0, CROUCH.hoof_l]]) },
                // 右蹄刨地：抬起向前 → 向后刮，两次
                thigh_r: { rotation: k([[0, [-6, 0, 0]], [0.5, CROUCH.thigh_r], [0.58, [-34, 0, 0]], [0.68, [6, 0, 0]], [0.78, [-34, 0, 0]], [0.88, [6, 0, 0]], [1.0, CROUCH.thigh_r]]) },
                shin_r: { rotation: k([[0, [10, 0, 0]], [0.5, CROUCH.shin_r], [0.58, [60, 0, 0]], [0.68, [24, 0, 0]], [0.78, [60, 0, 0]], [0.88, [24, 0, 0]], [1.0, CROUCH.shin_r]]) },
                hoof_r: { rotation: k([[0, [-4, 0, 0]], [0.5, CROUCH.hoof_r], [0.58, [-30, 0, 0]], [0.68, [-48, 0, 0]], [0.78, [-30, 0, 0]], [0.88, [-48, 0, 0]], [1.0, CROUCH.hoof_r]]) },
                tail_root: { rotation: k([[0, [12, 0, 0]], [0.5, CROUCH.tail_root], [1.0, CROUCH.tail_root]]) },
                tail_tip: { rotation: k([[0, [6, 0, 0]], [0.5, CROUCH.tail_tip], [1.0, CROUCH.tail_tip]]) },
            },
        },
        charge_run: {
            length: 0.5,
            loop: 'loop',
            // 伏身大步跑：前腿伸出着地、后腿蹬地、收腿高抬膝；每 1/8 循环贴地一次
            groundLock: [0, 0.0625, 0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5],
            bones: {
                pelvis: { rotation: k([[0, [22, 0, 0]], [0.125, [19, 0, 0]], [0.25, [22, 0, 0]], [0.375, [19, 0, 0]], [0.5, [22, 0, 0]]]) },
                torso: { rotation: k([[0, [40, 5, 0]], [0.25, [40, -5, 0]], [0.5, [40, 5, 0]]]) },
                neck: { rotation: k([[0, [8, 0, 0]], [0.5, [8, 0, 0]]]) },
                head: { rotation: k([[0, [8, 0, 0]], [0.125, [11, 0, 0]], [0.25, [8, 0, 0]], [0.375, [11, 0, 0]], [0.5, [8, 0, 0]]]) },
                // 右臂与左腿反相摆动（相对躯干；躯干已前倾 62°，-62° 才是世界中的竖直下垂）
                upper_arm_r: { rotation: k([[0, [-95, 0, -12]], [0.25, [-30, 0, -12]], [0.5, [-95, 0, -12]]]) },
                forearm_r: { rotation: k([[0, [-40, 0, 0]], [0.25, [-60, 0, 0]], [0.5, [-40, 0, 0]]]) },
                // 持斧左臂随步伐小幅前后摆（幅度小，斧刃始终与牛角同向）
                upper_arm_l: { rotation: k([[0, [22, 0, 22]], [0.25, [16, 0, 22]], [0.5, [22, 0, 22]]]) },
                forearm_l: { rotation: k([[0, TUCK.forearm_l], [0.5, TUCK.forearm_l]]) },
                hand_l: { rotation: k([[0, TUCK.hand_l], [0.5, TUCK.hand_l]]) },
                axe: { rotation: k([[0, TUCK.axe], [0.5, TUCK.axe]]) },
                // 左腿：0 秒前伸着地 → 0.25 秒后蹬 → 0.375 秒高抬膝收回
                thigh_l: { rotation: k([[0, [-60, 0, 0]], [0.125, [-28, 0, 0]], [0.25, [5, 0, 0]], [0.375, [-40, 0, 0]], [0.5, [-60, 0, 0]]]) },
                shin_l: { rotation: k([[0, [22, 0, 0]], [0.125, [32, 0, 0]], [0.25, [22, 0, 0]], [0.375, [80, 0, 0]], [0.5, [22, 0, 0]]]) },
                hoof_l: { rotation: k([[0, [18, 0, 0]], [0.125, [-21, 0, 0]], [0.25, [-40, 0, 0]], [0.375, [-30, 0, 0]], [0.5, [18, 0, 0]]]) },
                thigh_r: { rotation: k([[0, [5, 0, 0]], [0.125, [-40, 0, 0]], [0.25, [-60, 0, 0]], [0.375, [-28, 0, 0]], [0.5, [5, 0, 0]]]) },
                shin_r: { rotation: k([[0, [22, 0, 0]], [0.125, [80, 0, 0]], [0.25, [22, 0, 0]], [0.375, [32, 0, 0]], [0.5, [22, 0, 0]]]) },
                hoof_r: { rotation: k([[0, [-40, 0, 0]], [0.125, [-30, 0, 0]], [0.25, [18, 0, 0]], [0.375, [-21, 0, 0]], [0.5, [-40, 0, 0]]]) },
                tail_root: { rotation: k([[0, [-55, 0, 6]], [0.25, [-55, 0, -6]], [0.5, [-55, 0, 6]]]) },
                tail_tip: { rotation: k([[0, [-10, 0, 0]], [0.5, [-10, 0, 0]]]) },
            },
        },
        charge_recover: {
            length: 0.8,
            loop: 'once',
            // 从冲刺姿势减速、直起身，回到待机
            groundLock: [0, 0.3, 0.55, 0.8],
            bones: {
                pelvis: { rotation: k([[0, [22, 0, 0]], [0.3, [15, 0, 0]], [0.8, [0, 0, 0]]]) },
                torso: { rotation: k([[0, [40, 0, 0]], [0.3, [26, 0, 0]], [0.8, [6, 0, 0]]]) },
                neck: { rotation: k([[0, [8, 0, 0]], [0.8, [-3, 0, 0]]]) },
                head: { rotation: k([[0, [8, 0, 0]], [0.3, [-10, 0, 0]], [0.8, [-3, 0, 0]]]) },
                upper_arm_r: { rotation: k([[0, [-56, 0, -12]], [0.8, [-4, 0, -7]]]) },
                forearm_r: { rotation: k([[0, [-50, 0, 0]], [0.8, [-14, 0, 0]]]) },
                upper_arm_l: { rotation: k([[0, TUCK.upper_arm_l], [0.8, HOLD.upper_arm_l]]) },
                forearm_l: { rotation: k([[0, TUCK.forearm_l], [0.8, HOLD.forearm_l]]) },
                hand_l: { rotation: k([[0, TUCK.hand_l], [0.8, HOLD.hand_l]]) },
                axe: { rotation: k([[0, TUCK.axe], [0.8, HOLD.axe]]) },
                thigh_l: { rotation: k([[0, [-50, 0, 0]], [0.3, [-34, 0, 0]], [0.8, [-6, 0, 0]]]) },
                shin_l: { rotation: k([[0, [30, 0, 0]], [0.3, [40, 0, 0]], [0.8, [10, 0, 0]]]) },
                hoof_l: { rotation: k([[0, [0, 0, 0]], [0.3, [-20, 0, 0]], [0.8, [-4, 0, 0]]]) },
                thigh_r: { rotation: k([[0, [-10, 0, 0]], [0.3, [-20, 0, 0]], [0.8, [-6, 0, 0]]]) },
                shin_r: { rotation: k([[0, [30, 0, 0]], [0.3, [30, 0, 0]], [0.8, [10, 0, 0]]]) },
                hoof_r: { rotation: k([[0, [-40, 0, 0]], [0.3, [-24, 0, 0]], [0.8, [-4, 0, 0]]]) },
                tail_root: { rotation: k([[0, [-55, 0, 0]], [0.8, [12, 0, 0]]]) },
                tail_tip: { rotation: k([[0, [-10, 0, 0]], [0.8, [6, 0, 0]]]) },
            },
        },
        // ───────── 召唤版（T14 设计文档 §6.1） ─────────
        attack_swing: {
            length: 0.5,
            loop: 'hold',
            // 下劈（用户 2026-10-21：要用斧刃砍）：双刃斧的刃面在斧柄所在的平面里，整个挥砍中斧只绕柄保持 rz = 0，
            // 让刃面竖在挥动平面里；手臂外展由前臂反向抵消。举斧（0.18 秒）斧头在左肩后上方，劈下（0.3 秒）斧头朝正前略向下，
            // 下面那片刃口在前。两帧由 search_chop.js 在网格里搜出（按方块自身朝向检查不碰头、不入地，刃面法向接近左右方向）。
            groundLock: [0, 0.18, 0.24, 0.3, 0.5],
            bones: {
                pelvis: { rotation: k([[0, [0, 0, 0]], [0.18, [-3, 0, 0]], [0.3, [10, 0, 0]], [0.5, [0, 0, 0]]]) },
                torso: { rotation: k([[0, [6, 0, 0]], [0.18, [-8, 8, 0]], [0.3, [24, -4, 0]], [0.5, [6, 0, 0]]]) },
                neck: { rotation: k([[0, [-3, 0, 0]], [0.18, [4, -4, 0]], [0.3, [-12, 2, 0]], [0.5, [-3, 0, 0]]]) },
                head: { rotation: k([[0, [-3, 0, 0]], [0.18, [2, -4, 0]], [0.3, [-10, 2, 0]], [0.5, [-3, 0, 0]]]) },
                upper_arm_l: { rotation: k([[0, HOLD.upper_arm_l], [0.18, [-160, 0, 15]], [0.3, [-80, 0, 10]], [0.5, HOLD.upper_arm_l]]) },
                forearm_l: { rotation: k([[0, HOLD.forearm_l], [0.18, [-20, 0, -15]], [0.3, [-30, 0, -10]], [0.5, HOLD.forearm_l]]) },
                hand_l: { rotation: k([[0, HOLD.hand_l], [0.18, [30, 0, 0]], [0.3, [90, 0, 0]], [0.5, HOLD.hand_l]]) },
                axe: { rotation: k([[0, HOLD.axe], [0.18, [0, 0, 0]], [0.3, [0, 0, 0]], [0.5, HOLD.axe]]) },
                upper_arm_r: { rotation: k([[0, [-4, 0, -7]], [0.18, [-30, 0, -14]], [0.3, [18, 0, -10]], [0.5, [-4, 0, -7]]]) },
                forearm_r: { rotation: k([[0, [-14, 0, 0]], [0.18, [-30, 0, 0]], [0.3, [-10, 0, 0]], [0.5, [-14, 0, 0]]]) },
                thigh_l: { rotation: k([[0, [-6, 0, 0]], [0.3, [-22, 0, 0]], [0.5, [-6, 0, 0]]]) },
                shin_l: { rotation: k([[0, [10, 0, 0]], [0.3, [24, 0, 0]], [0.5, [10, 0, 0]]]) },
                hoof_l: { rotation: k([[0, [-4, 0, 0]], [0.3, [-8, 0, 0]], [0.5, [-4, 0, 0]]]) },
                thigh_r: { rotation: k([[0, [-6, 0, 0]], [0.3, [4, 0, 0]], [0.5, [-6, 0, 0]]]) },
                shin_r: { rotation: k([[0, [10, 0, 0]], [0.3, [12, 0, 0]], [0.5, [10, 0, 0]]]) },
                hoof_r: { rotation: k([[0, [-4, 0, 0]], [0.3, [-12, 0, 0]], [0.5, [-4, 0, 0]]]) },
                tail_root: { rotation: k([[0, [12, 0, 0]], [0.18, [8, 0, 10]], [0.3, [20, 0, -10]], [0.5, [12, 0, 0]]]) },
            },
        },
        toss: {
            length: 0.6,
            loop: 'hold',
            // 从刨地的伏身姿势猛地抬头上挑（0.15 秒）→ 直身昂首（0.3 秒）→ 回到待机（0.6 秒）
            groundLock: [0, 0.15, 0.3, 0.6],
            bones: {
                pelvis: { rotation: k([[0, CROUCH.pelvis], [0.15, [6, 0, 0]], [0.3, [-4, 0, 0]], [0.6, [0, 0, 0]]]) },
                torso: { rotation: k([[0, CROUCH.torso], [0.15, [8, 0, 0]], [0.3, [-8, 0, 0]], [0.6, [6, 0, 0]]]) },
                neck: { rotation: k([[0, CROUCH.neck], [0.15, [-24, 0, 0]], [0.3, [-20, 0, 0]], [0.6, [-3, 0, 0]]]) },
                head: { rotation: k([[0, CROUCH.head], [0.15, [-34, 0, 0]], [0.3, [-26, 0, 0]], [0.6, [-3, 0, 0]]]) },
                jaw: { rotation: k([[0, [0, 0, 0]], [0.15, [10, 0, 0]], [0.3, [6, 0, 0]], [0.6, [0, 0, 0]]]) },
                upper_arm_l: { rotation: k([[0, TUCK.upper_arm_l], [0.3, [6, 0, 30]], [0.6, HOLD.upper_arm_l]]) },
                forearm_l: { rotation: k([[0, TUCK.forearm_l], [0.3, [-60, 0, -30]], [0.6, HOLD.forearm_l]]) },
                hand_l: { rotation: k([[0, TUCK.hand_l], [0.3, [-30, 0, 0]], [0.6, HOLD.hand_l]]) },
                axe: { rotation: k([[0, TUCK.axe], [0.3, [0, 0, -30]], [0.6, HOLD.axe]]) },
                upper_arm_r: { rotation: k([[0, CROUCH.upper_arm_r], [0.15, [-30, 0, -24]], [0.3, [-10, 0, -20]], [0.6, [-4, 0, -7]]]) },
                forearm_r: { rotation: k([[0, CROUCH.forearm_r], [0.3, [-24, 0, 0]], [0.6, [-14, 0, 0]]]) },
                thigh_l: { rotation: k([[0, CROUCH.thigh_l], [0.15, [-28, 0, 0]], [0.3, [-10, 0, 0]], [0.6, [-6, 0, 0]]]) },
                shin_l: { rotation: k([[0, CROUCH.shin_l], [0.15, [30, 0, 0]], [0.3, [12, 0, 0]], [0.6, [10, 0, 0]]]) },
                hoof_l: { rotation: k([[0, CROUCH.hoof_l], [0.15, [-8, 0, 0]], [0.3, [-2, 0, 0]], [0.6, [-4, 0, 0]]]) },
                thigh_r: { rotation: k([[0, CROUCH.thigh_r], [0.15, [6, 0, 0]], [0.3, [-2, 0, 0]], [0.6, [-6, 0, 0]]]) },
                shin_r: { rotation: k([[0, CROUCH.shin_r], [0.15, [14, 0, 0]], [0.3, [10, 0, 0]], [0.6, [10, 0, 0]]]) },
                hoof_r: { rotation: k([[0, CROUCH.hoof_r], [0.15, [-20, 0, 0]], [0.3, [-8, 0, 0]], [0.6, [-4, 0, 0]]]) },
                tail_root: { rotation: k([[0, CROUCH.tail_root], [0.3, [24, 0, 0]], [0.6, [12, 0, 0]]]) },
                tail_tip: { rotation: k([[0, CROUCH.tail_tip], [0.3, [14, 0, 0]], [0.6, [6, 0, 0]]]) },
            },
        },
    };
    // 刨地（顶飞前摇 0.8 秒）：冲锋蓄力动作按 0.8 倍时间压缩（伏身低头、右蹄刨地两次）
    ANIMS.paw = {
        length: 0.8,
        loop: 'hold',
        groundLock: ANIMS.charge_windup.groundLock.map((t) => Math.round(t * 0.8 * 1000) / 1000),
        bones: Object.fromEntries(Object.entries(ANIMS.charge_windup.bones).map(([b, ch]) => [b,
            Object.fromEntries(Object.entries(ch).map(([c, frames]) => [c,
                frames.map(([t, v]) => [Math.round(t * 0.8 * 1000) / 1000, v])]))])),
    };
    MA.MINOTAUR_ANIMS = ANIMS;

    /** 在当前工程中（重新）生成全部动画。 */
    /** 握持约束：斧相对手只能绕斧柄旋转（卡片 rx、ry 必须为 0）。 */
    function checkGrip(name, def) {
        const axe = def.bones.axe;
        if (!axe || !axe.rotation) return;
        for (const [t, v] of axe.rotation) {
            if (Math.abs(v[0]) > 1e-6 || Math.abs(v[1]) > 1e-6) {
                throw new Error('动画 ' + name + ' 在 ' + t + ' 秒让斧离开握孔方向：' + JSON.stringify(v) + '（只能转 rz，姿势请改手腕或前臂）');
            }
        }
    }

    MA.buildMinotaurAnimations = () => {
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
     * 贴地：在 groundLock 列出的时刻定格动画，测量蹄子（含悬蹄）最低点，把骨盆位移的 y 修正到蹄底恰好着地。
     * 骨盆以下的一切都随骨盆平移，所以每个时刻只需一次修正；中间帧由线性插值衔接。
     */
    function groundLock(anim, def) {
        const pelvis = MA.bones.pelvis || Group.all.find((g) => g.name === 'pelvis');
        const animator = anim.getBoneAnimator(pelvis);
        const feet = Cube.all.filter((c) => /^(hoof|dewclaw)/.test(c.name));
        for (const t of def.groundLock) {
            let kf = animator.position.find((f) => Math.abs(f.time - t) < 1e-4);
            if (!kf) {
                kf = animator.addKeyframe({ channel: 'position', time: t, interpolation: 'linear', data_points: [{ x: 0, y: 0, z: 0 }] });
            }
        }
        for (const t of def.groundLock) {
            anim.select();
            Timeline.setTime(t);
            Animator.preview();
            let minY = Infinity;
            for (const c of feet) {
                c.mesh.updateMatrixWorld(true);
                minY = Math.min(minY, new THREE.Box3().setFromObject(c.mesh).min.y);
            }
            const kf = animator.position.find((f) => Math.abs(f.time - t) < 1e-4);
            const y = parseFloat(kf.data_points[0].y) || 0;
            kf.data_points[0].y = Math.round((y - minY) * 100) / 100;
        }
        Animator.preview();
    }

    /**
     * 互穿检查（卡片用例 MIN-08）：在每个贴地时刻检查斧与头部（颅、鼻吻、角、耳、颊）的包围盒不相交，
     * 且没有任何方块低于地面 0.05u。不通过即中止构建。
     */
    function checkClearance(anim, def) {
        const axe = Cube.all.filter((c) => /^axe/.test(c.name));
        const head = Cube.all.filter((c) => /^(cranium|muzzle|nose|horn|brow|ear|jowl)/.test(c.name));
        for (const t of def.groundLock) {
            anim.select();
            Timeline.setTime(t);
            Animator.preview();
            // 按方块自身朝向判断（lib/clearance.js），不用轴对齐包围盒——斜着的长斧柄的包围盒会误判
            const r = MA.preciseHit(axe, head);
            if (r.hit) {
                throw new Error('动画 ' + anim.name + ' 在 ' + t + ' 秒：' + r.hit);
            }
            for (const c of Cube.all) {
                if (c.visibility && new THREE.Box3().setFromObject(c.mesh).min.y < -0.05) {
                    throw new Error('动画 ' + anim.name + ' 在 ' + t + ' 秒：' + c.name + ' 低于地面');
                }
            }
        }
    }

    /** 导出 GeckoLib 读取的 Bedrock 动画文件，返回文本。 */
    MA.minotaurAnimations = (rel) => {
        MA.buildMinotaurAnimations();
        Modes.options.animate.select();
        for (const [name, def] of Object.entries(ANIMS)) {
            if (def.groundLock) {
                const anim = Animation.all.find((a) => a.name === name);
                groundLock(anim, def);
                checkClearance(anim, def);
            }
        }
        const text = JSON.stringify(Animator.buildFile(), null, '\t');
        MA.writeText(rel, text);
        return text;
    };

    /** 预览：切到动画模式，定格在某动画的某一时刻（秒）。 */
    MA.pose = (name, time) => {
        Modes.options.animate.select();
        const anim = Animation.all.find((a) => a.name === name);
        anim.select();
        Timeline.setTime(time);
        Animator.preview();
        return name + '@' + time;
    };
    return 'minotaur animations defined';
})();
