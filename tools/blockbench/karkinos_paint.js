// 卡尔基诺斯贴图细节（在按面明暗的底色之上绘制）。由 build_karkinos.js 调用，返回 {paint}。
// v1（2026-10-22，制作卡 09 §4）：
//   甲——橄榄青铜绿，深色不规则斑（低频噪声阈值），甲顶零星颗粒亮点，侧面最上一行甲缘亮线；
//   螯——同甲色，掌外侧颗粒；指尖深褐；步足关节橙褐；腹面淡黄，胸板分节线；
//   眼——黑亮，只在正面左上 1 像素高光，不加眼框；口器深褐，竖纹。
(function () {
    const MA = window.MA;
    const EDGE = [119, 133, 74];
    const SPOT = [52, 62, 34];
    const GLINT = [150, 162, 104];

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
        '#556236': 'shell', '#3B4526': 'shell_dark', '#77854A': 'shell', '#C9B27A': 'belly', '#B09A62': 'belly',
        '#8A6A32': 'joint', '#2A221C': 'tip', '#111214': 'eye', '#7A5A34': 'mouth',
    };

    function paint(ctx, nets) {
        const { local, recolor } = helpers(ctx, nets);
        let k = 0;
        for (const net of Object.values(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k * 11;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'shell' || mat === 'shell_dark') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, 0.94 + 0.12 * vnoise(X, Y, 2, seed));
                        if (vnoise(X, Y, 5, 7) > 0.66) c = mix(c, SPOT, 0.55);
                        if (face === 'top' && hash(X, Y, seed) > 0.95) c = mix(c, GLINT, 0.7);
                        if (vertical && v === 0 && h >= 4) c = mix(c, EDGE, 0.6);
                        if (face === 'bottom') c = scale(c, 0.85);
                        return c;
                    });
                } else if (mat === 'belly') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = 0.95 + 0.08 * hash(X, Y, seed);
                        if (face === 'bottom' && (v % 4 === 3)) f *= 0.82;
                        return scale(rgb, f);
                    });
                } else if (mat === 'joint') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.92 + 0.14 * hash(X, Y, seed)));
                } else if (mat === 'tip') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.9 + 0.25 * hash(X, Y, seed)));
                } else if (mat === 'mouth') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, u % 2 ? 0.86 : 1.04));
                }
            }
        }
        // ── 眼：黑亮，前上一像素高光 ──
        {
            const E = local('eye', 'front');
            if (E.r) E.px(0, 0, '#d8dde0');
        }
        // ── 掌外侧颗粒 ──
        for (const g of ['palm', 'palm_side']) {
            for (const f of ['left', 'top']) {
                const P = local(g, f);
                if (!P.r) continue;
                for (let v = 1; v < P.r[3] - 1; v += 2) {
                    for (let u = (v % 4 === 1 ? 1 : 2); u < P.r[2] - 1; u += 3) P.px(u, v, MA.shade('#77854a', 1.15));
                }
            }
        }
    }

    return { paint };
})();
