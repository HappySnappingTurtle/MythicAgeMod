// T9b 神庙祭司贴图细节（在底色之上绘制）。由 build_greek_guardian.js 调用，返回 {paint}。
// 位置用设计卡坐标（+X 角色左、+Y 上、+Z 前，单位 u）书写，经 faceRect 换算到面的像素，部件改尺寸后细节跟着走。
// 白模 v1 画身份必需的五官、紫边、金果；贴图 v1 加材质颗粒与细节（布纹、发丝、珠线、绞纹、碗瓣纹等）。
(function () {
    const MA = window.MA;
    const PURPLE = '#6B2A78';
    const PURPLE_DARK = '#4E1C59';
    const SCLERA = '#F2EDE2';
    const PUPIL = '#33271E';
    const BROW = '#6F695F';
    const MOUSTACHE = '#8A8276';
    const MOUTH = '#5A3C2C';
    const GOLD = '#D9B44A';

    // 可复现的像素噪声（不依赖 Math.random，重跑结果一致）
    const hash = (x, y, k) => {
        let h = (x * 374761393 + y * 668265263 + k * 2147483647) | 0;
        h = Math.imul(h ^ (h >>> 13), 1274126177);
        return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
    };

    // 材质按底色识别（与 params/greek_guardian_parts.py 的色板一致）
    const MATERIAL = {
        '#AC7859': 'skin', '#E9E2CF': 'cloth', '#E4DCC6': 'cloth', '#D3CAB2': 'cloth', '#6B2A78': 'purple',
        '#8E877D': 'hair', '#A79F92': 'hair', '#5B7A36': 'leaf', '#6E8F3F': 'leaf', '#C9A13B': 'metal',
        '#B0813A': 'metal', '#5A3B26': 'leather', '#6A1E2A': 'wine',
    };

    function makeHelpers(ctx, nets, P) {
        const tpu = P.texels_per_unit;
        const parts = Object.fromEntries(P.parts.concat(P.extra_parts).map((p) => [p.id, p]));
        /** 部件某个侧面上，卡片坐标矩形（a 为横向：前后面用 x，左右面用 z；b 为 y）对应的像素矩形。 */
        function faceRect(partId, face, a0, b0, a1, b1) {
            const part = parts[partId];
            const r = nets[part.uv_group].faces[face];
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
        /** 某个 UV 组某个面的像素坐标系（左上为原点）。 */
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
        /** 把一个矩形逐像素调亮/调暗：fn(u, v, w, h) 返回系数。 */
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

    /** 一个 UV 组四个侧面的底部若干像素行涂成某色（袍边、袖边）。 */
    function bottomBand(H, group, rows, color) {
        for (const f of SIDES) {
            const L = H.local(group, f);
            if (!L.r) continue;
            L.px(0, L.r[3] - rows, color, L.r[2], rows);
        }
    }

    function paint(ctx, nets, P) {
        const H = makeHelpers(ctx, nets, P);
        const { faceRect, fill, local, modulate } = H;

        // ── 材质颗粒（T9b 贴图 v1）──
        let k = 0;
        for (const net of Object.values(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'skin') {
                    modulate(r, (u, v) => 0.97 + 0.06 * hash(u, v, seed));
                } else if (mat === 'cloth') {
                    // 竖向布纹：每 4 列一道浅褶影，加极轻的织物噪点
                    modulate(r, (u, v) => (vertical && u % 4 === 3 ? 0.93 : 1.0) * (0.98 + 0.04 * hash(u, v, seed)));
                } else if (mat === 'purple') {
                    modulate(r, (u, v) => 0.92 + 0.14 * hash(u, v, seed));
                } else if (mat === 'hair') {
                    // 发丝：按列变化明暗，竖向成缕
                    modulate(r, (u, v) => 0.88 + 0.18 * hash(u, 0, seed) + 0.05 * hash(u, v, seed + 3));
                } else if (mat === 'leaf') {
                    modulate(r, (u, v) => 0.9 + 0.2 * hash(u, v, seed));
                } else if (mat === 'metal') {
                    modulate(r, (u, v) => (v === 0 && vertical ? 1.15 : 1.0) * (0.93 + 0.12 * hash(u, v, seed)));
                } else if (mat === 'leather') {
                    modulate(r, (u, v) => 0.9 + 0.15 * hash(u, v, seed));
                }
            }
        }

        // ── 金色珠线：下摆紫边、袖口紫边、披风底边 ──
        for (const g of ['hem_band', 'sleeve_band']) {
            for (const f of SIDES) {
                const L = local(g, f);
                if (!L.r) continue;
                for (let u = 1; u < L.r[2]; u += 3) L.px(u, L.r[3] - 1, GOLD);
            }
        }
        for (const f of ['front', 'back']) {
            const L = local('mantle', f);
            for (let u = 2; u < L.r[2] - 2; u += 3) L.px(u, L.r[3] - 3, GOLD);
        }
        // ── 紫腰绳：斜向绞纹 ──
        for (const f of SIDES) {
            const L = local('girdle', f);
            for (let v = 0; v < L.r[3]; v++) {
                for (let u = 0; u < L.r[2]; u++) {
                    if ((u + v) % 3 === 0) L.px(u, v, PURPLE_DARK);
                }
            }
        }
        // ── 须发末梢：后发与胡须下缘参差，末梢更浅 ──
        for (const [g, f] of [['hair_back', 'back'], ['beard_chin', 'front'], ['beard_front', 'front'], ['beard_side', 'left'], ['beard_side', 'right']]) {
            const L = local(g, f);
            // 参差：随机一部分像素压暗（不做一亮一暗交替，那样像梳齿）
            for (let u = 0; u < L.r[2]; u++) {
                if (hash(u, 1, L.r[0] + L.r[1]) < 0.45) L.px(u, L.r[3] - 1, MA.shade('#8E877D', 0.82));
            }
        }
        // ── 月桂叶尖提亮 ──
        for (const g of ['laurel_leaf', 'laurel_leaf_front']) {
            for (const f of SIDES) {
                const L = local(g, f);
                if (L.r) L.px(L.r[2] - 1, 0, MA.shade('#6E8F3F', 1.25));
            }
        }
        // ── 手：空手正面三道指缝；托碗的手顶面（掌心朝上）三道指缝 ──
        {
            const seam = MA.shade('#AC7859', 0.72);
            const open = local('hand_open', 'front');
            for (const u of [1, 3, 4]) open.px(u, open.r[3] - 2, seam, 1, 2);
            const cup = local('hand_cup', 'top');
            for (const u of [1, 3, 4]) cup.px(u, 0, seam, 1, 2);
        }
        // ── 奠酒碗：外壁竖向瓣纹，碗沿顶面提亮，酒面两点高光 ──
        for (const g of ['phiale_wall_x', 'phiale_wall_z']) {
            for (const f of SIDES) {
                const L = local(g, f);
                if (!L.r) continue;
                for (let u = 0; u < L.r[2]; u += 2) L.px(u, 0, MA.shade('#B0813A', 0.78), 1, L.r[3]);
            }
            const T = local(g, 'top');
            T.px(0, 0, MA.shade('#B0813A', 1.25), T.r[2], 1);
        }
        {
            const W = local('phiale_wine', 'surface');
            W.px(1, 1, '#9A3341', 2, 1);
            W.px(4, 4, '#8A2C38');
        }
        // ── 凉鞋：鞋底侧面一道缝线 ──
        for (const f of SIDES) {
            const L = local('sole', f);
            for (let u = 0; u < L.r[2]; u += 2) L.px(u, 0, MA.shade('#5A3B26', 1.3));
        }
        // ── 额头两道浅皱纹（对称）──
        {
            const L = local('head', 'front');
            const wr = MA.shade('#AC7859', 0.86);
            for (const [u, v] of [[5, 1], [6, 1], [7, 1], [6, 2], [7, 2]]) {
                L.px(u, v, wr);
                L.px(L.r[2] - 1 - u, v, wr);
            }
        }

        // ── 五官（T9b-R2）：按像素画、严格左右镜像（半格坐标换算到像素会左右取整不一致，导致口斜眼歪）。
        // 头前面 16×16 像素；图案写画面右半边（角色左侧），列号从 0 开始，mirror 生成另一半。
        //   W 眼白  D 瞳孔（居中，平视）  B 眉  b 眉梢下垂
        const mirrorPaint = (group, rows, colors, top) => {
            const L = local(group, 'front');
            rows.forEach((row, j) => {
                for (const [col, ch] of row) {
                    L.px(col, top + j, colors[ch]);
                    L.px(L.r[2] - 1 - col, top + j, colors[ch]);
                }
            });
        };
        const at = (cols, ch) => cols.map((c) => [c, ch]);
        mirrorPaint('head', [
            at([9, 10, 11, 12], 'B'),
            at([12, 13], 'b'),
            at([10], 'W').concat(at([11], 'D'), at([12], 'W')),
            at([10], 'W').concat(at([11], 'D'), at([12], 'W')),
        ], { W: SCLERA, D: PUPIL, B: BROW, b: BROW }, 4);
        // 胡须前片 14 像素宽（偶数，嘴正好居中）：八字须在鼻子两侧向外下垂，嘴线 4 像素居中
        mirrorPaint('beard_front', [
            at([9, 10, 11], 'M'),
            at([11, 12], 'M'),
            at([5, 6], 'm'),
        ], { M: MOUSTACHE, m: MOUTH }, 1);
        // 胡须竖纹（每隔 2 像素一道浅阴影）
        for (const g of ['beard_front', 'beard_chin']) {
            const L = local(g, 'front');
            for (let u = 1; u < L.r[2]; u += 2) {
                L.px(u, L.r[3] - 2, MA.shade('#A79F92', 0.85), 1, 2);
            }
        }

        // ── 紫边：胸前折返下缘、短袖已有紫边块；披风四周、肩搭布外缘、斜披带两侧 ──
        bottomBand(H, 'overfold', 1, PURPLE);
        for (const f of ['front', 'back']) {
            const L = local('mantle', f);
            L.px(0, 0, PURPLE, 1, L.r[3]);
            L.px(L.r[2] - 1, 0, PURPLE, 1, L.r[3]);
            L.px(0, L.r[3] - 2, PURPLE, L.r[2], 2);
        }
        for (const f of SIDES) {
            const L = local('mantle_shoulder', f);
            L.px(0, L.r[3] - 1, PURPLE, L.r[2], 1);
        }
        for (const f of ['front', 'back']) {
            const L = local('sash', f);
            L.px(0, 0, PURPLE, 1, L.r[3]);
            L.px(L.r[2] - 1, 0, PURPLE, 1, L.r[3]);
        }
        // 领口：长袍上身前面顶部中间一道浅 V 形紫边
        const neck = local('chiton', 'front');
        const mid = Math.floor(neck.r[2] / 2);
        for (let i = 0; i < 3; i++) {
            neck.px(mid - 3 + i, i, PURPLE);
            neck.px(mid + 2 - i, i, PURPLE);
        }
        // 下摆紫边上缘描一道深紫
        for (const f of SIDES) {
            const L = local('hem_band', f);
            if (L.r) L.px(0, 0, PURPLE_DARK, L.r[2], 1);
        }

        // ── 月桂：叶脉一道深绿，冠带上几颗金果 ──
        for (const g of ['laurel_leaf', 'laurel_leaf_front']) {
            for (const f of SIDES) {
                const L = local(g, f);
                if (L.r && L.r[2] >= 3) L.px(0, Math.floor(L.r[3] / 2), MA.shade('#6E8F3F', 0.7), L.r[2], 1);
            }
        }
        const band = local('wreath_side', 'left');
        for (const u of [3, 8, 13]) band.px(u, 0, GOLD);
        const back = local('wreath_back', 'back');
        for (const u of [4, 12]) back.px(u, 0, GOLD);
    }

    return { paint };
})();
