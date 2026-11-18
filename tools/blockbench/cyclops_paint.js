// 独眼巨人贴图细节（在按面明暗的底色之上绘制）。由 build_cyclops.js 调用，返回 {paint}。
// v1（2026-11-17，白模审阅用）：
//   皮肤——日晒粗糙的成团明暗，啤酒肚肚脐与下沿阴影、胸肌下沿、二头肌分界、颈部褶；指甲与趾甲；
//   独眼——眼白（上沿一行眼睑阴影，不描边、不加框）、几丝血丝；琥珀色虹膜 + 2×2 瞳孔 + 一点高光（按像素居中）；
//   眉骨——两道横皱与眉心竖纹；鼻——鼻孔；嘴——深色口内 + 上排参差的牙；
//   头发与胡须——竖向粗发缕、发梢亮；橄榄木——顺纹理的纵纹、树疤的年轮；皮革、腰绳（斜绞）、裹裙（粗布纹）；
//   羊皮——卷毛（2×2 亮团错开排）；巨石——斑驳与裂纹。
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

    // 材质按底色识别（与 params/cyclops_parts.py 的色板一致）
    const MATERIAL = {
        '#B98663': 'skin', '#9A6A4C': 'skin', '#2F251D': 'hair', '#3D2F22': 'hair', '#E9E2CF': 'eye', '#9C6B2E': 'iris', '#3A1F1A': 'mouth',
        '#E2D6B4': 'tooth', '#5A3A22': 'leather', '#7C4A2E': 'cloth', '#9C8656': 'rope', '#7A6A4E': 'wood', '#4F4330': 'wood',
        '#E4D9C0': 'fleece', '#B9AB8C': 'fleece', '#8A857A': 'stone', '#D6C5A6': 'nail',
    };
    const SKIN = '#b98663';

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
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, 0.93 + 0.12 * vnoise(X, Y, 3, seed));
                        if (hash(X, Y, seed) > 0.93) c = mix(c, [150, 90, 70], 0.25); // 晒斑
                        return c;
                    });
                } else if (mat === 'hair') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const strand = 0.78 + 0.36 * hash(vertical ? X : X + Y, 0, seed);
                        const tip = vertical && v >= h - 1 ? 1.2 : 1;
                        return scale(rgb, strand * tip);
                    });
                } else if (mat === 'wood') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        // 棍身竖直：侧面顺纹理的竖纹，顶底面画年轮
                        if (!vertical) {
                            const d = Math.max(Math.abs(u - (w - 1) / 2), Math.abs(v - (h - 1) / 2));
                            return scale(rgb, Math.round(d) % 2 ? 0.86 : 1.06);
                        }
                        let f = 0.84 + 0.26 * hash(X, 0, seed);
                        if (hash(X, Math.floor(Y / 3), seed + 1) > 0.9) f *= 0.75; // 裂纹
                        return scale(rgb, f);
                    });
                } else if (mat === 'leather') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.88 + 0.2 * vnoise(X, Y, 2, seed)));
                } else if (mat === 'cloth') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = (u + v) % 2 ? 0.94 : 1.03;
                        if (vertical && v === h - 1) f *= 0.8; // 毛边
                        return scale(rgb, f * (0.95 + 0.1 * vnoise(X, Y, 3, seed)));
                    });
                } else if (mat === 'rope') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, (u + v) % 3 === 0 ? 0.74 : 1.06));
                } else if (mat === 'fleece') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        // 卷毛：2×2 亮团错开排，团缝偏暗
                        const cx = Math.floor((X + (Math.floor(Y / 2) % 2)) / 2);
                        const cy = Math.floor(Y / 2);
                        const curl = hash(cx, cy, seed) > 0.35;
                        return scale(rgb, curl ? 1.02 + 0.04 * hash(X, Y, seed) : 0.92);
                    });
                } else if (mat === 'stone') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, 0.86 + 0.24 * vnoise(X, Y, 2, seed));
                        if (hash(X, Y, seed) > 0.95) c = scale(c, 0.7);
                        return c;
                    });
                } else if (mat === 'tooth') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, v === h - 1 ? 0.86 : 1));
                }
            }
        }

        // ── 独眼：眼白（上沿两行眼睑阴影，不描边、不加框）、左右外侧各一丝淡血丝；
        //    虹膜横竖两块交叉成圆（琥珀色，上沿一行压暗）、瞳孔 2×2 居中、高光在瞳孔左上（不压瞳孔） ──
        for (const g of ['eye_h', 'eye_v']) {
            const E = local(g, 'front');
            if (!E.r) continue;
            const w = E.r[2];
            const h = E.r[3];
            E.px(0, 0, MA.shade('#e9e2cf', 0.76), w, 1);
            E.px(0, 1, MA.shade('#e9e2cf', 0.9), w, 1);
            if (g === 'eye_h') {
                const mid = Math.floor(h / 2);
                E.px(0, mid, '#cf9a88', 1, 1);
                E.px(w - 1, mid, '#cf9a88', 1, 1);
            }
        }
        for (const g of ['iris_h', 'iris_v']) {
            const I = local(g, 'front');
            if (!I.r) continue;
            const w = I.r[2];
            const h = I.r[3];
            I.px(0, 0, MA.shade('#9c6b2e', 0.78), w, 1);
            const cu = Math.floor(w / 2) - 1;
            const cv = Math.floor(h / 2) - 1;
            I.px(cu, cv, '#120c08', 2, 2);
            if (g === 'iris_h') I.px(cu - 1, cv, '#f4ecd8', 1, 1);
        }

        // ── 眉骨：两道横皱与眉心竖纹（压在眼上，怒容） ──
        {
            const B = local('brow', 'front');
            if (B.r) {
                const w = B.r[2];
                B.px(1, 0, MA.shade('#9a6a4c', 0.82), w - 2, 1);
                B.px(Math.floor(w / 2) - 1, 1, MA.shade('#9a6a4c', 0.7), 2, B.r[3] - 1);
                const BB = local('brow', 'bottom');
                if (BB.r) BB.px(0, 0, MA.shade('#9a6a4c', 0.6), BB.r[2], BB.r[3]);
            }
        }

        // ── 鼻孔、嘴（口内深色 + 上排参差的牙）、颧骨下沿阴影 ──
        {
            const N = local('nose', 'bottom');
            if (N.r) {
                const w = N.r[2];
                N.px(1, 1, '#5a3424', 2, 1);
                N.px(w - 3, 1, '#5a3424', 2, 1);
            }
            const NF = local('nose', 'front');
            if (NF.r) NF.px(0, NF.r[3] - 1, MA.shade(SKIN, 0.82), NF.r[2], 1);
            const M = local('mouth', 'front');
            if (M.r) {
                const w = M.r[2];
                for (let u = 0; u < w; u++) {
                    if (u % 3 !== 2) M.px(u, 0, u % 2 ? '#d8cba6' : '#e8dcb8', 1, 1);
                }
            }
            const C = local('cheekbone', 'bottom');
            if (C.r) C.px(0, 0, MA.shade(SKIN, 0.7), C.r[2], C.r[3]);
        }

        // ── 身体细节：肚脐与肚子下沿、胸肌下沿、二头肌分界、颈褶 ──
        {
            const B = local('belly_front', 'front');
            if (B.r) {
                const w = B.r[2];
                B.px(Math.floor(w / 2) - 1, Math.floor(B.r[3] / 2), MA.shade(SKIN, 0.62), 2, 1);
                B.px(0, B.r[3] - 1, MA.shade(SKIN, 0.82), w, 1);
            }
            const P = local('pec', 'bottom');
            if (P.r) P.px(0, 0, MA.shade(SKIN, 0.68), P.r[2], P.r[3]);
            const PF = local('pec', 'front');
            if (PF.r) PF.px(0, PF.r[3] - 1, MA.shade(SKIN, 0.8), PF.r[2], 1);
            const BI = local('bicep', 'front');
            if (BI.r) BI.px(0, BI.r[3] - 1, MA.shade(SKIN, 0.82), BI.r[2], 1);
            const NK = local('neck', 'front');
            if (NK.r) NK.px(1, Math.floor(NK.r[3] / 2), MA.shade(SKIN, 0.84), NK.r[2] - 2, 1);
        }

        // ── 指甲与趾甲：左手四指下端、脚趾前面 ──
        {
            const F = local('fingers_l', 'bottom');
            if (F.r) {
                for (let u = 1; u < F.r[2] - 1; u += 3) F.px(0, u, '#d6c5a6', F.r[2], 2);
            }
            const FF = local('fingers_l', 'front');
            if (FF.r) FF.px(0, FF.r[3] - 2, '#d6c5a6', FF.r[2], 1);
            const T = local('toes', 'front');
            if (T.r) {
                const w = T.r[2];
                for (let u = 0; u < w; u += 3) T.px(u, 0, '#d6c5a6', 2, 1);
                for (let u = 2; u < w; u += 3) T.px(u, 0, MA.shade(SKIN, 0.7), 1, T.r[3]);
            }
            const K = local('knuckles_r', 'right');
            if (K.r) {
                for (let v = 0; v < K.r[3]; v += 3) K.px(0, v, MA.shade('#9a6a4c', 0.8), K.r[2], 1);
            }
        }
    }

    return { paint };
})();
