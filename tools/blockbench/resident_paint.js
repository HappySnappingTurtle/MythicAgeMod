// T19 希腊居民贴图细节（在底色之上绘制）。由 build_resident.js 调用，返回 {paint}。
// 辅助函数与祭司（greek_guardian_paint.js）一致：位置用设计卡坐标经 faceRect 换算，五官按像素图案左右镜像。
// 男女共用一张贴图：脸画在头的正面（男的嘴藏在胡须后面，女的露出）；肤色、服色由 resident_palettes.py 换色出变体。
(function () {
    const MA = window.MA;
    const SCLERA = '#F2EDE2';
    const PUPIL = '#2E2219';
    const BROW = '#2F231A';
    const MOUTH = '#6E3E2E';
    const LIP = '#8A4A3A';

    const hash = (x, y, k) => {
        let h = (x * 374761393 + y * 668265263 + k * 2147483647) | 0;
        h = Math.imul(h ^ (h >>> 13), 1274126177);
        return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
    };

    // 材质按底色识别（与 params/resident_parts.py 的色板一致）
    const MATERIAL = {
        '#AE7656': 'skin', '#3B2B20': 'hair', '#D6C7A6': 'cloth', '#BDAE8E': 'cloth', '#D2AC58': 'cloth',
        '#DDBB6C': 'cloth', '#B48F42': 'cloth', '#7C3A2F': 'wool', '#6B4A2E': 'cord', '#2F4F6E': 'cord',
        '#5A3B26': 'leather', '#A07A44': 'wicker', '#8A6638': 'wicker', '#7A5A36': 'wood', '#E6E1D6': 'marble',
        '#9C8B7C': 'clay', '#4E5A2C': 'olive', '#8A6A44': 'wood', '#5E4630': 'wood', '#6E5236': 'wood', '#9C7A4E': 'wood',
        '#8E9196': 'metal', '#B0813A': 'metal',
    };

    function makeHelpers(ctx, nets, P) {
        const local = (group, face) => {
            const net = nets[group];
            const r = net ? net.faces[face] : null;
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
        return { local, modulate };
    }

    const SIDES = ['left', 'front', 'right', 'back'];

    function paint(ctx, nets, P) {
        const { local, modulate } = makeHelpers(ctx, nets, P);

        // ── 材质颗粒 ──
        let k = 0;
        for (const [group, net] of Object.entries(nets)) {
            const mat = MATERIAL[net.color.toUpperCase()];
            for (const [face, r] of Object.entries(net.faces)) {
                k++;
                const seed = k;
                const vertical = face !== 'top' && face !== 'bottom';
                if (mat === 'skin') {
                    modulate(r, (u, v) => 0.97 + 0.06 * hash(u, v, seed));
                } else if (mat === 'cloth') {
                    // 竖向布纹：每 3 列一道浅褶影，加极轻的织物噪点
                    modulate(r, (u, v) => (vertical && u % 3 === 2 ? 0.92 : 1.0) * (0.97 + 0.05 * hash(u, v, seed)));
                } else if (mat === 'wool') {
                    // 披巾：粗毛呢，横竖交织的明暗
                    modulate(r, (u, v) => ((u + v) % 2 === 0 ? 1.03 : 0.95) * (0.95 + 0.08 * hash(u, v, seed)));
                } else if (mat === 'hair') {
                    modulate(r, (u, v) => 0.85 + 0.22 * hash(u, 0, seed) + 0.06 * hash(u, v, seed + 3));
                } else if (mat === 'cord') {
                    modulate(r, (u, v) => ((u + v) % 3 === 0 ? 0.78 : 1.0) * (0.95 + 0.08 * hash(u, v, seed)));
                } else if (mat === 'leather') {
                    modulate(r, (u, v) => 0.9 + 0.15 * hash(u, v, seed));
                } else if (mat === 'wicker') {
                    // 编篮：横向篾条，隔行错开的竖向压条
                    modulate(r, (u, v) => (v % 2 === 1 ? 0.8 : 1.05) * ((u + (v >> 1)) % 3 === 0 ? 0.88 : 1.0));
                } else if (mat === 'wood') {
                    modulate(r, (u, v) => (vertical && face !== 'left' && face !== 'right' ? (v % 3 === 0 ? 0.82 : 1.0) : 1.0)
                        * (0.92 + 0.12 * hash(u, v, seed)));
                } else if (mat === 'metal') {
                    // 金属：顶边一道亮、零星暗斑
                    modulate(r, (u, v) => (v === 0 && vertical ? 1.18 : 1.0) * (0.9 + 0.14 * hash(u, v, seed)));
                } else if (mat === 'marble') {
                    modulate(r, (u, v) => 0.97 + 0.05 * hash(u, v, seed) - ((u * 2 + v) % 9 === 0 ? 0.08 : 0));
                } else if (mat === 'clay') {
                    modulate(r, (u, v) => 0.9 + 0.16 * hash(u, v, seed));
                }
            }
            void group;
        }

        // ── 原木两端：年轮 ──
        for (const f of ['left', 'right']) {
            const L = local('wood_log', f);
            if (!L.r) continue;
            L.px(0, 0, '#B89466', L.r[2], L.r[3]);
            L.px(1, 1, '#A07F55', L.r[2] - 2, L.r[3] - 2);
            L.px(2, 2, '#B89466', Math.max(1, L.r[2] - 4), Math.max(1, L.r[3] - 4));
        }
        // ── 篮里的橄榄：一颗颗深色果 ──
        {
            const L = local('basket_olives', 'surface');
            if (L.r) {
                for (let v = 0; v < L.r[3]; v++) {
                    for (let u = 0; u < L.r[2]; u++) {
                        const h = hash(u, v, 77);
                        if (h < 0.35) L.px(u, v, '#2E3518');
                        else if (h > 0.9) L.px(u, v, '#6B7A3A');
                    }
                }
            }
        }

        // ── 袍边：男装短希顿下摆一道深色窄边；女装长袍下摆与折返下缘是织出来的深色带，中间一行回纹点 ──
        const band = (group, rows, color, dot) => {
            for (const f of SIDES) {
                const L = local(group, f);
                if (!L.r) continue;
                L.px(0, L.r[3] - rows, color, L.r[2], rows);
                if (dot) {
                    for (let u = 1; u < L.r[2]; u += 3) L.px(u, L.r[3] - Math.ceil(rows / 2), dot);
                }
            }
        };
        band('m_hem', 1, '#9A8B6C');
        band('f_hem', 3, '#6A2A20', '#E9D9A6');
        band('f_overfold', 2, '#6A2A20', '#E9D9A6');
        band('f_sleeve', 1, '#6A2A20');
        // 披巾：两侧与下缘一道深色边
        for (const f of ['front', 'back']) {
            const L = local('f_shawl_back', f);
            if (!L.r) continue;
            L.px(0, L.r[3] - 2, '#55261F', L.r[2], 2);
        }
        for (const f of ['front', 'back']) {
            const L = local('f_shawl_drape', f);
            if (!L.r) continue;
            L.px(0, 0, '#55261F', 1, L.r[3]);
            L.px(L.r[2] - 1, 0, '#55261F', 1, L.r[3]);
        }
        // 发带：两道细金线
        for (const f of SIDES) {
            const L = local('f_fillet', f);
            if (!L.r) continue;
            for (let u = 0; u < L.r[2]; u += 3) L.px(u, 0, '#C9A13B');
        }
        // 发髻：一圈盘绕的发缕
        for (const f of SIDES) {
            const L = local('f_bun', f);
            if (!L.r) continue;
            for (let v = 1; v < L.r[3]; v += 2) L.px(0, v, MA.shade('#3B2B20', 0.75), L.r[2], 1);
        }
        // 男：卷发额前一排小卷（隔像素压暗）
        {
            const L = local('m_hair_fringe', 'front');
            if (L.r) for (let u = 0; u < L.r[2]; u += 2) L.px(u, L.r[3] - 1, MA.shade('#3B2B20', 0.7));
        }

        // ── 手：正面三道指缝 ──
        {
            const seam = MA.shade('#AE7656', 0.72);
            const L = local('hand', 'front');
            for (const u of [1, 3, 4]) L.px(u, L.r[3] - 2, seam, 1, 2);
        }
        // ── 凉鞋：鞋底侧面一道缝线 ──
        for (const f of SIDES) {
            const L = local('sole', f);
            if (!L.r) continue;
            for (let u = 0; u < L.r[2]; u += 2) L.px(u, 0, MA.shade('#5A3B26', 1.3));
        }
        // ── 膝：上缘一行暗（与下摆的阴影衔接）──
        for (const f of SIDES) {
            const L = local('knee', f);
            if (L.r) L.px(0, 0, MA.shade('#AE7656', 0.85), L.r[2], 1);
        }

        // ── 五官：头正面 16×16 像素，按像素画、严格左右镜像。图案写画面右半边（角色左侧），mirror 生成另一半 ──
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
        // 眉、眼（眼白—瞳孔—眼白，瞳孔居中平视）
        mirrorPaint('head', [
            at([10, 11], 'B'),
            at([12], 'B'),
            at([10], 'W').concat(at([11], 'D'), at([12], 'W')),
            at([10], 'W').concat(at([11], 'D'), at([12], 'W')),
        ], { W: SCLERA, D: PUPIL, B: BROW }, 4);
        // 嘴（女装露出；男装被胡须挡住）：鼻子下方居中 4 像素，上唇一行浅
        mirrorPaint('head', [at([7, 6], 'm')], { m: MOUTH }, 15);
        mirrorPaint('head', [at([7], 'l')], { l: LIP }, 14);
        // 男：胡须前片 12 像素宽，八字须 + 嘴线居中
        mirrorPaint('m_beard_front', [
            at([8, 9, 10], 'M'),
            at([6, 7], 'm'),
        ], { M: MA.shade('#3B2B20', 0.8), m: MOUTH }, 0);
        // 腮红（极淡，两颊对称）
        mirrorPaint('head', [at([12, 13], 'c')], { c: MA.shade('#AE7656', 1.06) }, 10);
    }

    return { paint };
})();
