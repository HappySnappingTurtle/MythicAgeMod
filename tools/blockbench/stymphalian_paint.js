// 斯廷法利斯怪鸟贴图细节（在按面明暗的底色之上绘制）。由 build_stymphalian.js 调用，返回 {paint}。
// v1（2026-10-21，制作卡 07 §4）：
//   身体羽——暗青铜褐，鳞状羽缘（每片 4×3，下沿一道深弧，两排错开）；
//   覆羽——青铜圆鳞，上沿受光；飞羽与尾羽——亮青铜金属片：羽轴一道深线、一侧前缘亮边、零星划痕高光；
//   喙——深青铜锤纹，尖端两像素磨亮；腿——深灰褐，每 2 像素一道横向鳞纹；爪——青铜；
//   眼——琥珀 2×2、1 像素黑瞳、1 像素高光，左右按像素镜像，不加眼框。
(function () {
    const MA = window.MA;
    const EYE = '#e0a030';
    const PUPIL = '#120c08';
    const EYE_HI = '#fff2c8';

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

    // 材质按底色识别（与 params/stymphalian_parts.py 的色板一致）
    const MATERIAL = {
        '#4A3A2C': 'plume', '#3E3125': 'plume', '#5A4733': 'plume', '#7A5A30': 'covert', '#86643A': 'covert',
        '#9C6A2A': 'metal', '#5C4A32': 'beak', '#3D342C': 'leg', '#A07A40': 'talon',
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
    const BRIGHT = [255, 226, 150];

    /** 眼睛：琥珀 2×2，前下一像素黑瞳，前上一像素高光；u 从前缘量起，右侧按像素镜像。 */
    function eye(L, mirror) {
        if (!L.r) return;
        const w = L.r[2];
        const at = (u, v, c) => L.px(mirror ? w - 1 - u : u, v, c);
        const u0 = 1;
        const v0 = 1;
        at(u0, v0, EYE);
        at(u0 + 1, v0, EYE);
        at(u0, v0 + 1, PUPIL);
        at(u0 + 1, v0 + 1, EYE);
        at(u0, v0, EYE_HI);
    }

    function paint(ctx, nets) {
        const { local, recolor } = helpers(ctx, nets);
        let k = 0;
        for (const net of Object.values(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k * 7;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'plume') {
                    // 鳞状羽缘：4×3 一片，最下一行是深弧，两端更深；相邻两排错开半片
                    const TILE = [[1.04, 1.06, 1.06, 1.04], [1.0, 1.02, 1.02, 1.0], [0.8, 0.9, 0.9, 0.8]];
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const row = Math.floor(v / 3);
                        const uu = (u + (row % 2 ? 2 : 0)) % 4;
                        return scale(rgb, TILE[v % 3][uu] * (0.96 + 0.08 * vnoise(X, Y, 3, seed)));
                    });
                } else if (mat === 'covert') {
                    const TILE = [[1.1, 1.14, 1.14, 1.1], [0.98, 1.04, 1.04, 0.98], [0.86, 0.8, 0.8, 0.86]];
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const row = Math.floor(v / 3);
                        const uu = (u + (row % 2 ? 2 : 0)) % 4;
                        return scale(rgb, TILE[v % 3][uu] * (0.97 + 0.06 * hash(X, Y, seed)));
                    });
                } else if (mat === 'metal') {
                    // 金属羽片：横跨羽宽从前缘亮到后缘暗（打磨的弧面反光），前缘一像素高光、羽轴一道深线，
                    // 沿羽长有缓慢的明暗起伏，零星亮点；看起来是一片片青铜而不是木板
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const along = w >= h;
                        const across = along ? v : u;
                        const n = along ? h : w;
                        const pos = along ? u : v;
                        const mid = (n - 1) / 2;
                        const t = n > 1 ? across / (n - 1) : 0.5;
                        let c = scale(rgb, 1.25 - 0.5 * t);
                        c = scale(c, 0.9 + 0.2 * vnoise(pos, k, 3, seed));
                        if (n >= 3 && Math.abs(across - mid) < 0.6) c = scale(c, 0.7);
                        if (across === 0) c = mix(c, BRIGHT, 0.7);
                        if (hash(X, Y, seed + 5) > 0.96) c = mix(c, BRIGHT, 0.6);
                        return c;
                    });
                } else if (mat === 'beak') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.88 + 0.24 * vnoise(X, Y, 2, seed)));
                } else if (mat === 'leg') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = 0.95 + 0.08 * hash(X, Y, seed);
                        if (vertical && h > 4 && v % 2 === 1) f *= 0.84; // 跗跖横鳞
                        return scale(rgb, f);
                    });
                } else if (mat === 'talon') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.92 + 0.2 * hash(X, Y, seed)));
                }
            }
        }

        // ── 喙尖磨亮：beak_tip 各侧面最前两列 ──
        for (const f of ['left', 'right', 'top', 'bottom']) {
            const L = local('beak_tip', f);
            if (!L.r) continue;
            const w = L.r[2];
            const h = L.r[3];
            for (let v = 0; v < h; v++) {
                if (f === 'left') L.px(0, v, '#c9a061', 2, 1);
                else if (f === 'right') L.px(w - 2, v, '#c9a061', 2, 1);
            }
            if (f === 'top' || f === 'bottom') L.px(0, 0, '#c9a061', w, 2);
        }
        {
            const F = local('beak_tip', 'front');
            if (F.r) F.px(0, 0, '#d8b070', F.r[2], F.r[3]);
        }
        // ── 喙缝：beak_base 左右两面靠下一道深线 ──
        for (const f of ['left', 'right']) {
            const L = local('beak_base', f);
            if (L.r) L.px(0, L.r[3] - 1, '#2e2418', L.r[2], 1);
        }
        // ── 眼睛 ──
        eye(local('cranium', 'left'), false);
        eye(local('cranium', 'right'), true);
    }

    return { paint };
})();
