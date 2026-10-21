// 精确的穿插检查（T14，2026-10-21）：轴对齐包围盒对斜着的长斧柄过于保守（斧从头侧挥过也会判成穿进头里），
// 改为在斧方块的角点、棱上取样，变换到头部每个方块自己的坐标系里判断是否落在方块内。
(function () {
    const MA = window.MA;

    function samples(c) {
        const g = c.mesh.geometry;
        g.computeBoundingBox();
        const b = g.boundingBox;
        const pts = [];
        for (const fx of [0, 0.5, 1]) {
            for (const fy of [0, 0.5, 1]) {
                for (const fz of [0, 0.25, 0.5, 0.75, 1]) {
                    pts.push(new THREE.Vector3(b.min.x + (b.max.x - b.min.x) * fx, b.min.y + (b.max.y - b.min.y) * fy,
                        b.min.z + (b.max.z - b.min.z) * fz).applyMatrix4(c.mesh.matrixWorld));
                }
            }
        }
        return pts;
    }

    function inside(c, p) {
        const g = c.mesh.geometry;
        g.computeBoundingBox();
        const inv = new THREE.Matrix4().copy(c.mesh.matrixWorld).invert();
        return g.boundingBox.containsPoint(p.clone().applyMatrix4(inv));
    }

    /** 当前预览姿势下：a 组方块是否有取样点落进 b 组方块；同时返回 a 组最低点。 */
    MA.preciseHit = (aCubes, bCubes) => {
        for (const c of aCubes.concat(bCubes)) {
            c.mesh.updateMatrixWorld(true);
        }
        let hit = null;
        let minY = Infinity;
        for (const a of aCubes) {
            for (const p of samples(a)) {
                minY = Math.min(minY, p.y);
                if (!hit) {
                    for (const b of bCubes) {
                        if (inside(b, p)) {
                            hit = a.name + ' → ' + b.name;
                            break;
                        }
                    }
                }
            }
        }
        return { hit, minY };
    };

    /** 骨骼在世界里的局部轴（单位向量）：{x, y, z}。 */
    MA.boneAxes = (group) => {
        group.mesh.updateMatrixWorld(true);
        const e = group.mesh.matrixWorld.elements;
        const v = (i) => new THREE.Vector3(e[i], e[i + 1], e[i + 2]).normalize();
        return { x: v(0), y: v(4), z: v(8) };
    };
    return 'clearance loaded';
})();
