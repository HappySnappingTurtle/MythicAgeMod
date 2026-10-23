// 哈耳庇厄贴图细节（在按面明暗的底色之上绘制）。由 build_harpy.js 调用，返回 {paint}。
// v1（2026-10-22，制作卡 08 §4）：
//   皮肤——灰白，脸上颧下阴影；眼——黄 3 像素宽、中间 1 像素黑瞳，眼下一道凹陷阴影，斜挑黑眉，不加眼框；
//   头发——蓝黑竖向发丝；希顿——褪红，竖褶，下摆一圈奶白回纹；腰带深褐；
//   羽——身体与覆羽石板灰鳞状羽缘；飞羽炭黑，羽轴一道浅线、沿羽长每 4 像素一道浅灰横斑、羽尖 2 像素浅灰；
//   腿——黄灰，跗跖横鳞；爪——黑，尖端一像素亮；口腔暗红。
(function () {
    const MA = window.MA;
    const EYE = '#e8c030';
    const PUPIL = '#120c08';
    const BROW = '#1a1b21';
    const CREAM = '#d8c8a0';

    const hash = (x, y, k) => {
        let h = (x * 374761393 + y * 668265263 + k * 2147483647) | 0;
        h = Math.imul(h ^ (h >>> 13), 1274126177);
        return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
    };
    const vnoise = (x, y, scale, k) => {
        const gx = x / scale;
        const gy = y / scale;
        const x0 = Math.floor(gx);
        const y0 = Math.floor(gy);
        const fx = gx - x0;
        const fy = gy - y0;
        const sx = fx * fx * (3 - 2 * fx);
        const sy = fy * fy * (3 - 2 * fy);
        const a = hash(x0, y0, k) * (1 - sx) + hash(x0 + 1, y0, k) * sx;
        const b = hash(x0, y0 + 1, k) * (1 - sx) + hash(x0 + 1, y0 + 1, k) * sx;
        return a * (1 - sy) + b * sy;
    };

    function helpers(ctx, nets) {
        const local = (group, face) => {
            const r = nets[group] && nets[group].faces[face];
            return {
                r,
                px: (u, v, color, w = 1, h = 1) => {
                    if (!r || u < 0 || v < 0 || u >= r[2] || v >= r[3]) return;
                    ctx.fillStyle = color;
                    ctx.fillRect(r[0] + u, r[1] + v, Math.min(w, r[2] - u), Math.min(h, r[3] - v));
                },
            };
        };
        const recolor = (r, fn) => {
            const img = ctx.getImageData(r[0], r[1], r[2], r[3]);
            for (let v = 0; v < r[3]; v++) {
                for (let u = 0; u < r[2]; u++) {
                    const i = (v * r[2] + u) * 4;
                    const out = fn(u, v, r[2], r[3], r[0] + u, r[1] + v, [img.data[i], img.data[i + 1], img.data[i + 2]]);
                    for (let c = 0; c < 3; c++) img.data[i + c] = Math.max(0, Math.min(255, Math.round(out[c])));
                }
            }
            ctx.putImageData(img, r[0], r[1]);
        };
        return { local, recolor };
    }

    const scale = (rgb, f) => rgb.map((c) => c * f);
    const mix = (rgb, to, t) => rgb.map((c, i) => c + (to[i] - c) * t);
    const PALE = [158, 164, 172];

    // 材质按底色识别（与 params/harpy_parts.py 的色板一致）
    const MATERIAL = {
        '#C8BCB0': 'skin', '#AD9F93': 'skin', '#23242C': 'hair', '#7A2E2A': 'cloth', '#64241F': 'cloth',
        '#3A2A20': 'belt', '#4A1A1C': 'mouth', '#4C515B': 'plume', '#3C4049': 'plume', '#5B616C': 'covert',
        '#2C2E35': 'flight', '#A69A6A': 'leg', '#1C1A18': 'talon',
    };

    function paint(ctx, nets) {
        const { local, recolor } = helpers(ctx, nets);
        let k = 0;
        for (const net of Object.values(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k * 7;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'skin') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.97 + 0.05 * vnoise(X, Y, 3, seed)));
                } else if (mat === 'hair') {
                    // 竖向发丝：每 2 列一深一浅，沿长度缓慢起伏，偶有亮丝
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = u % 2 ? 0.82 : 1.06;
                        f *= 0.92 + 0.16 * vnoise(u, v, 4, seed);
                        if (hash(u, 0, seed) > 0.85) f *= 1.25;
                        return scale(rgb, f);
                    });
                } else if (mat === 'cloth') {
                    // 竖褶：每 3 列一道暗褶，褶旁一列亮
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        if (!vertical) return scale(rgb, 0.96 + 0.06 * hash(X, Y, seed));
                        const m = u % 3;
                        return scale(rgb, (m === 0 ? 0.8 : m === 1 ? 1.08 : 1.0) * (0.97 + 0.05 * hash(X, Y, seed)));
                    });
                } else if (mat === 'belt') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.9 + 0.18 * hash(X, Y, seed)));
                } else if (mat === 'plume') {
                    const TILE = [[1.06, 1.08, 1.08, 1.06], [1.0, 1.02, 1.02, 1.0], [0.8, 0.88, 0.88, 0.8]];
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const row = Math.floor(v / 3);
                        const uu = (u + (row % 2 ? 2 : 0)) % 4;
                        return scale(rgb, TILE[v % 3][uu] * (0.96 + 0.08 * vnoise(X, Y, 3, seed)));
                    });
                } else if (mat === 'covert') {
                    const TILE = [[1.12, 1.16, 1.16, 1.12], [0.98, 1.04, 1.04, 0.98], [0.84, 0.78, 0.78, 0.84]];
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const row = Math.floor(v / 3);
                        const uu = (u + (row % 2 ? 2 : 0)) % 4;
                        return scale(rgb, TILE[v % 3][uu] * (0.97 + 0.06 * hash(X, Y, seed)));
                    });
                } else if (mat === 'flight') {
                    // 飞羽：羽轴浅线、横斑、羽尖浅灰。羽片沿长边延伸；长边远端（v 大 / u 大）当作羽尖
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const along = w >= h;
                        const across = along ? v : u;
                        const n = along ? h : w;
                        const pos = along ? u : v;
                        const len = along ? w : h;
                        let c = scale(rgb, 1.1 - 0.2 * (n > 1 ? across / (n - 1) : 0.5));
                        if (n >= 3 && across === Math.floor((n - 1) / 2)) c = mix(c, PALE, 0.35);
                        if (len >= 6 && pos % 4 === 2) c = mix(c, PALE, 0.45);
                        if (len >= 6 && pos >= len - 2) c = mix(c, PALE, 0.7);
                        return scale(c, 0.95 + 0.1 * hash(X, Y, seed));
                    });
                } else if (mat === 'leg') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = 0.95 + 0.08 * hash(X, Y, seed);
                        if (vertical && h > 3 && v % 2 === 1) f *= 0.82;
                        return scale(rgb, f);
                    });
                } else if (mat === 'talon') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, v === h - 1 ? 2.4 : 0.9 + 0.2 * hash(X, Y, seed)));
                }
            }
        }

        // ── 希顿下摆回纹（chiton_hem 四个侧面最下 3 行）──
        for (const f of ['front', 'back', 'left', 'right']) {
            const L = local('chiton_hem', f);
            if (!L.r) continue;
            const w = L.r[2];
            const h = L.r[3];
            L.px(0, h - 3, CREAM, w, 1);
            for (let u = 0; u < w; u++) {
                if (u % 4 === 0 || u % 4 === 1) L.px(u, h - 2, CREAM);
            }
            L.px(0, h - 1, MA.shade(CREAM, 0.85), w, 1);
        }

        // ── 脸（head 前面）：眉、眼、眼下凹陷、颧下阴影；左右按像素镜像 ──
        {
            const H = local('head', 'front');
            if (H.r) {
                const w = H.r[2];
                const at = (u, v, c) => {
                    H.px(u, v, c);
                    H.px(w - 1 - u, v, c);
                };
                const shadow = MA.shade('#c8bcb0', 0.8);
                // 斜挑怒眉：外端高一行
                at(1, 2, BROW);
                at(2, 3, BROW);
                at(3, 3, BROW);
                // 眼：3 像素宽，中间黑瞳
                at(1, 4, EYE);
                at(2, 4, PUPIL);
                at(3, 4, EYE);
                // 眼下凹陷（饥饿的脸）
                at(1, 5, shadow);
                at(2, 5, shadow);
                at(3, 5, shadow);
                // 颧下阴影
                at(1, 7, shadow);
                at(0, 6, shadow);
            }
            const N = local('nose', 'front');
            if (N.r) N.px(0, N.r[3] - 1, MA.shade('#c8bcb0', 0.72), N.r[2], 1);
            // 下颌前面：顶行正中 4 像素的嘴（深），下面一行正中 2 像素下唇阴影；其余是下巴
            const J = local('jaw', 'front');
            if (J.r) {
                const m = Math.floor(J.r[2] / 2);
                J.px(m - 2, 0, '#3a1416', 4, 1);
                J.px(m - 1, 1, MA.shade('#c8bcb0', 0.86), 2, 1);
            }
        }
        // ── 中分：刘海前面与发顶正中 2 像素露出头皮 ──
        {
            const F = local('fringe', 'front');
            if (F.r) F.px(Math.floor(F.r[2] / 2) - 1, 0, MA.shade('#c8bcb0', 0.9), 2, F.r[3]);
            const T = local('hair_cap', 'top');
            if (T.r) T.px(Math.floor(T.r[2] / 2) - 1, 0, MA.shade('#c8bcb0', 0.85), 2, Math.ceil(T.r[3] * 0.6));
        }
        // ── 锁骨：领口前面两道浅影 ──
        {
            const C = local('collar', 'front');
            if (C.r) {
                const w = C.r[2];
                C.px(1, 1, MA.shade('#c8bcb0', 0.82), Math.floor(w / 2) - 2, 1);
                C.px(Math.ceil(w / 2) + 1, 1, MA.shade('#c8bcb0', 0.82), Math.floor(w / 2) - 2, 1);
            }
        }
    }

    return { paint };
})();
