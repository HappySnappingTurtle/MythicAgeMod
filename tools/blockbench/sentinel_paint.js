// 哨兵石像贴图细节（在按面明暗的底色之上绘制）。由 build_sentinel.js 调用，返回 {paint}。
// v3（2026-10-19，诱兽石「太糊」之后按同一精细度重画）：
//   黑石——成团的明暗（平滑噪声，不是逐像素杂点）、斜向凿痕、零星小坑与云母亮点；
//   青铜——成团的锤痕明暗、面下部与底面的小块铜绿（下沿有往下流的绿痕；凸起的抛光件不生）、零星划痕；盔沿、腰带、皮条裙铆钉，
//          护颊内框刻线，胫甲中脊，护臂两道箍，肩甲叠片；
//   鬃冠——竖向发缕、发梢亮；斗篷——竖褶明暗、下沿回纹边；弓把缠皮；方台回纹带；眼——两道暗绿刻痕（按像素镜像，不加框），上一行发光（_glowmask）。
// （胸肌上的小刻纹已去掉：远看像胸前又长了一双眼睛。）
(function () {
    const MA = window.MA;
    const RUNE = '#2e8a58';
    const RUNE_DARK = '#1c5a39';
    const EYE = '#2f9a62';
    const EYE_DEEP = '#123d27';
    const PATINA = [78, 138, 118];
    const PATINA_DARK = [47, 95, 82];
    const LEATHER = '#3b2a1c';
    const LEATHER_HI = '#5a4129';

    const hash = (x, y, k) => {
        let h = (x * 374761393 + y * 668265263 + k * 2147483647) | 0;
        h = Math.imul(h ^ (h >>> 13), 1274126177);
        return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
    };
    /** 平滑值噪声（网格点取 hash，双线性 + smoothstep），0–1。按图集坐标取样，同一面上连续。 */
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

    // 材质按底色识别（与 params/sentinel_parts.py 的色板一致）
    const MATERIAL = {
        '#30333A': 'stone', '#41454F': 'stone', '#24262C': 'stone', '#93693A': 'bronze', '#B28449': 'polished',
        '#694A26': 'bronze', '#1C1C20': 'hair', '#1D4A35': 'cloth', '#6B4A2A': 'wood',
    };

    function paint(ctx, nets, P) {
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
        /** 逐像素改色：fn(u, v, w, h, X, Y, rgb) 返回新的 [r, g, b]（X、Y 为图集坐标）。 */
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
        const scale = (rgb, f) => rgb.map((c) => c * f);
        const mix = (rgb, to, t) => rgb.map((c, i) => c + (to[i] - c) * t);
        const all = (group, fn) => {
            const n = nets[group];
            if (!n) return;
            for (const [face, r] of Object.entries(n.faces)) fn(local(group, face), face);
        };

        // ── 材质 ──
        let k = 0;
        for (const net of Object.values(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k * 7;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'stone') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = 0.74 + 0.52 * vnoise(X, Y, 3.5, seed) + 0.1 * vnoise(X, Y, 1.5, seed + 9);
                        const n = hash(X, Y, seed + 1);
                        if (n > 0.985) f *= 1.4; // 云母亮点
                        else if (n < 0.02) f *= 0.68; // 小坑
                        // 斜向凿痕：4×4 一格，选中的格子画一道对角短线
                        if (vertical && hash(X >> 2, Y >> 2, seed + 2) > 0.8 && (X & 3) === (Y & 3)) f *= 0.8;
                        return scale(rgb, f);
                    });
                } else if (mat === 'bronze' || mat === 'polished') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = 0.88 + 0.24 * vnoise(X, Y, 3, seed);
                        if (hash(X, Y, seed + 3) > 0.975) f *= 0.88; // 锤痕
                        let c = scale(rgb, f);
                        if (mat === 'polished') return c; // 凸起的抛光件（盔沿、鼻甲、胸肌、护膝）不生铜绿
                        // 铜绿：只在面的下部与底面成小块；最底一行往上流出一两像素的绿痕
                        const low = vertical && h > 1 ? v / (h - 1) : face === 'bottom' ? 1 : 0;
                        const p = vnoise(X, Y, 2, seed + 4);
                        if (low > 0.45 && p > 1.02 - 0.3 * low) c = mix(c, low > 0.8 ? PATINA_DARK : PATINA, 0.55);
                        else if (vertical && h > 3 && v >= h - 1 - Math.floor(hash(X, 0, seed + 5) * 3)
                            && hash(X, 1, seed + 5) > 0.55) c = mix(c, PATINA_DARK, 0.5);
                        return c;
                    });
                    // 零星划痕：一道 3 像素的浅色斜线
                    if (vertical && r[2] >= 6 && r[3] >= 4 && hash(r[0], r[1], seed + 6) > 0.6) {
                        const u0 = 1 + Math.floor(hash(r[0], 1, seed) * (r[2] - 4));
                        const v0 = 1 + Math.floor(hash(r[1], 2, seed) * (r[3] - 3));
                        recolor([r[0] + u0, r[1] + v0, 3, 2], (u, v, w, h, X, Y, rgb) =>
                            (u === 0 && v === 1) || (u === 1 && v === 1) || (u === 2 && v === 0) ? scale(rgb, 1.22) : rgb);
                    }
                } else if (mat === 'hair') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const strand = 0.78 + 0.42 * hash(X, 0, seed);
                        const tip = vertical && v === 0 ? 1.25 : 1;
                        return scale(rgb, strand * tip * (0.94 + 0.12 * hash(X, Y, seed + 1)));
                    });
                } else if (mat === 'cloth') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const fold = vertical ? 1 + 0.11 * Math.sin((u / 5) * 2 * Math.PI) : 1;
                        const fall = vertical ? 1 - 0.14 * (v / Math.max(1, h - 1)) : 1;
                        return scale(rgb, fold * fall * (0.95 + 0.08 * vnoise(X, Y, 3, seed)));
                    });
                } else if (mat === 'wood') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.88 + 0.2 * hash(0, Y, seed)));
                }
            }
        }

        const rivet = (L, u, v, base) => {
            L.px(u, v, MA.shade(base, 1.45));
            L.px(u, v + 1, MA.shade(base, 0.62));
        };

        // ── 方台：台身四面一道回纹带（上下两条边线 + 每 4 像素一组的回纹） ──
        for (const g of ['plinth_body', 'plinth_body_side']) {
            for (const f of ['front', 'back', 'left', 'right']) {
                const L = local(g, f);
                if (!L.r) continue;
                const h = L.r[3];
                L.px(0, 0, RUNE_DARK, L.r[2], 1);
                L.px(0, h - 1, RUNE_DARK, L.r[2], 1);
                for (let u = 0; u < L.r[2]; u++) {
                    const m = u % 4;
                    if (m === 0) L.px(u, 1, RUNE, 1, h - 2);
                    if (m === 1 || m === 2) L.px(u, 1, RUNE, 1, 1);
                    if (m === 2) L.px(u, 2, RUNE, 1, 1);
                }
            }
        }
        // 台面压边：外缘一圈崩口（暗）与受光棱（亮）
        all('plinth_cap', (L, face) => {
            if (face === 'top' || face === 'bottom' || !L.r) return;
            for (let u = 0; u < L.r[2]; u += 1) {
                if (hash(u, L.r[1], 31) > 0.82) L.px(u, 0, '#1d1f24');
            }
        });

        // ── 头盔：盔沿铆钉、盔顶中脊、护颊内框刻线 ──
        {
            const L = local('brow_band', 'front');
            if (L.r) for (let u = 2; u < L.r[2] - 1; u += 4) L.px(u, 0, MA.shade('#B28449', 1.4));
            const top = local('helm_cap', 'top');
            if (top.r) top.px(Math.floor(top.r[2] / 2) - 1, 0, MA.shade('#93693A', 1.25), 2, top.r[3]);
            for (const f of ['left', 'right']) {
                const C = local('cheek_guard', f);
                if (!C.r || C.r[2] < 4 || C.r[3] < 4) continue;
                const dark = MA.shade('#93693A', 0.62);
                C.px(1, 1, dark, C.r[2] - 2, 1);
                C.px(1, C.r[3] - 2, dark, C.r[2] - 2, 1);
                C.px(1, 1, dark, 1, C.r[3] - 2);
                C.px(C.r[2] - 2, 1, dark, 1, C.r[3] - 2);
            }
            const nape = local('nape_guard', 'back');
            if (nape.r) for (let u = 1; u < nape.r[2]; u += 3) nape.px(u, nape.r[3] - 2, MA.shade('#93693A', 0.7));
        }

        // ── 腰带与皮条裙：铆钉；皮条下沿穗边（每隔一像素一个暗缺口） ──
        {
            const B = local('belt', 'front');
            if (B.r) for (let u = 1; u < B.r[2]; u += 3) rivet(B, u, 0, '#694A26');
            for (const g of ['strip', 'strip_side']) {
                for (const f of ['front', 'back', 'left', 'right']) {
                    const L = local(g, f);
                    if (!L.r || L.r[2] < 2) continue;
                    rivet(L, Math.floor(L.r[2] / 2), 1, '#93693A');
                    for (let u = 0; u < L.r[2]; u += 2) L.px(u, L.r[3] - 1, '#3a2812');
                    L.px(0, 0, MA.shade('#93693A', 0.7), 1, L.r[3]); // 条与条之间的缝
                }
            }
        }

        // ── 胫甲中脊与护膝、护臂两道箍、肩甲叠片 ──
        {
            const G = local('greave', 'front');
            if (G.r) {
                const c = Math.floor(G.r[2] / 2);
                G.px(c - 1, 0, MA.shade('#93693A', 1.3), 1, G.r[3]);
                G.px(c, 0, MA.shade('#93693A', 0.78), 1, G.r[3]);
            }
            for (const g of ['vambrace', 'vambrace_level']) {
                for (const f of ['front', 'back', 'left', 'right', 'top', 'bottom']) {
                    const L = local(g, f);
                    if (!L.r || L.r[3] < 4) continue;
                    const dark = MA.shade('#93693A', 0.6);
                    const vertical = f !== 'top' && f !== 'bottom';
                    if (vertical) {
                        L.px(0, 1, dark, L.r[2], 1);
                        L.px(0, L.r[3] - 2, dark, L.r[2], 1);
                    } else {
                        L.px(1, 0, dark, 1, L.r[3]);
                        L.px(L.r[2] - 2, 0, dark, 1, L.r[3]);
                    }
                }
            }
            const T = local('pauldron', 'top');
            if (T.r) for (let v = 2; v < T.r[3]; v += 3) T.px(0, v, MA.shade('#93693A', 0.72), T.r[2], 1);
            for (const f of ['front', 'back', 'left', 'right']) {
                const L = local('pauldron', f);
                if (L.r) L.px(0, L.r[3] - 1, MA.shade('#93693A', 0.6), L.r[2], 1);
            }
        }

        // ── 弓把缠皮 ──
        all('bow_riser', (L, face) => {
            if (!L.r || face === 'top' || face === 'bottom') return;
            for (let v = 1; v < L.r[3] - 1; v++) L.px(0, v, v % 2 ? LEATHER : LEATHER_HI, L.r[2], 1);
        });

        // ── 斗篷：下缘一道暗绿回纹边 ──
        {
            const L = local('cape', 'back');
            if (L.r) {
                const h = L.r[3];
                L.px(0, h - 3, RUNE, L.r[2], 1);
                for (let u = 0; u < L.r[2]; u += 3) L.px(u, h - 2, RUNE, 1, 1);
            }
        }

        // ── 眼：两道暗绿刻痕，按像素图案画在头前面右半边并左右镜像（检查清单 §二：不加框、面宽偶数）。
        // 头前面 12×12 像素；第 0 行在盔下沿以上，眼在第 5、6 行（盔沿正下方）。列号 0 在画面左。
        {
            const L = local('head', 'front');
            const W = L.r[2];
            const eye = [[7, 5, EYE], [8, 5, EYE], [9, 5, EYE], [8, 6, EYE_DEEP], [9, 6, EYE_DEEP]];
            for (const [u, v, c] of eye) {
                L.px(u, v, c);
                L.px(W - 1 - u, v, c);
            }
            // 面颊下的阴影线（石像的颧骨）
            for (const u of [7, 8]) {
                L.px(u, 8, MA.shade('#30333A', 0.75));
                L.px(W - 1 - u, 8, MA.shade('#30333A', 0.75));
            }
        }
    }

    /** 发光贴图：只有眼睛的亮色刻痕（暗色那一行不发光，保留刻痕的深度）。 */
    function glow(ctx, nets) {
        const r = nets.head && nets.head.faces.front;
        if (!r) return;
        ctx.fillStyle = EYE;
        for (const u of [7, 8, 9]) {
            ctx.fillRect(r[0] + u, r[1] + 5, 1, 1);
            ctx.fillRect(r[0] + r[2] - 1 - u, r[1] + 5, 1, 1);
        }
    }

    return { paint, glow };
})();
