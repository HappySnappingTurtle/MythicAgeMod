// 半人马贴图细节（在按面明暗的底色之上绘制）。由 build_centaur.js 调用，返回 {paint}。
// v1（2026-10-21）：
//   皮肤——成团的暖色明暗，腹部画中线与三道腹肌横线、胸肌下沿阴影、前臂一两道青筋；
//   脸——帕特农柱间壁的怒容：斜眉压眼、眉间竖纹、深褐眼珠 + 高光（按像素镜像、不加眼框）、鼻翼阴影（嘴被胡须盖住）；
//   头发与胡须——竖向发缕、发梢亮；马身——栗色成团明暗，腹下偏暗；黑四肢与鬃尾——发丝竖纹；蹄——角质横纹；
//   豹皮——赭黄底、深褐斑点（2×2 去掉一个角的实心团，抖动网格错开；环形斑在小尺寸下像文字，已弃用），腹面浅色；弓——木弓臂纵纹、角质弓梢、弓把斜缠皮；箭囊——皮革明暗与两道压线；松树干——纵向树皮裂纹、根团带土、针叶成簇。
(function () {
    const MA = window.MA;

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

    // 材质按底色识别（与 params/centaur_parts.py 的色板一致）
    const MATERIAL = {
        '#8A4F2A': 'coat', '#6E3D20': 'coat', '#2C211B': 'hair', '#3A3634': 'hoof', '#C08A63': 'skin', '#A8734F': 'skin',
        '#3B2A1E': 'hair', '#C99A4A': 'pelt', '#E2C48A': 'pelt_pale', '#5A3B24': 'bark', '#4A3020': 'wood', '#7A5232': 'wrap',
        '#5A3A22': 'leather', '#8A6A44': 'wood', '#D2BF96': 'horn',
        '#6B5137': 'root', '#2F5A2E': 'needle',
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
    const SPOT = [70, 45, 25];
    const PALE = [226, 196, 138];

    function paint(ctx, nets) {
        const { local, recolor } = helpers(ctx, nets);
        let k = 0;
        for (const net of Object.values(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k * 7;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'coat') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, 0.92 + 0.14 * vnoise(X, Y, 4, seed));
                        if (face === 'bottom') c = scale(c, 0.8);
                        return c;
                    });
                } else if (mat === 'skin') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.95 + 0.09 * vnoise(X, Y, 3, seed)));
                } else if (mat === 'hair') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const strand = 0.82 + 0.3 * hash(X, 0, seed);
                        const tip = vertical && v === h - 1 ? 1.15 : 1;
                        return scale(rgb, strand * tip);
                    });
                } else if (mat === 'hoof') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, (v % 2 ? 0.9 : 1.05) * (0.95 + 0.1 * hash(X, Y, seed))));
                } else if (mat === 'pelt' || mat === 'pelt_pale') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, 0.94 + 0.1 * vnoise(X, Y, 3, seed));
                        if (face === 'bottom') return mix(c, PALE, 0.6);
                        if (mat === 'pelt_pale') return c;
                        // 斑点：抖动网格（每格 4×4，中心随机偏移），每个斑是 2×2 去掉一个角的实心团，小尺寸下比环形斑好认
                        const gx = Math.floor(X / 4);
                        const gy = Math.floor(Y / 4);
                        for (let dy = -1; dy <= 1; dy++) {
                            for (let dx = -1; dx <= 1; dx++) {
                                const cx = (gx + dx) * 4 + Math.floor(hash(gx + dx, gy + dy, 3) * 3);
                                const cy = (gy + dy) * 4 + Math.floor(hash(gx + dx, gy + dy, 5) * 3);
                                const ddx = X - cx;
                                const ddy = Y - cy;
                                // 2×2 的团，随机去掉一个角，近似圆斑
                                if (ddx >= 0 && ddx <= 1 && ddy >= 0 && ddy <= 1) {
                                    const cut = Math.floor(hash(gx + dx, gy + dy, 11) * 5);
                                    if (ddx + ddy * 2 !== cut) return mix(c, SPOT, 0.85);
                                }
                            }
                        }
                        return c;
                    });
                } else if (mat === 'wood') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.88 + 0.22 * hash(w >= h ? X : 0, w >= h ? 0 : Y, seed)));
                } else if (mat === 'wrap') {
                    // 弓把缠皮：斜向一道道
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, (u + v) % 2 ? 0.82 : 1.06));
                } else if (mat === 'leather') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = 0.9 + 0.18 * vnoise(X, Y, 2, seed);
                        if (vertical && (v === 1 || v === h - 2) && h > 4) f *= 1.18; // 箭囊上下两道压线
                        return scale(rgb, f);
                    });
                } else if (mat === 'horn') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.94 + 0.12 * vnoise(X, Y, 2, seed)));
                } else if (mat === 'bark') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const along = w >= h;
                        const across = along ? v : u;
                        let f = 0.9 + 0.2 * hash(along ? 0 : X, along ? Y : 0, seed);
                        if ((across + Math.floor(hash(along ? X >> 2 : 0, along ? 0 : Y >> 2, seed) * 3)) % 3 === 0) f *= 0.72;
                        return scale(rgb, f);
                    });
                } else if (mat === 'root') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => mix(scale(rgb, 0.85 + 0.3 * vnoise(X, Y, 2, seed)), [92, 70, 48],
                        hash(X, Y, seed) > 0.7 ? 0.5 : 0));
                } else if (mat === 'needle') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, hash(X, Y, seed) > 0.5 ? 1.18 : 0.78));
                }
            }
        }

        // ── 腹肌：中线 + 三道横线；胸肌下沿阴影 ──
        {
            const A = local('abdomen', 'front');
            if (A.r) {
                const w = A.r[2];
                const h = A.r[3];
                const mid = Math.floor(w / 2);
                A.px(mid - 1, 0, MA.shade('#c08a63', 0.8), 1, h);
                for (const fv of [0.25, 0.55, 0.85]) A.px(2, Math.floor(h * fv), MA.shade('#c08a63', 0.86), w - 4, 1);
            }
            const P = local('pec', 'bottom');
            if (P.r) P.px(0, 0, MA.shade('#c08a63', 0.7), P.r[2], P.r[3]);
            const PF = local('pec', 'front');
            if (PF.r) PF.px(0, PF.r[3] - 1, MA.shade('#c08a63', 0.82), PF.r[2], 1);
            for (const f of ['front', 'left', 'right']) {
                const F = local('forearm', f);
                if (F.r) F.px(Math.floor(F.r[2] / 2), 2, MA.shade('#c08a63', 0.86), 1, F.r[3] - 4);
            }
        }

        // ── 脸（head 前面）：斜眉、眼、眉间竖纹 ──
        {
            const H = local('head', 'front');
            if (H.r) {
                const w = H.r[2];
                const at = (u, v, c) => {
                    H.px(u, v, c);
                    H.px(w - 1 - u, v, c);
                };
                const dark = MA.shade('#c08a63', 0.62);
                // 眉骨（凸起的 brow 方块）盖住第 3–4 行；第 4 行画斜眉的下沿（外高内低）
                at(2, 4, '#3b2a1e');
                at(4, 4, '#3b2a1e');
                at(5, 4, '#3b2a1e');
                // 眼睛：2×2 深褐眼珠，内上角一像素高光；下方一道眼袋阴影（不加眼框）
                at(3, 5, '#2a1c14');
                at(4, 5, '#2a1c14');
                at(3, 6, '#2a1c14');
                at(4, 6, '#2a1c14');
                at(4, 5, '#e9d9c4');
                at(3, 7, MA.shade('#c08a63', 0.82));
                at(4, 7, MA.shade('#c08a63', 0.82));
                // 眉间竖纹（怒容）
                H.px(Math.floor(w / 2) - 1, 5, dark, 2, 2);
            }
            const N = local('nose', 'front');
            if (N.r) N.px(0, N.r[3] - 1, MA.shade('#c08a63', 0.75), N.r[2], 1);
            const B = local('brow', 'bottom');
            if (B.r) B.px(0, 0, MA.shade('#a8734f', 0.7), B.r[2], B.r[3]);
        }

        // ── 松树干两端截面：年轮 ──
        for (const f of ['left', 'right']) {
            const T = local('trunk', f);
            if (!T.r) continue;
            const c = Math.floor(T.r[2] / 2);
            T.px(c - 1, c - 1, '#a07a50', 2, 2);
            T.px(c - 2, c - 2, '#7a5634', 4, 1);
        }
    }

    return { paint };
})();
