// T7 弥诺陶洛斯贴图细节（在白模底色之上绘制）。由 build_minotaur.js 调用，返回 {paint, glow}：
//   paint(ctx, nets, P)：五官、纹路、材质颗粒，画在主贴图上；
//   glow(ctx, nets, P)：自发光遮罩（GeckoLib AutoGlowingGeoLayer 读取 <贴图>_glowmask.png），只含红眼。
// 位置用设计卡坐标（+X 角色左、+Y 上、+Z 前，单位 u）书写，经 faceRect 换算到具体面的像素，
// 所以部件尺寸调整后细节会跟着走。
(function () {
    const MA = window.MA;

    // 材质按底色识别（与 params/minotaur_parts.py 的色板一致）
    const MATERIAL = {
        '#865632': 'fur', '#6E4428': 'fur', '#47362D': 'mane', '#CAB38C': 'ivory', '#3A2E26': 'horn_tip',
        '#42382E': 'hoof', '#8C7A45': 'bronze', '#C9A13B': 'gold', '#D8B24A': 'gold', '#5A3A22': 'wood',
        '#A95135': 'cloth', '#5A3B26': 'cloth', '#8C8478': 'stone',
    };
    const EYE_RED = '#C8261C';
    const EYE_HOT = '#FF7B4F';
    const EYE_DIM = '#8E1A13';
    const LID = '#2A1A11';
    const LID_SOFT = MA.shade('#865632', 0.62);

    // 一只眼（画面右侧那只，即角色左眼；第 0 列靠鼻梁）。外侧眼睑高、内眼角低，形成怒视的斜线。
    //   k 浅眼睑阴影  K 深眼睑  R 红  H 亮点  D 暗红
    const EYE = ['...kK', 'KKRRR', '.RRHR', '..DDD'];
    const EYE_COLORS = { k: LID_SOFT, K: LID, R: EYE_RED, H: EYE_HOT, D: EYE_DIM };
    const EYE_TOP_ROW = 4; // 颅前面第 4 行（眉骨下缘下方），平视时不被鼻吻挡住
    const EYE_INNER_COL = 11; // 画面右眼内眼角所在列；左眼按面宽镜像（两眼之间留 4 像素皮毛）

    /** 在颅前面画两只眼；glowOnly 时只画会发光的红色像素（自发光遮罩）。 */
    function drawEyes(L, glowOnly) {
        const w = L.r[2];
        EYE.forEach((row, j) => {
            for (let i = 0; i < row.length; i++) {
                const ch = row[i];
                if (ch === '.' || (glowOnly && 'RHD'.indexOf(ch) < 0)) continue;
                const color = EYE_COLORS[ch];
                L.px(EYE_INNER_COL + i, EYE_TOP_ROW + j, color);
                L.px(w - 1 - (EYE_INNER_COL + i), EYE_TOP_ROW + j, color);
            }
        });
    }

    // 可复现的像素噪声（不依赖 Math.random，重跑结果一致）
    const hash = (x, y, k) => {
        let h = (x * 374761393 + y * 668265263 + k * 2147483647) | 0;
        h = Math.imul(h ^ (h >>> 13), 1274126177);
        return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
    };

    function makeHelpers(ctx, nets, P) {
        const tpu = P.texels_per_unit;
        const parts = Object.fromEntries(P.parts.concat(P.extra_parts).map((p) => [p.id, p]));
        /** 部件某个面上，卡片坐标矩形 [x0,y0]–[x1,y1]（侧面用 z 代替 x）对应的像素矩形 [u, v, w, h]。 */
        function faceRect(partId, face, a0, b0, a1, b1) {
            const part = parts[partId];
            const net = nets[part.uv_group];
            const r = net.faces[face];
            const [cx, cy, cz] = part.center_model_units;
            const [sx, sy, sz] = part.size_model_units;
            const ymax = cy + sy / 2;
            let u0;
            let u1;
            if (face === 'front') {
                u0 = (a0 - (cx - sx / 2)) * tpu;
                u1 = (a1 - (cx - sx / 2)) * tpu;
            } else if (face === 'back') {
                u0 = (cx + sx / 2 - a1) * tpu;
                u1 = (cx + sx / 2 - a0) * tpu;
            } else if (face === 'left') {
                u0 = (cz + sz / 2 - a1) * tpu;
                u1 = (cz + sz / 2 - a0) * tpu;
            } else if (face === 'right') {
                u0 = (a0 - (cz - sz / 2)) * tpu;
                u1 = (a1 - (cz - sz / 2)) * tpu;
            } else {
                throw new Error('faceRect 不支持面 ' + face);
            }
            const v0 = (ymax - Math.max(b0, b1)) * tpu;
            const v1 = (ymax - Math.min(b0, b1)) * tpu;
            const x = Math.round(r[0] + Math.max(0, Math.min(u0, u1)));
            const y = Math.round(r[1] + Math.max(0, v0));
            const w = Math.max(1, Math.round(Math.abs(u1 - u0)));
            const h = Math.max(1, Math.round(v1 - v0));
            return [x, y, Math.min(w, r[0] + r[2] - x), Math.min(h, r[1] + r[3] - y)];
        }
        const fill = (rect, color) => {
            ctx.fillStyle = color;
            ctx.fillRect(rect[0], rect[1], rect[2], rect[3]);
        };
        /** 在某个 UV 组某个面的像素坐标系（左上为原点）里画。 */
        const local = (group, face) => {
            const r = nets[group].faces[face];
            return {
                r,
                px: (u, v, color, w = 1, h = 1) => {
                    if (u < 0 || v < 0 || u >= r[2] || v >= r[3]) return;
                    ctx.fillStyle = color;
                    ctx.fillRect(r[0] + u, r[1] + v, Math.min(w, r[2] - u), Math.min(h, r[3] - v));
                },
            };
        };
        /** 把一个面的像素逐个调亮/调暗：fn(u, v, w, h) 返回系数。 */
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
        return { faceRect, fill, local, modulate, parts };
    }

    const SIDES = ['left', 'front', 'right', 'back'];

    function paint(ctx, nets, P) {
        const H = makeHelpers(ctx, nets, P);
        const { faceRect, fill, local, modulate } = H;

        // ── 材质颗粒：皮毛细噪点，鬃毛竖向发丝，布料竖向褶纹，木柄纵纹，金属轻微斑驳 ──
        let k = 0;
        for (const net of Object.values(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k;
                if (mat === 'fur') {
                    modulate(r, (u, v) => 0.95 + 0.1 * hash(u, v, seed));
                } else if (mat === 'mane') {
                    modulate(r, (u, v) => 0.86 + 0.2 * hash(u, 0, seed) + 0.06 * hash(u, v, seed + 7));
                } else if (mat === 'cloth') {
                    modulate(r, (u) => (u % 3 === 2 ? 0.88 : 1.0));
                } else if (mat === 'wood') {
                    modulate(r, (u, v) => 0.9 + 0.16 * hash(u, Math.floor(v / 5), seed));
                } else if (mat === 'gold' || mat === 'bronze') {
                    modulate(r, (u, v) => 0.95 + 0.08 * hash(u, v, seed));
                } else if (mat === 'ivory') {
                    modulate(r, (u, v) => 0.96 + 0.06 * hash(u, v, seed));
                }
            }
        }

        // ── 眼：小眼型、无眼框、两眼之间不连线（避免像戴眼镜）──
        drawEyes(local('cranium', 'front'), false);

        // ── 鼻吻：鼻孔、鼻头上缘高光、嘴缝 ──
        for (const s of [1, -1]) {
            const x0 = s > 0 ? 1.3 : -2.8;
            fill(faceRect('nose', 'front', x0, 34.6, x0 + 1.5, 33.4), '#2E1E16');
            fill(faceRect('nose', 'front', x0, 35.0, x0 + 1.5, 34.6), MA.shade('#CAB38C', 0.8));
        }
        fill(faceRect('nose', 'front', -3.5, 35.2, 3.5, 34.9), MA.shade('#CAB38C', 1.08));
        fill(faceRect('jaw', 'front', -3.5, 32.5, 3.5, 32.0), '#4C3426');
        fill(faceRect('muzzle', 'front', -3.4, 32.75, 3.4, 32.25), MA.shade('#CAB38C', 0.78));

        // ── 耳：正面画浅色内耳（左右共用 UV）──
        {
            const L = local('ear', 'front');
            L.px(1, 1, '#B98466', Math.max(1, L.r[2] - 3), Math.max(1, L.r[3] - 2));
        }

        // ── 胸：胸肌中缝与下缘阴影；腹部中线 ──
        fill(faceRect('pecs', 'front', -0.25, 29.5, 0.25, 26.0), MA.shade('#865632', 0.7));
        fill(faceRect('pecs', 'front', -6.5, 26.5, 6.5, 26.0), MA.shade('#865632', 0.75));
        fill(faceRect('abdomen', 'front', -0.25, 25.5, 0.25, 22.5), MA.shade('#865632', 0.8));

        // ── 左拳（握斧）：握孔沿卡片 Z 轴，斧柄从拇指一侧（前面）进、小指一侧（后面）出 ──
        // 掌心一侧（解剖右面，朝身体）画四根并排卷曲的手指；前后两面画食指、小指卷成的环，环心压暗。
        const knuckle = MA.shade('#865632', 0.6);
        {
            const ring = MA.shade('#865632', 0.55);
            const hole = '#3A2418';
            for (const z of [-1.2, 0.3, 1.8]) {
                fill(faceRect('hand_l', 'right', z, 13.6, z + 0.5, 10.0), knuckle);
            }
            fill(faceRect('hand_l', 'right', -2.7, 11.0, 3.3, 10.5), knuckle);
            for (const f of ['front', 'back']) {
                const a0 = 8.6;
                fill(faceRect('hand_l', f, a0, 13.6, a0 + 3, 13.1), ring);
                fill(faceRect('hand_l', f, a0, 10.9, a0 + 3, 10.4), ring);
                fill(faceRect('hand_l', f, a0, 13.6, a0 + 0.5, 10.4), ring);
                fill(faceRect('hand_l', f, a0 + 2.5, 13.6, a0 + 3, 10.4), ring);
                fill(faceRect('hand_l', f, a0 + 0.5, 13.1, a0 + 2.5, 10.9), hole);
            }
            fill(faceRect('hand_l', 'left', -2.7, 10.4, 3.3, 9.9), MA.shade('#865632', 1.15));
            for (const z of [-1.2, 0.3, 1.8]) {
                fill(faceRect('hand_l', 'left', z, 10.9, z + 0.5, 9.7), knuckle);
            }
        }
        // ── 右拳（空拳）：掌心一侧（解剖左面）四指，前面一道食指折痕，手背底缘指节 ──
        {
            for (const z of [-1.2, 0.3, 1.8]) {
                fill(faceRect('hand_r', 'left', z, 13.0, z + 0.5, 9.7), knuckle);
            }
            fill(faceRect('hand_r', 'front', -12.6, 11.6, -7.6, 11.1), knuckle);
            fill(faceRect('hand_r', 'right', -2.7, 10.4, 3.3, 9.9), MA.shade('#865632', 1.15));
            for (const z of [-1.2, 0.3, 1.8]) {
                fill(faceRect('hand_r', 'right', z, 10.9, z + 0.5, 9.7), knuckle);
            }
        }

        // ── 牛角环纹（角根、中段的四个侧面，每隔 3 像素一道）、角尖亮点 ──
        for (const g of ['horn_base', 'horn_mid']) {
            for (const f of SIDES) {
                const L = local(g, f);
                for (let v = 2; v < L.r[3]; v += 3) {
                    L.px(0, v, MA.shade('#CAB38C', 0.78), L.r[2], 1);
                }
            }
        }

        // ── 护腕：上下包边亮、中间一排铆钉 ──
        for (const f of SIDES) {
            const L = local('bracer', f);
            L.px(0, 0, MA.shade('#8C7A45', 1.25), L.r[2], 1);
            L.px(0, L.r[3] - 1, MA.shade('#8C7A45', 0.7), L.r[2], 1);
            for (let u = 1; u < L.r[2]; u += 3) {
                L.px(u, Math.floor(L.r[3] / 2), MA.shade('#C9A13B', 1.1));
            }
        }
        // 腰带：铆钉；带扣：中心凹槽
        for (const f of SIDES) {
            const L = local('belt', f);
            for (let u = 1; u < L.r[2]; u += 4) {
                L.px(u, 1, MA.shade('#C9A13B', 1.05), 1, 2);
            }
        }
        {
            const L = local('belt_buckle', 'front');
            L.px(2, 2, MA.shade('#C9A13B', 0.6), L.r[2] - 4, L.r[3] - 4);
        }

        // ── 铜章：外框、内亮框、中心双斧纹（前后共用）──
        {
            const L = local('medallion', 'front');
            const w = L.r[2];
            const h = L.r[3];
            L.px(0, 0, MA.shade('#C9A13B', 0.7), w, 1);
            L.px(0, h - 1, MA.shade('#C9A13B', 0.7), w, 1);
            L.px(0, 0, MA.shade('#C9A13B', 0.7), 1, h);
            L.px(w - 1, 0, MA.shade('#C9A13B', 0.7), 1, h);
            L.px(1, 1, MA.shade('#C9A13B', 1.25), w - 2, 1);
            // 拉布里斯双斧：上半是左右对称的蝶形双刃，下半是斧柄
            const LABRYS = ['#.##.#', '######', '#.##.#', '..##..', '..##..', '..##..'];
            const ox = Math.floor((w - 6) / 2);
            const oy = Math.floor((h - 6) / 2);
            LABRYS.forEach((row, j) => {
                for (let i = 0; i < 6; i++) {
                    if (row[i] === '#') L.px(ox + i, oy + j, '#6B5320');
                }
            });
        }

        // ── 腰布前片：两侧与下缘深边，两道金线，回纹（希腊方形螺旋）──
        {
            const L = local('loincloth', 'surface');
            const w = L.r[2];
            const h = L.r[3];
            const dark = MA.shade('#A95135', 0.7);
            L.px(0, 0, dark, 1, h);
            L.px(w - 1, 0, dark, 1, h);
            L.px(0, h - 1, dark, w, 1);
            const gold = '#D2A845';
            L.px(1, h - 11, gold, w - 2, 1);
            L.px(1, h - 3, gold, w - 2, 1);
            const KEY = ['1111111', '0000001', '1111101', '1000101', '1011101', '1000001', '1111111'];
            const ox = Math.floor((w - 7) / 2);
            const oy = h - 10;
            KEY.forEach((row, j) => {
                for (let i = 0; i < 7; i++) {
                    if (row[i] === '1') L.px(ox + i, oy + j, gold);
                }
            });
        }
        {
            const L = local('loincloth_back', 'surface');
            L.px(0, L.r[3] - 2, '#D2A845', L.r[2], 1);
        }

        // ── 蹄：上缘一道亮线，读出角质 ──
        for (const f of SIDES) {
            const L = local('hoof_toe', f);
            L.px(0, 0, MA.shade('#42382E', 1.35), L.r[2], 1);
        }

        // ── 斧：刃口亮边、刃身靠柄一侧暗线 ──
        for (const f of ['left', 'right']) {
            const E = local('axe_blade_edge', f);
            E.px(0, 0, '#F2D27A', E.r[2], 1);
            const B = local('axe_blade', f);
            B.px(0, 0, MA.shade('#D8B24A', 0.75), B.r[2], 1);
        }
    }

    /**
     * Boss 变体（T23 §8；卡片 03 §4「略深皮毛、明显但克制的迷宫束带纹样、少量战损；不靠全身发红发光区分身份」）：
     * 在召唤版贴图上改色——皮毛与鬃毛压暗偏黑褐、腰布换深绯红、青铜略旧；腰带与护腕铺一条金色回纹（迷宫纹样）；
     * 胸前与上臂几道浅色旧伤疤。眼睛与发光遮罩不变。
     */
    function boss(ctx, nets, P) {
        const H = makeHelpers(ctx, nets, P);
        const { local } = H;
        const tint = (r, m) => {
            const img = ctx.getImageData(r[0], r[1], r[2], r[3]);
            for (let i = 0; i < img.data.length; i += 4) {
                for (let c = 0; c < 3; c++) {
                    img.data[i + c] = Math.max(0, Math.min(255, Math.round(img.data[i + c] * m[c])));
                }
            }
            ctx.putImageData(img, r[0], r[1]);
        };
        const TINT = {
            fur: [0.66, 0.6, 0.6], mane: [0.6, 0.56, 0.56], cloth: [0.86, 0.55, 0.6], bronze: [0.86, 0.82, 0.76],
            ivory: [0.93, 0.9, 0.84], hoof: [0.8, 0.78, 0.78],
        };
        for (const net of Object.values(nets)) {
            const m = TINT[MATERIAL[net.color.toUpperCase()]];
            if (!m) continue;
            for (const r of Object.values(net.faces)) {
                tint(r, m);
            }
        }
        // 回纹：每 6 列一个方形螺旋单元，4 行高（腰带正好 4 行；护腕取中间 4 行）
        const KEY = ['111111', '000001', '111101', '100001'];
        const gold = '#C9A13B';
        const dark = '#3B2C16';
        const meander = (L, top) => {
            for (let v = 0; v < 4; v++) {
                for (let u = 0; u < L.r[2]; u++) {
                    L.px(u, top + v, KEY[v][u % 6] === '1' ? gold : dark);
                }
            }
        };
        for (const f of SIDES) {
            meander(local('belt', f), 0);
            const B = local('bracer', f);
            meander(B, Math.max(0, Math.floor((B.r[3] - 4) / 2)));
        }
        // 旧伤疤：浅色斜线（爪痕三道、刀痕一道）
        const scar = MA.shade('#865632', 0.95);
        const line = (L, u0, v0, n, du) => {
            for (let i = 0; i < n; i++) L.px(u0 + i * du, v0 + i, scar);
        };
        {
            const C = local('chest', 'front');
            line(C, 3, 2, 6, 1);
            line(C, 5, 2, 6, 1);
            line(C, 7, 2, 5, 1);
            line(C, C.r[2] - 6, 5, 5, -1);
        }
        {
            const U = local('upper_arm', 'front');
            line(U, 2, 3, 5, 1);
        }
    }

    function glow(ctx, nets, P) {
        const { local } = makeHelpers(ctx, nets, P);
        drawEyes(local('cranium', 'front'), true);
    }

    return { paint, glow, boss };
})();
