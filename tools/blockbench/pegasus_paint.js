// 天马贴图细节（在按面明暗的底色之上绘制）。由 build_pegasus.js 调用，返回 {paint, glow}。
// v1（2026-10-21）：
//   白色毛皮——成团的极淡灰蓝明暗（不逐像素杂点），腹下与腿内侧偏冷灰；
//   鬃与尾——竖向发缕、发梢亮；青铜蹄——锤痕明暗、蹄冠一道亮边；
//   翅膀——金色覆羽带画成两排圆鳞（古风卷翼的覆羽），白色飞羽画羽轴、羽片由轴向外渐暗、羽尖一点灰；
//   头——深褐眼珠 + 1 像素高光（按像素镜像，不加眼框），鼻端两个鼻孔、下唇口线；
//   眼睛发光层（_glowmask）只用于天眼：金色瞳光。
(function () {
    const MA = window.MA;
    const EYE = '#2a1c14';
    const EYE_HI = '#f4efe3';
    const NOSTRIL = '#6e605a';
    const MOUTH = '#8a7c76';
    const COOL = [196, 204, 214];

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

    // 材质按底色识别（与 params/pegasus_parts.py 的色板一致）
    const MATERIAL = {
        '#ECE9E2': 'coat', '#D9D8D4': 'coat', '#F4EFE3': 'hair', '#93693A': 'bronze', '#CFC6BF': 'skin',
        '#E6D6A8': 'covert', '#EFE4C4': 'covert', '#F6F4EF': 'feather',
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

    /** 眼睛：2×2 深褐眼珠，靠前上方一像素高光；左右两面按像素镜像（u → w-1-u）。 */
    function eye(L, mirror) {
        if (!L.r) return;
        const w = L.r[2];
        const h = L.r[3];
        const u0 = Math.max(1, Math.round(w * 0.32));
        const v0 = Math.max(1, Math.round(h * 0.3));
        const at = (u, v, c) => L.px(mirror ? w - 1 - u : u, v, c);
        at(u0, v0, EYE);
        at(u0 + 1, v0, EYE);
        at(u0, v0 + 1, EYE);
        at(u0 + 1, v0 + 1, EYE);
        at(u0 + 2, v0 + 1, MA.shade('#ece9e2', 0.8)); // 眼后一点阴影，让眼窝有深度
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
                if (mat === 'coat') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let c = scale(rgb, 0.97 + 0.06 * vnoise(X, Y, 4, seed));
                        if (face === 'bottom') c = mix(c, COOL, 0.35);
                        else if (vertical && h > 3) c = mix(c, COOL, 0.22 * (v / (h - 1)) ** 2);
                        return c;
                    });
                } else if (mat === 'hair') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const strand = 0.9 + 0.12 * hash(X, 0, seed);
                        const tip = vertical && v === h - 1 ? 0.9 : 1;
                        return scale(rgb, strand * tip * (0.97 + 0.05 * hash(X, Y, seed + 1)));
                    });
                } else if (mat === 'bronze') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        let f = 0.86 + 0.26 * vnoise(X, Y, 2, seed);
                        if (hash(X, Y, seed + 3) > 0.97) f *= 0.86;
                        if (vertical && v === 0) f *= 1.3; // 蹄冠亮边
                        return scale(rgb, f);
                    });
                } else if (mat === 'skin') {
                    recolor(r, (u, v, w, h, X, Y, rgb) => scale(rgb, 0.96 + 0.06 * vnoise(X, Y, 2, seed)));
                } else if (mat === 'covert') {
                    // 圆鳞覆羽：4×3 一片，下沿是 U 形暗弧、上沿受光；相邻两排错开半片
                    const TILE = [[1.0, 1.05, 1.05, 1.0], [0.93, 1.0, 1.0, 0.93], [0.97, 0.88, 0.88, 0.97]];
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const row = Math.floor(v / 3);
                        const uu = (u + (row % 2 ? 2 : 0)) % 4;
                        return scale(rgb, TILE[v % 3][uu] * (0.98 + 0.04 * hash(X, Y, seed)));
                    });
                } else if (mat === 'feather') {
                    // 羽片：沿长边的中线为羽轴（浅灰），向两侧略暗；最末两像素为羽尖（灰）
                    recolor(r, (u, v, w, h, X, Y, rgb) => {
                        const along = w >= h; // 长边是水平方向
                        const across = along ? v : u;
                        const n = along ? h : w;
                        const pos = along ? u : v;
                        const len = along ? w : h;
                        const mid = (n - 1) / 2;
                        let c = rgb;
                        if (n >= 3 && Math.abs(across - mid) < 0.6) c = scale(c, 0.86);
                        else c = scale(c, 1 - 0.05 * Math.abs(across - mid) / Math.max(1, mid));
                        if (len > 6 && (pos <= 1 || pos >= len - 2) && (face === 'top' || face === 'bottom')) {
                            // 羽尖在哪一端事先不知道：两端都画淡灰，远处看是一道细边
                            c = mix(c, [205, 205, 210], 0.35);
                        }
                        return c;
                    });
                }
            }
        }

        // ── 头：眼睛、鼻孔、口线 ──
        eye(local('cranium', 'left'), false);
        eye(local('cranium', 'right'), true);
        {
            const N = local('nose', 'front');
            if (N.r) {
                const w = N.r[2];
                const h = N.r[3];
                for (const u of [1, w - 3]) {
                    N.px(u, Math.floor(h * 0.3), NOSTRIL, 2, 2);
                    N.px(u, Math.floor(h * 0.3) + 2, MA.shade('#cfc6bf', 0.85), 2, 1);
                }
            }
            const M = local('lip', 'front');
            if (M.r) M.px(0, 0, MOUTH, M.r[2], 1);
            for (const f of ['left', 'right']) {
                const S = local('muzzle', f);
                if (S.r) S.px(0, S.r[3] - 2, MA.shade('#cfc6bf', 0.95), S.r[2], 2); // 吻部下沿过渡到灰皮
            }
        }
        // ── 鬐甲与鬃毛交界：鬃毛根部一道略深的线 ──
        all(local, nets, 'mane', (L, face) => {
            if (!L.r || face === 'top' || face === 'bottom') return;
            L.px(0, 0, MA.shade('#f4efe3', 0.88), L.r[2], 1);
        });
    }

    function all(local, nets, group, fn) {
        const n = nets[group];
        if (!n) return;
        for (const face of Object.keys(n.faces)) fn(local(group, face), face);
    }

    /** 天眼发光层：两只眼的位置画金色瞳光（其余透明）。 */
    function glow(ctx, nets) {
        const { local } = helpers(ctx, nets);
        for (const [f, mirror] of [['left', false], ['right', true]]) {
            const L = local('cranium', f);
            if (!L.r) continue;
            const w = L.r[2];
            const h = L.r[3];
            const u0 = Math.max(1, Math.round(w * 0.32));
            const v0 = Math.max(1, Math.round(h * 0.3));
            for (const [du, dv] of [[0, 0], [1, 0], [0, 1], [1, 1]]) {
                const u = u0 + du;
                L.px(mirror ? w - 1 - u : u, v0 + dv, '#ffd66b');
            }
        }
    }

    return { paint, glow };
})();
