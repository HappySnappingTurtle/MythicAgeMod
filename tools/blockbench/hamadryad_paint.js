// 树仙女贴图细节（在按面明暗的底色之上绘制）。由 build_hamadryad.js 调用，返回 {paint}。
// v1（2026-10-22，制作卡 10 §4）：
//   少女——皮肤浅橄榄，小臂与小腿外侧（左右两面）几道竖向树皮纹；发深栗竖丝，发辫与后发里点缀橡叶绿；
//   希顿亚麻竖褶，下摆一道深边；橡叶冠是叶脉纹，橡子上半截是壳斗；
//   脸：深绿 2×2 眼（1 像素高光）、细眉、浅玫色小嘴，不加眼框，左右按像素镜像；
//   化树——深橡树皮竖向沟纹（与本命橡木方块一致），树干中段正面一张只用阴影的脸（两道眼窝、一道嘴）；
//   叶团——橡叶色块深浅相间，约 1/5 像素镂空（贴图透明，GeckoLib 默认 cutout 渲染）。
(function () {
    const MA = window.MA;
    const EYE = '#2f4a22';
    const BROW = '#4a3a24';
    const LIP = '#b8786a';
    const LEAF_PX = '#6d8a3a';

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

    const MATERIAL = {
        '#D4B896': 'skin', '#4A3A24': 'hair', '#5D7A32': 'leaf', '#8A6A32': 'acorn', '#D8CFB4': 'linen', '#C4B998': 'linen',
        '#5D6B3A': 'girdle', '#3F2F1E': 'bark', '#2E2216': 'bark', '#4F6E2A': 'canopy', '#3E5A22': 'canopy',
    };
    const BARK_STREAK = new Set(['forearm', 'shin']);

    function paint(ctx, nets) {
        const { local, recolor } = helpers(ctx, nets);
        let k = 0;
        for (const [gname, net] of Object.entries(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k * 13;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'skin') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, 0.97 + 0.05 * vnoise(X, Y, 3, seed));
                        // 小臂、小腿外侧的树皮纹：每 2 列一道深竖纹，断断续续
                        if (BARK_STREAK.has(gname) && (face === 'left' || face === 'right') && u % 2 === 0 && vnoise(u, v, 2, seed) > 0.45) {
                            c = mix(c, [138, 106, 74], 0.55);
                        }
                        return c;
                    });
                } else if (mat === 'hair') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, (u % 2 ? 0.84 : 1.06) * (0.92 + 0.14 * vnoise(u, v, 4, seed)));
                        if ((gname === 'braid' || gname === 'hair_back') && hash(X, Y, seed) > 0.86) c = [109, 138, 58];
                        return c;
                    });
                } else if (mat === 'leaf') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, 0.9 + 0.2 * hash(X, Y, seed));
                        if ((u + v) % 3 === 0) c = scale(c, 0.8);
                        return c;
                    });
                } else if (mat === 'acorn') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => (v === 0 ? [92, 74, 40] : scale(rgb, 1.0 + 0.1 * hash(X, Y, seed))));
                } else if (mat === 'linen') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        if (!vertical) return scale(rgb, 0.97 + 0.04 * hash(X, Y, seed));
                        const m = u % 3;
                        let c = scale(rgb, (m === 0 ? 0.86 : m === 1 ? 1.05 : 1.0) * (0.98 + 0.04 * hash(X, Y, seed)));
                        if (gname === 'hem' && v === h - 1) c = scale(c, 0.8);
                        return c;
                    });
                } else if (mat === 'girdle') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, u % 3 === 0 ? 0.8 : 1.0));
                } else if (mat === 'bark') {
                    // 竖向沟纹：每 3 列一道深沟，沟纹沿高度缓慢错位
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const shift = Math.floor(vnoise(u, v, 6, seed) * 2);
                        const m = (u + shift) % 3;
                        let c = scale(rgb, m === 0 ? 0.68 : m === 1 ? 1.1 : 0.95);
                        if (!vertical) c = scale(rgb, 0.9 + 0.15 * vnoise(X, Y, 2, seed));
                        return scale(c, 0.94 + 0.1 * hash(X, Y, seed));
                    });
                } else if (mat === 'canopy') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const n = vnoise(X, Y, 2, seed);
                        return scale(rgb, n > 0.6 ? 1.18 : n < 0.3 ? 0.78 : 1.0);
                    });
                    // 镂空：约 1/5 的像素透明（不挖最外一圈，叶团轮廓保持完整）
                    for (let v = 1; v < r[3] - 1; v++) {
                        for (let u = 1; u < r[2] - 1; u++) {
                            if (hash(r[0] + u, r[1] + v, 77) > 0.8) ctx.clearRect(r[0] + u, r[1] + v, 1, 1);
                        }
                    }
                }
            }
        }

        // ── 少女的脸（head 前面）：眉、眼、小嘴；左右镜像 ──
        {
            const H = local('head', 'front');
            if (H.r) {
                const w = H.r[2];
                const at = (u, v, c) => {
                    H.px(u, v, c);
                    H.px(w - 1 - u, v, c);
                };
                at(2, 3, BROW);
                at(3, 3, BROW);
                at(2, 4, EYE);
                at(3, 4, EYE);
                at(2, 5, EYE);
                at(3, 5, EYE);
                at(3, 4, '#dfe8d0');
                at(2, 7, MA.shade('#d4b896', 0.93));
                H.px(Math.floor(w / 2) - 1, 7, LIP, 2, 1);
            }
            const N = local('nose', 'front');
            if (N.r) N.px(0, N.r[3] - 1, MA.shade('#d4b896', 0.8), N.r[2], 1);
        }
        // ── 化树树皮上的脸（trunk_mid 正面）：两道眼窝凹陷、一道嘴，只用阴影 ──
        {
            const T = local('trunk_mid', 'front');
            if (T.r) {
                const w = T.r[2];
                const cx = Math.floor(w / 2);
                const shade = '#1e160d';
                const at = (u, v, c, ww = 1, hh = 1) => {
                    T.px(cx - 1 - u - (ww - 1), v, c, ww, hh);
                    T.px(cx + u, v, c, ww, hh);
                };
                at(2, 9, shade, 3, 1);
                at(2, 10, shade, 3, 2);
                at(3, 12, MA.shade('#3f2f1e', 0.8), 2, 1);
                T.px(cx - 1, 11, MA.shade('#3f2f1e', 1.25), 2, 4);
                T.px(cx - 3, 18, shade, 6, 1);
                T.px(cx - 2, 19, shade, 4, 1);
            }
        }
    }

    return { paint };
})();
