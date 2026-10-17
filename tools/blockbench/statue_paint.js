// T11b 主神像贴图细节（在底色之上绘制）。由 build_statue.js 调用，返回 {paint}。
// 大理石：极轻的颗粒 + 稀疏的灰色细纹；须发：卷曲的小涡与竖向发缕；五官按像素图案画并左右镜像（检查清单 §二.4）；
// 布料：竖褶阴影；彩绘：希玛申下摆回纹、斜披带金边；金件：顶亮 + 斑驳；台座：浅刻回纹。
// 各主神共用；主神特有的细节写在 EXTRA[主神] 里。
(function () {
    const MA = window.MA;
    const MARBLE = '#E7E2D7';
    const SHADE = '#D2CBBC';
    const HAIR = '#DAD3C4';
    const PLINTH = '#CFCAC0';
    const GOLD = '#C9A13B';
    const BORDER = '#3E5C8A';
    const OCHRE = '#C79A3E';
    const BRONZE = '#A8782E';
    const DARK_GOLD = '#8A6420';
    const TEAL = '#2E7D7A';
    const PATINA = '#5E9C8F';
    const PATINA_DARK = '#3F6F68';

    const hash = (x, y, k) => {
        let h = (x * 374761393 + y * 668265263 + k * 2147483647) | 0;
        h = Math.imul(h ^ (h >>> 13), 1274126177);
        return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
    };

    const MATERIAL = {
        [MARBLE]: 'marble', [SHADE]: 'cloth', [HAIR]: 'hair', [PLINTH]: 'stone', [GOLD]: 'gold', [BORDER]: 'border',
        [BRONZE]: 'gold', [DARK_GOLD]: 'gold', [TEAL]: 'border', [PATINA]: 'gold', [PATINA_DARK]: 'gold',
    };

    const SIDES = ['left', 'front', 'right', 'back'];

    function helpers(ctx, nets) {
        const local = (group, face) => {
            const n = nets[group];
            const r = n ? n.faces[face] : null;
            return {
                r,
                px: (u, v, color, w = 1, h = 1) => {
                    if (!r || u < 0 || v < 0 || u >= r[2] || v >= r[3]) return;
                    ctx.fillStyle = color;
                    ctx.fillRect(r[0] + u, r[1] + v, Math.min(w, r[2] - u), Math.min(h, r[3] - v));
                },
            };
        };
        const modulate = (r, fn) => {
            const img = ctx.getImageData(r[0], r[1], r[2], r[3]);
            for (let v = 0; v < r[3]; v++) {
                for (let u = 0; u < r[2]; u++) {
                    const f = fn(u, v, r[2], r[3]);
                    const i = (v * r[2] + u) * 4;
                    for (let c = 0; c < 3; c++) {
                        img.data[i + c] = Math.max(0, Math.min(255, Math.round(img.data[i + c] * f)));
                    }
                }
            }
            ctx.putImageData(img, r[0], r[1]);
        };
        /** 按像素图案左右镜像绘制（图案写画面右半边的列号）。 */
        const mirror = (group, face, rows, colors, top) => {
            const L = local(group, face);
            if (!L.r) return;
            rows.forEach((row, j) => {
                for (const [col, ch] of row) {
                    L.px(col, top + j, colors[ch]);
                    L.px(L.r[2] - 1 - col, top + j, colors[ch]);
                }
            });
        };
        return { local, modulate, mirror };
    }

    /** 卷发 / 卷须：每 3×3 一个小涡（左上暗弧 + 中心亮点），行间错开半个涡。 */
    function curls(H, group, faces, base, seed) {
        // 小涡：一段 3 像素的弧形浅影（开口方向随机），行间错开、位置抖动，避免排成网格（v1 被看成一排孔）
        const dark = MA.shade(base, 0.88);
        const light = MA.shade(base, 1.05);
        const arcs = [[[0, 0], [1, 0], [0, 1]], [[0, 0], [1, 0], [1, 1]], [[0, 0], [0, 1], [1, 1]], [[1, 0], [0, 1], [1, 1]]];
        for (const f of faces) {
            const L = H.local(group, f);
            if (!L.r) continue;
            for (let v = 0; v < L.r[3]; v += 3) {
                const off = (v / 3) % 2 === 0 ? 0 : 2;
                for (let u = off; u < L.r[2]; u += 4) {
                    const h = hash(u, v, seed);
                    const du = h < 0.5 ? 0 : 1;
                    for (const [a, b] of arcs[Math.floor(hash(v, u, seed + 1) * 4)]) L.px(u + du + a, v + b, dark);
                    if (h > 0.3) L.px(u + du + 2, v, light);
                }
            }
        }
    }

    /** 波浪发（波塞冬）：每 3 行一道起伏的暗线，行间错开，像湿漉漉的长卷浪。 */
    function waves(H, group, faces, base, seed) {
        const dark = MA.shade(base, 0.8);
        const light = MA.shade(base, 1.07);
        for (const f of faces) {
            const L = H.local(group, f);
            if (!L.r) continue;
            for (let v = 0; v < L.r[3]; v += 3) {
                const off = (v / 3) % 2 === 0 ? 0 : 2;
                for (let u = 0; u < L.r[2]; u++) {
                    const ph = (u + off) % 4;
                    if (ph === 0 || ph === 1) L.px(u, v + (ph === 1 ? 1 : 0), dark);
                    else if (ph === 2 && hash(u, v, seed) > 0.5) L.px(u, v, light);
                }
            }
        }
    }

    /** 海浪纹（卷浪）：高 ≥4 像素的带子上画一行，周期 6。 */
    function wavePattern(L, color, top) {
        if (!L.r || L.r[3] - top < 4) return;
        const key = ['..XXX.', '.X...X', 'X..X..', 'XXX...'];
        for (let u = 0; u < L.r[2]; u++) {
            key.forEach((row, j) => {
                if (row[u % 6] === 'X') L.px(u, top + j, color);
            });
        }
    }

    /** 回纹（希腊钥匙纹）：高 ≥4 像素的带子上画一行，周期 6。 */
    function meander(L, color, top) {
        if (!L.r || L.r[3] - top < 4) return;
        const key = ['XXXXX.', '....X.', 'XXX.X.', 'X...X.'];
        for (let u = 0; u < L.r[2]; u++) {
            key.forEach((row, j) => {
                if (row[u % 6] === 'X') L.px(u, top + j, color);
            });
        }
    }

    function paint(ctx, nets, P, god) {
        const H = helpers(ctx, nets);
        const { local, modulate, mirror } = H;

        // ── 材质 ──
        let k = 0;
        for (const net of Object.values(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'marble' || mat === 'stone') {
                    // 颗粒 + 稀疏斜向细纹（大理石纹理）
                    const veinPhase = Math.floor(hash(seed, 0, 7) * 9);
                    modulate(r, (u, v) => {
                        const vein = (u + v + veinPhase) % 11 === 0 && hash(u, v, seed + 5) < 0.55 ? 0.94 : 1.0;
                        return vein * (0.985 + 0.03 * hash(u, v, seed));
                    });
                } else if (mat === 'cloth') {
                    modulate(r, (u, v) => (vertical && u % 3 === 2 ? 0.93 : 1.0) * (0.985 + 0.03 * hash(u, v, seed)));
                } else if (mat === 'hair') {
                    modulate(r, (u, v) => 0.95 + 0.06 * hash(u, 0, seed) + 0.03 * hash(u, v, seed + 3));
                } else if (mat === 'gold') {
                    modulate(r, (u, v) => (v === 0 && vertical ? 1.18 : 1.0) * (0.9 + 0.16 * hash(u, v, seed)));
                } else if (mat === 'border') {
                    modulate(r, (u, v) => 0.94 + 0.1 * hash(u, v, seed));
                }
            }
        }

        // ── 须发：卷发、卷须（宙斯）或波浪发（波塞冬，P.hair_style = 'waves'）──
        const hairFn = P.hair_style === 'waves' ? waves : curls;
        hairFn(H, 'hair_cap', ['top', 'left', 'right', 'back', 'front'], HAIR, 11);
        hairFn(H, 'fringe', ['front', 'top'], HAIR, 12);
        hairFn(H, 'hair_side', SIDES, HAIR, 13);
        hairFn(H, 'beard_front', ['front', 'left', 'right', 'bottom'], HAIR, 14);
        hairFn(H, 'beard_chin', SIDES.concat(['bottom']), HAIR, 15);
        hairFn(H, 'beard_mid', SIDES.concat(['bottom']), HAIR, 17);
        hairFn(H, 'beard_lower', SIDES.concat(['bottom']), HAIR, 18);
        hairFn(H, 'beard_side', SIDES, HAIR, 16);
        hairFn(H, 'beard_fork', SIDES.concat(['bottom']), HAIR, 19);
        hairFn(H, 'hair_front_lock', SIDES, HAIR, 20);
        if (P.hair_style === 'waves') hairFn(H, 'hair_lock', SIDES, HAIR, 21);  // 宙斯定版时垂肩发绺没画卷纹，保持不变
        // 后发：竖向发缕（每 2 列一道暗缝），末端参差
        {
            const L = local('hair_back', 'back');
            if (L.r) {
                for (let u = 1; u < L.r[2]; u += 2) L.px(u, 0, MA.shade(HAIR, 0.86), 1, L.r[3] - 1);
                for (let u = 0; u < L.r[2]; u++) if (hash(u, 3, 21) < 0.5) L.px(u, L.r[3] - 1, MA.shade(HAIR, 0.8));
            }
        }

        // ── 五官（大理石雕像：眼睑阴影 + 刻出的瞳孔，不画眼白与眼框）──
        // 头前面宽 W 像素（偶数）；图案按「离中线第几列」写在画面右半边（角色左侧），镜像出另一半；行号按眉骨下沿定位。
        //   L 上眼睑阴影  P 瞳孔刻痕  l 下眼睑浅影  c 颧骨浅影
        {
            const F = P.face || { eyeTop: 7 };
            const W = local('head', 'front').r[2];
            const c0 = W / 2;  // 中线右侧第一列
            const at = (k) => c0 + k;
            if (F.v === 2) {
                // v2.2 精细五官（眼窝在眉骨与颧骨之间）：上眼睑阴影一行、眼白 + 两像素虹膜（两眼对称、平视，不画单独的瞳孔
                // 以免斗鸡眼）、下眼睑浅影、眼角细纹；额头两道浅横纹（中间断开）；
                //   L 上眼睑  w 眼白（微亮的大理石）  I 虹膜（刻痕灰）  l 下眼睑  c 眼角纹  f 额纹
                mirror('head', 'front', [
                    [[at(1), 'L'], [at(2), 'L'], [at(3), 'L'], [at(4), 'L']],
                    [[at(1), 'w'], [at(2), 'I'], [at(3), 'I'], [at(4), 'w'], [at(5), 'c']],
                    [[at(2), 'l'], [at(3), 'l']],
                ], { L: MA.shade(MARBLE, 0.7), w: MA.shade(MARBLE, 1.04), I: '#6E665C', l: MA.shade(MARBLE, 0.88),
                    c: MA.shade(MARBLE, 0.9) }, F.eyeTop);
                mirror('head', 'front', [
                    [[at(1), 'f'], [at(2), 'f'], [at(3), 'f']],
                    [],
                    [[at(2), 'f'], [at(3), 'f'], [at(4), 'f']],
                ], { f: MA.shade(MARBLE, 0.92) }, F.eyeTop - 6);
            } else {
                //   L 上眼睑阴影  P 瞳孔刻痕  l 下眼睑浅影  c 颧骨浅影
                mirror('head', 'front', [
                    [[at(1), 'L'], [at(2), 'L'], [at(3), 'L']],
                    [[at(2), 'P'], [at(1), 'l'], [at(3), 'l']],
                    [[at(2), 'l']],
                    [[at(3), 'c'], [at(4), 'c']],
                ], { L: MA.shade(MARBLE, 0.74), P: '#6E665C', l: MA.shade(MARBLE, 0.88), c: MA.shade(MARBLE, 0.94) }, F.eyeTop);
            }
        }
        // 粗眉：发丝竖纹、下沿压暗；颧骨下沿（法令纹的起点）一道浅影
        for (const f of ['front', 'top']) {
            const E = local('eyebrow', f);
            if (E.r) for (let u = 1; u < E.r[2]; u += 3) E.px(u, 0, MA.shade(HAIR, 0.92), 1, E.r[3]);
        }
        {
            const E = local('eyebrow', 'front');
            if (E.r) E.px(0, E.r[3] - 1, MA.shade(HAIR, 0.75), E.r[2], 1);
            const C = local('cheek', 'front');
            if (C.r) C.px(0, C.r[3] - 1, MA.shade(MARBLE, 0.86), C.r[2], 1);
            const T = local('nose_tip', 'bottom');
            if (T.r) {
                T.px(0, 0, MA.shade(MARBLE, 0.6), 1, T.r[3]);
                T.px(T.r[2] - 1, 0, MA.shade(MARBLE, 0.6), 1, T.r[3]);
            }
            const N = local('nose_tip', 'front');
            if (N.r) N.px(1, 0, MA.shade(MARBLE, 1.05), Math.max(1, N.r[2] - 2), 1);
            const Lp = local('lower_lip', 'front');
            if (Lp.r) Lp.px(0, 0, MA.shade(MARBLE, 0.86), Lp.r[2], 1);
            for (const f of ['front', 'left', 'right']) {
                const M = local('moustache_end', f);
                if (M.r) for (let u = 0; u < M.r[2]; u += 2) M.px(u, 0, MA.shade(HAIR, 0.82), 1, M.r[3]);
            }
        }
        // 眉骨下沿压一道阴影，鼻梁两侧浅影
        {
            const B = local('brow', 'front');
            if (B.r) B.px(0, B.r[3] - 1, MA.shade(MARBLE, 0.84), B.r[2], 1);
            const N = local('nose', 'front');
            if (N.r) {
                N.px(0, 0, MA.shade(MARBLE, 0.95), 1, N.r[3]);
                N.px(N.r[2] - 1, 0, MA.shade(MARBLE, 0.95), 1, N.r[3]);
                N.px(1, N.r[3] - 1, MA.shade(MARBLE, 0.82), Math.max(1, N.r[2] - 2), 1);
            }
        }
        // 嘴：胡须前片宽 16 像素（偶数），唇线 4 像素居中，下唇一点亮
        {
            const W = local('beard_front', 'front').r[2];
            const c0 = W / 2;
            const v2 = (P.face || {}).v === 2;
            mirror('beard_front', 'front', v2 ? [
                [[c0, 'm'], [c0 + 1, 'm']],
            ] : [
                [[c0, 'm'], [c0 + 1, 'm']],
                [[c0, 'u']],
            ], { m: MA.shade(HAIR, 0.62), u: MA.shade(MARBLE, 1.04) }, (P.face || {}).mouthTop || 3);
        }
        // 八字须：两端向下卷
        {
            const M = local('moustache', 'front');
            if (M.r) {
                M.px(0, 0, MA.shade(HAIR, 0.82), M.r[2], 1);
                M.px(0, 1, MA.shade(HAIR, 0.78));
                M.px(M.r[2] - 1, 1, MA.shade(HAIR, 0.78));
            }
        }

        // ── 躯干肌肉：胸肌下缘、腹直肌（中线 + 两道横线）、肚脐 ──
        {
            const pec = local('pec', 'front');
            if (pec.r) pec.px(0, pec.r[3] - 1, MA.shade(MARBLE, 0.86), pec.r[2], 1);
            const ab = local('abdomen', 'front');
            if (ab.r) {
                const mid = Math.floor(ab.r[2] / 2);
                ab.px(mid - 1, 0, MA.shade(MARBLE, 0.9), 2, ab.r[3] - 1);
                for (const v of [2, 5]) {
                    ab.px(mid - 5, v, MA.shade(MARBLE, 0.92), 4, 1);
                    ab.px(mid + 1, v, MA.shade(MARBLE, 0.92), 4, 1);
                }
                ab.px(mid - 1, ab.r[3] - 2, MA.shade(MARBLE, 0.78), 2, 1);
            }
            const ch = local('chest', 'front');
            if (ch.r) {
                const mid = Math.floor(ch.r[2] / 2);
                ch.px(mid - 1, 4, MA.shade(MARBLE, 0.92), 2, ch.r[3] - 4);
            }
        }

        {
            const A = local('abs', 'front');
            if (A.r) A.px(0, A.r[3] - 1, MA.shade(MARBLE, 0.86), A.r[2], 1);
        }

        // ── 布料：斜披带两侧金边；下摆彩绘回纹（蓝底金纹）；垂布底边金线 ──
        for (const f of ['front', 'back']) {
            const L = local('sash', f);
            if (L.r) {
                L.px(0, 0, OCHRE, 1, L.r[3]);
                L.px(L.r[2] - 1, 0, OCHRE, 1, L.r[3]);
            }
        }
        const band = P.pattern === 'wave' ? wavePattern : meander;
        for (const f of SIDES) {
            band(local('skirt_hem', f), GOLD, 0);
        }
        for (const f of SIDES) {
            const L = local('drape_tip', f);
            if (L.r) L.px(0, L.r[3] - 1, OCHRE, L.r[2], 1);
        }
        for (const f of SIDES) {
            const L = local('waist_roll', f);
            if (L.r) for (let u = 0; u < L.r[2]; u += 3) L.px(u, 1, MA.shade(SHADE, 0.88), 2, 1);
        }

        // ── 金件：雷霆握柄的螺旋纹、叉尖提亮；冠叶叶脉；凉鞋带 ──
        {
            const G = local('bolt_grip', 'left');
            if (G.r) for (let u = 0; u < G.r[2]; u += 3) G.px(u, 0, MA.shade(GOLD, 0.7), 1, G.r[3]);
            const G2 = local('bolt_grip', 'top');
            if (G2.r) for (let v = 0; v < G2.r[3]; v += 3) G2.px(0, v, MA.shade(GOLD, 0.7), G2.r[2], 1);
            for (const f of SIDES) {
                const L = local('bolt_prong', f);
                if (L.r) L.px(L.r[2] - 1, 0, '#F4DC8A', 1, L.r[3]);
            }
        }
        {
            // 额前冠带：一排斜向小叶纹
            const L = local('wreath_front', 'front');
            if (L.r) for (let u = 0; u < L.r[2]; u += 2) L.px(u, 0, MA.shade(GOLD, 0.72));
        }
        for (const g of ['wreath_leaf', 'wreath_leaf_front']) {
            for (const f of SIDES) {
                const L = local(g, f);
                if (L.r && L.r[2] >= 3) L.px(0, Math.floor(L.r[3] / 2), MA.shade(GOLD, 0.72), L.r[2], 1);
            }
        }

        // ── 台座：上台四周浅刻回纹，下台顶边一道亮线 ──
        for (const f of SIDES) {
            (P.pattern === 'wave' ? wavePattern : meander)(local('plinth_top', f), MA.shade(PLINTH, 0.84), 0);
            const L = local('plinth_base', f);
            if (L.r) L.px(0, 0, MA.shade(PLINTH, 1.08), L.r[2], 1);
        }

        const extra = EXTRA[god];
        if (extra) extra(H, ctx, nets, P);
    }

    const EXTRA = {};

    EXTRA.poseidon = (H) => {
        const { local } = H;
        for (const f of ['front', 'left', 'right']) {
            const L = local('fist', f);
            if (!L.r) continue;
            for (let i = 1; i <= 3; i++) L.px(Math.round(L.r[2] * i / 4), 1, MA.shade(MARBLE, 0.8), 1, L.r[3] - 2);
        }
        // 戟杆螺旋暗纹；戟头叉尖提亮
        for (const f of SIDES) {
            const L = local('trident_shaft', f);
            if (L.r) for (let v = 0; v < L.r[3]; v += 4) L.px(0, v, MA.shade(BRONZE, 0.75), L.r[2], 1);
            for (const g of ['trident_prong_mid', 'trident_prong_side', 'trident_tip_mid']) {
                const P2 = local(g, f);
                if (P2.r) P2.px(0, 0, '#F4DC8A', P2.r[2], 1);
            }
        }
        // 冠齿：顶端提亮
        for (const g of ['crown_spike_mid', 'crown_spike1', 'crown_spike2', 'crown_spike_side']) {
            for (const f of SIDES) {
                const L = local(g, f);
                if (L.r) L.px(0, 0, '#F4DC8A', L.r[2], 1);
            }
        }
        // 后垂布与斜披带：竖褶
        for (const g of ['back_drape']) {
            for (const f of ['back', 'front']) {
                const L = local(g, f);
                if (L.r) for (let u = 2; u < L.r[2]; u += 4) L.px(u, 0, MA.shade(SHADE, 0.86), 1, L.r[3]);
            }
        }
        // 海豚：浅色腹部（底面与正背两面的下缘）、眼睛（胸段正背两面靠喙一端）、背鳍暗边
        for (const g of ['dolphin_tail', 'dolphin_body', 'dolphin_chest', 'dolphin_beak']) {
            const B = local(g, 'bottom');
            if (B.r) B.px(0, 0, MA.shade(PATINA, 1.3), B.r[2], B.r[3]);
            for (const f of ['front', 'back']) {
                const L = local(g, f);
                if (L.r) L.px(0, L.r[3] - 1, MA.shade(PATINA, 1.28), L.r[2], 1);
            }
        }
        for (const f of ['front', 'back']) {
            const L = local('dolphin_chest', f);
            // 胸段绕 z 转了约 160°，本地 +x 指向喙：正面的 u 随本地 x 增大，眼睛靠高 u 一端；背面相反
            if (L.r) L.px(f === 'front' ? L.r[2] - 2 : 1, 1, '#1E2A28');
        }
        for (const g of ['dolphin_wave', 'dolphin_wave_crest']) {
            const T = local(g, 'top');
            if (T.r) for (let u = 0; u < T.r[2]; u++) for (let v = 0; v < T.r[3]; v++) if ((u + v) % 3 === 0) T.px(u, v, '#E8F2EE');
            for (const f of SIDES) {
                const L = local(g, f);
                if (L.r) for (let u = 0; u < L.r[2]; u += 2) L.px(u, 0, '#E8F2EE');
            }
        }
    };

    EXTRA.zeus = (H) => {
        const { local } = H;
        // 拳头：正面四道指缝、顶面指节一排暗点
        for (const f of ['front', 'left', 'right']) {
            const L = local('fist', f);
            if (!L.r) continue;
            for (let i = 1; i <= 3; i++) L.px(Math.round(L.r[2] * i / 4), 1, MA.shade(MARBLE, 0.8), 1, L.r[3] - 2);
        }
        // 披风：前后面竖褶阴影（每 5 列），底缘金线
        for (const g of ['cape_upper', 'cape_lower']) {
            for (const f of ['front', 'back']) {
                const L = local(g, f);
                if (!L.r) continue;
                for (let u = 2; u < L.r[2]; u += 5) L.px(u, 0, MA.shade(SHADE, 0.88), 1, L.r[3]);
            }
        }
        for (const f of SIDES) {
            const L = local('cape_hem', f);
            if (L.r) for (let u = 0; u < L.r[2]; u += 2) L.px(u, 1, GOLD);
        }
        // 权杖：杖身螺旋暗纹
        for (const f of SIDES) {
            const L = local('sceptre_shaft', f);
            if (!L.r) continue;
            for (let v = 0; v < L.r[3]; v += 4) L.px(0, v, MA.shade(BRONZE, 0.75), L.r[2], 1);
        }
        // 金鹰：翅膀与翼尖鳞状羽纹、尾羽竖纹、胸腹扇贝羽、后颈羽竖纹、眼（金黄虹膜 + 黑瞳，在头两侧）、眉骨压暗
        const scales = (g, faces, base) => {
            for (const f of faces) {
                const L = local(g, f);
                if (!L.r) continue;
                for (let v = 1; v < L.r[3]; v += 2) {
                    for (let u = (v % 4 === 1 ? 0 : 1); u < L.r[2]; u += 2) L.px(u, v, MA.shade(base, 0.78));
                }
            }
        };
        scales('eagle_wing', SIDES, BRONZE);
        scales('eagle_wing_tips', SIDES.concat(['top']), BRONZE);
        scales('eagle_chest', ['front', 'left', 'right'], GOLD);
        scales('eagle_belly', ['front', 'left', 'right'], GOLD);
        for (const f of ['top', 'back', 'bottom']) {
            const L = local('eagle_tail', f);
            if (L.r) for (let u = 1; u < L.r[2]; u += 2) L.px(u, 0, MA.shade(BRONZE, 0.7), 1, L.r[3]);
        }
        for (const f of SIDES) {
            const L = local('eagle_hackle', f);
            if (L.r) for (let u = 0; u < L.r[2]; u += 2) L.px(u, 0, MA.shade(GOLD, 0.8), 1, L.r[3]);
        }
        for (const f of ['left', 'right']) {
            const L = local('eagle_head', f);
            if (!L.r) continue;
            const u = f === 'left' ? 1 : L.r[2] - 3;
            L.px(u, 1, '#F0D060', 2, 1);
            L.px(f === 'left' ? u : u + 1, 1, '#2A2420');
        }
        for (const f of SIDES) {
            const L = local('eagle_brow', f);
            if (L.r) L.px(0, L.r[3] - 1, MA.shade(GOLD, 0.7), L.r[2], 1);
        }
        // 下身：从左胯斜到右膝的两道布褶阴影（取代 v2 的斜搭布块）
        {
            const L = local('skirt', 'front');
            if (L.r) {
                for (let v = 0; v < L.r[3]; v++) {
                    const u = Math.round(L.r[2] - 1 - v * 0.75);
                    L.px(u, v, MA.shade(SHADE, 0.84));
                    L.px(u - 6, v, MA.shade(SHADE, 0.9));
                }
            }
        }
        // 金腰带：中间一道暗线
        for (const f of SIDES) {
            const L = local('belt', f);
            if (L.r) L.px(0, Math.floor(L.r[3] / 2), MA.shade(GOLD, 0.78), L.r[2], 1);
        }
    };
    return { paint, EXTRA };
})();
