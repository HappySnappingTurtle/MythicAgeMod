// Blockbench 构建脚本的公共函数。在 Blockbench 中通过 MCP risky_eval 执行：
//   window.MA_REQUIRE = require; (0, eval)(require('fs').readFileSync('<repo>/tools/blockbench/lib/bb.js', 'utf8'))
// （全局作用域里没有 require，所以先把 risky_eval 作用域中的 require 交给 window。）
// 之后用 MA.run('<相对路径>') 执行具体的构建脚本。坐标均为 Blockbench 编辑器坐标（单位 u，16u = 1 格）；
// 设计卡坐标到 Blockbench 坐标的换算只在 axes.js 中进行。
(function () {
    const req = window.MA_REQUIRE;
    const fs = req('fs');
    const path = req('path');
    const crypto = req('crypto');
    const REPO = window.MA_REPO || '/Users/hongyuwu/Documents/MythicAgeMod';

    const MA = (window.MA = window.MA || {});
    MA.REPO = REPO;
    MA.fs = fs;
    MA.path = path;

    MA.abs = (rel) => (path.isAbsolute(rel) ? rel : path.join(REPO, rel));
    MA.readJson = (rel) => JSON.parse(fs.readFileSync(MA.abs(rel), 'utf8'));
    MA.writeText = (rel, text) => {
        const p = MA.abs(rel);
        fs.mkdirSync(path.dirname(p), { recursive: true });
        fs.writeFileSync(p, text);
        return p;
    };
    MA.sha256 = (text) => crypto.createHash('sha256').update(text).digest('hex');

    /** 执行仓库中的一个脚本文件（全局作用域），返回它最后一个表达式的值。 */
    MA.run = (rel) => (0, eval)(fs.readFileSync(MA.abs(rel), 'utf8'));

    /** 新建 Bedrock 实体工程。会关闭同名的已打开工程，保证重跑结果一致。 */
    MA.newEntityProject = (name, texW, texH) => {
        for (const p of ModelProject.all.slice()) {
            if (p.name === name) {
                p.close(true);
            }
        }
        newProject(Formats.bedrock);
        Project.name = name;
        Project.geometry_name = name;
        Project.texture_width = texW;
        Project.texture_height = texH;
        Project.box_uv = false;
        return Project;
    };

    MA.bones = {};
    MA.bone = (name, pivot, parentName) => {
        const g = new Group({ name, origin: pivot.slice() });
        g.addTo(parentName ? MA.bones[parentName] : undefined);
        g.init();
        MA.bones[name] = g;
        return g;
    };

    /**
     * 方块。uv 为 {north|south|east|west|up|down: [x1, y1, x2, y2]}（贴图像素），缺省的面不渲染。
     * opts: rotation [x,y,z]（度），origin（旋转中心，缺省为方块中心），texture。
     */
    MA.cube = (name, boneName, from, to, uv, opts) => {
        opts = opts || {};
        const faces = {};
        for (const f of ['north', 'south', 'east', 'west', 'up', 'down']) {
            faces[f] = uv && uv[f] ? { uv: uv[f].slice(), texture: opts.texture ? opts.texture.uuid : undefined } : { uv: [0, 0, 0, 0], texture: null };
        }
        const center = [0, 1, 2].map((i) => (from[i] + to[i]) / 2);
        const c = new Cube({
            name,
            from: from.slice(),
            to: to.slice(),
            origin: (opts.origin || center).slice(),
            rotation: (opts.rotation || [0, 0, 0]).slice(),
            faces,
        });
        c.addTo(MA.bones[boneName]);
        c.init();
        return c;
    };

    MA.locator = (name, boneName, position) => {
        const l = new Locator({ name, position: position.slice() });
        l.addTo(MA.bones[boneName]);
        l.init();
        return l;
    };

    /** 用 2D 画布绘制贴图并加入工程。draw(ctx, w, h) 在画布上作画。 */
    MA.canvasTexture = (name, w, h, draw) => {
        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d');
        ctx.imageSmoothingEnabled = false;
        draw(ctx, w, h);
        const tex = new Texture({ name, width: w, height: h }).fromDataURL(canvas.toDataURL('image/png'));
        tex.add(false);
        tex.maCanvas = canvas; // 绘制用的原画布（导出变体贴图时在它上面复制改色，不等图片异步加载）
        return tex;
    };

    MA.applyTextureEverywhere = (tex) => {
        for (const c of Cube.all) {
            for (const f of Object.keys(c.faces)) {
                if (c.faces[f].texture !== null) {
                    c.faces[f].texture = tex.uuid;
                }
            }
        }
    };

    /** 把 #RRGGBB 颜色按系数调亮（>1）或调暗（<1）。 */
    MA.shade = (hex, f) => {
        const n = parseInt(hex.slice(1), 16);
        const c = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => Math.max(0, Math.min(255, Math.round(v * f))));
        return '#' + c.map((v) => v.toString(16).padStart(2, '0')).join('');
    };

    /** 5×7 点阵字母，用于校准贴图。 */
    MA.GLYPHS = {
        N: ['10001', '11001', '10101', '10011', '10001', '10001', '10001'],
        S: ['01111', '10000', '10000', '01110', '00001', '00001', '11110'],
        E: ['11111', '10000', '10000', '11110', '10000', '10000', '11111'],
        W: ['10001', '10001', '10001', '10101', '10101', '11011', '10001'],
        U: ['10001', '10001', '10001', '10001', '10001', '10001', '01110'],
        D: ['11110', '10001', '10001', '10001', '10001', '10001', '11110'],
        F: ['11111', '10000', '10000', '11110', '10000', '10000', '10000'],
        L: ['10000', '10000', '10000', '10000', '10000', '10000', '11111'],
    };
    MA.drawGlyph = (ctx, ch, x, y, scale, color) => {
        ctx.fillStyle = color;
        MA.GLYPHS[ch].forEach((row, j) => {
            for (let i = 0; i < row.length; i++) {
                if (row[i] === '1') {
                    ctx.fillRect(x + i * scale, y + j * scale, scale, scale);
                }
            }
        });
    };

    /** 导出 Bedrock 几何，返回写出的文本。 */
    MA.exportGeo = (rel) => {
        const text = Codecs.bedrock.compile();
        MA.writeText(rel, typeof text === 'string' ? text : JSON.stringify(text, null, '\t'));
        return MA.fs.readFileSync(MA.abs(rel), 'utf8');
    };

    MA.exportTexture = (tex, rel) => {
        const data = tex.getDataURL().split(',')[1];
        const p = MA.abs(rel);
        fs.mkdirSync(path.dirname(p), { recursive: true });
        fs.writeFileSync(p, Buffer.from(data, 'base64'));
        return p;
    };

    /** 保存 .bbmodel；exports 记录各导出文件的 sha256，供 check_rig.py 判断工程与导出是否同步。 */
    MA.saveProject = (rel, exportsByPath) => {
        const model = JSON.parse(Codecs.project.compile());
        model.mythic_age = { exports: {} };
        for (const [p, text] of Object.entries(exportsByPath || {})) {
            model.mythic_age.exports[p] = MA.sha256(text);
        }
        MA.writeText(rel, JSON.stringify(model, null, '\t'));
        Project.save_path = MA.abs(rel);
        Project.saved = true;
        return MA.abs(rel);
    };

    /**
     * 按部件尺寸自动排布 UV（同一 uv_group 只排一次，取该组第一个部件的尺寸）。
     * 以单个面为单位排布（长条部件的整张展开图会浪费大量空白）：面按高度降序做行排布，
     * 每个面四周留 1 像素保护带，超出图集时报错。薄片只有一个 surface 面。
     * 返回 {group: {group, color, plane, faces: {名: [x, y, w, h]}}}；面名为解剖学名（top/bottom/left/front/right/back）。
     */
    MA.packUvNets = (parts, atlas, tpu) => {
        const px = (u) => Math.max(1, Math.ceil(u * tpu - 1e-6));
        const nets = {};
        const items = [];
        for (const part of parts) {
            if (nets[part.uv_group]) {
                continue;
            }
            const [sx, sy, sz] = part.size_model_units;
            const plane = part.surface_kind === 'single_plane';
            nets[part.uv_group] = { group: part.uv_group, color: part.color, plane, faces: {} };
            const w = px(sx);
            const h = px(sy);
            const d = px(sz);
            const faces = plane
                ? { surface: [w, h] }
                : { top: [w, d], bottom: [w, d], left: [d, h], front: [w, h], right: [d, h], back: [w, h] };
            for (const [face, [fw, fh]] of Object.entries(faces)) {
                items.push({ group: part.uv_group, face, w: fw, h: fh });
            }
        }
        items.sort((a, b) => b.h - a.h || b.w - a.w || (a.group + a.face < b.group + b.face ? -1 : 1));
        let x = 0;
        let y = 0;
        let rowH = 0;
        for (const it of items) {
            if (x + it.w + 2 > atlas) {
                x = 0;
                y += rowH;
                rowH = 0;
            }
            if (y + it.h + 2 > atlas) {
                throw new Error('UV 排布超出 ' + atlas + '×' + atlas + '：' + it.group + '.' + it.face);
            }
            nets[it.group].faces[it.face] = [x + 1, y + 1, it.w, it.h];
            x += it.w + 2;
            rowH = Math.max(rowH, it.h + 2);
        }
        return nets;
    };

    /**
     * 用主视图截图并写入文件（异步；返回 Promise<路径>）。camera: {position, target, fov}，Blockbench 坐标。
     * 截图用于审阅记录，写到 docs/ 下（不入 git）。
     */
    MA.shot = (rel, camera) => {
        const preview = Preview.selected;
        preview.camera.position.set(...camera.position);
        preview.controls.target.set(...camera.target);
        if (camera.fov) {
            preview.setFOV(camera.fov);
        }
        preview.controls.update();
        // 直接渲染后取画布：Blockbench 窗口在后台时 setTimeout 与 Screencam 回调不会触发（2026-10-18 实测），
        // 同步渲染再读画布不受影响
        preview.render();
        const dataUrl = preview.canvas.toDataURL('image/png');
        const p = MA.abs(rel);
        fs.mkdirSync(path.dirname(p), { recursive: true });
        fs.writeFileSync(p, Buffer.from(dataUrl.split(',')[1], 'base64'));
        return Promise.resolve(p);
    };

    MA.reset = () => {
        MA.bones = {};
    };
    return 'MA lib loaded';
})();
