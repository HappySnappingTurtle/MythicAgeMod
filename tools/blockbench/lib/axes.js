// 设计卡坐标 → Blockbench 编辑器坐标的唯一换算（T7 §3，2026-10-14 实机校准）。
//
// 校准结果（art/blockbench/dev/calibration.bbmodel，实体偏航 0° 面朝 +Z）：
//   - Blockbench 的 north 面（-Z）出现在实体正前方，字母不镜像、UV 原点在左上；
//   - Blockbench +X 一侧的细杆出现在实体右手边；
//   - Bedrock 导出器把 Blockbench 的 X 取反（Blockbench +10 → 文件中 -10）。
// 设计卡：+X 为角色左、+Y 为上、+Z 为角色前。所以卡片 → Blockbench 是绕 Y 轴转 180°：
//   位置 (x, y, z) → (-x, y, -z)；欧拉角 (rx, ry, rz) → (-rx, ry, -rz)（每个轴单独共轭，任何旋转顺序都成立）。
// 卡片坐标与 Bedrock 文件坐标的关系因此是 (x, y, z) → (x, y, -z)。
(function () {
    const MA = window.MA;
    MA.axes = {
        pos: (p) => [-p[0], p[1], -p[2]],
        rot: (r) => [-r[0], r[1], -r[2]],
        /** 卡片中心 + 尺寸 → Blockbench from/to。 */
        box: (center, size) => {
            const c = MA.axes.pos(center);
            return {
                from: [c[0] - size[0] / 2, c[1] - size[1] / 2, c[2] - size[2] / 2],
                to: [c[0] + size[0] / 2, c[1] + size[1] / 2, c[2] + size[2] / 2],
            };
        },
        /**
         * 卡片的解剖学面名 → Blockbench 面名。卡片前（+Z）= north，后 = south，
         * 左（+X，角色左）在 Blockbench 中位于 -X = west，右 = east。
         */
        face: { front: 'north', back: 'south', left: 'west', right: 'east', top: 'up', bottom: 'down' },
    };
    return 'axes loaded';
})();
